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
public class MovePath
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
     * Generates a new, empty, movement path object.
     */
    public MovePath() {
        ;
    }
    
    /**
     * Generates a new movement path object that is a copy
     * of the specified movement path.
     * 
     * @param md the movement path to copy.
     */
    public MovePath(MovePath md) {
        this();
        append(md);
    }
    
    public String toString() {
        StringBuffer sb = new StringBuffer();
        for (Enumeration i =steps.elements();i.hasMoreElements();) {
            sb.append((MoveStep)i.nextElement());	
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
     * Add a new step to the movement path.
     * 
     * @param type the type of movement.
     */
    public void addStep(int type) {
        addStep(new MoveStep(this, type));
    }

    /**
     * Add a new step to the movement path with the given target.
     * 
     * @param type the type of movement.
     * @param target the <code>Targetable</code> object that is the target
     *          of this step. For example, the enemy being charged.
     */
    public void addStep(int type, Targetable target) {
        addStep(new MoveStep(this, type, target));
    }
    
    /**
     * Initializes a step as part of this movement path.  Then adds
     * it to the list.
     * @param step
     */
    protected void addStep(MoveStep step) {
        if (steps.size() > 0) {
            step.setPrev((MoveStep)steps.elementAt(steps.size() - 1));
        }
        steps.addElement(step);
    }

    public Enumeration getSteps() {
        return steps.elements();
    }
    
    public MoveStep getStep(int index) {
        return (MoveStep)steps.elementAt(index);
    }
    
    /**
     * Appends the specified movement path onto the end of the
     * current path.
     * 
     * @param md the movement path to append.
     */
    public void append(MovePath md) {
        for (final Enumeration i = md.getSteps(); i.hasMoreElements();) {
            addStep((MoveStep)i.nextElement());
        }
        compiled = false;
    }
    
    /**
     * Returns a new movement path object representing the path
     * that would result if you appended the curent movement
     * path to the path specified.
     * 
     * @param md the movement path to append.
     */
    public MovePath getAppended(MovePath md) {
        MovePath newMd = new MovePath(this);
        newMd.append(md);
        return newMd;
    }
  
    /**
     * Check for any of the specified type of step in the path
     */
    public boolean contains(int type) {
        for (final Enumeration i = getSteps(); i.hasMoreElements();) {
            MoveStep step = (MoveStep)i.nextElement();
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
            MoveStep step = (MoveStep)i.nextElement();
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
            final MoveStep step = (MoveStep)i.nextElement();
            step.clearAllFlags();
        }
    }
    
    /**
     * Returns the final coordinates if a mech were to perform all the steps
     * in this path.
     */
    public Coords getFinalCoords(Coords start, int facing) {
        int curFacing = facing;
        Coords curPos = new Coords(start);
        for (final Enumeration i = getSteps(); i.hasMoreElements();) {
            final MoveStep step = (MoveStep)i.nextElement();
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
     * in this path.
     */
    public int getFinalFacing(int facing) {
        for (final Enumeration i = getSteps(); i.hasMoreElements();) {
            final MoveStep step = (MoveStep)i.nextElement();
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
            final MoveStep step = (MoveStep)i.nextElement();
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
            final MoveStep step = (MoveStep)i.nextElement();
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
            final MoveStep step = (MoveStep)i.nextElement();
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
            steps.insertElementAt(new MoveStep(this, lateralShiftForTurn(stepType)), index);
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
            steps.insertElementAt(new MoveStep(this, lateralShiftBackwardsForTurn(stepType)), index);
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
            
            if (oppositeTurn(step1, step3) && step2 == MovePath.STEP_FORWARDS) {
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
            
            if (oppositeTurn(step1, step3) && step2 == MovePath.STEP_BACKWARDS) {
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
            case MovePath.STEP_TURN_LEFT :
                return turn2 == MovePath.STEP_TURN_RIGHT;
            case MovePath.STEP_TURN_RIGHT :
                return turn2 == MovePath.STEP_TURN_LEFT;
            default :
                return false;
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
     * Returns the lateral shift backwards that corresponds to the turn direction
     */
    public static int lateralShiftBackwardsForTurn(int turn) {
        switch (turn) {
            case MovePath.STEP_TURN_LEFT :
                return MovePath.STEP_LATERAL_RIGHT_BACKWARDS;
            case MovePath.STEP_TURN_RIGHT :
                return MovePath.STEP_LATERAL_LEFT_BACKWARDS;
            default :
                return turn;
        }
    }
    
    /**
     * Returns the turn direction that corresponds to the lateral shift 
     */
    public static int turnForLateralShift(int shift) {
        switch (shift) {
            case MovePath.STEP_LATERAL_LEFT :
                return MovePath.STEP_TURN_LEFT;
            case MovePath.STEP_LATERAL_RIGHT :
                return MovePath.STEP_TURN_RIGHT;
            default :
                return shift;
        }
    }
    
    /**
     * Returns the turn direction that corresponds to the lateral shift backwards
     */
    public static int turnForLateralShiftBackwards(int shift) {
        switch (shift) {
            case MovePath.STEP_LATERAL_LEFT_BACKWARDS :
                return MovePath.STEP_TURN_RIGHT;
            case MovePath.STEP_LATERAL_RIGHT_BACKWARDS :
                return MovePath.STEP_TURN_LEFT;
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
            final MoveStep step = (MoveStep)i.nextElement();
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
            final MoveStep step = (MoveStep)i.nextElement();
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
    
}
