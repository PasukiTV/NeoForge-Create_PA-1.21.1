package com.cosmolego527.create_pp.datagen.recipegenerators;

import com.cosmolego527.create_pp.CreatePP;
import com.cosmolego527.create_pp.block.ModBlocks;
import com.cosmolego527.create_pp.item.ModItems;
import com.cosmolego527.create_pp.util.ModTags;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.*;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.conditions.IConditionBuilder;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ModRecipeProvider extends RecipeProvider implements IConditionBuilder {
    public ModRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }


    @Override
    protected void buildRecipes(RecipeOutput recipeOutput) {

        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModBlocks.FACTORY_FLOOR.get(), 8)
                .pattern("I I")
                .pattern(" I ")
                .pattern("I I")
                .define('I', Items.IRON_INGOT)
                .unlockedBy("has_iron", has(Items.IRON_INGOT)).save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.AUTOMATON_PROCESSOR.get())
                .pattern("E E")
                .pattern("ACA")
                .pattern(" P ")
                .define('C', Items.CLOCK)
                .define('A', AllItems.ANDESITE_ALLOY.get())
                .define('P', AllItems.PRECISION_MECHANISM.get())
                .define('E', AllItems.ELECTRON_TUBE)
                .unlockedBy("has_precision_mechanism", has(AllItems.PRECISION_MECHANISM)).save(recipeOutput);

        SingleItemRecipeBuilder.stonecutting(Ingredient.of(AllBlocks.INDUSTRIAL_IRON_BLOCK),RecipeCategory.BUILDING_BLOCKS, ModBlocks.FACTORY_FLOOR)
                .unlockedBy("has_industrial_iron", has(AllBlocks.INDUSTRIAL_IRON_BLOCK))
                .save(recipeOutput, "create_programmablepals:factory_floor_from_industrial_iron_stonecutting");
        SingleItemRecipeBuilder.stonecutting(Ingredient.of(Items.IRON_INGOT),RecipeCategory.BUILDING_BLOCKS, ModBlocks.FACTORY_FLOOR, 2)
                .unlockedBy("has_iron_ingot", has(Items.IRON_INGOT))
                .save(recipeOutput, "create_programmablepals:factory_floor_from_iron_ingot_stonecutting");

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.PROGRAMMABLE_PAL_KIT_DEFAULT.get())
                .requires(ModTags.AllItemTags.PROGRAMMABLE_PAL_ITEM.tag)
                .unlockedBy("has_programmable_pal", has(ModTags.AllItemTags.PROGRAMMABLE_PAL_ITEM.tag))
                .save(recipeOutput, "create_programmablepals:ppal_from_any_ppal");



    }

    protected static void oreSmelting(RecipeOutput recipeOutput, List<ItemLike> pIngredients, RecipeCategory pCategory, ItemLike pResult,
                                      float pExperience, int pCookingTIme, String pGroup) {
        oreCooking(recipeOutput, RecipeSerializer.SMELTING_RECIPE, SmeltingRecipe::new, pIngredients, pCategory, pResult,
                pExperience, pCookingTIme, pGroup, "_from_smelting");
    }

    protected static void oreBlasting(RecipeOutput recipeOutput, List<ItemLike> pIngredients, RecipeCategory pCategory, ItemLike pResult,
                                      float pExperience, int pCookingTime, String pGroup) {
        oreCooking(recipeOutput, RecipeSerializer.BLASTING_RECIPE, BlastingRecipe::new, pIngredients, pCategory, pResult,
                pExperience, pCookingTime, pGroup, "_from_blasting");
    }

    protected static <T extends AbstractCookingRecipe> void oreCooking(RecipeOutput recipeOutput, RecipeSerializer<T> pCookingSerializer, AbstractCookingRecipe.Factory<T> factory,
                                                                       List<ItemLike> pIngredients, RecipeCategory pCategory, ItemLike pResult, float pExperience, int pCookingTime, String pGroup, String pRecipeName) {
        for(ItemLike itemlike : pIngredients) {
            SimpleCookingRecipeBuilder.generic(Ingredient.of(itemlike), pCategory, pResult, pExperience, pCookingTime, pCookingSerializer, factory).group(pGroup).unlockedBy(getHasName(itemlike), has(itemlike))
                    .save(recipeOutput, CreatePP.MOD_ID + ":" + getItemName(pResult) + pRecipeName + "_" + getItemName(itemlike));
        }
    }
}


