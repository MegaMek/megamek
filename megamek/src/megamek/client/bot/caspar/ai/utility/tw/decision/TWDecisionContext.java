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
package megamek.client.bot.caspar.ai.utility.tw.decision;

import megamek.ai.utility.DecisionContext;
import megamek.ai.utility.Intelligence;
import megamek.ai.utility.Memory;
import megamek.client.bot.princess.*;
import megamek.client.bot.caspar.ai.utility.tw.context.StructOfArraysEntity;
import megamek.client.bot.caspar.ai.utility.tw.context.TWWorld;
import megamek.client.bot.caspar.ai.utility.tw.intelligence.PathRankerUtilCalculator;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.MovePath;
import megamek.common.UnitRole;
import megamek.common.annotations.Nullable;
import megamek.common.options.OptionsConstants;

import java.util.*;

import static megamek.client.bot.princess.FireControl.getMaxDamageAtRange;


public class TWDecisionContext extends DecisionContext<Entity, Entity> {
    private final Memory memory;
    private final MovePath movePath;
    private final PathRankerUtilCalculator pathRankerUtilCalculator;
    private final Intelligence<Entity, Entity, RankedPath> intelligence;
    private final BehaviorSettings behaviorSettings;
    private final UnitBehavior.BehaviorType unitBehavior;
    private final Coords waypoint;
    private final FireControlState fireControlState;
    private FiringPhysicalDamage cachedDamage = null;

    public TWDecisionContext(
        Intelligence<Entity, Entity, RankedPath> intelligence,
        TWWorld world,
        Memory memory,
        Entity currentUnit,
        List<Entity> targetUnits,
        MovePath movePath,
        PathRankerUtilCalculator pathRankerUtilCalculator,
        UnitBehavior.BehaviorType unitBehavior,
        BehaviorSettings behaviorSettings,
        FireControlState fireControlState,
        @Nullable Coords waypoint,
        Map<String, Double> damageCache
    )
    {
        super(world, currentUnit, targetUnits, damageCache);
        this.intelligence = intelligence;
        this.movePath = movePath.clone();
        this.pathRankerUtilCalculator = pathRankerUtilCalculator;
        this.unitBehavior = unitBehavior;
        this.behaviorSettings = behaviorSettings;
        this.waypoint = waypoint;
        this.fireControlState = fireControlState;
        this.memory = memory;
    }

    public UnitBehavior.BehaviorType getUnitBehavior() {
        return unitBehavior;
    }

    public Memory getMemory() {
        return memory;
    }

    public BehaviorSettings getBehaviorSettings() {
        return behaviorSettings;
    }

    public Optional<RankedPath> getPreviouslyRanked() {
        return intelligence.getPastRankedPath(getCurrentUnit());
    }

    public int distanceToRetreatEdge(Entity entity) {
        return distanceToRetreatEdge(entity.getPosition());
    }
    public int distanceToRetreatEdge(Coords coords) {
        return pathRankerUtilCalculator.distanceToHomeEdge(coords, behaviorSettings.getRetreatEdge(), ((TWWorld)getWorld()).getGame());
    }

    public int distanceToDestinationEdge(Entity entity) {
        return distanceToDestinationEdge(entity.getPosition());
    }

    public int distanceToDestinationEdge(Coords coords) {
        return pathRankerUtilCalculator.distanceToHomeEdge(coords, behaviorSettings.getDestinationEdge(), ((TWWorld)getWorld()).getGame());
    }

    public int getCurrentUnitMaxRunMP() {
        return getCurrentUnit().getRunMP();
    }

    public double maxAmountOfDamageFromFriendsInRange(Coords coords, Coords targetCoords, int range) {
        var targetX = targetCoords.getX();
        var targetY = targetCoords.getY();
        var ids = getFriendIDsAtRange(coords, range);
        var entities = getWorld().getEntities(ids);
        var totalDamage = 0d;
        for (var entity : entities) {
            var rangeTowardsTarget = distance(entity.getPosition().getX(), entity.getPosition().getY(), targetX, targetY);
            totalDamage += getUnitMaxDamageAtRange(entity, rangeTowardsTarget);
        }
        return totalDamage;
    }

    @Override
    public double calculateUnitMaxDamageAtRange(Entity unit, int enemyRange) {
        return getMaxDamageAtRange(unit, enemyRange,
            getWorld().useBooleanOption(OptionsConstants.ADVCOMBAT_TACOPS_LOS_RANGE),
            getWorld().useBooleanOption(OptionsConstants.ADVCOMBAT_TACOPS_RANGE));
    }

    @Override
    public double getBonusFactor() {
        return intelligence.getBonusFactor(getCurrentUnit(), getMovePath());
    }

    public double getExpectedDamage() {
        return cacheDamage().takenDamage;
    }

    public double getTotalDamage() {
        return cacheDamage().getMaximumDamageEstimate();
    }

    public double getFiringDamage() {
        return cacheDamage().firingDamage;
    }

    public double getPhysicalDamage() {
        return cacheDamage().physicalDamage;
    }

    private FiringPhysicalDamage cacheDamage() {
        if (cachedDamage == null) {
            cachedDamage = pathRankerUtilCalculator.damageCalculator(movePath, getWorld().getEnemyUnits());
        }
        return cachedDamage;
    }

    public double getMovePathSuccessProbability() {
        return pathRankerUtilCalculator.getMovePathSuccessProbability(movePath);
    }

    public int getNumberOfEnemiesAtRange(Coords coords, int distance) {
        return getEnemyIDsAtRange(coords, distance).size();
    }

    public OptionalInt getDistanceDeltaFromTargetUnit(Entity entity) {
        if ((entity == null) || (entity.getPosition() == null) || (movePath.getFinalCoords() == null)) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(movePath.getFinalCoords().distance(entity.getPosition()) - movePath.getStartCoords().distance(entity.getPosition()));
    }

    public OptionalInt getDistanceDeltaFromTargetCoords(Coords coords) {
        if (movePath.getFinalCoords() == null) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(movePath.getFinalCoords().distance(coords) - movePath.getStartCoords().distance(coords));
    }

    public Optional<Entity> getClosestEnemy() {
        return getClosestEnemy(getCurrentUnit().getPosition());
    }

    public Optional<Entity> getClosestEnemyFromMovePathFinalPosition() {
        return getClosestEnemy(movePath.getFinalCoords());
    }

    public Optional<Entity> getClosestVIP() {
        return getClosestVIP(getCurrentUnit().getPosition());
    }

    public boolean isVIP(Entity entity) {
        return getVIPType(entity) != VIPType.NONE;
    }

    public VIPType getVIPType(Entity entity) {
        if (fireControlState.isCommander(entity)) {
            return VIPType.COMMANDER;
        } else if (fireControlState.isSubCommander(entity)) {
            return VIPType.SUB_COMMANDER;
        } else {
            return VIPType.NONE;
        }
    }

    public int getDistanceToDestination() {

        if (unitBehavior == UnitBehavior.BehaviorType.MoveToDestination) {
            return Optional.ofNullable(waypoint).map(coords -> getCurrentUnit().getPosition().distance(coords))
                .orElseGet(() -> distanceToDestinationEdge(getCurrentUnit()));
        } else if (unitBehavior == UnitBehavior.BehaviorType.ForcedWithdrawal) {
            return distanceToRetreatEdge(getCurrentUnit());
        }
        return -1;
    }

    /**
     * Returns the distance delta to the destination.
     * It will return {@code OptionalInt.empty()} if the unit has no destination at all, it will return positive if this movement path
     * gets it closer to the destination, and a negative number if it gets it away from the destination
     */
    public OptionalInt getDistanceDeltaToDestination() {
        var movePath = getMovePath();
        var startingPosition = movePath.getStartCoords();
        var finalPosition = movePath.getFinalCoords();
        if (finalPosition == null) {
            return OptionalInt.empty();
        }

        if (unitBehavior == UnitBehavior.BehaviorType.MoveToDestination) {
            if (waypoint != null) {
                return OptionalInt.of(startingPosition.distance(waypoint) - finalPosition.distance(waypoint));
            } else {
                var distToEdge = distanceToDestinationEdge(startingPosition);
                var finalDistToEdge = distanceToDestinationEdge(finalPosition);
                return OptionalInt.of(distToEdge - finalDistToEdge);
            }
        } else if (unitBehavior == UnitBehavior.BehaviorType.ForcedWithdrawal) {
            var distToEdge = distanceToRetreatEdge(startingPosition);
            var finalDistToEdge = distanceToRetreatEdge(finalPosition);
            return OptionalInt.of(distToEdge - finalDistToEdge);
        }

        return OptionalInt.empty();
    }

    public enum VIPType {
        COMMANDER,
        SUB_COMMANDER,
        NONE
    }

    public Optional<Entity> getClosestVIP(Coords coords) {
        var distance = Integer.MAX_VALUE;

        Entity currentEnemy = null;
        for (var enemy : getWorld().getEnemyUnits()) {
            if (!isVIP(enemy)) {
                continue;
            }
            var dist = enemy.getPosition().distance(coords);
            if (dist < distance) {
                distance = dist;
                currentEnemy = enemy;
            }
        }

        return Optional.ofNullable(currentEnemy);
    }

    public Optional<Entity> getClosestEnemy(Coords coords) {
        var distance = Integer.MAX_VALUE;
        Entity currentEnemy = null;
        for (var enemy : getWorld().getEnemyUnits()) {
            var dist = enemy.getPosition().distance(coords);
            if (dist < distance) {
                distance = dist;
                currentEnemy = enemy;
            }
        }

        return Optional.ofNullable(currentEnemy);
    }

    public Coords getFriendsClusterCentroid(Entity entity) {
        return ((TWWorld) getWorld()).getEntityClusterCentroid(entity);
    }

    public MovePath getMovePath() {
        return movePath;
    }

    public int getNumberOfFriendsInRange(Coords coords, int range) {
        var x = coords.getX();
        var y = coords.getY();

        var allXY = new int[Math.max(getWorld().getAllies().length, getWorld().getAllies().length)][2];
        getWorld().getAllies().getAllXY(allXY);
        int units = getUnitCount(range, x, y, allXY, getWorld().getAllies().length);
        getWorld().getOwnUnits().getAllXY(allXY);
        units += getUnitCount(range, x, y, allXY, getWorld().getOwnUnits().length);
        return units;
    }

    private int getUnitCount(int range, int x, int y, int[][] allXY, int length) {
        double dist;
        int units = 0;
        for (int i = 0; i < length; i++) {
            dist = distance(x, y, allXY[i][0], allXY[i][1]);
            if (dist <= range) {
                units++;
            }
        }
        return units;
    }

    public int getNumberOfEnemiesInRange(Coords coords, int range) {
        return getUnitCount(range, coords.getX(), coords.getY(), getWorld().getEnemies().getAllXY(), getWorld().getEnemies().length);
    }

    public List<Integer> getFriendIDsAtRange(Coords coords, int range) {
        var x = coords.getX();
        var y = coords.getY();
        var ids = getUnitIdsAtRange(getWorld().getAllies(), x, y, range);
        ids.addAll(getUnitIdsAtRange(getWorld().getOwnUnits(), x, y, range));
        return ids;
    }

    public List<Integer> getEnemyIDsAtRange(Coords coords, int range) {
        var x = coords.getX();
        var y = coords.getY();
        return getUnitIdsAtRange(getWorld().getEnemies(), x, y, range);
    }

    @Override
    public TWWorld getWorld() {
        return (TWWorld) super.getWorld();
    }

    private List<Integer> getUnitIdsAtRange(StructOfArraysEntity structOfArraysEntity, int x, int y, int range) {
        double dist;
        var allXY = structOfArraysEntity.getAllXY();
        var ids = new ArrayList<Integer>(allXY.length);
        for (int i = 0; i < structOfArraysEntity.length; i++) {
            dist = distance(x, y, allXY[i][0], allXY[i][1]);
            if (dist <= range) {
                ids.add(structOfArraysEntity.getId(i));
            }
        }
        return ids;
    }

    public List<Entity> getEnemiesAtRange(Coords coords, int range) {
        var ids = getEnemyIDsAtRange(coords, range);
        return getWorld().getEntities(ids);
    }

    public OptionalInt getDistanceToClosestEnemyAtFinalMovePathPosition() {
        return getDistanceToClosestEnemy(movePath.getFinalCoords());
    }

    public OptionalInt getDistanceToClosestEnemy(Entity self) {
        return getDistanceToClosestEnemy(self.getPosition());
    }

    public OptionalInt getDistanceToClosestEnemy(Coords coords) {
        var dist = Integer.MAX_VALUE;
        var x1 = coords.getX();
        var y1 = coords.getY();

        for (var xy : getWorld().getEnemies().getAllXY()) {
            dist = Math.min(distance(x1, y1, xy[0], xy[1]), dist);
        }
        if (dist == Integer.MAX_VALUE) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(dist);
    }

    public OptionalInt getDistanceToClosestEnemyWithRole(Coords coords, UnitRole role) {
        var dist = Integer.MAX_VALUE;
        var x1 = coords.getX();
        var y1 = coords.getY();
        var enemies = getWorld().getEnemies();
        var allXY = getWorld().getEnemies().getAllXY();
        for (int i = 0; i < allXY.length; i++) {
            if (enemies.getRole(i).equals(role)) {
                dist = Math.min(distance(x1, y1, allXY[i][0], allXY[i][1]), dist);
            }
        }
        if (dist == Integer.MAX_VALUE) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(dist);
    }

    private int distance(int x1, int y1, int x2, int y2) {
        int xd = Math.abs(x1 - x2);
        boolean xIsEven = ((x1 & 1) != 1);
        int yo = (xd / 2) + (xIsEven && ((x2 & 1) == 1) ? 1 : 0);
        int yMin = y1 - yo;
        int yMax = yMin + xd;
        int ym = 0;
        if (y2 < yMin) {
            ym = yMin - y2;
        }
        if (y2 > yMax) {
            ym = y2 - yMax;
        }
        return xd + ym;
    }


    public static final class TWDecisionContextBuilder {
        private MovePath movePath;
        private PathRankerUtilCalculator pathRankerUtilCalculator;
        private Intelligence<Entity, Entity, RankedPath> intelligence;
        private Memory memory;
        private BehaviorSettings behaviorSettings;
        private UnitBehavior.BehaviorType unitBehavior;
        private Coords waypoint;
        private FireControlState fireControlState;
        private TWWorld world;
        private Entity currentUnit;
        private List<Entity> targetUnits;
        private Map<String, Double> damageCache;

        public TWDecisionContextBuilder() {
        }

        public TWDecisionContextBuilder(TWDecisionContext other) {
            this.movePath = other.movePath;
            this.memory = other.memory;
            this.pathRankerUtilCalculator = other.pathRankerUtilCalculator;
            this.intelligence = other.intelligence;
            this.behaviorSettings = other.behaviorSettings;
            this.unitBehavior = other.unitBehavior;
            this.waypoint = other.waypoint;
            this.fireControlState = other.fireControlState;
            this.world = other.getWorld();
            this.currentUnit = other.getCurrentUnit();
            this.targetUnits = other.getEnemyUnits();
            this.damageCache = other.damageCache;
        }

        public static TWDecisionContextBuilder aTWDecisionContext() {
            return new TWDecisionContextBuilder();
        }

        public TWDecisionContextBuilder withMovePath(MovePath movePath) {
            this.movePath = movePath;
            return this;
        }

        public TWDecisionContextBuilder withPathRankerUtilCalculator(PathRankerUtilCalculator pathRankerUtilCalculator) {
            this.pathRankerUtilCalculator = pathRankerUtilCalculator;
            return this;
        }

        public TWDecisionContextBuilder withIntelligence(Intelligence<Entity, Entity, RankedPath> intelligence) {
            this.intelligence = intelligence;
            return this;
        }

        public TWDecisionContextBuilder withMemories(Memory memory) {
            this.memory = memory;
            return this;
        }

        public TWDecisionContextBuilder withBehaviorSettings(BehaviorSettings behaviorSettings) {
            this.behaviorSettings = behaviorSettings;
            return this;
        }

        public TWDecisionContextBuilder withUnitBehavior(UnitBehavior.BehaviorType unitBehavior) {
            this.unitBehavior = unitBehavior;
            return this;
        }

        public TWDecisionContextBuilder withWaypoint(Coords waypoint) {
            this.waypoint = waypoint;
            return this;
        }

        public TWDecisionContextBuilder withFireControlState(FireControlState fireControlState) {
            this.fireControlState = fireControlState;
            return this;
        }

        public TWDecisionContextBuilder withWorld(TWWorld world) {
            this.world = world;
            return this;
        }

        public TWDecisionContextBuilder withCurrentUnit(Entity currentUnit) {
            this.currentUnit = currentUnit;
            return this;
        }

        public TWDecisionContextBuilder withTargetUnits(List<Entity> targetUnits) {
            this.targetUnits = targetUnits;
            return this;
        }

        public TWDecisionContextBuilder withDamageCache(Map<String, Double> damageCache) {
            this.damageCache = damageCache;
            return this;
        }

        public TWDecisionContextBuilder withSharedDamageCache() {
            this.damageCache = new LinkedHashMap<>(128, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Double> eldest) {
                    return size() > DAMAGE_CACHE_SIZE;
                }
            };
            return this;
        }

        public TWDecisionContextBuilder but() {
            return aTWDecisionContext()
                .withMovePath(movePath)
                .withPathRankerUtilCalculator(pathRankerUtilCalculator)
                .withIntelligence(intelligence)
                .withBehaviorSettings(behaviorSettings)
                .withUnitBehavior(unitBehavior)
                .withWaypoint(waypoint)
                .withFireControlState(fireControlState)
                .withWorld(world)
                .withCurrentUnit(currentUnit)
                .withTargetUnits(targetUnits)
                .withDamageCache(damageCache);
        }

        public TWDecisionContext build() {
            return
                new TWDecisionContext(intelligence, world, memory, currentUnit, targetUnits, movePath, pathRankerUtilCalculator, unitBehavior,
                    behaviorSettings, fireControlState, waypoint, damageCache);
        }
    }
}
