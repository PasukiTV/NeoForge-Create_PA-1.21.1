package com.cosmolego527.create_pp.entity.programmable_pal;

import net.minecraft.world.Container;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;

final class PalInventoryRuntime {

    private PalInventoryRuntime() {
    }

    static int findToolSlot(Container inventory, int slotStart, int slotEnd, ItemStack target) {
        for (int slot = slotStart; slot < slotEnd; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!stack.isEmpty() && ItemStack.isSameItemSameComponents(stack, target))
                return slot;
        }
        return -1;
    }

    static ItemStack firstAxeInInventory(Container inventory, int slotStart, int slotEnd) {
        for (int slot = slotStart; slot < slotEnd; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.getItem() instanceof AxeItem)
                return stack;
        }
        return ItemStack.EMPTY;
    }

    static ItemStack bestCombatToolInInventory(Container inventory, int slotStart, int slotEnd) {
        for (int slot = slotStart; slot < slotEnd; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.getItem() instanceof SwordItem)
                return stack;
        }

        return firstAxeInInventory(inventory, slotStart, slotEnd);
    }
}