#!/usr/bin/env pwsh
# =============================================================================
# MediConnect — Email Test Script
# Fires all 13 email templates to a target inbox in one shot.
#
# Usage:
#   .\test-emails.ps1
#   .\test-emails.ps1 -To "other@gmail.com"
#   .\test-emails.ps1 -BackendUrl "http://localhost:8082"
# =============================================================================

param(
    [string]$To         = "takrounipc@gmail.com",
    [string]$BackendUrl = "http://localhost:8082",
    [string]$ContextPath = "/mediconnect"
)

$baseUrl   = "$BackendUrl$ContextPath"
$testUrl   = "$baseUrl/api/dev/email-test?to=$To"
$healthUrl = "$baseUrl/actuator/health"

$Green  = "Green"
$Red    = "Red"
$Yellow = "Yellow"
$Cyan   = "Cyan"
$White  = "White"

function Write-Header {
    Write-Host ""
    Write-Host "╔══════════════════════════════════════════════════════════╗" -ForegroundColor $Cyan
    Write-Host "║          MediConnect — Email Integration Test            ║" -ForegroundColor $Cyan
    Write-Host "╚══════════════════════════════════════════════════════════╝" -ForegroundColor $Cyan
    Write-Host ""
}

function Check-MailConfig {
    $propsFile = Join-Path $PSScriptRoot "src\main\resources\application.properties"
    if (Test-Path $propsFile) {
        $content = Get-Content $propsFile -Raw
        if ($content -notmatch "spring\.mail\.username\s*=\s*.+@") {
            Write-Host "⚠️  WARNING: spring.mail.username not configured in application.properties" -ForegroundColor $Yellow
            Write-Host "   Emails will be silently skipped by EmailService.isMailConfigured()." -ForegroundColor $Yellow
            Write-Host ""
            Write-Host "   Add these lines to application.properties (or your .env file):" -ForegroundColor $White
            Write-Host ""
            Write-Host "   spring.mail.host=smtp.gmail.com" -ForegroundColor $White
            Write-Host "   spring.mail.port=587" -ForegroundColor $White
            Write-Host "   spring.mail.username=YOUR_GMAIL@gmail.com" -ForegroundColor $White
            Write-Host "   spring.mail.password=YOUR_APP_PASSWORD" -ForegroundColor $White
            Write-Host "   spring.mail.properties.mail.smtp.auth=true" -ForegroundColor $White
            Write-Host "   spring.mail.properties.mail.smtp.starttls.enable=true" -ForegroundColor $White
            Write-Host "   mediconnect.mail.from-name=MediConnect" -ForegroundColor $White
            Write-Host "   mediconnect.mail.from-address=YOUR_GMAIL@gmail.com" -ForegroundColor $White
            Write-Host ""
            Write-Host "   → For Gmail: use an App Password (not your real password)." -ForegroundColor $Yellow
            Write-Host "   → Enable 2FA on your Google account, then generate an App Password at:" -ForegroundColor $Yellow
            Write-Host "     https://myaccount.google.com/apppasswords" -ForegroundColor $Cyan
            Write-Host ""

            $answer = Read-Host "Continue anyway (emails will be skipped by the server)? [y/N]"
            if ($answer -notmatch "^[yY]") {
                Write-Host "Aborted. Configure mail settings first." -ForegroundColor $Red
                exit 1
            }
        } else {
            Write-Host "✅ Mail configuration found in application.properties" -ForegroundColor $Green
        }
    }
}

function Wait-ForBackend {
    Write-Host "🔍 Checking backend is reachable at $baseUrl ..." -ForegroundColor $Cyan

    $maxAttempts = 10
    for ($i = 1; $i -le $maxAttempts; $i++) {
        try {
            $resp = Invoke-WebRequest -Uri $healthUrl -TimeoutSec 3 -UseBasicParsing -ErrorAction Stop
            if ($resp.StatusCode -eq 200) {
                Write-Host "✅ Backend is UP" -ForegroundColor $Green
                return
            }
        } catch {
            Write-Host "   Attempt $i/$maxAttempts — backend not ready yet, waiting 3s..." -ForegroundColor $Yellow
            Start-Sleep -Seconds 3
        }
    }

    # If no actuator, just try the test endpoint directly
    Write-Host "⚠️  Could not reach /actuator/health — trying to fire test anyway..." -ForegroundColor $Yellow
}

function Fire-EmailTest {
    Write-Host ""
    Write-Host "🚀 Firing all email templates to: $To" -ForegroundColor $Cyan
    Write-Host "   POST $testUrl" -ForegroundColor $White
    Write-Host ""

    try {
        $response = Invoke-RestMethod `
            -Method POST `
            -Uri $testUrl `
            -ContentType "application/json" `
            -TimeoutSec 30 `
            -ErrorAction Stop

        Write-Host "╔══════════════════════════════════════════════════════════╗" -ForegroundColor $Green
        Write-Host "║                    ✅  SUCCESS                           ║" -ForegroundColor $Green
        Write-Host "╚══════════════════════════════════════════════════════════╝" -ForegroundColor $Green
        Write-Host ""
        Write-Host "  📬 Destination  : $($response.destination)" -ForegroundColor $White
        Write-Host "  📧 Emails queued: $($response.emailsQueued)" -ForegroundColor $White
        Write-Host "  ℹ️  Status       : $($response.status)" -ForegroundColor $White
        Write-Host "  🔒 Note         : $($response.note)" -ForegroundColor $Yellow
        Write-Host ""
        Write-Host "══════════════════════════════════════════════════════════" -ForegroundColor $Cyan
        Write-Host "  What to check in your inbox ($To):" -ForegroundColor $Cyan
        Write-Host "══════════════════════════════════════════════════════════" -ForegroundColor $Cyan
        Write-Host ""
        Write-Host "   1.  📧 Verification Code             (6-digit code)" -ForegroundColor $White
        Write-Host "   2.  👋 Welcome                       (account created)" -ForegroundColor $White
        Write-Host "   3.  ✅ Doctor Approved               (green header)" -ForegroundColor $White
        Write-Host "   4.  ❌ Doctor Rejected               (with reason)" -ForegroundColor $White
        Write-Host "   5.  🎉 Participation Confirmed       (CTA button)" -ForegroundColor $White
        Write-Host "   6.  ⏳ Participation Waitlist        (amber theme)" -ForegroundColor $White
        Write-Host "   7.  🌟 Promoted from Waitlist        (blue theme)" -ForegroundColor $White
        Write-Host "   8.  🩺 Guest Invitation              (dark premium)" -ForegroundColor $White
        Write-Host "   9.  ❌ Event Cancelled               (red theme)" -ForegroundColor $White
        Write-Host "  10.  ⏰ 30-min Reminder               (purple/urgent)" -ForegroundColor $White
        Write-Host "  11.  🚀 Event Started — LIVE          (dark + red dot)" -ForegroundColor $White
        Write-Host "  12.  🗓️  Appointment Reminder          " -ForegroundColor $White
        Write-Host "  13.  🔄 Doctor Status Change          " -ForegroundColor $White
        Write-Host ""
        Write-Host "  ⏱️  Emails are sent ASYNC — check your inbox in ~5 seconds." -ForegroundColor $Yellow
        Write-Host ""

    } catch {
        $statusCode = $_.Exception.Response.StatusCode.value__
        $body       = $_.ErrorDetails.Message

        Write-Host "╔══════════════════════════════════════════════════════════╗" -ForegroundColor $Red
        Write-Host "║                    ❌  FAILED                           ║" -ForegroundColor $Red
        Write-Host "╚══════════════════════════════════════════════════════════╝" -ForegroundColor $Red
        Write-Host ""
        Write-Host "  HTTP Status : $statusCode" -ForegroundColor $Red
        Write-Host "  Error       : $($_.Exception.Message)" -ForegroundColor $Red
        if ($body) {
            Write-Host "  Body        : $body" -ForegroundColor $Red
        }
        Write-Host ""
        Write-Host "  Possible causes:" -ForegroundColor $Yellow
        Write-Host "   • Backend is not running (start with: .\mvnw.cmd spring-boot:run)" -ForegroundColor $Yellow
        Write-Host "   • Context path mismatch — check server.servlet.context-path in application.properties" -ForegroundColor $Yellow
        Write-Host "   • /api/dev/* is blocked by security config" -ForegroundColor $Yellow
        Write-Host ""
        exit 1
    }
}

# ── Main ──────────────────────────────────────────────────────────────────────
Write-Header
Check-MailConfig
Wait-ForBackend
Fire-EmailTest
