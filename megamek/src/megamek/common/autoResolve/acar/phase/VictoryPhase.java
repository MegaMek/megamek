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

import megamek.common.autoResolve.acar.SimulationContext;
import megamek.common.autoResolve.acar.SimulationManager;
import megamek.common.autoResolve.acar.report.IVictoryPhaseReporter;
import megamek.common.autoResolve.acar.report.VictoryPhaseReporter;
import megamek.common.autoResolve.component.Formation;
import megamek.common.autoResolve.damage.DamageApplierChooser;
import megamek.common.autoResolve.damage.EntityFinalState;
import megamek.common.enums.GamePhase;
import megamek.common.interfaces.IEntityRemovalConditions;
import megamek.common.strategicBattleSystems.SBFUnit;
import megamek.common.units.Entity;
import megamek.common.units.IAero;

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

        for (var entity : context.getRetreatingUnits()) {
            DamageApplierChooser.damageRemovedEntity(entity, IEntityRemovalConditions.REMOVE_IN_RETREAT);
        }
    }

    private static void applyDamageToEntitiesFromFormation(SimulationContext context, Formation formation) {
        for (var unit : formation.getUnits()) {
            if (unit.getCurrentArmor() < unit.getArmor()) {
                applyDamageToEntitiesFromUnit(context, unit);
            }
        }
    }

    private static void applyDamageToEntitiesFromUnit(SimulationContext context, SBFUnit unit) {
        for (var element : unit.getElements()) {
            var entityOpt = context.getEntity(element.getId());
            entityOpt.ifPresent(entity -> applyDamageToEntityFromUnit(unit, entity));
        }
    }

    private static void applyDamageToEntityFromUnit(SBFUnit unit, Entity entity) {
        double percent = (double) unit.getCurrentArmor() / unit.getArmor();
        var crits = Math.min(9, unit.getTargetingCrits() + unit.getMpCrits() + unit.getDamageCrits());
        percent -= percent * (crits / 11.0);
        percent = Math.min(0.95, percent);
        int armor = Math.max(entity.getTotalArmor(), 0);
        int internal = (entity instanceof IAero) ? ((IAero) entity).getSI() : entity.getTotalInternal();
        int totalDamage = (int) ((armor + internal) * (1.0 - percent));
        DamageApplierChooser.choose(entity, EntityFinalState.CREW_AND_ENTITY_MUST_SURVIVE)
              .applyDamageInClusters(totalDamage, -1);
    }
}
