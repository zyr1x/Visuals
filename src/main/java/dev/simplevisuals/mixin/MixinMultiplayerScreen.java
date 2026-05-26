package dev.simplevisuals.mixin;

import dev.simplevisuals.modules.manager.ServerManager;
import dev.simplevisuals.simplevisuals;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiplayerScreen.class)
public abstract class MixinMultiplayerScreen extends Screen {

    protected MixinMultiplayerScreen(Text title) { super(title); }

    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        MultiplayerScreenAccessor self = (MultiplayerScreenAccessor)(Object)this;
        simplevisuals.LOGGER.info("[ServerManager] onInit fired, serverList size: " + self.getServerList().size());
        ServerManager.prioritizeMyServer(self.getServerList());
        self.getServerListWidget().setServers(self.getServerList());
    }
}