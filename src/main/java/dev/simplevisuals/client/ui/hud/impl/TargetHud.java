package dev.simplevisuals.client.ui.hud.impl;

import dev.simplevisuals.client.events.impl.EventRender2D;
import dev.simplevisuals.client.ui.hud.HudElement;
import dev.simplevisuals.client.util.Network.Server;
import dev.simplevisuals.client.util.animations.Easing;
import dev.simplevisuals.client.util.animations.infinity.InfinityAnimation;
import dev.simplevisuals.client.util.math.MathUtils;
import dev.simplevisuals.client.util.renderer.Render2D;
import dev.simplevisuals.client.util.renderer.fonts.Fonts;
import dev.simplevisuals.client.managers.ThemeManager;
import dev.simplevisuals.modules.settings.impl.BooleanSetting;
import dev.simplevisuals.modules.impl.utility.NameProtect;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.util.Identifier;
import dev.simplevisuals.simplevisuals;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class TargetHud extends HudElement implements ThemeManager.ThemeChangeListener {

    private final ThemeManager themeManager;

    // Settings
    private final BooleanSetting displayAbsorption  = new BooleanSetting("displayAbsorption", true);
    private final BooleanSetting displayHudParticles = new BooleanSetting("hudParticles", true);
    private final BooleanSetting showHealthText      = new BooleanSetting("showHealthText", true);

    public TargetHud() {
        super("TargetHud");
        this.themeManager = ThemeManager.getInstance();
        themeManager.addThemeChangeListener(this);
        getSettings().add(displayAbsorption);
        getSettings().add(displayHudParticles);
        getSettings().add(showHealthText);
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
    private int          prevHurtTime = 0;
    private float        previousHp01 = -1f;
    private long         lastFrameMs  = System.currentTimeMillis();

    private static final long HUD_DURATION = 2000L;
    private static final Identifier STAR_TEX = simplevisuals.id("hud/star.png");

    // ─── Размеры карточки ────────────────────────────────────────────────────
    // Карточка: широкая, с аватаром слева, акцентной полосой, информацией справа
    private static final float W         = 138f;
    private static final float H         = 44f;
    private static final float ROUNDING  = 9f;
    private static final float AVATAR    = 30f;   // размер аватара
    private static final float PAD       = 5f;
    private static final float ACCENT_W  = 3f;    // толщина левой акцентной полосы

    // Частицы
    private final List<HudParticle> particles = new ArrayList<>();

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
        float dt = (now - lastFrameMs) / 1000f;
        lastFrameMs = now;

        // Обновляем цель
        if (mc.crosshairTarget != null && mc.crosshairTarget.getType() == HitResult.Type.ENTITY) {
            EntityHitResult hit = (EntityHitResult) mc.crosshairTarget;
            if (hit.getEntity() instanceof LivingEntity living && living.isAlive()) {
                Vec3d center = entityCenter(living, e);
                boolean invisible = living.hasStatusEffect(StatusEffects.INVISIBILITY) || living.isInvisible();
                if (!invisible && !isOccluded(mc.player.getCameraPosVec(e.getTickDelta()), center)) {
                    if (lastTarget != living) { animDirX = (float)(Math.random()*2-1); animDirY = (float)(Math.random()*2-1); prevHurtTime = 0; }
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
            if (forceFade && !preview) { forceFade = false; lastKnownCenter = null; previousHp01 = -1f; particles.clear(); prevHurtTime = 0; }
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

        // ── 1. Многослойный фон ──────────────────────────────────────────────

        // Самый дальний слой — едва заметная тень/свечение от акцента
        Render2D.drawRoundedRect(e.getContext().getMatrices(),
                posX - 2f, posY - 2f, W + 4f, H + 4f, ROUNDING + 2f,
                withAlpha(accent, (int)(18 * fadeAnim.getValue())));

        // Основной фон — тёмное стекло
        Render2D.drawRoundedRect(e.getContext().getMatrices(),
                posX, posY, W, H, ROUNDING,
                new Color(12, 12, 16, alpha));

        // Внутренний слой — чуть светлее сверху (объём)
        Render2D.drawRoundedRect(e.getContext().getMatrices(),
                posX + 1f, posY + 1f, W - 2f, H / 2.2f, ROUNDING - 1f,
                new Color(255, 255, 255, (int)(10 * fadeAnim.getValue())));

        // Тонкая внешняя рамка
        Render2D.drawRoundedRect(e.getContext().getMatrices(),
                posX, posY, W, H, ROUNDING,
                withAlpha(accent, (int)(45 * fadeAnim.getValue())));
        Render2D.drawRoundedRect(e.getContext().getMatrices(),
                posX + 0.8f, posY + 0.8f, W - 1.6f, H - 1.6f, ROUNDING - 0.8f,
                new Color(12, 12, 16, alpha));

        // ── 2. Акцентная полоса слева ────────────────────────────────────────
        // Градиент: яркий акцент сверху → тёмный акцент снизу
        // Рисуем двумя прямоугольниками с разной прозрачностью (имитация градиента)
        float accentX = posX + PAD / 2f;
        float accentY = posY + ROUNDING / 1.5f;
        float accentH = H - ROUNDING * 1.3f;

        Render2D.drawRoundedRect(e.getContext().getMatrices(),
                accentX, accentY, ACCENT_W, accentH, ACCENT_W / 2f,
                withAlpha(accent, (int)(255 * fadeAnim.getValue())));
        // Мягкое свечение вокруг полосы
        Render2D.drawRoundedRect(e.getContext().getMatrices(),
                accentX - 1.5f, accentY - 1f, ACCENT_W + 3f, accentH + 2f, ACCENT_W,
                withAlpha(accent, (int)(40 * fadeAnim.getValue())));

        // ── 3. Аватар ────────────────────────────────────────────────────────
        float avatarX = posX + PAD + ACCENT_W + PAD;
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
            // Для моба — логотип DV
            // Сначала тень
            Render2D.drawFont(e.getContext().getMatrices(), Fonts.BOLD.getFont(9f), "DV",
                    avatarX + AVATAR/2f - Fonts.BOLD.getWidth("DV", 9f)/2f + 0.6f,
                    avatarY + AVATAR/2f - Fonts.BOLD.getHeight(9f)/2f + 0.6f,
                    new Color(0, 0, 0, (int)(140 * fadeAnim.getValue())));
            // Основной цвет — акцент
            Render2D.drawFont(e.getContext().getMatrices(), Fonts.BOLD.getFont(9f), "DV",
                    avatarX + AVATAR/2f - Fonts.BOLD.getWidth("DV", 9f)/2f,
                    avatarY + AVATAR/2f - Fonts.BOLD.getHeight(9f)/2f,
                    withAlpha(accent, alpha));
        }

        // ── 4. Имя ───────────────────────────────────────────────────────────
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

        // Тень имени
        if (!nameDraw.isEmpty()) {
            Render2D.drawFont(e.getContext().getMatrices(), Fonts.BOLD.getFont(8.5f),
                    nameDraw, infoX + 0.5f, posY + PAD + 1.5f,
                    new Color(0, 0, 0, (int)(120 * fadeAnim.getValue())));
            Render2D.drawFont(e.getContext().getMatrices(), Fonts.BOLD.getFont(8.5f),
                    nameDraw, infoX, posY + PAD + 1f,
                    new Color(255, 255, 255, alpha));
        }

        // ── 5. HP текст ──────────────────────────────────────────────────────
        if (showHealthText.getValue()) {
            String hpStr = (int) rawHp + " / " + (int) maxHp;
            float hpTxtY = posY + PAD + 1f + Fonts.BOLD.getHeight(8.5f) + 3f;
            Render2D.drawFont(e.getContext().getMatrices(), Fonts.MEDIUM.getFont(7.5f),
                    hpStr, infoX, hpTxtY,
                    new Color(180, 180, 195, (int)(180 * fadeAnim.getValue())));

            // % с акцентным цветом справа
            int pct = (int) Math.round(hp01 * 100.0);
            String pctStr = pct + "%";
            float pctW = Fonts.BOLD.getWidth(pctStr, 7.5f);
            Render2D.drawFont(e.getContext().getMatrices(), Fonts.BOLD.getFont(7.5f),
                    pctStr, posX + W - PAD - pctW, hpTxtY,
                    withAlpha(accent, (int)(220 * fadeAnim.getValue())));
        }

        // ── 6. HP бар — тройной слой ─────────────────────────────────────────
        float barX = infoX;
        float barY = posY + H - PAD - 7f;
        float barW = W - PAD - (infoX - posX) - PAD;
        float barH = 5.5f;

        // Track (тёмный фон бара)
        Render2D.drawRoundedRect(e.getContext().getMatrices(),
                barX, barY, barW, barH, barH / 2f,
                new Color(6, 6, 10, (int)(220 * fadeAnim.getValue())));

        // Тонкий ободок трека
        Render2D.drawRoundedRect(e.getContext().getMatrices(),
                barX - 0.5f, barY - 0.5f, barW + 1f, barH + 1f, barH / 2f + 0.5f,
                new Color(255, 255, 255, (int)(15 * fadeAnim.getValue())));
        Render2D.drawRoundedRect(e.getContext().getMatrices(),
                barX, barY, barW, barH, barH / 2f,
                new Color(6, 6, 10, (int)(220 * fadeAnim.getValue())));

        // Анимированное заполнение HP
        float hpTargetPx = barW * hp01;
        float hpPx = Math.max(0f, Math.min(barW, hpAnim.animate(hpTargetPx, 100)));

        if (hpPx > 0.5f) {
            // Основной цвет — акцент
            Render2D.drawRoundedRect(e.getContext().getMatrices(),
                    barX, barY, hpPx, barH, Math.min(barH / 2f, hpPx / 2f),
                    withAlpha(accent, (int)(230 * fadeAnim.getValue())));

            // Блик сверху на заполненной части
            Render2D.drawRoundedRect(e.getContext().getMatrices(),
                    barX, barY, hpPx, barH * 0.45f, Math.min(barH / 2f, hpPx / 2f),
                    new Color(255, 255, 255, (int)(35 * fadeAnim.getValue())));
        }

        // Absorption bar (поверх HP, полупрозрачный жёлтый)
        if (displayAbsorption.getValue() && absorb > 0f) {
            float absPx = Math.max(0f, Math.min(barW, absAnim.animate(barW * abs01, 120)));
            if (absPx > 0.5f) {
                Render2D.drawRoundedRect(e.getContext().getMatrices(),
                        barX, barY, absPx, barH, Math.min(barH / 2f, absPx / 2f),
                        new Color(255, 190, 40, (int)(180 * fadeAnim.getValue())));
            }
        }

        // ── 7. Частицы при уроне ─────────────────────────────────────────────
        if (!preview && lastTarget != null && displayHudParticles.getValue()) {
            if (lastTarget.hurtTime > prevHurtTime) {
                spawnParticles(avatarX, avatarY, AVATAR);
            }
            prevHurtTime = lastTarget.hurtTime;
            previousHp01 = hp01;
        } else {
            previousHp01 = -1f;
            prevHurtTime = 0;
        }

        if (!displayHudParticles.getValue()) { particles.clear(); }
        if (!particles.isEmpty()) {
            Color base = themeManager.getCurrentTheme().getAccentColor();
            particles.removeIf(p -> p.tick(dt));
            for (HudParticle p : particles) {
                int a = (int)(Math.max(0f, p.alpha) * 255);
                if (a <= 2) continue;
                Render2D.drawTexture(
                        e.getContext().getMatrices(),
                        p.x - p.size/2f, p.y - p.size/2f, p.size, p.size, 0f,
                        STAR_TEX,
                        new Color(base.getRed(), base.getGreen(), base.getBlue(), a)
                );
            }
        }

        e.getContext().getMatrices().pop();
        super.onRender2D(e);
    }

    private void spawnParticles(float hx, float hy, float hs) {
        int count = 20;
        float cx = hx + hs / 2f;
        float cy = hy + hs / 2f;
        for (int i = 0; i < count; i++) {
            int edge = i % 4;
            float t = (float) Math.random();
            float sx, sy;
            if      (edge == 0) { sx = hx + t * hs; sy = hy; }
            else if (edge == 1) { sx = hx + hs;     sy = hy + t * hs; }
            else if (edge == 2) { sx = hx + t * hs; sy = hy + hs; }
            else                { sx = hx;           sy = hy + t * hs; }

            float angle = (float) Math.atan2(sy - cy, sx - cx)
                    + (float)((Math.random() - 0.5) * Math.toRadians(60));
            float speed = 8f + (float)(Math.random() * 10f);
            particles.add(new HudParticle(sx, sy,
                    (float)(Math.cos(angle) * speed),
                    (float)(Math.sin(angle) * speed),
                    4.5f + (float)(Math.random() * 3f),
                    1100 + (int)(Math.random() * 900)));
        }
    }

    // ─── Частица ─────────────────────────────────────────────────────────────
    private static class HudParticle {
        float x, y, vx, vy, size, alpha = 1f;
        long lifeMs, ageMs;

        HudParticle(float x, float y, float vx, float vy, float size, long lifeMs) {
            this.x = x; this.y = y; this.vx = vx; this.vy = vy;
            this.size = size; this.lifeMs = lifeMs;
        }

        boolean tick(float dt) {
            ageMs += (long)(dt * 1000f);
            float factor = (float) Math.pow(0.87, dt);
            vx *= factor; vy *= factor;
            x += vx * dt; y += vy * dt;
            float t = Math.max(0f, Math.min(1f, ageMs / (float) lifeMs));
            alpha = 1f - t;
            return ageMs >= lifeMs || alpha <= 0f;
        }
    }
}