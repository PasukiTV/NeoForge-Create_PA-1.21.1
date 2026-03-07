package com.cosmolego527.create_pp.entity.programmable_pal;

import com.mojang.authlib.GameProfile;
import com.simibubi.create.content.logistics.filter.FilterItemStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

final class PalUseActionRuntime {

    private PalUseActionRuntime() {
    }

    static int findMatchingInventorySlot(Level level, net.minecraft.world.Container inventory,
                                         int slotStart, int slotEnd, ItemStack configured) {
        FilterItemStack configuredFilter = FilterItemStack.of(configured.copy());
        for (int slot = slotStart; slot < slotEnd; slot++) {
            ItemStack candidate = inventory.getItem(slot);
            if (candidate.isEmpty())
                continue;
            if (configuredFilter.test(level, candidate))
                return slot;
        }
        return -1;
    }

    static ItemStack useInventoryItemOnTarget(Level level, GameProfile fakeProfile,
                                              double x, double y, double z, float yRot, float xRot,
                                              Direction palDirection, BlockPos targetPos, int targetIndex,
                                              ItemStack stackToUse) {
        if (!(level instanceof ServerLevel serverLevel))
            return stackToUse;

        Direction face = switch (targetIndex) {
            case 1 -> Direction.DOWN;
            case 2 -> palDirection.getOpposite();
            default -> Direction.UP;
        };

        FakePlayer fakePlayer = FakePlayerFactory.get(serverLevel, fakeProfile);
        fakePlayer.moveTo(x, y, z, yRot, xRot);

        ItemStack handStack = stackToUse.copy();
        fakePlayer.setItemInHand(InteractionHand.MAIN_HAND, handStack);

        InteractionResult result = InteractionResult.PASS;
        BlockPos[] targetsToTry = face == Direction.UP
                ? new BlockPos[]{targetPos, targetPos.above()}
                : new BlockPos[]{targetPos};

        for (BlockPos candidateTargetPos : targetsToTry) {
            Vec3 hitCenter = getInteractionHitPosition(level, candidateTargetPos, face);
            BlockHitResult hitResult = new BlockHitResult(hitCenter, face, candidateTargetPos, false);
            UseOnContext context = new UseOnContext(fakePlayer, InteractionHand.MAIN_HAND, hitResult);

            result = fakePlayer.gameMode.useItemOn(fakePlayer, serverLevel, handStack,
                    InteractionHand.MAIN_HAND, hitResult);
            if (!result.consumesAction())
                result = handStack.useOn(context);
            if (result.consumesAction())
                break;
        }

        if (!result.consumesAction())
            handStack.use(serverLevel, fakePlayer, InteractionHand.MAIN_HAND);

        return fakePlayer.getItemInHand(InteractionHand.MAIN_HAND).copy();
    }

    private static Vec3 getInteractionHitPosition(Level level, BlockPos targetPos, Direction face) {
        VoxelShape shape = level.getBlockState(targetPos).getShape(level, targetPos);
        if (shape.isEmpty()) {
            return Vec3.atCenterOf(targetPos)
                    .add(face.getStepX() * 0.25D, face.getStepY() * 0.25D, face.getStepZ() * 0.25D);
        }

        double epsilon = 0.01D;
        double x = targetPos.getX() + 0.5D;
        double y = targetPos.getY() + 0.5D;
        double z = targetPos.getZ() + 0.5D;

        switch (face) {
            case UP -> y = targetPos.getY() + Math.min(1D - epsilon, shape.max(Direction.Axis.Y) + epsilon);
            case DOWN -> y = targetPos.getY() + Math.min(1D - epsilon, shape.min(Direction.Axis.Y) + epsilon);
            case NORTH -> z = targetPos.getZ() + Math.min(1D - epsilon, shape.min(Direction.Axis.Z) + epsilon);
            case SOUTH -> z = targetPos.getZ() + Math.max(epsilon, shape.max(Direction.Axis.Z) - epsilon);
            case WEST -> x = targetPos.getX() + Math.min(1D - epsilon, shape.min(Direction.Axis.X) + epsilon);
            case EAST -> x = targetPos.getX() + Math.max(epsilon, shape.max(Direction.Axis.X) - epsilon);
        }

        return new Vec3(x, y, z);
    }
}
