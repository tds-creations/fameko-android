@echo off
setlocal enabledelayedexpansion

echo ========================================================
echo   FAMEKO PHASE 3: VOICE CALL AUDIO ^& RESILIENCE HARNESS
echo ========================================================

set DEVICE_A=f6f8a5a5
set DEVICE_B=RZ8M73RE1FE

for /f "tokens=*" %%v in ('adb -s %DEVICE_A% shell getprop ro.build.version.release') do set VER_A=%%v
for /f "tokens=*" %%v in ('adb -s %DEVICE_B% shell getprop ro.build.version.release') do set VER_B=%%v

echo Device A (%DEVICE_A%): OPPO PBEM00 - Android %VER_A%
echo Device B (%DEVICE_B%): Samsung SM-A105F - Android %VER_B%

mkdir build\reports\e2e_screenshots 2>nul

:: ========================================================
:: SCENARIO 1: PRIMARY CALL ASSIGNMENT
:: Customer on %DEVICE_A% (Android 10), Driver on %DEVICE_B% (Android 11)
:: ========================================================
echo.
echo ========================================================
echo   SCENARIO 1: PRIMARY VOICE CALL TEST
echo   Customer App -> %DEVICE_A% (Android %VER_A%)
echo   Driver App   -> %DEVICE_B% (Android %VER_B%)
echo ========================================================

echo [1/7] Launching Applications...
adb -s %DEVICE_A% shell am start -n com.example.famekodriver.customer/.CustomerSplashActivity >nul
adb -s %DEVICE_B% shell am start -n com.example.famekodriver/.DriverSplashActivity >nul
timeout /t 4 /nobreak >nul

echo [2/7] Customer Initiating Outgoing Call on %DEVICE_A%...
:: Tap call button on Customer App
adb -s %DEVICE_A% shell input tap 800 650
timeout /t 3 /nobreak >nul

adb -s %DEVICE_A% exec-out screencap -p > build\reports\e2e_screenshots\phase3_sc1_step1_customer_dialing.png
adb -s %DEVICE_B% exec-out screencap -p > build\reports\e2e_screenshots\phase3_sc1_step2_driver_ringing.png

echo [3/7] Driver Accepting Call on %DEVICE_B%...
:: Tap Accept on Driver App Call Overlay
adb -s %DEVICE_B% shell input tap 700 250
timeout /t 3 /nobreak >nul

adb -s %DEVICE_B% exec-out screencap -p > build\reports\e2e_screenshots\phase3_sc1_step3_driver_connected.png
adb -s %DEVICE_A% exec-out screencap -p > build\reports\e2e_screenshots\phase3_sc1_step3_customer_connected.png

echo [4/7] Testing Mute ^& Speakerphone Toggles on Both Devices...
:: Tap Mute on Customer App
adb -s %DEVICE_A% shell input tap 300 220
timeout /t 1 /nobreak >nul

:: Tap Speakerphone on Driver App
adb -s %DEVICE_B% shell input tap 500 220
timeout /t 2 /nobreak >nul

adb -s %DEVICE_A% exec-out screencap -p > build\reports\e2e_screenshots\phase3_sc1_step4_customer_muted.png
adb -s %DEVICE_B% exec-out screencap -p > build\reports\e2e_screenshots\phase3_sc1_step4_driver_speaker.png

echo [5/7] Ending Call on Driver App (%DEVICE_B%)...
:: Tap End Call button
adb -s %DEVICE_B% shell input tap 700 220
timeout /t 3 /nobreak >nul

adb -s %DEVICE_B% exec-out screencap -p > build\reports\e2e_screenshots\phase3_sc1_step5_call_ended.png

:: ========================================================
:: SCENARIO 2: NETWORK INTERRUPTION ^& RECOVERY TEST
:: ========================================================
echo.
echo [6/7] Testing Network Interruption ^& Reconnection Resilience...
echo Simulating brief Wi-Fi disconnect on Device B (%DEVICE_B%)...
adb -s %DEVICE_B% shell svc wifi disable
timeout /t 4 /nobreak >nul

adb -s %DEVICE_B% exec-out screencap -p > build\reports\e2e_screenshots\phase3_network_disconnected.png

echo Restoring Wi-Fi connectivity on Device B (%DEVICE_B%)...
adb -s %DEVICE_B% shell svc wifi enable
timeout /t 5 /nobreak >nul

adb -s %DEVICE_B% exec-out screencap -p > build\reports\e2e_screenshots\phase3_network_reconnected.png

:: ========================================================
:: SCENARIO 3: ROLE SWAPPED VOICE CALL TEST
:: Customer on %DEVICE_B% (Android 11), Driver on %DEVICE_A% (Android 10)
:: ========================================================
echo.
echo ========================================================
echo   SCENARIO 3: ROLE SWAPPED VOICE CALL TEST
echo   Customer App -> %DEVICE_B% (Android %VER_B%)
echo   Driver App   -> %DEVICE_A% (Android %VER_A%)
echo ========================================================

echo [1/4] Installing APKs in Swapped Roles...
adb -s %DEVICE_B% install -r app-customer\build\outputs\apk\debug\app-customer-debug.apk >nul
adb -s %DEVICE_A% install -r app\build\outputs\apk\debug\app-debug.apk >nul

echo [2/4] Launching Apps in Swapped Roles...
adb -s %DEVICE_B% shell am start -n com.example.famekodriver.customer/.CustomerSplashActivity >nul
adb -s %DEVICE_A% shell am start -n com.example.famekodriver/.DriverSplashActivity >nul
timeout /t 4 /nobreak >nul

echo [3/4] Customer Initiating Call on Device B (%DEVICE_B%)...
adb -s %DEVICE_B% shell input tap 800 650
timeout /t 3 /nobreak >nul

adb -s %DEVICE_B% exec-out screencap -p > build\reports\e2e_screenshots\phase3_sc3_step1_customer_dialing.png
adb -s %DEVICE_A% exec-out screencap -p > build\reports\e2e_screenshots\phase3_sc3_step2_driver_ringing.png

echo [4/4] Driver Accepting Call on Device A (%DEVICE_A%)...
adb -s %DEVICE_A% shell input tap 700 250
timeout /t 3 /nobreak >nul

adb -s %DEVICE_A% exec-out screencap -p > build\reports\e2e_screenshots\phase3_sc3_step3_driver_connected.png
adb -s %DEVICE_B% exec-out screencap -p > build\reports\e2e_screenshots\phase3_sc3_step3_customer_connected.png

echo.
echo ========================================================
echo   PHASE 3 VOICE CALL TEST HARNESS COMPLETED
echo ========================================================
exit /b 0
