# Red Auto Alert — Copilot Instructions

## Project Overview

Android app that captures Red Alert (Tzofar) rocket/missile notifications and forwards them to Android Auto as messaging-style notifications with TTS voice announcements.

## Architecture

- **model/** — `AlertEvent` data class, `AlertProcessor` interface
- **service/** — `AlertEventBus` (SharedFlow event distribution), `AlertNotificationListener` (captures notifications from `com.redalert.tzevaadom`)
- **processor/** — `AlertForwarder` (MessagingStyle + CarAppExtender), `TtsAlertAnnouncer` (Text-to-Speech)
- **ui/** — `SettingsActivity` (phone settings UI with debug log)
- **util/** — `PermissionHelper`, `PrefsManager`, `DebugLog`
- **RedAutoAlertApp** — Application class that initializes processors at process startup

## Key Patterns

- Processors are registered in `RedAutoAlertApp.onCreate()`, not in activities — this ensures they exist when the `NotificationListenerService` restarts independently
- `AlertEventBus` is a singleton that dispatches to all registered `AlertProcessor` implementations
- `DebugLog` is gated behind `BuildConfig.DEBUG` — no-op in release builds
- The app uses Material 3 theme (`Theme.Material3.DayNight`)

## Build

- **Language:** Kotlin 1.9.22
- **Android:** minSdk 26, targetSdk 34, compileSdk 34
- **Build system:** Gradle 8.5 with AGP 8.2.2
- **JDK:** Requires Java 17+ (JDK 21 recommended)
- **Build command:** `./gradlew assembleDebug`
- **APK output:** `app/build/outputs/apk/debug/app-debug.apk`

## Git Workflow

When asked to fix something or make code changes:

1. **If on `main`**: create a new feature branch, commit the changes, and open a pull request.
2. **If on a feature branch with an open PR**: commit the changes and push to the existing branch (the PR updates automatically).

## Conventions

- Use `CarAppExtender` for Android Auto notification compatibility
- All alert processing goes through `AlertEventBus.emitBlocking()` for synchronous dispatch
- New alert consumers should implement `AlertProcessor` interface and register in `RedAutoAlertApp`
- String resources in `res/values/strings.xml` — avoid hardcoded strings in layouts
- Use theme attributes (`?attr/color*`) not hardcoded colors

## Testing

- No unit tests yet — use the "Send Test Alert" button in the app UI
- Debug log card (visible in debug builds only) shows real-time event flow
- Monitor via `adb logcat -s RedAutoAlertApp:* AlertListener:* AlertEventBus:* TtsAnnouncer:*`

## CI

- GitHub Actions workflow: `.github/workflows/android-ci.yml`
- Builds on PR and push to `main`, uploads APK as artifact
- Gradle wrapper validation enabled
