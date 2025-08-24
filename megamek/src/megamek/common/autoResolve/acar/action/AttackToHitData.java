/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */

package megamek.common.autoResolve.acar.action;

import java.util.List;

import megamek.common.autoResolve.acar.SimulationContext;
import megamek.common.autoResolve.component.Formation;
import megamek.common.game.InGameObject;
import megamek.common.internationalization.I18n;
import megamek.common.rolls.TargetRoll;
import megamek.common.strategicBattleSystems.SBFUnit;

public class AttackToHitData extends TargetRoll {

    public AttackToHitData(int value, String desc) {
        super(value, desc);
    }

    public static AttackToHitData compileToHit(SimulationContext game, StandardUnitAttack attack) {
        if (!attack.isDataValid(game)) {
            return new AttackToHitData(TargetRoll.IMPOSSIBLE, I18n.getText("acar.invalid_attack"));
        }

        var attackingFormation = game.getFormation(attack.getEntityId()).orElseThrow();
        // var unit = attackingFormation.getUnits().get(attack.getUnitNumber());
        var toHit = new AttackToHitData(attackingFormation.getSkill(), I18n.getText("acar.skill"));

        processCriticalDamage(toHit, attackingFormation, attack);
        processRange(toHit, attack);
        // processCombatUnit(toHit, unit);
        processTMM(toHit, game, attack);
        processJUMP(toHit, game, attack);
        processMorale(toHit, game, attack);
        processSecondaryTarget(toHit, game, attack);
        processCover(toHit, game, attack);
        return toHit;
    }

    private static void processCover(AttackToHitData toHit, SimulationContext game, StandardUnitAttack attack) {
        var target = game.getFormation(attack.getTargetId()).orElseThrow();
        if (target.getMemory().getBoolean("cover").orElse(false)) {
            toHit.addModifier(+1, I18n.getText("acar.cover"));
        }
    }

    private static void processCriticalDamage(AttackToHitData toHit, Formation formation, StandardUnitAttack attack) {
        SBFUnit combatUnit = formation.getUnits().get(attack.getUnitNumber());
        if (combatUnit.getTargetingCrits() > 0) {
            toHit.addModifier(combatUnit.getTargetingCrits(), I18n.getText("acar.critical_target_damage"));
        }
    }

    private static void processCombatUnit(AttackToHitData toHit, SBFUnit unit) {
        switch (unit.getSkill()) {
            case 7 -> toHit.addModifier(+4, I18n.getText("acar.skill_7"));
            case 6 -> toHit.addModifier(+3, I18n.getText("acar.skill_6"));
            case 5 -> toHit.addModifier(+2, I18n.getText("acar.skill_5"));
            case 4 -> toHit.addModifier(+1, I18n.getText("acar.skill_4"));
            case 3 -> toHit.addModifier(0, I18n.getText("acar.skill_3"));
            case 2 -> toHit.addModifier(-1, I18n.getText("acar.skill_2"));
            case 1 -> toHit.addModifier(-2, I18n.getText("acar.skill_1"));
            case 0 -> toHit.addModifier(-3, I18n.getText("acar.skill_0"));
            default -> toHit.addModifier(TargetRoll.IMPOSSIBLE, I18n.getText("acar.invalid_skill"));
        }
    }

    private static void processRange(AttackToHitData toHit, StandardUnitAttack attack) {
        var range = attack.getRange();
        switch (range) {
            case SHORT -> toHit.addModifier(-1, I18n.getText("acar.short_range"));
            case MEDIUM -> toHit.addModifier(+2, I18n.getText("acar.medium_range"));
            case LONG -> toHit.addModifier(+4, I18n.getText("acar.long_range"));
            default -> toHit.addModifier(TargetRoll.IMPOSSIBLE, I18n.getText("acar.extreme_range"));
        }
    }

    private static void processTMM(AttackToHitData toHit, SimulationContext game, StandardUnitAttack attack) {
        var target = game.getFormation(attack.getTargetId()).orElseThrow();
        if (target.getTmm() > 0) {
            toHit.addModifier(target.getTmm(), I18n.getText("acar.TMM"));
        }
    }

    private static void processJUMP(AttackToHitData toHit, SimulationContext game, StandardUnitAttack attack) {
        var attacker = game.getFormation(attack.getEntityId()).orElseThrow();
        var target = game.getFormation(attack.getTargetId()).orElseThrow();
        if (attacker.getJumpUsedThisTurn() > 0) {
            toHit.addModifier(attacker.getJumpUsedThisTurn(), I18n.getText("acar.attacker_JUMP"));
        }
        if (target.getJumpUsedThisTurn() > 0) {
            toHit.addModifier(attacker.getJumpUsedThisTurn(), I18n.getText("acar.target_JUMP"));
        }
    }

    private static void processMorale(AttackToHitData toHit, SimulationContext game, StandardUnitAttack attack) {
        var target = game.getFormation(attack.getTargetId()).orElseThrow();
        switch (target.moraleStatus()) {
            case SHAKEN -> toHit.addModifier(1, I18n.getText("acar.shaken"));
            case UNSTEADY -> toHit.addModifier(2, I18n.getText("acar.unsteady"));
            case BROKEN -> toHit.addModifier(3, I18n.getText("acar.broken"));
            case ROUTED -> toHit.addModifier(4, I18n.getText("acar.routed"));
            default -> toHit.doNothing();
        }
    }

    private static void processSecondaryTarget(AttackToHitData toHit, SimulationContext game,
          StandardUnitAttack attack) {
        var attacker = game.getFormation(attack.getEntityId()).orElseThrow();
        if (targetsOfFormation(attacker, game).size() > 2) {
            toHit.addModifier(TargetRoll.IMPOSSIBLE, I18n.getText("acar.more_than_two_targets"));
        } else if (targetsOfFormation(attacker, game).size() == 2) {
            toHit.addModifier(+1, I18n.getText("acar.two_targets"));
        }
    }

    /**
     * Returns a list of target IDs of all the targets of all attacks that the attacker of the given attack is
     * performing this round. The result can be empty (the unit isn't attacking anything, or it is not the firing
     * phase), it can have one or two entries.
     *
     * @param unit The attacker to check attacks for
     * @param game The game
     *
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
