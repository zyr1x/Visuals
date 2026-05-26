package dev.simplevisuals.modules.impl.render;

import dev.simplevisuals.modules.api.Category;
import dev.simplevisuals.modules.api.Module;
import net.minecraft.client.resource.language.I18n;

public class Fullbright extends Module {

    public Fullbright() {
        super("Fullbright", Category.Render, I18n.translate("module.fullbright.description"));
    }
}