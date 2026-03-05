package com.cosmolego527.create_pp.block.custom;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class WrenchableStairBlock extends StairBlock implements IWrenchable {
    public WrenchableStairBlock(BlockState deferredBlock, BlockBehaviour.Properties properties) {
        super(deferredBlock, properties);
    }
}
