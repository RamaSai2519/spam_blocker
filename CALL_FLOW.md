# Call Flow Visualization

## Complete Call Processing Flow

```
┌─────────────────────────────────────────────────────────────────┐
│                     INCOMING CALL SCENARIO                      │
└─────────────────────────────────────────────────────────────────┘

[Phone Rings]
     │
     ↓
┌────────────────────────────────┐
│   CallReceiver.onReceive()     │
│   - Detects PHONE_STATE        │
│   - State = RINGING             │
└────────────┬───────────────────┘
             │
             ↓
┌────────────────────────────────┐
│   CallReceiver.onIncomingCall() │
│   1. Save current ringer mode  │
│   2. Set RINGER_MODE_SILENT    │
│   3. Set isCallActive = true   │
└────────────┬───────────────────┘
             │
             ↓
┌─────────────────────────────────────────────┐
│   CallStateManager.setCallIncoming(true)    │
│   - Set call start time                     │
│   - Set hasCheckedCaller = false            │
└─────────────────────┬───────────────────────┘
                      │
    ┌─────────────────┴─────────────────┐
    │                                   │
    ↓                                   ↓
[TRUECALLER PATH]              [TIMEOUT PATH]
                                        │
┌─────────────────────────────┐        │
│ Truecaller displays name    │        │
│ within ~5 seconds            │        │
└──────────┬──────────────────┘        │
           │                           │
           ↓                           │
┌────────────────────────────────┐    │
│ TruecallerAccessibilityService │    │
│ - onAccessibilityEvent()       │    │
│ - Detects Truecaller window    │    │
└──────────┬─────────────────────┘    │
           │                           │
           ↓                           │
┌────────────────────────────────┐    │
│ extractCallerName()            │    │
│ - Parse AccessibilityNodeInfo  │    │
│ - Filter UI elements           │    │
│ - Return caller name           │    │
└──────────┬─────────────────────┘    │
           │                           │
           ↓                           │
┌────────────────────────────────┐    │
│ processCallerName(name)        │    │
│ - Set checkedCaller = true     │    │
│ - Cancel timeout               │    │
└──────────┬─────────────────────┘    │
           │                           │
           ↓                           │
┌────────────────────────────────┐    │
│ KeywordManager.containsKeyword()│   │
│ - Convert to lowercase         │    │
│ - Check each keyword           │    │
└──────────┬─────────────────────┘    │
           │                           │
      ┌────┴────┐                      │
      │         │                      │
   MATCH      NO MATCH                 │
      │         │                      │
      ↓         ↓                      ↓
┌──────────┐ ┌──────────┐    ┌─────────────────┐
│  BLOCK   │ │  ALLOW   │    │ TIMEOUT (6 sec) │
└────┬─────┘ └────┬─────┘    └────────┬────────┘
     │            │                    │
     │            │                    │
     ↓            ↓                    ↓
┌─────────────────────────────────────────────┐
│           CallReceiver.endCall()            │
│           - Use TelecomManager (API 28+)    │
│           - Or reflection for older APIs    │
│           - Disconnect the call             │
└─────────────────────────────────────────────┘
                      OR
┌─────────────────────────────────────────────┐
│        CallReceiver.unmuteRinger()          │
│        - Restore previous ringer mode       │
│        - Call starts ringing               │
└─────────────────────────────────────────────┘

[Call Answered/Ended]
     │
     ↓
┌────────────────────────────────┐
│   CallReceiver.onCallEnded()   │
│   - State = IDLE                │
│   - Restore ringer mode         │
│   - Set isCallActive = false    │
└────────────┬───────────────────┘
             │
             ↓
┌─────────────────────────────────┐
│   CallStateManager.reset()      │
│   - Clear all state             │
│   - Reset timers                │
└─────────────────────────────────┘
```

## State Transitions

```
┌─────────────┐
│    IDLE     │ ← Initial state
└──────┬──────┘
       │ Phone rings
       ↓
┌─────────────┐
│  INCOMING   │ ← Call detected, ringer muted
│  SILENT     │
└──────┬──────┘
       │ Truecaller shows name OR timeout
       ↓
    ┌──┴──┐
    │     │
┌───┴──┐ ┌┴─────┐
│BLOCK │ │ALLOW │ ← Decision made
└───┬──┘ └┬─────┘
    │     │ Ringer unmuted
    │     ↓
    │  ┌──────────┐
    │  │ RINGING  │ ← User hears ring
    │  └────┬─────┘
    │       │
    └───────┴─────── Call answered/declined
            ↓
       ┌─────────┐
       │  IDLE   │ ← Back to initial state
       └─────────┘
```

## Timing Diagram

```
Time  Event                           Ringer Status    Screen
────  ──────────────────────────────  ──────────────  ────────────
0.0s  Call arrives                    SILENT          Phone app
0.1s  CallReceiver mutes ringer       SILENT          Phone app
1.0s  Truecaller launches              SILENT          Truecaller
2.0s  Truecaller loading...            SILENT          Truecaller
4.0s  Truecaller displays name        SILENT          Truecaller
4.1s  AccessibilityService reads name SILENT          Truecaller
4.2s  Keyword check performed         SILENT          Truecaller
      
      ┌─ IF SPAM ────────────────────────────────────────────┐
      │ 4.3s  Call ended                   SILENT  Call ended │
      └──────────────────────────────────────────────────────┘
      
      ┌─ IF NOT SPAM ────────────────────────────────────────┐
      │ 4.3s  Ringer unmuted              RINGING  Ringing...│
      │ 5.0s  User sees caller ID          RINGING  Ringing...│
      │ 8.0s  User answers/declines        NORMAL   Call/Idle│
      └──────────────────────────────────────────────────────┘
      
      ┌─ IF TIMEOUT ─────────────────────────────────────────┐
      │ 6.0s  Timeout reached              RINGING  Ringing...│
      │ 7.0s  User sees caller ID          RINGING  Ringing...│
      │ 10.0s User answers/declines        NORMAL   Call/Idle│
      └──────────────────────────────────────────────────────┘
```

## Component Communication

```
┌──────────────┐           ┌──────────────────┐
│  MainActivity│           │  KeywordManager  │
│              │◄─────────►│  (Shared Prefs)  │
└──────────────┘           └──────────────────┘
                                    ▲
                                    │
                           Reads keywords
                                    │
┌──────────────┐           ┌───────┴──────────┐
│ CallReceiver │           │  Truecaller      │
│              │           │  Accessibility   │
│  - Mute      │           │  Service         │
│  - Unmute    │           │                  │
│  - End Call  │◄──────────┤  - Read UI       │
└──────┬───────┘   Trigger │  - Check spam    │
       │                   └──────────────────┘
       │                            ▲
       │                            │
       │    ┌───────────────────┐   │
       └───►│ CallStateManager  │◄──┘
            │                   │
            │ - State tracking  │
            │ - Coordination    │
            │ - Timing          │
            └───────────────────┘
```

## Decision Tree

```
                    Call Arrives
                         │
                         ↓
                    Mute Ringer
                         │
                         ↓
            Is Accessibility Enabled?
                    ╱         ╲
                  YES          NO
                  ╱              ╲
                 ↓                ↓
        Monitor Truecaller    Wait 6 sec
                 │                │
                 ↓                ↓
        Name Found?           Unmute
            ╱      ╲             │
          YES       NO            ↓
          ╱          ╲        Allow Call
         ↓            ↓
    Contains        Wait
    Keyword?        6 sec
      ╱    ╲          │
    YES     NO        ↓
     ╱       ╲     Unmute
    ↓         ↓       │
Block     Allow      ↓
Call      Call    Allow Call
```
