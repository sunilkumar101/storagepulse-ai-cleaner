# StoragePulse AI

Disk usage treemap, AI duplicate photo cleaner, H.265 video compressor and biometric vault for Android. All file processing happens on the device.

- **Package:** `com.storagepulse.cleaner` · minSdk 26 · targetSdk 36 (Android 16) · 16 KB page-size ready
- **Stack:** Kotlin, Jetpack Compose Material 3, Jetpack Glance widgets, Hilt, Room, WorkManager, Media3 Transformer, Google Play Billing 8, Google Mobile Ads + UMP
- **Privacy Policy:** https://sunilkumar101.github.io/storagepulse-ai-cleaner/privacy-policy.html

## Features

Storage treemap & sunburst · AI duplicate / burst / blurry photo cleaner with Smart Keeper · swipe-to-clean large videos · H.265 compressor with before/after preview · WhatsApp & Telegram media cleaner · fast offline gallery, GPS stripping and AES-256-GCM vault · 30-day Safe Trash · unused apps finder · 3 home-screen widgets · optional one-time Pro upgrade.

## Build

Requires Android Studio's JDK 21.

```bash
export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"   # Windows Git Bash example
./gradlew testDebugUnitTest assembleDebug
```

Release builds are signed when `keystore/keystore.properties` exists (git-ignored):

```properties
storeFile=../keystore/storagepulse-upload.jks
storePassword=...
keyAlias=storagepulse-upload
keyPassword=...
```

Production AdMob ids are read from Gradle properties (`ADMOB_APP_ID`, `ADMOB_BANNER_ID`, `ADMOB_INTERSTITIAL_ID`, `ADMOB_REWARDED_ID`); without them release builds use Google's test ids.

```bash
./gradlew clean bundleRelease   # app/build/outputs/bundle/release/app-release.aab
```

## Repository layout

| Path | What |
|---|---|
| `app/` | Android app |
| `docs/` | GitHub Pages site: privacy policy, terms, data safety |
| `fastlane/` | Play Store listing metadata, screenshots, `Fastfile` / `Appfile` |
| `playstore_assets/` | Rendered store screenshots, feature graphic, icon, promo videos |
| `promo-video/` | Remotion source for the promo video and store graphics |
| `PLAY_STORE_SUBMISSION_GUIDE.md` | Play Console declarations and Data safety answers |

Never commit `keystore/`, `*.jks` or `play-service-account.json`.
