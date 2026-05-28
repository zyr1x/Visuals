package dev.dontvisuals.client.ui.hud.impl;

import dev.dontvisuals.client.events.impl.EventRender2D;
import dev.dontvisuals.client.ui.hud.HudElement;
import dev.dontvisuals.client.util.Network.Server;
import dev.dontvisuals.client.util.animations.Easing;
import dev.dontvisuals.client.util.animations.infinity.InfinityAnimation;
import dev.dontvisuals.client.util.math.MathUtils;
import dev.dontvisuals.client.util.renderer.Render2D;
import dev.dontvisuals.client.util.renderer.fonts.Fonts;
import dev.dontvisuals.client.managers.ThemeManager;
import dev.dontvisuals.modules.impl.utility.NameProtect;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.awt.*;

public class TargetHud extends HudElement implements ThemeManager.ThemeChangeListener {

    private final ThemeManager themeManager;

    public TargetHud() {
        super("TargetHud");
        this.themeManager = ThemeManager.getInstance();
        themeManager.addThemeChangeListener(this);
    }

    @Override public void onThemeChanged(ThemeManager.Theme theme) {}
    @Override public void onDisable() { themeManager.removeThemeChangeListener(this); super.onDisable(); }

    // ─── Анимации ────────────────────────────────────────────────────────────
    private final InfinityAnimation fadeAnim  = new InfinityAnimation(Easing.OUT_QUAD);
    private final InfinityAnimation scaleAnim = new InfinityAnimation(Easing.OUT_QUAD);
    private final InfinityAnimation slideAnim = new InfinityAnimation(Easing.OUT_QUAD);
    private final InfinityAnimation hpAnim    = new InfinityAnimation(Easing.BOTH_SINE);
    private final InfinityAnimation absAnim   = new InfinityAnimation(Easing.BOTH_SINE);

    // ─── Состояние ───────────────────────────────────────────────────────────
    private LivingEntity lastTarget   = null;
    private long         lastSeenTime = 0L;
    private boolean      forceFade    = false;
    private Vec3d        lastKnownCenter = null;
    private float        animDirX     = 1f;
    private float        animDirY     = 1f;
    private long         lastFrameMs  = System.currentTimeMillis();

    private static final long HUD_DURATION = 2000L;

    // ─── Размеры карточки ────────────────────────────────────────────────────
    private static final float W        = 110f;
    private static final float H        = 36f;
    private static final float ROUNDING = 7f;
    private static final float AVATAR   = 24f;
    private static final float PAD      = 4f;

    // ─── Утилиты ─────────────────────────────────────────────────────────────
    private Vec3d entityCenter(LivingEntity e, EventRender2D ev) {
        return e.getLerpedPos(ev.getTickDelta()).add(0, e.getHeight() * 0.5, 0);
    }
    private boolean isOccluded(Vec3d from, Vec3d to) {
        HitResult hr = mc.world.raycast(new RaycastContext(from, to,
                RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player));
        return hr.getType() != HitResult.Type.MISS;
    }
    private static Color withAlpha(Color c, int a) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, a)));
    }
    private static Color darken(Color c, float factor) {
        return new Color(
                Math.max(0, (int)(c.getRed()   * factor)),
                Math.max(0, (int)(c.getGreen() * factor)),
                Math.max(0, (int)(c.getBlue()  * factor)),
                c.getAlpha()
        );
    }
    private String ellipsize(String text, float size, float maxW) {
        if (text == null || text.isEmpty()) return "";
        if (Fonts.BOLD.getWidth(text, size) <= maxW) return text;
        String dots = "...";
        float dw = Fonts.BOLD.getWidth(dots, size);
        if (dw > maxW) return "";
        int lo = 0, hi = text.length(); String best = "";
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            String c = text.substring(0, mid) + dots;
            if (Fonts.BOLD.getWidth(c, size) <= maxW) { best = c; lo = mid + 1; } else hi = mid - 1;
        }
        return best;
    }

    // ─── Главный рендер ──────────────────────────────────────────────────────
    @Override
    public void onRender2D(EventRender2D e) {
        if (fullNullCheck() || closed()) return;

        long now = System.currentTimeMillis();
        lastFrameMs = now;

        // Обновляем цель
        if (mc.crosshairTarget != null && mc.crosshairTarget.getType() == HitResult.Type.ENTITY) {
            EntityHitResult hit = (EntityHitResult) mc.crosshairTarget;
            if (hit.getEntity() instanceof LivingEntity living && living.isAlive()) {
                Vec3d center = entityCenter(living, e);
                boolean invisible = living.hasStatusEffect(StatusEffects.INVISIBILITY) || living.isInvisible();
                if (!invisible && !isOccluded(mc.player.getCameraPosVec(e.getTickDelta()), center)) {
                    if (lastTarget != living) { animDirX = (float)(Math.random()*2-1); animDirY = (float)(Math.random()*2-1); }
                    lastTarget = living; lastSeenTime = now; forceFade = false; lastKnownCenter = center;
                } else {
                    lastTarget = null; forceFade = true; lastKnownCenter = center;
                }
            }
        }
        if (lastTarget != null && (!lastTarget.isAlive() || now - lastSeenTime > HUD_DURATION)) {
            lastTarget = null; forceFade = true;
        }

        boolean chatOpen   = mc.currentScreen instanceof net.minecraft.client.gui.screen.ChatScreen;
        boolean preview    = chatOpen && lastTarget == null;
        LivingEntity shown = preview ? mc.player : lastTarget;
        boolean shouldShow = shown != null && !forceFade;

        fadeAnim.animate(shouldShow ? 1f : 0f, 200);
        scaleAnim.animate(shouldShow ? 1f : 0.88f, 180);
        slideAnim.animate(shouldShow ? 0f : 1f, 220);

        if (fadeAnim.getValue() <= 0.01f) {
            if (forceFade && !preview) { forceFade = false; lastKnownCenter = null; }
            return;
        }
        if (shown == null) return;

        float posX = getX();
        float posY = getY();
        setBounds(posX, posY, W, H);

        int   alpha  = (int)(230 * fadeAnim.getValue());
        float scale  = scaleAnim.getValue() * toggledAnimation.getValue() * fadeAnim.getValue();
        float slideX = 18f * slideAnim.getValue() * animDirX;
        float slideY = 18f * slideAnim.getValue() * animDirY;

        float rawHp = MathUtils.round(Server.getHealth(shown, false));
        float maxHp = Math.max(1f, MathUtils.round(shown.getMaxHealth()));
        float absorb = Math.max(0f, MathUtils.round(shown.getAbsorptionAmount()));
        float hp01  = MathHelper.clamp(rawHp / maxHp, 0f, 1f);
        float abs01 = absorb > 0f ? MathHelper.clamp(absorb / maxHp, 0f, 1f) : 0f;

        e.getContext().getMatrices().push();
        e.getContext().getMatrices().translate(posX + W/2f + slideX, posY + H/2f + slideY, 0);
        e.getContext().getMatrices().scale(scale, scale, 1f);
        e.getContext().getMatrices().translate(-(posX + W/2f), -(posY + H/2f), 0);

        Color accent = themeManager.getCurrentTheme().getAccentColor();

        // ── 1. Фон ──────────────────────────────────────────────────────────


// Плавное рассеянное свечение
        Render2D.drawGlowOutline(e.getContext().getMatrices(),
                posX, posY, W, H, ROUNDING,
                withAlpha(accent, (int)(120 * fadeAnim.getValue())),
                (int)(80 * fadeAnim.getValue()),
                4f);

// Основной фон — тёмное стекло
        Render2D.drawRoundedRect(e.getContext().getMatrices(),
                posX, posY, W, H, ROUNDING,
                new Color(12, 12, 16, alpha));


        // ── 2. Аватар ────────────────────────────────────────────────────────
        float avatarX = posX + PAD + PAD;
        float avatarY = posY + (H - AVATAR) / 2f;

        // Тень под аватаром
        Render2D.drawRoundedRect(e.getContext().getMatrices(),
                avatarX - 1.5f, avatarY + 2f, AVATAR + 3f, AVATAR + 1f, 8f,
                new Color(0, 0, 0, (int)(80 * fadeAnim.getValue())));

        // Фон аватара
        Render2D.drawRoundedRect(e.getContext().getMatrices(),
                avatarX, avatarY, AVATAR, AVATAR, 7f,
                withAlpha(darken(accent, 0.3f), (int)(200 * fadeAnim.getValue())));

        // Акцентная рамка аватара
        Render2D.drawRoundedRect(e.getContext().getMatrices(),
                avatarX - 1f, avatarY - 1f, AVATAR + 2f, AVATAR + 2f, 8f,
                withAlpha(accent, (int)(120 * fadeAnim.getValue())));

        if (shown instanceof PlayerEntity player) {
            Render2D.drawTexture(
                    e.getContext().getMatrices(),
                    avatarX, avatarY, AVATAR, AVATAR, 7f,
                    0.125f, 0.125f, 0.125f, 0.125f,
                    ((AbstractClientPlayerEntity) player).getSkinTextures().texture(),
                    new Color(255, 255, 255, alpha)
            );
        } else {
            Render2D.drawFont(e.getContext().getMatrices(), Fonts.BOLD.getFont(9f), "DV",
                    avatarX + AVATAR/2f - Fonts.BOLD.getWidth("DV", 9f)/2f + 0.6f,
                    avatarY + AVATAR/2f - Fonts.BOLD.getHeight(9f)/2f + 0.6f,
                    new Color(0, 0, 0, (int)(140 * fadeAnim.getValue())));
            Render2D.drawFont(e.getContext().getMatrices(), Fonts.BOLD.getFont(9f), "DV",
                    avatarX + AVATAR/2f - Fonts.BOLD.getWidth("DV", 9f)/2f,
                    avatarY + AVATAR/2f - Fonts.BOLD.getHeight(9f)/2f,
                    withAlpha(accent, alpha));
        }

        // ── 3. Имя ───────────────────────────────────────────────────────────
        float infoX = avatarX + AVATAR + PAD + 2f;
        float infoW = posX + W - PAD - infoX;

        String name = shown.getName().getString();
        if (preview) {
            NameProtect np = NameProtect.getInstance();
            if (np != null && np.isToggled()) {
                String rep = np.getCustomName().getValue();
                name = (rep != null && !rep.isEmpty()) ? rep : "Protected";
            }
        }
        if (name == null || name.isEmpty()) name = "Unknown";
        String nameDraw = ellipsize(name, 8.5f, infoW);

        if (!nameDraw.isEmpty()) {
            Render2D.drawFont(e.getContext().getMatrices(), Fonts.BOLD.getFont(8.5f),
                    nameDraw, infoX + 0.5f, posY + PAD + 1.5f,
                    new Color(0, 0, 0, (int)(120 * fadeAnim.getValue())));
            Render2D.drawFont(e.getContext().getMatrices(), Fonts.BOLD.getFont(8.5f),
                    nameDraw, infoX, posY + PAD + 1f,
                    new Color(255, 255, 255, alpha));
        }

        // ── 4. HP текст ──────────────────────────────────────────────────────
        String hpStr = (int) rawHp + " / " + (int) maxHp;
        float hpTxtY = posY + PAD + 1f + Fonts.BOLD.getHeight(8.5f) + 1.5f;
        Render2D.drawFont(e.getContext().getMatrices(), Fonts.MEDIUM.getFont(7.5f),
                hpStr, infoX, hpTxtY,
                new Color(180, 180, 195, (int)(180 * fadeAnim.getValue())));

        int pct = (int) Math.round(hp01 * 100.0);
        String pctStr = pct + "%";
        float pctW = Fonts.BOLD.getWidth(pctStr, 7.5f);
        Render2D.drawFont(e.getContext().getMatrices(), Fonts.BOLD.getFont(7.5f),
                pctStr, posX + W - PAD - pctW, hpTxtY,
                withAlpha(accent, (int)(220 * fadeAnim.getValue())));

        // ── 5. HP бар ────────────────────────────────────────────────────────
        float barX = infoX;
        float barY = hpTxtY + Fonts.MEDIUM.getHeight(7.5f) + 1f;
        float barW = posX + W - PAD - barX;  // исправлено
        float barH = 5.5f;

        // Track
        Render2D.drawRoundedRect(e.getContext().getMatrices(),
                barX, barY, barW, barH, barH / 2f,
                new Color(6, 6, 10, (int)(220 * fadeAnim.getValue())));

        // Заполнение HP
        float hpPx = Math.max(0f, Math.min(barW, hpAnim.animate(barW * hp01, 100)));

        if (hpPx > 0.5f) {
            Render2D.drawRoundedRect(e.getContext().getMatrices(),
                    barX, barY, hpPx, barH, Math.min(barH / 2f, hpPx / 2f),
                    withAlpha(accent, (int)(230 * fadeAnim.getValue())));

            // Блик сверху
            Render2D.drawRoundedRect(e.getContext().getMatrices(),
                    barX, barY, hpPx, barH * 0.45f, Math.min(barH / 2f, hpPx / 2f),
                    new Color(255, 255, 255, (int)(35 * fadeAnim.getValue())));
        }

        // Absorption bar
        if (absorb > 0f) {
            float absPx = Math.max(0f, Math.min(barW, absAnim.animate(barW * abs01, 120)));
            if (absPx > 0.5f) {
                Render2D.drawRoundedRect(e.getContext().getMatrices(),
                        barX, barY, absPx, barH, Math.min(barH / 2f, absPx / 2f),
                        new Color(255, 190, 40, (int)(180 * fadeAnim.getValue())));
            }
        }

        e.getContext().getMatrices().pop();
        super.onRender2D(e);
    }
}