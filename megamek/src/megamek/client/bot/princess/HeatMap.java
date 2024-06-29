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
    private static final double MIN_TRACKER_TOLERANCE = 0.1;
    private static final double MAX_TRACKER_TOLERANCE = 0.9;
    private static final int MAX_TRACKER_ENTRIES = 100;
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
    private double mapTrimThreshold;

    // Scaling factor applied to unit weight
    private double weightScaling;

    // Tracking friendly units does not require checking visibility or detection
    private boolean isFriendlyTeam;

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
        mapTrimThreshold = MIN_TRACKER_TOLERANCE;

        weightScaling = 1.0;

        isFriendlyTeam = false;
    }


    /**
     * Base weight for movement trackers. Large values will result in movement being retained
     * longer. Typically 1.0 to 10.0 with 3.0 being the default.
     * @param newSetting  positive value, minimum 1.0
     */
    public void setMovementWeightValue (int newSetting) {
        movementWeight = (int) Math.max(newSetting, 1.0);
    }

    /**
     * Indicates if this heat map will track individual entities in addition to team positions.
     * @return
     */
    public boolean canTrackIndividuals () {
        return trackIndividualUnits;
    }

    /**
     * Enable or disable tracking of individual unit movement
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
     * Percentage of low-value tracking map entries allowed before they are removed
     *
     * @return  number between {@code MIN_TRACKER_TOLERANCE} and 0.9
     */
    public double getMapTrimThreshold () {
        return mapTrimThreshold;
    }

    /**
     * When the percentage of minimum values in any map exceeds this value, the map is trimmed of
     * all minimum values to maintain efficiency. Typical values are between 0.1 and 0.5.
     * TODO: consider auto-setting this based on percentage of map size or other factors
     * @param newSetting  positive value between {@code MIN_TRACKER_TOLERANCE} and
     *                    {@code MAX_TRACKER_TOLERANCE}
     */
    public void setMapTrimThreshold(double newSetting) {
        mapTrimThreshold = Math.min(Math.max(newSetting, MIN_TRACKER_TOLERANCE), MAX_TRACKER_TOLERANCE);
    }

    /**
     * Identifies if this is tracking a friendly team, so visibility and detection status don't apply
     */
    public boolean getIsTrackingFriendlyTeam () {
        return isFriendlyTeam;
    }

    /**
     * Tracking friendly entities doesn't require checking visibility or detection status
     */
    public void setIsTrackingFriendlyTeam (boolean newSetting) {
        isFriendlyTeam = newSetting;
    }

    // TODO: method to get nearest hotspot to provided position. Need to consider value vs range.

    /**
     * Get the highest ranked team positions. This could be multiple positions, a single position,
     * or none at all.
     * @return  highest rank position in the tracker; may return null if no positions available
     */
    public Collection<Coords> getHotSpots () {
        if (teamActivity.isEmpty()) {
            return null;
        }
        return getTopRatedPositions(teamActivity, 1, null);
    }


    // TODO: method to determine if team is travelling in column or front using team movement tracker

    // TODO: method to find point of advance using team movement tracker (highest value/group of values)

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
                    (isFriendlyTeam || e.isVisibleToEnemy() || e.isDetectedByEnemy()) &&
                    !e.isHidden()).
                collect(Collectors.toList())) {

            // Immobile - once you know where it is, it's (hopefully!) not moving ... or
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

    /**
     * Updates the heat map with a specific movement path. Filters out gun emplacements and ejected
     * MechWarriors and vehicle crews.
     * @param detailedMove  {@link MovePath} object, which includes an entity reference and Coords
     *                      for the positions moved through
     */
    public void updateTrackers (MovePath detailedMove) {
        if (detailedMove == null || detailedMove.getCoordsSet() == null) {
            return;
        }

        Entity tracked = detailedMove.getEntity();

        // Immobile - once you know where it is, it's (hopefully!) not moving ... or
        // Non-combat - not worth tracking
        if (tracked instanceof GunEmplacement || tracked instanceof EjectedCrew) {
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
        updateTeamMap(lastPosition, mapAdjustment);

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
     * Reduces the values for every entry in the map. This will gradually reduce the weights over
     * time for positions that are not regularly updated.
     */
    public void ageMaps (Game game) {

        // Bring out your dead!
        trimLastKnownPositions(game);

        if (enableDecay) {
            ageTeamMap(game);
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
     * @param game
     */
    public void refreshLastKnownCache (Game game) {

        // Don't bother updating destroyed entities
        trimLastKnownPositions(game);

        for (int curId : lastPositionCache.keySet()) {
            Entity curEntity = game.getEntity(curId);
            if (curEntity != null &&
                    curEntity.isDeployed() &&
                    curEntity.getPosition() != null &&
                    !curEntity.isCarcass() &&
                    (isFriendlyTeam || curEntity.isVisibleToEnemy() || curEntity.isDetectedByEnemy())) {
                lastPositionCache.put(curId, curEntity.getPosition());
            }
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
     * Apply decay rate to team activity map. Positions that have a trakced entity use a slower,
     * linear decay rate, while those that do not have a tracked entity use a faster exponential
     * decay rate
     * @param game
     */
    private void ageTeamMap (Game game) {

        // Get positions of tracked entities with known positions
        List<Coords> activePositions = new ArrayList<>();
        for (int curId : lastPositionCache.keySet()) {
            Entity curEntity = game.getEntity(curId);
            if (curEntity != null &&
                    !curEntity.isDestroyed() &&
                    curEntity.isDeployed() &&
                    !curEntity.isCarcass() &&
                    (isFriendlyTeam || curEntity.isVisibleToEnemy() || curEntity.isDetectedByEnemy())) {
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
                curWeight = Math.max(curWeight + decayModifier, MIN_WEIGHT);
            }
            teamActivity.put(curPosition, curWeight);
        }

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
     * Get the weight for tracking movement. For now, this is a simple number chosen so that the
     * entities movement can be evaluated for the past several rounds.
     * @param tracked
     * @return
     */
    private int getMovementWeightAdjustment(Entity tracked) {
        return (int) Math.floor(movementWeight * weightScaling);
    }

    /**
     * Apply decay rate to team movement map
     */
    private void ageTeamMovementMap () {
        int curWeight;

        for (Coords curPosition : teamMovement.keySet()) {
            curWeight = teamMovement.get(curPosition);
            if (curWeight <= MIN_WEIGHT) {
                continue;
            }
            curWeight = Math.max(curWeight + decayModifier, MIN_WEIGHT);
            teamMovement.put(curPosition, curWeight);
        }

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
     * Apply decay rate to the individual movement maps
     */
    private void ageEntityMap () {
        int curWeight;

        for (int curID : entityMovement.keySet()) {
            Map<Coords, Integer> curMap = entityMovement.get(curID);
            for (Coords curPosition : curMap.keySet()) {
                curWeight = curMap.get(curPosition);
                if (curWeight <= MIN_WEIGHT) {
                    continue;
                }
                curWeight = Math.max(curWeight + decayModifier, MIN_WEIGHT);
                curMap.put(curPosition, curWeight);
            }
        }

    }

    // TODO: add method to calculate entity movement vector based on weighted average of movement

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
     * Remove destroyed entities from the last known position tracker
     */
    private void trimLastKnownPositions (Game game) {
        for (Entity curCorpse : game.getOutOfGameEntitiesVector()) {
            if (curCorpse != null) {
                lastPositionCache.remove(curCorpse.getId());
            }
        }
    }

    /**
     * Removes all elements with a weight equal to or lower than the removal weight
     * @param checkMap
     */
    private void trimMap (Map<Coords, Integer> checkMap) {
        if ((double) checkMap.values().stream().filter(w -> w <= removalWeight).count() / checkMap.size() >= mapTrimThreshold) {
            checkMap.entrySet().removeIf(curPos -> curPos.getValue() <= removalWeight);
        }

        // Last ditch measures - trim out the lowest weights until the number of entries is
        // within reason
        while (checkMap.size() > MAX_TRACKER_ENTRIES) {
            OptionalInt lowestRank = checkMap.values().stream().mapToInt(w -> w).min();
            List<Coords> removals = checkMap.keySet().stream().filter(p -> checkMap.get(p) == lowestRank.getAsInt()).collect(Collectors.toList());
            for (Coords curPosition : removals) {
                checkMap.remove(curPosition);
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
