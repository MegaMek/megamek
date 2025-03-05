/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 2 or (at your option) any later version,
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
 */
package megamek.client.bot.caspar;


import megamek.client.bot.Agent;
import megamek.common.Entity;
import megamek.common.MovePath;
import megamek.common.UnitRole;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Default implementation of the InputAxisCalculator interface.
 * Calculates all input axes as described in the CASPAR documentation.
 * @author Luana Coppio
 */
public class DefaultInputAxisCalculator implements InputAxisCalculator {
    // Registry of axis calculators
    private final AxisCalculator[] axisCalculators;
    private final double[] inputVector;

    /**
     * Creates an input axis calculator with all required axis calculators.
     */
    public DefaultInputAxisCalculator() {
        // Initialize all axis calculators
        this.axisCalculators = new AxisCalculator[AxisType.values().length];
        int inputLayerSize = 0;
        for (AxisType axisType : AxisType.values()) {
            axisCalculators[axisType.ordinal()] = createCalculatorForType(axisType);
            inputLayerSize += axisCalculators[axisType.ordinal()].axis().length;
        }

        this.inputVector = new double[inputLayerSize];
    }

    /**
     * Factory method to create the appropriate calculator for each axis type.
     *
     * @param type The axis type
     * @return An axis calculator
     */
    private AxisCalculator createCalculatorForType(AxisType type) {
        return switch (type) {
            case ENEMY_THREAT_HEATMAP -> new EnemyThreatHeatmapCalculator();
            case FRIENDLY_THREAT_HEATMAP -> new FriendlyThreatHeatmapCalculator();
            case ENEMY_THREAT_NEARBY -> new EnemyThreatNearbyCalculator();
            case FRIENDLY_THREAT_NEARBY -> new FriendlyThreatNearbyCalculator();
            case UNIT_HEALTH -> new UnitHealthCalculator();
            case POSITION_CROWDING -> new PositionCrowdingCalculator();
            case DAMAGE_RATIO -> new DamageRatioCalculator();
            case DECOY_POTENTIAL -> new DecoyPotentialCalculator();
            case ECM_COVERAGE -> new EcmCoverageCalculator();
            case ENEMY_ECM_COVERAGE -> new UnderEnemyEcmCoverageCalculator();
            case ARMY_COHESION -> new ArmyCohesionCalculator();
            case ENVIRONMENTAL_COVER -> new EnvironmentalCoverCalculator();
            case ENVIRONMENTAL_HAZARDS -> new EnvironmentalHazardsCalculator();
            case FACING_ENEMY -> new FacingEnemyCalculator();
            case FAVORITE_TARGET_IN_RANGE -> new FavoriteTargetInRangeCalculator();
            case FLANKING_POSITION -> new FlankingPositionCalculator();
            case FORMATION_COHESION -> new FormationCohesionCalculator();
            case FRIENDLY_ARTILLERY_FIRE -> new FriendlyArtilleryFireCalculator();
            case COVERING_UNITS -> new CoveringUnitsCalculator();
            case HEAT_MANAGEMENT -> new HeatManagementCalculator();
            case ENEMY_VIP_DISTANCE -> new EnemyVipDistanceCalculator();
            case NEARBY_ENEMY_COUNT -> new NearbyEnemyCountCalculator();
            case ORIGINAL_BOT_SETTINGS -> new OriginalBotSettingsCalculator();
            case IS_CRIPPLED -> new IsCrippledCalculator();
            case MOVING_TOWARD_WAYPOINT -> new MovingTowardWaypointCalculator();
            case UNIT_MOVEMENT -> new UnitMovementCalculator();
            case UNIT_ROLE -> new UnitRoleCalculator();
            case THREAT_BY_ROLE -> new ThreatByRoleCalculator();
            case UNIT_TMM -> new UnitTmmCalculator();
            case KILL_CHANCE -> new KillChanceCalculator();
            case PILOTING_CAUTION -> new PilotingCautionCalculator();
            case RETREAT -> new RetreatCalculator();
            case SCOUTING -> new ScoutingCalculator();
            case STANDING_STILL -> new StandingStillCalculator();
            case STRATEGIC_GOAL -> new StrategicGoalCalculator();
            case TARGET_HEALTH -> new TargetHealthCalculator();
            case TARGET_WITHIN_OPTIMAL_RANGE -> new TargetWithinOptimalRangeCalculator();
            case TURNS_TO_ENCOUNTER -> new TurnsToEncounterCalculator();
        };
    }

    @Override
    public double[] calculateInputVector(MovePath movePath, Agent agent, GameState gameState) {
        // Start index for writing values
        int index = 0;

        // Calculate and insert each axis group
        for (AxisType axisType : AxisType.values()) {
            AxisCalculator calculator = axisCalculators[axisType.ordinal()];
            double[] axisValues = calculator.calculateAxis(movePath, agent, gameState);

            // Copy values to the input vector
            System.arraycopy(axisValues, 0, inputVector, index, axisValues.length);
            index += axisValues.length;
        }

        return inputVector;
    }

    /**
     * Enum for different types of input axes.
     */
    public enum AxisType {
        ENEMY_THREAT_HEATMAP,
        FRIENDLY_THREAT_HEATMAP,
        ENEMY_THREAT_NEARBY,
        FRIENDLY_THREAT_NEARBY,
        UNIT_HEALTH,
        POSITION_CROWDING,
        DAMAGE_RATIO,
        DECOY_POTENTIAL,
        ECM_COVERAGE,
        ENEMY_ECM_COVERAGE,
        ARMY_COHESION,
        ENVIRONMENTAL_COVER,
        ENVIRONMENTAL_HAZARDS,
        FACING_ENEMY,
        FAVORITE_TARGET_IN_RANGE,
        FLANKING_POSITION,
        FORMATION_COHESION,
        FRIENDLY_ARTILLERY_FIRE,
        COVERING_UNITS,
        HEAT_MANAGEMENT,
        ENEMY_VIP_DISTANCE,
        NEARBY_ENEMY_COUNT,
        ORIGINAL_BOT_SETTINGS,
        IS_CRIPPLED,
        MOVING_TOWARD_WAYPOINT,
        UNIT_MOVEMENT,
        UNIT_ROLE,
        THREAT_BY_ROLE,
        UNIT_TMM,
        KILL_CHANCE,
        PILOTING_CAUTION,
        RETREAT,
        SCOUTING,
        STANDING_STILL,
        STRATEGIC_GOAL,
        TARGET_HEALTH,
        TARGET_WITHIN_OPTIMAL_RANGE,
        TURNS_TO_ENCOUNTER
    }

    /**
     * Interface for calculating specific input axes.
     */
    public interface AxisCalculator {
        /**
         * Returns the number of values this axis calculator will produce.
         * @return The number of values
         */
        default double[] axis() {
            return new double[1];
        }

        /**
         * Calculates one or more input axes for a movement path.
         *
         * @param movePath The movement path to evaluate
         * @return An array of normalized values (0-1)
         */
        double[] calculateAxis(MovePath movePath, Agent agent, GameState gameState);
    }

    /**
     * Base class for axis calculators.
     */
    public abstract static class BaseAxisCalculator implements AxisCalculator {
        /**
         * Normalizes a value to the range [0, 1].
         *
         * @param value The value to normalize
         * @param min The minimum expected value
         * @param max The maximum expected value
         * @return A normalized value between 0 and 1
         */
        protected double normalize(double value, double min, double max) {
            if (max == min) {
                return 0.5; // Avoid division by zero
            }
            double normalized = (value - min) / (max - min);
            return Math.max(0, Math.min(1, normalized));
        }
    }

    // Example implementation for a few calculators

    /**
     * Calculates the enemy threat heatmap (100 values).
     */
    public static class EnemyThreatHeatmapCalculator extends BaseAxisCalculator {

        @Override
        public double[] axis() {
            return new double[31];
        }

        @Override
        public double[] calculateAxis(MovePath movePath, Agent agent, GameState gameState) {
            // This would calculate the 10x10 heatmap for enemy threats
            double[] heatmap = axis();

            // Implementation goes here
            // For now, we'll just return a placeholder

            return heatmap;
        }
    }

    /**
     * Calculates the friendly threat heatmap (100 values).
     */
    public static class FriendlyThreatHeatmapCalculator extends BaseAxisCalculator {

        @Override
        public double[] axis() {
            return new double[100];
        }

        @Override
        public double[] calculateAxis(MovePath movePath, Agent agent, GameState gameState) {
            // This would calculate the 10x10 heatmap for friendly threats
            double[] heatmap = axis();

            // Implementation goes here

            return heatmap;
        }
    }

    /**
     * Calculates the enemy threat in nearby hexes (31 values).
     */
    public static class EnemyThreatNearbyCalculator extends BaseAxisCalculator {

        @Override
        public double[] axis() {
            return new double[31];
        }

        @Override
        public double[] calculateAxis(MovePath movePath, Agent agent, GameState gameState) {
            // This would calculate the threat in the final position and all hexes up to 3 away
            double[] threats = axis();

            // Implementation goes here

            return threats;
        }
    }

    /**
     * Calculates the enemy threat in nearby hexes (31 values).
     */
    public static class FriendlyThreatNearbyCalculator extends BaseAxisCalculator {

        @Override
        public double[] axis() {
            return new double[31];
        }

        @Override
        public double[] calculateAxis(MovePath movePath, Agent agent, GameState gameState) {
            // This would calculate the threat in the final position and all hexes up to 3 away
            double[] threats = axis();

            // Implementation goes here

            return threats;
        }
    }

    /**
     * Calculates the unit health (5 values, average, front, left, right, rear).
     */
    public static class UnitHealthCalculator extends BaseAxisCalculator {

        @Override
        public double[] axis() {
            return new double[5];
        }

        @Override
        public double[] calculateAxis(MovePath movePath, Agent agent, GameState gameState) {
            // This would calculate the health of the unit as a percentage for
            // 0 - average
            // 1 - front
            // 2 - left
            // 3 - right
            // 4 - rear

            double[] health = axis();

            // Implementation goes here

            return health;
        }
    }

    /**
     * Calculates how crowded is the area of 31 hexes around the final position coordinate.
     */
    public static class PositionCrowdingCalculator extends BaseAxisCalculator {

        @Override
        public double[] calculateAxis(MovePath movePath, Agent agent, GameState gameState) {
            // This would calculate the health of the unit as a percentage for
            // 0 - average
            // 1 - front
            // 2 - left
            // 3 - right
            // 4 - rear

            double[] health = axis();

            // Implementation goes here

            return health;
        }
    }


    /**
     * Calculates the best expected damage ratio for the current attack
     */
    public static class DamageRatioCalculator extends BaseAxisCalculator {
        @Override
        public double[] calculateAxis(MovePath movePath, Agent agent, GameState gameState) {
            // This calculates the best expected damage ratio for the current attack
            double[] damageRatio = axis();

            // Implementation goes here

            return damageRatio;
        }
    }

    /**
     * Calculates the potential of the unit to act as a decoy
     */
    public static class DecoyPotentialCalculator extends BaseAxisCalculator {
        @Override
        public double[] calculateAxis(MovePath movePath, Agent agent, GameState gameState) {
            // This calculates the potential of the unit to act as a decoy
            double[] decoyPotential = axis();

            // Implementation goes here

            return decoyPotential;
        }
    }

    /**
     * Calculates the ecm coverage of the unit ECM in the map
     */
    public static class EcmCoverageCalculator extends BaseAxisCalculator {
        @Override
        public double[] calculateAxis(MovePath movePath, Agent agent, GameState gameState) {
            // This calculates the potential of the unit to act as a decoy
            double[] ecmCoverage = axis();

            // Implementation goes here

            return ecmCoverage;
        }
    }

    /**
     * Calculates if the unit is under enemy ECM coverage
     */
    public static class UnderEnemyEcmCoverageCalculator extends BaseAxisCalculator {
        @Override
        public double[] calculateAxis(MovePath movePath, Agent agent, GameState gameState) {
            // This calculates the potential of the unit to act as a decoy
            double[] ecmCoverage = axis();

            // Implementation goes here

            return ecmCoverage;
        }
    }

    /**
     * Calculates the cohesion of the enemy army
     */
    public static class ArmyCohesionCalculator extends BaseAxisCalculator {
        @Override
        public double[] calculateAxis(MovePath movePath, Agent agent, GameState gameState) {
            // This calculates the potential of the unit to act as a decoy
            double[] armyCohesion = axis();
            armyCohesion[0] = gameState.getEnemyUnitsCohesion();
            return armyCohesion;
        }
    }

    /**
     * Calculates the environmental cover of the final position against 5 enemy units that have you in sight
     */
    public static class EnvironmentalCoverCalculator extends BaseAxisCalculator {
        @Override
        public double[] calculateAxis(MovePath movePath, Agent agent, GameState gameState) {
            // This calculates the potential of the unit to act as a decoy
            double[] cover = axis();

            return cover;
        }
    }

    /**
     * Calculates the environmental hazards around the final position
     */
    public static class EnvironmentalHazardsCalculator extends BaseAxisCalculator {
        @Override
        public double[] calculateAxis(MovePath movePath, Agent agent, GameState gameState) {
            // This calculates the potential of the unit to act as a decoy
            double[] hazards = axis();

            return hazards;
        }
    }

    /**
     * Calculates the facing of the unit against the 5 closest enemy units in the final position
     */
    public static class FacingEnemyCalculator extends BaseAxisCalculator {
        @Override
        public double[] calculateAxis(MovePath movePath, Agent agent, GameState gameState) {
            // This calculates if the unit is facing the enemy
            double[] facing = axis();

            return facing;
        }
    }

    /**
     * Calculates if the favorite target role type is in range
     */
    public static class FavoriteTargetInRangeCalculator extends BaseAxisCalculator {

        @Override
        public double[] axis() {
            return new double[3];
        }

        @Override
        public double[] calculateAxis(MovePath movePath, Agent agent, GameState gameState) {
            // calculate if the favorite target role type is in range
            double[] favoriteTarget = axis();
            // 0 - SNIPER
            // 1 - MISSILE BOAT
            // 2 - JUGGERNAUT
            return favoriteTarget;
        }
    }

    /**
     * Calculates if the unit is in a flanking position
     */
    public static class FlankingPositionCalculator extends BaseAxisCalculator {
        @Override
        public double[] calculateAxis(MovePath movePath, Agent agent, GameState gameState) {
            // This calculates if the unit is in a flanking position
            double[] flanking = axis();

            return flanking;
        }
    }

    /**
     * Calculates the formation cohesion of the unit
     */
    public static class FormationCohesionCalculator extends BaseAxisCalculator {
        @Override
        public double[] calculateAxis(MovePath movePath, Agent agent, GameState gameState) {
            // This calculates the formation cohesion of the unit
            double[] formationCohesion = axis();

            return formationCohesion;
        }
    }

    /**
     * Calculates the friendly artillery fire potential
     */
    public static class FriendlyArtilleryFireCalculator extends BaseAxisCalculator {
        @Override
        public double[] calculateAxis(MovePath movePath, Agent agent, GameState gameState) {
            // This calculates the friendly artillery fire potential
            double[] artilleryFire = axis();

            return artilleryFire;
        }
    }

    /**
     * Calculates the number of units that the unit is covering
     */
    public static class CoveringUnitsCalculator extends BaseAxisCalculator {
        @Override
        public double[] calculateAxis(MovePath movePath, Agent agent, GameState gameState) {
            // This calculates the number of units that the unit is covering
            double[] coveringUnits = axis();

            return coveringUnits;
        }
    }

    /**
     * Calculates the heat management of the unit
     */
    public static class HeatManagementCalculator extends BaseAxisCalculator {
        @Override
        public double[] calculateAxis(MovePath movePath, Agent agent, GameState gameState) {
            // This calculates the heat management of the unit
            double[] heatManagement = axis();

            return heatManagement;
        }
    }

    /**
     * Calculates the distance to the closest enemy VIP
     */
    public static class EnemyVipDistanceCalculator extends BaseAxisCalculator {
        @Override
        public double[] calculateAxis(MovePath movePath, Agent agent, GameState gameState) {
            // This calculates the distance to the closest enemy VIP
            double[] vipDistance = axis();

            return vipDistance;
        }
    }

    /**
     * Calculates the number of nearby enemy units
     */
    public static class NearbyEnemyCountCalculator extends BaseAxisCalculator {
        @Override
        public double[] calculateAxis(MovePath movePath, Agent agent, GameState gameState) {
            // This calculates the number of nearby enemy units
            double[] nearbyEnemies = axis();

            return nearbyEnemies;
        }
    }

    /**
     * Calculates the original bot settings
     */
    public static class OriginalBotSettingsCalculator extends BaseAxisCalculator {
        @Override
        public double[] calculateAxis(MovePath movePath, Agent agent, GameState gameState) {
            // This calculates the original bot settings
            double[] botSettings = axis();

            return botSettings;
        }
    }

    /**
     * Calculates if the unit is crippled
     */
    public static class IsCrippledCalculator extends BaseAxisCalculator {
        @Override
        public double[] calculateAxis(MovePath movePath, Agent agent, GameState gameState) {
            // This calculates if the unit is crippled
            double[] isCrippled = axis();

            return isCrippled;
        }
    }

    /**
     * Calculates if the unit is moving toward the waypoint
     */
    public static class MovingTowardWaypointCalculator extends BaseAxisCalculator {
        @Override
        public double[] calculateAxis(MovePath movePath, Agent agent, GameState gameState) {
            // This calculates if the unit is moving toward the waypoint
            double[] movingTowardWaypoint = axis();

            return movingTowardWaypoint;
        }
    }

    /**
     * Calculates the unit movement
     */
    public static class UnitMovementCalculator extends BaseAxisCalculator {
        @Override
        public double[] calculateAxis(MovePath movePath, Agent agent, GameState gameState) {
            // This calculates the unit movement
            double[] unitMovement = axis();

            return unitMovement;
        }
    }
    /**
     * Calculates the unit role
     */
    public static class UnitRoleCalculator extends BaseAxisCalculator {
        @Override
        public double[] axis() {
            return new double[UnitRole.values().length];
        }
        @Override
        public double[] calculateAxis(MovePath movePath, Agent agent, GameState gameState) {
            // This calculates the unit role
            double[] unitRole = axis();
            Entity unit = movePath.getEntity();
            unitRole[unit.getRole().ordinal()] = 1d;
            return unitRole;
        }
    }

    /**
     * Calculates the threat by role
     */
    public static class ThreatByRoleCalculator extends BaseAxisCalculator {
        @Override
        public double[] axis() {
            return new double[UnitRole.values().length];
        }
        @Override
        public double[] calculateAxis(MovePath movePath, Agent agent, GameState gameState) {
            // This calculates the threat by role
            double[] threatByRole = axis();

            return threatByRole;
        }
    }

    /**
     * Calculates the unit TMM
     */
    public static class UnitTmmCalculator extends BaseAxisCalculator {
        @Override
        public double[] calculateAxis(MovePath movePath, Agent agent, GameState gameState) {
            // This calculates the unit TMM
            double[] unitTmm = axis();

            return unitTmm;
        }
    }

    /**
     * Calculates the kill chance
     */
    public static class KillChanceCalculator extends BaseAxisCalculator {
        @Override
        public double[] calculateAxis(MovePath movePath, Agent agent, GameState gameState) {
            // This calculates the kill chance
            double[] killChance = axis();

            return killChance;
        }
    }

    /**
     * Calculates the piloting caution
     */
    public static class PilotingCautionCalculator extends BaseAxisCalculator {
        @Override
        public double[] calculateAxis(MovePath movePath, Agent agent, GameState gameState) {
            // This calculates the piloting caution
            double[] pilotingCaution = axis();

            return pilotingCaution;
        }
    }

    /**
     * Calculates the retreat
     */
    public static class RetreatCalculator extends BaseAxisCalculator {
        @Override
        public double[] calculateAxis(MovePath movePath, Agent agent, GameState gameState) {
            // This calculates the retreat
            double[] retreat = axis();

            return retreat;
        }
    }

    /**
     * Calculates the scouting
     */
    public static class ScoutingCalculator extends BaseAxisCalculator {
        @Override
        public double[] calculateAxis(MovePath movePath, Agent agent, GameState gameState) {
            // This calculates the scouting
            double[] scouting = axis();

            return scouting;
        }
    }

    /**
     * Calculates the standing still
     */
    public static class StandingStillCalculator extends BaseAxisCalculator {
        @Override
        public double[] calculateAxis(MovePath movePath, Agent agent, GameState gameState) {
            // This calculates the standing still
            double[] standingStill = axis();

            return standingStill;
        }
    }

    /**
     * Calculates the strategic goal
     */
    public static class StrategicGoalCalculator extends BaseAxisCalculator {
        @Override
        public double[] calculateAxis(MovePath movePath, Agent agent, GameState gameState) {
            // This calculates the strategic goal
            double[] strategicGoal = axis();

            return strategicGoal;
        }
    }

    /**
     * Calculates the target health
     */
    public static class TargetHealthCalculator extends BaseAxisCalculator {
        @Override
        public double[] calculateAxis(MovePath movePath, Agent agent, GameState gameState) {
            // This calculates the target health
            double[] targetHealth = axis();

            return targetHealth;
        }
    }

    /**
     * Calculates the target within optimal range
     */
    public static class TargetWithinOptimalRangeCalculator extends BaseAxisCalculator {
        @Override
        public double[] calculateAxis(MovePath movePath, Agent agent, GameState gameState) {
            // This calculates the target within optimal range
            double[] targetWithinOptimalRange = axis();

            return targetWithinOptimalRange;
        }
    }

    /**
     * Calculates the turns to encounter
     */
    public static class TurnsToEncounterCalculator extends BaseAxisCalculator {
        @Override
        public double[] calculateAxis(MovePath movePath, Agent agent, GameState gameState) {
            // This calculates the turns to encounter
            double[] turnsToEncounter = axis();

            return turnsToEncounter;
        }
    }

}
