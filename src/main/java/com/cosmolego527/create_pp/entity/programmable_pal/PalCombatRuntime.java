package com.cosmolego527.create_pp.entity.programmable_pal;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

final class PalCombatRuntime {

    private PalCombatRuntime() {
    }

    static void runFightTapeBehavior(ProgrammablePalEntity pal) {
        if (!(pal.level() instanceof ServerLevel serverLevel))
            return;

        ItemStack combatTool = pal.bestCombatToolInInventory();
        if (!combatTool.isEmpty())
            pal.setHeldTool(combatTool);
        else
            pal.setHeldTool(ItemStack.EMPTY);

        var commanderUUID = pal.getCommanderUUID();
        Player commander = commanderUUID == null ? null : serverLevel.getPlayerByUUID(commanderUUID);
        if (commander == null)
            return;

        LivingEntity target = commander.getLastHurtMob();
        if (target == null || !target.isAlive() || target.distanceToSqr(pal) > 256.0D)
            target = commander.getLastHurtByMob();

        if (target != null && target.isAlive() && target != pal) {
            pal.setTarget(target);
        } else if (pal.shouldFollowCommander()) {
            double distSq = pal.distanceToSqr(commander);
            if (distSq > 9.0D)
                pal.getNavigation().moveTo(commander, 1.1D);
            else if (distSq < 4.0D)
                pal.getNavigation().stop();
        }
    }
}