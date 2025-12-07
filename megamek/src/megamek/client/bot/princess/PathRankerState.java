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
import megamek.common.compute.Compute;
import megamek.common.equipment.WeaponMounted;
import megamek.common.equipment.WeaponType;
import megamek.common.moves.Key;
import megamek.common.units.Entity;
import megamek.common.units.UnitRole;
import megamek.logging.MMLogger;

/**
 * This class handles state information for Princess' path ranking algorithms, as the path ranker and its subclasses are
 * intended to be basically stateless.
 */
public class PathRankerState {
    private static final MMLogger logger = MMLogger.create(PathRankerState.class);

    // ========== Role-Aware Positioning Constants (easy to tune) ==========

    // Role threat absorption weights - higher = draws more enemy fire
    public static final double THREAT_WEIGHT_JUGGERNAUT = 1.5;
    public static final double THREAT_WEIGHT_BRAWLER = 1.0;
    public static final double THREAT_WEIGHT_SKIRMISHER = 0.8;
    public static final double THREAT_WEIGHT_STRIKER = 0.7;
    public static final double THREAT_WEIGHT_AMBUSHER = 0.6;
    public static final double THREAT_WEIGHT_SCOUT = 0.5;
    public static final double THREAT_WEIGHT_SNIPER = 0.3;
    public static final double THREAT_WEIGHT_MISSILE_BOAT = 0.3;
    public static final double THREAT_WEIGHT_DEFAULT = 1.0;
    public static final double THREAT_WEIGHT_CIVILIAN = 0.0;

    // Armor thresholds for units without roles (total armor points)
    public static final int ARMOR_THRESHOLD_ASSAULT = 200;
    public static final int ARMOR_THRESHOLD_HEAVY = 120;
    public static final int ARMOR_THRESHOLD_MEDIUM = 60;

    // Threat weights for armor-based estimation
    public static final double THREAT_WEIGHT_ARMOR_ASSAULT = 1.3;
    public static final double THREAT_WEIGHT_ARMOR_HEAVY = 1.0;
    public static final double THREAT_WEIGHT_ARMOR_MEDIUM = 0.7;
    public static final double THREAT_WEIGHT_ARMOR_LIGHT = 0.4;

    // Role movement order multipliers - higher = moves earlier in phase
    public static final double MOVE_ORDER_SCOUT = 4.0;
    public static final double MOVE_ORDER_SNIPER = 3.0;
    public static final double MOVE_ORDER_MISSILE_BOAT = 3.0;
    public static final double MOVE_ORDER_SKIRMISHER = 2.0;
    public static final double MOVE_ORDER_STRIKER = 1.5;
    public static final double MOVE_ORDER_AMBUSHER = 1.5;
    public static final double MOVE_ORDER_BRAWLER = 1.0;
    public static final double MOVE_ORDER_JUGGERNAUT = 0.5;
    public static final double MOVE_ORDER_DEFAULT = 1.0;
    public static final double MOVE_ORDER_CIVILIAN = 0.1;

    // Speed thresholds for units without roles (walk MP)
    public static final int SPEED_THRESHOLD_FAST = 7;
    public static final int SPEED_THRESHOLD_MEDIUM = 5;
    public static final int SPEED_THRESHOLD_SLOW = 3;

    // Move order multipliers for speed-based estimation
    public static final double MOVE_ORDER_SPEED_FAST = 3.0;
    public static final double MOVE_ORDER_SPEED_MEDIUM = 1.5;
    public static final double MOVE_ORDER_SPEED_SLOW = 1.0;
    public static final double MOVE_ORDER_SPEED_VERY_SLOW = 0.5;

    // Optimal range calculation constants
    public static final int[] SAMPLE_RANGES = {1, 3, 6, 9, 12, 15, 18, 21, 24, 30};
    public static final int BASELINE_GUNNERY = 4;
    public static final int ROLE_RANGE_MISMATCH_THRESHOLD = 12;

    // ========== End Constants ==========

    private final Map<Key, Double> pathSuccessProbabilities = new HashMap<>();
    private final Map<Coords, Double> incomingFriendlyArtilleryDamage = new HashMap<>();

    // Damage Source Pool: tracks remaining unallocated threat from each enemy
    // Key: enemy entity ID, Value: remaining damage potential (reduces as allies engage)
    private final Map<Integer, Double> damageSourcePool = new HashMap<>();

    // Optimal range cache: entity ID -> optimal engagement range
    private final Map<Integer, Integer> optimalRangeCache = new HashMap<>();

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
        optimalRangeCache.clear();
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
        allocateDamageSource(enemyId, allocationFactor, null, false);
    }

    /**
     * Allocate (reduce) an enemy's threat in the pool after a friendly unit engages them.
     * When role-aware positioning is enabled, applies role-based weight to the allocation.
     *
     * @param enemyId               The entity ID of the enemy being engaged
     * @param allocationFactor      Fraction of threat to remove (0.0 to 1.0)
     * @param allocator             The entity allocating/engaging (for role weight calculation)
     * @param useRoleAwarePositioning Whether to apply role-based weights
     */
    public void allocateDamageSource(int enemyId, double allocationFactor, Entity allocator, boolean useRoleAwarePositioning) {
        Double currentThreat = damageSourcePool.get(enemyId);
        if (currentThreat == null) {
            return;
        }

        // Apply role weight if role-aware positioning is enabled
        double effectiveFactor = allocationFactor;
        if (useRoleAwarePositioning && allocator != null) {
            double roleWeight = getRoleThreatWeight(allocator);
            effectiveFactor *= roleWeight;
            logger.debug("Role-aware allocation: {} weight {} applied to factor {}",
                allocator.getDisplayName(), roleWeight, allocationFactor);
        }

        double reduction = currentThreat * Math.min(effectiveFactor, 1.0);
        double remaining = currentThreat - reduction;

        if (remaining <= 0.01) {
            // Fully allocated - remove from pool
            damageSourcePool.remove(enemyId);
            logger.debug("Damage pool: enemy {} fully allocated, removed from pool", enemyId);
        } else {
            damageSourcePool.put(enemyId, remaining);
            logger.debug("Damage pool: enemy {} reduced {} -> {} (factor {})",
                  enemyId, currentThreat, remaining, effectiveFactor);
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

    // ========== Optimal Range Calculation ==========

    /**
     * Get the optimal engagement range for a unit. Uses cached value if available.
     *
     * @param entity The entity to get optimal range for
     * @return The optimal range in hexes, or Integer.MAX_VALUE for civilians
     */
    public int getOptimalRange(Entity entity) {
        return optimalRangeCache.computeIfAbsent(entity.getId(), id -> calculateOptimalRange(entity));
    }

    /**
     * Calculate the optimal range for a unit based on expected damage output.
     * Takes into account weapon loadout, minimum ranges, and to-hit probabilities.
     *
     * @param entity The entity to calculate for
     * @return Optimal engagement range in hexes
     */
    private int calculateOptimalRange(Entity entity) {
        // Civilians should flee - maximum distance
        if (isCivilian(entity)) {
            return Integer.MAX_VALUE;
        }

        // Calculate optimal range from weapons (expected damage)
        int weaponOptimalRange = calculateOptimalRangeFromWeapons(entity);

        // For units with defined roles, check if role's preferred range makes sense
        UnitRole role = entity.getRole();
        if (role != null && role != UnitRole.UNDETERMINED && role != UnitRole.NONE) {
            int roleRange = getRangeForRole(role);

            // If role and weapons disagree significantly, trust weapons
            // (handles custom variants or misclassified units)
            if (Math.abs(roleRange - weaponOptimalRange) > ROLE_RANGE_MISMATCH_THRESHOLD) {
                logger.debug("{}: Role {} suggests range {}, but weapons suggest {}. Using weapons.",
                    entity.getDisplayName(), role, roleRange, weaponOptimalRange);
                return weaponOptimalRange;
            }

            // Use role's preferred range
            return roleRange;
        }

        // No role or undetermined - use weapon-based calculation
        return weaponOptimalRange;
    }

    /**
     * Calculate optimal range based purely on weapon loadout.
     * Finds the range where expected damage (damage x hit probability) is maximized.
     *
     * @param entity The entity to calculate for
     * @return Optimal range based on weapon analysis
     */
    private int calculateOptimalRangeFromWeapons(Entity entity) {
        List<WeaponMounted> weapons = entity.getWeaponList();
        if (weapons.isEmpty()) {
            return Integer.MAX_VALUE; // No weapons = flee
        }

        int bestRange = SAMPLE_RANGES[0];
        double bestExpectedDamage = 0.0;

        for (int range : SAMPLE_RANGES) {
            double expectedDamage = calculateExpectedDamageAtRange(entity, weapons, range);

            if (expectedDamage > bestExpectedDamage) {
                bestExpectedDamage = expectedDamage;
                bestRange = range;
            }
        }

        logger.debug("{}: Optimal weapon range = {} hexes (expected damage {})",
            entity.getDisplayName(), bestRange, bestExpectedDamage);

        return bestRange;
    }

    /**
     * Calculate total expected damage at a given range for all weapons.
     * Expected damage = sum of (weapon damage x probability to hit)
     *
     * @param entity  The entity
     * @param weapons List of weapons
     * @param range   Range to evaluate
     * @return Total expected damage
     */
    private double calculateExpectedDamageAtRange(Entity entity, List<WeaponMounted> weapons, int range) {
        double totalExpected = 0.0;

        for (WeaponMounted weapon : weapons) {
            if (weapon.isDestroyed() || weapon.isJammed() || !weapon.isReady()) {
                continue;
            }

            WeaponType weaponType = weapon.getType();
            if (weaponType == null) {
                continue;
            }

            // Check if weapon can fire at this range
            int longRange = weaponType.getLongRange();
            int minRange = weaponType.getMinimumRange();

            if (range > longRange) {
                continue; // Out of range
            }

            // Calculate base to-hit number (simplified: range modifiers + gunnery)
            int toHit = BASELINE_GUNNERY;

            // Add range modifier
            if (range <= weaponType.getShortRange()) {
                toHit += 0; // Short range
            } else if (range <= weaponType.getMediumRange()) {
                toHit += 2; // Medium range
            } else {
                toHit += 4; // Long range
            }

            // Add minimum range penalty
            if (minRange > 0 && range <= minRange) {
                toHit += (minRange - range + 1);
            }

            // Get probability (0-100)
            double hitProbability = Compute.oddsAbove(toHit) / 100.0;

            // Get damage
            double damage = weaponType.getDamage(range);
            if (damage == WeaponType.DAMAGE_BY_CLUSTER_TABLE) {
                // Use rack size / 2 as expected hits for cluster weapons
                damage = weaponType.getRackSize() / 2.0;
            } else if (damage <= 0) {
                continue; // Skip weapons with no damage
            }

            totalExpected += damage * hitProbability;
        }

        return totalExpected;
    }

    /**
     * Get the preferred engagement range for a unit role.
     *
     * @param role The unit role
     * @return Preferred range in hexes
     */
    private int getRangeForRole(UnitRole role) {
        if (role == null) {
            return 9; // Default medium range
        }

        return switch (role) {
            case BRAWLER, JUGGERNAUT -> 3;     // Close range fighters
            case STRIKER, AMBUSHER -> 6;       // Short-medium range
            case SKIRMISHER -> 9;               // Medium range, mobile
            case SNIPER, MISSILE_BOAT -> 15;   // Long range
            case SCOUT -> 12;                   // Medium-long, stay back but observe
            default -> 9;                       // Default medium range
        };
    }

    /**
     * Check if an entity should be treated as a civilian (non-combatant).
     *
     * @param entity The entity to check
     * @return true if civilian
     */
    public boolean isCivilian(Entity entity) {
        // No weapons = civilian
        if (entity.getWeaponList().isEmpty()) {
            return true;
        }
        // Could add additional checks here (quirks, designation, etc.)
        return false;
    }

    // ========== Role-Based Threat Weights ==========

    /**
     * Get the threat weight for an entity based on its role.
     * Higher weight = more threat absorbed (e.g., Juggernauts draw fire).
     *
     * @param entity The entity
     * @return Threat weight multiplier
     */
    public double getRoleThreatWeight(Entity entity) {
        // Civilians absorb zero threat
        if (isCivilian(entity)) {
            return THREAT_WEIGHT_CIVILIAN;
        }

        UnitRole role = entity.getRole();

        // Units without roles: estimate from armor
        if (role == null || role == UnitRole.UNDETERMINED || role == UnitRole.NONE) {
            return estimateThreatWeightFromArmor(entity);
        }

        return switch (role) {
            case JUGGERNAUT -> THREAT_WEIGHT_JUGGERNAUT;
            case BRAWLER -> THREAT_WEIGHT_BRAWLER;
            case SKIRMISHER -> THREAT_WEIGHT_SKIRMISHER;
            case STRIKER -> THREAT_WEIGHT_STRIKER;
            case SCOUT -> THREAT_WEIGHT_SCOUT;
            case AMBUSHER -> THREAT_WEIGHT_AMBUSHER;
            case SNIPER -> THREAT_WEIGHT_SNIPER;
            case MISSILE_BOAT -> THREAT_WEIGHT_MISSILE_BOAT;
            default -> THREAT_WEIGHT_DEFAULT;
        };
    }

    /**
     * Estimate threat weight from armor for units without defined roles.
     *
     * @param entity The entity
     * @return Estimated threat weight
     */
    private double estimateThreatWeightFromArmor(Entity entity) {
        int armor = entity.getTotalArmor();
        if (armor > ARMOR_THRESHOLD_ASSAULT) {
            return THREAT_WEIGHT_ARMOR_ASSAULT;
        }
        if (armor > ARMOR_THRESHOLD_HEAVY) {
            return THREAT_WEIGHT_ARMOR_HEAVY;
        }
        if (armor > ARMOR_THRESHOLD_MEDIUM) {
            return THREAT_WEIGHT_ARMOR_MEDIUM;
        }
        return THREAT_WEIGHT_ARMOR_LIGHT;
    }

    // ========== Role-Based Movement Order ==========

    /**
     * Get the movement order multiplier for an entity based on its role.
     * Higher multiplier = moves earlier in the phase.
     *
     * @param entity The entity
     * @return Movement order multiplier
     */
    public double getRoleMoveOrderMultiplier(Entity entity) {
        // Civilians move last
        if (isCivilian(entity)) {
            return MOVE_ORDER_CIVILIAN;
        }

        UnitRole role = entity.getRole();

        // Units without roles: estimate from speed
        if (role == null || role == UnitRole.UNDETERMINED || role == UnitRole.NONE) {
            return estimateMoveOrderFromSpeed(entity);
        }

        return switch (role) {
            case SCOUT -> MOVE_ORDER_SCOUT;
            case SNIPER -> MOVE_ORDER_SNIPER;
            case MISSILE_BOAT -> MOVE_ORDER_MISSILE_BOAT;
            case SKIRMISHER -> MOVE_ORDER_SKIRMISHER;
            case STRIKER -> MOVE_ORDER_STRIKER;
            case AMBUSHER -> MOVE_ORDER_AMBUSHER;
            case BRAWLER -> MOVE_ORDER_BRAWLER;
            case JUGGERNAUT -> MOVE_ORDER_JUGGERNAUT;
            default -> MOVE_ORDER_DEFAULT;
        };
    }

    /**
     * Estimate movement order from speed for units without defined roles.
     * Fast units move early (scouting), slow units move late (anchoring).
     *
     * @param entity The entity
     * @return Estimated movement order multiplier
     */
    private double estimateMoveOrderFromSpeed(Entity entity) {
        int walkMP = entity.getWalkMP();
        if (walkMP >= SPEED_THRESHOLD_FAST) {
            return MOVE_ORDER_SPEED_FAST;
        }
        if (walkMP >= SPEED_THRESHOLD_MEDIUM) {
            return MOVE_ORDER_SPEED_MEDIUM;
        }
        if (walkMP >= SPEED_THRESHOLD_SLOW) {
            return MOVE_ORDER_SPEED_SLOW;
        }
        return MOVE_ORDER_SPEED_VERY_SLOW;
    }

    /**
     * Check if a unit is a long-range optimal unit (sniper/missile boat).
     * Used for asymmetric aggression penalties.
     *
     * @param entity The entity
     * @return true if unit prefers long range
     */
    public boolean isLongRangeOptimal(Entity entity) {
        int optimalRange = getOptimalRange(entity);
        return optimalRange >= 12;
    }
}
