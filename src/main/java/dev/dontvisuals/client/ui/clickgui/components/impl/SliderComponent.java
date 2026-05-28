package dev.dontvisuals.client.ui.clickgui.components.impl;

import dev.dontvisuals.modules.settings.impl.NumberSetting;
import dev.dontvisuals.client.ui.clickgui.components.Component;
import dev.dontvisuals.client.util.math.MathUtils;
import dev.dontvisuals.client.util.renderer.fonts.Fonts;
import dev.dontvisuals.client.util.renderer.Render2D;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.util.math.MathHelper;

import java.awt.*;
import dev.dontvisuals.client.managers.ThemeManager;

public class SliderComponent extends Component {

    private final NumberSetting setting;
    private boolean drag;

    private float animatedRatio = -1f;
    private float hoverAmt      = 0f;

    public SliderComponent(NumberSetting setting) {
        super(setting.getName());
        this.setting   = setting;
        this.addHeight = () -> 4f;
        this.visible   = setting::isVisible;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        if (!visible.get() || getGlobalAlpha() <= 0.01f) return;

        float ga = Math.max(0f, Math.min(1f, getGlobalAlpha()));

        // Hover
        boolean hov = MathUtils.isHovered(x, y, width, height, (float) mouseX, (float) mouseY) || drag;
        hoverAmt += ((hov ? 1f : 0f) - hoverAmt) * 0.2f;

        // Перетаскивание
        if (drag) {
            float v = MathHelper.clamp(
                    MathUtils.round((mouseX - x - 6f) / (width - 12f)
                                    * (setting.getMax() - setting.getMin()) + setting.getMin(),
                            setting.getIncrement()),
                    setting.getMin(), setting.getMax());
            setting.setValue(v);
        }

        float ratio = (float) ((setting.getValue() - setting.getMin())
                / (setting.getMax() - setting.getMin()));
        ratio = MathHelper.clamp(ratio, 0f, 1f);
        if (animatedRatio < 0f) animatedRatio = ratio;
        animatedRatio += (ratio - animatedRatio) * 0.16f;

        Color accent = ThemeManager.getInstance().getCurrentTheme().getAccentColor();
        int   textA  = (int) (255 * ga);

        // ── Фон строки ──────────────────────────────────────────────────────
        Render2D.drawRoundedRect(ctx.getMatrices(), x, y, width, height + 4f, 5f,
                new Color(255, 255, 255, (int) ((10 + 6 * hoverAmt) * ga)));

        // ── Название ─────────────────────────────────────────────────────────
        Color themeText = ThemeManager.getInstance().getCurrentTheme().getTextColor();
        Render2D.drawFont(ctx.getMatrices(), Fonts.BOLD.getFont(7.5f),
                I18n.translate(setting.getName()),
                x + 6f, y + 3f,
                new Color(themeText.getRed(), themeText.getGreen(), themeText.getBlue(),
                        (int) (themeText.getAlpha() * ga)));

        // ── Значение справа ───────────────────────────────────────────────────
        String valStr = String.valueOf(setting.getValue());
        float  valW   = Fonts.BOLD.getWidth(valStr, 7f);
        Render2D.drawFont(ctx.getMatrices(), Fonts.BOLD.getFont(7f), valStr,
                x + width - valW - 6f, y + 3f,
                new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), textA));

        // ── Трек ─────────────────────────────────────────────────────────────
        float trackX = x + 6f;
        float trackY = y + height - 4f;
        float trackW = width - 12f;
        float trackH = 3f;

        Render2D.drawRoundedRect(ctx.getMatrices(), trackX, trackY, trackW, trackH, 1.5f,
                new Color(50, 50, 65, (int) (160 * ga)));

        // ── Заполнение ────────────────────────────────────────────────────────
        float fillW = trackW * animatedRatio;
        if (fillW > 0.5f) {
            Render2D.drawRoundedRect(ctx.getMatrices(), trackX, trackY, fillW, trackH, 1.5f,
                    new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), (int) (220 * ga)));
        }

        // ── Ручка ─────────────────────────────────────────────────────────────
        float knobR  = 4f + 1.5f * hoverAmt;
        float knobX  = trackX + fillW - knobR;
        float knobY  = trackY + trackH / 2f - knobR;
        Render2D.drawRoundedRect(ctx.getMatrices(), knobX, knobY, knobR * 2f, knobR * 2f, knobR,
                new Color(255, 255, 255, (int) (230 * ga)));
        // Обводка ручки акцентом
        if (hoverAmt > 0.05f) {
            Render2D.drawRoundedRect(ctx.getMatrices(),
                    knobX - 1f, knobY - 1f, knobR * 2f + 2f, knobR * 2f + 2f, knobR + 1f,
                    new Color(accent.getRed(), accent.getGreen(), accent.getBlue(),
                            (int) (120 * hoverAmt * ga)));
        }
    }

    @Override
    public void mouseClicked(double mx, double my, int btn) {
        if (btn == 0 && MathUtils.isHovered(x + 6f, y + height - 6f, width - 12f, 8f,
                (float) mx, (float) my)) {
            drag = true;
        }
    }

    @Override public void mouseReleased(double mx, double my, int btn) { if (btn == 0) drag = false; }
    @Override public void keyPressed(int k, int s, int m) {}
    @Override public void keyReleased(int k, int s, int m) {}
    @Override public void charTyped(char c, int m) {}
}