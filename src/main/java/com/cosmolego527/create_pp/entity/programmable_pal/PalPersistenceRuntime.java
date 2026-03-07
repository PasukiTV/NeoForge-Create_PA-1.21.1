package com.cosmolego527.create_pp.entity.programmable_pal;

import net.minecraft.core.BlockPos;

import java.util.ArrayDeque;
import java.util.Set;

final class PalPersistenceRuntime {

    private PalPersistenceRuntime() {
    }

    static long[] toLongArray(Iterable<BlockPos> positions, int size) {
        long[] values = new long[size];
        int index = 0;
        for (BlockPos pos : positions)
            values[index++] = pos.asLong();
        return values;
    }

    static void loadQueuedPositions(long[] packedPositions, ArrayDeque<BlockPos> outQueue) {
        outQueue.clear();
        for (long packed : packedPositions)
            outQueue.addLast(BlockPos.of(packed).immutable());
    }

    static void loadUniquePositions(long[] packedPositions, Set<BlockPos> outSet) {
        outSet.clear();
        for (long packed : packedPositions)
            outSet.add(BlockPos.of(packed).immutable());
    }
}