package com.fullfud.randomloot.managers;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ConfigSessionManager {
    private static final Map<UUID, ConfigSession> SESSIONS = new HashMap<>();

    public static void startSession(UUID playerId, String templateName, BlockPos pos) {
        SESSIONS.put(playerId, new ConfigSession(playerId, templateName, pos));
    }

    public static ConfigSession getSession(UUID playerId) {
        return SESSIONS.get(playerId);
    }

    public static void endSession(UUID playerId) {
        SESSIONS.remove(playerId);
    }

    public static class ConfigSession {
        public final UUID ownerUUID;
        public final String templateName;
        public final BlockPos chestPosition;
        public List<ItemStack> items;
        public Map<Integer, Integer> chances;
        public int currentItemIndex = 0;

        public ConfigSession(UUID ownerUUID, String templateName, BlockPos pos) {
            this.ownerUUID = ownerUUID;
            this.templateName = templateName;
            this.chestPosition = pos;
            this.chances = new HashMap<>();
        }
    }
}