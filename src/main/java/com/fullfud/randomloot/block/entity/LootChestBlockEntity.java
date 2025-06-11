package com.fullfud.randomloot.block.entity;

import com.fullfud.randomloot.util.ConfigStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
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

import java.util.List;
import java.util.Random;

public class LootChestBlockEntity extends BlockEntity implements MenuProvider {

    private final ItemStackHandler itemHandler = new ItemStackHandler(27);
    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();
    private String templateName;

    private int regenerationTimer = -1;
    private static final int REGENERATION_TIME = 6000; // 5 минут в тиках (5 * 60 * 20)
    private static final double PLAYER_CHECK_RADIUS = 50.0;

    public LootChestBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LOOT_CHEST_BE.get(), pos, state);
    }

    // Логика, которая выполняется каждый тик на сервере
    public static void serverTick(Level level, BlockPos pos, BlockState state, LootChestBlockEntity be) {
        if (be.templateName == null || be.templateName.isEmpty()) {
            return; // Если шаблон не задан, ничего не делаем
        }

        // Если сундук пуст и таймер не запущен, пытаемся его запустить
        if (be.isChestEmpty() && be.regenerationTimer == -1) {
            if (!isPlayerNearby(level, pos, PLAYER_CHECK_RADIUS)) {
                be.regenerationTimer = REGENERATION_TIME;
                be.setChanged();
            }
            return;
        }

        // Если таймер запущен
        if (be.regenerationTimer > 0) {
            // Если игрок подошел, сбрасываем таймер
            if (isPlayerNearby(level, pos, PLAYER_CHECK_RADIUS)) {
                be.regenerationTimer = -1;
                be.setChanged();
                return;
            }

            be.regenerationTimer--;

            // Когда таймер дошел до нуля, регенерируем лут
            if (be.regenerationTimer == 0) {
                be.regenerateLoot();
                be.regenerationTimer = -1; // Сбрасываем таймер
                be.setChanged();
            }
        }
    }

    private static boolean isPlayerNearby(Level level, BlockPos pos, double radius) {
        return level.hasNearbyAlivePlayer(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, radius);
    }

    private boolean isChestEmpty() {
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            if (!itemHandler.getStackInSlot(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private void regenerateLoot() {
        if (level == null || level.isClientSide()) return;

        ConfigStorage.LootTemplate template = ConfigStorage.loadTemplate(this.templateName, level.getServer());
        if (template == null) {
            System.err.println("Could not regenerate loot: template '" + this.templateName + "' not found!");
            return;
        }

        // Очищаем инвентарь перед заполнением
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            itemHandler.setStackInSlot(i, ItemStack.EMPTY);
        }

        Random random = new Random();
        List<ItemStack> itemsToSpawn = template.getItemsToSpawn(random);

        // Распределяем предметы по случайным слотам
        for (ItemStack stack : itemsToSpawn) {
            int slot = random.nextInt(itemHandler.getSlots());
            // Ищем свободный слот, если случайный занят
            for(int i = 0; i < itemHandler.getSlots(); i++) {
                int currentSlot = (slot + i) % itemHandler.getSlots();
                if (itemHandler.getStackInSlot(currentSlot).isEmpty()) {
                    itemHandler.setStackInSlot(currentSlot, stack.copy());
                    break;
                }
            }
        }
    }

    public void setTemplateName(String name) {
        this.templateName = name;
        setChanged();
    }
    
    // --- Стандартные методы BlockEntity ---

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.randomloot.loot_chest");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInv, Player player) {
        // Используем стандартное меню сундука
        return ChestMenu.threeRows(id, playerInv, this.itemHandler);
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
        if (templateName != null) {
            nbt.putString("templateName", templateName);
        }
        nbt.putInt("regenerationTimer", regenerationTimer);
        super.saveAdditional(nbt);
    }

    @Override
    public void load(CompoundTag nbt) {
        super.load(nbt);
        itemHandler.deserializeNBT(nbt.getCompound("inventory"));
        if (nbt.contains("templateName")) {
            this.templateName = nbt.getString("templateName");
        }
        this.regenerationTimer = nbt.getInt("regenerationTimer");
    }
}