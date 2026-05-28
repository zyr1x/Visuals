package dev.dontvisuals.modules.impl.render;

import dev.dontvisuals.client.events.impl.EventRender3D;
import dev.dontvisuals.client.events.impl.EventTick;
import dev.dontvisuals.client.managers.ThemeManager;
import dev.dontvisuals.client.util.perf.Perf;
import dev.dontvisuals.client.util.renderer.Render3D;
import dev.dontvisuals.modules.api.Category;
import dev.dontvisuals.modules.api.Module;
import dev.dontvisuals.modules.settings.impl.BooleanSetting;
import dev.dontvisuals.modules.settings.impl.ListSetting;
import dev.dontvisuals.modules.settings.impl.NumberSetting;
import dev.dontvisuals.dontvisuals;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.*;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import com.mojang.blaze3d.systems.RenderSystem;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class JumpCircle extends Module implements ThemeManager.ThemeChangeListener {

    // ── Режим ───────────────────────────────────────────────────────────────
    private final BooleanSetting modeBlocks   = new BooleanSetting("mode.blocks",   true,  () -> false);
    private final BooleanSetting modeTexture  = new BooleanSetting("mode.texture",  false, () -> false);
    private final ListSetting mode = new ListSetting(
            I18n.translate("setting.animationMode"), true,
            modeBlocks, modeTexture
    );

    // ── Общие ───────────────────────────────────────────────────────────────
    private final NumberSetting radius   = new NumberSetting("Radius",    4.0f, 1.0f, 8.0f,  0.5f);
    private final NumberSetting lifeTime = new NumberSetting("Life Time", 0.8f, 0.3f, 2.0f,  0.05f);

    // ── Blocks mode ─────────────────────────────────────────────────────────
    private final NumberSetting waveSpeed   = new NumberSetting("Wave Speed",  1.0f, 0.3f, 3.0f, 0.1f);
    private final BooleanSetting multiLayer = new BooleanSetting("Multi Layer", false);

    // ── Texture mode (старый JumpCircle) ────────────────────────────────────
    private final NumberSetting texSize  = new NumberSetting("Circle Size", 0.5f, 0.2f, 2.0f, 0.05f);
    private final NumberSetting texSpeed = new NumberSetting("Speed",       1.0f, 0.5f, 3.0f, 0.1f);

    private final BooleanSetting texFirst  = new BooleanSetting("First",  true,  () -> false);
    private final BooleanSetting texSecond = new BooleanSetting("Second", false, () -> false);
    private final ListSetting textureMode = new ListSetting("Texture", true, texFirst, texSecond);

    private final BooleanSetting animGrow   = new BooleanSetting("Grow",   true,  () -> false);
    private final BooleanSetting animPulse  = new BooleanSetting("Pulse",  false, () -> false);
    private final BooleanSetting animRipple = new BooleanSetting("Ripple", false, () -> false);
    private final ListSetting animationMode = new ListSetting("Animation", true, animGrow, animPulse, animRipple);

    private final Identifier texJump    = dontvisuals.id("textures/jumpcircle.png");
    private final Identifier texCircle2 = dontvisuals.id("textures/circle.png");

    // ── State ────────────────────────────────────────────────────────────────
    private final List<JumpEvent> events = new CopyOnWriteArrayList<>();
    private boolean wasOnGround = true;
    private long lastJumpTime = 0;
    private BlockPos lastGroundPos = null;

    private final ThemeManager themeManager;
    private Color currentColor;

    public JumpCircle() {
        super("JumpCircle", Category.Render, I18n.translate("module.jumpcircle.description"));

        // Blocks visibility
        waveSpeed.setVisible(() -> modeBlocks.getValue());
        multiLayer.setVisible(() -> modeBlocks.getValue());

        // Texture visibility
        texSize.setVisible(() -> modeTexture.getValue());
        texSpeed.setVisible(() -> modeTexture.getValue());
        textureMode.setVisible(() -> modeTexture.getValue());
        animationMode.setVisible(() -> modeTexture.getValue());

        this.themeManager = ThemeManager.getInstance();
        this.currentColor = themeManager.getThemeColor();
        themeManager.addThemeChangeListener(this);
    }

    @Override
    public void onThemeChanged(ThemeManager.Theme theme) {
        this.currentColor = theme.getBackgroundColor();
    }

    @Override
    public void onDisable() {
        themeManager.removeThemeChangeListener(this);
        events.clear();
        super.onDisable();
    }

    // ── Tick ─────────────────────────────────────────────────────────────────

    @EventHandler
    public void onTick(EventTick event) {
        if (fullNullCheck()) return;
        ClientPlayerEntity p = mc.player;
        if (p == null) return;

        boolean onGround = p.isOnGround();
        long now = System.currentTimeMillis();

        if (onGround) {
            lastGroundPos = BlockPos.ofFloored(p.getX(), p.getY() - 0.1, p.getZ());
        }

        if (wasOnGround && !onGround && p.getVelocity().y > 0.01 && (now - lastJumpTime) > 100) {
            spawnEvent(p);
            lastJumpTime = now;
        }
        wasOnGround = onGround;
    }

    private void spawnEvent(ClientPlayerEntity p) {
        World world = mc.world;
        if (world == null) return;

        Vec3d jumpOrigin = new Vec3d(p.getX(), p.getY(), p.getZ());
        long ttl = (long) (lifeTime.getValue() * 1000L);

        if (modeBlocks.getValue()) {
            BlockPos feet = lastGroundPos != null
                    ? lastGroundPos
                    : BlockPos.ofFloored(p.getX(), p.getY() - 0.1, p.getZ());

            int r = (int) Math.ceil(radius.getValue());
            int layers = multiLayer.getValue() ? 2 : 1;
            float rad = radius.getValue();

            List<HighlightBlock> blocks = new ArrayList<>();
            for (int dy = 0; dy < layers; dy++) {
                for (int dx = -r; dx <= r; dx++) {
                    for (int dz = -r; dz <= r; dz++) {
                        float dist = (float) Math.sqrt(dx * dx + dz * dz);
                        if (dist > rad) continue;
                        BlockPos bp = feet.add(dx, -dy, dz);
                        BlockState state = world.getBlockState(bp);
                        if (!state.isAir() && state.isOpaque()) {
                            blocks.add(new HighlightBlock(bp, dist));
                        }
                    }
                }
            }
            if (!blocks.isEmpty()) {
                events.add(new JumpEvent(jumpOrigin, blocks, ttl));
            }
        } else {
            // Texture mode — просто сохраняем origin
            events.add(new JumpEvent(jumpOrigin, null, ttl));
        }
    }

    // ── Render ───────────────────────────────────────────────────────────────

    @EventHandler
    public void onRender3D(EventRender3D.Game event) {
        if (fullNullCheck()) return;
        try (var __ = Perf.scopeCpu("JumpCircle.onRender3D")) {
            long now = System.currentTimeMillis();
            events.removeIf(ev -> (now - ev.spawnTime) > ev.ttl);
            if (events.isEmpty()) return;

            // Гарантируем один режим
            if (modeBlocks.getValue() && modeTexture.getValue())  modeTexture.setValue(false);
            if (modeTexture.getValue() && modeBlocks.getValue())  modeBlocks.setValue(false);
            if (!modeBlocks.getValue() && !modeTexture.getValue()) modeBlocks.setValue(true);

            if (modeBlocks.getValue()) {
                renderBlocks(event, now);
            } else {
                renderTexture(event, now);
            }
        }
    }

    // ── Blocks mode render ───────────────────────────────────────────────────

    private void renderBlocks(EventRender3D.Game event, long now) {
        Vec3d cam = mc.gameRenderer.getCamera().getPos();
        MatrixStack matrices = event.getMatrices();

        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        RenderSystem.disableCull();

        Color themeColor = themeManager.getCurrentTheme().getBackgroundColor();

        for (JumpEvent ev : events) {
            if (ev.blocks == null) continue;
            long age = now - ev.spawnTime;
            float t = Math.max(0f, Math.min(1f, age / (float) ev.ttl));
            float waveFront = t * radius.getValue() * waveSpeed.getValue();

            for (HighlightBlock hb : ev.blocks) {
                float alpha = computeAlpha(hb.dist, waveFront, t);
                if (alpha < 0.01f) continue;
                drawTopFace(matrices, hb.pos, cam, themeColor, (int) (alpha * 210));
            }
        }

        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private float computeAlpha(float dist, float waveFront, float t) {
        float maxR = Math.max(0.01f, radius.getValue());
        float wavePhase = waveFront / maxR - dist / maxR;
        if (wavePhase < 0f) return 0f;

        float alpha;
        if (wavePhase < 0.15f) {
            alpha = wavePhase / 0.15f;
        } else {
            float decay = (wavePhase - 0.15f) / 0.85f;
            alpha = (float) Math.pow(1.0f - Math.min(1f, decay), 2.0f);
        }
        alpha *= (float) Math.pow(1.0f - t, 0.4f);
        return Math.max(0f, Math.min(1f, alpha));
    }

    private void drawTopFace(MatrixStack matrices, BlockPos pos, Vec3d cam, Color color, int alpha) {
        float minX = (float) (pos.getX() - cam.x);
        float y    = (float) (pos.getY() + 1.0 - cam.y + 0.002);
        float minZ = (float) (pos.getZ() - cam.z);
        float maxX = minX + 1.0f;
        float maxZ = minZ + 1.0f;

        float r = color.getRed()   / 255.0f;
        float g = color.getGreen() / 255.0f;
        float b = color.getBlue()  / 255.0f;
        float a = alpha / 255.0f;

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        buffer.vertex(matrix, minX, y, minZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, y, minZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, y, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, y, maxZ).color(r, g, b, a);

        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }

    // ── Texture mode render (старый JumpCircle) ──────────────────────────────

    private void renderTexture(EventRender3D.Game event, long now) {
        MatrixStack matrices = event.getMatrices();

        for (JumpEvent ev : events) {
            long age = now - ev.spawnTime;
            float t = Math.max(0f, Math.min(1f, (age / (float) ev.ttl) * texSpeed.getValue()));

            float radiusMul;
            float alphaK;

            if (animPulse.getValue()) {
                float s = (float) Math.sin(Math.PI * 2.0f * t);
                radiusMul = 0.7f + 0.5f * Math.max(0f, s);
                alphaK = Math.max(0f, s) * (1.0f - t);
            } else if (animRipple.getValue()) {
                float te = easeOutCirc(Math.max(0f, Math.min(1f, t)));
                radiusMul = 0.6f + 0.8f * te;
                alphaK = (float) Math.pow(Math.sin(Math.max(0f, Math.min(1f, t)) * (float) Math.PI), 1.2f);
            } else {
                // Grow
                float te = bothSine(t);
                radiusMul = 0.6f + 0.4f * te;
                alphaK = (float) Math.pow(1.0f - t, 0.8f) * te;
            }

            alphaK = Math.max(0f, Math.min(1f, alphaK));
            int alpha = (int) (255 * alphaK);
            if (alpha <= 2) continue;

            float r = texSize.getValue() * radiusMul;
            Color themeColor = themeManager.getCurrentTheme().getBackgroundColor();
            Color drawColor = new Color(themeColor.getRed(), themeColor.getGreen(), themeColor.getBlue(), alpha);

            Vec3d cam = mc.getEntityRenderDispatcher().camera.getPos();

            matrices.push();
            matrices.translate(
                    ev.origin.x - cam.x,
                    ev.origin.y - cam.y,
                    ev.origin.z - cam.z
            );

            Render3D.drawTextureVivid(
                    matrices,
                    -r, 0, -r,
                    r, 0, r,
                    getSelectedTexture(),
                    drawColor,
                    1.0f
            );

            matrices.pop();
        }
    }

    private Identifier getSelectedTexture() {
        if (texFirst.getValue() && texSecond.getValue()) texSecond.setValue(false);
        if (!texFirst.getValue() && !texSecond.getValue()) texFirst.setValue(true);
        return texFirst.getValue() ? texJump : texCircle2;
    }

    private static float easeOutCirc(float t) {
        return (float) Math.sqrt(1.0 - Math.pow(t - 1.0, 2.0));
    }

    private static float bothSine(float t) {
        return (float) (-(Math.cos(Math.PI * t) - 1.0) / 2.0);
    }

    // ── Data ─────────────────────────────────────────────────────────────────

    private static class JumpEvent {
        final Vec3d origin;
        final List<HighlightBlock> blocks; // null для texture mode
        final long spawnTime = System.currentTimeMillis();
        final long ttl;

        JumpEvent(Vec3d origin, List<HighlightBlock> blocks, long ttl) {
            this.origin = origin;
            this.blocks = blocks;
            this.ttl    = ttl;
        }
    }

    private record HighlightBlock(BlockPos pos, float dist) {}
}