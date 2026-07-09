/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
package megamek.common.analysis;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import megamek.common.RangeType;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.compute.ComputeArc;
import megamek.common.equipment.AmmoMounted;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.WeaponMounted;
import megamek.common.equipment.WeaponType;
import megamek.common.units.Entity;
import megamek.common.units.Infantry;
import megamek.common.weapons.bayWeapons.BayWeapon;
import megamek.common.weapons.infantry.InfantryWeapon;

/**
 * An immutable per-unit damage-versus-range curve, computed from the unit's actual weapons and
 * loaded ammunition.
 *
 * <p>Three curves are exposed, indexed by range in hexes:</p>
 * <ul>
 *   <li>{@link #maxDamage(int)} - every functioning weapon that reaches the range, no to-hit
 *       weighting. Cluster weapons count their full rack.</li>
 *   <li>{@link #expectedDamage(int)} - each weapon's damage weighted by its 2d6 hit probability at
 *       that range (gunnery + range bracket modifier + minimum range penalty). Cluster weapons use
 *       the expected value of the cluster hits table instead of the full rack.</li>
 *   <li>{@link #sustainedDamage(int)} - the best heat-sustainable subset of weapons by expected
 *       damage: weapons are added in expected-damage-per-heat order until the unit's heat
 *       dissipation is spent. Equal to the expected curve for units that do not track heat.</li>
 * </ul>
 *
 * <p>The curves are unit properties, not situation properties: no terrain, movement, or target
 * modifiers are included - those belong to fire control at attack time. The gunnery skill baked
 * into the expected and sustained curves is the crew's actual gunnery (4 if the unit has no crew).
 * Ammunition is chosen per range by best expected damage, mirroring how a player would load for
 * the engagement. Weapon facing is ignored; the curve is the unit's best case in any direction.</p>
 *
 * <p>Infantry and battle armor follow their TW attack mechanics: a conventional platoon's small
 * arms deal ceil(per-trooper damage x shooting troopers) as one attack over the R/2R/3R infantry
 * brackets; a battle armor squad's direct-fire weapons roll the cluster table on the number of
 * firing suits, and its missile racks pool across suits into one larger cluster roll. Crew-served
 * field guns and squad support weapons fire once. ProtoMeks and all other unit types resolve as
 * individually mounted weapons.</p>
 *
 * <p>Known approximations: artillery is treated as a direct-fire weapon dealing its rack size;
 * cluster-roll bonuses from fire-control equipment (Artemis, Apollo) are not applied; infantry
 * use the standard bracket to-hit modifiers rather than the point-blank special cases.</p>
 *
 * <p>Build cost is O(weapons x maxRange) with small constants; instances are cheap enough to
 * rebuild once per game phase. Consumers that query per candidate path should cache the instance
 * per entity (see the CASPAR bot's phase-start caches for the intended lifecycle).</p>
 */
public final class DamageProfile {

    /** To-hit modifier for the medium range bracket (TW p.303). */
    private static final int BRACKET_MOD_MEDIUM = 2;
    /** To-hit modifier for the long range bracket (TW p.303). */
    private static final int BRACKET_MOD_LONG = 4;
    /** To-hit modifier for the extreme range bracket (TO:AR p.85). */
    private static final int BRACKET_MOD_EXTREME = 6;
    /** Gunnery skill assumed when the unit has no crew. */
    private static final int DEFAULT_GUNNERY = 4;

    /** Probability numerators (out of 36) for 2d6 rolls 2..12, used for cluster expectation. */
    private static final int[] TWO_D6_WEIGHTS = { 1, 2, 3, 4, 5, 6, 5, 4, 3, 2, 1 };

    /**
     * Expected cluster hits by rack size, shared across profiles (the table never changes).
     * Concurrent because profiles may be built from bot threads and the EDT at the same time.
     */
    private static final Map<Integer, Double> EXPECTED_CLUSTER_HITS_CACHE = new ConcurrentHashMap<>();

    /** The number of hex-side directions a unit can be attacked from or fire toward. */
    public static final int DIRECTIONS = 6;

    /**
     * Range bands for the per-direction bracket averages (inclusive hex ranges): short 1-7,
     * medium 8-14, long 15+.
     */
    private static final int SHORT_BAND_END = 7;
    private static final int MEDIUM_BAND_END = 14;

    /** Distance of the synthetic target used to test whether a weapon arc covers a direction. */
    private static final int ARC_PROBE_DISTANCE = 6;

    private final double[] maxDamageByRange;
    private final double[] expectedDamageByRange;
    private final double[] sustainedDamageByRange;
    private final ArcSummary[] arcSummaries;
    private final int gunnery;
    private final boolean containsCapitalWeapons;

    private DamageProfile(double[] maxDamageByRange, double[] expectedDamageByRange,
          double[] sustainedDamageByRange, ArcSummary[] arcSummaries, int gunnery,
          boolean containsCapitalWeapons) {
        this.maxDamageByRange = maxDamageByRange;
        this.expectedDamageByRange = expectedDamageByRange;
        this.sustainedDamageByRange = sustainedDamageByRange;
        this.arcSummaries = arcSummaries;
        this.gunnery = gunnery;
        this.containsCapitalWeapons = containsCapitalWeapons;
    }

    /**
     * Per-direction firepower summary for one of the six hex-side directions relative to the
     * unit's facing (0 = front, counting clockwise: 1 = front-right, 2 = rear-right, 3 = rear,
     * 4 = rear-left, 5 = front-left). Only weapons whose firing arc covers the direction
     * contribute; torso twist and turret rotation are not applied - arcs are as mounted.
     *
     * @param maximumAverage     mean of the maximum-damage curve over the direction's reach
     * @param shortRangeAverage  mean expected damage over ranges 1-7
     * @param mediumRangeAverage mean expected damage over ranges 8-14
     * @param longRangeAverage   mean expected damage over ranges 15 to the direction's reach
     * @param reach              longest range any bearing weapon covers in this direction
     */
    public record ArcSummary(double maximumAverage, double shortRangeAverage,
          double mediumRangeAverage, double longRangeAverage, int reach) {

        static final ArcSummary EMPTY = new ArcSummary(0, 0, 0, 0, 0);
    }

    /**
     * Builds the damage profile for a unit from its current weapon and ammunition state, using the
     * crew's gunnery skill (4 if the unit has no crew). Destroyed and out-of-ammo weapons
     * contribute nothing, so a profile built mid-game reflects battle damage as of when it was
     * built.
     *
     * @param entity          the unit to profile
     * @param useExtremeRange whether the TacOps extreme-range rules are in effect
     *
     * @return the unit's damage profile; never {@code null}, possibly empty (see {@link #hasWeapons()})
     */
    public static DamageProfile of(Entity entity, boolean useExtremeRange) {
        int crewGunnery = (entity.getCrew() != null) ? entity.getCrew().getGunnery() : DEFAULT_GUNNERY;
        return of(entity, useExtremeRange, crewGunnery);
    }

    /**
     * Builds the damage profile for a unit at an explicit gunnery skill instead of the crew's,
     * for what-if displays (e.g. the unit viewer's BV gunnery field).
     *
     * @param entity          the unit to profile
     * @param useExtremeRange whether the TacOps extreme-range rules are in effect
     * @param gunnery         the gunnery skill for the expected and sustained curves
     *
     * @return the unit's damage profile; never {@code null}, possibly empty (see {@link #hasWeapons()})
     */
    public static DamageProfile of(Entity entity, boolean useExtremeRange, int gunnery) {

        List<WeaponContribution> weapons = new ArrayList<>();
        int maxRange = 0;
        for (WeaponMounted weapon : entity.getWeaponList()) {
            if (weapon.isCrippled()) {
                continue;
            }
            // Large craft (DropShips, JumpShips, WarShips, Space Stations, Small Craft) list their
            // weapon bays here; the bay type itself has no damage or range. Expand each bay into
            // its member weapons, all firing through the bay's arc.
            if (weapon.getType() instanceof BayWeapon) {
                boolean[] bayBearing = WeaponContribution.computeBearing(entity, weapon);
                for (WeaponMounted bayMember : weapon.getBayWeapons()) {
                    if (bayMember.isCrippled()) {
                        continue;
                    }
                    WeaponContribution contribution = WeaponContribution.of(entity, bayMember, gunnery,
                          useExtremeRange, bayBearing);
                    if (contribution != null) {
                        weapons.add(contribution);
                        maxRange = Math.max(maxRange, contribution.maxRange());
                    }
                }
                continue;
            }
            WeaponContribution contribution = WeaponContribution.of(entity, weapon, gunnery, useExtremeRange);
            if (contribution != null) {
                weapons.add(contribution);
                maxRange = Math.max(maxRange, contribution.maxRange());
            }
        }

        double[] maxDamage = new double[maxRange + 1];
        double[] expectedDamage = new double[maxRange + 1];
        double[] sustainedDamage = new double[maxRange + 1];
        double[][] directionalMax = new double[DIRECTIONS][maxRange + 1];
        double[][] directionalExpected = new double[DIRECTIONS][maxRange + 1];
        int[] directionalReach = new int[DIRECTIONS];

        boolean tracksHeat = entity.tracksHeat();
        int heatCapacity = entity.getHeatCapacity();

        for (int range = 1; range <= maxRange; range++) {
            double maxAtRange = 0;
            double expectedAtRange = 0;
            List<WeaponAtRange> firing = new ArrayList<>(weapons.size());
            for (WeaponContribution weapon : weapons) {
                WeaponAtRange atRange = weapon.atRange(range);
                if (atRange != null) {
                    maxAtRange += atRange.maxDamage();
                    expectedAtRange += atRange.expectedDamage();
                    firing.add(atRange);
                    for (int direction = 0; direction < DIRECTIONS; direction++) {
                        if (weapon.bearsOn(direction)) {
                            directionalMax[direction][range] += atRange.maxDamage();
                            directionalExpected[direction][range] += atRange.expectedDamage();
                            directionalReach[direction] = Math.max(directionalReach[direction], range);
                        }
                    }
                }
            }
            maxDamage[range] = maxAtRange;
            expectedDamage[range] = expectedAtRange;
            sustainedDamage[range] = tracksHeat
                  ? bestSustainedDamage(firing, heatCapacity)
                  : expectedAtRange;
        }

        // Range 0 (same hex) mirrors range 1: the bracket rules treat both as point-blank.
        if (maxRange >= 1) {
            maxDamage[0] = maxDamage[1];
            expectedDamage[0] = expectedDamage[1];
            sustainedDamage[0] = sustainedDamage[1];
        }

        ArcSummary[] arcSummaries = new ArcSummary[DIRECTIONS];
        for (int direction = 0; direction < DIRECTIONS; direction++) {
            arcSummaries[direction] = summarizeArc(directionalMax[direction],
                  directionalExpected[direction], directionalReach[direction]);
        }

        boolean containsCapitalWeapons = false;
        for (WeaponContribution weapon : weapons) {
            WeaponType weaponType = weapon.weapon().getType();
            if (weaponType.isCapital() || weaponType.isSubCapital()) {
                containsCapitalWeapons = true;
                break;
            }
        }

        return new DamageProfile(maxDamage, expectedDamage, sustainedDamage, arcSummaries, gunnery,
              containsCapitalWeapons);
    }

    /**
     * @return {@code true} if any contributing weapon is capital or sub-capital scale. The curves are
     *       always in standard damage points (capital converts at x10); displays may use this to
     *       relabel their axes in capital scale for naval reading.
     */
    public boolean hasCapitalScaleWeapons() {
        return containsCapitalWeapons;
    }

    private static ArcSummary summarizeArc(double[] maxCurve, double[] expectedCurve, int reach) {
        if (reach < 1) {
            return ArcSummary.EMPTY;
        }
        return new ArcSummary(
              bandAverage(maxCurve, 1, reach),
              bandAverage(expectedCurve, 1, Math.min(SHORT_BAND_END, reach)),
              bandAverage(expectedCurve, SHORT_BAND_END + 1, Math.min(MEDIUM_BAND_END, reach)),
              bandAverage(expectedCurve, MEDIUM_BAND_END + 1, reach),
              reach);
    }

    /** Mean of a curve over an inclusive range band; 0 when the band is empty. */
    private static double bandAverage(double[] curve, int firstRange, int lastRange) {
        if (lastRange < firstRange) {
            return 0;
        }
        double total = 0;
        for (int range = firstRange; range <= lastRange; range++) {
            total += curve[range];
        }
        return total / (lastRange - firstRange + 1);
    }

    /**
     * @param direction the hex-side direction relative to the unit's facing: 0 = front, clockwise
     *                  (1 = front-right, 2 = rear-right, 3 = rear, 4 = rear-left, 5 = front-left)
     *
     * @return the firepower summary for that direction; an all-zero summary if no weapon bears
     */
    public ArcSummary arcSummary(int direction) {
        return arcSummaries[((direction % DIRECTIONS) + DIRECTIONS) % DIRECTIONS];
    }

    /**
     * Greedy heat allocation: fire weapons in expected-damage-per-heat order until dissipation is
     * spent. Heat-free weapons always fire. Greedy is not an exact knapsack solution, but it matches
     * how players alpha-judge and is deterministic.
     */
    private static double bestSustainedDamage(List<WeaponAtRange> firing, int heatCapacity) {
        firing.sort((a, b) -> Double.compare(b.damagePerHeat(), a.damagePerHeat()));
        double sustained = 0;
        int heatBudget = heatCapacity;
        for (WeaponAtRange weapon : firing) {
            if (weapon.heat() <= 0) {
                sustained += weapon.expectedDamage();
            } else if (weapon.heat() <= heatBudget) {
                sustained += weapon.expectedDamage();
                heatBudget -= weapon.heat();
            }
        }
        return sustained;
    }

    /**
     * @param range the range in hexes (0 is treated as point-blank, i.e. range 1)
     *
     * @return the total damage of every functioning weapon that reaches this range, unweighted
     */
    public double maxDamage(int range) {
        return curveValue(maxDamageByRange, range);
    }

    /**
     * @param range the range in hexes (0 is treated as point-blank, i.e. range 1)
     *
     * @return the to-hit-weighted damage at this range, using the crew's gunnery
     */
    public double expectedDamage(int range) {
        return curveValue(expectedDamageByRange, range);
    }

    /**
     * @param range the range in hexes (0 is treated as point-blank, i.e. range 1)
     *
     * @return the to-hit-weighted damage of the best heat-sustainable weapon subset at this range
     */
    public double sustainedDamage(int range) {
        return curveValue(sustainedDamageByRange, range);
    }

    private static double curveValue(double[] curve, int range) {
        if ((range < 0) || (range >= curve.length)) {
            return 0;
        }
        return curve[range];
    }

    /**
     * @return the longest range at which any functioning weapon can deal damage, under the range
     *       rules the profile was built with; 0 if the unit has no usable weapons
     */
    public int maxRange() {
        return maxDamageByRange.length - 1;
    }

    /**
     * @return {@code true} if the unit had at least one functioning weapon with a nonzero damage curve when
     *       the profile was built. Replaces sentinel values - consumers decide what "no weapons"
     *       means for them.
     */
    public boolean hasWeapons() {
        return maxDamageByRange.length > 1;
    }

    /**
     * The range where the expected-damage curve peaks: the unit's optimal engagement range. Ties go
     * to the longer range, since equal damage from farther away is strictly safer.
     *
     * @return the optimal range in hexes, or 0 if the unit has no usable weapons
     */
    public int peakExpectedRange() {
        int bestRange = 0;
        double bestDamage = 0;
        for (int range = 1; range < expectedDamageByRange.length; range++) {
            if (expectedDamageByRange[range] >= bestDamage) {
                bestDamage = expectedDamageByRange[range];
                bestRange = range;
            }
        }
        return bestRange;
    }

    /**
     * @return the peak of the expected-damage curve; the unit's threat at its best range. 0 if the
     *       unit has no usable weapons.
     */
    public double peakExpectedDamage() {
        double best = 0;
        for (int range = 1; range < expectedDamageByRange.length; range++) {
            best = Math.max(best, expectedDamageByRange[range]);
        }
        return best;
    }

    /**
     * @return the gunnery skill baked into the expected and sustained curves
     */
    public int gunnery() {
        return gunnery;
    }

    /**
     * The expected number of hits from the cluster hits table for a rack of the given size: the
     * probability-weighted average over all 2d6 rolls. Unlike the common "roll a 7" shortcut, this
     * is the true expectation.
     *
     * <p>Sizes without an exact table row - pooled battle armor racks can exceed the table's
     * largest entry - decompose into the largest available row plus the remainder, the same way
     * {@code Compute.missilesHit} resolves oversized BA missile attacks. Expectation is linear,
     * so the decomposed sum is exact.</p>
     */
    static double expectedClusterHits(int rackSize) {
        Double cached = EXPECTED_CLUSTER_HITS_CACHE.get(rackSize);
        if (cached != null) {
            return cached;
        }
        // Computed outside computeIfAbsent: the decomposition recurses into other sizes, and
        // ConcurrentHashMap forbids map updates from inside a computeIfAbsent mapping function.
        double expected = computeExpectedClusterHits(rackSize);
        EXPECTED_CLUSTER_HITS_CACHE.putIfAbsent(rackSize, expected);
        return expected;
    }

    private static double computeExpectedClusterHits(int rackSize) {
        if (rackSize <= 1) {
            // A single projectile is not a cluster: it simply hits when the attack hits.
            return Math.max(0, rackSize);
        }
        if (hasClusterTableRow(rackSize)) {
            double expected = 0;
            for (int roll = 2; roll <= 12; roll++) {
                expected += TWO_D6_WEIGHTS[roll - 2] * Compute.calculateClusterHitTableAmount(roll, rackSize);
            }
            return expected / 36.0;
        }
        // No exact row: split off the largest row below this size and resolve the remainder
        // separately (TW battle armor missile pooling; mirrors Compute.missilesHit).
        for (int candidate = rackSize - 1; candidate >= 2; candidate--) {
            if (hasClusterTableRow(candidate)) {
                return computeExpectedClusterHits(candidate)
                      + computeExpectedClusterHits(rackSize - candidate);
            }
        }
        // Unreachable: the table always contains a row for 2.
        return rackSize;
    }

    /** @return whether the cluster hits table has an exact row for this rack size */
    private static boolean hasClusterTableRow(int rackSize) {
        // Every real table row yields at least 1 hit on a roll of 7; missing sizes return 0.
        return Compute.calculateClusterHitTableAmount(7, rackSize) > 0;
    }

    /**
     * One weapon's resolved firing options: for each candidate ammunition, the range array to use.
     * Damage is resolved lazily per range so range-variable weapons (which override
     * {@code getDamage(int)}) are handled naturally. The bearing array records which of the six
     * hex-side directions the weapon's firing arc covers, as mounted (no torso twist). The trooper
     * count is 1 except where multiple soldiers fire the weapon as one attack: every rifleman in a
     * conventional platoon, or every suit in a battle armor squad (squad support weapons excepted).
     */
    private record WeaponContribution(WeaponMounted weapon, List<AmmoOption> ammoOptions, int gunnery,
          boolean useExtremeRange, boolean[] bearing, int troopers) {

        /** @return whether this weapon's firing arc covers the given hex-side direction */
        boolean bearsOn(int direction) {
            return bearing[direction];
        }

        /**
         * Determines which of the six hex-side sectors the weapon's firing arc covers by probing
         * every hex on a ring around the unit and mapping each hex to its sector. Probing only the
         * six sector spines would miss arcs whose boundaries run exactly along a spine - a
         * rear-mounted weapon's arc covers all three rear hexsides, but its boundary spines can
         * test as out-of-arc. A sector counts as covered when any of its ring hexes is in arc.
         * Pure arc geometry - no game or board.
         */
        private static boolean[] computeBearing(Entity entity, WeaponMounted weapon) {
            boolean[] bearing = new boolean[DIRECTIONS];
            // A weapon not yet allocated to a location (a unit under construction in MegaMekLab)
            // has no firing arc yet: it contributes to the omnidirectional damage curves but to no
            // radar sector, so the radars fill in as the loadout is placed.
            if (weapon.getLocation() == Entity.LOC_NONE) {
                return bearing;
            }
            int arc = entity.getWeaponArc(entity.getEquipmentNum(weapon));
            Coords center = new Coords(ARC_PROBE_DISTANCE * 2, ARC_PROBE_DISTANCE * 2);
            for (Coords probe : center.allAtDistance(ARC_PROBE_DISTANCE)) {
                if (ComputeArc.isInArc(center, 0, probe, arc)) {
                    bearing[center.direction(probe)] = true;
                }
            }
            return bearing;
        }

        /** Ammo types whose loaded munitions genuinely differ per shot, so all of them are candidates. */
        private static final List<AmmoType.AmmoTypeEnum> MULTI_PROFILE_AMMO = List.of(
              AmmoType.AmmoTypeEnum.ATM, AmmoType.AmmoTypeEnum.IATM, AmmoType.AmmoTypeEnum.MML);

        /**
         * @return the firing options for this weapon, or {@code null} if it has no way to fire (no usable
         *       ammunition for an ammo-dependent weapon)
         */
        static WeaponContribution of(Entity entity, WeaponMounted weapon, int gunnery, boolean useExtremeRange) {
            return of(entity, weapon, gunnery, useExtremeRange, null);
        }

        /**
         * @param precomputedBearing the firing directions to use instead of this weapon's own arc,
         *                           for bay members that fire through their bay's arc; {@code null}
         *                           to compute from the weapon itself
         */
        static WeaponContribution of(Entity entity, WeaponMounted weapon, int gunnery, boolean useExtremeRange,
              boolean[] precomputedBearing) {
            WeaponType weaponType = weapon.getType();
            List<AmmoOption> options = new ArrayList<>();

            boolean ammoless = (weaponType.getAmmoType() == AmmoType.AmmoTypeEnum.NA)
                  || (weaponType.getAmmoType() == AmmoType.AmmoTypeEnum.INFANTRY);
            if (weaponType instanceof InfantryWeapon infantryWeapon) {
                // Infantry weapons carry a single "infantry range" R instead of bracket fields;
                // getRanges() would report 0/0/0/3R and put the whole band in the long bracket.
                // The TW brackets are R / 2R / 3R (4R extreme); R=0 weapons reach only 1 hex.
                int infantryRange = infantryWeapon.getInfantryRange();
                int[] ranges = (infantryRange > 0)
                      ? new int[] { 0, infantryRange, infantryRange * 2, infantryRange * 3, infantryRange * 4 }
                      : new int[] { 0, 1, 1, 1, 1 };
                options.add(new AmmoOption(null, ranges));
            } else if (ammoless) {
                options.add(new AmmoOption(null, weaponType.getRanges(weapon)));
            } else {
                List<AmmoMounted> ammos;
                if (MULTI_PROFILE_AMMO.contains(weaponType.getAmmoType())) {
                    ammos = entity.getAmmo(weapon);
                } else if (weapon.getLinkedAmmo() != null) {
                    ammos = new ArrayList<>();
                    ammos.add(weapon.getLinkedAmmo());
                } else {
                    // Bay member weapons have no linked ammo - their ammunition lives in the bay -
                    // so fall back to any compatible ammo the unit carries.
                    ammos = entity.getAmmo(weapon);
                }
                for (AmmoMounted ammo : ammos) {
                    if ((ammo != null) && (ammo.getUsableShotsLeft() > 0)) {
                        options.add(new AmmoOption(ammo, weaponType.getRanges(weapon, ammo)));
                    }
                }
            }

            if (options.isEmpty()) {
                return null;
            }
            boolean[] bearing = (precomputedBearing != null) ? precomputedBearing
                  : computeBearing(entity, weapon);
            return new WeaponContribution(weapon, options, gunnery, useExtremeRange,
                  bearing, firingTroopers(entity, weapon));
        }

        /**
         * The number of soldiers firing this weapon as a single attack. Battle armor squads fire
         * one attack per weapon type across all active suits (squad support weapons fire once);
         * conventional infantry fire their small arms with every shooting trooper, but crew-served
         * field guns fire once. Everything else is a single weapon.
         */
        private static int firingTroopers(Entity entity, WeaponMounted weapon) {
            if (entity instanceof BattleArmor battleArmor) {
                return weapon.isSquadSupportWeapon() ? 1 : Math.max(1, battleArmor.getShootingStrength());
            }
            if ((entity instanceof Infantry infantry) && (weapon.getType() instanceof InfantryWeapon)) {
                return Math.max(1, infantry.getShootingStrength());
            }
            return 1;
        }

        /** @return the longest range this weapon reaches with any of its ammunition options */
        int maxRange() {
            int reach = 0;
            for (AmmoOption option : ammoOptions) {
                int optionReach = useExtremeRange
                      ? option.ranges()[RangeType.RANGE_EXTREME]
                      : option.ranges()[RangeType.RANGE_LONG];
                reach = Math.max(reach, optionReach);
            }
            return reach;
        }

        /**
         * Resolves this weapon at a range, choosing the ammunition with the best expected damage.
         *
         * @return the weapon's contribution at this range, or {@code null} if it cannot reach
         */
        WeaponAtRange atRange(int range) {
            WeaponAtRange best = null;
            for (AmmoOption option : ammoOptions) {
                WeaponAtRange candidate = resolve(option, range);
                if ((candidate != null)
                      && ((best == null) || (candidate.expectedDamage() > best.expectedDamage()))) {
                    best = candidate;
                }
            }
            return best;
        }

        private WeaponAtRange resolve(AmmoOption option, int range) {
            int[] ranges = option.ranges();
            int bracket = RangeType.rangeBracket(range, ranges, useExtremeRange, false);
            if (bracket == RangeType.RANGE_OUT) {
                return null;
            }

            int bracketMod = switch (bracket) {
                case RangeType.RANGE_MEDIUM -> BRACKET_MOD_MEDIUM;
                case RangeType.RANGE_LONG -> BRACKET_MOD_LONG;
                case RangeType.RANGE_EXTREME -> BRACKET_MOD_EXTREME;
                default -> 0;
            };
            int minRangeMod = (range <= ranges[RangeType.RANGE_MINIMUM])
                  ? (ranges[RangeType.RANGE_MINIMUM] - range + 1)
                  : 0;
            double hitProbability = Compute.oddsAbove(gunnery + bracketMod + minRangeMod) / 100.0;

            WeaponType weaponType = weapon.getType();

            // Infantry small arms: the whole platoon or squad fires as one attack dealing
            // ceil(per-trooper damage x shooting troopers) on a single to-hit roll (TW p.215).
            if (weaponType instanceof InfantryWeapon infantryWeapon) {
                double attackDamage = Math.ceil(infantryWeapon.getInfantryDamage() * troopers);
                if (attackDamage <= 0) {
                    return null;
                }
                return new WeaponAtRange(attackDamage, attackDamage * hitProbability, 0);
            }

            int baseDamage = weaponType.getDamage(range);

            double maxDamage;
            double expectedDamage;
            int heat = weaponType.getHeat();
            int rapidFireShots = rapidFireShots(weaponType);
            if (baseDamage == WeaponType.DAMAGE_BY_CLUSTER_TABLE) {
                // A battle armor squad pools its missiles into one cluster roll (TW p.218), so the
                // effective rack is rack x troopers; everything else fires its plain rack.
                int damagePerShot = (option.ammo() != null) ? option.ammo().getType().getDamagePerShot() : 1;
                int rackSize = weaponType.getRackSize() * troopers;
                maxDamage = (double) rackSize * damagePerShot;
                expectedDamage = expectedClusterHits(rackSize) * damagePerShot * hitProbability;
            } else if ((baseDamage == WeaponType.DAMAGE_ARTILLERY) || weaponType.hasFlag(WeaponType.F_ARTILLERY)
                  || (baseDamage == WeaponType.DAMAGE_SPECIAL) || (baseDamage == WeaponType.DAMAGE_VARIABLE)) {
                // Artillery deals its rack size on a hit; special/variable weapons that did not
                // resolve through getDamage(range) fall back to the same estimate.
                maxDamage = weaponType.getRackSize();
                expectedDamage = maxDamage * hitProbability;
            } else if (baseDamage <= 0) {
                return null;
            } else if (rapidFireShots > 1) {
                // Ultra autocannons fire 2 shots and rotaries up to 6, with hits resolved on the
                // cluster table for the shot count (TW p.118); heat accrues per shot. Jamming is
                // ignored (known approximation).
                maxDamage = (double) baseDamage * rapidFireShots;
                expectedDamage = baseDamage * expectedClusterHits(rapidFireShots) * hitProbability;
                heat *= rapidFireShots;
            } else if (troopers > 1) {
                // Battle armor direct-fire weapons: one attack, hits determined by the cluster
                // table rolled on the number of firing suits, each hit dealing weapon damage.
                maxDamage = (double) baseDamage * troopers;
                expectedDamage = baseDamage * expectedClusterHits(troopers) * hitProbability;
            } else {
                maxDamage = baseDamage;
                expectedDamage = baseDamage * hitProbability;
            }

            if (maxDamage <= 0) {
                return null;
            }

            // Capital and sub-capital weapons report capital-scale damage: 1 capital = 10 standard
            // (SO p.116). Convert so a WarShip's naval lasers and its standard-scale point defense
            // read on the same axis.
            if (weaponType.isCapital() || weaponType.isSubCapital()) {
                maxDamage *= 10;
                expectedDamage *= 10;
            }
            return new WeaponAtRange(maxDamage, expectedDamage, heat);
        }

        /**
         * The number of shots this weapon fires per turn at its maximum rate: 2 for ultra
         * autocannons, 6 for rotaries, 1 for everything else.
         */
        private static int rapidFireShots(WeaponType weaponType) {
            return switch (weaponType.getAmmoType()) {
                case AC_ULTRA, AC_ULTRA_THB -> 2;
                case AC_ROTARY -> 6;
                default -> 1;
            };
        }
    }

    /** One candidate ammunition load for a weapon: null ammo means the weapon needs none. */
    private record AmmoOption(AmmoMounted ammo, int[] ranges) {
    }

    /** One weapon's resolved damage at a specific range. */
    private record WeaponAtRange(double maxDamage, double expectedDamage, int heat) {

        /** @return expected damage per point of heat; heat-free weapons rank above everything */
        double damagePerHeat() {
            return (heat <= 0) ? Double.MAX_VALUE : (expectedDamage / heat);
        }
    }
}
