package dev.simplevisuals.client.ui.clickgui.components.impl;

import dev.simplevisuals.modules.settings.impl.StringSetting;
import dev.simplevisuals.client.ui.clickgui.components.Component;
import dev.simplevisuals.client.util.animations.Animation;
import dev.simplevisuals.client.util.animations.Easing;
import dev.simplevisuals.client.util.math.MathUtils;
import dev.simplevisuals.client.util.ColorUtils;
import dev.simplevisuals.client.util.renderer.fonts.Fonts;
import dev.simplevisuals.client.util.renderer.Render2D;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.resource.language.I18n;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import dev.simplevisuals.client.managers.ThemeManager;

public class StringComponent extends Component {

    private final StringSetting setting;
    private boolean typing;
    private final Animation focusAnim = new Animation(200, 1f, false, Easing.BOTH_SINE);
    private float scrollOffset = 0f;

    public StringComponent(StringSetting setting) {
        super(setting.getName());
        this.setting = setting;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        float ga = Math.max(0f, Math.min(1f, getGlobalAlpha()));
        focusAnim.update(typing);
        float fa = (float) focusAnim.getValue();

        Color accent    = ThemeManager.getInstance().getCurrentTheme().getAccentColor();
        Color themeText = ThemeManager.getInstance().getCurrentTheme().getTextColor();

        // Текст значения
        String val     = I18n.translate(setting.getValue());
        float  textW   = Fonts.REGULAR.getWidth(val, 8f);
        float  maxW    = width - 12f;
        if (textW > maxW) scrollOffset = textW - maxW;
        else scrollOffset = 0f;

        // ── Фон ──────────────────────────────────────────────────────────────
        // Акцентная рамка при фокусе
        if (fa > 0.01f) {
            Render2D.drawRoundedRect(ctx.getMatrices(), x - 1f, y - 1f, width + 2f, height + 2f, 6f,
                    new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), (int) (80 * fa * ga)));
        }
        Render2D.drawRoundedRect(ctx.getMatrices(), x, y, width, height, 5f,
                new Color(25, 25, 35, (int) (200 * ga)));

        Render2D.startScissor(ctx, x + 4, y, width - 8, height);

        float textX = x + 6f;
        float textY = y + (height - Fonts.REGULAR.getHeight(8f)) / 2f;

        // Placeholder / label
        if (!typing && setting.getValue().isEmpty()) {
            Render2D.drawFont(ctx.getMatrices(), Fonts.REGULAR.getFont(8f),
                    I18n.translate(setting.getName()),
                    textX, textY, new Color(100, 100, 120, (int) (180 * ga)));
        }

        // Значение
        if (!setting.getValue().isEmpty()) {
            Render2D.drawFont(ctx.getMatrices(), Fonts.REGULAR.getFont(8f),
                    val, textX - scrollOffset, textY,
                    new Color(themeText.getRed(), themeText.getGreen(), themeText.getBlue(),
                            (int) (themeText.getAlpha() * ga)));
        }

        // Курсор
        if (typing) {
            float curX = textX - scrollOffset + textW;
            Render2D.drawRect(ctx.getMatrices(), curX + 1f, textY, 1f, Fonts.REGULAR.getHeight(8f),
                    new Color(accent.getRed(), accent.getGreen(), accent.getBlue(),
                            (int) (200 * ga)));
        }

        Render2D.stopScissor(ctx);
    }

    @Override
    public void mouseClicked(double mx, double my, int btn) {
        if (MathUtils.isHovered(x, y, width, height, (float) mx, (float) my) && btn == 0)
            typing = !typing;
        else
            typing = false;
    }

    @Override
    public void mouseReleased(double mx, double my, int btn) {}

    @Override
    public void keyPressed(int key, int scan, int mods) {
        switch (key) {
            case GLFW.GLFW_KEY_BACKSPACE -> {
                if (typing && setting.getValue() != null && !setting.getValue().isEmpty())
                    setting.setValue(setting.getValue().substring(0, setting.getValue().length() - 1));
            }
            case GLFW.GLFW_KEY_ESCAPE, GLFW.GLFW_KEY_ENTER -> typing = false;
            case GLFW.GLFW_KEY_DELETE -> { if (typing) setting.setValue(""); }
            case GLFW.GLFW_KEY_V -> {
                if (Screen.hasControlDown() && typing) {
                    String clip = GLFW.glfwGetClipboardString(mc.getWindow().getHandle());
                    if (clip != null) setting.setValue(setting.getValue() + clip);
                }
            }
        }
    }

    @Override public void keyReleased(int k, int s, int m) {}

    @Override
    public void charTyped(char chr, int mods) {
        if (!typing) return;
        if (setting.isOnlyDigit() && !Character.isDigit(chr)) return;
        setting.setValue(setting.getValue() + chr);
    }
}