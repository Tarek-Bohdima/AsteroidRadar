# Keep filenames + line numbers in stack traces; rename the source-file
# attribute so the original .kt filename doesn't leak post-obfuscation.
# Production crashes still symbolicate via the AGP-emitted mapping.txt
# (uploaded as a release artifact by .github/workflows/release.yml).
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# AAR-shipped consumer rules from Hilt, Room, Retrofit, Moshi, Coil, kotlinx-
# parcelize, and androidx-* cover the rest of the runtime-reflection surface.
# Add reflective-keeps here only when a release smoke surfaces a missing one
# — debug with the r8-analyzer Claude Code skill (https://github.com/android/skills).

# kotlinx-serialization typed Nav-Compose routes encode `Asteroid` (and any
# future @Serializable route argument) via Json.encodeToString. R8 strips
# the synthetic `Companion.serializer()` accessor unless we keep the class
# + its companion. Phase 9c is the first new R8 keep surface since 6b.
-if @kotlinx.serialization.Serializable class **
-keepclasseswithmembers class <1> {
    static <1>$Companion Companion;
}
-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <1>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}
-if @kotlinx.serialization.Serializable class ** {
    public static ** INSTANCE;
}
-keepclassmembers class <1> {
    public static <1> INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}
