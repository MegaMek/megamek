/**
 * MegaMek - Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
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
        steps.addElement(new Step(type));
    }

    /**
     * Add a new step to the movement data with the given target.
     * 
     * @param type the type of movement.
     * @param target the <code>Targetable</code> object that is the target
     *          of this step. For example, the enemy being charged.
     */
    public void addStep(int type, Targetable target) {
        steps.addElement(new Step(type, target));
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
            this.steps.addElement(i.nextElement());
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
            if (step.isUsingMASC()) {
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
            if (step.getMovementType() != Entity.MOVE_ILLEGAL) {
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
            mpUsed =+ step.getMpUsed();
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
    
    /**
     * A single step in the entity's movment.
     */
    public class Step
        implements Serializable
    {
        private int type;
	private int targetId;
	private int targetType;
        private boolean onPavement;

        // these are all set using Compute.compile:
        private transient Coords position;
        private transient int facing;
        private transient int mpUsed;
        private transient int distance;
        private transient int movementType;
        private transient boolean danger;
        private transient boolean pastDanger;
        private transient boolean isUsingMASC;
        private transient int targetNumberMASC;

        /**
         * Create a step of the given type.
         *
         * @param       type - should match one of the MovementData constants,
         *              but this is not currently checked.
         */
        public Step(int type) {
            this.type = type;
            this.targetId = Entity.NONE;
            this.targetType = Targetable.TYPE_ENTITY;
            this.onPavement = false;
            this.isUsingMASC = false;
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
            this.type = type;
            this.targetId = target.getTargetId();
            this.targetType = target.getTargetType();
            this.onPavement = false;
            this.isUsingMASC = false;
        }

        /**
         * Create an exact duplicate of another step.
         *
         * @param       other - the <code>Step</code> to be duplicated.
         */
        public Step(Step other) {
            this.type = other.type;
            this.targetId = other.targetId;
            this.targetType = other.targetType;
            this.onPavement = other.onPavement;
            this.position = new Coords(other.position);
            this.facing = other.facing;
            this.mpUsed = other.mpUsed;
            this.distance = other.distance;
            this.movementType = other.movementType;
            this.danger = other.danger;
            this.pastDanger = other.pastDanger;
            this.isUsingMASC = other.isUsingMASC;
            this.targetNumberMASC = other.targetNumberMASC;
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

        /**
         * Specify that the step is from one pavement hex to another.  This
         * includes movement along roads and bridges.
         *
         * @param       pavement - this <code>boolean</code> value should be
         *              <code>true</code> if this step is from one pavement
         *              (or road or bridge) hex to another, <code>false</code>
         *              otherwise.
         */
        public void setOnPavement( boolean pavement ) {
            this.onPavement = pavement;
        }

        /**
         * Determine if the step is from one pavement hex to another.  This
         * includes movement along roads and bridges.
         *
         * @return      <code>true</code> if this step is from one pavement
         *              (or road or bridge) hex to another, <code>false</code>
         *              otherwise.
         */
        public boolean isOnPavement() {
            return this.onPavement;
        }

        public Coords getPosition() {
            return position;
        }
        
        public void setPosition(Coords position) {
            this.position = position;
        }
        
        public int getFacing() {
            return facing;
        }
        
        public void setFacing(int facing) {
            this.facing = facing;
        }
        
        public int getMpUsed() {
            return mpUsed;
        }
        
        public void setMpUsed(int mpUsed) {
            this.mpUsed = mpUsed;
        }
        
        public int getDistance() {
            return distance;
        }
        
        public void setDistance(int distance) {
            this.distance = distance;
        }
        
        public int getMovementType() {
            return movementType;
        }
        
        public void setMovementType(int movementType) {
            this.movementType = movementType;
        }
        
        public boolean isDanger() {
            return danger;
        }
        
        public void setDanger(boolean danger) {
            this.danger = danger;
        }
        
        public boolean isPastDanger() {
            return pastDanger;
        }
        
        public void setPastDanger(boolean pastDanger) {
            this.pastDanger = pastDanger;
        }

        public boolean isUsingMASC() {
            return isUsingMASC;
        }
        
        public void setUsingMASC(boolean usingMASC) {
            this.isUsingMASC = usingMASC;
        }

        public void setMASCNumber(int n) {
            this.targetNumberMASC = n;
        }

        public int getMASCNumber() {
            return targetNumberMASC;
        }
        
        public void clearAllFlags() {
            this.position = null;
            this.facing = 0;
            this.mpUsed = 0;
            this.distance = 0;
            this.movementType = 0;
            this.danger = false;
            this.pastDanger = false;
            this.isUsingMASC = false;
        }
        
        public Object clone() {
            return new Step(this);
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
                && other.position.equals(this.position)
                && other.facing == this.facing
                && other.mpUsed == this.mpUsed
                && other.distance == this.distance
                && other.movementType == this.movementType
                && other.danger == this.danger
                && other.pastDanger == this.pastDanger
                && other.isUsingMASC == this.isUsingMASC;
        }
    }
}
