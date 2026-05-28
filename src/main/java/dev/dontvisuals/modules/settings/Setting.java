package dev.dontvisuals.modules.settings;

import dev.dontvisuals.dontvisuals;
import dev.dontvisuals.client.events.impl.EventSettingChange;
import lombok.*;

import java.util.function.Supplier;

@Getter @Setter
public abstract class Setting<Value> {

    private final String name;
    protected Value value, defaultValue;
    private Supplier<Boolean> visible = () -> true;

    public Setting(String name, Value defaultValue) {
        this.name = name;
        this.defaultValue = defaultValue;
        this.value = defaultValue;
    }

    public Setting(String name, Value defaultValue, Supplier<Boolean> visible) {
        this.name = name;
        this.defaultValue = defaultValue;
        this.value = defaultValue;
        this.visible = visible;
    }

    public void setValue(Value value) {
        EventSettingChange event = new EventSettingChange(this);
        dontvisuals.getInstance().getEventHandler().post(event);
        if (!event.isCancelled()) {
            this.value = value;
            // Планируем автосохранение после успешного применения
            try {
                dev.dontvisuals.client.managers.AutoSaveManager asm = dontvisuals.getInstance().getAutoSaveManager();
                if (asm != null) asm.scheduleAutoSave();
            } catch (Throwable ignored) {}
        }
    }

    public void reset() {
        this.value = defaultValue;
    }

    public boolean isVisible() {
        return visible.get();
    }
}