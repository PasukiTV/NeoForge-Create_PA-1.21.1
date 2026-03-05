package com.cosmolego527.create_pp.block;

import com.cosmolego527.create_pp.CreatePP;
import com.cosmolego527.create_pp.block.custom.WrenchableBlock;
import com.cosmolego527.create_pp.block.custom.WrenchableSlabBlock;
import com.cosmolego527.create_pp.block.custom.WrenchableStairBlock;
import com.cosmolego527.create_pp.item.ModCreativeModeTabs;
import com.cosmolego527.create_pp.item.ModItems;
import com.simibubi.create.foundation.data.BuilderTransformers;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.foundation.data.SharedProperties;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import javax.swing.*;
import java.util.function.Supplier;

public class ModBlocks {
    private static final CreateRegistrate REGISTRATE = CreatePP.registrate();

    static {
        REGISTRATE.setCreativeTab(ModCreativeModeTabs.CREATE_PP_TAB);
    }

    public static final BlockEntry<WrenchableBlock> FACTORY_FLOOR = CreatePP.REGISTRATE.block("factory_floor", WrenchableBlock::new)
            .transform(BuilderTransformers.palettesIronBlock())
            .lang("Factory Floor")
            .register();




//    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(CreatePP.MOD_ID);
//
//
//    public static final DeferredBlock<Block> FACTORY_FLOOR = registerBlock("factory_floor",
//            () -> new WrenchableBlock(BlockBehaviour.Properties.of()
//                    .strength(4f)
//                    .requiresCorrectToolForDrops()
//                    .sound(SoundType.NETHERITE_BLOCK)));
//    public static final DeferredBlock<StairBlock> FACTORY_FLOOR_STAIRS = registerBlock("factory_floor_stairs",
//            () -> new WrenchableStairBlock(ModBlocks.FACTORY_FLOOR.get().defaultBlockState(),
//                    BlockBehaviour.Properties.of().strength(3f).requiresCorrectToolForDrops().sound(SoundType.NETHERITE_BLOCK)));
//    public static final DeferredBlock<SlabBlock> FACTORY_FLOOR_SLAB = registerBlock("factory_floor_slab",
//            () -> new WrenchableSlabBlock(BlockBehaviour.Properties.of().strength(2f).requiresCorrectToolForDrops().sound(SoundType.NETHERITE_BLOCK)));
//
//
//
//
//
//    private static <T extends Block> DeferredBlock<T> registerBlock(String name, Supplier<T> block) {
//        DeferredBlock<T> toReturn = BLOCKS.register(name, block );
//        registerBlockItem(name, toReturn);
//        return toReturn;
//    }
//
//    private static <T extends Block> void registerBlockItem(String name, DeferredBlock<T> block){
//        ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
//    }

    public static void register(){}
}