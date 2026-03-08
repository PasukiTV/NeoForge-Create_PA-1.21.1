package com.cosmolego527.create_pp.item.tape_program;

import net.minecraft.network.chat.Component;

final class TapeActionInputLayouts {

    private TapeActionInputLayouts() {
    }

    static void configureCheckBlockInputs(TapeProgramScreen screen) {
        screen.scrollInputWidget().setPosition(screen.leftPosValue() + 53, screen.topPosValue() + 64);
        screen.scrollInputWidget().setWidth(82);

        screen.tertiaryBackgroundInputWidget().setPosition(screen.leftPosValue() + 140, screen.topPosValue() + 64);
        screen.tertiaryBackgroundInputWidget().setWidth(58);
        screen.configureSelectionInput(screen.tertiaryBackgroundInputWidget(), screen.checkBlockTargetOptions(),
                Component.literal("Target"), screen.tertiaryScrollLabelWidget(), screen.getEditingCheckBlockTargetIndex(),
                screen::setEditingCheckBlockTargetIndex);
        screen.setInputVisibility(screen.tertiaryBackgroundInputWidget(), true, true);

        screen.secondaryBackgroundInputWidget().setPosition(screen.leftPosValue() + 77, screen.topPosValue() + 87);
        screen.secondaryBackgroundInputWidget().setWidth(121);
        screen.configureSelectionInput(screen.secondaryBackgroundInputWidget(), screen.checkBlockMatchActionOptions(),
                Component.literal("On Match"), screen.secondaryScrollLabelWidget(), screen.getEditingCheckBlockMatchActionIndex(),
                screen::setEditingCheckBlockMatchActionIndex);
        screen.setInputVisibility(screen.secondaryBackgroundInputWidget(), true, true);

        screen.scrollInputLabelWidget().setX(screen.leftPosValue() + 56);
        screen.scrollInputLabelWidget().setY(screen.topPosValue() + 68);
        screen.tertiaryScrollLabelWidget().setX(screen.leftPosValue() + 143);
        screen.tertiaryScrollLabelWidget().setY(screen.topPosValue() + 68);
        screen.secondaryScrollLabelWidget().setX(screen.leftPosValue() + 80);
        screen.secondaryScrollLabelWidget().setY(screen.topPosValue() + 91);

        screen.setInputVisibility(screen.moveDistanceInputWidget(), false, false);
        screen.setInputVisibility(screen.wideOptionInputWidget(), false, false);
        screen.setEditorLabelVisibility(true, true, false);
        screen.moveLinkToggleButtonWidget().active = false;
        screen.moveLinkToggleButtonWidget().visible = false;
        screen.setTargetSlotsActive(1);
    }

    static void configureHasItemInputs(TapeProgramScreen screen) {
        screen.scrollInputWidget().setPosition(screen.leftPosValue() + 53, screen.topPosValue() + 64);
        screen.scrollInputWidget().setWidth(82);

        screen.tertiaryBackgroundInputWidget().setPosition(screen.leftPosValue() + 140, screen.topPosValue() + 64);
        screen.tertiaryBackgroundInputWidget().setWidth(58);
        screen.configureSelectionInput(screen.tertiaryBackgroundInputWidget(), screen.checkBlockTargetOptions(),
                Component.literal("Target"), screen.tertiaryScrollLabelWidget(), screen.getEditingHasItemTargetIndex(),
                screen::setEditingHasItemTargetIndex);
        screen.setInputVisibility(screen.tertiaryBackgroundInputWidget(), true, true);

        screen.secondaryBackgroundInputWidget().setPosition(screen.leftPosValue() + 77, screen.topPosValue() + 87);
        screen.secondaryBackgroundInputWidget().setWidth(121);
        screen.configureSelectionInput(screen.secondaryBackgroundInputWidget(), screen.hasItemActionOptions(),
                Component.literal("Action"), screen.secondaryScrollLabelWidget(), screen.getEditingHasItemActionIndex(),
                screen::setEditingHasItemActionIndex);
        screen.setInputVisibility(screen.secondaryBackgroundInputWidget(), true, true);

        screen.scrollInputLabelWidget().setX(screen.leftPosValue() + 56);
        screen.scrollInputLabelWidget().setY(screen.topPosValue() + 68);
        screen.tertiaryScrollLabelWidget().setX(screen.leftPosValue() + 143);
        screen.tertiaryScrollLabelWidget().setY(screen.topPosValue() + 68);
        screen.secondaryScrollLabelWidget().setX(screen.leftPosValue() + 80);
        screen.secondaryScrollLabelWidget().setY(screen.topPosValue() + 91);

        screen.setInputVisibility(screen.moveDistanceInputWidget(), false, false);
        screen.setInputVisibility(screen.wideOptionInputWidget(), false, false);
        screen.setEditorLabelVisibility(true, true, false);
        screen.moveLinkToggleButtonWidget().active = false;
        screen.moveLinkToggleButtonWidget().visible = false;
        screen.setTargetSlotsActive(1);
    }

    static void configureRotateInputs(TapeProgramScreen screen) {
        screen.setInputVisibility(screen.secondaryBackgroundInputWidget(), false, false);
        screen.setInputVisibility(screen.tertiaryBackgroundInputWidget(), false, false);
        screen.setInputVisibility(screen.moveDistanceInputWidget(), false, false);

        screen.configureSelectionInput(screen.wideOptionInputWidget(), screen.rotateOptions(),
                Component.literal("Direction"), screen.wideOptionLabelWidget(), screen.getEditingRotateOptionIndex(),
                screen::setEditingRotateOptionIndex);
        screen.setInputVisibility(screen.wideOptionInputWidget(), true, true);
        screen.setEditorLabelVisibility(false, false, true);
        screen.moveLinkToggleButtonWidget().active = false;
        screen.moveLinkToggleButtonWidget().visible = false;
        screen.clearEditorTargetSlots();
    }

    static void configureRunTapeInputs(TapeProgramScreen screen) {
        screen.moveDistanceInputWidget().setPosition(screen.leftPosValue() + 77, screen.topPosValue() + 87);
        screen.moveDistanceInputWidget().setWidth(121);
        screen.secondaryScrollLabelWidget().setX(screen.leftPosValue() + 80);
        screen.configureRangeInput(screen.moveDistanceInputWidget(), screen.minMoveDistance(), screen.maxMoveDistance() + 1,
                Component.literal("Runs"), screen.secondaryScrollLabelWidget(), screen.getEditingRunTapeRepeatIndex() + 1,
                screen::setEditingRunTapeRepeatIndexFromState);
        screen.setInputVisibility(screen.moveDistanceInputWidget(), true, true);
        screen.setInputVisibility(screen.secondaryBackgroundInputWidget(), false, false);
        screen.setInputVisibility(screen.tertiaryBackgroundInputWidget(), false, false);
        screen.setInputVisibility(screen.wideOptionInputWidget(), false, false);
        screen.setEditorLabelVisibility(true, false, false);
        screen.moveLinkToggleButtonWidget().active = false;
        screen.moveLinkToggleButtonWidget().visible = false;
        screen.setTargetSlotsActive(1);
    }

    static void configureInteractInputs(TapeProgramScreen screen) {
        screen.scrollInputWidget().setPosition(screen.leftPosValue() + 53, screen.topPosValue() + 64);
        screen.scrollInputWidget().setWidth(82);

        screen.tertiaryBackgroundInputWidget().setPosition(screen.leftPosValue() + 140, screen.topPosValue() + 64);
        screen.tertiaryBackgroundInputWidget().setWidth(58);
        screen.configureSelectionInput(screen.tertiaryBackgroundInputWidget(), screen.interactTargetOptions(),
                Component.literal("Target"), screen.tertiaryScrollLabelWidget(), screen.getEditingInteractTargetIndex(),
                screen::setEditingInteractTargetIndex);
        screen.setInputVisibility(screen.tertiaryBackgroundInputWidget(), true, true);

        screen.secondaryBackgroundInputWidget().setPosition(screen.leftPosValue() + 140, screen.topPosValue() + 87);
        screen.secondaryBackgroundInputWidget().setWidth(58);
        screen.configureSelectionInput(screen.secondaryBackgroundInputWidget(), screen.interactModeOptions(),
                Component.literal("Mode"), screen.secondaryScrollLabelWidget(), screen.getEditingInteractModeIndex(), index -> {
                    screen.setEditingInteractModeIndex(index);
                    screen.moveDistanceInputWidget().titled(screen.interactStackLimitLabel());
                    screen.setInteractStackLimitLabelValue(screen.getEditingInteractMaxStacksIndex());
                });
        screen.setInputVisibility(screen.secondaryBackgroundInputWidget(), true, true);

        screen.moveDistanceInputWidget().setPosition(screen.leftPosValue() + 77, screen.topPosValue() + 87);
        screen.moveDistanceInputWidget().setWidth(58);
        screen.configureRangeInput(screen.moveDistanceInputWidget(), 0, screen.maxInteractStacks() + 1,
                screen.interactStackLimitLabel(), screen.wideOptionLabelWidget(), screen.getEditingInteractMaxStacksIndex(),
                value -> {
                    screen.setEditingInteractMaxStacksIndexFromState(value);
                    screen.setInteractStackLimitLabelValue(value);
                });
        screen.setInputVisibility(screen.moveDistanceInputWidget(), true, true);
        screen.setInteractStackLimitLabelValue(screen.getEditingInteractMaxStacksIndex());

        screen.scrollInputLabelWidget().setX(screen.leftPosValue() + 56);
        screen.scrollInputLabelWidget().setY(screen.topPosValue() + 68);
        screen.tertiaryScrollLabelWidget().setX(screen.leftPosValue() + 143);
        screen.tertiaryScrollLabelWidget().setY(screen.topPosValue() + 68);
        screen.secondaryScrollLabelWidget().setX(screen.leftPosValue() + 143);
        screen.secondaryScrollLabelWidget().setY(screen.topPosValue() + 91);
        screen.wideOptionLabelWidget().setX(screen.leftPosValue() + 80);
        screen.wideOptionLabelWidget().setY(screen.topPosValue() + 91);

        screen.setInputVisibility(screen.wideOptionInputWidget(), false, false);
        screen.setEditorLabelVisibility(true, true, true);
        screen.moveLinkToggleButtonWidget().active = false;
        screen.moveLinkToggleButtonWidget().visible = false;
        screen.setTargetSlotsActive(1);
    }

    static void configureMoveInputs(TapeProgramScreen screen) {
        screen.configureRangeInput(screen.moveDistanceInputWidget(), screen.minMoveDistance(), screen.maxMoveDistance() + 1,
                Component.literal("Distance"), screen.secondaryScrollLabelWidget(), screen.getEditingMoveDistanceIndex() + 1,
                screen::setEditingMoveDistanceIndexFromState);
        screen.setInputVisibility(screen.moveDistanceInputWidget(), true, true);

        screen.setInputVisibility(screen.secondaryBackgroundInputWidget(), false, false);
        screen.configureSelectionInput(screen.tertiaryBackgroundInputWidget(), screen.moveForwardOptions(),
                Component.literal("Direction"), screen.tertiaryScrollLabelWidget(), 0, index -> {
                });
        screen.setInputVisibility(screen.tertiaryBackgroundInputWidget(), false, true);
        screen.setInputVisibility(screen.wideOptionInputWidget(), false, false);
        screen.setEditorLabelVisibility(true, true, false);
        screen.moveLinkToggleButtonWidget().active = true;
        screen.moveLinkToggleButtonWidget().visible = true;
        screen.moveLinkToggleButtonWidget().green = screen.isEditingMoveStepCheckLink();
        screen.updateMoveLinkToggleTooltip();
        screen.clearEditorTargetSlots();
    }

    static void configureInactiveInputs(TapeProgramScreen screen) {
        screen.configureSelectionInput(screen.secondaryBackgroundInputWidget(), screen.inactiveSecondaryOptions(),
                Component.empty(), screen.secondaryScrollLabelWidget(), 0, index -> {
                });
        screen.setInputVisibility(screen.secondaryBackgroundInputWidget(), false, false);
        screen.setInputVisibility(screen.moveDistanceInputWidget(), false, false);
        screen.configureSelectionInput(screen.tertiaryBackgroundInputWidget(), screen.inactiveTertiaryOptions(),
                Component.empty(), screen.tertiaryScrollLabelWidget(), 0, index -> {
                });
        screen.setInputVisibility(screen.tertiaryBackgroundInputWidget(), false, false);
        screen.setInputVisibility(screen.wideOptionInputWidget(), false, false);
        screen.setEditorLabelVisibility(false, false, false);
        screen.moveLinkToggleButtonWidget().active = false;
        screen.moveLinkToggleButtonWidget().visible = false;
        screen.clearEditorTargetSlots();
    }
}
