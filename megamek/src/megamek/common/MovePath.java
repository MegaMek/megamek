/**
 * MegaMek - Copyright (C) 2000-2003 Ben Mazur (bmazur@sev.org)
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

import java.util.*;

/**
 * Holds movement path for an entity.
 */
public class MovePath implements Serializable {
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
     *TODO: should be a method of entity.
     */
    boolean isUsingManAce() {
        return entity.getCrew().getOptions().booleanOption("maneuvering_ace");
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        for (Enumeration i = steps.elements(); i.hasMoreElements();) {
            sb.append((MoveStep) i.nextElement());
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
    public MovePath addStep(int type) {
        return addStep(new MoveStep(this, type));
    }

    /**
	 * Add a new step to the movement path with the given target.
	 * 
	 * @param type
	 *            the type of movement.
	 * @param target
	 *            the <code>Targetable</code> object that is the target of
	 *            this step. For example, the enemy being charged.
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
        
        step.compile(game, entity, prev);

        if (!game.getBoard().contains(step.getPosition())) {
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

    /**
     * Not to be called from the server.
     */
    public void compile() {
        compile(game, entity);
    }
    
    /**
     * TODO: this could be recompile
     */
    public void compile(Game g, Entity en) {
        this.game = g;
        this.entity = en;
		compileLastStep();
    }

    public void removeLastStep() {
        if (steps.size() > 0) {
            steps.removeElementAt(steps.size() - 1);
        }
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
	 * Clear all flags in all steps
	 */
    /*
	 * public void clearAllFlags() { for (final Enumeration i = getSteps();
	 * i.hasMoreElements();) { final MoveStep step = (MoveStep)
	 * i.nextElement(); step.clearAllFlags(); }
	 */

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
        return (MoveStep) getStep(steps.size() - 1);
    }

    /**
	 * Removes impossible steps, if compiled. If not compiled, does nothing.
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
	 * 
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
            int stepType = getStep(index).getType();
            // remove all old steps
            steps.removeElementAt(index);
            steps.removeElementAt(index);
            steps.removeElementAt(index);
            // add new step
            MoveStep shift = new MoveStep(this, lateralShiftForTurn(stepType));
            addStep(shift);
        }
    }

    /**
	 * Returns the lateral shift that corresponds to the turn direction
	 */
    public static int lateralShiftForTurn(int turn) {
        switch (turn) {
            case MovePath.STEP_TURN_LEFT :
                return MovePath.STEP_LATERAL_LEFT;
            case MovePath.STEP_TURN_RIGHT :
                return MovePath.STEP_LATERAL_RIGHT;
            default :
                return turn;
        }
    }

    /**
	 * Returns the turn direction that corresponds to the lateral shift
	 */
    static int turnForLateralShift(int shift, int direction) {
        if (direction == MovePath.STEP_FORWARDS) {
            switch (shift) {
                case MovePath.STEP_LATERAL_LEFT :
                    return MovePath.STEP_TURN_LEFT;
                case MovePath.STEP_LATERAL_RIGHT :
                    return MovePath.STEP_TURN_RIGHT;
                default :
                    return shift;
            }
        } else {
            switch (shift) {
                case MovePath.STEP_TURN_LEFT :
                    return MovePath.STEP_LATERAL_RIGHT_BACKWARDS;
                case MovePath.STEP_TURN_RIGHT :
                    return MovePath.STEP_LATERAL_LEFT_BACKWARDS;
                default :
                    return shift;
            }
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
	 * Returns the logical number of hexes moved the path (does not count turns, etc).
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

    protected void compileLastStep() {
        for (int i = length() - 1; i >= 0; i--) {
            final MoveStep step = getStep(i);
            if (step.checkIllegal(game, entity)) {
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
        
    public void lazyPathfinder(Coords dest, int type) {
        int step = STEP_FORWARDS;
        if (type == STEP_BACKWARDS) {
            step = STEP_BACKWARDS;
        }
		Coords subDest = dest;
		if (!dest.equals(getFinalCoords())) {
		    subDest = dest.translated(dest.direction1(getFinalCoords()));
		}

        while(!getFinalCoords().equals(subDest)) {
            // adjust facing
            rotatePathfinder((getFinalCoords().direction1(subDest) + (step==STEP_BACKWARDS?3:0)) % 6);
            // step forwards
            addStep(step);
        }
		rotatePathfinder((getFinalCoords().direction1(dest) + (step==STEP_BACKWARDS?3:0)) % 6);
		if (!dest.equals(getFinalCoords())) {
		    addStep(type);
		}
    }
    
    public boolean isMoveLegal() {
        if (getLastStep() == null) {
            return true;
        }
        return getLastStep().isLegal();
    }
}