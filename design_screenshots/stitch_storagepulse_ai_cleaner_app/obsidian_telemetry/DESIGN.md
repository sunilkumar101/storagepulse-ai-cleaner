---
name: Obsidian Telemetry
colors:
  surface: '#0f131c'
  surface-dim: '#0f131c'
  surface-bright: '#353943'
  surface-container-lowest: '#0a0e17'
  surface-container-low: '#181b25'
  surface-container: '#1c1f29'
  surface-container-high: '#262a34'
  surface-container-highest: '#31353f'
  on-surface: '#dfe2ef'
  on-surface-variant: '#bbcabf'
  inverse-surface: '#dfe2ef'
  inverse-on-surface: '#2c303a'
  outline: '#86948a'
  outline-variant: '#3c4a42'
  surface-tint: '#4edea3'
  primary: '#4edea3'
  on-primary: '#003824'
  primary-container: '#10b981'
  on-primary-container: '#00422b'
  inverse-primary: '#006c49'
  secondary: '#c0c1ff'
  on-secondary: '#1000a9'
  secondary-container: '#3131c0'
  on-secondary-container: '#b0b2ff'
  tertiary: '#ffb95f'
  on-tertiary: '#472a00'
  tertiary-container: '#e29100'
  on-tertiary-container: '#523200'
  error: '#ffb4ab'
  on-error: '#690005'
  error-container: '#93000a'
  on-error-container: '#ffdad6'
  primary-fixed: '#6ffbbe'
  primary-fixed-dim: '#4edea3'
  on-primary-fixed: '#002113'
  on-primary-fixed-variant: '#005236'
  secondary-fixed: '#e1e0ff'
  secondary-fixed-dim: '#c0c1ff'
  on-secondary-fixed: '#07006c'
  on-secondary-fixed-variant: '#2f2ebe'
  tertiary-fixed: '#ffddb8'
  tertiary-fixed-dim: '#ffb95f'
  on-tertiary-fixed: '#2a1700'
  on-tertiary-fixed-variant: '#653e00'
  background: '#0f131c'
  on-background: '#dfe2ef'
  surface-variant: '#31353f'
typography:
  headline-xl:
    fontFamily: Plus Jakarta Sans
    fontSize: 32px
    fontWeight: '700'
    lineHeight: 40px
    letterSpacing: -0.02em
  headline-xl-mobile:
    fontFamily: Plus Jakarta Sans
    fontSize: 26px
    fontWeight: '700'
    lineHeight: 32px
    letterSpacing: -0.02em
  headline-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 22px
    fontWeight: '600'
    lineHeight: 28px
    letterSpacing: -0.01em
  headline-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 18px
    fontWeight: '600'
    lineHeight: 24px
  body-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  body-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  body-sm:
    fontFamily: Plus Jakarta Sans
    fontSize: 12px
    fontWeight: '400'
    lineHeight: 16px
  telemetry-xl:
    fontFamily: JetBrains Mono
    fontSize: 28px
    fontWeight: '700'
    lineHeight: 32px
    letterSpacing: -0.03em
  telemetry-lg:
    fontFamily: JetBrains Mono
    fontSize: 20px
    fontWeight: '600'
    lineHeight: 24px
    letterSpacing: -0.02em
  telemetry-md:
    fontFamily: JetBrains Mono
    fontSize: 14px
    fontWeight: '500'
    lineHeight: 18px
    letterSpacing: -0.01em
  telemetry-sm:
    fontFamily: JetBrains Mono
    fontSize: 11px
    fontWeight: '500'
    lineHeight: 14px
    letterSpacing: 0.02em
  label-caps:
    fontFamily: JetBrains Mono
    fontSize: 10px
    fontWeight: '600'
    lineHeight: 12px
    letterSpacing: 0.08em
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  gutter: 1rem
  margin: 1rem
  space-xs: 0.25rem
  space-sm: 0.5rem
  space-md: 1rem
  space-lg: 1.5rem
  space-xl: 2rem
---

## Brand & Style

This design system is engineered for utility-first precision and cybernetic refinement. Designed for high-volume file explorers, power users, and security-minded mobile operators, the interface balances dense telemetry with effortless visual scanning. 

### Design Direction
The visual identity draws from **Dark Glassmorphism combined with High-Tech Telemetry Systems**. Key characteristics:
- **Obsidian Baseline:** Deep, ink-like dark tones eliminate eye strain, conserve OLED battery life, and establish maximum contrast against vibrant phosphor data points.
- **Instrument Precision:** Avoids decorative skeuomorphism in favor of diagnostic clarity. Visual affordances resemble flight telemetry decks, military computing instruments, and professional hardware monitors.
- **Controlled Luminescence:** Neon accents indicate file category states, automation tasks, and live analyzer diagnostics without causing chromatic noise.

## Colors

The palette leverages a dark-field foundation where vibrant functional colors communicate data classification instantly.

### Palette Architecture
- **Root Canvas (`#090D16`):** Ground level obsidian backdrop. Absolute blacks are avoided to retain depth against card drop shadows.
- **Surface Elevation (`#111827`):** Elevated container layer. Renders at `85%` to `90%` opacity when combined with backdrop filters.
- **Surface Stroke (`#1F2937`):** A hairline 1px structural boundary to demarcate modules in pure dark space.
- **Primary / Clean Target (`#10B981` - Emerald):** Action confirmation, healthy telemetry, reclaimed storage, and social media/WhatsApp partitions.
- **AI Automation (`#6366F1` - Electric Indigo):** Intelligent scanning engines, duplicate neural matching, vault encryption, and automated file compression.
- **Diagnostic / Volatile (`#F59E0B` - Amber):** Warnings, audio streams, transient cache, stale APK warnings, and system residue.

### Category Data Tokens
- **Video Partition:** `#F43F5E` (Rose)
- **Photo Partition:** `#8B5CF6` (Violet)
- **Apps & APK Binaries:** `#06B6D4` (Cyan)
- **Audio & Temporary Cache:** `#F59E0B` (Amber)
- **Social Media Cache:** `#10B981` (Emerald)

### Functional Application Rules
- Telemetry metrics should match the category accent color of the data they represent.
- Never use full-saturation backgrounds for cards; use color exclusively in data visualizations, glyphs, badges, and progress tracks.

## Typography

The typographic hierarchy implements a dual-engine structure: **Plus Jakarta Sans** for spatial structure and interface copy, and **JetBrains Mono** for numerical values and technical metrics.

### Typographic Hierarchy Rules
- **Numerical Data & Values:** Any data representing capacity (`GB`, `MB`, `KB`), percentages (`%`), read/write rates, file counts, and memory locations must be rendered in `JetBrains Mono`.
- **System Labels & Section Headers:** Use uppercase tracking (`label-caps`) in JetBrains Mono for system state labels (e.g., `SCANNING`, `VAULT LOCKED`, `PARTITION MAP`).
- **Headings & Body Copy:** Titles, explanatory subtext, settings, and modal descriptions use `Plus Jakarta Sans` to maintain human readability.

## Layout & Spacing

Layouts follow an 8-point rhythmic grid with a tight 4-point subgrid tailored for high-density information display on mobile devices.

### Layout Principles
- **Grid Architecture:** 4-column fluid layout for mobile (viewports `< 600px`), scaling to 8-column for foldables/tablets (`600px - 1024px`), and 12-column for desktop/tablet landscape.
- **Safe Margins:** Standard canvas margin is `1rem` (16px) on mobile viewports. On larger devices (`> 600px`), section margins expand to `1.5rem` (24px).
- **Edge Offsets:** Persistent bottom navigation docks require a bottom clearance cushion of `5.5rem` (88px) to prevent floating action buttons (FAB) and scrollable lists from overlapping interaction zones.

## Elevation & Depth

Visual hierarchy is maintained via glassmorphic layering, subtle rim lighting, and disciplined luminosity rather than traditional drop shadows.

### Elevation Architecture
- **Base Canvas (Level 0):** Pure `#090D16` matte finish. No blur, no transparency.
- **Interactive Container (Level 1):** Solid background `#111827` mixed with `backdrop-filter: blur(12px)`. Outlined with a 1px solid border of `#1F2937`.
- **Raised Interactive Modules (Level 2):** Elevated cards, scan diagnostic overlays, and bottom sheets use `#1F2937` at `90%` opacity with an outer rim shadow: `0 8px 32px 0 rgba(0, 0, 0, 0.45)`.
- **Focus / Glow States:** When an element enters an active diagnostic state (e.g., active AI scan), it emits a diffused chromatic ring: `0 0 20px 2px rgba(99, 102, 241, 0.25)`.

## Shapes

The interface balances sharp technological precision with organic tactile surfaces through consistent medium-to-large border radiuses.

### Geometry Specifications
- **Standard Cards & Surfaces:** `16px` border-radius (`rounded-2xl`).
- **Interactive Elements & Input Fields:** `12px` border-radius (`rounded-xl`).
- **Pills, Badges & Floating Triggers:** Fully rounded (`9999px` / pill-shaped) to distinguish actionable tags from structural data containers.
- **Treemap Nodes & Gauges:** Strict internal corner synchronization (`8px` on nested cards inside `16px` containers) to preserve geometric proportion.

## Components

### Buttons & Interactive Controls
- **Primary CTA / One-Tap Cleaner:** Background uses `#10B981` (Emerald), text in `#090D16` bold Plus Jakarta Sans. Subtle top inner highlight (`inset 0 1px 0 rgba(255, 255, 255, 0.2)`). Height: 48px or 56px.
- **Secondary / AI Action:** Background uses `#6366F1` (Electric Indigo), text in `#FFFFFF`.
- **Tertiary / Ghost Actions:** 1px border `#1F2937`, transparent fill, text `#9CA3AF`. Hover/press fills with `#111827`.

### Segmented Storage Breakdown Bar
- A continuous 12px or 16px high horizontal bar divided into category slices.
- Slices map to category tokens: Videos (`#F43F5E`), Photos (`#8B5CF6`), Apps (`#06B6D4`), Social (`#10B981`), Audio/System (`#F59E0B`), and Free Space (`#1F2937`).
- Gaps between categories: 2px solid `#090D16`. Terminal ends follow card radius or 9999px.

### Circular Telemetry Gauges
- Circular percentage rings for total storage used, CPU load, and RAM allocation.
- Stroke width: 8px to 12px. Track background: `#1F2937`.
- Dynamic meter uses radial or conical gradient ending in `#10B981` (under 80% usage) transitioning to `#F59E0B` (80–90%) and `#F43F5E` (above 90%).
- Center area displays large metric values using `telemetry-xl`.

### Single-Line Pill Badges
- Strict single-line flex items (`white-space: nowrap; display: inline-flex; align-items: center;`).
- Height: 24px. Padding: 0 8px. Radius: 9999px.
- Background: 10% opacity tint of category accent; border: 1px solid 25% opacity tint of the same color; text: 100% full-intensity category accent in `telemetry-sm`.

### Bottom Navigation Bar (5-Tab Architecture)
- Fixed at viewport base. Background: `#090D16` at 85% opacity with `backdrop-filter: blur(20px)`.
- Height: 64px + device bottom safe inset. Top border: 1px solid `#1F2937`.
- Tab items: Dashboard, Treemap, AI Clean (center elevated trigger with Electric Indigo glow), Gallery, Tools.
- Inactive icons: `#6B7280`. Active tab: `#10B981` with a micro indicator dot (4px).

### Duplicate Detection & File Cards
- Surface: `#111827`, border: 1px `#1F2937`, radius: 16px.
- Features split thumbnail comparison, similarity percentage badge (e.g., `99.4% SIMILAR`), file size badge in `JetBrains Mono`, and selection radio button.

### Form Inputs & Checkboxes
- **Inputs:** Height: 44px, surface `#111827`, border 1px `#1F2937`, text `#F9FAFB`. Focus ring: 2px `#6366F1`.
- **Selection Controls:** Custom circular checkboxes. Unchecked: 1.5px border `#374151`. Checked: fill `#10B981` with sharp white glyph.