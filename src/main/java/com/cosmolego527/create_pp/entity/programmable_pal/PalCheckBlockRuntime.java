package com.cosmolego527.create_pp.entity.programmable_pal;

import com.simibubi.create.content.logistics.filter.FilterItemStack;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

final class PalCheckBlockRuntime {

    private PalCheckBlockRuntime() {
    }

    static boolean isHarvestableCrop(BlockState state) {
        if (state.isAir() || !state.is(BlockTags.CROPS))
            return false;

        if (state.getBlock() instanceof CropBlock cropBlock)
            return cropBlock.isMaxAge(state);

        if (state.hasProperty(BlockStateProperties.AGE_1))
            return state.getValue(BlockStateProperties.AGE_1) >= 1;
        if (state.hasProperty(BlockStateProperties.AGE_2))
            return state.getValue(BlockStateProperties.AGE_2) >= 2;
        if (state.hasProperty(BlockStateProperties.AGE_3))
            return state.getValue(BlockStateProperties.AGE_3) >= 3;
        if (state.hasProperty(BlockStateProperties.AGE_4))
            return state.getValue(BlockStateProperties.AGE_4) >= 4;
        if (state.hasProperty(BlockStateProperties.AGE_5))
            return state.getValue(BlockStateProperties.AGE_5) >= 5;
        if (state.hasProperty(BlockStateProperties.AGE_7))
            return state.getValue(BlockStateProperties.AGE_7) >= 7;
        if (state.hasProperty(BlockStateProperties.AGE_15))
            return state.getValue(BlockStateProperties.AGE_15) >= 15;
        if (state.hasProperty(BlockStateProperties.AGE_25))
            return state.getValue(BlockStateProperties.AGE_25) >= 25;

        return true;
    }

    static boolean matchesConfiguredCheckBlockItem(Level level, Entity self, CompoundTag data, BlockPos checkPos,
                                                   BlockState state, String matchItemTag) {
        if (state.isAir())
            return false;

        if (!data.contains(matchItemTag))
            return true;

        ItemStack configured = ItemStack.parseOptional(level.registryAccess(), data.getCompound(matchItemTag));
        if (configured.isEmpty())
            return true;

        FilterItemStack configuredFilter = FilterItemStack.of(configured.copy());

        ItemStack targetBlockItem = new ItemStack(state.getBlock().asItem());
        if (!targetBlockItem.isEmpty() && configuredFilter.test(level, targetBlockItem))
            return true;

        if (level instanceof ServerLevel serverLevel) {
            LootParams.Builder lootBuilder = new LootParams.Builder(serverLevel)
                    .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(checkPos))
                    .withParameter(LootContextParams.TOOL, ItemStack.EMPTY)
                    .withOptionalParameter(LootContextParams.THIS_ENTITY, self);

            for (ItemStack drop : state.getDrops(lootBuilder)) {
                if (configuredFilter.test(level, drop))
                    return true;
            }
        }

        return false;
    }
}
