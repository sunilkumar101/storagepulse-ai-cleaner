---
description: Implement a Stitch design screen in Jetpack Compose Material 3
argument-hint: <screen folder name in design_screenshots/>
---

Target screen: $ARGUMENTS

Read `design_screenshots/stitch_storagepulse_ai_cleaner_app/obsidian_telemetry/DESIGN.md` and the target screen's `code.html` + `screen.png` in `design_screenshots/stitch_storagepulse_ai_cleaner_app/<screen>/` and implement the exact layout in Jetpack Compose Material 3:

- Background `#090D16`, cards `#111827`, card stroke `#1F2937`
- Emerald `#10B981`, indigo `#6366F1`, rose `#F43F5E`, amber `#F59E0B` (category colors: see CLAUDE.md section 4)
- Fonts: Plus Jakarta Sans (UI text) + JetBrains Mono (numbers, sizes, paths)

Rules:
- All badges/chips use `maxLines = 1` (with `overflow = TextOverflow.Ellipsis`).
- Destructive buttons: red `#EF4444` container with bright white `#FFFFFF` text.
- Use theme tokens from the app's `ui/theme` package, not hard-coded colors in screens.
- Apply `WindowInsets.systemBars` padding (edge-to-edge, Android 16).
- Read only the one target screen's files, not every screen.
