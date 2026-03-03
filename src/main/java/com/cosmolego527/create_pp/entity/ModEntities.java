package com.cosmolego527.create_pp.entity;

import com.cosmolego527.create_pp.CreatePP;
import com.cosmolego527.create_pp.entity.client.ProgrammablePalRenderer;
import com.cosmolego527.create_pp.entity.custom.ProgrammablePalEntity;
import com.simibubi.create.foundation.data.CreateEntityBuilder;
import com.tterrag.registrate.util.entry.EntityEntry;
import com.tterrag.registrate.util.nullness.NonNullConsumer;
import com.tterrag.registrate.util.nullness.NonNullFunction;
import com.tterrag.registrate.util.nullness.NonNullSupplier;
import net.createmod.catnip.lang.Lang;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public class ModEntities {


    public static final EntityEntry<ProgrammablePalEntity> PROGRAMMABLE_PAL_ENTITY = register("programmable_pal", ProgrammablePalEntity::new, () -> ProgrammablePalRenderer::new,
            MobCategory.MISC, 10, 3, true, false, ProgrammablePalEntity::build).properties(p -> p.sized(0.625f, 0.875f))
            .register();

    private static <T extends Entity> CreateEntityBuilder<T, ?> register(String name, EntityType.EntityFactory<T> factory,
                                                                         NonNullSupplier<NonNullFunction<EntityRendererProvider.Context, EntityRenderer<? super T>>> renderer,
                                                                         MobCategory group, int range, int updateFrequency, boolean sendVelocity, boolean immuneToFire,
                                                                         NonNullConsumer<EntityType.Builder<T>> propertyBuilder) {
        String id = Lang.asId(name);
        return (CreateEntityBuilder<T, ?>) CreatePP.REGISTRATE
                .entity(id, factory, group)
                .properties(b -> b.setTrackingRange(range)
                        .setUpdateInterval(updateFrequency)
                        .setShouldReceiveVelocityUpdates(sendVelocity))
                .properties(propertyBuilder)
                .properties(b -> {
                    if (immuneToFire)
                        b.fireImmune();
                })
                .renderer(renderer);
    }
//    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
//            DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, CreatePP.MOD_ID);
//
//    public static final Supplier<EntityType<ProgrammablePalEntity>> PROGRAMMABLE_PAL =
//            ENTITY_TYPES.register("programmable_pal", () -> EntityType.Builder.of(ProgrammablePalEntity::new, MobCategory.CREATURE)
//                    .sized(0.625f, 0.875f).build("programmable_pal"));
//


    public static void register(){}


}
