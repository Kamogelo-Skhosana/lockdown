# Lockdown

Android starter for the digital incident response app. The Android shell uses Kotlin, Jetpack Compose and Material 3, following the design handoff. The FastAPI service is the future server-side boundary for Meta Model API calls.

## Android

Open `android/` in Android Studio, select JDK 17 and install Android SDK 35. The project uses Android Gradle Plugin 8.9.2 and expects Gradle 8.11.1. With that Gradle version installed, run `gradle wrapper --gradle-version 8.11.1` from `android/`, then sync and run the `app` configuration on an emulator or device.

`android/app/src/main/` contains the starter activity. Place JVM tests in `android/app/src/test/` and device tests in `android/app/src/androidTest/` as features are added.

## API

From `api/`, create and activate a Python virtual environment, then run:

```text
python -m pip install -r requirements-dev.txt
python -m uvicorn lockdown_api.main:app --reload
```

Open `http://127.0.0.1:8000/health` to check the service. Run `python -m pytest` from `api/` for the health endpoint test. Add future Meta API credentials to the server environment only.

Start with the [design handoff](https://github.com/Kamogelo-Skhosana/lockdown/issues/7) and [bootstrap issue](https://github.com/Kamogelo-Skhosana/lockdown/issues/9).
