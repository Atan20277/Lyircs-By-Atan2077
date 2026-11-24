package me.atan.lyrics.gui;

import me.atan.lyrics.config.AtanConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * Soft anime config screen with animated accents.
 */
public class AtanMenuScreen extends Screen {

    private final Screen parent;
    private AtanConfig cfg;
    private float anim;

    public AtanMenuScreen(Screen parent) {
        super(Component.literal("ATAN Lyrics"));
        this.parent = parent;
        this.cfg = AtanConfig.INSTANCE;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int startY = 52;
        int gap = 24;
        int w = 210;

        addBtn(cx, startY, w, cfg.enabled ? "§d●  Enabled" : "§7○  Disabled", b -> {
            cfg.enabled = !cfg.enabled;
            b.setMessage(Component.literal(cfg.enabled ? "§d●  Enabled" : "§7○  Disabled"));
            AtanConfig.save();
        });

        addBtn(cx, startY + gap, w, "Mode  ›  " + prettyMode(cfg.displayMode), b -> {
            var modes = AtanConfig.DisplayMode.values();
            cfg.displayMode = modes[(cfg.displayMode.ordinal() + 1) % modes.length];
            b.setMessage(Component.literal("Mode  ›  " + prettyMode(cfg.displayMode)));
            AtanConfig.save();
        });

        addBtn(cx, startY + gap * 2, w, "Karaoke  ›  " + onOff(cfg.karaokeHighlight), b -> {
            cfg.karaokeHighlight = !cfg.karaokeHighlight;
            b.setMessage(Component.literal("Karaoke  ›  " + onOff(cfg.karaokeHighlight)));
            AtanConfig.save();
        });

        addBtn(cx, startY + gap * 3, w, "Glow  ›  " + onOff(cfg.glow), b -> {
            cfg.glow = !cfg.glow;
            b.setMessage(Component.literal("Glow  ›  " + onOff(cfg.glow)));
            AtanConfig.save();
        });

        addBtn(cx, startY + gap * 4, w, "Party Mode  ›  " + (cfg.partyMode ? "§dON ♡" : "§7OFF"), b -> {
            cfg.partyMode = !cfg.partyMode;
            b.setMessage(Component.literal("Party Mode  ›  " + (cfg.partyMode ? "§dON ♡" : "§7OFF")));
            AtanConfig.save();
        });

        addBtn(cx, startY + gap * 5, w, "Floating  ›  " + onOff(cfg.displayMode == AtanConfig.DisplayMode.FLOATING_ANIME), b -> {
            if (cfg.displayMode == AtanConfig.DisplayMode.FLOATING_ANIME) {
                cfg.displayMode = AtanConfig.DisplayMode.HUD_KARAOKE;
            } else {
                cfg.displayMode = AtanConfig.DisplayMode.FLOATING_ANIME;
            }
            b.setMessage(Component.literal("Floating  ›  " + onOff(cfg.displayMode == AtanConfig.DisplayMode.FLOATING_ANIME)));
            AtanConfig.save();
        });

        addBtn(cx, startY + gap * 6, w, "3D World  ›  " + onOff(cfg.worldLyrics), b -> {
            cfg.worldLyrics = !cfg.worldLyrics;
            b.setMessage(Component.literal("3D World  ›  " + onOff(cfg.worldLyrics)));
            AtanConfig.save();
        });

        addBtn(cx, startY + gap * 7, w, "Widget  ›  " + onOff(cfg.showWidget), b -> {
            cfg.showWidget = !cfg.showWidget;
            b.setMessage(Component.literal("Widget  ›  " + onOff(cfg.showWidget)));
            AtanConfig.save();
        });

        // Done
        this.addRenderableWidget(Button.builder(Component.literal("§dDone  ♡"), b -> onClose())
                .bounds(cx - 55, startY + gap * 8 + 8, 110, 22).build());
    }

    private void addBtn(int cx, int y, int w, String label, Button.OnPress action) {
        this.addRenderableWidget(Button.builder(Component.literal(label), action)
                .bounds(cx - w / 2, y, w, 20).build());
    }

    private static String onOff(boolean v) {
        return v ? "§aON" : "§7OFF";
    }

    private static String prettyMode(AtanConfig.DisplayMode m) {
        return switch (m) {
            case HUD_CLASSIC -> "HUD Classic";
            case HUD_KARAOKE -> "HUD Karaoke";
            case HUD_SOFT -> "HUD Soft";
            case FLOATING_ANIME -> "Floating ♡";
            case WORLD_3D -> "3D World";
            case MINIMAL -> "Minimal";
            case PARTY -> "Party ♡";
        };
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        anim += delta * 0.04f;

        // Deep soft background
        g.fill(0, 0, this.width, this.height, 0xDD07060F);

        // Animated gradient bars
        int barAlpha = 180 + (int) (40 * Math.sin(anim));
        g.fill(0, 0, this.width, 3, withAlpha(0xC77DFF, barAlpha));
        g.fill(0, 3, this.width, 5, withAlpha(0xFF8AC5, barAlpha - 40));
        g.fill(0, this.height - 3, this.width, this.height, withAlpha(0x7DF9FF, 120));

        // Soft side glow
        for (int i = 0; i < 8; i++) {
            int a = 18 - i * 2;
            g.fill(i, 5, i + 1, this.height - 3, withAlpha(0xC77DFF, a));
            g.fill(this.width - 1 - i, 5, this.width - i, this.height - 3, withAlpha(0xFF8AC5, a));
        }

        // Title with subtle pulse
        float pulse = 0.85f + 0.15f * (float) Math.sin(anim * 1.7);
        int titleColor = withAlpha(0xE8A0FF, (int) (255 * pulse));
        g.drawCenteredString(this.font, "ATAN Lyrics", this.width / 2, 16, titleColor);
        g.drawCenteredString(this.font, "§7anime  ·  soft  ·  synced", this.width / 2, 28, 0xA0A0B8);

        // Custom image hint
        g.drawCenteredString(this.font, "§8Custom images → config/atanlyrics/custom/", this.width / 2, this.height - 18, 0x666688);

        super.render(g, mouseX, mouseY, delta);
    }

    private static int withAlpha(int rgb, int alpha) {
        return (Mth.clamp(alpha, 0, 255) << 24) | (rgb & 0x00FFFFFF);
    }

    @Override
    public void onClose() {
        AtanConfig.save();
        this.minecraft.setScreen(parent);
    }
}
