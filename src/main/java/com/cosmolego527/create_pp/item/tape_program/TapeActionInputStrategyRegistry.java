package com.cosmolego527.create_pp.item.tape_program;

import java.util.Map;
import java.util.function.Consumer;

final class TapeActionInputStrategyRegistry {

    private static final Consumer<TapeProgramScreen> INACTIVE = TapeProgramScreen::configureInactiveInputs;

    private final Map<String, Consumer<TapeProgramScreen>> strategies;

    private TapeActionInputStrategyRegistry(Map<String, Consumer<TapeProgramScreen>> strategies) {
        this.strategies = strategies;
    }

    static TapeActionInputStrategyRegistry createDefault() {
        return new TapeActionInputStrategyRegistry(Map.of(
                "check_block", TapeProgramScreen::configureCheckBlockInputs,
                "move", TapeProgramScreen::configureMoveInputs,
                "rotate", TapeProgramScreen::configureRotateInputs,
                "has_item", TapeProgramScreen::configureHasItemInputs,
                "run_tape", TapeProgramScreen::configureRunTapeInputs,
                "interact", TapeProgramScreen::configureInteractInputs
        ));
    }

    void configure(String actionKey, TapeProgramScreen screen) {
        strategies.getOrDefault(actionKey, INACTIVE).accept(screen);
    }
}
