@echo off
cd /d "%~dp0"
echo.
echo === RESTORE: go back to the original Google TV home screen ===
echo.
echo Devices your laptop can currently see:
adb devices
echo.
pause
adb shell pm enable --user 0 com.google.android.apps.tv.launcherx
adb shell pm enable --user 0 com.google.android.tungsten.setupwraith
echo.
choice /c YN /m "Also remove My Launcher from the TV completely"
if errorlevel 2 goto keep
adb uninstall com.example.tvlauncher
:keep
adb shell input keyevent KEYCODE_HOME
echo.
echo Done. Press Home on the TV. If it asks which Home app to use, pick Google TV.
echo.
pause
