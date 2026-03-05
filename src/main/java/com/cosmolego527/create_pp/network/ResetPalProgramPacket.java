package com.cosmolego527.create_pp.network;

import com.cosmolego527.create_pp.CreatePP;
import com.cosmolego527.create_pp.entity.programmable_pal.menu.ProgrammablePalMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ResetPalProgramPacket(int palEntityId) implements CustomPacketPayload {

    public static final Type<ResetPalProgramPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CreatePP.MOD_ID, "reset_pal_program"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ResetPalProgramPacket> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, ResetPalProgramPacket::palEntityId, ResetPalProgramPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ResetPalProgramPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer serverPlayer))
                return;
            if (!(serverPlayer.containerMenu instanceof ProgrammablePalMenu menu))
                return;
            if (menu.getPalId() != packet.palEntityId())
                return;

            menu.resetPalToProgramStart();
        });
    }
}

