/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */
package megamek.ai.utility;

import megamek.client.bot.princess.BehaviorSettings;
import megamek.client.bot.princess.CardinalEdge;
import megamek.client.bot.princess.UnitBehavior;
import megamek.common.*;
import megamek.common.annotations.Nullable;

import java.util.*;

/**
 * Abstract class providing context for decision-making in AI.
 * It includes information about the world, current unit, targets, and provides extension points
 * for adding custom functionalities through interfaces.
 *
 * @author Luana Coppio
 */
public abstract class DecisionContext {
    protected final static int DAMAGE_CACHE_SIZE = 10_000;

    private final World world;
    private final MovePath movePath;
    private final Coords waypoint;
    private final StrategicGoalsManager strategicGoalsManager;
    private final BehaviorSettings behaviorSettings;
    private final Map<String, Double> damageCache;

    private ThreatAssessment threatAssessment;
    private UnitInformationProvider unitInformationProvider;
    private DamageCalculator damageCalculator;

    public DecisionContext(
        World world,
        MovePath movePath,
        @Nullable Coords waypoint,
        StrategicGoalsManager strategicGoalsManager,
        @Nullable ThreatAssessment threatAssessment,
        @Nullable UnitInformationProvider unitInformationProvider,
        @Nullable DamageCalculator damageCalculator,
        BehaviorSettings behaviorSettings,
        Map<String, Double> damageCache) {
        this.world = world;
        this.movePath = movePath.clone();
        this.waypoint = waypoint;
        this.threatAssessment = threatAssessment;
        this.strategicGoalsManager = strategicGoalsManager;
        this.unitInformationProvider = unitInformationProvider;
        this.damageCalculator = damageCalculator;
        this.behaviorSettings = behaviorSettings;
        this.damageCache = Objects.requireNonNullElseGet(damageCache, () -> new LinkedHashMap<>(256, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Double> eldest) {
                return size() > DAMAGE_CACHE_SIZE;
            }
        });
    }

    public BehaviorSettings getBehaviorSettings() {
        return behaviorSettings;
    }

    public void setThreatAssessment(ThreatAssessment threatAssessment) {
        this.threatAssessment = threatAssessment;
    }

    public void setUnitInformationProvider(UnitInformationProvider unitInformationProvider) {
        this.unitInformationProvider = unitInformationProvider;
    }

    public void setDamageCalculator(DamageCalculator damageCalculator) {
        this.damageCalculator = damageCalculator;
    }

    /**
     * Gets the game world.
     *
     * @return The game world.
     */
    public World getWorld() {
        return world;
    }

    /**
     * Gets the current unit making the decision.
     *
     * @return The current unit.
     */
    public Entity getCurrentUnit() {
        return getUnitInformationProvider().getCurrentUnit();
    }

    /**
     * Gets the heatmap of the game world.
     *
     * @return The heatmap as an array of doubles.
     */
    public double[] getHeatmap() {
        return world.getHeatmap();
    }

    public int getTotalHealth() {
        return getUnitInformationProvider().getTotalHealth();
    }

    /**
     * Clears the damage cache.
     */
    public void clearCaches() {
        damageCache.clear();
    }

    /**
     * Abstract method to get the bonus factor for the decision.
     *
     * @return The bonus factor.
     */
    public abstract double getBonusFactor();

    /**
     * Calculates the maximum damage a unit can inflict at a given range.
     *
     * @param unit The unit for which to calculate the damage.
     * @param enemyRange The range at which the damage is calculated.
     * @return The maximum damage the unit can inflict at the given range.
     */
    public double calculateUnitMaxDamageAtRange(Targetable unit, int enemyRange) {
        return getThreatAssessment().calculateUnitMaxDamageAtRange(unit, enemyRange);
    }

    public double maxAmountOfDamageFromFriendsInRange(Coords targetCoords, int range) {
        return getThreatAssessment().maxAmountOfDamageFromFriendsInRange(targetCoords, range);
    }

    public int getNumberOfFriendsInRange(Coords coords, int range) {
        return getThreatAssessment().getNumberOfFriendsInRange(coords, range);
    }

    public int getNumberOfEnemiesInRange(Coords coords, int range) {
        return getThreatAssessment().getNumberOfEnemiesInRange(coords, range);
    }

    public List<Integer> getFriendIDsAtRange(Coords coords, int range) {
        return getThreatAssessment().getFriendIDsAtRange(coords, range);
    }

    public List<Integer> getEnemyIDsAtRange(Coords coords, int range) {
        return getThreatAssessment().getEnemyIDsAtRange(coords, range);
    }

    public List<Coords> getNClosestEnemiesPositions(Coords coords, int numberOfEnemies) {
        return getThreatAssessment().getNClosestEnemiesPositions(coords, numberOfEnemies);
    }

    public OptionalInt getDistanceToClosestEnemyAtFinalMovePathPosition() {
        return getThreatAssessment().getDistanceToClosestEnemyAtFinalMovePathPosition(getFinalPosition());
    }

    public OptionalInt getDistanceToClosestEnemyWithRole(UnitRole role) {
        return getThreatAssessment().getDistanceToClosestEnemyWithRole(getFinalPosition(), role);
    }

    public List<Coords> getThreateningEnemies(Coords coords) {
        return getThreatAssessment().getThreateningEnemies(coords);
    }

    public StructOfUnitArrays getStructOfEnemiesArrays() {
        return getThreatAssessment().getStructOfEnemiesArrays();
    }

    public StructOfUnitArrays getStructOfAlliesArrays() {
        return getThreatAssessment().getStructOfAlliesArrays();
    }

    public StructOfUnitArrays getStructOfOwnUnitsArrays() {
        return getThreatAssessment().getStructOfOwnUnitsArrays();
    }

    public Map<Coords, Double> getIncomingFriendlyArtilleryDamage() {
        return getThreatAssessment().getIncomingFriendlyArtilleryDamage();
    }

    public double getFiringDamage() {
        return getDamageCalculator().firingDamage();
    }

    /**
     * Gets the physical damage.
     *
     * @return The physical damage.
     */
    public double getPhysicalDamage() {
        return getDamageCalculator().physicalDamage();
    }

    /**
     * Gets the expected damage.
     *
     * @return The expected damage.
     */
    public double getExpectedDamage() {
        return getDamageCalculator().expectedDamage();
    }

    public double getEnemyThreat(Coords position) {
        return getThreatAssessment().getEnemyThreat(position);
    }

    public double getTotalDamage() {
        return getPhysicalDamage() + getFiringDamage();
    }

    /**
     * Gets the set of coordinates in the move path.
     *
     * @return The set of coordinates.
     */
    public Set<Coords> getCoordsSet() {
        return getUnitInformationProvider().getCoordsSet();
    }

    /**
     * Gets the final coordinates of the move path.
     *
     * @return The final coordinates.
     */
    public Coords getFinalPosition() {
        return getUnitInformationProvider().getFinalPosition();
    }


    /**
     * Gets the distance moved.
     *
     * @return The distance moved.
     */
    public int getDistanceMoved() {
        return getUnitInformationProvider().getDistanceMoved();
    }

    /**
     * Gets the hexes moved.
     *
     * @return The hexes moved.
     */
    public int getHexesMoved() {
        return getUnitInformationProvider().getHexesMoved();
    }

    /**
     * Gets the final facing of the move path.
     *
     * @return The final facing.
     */
    public int getFinalFacing() {
        return getUnitInformationProvider().getFinalFacing();
    }

    /**
     * Gets a list of friendly units within a specified range of a position.
     *
     * @param position The coordinates to check.
     * @param range The range within which to find friendly units.
     * @return A list of friendly units within the specified range.
     */
    public List<Targetable> getEnemiesWithinRange(Coords position, int range) {
        return getThreatAssessment().getEnemiesWithinRange(position, range);
    }

    /**
     * Gets a list of enemy units within a specified range of a position.
     *
     * @param position The coordinates to check.
     * @return A list of enemy units within the specified range.
     */
    public List<Targetable> getFriendliesWithinRange(Coords position, int range) {
        return getThreatAssessment().getFriendliesWithinRange(position, range);
    }

    /**
     * Gets the closest VIP unit.
     *
     * @return The closest VIP unit, returns empty if there is none.
     */
    public Optional<Targetable> getClosestVIP() {
        return getThreatAssessment().getClosestVIP(getFinalPosition());
    }

    public Optional<Targetable> getClosestEnemy() {
        return getThreatAssessment().getClosestEnemy(getFinalPosition());
    }

    public int getCurrentUnitMaxRunMP() {
        return getUnitInformationProvider().getMaxRunMP();
    }

    public int getMaxWeaponRange() {
        return getUnitInformationProvider().getMaxWeaponRange();
    }

    /**
     * Gets if the unit is jumping ot not
     * @return true if the unit is jumping, false otherwise
     */
    public boolean isJumping() {
        return getUnitInformationProvider().isJumping();
    }

    public int getFinalAltitude() {
        return getUnitInformationProvider().getFinalAltitude();
    }

    public double getMovePathSuccessProbability() {
        return getUnitInformationProvider().getMovePathSuccessProbability();
    }

    public UnitBehavior.BehaviorType getBehaviorType() {
        return getUnitInformationProvider().getBehaviorType();
    }

    public int getDistanceToHome() {
        return getDistanceToEdge(getBehaviorSettings().getRetreatEdge(), getFinalPosition());
    }

    public int getDistanceToDestination() {
        return getDistanceToDestination(getBehaviorSettings().getDestinationEdge(), getFinalPosition(), getWaypoint());
    }

    /**
     * Returns the distance delta to the destination.
     * It will return {@code OptionalInt.empty()} if the unit has no destination at all, it will return positive if this movement path
     * gets it closer to the destination, and a negative number if it gets it away from the destination
     */
    public OptionalInt getDistanceDeltaToDestination(Coords finalPosition, Coords startingPosition, Coords waypoint) {
        if (finalPosition == null) {
            return OptionalInt.empty();
        }
        CardinalEdge destinationEdge = getBehaviorSettings().getDestinationEdge();
        CardinalEdge retreatEdge = getBehaviorSettings().getRetreatEdge();

        if (getBehaviorType() == UnitBehavior.BehaviorType.MoveToDestination) {
            if (waypoint != null) {
                return OptionalInt.of(startingPosition.distance(waypoint) - finalPosition.distance(waypoint));
            } else {
                var distToEdge = getDistanceToEdge(destinationEdge, startingPosition);
                var finalDistToEdge = getDistanceToEdge(destinationEdge, finalPosition);
                return OptionalInt.of(distToEdge - finalDistToEdge);
            }
        } else if (getBehaviorType() == UnitBehavior.BehaviorType.ForcedWithdrawal) {
            var distToEdge = getDistanceToEdge(retreatEdge, startingPosition);
            var finalDistToEdge = getDistanceToEdge(retreatEdge, finalPosition);
            return OptionalInt.of(distToEdge - finalDistToEdge);
        }

        return OptionalInt.empty();
    }

    /**
     * Gets the distance to home.
     * @return The distance to home.
     */
    private int getDistanceToEdge(CardinalEdge edge, Coords finalPosition) {
        int boardHeight = getQuickBoardRepresentation().getHeight();
        int boardWidth = getQuickBoardRepresentation().getWidth();
        return switch (edge) {
            case NORTH -> finalPosition.getY();
            case SOUTH -> boardHeight - finalPosition.getY() - 1;
            case EAST -> boardWidth - finalPosition.getX() - 1;
            case WEST -> finalPosition.getX();
            case NONE -> -1;
            case NEAREST -> {
                int north = finalPosition.getY();
                int south = boardHeight - finalPosition.getY() - 1;
                int east = boardWidth - finalPosition.getX() - 1;
                int west = finalPosition.getX();
                yield Math.min(Math.min(north, south), Math.min(east, west));
            }
        };
    }

    private int getDistanceToDestination(CardinalEdge edge, Coords finalPosition, Coords waypoint) {
        if (getBehaviorType() == UnitBehavior.BehaviorType.MoveToDestination) {
            return Optional.ofNullable(waypoint).map(finalPosition::distance)
                .orElseGet(() -> getDistanceToEdge(edge, finalPosition));
        } else if (getBehaviorType() == UnitBehavior.BehaviorType.ForcedWithdrawal) {
            return getDistanceToEdge(edge, finalPosition);
        }
        return -1;
    }


    /**
     * Gets the strategic goals on the coordinates quadrant.
     * @param finalPosition The final position.
     * @return The strategic goals on the coordinates quadrant.
     */
    public List<Coords> getStrategicGoalsOnCoordsQuadrant(Coords finalPosition) {
        return getStrategicGoalsManager().getStrategicGoalsOnCoordsQuadrant(finalPosition);
    }

    public StrategicGoalsManager getStrategicGoalsManager() {
        return strategicGoalsManager;
    }

    /**
     * Gets the QuickBoardRepresentation from the world
     * @return QuickBoardRepresentation
     */
    public QuickBoardRepresentation getQuickBoardRepresentation() {
        return world.getQuickBoardRepresentation();
    }

    /**
     * Gets the threat assessment implementation.
     *
     * @return An Optional containing the threat assessment implementation, or empty if not set.
     */
    protected ThreatAssessment getThreatAssessment() {
        return threatAssessment;
    }

    /**
     * Gets the unit information provider implementation.
     *
     * @return An Optional containing the unit information provider implementation, or empty if not set.
     */
    protected UnitInformationProvider getUnitInformationProvider() {
        return unitInformationProvider;
    }

    /**
     * Gets the damage calculator implementation.
     *
     * @return An Optional containing the damage calculator implementation, or empty if not set.
     */
    protected DamageCalculator getDamageCalculator() {
        return damageCalculator;
    }

    /**
     * Gets the move path of the unit
     * @return MovePath
     */
    public MovePath getMovePath() {
        return movePath;
    }

    public Coords getStartingPosition() {
        return getUnitInformationProvider().getStartingPosition();
    }

    public int getHeatCapacity() {
        return getUnitInformationProvider().getHeatCapacity();
    }

    public Coords getWaypoint() {
        return waypoint;
    }

    public double getArmorRemainingPercent(Targetable target) {
        return getThreatAssessment().getArmorRemainingPercent(target);
    }

    public Coords getMyClusterCentroid(Targetable self) {
        return getUnitInformationProvider().getEntityClusterCentroid(self);
    }
}
