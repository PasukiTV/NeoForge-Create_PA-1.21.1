package com.cosmolego527.create_pp.entity.programmable_pal;

import com.simibubi.create.content.logistics.filter.FilterItemStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
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
                                String targetTag, String modeTag, String filterItemTag, String maxStacksIndexTag) {
        String target = data.getString(targetTag);
        if (!target.isEmpty() && !"storage".equals(target))
            return;

        Container storage = getFrontStorageContainer(level, palPos, palDirection);
        if (storage == null)
            return;

        @Nullable FilterItemStack transferFilter = null;
        if (data.contains(filterItemTag)) {
            ItemStack filterItem = ItemStack.parseOptional(level.registryAccess(), data.getCompound(filterItemTag));
            if (!filterItem.isEmpty())
                transferFilter = FilterItemStack.of(filterItem.copy());
        }

        int maxStacksIndex = 0;
        if (data.contains(maxStacksIndexTag))
            maxStacksIndex = Math.max(0, data.getInt(maxStacksIndexTag));

        String mode = data.getString(modeTag);
        if ("pull".equals(mode)) {
            int maxStacksPerType = maxStacksIndex == 0
                    ? Integer.MAX_VALUE
                    : Mth.clamp(maxStacksIndex, 1, Math.max(1, slotEnd - slotStart));
            pullItemsFromStorage(level, palInventory, slotStart, slotEnd, storage, transferFilter, maxStacksPerType);
            return;
        }

        int keepStacksPerType = maxStacksIndex == 0
                ? 0
                : Mth.clamp(maxStacksIndex, 1, Math.max(1, slotEnd - slotStart));
        pushItemsToStorage(level, palInventory, slotStart, slotEnd, storage, transferFilter, keepStacksPerType);
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

    private static void pullItemsFromStorage(Level level, Container palInventory, int slotStart, int slotEnd,
                                             Container storage, @Nullable FilterItemStack transferFilter,
                                             int maxStacksPerType) {
        boolean changed = false;
        for (int slot = 0; slot < storage.getContainerSize(); slot++) {
            ItemStack source = storage.getItem(slot);
            if (source.isEmpty())
                continue;
            if (!matchesFilter(level, source, transferFilter))
                continue;

            int currentTypeStacks = countPalStacksOfType(palInventory, slotStart, slotEnd, source);
            if (currentTypeStacks > maxStacksPerType)
                continue;

            ItemStack toMove = source.copy();
            ItemStack remainder = insertIntoPalStorageWithTypeStackLimit(
                    palInventory, slotStart, slotEnd, toMove, source, maxStacksPerType);
            int moved = source.getCount() - remainder.getCount();
            if (moved <= 0)
                continue;

            source.shrink(moved);
            storage.setItem(slot, source.isEmpty() ? ItemStack.EMPTY : source);
            changed = true;
        }

        if (changed) {
            storage.setChanged();
            palInventory.setChanged();
        }
    }

    private static void pushItemsToStorage(Level level, Container palInventory, int slotStart, int slotEnd,
                                           Container storage, @Nullable FilterItemStack transferFilter,
                                           int keepStacksPerType) {
        boolean changed = false;
        for (int slot = slotStart; slot < slotEnd; slot++) {
            ItemStack source = palInventory.getItem(slot);
            if (source.isEmpty())
                continue;
            if (!matchesFilter(level, source, transferFilter))
                continue;

            int currentTypeStacks = countPalStacksOfType(palInventory, slotStart, slotEnd, source);
            if (currentTypeStacks <= keepStacksPerType)
                continue;

            int sourceCountBefore = source.getCount();
            ItemStack remainder = insertIntoContainer(storage, source.copy());
            int moved = sourceCountBefore - remainder.getCount();
            if (moved <= 0)
                continue;

            palInventory.setItem(slot, remainder.isEmpty() ? ItemStack.EMPTY : remainder);
            changed = true;
        }

        if (changed) {
            storage.setChanged();
            palInventory.setChanged();
        }
    }

    private static ItemStack insertIntoPalStorageWithTypeStackLimit(Container palInventory, int slotStart,
                                                                     int slotEnd, ItemStack stack,
                                                                     ItemStack typePrototype,
                                                                     int maxStacksPerType) {
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

        int currentTypeStacks = countPalStacksOfType(palInventory, slotStart, slotEnd, typePrototype);

        for (int slot = slotStart; slot < slotEnd && !remaining.isEmpty(); slot++) {
            ItemStack existing = palInventory.getItem(slot);
            if (!existing.isEmpty())
                continue;
            if (currentTypeStacks >= maxStacksPerType)
                break;

            int moved = Math.min(Math.min(remaining.getMaxStackSize(), palInventory.getMaxStackSize()), remaining.getCount());
            ItemStack placed = remaining.copyWithCount(moved);
            palInventory.setItem(slot, placed);
            remaining.shrink(moved);
            currentTypeStacks++;
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

    private static int countPalStacksOfType(Container palInventory, int slotStart, int slotEnd, ItemStack prototype) {
        int total = 0;
        for (int slot = slotStart; slot < slotEnd; slot++) {
            ItemStack candidate = palInventory.getItem(slot);
            if (candidate.isEmpty())
                continue;
            if (ItemStack.isSameItemSameComponents(candidate, prototype))
                total++;
        }
        return total;
    }

    private static boolean matchesFilter(Level level, ItemStack stack, @Nullable FilterItemStack filter) {
        return filter == null || filter.test(level, stack);
    }
}
