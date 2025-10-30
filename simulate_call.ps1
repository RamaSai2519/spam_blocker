#!/usr/bin/env powershell

# Script to simulate incoming call on Android emulator
$emulatorHost = "localhost"
$emulatorPort = 5554
$authToken = "wMGSn8ZPS6YLDopp"
$phoneNumber = "+919398036558"

try {
    # Connect to emulator console
    $client = New-Object System.Net.Sockets.TcpClient($emulatorHost, $emulatorPort)
    $stream = $client.GetStream()
    $writer = New-Object System.IO.StreamWriter($stream)
    $reader = New-Object System.IO.StreamReader($stream)
    
    # Read welcome message
    Start-Sleep -Milliseconds 500
    $welcome = $reader.ReadLine()
    Write-Host "Emulator Console: $welcome"
    
    # Authenticate
    $writer.WriteLine("auth $authToken")
    $writer.Flush()
    Start-Sleep -Milliseconds 500
    $authResponse = $reader.ReadLine()
    Write-Host "Auth Response: $authResponse"
    
    # Send GSM call command
    $writer.WriteLine("gsm call $phoneNumber")
    $writer.Flush()
    Start-Sleep -Milliseconds 500
    $callResponse = $reader.ReadLine()
    Write-Host "Call Response: $callResponse"
    
    Write-Host "Incoming call simulated successfully!"
    Write-Host "Phone number: $phoneNumber"
    
    # Close connection
    $writer.WriteLine("quit")
    $writer.Flush()
    $client.Close()
    
} catch {
    Write-Error "Failed to simulate call: $($_.Exception.Message)"
} finally {
    if ($client) { $client.Close() }
}