package dev.dontvisuals.client.events.impl;

import dev.dontvisuals.client.events.Event;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.entity.Entity;

@AllArgsConstructor
@Getter
@Setter
public class TotemPopEvent extends Event {
    Entity entity;
}
