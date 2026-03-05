package com.cosmolego527.create_pp.event;

import com.cosmolego527.create_pp.CreatePP;
import com.cosmolego527.create_pp.entity.ModEntities;
import com.cosmolego527.create_pp.entity.programmable_pal.client.ProgrammablePalModel;
import com.cosmolego527.create_pp.entity.programmable_pal.ProgrammablePalEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;

@EventBusSubscriber(modid = CreatePP.MOD_ID)
public class ModEventBusEvents {

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(ProgrammablePalModel.LAYER_LOCATION, ProgrammablePalModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event){
        event.put(ModEntities.PROGRAMMABLE_PAL_ENTITY.get(), ProgrammablePalEntity.createPalAttributes().build());
    }


}


