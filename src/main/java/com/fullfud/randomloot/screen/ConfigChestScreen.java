package com.fullfud.randomloot.screen;

import com.fullfud.randomloot.inventory.ConfigChestMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class ConfigChestScreen extends AbstractContainerScreen<ConfigChestMenu> {
    private static final ResourceLocation CHEST_GUI_TEXTURE = new ResourceLocation("minecraft:textures/gui/container/generic_54.png");

    public ConfigChestScreen(ConfigChestMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);
        this.imageHeight = 168;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int x, int y) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int relX = (this.width - this.imageWidth) / 2;
        int relY = (this.height - this.imageHeight) / 2;
        guiGraphics.blit(CHEST_GUI_TEXTURE, relX, relY, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void init() {
        super.init();
        // Убрал центрирование заголовка, так как он берется из BlockEntity и может быть длинным
    }
}