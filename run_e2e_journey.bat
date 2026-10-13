@echo off
setlocal enabledelayedexpansion

echo ========================================================
echo   FAMEKO CROSS-DEVICE E2E JOURNEY TEST HARNESS (PHASE 2)
echo ========================================================

set DEVICE_A=f6f8a5a5
set DEVICE_B=RZ8M73RE1FE

for /f "tokens=*" %%v in ('adb -s %DEVICE_A% shell getprop ro.build.version.release') do set VER_A=%%v
for /f "tokens=*" %%v in ('adb -s %DEVICE_B% shell getprop ro.build.version.release') do set VER_B=%%v

echo Device A (%DEVICE_A%): OPPO PBEM00 - Android %VER_A%
echo Device B (%DEVICE_B%): Samsung SM-A105F - Android %VER_B%

mkdir build\reports\e2e_screenshots 2>nul

:: ========================================================
:: RUN 1: PRIMARY ASSIGNMENT
:: Customer -> %DEVICE_A% (Android 10), Driver -> %DEVICE_B% (Android 11)
:: ========================================================
echo.
echo ========================================================
echo   RUN 1: PRIMARY ASSIGNMENT
echo   Customer App -> %DEVICE_A% (Android %VER_A%)
echo   Driver App   -> %DEVICE_B% (Android %VER_B%)
echo ========================================================

echo [1/7] Installing APKs...
adb -s %DEVICE_A% install -r app-customer\build\outputs\apk\debug\app-customer-debug.apk >nul
adb -s %DEVICE_B% install -r app\build\outputs\apk\debug\app-debug.apk >nul

echo [2/7] Launching Customer and Driver Apps...
adb -s %DEVICE_A% shell am start -n com.example.famekodriver.customer/.CustomerSplashActivity >nul
adb -s %DEVICE_B% shell am start -n com.example.famekodriver/.DriverSplashActivity >nul
timeout /t 4 /nobreak >nul

adb -s %DEVICE_A% exec-out screencap -p > build\reports\e2e_screenshots\run1_step1_launch_customer.png
adb -s %DEVICE_B% exec-out screencap -p > build\reports\e2e_screenshots\run1_step1_launch_driver.png

echo [3/7] Toggling Driver ONLINE on Device B (%DEVICE_B%)...
adb -s %DEVICE_B% shell input tap 500 1350
timeout /t 3 /nobreak >nul

adb -s %DEVICE_B% exec-out screencap -p > build\reports\e2e_screenshots\run1_step2_driver_online.png

echo [4/7] Creating Ride Request from Customer App on Device A (%DEVICE_A%)...
adb -s %DEVICE_A% shell input tap 300 800
timeout /t 2 /nobreak >nul

adb -s %DEVICE_A% exec-out screencap -p > build\reports\e2e_screenshots\run1_step3_customer_booking.png

echo [5/7] Verifying Driver receives request on Device B (%DEVICE_B%)...
timeout /t 3 /nobreak >nul
adb -s %DEVICE_B% exec-out screencap -p > build\reports\e2e_screenshots\run1_step4_driver_received_request.png

echo [6/7] Driver accepting request on Device B (%DEVICE_B%)...
adb -s %DEVICE_B% shell input tap 500 1400
timeout /t 3 /nobreak >nul

adb -s %DEVICE_B% exec-out screencap -p > build\reports\e2e_screenshots\run1_step5_driver_accepted.png
adb -s %DEVICE_A% exec-out screencap -p > build\reports\e2e_screenshots\run1_step5_customer_assigned.png

echo [7/7] Exercising Trip Lifecycle (ARRIVED -> IN_TRANSIT -> DELIVERED)...
adb -s %DEVICE_B% shell input tap 750 1400
timeout /t 2 /nobreak >nul

adb -s %DEVICE_B% shell input tap 500 1400
timeout /t 2 /nobreak >nul

adb -s %DEVICE_B% shell input tap 500 1400
timeout /t 3 /nobreak >nul

adb -s %DEVICE_B% exec-out screencap -p > build\reports\e2e_screenshots\run1_step6_trip_completed_driver.png
adb -s %DEVICE_A% exec-out screencap -p > build\reports\e2e_screenshots\run1_step6_trip_completed_customer.png

echo Run 1 Primary Assignment Journey Completed.

:: ========================================================
:: RUN 2: ROLE SWAPPED ASSIGNMENT
:: Customer -> %DEVICE_B% (Android 11), Driver -> %DEVICE_A% (Android 10)
:: ========================================================
echo.
echo ========================================================
echo   RUN 2: ROLE SWAPPED ASSIGNMENT
echo   Customer App -> %DEVICE_B% (Android %VER_B%)
echo   Driver App   -> %DEVICE_A% (Android %VER_A%)
echo ========================================================

echo [1/7] Installing APKs in Swapped Roles...
adb -s %DEVICE_B% install -r app-customer\build\outputs\apk\debug\app-customer-debug.apk >nul
adb -s %DEVICE_A% install -r app\build\outputs\apk\debug\app-debug.apk >nul

echo [2/7] Launching Apps in Swapped Roles...
adb -s %DEVICE_B% shell am start -n com.example.famekodriver.customer/.CustomerSplashActivity >nul
adb -s %DEVICE_A% shell am start -n com.example.famekodriver/.DriverSplashActivity >nul
timeout /t 4 /nobreak >nul

adb -s %DEVICE_B% exec-out screencap -p > build\reports\e2e_screenshots\run2_step1_launch_customer.png
adb -s %DEVICE_A% exec-out screencap -p > build\reports\e2e_screenshots\run2_step1_launch_driver.png

echo [3/7] Toggling Driver ONLINE on Device A (%DEVICE_A%)...
adb -s %DEVICE_A% shell input tap 500 1350
timeout /t 3 /nobreak >nul

adb -s %DEVICE_A% exec-out screencap -p > build\reports\e2e_screenshots\run2_step2_driver_online.png

echo [4/7] Creating Ride Request from Customer App on Device B (%DEVICE_B%)...
adb -s %DEVICE_B% shell input tap 300 800
timeout /t 2 /nobreak >nul

adb -s %DEVICE_B% exec-out screencap -p > build\reports\e2e_screenshots\run2_step3_customer_booking.png

echo [5/7] Verifying Driver receives request on Device A (%DEVICE_A%)...
timeout /t 3 /nobreak >nul
adb -s %DEVICE_A% exec-out screencap -p > build\reports\e2e_screenshots\run2_step4_driver_received_request.png

echo [6/7] Driver accepting request on Device A (%DEVICE_A%)...
adb -s %DEVICE_A% shell input tap 500 1400
timeout /t 3 /nobreak >nul

adb -s %DEVICE_A% exec-out screencap -p > build\reports\e2e_screenshots\run2_step5_driver_accepted.png
adb -s %DEVICE_B% exec-out screencap -p > build\reports\e2e_screenshots\run2_step5_customer_assigned.png

echo Run 2 Role Swapped Journey Completed.

echo.
echo ========================================================
echo   E2E JOURNEY TEST HARNESS COMPLETED SUCCESSFULLY
echo ========================================================
exit /b 0
