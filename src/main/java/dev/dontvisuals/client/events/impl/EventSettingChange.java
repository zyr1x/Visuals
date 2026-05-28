package dev.dontvisuals.client.events.impl;

import dev.dontvisuals.client.events.Event;
import dev.dontvisuals.modules.settings.Setting;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor @Getter
public class EventSettingChange extends Event {
    private final Setting<?> setting;
}