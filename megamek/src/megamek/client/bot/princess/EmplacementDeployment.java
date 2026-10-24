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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.ToIntFunction;

import jakarta.annotation.Nullable;
import megamek.common.Hex;
import megamek.common.LosEffects;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.board.CubeCoords;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.WeaponType;
import megamek.common.game.Game;
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
 * <h2>The rule</h2>
 *
 * <ol>
 *     <li><b>Never in water or magma</b> unless there is nowhere else. An emplacement standing in a lake is legal to
 *     the building rules but nonsense on the table, and open water would otherwise win on field of fire.</li>
 *     <li><b>At least {@link #MINIMUM_SPACING_HEXES} from another emplacement</b>, best effort: the hexes that miss by
 *     the least are the only ones considered.</li>
 *     <li>Among those, the hex with the <b>best field of fire</b>, judged by line of sight to sample hexes out to the
 *     emplacement's longest range, counting double the ones toward the enemy. Higher ground breaks ties in its
 *     favour.</li>
 * </ol>
 *
 * <p>Emplacements are strictly height 1 and may not stand on other buildings (TO:AR), so the choice of hex is the only
 * way to give one a view.</p>
 */
public final class EmplacementDeployment {
    private static final MMLogger LOGGER = MMLogger.create(EmplacementDeployment.class);

    /** How far apart emplacements try to stand, so they cover different ground. */
    static final int MINIMUM_SPACING_HEXES = 3;

    /** Sightlines are only checked this far out; past it, a sample hex says little about the hex being judged. */
    static final int MAX_SIGHTLINE_RANGE = 15;

    /** The nearest ring of sample hexes; anything closer is seen from almost any hex. */
    static final int MIN_SIGHTLINE_RANGE = 3;

    /** Range used for an emplacement with no weapons. */
    static final int DEFAULT_REACH = 9;

    /** Line of sight is the expensive part, so it is only traced from this many of the best-spaced hexes. */
    static final int MAX_CANDIDATES_TO_SIGHT = 80;

    /** Field-of-fire points a level of ground is worth. */
    static final int LEVEL_WEIGHT = 2;

    /** The height of the unit an emplacement is looking for: a 'Mek. */
    private static final int TARGET_HEIGHT = 1;

    private EmplacementDeployment() {
    }

    /**
     * @param entity the unit being deployed
     *
     * @return {@code true} for a TO:AR gun emplacement, which deploys through {@link #choose}
     */
    public static boolean isGunEmplacementBuilding(Entity entity) {
        return (entity instanceof AbstractBuildingEntity building)
              && (building.getBldgClass() == IBuilding.GUN_EMPLACEMENT);
    }

    /**
     * Picks the hex to build the emplacement on.
     *
     * @param emplacement       the emplacement being placed
     * @param candidates        hexes it may legally deploy on, in the order the caller would otherwise scan them
     * @param board             the board it deploys on
     * @param otherEmplacements positions of the friendly emplacements already on that board
     * @param fieldOfFire       how much a hex can see, from {@link #fieldOfFire}; supplied so it can be stubbed
     *
     * @return the chosen hex, or {@code null} when there are no candidates
     */
    public static @Nullable Coords choose(Entity emplacement, List<Coords> candidates, Board board,
          List<Coords> otherEmplacements, ToIntFunction<Coords> fieldOfFire) {
        if (candidates.isEmpty()) {
            return null;
        }

        List<Coords> sites = candidates.stream().filter(c -> !isUnsuitableSite(board.getHex(c))).toList();
        if (sites.isEmpty()) {
            LOGGER.warn("[Emplacement] {}: every legal hex is water or magma; building on one anyway",
                  emplacement.getShortName());
            sites = candidates;
        }

        int leastCrowding = Integer.MAX_VALUE;
        for (Coords site : sites) {
            leastCrowding = Math.min(leastCrowding, crowding(site, otherEmplacements));
        }
        final int bestSpacing = leastCrowding;
        List<Coords> spaced = sites.stream()
              .filter(c -> crowding(c, otherEmplacements) == bestSpacing)
              .limit(MAX_CANDIDATES_TO_SIGHT)
              .toList();

        Coords best = null;
        int bestScore = Integer.MIN_VALUE;
        for (Coords site : spaced) {
            Hex hex = board.getHex(site);
            int level = (hex == null) ? 0 : hex.getLevel();
            int score = fieldOfFire.applyAsInt(site) + (LEVEL_WEIGHT * level);
            if (score > bestScore) {
                bestScore = score;
                best = site;
            }
        }
        LOGGER.debug("[Emplacement] {}: chose {} with score {} from {} spaced of {} candidate hexes (crowding {})",
              emplacement.getShortName(), best, bestScore, spaced.size(), candidates.size(), bestSpacing);
        return best;
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
     * The hexes a candidate is judged against: on rings at the near limit, half the reach and the full reach, twelve
     * per ring, one along each hex direction and one halfway between each pair.
     *
     * @param origin the hex being judged
     * @param reach  how far out to look, from {@link #reach}
     *
     * @return the sample hexes, some possibly off the board
     */
    static List<Coords> sightlineSamples(Coords origin, int reach) {
        Set<Integer> rings = new LinkedHashSet<>();
        rings.add(Math.min(MIN_SIGHTLINE_RANGE, reach));
        rings.add(Math.max(MIN_SIGHTLINE_RANGE, reach / 2));
        rings.add(reach);

        List<Coords> samples = new ArrayList<>();
        for (int ring : rings) {
            for (int direction = 0; direction < 6; direction++) {
                samples.add(origin.translated(direction, ring));
                int half = ring / 2;
                if (half > 0) {
                    samples.add(origin.translated(direction, ring - half).translated((direction + 1) % 6, half));
                }
            }
        }
        return samples;
    }

    /**
     * How much of the surrounding ground an emplacement built on a hex could see: one point per sample hex in line of
     * sight, two if it lies toward the enemy.
     *
     * @param game        the game, for line of sight
     * @param board       the board the emplacement deploys on
     * @param origin      the hex being judged
     * @param emplacement the emplacement, for its height and reach
     * @param enemyCentre the middle of the enemy's deployment zones, or {@code null} when unknown
     *
     * @return the field-of-fire score
     */
    static int fieldOfFire(Game game, Board board, Coords origin, Entity emplacement, @Nullable Coords enemyCentre) {
        Hex originHex = board.getHex(origin);
        if (originHex == null) {
            return 0;
        }
        int height = emplacementHeight(emplacement);
        int score = 0;
        for (Coords sample : sightlineSamples(origin, reach(emplacement))) {
            Hex sampleHex = board.contains(sample) ? board.getHex(sample) : null;
            if (sampleHex == null) {
                continue;
            }
            LosEffects.AttackInfo attackInfo = LosEffects.buildAttackInfo(origin, sample, board.getBoardId(),
                  height, TARGET_HEIGHT, originHex.getLevel(), sampleHex.getLevel());
            if (LosEffects.calculateLos(game, attackInfo).canSee()) {
                boolean towardEnemy = (enemyCentre != null)
                      && (sample.distance(enemyCentre) < origin.distance(enemyCentre));
                score += towardEnemy ? 2 : 1;
            }
        }
        return score;
    }

    private static int emplacementHeight(Entity emplacement) {
        if ((emplacement instanceof AbstractBuildingEntity building) && (building.getInternalBuilding() != null)) {
            return Math.max(1, building.getInternalBuilding().getHeight(CubeCoords.ZERO));
        }
        return 1;
    }
}
