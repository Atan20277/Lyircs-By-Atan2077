package me.atan.lyrics.media;

import me.atan.lyrics.AtanLyricsMod;
import me.atan.lyrics.util.LrclibClient;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Detects currently playing media across platforms and fetches timed lyrics.
 * Architecture mirrors the proven cross-platform approach:
 * - Windows: WinRT / SystemMediaTransportControls
 * - Linux: MPRIS via D-Bus
 * - macOS: AppleScript / MediaRemote
 * - Fallback: window title scraping
 *
 * Lyrics source: lrclib.net (same reliable public API)
 */
public final class MediaManager {

    private static final MediaManager INSTANCE = new MediaManager();

    private final AtomicReference<TrackInfo> currentTrack = new AtomicReference<>();
    private final AtomicReference<SyncedLyrics> currentLyrics = new AtomicReference<>();
    private MediaSession session;
    private boolean running = false;

    public static MediaManager getInstance() {
        return INSTANCE;
    }

    public void start() {
        if (running) return;
        running = true;

        // Detect platform and create appropriate session
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            session = new WindowsMediaSession();
        } else if (os.contains("mac") || os.contains("darwin")) {
            session = new MacMediaSession();
        } else {
            session = new LinuxMprisSession();
        }

        session.start();
        AtanLyricsMod.LOGGER.info("[ATAN Lyrics] Media session started ({})", session.getClass().getSimpleName());
    }

    public void tick() {
        if (session == null) return;

        Optional<TrackInfo> track = session.poll();
        if (track.isEmpty()) {
            currentTrack.set(null);
            currentLyrics.set(null);
            return;
        }

        TrackInfo newTrack = track.get();
        TrackInfo old = currentTrack.get();

        // New song detected → fetch lyrics
        if (old == null || !old.sameSong(newTrack)) {
            currentTrack.set(newTrack);
            currentLyrics.set(null);
            fetchLyricsAsync(newTrack);
        } else {
            // Same song, just update progress
            currentTrack.set(newTrack);
        }
    }

    private void fetchLyricsAsync(TrackInfo track) {
        CompletableFuture.supplyAsync(() -> LrclibClient.fetch(track.artist(), track.title(), track.durationMs()))
                .thenAccept(lyrics -> {
                    if (lyrics != null && currentTrack.get() != null && currentTrack.get().sameSong(track)) {
                        currentLyrics.set(lyrics);
                        AtanLyricsMod.LOGGER.info("[ATAN Lyrics] Lyrics loaded: {} - {}", track.artist(), track.title());
                    }
                })
                .exceptionally(ex -> {
                    AtanLyricsMod.LOGGER.debug("Lyrics fetch failed: {}", ex.getMessage());
                    return null;
                });
    }

    public Optional<TrackInfo> getCurrentTrack() {
        return Optional.ofNullable(currentTrack.get());
    }

    public Optional<SyncedLyrics> getCurrentLyrics() {
        return Optional.ofNullable(currentLyrics.get());
    }

    public Optional<String> getCurrentLine(double positionSeconds) {
        SyncedLyrics lyrics = currentLyrics.get();
        if (lyrics == null) return Optional.empty();
        return lyrics.getLineAt(positionSeconds);
    }
}
