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
import java.util.List;

import megamek.common.annotations.Nullable;
import megamek.common.board.Coords;
import megamek.common.game.Game;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;

/**
 * Deployment half of the Mutual Support doctrine: get a force onto the board as a formation rather than as a set of
 * unrelated units.
 *
 * <h2>What stock deployment does</h2>
 *
 * <p>Stock deployment ranks a hex on terrain hazard and on how much open ground surrounds it, and never once looks at
 * where the rest of the force went. Worse, the candidate list arrives shuffled ({@code BotClient.getStartingCoordsArray}
 * randomizes it to stop units piling into the upper-left corner of flat maps) and
 * {@link Princess#rankDeploymentCoords(Entity, List)} then scans only the first twenty or so entries. Each unit is
 * therefore choosing the best of a small random sample drawn from the whole deployment zone, which is close to an
 * independent uniform draw per unit. Measured on a 32-wide board, a twelve-unit company lands about 26 hexes wide and
 * never closes up.</p>
 *
 * <h2>The rule</h2>
 *
 * <p>A hex is in position when it satisfies both halves of a band:</p>
 *
 * <ul>
 *     <li><b>At least {@link FormationGeometry#MINIMUM_SPACING_HEXES} from the nearest friend</b>, so a force does not stack itself into
 *     one artillery template or block its own movement lanes. Measured against the nearest friend, because crowding is
 *     a local problem. This end of the band is best effort: it yields to support when a zone is too small to hold both
 *     (see {@link #isInPosition}), which a shallow deployment strip routinely is.</li>
 *     <li><b>Within the formation radius of the force's centre of mass</b>, so every unit can contribute fire where the
 *     force is fighting. See {@link FormationGeometry#formationRadius(List, double)}.</li>
 * </ul>
 *
 * <p><b>The upper bound is measured against the centre of mass, never against the nearest friend.</b> This is the whole
 * point. "Stay within supporting range of somebody" is satisfied by a chain, where each unit is close to one neighbour
 * and the force as a whole is strung across the map - which is precisely the picket line this rule exists to break. A
 * nearest-friend rule would rebuild it. Measuring against the centre forces the force to be compact rather than merely
 * connected.</p>
 *
 * <h2>How it is applied</h2>
 *
 * <p>The fix is an ordering, not a score. Candidates are sorted by how badly they miss the band, so the capped scan
 * sees hexes that keep the force together instead of a random spread. Everything already in position ties at zero and
 * keeps its shuffled order, which leaves the existing hazard and open-ground ranking free to pick among them exactly as
 * before. Cohesion decides <em>which part of the zone</em> to consider; terrain still decides the hex.</p>
 *
 * <p>This cannot slow the advance, which is the standing constraint on the whole doctrine: it changes where a unit
 * starts, never where it may move afterwards, and every legal hex remains reachable.</p>
 *
 * @see MutualSupportPathRanker the movement half of the same doctrine
 * @see SupportEnvelope the shared definition of supporting range
 */
public final class MutualSupportDeployment {
    private final static MMLogger logger = MMLogger.create(MutualSupportDeployment.class);

    private MutualSupportDeployment() {
    }

    /**
     * Reorders candidate deployment hexes so the ones that keep the force together are scanned first.
     *
     * @param deployedUnit    the unit being placed
     * @param candidates      legal deployment hexes, in the order the caller would otherwise scan them
     * @param friends         friendly units, deployed or not, from {@code BotClient.getFriendEntities()}
     * @param game            the game, used to keep the anchor to units sharing a board with the deploying unit
     * @param formationRadius how far from the force's centre a unit may form up, from
     *                        {@link FormationGeometry#formationRadius(List, double)}
     *
     * @return the candidates, reordered; the same list instance when there is nothing to reorder
     */
    public static List<Coords> prioritize(Entity deployedUnit, List<Coords> candidates, List<Entity> friends,
          Game game, int formationRadius) {
        if (candidates.size() < 2) {
            logger.debug("[MutualSupport] deploy [{}]: not reordered, only {} candidate hex(es)",
                  deployedUnit.getShortName(), candidates.size());
            return candidates;
        }
        List<Coords> friendlyPositions = anchorPositions(deployedUnit, friends, game);
        boolean firstOnBoard = friendlyPositions.isEmpty();
        Coords anchor = FormationGeometry.centroid(firstOnBoard ? candidates : friendlyPositions);
        if (firstOnBoard) {
            // Nothing to form up on yet, so the zone's own middle is the anchor and this unit becomes the
            // seed everything after it gathers around.
            logger.debug("[MutualSupport] deploy [{}]: no deployed friends, anchoring on zone centre {}",
                  deployedUnit.getShortName(), anchor);
        } else {
            logger.debug("[MutualSupport] deploy [{}]: anchor {} from {} friend(s), formation radius {}",
                  deployedUnit.getShortName(), anchor, friendlyPositions.size(), formationRadius);
        }
        return orderByFormation(candidates, anchor, friendlyPositions, formationRadius);
    }

    /**
     * Positions of the friendly units that get a vote on where the formation is.
     *
     * <p>A friend counts only if it is genuinely on the board beside us: same board, actually deployed, and not flying
     * over the top of the fight. Airborne units cover ground they are not holding, so gathering a company on a
     * fighter's shadow would be misleading.</p>
     */
    static List<Coords> anchorPositions(Entity deployedUnit, List<Entity> friends, Game game) {
        List<Coords> positions = new ArrayList<>();
        for (Entity friend : friends) {
            if ((friend.getId() != deployedUnit.getId())
                  && friend.isDeployed()
                  && !friend.isOffBoard()
                  && (friend.getPosition() != null)
                  && !friend.isAirborne()
                  && game.onTheSameBoard(deployedUnit, friend)) {
                positions.add(friend.getPosition());
            }
        }
        return positions;
    }

    /**
     * Sorts candidates by how badly they miss the formation band.
     *
     * <p>The sort is deliberately stable and the key is deliberately blunt. Every hex in position scores zero, so the
     * whole in-position set keeps the caller's original (shuffled) order and the terrain ranking downstream picks among
     * them untouched. Only hexes that are out of position are pushed back, and they keep their relative order too, so a
     * force that cannot satisfy the band still degrades smoothly.</p>
     */
    static List<Coords> orderByFormation(List<Coords> candidates, @Nullable Coords anchor,
          List<Coords> friendlyPositions, int formationRadius) {
        if (anchor == null) {
            return candidates;
        }
        // Score once per hex rather than once per comparison: a large deployment zone against a full company is
        // tens of thousands of distance calls, and a comparator would repeat them for every comparison.
        record ScoredHex(Coords hex, int outOfSupport, int crowding) {}

        List<ScoredHex> scored = new ArrayList<>(candidates.size());
        for (Coords candidate : candidates) {
            scored.add(new ScoredHex(candidate,
                  outOfSupport(candidate, anchor, formationRadius),
                  crowding(candidate, friendlyPositions)));
        }
        scored.sort(Comparator.comparingInt(ScoredHex::outOfSupport).thenComparingInt(ScoredHex::crowding));

        List<Coords> ordered = new ArrayList<>(candidates.size());
        for (ScoredHex entry : scored) {
            ordered.add(entry.hex());
        }
        return ordered;
    }

    /**
     * Whether a candidate satisfies both ends of the band.
     *
     * <p>The two penalties are ranked, not added. Support comes first and spacing breaks ties within it, so the force
     * never trades away being concentrated in order to spread out. That ordering matters because the two constraints
     * genuinely conflict: a shallow deployment strip has nowhere near enough room for a full company to hold both a
     * tight radius and a clear hex between every pair, so one of them has to yield, and concentration is the thing this
     * rule exists to buy. Spacing then does the most it can within that - among hexes equally in support, the least
     * crowded is scanned first.</p>
     */
    static boolean isInPosition(Coords candidate, Coords anchor, List<Coords> friendlyPositions, int formationRadius) {
        return (outOfSupport(candidate, anchor, formationRadius) == 0) && (crowding(candidate, friendlyPositions) == 0);
    }

    /** How far beyond its supporting range of the force's centre a candidate sits; zero inside it. */
    static int outOfSupport(Coords candidate, Coords anchor, int formationRadius) {
        return Math.max(0, anchor.distance(candidate) - formationRadius);
    }

    /** How far inside the minimum spacing the nearest friend is; zero once there is room. */
    static int crowding(Coords candidate, List<Coords> friendlyPositions) {
        int worst = 0;
        for (Coords friend : friendlyPositions) {
            worst = Math.max(worst, FormationGeometry.MINIMUM_SPACING_HEXES - candidate.distance(friend));
        }
        return worst;
    }

}
