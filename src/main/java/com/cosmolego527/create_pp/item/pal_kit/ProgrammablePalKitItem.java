package com.cosmolego527.create_pp.item.pal_kit;

import com.cosmolego527.create_pp.CreatePP;
import com.cosmolego527.create_pp.entity.programmable_pal.ProgrammablePalEntity;
import com.cosmolego527.create_pp.entity.programmable_pal.ProgrammablePalVariant;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class ProgrammablePalKitItem extends Item {

    private final ProgrammablePalVariant variant;

    public ProgrammablePalKitItem(Properties properties, ProgrammablePalVariant variant) {
        super(properties);
        this.variant = variant;
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
    public String getDescriptionId() {
        return "item." + CreatePP.MOD_ID + ".programmable_pal_kit_" + variant.name().toLowerCase();
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();

        BlockPos block = context.getClickedPos();
        Direction direction = context.getClickedFace();
        BlockPos blockPos = block.relative(direction);
        Vec3 pos = blockPos.getBottomCenter();

        if (!level.getBlockState(blockPos).isAir()) {
            if (player != null)
                player.displayClientMessage(Component.literal("Programmable Pal requires more space"), true);
            return InteractionResult.FAIL;
        }

        if (level.isClientSide)
            return InteractionResult.SUCCESS;

        ProgrammablePalEntity entity = new ProgrammablePalEntity(level, pos);
        ItemStack itemInHand = context.getItemInHand();
        ItemStack item = itemInHand.copy();
        item.setCount(1);
        entity.setItem(item);
        entity.setVariant(variant);
        level.addFreshEntity(entity);
        itemInHand.shrink(1);
        return InteractionResult.CONSUME;
    }

    public static boolean isPal(ItemStack stack) {
        return stack.getItem() instanceof ProgrammablePalKitItem;
    }
}