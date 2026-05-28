package dev.dontvisuals.client.ui.hud.impl;

import dev.dontvisuals.client.events.impl.EventRender2D;
import dev.dontvisuals.client.ui.hud.HudElement;
import dev.dontvisuals.client.managers.ThemeManager;
import dev.dontvisuals.client.util.renderer.Render2D;
import dev.dontvisuals.client.util.renderer.fonts.Fonts;
import dev.dontvisuals.client.util.renderer.fonts.Font;
import dev.dontvisuals.client.util.perf.Perf;

import java.awt.Color;

public class Watermark extends HudElement implements ThemeManager.ThemeChangeListener {

    private final ThemeManager themeManager;
    private Color bgColor;
    private Color textColor;
    private Color accentColor;

    private float totalWidth, totalHeight;

    public Watermark() {
        super("Watermark");
        this.themeManager = ThemeManager.getInstance();
        applyTheme(themeManager.getCurrentTheme());
        themeManager.addThemeChangeListener(this);
    }

    private void applyTheme(ThemeManager.Theme theme) {
        this.bgColor = new Color(30, 30, 30, 240);
        int brightness = (int) (0.299 * bgColor.getRed() + 0.587 * bgColor.getGreen() + 0.114 * bgColor.getBlue());
        if (brightness > 200 && bgColor.getAlpha() <= 150) {
            this.textColor = new Color(0, 0, 0, 255);
        } else {
            this.textColor = theme.getTextColor();
        }
        this.accentColor = theme.getAccentColor();
    }

    @Override
    public void onThemeChanged(ThemeManager.Theme theme) {
        applyTheme(theme);
    }

    @Override
    public void onRender2D(EventRender2D e) {
        if (fullNullCheck() || closed()) return;
        Perf.tryBeginFrame();
        try (var __ = Perf.scopeCpu("Watermark.onRender2D")) {

            var matrices = e.getContext().getMatrices();
            Font fontBold    = Fonts.BOLD;
            Font fontRegular = Fonts.REGULAR;
            Font fontIcons   = Fonts.SEMIBOLD;

            String title = "DontVisuals";
            String link  = "t.me/dontvisuals";

            float fontSizeTitle = 9f;
            float fontSizeLink  = 7f;

            float paddingX = 8f;
            float paddingY = 7f;
            float logoSize = 17.5f;

            float titleWidth = fontBold.getWidth(title, fontSizeTitle);
            float linkWidth  = fontRegular.getWidth(link, fontSizeLink);
            float textWidth  = Math.max(titleWidth, linkWidth);

            totalWidth  = paddingX * 2 + logoSize + 6 + textWidth;
            totalHeight = paddingY * 2 + fontSizeTitle + fontSizeLink + 2f;

            setBounds(getX(), getY(), totalWidth, totalHeight);

            // Фон
            Render2D.drawRoundedRect(matrices, getX(), getY(), totalWidth, totalHeight, 5f, bgColor);

            Color liveAccent = themeManager.getCurrentTheme().getAccentColor();

            // Реальная высота глифа "D" для точного центрирования
            float logoGlyphH = fontIcons.getHeight(logoSize);
            float logoCenteredY = getY() + (totalHeight - logoGlyphH) / 2f;

            // Глоу — остаётся как есть
             Render2D.drawGlowOutline(
                     matrices,
                     getX() + paddingX + 0.6f,
                     logoCenteredY,
                     logoSize,
                     logoGlyphH,
                     logoSize / 0.5f,
                     liveAccent,
                     150,
                     15
             );

            // Буква D — отдельная коррекция по Y
            float logoTextY = getY() + (totalHeight - fontSizeTitle) / 2f - 1f; // подбери -1f / -2f под свой шрифт

            Render2D.drawFont(
                    matrices,
                    fontIcons.getFont(logoSize),
                    "D",
                    getX() + paddingX + 2.6f,   // <-- двигай вправо, увеличивай значение
                    logoTextY - 4.6f,            // <-- двигай вверх, увеличивай значение
                    liveAccent
            );

            float textX = getX() + paddingX + logoSize + 6;
            float textY = getY() + paddingY;

            // Заголовок
            Render2D.drawFont(matrices, fontBold.getFont(fontSizeTitle), title, textX, textY, Color.WHITE);

            // Ссылка
            Render2D.drawFont(matrices, fontRegular.getFont(fontSizeLink), link,
                    textX, textY + fontSizeTitle + 2f, new Color(200, 200, 200));

            super.onRender2D(e);
        }
    }
}