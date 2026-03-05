package com.cosmolego527.create_pp.item.logistics.functions;

import com.cosmolego527.create_pp.util.CreatePPLang;
import com.google.common.collect.ImmutableList;
import com.simibubi.create.foundation.gui.ModularGuiLineBuilder;
import com.simibubi.create.foundation.utility.CreateLang;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.List;

public abstract class TextFunctionInstruction extends FunctionInstruction{

    protected String getLabelText(){return textData("Text");}

    @Override
    public List<Component> getTitleAs(String type) {
        return ImmutableList.of(CreatePPLang.translateDirect("function."+type+"."+getId().getPath()+".summary")
                .withStyle(ChatFormatting.DARK_GRAY), CreatePPLang.translateDirect("generic.in_quotes", Component.literal(getLabelText())));
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void initConfigurationWidgets(ModularGuiLineBuilder builder) {
        builder.addTextInput(0,121,(e,t)-> modifyEditBox(e), "Text");
    }
    @OnlyIn(Dist.CLIENT)
    protected void modifyEditBox(EditBox box){}
}


