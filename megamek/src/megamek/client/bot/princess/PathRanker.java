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
import megamek.common.BombType;
import megamek.common.Building;
import megamek.common.Compute;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.IGame;
import megamek.common.MovePath;
import megamek.common.MoveStep;
import megamek.common.TargetRoll;
import megamek.common.Targetable;
import megamek.common.annotations.Nullable;
import megamek.common.logging.LogLevel;
import megamek.common.options.OptionsConstants;
import megamek.common.util.StringUtil;

public abstract class PathRanker implements IPathRanker {

    //TODO: Introduce PathRankerCacheHelper class that contains "global" path ranker state
    //TODO: Introduce FireControlCacheHelpr class that contains "global" Fire Control state
    //PathRanker classes should be pretty stateless, except pointers to princess and such
    
    /**
     * The possible path ranker types.
     * If you're adding a new one, add it here then make sure to add it to Princess.InitializePathRankers
     */
    public enum PathRankerType {
        Basic,
        Infantry,
        NewtonianAerospace
    }
    
    private Princess owner;

    public PathRanker(Princess princess) {
        owner = princess;
    }

    /**
     * Gives the "utility" of a path; a number representing how good it is.
     * Rankers that extend this class should override this function
     */
    RankedPath rankPath(MovePath path, IGame game) {
        double fallTolerance = getOwner().getBehaviorSettings().getFallShameIndex() / 10d;
        Entity me = path.getEntity();
        int homeDistance = distanceToHomeEdge(me.getPosition(), getOwner().getHomeEdge(me), game);
        int maxWeaponRange = me.getMaxWeaponRange();
        List<Entity> enemies = getOwner().getEnemyEntities();
        List<Entity> friends = getOwner().getFriendEntities();
        Coords allyCenter = calcAllyCenter(me.getId(), friends, game);

        return rankPath(path, game, maxWeaponRange, fallTolerance, homeDistance, enemies, allyCenter);
    }

    abstract RankedPath rankPath(MovePath path, IGame game, int maxRange, double fallTolerance, int distanceHome,
                               List<Entity> enemies, Coords friendsCoords);

    public ArrayList<RankedPath> rankPaths(List<MovePath> movePaths, IGame game, int maxRange,
                                    double fallTolerance, int startingHomeDistance,
                                    List<Entity> enemies, List<Entity> friends) {
        final String METHOD_NAME = "rankPaths(ArrayList<MovePath>, IGame)";
        getOwner().methodBegin(getClass(), METHOD_NAME);

        try {
            // No point in ranking an empty list.
            if (movePaths.isEmpty()) {
                return new ArrayList<>();
            }

            // the cached path probability data is really only relevant for one iteration through this method
            getPathRankerState().getPathSuccessProbabilities().clear();
            
            // Let's try to whittle down this list.
            List<MovePath> validPaths = validatePaths(movePaths, game, maxRange, fallTolerance, startingHomeDistance);
            getOwner().log(getClass(), METHOD_NAME, LogLevel.DEBUG, "Validated " + validPaths.size() + " out of " +
                                                               movePaths.size() + " possible paths.");

            Coords allyCenter = calcAllyCenter(movePaths.get(0).getEntity().getId(), friends, game);

            ArrayList<RankedPath> returnPaths = new ArrayList<>(validPaths.size());
            final BigDecimal numberPaths = new BigDecimal(validPaths.size());
            BigDecimal count = BigDecimal.ZERO;
            BigDecimal interval = new BigDecimal(5);
            for (MovePath path : validPaths) {
                count = count.add(BigDecimal.ONE);
                returnPaths.add(rankPath(path, game, maxRange, fallTolerance, startingHomeDistance, enemies,
                                         allyCenter));
                BigDecimal percent = count.divide(numberPaths, 2, RoundingMode.DOWN).multiply(new BigDecimal(100))
                                          .round(new MathContext(0, RoundingMode.DOWN));
                if (percent.compareTo(interval) >= 0) {
                    getOwner().sendChat("... " + percent.intValue() + "% complete.", LogLevel.INFO);
                    interval = percent.add(new BigDecimal(5));
                }
            }
            return returnPaths;
        } finally {
            getOwner().methodEnd(getClass(), METHOD_NAME);
        }
    }

    private List<MovePath> validatePaths(List<MovePath> startingPathList, IGame game, int maxRange,
                                         double fallTolerance, int startingHomeDistance) {
        final String METHOD_NAME = "validatePaths(List<MovePath>, IGame, Targetable, int, double, int, int)";
        LogLevel logLevel = LogLevel.DEBUG;

        if (startingPathList.isEmpty()) {
            // Nothing to validate here, might as well return the empty list
            // straight away.
            return startingPathList;
        }

        Entity mover = startingPathList.get(0).getEntity();

        Targetable closestTarget = findClosestEnemy(mover, mover.getPosition(), game);
        int startingTargetDistance = (closestTarget == null ?
                                      Integer.MAX_VALUE :
                                      closestTarget.getPosition().distance(mover.getPosition()));

        List<MovePath> returnPaths = new ArrayList<>(startingPathList.size());
        boolean inRange = (maxRange >= startingTargetDistance);
        CardinalEdge homeEdge = getOwner().getHomeEdge(mover);
        boolean fleeing = getOwner().isFallingBack(mover);
        
        boolean isAirborneAeroOnGroundMap = mover.isAirborneAeroOnGroundMap();
        
        for (MovePath path : startingPathList) {
            // just in case
            if((path == null) || !path.isMoveLegal()) {
                continue;
            }

            StringBuilder msg = new StringBuilder("Validating Path: ").append(path.toString());

            try {
                // if we are an aero unit on the ground map, we want to discard paths that keep us at altitude 1 with no bombs
            	if(isAirborneAeroOnGroundMap) {
            		// if we have no bombs, we want to make sure our altitude is above 1
            		// if we do have bombs, we may consider altitude bombing (in the future)
            		if((path.getEntity().getBombs(BombType.F_GROUND_BOMB).size() == 0) &&
            		        (path.getFinalAltitude() < 2)) {
            		    msg.append("\n\tNo bombs but at altitude 1. No way.");
            		    continue;
            		}
            	}
            	
                Coords finalCoords = path.getFinalCoords();

                // If fleeing, skip any paths that don't get me closer to home.
                if (fleeing && (distanceToHomeEdge(finalCoords, homeEdge, game) >= startingHomeDistance)) {
                    logLevel = LogLevel.INFO;
                    msg.append("\n\tINVALID: Running away in wrong direction.");
                    continue;
                }

                // Make sure I'm trying to get/stay in range of a target.
                // Skip this part if I'm an aero on the ground map, as it's kind of irrelevant
                if(!isAirborneAeroOnGroundMap) {
                    Targetable closestToEnd = findClosestEnemy(mover, finalCoords, game);
                    String validation = validRange(finalCoords, closestToEnd, startingTargetDistance, maxRange, inRange);
                    if (!StringUtil.isNullOrEmpty(validation)) {
                        msg.append("\n\t").append(validation);
                        continue;
                    }
                }

                // Don't move on/through buildings that will not support our weight.
                if (willBuildingCollapse(path, game)) {
                    logLevel = LogLevel.INFO;
                    msg.append("\n\tINVALID: Building in path will collapse.");
                    continue;
                }

                // Skip any path where I am too likely to fail my piloting roll.
                double chance = getMovePathSuccessProbability(path, msg);
                if (chance < fallTolerance) {
                    logLevel = LogLevel.INFO;
                    msg.append("\n\tINVALID: Too likely to fall on my face.");
                    continue;
                }

                // If all the above checks have passed, this is a valid path.
                msg.append("\n\tVALID.");
                returnPaths.add(path);
            } finally {
                getOwner().log(getClass(), METHOD_NAME, logLevel, msg.toString());
            }
        }

        // If we've eliminated all valid paths, we'll just have to pick from the best of the invalid paths.
        if (returnPaths.isEmpty()) {
            return startingPathList;
        }

        return returnPaths;
    }

    /**
     * Returns the best path of a list of ranked paths.
     * 
     * @param ps The list of ranked paths to process
     * @return "Best" out of those paths
     */
    public RankedPath getBestPath(List<RankedPath> ps) {
        final String METHOD_NAME = "getBestPath(ArrayList<Rankedpath>)";
        getOwner().methodBegin(PathRanker.class, METHOD_NAME);

        try {
            if (ps.size() == 0) {
                return null;
            }
            return Collections.max(ps);
        } finally {
            getOwner().methodEnd(PathRanker.class, METHOD_NAME);
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
    public Entity findClosestEnemy(Entity me, Coords position, IGame game) {
        final String METHOD_NAME = "findClosestEnemy(Entity, Coords, IGame)";
        getOwner().methodBegin(PathRanker.class, METHOD_NAME);

        try {
            int range = 9999;
            Entity closest = null;
            List<Entity> enemies = getOwner().getEnemyEntities();
            for (Entity e : enemies) {
                // Skip airborne aero units as they're further away than they seem and hard to catch.
                // Also, skip withdrawing enemy bot units, to avoid humping disabled tanks and ejected mechwarriors
                if (e.isAirborneAeroOnGroundMap() || 
                        getOwner().getHonorUtil().isEnemyBroken(e.getTargetId(), e.getOwnerId(), getOwner().getForcedWithdrawal())) {
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
            getOwner().methodEnd(PathRanker.class, METHOD_NAME);
        }
    }

    /**
     * Returns the probability of success of a move path
     */
    protected double getMovePathSuccessProbability(MovePath movePath, StringBuilder msg) {
        // introduced a caching mechanism, as the success probability was being calculated at least twice
        if(getPathRankerState().getPathSuccessProbabilities().containsKey(movePath.getKey())) {
            return getPathRankerState().getPathSuccessProbabilities().get(movePath.getKey());
        }
        
        MovePath pathCopy = movePath.clone();
        List<TargetRoll> pilotingRolls = getPSRList(pathCopy);
        double successProbability = 1.0;
        msg.append("\n\tCalculating Move Path Success");
        for (TargetRoll roll : pilotingRolls) {

            // Skip the getting up check.  That's handled when checking for being immobile.
            if (roll.getDesc().toLowerCase().contains("getting up")) {
                continue;
            }
            if (roll.getDesc().toLowerCase().contains("careful stand")) {
                continue;
            }
            boolean naturalAptPilot = movePath.getEntity().hasAbility(OptionsConstants.PILOT_APTITUDE_PILOTING);
            if (naturalAptPilot) {
                msg.append("\n\t\tPilot has Natural Aptitude Piloting");
            }

            msg.append("\n\t\tRoll ").append(roll.getDesc()).append(" ").append(roll.getValue());
            double odds = Compute.oddsAbove(roll.getValue(), naturalAptPilot) / 100;
            msg.append(" (").append(NumberFormat.getPercentInstance().format(odds)).append(")");
            successProbability *= odds;
        }

        // Account for MASC
        if (pathCopy.hasActiveMASC()) {
            msg.append("\n\t\tMASC ");
            int target = pathCopy.getEntity().getMASCTarget();
            msg.append(target);
            // todo Does Natural Aptitude Piloting apply to this?  I assume not.
            double odds = Compute.oddsAbove(target) / 100;
            msg.append(" (").append(NumberFormat.getPercentInstance().format(odds)).append(")");
            successProbability *= odds;
        }
        msg.append("\n\t\tTotal = ").append(NumberFormat.getPercentInstance().format(successProbability));

        getPathRankerState().getPathSuccessProbabilities().put(movePath.getKey(), successProbability);
        
        return successProbability;
    }

    protected List<TargetRoll> getPSRList(MovePath path) {
        return SharedUtility.getPSRList(path);
    }

    public int distanceToHomeEdge(Coords position, CardinalEdge homeEdge, IGame game) {
        final String METHOD_NAME = "distanceToHomeEdge(Coords, HomeEdge, IGame)";
        getOwner().methodBegin(getClass(), METHOD_NAME);

        try {
            Coords edgeCoords;
            int boardHeight = game.getBoard().getHeight();
            int boardWidth = game.getBoard().getWidth();
            StringBuilder msg = new StringBuilder("Getting distance to home edge: ");
            if (CardinalEdge.NORTH.equals(homeEdge)) {
                msg.append("North");
                edgeCoords = new Coords(position.getX(), 0);
            } else if (CardinalEdge.SOUTH.equals(homeEdge)) {
                msg.append("South");
                edgeCoords = new Coords(position.getX(), boardHeight);
            } else if (CardinalEdge.WEST.equals(homeEdge)) {
                msg.append("West");
                edgeCoords = new Coords(0, position.getY());
            } else if (CardinalEdge.EAST.equals(homeEdge)) {
                msg.append("East");
                edgeCoords = new Coords(boardWidth, position.getY());
            } else {
                msg.append("Default");
                getOwner().log(getClass(), METHOD_NAME, LogLevel.WARNING, "Invalid home edge.  Defaulting to NORTH.");
                edgeCoords = new Coords(boardWidth / 2, 0);
            }
            msg.append(edgeCoords.toFriendlyString());

            int distance = edgeCoords.distance(position);
            msg.append(" dist = ").append(NumberFormat.getInstance().format(distance));

            getOwner().log(getClass(), METHOD_NAME, LogLevel.DEBUG, msg.toString());
            return distance;
        } finally {
            getOwner().methodEnd(getClass(), METHOD_NAME);
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
    private boolean willBuildingCollapse(MovePath path, IGame game) {
        // airborne aircraft cannot collapse buildings
        if(path.getEntity().isAero() || path.getEntity().hasETypeFlag(Entity.ETYPE_VTOL)) {
            return false;
        }
        
        // If we're jumping onto a building, make sure it can support our weight.
        if (path.isJumping()) {
            final Coords finalCoords = path.getFinalCoords();
            final Building building = game.getBoard().getBuildingAt(finalCoords);
            if (building == null) {
                return false;
            }

            // Give ourselves a 10-ton margin of error in case someone shoots at the building.
            double mass = path.getEntity().getWeight() + 10;

            // Add the mass of anyone else standing in/on this building.
            mass += owner.getMassOfAllInBuilding(game, finalCoords);

            return (mass > building.getCurrentCF(finalCoords));
        }

        // If we're not jumping, check each building to see if it will collapse if it has a basement.
        final double mass = path.getEntity().getWeight() + 10;
        final Enumeration<MoveStep> steps = path.getSteps();
        while (steps.hasMoreElements()) {
            final MoveStep step = steps.nextElement();
            final Building building = game.getBoard().getBuildingAt(step.getPosition());
            if (building == null) {
                continue;
            }

            // Add the mass of anyone else standing in/on this building.
            double fullMass = mass + owner.getMassOfAllInBuilding(game, step.getPosition());

            if (fullMass > building.getCurrentCF(step.getPosition())) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    Coords calcAllyCenter(int myId, List<Entity> friends, IGame game) {
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

            xTotal += friendPosition.getX();
            yTotal += friendPosition.getY();
            friendOnBoardCount++;
        }

        if (friendOnBoardCount == 0) {
            return null;
        }

        int xCenter = Math.round(xTotal / friendOnBoardCount);
        int yCenter = Math.round(yTotal / friendOnBoardCount);
        Coords center = new Coords(xCenter, yCenter);

        if (!game.getBoard().contains(center)) {
            getOwner().log(getClass(), "calcAllyCenter(int, List<Entity>, IGame)", LogLevel.ERROR,
                           "Center of ally group " + center.toFriendlyString() + " not within board boundaries.");
            return null;
        }

        return center;
    }

    protected Princess getOwner() {
        return owner;
    }
    
    /**
     * Convenience property to access bot-wide state information.
     * @return
     */
    protected PathRankerState getPathRankerState() {
        return owner.getPathRankerState();
    }
}
