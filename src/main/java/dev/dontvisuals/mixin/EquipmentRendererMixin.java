package dev.dontvisuals.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.dontvisuals.modules.impl.render.HitColor;
import dev.dontvisuals.dontvisuals;
import dev.dontvisuals.util.HitColorTintState;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.entity.equipment.EquipmentRenderer;
import net.minecraft.util.Identifier;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EquipmentRenderer.class)
public class EquipmentRendererMixin {

    // 1) Меняем render layer — getArmorCutoutNoCull не читает overlay (Sampler1 отсутствует в шейдере).
    //    getEntityCutoutNoCull использует entity шейдер который overlay читает.
    @Redirect(method = "*", require = 0,
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/render/RenderLayer;getArmorCutoutNoCull(Lnet/minecraft/util/Identifier;)Lnet/minecraft/client/render/RenderLayer;"))
    private RenderLayer dontvisuals$armorLayer(Identifier texture) {
        HitColor module = dontvisuals.getInstance().getModuleManager().getModule(HitColor.class);
        if (module != null && module.isToggled()
                && Boolean.TRUE.equals(HitColorTintState.SHOULD_TINT.get())
                && module.mode.getValue() == HitColor.TintMode.FULL) {
            return RenderLayer.getEntityCutoutNoCull(texture); // entity шейдер = overlay работает
        }
        return RenderLayer.getArmorCutoutNoCull(texture);
    }

    // 2) Меняем UV — DEFAULT_UV (строка 10 = без эффекта) на hurt UV (строка 0 = цвет темы).
    @ModifyExpressionValue(method = "*",
            at = @At(value = "FIELD",
                    target = "Lnet/minecraft/client/render/OverlayTexture;DEFAULT_UV:I",
                    opcode = Opcodes.GETSTATIC),
            allow = Integer.MAX_VALUE)
    private int dontvisuals$overlayUV(int original) {
        HitColor module = dontvisuals.getInstance().getModuleManager().getModule(HitColor.class);
        if (module != null && module.isToggled()
                && Boolean.TRUE.equals(HitColorTintState.SHOULD_TINT.get())
                && module.mode.getValue() == HitColor.TintMode.FULL) {
            return OverlayTexture.packUv(0, 0);
        }
        return original;
    }
}
