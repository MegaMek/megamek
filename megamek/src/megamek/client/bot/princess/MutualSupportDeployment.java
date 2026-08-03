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

/**
 * Deployment half of the Mutual Support doctrine: get a company onto the board as a formation rather than as twelve
 * unrelated units.
 *
 * <p>Stock deployment ranks a hex on terrain hazard and on how much open ground surrounds it, and never once looks at
 * where the rest of the force went. Worse, the candidate list arrives shuffled ({@code BotClient.getStartingCoordsArray}
 * randomizes it to stop units piling into the upper-left corner of flat maps) and
 * {@link Princess#rankDeploymentCoords(Entity, List)} then scans only the first twenty or so entries. Each unit is
 * therefore choosing the best of a small random sample drawn from the whole deployment zone, which is close to an
 * independent uniform draw per unit. Measured on a 32-wide board, a twelve-unit company lands about 26 hexes wide and
 * never closes up.</p>
 *
 * <p>The fix is an ordering, not a score. Candidates are sorted by how far outside the formation they sit, so the capped
 * scan sees hexes near the force instead of a random spread. Everything already inside {@link #FORMATION_RADIUS_HEXES}
 * of the anchor ties at zero and keeps its shuffled order, which leaves the existing hazard and open-ground ranking free
 * to pick among them exactly as before. Cohesion decides <em>which part of the zone</em> to consider; terrain still
 * decides the hex.</p>
 *
 * <p>This cannot slow the advance, which is the standing constraint on the whole doctrine: it changes where a unit
 * starts, never where it may move afterwards, and a concentrated company starts closer to its own centre of mass than a
 * dispersed one does.</p>
 *
 * @see MutualSupportPathRanker the movement half of the same doctrine
 */
public final class MutualSupportDeployment {

    /**
     * How far from the formation anchor a unit may deploy before it is treated as out of position, in hexes.
     *
     * <p>This is an upper bound on the formation, not a target frontage, and the two can differ a lot. The caller scans
     * only about twenty candidates, so when more than that many hexes tie at zero the tie set is truncated and the
     * force ends up tighter than the radius alone implies. On a shallow deployment strip a radius of 6 covers roughly
     * 26 hexes, more than the scan will reach, and the measured company frontage came out near 7 hexes rather than the
     * 13 the geometry suggests. Lower this only to tighten the force further; to loosen it, the scan limit in
     * {@link Princess#rankDeploymentCoords(Entity, List)} is the binding constraint.</p>
     */
    static final int FORMATION_RADIUS_HEXES = 6;

    private MutualSupportDeployment() {
    }

    /**
     * Reorders candidate deployment hexes so the ones that keep the force together are scanned first.
     *
     * @param deployedUnit the unit being placed
     * @param candidates   legal deployment hexes, in the order the caller would otherwise scan them
     * @param friends      friendly units, deployed or not, from {@code BotClient.getFriendEntities()}
     * @param game         the game, used to keep the anchor to units sharing a board with the deploying unit
     *
     * @return the candidates, reordered; the same list instance when there is nothing to reorder
     */
    public static List<Coords> prioritize(Entity deployedUnit, List<Coords> candidates, List<Entity> friends,
          Game game) {
        if (candidates.size() < 2) {
            return candidates;
        }
        return orderByFormation(candidates, formationAnchor(deployedUnit, candidates, friends, game));
    }

    /**
     * Picks the point the formation should gather on.
     *
     * <p>The anchor is the centre of mass of the units already on the board. The first unit of a force has no such
     * centre, so it anchors on the middle of its own deployment zone: that seeds the formation on the force's axis of
     * advance instead of wherever the shuffle happened to look first, and stops one unlucky corner placement from
     * dragging the whole company into it.</p>
     */
    static @Nullable Coords formationAnchor(Entity deployedUnit, List<Coords> candidates, List<Entity> friends,
          Game game) {
        List<Coords> anchorPoints = new ArrayList<>();
        for (Entity friend : friends) {
            if (isEligibleAnchor(deployedUnit, friend, game)) {
                anchorPoints.add(friend.getPosition());
            }
        }
        return centroid(anchorPoints.isEmpty() ? candidates : anchorPoints);
    }

    /**
     * A friend anchors the formation only if it is genuinely on the board next to us: same board, actually deployed,
     * and not flying over the top of the fight. Airborne units cover ground they are not holding, so gathering the
     * company on a fighter's shadow would be misleading.
     */
    private static boolean isEligibleAnchor(Entity deployedUnit, Entity friend, Game game) {
        return (friend.getId() != deployedUnit.getId())
              && friend.isDeployed()
              && !friend.isOffBoard()
              && (friend.getPosition() != null)
              && !friend.isAirborne()
              && game.onTheSameBoard(deployedUnit, friend);
    }

    /**
     * Sorts candidates by how far outside the formation radius they lie.
     *
     * <p>The sort is deliberately stable and the key is deliberately blunt. Every hex inside the radius scores zero, so
     * the whole in-formation set keeps the caller's original (shuffled) order and the terrain ranking downstream picks
     * among them untouched. Only genuinely out-of-position hexes are pushed back, and they keep their relative order
     * too, so a force that cannot fit inside the radius still degrades smoothly outwards.</p>
     */
    static List<Coords> orderByFormation(List<Coords> candidates, @Nullable Coords anchor) {
        if (anchor == null) {
            return candidates;
        }
        List<Coords> ordered = new ArrayList<>(candidates);
        ordered.sort(Comparator.comparingInt(candidate -> distanceOutsideFormation(anchor, candidate)));
        return ordered;
    }

    /**
     * Hexes inside the formation radius are all equally good; beyond it, closer is better.
     */
    static int distanceOutsideFormation(Coords anchor, Coords candidate) {
        return Math.max(0, anchor.distance(candidate) - FORMATION_RADIUS_HEXES);
    }

    /**
     * Centre of mass of a set of hexes, rounded to the nearest hex.
     */
    static @Nullable Coords centroid(List<Coords> positions) {
        if (positions.isEmpty()) {
            return null;
        }
        long totalX = 0;
        long totalY = 0;
        for (Coords position : positions) {
            totalX += position.getX();
            totalY += position.getY();
        }
        return new Coords(Math.round((float) totalX / positions.size()),
              Math.round((float) totalY / positions.size()));
    }
}
