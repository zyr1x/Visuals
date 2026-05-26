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
import dev.simplevisuals.modules.settings.impl.ListSetting;
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
import java.util.List;
import java.util.ArrayList;

import java.awt.*;

public class TargetHud extends HudElement implements ThemeManager.ThemeChangeListener {

    private final ThemeManager themeManager;
    private Color bgColor;
    private Color textColor;
    private Color headerTextColor;
    private Color lowDurabilityColor;
    private Color absorbColor;

    // Settings
    private final BooleanSetting displayAbsorption = new BooleanSetting("displayAbsorption", true);
    private final BooleanSetting displayHudParticles = new BooleanSetting("hudParticles", true);
    private final ListSetting style;

    public TargetHud() {
        super("TargetHud");
        this.themeManager = ThemeManager.getInstance();
        applyTheme(themeManager.getCurrentTheme());
        themeManager.addThemeChangeListener(this);

        // Build ListSetting with internal options so individual booleans are not exposed as separate settings
        BooleanSetting optDefault = new BooleanSetting("targethud.style.default", true, () -> false);
        BooleanSetting optCard = new BooleanSetting("targethud.style.card", false, () -> false);
        this.style = new ListSetting("targethud.style", () -> true, true, optDefault, optCard).setSingleSelect(true);

        getSettings().add(displayAbsorption);
        getSettings().add(displayHudParticles);
        getSettings().add(style);
    }

    private void applyTheme(ThemeManager.Theme theme) {
        // Match Potions background (30,30,30,240)
        this.bgColor = new Color(30, 30, 30, 255);

        int brightness = (int) (0.299 * bgColor.getRed() + 0.587 * bgColor.getGreen() + 0.114 * bgColor.getBlue());
        if (brightness > 200 && bgColor.getAlpha() <= 150) {
            this.textColor = new Color(0, 0, 0, 255);
        } else {
            this.textColor = theme.getTextColor();
        }

        this.headerTextColor = this.textColor;
        this.lowDurabilityColor = new Color(200, 80, 80, 220);
        this.absorbColor = new Color(255, 190, 0, 255);
    }

    @Override
    public void onThemeChanged(ThemeManager.Theme theme) {
        applyTheme(theme);
    }

    @Override
    public void onDisable() {
        themeManager.removeThemeChangeListener(this);
        super.onDisable();
    }

    // Animations
    private final InfinityAnimation fadeAnimation = new InfinityAnimation(Easing.OUT_QUAD);
    private final InfinityAnimation scaleAnimation = new InfinityAnimation(Easing.OUT_QUAD);
    private final InfinityAnimation slideAnimation = new InfinityAnimation(Easing.OUT_QUAD);
    private final InfinityAnimation hpAnimPx = new InfinityAnimation(Easing.BOTH_SINE);
    private final InfinityAnimation absAnimPx = new InfinityAnimation(Easing.BOTH_SINE);

    private LivingEntity lastTarget = null;
    private long lastSeenTime = 0L;
    private static final long HUD_DURATION = 2000;
    private boolean forceFade = false;
    private Vec3d lastKnownCenter = null;

    private float animationDirectionX = 1f;
    private float animationDirectionY = 1f;
    private int lastHurtTicks = 0;
    private int prevHurtTime = 0;

    // Dimensions
    private static final float ROUNDING = 6f;
    private static final float SPACING = 5f;

    // HUD dimensions
    private static final float WIDTH = 122f;
    private static final float HEIGHT = 64f / 1.5f;

    // Head size (reduced)
    private static final float HEAD = 21.5f;

    // HUD star particles (reused from DamageParticles -> hud/star.png)
    private static final Identifier STAR_TEX = simplevisuals.id("hud/star.png");
    private final List<HudParticle> hudParticles = new ArrayList<>();
    private long lastFrameTimeMs = System.currentTimeMillis();
    private float previousHp01 = -1f;

    private Vec3d entityCenter(LivingEntity ent, EventRender2D e) {
        Vec3d lp = ent.getLerpedPos(e.getTickDelta());
        return lp.add(0, ent.getHeight() * 0.5, 0);
    }

    private static boolean isInvisibleAndUnrevealed(LivingEntity e) {
        return e.hasStatusEffect(StatusEffects.INVISIBILITY) || e.isInvisible();
    }

    private boolean isOccluded(Vec3d from, Vec3d to) {
        HitResult hr = mc.world.raycast(new RaycastContext(
                from, to,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                mc.player
        ));
        return hr.getType() != HitResult.Type.MISS;
    }

    private Color getHitColor(LivingEntity entity, int alpha) {
        return new Color(255, 255, 255, alpha); // стандартный цвет
    }

    @Override
    public void onRender2D(EventRender2D e) {
        if (fullNullCheck() || closed()) return;

        long now = System.currentTimeMillis();
        float dt = (now - lastFrameTimeMs) / 1000f;
        lastFrameTimeMs = now;

        if (mc.crosshairTarget != null && mc.crosshairTarget.getType() == HitResult.Type.ENTITY) {
            EntityHitResult hit = (EntityHitResult) mc.crosshairTarget;
            if (hit.getEntity() instanceof LivingEntity living && living.isAlive()) {
                Vec3d center = entityCenter(living, e);

                if (isInvisibleAndUnrevealed(living)) {
                } else if (isOccluded(mc.player.getCameraPosVec(e.getTickDelta()), center)) {
                    lastTarget = null;
                    forceFade = true;
                    lastKnownCenter = center;
                } else {
                    if (lastTarget == null) {
                        animationDirectionX = (float) (Math.random() * 2 - 1);
                        animationDirectionY = (float) (Math.random() * 2 - 1);
                    }
                    if (lastTarget != living) {
                        prevHurtTime = 0;
                    }
                    lastTarget = living;
                    lastSeenTime = now;
                    forceFade = false;
                    lastKnownCenter = center;
                }
            }
        }

        if (lastTarget != null && (!lastTarget.isAlive() || now - lastSeenTime > HUD_DURATION)) {
            lastTarget = null;
            forceFade = true;
        }

        boolean chatOpen = mc.currentScreen instanceof net.minecraft.client.gui.screen.ChatScreen;
        boolean previewMode = chatOpen && lastTarget == null;
        LivingEntity previewEntity = previewMode ? mc.player : null;

        boolean shouldShow = (lastTarget != null && !forceFade) || previewMode;
        fadeAnimation.animate(shouldShow ? 1f : 0f, 200);
        scaleAnimation.animate(shouldShow ? 1f : 0.9f, 200);
        slideAnimation.animate(shouldShow ? 0f : 1f, 220);

        if (fadeAnimation.getValue() <= 0 && forceFade && !previewMode) {
            forceFade = false;
            lastKnownCenter = null;
            previousHp01 = -1f;
            hudParticles.clear();
            prevHurtTime = 0;
            return;
        }

        float posX = getX();
        float posY = getY();
        setBounds(posX, posY, WIDTH, HEIGHT);

        int alpha = (int) (230 * fadeAnimation.getValue());
        if (alpha <= 0) return;

        LivingEntity target = lastTarget;
        if (!previewMode && target == null && lastKnownCenter == null) return;

        float rawHp = target != null ? MathUtils.round(Server.getHealth(target, false)) : (previewEntity != null ? MathUtils.round(Server.getHealth(previewEntity, false)) : 0f);
        float maxHp = target != null ? Math.max(1f, MathUtils.round(target.getMaxHealth())) : (previewEntity != null ? Math.max(1f, MathUtils.round(previewEntity.getMaxHealth())) : 20f);
        float absorb = target != null ? Math.max(0f, MathUtils.round(target.getAbsorptionAmount())) : (previewEntity != null ? Math.max(0f, MathUtils.round(previewEntity.getAbsorptionAmount())) : 0f);

        float hp01 = MathHelper.clamp(rawHp / maxHp, 0f, 1f);
        float abs01 = absorb > 0f ? MathHelper.clamp(absorb / maxHp, 0f, 1f) : 0f;

        float scale = scaleAnimation.getValue() * toggledAnimation.getValue() * fadeAnimation.getValue();
        float slideX = 16f * slideAnimation.getValue() * animationDirectionX;
        float slideY = 16f * slideAnimation.getValue() * animationDirectionY;

        e.getContext().getMatrices().push();
        e.getContext().getMatrices().translate(posX + WIDTH / 2f + slideX, posY + HEIGHT / 2f + slideY, 0f);
        e.getContext().getMatrices().scale(scale, scale, 1f);
        e.getContext().getMatrices().translate(-(posX + WIDTH / 2f), -(posY + HEIGHT / 2f), 0f);

        // Alternate style: simple card with percent bar inside
        if (style.getName("targethud.style.card").getValue()) {
            // Card dimensions match default TargetHud
            float cardW = WIDTH - 2f;
            float cardH = HEIGHT + 2f;
            setBounds(posX, posY, cardW, cardH);

            // Background
            Render2D.drawRoundedRect(
                    e.getContext().getMatrices(),
                    posX, posY,
                    cardW, cardH,
                    ROUNDING,
                    new Color(bgColor.getRed(), bgColor.getGreen(), bgColor.getBlue(), alpha)
            );

            // Avatar box (use same size/placement vibe as default)
            float avatarSize = HEAD + 12f;
            float avatarX = posX + SPACING;
            float avatarY = posY + (cardH / 2f - avatarSize / 2f) - 0.5f;
            Render2D.drawRoundedRect(e.getContext().getMatrices(), avatarX, avatarY, avatarSize, avatarSize, 6f,
                    new Color(80, 80, 80, Math.min(200, alpha)));

            // Draw head texture same as in default style when possible
            if (previewMode && previewEntity instanceof PlayerEntity) {
                Color headColor = getHitColor(previewEntity, alpha);
                Render2D.drawTexture(
                        e.getContext().getMatrices(),
                        avatarX, avatarY,
                        avatarSize, avatarSize,
                        3f,
                        0.125f, 0.125f, 0.125f, 0.125f,
                        ((AbstractClientPlayerEntity) previewEntity).getSkinTextures().texture(),
                        headColor
                );
            } else if (target instanceof PlayerEntity) {
                Color headColor = getHitColor(target, alpha);
                Render2D.drawTexture(
                        e.getContext().getMatrices(),
                        avatarX, avatarY,
                        avatarSize, avatarSize,
                        3f,
                        0.125f, 0.125f, 0.125f, 0.125f,
                        ((AbstractClientPlayerEntity) target).getSkinTextures().texture(),
                        headColor
                );
            } else {
                // Fallback mark for non-player entities
                Render2D.drawFont(
                        e.getContext().getMatrices(),
                        Fonts.BOLD.getFont(8.5f),
                        "?",
                        avatarX + (avatarSize / 2f) - Fonts.BOLD.getWidth("?", 8.5f) / 2f,
                        avatarY + (avatarSize / 2f) - Fonts.BOLD.getHeight(8.5f) / 2f,
                        new Color(textColor.getRed(), textColor.getGreen(), textColor.getBlue(), alpha)
                );
            }

            // Name text (with ellipsis if overflow)
            String dispName = (previewMode && previewEntity != null) ? previewEntity.getName().getString()
                    : ((target != null && !target.getName().getString().isEmpty()) ? target.getName().getString() : "Unknown");
            // Hide name in preview when NameProtect is enabled
            if (previewMode) {
                NameProtect np = NameProtect.getInstance();
                if (np != null && np.isToggled()) {
                    String replacement = np.getCustomName().getValue();
                    dispName = replacement != null && !replacement.isEmpty() ? replacement : "Protected";
                }
            }
            float nameX = avatarX + avatarSize + SPACING * 2 - 4f;
            float maxNameWCard = (posX + cardW - SPACING) - nameX;
            String drawName = ellipsize(dispName, 9f, maxNameWCard);
            Render2D.drawFont(
                    e.getContext().getMatrices(),
                    Fonts.BOLD.getFont(9f),
                    drawName,
                    nameX,
                    posY + SPACING + 1f,
                    new Color(textColor.getRed(), textColor.getGreen(), textColor.getBlue(), alpha)
            );

            // Progress bar with percent text inside
            float barW2 = cardW - SPACING - 42f;
            float barH2 = 12f;
            float barX2 = posX + SPACING + 38f;
            float barY2 = posY + cardH - SPACING - barH2 - 1.5f;

            // Track
            Render2D.drawRoundedRect(e.getContext().getMatrices(), barX2, barY2, barW2, barH2, 2f,
                    new Color(150, 150, 150, Math.min(160, alpha)));
            // Fill with smooth decrease animation (same as default)
            float hpTargetPx2 = barW2 * hp01;
            float fillW = Math.max(0f, Math.min(barW2, hpAnimPx.animate(hpTargetPx2, 100)));
            Render2D.drawRoundedRect(e.getContext().getMatrices(), barX2, barY2, fillW, barH2, 2f,
                    themeManager.getCurrentTheme().getAccentColor());

            // Percent text centered
            int percent = (int) Math.round(hp01 * 100.0);
            String pct = percent + "%";
            float pctX = barX2 + (barW2 - Fonts.BOLD.getWidth(pct, 8.5f)) / 2f;
            float pctY = barY2 + (barH2 - Fonts.BOLD.getHeight(8.5f)) / 2f + 0.2f;
            Render2D.drawFont(e.getContext().getMatrices(), Fonts.BOLD.getFont(7.5f), pct, pctX, pctY,
                    new Color(textColor.getRed(), textColor.getGreen(), textColor.getBlue(), alpha));

            e.getContext().getMatrices().pop();
            super.onRender2D(e);
            return;
        }

        // Default style
        float headX = posX + SPACING;
        float headY = posY + SPACING;
        float barX = headX + HEAD + SPACING * 2;
        // duplicates removed: barY/barW/barH already declared above

        // Avatar box
        Render2D.drawRoundedRect(e.getContext().getMatrices(), headX, headY, HEAD, HEAD, 6f,
                new Color(80, 80, 80, Math.min(200, alpha)));

        // Draw head texture same as in default style when possible
        if (previewMode && previewEntity instanceof PlayerEntity) {
            Color headColor = getHitColor(previewEntity, alpha);
            Render2D.drawTexture(
                    e.getContext().getMatrices(),
                    headX, headY,
                    HEAD, HEAD,
                    3f,
                    0.125f, 0.125f, 0.125f, 0.125f,
                    ((AbstractClientPlayerEntity) previewEntity).getSkinTextures().texture(),
                    headColor
            );
        } else if (target instanceof PlayerEntity) {
            Color headColor = getHitColor(target, alpha);
            Render2D.drawTexture(
                    e.getContext().getMatrices(),
                    headX, headY,
                    HEAD, HEAD,
                    3f,
                    0.125f, 0.125f, 0.125f, 0.125f,
                    ((AbstractClientPlayerEntity) target).getSkinTextures().texture(),
                    headColor
            );
        } else {
            // Fallback mark for non-player entities
            Render2D.drawFont(
                    e.getContext().getMatrices(),
                    Fonts.BOLD.getFont(8.5f),
                    "?",
                    headX + (HEAD / 2f) - Fonts.BOLD.getWidth("?", 8.5f) / 2f,
                    headY + (HEAD / 2f) - Fonts.BOLD.getHeight(8.5f) / 2f,
                    new Color(textColor.getRed(), textColor.getGreen(), textColor.getBlue(), alpha)
            );
        }

        // Text
        float textX = headX + HEAD + SPACING * 2;
        float textY = posY + SPACING + 1f;
        String name = previewMode && previewEntity != null ? previewEntity.getName().getString() : ((target != null && !target.getName().getString().isEmpty())
                ? target.getName().getString() : "Unknown");
        // Hide name in preview when NameProtect is enabled
        if (previewMode) {
            NameProtect np = NameProtect.getInstance();
            if (np != null && np.isToggled()) {
                String replacement = np.getCustomName().getValue();
                name = replacement != null && !replacement.isEmpty() ? replacement : "Protected";
            }
        }
        float maxNameW = (posX + WIDTH - SPACING) - textX;
        String nameDraw = ellipsize(name, 9f, maxNameW);
        Render2D.drawFont(e.getContext().getMatrices(), Fonts.BOLD.getFont(9f),
                nameDraw, textX, textY, new Color(headerTextColor.getRed(), headerTextColor.getGreen(), headerTextColor.getBlue(), alpha));
        float barY = posY + HEIGHT - SPACING - 6f;
        float barW = WIDTH - SPACING * 2;
        float barH = 6.5f;

        Render2D.drawRoundedRect(e.getContext().getMatrices(), barX, barY, barW, barH, 3f,
                new Color(32, 32, 32, Math.min(alpha, 200)));

        float hpTargetPx = barW * hp01;
        float absTargetPx = barW * abs01;
        float hpPx = Math.min(barW, hpAnimPx.animate(hpTargetPx, 100));
        float absPx = absorb > 0f ? Math.min(barW, absAnimPx.animate(absTargetPx, 120)) : 0f;

        // Spawn star particles on damage (skip in preview, or when particles are disabled)
        if (!previewMode && target != null && displayHudParticles.getValue()) {
            // Trigger when hurtTime increased compared to previous frame (new hit)
            if (target.hurtTime > prevHurtTime) {
                // Spawn a consistent amount per hit
                int spawn = 24;

                // Head center for outward direction
                float centerX = headX + HEAD / 2f;
                float centerY = headY + HEAD / 2f;

                // Distribute particles across all four edges
                int perSideBase = Math.max(1, spawn / 4);
                int remainder = spawn - perSideBase * 4;
                for (int edge = 0; edge < 4; edge++) {
                    int count = perSideBase + (edge < remainder ? 1 : 0);
                    for (int i = 0; i < count; i++) {
                        float spawnX;
                        float spawnY;
                        float t = (float) Math.random();
                        if (edge == 0) { // top
                            spawnX = headX + t * HEAD;
                            spawnY = headY;
                        } else if (edge == 1) { // right
                            spawnX = headX + HEAD;
                            spawnY = headY + t * HEAD;
                        } else if (edge == 2) { // bottom
                            spawnX = headX + t * HEAD;
                            spawnY = headY + HEAD;
                        } else { // left
                            spawnX = headX;
                            spawnY = headY + t * HEAD;
                        }

                        // Base direction: from head center to spawn point (outward)
                        float baseAngle = (float) Math.atan2(spawnY - centerY, spawnX - centerX);
                        // Small jitter +/- 35 degrees to add variation
                        float jitter = (float) ((Math.random() - 0.5) * Math.toRadians(70));
                        float angle = baseAngle + jitter;

                        // Slower speed
                        float speed = 7f + (float) (Math.random() * 9f); // px/s
                        float vx = (float) (Math.cos(angle) * speed);
                        float vy = (float) (Math.sin(angle) * speed);

                        float size = 5.5f + (float) (Math.random() * 3.5f);
                        long lifeMs = 1200 + (int) (Math.random() * 1200); // longer lifetime
                        hudParticles.add(new HudParticle(spawnX, spawnY, vx, vy, size, lifeMs));
                    }
                }
                prevHurtTime = target.hurtTime;
            }
            prevHurtTime = target.hurtTime;
            previousHp01 = hp01;
        } else {
            previousHp01 = -1f;
            prevHurtTime = 0;
        }

        // Use live accent color from theme for gradient themes
        Color liveAccent = themeManager.getCurrentTheme().getAccentColor();
        Render2D.drawRoundedRect(e.getContext().getMatrices(), barX, barY, hpPx, barH,
                Math.min(2f, hpPx / 2f), liveAccent);

        if (displayAbsorption.getValue() && absorb > 0f && absPx > 0f) {
            Render2D.drawRoundedRect(e.getContext().getMatrices(), barX, barY, absPx, barH,
                    Math.min(2f, absPx / 2f), absorbColor);
        }

        // Update and render HUD particles (and clear if disabled)
        if (!displayHudParticles.getValue()) {
            hudParticles.clear();
        }
        if (displayHudParticles.getValue() && !hudParticles.isEmpty()) {
            Color base = themeManager.getCurrentTheme().getAccentColor();
            hudParticles.removeIf(p -> p.updateAndIsDead(dt));
            for (HudParticle p : hudParticles) {
                int a = (int) (Math.min(1f, Math.max(0f, p.alpha)) * 255);
                if (a <= 0) continue;
                float size = p.size;
                Render2D.drawTexture(
                        e.getContext().getMatrices(),
                        p.x - size / 2f,
                        p.y - size / 2f,
                        size,
                        size,
                        0f,
                        STAR_TEX,
                        new Color(base.getRed(), base.getGreen(), base.getBlue(), a)
                );
            }
        }

        e.getContext().getMatrices().pop();
        super.onRender2D(e);
    }

    // Trim text to fit maxWidth by appending "..." when necessary using the provided font size
    private String ellipsize(String text, float fontSize, float maxWidth) {
        if (text == null) return "";
        if (Fonts.BOLD.getWidth(text, fontSize) <= maxWidth) return text;
        String ellipsis = "...";
        float ellipsisW = Fonts.BOLD.getWidth(ellipsis, fontSize);
        if (ellipsisW > maxWidth) return ""; // not enough space for anything
        int lo = 0, hi = text.length();
        String best = "";
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            String candidate = text.substring(0, mid) + ellipsis;
            float w = Fonts.BOLD.getWidth(candidate, fontSize);
            if (w <= maxWidth) {
                best = candidate;
                lo = mid + 1;
            } else {
                hi = mid - 1;
            }
        }
        return best;
    }

    private static class HudParticle {
        float x;
        float y;
        float vx;
        float vy;
        float size;
        long lifeMs;
        long ageMs;
        float alpha = 1f;

        HudParticle(float x, float y, float vx, float vy, float size, long lifeMs) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.size = size;
            this.lifeMs = lifeMs;
            this.ageMs = 0L;
        }

        boolean updateAndIsDead(float dtSeconds) {
            long dtMs = (long) (dtSeconds * 1000f);
            this.ageMs += dtMs;
            float t = Math.max(0f, Math.min(1f, ageMs / (float) lifeMs));
            // Smooth drift with gentle damping, no gravity
            float dampingPerSecond = 0.88f; // retain ~88% speed per second
            float factor = (float) Math.pow(dampingPerSecond, dtSeconds);
            this.vx *= factor;
            this.vy *= factor;
            this.x += vx * dtSeconds;
            this.y += vy * dtSeconds;
            // Fade out towards end
            this.alpha = 1f - t;
            return ageMs >= lifeMs || alpha <= 0f;
        }
    }
}