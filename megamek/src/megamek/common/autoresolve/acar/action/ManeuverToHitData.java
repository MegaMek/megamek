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

package megamek.common.autoresolve.acar.action;

import megamek.common.rolls.TargetRoll;
import megamek.common.autoresolve.component.EngagementControl;
import megamek.common.autoresolve.component.Formation;
import megamek.common.internationalization.I18n;

public class ManeuverToHitData extends TargetRoll {

    public ManeuverToHitData(int value, String desc) {
        super(value, desc);
    }

    public static ManeuverToHitData compileToHit(Formation formation) {
        var toHit = new ManeuverToHitData(formation.getTactics(), I18n.getText("acar.formation_tactics"));
        processFormationModifiers(toHit, formation);
        processCombatUnit(toHit, formation);
        return toHit;
    }

    private static void processFormationModifiers(ManeuverToHitData toHit, Formation formation) {
        if (formation.getEngagementControl() == EngagementControl.FORCED_ENGAGEMENT) {
            toHit.addModifier(1, I18n.getText("acar.forced_engagement"));
        }
        if (formation.isAerospace()) {
            toHit.addModifier(2, I18n.getText("acar.aerospace_formation"));
        }
    }

    private static void processCombatUnit(ManeuverToHitData toHit, Formation formation) {
        switch (formation.getSkill()) {
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

        switch (formation.moraleStatus()) {
            case SHAKEN -> toHit.addModifier(+0, I18n.getText("acar.shaken_morale"));
            case UNSTEADY -> toHit.addModifier(+1, I18n.getText("acar.unsteady_morale"));
            case BROKEN -> toHit.addModifier(+2, I18n.getText("acar.broken_morale"));
            case ROUTED -> toHit.addModifier(+2, I18n.getText("acar.routed_morale"));
            default -> toHit.doNothing();
        }
    }
}
