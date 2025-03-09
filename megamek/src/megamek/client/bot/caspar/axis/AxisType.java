package megamek.client.bot.caspar.axis;

/**
 * Enum for different types of input axes.
 */
public enum AxisType {
    ENEMY_THREAT_HEATMAP(new EnemyThreatHeatmapCalculator()),
    FRIENDLY_THREAT_HEATMAP(new FriendlyThreatHeatmapCalculator()),
    ENEMY_THREAT_NEARBY(new EnemyThreatNearbyCalculator()),
    FRIENDLY_THREAT_NEARBY(new FriendlyThreatNearbyCalculator()),
    UNIT_HEALTH(new UnitHealthCalculator()),
    POSITION_CROWDING(new PositionCrowdingCalculator()),
    DAMAGE_RATIO(new DamageRatioCalculator()),
    DECOY_POTENTIAL(new DecoyPotentialCalculator()),
    ECM_COVERAGE(new EcmCoverageCalculator()),
    ENEMY_ECM_COVERAGE(new UnderEnemyEcmCoverageCalculator()),
    ENVIRONMENTAL_COVER(new EnvironmentalCoverCalculator()),
    ENVIRONMENTAL_HAZARDS(new EnvironmentalHazardsCalculator()),
    FACING_ENEMY(new FacingEnemyCalculator()),
    FAVORITE_TARGET_IN_RANGE(new FavoriteTargetInRangeCalculator()),
    FLANKING_POSITION(new FlankingPositionCalculator()),
    FORMATION_COHESION(new FormationCohesionCalculator()),
    FORMATION_SEPARATION(new FormationSeparationCalculator()),
    FORMATION_ALIGNMENT(new FormationAlignmentCalculator()),
    FRIENDLY_ARTILLERY_FIRE(new FriendlyArtilleryFireCalculator()),
    COVERING_UNITS(new CoveringUnitsCalculator()),
    HEAT_MANAGEMENT(new HeatManagementCalculator()),
    ENEMY_VIP_DISTANCE(new EnemyVipDistanceCalculator()),
    NEARBY_ENEMY_COUNT(new NearbyEnemyCountCalculator()),
    ORIGINAL_BOT_SETTINGS(new OriginalBotSettingsCalculator()),
    IS_CRIPPLED(new IsCrippledCalculator()),
    MOVING_TOWARD_WAYPOINT(new MovingTowardWaypointCalculator()),
    UNIT_MOVEMENT(new UnitMovementCalculator()),
    UNIT_ROLE(new UnitRoleCalculator()),
    THREAT_BY_ROLE(new ThreatByRoleCalculator()),
    UNIT_TMM(new UnitTmmCalculator()),
    KILL_CHANCE(new KillChanceCalculator()),
    PILOTING_CAUTION(new PilotingCautionCalculator()),
    RETREAT(new RetreatCalculator()),
    SCOUTING(new ScoutingCalculator()),
    STANDING_STILL(new StandingStillCalculator()),
    STRATEGIC_GOAL(new StrategicGoalCalculator()),
    TARGET_HEALTH(new TargetHealthCalculator()),
    TARGET_WITHIN_OPTIMAL_RANGE(new TargetWithinOptimalRangeCalculator()),
    TURNS_TO_ENCOUNTER(new TurnsToEncounterCalculator());

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
