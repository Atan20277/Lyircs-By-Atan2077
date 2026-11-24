package me.atan.lyrics.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.atan.lyrics.media.SyncedLyrics;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Client for lrclib.net — the same public lyrics source used by many modern lyrics tools.
 * Clean, original implementation.
 */
public final class LrclibClient {

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .build();

    private static final Pattern LRC_LINE = Pattern.compile("\\[(\\d{2}):(\\d{2})\\.(\\d{2,3})](.*)");

    private LrclibClient() {}

    public static SyncedLyrics fetch(String artist, String title, long durationMs) {
        try {
            String qArtist = URLEncoder.encode(artist == null ? "" : artist, StandardCharsets.UTF_8);
            String qTitle = URLEncoder.encode(title == null ? "" : title, StandardCharsets.UTF_8);
            long durationSec = durationMs > 0 ? durationMs / 1000 : 0;

            String url = String.format(
                    "https://lrclib.net/api/get?artist_name=%s&track_name=%s&duration=%d",
                    qArtist, qTitle, durationSec
            );

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .header("User-Agent", "ATAN-Lyrics/1.0")
                    .GET()
                    .build();

            HttpResponse<String> resp = CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) {
                // fallback search
                return searchAndFetch(artist, title);
            }

            JsonObject json = JsonParser.parseString(resp.body()).getAsJsonObject();
            if (json.has("syncedLyrics") && !json.get("syncedLyrics").isJsonNull()) {
                return parseLrc(json.get("syncedLyrics").getAsString());
            }
            if (json.has("plainLyrics") && !json.get("plainLyrics").isJsonNull()) {
                // plain only — no timing
                return null;
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static SyncedLyrics searchAndFetch(String artist, String title) {
        try {
            String q = URLEncoder.encode((artist + " " + title).trim(), StandardCharsets.UTF_8);
            String url = "https://lrclib.net/api/search?q=" + q;

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .header("User-Agent", "ATAN-Lyrics/1.0")
                    .GET()
                    .build();

            HttpResponse<String> resp = CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) return null;

            JsonArray arr = JsonParser.parseString(resp.body()).getAsJsonArray();
            if (arr.isEmpty()) return null;

            JsonObject best = arr.get(0).getAsJsonObject();
            if (best.has("syncedLyrics") && !best.get("syncedLyrics").isJsonNull()) {
                return parseLrc(best.get("syncedLyrics").getAsString());
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static SyncedLyrics parseLrc(String lrc) {
        List<SyncedLyrics.Line> lines = new ArrayList<>();
        for (String raw : lrc.split("\\R")) {
            Matcher m = LRC_LINE.matcher(raw.trim());
            if (m.find()) {
                int min = Integer.parseInt(m.group(1));
                int sec = Integer.parseInt(m.group(2));
                String frac = m.group(3);
                double ms = frac.length() == 2 ? Integer.parseInt(frac) * 10 : Integer.parseInt(frac);
                double time = min * 60.0 + sec + ms / 1000.0;
                String text = m.group(4).trim();
                if (!text.isEmpty()) {
                    lines.add(new SyncedLyrics.Line(time, text));
                }
            }
        }
        return lines.isEmpty() ? null : new SyncedLyrics(lines);
    }
}
