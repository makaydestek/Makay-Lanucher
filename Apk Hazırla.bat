@echo off
setlocal EnableExtensions EnableDelayedExpansion
title Makay Launcher - APK derle ve GitHub'a yayinla
cd /d "%~dp0"

set "PATH=%ProgramFiles%\GitHub CLI;%LocalAppData%\Programs\GitHub CLI;%PATH%"
set "REPO=makaydestek/Makay-Lanucher"

if not exist "APK" mkdir "APK"

echo ============================================================
echo  Makay Launcher - derle + GitHub Release
echo  Repo: https://github.com/makaydestek/Makay-Lanucher
echo ============================================================
echo.

REM ============================================================
REM [1/7] Surum artir (derleme basarisiz olursa geri alinir)
REM ============================================================
echo [1/7] Surum artiriliyor...
set "VERBACKUP=%TEMP%\makay_launcher_version_backup.txt"
copy /Y "version.properties" "%VERBACKUP%" >nul

powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "$p = Join-Path '%~dp0' 'version.properties';" ^
  "$name = '1.0.0'; $code = 1; $update = '';" ^
  "if (Test-Path $p) {" ^
  "  Get-Content $p | ForEach-Object {" ^
  "    if ($_ -match '^VERSION_NAME=(.+)$') { $name = $Matches[1].Trim() }" ^
  "    if ($_ -match '^VERSION_CODE=(.+)$') { $code = [int]$Matches[1].Trim() }" ^
  "    if ($_ -match '^UPDATE_URL=(.*)$') { $update = $Matches[1].Trim() }" ^
  "  }" ^
  "}" ^
  "$parts = $name.Split('.');" ^
  "if ($parts.Length -lt 3) { $parts = @('1','0','0') }" ^
  "$parts[2] = [string](([int]$parts[2]) + 1);" ^
  "$name = $parts[0] + '.' + $parts[1] + '.' + $parts[2];" ^
  "$code = $code + 1;" ^
  "$enc = New-Object System.Text.UTF8Encoding($false);" ^
  "[IO.File]::WriteAllText($p, ('VERSION_NAME=' + $name + [Environment]::NewLine + 'VERSION_CODE=' + $code + [Environment]::NewLine + 'UPDATE_URL=' + $update + [Environment]::NewLine), $enc);"

if errorlevel 1 (
    echo [HATA] Surum dosyasi guncellenemedi.
    goto :FAIL
)

set "VER="
set "CODE="
for /f "usebackq tokens=1,* delims==" %%A in ("version.properties") do (
    if /i "%%A"=="VERSION_NAME" set "VER=%%B"
    if /i "%%A"=="VERSION_CODE" set "CODE=%%B"
)
if not defined VER (
    echo [HATA] VERSION_NAME okunamadi.
    goto :FAIL
)
if not defined CODE (
    echo [HATA] VERSION_CODE okunamadi.
    goto :FAIL
)
echo      Yeni surum: %VER%  (kod %CODE%)

REM ============================================================
REM [2/7] Derle
REM ============================================================
echo.
echo [2/7] Derleniyor: Makay Launcher %VER%
echo       Bir kac dakika surebilir...
echo.
call "%~dp0gradlew.bat" :app:assembleRelease
if errorlevel 1 (
    echo.
    echo [HATA] Derleme basarisiz oldu.
    echo        Surum geri aliniyor: %VER% -^> eski deger
    copy /Y "%VERBACKUP%" "version.properties" >nul
    goto :FAIL
)

set "SRC=%~dp0app\build\outputs\apk\release\app-release.apk"
set "DST=%~dp0APK\MakayLauncher-%VER%.apk"
set "DSTM=%~dp0APK\MakayLauncher-%VER%-mapping.txt"
if not exist "%SRC%" (
    echo [HATA] APK bulunamadi: %SRC%
    copy /Y "%VERBACKUP%" "version.properties" >nul
    goto :FAIL
)

copy /Y "%SRC%" "%DST%" >nul
if not exist "%DST%" (
    echo [HATA] APK kopyalanamadi: %DST%
    goto :FAIL
)

REM ============================================================
REM [3/7] Eski yerel surumleri temizle (yalnizca %VER% kalir)
REM ============================================================
echo.
echo [3/7] Eski yerel surumler temizleniyor...
for %%F in ("APK\MakayLauncher-*.apk") do (
    if exist "%%F" if /i not "%%~nxF"=="MakayLauncher-%VER%.apk" del /q "%%F" >nul 2>&1
)
for %%F in ("APK\MakayLauncher-*.apk.sha256") do (
    if exist "%%F" if /i not "%%~nxF"=="MakayLauncher-%VER%.apk.sha256" del /q "%%F" >nul 2>&1
)
for %%F in ("APK\MakayLauncher-*-mapping.txt") do (
    if exist "%%F" if /i not "%%~nxF"=="MakayLauncher-%VER%-mapping.txt" del /q "%%F" >nul 2>&1
)
REM R8 obfuscation haritasi - assembleRelease doLast'i APK/ klasorune yazar.
if not exist "%~dp0APK\MakayLauncher-%VER%-mapping.txt" (
    echo [UYARI] R8 mapping.txt yok - release stack trace'leri cozulemez.
)
REM APK klasoru yalnizca .apk + .apk.sha256 + -mapping.txt icerir (eski artik dosyalarini temizle)
if exist "%~dp0APK\latest.json" del /q "%~dp0APK\latest.json" >nul 2>&1
if exist "%~dp0APK\README.md" del /q "%~dp0APK\README.md" >nul 2>&1

REM ============================================================
REM [4/7] SHA256 + latest.json uret (BOM'suz UTF-8)
REM ============================================================
echo [4/7] SHA256 ozeti ve latest.json hazirlaniyor...
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "$apk = '%DST%';" ^
  "$ver = '%VER%';" ^
  "$code = [int]%CODE%;" ^
  "$hash = (Get-FileHash -LiteralPath $apk -Algorithm SHA256).Hash.ToLower();" ^
  "$enc = New-Object System.Text.UTF8Encoding($false);" ^
  "[IO.File]::WriteAllText(($apk + '.sha256'), ($hash + '  ' + [IO.Path]::GetFileName($apk) + [Environment]::NewLine), $enc);" ^
  "$apkUrl = 'https://github.com/makaydestek/Makay-Lanucher/releases/download/' + $ver + '/MakayLauncher-' + $ver + '.apk';" ^
  "$obj = [ordered]@{ versionName = $ver; versionCode = $code; apkUrl = $apkUrl; notes = ''; sha256 = $hash };" ^
  "$json = $obj | ConvertTo-Json -Compress;" ^
  "[IO.File]::WriteAllText((Join-Path '%~dp0' 'latest.json'), $json, $enc);" ^
  "Write-Output ('       SHA256: ' + $hash)"
if errorlevel 1 (
    echo [HATA] SHA256 / latest.json uretilemedi.
    goto :FAIL
)

REM ============================================================
REM [5/7] GitHub CLI kontrol
REM ============================================================
echo.
echo [5/7] GitHub CLI kontrol ediliyor...
where gh >nul 2>&1
if errorlevel 1 (
    echo [HATA] GitHub CLI ^(gh^) bulunamadi.
    echo        Kurulum: winget install --id GitHub.cli
    goto :FAIL
)
gh auth status >nul 2>&1
if errorlevel 1 (
    echo [HATA] GitHub oturumu yok. Once: gh auth login
    goto :FAIL
)

REM ============================================================
REM [6/7] GitHub Release olustur / dosyalari yukle
REM ============================================================
echo.
echo [6/7] GitHub Release %VER% yayinlaniyor...
gh release view "%VER%" --repo %REPO% >nul 2>&1
if errorlevel 1 goto :createrelease

echo       Release %VER% mevcut - dosyalar guncelleniyor...
REM Mapping yalnizca varsa yuklenir; gh bos argumani reddettigi icin dallandirilir.
if exist "%DSTM%" (
    gh release upload "%VER%" "%DST%" "%DST%.sha256" "%DSTM%" --repo %REPO% --clobber
) else (
    echo       [UYARI] mapping yok, APK + SHA-256 yukleniyor.
    gh release upload "%VER%" "%DST%" "%DST%.sha256" --repo %REPO% --clobber
)
if errorlevel 1 goto :FAIL
goto :releaseend

:createrelease
echo       Yeni release olusturuluyor...
REM Bos not dosyasi gerekli: gh, bos argumani "flag needs an argument" diye reddeder.
set "NOTES=%TEMP%\makay-launcher-release-notes.txt"
type nul > "!NOTES!"
if exist "%DSTM%" (
    gh release create "%VER%" "%DST%" "%DST%.sha256" "%DSTM%" --repo %REPO% --title "%VER%" --notes-file "!NOTES!"
) else (
    gh release create "%VER%" "%DST%" "%DST%.sha256" --repo %REPO% --title "%VER%" --notes-file "!NOTES!"
)
if errorlevel 1 (
    del /q "!NOTES!" >nul 2>&1
    echo [HATA] Release olusturulamadi.
    goto :FAIL
)
del /q "!NOTES!" >nul 2>&1
:releaseend
echo       OK: https://github.com/makaydestek/Makay-Lanucher/releases/tag/%VER%

REM ============================================================
REM [7/7] latest.json + version.properties main dalina yaz
REM         (uygulamanin guncelleme adresi) ve eski release'leri sil
REM ============================================================
echo.
echo [7/7] latest.json main dalina gonderiliyor...
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "$repo = '%REPO%';" ^
  "$enc = New-Object System.Text.UTF8Encoding($false);" ^
  "function Push-File([string]$local, [string]$remote, [string]$msg) {" ^
  "  $b64 = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes([IO.File]::ReadAllText($local, $enc)));" ^
  "  $sha = & gh api ('repos/' + $repo + '/contents/' + $remote) --jq .sha 2> $null;" ^
  "  if ($LASTEXITCODE -ne 0) { $sha = $null }" ^
  "  $a = @('api','-X','PUT',('repos/' + $repo + '/contents/' + $remote),'--field',('message=' + $msg),'--field',('content=' + $b64),'--field','branch=main');" ^
  "  if ($sha) { $a += @('--field',('sha=' + $sha)) }" ^
  "  $out = & gh @a 2> $null;" ^
  "  return $LASTEXITCODE;" ^
  "}" ^
  "$r1 = Push-File (Join-Path '%~dp0' 'latest.json') 'latest.json' ('latest.json %VER%');" ^
  "$r2 = Push-File (Join-Path '%~dp0' 'version.properties') 'version.properties' ('Yayin: Makay Launcher v%VER%');" ^
  "if ($r1 -ne 0) { Write-Output '[UYARI] latest.json push edilemedi.' }" ^
  "if ($r2 -ne 0) { Write-Output '[UYARI] version.properties push edilemedi.' }" ^
  "exit ([Math]::Max($r1, $r2))"

echo.
echo Eski GitHub release'leri siliniyor (yalnizca %VER% kalir)...
for /f "usebackq delims=" %%T in (`gh release list --repo %REPO% --json tagName -q ".[].tagName" 2^>nul`) do (
    if /i not "%%T"=="%VER%" (
        echo       siliniyor: %%T
        gh release delete "%%T" --repo %REPO% --yes --cleanup-tag >nul 2>&1
    )
)

echo.
echo ============================================================
echo  APK hazir: APK\MakayLauncher-%VER%.apk
echo  Release : https://github.com/makaydestek/Makay-Lanucher/releases/tag/%VER%
echo  Manifest: https://raw.githubusercontent.com/makaydestek/Makay-Lanucher/main/latest.json
echo  Ayni imza + yuksek surum = guncelleme; ayarlar ve izgara korunur.
echo  Onceki launcher: Ayarlar - Ana ekran uygulamasini degistir
echo ============================================================
start "" "%~dp0APK"
echo.
pause
endlocal
exit /b 0

:FAIL
echo.
echo ============================================================
echo  BASARISIZ - pencereyi kapatmak icin bir tus
echo ============================================================
pause
endlocal
exit /b 1
