package dev.dontvisuals.client.ui.clickgui.components.impl;

import dev.dontvisuals.modules.settings.impl.BooleanSetting;
import dev.dontvisuals.client.ui.clickgui.components.Component;
import dev.dontvisuals.client.util.animations.Animation;
import dev.dontvisuals.client.util.animations.Easing;
import dev.dontvisuals.client.util.math.MathUtils;
import dev.dontvisuals.client.util.renderer.fonts.Fonts;
import dev.dontvisuals.client.util.renderer.Render2D;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.resource.language.I18n;

import java.awt.*;
import dev.dontvisuals.client.managers.ThemeManager;

public class BooleanComponent extends Component {

    private final BooleanSetting setting;
    private final Animation toggleAnim = new Animation(200, 1f, false, Easing.BOTH_SINE);
    private boolean animInit = false;

    public BooleanComponent(BooleanSetting setting) {
        super(setting.getName());
        this.setting = setting;
        this.visible = setting::isVisible;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        float ga = Math.max(0f, Math.min(1f, getGlobalAlpha()));

        if (!animInit) {
            toggleAnim.setDuration(0); toggleAnim.update(setting.getValue());
            toggleAnim.setDuration(200); animInit = true;
        } else {
            toggleAnim.update(setting.getValue());
        }
        float ta = (float) toggleAnim.getValue();

        Color accent    = ThemeManager.getInstance().getCurrentTheme().getAccentColor();
        Color themeText = ThemeManager.getInstance().getCurrentTheme().getTextColor();

        // ── Фон строки ──────────────────────────────────────────────────────
        Render2D.drawRoundedRect(ctx.getMatrices(), x, y, width, height, 5f,
                new Color(255, 255, 255, (int) (8 * ga)));

        // ── Текст ────────────────────────────────────────────────────────────
        Render2D.drawFont(ctx.getMatrices(), Fonts.BOLD.getFont(7.5f),
                I18n.translate(setting.getName()),
                x + 6f, y + (height - Fonts.BOLD.getHeight(7.5f)) / 2f,
                new Color(themeText.getRed(), themeText.getGreen(), themeText.getBlue(),
                        (int) (themeText.getAlpha() * ga)));

        // ── Toggle (справа) ───────────────────────────────────────────────────
        float sw = 22f, sh = 11f;
        float sx = x + width - sw - 6f;
        float sy = y + (height - sh) / 2f;

        // Трек (интерполяция цвета)
        Color trackOff = new Color(40, 40, 55, (int) (180 * ga));
        Color trackOn  = new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), (int) (210 * ga));
        int r = (int) (trackOff.getRed()   + (trackOn.getRed()   - trackOff.getRed())   * ta);
        int g = (int) (trackOff.getGreen() + (trackOn.getGreen() - trackOff.getGreen()) * ta);
        int b = (int) (trackOff.getBlue()  + (trackOn.getBlue()  - trackOff.getBlue())  * ta);
        int a = (int) (trackOff.getAlpha() + (trackOn.getAlpha() - trackOff.getAlpha()) * ta);
        Render2D.drawRoundedRect(ctx.getMatrices(), sx, sy, sw, sh, sh / 2f, new Color(r, g, b, a));

        // Ползунок
        float pad = 2f;
        float th  = sh - pad * 2;
        float tx  = sx + pad + (sw - th - pad * 2) * ta;
        Render2D.drawRoundedRect(ctx.getMatrices(), tx, sy + pad, th, th, th / 2f,
                new Color(255, 255, 255, (int) (230 * ga)));
    }

    @Override
    public void mouseClicked(double mx, double my, int btn) {
        float sw = 22f, sh = 11f;
        float sx = x + width - sw - 6f;
        float sy = y + (height - sh) / 2f;
        if (btn == 0 && MathUtils.isHovered(x, y, width, height, (float) mx, (float) my)) {
            setting.setValue(!setting.getValue());
        }
    }

    @Override public void mouseReleased(double mx, double my, int btn) {}
    @Override public void keyPressed(int k, int s, int m) {}
    @Override public void keyReleased(int k, int s, int m) {}
    @Override public void charTyped(char c, int m) {}
}