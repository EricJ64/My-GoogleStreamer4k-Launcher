# My Launcher for Google TV Streamer - setup guide

You do not need to know how to code. There are four parts, and every part is
copy-and-click. Do them in order.

What you get: a simple home screen showing only the apps you choose, plus a
"Settings" tile and an "Add / remove apps" tile so you can change your list any
time with the remote. It can be undone at any time (Part 4).

> Honest note: I could not test this on a real Google TV Streamer. The launcher
> follows standard Android TV rules and the "make it the default" method is the
> one used widely for Google TV devices, but it has not been confirmed on your
> exact box. Part 4 is the safety net, so read it before you start.

---

## Part 1 - Build the APK (free, nothing to install)

The APK is the installable app file. GitHub builds it for you.

1. Go to https://github.com and create a free account (or sign in).
2. Click the **+** at the top right, then **New repository**.
   - Name it `my-launcher`. Leave everything else as it is. Click **Create repository**.
3. On the new empty repository page click **uploading an existing file**.
4. Unzip `tv-launcher-project.zip` on your laptop. Open the unzipped folder.
   Select **everything inside it** (the `app` folder, the `.github` folder,
   `build.gradle`, `settings.gradle`, `gradle.properties`, and so on) and drag it
   all into the GitHub page. Wait for the upload to finish, then click **Commit changes**.
5. Check the `.github` folder uploaded. Open your repository and look for a
   folder called `.github` in the file list.
   - If it is missing: click **Add file > Create new file**, type exactly
     `.github/workflows/build.yml` as the file name, open `.github/workflows/build.yml`
     from the unzipped folder in Notepad, copy all of it, paste it in, and click **Commit changes**.
6. Click the **Actions** tab at the top. You should see a run called "Build APK"
   working (yellow circle). If nothing is running, click **Build APK** on the
   left, then **Run workflow**, then the green **Run workflow** button.
7. Wait about 3 to 5 minutes until it shows a green tick.
8. Click the finished run. Scroll to **Artifacts** at the bottom and click
   **my-launcher-apk**. It downloads a zip. Unzip it. Inside is **app-debug.apk**.

If the run shows a red cross, click it, copy the red error text, and paste it to
me. I will fix it.

---

## Part 2 - Get your laptop talking to the Streamer

### 2a. Turn on Developer options on the Streamer
1. On the Streamer: **Settings > System > About**.
2. Highlight **Android TV OS Build** (not "Version") and press the centre button
   of the remote about 7 times until a message says "You are now a developer!".
3. Go back to **Settings > System**. **Developer options** now appears. Open it.
4. Turn on **USB debugging**. Choose OK/Allow.

### 2b. Install ADB on your laptop
ADB is Google's small tool that lets your laptop send commands to the Streamer.
1. Download **SDK Platform-Tools for Windows** from
   https://developer.android.com/tools/releases/platform-tools
2. Right-click the zip, **Extract All**. You will get a folder called `platform-tools`.
3. Copy these into that `platform-tools` folder, next to `adb.exe`:
   - `app-debug.apk` (from Part 1)
   - all four `.bat` files from the `windows-adb-helpers` folder of this project

### 2c. Connect
1. Find the Streamer's IP address on the TV: **Settings > System > About > Status**
   (it looks like 192.168.1.45). Your laptop and the Streamer must be on the same
   Wi-Fi or network.
2. On your laptop, open the `platform-tools` folder. Click the address bar at the
   top of the window, type `cmd`, and press Enter. A black window opens.
3. Type this (use your own IP address) and press Enter:

       adb connect 192.168.1.45:5555

4. Look at the TV. A box asks "Allow USB debugging?". Tick **Always allow from
   this computer** and choose **Allow**.
5. If it said it could not connect, just run the same command once more.
   Then type `adb devices` and press Enter. Your Streamer should be listed and say `device`.

**If it still refuses to connect:** the Streamer may want "Wireless debugging"
instead. In Developer options turn on **Wireless debugging**, choose **Pair
device with pairing code**, and the TV shows an IP:port and a 6 digit code. In
the black window type `adb pair IP:PORT` (using the pairing IP:port shown), enter the
code, then use the (different) IP:port shown on the main Wireless debugging
screen with `adb connect IP:PORT`. If you get stuck, tell me what the black
window says and I will help.

---

## Part 3 - Install and switch over

1. In the `platform-tools` folder, double-click **1-install-launcher.bat**.
   It should say `Success`.
2. On the TV, open **My Launcher** from the Apps row of the normal home screen.
   The first time it opens a list of your apps. Press **OK** on each app you want
   on your home screen (a green tick appears). **Google Play Store is already
   ticked so you can always install more apps.** Press **Back** when done.
3. Check it looks right and that your apps open. If not, stop here and tell me.
4. Double-click **2-make-default-home.bat**. This turns off the stock Google TV
   home screen. The Home button now opens My Launcher.
   - If a setup screen appears instead of your launcher, double-click
     **2b-only-if-needed-disable-setup-screen.bat**, then press Home again.

### Using it afterwards
- **Add or remove apps:** choose the **Add / remove apps** tile, press OK on an
  app to tick or untick it, press Back.
- **Settings:** the **Settings** tile (your Home screen no longer has a gear icon).
- New apps you install from the Play Store appear in the Add / remove list.
- Apps appear in the order you added them.

---

## Part 4 - Go back to the original home screen

Double-click **RESTORE-original-home.bat**. Your laptop must be connected to the
Streamer first (Part 2c). It re-enables the standard Google TV home screen and
asks whether to also remove My Launcher.

**If you cannot connect any more** (for example if the Streamer lost its settings),
the last resort is a factory reset from the Streamer's Settings. Everything the
scripts changed is wiped by a reset, so you always get the original back.

---

## Good to know
- Software updates from Google may occasionally re-enable the stock home screen.
  If your Home button goes back to the original, just run step 2 again.
- I have not confirmed whether a restart of the Streamer does the same. If it
  does, the fix is the same: run step 2 again.
- The APK is a "debug" build. That is fine for personal use and installs the
  same way. It is not for the Play Store.
- Everything this app does stays on your device. It has no internet access and
  collects nothing.
