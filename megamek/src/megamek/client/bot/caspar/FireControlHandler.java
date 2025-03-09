/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 2 or (at your option) any later version,
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
 */
package megamek.client.bot.caspar;

import megamek.client.bot.common.GameState;
import megamek.client.bot.princess.FiringPlan;
import megamek.codeUtilities.MathUtility;
import megamek.common.Compute;
import megamek.common.Entity;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Handles firing decisions for CASPAR AI units.
 * Determines target selection, weapon allocation, and special attack usage.
 */
public class FireControlHandler {
    private final DifficultyManager difficultyManager;

    private record UnitId(int id) { }
    private final Map<UnitId, Integer> enemyTargetCounts = new HashMap<>();
    private final Set<UnitId> enemyUnits = new HashSet<>();
    /**
     * Record an enemy target, incrementing the number of units targeting the enemy this turn
     * @param enemyId The enemy id
     */
    public void recordEnemyTarget(int enemyId) {
        enemyTargetCounts.put(new UnitId(enemyId), enemyTargetCounts.getOrDefault(new UnitId(enemyId), 0) + 1);
    }

    /**
     * Get the number of times an enemy has been targeted
     * @param unit The target unit
     * @return The number of times the enemy has been targeted
     */
    public int getEnemyTargetCount(Entity unit) {
        return getEnemyTargetCount(unit.getId());
    }

    private int getEnemyTargetCount(int unitId) {
        return getEnemyTargetCount(new UnitId(unitId));
    }

    private int getEnemyTargetCount(UnitId unitId) {
        return enemyTargetCounts.getOrDefault(unitId, 0);
    }
    /**
     * Reset the enemy target counts
     */
    public void resetEnemyTargets() {
        enemyTargetCounts.clear();
    }


    /**
     * Creates a fire control handler with the specified difficulty manager.
     *
     * @param difficultyManager The difficulty manager to use
     */
    public FireControlHandler(DifficultyManager difficultyManager) {
        this.difficultyManager = difficultyManager;
    }

    /**
     * Determines the firing solution for a unit.
     *
     * @param unit            The unit to fire with
     * @param possibleTargets List of possible targets
     * @param gameState       The current game state
     * @return The selected firing solution
     */
    public FiringPlan determineFiringPlan(Entity unit, List<Entity> possibleTargets, GameState gameState) {
        if (possibleTargets.isEmpty()) {
            return new FiringPlan();
        }

        // Calculate target scores
        Map<Entity, Double> targetScores = possibleTargets.stream()
              .collect(Collectors.toMap(
                    target -> target,
                    target -> calculateTargetScore(unit, target)
              ));

        // Sort targets by score
        List<Entity> sortedTargets = possibleTargets.stream()
              .sorted(Comparator.comparingDouble(targetScores::get).reversed())
              .toList();

        return new FiringPlan(sortedTargets.get(0));
    }

    private double calculateTargetScore(Entity unit, Entity target) {
        if (cantBeTargeted(unit, target)) {
            return 0d;
        }

        return getPriorityModifier(target) * switch (difficultyManager.getDifficultyLevel()) {
            case BEGINNER -> maxTargetsModifier(target, 1) * calculateTargetScoreClosest(unit, target);
            case EASY -> maxTargetsModifier(target, 2) * calculateTargetScoreClosest(unit, target);
            case MEDIUM -> maxTargetsModifier(target, 3) * calculateTargetScoreEnemyBV(target);
            case HARD -> calculateTargetScoreDamageCaused(unit, target) * calculateTargetScoreEnemyBV(target);
            case HARDCORE -> (getEnemyTargetCount(target) + 1) * calculateTargetScoreDamageCaused(unit, target) * calculateTargetScoreEnemyBV(target);
        };
    }

    private boolean cantBeTargeted(Entity unit, Entity target) {
        int distance = unit.getPosition().distance(target.getPosition());
        return distance > unit.getMaxWeaponRange();
    }

    private double getPriorityModifier(Entity target) {
        var unitId = new UnitId(target.getId());
        if (enemyUnits.contains(unitId)) {
            return 100d;
        }
        return 1d;
    }

    /**
     * Modifies the target score based on the number of targets already selected.
     * @param target The target to evaluate
     * @param maxTargets The maximum number of units targeting the target that are acceptable
     * @return The target score modifier
     */
    private double maxTargetsModifier(Entity target, int maxTargets) {
        if (getEnemyTargetCount(target) >= maxTargets) {
            return 0d;
        }
        return 1d;
    }

    /**
     * Calculates the target score for a beginner difficulty level.
     * It will pick the closest enemy that has not been targeted yet.
     * @param unit The unit firing
     * @param target The target to evaluate
     * @return The target score
     */
    private double calculateTargetScoreClosest(Entity unit, Entity target) {
        return unit.getMaxWeaponRange() - unit.getPosition().distance(target.getPosition());
    }

    /**
     * Calculates the amount of damage that the unit can cause to the target relative to the target's health.
     * @param unit The unit firing
     * @param target The target to evaluate
     * @return The target score
     */
    private double calculateTargetScoreDamageCaused(Entity unit, Entity target) {
        int totalHealth = Math.max(target.getTotalArmor() + target.getTotalInternal(), 0);
        int totalDamage = Compute.computeTotalDamage(unit.getWeaponList());
        return MathUtility.clamp01(totalDamage / (totalHealth + 1d));
    }

    /**
     * Calculates the target score using its BV.
     * @param target The target to evaluate
     * @return The target score
     */
    private double calculateTargetScoreEnemyBV(Entity target) {
        return target.getInitialBV();
    }
}
