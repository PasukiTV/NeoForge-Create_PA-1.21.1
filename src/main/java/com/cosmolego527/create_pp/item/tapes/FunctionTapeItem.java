package com.cosmolego527.create_pp.item.tapes;

import com.cosmolego527.create_pp.ModMenuTypes;
import com.cosmolego527.create_pp.component.ModDataComponentTypes;
import com.cosmolego527.create_pp.item.ModItems;
import com.cosmolego527.create_pp.item.logistics.functions.FunctionInstruction;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.foundation.item.ItemHelper;
import com.simibubi.create.foundation.recipe.ItemCopyingRecipe.SupportsItemCopying;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.ItemStackHandler;

import javax.annotation.Nonnull;

public class FunctionTapeItem extends Item implements MenuProvider, SupportsItemCopying {

    public FunctionInstruction[] functionInstructions;

    private FunctionType type;

    public enum FunctionType {
        TAPE, BOOL, INT, STRING, VOID, FLOAT;
    }

    public static FunctionTapeItem programmableTapeItem(Properties properties) {return new FunctionTapeItem(FunctionType.TAPE, properties);}
    public static FunctionTapeItem boolFuncItem(Properties properties) {return new FunctionTapeItem(FunctionType.BOOL, properties);}
    public static FunctionTapeItem intFuncItem(Properties properties) {return new FunctionTapeItem(FunctionType.INT, properties);}
    public static FunctionTapeItem stringFuncItem(Properties properties) {return new FunctionTapeItem(FunctionType.STRING, properties);}
    public static FunctionTapeItem voidFuncItem(Properties properties) {return new FunctionTapeItem(FunctionType.VOID, properties);}
    public static FunctionTapeItem floatFuncItem(Properties properties) {return new FunctionTapeItem(FunctionType.FLOAT, properties);}

    public FunctionTapeItem(FunctionType type, Properties properties) {
        super(properties);
        this.type = type;
    }
    @Nonnull
    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getPlayer() == null)
            return InteractionResult.PASS;
        return use(context.getLevel(), context.getPlayer(), context.getHand()).getResult();
    }
    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack heldItem = player.getItemInHand(hand);
        if (type != FunctionType.TAPE)
            return InteractionResultHolder.pass(heldItem);

        if (!player.isShiftKeyDown() && hand == InteractionHand.MAIN_HAND) {
            if (!world.isClientSide && player instanceof ServerPlayer)
                player.openMenu(this, buf -> {
                    ItemStack.STREAM_CODEC.encode(buf, heldItem);
                });
            return InteractionResultHolder.success(heldItem);
        }
        return InteractionResultHolder.pass(heldItem);
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        //ItemStack heldItem = player.getMainHandItem();
        //if (type == FunctionType.BOOL)
        //    return AttributeFilterMenu.create(id, inv, heldItem);
        //if (type == FunctionType.INT)
        //    return AttributeFilterMenu.create(id, inv, heldItem);
        //if (type == FunctionType.STRING)
        //    return PackageFilterMenu.create(id, inv, heldItem);
        //if (type == FunctionType.VOID)
        //    return new VoidFunctionMenu(ModMenuTypes.VOID_FUNCTION_MENU.get(), id, inv, heldItem);
        if (type != FunctionType.TAPE)
            return null;
        ItemStack heldItem = player.getMainHandItem();
        return new TapeProgramMenu(ModMenuTypes.TAPE_PROGRAM_MENU.get(), id, inv, heldItem);
       // return null;
    }

    @Override
    public Component getDisplayName() {
        return getDescription();
    }

    public static ItemStackHandler getFilterItems(ItemStack stack) {
        ItemStackHandler newInv = new ItemStackHandler(18);
        if (ModItems.PROGRAMMABLE_TAPE.get() != stack.getItem())
            throw new IllegalArgumentException("Cannot get filter items from non-filter: " + stack);
        if (!stack.has(AllDataComponents.FILTER_ITEMS))
            return newInv;

        //noinspection DataFlowIssue - It's fine:tm: we check if it has the component before doing this
        ItemHelper.fillItemStackHandler(stack.get(AllDataComponents.FILTER_ITEMS), newInv);

        return newInv;
    }



    @Override
    public DataComponentType<?> getComponentType() {
        return switch (type) {
            case TAPE -> null;
            case BOOL -> AllDataComponents.FILTER_ITEMS;
            case INT -> AllDataComponents.ATTRIBUTE_FILTER_MATCHED_ATTRIBUTES;
            case STRING, FLOAT -> AllDataComponents.PACKAGE_ADDRESS;
            case VOID -> ModDataComponentTypes.VOID_FUNCTION_DATA;

        };
    }


}
