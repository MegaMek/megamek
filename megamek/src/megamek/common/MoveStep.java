/*
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
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

/*
 * Created on Aug 28, 2003
 */
package megamek.common;

import java.io.Serializable;


/**
 * A single step in the entity's movment.
 */
public class MoveStep
    implements Serializable
{
    private int type = 0;
	private int targetId = Entity.NONE;
	private int targetType = Targetable.TYPE_ENTITY;
    
    private MoveState state = new MoveState();

    private MoveStep prev = null;
    private MovePath parent = null;
    
    /**
     * Create a step of the given type.
     *
     * @param       type - should match one of the MovePath constants,
     *              but this is not currently checked.
     */
    public MoveStep(MovePath path, int type) {
        this.type = type;
        this.parent = path;
    }

    /**
     * Create a step with the given target.
     *
     * @param       type - should match one of the MovePath constants,
     *              but this is not currently checked.
     * @param       target - the <code>Targetable</code> that is the target
     *              of this step. For example, the enemy being charged.
     */
    public MoveStep( MovePath path, int type, Targetable target ) {
        this(path, type);
        this.targetId = target.getTargetId();
        this.targetType = target.getTargetType();
    }


    public String toString() {
        switch (type) {
            case MovePath.STEP_BACKWARDS :
                return "B";
            case MovePath.STEP_CHARGE :
                return "Ch";
            case MovePath.STEP_DFA :
                return "DFA";
            case MovePath.STEP_FORWARDS :
                return "F";
            case MovePath.STEP_GET_UP :
                return "Up";
            case MovePath.STEP_GO_PRONE :
                return "Prone";
            case MovePath.STEP_START_JUMP :
                return "StrJump";
            case MovePath.STEP_TURN_LEFT :
                return "L";
            case MovePath.STEP_TURN_RIGHT :
                return "R";
            case MovePath.STEP_LATERAL_LEFT :
                return "ShL";
            case MovePath.STEP_LATERAL_RIGHT :
                return "ShR";
            case MovePath.STEP_LATERAL_LEFT_BACKWARDS :
                return "ShLB";
            case MovePath.STEP_LATERAL_RIGHT_BACKWARDS :
                return "ShRB";
            case MovePath.STEP_UNJAM_RAC :
                return "Unjam";
            case MovePath.STEP_LOAD :
                return "Load";
            case MovePath.STEP_UNLOAD :
                return "Unload";
            case MovePath.STEP_EJECT :
                return "Eject";
            default :
                return "???";
        }
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
        this.state = new MoveState();
    }
    
    public void setStateFromPrev() {
        // if there's no previous step, then we can't
        if (prev == null) {
            return;
        }
        state.setFromPrev(prev.getState());
    }
    
    public Object clone() {
        MoveStep other = new MoveStep(this.parent, this.type);
        
        other.targetId = this.targetId;
        other.targetType = this.targetType;
        other.state = (MoveState)this.state.clone();
        
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
        MoveStep other = (MoveStep)object;
        return other.type == this.type
            && other.targetId == this.targetId
            && other.targetType == this.targetType
            && other.state.equals(this.state);
    }
    /**
     * @return
     */
    public MoveState getState() {
        return state;
    }

    /**
     * @param state
     */
    public void setState(MoveState state) {
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
    public MoveStep getPrev() {
        return prev;
    }

    /**
     * @param step
     */
    public void setPrev(MoveStep step) {
        prev = step;
    }
}