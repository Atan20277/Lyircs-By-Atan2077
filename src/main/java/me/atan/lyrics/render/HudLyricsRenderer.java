package me.atan.lyrics.render;

import me.atan.lyrics.config.AtanConfig;
import me.atan.lyrics.media.MediaManager;
import me.atan.lyrics.media.SyncedLyrics;
import me.atan.lyrics.media.TrackInfo;
import me.atan.lyrics.util.CustomTextures;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * Anime-styled HUD lyrics with Glow, Floating, Karaoke, Party effects
 * and custom image support (1.21.11).
 */
public final class HudLyricsRenderer {

    private static final List<Particle> PARTICLES = new ArrayList<>();
    private static final Random RNG = new Random();
    private static float time;

    private HudLyricsRenderer() {}

    public static void render(GuiGraphics g, DeltaTracker deltaTracker) {
        AtanConfig cfg = AtanConfig.INSTANCE;
        if (!cfg.enabled) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui) return;

        float partial = deltaTracker.getGameTimeDeltaPartialTick(false);
        time += partial * 0.05f;

        Optional<TrackInfo> trackOpt = MediaManager.getInstance().getCurrentTrack();
        if (trackOpt.isEmpty()) return;

        TrackInfo track = trackOpt.get();
        if (!track.playing() && cfg.displayMode != AtanConfig.DisplayMode.MINIMAL) return;

        double pos = track.positionSeconds() + cfg.syncOffset;
        Optional<SyncedLyrics> lyricsOpt = MediaManager.getInstance().getCurrentLyrics();

        Font font = mc.font;
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();

        // Optional full-screen custom background
        if (cfg.customBackground != null && !cfg.customBackground.isBlank()) {
            ResourceLocation bg = CustomTextures.get(cfg.customBackground);
            if (bg != null) {
                g.blit(bg, 0, 0, 0, 0, sw, sh, sw, sh);
            }
        }

        // Widget
        if (cfg.showWidget) {
            renderWidget(g, font, track, cfg, sw, sh);
        }

        if (lyricsOpt.isEmpty()) return;
        SyncedLyrics lyrics = lyricsOpt.get();

        Optional<SyncedLyrics.Line> current = lyrics.getCurrentLine(pos);
        Optional<SyncedLyrics.Line> next = lyrics.getNextLine(pos);
        Optional<SyncedLyrics.Line> prev = lyrics.getPreviousLine(pos);

        if (current.isEmpty()) return;

        String text = current.get().text();
        if (cfg.uppercase) text = text.toUpperCase();

        float scale = cfg.scale;
        boolean floating = cfg.displayMode == AtanConfig.DisplayMode.FLOATING_ANIME
                || cfg.displayMode == AtanConfig.DisplayMode.PARTY
                || cfg.animation == AtanConfig.AnimationStyle.BOUNCE;

        float floatY = floating ? (float) Math.sin(time * cfg.floatSpeed) * cfg.floatAmplitude : 0f;
        int baseY = (int) (sh * 0.76f + floatY);

        // Soft background panel
        if (cfg.background && cfg.displayMode != AtanConfig.DisplayMode.MINIMAL) {
            int textW = (int) (font.width(text) * scale);
            int padX = 18, padY = 10;
            int bgX = (sw - textW) / 2 - padX;
            int bgY = baseY - padY;
            int bgW = textW + padX * 2;
            int bgH = (int) (font.lineHeight * scale) + padY * 2;
            int a = (int) (cfg.backgroundOpacity * 200);
            g.fill(bgX + 2, bgY, bgX + bgW - 2, bgY + bgH, (a << 24) | 0x0C0A14);
            g.fill(bgX, bgY + 2, bgX + bgW, bgY + bgH - 2, (a << 24) | 0x0C0A14);
            g.fill(bgX + 8, bgY, bgX + bgW - 8, bgY + 1, withAlpha(cfg.accentColor, 160));
        }

        // Glow
        if (cfg.glow) {
            int glowCol = withAlpha(cfg.glowColor, (int) (90 * cfg.glowStrength));
            for (int dx = -2; dx <= 2; dx++) {
                for (int dy = -2; dy <= 2; dy++) {
                    if (dx == 0 && dy == 0) continue;
                    if (Math.abs(dx) + Math.abs(dy) > 3) continue;
                    drawCentered(g, font, text, sw / 2f + dx, baseY + dy, scale, glowCol, false);
                }
            }
        }

        // Main line
        if (cfg.karaokeHighlight && cfg.displayMode != AtanConfig.DisplayMode.MINIMAL) {
            double lineStart = current.get().time();
            double lineEnd = next.map(SyncedLyrics.Line::time).orElse(lineStart + 4.0);
            float progress = (float) Mth.clamp((pos - lineStart) / Math.max(0.01, lineEnd - lineStart), 0, 1);
            drawCentered(g, font, text, sw / 2f, baseY, scale, withAlpha(cfg.primaryColor, 130), cfg.dropShadow);
            if (progress > 0.02f) {
                drawCentered(g, font, text, sw / 2f, baseY, scale, cfg.secondaryColor, false);
            }
        } else {
            drawCentered(g, font, text, sw / 2f, baseY, scale, cfg.primaryColor, cfg.dropShadow);
        }

        // Previous / Next
        if (cfg.showPreviousLine && prev.isPresent() && cfg.displayMode != AtanConfig.DisplayMode.MINIMAL) {
            String p = prev.get().text();
            if (cfg.uppercase) p = p.toUpperCase();
            drawCentered(g, font, p, sw / 2f, baseY - (int) (20 * scale), scale * 0.72f,
                    withAlpha(cfg.primaryColor, 80), false);
        }
        if (cfg.showNextPreview && next.isPresent() && cfg.displayMode != AtanConfig.DisplayMode.MINIMAL) {
            String n = next.get().text();
            if (cfg.uppercase) n = n.toUpperCase();
            drawCentered(g, font, n, sw / 2f, baseY + (int) (22 * scale), scale * 0.68f,
                    withAlpha(cfg.accentColor, 120), false);
        }

        // Party
        if (cfg.partyMode || cfg.displayMode == AtanConfig.DisplayMode.PARTY) {
            spawnParty(sw / 2f, baseY, cfg);
            renderParticles(g);
        }
    }

    private static void drawCentered(GuiGraphics g, Font font, String text, float cx, float y,
                                     float scale, int color, boolean shadow) {
        g.pose().pushMatrix();
        g.pose().translate(cx, y);
        g.pose().scale(scale, scale);
        int w = font.width(text);
        if (shadow) {
            g.drawString(font, text, -w / 2 + 1, 1, withAlpha(0x000000, 140), false);
        }
        g.drawString(font, text, -w / 2, 0, color, false);
        g.pose().popMatrix();
    }

    private static void renderWidget(GuiGraphics g, Font font, TrackInfo track, AtanConfig cfg,
                                     int sw, int sh) {
        int x = (int) (sw * cfg.widgetX);
        int y = (int) (sh * cfg.widgetY);
        float s = cfg.widgetScale;

        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.pose().scale(s, s);

        // Custom art override
        ResourceLocation art = null;
        if (cfg.customWidgetArt != null && !cfg.customWidgetArt.isBlank()) {
            art = CustomTextures.get(cfg.customWidgetArt);
        }

        int panelW = 172;
        int panelH = 42;
        int artSize = 36;

        if (art != null && cfg.widgetShowArt) {
            panelW = 210;
            g.fill(0, 0, panelW, panelH, 0xBB0A0818);
            g.blit(art, 3, 3, 0, 0, artSize, artSize, artSize, artSize);
            g.drawString(font, track.title() != null ? trim(track.title(), 20) : "Unknown", 46, 8, cfg.primaryColor, false);
            g.drawString(font, track.artist() != null ? trim(track.artist(), 22) : "", 46, 20, withAlpha(cfg.secondaryColor, 210), false);
        } else {
            g.fill(0, 0, panelW, panelH, 0xBB0A0818);
            g.drawString(font, "♪ " + (track.title() != null ? trim(track.title(), 22) : "Unknown"), 8, 8, cfg.primaryColor, false);
            g.drawString(font, track.artist() != null ? trim(track.artist(), 24) : "", 10, 20, withAlpha(cfg.secondaryColor, 210), false);
        }

        g.fill(0, 0, panelW, 2, cfg.accentColor);
        g.fill(0, panelH - 2, panelW, panelH, withAlpha(cfg.secondaryColor, 100));

        if (cfg.widgetShowProgress && track.durationMs() > 0) {
            float p = Mth.clamp((float) track.positionMs() / track.durationMs(), 0, 1);
            int barX = art != null ? 46 : 8;
            int barW = panelW - barX - 8;
            g.fill(barX, 34, barX + barW, 37, 0x33FFFFFF);
            g.fill(barX, 34, barX + (int) (barW * p), 37, cfg.accentColor);
        }

        g.pose().popMatrix();
    }

    private static String trim(String s, int max) {
        return s.length() > max ? s.substring(0, max - 1) + "…" : s;
    }

    private static void spawnParty(float cx, float cy, AtanConfig cfg) {
        if (PARTICLES.size() > 48) return;
        if (RNG.nextFloat() > 0.35f) return;
        float angle = RNG.nextFloat() * (float) Math.PI * 2;
        float speed = 0.4f + RNG.nextFloat() * 1.2f;
        PARTICLES.add(new Particle(
                cx + (RNG.nextFloat() - 0.5f) * 80,
                cy + (RNG.nextFloat() - 0.5f) * 20,
                (float) Math.cos(angle) * speed,
                (float) Math.sin(angle) * speed - 0.6f,
                0.6f + RNG.nextFloat() * 0.8f,
                RNG.nextBoolean() ? cfg.secondaryColor : cfg.accentColor,
                40 + RNG.nextInt(40)
        ));
    }

    private static void renderParticles(GuiGraphics g) {
        Iterator<Particle> it = PARTICLES.iterator();
        while (it.hasNext()) {
            Particle p = it.next();
            p.life--;
            if (p.life <= 0) { it.remove(); continue; }
            p.x += p.vx;
            p.y += p.vy;
            p.vy += 0.03f;
            float alpha = p.life / (float) p.maxLife;
            int col = withAlpha(p.color, (int) (alpha * 220));
            int size = Math.max(1, (int) (p.size * alpha * 3));
            g.fill((int) p.x, (int) p.y, (int) p.x + size, (int) p.y + size, col);
        }
    }

    private static int withAlpha(int rgb, int alpha) {
        return (Math.min(255, Math.max(0, alpha)) << 24) | (rgb & 0x00FFFFFF);
    }

    private static class Particle {
        float x, y, vx, vy, size;
        int color, life, maxLife;
        Particle(float x, float y, float vx, float vy, float size, int color, int life) {
            this.x = x; this.y = y; this.vx = vx; this.vy = vy;
            this.size = size; this.color = color; this.life = life; this.maxLife = life;
        }
    }
}
