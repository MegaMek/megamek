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
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.TreeMap;
import java.util.TreeSet;

import megamek.client.bot.princess.BotGeometry.ConvexBoardArea;
import megamek.client.bot.princess.BotGeometry.CoordFacingCombo;
import megamek.common.Aero;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.EntityMovementType;
import megamek.common.IGame;
import megamek.common.ManeuverType;
import megamek.common.MovePath;
import megamek.common.MovePath.MoveStepType;
import megamek.common.MoveStep;

public class PathEnumerator {

    /**
     * an entity and the paths it might take
     */
    /*
     * public class PathEnumeratorEntityPaths { public
     * PathEnumeratorEntityPaths(Entity e,ArrayList<MovePath> p) { entity=e;
     * paths=p; } Entity entity; ArrayList<MovePath> paths; };
     */

    /**
     * returns an integer that represents the ending position and facing
     *
     * @param p
     * @return
     */
    public static int hashPath(MovePath p) {
        int off = ((p.getLastStep() != null) && (p.getLastStep().getType() == MoveStepType.OFF)) ? 1
                : 0;
        return ((((((((p.getFinalCoords().hashCode() * 7) + p.getFinalFacing()) * 2) + (p
                .getFinalHullDown() ? 0 : 1)) * 2)
                + ((p.getFinalProne() ? 0 : 1) * 2) + (p
                .contains(MoveStepType.MANEUVER) ? 0 : 1))) * 2)
                + off;
    }

    /**
     * information used in intermediate move path calculations
     */
    public class MovePathCalculation {
        LinkedList<MovePath> open_bymp;
        TreeMap<Integer, Integer> closed; // movepath, mp used
        ArrayList<MovePath> potential_moves;
        // to limit aero paths, keep track of which units get passed over
        TreeMap<Integer, HashSet<Integer>> passed_over;

        MovePathCalculation() {
            open_bymp = new LinkedList<MovePath>();
            closed = new TreeMap<Integer, Integer>();
            potential_moves = new ArrayList<MovePath>();
            passed_over = new TreeMap<Integer, HashSet<Integer>>();
        }

        /**
         * @return 0 if they a different state, -1 if they are they are
         *         different and a is equal or better, +1 if they are different
         *         and b is better
         */
        int comparePath(MovePath a, MovePath b) {
            if (a.getFinalCoords() != b.getFinalCoords()) {
                return 0;
            }
            if (a.getFinalFacing() != b.getFinalFacing()) {
                return 0;
            }
            if (a.getFinalHullDown() != b.getFinalHullDown()) {
                return 0;
            }
            if (a.getFinalProne() != b.getFinalProne()) {
                return 0;
            }
            if (a.getMpUsed() < b.getMpUsed()) {
                return -1;
            }
            return 1;
        }

        /**
         * returns true if movepath has already been seen using the same or less
         * mp in open or closed
         */
        boolean hasAlreadyConsidered(MovePath p) {
            Integer mpinclosed = closed.get(PathEnumerator.hashPath(p));
            if ((mpinclosed != null) && (mpinclosed <= p.getMpUsed())) {
                return true;
            }
            return false;
        }

        /**
         * This functions answers the question "Have I already considered a move
         * that ended on the same hex, and passed over at least the same units
         * this move does" The notion being that those are the only things that
         * matter in an aero move, and redundant moves can lead to things taking
         * far too long
         */
        boolean hasAlreadyAeroConsidered(MovePath p, IGame game) {
            Integer pathhash = PathEnumerator.hashPath(p);
            // build the list of units this path goes over
            HashSet<Integer> flown_over = new HashSet<Integer>();
            for (Enumeration<MoveStep> e = p.getSteps(); e.hasMoreElements();) {
                Coords cord = e.nextElement().getPosition();
                Entity enemy = game.getFirstEnemyEntity(cord, p.getEntity());
                if (enemy != null) {
                    flown_over.add(enemy.getId());
                }
            }
            HashSet<Integer> already = passed_over.get(pathhash);
            if ((already == null) || (!already.containsAll(flown_over))) {
                passed_over.put(pathhash, flown_over);
                return false;
            }
            return true;
        }

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

    HashMap<Integer, ArrayList<MovePath>> unit_paths = new HashMap<Integer, ArrayList<MovePath>>();
    HashMap<Integer, ConvexBoardArea> unit_movable_areas = new HashMap<Integer, ConvexBoardArea>();
    HashMap<Integer, HashSet<CoordFacingCombo>> unit_potential_locations = new HashMap<Integer, HashSet<CoordFacingCombo>>();
    HashMap<Integer, CoordFacingCombo> last_known_location = new HashMap<Integer, CoordFacingCombo>();
    IGame game;

    void clear() {
        unit_paths.clear();
        unit_potential_locations.clear();
        last_known_location.clear();
    }

    Coords getLastKnownCoords(Integer entityid) {
        CoordFacingCombo ccr = last_known_location.get(entityid);
        if (ccr == null) {
            return null;
        }
        return ccr.coords;
    }

    public TreeSet<Integer> getEntitiesWithLocation(Coords c,
            boolean groundnotair) {
        TreeSet<Integer> ret = new TreeSet<Integer>();
        if (c == null) {
            return ret;
        }
        for (Iterator<Integer> uplit = unit_potential_locations.keySet()
                .iterator(); uplit.hasNext();) {
            Integer onentity = uplit.next();
            if ((!(game.getEntity(onentity) instanceof Aero))
                    || (!groundnotair)) {
                for (int i = 0; i < 5; i++) {
                    if (unit_potential_locations.get(onentity).contains(
                            new CoordFacingCombo(c, i))) {
                        ret.add(onentity);
                        break;
                    }
                }
            }
        }
        return ret;
    }

    /**
     * From a list of potential moves, make a potential ending location chart
     */
    void updateUnitLocations(Entity e, ArrayList<MovePath> paths) {
        // clear previous locations for this entity
        unit_potential_locations.remove(e.getId());
        //
        HashSet<CoordFacingCombo> toadd = new HashSet<CoordFacingCombo>();
        for (MovePath p : paths) {
            toadd.add(new CoordFacingCombo(p));
        }
        unit_potential_locations.put(e.getId(), toadd);
    }

    /**
     * calculates all moves for a given unit, keeping the shortest path to each
     * hex/facing pair
     */
    public void recalculateMovesFor(IGame g, Entity e) {
        last_known_location.put(e.getId(), new CoordFacingCombo(
                e.getPosition(), e.getFacing()));
        unit_paths.remove(e.getId());
        MovePathCalculation paths = new MovePathCalculation();
        ArrayList<MovePath> starting_moves = getValidStartingMoves(g, e);
        // add any starting moves that are valid moves
        for (MovePath p : starting_moves) {
            // paths.open_byhash.put(hashPath(p),p);
            paths.open_bymp.add(p);
            if (p.isMoveLegal()) {
                paths.potential_moves.add(p);
            }
        }

        boolean aero_has_flyoff_option = false;
        // int toolong_counter=0;
        while (!paths.open_bymp.isEmpty()) {
            /*
             * toolong_counter++; if(toolong_counter%1000==0) {
             * System.err.println("PathEnumerator Update:");
             * System.err.println("open size "
             * +Integer.toString(paths.open_bymp.size()));
             * System.err.println("potential moves size "
             * +Integer.toString(paths.potential_moves.size())); }
             */
            MovePath onpath = paths.open_bymp.pop();
            //
            ArrayList<MovePath> nextpaths = getNextMoves(g, onpath);
            for (MovePath nextpath : nextpaths) {
                // add to open as potential path
                if ((e instanceof Aero)
                        || (!paths.hasAlreadyConsidered(nextpath))) {
                    // paths.open_bymp.add(nextpath); breadth-first
                    paths.open_bymp.push(nextpath); // depth first, saves memory
                    if (!(e instanceof Aero)) {
                        paths.closed.put(PathEnumerator.hashPath(nextpath),
                                nextpath.getMpUsed());
                    }
                    // if legal to finish, add as potential location
                    if ((e instanceof Aero) && isLegalAeroMove(nextpath)) {
                        if ((nextpath.getLastStep() != null)
                                && ((nextpath.getLastStep().getType() == MoveStepType.OFF) || (nextpath
                                        .getLastStep().getType() == MoveStepType.RETURN))) {
                            if (aero_has_flyoff_option) {
                                continue; // no need to compute more than one
                                          // flyoff option
                            } else {
                                aero_has_flyoff_option = true;
                            }
                        }
                        if (paths.hasAlreadyAeroConsidered(nextpath, g)) {
                            continue;
                        }
                        paths.potential_moves.add(nextpath);
                    } else if (nextpath.isMoveLegal()) {
                        paths.potential_moves.add(nextpath);
                    }
                }
            }
        }
        updateUnitLocations(e, paths.potential_moves);
        // System.err.println("calculated potential move count of "+paths.potential_moves.size()+" for entity "+e.getChassis());
        // System.err.println("#of partial moves: "+paths.closed.size());
        unit_paths.put(e.getId(), paths.potential_moves);
        // calculate bounding area for move
        ConvexBoardArea myarea = new ConvexBoardArea();
        myarea.addCoordFacingCombos(unit_potential_locations.get(e.getId())
                .iterator());
        unit_movable_areas.put(e.getId(), myarea);
    }

    /**
     * gets all the moves that are valid for an entity
     *
     * @param e
     * @return
     */
    ArrayList<MovePath> getValidStartingMoves(IGame g, Entity e) {
        ArrayList<MovePath> ret = new ArrayList<MovePath>();
        ret.add(new MovePath(g, e));
        // meks may want to start with a jump
        if (e.getJumpMP() != 0) {
            MovePath jumpmove = new MovePath(g, e);
            jumpmove.addStep(MoveStepType.START_JUMP);
            jumpmove.getStep(0).addDistance(1);
            ret.add(jumpmove);
        }
        // System.err.println("number of starting moves for "+e.getChassis()+" is "+ret.size());
        return ret;
    }

    /**
     * An extension of getNextMoves in movepath
     *
     * @param start
     * @return
     */
    ArrayList<MovePath> getNextMoves(IGame game, MovePath start) {
        ArrayList<MovePath> ret = new ArrayList<MovePath>();
        if (start.getEntity() instanceof Aero) {
            // if I've already done something illegal, or flown off, ignore
            if ((start.getSecondLastStep() != null)
                    && (start.getSecondLastStep().getMovementType() == EntityMovementType.MOVE_ILLEGAL)) {
                return ret;
            }
            if ((start.getLastStep() != null)
                    && (start.getLastStep().getType() == MoveStepType.OFF)) {
                return ret;
            }
            if ((start.getLastStep() != null)
                    && (start.getLastStep().getType() == MoveStepType.RETURN)) {
                return ret;
            }
            boolean can_accel = (start.getLastStep() == null)
                    || (start.getLastStep().getType() == MoveStepType.ACC);
            boolean can_deccel = (start.getLastStep() == null)
                    || (start.getLastStep().getType() == MoveStepType.DEC);
            boolean has_moved = !(can_accel || can_deccel);
            // move forward
            // FIXME is velocity*16 -always- the number of hexes moved?
            if ((start.getFinalVelocity() * 16) > start.getHexesMoved()) {
                ret.add(start.clone().addStep(MoveStepType.FORWARDS));
            }
            // accelerate
            // FIXME max final velocity hardcoded in, is there a place I can
            // look this up?
            if (can_accel && (start.getFinalVelocity() < 4)) {
                ret.add(start.clone().addStep(MoveStepType.ACC));
            }
            if (can_deccel && (start.getFinalVelocity() > 0)) {
                ret.add(start.clone().addStep(MoveStepType.DEC));
            }
            // turn left and right
            if ((start.getLastStep() != null)
                    && ((start.getLastStep().dueFreeTurn()))
                    && (start.getLastStep().getType() != MoveStepType.TURN_RIGHT)
                    && (start.getLastStep().getType() != MoveStepType.TURN_LEFT)) {
                ret.add(start.clone().addStep(MoveStepType.TURN_LEFT));
                ret.add(start.clone().addStep(MoveStepType.TURN_RIGHT));
            }
            // fly off of edge of board
            Coords c = start.getFinalCoords();
            if (((c.x == 0) || (c.y == 0)
                    || (c.x == (game.getBoard().getWidth() - 1)) || (c.y == (game
                    .getBoard().getHeight() - 1)))
                    && (start.getFinalVelocity() > 0)) {
                ret.add(start.clone().addStep(MoveStepType.RETURN));
            }
            // maneuvers
            // 1 maneuver per turn
            // /*
            if (!start.contains(MoveStepType.MANEUVER)) {
                // side slips
                ret.add(start.clone()
                        .addManeuver(ManeuverType.MAN_SIDE_SLIP_LEFT)
                        .addStep(MoveStepType.LATERAL_LEFT, true, true));
                ret.add(start.clone()
                        .addManeuver(ManeuverType.MAN_SIDE_SLIP_RIGHT)
                        .addStep(MoveStepType.LATERAL_RIGHT, true, true));
            }
            // */

            if ((!start.contains(MoveStepType.MANEUVER)) && (!has_moved)) {
                // System.err.println("adding start maneuvers");
                // hammerhead TODO figure out how these work
                // ret.add(start.clone().addManeuver(ManeuverType.MAN_HAMMERHEAD).
                // addStep(MoveStepType.YAW, true, true));
                // immelmen
                if (start.getFinalVelocity() > 2) {
                    // there is no reason to do an immelman and not turn
                    // ret.add(start.clone().addManeuver(ManeuverType.MAN_IMMELMAN));
                    ret.add(start.clone()
                            .addManeuver(ManeuverType.MAN_IMMELMAN)
                            .addStep(MoveStepType.TURN_LEFT));
                    ret.add(start.clone()
                            .addManeuver(ManeuverType.MAN_IMMELMAN)
                            .addStep(MoveStepType.TURN_LEFT)
                            .addStep(MoveStepType.TURN_LEFT));
                    ret.add(start.clone()
                            .addManeuver(ManeuverType.MAN_IMMELMAN)
                            .addStep(MoveStepType.TURN_RIGHT));
                    ret.add(start.clone()
                            .addManeuver(ManeuverType.MAN_IMMELMAN)
                            .addStep(MoveStepType.TURN_RIGHT)
                            .addStep(MoveStepType.TURN_RIGHT));
                    ret.add(start.clone()
                            .addManeuver(ManeuverType.MAN_IMMELMAN)
                            .addStep(MoveStepType.TURN_RIGHT)
                            .addStep(MoveStepType.TURN_RIGHT)
                            .addStep(MoveStepType.TURN_RIGHT));
                }
                // split s
                if (start.getFinalAltitude() > 2) {
                    // there is no reason to do a split-s and not turn
                    // ret.add(start.clone().addManeuver(ManeuverType.MAN_SPLIT_S));
                    ret.add(start.clone().addManeuver(ManeuverType.MAN_SPLIT_S)
                            .addStep(MoveStepType.TURN_LEFT));
                    ret.add(start.clone().addManeuver(ManeuverType.MAN_SPLIT_S)
                            .addStep(MoveStepType.TURN_LEFT)
                            .addStep(MoveStepType.TURN_LEFT));
                    ret.add(start.clone().addManeuver(ManeuverType.MAN_SPLIT_S)
                            .addStep(MoveStepType.TURN_RIGHT));
                    ret.add(start.clone().addManeuver(ManeuverType.MAN_SPLIT_S)
                            .addStep(MoveStepType.TURN_RIGHT)
                            .addStep(MoveStepType.TURN_RIGHT));
                    ret.add(start.clone().addManeuver(ManeuverType.MAN_SPLIT_S)
                            .addStep(MoveStepType.TURN_RIGHT)
                            .addStep(MoveStepType.TURN_RIGHT)
                            .addStep(MoveStepType.TURN_RIGHT));
                }
                // loop
                if (start.getFinalVelocity() > 4) {
                    ret.add(start.clone().addManeuver(ManeuverType.MAN_LOOP)
                            .addStep(MoveStepType.LOOP, true, true));
                }
            }
        } else { // meks and tanks and infantry oh my
            // if I've already done something illegal, ignore
            if ((start.getSecondLastStep() != null)
                    && (start.getSecondLastStep().getMovementType() == EntityMovementType.MOVE_ILLEGAL)) {
                return ret;
            }
            // if I'm out of movement points, ignore
            if (start.getMpUsed() >= start.getEntity().getRunMP()) {
                return ret;
            }
            // some useful variables
            MoveStepType laststeptype = start.getLastStep() == null ? null
                    : start.getLastStep().getType();
            Coords finalcoords = start.getLastStep() == null ? start
                    .getEntity().getPosition() : start.getFinalCoords();
            if (finalcoords == null) {
                return ret;
            }
            int finalfacing = start.getLastStep() == null ? start.getEntity()
                    .getFacing() : start.getFinalFacing();
            // move forward
            if (game.getBoard().contains(finalcoords.translated(finalfacing))
                    && (laststeptype != MoveStepType.BACKWARDS)) {
                ret.add(start.clone().addStep(MoveStepType.FORWARDS));
            }
            // move backward
            if (game.getBoard().contains(
                    finalcoords.translated((finalfacing + 3) % 6))
                    && (laststeptype != MoveStepType.FORWARDS)) {
                ret.add(start.clone().addStep(MoveStepType.BACKWARDS));
            }
            // turn left and right
            int last_consec_turns = countLastConsecutiveTurns(start);
            if ((laststeptype != MoveStepType.TURN_RIGHT)
                    && (last_consec_turns < 2)) {
                ret.add(start.clone().addStep(MoveStepType.TURN_LEFT));
            }
            // trick here, only do 180 degree turns by turning right, never left
            if ((laststeptype != MoveStepType.TURN_LEFT)
                    && (last_consec_turns < 3)) {
                ret.add(start.clone().addStep(MoveStepType.TURN_RIGHT));
            }
            // get up if laying down
            if (start.getFinalProne() || start.getFinalHullDown()) {
                if (start.getEntity().isCarefulStand()) {
                    ret.add(start.clone().addStep(MoveStepType.CAREFUL_STAND));
                } else {
                    ret.add(start.clone().addStep(MoveStepType.GET_UP));
                }
            }

            // finally, if a move is illegal to stop with a mek, it isn't a
            // legal move
            ArrayList<MovePath> legalmoves = new ArrayList<MovePath>();
            for (MovePath p : ret) {
                if (p.isMoveLegal()) {
                    legalmoves.add(p);
                }
            }
            return legalmoves;
        }
        return ret;
    }

    int countLastConsecutiveTurns(MovePath p) {
        int ret = 0;
        for (int i = p.length() - 1; i >= 0; i--) {
            if ((p.getStep(i).getType() == MoveStepType.TURN_RIGHT)
                    || (p.getStep(i).getType() == MoveStepType.TURN_LEFT)) {
                ret++;
            } else {
                return ret;
            }
        }
        return ret;
    }

    public void debugPrintContents() {
        for (Iterator<Integer> it = unit_paths.keySet().iterator(); it
                .hasNext();) {
            Integer id = it.next();
            Entity mye = game.getEntity(id);
            ArrayList<MovePath> paths = unit_paths.get(id);
            int paths_size = paths.size();
            System.err.println("unit " + mye.getDisplayName() + " has "
                    + paths_size + "paths ");
            System.err.println("  and "
                    + unit_potential_locations.get(id).size()
                    + " ending locations");
        }
    }

    /**
     * Returns whether a movepath is legit for an aero unit isMoveLegal() seems
     * to disagree with me on some aero moves, but I can't exactly figure out
     * why, and who is right, so I'm just going to put a list of exceptions here
     * instead of possibly screwing up isMoveLegal for everyone I think it has
     * to do with flyoff or return at the end of a move, and this also affects
     * cliptopossible
     *
     * @param p
     * @return
     */
    public boolean isLegalAeroMove(MovePath p) {
        if (!(p.getEntity() instanceof Aero)) {
            return true; // no non-aeros allowed
        }
        if (!p.isMoveLegal()) {
            if (p.getLastStep() == null) {
                return false;
            }
            if ((p.getLastStep().getType() != MoveStepType.RETURN)
                    && (p.getLastStep().getType() != MoveStepType.OFF)) {
                return false;
            }
        }
        if ((p.getLastStep() != null)
                && (p.getLastStep().getVelocityLeft() != 0)) {
            if ((p.getLastStep().getType() != MoveStepType.RETURN)
                    && (p.getLastStep().getType() != MoveStepType.OFF)) {
                return false;
            }
        }
        return true;
    }

}
