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

import megamek.ai.utility.*;
import megamek.client.bot.caspar.ai.utility.tw.ClusteringService;
import megamek.client.bot.caspar.ai.utility.tw.context.TWWorld;
import megamek.client.bot.caspar.ai.utility.tw.intelligence.PathRankerUtilCalculator;
import megamek.client.bot.princess.BehaviorSettings;
import megamek.client.bot.princess.FireControlState;
import megamek.client.bot.princess.FiringPhysicalDamage;
import megamek.client.bot.princess.UnitBehavior;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.MovePath;
import megamek.common.Targetable;
import megamek.common.annotations.Nullable;
import megamek.common.options.OptionsConstants;

import java.util.*;
import java.util.stream.Collectors;

import static megamek.client.bot.princess.FireControl.getMaxDamageAtRange;


public class TWDecisionContext extends DecisionContext {
    private final PathRankerUtilCalculator pathRankerUtilCalculator;
    private final Intelligence intelligence;
    private FiringPhysicalDamage cachedDamage;

    public TWDecisionContext(
        Intelligence intelligence,
        World world,
        Entity currentUnit,
        MovePath movePath,
        PathRankerUtilCalculator pathRankerUtilCalculator,
        UnitBehavior.BehaviorType unitBehavior,
        BehaviorSettings behaviorSettings,
        FireControlState fireControlState,
        @Nullable Coords waypoint,
        StrategicGoalsManager strategicGoalsManager,
        ClusteringService clusteringService,
        Map<String, Double> damageCache
    )
    {
        super(
            world,
            movePath,
            waypoint,
            strategicGoalsManager,
            new ThreatAssessment() {
                @Override
                public double getEnemyThreat(Coords position) {
                    return world.getQuickBoardRepresentation().getThreatLevel(position);
                }

                @Override
                public double calculateUnitMaxDamageAtRange(Targetable unit, int enemyRange) {
                    if (unit instanceof Entity entity) {
                        return getMaxDamageAtRange(entity, enemyRange,
                            world.useBooleanOption(OptionsConstants.ADVCOMBAT_TACOPS_LOS_RANGE),
                            world.useBooleanOption(OptionsConstants.ADVCOMBAT_TACOPS_RANGE));
                    } else {
                        return 0d;
                    }
                }

                @Override
                public StructOfUnitArrays getStructOfEnemiesArrays() {
                    return world.getStructOfEnemyUnitArrays();
                }

                @Override
                public StructOfUnitArrays getStructOfAlliesArrays() {
                    return world.getStructOfAllyUnitArrays();
                }

                @Override
                public StructOfUnitArrays getStructOfOwnUnitsArrays() {
                    return world.getStructOfOwnUnitsArrays();
                }

                @Override
                public Map<Coords, Double> getIncomingFriendlyArtilleryDamage() {
                    return pathRankerUtilCalculator.getIncomingFriendlyArtilleryDamage();
                }

                @Override
                public List<Targetable> getEnemiesList() {
                    return new ArrayList<>(world.getEnemyUnits());
                }

                @Override
                public List<Targetable> getAlliesList() {
                    return new ArrayList<>(world.getAlliedUnits());
                }

                @Override
                public List<Targetable> getOwnUnitsList() {
                    return new ArrayList<>(world.getMyUnits());
                }

                @Override
                public double getArmorRemainingPercent(Targetable target) {
                    if (target instanceof Entity entity) {
                        return entity.getArmorRemainingPercent();
                    }
                    return 0;
                }

                @Override
                public double maxAmountOfDamageFromFriendsInRange(Coords targetCoords, int range) {
                    var targetX = targetCoords.getX();
                    var targetY = targetCoords.getY();
                    var ids = getFriendIDsAtRange(targetCoords, range);
                    var entities = getEntities(ids);
                    var totalDamage = 0d;
                    for (var entity : entities) {
                        var rangeTowardsTarget = Coords.distance(entity.getPosition().getX(), entity.getPosition().getY(), targetX, targetY);
                        totalDamage += calculateUnitMaxDamageAtRange(entity, rangeTowardsTarget);
                    }
                    return totalDamage;
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

                @Override
                public Optional<Targetable> getClosestVIP(Coords finalCoords) {
                    Set<Targetable> entities = world.getEnemyUnits()
                        .stream()
                        .filter(e -> e instanceof Entity entity && isVIP(entity))
                        .collect(Collectors.toSet());
                    return entities.stream().min(Comparator.comparingInt(entity -> entity.getPosition().distance(finalCoords)));
                }
            },
            new UnitInformationProvider() {
                @Override
                public Set<Coords> getCoordsSet() {
                    return movePath.getCoordsSet();
                }

                @Override
                public Coords getFinalPosition() {
                    return movePath.getFinalCoords();
                }

                @Override
                public Coords getStartingPosition() {
                    return movePath.getStartCoords();
                }

                @Override
                public int getFinalFacing() {
                    return movePath.getFinalFacing();
                }

                @Override
                public int getDistanceMoved() {
                    return movePath.getDistanceTravelled();
                }

                @Override
                public int getHexesMoved() {
                    return movePath.getHexesMoved();
                }

                @Override
                public boolean isJumping() {
                    return movePath.isJumping();
                }

                @Override
                public int getFinalAltitude() {
                    return movePath.getFinalAltitude();
                }

                @Override
                public double getMovePathSuccessProbability() {
                    return pathRankerUtilCalculator.getMovePathSuccessProbability(movePath);
                }

                @Override
                public int getMaxRunMP() {
                    return currentUnit.getRunMP();
                }

                @Override
                public int getMaxWeaponRange() {
                    return currentUnit.getMaxWeaponRange();
                }

                @Override
                public UnitBehavior.BehaviorType getBehaviorType() {
                    return unitBehavior;
                }

                @Override
                public Entity getCurrentUnit() {
                    return currentUnit;
                }

                @Override
                public int getTotalHealth() {
                    return currentUnit.getTotalArmor();
                }

                @Override
                public int getHeatCapacity() {
                    return currentUnit.getHeatCapacity();
                }

                @Override
                public Coords getEntityClusterCentroid(Targetable self) {
                    return clusteringService.getClusterMidpoint(self);
                }
            },
            new DamageCalculator() {
                @Override
                public double firingDamage() {
                    return 0;
                }

                @Override
                public double physicalDamage() {
                    return 0;
                }

                @Override
                public double expectedDamage() {
                    return 0;
                }
            },
            behaviorSettings,
            damageCache);
        this.intelligence = intelligence;
        this.pathRankerUtilCalculator = pathRankerUtilCalculator;

        setDamageCalculator(new DamageCalculator() {
            @Override
            public double firingDamage() {
                return cachedDamage.firingDamage;
            }

            @Override
            public double physicalDamage() {
                return cachedDamage.physicalDamage;
            }

            @Override
            public double expectedDamage() {
                return cachedDamage.takenDamage;
            }
        });
    }

    @Override
    public double getBonusFactor() {
        return intelligence.getBonusFactor(getCurrentUnit());
    }

    @Override
    public double getExpectedDamage() {
        return cacheDamage().takenDamage;
    }

    @Override
    public double getTotalDamage() {
        return cacheDamage().getMaximumDamageEstimate();
    }

    @Override
    public double getFiringDamage() {
        return cacheDamage().firingDamage;
    }

    @Override
    public double getPhysicalDamage() {
        return cacheDamage().physicalDamage;
    }

    private FiringPhysicalDamage cacheDamage() {
        if (cachedDamage == null && pathRankerUtilCalculator != null) {
            List<Entity> enemiesList = new ArrayList<>();
            for (var entity : getWorld().getEnemyUnits()) {
                if (entity instanceof Entity enemy) {
                    enemiesList.add(enemy);
                }
            }
            cachedDamage = pathRankerUtilCalculator.damageCalculator(getMovePath(), enemiesList);
        }
        return cachedDamage;
    }


    public enum VIPType {
        COMMANDER,
        SUB_COMMANDER,
        NONE
    }

    public Coords getMyClusterCentroid(Targetable entity) {
        return getWorld().getEntityClusterCentroid(entity);
    }

    @Override
    public TWWorld getWorld() {
        return (TWWorld) super.getWorld();
    }

    public static final class TWDecisionContextBuilder {
        private PathRankerUtilCalculator pathRankerUtilCalculator;
        private Intelligence intelligence;
        private FiringPhysicalDamage cachedDamage;
        private World world;
        private MovePath movePath;
        private Coords waypoint;
        private Entity currentUnit;
        private UnitBehavior.BehaviorType unitBehavior;
        private StrategicGoalsManager strategicGoalsManager;
        private BehaviorSettings behaviorSettings;
        private Map<String, Double> damageCache;
        private ThreatAssessment threatAssessment;
        private UnitInformationProvider unitInformationProvider;
        private DamageCalculator damageCalculator;
        private FireControlState fireControlState;
        private ClusteringService clusteringService;

        private TWDecisionContextBuilder() {
        }

        public static TWDecisionContextBuilder aTWDecisionContext() {
            return new TWDecisionContextBuilder();
        }

        public TWDecisionContextBuilder withPathRankerUtilCalculator(PathRankerUtilCalculator pathRankerUtilCalculator) {
            this.pathRankerUtilCalculator = pathRankerUtilCalculator;
            return this;
        }

        public TWDecisionContextBuilder withIntelligence(Intelligence intelligence) {
            this.intelligence = intelligence;
            return this;
        }

        public TWDecisionContextBuilder withCachedDamage(FiringPhysicalDamage cachedDamage) {
            this.cachedDamage = cachedDamage;
            return this;
        }

        public TWDecisionContextBuilder withUnitBehaviorType(UnitBehavior.BehaviorType unitBehavior) {
            this.unitBehavior = unitBehavior;
            return this;
        }

        public TWDecisionContextBuilder withFireControlState(FireControlState fireControlState) {
            this.fireControlState = fireControlState;
            return this;
        }



        public TWDecisionContextBuilder withCurrentUnit(Entity currentUnit) {
            this.currentUnit = currentUnit;
            return this;
        }

        public TWDecisionContextBuilder withWorld(World world) {
            this.world = world;
            return this;
        }

        public TWDecisionContextBuilder withMovePath(MovePath movePath) {
            this.movePath = movePath;
            return this;
        }

        public TWDecisionContextBuilder withWaypoint(Coords waypoint) {
            this.waypoint = waypoint;
            return this;
        }

        public TWDecisionContextBuilder withStrategicGoalsManager(StrategicGoalsManager strategicGoalsManager) {
            this.strategicGoalsManager = strategicGoalsManager;
            return this;
        }

        public TWDecisionContextBuilder withBehaviorSettings(BehaviorSettings behaviorSettings) {
            this.behaviorSettings = behaviorSettings;
            return this;
        }

        public TWDecisionContextBuilder withDamageCache(Map<String, Double> damageCache) {
            this.damageCache = damageCache;
            return this;
        }

        public TWDecisionContextBuilder withThreatAssessment(ThreatAssessment threatAssessment) {
            this.threatAssessment = threatAssessment;
            return this;
        }

        public TWDecisionContextBuilder withUnitInformationProvider(UnitInformationProvider unitInformationProvider) {
            this.unitInformationProvider = unitInformationProvider;
            return this;
        }

        public TWDecisionContextBuilder withDamageCalculator(DamageCalculator damageCalculator) {
            this.damageCalculator = damageCalculator;
            return this;
        }

        public TWDecisionContextBuilder but() {
            return aTWDecisionContext()
                .withPathRankerUtilCalculator(pathRankerUtilCalculator)
                .withIntelligence(intelligence)
                .withCachedDamage(cachedDamage)
                .withWorld(world)
                .withMovePath(movePath)
                .withWaypoint(waypoint)
                .withStrategicGoalsManager(strategicGoalsManager)
                .withBehaviorSettings(behaviorSettings)
                .withUnitBehaviorType(unitBehavior)
                .withCurrentUnit(currentUnit)
                .withFireControlState(fireControlState)
                .withDamageCache(damageCache)
                .withThreatAssessment(threatAssessment)
                .withUnitInformationProvider(unitInformationProvider)
                .withDamageCalculator(damageCalculator);
        }

        public TWDecisionContext build() {
            TWDecisionContext tWDecisionContext = new TWDecisionContext(
                intelligence,
                world,
                currentUnit,
                movePath,
                pathRankerUtilCalculator,
                unitBehavior,
                behaviorSettings,
                fireControlState,
                waypoint,
                strategicGoalsManager,
                clusteringService,
                damageCache);
//            tWDecisionContext.setThreatAssessment(threatAssessment);
//            tWDecisionContext.setUnitInformationProvider(unitInformationProvider);
//            tWDecisionContext.setDamageCalculator(damageCalculator);
            tWDecisionContext.cachedDamage = this.cachedDamage;
            return tWDecisionContext;
        }
    }
}
