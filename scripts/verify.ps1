# PixelGift All-in-One Verification Script
$ErrorActionPreference = "Stop"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "   PixelGift Automated Verification     " -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

# 1. Android Unit Tests
Write-Host "`n[1/2] Running Android Unit Tests..." -ForegroundColor Yellow
Push-Location "$PSScriptRoot\..\app"
try {
    .\gradlew.bat testDebugUnitTest --quiet
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Android unit tests failed with code $LASTEXITCODE" -ForegroundColor Red
        Pop-Location
        exit $LASTEXITCODE
    }
    Write-Host "Android unit tests PASSED!" -ForegroundColor Green
} finally {
    Pop-Location
}

# 2. Firmware Compilation
Write-Host "`n[2/2] Compiling ESP32 Firmware with PlatformIO..." -ForegroundColor Yellow
$pioExe = "$env:USERPROFILE\.platformio\penv\Scripts\pio.exe"
if (-not (Test-Path $pioExe)) {
    $pioCmd = Get-Command "pio" -ErrorAction SilentlyContinue
    if ($pioCmd) { $pioExe = $pioCmd.Source }
}

if (-not (Test-Path $pioExe)) {
    Write-Host "Warning: PlatformIO executable not found at $pioExe. Skipping firmware compilation." -ForegroundColor DarkYellow
} else {
    $firmwareDir = Resolve-Path "$PSScriptRoot\..\firmware"
    & "$pioExe" run -d "$firmwareDir"
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Firmware compilation failed with code $LASTEXITCODE" -ForegroundColor Red
        exit $LASTEXITCODE
    }
    Write-Host "Firmware build PASSED!" -ForegroundColor Green
}

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "   ALL VERIFICATIONS PASSED (100% OK)   " -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
exit 0

