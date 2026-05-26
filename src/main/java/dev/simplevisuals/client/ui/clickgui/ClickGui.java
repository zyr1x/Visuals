package dev.simplevisuals.client.ui.clickgui;

import dev.simplevisuals.client.ui.clickgui.components.impl.ModuleComponent;
import dev.simplevisuals.client.util.Wrapper;
import dev.simplevisuals.client.util.animations.Animation;
import dev.simplevisuals.client.util.animations.Easing;
import dev.simplevisuals.client.util.renderer.Render2D;
import dev.simplevisuals.client.util.renderer.fonts.Fonts;
import dev.simplevisuals.client.managers.ThemeManager;
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

    // ─── Анимации ────────────────────────────────────────────────────────────
    private final Animation openAnimation     = new Animation(320, 1f, true,  Easing.OUT_QUART);
    private final Animation settingsAnimation = new Animation(260, 1f, false, Easing.OUT_QUART);

    private final ThemeManager themeManager;

    private boolean closing = false;
    private float   uiAlpha = 0f;

    // Категории-вкладки
    private static final Category[] TABS = {
            Category.Render,
            Category.Utility,
            Category.Theme
    };

    private Category selectedCategory = Category.Render;

    // Размер главной панели (фиксированный)
    private static final float PANEL_W = 560f;
    private static final float PANEL_H = 370f;

    // Позиция — пересчитывается каждый кадр
    private float panelX, panelY;

    // Компоненты по категориям
    private final Map<Category, List<ModuleComponent>> componentsByCategory = new EnumMap<>(Category.class);

    // Скролл списка модулей
    private float scrollY       = 0f;
    private float scrollYTarget = 0f;
    private float maxScroll     = 0f;

    // Панель настроек слева
    private ModuleComponent activeSettings        = null;
    private float           settingsScrollY       = 0f;
    private float           settingsScrollYTarget = 0f;
    private float           settingsMaxScroll     = 0f;

    // Поиск
    private String  searchQuery   = "";
    private boolean searchFocused = false;

    // Константы компоновки
    private static final float TAB_H        = 28f;
    private static final float SEARCH_H     = 28f;
    private static final float MODULE_H     = 36f;
    private static final float MODULE_GAP   = 6f;
    private static final float SETTINGS_W   = 200f;
    private static final float SETTINGS_GAP = 6f;

    public ClickGui() {
        super(Text.of("simplevisuals-clickgui"));
        this.themeManager = ThemeManager.getInstance();
    }

    @Override
    public void init() {
        super.init();
        buildComponentsCache();
        scrollY        = 0f;
        scrollYTarget  = 0f;
        activeSettings = null;
        closing        = false;
        openAnimation.update(true);
    }

    private void buildComponentsCache() {
        componentsByCategory.clear();
        for (Category cat : TABS) {
            if (cat == Category.Theme) continue;
            List<Module> mods = simplevisuals.getInstance().getModuleManager().getModules(cat);
            List<ModuleComponent> comps = new ArrayList<>(mods.size());
            for (Module m : mods) comps.add(new ModuleComponent(m));
            componentsByCategory.put(cat, comps);
        }
    }

    // ─── Центрирование с учётом панели настроек ──────────────────────────────
    // Весь блок (настройки + главная панель) всегда по центру экрана.
    // По мере анимации settingsAnimation главная панель плавно смещается вправо,
    // а вместе с ней сдвигается и панель настроек — итого блок остаётся по центру.
    private void recalcPanelPosition(float centerY, float slideOff) {
        float settingsAnim    = (float) settingsAnimation.getValue();
        float settingsVisible = (SETTINGS_W + SETTINGS_GAP) * settingsAnim;
        float totalW          = settingsVisible + PANEL_W;

        panelX = (mc.getWindow().getScaledWidth() - totalW) / 2f + settingsVisible;
        panelY = centerY + slideOff;
    }

    // ─── Звук ────────────────────────────────────────────────────────────────
    private void playToggleSound(boolean wasToggled) {
        ClientSound cs = simplevisuals.getInstance().getModuleManager().getModule(ClientSound.class);
        if (cs != null && cs.isToggled()) {
            String id  = wasToggled ? cs.getDisableSoundId() : cs.getEnableSoundId();
            float  vol = cs.getVolume().getValue();
            MinecraftClient.getInstance().getSoundManager().play(
                    PositionedSoundInstance.master(SoundEvent.of(Identifier.of(id)), 1.0f, vol));
        }
    }

    // ─── Закрытие ────────────────────────────────────────────────────────────
    @Override
    public void close() {
        if (!closing) {
            closing = true;
            openAnimation.update(false);
        }
    }

    @Override public boolean shouldPause() { return false; }

    // ════════════════════════════════════════════════════════════════════════
    //  RENDER
    // ════════════════════════════════════════════════════════════════════════
    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        if (closing) {
            openAnimation.update(false);
            if (openAnimation.getValue() <= 0.01f) {
                simplevisuals.getInstance().getModuleManager().getModule(UI.class).setToggled(false);
                super.close();
                return;
            }
        } else {
            openAnimation.update(true);
        }

        uiAlpha = (float) Math.max(0, Math.min(1, openAnimation.getValue()));

        float centerY  = (mc.getWindow().getScaledHeight() - PANEL_H) / 2f;
        float slideOff = (1f - uiAlpha) * 14f;

        recalcPanelPosition(centerY, slideOff);

        // Затемнение фона
        int backdropA = (int) (160 * uiAlpha);
        if (backdropA > 0)
            Render2D.drawRect(ctx.getMatrices(), 0, 0,
                    mc.getWindow().getScaledWidth(), mc.getWindow().getScaledHeight(),
                    new Color(0, 0, 0, backdropA));

        renderSettingsPanel(ctx, mouseX, mouseY, delta);
        renderMainPanel(ctx, mouseX, mouseY, delta);
    }

    // ─── Панель настроек (слева) ─────────────────────────────────────────────
    private void renderSettingsPanel(DrawContext ctx, int mouseX, int mouseY, float delta) {
        boolean hasSettings = activeSettings != null && !activeSettings.getComponents().isEmpty();
        settingsAnimation.update(hasSettings);
        float anim = (float) settingsAnimation.getValue();
        if (anim <= 0.01f) return;

        float alpha  = Math.min(1f, anim * uiAlpha);
        int   panelA = (int) (255 * alpha);

        // Всегда вплотную слева от главной панели (без slideOff — двигается вместе с ней)
        float spX = panelX - SETTINGS_W - SETTINGS_GAP;
        float spY = panelY;

        Render2D.drawRoundedRect(ctx.getMatrices(), spX, spY, SETTINGS_W, PANEL_H, 10f,
                new Color(18, 18, 24, panelA));

        int textA = (int) (255 * alpha);
        if (activeSettings != null) {
            String title = I18n.translate(activeSettings.getModule().getName());
            if (title != null && !title.isEmpty()) {
                Render2D.drawFont(ctx.getMatrices(), Fonts.BOLD.getFont(9f),
                        title, spX + 14f, spY + 14f,
                        new Color(255, 255, 255, textA));
            }
        }

        Render2D.drawRect(ctx.getMatrices(), spX + 10f, spY + 30f, SETTINGS_W - 20f, 1f,
                new Color(255, 255, 255, (int) (30 * alpha)));

        float cX = spX + 8f;
        float cY = spY + 38f;
        float cW = SETTINGS_W - 16f;
        float cH = PANEL_H - 46f;

        float smooth = 0.18f;
        settingsScrollY += (settingsScrollYTarget - settingsScrollY) * smooth;
        settingsScrollY  = clamp(settingsScrollY, 0f, settingsMaxScroll);

        Render2D.startScissor(ctx, cX, cY, cW, cH);
        if (activeSettings != null) {
            activeSettings.setGlobalAlpha(alpha);
            float total = activeSettings.renderSettingsExternally(
                    ctx, cX, cY, cW, cX, cY, cW, cH, mouseX, mouseY, delta, settingsScrollY);
            settingsMaxScroll = Math.max(0f, total - cH);
        }
        Render2D.stopScissor(ctx);

        renderScrollbar(ctx, spX + SETTINGS_W - 4f, cY, cH, settingsScrollY, settingsMaxScroll, alpha);
    }

    // ─── Главная панель ───────────────────────────────────────────────────────
    private void renderMainPanel(DrawContext ctx, int mouseX, int mouseY, float delta) {
        int panelA = (int) (255 * uiAlpha);

        Render2D.drawRoundedRect(ctx.getMatrices(), panelX, panelY, PANEL_W, PANEL_H, 10f,
                new Color(14, 14, 20, panelA));

        renderHeader(ctx);

        if (selectedCategory == Category.Theme) {
            renderThemeTab(ctx, mouseX, mouseY);
        } else {
            renderModuleList(ctx, mouseX, mouseY, delta);
        }
    }

    // ─── Шапка ───────────────────────────────────────────────────────────────
    private void renderHeader(DrawContext ctx) {
        int textA = (int) (255 * uiAlpha);

        Render2D.drawFont(ctx.getMatrices(), Fonts.BOLD.getFont(11f),
                "DontVisuals", panelX + 18f, panelY + 12f,
                new Color(255, 255, 255, textA));

        Render2D.drawRect(ctx.getMatrices(), panelX, panelY + 34f, PANEL_W, 1f,
                new Color(255, 255, 255, (int) (20 * uiAlpha)));

        float tabStartX = panelX + 14f;
        float tabY      = panelY + 40f;
        float tabW      = 68f;
        float tabGap    = 4f;
        Color accent    = themeManager.getCurrentTheme().getAccentColor();

        for (int i = 0; i < TABS.length; i++) {
            Category cat    = TABS[i];
            boolean  active = cat == selectedCategory;
            float    tx     = tabStartX + i * (tabW + tabGap);
            String   label  = cat.name();

            if (active) {
                Render2D.drawRoundedRect(ctx.getMatrices(), tx, tabY, tabW, TAB_H, 6f,
                        new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), (int) (220 * uiAlpha)));
                float tw = Fonts.BOLD.getWidth(label, 8.5f);
                Render2D.drawFont(ctx.getMatrices(), Fonts.BOLD.getFont(8.5f), label,
                        tx + (tabW - tw) / 2f, tabY + (TAB_H - Fonts.BOLD.getHeight(8.5f)) / 2f,
                        new Color(255, 255, 255, textA));
            } else {
                Render2D.drawRoundedRect(ctx.getMatrices(), tx, tabY, tabW, TAB_H, 6f,
                        new Color(255, 255, 255, (int) (12 * uiAlpha)));
                float tw = Fonts.MEDIUM.getWidth(label, 8.5f);
                Render2D.drawFont(ctx.getMatrices(), Fonts.MEDIUM.getFont(8.5f), label,
                        tx + (tabW - tw) / 2f, tabY + (TAB_H - Fonts.MEDIUM.getHeight(8.5f)) / 2f,
                        new Color(180, 180, 190, textA));
            }
        }

        // Поиск скрываем на вкладке Theme
        if (selectedCategory != Category.Theme) {
            float searchX = panelX + PANEL_W - 160f - 14f;
            renderSearchBox(ctx, searchX, tabY, 160f, TAB_H);
        }
    }

    private void renderSearchBox(DrawContext ctx, float sx, float sy, float sw, float sh) {
        int a = (int) (255 * uiAlpha);
        Render2D.drawRoundedRect(ctx.getMatrices(), sx, sy, sw, sh, 6f,
                new Color(255, 255, 255, (int) (14 * uiAlpha)));
        Render2D.drawFont(ctx.getMatrices(), Fonts.SEMIBOLD.getFont(9f), "D",
                sx + 8f, sy + (sh - Fonts.SEMIBOLD.getHeight(9f)) / 2f,
                new Color(120, 120, 140, a));
        String display = searchQuery.isEmpty() ? "Search..." : searchQuery;
        Color  col     = searchQuery.isEmpty()
                ? new Color(100, 100, 120, a)
                : new Color(220, 220, 230, a);
        Render2D.drawFont(ctx.getMatrices(), Fonts.SEMIBOLD.getFont(8f), display,
                sx + 22f, sy + (sh - Fonts.SEMIBOLD.getHeight(8f)) / 2f, col);
        if (searchFocused) {
            float cursorX = sx + 22f + Fonts.BOLD.getWidth(searchQuery, 8f);
            float cursorY = sy + (sh - Fonts.BOLD.getHeight(8f)) / 2f;
            Render2D.drawRect(ctx.getMatrices(), cursorX + 1f, cursorY,
                    1f, Fonts.REGULAR.getHeight(8f),
                    new Color(200, 200, 220, (int) (180 * uiAlpha)));
        }
    }

    // ─── Вкладка тем ─────────────────────────────────────────────────────────
    private void renderThemeTab(DrawContext ctx, int mouseX, int mouseY) {
        ThemeManager.Theme[] themes = themeManager.getAvailableThemes();
        String currentName = themeManager.getCurrentTheme().getName();

        float startX = panelX + 14f;
        float startY = panelY + TAB_H + SEARCH_H + 28f;

        float cellW = 118f;
        float cellH = 52f;
        float gap   = 8f;
        int   cols  = 4;

        for (int i = 0; i < themes.length; i++) {
            ThemeManager.Theme t  = themes[i];
            float cx = startX + (i % cols) * (cellW + gap);
            float cy = startY + (i / cols) * (cellH + gap);

            boolean active = t.getName().equals(currentName);
            Color   accent = t.getAccentColor();

            // Фон карточки
            int bgA = (int) ((active ? 180 : 45) * uiAlpha);
            Render2D.drawRoundedRect(ctx.getMatrices(), cx, cy, cellW, cellH, 8f,
                    new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), bgA));

            // Рамка у активной
            if (active) {
                Render2D.drawRoundedRect(ctx.getMatrices(),
                        cx - 1.5f, cy - 1.5f, cellW + 3f, cellH + 3f, 9.5f,
                        new Color(accent.getRed(), accent.getGreen(), accent.getBlue(),
                                (int) (160 * uiAlpha)));
            }

            // Кружок с акцентным цветом (полностью непрозрачный)
            Render2D.drawRoundedRect(ctx.getMatrices(),
                    cx + 10f, cy + (cellH - 16f) / 2f, 16f, 16f, 8f,
                    new Color(accent.getRed(), accent.getGreen(), accent.getBlue(),
                            (int) (255 * uiAlpha)));

            // Название темы
            String label = t.getName();
            float  tw    = Fonts.BOLD.getWidth(label, 8.5f);
            float  tx    = cx + 32f + (cellW - 32f - tw) / 2f;
            float  ty    = cy + (cellH - Fonts.BOLD.getHeight(8.5f)) / 2f;
            Render2D.drawFont(ctx.getMatrices(), Fonts.BOLD.getFont(8.5f), label,
                    tx, ty, new Color(255, 255, 255, (int) (230 * uiAlpha)));
        }

        // Подпись снизу
        Render2D.drawFont(ctx.getMatrices(), Fonts.MEDIUM.getFont(7.5f),
                "Click a theme to apply",
                startX, startY + 2 * (cellH + gap) + 6f,
                new Color(140, 140, 160, (int) (120 * uiAlpha)));
    }

    // ─── Список модулей ───────────────────────────────────────────────────────
    private void renderModuleList(DrawContext ctx, int mouseX, int mouseY, float delta) {
        float listX = panelX + 10f;
        float listY = panelY + TAB_H + SEARCH_H + 28f;
        float listW = PANEL_W - 20f;
        float listH = PANEL_H - (listY - panelY) - 10f;

        float smooth = 0.18f;
        scrollY += (scrollYTarget - scrollY) * smooth;
        scrollY  = clamp(scrollY, 0f, maxScroll);

        List<ModuleComponent> comps = getFilteredComponents();
        if (comps == null) comps = Collections.emptyList();

        Render2D.startScissor(ctx, listX, listY, listW, listH);

        float curY = listY - scrollY;
        for (ModuleComponent mc : comps) {
            mc.setX(listX);
            mc.setY(curY);
            mc.setWidth(listW);
            mc.setHeight(MODULE_H);
            mc.setRenderExternally(true);
            mc.setGlobalAlpha(uiAlpha);
            mc.render(ctx, mouseX, mouseY, delta);
            curY += mc.getHeight() + MODULE_GAP;
        }

        Render2D.stopScissor(ctx);

        float contentH = comps.size() * (MODULE_H + MODULE_GAP);
        maxScroll     = Math.max(0f, contentH - listH);
        scrollYTarget = clamp(scrollYTarget, 0f, maxScroll);

        renderScrollbar(ctx, panelX + PANEL_W - 4f, listY, listH, scrollY, maxScroll, uiAlpha);
    }

    private List<ModuleComponent> getFilteredComponents() {
        List<ModuleComponent> base = componentsByCategory.getOrDefault(selectedCategory, Collections.emptyList());
        if (searchQuery.isEmpty()) return base;
        List<ModuleComponent> filtered = new ArrayList<>();
        String q = searchQuery.toLowerCase();
        for (ModuleComponent mc : base) {
            if (mc.getModule().getName().toLowerCase().contains(q)) filtered.add(mc);
        }
        return filtered;
    }

    // ─── Скроллбар ───────────────────────────────────────────────────────────
    private void renderScrollbar(DrawContext ctx, float trackX, float trackY,
                                 float trackH, float scroll, float maxScr, float alpha) {
        if (maxScr <= 0.5f) return;
        Color accent = themeManager.getCurrentTheme().getAccentColor();

        Render2D.drawRect(ctx.getMatrices(), trackX, trackY, 2f, trackH,
                new Color(0, 0, 0, (int) (80 * alpha)));

        float ratio  = trackH / Math.max(trackH + maxScr, 1f);
        float thumbH = Math.max(16f, trackH * ratio);
        float travel = trackH - thumbH;
        float thumbY = trackY + travel * (maxScr <= 0f ? 0f : scroll / maxScr);
        Render2D.drawRoundedRect(ctx.getMatrices(), trackX - 0.5f, thumbY, 3f, thumbH, 1.5f,
                new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), (int) (200 * alpha)));
    }

    // ════════════════════════════════════════════════════════════════════════
    //  ВВОД
    // ════════════════════════════════════════════════════════════════════════
    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (closing) return false;

        // ── Вкладки ──
        float tabStartX = panelX + 14f;
        float tabY      = panelY + 40f;
        float tabW      = 68f;
        float tabGap    = 4f;
        for (int i = 0; i < TABS.length; i++) {
            float tx = tabStartX + i * (tabW + tabGap);
            if (mx >= tx && mx <= tx + tabW && my >= tabY && my <= tabY + TAB_H) {
                selectedCategory  = TABS[i];
                scrollY           = 0f;
                scrollYTarget     = 0f;
                activeSettings    = null;
                searchQuery       = "";
                searchFocused     = false;
                return true;
            }
        }

        // ── Клики по вкладке тем ──
        if (selectedCategory == Category.Theme) {
            ThemeManager.Theme[] themes = themeManager.getAvailableThemes();
            float startX = panelX + 14f;
            float startY = panelY + TAB_H + SEARCH_H + 28f;
            float cellW  = 118f, cellH = 52f, gap = 8f;
            int   cols   = 4;
            for (int i = 0; i < themes.length; i++) {
                float cx = startX + (i % cols) * (cellW + gap);
                float cy = startY + (i / cols) * (cellH + gap);
                if (mx >= cx && mx <= cx + cellW && my >= cy && my <= cy + cellH) {
                    themeManager.setTheme(themes[i]);
                    return true;
                }
            }
            return true;
        }

        // ── Поиск ──
        float searchX = panelX + PANEL_W - 160f - 14f;
        if (mx >= searchX && mx <= searchX + 160f && my >= tabY && my <= tabY + TAB_H) {
            searchFocused = true;
            return true;
        } else {
            searchFocused = false;
        }

        // ── Список модулей ──
        float listX = panelX + 10f;
        float listY = panelY + TAB_H + SEARCH_H + 28f;
        float listW = PANEL_W - 20f;
        float listH = PANEL_H - (listY - panelY) - 10f;

        if (mx >= listX && mx <= listX + listW && my >= listY && my <= listY + listH) {
            List<ModuleComponent> comps = getFilteredComponents();
            if (comps == null) comps = Collections.emptyList();

            for (ModuleComponent mc : comps) {
                if (mc.isBindModeMenuOpen()) { mc.mouseClicked(mx, my, btn); return true; }
            }

            for (ModuleComponent mc : comps) {
                boolean wasToggled = mc.getModule().isToggled();
                float   headerH    = mc.getHeight();

                if (btn == 1 && mx >= mc.getX() && mx <= mc.getX() + mc.getWidth()
                        && my >= mc.getY() && my <= mc.getY() + headerH) {
                    activeSettings        = mc.getComponents().isEmpty() ? null : mc;
                    settingsScrollY       = 0f;
                    settingsScrollYTarget = 0f;
                    return true;
                }

                mc.mouseClicked(mx, my, btn);
                if (btn == 0 && wasToggled != mc.getModule().isToggled())
                    playToggleSound(wasToggled);
            }
        }

        // ── Панель настроек ──
        if (activeSettings != null && !activeSettings.getComponents().isEmpty()) {
            float anim = (float) settingsAnimation.getValue();
            if (anim > 0.1f) {
                float spX = panelX - SETTINGS_W - SETTINGS_GAP;
                float cX  = spX + 8f;
                float cY  = panelY + 38f;
                float cW  = SETTINGS_W - 16f;
                float cH  = PANEL_H - 46f;
                if (mx >= cX && mx <= cX + cW && my >= cY && my <= cY + cH) {
                    activeSettings.mouseClickedExternal(mx, my, btn);
                    return true;
                }
            }
        }

        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int btn) {
        List<ModuleComponent> comps = getFilteredComponents();
        if (comps != null) {
            for (ModuleComponent mc : comps) {
                if (mc.isBindModeMenuOpen()) { mc.mouseReleased(mx, my, btn); return true; }
            }
            for (ModuleComponent mc : comps) mc.mouseReleased(mx, my, btn);
        }
        if (activeSettings != null) activeSettings.mouseReleasedExternal(mx, my, btn);
        return super.mouseReleased(mx, my, btn);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double h, double v) {
        float listX = panelX + 10f;
        float listY = panelY + TAB_H + SEARCH_H + 28f;
        float listW = PANEL_W - 20f;
        float listH = PANEL_H - (listY - panelY) - 10f;
        float step  = (float) (-v * 12f);

        if (mx >= listX && mx <= listX + listW && my >= listY && my <= listY + listH) {
            scrollYTarget = clamp(scrollYTarget + step, 0f, maxScroll);
            return true;
        }

        if (activeSettings != null && !activeSettings.getComponents().isEmpty()) {
            float anim = (float) settingsAnimation.getValue();
            if (anim > 0.1f) {
                float spX = panelX - SETTINGS_W - SETTINGS_GAP;
                float cX  = spX + 8f;
                float cY  = panelY + 38f;
                float cW  = SETTINGS_W - 16f;
                float cH  = PANEL_H - 46f;
                if (mx >= cX && mx <= cX + cW && my >= cY && my <= cY + cH) {
                    settingsScrollYTarget = clamp(settingsScrollYTarget + step, 0f, settingsMaxScroll);
                    return true;
                }
            }
        }
        return super.mouseScrolled(mx, my, h, v);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (closing) return false;

        if (key == GLFW.GLFW_KEY_ESCAPE) {
            if (searchFocused && !searchQuery.isEmpty()) {
                searchQuery   = "";
                searchFocused = false;
                return true;
            }
            close();
            return true;
        }

        if (searchFocused) {
            if (key == GLFW.GLFW_KEY_BACKSPACE && !searchQuery.isEmpty())
                searchQuery = searchQuery.substring(0, searchQuery.length() - 1);
            return true;
        }

        List<ModuleComponent> comps = getFilteredComponents();
        if (comps != null) for (ModuleComponent mc : comps) mc.keyPressed(key, scan, mods);
        if (activeSettings != null) activeSettings.keyPressedExternal(key, scan, mods);

        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean charTyped(char chr, int mods) {
        if (closing) return false;

        if (searchFocused) {
            searchQuery += chr;
            return true;
        }

        List<ModuleComponent> comps = getFilteredComponents();
        if (comps != null) for (ModuleComponent mc : comps) mc.charTyped(chr, mods);
        if (activeSettings != null) activeSettings.charTypedExternal(chr, mods);

        return super.charTyped(chr, mods);
    }

    // ─── Утилиты ─────────────────────────────────────────────────────────────
    private static float clamp(float v, float lo, float hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}