package dev.dontvisuals.util;

import dev.firstdark.rpc.DiscordRpc;
import dev.firstdark.rpc.enums.ActivityType;
import dev.firstdark.rpc.models.DiscordRichPresence;
import dev.firstdark.rpc.enums.ErrorCode;
import dev.firstdark.rpc.models.User;
import dev.firstdark.rpc.handlers.RPCEventHandler;

public class DiscordRichPresenceUtil {
    private static final String DEFAULT_APP_ID = "1509598467918528772";

    private static volatile boolean running = false;
    private static Thread rpcThread;
    private static DiscordRpc rpc;

    public static String state;

    public static synchronized void discordrpc() {
        startDiscord(null);
    }

    public static synchronized void startDiscord(String applicationId) {
        if (running) return;
        String appId = applicationId;
        if (appId == null || appId.isEmpty()) {
            appId = System.getProperty("discord.app.id", System.getenv("DISCORD_APP_ID"));
        }
        if (appId == null || appId.isEmpty()) appId = DEFAULT_APP_ID;

        rpc = new DiscordRpc();
        rpc.setDebugMode(false);

        RPCEventHandler handler = new RPCEventHandler() {
            @Override
            public void ready(User user) {
                running = true;
                System.out.println("[DiscordRPC] Ready! User: " + user.getUsername());
                pushPresence();
            }

            @Override
            public void disconnected(ErrorCode errorCode, String message) {
                running = false;
                System.out.println("[DiscordRPC] Disconnected: " + errorCode + " - " + message);
            }

            @Override
            public void errored(ErrorCode errorCode, String message) {
                System.out.println("[DiscordRPC] Error: " + errorCode + " - " + message);
            }
        };

        try {
            rpc.init(appId, handler, false);
        } catch (Throwable t) {
            System.out.println("[DiscordRPC] Init failed: " + t.getMessage());
            return;
        }

        rpcThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(2000);
                    if (running) pushPresence();
                } catch (InterruptedException e) {
                    break;
                } catch (Throwable ignored) {}
            }
        }, "Discord-RPC-FirstDark-Thread");
        rpcThread.setDaemon(true);
        rpcThread.start();
    }

    public static synchronized void shutdownDiscord() {
        running = false;
        if (rpcThread != null) {
            rpcThread.interrupt();
            rpcThread = null;
        }
        try { if (rpc != null) rpc.shutdown(); } catch (Throwable ignored) {}
        rpc = null;
    }

    private static void pushPresence() {
        if (rpc == null) return;
        DiscordRichPresence presence = DiscordRichPresence.builder()
                .details("DontVisuals")
                .state(state != null && !state.isEmpty() ? state : "t.me/dontvisuals")
                .largeImageText("DontVisuals")
                .smallImageText("Playing")
                .activityType(ActivityType.PLAYING)
                .button(DiscordRichPresence.RPCButton.of("Скачать", "https://t.me/dontvisuals"))
                .build();
        try { rpc.updatePresence(presence); } catch (Throwable ignored) {}
    }
}