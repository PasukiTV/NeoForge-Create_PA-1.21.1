package com.cosmolego527.create_pp.network;

import com.cosmolego527.create_pp.CreatePP;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = CreatePP.MOD_ID)
public class ModPackets {

    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(SaveTapeProgramPacket.TYPE, SaveTapeProgramPacket.STREAM_CODEC, SaveTapeProgramPacket::handle);
        registrar.playToServer(ResetPalProgramPacket.TYPE, ResetPalProgramPacket.STREAM_CODEC, ResetPalProgramPacket::handle);
    }
}

