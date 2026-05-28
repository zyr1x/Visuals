package dev.dontvisuals.mixin;

import dev.dontvisuals.modules.impl.utility.AutoSprint;
import dev.dontvisuals.dontvisuals;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public class AutoSprintMixin {
    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        AutoSprint module = dontvisuals.getInstance().getModuleManager().getModule(AutoSprint.class);
        if (module != null && module.isToggled()) {
            module.onTick(new dev.dontvisuals.client.events.impl.EventTick());
        }
    }
}