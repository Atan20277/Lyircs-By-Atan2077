package me.atan.lyrics.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * ATAN Lyrics configuration — anime focused.
 */
public class AtanConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir().resolve("atanlyrics.json");
    public static final Path CUSTOM_DIR = FabricLoader.getInstance()
            .getConfigDir().resolve("atanlyrics/custom");

    public static AtanConfig INSTANCE = new AtanConfig();

    // General
    public boolean enabled = true;
    public String language = "auto";
    public double syncOffset = 0.0;

    // Display
    public DisplayMode displayMode = DisplayMode.HUD_KARAOKE;
    public boolean showWidget = true;
    public boolean showPreviousLine = true;
    public boolean showNextPreview = true;
    public boolean instrumentalCountdown = true;

    // Style
    public String fontId = "default";
    public boolean bold = false;
    public boolean uppercase = false;
    public float scale = 1.15f;
    public float opacity = 0.95f;
    public int primaryColor = 0xFFE8A0FF;      // soft lilac
    public int secondaryColor = 0xFFFF8AC5;    // pink
    public int accentColor = 0xFF7DF9FF;       // cyan
    public int glowColor = 0xFFC77DFF;         // purple glow
    public boolean glow = true;
    public float glowStrength = 0.85f;
    public boolean outline = false;
    public boolean dropShadow = true;
    public boolean background = true;
    public float backgroundOpacity = 0.42f;

    // Karaoke / Animation
    public boolean karaokeHighlight = true;
    public AnimationStyle animation = AnimationStyle.SOFT_FADE;
    public boolean partyMode = false;
    public boolean typewriter = false;
    public float floatAmplitude = 4.0f;        // floating bounce px
    public float floatSpeed = 1.6f;

    // 3D World
    public boolean worldLyrics = false;
    public float worldDistance = 3.5f;
    public float worldHeight = 1.8f;
    public boolean facePlayer = true;

    // Widget
    public float widgetX = 0.015f;
    public float widgetY = 0.015f;
    public float widgetScale = 1.05f;
    public boolean widgetShowArt = true;
    public boolean widgetShowProgress = true;

    // Custom images (relative to config/atanlyrics/custom/)
    public String customBackground = "";       // e.g. "bg.png"
    public String customWidgetArt = "";        // override album art
    public String customIcon = "";

    public enum DisplayMode {
        HUD_CLASSIC,
        HUD_KARAOKE,
        HUD_SOFT,
        FLOATING_ANIME,
        WORLD_3D,
        MINIMAL,
        PARTY
    }

    public enum AnimationStyle {
        NONE,
        SOFT_FADE,
        SLIDE_UP,
        TYPEWRITER,
        BOUNCE,
        GLOW_PULSE
    }

    public static void load() {
        try {
            Files.createDirectories(CUSTOM_DIR);
        } catch (IOException ignored) {}

        if (Files.exists(CONFIG_PATH)) {
            try {
                String json = Files.readString(CONFIG_PATH);
                INSTANCE = GSON.fromJson(json, AtanConfig.class);
                if (INSTANCE == null) INSTANCE = new AtanConfig();
            } catch (Exception e) {
                INSTANCE = new AtanConfig();
            }
        } else {
            save();
        }
    }

    public static void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.writeString(CONFIG_PATH, GSON.toJson(INSTANCE));
        } catch (IOException e) {
            // ignore
        }
    }

    public Path resolveCustom(String name) {
        if (name == null || name.isBlank()) return null;
        Path p = CUSTOM_DIR.resolve(name).normalize();
        if (!p.startsWith(CUSTOM_DIR)) return null; // path traversal guard
        return Files.exists(p) ? p : null;
    }
}
