package dev.dontvisuals.mixin;

import dev.dontvisuals.modules.impl.utility.HitSound;
import dev.dontvisuals.dontvisuals;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SuppressWarnings("unused")
@Mixin(SoundManager.class)
public abstract class HitSoundSuppressMixin {

    @Inject(
            method = "play(Lnet/minecraft/client/sound/SoundInstance;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void suppressVanillaHitSounds(
            SoundInstance sound,
            CallbackInfo ci
    ) {
        HitSound module = dontvisuals.getInstance().getModuleManager().getModule(HitSound.class);
        if (module == null || !module.shouldSuppressVanilla()) return;

        String id = sound.getId().toString();

        if (id.equals("minecraft:entity.player.attack.crit")
                || id.equals("minecraft:entity.player.attack.knockback")
                || id.equals("minecraft:entity.player.attack.nodamage")
                || id.equals("minecraft:entity.player.attack.strong")
                || id.equals("minecraft:entity.player.attack.sweep")
                || id.equals("minecraft:entity.player.attack.weak")) {
            ci.cancel();
        }
    }
}