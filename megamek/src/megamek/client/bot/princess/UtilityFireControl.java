/*
 * Copyright (c) 2019-2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */

package megamek.client.bot.princess;

import megamek.common.*;
import megamek.common.annotations.Nullable;
import megamek.common.equipment.AmmoMounted;
import megamek.common.equipment.WeaponMounted;
import megamek.common.options.OptionsConstants;
import megamek.common.weapons.Weapon;
import megamek.logging.MMLogger;

import java.util.*;

import static megamek.client.bot.princess.UtilityPathRanker.clamp01;

/**
 * Princess-Bot fire control class used to calculate firing plans for units that
 * can shoot at multiple targets without incurring a penalty.
 *
 * @author NickAragua
 *
 */
public class UtilityFireControl extends FireControl {
    private final static MMLogger logger = MMLogger.create(UtilityFireControl.class);

    public UtilityFireControl(Princess owningPrincess) {
        super(owningPrincess);
    }

    /**
     * Calculates the best firing plan for a particular entity, assuming that
     * everybody has already moved.
     * Assumes no restriction on number of units that may be targeted.
     */
    @Override
    public FiringPlan getBestFiringPlan(final Entity shooter,
            final IHonorUtil honorUtil,
            final Game game,
            final Map<WeaponMounted, Double> ammoConservation) {
        FiringPlan bestPlan = null;

        // Get a list of potential targets.
        final List<Targetable> enemies = getTargetableEnemyEntities(shooter, game, owner.getFireControlState());

        // Loop through each enemy and find the best plan for attacking them.
        for (final Targetable enemy : enemies) {
            if (owner.getBehaviorSettings().getIgnoredUnitTargets().contains(enemy.getId())) {
                logger.info(enemy.getDisplayName() + " is being explicitly ignored");
                continue;
            }

            final int playerId = enemy.getOwnerId();
            final boolean priorityTarget = owner.getPriorityUnitTargets().contains(enemy.getId());
            final boolean isEnemyBroken = honorUtil.isEnemyBroken(enemy.getId(), playerId, owner.getForcedWithdrawal());
            // Only skip retreating enemies that are not priority targets so long as they haven't fired on me while retreating.
            if (!priorityTarget && isEnemyBroken) {
                logger.info(enemy.getDisplayName() + " is broken and not priority - ignoring");
                continue;
            }

            final FiringPlanCalculationParameters parameters = new FiringPlanCalculationParameters.Builder().buildExact(
                shooter,
                enemy,
                ammoConservation);
            final FiringPlan plan = determineBestFiringPlan(parameters);

            int swarmTargetCount = owner.getSwarmContext().getEnemyTargetCount(plan.getTarget().getId());
            double focusFireBonus = 1.0 + (swarmTargetCount * 0.1); // 10% per targeting unit
            plan.setUtility(plan.getUtility() * focusFireBonus);

            if ((bestPlan == null)
                || (plan.getUtility() > bestPlan.getUtility())) {
                bestPlan = plan;
            }
        }
        if (bestPlan != null) {
            owner.getSwarmContext().recordEnemyTarget(bestPlan.getTarget().getId());
        }
        // Return the best overall plan.
        return bestPlan;
    }

    void calculateUtilityUsingUtilityForReal(final FiringPlan firingPlan,
                          final int overheatTolerance,
                          final boolean shooterIsAero) {
        int overheat = 0;
        if (firingPlan.getHeat() > overheatTolerance) {
            overheat = firingPlan.getHeat() - overheatTolerance;
        }

        double modifier = 0.5;
        modifier += calcCommandUtility(firingPlan.getTarget());
        modifier += calcStrategicBuildingTargetUtility(firingPlan.getTarget());
        modifier += (calcPriorityUnitTargetUtility(firingPlan.getTarget()) * 2); // priority target currently is 0.25, this makes it 0.5
        modifier = clamp01(modifier);

        double expectedDamage = firingPlan.getExpectedDamage();
        double utility = 1.0;
        utility *= modifier;
        utility *= (firingPlan.getExpectedCriticals() + 1);
        utility *= clamp01(firingPlan.getKillProbability() + 0.5);
        // Multiply the combined damage/crit/kill utility for a target by a log-scaled
        // factor based on the target's damage potential.
        utility *= calcTargetPotentialDamageMultiplier(firingPlan.getTarget());
        utility *= calcDamageAllocationUtility(firingPlan.getTarget(), expectedDamage);
        utility *= aresConvention(firingPlan.getTarget());

        utility *= overheatUtility(overheat);
        firingPlan.setUtility(clamp01(utility));
    }

    protected double overheatUtility(final int overheat) {
        double selfPreservationFactor = owner.getBehaviorSettings().getSelfPreservationIndex() / 10.0;
        return clamp01(1.0 - overheat * selfPreservationFactor);
    }

    @Override
    protected double calcCivilianTargetDisutility(final Targetable target) {
        if (owner.getHonorUtil().iAmAPirate()) {
            return 0;
        }
        return super.calcCivilianTargetDisutility(target);
    }

    protected double aresConvention(final Targetable target) {
        if (owner.getHonorUtil().iAmAPirate()) {
            return 1;
        }
        if (!(target instanceof Entity entity)) {
            return 1;
        }
        if (entity instanceof MekWarrior) {
            return 0;
        }
        if (entity.isMilitary()) {
            return 1;
        }
        if (owner.getPriorityUnitTargets().contains(entity.getId())) {
            return 5; // we really want to kill priority targets
        }
        if (owner.getHonorUtil().isEnemyDishonored(entity.getOwnerId())) {
            return 1;
        }
        return 0;
    }

}
