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
import megamek.common.internationalization.Internationalization;

public class EngagementControlToHitData extends TargetRoll {

    public EngagementControlToHitData(int value, String desc) {
        super(value, desc);
    }

    public static EngagementControlToHitData compileToHit(SimulationContext game, EngagementControlAction engagementControl) {
        if (engagementControl.isInvalid(game)) {
            return new EngagementControlToHitData(TargetRoll.IMPOSSIBLE, Internationalization.getText("acar.invalid_engagement_control"));
        }
        var attackingFormationOpt = game.getFormation(engagementControl.getEntityId());
        if (attackingFormationOpt.isEmpty()) {
            return new EngagementControlToHitData(TargetRoll.IMPOSSIBLE, Internationalization.getText("acar.invalid_attacking_formation"));
        }

        var attackingFormation = attackingFormationOpt.get();
        var toHit = new EngagementControlToHitData(attackingFormation.getTactics(), Internationalization.getText("acar.formation_tactics"));
        processFormationModifiers(toHit, game, engagementControl);
        processMorale(toHit, game, engagementControl);
        processEngagementAndControlChosen(toHit, game, engagementControl);
        processSizeDifference(toHit, game, engagementControl);
        processPlayerSkill(toHit, game, attackingFormation);
        return toHit;
    }

    private static void processEngagementAndControlChosen(
        EngagementControlToHitData toHit, SimulationContext game, EngagementControlAction engagementControl) {
        switch (engagementControl.getEngagementControl()) {
            case FORCED_ENGAGEMENT:
                toHit.addModifier(-3, Internationalization.getText("acar.force_engagement"));
                break;
            case EVADE:
                toHit.addModifier(-3, Internationalization.getText("acar.evade"));
                break;
            case OVERRUN:
                processSizeDifference(toHit, game, engagementControl);
                break;
            default:
                break;
        }
    }

    private static void processPlayerSkill(EngagementControlToHitData toHit, SimulationContext game, Formation formation) {
        switch (game.getPlayerSkill(formation.getOwnerId())) {
            case NONE -> toHit.addModifier(4, Internationalization.getText("acar.skill_7"));
            case ULTRA_GREEN -> toHit.addModifier(3, Internationalization.getText("acar.skill_6"));
            case GREEN -> toHit.addModifier(2, Internationalization.getText("acar.skill_5"));
            case REGULAR -> toHit.addModifier(1, Internationalization.getText("acar.skill_4"));
            case VETERAN -> toHit.addModifier(0, Internationalization.getText("acar.skill_3"));
            case ELITE -> toHit.addModifier(-1, Internationalization.getText("acar.skill_2"));
            case HEROIC -> toHit.addModifier(-2, Internationalization.getText("acar.skill_1"));
            case LEGENDARY -> toHit.addModifier(-3, Internationalization.getText("acar.skill_0"));
        }
    }

    private static void processFormationModifiers(
        EngagementControlToHitData toHit, SimulationContext game, EngagementControlAction engagementControl) {
        var formationOpt = game.getFormation(engagementControl.getEntityId());
        if (formationOpt.isEmpty()) {
            return;
        }
        var formation = formationOpt.get();

        var formationIsInfantryOnly = formation.isInfantry();
        var formationIsVehicleOnly = formation.isVehicle();

        if (formationIsInfantryOnly) {
            toHit.addModifier(2, Internationalization.getText("acar.formation_is_infantry_only"));
        }
        if (formationIsVehicleOnly) {
            toHit.addModifier(1, Internationalization.getText("acar.formation_is_vehicle_only"));
        }
    }

    private static void processSizeDifference(
        EngagementControlToHitData toHit, SimulationContext game, EngagementControlAction engagementControl) {
        var attackerOpt = game.getFormation(engagementControl.getEntityId());
        var targetOpt = game.getFormation(engagementControl.getTargetFormationId());
        if (attackerOpt.isEmpty() || targetOpt.isEmpty()) {
            return;
        }
        int sizeDifference = attackerOpt.get().getSize() - targetOpt.get().getSize();
        toHit.addModifier(sizeDifference, Internationalization.getText("acar.size_difference"));
    }

    private static void processMorale(EngagementControlToHitData toHit, SimulationContext game, EngagementControlAction engagementControl) {
        var targetOpt = game.getFormation(engagementControl.getTargetFormationId());
        if (targetOpt.isEmpty()) {
            return;
        }
        switch (targetOpt.get().moraleStatus()) {
            case SHAKEN -> toHit.addModifier(+1, Internationalization.getText("acar.shaken_morale"));
            case UNSTEADY -> toHit.addModifier(+2, Internationalization.getText("acar.unsteady_morale"));
            case BROKEN -> toHit.addModifier(+3, Internationalization.getText("acar.broken_morale"));
            case ROUTED -> toHit.addModifier(TargetRoll.AUTOMATIC_FAIL, Internationalization.getText("acar.routed_morale"));
        }
    }
}
