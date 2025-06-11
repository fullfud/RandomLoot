package com.fullfud.randomloot.block;

import com.fullfud.randomloot.block.entity.LootChestBlockEntity;
import com.fullfud.randomloot.block.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class LootChestBlock extends BaseEntityBlock {
    public LootChestBlock(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState pState) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LootChestBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        // Запускаем логику тика ТОЛЬКО на сервере
        if (level.isClientSide()) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.LOOT_CHEST_BE.get(), LootChestBlockEntity::serverTick);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        // При установке блока, читаем NBT из предмета и записываем в BlockEntity
        if (stack.hasTag()) {
            CompoundTag nbt = stack.getTag();
            if (nbt != null && nbt.contains("templateName")) {
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof LootChestBlockEntity lootBe) {
                    lootBe.setTemplateName(nbt.getString("templateName"));
                }
            }
        }
    }

    // При открытии сундука - просто даем открыть инвентарь
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof LootChestBlockEntity) {
                player.openMenu((LootChestBlockEntity)be);
            }
        }
        return InteractionResult.SUCCESS;
    }
}