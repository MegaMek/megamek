/*
* MegaMek -
* Copyright (C) 2017 The MegaMek Team
*
* This program is free software; you can redistribute it and/or modify it under
* the terms of the GNU General Public License as published by the Free Software
* Foundation; either version 2 of the License, or (at your option) any later
* version.
*
* This program is distributed in the hope that it will be useful, but WITHOUT
* ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
* FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
* details.
*/

package megamek.common.pathfinder;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import megamek.common.Board;
import megamek.common.Building;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.EntityMovementMode;
import megamek.common.IBoard;
import megamek.common.IHex;
import megamek.common.MiscType;
import megamek.common.MovePath;
import megamek.common.Terrains;
import megamek.common.MovePath.MoveStepType;
import megamek.common.annotations.Nullable;
import megamek.common.MoveStep;

/**
 * This class is intended to be used to find a (potentially long) legal path
 * given a movement type from a particular hex to the specified board edge
 * 
 * Note: This class is largely obsolete now, only used for its static methods
 * @author NickAragua
 *
 */
public class BoardEdgePathFinder {
    // This is a map that will tell us if a particular coordinate has a move path to a particular edge
    Map<Integer, Map<Coords, MovePath>> edgePathCache;

    // This is a map that will tell us the longest non-repeating path available to a particular coordinate
    // Useful in situations where a unit has no possible way to get from the deployment zone to the opposite edge
    // It is accumulated over multiple calls to findPathToEdge()
    // It will basically tell us the available "surface area" from a particular set of coordinates
    Map<Coords, MovePath> longestNonEdgePathCache;

    // This is a map that will tell us all the paths that connect to the path that's the key
    // Useful in a) determining a full path to the edge and
    // b) purging paths that would become invalid for whatever reason (building collapse or terrain destruction usually)
    Map<MovePath, List<MovePath>> connectedPaths;

    /**
     * Constructor - initializes internal caches
     */
    public BoardEdgePathFinder() {
        edgePathCache = new HashMap<>();
        longestNonEdgePathCache = new HashMap<>();
        connectedPaths = new HashMap<>();
    }

    /**
     * Figures out the "opposite" edge for the given entity.
     * @param entity Entity to evaluate
     * @return the Board.START_ constant representing the "opposite" edge
     */
    private int determineOppositeEdge(Entity entity) {
        IBoard board = entity.getGame().getBoard();

        // the easiest part is if the entity is supposed to start on a particular edge. Just return the opposite edge.
        int oppositeEdge = board.getOppositeEdge(entity.getStartingPos());
        if(oppositeEdge != Board.START_NONE) {
            return oppositeEdge;
        }

        // otherwise, we determine which edge of the board is closest to current position (using math) and return the "opposite" edge.
        // the lesser of entity position x or y determines the opposite.
        // if they're even, pick one arbitrarily

        int edge = Board.START_NONE;
        
        double normalizedXPosition = (double) entity.getPosition().getX() / board.getWidth();
        double normalizedYPosition = (double) entity.getPosition().getY() / board.getHeight();

        // if x is closer to the west edge and less than the y coordinate, use east edge as opposite
        if((entity.getPosition().getX() < (board.getWidth() / 2)) &&
                (normalizedXPosition < normalizedYPosition)) {
            edge = Board.START_W;
        }

        // if x is closer to the east edge and greater than the y coordinate, use west edge as opposite
        else if((entity.getPosition().getX() >= (board.getWidth() / 2)) &&
                (normalizedXPosition > normalizedYPosition)) {
            edge = Board.START_E;
        }

        // if y is closer to the north edge and greater than the x coordinate, use south edge as opposite
        else if((entity.getPosition().getY() < (board.getHeight() / 2)) &&
                (normalizedYPosition < normalizedXPosition)) {
            edge = Board.START_N;
        }
        // if y is closer to the south edge and greater than the x coordinate, use the north edge as opposite
        else if((entity.getPosition().getY() >= (board.getHeight() / 2)) &&
                (normalizedYPosition > normalizedXPosition)) {
            edge = Board.START_S;
        }

        return board.getOppositeEdge(edge);
    }

    /**
     * Helper function to set the entity to an appropriate facing given the destination region
     * Changes the actual entity's facing.
     * @param entity The entity
     * @param destinationRegion The region
     */
    private int getAppropriateFacing(Entity entity, int destinationRegion) {
        switch(destinationRegion) {
        case Board.START_N:
            return 0;
        case Board.START_S:
            return 3;
        case Board.START_E:
            if(entity.getPosition().getY() < entity.getGame().getBoard().getHeight() / 2) {
                return 2;
            }
            else {
                return 1;
            }
        case Board.START_W:
            if(entity.getPosition().getY() < entity.getGame().getBoard().getHeight() / 2) {
                return 4;
            }
            else {
                return 5;
            }
        }

        return -1;
    }

    /**
     * Helper function to directly the entity to an appropriate facing given the destination region
     * Changes the actual entity's facing.
     * @param entity The entity
     * @param destinationRegion The region
     */
    private void setAppropriateFacing(Entity entity, int destinationRegion) {
        entity.setFacing(getAppropriateFacing(entity, destinationRegion));
    }

    /**
     * Helper method that attempts to find a path that connects from the entity's current position
     * to the path's desired edge. The reason being that a particular path may technically lead to an edge,
     * but we cut the path generation short when it reaches another path that already goes to that edge.
     * @param entity
     * @return
     */
    public MovePath findCombinedPath(Entity entity) {
        MovePath currentPath = null;
        MovePath connectedPath = new MovePath(entity.getGame(), entity);

        int destinationRegion = determineOppositeEdge(entity);
        if(edgePathCache.containsKey(destinationRegion)) {
            currentPath = edgePathCache.get(destinationRegion).get(entity.getPosition());
        }

        if(currentPath == null) {
            return this.findPathToEdge(entity);
        }

        while(!isOnBoardEdge(currentPath, destinationRegion) && (connectedPath != null)) {
            if(edgePathCache.containsKey(destinationRegion)) {
                connectedPath = edgePathCache.get(destinationRegion).get(currentPath.getFinalCoords());
            } else {
                // this indicates that the end point of the current path does not go on to the desired edge
                connectedPath = null;
            }

            //
            if(currentPath != null) {
                currentPath = joinPaths(entity, currentPath, connectedPath);
            }
        }

        return currentPath;
    }

    /**
     * Helper method that takes two paths and "joins" them together.
     * The resulting path has all the steps of the starting path, a turn to get the unit to face in the direction of the second path,
     * and the rest of the second path starting from the intersection.
     * @param startingPath The beginning path
     * @param endingPath The end path
     * @return Combined path
     */
    private MovePath joinPaths(Entity entity, MovePath startingPath, MovePath endingPath) {

        // step 1: check if we've already found a path to the edge from these coordinates
        // this path may be "incomplete", but it's a good starting point.
        // the initial part of the path then is from the entity's current position
        // then, we find any path that "extends" the initial path and follow that (repeat until we reach the end)

        MovePath joinedPath = new MovePath(entity.getGame(), entity);
        boolean intersected = false;

        for(MoveStep step : startingPath.getStepVector()) {
            if(step.getPosition() == joinedPath.getFinalCoords()) {
                matchFacingToPath(joinedPath, step);
                intersected = true;
            }

            if(intersected) {
                joinedPath.addStep(step.getType());
            }
        }

        intersected = false;

        for(MoveStep step : endingPath.getStepVector()) {
            // this is the point where we intersect
            if(step.getPosition() == startingPath.getFinalCoords()) {
                matchFacingToPath(joinedPath, step);
                intersected = true;
            }

            if(intersected) {
                joinedPath.addStep(step.getType());
            }
        }

        return joinedPath;
    }

    /**
     * Helper function that, given a unit facing and a move step, adds turns to the given path until the facing of the path matches
     * the facing of the step.
     * @param initialPath
     * @param intersectionStep
     */
    private void matchFacingToPath(MovePath initialPath, MoveStep intersectionStep) {
        // algorithm: from initial facing, two rotation paths: add and subtract one
        // until we reach the desired facing with either.
        // could probably be done with geometry instead, but I'm not *that* good with abstract math

        int initialFacing = initialPath.getFinalFacing();
        int desiredFacing = intersectionStep.getFacing();
        int leftTurnFacing = initialFacing;
        int rightTurnFacing = initialFacing;
        int leftTurnCount = 0;
        int rightTurnCount = 0;

        while((leftTurnFacing != desiredFacing) && (rightTurnFacing != desiredFacing)) {
            leftTurnFacing--;
            rightTurnFacing++;
            leftTurnCount++;
            rightTurnCount++;

            // "wrap around" if we hit 0 from either edge
            if(leftTurnFacing < 0) {
                leftTurnFacing = 5;
            }

            if(rightTurnFacing > 5) {
                rightTurnFacing = 0;
            }
        }

        MoveStepType turnDirection = leftTurnCount > rightTurnCount ? MoveStepType.TURN_RIGHT : MoveStepType.TURN_LEFT;
        int turnCount = leftTurnCount > rightTurnCount ? rightTurnCount : leftTurnCount;

        for(int count = 0; count < turnCount; count++) {
            initialPath.addStep(turnDirection);
        }
    }

    /**
     * Invalidate all paths that go through this set of coordinates (because of a building or bridge collapse), or some other terrain change
     * either directly or by connecting to a path that goes through this set of coordinates.
     * @param coords
     */
    public void invalidatePaths(Coords coords) {
        // identify if this set of coordinates has a path that leads to an edge
        // loop through all paths in the path cache destined for the edge, and invalidate the ones that connect to the initial identified path
        // invalidate the first path

        for(Map<Coords, MovePath> coordinatePaths : edgePathCache.values()) {
            MovePath directPath = coordinatePaths.get(coords);

            if(directPath != null) {
                // first, clear out all cached coordinate-path entries for this path
                for(Coords pathCoords : directPath.getCoordsSet()) {
                    coordinatePaths.remove(pathCoords);
                }

                // for each path that connects to this path, invalidate it
                if(connectedPaths.containsKey(directPath)) {
                    for(MovePath connectedPath : connectedPaths.get(directPath)) {
                        invalidatePaths(connectedPath.getStartCoords());
                    }
                }
            }
        }
    }

    /**
     * Finds a legal path for the given entity to the "opposite" board edge
     * Completely ignores movement risk
     * Mostly ignores movement cost
     * "opposite" is defined as the cardinal edge furthest from the entity's current location
     * @param entity The entity for which to calculate the path
     * @return A legal move path from the entity's current location to the "opposite" edge (over several turns)
     */
    public MovePath findPathToEdge(Entity entity) {
        int destinationRegion = determineOppositeEdge(entity);

        // back up and restore the entity's original facing, as it's not nice to have side effects
        int originalFacing = entity.getFacing();
        setAppropriateFacing(entity, destinationRegion);
        MovePath pathToEdge = findPathToEdge(entity, destinationRegion);
        entity.setFacing(originalFacing);

        return pathToEdge;
    }

    /**
     * Finds a legal path for the given entity to the given board edge (please pass in a cardinal edge)
     * Completely ignores movement risk
     * Mostly ignores movement cost
     * @param entity The entity for which to calculate the path
     * @param destinationRegion The destination edge
     * @return A legal move path from the entity's current location to the edge (over several turns)
     */
    public MovePath findPathToEdge(Entity entity, int destinationRegion) {
        MovePath startPath = new MovePath(entity.getGame(), entity);
        if(entity.hasETypeFlag(Entity.ETYPE_INFANTRY)) {
            startPath.addStep(MoveStepType.CLIMB_MODE_OFF);
        } else {
            startPath.addStep(MoveStepType.CLIMB_MODE_ON);
        }

        Comparator<MovePath> movePathComparator = new SortByDistanceToEdge(destinationRegion);

        List<MovePath> candidates = new ArrayList<>();
        candidates.add(startPath);

        // a collection of coordinates we've already visited, so we don't loop back.
        Set<Coords> visitedCoords = new HashSet<>();
        visitedCoords.add(startPath.getFinalCoords());
        longestNonEdgePathCache.put(entity.getPosition(), startPath);

        while(!candidates.isEmpty()) {
            MovePath cachedPath = this.getCachedPathForCoordinates(candidates.get(0).getFinalCoords(), destinationRegion);

            if(cachedPath != null || isOnBoardEdge(candidates.get(0), destinationRegion)) {
                // if we've found a cached path and the length of the current candidate is 1
                // (it's always at least 1 due to adding the climb mode switch explicitly),
                // then we should return the cached path instead
                MovePath returnPath = ((candidates.get(0).length() == 1) && (cachedPath != null)) ? cachedPath : candidates.get(0);

                cacheGoodPath(returnPath, destinationRegion);
                return returnPath;
            }

            candidates.addAll(generateChildNodes(candidates.get(0), visitedCoords));

            // if this path moved around more than the current 'longest path', store it, just in case
            if(candidates.get(0).getHexesMoved() > longestNonEdgePathCache.get(entity.getPosition()).getHexesMoved()) {
                longestNonEdgePathCache.put(entity.getPosition(), candidates.get(0));
            }

            candidates.remove(0);
            candidates.sort(movePathComparator);
        }

        return null;
    }

    /**
     * Gets the currently stored longest non-edge path from the given entity's current position
     * @param coords The coordinates to check
     * @return A move path or null if these coordinates haven't been evaluated.
     */
    public @Nullable MovePath getLongestNonEdgePath(Coords coords) {
        return longestNonEdgePathCache.get(coords);
    }

    /**
     * Helper function that gets us a cached path for the given set of coordinates if they have a path cached
     * @param coords Coordinates to check
     * @param destinationRegion Where we're going
     * @return True or false
     */
    protected MovePath getCachedPathForCoordinates(Coords coords, int destinationRegion) {
        return edgePathCache.containsKey(destinationRegion) ? edgePathCache.get(destinationRegion).get(coords) : null;
    }

    /**
     * Helper function that tells us if the given set of coordinates have a path cached already
     * @param coords Coordinates to check
     * @param destinationRegion Where we're going
     * @return True or false
     */
    @SuppressWarnings("unused")
    private boolean coordinatesHaveCachedPath(Coords coords, int destinationRegion) {
        return edgePathCache.containsKey(destinationRegion) && edgePathCache.get(destinationRegion).containsKey(coords);
    }

    /**
     * Worker function that caches a path that gets to the destination region
     * @param path The path to cache
     * @param destinationRegion The region of the board to which the path moves
     */
    protected void cacheGoodPath(MovePath path, int destinationRegion) {
        // don't bother with all this stuff if we're not moving
        if(path.length() == 0) {
            return;
        }

        // first, attempt to connect this tributary to the trunk
        // a tributary is a smaller river that connects to a larger body of water (that's the trunk)
        MovePath trunk = getCachedPathForCoordinates(path.getFinalCoords(), destinationRegion);
        if(trunk != null) {
            if(!connectedPaths.containsKey(trunk)) {
                connectedPaths.put(trunk, new ArrayList<>());
            }

            //System.out.println("Next path connects to " + trunk.toString() + " at coordinates " + path.getFinalCoords().toString());
            connectedPaths.get(trunk).add(path);
        }

        // cache the path for the set of coordinates if one doesn't yet exist
        Map<Coords, MovePath> coordinatePathMap;

        if(!edgePathCache.containsKey(destinationRegion)) {
            coordinatePathMap = new HashMap<>();
            edgePathCache.put(destinationRegion, coordinatePathMap);
        }
        else {
            coordinatePathMap = edgePathCache.get(destinationRegion);
        }

        // cache the path for the set of coordinates if one doesn't yet exist
        // or if the current path is better than the cached one
        for(Coords coords : path.getCoordsSet()) {
            if(!coordinatePathMap.containsKey(coords) ||
                    coordinatePathMap.get(coords).getMpUsed() > path.getMpUsed()) {
                coordinatePathMap.put(coords, path);

            }
        }
    }

    /**
     * Function that generates all possible "legal" moves resulting from the given path
     * and updates the set of visited coordinates so we don't visit them again.
     * @param parentPath The path for which to generate child nodes
     * @param visitedCoords Set of visited coordinates so we don't loop around
     * @return List of valid children. Between 0 and 3 inclusive.
     */
    protected List<MovePath> generateChildNodes(MovePath parentPath, Set<Coords> visitedCoords) {
        List<MovePath> children = new ArrayList<>();

        // the children of a move path are:
        //      turn left and step forward one
        //      step forward one
        //      turn right and step forward one
        MovePath leftChild = parentPath.clone();
        leftChild.addStep(MoveStepType.TURN_LEFT);
        leftChild.addStep(MoveStepType.FORWARDS);
        processChild(leftChild, children, visitedCoords);

        MovePath centerChild = parentPath.clone();
        centerChild.addStep(MoveStepType.FORWARDS);
        processChild(centerChild, children, visitedCoords);

        MovePath rightChild = parentPath.clone();
        rightChild.addStep(MoveStepType.TURN_RIGHT);
        rightChild.addStep(MoveStepType.FORWARDS);
        processChild(rightChild, children, visitedCoords);

        return children;
    }

    /**
     * Helper function that handles logic related to potentially adding a generated child path
     * to the list of child paths.
     */
    protected void processChild(MovePath child, List<MovePath> children, Set<Coords> visitedCoords) {
        if(!visitedCoords.contains(child.getFinalCoords()) && isLegalMove(child).isLegal()) {
            visitedCoords.add(child.getFinalCoords());
            children.add(child);
        }
    }
    
    /**
     * A "light-weight" version of the logic found in "isMovementPossible" in MoveStep.java
     *
     * @param movePath The move path to process
     * @return Whether or not the given move path is "legal" in the context of this pathfinder.
     */
    protected MoveLegalityIndicator isLegalMove(MovePath movePath) {
        Coords dest = movePath.getFinalCoords();
        IBoard board = movePath.getGame().getBoard();
        IHex destHex = board.getHex(dest);
        Building destinationBuilding = board.getBuildingAt(dest);
        
        return isLegalMove(movePath, destHex, destinationBuilding);
    }

    /**
     * A "light-weight" version of the logic found in "isMovementPossible" in MoveStep.java
     * 
     *
     * @param movePath The move path to process
     * @param destHex the hex at the end of the path
     * @param destinationBuilding the building at the end of the path, can be null
     * @return Whether or not the given move path is "legal" in the context of this pathfinder.
     */
    private MoveLegalityIndicator isLegalMove(MovePath movePath, IHex destHex, Building destinationBuilding) {        
        Coords dest = movePath.getFinalCoords();
        IBoard board = movePath.getGame().getBoard();
        Coords src = movePath.getSecondLastStep().getPosition();
        IHex srcHex = board.getHex(src);
        Entity entity = movePath.getEntity();

        MoveLegalityIndicator mli = new MoveLegalityIndicator();
        
        boolean destinationInBounds = board.contains(dest);
        if(!destinationInBounds) {
            mli.outOfBounds = true;
            return mli;
        }

        // we only need to be able to legally move into the hex from the previous hex.
        // we don't care about stacking limits, remaining unit mp or other transient data
        // quadvees are not considered "tracked" for the purposes of this exercise because they can transform
        boolean isTracked = entity.getMovementMode() == EntityMovementMode.TRACKED && !entity.hasETypeFlag(Entity.ETYPE_QUADVEE);
        boolean isHovercraft = entity.getMovementMode() == EntityMovementMode.HOVER;
        boolean isWheeled = entity.getMovementMode() == EntityMovementMode.WHEELED;
        boolean isAmphibious = movePath.getCachedEntityState().hasWorkingMisc(MiscType.F_AMPHIBIOUS) ||
                            movePath.getCachedEntityState().hasWorkingMisc(MiscType.F_FULLY_AMPHIBIOUS) ||
                            movePath.getCachedEntityState().hasWorkingMisc(MiscType.F_LIMITED_AMPHIBIOUS);
        boolean destHexHasRoad = destHex.containsTerrain(Terrains.ROAD);
        
        // this indicates that we are stepping off a bridge
        boolean sourceIsBridge = srcHex.containsTerrain(Terrains.BRIDGE_CF) &&
                movePath.getSecondLastStep().getElevation() == srcHex.maxTerrainFeatureElevation(false);
        
        // this indicates that we are stepping onto a bridge
        boolean destinationIsBridge = destHex.containsTerrain(Terrains.BRIDGE_CF) && 
                movePath.getFinalElevation() == destHex.maxTerrainFeatureElevation(false);
        
        // jumpers can clear higher objects than walkers and crawlers
        int maxUpwardElevationChange = movePath.isJumping() ? movePath.getCachedEntityState().getJumpMP() : entity.getMaxElevationChange();
        // jumpers can just hop down wherever they want
        int maxDownwardElevationChange = movePath.isJumping() ? Entity.UNLIMITED_JUMP_DOWN : entity.getMaxElevationDown();
        mli.destHexElevation = calculateUnitElevationInHex(destHex, entity, isHovercraft, isAmphibious, destinationIsBridge);
        mli.srcHexElevation = calculateUnitElevationInHex(srcHex, entity, isHovercraft, isAmphibious, sourceIsBridge);     
        
        mli.elevationChange = mli.destHexElevation - mli.srcHexElevation;
        mli.steppingOntoBridge = destinationIsBridge;
        
        mli.destinationImpassable = destHex.containsTerrain(Terrains.IMPASSABLE);
        
        boolean destinationHasBuilding = destHex.containsTerrain(Terrains.BLDG_CF) || destHex.containsTerrain(Terrains.FUEL_TANK_CF);
        
        // if we're going to step onto a bridge that will collapse, let's not consider going there
        mli.destinationHasWeakBridge =  destinationIsBridge && destinationBuilding.getCurrentCF(dest) < entity.getWeight();

        // if we're going to step onto a building that will collapse, let's not consider going there
        mli.destinationHasWeakBuilding = destinationHasBuilding && destinationBuilding.getCurrentCF(dest) < entity.getWeight();

        // this condition indicates that that we are unable to go to the destination because it's too high compared to the source
        mli.goingUpTooHigh = mli.destHexElevation - mli.srcHexElevation > maxUpwardElevationChange;

        // this condition indicates that we are unable to go to the destination because it's too low compared to the source
        mli.goingDownTooLow = mli.srcHexElevation - mli.destHexElevation > maxDownwardElevationChange;

        // tanks cannot go into jungles or heavy woods unless there is a road
        mli.tankIntoHeavyWoods = isTracked &&
                (destHex.terrainLevel(Terrains.JUNGLE) > 0 || destHex.terrainLevel(Terrains.WOODS) > 1) && !destHexHasRoad;

        // hovercraft and wheeled units cannot go into jungles or woods unless there is a road
        mli.weakTankIntoWoods = (isHovercraft || isWheeled) &&
                (destHex.terrainLevel(Terrains.JUNGLE) > 0 || destHex.terrainLevel(Terrains.WOODS) > 0) && !destHexHasRoad;

        // wheeled tanks cannot go into rough terrain or rubble of any kind, or buildings for that matter
        // even if you level them they still turn to rubble. Additionally, they cannot go into deep snow.
        mli.wheeledTankRestriction = isWheeled && !destHexHasRoad &&
                (destHex.containsTerrain(Terrains.ROUGH) || destHex.containsTerrain(Terrains.RUBBLE)
                || destinationHasBuilding
                || (destHex.containsTerrain(Terrains.SNOW) && (destHex.terrainLevel(Terrains.SNOW) > 1)));

        // tracked and wheeled tanks cannot go into water without a bridge, unless amphibious
        mli.groundTankIntoWater = (isTracked || isWheeled) && 
                destHex.containsTerrain(Terrains.WATER) && (destHex.depth() > 0) && 
                !isAmphibious && !destHex.containsTerrain(Terrains.BRIDGE);

        // naval units cannot go out of water
        mli.shipOutofWater = entity.isNaval() &&
                (!destHex.containsTerrain(Terrains.WATER) || destHex.depth() < 1);

        // for future expansion of this functionality, we may consider the possibility that a building or bridge
        // will be destroyed intentionally by the bot to make way for a unit to cross
        // for now, vehicles simply will not consider going through buildings as an option
        mli.tankGoingThroughBuilding = (isWheeled || isTracked || isHovercraft) && destinationHasBuilding;

        return mli;
    }

    /**
     * Helper function that calculates the effective elevation for a unit standing there.
     * @param hex The hex to check
     * @param entity The entity to check
     * @return The effective elevation
     */
    public static int calculateUnitElevationInHex(IHex hex, Entity entity, boolean isHovercraft, boolean isAmphibious) {
        return calculateUnitElevationInHex(hex, entity, isHovercraft, isAmphibious, false);
    }
    
    /**
     * Helper function that calculates the effective elevation for a unit standing there.
     * @param hex The hex to check
     * @param entity The entity to check
     * @param bridgeTop Whether we're going on top of a bridge or under it
     * @return The effective elevation
     */
    public static int calculateUnitElevationInHex(IHex hex, Entity entity, boolean isHovercraft, boolean isAmphibious, boolean useBridgeTop) {
        // we calculate the height of a hex as "on the ground" by default
        // Special exceptions:
        // We are a mech, which can hop on top of some buildings
        // We are naval unit going under a bridge, in which case the height is the water level (naval units go on the surface, mostly)
        // We are non-naval going into water but not onto a bridge, in which case the height is the floor (mechs sink to the bottom)
        // if we are explicitly going to the top of a bridge, use that.
        
        if(useBridgeTop && !entity.isSurfaceNaval() && hex.containsTerrain(Terrains.BRIDGE_CF)) {
            return hex.ceiling();
        }
        
        int hexElevation = hex.getLevel();
        
        if (entity.hasETypeFlag(Entity.ETYPE_MECH) && 
                (hex.containsTerrain(Terrains.BLDG_CF) || hex.containsTerrain(Terrains.FUEL_TANK_CF))) {
            hexElevation = hex.ceiling();
        } else if(entity.isNaval() && hex.containsTerrain(Terrains.BRIDGE)) {
            hexElevation = hex.getLevel();
        } else if(!entity.isSurfaceNaval() && !isHovercraft && !isAmphibious &&
                hex.containsTerrain(Terrains.WATER) && !hex.containsTerrain(Terrains.BRIDGE)) {
            hexElevation = hex.floor();
        }

        return hexElevation;
    }

    /**
     * Determines if the given move path ends on the given board edge
     * @param movePath The move path to check.
     * @param destinationRegion The edge to check for.
     * @return True or false.
     */
    protected boolean isOnBoardEdge(MovePath movePath, int destinationRegion) {
        Coords coords = movePath.getFinalCoords();

        switch(destinationRegion) {
        case Board.START_N:
            return coords.getY() == 0;
        case Board.START_S:
            return coords.getY() == movePath.getGame().getBoard().getHeight() - 1;
        case Board.START_E:
            return coords.getX() == movePath.getGame().getBoard().getWidth() - 1;
        case Board.START_W:
            return coords.getX() == 0;
        default:
            return false;
        }
    }

    /**
     * Comparator implementation useful in comparing how much closer a given path is to the internal
     * "destination edge" than the other.
     * @author NickAragua
     *
     */
    private class SortByDistanceToEdge implements Comparator<MovePath> {
        private int targetRegion;

        /**
         * Constructor - initializes the destination edge.
         * @param targetRegion Destination edge
         */
        public SortByDistanceToEdge(int targetRegion) {
            this.targetRegion = targetRegion;
        }

        /**
         * compare the first move path to the second
         * Favors paths that move closer to the destination edge first.
         * in case of tie, favors paths that cost less MP
         */
        public int compare(MovePath first, MovePath second) {
            // normalize MP cost difference over max MP cost
            int costDifference = first.getMpUsed() - second.getMpUsed();
            int distanceDifference;

            switch(targetRegion) {
            // if we're heading south, the one with the bigger y coordinate is further along
            case Board.START_S:
                distanceDifference = second.getFinalCoords().getY() - first.getFinalCoords().getY();
                break;
            // if we're heading north, the one with the smaller y coordinate is further along
            case Board.START_N:
                distanceDifference = first.getFinalCoords().getY() - second.getFinalCoords().getY();
                break;
            // if we're heading east, the one with the bigger x coordinate is further along
            case Board.START_E:
                distanceDifference = second.getFinalCoords().getX() - first.getFinalCoords().getX();
                break;
            // if we're heading west, the one with the smaller x coordinate is further along
            case Board.START_W:
                distanceDifference = first.getFinalCoords().getX() - second.getFinalCoords().getX();
                break;
            default:
                distanceDifference = 0;
                break;
            }

            return distanceDifference != 0 ? distanceDifference : costDifference;
        }
    }
    
    public static class MoveLegalityIndicator {
        public boolean destinationImpassable;
        public boolean destinationHasWeakBridge;
        public boolean destinationHasWeakBuilding;
        public boolean goingUpTooHigh;
        public boolean goingDownTooLow;
        public boolean tankIntoHeavyWoods;
        public boolean weakTankIntoWoods;
        public boolean wheeledTankRestriction;
        public boolean groundTankIntoWater;
        public boolean shipOutofWater;
        public boolean tankGoingThroughBuilding;
        public boolean outOfBounds;
        
        // these are not strictly legality indicators, but they are useful to keep track of
        // so we store them here to avoid re-computing them later
        public int elevationChange;
        public boolean steppingOntoBridge;
        public int srcHexElevation;
        public int destHexElevation;
        
        public boolean isLegal() {
            return
                    !outOfBounds &&
                    !destinationImpassable &&
                    !destinationHasWeakBridge &&
                    !destinationHasWeakBuilding &&
                    !goingUpTooHigh &&
                    !goingDownTooLow &&
                    !tankIntoHeavyWoods &&
                    !weakTankIntoWoods &&
                    !wheeledTankRestriction &&
                    !groundTankIntoWater &&
                    !shipOutofWater &&
                    !tankGoingThroughBuilding;
        }
    }
}
