package me.atan.lyrics.media;

import java.util.Objects;

public record TrackInfo(
        String title,
        String artist,
        String album,
        long durationMs,
        long positionMs,
        boolean playing,
        String artUrl
) {
    public boolean sameSong(TrackInfo other) {
        if (other == null) return false;
        return Objects.equals(title, other.title) && Objects.equals(artist, other.artist);
    }

    public double positionSeconds() {
        return positionMs / 1000.0;
    }
}
