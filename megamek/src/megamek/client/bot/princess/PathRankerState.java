/*
 * Copyright (C) 2018-2025 The MegaMek Team. All Rights Reserved.
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import megamek.common.board.Coords;
import megamek.common.moves.Key;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;

/**
 * This class handles state information for Princess' path ranking algorithms, as the path ranker and its subclasses are
 * intended to be basically stateless.
 */
public class PathRankerState {
    private static final MMLogger logger = MMLogger.create(PathRankerState.class);

    private final Map<Key, Double> pathSuccessProbabilities = new HashMap<>();
    private final Map<Coords, Double> incomingFriendlyArtilleryDamage = new HashMap<>();

    // Damage Source Pool: tracks remaining unallocated threat from each enemy
    // Key: enemy entity ID, Value: remaining damage potential (reduces as allies engage)
    private final Map<Integer, Double> damageSourcePool = new HashMap<>();

    /**
     * The map of success probabilities for given move paths. The calculation of a move success probability is pretty
     * complex, so we can cache them here.
     *
     * @return Map of path keys to success probabilities.
     */
    public Map<Key, Double> getPathSuccessProbabilities() {
        return pathSuccessProbabilities;
    }

    /**
     * Map of coordinates to known incoming artillery damage.
     *
     * @return Map of coordinates.
     */
    public Map<Coords, Double> getIncomingFriendlyArtilleryDamage() {
        return incomingFriendlyArtilleryDamage;
    }

    /**
     * Convenience method that clears the current path ranker state.
     */
    public void clearState() {
        pathSuccessProbabilities.clear();
        incomingFriendlyArtilleryDamage.clear();
        damageSourcePool.clear();
    }

    /**
     * Initialize the damage source pool with all enemy units at the start of movement phase. Each enemy's threat is set
     * to their maximum damage potential.
     *
     * @param enemies List of enemy entities
     * @param owner   The Princess bot (used to calculate damage potential)
     */
    public void initializeDamagePool(List<Entity> enemies, Princess owner) {
        damageSourcePool.clear();
        for (Entity enemy : enemies) {
            if (enemy.getPosition() == null || enemy.isOffBoard()) {
                continue;
            }
            double damagePotential = calculateEnemyDamagePotential(enemy, owner);
            damageSourcePool.put(enemy.getId(), damagePotential);
            logger.debug("Damage pool initialized: {} with threat {}", enemy.getDisplayName(), damagePotential);
        }
        logger.info("Damage source pool initialized with {} enemies", damageSourcePool.size());
    }

    /**
     * Calculate an enemy's maximum damage potential. Uses cached damage from FireControl if available, otherwise
     * estimates from weapon range.
     */
    private double calculateEnemyDamagePotential(Entity enemy, Princess owner) {
        // Use FireControl's damage calculation at max range as a reasonable estimate
        int maxRange = owner.getMaxWeaponRange(enemy, false);
        return FireControl.getMaxDamageAtRange(enemy, maxRange, false, false);
    }

    /**
     * Get the remaining threat from an enemy in the damage pool. Returns 0 if the enemy is not in the pool (already
     * fully allocated).
     *
     * @param enemyId The entity ID of the enemy
     *
     * @return Remaining unallocated damage potential
     */
    public double getRemainingThreat(int enemyId) {
        return damageSourcePool.getOrDefault(enemyId, 0.0);
    }

    /**
     * Check if the damage pool has been initialized (has any entries).
     *
     * @return true if the pool contains enemy threat data
     */
    public boolean isDamagePoolInitialized() {
        return !damageSourcePool.isEmpty();
    }

    /**
     * Allocate (reduce) an enemy's threat in the pool after a friendly unit engages them. The allocation factor
     * represents what portion of the enemy's threat is being "handled".
     *
     * @param enemyId          The entity ID of the enemy being engaged
     * @param allocationFactor Fraction of threat to remove (0.0 to 1.0)
     */
    public void allocateDamageSource(int enemyId, double allocationFactor) {
        Double currentThreat = damageSourcePool.get(enemyId);
        if (currentThreat == null) {
            return;
        }

        double reduction = currentThreat * Math.min(allocationFactor, 1.0);
        double remaining = currentThreat - reduction;

        if (remaining <= 0.01) {
            // Fully allocated - remove from pool
            damageSourcePool.remove(enemyId);
            logger.debug("Damage pool: enemy {} fully allocated, removed from pool", enemyId);
        } else {
            damageSourcePool.put(enemyId, remaining);
            logger.debug("Damage pool: enemy {} reduced {} -> {} (factor {})",
                  enemyId, currentThreat, remaining, allocationFactor);
        }
    }

    /**
     * Get the full damage source pool map (for testing/debugging).
     *
     * @return Map of enemy IDs to remaining threat values
     */
    public Map<Integer, Double> getDamageSourcePool() {
        return damageSourcePool;
    }
}
