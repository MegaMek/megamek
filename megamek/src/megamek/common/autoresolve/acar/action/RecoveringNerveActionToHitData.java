/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 *  This file is part of MekHQ.
 *
 *  MekHQ is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  MekHQ is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */

package megamek.common.autoresolve.acar.action;

import megamek.common.TargetRoll;
import mekhq.campaign.autoresolve.acar.SimulationContext;
import mekhq.campaign.autoresolve.acar.action.RecoveringNerveAction;
import mekhq.campaign.autoresolve.component.Formation;
import mekhq.utilities.Internationalization;

public class RecoveringNerveActionToHitData extends TargetRoll {

    public RecoveringNerveActionToHitData(int value, String desc) {
        super(value, desc);
    }

    public static RecoveringNerveActionToHitData compileToHit(SimulationContext game, RecoveringNerveAction recoveringNerveAction) {
        if (recoveringNerveAction.isInvalid(game)) {
            return new RecoveringNerveActionToHitData(TargetRoll.IMPOSSIBLE, Internationalization.getText("acar.invalid_nerve_recovering"));
        }
        var formation = game.getFormation(recoveringNerveAction.getEntityId()).orElseThrow();
        RecoveringNerveActionToHitData toHit = new RecoveringNerveActionToHitData(3 + formation.getSkill(), Internationalization.getText("acar.formation_morale"));
        processSkill(toHit, formation);
        return toHit;
    }

    private static void processSkill(RecoveringNerveActionToHitData toHit, Formation formation) {
        switch (formation.getSkill()) {
            case 7 -> toHit.addModifier(+2, Internationalization.getText("acar.skill_7"));
            case 6 -> toHit.addModifier(+1, Internationalization.getText("acar.skill_6"));
            case 5 -> toHit.addModifier(0, Internationalization.getText("acar.skill_5"));
            case 4 -> toHit.addModifier(-1, Internationalization.getText("acar.skill_4"));
            case 3 -> toHit.addModifier(-2, Internationalization.getText("acar.skill_3"));
            case 2 -> toHit.addModifier(-3, Internationalization.getText("acar.skill_2"));
            case 1 -> toHit.addModifier(-4, Internationalization.getText("acar.skill_1"));
            case 0 -> toHit.addModifier(-5, Internationalization.getText("acar.skill_0"));
            default -> toHit.addModifier(TargetRoll.IMPOSSIBLE, Internationalization.getText("acar.invalid_skill"));
        }
    }
}
