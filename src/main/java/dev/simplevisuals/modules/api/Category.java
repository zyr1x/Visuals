package dev.simplevisuals.modules.api;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor @Getter
public enum Category {
    Theme("I"),
    Render("H"),
    Utility("I"),
    Hud("LOL");

    private final String icon;
}