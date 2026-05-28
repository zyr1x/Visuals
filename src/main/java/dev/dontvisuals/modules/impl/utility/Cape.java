package dev.dontvisuals.modules.impl.utility;

import dev.dontvisuals.modules.api.Category;
import dev.dontvisuals.modules.api.Module;
import dev.dontvisuals.modules.settings.impl.BooleanSetting;
import dev.dontvisuals.modules.settings.impl.ListSetting;
import net.minecraft.client.resource.language.I18n;

public class Cape extends Module {

    public Cape() {
        super("Cape", Category.Utility, I18n.translate("module.cape.description"));
    }

}
