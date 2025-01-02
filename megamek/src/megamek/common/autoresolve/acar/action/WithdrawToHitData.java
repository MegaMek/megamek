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
import mekhq.campaign.autoresolve.component.Formation;
import mekhq.utilities.Internationalization;

public class WithdrawToHitData extends TargetRoll {

    public WithdrawToHitData(int value, String desc) {
        super(value, desc);
    }

    public static WithdrawToHitData compileToHit(SimulationContext game, Formation formation) {
        var toHit = new WithdrawToHitData(formation.getTactics(), Internationalization.getText("acar.formation_tactics"));
        processFormationModifiers(toHit, formation);
        processJumpModifiers(toHit, formation);
        processMorale(toHit, formation);
        processIsCrippled(toHit, formation);
        return toHit;
    }

    private static void processIsCrippled(WithdrawToHitData toHit, Formation formation) {
        if (formation.isCrippled()) {
            toHit.addModifier(1, Internationalization.getText("acar.withdraw.crippled"));
        }
    }

    private static void processJumpModifiers(WithdrawToHitData toHit, Formation formation) {
        toHit.addModifier( 2 - formation.getJumpMove(), Internationalization.getText("acar.jump_modifier"));
    }

    private static void processFormationModifiers(WithdrawToHitData toHit, Formation formation) {
        var formationIsInfantryOnly = formation.isInfantry();
        var formationIsVehicleOnly = formation.isVehicle();

        if (formationIsInfantryOnly) {
            toHit.addModifier(2, Internationalization.getText("acar.formation_is_infantry_only"));
        }
        if (formationIsVehicleOnly) {
            toHit.addModifier(1, Internationalization.getText("acar.formation_is_vehicle_only"));
        }
    }

    private static void processMorale(WithdrawToHitData toHit, Formation formation) {
        switch (formation.moraleStatus()) {
            case SHAKEN -> toHit.addModifier(+1, Internationalization.getText("acar.shaken_morale"));
            case UNSTEADY -> toHit.addModifier(+2, Internationalization.getText("acar.unsteady_morale"));
            case BROKEN -> toHit.addModifier(+3, Internationalization.getText("acar.broken_morale"));
            case ROUTED -> toHit.addModifier(TargetRoll.AUTOMATIC_FAIL, Internationalization.getText("acar.routed_morale"));
        }
    }
}
