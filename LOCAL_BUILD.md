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

Example: generate a keystore locally (this repo already contains a `release-keystore.jks` generated locally — DO NOT commit your own keystore to public repos):

```bash
# generate (example) — you should create your own secure password
keytool -genkeypair -v -keystore release-keystore.jks -alias devnet_release -keyalg RSA -keysize 2048 -validity 10000
```

Configure signing env vars before building a release:

```bash
export KEYSTORE_PATH="$PWD/release-keystore.jks"
export STORE_PASSWORD="<your_keystore_password>"
export KEY_PASSWORD="<your_key_password>"
export KEY_ALIAS="devnet_release"

./gradlew :app:assembleRelease
```

For CI (GitHub Actions) add `KEYSTORE_PATH` (path in runner or use secure upload), `STORE_PASSWORD`, `KEY_PASSWORD`, and `KEY_ALIAS` as repository secrets and reference them in workflow.
