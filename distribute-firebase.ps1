param (
    [string]$AppId = "",
    [string]$ReleaseNotes = "",
    [string]$Groups = "",
    [string]$Testers = ""
)

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ConfigFile = "$ScriptDir\firebase.config.json"
$ApkPath = "$ScriptDir\dist\CoCRank-release-latest.apk"

Write-Host "=================================================" -ForegroundColor Cyan
Write-Host "   CoC Rank Mobile - Firebase App Distribution" -ForegroundColor Yellow
Write-Host "=================================================" -ForegroundColor Cyan

# 1. Kiem tra file APK
if (-not (Test-Path $ApkPath)) {
    $ApkPath = "$ScriptDir\app\build\outputs\apk\release\app-release.apk"
}

if (-not (Test-Path $ApkPath)) {
    Write-Host "`nChua tim thay file Release APK. Dang tu dong chay build..." -ForegroundColor Yellow
    powershell -ExecutionPolicy Bypass -File "$ScriptDir\build-release.ps1" -NoOpenFolder
    $ApkPath = "$ScriptDir\dist\CoCRank-release-latest.apk"
}

# 2. Doc cau hinh tu secrets.properties, firebase.config.json hoac bien moi truong
$Secrets = @{}
$SecretsFile = "$ScriptDir\secrets.properties"
if (Test-Path $SecretsFile) {
    Get-Content $SecretsFile | ForEach-Object {
        if ($_ -match "^\s*([^#=]+)\s*=\s*(.*)$") {
            $Secrets[$matches[1].Trim()] = $matches[2].Trim()
        }
    }
}

$Config = @{}
if (Test-Path $ConfigFile) {
    try {
        $Config = Get-Content $ConfigFile -Raw | ConvertFrom-Json
    } catch {}
}

if ([string]::IsNullOrWhiteSpace($AppId)) {
    $AppId = if ($Config.appId) { $Config.appId } else { $env:FIREBASE_APP_ID }
}

if ([string]::IsNullOrWhiteSpace($ReleaseNotes)) {
    $ReleaseNotes = if ($Config.releaseNotes) { $Config.releaseNotes } else { "CoC Rank Mobile - Ban phat hanh tu dong $(Get-Date -Format 'dd/MM/yyyy HH:mm')" }
}

if ([string]::IsNullOrWhiteSpace($Groups)) {
    $Groups = if ($Config.groups) { $Config.groups } else { $env:FIREBASE_GROUPS }
}

if ([string]::IsNullOrWhiteSpace($Testers)) {
    $Testers = if ($Secrets.FIREBASE_TESTERS) { $Secrets.FIREBASE_TESTERS } elseif ($Config.testers) { $Config.testers } else { $env:FIREBASE_TESTERS }
}

# 3. Kiem tra App ID
if ([string]::IsNullOrWhiteSpace($AppId)) {
    Write-Host "`n[!] Chua co Firebase App ID." -ForegroundColor Yellow
    Write-Host "Huong dan lay App ID:" -ForegroundColor Gray
    Write-Host "  1. Vao Firebase Console -> Project settings -> General" -ForegroundColor Gray
    Write-Host "  2. Tim phan 'Your apps' -> Sao chep App ID (dang: 1:xxx:android:xxx)" -ForegroundColor Gray
    $AppId = Read-Host "`nNhap Firebase App ID cua ban"

    if ([string]::IsNullOrWhiteSpace($AppId)) {
        Write-Host "X Loi: Khong co App ID de upload len Firebase." -ForegroundColor Red
        exit 1
    }

    # Luu lai vao firebase.config.json de khong phai nhap lai lan sau
    $ConfigObj = @{
        appId = $AppId.Trim()
        groups = if ($Groups) { $Groups } else { "testers" }
        testers = if ($Testers) { $Testers } else { "" }
        releaseNotes = $ReleaseNotes
    }
    $ConfigObj | ConvertTo-Json -Depth 4 | Set-Content $ConfigFile -Encoding UTF8
    Write-Host "-> Da luu App ID vao firebase.config.json!" -ForegroundColor Green
}

Write-Host "`n[1/2] Dang kiem tra xac thuc Firebase..." -ForegroundColor Yellow
# Kiem tra xem da dang nhap firebase chua
$LoginCheck = & npx.cmd firebase-tools login:list 2>&1
if ($LASTEXITCODE -ne 0 -or ($LoginCheck -notmatch "@")) {
    Write-Host "Chua dang nhap Firebase CLI. Trinh duyet se mo de dang nhap..." -ForegroundColor Yellow
    & npx.cmd firebase-tools login
}

# 4. Upload len Firebase App Distribution
Write-Host "`n[2/2] Dang tai file APK len Firebase App Distribution..." -ForegroundColor Cyan
Write-Host "  - APK: $ApkPath" -ForegroundColor Gray
Write-Host "  - App ID: $AppId" -ForegroundColor Gray

$ArgsList = @("firebase-tools", "appdistribution:distribute", "`"$ApkPath`"", "--app", "`"$AppId`"", "--release-notes", "`"$ReleaseNotes`"")
if (-not [string]::IsNullOrWhiteSpace($Groups)) {
    $ArgsList += @("--groups", "`"$Groups`"")
}
if (-not [string]::IsNullOrWhiteSpace($Testers)) {
    $ArgsList += @("--testers", "`"$Testers`"")
}

& npx.cmd $ArgsList

if ($LASTEXITCODE -eq 0) {
    Write-Host "`n=================================================" -ForegroundColor Cyan
    Write-Host " THANH CONG! Ban build da duoc phat hanh len Firebase App Distribution." -ForegroundColor Green
    Write-Host " Testers se nhan duoc thong bao hoac co the tai ve ngay lap tuc tren app tester." -ForegroundColor Yellow
    Write-Host "=================================================" -ForegroundColor Cyan
} else {
    Write-Host "`nX Lỗi tải lên Firebase App Distribution." -ForegroundColor Red
}
