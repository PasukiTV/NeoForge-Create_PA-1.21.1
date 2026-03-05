package com.cosmolego527.create_pp;

import com.cosmolego527.create_pp.entity.programmable_pal.client.ProgrammablePalScreen;
import com.cosmolego527.create_pp.entity.programmable_pal.menu.ProgrammablePalMenu;
import com.cosmolego527.create_pp.item.tape_program.TapeProgramMenu;
import com.cosmolego527.create_pp.item.tape_program.TapeProgramScreen;
import com.tterrag.registrate.builders.MenuBuilder;
import com.tterrag.registrate.util.entry.MenuEntry;
import com.tterrag.registrate.util.nullness.NonNullSupplier;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.world.inventory.AbstractContainerMenu;


public class ModMenuTypes {
    public static final MenuEntry<TapeProgramMenu> TAPE_PROGRAM_MENU =
            register("tape_program_menu", TapeProgramMenu::new, () -> TapeProgramScreen::new);

    public static final MenuEntry<ProgrammablePalMenu> PROGRAMMABLE_PAL_MENU =
            register("programmable_pal_menu", ProgrammablePalMenu::new, () -> ProgrammablePalScreen::new);


    private static <C extends AbstractContainerMenu, S extends Screen & MenuAccess<C>> MenuEntry<C> register(
            String name, MenuBuilder.ForgeMenuFactory<C> factory, NonNullSupplier<MenuBuilder.ScreenFactory<C, S>> screenFactory) {
        return CreatePP.REGISTRATE
                .menu(name, factory, screenFactory)
                .register();
    }

    public static void register(){}

}


