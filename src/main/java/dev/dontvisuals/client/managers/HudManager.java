package dev.dontvisuals.client.managers;

import dev.dontvisuals.client.ui.hud.impl.ArmorHUD;
import dev.dontvisuals.client.ui.hud.impl.Potions;
import dev.dontvisuals.client.util.math.MathUtils;
import dev.dontvisuals.modules.api.Module;
import dev.dontvisuals.client.events.impl.EventMouse;
import dev.dontvisuals.client.events.impl.EventRender2D;
import dev.dontvisuals.client.ui.hud.HudElement;
import dev.dontvisuals.client.ui.hud.impl.TargetHud;
import dev.dontvisuals.client.ui.hud.impl.Watermark;
import dev.dontvisuals.client.ui.hud.windows.Window;
import dev.dontvisuals.client.util.render.Wrapper;
import dev.dontvisuals.modules.settings.Setting;
import dev.dontvisuals.modules.settings.impl.BooleanSetting;
import dev.dontvisuals.modules.settings.impl.ListSetting;
import dev.dontvisuals.dontvisuals;
import lombok.Getter;
import lombok.Setter;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.ChatScreen;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static dev.dontvisuals.client.util.Wrapper.mc;

@Getter
public class HudManager implements Wrapper {

    @Setter private HudElement currentDragging;
    private final List<HudElement> hudElements = new ArrayList<>();
    protected final ListSetting elements = new ListSetting("setting.layout",
            new BooleanSetting("Watermark", true),
            new BooleanSetting("TargetHud", true),
            new BooleanSetting("Potions", true),
            new BooleanSetting("ArmorHUD", true),
            new BooleanSetting("Hotbar", true)
    );
    @Setter private Window window;

    public HudManager() {
        dontvisuals.getInstance().getEventHandler().subscribe(this);

        addElements(
                new Watermark(),
                new TargetHud(),
                new Potions(),
                new ArmorHUD()
        );

        for (HudElement element : hudElements) {
            try {
                for (Field field : element.getClass().getDeclaredFields()) {
                    if (!Setting.class.isAssignableFrom(field.getType())) continue;
                    field.setAccessible(true);
                    Setting<?> setting = (Setting<?>) field.get(element);
                    if (setting != null && !element.getSettings().contains(setting)) element.getSettings().add(setting);
                }
            } catch (Exception ignored) {}

            // Subscribe HUD elements to the event bus so they can render and handle input
            try {
                dontvisuals.getInstance().getEventHandler().subscribe(element);
            } catch (Throwable ignored) {}
        }
    }

    @EventHandler
    public void onRender2D(EventRender2D e) {
        if (Module.fullNullCheck()) return;

        if (window != null) {
            if (!(mc.currentScreen instanceof ChatScreen)) window.reset();

            if (window.closed()) {
                window = null;
                return;
            }

            window.render(e.getContext(), mouseX(), mouseY());
        }
    }

    @EventHandler
    public void onMouse(EventMouse e) {
        if (!(mc.currentScreen instanceof ChatScreen) || Module.fullNullCheck()) return;

        if (e.getAction() == 1) {
            if (window != null) {
                if (MathUtils.isHovered(window.getX(), window.getY(), window.getWidth(), window.getFinalHeight(), mouseX(), mouseY())) {
                    window.mouseClicked(mouseX(), mouseY(), e.getButton());
                    return;
                } else window.reset();
            }

            if (e.getButton() == 1) {
                // If right-click is over any HUD element area, let the element handle opening its own settings
                for (HudElement element : hudElements) {
                    if (MathUtils.isHovered(element.getX(), element.getY(), element.getWidth(), element.getHeight(), mouseX(), mouseY())) {
                        return;
                    }
                }

                // Otherwise, open the global elements window
                for (HudElement element : hudElements) {
                    if (element.getWindow() == null) continue;
                    if (element.getSettings().size() == 1) return;
                    element.getWindow().reset();
                }

                window = new Window(mouseX() + 3, mouseY() + 3, 100, 12.5f, List.of(elements));
            }
        }
    }

    public int mouseX() {
        return (int) (mc.mouse.getX() / mc.getWindow().getScaleFactor());
    }

    public int mouseY() {
        return (int) (mc.mouse.getY() / mc.getWindow().getScaleFactor());
    }

    private void addElements(HudElement... element) {
        this.hudElements.addAll(List.of(element));
    }
}