package me.atan.lyrics.media;

import me.atan.lyrics.AtanLyricsMod;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Linux media session using playerctl (MPRIS).
 * Extremely reliable when playerctl is installed.
 * Falls back to empty if playerctl is missing.
 */
public class LinuxMprisSession implements MediaSession {

    private boolean active = false;
    private boolean playerctlAvailable = false;

    @Override
    public void start() {
        active = true;
        playerctlAvailable = checkPlayerctl();
        if (playerctlAvailable) {
            AtanLyricsMod.LOGGER.info("[ATAN] Linux media session ready (playerctl / MPRIS)");
        } else {
            AtanLyricsMod.LOGGER.warn("[ATAN] playerctl not found — install it for media detection (apt/pacman/dnf install playerctl)");
        }
    }

    @Override
    public void stop() {
        active = false;
    }

    @Override
    public Optional<TrackInfo> poll() {
        if (!active || !playerctlAvailable) return Optional.empty();

        try {
            // status
            String status = run("playerctl", "status").orElse("").trim();
            boolean playing = status.equalsIgnoreCase("Playing");

            String title = run("playerctl", "metadata", "title").orElse("").trim();
            String artist = run("playerctl", "metadata", "artist").orElse("").trim();
            String album = run("playerctl", "metadata", "album").orElse("").trim();

            if (title.isEmpty() && artist.isEmpty()) return Optional.empty();

            long positionMs = 0;
            long durationMs = 0;

            String posStr = run("playerctl", "position").orElse("").trim();
            if (!posStr.isEmpty()) {
                try {
                    positionMs = (long) (Double.parseDouble(posStr) * 1000);
                } catch (NumberFormatException ignored) {}
            }

            // length is in microseconds under mpris:length
            String lenStr = run("playerctl", "metadata", "mpris:length").orElse("").trim();
            if (!lenStr.isEmpty()) {
                try {
                    durationMs = Long.parseLong(lenStr) / 1000; // µs → ms
                } catch (NumberFormatException ignored) {}
            }

            String art = run("playerctl", "metadata", "mpris:artUrl").orElse(null);
            if (art != null && art.isBlank()) art = null;

            return Optional.of(new TrackInfo(
                    title.isEmpty() ? "Unknown" : title,
                    artist,
                    album.isEmpty() ? null : album,
                    durationMs,
                    positionMs,
                    playing,
                    art
            ));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private boolean checkPlayerctl() {
        try {
            Process p = new ProcessBuilder("playerctl", "--version").start();
            return p.waitFor(800, TimeUnit.MILLISECONDS) && p.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private Optional<String> run(String... cmd) {
        try {
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            if (!p.waitFor(1200, TimeUnit.MILLISECONDS)) {
                p.destroyForcibly();
                return Optional.empty();
            }
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = r.readLine()) != null) {
                    if (sb.length() > 0) sb.append('\n');
                    sb.append(line);
                }
                return Optional.of(sb.toString());
            }
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
