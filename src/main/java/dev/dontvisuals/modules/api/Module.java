package dev.dontvisuals.modules.api;

import dev.dontvisuals.dontvisuals;
import dev.dontvisuals.modules.settings.Setting;
import dev.dontvisuals.modules.settings.api.Bind;
import dev.dontvisuals.client.util.Wrapper;
import dev.dontvisuals.client.util.notify.Notify;
import dev.dontvisuals.client.util.notify.NotifyIcons;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.resource.language.I18n;

import java.util.ArrayList;
import java.util.List;

@Getter
public abstract class Module implements Wrapper {
    private final String name, description;
    private final Category category;
    protected boolean toggled;
    protected boolean silent = false;
    @Setter private Bind bind = new Bind(-1, false);
    private final List<Setting<?>> settings = new ArrayList<>();

    public Module(String name, Category category, String description) {
        this.name = name;
        this.category = category;
        this.description = description;
    }

    public Module(String name, Category category) {
        this(name, category, name);
    }

    public void onEnable() {
        toggled = true;
        dontvisuals.getInstance().getEventHandler().subscribe(this);
        if (!silent && !fullNullCheck() && !name.equals("UI")) {
            String translatedName = I18n.translate(name);
            String msg = I18n.translate("notify.featureEnabled", translatedName);
            dontvisuals.getInstance().getNotifyManager().add(new Notify(NotifyIcons.successIcon, msg, 1000));
        }
    }

    public void onDisable() {
        toggled = false;
        dontvisuals.getInstance().getEventHandler().unsubscribe(this);
        if (!silent && !fullNullCheck() && !name.equals("UI")) {
            String translatedName = I18n.translate(name);
            String msg = I18n.translate("notify.featureDisabled", translatedName);
            dontvisuals.getInstance().getNotifyManager().add(new Notify(NotifyIcons.failIcon, msg, 1000));
        }
    }

    public void setToggled(boolean toggled) {
        if (toggled) onEnable();
        else onDisable();
        try {
            dev.dontvisuals.client.managers.AutoSaveManager asm = dontvisuals.getInstance().getAutoSaveManager();
            if (asm != null) asm.scheduleAutoSave();
        } catch (Throwable ignored) {}
    }

    public void toggle() {
        setToggled(!toggled);
    }

    public static boolean fullNullCheck() {
        return mc.player == null || mc.world == null;
    }
}