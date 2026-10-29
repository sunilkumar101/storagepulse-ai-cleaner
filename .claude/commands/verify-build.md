---
description: Build the debug APK and fix any compile, resource, manifest or lint errors
---

Run `./gradlew assembleDebug` and fix any Kotlin compilation, resource, manifest, or lint errors immediately in the affected files only.

- On Windows use `./gradlew.bat assembleDebug` if `./gradlew` is not runnable.
- The system JDK is 25, which Gradle 8.13 cannot run on. Always build with Android Studio's JDK 21:
  `export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"` (bash) before `./gradlew`.
- If the build fails with "Could not move temporary workspace", that is a Windows file-lock flake: just re-run.
- Also run `./gradlew lintDebug`; fix errors, ignore "newer version available" warnings unless asked.
- Touch only the files named in the error output. Do not refactor unrelated code.
- Re-run the build after each fix until it passes, then report what was fixed.

$ARGUMENTS
