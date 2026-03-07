package com.cosmolego527.create_pp.entity.programmable_pal;

final class PalTagKeys {

    private PalTagKeys() {
    }

    static final String ACTION_KEY = "PalActionKey";

    static final String ACTION_MOVE = "move";
    static final String ACTION_ROTATE = "rotate";
    static final String ACTION_CHECK_BLOCK = "check_block";
    static final String ACTION_HAS_ITEM = "has_item";
    static final String ACTION_RUN_TAPE = "run_tape";
    static final String ACTION_INTERACT = "interact";

    static final String CHECK_BLOCK_TARGET_INDEX = "PalCheckBlockTargetIndex";
    static final String MOVE_DISTANCE_INDEX = "PalMoveDistanceIndex";
    static final String ROTATE_OPTION_INDEX = "PalRotateOptionIndex";
    static final String MOVE_STEP_CHECK_LINK = "PalMoveStepCheckLink";
    static final String CHECK_BLOCK_MATCH_ACTION_KEY = "PalCheckBlockMatchActionKey";
    static final String CHECK_BLOCK_MATCH_ITEM = "PalCheckBlockMatchItem";
    static final String HAS_ITEM_TARGET_INDEX = "PalHasItemTargetIndex";
    static final String HAS_ITEM_ACTION_KEY = "PalHasItemActionKey";
    static final String HAS_ITEM_MATCH_ITEM = "PalHasItemMatchItem";
    static final String RUN_TAPE_ITEM = "PalRunTapeItem";
    static final String RUN_TAPE_REPEAT_COUNT = "PalRunTapeRepeatCount";
    static final String INTERACT_TARGET_KEY = "PalInteractTargetKey";
    static final String INTERACT_MODE_KEY = "PalInteractModeKey";
    static final String INTERACT_FILTER_ITEM = "PalInteractFilterItem";
    static final String INTERACT_KEEP_ITEM = "PalInteractKeepItem";
}