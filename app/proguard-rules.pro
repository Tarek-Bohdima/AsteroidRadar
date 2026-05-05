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

# Navigation safe-args resolves `app:argType="com.tarek.asteroidradar.domain.X"`
# from the nav graph XML via Class.forName at NavInflater time — R8's class
# renaming breaks that lookup. Keep the domain package's class names so
# parcelable arg types stay resolvable.
-keep class com.tarek.asteroidradar.domain.** { *; }
