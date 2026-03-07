package com.cosmolego527.create_pp.entity.programmable_pal;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

final class PalActionRuntime {

    private PalActionRuntime() {
    }

    static void executeHasItem(ProgrammablePalEntity pal, CompoundTag data) {
        if (!data.contains(PalTagKeys.HAS_ITEM_MATCH_ITEM))
            return;

        ItemStack configured = ItemStack.parseOptional(pal.level().registryAccess(),
                data.getCompound(PalTagKeys.HAS_ITEM_MATCH_ITEM));
        if (configured.isEmpty())
            return;

        Container inventory = pal.getInventory();
        int inventorySlot = PalUseActionRuntime.findMatchingInventorySlot(pal.level(), inventory,
                pal.getToolSlotStart(), pal.getToolSlotEnd(), configured);
        if (inventorySlot < 0)
            return;

        ItemStack stackToUse = inventory.getItem(inventorySlot);
        if (stackToUse.isEmpty())
            return;

        if ("use".equals(data.getString(PalTagKeys.HAS_ITEM_ACTION_KEY))) {
            int targetIndex = data.getInt(PalTagKeys.HAS_ITEM_TARGET_INDEX);
            BlockPos targetPos = pal.getCheckTargetPosition(targetIndex);
            ItemStack updated = PalUseActionRuntime.useInventoryItemOnTarget(pal.level(), pal.getFakePlayerProfile(),
                    pal.getX(), pal.getY(), pal.getZ(), pal.getYRot(), pal.getXRot(), pal.getDirection(),
                    targetPos, targetIndex, stackToUse);
            inventory.setItem(inventorySlot, updated.copy());
            pal.setHeldTool(updated);
        }
    }

    static void executeCheckBlock(ProgrammablePalEntity pal, CompoundTag data) {
        int targetIndex = data.getInt(PalTagKeys.CHECK_BLOCK_TARGET_INDEX);
        BlockPos checkPos = pal.getCheckTargetPosition(targetIndex);
        BlockState state = pal.level().getBlockState(checkPos);

        if (!PalCheckBlockRuntime.matchesConfiguredCheckBlockItem(pal.level(), pal, data, checkPos, state,
                PalTagKeys.CHECK_BLOCK_MATCH_ITEM))
            return;

        pal.applyCheckBlockMatchAction(data.getString(PalTagKeys.CHECK_BLOCK_MATCH_ACTION_KEY), checkPos, state);
    }
}