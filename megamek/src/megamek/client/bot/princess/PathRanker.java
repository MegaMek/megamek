/*
 * Copyright (C) 2000-2011 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2011-2025 The MegaMek Team. All Rights Reserved.
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

import static megamek.client.ui.SharedUtility.predictLeapDamage;
import static megamek.client.ui.SharedUtility.predictLeapFallDamage;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.Optional;
import java.util.TreeSet;

import megamek.client.bot.BotLogger;
import megamek.client.bot.princess.UnitBehavior.BehaviorType;
import megamek.client.ui.Messages;
import megamek.client.ui.SharedUtility;
import megamek.common.annotations.Nullable;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.equipment.enums.BombType;
import megamek.common.game.Game;
import megamek.common.moves.MovePath;
import megamek.common.moves.MoveStep;
import megamek.common.options.OptionsConstants;
import megamek.common.rolls.TargetRoll;
import megamek.common.units.Entity;
import megamek.common.units.IBuilding;
import megamek.common.units.Targetable;
import megamek.logging.MMLogger;
import org.apache.logging.log4j.Level;

public abstract class PathRanker implements IPathRanker {
    private final static MMLogger logger = MMLogger.create(PathRanker.class);
    private static final BotLogger botLogger = new BotLogger();
    // TODO: Introduce PathRankerCacheHelper class that contains "global" path
    // ranker state
    // TODO: Introduce FireControlCacheHelper class that contains "global" Fire
    // Control state
    // PathRanker classes should be pretty stateless, except pointers to princess
    // and such

    /**
     * The possible path ranker types. If you're adding a new one, add it here then make sure to add it to
     * Princess.InitializePathRankers
     */
    public enum PathRankerType {
        Basic,
        Infantry,
        NewtonianAerospace,
        Utility
    }

    private final Princess owner;

    public PathRanker(Princess princess) {
        owner = princess;
    }

    protected abstract RankedPath rankPath(MovePath path, Game game, int maxRange,
          double fallTolerance, List<Entity> enemies,
          Coords friendsCoords);

    @Override
    public TreeSet<RankedPath> rankPaths(List<MovePath> movePaths, Game game, int maxRange,
          double fallTolerance, List<Entity> enemies,
          List<Entity> friends) {
        // No point in ranking an empty list.
        if (movePaths.isEmpty()) {
            return new TreeSet<>();
        }

        // the cached path probability data is really only relevant for one iteration
        // through this method
        getPathRankerState().getPathSuccessProbabilities().clear();

        // Let's try to whittle down this list.
        List<MovePath> validPaths = validatePaths(movePaths, game, maxRange, fallTolerance);
        logger.debug("Validated {} out of {} possible paths.", validPaths.size(), movePaths.size());

        // If the heat map of friendly activity has sufficient data, use the nearest hot
        // spot as
        // the anchor point
        Coords allyCenter = owner.getFriendlyHotSpot(movePaths.get(0).getEntity().getPosition());
        if (allyCenter == null) {
            allyCenter = this.calculateAlliesCenter(movePaths.get(0).getEntity().getId(), friends, game);
        }

        TreeSet<RankedPath> returnPaths = new TreeSet<>(Collections.reverseOrder());

        try {
            final BigDecimal numberPaths = new BigDecimal(validPaths.size());
            BigDecimal count = BigDecimal.ZERO;
            BigDecimal interval = new BigDecimal(5);
            boolean pathsHaveExpectedDamage = false;
            for (MovePath path : validPaths) {
                try {
                    count = count.add(BigDecimal.ONE);

                    RankedPath rankedPath = rankPath(path, game, maxRange, fallTolerance, enemies, allyCenter);

                    returnPaths.add(rankedPath);

                    // we want to keep track of if any of the paths we've considered have some kind
                    // of damage potential
                    pathsHaveExpectedDamage |= (rankedPath.getExpectedDamage() > 0);

                    BigDecimal percent = count.divide(numberPaths, 2, RoundingMode.DOWN).multiply(new BigDecimal(100))
                          .round(new MathContext(0, RoundingMode.DOWN));
                    if (percent.compareTo(interval) >= 0) {
                        if (logger.isLevelLessSpecificThan(Level.INFO)) {
                            getOwner().sendChat("... " + percent.intValue() + "% complete.");
                        }
                        interval = percent.add(new BigDecimal(5));
                    }
                } catch (Exception e) {
                    logger.error(e, "{} while processing {}", e.getMessage(), path);
                }
            }
            Entity mover = movePaths.get(0).getEntity();
            UnitBehavior behaviorTracker = getOwner().getUnitBehaviorTracker();
            boolean noDamageButCanDoDamage = !pathsHaveExpectedDamage
                  && (FireControl.getMaxDamageAtRange(mover, 1, false, false) > 0);

            // if we're trying to fight, but aren't going to be doing any damage no matter
            // how we move
            // then let's try to get closer
            if (noDamageButCanDoDamage
                  && (behaviorTracker.getBehaviorType(mover, getOwner()) == BehaviorType.Engaged)) {
                behaviorTracker.overrideBehaviorType(mover, BehaviorType.MoveToContact);
                return rankPaths(getOwner().getMovePathsAndSetNecessaryTargets(mover, true),
                      game, maxRange, fallTolerance, enemies, friends);
            }
        } catch (Exception exception) {
            logger.error(exception, exception.getMessage());
            return returnPaths;
        }
        botLogger.append(game, true);
        // log at most 500 paths
        int i = 0;
        int maxRankedPaths = Math.max(500, returnPaths.size());
        for (RankedPath rankedPath : returnPaths) {
            if (maxRankedPaths == i) {
                break;
            }
            botLogger.append(rankedPath, i);
            i++;
        }

        return returnPaths;
    }

    private List<MovePath> validatePaths(List<MovePath> startingPathList, Game game, int maxRange,
          double fallTolerance) {
        if (startingPathList.isEmpty()) {
            // Nothing to validate here, might as well return the empty list
            // straight away.
            return startingPathList;
        }

        Entity mover = startingPathList.get(0).getEntity();

        Targetable closestTarget = findClosestEnemy(mover, mover.getPosition(), game);
        int startingTargetDistance = (closestTarget == null) ? Integer.MAX_VALUE
              : closestTarget.getPosition().distance(mover.getPosition());
        boolean hasNoEnemyAvailable = (closestTarget == null);
        List<MovePath> returnPaths = new ArrayList<>(startingPathList.size());
        boolean inRange = maxRange >= startingTargetDistance;

        boolean isAirborneAeroOnGroundMap = mover.isAirborneAeroOnGroundMap();
        boolean needToUnjamRAC = mover.canUnjamRAC();
        int walkMP = mover.getWalkMP();

        for (MovePath path : startingPathList) {
            // just in case
            if ((path == null) || !path.isMoveLegal()) {
                continue;
            }

            logger.trace("Validating path {}", path);
            // if we are an aero unit on the ground map, we want to discard paths that keep
            // us at altitude 1 with no bombs
            if (isAirborneAeroOnGroundMap) {
                // if we have no bombs, we want to make sure our altitude is above 1
                // if we do have bombs, we may consider altitude bombing (in the future)
                if (path.getEntity().getBombs(BombType.F_GROUND_BOMB).isEmpty()
                      && (path.getFinalAltitude() < 2)) {
                    logger.trace("INVALID: No bombs but at altitude 1. No way.");
                    continue;
                }
            }

            Coords finalCoords = path.getFinalCoords();

            // Make sure I'm trying to get/stay in range of a target.
            // Skip this part if I'm an aero on the ground map, as it's kind of irrelevant
            // also skip this part if I'm attempting to retreat, as engagement is not the
            // point here
            if (!isAirborneAeroOnGroundMap && !getOwner().wantsToFallBack(mover) && !hasNoEnemyAvailable) {
                Targetable closestToEnd = findClosestEnemy(mover, finalCoords, game);
                var validation = validRange(finalCoords, closestToEnd, startingTargetDistance, maxRange,
                      inRange);
                if (!validation) {
                    logger.trace("Invalid range to target.");
                    continue;
                }
            }

            // Don't move on/through buildings that will not support our weight.
            if (willBuildingCollapse(path, game)) {
                logger.trace("INVALID: Building in path will collapse.");
                continue;
            }

            // Skip any path where I am too likely to fail my piloting roll.
            double chance = getMovePathSuccessProbability(path);
            if (chance < fallTolerance) {
                logger.trace("INVALID: Too likely to fall on my face.");
                continue;
            }

            // first crack at logic involving unjamming RACs: just do it
            if (needToUnjamRAC && ((path.getMpUsed() > walkMP) || path.isJumping())) {
                logger.trace("INADVISABLE: Want to unjam autocannon but path involves running or jumping");
                continue;
            }

            // If all the above checks have passed, this is a valid path.
            logger.trace("VALID");
            returnPaths.add(path);

        }

        // If we've eliminated all valid paths, let's try to pick out a long range path
        // instead
        if (returnPaths.isEmpty()) {
            return getOwner().getMovePathsAndSetNecessaryTargets(mover, true);
        }

        return returnPaths;
    }

    /**
     * Returns the best path of a list of ranked paths.
     *
     * @param ps The list of ranked paths to process
     *
     * @return "Best" out of those paths
     */
    @Override
    public @Nullable RankedPath getBestPath(TreeSet<RankedPath> ps) {
        return ps.isEmpty() ? null : ps.first();
    }

    /**
     * Performs initialization to help speed later calls of rankPath for this unit on this turn. Rankers that extend
     * this class should override this function
     */
    @Override
    public void initUnitTurn(Entity unit, Game game) {
    }

    @Override
    public Targetable findClosestEnemy(Entity me, Coords position, Game game) {
        return findClosestEnemy(me, position, game, true);
    }

    /**
     * Find the closest enemy to a unit with a path
     */
    @Override
    public Targetable findClosestEnemy(Entity me, Coords position, Game game,
          boolean includeStrategicTargets, int minDistance) {
        int range = Integer.MAX_VALUE;
        Targetable closest = null;
        List<Entity> enemies = getOwner().getEnemyEntities();
        var ignoredTargets = owner.getBehaviorSettings().getIgnoredUnitTargets();
        var priorityTargets = getOwner().getBehaviorSettings().getPriorityUnitTargets();
        for (Entity enemy : enemies) {
            // For now, skip anything not on the same map
            if (!game.onTheSameBoard(me, enemy)) {
                continue;
            }

            // Skip airborne aero units as they're further away than they seem and hard to catch.
            // Also, skip withdrawing enemy bot units that are not priority targets skip ignored units
            if (enemy.isAirborneAeroOnGroundMap()
                  || (!priorityTargets.contains(enemy.getId())
                  && getOwner().getHonorUtil().isEnemyBroken(enemy.getId(), enemy.getOwnerId(),
                  getOwner().getForcedWithdrawal()))
                  || ignoredTargets.contains(enemy.getId())) {
                continue;
            }

            // If a unit has not moved, assume it will move away from me.
            int unmovedDistanceModifier = 0;
            if (enemy.isSelectableThisTurn() && !enemy.isImmobile()) {
                unmovedDistanceModifier = enemy.getWalkMP();
            }

            int distance = position.distance(enemy.getPosition());
            if (((distance + unmovedDistanceModifier) < range) && ((distance + unmovedDistanceModifier)
                  >= minDistance)) {
                range = distance;
                closest = enemy;
            }
        }

        // if specified, we also consider strategic targets
        if (includeStrategicTargets) {
            for (Targetable t : getOwner().getFireControlState().getAdditionalTargets()) {
                int distance = position.distance(t.getPosition());
                if (distance < range) {
                    range = distance;
                    closest = t;
                }
            }
        }

        return closest;
    }

    /**
     * Calculates the probability that a unit can successfully complete the given movement path.
     * <p>
     * This method evaluates all piloting skill rolls required along the path and computes the combined probability of
     * passing all of them. It accounts for:
     * <ul>
     *   <li>Piloting skill rolls from difficult terrain, elevation changes, etc.</li>
     *   <li>MASC failure chances if MASC is activated during the move</li>
     *   <li>Supercharger failure chances if a supercharger is used</li>
     * </ul>
     * <p>
     * The method skips "getting up" and "careful stand" piloting rolls as these are handled
     * separately when evaluating immobile status. Results are cached to avoid redundant
     * calculations during path evaluation.
     * <p>
     * The probability is expressed as a value between 0.0 (guaranteed failure) and
     * 1.0 (guaranteed success).
     *
     * @param movePath The movement path to evaluate
     *
     * @return The probability (0.0 to 1.0) that all required piloting rolls will succeed
     */
    protected double getMovePathSuccessProbability(MovePath movePath) {
        // introduced a caching mechanism, as the success probability was being
        // calculated at least twice
        if (getPathRankerState().getPathSuccessProbabilities().containsKey(movePath.getKey())) {
            return getPathRankerState().getPathSuccessProbabilities().get(movePath.getKey());
        }

        MovePath pathCopy = movePath.clone();
        List<TargetRoll> pilotingRolls = getPSRList(pathCopy);
        double successProbability = 1.0;
        logger.trace("Calculating Move Path Success for {}", pathCopy);

        for (TargetRoll roll : pilotingRolls) {
            // Skip the getting up check. That's handled when checking for being immobile.
            if (roll.getDesc().toLowerCase().contains("getting up")) {
                continue;
            } else if (roll.getDesc().toLowerCase().contains("careful stand")) {
                continue;
            }
            boolean naturalAptPilot = movePath.getEntity().hasAbility(OptionsConstants.PILOT_APTITUDE_PILOTING);
            if (naturalAptPilot) {
                logger.trace("Pilot has Natural Aptitude Piloting");
            }

            double odds = Compute.oddsAbove(roll.getValue(), naturalAptPilot) / 100d;
            logger.trace("Odds above {} = {}", roll.getValue(), odds);
            successProbability *= odds;
        }

        // Account for MASC
        if (pathCopy.hasActiveMASC()) {
            int target = pathCopy.getEntity().getMASCTarget();
            // TODO : Does Natural Aptitude Piloting apply to this? I assume not.
            double odds = Compute.oddsAbove(target) / 100d;
            logger.trace("MASC target {}, odds = {}", target, odds);
            successProbability *= odds;
        }
        // Account for Supercharger
        if (pathCopy.hasActiveSupercharger()) {
            int target = pathCopy.getEntity().getSuperchargerTarget();
            // todo Does Natural Aptitude Piloting apply to this? I assume not.
            double odds = Compute.oddsAbove(target) / 100d;
            logger.trace("Supercharger target {}, odds = {}", target, odds);
            successProbability *= odds;
        }
        logger.trace("Success probability = {}", successProbability);
        getPathRankerState().getPathSuccessProbabilities().put(movePath.getKey(), successProbability);

        return successProbability;
    }

    /**
     * Estimates the most expected damage that a path could cause, given the pilot skill of the path ranker and various
     * conditions.
     * <p>
     * XXX Sleet01: add fall pilot damage, skid damage, and low-gravity overspeed damage calcs
     */
    protected double calculateMovePathPSRDamage(Entity movingEntity, MovePath path) {
        double damage = 0.0;

        List<TargetRoll> pilotingRolls = getPSRList(path);
        for (TargetRoll roll : pilotingRolls) {
            // Have to use if/else as switch/case won't allow runtime loading of strings without SDK 17 LTS support
            String description = roll.getLastPlainDesc().toLowerCase();
            if (
                  description.contains(Messages.getString("TacOps.leaping.leg_damage"))
            ) {
                damage += predictLeapDamage(movingEntity, roll);
            } else if (
                  description.contains(Messages.getString("TacOps.leaping.fall_damage"))
            ) {
                damage += predictLeapFallDamage(movingEntity, roll);
            }
        }

        return damage;
    }

    protected List<TargetRoll> getPSRList(MovePath path) {
        return SharedUtility.getPSRList(path);
    }

    @Override
    public int distanceToHomeEdge(Coords position, int boardId, CardinalEdge homeEdge, Game game) {
        int width = game.getBoard(boardId).getWidth();
        int height = game.getBoard(boardId).getHeight();

        int distance;
        switch (homeEdge) {
            case NORTH: {
                distance = position.getY();
                break;
            }
            case SOUTH: {
                distance = height - position.getY() - 1;
                break;
            }
            case WEST: {
                distance = position.getX();
                break;
            }
            case EAST: {
                distance = width - position.getX() - 1;
                break;
            }
            default: {
                logger.warn("Invalid home edge. Defaulting to NORTH.");
                distance = position.getY();
            }
        }

        return distance;
    }

    private boolean validRange(Coords finalCoords, Targetable target, int startingTargetDistance,
          int maxRange, boolean inRange) {
        if (target == null) {
            return false;
        }

        // If I am not currently in range, discard any path that takes me further away
        // from my target.
        int finalDistanceToTarget = finalCoords.distance(target.getPosition());
        if (!inRange) {
            if (finalDistanceToTarget > startingTargetDistance) {
                logger.trace("INVALID: Not in range and moving further away.");
                return false;
            }
        } else { // If I am in range, discard any path that takes me out of range.
            if (finalDistanceToTarget > maxRange) {
                logger.trace("INVALID: In range and moving out of range.");
                return false;
            }
        }

        return true;
    }

    /**
     * Check the path being moved to see if there is a danger of building collapse. Allows a margin of error of 10 tons
     * in case someone decides to shoot at the building. If jumping, only the landing point is checked. For all other
     * move types, the entire path is checked.
     * TODO : reread the rules on basement collapse
     * TODO : skip basement check if random basement option is turned off
     * TODO : incorporate test for building damage just from moving through building
     *
     * @param path The {@link MovePath} being traversed.
     * @param game The current {@link Game}
     *
     * @return True if there is a building in our path that might collapse.
     */
    private boolean willBuildingCollapse(MovePath path, Game game) {
        // airborne aircraft cannot collapse buildings
        if (path.getEntity().isAero() || path.getEntity().hasETypeFlag(Entity.ETYPE_VTOL)) {
            return false;
        }

        // If we're jumping onto a building, make sure it can support our weight.
        if (path.isJumping()) {
            final Coords finalCoords = path.getFinalCoords();
            Optional<IBuilding> building = game.getBuildingAt(finalCoords, path.getFinalBoardId());
            if (building.isEmpty()) {
                return false;
            }

            // Give ourselves a 10-ton margin of error in case someone shoots at the
            // building.
            double mass = path.getEntity().getWeight() + 10;

            // Add the mass of anyone else standing in/on this building.
            mass += owner.getMassOfAllInBuilding(game, finalCoords, path.getFinalBoardId());

            return (mass > building.get().getCurrentCF(finalCoords));
        }

        // If we're not jumping, check each building to see if it will collapse if it
        // has a basement.
        final double mass = path.getEntity().getWeight() + 10;
        final ListIterator<MoveStep> steps = path.getSteps();
        while (steps.hasNext()) {
            final MoveStep step = steps.next();
            final IBuilding building = game.getBoard(step.getBoardId()).getBuildingAt(step.getPosition());
            if (building == null) {
                continue;
            }

            // Add the mass of anyone else standing in/on this building.
            double fullMass = mass + owner.getMassOfAllInBuilding(game, step.getPosition(), step.getBoardId());

            if (fullMass > building.getCurrentCF(step.getPosition())) {
                return true;
            }
        }
        return false;
    }

    public @Nullable Coords calculateAlliesCenter(int myId, @Nullable List<Entity> friends, Game game) {
        return calcAllyCenter(myId, friends, game);
    }

    public static @Nullable Coords calcAllyCenter(int myId, @Nullable List<Entity> friends, Game game) {
        if ((friends == null) || friends.size() <= 1) {
            return null;
        }

        int xTotal = 0;
        int yTotal = 0;
        int friendOnBoardCount = 0;
        Entity me = game.getEntity(myId);
        if (me == null) {
            return null;
        }
        Board board = game.getBoard(me);

        for (Entity friend : friends) {
            if (friend.getId() == myId) {
                continue;
            }

            // Skip any friends not on the board.
            if (friend.isOffBoard() || !game.onTheSameBoard(me, friend)) {
                continue;
            }
            Coords friendPosition = friend.getPosition();
            if ((friendPosition == null) || !board.contains(friendPosition)) {
                continue;
            }

            xTotal += friendPosition.getX();
            yTotal += friendPosition.getY();
            friendOnBoardCount++;
        }

        if (friendOnBoardCount == 0) {
            return null;
        }

        int xCenter = Math.round((float) xTotal / friendOnBoardCount);
        int yCenter = Math.round((float) yTotal / friendOnBoardCount);
        Coords center = new Coords(xCenter, yCenter);

        if (!board.contains(center)) {
            logger.error("Center of ally group {} not within board boundaries.", center.toFriendlyString());
            return null;
        }

        return center;
    }

    protected Princess getOwner() {
        return owner;
    }

    /**
     * Convenience property to access bot-wide state information.
     *
     * @return the owner's path ranker state
     */
    protected PathRankerState getPathRankerState() {
        return owner.getPathRankerState();
    }
}
