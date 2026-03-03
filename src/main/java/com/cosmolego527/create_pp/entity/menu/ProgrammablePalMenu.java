package com.cosmolego527.create_pp.entity.menu;

import com.cosmolego527.create_pp.entity.custom.ProgrammablePalEntity;
import com.cosmolego527.create_pp.util.ModTags;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class ProgrammablePalMenu extends AbstractContainerMenu {

    public static final int TAPE_SLOT_INDEX = 0;
    public static final int PAL_STORAGE_START = 1;
    public static final int PAL_STORAGE_SIZE = 27;
    public static final int PAL_SLOT_COUNT = PAL_STORAGE_START + PAL_STORAGE_SIZE;

    private static final int SLOTS_PER_ROW = 9;

    private final ProgrammablePalEntity pal;
    private final Container palInventory;

    public ProgrammablePalMenu(MenuType<?> type, int containerId, Inventory playerInventory,
                               RegistryFriendlyByteBuf extraData) {
        this(type, containerId, playerInventory,
                (ProgrammablePalEntity) playerInventory.player.level().getEntity(extraData.readInt()));
    }

    public ProgrammablePalMenu(MenuType<?> type, int containerId, Inventory playerInventory, ProgrammablePalEntity pal) {
        super(type, containerId);
        this.pal = pal;
        this.palInventory = pal.getInventory();
        this.palInventory.startOpen(playerInventory.player);

        // Extra tape slot to the left of the chest GUI body.
        addSlot(new Slot(palInventory, TAPE_SLOT_INDEX, -18, 20) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return ModTags.AllItemTags.PROGRAMMABLE_INSTRUCTION_ITEM.matches(stack);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });

        // Single chest-like storage: 3 rows x 9 columns.
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < SLOTS_PER_ROW; col++) {
                int palSlot = PAL_STORAGE_START + col + row * SLOTS_PER_ROW;
                addSlot(new Slot(palInventory, palSlot, 8 + col * 18, 18 + row * 18));
            }
        }

        for (int row = 0; row < 3; row++)
            for (int col = 0; col < SLOTS_PER_ROW; col++)
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));

        for (int hotbar = 0; hotbar < SLOTS_PER_ROW; hotbar++)
            addSlot(new Slot(playerInventory, hotbar, 8 + hotbar * 18, 142));
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem())
            return ItemStack.EMPTY;

        ItemStack stackInSlot = slot.getItem();
        ItemStack stackCopy = stackInSlot.copy();

        if (index < PAL_SLOT_COUNT) {
            if (!moveItemStackTo(stackInSlot, PAL_SLOT_COUNT, slots.size(), true))
                return ItemStack.EMPTY;
        } else {
            if (ModTags.AllItemTags.PROGRAMMABLE_INSTRUCTION_ITEM.matches(stackInSlot)) {
                if (!moveItemStackTo(stackInSlot, TAPE_SLOT_INDEX, TAPE_SLOT_INDEX + 1, false))
                    return ItemStack.EMPTY;
            } else if (!moveItemStackTo(stackInSlot, PAL_STORAGE_START, PAL_SLOT_COUNT, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (stackInSlot.isEmpty())
            slot.set(ItemStack.EMPTY);
        else
            slot.setChanged();

        return stackCopy;
    }

    @Override
    public boolean stillValid(Player player) {
        return pal != null && pal.isAlive() && pal.distanceToSqr(player) <= 64.0D;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        palInventory.stopOpen(player);
    }
}