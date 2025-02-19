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
package megamek.ai.utility;

import megamek.ai.dataset.UnitAction;
import megamek.ai.dataset.UnitState;
import megamek.client.bot.princess.BehaviorSettings;
import megamek.client.bot.princess.FireControl;
import megamek.client.bot.princess.UnitBehavior;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.MovePath;
import megamek.common.Targetable;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Concrete implementation of the DecisionContext abstract class.
 * Provides concrete implementations for the abstract methods and the extension point interfaces.
 *
 * @author Luana Coppio
 */
public class ConcreteDecisionContext extends DecisionContext {

    private final double bonusFactor;

    public ConcreteDecisionContext(
        World world,
        MovePath movePath,
        Coords waypoint,
        Map<String, Double> damageCache,
        double bonusFactor,
        StrategicGoalsManager strategicGoalsManager,
        ThreatAssessment threatAssessment,
        UnitInformationProvider unitInformationProvider,
        BehaviorSettings behaviorSettings,
        DamageCalculator damageCalculator)
    {
        super(
            world,
            movePath,
            waypoint,
            strategicGoalsManager,
            threatAssessment,
            unitInformationProvider,
            damageCalculator,
            behaviorSettings,
            damageCache);
        this.bonusFactor = bonusFactor;
    }

    /**
     * Gets the bonus factor for the decision.
     *
     * @return The bonus factor.
     */
    @Override
    public double getBonusFactor() {
        return bonusFactor;
    }

    /**
     * Concrete implementation of the ThreatAssessment interface.
     */
    public static class ConcreteThreatAssessment implements ThreatAssessment {
        private final QuickBoardRepresentation quickBoardRepresentation;
        private final List<Targetable> friendlies;
        private final List<Targetable> enemies;
        private final List<Targetable> ownUnits;
        private final StructOfUnitArrays friendsStruct;
        private final StructOfUnitArrays enemiesStruct;
        private final StructOfUnitArrays ownStruct;

        public ConcreteThreatAssessment(
            QuickBoardRepresentation quickBoardRepresentation,
            List<Targetable> friendlies,
            List<Targetable> enemies,
            List<Targetable> ownUnits,
            StructOfUnitArrays friendsStruct,
            StructOfUnitArrays enemiesStruct,
            StructOfUnitArrays ownStruct
        )
        {
            this.quickBoardRepresentation = quickBoardRepresentation;
            this.friendlies = friendlies;
            this.enemies = enemies;
            this.ownUnits = ownUnits;
            this.friendsStruct = friendsStruct;
            this.enemiesStruct = enemiesStruct;
            this.ownStruct = ownStruct;
        }

        /**
         * Gets a list of friendly units within a specified range of a position.
         *
         * @param position The coordinates to check.
         * @param range The range within which to find friendly units.
         * @return A list of friendly units within the specified range.
         */
        @Override
        public List<Targetable> getFriendliesWithinRange(Coords position, int range) {
            return friendlies.stream()
                .filter(f -> f.getPosition().distance(position) <= range)
                .collect(Collectors.toList());
        }

        /**
         * Gets a list of enemy units within a specified range of a position.
         *
         * @param position The coordinates to check.
         * @return A list of enemy units within the specified range.
         */
        @Override
        public List<Targetable> getEnemiesWithinRange(Coords position, int range) {
            return enemies.stream()
                .filter(e -> e.getPosition().distance(position) <= range)
                .collect(Collectors.toList());
        }

        @Override
        public double calculateUnitMaxDamageAtRange(Targetable unit, int enemyRange) {
            if (unit instanceof UnitState unitState) {
                return FireControl.getMaxDamageAtRange(unitState.entity(),
                enemyRange,
                false,
                false);
            }
            return 0;
        }

        @Override
        public double maxAmountOfDamageFromFriendsInRange(Coords targetCoords, int range) {
            var targetX = targetCoords.getX();
            var targetY = targetCoords.getY();
            var totalDamage = 0d;
            for (var unitState : friendlies) {
                var rangeTowardsTarget = Coords.distance(unitState.getPosition().getX(), unitState.getPosition().getY(), targetX, targetY);
                if (rangeTowardsTarget <= range) {
                    totalDamage +=  calculateUnitMaxDamageAtRange(unitState, rangeTowardsTarget);
                }
            }
            return totalDamage;
        }

        @Override
        public StructOfUnitArrays getStructOfEnemiesArrays() {
            return enemiesStruct;
        }

        @Override
        public StructOfUnitArrays getStructOfAlliesArrays() {
            return friendsStruct;
        }

        @Override
        public StructOfUnitArrays getStructOfOwnUnitsArrays() {
            return ownStruct;
        }

        @Override
        public Map<Coords, Double> getIncomingFriendlyArtilleryDamage() {
            return Map.of();
        }

        @Override
        public Optional<Targetable> getClosestVIP(Coords position) {
            Set<Targetable> entities = getEnemiesList().stream()
                .map(e -> (UnitState) e)
                .filter(e -> e.entity().isCommander() || e.entity().hasC3M() || e.entity().hasC3i() || e.entity().hasC3MM())
                .collect(Collectors.toSet());
            return entities.stream().min(Comparator.comparingInt(e -> e.getPosition().distance(position)));
        }

        @Override
        public List<Targetable> getEnemiesList() {
            return new ArrayList<>(enemies);
        }

        @Override
        public List<Targetable> getAlliesList() {
            return new ArrayList<>(friendlies);
        }

        @Override
        public List<Targetable> getOwnUnitsList() {
            return new ArrayList<>(ownUnits);
        }

        @Override
        public double getArmorRemainingPercent(Targetable target) {
            if (target instanceof UnitState unitState) {
                return unitState.armorP();
            }
            return 0.0;
        }

        /**
         * Gets the enemy threat level at the specified coordinates.
         *
         * @param position The coordinates to assess.
         * @return The enemy threat level.
         */
        @Override
        public double getEnemyThreat(Coords position) {
            return quickBoardRepresentation.getThreatLevel(position);
        }
    }

    /**
     * Concrete implementation of the UnitInformationProvider interface.
     */
    public static class ConcreteUnitInformationProvider implements UnitInformationProvider {
        private final Entity currentUnit;
        private final UnitAction unitAction;
        private final MovePath movePath;
        private final int finalFacing;

        public ConcreteUnitInformationProvider(
            Entity currentUnit,
            UnitAction unitAction,
            MovePath movePath,
            int finalFacing
        ) {
            this.currentUnit = currentUnit;
            this.unitAction = unitAction;
            this.movePath = movePath;
            this.finalFacing = finalFacing;
        }

        @Override
        public int getMaxRunMP() {
            return currentUnit.getRunMP();
        }

        @Override
        public UnitBehavior.BehaviorType getBehaviorType() {
            return UnitBehavior.BehaviorType.Engaged;
        }

        @Override
        public int getMaxWeaponRange() {
            return currentUnit.getMaxWeaponRange();
        }

        @Override
        public int getTotalHealth() {
            return (int) Math.round(unitAction.armorP() * currentUnit.getTotalOArmor());
        }

        @Override
        public int getHeatCapacity() {
            return currentUnit.getHeatCapacity();
        }

        @Override
        public Coords getEntityClusterCentroid(Targetable self) {
            return null;
        }

        @Override
        public Entity getCurrentUnit() {
            return currentUnit;
        }

        @Override
        public Set<Coords> getCoordsSet() {
            return movePath.getCoordsSet();
        }

        @Override
        public Coords getFinalPosition() {
            return unitAction.finalPosition();
        }

        @Override
        public Coords getStartingPosition() {
            return unitAction.currentPosition();
        }

        @Override
        public int getFinalFacing() {
            return finalFacing;
        }

        @Override
        public int getDistanceMoved() {
            return unitAction.distance();
        }

        @Override
        public int getHexesMoved() {
            return unitAction.hexesMoved();
        }

        @Override
        public boolean isJumping() {
            return unitAction.jumping();
        }

        @Override
        public int getFinalAltitude() {
            return movePath.getFinalAltitude();
        }

        @Override
        public double getMovePathSuccessProbability() {
            return 1d;
        }

    }

    /**
     * Concrete implementation of the DamageCalculator interface.
     * @param firingDamage   The firing damage.
     * @param physicalDamage The physical damage.
     * @param expectedDamage The expected damage.
     */
    public record ConcreteDamageCalculator(double firingDamage, double physicalDamage, double expectedDamage) implements DamageCalculator {
    }

}
