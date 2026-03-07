package com.cosmolego527.create_pp.entity.programmable_pal;

import com.simibubi.create.content.trains.schedule.Schedule;
import com.simibubi.create.content.trains.schedule.destination.ScheduleInstruction;
import net.minecraft.nbt.CompoundTag;

final class PalProgramRuntime {

    private PalProgramRuntime() {
    }

    static void runProgramTick(ProgrammablePalEntity pal) {
        if (pal.isFightTapeActive()) {
            pal.handleFightTapeProgramTick();
            return;
        }

        if (!pal.hasActiveInstructionTape()) {
            pal.handleNoInstructionTapeProgramTick();
            return;
        }

        pal.captureProgramStartIfNeeded();

        if (pal.tickChopTask())
            return;

        if (pal.consumeInstructionCooldown())
            return;

        if (pal.handleQueuedMoveProgramTick())
            return;

        if (pal.handleActiveRunTapeProgramTick())
            return;

        pal.executeMainInstructionProgramTick();
    }

    static boolean executeInstruction(ProgrammablePalEntity pal, ScheduleInstruction instruction, Schedule schedule) {
        CompoundTag data = instruction.getData();
        String action = data.getString(PalTagKeys.ACTION_KEY);

        if (PalTagKeys.ACTION_CHECK_BLOCK.equals(action)) {
            pal.executeCheckBlock(data);
            return true;
        }

        if (PalTagKeys.ACTION_HAS_ITEM.equals(action)) {
            pal.executeHasItem(data);
            return true;
        }

        if (PalTagKeys.ACTION_ROTATE.equals(action)) {
            pal.executeRotate(data);
            return true;
        }

        if (PalTagKeys.ACTION_RUN_TAPE.equals(action)) {
            if (pal.isNestedRunTapeExecution()) {
                pal.executeRunTapeImmediate(data);
                return true;
            }

            pal.startRunTape(data);
            if (!pal.hasActiveRunTapeProgram())
                return true;
            return pal.executeActiveRunTapeStep();
        }

        if (PalTagKeys.ACTION_INTERACT.equals(action)) {
            pal.executeInteract(data);
            return true;
        }

        if (PalTagKeys.ACTION_MOVE.equals(action)) {
            pal.executeMoveForward(data, schedule);
            return true;
        }

        return true;
    }
}