package com.fullfud.randomloot;

import com.fullfud.randomloot.block.ModBlocks;
import com.fullfud.randomloot.block.entity.ConfigChestBlockEntity;
import com.fullfud.randomloot.block.entity.ModBlockEntities;
import com.fullfud.randomloot.command.ModCommands;
import com.fullfud.randomloot.inventory.ModMenuTypes;
import com.fullfud.randomloot.item.ModItems;
import com.fullfud.randomloot.managers.ConfigSessionManager;
import com.fullfud.randomloot.screen.ConfigChestScreen;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import java.util.UUID;

@Mod(ExampleMod.MODID)
public class ExampleMod {
    public static final String MODID = "randomloot";

    public ExampleMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Регистрация компонентов мода
        ModItems.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModMenuTypes.register(modEventBus);

        // Регистрация обработчиков событий Forge
        MinecraftForge.EVENT_BUS.register(this);
    }

    // Обработчик для регистрации команд
    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        ModCommands.register(event.getDispatcher());
    }

    // Обработчик для установки нашего блока-конфигуратора
    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        ItemStack stack = event.getItemStack();
        if (stack.getItem() != ModItems.LOOT_CHEST_SPAWNER.get()) {
            return;
        }

        event.setCanceled(true);
        Level world = event.getLevel();
        if (world.isClientSide()) return;

        BlockPos posToPlace = event.getPos().relative(event.getFace());
        if (!world.getBlockState(posToPlace).canBeReplaced(null)) {
            return;
        }

        // Устанавливаем блок
        BlockState configBlockState = ModBlocks.CONFIG_CHEST.get().defaultBlockState();
        world.setBlock(posToPlace, configBlockState, 3);

        if (world.getBlockEntity(posToPlace) instanceof ConfigChestBlockEntity configEntity) {
            String templateName = stack.getOrCreateTag().getString("templateName");
            UUID playerUUID = event.getPlayer().getUUID();

            configEntity.setOwner(playerUUID);
            configEntity.setTemplateName(templateName);

            // Начинаем сессию конфигурации
            ConfigSessionManager.startSession(playerUUID, templateName, posToPlace);

            // Открываем GUI
            configBlockState.getBlock().use(configBlockState, world, posToPlace, event.getPlayer(), event.getHand(), event.getHitVec());
        }

        if (!event.getPlayer().isCreative()) {
            stack.shrink(1);
        }
    }

    // Вложенный класс для обработки событий только на стороне клиента
    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            // Регистрируем экран для нашего меню
            MenuScreens.register(ModMenuTypes.CONFIG_CHEST_MENU.get(), ConfigChestScreen::new);
        }
    }
}