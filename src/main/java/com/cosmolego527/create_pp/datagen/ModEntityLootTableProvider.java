package com.cosmolego527.create_pp.datagen;

import com.cosmolego527.create_pp.entity.ModEntities;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.EntityLootSubProvider;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.level.storage.loot.LootTable;

public class ModEntityLootTableProvider extends EntityLootSubProvider {
    protected ModEntityLootTableProvider(FeatureFlagSet required, HolderLookup.Provider registries) {
        super(required, registries);
    }

    @Override
    public void generate() {
        this.add(ModEntities.PROGRAMMABLE_PAL_ENTITY.get(), LootTable.lootTable());
    }
}


