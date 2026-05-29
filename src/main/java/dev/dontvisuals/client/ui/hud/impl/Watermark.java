package dev.dontvisuals.client.ui.hud.impl;

import dev.dontvisuals.client.events.impl.EventRender2D;
import dev.dontvisuals.client.ui.hud.HudElement;
import dev.dontvisuals.client.managers.ThemeManager;
import dev.dontvisuals.client.util.renderer.Render2D;
import dev.dontvisuals.client.util.renderer.fonts.Fonts;
import dev.dontvisuals.client.util.renderer.fonts.Font;
import dev.dontvisuals.client.util.perf.Perf;
import net.minecraft.util.Identifier;

import java.awt.Color;

public class Watermark extends HudElement implements ThemeManager.ThemeChangeListener {

    private static final Identifier GOTHIC_D = Identifier.of("dontvisuals", "textures/logo/gothic_d.png");

    private final ThemeManager themeManager;

    public Watermark() {
        super("Watermark");
        this.themeManager = ThemeManager.getInstance();
        themeManager.addThemeChangeListener(this);
    }

    @Override
    public void onThemeChanged(ThemeManager.Theme theme) {
        // reserved for future theme handling
    }

    @Override
    public void onRender2D(EventRender2D e) {
        if (fullNullCheck() || closed()) return;
        Perf.tryBeginFrame();
        Perf.scopeCpu("Watermark.onRender2D");

        var matrices = e.getContext().getMatrices();
        Font fontBold    = Fonts.BOLD;
        Font fontRegular = Fonts.REGULAR;

        String title = "DontVisuals";
        String link  = "t.me/dontvisuals";

        float fontSizeTitle = 9f;
        float fontSizeLink  = 7f;

        float paddingX  = 8f;
        float paddingY  = 7f;
        float logoSize  = 26.5f;

        float titleWidth = fontBold.getWidth(title, fontSizeTitle);
        float linkWidth  = fontRegular.getWidth(link, fontSizeLink);
        float textWidth  = Math.max(titleWidth, linkWidth);

        float totalWidth  = paddingX * 2 + logoSize + 6 + textWidth;
        float totalHeight = paddingY * 2 + fontSizeTitle + fontSizeLink + 2f;

        setBounds(getX(), getY(), totalWidth, totalHeight);

        Color bgColor    = new Color(30, 30, 30, 240);
        Color liveAccent = themeManager.getCurrentTheme().getAccentColor();

        Render2D.drawRoundedRect(matrices, getX(), getY(), totalWidth, totalHeight, 5f, bgColor);

        float logoCenteredY  = getY() + (totalHeight - logoSize) / 2f;
        float logoX          = getX() + paddingX;
        float letterOffsetX  = 0.8f; // <-- двигай только букву вправо

        Render2D.drawGlowOutline(
                matrices,
                logoX + 0.6f,
                logoCenteredY,
                logoSize,
                logoSize,
                logoSize / 0.5f,
                liveAccent,
                150,
                15
        );

        Render2D.drawTexture(
                matrices,
                logoX + letterOffsetX,
                logoCenteredY,
                logoSize,
                logoSize,
                0f,
                GOTHIC_D,
                liveAccent
        );

        float textX = getX() + paddingX + logoSize + 6;
        float textY = getY() + paddingY;

        Render2D.drawFont(matrices, fontBold.getFont(fontSizeTitle), title, textX, textY, Color.WHITE);
        Render2D.drawFont(matrices, fontRegular.getFont(fontSizeLink), link,
                textX, textY + fontSizeTitle + 2f, new Color(200, 200, 200));

        super.onRender2D(e);
    }
}