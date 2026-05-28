package dev.dontvisuals.modules.impl.render;

import dev.dontvisuals.modules.api.Category;
import dev.dontvisuals.modules.api.Module;
import net.minecraft.client.resource.language.I18n;

public class Fullbright extends Module {

    public Fullbright() {
        super("Fullbright", Category.Render, I18n.translate("module.fullbright.description"));
    }
}