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

package megamek.common.autoResolve.acar.phase;

import java.util.List;
import java.util.stream.Collectors;

import megamek.common.autoResolve.acar.SimulationManager;
import megamek.common.autoResolve.acar.action.MoraleCheckAction;
import megamek.common.autoResolve.acar.action.RecoveringNerveAction;
import megamek.common.autoResolve.acar.action.WithdrawAction;
import megamek.common.autoResolve.acar.order.OrderType;
import megamek.common.autoResolve.acar.report.EndPhaseReporter;
import megamek.common.autoResolve.acar.report.IEndPhaseReporter;
import megamek.common.autoResolve.component.Formation;
import megamek.common.autoResolve.damage.EntityFinalState;
import megamek.common.enums.GamePhase;
import megamek.common.interfaces.IEntityRemovalConditions;
import megamek.common.strategicBattleSystems.SBFElementType;
import megamek.common.strategicBattleSystems.SBFFormation;
import megamek.common.strategicBattleSystems.SBFUnit;
import megamek.common.util.weightedMaps.WeightedDoubleMap;

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
              .collect(Collectors.toSet());

        for (var order : getContext().getOrders()) {
            if (order.isEligible(getContext())) {
                if (order.getOrderType().equals(OrderType.WITHDRAW_IF_CONDITION_IS_MET)) {
                    var formations = getContext().getActiveFormations(order.getOwnerId());
                    forcedWithdrawingUnits.addAll(formations);
                    order.setConcluded(true);
                } else if (order.getOrderType().equals(OrderType.FLEE_NORTH)) {
                    var formations = getContext().getActiveFormations(order.getOwnerId());
                    forcedWithdrawingUnits.addAll(formations);
                    order.setConcluded(true);
                } else if (order.getOrderType().equals(OrderType.FLEE_SOUTH)) {
                    var formations = getContext().getActiveFormations(order.getOwnerId());
                    forcedWithdrawingUnits.addAll(formations);
                    order.setConcluded(true);
                }
            }
        }

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
            if (!formation.isSingleEntity()) {
                reporter.reportUnitDestroyed(formation, unit);
            }
            for (var element : unit.getElements()) {
                var entityOpt = getContext().getEntity(element.getId());
                if (entityOpt.isPresent()) {
                    var entity = entityOpt.get();
                    var removalConditionTable = entity.isEjectionPossible() ?
                          REMOVAL_CONDITIONS_TABLE : REMOVAL_CONDITIONS_TABLE_NO_EJECTION;
                    entity.setRemovalCondition(removalConditionTable.randomItem());
                    if (formation.isSingleEntity()) {
                        reporter.reportElementDestroyed(formation, unit, entity);
                    }
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
