/*
 * MegaMek - Copyright (C) 2000-2011 Ben Mazur (bmazur@sev.org)
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
package megamek.client.bot.princess;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import megamek.client.bot.princess.BotGeometry.ConvexBoardArea;
import megamek.client.bot.princess.BotGeometry.CoordFacingCombo;
import megamek.common.Aero;
import megamek.common.Compute;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.IAero;
import megamek.common.IGame;
import megamek.common.IHex;
import megamek.common.MovePath;
import megamek.common.MovePath.MoveStepType;
import megamek.common.logging.LogLevel;
import megamek.common.Terrains;
import megamek.common.pathfinder.AbstractPathFinder.Filter;
import megamek.common.pathfinder.AeroGroundPathFinder;
import megamek.common.pathfinder.AeroGroundPathFinder.AeroGroundOffBoardFilter;
import megamek.common.pathfinder.AeroLowAltitudePathFinder;
import megamek.common.pathfinder.AeroSpacePathFinder;
import megamek.common.pathfinder.InfantryPathFinder;
import megamek.common.pathfinder.LongestPathFinder;
import megamek.common.pathfinder.MovePathFinder;
import megamek.common.pathfinder.NewtonianAerospacePathFinder;
import megamek.common.pathfinder.ShortestPathFinder;

public class PathEnumerator {

    private final Princess owner;
    private final IGame game;
    private final Map<Integer, List<MovePath>> unitPaths = new ConcurrentHashMap<>();
    private final Map<Integer, ConvexBoardArea> unitMovableAreas = new ConcurrentHashMap<>();
    private final Map<Integer, Set<CoordFacingCombo>> unitPotentialLocations = new ConcurrentHashMap<>();
    private final Map<Integer, CoordFacingCombo> lastKnownLocations = new ConcurrentHashMap<>();

    private AtomicBoolean mapHasBridges = null;
    private final Object BRIDGE_LOCK = new Object();

    //todo VTOL elevation changes.

    public PathEnumerator(Princess owningPrincess, IGame game) {
        owner = owningPrincess;
        this.game = game;
    }

    /**
     * an entity and the paths it might take
     */
    /*
     * public class PathEnumeratorEntityPaths { public
     * PathEnumeratorEntityPaths(Entity e,ArrayList<MovePath> p) { entity=e;
     * paths=p; } Entity entity; ArrayList<MovePath> paths; };
     */
    private Princess getOwner() {
        return owner;
    }

    /*
     * //moved to BotGeometry //This is a list of all possible places on the map
     * an entity could end up //the structure is <EntityId, HasSet< places they
     * can move > > public class CoordFacingCombo { CoordFacingCombo() {};
     * CoordFacingCombo(MovePath p) { coords=p.getFinalCoords();
     * facing=p.getFinalFacing(); } CoordFacingCombo(Coords c,int f) { coords=c;
     * facing=f; } CoordFacingCombo(Entity e) { coords=e.getPosition();
     * facing=e.getFacing(); } Coords coords; int facing;
     *
     * @Override public boolean equals(Object o) { CoordFacingCombo
     * c=(CoordFacingCombo)o; if(!coords.equals(c.coords)) return false;
     * if(!(facing==c.facing)) return false; return true; }
     *
     * @Override public int hashCode() { return coords.hashCode()*6+facing; } }
     */

    void clear() {
        final String METHOD_NAME = "clear()";
        getOwner().methodBegin(getClass(), METHOD_NAME);
        try {
            getUnitPaths().clear();
            getUnitPotentialLocations().clear();
            getLastKnownLocations().clear();
        } finally {
            getOwner().methodEnd(getClass(), METHOD_NAME);
        }
    }

    Coords getLastKnownCoords(Integer entityId) {
        final String METHOD_NAME = "getLastKnownCoords(Integer)";
        getOwner().methodBegin(getClass(), METHOD_NAME);
        try {
            CoordFacingCombo ccr = getLastKnownLocations().get(entityId);
            if (ccr == null) {
                return null;
            }
            return ccr.getCoords();
        } finally {
            getOwner().methodEnd(getClass(), METHOD_NAME);
        }
    }

    /**
     * Returns all {@link Entity} objects located at the given {@link Coords}.
     *
     * @param location   The {@link Coords} to be searched for units.
     * @param groundOnly Set TRUE to ignore {@link Aero} units.
     * @return A {@link Set} of {@link Entity} objects at the given {@link Coords}.
     */
    public Set<Integer> getEntitiesWithLocation(Coords location, boolean groundOnly) {
        final String METHOD_NAME = "getEntitiesWithLocation(Coords, boolean)";
        getOwner().methodBegin(getClass(), METHOD_NAME);
        try {
            Set<Integer> returnSet = new TreeSet<>();
            if (location == null) {
                return returnSet;
            }
            for (Integer id : getUnitPotentialLocations().keySet()) {
                if (groundOnly
                        && getGame().getEntity(id) != null
                        && getGame().getEntity(id).isAero()) {
                    continue;
                }

                for (int facing = 0; facing < 5; facing++) {
                    if (getUnitPotentialLocations().get(id).contains(CoordFacingCombo.createCoordFacingCombo
                            (location, facing))) {
                        returnSet.add(id);
                        break;
                    }
                }
            }
            return returnSet;
        } finally {
            getOwner().methodEnd(getClass(), METHOD_NAME);
        }
    }

    /**
     * From a list of potential moves, make a potential ending location chart
     */
    void updateUnitLocations(Entity entity, List<MovePath> paths) {
        final String METHOD_NAME = "updateUnitLocations(Entity, ArrayList<MovePath>)";
        getOwner().methodBegin(getClass(), METHOD_NAME);
        try {
            // clear previous locations for this entity
            getUnitPotentialLocations().remove(entity.getId());
            //
            Set<CoordFacingCombo> toAdd = new HashSet<>();
            for (MovePath path : paths) {
                toAdd.add(CoordFacingCombo.createCoordFacingCombo(path));
            }
            getUnitPotentialLocations().put(entity.getId(), toAdd);
        } finally {
            getOwner().methodEnd(getClass(), METHOD_NAME);
        }
    }

    /**
     * calculates all moves for a given unit, keeping the shortest path to each hex/facing pair
     */
    public void recalculateMovesFor(final Entity mover) {
        final String METHOD_NAME = "recalculateMovesFor(IGame, Entity)";
        getOwner().methodBegin(getClass(), METHOD_NAME);
        try {

            // Record it's current position.
            getLastKnownLocations().put(
                    mover.getId(),
                    CoordFacingCombo.createCoordFacingCombo(
                            mover.getPosition(), mover.getFacing()));

            // Clear out any already calculated paths.
            getUnitPaths().remove(mover.getId());

            // Start constructing the new list of paths.
            List<MovePath> paths = new ArrayList<>();
            
            // Aero movement on atmospheric ground maps
            // currently only applies to a) conventional aircraft, b) aerotech units, c) lams in air mode
            if(mover.isAirborneAeroOnGroundMap() && !((IAero) mover).isSpheroid()) {
                AeroGroundPathFinder apf = AeroGroundPathFinder.getInstance(getGame());
                MovePath startPath = new MovePath(getGame(), mover);
                apf.run(startPath);
                paths.addAll(apf.getAllComputedPathsUncategorized());
                
                // Remove illegal paths.
                Filter<MovePath> filter = new Filter<MovePath>() {
                    @Override
                    public boolean shouldStay(MovePath movePath) {
                        return isLegalAeroMove(movePath);
                    }
                };
                
                this.owner.log(this.getClass(), METHOD_NAME, LogLevel.DEBUG, "Unfiltered paths: " + paths.size());
                paths = new ArrayList<>(filter.doFilter(paths));
                this.owner.log(this.getClass(), METHOD_NAME, LogLevel.DEBUG, "Filtered out illegal paths: " + paths.size());
                AeroGroundOffBoardFilter offBoardFilter = new AeroGroundOffBoardFilter();
                paths = new ArrayList<>(offBoardFilter.doFilter(paths));
                
                MovePath offBoardPath = offBoardFilter.getShortestPath();
                if(offBoardPath != null) {
                    paths.add(offBoardFilter.getShortestPath());
                }
                
                this.owner.log(this.getClass(), METHOD_NAME, LogLevel.DEBUG, "Filtered out offboard paths: " + paths.size());
                
                // This is code useful for debugging, but puts out a lot of log entries, which slows things down. 
                HashMap<Integer, Integer> pathLengths = new HashMap<Integer, Integer>();
                for(MovePath path : paths) {
                    if(!pathLengths.containsKey(path.length())) {
                        pathLengths.put(path.length(), 0);
                    }
                    Integer lengthCount = pathLengths.get(path.length());
                    pathLengths.put(path.length(), lengthCount + 1);
                    
                    this.owner.log(this.getClass(), "Path ", LogLevel.DEBUG, path.toString());
                }
                
                for(Integer length : pathLengths.keySet()) {
                    this.owner.log(this.getClass(), METHOD_NAME, LogLevel.DEBUG, "Paths of length " + length + ": " + pathLengths.get(length));
                }
            // this handles the case of the mover being an aerospace unit and "advances space flight" rules being on
            } else if(mover.isAero() && game.useVectorMove()) {
                NewtonianAerospacePathFinder npf = NewtonianAerospacePathFinder.getInstance(getGame());
                npf.run(new MovePath(game, mover));
                paths.addAll(npf.getAllComputedPathsUncategorized());
            // this handles the case of the mover being an aerospace unit on a space map
            } else if(mover.isAero() && game.getBoard().inSpace()) {
                AeroSpacePathFinder apf = AeroSpacePathFinder.getInstance(getGame());
                apf.run(new MovePath(game, mover));
                paths.addAll(apf.getAllComputedPathsUncategorized());
            // this handles the case of the mover being an infantry unit of some kind
            } else if(mover.isAero() && game.getBoard().inAtmosphere()) {
                AeroLowAltitudePathFinder apf = AeroLowAltitudePathFinder.getInstance(getGame());
                apf.run(new MovePath(game, mover));
                paths.addAll(apf.getAllComputedPathsUncategorized());
            // this handles the case of the mover being an infantry unit of some kind
            }else if(mover.hasETypeFlag(Entity.ETYPE_INFANTRY)) {
                InfantryPathFinder ipf = InfantryPathFinder.getInstance(getGame());
                ipf.run(new MovePath(game, mover));
                paths.addAll(ipf.getAllComputedPathsUncategorized());
            } else { // Non-Aero movement
                // TODO: Will this cause Princess to never use MASC?
                LongestPathFinder lpf = LongestPathFinder
                        .newInstanceOfLongestPath(mover.getRunMPwithoutMASC(),
                                MoveStepType.FORWARDS, getGame());
                lpf.run(new MovePath(game, mover));
                paths.addAll(lpf.getLongestComputedPaths());

                //add walking moves
                lpf = LongestPathFinder.newInstanceOfLongestPath(
                        mover.getWalkMP(), MoveStepType.BACKWARDS, getGame());
                lpf.run(new MovePath(getGame(), mover));
                paths.addAll(lpf.getLongestComputedPaths());

                //add jumping moves
                if (mover.getJumpMP() > 0) {
                    ShortestPathFinder spf = ShortestPathFinder
                            .newInstanceOfOneToAll(mover.getJumpMP(),
                                    MoveStepType.FORWARDS, getGame());
                    spf.run((new MovePath(game, mover))
                            .addStep(MoveStepType.START_JUMP));
                    paths.addAll(spf.getAllComputedPathsUncategorized());
                }

                for(MovePath path : paths) {
                    this.owner.log(this.getClass(), "Path ", LogLevel.DEBUG, path.toString());
                }
                
                // Try climbing over obstacles and onto bridges
                adjustPathsForBridges(paths);

                //filter those paths that end in illegal state
                Filter<MovePath> filter = new Filter<MovePath>() {
                    @Override
                    public boolean shouldStay(MovePath movePath) {
                        boolean isLegal = movePath.isMoveLegal();
                        return isLegal
                                && (Compute.stackingViolation(getGame(),
                                        mover.getId(),
                                        movePath.getFinalCoords()) == null);
                    }
                };
                paths = new ArrayList<>(filter.doFilter(paths));
            }

            // Update our locations and add the computed paths.
            updateUnitLocations(mover, paths);
            getUnitPaths().put(mover.getId(), paths);

            // calculate bounding area for move
            ConvexBoardArea myArea = new ConvexBoardArea(owner);
            myArea.addCoordFacingCombos(getUnitPotentialLocations().get(
                    mover.getId()).iterator());
            getUnitMovableAreas().put(mover.getId(), myArea);
        } finally {
            getOwner().methodEnd(getClass(), METHOD_NAME);
        }
    }

    private void adjustPathsForBridges(List<MovePath> paths) {
        if (!worryAboutBridges()) {
            return;
        }

        for (MovePath path : paths) {
            adjustPathForBridge(path);
        }
    }

    private void adjustPathForBridge(MovePath path) {
        boolean needsAdjust = false;
        for (Coords c : path.getCoordsSet()) {
            IHex hex = getGame().getBoard().getHex(c);
            if ((hex != null) && hex.containsTerrain(Terrains.BRIDGE)) {
                if (getGame().getBoard().getBuildingAt(c).getCurrentCF(c) >=
                    path.getEntity().getWeight()) {
                    needsAdjust = true;
                    break;
                } else {
                    needsAdjust = false;
                    break;
                }
            }
        }
        if (!needsAdjust) {
            return;
        }
        MovePath adjusted = new MovePath(getGame(), path.getEntity());
        adjusted.addStep(MoveStepType.CLIMB_MODE_ON);
        adjusted.addSteps(path.getStepVector(), true);
        adjusted.addStep(MoveStepType.CLIMB_MODE_OFF);
        path.replaceSteps(adjusted.getStepVector());
    }

//    public void debugPrintContents() {
//        final String METHOD_NAME = "debugPrintContents()";
//        getOwner().methodBegin(getClass(), METHOD_NAME);
//        try {
//            for (Integer id : getUnitPaths().keySet()) {
//                Entity entity = getGame().getEntity(id);
//                List<MovePath> paths = getUnitPaths().get(id);
//                int pathsSize = paths.size();
//                String msg = "Unit " + entity.getDisplayName() + " has " + pathsSize + " paths and " +
//                             getUnitPotentialLocations().get(id).size() + " ending locations.";
//                getOwner().log(getClass(), METHOD_NAME, msg);
//            }
//        } finally {
//            getOwner().methodEnd(getClass(), METHOD_NAME);
//        }
//    }

    /**
     * Returns whether a {@link MovePath} is legit for an {@link Aero} unit isMoveLegal() seems  to disagree with me
     * on some aero moves, but I can't exactly figure out why, and who is right. So, I'm just going to put a list of
     * exceptions here instead of possibly screwing up {@link MovePath#isMoveLegal()} for everyone.  I think it has
     * to do with flyoff or return at the end of a move.  This also affects cliptopossible
     *
     * @param path The path to be examined.
     * @return TRUE if the path is legal.
     */
    public boolean isLegalAeroMove(MovePath path) {
        final String METHOD_NAME = "isLegalAeroMove(MovePath)";
        getOwner().methodBegin(getClass(), METHOD_NAME);
        try {
            // no non-aeros allowed
            if (!path.getEntity().isAero()) {
                return true;
            }

            if (!path.isMoveLegal()) {
                if (path.getLastStep() == null) {
                	LogAeroMoveLegalityEvaluation("illegal move with null last step", path);
                    return false;
                }
                if ((path.getLastStep().getType() != MoveStepType.RETURN) &&
                    (path.getLastStep().getType() != MoveStepType.OFF)) {
                	LogAeroMoveLegalityEvaluation("illegal move without return/off at the end", path);
                    return false;
                }
            }

            // we have to have used all velocity by the last step
            if ((path.getLastStep() != null) && (path.getLastStep().getVelocityLeft() != 0)) {
                if ((path.getLastStep().getType() != MoveStepType.RETURN) &&
                    (path.getLastStep().getType() != MoveStepType.OFF)) {
                	LogAeroMoveLegalityEvaluation("not all velocity used without return/off at the end", path);
                    return false;
                }
            }
            return true;
        } finally {
            getOwner().methodEnd(getClass(), METHOD_NAME);
        }
    }
    
    private void LogAeroMoveLegalityEvaluation(String whyNot, MovePath path) {
    	this.getOwner().log(this.getClass(), "isLegalAeroMove", LogLevel.DEBUG, 
    			path.length() + ":" + 
    			path.toString() + ":" + whyNot);
    }

    protected Map<Integer, List<MovePath>> getUnitPaths() {
        return unitPaths;
    }

    public Map<Integer, ConvexBoardArea> getUnitMovableAreas() {
        return unitMovableAreas;
    }

    protected Map<Integer, Set<CoordFacingCombo>> getUnitPotentialLocations() {
        return unitPotentialLocations;
    }

    protected Map<Integer, CoordFacingCombo> getLastKnownLocations() {
        return lastKnownLocations;
    }

    protected IGame getGame() {
        return game;
    }

    private boolean worryAboutBridges() {
        if (mapHasBridges != null) {
            return mapHasBridges.get();
        }

        synchronized (BRIDGE_LOCK) {
            if (mapHasBridges != null) {
                return mapHasBridges.get();
            }

            mapHasBridges = new AtomicBoolean(getGame().getBoard()
                                                       .containsBridges());
        }

        return mapHasBridges.get();
    }
}
