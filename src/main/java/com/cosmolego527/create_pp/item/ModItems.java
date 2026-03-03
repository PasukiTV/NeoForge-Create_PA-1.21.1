package com.cosmolego527.create_pp.item;

import com.cosmolego527.create_pp.CreatePP;
import com.cosmolego527.create_pp.item.custom.ProgrammablePalKitItem;
import com.cosmolego527.create_pp.item.tapes.FunctionTapeItem;
import com.cosmolego527.create_pp.sound.ModSounds;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.util.entry.ItemEntry;
import net.minecraft.world.item.Item;

public class ModItems {
    private static final CreateRegistrate REGISTRATE = CreatePP.registrate();

    static {
        REGISTRATE.setCreativeTab(ModCreativeModeTabs.CREATE_PP_TAB);
    }

    public static final ItemEntry<Item> AUTOMATON_PROCESSOR =
            CreatePP.REGISTRATE.item("automaton_processor", Item::new)
                    .register();


    public static final ItemEntry<FunctionTapeItem>
            PROGRAMMABLE_TAPE =
            CreatePP.REGISTRATE.item("programmable_tape", FunctionTapeItem::programmableTapeItem)
                    .register(),
            VOID_FUNCTION_TAPE =
            CreatePP.REGISTRATE.item("void_function_tape", FunctionTapeItem::voidFuncItem)
                    .register(),
            BOOL_FUNCTION_TAPE =
            CreatePP.REGISTRATE.item("bool_function_tape", FunctionTapeItem::boolFuncItem)
                    .register(),
            STRING_FUNCTION_TAPE =
            CreatePP.REGISTRATE.item("string_function_tape", FunctionTapeItem::stringFuncItem)
                    .register(),
            INT_FUNCTION_TAPE =
            CreatePP.REGISTRATE.item("int_function_tape", FunctionTapeItem::intFuncItem)
                    .register(),
            FLOAT_FUNCTION_TAPE =
            CreatePP.REGISTRATE.item("float_function_tape", FunctionTapeItem::intFuncItem)
                    .register();


    public static final ItemEntry<Item> CONCLUSES_MUSIC_DISC = CreatePP.REGISTRATE.item("concluses_music_disc", Item::new)
            .lang("Concluses Music Disc")
            .properties(p -> p.jukeboxPlayable(ModSounds.CONCLUSES_KEY).stacksTo(1))
            .register();
    public static final ItemEntry<Item> SESULCNOC_MUSIC_DISC = CreatePP.REGISTRATE.item("sesulcnoc_music_disc", Item::new)
            .lang("sesulcnoC Music Disc")
            .properties(p -> p.jukeboxPlayable(ModSounds.SESULCNOC_KEY).stacksTo(1))
            .register();

    public static final ItemEntry<ProgrammablePalKitItem>
            PROGRAMMABLE_PAL_KIT_WHITE = CreatePP.REGISTRATE.item("programmable_pal_box_dyed4", ProgrammablePalKitItem::PPalWhite).lang("Programmable Pal").register(),
            PROGRAMMABLE_PAL_KIT_LIGHTGRAY = CreatePP.REGISTRATE.item("programmable_pal_box_dyed3", ProgrammablePalKitItem::PPalLightGray).lang("Programmable Pal").register(),
            PROGRAMMABLE_PAL_KIT_GRAY = CreatePP.REGISTRATE.item("programmable_pal_box_dyed2", ProgrammablePalKitItem::PPalGray).lang("Programmable Pal").register(),
            PROGRAMMABLE_PAL_KIT_BLACK = CreatePP.REGISTRATE.item("programmable_pal_box_dyed1", ProgrammablePalKitItem::PPalBlack).lang("Programmable Pal").register(),
            PROGRAMMABLE_PAL_KIT_RED = CreatePP.REGISTRATE.item("programmable_pal_box_dyed5", ProgrammablePalKitItem::PPalRed).lang("Programmable Pal").register(),
            PROGRAMMABLE_PAL_KIT_ORANGE = CreatePP.REGISTRATE.item("programmable_pal_box_dyed6", ProgrammablePalKitItem::PPalOrange).lang("Programmable Pal").register(),
            PROGRAMMABLE_PAL_KIT_YELLOW = CreatePP.REGISTRATE.item("programmable_pal_box_dyed7", ProgrammablePalKitItem::PPalYellow).lang("Programmable Pal").register(),
            PROGRAMMABLE_PAL_KIT_LIME = CreatePP.REGISTRATE.item("programmable_pal_box_dyed8", ProgrammablePalKitItem::PPalLime).lang("Programmable Pal").register(),
            PROGRAMMABLE_PAL_KIT_GREEN = CreatePP.REGISTRATE.item("programmable_pal_box_dyed9", ProgrammablePalKitItem::PPalGreen).lang("Programmable Pal").register(),
            PROGRAMMABLE_PAL_KIT_LIGHTBLUE = CreatePP.REGISTRATE.item("programmable_pal_box_dyed10", ProgrammablePalKitItem::PPalLightBlue).lang("Programmable Pal").register(),
            PROGRAMMABLE_PAL_KIT_CYAN = CreatePP.REGISTRATE.item("programmable_pal_box_dyed11", ProgrammablePalKitItem::PPalCyan).lang("Programmable Pal").register(),
            PROGRAMMABLE_PAL_KIT_BLUE = CreatePP.REGISTRATE.item("programmable_pal_box_dyed12", ProgrammablePalKitItem::PPalBlue).lang("Programmable Pal").register(),
            PROGRAMMABLE_PAL_KIT_PURPLE = CreatePP.REGISTRATE.item("programmable_pal_box_dyed13", ProgrammablePalKitItem::PPalPurple).lang("Programmable Pal").register(),
            PROGRAMMABLE_PAL_KIT_MAGENTA = CreatePP.REGISTRATE.item("programmable_pal_box_dyed14", ProgrammablePalKitItem::PPalMagenta).lang("Programmable Pal").register(),
            PROGRAMMABLE_PAL_KIT_PINK = CreatePP.REGISTRATE.item("programmable_pal_box_dyed15", ProgrammablePalKitItem::PPalPink).lang("Programmable Pal").register(),
            PROGRAMMABLE_PAL_KIT_BROWN = CreatePP.REGISTRATE.item("programmable_pal_box_dyed16", ProgrammablePalKitItem::PPalBrown).lang("Programmable Pal").register(),
            PROGRAMMABLE_PAL_KIT_DEFAULT = CreatePP.REGISTRATE.item("programmable_pal_box", ProgrammablePalKitItem::PPalDefault).lang("Programmable Pal").register();

//    static{
//        boolean created = false;
//        for(ProgrammablePalStyles.PPalStyle style : ProgrammablePalStyles.STYLES){
//
//            ItemBuilder<ProgrammablePalKit, CreateRegistrate> programmablePalKit = CPP_BuilderTransformers.programmablePalItem(style);
//
//            if (created)
//                programmablePalKit.setData(ProviderType.LANG, NonNullBiConsumer.noop());
//
//            created |= style.made();
//            programmablePalKit.register();
//        }
//    }

//    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CreatePP.MOD_ID);
//
//    public static final DeferredItem<Item> AUTOMATON_PROCESSOR = ITEMS.register("automaton_processor",
//            () -> new Item(new Item.Properties()));
//
//
//    public static final DeferredItem<Item> COLORED_TAPE_VOID = ITEMS.register("colored_tape_void",
//            () -> new FunctionTapeItem(FunctionTapeItem.FunctionType.VOID, new Item.Properties()));
//    public static final DeferredItem<Item> COLORED_TAPE_FLOAT = ITEMS.register("colored_tape_float",
//            () -> new Item(new Item.Properties()));
//    public static final DeferredItem<Item> COLORED_TAPE_BOOL = ITEMS.register("colored_tape_bool",
//            () -> new FunctionTapeItem(FunctionTapeItem.FunctionType.BOOL, new Item.Properties()));
//    public static final DeferredItem<Item> COLORED_TAPE_INT = ITEMS.register("colored_tape_int",
//            () -> new FunctionTapeItem(FunctionTapeItem.FunctionType.INT, new Item.Properties()));
//    public static final DeferredItem<Item> COLORED_TAPE_STRING = ITEMS.register("colored_tape_string",
//            () -> new FunctionTapeItem(FunctionTapeItem.FunctionType.STRING, new Item.Properties()));
//
//    //public static final ItemEntry<FunctionTapeItem> COLORED_TAPE_VOID = REGISTRATE.item("colored_tape_void", FunctionTapeItem::voidFuncItem)
//    //        .lang("Void Function Tape")
//    //        .register();
//
//    public static final DeferredItem<Item> CONCLUSES_MUSIC_DISC = ITEMS.register("concluses_music_disc",
//            ()-> new Item(new Item.Properties().jukeboxPlayable(ModSounds.CONSLUSES_KEY).stacksTo(1)));
//    public static final DeferredItem<Item> SESULCNOC_MUSIC_DISC = ITEMS.register("sesulcnoc_music_disc",
//            ()-> new Item(new Item.Properties().jukeboxPlayable(ModSounds.SESULCNOC_KEY).stacksTo(1)));

//    public static final DeferredItem<Item> PROGRAMMABLE_PAL_BOX = ITEMS.register("programmable_pal_box",
//            ()-> new DeferredSpawnEggItem(ModEntities.PROGRAMMABLE_PAL, 0xFFFFFF, 0xFFFFFF,
//                    new Item.Properties()));


    public static void register() {

    }
}
