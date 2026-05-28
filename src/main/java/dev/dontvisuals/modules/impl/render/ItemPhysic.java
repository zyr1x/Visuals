package dev.dontvisuals.modules.impl.render;

import dev.dontvisuals.modules.api.Category;
import dev.dontvisuals.modules.api.Module;
import dev.dontvisuals.modules.settings.impl.NumberSetting;
import net.minecraft.client.resource.language.I18n;

public class ItemPhysic extends Module {
    public ItemPhysic() {
        super("ItemPhysic", Category.Render, I18n.translate("module.itemphysic.description"));
    }
}
