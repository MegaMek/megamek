/**
 * MegaMek -
 * Copyright (C) 2000,2001,2002,2003,2004,2005 Ben Mazur (bmazur@sev.org)
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

package megamek.common;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;
import java.util.Vector;

import megamek.common.preference.PreferenceManager;

/**
 * Holds movement path for an entity.
 */
public class MovePath implements Cloneable, Serializable {
    private static final long serialVersionUID = -4258296679177532986L;
    public static final int STEP_FORWARDS = 1;
    public static final int STEP_BACKWARDS = 2;
    public static final int STEP_TURN_LEFT = 3;
    public static final int STEP_TURN_RIGHT = 4;
    public static final int STEP_GET_UP = 5;
    public static final int STEP_GO_PRONE = 6;
    public static final int STEP_START_JUMP = 7;
    public static final int STEP_CHARGE = 8;
    public static final int STEP_DFA = 9;
    public static final int STEP_FLEE = 10;
    public static final int STEP_LATERAL_LEFT = 11;
    public static final int STEP_LATERAL_RIGHT = 12;
    public static final int STEP_LATERAL_LEFT_BACKWARDS = 13;
    public static final int STEP_LATERAL_RIGHT_BACKWARDS = 14;
    public static final int STEP_UNJAM_RAC = 15;
    public static final int STEP_LOAD = 16;
    public static final int STEP_UNLOAD = 17;
    public static final int STEP_EJECT = 18;
    public static final int STEP_CLEAR_MINEFIELD = 19;
    public static final int STEP_UP = 20;
    public static final int STEP_DOWN = 21;
    public static final int STEP_SEARCHLIGHT = 22;
    public static final int STEP_LAY_MINE = 23;
    public static final int STEP_HULL_DOWN = 24;
    public static final int STEP_CLIMB_MODE_ON = 25;
    public static final int STEP_CLIMB_MODE_OFF = 26;
    public static final int STEP_SWIM = 27;
    public static final int STEP_DIG_IN = 28;
    public static final int STEP_FORTIFY = 29;
    public static final int STEP_SHAKE_OFF_SWARMERS = 30;
    public static final int STEP_TAKEOFF = 31;
    public static final int STEP_LAND = 32;
    public static final int STEP_ACC = 33;
    public static final int STEP_DEC = 34;
    public static final int STEP_EVADE = 35;
    public static final int STEP_ACCN = 36;
    public static final int STEP_DECN = 37;
    public static final int STEP_ROLL = 38;
    public static final int STEP_OFF  = 39;
    public static final int STEP_LAUNCH = 40;
    public static final int STEP_THRUST = 41;
    public static final int STEP_YAW = 42;
    public static final int STEP_CRASH = 43;
    public static final int STEP_STALL = 44;
    public static final int STEP_RECOVER = 45;
    public static final int STEP_RAM = 46;
    public static final int STEP_HOVER = 47;
    public static final int STEP_MANEUVER = 48;
    public static final int STEP_LOOP = 49;
    public static final int STEP_CAREFUL_STAND = 50;
    public static final int STEP_JOIN = 51;

    public static class Key {
        private final Coords coords;
        private final int facing;
        private final int type;

        public Key(final Coords coords, final int facing, final int type) {
            this.coords = coords;
            this.facing = facing;
            this.type = type;
        }

        @Override
        public boolean equals(final Object obj) {
            final Key s1 = (Key) obj;
            if (s1 != null) {
                return (type == type) && (facing == s1.facing)
                        && coords.equals(s1.coords);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return type + 7 * (facing + 31 * coords.hashCode());
        }
    }

    protected Vector<MoveStep> steps = new Vector<MoveStep>();

    protected transient IGame game;
    protected transient Entity entity;

    public static final int DEFAULT_PATHFINDER_TIME_LIMIT = 2000;

    //is this move path being done using careful movement?
    private boolean careful = true;

    /**
     * Generates a new, empty, movement path object.
     */
    public MovePath(final IGame game, final Entity entity) {
        this.entity = entity;
        this.game = game;
    }

    public Entity getEntity() {
        return entity;
    }

    public Key getKey() {
        return new Key(getFinalCoords(), getFinalFacing(), getFinalProne() ? 0
                : isJumping() ? 1 : 2);
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer();
        for (final Enumeration<MoveStep> i = steps.elements(); i
                .hasMoreElements();) {
            sb.append(i.nextElement().toString());
            sb.append(' ');
        }
        return sb.toString();
    }

    /**
     * Returns the number of steps in this movement
     */
    public int length() {
        return steps.size();
    }

    /**
     * Add a new step to the movement path.
     *
     * @param type the type of movement.
     */
    public MovePath addStep(final int type) {
        // TODO : detect steps off the map *here*.
        return addStep(new MoveStep(this, type));
    }

    /**
     * Add a new step to the movement path with the given target.
     *
     * @param type the type of movement.
     * @param target the <code>Targetable</code> object that is the target of
     *            this step. For example, the enemy being charged.
     */
    public MovePath addStep(final int type, final Targetable target) {
        return addStep(new MoveStep(this, type, target));
    }

    public MovePath addStep(final int type, final int mineToLay) {
        return addStep(type, -1, mineToLay);
    }

    public MovePath addStep(final int type, final int recover, final int mineToLay) {
        return addStep(new MoveStep(this, type, recover, mineToLay));
    }

    public MovePath addStep(int type, TreeMap<Integer, Vector<Integer>> targets) {
        return addStep(new MoveStep(this, type, targets));
    }

    public MovePath addStep(final int type, final boolean noCost) {
        return addStep(new MoveStep(this, type, noCost));
    }

    public MovePath addStep(final int type, final boolean noCost, final boolean isManeuver) {
        return addStep(new MoveStep(this, type, noCost, isManeuver));
    }

    public MovePath addStep(final int type, final Minefield mf) {
        return addStep(new MoveStep(this, type, mf));
    }

    public MovePath addManeuver(final int manType) {
        return addStep(new MoveStep(this, STEP_MANEUVER, -1, -1, manType));
    }

    public boolean canShift() {
        return ((entity instanceof QuadMech) || entity.isUsingManAce())
                && !isJumping();
    }

    /**
     * Initializes a step as part of this movement path. Then adds it to the
     * list.
     *
     * @param step
     */
    protected MovePath addStep(final MoveStep step) {
        steps.addElement(step);

        // transform lateral shifts for quads or maneuverability aces
        if (canShift()) {
            transformLateralShift();
        }
        final MoveStep prev = getStep(steps.size() - 2);

        try {
            step.compile(game, entity, prev);
        } catch (final RuntimeException re) {
            // // N.B. the pathfinding will try steps off the map.
            // re.printStackTrace();
            step.setMovementType(IEntityMovementType.MOVE_ILLEGAL);
        }

        // check for illegal jumps
        final Coords start = entity.getPosition();
        final Coords land = step.getPosition();
        final int distance = start.distance(land);
        if (isJumping()) {
            if (step.getMpUsed() > distance) {
                step.setMovementType(IEntityMovementType.MOVE_ILLEGAL);
            }
        }

        // If the new step is legal and is a different position than
        // the previous step, then update the older steps, letting
        // them know that they are no longer the end of the path.
        if (step.isLegal() && (null != prev) && !land.equals(prev.getPosition())) {

            // Loop through the steps from back to front.
            // Stop looping when the step says to, or we run out of steps.
            int index = steps.size() - 2;
            while ((index >= 0) && getStep(index).setEndPos(false)) {
                index--;
            }

        } // End step-is-legal

        return this;
    }

    public void compile(final IGame g, final Entity en) {
        game = g;
        entity = en;
        final Vector<MoveStep> temp = new Vector<MoveStep>(steps);
        steps.removeAllElements();
        for (int i = 0; i < temp.size(); i++) {
            MoveStep step = temp.elementAt(i);
            if (step.getTarget(game) != null) {
                step = new MoveStep(this, step.getType(), step.getTarget(game));
            } else if (step.getRecoveryUnit() != -1) {
                step = new MoveStep(this, step.getType(), step.getRecoveryUnit(), -1);
            } else if (step.getMineToLay() != -1){
                step = new MoveStep(this, step.getType(), step.getMineToLay());
            } else if (step.getLaunched().size() > 0) {
                step = new MoveStep(this, step.getType(), step.getLaunched());
            } else if (step.getManeuverType() != ManeuverType.MAN_NONE) {
                step = new MoveStep(this, step.getType(), -1, -1, step.getManeuverType());
            } else if (step.isManeuver()) {
                step = new MoveStep(this, step.getType(), step.hasNoCost(), step.isManeuver());
            } else if(step.hasNoCost()) {
                step = new MoveStep(this, step.getType(), step.hasNoCost());
            } else if(null != step.getMinefield()) {
                step = new MoveStep(this, step.getType(), step.getMinefield());
            } else {

                step = new MoveStep(this, step.getType());
            }
            this.addStep(step);
        }
        clipToPossible();
    }

    public void removeLastStep() {
        if (steps.size() > 0) {
            steps.removeElementAt(steps.size() - 1);
        }

        // Find the new last step in the path.
        int index = steps.size() - 1;
        while ((index >= 0) && getStep(index).setEndPos(true)
                && !getStep(index).isLegal()) {
            index--;
        }
    }

    public void clear() {
        steps.removeAllElements();
    }

    public Enumeration<MoveStep> getSteps() {
        return steps.elements();
    }

    public MoveStep getStep(final int index) {
        if ((index < 0) || (index >= steps.size())) {
            return null;
        }
        return steps.elementAt(index);
    }

    /**
     * Check for any of the specified type of step in the path
     */
    public boolean contains(final int type) {
        for (final Enumeration<MoveStep> i = getSteps(); i.hasMoreElements();) {
            final MoveStep step = i.nextElement();
            if (step.getType() == type) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check for MASC use
     */
    public boolean hasActiveMASC() {
        for (final Enumeration<MoveStep> i = getSteps(); i.hasMoreElements();) {
            final MoveStep step = i.nextElement();
            if (step.isUsingMASC()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the final coordinates if a mech were to perform all the steps in
     * this path.
     */
    public Coords getFinalCoords() {
        if (getLastStep() != null) {
            return getLastStep().getPosition();
        }
        return entity.getPosition();
    }

    /**
     * Returns the final facing if a mech were to perform all the steps in this
     * path.
     */
    public int getFinalFacing() {
        if (getLastStep() != null) {
            return getLastStep().getFacing();
        }
        return entity.getFacing();
    }

    /**
     * Returns whether or not a unit would end up prone after all of the steps
     */
    public boolean getFinalProne() {
        if (getLastStep() != null) {
            return getLastStep().isProne();
        }
        if (entity == null) {
            return false;
        }
        return entity.isProne();
    }

    /**
     * Returns whether or not a unit would end up prone after all of the steps
     */
    public boolean getFinalHullDown() {
        if (getLastStep() != null) {
            return getLastStep().isHullDown();
        }
        if (entity == null) {
            return false;
        }
        return entity.isHullDown();
    }

    /**
     * Returns whether or not a unit would be in climb mode after all the steps
     */
    public boolean getFinalClimbMode() {
        if (getLastStep() != null) {
            return getLastStep().climbMode();
        }
        if (entity == null) {
            return false;
        }
        return entity.climbMode();
    }

    /**
     * get final elevation relative to the hex.
     */
    public int getFinalElevation() {
        if (getLastStep() != null) {
            return getLastStep().getElevation();
        }
        return entity.getElevation();
    }

    public int getFinalVelocity() {
        if (getLastStep() != null) {
            return getLastStep().getVelocity();
        }
        if(entity instanceof Aero) {
            return ((Aero)entity).getCurrentVelocity();
        }
        return 0;
    }

    public int getFinalNDown() {
        if (getLastStep() != null) {
            return getLastStep().getNDown();
        }

        return 0;
    }

    /**
     * Returns the final vector for advanced movement
     */
    public int[] getFinalVectors() {
        if (getLastStep() != null) {
            return getLastStep().getVectors();
        }
        return entity.getVectors();
    }

    public int getLastStepMovementType() {
        if (getLastStep() == null) {
            return IEntityMovementType.MOVE_NONE;
        }
        return getLastStep().getMovementType();
    }

    public MoveStep getLastStep() {
        return getStep(steps.size() - 1);
    }

    public MoveStep getSecondLastStep() {
        if (steps.size() > 1) {
            return getStep(steps.size() - 2);
        }
        return getLastStep();
    }

    /* Debug method */
    public void printAllSteps() {
        System.out.println("*Steps*");
        for (int i = 0; i < steps.size(); i++) {
            System.out.println("  " + i + ": " + getStep(i) + ", "
                    + getStep(i).getMovementType());
        }
    }

    /**
     * Removes impossible steps.
     */
    public void clipToPossible() {
        // hopefully there's no impossible steps in the middle of possible ones
        final Vector<MoveStep> goodSteps = new Vector<MoveStep>();
        for (final Enumeration<MoveStep> i = steps.elements(); i
                .hasMoreElements();) {
            final MoveStep step = i.nextElement();
            if (step.getMovementType() != IEntityMovementType.MOVE_ILLEGAL) {
                goodSteps.addElement(step);
            }
        }
        steps = goodSteps;
    }

    /**
     * Changes turn-forwards-opposite-turn sequences into quad lateral shifts.
     * <p/> Finds the sequence of three steps that can be transformed, then
     * removes all three and replaces them with the lateral shift step.
     */
    private void transformLateralShift() {
        if (steps.size() < 3) {
            return;
        }
        final int index = steps.size() - 3;
        final MoveStep step1 = getStep(index);
        final MoveStep step2 = getStep(index + 1);
        final MoveStep step3 = getStep(index + 2);

        if (step1.oppositeTurn(step3)
                && ((step2.getType() == MovePath.STEP_BACKWARDS) || (step2
                        .getType() == MovePath.STEP_FORWARDS))) {
            final int stepType = step1.getType();
            final int direction = step2.getType();
            // remove all old steps
            steps.removeElementAt(index);
            steps.removeElementAt(index);
            steps.removeElementAt(index);
            // add new step
            final MoveStep shift = new MoveStep(this, lateralShiftForTurn(
                    stepType, direction));
            addStep(shift);
        }
    }

    /**
     * Returns the lateral shift that corresponds to the turn direction
     */
    public static int lateralShiftForTurn(final int turn, final int direction) {
        if (direction == MovePath.STEP_FORWARDS) {
            switch (turn) {
                case MovePath.STEP_TURN_LEFT:
                    return MovePath.STEP_LATERAL_LEFT;
                case MovePath.STEP_TURN_RIGHT:
                    return MovePath.STEP_LATERAL_RIGHT;
                default:
                    return turn;
            }
        }
        switch (turn) {
            case MovePath.STEP_TURN_LEFT:
                return MovePath.STEP_LATERAL_LEFT_BACKWARDS;
            case MovePath.STEP_TURN_RIGHT:
                return MovePath.STEP_LATERAL_RIGHT_BACKWARDS;
            default:
                return turn;
        }
    }

    /**
     * Returns the turn direction that corresponds to the lateral shift
     */
    static int turnForLateralShift(final int shift) {
        switch (shift) {
            case MovePath.STEP_LATERAL_LEFT:
                return MovePath.STEP_TURN_LEFT;
            case MovePath.STEP_LATERAL_RIGHT:
                return MovePath.STEP_TURN_RIGHT;
            case MovePath.STEP_LATERAL_LEFT_BACKWARDS:
                return MovePath.STEP_TURN_LEFT;
            case MovePath.STEP_LATERAL_RIGHT_BACKWARDS:
                return MovePath.STEP_TURN_RIGHT;
            default:
                return shift;
        }
    }

    /**
     * Returns the direction (either MovePath.STEP_TURN_LEFT or STEP_TURN_RIGHT)
     * that the destination facing lies in.
     */
    public static int getDirection(final int facing, final int destFacing) {
        final int rotate = (destFacing + (6 - facing)) % 6;
        return rotate >= 3 ? STEP_TURN_LEFT : STEP_TURN_RIGHT;
    }

    /**
     * Returns the adjusted facing, given the start facing.
     */
    public static int getAdjustedFacing(final int facing, final int movement) {
        if (movement == STEP_TURN_RIGHT) {
            return (facing + 1) % 6;
        } else if (movement == STEP_TURN_LEFT) {
            return (facing + 5) % 6;
        }
        return facing;
    }

    /**
     * Returns the number of MPs used in the path
     */
    public int getMpUsed() {
        if (getLastStep() != null) {
            return getLastStep().getMpUsed();
        }
        return 0;
    }

    /**
     * Returns the logical number of hexes moved the path (does not count turns,
     * etc).
     */
    public int getHexesMoved() {
        if (getLastStep() == null) {
            return 0;
        }
        return getLastStep().getDistance();
    }

    /**
     * Returns true if the entity is jumping or if it's a flying lam.
     */
    public boolean isJumping() {
        if (steps.size() > 0) {
            boolean jump = false;
            for (MoveStep step : steps) {
                if (step.getType() == MovePath.STEP_START_JUMP) {
                    jump = true;
                }
            }
            return jump;
        }
        return isFlying();
    }

    /**
     * Returns if the entity is flying at the last step of this movepath.
     * WARNING: This function will only evaluate the path of
     * <code>LandAirMech</code>s, for all other types it will return false.
     *
     * @return true if it's a flying LAM.
     */
    public boolean isFlying() {
        if (entity instanceof LandAirMech) {
            boolean flying = entity.isFlying();
            for (MoveStep step : steps) {
                if (step.getType() == STEP_TAKEOFF) {
                    flying = true;
                } else if (step.getType() == STEP_LAND) {
                    flying = false;
                }
            }
            return flying;
        }

        return false;
    }

    /**
     * Extend the current path to the destination <code>Coords</code>.
     *
     * @param dest the destination <code>Coords</code> of the move.
     * @param type the type of movment step required.
     */
    public void findPathTo(final Coords dest, final int type) {
        final int timeLimit = PreferenceManager.getClientPreferences()
                .getMaxPathfinderTime();

        if (timeLimit >= 5000) {
            System.out.print("WARNING!!!  Settings allow up to ");
            System.out.print(timeLimit);
            System.out.println(" milliseconds to find the optimum path!");
        }
        notSoLazyPathfinder(dest, type, timeLimit);
    }

    public boolean isMoveLegal() {
        // Moves which end up off of the board are not legal.
        if (!game.getBoard().contains(getFinalCoords())) {
            return false;
        }

        //for aero units move must use up all their velocity
        if(entity instanceof Aero) {
            Aero a = (Aero)entity;
            if(getLastStep() == null) {
                if((a.getCurrentVelocity() > 0) && !game.useVectorMove()) {
                    return false;
                }
            } else {
                if((getLastStep().getVelocityLeft() > 0) && !game.useVectorMove() && (getLastStep().getType() != MovePath.STEP_FLEE)) {
                    return false;
                }
            }
        }

        if (getLastStep() == null) {
            return true;
        }

        if (getLastStep().getMovementType() == STEP_CHARGE) {
            return getSecondLastStep().isLegal();
        }
        if (getLastStep().getMovementType() == STEP_RAM) {
            return getSecondLastStep().isLegal();
        }
        return getLastStep().isLegal();
    }

    /**
     * An A* pathfinder to get from the end of the current path (or entity's
     * position if empty) to the destination.
     *
     * @param dest The goal hex
     * @param type The type of move we want to do
     * @param timeLimit the maximum <code>int</code> number of milliseconds to
     *            take hunting for an ideal path.
     */
    private void notSoLazyPathfinder(final Coords dest, final int type,
            final int timeLimit) {
        final long endTime = System.currentTimeMillis() + timeLimit;

        int step = type;
        if (step != MovePath.STEP_BACKWARDS) {
            step = MovePath.STEP_FORWARDS;
        }

        final MovePathComparator mpc = new MovePathComparator(dest,
                step == MovePath.STEP_BACKWARDS);

        MovePath bestPath = clone();

        final HashMap<MovePath.Key, MovePath> discovered = new HashMap<MovePath.Key, MovePath>();
        discovered.put(bestPath.getKey(), bestPath);

        final ArrayList<MovePath> candidates = new ArrayList<MovePath>();
        candidates.add(bestPath);

        boolean keepLooping = getFinalCoords().distance(dest) > 1;
        int loopcount = 0;

        while ((candidates.size() > 0) && keepLooping) {
            final MovePath candidatePath = candidates.remove(0);
            final Coords startingPos = candidatePath.getFinalCoords();
            final int startingElev = candidatePath.getFinalElevation();

            if (candidatePath.getFinalCoords().distance(dest) == 1) {
                bestPath = candidatePath;
                keepLooping = false;
                break;
            }

            final Iterator<MovePath> adjacent = candidatePath.getNextMoves(
                    step == STEP_BACKWARDS, step == STEP_FORWARDS).iterator();
            while (adjacent.hasNext()) {
                final MovePath expandedPath = adjacent.next();

                if (expandedPath.getLastStep().isMovementPossible(game,
                        startingPos, startingElev)) {
                    final MovePath found = discovered
                            .get(expandedPath.getKey());
                    if ((found != null) && (mpc.compare(found, expandedPath) <= 0)) {
                        continue;
                    }
                    int index = Collections.<MovePath> binarySearch(candidates,
                            expandedPath, mpc);
                    if (index < 0) {
                        index = -index - 1;
                    }
                    candidates.add(index, expandedPath);
                    discovered.put(expandedPath.getKey(), expandedPath);
                    if (candidates.size() > 100) {
                        candidates.remove(candidates.size() - 1);
                    }
                }
            }
            loopcount++;
            if ((loopcount % 256 == 0) && keepLooping && (candidates.size() > 0)) {
                final MovePath front = candidates.get(0);
                if (front.getFinalCoords().distance(dest) < bestPath
                        .getFinalCoords().distance(dest)) {
                    bestPath = front;
                    keepLooping = System.currentTimeMillis() < endTime;
                } else {
                    keepLooping = false;
                }
            }
        } // end while

        if (getFinalCoords().distance(dest) > bestPath.getFinalCoords()
                .distance(dest)) {
            // Make the path we found, this path.
            steps = bestPath.steps;
        }
        if (!getFinalCoords().equals(dest)) {
            lazyPathfinder(dest, type);
        }
    }

    /**
     * Find the shortest path to the destination <code>Coords</code> by hex
     * count. This right choice <em>only</em> when making a simple move like a
     * straight line or one with a single turn.
     *
     * @param dest the destination <code>Coords</code> of the move.
     * @param type the type of movment step required.
     */
    private void lazyPathfinder(final Coords dest, final int type) {
        int step = STEP_FORWARDS;
        if (type == STEP_BACKWARDS) {
            step = STEP_BACKWARDS;
        }
        Coords subDest = dest;
        if (!dest.equals(getFinalCoords())) {
            subDest = dest.translated(dest.direction(getFinalCoords()));
        }

        while (!getFinalCoords().equals(subDest)) {
            // adjust facing
            rotatePathfinder((getFinalCoords().direction(subDest) + (step == STEP_BACKWARDS ? 3
                    : 0)) % 6, false);
            // step forwards
            addStep(step);
        }
        rotatePathfinder((getFinalCoords().direction(dest) + (step == STEP_BACKWARDS ? 3
                : 0)) % 6, false);
        if (!dest.equals(getFinalCoords())) {
            addStep(type);
        }
    }

    /**
     * Returns a list of possible moves that result in a
     * facing/position/(jumping|prone) change, special steps (mine clearing and
     * such) must be handled elsewhere.
     */
    public List<MovePath> getNextMoves(boolean backward, boolean forward) {
        final ArrayList<MovePath> result = new ArrayList<MovePath>();
        final MoveStep last = getLastStep();
        if (isJumping()) {
            final MovePath left = clone();
            final MovePath right = clone();

            // From here, we can move F, LF, RF, LLF, RRF, and RRRF.
            result.add(clone().addStep(MovePath.STEP_FORWARDS));
            for (int turn = 0; turn < 2; turn++) {
                left.addStep(MovePath.STEP_TURN_LEFT);
                right.addStep(MovePath.STEP_TURN_RIGHT);
                result.add(left.clone().addStep(MovePath.STEP_FORWARDS));
                result.add(right.clone().addStep(MovePath.STEP_FORWARDS));
            }
            right.addStep(MovePath.STEP_TURN_RIGHT);
            result.add(right.addStep(MovePath.STEP_FORWARDS));

            // We've got all our next steps.
            return result;
        }

        //need to do a separate section here for Aeros.
        //just like jumping for now, but I could add some other stuff
        //here later
        if(entity instanceof Aero) {
            MovePath left = clone();
            MovePath right = clone();

            // From here, we can move F, LF, RF, LLF, RRF, and RRRF.
            result.add((clone()).addStep(MovePath.STEP_FORWARDS) );
            for ( int turn = 0; turn < 2; turn++ ) {
                left.addStep(MovePath.STEP_TURN_LEFT);
                right.addStep(MovePath.STEP_TURN_RIGHT);
                result.add(left.clone().addStep(MovePath.STEP_FORWARDS));
                result.add(right.clone().addStep(MovePath.STEP_FORWARDS));
            }
            right.addStep(MovePath.STEP_TURN_RIGHT);
            result.add( right.addStep(MovePath.STEP_FORWARDS) );

            // We've got all our next steps.
            return result;
        }

        if (getFinalProne() || getFinalHullDown()) {
            if ((last != null) && (last.getType() != STEP_TURN_RIGHT)) {
                result.add(clone().addStep(MovePath.STEP_TURN_LEFT));
            }
            if ((last != null) && (last.getType() != STEP_TURN_LEFT)) {
                result.add(clone().addStep(MovePath.STEP_TURN_RIGHT));
            }

            if ( entity.isCarefulStand() ) {
                result.add(clone().addStep(MovePath.STEP_CAREFUL_STAND));
            } else {
                result.add(clone().addStep(MovePath.STEP_GET_UP));
            }
            return result;
        }
        if (canShift()) {
            if (forward
                    && (!backward || ((last == null) || (last.getType() != MovePath.STEP_LATERAL_LEFT)))) {
                result.add(clone().addStep(STEP_LATERAL_RIGHT));
            }
            if (forward
                    && (!backward || ((last == null) || (last.getType() != MovePath.STEP_LATERAL_RIGHT)))) {
                result.add(clone().addStep(MovePath.STEP_LATERAL_LEFT));
            }
            if (backward
                    && (!forward || ((last == null) || (last.getType() != MovePath.STEP_LATERAL_LEFT_BACKWARDS)))) {
                result.add(clone().addStep(
                        MovePath.STEP_LATERAL_RIGHT_BACKWARDS));
            }
            if (backward
                    && (!forward || ((last == null) || (last.getType() != MovePath.STEP_LATERAL_RIGHT_BACKWARDS)))) {
                result.add(clone().addStep(
                        MovePath.STEP_LATERAL_LEFT_BACKWARDS));
            }
        }
        if (forward
                && (!backward || ((last == null) || (last.getType() != MovePath.STEP_BACKWARDS)))) {
            result.add(clone().addStep(MovePath.STEP_FORWARDS));
        }
        if ((last == null) || (last.getType() != MovePath.STEP_TURN_LEFT)) {
            result.add(clone().addStep(MovePath.STEP_TURN_RIGHT));
        }
        if ((last == null) || (last.getType() != MovePath.STEP_TURN_RIGHT)) {
            result.add(clone().addStep(MovePath.STEP_TURN_LEFT));
        }
        if (backward
                && (!forward || ((last == null) || (last.getType() != MovePath.STEP_FORWARDS)))) {
            result.add(clone().addStep(MovePath.STEP_BACKWARDS));
        }
        return result;
    }

    /**
     * Clones this path, will contain a new clone of the steps so that the clone
     * is independent from the original.
     *
     * @return the cloned MovePath
     */
    @Override
    public MovePath clone() {
        final MovePath copy = new MovePath(game, entity);
        copy.steps = new Vector<MoveStep>(steps);
        copy.careful = careful;
        return copy;
    }

    /**
     * Rotate from the current facing to the destination facing.
     */
    public void rotatePathfinder(final int destFacing, final boolean isManeuver) {
        while (getFinalFacing() != destFacing) {
            final int stepType = getDirection(getFinalFacing(), destFacing);
            addStep(stepType, isManeuver, isManeuver);
        }
    }

    protected static class MovePathComparator implements Comparator<MovePath> {
        private final Coords destination;
        boolean backward;

        public MovePathComparator(final Coords destination,
                final boolean backward) {
            this.destination = destination;
            this.backward = backward;
        }

        public int compare(final MovePath first, final MovePath second) {
            final int firstDist = first.getMpUsed()
                    + first.getFinalCoords().distance(destination)
                    + getFacingDiff(first);
            final int secondDist = second.getMpUsed()
                    + second.getFinalCoords().distance(destination)
                    + getFacingDiff(second);
            return firstDist - secondDist;
        }

        private int getFacingDiff(final MovePath first) {
            if (first.isJumping()) {
                return 0;
            }
            int firstFacing = Math.abs((first.getFinalCoords().direction(
                    destination) + (backward ? 3 : 0))
                    % 6 - first.getFinalFacing());
            if (firstFacing > 3) {
                firstFacing = 6 - firstFacing;
            }
            if (first.canShift()) {
                firstFacing = Math.max(0, firstFacing - 1);
            }
            return firstFacing;
        }
    }

    /*
     * Get the position in the step immediately prior to the final position
     */
    public Coords getSecondFinalPosition(Coords startPos) {

        Coords priorPos = startPos;
        Coords finalPos = getFinalCoords();

        //if we moved one or fewer hexes, then just return starting position
        if(getHexesMoved() < 2) {
            return priorPos;
        }

         for (final Enumeration<MoveStep> i = getSteps(); i.hasMoreElements();) {
             final MoveStep step = i.nextElement();
             if(step.getPosition() != finalPos) {
                 priorPos = step.getPosition();
             }
         }
         return priorPos;

    }

    public boolean isCareful() {
        return careful;
    }

    public void setCareful(boolean b) {
        careful = b;
    }
}
