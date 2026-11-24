package me.atan.lyrics.gui;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public class MenuKey {
    public static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath("atanlyrics", "key.categories.atanlyrics")
    );

    public static final KeyMapping OPEN_MENU = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.atanlyrics.open_menu",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_RIGHT_SHIFT,
            CATEGORY
    ));

    public static void init() {
    }
}
