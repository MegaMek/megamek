/**
 * MegaMek - Copyright (C) 2000,2001,2002,2003,2004 Ben Mazur (bmazur@sev.org)
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

import java.io.Serializable;
import java.util.Enumeration;
import java.util.Vector;

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

    protected Vector steps = new Vector();

    protected transient Game game;
    protected transient Entity entity;

    /**
     * Time limit on search in ms
     * 2s on Athlon XP 2000 is enough to find all but the stubborn paths.
     *   9093 paths in 661ms
     *   13658 paths in 1372ms
     *   20001 paths in 2193ms
     *   (exponential growth)
     */
    public static final int DEFAULT_PATHFINDER_TIME_LIMIT = 2000;

    /**
     * Generates a new, empty, movement path object.
     */
    public MovePath(Game game, Entity entity) {
        this.entity = entity;
        this.game = game;
    }

    public Entity getEntity() {
        return entity;
    }

    public boolean isInfantry() {
        return entity instanceof Infantry;
    }

    /**
     * TODO: should be a method of entity.
     */
    boolean isUsingManAce() {
        return entity.getCrew().getOptions().booleanOption("maneuvering_ace");
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        for (Enumeration i = steps.elements(); i.hasMoreElements();) {
            sb.append( i.nextElement().toString() );
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
        return addStep(new MoveStep(this, type));
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
        return ((entity instanceof QuadMech) || isUsingManAce()) && !isJumping();
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
        
        // TODO: more elegant method possible here?
        if (prev != null && prev.isStackingViolation()) {
            // if previous step is stacking violation, fully recompile
            compile(game, entity);
            return this;
        }

        try {
            step.compile(game, entity, prev);
        } catch (RuntimeException re) {
            step.setMovementType(Entity.MOVE_ILLEGAL);
        }

        if (!step.isLegal()) {
            return this;
        }

        // set moveType, illegal, trouble flags
        step.compileIllegal(game, entity, prev);

        // check for illegal jumps
        if (isJumping()) {
            Coords start = entity.getPosition();
            Coords land = step.getPosition();
            int distance = start.distance(land);

            if (step.getMpUsed() > distance) {
                step.setMovementType(Entity.MOVE_ILLEGAL);
            }
        }
        return this;
    }

    public void compile(Game g, Entity en) {
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
        compileLastStep();
    }

    public void removeLastStep() {
        if (steps.size() > 0) {
            steps.removeElementAt(steps.size() - 1);
        }
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
        if (entity == null) return false;
        return entity.isProne();
    }

    public int getLastStepMovementType() {
        if (getLastStep() == null) {
            return Entity.MOVE_NONE;
        }
        return getLastStep().getMovementType();
    }

    public MoveStep getLastStep() {
        return getStep(steps.size() - 1);
    }

    /* Debug method */
    public void printAllSteps() {
        System.out.println("*Steps*");
        for (int i = 0;i < steps.size();i++) {
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
            if (step.getMovementType() != Entity.MOVE_ILLEGAL) {
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
                && (step2.getType() == MovePath.STEP_BACKWARDS
                    || step2.getType() == MovePath.STEP_FORWARDS)) {
            int stepType = step1.getType();
            int direction = step2.getType();
            // remove all old steps
            steps.removeElementAt(index);
            steps.removeElementAt(index);
            steps.removeElementAt(index);
            // add new step
            MoveStep shift = new MoveStep
                (this, lateralShiftForTurn(stepType, direction));
            addStep(shift);
        }
    }

    /**
     * Returns the lateral shift that corresponds to the turn direction
     */
    public static int lateralShiftForTurn(int turn, int direction) {
        if (direction == MovePath.STEP_FORWARDS) {
            switch (turn) {
                case MovePath.STEP_TURN_LEFT:
                    return MovePath.STEP_LATERAL_LEFT;
                case MovePath.STEP_TURN_RIGHT:
                    return MovePath.STEP_LATERAL_RIGHT;
                default :
                    return turn;
            }
        } else {
            switch (turn) {
                case MovePath.STEP_TURN_LEFT:
                    return MovePath.STEP_LATERAL_LEFT_BACKWARDS;
                case MovePath.STEP_TURN_RIGHT:
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
            case MovePath.STEP_LATERAL_LEFT:
                return MovePath.STEP_TURN_LEFT;
            case MovePath.STEP_LATERAL_RIGHT:
                return MovePath.STEP_TURN_RIGHT;
            case MovePath.STEP_LATERAL_LEFT_BACKWARDS:
                return MovePath.STEP_TURN_LEFT;
            case MovePath.STEP_LATERAL_RIGHT_BACKWARDS:
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

    public void compileLastStep() {
        compileLastStep(true);
    }

    public void compileLastStep(boolean clip) {
        if (clip)
            clipToPossible(); //get rid of "trailing garbage"

        for (int i = length() - 1; i >= 0; i--) {
            final MoveStep step = getStep(i);
            if (step.checkAndSetIllegal(game, entity)) {
                continue;
            }
            break;
        }
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

    /**
     * Extend the current path to the destination <code>Coords</code>.
     *
     * @param   dest the destination <code>Coords</code> of the move.
     * @param   type the type of movment step required.
     */
    public void findPathTo( Coords dest, int type ) {
        int timeLimit = Settings.maxPathfinderTime;

        // Belt-and-suspenders check for negative numbers.
        if ( 0 >= timeLimit ) {
            this.lazyPathfinder( dest, type );
        } else {
            if ( 10000 <= timeLimit ) {
                System.out.print( "WARNING!!!  Settings allow up to " );
                System.out.print( timeLimit );
                System.out.println( " milliseconds to find the optimum path!");
            }
            this.notSoLazyPathfinder( dest, type, timeLimit );
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
            subDest = dest.translated(dest.direction1(getFinalCoords()));
        }

        while (!getFinalCoords().equals(subDest)) {
            // adjust facing
            rotatePathfinder((getFinalCoords().direction1(subDest)
                              + (step == STEP_BACKWARDS ? 3 : 0)) % 6);
            // step forwards
            addStep(step);
        }
        rotatePathfinder((getFinalCoords().direction1(dest)
                          + (step == STEP_BACKWARDS ? 3 : 0)) % 6);
        if (!dest.equals(getFinalCoords())) {
            addStep(type);
        }
    }

    public boolean isMoveLegal() {
        // Moves which end up off of the board are not legal.
        if ( !game.getBoard().contains(getFinalCoords()) ) {
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
    private void notSoLazyPathfinder( final Coords dest, final int type,
                                     final int timeLimit ) {
        long startTime = System.currentTimeMillis();
        long endTime = System.currentTimeMillis() + timeLimit;

        // Seperate the steps that we are going to perform,
        // from the type of action requested at the end.
        int step = type;
        if(step != MovePath.STEP_BACKWARDS) {
            step = MovePath.STEP_FORWARDS;
        }

        //Sanity Check: if we are at the destination, we are done
        if (this.getFinalCoords().equals(dest)) {
            return;
        }

        // A comparator for candidate paths that uses a
        // heuristic (the distance to the destination).
        MovePathComparator mpc = new MovePathComparator(dest, false);

        // This will be be the candidate path we've just
        // pulled off the front of the candidates list
        MovePath candidatePath = (MovePath) this.clone();

        // Will hold the list of candidate paths.
        ArrayList candidates = new ArrayList();
        candidates.add(candidatePath);

        boolean keepLooping = true;
        int loopcount = 0;
        while(candidates.size() > 0  && keepLooping) {
            //get the front item-- the most likely path
            candidatePath = (MovePath) candidates.remove(0);

            MoveStep candidateLastStep = candidatePath.getLastStep();
            Coords startingPos;
            if (candidateLastStep == null) {
                startingPos = this.entity.getPosition();
            }
            else {
                startingPos = candidatePath.getLastStep().getPosition();
            }

            MovePath expandedPath = (MovePath) candidatePath.clone();
            expandedPath.addStep(step);

            //does our moving forward gets us to the destination?
            if (expandedPath.getFinalCoords().equals(dest)) {
                if (type != MovePath.STEP_FORWARDS
                    && type != MovePath.STEP_BACKWARDS) {
                    // If we were not just moving (like charge or
                    // DFA), then try adding a step of that type.
                    MovePath pathOriginalType =
                        (MovePath) candidatePath.clone();
                    pathOriginalType.addStep(type);
                    if (pathOriginalType.getFinalCoords().equals(dest)) {
                        candidatePath = pathOriginalType;
                        break;
                    }
                    else {
                        // Fall back to the candidate path.
                        candidatePath = expandedPath;
                        break;
                    }
                }
                else {
                    // If we were just trying to move forwards
                    // or backwards, then we're done.
                    candidatePath = expandedPath;
                    break;
                }
            }

            // Check to see if the movement isn't possible for reasons
            // other than MPs, (off the board, cliffs, etc).
            else if (expandedPath.getLastStep().isMovementPossible
                     (this.game, startingPos)) {
                // Find where to insert it into our
                // list of candidates, and insert.
                int index = Collections.binarySearch(candidates,
                                                     expandedPath,
                                                     mpc);
                index += 1;
                if (index < 0) {
                    // If the index is less than zero then it didn't find
                    // an equivalent one in the list and the index returned
                    // needs to be changed to the insertion point -see
                    // binarySearch javadoc
                    index *= -1;
                } //else it was found, just insert after the found one.
                candidates.add(index, expandedPath);
            }


            // If the last step was turn right,
            // don't bother trying to turn left.
            if(candidateLastStep == null
               || candidateLastStep.getType() != MovePath.STEP_TURN_RIGHT) {
                //try turning left
                expandedPath = (MovePath) candidatePath.clone();
                expandedPath.addStep(MovePath.STEP_TURN_LEFT);
                int index = Collections.binarySearch(candidates,
                                                     expandedPath,
                                                     mpc);
                index += 1;
                if (index < 0) {
                    // If the index is less than zero then it didn't find
                    // an equivalent one in the list and the index returned
                    // needs to be changed to the insertion point -see
                    // binarySearch javadoc
                    index *= -1;
                } //else it was found, just insert after the found one.
                candidates.add(index, expandedPath);
            }

            //if the last step was turn left, don't bother trying to turn right
            if (candidateLastStep == null
                || candidateLastStep.getType() != MovePath.STEP_TURN_LEFT) {
                //try turning right
                // Since this is the last variant we are going to be creating,
                // just use the original object instead of creating a new one
                // only to let the original be collected.
                expandedPath.addStep(MovePath.STEP_TURN_RIGHT);

                int index = Collections.binarySearch(candidates,
                                                     expandedPath,
                                                     mpc);
                index += 1;
                if (index < 0) {
                    // If the index is less than zero then it didn't find
                    // an equivalent one in the list and the index returned
                    // needs to be changed to the insertion point -see
                    // binarySearch javadoc
                    index *= -1;
                } //else it was found, just insert after the found one.
                candidates.add(index, expandedPath);
            }

            loopcount++;
            if(loopcount % 64 == 0) {
                keepLooping  = System.currentTimeMillis() < endTime; 
            }

        } //end while

        //Make the path we found, this path.
        this.steps = candidatePath.steps;

        /*
        //Timing goodies
        long time = System.currentTimeMillis();
        time -= startTime;
        StringBuffer sb = new StringBuffer("Expanded Nodes: ");
        sb.append(loopcount);
        sb.append(" in ");
        sb.append(time);
        sb.append("ms: ");
        sb.append(1000.0 * (double) loopcount / (double) time);
        sb.append(" NodesExpanded/s");
        System.out.println(sb.toString());
        */
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
     * A class that will compare MovePaths, optionally using a heuristic
     * Current heuristic is the strait line distance from the end of the
     * path to the destination.
     */
    protected class MovePathComparator implements Comparator {
        private Coords destination;
        private boolean heuristicOnly;

        /**
         * Constructor for MovePathComparator
         * @param destination the target destination for the heuristic,
         *      <code>null</code> for no heuristic use
         * @param heuristicOnly whether to only use the heuristic in comparison
         */
        public MovePathComparator(Coords destination, boolean heuristicOnly) {
            this.destination = destination;
            this.heuristicOnly = heuristicOnly;
        }

        /**
         * Whether or not this comparator will
         * only use the heuristic in comparing
         * @return is heuristic only
         */
        public boolean isHeuristicOnly() {
            return heuristicOnly;
        }

        /**
         * Should this compare only on the heuristic
         * @param heuristicOnly - the value
         */
        public void setHeuristicOnly(boolean heuristicOnly) {
            this.heuristicOnly = heuristicOnly;
        }

        /**
         * The destination of the heuristic
         * @return destination value
         */
        public Coords getDestination() {
            return destination;
        }

        /**
         * Set destination of the heuristic
         * @param destination destination value
         */
        public void setDestination(Coords destination) {
            this.destination = destination;
        }

        /**
         * MovePathComparator for sorting a list of MovePaths with heuristic
         * for A* search. The heuristic is the Coords.distance(destination)
         * from the final coordinates of the MovePath to the destination.
         * If the destination is null, then it just compares the cost for
         * the MovePath so far.
         * <p/>
         * Compares its two arguments for order.  Returns a negative integer,
         * zero, or a positive integer as the first argument is less than,
         * equal to, or greater than the second.<p>
         * <p/>
         * The implementor must ensure that <tt>sgn(compare(x, y)) ==
         * -sgn(compare(y, x))</tt> for all <tt>x</tt> and <tt>y</tt>.  (This
         * implies that <tt>compare(x, y)</tt> must throw an exception if and
         * only if <tt>compare(y, x)</tt> throws an exception.)<p>
         * <p/>
         * The implementor must also ensure that the relation is transitive:
         * <tt>((compare(x, y)&gt;0) &amp;&amp; (compare(y, z)&gt;0))</tt>
         * implies <tt>compare(x, z)&gt;0</tt>.<p>
         * <p/>
         * Finally, the implementer must ensure that <tt>compare(x, y)==0</tt>
         * implies that <tt>sgn(compare(x, z))==sgn(compare(y, z))</tt> for all
         * <tt>z</tt>.<p>
         * <p/>
         * It is generally the case, but <i>not</i> strictly required that
         * <tt>(compare(x, y)==0) == (x.equals(y))</tt>.  Generally speaking,
         * any comparator that violates this condition should clearly indicate
         * this fact.  The recommended language is "Note: this comparator
         * imposes orderings that are inconsistent with equals."
         *
         * @param o1 the first object to be compared.
         * @param o2 the second object to be compared.
         * @return a negative integer, zero, or a positive integer as the
         *         first argument is less than, equal to, or greater than the
         *         second.
         * @throws ClassCastException if the arguments' types prevent them from
         *                            being compared by this Comparator.
         */
        public int compare(Object o1, Object o2) {
            MovePath first = (MovePath) o1;
            MovePath second = (MovePath) o2;

            int firstGuess = 0, secondGuess = 0, firstMP = 0, secondMP = 0;

            if (destination != null) {
                firstGuess = first.getFinalCoords().distance(destination);
                secondGuess = second.getFinalCoords().distance(destination);
            }
            if (heuristicOnly) {
                return firstGuess - secondGuess;
            } else {
                firstMP = first.getMpUsed() + firstGuess;
                secondMP = second.getMpUsed() + secondGuess;
                return firstMP - secondMP;
            }


        }
    }

}
