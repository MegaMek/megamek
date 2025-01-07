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
package megamek.common.autoresolve.acar.phase;

import megamek.common.Entity;
import megamek.common.IEntityRemovalConditions;
import megamek.common.autoresolve.acar.SimulationContext;
import megamek.common.autoresolve.acar.SimulationManager;
import megamek.common.autoresolve.acar.report.IVictoryPhaseReporter;
import megamek.common.autoresolve.acar.report.VictoryPhaseReporter;
import megamek.common.autoresolve.component.Formation;
import megamek.common.autoresolve.damage.DamageApplierChooser;
import megamek.common.autoresolve.damage.EntityFinalState;
import megamek.common.enums.GamePhase;
import megamek.common.strategicBattleSystems.SBFUnit;

public class VictoryPhase extends PhaseHandler {

    private final IVictoryPhaseReporter victoryPhaseReporter;

    public VictoryPhase(SimulationManager gameManager) {
        super(gameManager, GamePhase.VICTORY);
        victoryPhaseReporter = VictoryPhaseReporter.create(gameManager);
    }

    @Override
    protected void executePhase() {
        applyDamageToRemainingUnits(getContext());
        victoryPhaseReporter.victoryHeader();
        victoryPhaseReporter.victoryResult(getSimulationManager());
    }

    private static void applyDamageToRemainingUnits(SimulationContext context) {
        for (var formation : context.getActiveFormations()) {
            applyDamageToEntitiesFromFormation(context, formation);
        }

        for (var inGameObject : context.getGraveyard()) {
            if (inGameObject instanceof Entity entity) {
                DamageApplierChooser.damageRemovedEntity(entity, entity.getRemovalCondition());
            }
        }

        for (var entity : context.getRetreatingUnits()) {
            DamageApplierChooser.damageRemovedEntity(entity, IEntityRemovalConditions.REMOVE_IN_RETREAT);
        }
    }

    private static void applyDamageToEntitiesFromFormation(SimulationContext context, Formation formation) {
        for ( var unit : formation.getUnits()) {
            if (unit.getCurrentArmor() < unit.getArmor()) {
                applyDamageToEntitiesFromUnit(context, unit);
            }
        }
    }

    private static void applyDamageToEntitiesFromUnit(SimulationContext context, SBFUnit unit) {
        for (var element : unit.getElements()) {
            var entityOpt = context.getEntity(element.getId());
            if (entityOpt.isPresent()) {
                var entity = entityOpt.get();
                applyDamageToEntityFromUnit(unit, entity);
            }
        }
    }

    private static void applyDamageToEntityFromUnit(SBFUnit unit, Entity entity) {
        var percent = (double) unit.getCurrentArmor() / unit.getArmor();
        var crits = Math.min(9, unit.getTargetingCrits() + unit.getMpCrits() + unit.getDamageCrits());
        percent -= percent * (crits / 11.0);
        percent = Math.min(0.95, percent);
        var totalDamage = (int) ((entity.getTotalArmor() + entity.getTotalInternal()) * (1 - percent));
        DamageApplierChooser.choose(entity, EntityFinalState.CREW_AND_ENTITY_MUST_SURVIVE)
            .applyDamageInClusters(totalDamage, 5);
    }
}
