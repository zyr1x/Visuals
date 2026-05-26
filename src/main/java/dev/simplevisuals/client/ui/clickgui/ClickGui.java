package dev.simplevisuals.client.ui.clickgui;

import dev.simplevisuals.client.ui.clickgui.components.impl.ModuleComponent;
import dev.simplevisuals.client.util.Wrapper;
import dev.simplevisuals.client.util.animations.Animation;
import dev.simplevisuals.client.util.animations.Easing;
import dev.simplevisuals.client.util.renderer.Render2D;
import dev.simplevisuals.client.util.renderer.fonts.Fonts;
import dev.simplevisuals.client.managers.ThemeManager; // Import ThemeManager
import dev.simplevisuals.modules.api.Category;
import dev.simplevisuals.modules.api.Module;
import dev.simplevisuals.modules.impl.render.UI;
import dev.simplevisuals.modules.impl.utility.ClientSound;
import dev.simplevisuals.simplevisuals;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.client.MinecraftClient;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.client.sound.PositionedSoundInstance;
import org.lwjgl.glfw.GLFW;
import net.minecraft.client.resource.language.I18n;

import java.awt.*;
import java.util.*;
import java.util.List;

public class ClickGui extends Screen implements Wrapper {

    private final Animation yAnimation = new Animation(360, 1f, true, Easing.OUT_QUART);
    private final ThemeManager themeManager; // Add ThemeManager

    private String description = "";
    private boolean closing = false;
    private float uiAlpha = 0f;
    private float contentOffsetY = 0f;

    private static final Category[] TABS = {
            Category.Render,
            Category.Utility,
            Category.Theme // Add Theme category
    };

    private Category selectedCategory = Category.Render;

    private float x, y, width, height;

    private final Map<Category, List<ModuleComponent>> componentsByCategory = new EnumMap<>(Category.class);

    private float scrollY = 0f;
    private float maxScroll = 0f;
    private float tabScrollY = 0f;
    private float maxTabScroll = 0f;
    private float scrollYTarget = 0f;

    private static final int COLS = 2;
    private static final float GAP = 8f;

    private ModuleComponent activeSettings = null;
    private float settingsScrollY = 0f;
    private float settingsMaxScroll = 0f;
    private float settingsScrollYTarget = 0f;
    private final Animation settingsAnimation = new Animation(280, 1f, true, Easing.OUT_QUART);

    public ClickGui() {
        super(Text.of("simplevisuals-clickgui"));
        this.themeManager = ThemeManager.getInstance(); // Initialize ThemeManager
    }

    @Override
    public void init() {
        super.init();
        this.width = 320f;
        this.height = 300f;
        this.x = (mc.getWindow().getScaledWidth() - this.width) / 2f;
        this.y = (mc.getWindow().getScaledHeight() - this.height) / 2f; // фиксированная позиция по центру

        buildComponentsCache();
        scrollY = 0f;
        scrollYTarget = 0f;
        tabScrollY = 0f;

        closing = false;
        yAnimation.update(true); // Animation for opening (fade)
    }

    private void buildComponentsCache() {
        componentsByCategory.clear();
        for (Category cat : TABS) {
            if (cat != Category.Theme) { // Skip Theme category for module components
                List<Module> mods = simplevisuals.getInstance().getModuleManager().getModules(cat);
                List<ModuleComponent> comps = new ArrayList<>(mods.size());
                for (Module m : mods) comps.add(new ModuleComponent(m));
                componentsByCategory.put(cat, comps);
            }
        }
    }

    // Method to play sound on module toggle or theme change
    private void playToggleSound(boolean wasToggled) {
        ClientSound clientSound = simplevisuals.getInstance().getModuleManager().getModule(ClientSound.class);
        if (clientSound != null && clientSound.isToggled()) {
            String soundId = wasToggled ? clientSound.getDisableSoundId() : clientSound.getEnableSoundId();
            float volume = clientSound.getVolume().getValue();
            MinecraftClient.getInstance().getSoundManager().play(
                    PositionedSoundInstance.master(
                            SoundEvent.of(Identifier.of(soundId)),
                            1.0f,
                            volume
                    )
            );
        }
    }

    @Override
    public void close() {
        if (!closing) {
            closing = true;
            yAnimation.update(false); // Animation for closing (fade out)
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        float targetY = (mc.getWindow().getScaledHeight() - this.height) / 2f;

        if (closing) {
            yAnimation.update(false);
            // Плавное смещение к центру при закрытии (чуть ниже → к центру)
            float offset = (1f - yAnimation.getValue()) * 12f;
            this.y = targetY + offset;
            if (yAnimation.getValue() <= 0.01f) {
                simplevisuals.getInstance().getModuleManager().getModule(UI.class).setToggled(false);
                super.close();
                return;
            }
        } else {
            yAnimation.update(true);
            // Появление: старт чуть ниже и плавно встаём по центру
            float offset = (1f - yAnimation.getValue()) * 12f;
            this.y = targetY + offset;
        }

        this.x = (mc.getWindow().getScaledWidth() - this.width) / 2f;

        // Используем fade по альфе + небольшой вертикальный оффсет для контента
        uiAlpha = Math.max(0f, Math.min(1f, yAnimation.getValue()));
        contentOffsetY = (1f - uiAlpha) * 8f;

        // Затемнение заднего фона под GUI
        int backdropAlpha = (int) (140 * uiAlpha);
        if (backdropAlpha > 0) {
            Render2D.drawRect(context.getMatrices(), 0f, 0f,
                    mc.getWindow().getScaledWidth(), mc.getWindow().getScaledHeight(),
                    new Color(0, 0, 0, backdropAlpha));
        }

        // Красивое многоступенчатое свечение вокруг панели GUI
        renderPanelGlow(context);

        int alpha = (int) (255 * uiAlpha);
        Render2D.drawRoundedRect(context.getMatrices(), x, y, width, height, 8f,
                new Color(30, 30, 30, alpha));

        renderCategories(context);
        renderTopDescription(context);
        renderModulesArea(context, mouseX, mouseY, delta);
        renderBottomHints(context);
    }

    private void renderBottomHints(DrawContext ctx) {
        if (uiAlpha <= 0f) return;

        String hint1 = I18n.translate("simplevisuals.clickgui.hint.bind");
        String hint2 = I18n.translate("simplevisuals.clickgui.hint.settings");
        String hint3 = I18n.translate("simplevisuals.clickgui.hint.hud_move");

        float gap = 2f;
        float fontSize = 8f;
        float lineHeight = Fonts.MEDIUM.getHeight(fontSize) + gap;

        float startY = y + height + 8f; // ниже ClickGUI
        int textA = (int) (230 * uiAlpha);
        Color textColor = new Color(255, 255, 255, textA);

        float screenCenterX = mc.getWindow().getScaledWidth() / 2f;

        // line 1
        float w1 = Fonts.MEDIUM.getWidth(hint1, fontSize);
        float x1 = screenCenterX - w1 / 2f;
        Render2D.drawFont(ctx.getMatrices(), Fonts.MEDIUM.getFont(fontSize), hint1, x1, startY, textColor);

        // line 2
        float w2 = Fonts.MEDIUM.getWidth(hint2, fontSize);
        float x2 = screenCenterX - w2 / 2f;
        Render2D.drawFont(ctx.getMatrices(), Fonts.MEDIUM.getFont(fontSize), hint2, x2, startY + lineHeight, textColor);

        // line 3
        float w3 = Fonts.MEDIUM.getWidth(hint3, fontSize);
        float x3 = screenCenterX - w3 / 2f;
        Render2D.drawFont(ctx.getMatrices(), Fonts.MEDIUM.getFont(fontSize), hint3, x3, startY + lineHeight * 2f, textColor);
    }

    private void renderPanelGlow(DrawContext ctx) {
        if (uiAlpha <= 0f) return;

        float gx = x;
        float gy = y;
        float gw = width;
        float gh = height;

        // Шейдерный blur вокруг панели (аккуратный мягкий ореол)
        float radius = 8f;
        float blurRadius = 10f;
        int a = (int) (22 * uiAlpha);
        if (a > 0) {
            // Немного расширим область, чтобы свечение выглядело как тень
            Render2D.drawShaderBlurRect(ctx.getMatrices(), gx - 2f, gy - 2f, gw + 4f, gh + 4f, radius + 1f, blurRadius,
                    new Color(255, 255, 255, a));
        }
    }

    private void renderCategories(DrawContext ctx) {
        float startY = y + 10f + contentOffsetY; // плавный вертикальный оффсет при открытии
        float tabW = 40f;
        float tabH = 15f;
        float gap = 6f;

        int tabsCount = TABS.length;
        float totalTabsWidth = tabsCount * tabW + (tabsCount - 1) * gap;
        float startX = x + (width - totalTabsWidth) / 2f;

        startScissorScaled(ctx, x + 8f, startY, width - 16f, tabH);

        float offsetX = 0f; // horizontal layout
        for (Category cat : TABS) {
            boolean active = cat == selectedCategory;
            int baseAlpha = (int) (255 * uiAlpha);
            int bgAlphaInactive = (int) (170 * uiAlpha);
            Color currentThemeTextColor = themeManager.getCurrentTheme().getTextColor();
            Color color = active
                    ? themeManager.getCurrentTheme() instanceof ThemeManager.LightTheme
                        ? new Color(0, 0, 0, baseAlpha)
                        : new Color(currentThemeTextColor.getRed(), currentThemeTextColor.getGreen(), currentThemeTextColor.getBlue(), baseAlpha)
                    : new Color(190, 190, 190, baseAlpha);

            float drawX = startX + offsetX;
            if (active) {
                // Активная вкладка - акцентный цвет темы с альфой
                Color acc = themeManager.getCurrentTheme().getAccentColor();
                Render2D.drawRoundedRect(ctx.getMatrices(), drawX, startY, tabW, tabH, 4f,
                        new Color(acc.getRed(), acc.getGreen(), acc.getBlue(), baseAlpha));
            } else {
                // Неактивные вкладки - фон с альфой
                Render2D.drawRoundedRect(ctx.getMatrices(), drawX, startY, tabW, tabH, 4f,
                        new Color(20, 20, 20, bgAlphaInactive));
            }

            String catName = cat.name();
            float textWidth = Fonts.MEDIUM.getWidth(catName, 9f);
            float textX = drawX + (tabW - textWidth) / 2f + 1.5f;
            float textY = startY + (tabH - Fonts.MEDIUM.getHeight(9f)) / 2f + 1f;

            Render2D.drawFont(ctx.getMatrices(), Fonts.MEDIUM.getFont(8f), catName,
                    textX, textY, color);

            offsetX += tabW + gap;
        }

        Render2D.stopScissor(ctx);
    }

    private void renderTopDescription(DrawContext ctx) {
        if (description == null || description.isEmpty()) return;

        String descText = I18n.translate(description);
        float textW = Fonts.MEDIUM.getWidth(descText, 9f);

        // Place just above the panel and make it overhang the panel width
        float bgOverhang = 12f; // how far to extend past the panel on each side
        float bgX = x - bgOverhang;
        float bgY = y - 14f; // slightly above the ClickGUI panel
        float bgW = width + bgOverhang * 2f;
        float bgH = 14f;

        // Center text relative to the ClickGUI panel
        float textX = x + (width - textW) / 2f;
        float textY = bgY - 5f;

        int panelAlpha = (int) (120 * uiAlpha);
        int textAlpha = (int) (255 * uiAlpha);

        Render2D.drawFont(ctx.getMatrices(), Fonts.MEDIUM.getFont(9f), descText, textX, textY,
                new Color(255, 255, 255, textAlpha));
        description = "";
    }

    private void startScissorScaled(DrawContext ctx, float rx, float ry, float rw, float rh) {
        Render2D.startScissor(ctx, rx, ry, rw, rh);
    }

    private void renderModulesArea(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // Left content (modules/themes)
        float tabH = 18f;
        float leftX = x + 8f;
        float leftY = y + 10f + tabH + 9f + contentOffsetY; // применяем оффсет
        float leftW = width - 16f;
        float leftH = height - ((leftY - y) + 8f);

        // Smooth scroll interpolation for main list
        float listSmooth = 0.18f;
        scrollY += (scrollYTarget - scrollY) * listSmooth;
        scrollY = clamp(scrollY, 0f, maxScroll);

        startScissorScaled(ctx, leftX, leftY, leftW, leftH);

        if (selectedCategory == Category.Theme) {
            float themeY = leftY + 2f - scrollY;
            float totalHeight = 0f;
            for (ThemeManager.Theme theme : themeManager.getAvailableThemes()) {
                Render2D.drawFont(ctx.getMatrices(), Fonts.MEDIUM.getFont(8f),
                        theme.getName(), leftX + 6f, themeY + 2f, themeManager.getCurrentTheme().getTextColor());
                Render2D.drawRect(ctx.getMatrices(), leftX + leftW - 20f, themeY + 2f,
                        10f, 10f, theme.getBackgroundColor());
                themeY += 20f;
                totalHeight += 20f;
            }
            maxScroll = Math.max(0f, totalHeight - leftH);
            scrollYTarget = clamp(scrollYTarget, 0f, maxScroll);
            scrollY = clamp(scrollY, 0f, maxScroll);
        } else {
            List<ModuleComponent> comps = componentsByCategory.getOrDefault(selectedCategory, Collections.emptyList());
            int cols = COLS;
            float gap = GAP;
            float colW = (leftW - gap * (cols - 1)) / cols;
            float[] baseY = new float[cols];
            Arrays.fill(baseY, leftY + 2f);
            float maxBottom = leftY;
            int placed = 0;
            for (ModuleComponent mcComp : comps) {
                int col = placed % cols;
                float cx = leftX + col * (colW + gap);
                float cyDraw = baseY[col] - scrollY;
                mcComp.setX(cx);
                mcComp.setY(cyDraw);
                mcComp.setWidth(colW);
                mcComp.setRenderExternally(true);
                mcComp.setGlobalAlpha(uiAlpha);
                // принудительно скрыть внутренние дети (не рисовать раскрытые настройки слева)
                if (mcComp.getOpenAnimation().getValue() > 0f && mcComp != activeSettings) {
                    // оставляем как есть — внутренний рендер отключён флагом renderExternally
                }
                mcComp.render(ctx, mouseX, mouseY, delta);
                float totalH = mcComp.getHeight();
                baseY[col] += totalH + gap;
                maxBottom = Math.max(maxBottom, baseY[col]);
                placed++;
            }
            float contentBottom = leftY + leftH;
            maxScroll = Math.max(0f, (maxBottom - gap) - contentBottom);
            scrollYTarget = clamp(scrollYTarget, 0f, maxScroll);
            scrollY = clamp(scrollY, 0f, maxScroll);

            // Мягкий оверлей для плавного исчезновения модулей при открытии/закрытии
            float tModules = 1f - uiAlpha;
            float easedModules = tModules * tModules * (3f - 2f * tModules); // smoothstep
            int modulesOverlayAlpha = (int) (140 * easedModules);
            if (modulesOverlayAlpha > 2) {
                Render2D.drawRoundedRect(ctx.getMatrices(), leftX + 1f, leftY + 1f, leftW - 2f, leftH - 2f, 3f,
                        new Color(30, 30, 30, modulesOverlayAlpha));
            }
        }

        Render2D.stopScissor(ctx);

        // Scrollbar for modules/themes list when content overflows (rendered to the right of ClickGUI)
        if (maxScroll > 0.5f) {
            float trackX = x + width - 4f;
            float trackY = leftY;
            float trackW = 2f;
            float trackH = leftH;
            Render2D.drawRect(ctx.getMatrices(), trackX, trackY, trackW, trackH,
                    new Color(0, 0, 0, Math.min(90, (int) (90f * uiAlpha))));

            float visibleRatio = leftH / Math.max(leftH + maxScroll, 1f);
            float thumbH = Math.max(14f, trackH * visibleRatio);
            float maxThumbTravel = trackH - thumbH;
            float scrollRatio = maxScroll <= 0f ? 0f : (scrollY / maxScroll);
            float thumbY = trackY + maxThumbTravel * scrollRatio;
            Color thumbColor = new Color(
                    themeManager.getCurrentTheme().getAccentColor().getRed(),
                    themeManager.getCurrentTheme().getAccentColor().getGreen(),
                    themeManager.getCurrentTheme().getAccentColor().getBlue(),
                    Math.min(180, (int) (180f * uiAlpha))
            );
            Render2D.drawRoundedRect(ctx.getMatrices(), trackX - 0.5f, thumbY, trackW + 1f, thumbH, 1.5f, thumbColor);
        }

        ModuleComponent target = activeSettings;
        boolean hasSettings = target != null && !target.getComponents().isEmpty();
        // settings panel animation
        settingsAnimation.update(hasSettings);
        float anim = settingsAnimation.getValue();
        if (anim > 0.01f) {
            // right settings panel (slides in from right with fade)
            float basePanelX = x + width + 5f;
            float panelY = y + 90f + contentOffsetY; // применяем оффсет
            float panelW = 120f;
            float panelH = (height - 200f);
            float slideOffset = (1f - anim) * 40f;
            float drawPanelX = basePanelX + slideOffset;

            int settingsAlpha = (int) (255 * Math.min(1f, anim * uiAlpha));
            float smoothPanel = (float) (Math.pow(Math.min(1f, anim * uiAlpha), 2) * (3 - 2 * Math.min(1f, anim * uiAlpha)));
            int panelA = (int) (255 * smoothPanel);

            // Убрали blur под панелью настроек: лёгкая подложка
            Render2D.drawRoundedRect(ctx.getMatrices(), drawPanelX - 1f, panelY - 1f, panelW + 2f, panelH + 2f, 3f,
                    new Color(255, 255, 255, Math.min(18, (int) (18f * anim * uiAlpha))));

            Render2D.drawRoundedRect(ctx.getMatrices(), drawPanelX, panelY, panelW, panelH, 3f,
                    new Color(30, 30, 30, panelA));

            float rContentX = drawPanelX + 8f;
            float rContentY = panelY + 8f;
            float rContentW = panelW - 16f;
            float rContentH = panelH - 16f;

            float total = 0f;
            if (hasSettings) {
                target.setGlobalAlpha(Math.min(1f, anim * uiAlpha));
                total = target.renderSettingsExternally(ctx, rContentX, rContentY, rContentW,
                        rContentX, rContentY, rContentW, rContentH, mouseX, mouseY, delta, settingsScrollY);
            }
            settingsMaxScroll = Math.max(0f, total - rContentH);
            scrollYTarget = clamp(scrollYTarget, 0f, maxScroll);
            scrollY = clamp(scrollY, 0f, maxScroll);
            settingsScrollYTarget = clamp(settingsScrollYTarget, 0f, settingsMaxScroll);
            // Smooth scroll interpolation for settings list
            float settingsSmooth = 0.2f;
            settingsScrollY += (settingsScrollYTarget - settingsScrollY) * settingsSmooth;
            settingsScrollY = clamp(settingsScrollY, 0f, settingsMaxScroll);

            // vertical scrollbar on the right of settings content
            if (settingsMaxScroll > 0.5f) {
                float trackX = drawPanelX + panelW - 3f;
                float trackY = rContentY;
                float trackW = 2f;
                float trackH = rContentH;
                Render2D.drawRect(ctx.getMatrices(), trackX, trackY, trackW, trackH,
                        new Color(0, 0, 0, Math.min(90, (int) (90f * anim * uiAlpha))));

                float visibleRatio = rContentH / Math.max(rContentH + settingsMaxScroll, 1f);
                float thumbH = Math.max(14f, trackH * visibleRatio);
                float maxThumbTravel = trackH - thumbH;
                float scrollRatio = settingsMaxScroll <= 0f ? 0f : (settingsScrollY / settingsMaxScroll);
                float thumbY = trackY + maxThumbTravel * scrollRatio;
                Color thumbColor = new Color(
                        themeManager.getCurrentTheme().getAccentColor().getRed(),
                        themeManager.getCurrentTheme().getAccentColor().getGreen(),
                        themeManager.getCurrentTheme().getAccentColor().getBlue(),
                        Math.min(180, (int) (180f * anim * uiAlpha))
                );
                Render2D.drawRoundedRect(ctx.getMatrices(), trackX - 0.5f, thumbY, trackW + 1f, thumbH, 1.5f, thumbColor);
            }

            // Мягкий скруглённый оверлей над содержимым (снижаем альфу и убираем жёсткие края) с плавной кривой
            float tSettings = 1f - Math.min(1f, anim * uiAlpha);
            float easedSettings = tSettings * tSettings * (3f - 2f * tSettings); // smoothstep
            int contentOverlayAlpha = (int) (160 * easedSettings);
            if (contentOverlayAlpha > 6) {
                Render2D.drawRoundedRect(ctx.getMatrices(), rContentX, rContentY, rContentW, rContentH, 2f,
                        new Color(30, 30, 30, contentOverlayAlpha));
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (closing) return false;

        float startY = y + 10f + contentOffsetY;
        float tabW = 40f;
        float tabH = 15f;
        float gap = 6f;
        int tabsCount = TABS.length;
        float totalTabsWidth = tabsCount * tabW + (tabsCount - 1) * gap;
        float startX = x + (width - totalTabsWidth) / 2f;

        // Handle category tab clicks (horizontal)
        float offsetX = 0f;
        for (Category cat : TABS) {
            float drawX = startX + offsetX;
            if (mouseX >= drawX && mouseX <= drawX + tabW &&
                    mouseY >= startY && mouseY <= startY + tabH) {
                selectedCategory = cat;
                scrollY = 0f;
                scrollYTarget = 0f;
                activeSettings = null;
                return true;
            }
            offsetX += tabW + gap;
        }

        // Left content area bounds
        float leftX = x + 8f;
        float leftY = y + 10f + tabH + 9f + contentOffsetY;
        float leftW = width - 16f;
        float leftH = height - ((leftY - y) + 8f);

        if (mouseX >= leftX && mouseX <= leftX + leftW && mouseY >= leftY && mouseY <= leftY + leftH) {
            if (selectedCategory == Category.Theme && button == 0) {
                float themeY = leftY + 2f - scrollY;
                for (ThemeManager.Theme theme : themeManager.getAvailableThemes()) {
                    if (mouseX >= leftX && mouseX <= leftX + leftW && mouseY >= themeY && mouseY <= themeY + 20f) {
                        ThemeManager.Theme previousTheme = themeManager.getCurrentTheme();
                        themeManager.setTheme(theme);
                        if (previousTheme != theme) { playToggleSound(true); }
                        return true;
                    }
                    themeY += 20f;
                }
            } else {
                List<ModuleComponent> comps = componentsByCategory.getOrDefault(selectedCategory, Collections.emptyList());
                // Если какое-либо меню бинда открыто — сделать модальным: обработать клик только этим компонентом
                for (ModuleComponent mcComp : comps) {
                    if (mcComp.isBindModeMenuOpen()) {
                        mcComp.mouseClicked(mouseX, mouseY, button);
                        return true;
                    }
                }
                for (ModuleComponent mcComp : comps) {
                    boolean wasToggled = mcComp.getModule().isToggled();
                    float headerH = 24f;
                    if (button == 1 && mouseX >= mcComp.getX() && mouseX <= mcComp.getX() + mcComp.getWidth() &&
                            mouseY >= mcComp.getY() && mouseY <= mcComp.getY() + headerH) {
                        activeSettings = mcComp.getComponents().isEmpty() ? null : mcComp;
                        settingsScrollY = 0f;
                        settingsScrollYTarget = 0f;
                    }
                    mcComp.mouseClicked(mouseX, mouseY, button);
                    if (button == 0 && wasToggled != mcComp.getModule().isToggled()) {
                        playToggleSound(wasToggled);
                    }
                }
            }
        }

        // Right panel interactions for settings (only if shown)
        if (activeSettings != null && !activeSettings.getComponents().isEmpty()) {
            float anim = settingsAnimation.getValue();
            if (anim > 0.2f) {
                float basePanelX = x + width + 5f;
                float panelY = y + 90f + contentOffsetY;
                float panelW = 120f;
                float panelH = (height - 200f);
                float slideOffset = (1f - anim) * 40f;
                float drawPanelX = basePanelX + slideOffset;
                float rContentX = drawPanelX + 8f;
                float rContentY = panelY + 8f;
                float rContentW = panelW - 16f;
                float rContentH = panelH - 16f;
                if (mouseX >= rContentX && mouseX <= rContentX + rContentW && mouseY >= rContentY && mouseY <= rContentY + rContentH) {
                    activeSettings.mouseClickedExternal(mouseX, mouseY, button);
                    return true;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (selectedCategory != Category.Theme) {
            List<ModuleComponent> comps = componentsByCategory.getOrDefault(selectedCategory, Collections.emptyList());
            for (ModuleComponent mcComp : comps) {
                if (mcComp.isBindModeMenuOpen()) {
                    mcComp.mouseReleased(mouseX, mouseY, button);
                    return true;
                }
            }
        }
        if (selectedCategory != Category.Theme) {
            List<ModuleComponent> comps = componentsByCategory.getOrDefault(selectedCategory, Collections.emptyList());
            for (ModuleComponent mcComp : comps) {
                mcComp.mouseReleased(mouseX, mouseY, button);
            }
        }
        if (activeSettings != null) {
            activeSettings.mouseReleasedExternal(mouseX, mouseY, button);
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        float tabH = 18f;
        float leftX = x + 8f;
        float leftY = y + 10f + tabH + 9f + contentOffsetY;
        float leftW = width - 16f;
        float leftH = height - ((leftY - y) + 8f);
        float step = (float) (-vertical * 10f);
        if (mouseX >= leftX && mouseX <= leftX + leftW && mouseY >= leftY && mouseY <= leftY + leftH) {
            scrollYTarget = clamp(scrollYTarget + step, 0f, maxScroll);
            return true;
        }

        if (activeSettings != null && !activeSettings.getComponents().isEmpty()) {
            float anim = settingsAnimation.getValue();
            if (anim > 0.2f) {
                float basePanelX = x + width + 5f;
                float panelY = y + 90f + contentOffsetY;
                float panelW = 120f;
                float panelH = (height - 200f);
                float slideOffset = (1f - anim) * 40f;
                float drawPanelX = basePanelX + slideOffset;
                float rContentX = drawPanelX + 8f;
                float rContentY = panelY + 8f;
                float rContentW = panelW - 16f;
                float rContentH = panelH - 16f;
                if (mouseX >= rContentX && mouseX <= rContentX + rContentW && mouseY >= rContentY && mouseY <= rContentY + rContentH) {
                    settingsScrollYTarget = clamp(settingsScrollYTarget + step, 0f, settingsMaxScroll);
                    return true;
                }
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (closing) return false;

        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }

        if (selectedCategory != Category.Theme) {
            List<ModuleComponent> comps = componentsByCategory.getOrDefault(selectedCategory, Collections.emptyList());
            for (ModuleComponent mcComp : comps) {
                mcComp.keyPressed(keyCode, scanCode, modifiers);
            }
        }
        if (activeSettings != null) {
            activeSettings.keyPressedExternal(keyCode, scanCode, modifiers);
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (closing) return false;

        if (selectedCategory != Category.Theme) {
            List<ModuleComponent> comps = componentsByCategory.getOrDefault(selectedCategory, Collections.emptyList());
            for (ModuleComponent mcComp : comps) {
                mcComp.charTyped(chr, modifiers);
            }
        }
        if (activeSettings != null) {
            activeSettings.charTypedExternal(chr, modifiers);
        }

        return super.charTyped(chr, modifiers);
    }

    public void setDescription(String text) {
        this.description = text == null ? "" : text;
    }

    private static float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }
}