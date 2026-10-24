@echo off
cd /d "%~dp0"
echo.
echo === STEP 2: Make My Launcher your Home screen ===
echo.
echo This turns OFF the standard Google TV home screen so that the Home
echo button opens My Launcher instead.
echo.
echo Only continue if My Launcher already opens and works on the TV.
echo You can undo this at any time by running RESTORE-original-home.bat
echo.
pause
adb shell pm disable-user --user 0 com.google.android.apps.tv.launcherx
echo.
echo Pressing the Home button on the TV for you...
adb shell input keyevent KEYCODE_HOME
echo.
echo You should now see My Launcher.
echo If you see something else (like a setup screen), run
echo 2b-only-if-needed-disable-setup-screen.bat and press Home again.
echo If anything looks wrong, run RESTORE-original-home.bat
echo.
pause
