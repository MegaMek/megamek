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


public class MoveState implements Cloneable, Serializable {
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
    public void setFromPrev(MoveState prev) {
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
       facing = MovePath.getAdjustedFacing(facing, stepType);
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