@echo off
cd /d "%~dp0"
echo.
echo === OPTIONAL STEP 2b: only run this if step 2 showed a setup screen
echo     instead of My Launcher when you pressed Home ===
echo.
echo This turns off the Google TV setup helper, which on some devices
echo steps in as the Home screen when the normal one is disabled.
echo RESTORE-original-home.bat will turn it back on.
echo.
pause
adb shell pm disable-user --user 0 com.google.android.tungsten.setupwraith
adb shell input keyevent KEYCODE_HOME
echo.
pause
