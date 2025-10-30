# Spam Blocker - Android App

An Android application that intelligently blocks spam calls by monitoring Truecaller's caller identification and matching against user-defined keywords.

## Features

- **Silent Call Detection**: Automatically mutes incoming calls initially
- **Truecaller Integration**: Reads caller names displayed by Truecaller using Accessibility Service
- **Keyword-Based Blocking**: Blocks calls when caller name contains user-defined keywords
- **Smart Timeout**: Allows calls after 6 seconds if no caller name is detected
- **User-Friendly UI**: Simple interface to manage blocked keywords

## How It Works

1. When a call comes in, the app immediately mutes the ringtone
2. The accessibility service monitors Truecaller for the caller's name (typically appears within 5 seconds)
3. Once the name appears, it checks against your blocked keywords
4. If a keyword matches: The call is automatically ended
5. If no keyword matches: The ringtone is restored and the call rings normally
6. If no name appears within 6 seconds: The ringtone is restored (safe default)

## Requirements

- Android 7.0 (API 24) or higher
- Truecaller app installed on the device
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
2. Add keywords that you want to block (e.g., "spam", "telemarketer", "promo")
3. Keywords are case-insensitive and will match partial text
4. The app will automatically monitor incoming calls

## Technical Architecture

### Components

- **MainActivity**: User interface for managing blocked keywords
- **CallReceiver**: BroadcastReceiver that detects incoming calls and manages audio
- **TruecallerAccessibilityService**: Accessibility service that reads Truecaller's UI
- **KeywordManager**: Manages storage and matching of blocked keywords
- **CallStateManager**: Coordinates call state between components

### Permissions

- `READ_PHONE_STATE`: Detect incoming calls
- `READ_CALL_LOG`: Access call information
- `ANSWER_PHONE_CALLS`: End spam calls (Android 8.0+)
- `MODIFY_AUDIO_SETTINGS`: Mute/unmute ringtone
- `BIND_ACCESSIBILITY_SERVICE`: Read Truecaller's displayed caller name

## Limitations

- Requires Truecaller to be installed and active
- Truecaller must display the caller name for blocking to work
- Accessibility service must remain enabled
- May not work with all Truecaller versions if UI changes significantly

## Privacy

- All keywords are stored locally on the device
- No data is transmitted to external servers
- The app only reads Truecaller's UI during incoming calls

## License

This project is open source and available for educational purposes.