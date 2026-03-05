package com.cosmolego527.create_pp.entity.programmable_pal;

import java.util.Arrays;
import java.util.Comparator;

public enum ProgrammablePalVariant {
    WHITE(1),
    LIGHTGRAY(2),
    GRAY(3),
    BLACK(4),
    RED(5),
    ORANGE(6),
    YELLOW(7),
    LIME(8),
    GREEN(9),
    LIGHTBLUE(10),
    CYAN(11),
    BLUE(12),
    PURPLE(13),
    MAGENTA(14),
    PINK(15),
    BROWN(16),
    DEFAULT(0);

    private static final ProgrammablePalVariant[] BY_ID = Arrays.stream(values()).sorted(
            Comparator.comparingInt(ProgrammablePalVariant::getId)).toArray(ProgrammablePalVariant[]::new);
    private final int id;

    ProgrammablePalVariant(int id){
        this.id = id;
    }
    public int getId(){
        return id;
    }
    public static ProgrammablePalVariant byId(int id){
        return BY_ID[id % BY_ID.length];
    }
}


