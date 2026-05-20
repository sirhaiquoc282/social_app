@echo off
setlocal

set SDK_PATH=C:\Users\Lenovo\AppData\Local\Android\Sdk
set ADB=%SDK_PATH%\platform-tools\adb.exe
set APK=app\build\outputs\apk\debug\app-debug.apk
set PACKAGE=com.example.socialapp
set ACTIVITY=%PACKAGE%/.MainActivity
set AVD=Pixel_9a

echo ================================================
echo    SocialApp - Build và Deploy Script
echo ================================================
echo.

:: === Bước 1: Build ===
echo [1/3] Dang build APK...
call gradlew.bat assembleDebug
if %ERRORLEVEL% neq 0 (
    echo.
    echo [FAILED] Build that bai!
    pause
    exit /b 1
)
echo [OK] Build thanh cong!
echo.

:: === Bước 2: Kiểm tra emulator ===
echo [2/3] Kiem tra emulator...
for /f "tokens=*" %%i in ('"%ADB%" devices ^| findstr /v "List" ^| findstr "device"') do (
    set DEVICE_FOUND=1
)

if not defined DEVICE_FOUND (
    echo Khong tim thay emulator! Dang khoi dong %AVD%...
    start "" "%SDK_PATH%\emulator\emulator.exe" -avd %AVD% -no-snapshot-load
    echo Cho emulator boot (30 giay)...
    timeout /t 30 /nobreak > nul
)

:: Chờ boot hoàn chỉnh
:WAIT_BOOT
"%ADB%" shell getprop sys.boot_completed 2>nul | findstr "1" > nul
if %ERRORLEVEL% neq 0 (
    echo Dang boot...
    timeout /t 5 /nobreak > nul
    goto WAIT_BOOT
)
echo [OK] Emulator san sang!
echo.

:: === Bước 3: Cài và mở app ===
echo [3/3] Cai APK len emulator...
"%ADB%" install -r %APK%
if %ERRORLEVEL% neq 0 (
    echo [FAILED] Cai APK that bai!
    pause
    exit /b 1
)

echo Mo app...
"%ADB%" shell am start -n "%ACTIVITY%"

echo.
echo ================================================
echo  DONE! SocialApp dang chay tren emulator!
echo ================================================
echo.
echo Luu y: De chat/call hoat dong, can:
echo   1. Them google-services.json tu Firebase Console
echo   2. Them AGORA_APP_ID vao local.properties
echo ================================================
pause

