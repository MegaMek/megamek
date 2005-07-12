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

import com.sun.java.util.collections.Comparator;
import com.sun.java.util.collections.ArrayList;
import com.sun.java.util.collections.Collections;
import com.sun.java.util.collections.HashMap;
import com.sun.java.util.collections.Iterator;
import com.sun.java.util.collections.List;

import java.io.Serializable;
import java.util.Enumeration;
import java.util.Vector;

import megamek.common.preference.PreferenceManager;

/**
 * Holds movement path for an entity.
 */
public class MovePath implements Cloneable, Serializable {
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

    public static class Key {
        private Coords coords;
        private int facing;
        private int type;

        public Key(Coords coords, int facing, int type) {
            this.coords = coords;
            this.facing = facing;
            this.type = type;
        }

        public boolean equals(Object obj) {
            Key s1 = (Key) obj;
            if (s1 != null) {
                return type == type && facing == s1.facing && coords.equals(s1.coords);
            }
            return false;
        }

        public int hashCode() {
            return type + 7 * (facing + 31 * coords.hashCode());
        }
    }

    protected Vector steps = new Vector();

    protected transient IGame game;
    protected transient Entity entity;

    public static final int DEFAULT_PATHFINDER_TIME_LIMIT = 2000;

    /**
     * Generates a new, empty, movement path object.
     */
    public MovePath(IGame game, Entity entity) {
        this.entity = entity;
        this.game = game;
    }

    public Entity getEntity() {
        return entity;
    }

    public Key getKey() {
        return new Key(getFinalCoords(), getFinalFacing(), getFinalProne()?0:isJumping()?1:2);
    }


    public String toString() {
        StringBuffer sb = new StringBuffer();
        for (Enumeration i = steps.elements(); i.hasMoreElements();) {
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
    public MovePath addStep(int type) {
        // TODO : detect steps off the map *here*.
        return addStep( new MoveStep( this, type ) );
    }

    /**
     * Add a new step to the movement path with the given target.
     *
     * @param type   the type of movement.
     * @param target the <code>Targetable</code> object that is the target of
     *               this step. For example, the enemy being charged.
     */
    public MovePath addStep(int type, Targetable target) {
        return addStep(new MoveStep(this, type, target));
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
    protected MovePath addStep(MoveStep step) {
        steps.addElement(step);

        // transform lateral shifts for quads or maneuverability aces
        if (canShift()) {
            transformLateralShift();
        }
        MoveStep prev = getStep(steps.size() - 2);

        try {
            step.compile(game, entity, prev);
        } catch (RuntimeException re) {
//             // N.B. the pathfinding will try steps off the map.
//             re.printStackTrace();
            step.setMovementType(IEntityMovementType.MOVE_ILLEGAL);
        }

        // check for illegal jumps
        Coords start = entity.getPosition();
        Coords land = step.getPosition();
        int distance = start.distance(land);
        if (isJumping()) {
            if (step.getMpUsed() > distance) {
                step.setMovementType(IEntityMovementType.MOVE_ILLEGAL);
            }
        }

        // If the new step is legal and is a different position than
        // the previous step, then update the older steps, letting
        // them know that they are no longer the end of the path.
        if ( step.isLegal() && null != prev
             && !land.equals( prev.getPosition() ) ) {

            // Loop through the steps from back to front.
            // Stop looping when the step says to, or we run out of steps.
            int index = steps.size() - 2;
            while ( index >= 0 && getStep( index ).setEndPos( false ) )
                index--;

        } // End step-is-legal

        return this;
    }

    public void compile(IGame g, Entity en) {
        this.game = g;
        this.entity = en;
        Vector temp = (Vector) steps.clone();
        steps.removeAllElements();
        for (int i = 0; i < temp.size(); i++) {
            MoveStep step = (MoveStep) temp.elementAt(i);
            if (step.getTarget(game) != null) {
                step = new MoveStep(this, step.getType(), step.getTarget(game));
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
        while ( index >= 0
                && getStep( index ).setEndPos( true )
                && !getStep( index ).isLegal() )
            index--;
    }

    public void clear() {
        steps.removeAllElements();
    }

    public Enumeration getSteps() {
        return steps.elements();
    }

    public MoveStep getStep(int index) {
        if (index < 0 || index >= steps.size()) {
            return null;
        }
        return (MoveStep) steps.elementAt(index);
    }

    /**
     * Check for any of the specified type of step in the path
     */
    public boolean contains(int type) {
        for (final Enumeration i = getSteps(); i.hasMoreElements();) {
            MoveStep step = (MoveStep) i.nextElement();
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
        for (final Enumeration i = getSteps(); i.hasMoreElements();) {
            MoveStep step = (MoveStep) i.nextElement();
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
        if (entity == null)
            return false;
        return entity.isProne();
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

    public int getLastStepMovementType() {
        if (getLastStep() == null) {
            return IEntityMovementType.MOVE_NONE;
        }
        return getLastStep().getMovementType();
    }

    public MoveStep getLastStep() {
        return getStep(steps.size() - 1);
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
        // hopefully there's no impossible steps in the middle of possible ones
        Vector goodSteps = new Vector();
        for (final Enumeration i = steps.elements(); i.hasMoreElements();) {
            final MoveStep step = (MoveStep) i.nextElement();
            if (step.getMovementType() != IEntityMovementType.MOVE_ILLEGAL) {
                goodSteps.addElement(step);
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
        int index = steps.size() - 3;
        MoveStep step1 = getStep(index);
        MoveStep step2 = getStep(index + 1);
        MoveStep step3 = getStep(index + 2);

        if (step1.oppositeTurn(step3)
            && (step2.getType() == MovePath.STEP_BACKWARDS || step2.getType() == MovePath.STEP_FORWARDS)) {
            int stepType = step1.getType();
            int direction = step2.getType();
            // remove all old steps
            steps.removeElementAt(index);
            steps.removeElementAt(index);
            steps.removeElementAt(index);
            // add new step
            MoveStep shift = new MoveStep(this, lateralShiftForTurn(stepType, direction));
            addStep(shift);
        }
    }

    /**
     * Returns the lateral shift that corresponds to the turn direction
     */
    public static int lateralShiftForTurn(int turn, int direction) {
        if (direction == MovePath.STEP_FORWARDS) {
            switch (turn) {
                case MovePath.STEP_TURN_LEFT :
                    return MovePath.STEP_LATERAL_LEFT;
                case MovePath.STEP_TURN_RIGHT :
                    return MovePath.STEP_LATERAL_RIGHT;
                default :
                    return turn;
            }
        } else {
            switch (turn) {
                case MovePath.STEP_TURN_LEFT :
                    return MovePath.STEP_LATERAL_LEFT_BACKWARDS;
                case MovePath.STEP_TURN_RIGHT :
                    return MovePath.STEP_LATERAL_RIGHT_BACKWARDS;
                default :
                    return turn;
            }
        }
    }

    /**
     * Returns the turn direction that corresponds to the lateral shift
     */
    static int turnForLateralShift(int shift) {
        switch (shift) {
            case MovePath.STEP_LATERAL_LEFT :
                return MovePath.STEP_TURN_LEFT;
            case MovePath.STEP_LATERAL_RIGHT :
                return MovePath.STEP_TURN_RIGHT;
            case MovePath.STEP_LATERAL_LEFT_BACKWARDS :
                return MovePath.STEP_TURN_LEFT;
            case MovePath.STEP_LATERAL_RIGHT_BACKWARDS :
                return MovePath.STEP_TURN_RIGHT;
            default :
                return shift;
        }
    }

    /**
     * Returns the direction (either MovePath.STEP_TURN_LEFT or
     * STEP_TURN_RIGHT) that the destination facing lies in.
     */
    public static int getDirection(int facing, int destFacing) {
        final int rotate = (destFacing + (6 - facing)) % 6;
        return rotate >= 3 ? STEP_TURN_LEFT : STEP_TURN_RIGHT;
    }

    /**
     * Returns the adjusted facing, given the start facing.
     */
    public static int getAdjustedFacing(int facing, int movement) {
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
     * Returns the logical number of hexes moved
     * the path (does not count turns, etc).
     */
    public int getHexesMoved() {
        if (getLastStep() == null) {
            return 0;
        }
        return getLastStep().getDistance();
    }

    public boolean isJumping() {
        if (steps.size() > 0) {
            return getStep(0).getType() == MovePath.STEP_START_JUMP;
        }
        return false;
    }

    /**
     * Extend the current path to the destination <code>Coords</code>.
     *
     * @param   dest the destination <code>Coords</code> of the move.
     * @param   type the type of movment step required.
     */
    public void findPathTo(Coords dest, int type) {
        int timeLimit = PreferenceManager.getClientPreferences().getMaxPathfinderTime();

        if (timeLimit >= 5000) {
            System.out.print("WARNING!!!  Settings allow up to ");
            System.out.print(timeLimit);
            System.out.println(" milliseconds to find the optimum path!");
        }
        this.notSoLazyPathfinder(dest, type, timeLimit);
    }

    public boolean isMoveLegal() {
        // Moves which end up off of the board are not legal.
        if (!game.getBoard().contains(getFinalCoords())) {
            return false;
        }

        if (getLastStep() == null) {
            return true;
        }

        return getLastStep().isLegal();
    }

    /**
     * An A* pathfinder to get from the end of the current path
     * (or entity's position if empty) to the destination.
     *
     * @param dest The goal hex
     * @param type The type of move we want to do
     * @param timeLimit the maximum <code>int</code> number of
     *          milliseconds to take hunting for an ideal path.
     */
    private void notSoLazyPathfinder(final Coords dest, final int type, final int timeLimit) {
        long endTime = System.currentTimeMillis() + timeLimit;

        int step = type;
        if (step != MovePath.STEP_BACKWARDS) {
            step = MovePath.STEP_FORWARDS;
        }

        MovePathComparator mpc = new MovePathComparator(dest, step == MovePath.STEP_BACKWARDS);

        MovePath bestPath = (MovePath) this.clone();

        HashMap discovered = new HashMap();
        discovered.put(bestPath.getKey(), bestPath);

        ArrayList candidates = new ArrayList();
        candidates.add(bestPath);

        boolean keepLooping = this.getFinalCoords().distance(dest) > 1;
        int loopcount = 0;

        while (candidates.size() > 0 && keepLooping) {
            MovePath candidatePath = (MovePath) candidates.remove(0);
            Coords startingPos = candidatePath.getFinalCoords();
            
            if (candidatePath.getFinalCoords().distance(dest) == 1) {
                bestPath = candidatePath;
                keepLooping = false;
                break;
            }

            Iterator adjacent = candidatePath.getNextMoves(step == STEP_BACKWARDS, step == STEP_FORWARDS).iterator();
            while (adjacent.hasNext()) {
                MovePath expandedPath = (MovePath) adjacent.next();

                if (expandedPath.getLastStep().isMovementPossible(this.game, startingPos)) {
                    MovePath found = (MovePath) discovered.get(expandedPath.getKey());
                    if (found != null && mpc.compare(found, expandedPath) <= 0) {
                        continue;
                    }
                    int index = Collections.binarySearch(candidates, expandedPath, mpc);
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
            if (loopcount % 256 == 0 && keepLooping && candidates.size() > 0) {
                MovePath front = (MovePath)candidates.get(0);
                if (front.getFinalCoords().distance(dest) < bestPath.getFinalCoords().distance(dest)) {
                    bestPath = front;
                    keepLooping = System.currentTimeMillis() < endTime;
                } else {
                    keepLooping = false;
                }
            }
        } //end while

        if (getFinalCoords().distance(dest) > bestPath.getFinalCoords().distance(dest)) {
            //Make the path we found, this path.
            this.steps = bestPath.steps;
        }
        if (!getFinalCoords().equals(dest)) {
            lazyPathfinder(dest, type);
        }
    }
    
    /**
     * Find the shortest path to the destination <code>Coords</code> by
     * hex count.  This right choice <em>only</em> when making a simple
     * move like a straight line or one with a single turn.
     *
     * @param   dest the destination <code>Coords</code> of the move.
     * @param   type the type of movment step required.
     */
    private void lazyPathfinder(Coords dest, int type) {
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
            rotatePathfinder((getFinalCoords().direction(subDest)
                              + (step == STEP_BACKWARDS ? 3 : 0)) % 6);
            // step forwards
            addStep(step);
        }
        rotatePathfinder((getFinalCoords().direction(dest)
                          + (step == STEP_BACKWARDS ? 3 : 0)) % 6);
        if (!dest.equals(getFinalCoords())) {
            addStep(type);
        }
    }

    /**
     * Returns a list of possible moves that result in a
     * facing/position/(jumping|prone) change, special steps (mine clearing and
     * such) must be handled elsewhere.
     */
    public List getNextMoves(boolean backward, boolean forward) {
        ArrayList result = new ArrayList();
        MoveStep last = getLastStep();
        if (isJumping()) {
            MovePath left = (MovePath) this.clone();
            MovePath right = (MovePath) this.clone();

            // From here, we can move F, LF, RF, LLF, RRF, and RRRF.
            result.add( ((MovePath) this.clone())
                        .addStep(MovePath.STEP_FORWARDS) );
            for ( int turn = 0; turn < 2; turn++ ) {
                left.addStep(MovePath.STEP_TURN_LEFT);
                right.addStep(MovePath.STEP_TURN_RIGHT);
                result.add( ((MovePath) left.clone())
                            .addStep(MovePath.STEP_FORWARDS) );
                result.add( ((MovePath) right.clone())
                            .addStep(MovePath.STEP_FORWARDS) );
            }
            right.addStep(MovePath.STEP_TURN_RIGHT);
            result.add( right.addStep(MovePath.STEP_FORWARDS) );

            // We've got all our next steps.            
            return result;
        }
        if (getFinalProne()) {
            if (last != null && last.getType() != STEP_TURN_RIGHT) {
                result.add(((MovePath) this.clone()).addStep(MovePath.STEP_TURN_LEFT));
            }
            if (last != null && last.getType() != STEP_TURN_LEFT) {
                result.add(((MovePath) this.clone()).addStep(MovePath.STEP_TURN_RIGHT));
            }
            result.add(((MovePath) this.clone()).addStep(MovePath.STEP_GET_UP));
            return result;
        }
        if (canShift()) {
            if (forward && (!backward || (last == null || last.getType() != MovePath.STEP_LATERAL_LEFT))) {
                result.add(((MovePath) this.clone()).addStep(STEP_LATERAL_RIGHT));
            }
            if (forward && (!backward || (last == null || last.getType() != MovePath.STEP_LATERAL_RIGHT))) {
                result.add(((MovePath) this.clone()).addStep(MovePath.STEP_LATERAL_LEFT));
            }
            if (backward && (!forward || (last == null || last.getType() != MovePath.STEP_LATERAL_LEFT_BACKWARDS))) {
                result.add(((MovePath) this.clone()).addStep(MovePath.STEP_LATERAL_RIGHT_BACKWARDS));
            }
            if (backward && (!forward || (last == null || last.getType() != MovePath.STEP_LATERAL_RIGHT_BACKWARDS))) {
                result.add(((MovePath) this.clone()).addStep(MovePath.STEP_LATERAL_LEFT_BACKWARDS));
            }
        }
        if (forward && (!backward || (last == null || last.getType() != MovePath.STEP_BACKWARDS))) {
            result.add(((MovePath) this.clone()).addStep(MovePath.STEP_FORWARDS));
        }
        if (last == null || last.getType() != MovePath.STEP_TURN_LEFT) {
            result.add(((MovePath) this.clone()).addStep(MovePath.STEP_TURN_RIGHT));
        }
        if (last == null || last.getType() != MovePath.STEP_TURN_RIGHT) {
            result.add(((MovePath) this.clone()).addStep(MovePath.STEP_TURN_LEFT));
        }
        if (backward && (!forward || (last == null || last.getType() != MovePath.STEP_FORWARDS))) {
            result.add(((MovePath) this.clone()).addStep(MovePath.STEP_BACKWARDS));
        }
        return result;
    }

    /**
     * Clones this path, will contain a new clone of the steps
     * so that the clone is independent from the original.
     *
     * @return the cloned MovePath
     */
    public Object clone() {
        MovePath copy = new MovePath(this.game, this.entity);
        copy.steps = (Vector) steps.clone();
        return copy;
    }

    /**
     * Rotate from the current facing to the destination facing.
     */
    public void rotatePathfinder(int destFacing) {
        while (getFinalFacing() != destFacing) {
            int stepType = getDirection(getFinalFacing(), destFacing);
            addStep(stepType);
        }
    }

    protected static class MovePathComparator implements Comparator {
        private Coords destination;
        boolean backward;

        public MovePathComparator(Coords destination, boolean backward) {
            this.destination = destination;
            this.backward = backward;
        }

        public int compare(Object o1, Object o2) {
            MovePath first = (MovePath) o1;
            MovePath second = (MovePath) o2;

            int firstDist = first.getMpUsed() + first.getFinalCoords().distance(destination) + getFacingDiff(first);
            int secondDist = second.getMpUsed() + second.getFinalCoords().distance(destination) + getFacingDiff(second);
            return firstDist - secondDist;
        }

        private int getFacingDiff(MovePath first) {
            if (first.isJumping()) {
                return 0;
            }
            int firstFacing = Math.abs((first.getFinalCoords().direction(destination) + (backward?3:0))%6 - first.getFinalFacing());
            if (firstFacing > 3) {
                firstFacing = 6 - firstFacing;
            }
            if (first.canShift()) {
                firstFacing = Math.max(0, firstFacing - 1);
            }
            return firstFacing;
        }
    }
}
