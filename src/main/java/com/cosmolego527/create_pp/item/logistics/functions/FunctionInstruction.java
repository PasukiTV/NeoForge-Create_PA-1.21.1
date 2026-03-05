package com.cosmolego527.create_pp.item.logistics.functions;

import com.simibubi.create.Create;
import com.simibubi.create.content.trains.graph.DiscoveredPath;
import com.simibubi.create.content.trains.schedule.Schedule;
import com.simibubi.create.content.trains.schedule.ScheduleRuntime;
import com.simibubi.create.content.trains.schedule.destination.DestinationInstruction;
import com.simibubi.create.content.trains.schedule.destination.ScheduleInstruction;
import com.simibubi.create.foundation.codec.CreateStreamCodecs;
import net.createmod.catnip.data.Pair;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.function.Supplier;

public abstract class FunctionInstruction extends FunctionDataEntry{
    //public static final StreamCodec<RegistryFriendlyByteBuf, FunctionInstruction> STREAM_CODEC = CreateStreamCodecs.ofLegacyNbtWithRegistries(
    //        FunctionInstruction::write, FunctionInstruction::fromTag
    //);

    public abstract boolean supportsConditions();

    @Nullable
    public abstract DiscoveredPath start(ScheduleRuntime runtime, Level level);

    public final CompoundTag write(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        CompoundTag dataCopy =  data.copy();
        writeAdditional(registries, dataCopy);
        tag.putString("Id", getId().toString());
        tag.put("Data", dataCopy);
        return tag;
    }

    public static FunctionInstruction fromTag(HolderLookup.Provider registries, CompoundTag tag) {
        ResourceLocation location = ResourceLocation.parse(tag.getString("Id"));
        Supplier<? extends FunctionInstruction> supplier = null;
        //for (Pair<ResourceLocation, Supplier<? extends FunctionInstruction>> pair : AbstractFunction.INSTRUCTION_TYPES)
        //    if (pair.getFirst()
        //            .equals(location))
        //        supplier = pair.getSecond();

        if (supplier == null) {
            Create.LOGGER.warn("Could not parse schedule instruction type: " + location);
            // return new MovementInstruction();
        }

        FunctionInstruction scheduleDestination = supplier.get();
        // Left around for migration purposes. Data added in writeAdditional has moved into the "Data" tag
        scheduleDestination.readAdditional(registries, tag);
        CompoundTag data = tag.getCompound("Data");
        scheduleDestination.readAdditional(registries, data);
        scheduleDestination.data = data;
        return scheduleDestination;
    }

}


