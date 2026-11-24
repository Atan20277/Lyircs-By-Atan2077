# ATAN Lyrics

**Beautiful anime-styled synced lyrics for Minecraft 1.21.11**

Completely original client-side Fabric mod. Soft pastel HUD, karaoke highlight, glow, floating text, party particles and a pretty config menu.

## Features

- **Real media detection**
  - **Windows** — SMTC (WinRT) + window title fallback (Spotify, Chrome, Edge…)
  - **Linux** — `playerctl` (MPRIS)
  - **macOS** — AppleScript (Music.app + Spotify)
- Timed lyrics from **lrclib.net**
- Display modes: HUD Classic / Karaoke / Soft / Floating Anime / 3D World / Minimal / Party ♡
- Soft glow, karaoke fill, previous & next line, instrumental countdown
- Now-playing widget with progress bar
- **Custom images** — drop PNG/JPG into `config/atanlyrics/custom/`
- Clean anime config menu with animated accents
- No leftover code or credits from any other lyrics mod

## Keybind

- **Right Shift** → Open menu

## Custom Images

1. Open `.minecraft/config/atanlyrics/custom/`
2. Put your images there (e.g. `bg.png`, `art.png`)
3. In config you can reference them by filename

## Building

```bash
./gradlew build
```

Output jar → `build/libs/atan-lyrics-1.1.0+1.21.11.jar`

## Requirements

- Minecraft **1.21.11**
- Fabric Loader ≥ 0.16
- Fabric API
- Java 21

### Optional for best media detection
- **Linux**: install `playerctl`
- **Windows**: works out of the box (PowerShell)
- **macOS**: works out of the box

## License

MIT

---

ATAN Lyrics — soft, pretty, synced ♡
