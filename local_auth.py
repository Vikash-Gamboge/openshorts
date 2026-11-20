"""Standalone SQLite-backed public authentication for Pro Clipping Master / OpenShorts.

Mounted automatically by ``app.py`` whenever ``BILLING_ENABLED`` is false so that
Sign Up, Log In, Log Out (with server-side JWT revocation), Account Profile, and
Permanent Account Deletion work out of the box on Render without requiring a
separate PostgreSQL instance.

When ``BILLING_ENABLED=true`` and ``DATABASE_URL`` are configured, ``cloud/auth.py``
handles these exact same routes against PostgreSQL instead.
"""

from __future__ import annotations

import base64
import hashlib
import hmac
import json
import os
import re
import secrets
import sqlite3
import threading
import time
import uuid
from typing import Any, Dict, Optional

try:
    from fastapi import APIRouter, HTTPException, Request
    from pydantic import BaseModel
except ImportError:  # Allows pure-stdlib unit testing without FastAPI installed
    APIRouter = None
    HTTPException = None
    Request = Any
    BaseModel = object

_EMAIL_RE = re.compile(r"^[^@\s]+@[^@\s]+\.[^@\s]+$")
_SCRYPT_N = 16384
_SCRYPT_R = 8
_SCRYPT_P = 1
_SCRYPT_DKLEN = 32
_JWT_TTL_SECONDS = 30 * 24 * 3600  # 30 days
_FREE_PLAN_MINUTES = 20.0
_AUTH_RATE_LIMIT = 30
_AUTH_RATE_WINDOW = 3600.0

_db_lock = threading.RLock()
_rate_lock = threading.Lock()
_rate_buckets: Dict[str, list] = {}


def _default_db_path() -> str:
    env_path = os.environ.get("LOCAL_AUTH_DB_PATH", "").strip()
    if env_path:
        return env_path
    out_dir = os.environ.get("OUTPUT_DIR", "output")
    os.makedirs(out_dir, exist_ok=True)
    return os.path.join(out_dir, "users.db")


def _jwt_secret(db_path: Optional[str] = None) -> bytes:
    env_secret = os.environ.get("JWT_SECRET", "").strip()
    if env_secret:
        return env_secret.encode("utf-8")
    base_dir = os.path.dirname(os.path.abspath(db_path or _default_db_path()))
    os.makedirs(base_dir, exist_ok=True)
    secret_file = os.path.join(base_dir, ".jwt_secret")
    with _db_lock:
        if os.path.exists(secret_file):
            with open(secret_file, "r", encoding="utf-8") as f:
                stored = f.read().strip()
                if stored:
                    return stored.encode("utf-8")
        generated = secrets.token_hex(32)
        with open(secret_file, "w", encoding="utf-8") as f:
            f.write(generated)
        try:
            os.chmod(secret_file, 0o600)
        except OSError:
            pass
        return generated.encode("utf-8")


def _b64url_encode(raw: bytes) -> str:
    return base64.urlsafe_b64encode(raw).rstrip(b"=").decode("ascii")


def _b64url_decode(encoded: str) -> bytes:
    padding = "=" * (-len(encoded) % 4)
    return base64.urlsafe_b64decode(encoded + padding)


def hash_password(password: str) -> str:
    salt = secrets.token_hex(16)
    dk = hashlib.scrypt(
        password.encode("utf-8"),
        salt=bytes.fromhex(salt),
        n=_SCRYPT_N,
        r=_SCRYPT_R,
        p=_SCRYPT_P,
        dklen=_SCRYPT_DKLEN,
    )
    return f"scrypt${_SCRYPT_N}${_SCRYPT_R}${_SCRYPT_P}${salt}${dk.hex()}"


def verify_password(password: str, stored_hash: Optional[str]) -> bool:
    if not stored_hash or not isinstance(stored_hash, str):
        return False
    parts = stored_hash.split("$")
    if len(parts) != 6 or parts[0] != "scrypt":
        return False
    try:
        n, r, p = int(parts[1]), int(parts[2]), int(parts[3])
        salt = bytes.fromhex(parts[4])
        expected = bytes.fromhex(parts[5])
    except (ValueError, TypeError):
        return False
    derived = hashlib.scrypt(
        password.encode("utf-8"),
        salt=salt,
        n=n,
        r=r,
        p=p,
        dklen=len(expected),
    )
    return hmac.compare_digest(derived, expected)


def create_jwt(user_id: str, email: str, token_version: int, db_path: Optional[str] = None) -> str:
    now = int(time.time())
    header = {"alg": "HS256", "typ": "JWT"}
    payload = {
        "sub": user_id,
        "email": email,
        "tv": int(token_version),
        "iat": now,
        "exp": now + _JWT_TTL_SECONDS,
    }
    h_b64 = _b64url_encode(json.dumps(header, separators=(",", ":")).encode("utf-8"))
    p_b64 = _b64url_encode(json.dumps(payload, separators=(",", ":")).encode("utf-8"))
    signing_input = f"{h_b64}.{p_b64}".encode("ascii")
    sig = hmac.new(_jwt_secret(db_path), signing_input, hashlib.sha256).digest()
    return f"{h_b64}.{p_b64}.{_b64url_encode(sig)}"


def decode_jwt(token: str, db_path: Optional[str] = None) -> Optional[Dict[str, Any]]:
    parts = (token or "").strip().split(".")
    if len(parts) != 3:
        return None
    h_b64, p_b64, s_b64 = parts
    try:
        signing_input = f"{h_b64}.{p_b64}".encode("ascii")
        expected_sig = hmac.new(_jwt_secret(db_path), signing_input, hashlib.sha256).digest()
        provided_sig = _b64url_decode(s_b64)
        if not hmac.compare_digest(expected_sig, provided_sig):
            return None
        payload = json.loads(_b64url_decode(p_b64).decode("utf-8"))
        if int(payload.get("exp", 0)) < int(time.time()):
            return None
        return payload
    except Exception:
        return None


def _connect(db_path: Optional[str] = None) -> sqlite3.Connection:
    path = db_path or _default_db_path()
    os.makedirs(os.path.dirname(os.path.abspath(path)), exist_ok=True)
    conn = sqlite3.connect(path, check_same_thread=False)
    conn.row_factory = sqlite3.Row
    conn.execute(
        """
        CREATE TABLE IF NOT EXISTS users (
            id TEXT PRIMARY KEY,
            email TEXT UNIQUE NOT NULL,
            display_name TEXT NOT NULL DEFAULT '',
            password_hash TEXT NOT NULL,
            plan TEXT NOT NULL DEFAULT 'free',
            token_version INTEGER NOT NULL DEFAULT 1,
            minutes_used REAL NOT NULL DEFAULT 0.0,
            created_at REAL NOT NULL
        )
        """
    )
    conn.commit()
    return conn


def _normalize_email(email: str) -> str:
    return (email or "").strip().lower()


def _minutes_payload(minutes_used: float = 0.0) -> Dict[str, float]:
    used = max(0.0, float(minutes_used or 0.0))
    rem = max(0.0, _FREE_PLAN_MINUTES - used)
    return {
        "plan_allowance": _FREE_PLAN_MINUTES,
        "plan_used": round(used, 2),
        "plan_remaining": round(rem, 2),
        "topup_remaining": 0.0,
        "remaining": round(rem, 2),
    }


def _user_dto(row: sqlite3.Row) -> Dict[str, Any]:
    return {
        "id": row["id"],
        "email": row["email"],
        "display_name": row["display_name"] or "",
        "entitled": True,
        "plan": row["plan"] or "free",
    }


def _check_rate(key: str) -> bool:
    now = time.time()
    with _rate_lock:
        bucket = [t for t in _rate_buckets.get(key, []) if now - t < _AUTH_RATE_WINDOW]
        if len(bucket) >= _AUTH_RATE_LIMIT:
            _rate_buckets[key] = bucket
            return False
        bucket.append(now)
        _rate_buckets[key] = bucket
        return True


def signup_user(
    email: str,
    password: str,
    display_name: Optional[str] = None,
    db_path: Optional[str] = None,
) -> Dict[str, Any]:
    clean_email = _normalize_email(email)
    if not clean_email or not _EMAIL_RE.match(clean_email):
        raise ValueError("Please enter a valid email address.")
    if not password or len(password) < 8:
        raise ValueError("Password must be at least 8 characters long.")
    clean_name = (display_name or "").strip()[:80] or clean_email.split("@")[0]

    with _db_lock:
        conn = _connect(db_path)
        try:
            existing = conn.execute("SELECT id FROM users WHERE email = ?", (clean_email,)).fetchone()
            if existing is not None:
                raise KeyError("An account with this email already exists. Please log in instead.")
            user_id = str(uuid.uuid4())
            pw_hash = hash_password(password)
            conn.execute(
                """
                INSERT INTO users (id, email, display_name, password_hash, plan, token_version, minutes_used, created_at)
                VALUES (?, ?, ?, ?, 'free', 1, 0.0, ?)
                """,
                (user_id, clean_email, clean_name, pw_hash, time.time()),
            )
            conn.commit()
            row = conn.execute("SELECT * FROM users WHERE id = ?", (user_id,)).fetchone()
        finally:
            conn.close()

    token = create_jwt(row["id"], row["email"], row["token_version"], db_path=db_path)
    return {
        "token": token,
        "user": _user_dto(row),
        "minutes": _minutes_payload(row["minutes_used"]),
    }


def login_user(
    email: str,
    password: str,
    db_path: Optional[str] = None,
) -> Dict[str, Any]:
    clean_email = _normalize_email(email)
    if not clean_email or not password:
        raise PermissionError("Invalid email or password.")

    with _db_lock:
        conn = _connect(db_path)
        try:
            row = conn.execute("SELECT * FROM users WHERE email = ?", (clean_email,)).fetchone()
        finally:
            conn.close()

    if row is None or not verify_password(password, row["password_hash"]):
        raise PermissionError("Invalid email or password.")

    token = create_jwt(row["id"], row["email"], row["token_version"], db_path=db_path)
    return {
        "token": token,
        "user": _user_dto(row),
        "minutes": _minutes_payload(row["minutes_used"]),
    }


def get_user_by_token(token: str, db_path: Optional[str] = None) -> Optional[Dict[str, Any]]:
    payload = decode_jwt(token, db_path=db_path)
    if not payload or not payload.get("sub"):
        return None
    with _db_lock:
        conn = _connect(db_path)
        try:
            row = conn.execute("SELECT * FROM users WHERE id = ?", (payload["sub"],)).fetchone()
        finally:
            conn.close()
    if row is None:
        return None
    if int(payload.get("tv", -1)) != int(row["token_version"]):
        return None
    return dict(row)


def logout_user(user_id: str, db_path: Optional[str] = None) -> None:
    with _db_lock:
        conn = _connect(db_path)
        try:
            conn.execute(
                "UPDATE users SET token_version = token_version + 1 WHERE id = ?",
                (user_id,),
            )
            conn.commit()
        finally:
            conn.close()


def delete_user_account(user_id: str, confirm_email: str, db_path: Optional[str] = None) -> None:
    with _db_lock:
        conn = _connect(db_path)
        try:
            row = conn.execute("SELECT email FROM users WHERE id = ?", (user_id,)).fetchone()
            if row is None:
                raise PermissionError("Account not found.")
            if _normalize_email(confirm_email) != _normalize_email(row["email"]):
                raise ValueError("Please enter your exact account email to confirm deletion.")
            conn.execute("DELETE FROM users WHERE id = ?", (user_id,))
            conn.commit()
        finally:
            conn.close()


def _extract_bearer(request: Request) -> str:
    auth_header = (request.headers.get("Authorization") or "").strip()
    if auth_header.lower().startswith("bearer "):
        return auth_header[7:].strip()
    return ""


def get_user_from_request(request: Request) -> Optional[Dict[str, Any]]:
    token = _extract_bearer(request)
    if not token:
        return None
    return get_user_by_token(token)


def get_user_id_from_request(request: Request) -> Optional[str]:
    user = get_user_from_request(request)
    return user["id"] if user else None


if APIRouter is not None:
    router = APIRouter()

    class LocalSignupBody(BaseModel):
        email: str
        password: str
        display_name: Optional[str] = None

    class LocalLoginBody(BaseModel):
        email: str
        password: str

    class LocalDeleteBody(BaseModel):
        confirm: str
        reason: Optional[str] = "other"

    def _client_ip(request: Request) -> str:
        fwd = request.headers.get("x-forwarded-for")
        if fwd:
            return fwd.split(",")[0].strip()
        return request.client.host if request.client else "unknown"

    @router.post("/api/auth/signup")
    async def api_signup(body: LocalSignupBody, request: Request):
        if not _check_rate(f"signup:{_client_ip(request)}"):
            raise HTTPException(status_code=429, detail="Too many sign-up attempts. Please wait a bit and try again.")
        try:
            return signup_user(body.email, body.password, body.display_name)
        except ValueError as e:
            raise HTTPException(status_code=400, detail=str(e))
        except KeyError as e:
            raise HTTPException(status_code=409, detail=e.args[0])

    @router.post("/api/auth/login")
    async def api_login(body: LocalLoginBody, request: Request):
        if not _check_rate(f"login:{_client_ip(request)}"):
            raise HTTPException(status_code=429, detail="Too many login attempts. Please wait a bit and try again.")
        try:
            return login_user(body.email, body.password)
        except PermissionError as e:
            raise HTTPException(status_code=401, detail=str(e))

    @router.post("/api/auth/logout")
    async def api_logout(request: Request):
        user = get_user_from_request(request)
        if user is not None:
            logout_user(user["id"])
        return {"ok": True}

    @router.get("/api/me")
    async def api_me(request: Request):
        user = get_user_from_request(request)
        if user is None:
            raise HTTPException(status_code=401, detail="Please sign in to continue.")
        retention_hours = max(1, int(os.environ.get("ORIGINAL_VIDEO_RETENTION_HOURS", "6") or 6))
        return {
            "user": {
                "id": user["id"],
                "email": user["email"],
                "display_name": user["display_name"] or "",
                "entitled": True,
                "plan": user["plan"] or "free",
            },
            "entitled": True,
            "plan": user["plan"] or "free",
            "status": "active",
            "minutes": _minutes_payload(user.get("minutes_used", 0.0)),
            "storage_provider": "server",
            "original_retention_hours": retention_hours,
        }

    @router.delete("/api/account")
    async def api_delete_account(body: LocalDeleteBody, request: Request):
        user = get_user_from_request(request)
        if user is None:
            raise HTTPException(status_code=401, detail="Please sign in to continue.")
        try:
            delete_user_account(user["id"], body.confirm)
        except ValueError as e:
            raise HTTPException(status_code=400, detail=str(e))
        except PermissionError as e:
            raise HTTPException(status_code=401, detail=str(e))
        return {"deleted": True}

    @router.get("/api/billing/plans")
    async def api_plans():
        return {
            "payments_configured": False,
            "plans": [
                {
                    "kind": "subscription",
                    "price_id": "",
                    "lookup_key": "starter_monthly",
                    "plan": "starter",
                    "interval": "month",
                    "minutes": 100,
                    "amount": 1900,
                    "currency": "usd",
                    "checkout_available": False,
                },
                {
                    "kind": "subscription",
                    "price_id": "",
                    "lookup_key": "creator_monthly",
                    "plan": "creator",
                    "interval": "month",
                    "minutes": 300,
                    "amount": 3900,
                    "currency": "usd",
                    "checkout_available": False,
                },
                {
                    "kind": "subscription",
                    "price_id": "",
                    "lookup_key": "pro_monthly",
                    "plan": "pro",
                    "interval": "month",
                    "minutes": 750,
                    "amount": 7900,
                    "currency": "usd",
                    "checkout_available": False,
                },
            ],
        }

    @router.post("/api/billing/checkout")
    async def api_checkout():
        raise HTTPException(
            status_code=503,
            detail="Online plan checkout is currently being finalized. Continue enjoying your free credits in the meantime.",
        )

    @router.get("/api/history")
    async def api_history(request: Request):
        user = get_user_from_request(request)
        if user is None:
            raise HTTPException(status_code=401, detail="Please sign in to view your clip history.")
        return {"videos": []}

    @router.get("/api/projects")
    async def api_projects(request: Request):
        user = get_user_from_request(request)
        if user is None:
            raise HTTPException(status_code=401, detail="Please sign in to view your projects.")
        return {"projects": []}
