/*
package com.cosmolego527.create_pp.util.ai;

import com.cosmolego527.create_pp.entity.custom.ProgrammablePalEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.util.GoalUtils;
import net.minecraft.world.phys.Vec3;

import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;

public class RelativePos {

//    public static Vec3 getPos(ProgrammablePalEntity pPal, int distance, int direction){
//        boolean flag = GoalUtils.mobRestricted(pPal, 100);
//        return generatePos()
//    }


    public static Vec3 generatePos(Supplier<BlockPos> posSupplier, ProgrammablePalEntity pPal){
        return generatePos(posSupplier, pPal::getWalkTargetValue);
    }
    public static Vec3 generatePos(Supplier<BlockPos> posSupplier, ToDoubleFunction<BlockPos> toDubFunc) {
        double d0 = Double.NEGATIVE_INFINITY;
        BlockPos blockPos = null;

        for (int i = 0; i < 10; i++){
            BlockPos blockpos1 = posSupplier.get();
            if (blockpos1 != null){
                double d1 = toDubFunc.applyAsDouble(blockpos1);
                if(d1 > d0){
                    d0=d1;
                    blockPos = blockpos1;
                }
            }
        }
        return blockPos != null ? Vec3.atBottomCenterOf(blockPos) : null;
    }
}
*/
