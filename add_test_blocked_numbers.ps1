# Test script to add blocked numbers for testing
# This script simulates adding blocked numbers to test the functionality

# Add some test blocked numbers
adb shell am broadcast -a com.spam_blocker.TEST_ADD_BLOCKED_NUMBER --es phone_number "+1234567890" --es reason "Test blocked number" --es caller_info "SPAM LIKELY"
adb shell am broadcast -a com.spam_blocker.TEST_ADD_BLOCKED_NUMBER --es phone_number "+9876543210" --es reason "Keyword match: telemarketer" --es caller_info "Unknown Caller - Telemarketer"
adb shell am broadcast -a com.spam_blocker.TEST_ADD_BLOCKED_NUMBER --es phone_number "+5555555555" --es reason "Keyword match: promo" --es caller_info "Promotional Call"

Write-Host "Test blocked numbers added!"
Write-Host "Open the app and click 'View Blocked Numbers' to see them."