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
    public static final int BTN_CLEAR = 4;

    public static final int BTN_SELECT_PREV = 10;
    public static final int BTN_SELECT_NEXT = 11;
    public static final int BTN_WAIT_AMOUNT_DEC = 12;
    public static final int BTN_WAIT_AMOUNT_INC = 13;
    public static final int BTN_WAIT_UNIT_PREV = 14;
    public static final int BTN_WAIT_UNIT_NEXT = 15;
    public static final int BTN_CONFIRM_SELECTION = 16;

    private static final String[] ACTIONS = {"move_forward", "turn_left", "turn_right", "wait"};

    private final Inventory playerInventory;
    private final ItemStack contentHolder;
    private final List<String> instructions;

    private int selectedAction = 0;
    private int waitAmount = 5;
    private WaitUnit waitUnit = WaitUnit.SECONDS;

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
            case BTN_CLEAR -> clearInstructions();

            case BTN_SELECT_PREV -> select(-1);
            case BTN_SELECT_NEXT -> select(1);
            case BTN_WAIT_AMOUNT_DEC -> addWaitAmount(-1);
            case BTN_WAIT_AMOUNT_INC -> addWaitAmount(1);
            case BTN_WAIT_UNIT_PREV -> rotateWaitUnit(-1);
            case BTN_WAIT_UNIT_NEXT -> rotateWaitUnit(1);
            case BTN_CONFIRM_SELECTION -> confirmSelectedAction();
            default -> false;
        };
    }

    private boolean select(int delta) {
        int length = ACTIONS.length;
        selectedAction = (selectedAction + delta) % length;
        if (selectedAction < 0)
            selectedAction += length;
        return true;
    }

    private boolean addWaitAmount(int delta) {
        waitAmount = Math.max(1, Math.min(999, waitAmount + delta));
        return true;
    }

    private boolean rotateWaitUnit(int delta) {
        WaitUnit[] units = WaitUnit.values();
        int index = (waitUnit.ordinal() + delta) % units.length;
        if (index < 0)
            index += units.length;
        waitUnit = units[index];
        return true;
    }

    private boolean confirmSelectedAction() {
        String action = ACTIONS[selectedAction];
        if (!"wait".equals(action))
            return addInstruction(action);

        return addInstruction("wait_" + waitAmount + waitUnit.suffix);
    }

    private boolean addInstruction(String instruction) {
        if (instructions.size() >= 64)
            return false;
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
        if (root == null || !root.contains("Instructions", ListTag.TAG_LIST))
            return List.of();

        ListTag list = root.getList("Instructions", StringTag.TAG_STRING);
        List<String> result = new ArrayList<>(list.size());
        for (int i = 0; i < list.size(); i++)
            result.add(list.getString(i));
        return result;
    }

    public List<String> getInstructions() {
        return List.copyOf(instructions);
    }

    public enum WaitUnit {
        SECONDS("s"),
        MINUTES("m"),
        HOURS("h");

        public final String suffix;

        WaitUnit(String suffix) {
            this.suffix = suffix;
        }
    }
}