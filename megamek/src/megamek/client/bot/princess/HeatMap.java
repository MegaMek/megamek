/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.stream.Collectors;

import megamek.common.board.Coords;
import megamek.common.units.EjectedCrew;
import megamek.common.units.Entity;
import megamek.common.game.Game;
import megamek.common.equipment.GunEmplacement;
import megamek.common.moves.MovePath;
import megamek.common.moves.MoveStep;

/**
 * Tracks activity of units on the map. Board positions that are frequently occupied by units get higher values, while
 * those that are the least frequent get lower values. This creates a rough history of where units were concentrated
 * within the last couple of game turns which can be used for intelligent estimates about where they will be in future
 * turns. Normal operation is to call {@code ageMaps()} method once per round so previously set entries are reduced over
 * time.
 */
public class HeatMap {

    private static final int MIN_DECAY_MODIFIER = -1;
    private static final int MIN_WEIGHT = 0;
    private static final double MIN_TRACKER_TOLERANCE = 0.1;
    private static final double MAX_TRACKER_TOLERANCE = 0.9;
    private static final int MAX_TRACKER_ENTRIES = 100;
    private static final int MOVEMENT_WEIGHT_DEFAULT = 3;

    // Which team this is tracking
    private int teamId;

    // Tracking team activity
    private Map<Coords, Integer> teamActivity;
    private Map<Coords, Integer> teamMovement;
    private Map<Integer, Map<Coords, Integer>> entityMovement;
    private Map<Integer, Coords> lastPositionCache;

    // Tracking individual units
    private boolean trackIndividualUnits;

    // Control whether decay is applied or not
    private boolean enableDecay;
    private int activityDecayModifier;
    private int movementDecayModifier;

    private int movementWeight;
    private int removalWeight;

    // How many entries can be minimum value before the tracker is purged
    private double mapTrimThreshold;

    // Scaling factor applied to unit weight
    private double weightScaling;

    // Tracking friendly units does not require checking visibility or detection
    private boolean isFriendlyTeam;

    /**
     * Constructor. Initializes trackers to empty but usable states.
     */
    public HeatMap(int newTeamID) {
        initialize(newTeamID);
    }

    /**
     * Initialize backing objects
     *
     * @param newTeamId ID of team this heat map is tracking
     */
    private void initialize(int newTeamId) {
        teamId = newTeamId;
        teamActivity = new HashMap<>();
        teamMovement = new HashMap<>();

        trackIndividualUnits = false;
        entityMovement = new HashMap<>();
        lastPositionCache = new HashMap<>();

        enableDecay = true;
        movementDecayModifier = MIN_DECAY_MODIFIER;
        activityDecayModifier = MIN_DECAY_MODIFIER;

        movementWeight = MOVEMENT_WEIGHT_DEFAULT;
        removalWeight = MIN_WEIGHT;
        mapTrimThreshold = MIN_TRACKER_TOLERANCE;

        weightScaling = 1.0;

        isFriendlyTeam = false;
    }


    /**
     * Base weight for movement trackers. Large values will result in movement being retained longer. Normal range 1 to
     * 10 with 3.0 being the default.
     *
     * @param newSetting positive value, minimum 1.0
     */
    public void setMovementWeightValue(int newSetting) {
        movementWeight = (int) Math.max(newSetting, 1.0);
    }

    /**
     * Indicates if the movement tracker in this heat map will track individuals in addition to overall team
     *
     * @return true if individual unit movement is being tracked
     */
    public boolean canTrackIndividuals() {
        return trackIndividualUnits;
    }

    /**
     * Enable or disable tracking of individual unit movements
     *
     * @param newSetting true, to track individual movement of all units
     */
    public void setTrackIndividuals(boolean newSetting) {
        trackIndividualUnits = newSetting;
    }

    /**
     * Determines if decay is applied to the map when {@code ageMaps()} is called. Default setting is true.
     *
     * @return false if decay rate is disabled
     */
    public boolean isDecayEnabled() {
        return enableDecay;
    }

    /**
     * Change the setting to enable or disable decay rate on tracker weights
     *
     * @param newSetting false, to disable decay
     */
    public void changeDecayEnabled(boolean newSetting) {
        enableDecay = newSetting;
    }

    /**
     * Reduction weight for aging the activity tracker
     *
     * @return a negative number
     */
    public int getActivityDecay() {
        return activityDecayModifier;
    }

    /**
     * Set the reduction weight applied to each entry in the activity tracker. Higher absolute values will cause the
     * entries to reduce more quickly, reducing their relative importance.
     *
     * @param newSetting a negative number
     */
    public void setActivityDecay(int newSetting) {
        activityDecayModifier = Math.min(newSetting, MIN_DECAY_MODIFIER);
    }

    /**
     * Reduction weight for aging the team movement and individual movement trackers
     *
     * @return a negative number
     */
    public int getMovementDecay() {
        return movementDecayModifier;
    }

    /**
     * Set reduction weight applied to the team movement and individual movement trackers
     *
     * @param newSetting a negative number
     */
    public void setMovementDecay(int newSetting) {
        movementDecayModifier = Math.min(newSetting, MIN_DECAY_MODIFIER);
    }

    /**
     * Scaling factor applied when calculating weights for use in the trackers
     *
     * @return positive, non-zero number, typically between 0.1 and 5.0 with default of 1.0
     */
    public double getWeightScaling() {
        return weightScaling;
    }

    /**
     * Scaling factor applied when calculating weights for the trackers. 0.5 is half normal, 2.0 is twice normal, etc.
     * Higher factor mean larger weight, and bigger impact on the map. Typical values will be in the range of 0.1 to
     * 5.0, although values outside that are permitted.
     *
     * @param newSetting positive, non-zero number
     */
    public void setWeightScaling(double newSetting) {
        if (newSetting > 0) {
            weightScaling = newSetting;
        }
    }

    /**
     * The weight at which an entry is removed from the trackers when trimmed for size constraints. Normally 0 (zero)
     * but may be higher for more aggressive trimming.
     */
    public int getRemovalWeight() {
        return removalWeight;
    }

    /**
     * The weight at which an entry is removed from the trackers when trimmed for size constraints. Must be 0 (zero) or
     * positive number.
     *
     * @param newSetting positive number, may be 0 (zero)
     */
    public void setRemovalWeight(int newSetting) {
        removalWeight = Math.max(newSetting, 0);
    }

    /**
     * Percentage of low-value tracking map entries allowed before they are removed
     *
     * @return number between {@code MIN_TRACKER_TOLERANCE} and 0.9
     */
    public double getMapTrimThreshold() {
        return mapTrimThreshold;
    }

    /**
     * When the percentage of minimum values in any tracker exceeds this value, any entries at or below
     * {@code mapTrimThreshold} are removed to maintain efficiency. Typical values are between 0.1 and 0.5.
     *
     * @param newSetting positive value between {@code MIN_TRACKER_TOLERANCE} and {@code MAX_TRACKER_TOLERANCE}
     */
    public void setMapTrimThreshold(double newSetting) {
        mapTrimThreshold = Math.min(Math.max(newSetting, MIN_TRACKER_TOLERANCE), MAX_TRACKER_TOLERANCE);
    }

    /**
     * Identifies if this is tracking a friendly team, so visibility and detection status don't apply
     */
    public boolean getIsTrackingFriendlyTeam() {
        return isFriendlyTeam;
    }

    /**
     * Tracking friendly entities doesn't require checking visibility or detection status
     *
     * @param newSetting true, if this is tracking entities on the same team
     */
    public void setIsTrackingFriendlyTeam(boolean newSetting) {
        isFriendlyTeam = newSetting;
    }


    /**
     * Get all hot-spots (positions of high activity) in descending order
     *
     * @return list of positions, or null if team activity tracker is empty
     */
    public List<Coords> getHotSpots() {

        // If there are no hot-spots, return null
        if (teamActivity.isEmpty() || teamActivity.values().stream().allMatch(w -> w == MIN_WEIGHT)) {
            return null;
        }

        // Sort the weighted positions by descending hot-spot value
        List<Coords> rankedPositions = new ArrayList<>();
        List<Coords> workingPositions = teamActivity.
              keySet().
              stream().
              filter(p -> teamActivity.get(p) > MIN_WEIGHT).
              collect(Collectors.toList());
        List<Integer> workingWeights = workingPositions.
              stream().
              map(this::getHotSpotRating).
              collect(Collectors.
                    toList());
        while (!workingPositions.isEmpty()) {
            OptionalInt maxRating = workingWeights.stream().mapToInt(w -> w).max();

            for (int i = 0; i < workingPositions.size(); i++) {
                if (workingWeights.get(i) == maxRating.getAsInt()) {
                    rankedPositions.add(workingPositions.get(i));
                }
            }

            workingPositions.removeIf(rankedPositions::contains);
            workingWeights.removeIf(w -> w == maxRating.getAsInt());
        }

        return rankedPositions;
    }

    /**
     * Get the closest hot-spot (position of high activity). May return null if the activity tracker is empty.
     *
     * @return {@link Coords} with nearest hot-spot, or null if no valid position
     */
    public Coords getHotSpot(Coords testPosition, boolean topOnly) {

        // If there are no hot-spots, return null
        if (teamActivity.isEmpty() || teamActivity.values().stream().allMatch(w -> w == MIN_WEIGHT)) {
            return null;
        }

        // Sort the weighted positions by descending hot-spot value
        List<Coords> rankedPositions = new ArrayList<>();
        List<Coords> workingPositions = teamActivity.
              keySet().
              stream().
              filter(p -> teamActivity.get(p) > MIN_WEIGHT).
              collect(Collectors.toList());
        List<Integer> workingWeights = workingPositions.
              stream().
              map(this::getHotSpotRating).
              collect(Collectors.toList());

        while (!workingPositions.isEmpty()) {
            OptionalInt maxRating = workingWeights.stream().mapToInt(w -> w).max();

            for (int i = 0; i < workingPositions.size(); i++) {
                if (workingWeights.get(i) == maxRating.getAsInt()) {
                    rankedPositions.add(workingPositions.get(i));
                }
            }

            if (!topOnly) {
                workingPositions.removeIf(rankedPositions::contains);
                workingWeights.removeIf(w -> w == maxRating.getAsInt());
            } else {
                workingPositions.clear();
            }
        }

        // Get the hot-spot closest to the provided position, with the highest value
        int shortestRange = Integer.MAX_VALUE;
        Coords bestPosition = null;
        for (Coords curPosition : rankedPositions) {
            if (shortestRange > curPosition.distance(testPosition)) {
                bestPosition = curPosition;
                shortestRange = curPosition.direction(testPosition);
            }
        }

        return bestPosition;
    }

    /**
     * Get the hotspot (position with high activity) with the highest rating. If multiple hot-spots of equal value are
     * present, any one of them may be returned.
     *
     * @return map position, may return null
     */
    public Coords getHotSpot() {
        List<Coords> rankedPositions = getHotSpots();
        if (rankedPositions != null && !rankedPositions.isEmpty()) {
            return rankedPositions.get(0);
        } else {
            return null;
        }
    }

    /**
     * Adjusts the trackers using the current position of each provided entity that matches the team this heat map is
     * tracking. Filters out gun emplacements and ejected MekWarriors/vehicle crews, as well as functional units with a
     * dead pilot/crew.
     *
     * @param tracked list of entities to process
     */
    public void updateTrackers(List<Entity> tracked) {

        // Filter out the entities that are on the team for this tracker and are considered 'active'
        // i.e. crew is alive, unit is deployed, can be seen, and is not a concealed unit
        for (Entity curTracked : tracked.
              stream().
              filter(e -> e.getOwner().getTeam() == teamId && isTrackable(e)).
              toList()) {

            // Immobile - once you know where it is, it's (hopefully!) not moving ... or
            // Non-combat - not worth tracking
            if (curTracked.isBuildingEntityOrGunEmplacement() || curTracked instanceof EjectedCrew) {
                continue;
            }

            // Everything rides on valid coordinates, so double check
            if (curTracked.getPosition() == null) {
                continue;
            }

            // Update the team, team movement, and entity movement trackers
            processEntity(curTracked);
        }
    }

    /**
     * Updates the trackers using a specific movement path. Filters out gun emplacements and ejected MekWarriors/vehicle
     * crews. Because this information is only available for entities under direct Princess control, this is normally
     * limited to tracking friendly entities.
     *
     * @param detailedMove {@link MovePath} object, which includes an entity reference and Coords for the positions
     *                     moved through
     */
    public void updateTrackers(MovePath detailedMove) {
        if (detailedMove == null || detailedMove.getCoordsSet() == null || detailedMove.length() == 0) {
            return;
        }

        Entity tracked = detailedMove.getEntity();

        // Immobile - once you know where it is, it's (hopefully!) not moving ... or
        // Non-combat - not worth tracking
        if (tracked.isBuildingEntityOrGunEmplacement() || tracked instanceof EjectedCrew) {
            return;
        }

        // Get the map adjustments, based on entity position
        int mapAdjustment = getTeamWeightAdjustment(detailedMove.getEntity());

        List<Coords> path = new ArrayList<>();

        Coords startPosition = null;
        Coords lastPosition = detailedMove.getLastStep().getPosition();

        for (MoveStep curStep : detailedMove.getStepVector()) {
            Coords curPosition = curStep.getPosition();
            if (startPosition != null) {
                if (!path.contains(curPosition) && !curPosition.equals(startPosition)) {
                    path.add(curPosition);
                }
            } else {
                startPosition = curPosition;
            }
        }

        // Update the team map, adding the position if not already present
        updateTeamActivityMap(lastPosition, mapAdjustment);

        // Only process the movement trackers if the entity has moved from it's last known position
        if (lastPosition != null && !path.isEmpty()) {
            mapAdjustment = getMovementWeightAdjustment(tracked);
            updateTeamMovementMap(path, mapAdjustment);

            // If individual entity tracking is enabled, adjust the entity map
            if (trackIndividualUnits) {
                updateEntityMap(tracked.getId(), path, mapAdjustment);
            }
        }

        // Stash this position for movement interpolation in the next round
        updateLastKnown(tracked.getId(), lastPosition);
    }

    /**
     * Reduces the values for every entry in the trackers. This will gradually reduce the weights over time for
     * positions that are not regularly updated. If enough entries are below the set threshold, all entries which are at
     * or below that threshold will be removed.
     */
    public void ageMaps(Game game) {

        // Bring out your dead!
        trimLastKnownPositions(game);

        if (enableDecay) {
            ageTeamActivityMap(game);
            ageTeamMovementMap();
            ageEntityMap();
        }

        // If the tracking maps are hitting the set limits, trim the entries back
        trimMap(teamActivity);
        trimMap(teamMovement);
        for (Map<Coords, Integer> entityMap : entityMovement.values()) {
            trimMap(entityMap);
        }

    }

    /**
     * Manually update the last known positions of tracked entities
     *
     * @param game current game
     */
    public void refreshLastKnownCache(Game game) {

        // Don't bother updating destroyed entities
        trimLastKnownPositions(game);

        for (int curId : lastPositionCache.keySet()) {
            Entity curEntity = game.getEntity(curId);
            if (curEntity != null && isTrackable(curEntity)) {
                lastPositionCache.put(curId, curEntity.getPosition());
            }
        }

    }


    /**
     * Apply the entity position to the activity and movement trackers
     *
     * @param tracked entity to process
     */
    private void processEntity(Entity tracked) {
        Coords position = new Coords(tracked.getPosition().getX(), tracked.getPosition().getY());

        // Get the map adjustments, based on entity BV
        int mapAdjustment = getTeamWeightAdjustment(tracked);

        // Update the team activity tracker, adding the position if not already present
        updateTeamActivityMap(position, mapAdjustment);

        // Only update the movement trackers if the entity has moved from it's last known position
        if (!position.equals(lastPositionCache.get(tracked.getId()))) {

            // Interpolate movement from last known position
            mapAdjustment = getMovementWeightAdjustment(tracked);
            List<Coords> path = new ArrayList<>();
            Coords startPosition = lastPositionCache.get(tracked.getId());
            if (lastPositionCache.containsKey(tracked.getId())) {
                path = Coords.intervening(startPosition, position);
                // Remove the starting point, as it was already counted in the last round
                path.remove(startPosition);
            } else {
                path.add(position);
            }

            updateTeamMovementMap(path, mapAdjustment);

            // If individual entity tracking is enabled, adjust the entity tracker
            if (trackIndividualUnits) {
                updateEntityMap(tracked.getId(), path, mapAdjustment);
            }

            // Stash this position for movement interpolation in the next round
            updateLastKnown(tracked.getId(), position);
        }
    }

    /**
     * Update the team activity tracker for a given position
     *
     * @param position   position to update
     * @param adjustment modifier to existing value
     */
    private void updateTeamActivityMap(Coords position, int adjustment) {
        if (position != null) {
            int mapValue = teamActivity.getOrDefault(position, 0) + adjustment;
            teamActivity.put(position, mapValue);
        }
    }

    /**
     * Get the weight of the entity for adjusting the team activity tracker. Higher BV units get a larger weight, while
     * faster units have a reduced weight.
     *
     * @param tracked entity to calculate weight for
     *
     * @return weight to modify tracker with
     */
    private int getTeamWeightAdjustment(Entity tracked) {

        int bvWeight = tracked.getBvCalculator().retrieveBV();
        if (bvWeight == -1) {
            bvWeight = tracked.getInitialBV();
        }

        bvWeight = Math.max(bvWeight - (100 * Math.max(tracked.getAnyTypeMaxJumpMP(), tracked.getWalkMP())), 1);
        bvWeight = (int) Math.floor(weightScaling * bvWeight);

        return bvWeight;
    }

    /**
     * Apply decay rate to team activity map. Positions that have a tracked entity use a slower, linear decay rate,
     * while those that do not have a tracked entity use a faster exponential decay rate
     *
     * @param game current game
     */
    private void ageTeamActivityMap(Game game) {

        // Get positions of tracked entities with known positions
        List<Coords> activePositions = new ArrayList<>();
        for (int curId : lastPositionCache.keySet()) {
            Entity curEntity = game.getEntity(curId);
            if (curEntity != null && isTrackable(curEntity)) {
                activePositions.add(lastPositionCache.get(curId));
            }
        }

        int curWeight;

        for (Coords curPosition : teamActivity.keySet()) {
            curWeight = teamActivity.get(curPosition);
            if (curWeight <= MIN_WEIGHT) {
                continue;
            }
            // If this position does not have a tracked entity, use exponential decay.
            // Otherwise, use linear decay.
            if (!activePositions.contains(curPosition)) {
                curWeight = Math.max((int) Math.floor(curWeight / 2.0), MIN_WEIGHT);
            } else {
                curWeight = Math.max(curWeight + activityDecayModifier, MIN_WEIGHT);
            }
            teamActivity.put(curPosition, curWeight);
        }

    }

    /**
     * Update the team movement tracker for a given path
     *
     * @param path       positions an entity has moved through
     * @param adjustment weight to modify tracker with
     */
    private void updateTeamMovementMap(List<Coords> path, int adjustment) {
        int mapValue = adjustment;
        for (Coords curPosition : path) {
            if (teamMovement.containsKey(curPosition)) {
                mapValue = teamMovement.get(curPosition) + adjustment;
            }
            teamMovement.put(curPosition, mapValue);
        }
    }

    /**
     * Get the weight for tracking movement. For now, this is a simple number chosen so that the entity's movement can
     * be evaluated for the past several rounds.
     *
     * @param tracked entity to calculate weight for
     *
     * @return weight to modify tracker with
     */
    private int getMovementWeightAdjustment(Entity tracked) {
        return (int) Math.floor(movementWeight * weightScaling);
    }

    /**
     * Apply decay rate to team movement map
     */
    private void ageTeamMovementMap() {
        int curWeight;

        for (Coords curPosition : teamMovement.keySet()) {
            curWeight = teamMovement.get(curPosition);
            if (curWeight <= MIN_WEIGHT) {
                continue;
            }
            curWeight = Math.max(curWeight + movementDecayModifier, MIN_WEIGHT);
            teamMovement.put(curPosition, curWeight);
        }

    }

    /**
     * Update the entity movement tracker
     *
     * @param trackedId  game ID of entity
     * @param positions  positions entity has moved through
     * @param adjustment weight to modify tracker with
     */
    private void updateEntityMap(int trackedId, List<Coords> positions, int adjustment) {
        Map<Coords, Integer> positionMap;

        if (entityMovement.containsKey(trackedId)) {
            positionMap = entityMovement.get(trackedId);
            int mapValue = adjustment;
            for (Coords curPosition : positions) {
                if (positionMap.containsKey(curPosition)) {
                    mapValue = positionMap.get(curPosition) + adjustment;
                }
                positionMap.put(curPosition, mapValue);
            }
        } else {
            positionMap = new HashMap<>();
            for (Coords curPosition : positions) {
                positionMap.put(curPosition, adjustment);
            }
            entityMovement.put(trackedId, positionMap);
        }
    }

    /**
     * Apply decay rate to the individual movement maps
     */
    private void ageEntityMap() {
        int curWeight;

        for (int curID : entityMovement.keySet()) {
            Map<Coords, Integer> curMap = entityMovement.get(curID);
            for (Coords curPosition : curMap.keySet()) {
                curWeight = curMap.get(curPosition);
                if (curWeight <= MIN_WEIGHT) {
                    continue;
                }
                curWeight = Math.max(curWeight + movementDecayModifier, MIN_WEIGHT);
                curMap.put(curPosition, curWeight);
            }
        }

    }

    /**
     * Determines if a particular entity is valid for tracking i.e. it is a ground unit, deployed, on map, not hidden,
     * and either considered a friendly unit or an enemy unit that is detected visually or through sensors. Ejected
     * MekWarriors and vehicle crews, gun emplacements, and entities with dead crews ('carcass') are rejected.
     *
     * @param testEntity entity to check
     *
     * @return true, if entity is valid for tracking
     */
    public boolean isTrackable(Entity testEntity) {
        return (testEntity != null) &&
              !testEntity.isDestroyed() &&
              testEntity.isGround() &&
              testEntity.isDeployed() &&
              !testEntity.isOffBoard() &&
              !testEntity.isCarcass() &&
              !testEntity.isHidden() &&
              (isFriendlyTeam || testEntity.isVisibleToEnemy() || testEntity.isDetectedByEnemy()) &&
              !(testEntity instanceof EjectedCrew) &&
              !(testEntity.isBuildingEntityOrGunEmplacement());
    }

    /**
     * Convenience method for external callers to pre-filter entities prior to passing them for processing into a heat
     * map
     *
     * @param testEntity entity to check
     *
     * @return true, if entity is valid for testing
     */
    public static boolean validateForTracking(Entity testEntity) {
        return (testEntity != null) &&
              !testEntity.isDestroyed() &&
              testEntity.isGround() &&
              testEntity.isDeployed() &&
              !testEntity.isOffBoard() &&
              !testEntity.isCarcass() &&
              !testEntity.isHidden() &&
              !(testEntity instanceof EjectedCrew) &&
              !(testEntity.isBuildingEntityOrGunEmplacement());
    }

    /**
     * Keep track of the last known position of each entity for interpolating movement
     *
     * @param trackedId game ID of entity to update
     * @param position  new position
     */
    private void updateLastKnown(int trackedId, Coords position) {
        if (!lastPositionCache.containsKey(trackedId) || !lastPositionCache.get(trackedId).equals(position)) {
            lastPositionCache.put(trackedId, position);
        }
    }

    /**
     * Remove destroyed entities from the last known position tracker
     *
     * @param game current game
     */
    private void trimLastKnownPositions(Game game) {
        for (Entity curCorpse : game.getOutOfGameEntitiesVector()) {
            if (curCorpse != null) {
                lastPositionCache.remove(curCorpse.getId());
            }
        }
    }

    /**
     * If the percentage of entries in the given tracker that are low enough for removal is higher than the provided
     * threshold, this will remove them. If the number of entries in the tracker exceed the maximum number then the
     * lowest values
     *
     * @param checkMap tracker to update
     */
    private void trimMap(Map<Coords, Integer> checkMap) {
        if ((double) checkMap.values().stream().filter(w -> w <= removalWeight).count() / checkMap.size()
              >= mapTrimThreshold) {
            checkMap.entrySet().removeIf(curPos -> curPos.getValue() <= removalWeight);
        }

        // Last ditch measures - trim out the lowest weights until the number of entries is
        // within reason
        while (checkMap.size() > MAX_TRACKER_ENTRIES) {
            OptionalInt lowestRank = checkMap.values().stream().mapToInt(w -> w).min();
            List<Coords> removals = checkMap.keySet()
                  .stream()
                  .filter(p -> checkMap.get(p) == lowestRank.getAsInt())
                  .toList();
            for (Coords curPosition : removals) {
                checkMap.remove(curPosition);
            }
        }

    }

    /**
     * Get the highest rated positions in a tracker, given a base point and limited distance
     *
     * @param trackingMap tracker to process
     * @param maxRange    how far from provided position to consider
     * @param basePoint   base point
     *
     * @return list of positions, sorted from highest weight to lowest
     */
    private Collection<Coords> getTopRatedPositions(Map<Coords, Integer> trackingMap, int maxRange, Coords basePoint) {
        Collection<Coords> topSet = new HashSet<>();
        List<Coords> mapCoords = new ArrayList<>();
        List<Integer> mapWeights = new ArrayList<>();

        splitAndSort(trackingMap, mapCoords, mapWeights);

        int maxWeight = mapWeights.get(0);
        for (int i = 0; i < mapCoords.size() && mapWeights.get(i) == maxWeight; i++) {
            if (basePoint == null || basePoint.distance(mapCoords.get(i)) <= maxRange) {
                topSet.add(mapCoords.get(i));
            }
        }

        return topSet;
    }

    /**
     * Split the provided tracker into two lists, sorted from the highest weight to lowest
     *
     * @param trackingMap tracker to process
     * @param positions   list of {@link Coords} to populate
     * @param weights     list of tracker weights to populate
     *
     * @throws IllegalArgumentException if uninitialized lists are provided
     */
    private void splitAndSort(Map<Coords, Integer> trackingMap, List<Coords> positions, List<Integer> weights) {
        if (positions == null || weights == null) {
            throw new IllegalArgumentException("Expecting initialized List<> object(s).");
        }

        Map<Coords, Integer> workingMap = new HashMap<>(trackingMap);

        while (!workingMap.isEmpty()) {
            int maxWeight = workingMap.values().stream().mapToInt(w -> w).max().getAsInt();

            for (Coords curPosition : workingMap.keySet()) {
                if (workingMap.get(curPosition) == maxWeight) {
                    positions.add(curPosition);
                    weights.add(maxWeight);
                }
            }

            workingMap.entrySet().removeIf(curPosition -> curPosition.getValue() == maxWeight);
        }

    }

    /**
     * Calculate a value for a position using both the weights in the activity tracker and how far they are from that
     * position
     *
     * @param testPosition position to calculate for, may be in the activity tracker or not
     *
     * @return integer value representing how 'hot' the provided position is
     */
    private int getHotSpotRating(Coords testPosition) {

        // When there is only one position in the activity tracker, the result is always that value
        if (teamActivity.size() == 1) {
            return teamActivity.values().stream().findFirst().get();
        }

        // Get the farthest position for scaling purposes
        int longestDistance = teamActivity.keySet().stream().mapToInt(testPosition::distance).max().getAsInt();

        // Sum the distances of each usable weight weighted by the distance from the provided
        // position
        double runningTotal = teamActivity.
              keySet().
              stream().
              mapToDouble(curPosition -> teamActivity.get(curPosition) *
                    (double) (longestDistance - testPosition.distance(curPosition)) / longestDistance).
              sum();

        return (int) Math.floor(runningTotal);
    }

}
