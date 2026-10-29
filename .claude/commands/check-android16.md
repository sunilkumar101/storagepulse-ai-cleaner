---
description: Audit the project for Android 16 (API 36) compliance and fix gaps
---

Verify full Android 16 (API Level 36) compliance:

- [ ] `compileSdk = 36`, `targetSdk = 36`
- [ ] 16 KB memory page size alignment: `ndkVersion` 28.2.13676358 or newer, and `packaging { jniLibs { useLegacyPackaging = false } }`
- [ ] `enableEdgeToEdge()` in every Activity, with proper `WindowInsets.systemBars` padding on all screens (no content under status/nav bars)
- [ ] `android:enableOnBackInvokedCallback="true"` in the manifest, with Compose `BackHandler` / predictive back where screens intercept back
- [ ] Responsive layouts without orientation locks (no `android:screenOrientation`, no `resizeableActivity="false"`); layouts work on tablets/foldables and in landscape

For each item report PASS/FAIL with the file and line, then fix every FAIL. Finish by running `/verify-build`.
