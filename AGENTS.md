# AGENTS.md — mikaScreenlock

This file provides context and operational conventions so any coding agent can seamlessly pick up development on `mikaScreenlock`.

## Project Overview

- **App Name**: `mikaScreenlock`
- **Package / Application ID**: `com.example.screenlock` (**do not change** unless explicitly requested, so existing installs update cleanly in-place without losing permissions)
- **Tech Stack**: Android (Kotlin 2.3.20, AGP 9.0.1, Gradle 9, Jetpack Compose + Material 3)
- **SDK Targets**: `minSdk = 26`, `targetSdk = 34`, `compileSdk = 36`

## Core Architecture

All app logic lives under `app/src/main/kotlin/com/example/screenlock/`:

- `MainActivity.kt`: Jetpack Compose UI, permission onboarding (`POST_NOTIFICATIONS`, `SYSTEM_ALERT_WINDOW`, Accessibility Service check), mode switching, service start, and "Lock Now" trigger.
- `LockAccessibilityService.kt`: Primary lock controller. Intercepts physical volume keys (`KEYCODE_VOLUME_DOWN` ×2 within 1200ms to lock; `KEYCODE_VOLUME_UP` ×2 within 1200ms while locked to unlock), blocks other hardware keys while locked, and collapses the status bar / notification shade (`extremeCollapse()`) if SystemUI tries to appear.
- `LockOverlayManager.kt`: Manages the fullscreen `TYPE_ACCESSIBILITY_OVERLAY` view (dim mode `Color.argb(249, 0, 0, 0)` vs transparent mode `Color.TRANSPARENT`) and the 2-second pill-shaped status toast.
- `LockService.kt`: Foreground service (`foregroundServiceType="specialUse"`, subtype `screenlock`) with persistent notification and a fallback `ContentObserver` volume triple-press listener.
- `LockTileService.kt`: Quick Settings Tile (`QS_TILE`) for locking from the notification shade.
- `ui/theme/`: `Color.kt`, `Theme.kt`, `Type.kt` — soft, eye-friendly **Warm Mist (`#F6F8F5`) × Eucalyptus Sage (`#6B9080`)** palette.

## Critical Invariants

1. **Preserve `applicationId`**: Keep `com.example.screenlock` so `adb install -r` updates in-place on the user's phone.
2. **Preserve overlay parameters**: In `LockOverlayManager.kt`, keep `TYPE_ACCESSIBILITY_OVERLAY`, `flags = -2130704504`, `layoutInDisplayCutoutMode = SHORT_EDGES`, and `systemUiVisibility = 4871` unless specifically modifying overlay behavior.
3. **Visual Style**: Keep the UI soft, calm, and low-glare (Warm Mist background, rounded white cards, soft sage accents, no harsh dark action bar). App icon assets live in `res/drawable-nodpi/ic_app_logo.png`, `res/mipmap-*/ic_launcher*.png`, and `res/drawable/ic_tile_lock.xml`.
4. **APK Signing**: Debug builds on this Mac share `~/.android/debug.keystore`. Building on a different machine or VPS with a different debug keystore will require uninstalling the app before installing unless a shared signing keystore is configured.

## Build & USB Install Commands (macOS)

System `java` may not be configured globally; use Homebrew OpenJDK 21 explicitly:

```bash
JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home ./gradlew assembleDebug
cp app/build/outputs/apk/debug/app-debug.apk mikaScreenlock.apk
```

Install onto a USB-debugging connected Android device and launch:

```bash
~/Library/Android/sdk/platform-tools/adb install -r mikaScreenlock.apk
~/Library/Android/sdk/platform-tools/adb shell am start -n com.example.screenlock/.MainActivity
```

## Version Control & Deployment

This repository syncs to **two** remotes — always push to both after committing:

- `origin` (GitHub): `git@github.com:mikazukitw-hub/mikaScreenlock.git`
- `vps` (VPS working tree): `ssh://my-vps/root/projects/mikaScreenlock` (configured with `receive.denyCurrentBranch=updateInstead`, so pushing directly updates the working directory on the VPS)

```bash
git push origin main && git push vps main
```

Note: `*.apk` and `app/build/` are git-ignored.

## Known Technical Notes (Future Improvements)

- `LockService` and `LockAccessibilityService` currently instantiate separate `LockOverlayManager` instances; unifying overlay state under `LockAccessibilityService` (or a shared singleton) will prevent desync when locking from the notification action vs. volume keys.
- `LockTileService.onClick()` currently uses `startForegroundService` targeting `LockAccessibilityService`; switching to `startService` (or delegating via `LockAccessibilityService.instance`) avoids potential Android FGS timeout exceptions.
- Selected lock mode (`isTransparentMode`) is currently in-memory only and resets on app restart; persisting via `SharedPreferences` / DataStore is a recommended enhancement.
