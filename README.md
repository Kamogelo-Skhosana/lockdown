# Lockdown

Android starter for the digital incident response app. The Android shell uses Kotlin, Jetpack Compose and Material 3, following the design handoff. FastAPI handles optional Meta Model API calls on the server.

## Android

Open `android/` in Android Studio, select JDK 17 and install Android SDK 35. The project uses Android Gradle Plugin 8.9.2 and expects Gradle 8.11.1. With that Gradle version installed, run `gradle wrapper --gradle-version 8.11.1` from `android/`, then sync and run the `app` configuration on an emulator or device.

`android/app/src/main/` contains the starter activity. Place JVM tests in `android/app/src/test/` and device tests in `android/app/src/androidTest/` as features are added.

## API

From `api/`, create and activate a Python virtual environment, then run:

```text
python -m pip install -r requirements-dev.txt
python -m uvicorn lockdown_api.main:app --reload
```

Open `http://127.0.0.1:8000/health` to check the service. Run `python -m pytest` from `api/` for the health endpoint test. Keep Meta API credentials in the server environment.

## Online assessment setup

Set `META_MODEL_API_KEY` and `META_MODEL_ID` in the backend environment for live analysis. Set `META_MODEL_API_ENABLED=false` to rehearse manual fallback. The per-process limits default to 30 attempts over the process lifetime and six attempts per rolling minute. Configure them with `META_MODEL_REQUEST_LIMIT` and `META_MODEL_REQUESTS_PER_MINUTE`.

For emulator or device testing, use a reachable HTTPS backend with a trusted certificate. Set `LOCKDOWN_ASSESSMENT_URL` to the full HTTPS endpoint ending in `/assessment` before building the APK, then reinstall the rebuilt app. This setting is a URL only. Keep the Meta key on the backend. A build with an empty endpoint supports the offline flow.

Verify `/health` first, then test **Get emergency help → Start an incident → Payment fraud** with a synthetic description and choose **Agree and analyse**. Check that the app shows the assessment from the configured backend. Disable the provider and repeat to verify manual fallback. Record the commit, device or emulator API level, and outcome in #9 and #16.

Start with the [design handoff](https://github.com/Kamogelo-Skhosana/lockdown/issues/7) and [bootstrap issue](https://github.com/Kamogelo-Skhosana/lockdown/issues/9).
