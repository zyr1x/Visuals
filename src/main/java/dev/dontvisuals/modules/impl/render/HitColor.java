package dev.dontvisuals.modules.impl.render;

import dev.dontvisuals.client.util.IOverlayTexture;
import dev.dontvisuals.client.events.impl.EventRender2D;
import dev.dontvisuals.client.events.impl.EventSettingChange;
import dev.dontvisuals.client.events.impl.EventThemeChanged;
import dev.dontvisuals.modules.api.Category;
import dev.dontvisuals.modules.api.Module;
import dev.dontvisuals.modules.settings.api.Nameable;
import dev.dontvisuals.modules.settings.impl.EnumSetting;
import dev.dontvisuals.modules.settings.impl.NumberSetting;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.MinecraftClient;

public class HitColor extends Module {

    public enum TintMode implements Nameable {
        /** Режим 1 — только скин светится, броня остаётся обычной */
        SKIN_ONLY("Скин"),
        /** Режим 2 — всё тело светится, включая броню */
        FULL("Полностью");

        private final String name;
        TintMode(String name) { this.name = name; }

        @Override
        public String getName() { return name; }
    }

    public final EnumSetting<TintMode> mode = new EnumSetting<>("setting.mode", TintMode.SKIN_ONLY);
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
        if (event.getSetting() == alpha || event.getSetting() == mode) {
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
