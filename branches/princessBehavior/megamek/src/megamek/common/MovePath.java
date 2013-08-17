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

    public enum MoveStepType {
        NONE, FORWARDS, BACKWARDS, TURN_LEFT, TURN_RIGHT, GET_UP, GO_PRONE, START_JUMP, CHARGE, DFA, FLEE, LATERAL_LEFT, LATERAL_RIGHT, LATERAL_LEFT_BACKWARDS, LATERAL_RIGHT_BACKWARDS, UNJAM_RAC, LOAD, UNLOAD, EJECT, CLEAR_MINEFIELD, UP, DOWN, SEARCHLIGHT, LAY_MINE, HULL_DOWN, CLIMB_MODE_ON, CLIMB_MODE_OFF, SWIM, DIG_IN, FORTIFY, SHAKE_OFF_SWARMERS, TAKEOFF, VTAKEOFF, LAND, ACC, DEC, EVADE, SHUTDOWN, STARTUP, SELF_DESTRUCT, ACCN, DECN, ROLL, OFF, RETURN, LAUNCH, THRUST, YAW, CRASH, RECOVER, RAM, HOVER, MANEUVER, LOOP, CAREFUL_STAND, JOIN, DROP, VLAND, MOUNT, UNDOCK;
    }

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
            if (!(obj instanceof Key)) {
                return false;
            }
            final Key s1 = (Key) obj;
            return (type == s1.type) && (facing == s1.facing) && coords.equals(s1.coords);
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

    // is this move path being done using careful movement?
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
        return new Key(getFinalCoords(), getFinalFacing(), getFinalProne() ? 0 : isJumping() ? 1 : 2);
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer();
        for (final Enumeration<MoveStep> i = steps.elements(); i.hasMoreElements();) {
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
     * @param type
     *            the type of movement.
     */
    public MovePath addStep(final MoveStepType type) {
        // TODO : detect steps off the map *here*.
        return addStep(new MoveStep(this, type));
    }

    /**
     * Add a new step to the movement path with the given target.
     *
     * @param type
     *            the type of movement.
     * @param target
     *            the <code>Targetable</code> object that is the target of this
     *            step. For example, the enemy being charged.
     */
    public MovePath addStep(final MoveStepType type, final Targetable target) {
        return addStep(new MoveStep(this, type, target));
    }
    
    public MovePath addStep(final MoveStepType type, final Targetable target, final Coords pos) {
        return addStep(new MoveStep(this, type, target, pos));
    }

    public MovePath addStep(final MoveStepType type, final int mineToLay) {
        return addStep(type, -1, mineToLay);
    }

    public MovePath addStep(final MoveStepType type, final int recover, final int mineToLay) {
        return addStep(new MoveStep(this, type, recover, mineToLay));
    }

    public MovePath addStep(MoveStepType type, TreeMap<Integer, Vector<Integer>> targets) {
        return addStep(new MoveStep(this, type, targets));
    }

    public MovePath addStep(final MoveStepType type, final boolean noCost) {
        return addStep(new MoveStep(this, type, noCost));
    }

    public MovePath addStep(final MoveStepType type, final boolean noCost, final boolean isManeuver) {
        return addStep(new MoveStep(this, type, noCost, isManeuver));
    }

    public MovePath addStep(final MoveStepType type, final Minefield mf) {
        return addStep(new MoveStep(this, type, mf));
    }

    public MovePath addManeuver(final int manType) {
        return addStep(new MoveStep(this, MoveStepType.MANEUVER, -1, -1, manType));
    }

    public boolean canShift() {
        return ((entity instanceof QuadMech) || entity.isUsingManAce()) && !isJumping();
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
            step.setMovementType(EntityMovementType.MOVE_ILLEGAL);
        }

        // check for illegal jumps
        final Coords start = entity.getPosition();
        final Coords land = step.getPosition();
        if (start == null || land == null) { 
            // If we have null for either coordinate then we know the step
            // isn't legal.
            step.setMovementType(EntityMovementType.MOVE_ILLEGAL);
        } else {
            final int distance = start.distance(land);
            if (isJumping() && entity.getJumpType() != Mech.JUMP_BOOSTER) {
                if (step.isThisStepBackwards() || step.getMpUsed() > distance) {
                    step.setMovementType(EntityMovementType.MOVE_ILLEGAL);
                }
            }
        }
        
        if (shouldMechanicalJumpCauseFallDamage()){
            step.setDanger(true);
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
            if(step.getTargetPosition() != null && step.getTarget(game) != null) {
                step = new MoveStep(this, step.getType(), step.getTarget(game), step.getTargetPosition());
            } else if (step.getTarget(game) != null) {
                step = new MoveStep(this, step.getType(), step.getTarget(game));
            } else if (step.getRecoveryUnit() != -1) {
                step = new MoveStep(this, step.getType(), step.getRecoveryUnit(), -1);
            } else if (step.getMineToLay() != -1) {
                step = new MoveStep(this, step.getType(), step.getMineToLay());
            } else if (step.getLaunched().size() > 0) {
                step = new MoveStep(this, step.getType(), step.getLaunched());
            } else if (step.getManeuverType() != ManeuverType.MAN_NONE) {
                step = new MoveStep(this, step.getType(), -1, -1, step.getManeuverType());
            } else if (step.isManeuver()) {
                step = new MoveStep(this, step.getType(), step.hasNoCost(), step.isManeuver());
            } else if (step.hasNoCost()) {
                step = new MoveStep(this, step.getType(), step.hasNoCost());
            } else if (null != step.getMinefield()) {
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
            final MoveStep step1 = getStep(steps.size() - 1);

            if(step1.getType() == MovePath.MoveStepType.START_JUMP) {
                entity.setIsJumpingNow(false);
            }

            steps.removeElementAt(steps.size() - 1);
        }

        // Find the new last step in the path.
        int index = steps.size() - 1;
        while ((index >= 0) && getStep(index).setEndPos(true) && !getStep(index).isLegal()) {
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
    public boolean contains(final MoveStepType type) {
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
     * Returns whether or not a unit would end up hull-down after all of the steps
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
    
    /**
     * Returns the highest elevation in the current path
     * @return
     */
    public int getMaxElevation(){
        int maxElev = 0;
        for (MoveStep step : steps){
            maxElev = Math.max(maxElev, 
                    game.getBoard().getHex(step.getPosition()).getElevation());
        }
        return maxElev;
    }

    /**
     * get final altitude
     */
    public int getFinalAltitude() {
        if (getLastStep() != null) {
            return getLastStep().getAltitude();
        }
        return entity.getAltitude();
    }

    public int getFinalVelocity() {
        if (getLastStep() != null) {
            return getLastStep().getVelocity();
        }
        if (entity instanceof Aero) {
            return ((Aero) entity).getCurrentVelocity();
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

    public EntityMovementType getLastStepMovementType() {
        if (getLastStep() == null) {
            return EntityMovementType.MOVE_NONE;
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
            System.out.println("  " + i + ": " + getStep(i) + ", " + getStep(i).getMovementType());
        }
    }

    /**
     * Removes impossible steps.
     */
    public void clipToPossible() {
        if (steps.size() == 0) {
            // nothing to clip
            return;
        }
        // Do a final check for bad moves, and clip movement after the first bad
        // one
        final Vector<MoveStep> goodSteps = new Vector<MoveStep>();
        Enumeration<MoveStep> i = steps.elements();
        MoveStep step = i.nextElement();

        // Can't move out of a hex with an enemy unit unless we started
        // there, BUT we're allowed to turn, unload, or go prone.
        if (Compute.isEnemyIn(game, entity, entity.getPosition(), false, entity instanceof Mech, entity.getElevation())) {
            // This is an enemy, we can't go out and back in, and go out again
            boolean left = false;
            boolean returned = false;
            while (i.hasMoreElements()) {
                step = i.nextElement();
                if (!left) {
                    if (!step.getPosition().equals(entity.getPosition())
                            || !(step.getElevation() == entity.getElevation())) {
                        // we left the location
                        left = true;
                        continue;
                    }
                    continue;
                }
                if (!returned) {
                    if (step.getPosition().equals(entity.getPosition())
                            && (step.getElevation() == entity.getElevation())) {
                        // we returned to the location
                        returned = true;
                        continue;
                    }
                    continue;
                }
                // we've returned, anything other than the following 4 types are
                // illegal
                if ((step.getType() != MovePath.MoveStepType.TURN_LEFT)
                        && (step.getType() != MovePath.MoveStepType.TURN_RIGHT)
                        && (step.getType() != MovePath.MoveStepType.UNLOAD)
                        && (step.getType() != MovePath.MoveStepType.GO_PRONE)) {
                    // we only need to identify the first illegal move
                    step.setMovementType(EntityMovementType.MOVE_ILLEGAL);
                    break;
                }
            }
        }
        i = steps.elements();
        while (i.hasMoreElements()) {
            step = i.nextElement();
            if (step.getMovementType() != EntityMovementType.MOVE_ILLEGAL) {
                goodSteps.addElement(step);
            } else {
                break;
            }

        }
        steps = goodSteps;
    }

    /**
     * Changes turn-forwards-opposite-turn sequences into quad lateral shifts.
     * <p/>
     * Finds the sequence of three steps that can be transformed, then removes
     * all three and replaces them with the lateral shift step.
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
                && ((step2.getType() == MovePath.MoveStepType.BACKWARDS) || (step2.getType() == MovePath.MoveStepType.FORWARDS))) {
            final MoveStepType stepType = step1.getType();
            final MoveStepType direction = step2.getType();
            // remove all old steps
            steps.removeElementAt(index);
            steps.removeElementAt(index);
            steps.removeElementAt(index);
            // add new step
            final MoveStep shift = new MoveStep(this, lateralShiftForTurn(stepType, direction));
            addStep(shift);
        }
    }

    /**
     * Returns the lateral shift that corresponds to the turn direction
     */
    public static MoveStepType lateralShiftForTurn(final MoveStepType turn, final MoveStepType direction) {
        if (direction == MoveStepType.FORWARDS) {
            switch (turn) {
            case TURN_LEFT:
                return MoveStepType.LATERAL_LEFT;
            case TURN_RIGHT:
                return MoveStepType.LATERAL_RIGHT;
            default:
                return turn;
            }
        }
        switch (turn) {
        case TURN_LEFT:
            return MoveStepType.LATERAL_LEFT_BACKWARDS;
        case TURN_RIGHT:
            return MoveStepType.LATERAL_RIGHT_BACKWARDS;
        default:
            return turn;
        }
    }

    /**
     * Returns the turn direction that corresponds to the lateral shift
     */
    static MoveStepType turnForLateralShift(final MoveStepType shift) {
        switch (shift) {
        case LATERAL_LEFT:
            return MoveStepType.TURN_LEFT;
        case LATERAL_RIGHT:
            return MoveStepType.TURN_RIGHT;
        case LATERAL_LEFT_BACKWARDS:
            return MoveStepType.TURN_LEFT;
        case LATERAL_RIGHT_BACKWARDS:
            return MoveStepType.TURN_RIGHT;
        default:
            return shift;
        }
    }

    /**
     * Returns the direction (either MovePath.MoveStepType.TURN_LEFT or
     * MoveStepType.TURN_RIGHT) that the destination facing lies in.
     */
    public static MoveStepType getDirection(final int facing, final int destFacing) {
        final int rotate = (destFacing + (6 - facing)) % 6;
        return rotate >= 3 ? MoveStepType.TURN_LEFT : MoveStepType.TURN_RIGHT;
    }

    /**
     * Returns the adjusted facing, given the start facing.
     */
    public static int getAdjustedFacing(final int facing, final MoveStepType movement) {
        if (movement == MoveStepType.TURN_RIGHT) {
            return (facing + 1) % 6;
        } else if (movement == MoveStepType.TURN_LEFT) {
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
                if (step.getType() == MovePath.MoveStepType.START_JUMP) {
                    jump = true;
                }
            }
            return jump;
        }
        return false;
    }
    
    /**
     * Extend the current path to the destination <code>Coords</code>, moving 
     * only in one direction.  This method works by applying the supplied move
     * step as long as it moves closer to the destination.  If the destination
     * cannot be reached solely by the provided movestep, the pathfinder will
     * quit once it gets as closer as it can.
     *
     * @param dest
     *            the destination <code>Coords</code> of the move.
     * @param type
     *            the type of movement step required.
     *  @param direction
     *            the direction of movement.
     */
    public void findSimplePathTo(final Coords dest, final MoveStepType type, 
            int direction, int facing) {
        Coords src = this.getFinalCoords();
        Coords currStep = src;
        Coords nextStep = currStep.translated(direction);
        while (dest.distance(nextStep) < dest.distance(currStep)){
            addStep(type);
            currStep = nextStep;
            nextStep = currStep.translated(direction);
        } 
        
        // Did we reach the destination?  If not, try another direction
        if (!currStep.equals(dest)){           
            int dir = currStep.direction(dest);
            // Java does mod different from how we want... 
            dir = ((dir - facing) % 6 + 6) % 6;
            switch (dir)
            {
                case 0:
                    findSimplePathTo(dest, MoveStepType.FORWARDS,currStep.direction(dest),facing);
                    break;
                case 1:
                    findSimplePathTo(dest, MoveStepType.LATERAL_RIGHT,currStep.direction(dest),facing);
                    break;
                case 2:
                    // TODO: backwards lateral shifts are switched: 
                    //  LATERAL_LEFT_BACKWARDS moves back+right and vice-versa
                    findSimplePathTo(dest, MoveStepType.LATERAL_LEFT_BACKWARDS,currStep.direction(dest),facing);
                    break;
                case 3:
                    findSimplePathTo(dest, MoveStepType.BACKWARDS,currStep.direction(dest),facing);
                    break;                    
                case 4:
                    // TODO: backwards lateral shifts are switched: 
                    //  LATERAL_LEFT_BACKWARDS moves back+right and vice-versa                    
                    findSimplePathTo(dest, MoveStepType.LATERAL_RIGHT_BACKWARDS,currStep.direction(dest),facing);
                    break;
                case 5:
                    findSimplePathTo(dest, MoveStepType.LATERAL_LEFT,currStep.direction(dest),facing);
                    break;                    
            }
        }
    }

    /**
     * Extend the current path to the destination <code>Coords</code>.
     *
     * @param dest
     *            the destination <code>Coords</code> of the move.
     * @param type
     *            the type of movment step required.
     */
    public void findPathTo(final Coords dest, final MoveStepType type) {
        final int timeLimit = PreferenceManager.getClientPreferences().getMaxPathfinderTime();

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

        // for aero units move must use up all their velocity
        if (entity instanceof Aero) {
            Aero a = (Aero) entity;
            if (getLastStep() == null) {
                if ((a.getCurrentVelocity() > 0) && !game.useVectorMove()) {
                    return false;
                }
            } else {
                if ((getLastStep().getVelocityLeft() > 0) && !game.useVectorMove()
                        && (getLastStep().getType() != MovePath.MoveStepType.FLEE)) {
                    return false;
                }
            }
        }

        if (getLastStep() == null) {
            return true;
        }

        if (getLastStep().getType() == MoveStepType.CHARGE) {
            return getSecondLastStep().isLegal();
        }
        if (getLastStep().getType() == MoveStepType.RAM) {
            return getSecondLastStep().isLegal();
        }
        return getLastStep().isLegal();
    }

    /**
     * An A* pathfinder to get from the end of the current path (or entity's
     * position if empty) to the destination.
     *
     * @param dest
     *            The goal hex
     * @param type
     *            The type of move we want to do
     * @param timeLimit
     *            the maximum <code>int</code> number of milliseconds to take
     *            hunting for an ideal path.
     */
    private void notSoLazyPathfinder(final Coords dest, final MoveStepType type, final int timeLimit) {
        final long endTime = System.currentTimeMillis() + timeLimit;

        MoveStepType step = type;
        if (step != MoveStepType.BACKWARDS) {
            step = MoveStepType.FORWARDS;
        }

        final MovePathComparator mpc = new MovePathComparator(dest, step == MovePath.MoveStepType.BACKWARDS);

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

            final Iterator<MovePath> adjacent = candidatePath.getNextMoves(step == MoveStepType.BACKWARDS,
                    step == MoveStepType.FORWARDS).iterator();
            while (adjacent.hasNext()) {
                final MovePath expandedPath = adjacent.next();

                if (expandedPath.getLastStep().isMovementPossible(game, startingPos, startingElev)) {
                    final MovePath found = discovered.get(expandedPath.getKey());
                    if ((found != null) && (mpc.compare(found, expandedPath) <= 0)) {
                        continue;
                    }
                    int index = Collections.<MovePath> binarySearch(candidates, expandedPath, mpc);
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
                if (front.getFinalCoords().distance(dest) < bestPath.getFinalCoords().distance(dest)) {
                    bestPath = front;
                    keepLooping = System.currentTimeMillis() < endTime;
                } else {
                    keepLooping = false;
                }
            }
        } // end while

        if (getFinalCoords().distance(dest) > bestPath.getFinalCoords().distance(dest)) {
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
     * @param dest
     *            the destination <code>Coords</code> of the move.
     * @param type
     *            the type of movment step required.
     */
    private void lazyPathfinder(final Coords dest, final MoveStepType type) {
        MoveStepType step = MoveStepType.FORWARDS;
        if (type == MoveStepType.BACKWARDS) {
            step = MoveStepType.BACKWARDS;
        }
        Coords subDest = dest;
        if (!dest.equals(getFinalCoords())) {
            subDest = dest.translated(dest.direction(getFinalCoords()));
        }

        while (!getFinalCoords().equals(subDest)) {
            // adjust facing
            rotatePathfinder((getFinalCoords().direction(subDest) + (step == MoveStepType.BACKWARDS ? 3 : 0)) % 6,
                    false);
            // step forwards
            addStep(step);
        }
        rotatePathfinder((getFinalCoords().direction(dest) + (step == MoveStepType.BACKWARDS ? 3 : 0)) % 6, false);
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
            result.add(clone().addStep(MovePath.MoveStepType.FORWARDS));
            for (int turn = 0; turn < 2; turn++) {
                left.addStep(MovePath.MoveStepType.TURN_LEFT);
                right.addStep(MovePath.MoveStepType.TURN_RIGHT);
                result.add(left.clone().addStep(MovePath.MoveStepType.FORWARDS));
                result.add(right.clone().addStep(MovePath.MoveStepType.FORWARDS));
            }
            right.addStep(MovePath.MoveStepType.TURN_RIGHT);
            result.add(right.addStep(MovePath.MoveStepType.FORWARDS));

            // We've got all our next steps.
            return result;
        }

        // need to do a separate section here for Aeros.
        // just like jumping for now, but I could add some other stuff
        // here later
        if (entity instanceof Aero) {
            MovePath left = clone();
            MovePath right = clone();

            // From here, we can move F, LF, RF, LLF, RRF, and RRRF.
            result.add((clone()).addStep(MovePath.MoveStepType.FORWARDS));
            for (int turn = 0; turn < 2; turn++) {
                left.addStep(MovePath.MoveStepType.TURN_LEFT);
                right.addStep(MovePath.MoveStepType.TURN_RIGHT);
                result.add(left.clone().addStep(MovePath.MoveStepType.FORWARDS));
                result.add(right.clone().addStep(MovePath.MoveStepType.FORWARDS));
            }
            right.addStep(MovePath.MoveStepType.TURN_RIGHT);
            result.add(right.addStep(MovePath.MoveStepType.FORWARDS));

            // We've got all our next steps.
            return result;
        }

        // If the unit is prone or hull-down it limits movement options, unless
        //  it's a tank; tanks can just drive out of hull-down and they cannot 
        //  be prone.
        if (getFinalProne() || getFinalHullDown() && !(entity instanceof Tank)) {
            if ((last != null) && (last.getType() != MoveStepType.TURN_RIGHT)) {
                result.add(clone().addStep(MovePath.MoveStepType.TURN_LEFT));
            }
            if ((last != null) && (last.getType() != MoveStepType.TURN_LEFT)) {
                result.add(clone().addStep(MovePath.MoveStepType.TURN_RIGHT));
            }

            if (entity.isCarefulStand()) {
                result.add(clone().addStep(MovePath.MoveStepType.CAREFUL_STAND));
            } else {
                result.add(clone().addStep(MovePath.MoveStepType.GET_UP));
            }
            return result;
        }
        if (canShift()) {
            if (forward && (!backward || ((last == null) || (last.getType() != MovePath.MoveStepType.LATERAL_LEFT)))) {
                result.add(clone().addStep(MoveStepType.LATERAL_RIGHT));
            }
            if (forward && (!backward || ((last == null) || (last.getType() != MovePath.MoveStepType.LATERAL_RIGHT)))) {
                result.add(clone().addStep(MovePath.MoveStepType.LATERAL_LEFT));
            }
            if (backward
                    && (!forward || ((last == null) || (last.getType() != MovePath.MoveStepType.LATERAL_LEFT_BACKWARDS)))) {
                result.add(clone().addStep(MovePath.MoveStepType.LATERAL_RIGHT_BACKWARDS));
            }
            if (backward
                    && (!forward || ((last == null) || (last.getType() != MovePath.MoveStepType.LATERAL_RIGHT_BACKWARDS)))) {
                result.add(clone().addStep(MovePath.MoveStepType.LATERAL_LEFT_BACKWARDS));
            }
        }
        if (forward && (!backward || ((last == null) || (last.getType() != MovePath.MoveStepType.BACKWARDS)))) {
            result.add(clone().addStep(MovePath.MoveStepType.FORWARDS));
        }
        if ((last == null) || (last.getType() != MovePath.MoveStepType.TURN_LEFT)) {
            result.add(clone().addStep(MovePath.MoveStepType.TURN_RIGHT));
        }
        if ((last == null) || (last.getType() != MovePath.MoveStepType.TURN_RIGHT)) {
            result.add(clone().addStep(MovePath.MoveStepType.TURN_LEFT));
        }
        if (backward && (!forward || ((last == null) || (last.getType() != MovePath.MoveStepType.FORWARDS)))) {
            result.add(clone().addStep(MovePath.MoveStepType.BACKWARDS));
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
            final MoveStepType stepType = getDirection(getFinalFacing(), destFacing);
            addStep(stepType, isManeuver, isManeuver);
        }
    }
    
    /**
     * Returns true if a jump using mechanical jump boosters would cause falling
     * damage.  Mechanical jump boosters are only designed to handle the stress
     * of falls from a height equal to their jumpMP; if a jump has a fall that
     * is further than the jumpMP of the unit, fall damage applies.
     * @return
     */
    public boolean shouldMechanicalJumpCauseFallDamage(){
        if (isJumping() && entity.getJumpType() == Mech.JUMP_BOOSTER && 
                getJumpMaxElevationChange() > entity.getJumpMP()){
          return true;  
        }
        return false;
    }
    
    /**
     * Returns the highest elevation along a jump path.
     * @return
     */
    public Coords getJumpPathHighestPoint(){
        Coords highestCoords = null;
        int highestElevation = 0;
        for (MoveStep step : steps){
            if (game.getBoard().getHex(step.getPosition()).getElevation() > highestElevation) {
                highestElevation = step.getElevation();
                highestCoords = step.getPosition();
            }
        }
        return highestCoords;
    }
    /**
     * Returns the distance between the highest elevation in the jump path and
     * the elevation at the landing point.  This gives the largest distance the
     * unit has fallen during the jump.
     * 
     */
    public int getJumpMaxElevationChange(){
        return getMaxElevation() - 
                game.getBoard().getHex(getFinalCoords()).getElevation(); 
    }

    protected static class MovePathComparator implements Comparator<MovePath> {
        private final Coords destination;
        boolean backward;

        public MovePathComparator(final Coords destination, final boolean backward) {
            this.destination = destination;
            this.backward = backward;
        }

        public int compare(final MovePath first, final MovePath second) {
            final int firstDist = first.getMpUsed() + first.getFinalCoords().distance(destination)
                    + getFacingDiff(first);
            final int secondDist = second.getMpUsed() + second.getFinalCoords().distance(destination)
                    + getFacingDiff(second);
            return firstDist - secondDist;
        }

        private int getFacingDiff(final MovePath first) {
            if (first.isJumping()) {
                return 0;
            }
            int firstFacing = Math.abs((first.getFinalCoords().direction(destination) + (backward ? 3 : 0)) % 6
                    - first.getFinalFacing());
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

        // if we moved one or fewer hexes, then just return starting position
        if (getHexesMoved() < 2) {
            return priorPos;
        }

        for (final Enumeration<MoveStep> i = getSteps(); i.hasMoreElements();) {
            final MoveStep step = i.nextElement();
            if (step.getPosition() != finalPos) {
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
