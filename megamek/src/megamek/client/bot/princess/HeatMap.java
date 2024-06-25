package megamek.client.bot.princess;

import megamek.common.Coords;
import megamek.common.EjectedCrew;
import megamek.common.Entity;
import megamek.common.GunEmplacement;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Tracks activity of visible units on the map. Board positions that are frequently occupied by
 * units get higher values while those that are the least frequent get lower values. This creates a
 * rough history of where units were concentrated which can be used for intelligent estimates about
 * where they will be in future turns.
 */
public class HeatMap {

    private static final int MIN_DECAY_MODIFIER = -1;
    private static final int MIN_WEIGHT = 0;
    private static final int MAX_WEIGHT = 100;
    private static final double BV_DIVISOR = 100.0;
    private static final int MAX_MP_WEIGHT = 10;
    private static final int MIN_TRACKER_POSITIONS = 10;

    // Which team this is tracking
    private int teamId;

    // Tracking team activity
    private Map<Coords, Integer> teamActivity;

    // Tracking individual units
    private boolean trackIndividualUnits;
    private Map<Integer, Map<Coords, Integer>> entityActivity;

    // Control whether decay is applied or not
    private boolean enableDecay;
    private int decayModifier;

    private int removalWeight;
    private int maxPositions;

    // Scaling factor applied to unit weight
    private double weightScaling;


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

        enableDecay = true;
        decayModifier = MIN_DECAY_MODIFIER;

        removalWeight = MIN_WEIGHT;
        maxPositions = MIN_TRACKER_POSITIONS;

        weightScaling = 1.0;
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
    public void setTrackIndividuals(boolean newSetting) {
        trackIndividualUnits = newSetting;
    }

    /**
     * Determines if decay is applied to the map when {@code ageMaps()} is called
     * @return  false if decay rate is disabled
     */
    public boolean isDecayEnabled() {
        return enableDecay;
    }

    /**
     * Change the setting to enable or disable decay rate on tracker weights
     * @param newSetting
     */
    public void changeDecayEnabled (boolean newSetting) {
        enableDecay = newSetting;
    }

    /**
     * Reduction weight for each map entry on each game turn
     * @return  a negative number
     */
    public int getDecayModifier() {
        return decayModifier;
    }

    /**
     * Set reduction weight for each map entry on each game turn. Must be negative number.
     * @param newSetting
     */
    public void setDecayModifier(int newSetting) {
        decayModifier = Math.min(newSetting, MIN_DECAY_MODIFIER);
    }

    /**
     * Scaling factor applied when converting entities to weights for the tracking maps
     * @return   positive, non-zero number, typically between 0.1 and 5.0 with default of 1.0
     */
    public double getWeightScaling () {
        return weightScaling;
    }

    /**
     * Scaling factor applied when converting entities to weights for the tracking maps. 0.5 is
     * half normal, 2.0 is twice normal, etc. Higher factor mean larger weight, and bigger impact
     * on the map. Typical values will be in the range of 0.1 to 5.0, although values outside that
     * are permitted.
     * @param newSetting  positive, non-zero number
     */
    public void setWeightScaling (double newSetting) {
        if (newSetting > 0) {
            weightScaling = newSetting;
        }
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

    // TODO: get nearest team hotspot to provided position. Need to consider value vs range.

    /**
     * Get the highest ranked team positions. This could be multiple positions, a single position,
     * or none at all.
     * @return  highest rank position in the tracker; may return null if no positions available
     */
    public Collection<Coords> getTopTeamPositions () {
        if (teamActivity.isEmpty()) {
            return null;
        }
        return getTopRatedPositions(teamActivity, 1, null);
    }

    /**
     * Gets the highest ranked positions for the specified entity ID. This could be multiple
     * positions, a single position, or none at all.
     * @param tracked  game ID of the desired entity
     * @return   highest ranked position in the entity tracker; may return null if entity tracker
     *           is not in use or no positions are available
     */
    public Collection<Coords> getTopEntityPositions (int tracked) {
        if (!trackIndividualUnits ||
                entityActivity.isEmpty() ||
                !entityActivity.containsKey(tracked)) {
            return null;
        }
        Map<Coords, Integer> trackedMap = entityActivity.get(tracked);
        return getTopRatedPositions(trackedMap, 1, null);
    }

    // TODO: add method to calculate and return hotspots (probably List<Collection<Coords>>)

    /**
     * Adjusts the heat map using the current position of each provided entity. Filters out gun
     * emplacements and ejected MechWarriors and vehicle crews
     * @param tracked
     */
    public void updateTrackers (List<Entity> tracked) {

        // Filter out the entities that are on the team for this tracker and are considered 'active'
        // i.e. crew is alive, unit is deployed, can be seen, and is not a concealed unit
        for (Entity curTracked : tracked.
                stream().
                filter(e -> e.getOwner().getTeam() == teamId &&
                    !e.isCarcass() &&
                    e.isDeployed() &&
                    (e.isVisibleToEnemy() || e.isDetectedByEnemy()) &&
                    !e.isHidden()).
                collect(Collectors.toList())) {

            // Immobile - once you know where it is, it's (hopefully!) not moving ...
            // Non-combat - not worth tracking
            if (curTracked instanceof GunEmplacement || curTracked instanceof EjectedCrew) {
                continue;
            }

            // Everything rides on valid coordinates, so double check
            if (curTracked.getPosition() == null) {
                continue;
            }

            // Update the team and entity trackers
            processEntity(curTracked);
        }
    }


    // TODO: add method for updating entity tracker via movement path

    /**
     * Reduces the values for every entry in the map. This will gradually reduce the weights over
     * time for positions that are not regularly updated.
     */
    public void ageMaps () {

        if (enableDecay) {

            int curWeight;
            for (Coords curPosition : teamActivity.keySet()) {
                curWeight = Math.max(teamActivity.get(curPosition) + decayModifier, MIN_WEIGHT);
                teamActivity.put(curPosition, curWeight);
            }

            for (int curID : entityActivity.keySet()) {
                Map<Coords, Integer> curMap = entityActivity.get(curID);
                for (Coords curPosition : curMap.keySet()) {
                    curWeight = Math.max(curMap.get(curPosition) + decayModifier, MIN_WEIGHT);
                    curMap.put(curPosition, curWeight);
                }
            }

        }

        // if the tracking maps are hitting the set limits, trim the entries back
        if (teamActivity.size() >= maxPositions ||
                entityActivity.values().stream().anyMatch(m -> m.size() >= maxPositions)) {
            trimMaps();
        }
    }


    /**
     * Apply the position to the team map and the entity map if used
     * @param tracked
     */
    private void processEntity (Entity tracked) {
        final Coords position = new Coords(tracked.getPosition().getX(), tracked.getPosition().getY());

        // Get the map adjustments, based on entity movement
        int mapAdjustment = getTeamWeightAdjustment(tracked);

        // Update the team map, adding the position if not already present
        updateTeamMap(position, mapAdjustment);

        // If individual entity tracking is enabled, adjust the entity map
        if (trackIndividualUnits) {
            mapAdjustment = getEntityWeightAdjustment(tracked);
            updateEntityMap(tracked.getId(), position, mapAdjustment);
        }

    }

    /**
     * Update the team tracker for a given location
     * @param position
     * @param adjustment
     */
    private void updateTeamMap (Coords position, int adjustment) {
        int mapValue = Math.min(teamActivity.getOrDefault(position, 0) + adjustment, MAX_WEIGHT);
        teamActivity.put(position, mapValue);
    }


    /**
     * Get the weight of the entity for adjusting the team map. Higher BV units get a larger weight,
     * while faster units have a reduced weight.
     * @param tracked
     * @return
     */
    private int getTeamWeightAdjustment (Entity tracked) {

        int bvWeight = tracked.getBvCalculator().retrieveBV();
        if (bvWeight == -1) {
            bvWeight = tracked.getInitialBV();
        }
        bvWeight = (int) Math.floor(weightScaling * bvWeight / BV_DIVISOR);

        bvWeight = Math.max(bvWeight - Math.max(tracked.getJumpMP(), tracked.getWalkMP()), 1);

        return bvWeight;
    }

    /**
     * Update the entity tracker for a given position
     *
     * @param trackedId
     * @param position
     * @param adjustment
     */
    private void updateEntityMap (int trackedId, Coords position, int adjustment) {
        Map<Coords, Integer> positionMap;

        if (entityActivity.containsKey(trackedId)) {
            positionMap = entityActivity.get(trackedId);
            int mapValue = adjustment;
            if (positionMap.containsKey(position)) {
                mapValue = positionMap.get(position) + adjustment;
            }
            positionMap.put(position, mapValue);
        } else {
            positionMap = new HashMap<>();
            positionMap.put(position, adjustment);
            entityActivity.put(trackedId, positionMap);
        }
    }

    /**
     * Get the weight of the entity for adjusting the individual map. Faster units, and jumping
     * units in particular, are not likely to stay in the same place so get a much lower weight.
     * @param tracked
     * @return
     */
    private int getEntityWeightAdjustment (Entity tracked) {
        int moveWeight = MAX_MP_WEIGHT - Math.min(tracked.getWalkMP(), MAX_MP_WEIGHT);
        int jumpWeight = 0;
        if (tracked.getJumpMP() > 0) {
            jumpWeight = MAX_MP_WEIGHT - Math.min(tracked.getJumpMP(), MAX_MP_WEIGHT);
        }

        if (jumpWeight > 0) {
            return jumpWeight <= moveWeight ? moveWeight - 1 : jumpWeight;
        }

        return (int) Math.floor(moveWeight * weightScaling);
    }

    /**
     * Removes entries from the maps which are at or below the removal weight, then clips each
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

    /**
     * Get the highest rated positions from a tracking map
     * @param trackingMap
     * @param maxRange
     * @param basePoint
     * @return
     */
    private Collection<Coords> getTopRatedPositions (Map<Coords, Integer> trackingMap, int maxRange, Coords basePoint) {
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
     * Split the provided Map into two lists, sorted from highest weight to lowest weight
     * @param input
     * @param positions
     * @param weights
     */
    private void splitAndSort (Map<Coords, Integer> input, List<Coords> positions, List<Integer> weights) {
        if (positions == null || weights == null) {
            throw new IllegalArgumentException("Expecting initialized List<> object(s).");
        }

        Map<Coords, Integer> workingMap = new HashMap<>(input);

        while (!workingMap.isEmpty()) {
            int maxWeight = input.values().stream().mapToInt(w -> w).max().getAsInt();

            for (Coords curPosition : input.keySet()) {
                if (input.get(curPosition) == maxWeight) {
                    positions.add(curPosition);
                    weights.add(maxWeight);
                }
            }

            workingMap.entrySet().removeIf(curPosition -> curPosition.getValue() == maxWeight);
        }

    }

}
