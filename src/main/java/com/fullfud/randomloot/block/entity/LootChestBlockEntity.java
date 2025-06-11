package com.fullfud.randomloot.block.entity;

import com.fullfud.randomloot.inventory.LootChestMenu;
import com.fullfud.randomloot.util.ConfigStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.LidBlockEntity;
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

public class LootChestBlockEntity extends BlockEntity implements MenuProvider, LidBlockEntity {

    private final ItemStackHandler itemHandler = new ItemStackHandler(27);
    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();
    private String templateName;

    private int regenerationTimer = -1;
    private static final int REGENERATION_TIME = 6000;
    private static final double PLAYER_CHECK_RADIUS = 50.0;
    
    public float lidAngle;
    public float prevLidAngle;
    private int openers;

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
        be.prevLidAngle = be.lidAngle;
        float f = 0.1F;
        if (be.openers > 0 && be.lidAngle == 0.0F) {
            level.playSound(null, pos, SoundEvents.CHEST_OPEN, SoundSource.BLOCKS, 0.5F, level.random.nextFloat() * 0.1F + 0.9F);
        }
        if (be.openers == 0 && be.lidAngle > 0.0F || be.openers > 0 && be.lidAngle < 1.0F) {
            float f1 = be.lidAngle;
            if (be.openers > 0) {
                be.lidAngle += f;
            } else {
                be.lidAngle -= f;
            }
            if (be.lidAngle > 1.0F) {
                be.lidAngle = 1.0F;
            }
            float f2 = 0.5F;
            if (be.lidAngle < f2 && f1 >= f2) {
                level.playSound(null, pos, SoundEvents.CHEST_CLOSE, SoundSource.BLOCKS, 0.5F, level.random.nextFloat() * 0.1F + 0.9F);
            }
            if (be.lidAngle < 0.0F) {
                be.lidAngle = 0.0F;
            }
        }
    }

    @Override
    public boolean triggerEvent(int id, int type) {
        if (id == 1) {
            this.openers = type;
            return true;
        }
        return super.triggerEvent(id, type);
    }
    
    public void startOpen(Player player) {
        if (!this.remove && !player.isSpectator()) {
            this.openers++;
            this.level.blockEvent(this.getBlockPos(), this.getBlockState().getBlock(), 1, this.openers);
        }
    }

    public void stopOpen(Player player) {
        if (!this.remove && !player.isSpectator()) {
            this.openers--;
            this.level.blockEvent(this.getBlockPos(), this.getBlockState().getBlock(), 1, this.openers);
        }
    }
    
    @Override
    public float getOpenNess(float partialTicks) {
        return Mth.lerp(partialTicks, this.prevLidAngle, this.lidAngle);
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
            for (int i = 0; i < itemHandler.getSlots(); i++) {
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