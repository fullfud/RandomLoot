package com.fullfud.randomloot.client.renderer;

import com.fullfud.randomloot.block.entity.LootChestBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.ChestRenderer;

public class LootChestRenderer implements BlockEntityRenderer<LootChestBlockEntity> {

    private final ChestRenderer<LootChestBlockEntity> realChestRenderer;

    public LootChestRenderer(BlockEntityRendererProvider.Context context) {
        this.realChestRenderer = new ChestRenderer<>(context);
    }

    @Override
    public void render(LootChestBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        this.realChestRenderer.render(blockEntity, partialTick, poseStack, buffer, packedLight, packedOverlay);
    }
}