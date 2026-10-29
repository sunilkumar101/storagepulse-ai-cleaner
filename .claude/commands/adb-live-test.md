---
description: Install on the connected phone, drive the UI via adb, screenshot and fix bugs
argument-hint: [screen or feature to test]
---

Focus: $ARGUMENTS

Check my connected phone via `adb devices`, install the APK via `./gradlew installDebug`, clear logcat (`adb logcat -c`), launch the screen on my phone, dump UI nodes via `adb shell uiautomator dump /sdcard/ui.xml && adb pull /sdcard/ui.xml build/ui.xml`, tap interactive buttons/toggles via `adb shell input tap`, capture a real screenshot from my phone via `adb exec-out screencap -p > build/live_screen.png`, visually inspect `build/live_screen.png` and `adb logcat -d *:E` for any UI bug, text clipping, crash, or broken feature, and fix it immediately.

Steps:
1. `adb devices` — stop and tell me if no device is listed or it shows `unauthorized`.
2. `./gradlew installDebug`
3. `adb logcat -c`
4. `adb shell am start -n com.storagepulse.cleaner/.MainActivity` (add the deep-link/route extra for the target screen if one exists).
5. `adb shell uiautomator dump /sdcard/ui.xml && adb pull /sdcard/ui.xml build/ui.xml` — read node `bounds` to get tap coordinates.
6. `adb shell input tap <x> <y>` on each interactive button/toggle; re-dump after navigation.
7. `adb exec-out screencap -p > build/live_screen.png` (in PowerShell use `cmd /c "adb exec-out screencap -p > build\live_screen.png"` so the PNG is not corrupted) and read the image.
8. `adb logcat -d *:E` — check for crashes / errors from `com.storagepulse.cleaner`.
9. Fix any bug found, reinstall, and repeat until the screen is clean.

Test phone notes (OPPO CPH1937, Android 11 / API 30, 720×1600 @ 320dpi = 360dp wide):
- adb is at `$LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe` (not on PATH). In Git Bash set `MSYS_NO_PATHCONV=1` so `/sdcard/...` paths are not rewritten.
- ColorOS blocks `pm clear` and `appops set` from shell. To reset permissions, `adb uninstall com.storagepulse.cleaner` then reinstall (the phone may ask to confirm a USB install on screen).
- If the screen is off/locked, ask the user to unlock it. Never enter their PIN.
- Bottom nav tab centers (y≈1444): Dashboard 72, Treemap 216, AI Clean 360, Gallery 504, Tools 648.
- `uiautomator dump` hangs while an indeterminate progress bar is animating (e.g. during a scan). Wait for logcat `StorageScanner: Scan …` (`adb logcat -d -s StorageScanner:I`) or use screencap instead.
- To open All Files Access directly: `adb shell am start -a android.settings.MANAGE_APP_ALL_FILES_ACCESS_PERMISSION -d package:com.storagepulse.cleaner`, toggle at (636,456).
- `./gradlew connectedDebugAndroidTest` UNINSTALLS the app afterwards (permissions reset). Run `installDebug` and re-grant access after it.
- Double-tap via adb: `adb shell "input tap X Y & sleep 0.15; input tap X Y"` (two separate `input` calls are too slow).
- Compose merges a clickable row's texts; find rows by `text="Name"` prefix, not the chevron's content-desc.
- To test Purge, create your OWN test files (e.g. `/sdcard/Download/SPTest/`), never purge the user's real clutter; afterwards remove them, their `.StoragePulseTrash/*` copies and (force-stopped) `run-as com.storagepulse.cleaner rm databases/storagepulse.db*` if only test rows exist.
- Test media must be indexed properly: after `cp`, run `adb shell "content call --uri content://media --method scan_file --arg /storage/emulated/0/<path>"` (the old MEDIA_SCANNER broadcast leaves rows `is_pending=1`, which apps cannot see). Run it again on deleted paths/folders to remove their rows.
- Wait ~10 s after `am start` before tapping tabs (the start route loads first).
- Long analyses: keep the screen awake; OPPO kills CPU-heavy apps once they go to background. Logcat is rate-limited on this phone — the debug build writes `files/dedup_stats.txt` (`run-as com.storagepulse.cleaner cat files/dedup_stats.txt`).
- `connectedDebugAndroidTest` uninstalls the app = loses the ~6 min photo fingerprint cache.
- If the phone shows another app (the user may be using it), stop UI testing and do not screenshot.
- Screenshots can capture the user's private notifications. Never quote them, and delete such screenshots from build/.

Never tap the destructive "Delete permanently" / "Empty trash" actions on real user files during testing.
