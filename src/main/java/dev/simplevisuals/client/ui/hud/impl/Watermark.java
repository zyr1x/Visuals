package dev.simplevisuals.client.ui.hud.impl;

import dev.simplevisuals.client.events.impl.EventRender2D;
import dev.simplevisuals.client.ui.hud.HudElement;
import dev.simplevisuals.client.managers.ThemeManager;
import dev.simplevisuals.client.util.renderer.Render2D;
import dev.simplevisuals.client.util.renderer.fonts.Fonts;
import dev.simplevisuals.client.util.renderer.fonts.Font;
import dev.simplevisuals.client.util.perf.Perf;

import java.awt.Color;

public class Watermark extends HudElement implements ThemeManager.ThemeChangeListener {

    private final ThemeManager themeManager;
    private Color bgColor;
    private Color textColor;
    private Color accentColor; // цвет для логотипа и глова

    private float totalWidth, totalHeight;

    public Watermark() {
        super("Watermark");
        this.themeManager = ThemeManager.getInstance();
        applyTheme(themeManager.getCurrentTheme());
        themeManager.addThemeChangeListener(this);
    }

    private void applyTheme(ThemeManager.Theme theme) {
        // сохраняем прежний фиксированный фон (как было в оригинальном коде)
        this.bgColor = new Color(30, 30, 30, 240);

        // оставляем логику выбора текста как была (можно не использовать, но пусть будет)
        int brightness = (int) (0.299 * bgColor.getRed() + 0.587 * bgColor.getGreen() + 0.114 * bgColor.getBlue());
        if (brightness > 200 && bgColor.getAlpha() <= 150) {
            this.textColor = new Color(0, 0, 0, 255);
        } else {
            this.textColor = theme.getTextColor();
        }

        // только акцент цвета используем для логотипа и глова
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
        Font fontBold = Fonts.BOLD;
        Font fontRegular = Fonts.REGULAR;
        Font fontIcons = Fonts.ICONS;

        String title = "SimpleVisuals";
        String link = "t.me/SimpleVisuals";

        // размеры шрифтов (как у тебя)
        float fontSizeTitle = 9f;
        float fontSizeLink = 7f;

        // отступы и размер логотипа
        float paddingX = 8f;
        float paddingY = 5f;
        float logoSize = 17.5f;

        float titleWidth = fontBold.getWidth(title, fontSizeTitle);
        float linkWidth = fontRegular.getWidth(link, fontSizeLink);
        float textWidth = Math.max(titleWidth, linkWidth);

        totalWidth = paddingX * 2 + logoSize + 6 + textWidth;
        totalHeight = paddingY * 2 + fontSizeTitle + fontSizeLink + 2f;

        // обновляем границы перед рисованием
        setBounds(getX(), getY(), totalWidth, totalHeight);

        // фон — оставляем фиксированным
        Render2D.drawRoundedRect(
                matrices,
                getX(), getY(),
                totalWidth, totalHeight,
                5f,
                bgColor
        );

        // глоу — динамический в цвете темы (акцент может быть градиентным, берём актуальный на кадр)
        Color liveAccent = themeManager.getCurrentTheme().getAccentColor();
        Render2D.drawGlowOutline(
                matrices,
                getX() + paddingX + 0.6f,
                getY() + (totalHeight - logoSize) / 2f,
                logoSize,
                logoSize,
                logoSize / 0.5f,
                liveAccent,
                150,
                15
        );

        // логотип (иконка) — тоже в актуальном цвете темы
        Render2D.drawFont(
                matrices,
                fontIcons.getFont(logoSize),
                "R",
                getX() + paddingX,
                getY() + (totalHeight - logoSize) / 2f,
                liveAccent
        );

        float textX = getX() + paddingX + logoSize + 6;
        float textY = getY() + paddingY;

        // заголовок — оставлен белым как в оригинале
        Render2D.drawFont(
                matrices,
                fontBold.getFont(fontSizeTitle),
                title,
                textX,
                textY,
                Color.WHITE
        );

        // линк — оставлен серым как в оригинале
        Render2D.drawFont(
                matrices,
                fontRegular.getFont(fontSizeLink),
                link,
                textX,
                textY + fontSizeTitle + 2f,
                new Color(200, 200, 200)
        );

        super.onRender2D(e);
        }
    }
}
