/*
 * Copyright (C) 2018-2025 The MegaMek Team. All Rights Reserved.
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

import java.util.Iterator;
import java.util.List;

import megamek.client.bot.princess.coverage.Builder;
import megamek.client.bot.princess.geometry.ConvexBoardArea;
import megamek.common.LosEffects;
import megamek.common.OffBoardDirection;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.compute.ComputeArc;
import megamek.common.game.Game;
import megamek.common.moves.MovePath;
import megamek.common.options.OptionsConstants;
import megamek.common.units.Entity;
import megamek.common.units.IAero;

public class NewtonianAerospacePathRanker extends BasicPathRanker {
    public NewtonianAerospacePathRanker(Princess owningPrincess) {
        super(owningPrincess);
    }

    /**
     * Find the closest enemy to a unit with a path that ends at the given position.
     */
    @Override
    public Entity findClosestEnemy(Entity me, Coords position, Game game) {
        int range = Integer.MAX_VALUE;
        Entity closest = null;
        List<Entity> enemies = getOwner().getEnemyEntities();
        var ignoredTargets = getOwner().getBehaviorSettings().getIgnoredUnitTargets();
        var priorityTargets = getOwner().getBehaviorSettings().getPriorityUnitTargets();
        for (Entity enemy : enemies) {
            // targets that are withdrawing and are not priorities are ignored
            // targets in the ignore list are ignored
            // therefore... a priority target in the ignore list is ignored
            if ((!priorityTargets.contains(enemy.getId()) &&
                  getOwner().getHonorUtil()
                        .isEnemyBroken(enemy.getId(), enemy.getOwnerId(), getOwner().getForcedWithdrawal())) ||
                  ignoredTargets.contains(enemy.getId())) {
                continue;
            }

            // If a unit has not moved, assume it will move away from me.
            int unmovedDistMod = 0;
            if (enemy.isSelectableThisTurn() && !enemy.isImmobile()) {
                unmovedDistMod = enemy.getWalkMP();
            }

            if ((position.distance(enemy.getPosition()) + unmovedDistMod) < range) {
                range = position.distance(enemy.getPosition());
                closest = enemy;
            }
        }
        return closest;
    }

    /**
     * Calculate the damage potential
     */
    @Override
    double calculateMyDamagePotential(MovePath path, Entity enemy, int distance, Game game) {
        Entity me = path.getEntity();

        int maxRange = getOwner().getMaxWeaponRange(me, enemy.isAirborne());
        if (distance > maxRange) {
            return 0;
        }

        // If I don't have LoS, I can't do damage. We're on a space map, so this probably is unnecessary.
        LosEffects losEffects = LosEffects.calculateLOS(game,
              me,
              enemy,
              path.getFinalCoords(),
              enemy.getPosition(),
              false);
        if (!losEffects.canSee()) {
            return 0;
        }

        FiringPlan myFiringPlan;

        FiringPlanCalculationParameters guess = new Builder().buildGuess(path.getEntity(),
              new EntityState(path),
              enemy,
              null,
              Entity.DOES_NOT_TRACK_HEAT,
              null);
        myFiringPlan = getFireControl(path.getEntity()).determineBestFiringPlan(guess);

        return myFiringPlan.getUtility();
    }

    /**
     * Guesses a number of things about an enemy that has not yet moved
     */
    @Override
    EntityEvaluationResponse evaluateUnmovedEnemy(Entity enemy, MovePath path, boolean useExtremeRange,
          boolean useLOSRange) {
        EntityEvaluationResponse returnResponse = new EntityEvaluationResponse();

        Coords finalCoords = path.getFinalCoords();
        Coords closest = getClosestCoordsTo(enemy.getId(), finalCoords);
        if (closest == null) {
            return returnResponse;
        }
        int range = closest.distance(finalCoords);
        if (range == 0) {
            range = 1;
        }


        // placeholder logic:
        // if we are a spheroid, we can fire viably in any direction
        // if we are a fighter or aerodyne DropShip, our most effective arc is forward
        // larger craft are usually bristling with weapons all around
        int arcToUse = ((IAero) path.getEntity()).isSpheroid() ? Compute.ARC_360 : Compute.ARC_NOSE;
        double vertexCoverage = 1.0;

        // The idea here is that, if we have a limited firing arc, the target
        // will likely make an effort to move out of the arc. It reduces our expected damage
        // we calculate the proportion by looking at the number of "enemy movable area" vertices
        // that are in our main firing arc, compared to the max (6).
        if (arcToUse != Compute.ARC_360) {
            int inArcVertexCount = 0;
            ConvexBoardArea movableArea = getPathEnumerator().getUnitMovableAreas().get(enemy.getId());

            for (int vertexNum = 0; vertexNum < 6; vertexNum++) {
                Coords vertex = movableArea.getVertexNum(vertexNum);

                if (vertex != null && ComputeArc.isInArc(finalCoords, path.getFinalFacing(), vertex, arcToUse)) {
                    inArcVertexCount++;
                }
            }

            vertexCoverage = (double) inArcVertexCount / 6;
        }

        double myDamageDiscount = Compute.oddsAbove(path.getEntity().getCrew().getGunnery()) / 100 * vertexCoverage;

        // my estimated damage is my max damage at the
        returnResponse.addToMyEstimatedDamage(getMaxDamageAtRange(path.getEntity(),
              range,
              useExtremeRange,
              useLOSRange) * myDamageDiscount);

        int sensorShadowMod = calculateSensorShadowMod(path);
        double enemyDamageDiscount = Compute.oddsAbove(enemy.getCrew().getGunnery() + sensorShadowMod) / 100;
        //in general, if an enemy can end its position in range, it can hit me
        returnResponse.addToEstimatedEnemyDamage(getMaxDamageAtRange(enemy, range, useExtremeRange, useLOSRange) *
              enemyDamageDiscount);

        return returnResponse;
    }

    /**
     * Estimates the sensor shadow modifier for a given path. Only checks adjacent hexes and doesn't attempt to count
     * intervening craft also only counts friendly entities that have already moved
     *
     * @param path The path to check
     *
     * @return 0 if there's no
     */
    int calculateSensorShadowMod(MovePath path) {
        if (!path.getGame().getOptions().booleanOption(OptionsConstants.ADVANCED_AERO_RULES_STRATOPS_SENSOR_SHADOW)) {
            return 0;
        }

        int sensorShadowMod = 0;
        List<Coords> coordsToCheck = path.getFinalCoords().allAdjacent();
        coordsToCheck.add(path.getFinalCoords());
        for (Coords coords : coordsToCheck) {
            // if the coordinate contains a large craft within a certain mass of me, it will generate a sensor shadow
            Iterator<Entity> potentialShadowIter = path.getGame().getFriendlyEntities(coords, path.getEntity());

            while (potentialShadowIter.hasNext() && sensorShadowMod == 0) {
                Entity potentialShadow = potentialShadowIter.next();
                if (potentialShadow.isDone() &&
                      potentialShadow.isLargeCraft() &&
                      (potentialShadow.getWeight() - path.getEntity().getWeight() >=
                            -WeaponAttackAction.STRATOPS_SENSOR_SHADOW_WEIGHT_DIFF)) {
                    sensorShadowMod = 1;
                }
            }

            if (sensorShadowMod == 1) {
                break;
            }
        }

        return sensorShadowMod;
    }

    /**
     * Tells me whether this path will result in me flying to a location from which there is absolutely no way to remain
     * on the board the following turn.
     * <p>
     * We also take into account the possibility that we are intentionally trying to
     * <ul>
     *     <li>retreat</li>
     *     <li>fly off a particular</li>
     * </ul>
     * edge
     *
     * @param path The path to examine
     *
     * @return 0 if we are A-Ok with it, .5 (maybe tune this) if we aren't looking to go off board
     */
    @Override
    protected double calculateOffBoardMod(MovePath path) {
        // step one: project the given path's vector over the next turn.
        OffBoardDirection offBoardDirection = calculateOffBoardDirection(path.getEntity(),
              path.getFinalCoords(),
              path.getFinalVectors());

        // if we want to flee the board from the edge in question, we're ok
        if (getOwner().isFallingBack(path.getEntity()) &&
              (getOwner().getHomeEdge(path.getEntity()) ==
                    CardinalEdge.getCardinalEdge(offBoardDirection.getValue()))) {
            return 0.0;
        }

        if (offBoardDirection == OffBoardDirection.NONE) {
            return 0.0;
        }

        return .5;
    }

    /**
     * Worker function that determines the direction in which the given entity will go off board if it starts at the
     * given coordinates with the given vectors.
     *
     * @param entity         Entity to examine
     * @param startingCoords Starting coordinates
     * @param vectors        Starting velocity vector
     *
     * @return Flight direction. OffBoardDirection.NONE if the given entity will be able to remain on board.
     */
    private static OffBoardDirection calculateOffBoardDirection(Entity entity, Coords startingCoords, int[] vectors) {
        Coords nextCoords = Compute.getFinalPosition(startingCoords, vectors);
        int availableThrust = entity.getRunMP();
        Board board = entity.getGame().getBoard();
        OffBoardDirection offBoardDirection = OffBoardDirection.NONE;

        // step one: check if the position is out of bounds by more than the unit has available thrust
        if (nextCoords.getX() < -availableThrust) {
            offBoardDirection = OffBoardDirection.WEST;
        } else if (nextCoords.getX() > board.getWidth() + availableThrust) {
            offBoardDirection = OffBoardDirection.EAST;
        } else if (nextCoords.getY() < -availableThrust) {
            offBoardDirection = OffBoardDirection.NORTH;
        } else if (nextCoords.getY() > board.getHeight() + availableThrust) {
            offBoardDirection = OffBoardDirection.SOUTH;
        }

        return offBoardDirection;
    }

    /**
     * Whether an entity will go off board if it starts at the given coordinates with the given vectors.
     *
     * @param entity Entity to examine
     * @param coords Starting coordinates
     *
     * @return Whether the entity will go off board or not.
     */
    public static boolean willFlyOffBoard(Entity entity, Coords coords) {
        OffBoardDirection offBoardDirection = calculateOffBoardDirection(entity, coords, entity.getVectors());

        return offBoardDirection != OffBoardDirection.NONE;
    }

    /**
     * Worker function that determines if a given enemy entity should be evaluated as if it has moved.
     */
    @Override
    protected boolean evaluateAsMoved(Entity enemy) {
        return !enemy.isSelectableThisTurn() || enemy.isImmobile();
    }
}
