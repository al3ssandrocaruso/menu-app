# R8 configuration for the release build (minifyEnabled + shrinkResources).
#
# No app-specific keep rules are required:
#
#  * JSON is parsed with Gson's tree API and models are constructed explicitly in
#    MenuJsonParser, so no model class is populated reflectively and none needs to survive
#    obfuscation. (A reflective `Gson.fromJson<T>()` binding would have needed keep rules.)
#  * Room, Coil, Volley and ZXing ship their own consumer rules in their AARs/JARs.
#  * MenuApplication and MainActivity are referenced from AndroidManifest.xml, which R8 treats
#    as a root.
#
# Keep line numbers in stack traces from release builds.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
