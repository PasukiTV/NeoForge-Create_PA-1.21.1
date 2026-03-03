/*
package com.cosmolego527.create_pp;

import com.simibubi.create.Create;
import net.createmod.catnip.gui.TextureSheetSegment;
import net.createmod.catnip.gui.UIRenderHelper;
import net.createmod.catnip.gui.element.ScreenElement;
import net.createmod.catnip.theme.Color;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public enum ModGuiTextures implements ScreenElement, TextureSheetSegment {

    SCHEDULE_TEST("test_schedule", 256, 226),
    SCHEDULE_CARD_DARK_TEST("test_schedule", 5, 233, 1, 1),
    SCHEDULE_CARD_MEDIUM_TEST("test_schedule", 6, 233, 1, 1),
    SCHEDULE_CARD_LIGHT_TEST("test_schedule", 7, 233, 1, 1),
    SCHEDULE_CARD_MOVE_UP_TEST("test_schedule", 51, 230, 12, 12),
    SCHEDULE_CARD_MOVE_DOWN_TEST("test_schedule", 65, 230, 12, 12),
    SCHEDULE_CARD_REMOVE_TEST("test_schedule", 51, 243, 12, 12),
    SCHEDULE_CARD_DUPLICATE_TEST("test_schedule", 65, 243, 12, 12),
    SCHEDULE_CARD_NEW_TEST("test_schedule", 79, 239, 16, 16),
    SCHEDULE_CONDITION_NEW_TEST("test_schedule", 96, 239, 19, 16),
    SCHEDULE_CONDITION_LEFT_TEST("test_schedule", 116, 239, 6, 16),
    SCHEDULE_CONDITION_LEFT_CLEAN_TEST("test_schedule", 147, 239, 2, 16),
    SCHEDULE_CONDITION_MIDDLE_TEST("test_schedule", 123, 239, 1, 16),
    SCHEDULE_CONDITION_ITEM_TEST("test_schedule", 125, 239, 18, 16),
    SCHEDULE_CONDITION_RIGHT_TEST("test_schedule", 144, 239, 2, 16),
    SCHEDULE_CONDITION_APPEND_TEST("test_schedule", 150, 245, 10, 10),
    SCHEDULE_SCROLL_LEFT_TEST("test_schedule", 161, 247, 4, 8),
    SCHEDULE_SCROLL_RIGHT_TEST("test_schedule", 166, 247, 4, 8),
    SCHEDULE_STRIP_DARK_TEST("test_schedule", 5, 235, 3, 1),
    SCHEDULE_STRIP_LIGHT_TEST("test_schedule", 5, 237, 3, 1),
    SCHEDULE_STRIP_WAIT_TEST("test_schedule", 1, 239, 11, 16),
    SCHEDULE_STRIP_TRAVEL_TEST("test_schedule", 12, 239, 11, 16),
    SCHEDULE_STRIP_DOTTED_TEST("test_schedule", 23, 239, 11, 16),
    SCHEDULE_STRIP_END_TEST("test_schedule", 34, 239, 11, 16),
    SCHEDULE_STRIP_ACTION_TEST("test_schedule", 209, 239, 11, 16),
    SCHEDULE_EDITOR_TEST("test_schedule_2", 256, 89),
    SCHEDULE_EDITOR_ADDITIONAL_SLOT_TEST("test_schedule_2", 55, 47, 32, 18),
    SCHEDULE_EDITOR_INACTIVE_SLOT_TEST("test_schedule_2", 0, 91, 18, 18),
    SCHEDULE_POINTER_TEST("test_schedule", 185, 239, 21, 16),
    SCHEDULE_POINTER_OFFSCREEN_TEST("test_schedule", 171, 239, 13, 16),



    ;
    public static final int FONT_COLOR = 0x575F7A;

    public final ResourceLocation location;
    private final int width;
    private final int height;
    private final int startX;
    private final int startY;

    ModGuiTextures(String location, int width, int height) {
        this(location, 0, 0, width, height);
    }

    ModGuiTextures(String location, int startX, int startY, int width, int height) {
        this(Create.ID, location, startX, startY, width, height);
    }

    ModGuiTextures(String namespace, String location, int startX, int startY, int width, int height) {
        this.location = ResourceLocation.fromNamespaceAndPath(namespace, "textures/gui/" + location + ".png");
        this.width = width;
        this.height = height;
        this.startX = startX;
        this.startY = startY;
    }

    @Override
    public ResourceLocation getLocation() {
        return location;
    }

    @OnlyIn(Dist.CLIENT)
    public void render(GuiGraphics graphics, int x, int y) {
        graphics.blit(location, x, y, startX, startY, width, height);
    }

    @OnlyIn(Dist.CLIENT)
    public void render(GuiGraphics graphics, int x, int y, Color c) {
        bind();
        UIRenderHelper.drawColoredTexture(graphics, c, x, y, startX, startY, width, height);
    }

    @Override
    public int getStartX() {
        return startX;
    }

    @Override
    public int getStartY() {
        return startY;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }
}
*/
