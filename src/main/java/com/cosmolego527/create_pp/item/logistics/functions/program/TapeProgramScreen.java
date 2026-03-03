package com.cosmolego527.create_pp.item.logistics.functions.program;

import com.cosmolego527.create_pp.component.ModDataComponentTypes;
import com.cosmolego527.create_pp.network.SaveTapeProgramPacket;
import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.trains.schedule.*;
import com.simibubi.create.content.trains.schedule.condition.ScheduleWaitCondition;
import com.simibubi.create.content.trains.schedule.destination.ScheduleInstruction;
import com.simibubi.create.foundation.gui.*;
import com.simibubi.create.foundation.gui.menu.AbstractSimiContainerScreen;
import com.simibubi.create.foundation.gui.widget.IconButton;
import com.simibubi.create.foundation.gui.widget.Indicator;
import com.simibubi.create.foundation.gui.widget.Indicator.State;
import com.simibubi.create.foundation.gui.widget.Label;
import com.simibubi.create.foundation.gui.widget.SelectionScrollInput;
import com.simibubi.create.foundation.gui.widget.TooltipArea;
import com.simibubi.create.foundation.utility.CreateLang;

import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.animation.LerpedFloat.Chaser;
import net.createmod.catnip.data.Pair;
import net.createmod.catnip.gui.UIRenderHelper;
import net.createmod.catnip.gui.element.GuiGameElement;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class TapeProgramScreen extends AbstractSimiContainerScreen<TapeProgramMenu> implements ScreenWithStencils {

    private static final int CARD_HEADER = 22;
    private static final int CARD_WIDTH = 195;

    private List<Rect2i> extraAreas = Collections.emptyList();

    private List<LerpedFloat> horizontalScrolls = new ArrayList<>();
    private LerpedFloat scroll = LerpedFloat.linear()
            .startWithValue(0);

    private Schedule schedule;

    private IconButton confirmButton;
    private IconButton cyclicButton;
    private Indicator cyclicIndicator;

    private ScheduleInstruction editingDestination;
    private ScheduleWaitCondition editingCondition;
    private SelectionScrollInput scrollInput;
    private SelectionScrollInput secondaryBackgroundInput;
    private Label scrollInputLabel;
    private Label secondaryScrollLabel;
    private IconButton editorConfirm, editorDelete;
    private ModularGuiLine editorSubWidgets;
    private Consumer<Boolean> onEditorClose;
    private final IdentityHashMap<ScheduleInstruction, Integer> actionSelection = new IdentityHashMap<>();
    private final IdentityHashMap<ScheduleInstruction, Integer> checkBlockTargetSelection = new IdentityHashMap<>();
    private final IdentityHashMap<ScheduleInstruction, Integer> moveDirectionSelection = new IdentityHashMap<>();
    private int editingActionIndex = 0;
    private int editingCheckBlockTargetIndex = 0;
    private int editingMoveDirectionIndex = 0;
    private boolean scheduleSavedToServer = false;
    private boolean closeAfterSave = false;
    private int closeAfterSaveTicks = 0;

    private static final List<String> PAL_ACTION_KEYS = List.of(
            "move",
            "jump",
            "wait",
            "check_block",
            "if_has_item",
            "harvest",
            "place",
            "go_to_storage"
    );

    private static final List<Component> PAL_ACTION_OPTIONS = List.of(
            Component.literal("Move"),
            Component.literal("Jump"),
            Component.literal("Wait"),
            Component.literal("Check Block"),
            Component.literal("If Has Item"),
            Component.literal("Harvest"),
            Component.literal("Place"),
            Component.literal("Go to Storage")
    );

    private static final List<Component> CHECK_BLOCK_TARGET_OPTIONS = List.of(
            Component.literal("Below"),
            Component.literal("Above"),
            Component.literal("Front")
    );

    private static final List<Component> MOVE_DIRECTION_OPTIONS = List.of(
            Component.literal("North"),
            Component.literal("East"),
            Component.literal("South"),
            Component.literal("West")
    );


    private static final String ACTION_INDEX_TAG = "PalActionIndex";
    private static final String ACTION_KEY_TAG = "PalActionKey";
    private static final String CHECK_BLOCK_TARGET_INDEX_TAG = "PalCheckBlockTargetIndex";
    private static final String MOVE_DIRECTION_INDEX_TAG = "PalMoveDirectionIndex";

    public TapeProgramScreen(TapeProgramMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        schedule = new Schedule();
        CompoundTag tag = menu.contentHolder.getOrDefault(ModDataComponentTypes.VOID_FUNCTION_DATA, new CompoundTag());
        if (!tag.isEmpty())
            schedule = Schedule.fromTag(menu.player.registryAccess(), tag);
        menu.slotsActive = false;
        editorSubWidgets = new ModularGuiLine();
    }

    @Override
    protected void init() {
        AllGuiTextures bg = AllGuiTextures.SCHEDULE;
        setWindowSize(bg.getWidth(), bg.getHeight());
        super.init();
        clearWidgets();

        confirmButton = new IconButton(leftPos + bg.getWidth() - 42, topPos + bg.getHeight() - 30, AllIcons.I_CONFIRM);
        confirmButton.withCallback(this::saveAndClose);
        addRenderableWidget(confirmButton);

        cyclicIndicator = new Indicator(leftPos + 21, topPos + 196, CommonComponents.EMPTY);
        cyclicIndicator.state = schedule.cyclic ? State.ON : State.OFF;

        List<Component> tip = new ArrayList<>();
        tip.add(CreateLang.translateDirect("schedule.loop"));
        tip.add(CreateLang.translateDirect("gui.schematicannon.optionDisabled")
                .withStyle(ChatFormatting.RED));
        tip.add(CreateLang.translateDirect("schedule.loop1")
                .withStyle(ChatFormatting.GRAY));
        tip.add(CreateLang.translateDirect("schedule.loop2")
                .withStyle(ChatFormatting.GRAY));

        List<Component> tipEnabled = new ArrayList<>(tip);
        tipEnabled.set(1, CreateLang.translateDirect("gui.schematicannon.optionEnabled")
                .withStyle(ChatFormatting.DARK_GREEN));

        cyclicButton = new IconButton(leftPos + 21, topPos + 196, AllIcons.I_REFRESH);
        cyclicButton.withCallback(() -> {
            schedule.cyclic = !schedule.cyclic;
            cyclicButton.green = schedule.cyclic;
            cyclicButton.getToolTip().clear();
            cyclicButton.getToolTip().addAll(schedule.cyclic ? tipEnabled : tip);
        });
        cyclicButton.green = schedule.cyclic;
        cyclicButton.getToolTip().clear();
        cyclicButton.getToolTip().addAll(schedule.cyclic ? tipEnabled : tip);

        addRenderableWidget(cyclicButton);

        stopEditing();
        extraAreas = ImmutableList.of(new Rect2i(leftPos + bg.getWidth(), topPos + bg.getHeight() - 56, 48, 48));
        horizontalScrolls.clear();
        for (int i = 0; i < schedule.entries.size(); i++)
            horizontalScrolls.add(LerpedFloat.linear()
                    .startWithValue(0));
    }

    protected void startEditing(IScheduleInput field, Consumer<Boolean> onClose, boolean allowDeletion) {
        onEditorClose = onClose;
        confirmButton.visible = false;
        cyclicButton.visible = false;
        cyclicIndicator.visible = false;

        scrollInput = new SelectionScrollInput(leftPos + 56, topPos + 65, 143, 16);
        secondaryBackgroundInput = new SelectionScrollInput(leftPos + 77, topPos + 87, 121, 16);
        secondaryScrollLabel = new Label(leftPos + 80, topPos + 91, CommonComponents.EMPTY).withShadow();
        scrollInputLabel = new Label(leftPos + 59, topPos + 69, CommonComponents.EMPTY).withShadow();
        editorConfirm = new IconButton(leftPos + 56 + 168, topPos + 65 + 22, AllIcons.I_CONFIRM);
        if (allowDeletion)
            editorDelete = new IconButton(leftPos + 56 - 45, topPos + 65 + 22, AllIcons.I_TRASH);
        menu.slotsActive = true;
        menu.targetSlotsActive = field.slotsTargeted();

        for (int i = 0; i < field.slotsTargeted(); i++) {
            ItemStack item = field.getItem(i);
            menu.ghostInventory.setStackInSlot(i, item);
        }

        if (field instanceof ScheduleInstruction instruction) {
            editingDestination = instruction;
            editingActionIndex = getActionIndex(instruction);
            editingCheckBlockTargetIndex = getCheckBlockTargetIndex(instruction);
            updateEditorSubwidgets(editingDestination);
            scrollInput.forOptions(PAL_ACTION_OPTIONS)
                    .titled(Component.literal("Action"))
                    .writingTo(scrollInputLabel)
                    .calling(index -> {
                        editingActionIndex = index;
                        updateSecondarySelector();
                    })
                    .setState(editingActionIndex);
            updateSecondarySelector();
        }



        addRenderableWidget(scrollInput);
        addRenderableWidget(secondaryBackgroundInput);
        addRenderableWidget(scrollInputLabel);
        addRenderableWidget(secondaryScrollLabel);
        addRenderableWidget(editorConfirm);
        if (allowDeletion)
            addRenderableWidget(editorDelete);
    }

    private List<Pair<ResourceLocation, Supplier<? extends ScheduleInstruction>>> getPalInstructionTypes() {
        List<Pair<ResourceLocation, Supplier<? extends ScheduleInstruction>>> filteredTypes = new ArrayList<>();

        for (Pair<ResourceLocation, Supplier<? extends ScheduleInstruction>> instructionType : Schedule.INSTRUCTION_TYPES) {
            String path = instructionType.getFirst().getPath();
            if (path.contains("rename") || path.contains("delivery") || path.contains("retrieval"))
                filteredTypes.add(instructionType);
        }

        return filteredTypes;
    }


    private int getCheckBlockTargetIndex(ScheduleInstruction instruction) {
        CompoundTag data = instruction.getData();
        int stored = data.contains(CHECK_BLOCK_TARGET_INDEX_TAG) ? data.getInt(CHECK_BLOCK_TARGET_INDEX_TAG)
                : checkBlockTargetSelection.getOrDefault(instruction, 0);
        return Mth.clamp(stored, 0, CHECK_BLOCK_TARGET_OPTIONS.size() - 1);
    }


    private int getMoveDirectionIndex(ScheduleInstruction instruction) {
        CompoundTag data = instruction.getData();
        int stored = data.contains(MOVE_DIRECTION_INDEX_TAG) ? data.getInt(MOVE_DIRECTION_INDEX_TAG)
                : moveDirectionSelection.getOrDefault(instruction, 0);
        return Mth.clamp(stored, 0, MOVE_DIRECTION_OPTIONS.size() - 1);
    }

    private void updateSecondarySelector() {
        if (secondaryBackgroundInput == null || secondaryScrollLabel == null)
            return;

        if (isCheckBlockAction(editingActionIndex)) {
            secondaryBackgroundInput.forOptions(CHECK_BLOCK_TARGET_OPTIONS)
                    .titled(Component.literal("Check Block"))
                    .writingTo(secondaryScrollLabel)
                    .calling(index -> editingCheckBlockTargetIndex = index)
                    .setState(editingCheckBlockTargetIndex);
            return;
        }

        if (isMoveAction(editingActionIndex)) {
            secondaryBackgroundInput.forOptions(MOVE_DIRECTION_OPTIONS)
                    .titled(Component.literal("Move"))
                    .writingTo(secondaryScrollLabel)
                    .calling(index -> editingMoveDirectionIndex = index)
                    .setState(editingMoveDirectionIndex);
            return;
        }

        secondaryBackgroundInput.forOptions(List.of(Component.empty()))
                .titled(Component.empty())
                .writingTo(secondaryScrollLabel)
                .calling(index -> {
                })
                .setState(0);
    }

    private int getActionIndex(ScheduleInstruction instruction) {
        CompoundTag data = instruction.getData();
        if (data.contains(ACTION_KEY_TAG))
            return getActionIndexByKey(data.getString(ACTION_KEY_TAG));

        int stored = data.contains(ACTION_INDEX_TAG) ? data.getInt(ACTION_INDEX_TAG)
                : actionSelection.getOrDefault(instruction, 0);
        return Mth.clamp(stored, 0, PAL_ACTION_OPTIONS.size() - 1);
    }

    private int getActionIndexByKey(String actionKey) {
        int index = PAL_ACTION_KEYS.indexOf(actionKey);
        if (index == -1)
            return 0;
        return Mth.clamp(index, 0, PAL_ACTION_OPTIONS.size() - 1);
    }

    private String getActionKeyForIndex(int index) {
        int clamped = Mth.clamp(index, 0, PAL_ACTION_KEYS.size() - 1);
        return PAL_ACTION_KEYS.get(clamped);
    }

    private boolean isCheckBlockAction(int index) {
        return "check_block".equals(getActionKeyForIndex(index));
    }

    private boolean isMoveAction(int index) {
        return "move".equals(getActionKeyForIndex(index));
    }

    private Component getActionLabel(ScheduleInstruction instruction) {
        int actionIndex = getActionIndex(instruction);
        Component action = PAL_ACTION_OPTIONS.get(actionIndex);

        if (isCheckBlockAction(actionIndex)) {
            Component target = CHECK_BLOCK_TARGET_OPTIONS.get(getCheckBlockTargetIndex(instruction));
            return Component.literal(action.getString() + " " + target.getString());
        }

        if (isMoveAction(actionIndex)) {
            Component direction = MOVE_DIRECTION_OPTIONS.get(getMoveDirectionIndex(instruction));
            return Component.literal(action.getString() + " " + direction.getString());
        }

        return action;
    }

    private String getActionName(ScheduleInstruction instruction) {
        return getActionLabel(instruction).getString();
    }

    private Pair<ItemStack, Component> getDisplaySummary(ScheduleInstruction instruction) {
        return Pair.of(ItemStack.EMPTY, getActionLabel(instruction));
    }


    protected void stopEditing() {
        confirmButton.visible = true;
        cyclicButton.visible = true;
        cyclicIndicator.visible = true;

        if (editingCondition == null && editingDestination == null)
            return;

        removeWidget(scrollInput);
        removeWidget(secondaryBackgroundInput);
        removeWidget(scrollInputLabel);
        removeWidget(secondaryScrollLabel);
        removeWidget(editorConfirm);
        removeWidget(editorDelete);

        IScheduleInput editing = editingCondition == null ? editingDestination : editingCondition;
        for (int i = 0; i < editing.slotsTargeted(); i++)
            editing.setItem(i, menu.ghostInventory.getStackInSlot(i));

        if (editingDestination != null) {
            CompoundTag destinationData = editingDestination.getData();
            destinationData.putInt(ACTION_INDEX_TAG, editingActionIndex);
            destinationData.putString(ACTION_KEY_TAG, getActionKeyForIndex(editingActionIndex));
            actionSelection.put(editingDestination, editingActionIndex);
            if (isCheckBlockAction(editingActionIndex)) {
                destinationData.putInt(CHECK_BLOCK_TARGET_INDEX_TAG, editingCheckBlockTargetIndex);
                checkBlockTargetSelection.put(editingDestination, editingCheckBlockTargetIndex);
            } else {
                destinationData.remove(CHECK_BLOCK_TARGET_INDEX_TAG);
                checkBlockTargetSelection.remove(editingDestination);
            }

            if (isMoveAction(editingActionIndex)) {
                destinationData.putInt(MOVE_DIRECTION_INDEX_TAG, editingMoveDirectionIndex);
                moveDirectionSelection.put(editingDestination, editingMoveDirectionIndex);
            } else {
                destinationData.remove(MOVE_DIRECTION_INDEX_TAG);
                moveDirectionSelection.remove(editingDestination);
            }
        }

        editorSubWidgets.saveValues(editing.getData());
        editorSubWidgets.forEach(this::removeWidget);
        editorSubWidgets.clear();

        editingCondition = null;
        editingDestination = null;
        editorConfirm = null;
        secondaryBackgroundInput = null;
        secondaryScrollLabel = null;
        editorDelete = null;
        menu.slotsActive = false;
        init();
    }

    protected void updateEditorSubwidgets(IScheduleInput field) {
        menu.targetSlotsActive = field.slotsTargeted();

        editorSubWidgets.forEach(this::removeWidget);
        editorSubWidgets.clear();
        editorSubWidgets.add(Pair.of(new TooltipArea(leftPos + 77, topPos + 87, 121, 18), "secondary_selector_background"));
    }


    @Override
    protected void containerTick() {
        super.containerTick();

        if (closeAfterSave && --closeAfterSaveTicks <= 0) {
            closeAfterSave = false;
            minecraft.player.closeContainer();
            return;
        }

        scroll.tickChaser();
        for (LerpedFloat lerpedFloat : horizontalScrolls)
            lerpedFloat.tickChaser();


        schedule.savedProgress =
                schedule.entries.isEmpty() ? 0 : Mth.clamp(schedule.savedProgress, 0, schedule.entries.size() - 1);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        partialTicks = minecraft.getTimer().getGameTimeDeltaPartialTick(false);

        if (menu.slotsActive)
            super.render(graphics, mouseX, mouseY, partialTicks);
        else {
            renderBackground(graphics, mouseX, mouseY, partialTicks);
            renderBg(graphics, partialTicks, mouseX, mouseY);
            for (Renderable widget : this.renderables)
                widget.render(graphics, mouseX, mouseY, partialTicks);
            renderForeground(graphics, mouseX, mouseY, partialTicks);
        }
    }

    protected void renderSchedule(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        PoseStack matrixStack = graphics.pose();
        UIRenderHelper.swapAndBlitColor(minecraft.getMainRenderTarget(), UIRenderHelper.framebuffer);

        UIRenderHelper.drawStretched(graphics, leftPos + 33, topPos + 16, 3, 173, 200,
                AllGuiTextures.SCHEDULE_STRIP_DARK);

        int yOffset = 25;
        List<ScheduleEntry> entries = schedule.entries;
        float scrollOffset = -scroll.getValue(partialTicks);

        for (int i = 0; i <= entries.size(); i++) {

            if (schedule.savedProgress == i && !schedule.entries.isEmpty()) {
                matrixStack.pushPose();
                float expectedY = scrollOffset + topPos + yOffset + 4;
                float actualY = Mth.clamp(expectedY, topPos + 18, topPos + 170);
                matrixStack.translate(0, actualY, 0);
                (expectedY == actualY ? AllGuiTextures.SCHEDULE_POINTER : AllGuiTextures.SCHEDULE_POINTER_OFFSCREEN)
                        .render(graphics, leftPos, 0);
                matrixStack.popPose();
            }

            startStencil(graphics, leftPos + 16, topPos + 16, 220, 173);
            matrixStack.pushPose();
            matrixStack.translate(0, scrollOffset, 0);
            if (i == 0 || entries.size() == 0)
                UIRenderHelper.drawStretched(graphics, leftPos + 33, topPos + 16, 3, 10, -100,
                        AllGuiTextures.SCHEDULE_STRIP_LIGHT);

            if (i == entries.size()) {
                if (i > 0)
                    yOffset += 9;
                AllGuiTextures.SCHEDULE_STRIP_END.render(graphics, leftPos + 29, topPos + yOffset);
                AllGuiTextures.SCHEDULE_CARD_NEW.render(graphics, leftPos + 43, topPos + yOffset);
                matrixStack.popPose();
                endStencil();
                break;
            }

            ScheduleEntry scheduleEntry = entries.get(i);
            int cardY = yOffset;
            int cardHeight = renderScheduleEntry(graphics, scheduleEntry, cardY, mouseX, mouseY, partialTicks);
            yOffset += cardHeight;

            if (i + 1 < entries.size()) {
                AllGuiTextures.SCHEDULE_STRIP_DOTTED.render(graphics, leftPos + 29, topPos + yOffset - 3);
                yOffset += 10;
            }

            matrixStack.popPose();
            endStencil();

            continue;
        }

        int zLevel = 200;
        graphics.fillGradient(leftPos + 16, topPos + 16, leftPos + 16 + 220, topPos + 16 + 10, zLevel, 0x77000000,
                0x00000000);
        graphics.fillGradient(leftPos + 16, topPos + 179, leftPos + 16 + 220, topPos + 179 + 10, zLevel, 0x00000000,
                0x77000000);
        UIRenderHelper.swapAndBlitColor(UIRenderHelper.framebuffer, minecraft.getMainRenderTarget());
    }

    public int renderScheduleEntry(GuiGraphics graphics, ScheduleEntry entry, int yOffset, int mouseX, int mouseY,
                                   float partialTicks) {
        int zLevel = -100;

        AllGuiTextures light = AllGuiTextures.SCHEDULE_CARD_LIGHT;
        AllGuiTextures medium = AllGuiTextures.SCHEDULE_CARD_MEDIUM;
        AllGuiTextures dark = AllGuiTextures.SCHEDULE_CARD_DARK;

        int cardWidth = CARD_WIDTH;
        int cardHeader = CARD_HEADER;
        boolean supportsConditions = false;
        int cardHeight = cardHeader + 4;

        PoseStack matrixStack = graphics.pose();
        matrixStack.pushPose();
        matrixStack.translate(leftPos + 25, topPos + yOffset, 0);

        UIRenderHelper.drawStretched(graphics, 0, 1, cardWidth, cardHeight - 2, zLevel, light);
        UIRenderHelper.drawStretched(graphics, 1, 0, cardWidth - 2, cardHeight, zLevel, light);
        UIRenderHelper.drawStretched(graphics, 1, 1, cardWidth - 2, cardHeight - 2, zLevel, dark);
        UIRenderHelper.drawStretched(graphics, 2, 2, cardWidth - 4, cardHeight - 4, zLevel, medium);
        UIRenderHelper.drawStretched(graphics, 2, 2, cardWidth - 4, cardHeader, zLevel,
                supportsConditions ? light : medium);

        AllGuiTextures.SCHEDULE_CARD_REMOVE.render(graphics, cardWidth - 14, 2);
        AllGuiTextures.SCHEDULE_CARD_DUPLICATE.render(graphics, cardWidth - 14, cardHeight - 14);

        int i = schedule.entries.indexOf(entry);
        if (i > 0)
            AllGuiTextures.SCHEDULE_CARD_MOVE_UP.render(graphics, cardWidth, cardHeader - 14);
        if (i < schedule.entries.size() - 1)
            AllGuiTextures.SCHEDULE_CARD_MOVE_DOWN.render(graphics, cardWidth, cardHeader);

        UIRenderHelper.drawStretched(graphics, 8, 0, 3, cardHeight + 10, zLevel, AllGuiTextures.SCHEDULE_STRIP_LIGHT);
        (supportsConditions ? AllGuiTextures.SCHEDULE_STRIP_TRAVEL : AllGuiTextures.SCHEDULE_STRIP_ACTION)
                .render(graphics, 4, 6);

        if (supportsConditions)
            AllGuiTextures.SCHEDULE_STRIP_WAIT.render(graphics, 4, 28);

        Pair<ItemStack, Component> displaySummary = getDisplaySummary(entry.instruction);
        renderInput(graphics, displaySummary, 26, 5, false, 100);

        matrixStack.popPose();

        return cardHeight;
    }





    protected int renderInput(GuiGraphics graphics, Pair<ItemStack, Component> pair, int x, int y, boolean clean,
                              int minSize) {
        ItemStack stack = pair.getFirst();
        Component text = pair.getSecond();
        boolean hasItem = !stack.isEmpty();
        int fieldSize = Math.min(getFieldSize(minSize, pair), 150);
        PoseStack matrixStack = graphics.pose();
        matrixStack.pushPose();

        AllGuiTextures left =
                clean ? AllGuiTextures.SCHEDULE_CONDITION_LEFT_CLEAN : AllGuiTextures.SCHEDULE_CONDITION_LEFT;
        AllGuiTextures middle = AllGuiTextures.SCHEDULE_CONDITION_MIDDLE;
        AllGuiTextures item = AllGuiTextures.SCHEDULE_CONDITION_ITEM;
        AllGuiTextures right = AllGuiTextures.SCHEDULE_CONDITION_RIGHT;

        matrixStack.translate(x, y, 0);
        UIRenderHelper.drawStretched(graphics, 0, 0, fieldSize, 16, -100, middle);
        left.render(graphics, clean ? 0 : -3, 0);
        right.render(graphics, fieldSize - 2, 0);
        if (hasItem)
            item.render(graphics, 3, 0);
        if (hasItem) {
            item.render(graphics, 3, 0);
            if (stack.getItem() != Items.STRUCTURE_VOID)
                GuiGameElement.of(stack)
                        .at(4, 0)
                        .render(graphics);
        }

        if (text != null)
            graphics.drawString(font, font.substrByWidth(text, 120)
                    .getString(), hasItem ? 28 : 8, 4, 0xff_f2f2ee);

        matrixStack.popPose();
        return fieldSize;
    }

    private Component clickToEdit = CreateLang.translateDirect("gui.schedule.lmb_edit")
            .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC);
    private Component rClickToDelete = CreateLang.translateDirect("gui.schedule.rmb_remove")
            .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC);

    public boolean action(@Nullable GuiGraphics graphics, double mouseX, double mouseY, int click) {
        if (editingCondition != null || editingDestination != null)
            return false;

        Component empty = CommonComponents.EMPTY;

        int mx = (int) mouseX;
        int my = (int) mouseY;
        int x = mx - leftPos - 25;
        int y = my - topPos - 25;
        if (x < 0 || x >= 205)
            return false;
        if (y < 0 || y >= 173)
            return false;
        y += scroll.getValue(0);

        List<ScheduleEntry> entries = schedule.entries;
        for (int i = 0; i < entries.size(); i++) {
            ScheduleEntry entry = entries.get(i);
            int cardHeight = CARD_HEADER + 4;

            if (y >= cardHeight + 5) {
                y -= cardHeight + 10;
                if (y < 0)
                    return false;
                continue;
            }

            int fieldSize = getFieldSize(100, getDisplaySummary(entry.instruction));
            if (x > 25 && x <= 25 + fieldSize && y > 4 && y <= 20) {
                List<Component> components = new ArrayList<>();
                components.add(Component.literal(getActionName(entry.instruction)).withStyle(ChatFormatting.GRAY));
                components.add(empty);
                components.add(clickToEdit);
                renderActionTooltip(graphics, components, mx, my);
                if (click == 0)
                    startEditing(entry.instruction, confirmed -> {
                        if (confirmed)
                            entry.instruction = editingDestination;
                    }, false);
                return true;
            }

            if (x > 180 && x <= 192) {
                if (y > 0 && y <= 14) {
                    renderActionTooltip(graphics,
                            ImmutableList.of(CreateLang.translateDirect("gui.schedule.remove_entry")), mx, my);
                    if (click == 0) {
                        entries.remove(entry);
                        init();
                    }
                    return true;
                }
                if (y > cardHeight - 14) {
                    renderActionTooltip(graphics,
                            ImmutableList.of(CreateLang.translateDirect("gui.schedule.duplicate")), mx, my);
                    if (click == 0) {
                        entries.add(entries.indexOf(entry), entry.clone(minecraft.level.registryAccess()));
                        init();
                    }
                    return true;
                }
            }

            if (x > 194) {
                if (y > 7 && y <= 20 && i > 0) {
                    renderActionTooltip(graphics, ImmutableList.of(CreateLang.translateDirect("gui.schedule.move_up")),
                            mx, my);
                    if (click == 0) {
                        entries.remove(entry);
                        entries.add(i - 1, entry);
                        init();
                    }
                    return true;
                }
                if (y > 20 && y <= 33 && i < entries.size() - 1) {
                    renderActionTooltip(graphics,
                            ImmutableList.of(CreateLang.translateDirect("gui.schedule.move_down")), mx, my);
                    if (click == 0) {
                        entries.remove(entry);
                        entries.add(i + 1, entry);
                        init();
                    }
                    return true;
                }
            }

            return false;
        }

        if (x < 18 || x > 33 || y > 14)
            return false;

        renderActionTooltip(graphics, ImmutableList.of(CreateLang.translateDirect("gui.schedule.add_entry")), mx, my);
        if (click == 0) {
            List<Pair<ResourceLocation, Supplier<? extends ScheduleInstruction>>> instructionTypes = getPalInstructionTypes();
            if (instructionTypes.isEmpty())
                return true;
            startEditing(instructionTypes.get(0).getSecond().get(), confirmed -> {
                if (!confirmed)
                    return;

                ScheduleEntry entry = new ScheduleEntry();
                entry.instruction = editingDestination;
                entry.conditions.clear();
                entry.instruction.getData().putInt(ACTION_INDEX_TAG, editingActionIndex);
                entry.instruction.getData().putString(ACTION_KEY_TAG, getActionKeyForIndex(editingActionIndex));
                if (isCheckBlockAction(editingActionIndex))
                    entry.instruction.getData().putInt(CHECK_BLOCK_TARGET_INDEX_TAG, editingCheckBlockTargetIndex);
                if (isMoveAction(editingActionIndex))
                    entry.instruction.getData().putInt(MOVE_DIRECTION_INDEX_TAG, editingMoveDirectionIndex);
                actionSelection.put(entry.instruction, editingActionIndex);
                if (isCheckBlockAction(editingActionIndex))
                    checkBlockTargetSelection.put(entry.instruction, editingCheckBlockTargetIndex);
                if (isMoveAction(editingActionIndex))
                    moveDirectionSelection.put(entry.instruction, editingMoveDirectionIndex);
                schedule.entries.add(entry);
            }, true);
        }
        return true;
    }


    private void renderActionTooltip(@Nullable GuiGraphics graphics, List<Component> tooltip, int mx, int my) {
        if (graphics != null)
            graphics.renderTooltip(font, tooltip, Optional.empty(), mx, my);
    }

    private int getFieldSize(int minSize, Pair<ItemStack, Component> pair) {
        ItemStack stack = pair.getFirst();
        Component text = pair.getSecond();
        boolean hasItem = !stack.isEmpty();
        return Math.max((text == null ? 0 : font.width(text)) + (hasItem ? 20 : 0) + 16, minSize);
    }

    @Override
    public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
        if (editorConfirm != null && editorConfirm.isMouseOver(pMouseX, pMouseY) && onEditorClose != null) {
            onEditorClose.accept(true);
            stopEditing();
            return true;
        }
        if (editorDelete != null && editorDelete.isMouseOver(pMouseX, pMouseY) && onEditorClose != null) {
            onEditorClose.accept(false);
            stopEditing();
            return true;
        }
        if (action(null, pMouseX, pMouseY, pButton))
            return true;

        return super.mouseClicked(pMouseX, pMouseY, pButton);
    }

    @Override
    public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
        if (editingCondition == null && editingDestination == null)
            return super.keyPressed(pKeyCode, pScanCode, pModifiers);
        InputConstants.Key mouseKey = InputConstants.getKey(pKeyCode, pScanCode);
        boolean hitEnter = (pKeyCode == 257 || pKeyCode == 335);
        boolean hitE = getFocused() == null && minecraft.options.keyInventory.isActiveAndMatches(mouseKey);
        if (hitE || hitEnter) {
            onEditorClose.accept(true);
            stopEditing();
            return true;
        }
        return super.keyPressed(pKeyCode, pScanCode, pModifiers);
    }

    @Override
    public boolean mouseScrolled(double pMouseX, double pMouseY, double pScrollX, double pScrollY) {
        if (editingCondition != null || editingDestination != null)
            return super.mouseScrolled(pMouseX, pMouseY, pScrollX, pScrollY);

        float chaseTarget = scroll.getChaseTarget();
        float max = 40 - 173;
        for (ScheduleEntry scheduleEntry : schedule.entries)
            max += CARD_HEADER + 4 + 10;
        if (max > 0) {
            chaseTarget -= pScrollY * 12;
            chaseTarget = Mth.clamp(chaseTarget, 0, max);
            scroll.chase((int) chaseTarget, 0.7f, Chaser.EXP);
        } else
            scroll.chase(0, 0.7f, Chaser.EXP);

        return super.mouseScrolled(pMouseX, pMouseY, pScrollX, pScrollY);
    }

    @Override
    protected void renderForeground(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        PoseStack matrixStack = graphics.pose();
        super.renderForeground(graphics, mouseX, mouseY, partialTicks);
        action(graphics, mouseX, mouseY, -1);

        if (editingCondition == null && editingDestination == null)
            return;

        int x = leftPos + 53;
        int y = topPos + 87;
        if (mouseX < x || mouseY < y || mouseX >= x + 120 || mouseY >= y + 18)
            return;

        IScheduleInput rendered = editingCondition == null ? editingDestination : editingCondition;

        for (int i = 0; i < Math.max(1, rendered.slotsTargeted()); i++) {
            List<Component> secondLineTooltip = rendered.getSecondLineTooltip(i);
            if (secondLineTooltip == null)
                continue;
            Slot slot = menu.getSlot(36 + i);
            if (slot == null || !slot.getItem()
                    .isEmpty())
                continue;
            if (mouseX < leftPos + slot.x || mouseX > leftPos + slot.x + 18)
                continue;
            if (mouseY < topPos + slot.y || mouseY > topPos + slot.y + 18)
                continue;
            renderActionTooltip(graphics, secondLineTooltip, mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float pPartialTick, int pMouseX, int pMouseY) {
        AllGuiTextures.SCHEDULE.render(graphics, leftPos, topPos);
        FormattedCharSequence formattedcharsequence = title.getVisualOrderText();
        int center = leftPos + (AllGuiTextures.SCHEDULE.getWidth() - 8) / 2;
        graphics.drawString(font, formattedcharsequence, (float) (center - font.width(formattedcharsequence) / 2),
                (float) topPos + 4, 0x505050, false);
        renderSchedule(graphics, pMouseX, pMouseY, pPartialTick);

        if (editingCondition == null && editingDestination == null)
            return;

        graphics.fillGradient(0, 0, this.width, this.height, -1072689136, -804253680);
        AllGuiTextures.SCHEDULE_EDITOR.render(graphics, leftPos - 2, topPos + 40);
        AllGuiTextures.PLAYER_INVENTORY.render(graphics, leftPos + 38, topPos + 122);
        graphics.drawString(font, playerInventoryTitle, leftPos + 46, topPos + 128, 0x505050, false);

        formattedcharsequence = editingCondition == null ? CreateLang.translateDirect("schedule.instruction.editor")
                .getVisualOrderText()
                : CreateLang.translateDirect("schedule.condition.editor")
                .getVisualOrderText();
        graphics.drawString(font, formattedcharsequence, (float) (center - font.width(formattedcharsequence) / 2),
                (float) topPos + 44, 0x505050, false);

        IScheduleInput rendered = editingCondition == null ? editingDestination : editingCondition;

        for (int i = 0; i < rendered.slotsTargeted(); i++)
            AllGuiTextures.SCHEDULE_EDITOR_ADDITIONAL_SLOT.render(graphics, leftPos + 53 + 20 * i, topPos + 87);

        if (rendered.slotsTargeted() == 0) {
            AllGuiTextures.SCHEDULE_EDITOR_INACTIVE_SLOT.render(graphics, leftPos + 53, topPos + 87);
        }

        PoseStack pPoseStack = graphics.pose();
        pPoseStack.pushPose();
        pPoseStack.translate(0, getGuiTop() + 87, 0);
        editorSubWidgets.renderWidgetBG(getGuiLeft() + 77, graphics);
        pPoseStack.popPose();
    }
    private void saveProgram() {
        if (scheduleSavedToServer)
            return;
        scheduleSavedToServer = true;

        CompoundTag scheduleTag = schedule.entries.isEmpty() ? new CompoundTag() : schedule.write(menu.player.registryAccess());
        PacketDistributor.sendToServer(new SaveTapeProgramPacket(scheduleTag));
    }

    private void saveAndClose() {
        saveProgram();
        closeAfterSave = true;
        closeAfterSaveTicks = 2;
    }

    @Override
    public void removed() {
        saveProgram();
        super.removed();
    }

    @Override
    public List<Rect2i> getExtraAreas() {
        return extraAreas;
    }

    public Font getFont() {
        return font;
    }

}
