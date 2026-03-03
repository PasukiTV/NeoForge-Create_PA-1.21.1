/*package com.cosmolego527.create_pp.util;

import com.cosmolego527.create_pp.item.logistics.functionblocks.BoolFuncBlock;
import com.cosmolego527.create_pp.item.logistics.functionblocks.IfElseBlock;
import com.cosmolego527.create_pp.item.logistics.functionblocks.MoveFuncBlock;
import com.cosmolego527.create_pp.item.logistics.functionblocks.abstractblocks.ActiveFuncBlock;
import com.cosmolego527.create_pp.item.logistics.functionblocks.abstractblocks.FuncBlock;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;

public class FuncBlockSerializer {
    public static CompoundTag serialize(FuncBlock block) {
        CompoundTag tag = new CompoundTag();

        if (block instanceof MoveFuncBlock move) {
            tag.putString("Type", "Move");
        }

        else if (block instanceof ActiveFuncBlock active) {
            tag.putString("Type", "Active");
        }

        else if (block instanceof BoolFuncBlock bool) {
            tag.putString("Type", "Bool");
            tag.putBoolean("BoolCondition", bool.getValue());
        }

        else if (block instanceof IfElseBlock ifElse) {
            tag.putString("Type", "IfElse");
            tag.put("Condition", serialize(ifElse.getCondition()));
            tag.put("Then", writeBlockList(ifElse.getThenAction()));
            if (ifElse.getElseAction() != null)
                tag.put("Else", writeBlockList(ifElse.getElseAction()));
        }

        return tag;
    }

    public static FuncBlock deserialize(CompoundTag tag) {
        String type = tag.getString("Type");

        return switch (type) {
            case "Move" -> new MoveFuncBlock();
            case "Bool" -> new BoolFuncBlock(tag.getBoolean("BoolCondition"));
            case "IfElse" -> new IfElseBlock(
                    deserialize(tag.getCompound("Condition")),
                    readBlockList(tag.getList("Then", Tag.TAG_COMPOUND)),
                    tag.contains("Else") ? readBlockList(tag.getList("Else", Tag.TAG_COMPOUND)) : null
            );
            default -> throw new IllegalArgumentException("Unknown FuncBlock type: " + type);
        };
    }

    private static ListTag writeBlockList(List<FuncBlock> blocks) {
        ListTag tagList = new ListTag();
        for (FuncBlock b : blocks)
            tagList.add(serialize(b));
        return tagList;
    }

    private static List<FuncBlock> readBlockList(ListTag tagList) {
        List<FuncBlock> blocks = new ArrayList<>();
        for (Tag tag : tagList)
            blocks.add(deserialize((CompoundTag) tag));
        return blocks;
    }
}*/
