package dev.dontvisuals;


import dev.dontvisuals.client.managers.*;
import dev.dontvisuals.client.ui.mainmenu.MainMenu;
import dev.dontvisuals.client.util.Wrapper;
import dev.dontvisuals.modules.manager.ServerManager;
import meteordevelopment.orbit.EventBus;
import meteordevelopment.orbit.IEventBus;
import dev.dontvisuals.client.ui.clickgui.ClickGui;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import lombok.*;

import java.io.File;
import java.lang.invoke.MethodHandles;

@Getter
public class  dontvisuals implements ModInitializer, Wrapper {

    @Getter private static dontvisuals instance;

    private IEventBus eventHandler;
    private long initTime;
    private ModuleManager moduleManager;
    private CommandManager commandManager;
    private ConfigManager configManager;
    private AutoSaveManager autoSaveManager;
    private NotifyManager notifyManager;
    private PerformanceManager performanceManager;
    private ClickGui clickGui;
    private HudManager hudManager;
    private dev.dontvisuals.client.managers.AltManager altManager;
    private MainMenu mainmenu;
    private dev.dontvisuals.client.ui.hud.impl.WaypointOverlay waypointOverlay;
    private ServerManager serverManager;

    public static Logger LOGGER = LogManager.getLogger(dontvisuals.class);
    private final File globalsDir = new File(mc.runDirectory, "dontvisuals");
    private final File configsDir = new File(globalsDir, "configs");

    @Override
    public void onInitialize() {
        LOGGER.info("[dontvisuals] Starting initialization.");
        initTime = System.currentTimeMillis();
        instance = this;

        serverManager = new ServerManager();

        createDirs(globalsDir, configsDir);
        eventHandler = new EventBus();

        eventHandler.registerLambdaFactory("dev.dontvisuals",
                (lookupInMethod, klass) -> (MethodHandles.Lookup) lookupInMethod.invoke(null, klass, MethodHandles.lookup())
        );

        FriendsManager.init(globalsDir);
        AltManager.init(globalsDir);
        String lastAlt = AltManager.getLastUsedNickname();
        if (lastAlt != null && !lastAlt.isEmpty()) {
            AltManager.applyNickname(lastAlt);
        }

        notifyManager = new NotifyManager();
        performanceManager = new PerformanceManager();
        moduleManager = new ModuleManager();
        commandManager = new CommandManager();
        configManager = new ConfigManager();
        autoSaveManager = new AutoSaveManager();
        clickGui = new ClickGui();
        hudManager = new HudManager();
        mainmenu = new MainMenu();

        // Always-on waypoint overlay
        waypointOverlay = new dev.dontvisuals.client.ui.hud.impl.WaypointOverlay();
        eventHandler.subscribe(waypointOverlay);

        // Загружаем автоматически сохраненную конфигурацию
        autoSaveManager.loadAutoSave();

        // Регистрация события для замены TitleScreen на MainMenu
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.currentScreen instanceof TitleScreen && !(client.currentScreen instanceof MainMenu)) {
                client.setScreen(mainmenu);
            }
        });

        LOGGER.info("[dontvisuals] Successfully initialized for {} ms.", System.currentTimeMillis() - initTime);
    }

    private void createDirs(File... file) {
        for (File f : file) f.mkdirs();
    }

    public static Identifier id(String texture) {
        return Identifier.of("dontvisuals", texture);
    }
}