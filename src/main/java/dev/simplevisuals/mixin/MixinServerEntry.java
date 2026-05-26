package dev.simplevisuals.mixin;

import dev.simplevisuals.modules.manager.ServerManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerServerListWidget;
import net.minecraft.client.network.ServerInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiplayerServerListWidget.ServerEntry.class)
public abstract class MixinServerEntry {

    @Inject(method = "render", at = @At("HEAD"))
    private void onRender(DrawContext context, int index, int y, int x,
                          int entryWidth, int entryHeight,
                          int mouseX, int mouseY, boolean hovered,
                          float tickDelta, CallbackInfo ci) {

        // Достаём ServerInfo из Entry через accessor
        ServerInfo info = ((ServerEntryAccessor)(Object)this).getServer();

        if (ServerManager.isMyServer(info)) {
            // Полупрозрачный жёлтый фон: ARGB 0x44FFFF00
            context.fill(x, y, x + entryWidth, y + entryHeight, 0x44FFFF00);
        }
    }
}