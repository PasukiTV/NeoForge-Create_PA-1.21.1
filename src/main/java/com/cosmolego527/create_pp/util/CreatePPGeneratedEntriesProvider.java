package com.cosmolego527.create_pp.util;

import com.cosmolego527.create_pp.CreatePP;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class CreatePPGeneratedEntriesProvider extends DatapackBuiltinEntriesProvider {

    public static final RegistrySetBuilder BUILDER = new RegistrySetBuilder();

    public CreatePPGeneratedEntriesProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries){
        super(output, registries, BUILDER, Set.of(CreatePP.MOD_ID));
    }

    @Override
    public String getName() {
        return "Create: Programmable Pals' Generated Registry Entries";
    }
}


