package dev.dontvisuals.modules.manager;

import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.option.ServerList;

public class ServerManager {

    // Твой сервер — меняй как хочешь
    public static final String MY_SERVER_ADDRESS = "mc.donttime.space";
    public static final String MY_SERVER_NAME    = "САМЫЙ ПИЗДАТЫЙ СЕРВЕР";

    /**
     * Возвращает true, если переданный ServerInfo — твой сервер.
     */
    public static boolean isMyServer(ServerInfo info) {
        if (info == null) return false;
        return info.address.equalsIgnoreCase(MY_SERVER_ADDRESS);
    }

    /**
     * Переставляет твой сервер на первое место в ServerList.
     * Вызывается миксином после загрузки списка.
     */
    public static void prioritizeMyServer(ServerList serverList) {
        // Ищем наш сервер
        ServerInfo mine = null;
        for (int i = 0; i < serverList.size(); i++) {
            if (isMyServer(serverList.get(i))) {
                mine = serverList.get(i);
                break;
            }
        }

        // Если нашего сервера нет — добавляем
        if (mine == null) {
            mine = new ServerInfo(MY_SERVER_NAME, MY_SERVER_ADDRESS, ServerInfo.ServerType.OTHER);
            serverList.add(mine, false);
            // Поднимаем на первое место
            for (int i = serverList.size() - 1; i > 0; i--) {
                serverList.swapEntries(i, i - 1);
            }
        } else {
            // Уже есть — находим индекс и тащим наверх
            int idx = -1;
            for (int i = 0; i < serverList.size(); i++) {
                if (isMyServer(serverList.get(i))) { idx = i; break; }
            }
            for (int i = idx; i > 0; i--) {
                serverList.swapEntries(i, i - 1);
            }
        }

        serverList.saveFile();
    }
}