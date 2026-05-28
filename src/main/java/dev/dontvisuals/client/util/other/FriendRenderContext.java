package dev.dontvisuals.client.util.other;

import net.minecraft.entity.LivingEntity;

public class FriendRenderContext {
    public static final ThreadLocal<LivingEntity> CURRENT = new ThreadLocal<>();
} 