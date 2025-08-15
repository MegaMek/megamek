/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.strategicBattleSystems;

import java.util.List;

import megamek.common.InGameObject;
import megamek.common.TargetRoll;
import megamek.common.TargetRollModifier;
import megamek.common.actions.sbf.SBFAttackAction;
import megamek.common.actions.sbf.SBFStandardUnitAttack;

public class SBFToHitData extends TargetRoll {

    public SBFToHitData(int value, String desc) {
        super(value, desc);
    }

    public SBFToHitData() {}

    public static SBFToHitData compiletoHit(SBFGame game, SBFStandardUnitAttack attack) {
        if (!attack.isDataValid(game)) {
            return new SBFToHitData(TargetRoll.IMPOSSIBLE, "Invalid attack");
        }
        //noinspection OptionalGetWithoutIsPresent
        SBFFormation attackingFormation = game.getFormation(attack.getEntityId()).get();
        SBFToHitData toHit = new SBFToHitData(attackingFormation.getSkill(), "Skill");
        if (!processRange(toHit, game, attack)) {
            return toHit;
        }
        processTMM(toHit, game, attack);
        processJUMP(toHit, game, attack);
        processMorale(toHit, game, attack);
        processSecondaryTarget(toHit, game, attack);
        return toHit;
    }

    private static boolean processRange(SBFToHitData toHit, SBFGame game, SBFStandardUnitAttack attack) {
        //noinspection OptionalGetWithoutIsPresent
        SBFFormation attackingFormation = game.getFormation(attack.getEntityId()).get();
        //noinspection OptionalGetWithoutIsPresent
        SBFFormation target = game.getFormation(attack.getTargetId()).get();

        //TODO check if position is null
        // subclass for arty attacks, building attacks?
        if (!attackingFormation.getPosition().isSameBoardAs(target.getPosition())) {
            toHit.addModifier(new TargetRollModifier(TargetRoll.IMPOSSIBLE, "not on the same board"));
            return false;
        }
        int range = attackingFormation.getPosition().coords().distance(target.getPosition().coords());
        if (range > 2) {
            toHit.addModifier(new TargetRollModifier(TargetRoll.IMPOSSIBLE, "out of range"));
            return false;
        } else if (range == 2) {
            toHit.addModifier(new TargetRollModifier(3, "extreme range"));
        } else if (range == 1) {
            toHit.addModifier(new TargetRollModifier(3, "long range"));
        }
        return true;
    }

    private static void processTMM(SBFToHitData toHit, SBFGame game, SBFStandardUnitAttack attack) {
        //noinspection OptionalGetWithoutIsPresent
        SBFFormation target = game.getFormation(attack.getTargetId()).get();
        if (target.getTmm() > 0) {
            toHit.addModifier(target.getTmm(), "TMM");
        }
    }

    private static void processJUMP(SBFToHitData toHit, SBFGame game, SBFStandardUnitAttack attack) {
        //noinspection OptionalGetWithoutIsPresent
        SBFFormation attacker = game.getFormation(attack.getEntityId()).get();
        //noinspection OptionalGetWithoutIsPresent
        SBFFormation target = game.getFormation(attack.getTargetId()).get();
        if (attacker.getJumpUsedThisTurn() > 0) {
            toHit.addModifier(attacker.getJumpUsedThisTurn(), "attacker JUMP");
        }
        if (target.getJumpUsedThisTurn() > 0) {
            toHit.addModifier(attacker.getJumpUsedThisTurn(), "target JUMP");
        }
    }

    private static void processMorale(SBFToHitData toHit, SBFGame game, SBFStandardUnitAttack attack) {
        //noinspection OptionalGetWithoutIsPresent
        SBFFormation target = game.getFormation(attack.getTargetId()).get();
        switch (target.moraleStatus()) {
            case SHAKEN -> toHit.addModifier(-1, "shaken");
            case BROKEN -> toHit.addModifier(-2, "broken");
            case ROUTED -> toHit.addModifier(-3, "routed");
            default -> toHit.doNothing();
        }
    }

    private static void processSecondaryTarget(SBFToHitData toHit, SBFGame game, SBFStandardUnitAttack attack) {
        //noinspection OptionalGetWithoutIsPresent
        SBFFormation attacker = game.getFormation(attack.getEntityId()).get();
        if (targetsOfFormation(attacker, game).size() > 2) {
            toHit.addModifier(TargetRoll.IMPOSSIBLE, "too many targets");
        } else if (targetsOfFormation(attacker, game).size() == 2) {
            toHit.addModifier(1, "two targets");
        }
    }

    /**
     * Returns a list of target IDs of all the targets of all attacks that the attacker of the given attack is
     * performing this round. The result can be empty (the unit isn't attacking anything or it is not the firing phase),
     * it can have one or two entries.
     *
     * @param unit The attacker to check attacks for
     * @param game The game
     *
     * @return A list of all target IDs
     */
    public static List<Integer> targetsOfFormation(InGameObject unit, SBFGame game) {
        return game.getActionsVector().stream()
              .filter(a -> a.getEntityId() == unit.getId())
              .filter(a -> a instanceof SBFAttackAction)
              .map(a -> ((SBFAttackAction) a).getTargetId())
              .distinct()
              .toList();
    }
}
