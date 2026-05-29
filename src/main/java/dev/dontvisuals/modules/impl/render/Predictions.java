package dev.dontvisuals.modules.impl.render;

import dev.dontvisuals.client.managers.ThemeManager;
import dev.dontvisuals.client.events.impl.EventRender2D;
import dev.dontvisuals.client.events.impl.EventRender3D;
import dev.dontvisuals.modules.api.Category;
import dev.dontvisuals.modules.api.Module;
import dev.dontvisuals.modules.settings.impl.BooleanSetting;
import dev.dontvisuals.client.util.renderer.Render2D;
import dev.dontvisuals.client.util.renderer.Render3D;
import dev.dontvisuals.client.util.renderer.fonts.Fonts;
import dev.dontvisuals.client.util.world.WorldUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.entity.projectile.thrown.PotionEntity;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.client.resource.language.I18n;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class Predictions extends Module implements ThemeManager.ThemeChangeListener {

    private record ProjectilePoint(
            Vec3d pos, int ticks, boolean isLandingPoint, long creationTime,
            ProjectileType type, boolean isMoving, Entity hitEntity,
            boolean isPlayerThrown, int entityId
    ) {}

    private enum ProjectileType {
        PEARL, ARROW, TRIDENT, POTION
    }

    private final List<ProjectilePoint> projectilePoints     = new ArrayList<>();
    private final List<ProjectilePoint> prevProjectilePoints = new ArrayList<>();
    private final ThemeManager themeManager;
    private Color currentColor;
    private Color currentColorSecondary;

    private final BooleanSetting showPearl        = new BooleanSetting("Show Pearl",         true,  () -> true);
    private final BooleanSetting showBow          = new BooleanSetting("Show Bow",            true,  () -> true);
    private final BooleanSetting showCrossbow     = new BooleanSetting("Show Crossbow",       true,  () -> true);
    private final BooleanSetting showTrident      = new BooleanSetting("Show Trident",        true,  () -> true);
    private final BooleanSetting showPotion       = new BooleanSetting("Show Potion",         true,  () -> true);
    private final BooleanSetting showWhenHolding  = new BooleanSetting("Show When Holding",   true,  () -> true);
    private final BooleanSetting showLandingInfo  = new BooleanSetting("Show Landing Info",   true,  () -> true);
    private final BooleanSetting highlightPlayers = new BooleanSetting("Highlight Players",   true,  () -> true);

    private static final double highlightRange  = 100.0;
    private static final int    MAX_TICKS       = 240;
    // Уменьшаем substeps — основная причина лагов (было 8 → 4, точность достаточная)
    private static final int    SUBSTEPS        = 4;
    private static final float  BASE_LINE_WIDTH = 5.0f;   // жирнее — лучше видно

    // Pearl
    private static final double PEARL_SPEED        = 1.5;
    private static final double PEARL_SPEED_FACTOR = 1.06;
    private static final double PEARL_GRAVITY      = 0.03;
    // Arrow
    private static final double ARROW_SPEED   = 3.0;
    private static final double ARROW_GRAVITY = 0.05;
    // Trident
    private static final double TRIDENT_SPEED   = 2.5;
    private static final double TRIDENT_GRAVITY = 0.05;
    // Potion
    private static final double POTION_SPEED   = 0.5;
    private static final double POTION_GRAVITY = 0.05;

    private static final double WATER_DRAG = 0.8;
    private static final double AIR_DRAG   = 0.99;

    public Predictions() {
        super("Predictions", Category.Render, I18n.translate("module.predictions.description"));
        this.themeManager            = ThemeManager.getInstance();
        this.currentColor            = themeManager.getThemeColor();
        this.currentColorSecondary   = themeManager.getCurrentTheme().getSecondaryBackgroundColor();
        themeManager.addThemeChangeListener(this);
    }

    @Override
    public void onThemeChanged(ThemeManager.Theme theme) {
        this.currentColor          = theme.getBackgroundColor();
        this.currentColorSecondary = theme.getSecondaryBackgroundColor();
    }

    @Override
    public void onDisable() {
        themeManager.removeThemeChangeListener(this);
        projectilePoints.clear();
        prevProjectilePoints.clear();
        super.onDisable();
    }

    @EventHandler
    public void onRender2D(EventRender2D e) {
        if (fullNullCheck()) return;
        if (showLandingInfo.getValue() && !projectilePoints.isEmpty()) {
            renderLandingInfo2D(e);
        }
    }

    @EventHandler
    public void onRender3D(EventRender3D.Game e) {
        if (fullNullCheck()) return;

        Render3D.setTickDelta(e.getTickDelta());

        prevProjectilePoints.clear();
        prevProjectilePoints.addAll(projectilePoints);
        projectilePoints.clear();

        // ──────────────────────────────────────────────────────────────
        // Кэшируем список сущностей ОДИН РАЗ — главный источник лагов.
        // Раньше mc.world.getEntities() вызывался внутри двойного цикла
        // MAX_TICKS * SUBSTEPS = до 1920 раз за кадр.
        // ──────────────────────────────────────────────────────────────
        List<Entity> cachedEntities = new ArrayList<>();
        for (Entity ent : mc.world.getEntities()) cachedEntities.add(ent);

        // Флаг: летит ли уже снаряд, БРОШЕННЫЙ САМИМ ИГРОКОМ.
        // Если да — не рисуем предсказание из руки, чтобы не было двух полосок.
        boolean hasOwnFlying = false;

        for (Entity ent : cachedEntities) {
            if (ent instanceof EnderPearlEntity pearl && showPearl.getValue()) {
                boolean own = pearl.getOwner() == mc.player;
                if (own) hasOwnFlying = true;
                simulateGeneric(pearl.getPos(), pearl.getVelocity(), pearl.isTouchingWater(),
                        ProjectileType.PEARL, own, pearl.getId(), cachedEntities);

            } else if (ent instanceof ArrowEntity arrow && arrow.getOwner() != null
                    && (showBow.getValue() || showCrossbow.getValue())) {
                boolean moving = !arrow.isOnGround() && arrow.getVelocity().lengthSquared() >= 0.01;
                if (moving) {
                    boolean own = arrow.getOwner() == mc.player;
                    if (own) hasOwnFlying = true;
                    simulateGeneric(arrow.getPos(), arrow.getVelocity(), arrow.isTouchingWater(),
                            ProjectileType.ARROW, own, arrow.getId(), cachedEntities);
                }

            } else if (ent instanceof TridentEntity trident && trident.getOwner() != null
                    && showTrident.getValue()) {
                boolean moving = !trident.isOnGround() && trident.getVelocity().lengthSquared() >= 0.01;
                if (moving) {
                    boolean own = trident.getOwner() == mc.player;
                    if (own) hasOwnFlying = true;
                    simulateGeneric(trident.getPos(), trident.getVelocity(), trident.isTouchingWater(),
                            ProjectileType.TRIDENT, own, trident.getId(), cachedEntities);
                }

            } else if (ent instanceof PotionEntity potion && showPotion.getValue()) {
                boolean own = potion.getOwner() == mc.player;
                if (own) hasOwnFlying = true;
                simulateGeneric(potion.getPos(), potion.getVelocity(), potion.isTouchingWater(),
                        ProjectileType.POTION, own, potion.getId(), cachedEntities);
            }
        }

        // ──────────────────────────────────────────────────────────────
        // Предсказание из руки — только когда собственный снаряд НЕ летит.
        // ──────────────────────────────────────────────────────────────
        if (!hasOwnFlying && showWhenHolding.getValue()) {
            boolean holdingPearl    = isHoldingItem(Items.ENDER_PEARL);
            boolean holdingBow      = isHoldingItem(Items.BOW);
            boolean holdingCrossbow = isHoldingItem(Items.CROSSBOW);
            boolean holdingTrident  = isHoldingItem(Items.TRIDENT);
            boolean holdingPotion   = isHoldingItem(Items.SPLASH_POTION)
                    || isHoldingItem(Items.LINGERING_POTION);

            boolean isBowDrawn      = mc.player.isUsingItem()
                    && mc.player.getActiveItem().getItem() == Items.BOW;
            boolean isCrossbowLoaded = holdingCrossbow
                    && (CrossbowItem.isCharged(mc.player.getMainHandStack())
                    || CrossbowItem.isCharged(mc.player.getOffHandStack()));
            boolean isTridentWinding = mc.player.isUsingItem()
                    && mc.player.getActiveItem().getItem() == Items.TRIDENT;

            if (holdingPearl    && showPearl.getValue())
                simulateFromHand(ProjectileType.PEARL,   PEARL_SPEED,   -1, cachedEntities);
            if (holdingBow      && showBow.getValue()      && isBowDrawn)
                simulateFromHand(ProjectileType.ARROW,   ARROW_SPEED,   -2, cachedEntities);
            if (holdingCrossbow && showCrossbow.getValue() && isCrossbowLoaded)
                simulateFromHand(ProjectileType.ARROW,   ARROW_SPEED,   -3, cachedEntities);
            if (holdingTrident  && showTrident.getValue()  && isTridentWinding)
                simulateFromHand(ProjectileType.TRIDENT, TRIDENT_SPEED, -4, cachedEntities);
            if (holdingPotion   && showPotion.getValue())
                simulateFromHand(ProjectileType.POTION,  POTION_SPEED,  -5, cachedEntities);
        }

        if (!projectilePoints.isEmpty()) {
            renderProjectileTrajectory3D(e);
        }

        if (highlightPlayers.getValue()) {
            for (ProjectilePoint pt : projectilePoints) {
                if (pt.isLandingPoint() && pt.isPlayerThrown() && pt.hitEntity() instanceof LivingEntity living
                        && living != mc.player && living.isAlive()) {
                    double dist = mc.player.getPos().distanceTo(living.getPos());
                    if (dist <= highlightRange) renderPlayerHighlight(e, living);
                }
            }
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    // Хелперы
    // ──────────────────────────────────────────────────────────────────────

    private boolean isHoldingItem(net.minecraft.item.Item item) {
        if (mc.player == null) return false;
        return mc.player.getMainHandStack().getItem() == item
                || mc.player.getOffHandStack().getItem() == item;
    }

    /**
     * Стартовая точка для предсказания из руки.
     * Сдвинута на 0.6 блока вперёд по взгляду — линия не влетает в лицо.
     */
    private Vec3d getThrowStartPos() {
        float yaw   = mc.player.getYaw();
        float pitch = mc.player.getPitch();
        Vec3d dir = new Vec3d(
                -Math.sin(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)),
                -Math.sin(Math.toRadians(pitch)),
                Math.cos(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch))
        );
        return mc.player.getEyePos().add(dir.multiply(0.6));
    }

    private Vec3d getThrowVelocity(double speed) {
        float yaw   = mc.player.getYaw();
        float pitch = mc.player.getPitch();
        return new Vec3d(
                -Math.sin(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)),
                -Math.sin(Math.toRadians(pitch)),
                Math.cos(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch))
        ).multiply(speed);
    }

    private double gravityFor(ProjectileType type) {
        return switch (type) {
            case PEARL   -> PEARL_GRAVITY;
            case ARROW   -> ARROW_GRAVITY;
            case TRIDENT -> TRIDENT_GRAVITY;
            case POTION  -> POTION_GRAVITY;
        };
    }

    /** Ловим и стоячую, и текущую воду */
    private boolean isInWater(Vec3d pos) {
        var fluid = mc.world.getBlockState(BlockPos.ofFloored(pos)).getFluidState();
        return !fluid.isEmpty();
    }

    // ──────────────────────────────────────────────────────────────────────
    // Симуляция
    // ──────────────────────────────────────────────────────────────────────

    private void simulateFromHand(ProjectileType type, double speed, int entityId, List<Entity> entities) {
        simulateGeneric(getThrowStartPos(), getThrowVelocity(speed), false,
                type, true, entityId, entities);
    }

    /**
     * Единый метод симуляции для всех типов снарядов.
     * entities — кэшированный список, НЕ вызываем mc.world.getEntities() внутри.
     */
    private void simulateGeneric(Vec3d pos, Vec3d vel, boolean startInWater,
                                 ProjectileType type, boolean isPlayerThrown,
                                 int entityId, List<Entity> entities) {
        boolean inWater    = startInWater;
        double  speedFactor = type == ProjectileType.PEARL ? PEARL_SPEED_FACTOR : 1.0;
        double  gravity     = gravityFor(type);

        // Время фиксируется ОДИН РАЗ — от него отсчитываем elapsed в renderLandingInfo2D
        long simStartTime = System.nanoTime();

        int ticks = 0;
        outer:
        for (int t = 0; t < MAX_TICKS; t++) {
            for (int s = 0; s < SUBSTEPS; s++) {
                Vec3d prev = pos;
                inWater = isInWater(pos) || inWater;

                double drag = Math.pow(inWater ? WATER_DRAG : AIR_DRAG, 1.0 / SUBSTEPS);
                pos = pos.add(vel.multiply(speedFactor / SUBSTEPS));

                // Коллизия с блоком
                RaycastContext ctx = new RaycastContext(prev, pos,
                        RaycastContext.ShapeType.COLLIDER,
                        RaycastContext.FluidHandling.NONE, mc.player);
                HitResult hit = mc.world.raycast(ctx);
                if (hit.getType() == HitResult.Type.BLOCK) {
                    BlockHitResult bhr = (BlockHitResult) hit;
                    projectilePoints.add(new ProjectilePoint(
                            bhr.getPos(), ticks, true, simStartTime,
                            type, true, null, isPlayerThrown, entityId));
                    break outer;
                }

                // За пределы мира
                if (pos.y < mc.world.getBottomY()) {
                    projectilePoints.add(new ProjectilePoint(
                            pos, ticks, true, simStartTime,
                            type, true, null, isPlayerThrown, entityId));
                    break outer;
                }

                // Коллизия с сущностью (только для снарядов игрока — чужие не проверяем)
                Entity hitEntity = null;
                if (isPlayerThrown) {
                    for (Entity entity : entities) {
                        if (entity == mc.player) continue;
                        if (entity.getBoundingBox().expand(0.3).contains(pos)) {
                            hitEntity = entity;
                            break;
                        }
                    }
                }

                projectilePoints.add(new ProjectilePoint(
                        pos, ticks, hitEntity != null, simStartTime,
                        type, true, hitEntity, isPlayerThrown, entityId));

                if (hitEntity != null) break outer;

                double gravMult = inWater ? 0.2 : 1.0;
                vel = vel.subtract(0, (gravity * speedFactor * gravMult) / SUBSTEPS, 0)
                        .multiply(drag);
            }
            ticks++;
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    // Рендер
    // ──────────────────────────────────────────────────────────────────────

    private void renderProjectileTrajectory3D(EventRender3D.Game e) {
        List<List<ProjectilePoint>> trajectories     = groupTrajectories(projectilePoints);
        List<List<ProjectilePoint>> prevTrajectories = groupTrajectories(prevProjectilePoints);
        float tickDelta = e.getTickDelta();

        Render3D.prepare();
        Render3D.DEBUG_LINE_WIDTH = BASE_LINE_WIDTH;

        for (int g = 0; g < trajectories.size(); g++) {
            List<ProjectilePoint> traj     = trajectories.get(g);
            List<ProjectilePoint> prevTraj = g < prevTrajectories.size() ? prevTrajectories.get(g) : null;
            if (traj.size() < 2) continue;

            Vec3d prevPos = interpolatePoint(prevTraj, traj, 0, tickDelta);

            for (int i = 1; i < traj.size(); i++) {
                Vec3d currPos = interpolatePoint(prevTraj, traj, i, tickDelta);
                float t = (float) i / (float) traj.size();

                int r  = clamp((int)(currentColor.getRed()   * (1f - t) + currentColorSecondary.getRed()   * t));
                int gg = clamp((int)(currentColor.getGreen() * (1f - t) + currentColorSecondary.getGreen() * t));
                int b  = clamp((int)(currentColor.getBlue()  * (1f - t) + currentColorSecondary.getBlue()  * t));
                // Минимальная альфа 120 — траектория хорошо видна даже на хвосте
                int a  = Math.max(120, (int)(255f * (1f - t)));

                Render3D.drawLine(prevPos, currPos, new Color(r, gg, b, a).getRGB(), BASE_LINE_WIDTH);

                // Хайлайт сущности при попадании
                ProjectilePoint pt = traj.get(i);
                if (pt.isPlayerThrown() && pt.hitEntity() instanceof LivingEntity living
                        && living.isAlive() && pt.isLandingPoint()) {
                    Box box = living.getBoundingBox().expand(0.1);
                    Render3D.renderBox(e.getMatrices(), box, new Color(r, gg, b, 80));
                    Render3D.renderBoxOutline(e.getMatrices(), box, new Color(r, gg, b, 255));
                }

                prevPos = currPos;
            }

            // Крестик в точке приземления на земле
            ProjectilePoint last = traj.get(traj.size() - 1);
            if (last.isLandingPoint() && last.hitEntity() == null) {
                int argb = new Color(
                        currentColor.getRed(), currentColor.getGreen(),
                        currentColor.getBlue(), 220).getRGB();
                float thick = BASE_LINE_WIDTH * 1.6f;
                Render3D.drawLine(last.pos().add(-0.35, 0.02, 0),
                        last.pos().add( 0.35, 0.02, 0), argb, thick);
                Render3D.drawLine(last.pos().add(0, 0.02, -0.35),
                        last.pos().add(0, 0.02,  0.35), argb, thick);
            }
        }

        Render3D.render();
    }

    /**
     * Группировка по (type, entityId).
     * entityId уникален для каждого летящего снаряда, поэтому
     * два перла одновременно в воздухе → две отдельные траектории.
     */
    private List<List<ProjectilePoint>> groupTrajectories(List<ProjectilePoint> points) {
        List<List<ProjectilePoint>> out     = new ArrayList<>();
        List<ProjectilePoint>       current = null;
        ProjectileType lastType = null;
        int            lastId   = Integer.MIN_VALUE;

        for (ProjectilePoint pt : points) {
            if (current == null || lastType != pt.type() || lastId != pt.entityId()) {
                if (current != null && !current.isEmpty()) out.add(current);
                current  = new ArrayList<>();
                lastType = pt.type();
                lastId   = pt.entityId();
            }
            current.add(pt);
        }
        if (current != null && !current.isEmpty()) out.add(current);
        return out;
    }

    private Vec3d interpolatePoint(List<ProjectilePoint> prev, List<ProjectilePoint> curr,
                                   int index, float t) {
        Vec3d c = curr.get(index).pos();
        if (prev == null || prev.isEmpty()) return c;
        Vec3d p  = prev.get(Math.min(index, prev.size() - 1)).pos();
        float tt = 1f - (float) Math.pow(1f - Math.min(Math.max(t, 0f), 1f), 3.0);
        return new Vec3d(
                p.x + (c.x - p.x) * tt,
                p.y + (c.y - p.y) * tt,
                p.z + (c.z - p.z) * tt);
    }

    private void renderLandingInfo2D(EventRender2D e) {
        MatrixStack ms = e.getContext().getMatrices();
        long nowNano = System.nanoTime();
        for (ProjectilePoint pp : projectilePoints) {
            if (!pp.isLandingPoint()) continue;
            Vec3d lp = WorldUtils.getPosition(pp.pos);
            if (lp.z <= 0 || lp.z >= 1) continue;

            // pp.ticks — симуляционные тики от текущей позиции снаряда до земли.
            // Вычитаем реальное время, прошедшее с момента расчёта точки,
            // чтобы счётчик плавно убывал между тиками симуляции.
            double simulatedSec = pp.ticks / 20.0;
            double elapsedSec   = (nowNano - pp.creationTime()) / 1_000_000_000.0;
            double remaining    = Math.max(0.0, simulatedSec - elapsedSec);
            String timeText = String.format("%.1fs", remaining);
            int boxX = (int) lp.x - 20, boxY = (int) lp.y + 4;
            int boxW = 40,              boxH  = 15;
            float fontSize = 7.5f;

            Render2D.drawRoundedRect(ms, boxX, boxY, boxW, boxH - 1, 3, new Color(0, 0, 0, 160));

            ms.push();
            float scale = 0.6f;
            int half = (int)(8 * scale);
            ms.translate(boxX + boxW / 2 - half, boxY + boxH / 2 - half, 0);
            ms.scale(scale, scale, 1f);

            ItemStack itemStack = switch (pp.type()) {
                case PEARL   -> new ItemStack(Items.ENDER_PEARL);
                case ARROW   -> new ItemStack(Items.ARROW);
                case TRIDENT -> new ItemStack(Items.TRIDENT);
                case POTION  -> new ItemStack(Items.SPLASH_POTION);
            };
            e.getContext().drawItem(itemStack, -20, -1);
            ms.pop();

            Render2D.drawFont(ms, Fonts.MEDIUM.getFont(fontSize), timeText,
                    boxX + 18, boxY + boxH / 2f - Fonts.MEDIUM.getHeight(fontSize) / 2f,
                    Color.WHITE);
        }
    }

    private void renderPlayerHighlight(EventRender3D.Game e, LivingEntity player) {
        Box box = player.getBoundingBox().expand(0.1);
        Render3D.renderBox(e.getMatrices(), box,
                new Color(currentColor.getRed(), currentColor.getGreen(), currentColor.getBlue(), 80));
        Render3D.renderBoxOutline(e.getMatrices(), box, currentColor);
    }

    private static int clamp(int v) { return Math.min(255, Math.max(0, v)); }
}