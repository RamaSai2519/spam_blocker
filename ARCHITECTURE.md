# Architecture Documentation

## Overview

The Spam Blocker app uses a combination of Android system services and accessibility features to monitor incoming calls and block spam based on Truecaller's caller identification.

## Component Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                        MainActivity                          │
│  - User Interface for managing keywords                      │
│  - Permission requests                                       │
│  - Accessibility service setup                               │
└────────────────────┬────────────────────────────────────────┘
                     │
                     │ manages
                     ↓
            ┌────────────────────┐
            │  KeywordManager    │
            │  - SharedPreferences│
            │  - Keyword storage  │
            │  - Match logic      │
            └────────────────────┘
                     ↑
                     │ uses
                     │
    ┌────────────────┴──────────────────────┐
    │                                       │
┌───┴──────────────────┐     ┌─────────────┴──────────────┐
│   CallReceiver       │     │ TruecallerAccessibility   │
│   - Phone state      │     │ Service                    │
│   - Mute audio       │     │ - Read Truecaller UI       │
│   - End calls        │     │ - Extract caller name      │
└───┬──────────────────┘     └─────────────┬──────────────┘
    │                                      │
    │         ┌─────────────────────┐      │
    └────────→│  CallStateManager   │←─────┘
              │  - Coordinates state │
              │  - Timing management │
              └─────────────────────┘
```

## Call Flow Sequence

### 1. Incoming Call Detection
```
Phone rings
    ↓
CallReceiver.onReceive() triggered with PHONE_STATE action
    ↓
State = RINGING detected
    ↓
CallReceiver.onIncomingCall()
    - Save current ringer mode
    - Set ringer to SILENT
    - Notify CallStateManager: call incoming
```

### 2. Truecaller Name Detection
```
Truecaller displays caller info
    ↓
TruecallerAccessibilityService.onAccessibilityEvent()
    ↓
Check: Is call incoming? YES
Check: Already processed? NO
Check: Within timeout (6s)? YES
    ↓
Extract caller name from Truecaller UI
    ↓
Parse AccessibilityNodeInfo tree for text content
    ↓
Filter out UI elements (buttons, phone numbers)
    ↓
Return potential caller name
```

### 3. Spam Check
```
Caller name extracted: "John Marketing"
    ↓
KeywordManager.containsKeyword("John Marketing")
    ↓
Check each keyword in lowercase
    - "spam" → NO
    - "marketing" → YES (MATCH!)
    ↓
CallStateManager.setCheckedCaller(true)
    ↓
Decision: SPAM DETECTED
    ↓
CallReceiver.endCall()
    - Use TelecomManager (Android 9+) or reflection
    - Disconnect the call
```

### 4. Allow Call Path
```
Caller name extracted: "John Doe"
    ↓
KeywordManager.containsKeyword("John Doe")
    ↓
Check each keyword in lowercase
    - "spam" → NO
    - "marketing" → NO
    - (no matches)
    ↓
Decision: ALLOW CALL
    ↓
CallReceiver.unmuteRinger()
    - Restore previous ringer mode
    - Call starts ringing
```

### 5. Timeout Scenario
```
6 seconds elapsed
    ↓
TruecallerAccessibilityService timeout handler
    ↓
Check: Caller name found? NO
Check: Already processed? NO
    ↓
Decision: TIMEOUT - ALLOW BY DEFAULT
    ↓
CallReceiver.unmuteRinger()
    - Safety mechanism to prevent missed calls
```

## Key Design Decisions

### 1. Silent-by-Default Approach
**Decision**: Mute calls immediately when they arrive.

**Rationale**:
- Prevents spam ringtones from disturbing the user
- Gives time for Truecaller to display caller info
- Can unmute if call is legitimate

**Alternative Considered**: Let ring normally, then mute if spam detected
- Rejected: User would hear initial ring from spam calls

### 2. 6-Second Timeout
**Decision**: Allow calls after 6 seconds if no name detected.

**Rationale**:
- Truecaller typically displays names within 5 seconds
- 1-second buffer for processing
- Prevents legitimate calls from being permanently silent
- Better to allow a spam call than block a legitimate one

### 3. Accessibility Service for Truecaller
**Decision**: Use AccessibilityService instead of direct API.

**Rationale**:
- Truecaller doesn't provide public API
- Accessibility service can read on-screen content
- Works with current Truecaller versions
- No need for root access

**Risk**: May break if Truecaller changes UI significantly

### 4. Keyword-Based Matching
**Decision**: Simple substring matching (case-insensitive).

**Rationale**:
- Easy for users to understand
- Works for common spam patterns ("marketing", "promo", etc.)
- Low false-positive rate with careful keyword selection
- Efficient performance

**Future Enhancement**: Regex patterns for advanced users

### 5. State Management Pattern
**Decision**: Singleton CallStateManager for coordination.

**Rationale**:
- Multiple components need shared state
- BroadcastReceiver and AccessibilityService are separate processes
- Singleton ensures consistent state
- Simple timeout management

## Security Considerations

### 1. Permissions
- **READ_PHONE_STATE**: Essential for detecting calls
- **ANSWER_PHONE_CALLS**: Required to end spam calls (Android 8.0+)
- **MODIFY_AUDIO_SETTINGS**: Needed to mute/unmute ringer
- **BIND_ACCESSIBILITY_SERVICE**: Required to read Truecaller UI

All permissions are justified and minimal for functionality.

### 2. Data Privacy
- Keywords stored locally using SharedPreferences
- No network communication
- No external data sharing
- Only reads Truecaller during active calls

### 3. Accessibility Service Security
- Limited to Truecaller package only
- Only active during incoming calls
- Minimal data extraction (just caller name)
- No logging of sensitive information

## Performance Considerations

### 1. Memory Usage
- Lightweight: No heavy background processing
- SharedPreferences for simple storage
- No database overhead
- Minimal object allocation

### 2. Battery Impact
- Services only active during calls
- No continuous background operations
- Event-driven architecture
- Quick processing (< 100ms for keyword check)

### 3. Responsiveness
- Immediate audio muting (< 100ms)
- Fast keyword matching with Set data structure
- Asynchronous call ending
- No UI blocking operations

## Error Handling

### 1. Permissions Not Granted
- UI indicates missing permissions
- Graceful degradation: app continues to work for granted permissions
- Clear user guidance to enable permissions

### 2. Accessibility Service Disabled
- Status indicator in main UI
- Direct link to accessibility settings
- App continues to work (keyword management)

### 3. Truecaller Not Installed
- App still functional for keyword management
- Timeout mechanism ensures calls aren't permanently silent
- User can still use manually

### 4. Call End Failure
- Fallback to older APIs via reflection
- Logged for debugging
- Call remains silent (user can manually decline)

## Testing Strategy

### Unit Testing (Future)
- KeywordManager: Storage and matching logic
- CallStateManager: State transitions and timing

### Integration Testing (Manual)
- Complete call flow with various scenarios
- Permission granting flows
- UI interactions

### Device Testing
- Multiple Android versions (7.0+)
- Different OEM customizations
- Various Truecaller versions

## Maintenance & Updates

### Monitoring Points
1. Android OS updates affecting permissions
2. Truecaller UI/UX changes
3. Accessibility API changes
4. New telephony APIs

### Update Strategy
- Regular testing with Truecaller updates
- Monitor crash reports
- Community feedback on compatibility
- Version-specific workarounds if needed
