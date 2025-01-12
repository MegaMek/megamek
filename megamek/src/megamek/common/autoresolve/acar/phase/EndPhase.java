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

import megamek.common.IEntityRemovalConditions;
import megamek.common.autoresolve.acar.SimulationManager;
import megamek.common.autoresolve.acar.action.MoraleCheckAction;
import megamek.common.autoresolve.acar.action.RecoveringNerveAction;
import megamek.common.autoresolve.acar.action.WithdrawAction;
import megamek.common.autoresolve.acar.report.EndPhaseReporter;
import megamek.common.autoresolve.acar.report.IEndPhaseReporter;
import megamek.common.autoresolve.component.Formation;
import megamek.common.autoresolve.damage.EntityFinalState;
import megamek.common.enums.GamePhase;
import megamek.common.strategicBattleSystems.SBFElementType;
import megamek.common.strategicBattleSystems.SBFFormation;
import megamek.common.strategicBattleSystems.SBFUnit;
import megamek.common.util.weightedMaps.WeightedDoubleMap;

import java.util.List;

public class EndPhase extends PhaseHandler {

    private final IEndPhaseReporter reporter;

    public EndPhase(SimulationManager simulationManager) {
        super(simulationManager, GamePhase.END);
        reporter = EndPhaseReporter.create(simulationManager);
    }

    @Override
    protected void executePhase() {
        reporter.endPhaseHeader();
        checkUnitDestruction();
        checkMorale();
        checkWithdrawingForces();
        checkRecoveringNerves();
        forgetEverything();
    }

    private void checkUnitDestruction() {
        var allFormations = getContext().getActiveDeployedFormations();
        for (var formation : allFormations) {
            var destroyedUnits = formation.getUnits().stream()
                .filter(u -> u.getCurrentArmor() <= 0)
                .toList();
            if (!destroyedUnits.isEmpty()) {
                destroyUnits(formation, destroyedUnits);
            }
        }
    }

    private void checkWithdrawingForces() {
        if (getSimulationManager().isVictory()) {
            // If the game is over, no need to withdraw
            return;
        }
        var forcedWithdrawingUnits = getSimulationManager().getGame().getActiveDeployedFormations().stream()
            .filter(f -> f.moraleStatus() == Formation.MoraleStatus.ROUTED || f.isCrippled())
            .filter(f -> !(f.isType(SBFElementType.BA) || f.isType(SBFElementType.CI)))
            .toList();

        for (var formation : forcedWithdrawingUnits) {
            getSimulationManager().addWithdraw(new WithdrawAction(formation.getId()));
        }
    }

    private void checkMorale() {
        var formationNeedsMoraleCheck = getSimulationManager().getGame().getActiveDeployedFormations().stream()
            .filter(Formation::hadHighStressEpisode)
            .toList();

        for (var formation : formationNeedsMoraleCheck) {
            getSimulationManager().addMoraleCheck(new MoraleCheckAction(formation.getId()), formation);
        }
    }

    private void checkRecoveringNerves() {
        var recoveringNerves = getSimulationManager().getGame().getActiveDeployedFormations().stream()
            .filter(SBFFormation::isDeployed)
            .filter(f -> f.moraleStatus().ordinal() > Formation.MoraleStatus.NORMAL.ordinal())
            .toList();

        for (var formation : recoveringNerves) {
            getSimulationManager().addNerveRecovery(new RecoveringNerveAction(formation.getId()));
        }
    }

    private void forgetEverything() {
        var formations = getSimulationManager().getGame().getActiveFormations();
        formations.forEach(Formation::reset);
    }

    private static final WeightedDoubleMap<Integer> REMOVAL_CONDITIONS_TABLE = WeightedDoubleMap.of(
        IEntityRemovalConditions.REMOVE_SALVAGEABLE, 2,
        IEntityRemovalConditions.REMOVE_DEVASTATED, 5,
        IEntityRemovalConditions.REMOVE_EJECTED, 10
    );

    private static final WeightedDoubleMap<Integer> REMOVAL_CONDITIONS_TABLE_NO_EJECTION = WeightedDoubleMap.of(
        IEntityRemovalConditions.REMOVE_SALVAGEABLE, 4,
        IEntityRemovalConditions.REMOVE_DEVASTATED, 6
    );

    private void destroyUnits(Formation formation, List<SBFUnit> destroyedUnits) {
        for (var unit : destroyedUnits) {
            for (var element : unit.getElements()) {
                var entityOpt = getContext().getEntity(element.getId());
                if (entityOpt.isPresent()) {
                    var entity = entityOpt.get();
                    var removalConditionTable = entity.isEjectionPossible() ?
                        REMOVAL_CONDITIONS_TABLE : REMOVAL_CONDITIONS_TABLE_NO_EJECTION;
                    entity.setRemovalCondition(removalConditionTable.randomItem());

                    reporter.reportUnitDestroyed(entity);
                    getContext().addUnitToGraveyard(entity);
                    getContext().applyDamageToEntityFromUnit(
                        unit, entity, EntityFinalState.fromEntityRemovalState(entity.getRemovalCondition()));
                }
            }

            formation.removeUnit(unit);
            if (formation.getUnits().isEmpty()) {
                getContext().removeFormation(formation);
            }
        }
    }
}
