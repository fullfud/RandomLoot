package com.fullfud.randomloot.block.entity;

import com.fullfud.randomloot.inventory.LootChestMenu;
import com.fullfud.randomloot.util.ConfigStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
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
    
    private final ContainerOpenersCounter openersCounter = new ContainerOpenersCounter() {
        @Override
        protected void onOpen(Level level, BlockPos pos, BlockState state) {
            level.playSound(null, pos, SoundEvents.CHEST_OPEN, SoundSource.BLOCKS, 0.5F, level.random.nextFloat() * 0.1F + 0.9F);
        }

        @Override
        protected void onClose(Level level, BlockPos pos, BlockState state) {
            level.playSound(null, pos, SoundEvents.CHEST_CLOSE, SoundSource.BLOCKS, 0.5F, level.random.nextFloat() * 0.1F + 0.9F);
        }

        @Override
        protected void onOpenCountChanged(Level level, BlockPos pos, BlockState state, int previousCount, int newCount) {
            level.blockEvent(pos, state.getBlock(), 1, newCount);
        }

        @Override
        protected boolean isOwnContainer(Player player) {
            if (player.containerMenu instanceof LootChestMenu menu) {
                // Проверяем, что игрок смотрит именно в этот сундук
                return menu.stillValid(player);
            }
            return false;
        }
    };


    public LootChestBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LOOT_CHEST_BE.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, LootChestBlockEntity be) {
        if (be.templateName == null || be.templateName.isEmpty()) {
            return;
        }

        if (be.isChestEmpty() && be.regenerationTimer == -1) {
            if (!isPlayerNearby(level, pos, PLAYER_CHECK_RADIUS)) {
                be.regenerationTimer = REGENERATION_TIME;
                be.setChanged();
            }
            return;
        }

        if (be.regenerationTimer > 0) {
            if (isPlayerNearby(level, pos, PLAYER_CHECK_RADIUS)) {
                be.regenerationTimer = -1;
                be.setChanged();
                return;
            }

            be.regenerationTimer--;

            if (be.regenerationTimer == 0) {
                be.regenerateLoot();
                be.regenerationTimer = -1;
                be.setChanged();
            }
        }
    }
    
    public static void clientTick(Level level, BlockPos pos, BlockState state, LootChestBlockEntity be) {
        be.openersCounter.recheckOpeners(level, pos, state);
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

        for (int i = 0; i < itemHandler.getSlots(); i++) {
            itemHandler.setStackInSlot(i, ItemStack.EMPTY);
        }

        Random random = new Random();
        List<ItemStack> itemsToSpawn = template.getItemsToSpawn(random);

        for (ItemStack stack : itemsToSpawn) {
            int slot = random.nextInt(itemHandler.getSlots());
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

    @Override
    public boolean triggerEvent(int id, int type) {
        if (id == 1) {
            this.openersCounter.setOpenCount(type);
            return true;
        }
        return super.triggerEvent(id, type);
    }

    public void startOpen(Player pPlayer) {
        if (!this.remove && !pPlayer.isSpectator()) {
            this.openersCounter.incrementOpeners(pPlayer, this.getLevel(), this.getBlockPos(), this.getBlockState());
        }
    }

    public void stopOpen(Player pPlayer) {
        if (!this.remove && !pPlayer.isSpectator()) {
            this.openersCounter.decrementOpeners(pPlayer, this.getLevel(), this.getBlockPos(), this.getBlockState());
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.randomloot.loot_chest");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInv, Player player) {
        return new LootChestMenu(id, playerInv, this);
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