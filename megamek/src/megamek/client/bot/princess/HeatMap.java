package megamek.client.bot.princess;

import megamek.common.*;

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
    private static final int MOVEMENT_WEIGHT_DEFAULT = 3;

    // Which team this is tracking
    private int teamId;

    // Tracking team activity
    private Map<Coords, Integer> teamActivity;
    private Map<Coords, Integer> teamMovement;

    // Tracking individual units
    private boolean trackIndividualUnits;
    private Map<Integer, Coords> lastPositionCache;
    private Map<Integer, Map<Coords, Integer>> entityMovement;

    // Control whether decay is applied or not
    private boolean enableDecay;
    private int decayModifier;

    private int movementWeight;
    private int removalWeight;
    private int maxPositions;

    // Scaling factor applied to unit weight
    private double weightScaling;

    // TODO: cache entity positions by ID so movement can be interpolated between two points
    // TODO: add flag for interpolating movement and applying to each position in the path

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
        teamMovement = new HashMap<>();

        trackIndividualUnits = false;
        entityMovement = new HashMap<>();
        lastPositionCache = new HashMap<>();

        enableDecay = true;
        decayModifier = MIN_DECAY_MODIFIER;

        movementWeight = MOVEMENT_WEIGHT_DEFAULT;
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
     * @param newSetting   a negative number
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
     * The weight at which an entry is removed from the trackers when trimmed for size constraints.
     * Normally 0 (zero) but may be higher for more aggressive trimming.
     */
    public int getRemovalWeight () {
        return removalWeight;
    }

    /**
     * The weight at which an entry is removed from the trackers when trimmed for size constraints.
     * Must be 0 (zero) or positive number.
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

            // Update the team, team movement, and entity movement trackers
            processEntity(curTracked);
        }
    }

    // TODO: add method to calculate entity movement vector based on weighted average of movement


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

            for (Coords curPosition : teamMovement.keySet()) {
                curWeight = Math.max(teamMovement.get(curPosition) + decayModifier, MIN_WEIGHT);
                teamMovement.put(curPosition, curWeight);
            }

            for (int curID : entityMovement.keySet()) {
                Map<Coords, Integer> curMap = entityMovement.get(curID);
                for (Coords curPosition : curMap.keySet()) {
                    curWeight = Math.max(curMap.get(curPosition) + decayModifier, MIN_WEIGHT);
                    curMap.put(curPosition, curWeight);
                }
            }

        }

        // if the tracking maps are hitting the set limits, trim the entries back
        if (teamActivity.size() >= maxPositions ||
                teamMovement.size() >= maxPositions ||
                entityMovement.values().stream().anyMatch(m -> m.size() >= maxPositions)) {
            trimMaps();
        }
    }


    /**
     * Apply the position to the team maps and the entity movement map if used
     * @param tracked
     */
    private void processEntity (Entity tracked) {
        Coords position = new Coords(tracked.getPosition().getX(), tracked.getPosition().getY());

        // Get the map adjustments, based on entity position
        int mapAdjustment = getTeamWeightAdjustment(tracked);

        // Update the team map, adding the position if not already present
        updateTeamMap(position, mapAdjustment);

        // Only process the movement trackers if the entity has moved from it's last known position
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

            // If individual entity tracking is enabled, adjust the entity map
            if (trackIndividualUnits) {
                updateEntityMap(tracked.getId(), path, mapAdjustment);
            }

            // Stash this position for movement interpolation in the next round
            updateLastKnown(tracked.getId(), position);
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
     * Update the team movement tracker for a given path
     * @param path
     * @param adjustment
     */
    private void updateTeamMovementMap (List<Coords> path, int adjustment) {
        int mapValue = adjustment;
        for (Coords curPosition : path) {
            if (teamMovement.containsKey(curPosition)) {
                mapValue = teamMovement.get(curPosition) + adjustment;
            }
            teamMovement.put(curPosition, mapValue);
        }
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
     * Get the weight for tracking movement. For now, this is a simple number chosen so that the
     * entities movement can bee evaluated for the past several rounds.
     * @param tracked
     * @return
     */
    private int getMovementWeightAdjustment(Entity tracked) {
        return (int) Math.floor(movementWeight * weightScaling);
    }

    /**
     * Update the entity tracker for a given position
     *
     * @param trackedId
     * @param positions
     * @param adjustment
     */
    private void updateEntityMap (int trackedId, List<Coords> positions, int adjustment) {
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
     * Keep track of the last known position of each entity for interpolating movement
     * @param tracked
     */
    private void updateLastKnown (int trackedId, Coords position) {
        if (!lastPositionCache.containsKey(trackedId) || !lastPositionCache.get(trackedId).equals(position)) {
            lastPositionCache.put(trackedId, position);
        }
    }

    /**
     * Removes entries from the maps which are at or below the removal weight, then clips each
     * map to meet the maximum entry limit
     */
    private void trimMaps () {

        // Team trackers
        teamActivity.entrySet().removeIf(curPos -> curPos.getValue() <= removalWeight);
        teamMovement.entrySet().removeIf(curPos -> curPos.getValue() <= removalWeight);

        // Entity tracker
        for (int curID : entityMovement.keySet()) {
            Map<Coords, Integer> positionMap = entityMovement.get(curID);
            positionMap.entrySet().removeIf(curPos -> curPos.getValue() <= removalWeight);
        }

        clipMap(teamActivity);
        clipMap(teamMovement);

        for (int curId : entityMovement.keySet()) {
            Map<Coords, Integer> curMap = entityMovement.get(curId);
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
