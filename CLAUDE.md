# StoragePulse AI — Project Rules

**App:** StoragePulse AI: DiskUsage, Duplicate Cleaner & Fast Vault
**Package:** `com.storagepulse.cleaner`
**Stack:** 100% Kotlin, Jetpack Compose Material 3, Jetpack Glance (widgets). Native Android, commercial release on Google Play.

Deliverables in scope: the Android app (live-tested on a connected phone via ADB), a Remotion promo video for the store listing autoplay, a GitHub repo with a GitHub Pages privacy policy, and a complete Play Store ASO release.

---

## 1. Slash commands (`.claude/commands/`)

| Command | Use it for |
|---|---|
| `/verify-build` | `./gradlew assembleDebug`, fix compile/resource/manifest/lint errors in the affected files only |
| `/stitch-ui <screen>` | Build one Stitch design screen in Compose, pixel-matched to `code.html` + `screen.png` |
| `/adb-live-test [screen]` | Install on the phone, drive UI with `adb shell input tap`, screenshot to `build/live_screen.png`, read `logcat *:E`, fix |
| `/check-android16` | Audit + fix API 36 compliance (checklist in section 6) |

Run `/verify-build` after every meaningful code change and `/adb-live-test` after every finished screen.

## 2. Installed skills (`.agents/skills/`, symlinked for Claude Code)

- **Remotion** (`remotion-dev/skills`): `remotion-create`, `remotion-best-practices`, `remotion-render`, `remotion-studio`, `remotion-captions`, `remotion-multimedia`, etc. — use for the promo video in `/promo-video/`.
- **claude-motion** (`whaleyxbt/claude-motion`): `review-loop`, `sound-design`, `web-sims`.
- **LottieFiles** (`LottieFiles/motion-design-skill`): `motion-design` (this replaced claude-motion's skill of the same name).

## 3. Reference repos (`/reference_repos/`, git-ignored)

Shallow clones for **reading algorithms only**. Write 100% original Kotlin + Compose code — never copy code. sdmaid-se, Fossify Gallery and Amaze are **GPL-3.0**; copying from them would force our commercial app to be GPL. diskusage is Apache-2.0 but we still write our own.

**TOKEN-SAVING RULE:** never read or search an entire repo. Open only these files/folders, and only when the task needs them:

| Topic | Path |
|---|---|
| Treemap layout math | `reference_repos/diskusage/app/src/main/java/com/google/android/diskusage/filesystem/entity/FileSystemEntry.java` |
| Perceptual image hash (duplicates) | `reference_repos/sdmaid-se/app-tool-deduplicator/src/main/java/eu/darken/sdmse/deduplicator/core/scanner/phash/` (note: sdmaid-se uses a pHash; we implement our own dHash) |
| StorageStatsManager / app storage | `reference_repos/sdmaid-se/app-tool-analyzer/` (there is no `app-tool-storageanalyzer` module) |
| Fast MediaStore queries + Vault | `reference_repos/FossifyGallery/app/src/main/kotlin/org/fossify/gallery/helpers/` |
| File operations (if needed) | `reference_repos/AmazeFileManager/` — targeted file only |

## 4. Design system — Stitch screens (`./design_screenshots/`)

Export root: `design_screenshots/stitch_storagepulse_ai_cleaner_app/` (below: `<root>`).
Source of truth: `<root>/obsidian_telemetry/DESIGN.md` plus each screen's `code.html` + `screen.png`.

**Obsidian Telemetry tokens** (from DESIGN.md prose; use these, not the Material auto-palette in its YAML header)
- Canvas `#090D16` · Cards/surfaces `#111827` · 1px card stroke `#1F2937`
- Emerald `#10B981` (primary / clean / reclaimed space) · Indigo `#6366F1` (AI, duplicates, vault, compression) · Amber `#F59E0B` (warnings, cache, stale APKs)
- Category colors: Video `#F43F5E` rose · Photo `#8B5CF6` violet · Apps/APK `#06B6D4` cyan · Audio/temp `#F59E0B` amber · Social `#10B981` emerald
- Destructive buttons: `#EF4444` container, `#FFFFFF` text
- Fonts: **Plus Jakarta Sans** (titles, body) + **JetBrains Mono** (every GB/MB/%/count, plus uppercase state labels like `SCANNING`, `VAULT LOCKED`)
- Color only in data viz, glyphs, badges and progress tracks — never full-saturation card backgrounds.
- Layout: 8pt grid, 16dp margins (24dp above 600dp width), 88dp bottom clearance above the bottom nav.
- Bottom nav: Dashboard · Treemap · AI Clean · Gallery · Tools.
- All badges/chips: `maxLines = 1` + ellipsis. No text clipping anywhere.

**The 10 screens**

| # | Screen | Folder in `<root>/` |
|---|---|---|
| 1 | Storage access + offline privacy disclosure (onboarding) | `storage_access_offline_privacy_disclosure` |
| 2 | Main storage command center (dashboard) | `main_storage_command_center` |
| 3 | Interactive DiskUsage treemap / sunburst explorer | `interactive_diskusage_treemap_sunburst_explorer` |
| 4 | AI duplicate & blurry photo cleaner | `ai_duplicate_blurry_photo_cleaner` |
| 5 | Swipe-to-clean large media reviewer | `swipe_to_clean_large_media_reviewer` |
| 6 | Social media / chat storage cleaner | `social_media_chat_storage_cleaner` |
| 7 | Lossless video & photo size compressor | `lossless_video_photo_size_compressor` |
| 8 | Fast offline gallery + biometric vault | `fast_offline_gallery_biometric_vault` |
| 9 | Safe recycle bin + unused apps analyzer | `safe_recycle_bin_unused_apps_analyzer` |
| 10 | Home screen widgets + Pro + settings | `home_screen_widgets_pro_settings` |

**Pro upgrade (from screen 10):** one-time $2.99 lifetime purchase via Google Play Billing — ad-free, batch compression, automated weekly purge, all widget layouts.

## 5. Jetpack Glance home screen widgets (3)

Designed in `<root>/home_screen_widgets_pro_settings/`:

1. **StoragePulse 4×2** — used/total GB with % badge, segmented category bar (Videos, Photos, WA, Apps, Sys), "Recoverable cache" size and a **Boost** button.
2. **Daily Clutter 2×2** — clutter size, screenshot count, heavy video count, and **Swipe to Purge** (opens the swipe reviewer).
3. **Minimal Dock Strip 4×1** — free space, Safe Trash size, and **Rescan**.

All widgets use the Obsidian Telemetry colors, update via WorkManager (not tight loops), and respect Glance size buckets. Widget actions (Boost, Purge) only move items into Safe Trash — a widget never deletes anything permanently.

## 6. 30-Day Safe Trash quarantine rule (non-negotiable)

- **Nothing the user "deletes" in the app is deleted immediately.** Files are moved into the app's Safe Trash (Android 11+: `MediaStore.createTrashRequest` for media; app-private quarantine folder otherwise), with original path + timestamp recorded in Room.
- Items stay restorable for **30 days** (default; Settings lets the user pick 7 / 30 / 60 days), then a WorkManager job purges them.
- The Pro "Automated Weekly Purge" also only moves items into Safe Trash; it never skips quarantine.
- Permanent "Delete now" / "Empty trash" requires an explicit confirmation dialog with a red `#EF4444` / white `#FFFFFF` button that states the count and size.
- Duplicate cleaner always keeps at least one copy of every group; auto-select never selects all copies.
- During `/adb-live-test`, never trigger permanent deletion of real user files.

## 7. Android 16 (API 36) compliance

- `compileSdk = 36`, `targetSdk = 36`
- 16 KB page size: `ndkVersion` ≥ 28.2.13676358, `jniLibs.useLegacyPackaging = false`
- `enableEdgeToEdge()` + `WindowInsets.systemBars` padding on every screen
- `android:enableOnBackInvokedCallback="true"` + Compose `BackHandler` / predictive back
- No orientation locks; responsive on phones, foldables, tablets, landscape
- Storage access via scoped storage / MediaStore / SAF; `MANAGE_EXTERNAL_STORAGE` only if justified for the Play "All files access" declaration (file-manager / disk-analyzer use case).

## 8. Release, repo & privacy

- `.gitignore` already excludes `/reference_repos/`, `/promo-video/node_modules/`, `/promo-video/out/`, build output, `.gradle/`, `local.properties`, `*.jks`, `*.keystore`, `play-service-account.json`. Never commit signing keys or the Play service-account JSON.
- Privacy policy lives on GitHub Pages; the app processes files **on-device only** — the policy, Play Data Safety form and store listing must all say the same thing.
- Promo video lives in `/promo-video/` (Remotion), rendered to `/promo-video/out/`.

## 9. Build setup (this machine)

- AGP 8.13.2 · Gradle 8.13 · Kotlin 2.3.20 · KSP 2.3.12 · Hilt 2.58 · Compose BOM 2026.06.01 · build-tools 36.0.0 (35.0.0 on this SDK is corrupt).
- Build with Android Studio's JDK 21: `export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"` (system JDK 25 breaks Gradle 8.13).
- Billing Library is v8 (`billing-ktx 8.3.0`): Play requires v8+ for new apps/updates after 31 Aug 2026.
- AdMob: debug = Google TEST ids; release reads `ADMOB_*` gradle properties (falls back to test ids). Set real ids + UMP consent before release.
- Plus Jakarta Sans is not bundled yet (`PlusJakartaSans = FontFamily.SansSerif` in `ui/theme/Type.kt`). JetBrains Mono is in `res/font/`.

## 10. Implemented so far

- Phase 1: Gradle/API 36 setup, Obsidian theme, bottom nav, Screen 1 (storage disclosure, full + scoped access).
- Phase 2: `data/storage/StorageScannerRepository` (16-way parallel walk, MediaStore fallback, ~52k files in ~5 s on the test phone), `data/trash/SafeTrashRepository` + Room `trash_items` + daily `TrashPurgeWorker` (quarantine folder `/storage/emulated/0/.StoragePulseTrash`, survives uninstall), Screen 2 dashboard. Instrumented tests: `app/src/androidTest/.../SafeTrashRepositoryTest.kt`.
- Phase 3: Screen 3 `ui/screens/treemap/` — own squarified layout (`SquarifiedLayout.kt`), 2-level `TreemapCanvas`, 3-ring `SunburstCanvas`, breadcrumbs, Back = zoom out, inspector, View sheet, Compress → `Routes.compressor(folder)`, Purge → `data/clean/ClutterCleaner` (byte-by-byte duplicate check before moving; files that fail are dropped from clutter). Scanner now keeps `clutter` + `duplicateGroups` and patches the tree live via `applyRemoval`.
- Purge only ever moves *clutter* (duplicate extras, WhatsApp Sent copies, APK installers, thumbnails) — never a whole folder.
- Phase 4: `data/dedup/AiDeduplicatorEngine` (dHash 9×8 + Laplacian sharpness on ~256 px thumbnails, SHA-256 only for same-size files, Room `media_fingerprints` cache, DB v2 + `MIGRATION_1_2`), Screen 4 `ui/screens/clean/AiDuplicateCleanerScreen` (AI Clean tab), Screen 5 `SwipeToCleanScreen` (Media3 ExoPlayer on a TextureView, swipe up = Safe Trash, down = keep, Undo restores). Every exact/similar group always keeps ≥1 photo; old screenshots are never pre-selected.
- Phase 4 numbers on the test phone (17,114 photos): first analysis 6m 9s (EXIF thumbnails first, sampled decode fallback, all measured in a 160 px box), cached re-analysis 2 s. Sharpness scale tuned on real data (SHARPNESS_SCALE 1500, BLUR_SCORE_THRESHOLD 8).
- Safe Trash quarantines on the SAME volume as the file (`<volume>/.StoragePulseTrash`, incl. SD cards) so moves are instant renames.
- Known limitation: a few videos whose decoder ignores rotation metadata play sideways in the Swipe Deck (poster is upright). Long analyses only run while the screen is on — move to WorkManager in a later phase.
- Phase 5: Screen 6 `ui/screens/compressor/` + `data/compressor/MediaCompressorEngine` (Media3 Transformer, HW HEVC → H.264 fallback, 3 tiers, real 2 s preview with PSNR/projected size/speed, writes next to the original then swaps; originals → Safe Trash or kept as-is, never hard-deleted). Rewarded ad gate `data/ads/RewardedAdGate` (Google TEST unit id, never blocks if no ad; TODO UMP consent before release). Screen 7 `ui/screens/social/` + `data/social/SocialChatCleanerRepository` (WhatsApp/WA Business, Telegram, TikTok/CapCut, Instagram roots; Sent + Status + voice >90 days pre-selected, received videos never). Shared sub-nav `ui/components/CleanerSubNav`.
- Test phone: 26.7 MB/10 s clip → 5.9 MB at Balanced 1080p, 40 dB PSNR, encoder OMX.qcom.video.encoder.hevc ~0.7× real-time; WhatsApp scan 38k files ≈ 7 s.
- Phase 6: Screen 8 `ui/screens/gallery/` (FastGalleryAndVaultScreen + CropEditorDialog + VaultScreen) on `data/gallery/GalleryRepository` (one MediaStore Files query grouped in memory, ~0.8 s for 17.7k items; cover pins in DataStore "gallery"; GPS strip = framework ExifInterface, JPEG only; crop saves a new `_crop.jpg`). Vault `data/vault/` = AES-256-GCM Keystore key, 1 MiB chunks, encrypt → decrypt-verify SHA-256 → delete original; files in `filesDir/private_vault` (lost on uninstall). Room DB v3 (`vault_items`, `MIGRATION_2_3`). `MainActivity` is a `FragmentActivity` (BiometricPrompt). Screen 9 `ui/screens/tools/SafeTrashAndAppsScreen` on `data/apps/AppSpaceRepository` (user-installed launcher apps only — system apps, even updated ones, are excluded because uninstall would only drop updates; `<queries>` launcher intent, no QUERY_ALL_PACKAGES).
- Phase 7: Screen 10 `ui/screens/settings/WidgetsAndProSettingsScreen` (route `settings`, opened from the Dashboard avatar) with Compose previews of the widgets. 3 Glance widgets in `ui/widgets/` (StorageBoost 4×2 + DailyClutter 2×2 = Pro, MinimalStrip 4×1 = free; locked layout for free users) read `data/widgets/WidgetDataRepository` (DataStore "widget"; volume stats + 2 MediaStore queries + last scan). `WidgetRefreshWorker` every 6 h, on app stop and on the strip's Rescan; widget buttons deep-link via `MainActivity.EXTRA_ROUTE` → `ui/navigation/DeepLinks`. Pro = `data/billing/ProRepository` (Billing **v8** — Play requires 8+; product `storagepulse_pro_lifetime`, cached in DataStore "pro", debug-only override switch). Ads = `ads/AdMobManager` (BuildConfig ids: test ids in debug, gradle properties `ADMOB_APP_ID/ADMOB_BANNER_ID/ADMOB_INTERSTITIAL_ID/ADMOB_REWARDED_ID` in release, falling back to test ids); adaptive banner above the bottom nav on tab screens, interstitial after an AI Clean move (3-min cap, never first 3 min), rewarded before batch compression; Pro hides/skips all. Accent (Obsidian/Indigo/Amber) recolors `colorScheme.primary*`, the nav bar and widgets — most screens still use fixed Emerald tokens. `notify/WeeklyClutterWorker` Sunday 20:00 (off by default). Retention change applies to newly trashed files only.
- Glance widgets: no `cornerRadius` before API 31 — use `drawable/widget_bg` / tinted `widget_round`. WorkManager won't force-run periodic work early; test workers with `TestListenableWorkerBuilder` (`notify/WeeklyClutterWorkerTest`).
- Phase 8 (live audit): fixed sticky onboarding CTA, redirect to onboarding when access is revoked while the app is away (`AppNavHost` resume check), Dashboard Swipe Deck badge now counts MediaStore ≥100 MB videos incl. SD card (same list as the deck), treemap root crumb "Internal" + 48dp root button, 2-line sunburst centre, Pro-aware compressor CTA, plurals, trash "days left" rounds up. Store screenshots (personal media blurred) in `playstore_assets/raw_screenshots/`.
- Phase 9 (store assets): Remotion 4.0.539 project in `promo-video/` (Studio: `.claude/launch.json` → `promo-studio`, port 3000). Compositions `StoreBanner16x9` / `StoreBanner9x16` (18 s, 60 fps, 5 scenes in `src/scenes/`, TransitionSeries), stills `Shot01…Shot08`, `FeatureGraphic`, `HiResIcon`. Render with `npm run render:all` (or `render:16x9`, `render:9x16`, `render:shots`, `render:graphics`) → `playstore_assets/`. Review loop: `scripts/sheet.py <mp4> <prefix> [times]` builds labelled 4×2 contact sheets from a `--scale=0.5` draft. Store copy stays factual (no "0.4 s", no invented quality %); treemap/video sizes in the video are labelled demo data. Blurred personal media in real screenshots is covered with original vector demo art (`SceneArt`), never stock or user photos. Launcher icon = store icon (storage ring + pulse).
- Cold start: debug build ~4.4 s (unoptimised debug code), release-like `staging` build (R8, debug-signed: `./gradlew installStaging`) ~0.5 s. Use `staging` for perf checks and R8 smoke tests. AdMob `OPTIMIZE_INITIALIZATION` / `OPTIMIZE_AD_LOADING` flags are on.
- After changing files, call `data/storage/scanAndWait()` (not fire-and-forget `scanFile`) before reloading MediaStore, or the gallery shows stale rows.

### `/adb-live-test` notes (this machine)
- adb is not on PATH in Git Bash: use `/c/Users/sunil/AppData/Local/Android/Sdk/platform-tools/adb.exe` and `export MSYS_NO_PATHCONV=1` (else `/sdcard/...` becomes a Windows path).
- Before every tap, check `dumpsys window | grep mCurrentFocus` is `com.storagepulse.cleaner`; `am start` can silently leave the launcher in front. Hide the keyboard (`keyevent 4`) before tapping list items.
- Instrumented tests without uninstalling the app: `./gradlew installDebug installDebugAndroidTest` then `adb shell am instrument -w -e class <classes> com.storagepulse.cleaner.test/androidx.test.runner.AndroidJUnitRunner`.
- The vault, Usage Access and screen unlock need the user (fingerprint/PIN, settings toggle). Never enter their PIN.
- Test media goes in `Pictures/SPTest` / `Movies/SPTest` named `SPTEST_*`; delete it afterwards and rescan so no MediaStore rows remain.
- Index pushed test files with `adb shell content call --uri content://media/external/file --method scan_file --arg <path>`. The `MEDIA_SCANNER_SCAN_FILE` broadcast leaves `is_pending=1` rows that other apps cannot see.
- OPPO blocks `adb shell appops set` (no MANAGE_APP_OPS_MODES): toggle our own All Files / Usage Access in the Settings page instead. Revoking All Files Access kills the process.
- Long lists (AI Clean, compressor picker sheet): scroll from the middle of the list; flings that start on the sticky bar or drag a bottom sheet down dismiss/miss.

## 11. Working rules

- Edit only the files a task needs; keep fixes minimal.
- Read a design screen's files only when building that screen.
- Every new screen: `/stitch-ui` → `/verify-build` → `/adb-live-test`.
