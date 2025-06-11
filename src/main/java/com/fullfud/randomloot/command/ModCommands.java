// Файл: src/main/java/com/fullfud/randomloot/command/ModCommands.java
package com.fullfud.randomloot.command;

import com.fullfud.randomloot.block.entity.ConfigChestBlockEntity;
import com.fullfud.randomloot.item.ModItems;
import com.fullfud.randomloot.managers.ConfigSessionManager;
import com.fullfud.randomloot.util.ConfigStorage;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import java.util.List;

public class ModCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("randomloot")
            .then(Commands.literal("chest")
                .then(Commands.literal("create")
                    .requires(source -> source.hasPermission(2))
                    .then(Commands.argument("templateName", StringArgumentType.string())
                        .executes(context -> createChestTemplate(context.getSource(), StringArgumentType.getString(context, "templateName")))
                    )
                )
                .then(Commands.literal("open")
                    .requires(source -> source.hasPermission(2))
                    .then(Commands.argument("templateName", StringArgumentType.string())
                        .executes(context -> openConfigChest(context.getSource(), StringArgumentType.getString(context, "templateName")))
                    )
                )
                .then(Commands.literal("confirm")
                    .requires(source -> source.hasPermission(2))
                    .then(Commands.argument("templateName", StringArgumentType.string())
                        .executes(context -> confirmConfigChest(context.getSource(), StringArgumentType.getString(context, "templateName")))
                    )
                )
                .then(Commands.literal("setchance")
                    .requires(source -> source.hasPermission(2))
                    .then(Commands.argument("chance", IntegerArgumentType.integer(1, 100))
                        .executes(context -> setItemChance(context.getSource(), IntegerArgumentType.getInteger(context, "chance")))
                    )
                )
            )
        );
    }

    private static int createChestTemplate(CommandSourceStack source, String templateName) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("Эту команду может выполнять только игрок."));
            return 0;
        }

        if (ConfigStorage.templateExists(templateName, player.getServer())) {
            source.sendFailure(Component.literal("Шаблон с именем '" + templateName + "' уже существует."));
            return 0;
        }

        ItemStack spawnerItem = new ItemStack(ModItems.LOOT_CHEST_SPAWNER.get());
        spawnerItem.getOrCreateTag().putString("templateName", templateName);
        spawnerItem.setHoverName(Component.literal("Установщик сундука: " + templateName).withStyle(ChatFormatting.YELLOW));

        player.getInventory().add(spawnerItem);
        source.sendSuccess(() -> Component.literal("Вы получили установщик для шаблона '" + templateName + "'. Поставьте его на землю."), true);
        return 1;
    }

    private static int openConfigChest(CommandSourceStack source, String templateName) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;

        ConfigSessionManager.ConfigSession session = ConfigSessionManager.getSession(player.getUUID());
        if (session == null || !session.templateName.equals(templateName)) {
            source.sendFailure(Component.literal("У вас нет активной сессии для шаблона '" + templateName + "'."));
            return 0;
        }

        BlockPos chestPos = session.chestPosition;
        BlockEntity be = player.level().getBlockEntity(chestPos);
        if (!(be instanceof ConfigChestBlockEntity)) {
            source.sendFailure(Component.literal("Не удалось найти сундук конфигурации. Возможно, он был сломан."));
            return 0;
        }

        be.getBlockState().getBlock().use(be.getBlockState(), player.level(), chestPos, player, player.getUsedItemHand(), null);
        return 1;
    }

    private static int confirmConfigChest(CommandSourceStack source, String templateName) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;

        ConfigSessionManager.ConfigSession session = ConfigSessionManager.getSession(player.getUUID());
        if (session == null || !session.templateName.equals(templateName)) {
            source.sendFailure(Component.literal("У вас нет активной сессии для шаблона '" + templateName + "'."));
            return 0;
        }

        ServerLevel world = player.serverLevel();
        BlockPos chestPos = session.chestPosition;
        BlockEntity be = world.getBlockEntity(chestPos);

        if (!(be instanceof ConfigChestBlockEntity configChest)) {
            source.sendFailure(Component.literal("Не удалось найти сундук конфигурации."));
            return 0;
        }

        List<ItemStack> items = configChest.getItems();
        if (items.isEmpty()) {
            source.sendFailure(Component.literal("Сундук пуст! Положите в него предметы для шаблона."));
            return 0;
        }

        session.items = items;
        world.removeBlock(chestPos, false);

        source.sendSuccess(() -> Component.literal("Конфигурация предметов сохранена. Теперь задайте шансы выпадения."), true);
        askForItemChance(player, session);
        return 1;
    }

    private static void askForItemChance(ServerPlayer player, ConfigSessionManager.ConfigSession session) {
        if (session.currentItemIndex >= session.items.size()) {
            finishConfiguration(player, session);
            return;
        }

        ItemStack currentItem = session.items.get(session.currentItemIndex);
        MutableComponent message = Component.literal("Предмет " + (session.currentItemIndex + 1) + "/" + session.items.size() + ": ")
                .append(currentItem.getDisplayName().copy().withStyle(ChatFormatting.AQUA))
                .append(Component.literal("\nЗадайте шанс его выпадения (в процентах):"));
        player.sendSystemMessage(message);

        MutableComponent buttons = Component.literal("[")
                .append(createChanceButton("10%", 10)).append(Component.literal("] ["))
                .append(createChanceButton("25%", 25)).append(Component.literal("] ["))
                .append(createChanceButton("50%", 50)).append(Component.literal("] ["))
                .append(createChanceButton("75%", 75)).append(Component.literal("] ["))
                .append(createChanceButton("100%", 100)).append(Component.literal("]"));
        player.sendSystemMessage(buttons);
    }

    private static Component createChanceButton(String label, int chance) {
        return Component.literal(label)
                .withStyle(style -> style
                        .withColor(ChatFormatting.GREEN)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/randomloot chest setchance " + chance))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Установить шанс " + chance + "%")))
                );
    }

    private static int setItemChance(CommandSourceStack source, int chance) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;

        ConfigSessionManager.ConfigSession session = ConfigSessionManager.getSession(player.getUUID());
        if (session == null || session.items == null) {
            source.sendFailure(Component.literal("У вас нет активной сессии настройки."));
            return 0;
        }

        session.chances.put(session.currentItemIndex, chance);
        ItemStack currentItem = session.items.get(session.currentItemIndex);
        source.sendSuccess(() -> Component.literal("Шанс " + chance + "% для ").append(currentItem.getDisplayName()).append(Component.literal(" установлен.")), false);

        session.currentItemIndex++;
        askForItemChance(player, session);
        return 1;
    }

    private static void finishConfiguration(ServerPlayer player, ConfigSessionManager.ConfigSession session) {
        player.sendSystemMessage(Component.literal("Настройка шаблона '" + session.templateName + "' завершена!")
                .withStyle(ChatFormatting.GOLD));
        
        ConfigStorage.saveTemplate(session, player.getServer());
        
        ConfigSessionManager.endSession(player.getUUID());
    }
}