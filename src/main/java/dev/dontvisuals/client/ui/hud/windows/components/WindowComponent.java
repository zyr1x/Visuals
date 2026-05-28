package dev.dontvisuals.client.ui.hud.windows.components;

import dev.dontvisuals.client.ui.hud.windows.components.impl.BooleanComponent;
import dev.dontvisuals.client.ui.clickgui.components.Component;
import dev.dontvisuals.client.util.animations.Animation;
import lombok.*;

@Getter @Setter
public abstract class WindowComponent extends Component {
	protected Animation animation;

	public WindowComponent(String name) {
		super(name);
	}
}