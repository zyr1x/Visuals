package dev.simplevisuals.client.ui.clickgui.components.impl;

import dev.simplevisuals.client.managers.ThemeManager;
import dev.simplevisuals.client.ui.clickgui.components.Component;
import dev.simplevisuals.client.util.animations.Animation;
import dev.simplevisuals.client.util.animations.Easing;
import dev.simplevisuals.client.util.math.MathUtils;
import dev.simplevisuals.client.util.renderer.Render2D;
import dev.simplevisuals.client.util.renderer.fonts.Fonts;
import dev.simplevisuals.modules.settings.impl.BooleanSetting;
import dev.simplevisuals.modules.settings.impl.ListSetting;
import lombok.Getter;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.resource.language.I18n;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class ListComponent extends Component {

    private final ListSetting setting;
    private final Map<BooleanSetting, Animation> pickAnims = new HashMap<>();

    @Getter private final Animation openAnim = new Animation(250, 1f, false, Easing.BOTH_SINE);
    private boolean open;

    public ListComponent(ListSetting setting) {
        super(setting.getName());
        this.setting = setting;
        for (BooleanSetting bs : setting.getValue())
            pickAnims.put(bs, new Animation(220, 1f, false, Easing.BOTH_SINE));
        this.visible   = setting::isVisible;
        this.addHeight = () -> openAnim.getValue() > 0
                ? (setting.getValue().size() * 18f) * (float) openAnim.getValue()
                : 0f;
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

        // ── Заголовок ────────────────────────────────────────────────────────
        Render2D.drawFont(ctx.getMatrices(), Fonts.BOLD.getFont(7.5f),
                I18n.translate(setting.getName()),
                x + 6f, y + (height - Fonts.BOLD.getHeight(7.5f)) / 2f,
                new Color(themeText.getRed(), themeText.getGreen(), themeText.getBlue(),
                        (int) (themeText.getAlpha() * ga)));

        // Счётчик
        String counter = "(" + setting.getToggled().size() + "/" + setting.getValue().size() + ")";
        float  cw      = Fonts.REGULAR.getWidth(counter, 7f);
        Render2D.drawFont(ctx.getMatrices(), Fonts.REGULAR.getFont(7f), counter,
                x + width - cw - 6f, y + (height - Fonts.REGULAR.getHeight(7f)) / 2f,
                new Color(120, 120, 140, (int) (200 * ga)));

        // ── Список элементов ─────────────────────────────────────────────────
        if (openAnim.getValue() > 0.01f) {
            float listA = (float) Math.min(1f, openAnim.getValue() * ga);
            float yOff  = height;
            for (BooleanSetting bs : setting.getValue()) {
                Animation pa = pickAnims.get(bs);
                pa.update(bs.getValue());
                float pa2 = (float) pa.getValue();

                if (pa2 > 0.01f) {
                    Render2D.drawRoundedRect(ctx.getMatrices(), x + 2f, y + yOff + 1f,
                            width - 4f, 16f, 4f,
                            new Color(accent.getRed(), accent.getGreen(), accent.getBlue(),
                                    (int) (55 * pa2 * listA)));
                }

                Render2D.startScissor(ctx, x, y + yOff, width, 18f);
                float slide = (1f - (float) openAnim.getValue()) * 5f;
                Render2D.drawFont(ctx.getMatrices(), Fonts.BOLD.getFont(7.5f),
                        I18n.translate(bs.getName()),
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
            if (btn == 1) { open = !open; return; }
            if (btn == 0) {
                if (setting.isSingleSelect()) {
                    // Следующий вариант
                    var opts = setting.getValue();
                    int cur = -1;
                    for (int i = 0; i < opts.size(); i++) if (opts.get(i).getValue()) { cur = i; break; }
                    int next = (cur + 1) % Math.max(1, opts.size());
                    for (BooleanSetting b : opts) b.setValue(false);
                    opts.get(next).setValue(true);
                } else {
                    open = !open;
                }
                return;
            }
        }
        if (openAnim.getValue() > 0 && btn == 0) {
            float yOff   = height;
            float visH   = (float) (setting.getValue().size() * 18f
                    * Math.max(0f, Math.min(1f, openAnim.getValue())));
            for (BooleanSetting bs : setting.getValue()) {
                if (yOff >= height + visH) break;
                if (MathUtils.isHovered(x, y + yOff, width, 18f, (float) mx, (float) my)) {
                    if (setting.isSingleSelect()) {
                        for (BooleanSetting all : setting.getValue()) all.setValue(false);
                        bs.setValue(true);
                    } else {
                        bs.setValue(!bs.getValue());
                    }
                    break;
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