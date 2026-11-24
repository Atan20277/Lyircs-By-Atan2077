package me.atan.lyrics;

import me.atan.lyrics.config.AtanConfig;
import me.atan.lyrics.gui.AtanMenuScreen;
import me.atan.lyrics.gui.MenuKey;
import me.atan.lyrics.media.MediaManager;
import me.atan.lyrics.render.HudLyricsRenderer;
import me.atan.lyrics.render.WorldLyricsRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ATAN Lyrics — original anime-styled synced lyrics mod for Minecraft 1.21.11
 */
public final class AtanLyricsMod implements ClientModInitializer {

    public static final String MOD_ID = "atanlyrics";
    public static final Logger LOGGER = LoggerFactory.getLogger("ATAN Lyrics");

    private static final int POLL_INTERVAL = 8;
    private int tickCounter = 0;

    @Override
    public void onInitializeClient() {
        AtanConfig.load();
        MediaManager.getInstance().start();

        KeyBindingHelper.registerKeyBinding(MenuKey.OPEN_MENU);

        // 1.21.11+ preferred HUD API
        HudElementRegistry.attachElementAfter(
                VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath(MOD_ID, "lyrics"),
                (graphics, deltaTracker) -> HudLyricsRenderer.render(graphics, deltaTracker)
        );

        WorldRenderEvents.BeforeTranslucent.register(WorldLyricsRenderer::render);

        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);

        LOGGER.info("[ATAN Lyrics] Ready ♡  Minecraft 1.21.11 — soft anime lyrics");
    }

    private void onClientTick(Minecraft client) {
        if (MenuKey.OPEN_MENU.consumeClick() && client.screen == null) {
            client.setScreen(new AtanMenuScreen(null));
        }

        if (++tickCounter >= POLL_INTERVAL) {
            tickCounter = 0;
            MediaManager.getInstance().tick();
        }
    }
}
