/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.common.strategicBattleSystems;

import megamek.common.TargetRoll;
import megamek.common.TargetRollModifier;
import megamek.common.actions.EntityAction;

public class SBFToHitData extends TargetRoll {

    //TODO subclass this AttackEnv, ActionEnv, ArtyEnv? only attacks use Unit
    public record ActionEnvironment(SBFGame game, SBFFormation attackingFormation, SBFUnit attackingUnit,
                                    SBFFormation target, EntityAction action) { }

    public SBFToHitData(int value, String desc) {
        super(value, desc);
    }

    public SBFToHitData() {
    }

    public static SBFToHitData compiletoHit(ActionEnvironment data) {
        SBFToHitData toHit = new SBFToHitData(data.attackingUnit.getSkill(), "Skill");
        if (!processRange(toHit, data)) {
            return toHit;
        }
        return toHit;
    }

    private static boolean processRange(SBFToHitData toHit, ActionEnvironment data) {
        SBFFormation attackingFormation = data.attackingFormation;
        SBFFormation target = data.target;

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
}
