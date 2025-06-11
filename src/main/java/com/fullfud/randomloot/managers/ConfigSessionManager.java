package com.fullfud.randomloot.managers;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ConfigSessionManager {
    // Хранит сессию для каждого игрока: UUID игрока -> его сессия
    private static final Map<UUID, ConfigSession> SESSIONS = new HashMap<>();

    public static void startSession(UUID playerId, String templateName, BlockPos pos) {
        SESSIONS.put(playerId, new ConfigSession(templateName, pos));
    }

    public static ConfigSession getSession(UUID playerId) {
        return SESSIONS.get(playerId);
    }
    
    public static void endSession(UUID playerId) {
        SESSIONS.remove(playerId);
    }

    // Внутренний класс для хранения данных сессии
    public static class ConfigSession {
        public final String templateName;
        public final BlockPos chestPosition;
        public List<ItemStack> items; // Список предметов из сундука
        public Map<Integer, Integer> chances; // Индекс предмета -> его шанс
        public int currentItemIndex = 0; // Для пошаговой настройки

        public ConfigSession(String templateName, BlockPos pos) {
            this.templateName = templateName;
            this.chestPosition = pos;
            this.chances = new HashMap<>();
        }
    }
}