package com.cosmolego527.create_pp.datagen.recipegenerators;

import com.cosmolego527.create_pp.CreatePP;
import com.cosmolego527.create_pp.item.ModItems;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.api.data.recipe.MechanicalCraftingRecipeGen;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.concurrent.CompletableFuture;

public class CreatePPMechanicalCraftingRecipeGen extends MechanicalCraftingRecipeGen {

    GeneratedRecipe

            PROGRAMMABLE_PAL_WHITE = create(ModItems.PROGRAMMABLE_PAL_KIT_WHITE::get)
            .recipe(b->b
                    .key('G', Ingredient.of(Items.WHITE_STAINED_GLASS))
                    .key('P', Ingredient.of(ModItems.AUTOMATON_PROCESSOR))
                    .key('A', Ingredient.of(AllBlocks.ANDESITE_CASING))
                    .key('B', Ingredient.of(AllItems.BRASS_HAND))
                    .key('C', Ingredient.of(AllItems.COPPER_DIVING_BOOTS))
                    .patternLine(" G ")
                    .patternLine(" P ")
                    .patternLine("BAB")
                    .patternLine(" C ")
                    .disallowMirrored()),
            PROGRAMMABLE_PAL_LIGHTGRAY = create(ModItems.PROGRAMMABLE_PAL_KIT_LIGHTGRAY::get)
                    .recipe(b->b
                            .key('G', Ingredient.of(Items.LIGHT_GRAY_STAINED_GLASS))
                            .key('P', Ingredient.of(ModItems.AUTOMATON_PROCESSOR))
                            .key('A', Ingredient.of(AllBlocks.ANDESITE_CASING))
                            .key('B', Ingredient.of(AllItems.BRASS_HAND))
                            .key('C', Ingredient.of(AllItems.COPPER_DIVING_BOOTS))
                            .patternLine(" G ")
                            .patternLine(" P ")
                            .patternLine("BAB")
                            .patternLine(" C ")
                            .disallowMirrored()),
            PROGRAMMABLE_PAL_GRAY = create(ModItems.PROGRAMMABLE_PAL_KIT_GRAY::get)
                    .recipe(b->b
                            .key('G', Ingredient.of(Items.GRAY_STAINED_GLASS))
                            .key('P', Ingredient.of(ModItems.AUTOMATON_PROCESSOR))
                            .key('A', Ingredient.of(AllBlocks.ANDESITE_CASING))
                            .key('B', Ingredient.of(AllItems.BRASS_HAND))
                            .key('C', Ingredient.of(AllItems.COPPER_DIVING_BOOTS))
                            .patternLine(" G ")
                            .patternLine(" P ")
                            .patternLine("BAB")
                            .patternLine(" C ")
                            .disallowMirrored()),
            PROGRAMMABLE_PAL_BLACK = create(ModItems.PROGRAMMABLE_PAL_KIT_BLACK::get)
                    .recipe(b->b
                            .key('G', Ingredient.of(Items.BLACK_STAINED_GLASS))
                            .key('P', Ingredient.of(ModItems.AUTOMATON_PROCESSOR))
                            .key('A', Ingredient.of(AllBlocks.ANDESITE_CASING))
                            .key('B', Ingredient.of(AllItems.BRASS_HAND))
                            .key('C', Ingredient.of(AllItems.COPPER_DIVING_BOOTS))
                            .patternLine(" G ")
                            .patternLine(" P ")
                            .patternLine("BAB")
                            .patternLine(" C ")
                            .disallowMirrored()),
            PROGRAMMABLE_PAL_RED = create(ModItems.PROGRAMMABLE_PAL_KIT_RED::get)
                    .recipe(b->b
                            .key('G', Ingredient.of(Items.RED_STAINED_GLASS))
                            .key('P', Ingredient.of(ModItems.AUTOMATON_PROCESSOR))
                            .key('A', Ingredient.of(AllBlocks.ANDESITE_CASING))
                            .key('B', Ingredient.of(AllItems.BRASS_HAND))
                            .key('C', Ingredient.of(AllItems.COPPER_DIVING_BOOTS))
                            .patternLine(" G ")
                            .patternLine(" P ")
                            .patternLine("BAB")
                            .patternLine(" C ")
                            .disallowMirrored()),
            PROGRAMMABLE_PAL_ORANGE = create(ModItems.PROGRAMMABLE_PAL_KIT_ORANGE::get)
                    .recipe(b->b
                            .key('G', Ingredient.of(Items.ORANGE_STAINED_GLASS))
                            .key('P', Ingredient.of(ModItems.AUTOMATON_PROCESSOR))
                            .key('A', Ingredient.of(AllBlocks.ANDESITE_CASING))
                            .key('B', Ingredient.of(AllItems.BRASS_HAND))
                            .key('C', Ingredient.of(AllItems.COPPER_DIVING_BOOTS))
                            .patternLine(" G ")
                            .patternLine(" P ")
                            .patternLine("BAB")
                            .patternLine(" C ")
                            .disallowMirrored()),
            PROGRAMMABLE_PAL_YELLO = create(ModItems.PROGRAMMABLE_PAL_KIT_YELLOW::get)
                    .recipe(b->b
                            .key('G', Ingredient.of(Items.YELLOW_STAINED_GLASS))
                            .key('P', Ingredient.of(ModItems.AUTOMATON_PROCESSOR))
                            .key('A', Ingredient.of(AllBlocks.ANDESITE_CASING))
                            .key('B', Ingredient.of(AllItems.BRASS_HAND))
                            .key('C', Ingredient.of(AllItems.COPPER_DIVING_BOOTS))
                            .patternLine(" G ")
                            .patternLine(" P ")
                            .patternLine("BAB")
                            .patternLine(" C ")
                            .disallowMirrored()),
            PROGRAMMABLE_PAL_LIME = create(ModItems.PROGRAMMABLE_PAL_KIT_LIME::get)
                    .recipe(b->b
                            .key('G', Ingredient.of(Items.LIME_STAINED_GLASS))
                            .key('P', Ingredient.of(ModItems.AUTOMATON_PROCESSOR))
                            .key('A', Ingredient.of(AllBlocks.ANDESITE_CASING))
                            .key('B', Ingredient.of(AllItems.BRASS_HAND))
                            .key('C', Ingredient.of(AllItems.COPPER_DIVING_BOOTS))
                            .patternLine(" G ")
                            .patternLine(" P ")
                            .patternLine("BAB")
                            .patternLine(" C ")
                            .disallowMirrored()),
            PROGRAMMABLE_PAL_GREEN = create(ModItems.PROGRAMMABLE_PAL_KIT_GREEN::get)
                    .recipe(b->b
                            .key('G', Ingredient.of(Items.GREEN_STAINED_GLASS))
                            .key('P', Ingredient.of(ModItems.AUTOMATON_PROCESSOR))
                            .key('A', Ingredient.of(AllBlocks.ANDESITE_CASING))
                            .key('B', Ingredient.of(AllItems.BRASS_HAND))
                            .key('C', Ingredient.of(AllItems.COPPER_DIVING_BOOTS))
                            .patternLine(" G ")
                            .patternLine(" P ")
                            .patternLine("BAB")
                            .patternLine(" C ")
                            .disallowMirrored()),
            PROGRAMMABLE_PAL_LIGHTBLUE = create(ModItems.PROGRAMMABLE_PAL_KIT_LIGHTBLUE::get)
                    .recipe(b->b
                            .key('G', Ingredient.of(Items.LIGHT_BLUE_STAINED_GLASS))
                            .key('P', Ingredient.of(ModItems.AUTOMATON_PROCESSOR))
                            .key('A', Ingredient.of(AllBlocks.ANDESITE_CASING))
                            .key('B', Ingredient.of(AllItems.BRASS_HAND))
                            .key('C', Ingredient.of(AllItems.COPPER_DIVING_BOOTS))
                            .patternLine(" G ")
                            .patternLine(" P ")
                            .patternLine("BAB")
                            .patternLine(" C ")
                            .disallowMirrored()),
            PROGRAMMABLE_PAL_CYAN = create(ModItems.PROGRAMMABLE_PAL_KIT_CYAN::get)
                    .recipe(b->b
                            .key('G', Ingredient.of(Items.CYAN_STAINED_GLASS))
                            .key('P', Ingredient.of(ModItems.AUTOMATON_PROCESSOR))
                            .key('A', Ingredient.of(AllBlocks.ANDESITE_CASING))
                            .key('B', Ingredient.of(AllItems.BRASS_HAND))
                            .key('C', Ingredient.of(AllItems.COPPER_DIVING_BOOTS))
                            .patternLine(" G ")
                            .patternLine(" P ")
                            .patternLine("BAB")
                            .patternLine(" C ")
                            .disallowMirrored()),
            PROGRAMMABLE_PAL_BLUE = create(ModItems.PROGRAMMABLE_PAL_KIT_BLUE::get)
                    .recipe(b->b
                            .key('G', Ingredient.of(Items.BLUE_STAINED_GLASS))
                            .key('P', Ingredient.of(ModItems.AUTOMATON_PROCESSOR))
                            .key('A', Ingredient.of(AllBlocks.ANDESITE_CASING))
                            .key('B', Ingredient.of(AllItems.BRASS_HAND))
                            .key('C', Ingredient.of(AllItems.COPPER_DIVING_BOOTS))
                            .patternLine(" G ")
                            .patternLine(" P ")
                            .patternLine("BAB")
                            .patternLine(" C ")
                            .disallowMirrored()),
            PROGRAMMABLE_PAL_PURPLE = create(ModItems.PROGRAMMABLE_PAL_KIT_PURPLE::get)
                    .recipe(b->b
                            .key('G', Ingredient.of(Items.PURPLE_STAINED_GLASS))
                            .key('P', Ingredient.of(ModItems.AUTOMATON_PROCESSOR))
                            .key('A', Ingredient.of(AllBlocks.ANDESITE_CASING))
                            .key('B', Ingredient.of(AllItems.BRASS_HAND))
                            .key('C', Ingredient.of(AllItems.COPPER_DIVING_BOOTS))
                            .patternLine(" G ")
                            .patternLine(" P ")
                            .patternLine("BAB")
                            .patternLine(" C ")
                            .disallowMirrored()),
            PROGRAMMABLE_PAL_MAGENTA = create(ModItems.PROGRAMMABLE_PAL_KIT_MAGENTA::get)
                    .recipe(b->b
                            .key('G', Ingredient.of(Items.MAGENTA_STAINED_GLASS))
                            .key('P', Ingredient.of(ModItems.AUTOMATON_PROCESSOR))
                            .key('A', Ingredient.of(AllBlocks.ANDESITE_CASING))
                            .key('B', Ingredient.of(AllItems.BRASS_HAND))
                            .key('C', Ingredient.of(AllItems.COPPER_DIVING_BOOTS))
                            .patternLine(" G ")
                            .patternLine(" P ")
                            .patternLine("BAB")
                            .patternLine(" C ")
                            .disallowMirrored()),
            PROGRAMMABLE_PAL_PINK = create(ModItems.PROGRAMMABLE_PAL_KIT_PINK::get)
                    .recipe(b->b
                            .key('G', Ingredient.of(Items.PINK_STAINED_GLASS))
                            .key('P', Ingredient.of(ModItems.AUTOMATON_PROCESSOR))
                            .key('A', Ingredient.of(AllBlocks.ANDESITE_CASING))
                            .key('B', Ingredient.of(AllItems.BRASS_HAND))
                            .key('C', Ingredient.of(AllItems.COPPER_DIVING_BOOTS))
                            .patternLine(" G ")
                            .patternLine(" P ")
                            .patternLine("BAB")
                            .patternLine(" C ")
                            .disallowMirrored()),
            PROGRAMMABLE_PAL_BROWN = create(ModItems.PROGRAMMABLE_PAL_KIT_BROWN::get)
                    .recipe(b->b
                            .key('G', Ingredient.of(Items.BROWN_STAINED_GLASS))
                            .key('P', Ingredient.of(ModItems.AUTOMATON_PROCESSOR))
                            .key('A', Ingredient.of(AllBlocks.ANDESITE_CASING))
                            .key('B', Ingredient.of(AllItems.BRASS_HAND))
                            .key('C', Ingredient.of(AllItems.COPPER_DIVING_BOOTS))
                            .patternLine(" G ")
                            .patternLine(" P ")
                            .patternLine("BAB")
                            .patternLine(" C ")
                            .disallowMirrored())

                    ;

    public CreatePPMechanicalCraftingRecipeGen(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, CreatePP.MOD_ID);
    }
}