package dev.dontvisuals.mixin;

import dev.dontvisuals.client.util.other.FriendRenderContext;
import dev.dontvisuals.modules.impl.render.HitColor;
import dev.dontvisuals.dontvisuals;
import dev.dontvisuals.util.HitColorTintState;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<T extends LivingEntity, S extends LivingEntityRenderState, M extends EntityModel<S>> {

    @Shadow
    protected abstract Identifier getTexture(S state);

    /**
     * updateRenderState вызывается НЕПОСРЕДСТВЕННО перед render(S state, ...) для каждой entity.
     * Здесь у нас есть прямой доступ к entity.hurtTime, поэтому SHOULD_TINT всегда корректен.
     *
     * Порядок для каждой entity:
     *   1. updateRenderState(entity, state, tickDelta) → HEAD → мы устанавливаем SHOULD_TINT
     *   2. render(state, matrices, ...) → броня читает SHOULD_TINT (верное значение)
     *   3. render(state, ...) RETURN → мы сбрасываем SHOULD_TINT
     */
    @Inject(
            method = "updateRenderState(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;F)V",
            at = @At("RETURN")
    )
    private void dontvisuals$setTintState(T entity, S state, float tickDelta, CallbackInfo ci) {
        HitColor module = dontvisuals.getInstance().getModuleManager().getModule(HitColor.class);
        if (module != null && module.isToggled()) {
            // entity — это T extends LivingEntity, hurtTime — public int на LivingEntity
            HitColorTintState.SHOULD_TINT.set(entity.hurtTime > 0);
        } else {
            HitColorTintState.SHOULD_TINT.set(false);
        }
    }

    /**
     * Сбрасываем SHOULD_TINT и FriendRenderContext после завершения рендера entity.
     */
    @Inject(
            method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At("RETURN")
    )
    private void dontvisuals$clearOwner(S state, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        FriendRenderContext.CURRENT.remove();
        HitColorTintState.SHOULD_TINT.set(false);
    }
}
