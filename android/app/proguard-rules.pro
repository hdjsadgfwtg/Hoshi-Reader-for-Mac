# kotlinx-serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.hoshi.reader.**$$serializer { *; }
-keepclassmembers class com.hoshi.reader.** {
    *** Companion;
}
-keepclasseswithmembers class com.hoshi.reader.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# epub4j
-keep class io.documentnode.epub4j.** { *; }
-dontwarn org.xmlpull.**
-dontwarn nl.siegmann.epublib.**
