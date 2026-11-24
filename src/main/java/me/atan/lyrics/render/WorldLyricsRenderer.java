package me.atan.lyrics.render;

import me.atan.lyrics.config.AtanConfig;
import me.atan.lyrics.media.MediaManager;
import me.atan.lyrics.media.SyncedLyrics;
import me.atan.lyrics.media.TrackInfo;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/**
 * 3D / floating world lyrics (anime floating text style).
 */
public final class WorldLyricsRenderer {

    private WorldLyricsRenderer() {}

    public static void render(WorldRenderContext context) {
        AtanConfig cfg = AtanConfig.INSTANCE;
        if (!cfg.enabled || !cfg.worldLyrics) return;

        Optional<TrackInfo> trackOpt = MediaManager.getInstance().getCurrentTrack();
        if (trackOpt.isEmpty() || !trackOpt.get().playing()) return;

        Optional<SyncedLyrics> lyricsOpt = MediaManager.getInstance().getCurrentLyrics();
        if (lyricsOpt.isEmpty()) return;

        double pos = trackOpt.get().positionSeconds() + cfg.syncOffset;
        Optional<String> line = lyricsOpt.get().getLineAt(pos);
        if (line.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        // Position in front of the player
        Vec3 look = mc.player.getViewVector(1.0f);
        Vec3 base = mc.player.getEyePosition(1.0f)
                .add(look.scale(cfg.worldDistance))
                .add(0, cfg.worldHeight - 1.6, 0);

        // Rendering 3D text requires careful matrix handling with the world render context.
        // Full implementation would use Font.drawInBatch with pose stack transformed to world space.
        // Placeholder kept clean for extension.
    }
}
