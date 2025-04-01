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
package megamek.client.bot.caspar.axis;

import megamek.ai.axis.AxisCalculator;

/**
 * Enum for different types of input axes.
 */
public enum AxisType {
    MP_PERCENTAGE(new MpPercentageCalculator()),
    HEAT_PERCENTAGE(new HeatManagementCalculator()),
    ARMOR_PERCENTAGE(new MyArmorPercentageCalculator()),
    INTERNAL_PERCENTAGE(new InternalPercentageCalculator()),
    JUMPING(new IsJumpingCalculator()),
    DISTANCE_TRAVELED(new DistanceTraveledCalculator()),
    HEXES_MOVED(new HexesMovedCalculator()),
    CHANCE_OF_FAILURE(new ChanceOfFailureCalculator()),
    MAX_RANGE(new MaxRangeCalculator()),
    TOTAL_DAMAGE(new TotalDamageCalculator()),
    IS_FACING_ENEMY(new IsFacingEnemyCalculator()),
    ENEMY_IN_RANGE(new EnemyInRangeCalculator()),
    ALLIES_NEARBY(new MyAlliesNearbyCalculator()),
    ENEMIES_NEARBY(new EnemiesNearbyCalculator()),
    UNIT_HEALTH_AVERAGE(new UnitHealthCalculator()),
    DAMAGE_RATIO(new DamageRatioCalculator()),
    ECM_COVERAGE(new EcmCoverageCalculator()),
    ENEMY_ECM_COVERAGE(new UnderEnemyEcmCoverageCalculator()),
    ENVIRONMENTAL_COVER(new EnvironmentalCoverCalculator()),
    ENVIRONMENTAL_HAZARDS(new EnvironmentalHazardsCalculator()),
    FAVORITE_TARGET_IN_RANGE(new FavoriteTargetInRangeCalculator()),
    FLANKING_POSITION(new FlankingPositionCalculator()),
    FORMATION_COHESION(new FormationCohesionCalculator()),
    FORMATION_SEPARATION(new FormationSeparationCalculator()),
    FORMATION_ALIGNMENT(new FormationAlignmentCalculator()),
    FRIENDLY_ARTILLERY_FIRE(new FriendlyArtilleryFireCalculator()),
    COVERING_UNITS(new CoveringUnitsCalculator()),
    ENEMY_VIP_DISTANCE(new EnemyVipDistanceCalculator()),
    IS_CRIPPLED(new IsCrippledCalculator()),
    MOVING_TOWARD_WAYPOINT(new MovingTowardWaypointCalculator()),
    UNIT_ROLE_BEHAVIOR(new UnitRoleBehaviorCalculator()),
    THREAT_BY_ROLE(new ThreatByRoleCalculator()),
    UNIT_TMM(new UnitTmmCalculator()),
    PILOTING_CAUTION(new PilotingCautionCalculator()),
    RETREAT(new RetreatCalculator()),
    SCOUTING(new ScoutingCalculator()),
    STANDING_STILL(new StandingStillCalculator()),
    STRATEGIC_GOAL(new StrategicGoalCalculator()),
    TARGET_HEALTH(new TargetHealthCalculator()),
    TARGET_WITHIN_OPTIMAL_RANGE(new TargetWithinOptimalRangeCalculator()),
    TURNS_TO_ENCOUNTER(new TurnsToEncounterCalculator()),
    UNIT_ROLE(new MyUnitRoleCalculator());

    private final AxisCalculator axisCalculator;

    AxisType(AxisCalculator axisCalculator) {
        this.axisCalculator = axisCalculator;
    }

    public AxisCalculator getAxisCalculator() {
        return axisCalculator;
    }

    public int axisLength() {
        return axisCalculator.axis().length;
    }

    public static int totalAxisLength() {
        int total = 0;
        for (AxisType axisType : values()) {
            total += axisType.axisLength();
        }
        return total;
    }

    public static AxisCalculator[] axisCalculators() {
        AxisCalculator[] axisCalculators = new AxisCalculator[values().length];
        for (AxisType axisType : values()) {
            axisCalculators[axisType.ordinal()] = axisType.getAxisCalculator();
        }
        return axisCalculators;
    }

}
