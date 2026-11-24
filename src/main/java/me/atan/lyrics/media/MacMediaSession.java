package me.atan.lyrics.media;

import me.atan.lyrics.AtanLyricsMod;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * macOS media session using AppleScript (osascript).
 * Works with Music.app, Spotify, and many other players that support media keys.
 */
public class MacMediaSession implements MediaSession {

    private boolean active = false;

    @Override
    public void start() {
        active = true;
        AtanLyricsMod.LOGGER.info("[ATAN] macOS media session ready (AppleScript)");
    }

    @Override
    public void stop() {
        active = false;
    }

    @Override
    public Optional<TrackInfo> poll() {
        if (!active) return Optional.empty();

        // Try Music.app first, then Spotify
        Optional<TrackInfo> music = tryMusicApp();
        if (music.isPresent()) return music;

        return trySpotify();
    }

    private Optional<TrackInfo> tryMusicApp() {
        String script = """
            tell application "System Events"
              set musicRunning to (name of processes) contains "Music"
            end tell
            if not musicRunning then return ""
            tell application "Music"
              if player state is not playing and player state is not paused then return ""
              set t to name of current track
              set a to artist of current track
              set al to album of current track
              set pos to player position
              set dur to duration of current track
              set st to player state as string
              return t & tab & a & tab & al & tab & pos & tab & dur & tab & st
            end tell
            """;
        return parseAppleScript(script, true);
    }

    private Optional<TrackInfo> trySpotify() {
        String script = """
            tell application "System Events"
              set spotRunning to (name of processes) contains "Spotify"
            end tell
            if not spotRunning then return ""
            tell application "Spotify"
              if player state is not playing and player state is not paused then return ""
              set t to name of current track
              set a to artist of current track
              set al to album of current track
              set pos to player position
              set dur to duration of current track
              set st to player state as string
              return t & tab & a & tab & al & tab & pos & tab & dur & tab & st
            end tell
            """;
        return parseAppleScript(script, false);
    }

    private Optional<TrackInfo> parseAppleScript(String script, boolean durationInSeconds) {
        try {
            ProcessBuilder pb = new ProcessBuilder("osascript", "-e", script);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            if (!p.waitFor(2000, TimeUnit.MILLISECONDS)) {
                p.destroyForcibly();
                return Optional.empty();
            }

            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                String line = r.readLine();
                if (line == null || line.isBlank()) return Optional.empty();

                String[] parts = line.split("\t", -1);
                if (parts.length < 6) return Optional.empty();

                String title = parts[0].trim();
                String artist = parts[1].trim();
                String album = parts[2].trim();
                double posSec = parseDouble(parts[3], 0);
                double durSec = parseDouble(parts[4], 0);
                String state = parts[5].trim().toLowerCase();
                boolean playing = state.contains("playing");

                if (title.isEmpty()) return Optional.empty();

                // Music.app duration is already seconds; Spotify duration is ms
                long durationMs = durationInSeconds ? (long) (durSec * 1000) : (long) durSec;
                long positionMs = (long) (posSec * 1000);

                return Optional.of(new TrackInfo(
                        title, artist,
                        album.isEmpty() ? null : album,
                        durationMs, positionMs, playing, null
                ));
            }
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private static double parseDouble(String s, double def) {
        try { return Double.parseDouble(s.trim()); } catch (Exception e) { return def; }
    }
}
