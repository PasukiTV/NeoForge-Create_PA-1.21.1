package com.cosmolego527.create_pp.item.tape_program;

import com.simibubi.create.content.trains.schedule.destination.ScheduleInstruction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import java.util.IdentityHashMap;
import java.util.List;

final class TapeInstructionCodec {

    int clampIndex(int value, int size) {
        if (size <= 0)
            return 0;
        return Math.max(0, Math.min(value, size - 1));
    }

    int getStoredIndexOrDefault(CompoundTag data, String key, IdentityHashMap<ScheduleInstruction, Integer> cache,
                                ScheduleInstruction instruction, int fallback) {
        return data.contains(key) ? data.getInt(key) : cache.getOrDefault(instruction, fallback);
    }


    int getStoredClampedIndex(ScheduleInstruction instruction, String indexTag,
                              IdentityHashMap<ScheduleInstruction, Integer> cache, int fallback, int optionSize) {
        CompoundTag data = instruction.getData();
        int stored = getStoredIndexOrDefault(data, indexTag, cache, instruction, fallback);
        return clampIndex(stored, optionSize);
    }

    int getTagIntClampedOrDefault(ScheduleInstruction instruction, String indexTag, int fallback, int optionSize) {
        CompoundTag data = instruction.getData();
        int stored = data.contains(indexTag) ? data.getInt(indexTag) : fallback;
        return clampIndex(stored, optionSize);
    }

    int getKeyedIndexOrDefault(ScheduleInstruction instruction, String keyTag, List<String> keys,
                               int defaultIndex, int optionSize) {
        CompoundTag data = instruction.getData();
        if (data.contains(keyTag)) {
            int byKey = keys.indexOf(data.getString(keyTag));
            if (byKey >= 0)
                return clampIndex(byKey, optionSize);
        }
        return clampIndex(defaultIndex, optionSize);
    }
    int getKeyedIndexOrCached(ScheduleInstruction instruction, String keyTag, List<String> keys,
                              IdentityHashMap<ScheduleInstruction, Integer> cache, int optionSize) {
        CompoundTag data = instruction.getData();
        if (data.contains(keyTag)) {
            int byKey = keys.indexOf(data.getString(keyTag));
            if (byKey >= 0)
                return clampIndex(byKey, optionSize);
        }
        return clampIndex(cache.getOrDefault(instruction, 0), optionSize);
    }

    ItemStack getOptionalItem(ScheduleInstruction instruction, String tagKey, HolderLookup.Provider access) {
        CompoundTag data = instruction.getData();
        if (!data.contains(tagKey))
            return ItemStack.EMPTY;
        return ItemStack.parseOptional(access, data.getCompound(tagKey));
    }

    ItemStack getRunTapeItem(ScheduleInstruction instruction, String tagKey, HolderLookup.Provider access) {
        CompoundTag data = instruction.getData();
        if (data.contains(tagKey)) {
            ItemStack stored = ItemStack.parseOptional(access, data.getCompound(tagKey));
            if (!stored.isEmpty())
                return stored;
        }
        ItemStack fallback = instruction.getItem(0);
        return fallback == null ? ItemStack.EMPTY : fallback;
    }

    int getActionIndex(ScheduleInstruction instruction, String actionKeyTag,
                       IdentityHashMap<ScheduleInstruction, Integer> actionSelection,
                       List<String> actionKeys, int optionSize) {
        CompoundTag data = instruction.getData();
        if (data.contains(actionKeyTag))
            return getActionIndexByKey(data.getString(actionKeyTag), actionKeys, optionSize);
        return clampIndex(actionSelection.getOrDefault(instruction, 0), optionSize);
    }

    int getActionIndexByKey(String actionKey, List<String> actionKeys, int optionSize) {
        int index = actionKeys.indexOf(actionKey);
        if (index == -1)
            return 0;
        return clampIndex(index, optionSize);
    }

    String getKeyForIndex(int index, List<String> keys) {
        return keys.get(clampIndex(index, keys.size()));
    }

    boolean getBooleanOrDefault(ScheduleInstruction instruction, String tagKey,
                                IdentityHashMap<ScheduleInstruction, Boolean> cache) {
        CompoundTag data = instruction.getData();
        if (data.contains(tagKey))
            return data.getBoolean(tagKey);
        return cache.getOrDefault(instruction, false);
    }
}
