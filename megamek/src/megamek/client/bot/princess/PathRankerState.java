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

import megamek.common.alphaStrike.ASDamageVector;
import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.alphaStrike.BattleForceSUA;
import megamek.common.alphaStrike.conversion.ASConverter;
import megamek.common.board.Coords;
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

    // Optimal range target hexes (midpoints of Alpha Strike range brackets)
    public static final int OPTIMAL_RANGE_MELEE = 1;    // Adjacent hex for physical attacks
    public static final int OPTIMAL_RANGE_SHORT = 3;    // 0-6 hexes
    public static final int OPTIMAL_RANGE_MEDIUM = 12;  // 7-24 hexes
    public static final int OPTIMAL_RANGE_LONG = 21;    // 25-42 hexes

    // Gunnery skill thresholds for range adjustment
    public static final int GUNNERY_ELITE = 3;    // Gunnery 2-3: can afford longer range
    public static final int GUNNERY_STANDARD = 4; // Gunnery 4: baseline
    public static final int GUNNERY_GREEN = 5;    // Gunnery 5+: needs to close in

    // Damage threshold for range changes - prevents oscillation on minor damage drops
    // A 1-point swing (e.g., 5->4) won't trigger repositioning; requires 2+ point difference
    public static final int DAMAGE_THRESHOLD = 2;

    // Melee threat multipliers by role - higher = more dangerous at close range
    // Used to penalize being at melee range (1 hex) of melee-focused enemies
    public static final double MELEE_THREAT_BRAWLER = 1.0;
    public static final double MELEE_THREAT_JUGGERNAUT = 1.0;
    public static final double MELEE_THREAT_STRIKER = 0.5;
    public static final double MELEE_THREAT_SKIRMISHER = 0.5;
    public static final double MELEE_THREAT_AMBUSHER = 0.5;
    public static final double MELEE_THREAT_SNIPER = 0.2;
    public static final double MELEE_THREAT_MISSILE_BOAT = 0.2;
    public static final double MELEE_THREAT_SCOUT = 0.2;
    public static final double MELEE_THREAT_DEFAULT = 0.5;
    public static final double MELEE_THREAT_MEL_BONUS = 50.0;  // Bonus penalty for units with MEL ability
    public static final double MELEE_THREAT_BASE_PENALTY = 50.0;  // Base penalty at melee range

    // ========== End Constants ==========

    private final Map<Key, Double> pathSuccessProbabilities = new HashMap<>();
    private final Map<Coords, Double> incomingFriendlyArtilleryDamage = new HashMap<>();

    // Damage Source Pool: tracks remaining unallocated threat from each enemy
    // Key: enemy entity ID, Value: remaining damage potential (reduces as allies engage)
    private final Map<Integer, Double> damageSourcePool = new HashMap<>();

    // Optimal range cache: entity ID -> optimal engagement range
    private final Map<Integer, Integer> optimalRangeCache = new HashMap<>();

    // Alpha Strike damage cache: entity ID -> AS damage vector (S/M/L values)
    // Populated once at phase start to reflect current weapon/ammo state
    private final Map<Integer, ASDamageVector> asDamageCache = new HashMap<>();

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
        asDamageCache.clear();
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
     * Initialize Alpha Strike damage cache for all units at the start of movement phase.
     * This performs the full AS conversion once per unit, reflecting current weapon/ammo state.
     * Must be called before getOptimalRange() to get accurate values for damaged units.
     *
     * @param allUnits List of all entities (friendly and enemy) to cache AS values for
     */
    public void initializeASDamageCache(List<Entity> allUnits) {
        asDamageCache.clear();
        optimalRangeCache.clear();  // Clear optimal range cache too - will be recalculated from fresh AS values

        for (Entity entity : allUnits) {
            if (entity.isDestroyed() || entity.isOffBoard()) {
                continue;
            }
            try {
                AlphaStrikeElement element = ASConverter.convert(entity);
                ASDamageVector damage = element.getStandardDamage();
                if (damage != null) {
                    asDamageCache.put(entity.getId(), damage);
                    logger.debug("AS damage cached for {}: S={} M={} L={}",
                        entity.getDisplayName(),
                        damage.S().damage + (damage.S().minimal ? "*" : ""),
                        damage.M().damage + (damage.M().minimal ? "*" : ""),
                        damage.L().damage + (damage.L().minimal ? "*" : ""));
                }
            } catch (Exception e) {
                logger.warn("Failed to convert {} to Alpha Strike: {}", entity.getDisplayName(), e.getMessage());
            }
        }
        logger.info("AS damage cache initialized for {} units", asDamageCache.size());
    }

    /**
     * Get cached AS damage vector for an entity.
     * Returns null if not cached (call initializeASDamageCache first).
     *
     * @param entityId The entity ID
     * @return Cached ASDamageVector, or null if not found
     */
    public ASDamageVector getCachedASDamage(int entityId) {
        return asDamageCache.get(entityId);
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
     * Uses weapon loadout analysis (expected damage at each range bracket) to find
     * where the unit deals maximum damage.
     *
     * @param entity The entity to calculate for
     * @return Optimal engagement range in hexes
     */
    private int calculateOptimalRange(Entity entity) {
        // Always calculate from actual weapon loadout for accuracy
        return calculateOptimalRangeFromWeapons(entity);
    }

    /**
     * Calculate optimal range using Alpha Strike damage values and gunnery skill.
     * Returns the range bracket (S/M/L) where the unit deals highest expected damage,
     * adjusted for the pilot's gunnery skill.
     *
     * Uses cached AS damage values if available (from initializeASDamageCache),
     * otherwise falls back to fresh AS conversion.
     *
     * @param entity The entity to calculate for
     * @return Optimal range in hexes (3, 12, or 21)
     */
    private int calculateOptimalRangeFromWeapons(Entity entity) {
        // No weapons = flee (basic check before AS conversion)
        if (entity.getWeaponList().isEmpty()) {
            return Integer.MAX_VALUE;
        }

        // Try to get Alpha Strike element for damage values
        AlphaStrikeElement element;
        try {
            element = ASConverter.convert(entity);
        } catch (Exception e) {
            // ASConverter can fail on mock entities in tests - return flee
            return Integer.MAX_VALUE;
        }

        if (element == null) {
            return Integer.MAX_VALUE;
        }

        boolean hasMEL = element.hasSUA(BattleForceSUA.MEL);

        // Try to get cached AS damage values first (populated at phase start)
        ASDamageVector damage = asDamageCache.get(entity.getId());

        // Fall back to fresh conversion if not cached
        if (damage == null) {
            damage = element.getStandardDamage();
        }

        // Get damage values (minimal damage counts as having some damage value)
        int sDamage = (damage != null) ? damage.S().damage + (damage.S().minimal ? 1 : 0) : 0;
        int mDamage = (damage != null) ? damage.M().damage + (damage.M().minimal ? 1 : 0) : 0;
        int lDamage = (damage != null) ? damage.L().damage + (damage.L().minimal ? 1 : 0) : 0;

        // MEL (Melee) ability: Add physical attack damage to short range
        // In Alpha Strike, melee damage = Size + 1 (for MEL bonus)
        // This makes dedicated melee units want to close to engagement range
        if (hasMEL) {
            int melDamage = element.getSize() + 1;  // Size (1-4) + MEL bonus
            sDamage += melDamage;
            logger.info("{}: MEL ability adds {} damage to short range (Size {} + 1)",
                entity.getDisplayName(), melDamage, element.getSize());
        }

        // No damage capability at all
        if (sDamage == 0 && mDamage == 0 && lDamage == 0) {
            return Integer.MAX_VALUE;
        }

        // Get gunnery skill (default to 4 if no crew)
        int gunnery = GUNNERY_STANDARD;
        if (entity.getCrew() != null) {
            gunnery = entity.getCrew().getGunnery();
        }

        // Log for debugging
        logger.info("{}: AS damage S={} M={} L={}, Gunnery={}, MEL={}",
            entity.getDisplayName(), sDamage, mDamage, lDamage, gunnery, hasMEL);

        // Determine base optimal range from damage profile
        int baseRange = determineBaseOptimalRange(sDamage, mDamage, lDamage);

        // Adjust for gunnery skill
        int adjustedRange = adjustRangeForGunnery(baseRange, gunnery, sDamage, mDamage, lDamage);

        // MEL units with short-range optimal should close to melee range (1 hex)
        // Otherwise they'll stop at 3 hexes and never use their melee weapons
        if (hasMEL && adjustedRange == OPTIMAL_RANGE_SHORT) {
            adjustedRange = OPTIMAL_RANGE_MELEE;
            logger.info("{}: MEL unit - adjusting optimal from {} to {} (melee range)",
                entity.getDisplayName(), OPTIMAL_RANGE_SHORT, OPTIMAL_RANGE_MELEE);
        }

        logger.info("{}: Base range={}, Adjusted range={} hexes (Gunnery {})",
            entity.getDisplayName(), baseRange, adjustedRange, gunnery);

        return adjustedRange;
    }

    /**
     * Determine the base optimal range from damage values alone.
     * For flat profiles (S=M=L), returns MEDIUM as a safe default.
     * Uses DAMAGE_THRESHOLD to prevent oscillation on minor damage changes.
     */
    private int determineBaseOptimalRange(int sDamage, int mDamage, int lDamage) {
        // Flat damage profile - prefer medium range (safe positioning, same damage)
        if (sDamage == mDamage && mDamage == lDamage) {
            return OPTIMAL_RANGE_MEDIUM;
        }

        // Clear winner at long range (must be significantly better than both M and S)
        if (lDamage > mDamage + DAMAGE_THRESHOLD && lDamage > sDamage + DAMAGE_THRESHOLD) {
            return OPTIMAL_RANGE_LONG;
        }

        // Medium is significantly better than short
        if (mDamage > sDamage + DAMAGE_THRESHOLD) {
            return OPTIMAL_RANGE_MEDIUM;
        }

        // Short range dominant, or close call - stay aggressive
        return OPTIMAL_RANGE_SHORT;
    }

    /**
     * Adjust optimal range based on gunnery skill.
     * Elite gunners can afford longer range, green gunners need to close in.
     * Only shifts if the longer range has BETTER damage (not just equal).
     */
    private int adjustRangeForGunnery(int baseRange, int gunnery, int sDamage, int mDamage, int lDamage) {
        // Elite gunners (2-3): shift UP one bracket if damage is BETTER at longer range
        if (gunnery <= GUNNERY_ELITE) {
            if (baseRange == OPTIMAL_RANGE_SHORT && mDamage > sDamage) {
                return OPTIMAL_RANGE_MEDIUM;
            } else if (baseRange == OPTIMAL_RANGE_MEDIUM && lDamage > mDamage) {
                return OPTIMAL_RANGE_LONG;
            }
        }

        // Green gunners (5+): shift DOWN one bracket if damage is at least equal at closer range
        if (gunnery >= GUNNERY_GREEN) {
            if (baseRange == OPTIMAL_RANGE_LONG && mDamage >= lDamage) {
                return OPTIMAL_RANGE_MEDIUM;
            } else if (baseRange == OPTIMAL_RANGE_MEDIUM && sDamage >= mDamage) {
                return OPTIMAL_RANGE_SHORT;
            }
        }

        // Standard gunnery (4) or no adjustment needed
        return baseRange;
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

    // ========== Melee Threat Assessment ==========

    /**
     * Get the melee threat multiplier for an entity based on its role.
     * Higher multiplier = more dangerous at close range.
     *
     * @param entity The enemy entity
     * @return Melee threat multiplier (0.2 to 1.0)
     */
    public double getMeleeThreatMultiplier(Entity entity) {
        UnitRole role = entity.getRole();
        if (role == null || role == UnitRole.UNDETERMINED || role == UnitRole.NONE) {
            return MELEE_THREAT_DEFAULT;
        }

        return switch (role) {
            case BRAWLER -> MELEE_THREAT_BRAWLER;
            case JUGGERNAUT -> MELEE_THREAT_JUGGERNAUT;
            case STRIKER -> MELEE_THREAT_STRIKER;
            case SKIRMISHER -> MELEE_THREAT_SKIRMISHER;
            case AMBUSHER -> MELEE_THREAT_AMBUSHER;
            case SNIPER -> MELEE_THREAT_SNIPER;
            case MISSILE_BOAT -> MELEE_THREAT_MISSILE_BOAT;
            case SCOUT -> MELEE_THREAT_SCOUT;
            default -> MELEE_THREAT_DEFAULT;
        };
    }

    /**
     * Check if an entity has the MEL (Melee) special ability.
     * Uses cached AS data if available.
     *
     * @param entity The entity to check
     * @return true if the entity has MEL
     */
    public boolean hasEnemyMEL(Entity entity) {
        try {
            AlphaStrikeElement element = ASConverter.convert(entity);
            return element != null && element.hasSUA(BattleForceSUA.MEL);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Calculate the melee threat penalty for being at melee range of an enemy.
     * Returns 0 if not at melee range (distance > 1).
     *
     * @param enemy The enemy entity
     * @param distance Distance from our position to the enemy
     * @return Penalty value to add to threat assessment
     */
    public double calculateMeleeThreatPenalty(Entity enemy, int distance) {
        if (distance > 1) {
            return 0.0;  // Only applies at melee range (adjacent hexes)
        }

        double roleMultiplier = getMeleeThreatMultiplier(enemy);
        double melBonus = hasEnemyMEL(enemy) ? MELEE_THREAT_MEL_BONUS : 0.0;

        double penalty = (MELEE_THREAT_BASE_PENALTY * roleMultiplier) + melBonus;

        logger.debug("Melee threat penalty for {} at distance {}: {} (role mult: {}, MEL bonus: {})",
            enemy.getDisplayName(), distance, penalty, roleMultiplier, melBonus);

        return penalty;
    }

    // ========== Static Calculation Methods for TSV Logging ==========
    // These methods allow UnitAction/UnitState to calculate role-aware values
    // without needing a PathRankerState instance.

    /**
     * Calculate the optimal engagement range for an entity based on weapon loadout.
     * Uses expected damage calculation (damage x probability to hit) at each range bracket
     * to find where the unit deals maximum damage.
     * Static version for use by TSV logging (UnitAction/UnitState).
     *
     * @param entity The entity to calculate for
     * @return Optimal engagement range in hexes, or Integer.MAX_VALUE for civilians/unarmed
     */
    public static int calculateOptimalRangeForEntity(Entity entity) {
        // Always calculate from actual weapon loadout for accuracy
        return calculateWeaponOptimalRange(entity);
    }

    /**
     * Calculate threat weight for an entity based on its role.
     * Static version for use by TSV logging (UnitAction/UnitState).
     * Higher weight = more threat absorbed (e.g., Juggernauts draw fire).
     *
     * @param entity The entity
     * @return Threat weight multiplier
     */
    public static double calculateThreatWeightForEntity(Entity entity) {
        // Civilians absorb zero threat
        if (isEntityCivilian(entity)) {
            return THREAT_WEIGHT_CIVILIAN;
        }

        UnitRole role = entity.getRole();

        // Units without roles: estimate from armor
        if (role == null || role == UnitRole.UNDETERMINED || role == UnitRole.NONE) {
            return estimateArmorThreatWeight(entity);
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
     * Calculate movement order multiplier for an entity based on its role.
     * Static version for use by TSV logging (UnitAction/UnitState).
     * Higher multiplier = moves earlier in the phase.
     *
     * @param entity The entity
     * @return Movement order multiplier
     */
    public static double calculateMoveOrderMultiplierForEntity(Entity entity) {
        // Civilians move last
        if (isEntityCivilian(entity)) {
            return MOVE_ORDER_CIVILIAN;
        }

        UnitRole role = entity.getRole();

        // Units without roles: estimate from speed
        if (role == null || role == UnitRole.UNDETERMINED || role == UnitRole.NONE) {
            return estimateSpeedMoveOrder(entity);
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

    // ========== Static Helper Methods ==========

    /**
     * Static version of civilian check for use by static methods.
     * Units with MEL (dedicated melee weapons) are not considered civilians even without ranged weapons.
     */
    private static boolean isEntityCivilian(Entity entity) {
        if (!entity.getWeaponList().isEmpty()) {
            return false;  // Has ranged weapons
        }
        // Check for MEL - dedicated melee units without ranged weapons are still combatants
        try {
            AlphaStrikeElement element = ASConverter.convert(entity);
            if (element != null && element.hasSUA(BattleForceSUA.MEL)) {
                return false;  // Has dedicated melee weapons
            }
        } catch (Exception e) {
            // ASConverter can fail on mock entities in tests - treat as civilian
        }
        return true;  // No ranged weapons and no MEL = civilian
    }

    /**
     * Calculate optimal range using Alpha Strike damage values and gunnery skill.
     * Static version for use by TSV logging (UnitAction/UnitState).
     *
     * @param entity The entity to calculate for
     * @return Optimal range in hexes (3, 12, or 21), or Integer.MAX_VALUE for unarmed
     */
    private static int calculateWeaponOptimalRange(Entity entity) {
        // No weapons = flee (basic check before AS conversion)
        if (entity.getWeaponList().isEmpty()) {
            return Integer.MAX_VALUE;
        }

        // Try to get Alpha Strike element for damage values
        AlphaStrikeElement element;
        try {
            element = ASConverter.convert(entity);
        } catch (Exception e) {
            // ASConverter can fail on mock entities in tests - return flee
            return Integer.MAX_VALUE;
        }

        if (element == null) {
            return Integer.MAX_VALUE;
        }

        boolean hasMEL = element.hasSUA(BattleForceSUA.MEL);

        // Get Alpha Strike damage values
        ASDamageVector damage = element.getStandardDamage();

        // Get damage values (minimal damage counts as having some damage value)
        int sDamage = (damage != null) ? damage.S().damage + (damage.S().minimal ? 1 : 0) : 0;
        int mDamage = (damage != null) ? damage.M().damage + (damage.M().minimal ? 1 : 0) : 0;
        int lDamage = (damage != null) ? damage.L().damage + (damage.L().minimal ? 1 : 0) : 0;

        // MEL (Melee) ability: Add physical attack damage to short range
        // In Alpha Strike, melee damage = Size + 1 (for MEL bonus)
        // This makes dedicated melee units want to close to engagement range
        if (hasMEL) {
            int melDamage = element.getSize() + 1;  // Size (1-4) + MEL bonus
            sDamage += melDamage;
        }

        // No damage capability at all
        if (sDamage == 0 && mDamage == 0 && lDamage == 0) {
            return Integer.MAX_VALUE;
        }

        // Get gunnery skill (default to 4 if no crew)
        int gunnery = GUNNERY_STANDARD;
        if (entity.getCrew() != null) {
            gunnery = entity.getCrew().getGunnery();
        }

        // Determine base optimal range from damage profile
        int baseRange = determineBaseOptimalRangeStatic(sDamage, mDamage, lDamage);

        // Adjust for gunnery skill
        int adjustedRange = adjustRangeForGunneryStatic(baseRange, gunnery, sDamage, mDamage, lDamage);

        // MEL units with short-range optimal should close to melee range (1 hex)
        // Otherwise they'll stop at 3 hexes and never use their melee weapons
        if (hasMEL && adjustedRange == OPTIMAL_RANGE_SHORT) {
            adjustedRange = OPTIMAL_RANGE_MELEE;
        }

        return adjustedRange;
    }

    /**
     * Determine the base optimal range from damage values alone.
     * Static version for use by static methods.
     * Uses DAMAGE_THRESHOLD to prevent oscillation on minor damage changes.
     */
    private static int determineBaseOptimalRangeStatic(int sDamage, int mDamage, int lDamage) {
        // Flat damage profile - prefer medium range (safe positioning, same damage)
        if (sDamage == mDamage && mDamage == lDamage) {
            return OPTIMAL_RANGE_MEDIUM;
        }

        // Clear winner at long range (must be significantly better than both M and S)
        if (lDamage > mDamage + DAMAGE_THRESHOLD && lDamage > sDamage + DAMAGE_THRESHOLD) {
            return OPTIMAL_RANGE_LONG;
        }

        // Medium is significantly better than short
        if (mDamage > sDamage + DAMAGE_THRESHOLD) {
            return OPTIMAL_RANGE_MEDIUM;
        }

        // Short range dominant, or close call - stay aggressive
        return OPTIMAL_RANGE_SHORT;
    }

    /**
     * Adjust optimal range based on gunnery skill.
     * Static version for use by static methods.
     * Only shifts if the longer range has BETTER damage (not just equal).
     */
    private static int adjustRangeForGunneryStatic(int baseRange, int gunnery, int sDamage, int mDamage, int lDamage) {
        // Elite gunners (2-3): shift UP one bracket if damage is BETTER at longer range
        if (gunnery <= GUNNERY_ELITE) {
            if (baseRange == OPTIMAL_RANGE_SHORT && mDamage > sDamage) {
                return OPTIMAL_RANGE_MEDIUM;
            } else if (baseRange == OPTIMAL_RANGE_MEDIUM && lDamage > mDamage) {
                return OPTIMAL_RANGE_LONG;
            }
        }

        // Green gunners (5+): shift DOWN one bracket if damage is at least equal at closer range
        if (gunnery >= GUNNERY_GREEN) {
            if (baseRange == OPTIMAL_RANGE_LONG && mDamage >= lDamage) {
                return OPTIMAL_RANGE_MEDIUM;
            } else if (baseRange == OPTIMAL_RANGE_MEDIUM && sDamage >= mDamage) {
                return OPTIMAL_RANGE_SHORT;
            }
        }

        // Standard gunnery (4) or no adjustment needed
        return baseRange;
    }

    /**
     * Estimate threat weight from armor for units without defined roles.
     * Static version for use by static methods.
     */
    private static double estimateArmorThreatWeight(Entity entity) {
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

    /**
     * Estimate movement order from speed for units without defined roles.
     * Static version for use by static methods.
     */
    private static double estimateSpeedMoveOrder(Entity entity) {
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
}
