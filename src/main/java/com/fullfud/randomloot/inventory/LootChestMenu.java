package com.fullfud.randomloot.inventory;

import com.fullfud.randomloot.block.ModBlocks;
import com.fullfud.randomloot.block.entity.LootChestBlockEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.SlotItemHandler;

public class LootChestMenu extends AbstractContainerMenu {
    private final LootChestBlockEntity blockEntity;
    private final Level level;

    public LootChestMenu(int id, Inventory playerInv, BlockEntity entity) {
        super(ModMenuTypes.LOOT_CHEST_MENU.get(), id);
        this.blockEntity = (LootChestBlockEntity) entity;
        this.level = playerInv.player.level();

        var blockInventory = blockEntity.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER)
                .orElseThrow(() -> new IllegalStateException("Items capability not found for Loot Chest!"));

        // Слоты сундука (3 ряда по 9 слотов)
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new SlotItemHandler(blockInventory, j + i * 9, 8 + j * 18, 18 + i * 18));
            }
        }

        // Слоты инвентаря игрока (3 ряда по 9)
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(playerInv, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }

        // Слоты хотбара игрока (1 ряд по 9)
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInv, i, 8 + i * 18, 142));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player playerIn, int index) {
        Slot sourceSlot = slots.get(index);
        if (sourceSlot == null || !sourceSlot.hasItem()) return ItemStack.EMPTY;
        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack copyOfSourceStack = sourceStack.copy();

        if (index < 27) { // Из сундука в инвентарь
            if (!moveItemStackTo(sourceStack, 27, 63, true)) {
                return ItemStack.EMPTY;
            }
        } else if (index < 63) { // Из инвентаря в сундук
            if (!moveItemStackTo(sourceStack, 0, 27, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
        }

        if (sourceStack.getCount() == 0) {
            sourceSlot.set(ItemStack.EMPTY);
        } else {
            sourceSlot.setChanged();
        }
        sourceSlot.onTake(playerIn, sourceStack);
        return copyOfSourceStack;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(level, blockEntity.getBlockPos()),
                player, ModBlocks.LOOT_CHEST.get());
    }
}