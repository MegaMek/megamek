/*
 * Copyright (C) 2025-2026 The MegaMek Team. All Rights Reserved.
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
package megamek.client.bot.princess;

import java.util.ArrayList;
import java.util.List;

import megamek.common.board.Coords;
import megamek.common.compute.MarinePointsScoreCalculator;
import megamek.common.game.Game;
import megamek.common.units.AbstractBuildingEntity;
import megamek.common.units.EjectedCrew;
import megamek.common.units.Entity;
import megamek.common.units.Infantry;

/**
 * Helper class for Princess AI infantry vs infantry combat decisions.
 * Provides calculations and decision logic for initiating, reinforcing, and withdrawing from
 * building/vessel interior combat using the MarinePointsScore system.
 */
public class InfantryCombatHelper {

    // Tunable constants for combat decision-making
    private static final double INITIATION_BASE_THRESHOLD = 2.0;
    private static final double INITIATION_BRAVERY_FACTOR = 0.5;
    private static final double WITHDRAWAL_MULTIPLIER = 0.75;
    private static final double REINFORCEMENT_AGGRESSION_FACTOR = 0.1;
    private static final int MAX_REINFORCEMENT_DISTANCE = 2;
    private static final double CREW_DEFENDER_PERCENTAGE = 0.5;

    /**
     * Calculate the Marine Points Score for an attacking unit.
     *
     * @param entity   The attacking entity (must be Infantry)
     * @param building The target building/vessel
     * @return MPS value for the attacker
     */
    public static int calculateAttackerMPS(Entity entity, Entity building) {
        if (!(entity instanceof Infantry) || !(building instanceof AbstractBuildingEntity)) {
            return 0;
        }
        return MarinePointsScoreCalculator.calculateMPS(entity, (AbstractBuildingEntity) building);
    }

    /**
     * Calculate total enemy defender MPS in a hex.
     * Includes all enemy infantry + 50% of crew as potential defenders.
     *
     * @param game     The current game
     * @param target   The building/vessel being defended
     * @param attacker The attacking entity
     * @return Total estimated defender MPS
     */
    public static int calculateEnemyMPS(Game game, Entity target, Entity attacker) {
        int totalDefenderMPS = 0;

        // Add all enemy infantry in the hex
        List<Entity> enemyInfantry = getEnemyInfantryInHex(
                game, target.getPosition(), attacker.getOwnerId());

        for (Entity enemy : enemyInfantry) {
            if (target instanceof AbstractBuildingEntity) {
                totalDefenderMPS += MarinePointsScoreCalculator.calculateMPS(enemy, (AbstractBuildingEntity) target);
            }
        }

        // Add estimated crew defenders (50%)
        totalDefenderMPS += estimateCrewDefenders(target);

        return totalDefenderMPS;
    }

    /**
     * Calculate MPS ratio (attacker / defender).
     *
     * @param attackerMPS Attacker's MPS
     * @param defenderMPS Defender's MPS
     * @return Ratio, or 0 if defender MPS is 0
     */
    public static double calculateMPSRatio(int attackerMPS, int defenderMPS) {
        if (defenderMPS <= 0) {
            return attackerMPS > 0 ? Double.MAX_VALUE : 0;
        }
        return (double) attackerMPS / (double) defenderMPS;
    }

    /**
     * Calculate initiation threshold based on bravery.
     * Formula: 2.0 - (bravery / 3.0) * 0.5
     * <p>
     * - Low bravery (0.1): ~2.0 (needs 2:1 odds)
     * - Average bravery (1.5): ~1.75 (needs 7:4 odds)
     * - High bravery (3.0): 1.5 (needs 3:2 odds)
     *
     * @param bravery Bravery value from BehaviorSettings (0.1 to 3.0)
     * @return MPS ratio threshold required to initiate
     */
    public static double calculateInitiationThreshold(double bravery) {
        return INITIATION_BASE_THRESHOLD - (bravery / 3.0) * INITIATION_BRAVERY_FACTOR;
    }

    /**
     * Calculate reinforcement target ratio based on aggression.
     * Formula: initiationThreshold + (hyperAggression / 10.0)
     * <p>
     * Princess will reinforce to maintain ratio above this target.
     *
     * @param initiationThreshold The threshold used to initiate combat
     * @param hyperAggression     HyperAggression value from BehaviorSettings (0.25 to 500)
     * @return Target MPS ratio to maintain via reinforcement
     */
    public static double calculateReinforcementTargetRatio(double initiationThreshold,
                                                             double hyperAggression) {
        return initiationThreshold + (hyperAggression * REINFORCEMENT_AGGRESSION_FACTOR);
    }

    /**
     * Calculate withdrawal threshold.
     * Formula: initiationThreshold * 0.75
     * <p>
     * Provides hysteresis - won't withdraw until significantly worse than initiation threshold.
     *
     * @param initiationThreshold The threshold used to initiate combat
     * @return MPS ratio threshold below which to withdraw
     */
    public static double calculateWithdrawalThreshold(double initiationThreshold) {
        return initiationThreshold * WITHDRAWAL_MULTIPLIER;
    }

    /**
     * Determine if Princess should initiate infantry combat.
     *
     * @param attacker The potential attacking infantry
     * @param target   The target building/vessel
     * @param game     The current game
     * @param behavior Princess's behavior settings
     * @return true if should initiate combat
     */
    public static boolean shouldInitiateCombat(Entity attacker, Entity target,
                                                 Game game, BehaviorSettings behavior) {
        // Validate inputs
        if (!(attacker instanceof Infantry) || target == null) {
            return false;
        }

        // Don't initiate if already in combat
        if (attacker.getInfantryCombatTargetId() != Entity.NONE) {
            return false;
        }

        // Don't initiate if fleeing/crippled under forced withdrawal
        if (behavior.isForcedWithdrawal() && attacker.isCrippled()) {
            return false;
        }

        // Calculate MPS and ratio
        int attackerMPS = calculateAttackerMPS(attacker, target);
        int defenderMPS = calculateEnemyMPS(game, target, attacker);

        if (attackerMPS <= 0 || defenderMPS <= 0) {
            return false;
        }

        double ratio = calculateMPSRatio(attackerMPS, defenderMPS);
        double threshold = calculateInitiationThreshold(behavior.getBraveryValue());

        // Initiate if ratio meets or exceeds threshold
        return ratio >= threshold;
    }

    /**
     * Determine if Princess should reinforce existing infantry combat.
     *
     * @param reinforcement The potential reinforcement infantry
     * @param targetId      The building/vessel ID where combat is happening
     * @param game          The current game
     * @param behavior      Princess's behavior settings
     * @return true if should send reinforcement
     */
    public static boolean shouldReinforce(Entity reinforcement, int targetId,
                                           Game game, BehaviorSettings behavior) {
        // Validate inputs
        if (!(reinforcement instanceof Infantry)) {
            return false;
        }

        // Don't reinforce if already in combat
        if (reinforcement.getInfantryCombatTargetId() != Entity.NONE) {
            return false;
        }

        Entity target = game.getEntity(targetId);
        if (!(target instanceof AbstractBuildingEntity)) {
            return false;
        }

        // Find all entities in this combat by checking their infantryCombatTargetId
        List<Entity> attackers = new ArrayList<>();
        List<Entity> defenders = new ArrayList<>();

        for (Entity entity : game.getEntitiesVector()) {
            if (entity.getInfantryCombatTargetId() == targetId) {
                if (entity.isInfantryCombatAttacker()) {
                    attackers.add(entity);
                } else {
                    defenders.add(entity);
                }
            }
        }

        if (attackers.isEmpty() || defenders.isEmpty()) {
            return false; // No active combat
        }

        // Check if on same team as attackers
        Entity firstAttacker = attackers.get(0);
        if (!reinforcement.getOwner().isEnemyOf(firstAttacker.getOwner())) {
            return false;
        }

        // Calculate current MPS ratio
        int attackerMPS = 0;
        for (Entity attacker : attackers) {
            attackerMPS += calculateAttackerMPS(attacker, target);
        }

        int defenderMPS = 0;
        for (Entity defender : defenders) {
            defenderMPS += calculateAttackerMPS(defender, target);
        }

        if (defenderMPS <= 0) {
            return false; // Combat should be over
        }

        double currentRatio = calculateMPSRatio(attackerMPS, defenderMPS);
        double initiationThreshold = calculateInitiationThreshold(behavior.getBraveryValue());
        double targetRatio = calculateReinforcementTargetRatio(
                initiationThreshold, behavior.getHyperAggressionValue());

        // Reinforce if current ratio is below target
        return currentRatio < targetRatio;
    }

    /**
     * Determine if Princess should withdraw from infantry combat.
     *
     * @param entity   The infantry unit in combat
     * @param targetId The building/vessel ID
     * @param game     The current game
     * @param behavior Princess's behavior settings
     * @return true if should withdraw
     */
    public static boolean shouldWithdraw(Entity entity, int targetId,
                                          Game game, BehaviorSettings behavior) {
        // Only attackers can withdraw
        if (!entity.isInfantryCombatAttacker()) {
            return false;
        }

        Entity target = game.getEntity(targetId);
        if (!(target instanceof AbstractBuildingEntity)) {
            return false;
        }

        // Find all entities in this combat by checking their infantryCombatTargetId
        List<Entity> attackers = new ArrayList<>();
        List<Entity> defenders = new ArrayList<>();

        for (Entity e : game.getEntitiesVector()) {
            if (e.getInfantryCombatTargetId() == targetId) {
                if (e.isInfantryCombatAttacker()) {
                    attackers.add(e);
                } else {
                    defenders.add(e);
                }
            }
        }

        if (attackers.isEmpty() || defenders.isEmpty()) {
            return false; // Combat doesn't exist or is over
        }

        // Calculate current MPS ratio
        int attackerMPS = 0;
        for (Entity attacker : attackers) {
            attackerMPS += calculateAttackerMPS(attacker, target);
        }

        int defenderMPS = 0;
        for (Entity defender : defenders) {
            defenderMPS += calculateAttackerMPS(defender, target);
        }

        if (defenderMPS <= 0) {
            return false; // Winning, don't withdraw
        }

        double currentRatio = calculateMPSRatio(attackerMPS, defenderMPS);
        double initiationThreshold = calculateInitiationThreshold(behavior.getBraveryValue());
        double withdrawalThreshold = calculateWithdrawalThreshold(initiationThreshold);

        // Withdraw if ratio drops below withdrawal threshold
        return currentRatio < withdrawalThreshold;
    }

    /**
     * Get all enemy infantry in a hex.
     *
     * @param game    The current game
     * @param coords  The coordinates to check
     * @param ownerId The owner ID of the attacking player
     * @return List of enemy infantry entities
     */
    public static List<Entity> getEnemyInfantryInHex(Game game, Coords coords, int ownerId) {
        List<Entity> enemyInfantry = new ArrayList<>();

        for (Entity entity : game.getEntitiesVector(coords)) {
            if (entity instanceof Infantry &&
                entity.getOwnerId() != ownerId &&
                !(entity instanceof EjectedCrew) &&
                entity.isInBuilding()) {
                enemyInfantry.add(entity);
            }
        }

        return enemyInfantry;
    }

    /**
     * Estimate crew defenders (50% of total crew).
     * Uses 50% of the entity's crew size as potential defenders.
     *
     * @param target The target building/vessel
     * @return Estimated number of crew defenders
     */
    public static int estimateCrewDefenders(Entity target) {
        if (target.getCrew() != null) {
            return (int) Math.ceil(target.getCrew().getSize() * CREW_DEFENDER_PERCENTAGE);
        }

        return 0;
    }

    /**
     * Find the best infantry unit to send as reinforcement.
     * Priority: MPS/mass ratio, proximity, availability.
     *
     * @param candidates List of candidate units
     * @param target     The target building/vessel
     * @param game       The current game
     * @return Best reinforcement unit, or null if none suitable
     */
    public static Entity findBestReinforcement(List<Entity> candidates,
                                                 Entity target, Game game) {
        Entity best = null;
        double bestScore = Double.NEGATIVE_INFINITY;

        for (Entity candidate : candidates) {
            // Skip if not infantry or already in combat
            if (!(candidate instanceof Infantry) ||
                candidate.getInfantryCombatTargetId() != Entity.NONE) {
                continue;
            }

            // Calculate score: MPS / distance
            int mps = calculateAttackerMPS(candidate, target);
            int distance = candidate.getPosition().distance(target.getPosition());

            if (distance > MAX_REINFORCEMENT_DISTANCE) {
                continue; // Too far
            }

            // Avoid division by zero, closer is better
            double score = mps / (double) (distance + 1);

            if (score > bestScore) {
                bestScore = score;
                best = candidate;
            }
        }

        return best;
    }
}
