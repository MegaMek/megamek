/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
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
package megamek.client.bot.duchess.ai.utility.tw.decision;

import megamek.ai.utility.DecisionContext;
import megamek.client.bot.duchess.Duchess;
import megamek.client.bot.duchess.ai.utility.tw.context.TWWorld;
import megamek.client.bot.duchess.ai.utility.tw.intelligence.PathRankerUtilCalculator;
import megamek.client.bot.duchess.ai.utility.tw.intelligence.SimpleIntelligence;
import megamek.client.bot.princess.*;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.MovePath;
import megamek.common.options.OptionsConstants;

import java.util.List;

import static megamek.client.bot.princess.FireControl.getMaxDamageAtRange;


public class TWDecisionContext extends DecisionContext<Entity, Entity> {

    private final MovePath movePath;
    private final Duchess duchess;
    private final PathRankerUtilCalculator pathRankerUtilCalculator;

    public TWDecisionContext(Duchess duchess, TWWorld world, Entity currentUnit, List<Entity> targetUnits, MovePath movePath, PathRankerUtilCalculator pathRankerUtilCalculator) {
        super(duchess, world, currentUnit, targetUnits);
        this.duchess = duchess;
        this.movePath = movePath.clone();
        this.pathRankerUtilCalculator = pathRankerUtilCalculator;
    }

    public Duchess getDuchess() {
        return duchess;
    }

    public UnitBehavior.BehaviorType getUnitBehavior(Entity entity) {
        return duchess.getUnitBehaviorTracker().getBehaviorType(entity, duchess);
    }

    public int distanceToRetreatEdge(Entity entity) {
        return pathRankerUtilCalculator.distanceToHomeEdge(entity.getPosition(), duchess.getBehaviorSettings().getRetreatEdge(), ((TWWorld)getWorld()).getGame());
    }

    public int distanceToDestinationEdge(Entity entity) {
        return pathRankerUtilCalculator.distanceToHomeEdge(entity.getPosition(), duchess.getBehaviorSettings().getDestinationEdge(), ((TWWorld)getWorld()).getGame());
    }

    @Override
    public double calculateUnitMaxDamageAtRange(Entity unit, int enemyRange) {
        return getMaxDamageAtRange(unit, enemyRange,
            getWorld().useBooleanOption(OptionsConstants.ADVCOMBAT_TACOPS_LOS_RANGE),
            getWorld().useBooleanOption(OptionsConstants.ADVCOMBAT_TACOPS_RANGE));
    }
    private SimpleIntelligence.FiringPhysicalDamage cachedDamage;

    public double getExpectedDamage() {
        if (cachedDamage == null) {
            cachedDamage = pathRankerUtilCalculator.damageCalculator(movePath, getWorld().getEnemyUnits());
        }
        return cachedDamage.takenDamage();
    }

    public double getTotalDamage() {
        if (cachedDamage == null) {
            cachedDamage = pathRankerUtilCalculator.damageCalculator(movePath, getWorld().getEnemyUnits());
        }
        return cachedDamage.firingDamage() + cachedDamage.physicalDamage();
    }

    public double getFiringDamage() {
        if (cachedDamage == null) {
            cachedDamage = pathRankerUtilCalculator.damageCalculator(movePath, getWorld().getEnemyUnits());
        }
        return cachedDamage.firingDamage();
    }

    public double getPhysicalDamage() {
        if (cachedDamage == null) {
            cachedDamage = pathRankerUtilCalculator.damageCalculator(movePath, getWorld().getEnemyUnits());
        }
        return cachedDamage.physicalDamage();
    }

    public double getMovePathSuccessProbability() {
        return pathRankerUtilCalculator.getMovePathSuccessProbability(movePath, new StringBuilder());
    }

    public List<Entity> getClosestEnemyCluster(Entity entity) {
        var cluster = ((TWWorld) getWorld()).getEntityCluster(entity);
        var enemyCluster = ((TWWorld) getWorld()).getClosestEnemyCluster(cluster);
        return enemyCluster.getMembers();
    }

    public Entity getClosestEnemy(Coords coords) {
        var distance = Integer.MAX_VALUE;
        Entity currentEnemy = null;
        for (var enemy : getWorld().getEnemyUnits()) {
            var dist = enemy.getPosition().distance(coords);
            if (dist < distance) {
                distance = dist;
                currentEnemy = enemy;
            }
        }

        return currentEnemy;
    }

    public Coords getFriendsClusterCentroid(Entity entity) {
        return ((TWWorld) getWorld()).getEntityClusterCentroid(entity);
    }



    @Override
    public double getBonusFactor(DecisionContext<Entity, Entity> lastContext) {
        return 0;
    }

    public MovePath getMovePath() {
        return movePath;
    }
}
