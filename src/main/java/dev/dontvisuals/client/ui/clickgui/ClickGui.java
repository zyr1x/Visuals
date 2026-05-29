package dev.dontvisuals.client.ui.clickgui;

import dev.dontvisuals.client.ui.clickgui.components.impl.ModuleComponent;
import dev.dontvisuals.client.util.Wrapper;
import dev.dontvisuals.client.util.animations.Animation;
import dev.dontvisuals.client.util.animations.Easing;
import dev.dontvisuals.client.util.renderer.Render2D;
import dev.dontvisuals.client.util.renderer.fonts.Fonts;
import dev.dontvisuals.client.managers.ThemeManager;
import dev.dontvisuals.modules.api.Category;
import dev.dontvisuals.modules.api.Module;
import dev.dontvisuals.modules.impl.render.UI;
import dev.dontvisuals.modules.impl.utility.ClientSound;
import dev.dontvisuals.dontvisuals;
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

    private final Animation openAnimation     = new Animation(320, 1f, true,  Easing.OUT_QUART);
    private final Animation settingsAnimation = new Animation(260, 1f, false, Easing.OUT_QUART);

    private final ThemeManager themeManager;

    private boolean closing = false;
    private float   uiAlpha = 0f;

    private static final Category[] TABS = {
            Category.Render,
            Category.Utility,
            Category.Theme
    };

    private static Category selectedCategory = Category.Render;
    private static float scrollY             = 0f;
    private static float scrollYTarget       = 0f;
    private static float themeScrollY       = 0f;
    private static float themeScrollYTarget = 0f;
    private float        themeMaxScroll     = 0f;

    private static final float SIDEBAR_W    = 150f;
    private static final float CONTENT_W    = 370f;
    private static final float PANEL_W      = SIDEBAR_W + CONTENT_W;
    private static final float PANEL_H      = 300f;

    private float panelX, panelY;

    private final Map<Category, List<ModuleComponent>> componentsByCategory = new EnumMap<>(Category.class);

    private float maxScroll = 0f;

    private ModuleComponent activeSettings        = null;
    private float           settingsScrollY       = 0f;
    private float           settingsScrollYTarget = 0f;
    private float           settingsMaxScroll     = 0f;
    private static final float SETTINGS_W   = 140f;
    private static final float SETTINGS_GAP = 6f;

    private String  searchQuery   = "";
    private boolean searchFocused = false;

    private static final float HEADER_H    = 44f;
    private static final float MODULE_H    = 40f;
    private static final float MODULE_GAP  = 5f;
    private static final float MODULE_COLS = 2;

    public ClickGui() {
        super(Text.of("dontvisuals-clickgui"));
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
            List<Module> mods = dontvisuals.getInstance().getModuleManager().getModules(cat);
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

    private void playToggleSound(boolean wasToggled) {
        ClientSound cs = dontvisuals.getInstance().getModuleManager().getModule(ClientSound.class);
        if (cs != null && cs.isToggled()) {
            String id  = wasToggled ? cs.getDisableSoundId() : cs.getEnableSoundId();
            float  vol = cs.getVolume().getValue();
            MinecraftClient.getInstance().getSoundManager().play(
                    PositionedSoundInstance.master(SoundEvent.of(Identifier.of(id)), 1.0f, vol));
        }
    }

    @Override
    public void close() {
        if (!closing) {
            closing = true;
            openAnimation.update(false);
        }
    }

    @Override public boolean shouldPause() { return false; }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        if (closing) {
            openAnimation.update(false);
            if (openAnimation.getValue() <= 0.01f) {
                dontvisuals.getInstance().getModuleManager().getModule(UI.class).setToggled(false);
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

        int backdropA = (int) (160 * uiAlpha);
        if (backdropA > 0)
            Render2D.drawRect(ctx.getMatrices(), 0, 0,
                    mc.getWindow().getScaledWidth(), mc.getWindow().getScaledHeight(),
                    new Color(0, 0, 0, backdropA));

        renderSettingsPanel(ctx, mouseX, mouseY, delta);
        renderMainPanel(ctx, mouseX, mouseY, delta);
    }

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

    private void renderMainPanel(DrawContext ctx, int mouseX, int mouseY, float delta) {
        int panelA = (int)(255 * uiAlpha);

        Render2D.drawRoundedRect(ctx.getMatrices(), panelX, panelY, PANEL_W, PANEL_H, 12f,
                new Color(14, 14, 20, panelA));

        renderSidebar(ctx, mouseX, mouseY);
        renderContentArea(ctx, mouseX, mouseY, delta);
    }

    private void renderSidebar(DrawContext ctx, int mouseX, int mouseY) {
        int panelA = (int)(255 * uiAlpha);

        Render2D.drawRoundedRect(ctx.getMatrices(), panelX, panelY, SIDEBAR_W, PANEL_H, 12f,
                new Color(20, 20, 28, panelA));
        Render2D.drawRect(ctx.getMatrices(),
                panelX + SIDEBAR_W - 14f, panelY,
                14f, PANEL_H,
                new Color(20, 20, 28, panelA));
        Render2D.drawRect(ctx.getMatrices(), panelX + SIDEBAR_W - 1f, panelY + 10f,
                1f, PANEL_H - 20f, new Color(255, 255, 255, (int)(15 * uiAlpha)));

        Render2D.drawFont(ctx.getMatrices(), Fonts.BOLD.getFont(13f),
                "DV", panelX + 14f, panelY + 14f,
                new Color(255, 255, 255, (int)(220 * uiAlpha)));

        renderSidebarSearch(ctx, mouseX, mouseY);

        Render2D.drawRect(ctx.getMatrices(), panelX + 10f, panelY + 68f, SIDEBAR_W - 20f, 1f,
                new Color(255, 255, 255, (int)(18 * uiAlpha)));

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
                boolean hovered = mouseX >= tx && mouseX <= tx + tw && mouseY >= ty && mouseY <= ty + tabH;
                if (hovered) {
                    Render2D.drawRoundedRect(ctx.getMatrices(), tx, ty, tw, tabH, 7f,
                            new Color(255, 255, 255, (int)(10 * uiAlpha)));
                }
            }

            String icon = getCategoryIcon(cat);
            Render2D.drawFont(ctx.getMatrices(), Fonts.REGULAR.getFont(9f),
                    icon, tx + 9f, ty + (tabH - Fonts.REGULAR.getHeight(9f)) / 2f,
                    new Color(active ? 255 : 180, active ? 255 : 180, active ? 255 : 190,
                            (int)(230 * uiAlpha)));

            String label = cat.name();
            Render2D.drawFont(ctx.getMatrices(),
                    active ? Fonts.BOLD.getFont(8.5f) : Fonts.MEDIUM.getFont(8.5f),
                    label,
                    tx + 26f, ty + (tabH - Fonts.MEDIUM.getHeight(8.5f)) / 2f,
                    new Color(active ? 255 : 190, active ? 255 : 190, active ? 255 : 200,
                            (int)(230 * uiAlpha)));
        }

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

        float avatarX    = bx + 5f;
        float avatarY    = by + (bh - 16f) / 2f;
        float avatarSize = 16f;

        if (mc.player != null) {
            // Фон аватара
            Render2D.drawRoundedRect(ctx.getMatrices(),
                    avatarX, avatarY, avatarSize, avatarSize, 4f,
                    new Color(30, 30, 40, (int)(200 * uiAlpha)));

            // Лицо скина — точно как в TargetHud, UV: начало 0.125 (8/64), размер 0.125 (8/64)
            Render2D.drawTexture(
                    ctx.getMatrices(),
                    avatarX, avatarY, avatarSize, avatarSize, 4f,
                    0.125f, 0.125f, 0.125f, 0.125f,
                    ((net.minecraft.client.network.AbstractClientPlayerEntity) mc.player)
                            .getSkinTextures().texture(),
                    new Color(255, 255, 255, (int)(255 * uiAlpha))
            );
        } else {
            fallbackAvatar(ctx, avatarX, avatarY, (int) avatarSize);
        }

        Render2D.drawFont(ctx.getMatrices(), Fonts.MEDIUM.getFont(7.5f), username,
                bx + 26f, by + (bh - Fonts.MEDIUM.getHeight(7.5f)) / 2f,
                new Color(200, 200, 210, a));
    }

    private void fallbackAvatar(DrawContext ctx, float x, float y, int size) {
        Color accent = themeManager.getCurrentTheme().getAccentColor();
        Render2D.drawRoundedRect(ctx.getMatrices(), x, y, size, size, 4f,
                new Color(accent.getRed(), accent.getGreen(), accent.getBlue(),
                        (int)(180 * uiAlpha)));
    }

    private String getCategoryIcon(Category cat) {
        return switch (cat) {
            case Render  -> "R";
            case Utility -> "U";
            case Theme   -> "T";
            default      -> "DEF";
        };
    }

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

    private static final float THEME_CELL_W   = 155f;
    private static final float THEME_CELL_H   = 34f;
    private static final float THEME_GAP      = 6f;
    private static final float THEME_COL_GAP  = 10f;
    private static final float THEME_HEADER_H = 18f;

    private void renderThemeTab(DrawContext ctx, int mouseX, int mouseY, float startCX, float startCY, float cw) {
        ThemeManager.Theme[] staticThemes   = themeManager.getStaticThemes();
        ThemeManager.Theme[] gradientThemes = themeManager.getGradientThemes();
        String currentName = themeManager.getCurrentTheme().getName();

        float pad    = 12f;
        float colX1  = startCX + pad;
        float colX2  = colX1 + THEME_CELL_W + THEME_COL_GAP;

        float clipX  = startCX;
        float clipY  = startCY;
        float clipW  = cw;
        float clipH  = PANEL_H;

        float smooth = 0.18f;
        themeScrollY += (themeScrollYTarget - themeScrollY) * smooth;

        int maxItems = Math.max(staticThemes.length, gradientThemes.length);
        float totalContentH = THEME_HEADER_H + 4f + maxItems * (THEME_CELL_H + THEME_GAP) + 10f;
        themeMaxScroll = Math.max(0f, totalContentH - clipH);
        themeScrollY   = clamp(themeScrollY, 0f, themeMaxScroll);
        themeScrollYTarget = clamp(themeScrollYTarget, 0f, themeMaxScroll);

        float baseY  = startCY + 10f - themeScrollY;

        float headerFixedY = startCY + 10f;
        Render2D.drawFont(ctx.getMatrices(), Fonts.BOLD.getFont(7f),
                "Статичные",
                colX1, headerFixedY + (THEME_HEADER_H - Fonts.BOLD.getHeight(7f)) / 2f,
                new Color(180, 180, 200, (int)(200 * uiAlpha)));
        Render2D.drawRect(ctx.getMatrices(),
                colX1, headerFixedY + THEME_HEADER_H - 1f,
                THEME_CELL_W, 1f,
                new Color(255, 255, 255, (int)(18 * uiAlpha)));

        Render2D.drawFont(ctx.getMatrices(), Fonts.BOLD.getFont(7f),
                "Переливающиеся",
                colX2, headerFixedY + (THEME_HEADER_H - Fonts.BOLD.getHeight(7f)) / 2f,
                new Color(180, 180, 200, (int)(200 * uiAlpha)));
        Render2D.drawRect(ctx.getMatrices(),
                colX2, headerFixedY + THEME_HEADER_H - 1f,
                THEME_CELL_W, 1f,
                new Color(255, 255, 255, (int)(18 * uiAlpha)));

        float listClipY = headerFixedY + THEME_HEADER_H + 2f;
        float listClipH = clipH - (listClipY - startCY) - 8f;
        Render2D.startScissor(ctx, clipX, listClipY, clipW, listClipH);

        float cellY = baseY + THEME_HEADER_H + 4f;
        for (ThemeManager.Theme t : staticThemes) {
            renderThemeCell(ctx, mouseX, mouseY, t, colX1, cellY, currentName);
            cellY += THEME_CELL_H + THEME_GAP;
        }

        cellY = baseY + THEME_HEADER_H + 4f;
        for (ThemeManager.Theme t : gradientThemes) {
            renderThemeCell(ctx, mouseX, mouseY, t, colX2, cellY, currentName);
            cellY += THEME_CELL_H + THEME_GAP;
        }

        Render2D.stopScissor(ctx);

        renderScrollbar(ctx, startCX + clipW - 3f, listClipY, listClipH,
                themeScrollY, themeMaxScroll, uiAlpha);
    }

    private void renderThemeCell(DrawContext ctx, int mouseX, int mouseY,
                                 ThemeManager.Theme t, float tx, float ty, String currentName) {
        boolean active  = t.getName().equals(currentName);
        boolean hovered = mouseX >= tx && mouseX <= tx + THEME_CELL_W
                && mouseY >= ty && mouseY <= ty + THEME_CELL_H;
        Color accent = t.getAccentColor();

        int bgA = (int)((active ? 160 : hovered ? 70 : 40) * uiAlpha);
        Render2D.drawRoundedRect(ctx.getMatrices(), tx, ty, THEME_CELL_W, THEME_CELL_H, 7f,
                new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), bgA));

        if (active) {
            Render2D.drawRoundedRect(ctx.getMatrices(),
                    tx - 1.5f, ty - 1.5f, THEME_CELL_W + 3f, THEME_CELL_H + 3f, 8.5f,
                    new Color(accent.getRed(), accent.getGreen(), accent.getBlue(),
                            (int)(140 * uiAlpha)));
        }

        float circleSize = 12f;
        float circleX = tx + 10f;
        float circleY = ty + (THEME_CELL_H - circleSize) / 2f;
        Render2D.drawRoundedRect(ctx.getMatrices(),
                circleX, circleY, circleSize, circleSize, circleSize / 2f,
                new Color(accent.getRed(), accent.getGreen(), accent.getBlue(),
                        (int)(255 * uiAlpha)));

        String label = t.getName();
        Render2D.drawFont(ctx.getMatrices(), Fonts.BOLD.getFont(7.5f), label,
                tx + 28f, ty + (THEME_CELL_H - Fonts.BOLD.getHeight(7.5f)) / 2f,
                new Color(255, 255, 255, (int)(230 * uiAlpha)));
    }

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

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (closing) return false;

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
                    themeScrollY       = 0f;
                    themeScrollYTarget = 0f;
                    activeSettings = null;
                    searchQuery    = "";
                    searchFocused  = false;
                }
                selectedCategory = TABS[i];
                return true;
            }
        }

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

        if (selectedCategory == Category.Theme) {
            float pad    = 12f;
            float colX1  = panelX + SIDEBAR_W + pad;
            float colX2  = colX1 + THEME_CELL_W + THEME_COL_GAP;
            float startY = panelY + 10f + THEME_HEADER_H + 4f - themeScrollY;
            float clipTop = panelY + 10f + THEME_HEADER_H + 2f;
            float clipBot = panelY + PANEL_H - 8f;

            ThemeManager.Theme[] staticThemes = themeManager.getStaticThemes();
            float cellY = startY;
            for (ThemeManager.Theme t : staticThemes) {
                if (my >= clipTop && my <= clipBot
                        && mx >= colX1 && mx <= colX1 + THEME_CELL_W
                        && my >= cellY && my <= cellY + THEME_CELL_H) {
                    themeManager.setTheme(t);
                    return true;
                }
                cellY += THEME_CELL_H + THEME_GAP;
            }

            ThemeManager.Theme[] gradientThemes = themeManager.getGradientThemes();
            cellY = startY;
            for (ThemeManager.Theme t : gradientThemes) {
                if (my >= clipTop && my <= clipBot
                        && mx >= colX2 && mx <= colX2 + THEME_CELL_W
                        && my >= cellY && my <= cellY + THEME_CELL_H) {
                    themeManager.setTheme(t);
                    return true;
                }
                cellY += THEME_CELL_H + THEME_GAP;
            }

            return true;
        }

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

        if (selectedCategory == Category.Theme) {
            float themeAreaX = panelX + SIDEBAR_W;
            float themeAreaW = CONTENT_W;
            if (mx >= themeAreaX && mx <= themeAreaX + themeAreaW
                    && my >= panelY && my <= panelY + PANEL_H) {
                themeScrollYTarget = clamp(themeScrollYTarget + step, 0f, themeMaxScroll);
                return true;
            }
        }

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

    private static float clamp(float v, float lo, float hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}