/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 *
 */

package megamek.client.bot.caspar.ai.utility.tw.intelligence;

import megamek.client.bot.princess.*;
import megamek.common.*;
import megamek.common.options.OptionsConstants;

import java.util.List;
import java.util.Map;

public class SimplePathRankerUtilCalculator implements PathRankerUtilCalculator {

    private final BasicPathRanker pathRanker;
    private final Game game;
    private final Princess owner;

    public SimplePathRankerUtilCalculator(Princess princess, Game game, BasicPathRanker pathRanker) {
        this.pathRanker = pathRanker;
        this.owner = princess;
        this.game = game;
    }

    @Override
    public FiringPhysicalDamage damageCalculator(MovePath path, List<Entity> enemies) {
        Entity movingUnit = path.getEntity();
        MovePath pathCopy = path.clone();

        double expectedDamageTaken = calculateMovePathPSRDamage(movingUnit, pathCopy);
        expectedDamageTaken += checkPathForHazards(pathCopy, movingUnit, game);
        expectedDamageTaken += MinefieldUtil.checkPathForMinefieldHazards(pathCopy);

        FiringPhysicalDamage damageEstimate = new FiringPhysicalDamage();

        boolean extremeRange = game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_RANGE);
        boolean losRange = game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_LOS_RANGE);
        for (Entity enemy : enemies) {
            if (shouldSkipEnemy(enemy)) {
                continue;
            }

            EntityEvaluationResponse eval = evaluateEnemy(enemy, path, pathCopy, extremeRange, losRange);

            if (!owner.getBehaviorSettings().getIgnoredUnitTargets().contains(enemy.getId())) {
                updateDamageEstimate(damageEstimate, eval);
            }

            expectedDamageTaken += eval.getEstimatedEnemyDamage();
        }

        expectedDamageTaken += calculateFriendlyArtilleryDamage(path);

        damageEstimate = calcDamageToStrategicTargets(pathCopy, game, owner.getFireControlState(), damageEstimate);

        if (shouldDisablePhysicalDamage(path)) {
            damageEstimate.physicalDamage = 0;
        }

        return new FiringPhysicalDamage(damageEstimate.firingDamage, damageEstimate.physicalDamage, expectedDamageTaken);
    }

    private boolean shouldSkipEnemy(Entity enemy) {
        return enemy instanceof MekWarrior || enemy.isOffBoard() || enemy.getPosition() == null || !game.getBoard().contains(enemy.getPosition())
            || owner.getHonorUtil().isEnemyBroken(enemy.getId(), enemy.getOwnerId(), owner.getForcedWithdrawal());
    }

    private EntityEvaluationResponse evaluateEnemy(Entity enemy, MovePath path, MovePath pathCopy, boolean extremeRange, boolean losRange) {
        if (evaluateAsMoved(enemy)) {
            return evaluateMovedEnemy(enemy, pathCopy, game);
        } else {
            return evaluateUnmovedEnemy(enemy, path, extremeRange, losRange);
        }
    }

    private void updateDamageEstimate(FiringPhysicalDamage damageEstimate, EntityEvaluationResponse eval) {
        if (damageEstimate.firingDamage < eval.getMyEstimatedDamage()) {
            damageEstimate.firingDamage = eval.getMyEstimatedDamage();
        }
        if (damageEstimate.physicalDamage < eval.getMyEstimatedPhysicalDamage()) {
            damageEstimate.physicalDamage = eval.getMyEstimatedPhysicalDamage();
        }
    }

    private double calculateFriendlyArtilleryDamage(MovePath path) {
        double friendlyArtilleryDamage = 0;
        if (!path.getEntity().isAirborne() && !path.getEntity().isAirborneVTOLorWIGE()) {
            Map<Coords, Double> artyDamage = owner.getPathRankerState().getIncomingFriendlyArtilleryDamage();
            if (!artyDamage.containsKey(path.getFinalCoords())) {
                friendlyArtilleryDamage = ArtilleryTargetingControl.evaluateIncomingArtilleryDamage(path.getFinalCoords(), owner);
                artyDamage.put(path.getFinalCoords(), friendlyArtilleryDamage);
            } else {
                friendlyArtilleryDamage = artyDamage.get(path.getFinalCoords());
            }
        }
        return friendlyArtilleryDamage;
    }

    private boolean shouldDisablePhysicalDamage(MovePath path) {
        return game.getOptions().booleanOption(OptionsConstants.ALLOWED_NO_CLAN_PHYSICAL) && path.getEntity().getCrew().isClanPilot();
    }

    @Override
    public double getMovePathSuccessProbability(MovePath path) {
        return pathRanker.getMovePathSuccessProbability(path);
    }

    @Override
    public int distanceToHomeEdge(Coords position, CardinalEdge homeEdge, Game game) {
        return pathRanker.distanceToHomeEdge(position, homeEdge, game);
    }

    @Override
    public double calculateMovePathPSRDamage(Entity movingUnit, MovePath pathCopy) {
        return pathRanker.calculateMovePathPSRDamage(movingUnit, pathCopy);
    }

    @Override
    public double checkPathForHazards(MovePath path, Entity movingUnit, Game game) {
        return pathRanker.checkPathForHazards(path, movingUnit, game);
    }

    @Override
    public FiringPhysicalDamage calcDamageToStrategicTargets(MovePath path, Game game, FireControlState fireControlState, FiringPhysicalDamage damageStructure) {
        return pathRanker.calcDamageToStrategicTargets(path, game, fireControlState, damageStructure);
    }

    @Override
    public boolean evaluateAsMoved(Entity enemy) {
        return pathRanker.evaluateAsMoved(enemy);
    }

    @Override
    public EntityEvaluationResponse evaluateMovedEnemy(Entity enemy, MovePath path, Game game) {
        return pathRanker.evaluateMovedEnemy(enemy, path, game);
    }

    @Override
    public EntityEvaluationResponse evaluateUnmovedEnemy(Entity enemy, MovePath path, boolean extremeRange, boolean losRange) {
        return pathRanker.evaluateUnmovedEnemy(enemy, path, extremeRange, losRange);
    }
}
