/*
package com.cosmolego527.create_pp.item.logistics.functionblocks;

import com.cosmolego527.create_pp.entity.custom.ProgrammablePalEntity;
import com.cosmolego527.create_pp.item.logistics.functionblocks.abstractblocks.FuncBlock;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

public class MoveFuncBlock extends FuncBlock {
    @Override
    public void execute(FunctionContext context) {
        ProgrammablePalEntity pPal = context.Ppal;
        Direction direction = pPal.getDirection();
        Vec3 position = pPal.position();
        Vec3 destination = position.relative(direction, 1d);
        pPal.getNavigation().moveTo(destination.x,destination.y,destination.z, pPal.getSpeed());
    }
}
*/
