package dev.dontvisuals.mixin;

import dev.dontvisuals.client.util.Wrapper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;

/**
 * SHOULD_TINT теперь выставляется в LivingEntityRendererMixin через updateRenderState,
 * поэтому tick-injection здесь не нужен.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity implements Wrapper {

    public LivingEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }
}
