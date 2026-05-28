package dev.dontvisuals.modules.impl.render;

import dev.dontvisuals.client.managers.ThemeManager;
import dev.dontvisuals.modules.api.Category;
import dev.dontvisuals.modules.api.Module;
import dev.dontvisuals.modules.settings.impl.NumberSetting;

import java.awt.*;
import net.minecraft.client.resource.language.I18n;

public class CustomFog extends Module {

    private final ThemeManager themeManager;
    private final NumberSetting fogDistance = new NumberSetting(
            "setting.fogDistance",
            64.0f,
            0.0f,
            256.0f,
            1.0f
    );

    public CustomFog() {
        super("CustomFog", Category.Render, I18n.translate("module.customfog.description"));
        this.themeManager = ThemeManager.getInstance();
    }

    public Color getSkyColor() {
        // Получаем актуальный цвет темы (включая градиентные темы)
        return themeManager.getCurrentTheme().getBackgroundColor();
    }

    public Color getSkyColorSecondary() {
        // Получаем актуальный цвет темы (включая градиентные темы)
        return themeManager.getCurrentTheme().getSecondaryBackgroundColor();
    }

    public float getFogDistance() {
        return fogDistance.getValue();
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }

}
