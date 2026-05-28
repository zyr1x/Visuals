package dev.dontvisuals.client.ui.clickgui.components.impl;

import dev.dontvisuals.modules.settings.api.Bind;
import dev.dontvisuals.modules.settings.impl.BindSetting;
import dev.dontvisuals.client.ui.clickgui.components.Component;
import dev.dontvisuals.client.util.animations.Animation;
import dev.dontvisuals.client.util.animations.Easing;
import dev.dontvisuals.client.util.animations.infinity.InfinityAnimation;
import dev.dontvisuals.client.util.math.MathUtils;
import dev.dontvisuals.client.util.renderer.Render2D;
import dev.dontvisuals.client.util.renderer.fonts.Fonts;
import dev.dontvisuals.client.managers.ThemeManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.resource.language.I18n;
import org.lwjgl.glfw.GLFW;

import java.awt.*;

public class BindComponent extends Component {

	private final BindSetting setting;
	private final InfinityAnimation widthAnim = new InfinityAnimation(Easing.LINEAR);
	private final Animation bindingAnim       = new Animation(200, 1f, false, Easing.BOTH_SINE);
	private boolean binding;

	public BindComponent(BindSetting setting) {
		super(setting.getName());
		this.setting = setting;
	}

	@Override
	public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
		float ga = Math.max(0f, Math.min(1f, getGlobalAlpha()));
		bindingAnim.update(binding);
		float ba = (float) bindingAnim.getValue();

		Color accent    = ThemeManager.getInstance().getCurrentTheme().getAccentColor();
		Color themeText = ThemeManager.getInstance().getCurrentTheme().getTextColor();

		String bindText = setting.getValue().toString().replace("_", " ");
		float  bw       = Fonts.REGULAR.getWidth(bindText, 7f);
		float  animW    = widthAnim.animate(bw + 10f, 160);

		// ── Фон строки ──────────────────────────────────────────────────────
		Render2D.drawRoundedRect(ctx.getMatrices(), x, y, width, height, 5f,
				new Color(255, 255, 255, (int) (8 * ga)));

		// ── Название ─────────────────────────────────────────────────────────
		Render2D.drawFont(ctx.getMatrices(), Fonts.BOLD.getFont(7.5f),
				I18n.translate(setting.getName()),
				x + 6f, y + (height - Fonts.BOLD.getHeight(7.5f)) / 2f,
				new Color(themeText.getRed(), themeText.getGreen(), themeText.getBlue(),
						(int) (themeText.getAlpha() * ga)));

		// ── Бейдж с бинд-значением ───────────────────────────────────────────
		float badgeX = x + width - animW - 6f;
		float badgeY = y + (height - (height - 6f)) / 2f;
		float badgeH = height - 6f;
		Render2D.drawRoundedRect(ctx.getMatrices(), badgeX, badgeY, animW, badgeH, 4f,
				new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), (int) (150 * ga)));

		// Текст бинда (нормальный)
		int normA = (int) (255 * (1f - ba) * ga);
		if (normA > 0)
			Render2D.drawFont(ctx.getMatrices(), Fonts.BOLD.getFont(7f), bindText,
					badgeX + (animW - bw) / 2f,
					badgeY + (badgeH - Fonts.BOLD.getHeight(7f)) / 2f,
					new Color(255, 255, 255, normA));

		// "..." при ожидании нажатия
		int dotsA = (int) (255 * ba * ga);
		if (dotsA > 0) {
			String dots = "...";
			float  dw   = Fonts.BOLD.getWidth(dots, 7f);
			Render2D.drawFont(ctx.getMatrices(), Fonts.BOLD.getFont(7f), dots,
					badgeX + (animW - dw) / 2f,
					badgeY + (badgeH - Fonts.BOLD.getHeight(7f)) / 2f,
					new Color(255, 255, 255, dotsA));
		}
	}

	@Override
	public void mouseClicked(double mx, double my, int btn) {
		String bindText = setting.getValue().toString().replace("_", " ");
		float  bw       = Fonts.REGULAR.getWidth(bindText, 7f);
		float  badgeX   = x + width - bw - 16f;
		float  badgeH   = height - 6f;

		if (!binding && btn == 0
				&& MathUtils.isHovered(badgeX, y + 3f, bw + 10f, badgeH, (float) mx, (float) my)) {
			binding = true;
			return;
		}
		if (binding) {
			Bind.Mode mode = setting.getValue() != null ? setting.getValue().getMode() : Bind.Mode.TOGGLE;
			setting.setValue(new Bind(btn, true, mode));
			binding = false;
		}
	}

	@Override public void mouseReleased(double mx, double my, int btn) {}

	@Override
	public void keyPressed(int key, int scan, int mods) {
		if (binding) {
			if (key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_DELETE)
				setting.setValue(new Bind(-1, false));
			else {
				Bind.Mode mode = setting.getValue() != null ? setting.getValue().getMode() : Bind.Mode.TOGGLE;
				setting.setValue(new Bind(key, false, mode));
			}
			binding = false;
		}
	}

	@Override public void keyReleased(int k, int s, int m) {}
	@Override public void charTyped(char c, int m) {}
}