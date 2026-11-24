package me.atan.lyrics.media;

import java.util.Optional;

/**
 * Abstraction for platform media session.
 * Implementations:
 *  - WindowsMediaSession  (WinRT / SMTC)
 *  - LinuxMprisSession    (D-Bus MPRIS)
 *  - MacMediaSession      (AppleScript / MediaRemote)
 */
public interface MediaSession {
    void start();
    void stop();
    Optional<TrackInfo> poll();
}
