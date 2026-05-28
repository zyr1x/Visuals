package dev.dontvisuals.client.ui.clickgui.components.impl;

import dev.dontvisuals.modules.settings.impl.EnumSetting;
import dev.dontvisuals.client.ui.clickgui.components.Component;
import dev.dontvisuals.modules.settings.api.Nameable;
import dev.dontvisuals.client.util.animations.Animation;
import dev.dontvisuals.client.util.animations.Easing;
import dev.dontvisuals.client.util.math.MathUtils;
import dev.dontvisuals.client.util.renderer.fonts.Fonts;
import dev.dontvisuals.client.util.renderer.Render2D;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.resource.language.I18n;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import dev.dontvisuals.client.managers.ThemeManager;

public class EnumComponent extends Component {

    private final EnumSetting<?> setting;
    private final Animation openAnim = new Animation(250, 1f, false, Easing.BOTH_SINE);
    private final Map<Enum<?>, Animation> pickAnims = new HashMap<>();
    private boolean open;

    public EnumComponent(EnumSetting<?> setting) {
        super(setting.getName());
        this.setting = setting;
        for (Enum<?> e : setting.getValue().getClass().getEnumConstants())
            pickAnims.put(e, new Animation(220, 1f, false, Easing.BOTH_SINE));
        this.addHeight = () -> openAnim.getValue() > 0
                ? (setting.getValue().getClass().getEnumConstants().length * 18f) * (float) openAnim.getValue()
                : 0f;
        this.visible = setting::isVisible;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        float ga = Math.max(0f, Math.min(1f, getGlobalAlpha()));
        openAnim.update(open);

        Color themeText = ThemeManager.getInstance().getCurrentTheme().getTextColor();
        Color accent    = ThemeManager.getInstance().getCurrentTheme().getAccentColor();

        // ── Фон строки ──────────────────────────────────────────────────────
        Render2D.drawRoundedRect(ctx.getMatrices(), x, y, width, height, 5f,
                new Color(255, 255, 255, (int) (8 * ga)));

        // ── Текст: Название: Значение ─────────────────────────────────────────
        String label = I18n.translate(setting.getName()) + ": "
                + I18n.translate(setting.currentEnumName());
        Render2D.drawFont(ctx.getMatrices(), Fonts.BOLD.getFont(7.5f),
                label, x + 6f, y + (height - Fonts.BOLD.getHeight(7.5f)) / 2f,
                new Color(themeText.getRed(), themeText.getGreen(), themeText.getBlue(),
                        (int) (themeText.getAlpha() * ga)));

        // Стрелка (▾)
        String arrow = open ? "▴" : "▾";
        float  aw    = Fonts.REGULAR.getWidth(arrow, 7f);
        Render2D.drawFont(ctx.getMatrices(), Fonts.REGULAR.getFont(7f), arrow,
                x + width - aw - 6f, y + (height - Fonts.REGULAR.getHeight(7f)) / 2f,
                new Color(themeText.getRed(), themeText.getGreen(), themeText.getBlue(),
                        (int) (160 * ga)));

        // ── Выпадающий список ────────────────────────────────────────────────
        if (openAnim.getValue() > 0.01f) {
            float listA = (float) Math.min(1f, openAnim.getValue() * ga);
            float yOff  = height;
            for (Enum<?> e : setting.getValue().getClass().getEnumConstants()) {
                Animation pa = pickAnims.get(e);
                pa.update(e == setting.getValue());
                float pa2 = (float) pa.getValue();

                // Фон выбранного
                if (pa2 > 0.01f) {
                    Render2D.drawRoundedRect(ctx.getMatrices(), x + 2f, y + yOff + 1f,
                            width - 4f, 16f, 4f,
                            new Color(accent.getRed(), accent.getGreen(), accent.getBlue(),
                                    (int) (60 * pa2 * listA)));
                }
                // Текст пункта
                Render2D.startScissor(ctx, x, y + yOff, width, 18f);
                float slide = (1f - (float) openAnim.getValue()) * 6f;
                Render2D.drawFont(ctx.getMatrices(), Fonts.BOLD.getFont(7.5f),
                        I18n.translate(((Nameable) e).getName()),
                        x + 10f + slide, y + yOff + 4f,
                        new Color(themeText.getRed(), themeText.getGreen(), themeText.getBlue(),
                                (int) (themeText.getAlpha() * listA)));
                Render2D.stopScissor(ctx);
                yOff += 18f;
            }
        }
    }

    @Override
    public void mouseClicked(double mx, double my, int btn) {
        if (MathUtils.isHovered(x, y, width, height, (float) mx, (float) my)) {
            if (btn == 0) { setting.increaseEnum(); return; }
            if (btn == 1) { open = !open; return; }
        }
        if (open && btn == 0) {
            float yOff = height;
            for (Enum<?> e : setting.getValue().getClass().getEnumConstants()) {
                if (MathUtils.isHovered(x, y + yOff, width, 18f, (float) mx, (float) my)) {
                    setting.setEnumValue(((Nameable) e).getName()); break;
                }
                yOff += 18f;
            }
        }
    }

    @Override public void mouseReleased(double mx, double my, int btn) {}
    @Override public void keyPressed(int k, int s, int m) {}
    @Override public void keyReleased(int k, int s, int m) {}
    @Override public void charTyped(char c, int m) {}
}