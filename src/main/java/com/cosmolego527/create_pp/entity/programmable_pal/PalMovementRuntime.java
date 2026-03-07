package com.cosmolego527.create_pp.entity.programmable_pal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.state.BlockState;

final class PalMovementRuntime {

    private PalMovementRuntime() {
    }

    static boolean canMoveThrough(ProgrammablePalEntity pal, BlockPos pos) {
        BlockState state = pal.level().getBlockState(pos);
        if (state.is(BlockTags.LEAVES))
            return true;
        return state.getCollisionShape(pal.level(), pos).isEmpty();
    }

    static BlockPos getFeetReferencePos(ProgrammablePalEntity pal) {
        BlockPos base = pal.blockPosition();
        return canMoveThrough(pal, base) ? base : base.above();
    }

    static BlockPos getCheckTargetPosition(ProgrammablePalEntity pal, int targetIndex) {
        BlockPos feetPos = getFeetReferencePos(pal);
        return switch (targetIndex) {
            case 1 -> feetPos.above();
            case 2 -> feetPos.relative(pal.getDirection());
            default -> feetPos.below();
        };
    }

    static Direction resolveRotateDirection(Direction current, int option) {
        return switch (option) {
            case 0 -> current.getClockWise();
            case 1 -> current.getCounterClockWise();
            case 2 -> Direction.NORTH;
            case 3 -> Direction.EAST;
            case 4 -> Direction.SOUTH;
            case 5 -> Direction.WEST;
            default -> current;
        };
    }

    static boolean executeQueuedMoveStep(ProgrammablePalEntity pal) {
        pal.getNavigation().stop();

        Direction direction = pal.getDirection();

        pal.setYRot(direction.toYRot());
        pal.setYHeadRot(direction.toYRot());
        pal.setYBodyRot(direction.toYRot());

        BlockPos currentFeet = getFeetReferencePos(pal);
        BlockPos nextFeet = currentFeet.relative(direction);

        if (canMoveThrough(pal, nextFeet)) {
            pal.setPos(nextFeet.getX() + 0.5D, nextFeet.getY(), nextFeet.getZ() + 0.5D);
            return true;
        }

        // Allow only tiny step-ups (e.g. farmland -> full block), but not full 1-block climbs.
        BlockPos nextGround = nextFeet.below();
        BlockState nextState = pal.level().getBlockState(nextGround);
        BlockPos steppedFeet = nextGround.above();
        double nextCollisionTop = nextState.getCollisionShape(pal.level(), nextGround).max(Direction.Axis.Y);
        double stepHeight = (nextGround.getY() + nextCollisionTop) - pal.getY();

        boolean canSmallStep = !canMoveThrough(pal, nextGround)
                && canMoveThrough(pal, steppedFeet)
                && nextCollisionTop > 0.0D
                && nextCollisionTop <= 1.0D
                && stepHeight > 0.0D
                && stepHeight <= 0.2D;

        if (!canSmallStep)
            return false;

        pal.setPos(nextGround.getX() + 0.5D, pal.getY() + stepHeight, nextGround.getZ() + 0.5D);
        return true;
    }
}