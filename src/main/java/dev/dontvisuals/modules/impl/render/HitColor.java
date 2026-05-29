package dev.dontvisuals.modules.impl.render;

import dev.dontvisuals.client.util.IOverlayTexture;
import dev.dontvisuals.client.events.impl.EventRender2D;
import dev.dontvisuals.client.events.impl.EventSettingChange;
import dev.dontvisuals.client.events.impl.EventThemeChanged;
import dev.dontvisuals.modules.api.Category;
import dev.dontvisuals.modules.api.Module;
import dev.dontvisuals.modules.settings.impl.NumberSetting;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.MinecraftClient;

public class HitColor extends Module {

    public final NumberSetting alpha = new NumberSetting("setting.alpha", 0.8f, 0.1f, 1.0f, 0.05f);

    public HitColor() {
        super("HitColor", Category.Render, "module.hitcolor.description");
    }

    @Override
    public void onEnable() {
        super.onEnable();
        refreshTexture();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        refreshTexture();
    }

    @EventHandler
    public void onRender2D(EventRender2D event) {
        refreshTexture();
    }

    @EventHandler
    public void onSettingChange(EventSettingChange event) {
        if (event.getSetting() == alpha) {
            refreshTexture();
        }
    }

    @EventHandler
    public void onThemeChanged(EventThemeChanged event) {
        refreshTexture();
    }

    public void refreshTexture() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.gameRenderer == null) return;
        var overlayTexture = mc.gameRenderer.getOverlayTexture();
        if (overlayTexture == null) return;
        ((IOverlayTexture) overlayTexture).dontvisuals$reload();
    }
}