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
package megamek.client.bot.princess;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.ToIntFunction;

import jakarta.annotation.Nullable;
import megamek.client.bot.princess.EmplacementFirepower.WeaponReach;
import megamek.common.Hex;
import megamek.common.LosEffects;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.board.CubeCoords;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.WeaponType;
import megamek.common.game.Game;
import megamek.common.options.OptionsConstants;
import megamek.common.units.AbstractBuildingEntity;
import megamek.common.units.Entity;
import megamek.common.units.IBuilding;
import megamek.common.units.Terrains;
import megamek.logging.MMLogger;

/**
 * Picks the hex a TO:AR gun emplacement (an Advanced Building of the gun emplacement class) is built on.
 *
 * <h2>Why it needs its own placement</h2>
 *
 * <p>These emplacements are buildings, not {@link megamek.common.equipment.GunEmplacement} turrets, so they used to
 * fall through to the deployment ranking built for units that move. That ranking scores a hex on how many paths lead
 * out of it and how easily the unit can reach the far edge, neither of which means anything for a building, and it
 * penalised every terrain type in the hex so the bot hunted for bare ground. On a town map the bare ground in the
 * centre zone is the ragged fringe of the town, so the emplacements piled up there, height 1 among taller buildings,
 * unable to see or be seen (MegaMek issue #9302).</p>
 *
 * <p>The first cure judged a hex purely on how far it could see. In a city the only long views are down the streets,
 * so the emplacements went into the roadway, and because only infantry may enter an armoured building each one walled
 * its street off. The rule below sites them the way a fortification officer would: beside the street, covering it by
 * fire.</p>
 *
 * <h2>The rule</h2>
 *
 * <ol>
 *     <li><b>The least troublesome ground</b>, see {@link SiteProblem}: never in water or magma, never across a
 *     bottleneck, never on a road, never in place of a building, each only while some better hex is legal.</li>
 *     <li><b>At least {@link #MINIMUM_SPACING_HEXES} from another emplacement</b>, best effort: the hexes that miss by
 *     the least are the only ones considered.</li>
 *     <li>Among those, the hex whose <b>guns are worth the most over the ground it overlooks</b>. Each hex in line of
 *     sight counts for the damage the emplacement can expect to do there ({@link EmplacementFirepower}), weighed by
 *     how much the ground matters ({@link EmplacementApproach}): most on the enemy's routes into the place defended,
 *     less the further from that place, and double on a road. Higher ground breaks ties in its favour.</li>
 *     <li><b>Interlocking fire.</b> Ground one other emplacement already covers counts for more, not less, so that
 *     the enemy's way in lies under two guns; a third gun on the same ground adds little. An emplacement also earns a
 *     bonus for each neighbour whose flanks it can hit and for each neighbour that can hit its own, so nothing walks
 *     up to one of them unengaged.</li>
 *     <li><b>Depth by reach.</b> Each emplacement loses score for standing nearer to or further from the front line
 *     than its weapons fight best ({@link EmplacementFirepower#standoff}): short-range guns in the line, long-range
 *     guns a band behind.</li>
 *     <li><b>Indirect fire.</b> Missile racks and mortars also count ground they cannot see, when another emplacement
 *     can see it or it lies on the enemy's routes and so will be watched, and they stand furthest back.</li>
 *     <li>An emplacement with <b>artillery</b> needs no view, so it takes the hex that sees, and is seen from, the
 *     least.</li>
 * </ol>
 *
 * <p>Emplacements are strictly height 1 and may not stand on other buildings (TO:AR), so the choice of hex is the only
 * way to give one a view.</p>
 */
public final class EmplacementDeployment {
    private static final MMLogger LOGGER = MMLogger.create(EmplacementDeployment.class);

    /** The closest two emplacements stand, so that one blast does not catch both. */
    static final int MINIMUM_SPACING_HEXES = 3;

    /** Ground is only judged this far out; past it, an emplacement's guns are worth little. */
    static final int MAX_SIGHTLINE_RANGE = 15;

    /** The shortest distance an emplacement is ever judged out to. */
    static final int MIN_SIGHTLINE_RANGE = 3;

    /** Range used for an emplacement with no weapons. */
    static final int DEFAULT_REACH = 9;

    /**
     * Line of sight is the expensive part, so it is only traced from this many of the best-spaced hexes: the first
     * ones in the list given to {@link #choose}, which is why the caller sorts it with {@link #mostPromisingFirst}.
     */
    static final int MAX_CANDIDATES_TO_SIGHT = 120;

    /** Only one hex in this many is checked for line of sight; see {@link #isSampleHex}. */
    static final int SAMPLE_SPACING = 3;

    /** How much more a road hex is worth: it is where vehicles and fast units will be. */
    static final int ROAD_WEIGHT = 2;

    /**
     * What ground exactly one other emplacement already covers is worth, in hundredths of its full value: more than
     * fresh ground, because a second gun there makes a crossfire.
     */
    static final int SECOND_GUN_PERCENT = 150;

    /** What ground two or more other emplacements already cover is worth, in hundredths of its full value. */
    static final int EXTRA_GUN_PERCENT = 30;

    /**
     * The bonus, in hundredths of the score, for each neighbour whose flanks this emplacement can hit, and again for
     * each neighbour that can hit the flanks of this one.
     */
    static final int MUTUAL_SUPPORT_PERCENT = 10;

    /** How many neighbours count toward each half of the mutual support bonus. */
    static final int MAX_NEIGHBOURS_COUNTED = 2;

    /** Ground worth no more than this is merely in view, not covered: out of range or too far out to matter. */
    static final int SEEN_ONLY_VALUE = 1;

    /** The height of the unit an emplacement is looking for: a 'Mek. */
    private static final int TARGET_HEIGHT = 1;

    /**
     * What is wrong with building on a hex, mildest first. An emplacement is only built on a hex with a problem when
     * every legal hex has one at least as bad.
     */
    enum SiteProblem {
        /** Nothing: clear ground beside the traffic. */
        NONE,
        /** A small building stands here, which the emplacement would replace. */
        REPLACES_BUILDING,
        /** The hex carries a road or a bridge. */
        ON_ROAD,
        /** An emplacement here would wall off a street; see {@link EmplacementStreetCheck}. */
        BLOCKS_PASSAGE,
        /** Water or magma: legal to the building rules but nonsense on the table. */
        WATER_OR_MAGMA
    }

    private EmplacementDeployment() {
    }

    /**
     * @param entity the unit being deployed
     *
     * @return {@code true} for a TO:AR gun emplacement, which deploys through {@link #site}
     */
    public static boolean isGunEmplacementBuilding(Entity entity) {
        return (entity instanceof AbstractBuildingEntity building)
              && (building.getBldgClass() == IBuilding.GUN_EMPLACEMENT);
    }

    /**
     * Sites an emplacement: works out what is defended and how the enemy comes at it, what the emplacements already
     * placed cover, and where this one's weapons fight best, then picks its hex with {@link #choose}.
     *
     * @param game              the game, for line of sight and the indirect fire rule
     * @param board             the board it deploys on
     * @param emplacement       the emplacement being placed
     * @param legalCoords       hexes it may legally deploy on
     * @param otherEmplacements the friendly emplacements already deployed on that board
     * @param enemyCentre       the middle of the enemy's deployment zones, or {@code null} when unknown
     *
     * @return the chosen hex, or {@code null} when there are no legal hexes
     */
    public static @Nullable Coords site(Game game, Board board, Entity emplacement, List<Coords> legalCoords,
          List<Entity> otherEmplacements, @Nullable Coords enemyCentre) {
        EmplacementApproach approach = EmplacementApproach.plan(board, legalCoords, enemyCentre);
        List<WeaponReach> weapons = EmplacementFirepower.weaponsOf(emplacement);
        boolean isIndirectFireInUse = game.getOptions().booleanOption(OptionsConstants.BASE_INDIRECT_FIRE)
              && EmplacementFirepower.canFireIndirectly(weapons);

        Map<Coords, Map<Coords, Integer>> neighbourGround = new HashMap<>();
        Set<Coords> spottedGround = new HashSet<>();
        for (Entity other : otherEmplacements) {
            Map<Coords, Integer> ground = groundCovered(game, board, other.getPosition(), other, approach, Set.of());
            neighbourGround.put(other.getPosition(), ground);
            if (isIndirectFireInUse) {
                spottedGround.addAll(ground.keySet());
            }
        }
        if (isIndirectFireInUse) {
            // the enemy's routes in are what the front line is sited to watch
            spottedGround.addAll(approach.routes());
        }
        int standoff = EmplacementFirepower.standoff(weapons, isIndirectFireInUse);
        LOGGER.info("[Emplacement] {}: defending {} out to {} hexes; {} hexes on the enemy's routes in from {}, "
                    + "{} of them the front line; belongs {} hexes behind it and guards neighbours within {}; "
                    + "indirect fire {}",
              emplacement.getShortName(), approach.defended().toFriendlyString(), approach.radius(),
              approach.routes().size(),
              (enemyCentre == null) ? "an unknown direction" : enemyCentre.toFriendlyString(),
              approach.front().size(), standoff, EmplacementFirepower.guardRange(weapons),
              isIndirectFireInUse ? "counted" : "not counted");

        List<Coords> mostPromisingFirst = mostPromisingFirst(approach.withinTheDefence(legalCoords), board,
              emplacement, approach, standoff);
        return choose(emplacement, mostPromisingFirst, board, new ArrayList<>(neighbourGround.keySet()),
              site -> neighbourGround.containsKey(site) ? neighbourGround.get(site)
                    : groundCovered(game, board, site, emplacement, approach, spottedGround),
              site -> approach.depthPercent(site, standoff));
    }

    /**
     * Picks the hex to build the emplacement on.
     *
     * @param emplacement       the emplacement being placed
     * @param candidates        hexes it may legally deploy on, most promising first; only the first
     *                          {@link #MAX_CANDIDATES_TO_SIGHT} suitable ones are judged
     * @param board             the board it deploys on
     * @param otherEmplacements positions of the friendly emplacements already on that board
     * @param groundCovered     the hexes an emplacement on a given hex overlooks and what each is worth, from
     *                          {@link #groundCovered}; asked for the candidates and for the other emplacements'
     *                          hexes alike, and supplied so it can be stubbed
     * @param depthPercent      the share of its score an emplacement keeps on a given hex for standing the right
     *                          distance behind the front, from {@link EmplacementApproach#depthPercent}
     *
     * @return the chosen hex, or {@code null} when there are no candidates
     */
    public static @Nullable Coords choose(Entity emplacement, List<Coords> candidates, Board board,
          List<Coords> otherEmplacements, Function<Coords, Map<Coords, Integer>> groundCovered,
          ToIntFunction<Coords> depthPercent) {
        if (candidates.isEmpty()) {
            return null;
        }

        List<Coords> sites = leastTroublesomeSites(emplacement, candidates, board, otherEmplacements);

        int leastCrowding = Integer.MAX_VALUE;
        for (Coords site : sites) {
            leastCrowding = Math.min(leastCrowding, crowding(site, otherEmplacements));
        }
        final int bestSpacing = leastCrowding;
        List<Coords> spaced = sites.stream()
              .filter(site -> crowding(site, otherEmplacements) == bestSpacing)
              .limit(MAX_CANDIDATES_TO_SIGHT)
              .toList();

        Map<Coords, Map<Coords, Integer>> neighbourGround = new HashMap<>();
        for (Coords other : otherEmplacements) {
            neighbourGround.put(other, groundCovered.apply(other));
        }
        boolean isIndirectFire = EmplacementFirepower.isIndirectFire(emplacement);
        int guardRange = EmplacementFirepower.guardRange(EmplacementFirepower.weaponsOf(emplacement));

        Coords best = null;
        int bestScore = Integer.MIN_VALUE;
        int bestLevel = Integer.MIN_VALUE;
        for (Coords site : spaced) {
            Hex hex = board.getHex(site);
            int level = (hex == null) ? 0 : hex.getLevel();
            Map<Coords, Integer> covered = groundCovered.apply(site);
            int score = isIndirectFire ? -covered.size()
                  : (score(site, covered, neighbourGround, guardRange) * depthPercent.applyAsInt(site)) / 100;
            if ((score > bestScore) || ((score == bestScore) && (level > bestLevel))) {
                bestScore = score;
                bestLevel = level;
                best = site;
            }
        }
        LOGGER.info("[Emplacement] {}: chose {} (level {}, score {}, {}, depth {} percent) from {} spaced of {} "
                    + "candidate hexes (crowding {}, {} other emplacements)",
              emplacement.getShortName(), best, bestLevel, bestScore,
              isIndirectFire ? "artillery: least seen" : "most firepower over ground under interlocking fire",
              (best == null) ? 0 : depthPercent.applyAsInt(best), spaced.size(), candidates.size(), bestSpacing,
              otherEmplacements.size());
        return best;
    }

    /**
     * Keeps the candidates with the mildest {@link SiteProblem}, and says so in the log when even those have one.
     */
    private static List<Coords> leastTroublesomeSites(Entity emplacement, List<Coords> candidates, Board board,
          List<Coords> otherEmplacements) {
        Map<Coords, SiteProblem> problems = new HashMap<>();
        Map<SiteProblem, Integer> tally = new EnumMap<>(SiteProblem.class);
        SiteProblem mildest = SiteProblem.WATER_OR_MAGMA;
        for (Coords candidate : candidates) {
            SiteProblem problem = siteProblem(board, candidate, otherEmplacements);
            problems.put(candidate, problem);
            tally.merge(problem, 1, Integer::sum);
            if (problem.compareTo(mildest) < 0) {
                mildest = problem;
            }
        }
        if (mildest == SiteProblem.NONE) {
            LOGGER.info("[Emplacement] {}: the {} legal hexes sort as {}", emplacement.getShortName(),
                  candidates.size(), tally);
        } else {
            LOGGER.warn("[Emplacement] {}: no clear ground among {} legal hexes, which sort as {}; the best "
                  + "available is {}", emplacement.getShortName(), candidates.size(), tally, mildest);
        }
        List<Coords> accepted = new ArrayList<>();
        for (Coords candidate : candidates) {
            if (problems.get(candidate) == mildest) {
                accepted.add(candidate);
            }
        }
        return accepted;
    }

    /**
     * @param board             the board
     * @param site              the hex to check
     * @param otherEmplacements positions of the emplacements already placed
     *
     * @return the worst thing wrong with building an emplacement on the hex
     */
    static SiteProblem siteProblem(Board board, Coords site, List<Coords> otherEmplacements) {
        Hex hex = board.getHex(site);
        if (isUnsuitableSite(hex)) {
            return SiteProblem.WATER_OR_MAGMA;
        }
        if (EmplacementStreetCheck.blocksPassage(board, site, otherEmplacements)) {
            return SiteProblem.BLOCKS_PASSAGE;
        }
        if (EmplacementStreetCheck.isOnRoad(hex)) {
            return SiteProblem.ON_ROAD;
        }
        if (hex.containsTerrain(Terrains.BUILDING)) {
            return SiteProblem.REPLACES_BUILDING;
        }
        return SiteProblem.NONE;
    }

    /**
     * @param hex the hex to check; may be {@code null}
     *
     * @return {@code true} if no emplacement should be built here while any other hex will do
     */
    static boolean isUnsuitableSite(@Nullable Hex hex) {
        if (hex == null) {
            return true;
        }
        return (hex.containsTerrain(Terrains.WATER) && (hex.terrainLevel(Terrains.WATER) > 0))
              || hex.containsTerrain(Terrains.MAGMA);
    }

    /**
     * How far a hex falls short of the spacing from the nearest other emplacement.
     *
     * @param hex    the hex being judged
     * @param others positions of the other emplacements
     *
     * @return 0 when far enough from all of them, up to {@link #MINIMUM_SPACING_HEXES} when on top of one
     */
    static int crowding(Coords hex, List<Coords> others) {
        int nearest = Integer.MAX_VALUE;
        for (Coords other : others) {
            nearest = Math.min(nearest, hex.distance(other));
        }
        return Math.max(0, MINIMUM_SPACING_HEXES - nearest);
    }

    /**
     * What the ground an emplacement overlooks is worth to the defence as a whole, under interlocking fire.
     *
     * <p>Ground nobody else covers counts in full. Ground one other emplacement covers counts for
     * {@link #SECOND_GUN_PERCENT}, because the second gun makes a crossfire, and ground two or more already cover
     * for only {@link #EXTRA_GUN_PERCENT}. On top of that comes {@link #MUTUAL_SUPPORT_PERCENT} for each neighbour
     * within {@code guardRange} whose flanks this emplacement can hit, and the same for each neighbour that can hit
     * the flanks of this one, up to {@link #MAX_NEIGHBOURS_COUNTED} each way.</p>
     *
     * @param site            the hex the emplacement would be built on
     * @param covered         the hexes it overlooks and the value of each, from {@link #groundCovered}
     * @param neighbourGround the same for each emplacement already placed, by the hex it stands on
     * @param guardRange      how close a neighbour must stand to be guarded, from
     *                        {@link EmplacementFirepower#guardRange}
     *
     * @return the score; higher is better
     */
    static int score(Coords site, Map<Coords, Integer> covered, Map<Coords, Map<Coords, Integer>> neighbourGround,
          int guardRange) {
        int score = 0;
        for (Map.Entry<Coords, Integer> ground : covered.entrySet()) {
            int otherGuns = 0;
            for (Map<Coords, Integer> neighbour : neighbourGround.values()) {
                if (neighbour.getOrDefault(ground.getKey(), 0) > SEEN_ONLY_VALUE) {
                    otherGuns++;
                }
            }
            int percent = switch (otherGuns) {
                case 0 -> 100;
                case 1 -> SECOND_GUN_PERCENT;
                default -> EXTRA_GUN_PERCENT;
            };
            score += (ground.getValue() * percent) / 100;
        }

        int neighboursGuarded = 0;
        int neighboursGuarding = 0;
        for (Map.Entry<Coords, Map<Coords, Integer>> neighbour : neighbourGround.entrySet()) {
            boolean isCloseEnough = site.distance(neighbour.getKey()) <= guardRange;
            if (isCloseEnough && coversFlanksOf(neighbour.getKey(), covered)) {
                neighboursGuarded++;
            }
            if (coversFlanksOf(site, neighbour.getValue())) {
                neighboursGuarding++;
            }
        }
        int supportPercent = MUTUAL_SUPPORT_PERCENT
              * (Math.min(MAX_NEIGHBOURS_COUNTED, neighboursGuarded)
              + Math.min(MAX_NEIGHBOURS_COUNTED, neighboursGuarding));
        return score + ((score * supportPercent) / 100);
    }

    /** Whether any hex beside an emplacement is among the ground another one covers. */
    private static boolean coversFlanksOf(Coords emplacement, Map<Coords, Integer> covered) {
        for (int direction = 0; direction < 6; direction++) {
            if (covered.getOrDefault(emplacement.translated(direction), 0) > SEEN_ONLY_VALUE) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param emplacement the emplacement
     *
     * @return the longest long range among its weapons, held to the sightline limits
     */
    static int reach(Entity emplacement) {
        int longest = 0;
        for (Mounted<?> weapon : emplacement.getWeaponList()) {
            if (weapon.getType() instanceof WeaponType weaponType) {
                longest = Math.max(longest, weaponType.getLongRange());
            }
        }
        if (longest == 0) {
            longest = DEFAULT_REACH;
        }
        return Math.clamp(longest, MIN_SIGHTLINE_RANGE, MAX_SIGHTLINE_RANGE);
    }

    /**
     * Whether a hex is one of those checked for line of sight. Checking every hex in reach from every candidate is too
     * slow, so one hex in {@link #SAMPLE_SPACING} is used. The choice depends only on the hex, never on who is looking,
     * so two emplacements that overlook the same ground are seen to overlook the same sample hexes.
     *
     * @param hex the hex
     *
     * @return {@code true} if line of sight to the hex is checked
     */
    static boolean isSampleHex(Coords hex) {
        return Math.floorMod(hex.getX() + (2 * hex.getY()), SAMPLE_SPACING) == 0;
    }

    /**
     * The ground an emplacement built on a hex would cover: every sample hex within reach that it can see, and every
     * one it cannot see but could fire on indirectly, with what each is worth. A hex is worth the damage the
     * emplacement can expect to do to a moving target there, weighed by {@link EmplacementApproach#weightPercent}
     * and times {@link #ROAD_WEIGHT} if it is a road.
     *
     * @param game          the game, for line of sight
     * @param board         the board the emplacement deploys on
     * @param origin        the hex being judged
     * @param emplacement   the emplacement, for its height, weapons and crew
     * @param approach      what is defended and how the enemy reaches it, from {@link EmplacementApproach#plan}
     * @param spottedGround hexes a friendly unit can be expected to see, on which the emplacement may fire
     *                      indirectly; empty when it has no indirect weapon or the rule is not in play
     *
     * @return the hexes covered and the value of each
     */
    static Map<Coords, Integer> groundCovered(Game game, Board board, Coords origin, Entity emplacement,
          EmplacementApproach approach, Set<Coords> spottedGround) {
        Map<Coords, Integer> covered = new HashMap<>();
        Hex originHex = board.getHex(origin);
        if (originHex == null) {
            return covered;
        }
        int height = emplacementHeight(emplacement);
        List<WeaponReach> weapons = EmplacementFirepower.weaponsOf(emplacement);
        int gunnery = EmplacementFirepower.gunneryOf(emplacement);
        for (Coords sample : origin.allAtDistanceOrLess(reach(emplacement))) {
            Hex sampleHex = sampleHexAt(board, origin, sample);
            if (sampleHex == null) {
                continue;
            }
            LosEffects.AttackInfo attackInfo = LosEffects.buildAttackInfo(origin, sample, board.getBoardId(),
                  height, TARGET_HEIGHT, originHex.getLevel(), sampleHex.getLevel());
            int distance = origin.distance(sample);
            if (LosEffects.calculateLos(game, attackInfo).canSee()) {
                int firepower = EmplacementFirepower.valueAtRange(weapons, distance, gunnery);
                // a hex in view but out of range still counts as seen, which is what artillery wants to avoid
                covered.put(sample, Math.max(SEEN_ONLY_VALUE, groundValue(firepower, sample, sampleHex, approach)));
            } else if (spottedGround.contains(sample)) {
                int firepower = EmplacementFirepower.indirectValueAtRange(weapons, distance, gunnery);
                int value = groundValue(firepower, sample, sampleHex, approach);
                if (value > SEEN_ONLY_VALUE) {
                    covered.put(sample, value);
                }
            }
        }
        return covered;
    }

    /**
     * Sorts the hexes an emplacement may deploy on so that the ones worth tracing line of sight from come first.
     * A hex's promise is what its guns would be worth if nothing blocked their view: cheap to work out, and never
     * less than what {@link #groundCovered} will find. An emplacement with artillery wants the opposite, so its list
     * runs the other way.
     *
     * @param candidates  hexes it may legally deploy on
     * @param board       the board it deploys on
     * @param emplacement the emplacement, for its weapons and crew
     * @param approach    what is defended and how the enemy reaches it
     * @param standoff    how far behind the front the emplacement belongs, from
     *                    {@link EmplacementFirepower#standoff}
     *
     * @return the same hexes, most promising first
     */
    static List<Coords> mostPromisingFirst(List<Coords> candidates, Board board, Entity emplacement,
          EmplacementApproach approach, int standoff) {
        List<WeaponReach> weapons = EmplacementFirepower.weaponsOf(emplacement);
        int gunnery = EmplacementFirepower.gunneryOf(emplacement);
        int reach = reach(emplacement);
        Map<Coords, Integer> promise = new HashMap<>();
        for (Coords candidate : candidates) {
            int groundPromise = promise(candidate, board, weapons, gunnery, reach, approach);
            promise.put(candidate, (groundPromise * approach.depthPercent(candidate, standoff)) / 100);
        }
        Comparator<Coords> byPromise = Comparator.comparingInt(promise::get);
        List<Coords> sorted = new ArrayList<>(candidates);
        sorted.sort(EmplacementFirepower.isIndirectFire(emplacement) ? byPromise : byPromise.reversed());
        return sorted;
    }

    private static int promise(Coords origin, Board board, List<WeaponReach> weapons, int gunnery, int reach,
          EmplacementApproach approach) {
        int promise = 0;
        for (Coords sample : origin.allAtDistanceOrLess(reach)) {
            Hex sampleHex = sampleHexAt(board, origin, sample);
            if (sampleHex != null) {
                int firepower = EmplacementFirepower.valueAtRange(weapons, origin.distance(sample), gunnery);
                promise += groundValue(firepower, sample, sampleHex, approach);
            }
        }
        return promise;
    }

    /**
     * @return the hex at {@code sample} when it is one the rule checks, on the board and not the hex being judged;
     *       otherwise {@code null}
     */
    private static @Nullable Hex sampleHexAt(Board board, Coords origin, Coords sample) {
        if (!isSampleHex(sample) || sample.equals(origin)) {
            return null;
        }
        return board.contains(sample) ? board.getHex(sample) : null;
    }

    private static int groundValue(int firepower, Coords ground, Hex groundHex, EmplacementApproach approach) {
        return ((firepower * approach.weightPercent(ground)) / 100)
              * (EmplacementStreetCheck.isOnRoad(groundHex) ? ROAD_WEIGHT : 1);
    }

    private static int emplacementHeight(Entity emplacement) {
        if ((emplacement instanceof AbstractBuildingEntity building) && (building.getInternalBuilding() != null)) {
            return Math.max(1, building.getInternalBuilding().getHeight(CubeCoords.ZERO));
        }
        return 1;
    }
}
