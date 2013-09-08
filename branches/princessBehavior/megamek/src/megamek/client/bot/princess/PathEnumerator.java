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
import java.util.LinkedList;
import java.util.TreeMap;
import java.util.TreeSet;

import megamek.client.bot.princess.BotGeometry.ConvexBoardArea;
import megamek.client.bot.princess.BotGeometry.CoordFacingCombo;
import megamek.common.Aero;
import megamek.common.AmmoType;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.EntityMovementType;
import megamek.common.IGame;
import megamek.common.Infantry;
import megamek.common.ManeuverType;
import megamek.common.Mounted;
import megamek.common.MovePath;
import megamek.common.MovePath.MoveStepType;
import megamek.common.MoveStep;
import megamek.common.WeaponType;
import megamek.common.util.Logger;

public class PathEnumerator {

    private static Princess owner;

    public PathEnumerator(Princess owningPrincess) {
        owner = owningPrincess;
    }

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
        final String METHOD_NAME = "hashPath(MovePath)";
        Logger.methodBegin(PathEnumerator.class, METHOD_NAME);
        try {
            int off = ((p.getLastStep() != null) && (p.getLastStep().getType() == MoveStepType.OFF)) ? 1
                                                                                                     : 0;
            return ((((((((p.getFinalCoords().hashCode() * 7) + p.getFinalFacing()) * 2) + (p
                                                                                                    .getFinalHullDown
                                                                                                            () ? 0 :
                                                                                            1)) * 2)
                      + ((p.getFinalProne() ? 0 : 1) * 2) + (p
                                                                     .contains(MoveStepType.MANEUVER) ? 0 : 1))) * 2)
                   + off;
        } finally {
            Logger.methodEnd(PathEnumerator.class, METHOD_NAME);
        }
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
            final String METHOD_NAME = "MovePathCalculation()";
            Logger.methodBegin(getClass(), METHOD_NAME);
            try {
                open_bymp = new LinkedList<MovePath>();
                closed = new TreeMap<Integer, Integer>();
                potential_moves = new ArrayList<MovePath>();
                passed_over = new TreeMap<Integer, HashSet<Integer>>();
            } finally {
                Logger.methodEnd(getClass(), METHOD_NAME);
            }
        }

        /**
         * @return 0 if they a different state, -1 if they are they are
         *         different and a is equal or better, +1 if they are different
         *         and b is better
         */
        int comparePath(MovePath a, MovePath b) {
            final String METHOD_NAME = "comparePath(MovePath, MovePath)";
            Logger.methodBegin(getClass(), METHOD_NAME);
            try {
                if (!a.getFinalCoords().equals(b.getFinalCoords())) {
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
            } finally {
                Logger.methodEnd(getClass(), METHOD_NAME);
            }
        }

        /**
         * returns true if movepath has already been seen using the same or less
         * mp in open or closed
         */
        boolean hasAlreadyConsidered(MovePath p) {
            final String METHOD_NAME = "hasAlreadyConsidered(MovePath)";
            Logger.methodBegin(getClass(), METHOD_NAME);
            try {
                Integer mpinclosed = closed.get(PathEnumerator.hashPath(p));
                return (mpinclosed != null) && (mpinclosed <= p.getMpUsed());
            } finally {
                Logger.methodEnd(getClass(), METHOD_NAME);
            }
        }

        /**
         * This functions answers the question "Have I already considered a move
         * that ended on the same hex, and passed over at least the same units
         * this move does" The notion being that those are the only things that
         * matter in an aero move, and redundant moves can lead to things taking
         * far too long
         */
        boolean hasAlreadyAeroConsidered(MovePath p, IGame game) {
            final String METHOD_NAME = "hasAlreadyAeroConsidered(MovePath, IGame)";
            Logger.methodBegin(getClass(), METHOD_NAME);
            try {
                Integer pathhash = PathEnumerator.hashPath(p);
                // build the list of units this path goes over
                HashSet<Integer> flown_over = new HashSet<Integer>();
                for (Enumeration<MoveStep> e = p.getSteps(); e.hasMoreElements(); ) {
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
            } finally {
                Logger.methodEnd(getClass(), METHOD_NAME);
            }
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
    HashMap<Integer, HashSet<CoordFacingCombo>> unit_potential_locations = new HashMap<Integer,
            HashSet<CoordFacingCombo>>();
    HashMap<Integer, CoordFacingCombo> last_known_location = new HashMap<Integer, CoordFacingCombo>();
    IGame game;

    void clear() {
        final String METHOD_NAME = "clear()";
        Logger.methodBegin(getClass(), METHOD_NAME);
        try {
            unit_paths.clear();
            unit_potential_locations.clear();
            last_known_location.clear();
        } finally {
            Logger.methodEnd(getClass(), METHOD_NAME);
        }
    }

    Coords getLastKnownCoords(Integer entityid) {
        final String METHOD_NAME = "getLastKnownCoords(Integer)";
        Logger.methodBegin(getClass(), METHOD_NAME);
        try {
            CoordFacingCombo ccr = last_known_location.get(entityid);
            if (ccr == null) {
                return null;
            }
            return ccr.coords;
        } finally {
            Logger.methodEnd(getClass(), METHOD_NAME);
        }
    }

    public TreeSet<Integer> getEntitiesWithLocation(Coords c, boolean groundnotair) {
        final String METHOD_NAME = "getEntitiesWithLocation(Coords, boolean)";
        Logger.methodBegin(getClass(), METHOD_NAME);
        try {
            TreeSet<Integer> ret = new TreeSet<Integer>();
            if (c == null) {
                return ret;
            }
            for (Integer onentity : unit_potential_locations.keySet()) {
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
        } finally {
            Logger.methodEnd(getClass(), METHOD_NAME);
        }
    }

    /**
     * From a list of potential moves, make a potential ending location chart
     */
    void updateUnitLocations(Entity e, ArrayList<MovePath> paths) {
        final String METHOD_NAME = "updateUnitLocations(Entity, ArrayList<MovePath>)";
        Logger.methodBegin(getClass(), METHOD_NAME);
        try {
            // clear previous locations for this entity
            unit_potential_locations.remove(e.getId());
            //
            HashSet<CoordFacingCombo> toadd = new HashSet<CoordFacingCombo>();
            for (MovePath p : paths) {
                toadd.add(new CoordFacingCombo(p));
            }
            unit_potential_locations.put(e.getId(), toadd);
        } finally {
            Logger.methodEnd(getClass(), METHOD_NAME);
        }
    }

    /**
     * calculates all moves for a given unit, keeping the shortest path to each
     * hex/facing pair
     */
    public void recalculateMovesFor(IGame g, Entity e) {
        final String METHOD_NAME = "recalculateMovesFor(IGame, Entity)";
        Logger.methodBegin(getClass(), METHOD_NAME);
        try {
            last_known_location.put(e.getId(), new CoordFacingCombo(e.getPosition(), e.getFacing()));
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
                                                                                                       .getLastStep()
                                                                                                       .getType() ==
                                                                                               MoveStepType.RETURN))) {
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
            // System.err.println("calculated potential move count of "+paths.potential_moves.size()+" for entity "+e
            // .getChassis());
            // System.err.println("#of partial moves: "+paths.closed.size());
            unit_paths.put(e.getId(), paths.potential_moves);
            // calculate bounding area for move
            ConvexBoardArea myarea = new ConvexBoardArea();
            myarea.addCoordFacingCombos(unit_potential_locations.get(e.getId())
                                                                .iterator());
            unit_movable_areas.put(e.getId(), myarea);
        } finally {
            Logger.methodEnd(getClass(), METHOD_NAME);
        }
    }

    /**
     * gets all the moves that are valid for an entity
     *
     * @param e
     * @return
     */
    ArrayList<MovePath> getValidStartingMoves(IGame g, Entity e) {
        final String METHOD_NAME = "getValidStartingMoves(IGame, Entity)";
        Logger.methodBegin(getClass(), METHOD_NAME);
        try {
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
        } finally {
            Logger.methodEnd(getClass(), METHOD_NAME);
        }
    }

    /**
     * An extension of getNextMoves in movepath
     *
     * @param start
     * @return
     */
    ArrayList<MovePath> getNextMoves(IGame game, MovePath start) {
        final String METHOD_NAME = "getNextMoves(IGame, MovePath)";
        Logger.methodBegin(getClass(), METHOD_NAME);
        try {
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
                int maxMP = start.getEntity().getRunMP();
                if ((maxMP == 0) && (start.getEntity() instanceof Infantry)) {
                    maxMP = 1; //Infantry are a special case.
                }
                if (start.getMpUsed() >= maxMP) {
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
                    if (start.getEntity().isCarefulStand() && (start.getEntity().checkGetUp(start.clone().addStep
                            (MoveStepType.CAREFUL_STAND).getLastStep()).getValue() < 13)) {
                        ret.add(start.clone().addStep(MoveStepType.CAREFUL_STAND));
                    } else if (start.getEntity().checkGetUp(start.clone().addStep(MoveStepType.GET_UP).getLastStep())
                                    .getValue() < 13) {
                        ret.add(start.clone().addStep(MoveStepType.GET_UP));
                    }
                }

                // If this unit has a jammed RAC, and it has only walked,
                // add an unjam action
                if (start.getLastStep() != null) {
                    if (start.getEntity().canUnjamRAC()) {
                        if ((start.getLastStep().getMovementType() == EntityMovementType.MOVE_WALK)
                            || (start.getLastStep().getMovementType() == EntityMovementType.MOVE_VTOL_WALK)
                            || (start.getLastStep().getMovementType() == EntityMovementType.MOVE_NONE)) {
                            // Cycle through all available weapons, only unjam if the
                            // jam(med)
                            // RACs count for a significant portion of possible damage
                            int rac_damage = 0;
                            int other_damage = 0;
                            int clearance_range = 0;
                            for (Mounted equip : start.getEntity().getWeaponList()) {
                                WeaponType test_weapon;

                                test_weapon = (WeaponType) equip.getType();
                                if ((test_weapon.getAmmoType() == AmmoType.T_AC_ROTARY
                                     || (game.getOptions().booleanOption("uac_tworolls")
                                         && (test_weapon.getAmmoType() == AmmoType.T_AC_ULTRA
                                             || test_weapon.getAmmoType() == AmmoType.T_AC_ULTRA_THB)))
                                    && (equip.isJammed())) {
                                    rac_damage = rac_damage + (4 * (test_weapon.getDamage()));
                                } else {
                                    if (equip.canFire()) {
                                        other_damage += test_weapon.getDamage();
                                        if (test_weapon.getMediumRange() > clearance_range) {
                                            clearance_range = test_weapon.getMediumRange();
                                        }
                                    }
                                }
                            }
                            // Even if the jammed RAC doesn't make up a significant
                            // portion
                            // of the units damage, its still better to have it
                            // functional
                            // If nothing is "close" then unjam anyways
                            int check_range = 100;
                            for (Enumeration<Entity> unit_selection = game
                                    .getEntities(); unit_selection.hasMoreElements(); ) {
                                Entity enemy = unit_selection.nextElement();
                                if ((start.getEntity().getPosition() != null)
                                    && (enemy.getPosition() != null)
                                    && (enemy.isEnemyOf(start.getEntity()))) {
                                    if (enemy.isVisibleToEnemy()) {
                                        if (start.getEntity().getPosition()
                                                 .distance(enemy.getPosition()) < check_range) {
                                            check_range = start.getEntity()
                                                               .getPosition().distance(
                                                            enemy.getPosition());
                                        }
                                    }
                                }
                            }
                            if ((rac_damage >= other_damage)
                                || (check_range < clearance_range)) {
                                ret.add(start.clone().addStep(MoveStepType.UNJAM_RAC));
                            }
                        }
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
        } finally {
            Logger.methodEnd(getClass(), METHOD_NAME);
        }
    }

    int countLastConsecutiveTurns(MovePath p) {
        final String METHOD_NAME = "countLastConsecutiveTurns(MovePath)";
        Logger.methodBegin(getClass(), METHOD_NAME);
        try {
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
        } finally {
            Logger.methodEnd(getClass(), METHOD_NAME);
        }
    }

    public void debugPrintContents() {
        final String METHOD_NAME = "debugPrintContents()";
        Logger.methodBegin(getClass(), METHOD_NAME);
        try {
            for (Integer id : unit_paths.keySet()) {
                Entity mye = game.getEntity(id);
                ArrayList<MovePath> paths = unit_paths.get(id);
                int paths_size = paths.size();
                Logger.log(getClass(), METHOD_NAME, "unit " + mye.getDisplayName() + " has " + paths_size + "paths ");
                Logger.log(getClass(), METHOD_NAME, " and " + unit_potential_locations.get(id).size() + " ending " +
                                                    "locations");
            }
        } finally {
            Logger.methodEnd(getClass(), METHOD_NAME);
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
        final String METHOD_NAME = "isLegalAeroMove(MovePath)";
        Logger.methodBegin(getClass(), METHOD_NAME);
        try {
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
        } finally {
            Logger.methodEnd(getClass(), METHOD_NAME);
        }
    }

}
