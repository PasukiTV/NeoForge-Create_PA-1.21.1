package com.cosmolego527.create_pp.datagen;

import com.cosmolego527.create_pp.CreatePP;
import com.cosmolego527.create_pp.item.ModItems;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public class ModItemModelProvider extends ItemModelProvider {
    public ModItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, CreatePP.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerModels() {

        basicItem(ModItems.AUTOMATON_PROCESSOR.get());

        basicItem(ModItems.PROGRAMMABLE_TAPE.get());
        basicItem(ModItems.FIGHT_TAPE.get());
//        basicItem(ModItems.VOID_FUNCTION_TAPE.get());
//        basicItem(ModItems.BOOL_FUNCTION_TAPE.get());
//        basicItem(ModItems.STRING_FUNCTION_TAPE.get());
//        basicItem(ModItems.INT_FUNCTION_TAPE.get());
//        basicItem(ModItems.FLOAT_FUNCTION_TAPE.get());

        basicItem(ModItems.CONCLUSES_MUSIC_DISC.get());
        basicItem(ModItems.SESULCNOC_MUSIC_DISC.get());

        basicItem(ModItems.PROGRAMMABLE_PAL_KIT_DEFAULT.get());
        basicItem(ModItems.PROGRAMMABLE_PAL_KIT_WHITE.get());
        basicItem(ModItems.PROGRAMMABLE_PAL_KIT_LIGHTGRAY.get());
        basicItem(ModItems.PROGRAMMABLE_PAL_KIT_GRAY.get());
        basicItem(ModItems.PROGRAMMABLE_PAL_KIT_BLACK.get());
        basicItem(ModItems.PROGRAMMABLE_PAL_KIT_RED.get());
        basicItem(ModItems.PROGRAMMABLE_PAL_KIT_ORANGE.get());
        basicItem(ModItems.PROGRAMMABLE_PAL_KIT_YELLOW.get());
        basicItem(ModItems.PROGRAMMABLE_PAL_KIT_LIME.get());
        basicItem(ModItems.PROGRAMMABLE_PAL_KIT_GREEN.get());
        basicItem(ModItems.PROGRAMMABLE_PAL_KIT_LIGHTBLUE.get());
        basicItem(ModItems.PROGRAMMABLE_PAL_KIT_CYAN.get());
        basicItem(ModItems.PROGRAMMABLE_PAL_KIT_BLUE.get());
        basicItem(ModItems.PROGRAMMABLE_PAL_KIT_PURPLE.get());
        basicItem(ModItems.PROGRAMMABLE_PAL_KIT_MAGENTA.get());
        basicItem(ModItems.PROGRAMMABLE_PAL_KIT_PINK.get());
        basicItem(ModItems.PROGRAMMABLE_PAL_KIT_BROWN.get());
    }
}
