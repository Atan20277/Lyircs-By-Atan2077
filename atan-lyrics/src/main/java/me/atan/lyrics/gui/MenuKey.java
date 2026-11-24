package me.atan.lyrics.gui;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public final class MenuKey {
    public static final KeyMapping OPEN_MENU = new KeyMapping(
            "key.atanlyrics.open_menu",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_RIGHT_SHIFT,
            "key.categories.atanlyrics"
    );
    private MenuKey() {}
}
