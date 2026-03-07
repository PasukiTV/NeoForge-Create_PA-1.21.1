package com.cosmolego527.create_pp.entity.programmable_pal;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

final class PalChopRuntime {

    private PalChopRuntime() {
    }

    static boolean tickChopTask(ProgrammablePalEntity pal) {
        if (pal.getChopCooldown() > 0) {
            pal.setChopCooldown(pal.getChopCooldown() - 1);
            return true;
        }

        if (!pal.acquireNextChopTarget())
            return false;

        BlockPos target = pal.getCurrentChopTarget();
        if (target == null)
            return false;

        ItemStack tool = pal.getHeldTool();
        if (tool.isEmpty() || !(tool.getItem() instanceof AxeItem)) {
            tool = pal.firstAxeInInventory();
            if (tool.isEmpty()) {
                pal.clearChopTask();
                return false;
            }
            pal.setHeldTool(tool);
        }

        int toolSlot = pal.findToolSlot(tool);

        BlockState state = pal.level().getBlockState(target);
        if (state.isAir() || state.getDestroySpeed(pal.level(), target) < 0) {
            pal.finishCurrentChopTarget();
            return true;
        }

        float hardness = Math.max(0.1f, state.getDestroySpeed(pal.level(), target));
        float toolSpeed = Math.max(1f, tool.getDestroySpeed(state));
        float nextProgress = pal.getCurrentChopProgress() + toolSpeed / (hardness * 30f);
        pal.setCurrentChopProgress(nextProgress);

        int breakStage = Math.max(0, Math.min(9, (int) (nextProgress * 10f) - 1));
        pal.level().destroyBlockProgress(pal.getId(), target, breakStage);

        if (nextProgress >= 1f) {
            pal.breakBlockAndStoreDrops(target, state, tool);
            tool.hurtAndBreak(1, pal, EquipmentSlot.MAINHAND);
            if (toolSlot >= 0)
                pal.getInventory().setItem(toolSlot, tool.copy());

            if (tool.isEmpty()) {
                pal.setHeldTool(ItemStack.EMPTY);
                pal.clearChopTask();
                return false;
            }

            pal.setHeldTool(tool);
            pal.finishCurrentChopTarget();
            pal.setChopCooldown(4);
        }

        return true;
    }

    static boolean isAdjacentChopReach(BlockPos palPos, BlockPos target) {
        int dx = Math.abs(target.getX() - palPos.getX());
        int dy = Math.abs(target.getY() - palPos.getY());
        int dz = Math.abs(target.getZ() - palPos.getZ());
        return dx <= 1 && dy <= 1 && dz <= 1;
    }

    static boolean queueBlockingLeavesTowardsTarget(Level level, BlockPos palPos, BlockPos target,
                                                    Consumer<BlockPos> queueChopTarget) {
        int stepX = Integer.compare(target.getX(), palPos.getX());
        int stepZ = Integer.compare(target.getZ(), palPos.getZ());

        BlockPos cursor = palPos;
        boolean queuedAny = false;

        for (int i = 0; i < 3; i++) {
            cursor = cursor.offset(stepX, 0, stepZ);
            if (level.getBlockState(cursor).is(BlockTags.LEAVES)) {
                queueChopTarget.accept(cursor);
                queuedAny = true;
            }

            BlockPos above = cursor.above();
            if (level.getBlockState(above).is(BlockTags.LEAVES)) {
                queueChopTarget.accept(above);
                queuedAny = true;
            }
        }

        return queuedAny;
    }

    static void queueChopTarget(BlockPos pos, Set<BlockPos> queuedChopTargets, ArrayDeque<BlockPos> pendingChopTargets) {
        BlockPos immutablePos = pos.immutable();
        if (queuedChopTargets.add(immutablePos))
            pendingChopTargets.addLast(immutablePos);
    }

    static void removePendingLeaves(Level level, Set<BlockPos> pendingLeafRemoval,
                                    BiConsumer<BlockPos, BlockState> breakBlockAndStoreDrops) {
        for (BlockPos leafPos : pendingLeafRemoval) {
            BlockState leafState = level.getBlockState(leafPos);
            if (leafState.is(BlockTags.LEAVES))
                breakBlockAndStoreDrops.accept(leafPos, leafState);
        }
        pendingLeafRemoval.clear();
    }

    static void mineTreeOrBlock(Level level, BlockPos origin, BlockState originState, Runnable clearChopTask,
                                Set<BlockPos> pendingLeafRemoval, Consumer<BlockPos> queueChopTarget) {
        clearChopTask.run();

        if (originState.isAir())
            return;

        if (!originState.is(BlockTags.LOGS)) {
            queueChopTarget.accept(origin);
            return;
        }

        Set<BlockPos> visitedLogs = new java.util.HashSet<>();
        Set<BlockPos> visitedLeaves = new java.util.HashSet<>();
        ArrayDeque<BlockPos> logQueue = new ArrayDeque<>();
        ArrayDeque<BlockPos> leafQueue = new ArrayDeque<>();
        pendingLeafRemoval.clear();
        logQueue.add(origin);

        while (!logQueue.isEmpty()) {
            BlockPos current = logQueue.removeFirst();
            if (!visitedLogs.add(current))
                continue;

            BlockState currentState = level.getBlockState(current);
            if (!currentState.is(BlockTags.LOGS))
                continue;

            queueChopTarget.accept(current);

            for (BlockPos neighbor : BlockPos.betweenClosed(current.offset(-1, -1, -1), current.offset(1, 1, 1))) {
                BlockPos immutableNeighbor = neighbor.immutable();
                BlockState neighborState = level.getBlockState(immutableNeighbor);
                if (neighborState.is(BlockTags.LOGS) && !visitedLogs.contains(immutableNeighbor))
                    logQueue.addLast(immutableNeighbor);
                if (neighborState.is(BlockTags.LEAVES) && !visitedLeaves.contains(immutableNeighbor))
                    leafQueue.addLast(immutableNeighbor);
            }
        }

        while (!leafQueue.isEmpty()) {
            BlockPos currentLeaf = leafQueue.removeFirst();
            if (!visitedLeaves.add(currentLeaf))
                continue;

            BlockState currentLeafState = level.getBlockState(currentLeaf);
            if (!currentLeafState.is(BlockTags.LEAVES))
                continue;

            pendingLeafRemoval.add(currentLeaf.immutable());

            for (BlockPos neighbor : BlockPos.betweenClosed(currentLeaf.offset(-1, -1, -1), currentLeaf.offset(1, 1, 1))) {
                BlockPos immutableNeighbor = neighbor.immutable();
                if (visitedLeaves.contains(immutableNeighbor))
                    continue;
                if (level.getBlockState(immutableNeighbor).is(BlockTags.LEAVES))
                    leafQueue.addLast(immutableNeighbor);
            }
        }
    }
}