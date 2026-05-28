package dev.dontvisuals.modules.impl.render;

import dev.dontvisuals.modules.api.Category;
import dev.dontvisuals.modules.api.Module;
import dev.dontvisuals.modules.settings.impl.BooleanSetting;
import net.minecraft.client.resource.language.I18n;
import org.jetbrains.annotations.NotNull;

public class NoRender extends Module {

    public @NotNull BooleanSetting hurtCam = new BooleanSetting(I18n.translate("NoHurtCam"), true);
    public @NotNull BooleanSetting fire = new BooleanSetting(I18n.translate("Fire"), true);
    public @NotNull BooleanSetting totem = new BooleanSetting(I18n.translate("Totem"), true);
    public @NotNull BooleanSetting potions = new BooleanSetting(I18n.translate("Potions"), true);
    public @NotNull BooleanSetting blocks = new BooleanSetting(I18n.translate("Blocks"), true);
    public @NotNull BooleanSetting scoreboard = new BooleanSetting(I18n.translate("ScoreBoard"), false);
    public @NotNull BooleanSetting bossBar = new BooleanSetting(I18n.translate("BossBar"), false);
    public @NotNull BooleanSetting particles = new BooleanSetting(I18n.translate("Particles"), true);
    public @NotNull BooleanSetting armor = new BooleanSetting(I18n.translate("Armor"), false);
    public @NotNull BooleanSetting limbs = new BooleanSetting(I18n.translate("Limbs"), false);
    public @NotNull BooleanSetting vignette = new BooleanSetting(I18n.translate("Vignette"), true);

    public NoRender() {
        super("NoRender", Category.Render, I18n.translate("module.norender.description"));
    }
}