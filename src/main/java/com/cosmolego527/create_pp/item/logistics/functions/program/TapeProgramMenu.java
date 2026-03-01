package com.cosmolego527.create_pp.item.logistics.functions.program;

import com.cosmolego527.create_pp.component.ModDataComponentTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class TapeProgramMenu extends AbstractContainerMenu {
    public static final int BTN_ADD_MOVE = 0;
    public static final int BTN_ADD_WAIT = 1;
    public static final int BTN_ADD_TURN_LEFT = 2;
    public static final int BTN_ADD_TURN_RIGHT = 3;
    public static final int BTN_CHECK_BLOCK_BELOW = 4;
    public static final int BTN_CHECK_BLOCK_FRONT = 5;
    public static final int BTN_CLEAR = 6;

    private final Inventory playerInventory;
    private final ItemStack contentHolder;
    private final List<String> instructions;

    public TapeProgramMenu(MenuType<?> type, int id, Inventory inv, RegistryFriendlyByteBuf extraData) {
        this(type, id, inv, ItemStack.STREAM_CODEC.decode(extraData));
    }

    public TapeProgramMenu(MenuType<?> type, int id, Inventory inv, ItemStack contentHolder) {
        super(type, id);
        this.playerInventory = inv;
        this.contentHolder = contentHolder;
        this.instructions = new ArrayList<>(readInstructions(contentHolder));
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        ItemStack selected = playerInventory.getSelected();
        return !selected.isEmpty() && selected.getItem() == contentHolder.getItem();
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        return switch (id) {
            case BTN_ADD_MOVE -> addInstruction("move_forward");
            case BTN_ADD_WAIT -> addInstruction("wait_20_ticks");
            case BTN_ADD_TURN_LEFT -> addInstruction("turn_left");
            case BTN_ADD_TURN_RIGHT -> addInstruction("turn_right");
            case BTN_CHECK_BLOCK_BELOW -> addInstruction("check_block_below");
            case BTN_CHECK_BLOCK_FRONT -> addInstruction("check_block_front");
            case BTN_CLEAR -> clearInstructions();
            default -> false;
        };
    }

    private boolean addInstruction(String instruction) {
        if (instructions.size() >= 64) {
            return false;
        }
        instructions.add(instruction);
        writeToSelectedStack();
        return true;
    }

    private boolean clearInstructions() {
        instructions.clear();
        writeToSelectedStack();
        return true;
    }

    private void writeToSelectedStack() {
        ItemStack selected = playerInventory.getSelected();
        CompoundTag root = selected.getOrDefault(ModDataComponentTypes.VOID_FUNCTION_DATA, new CompoundTag()).copy();
        ListTag list = new ListTag();
        for (String instruction : instructions) {
            list.add(StringTag.valueOf(instruction));
        }
        root.put("Instructions", list);
        selected.set(ModDataComponentTypes.VOID_FUNCTION_DATA, root);
    }

    private static List<String> readInstructions(ItemStack stack) {
        CompoundTag root = stack.get(ModDataComponentTypes.VOID_FUNCTION_DATA);
        if (root == null || !root.contains("Instructions", ListTag.TAG_LIST)) {
            return List.of();
        }

        ListTag list = root.getList("Instructions", StringTag.TAG_STRING);
        List<String> result = new ArrayList<>(list.size());
        for (int i = 0; i < list.size(); i++) {
            result.add(list.getString(i));
        }
        return result;
    }

    public ItemStack getContentHolder() {
        return contentHolder;
    }

    public List<String> getInstructions() {
        return List.copyOf(instructions);
    }
}