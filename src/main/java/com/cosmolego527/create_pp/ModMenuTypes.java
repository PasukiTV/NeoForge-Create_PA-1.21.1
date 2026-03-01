package com.cosmolego527.create_pp;

//import com.cosmolego527.create_pp.item.logistics.functions.voidfunc.VoidFunctionMenu;
//import com.cosmolego527.create_pp.item.logistics.functions.voidfunc.VoidFunctionScreen;
import com.cosmolego527.create_pp.item.logistics.functions.program.TapeProgramMenu;
import com.cosmolego527.create_pp.item.logistics.functions.program.TapeProgramScreen;
import com.tterrag.registrate.builders.MenuBuilder;
import com.tterrag.registrate.util.entry.MenuEntry;
import com.tterrag.registrate.util.nullness.NonNullSupplier;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.world.inventory.AbstractContainerMenu;


public class ModMenuTypes {
    //public static final MenuEntry<VoidFunctionMenu> VOID_FUNCTION_MENU =
    //        register("void_function_menu", VoidFunctionMenu::new, () -> VoidFunctionScreen::new);
    public static final MenuEntry<TapeProgramMenu> TAPE_PROGRAM_MENU =
            register("tape_program_menu", TapeProgramMenu::new, () -> TapeProgramScreen::new);






    private static <C extends AbstractContainerMenu, S extends Screen & MenuAccess<C>> MenuEntry<C> register(
            String name, MenuBuilder.ForgeMenuFactory<C> factory, NonNullSupplier<MenuBuilder.ScreenFactory<C, S>> screenFactory) {
        return CreatePP.REGISTRATE
                .menu(name, factory, screenFactory)
                .register();
    }

    public static void register(){}

//    public static final DeferredRegister<MenuType<?>> MENUS =
//            DeferredRegister.create(Registries.MENU, CreatePP.MOD_ID);
//
//    public static final DeferredHolder<MenuType<?>, MenuType<VoidFunctionMenu>> VOID_FUNCTION_MENU =
//            registerMenuType("void_function_menu", VoidFunctionMenu::new);
//
//
//    private static <T extends AbstractContainerMenu>DeferredHolder<MenuType<?>, MenuType<T>> registerMenuType(String name, IContainerFactory<T> factory){
//
//        return MENUS.register(name, ()-> IMenuTypeExtension.create(factory));
//    }
//
//    public static void register(IEventBus eventBus){
//        MENUS.register(eventBus);
//    }
//    public <T extends AbstractContainerMenu, SC extends Screen & MenuAccess<T>> MenuBuilder<T, SC, S> menu(String name, MenuBuilder.ForgeMenuFactory<T> factory, NonNullSupplier<MenuBuilder.ScreenFactory<T, SC>> screenFactory) {
//        return menu(self(), name, factory, screenFactory);
//    }
}
