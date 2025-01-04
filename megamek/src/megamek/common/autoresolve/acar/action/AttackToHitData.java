/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */
package megamek.common.autoresolve.acar.action;

import megamek.common.InGameObject;
import megamek.common.TargetRoll;
import megamek.common.autoresolve.acar.SimulationContext;
import megamek.common.autoresolve.component.Formation;
import megamek.common.strategicBattleSystems.SBFUnit;
import megamek.common.internationalization.Internationalization;

import java.util.List;

public class AttackToHitData extends TargetRoll {

    public AttackToHitData(int value, String desc) {
        super(value, desc);
    }

    public static AttackToHitData compileToHit(SimulationContext game, StandardUnitAttack attack) {
        if (!attack.isDataValid(game)) {
            return new AttackToHitData(TargetRoll.IMPOSSIBLE, Internationalization.getText("acar.invalid_attack"));
        }

        var attackingFormation = game.getFormation(attack.getEntityId()).orElseThrow();
        var unit = attackingFormation.getUnits().get(attack.getUnitNumber());
        var toHit = new AttackToHitData(attackingFormation.getSkill(), Internationalization.getText("acar.skill"));

        processCriticalDamage(toHit, attackingFormation, attack);
        processRange(toHit, attack);
        processCombatUnit(toHit, unit);
        processTMM(toHit, game, attack);
        processJUMP(toHit, game, attack);
        processMorale(toHit, game, attack);
        processSecondaryTarget(toHit, game, attack);
        return toHit;
    }

    private static void processCriticalDamage(AttackToHitData toHit, Formation formation, StandardUnitAttack attack) {
        SBFUnit combatUnit = formation.getUnits().get(attack.getUnitNumber());
        if (combatUnit.getTargetingCrits() > 0) {
            toHit.addModifier(combatUnit.getTargetingCrits(), Internationalization.getText("acar.critical_target_damage"));
        }
    }

    private static void processCombatUnit(AttackToHitData toHit, SBFUnit unit) {
        switch (unit.getSkill()) {
            case 7 -> toHit.addModifier(+4, Internationalization.getText("acar.skill_7"));
            case 6 -> toHit.addModifier(+3, Internationalization.getText("acar.skill_6"));
            case 5 -> toHit.addModifier(+2, Internationalization.getText("acar.skill_5"));
            case 4 -> toHit.addModifier(+1, Internationalization.getText("acar.skill_4"));
            case 3 -> toHit.addModifier(0, Internationalization.getText("acar.skill_3"));
            case 2 -> toHit.addModifier(-1, Internationalization.getText("acar.skill_2"));
            case 1 -> toHit.addModifier(-2, Internationalization.getText("acar.skill_1"));
            case 0 -> toHit.addModifier(-3, Internationalization.getText("acar.skill_0"));
            default -> toHit.addModifier(TargetRoll.IMPOSSIBLE, Internationalization.getText("acar.invalid_skill"));
        }
    }

    private static void processRange(AttackToHitData toHit, StandardUnitAttack attack) {
        var range = attack.getRange();
        switch (range) {
            case SHORT -> toHit.addModifier(-1, Internationalization.getText( "acar.short_range"));
            case MEDIUM -> toHit.addModifier(+2, Internationalization.getText("acar.medium_range"));
            case LONG -> toHit.addModifier(+4, Internationalization.getText("acar.long_range"));
            case EXTREME -> toHit.addModifier(TargetRoll.IMPOSSIBLE, Internationalization.getText( "acar.extreme_range"));
        }
    }

    private static void processTMM(AttackToHitData toHit, SimulationContext game, StandardUnitAttack attack) {
        var target = game.getFormation(attack.getTargetId()).orElseThrow();
        if (target.getTmm() > 0) {
            toHit.addModifier(target.getTmm(), Internationalization.getText( "acar.TMM"));
        }
    }

    private static void processJUMP(AttackToHitData toHit, SimulationContext game, StandardUnitAttack attack) {
        var attacker = game.getFormation(attack.getEntityId()).orElseThrow();
        var target = game.getFormation(attack.getTargetId()).orElseThrow();
        if (attacker.getJumpUsedThisTurn() > 0) {
            toHit.addModifier(attacker.getJumpUsedThisTurn(), Internationalization.getText("acar.attacker_JUMP"));
        }
        if (target.getJumpUsedThisTurn() > 0) {
            toHit.addModifier(attacker.getJumpUsedThisTurn(), Internationalization.getText("acar.target_JUMP"));
        }
    }

    private static void processMorale(AttackToHitData toHit, SimulationContext game, StandardUnitAttack attack) {
        var target = game.getFormation(attack.getTargetId()).orElseThrow();
        switch (target.moraleStatus()) {
            case SHAKEN -> toHit.addModifier(1, Internationalization.getText("acar.shaken"));
            case UNSTEADY -> toHit.addModifier(2, Internationalization.getText("acar.unsteady"));
            case BROKEN -> toHit.addModifier(3, Internationalization.getText("acar.broken"));
            case ROUTED -> toHit.addModifier(4, Internationalization.getText("acar.routed"));
        }
    }

    private static void processSecondaryTarget(AttackToHitData toHit, SimulationContext game, StandardUnitAttack attack) {
        var attacker = game.getFormation(attack.getEntityId()).orElseThrow();
        if (targetsOfFormation(attacker, game).size() > 2) {
            toHit.addModifier(TargetRoll.IMPOSSIBLE, Internationalization.getText("acar.more_than_two_targets"));
        } else if (targetsOfFormation(attacker, game).size() == 2) {
            toHit.addModifier(+1, Internationalization.getText("acar.two_targets"));
        }
    }

    /**
     * Returns a list of target IDs of all the targets of all attacks that the attacker of the given
     * attack is performing this round. The result can be empty (the unit isn't attacking anything or
     * it is not the firing phase), it can have one or two entries.
     *
     * @param unit The attacker to check attacks for
     * @param game The game
     * @return A list of all target IDs
     */
    public static List<Integer> targetsOfFormation(InGameObject unit, SimulationContext game) {
        return game.getActionsVector().stream()
            .filter(a -> a.getEntityId() == unit.getId())
            .filter(AttackAction.class::isInstance)
            .map(AttackAction.class::cast)
            .map(AttackAction::getTargetId)
            .distinct()
            .toList();
    }
}
