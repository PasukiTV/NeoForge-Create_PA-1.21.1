package com.cosmolego527.create_pp.entity.programmable_pal;

import com.cosmolego527.create_pp.CreatePP;
import com.cosmolego527.create_pp.entity.programmable_pal.ProgrammablePalVariant;
import com.google.common.collect.ImmutableList;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Locale;

public class ProgrammablePalStyles {
    public record PPalStyle(String type, ProgrammablePalVariant Variant, boolean made){
        public ResourceLocation getItemId(){
            String variant = Variant.name().toLowerCase(Locale.ROOT);
            String id = type + "_programmable_pal_" + variant;
            return CreatePP.asResource(id);
        }
    }

    public static final List<PPalStyle> STYLES = ImmutableList.of(
            new PPalStyle("programmable_pal_kit", ProgrammablePalVariant.WHITE, true),
            new PPalStyle("programmable_pal_kit", ProgrammablePalVariant.LIGHTGRAY, true),
            new PPalStyle("programmable_pal_kit", ProgrammablePalVariant.GRAY, true),
            new PPalStyle("programmable_pal_kit", ProgrammablePalVariant.BLACK, true),
            new PPalStyle("programmable_pal_kit", ProgrammablePalVariant.RED, true),
            new PPalStyle("programmable_pal_kit", ProgrammablePalVariant.ORANGE, true),
            new PPalStyle("programmable_pal_kit", ProgrammablePalVariant.YELLOW, true),
            new PPalStyle("programmable_pal_kit", ProgrammablePalVariant.LIME, true),
            new PPalStyle("programmable_pal_kit", ProgrammablePalVariant.GREEN, true),
            new PPalStyle("programmable_pal_kit", ProgrammablePalVariant.LIGHTBLUE, true),
            new PPalStyle("programmable_pal_kit", ProgrammablePalVariant.CYAN, true),
            new PPalStyle("programmable_pal_kit", ProgrammablePalVariant.BLUE, true),
            new PPalStyle("programmable_pal_kit", ProgrammablePalVariant.PURPLE, true),
            new PPalStyle("programmable_pal_kit", ProgrammablePalVariant.MAGENTA, true),
            new PPalStyle("programmable_pal_kit", ProgrammablePalVariant.PINK, true),
            new PPalStyle("programmable_pal_kit", ProgrammablePalVariant.BROWN, true),
            new PPalStyle("programmable_pal_kit", ProgrammablePalVariant.DEFAULT, true)
    );
}


