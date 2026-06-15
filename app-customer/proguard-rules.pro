# ProGuard rules for app-customer

# Keep domain models to prevent obfuscation breaking JSON serialization
-keep class com.example.famekodriver.core.domain.model.** { *; }
-keep class com.example.famekodriver.core.data.model.** { *; }

# Keep JDBC and PostgreSQL classes for database connections
-keep class org.postgresql.** { *; }
-keep class java.sql.** { *; }
-dontwarn org.postgresql.**

# Gson specific rules
-keepattributes Signature, *Annotation*, EnclosingMethod
-keep class sun.misc.Unsafe { *; }
-keep class com.google.gson.stream.** { *; }
-keep class com.google.gson.reflect.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Retrofit rules
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keep class retrofit2.** { *; }
-dontwarn retrofit2.**
-keep class com.example.famekodriver.core.network.** { *; }

# OsmDroid (Maps)
-keep class org.osmdroid.** { *; }
-dontwarn org.osmdroid.**

# Material Design
-keep class com.google.android.material.** { *; }
-dontwarn com.google.android.material.**

# Jackson (if used via core dependencies)
-keepnames class com.fasterxml.jackson.** { *; }
-dontwarn com.fasterxml.jackson.databind.**

-keepattributes InnerClasses
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
