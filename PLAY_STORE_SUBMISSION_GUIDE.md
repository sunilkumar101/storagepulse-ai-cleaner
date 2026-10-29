# StoragePulse AI: Google Play submission guide

Copy-paste answers for Play Console. Package: `com.storagepulse.cleaner` · version 1.0.0 (versionCode 1) · targetSdk 36.

Privacy Policy URL: **https://sunilkumar101.github.io/storagepulse-ai-cleaner/privacy-policy.html**
Contact email: **skkhatri2024@gmail.com**

---

## 0. Before you upload (blocking)

1. **Real AdMob ids.** Release builds fall back to Google's *test* ids until you set them. Put them in `~/.gradle/gradle.properties` (never in the repo):
   ```properties
   ADMOB_APP_ID=ca-app-pub-XXXXXXXXXXXXXXXX~XXXXXXXXXX
   ADMOB_BANNER_ID=ca-app-pub-XXXXXXXXXXXXXXXX/XXXXXXXXXX
   ADMOB_INTERSTITIAL_ID=ca-app-pub-XXXXXXXXXXXXXXXX/XXXXXXXXXX
   ADMOB_REWARDED_ID=ca-app-pub-XXXXXXXXXXXXXXXX/XXXXXXXXXX
   ```
   Then rebuild: `./gradlew clean bundleRelease`.
2. **AdMob consent message.** In AdMob → Privacy & messaging, create a GDPR (EEA/UK/CH) message for this app. The app already calls Google UMP before loading ads and shows "Ad privacy choices" in Settings when required.
3. **In-app product.** Play Console → Monetize → Products → In-app products → create `storagepulse_pro_lifetime`, one-time, US$2.99, then Activate. (Needs a Payments profile.)
4. **Back up the upload key**: `keystore/storagepulse-upload.jks` + the password in `keystore/KEYSTORE_CREDENTIALS.txt` (git-ignored).
5. **First upload is manual.** Create the app in Play Console and upload the first `app/build/outputs/bundle/release/app-release.aab` in the web UI (Internal testing). After that `bundle exec fastlane release` works.
6. Opt in to **Play App Signing** (default). The upload certificate SHA-256 is in `keystore/KEYSTORE_CREDENTIALS.txt`.
7. Promo video: upload `playstore_assets/storagepulse_promo_16x9.mp4` to YouTube (public or unlisted, ads off) and paste the URL in Store listing → Video.

---

## 1. All files access (MANAGE_EXTERNAL_STORAGE) declaration

Play Console → App content → **All files access permission**.

**Core functionality:** *File management / Disk usage analyser (device storage analysis and cleanup)*

**Describe the core feature that requires this permission** (≈500 chars):
> StoragePulse AI is a disk usage analyzer and storage cleaner. Its core feature is a full-device treemap/sunburst map that measures the size of every folder on shared storage, including documents, APK installers, downloads and app media folders (WhatsApp, Telegram). It finds exact duplicate files across folders, removes leftover APKs and redundant chat media, moves files into a 30-day Safe Trash quarantine folder, and replaces videos with compressed copies. None of this is possible with MediaStore, which only exposes photos, videos and audio.

**Why a more privacy-friendly alternative (MediaStore / SAF) is not enough:**
> MediaStore cannot list non-media files (documents, .apk, archives) or report folder sizes, so the storage map would miss large parts of the disk. The Storage Access Framework requires the user to pick folders one at a time and cannot measure the whole device or detect duplicates across folders. Users who do not want to grant All files access can choose "Scoped Mode" (photos and videos only, via READ_MEDIA_*), and the app keeps working with reduced features.

**How the user is informed:** before requesting access the app shows a full-screen disclosure ("Why StoragePulse needs file access") with the treemap, on-device processing and Safe Trash explanations, and a "Scan media only (Scoped Mode)" alternative.

**Data handling:** files are processed on the device only and never uploaded (see Privacy Policy section 2–3).

**Demo video** (Play asks for one): record a 30–60 s screen capture: onboarding disclosure → grant All files access → Dashboard → Treemap drill-down → Purge to Safe Trash → Restore. Upload it to YouTube (unlisted) or Google Drive and paste the link.

---

## 2. Data safety form

Play Console → App content → **Data safety**. Answers reflect the Google Mobile Ads SDK (AdMob) in the free version; the app itself sends nothing.

**Data collection and security**
| Question | Answer |
|---|---|
| Does your app collect or share any of the required user data types? | **Yes** (through the Google Mobile Ads SDK) |
| Is all of the user data collected by your app encrypted in transit? | **Yes** |
| Do you provide a way for users to request that their data is deleted? | **No** (we keep no data on servers; on-device data is deleted by uninstalling; advertising ID can be reset in Android settings) |

**Data types** (mark *Collected* = Yes, *Shared* = Yes, *Processed ephemerally* = No, *Required* = Yes, because free users always get ads):
| Category → type | Purposes |
|---|---|
| Location → **Approximate location** | Advertising or marketing, Fraud prevention, security and compliance |
| App activity → **App interactions** | Advertising or marketing, Analytics |
| App info and performance → **Diagnostics** | Analytics |
| Device or other IDs → **Device or other IDs** | Advertising or marketing, Analytics, Fraud prevention, security and compliance |

**Not collected** (leave unchecked): Personal info, Financial info (Google Play handles payments), Health and fitness, Messages, **Photos and videos**, Audio files, **Files and docs**, Calendar, Contacts, Web browsing, App activity → installed apps / other user-generated content, Search history.

> Photos, videos and files are *accessed* on the device but never leave it, so per Google's definition they are **not collected**.

---

## 3. Other App content declarations

| Section | Answer |
|---|---|
| Privacy policy | https://sunilkumar101.github.io/storagepulse-ai-cleaner/privacy-policy.html |
| Ads | **Yes, my app contains ads** |
| Advertising ID | **Yes** → purposes: Advertising or marketing, Analytics, Fraud prevention |
| App access | All functionality is available without special access (no login). Note for reviewers: grant "All files access" or choose Scoped Mode on the first screen. |
| Content rating | Utility / productivity; no violence, sex, gambling, user-to-user communication or location sharing → expected **Everyone / PEGI 3** |
| Target audience | **18+** (or 13+); not designed for children |
| News app | No |
| Government app | No |
| Financial features | None |
| Health | None |
| Data deletion URL (if asked) | https://sunilkumar101.github.io/storagepulse-ai-cleaner/privacy-policy.html |

Usage Access (`PACKAGE_USAGE_STATS`), `REQUEST_DELETE_PACKAGES`, `USE_BIOMETRIC` and `POST_NOTIFICATIONS` need no Play declaration form; they are explained in the Privacy Policy.

---

## 4. Store listing

| Field | Value |
|---|---|
| App name | `StoragePulse: Disk Usage Map` (28/30) |
| Short description | `fastlane/metadata/android/en-US/short_description.txt` (80/80) |
| Full description | `fastlane/metadata/android/en-US/full_description.txt` (3,959/4,000) |
| App icon 512×512 | `fastlane/metadata/android/en-US/images/icon.png` |
| Feature graphic 1024×500 | `fastlane/metadata/android/en-US/images/featureGraphic.png` |
| Phone screenshots (8, 1080×1920) | `fastlane/metadata/android/en-US/images/phoneScreenshots/` |
| Category | Tools |
| Tags | Storage cleaner, File manager, Photo gallery |
| Release notes | `fastlane/metadata/android/en-US/changelogs/1.txt` |

Avoid other apps' names in the title or description (Play metadata / impersonation policy).

---

## 5. Upload with fastlane (after the first manual upload)

```bash
gem install bundler
bundle install
# play-service-account.json in the project root (Play Console > Setup > API access), git-ignored
./gradlew clean bundleRelease
bundle exec fastlane release                    # internal testing
bundle exec fastlane release track:production   # creates a DRAFT production release for you to review
bundle exec fastlane listing                    # text + graphics only
```
