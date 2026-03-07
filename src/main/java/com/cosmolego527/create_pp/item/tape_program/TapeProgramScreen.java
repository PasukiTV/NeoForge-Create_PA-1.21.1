package com.cosmolego527.create_pp.item.tape_program;

import com.cosmolego527.create_pp.component.ModDataComponentTypes;
import com.cosmolego527.create_pp.item.ModItems;
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
import com.simibubi.create.foundation.gui.widget.ScrollInput;
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
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
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
import java.util.function.IntConsumer;
import java.util.function.Supplier;

public class TapeProgramScreen extends AbstractSimiContainerScreen<TapeProgramMenu> implements ScreenWithStencils {

    private static final int CARD_HEADER = 22;
    private static final int CARD_WIDTH = 195;
    private static final ResourceLocation TAPE_PROGRAM_EDITOR_TEXTURE = ResourceLocation.fromNamespaceAndPath("create_programmablepals", "textures/gui/tape_program_2.png");

    private List<Rect2i> extraAreas = Collections.emptyList();

    private List<LerpedFloat> horizontalScrolls = new ArrayList<>();
    private LerpedFloat scroll = LerpedFloat.linear()
            .startWithValue(0);

    private Schedule schedule;

    private IconButton confirmButton;
    private Button renameButton;
    private EditBox tapeNameBox;
    private boolean renamingTapeName = false;
    private String tapeCustomName = "";
    private IconButton cyclicButton;
    private Indicator cyclicIndicator;

    private ScheduleInstruction editingDestination;
    private ScheduleWaitCondition editingCondition;
    private SelectionScrollInput scrollInput;
    private SelectionScrollInput secondaryBackgroundInput;
    private SelectionScrollInput tertiaryBackgroundInput;
    private ScrollInput moveDistanceInput;
    private SelectionScrollInput wideOptionInput;
    private IconButton moveLinkToggleButton;
    private Label scrollInputLabel;
    private Label secondaryScrollLabel;
    private Label tertiaryScrollLabel;
    private Label wideOptionLabel;
    private IconButton editorConfirm, editorDelete;
    private ModularGuiLine editorSubWidgets;
    private Consumer<Boolean> onEditorClose;
    private final TapeActionInputStrategyRegistry actionInputStrategyRegistry = TapeActionInputStrategyRegistry.createDefault();
    private final TapeInstructionCodec instructionCodec = new TapeInstructionCodec();
    private final IdentityHashMap<ScheduleInstruction, Integer> actionSelection = new IdentityHashMap<>();
    private final IdentityHashMap<ScheduleInstruction, Integer> checkBlockTargetSelection = new IdentityHashMap<>();
    private final IdentityHashMap<ScheduleInstruction, Integer> rotateOptionSelection = new IdentityHashMap<>();
    private final IdentityHashMap<ScheduleInstruction, Integer> moveDistanceSelection = new IdentityHashMap<>();
    private final IdentityHashMap<ScheduleInstruction, Boolean> moveStepCheckLinkSelection = new IdentityHashMap<>();
    private final IdentityHashMap<ScheduleInstruction, Integer> checkBlockMatchActionSelection = new IdentityHashMap<>();
    private final IdentityHashMap<ScheduleInstruction, Integer> runTapeRepeatSelection = new IdentityHashMap<>();
    private final IdentityHashMap<ScheduleInstruction, Integer> interactTargetSelection = new IdentityHashMap<>();
    private final IdentityHashMap<ScheduleInstruction, Integer> interactModeSelection = new IdentityHashMap<>();
    private int editingActionIndex = 0;
    private int editingCheckBlockTargetIndex = 0;
    private int editingRotateOptionIndex = 0;
    private int editingMoveDistanceIndex = 0;
    private boolean editingMoveStepCheckLink = false;
    private int editingCheckBlockMatchActionIndex = 0;
    private int editingRunTapeRepeatIndex = 0;
    private int editingHasItemTargetIndex = 0;
    private int editingHasItemActionIndex = 0;
    private int editingInteractTargetIndex = 0;
    private int editingInteractModeIndex = 0;
    private boolean scheduleSavedToServer = false;
    private boolean closeAfterSave = false;
    private int closeAfterSaveTicks = 0;

    private static final List<String> PAL_ACTION_KEYS = List.of(
            "move",
            "rotate",
            "check_block",
            "has_item",
            "run_tape",
            "interact"
    );

    private static final List<Component> PAL_ACTION_OPTIONS = List.of(
            Component.literal("Move"),
            Component.literal("Rotate"),
            Component.literal("Check Block"),
            Component.literal("Has Item"),
            Component.literal("Run Tape"),
            Component.literal("Interact")
    );

    private static final List<Component> CHECK_BLOCK_TARGET_OPTIONS = List.of(
            Component.literal("Below"),
            Component.literal("Above"),
            Component.literal("Front")
    );

    private static final List<String> CHECK_BLOCK_MATCH_ACTION_KEYS = List.of(
            "harvest",
            "chop"
    );

    private static final List<Component> CHECK_BLOCK_MATCH_ACTION_OPTIONS = List.of(
            Component.literal("Harvest"),
            Component.literal("Chop")
    );

    private static final List<String> HAS_ITEM_ACTION_KEYS = List.of(
            "use",
            "placeholder"
    );

    private static final List<Component> HAS_ITEM_ACTION_OPTIONS = List.of(
            Component.literal("Use"),
            Component.literal("Placeholder")
    );

    private static final List<String> INTERACT_TARGET_KEYS = List.of(
            "storage"
    );

    private static final List<Component> INTERACT_TARGET_OPTIONS = List.of(
            Component.literal("Storage")
    );

    private static final List<String> INTERACT_MODE_KEYS = List.of(
            "pull",
            "push"
    );

    private static final List<Component> INTERACT_MODE_OPTIONS = List.of(
            Component.literal("Pull"),
            Component.literal("Push")
    );

    private static final List<Component> ROTATE_OPTIONS = List.of(
            Component.literal("Right"),
            Component.literal("Left"),
            Component.literal("North"),
            Component.literal("East"),
            Component.literal("South"),
            Component.literal("West")
    );

    private static final List<Component> MOVE_FORWARD_OPTIONS = List.of(
            Component.literal("Forward")
    );

    private static final List<Component> INACTIVE_SECONDARY_OPTIONS = List.of(
            Component.empty(),
            Component.empty(),
            Component.empty(),
            Component.empty()
    );

    private static final List<Component> INACTIVE_TERTIARY_OPTIONS = List.of(
            Component.empty(),
            Component.empty(),
            Component.empty(),
            Component.empty()
    );

    private static final int MIN_MOVE_DISTANCE = 1;
    private static final int MAX_MOVE_DISTANCE = 99;
    private static final String MOVE_LINK_TOOLTIP_PREFIX = "Link next Check after each Move step: ";

    private static final String ACTION_KEY_TAG = "PalActionKey";
    private static final String CHECK_BLOCK_TARGET_INDEX_TAG = "PalCheckBlockTargetIndex";
    private static final String ROTATE_OPTION_INDEX_TAG = "PalRotateOptionIndex";
    private static final String MOVE_DISTANCE_INDEX_TAG = "PalMoveDistanceIndex";
    private static final String MOVE_STEP_CHECK_LINK_TAG = "PalMoveStepCheckLink";
    private static final String CHECK_BLOCK_MATCH_ACTION_KEY_TAG = "PalCheckBlockMatchActionKey";
    private static final String CHECK_BLOCK_MATCH_ITEM_TAG = "PalCheckBlockMatchItem";
    private static final String HAS_ITEM_TARGET_INDEX_TAG = "PalHasItemTargetIndex";
    private static final String HAS_ITEM_ACTION_KEY_TAG = "PalHasItemActionKey";
    private static final String HAS_ITEM_MATCH_ITEM_TAG = "PalHasItemMatchItem";
    private static final String RUN_TAPE_ITEM_TAG = "PalRunTapeItem";
    private static final String RUN_TAPE_REPEAT_COUNT_TAG = "PalRunTapeRepeatCount";
    private static final String INTERACT_TARGET_KEY_TAG = "PalInteractTargetKey";
    private static final String INTERACT_MODE_KEY_TAG = "PalInteractModeKey";
    private static final String INTERACT_FILTER_ITEM_TAG = "PalInteractFilterItem";
    private static final String INTERACT_KEEP_ITEM_TAG = "PalInteractKeepItem";

    /**
     * Implements TapeProgramScreen behavior for the programmable pal feature.
     */
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
    /**
     * Handles the init lifecycle step for this screen/entity.
     */
    protected void init() {
        AllGuiTextures bg = AllGuiTextures.SCHEDULE;
        setWindowSize(bg.getWidth(), bg.getHeight());
        super.init();
        clearWidgets();

        tapeCustomName = menu.contentHolder.has(DataComponents.CUSTOM_NAME)
                ? menu.contentHolder.get(DataComponents.CUSTOM_NAME).getString()
                : "";

        tapeNameBox = new EditBox(font, leftPos + 41, topPos + 18, 150, 14, Component.literal("Tape Name"));
        tapeNameBox.setMaxLength(48);
        tapeNameBox.setBordered(true);
        tapeNameBox.setValue(tapeCustomName);
        tapeNameBox.setHint(Component.literal("Tape Name"));
        tapeNameBox.visible = false;
        tapeNameBox.active = false;
        addRenderableWidget(tapeNameBox);

        renameButton = Button.builder(Component.literal("✎"), b -> toggleTapeNameEditing())
                .pos(leftPos + bg.getWidth() - 58, topPos + 2)
                .size(14, 14)
                .build();
        addRenderableWidget(renameButton);

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

    /**
     * Manages editor UI state in startEditing.
     */
    protected void startEditing(IScheduleInput field, Consumer<Boolean> onClose, boolean allowDeletion) {
        onEditorClose = onClose;
        confirmButton.visible = false;
        cyclicButton.visible = false;
        cyclicIndicator.visible = false;

        scrollInput = new SelectionScrollInput(leftPos + 56, topPos + 65, 143, 16);
        secondaryBackgroundInput = new SelectionScrollInput(leftPos + 77, topPos + 87, 58, 16);
        tertiaryBackgroundInput = new SelectionScrollInput(leftPos + 140, topPos + 87, 58, 16);
        moveDistanceInput = new ScrollInput(leftPos + 77, topPos + 87, 58, 16);
        wideOptionInput = new SelectionScrollInput(leftPos + 77, topPos + 87, 121, 16);
        secondaryScrollLabel = new Label(leftPos + 80, topPos + 91, CommonComponents.EMPTY).withShadow();
        tertiaryScrollLabel = new Label(leftPos + 143, topPos + 91, CommonComponents.EMPTY).withShadow();
        wideOptionLabel = new Label(leftPos + 80, topPos + 91, CommonComponents.EMPTY).withShadow();
        secondaryScrollLabel.visible = false;
        tertiaryScrollLabel.visible = false;
        wideOptionLabel.visible = false;
        scrollInputLabel = new Label(leftPos + 59, topPos + 69, CommonComponents.EMPTY).withShadow();
        editorConfirm = new IconButton(leftPos + 56 + 168, topPos + 65 + 22, AllIcons.I_CONFIRM);
        moveLinkToggleButton = new IconButton(leftPos + 56, topPos + 87, AllIcons.I_REFRESH);
        moveLinkToggleButton.withCallback(() -> {
            editingMoveStepCheckLink = !editingMoveStepCheckLink;
            moveLinkToggleButton.green = editingMoveStepCheckLink;
            updateMoveLinkToggleTooltip();
        });
        if (allowDeletion)
            editorDelete = new IconButton(leftPos + 56 - 45, topPos + 65 + 22, AllIcons.I_TRASH);
        menu.slotsActive = true;
        clearEditorTargetSlots();

        for (int i = 0; i < field.slotsTargeted(); i++) {
            ItemStack item = field.getItem(i);
            menu.ghostInventory.setStackInSlot(i, item);
        }

        if (field instanceof ScheduleInstruction instruction) {
            editingDestination = instruction;
            editingActionIndex = getActionIndex(instruction);
            editingCheckBlockTargetIndex = getCheckBlockTargetIndex(instruction);
            editingRotateOptionIndex = getRotateOptionIndex(instruction);
            editingMoveDistanceIndex = getMoveDistanceIndex(instruction);
            editingMoveStepCheckLink = getMoveStepCheckLinkEnabled(instruction);
            editingCheckBlockMatchActionIndex = getCheckBlockMatchActionIndex(instruction);
            editingRunTapeRepeatIndex = getRunTapeRepeatIndex(instruction);
            editingHasItemTargetIndex = getHasItemTargetIndex(instruction);
            editingHasItemActionIndex = getHasItemActionIndex(instruction);
            editingInteractTargetIndex = getInteractTargetIndex(instruction);
            editingInteractModeIndex = getInteractModeIndex(instruction);
            updateEditorSubwidgets(editingDestination);
            if (isCheckBlockAction(editingActionIndex))
                menu.ghostInventory.setStackInSlot(0, getCheckBlockMatchItem(instruction));
            if (isHasItemAction(editingActionIndex))
                menu.ghostInventory.setStackInSlot(0, getHasItemMatchItem(instruction));
            if (isRunTapeAction(editingActionIndex))
                menu.ghostInventory.setStackInSlot(0, getRunTapeItem(instruction));
            if (isInteractAction(editingActionIndex)) {
                menu.ghostInventory.setStackInSlot(0, getInteractFilterItem(instruction));
                menu.ghostInventory.setStackInSlot(1, getInteractKeepItem(instruction));
            }

            scrollInput.forOptions(PAL_ACTION_OPTIONS)
                    .titled(Component.literal("Action"))
                    .setState(editingActionIndex)
                    .writingTo(scrollInputLabel)
                    .calling(index -> {
                        editingActionIndex = index;
                        updateSecondarySelector();
                    });
            updateSecondarySelector();
        }



        addRenderableWidget(scrollInput);
        addRenderableWidget(secondaryBackgroundInput);
        addRenderableWidget(tertiaryBackgroundInput);
        addRenderableWidget(moveDistanceInput);
        addRenderableWidget(wideOptionInput);
        addRenderableWidget(moveLinkToggleButton);
        addRenderableWidget(scrollInputLabel);
        addRenderableWidget(secondaryScrollLabel);
        addRenderableWidget(tertiaryScrollLabel);
        addRenderableWidget(wideOptionLabel);
        addRenderableWidget(editorConfirm);
        if (allowDeletion)
            addRenderableWidget(editorDelete);
    }

    /**
     * Returns data needed by getPalInstructionTypes.
     */
    private List<Pair<ResourceLocation, Supplier<? extends ScheduleInstruction>>> getPalInstructionTypes() {
        List<Pair<ResourceLocation, Supplier<? extends ScheduleInstruction>>> filteredTypes = new ArrayList<>();

        for (Pair<ResourceLocation, Supplier<? extends ScheduleInstruction>> instructionType : Schedule.INSTRUCTION_TYPES) {
            String path = instructionType.getFirst().getPath();
            if (path.contains("rename") || path.contains("delivery") || path.contains("retrieval"))
                filteredTypes.add(instructionType);
        }

        return filteredTypes;
    }

    private int clampIndex(int value, int size) {
        return instructionCodec.clampIndex(value, size);
    }

    /**
     * Returns data needed by getCheckBlockTargetIndex.
     */
    private int getCheckBlockTargetIndex(ScheduleInstruction instruction) {
        return instructionCodec.getStoredClampedIndex(instruction, CHECK_BLOCK_TARGET_INDEX_TAG,
                checkBlockTargetSelection, 0, CHECK_BLOCK_TARGET_OPTIONS.size());
    }

    /**
     * Returns data needed by getCheckBlockMatchActionIndex.
     */
    private int getCheckBlockMatchActionIndex(ScheduleInstruction instruction) {
        return instructionCodec.getKeyedIndexOrCached(instruction, CHECK_BLOCK_MATCH_ACTION_KEY_TAG,
                CHECK_BLOCK_MATCH_ACTION_KEYS, checkBlockMatchActionSelection, CHECK_BLOCK_MATCH_ACTION_OPTIONS.size());
    }

    /**
     * Returns data needed by getCheckBlockMatchItem.
     */
    private ItemStack getCheckBlockMatchItem(ScheduleInstruction instruction) {
        return instructionCodec.getOptionalItem(instruction, CHECK_BLOCK_MATCH_ITEM_TAG, menu.player.registryAccess());
    }

    /**
     * Returns data needed by getCheckBlockMatchActionKeyForIndex.
     */
    private String getCheckBlockMatchActionKeyForIndex(int index) {
        return instructionCodec.getKeyForIndex(index, CHECK_BLOCK_MATCH_ACTION_KEYS);
    }

    private int getHasItemTargetIndex(ScheduleInstruction instruction) {
        return instructionCodec.getTagIntClampedOrDefault(instruction, HAS_ITEM_TARGET_INDEX_TAG, 0,
                CHECK_BLOCK_TARGET_OPTIONS.size());
    }

    private int getHasItemActionIndex(ScheduleInstruction instruction) {
        return instructionCodec.getKeyedIndexOrDefault(instruction, HAS_ITEM_ACTION_KEY_TAG, HAS_ITEM_ACTION_KEYS, 0,
                HAS_ITEM_ACTION_OPTIONS.size());
    }

    private ItemStack getHasItemMatchItem(ScheduleInstruction instruction) {
        return instructionCodec.getOptionalItem(instruction, HAS_ITEM_MATCH_ITEM_TAG, menu.player.registryAccess());
    }

    private String getHasItemActionKeyForIndex(int index) {
        return instructionCodec.getKeyForIndex(index, HAS_ITEM_ACTION_KEYS);
    }

    private ItemStack getRunTapeItem(ScheduleInstruction instruction) {
        return instructionCodec.getRunTapeItem(instruction, RUN_TAPE_ITEM_TAG, menu.player.registryAccess());
    }

    private int getRunTapeRepeatIndex(ScheduleInstruction instruction) {
        return instructionCodec.getStoredClampedIndex(instruction, RUN_TAPE_REPEAT_COUNT_TAG, runTapeRepeatSelection, 0,
                MAX_MOVE_DISTANCE);
    }


    private int getInteractTargetIndex(ScheduleInstruction instruction) {
        return instructionCodec.getKeyedIndexOrCached(instruction, INTERACT_TARGET_KEY_TAG,
                INTERACT_TARGET_KEYS, interactTargetSelection, INTERACT_TARGET_OPTIONS.size());
    }

    private int getInteractModeIndex(ScheduleInstruction instruction) {
        return instructionCodec.getKeyedIndexOrCached(instruction, INTERACT_MODE_KEY_TAG,
                INTERACT_MODE_KEYS, interactModeSelection, INTERACT_MODE_OPTIONS.size());
    }

    private String getInteractTargetKeyForIndex(int index) {
        return instructionCodec.getKeyForIndex(index, INTERACT_TARGET_KEYS);
    }

    private String getInteractModeKeyForIndex(int index) {
        return instructionCodec.getKeyForIndex(index, INTERACT_MODE_KEYS);
    }

    private ItemStack getInteractFilterItem(ScheduleInstruction instruction) {
        return instructionCodec.getOptionalItem(instruction, INTERACT_FILTER_ITEM_TAG, menu.player.registryAccess());
    }

    private ItemStack getInteractKeepItem(ScheduleInstruction instruction) {
        return instructionCodec.getOptionalItem(instruction, INTERACT_KEEP_ITEM_TAG, menu.player.registryAccess());
    }
    /**
     * Returns data needed by getRotateOptionIndex.
     */
    private int getRotateOptionIndex(ScheduleInstruction instruction) {
        return instructionCodec.getStoredClampedIndex(instruction, ROTATE_OPTION_INDEX_TAG, rotateOptionSelection, 0,
                ROTATE_OPTIONS.size());
    }

    /**
     * Returns data needed by getMoveDistanceIndex.
     */
    private int getMoveDistanceIndex(ScheduleInstruction instruction) {
        return instructionCodec.getStoredClampedIndex(instruction, MOVE_DISTANCE_INDEX_TAG, moveDistanceSelection, 0,
                MAX_MOVE_DISTANCE);
    }


    /**
     * Checks whether move instruction should run the following check-block after each step.
     */
    private boolean getMoveStepCheckLinkEnabled(ScheduleInstruction instruction) {
        return instructionCodec.getBooleanOrDefault(instruction, MOVE_STEP_CHECK_LINK_TAG, moveStepCheckLinkSelection);
    }

    /**
     * Manages editor UI state in updateSecondarySelector.
     */
    private void updateSecondarySelector() {
        if (secondaryBackgroundInput == null || secondaryScrollLabel == null || tertiaryBackgroundInput == null
                || tertiaryScrollLabel == null || moveDistanceInput == null || wideOptionInput == null
                || wideOptionLabel == null || moveLinkToggleButton == null)
            return;

        resetEditorSelectorLayout();
        refreshEditorBackgrounds();

        String actionKey = getActionKeyForIndex(editingActionIndex);
        actionInputStrategyRegistry.configure(actionKey, this);
    }



    private void resetEditorSelectorLayout() {
        scrollInput.setPosition(leftPos + 53, topPos + 64);
        scrollInput.setWidth(145);

        secondaryBackgroundInput.setPosition(leftPos + 77, topPos + 87);
        secondaryBackgroundInput.setWidth(58);

        tertiaryBackgroundInput.setPosition(leftPos + 140, topPos + 87);
        tertiaryBackgroundInput.setWidth(58);

        moveDistanceInput.setPosition(leftPos + 77, topPos + 87);
        moveDistanceInput.setWidth(58);

        wideOptionInput.setPosition(leftPos + 77, topPos + 87);
        wideOptionInput.setWidth(121);

        scrollInputLabel.setX(leftPos + 56);
        scrollInputLabel.setY(topPos + 68);
        secondaryScrollLabel.setX(leftPos + 80);
        secondaryScrollLabel.setY(topPos + 91);
        tertiaryScrollLabel.setX(leftPos + 143);
        tertiaryScrollLabel.setY(topPos + 91);
    }
    void setEditorLabelVisibility(boolean secondaryVisible, boolean tertiaryVisible, boolean wideVisible) {
        secondaryScrollLabel.visible = secondaryVisible;
        tertiaryScrollLabel.visible = tertiaryVisible;
        wideOptionLabel.visible = wideVisible;
    }

    void configureSelectionInput(SelectionScrollInput input, List<Component> options, MutableComponent title,
                                         Label label, int state, IntConsumer onChange) {
        input.forOptions(options)
                .titled(title)
                .setState(clampIndex(state, options.size()))
                .writingTo(label)
                .calling(onChange::accept);
    }

    void configureRangeInput(ScrollInput input, int minInclusive, int maxExclusive, MutableComponent title,
                                     Label label, int state, IntConsumer onChange) {
        int clampedState = Mth.clamp(state, minInclusive, maxExclusive - 1);
        input.withRange(minInclusive, maxExclusive)
                .titled(title)
                .setState(clampedState)
                .writingTo(label)
                .calling(onChange::accept);
    }

    void setInputVisibility(SelectionScrollInput input, boolean active, boolean visible) {
        input.active = active;
        input.visible = visible;
    }

    void setInputVisibility(ScrollInput input, boolean active, boolean visible) {
        input.active = active;
        input.visible = visible;
    }

    void clearEditorTargetSlots() {
        menu.targetSlotsActive = 0;
        menu.ghostInventory.setStackInSlot(0, ItemStack.EMPTY);
        menu.ghostInventory.setStackInSlot(1, ItemStack.EMPTY);
    }

    void updateMoveLinkToggleTooltip() {
        moveLinkToggleButton.getToolTip().clear();
        moveLinkToggleButton.getToolTip().add(
                Component.literal(MOVE_LINK_TOOLTIP_PREFIX + (editingMoveStepCheckLink ? "ON" : "OFF")));
    }

    int leftPosValue() {
        return leftPos;
    }

    int topPosValue() {
        return topPos;
    }


    SelectionScrollInput scrollInputWidget() { return scrollInput; }
    SelectionScrollInput secondaryBackgroundInputWidget() { return secondaryBackgroundInput; }
    SelectionScrollInput tertiaryBackgroundInputWidget() { return tertiaryBackgroundInput; }
    ScrollInput moveDistanceInputWidget() { return moveDistanceInput; }
    SelectionScrollInput wideOptionInputWidget() { return wideOptionInput; }
    IconButton moveLinkToggleButtonWidget() { return moveLinkToggleButton; }
    Label scrollInputLabelWidget() { return scrollInputLabel; }
    Label secondaryScrollLabelWidget() { return secondaryScrollLabel; }
    Label tertiaryScrollLabelWidget() { return tertiaryScrollLabel; }
    Label wideOptionLabelWidget() { return wideOptionLabel; }

    int getEditingCheckBlockTargetIndex() { return editingCheckBlockTargetIndex; }
    void setEditingCheckBlockTargetIndex(int value) { editingCheckBlockTargetIndex = value; }
    int getEditingCheckBlockMatchActionIndex() { return editingCheckBlockMatchActionIndex; }
    void setEditingCheckBlockMatchActionIndex(int value) { editingCheckBlockMatchActionIndex = value; }
    int getEditingHasItemTargetIndex() { return editingHasItemTargetIndex; }
    void setEditingHasItemTargetIndex(int value) { editingHasItemTargetIndex = value; }
    int getEditingHasItemActionIndex() { return editingHasItemActionIndex; }
    void setEditingHasItemActionIndex(int value) { editingHasItemActionIndex = value; }
    int getEditingRotateOptionIndex() { return editingRotateOptionIndex; }
    void setEditingRotateOptionIndex(int value) { editingRotateOptionIndex = value; }
    int getEditingRunTapeRepeatIndex() { return editingRunTapeRepeatIndex; }
    void setEditingRunTapeRepeatIndexFromState(int value) {
        editingRunTapeRepeatIndex = Mth.clamp(value - 1, 0, MAX_MOVE_DISTANCE - 1);
    }
    int getEditingInteractTargetIndex() { return editingInteractTargetIndex; }
    void setEditingInteractTargetIndex(int value) { editingInteractTargetIndex = value; }
    int getEditingInteractModeIndex() { return editingInteractModeIndex; }
    void setEditingInteractModeIndex(int value) { editingInteractModeIndex = value; }
    int getEditingMoveDistanceIndex() { return editingMoveDistanceIndex; }
    void setEditingMoveDistanceIndexFromState(int value) {
        editingMoveDistanceIndex = Mth.clamp(value - 1, 0, MAX_MOVE_DISTANCE - 1);
    }
    boolean isEditingMoveStepCheckLink() { return editingMoveStepCheckLink; }

    List<Component> checkBlockTargetOptions() { return CHECK_BLOCK_TARGET_OPTIONS; }
    List<Component> checkBlockMatchActionOptions() { return CHECK_BLOCK_MATCH_ACTION_OPTIONS; }
    List<Component> hasItemActionOptions() { return HAS_ITEM_ACTION_OPTIONS; }
    List<Component> interactTargetOptions() { return INTERACT_TARGET_OPTIONS; }
    List<Component> interactModeOptions() { return INTERACT_MODE_OPTIONS; }
    List<Component> rotateOptions() { return ROTATE_OPTIONS; }
    List<Component> moveForwardOptions() { return MOVE_FORWARD_OPTIONS; }
    List<Component> inactiveSecondaryOptions() { return INACTIVE_SECONDARY_OPTIONS; }
    List<Component> inactiveTertiaryOptions() { return INACTIVE_TERTIARY_OPTIONS; }
    int minMoveDistance() { return MIN_MOVE_DISTANCE; }
    int maxMoveDistance() { return MAX_MOVE_DISTANCE; }
    void setTargetSlotsActive(int count) { menu.targetSlotsActive = count; }

    /**
     * Applies selector setup for the check-block action.
     */
    void configureCheckBlockInputs() {
        TapeActionInputLayouts.configureCheckBlockInputs(this);
    }

    /**
     * Applies selector setup for the has-item action.
     */
    void configureHasItemInputs() {
        TapeActionInputLayouts.configureHasItemInputs(this);
    }

    /**
     * Applies selector setup for the rotate action.
     */
    void configureRotateInputs() {
        TapeActionInputLayouts.configureRotateInputs(this);
    }

    /**
     * Applies selector setup for the run-tape action.
     */
    void configureRunTapeInputs() {
        TapeActionInputLayouts.configureRunTapeInputs(this);
    }

    /**
     * Applies selector setup for the interact action.
     */
    void configureInteractInputs() {
        TapeActionInputLayouts.configureInteractInputs(this);
    }

    /**
     * Applies selector setup for the move action.
     */
    void configureMoveInputs() {
        TapeActionInputLayouts.configureMoveInputs(this);
    }

    /**
     * Applies a safe inactive state for selectors when no secondary inputs are needed.
     */
    void configureInactiveInputs() {
        TapeActionInputLayouts.configureInactiveInputs(this);
    }

    /**
     * Returns data needed by getActionIndex.
     */
    private int getActionIndex(ScheduleInstruction instruction) {
        return instructionCodec.getActionIndex(instruction, ACTION_KEY_TAG, actionSelection, PAL_ACTION_KEYS,
                PAL_ACTION_OPTIONS.size());
    }

    /**
     * Returns data needed by getActionIndexByKey.
     */
    private int getActionIndexByKey(String actionKey) {
        return instructionCodec.getActionIndexByKey(actionKey, PAL_ACTION_KEYS, PAL_ACTION_OPTIONS.size());
    }

    /**
     * Returns data needed by getActionKeyForIndex.
     */
    private String getActionKeyForIndex(int index) {
        return instructionCodec.getKeyForIndex(index, PAL_ACTION_KEYS);
    }

    /**
     * Checks the state used by isCheckBlockAction.
     */
    private boolean isCheckBlockAction(int index) {
        return "check_block".equals(getActionKeyForIndex(index));
    }

    /**
     * Checks the state used by isMoveAction.
     */
    private boolean isMoveAction(int index) {
        return "move".equals(getActionKeyForIndex(index));
    }

    private boolean isRotateAction(int index) {
        return "rotate".equals(getActionKeyForIndex(index));
    }

    private boolean isHasItemAction(int index) {
        return "has_item".equals(getActionKeyForIndex(index));
    }

    private boolean isRunTapeAction(int index) {
        return "run_tape".equals(getActionKeyForIndex(index));
    }

    private boolean isInteractAction(int index) {
        return "interact".equals(getActionKeyForIndex(index));
    }

    private int getTargetSlotCountForAction(int index, IScheduleInput fallback) {
        if (isInteractAction(index))
            return 2;
        if (isCheckBlockAction(index) || isHasItemAction(index) || isRunTapeAction(index))
            return 1;
        return fallback.slotsTargeted();
    }

    private boolean isItemSlotAction(int index) {
        return isCheckBlockAction(index) || isHasItemAction(index) || isRunTapeAction(index) || isInteractAction(index);
    }

    /**
     * Returns data needed by getActionLabel.
     */
    private Component getActionLabel(ScheduleInstruction instruction) {
        int actionIndex = getActionIndex(instruction);
        Component action = PAL_ACTION_OPTIONS.get(actionIndex);

        if (isCheckBlockAction(actionIndex)) {
            Component target = CHECK_BLOCK_TARGET_OPTIONS.get(getCheckBlockTargetIndex(instruction));
            Component matchAction = CHECK_BLOCK_MATCH_ACTION_OPTIONS.get(getCheckBlockMatchActionIndex(instruction));
            return Component.literal(action.getString() + " " + target.getString() + " -> " + matchAction.getString());
        }

        if (isMoveAction(actionIndex)) {
            int distance = getMoveDistanceIndex(instruction) + 1;
            String linked = getMoveStepCheckLinkEnabled(instruction) ? " + LinkCheck" : "";
            return Component.literal(action.getString() + " " + distance + " Forward" + linked);
        }

        if (isRotateAction(actionIndex)) {
            Component direction = ROTATE_OPTIONS.get(getRotateOptionIndex(instruction));
            return Component.literal(action.getString() + " " + direction.getString());
        }

        if (isHasItemAction(actionIndex)) {
            Component target = CHECK_BLOCK_TARGET_OPTIONS.get(getHasItemTargetIndex(instruction));
            Component actionOption = HAS_ITEM_ACTION_OPTIONS.get(getHasItemActionIndex(instruction));
            return Component.literal(action.getString() + " " + target.getString() + " -> " + actionOption.getString());
        }

        if (isRunTapeAction(actionIndex)) {
            int runs = getRunTapeRepeatIndex(instruction) + 1;
            ItemStack runTape = getRunTapeItem(instruction);
            if (!runTape.isEmpty())
                return Component.literal(action.getString() + " x" + runs + " " + runTape.getHoverName().getString());
            return Component.literal(action.getString() + " x" + runs);
        }

        if (isInteractAction(actionIndex)) {
            Component target = INTERACT_TARGET_OPTIONS.get(getInteractTargetIndex(instruction));
            Component mode = INTERACT_MODE_OPTIONS.get(getInteractModeIndex(instruction));
            ItemStack filter = getInteractFilterItem(instruction);
            ItemStack keep = getInteractKeepItem(instruction);
            String filterName = filter.isEmpty() ? "Any" : filter.getHoverName().getString();
            String keepName = keep.isEmpty() ? "None" : keep.getHoverName().getString();
            return Component.literal(action.getString() + " " + target.getString() + " " + mode.getString()
                    + " [" + filterName + "] keep [" + keepName + "]");
        }

        return action;
    }

    /**
     * Returns data needed by getActionName.
     */
    private String getActionName(ScheduleInstruction instruction) {
        return getActionLabel(instruction).getString();
    }

    /**
     * Returns data needed by getDisplaySummary.
     */
    private Pair<ItemStack, Component> getDisplaySummary(ScheduleInstruction instruction) {
        return Pair.of(ItemStack.EMPTY, getActionLabel(instruction));
    }

    /**
     * Manages editor UI state in stopEditing.
     */
    private void removeEditorWidgets() {
        removeWidget(scrollInput);
        removeWidget(secondaryBackgroundInput);
        removeWidget(tertiaryBackgroundInput);
        removeWidget(moveDistanceInput);
        removeWidget(wideOptionInput);
        removeWidget(moveLinkToggleButton);
        removeWidget(scrollInputLabel);
        removeWidget(secondaryScrollLabel);
        removeWidget(tertiaryScrollLabel);
        removeWidget(wideOptionLabel);
        removeWidget(editorConfirm);
        removeWidget(editorDelete);
    }

    private void clearEditingReferences() {
        editingCondition = null;
        editingDestination = null;
        editorConfirm = null;
        secondaryBackgroundInput = null;
        tertiaryBackgroundInput = null;
        moveDistanceInput = null;
        wideOptionInput = null;
        moveLinkToggleButton = null;
        secondaryScrollLabel = null;
        tertiaryScrollLabel = null;
        wideOptionLabel = null;
        editorDelete = null;
    }

    protected void stopEditing() {
        confirmButton.visible = true;
        cyclicButton.visible = true;
        cyclicIndicator.visible = true;

        if (editingCondition == null && editingDestination == null)
            return;

        removeEditorWidgets();

        IScheduleInput editing = editingCondition == null ? editingDestination : editingCondition;
        int activeTargetSlots = getTargetSlotCountForAction(editingActionIndex, editing);
        for (int i = 0; i < activeTargetSlots; i++)
            editing.setItem(i, menu.ghostInventory.getStackInSlot(i));

        if (editingDestination != null) {
            CompoundTag destinationData = editingDestination.getData();
            destinationData.putString(ACTION_KEY_TAG, getActionKeyForIndex(editingActionIndex));
            actionSelection.put(editingDestination, editingActionIndex);

            if (isCheckBlockAction(editingActionIndex)) {
                destinationData.putInt(CHECK_BLOCK_TARGET_INDEX_TAG, editingCheckBlockTargetIndex);
                destinationData.putString(CHECK_BLOCK_MATCH_ACTION_KEY_TAG,
                        getCheckBlockMatchActionKeyForIndex(editingCheckBlockMatchActionIndex));
                ItemStack matchItem = menu.ghostInventory.getStackInSlot(0);
                if (matchItem.isEmpty())
                    destinationData.remove(CHECK_BLOCK_MATCH_ITEM_TAG);
                else
                    destinationData.put(CHECK_BLOCK_MATCH_ITEM_TAG, matchItem.saveOptional(menu.player.registryAccess()));
                checkBlockTargetSelection.put(editingDestination, editingCheckBlockTargetIndex);
                checkBlockMatchActionSelection.put(editingDestination, editingCheckBlockMatchActionIndex);
            } else {
                destinationData.remove(CHECK_BLOCK_TARGET_INDEX_TAG);
                destinationData.remove(CHECK_BLOCK_MATCH_ACTION_KEY_TAG);
                destinationData.remove(CHECK_BLOCK_MATCH_ITEM_TAG);
                checkBlockTargetSelection.remove(editingDestination);
                checkBlockMatchActionSelection.remove(editingDestination);
            }

            if (isHasItemAction(editingActionIndex)) {
                destinationData.putInt(HAS_ITEM_TARGET_INDEX_TAG, editingHasItemTargetIndex);
                destinationData.putString(HAS_ITEM_ACTION_KEY_TAG, getHasItemActionKeyForIndex(editingHasItemActionIndex));
                ItemStack hasItemStack = menu.ghostInventory.getStackInSlot(0);
                if (hasItemStack.isEmpty())
                    destinationData.remove(HAS_ITEM_MATCH_ITEM_TAG);
                else
                    destinationData.put(HAS_ITEM_MATCH_ITEM_TAG, hasItemStack.saveOptional(menu.player.registryAccess()));
            } else {
                destinationData.remove(HAS_ITEM_TARGET_INDEX_TAG);
                destinationData.remove(HAS_ITEM_ACTION_KEY_TAG);
                destinationData.remove(HAS_ITEM_MATCH_ITEM_TAG);
            }

            if (isRunTapeAction(editingActionIndex)) {
                ItemStack runTapeStack = menu.ghostInventory.getStackInSlot(0);
                destinationData.putInt(RUN_TAPE_REPEAT_COUNT_TAG, editingRunTapeRepeatIndex);
                runTapeRepeatSelection.put(editingDestination, editingRunTapeRepeatIndex);
                if (runTapeStack.isEmpty() || runTapeStack.getItem() != ModItems.PROGRAMMABLE_TAPE.get()) {
                    destinationData.remove(RUN_TAPE_ITEM_TAG);
                    editingDestination.setItem(0, ItemStack.EMPTY);
                } else {
                    destinationData.put(RUN_TAPE_ITEM_TAG, runTapeStack.saveOptional(menu.player.registryAccess()));
                    editingDestination.setItem(0, runTapeStack.copy());
                }
            } else {
                destinationData.remove(RUN_TAPE_ITEM_TAG);
                destinationData.remove(RUN_TAPE_REPEAT_COUNT_TAG);
                runTapeRepeatSelection.remove(editingDestination);
            }

            if (isInteractAction(editingActionIndex)) {
                destinationData.putString(INTERACT_TARGET_KEY_TAG, getInteractTargetKeyForIndex(editingInteractTargetIndex));
                destinationData.putString(INTERACT_MODE_KEY_TAG, getInteractModeKeyForIndex(editingInteractModeIndex));
                interactTargetSelection.put(editingDestination, editingInteractTargetIndex);
                interactModeSelection.put(editingDestination, editingInteractModeIndex);

                ItemStack filterItem = menu.ghostInventory.getStackInSlot(0);
                ItemStack keepItem = menu.ghostInventory.getStackInSlot(1);
                if (filterItem.isEmpty())
                    destinationData.remove(INTERACT_FILTER_ITEM_TAG);
                else
                    destinationData.put(INTERACT_FILTER_ITEM_TAG, filterItem.saveOptional(menu.player.registryAccess()));
                if (keepItem.isEmpty())
                    destinationData.remove(INTERACT_KEEP_ITEM_TAG);
                else
                    destinationData.put(INTERACT_KEEP_ITEM_TAG, keepItem.saveOptional(menu.player.registryAccess()));
            } else {
                destinationData.remove(INTERACT_TARGET_KEY_TAG);
                destinationData.remove(INTERACT_MODE_KEY_TAG);
                destinationData.remove(INTERACT_FILTER_ITEM_TAG);
                destinationData.remove(INTERACT_KEEP_ITEM_TAG);
                interactTargetSelection.remove(editingDestination);
                interactModeSelection.remove(editingDestination);
            }

            if (isMoveAction(editingActionIndex)) {
                destinationData.putInt(MOVE_DISTANCE_INDEX_TAG, editingMoveDistanceIndex);
                destinationData.putBoolean(MOVE_STEP_CHECK_LINK_TAG, editingMoveStepCheckLink);
                moveDistanceSelection.put(editingDestination, editingMoveDistanceIndex);
                moveStepCheckLinkSelection.put(editingDestination, editingMoveStepCheckLink);
            } else {
                destinationData.remove(MOVE_DISTANCE_INDEX_TAG);
                destinationData.remove(MOVE_STEP_CHECK_LINK_TAG);
                moveDistanceSelection.remove(editingDestination);
                moveStepCheckLinkSelection.remove(editingDestination);
            }

            if (isRotateAction(editingActionIndex)) {
                destinationData.putInt(ROTATE_OPTION_INDEX_TAG, editingRotateOptionIndex);
                rotateOptionSelection.put(editingDestination, editingRotateOptionIndex);
            } else {
                destinationData.remove(ROTATE_OPTION_INDEX_TAG);
                rotateOptionSelection.remove(editingDestination);
            }
        }

        editorSubWidgets.saveValues(editing.getData());
        editorSubWidgets.forEach(this::removeWidget);
        editorSubWidgets.clear();

        clearEditingReferences();
        menu.slotsActive = false;
        init();
    }

    /**
     * Manages editor UI state in updateEditorSubwidgets.
     */
    protected void updateEditorSubwidgets(IScheduleInput field) {
        menu.targetSlotsActive = getTargetSlotCountForAction(editingActionIndex, field);
        resetEditorSelectorLayout();
        refreshEditorBackgrounds();
    }

    /**
     * Manages editor UI state in refreshEditorBackgrounds.
     */
    private void refreshEditorBackgrounds() {
        editorSubWidgets.forEach(this::removeWidget);
        editorSubWidgets.clear();

        if (isMoveAction(editingActionIndex)) {
            editorSubWidgets.add(Pair.of(new TooltipArea(leftPos + 77, topPos + 87, 58, 18), "move_distance_selector_background"));
            editorSubWidgets.add(Pair.of(new TooltipArea(leftPos + 140, topPos + 87, 58, 18), "move_forward_selector_background"));
            return;
        }

        if (isRotateAction(editingActionIndex)) {
            editorSubWidgets.add(Pair.of(new TooltipArea(leftPos + 77, topPos + 87, 121, 18), "rotate_selector_background"));
            return;
        }

        if (isCheckBlockAction(editingActionIndex)) {
            // Bottom row only; top split is rendered directly in renderBg for precise alignment.
            editorSubWidgets.add(Pair.of(new TooltipArea(leftPos + 77, topPos + 87, 121, 18), "check_block_match_selector_background"));
            return;
        }

        if (isHasItemAction(editingActionIndex)) {
            // Bottom row only; top split is rendered directly in renderBg for precise alignment.
            editorSubWidgets.add(Pair.of(new TooltipArea(leftPos + 77, topPos + 87, 121, 18), "check_block_match_selector_background"));
            return;
        }

        if (isRunTapeAction(editingActionIndex)) {
            editorSubWidgets.add(Pair.of(new TooltipArea(leftPos + 77, topPos + 87, 121, 18), "run_tape_runs_selector_background"));
            return;
        }

        if (isInteractAction(editingActionIndex)) {
            // Bottom row only; top split is rendered directly in renderBg for precise alignment.
            editorSubWidgets.add(Pair.of(new TooltipArea(leftPos + 97, topPos + 87, 101, 18), "rotate_selector_background"));
            return;
        }
    }

    @Override
    /**
     * Handles the containerTick lifecycle step for this screen/entity.
     */
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
    /**
     * Handles the render lifecycle step for this screen/entity.
     */
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


    /**
     * Implements renderSchedule behavior for the programmable pal feature.
     */
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

    /**
     * Processes action input handling for the screen.
     */
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
                entry.instruction.getData().putString(ACTION_KEY_TAG, getActionKeyForIndex(editingActionIndex));
                if (isCheckBlockAction(editingActionIndex)) {
                    entry.instruction.getData().putInt(CHECK_BLOCK_TARGET_INDEX_TAG, editingCheckBlockTargetIndex);
                    entry.instruction.getData().putString(CHECK_BLOCK_MATCH_ACTION_KEY_TAG, getCheckBlockMatchActionKeyForIndex(editingCheckBlockMatchActionIndex));
                    ItemStack matchItem = menu.ghostInventory.getStackInSlot(0);
                    if (!matchItem.isEmpty())
                        entry.instruction.getData().put(CHECK_BLOCK_MATCH_ITEM_TAG, matchItem.saveOptional(menu.player.registryAccess()));
                }
                if (isHasItemAction(editingActionIndex)) {
                    entry.instruction.getData().putInt(HAS_ITEM_TARGET_INDEX_TAG, editingHasItemTargetIndex);
                    entry.instruction.getData().putString(HAS_ITEM_ACTION_KEY_TAG, getHasItemActionKeyForIndex(editingHasItemActionIndex));
                    ItemStack hasItemStack = menu.ghostInventory.getStackInSlot(0);
                    if (!hasItemStack.isEmpty())
                        entry.instruction.getData().put(HAS_ITEM_MATCH_ITEM_TAG, hasItemStack.saveOptional(menu.player.registryAccess()));
                }
                if (isRunTapeAction(editingActionIndex)) {
                    entry.instruction.getData().putInt(RUN_TAPE_REPEAT_COUNT_TAG, editingRunTapeRepeatIndex);
                    ItemStack runTapeStack = menu.ghostInventory.getStackInSlot(0);
                    if (!runTapeStack.isEmpty() && runTapeStack.getItem() == ModItems.PROGRAMMABLE_TAPE.get()) {
                        entry.instruction.getData().put(RUN_TAPE_ITEM_TAG, runTapeStack.saveOptional(menu.player.registryAccess()));
                        entry.instruction.setItem(0, runTapeStack.copy());
                    }
                }

                if (isInteractAction(editingActionIndex)) {
                    entry.instruction.getData().putString(INTERACT_TARGET_KEY_TAG, getInteractTargetKeyForIndex(editingInteractTargetIndex));
                    entry.instruction.getData().putString(INTERACT_MODE_KEY_TAG, getInteractModeKeyForIndex(editingInteractModeIndex));
                    ItemStack filterItem = menu.ghostInventory.getStackInSlot(0);
                    ItemStack keepItem = menu.ghostInventory.getStackInSlot(1);
                    if (!filterItem.isEmpty())
                        entry.instruction.getData().put(INTERACT_FILTER_ITEM_TAG, filterItem.saveOptional(menu.player.registryAccess()));
                    if (!keepItem.isEmpty())
                        entry.instruction.getData().put(INTERACT_KEEP_ITEM_TAG, keepItem.saveOptional(menu.player.registryAccess()));
                }
                if (isMoveAction(editingActionIndex)) {
                    entry.instruction.getData().putInt(MOVE_DISTANCE_INDEX_TAG, editingMoveDistanceIndex);
                    entry.instruction.getData().putBoolean(MOVE_STEP_CHECK_LINK_TAG, editingMoveStepCheckLink);
                }
                if (isRotateAction(editingActionIndex)) {
                    entry.instruction.getData().putInt(ROTATE_OPTION_INDEX_TAG, editingRotateOptionIndex);
                }
                actionSelection.put(entry.instruction, editingActionIndex);
                if (isCheckBlockAction(editingActionIndex)) {
                    checkBlockTargetSelection.put(entry.instruction, editingCheckBlockTargetIndex);
                    checkBlockMatchActionSelection.put(entry.instruction, editingCheckBlockMatchActionIndex);
                }

                if (isInteractAction(editingActionIndex)) {
                    interactTargetSelection.put(entry.instruction, editingInteractTargetIndex);
                    interactModeSelection.put(entry.instruction, editingInteractModeIndex);
                }
                if (isMoveAction(editingActionIndex)) {
                    moveDistanceSelection.put(entry.instruction, editingMoveDistanceIndex);
                    moveStepCheckLinkSelection.put(entry.instruction, editingMoveStepCheckLink);
                }
                if (isRotateAction(editingActionIndex)) {
                    rotateOptionSelection.put(entry.instruction, editingRotateOptionIndex);
                }
                if (isRunTapeAction(editingActionIndex)) {
                    runTapeRepeatSelection.put(entry.instruction, editingRunTapeRepeatIndex);
                }
                schedule.entries.add(entry);
            }, true);
        }
        return true;
    }

    /**
     * Implements renderActionTooltip behavior for the programmable pal feature.
     */
    private void renderActionTooltip(@Nullable GuiGraphics graphics, List<Component> tooltip, int mx, int my) {
        if (graphics != null)
            graphics.renderTooltip(font, tooltip, Optional.empty(), mx, my);
    }

    /**
     * Returns data needed by getFieldSize.
     */
    private int getFieldSize(int minSize, Pair<ItemStack, Component> pair) {
        ItemStack stack = pair.getFirst();
        Component text = pair.getSecond();
        boolean hasItem = !stack.isEmpty();
        return Math.max((text == null ? 0 : font.width(text)) + (hasItem ? 20 : 0) + 16, minSize);
    }

    @Override
    /**
     * Processes mouseClicked input handling for the screen.
     */
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
    /**
     * Processes keyPressed input handling for the screen.
     */
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
    /**
     * Processes mouseScrolled input handling for the screen.
     */
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
    /**
     * Handles the renderForeground lifecycle step for this screen/entity.
     */
    protected void renderForeground(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.renderForeground(graphics, mouseX, mouseY, partialTicks);
        action(graphics, mouseX, mouseY, -1);


        if (editingCondition == null && editingDestination == null)
            return;

        int x = leftPos + 53;
        int y = topPos + 87;
        if (mouseX < x || mouseY < y || mouseX >= x + 120 || mouseY >= y + 18)
            return;

        IScheduleInput rendered = editingCondition == null ? editingDestination : editingCondition;

        int renderedTargetSlots = getTargetSlotCountForAction(editingActionIndex, rendered);
        for (int i = 0; i < Math.max(1, renderedTargetSlots); i++) {
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
    /**
     * Handles the renderBg lifecycle step for this screen/entity.
     */
    protected void renderBg(GuiGraphics graphics, float pPartialTick, int pMouseX, int pMouseY) {
        AllGuiTextures.SCHEDULE.render(graphics, leftPos, topPos);
        Component headerTitle = tapeCustomName.isBlank() ? title : Component.literal(tapeCustomName);
        FormattedCharSequence formattedcharsequence = headerTitle.getVisualOrderText();
        int center = leftPos + (AllGuiTextures.SCHEDULE.getWidth() - 8) / 2;
        graphics.drawString(font, formattedcharsequence, (float) (center - font.width(formattedcharsequence) / 2),
                (float) topPos + 4, 0x505050, false);
        renderSchedule(graphics, pMouseX, pMouseY, pPartialTick);

        if (editingCondition == null && editingDestination == null)
            return;

        graphics.fillGradient(0, 0, this.width, this.height, -1072689136, -804253680);
        graphics.blit(TAPE_PROGRAM_EDITOR_TEXTURE, leftPos - 2, topPos + 40, 0, 0, 256, 89, 256, 256);
        if (scrollInput != null && scrollInput.visible) {
            renderSelectorBackground(graphics, scrollInput.getX(), scrollInput.getY(), scrollInput.getWidth());
            if (editingDestination != null
                    && (isInteractAction(editingActionIndex) || isCheckBlockAction(editingActionIndex) || isHasItemAction(editingActionIndex))
                    && tertiaryBackgroundInput != null && tertiaryBackgroundInput.visible)
                renderSelectorBackground(graphics, tertiaryBackgroundInput.getX(), tertiaryBackgroundInput.getY(),
                        tertiaryBackgroundInput.getWidth());
        }
        AllGuiTextures.PLAYER_INVENTORY.render(graphics, leftPos + 38, topPos + 122);
        graphics.drawString(font, playerInventoryTitle, leftPos + 46, topPos + 128, 0x505050, false);

        formattedcharsequence = editingCondition == null ? CreateLang.translateDirect("schedule.instruction.editor")
                .getVisualOrderText()
                : CreateLang.translateDirect("schedule.condition.editor")
                .getVisualOrderText();
        graphics.drawString(font, formattedcharsequence, (float) (center - font.width(formattedcharsequence) / 2),
                (float) topPos + 44, 0x505050, false);

        IScheduleInput rendered = editingCondition == null ? editingDestination : editingCondition;

        int renderedTargetSlots = getTargetSlotCountForAction(editingActionIndex, rendered);
        for (int i = 0; i < renderedTargetSlots; i++)
            AllGuiTextures.SCHEDULE_EDITOR_ADDITIONAL_SLOT.render(graphics, leftPos + 53 + 20 * i, topPos + 87);

        if (renderedTargetSlots == 0) {
            AllGuiTextures.SCHEDULE_EDITOR_INACTIVE_SLOT.render(graphics, leftPos + 53, topPos + 87);
        }

        PoseStack pPoseStack = graphics.pose();
        pPoseStack.pushPose();
        pPoseStack.translate(0, getGuiTop() + 87, 0);
        int editorBackgroundX = getGuiLeft() + 77;
        if (editingDestination != null && (isInteractAction(editingActionIndex) || isCheckBlockAction(editingActionIndex)))
            editorBackgroundX = getGuiLeft() + 97;
        editorSubWidgets.renderWidgetBG(editorBackgroundX, graphics);
        pPoseStack.popPose();

    }

    private void renderSelectorBackground(GuiGraphics graphics, int x, int y, int width) {
        UIRenderHelper.drawStretched(graphics, x, y, width, 18, 0, AllGuiTextures.DATA_AREA);
        AllGuiTextures.DATA_AREA_START.render(graphics, x, y);
        AllGuiTextures.DATA_AREA_END.render(graphics, x + width - 2, y);
    }

    private void toggleTapeNameEditing() {
        renamingTapeName = !renamingTapeName;
        if (renamingTapeName) {
            tapeNameBox.visible = true;
            tapeNameBox.active = true;
            tapeNameBox.setValue(tapeCustomName);
            setFocused(tapeNameBox);
            return;
        }

        applyTapeNameFromBox();
        tapeNameBox.visible = false;
        tapeNameBox.active = false;
    }

    private void applyTapeNameFromBox() {
        if (tapeNameBox == null)
            return;
        tapeCustomName = tapeNameBox.getValue().trim();
    }

    /**
     * Implements saveProgram behavior for the programmable pal feature.
     */
    private void saveProgram() {
        if (scheduleSavedToServer)
            return;
        scheduleSavedToServer = true;

        if (renamingTapeName) {
            applyTapeNameFromBox();
            renamingTapeName = false;
            tapeNameBox.visible = false;
            tapeNameBox.active = false;
        }

        CompoundTag scheduleTag = schedule.entries.isEmpty() ? new CompoundTag() : schedule.write(menu.player.registryAccess());
        PacketDistributor.sendToServer(new SaveTapeProgramPacket(scheduleTag, tapeCustomName));
    }

    /**
     * Implements saveAndClose behavior for the programmable pal feature.
     */
    private void saveAndClose() {
        saveProgram();
        closeAfterSave = true;
        closeAfterSaveTicks = 2;
    }

    @Override
    /**
     * Implements removed behavior for the programmable pal feature.
     */
    public void removed() {
        saveProgram();
        super.removed();
    }

    @Override
    /**
     * Returns data needed by getExtraAreas.
     */
    public List<Rect2i> getExtraAreas() {
        return extraAreas;
    }

    /**
     * Returns data needed by getFont.
     */
    public Font getFont() {
        return font;
    }

}






























