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

    // --- СОХРАНЯЕМОЕ СОСТОЯНИЕ ---
    private static Category selectedCategory = Category.Render;
    private static float scrollY             = 0f;
    private static float scrollYTarget       = 0f;
    // -----------------------------

    // ─── Размеры панели ───────────────────────────────────────────────────────
    // Левая боковая панель с категориями
    private static final float SIDEBAR_W    = 150f;
    // Правая панель с модулями
    private static final float CONTENT_W    = 370f;
    // Общий размер
    private static final float PANEL_W      = SIDEBAR_W + CONTENT_W;
    private static final float PANEL_H      = 300f;

    // Позиция (пересчитывается каждый кадр)
    private float panelX, panelY;

    // Компоненты по категориям
    private final Map<Category, List<ModuleComponent>> componentsByCategory = new EnumMap<>(Category.class);

    // Скролл
    private float maxScroll = 0f;

    // Панель настроек (правее content)
    private ModuleComponent activeSettings        = null;
    private float           settingsScrollY       = 0f;
    private float           settingsScrollYTarget = 0f;
    private float           settingsMaxScroll     = 0f;
    private static final float SETTINGS_W   = 140f;
    private static final float SETTINGS_GAP = 6f;

    // Поиск
    private String  searchQuery   = "";
    private boolean searchFocused = false;

    // Константы компоновки
    private static final float HEADER_H    = 44f;  // шапка с лого + поиск
    private static final float MODULE_H    = 40f;  // высота карточки модуля
    private static final float MODULE_GAP  = 5f;
    private static final float MODULE_COLS = 2;    // 2 колонки

    public ClickGui() {
        super(Text.of("simplevisuals-clickgui"));
        this.themeManager = ThemeManager.getInstance();
    }

    @Override
    public void init() {
        super.init();
        buildComponentsCache();
        closing = false;
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

    private void recalcPanelPosition(float centerY, float slideOff) {
        float settingsAnim    = (float) settingsAnimation.getValue();
        float settingsVisible = (SETTINGS_W + SETTINGS_GAP) * settingsAnim;
        float totalW          = PANEL_W + settingsVisible;

        panelX = (mc.getWindow().getScaledWidth() - totalW) / 2f;
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

    // ─── Панель настроек (правее content) ────────────────────────────────────
    private void renderSettingsPanel(DrawContext ctx, int mouseX, int mouseY, float delta) {
        boolean hasSettings = activeSettings != null && !activeSettings.getComponents().isEmpty();
        settingsAnimation.update(hasSettings);
        float anim = (float) settingsAnimation.getValue();
        if (anim <= 0.01f) return;

        float alpha  = Math.min(1f, anim * uiAlpha);
        int   panelA = (int) (255 * alpha);

        float spX = panelX + PANEL_W + SETTINGS_GAP;
        float spY = panelY;

        Render2D.drawRoundedRect(ctx.getMatrices(), spX, spY, SETTINGS_W, PANEL_H, 10f,
                new Color(18, 18, 24, panelA));
        Render2D.drawGradientRect(ctx.getMatrices(), spX, spY, SETTINGS_W, PANEL_H,
                new Color(255, 255, 255, (int)(18 * alpha)),
                new Color(255, 255, 255, 0), false);

        int textA = (int)(255 * alpha);
        if (activeSettings != null) {
            String title = I18n.translate(activeSettings.getModule().getName());
            if (title != null && !title.isEmpty()) {
                Render2D.drawFont(ctx.getMatrices(), Fonts.BOLD.getFont(9f),
                        title, spX + 14f, spY + 14f,
                        new Color(255, 255, 255, textA));
            }
        }
        Render2D.drawRect(ctx.getMatrices(), spX + 10f, spY + 30f, SETTINGS_W - 20f, 1f,
                new Color(255, 255, 255, (int)(30 * alpha)));

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
        int panelA = (int)(255 * uiAlpha);

        // Общий фон всей панели
        Render2D.drawRoundedRect(ctx.getMatrices(), panelX, panelY, PANEL_W, PANEL_H, 12f,
                new Color(14, 14, 20, panelA));
        Render2D.drawGradientRect(ctx.getMatrices(), panelX, panelY, PANEL_W, PANEL_H,
                new Color(255, 255, 255, (int)(12 * uiAlpha)),
                new Color(255, 255, 255, 0), false);

        renderSidebar(ctx, mouseX, mouseY);
        renderContentArea(ctx, mouseX, mouseY, delta);
    }

    // ─── Левая боковая панель ─────────────────────────────────────────────────
    private void renderSidebar(DrawContext ctx, int mouseX, int mouseY) {
        int panelA = (int)(255 * uiAlpha);

        // Фон сайдбара (чуть светлее)
        Render2D.drawRoundedRect(ctx.getMatrices(), panelX, panelY, SIDEBAR_W, PANEL_H, 12f,
                new Color(20, 20, 28, panelA));
        // Правая граница сайдбара
        Render2D.drawRect(ctx.getMatrices(), panelX + SIDEBAR_W - 1f, panelY + 10f,
                1f, PANEL_H - 20f, new Color(255, 255, 255, (int)(15 * uiAlpha)));

        // Логотип / название клиента
        Render2D.drawFont(ctx.getMatrices(), Fonts.BOLD.getFont(13f),
                "DV", panelX + 14f, panelY + 14f,
                new Color(255, 255, 255, (int)(220 * uiAlpha)));

        // Поиск
        renderSidebarSearch(ctx, mouseX, mouseY);

        // Разделитель
        Render2D.drawRect(ctx.getMatrices(), panelX + 10f, panelY + 68f, SIDEBAR_W - 20f, 1f,
                new Color(255, 255, 255, (int)(18 * uiAlpha)));

        // Кнопки категорий
        float tabY = panelY + 78f;
        float tabH = 28f;
        float tabGap = 4f;
        Color accent = themeManager.getCurrentTheme().getAccentColor();

        for (int i = 0; i < TABS.length; i++) {
            Category cat    = TABS[i];
            boolean  active = cat == selectedCategory;
            float    ty     = tabY + i * (tabH + tabGap);
            float    tx     = panelX + 8f;
            float    tw     = SIDEBAR_W - 16f;

            if (active) {
                Render2D.drawRoundedRect(ctx.getMatrices(), tx, ty, tw, tabH, 7f,
                        new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), (int)(210 * uiAlpha)));
            } else {
                // Hover-подсветка
                boolean hovered = mouseX >= tx && mouseX <= tx + tw && mouseY >= ty && mouseY <= ty + tabH;
                if (hovered) {
                    Render2D.drawRoundedRect(ctx.getMatrices(), tx, ty, tw, tabH, 7f,
                            new Color(255, 255, 255, (int)(10 * uiAlpha)));
                }
            }

            // Иконка категории
            String icon = getCategoryIcon(cat);
            Render2D.drawFont(ctx.getMatrices(), Fonts.REGULAR.getFont(9f),
                    icon, tx + 9f, ty + (tabH - Fonts.REGULAR.getHeight(9f)) / 2f,
                    new Color(active ? 255 : 180, active ? 255 : 180, active ? 255 : 190,
                            (int)(230 * uiAlpha)));

            // Название категории
            String label = cat.name();
            Render2D.drawFont(ctx.getMatrices(),
                    active ? Fonts.BOLD.getFont(8.5f) : Fonts.MEDIUM.getFont(8.5f),
                    label,
                    tx + 26f, ty + (tabH - Fonts.MEDIUM.getHeight(8.5f)) / 2f,
                    new Color(active ? 255 : 190, active ? 255 : 190, active ? 255 : 200,
                            (int)(230 * uiAlpha)));
        }

        // Никнейм внизу
        renderBottomUser(ctx);
    }

    private void renderSidebarSearch(DrawContext ctx, int mouseX, int mouseY) {
        float sx = panelX + 8f;
        float sy = panelY + 36f;
        float sw = SIDEBAR_W - 16f;
        float sh = 22f;
        int   a  = (int)(255 * uiAlpha);

        Render2D.drawRoundedRect(ctx.getMatrices(), sx, sy, sw, sh, 6f,
                new Color(255, 255, 255, (int)(12 * uiAlpha)));

        // Иконка поиска
        Render2D.drawFont(ctx.getMatrices(), Fonts.SEMIBOLD.getFont(8f),
                "D", sx + 7f, sy + (sh - Fonts.SEMIBOLD.getHeight(8f)) / 2f,
                new Color(140, 140, 160, a));

        String display = searchQuery.isEmpty() ? "Поиск" : searchQuery;
        Color  col     = searchQuery.isEmpty() ? new Color(100, 100, 120, a) : new Color(220, 220, 230, a);
        Render2D.drawFont(ctx.getMatrices(), Fonts.SEMIBOLD.getFont(7.5f), display,
                sx + 20f, sy + (sh - Fonts.SEMIBOLD.getHeight(7.5f)) / 2f, col);

        if (searchFocused) {
            float cursorX = sx + 20f + Fonts.BOLD.getWidth(searchQuery, 7.5f);
            float cursorY = sy + (sh - Fonts.BOLD.getHeight(7.5f)) / 2f;
            Render2D.drawRect(ctx.getMatrices(), cursorX + 1f, cursorY,
                    1f, Fonts.REGULAR.getHeight(7.5f),
                    new Color(200, 200, 220, (int)(180 * uiAlpha)));
        }
    }

    private void renderBottomUser(DrawContext ctx) {
        String username = mc.getSession().getUsername();
        float  bx       = panelX + 8f;
        float  bw       = SIDEBAR_W - 16f;
        float  bh       = 26f;
        float  by       = panelY + PANEL_H - bh - 8f;
        int    a        = (int)(200 * uiAlpha);

        Render2D.drawRoundedRect(ctx.getMatrices(), bx, by, bw, bh, 7f,
                new Color(255, 255, 255, (int)(8 * uiAlpha)));

        // Аватар-заглушка
        Color accent = themeManager.getCurrentTheme().getAccentColor();
        Render2D.drawRoundedRect(ctx.getMatrices(), bx + 5f, by + (bh - 16f) / 2f, 16f, 16f, 8f,
                new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), (int)(180 * uiAlpha)));

        Render2D.drawFont(ctx.getMatrices(), Fonts.MEDIUM.getFont(7.5f), username,
                bx + 26f, by + (bh - Fonts.MEDIUM.getHeight(7.5f)) / 2f,
                new Color(200, 200, 210, a));
    }

    private String getCategoryIcon(Category cat) {
        return switch (cat) {
            case Render  -> "R";
            case Utility -> "U";
            case Theme   -> "T";
            default      -> "DEF";
        };
    }

    // ─── Область контента (правее сайдбара) ──────────────────────────────────
    private void renderContentArea(DrawContext ctx, int mouseX, int mouseY, float delta) {
        float cx = panelX + SIDEBAR_W;
        float cy = panelY;
        float cw = CONTENT_W;

        if (selectedCategory == Category.Theme) {
            renderThemeTab(ctx, mouseX, mouseY, cx, cy, cw);
        } else {
            renderModuleList(ctx, mouseX, mouseY, delta, cx, cy, cw);
        }
    }

    // ─── Вкладка тем ─────────────────────────────────────────────────────────
    private void renderThemeTab(DrawContext ctx, int mouseX, int mouseY, float startCX, float startCY, float cw) {
        ThemeManager.Theme[] themes = themeManager.getAvailableThemes();
        String currentName = themeManager.getCurrentTheme().getName();

        float sx   = startCX + 12f;
        float sy   = startCY + 14f;
        float cellW = 80f;
        float cellH = 38f;
        float gap   = 8f;
        int   cols  = (int)((cw - 12f) / (cellW + gap));

        for (int i = 0; i < themes.length; i++) {
            ThemeManager.Theme t  = themes[i];
            float tx = sx + (i % cols) * (cellW + gap);
            float ty = sy + (i / cols) * (cellH + gap);

            boolean active = t.getName().equals(currentName);
            Color   accent = t.getAccentColor();

            int bgA = (int)((active ? 180 : 45) * uiAlpha);
            Render2D.drawRoundedRect(ctx.getMatrices(), tx, ty, cellW, cellH, 8f,
                    new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), bgA));

            if (active) {
                Render2D.drawRoundedRect(ctx.getMatrices(),
                        tx - 1.5f, ty - 1.5f, cellW + 3f, cellH + 3f, 9.5f,
                        new Color(accent.getRed(), accent.getGreen(), accent.getBlue(),
                                (int)(160 * uiAlpha)));
            }

            Render2D.drawRoundedRect(ctx.getMatrices(),
                    tx + 8f, ty + (cellH - 14f) / 2f, 14f, 14f, 7f,
                    new Color(accent.getRed(), accent.getGreen(), accent.getBlue(),
                            (int)(255 * uiAlpha)));

            String label = t.getName();
            float  lw    = Fonts.BOLD.getWidth(label, 7.5f);
            Render2D.drawFont(ctx.getMatrices(), Fonts.BOLD.getFont(7.5f), label,
                    tx + 26f + (cellW - 26f - lw) / 2f, ty + (cellH - Fonts.BOLD.getHeight(7.5f)) / 2f,
                    new Color(255, 255, 255, (int)(230 * uiAlpha)));
        }
    }

    // ─── Список модулей в 2 колонки ──────────────────────────────────────────
    private void renderModuleList(DrawContext ctx, int mouseX, int mouseY, float delta,
                                  float areaX, float areaY, float areaW) {
        float padH  = 10f;
        float padV  = 10f;
        float listX = areaX + padH;
        float listY = areaY + padV;
        float listW = areaW - padH * 2f;
        float listH = PANEL_H - padV * 2f;

        float smooth = 0.18f;
        scrollY += (scrollYTarget - scrollY) * smooth;
        scrollY  = clamp(scrollY, 0f, maxScroll);

        List<ModuleComponent> comps = getFilteredComponents();
        if (comps == null) comps = Collections.emptyList();

        // Ширина одной карточки (2 колонки с зазором)
        float colGap = 6f;
        float cardW  = (listW - colGap) / MODULE_COLS;

        Render2D.startScissor(ctx, listX, listY, listW, listH);

        float baseY = listY - scrollY;
        for (int i = 0; i < comps.size(); i++) {
            ModuleComponent mc = comps.get(i);
            int   col  = i % (int) MODULE_COLS;
            int   row  = i / (int) MODULE_COLS;
            float mcX  = listX + col * (cardW + colGap);
            float mcY  = baseY + row * (MODULE_H + MODULE_GAP);

            mc.setX(mcX);
            mc.setY(mcY);
            mc.setWidth(cardW);
            mc.setHeight(MODULE_H);
            mc.setRenderExternally(true);
            mc.setGlobalAlpha(uiAlpha);
            mc.render(ctx, mouseX, mouseY, delta);
        }

        Render2D.stopScissor(ctx);

        int rows      = (int) Math.ceil((double) comps.size() / MODULE_COLS);
        float contentH = rows * (MODULE_H + MODULE_GAP);
        maxScroll      = Math.max(0f, contentH - listH);
        scrollYTarget  = clamp(scrollYTarget, 0f, maxScroll);

        renderScrollbar(ctx, areaX + areaW - 3f, listY, listH, scrollY, maxScroll, uiAlpha);
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
                new Color(0, 0, 0, (int)(80 * alpha)));
        float ratio  = trackH / Math.max(trackH + maxScr, 1f);
        float thumbH = Math.max(16f, trackH * ratio);
        float travel = trackH - thumbH;
        float thumbY = trackY + travel * (maxScr <= 0f ? 0f : scroll / maxScr);
        Render2D.drawRoundedRect(ctx.getMatrices(), trackX - 0.5f, thumbY, 3f, thumbH, 1.5f,
                new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), (int)(200 * alpha)));
    }

    // ════════════════════════════════════════════════════════════════════════
    //  ВВОД
    // ════════════════════════════════════════════════════════════════════════
    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (closing) return false;

        // ── Вкладки (в сайдбаре) ──
        float tabY   = panelY + 78f;
        float tabH   = 28f;
        float tabGap = 4f;
        float tx     = panelX + 8f;
        float tw     = SIDEBAR_W - 16f;
        for (int i = 0; i < TABS.length; i++) {
            float ty = tabY + i * (tabH + tabGap);
            if (mx >= tx && mx <= tx + tw && my >= ty && my <= ty + tabH) {
                if (TABS[i] != selectedCategory) {
                    scrollY        = 0f;
                    scrollYTarget  = 0f;
                    activeSettings = null;
                    searchQuery    = "";
                    searchFocused  = false;
                }
                selectedCategory = TABS[i];
                return true;
            }
        }

        // ── Клики по поиску (в сайдбаре) ──
        float sx = panelX + 8f;
        float sy = panelY + 36f;
        float sw = SIDEBAR_W - 16f;
        float sh = 22f;
        if (mx >= sx && mx <= sx + sw && my >= sy && my <= sy + sh) {
            searchFocused = true;
            return true;
        } else {
            searchFocused = false;
        }

        // ── Клики по теме ──
        if (selectedCategory == Category.Theme) {
            ThemeManager.Theme[] themes = themeManager.getAvailableThemes();
            float startX = panelX + SIDEBAR_W + 12f;
            float startY = panelY + 14f;
            float cellW  = 80f, cellH = 38f, gap = 8f;
            float contentW = CONTENT_W - 12f;
            int   cols   = (int)(contentW / (cellW + gap));
            for (int i = 0; i < themes.length; i++) {
                float tcx = startX + (i % cols) * (cellW + gap);
                float tcy = startY + (i / cols) * (cellH + gap);
                if (mx >= tcx && mx <= tcx + cellW && my >= tcy && my <= tcy + cellH) {
                    themeManager.setTheme(themes[i]);
                    return true;
                }
            }
            return true;
        }

        // ── Список модулей ──
        float colGap = 6f;
        float listX  = panelX + SIDEBAR_W + 10f;
        float listY  = panelY + 10f;
        float listW  = CONTENT_W - 20f;
        float listH  = PANEL_H - 20f;
        float cardW  = (listW - colGap) / MODULE_COLS;

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
                float spX = panelX + PANEL_W + SETTINGS_GAP;
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
        float listX = panelX + SIDEBAR_W + 10f;
        float listY = panelY + 10f;
        float listW = CONTENT_W - 20f;
        float listH = PANEL_H - 20f;
        float step  = (float)(-v * 12f);

        if (mx >= listX && mx <= listX + listW && my >= listY && my <= listY + listH) {
            scrollYTarget = clamp(scrollYTarget + step, 0f, maxScroll);
            return true;
        }

        if (activeSettings != null && !activeSettings.getComponents().isEmpty()) {
            float anim = (float) settingsAnimation.getValue();
            if (anim > 0.1f) {
                float spX = panelX + PANEL_W + SETTINGS_GAP;
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