package com.cosmolego527.create_pp.item.logistics.functions.program;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;

public class TapeProgramScreen extends AbstractContainerScreen<TapeProgramMenu> {
    private final List<String> instructions;
    private static final int VISIBLE_ROWS = 10;
    private static final int HEADER_HEIGHT = 18;
    private static final int LIST_TOP = 26;
    private static final int LIST_BOTTOM = 144;
    private int scrollOffset = 0;

    public TapeProgramScreen(TapeProgramMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.instructions = new ArrayList<>(menu.getInstructions());
        this.imageWidth = 252;
        this.imageHeight = 150;
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelX = 10000;
        this.inventoryLabelY = 10000;
    }

    @Override
    protected void init() {
        super.init();
        int y = topPos + 28;
        int buttonX = leftPos + imageWidth - 82;

        addRenderableWidget(Button.builder(Component.literal("+ Move"), b -> press(TapeProgramMenu.BTN_ADD_MOVE, "move_forward"))
                .pos(buttonX, y)
                .size(76, 20)
                .build());

        addRenderableWidget(Button.builder(Component.literal("+ Wait"), b -> press(TapeProgramMenu.BTN_ADD_WAIT, "wait_20_ticks"))
                .pos(buttonX, y + 24)
                .size(76, 20)
                .build());

        addRenderableWidget(Button.builder(Component.literal("+ Turn Left"), b -> press(TapeProgramMenu.BTN_ADD_TURN_LEFT, "turn_left"))
                .pos(buttonX, y + 48)
                .size(76, 20)
                .build());

        addRenderableWidget(Button.builder(Component.literal("+ Turn Right"), b -> press(TapeProgramMenu.BTN_ADD_TURN_RIGHT, "turn_right"))
                .pos(buttonX, y + 72)
                .size(76, 20)
                .build());


        addRenderableWidget(Button.builder(Component.literal("Clear"), b -> {
                    if (press(TapeProgramMenu.BTN_CLEAR, null)) {
                        instructions.clear();
                        scrollOffset = 0;
                    }
                })
                .pos(buttonX, y + 96)
                .size(76, 20)
                .build());
    }

    private boolean press(int buttonId, String localInstruction) {
        if (minecraft == null || minecraft.gameMode == null) {
            return false;
        }

        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, buttonId);

        if (localInstruction != null && instructions.size() < 64) {
            instructions.add(localInstruction);
            scrollOffset = maxScrollOffset();
        }
        return true;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;

        graphics.fill(x, y, x + imageWidth, y + imageHeight, 0xD41E1E1E);
        graphics.fill(x + 6, y + HEADER_HEIGHT, x + imageWidth - 88, y + LIST_BOTTOM, 0xD40D0D0D);
        drawScrollbar(graphics, x + imageWidth - 96, y + LIST_TOP);
    }

    private void drawScrollbar(GuiGraphics graphics, int x, int y) {
        int trackX0 = x + 0;
        int trackY0 = y;
        int trackX1 = trackX0 + 8;
        int trackY1 = trackY0 + 110;
        graphics.fill(trackX0, trackY0, trackX1, trackY1, 0xFF2E2E2E);

        int maxOffset = maxScrollOffset();
        int thumbHeight = Math.max(12, (int) (110f * ((float) VISIBLE_ROWS / Math.max(VISIBLE_ROWS, instructions.size()))));
        int movable = 110 - thumbHeight;
        int thumbY = trackY0 + (maxOffset == 0 ? 0 : (movable * scrollOffset / maxOffset));
        graphics.fill(trackX0 + 1, thumbY, trackX1 - 1, thumbY + thumbHeight, 0xFF8B8B8B);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        renderBackground(graphics, mouseX, mouseY, partialTicks);
        super.render(graphics, mouseX, mouseY, partialTicks);

        int listX = leftPos + 10;
        int listY = topPos + LIST_TOP;

        int end = Math.min(scrollOffset + VISIBLE_ROWS, instructions.size());
        for (int i = scrollOffset; i < end; i++) {
            int visualRow = i - scrollOffset;
            String line = (i + 1) + ". " + instructions.get(i);
            graphics.drawString(font, line, listX, listY + visualRow * 12, 0xDDDDDD, false);
        }

        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (isMouseOverList(mouseX, mouseY)) {
            if (scrollY < 0) {
                scrollOffset = Math.min(maxScrollOffset(), scrollOffset + 1);
            } else if (scrollY > 0) {
                scrollOffset = Math.max(0, scrollOffset - 1);
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private boolean isMouseOverList(double mouseX, double mouseY) {
        int x0 = leftPos + 6;
        int y0 = topPos + HEADER_HEIGHT;
        int x1 = leftPos + imageWidth - 88;
        int y1 = topPos + LIST_BOTTOM;
        return mouseX >= x0 && mouseX < x1 && mouseY >= y0 && mouseY < y1;
    }

    private int maxScrollOffset() {
        return Math.max(0, instructions.size() - VISIBLE_ROWS);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xFFFFFF, false);
        graphics.drawString(this.font, Component.literal("Programm"), 10, 18, 0xA0FFA0, false);
    }
}