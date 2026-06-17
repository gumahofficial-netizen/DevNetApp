Local build instructions

Prerequisites:
- Java 11
- Android SDK (platform-tools, platforms;android-35, build-tools 35.0.0)
- Gradle (or let Gradle wrapper be generated)

Build commands (Linux):

```bash
# Generate wrapper if missing
gradle wrapper

# Build debug APK
./gradlew :app:assembleDebug

# Output APK:
app/build/outputs/apk/debug/app-debug.apk
```

If you need a signed release build, create a keystore and set environment variables or configure `app/build.gradle.kts` signingConfigs with your keystore path and passwords.
