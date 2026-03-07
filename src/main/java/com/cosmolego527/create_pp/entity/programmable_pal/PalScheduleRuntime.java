package com.cosmolego527.create_pp.entity.programmable_pal;

import com.cosmolego527.create_pp.component.ModDataComponentTypes;
import com.simibubi.create.content.trains.schedule.Schedule;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

final class PalScheduleRuntime {

    private PalScheduleRuntime() {
    }

    static @Nullable Schedule getScheduleFromTape(Level level, ItemStack instructions) {
        if (instructions.isEmpty())
            return null;

        CompoundTag programTag = instructions.get(ModDataComponentTypes.VOID_FUNCTION_DATA);
        if (programTag == null || programTag.isEmpty())
            return null;

        Schedule schedule = Schedule.fromTag(level.registryAccess(), programTag);
        if (schedule.entries.isEmpty())
            return null;

        return schedule;
    }

    static boolean hasProgramData(ItemStack instructions) {
        if (instructions.isEmpty())
            return false;

        CompoundTag programTag = instructions.get(ModDataComponentTypes.VOID_FUNCTION_DATA);
        return programTag != null && !programTag.isEmpty();
    }

    static int advanceMainInstructionPointer(Level level, ItemStack instructions, int currentPointer) {
        Schedule schedule = getScheduleFromTape(level, instructions);
        if (schedule == null)
            return 0;

        int nextPointer = currentPointer + 1;
        if (nextPointer >= schedule.entries.size())
            return 0;
        return nextPointer;
    }
}