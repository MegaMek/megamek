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
    public static final int        STEP_FORWARDS        = 1;
    public static final int        STEP_BACKWARDS       = 2;
    public static final int        STEP_TURN_LEFT       = 3;
    public static final int        STEP_TURN_RIGHT      = 4;
    public static final int        STEP_GET_UP          = 5;
    public static final int        STEP_GO_PRONE        = 6;
    public static final int        STEP_START_JUMP      = 7;
    public static final int        STEP_CHARGE          = 8;
    public static final int        STEP_DFA             = 9;
    
    private Vector steps = new Vector();
    
    private transient boolean compiled = false;
    
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
     * A single step in the entity's movment.
     */
    public class Step
        implements Serializable
    {
        private int type;
        
        // these are all set using Compute.compile:
        private transient Coords position;
        private transient int facing;
        private transient int mpUsed;
        private transient int distance;
        private transient int movementType;
        private transient boolean danger;
        private transient boolean pastDanger;
        
        public Step(int type) {
            this.type = type;
        }
        
        public int getType() {
            return type;
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
        
        public void clearAllFlags() {
            Coords position = null;
            int facing = 0;
            int mpUsed = 0;
            int distance = 0;
            int movementType = 0;
            boolean danger = false;
            boolean pastDanger = false;
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
            return other.type == this.type && other.position.equals(this.position)
            && other.facing == this.facing && other.mpUsed == this.mpUsed
            && other.distance == this.distance && other.movementType == this.movementType
            && other.danger == this.danger && other.pastDanger == this.pastDanger;
        }
    }
}
