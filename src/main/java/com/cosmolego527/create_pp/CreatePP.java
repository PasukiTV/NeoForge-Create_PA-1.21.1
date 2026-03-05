package com.cosmolego527.create_pp;

import com.cosmolego527.create_pp.block.ModBlocks;
import com.cosmolego527.create_pp.component.ModDataComponentTypes;
import com.cosmolego527.create_pp.datagen.DataGenerators;
import com.cosmolego527.create_pp.entity.ModEntities;
import com.cosmolego527.create_pp.event.ModEventBusEvents;
import com.cosmolego527.create_pp.item.ModCreativeModeTabs;
import com.cosmolego527.create_pp.item.ModItems;
import com.cosmolego527.create_pp.sound.ModSounds;
import com.cosmolego527.create_pp.util.ModTags;
import com.simibubi.create.AllCreativeModeTabs;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.foundation.item.ItemDescription;
import com.simibubi.create.foundation.item.KineticStats;
import com.simibubi.create.foundation.item.TooltipModifier;
import net.createmod.catnip.lang.FontHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.ModLoadingContext;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(CreatePP.MOD_ID)
public class CreatePP {
    // Define mod id in a common place for everything to reference
    public static final String MOD_ID = "create_programmablepals";

    public static final String NAME = "Create: Programmable Pals";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();

    private static final StackWalker STACK_WALKER = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

    public static final CreateRegistrate REGISTRATE = CreateRegistrate.create(CreatePP.MOD_ID)
            .defaultCreativeTab((ResourceKey<CreativeModeTab>) null)
            .setTooltipModifierFactory(item ->
                    new ItemDescription.Modifier(item, FontHelper.Palette.STANDARD_CREATE)
                            .andThen(TooltipModifier.mapNull(KineticStats.create(item)))
            );


    public CreatePP(IEventBus eventBus, ModContainer modContainer) {
        onCtor(eventBus, modContainer);
    }

    private void onCtor(IEventBus modEventBus, ModContainer modContainer) {
        ModLoadingContext modLoadingContext = ModLoadingContext.get();
        REGISTRATE.registerEventListeners(modEventBus);

        ModSounds.prepare();
        ModTags.init();
        ModCreativeModeTabs.register(modEventBus);
        ModBlocks.register();
        ModItems.register();
        //ModBlockEntities.register();
        ModMenuTypes.register();
        //ModPackets.register();
        //ModConfigs.register(modLoadingContext, modContainer);
        ModEntities.register();
        //ModDisplaySources.register();
        ModDataComponentTypes.register(modEventBus);

        modEventBus.addListener(EventPriority.HIGHEST, DataGenerators::gatherDataHighPriority);
        modEventBus.addListener(EventPriority.LOWEST, DataGenerators::gatherData);
        modEventBus.addListener(ModSounds::register);
    }

    public static ResourceLocation asResource(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
    public static CreateRegistrate registrate() {
        if (!STACK_WALKER.getCallerClass().getPackageName().startsWith("com.cosmolego527.create_pp"))
            throw new UnsupportedOperationException("Don't use my create registrate instance, make your own!!");
        return REGISTRATE;
    }
}

