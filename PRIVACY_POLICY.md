# Privacy Policy — Red Auto Alert

**Last updated:** March 2026

## Overview

Red Auto Alert is a safety utility app that forwards rocket and missile alert notifications from the Red Alert (Tzofar) app to Android Auto, helping drivers stay informed of critical safety events.

## What Data We Access

Red Auto Alert uses Android's Notification Listener Service to read notifications **only** from the following apps:
- Red Alert: Israel (com.redalert.tzevaadom)
- RedAlert (com.red.alert)

The data accessed from these notifications includes:
- Alert title (e.g., "Red Alert")
- Alert text (e.g., city names under threat)
- Alert timestamp

## How Data Is Used

Notification data is used **exclusively** to:
1. Re-post the alert as an Android Auto-compatible notification on your car display
2. Announce the alert via Text-to-Speech (TTS) through your car speakers

## Data Storage and Transmission

- **No data is stored** — alert data exists only in memory for the duration of the notification
- **No data is transmitted** — all processing happens entirely on your device
- **No data is shared** with any third party
- **No analytics or tracking** of any kind
- **Debug logs** (available only in debug builds) use an in-memory ring buffer that is cleared when the app is closed

## Permissions

| Permission | Purpose |
|-----------|---------|
| Notification Access | Read Red Alert notifications to forward them to Android Auto |
| Post Notifications | Display forwarded alerts as notifications on your device and car |

## Your Control

- You can disable notification forwarding at any time from the app's settings
- You can revoke Notification Access at any time via Android Settings → Notifications → Notification access
- Uninstalling the app removes all access immediately

## Children's Privacy

This app is not directed at children under 13 and does not knowingly collect data from children.

## Changes to This Policy

We may update this privacy policy from time to time. Changes will be reflected in the "Last updated" date above.

## Contact

For privacy-related questions, please open an issue at:
https://github.com/yehiyam/redAutoAlert/issues
