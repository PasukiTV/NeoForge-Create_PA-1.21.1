package com.cosmolego527.create_pp.entity.programmable_pal;

import com.simibubi.create.content.logistics.filter.FilterItemStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

final class PalInteractRuntime {

    private PalInteractRuntime() {
    }

    static void executeInteract(Level level, BlockPos palPos, Direction palDirection, Container palInventory,
                                int slotStart, int slotEnd, CompoundTag data,
                                String targetTag, String modeTag, String filterItemTag, String keepItemTag) {
        String target = data.getString(targetTag);
        if (!target.isEmpty() && !"storage".equals(target))
            return;

        if (!data.contains(filterItemTag))
            return;

        ItemStack filterItem = ItemStack.parseOptional(level.registryAccess(), data.getCompound(filterItemTag));
        if (filterItem.isEmpty())
            return;

        Container storage = getFrontStorageContainer(level, palPos, palDirection);
        if (storage == null)
            return;

        FilterItemStack transferFilter = FilterItemStack.of(filterItem.copy());
        String mode = data.getString(modeTag);
        ItemStack keepItem = ItemStack.EMPTY;
        if (data.contains(keepItemTag))
            keepItem = ItemStack.parseOptional(level.registryAccess(), data.getCompound(keepItemTag));

        if ("pull".equals(mode)) {
            pullFilteredItemsFromStorage(level, palInventory, slotStart, slotEnd, storage, transferFilter, keepItem);
            return;
        }

        pushFilteredItemsToStorage(level, palInventory, slotStart, slotEnd, storage, transferFilter, keepItem);
    }

    private static @Nullable Container getFrontStorageContainer(Level level, BlockPos palPos, Direction palDirection) {
        BlockPos storagePos = palPos.relative(palDirection);
        BlockState storageState = level.getBlockState(storagePos);

        if (storageState.getBlock() instanceof ChestBlock chestBlock) {
            Container chestContainer = ChestBlock.getContainer(chestBlock, storageState, level, storagePos, true);
            if (chestContainer != null)
                return chestContainer;
        }

        BlockEntity blockEntity = level.getBlockEntity(storagePos);
        if (blockEntity instanceof Container container)
            return container;
        return null;
    }

    private static void pullFilteredItemsFromStorage(Level level, Container palInventory, int slotStart, int slotEnd,
                                                     Container storage, FilterItemStack transferFilter, ItemStack keepItem) {
        FilterItemStack keepFilter = keepItem.isEmpty() ? null : FilterItemStack.of(keepItem.copy());
        int keepCurrentCount = keepFilter != null ? countPalItemsMatching(level, palInventory, slotStart, slotEnd, keepFilter) : 0;

        boolean changed = false;
        for (int slot = 0; slot < storage.getContainerSize(); slot++) {
            ItemStack source = storage.getItem(slot);
            if (source.isEmpty())
                continue;
            if (!transferFilter.test(level, source))
                continue;

            boolean isKeepFilteredItem = keepFilter != null && keepFilter.test(level, source);
            int movable = source.getCount();
            if (isKeepFilteredItem) {
                int keepGoalForMatchedItem = source.getMaxStackSize();
                int keepRemaining = Math.max(0, keepGoalForMatchedItem - keepCurrentCount);
                if (keepRemaining <= 0)
                    continue;
                movable = Math.min(movable, keepRemaining);
            }

            if (movable <= 0)
                continue;

            ItemStack toMove = source.copyWithCount(movable);
            ItemStack remainder = insertIntoPalStorage(palInventory, slotStart, slotEnd, toMove);
            int moved = movable - remainder.getCount();
            if (moved <= 0)
                continue;

            source.shrink(moved);
            storage.setItem(slot, source.isEmpty() ? ItemStack.EMPTY : source);
            changed = true;

            if (isKeepFilteredItem)
                keepCurrentCount += moved;
        }

        if (changed) {
            storage.setChanged();
            palInventory.setChanged();
        }
    }

    private static void pushFilteredItemsToStorage(Level level, Container palInventory, int slotStart, int slotEnd,
                                                   Container storage, FilterItemStack transferFilter, ItemStack keepItem) {
        FilterItemStack keepFilter = keepItem.isEmpty() ? null : FilterItemStack.of(keepItem.copy());
        int keepCurrentCount = keepFilter != null ? countPalItemsMatching(level, palInventory, slotStart, slotEnd, keepFilter) : 0;

        boolean changed = false;
        for (int slot = slotStart; slot < slotEnd; slot++) {
            ItemStack source = palInventory.getItem(slot);
            if (source.isEmpty())
                continue;
            if (!transferFilter.test(level, source))
                continue;

            boolean isKeepFilteredItem = keepFilter != null && keepFilter.test(level, source);
            int movable = source.getCount();
            if (isKeepFilteredItem) {
                int keepGoalForMatchedItem = source.getMaxStackSize();
                int excess = Math.max(0, keepCurrentCount - keepGoalForMatchedItem);
                movable = Math.min(movable, excess);
            }

            if (movable <= 0)
                continue;

            ItemStack toMove = source.copyWithCount(movable);
            ItemStack remainder = insertIntoContainer(storage, toMove);
            int moved = movable - remainder.getCount();
            if (moved <= 0)
                continue;

            source.shrink(moved);
            palInventory.setItem(slot, source.isEmpty() ? ItemStack.EMPTY : source);
            changed = true;

            if (isKeepFilteredItem)
                keepCurrentCount = Math.max(0, keepCurrentCount - moved);
        }

        if (changed) {
            storage.setChanged();
            palInventory.setChanged();
        }
    }

    private static ItemStack insertIntoPalStorage(Container palInventory, int slotStart, int slotEnd, ItemStack stack) {
        ItemStack remaining = stack.copy();

        for (int slot = slotStart; slot < slotEnd && !remaining.isEmpty(); slot++) {
            ItemStack existing = palInventory.getItem(slot);
            if (existing.isEmpty())
                continue;
            if (!ItemStack.isSameItemSameComponents(existing, remaining))
                continue;

            int maxSize = Math.min(existing.getMaxStackSize(), palInventory.getMaxStackSize());
            int free = maxSize - existing.getCount();
            if (free <= 0)
                continue;

            int moved = Math.min(free, remaining.getCount());
            existing.grow(moved);
            remaining.shrink(moved);
            palInventory.setItem(slot, existing);
        }

        for (int slot = slotStart; slot < slotEnd && !remaining.isEmpty(); slot++) {
            ItemStack existing = palInventory.getItem(slot);
            if (!existing.isEmpty())
                continue;

            int moved = Math.min(Math.min(remaining.getMaxStackSize(), palInventory.getMaxStackSize()), remaining.getCount());
            ItemStack placed = remaining.copyWithCount(moved);
            palInventory.setItem(slot, placed);
            remaining.shrink(moved);
        }

        return remaining;
    }

    private static ItemStack insertIntoContainer(Container storage, ItemStack stack) {
        ItemStack remaining = stack.copy();

        for (int slot = 0; slot < storage.getContainerSize() && !remaining.isEmpty(); slot++) {
            ItemStack existing = storage.getItem(slot);
            if (existing.isEmpty())
                continue;
            if (!ItemStack.isSameItemSameComponents(existing, remaining))
                continue;
            if (!storage.canPlaceItem(slot, remaining))
                continue;

            int maxSize = Math.min(existing.getMaxStackSize(), storage.getMaxStackSize());
            int free = maxSize - existing.getCount();
            if (free <= 0)
                continue;

            int moved = Math.min(free, remaining.getCount());
            existing.grow(moved);
            remaining.shrink(moved);
            storage.setItem(slot, existing);
        }

        for (int slot = 0; slot < storage.getContainerSize() && !remaining.isEmpty(); slot++) {
            ItemStack existing = storage.getItem(slot);
            if (!existing.isEmpty())
                continue;
            if (!storage.canPlaceItem(slot, remaining))
                continue;

            int moved = Math.min(Math.min(remaining.getMaxStackSize(), storage.getMaxStackSize()), remaining.getCount());
            ItemStack placed = remaining.copyWithCount(moved);
            storage.setItem(slot, placed);
            remaining.shrink(moved);
        }

        return remaining;
    }

    private static int countPalItemsMatching(Level level, Container palInventory, int slotStart, int slotEnd,
                                             @Nullable FilterItemStack filter) {
        if (filter == null)
            return 0;

        int total = 0;
        for (int slot = slotStart; slot < slotEnd; slot++) {
            ItemStack candidate = palInventory.getItem(slot);
            if (candidate.isEmpty())
                continue;
            if (filter.test(level, candidate))
                total += candidate.getCount();
        }
        return total;
    }
}
