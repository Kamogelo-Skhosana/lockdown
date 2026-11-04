# Android release

Build from the exact commit intended for delivery. Use a trusted shell or release environment with JDK 17, Gradle 8.11.1 and Android SDK 35. Keep the signing keystore outside the checkout and retain it securely for future upgrades.

Set these variables in the trusted build environment:

- `LOCKDOWN_KEYSTORE` — absolute path to the release keystore
- `LOCKDOWN_STORE_PASSWORD` — keystore password
- `LOCKDOWN_KEY_ALIAS` — release key alias
- `LOCKDOWN_KEY_PASSWORD` — release key password

Run from the repository root on Linux:

```sh
set -eu
: "${LOCKDOWN_KEYSTORE:?Set the keystore path}"
: "${LOCKDOWN_STORE_PASSWORD:?Set the keystore password}"
: "${LOCKDOWN_KEY_ALIAS:?Set the key alias}"
: "${LOCKDOWN_KEY_PASSWORD:?Set the key password}"
test -f "$LOCKDOWN_KEYSTORE"
gradle -p android :app:assembleRelease
apk=android/app/build/outputs/apk/release/app-release.apk
"${ANDROID_HOME:?Set Android SDK path}/build-tools/35.0.0/apksigner" verify --verbose --print-certs "$apk"
sha256sum "$apk"
```

Record the commit, APK SHA-256, signer certificate fingerprint and device/Android versions. Test a clean install and, when a previous release signed with the same key exists, an upgrade. Complete an offline incident after install and run the release checks in #18. Share the verified APK and checksum through the approved delivery channel. Use the debug APK from Verify Lockdown for ordinary development tests.
