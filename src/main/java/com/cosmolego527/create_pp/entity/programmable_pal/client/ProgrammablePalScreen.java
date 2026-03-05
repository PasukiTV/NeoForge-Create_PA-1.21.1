package com.cosmolego527.create_pp.entity.programmable_pal.client;

import com.cosmolego527.create_pp.entity.programmable_pal.menu.ProgrammablePalMenu;
import com.cosmolego527.create_pp.network.ResetPalProgramPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

public class ProgrammablePalScreen extends AbstractContainerScreen<ProgrammablePalMenu> {

    private static final ResourceLocation TEXTURE = ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png");
    private static final int CHEST_ROWS = 3;

    public ProgrammablePalScreen(ProgrammablePalMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        imageWidth = 176;
        imageHeight = 114 + CHEST_ROWS * 18;
        inventoryLabelY = imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        int chestHeight = 17 + CHEST_ROWS * 18;

        guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, chestHeight);
        guiGraphics.blit(TEXTURE, x, y + chestHeight, 0, 126, imageWidth, 96);

        int tapeSlotX = x - 18;
        int tapeSlotY = y + 20;
        guiGraphics.fill(tapeSlotX, tapeSlotY, tapeSlotX + 18, tapeSlotY + 18, 0xFF8B8B8B);
        guiGraphics.fill(tapeSlotX + 1, tapeSlotY + 1, tapeSlotX + 17, tapeSlotY + 17, 0xFF373737);
        guiGraphics.fill(tapeSlotX, tapeSlotY, tapeSlotX + 18, tapeSlotY + 1, 0xFFFFFFFF);
        guiGraphics.fill(tapeSlotX, tapeSlotY, tapeSlotX + 1, tapeSlotY + 18, 0xFFFFFFFF);
        guiGraphics.fill(tapeSlotX + 17, tapeSlotY, tapeSlotX + 18, tapeSlotY + 18, 0xFF5A5A5A);
        guiGraphics.fill(tapeSlotX, tapeSlotY + 17, tapeSlotX + 18, tapeSlotY + 18, 0xFF5A5A5A);
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.literal("Reset"), b ->
                        PacketDistributor.sendToServer(new ResetPalProgramPacket(menu.getPalId())))
                .pos(leftPos + imageWidth - 58, topPos - 20)
                .size(50, 20)
                .build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }
}

