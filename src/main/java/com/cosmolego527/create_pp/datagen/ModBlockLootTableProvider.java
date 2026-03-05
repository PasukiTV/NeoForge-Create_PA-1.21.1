package com.cosmolego527.create_pp.datagen;

import com.cosmolego527.create_pp.CreatePP;
import com.cosmolego527.create_pp.block.ModBlocks;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.Set;


public class ModBlockLootTableProvider //extends BlockLootSubProvider
{
//    protected ModBlockLootTableProvider(HolderLookup.Provider registries) {
//        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
//    }
//
////If ever lost, return here
////https://youtu.be/T-9h-FbAQH0?si=K8-w1Hw96DyAD5hr&t=494
//    @Override
//    protected void generate() {
//        dropSelf(ModBlocks.FACTORY_FLOOR.get());
//        add(ModBlocks.FACTORY_FLOOR_SLAB.get(),
//                block -> createSlabItemTable(ModBlocks.FACTORY_FLOOR_SLAB.get()));
//    }
//
//    @Override
//    protected Iterable<Block> getKnownBlocks() {
//        return CreatePP.BLOCKS.getEntries().stream().map(Holder::value)::iterator;
//    }
}


