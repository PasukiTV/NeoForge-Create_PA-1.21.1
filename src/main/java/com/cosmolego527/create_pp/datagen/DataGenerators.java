package com.cosmolego527.create_pp.datagen;


import com.cosmolego527.create_pp.CreatePP;
import com.cosmolego527.create_pp.datagen.recipegenerators.CreatePPMechanicalCraftingRecipeGen;
import com.cosmolego527.create_pp.datagen.recipegenerators.ModRecipeProvider;
import com.cosmolego527.create_pp.sound.ModSounds;
import com.cosmolego527.create_pp.util.CreatePPGeneratedEntriesProvider;
import com.cosmolego527.create_pp.util.CreatePPRegistrateTags;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.simibubi.create.AllKeys;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.Create;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.data.recipe.CreateRecipeProvider;
import com.simibubi.create.foundation.ponder.CreatePonderPlugin;
import com.simibubi.create.foundation.utility.FilesHelper;
import com.simibubi.create.infrastructure.data.*;
import com.tterrag.registrate.providers.ProviderType;
import net.createmod.ponder.foundation.PonderIndex;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

public class DataGenerators {



    public static void gatherDataHighPriority(GatherDataEvent event) {
        if (event.getMods().contains(CreatePP.MOD_ID))
            addExtraRegistrateData();
    }
    public static void gatherData(GatherDataEvent event) {
        if (!event.getMods().contains(CreatePP.MOD_ID))
            return;

        DataGenerator generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();
        ExistingFileHelper existingFileHelper = event.getExistingFileHelper();

        //generator.addProvider(event.includeClient(), AllSoundEvents.provider(generator));

        CreatePPGeneratedEntriesProvider generatedEntriesProvider = new CreatePPGeneratedEntriesProvider(output, lookupProvider);
        lookupProvider = generatedEntriesProvider.getRegistryProvider();
        generator.addProvider(event.includeServer(), generatedEntriesProvider);

        generator.addProvider(event.includeServer(), new ModRecipeProvider(output, lookupProvider));

        BlockTagsProvider blockTagsProvider = new ModBlockTagProvider(output, lookupProvider, existingFileHelper);
        generator.addProvider(event.includeServer(), blockTagsProvider);

        generator.addProvider(event.includeServer(), new ModItemTagProvider(output, lookupProvider, blockTagsProvider.contentsGetter(), existingFileHelper));

        generator.addProvider(event.includeClient(), new ModItemModelProvider(output,existingFileHelper));

//        generator.addProvider(event.includeServer(), new CreateRecipeSerializerTagsProvider(output, lookupProvider, existingFileHelper));
//        generator.addProvider(event.includeServer(), new CreateContraptionTypeTagsProvider(output, lookupProvider, existingFileHelper));
//        generator.addProvider(event.includeServer(), new CreateMountedItemStorageTypeTagsProvider(output, lookupProvider, existingFileHelper));
//        generator.addProvider(event.includeServer(), new DamageTypeTagGen(output, lookupProvider, existingFileHelper));
//        generator.addProvider(event.includeServer(), new AllAdvancements(output, lookupProvider));
//        generator.addProvider(event.includeServer(), new CreateStandardRecipeGen(output, lookupProvider));
        generator.addProvider(event.includeServer(), new CreatePPMechanicalCraftingRecipeGen(output, lookupProvider));
//        generator.addProvider(event.includeServer(), new CreateSequencedAssemblyRecipeGen(output, lookupProvider));
//        generator.addProvider(event.includeServer(), new CreateDatamapProvider(output, lookupProvider));



//        if (event.includeServer()) {
//            CreateRecipeProvider.registerAllProcessing(generator, output, lookupProvider);
//        }
    }

    private static void addExtraRegistrateData() {
        CreatePPRegistrateTags.addGenerators();
//
        CreatePP.registrate().addDataGenerator(ProviderType.LANG, provider -> {
            BiConsumer<String, String> langConsumer = provider::add;

            provideDefaultLang("interface", langConsumer);
            provideDefaultLang("tooltips", langConsumer);
//            AllAdvancements.provideLang(langConsumer);
            ModSounds.provideLang(langConsumer);
//            AllKeys.provideLang(langConsumer);
//            providePonderLang(langConsumer);
        });
    }

    private static void provideDefaultLang(String fileName, BiConsumer<String, String> consumer) {
        String path = "assets/create_programmablepals/lang/default/" + fileName + ".json";
        JsonElement jsonElement = FilesHelper.loadJsonResource(path);
        if (jsonElement == null) {
            throw new IllegalStateException(String.format("Could not find default lang file: %s", path));
        }
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue().getAsString();
            consumer.accept(key, value);
        }
    }

    private static void providePonderLang(BiConsumer<String, String> consumer) {
        // Register this since FMLClientSetupEvent does not run during datagen
        PonderIndex.addPlugin(new CreatePonderPlugin());

        PonderIndex.getLangAccess().provideLang(CreatePP.MOD_ID, consumer);
    }
}


