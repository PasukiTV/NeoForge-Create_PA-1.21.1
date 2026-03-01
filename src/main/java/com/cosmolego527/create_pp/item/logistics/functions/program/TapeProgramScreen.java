package com.cosmolego527.create_pp.item.logistics.functions.program;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;

public class TapeProgramScreen extends AbstractContainerScreen<TapeProgramMenu> {
    private static final int VISIBLE_ROWS = 9;

    private static final int LIST_X = 24;
    private static final int LIST_Y = 30;
    private static final int LIST_W = 204;
    private static final int LIST_H = 122;

    private static final String[] ACTION_NAMES = {"Move Forward", "Turn Left", "Turn Right", "Wait"};
    private static final String[] WAIT_UNITS = {"Sekunden", "Minuten", "Stunden"};

    private final List<String> instructions;
    private int scrollOffset = 0;

    private boolean pickerOpen = false;
    private int selectedAction = 0;
    private int waitAmount = 5;
    private int waitUnit = 0;

    private Button addButton;
    private Button clearButton;

    private Button pickerCancelButton;
    private Button pickerConfirmButton;
    private Button waitMinusButton;
    private Button waitPlusButton;
    private Button waitUnitButton;

    public TapeProgramScreen(TapeProgramMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.instructions = new ArrayList<>(menu.getInstructions());
        this.imageWidth = 256;
        this.imageHeight = 175;
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelX = 10000;
        this.inventoryLabelY = 10000;
    }

    @Override
    protected void init() {
        super.init();

        addButton = addRenderableWidget(Button.builder(Component.literal("+"), b -> {
                    pickerOpen = true;
                    updatePickerWidgets();
                })
                .pos(leftPos + 24, topPos + 156)
                .size(20, 16)
                .build());

        clearButton = addRenderableWidget(Button.builder(Component.literal("Clear"), b -> {
                    if (clickServer(TapeProgramMenu.BTN_CLEAR)) {
                        instructions.clear();
                        scrollOffset = 0;
                    }
                })
                .pos(leftPos + 48, topPos + 156)
                .size(56, 16)
                .build());

        pickerCancelButton = addRenderableWidget(Button.builder(Component.literal("Abbrechen"), b -> {
                    pickerOpen = false;
                    updatePickerWidgets();
                })
                .pos(leftPos + 82, topPos + 120)
                .size(92, 20)
                .build());

        pickerConfirmButton = addRenderableWidget(Button.builder(Component.literal("Bestätigen"), b -> confirmSelection())
                .pos(leftPos + 178, topPos + 120)
                .size(92, 20)
                .build());

        waitMinusButton = addRenderableWidget(Button.builder(Component.literal("-"), b -> {
                    waitAmount = Math.max(1, waitAmount - 1);
                    clickServer(TapeProgramMenu.BTN_WAIT_AMOUNT_DEC);
                })
                .pos(leftPos + 110, topPos + 95)
                .size(20, 20)
                .build());

        waitPlusButton = addRenderableWidget(Button.builder(Component.literal("+"), b -> {
                    waitAmount = Math.min(999, waitAmount + 1);
                    clickServer(TapeProgramMenu.BTN_WAIT_AMOUNT_INC);
                })
                .pos(leftPos + 170, topPos + 95)
                .size(20, 20)
                .build());

        waitUnitButton = addRenderableWidget(Button.builder(Component.literal(waitUnitLabel()), b -> {
                    waitUnit = (waitUnit + 1) % WAIT_UNITS.length;
                    clickServer(TapeProgramMenu.BTN_WAIT_UNIT_NEXT);
                    waitUnitButton.setMessage(Component.literal(waitUnitLabel()));
                })
                .pos(leftPos + 196, topPos + 95)
                .size(74, 20)
                .build());

        updatePickerWidgets();
    }

    private void confirmSelection() {
        if (clickServer(TapeProgramMenu.BTN_CONFIRM_SELECTION)) {
            if ("Wait".equals(ACTION_NAMES[selectedAction])) {
                instructions.add("wait_" + waitAmount + waitUnitSuffix());
            } else {
                instructions.add(actionKey());
            }
            scrollOffset = maxScrollOffset();
        }
        pickerOpen = false;
        updatePickerWidgets();
    }

    private void updatePickerWidgets() {
        if (pickerCancelButton == null)
            return;

        pickerCancelButton.visible = pickerOpen;
        pickerConfirmButton.visible = pickerOpen;

        boolean showWait = pickerOpen && "Wait".equals(ACTION_NAMES[selectedAction]);
        waitMinusButton.visible = showWait;
        waitPlusButton.visible = showWait;
        waitUnitButton.visible = showWait;

        addButton.active = !pickerOpen;
        clearButton.active = !pickerOpen;
    }

    private String actionKey() {
        return switch (selectedAction) {
            case 0 -> "move_forward";
            case 1 -> "turn_left";
            case 2 -> "turn_right";
            default -> "move_forward";
        };
    }

    private String waitUnitLabel() {
        return WAIT_UNITS[waitUnit];
    }

    private String waitUnitSuffix() {
        return switch (waitUnit) {
            case 0 -> "s";
            case 1 -> "m";
            default -> "h";
        };
    }

    private boolean clickServer(int buttonId) {
        if (minecraft == null || minecraft.gameMode == null)
            return false;
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, buttonId);
        return true;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;

        graphics.fill(x, y, x + imageWidth, y + imageHeight, 0xD4232323);
        graphics.fill(x + 1, y + 1, x + imageWidth - 1, y + 18, 0xD4D0D0D0);
        graphics.fill(x + LIST_X, y + LIST_Y, x + LIST_X + LIST_W, y + LIST_Y + LIST_H, 0xE00D0D0D);

        drawListScrollbar(graphics, x + LIST_X + LIST_W - 7, y + LIST_Y + 3);

        if (pickerOpen) {
            drawPickerOverlay(graphics, x + 72, y + 44);
        }
    }

    private void drawPickerOverlay(GuiGraphics graphics, int x, int y) {
        graphics.fill(x, y, x + 200, y + 100, 0xF01A1A1A);
        graphics.fill(x + 1, y + 1, x + 199, y + 18, 0xF0D0D0D0);
        graphics.drawString(font, Component.literal("Instruction Editor"), x + 52, y + 6, 0x101010, false);

        graphics.drawString(font, Component.literal("Aktion:"), x + 10, y + 28, 0xDCDCDC, false);
        graphics.fill(x + 62, y + 24, x + 190, y + 42, 0xFF505050);
        graphics.drawString(font, Component.literal(ACTION_NAMES[selectedAction]), x + 68, y + 30, 0xF0F0F0, false);
        graphics.drawString(font, Component.literal("Scrollrad zum Wechseln"), x + 62, y + 45, 0xAAAAAA, false);

        if ("Wait".equals(ACTION_NAMES[selectedAction])) {
            graphics.drawString(font, Component.literal("Warten:"), x + 10, y + 76, 0xDCDCDC, false);
            graphics.fill(x + 134, y + 95, x + 166, y + 115, 0xFF505050);
            graphics.drawString(font, Component.literal(String.valueOf(waitAmount)), x + 144, y + 101, 0xF0F0F0, false);
        }
    }

    private void drawListScrollbar(GuiGraphics graphics, int x, int y) {
        int trackH = LIST_H - 6;
        graphics.fill(x, y, x + 5, y + trackH, 0xFF2C2C2C);

        int maxOffset = maxScrollOffset();
        int thumbH = Math.max(10, (int) (trackH * ((float) VISIBLE_ROWS / Math.max(VISIBLE_ROWS, instructions.size()))));
        int movable = trackH - thumbH;
        int thumbY = y + (maxOffset == 0 ? 0 : (movable * scrollOffset / maxOffset));
        graphics.fill(x + 1, thumbY, x + 4, thumbY + thumbH, 0xFF8E8E8E);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        renderBackground(graphics, mouseX, mouseY, partialTicks);
        super.render(graphics, mouseX, mouseY, partialTicks);

        int listTextX = leftPos + LIST_X + 4;
        int listTextY = topPos + LIST_Y + 4;

        int end = Math.min(scrollOffset + VISIBLE_ROWS, instructions.size());
        for (int i = scrollOffset; i < end; i++) {
            int visualRow = i - scrollOffset;
            String line = (i + 1) + ". " + instructions.get(i);
            graphics.drawString(font, line, listTextX, listTextY + visualRow * 12, 0xE4E4E4, false);
        }

        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (pickerOpen && isMouseOverPickerAction(mouseX, mouseY)) {
            if (scrollY < 0) {
                selectedAction = (selectedAction + 1) % ACTION_NAMES.length;
                clickServer(TapeProgramMenu.BTN_SELECT_NEXT);
            } else if (scrollY > 0) {
                selectedAction = (selectedAction - 1 + ACTION_NAMES.length) % ACTION_NAMES.length;
                clickServer(TapeProgramMenu.BTN_SELECT_PREV);
            }
            updatePickerWidgets();
            return true;
        }

        if (isMouseOverList(mouseX, mouseY)) {
            if (scrollY < 0)
                scrollOffset = Math.min(maxScrollOffset(), scrollOffset + 1);
            else if (scrollY > 0)
                scrollOffset = Math.max(0, scrollOffset - 1);
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private boolean isMouseOverList(double mouseX, double mouseY) {
        int x0 = leftPos + LIST_X;
        int y0 = topPos + LIST_Y;
        int x1 = x0 + LIST_W;
        int y1 = y0 + LIST_H;
        return mouseX >= x0 && mouseX < x1 && mouseY >= y0 && mouseY < y1;
    }

    private boolean isMouseOverPickerAction(double mouseX, double mouseY) {
        int x0 = leftPos + 72 + 62;
        int y0 = topPos + 44 + 24;
        int x1 = x0 + 128;
        int y1 = y0 + 18;
        return mouseX >= x0 && mouseX < x1 && mouseY >= y0 && mouseY < y1;
    }

    private int maxScrollOffset() {
        return Math.max(0, instructions.size() - VISIBLE_ROWS);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x303030, false);
        graphics.drawString(this.font, Component.literal("Programm"), LIST_X + 2, LIST_Y - 11, 0xA0FFA0, false);
    }
}