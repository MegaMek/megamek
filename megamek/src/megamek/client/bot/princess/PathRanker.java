/*
 * MegaMek - Copyright (C) 2000-2011 Ben Mazur (bmazur@sev.org)
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */
package megamek.client.bot.princess;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import megamek.client.ui.SharedUtility;
import megamek.common.Aero;
import megamek.common.Building;
import megamek.common.Compute;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.IGame;
import megamek.common.MovePath;
import megamek.common.MoveStep;
import megamek.common.TargetRoll;
import megamek.common.Targetable;
import megamek.common.logging.LogLevel;
import megamek.common.util.StringUtil;

public class PathRanker {

    private static Princess owner;

    public PathRanker(Princess princess) {
        owner = princess;
    }

    /**
     * Gives the "utility" of a path; a number representing how good it is.
     * Rankers that extend this class should override this function
     */
    public RankedPath rankPath(MovePath path, IGame game) {
        double fallTolerance = owner.getBehaviorSettings().getFallShameIndex() / 10d;
        Entity me = path.getEntity();
        int homeDistance = distanceToHomeEdge(me.getPosition(), owner.getHomeEdge(), game);
        int maxWeaponRange = me.getMaxWeaponRange();
        List<Entity> enemies = getEnemies(me, game);
        List<Entity> friends = getFriends(me, game);
        Coords allyCenter = calcAllyCenter(me.getId(), friends, game);

        return rankPath(path, game, maxWeaponRange, fallTolerance, homeDistance, enemies, allyCenter);
    }

    public RankedPath rankPath(MovePath path, IGame game, int maxRange, double fallTolerance, int distanceHome,
                               List<Entity> enemies, Coords friendsCoords) {
        return null;
    }

    public ArrayList<RankedPath> rankPaths(ArrayList<MovePath> movePaths, IGame game, int maxRange,
                                           double fallTollerance, int startingHomeDistance,
                                           int startingDistToNearestEnemy, List<Entity> enemies,
                                           List<Entity> friends) {
        final String METHOD_NAME = "rankPaths(ArrayList<MovePath>, IGame)";
        owner.methodBegin(getClass(), METHOD_NAME);

        try {
            // Let's try to whittle down this list.
            List<MovePath> validPaths = validatePaths(movePaths, game, maxRange, fallTollerance, startingHomeDistance);
            owner.log(getClass(), METHOD_NAME, LogLevel.INFO, "Validated " + validPaths.size() + " out of " +
                    movePaths.size() + " possible paths.");

            Coords allyCenter = calcAllyCenter(movePaths.get(0).getEntity().getId(), friends, game);

            ArrayList<RankedPath> returnPaths = new ArrayList<RankedPath>(validPaths.size());
            final BigDecimal numberPaths = new BigDecimal(validPaths.size());
            BigDecimal count = BigDecimal.ZERO;
            BigDecimal interval = new BigDecimal(5);
            for (MovePath path : validPaths) {
                count = count.add(BigDecimal.ONE);
                returnPaths.add(rankPath(path, game, maxRange, fallTollerance, startingHomeDistance, enemies,
                                         allyCenter));
                BigDecimal percent = count.divide(numberPaths, 2, RoundingMode.DOWN).multiply(new BigDecimal(100))
                                          .round(new MathContext(0, RoundingMode.DOWN));
                if ((percent.compareTo(interval) >= 0)
                        && (LogLevel.INFO.getLevel() <= owner.getVerbosity().getLevel())) {
                    owner.sendChat("... " + percent.intValue() + "% complete.");
                    interval = percent.add(new BigDecimal(5));
                }
            }
            return returnPaths;
        } finally {
            owner.methodEnd(getClass(), METHOD_NAME);
        }
    }

    private List<MovePath> validatePaths(List<MovePath> startingPathList, IGame game, int maxRange,
                                         double fallTolerance, int startingHomeDistance) {
        final String METHOD_NAME = "validatePaths(List<MovePath>, IGame, Targetable, int, double, int, int)";

        Entity mover = startingPathList.get(0).getEntity();

        // No support yet for Aero units.
        if (mover instanceof Aero) {
            return startingPathList;
        }

        Targetable closestTarget = findClosestEnemy(mover, mover.getPosition(), game);
        int startingTargetDistance = (closestTarget == null ?
                Integer.MAX_VALUE :
                closestTarget.getPosition().distance(mover.getPosition()));

        List<MovePath> returnPaths = new ArrayList<MovePath>(startingPathList.size());
        boolean inRange = (maxRange >= startingTargetDistance);
        HomeEdge homeEdge = owner.getHomeEdge();
        boolean fleeing = owner.isMustFlee() || owner.shouldFlee();

        for (MovePath path : startingPathList) {
            StringBuilder msg = new StringBuilder("Validating Path: ").append(path.toString());

            try {
                Coords finalCoords = path.getFinalCoords();

                // If fleeing, skip any paths that don't get me closer to home.
                if (fleeing && (distanceToHomeEdge(finalCoords, homeEdge, game) >= startingHomeDistance)) {
                    msg.append("\n  INVALID: Running away in wrong direction.");
                    continue;
                }

                // Make sure I'm trying to get/stay in range of a target.
                Targetable closestToEnd = findClosestEnemy(mover, finalCoords, game);
                String validation = validRange(finalCoords, closestToEnd, startingTargetDistance, maxRange, inRange);
                if (!StringUtil.isNullOrEmpty(validation)) {
                    msg.append("\n  ").append(validation);
                    continue;
                }

                // Don't move on/through buildings that will not support our weight.
                if (willBuildingCollapse(path, game)) {
                    msg.append("\n  INVALID: Building in path will collapse.");
                    continue;
                }

                // Skip any path where I am too likely to fail my piloting roll.
                if (getMovePathSuccessProbability(path) < fallTolerance) {
                    msg.append("\n  INVALID: Too likely to fall on my face.");
                    continue;
                }

                // If all the above checks have passed, this is a valid path.
                msg.append("\n  VALID.");
                returnPaths.add(path);

            } finally {
                owner.log(getClass(), METHOD_NAME, LogLevel.INFO, msg.toString());
            }
        }

        // If we've eliminated all valid paths, we'll just have to pick from the best of the invalid paths.
        if (returnPaths.isEmpty()) {
            return startingPathList;
        }

        return returnPaths;
    }

    public static ArrayList<RankedPath> filterPathsLessThan(ArrayList<RankedPath> ps, double lessthan) {
        final String METHOD_NAME = "filterPathsLessThan(ArrayList<Rankedpath>, double)";
        owner.methodBegin(PathRanker.class, METHOD_NAME);

        try {
            ArrayList<RankedPath> ret = new ArrayList<RankedPath>();
            for (RankedPath p : ps) {
                if (p.rank > lessthan) {
                    ret.add(p);
                }
            }
            return ret;
        } finally {
            owner.methodEnd(PathRanker.class, METHOD_NAME);
        }
    }

    public static RankedPath getBestPath(List<RankedPath> ps) {
        final String METHOD_NAME = "getBestPath(ArrayList<Rankedpath>)";
        owner.methodBegin(PathRanker.class, METHOD_NAME);

        try {
            if (ps.size() == 0) {
                return null;
            }
            return Collections.max(ps);
        } finally {
            owner.methodEnd(PathRanker.class, METHOD_NAME);
        }
    }


    /**
     * Performs initialization to help speed later calls of rankPath for this
     * unit on this turn. Rankers that extend this class should override this
     * function
     */
    public void initUnitTurn(Entity unit, IGame game) {
    }

    /**
     * Find the closest enemy to a unit with a path
     */
    Entity findClosestEnemy(Entity me, Coords position, IGame game) {
        final String METHOD_NAME = "findClosestEnemy(Entity, Coords, IGame)";
        owner.methodBegin(PathRanker.class, METHOD_NAME);

        try {
            int range = 9999;
            Entity closest = null;
            List<Entity> enemies = getEnemies(me, game);
            for (Entity e : enemies) {
                // Skip airborne aero units as they're further away than they seem and hard to catch.
                if (e instanceof Aero && e.isAirborne()) {
                    continue;
                }

                // If a unit has not moved, assume it will move away from me.
                int unmovedDistMod = 0;
                if (e.isSelectableThisTurn() && !e.isImmobile()) {
                    unmovedDistMod = e.getWalkMP(true, false, false);
                }

                if ((position.distance(e.getPosition()) + unmovedDistMod) < range) {
                    range = position.distance(e.getPosition());
                    closest = e;
                }
            }
            return closest;
        } finally {
            owner.methodEnd(PathRanker.class, METHOD_NAME);
        }
    }

    /**
     * Find the closest friend to a unit with a path
     */
    Entity findClosestFriend(MovePath p, IGame game) {
        final String METHOD_NAME = "findClosestFriend(MovePath, IGame";
        owner.methodBegin(getClass(), METHOD_NAME);

        try {
            int range = 9999;
            Entity closest = null;
            ArrayList<Entity> friends = getFriends(p.getEntity(), game);
            for (Entity e : friends) {
                if (p.getFinalCoords().distance(e.getPosition()) < range) {
                    range = p.getFinalCoords().distance(e.getPosition());
                    closest = e;
                }
            }
            return closest;
        } finally {
            owner.methodEnd(getClass(), METHOD_NAME);
        }
    }

    /**
     * Get all the enemies of a unit
     */
    List<Entity> getEnemies(Entity myunit, IGame game) {
        final String METHOD_NAME = "getEnemies(Entity, IGame)";
        owner.methodBegin(PathRanker.class, METHOD_NAME);

        try {
            ArrayList<Entity> enemies = new ArrayList<Entity>();
            for (Enumeration<Entity> i = game.getEntities(); i.hasMoreElements(); ) {
                Entity entity = i.nextElement();
                if (entity.getOwner().isEnemyOf(myunit.getOwner())
                        && (entity.getPosition() != null) && !entity.isOffBoard()) {
                    enemies.add(entity);
                }
            }
            return enemies;
        } finally {
            owner.methodEnd(PathRanker.class, METHOD_NAME);
        }
    }

    /**
     * Get all the friends of a unit
     */
    ArrayList<Entity> getFriends(Entity myunit, IGame game) {
        final String METHOD_NAME = "filterPathsLessThan(ArrayList<Rankedpath>, double)";
        owner.methodBegin(getClass(), METHOD_NAME);

        try {
            ArrayList<Entity> friends = new ArrayList<Entity>();
            for (Enumeration<Entity> i = game.getEntities(); i.hasMoreElements(); ) {
                Entity entity = i.nextElement();
                if (!entity.getOwner().isEnemyOf(myunit.getOwner())
                        && (entity.getPosition() != null) && !entity.isOffBoard()
                        && (!entity.equals(myunit))) {
                    friends.add(entity);
                }
            }
            return friends;
        } finally {
            owner.methodEnd(getClass(), METHOD_NAME);
        }
    }

    /**
     * Returns the probability of success of a movepath
     */
    public double getMovePathSuccessProbability(MovePath movePath) {
        MovePath pathCopy = movePath.clone();
        List<TargetRoll> pilotingRolls = getPSRList(pathCopy);
        double successProbability = 1.0;
        StringBuilder msg = new StringBuilder("Calculating Move Path Success");
        for (TargetRoll roll : pilotingRolls) {

            // Skip the getting up check.  That's handled when checking for being immobile.
            if (roll.getDesc().toLowerCase().contains("getting up")) {
                continue;
            }
            if (roll.getDesc().toLowerCase().contains("careful stand")) {
                continue;
            }

            msg.append("\n\tRoll ").append(roll.getDesc()).append(" ").append(roll.getValue());
            double odds = Compute.oddsAbove(roll.getValue()) / 100;
            msg.append(" (").append(NumberFormat.getPercentInstance().format(odds)).append(")");
            successProbability *= odds;
        }

        // Account for MASC
        if (pathCopy.hasActiveMASC()) {
            msg.append("\n\tMASC ");
            int target = pathCopy.getEntity().getMASCTarget();
            msg.append(target);
            double odds = Compute.oddsAbove(target) / 100;
            msg.append(" (").append(NumberFormat.getPercentInstance().format(odds)).append(")");
            successProbability *= (Compute.oddsAbove(pathCopy.getEntity().getMASCTarget()) / 100);
        }
        msg.append("\n\tTotal = ").append(NumberFormat.getPercentInstance().format(successProbability));

        LogLevel logLevel = (successProbability < 1.0 ? LogLevel.INFO : LogLevel.DEBUG);
        owner.log(getClass(), "getMovePathSuccessProbability(MovePath)", logLevel, msg.toString());

        return successProbability;
    }

    protected List<TargetRoll> getPSRList(MovePath path) {
        return SharedUtility.getPSRList(path);
    }

    public int distanceToHomeEdge(Coords position, HomeEdge homeEdge, IGame game) {
        final String METHOD_NAME = "distanceToHomeEdge(Coords, HomeEdge, IGame)";
        owner.methodBegin(getClass(), METHOD_NAME);

        try {
            Coords edgeCoords;
            int boardHeight = game.getBoard().getHeight();
            int boardWidth = game.getBoard().getWidth();
            StringBuilder msg = new StringBuilder("Getting distance to home edge: ");
            if (HomeEdge.NORTH.equals(homeEdge)) {
                msg.append("North");
                edgeCoords = new Coords(position.x, 0);
            } else if (HomeEdge.SOUTH.equals(homeEdge)) {
                msg.append("South");
                edgeCoords = new Coords(position.x, boardHeight);
            } else if (HomeEdge.WEST.equals(homeEdge)) {
                msg.append("West");
                edgeCoords = new Coords(0, position.y);
            } else if (HomeEdge.EAST.equals(homeEdge)) {
                msg.append("East");
                edgeCoords = new Coords(boardWidth, position.y);
            } else {
                msg.append("Default");
                owner.log(getClass(), METHOD_NAME, LogLevel.WARNING, "Invalid home edge.  Defaulting to NORTH.");
                edgeCoords = new Coords(boardWidth / 2, 0);
            }
            msg.append(edgeCoords.toFriendlyString());

            int distance = edgeCoords.distance(position);
            msg.append(" dist = ").append(NumberFormat.getInstance().format(distance));

            owner.log(getClass(), METHOD_NAME, LogLevel.DEBUG, msg.toString());
            return distance;
        } finally {
            owner.methodEnd(getClass(), METHOD_NAME);
        }
    }

    private String validRange(Coords finalCoords, Targetable target, int startingTargetDistance, int maxRange,
                              boolean inRange) {
        if (target == null) {
            return null;
        }

        // If I am not currently in range, discard any path that takes me further away from my target.
        int finalDistanceToTarget = finalCoords.distance(target.getPosition());
        if (!inRange) {
            if (finalDistanceToTarget > startingTargetDistance) {
                return "INVALID: Not in range and moving further away.";
            }

        } else { // If I am in range, discard any path that takes me out of range.
            if (finalDistanceToTarget > maxRange) {
                return "INVALID: In range and moving out of range.";
            }
        }

        return null;
    }

    /**
     * Check the path being moved to see if there is a danger of building collapse.  Allows a margin of error of 10
     * tons in case someone decides to shoot at the building.  If jumping, only the landing point is checked.  For
     * all other move types, the entire path is checked.
     * todo reread the rules on basement collapse
     * todo skip basement check if random basement option is turned off
     * todo incorporate test for building damage just from moving through building
     *
     * @param path The {@link MovePath} being traversed.
     * @param game The {@link IGame} being played.
     * @return True if there is a building in our path that might collapse.
     */
    protected boolean willBuildingCollapse(MovePath path, IGame game) {
        // If we're jumping onto a building, make sure it can support our weight.
        if (path.isJumping()) {
            Coords finalCoords = path.getFinalCoords();
            Building building = game.getBoard().getBuildingAt(finalCoords);
            if (building == null) {
                return false;
            }

            // Give ourselves a 10-ton margin of error in case someone shoots at the building.
            float mass = path.getEntity().getWeight() + 10;
            return (mass > building.getCurrentCF(finalCoords));
        }

        // If we're not jumping, check each building to see if it will collapse if it has a basement.
        float mass = path.getEntity().getWeight() + 10;
        Enumeration<MoveStep> steps = path.getSteps();
        while (steps.hasMoreElements()) {
            MoveStep step = steps.nextElement();
            Building building = game.getBoard().getBuildingAt(step.getPosition());
            if (building == null) {
                continue;
            }

            if (mass > building.getCurrentCF(step.getPosition())) {
                return true;
            }
        }
        return false;
    }

    public Coords calcAllyCenter(int myId, List<Entity> friends, IGame game) {
        if ((friends == null) || friends.isEmpty()) {
            return null;
        }

        int xTotal = 0;
        int yTotal = 0;
        int friendOnBoardCount = 0;

        for (Entity friend : friends) {
            if (friend.getId() == myId) {
                continue;
            }

            // Skip any friends not on the board.
            if (friend.isOffBoard()) {
                continue;
            }
            Coords friendPosition = friend.getPosition();
            if ((friendPosition == null) || !game.getBoard().contains(friendPosition)) {
                continue;
            }

            xTotal += friendPosition.x;
            yTotal += friendPosition.y;
            friendOnBoardCount++;
        }

        if (friendOnBoardCount == 0) {
            return null;
        }

        int xCenter = Math.round(xTotal / friendOnBoardCount);
        int yCenter = Math.round(yTotal / friendOnBoardCount);
        Coords center = new Coords(xCenter, yCenter);

        if (!game.getBoard().contains(center)) {
            owner.log(getClass(), "calcAllyCenter(int, List<Entity>, IGame)", LogLevel.ERROR, "Center of ally group " +
                    center.toFriendlyString() + " not within board boundaries.");
            return null;
        }

        return center;
    }

    public double distanceToClosestEnemy(Entity me, Coords position, IGame game) {
        return 0;
    }
}
