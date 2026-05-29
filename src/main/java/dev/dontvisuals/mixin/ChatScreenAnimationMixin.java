package dev.dontvisuals.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.dontvisuals.modules.impl.render.BetterMinecraft;
import dev.dontvisuals.dontvisuals;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatScreen.class)
public abstract class ChatScreenAnimationMixin {

    @Inject(method = "init", at = @At("HEAD"))
    private void dontvisuals$onChatOpen(CallbackInfo ci) {
        BetterMinecraft module = dontvisuals.getInstance().getModuleManager().getModule(BetterMinecraft.class);
        if (module == null || !module.isToggled() || !module.smoothChat.getValue()) return;

        module.getChatAnimation().reset();
        module.getChatAnimation().update(true);
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void dontvisuals$applyChatAnimation(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        BetterMinecraft module = dontvisuals.getInstance().getModuleManager().getModule(BetterMinecraft.class);
        if (module == null || !module.isToggled() || !module.smoothChat.getValue()) return;

        float value = (float) module.getChatAnimation().getValue();

        MatrixStack matrices = context.getMatrices();
        matrices.push();

        float slideOffset = 20.0f;
        float slide = slideOffset * (1.0f - value);
        matrices.translate(0, slide, 0);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1f, 1f, 1f, value);
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void dontvisuals$cleanupChatAnimation(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        BetterMinecraft module = dontvisuals.getInstance().getModuleManager().getModule(BetterMinecraft.class);
        if (module == null || !module.isToggled() || !module.smoothChat.getValue()) return;

        context.getMatrices().pop();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.disableBlend();
    }
}