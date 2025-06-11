package com.fullfud.randomloot.util;

import com.fullfud.randomloot.ExampleMod;
import com.fullfud.randomloot.managers.ConfigSessionManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.LevelResource;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class ConfigStorage {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public record LootTemplate(List<ItemEntry> items) {
        public List<ItemStack> getItemsToSpawn(Random random) {
            List<ItemStack> result = new ArrayList<>();
            if (items == null) return result;

            for (ItemEntry entry : items) {
                if (random.nextInt(100) < entry.chance()) {
                    result.add(entry.toItemStack());
                }
            }
            return result;
        }
    }

    public record ItemEntry(String itemId, int count, int chance, @Nullable CompoundTag nbt) {
        public ItemEntry(ItemStack stack, int chance) {
            this(
                    BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(),
                    stack.getCount(),
                    chance,
                    stack.hasTag() ? stack.getTag().copy() : null
            );
        }

        public ItemStack toItemStack() {
            Item item = BuiltInRegistries.ITEM.get(new ResourceLocation(itemId));
            ItemStack stack = new ItemStack(item, count);
            if (nbt != null) {
                stack.setTag(nbt.copy());
            }
            return stack;
        }
    }

    private static Path getConfigDir(MinecraftServer server) {
        return server.getWorldPath(new LevelResource("serverconfig")).resolve(ExampleMod.MODID);
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
            server.getPlayerList().getPlayer(session.ownerUUID)
                    .sendSystemMessage(Component.literal("Шаблон '" + session.templateName + "' успешно сохранен!").withStyle(ChatFormatting.GREEN));
        } catch (IOException e) {
            server.getPlayerList().getPlayer(session.ownerUUID)
                    .sendSystemMessage(Component.literal("Ошибка при сохранении шаблона: " + e.getMessage()).withStyle(ChatFormatting.RED));
            e.printStackTrace();
        }
    }

    @Nullable
    public static LootTemplate loadTemplate(String templateName, MinecraftServer server) {
        Path templateFile = getConfigDir(server).resolve(templateName + ".json");
        if (!Files.exists(templateFile)) {
            return null;
        }
        try {
            String json = Files.readString(templateFile);
            return GSON.fromJson(json, LootTemplate.class);
        } catch (IOException | JsonSyntaxException e) {
            e.printStackTrace();
            return null;
        }
    }
}