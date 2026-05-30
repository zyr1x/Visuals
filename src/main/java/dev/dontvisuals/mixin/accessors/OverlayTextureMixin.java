package dev.dontvisuals.mixin.accessors;

import dev.dontvisuals.client.util.IOverlayTexture;
import dev.dontvisuals.client.managers.ThemeManager;
import dev.dontvisuals.modules.impl.render.HitColor;
import dev.dontvisuals.dontvisuals;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(OverlayTexture.class)
public class OverlayTextureMixin implements IOverlayTexture {

    @Shadow
    private NativeImageBackedTexture texture;

    @Override
    public void dontvisuals$reload() {
        HitColor module = dontvisuals.getInstance().getModuleManager().getModule(HitColor.class);
        NativeImage image = this.texture.getImage();
        if (image == null) return;

        for (int v = 0; v < 16; v++) {
            for (int u = 0; u < 16; u++) {
                if (v < 8) {
                    if (module != null && module.isToggled()) {
                        java.awt.Color theme = ThemeManager.getInstance()
                                .getCurrentTheme().getBackgroundColor();
                        int overlayAlpha = (int) (255 * module.alpha.getValue());
                        image.setColorArgb(u, v, toAbgr(
                                theme.getRed(), theme.getGreen(), theme.getBlue(), overlayAlpha));
                    } else {
                        image.setColorArgb(u, v, toAbgr(255, 0, 0, 127));
                    }
                }
            }
        }

        this.texture.bindTexture();
        this.texture.upload();
    }

    private static int toAbgr(int r, int g, int b, int a) {
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
