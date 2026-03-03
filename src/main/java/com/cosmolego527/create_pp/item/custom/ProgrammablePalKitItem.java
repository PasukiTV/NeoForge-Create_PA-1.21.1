package com.cosmolego527.create_pp.item.custom;

import com.cosmolego527.create_pp.CreatePP;
import com.cosmolego527.create_pp.entity.ProgrammablePalVariant;
import com.cosmolego527.create_pp.entity.custom.ProgrammablePalEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class ProgrammablePalKitItem extends Item {

    public ProgrammablePalVariant Variant;

    public ProgrammablePalKitItem(Properties properties, ProgrammablePalVariant variant) {
        super(properties);
        this.Variant = variant;
    }

    public static ProgrammablePalKitItem PPalWhite(Properties p){return new ProgrammablePalKitItem(p, ProgrammablePalVariant.WHITE);}
    public static ProgrammablePalKitItem PPalLightGray(Properties p){return new ProgrammablePalKitItem(p, ProgrammablePalVariant.LIGHTGRAY);}
    public static ProgrammablePalKitItem PPalGray(Properties p){return new ProgrammablePalKitItem(p, ProgrammablePalVariant.GRAY);}
    public static ProgrammablePalKitItem PPalBlack(Properties p){return new ProgrammablePalKitItem(p, ProgrammablePalVariant.BLACK);}
    public static ProgrammablePalKitItem PPalRed(Properties p){return new ProgrammablePalKitItem(p, ProgrammablePalVariant.RED);}
    public static ProgrammablePalKitItem PPalOrange(Properties p){return new ProgrammablePalKitItem(p, ProgrammablePalVariant.ORANGE);}
    public static ProgrammablePalKitItem PPalYellow(Properties p){return new ProgrammablePalKitItem(p, ProgrammablePalVariant.YELLOW);}
    public static ProgrammablePalKitItem PPalLime(Properties p){return new ProgrammablePalKitItem(p, ProgrammablePalVariant.LIME);}
    public static ProgrammablePalKitItem PPalGreen(Properties p){return new ProgrammablePalKitItem(p, ProgrammablePalVariant.GREEN);}
    public static ProgrammablePalKitItem PPalLightBlue(Properties p){return new ProgrammablePalKitItem(p, ProgrammablePalVariant.LIGHTBLUE);}
    public static ProgrammablePalKitItem PPalCyan(Properties p){return new ProgrammablePalKitItem(p, ProgrammablePalVariant.CYAN);}
    public static ProgrammablePalKitItem PPalBlue(Properties p){return new ProgrammablePalKitItem(p, ProgrammablePalVariant.BLUE);}
    public static ProgrammablePalKitItem PPalPurple(Properties p){return new ProgrammablePalKitItem(p, ProgrammablePalVariant.PURPLE);}
    public static ProgrammablePalKitItem PPalMagenta(Properties p){return new ProgrammablePalKitItem(p, ProgrammablePalVariant.MAGENTA);}
    public static ProgrammablePalKitItem PPalPink(Properties p){return new ProgrammablePalKitItem(p, ProgrammablePalVariant.PINK);}
    public static ProgrammablePalKitItem PPalBrown(Properties p){return new ProgrammablePalKitItem(p, ProgrammablePalVariant.BROWN);}
    public static ProgrammablePalKitItem PPalDefault(Properties p){return new ProgrammablePalKitItem(p, ProgrammablePalVariant.DEFAULT);}

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        player.startUsingItem(hand);
        return InteractionResultHolder.success(itemStack);
    }

    @Override
    public String getDescriptionId() {
        return "item." + CreatePP.MOD_ID + ".programmable_pal_kit_" + Variant.name().toLowerCase();
    }

    @Override
    public boolean hasCustomEntity(ItemStack stack) {
        return true;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();

        float h = 0.875f, r = 0.625f/2f;

        BlockPos block = context.getClickedPos();
        Vec3 point = context.getClickLocation();
        Direction direction = context.getClickedFace();
        if(direction.getAxis().isHorizontal()) point = point.add(Vec3.atLowerCornerOf(direction.getNormal()).scale(r));
        var blockPos = block.relative(direction);
        var pos = blockPos.getBottomCenter();
        if (!level.getBlockState(blockPos).isAir()) {
            context.getPlayer().displayClientMessage(Component.literal("Programmable Pal requires more space"), true);
            return super.useOn(context);
        }

        ProgrammablePalEntity entity = new ProgrammablePalEntity(level, pos);
        ItemStack itemInHand = context.getItemInHand();
        var item = itemInHand.copy();
        item.setCount(1);
        entity.setItem(item);
        entity.setVariant(Variant);
        level.addFreshEntity(entity);
        itemInHand.shrink(1);
        return InteractionResult.SUCCESS;
    }

    public static boolean isPal(ItemStack stack) {
        return stack.getItem() instanceof ProgrammablePalKitItem;
    }
}
