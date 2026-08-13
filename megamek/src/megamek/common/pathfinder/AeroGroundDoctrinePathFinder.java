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
package megamek.common.pathfinder;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import megamek.common.enums.MoveStepType;
import megamek.common.equipment.enums.BombType;
import megamek.common.game.Game;
import megamek.common.moves.MovePath;
import megamek.common.units.Entity;

/**
 * Ground-mapsheet aerospace paths that offer the ranker an altitude to choose from.
 *
 * <p>The stock finder drives every path it generates to a single altitude - {@code OPTIMAL_STRIKE_ALTITUDE},
 * 5 - so however good a ranker is, it never sees an alternative and altitude is settled before scoring
 * begins. Its author left the intent in a comment next to the call: <i>"repeat with 1, 3, 7 when we settle
 * things down?"</i></p>
 *
 * <p>Altitude is the one thing a fighter over a ground mapsheet can actually change inside a turn. It must
 * fly eight to fifty-two hexes in a straight line between single-hexside facing changes (TW p.92), so a
 * velocity-3 fighter crossing the board gets about three turns; it can change altitude every hex. Fixing
 * altitude at a constant removes the only responsive control it has - and, because the air-to-air dead zone
 * is measured in low-altitude hexes, one level of mismatch blocks all fire within seventeen ground hexes.</p>
 *
 * <h2>Keeping the path count down</h2>
 *
 * <p>Every extra candidate multiplies an already large path set, so two things bound it. The candidate list
 * is short and intent-driven rather than a sweep of altitudes 1 to 10, and the generated paths are deduplicated
 * by the altitude they actually reach. That second check does most of the work: the stock altitude adjustment
 * will only climb as far as thrust allows and will only descend one level per turn, so several distinct
 * wishes routinely collapse onto the same reachable altitude and only one copy survives.</p>
 */
public class AeroGroundDoctrinePathFinder extends AeroGroundPathFinder {

    /**
     * Ceiling on distinct altitudes attempted per path.
     *
     * <p>Three covers the cases that matter - hold the strike altitude, meet an opponent who has committed to
     * a different one, and drop to the deck to use bombs - without turning path generation into a sweep.</p>
     */
    static final int MAXIMUM_CANDIDATE_ALTITUDES = 3;

    protected AeroGroundDoctrinePathFinder(Game game) {
        super(game);
    }

    public static AeroGroundDoctrinePathFinder getInstance(Game game) {
        return new AeroGroundDoctrinePathFinder(game);
    }

    /**
     * Produces each path at every candidate altitude it can actually reach, instead of only at the strike
     * altitude.
     */
    @Override
    protected List<MovePath> getAltitudeAdjustedPaths(List<MovePath> startingPaths) {
        List<MovePath> adjustedPaths = new ArrayList<>();

        for (MovePath start : startingPaths) {
            boolean choppedOffFlyOff = false;

            // Going off board needs the tail chopped before altitude is applied, and put back after.
            if (start.fliesOffBoard()) {
                start.removeLastStep();
                choppedOffFlyOff = true;
            }

            Set<Integer> altitudesReached = new LinkedHashSet<>();
            for (int desiredAltitude : candidateAltitudes(start.getEntity())) {
                MovePath candidate = adjustTowardsDesiredAltitude(start, desiredAltitude);
                // Several wishes collapse onto one reachable altitude; keep the first and drop the rest.
                if (!altitudesReached.add(candidate.getFinalAltitude())) {
                    continue;
                }
                if (choppedOffFlyOff) {
                    candidate.addStep(MoveStepType.RETURN);
                }
                adjustedPaths.add(candidate);
            }
        }

        return adjustedPaths;
    }

    /**
     * The altitudes worth trying for this unit, most useful first.
     *
     * <p>Ordered by how much is known. An opponent that has already moved has committed to an altitude for
     * the turn, so matching it is the one choice guaranteed to produce an engagement; that comes first. The
     * strike altitude is the fallback that works when nothing is known. Nap-of-the-earth is only worth
     * generating for a unit carrying something to drop, since the stock ranker discards low paths from an
     * aircraft with no bombs anyway.</p>
     *
     * @param mover the unit whose paths are being generated
     *
     * @return distinct candidate altitudes, at most {@value #MAXIMUM_CANDIDATE_ALTITUDES} of them
     */
    protected List<Integer> candidateAltitudes(Entity mover) {
        Set<Integer> altitudes = new LinkedHashSet<>();

        for (int committedAltitude : committedEnemyAltitudes(mover)) {
            altitudes.add(committedAltitude);
            if (altitudes.size() >= MAXIMUM_CANDIDATE_ALTITUDES) {
                break;
            }
        }

        altitudes.add(OPTIMAL_STRIKE_ALTITUDE);

        if (carriesGroundBombs(mover)) {
            altitudes.add(NAP_OF_THE_EARTH);
        }

        List<Integer> candidates = new ArrayList<>(altitudes);
        if (candidates.size() > MAXIMUM_CANDIDATE_ALTITUDES) {
            return candidates.subList(0, MAXIMUM_CANDIDATE_ALTITUDES);
        }
        return candidates;
    }

    /**
     * Altitudes held by enemy aircraft that have already moved this turn, nearest to the mover first.
     *
     * <p>Only units that have moved are consulted. An opponent still to move may be anywhere in its own
     * reachable band by the time the shooting starts, so matching where it happens to be sitting now is not
     * reacting to anything - it is committing to a guess.</p>
     *
     * <p>Board-filtered, because the game's entity lists span every board and an aircraft over a different
     * map contributes an altitude that means nothing here.</p>
     */
    private List<Integer> committedEnemyAltitudes(Entity mover) {
        List<Entity> committed = new ArrayList<>();
        Iterator<Entity> enemies = game.getAllEnemyEntities(mover);
        while (enemies.hasNext()) {
            Entity enemy = enemies.next();
            if (!enemy.isAero() || !enemy.isAirborne() || (enemy.getPosition() == null)) {
                continue;
            }
            if (enemy.getBoardId() != mover.getBoardId()) {
                continue;
            }
            if (enemy.isSelectableThisTurn() && !enemy.isImmobile()) {
                continue;
            }
            committed.add(enemy);
        }

        if ((committed.size() > 1) && (mover.getPosition() != null)) {
            committed.sort((left, right) -> Integer.compare(mover.getPosition().distance(left.getPosition()),
                  mover.getPosition().distance(right.getPosition())));
        }

        List<Integer> altitudes = new ArrayList<>();
        for (Entity enemy : committed) {
            altitudes.add(enemy.getAltitude());
        }
        return altitudes;
    }

    /**
     * @param mover the unit to check
     *
     * @return {@code true} if the unit still has bombs it could drop on a ground target
     */
    private boolean carriesGroundBombs(Entity mover) {
        return !mover.getBombs(BombType.F_GROUND_BOMB).isEmpty();
    }
}
