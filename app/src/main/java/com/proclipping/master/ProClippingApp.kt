package com.proclipping.master

import android.app.Application
import com.proclipping.master.data.local.SettingsDataStore
import com.proclipping.master.data.remote.OpenShortsApiClient
import com.proclipping.master.data.repository.ClipDownloadManager
import com.proclipping.master.data.repository.ClippingRepository

class AppContainer(application: Application) {
    val apiClient: OpenShortsApiClient = OpenShortsApiClient()
    val settingsDataStore: SettingsDataStore = SettingsDataStore(application, apiClient.json)
    val clippingRepository: ClippingRepository = ClippingRepository(
        context = application,
        apiClient = apiClient,
        settingsDataStore = settingsDataStore
    )
    val clipDownloadManager: ClipDownloadManager = ClipDownloadManager(application)
}

class ProClippingApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
