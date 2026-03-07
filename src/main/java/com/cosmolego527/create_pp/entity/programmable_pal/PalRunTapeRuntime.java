package com.cosmolego527.create_pp.entity.programmable_pal;

import com.cosmolego527.create_pp.component.ModDataComponentTypes;
import com.cosmolego527.create_pp.item.ModItems;
import com.simibubi.create.content.trains.schedule.Schedule;
import com.simibubi.create.content.trains.schedule.ScheduleEntry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

final class PalRunTapeRuntime {

    private PalRunTapeRuntime() {
    }

    private static @Nullable Schedule resolveNestedSchedule(Level level, CompoundTag data, String runTapeItemTag,
                                                    Item programmableTapeItem) {
        if (!data.contains(runTapeItemTag))
            return null;

        ItemStack nestedTape = ItemStack.parseOptional(level.registryAccess(), data.getCompound(runTapeItemTag));
        if (nestedTape.isEmpty() || nestedTape.getItem() != programmableTapeItem)
            return null;

        CompoundTag nestedProgramTag = nestedTape.get(ModDataComponentTypes.VOID_FUNCTION_DATA);
        if (nestedProgramTag == null || nestedProgramTag.isEmpty())
            return null;

        Schedule nestedSchedule = Schedule.fromTag(level.registryAccess(), nestedProgramTag);
        if (nestedSchedule.entries.isEmpty())
            return null;

        return nestedSchedule;
    }

    private static @Nullable CompoundTag resolveNestedProgramTag(Level level, CompoundTag data, String runTapeItemTag,
                                                         Item programmableTapeItem) {
        if (!data.contains(runTapeItemTag))
            return null;

        ItemStack nestedTape = ItemStack.parseOptional(level.registryAccess(), data.getCompound(runTapeItemTag));
        if (nestedTape.isEmpty() || nestedTape.getItem() != programmableTapeItem)
            return null;

        CompoundTag nestedProgramTag = nestedTape.get(ModDataComponentTypes.VOID_FUNCTION_DATA);
        if (nestedProgramTag == null || nestedProgramTag.isEmpty())
            return null;

        return nestedProgramTag;
    }

    private static int resolveRepeatCount(CompoundTag data, String repeatCountTag) {
        return Math.max(1, data.getInt(repeatCountTag) + 1);
    }

    static void startRunTape(ProgrammablePalEntity pal, CompoundTag data) {
        CompoundTag nestedProgramTag = resolveNestedProgramTag(pal.level(), data,
                PalTagKeys.RUN_TAPE_ITEM, ModItems.PROGRAMMABLE_TAPE.get());
        if (nestedProgramTag == null) {
            pal.clearActiveRunTapeState();
            return;
        }

        Schedule nestedSchedule = resolveNestedSchedule(pal.level(), data,
                PalTagKeys.RUN_TAPE_ITEM, ModItems.PROGRAMMABLE_TAPE.get());
        if (nestedSchedule == null) {
            pal.clearActiveRunTapeState();
            return;
        }

        pal.setActiveRunTapeProgramTag(nestedProgramTag.copy());
        pal.setActiveRunTapeInstructionPointer(0);
        pal.setActiveRunTapeRemainingRuns(resolveRepeatCount(data, PalTagKeys.RUN_TAPE_REPEAT_COUNT));
    }

    static boolean executeActiveRunTapeStep(ProgrammablePalEntity pal) {
        CompoundTag activeProgram = pal.getActiveRunTapeProgramTag();
        if (activeProgram == null)
            return true;

        Schedule nestedSchedule = Schedule.fromTag(pal.level().registryAccess(), activeProgram);
        if (nestedSchedule.entries.isEmpty()) {
            pal.clearActiveRunTapeState();
            return true;
        }

        if (pal.getActiveRunTapeInstructionPointer() >= nestedSchedule.entries.size()) {
            pal.clearActiveRunTapeState();
            return true;
        }

        int pointer = pal.getActiveRunTapeInstructionPointer();
        var nestedInstruction = nestedSchedule.entries.get(pointer).instruction;
        pal.setActiveRunTapeInstructionPointer(pointer + 1);

        pal.incrementRunTapeDepth();
        try {
            pal.executeInstruction(nestedInstruction, nestedSchedule);
        } finally {
            pal.decrementRunTapeDepth();
        }

        if (pal.consumeRepeatCurrentInstruction()) {
            pal.setActiveRunTapeInstructionPointer(pointer);
            return false;
        }

        if (pal.getActiveRunTapeInstructionPointer() >= nestedSchedule.entries.size()) {
            if (pal.getActiveRunTapeRemainingRuns() > 1) {
                pal.decrementActiveRunTapeRemainingRuns();
                pal.setActiveRunTapeInstructionPointer(0);
                return false;
            }
            pal.clearActiveRunTapeState();
            return true;
        }

        return false;
    }

    static void executeRunTapeImmediate(ProgrammablePalEntity pal, CompoundTag data) {
        if (pal.getRunTapeDepth() > PalRuntimeConfig.MAX_RUN_TAPE_DEPTH)
            return;

        Schedule nestedSchedule = resolveNestedSchedule(pal.level(), data,
                PalTagKeys.RUN_TAPE_ITEM, ModItems.PROGRAMMABLE_TAPE.get());
        if (nestedSchedule == null)
            return;

        int repeatCount = resolveRepeatCount(data, PalTagKeys.RUN_TAPE_REPEAT_COUNT);

        pal.incrementRunTapeDepth();
        try {
            for (int run = 0; run < repeatCount; run++) {
                for (ScheduleEntry entry : nestedSchedule.entries)
                    pal.executeInstruction(entry.instruction, nestedSchedule);
            }
        } finally {
            pal.decrementRunTapeDepth();
        }
    }
}