package me.atan.lyrics.util;

import com.mojang.blaze3d.platform.NativeImage;
import me.atan.lyrics.AtanLyricsMod;
import me.atan.lyrics.config.AtanConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Loads custom user images from config/atanlyrics/custom/
 * and registers them as DynamicTextures.
 */
public final class CustomTextures {

    private static final Map<String, Identifier> CACHE = new HashMap<>();
    private static final String NAMESPACE = "atanlyrics_custom";

    private CustomTextures() {}

    public static Identifier get(String filename) {
        if (filename == null || filename.isBlank()) return null;
        return CACHE.computeIfAbsent(filename, CustomTextures::load);
    }

    private static Identifier load(String filename) {
        Path path = AtanConfig.INSTANCE.resolveCustom(filename);
        if (path == null) {
            AtanLyricsMod.LOGGER.warn("[ATAN] Custom image not found: {}", filename);
            return null;
        }

        try (InputStream in = Files.newInputStream(path)) {
            NativeImage image = NativeImage.read(in);
            DynamicTexture tex = new DynamicTexture(() -> "atanlyrics/" + filename, image);
            Identifier id = Identifier.fromNamespaceAndPath(NAMESPACE, sanitize(filename));
            Minecraft.getInstance().getTextureManager().register(id, tex);
            AtanLyricsMod.LOGGER.info("[ATAN] Loaded custom image: {}", filename);
            return id;
        } catch (Exception e) {
            AtanLyricsMod.LOGGER.error("[ATAN] Failed to load custom image: " + filename, e);
            return null;
        }
    }

    private static String sanitize(String name) {
        return name.toLowerCase().replaceAll("[^a-z0-9/._-]", "_");
    }

    public static void reload() {
        CACHE.clear();
    }
}
