package com.fullfud.randomloot.block.entity;

import com.fullfud.randomloot.ExampleMod;
import com.fullfud.randomloot.block.ModBlocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, ExampleMod.MODID);

    // BE для сундука-конфигуратора
    public static final RegistryObject<BlockEntityType<ConfigChestBlockEntity>> CONFIG_CHEST_BE =
            BLOCK_ENTITIES.register("config_chest_be", () ->
                    BlockEntityType.Builder.of(ConfigChestBlockEntity::new,
                            ModBlocks.CONFIG_CHEST.get()).build(null));

    // BE для финального лут-сундука
    public static final RegistryObject<BlockEntityType<LootChestBlockEntity>> LOOT_CHEST_BE =
            BLOCK_ENTITIES.register("loot_chest_be", () ->
                    BlockEntityType.Builder.of(LootChestBlockEntity::new,
                            ModBlocks.LOOT_CHEST.get()).build(null));


    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}