package com.cosmolego527.create_pp.entity.programmable_pal;

import net.minecraft.core.BlockPos;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

final class PalBlockBreakRuntime {

    private PalBlockBreakRuntime() {
    }

    private static void collectBlockDropsToInventory(Level level, Entity breaker, SimpleContainer inventory,
                                             BlockPos blockPos, BlockState state, ItemStack tool,
                                             double dropX, double dropY, double dropZ) {
        if (!(level instanceof ServerLevel serverLevel))
            return;

        LootParams.Builder lootBuilder = new LootParams.Builder(serverLevel)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(blockPos))
                .withParameter(LootContextParams.TOOL, tool)
                .withOptionalParameter(LootContextParams.THIS_ENTITY, breaker);

        for (ItemStack drop : state.getDrops(lootBuilder)) {
            ItemStack remainder = inventory.addItem(drop.copy());
            if (!remainder.isEmpty())
                level.addFreshEntity(new ItemEntity(level, dropX, dropY, dropZ, remainder));
        }
    }

    static void breakBlockAndStoreDrops(Level level, Entity breaker, SimpleContainer inventory,
                                        BlockPos blockPos, BlockState state, ItemStack tool,
                                        double dropX, double dropY, double dropZ) {
        if (state.isAir())
            return;

        collectBlockDropsToInventory(level, breaker, inventory, blockPos, state, tool, dropX, dropY, dropZ);
        level.destroyBlock(blockPos, false, breaker);
    }
}