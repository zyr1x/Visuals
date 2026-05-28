package dev.dontvisuals.modules.impl.render;

import dev.dontvisuals.dontvisuals;
import dev.dontvisuals.client.ChatUtils;
import dev.dontvisuals.client.events.impl.EventTick;
import dev.dontvisuals.client.ui.clickgui.ClickGui;
import dev.dontvisuals.modules.api.Category;
import dev.dontvisuals.modules.api.Module;
import dev.dontvisuals.modules.settings.api.Bind;
import dev.dontvisuals.modules.settings.impl.BooleanSetting;
import dev.dontvisuals.modules.settings.impl.ListSetting;
import meteordevelopment.orbit.EventHandler;
import org.lwjgl.glfw.GLFW;
import net.minecraft.client.resource.language.I18n;

public class UI extends Module {


    public UI() {
        super("UI", Category.Render, I18n.translate("module.ui.description"));
        setBind(new Bind(GLFW.GLFW_KEY_RIGHT_SHIFT, false));

    }

    @EventHandler
    public void onTick(EventTick e) {
        if (!(mc.currentScreen instanceof ClickGui) && !(mc.currentScreen instanceof ClickGui)) {
            setToggled(false);
        }
    }

    @Override
    public void onEnable() {
        super.onEnable();

		// Allow opening only when in a world
		if (mc.player == null || mc.world == null) {
			ChatUtils.sendMessage(I18n.translate("dontvisuals.ui.onlyInWorld"));
			setToggled(false);
			return;
		}

		mc.setScreen(dontvisuals.getInstance().getClickGui());


    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (mc.currentScreen instanceof ClickGui) {
            ((ClickGui) mc.currentScreen).close();
        }
    }
}