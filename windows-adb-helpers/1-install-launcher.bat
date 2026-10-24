@echo off
cd /d "%~dp0"
echo.
echo === STEP 1: Install My Launcher on your Google TV Streamer ===
echo.
echo Devices your laptop can currently see:
adb devices
echo.
echo (Your Streamer should be listed above and say "device".
echo  If it says "unauthorized", look at the TV and press Allow.
echo  If the list is empty, connect to the Streamer first - see the guide.)
echo.
if not exist app-debug.apk goto missing
adb install -r app-debug.apk
echo.
echo If you see the word Success above, the launcher is installed.
echo.
echo NEXT: on the TV, open "My Launcher" from your Apps row and try it out.
echo Do NOT run step 2 until it opens and looks right.
echo.
pause
exit /b 0

:missing
echo.
echo Could not find app-debug.apk in this folder.
echo Copy app-debug.apk into the same folder as this file and adb.exe, then try again.
echo.
pause
exit /b 1
