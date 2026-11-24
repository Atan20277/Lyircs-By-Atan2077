package me.atan.lyrics.gui;

import net.minecraft.client.gui.screens.Screen;

public class ModMenuIntegration {
    public Screen createConfigScreen(Screen parent) {
        return new AtanMenuScreen(parent);
    }
}
