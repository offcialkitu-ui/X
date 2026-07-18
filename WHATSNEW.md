## What's New

### 🎵 Smarter Mixes, Smoother Playback
- **AutoMix (Beta)** — DJ-style, beat-matched crossfades are here. Songs blend seamlessly using BPM and beat grid analysis, with automatic fallback to regular crossfades when needed. Thanks @barathsuresh!
- **Crossfade reliability** — Heavy beat analysis now runs in the background, so your music never stutters or freezes — even on older devices.

### 🎨 Liquid Glass Gets Better (Beta)
- The Liquid Glass background now scrolls properly — no more cut-off strips.
- "Unknown" background style is now correctly labeled as "Liquid Glass."
- Adaptive text colors for the miniplayer and bottom nav bar in both Light and Dark themes.

### 🔍 Echo Extractor
- A new extractor is now seamlessly built into Echo Music.
- It automatically checks for updates once every 24 hours — no manual tapping needed.
- Settings page cleaned up: removed redundant toggle cards and text.

### 💜 Lossless Audio, Coming Soon
- Added a **Lossless Funding Tracker** in Player Settings so you can see server costs and progress toward Qobuz integration.

### 🛠️ Fixes & Polish
- Fixed a crash on app open caused by a playlist database migration. Thanks @jester-sys!
- **Spanish translation** is now 100% complete — including playlist export, listening history, Spotify import, and Echo Brain. Thanks @weblate!

### 🔧 Under the Hood
- Background fetching is now protected against system clock drift — no more getting stuck.
- Player JS cache writes are fully atomic, and token minting is more reliable.
- Improved memory handling during PoToken generation to prevent data leaks.