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
 * Holds movement data for an entity.
 */
public class MovementData
    implements Serializable 
{
    public static final int        STEP_FORWARDS                 = 1;
    public static final int        STEP_BACKWARDS                = 2;
    public static final int        STEP_TURN_LEFT                = 3;
    public static final int        STEP_TURN_RIGHT               = 4;
    public static final int        STEP_GET_UP                   = 5;
    public static final int        STEP_GO_PRONE                 = 6;
    public static final int        STEP_START_JUMP               = 7;
    public static final int        STEP_CHARGE                   = 8;
    public static final int        STEP_DFA                      = 9;
    public static final int        STEP_FLEE                     = 10;
    public static final int        STEP_LATERAL_LEFT             = 11;
    public static final int        STEP_LATERAL_RIGHT            = 12;
    public static final int        STEP_LATERAL_LEFT_BACKWARDS   = 13;
    public static final int        STEP_LATERAL_RIGHT_BACKWARDS  = 14;
    public static final int        STEP_UNJAM_RAC                = 15;
    public static final int        STEP_LOAD                     = 16;
    public static final int        STEP_UNLOAD                   = 17;
    public static final int        STEP_EJECT                    = 18;
    public static final int        STEP_CLEAR_MINEFIELD          = 19;

    private Vector steps = new Vector();
    
    private boolean compiled = false;
    
    
    /**
     * Generates a new, empty, movement data object.
     */
    public MovementData() {
        ;
    }
    
    /**
     * Generates a new movement data object that is a copy
     * of the specified movement data.
     * 
     * @param md the movement data to copy.
     */
    public MovementData(MovementData md) {
        this();
        append(md);
    }
    
    public String toString() {
        StringBuffer sb = new StringBuffer();
        for (Enumeration i =steps.elements();i.hasMoreElements();) {
            sb.append((Step)i.nextElement());	
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
    
    public boolean isCompiled() {
        return compiled;
    }
    
    public void setCompiled(boolean compiled) {
        this.compiled = compiled;
    }

    /**
     * Add a new step to the movement data.
     * 
     * @param type the type of movement.
     */
    public void addStep(int type) {
        addStep(new Step(type));
    }

    /**
     * Add a new step to the movement data with the given target.
     * 
     * @param type the type of movement.
     * @param target the <code>Targetable</code> object that is the target
     *          of this step. For example, the enemy being charged.
     */
    public void addStep(int type, Targetable target) {
        addStep(new Step(type, target));
    }
    
    /**
     * Initializes a step as part of this movement data.  Then adds
     * it to the list.
     * @param step
     */
    protected void addStep(Step step) {
        step.parent = this;
        if (steps.size() > 0) {
            step.prev = (Step)steps.elementAt(steps.size() - 1);
        }
        steps.addElement(step);
    }

    public Enumeration getSteps() {
        return steps.elements();
    }
    
    public Step getStep(int index) {
        return (Step)steps.elementAt(index);
    }
    
    /**
     * Appends the specified movement data onto the end of the
     * current data.
     * 
     * @param md the movement data to append.
     */
    public void append(MovementData md) {
        for (final Enumeration i = md.getSteps(); i.hasMoreElements();) {
            addStep((Step)i.nextElement());
        }
        compiled = false;
    }
    
    /**
     * Returns a new movement data object representing the data
     * that would result if you appended the curent movement
     * data to the data specified.
     * 
     * @param md the movement data to append.
     */
    public MovementData getAppended(MovementData md) {
        MovementData newMd = new MovementData(this);
        newMd.append(md);
        return newMd;
    }
  
    /**
     * Check for any of the specified type of step in the data
     */
    public boolean contains(int type) {
        for (final Enumeration i = getSteps(); i.hasMoreElements();) {
            Step step = (Step)i.nextElement();
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
            Step step = (Step)i.nextElement();
            if (step.getState().isUsingMASC()) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Clear all flags in all steps
     */
    public void clearAllFlags() {
        for (final Enumeration i = getSteps(); i.hasMoreElements();) {
            final Step step = (Step)i.nextElement();
            step.clearAllFlags();
        }
    }
    
    /**
     * Returns the final coordinates if a mech were to perform all the steps
     * in these data.
     */
    public Coords getFinalCoords(Coords start, int facing) {
        int curFacing = facing;
        Coords curPos = new Coords(start);
        for (final Enumeration i = getSteps(); i.hasMoreElements();) {
            final Step step = (Step)i.nextElement();
            // adjust facing
            switch (step.getType()) {
            case  STEP_TURN_LEFT :
            case STEP_TURN_RIGHT:
                curFacing = getAdjustedFacing(curFacing, step.getType());
                break;
            case STEP_FORWARDS :
            case STEP_CHARGE :
            case STEP_DFA :
                curPos = curPos.translated(curFacing);
                break;
            case STEP_BACKWARDS :
                curPos = curPos.translated((curFacing + 3) % 6);
                break;
            case STEP_LATERAL_LEFT :
                curPos = curPos.translated((curFacing + 5) % 6);
                break;
            case STEP_LATERAL_RIGHT :
                curPos = curPos.translated((curFacing + 1) % 6);
                break;
            case STEP_LATERAL_LEFT_BACKWARDS :
                curPos = curPos.translated((curFacing + 4) % 6);
                break;
            case STEP_LATERAL_RIGHT_BACKWARDS :
                curPos = curPos.translated((curFacing + 2) % 6);
                break;
            }
        }
        return curPos;
    }
    
    /**
     * Returns the final facing if a mech were to perform all the steps
     * in these data.
     */
    public int getFinalFacing(int facing) {
        for (final Enumeration i = getSteps(); i.hasMoreElements();) {
            final Step step = (Step)i.nextElement();
            if (step.getType() == STEP_TURN_LEFT 
                || step.getType() == STEP_TURN_RIGHT) {
                facing = getAdjustedFacing(facing, step.getType());
            }
        }
        return facing;
    }
    
    /**
     * Returns whether or not a unit would end up prone after all of the steps
     */
    public boolean getFinalProne(boolean bCurProne) {
        boolean bProne = bCurProne;
        for (final Enumeration i = getSteps(); i.hasMoreElements();) {
            final Step step = (Step)i.nextElement();
            if (step.getType() == STEP_GO_PRONE) {
                bProne = true;
            } else if (step.getType() == STEP_GET_UP) {
                bProne = false;
            }
        }
        return bProne;
    }

    public int getLastStepMovementType() {
        int lastStepMoveType = Entity.MOVE_NONE;
        for (final Enumeration i = getSteps(); i.hasMoreElements();) {
            final Step step = (Step)i.nextElement();
            if (step.getState().getMovementType() == Entity.MOVE_ILLEGAL) {
                break;
            } else {
                lastStepMoveType = step.getState().getMovementType();
            }
        }
        return lastStepMoveType;
    }
    
    /**
     * Removes impossible steps, if compiled.  If not compiled, does nothing.
     */
    public void clipToPossible() {
        if (!compiled) {
            return;
        }
        // hopefully there's no impossible steps in the middle of possible ones
        Vector goodSteps = new Vector();
        for (final Enumeration i = steps.elements(); i.hasMoreElements();) {
            final Step step = (Step)i.nextElement();
            if (step.getState().getMovementType() != Entity.MOVE_ILLEGAL) {
                goodSteps.addElement(step);
            }
        }
        steps = goodSteps;
    }
    
    /**
     * Changes all turn-forwards-opposite-turn sequences into quad lateral 
     * shifts.
     *
     * Finds the sequence of three steps that can be transformed,
     * then removes all three and replaces them with the lateral shift step.
     */
    public void transformLateralShifts() {
        int index;
        while ((index = firstLateralShift()) != -1) {
            int stepType = getStep(index).getType();
            steps.removeElementAt(index);
            steps.removeElementAt(index);
            steps.removeElementAt(index);
            steps.insertElementAt(new Step(lateralShiftForTurn(stepType)), index);
        }
    }

    /**
     * Changes all turn-backwards-opposite-turn sequences into quad lateral 
     * shifts.
     *
     * Finds the sequence of three steps that can be transformed,
     * then removes all three and replaces them with the lateral shift step.
     */
    public void transformLateralShiftsBackwards() {
        int index;
        while ((index = firstLateralShiftBackwards()) != -1) {
            int stepType = getStep(index).getType();
            steps.removeElementAt(index);
            steps.removeElementAt(index);
            steps.removeElementAt(index);
            steps.insertElementAt(new Step(lateralShiftBackwardsForTurn(stepType)), index);
        }
    }
    
    /**
     * Returns the index of the first step which starts movement that can be
     * converted into a quad lateral shift, or -1 if none are found.
     */
    private int firstLateralShift() {
        for (int i = 0; i < length() - 2; i++) {
            int step1 = getStep(i).getType();
            int step2 = getStep(i + 1).getType();
            int step3 = getStep(i + 2).getType();
            
            if (oppositeTurn(step1, step3) && step2 == MovementData.STEP_FORWARDS) {
                return i;
            }
        }
        
        return -1;
    }

    /**
     * Returns the index of the first step which starts movement that can be
     * converted into a quad lateral shift backwards, or -1 if none are found.
     */
    private int firstLateralShiftBackwards() {
        for (int i = 0; i < length() - 2; i++) {
            int step1 = getStep(i).getType();
            int step2 = getStep(i + 1).getType();
            int step3 = getStep(i + 2).getType();
            
            if (oppositeTurn(step1, step3) && step2 == MovementData.STEP_BACKWARDS) {
                return i;
            }
        }
        
        return -1;
    }

    /**
     * Returns whether the two step types contain opposite turns
     */
    private static boolean oppositeTurn(int turn1, int turn2) {
        switch (turn1) {
            case MovementData.STEP_TURN_LEFT :
                return turn2 == MovementData.STEP_TURN_RIGHT;
            case MovementData.STEP_TURN_RIGHT :
                return turn2 == MovementData.STEP_TURN_LEFT;
            default :
                return false;
        }
    }
    
    /**
     * Returns the lateral shift that corresponds to the turn direction
     */
    public static int lateralShiftForTurn(int turn) {
        switch (turn) {
            case MovementData.STEP_TURN_LEFT :
                return MovementData.STEP_LATERAL_LEFT;
            case MovementData.STEP_TURN_RIGHT :
                return MovementData.STEP_LATERAL_RIGHT;
            default :
                return turn;
        }
    }

    /**
     * Returns the lateral shift backwards that corresponds to the turn direction
     */
    public static int lateralShiftBackwardsForTurn(int turn) {
        switch (turn) {
            case MovementData.STEP_TURN_LEFT :
                return MovementData.STEP_LATERAL_RIGHT_BACKWARDS;
            case MovementData.STEP_TURN_RIGHT :
                return MovementData.STEP_LATERAL_LEFT_BACKWARDS;
            default :
                return turn;
        }
    }
    
    /**
     * Returns the turn direction that corresponds to the lateral shift 
     */
    public static int turnForLateralShift(int shift) {
        switch (shift) {
            case MovementData.STEP_LATERAL_LEFT :
                return MovementData.STEP_TURN_LEFT;
            case MovementData.STEP_LATERAL_RIGHT :
                return MovementData.STEP_TURN_RIGHT;
            default :
                return shift;
        }
    }
    
    /**
     * Returns the turn direction that corresponds to the lateral shift backwards
     */
    public static int turnForLateralShiftBackwards(int shift) {
        switch (shift) {
            case MovementData.STEP_LATERAL_LEFT_BACKWARDS :
                return MovementData.STEP_TURN_RIGHT;
            case MovementData.STEP_LATERAL_RIGHT_BACKWARDS :
                return MovementData.STEP_TURN_LEFT;
            default :
                return shift;
        }
    }
    
    /**
     * Returns the direction (either MovementData.STEP_TURN_LEFT or 
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
        } else if(movement == STEP_TURN_LEFT) {
            return (facing + 5) % 6;
        }
        return facing;
    }
    
    /**
     * Returns the number of MPs used in the path
     */
    public int getMpUsed() {
        int mpUsed = 0;
        
        for (final Enumeration i = steps.elements(); i.hasMoreElements();) {
            final Step step = (Step)i.nextElement();
            mpUsed =+ step.getState().getMpUsed();
        };
        
        return mpUsed;
    };
    /**
     * Returns the number of hexes moved the path (does not count turns, etc).
     */
    public int getHexesMoved() {
        int hexes = 0;
        
        for (final Enumeration i = steps.elements(); i.hasMoreElements();) {
            final Step step = (Step)i.nextElement();
            if ( (step.getType() == STEP_FORWARDS)
            || (step.getType() == STEP_BACKWARDS)
            || (step.getType() == STEP_CHARGE)
            || (step.getType() == STEP_LATERAL_RIGHT)
            || (step.getType() == STEP_LATERAL_LEFT)
            || (step.getType() == STEP_LATERAL_RIGHT_BACKWARDS)
            || (step.getType() == STEP_LATERAL_LEFT_BACKWARDS) ) {
                hexes++;
            };
        };
        
        return hexes;
    };
    
    public static class MovementState implements Cloneable, Serializable {
        Coords position;
        int facing;
        int elevation;
        //
        int mp; // this step
        int mpUsed; // whole path
        int distance;
        int movementType;
        boolean isProne;
        //
        boolean legal;
        boolean danger; // keep psr
        boolean pastDanger;
        boolean isUsingMASC;
        int targetNumberMASC; // psr
        //
        boolean firstStep; // check if no previous
        boolean isInfantry; // method
        boolean isTurning; // method
        boolean isUnloaded;
        boolean prevStepOnPavement; // prev
        //
        Coords lastPos; // prev
        boolean hasJustStood;
        boolean lastWasBackwards; // prev
        boolean thisStepBackwards;
        int overallMoveType;
        boolean isJumping;
        boolean isRunProhibited;
        boolean onlyPavement; // additive
        boolean isPavementStep;
        boolean isUsingManAce; // method

        /**
         * Takes the given state as the previous state and sets flags from it.
         * 
         * @param state
         */
        public void setFromPrev(MovementState prev) {
           this.hasJustStood =  prev.hasJustStood;
           this.facing = prev.getFacing();
           this.position = prev.getPosition();
           
           this.distance = prev.getDistance();
           this.mpUsed = prev.mpUsed;
           this.isPavementStep = prev.isPavementStep;
           this.onlyPavement = prev.onlyPavement;
           this.isJumping = prev.isJumping;
           this.isRunProhibited = prev.isRunProhibited;
        }
        
        /**
         * Sets this state as coming from the entity.
         * @param entity
         */
        public void setFromEntity(Entity entity, Game game) {
            this.position = entity.getPosition();
            this.facing = entity.getFacing();
            // elevation
            this.mpUsed = entity.mpUsed;
            this.distance = entity.delta_distance;
            this.isProne = entity.isProne();

            // check pavement
            if (position != null) {
                Hex curHex = game.board.getHex(position);
                if (curHex.hasPavement()) {
                    onlyPavement = true;
                    isPavementStep = true;
                }
            }

            this.isInfantry = (entity instanceof Infantry);
        }
        
        /**
         * Adjusts facing to comply with the type of step indicated.
         * @param stepType
         */
        public void adjustFacing(int stepType) {
           facing = MovementData.getAdjustedFacing(facing, stepType);
        }
        
        /**
         * Moves the position one hex in the direction indicated.  Does not
         * change facing.
         * @param dir
         */
        public void moveInDir(int dir) {
            position = position.translated(dir);
        }
        
        /**
         * Adds a certain amount to the distance parameter.
         * @param increment
         */
        public void addDistance(int increment) {
            distance += increment;
        }
        
        /**
         * Adds a certain amount to the mpUsed parameter.
         * @param increment
         */
        public void addMpUsed(int increment) {
            mpUsed += increment;
        }
        
        /**
         * @return
         */
        public boolean isDanger() {
            return danger;
        }

        /**
         * @return
         */
        public int getDistance() {
            return distance;
        }

        /**
         * @return
         */
        public int getElevation() {
            return elevation;
        }

        /**
         * @return
         */
        public int getFacing() {
            return facing;
        }

        /**
         * @return
         */
        public boolean isFirstStep() {
            return firstStep;
        }

        /**
         * @return
         */
        public boolean isHasJustStood() {
            return hasJustStood;
        }

        /**
         * @return
         */
        public boolean isInfantry() {
            return isInfantry;
        }

        /**
         * @return
         */
        public boolean isJumping() {
            return isJumping;
        }

        /**
         * @return
         */
        public boolean isPavementStep() {
            return isPavementStep;
        }

        /**
         * @return
         */
        public boolean isProne() {
            return isProne;
        }

        /**
         * @return
         */
        public boolean isRunProhibited() {
            return isRunProhibited;
        }

        /**
         * @return
         */
        public boolean isTurning() {
            return isTurning;
        }

        /**
         * @return
         */
        public boolean isUnloaded() {
            return isUnloaded;
        }

        /**
         * @return
         */
        public boolean isUsingManAce() {
            return isUsingManAce;
        }

        /**
         * @return
         */
        public boolean isUsingMASC() {
            return isUsingMASC;
        }

        /**
         * @return
         */
        public Coords getLastPos() {
            return lastPos;
        }

        /**
         * @return
         */
        public boolean isLastWasBackwards() {
            return lastWasBackwards;
        }

        /**
         * @return
         */
        public boolean isLegal() {
            return legal;
        }

        /**
         * @return
         */
        public int getMovementType() {
            return movementType;
        }

        /**
         * @return
         */
        public int getMpUsed() {
            return mpUsed;
        }

        /**
         * @return
         */
        public boolean isOnlyPavement() {
            return onlyPavement;
        }

        /**
         * @return
         */
        public int getOverallMoveType() {
            return overallMoveType;
        }

        /**
         * @return
         */
        public boolean isPastDanger() {
            return pastDanger;
        }

        /**
         * @return
         */
        public Coords getPosition() {
            return position;
        }

        /**
         * @return
         */
        public boolean isPrevStepOnPavement() {
            return prevStepOnPavement;
        }

        /**
         * @return
         */
        public int getTargetNumberMASC() {
            return targetNumberMASC;
        }

        /**
         * @return
         */
        public boolean isThisStepBackwards() {
            return thisStepBackwards;
        }

        /**
         * @param b
         */
        public void setDanger(boolean b) {
            danger = b;
        }

        /**
         * @param i
         */
        public void setDistance(int i) {
            distance = i;
        }

        /**
         * @param i
         */
        public void setElevation(int i) {
            elevation = i;
        }

        /**
         * @param i
         */
        public void setFacing(int i) {
            facing = i;
        }

        /**
         * @param b
         */
        public void setFirstStep(boolean b) {
            firstStep = b;
        }

        /**
         * @param b
         */
        public void setHasJustStood(boolean b) {
            hasJustStood = b;
        }

        /**
         * @param b
         */
        public void setInfantry(boolean b) {
            isInfantry = b;
        }

        /**
         * @param b
         */
        public void setJumping(boolean b) {
            isJumping = b;
        }

        /**
         * @param b
         */
        public void setPavementStep(boolean b) {
            isPavementStep = b;
        }

        /**
         * @param b
         */
        public void setProne(boolean b) {
            isProne = b;
        }

        /**
         * @param b
         */
        public void setRunProhibited(boolean b) {
            isRunProhibited = b;
        }

        /**
         * @param b
         */
        public void setTurning(boolean b) {
            isTurning = b;
        }

        /**
         * @param b
         */
        public void setUnloaded(boolean b) {
            isUnloaded = b;
        }

        /**
         * @param b
         */
        public void setUsingManAce(boolean b) {
            isUsingManAce = b;
        }

        /**
         * @param b
         */
        public void setUsingMASC(boolean b) {
            isUsingMASC = b;
        }

        /**
         * @param coords
         */
        public void setLastPos(Coords coords) {
            lastPos = coords;
        }

        /**
         * @param b
         */
        public void setLastWasBackwards(boolean b) {
            lastWasBackwards = b;
        }

        /**
         * @param b
         */
        public void setLegal(boolean b) {
            legal = b;
        }

        /**
         * @param i
         */
        public void setMovementType(int i) {
            movementType = i;
        }

        /**
         * @param i
         */
        public void setMpUsed(int i) {
            mpUsed = i;
        }

        /**
         * @param b
         */
        public void setOnlyPavement(boolean b) {
            onlyPavement = b;
        }

        /**
         * @param i
         */
        public void setOverallMoveType(int i) {
            overallMoveType = i;
        }

        /**
         * @param b
         */
        public void setPastDanger(boolean b) {
            pastDanger = b;
        }

        /**
         * @param coords
         */
        public void setPosition(Coords coords) {
            position = coords;
        }

        /**
         * @param b
         */
        public void setPrevStepOnPavement(boolean b) {
            prevStepOnPavement = b;
        }

        /**
         * @param i
         */
        public void setTargetNumberMASC(int i) {
            targetNumberMASC = i;
        }

        /**
         * @param b
         */
        public void setThisStepBackwards(boolean b) {
            thisStepBackwards = b;
        }
        
        public Object clone() {
            try {
                return super.clone();
            } catch (CloneNotSupportedException e) {
                e.printStackTrace();
                return null;
            }
        }


        /**
         * @return
         */
        public int getMp() {
            return mp;
        }

        /**
         * @param i
         */
        public void setMp(int i) {
            mp = i;
        }

    }
    
    /**
     * A single step in the entity's movment.
     */
    public class Step
        implements Serializable
    {
        private int type = 0;
    	private int targetId = Entity.NONE;
    	private int targetType = Targetable.TYPE_ENTITY;
        
        private MovementState state = new MovementState();

        private Step prev = null;
        private MovementData parent = null;
        
        /**
         * Create a step of the given type.
         *
         * @param       type - should match one of the MovementData constants,
         *              but this is not currently checked.
         */
        public Step(int type) {
            this.type = type;
        }

        /**
         * Create a step with the given target.
         *
         * @param       type - should match one of the MovementData constants,
         *              but this is not currently checked.
         * @param       target - the <code>Targetable</code> that is the target
         *              of this step. For example, the enemy being charged.
         */
        public Step( int type, Targetable target ) {
            this(type);
            this.targetId = target.getTargetId();
            this.targetType = target.getTargetType();
        }


        public String toString() {
            switch (type) {
            case MovementData.STEP_BACKWARDS:return "B";	
            case MovementData.STEP_CHARGE:return "Ch";	
            case MovementData.STEP_DFA:return "DFA";	
            case MovementData.STEP_FORWARDS:return "F";	
            case MovementData.STEP_GET_UP:return "Up";	
            case MovementData.STEP_GO_PRONE:return "Prone";	
            case MovementData.STEP_START_JUMP:return "StrJump";	
            case MovementData.STEP_TURN_LEFT:return "L";	
            case MovementData.STEP_TURN_RIGHT:return "R";	
            case MovementData.STEP_LATERAL_LEFT:return "ShL";	
            case MovementData.STEP_LATERAL_RIGHT:return "ShR";	
            case MovementData.STEP_LATERAL_LEFT_BACKWARDS:return "ShLB";
            case MovementData.STEP_LATERAL_RIGHT_BACKWARDS:return "ShRB";
            case MovementData.STEP_UNJAM_RAC:return "Unjam";	
	    case MovementData.STEP_LOAD:return "Load";	
	    case MovementData.STEP_UNLOAD:return "Unload";	
	    case MovementData.STEP_EJECT:return "Eject";	
            }
            return"";
        }
        
        public int getType() {
            return type;
        }

        /**
         * Set the target of the current step.
         *
         * @param       target - the <code>Targetable</code> that is the target
         *              of this step. For example, the enemy being charged.
         *              If there is no target, pass a <code>null</code>
         */
        public void setTarget( Targetable target ) {
            if ( target == null ) {
                this.targetId = Entity.NONE;
                this.targetType = Targetable.TYPE_ENTITY;
            } else {
                this.targetId = target.getTargetId();
                this.targetType = target.getTargetType();
            }
        }

        /**
         * Get the target of the current step.
         *
         * @param       game - The <code>Game</code> object.
         * @return      The <code>Targetable</code> that is the target of
         *              this step. For example, the enemy being charged.
         *              This value may be <code>null</code>
         */
        public Targetable getTarget( Game game ) {
            if ( this.targetId == Entity.NONE ) {
                return null;
            }
            return game.getTarget( this.targetType, this.targetId );
        }


        
        public void clearAllFlags() {
            this.state = new MovementState();
        }
        
        public void setStateFromPrev() {
            // if there's no previous step, then we can't
            if (prev == null) {
                return;
            }
            state.setFromPrev(prev.getState());
        }
        
        public Object clone() {
            Step other = new Step(this.type);
            
            other.targetId = this.targetId;
            other.targetType = this.targetType;
            other.state = (MovementState)this.state.clone();
            
            return other;
        }
        
        /**
         * Steps are equal if everything about them is equal
         */
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            } else if (object == null || getClass() != object.getClass()) {
                return false;
            }
            Step other = (Step)object;
            return other.type == this.type
                && other.targetId == this.targetId
                && other.targetType == this.targetType
                && other.state.equals(this.state);
        }
        /**
         * @return
         */
        public MovementState getState() {
            return state;
        }

        /**
         * @param state
         */
        public void setState(MovementState state) {
            this.state = state;
        }

        /**
         * @return
         */
        public int getDistance() {
            return state.getDistance();
        }

        /**
         * @return
         */
        public int getElevation() {
            return state.getElevation();
        }

        /**
         * @return
         */
        public int getFacing() {
            return state.getFacing();
        }

        /**
         * @return
         */
        public Coords getLastPos() {
            return state.getLastPos();
        }

        /**
         * @return
         */
        public int getMovementType() {
            return state.getMovementType();
        }

        /**
         * @return
         */
        public int getMpUsed() {
            return state.getMpUsed();
        }

        /**
         * @return
         */
        public int getOverallMoveType() {
            return state.getOverallMoveType();
        }

        /**
         * @return
         */
        public Coords getPosition() {
            return state.getPosition();
        }

        /**
         * @return
         */
        public int getTargetNumberMASC() {
            return state.getTargetNumberMASC();
        }

        /* (non-Javadoc)
         * @see java.lang.Object#hashCode()
         */
        public int hashCode() {
            return state.hashCode();
        }

        /**
         * @return
         */
        public boolean isDanger() {
            return state.isDanger();
        }

        /**
         * @return
         */
        public boolean isFirstStep() {
            return state.isFirstStep();
        }

        /**
         * @return
         */
        public boolean isHasJustStood() {
            return state.isHasJustStood();
        }

        /**
         * @return
         */
        public boolean isInfantry() {
            return state.isInfantry();
        }

        /**
         * @return
         */
        public boolean isJumping() {
            return state.isJumping();
        }

        /**
         * @return
         */
        public boolean isLastWasBackwards() {
            return state.isLastWasBackwards();
        }

        /**
         * @return
         */
        public boolean isLegal() {
            return state.isLegal();
        }

        /**
         * @return
         */
        public boolean isOnlyPavement() {
            return state.isOnlyPavement();
        }

        /**
         * @return
         */
        public boolean isPastDanger() {
            return state.isPastDanger();
        }

        /**
         * @return
         */
        public boolean isPavementStep() {
            return state.isPavementStep();
        }

        /**
         * @return
         */
        public boolean isPrevStepOnPavement() {
            return state.isPrevStepOnPavement();
        }

        /**
         * @return
         */
        public boolean isProne() {
            return state.isProne();
        }

        /**
         * @return
         */
        public boolean isRunProhibited() {
            return state.isRunProhibited();
        }

        /**
         * @return
         */
        public boolean isThisStepBackwards() {
            return state.isThisStepBackwards();
        }

        /**
         * @return
         */
        public boolean isTurning() {
            return state.isTurning();
        }

        /**
         * @return
         */
        public boolean isUnloaded() {
            return state.isUnloaded();
        }

        /**
         * @return
         */
        public boolean isUsingManAce() {
            return state.isUsingManAce();
        }

        /**
         * @return
         */
        public boolean isUsingMASC() {
            return state.isUsingMASC();
        }

        /**
         * @param b
         */
        public void setDanger(boolean b) {
            state.setDanger(b);
        }

        /**
         * @param i
         */
        public void setDistance(int i) {
            state.setDistance(i);
        }

        /**
         * @param i
         */
        public void setElevation(int i) {
            state.setElevation(i);
        }

        /**
         * @param i
         */
        public void setFacing(int i) {
            state.setFacing(i);
        }

        /**
         * @param b
         */
        public void setFirstStep(boolean b) {
            state.setFirstStep(b);
        }

        /**
         * @param b
         */
        public void setHasJustStood(boolean b) {
            state.setHasJustStood(b);
        }

        /**
         * @param b
         */
        public void setInfantry(boolean b) {
            state.setInfantry(b);
        }

        /**
         * @param b
         */
        public void setJumping(boolean b) {
            state.setJumping(b);
        }

        /**
         * @param coords
         */
        public void setLastPos(Coords coords) {
            state.setLastPos(coords);
        }

        /**
         * @param b
         */
        public void setLastWasBackwards(boolean b) {
            state.setLastWasBackwards(b);
        }

        /**
         * @param b
         */
        public void setLegal(boolean b) {
            state.setLegal(b);
        }

        /**
         * @param i
         */
        public void setMovementType(int i) {
            state.setMovementType(i);
        }

        /**
         * @param i
         */
        public void setMpUsed(int i) {
            state.setMpUsed(i);
        }

        /**
         * @param b
         */
        public void setOnlyPavement(boolean b) {
            state.setOnlyPavement(b);
        }

        /**
         * @param i
         */
        public void setOverallMoveType(int i) {
            state.setOverallMoveType(i);
        }

        /**
         * @param b
         */
        public void setPastDanger(boolean b) {
            state.setPastDanger(b);
        }

        /**
         * @param b
         */
        public void setPavementStep(boolean b) {
            state.setPavementStep(b);
        }

        /**
         * @param coords
         */
        public void setPosition(Coords coords) {
            state.setPosition(coords);
        }

        /**
         * @param b
         */
        public void setPrevStepOnPavement(boolean b) {
            state.setPrevStepOnPavement(b);
        }

        /**
         * @param b
         */
        public void setProne(boolean b) {
            state.setProne(b);
        }

        /**
         * @param b
         */
        public void setRunProhibited(boolean b) {
            state.setRunProhibited(b);
        }

        /**
         * @param i
         */
        public void setTargetNumberMASC(int i) {
            state.setTargetNumberMASC(i);
        }

        /**
         * @param b
         */
        public void setThisStepBackwards(boolean b) {
            state.setThisStepBackwards(b);
        }

        /**
         * @param b
         */
        public void setTurning(boolean b) {
            state.setTurning(b);
        }

        /**
         * @param b
         */
        public void setUnloaded(boolean b) {
            state.setUnloaded(b);
        }

        /**
         * @param b
         */
        public void setUsingManAce(boolean b) {
            state.setUsingManAce(b);
        }

        /**
         * @param b
         */
        public void setUsingMASC(boolean b) {
            state.setUsingMASC(b);
        }

        /**
         * @return
         */
        public Step getPrev() {
            return prev;
        }

    }
}
