package dev.simplevisuals.client.ui.mainmenu;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.simplevisuals.client.render.builders.impl.BlurBuilder;
import dev.simplevisuals.client.render.builders.impl.RectangleBuilder;
import dev.simplevisuals.client.render.builders.impl.TextBuilder;
import dev.simplevisuals.client.render.builders.states.QuadColorState;
import dev.simplevisuals.client.render.builders.states.QuadRadiusState;
import dev.simplevisuals.client.render.builders.states.SizeState;
import dev.simplevisuals.client.util.animations.Animation;
import dev.simplevisuals.client.util.animations.Easing;
import dev.simplevisuals.client.util.renderer.fonts.Fonts;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.text.Text;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

import org.joml.Matrix4f;

import java.awt.Color;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MainMenu extends Screen {

    // ===== Static session tracking =====
    private static final Animation sessionTimer = new Animation(0, 0, true, Easing.LINEAR);
    private static boolean isFirstInstance = true;
    private static boolean welcomePlayed = false;

    // ===== UI state =====
    private final List<AnimatedButton> buttons = new ArrayList<>();
    private Animation alphaAnimation;
    private boolean isExiting = false;
    private boolean isNewEntry = true;
    private int lastWidth = 0;
    private int lastHeight = 0;

    public MainMenu() {
        super(Text.literal(I18n.translate("simplevisuals.mainmenu.screen_title")));
        alphaAnimation = new Animation(500, 1.0, true, Easing.OUT_EXPO);

        if (isFirstInstance) {
            isFirstInstance = false;
        }
    }

    @Override
    protected void init() {
        super.init();

        lastWidth = this.width;
        lastHeight = this.height;
        buttons.clear();

        float buttonWidth = 125f;
        float buttonHeight = 22f;
        float margin = 3f;

        float centerX = this.width / 2f - buttonWidth / 2f;
        float centerY = this.height / 2f;

        // Add buttons
        buttons.add(new AnimatedButton(centerX, centerY - buttonHeight * 2f - margin * 2, buttonWidth, buttonHeight, I18n.translate("simplevisuals.mainmenu.singleplayer"),
                btn -> startExitAnimation(new AnimatedScreenWrapper(new SelectWorldScreen(this), this))));

        buttons.add(new AnimatedButton(centerX, centerY - buttonHeight - margin, buttonWidth, buttonHeight, I18n.translate("simplevisuals.mainmenu.multiplayer"),
                btn -> startExitAnimation(new AnimatedScreenWrapper(new MultiplayerScreen(this), this))));

        buttons.add(new AnimatedButton(centerX, centerY, buttonWidth, buttonHeight, I18n.translate("simplevisuals.mainmenu.options"),
                btn -> startExitAnimation(new AnimatedScreenWrapper(new OptionsScreen(this, this.client.options), this))));

        // Split Quit into Alt Manager and Quit
        float halfWidth = (buttonWidth - margin) / 2f;
        buttons.add(new AnimatedButton(centerX, centerY + buttonHeight + margin, halfWidth, buttonHeight, I18n.translate("simplevisuals.mainmenu.altmanager"),
                btn -> startExitAnimation(new AnimatedScreenWrapper(new AltManagerScreen(this), this))));
        buttons.add(new AnimatedButton(centerX + halfWidth + margin, centerY + buttonHeight + margin, halfWidth, buttonHeight, I18n.translate("simplevisuals.mainmenu.quit"),
                btn -> startExitAnimation(null)));

        if (isNewEntry) {
            isExiting = false;
            isNewEntry = false;
            alphaAnimation = new Animation(500, 1.0, true, Easing.OUT_EXPO);
            buttons.forEach(AnimatedButton::resetAnimations);
        }

        if (!welcomePlayed) {
            welcomePlayed = true;
            MinecraftClient.getInstance().getSoundManager().play(
                    PositionedSoundInstance.master(
                            SoundEvent.of(Identifier.of("simplevisuals:welcome")),
                            1.0f,
                            1.0f
                    )
            );
        }
    }

    private void startExitAnimation(Screen nextScreen) {
        if (isExiting) return;

        isExiting = true;
        isNewEntry = true;
        alphaAnimation.update(false);

        if (nextScreen == null) {
            sessionTimer.reset();
        }

        new Thread(() -> {
            try {
                Thread.sleep(10); // match fade out duration
                client.execute(() -> {
                    if (nextScreen == null) client.scheduleStop();
                    else client.setScreen(nextScreen);
                });
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (AnimatedButton btn : buttons) {
            if (btn.isMouseOver((int) mouseX, (int) mouseY)) {
                btn.onPress();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }


    @Override
    public void render(DrawContext drawContext, int mouseX, int mouseY, float delta) {
        if (this.width != lastWidth || this.height != lastHeight) {
            this.init();
        }

        float alpha = alphaAnimation.getValue();
        Matrix4f matrix = drawContext.getMatrices().peek().getPositionMatrix();

        // Background gradient (darker at top, lighter at bottom)
        new RectangleBuilder()
                .size(new SizeState(this.width, this.height))
                .radius(new QuadRadiusState(0))
                .color(new QuadColorState(
                        new Color(10, 10, 15, (int) (alpha * 255)), // Top-left (dark)
                        new Color(10, 10, 15, (int) (alpha * 255)), // Top-right (dark)
                        new Color(40, 40, 50, (int) (alpha * 255)), // Bottom-right (lighter)
                        new Color(40, 40, 50, (int) (alpha * 255))  // Bottom-left (lighter)
                ))
                .build()
                .render(matrix, 0f, 0f, 0f);

        // Размытие и фон
        new BlurBuilder()
                .size(new SizeState(this.width, this.height))
                .radius(new QuadRadiusState(0))
                .color(new QuadColorState(new Color(15, 15, 20, (int) (alpha * 10))))
                .blurRadius(0f)
                .smoothness(30f)
                .build()
                .render(matrix, 0f, 0f, 0f);

        // Заголовок
        String title = I18n.translate("simplevisuals.mainmenu.title");
        float titleX = this.width / 2f - Fonts.BOLD.getWidth(title, 10f) / 2f;
        float titleY = this.height / 2f - 75;

        new TextBuilder()
                .font(Fonts.BOLD.font())
                .text(title)
                .size(10f)
                .color(new Color(200, 200, 230, (int) (alpha * 255)))
                .smoothness(0.5f)
                .build()
                .render(matrix, titleX, titleY, 0f);

        // Копирайт
        String copyright = I18n.translate("simplevisuals.mainmenu.copyright");
        float copyrightX = this.width - Fonts.REGULAR.getWidth(copyright, 6f) - 5;

        new TextBuilder()
                .font(Fonts.REGULAR.font())
                .text(copyright)
                .size(6f)
                .color(new Color(255, 255, 255, (int) (alpha * 255)))
                .smoothness(0.5f)
                .build()
                .render(matrix, copyrightX, this.height - 10, 0f);

        // Кнопки
        for (AnimatedButton btn : buttons) {
            btn.render(drawContext, mouseX, mouseY, delta, alpha);
        }
    }

    // ===== Internal button class =====
    public static class AnimatedButton {
        private final float x, y, width, height;
        private final String message;
        private final PressAction action;
        private final Animation hoverAnimation;

        public AnimatedButton(float x, float y, float width, float height, String message, PressAction action) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.message = message;
            this.action = action;
            this.hoverAnimation = new Animation(200, 1.0, false, Easing.OUT_EXPO);
        }

        public void resetAnimations() {
            hoverAnimation.reset();
        }

        public void render(DrawContext drawContext, int mouseX, int mouseY, float delta, float alpha) {
            boolean hovered = isMouseOver(mouseX, mouseY);
            hoverAnimation.update(hovered);

            float hoverProgress = hoverAnimation.getValue();
            Matrix4f matrix = drawContext.getMatrices().peek().getPositionMatrix();

            // Кнопка
            new RectangleBuilder()
                    .size(new SizeState(width, height))
                    .radius(new QuadRadiusState(8))
                    .color(new QuadColorState(new Color(50, 50, 60, (int) (alpha * 150 + hoverProgress * 100))))
                    .build()
                    .render(matrix, x, y, 0f);

            // Текст
            Color base = new Color(220, 220, 240);
            Color hover = new Color(255, 255, 255);
            int r = (int) (base.getRed() + (hover.getRed() - base.getRed()) * hoverProgress);
            int g = (int) (base.getGreen() + (hover.getGreen() - base.getGreen()) * hoverProgress);
            int b = (int) (base.getBlue() + (hover.getBlue() - base.getBlue()) * hoverProgress);

            Color textColor = new Color(r, g, b, (int) (alpha * 255));
            float textX = x + width / 2f - Fonts.REGULAR.getWidth(message, 7f) / 2f;
            float textY = y + height / 2f - Fonts.REGULAR.getHeight(7f) / 2f;

            new TextBuilder()
                    .font(Fonts.REGULAR.font())
                    .text(message)
                    .size(7f)
                    .color(textColor)
                    .smoothness(0.5f)
                    .build()
                    .render(matrix, textX, textY, 0f);
        }

        public boolean isMouseOver(int mouseX, int mouseY) {
            return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
        }

        public void onPress() {
            action.onPress(this);
        }
    }

    @FunctionalInterface
    public interface PressAction {
        void onPress(AnimatedButton button);
    }
}