/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */

package megamek.client.bot.princess;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import megamek.client.commands.ClientCommand;
import megamek.common.Coords;
import megamek.common.Dropship;
import megamek.common.Entity;
import megamek.common.Game;
import megamek.common.IAero;
import megamek.common.MovePath;
import megamek.common.MovePath.MoveStepType;
import megamek.common.UnitType;
import megamek.logging.MMLogger;

/**
 * Helper class that contains functionality relating mostly to aero unit paths.
 *
 * @author NickAragua
 */
public class AeroPathUtil {
    private AeroPathUtil() {

    }

    private static final MMLogger logger = MMLogger.create(AeroPathUtil.class);

    protected static final List<List<MoveStepType>> TURNS = new ArrayList<>();

    static {
        // put together a pre-defined array of turns. Indexes correspond to the
        // directional values found in Coords.java
        TURNS.add(new ArrayList<>()); // "no turns"

        TURNS.add(new ArrayList<>());
        TURNS.get(1).add(MoveStepType.TURN_RIGHT);

        TURNS.add(new ArrayList<>());
        TURNS.get(2).add(MoveStepType.TURN_RIGHT);
        TURNS.get(2).add(MoveStepType.TURN_RIGHT);

        TURNS.add(new ArrayList<>());
        TURNS.get(3).add(MoveStepType.TURN_RIGHT);
        TURNS.get(3).add(MoveStepType.TURN_RIGHT);
        TURNS.get(3).add(MoveStepType.TURN_RIGHT);

        TURNS.add(new ArrayList<>());
        TURNS.get(4).add(MoveStepType.TURN_LEFT);
        TURNS.get(4).add(MoveStepType.TURN_LEFT);

        TURNS.add(new ArrayList<>());
        TURNS.get(5).add(MoveStepType.TURN_LEFT);
    }

    // Used to protect the TURNS List to ensure it can't be modified outside of the
    // class.
    public static List<List<MoveStepType>> getTurns() {
        return TURNS;
    }

    /**
     * Determines if the aircraft undertaking the given path will stall at the end
     * of the turn. Only relevant for aerodyne units
     *
     * @param movePath the path to check
     * @return whether the aircraft will stall at the end of the path
     */
    public static boolean willStall(MovePath movePath) {
        // Stalling only happens in atmospheres on ground maps
        if (!movePath.isOnAtmosphericGroundMap()) {
            return false;
        }

        // aircraft that are not VTOLs or spheroids will stall if the final velocity is
        // zero after all acc/dec aerodyne units can actually land or "vertical land"
        // and it's ok to do so (even though you're unlikely to find the 20 clear
        // spaces) spheroids will stall if they don't move or land

        boolean isAirborne = movePath.getEntity().isAirborne();
        boolean isSpheroid = UnitType.isSpheroidDropship(movePath.getEntity());

        if ((movePath.getFinalVelocity() == 0) && isAirborne && !isSpheroid) {
            return true;
        }

        return (isSpheroid && (movePath.getFinalNDown() == 0) &&
                (movePath.getMpUsed() == 0) &&
                !movePath.contains(MoveStepType.VLAND));
    }

    /**
     * Determines if the aircraft undertaking the given path will become a lawn dart
     *
     * @param movePath the path to check
     * @return True or false
     */
    public static boolean willCrash(MovePath movePath) {
        return movePath.getEntity().isAero() &&
                (movePath.getFinalAltitude() < 1) &&
                !movePath.contains(MoveStepType.VLAND) &&
                !movePath.contains(MoveStepType.LAND);
    }

    /**
     * A quick determination that checks the given path for the most common causes
     * of a PSR and whether it leads us off board. The idea being that a safe path
     * off board should not include any PSRs.
     *
     * @param movePath The path to check
     * @return True or false
     */
    public static boolean isSafePathOffBoard(MovePath movePath) {
        // common causes of PSR include, but are not limited to:
        // - stalling your aircraft
        // - crashing your aircraft into the ground
        // - executing maneuvers
        // - thrusting too hard
        // see your doctor if you experience any of these symptoms as it may lead to
        // your aircraft transforming into a lawn dart
        return !willStall(movePath) &&
                !willCrash(movePath) &&
                movePath.fliesOffBoard() &&
                !movePath.contains(MoveStepType.MANEUVER) &&
                (movePath.getMpUsed() <= movePath.getEntity().getWalkMP()) &&
                (movePath.getEntity().isAero() && (movePath.getMpUsed() <= ((IAero) movePath.getEntity()).getSI()));
    }

    /**
     * Generates paths that begin with all valid acceleration sequences for this
     * aircraft.
     *
     * @param startingPath The initial path, hopefully empty.
     * @return The child paths with all the accelerations this unit possibly can
     *         undertake.
     */
    public static Collection<MovePath> generateValidAccelerations(MovePath startingPath, int lowerBound,
            int upperBound) {
        Collection<MovePath> paths = new ArrayList<>();

        // sanity check: if we've already done something else with the path, there's no
        // acceleration to be done
        if (startingPath.length() > 0) {
            return paths;
        }

        int currentVelocity = startingPath.getFinalVelocity();

        // we go from the lower bound to the current velocity and generate paths with
        // the required number of DECs to get to the desired velocity
        for (int desiredVelocity = lowerBound; desiredVelocity < currentVelocity; desiredVelocity++) {
            MovePath path = startingPath.clone();

            for (int deltaVelocity = 0; deltaVelocity < currentVelocity - desiredVelocity; deltaVelocity++) {
                path.addStep(MoveStepType.DEC);
            }

            paths.add(path);
        }

        // If the unaltered starting path is within acceptable velocity bounds, it's
        // also a valid "acceleration".
        if (startingPath.getFinalVelocity() <= upperBound &&
                startingPath.getFinalVelocity() >= lowerBound) {
            paths.add(startingPath.clone());
        }

        // we go from the current velocity to the upper bound and generate paths with
        // the required number of DECs to get to the desired velocity
        for (int desiredVelocity = currentVelocity; desiredVelocity < upperBound; desiredVelocity++) {
            MovePath path = startingPath.clone();

            for (int deltaVelocity = 0; deltaVelocity < upperBound - desiredVelocity; deltaVelocity++) {
                path.addStep(MoveStepType.ACC);
            }

            paths.add(path);
        }

        return paths;
    }

    /**
     * Helper function to calculate the maximum thrust we should use for a
     * particular aircraft We limit ourselves to the lowest of "safe thrust" and
     * "structural integrity", as anything further is unsafe, meaning it requires a
     * PSR.
     *
     * @param aero The aero entity for which to calculate max thrust.
     * @return The max thrust.
     */
    public static int calculateMaxSafeThrust(IAero aero) {
        return Math.min(aero.getCurrentThrust(), aero.getSI()); // we should only thrust up to our SI
    }

    /**
     * Given a move path, generate all possible increases and decreases in
     * elevation.
     *
     * @param path The move path to process.
     * @return Collection of generated paths.
     */
    public static List<MovePath> generateValidAltitudeChanges(MovePath path) {
        List<MovePath> paths = new ArrayList<>();

        // clone path add UP
        // if path uses more MP than entity has available or altitude higher than 10,
        // stop
        for (int altChange = 0;; altChange++) {
            int altChangeCost = altChange * 2;

            // if we are going to attempt to change altitude but won't actually be able to,
            // break out.
            if ((path.getFinalAltitude() + altChange > 10) ||
                    path.getMpUsed() + altChangeCost > path.getEntity().getRunMP()) {
                break;
            }

            MovePath childPath = path.clone();

            for (int numSteps = 0; numSteps < altChange; numSteps++) {
                childPath.addStep(MoveStepType.UP);
            }

            if ((childPath.getFinalAltitude() > 10) || childPath.getMpUsed() > path.getEntity().getRunMP()) {
                break;
            }

            paths.add(childPath);
        }

        // clone path add DOWN
        // if the path is already at minimum altitude, skip this
        // if path uses more MP than entity has available or altitude lower than 1, stop
        if (path.getFinalAltitude() > 1) {
            for (int altChange = 1;; altChange++) {
                MovePath childPath = path.clone();

                for (int numSteps = 0; numSteps < altChange; numSteps++) {
                    childPath.addStep(MoveStepType.DOWN);
                }

                // going down doesn't use MP, but if we drop down more than 2 altitude it causes
                // a massive difficulty PSR, which is just not worth it.
                if ((childPath.getFinalAltitude() < 1) || childPath.length() > 2) {
                    break;
                }

                paths.add(childPath);
            }
        }

        return paths;
    }

    /**
     * Given a move path, generates all possible rotations from it, without any
     * regard to legality. Mostly because it's intended for spheroid DropShips in
     * atmosphere, which can rotate as much as they want.
     */
    public static List<MovePath> generateValidRotations(MovePath path) {
        List<MovePath> childPaths = new ArrayList<>();

        for (int x = 1; x < TURNS.size(); x++) {
            MovePath childPath = path.clone();

            for (MoveStepType turn : TURNS.get(x)) {
                childPath.addStep(turn);
            }

            childPaths.add(childPath);
        }

        return childPaths;
    }

    public static int getSpheroidDir(Game game, Entity mover) {
        logger.debug("Deciding where to point %s...", mover.getDisplayName());

        // Face the center of the board
        int dir = mover.getPosition().direction(game.getBoard().getCenter());
        logger.debug("Map center is to the %s", ClientCommand.getDirection(dir));

        int enemyDir = dir;

        // Get all enemies, find centroid, face that.
        final Coords centroid;
        ArrayList<Entity> enemies = new ArrayList<>();
        Iterator<Entity> eIt = game.getAllEnemyEntities(mover);
        while (eIt.hasNext()) {
            enemies.add(eIt.next());
        }
        if (!enemies.isEmpty()) {
            // Calc center of allies _of the enemy_
            centroid = PathRanker.calcAllyCenter(enemies.get(0).getId(), enemies, game);
            enemyDir = mover.getPosition().direction(centroid);
            logger.debug("Enemies are over in %s", ClientCommand.getDirection(enemyDir));
        }

        // Then determine if we need to protect part of the ship
        if (mover.getDamageLevel() == Entity.DMG_NONE) {
            dir = enemyDir;
            logger.debug("Being hale and hearty, we will aim toward the %s", ClientCommand.getDirection(dir));
        } else {
            int leastArmor = 9999999;
            int leastLoc = Dropship.LOC_NONE;

            for (int i = Dropship.LOC_NOSE; i <= Dropship.LOC_FUSELAGE; i++) {
                if (mover.getArmor(i) < leastArmor) {
                    leastArmor = mover.getArmor(i);
                    leastLoc = i;
                }
            }

            if (leastLoc != Dropship.LOC_NONE) {
                // Turn away, turn away, turn away, from enemy!
                switch (leastLoc) {
                    case Dropship.LOC_NOSE:
                        // Turn 180, face away from enemy.
                        dir = (enemyDir + 3) % 6;
                    case Dropship.LOC_LWING:
                        // Turn left two hex sides.
                        dir = (enemyDir + 4) % 6;
                    case Dropship.LOC_RWING:
                        // Turn right two hex sides.
                        dir = (enemyDir + 2) % 6;
                    case Dropship.LOC_AFT:
                    case Dropship.LOC_WINGS:
                    case Dropship.LOC_FUSELAGE:
                        // Default is nose to enemy. If this should be different, suggest using NOSE
                        // case.
                        dir = enemyDir;
                }
                logger.debug("We've taken a lot of damage to our %s", mover.getLocationAbbr(leastLoc));
                logger.debug("nTurning to the %s to protect ourselves!", ClientCommand.getDirection(dir));
            }
        }

        return dir;
    }
}
