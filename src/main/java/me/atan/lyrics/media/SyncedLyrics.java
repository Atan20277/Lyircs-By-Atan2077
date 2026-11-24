package me.atan.lyrics.media;

import java.util.List;
import java.util.Optional;

public record SyncedLyrics(List<Line> lines) {

    public record Line(double time, String text) {}

    public Optional<String> getLineAt(double positionSeconds) {
        if (lines == null || lines.isEmpty()) return Optional.empty();

        Line current = null;
        for (Line line : lines) {
            if (line.time() <= positionSeconds) {
                current = line;
            } else {
                break;
            }
        }
        return current != null ? Optional.of(current.text()) : Optional.empty();
    }

    public Optional<Line> getCurrentLine(double positionSeconds) {
        if (lines == null || lines.isEmpty()) return Optional.empty();
        Line current = null;
        for (Line line : lines) {
            if (line.time() <= positionSeconds) {
                current = line;
            } else break;
        }
        return Optional.ofNullable(current);
    }

    public Optional<Line> getNextLine(double positionSeconds) {
        if (lines == null) return Optional.empty();
        for (Line line : lines) {
            if (line.time() > positionSeconds) {
                return Optional.of(line);
            }
        }
        return Optional.empty();
    }

    public Optional<Line> getPreviousLine(double positionSeconds) {
        if (lines == null || lines.isEmpty()) return Optional.empty();
        Line prev = null;
        for (Line line : lines) {
            if (line.time() <= positionSeconds) {
                prev = line;
            } else break;
        }
        // We already have current; look one before
        if (prev == null) return Optional.empty();
        int idx = lines.indexOf(prev);
        if (idx > 0) return Optional.of(lines.get(idx - 1));
        return Optional.empty();
    }
}
