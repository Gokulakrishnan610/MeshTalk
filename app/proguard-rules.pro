# Room
-keepclassmembers class * extends androidx.room.RoomDatabase {
    <init>(...);
}
-keep class * extends androidx.room.RoomDatabase
-keep class * { @androidx.room.Entity *; }

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.examples.android.model.** { <fields>; }
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# MeshTalk Models (Ensure Gson can deserialize them)
-keep class com.ble_mesh.meshtalk.data.model.** { *; }

# BouncyCastle
-dontwarn org.bouncycastle.**
-keep class org.bouncycastle.** { *; }

# Security Crypto
-keep class androidx.security.crypto.** { *; }

# General
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Fix for Tink/Security-Crypto missing optional dependencies
-dontwarn javax.annotation.concurrent.ThreadSafe
-dontwarn org.joda.time.Instant
-dontwarn com.google.crypto.tink.**
