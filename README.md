# Spam Blocker - Android App

An Android application that intelligently blocks spam calls by scanning the entire screen for caller identification information and matching against user-defined keywords.

## Features

- **Silent Call Detection**: Automatically mutes incoming calls initially
- **Comprehensive Screen Monitoring**: Scans entire screen including call apps and overlay apps like Truecaller
- **Smart Timing**: Waits 5 seconds then performs comprehensive screen scan
- **Keyword-Based Blocking**: Blocks calls when any screen text contains user-defined keywords
- **Universal Compatibility**: Works with any caller ID app or overlay, not just Truecaller
- **Smart Timeout**: Allows calls after 10 seconds if no keywords detected
- **User-Friendly UI**: Simple interface to manage blocked keywords

## How It Works

1. When a call comes in, the app immediately mutes the ringtone
2. The accessibility service waits 5 seconds for caller ID information to appear
3. After 5 seconds, it scans the entire screen (call apps, overlays, notifications, etc.)
4. It searches all visible text for your blocked keywords
5. If a keyword matches: The call is automatically ended
6. If no keyword matches: The ringtone is restored and the call rings normally
7. If no scan completes within 10 seconds: The ringtone is restored (safe default)

## Requirements

- Android 7.0 (API 24) or higher
- Any caller ID app (Truecaller, Google Phone, etc.) or none at all
- Required permissions:
  - Phone state access
  - Call log access
  - Audio settings modification
  - Answer phone calls (Android 8.0+)
  - Accessibility service

## Installation

1. Build the APK using Android Studio or Gradle:
   ```bash
   ./gradlew assembleDebug
   ```

2. Install the APK on your Android device:
   ```bash
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

3. Grant all required permissions when prompted

4. Enable the Accessibility Service:
   - Open Settings → Accessibility
   - Find "Spam Blocker" service
   - Toggle it on

## Usage

1. Open the Spam Blocker app
2. Add keywords that you want to block (e.g., "spam", "telemarketer", "promo", "marketing")
3. Keywords are case-insensitive and will match partial text anywhere on screen
4. The app will automatically monitor incoming calls

### Example Scenarios

**With Truecaller:**
- Call comes in → Muted for 5 seconds → Truecaller shows "Promo Offer" → Keyword "promo" matches → Call blocked

**With Google Phone app:**
- Call comes in → Muted for 5 seconds → Google Phone shows "SPAM LIKELY" → Keyword "spam" matches → Call blocked

**With any caller ID service:**
- Call comes in → Muted for 5 seconds → Screen shows "Marketing Call" → Keyword "marketing" matches → Call blocked

**Without any caller ID:**
- Call comes in → Muted for 5 seconds → No caller info appears → After 10 seconds total → Call allowed

## Technical Architecture

### Components

- **MainActivity**: User interface for managing blocked keywords
- **CallReceiver**: BroadcastReceiver that detects incoming calls and manages audio
- **TruecallerAccessibilityService**: Accessibility service that scans entire screen for caller information
- **KeywordManager**: Manages storage and matching of blocked keywords
- **CallStateManager**: Coordinates call state between components

### Permissions

- `READ_PHONE_STATE`: Detect incoming calls
- `READ_CALL_LOG`: Access call information
- `ANSWER_PHONE_CALLS`: End spam calls (Android 8.0+)
- `MODIFY_AUDIO_SETTINGS`: Mute/unmute ringtone
- `BIND_ACCESSIBILITY_SERVICE`: Read entire screen content during calls

## Limitations

- Accessibility service must remain enabled
- Brief 5-second delay before scanning begins
- May not catch caller info that appears and disappears very quickly
- Some system overlays may not be accessible to the service

## Privacy

- All keywords are stored locally on the device
- No data is transmitted to external servers
- The app only scans screen content during incoming calls
- Screen content is only checked for keyword matches and not stored

## License

This project is open source and available for educational purposes.