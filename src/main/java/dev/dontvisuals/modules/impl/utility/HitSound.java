package dev.dontvisuals.modules.impl.utility;

import dev.dontvisuals.modules.api.Category;
import dev.dontvisuals.modules.api.Module;
import dev.dontvisuals.modules.settings.impl.BooleanSetting;
import dev.dontvisuals.modules.settings.impl.ListSetting;
import dev.dontvisuals.modules.settings.impl.NumberSetting;
import org.jetbrains.annotations.NotNull;
import net.minecraft.client.resource.language.I18n;

public class HitSound extends Module {

    private final @NotNull BooleanSetting bell  = new BooleanSetting("mode.bell",  true,  () -> false);
    private final @NotNull BooleanSetting crime = new BooleanSetting("mode.crime", false, () -> false);
    private final @NotNull BooleanSetting nya   = new BooleanSetting("mode.nya",   false, () -> false);
    private final @NotNull BooleanSetting skeet = new BooleanSetting("mode.skeet", false, () -> false);
    private final @NotNull BooleanSetting uwu   = new BooleanSetting("mode.uwu",   false, () -> false);
    private final @NotNull BooleanSetting pook = new BooleanSetting("mode.pook", false, () -> false);

    private final @NotNull BooleanSetting vanillaSound = new BooleanSetting(
            "setting.vanilla_sound", false, () -> true
    );

    private final @NotNull NumberSetting Volume = new NumberSetting(
            "setting.volume", 1.00f, 0.1f, 2.0f, 0.01f
    );

    private final @NotNull ListSetting mode = new ListSetting(
            "setting.sound", true, bell, crime, nya, skeet, uwu, pook
    );

    public HitSound() {
        super("HitSound", Category.Utility, I18n.translate("module.hitsound.description"));
    }

    public @NotNull String getSelectedSound() {
        if (crime.getValue()) return "dontvisuals:crime";
        if (nya.getValue())   return "dontvisuals:nya";
        if (skeet.getValue()) return "dontvisuals:skeet";
        if (uwu.getValue())   return "dontvisuals:uwu";
        if (pook.getValue())   return "dontvisuals:pook";
        return "dontvisuals:bell";
    }

    public @NotNull NumberSetting getVolume() {
        return Volume;
    }

    public boolean shouldSuppressVanilla() {
        return isToggled() && !vanillaSound.getValue();
    }
}