# Keep kotlinx.serialization-generated serializers used by type-safe navigation routes.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers @kotlinx.serialization.Serializable class com.rave.app.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
