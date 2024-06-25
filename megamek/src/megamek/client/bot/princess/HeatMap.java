package megamek.client.bot.princess;

import megamek.common.Coords;
import megamek.common.EjectedCrew;
import megamek.common.Entity;
import megamek.common.GunEmplacement;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Tracks activity of visible enemy units on the map. Board positions that are frequently occupied
 * by enemy units get pushed to the top while those that are the least frequent get removed. This
 * supports making rough guesses about where enemy units may be present, both in general and for
 * specific units.
 */
public class HeatMap {

    private static final int MIN_AGE_MODIFIER = -1;
    private static final int MIN_WEIGHT = 0;
    private static final int MAX_WEIGHT = 100;
    private static final int MAX_MP_WEIGHT = 10;
    private static final int MIN_TRACKER_POSITIONS = 10;

    // Which team this is tracking
    private int teamId;

    // Tracking team activity
    private Map<Coords, Integer> teamActivity;

    // Tracking individual units
    private boolean trackIndividualUnits;
    private Map<Integer, Map<Coords, Integer>> entityActivity;

    private int ageModifier;
    private int removalWeight;
    private int maxPositions;

    /**
     * Constructor. Initializes all backing objects to an empty state.
     */
    public HeatMap (int newTeamID) {
        initialize(newTeamID);
    }

    /**
     * Initialize backing objects
     * @param newTeamId
     */
    private void initialize (int newTeamId) {
        teamId = newTeamId;
        teamActivity = new HashMap<>();

        trackIndividualUnits = false;
        entityActivity = new HashMap<>();

        ageModifier = MIN_AGE_MODIFIER;
        removalWeight = MIN_WEIGHT;
        maxPositions = MIN_TRACKER_POSITIONS;
    }


    /**
     * Indicates if this heat map will track individual entities in addition to team positions.
     * @return
     */
    public boolean canTrackIndividuals () {
        return trackIndividualUnits;
    }

    /**
     * Enable or disable tracking of individual units in addition to
     * @param newSetting
     */
    public void enableTrackIndividuals (boolean newSetting) {
        trackIndividualUnits = newSetting;
    }

    /**
     * Reduction weight for each map entry on each game turn
     * @return  a negative number
     */
    public int getAgeModifier () {
        return ageModifier;
    }

    /**
     * Set reduction weight for each map entry on each game turn. Must be negative number.
     * @param newSetting
     */
    public void setAgeModifier (int newSetting) {
        ageModifier = Math.min(newSetting, MIN_AGE_MODIFIER);
    }

    /**
     * The weight at which an entry in the tracker is removed. Typically 0 (zero) but may be higher
     * for more aggressive pruning.
     */
    public int getRemovalWeight () {
        return removalWeight;
    }

    /**
     * The weight at which an entry in the tracker is removed. Typically 0 (zero) but may be higher
     * for more aggressive pruning. Should not be a negative number.
     *
     * @param newSetting  positive number, may be 0 (zero)
     */
    public void setRemovalWeight (int newSetting) {
        removalWeight = Math.max(newSetting, 0);
    }

    /**
     * The maximum number of positions that can be tracked
     *
     * @return
     */
    public int getMaximumTrackerSize () {
        return maxPositions;
    }

    /**
     * The maximum number of positions that will be tracked by team, as well as per entity if
     * that setting is enabled. Setting this value low will minimize memory use but will be less
     * useful.
     * TODO: consider auto-setting this based on percentage of map size or other factors
     * @param newSetting
     */
    public void setMaxPositions (int newSetting) {
        maxPositions = Math.max(newSetting, MIN_TRACKER_POSITIONS);
    }

    /**
     * Get the highest ranked team positions. This could be multiple positions, a single position,
     * or none at all.
     * @return  highest rank position in the tracker; may return null if no positions available
     */
    public Collection<Coords> getTopTeamPosition() {
        if (teamActivity == null || teamActivity.isEmpty()) {
            return null;
        }
        OptionalInt maxWeight = teamActivity.values().stream().mapToInt(w -> w).max();
        return teamActivity.
                keySet().
                stream().
                filter(curPosition -> teamActivity.get(curPosition) == maxWeight.getAsInt()).
                collect(Collectors.toSet());
    }

    /**
     * Gets the highest ranked positions for the specified entity ID. This could be multiple
     * positions, a single position, or none at all.
     * @param tracked  game ID of the desired entity
     * @return   highest ranked position in the entity tracker; may return null if entity tracker
     *           is not in use or no positions are available
     */
    public Collection<Coords> getTopEntityPosition (int tracked) {
        if (!trackIndividualUnits ||
                entityActivity == null ||
                entityActivity.isEmpty() ||
                !entityActivity.containsKey(tracked)) {
            return null;
        }
        Map<Coords, Integer> trackedMap = entityActivity.get(tracked);
        OptionalInt maxWeight = trackedMap.values().stream().mapToInt(w -> w).max();
        return trackedMap.
                keySet().
                stream().
                filter(curPosition -> trackedMap.get(curPosition) == maxWeight.getAsInt()).
                collect(Collectors.toSet());
    }


    /**
     * Adjusts the heat map using the current position of each provided entity. Filters out gun
     * emplacements and ejected MechWarriors and vehicle crews
     * @param tracked
     */
    public void updateTrackers (List<Entity> tracked) {
        // For every entity that is on the team of this tracker
        for (Entity curTracked : tracked.stream().filter(e -> e.getOwner().getTeam() == teamId).collect(Collectors.toList())) {
            // Immobile - once you know where it is, it's (hopefully!) not moving
            if (curTracked instanceof GunEmplacement || curTracked instanceof EjectedCrew) {
                continue;
            }

            // Entity must be deployed, on the game map, not hidden, and visible during
            // double-blind/sensor contact conditions
            if (!curTracked.isDeployed() ||
                    curTracked.getPosition() == null ||
                    curTracked.isHidden() ||
                    !curTracked.isVisibleToEnemy() ||
                    !curTracked.isDetectedByEnemy()) {
                continue;
            }

            // Update the team and entity trackers
            processEntity(curTracked);
        }
    }


    /**
     * Reduces the values for every entry in the map. This will gradually reduce the weights over
     * time for positions that are not regularly updated.
     */
    public void ageMaps () {

        int curWeight;
        for (Coords curPosition : teamActivity.keySet()) {
            curWeight = teamActivity.get(curPosition) - ageModifier;
            teamActivity.put(curPosition, curWeight);
        }

        for (int curID : entityActivity.keySet()) {
            Map<Coords, Integer> curMap = entityActivity.get(curID);
            for (Coords curPosition : curMap.keySet()) {
                curWeight = curMap.get(curPosition) - ageModifier;
                curMap.put(curPosition, curWeight);
            }
        }

        // Remove any entries that hit the threshold for removal, and as needed for size
        trimMaps();
    }


    /**
     * Apply the entities position to the team map and the entity map if used
     * @param tracked
     */
    private void processEntity (Entity tracked) {
        Coords position = tracked.getPosition();

        // Get the map adjustments, based on entity movement
        int mapAdjustment = getTeamWeightAdjustment(tracked);

        // Update the team map, adding the position if not already present
        updateTeamMap(position, mapAdjustment);

        // If individual entity tracking is enabled, adjust the entity map
        if (trackIndividualUnits) {
            mapAdjustment = getEntityWeightAdjustment(tracked);
        }

    }

    /**
     * Update the team tracker for a given location
     * @param position
     * @param adjustment
     */
    private void updateTeamMap (Coords position, int adjustment) {
        int mapValue = teamActivity.getOrDefault(position, 0) + adjustment;
        teamActivity.put(position, mapValue);
    }


    /**
     * Get the weight of the entity for adjusting the team map. Faster units, and jumping units
     * in particular, are not likely to stay in the same place so get a much lower weight.
     * @param tracked
     * @return
     */
    private int getTeamWeightAdjustment (Entity tracked) {
        int moveWeight = MAX_MP_WEIGHT - Math.min(tracked.getWalkMP(), MAX_MP_WEIGHT);
        int jumpWeight = MAX_MP_WEIGHT - Math.min(tracked.getJumpMP(), MAX_MP_WEIGHT);

        if (jumpWeight > 0) {
            return jumpWeight <= moveWeight ? moveWeight - 1 : jumpWeight;
        }

        return moveWeight;
    }

    /**
     * Update the entity tracker for a given location
     * @param tracked
     * @param adjustment
     */
    private void updateEntityMap (Entity tracked, int adjustment) {
        int mapValue;
        Coords position = tracked.getPosition();
        Map<Coords, Integer> positionMap;

        if (entityActivity.containsKey(tracked.getId())) {
            positionMap = entityActivity.get(tracked.getId());
            if (positionMap.containsKey(position)) {
                mapValue = positionMap.get(position) + adjustment;
            } else {
                positionMap.put(position, adjustment);
            }
        } else {
            positionMap = new HashMap<>();
            positionMap.put(position, adjustment);
            entityActivity.put(tracked.getId(), positionMap);
        }
    }

    /**
     * Get the weight of the entity for adjust the individual map. Faster units, and jumping units
     * in particular, are not likely to stay in the same place so get a much lower weight.
     * Technically a duplicate of the team weight calculation, but it might be necessary for
     * different weight values.
     * @param tracked
     * @return
     */
    private int getEntityWeightAdjustment (Entity tracked) {
        int moveWeight = MAX_MP_WEIGHT - Math.min(tracked.getWalkMP(), MAX_MP_WEIGHT);
        int jumpWeight = MAX_MP_WEIGHT - Math.min(tracked.getJumpMP(), MAX_MP_WEIGHT);

        if (jumpWeight > 0) {
            return jumpWeight <= moveWeight ? moveWeight - 1 : jumpWeight;
        }

        return moveWeight;
    }

    /**
     * Removes entries from the maps which are at or below the removal weight, then trims each
     * map to meet the maximum entry limit
     */
    private void trimMaps () {

        // Team tracker
        teamActivity.entrySet().removeIf(curPos -> curPos.getValue() <= removalWeight);

        // Entity tracker
        for (int curID : entityActivity.keySet()) {
            Map<Coords, Integer> positionMap = entityActivity.get(curID);
            positionMap.entrySet().removeIf(curPos -> curPos.getValue() <= removalWeight);
        }

        clipMap(teamActivity);

        for (int curId : entityActivity.keySet()) {
            Map<Coords, Integer> curMap = entityActivity.get(curId);
            clipMap(curMap);
        }

    }

    /**
     * If the number of map entries exceeds the maximum number, removed the lowest ranked members
     * until the number of entries is acceptable
     * @param positionTracker
     */
    private void clipMap (Map<Coords, Integer> positionTracker) {
        while (maxPositions > 0 && positionTracker.size() > maxPositions) {
            OptionalInt lowestRank = positionTracker.values().stream().mapToInt(w -> w).min();
            List<Coords> removals = positionTracker.keySet().stream().filter(p -> positionTracker.get(p) == lowestRank.getAsInt()).collect(Collectors.toList());
            for (Coords curPosition : removals) {
                positionTracker.remove(curPosition);
            }
        }
    }

}
