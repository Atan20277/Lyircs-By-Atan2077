package me.atan.lyrics;

import me.atan.lyrics.config.AtanConfig;
import me.atan.lyrics.gui.AtanMenuScreen;
import me.atan.lyrics.gui.MenuKey;
import me.atan.lyrics.media.MediaManager;
import me.atan.lyrics.render.HudLyricsRenderer;
import me.atan.lyrics.render.WorldLyricsRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AtanLyricsMod implements ClientModInitializer {
    public static final String MOD_ID = "atanlyrics";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        AtanConfig.load();
        MenuKey.init();
        MediaManager.getInstance().start();

        HudRenderCallback.EVENT.register((graphics, tickDelta) -> HudLyricsRenderer.render(graphics, tickDelta));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            MediaManager.getInstance().tick();
            WorldLyricsRenderer.render();
            while (MenuKey.OPEN_MENU.consumeClick()) {
                if (client.screen == null) {
                    client.setScreen(new AtanMenuScreen(null));
                }
            }
        });

        LOGGER.info("Initialized {} ({})", MOD_ID, Identifier.fromNamespaceAndPath(MOD_ID, "lyrics"));
    }
}
