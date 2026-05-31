package dev.dontvisuals.client.util.renderer;

import dev.dontvisuals.client.render.builders.*;
import dev.dontvisuals.client.render.builders.states.*;
import dev.dontvisuals.client.render.renderers.impl.*;
import dev.dontvisuals.client.util.Wrapper;

import dev.dontvisuals.client.util.renderer.fonts.Instance;
import lombok.experimental.UtilityClass;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

import java.awt.*;
import java.awt.image.BufferedImage;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.*;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.Tessellator;
import org.joml.Matrix4f;


@UtilityClass
public class Render2D implements Wrapper {

    public void drawRoundedRect(MatrixStack stack, float x, float y, float width, float height, float radius, Color color) {
        BuiltRectangle built = Builder.rectangle()
                .size(new SizeState(width, height))
                .radius(new QuadRadiusState(radius))
                .color(new QuadColorState(color))
                .build();
        built.render(stack.peek().getPositionMatrix(), x, y);
    }

    public void drawRockstarRoundedRect(MatrixStack stack, float x, float y, float width, float height, float radius, Color color) {
        RockstarBuiltRectangle built = Builder.rockstarRectangle()
                .size(new SizeState(width, height))
                .radius(new QuadRadiusState(radius))
                .color(new QuadColorState(color))
                .build();
        built.render(stack.peek().getPositionMatrix(), x, y);
    }

    public void drawBlurredRect(MatrixStack stack, float x, float y, float width, float height, float radius, float blurRadius, Color color) {
        int layers = 3;
        float baseAlpha = color.getAlpha();
        for (int i = 0; i < layers; i++) {
            float falloff = 1f - (i + 1f) / layers;
            int a = Math.max(0, Math.min(255, (int)(baseAlpha * (0.6f * falloff + 0.2f))));
            float spread = Math.max(1f, Math.min(12f, blurRadius * 0.5f)) * (i + 1) * 0.5f;
            drawRoundedRect(stack, x - spread, y - spread,
                    width + spread * 2f, height + spread * 2f,
                    radius + i * 0.6f,
                    new Color(color.getRed(), color.getGreen(), color.getBlue(), a));
        }
    }

    public void drawRockstarBlurredRect(MatrixStack stack, float x, float y, float width, float height, float radius, float blurRadius, Color color) {
        RockstarBuiltBlur built = Builder.rockstarBlur()
                .size(new SizeState(width, height))
                .radius(new QuadRadiusState(radius))
                .color(new QuadColorState(color))
                .build();
        built.render(stack.peek().getPositionMatrix(), x, y);
        drawRockstarRoundedRect(stack, x, y, width, height, radius,
                new Color(color.getRed(), color.getGreen(), color.getBlue(), 60));
    }

    public void drawShaderBlurRect(MatrixStack stack, float x, float y, float width, float height, float radius, float blurRadius, Color color) {
        BuiltBlur built = Builder.blur()
                .size(new SizeState(width, height))
                .radius(new QuadRadiusState(radius))
                .blurRadius(blurRadius)
                .color(new QuadColorState(color))
                .build();
        built.render(stack.peek().getPositionMatrix(), x, y);
    }

    public void drawBorder(MatrixStack stack, float x, float y, float width, float height, float radius, float internalSmoothness, float externalSmoothness, Color color) {
        BuiltBorder built = Builder.border()
                .size(new SizeState(width, height))
                .radius(new QuadRadiusState(radius))
                .smoothness(internalSmoothness, externalSmoothness)
                .color(new QuadColorState(color))
                .build();
        built.render(stack.peek().getPositionMatrix(), x, y);
    }

    public void drawRoundedRect2(MatrixStack stack, float x, float y, float width, float height, float radius1, float radius2, float radius3, float radius4, Color color) {
        BuiltRectangle built = Builder.rectangle()
                .size(new SizeState(width, height))
                .radius(new QuadRadiusState(radius1, radius2, radius3, radius4))
                .color(new QuadColorState(color))
                .build();
        built.render(stack.peek().getPositionMatrix(), x, y);
    }

    public void drawRoundedGradientRect(MatrixStack stack, float x, float y, float width, float height, float radius, Color topColor, Color bottomColor) {
        BuiltRectangle top = Builder.rectangle()
                .size(new SizeState(width, height / 2f))
                .radius(new QuadRadiusState(radius, radius, 0, 0))
                .color(new QuadColorState(topColor, topColor, topColor, topColor))
                .build();
        top.render(stack.peek().getPositionMatrix(), x, y);

        BuiltRectangle bot = Builder.rectangle()
                .size(new SizeState(width, height / 2f))
                .radius(new QuadRadiusState(0, 0, radius, radius))
                .color(new QuadColorState(bottomColor, bottomColor, bottomColor, bottomColor))
                .build();
        bot.render(stack.peek().getPositionMatrix(), x, y + height / 2f);

        BuiltRectangle mid = Builder.rectangle()
                .size(new SizeState(width, height * 0.35f))
                .radius(new QuadRadiusState(0))
                .color(new QuadColorState(topColor, bottomColor, bottomColor, topColor))
                .build();
        mid.render(stack.peek().getPositionMatrix(), x, y + height * 0.325f);
    }

    public void drawStyledRect(MatrixStack stack, float x, float y, float width, float height, float radius, Color color, int blurAlpha) {
        drawBlurredRect(stack, x, y, width, height, radius, 10f, new Color(255, 255, 255, blurAlpha));
        drawRoundedRect(stack, x, y, width, height, radius, color);
    }

    public void drawRect(MatrixStack stack, float x, float y, float width, float height, Color color) {
        BuiltRectangle built = Builder.rectangle()
                .size(new SizeState(width, height))
                .radius(new QuadRadiusState(0))
                .color(new QuadColorState(color))
                .smoothness(0f)
                .build();
        built.render(stack.peek().getPositionMatrix(), x, y);
    }

    public void drawGradientRect(MatrixStack stack, float x, float y, float width, float height, Color start, Color end, boolean horizontal) {
        QuadColorState quad;
        if (horizontal) {
            // TL=start, TR=end, BR=end, BL=start  (left → right)
            quad = new QuadColorState(start, end, end, start);
        } else {
            // TL=start, TR=start, BR=end, BL=end  (top → bottom)
            quad = new QuadColorState(start, start, end, end);
        }
        BuiltRectangle built = Builder.rectangle()
                .size(new SizeState(width, height))
                .radius(new QuadRadiusState(0))
                .color(quad)
                .build();
        built.render(stack.peek().getPositionMatrix(), x, y);
    }

    public void drawFont(MatrixStack stack, Instance instance, String text, float x, float y, Color color) {
        BuiltText built = Builder.text()
                .size(instance.size())
                .font(instance.font())
                .text(text)
                .thickness(0.05f)
                .color(color)
                .build();
        built.render(stack.peek().getPositionMatrix(), x, y);
    }

    public void drawTexture(MatrixStack stack, float x, float y, float width, float height, float radius, Identifier texture, Color color) {
        drawTexture(stack, x, y, width, height, radius, mc.getTextureManager().getTexture(texture), color);
    }

    public void drawTexture(MatrixStack stack, float x, float y, float width, float height, float radius, AbstractTexture texture, Color color) {
        BuiltTexture built = Builder.texture()
                .size(new SizeState(width, height))
                .radius(new QuadRadiusState(radius))
                .texture(1f, 1f, 1f, 1f, texture)
                .color(new QuadColorState(color))
                .build();
        built.render(stack.peek().getPositionMatrix(), x, y);
    }

    public void drawTexture(MatrixStack stack, float x, float y, float width, float height, float radius, float u, float v, float textWidth, float texHeight, Identifier texture, Color color) {
        BuiltTexture built = Builder.texture()
                .size(new SizeState(width, height))
                .radius(new QuadRadiusState(radius))
                .texture(u, v, textWidth, texHeight, mc.getTextureManager().getTexture(texture))
                .color(new QuadColorState(color))
                .build();
        built.render(stack.peek().getPositionMatrix(), x, y);
    }

    public void startScissor(DrawContext context, float x, float y, float width, float height) {
        context.enableScissor((int) x, (int) y, (int)(x + width), (int)(y + height));
    }

    public void stopScissor(DrawContext context) {
        context.disableScissor();
    }

    public AbstractTexture convert(BufferedImage image) {
        int width = image.getWidth(), height = image.getHeight();
        NativeImage img = new NativeImage(width, height, false);
        for (int y = 0; y < height; y++)
            for (int x = 0; x < width; x++)
                img.setColorArgb(x, y, image.getRGB(x, y));
        return new NativeImageBackedTexture(img);
    }

    public void drawLine(MatrixStack stack, float x1, float y1, float x2, float y2, float width, Color color) {
        float dx = x2 - x1, dy = y2 - y1;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length <= 0.5f) return;
        float angle = (float) Math.atan2(dy, dx);
        BuiltRectangle built = Builder.rectangle()
                .size(new SizeState(length, width))
                .radius(new QuadRadiusState(0))
                .color(new QuadColorState(color))
                .build();
        stack.push();
        stack.translate(x1, y1, 0);
        stack.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Z.rotation(angle));
        built.render(stack.peek().getPositionMatrix(), 0, -width / 2f);
        stack.pop();
    }

    public void drawGlow(MatrixStack stack, float x, float y, float width, float height, float radius, Color color, float intensity, int layers) {
        float step = intensity / layers;
        float blurStep = radius / layers;
        for (int i = 0; i < layers; i++) {
            float alpha = step * (i + 1);
            float layerRadius = radius + blurStep * i;
            float offset = i * 2;
            drawBlurredRect(stack, x - offset, y - offset,
                    width + offset * 2, height + offset * 2,
                    layerRadius, layerRadius,
                    new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) alpha));
        }
    }

    public void drawTextGlow(MatrixStack stack, float x, float y, float width, float height, float radius, Color color, int glowAlpha, int layers) {
        for (int i = layers; i > 0; i--) {
            float factor = i / (float) layers;
            float offset = i * 1.5f;
            int alpha = (int)(glowAlpha * factor);
            drawBlurredRect(stack, x - offset, y - offset,
                    width + offset * 2, height + offset * 2,
                    radius + offset / 2f, radius + offset / 2f,
                    new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha));
        }
    }

    public void drawGlowOutline(MatrixStack stack, float x, float y, float width, float height, float radius, Color glowColor, int intensity, float glowRadius) {
        drawBorder(stack, x - glowRadius, y - glowRadius,
                width + glowRadius * 2, height + glowRadius * 2,
                radius + glowRadius, 1f, glowRadius,
                new Color(glowColor.getRed(), glowColor.getGreen(), glowColor.getBlue(), intensity));
    }

    public void drawCornerOutline(MatrixStack stack, float x, float y, float width, float height, float cornerSize, Color color) {
        float t = 2f;
        drawRect(stack, x, y, cornerSize, t, color);
        drawRect(stack, x, y, t, cornerSize, color);
        drawRect(stack, x + width - cornerSize, y, cornerSize, t, color);
        drawRect(stack, x + width - t, y, t, cornerSize, color);
        drawRect(stack, x, y + height - t, cornerSize, t, color);
        drawRect(stack, x, y + height - cornerSize, t, cornerSize, color);
        drawRect(stack, x + width - cornerSize, y + height - t, cornerSize, t, color);
        drawRect(stack, x + width - t, y + height - cornerSize, t, cornerSize, color);
    }

    public void drawRoundedCorner(MatrixStack stack, float x, float y, float width, float height,
                                  float cornerSize, Color cornerColor, float thickness) {
        if (cornerColor.getAlpha() <= 0) return;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(cornerColor.getRed() / 255f, cornerColor.getGreen() / 255f,
                cornerColor.getBlue() / 255f, cornerColor.getAlpha() / 255f);
        org.joml.Matrix4f matrix = stack.peek().getPositionMatrix();
        drawCornerElement(matrix, x, y, cornerSize, thickness, true, true);
        drawCornerElement(matrix, x + width - cornerSize, y, cornerSize, thickness, false, true);
        drawCornerElement(matrix, x, y + height - cornerSize, cornerSize, thickness, true, false);
        drawCornerElement(matrix, x + width - cornerSize, y + height - cornerSize, cornerSize, thickness, false, false);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.disableBlend();
    }

    private void drawCornerElement(org.joml.Matrix4f matrix, float x, float y, float size,
                                   float thickness, boolean left, boolean top) {
        drawQuad(matrix, left ? x : x + size - thickness,
                top ? y : y + size - thickness,
                left ? size : thickness, thickness);
        drawQuad(matrix, left ? x : x + size - thickness,
                top ? y : y + size - thickness,
                thickness, left ? thickness : size);
    }

    private void drawQuad(org.joml.Matrix4f matrix, float x, float y, float w, float h) {
        BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        buf.vertex(matrix, x,     y,     0).color(1f, 1f, 1f, 1f);
        buf.vertex(matrix, x + w, y,     0).color(1f, 1f, 1f, 1f);
        buf.vertex(matrix, x + w, y + h, 0).color(1f, 1f, 1f, 1f);
        buf.vertex(matrix, x,     y + h, 0).color(1f, 1f, 1f, 1f);
        BufferRenderer.drawWithGlobalProgram(buf.end());
    }

    public void drawRoundedCorner(MatrixStack stack, float x, float y, float width, float height,
                                  float cornerSize, Color cornerColor) {
        drawRoundedCorner(stack, x, y, width, height, cornerSize, cornerColor, 2f);
    }

    // =========================================================================
    //  НОВЫЕ МЕТОДЫ
    // =========================================================================

    /**
     * Горизонтальный градиент со скруглёнными углами (left → right).
     * Использует QuadColorState напрямую — никаких артефактов от двух перекрывающихся rect.
     */
    public void drawRoundedGradientRectH(MatrixStack stack, float x, float y,
                                         float width, float height, float radius,
                                         Color leftColor, Color rightColor) {
        // QuadColorState(topLeft, topRight, bottomRight, bottomLeft)
        BuiltRectangle built = Builder.rectangle()
                .size(new SizeState(width, height))
                .radius(new QuadRadiusState(radius))
                .color(new QuadColorState(leftColor, rightColor, rightColor, leftColor))
                .build();
        built.render(stack.peek().getPositionMatrix(), x, y);
    }

    /**
     * Плавное рассеянное свечение через drawBorder с большой externalSmoothness.
     * drawBorder использует SDF-шейдер и даёт настоящий smooth fade без ступенек.
     *
     * @param color     цвет свечения
     * @param intensity альфа свечения (0–255), рекомендуется 40–90
     * @param spread    радиус рассеивания в пикселях (рекомендуется 8–20)
     */
    public void drawSoftGlow(MatrixStack stack, float x, float y, float width, float height,
                             float radius, Color color, int intensity, int layers, float spread) {
        // Используем drawBorder с нулевой внутренней плавностью и большой внешней —
        // это SDF border shader который даёт настоящий gaussian-like fade наружу
        drawBorder(
                stack,
                x - spread, y - spread,
                width + spread * 2f, height + spread * 2f,
                radius + spread,
                0.5f,       // internalSmoothness — тонкая внутренняя граница
                spread,     // externalSmoothness — вся плавность уходит наружу
                new Color(color.getRed(), color.getGreen(), color.getBlue(),
                        Math.max(0, Math.min(255, intensity)))
        );
    }
}