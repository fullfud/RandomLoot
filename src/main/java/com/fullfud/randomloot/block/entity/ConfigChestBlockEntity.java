package com.fullfud.randomloot.block.entity;

import com.fullfud.randomloot.inventory.ConfigChestMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ConfigChestBlockEntity extends BlockEntity implements MenuProvider {

    private final ItemStackHandler itemHandler = new ItemStackHandler(27); // Стандартный размер сундука
    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();

    private UUID owner;
    private String templateName;

    public ConfigChestBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CONFIG_CHEST_BE.get(), pos, state);
    }

    @Override
    public Component getDisplayName() {
        return Component.literal(this.templateName != null ? "Настройка шаблона: " + this.templateName : "Сундук Конфигурации");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new ConfigChestMenu(id, playerInventory, this);
    }

    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return lazyItemHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        lazyItemHandler = LazyOptional.of(() -> itemHandler);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyItemHandler.invalidate();
    }

    @Override
    protected void saveAdditional(CompoundTag nbt) {
        nbt.put("inventory", itemHandler.serializeNBT());
        if (owner != null) {
            nbt.putUUID("owner", owner);
        }
        if (templateName != null) {
            nbt.putString("templateName", templateName);
        }
        super.saveAdditional(nbt);
    }

    @Override
    public void load(CompoundTag nbt) {
        super.load(nbt);
        itemHandler.deserializeNBT(nbt.getCompound("inventory"));
        if (nbt.hasUUID("owner")) {
            this.owner = nbt.getUUID("owner");
        }
        if (nbt.contains("templateName")) {
            this.templateName = nbt.getString("templateName");
        }
    }

    // Наши кастомные методы
    public void setOwner(UUID owner) { this.owner = owner; setChanged(); }
    public UUID getOwner() { return owner; }
    public void setTemplateName(String name) { this.templateName = name; setChanged(); }

    public List<ItemStack> getItems() {
        List<ItemStack> items = new ArrayList<>();
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            ItemStack stack = itemHandler.getStackInSlot(i);
            if (!stack.isEmpty()) {
                items.add(stack.copy());
            }
        }
        return items;
    }

    public static void tick(Level level, BlockPos blockPos, BlockState blockState, ConfigChestBlockEntity configChestBlockEntity) {
        // Логика тика, если потребуется
    }
}