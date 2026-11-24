package me.atan.lyrics.media;

import me.atan.lyrics.AtanLyricsMod;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Windows media session.
 * Primary: PowerShell + Windows Runtime SMTC (when available)
 * Fallback: Window title parsing for Spotify, Chrome, Edge, Discord, etc.
 */
public class WindowsMediaSession implements MediaSession {

    private boolean active = false;
    private static final Pattern SPOTIFY_TITLE = Pattern.compile("^(.+?) - (.+)$");
    private static final Pattern YT_TITLE = Pattern.compile("^(.+?) - YouTube");

    @Override
    public void start() {
        active = true;
        AtanLyricsMod.LOGGER.info("[ATAN] Windows media session ready (SMTC + title fallback)");
    }

    @Override
    public void stop() {
        active = false;
    }

    @Override
    public Optional<TrackInfo> poll() {
        if (!active) return Optional.empty();

        // 1) Try modern SMTC via PowerShell
        Optional<TrackInfo> smtc = trySmtc();
        if (smtc.isPresent()) return smtc;

        // 2) Fallback: parse common player window titles
        return tryWindowTitle();
    }

    private Optional<TrackInfo> trySmtc() {
        try {
            // Compact PowerShell that queries GlobalSystemMediaTransportControlsSessionManager
            String ps = """
                $ErrorActionPreference='SilentlyContinue'
                Add-Type -AssemblyName System.Runtime.WindowsRuntime
                $asTask = [System.WindowsRuntimeSystemExtensions].GetMethods() | Where-Object { $_.Name -eq 'AsTask' -and $_.GetParameters().Count -eq 1 -and $_.GetParameters()[0].ParameterType.Name -eq 'IAsyncOperation`1' }
                $null = [Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager,Windows.Media.Control,ContentType=WindowsRuntime]
                $mgrOp = [Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager]::RequestAsync()
                $mgr = $asTask.MakeGenericMethod([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager]).Invoke($null, @($mgrOp)).Result
                $session = $mgr.GetCurrentSession()
                if (-not $session) { exit 1 }
                $prop = $session.TryGetMediaPropertiesAsync().GetAwaiter().GetResult()
                $timeline = $session.GetTimelineProperties()
                $status = $session.GetPlaybackInfo().PlaybackStatus
                $playing = if ($status -eq 4) { '1' } else { '0' }
                $pos = [math]::Round($timeline.Position.TotalMilliseconds)
                $dur = [math]::Round($timeline.EndTime.TotalMilliseconds)
                Write-Output ($prop.Title + "`t" + $prop.Artist + "`t" + $prop.AlbumTitle + "`t" + $pos + "`t" + $dur + "`t" + $playing)
                """;

            ProcessBuilder pb = new ProcessBuilder(
                    "powershell.exe", "-NoProfile", "-NonInteractive", "-Command", ps
            );
            pb.redirectErrorStream(true);
            Process p = pb.start();
            boolean finished = p.waitFor(2500, TimeUnit.MILLISECONDS);
            if (!finished) {
                p.destroyForcibly();
                return Optional.empty();
            }

            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                String line = r.readLine();
                if (line == null || line.isBlank()) return Optional.empty();
                String[] parts = line.split("\t", -1);
                if (parts.length < 6) return Optional.empty();

                String title = emptyToNull(parts[0]);
                String artist = emptyToNull(parts[1]);
                String album = emptyToNull(parts[2]);
                long pos = parseLong(parts[3], 0);
                long dur = parseLong(parts[4], 0);
                boolean playing = "1".equals(parts[5]);

                if (title == null && artist == null) return Optional.empty();
                return Optional.of(new TrackInfo(
                        title != null ? title : "Unknown",
                        artist != null ? artist : "",
                        album,
                        dur, pos, playing, null
                ));
            }
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private Optional<TrackInfo> tryWindowTitle() {
        try {
            // Get foreground or known player window titles via PowerShell
            String ps = """
                $procs = @('Spotify','chrome','msedge','firefox','Discord','vlc','MusicBee','iTunes')
                foreach ($name in $procs) {
                  Get-Process -Name $name -ErrorAction SilentlyContinue | ForEach-Object {
                    if ($_.MainWindowTitle -and $_.MainWindowTitle.Length -gt 3) {
                      Write-Output ($name + "`t" + $_.MainWindowTitle)
                    }
                  }
                }
                """;

            ProcessBuilder pb = new ProcessBuilder(
                    "powershell.exe", "-NoProfile", "-NonInteractive", "-Command", ps
            );
            pb.redirectErrorStream(true);
            Process p = pb.start();
            if (!p.waitFor(1500, TimeUnit.MILLISECONDS)) {
                p.destroyForcibly();
                return Optional.empty();
            }

            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = r.readLine()) != null) {
                    String[] parts = line.split("\t", 2);
                    if (parts.length < 2) continue;
                    String player = parts[0];
                    String title = parts[1].trim();

                    if (title.isEmpty() || title.equalsIgnoreCase(player)) continue;

                    // Spotify: "Artist - Title"
                    if (player.equalsIgnoreCase("Spotify")) {
                        Matcher m = SPOTIFY_TITLE.matcher(title);
                        if (m.find()) {
                            return Optional.of(new TrackInfo(m.group(2).trim(), m.group(1).trim(), null, 0, 0, true, null));
                        }
                        return Optional.of(new TrackInfo(title, "Spotify", null, 0, 0, true, null));
                    }

                    // YouTube in browser
                    Matcher yt = YT_TITLE.matcher(title);
                    if (yt.find()) {
                        String t = yt.group(1).trim();
                        // often "Song Name - Artist - YouTube"
                        String[] bits = t.split(" - ", 2);
                        if (bits.length == 2) {
                            return Optional.of(new TrackInfo(bits[0].trim(), bits[1].trim(), null, 0, 0, true, null));
                        }
                        return Optional.of(new TrackInfo(t, "YouTube", null, 0, 0, true, null));
                    }

                    // Generic
                    return Optional.of(new TrackInfo(title, player, null, 0, 0, true, null));
                }
            }
        } catch (Exception ignored) {}
        return Optional.empty();
    }

    private static String emptyToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    private static long parseLong(String s, long def) {
        try { return Long.parseLong(s.trim()); } catch (Exception e) { return def; }
    }
}
