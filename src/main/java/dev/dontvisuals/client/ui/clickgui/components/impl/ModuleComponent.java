package dev.dontvisuals.client.ui.clickgui.components.impl;

import dev.dontvisuals.modules.settings.impl.*;
import dev.dontvisuals.client.ui.clickgui.components.Component;
import dev.dontvisuals.modules.api.Module;
import dev.dontvisuals.modules.settings.Setting;
import dev.dontvisuals.modules.settings.api.Bind;
import dev.dontvisuals.client.util.animations.Animation;
import dev.dontvisuals.client.util.animations.Easing;
import dev.dontvisuals.client.util.math.MathUtils;
import dev.dontvisuals.client.util.renderer.fonts.Fonts;
import dev.dontvisuals.client.util.renderer.Render2D;
import lombok.Getter;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import dev.dontvisuals.client.managers.ThemeManager;
import net.minecraft.client.resource.language.I18n;

public class ModuleComponent extends Component {

    private final Module module;
    private boolean binding;
    private boolean showBindModeMenu;
    private boolean renderExternally;

    private float bindTextX = Float.NaN, bindTextY = Float.NaN;
    private float bindTextW = 0f, bindTextH = 0f;

    @Getter private final List<Component> components = new ArrayList<>();

    private final Animation hoverAnim    = new Animation(180, 1f, false, Easing.BOTH_SINE);
    private final Animation bindMenuAnim = new Animation(180, 1f, false, Easing.BOTH_SINE);

    // toggleAnim инициализируется в конструкторе с реальным состоянием модуля
    private final Animation toggleAnim;

    private boolean hoverAnimInit   = false;
    private boolean lastToggleState = false;

    private static final float H = 36f;

    public ModuleComponent(Module module) {
        super(module.getName());
        this.module          = module;
        this.lastToggleState = module.isToggled();

        // ── ФИКС: сразу ставим анимацию в реальное состояние модуля ──────────
        // Создаём с duration=0, обновляем до нужного значения, потом ставим нормальный duration
        this.toggleAnim = new Animation(0, 1f, false, Easing.BOTH_SINE);
        this.toggleAnim.update(module.isToggled());
        this.toggleAnim.setDuration(200);
        // ─────────────────────────────────────────────────────────────────────

        for (Setting<?> s : module.getSettings()) {
            if      (s instanceof BooleanSetting) components.add(new BooleanComponent((BooleanSetting) s));
            else if (s instanceof NumberSetting)  components.add(new SliderComponent((NumberSetting) s));
            else if (s instanceof EnumSetting)    components.add(new EnumComponent((EnumSetting<?>) s));
            else if (s instanceof StringSetting)  components.add(new StringComponent((StringSetting) s));
            else if (s instanceof ListSetting)    components.add(new ListComponent((ListSetting) s));
            else if (s instanceof BindSetting)    components.add(new BindComponent((BindSetting) s));
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  RENDER
    // ════════════════════════════════════════════════════════════════════════
    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        float ga = Math.max(0f, Math.min(1f, globalAlpha));

        // Hover
        boolean hovered = MathUtils.isHovered(x, y, width, H, mouseX, mouseY);
        if (!hoverAnimInit) {
            hoverAnim.setDuration(0);
            hoverAnim.update(hovered);
            hoverAnim.setDuration(180);
            hoverAnimInit = true;
        } else {
            hoverAnim.update(hovered);
        }

        // Toggle — просто следим за изменениями, инициализация уже в конструкторе
        boolean cur = module.isToggled();
        if (cur != lastToggleState) {
            toggleAnim.update(cur);
            lastToggleState = cur;
        }

        float ha = (float) hoverAnim.getValue();
        float ta = (float) toggleAnim.getValue();

        // ── Фон строки ──────────────────────────────────────────────────────
        int rowBgA = (int) ((16 + 10 * ha) * ga);
        Render2D.drawRoundedRect(ctx.getMatrices(), x, y, width, H, 6f,
                new Color(255, 255, 255, rowBgA));

        // ── Название модуля ──────────────────────────────────────────────────
        int nameA = (int) ((180 + 75 * ha) * ga);
        String name = I18n.translate(module.getName());
        Render2D.drawFont(ctx.getMatrices(), Fonts.BOLD.getFont(8.5f),
                name, x + 14f, y + (H - Fonts.BOLD.getHeight(8.5f)) / 2f,
                new Color(255, 255, 255, nameA));

        // ── Текст бинда (под названием, маленький) ───────────────────────────
        String bindStr = "";
        if (binding) bindStr = "Press key...";
        else if (module.getBind() != null && module.getBind().getKey() != -1)
            bindStr = module.getBind().toString().replace("_", " ");

        if (!bindStr.isEmpty()) {
            float bfs = 6f;
            bindTextW = Fonts.REGULAR.getWidth(bindStr, bfs);
            bindTextH = Fonts.REGULAR.getHeight(bfs);
            bindTextX = x + 14f;
            bindTextY = y + H - bindTextH - 4f;
            Render2D.drawFont(ctx.getMatrices(), Fonts.REGULAR.getFont(bfs),
                    bindStr, bindTextX, bindTextY,
                    new Color(140, 140, 160, (int) (180 * ga)));
        } else {
            bindTextX = Float.NaN;
            bindTextY = Float.NaN;
        }

        // ── Toggle переключатель (справа) ────────────────────────────────────
        float sw = 28f, sh = 14f;
        float sx = x + width - sw - 14f;
        float sy = y + (H - sh) / 2f;

        Color accent = ThemeManager.getInstance().getCurrentTheme().getAccentColor();

        // Трек
        Color trackOff = new Color(40, 40, 55, (int) (200 * ga));
        Color trackOn  = new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), (int) (220 * ga));
        int tr  = (int) (trackOff.getRed()   + (trackOn.getRed()   - trackOff.getRed())   * ta);
        int tg  = (int) (trackOff.getGreen() + (trackOn.getGreen() - trackOff.getGreen()) * ta);
        int tb  = (int) (trackOff.getBlue()  + (trackOn.getBlue()  - trackOff.getBlue())  * ta);
        int ta2 = (int) (trackOff.getAlpha() + (trackOn.getAlpha() - trackOff.getAlpha()) * ta);
        Render2D.drawRoundedRect(ctx.getMatrices(), sx, sy, sw, sh, sh / 2f,
                new Color(tr, tg, tb, ta2));

        // Ползунок
        float pad    = 2.5f;
        float th2    = sh - pad * 2;
        float travel = sw - th2 - pad * 2;
        float tx2    = sx + pad + travel * ta;
        Render2D.drawRoundedRect(ctx.getMatrices(), tx2, sy + pad, th2, th2, th2 / 2f,
                new Color(255, 255, 255, (int) (240 * ga)));

        // ── Контекстное меню режима бинда ────────────────────────────────────
        bindMenuAnim.update(showBindModeMenu);
        if (bindMenuAnim.getValue() > 0.01f) {
            renderBindModeMenu(ctx, ga);
        }
    }

    private void renderBindModeMenu(DrawContext ctx, float ga) {
        float a       = (float) Math.min(1f, bindMenuAnim.getValue() * ga);
        float itemW   = 82f;
        float itemH   = 16f;
        float menuH   = itemH * 2 + 8f;
        float menuX   = x + width - itemW - 16f;
        float slideOff = (1f - (float) bindMenuAnim.getValue()) * 6f;
        float menuY   = (Float.isNaN(bindTextY) ? y + H + 2f : bindTextY - menuH - 3f) + slideOff;

        Render2D.drawRoundedRect(ctx.getMatrices(), menuX, menuY, itemW, menuH, 4f,
                new Color(20, 20, 28, (int) (230 * a)));

        Color accent  = ThemeManager.getInstance().getCurrentTheme().getAccentColor();
        boolean isToggle = module.getBind() != null && module.getBind().getMode() == Bind.Mode.TOGGLE;
        boolean isHold   = module.getBind() != null && module.getBind().getMode() == Bind.Mode.HOLD;
        int textA = (int) (255 * a);

        if (isToggle) {
            Render2D.drawRoundedRect(ctx.getMatrices(), menuX + 3f, menuY + 3f, itemW - 6f, itemH, 3f,
                    new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), (int) (80 * a)));
        }
        Render2D.drawFont(ctx.getMatrices(), Fonts.BOLD.getFont(7.5f),
                "Toggle", menuX + 10f, menuY + 4f,
                new Color(255, 255, 255, textA));

        if (isHold) {
            Render2D.drawRoundedRect(ctx.getMatrices(), menuX + 3f, menuY + 3f + itemH, itemW - 6f, itemH, 3f,
                    new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), (int) (80 * a)));
        }
        Render2D.drawFont(ctx.getMatrices(), Fonts.BOLD.getFont(7.5f),
                "Hold", menuX + 10f, menuY + 4f + itemH,
                new Color(255, 255, 255, textA));
    }

    // ════════════════════════════════════════════════════════════════════════
    //  MOUSE / KEY
    // ════════════════════════════════════════════════════════════════════════
    @Override
    public void mouseClicked(double mx, double my, int btn) {
        boolean onHeader   = MathUtils.isHovered(x, y, width, H, (float) mx, (float) my);
        boolean onBindText = !Float.isNaN(bindTextX) &&
                MathUtils.isHovered(bindTextX, bindTextY - 1f, bindTextW, bindTextH + 2f, (float) mx, (float) my);

        if (btn == 0 && onBindText && !binding
                && module.getBind() != null && !module.getBind().isEmpty()) {
            showBindModeMenu = !showBindModeMenu;
            return;
        }

        if (showBindModeMenu) {
            float itemW = 82f, itemH = 16f, menuH = itemH * 2 + 8f;
            float menuX = x + width - itemW - 16f;
            float menuY = Float.isNaN(bindTextY) ? y + H + 2f : bindTextY - menuH - 3f;
            if (MathUtils.isHovered(menuX, menuY + 3f, itemW - 6f, itemH, (float) mx, (float) my)) {
                module.getBind().setMode(Bind.Mode.TOGGLE);
                showBindModeMenu = false;
                return;
            }
            if (MathUtils.isHovered(menuX, menuY + 3f + itemH, itemW - 6f, itemH, (float) mx, (float) my)) {
                module.getBind().setMode(Bind.Mode.HOLD);
                showBindModeMenu = false;
                return;
            }
            showBindModeMenu = false;
            return;
        }

        if (onHeader) {
            if (btn == 0 && !binding) { module.toggle(); return; }
            if (btn == 2 && !binding) { binding = true;  return; }
        }
    }

    @Override
    public void mouseReleased(double mx, double my, int btn) {}

    @Override
    public void keyPressed(int key, int scan, int mods) {
        if (binding) {
            if (key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_DELETE) {
                module.setBind(new Bind(-1, false));
            } else {
                Bind.Mode mode = module.getBind() != null ? module.getBind().getMode() : Bind.Mode.TOGGLE;
                module.setBind(new Bind(key, false, mode));
            }
            binding = false;
        }
    }

    @Override public void keyReleased(int k, int s, int m) {}
    @Override public void charTyped(char c, int m) {}

    @Override
    public float getHeight() { return H; }

    public float renderSettingsExternally(DrawContext ctx, float cX, float cY, float cW,
                                          float clipX, float clipY, float clipW, float clipH,
                                          int mx, int my, float delta, float scrollY) {
        float curY  = cY - scrollY;
        float total = 0f;
        ctx.enableScissor((int) clipX, (int) clipY, (int) (clipX + clipW), (int) (clipY + clipH));
        for (Component c : components) {
            if (!c.getVisible().get()) continue;
            c.setX(cX);
            c.setY(curY);
            c.setWidth(cW);
            c.setHeight(20f);
            c.setGlobalAlpha(globalAlpha);
            c.render(ctx, mx, my, delta);
            float h = c.getHeight() + c.getAddHeight().get();
            curY  += h;
            total += h;
        }
        ctx.disableScissor();
        return total;
    }

    public void mouseClickedExternal(double mx, double my, int btn) {
        for (Component c : components) c.mouseClicked(mx, my, btn);
    }
    public void mouseReleasedExternal(double mx, double my, int btn) {
        for (Component c : components) c.mouseReleased(mx, my, btn);
    }
    public void keyPressedExternal(int key, int scan, int mods) {
        for (Component c : components) c.keyPressed(key, scan, mods);
    }
    public void keyReleasedExternal(int key, int scan, int mods) {
        for (Component c : components) c.keyReleased(key, scan, mods);
    }
    public void charTypedExternal(char chr, int mods) {
        for (Component c : components) c.charTyped(chr, mods);
    }

    public Module  getModule()          { return module; }
    public boolean isBindModeMenuOpen() { return showBindModeMenu; }
    public void    setRenderExternally(boolean v) { this.renderExternally = v; }
    public boolean isRenderExternally() { return renderExternally; }

    public float getChildrenFullHeight() {
        float h = 0f;
        for (Component c : components) {
            if (!c.getVisible().get()) continue;
            h += 20f + c.getAddHeight().get();
        }
        return h;
    }
}