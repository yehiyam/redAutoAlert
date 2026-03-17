# Red Auto Alert

Red Alert (Tzofar) → Android Auto notification bridge.

Captures rocket/missile alerts from the Red Alert app and displays them on Android Auto.

## Features
- 🚨 Forwards Red Alert notifications to Android Auto as messaging-style notifications
- 🔊 Text-to-Speech voice announcements (Hebrew, English, Russian, Arabic)
- ⚡ Zero-latency: captures notifications the instant they arrive
- 🚗 Works with any Android Auto head unit

## How It Works
1. `NotificationListenerService` captures notifications from `com.redalert.tzevaadom`
2. Parses alert type (rocket, drone, earthquake, etc.) and affected cities
3. Re-posts as `MessagingStyle` notification with `CarAppExtender`
4. Android Auto displays the alert on the car screen
5. TTS engine announces the alert over car speakers

## Setup
1. Install the Red Alert app (Tzofar) from Google Play
2. Configure your area alerts in the Red Alert app
3. Install this APK (sideload)
4. Open Red Auto Alert → Grant "Notification Access" permission
5. On Android 13+: You may need to enable "Restricted settings" first
6. Connect to Android Auto — alerts will now show on your car!

## Build
```bash
./gradlew assembleDebug
```
APK will be at `app/build/outputs/apk/debug/app-debug.apk`

## Architecture
```
model/
  AlertEvent.kt          - Alert data class
  AlertProcessor.kt      - Interface for alert consumers
service/
  AlertEventBus.kt       - SharedFlow event distribution
  AlertNotificationListener.kt - Captures Red Alert notifications
processor/
  AlertForwarder.kt      - MessagingStyle + CarAppExtender
  TtsAlertAnnouncer.kt   - Text-to-Speech
ui/
  SettingsActivity.kt    - Phone settings UI
util/
  PermissionHelper.kt    - Permission management
  PrefsManager.kt        - SharedPreferences
```

## Future (Phase 2)
- Car App Library screen with persistent alert display
- Alert history on car screen
- Countdown timer to shelter
