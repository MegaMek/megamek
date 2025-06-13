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

import megamek.common.TargetRoll;
import megamek.common.autoresolve.acar.SimulationContext;
import megamek.common.autoresolve.component.Formation;
import megamek.common.internationalization.I18n;

public class RecoveringNerveActionToHitData extends TargetRoll {

    public RecoveringNerveActionToHitData(int value, String desc) {
        super(value, desc);
    }

    public static RecoveringNerveActionToHitData compileToHit(SimulationContext game, RecoveringNerveAction recoveringNerveAction) {
        if (recoveringNerveAction.isInvalid(game)) {
            return new RecoveringNerveActionToHitData(TargetRoll.IMPOSSIBLE, I18n.getText("acar.invalid_nerve_recovering"));
        }
        var formation = game.getFormation(recoveringNerveAction.getEntityId()).orElseThrow();
        RecoveringNerveActionToHitData toHit = new RecoveringNerveActionToHitData(3 + formation.getSkill(), I18n.getText("acar.formation_morale"));
        processSkill(toHit, formation);
        return toHit;
    }

    private static void processSkill(RecoveringNerveActionToHitData toHit, Formation formation) {
        switch (formation.getSkill()) {
            case 7 -> toHit.addModifier(+2, I18n.getText("acar.skill_7"));
            case 6 -> toHit.addModifier(+1, I18n.getText("acar.skill_6"));
            case 5 -> toHit.addModifier(0, I18n.getText("acar.skill_5"));
            case 4 -> toHit.addModifier(-1, I18n.getText("acar.skill_4"));
            case 3 -> toHit.addModifier(-2, I18n.getText("acar.skill_3"));
            case 2 -> toHit.addModifier(-3, I18n.getText("acar.skill_2"));
            case 1 -> toHit.addModifier(-4, I18n.getText("acar.skill_1"));
            case 0 -> toHit.addModifier(-5, I18n.getText("acar.skill_0"));
            default -> toHit.addModifier(TargetRoll.IMPOSSIBLE, I18n.getText("acar.invalid_skill"));
        }
    }
}
