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

package megamek.common.autoresolve.acar.phase;

import megamek.common.Entity;
import megamek.common.enums.GamePhase;
import megamek.common.strategicBattleSystems.SBFUnit;
import mekhq.campaign.autoresolve.acar.SimulationContext;
import mekhq.campaign.autoresolve.acar.SimulationManager;
import mekhq.campaign.autoresolve.acar.report.VictoryPhaseReporter;
import mekhq.campaign.autoresolve.component.Formation;
import mekhq.campaign.unit.damage.DamageApplierChooser;
import mekhq.campaign.unit.damage.EntityFinalState;

public class VictoryPhase extends PhaseHandler {

    private final VictoryPhaseReporter victoryPhaseReporter;

    public VictoryPhase(SimulationManager gameManager) {
        super(gameManager, GamePhase.VICTORY);
        victoryPhaseReporter = new VictoryPhaseReporter(gameManager.getGame(), gameManager::addReport);
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
