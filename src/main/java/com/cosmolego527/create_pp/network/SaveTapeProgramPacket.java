package com.cosmolego527.create_pp.network;

import com.cosmolego527.create_pp.CreatePP;
import com.cosmolego527.create_pp.component.ModDataComponentTypes;
import com.cosmolego527.create_pp.item.tapes.TapeProgramMenu;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SaveTapeProgramPacket(CompoundTag scheduleTag) implements CustomPacketPayload {

    public static final Type<SaveTapeProgramPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CreatePP.MOD_ID, "save_tape_program"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SaveTapeProgramPacket> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.COMPOUND_TAG, SaveTapeProgramPacket::scheduleTag, SaveTapeProgramPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SaveTapeProgramPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer serverPlayer))
                return;
            if (!(serverPlayer.containerMenu instanceof TapeProgramMenu menu))
                return;

            if (packet.scheduleTag() == null || packet.scheduleTag().isEmpty())
                menu.contentHolder.remove(ModDataComponentTypes.VOID_FUNCTION_DATA);
            else
                menu.contentHolder.set(ModDataComponentTypes.VOID_FUNCTION_DATA, packet.scheduleTag().copy());
        });
    }
}