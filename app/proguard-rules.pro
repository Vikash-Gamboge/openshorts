# Keep Kotlinx Serialization models
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.proclipping.master.data.model.**$$serializer { *; }
-keepclassmembers class com.proclipping.master.data.model.** {
    *** Companion;
}
-keepclasseswithmembers class com.proclipping.master.data.model.** {
    kotlinx.serialization.KSerializer serializer(...);
}
