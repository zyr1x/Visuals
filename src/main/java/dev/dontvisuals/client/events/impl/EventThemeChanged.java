package dev.dontvisuals.client.events.impl;

import dev.dontvisuals.client.events.Event;
import dev.dontvisuals.client.managers.ThemeManager;

public class EventThemeChanged extends Event {
    private final ThemeManager.Theme theme;
    
    public EventThemeChanged(ThemeManager.Theme theme) {
        this.theme = theme;
    }
    
    public ThemeManager.Theme getTheme() {
        return theme;
    }
} 