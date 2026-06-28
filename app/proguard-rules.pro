# Keep kotlinx.serialization generated serializers.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers class com.claudeusage.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.claudeusage.app.**$$serializer { *; }
-keepclassmembers class com.claudeusage.app.** {
    *** Companion;
}

# OkHttp platform shim warnings.
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# Tink (via androidx.security-crypto) references Error Prone / javax annotations
# that aren't on the runtime classpath. R8 treats these as missing-class errors
# in full mode, so silence them — they're compile-only annotations.
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.annotation.**
-dontwarn com.google.api.client.http.**
-keep class com.google.crypto.tink.** { *; }

# Glance / Compose runtime.
-keep class androidx.glance.** { *; }
