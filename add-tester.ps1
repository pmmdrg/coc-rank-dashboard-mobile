param (
    [string[]]$Emails,
    [switch]$SkipDistributeCurrentRelease
)

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ConfigFile = "$ScriptDir\firebase.config.json"
$ApkPath = "$ScriptDir\dist\CoCRank-release-latest.apk"

Write-Host "=================================================" -ForegroundColor Cyan
Write-Host "   CoC Rank Mobile - Them Tester vao Firebase" -ForegroundColor Yellow
Write-Host "=================================================" -ForegroundColor Cyan

# 1. Doc cau hinh firebase.config.json
$Config = @{}
if (Test-Path $ConfigFile) {
    try {
        $Config = Get-Content $ConfigFile -Raw | ConvertFrom-Json
    } catch {}
}

$ProjectId = if ($Config.projectId) { $Config.projectId } else { "coc-rank-mobile" }
$AppId = if ($Config.appId) { $Config.appId } else { "1:337547815951:android:f2f2c3e432ee4fa84c077a" }

# 2. Xac dinh danh sach Email
if ($null -eq $Emails -or $Emails.Count -eq 0) {
    $inputEmail = Read-Host "Nhap dia chi email tester (hoac nhieu email cach nhau boi dau phay)"
    if ([string]::IsNullOrWhiteSpace($inputEmail)) {
        Write-Host "Khong co email nao duoc nhap. Da huy thao tac." -ForegroundColor Yellow
        exit 0
    }
    $Emails = $inputEmail -split "[,;]" | ForEach-Object { $_.Trim() } | Where-Object { $_ -ne "" }
}

$validEmails = @()
foreach ($email in $Emails) {
    $clean = $email.Trim()
    if ($clean -match "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$") {
        $validEmails += $clean
    } else {
        Write-Host "[!] Email '$clean' khong hop le, bo qua." -ForegroundColor Yellow
    }
}

if ($validEmails.Count -eq 0) {
    Write-Host "X Khong co email hop le nao de them." -ForegroundColor Red
    exit 1
}

# 3. Kiem tra dang nhap Firebase
Write-Host "`n[1/3] Kiem tra xac thuc Firebase..." -ForegroundColor Yellow
$LoginCheck = & npx.cmd firebase-tools login:list 2>&1
if ($LASTEXITCODE -ne 0 -or ($LoginCheck -notmatch "@")) {
    Write-Host "Chua dang nhap Firebase CLI. Dang mo trinh duyet dang nhap..." -ForegroundColor Yellow
    & npx.cmd firebase-tools login
}

# 4. Them tester vao Firebase App Distribution Project
Write-Host "`n[2/3] Dang them tester vao du an Firebase ($ProjectId)..." -ForegroundColor Cyan
$emailsArg = $validEmails -join " "
Write-Host "  -> Email: $($validEmails -join ', ')" -ForegroundColor Gray

& npx.cmd firebase-tools appdistribution:testers:add $validEmails --project $ProjectId

if ($LASTEXITCODE -ne 0) {
    Write-Host "X Co loi khi them tester vao project." -ForegroundColor Red
    exit 1
}
Write-Host "-> Da them tester vao project thanh cong!" -ForegroundColor Green

# 5. Cap nhat vao file secrets.properties (git-ignored) de khong lo email len Git
$SecretsFile = "$ScriptDir\secrets.properties"
$existingTesters = @()
if (Test-Path $SecretsFile) {
    Get-Content $SecretsFile | ForEach-Object {
        if ($_ -match "^\s*FIREBASE_TESTERS\s*=\s*(.*)$") {
            $existingTesters = $matches[1] -split "[,;]" | ForEach-Object { $_.Trim() } | Where-Object { $_ -ne "" }
        }
    }
}
$allTesters = ($existingTesters + $validEmails) | Select-Object -Unique
$testersString = $allTesters -join ", "

if (Test-Path $SecretsFile) {
    $content = Get-Content $SecretsFile
    if ($content -match "FIREBASE_TESTERS=") {
        $content = $content -replace "FIREBASE_TESTERS=.*", "FIREBASE_TESTERS=$testersString"
    } else {
        $content += "FIREBASE_TESTERS=$testersString"
    }
    Set-Content -Path $SecretsFile -Value $content -Encoding UTF8
} else {
    Set-Content -Path $SecretsFile -Value "FIREBASE_TESTERS=$testersString" -Encoding UTF8
}
Write-Host "-> Da luu danh sach tester vao secrets.properties (an khoi Git): $testersString" -ForegroundColor Green

# 6. Gui ban build hien tai cho cac tester vua them (neu co san file APK)
if (-not $SkipDistributeCurrentRelease -and (Test-Path $ApkPath)) {
    Write-Host "`n[3/3] Dang cap quyen truy cap ban build hien tai cho tester..." -ForegroundColor Cyan
    $releaseNotes = if ($Config.releaseNotes) { $Config.releaseNotes } else { "CoC Rank Mobile latest release" }
    & npx.cmd firebase-tools appdistribution:distribute "`"$ApkPath`"" --app "`"$AppId`"" --testers "`"$testersString`"" --release-notes "`"$releaseNotes`""
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "`n=================================================" -ForegroundColor Cyan
        Write-Host " THANH CONG! Email moi tai app da duoc gui toi:" -ForegroundColor Green
        foreach ($m in $validEmails) {
            Write-Host "  -> $m" -ForegroundColor Yellow
        }
        Write-Host " Tester co the kiem tra hop thu den va mo App Tester de cai dat!" -ForegroundColor Green
        Write-Host "=================================================" -ForegroundColor Cyan
    }
} else {
    Write-Host "`nTester da duoc them vao he thong va se tu dong nhan duoc cac ban build tiep theo." -ForegroundColor Green
}
