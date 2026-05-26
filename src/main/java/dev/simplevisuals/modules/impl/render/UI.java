package dev.simplevisuals.modules.impl.render;

import dev.simplevisuals.simplevisuals;
import dev.simplevisuals.client.ChatUtils;
import dev.simplevisuals.client.events.impl.EventTick;
import dev.simplevisuals.client.ui.clickgui.ClickGui;
import dev.simplevisuals.modules.api.Category;
import dev.simplevisuals.modules.api.Module;
import dev.simplevisuals.modules.settings.api.Bind;
import dev.simplevisuals.modules.settings.impl.BooleanSetting;
import dev.simplevisuals.modules.settings.impl.ListSetting;
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
			ChatUtils.sendMessage(I18n.translate("simplevisuals.ui.onlyInWorld"));
			setToggled(false);
			return;
		}

		mc.setScreen(simplevisuals.getInstance().getClickGui());


    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (mc.currentScreen instanceof ClickGui) {
            ((ClickGui) mc.currentScreen).close();
        }
    }
}