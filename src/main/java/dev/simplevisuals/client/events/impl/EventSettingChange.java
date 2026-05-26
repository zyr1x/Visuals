package dev.simplevisuals.client.events.impl;

import dev.simplevisuals.client.events.Event;
import dev.simplevisuals.modules.settings.Setting;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor @Getter
public class EventSettingChange extends Event {
    private final Setting<?> setting;
}