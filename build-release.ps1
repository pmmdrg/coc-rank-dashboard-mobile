param (
    [switch]$SkipTest,
    [switch]$NoOpenFolder,
    [switch]$UploadFirebase
)

$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path

Write-Host "=================================================" -ForegroundColor Cyan
Write-Host "   CoC Rank Mobile - Automated Release Pipeline" -ForegroundColor Yellow
Write-Host "=================================================" -ForegroundColor Cyan

# 1. Chay Unit Tests tu dong
if (-not $SkipTest) {
    Write-Host "`n[1/3] Dang chay kiem thu tu dong (Unit Tests)..." -ForegroundColor Yellow
    & "$ScriptDir\gradlew.bat" testReleaseUnitTest --quiet
    if ($LASTEXITCODE -ne 0) {
        Write-Host "[X] Loi: Unit tests that bai! Huy qua trinh build release." -ForegroundColor Red
        exit $LASTEXITCODE
    }
    Write-Host "  -> Unit tests PASSED 100%!" -ForegroundColor Green
} else {
    Write-Host "`n[1/3] Da bo qua Unit Tests (SkipTest flag)." -ForegroundColor Gray
}

# 2. Build Signed Release APK
Write-Host "`n[2/3] Dang bien dich va ky so ban Release APK..." -ForegroundColor Yellow
& "$ScriptDir\gradlew.bat" assembleRelease --quiet
if ($LASTEXITCODE -ne 0) {
    Write-Host "[X] Loi bien dich ban Release APK." -ForegroundColor Red
    exit $LASTEXITCODE
}
Write-Host "  -> Bien dich Signed Release APK THANH CONG!" -ForegroundColor Green

# 3. Xuat file APK ra thu muc dist/ (Ghi de thay the file cu)
$ReleaseApkSrc = "$ScriptDir\app\build\outputs\apk\release\app-release.apk"
$DistDir = "$ScriptDir\dist"
if (-not (Test-Path $DistDir)) {
    New-Item -ItemType Directory -Path $DistDir -Force | Out-Null
}

$TargetLatestApk = "$DistDir\CoCRank-release-latest.apk"
Copy-Item -Path $ReleaseApkSrc -Destination $TargetLatestApk -Force

$SizeMb = [math]::Round(((Get-Item $TargetLatestApk).Length / 1MB), 2)

Write-Host "`n[3/3] Xuat file hoan tat (Da ghi de thay the file cu)!" -ForegroundColor Green
Write-Host "  - File APK: $TargetLatestApk ($SizeMb MB)" -ForegroundColor Cyan

Write-Host "`n=================================================" -ForegroundColor Cyan
Write-Host " HOAN TAT! File APK da san sang de test/cai dat." -ForegroundColor Green
Write-Host "=================================================" -ForegroundColor Cyan

if ($UploadFirebase) {
    Write-Host "`nDang chuyen tiep lenh day len Firebase App Distribution..." -ForegroundColor Cyan
    powershell -ExecutionPolicy Bypass -File "$ScriptDir\distribute-firebase.ps1"
} elseif (-not $NoOpenFolder) {
    explorer.exe "/select,`"$TargetLatestApk`""
}
