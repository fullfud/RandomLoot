package com.fullfud.randomloot.util;

import com.fullfud.randomloot.ExampleMod;
import com.fullfud.randomloot.managers.ConfigSessionManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ConfigStorage {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // Структура для хранения данных в JSON
    private record LootTemplate(List<ItemEntry> items) {}
    private record ItemEntry(String itemId, int count, int chance, @Nullable CompoundTag nbt) {
        ItemEntry(ItemStack stack, int chance) {
            this(
                net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(),
                stack.getCount(),
                chance,
                stack.hasTag() ? stack.getTag().copy() : null
            );
        }
    }

    private static Path getConfigDir(MinecraftServer server) {
        return server.getWorldPath(net.minecraft.world.level.storage.LevelResource.SERVER_CONFIG_DIR).resolve(ExampleMod.MODID);
    }

    public static boolean templateExists(String templateName, MinecraftServer server) {
        Path configDir = getConfigDir(server);
        Path templateFile = configDir.resolve(templateName + ".json");
        return Files.exists(templateFile);
    }

    public static void saveTemplate(ConfigSessionManager.ConfigSession session, MinecraftServer server) {
        List<ItemEntry> itemEntries = session.items.stream()
                .map(stack -> new ItemEntry(stack, session.chances.getOrDefault(session.items.indexOf(stack), 100)))
                .collect(Collectors.toList());

        LootTemplate template = new LootTemplate(itemEntries);
        Path configDir = getConfigDir(server);

        try {
            if (!Files.exists(configDir)) {
                Files.createDirectories(configDir);
            }
            Path templateFile = configDir.resolve(session.templateName + ".json");
            Files.writeString(templateFile, GSON.toJson(template));
            server.getPlayerList().getPlayer(session.getOwnerUUID())
                  .sendSystemMessage(Component.literal("Шаблон '" + session.templateName + "' успешно сохранен!").withStyle(net.minecraft.ChatFormatting.GREEN));
        } catch (IOException e) {
            server.getPlayerList().getPlayer(session.getOwnerUUID())
                  .sendSystemMessage(Component.literal("Ошибка при сохранении шаблона: " + e.getMessage()).withStyle(net.minecraft.ChatFormatting.RED));
            e.printStackTrace();
        }
    }
}