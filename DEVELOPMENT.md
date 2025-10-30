# Development Guide

## Building the App

### Prerequisites
- Android Studio Arctic Fox (2020.3.1) or later
- Android SDK with API level 34
- Java Development Kit (JDK) 8 or higher

### Build Steps

1. **Open in Android Studio**
   ```
   File → Open → Select project directory
   ```

2. **Sync Gradle**
   - Android Studio will automatically sync Gradle dependencies
   - If not, click "Sync Project with Gradle Files"

3. **Build APK**
   ```
   Build → Build Bundle(s) / APK(s) → Build APK(s)
   ```
   Or via command line:
   ```bash
   ./gradlew assembleDebug
   ```

4. **Find the APK**
   ```
   app/build/outputs/apk/debug/app-debug.apk
   ```

## Installation on Device

### Via ADB (Android Debug Bridge)
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Via File Transfer
1. Transfer the APK to your Android device
2. Open the file on your device
3. Allow installation from unknown sources if prompted
4. Install the app

## Configuration

### Step 1: Grant Permissions
On first launch, grant the following permissions:
- Read Phone State
- Read Call Log
- Modify Audio Settings
- Answer Phone Calls (Android 8.0+)

### Step 2: Enable Accessibility Service
1. Tap "Enable Accessibility Service" button in the app
2. Find "Spam Blocker" in the list
3. Toggle it ON
4. Confirm the security warning

### Step 3: Add Blocked Keywords
1. Type a keyword in the text field (e.g., "spam", "marketing", "promo")
2. Tap "Add Keyword"
3. The keyword will appear in the list below
4. To remove a keyword, tap the delete icon next to it

## Testing

### Manual Testing
1. Have someone call you from a number not in your contacts
2. Ensure Truecaller is installed and active
3. The ringtone should be muted initially
4. Truecaller should display the caller name
5. If the name contains a blocked keyword, the call will be ended
6. If not, the ringtone will start playing

### Testing Tips
- Add a test keyword like "test" or "spam"
- Have a friend whose number shows these words in Truecaller call you
- Verify the call is blocked
- Remove the keyword and test again to verify calls are allowed

## Troubleshooting

### App not blocking calls
- Verify Accessibility Service is enabled (Settings → Accessibility)
- Check that all permissions are granted
- Ensure Truecaller is installed and active
- Check Android logs: `adb logcat | grep Truecaller`

### Ringtone not working
- Check that Audio permissions are granted
- Verify the ringer mode is not set to silent in system settings
- Test by temporarily disabling the Accessibility Service

### Accessibility Service crashes
- Check Android logs: `adb logcat | grep TruecallerAccessibility`
- Disable and re-enable the service
- Restart the device

## Code Structure

```
app/src/main/java/com/spam_blocker/
├── MainActivity.java                      # Main UI
├── CallReceiver.java                      # Phone state listener
├── CallStateManager.java                  # State coordination
├── TruecallerAccessibilityService.java    # Truecaller monitoring
├── KeywordManager.java                    # Keyword storage
└── KeywordAdapter.java                    # RecyclerView adapter

app/src/main/res/
├── layout/
│   ├── activity_main.xml                  # Main screen layout
│   └── item_keyword.xml                   # Keyword list item
├── values/
│   └── strings.xml                        # String resources
└── xml/
    └── accessibility_service_config.xml   # Accessibility config
```

## Known Limitations

1. **Truecaller Dependency**: The app requires Truecaller to display caller information
2. **UI Changes**: If Truecaller updates its UI significantly, the accessibility service may need updates
3. **Delay**: There's a brief (~5 second) window where calls are silent
4. **Android Restrictions**: Some OEMs may restrict accessibility services or background operations

## Future Enhancements

- Add whitelist functionality for always-allowed numbers
- Implement pattern matching (regex) for keywords
- Add call blocking statistics
- Create notification history for blocked calls
- Support for other caller ID apps
- Cloud backup for keywords
