package com.cosmolego527.create_pp.util;

import com.cosmolego527.create_pp.CreatePP;
import com.cosmolego527.create_pp.item.ModItems;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllTags;
import com.simibubi.create.Create;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.foundation.data.TagGen;
import com.simibubi.create.foundation.data.recipe.Mods;
import com.simibubi.create.infrastructure.data.CreateRegistrateTags;
import com.tterrag.registrate.providers.ProviderType;
import com.tterrag.registrate.providers.RegistrateTagsProvider;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.common.Tags;

import java.util.ArrayList;
import java.util.List;

public class CreatePPRegistrateTags {
    private static final CreateRegistrate REGISTRATE = CreatePP.registrate();

    public static void addGenerators() {
        REGISTRATE.addDataGenerator(ProviderType.BLOCK_TAGS, CreatePPRegistrateTags::genBlockTags);
        REGISTRATE.addDataGenerator(ProviderType.ITEM_TAGS, CreatePPRegistrateTags::genItemTags);
        //REGISTRATE.addDataGenerator(ProviderType.FLUID_TAGS, CreatePPRegistrateTags::genFluidTags);
        REGISTRATE.addDataGenerator(ProviderType.ENTITY_TAGS, CreatePPRegistrateTags::genEntityTags);
    }

    private static void genBlockTags(RegistrateTagsProvider<Block> provIn) {
        TagGen.CreateTagsProvider<Block> prov = new TagGen.CreateTagsProvider<>(provIn, Block::builtInRegistryHolder);

        // VALIDATE

        for (ModTags.AllBlockTags tag : ModTags.AllBlockTags.values()) {
            if (tag.alwaysDatagen) {
                prov.getOrCreateRawBuilder(tag.tag);
            }
        }
    }

    private static void genItemTags(RegistrateTagsProvider<Item> provIn) {
        TagGen.CreateTagsProvider<Item> prov = new TagGen.CreateTagsProvider<>(provIn, Item::builtInRegistryHolder);

        prov.tag(ModTags.AllItemTags.PROGRAMMABLE_PAL_ITEM.tag)
                        .add(
                                ModItems.PROGRAMMABLE_PAL_KIT_WHITE.get(),
                                ModItems.PROGRAMMABLE_PAL_KIT_LIGHTGRAY.get(),
                                ModItems.PROGRAMMABLE_PAL_KIT_GRAY.get(),
                                ModItems.PROGRAMMABLE_PAL_KIT_BLACK.get(),
                                ModItems.PROGRAMMABLE_PAL_KIT_RED.get(),
                                ModItems.PROGRAMMABLE_PAL_KIT_ORANGE.get(),
                                ModItems.PROGRAMMABLE_PAL_KIT_YELLOW.get(),
                                ModItems.PROGRAMMABLE_PAL_KIT_LIME.get(),
                                ModItems.PROGRAMMABLE_PAL_KIT_GREEN.get(),
                                ModItems.PROGRAMMABLE_PAL_KIT_LIGHTBLUE.get(),
                                ModItems.PROGRAMMABLE_PAL_KIT_CYAN.get(),
                                ModItems.PROGRAMMABLE_PAL_KIT_BLUE.get(),
                                ModItems.PROGRAMMABLE_PAL_KIT_PURPLE.get(),
                                ModItems.PROGRAMMABLE_PAL_KIT_MAGENTA.get(),
                                ModItems.PROGRAMMABLE_PAL_KIT_PINK.get(),
                                ModItems.PROGRAMMABLE_PAL_KIT_BROWN.get(),
                                ModItems.PROGRAMMABLE_PAL_KIT_DEFAULT.get()
                        );
        prov.tag(ModTags.AllItemTags.PROGRAMMABLE_INSTRUCTION_ITEM.tag)
                .add(ModItems.PROGRAMMABLE_TAPE.get());

        for (ModTags.AllItemTags tag : ModTags.AllItemTags.values()) {
            if (tag.alwaysDatagen) {
                prov.getOrCreateRawBuilder(tag.tag);
            }
        }
    }


/*    private static void genFluidTags(RegistrateTagsProvider<Fluid> provIn) {
        TagGen.CreateTagsProvider<Fluid> prov = new TagGen.CreateTagsProvider<>(provIn, Fluid::builtInRegistryHolder);

        for (ModTags.AllFluidTags tag : ModTags.AllFluidTags.values()) {
            if (tag.alwaysDatagen) {
                prov.getOrCreateRawBuilder(tag.tag);
            }
        }
    }*/

    private static void genEntityTags(RegistrateTagsProvider<EntityType<?>> provIn) {
        TagGen.CreateTagsProvider<EntityType<?>> prov = new TagGen.CreateTagsProvider<>(provIn, EntityType::builtInRegistryHolder);

        for (ModTags.AllEntityTags tag : ModTags.AllEntityTags.values()) {
            if (tag.alwaysDatagen) {
                prov.getOrCreateRawBuilder(tag.tag);
            }
        }
    }
}

