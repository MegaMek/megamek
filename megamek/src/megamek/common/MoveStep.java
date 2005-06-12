/*
 * MegaMek -
 * Copyright (C) 2000,2001,2002,2003,2004,2005 Ben Mazur (bmazur@sev.org)
 * 
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place, Suite 330, Boston, MA 02111-1307 USA
 */

/*
 * Created on Aug 28, 2003
 */
package megamek.common;

import java.io.Serializable;
import java.util.Enumeration;

/**
 * A single step in the entity's movment.
 */
public class MoveStep implements Serializable {
    private int type = 0;
    private int targetId = Entity.NONE;
    private int targetType = Targetable.TYPE_ENTITY;

    private Coords position;
    private int facing;

    private int mp; // this step
    private int mpUsed; // whole path

    private int heat; //this step
    private int totalHeat;

    private int distance;
    
    private int elevation=-999;

    /**
     * This step's static movement type.  Additional
     * steps in the path will not change this value.
     */
    private int movementType;

    private boolean isProne;

    private boolean danger; // keep psr
    private boolean pastDanger;
    private boolean isUsingMASC;
    private int targetNumberMASC; // psr
    //
    private boolean firstStep; // check if no previous
    private boolean isTurning; // method
    private boolean isUnloaded;
    private boolean hasEverUnloaded;
    private boolean prevStepOnPavement; // prev
    private boolean hasJustStood;
    private boolean thisStepBackwards;
    private boolean onlyPavement; // additive
    private boolean isPavementStep;
    private boolean isRunProhibited = false;
    private boolean isStackingViolation = false;
    private MovePath parent = null;

    /**
     * Flag that indicates that this step is into prohibited terrain.
     * <p/>
     * If the unit is jumping, this step is only invalid if it is the
     * end of the path.
     */
    private boolean terrainInvalid = false;

    /**
     * Flag that indicates that this step's position is the end of a path.
     */
    private boolean isEndPos = true;

    /**
     * Create a step of the given type.
     * 
     * @param type -
     *            should match one of the MovePath constants, but this is not
     *            currently checked.
     */
    public MoveStep(MovePath path, int type) {
        this.type = type;
        this.parent = path;
        if (type==MovePath.STEP_UNLOAD) {
            hasEverUnloaded=true;
        } else {
            hasEverUnloaded=false;
        }
    }

    /**
     * Create a step with the given target.
     * 
     * @param type -
     *            should match one of the MovePath constants, but this is not
     *            currently checked.
     * @param target -
     *            the <code>Targetable</code> that is the target of this
     *            step. For example, the enemy being charged.
     */
    public MoveStep(MovePath path, int type, Targetable target) {
        this(path, type);
        this.targetId = target.getTargetId();
        this.targetType = target.getTargetType();
        if (type==MovePath.STEP_UNLOAD) {
            hasEverUnloaded=true;
        } else {
            hasEverUnloaded=false;
        }
    }

    void setParent(MovePath path) {
        parent = path;
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
            case MovePath.STEP_UP :
                return "U";
            case MovePath.STEP_DOWN :
                return "D";
            default :
                return "???";
        }
    }

    public int getType() {
        return type;
    }
    
    public MovePath getParent() {
        return parent;
    }

    /**
     * Set the target of the current step.
     * 
     * @param target -
     *            the <code>Targetable</code> that is the target of this
     *            step. For example, the enemy being charged. If there is no
     *            target, pass a <code>null</code>
     */
    public void setTarget(Targetable target) {
        if (target == null) {
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
     * @param game -
     *            The <code>Game</code> object.
     * @return The <code>Targetable</code> that is the target of this step.
     *         For example, the enemy being charged. This value may be <code>null</code>
     */
    public Targetable getTarget(IGame game) {
        if (this.targetId == Entity.NONE) {
            return null;
        }
        return game.getTarget(this.targetType, this.targetId);
    }

    /**
     * Compile the static move data for this step.
     *
     * @param   game the <code>Game</code> being played.
     * @param   entity the <code>Entity</code> taking this step.
     * @param   prev the previous step in the path.
     */
    protected void compile(final IGame game, final Entity entity, MoveStep prev)
    {
        final boolean isInfantry = entity instanceof Infantry;
        copy(game, prev);

        // Is this the first step?
        if (prev == null) {
            setFirstStep( true );
            prev = new MoveStep(parent, MovePath.STEP_FORWARDS);
            prev.setFromEntity(entity, game);
        }

        switch (getType()) {
            case MovePath.STEP_UNLOAD :
                // Infantry in immobilized transporters get
                // a special "unload stranded" game turn.
                hasEverUnloaded=true;
                break;
            case MovePath.STEP_LOAD :
                setMp(1);
                break;
            case MovePath.STEP_TURN_LEFT :
            case MovePath.STEP_TURN_RIGHT :
                // Check for pavement movement.
                if (Compute.canMoveOnPavement(game, prev.getPosition(),
                                              getPosition())) {
                    setPavementStep(true);
                } else {
                    setPavementStep(false);
                    setOnlyPavement(false);
                }

                // Infantry can turn for free.
                setMp( ( parent.isJumping() || isHasJustStood()
                         || isInfantry ) ? 0 : 1 );
                adjustFacing(getType());
                break;
            case MovePath.STEP_FORWARDS :
            case MovePath.STEP_BACKWARDS :
            case MovePath.STEP_CHARGE :
            case MovePath.STEP_DFA :
                // step forwards or backwards
                if (getType() == MovePath.STEP_BACKWARDS) {
                    moveInDir((getFacing() + 3) % 6);
                    setThisStepBackwards(true);
                    setRunProhibited(true);
                } else {
                    moveInDir(getFacing());
                    setThisStepBackwards(false);
                }

                // Check for pavement movement.
                if (Compute.canMoveOnPavement(game, prev.getPosition(),
                                              getPosition())) {
                    setPavementStep(true);
                } else {
                    setPavementStep(false);
                    setOnlyPavement(false);
                }

                calcMovementCostFor(game, prev.getPosition());

                // check for water
                if (!isPavementStep()
                    && game.getBoard().getHex(getPosition()).terrainLevel(Terrains.WATER) > 0
                    && entity.getMovementMode() != IEntityMovementMode.HOVER && !(entity instanceof VTOL)) {
                    setRunProhibited(true);
                }
                setHasJustStood(false);
                if (prev.isThisStepBackwards() != isThisStepBackwards()) {
                    setDistance(0); //start over after shifting gears
                }
                addDistance(1);
                if(entity instanceof VTOL) {
                    setElevation(((VTOL)entity).calcElevation(game.getBoard().getHex(prev.getPosition()),game.getBoard().getHex(getPosition()),elevation));
                }
                break;
            case MovePath.STEP_LATERAL_LEFT :
            case MovePath.STEP_LATERAL_RIGHT :
            case MovePath.STEP_LATERAL_LEFT_BACKWARDS :
            case MovePath.STEP_LATERAL_RIGHT_BACKWARDS :
                if (getType() == MovePath.STEP_LATERAL_LEFT_BACKWARDS
                    || getType() == MovePath.STEP_LATERAL_RIGHT_BACKWARDS) {
                    moveInDir(
                        (MovePath
                            .getAdjustedFacing(
                                getFacing(),
                                MovePath.turnForLateralShift(getType()))
                            + 3)
                            % 6);
                    setThisStepBackwards(true);
                    setRunProhibited(true);
                } else {
                    moveInDir(
                        MovePath.getAdjustedFacing(
                            getFacing(),
                            MovePath.turnForLateralShift(getType())));
                    setThisStepBackwards(false);
                }

                // Check for pavement movement.
                if (Compute.canMoveOnPavement(game, prev.getPosition(),
                                              getPosition())) {
                    setPavementStep(true);
                } else {
                    setPavementStep(false);
                    setOnlyPavement(false);
                }

                calcMovementCostFor(game, prev.getPosition());
                setMp(getMp() + 1);
                // check for water
                if (!isPavementStep() &&
                    game.getBoard().getHex(getPosition()).terrainLevel(Terrains.WATER) > 0 &&
                    !(entity instanceof VTOL)) {
                    setRunProhibited(true);
                }
                setHasJustStood(false);
                if (prev.isThisStepBackwards() != isThisStepBackwards()) {
                    setDistance(0); //start over after shifting gears
                }
                addDistance(1);
                break;
            case MovePath.STEP_GET_UP :
                // mechs with 1 MP are allowed to get up
                setMp(entity.getRunMP() == 1 ? 1 : 2);
                setHasJustStood(true);
                break;
            case MovePath.STEP_GO_PRONE :
                setMp(1);
                break;
            case MovePath.STEP_START_JUMP :
                break;
            case MovePath.STEP_UP :
                setElevation(elevation+1);
                setMp(1);
                break;
            case MovePath.STEP_DOWN : 
                setElevation(elevation-1);
                setMp(1);
                break;
            default :
                setMp(0);
        }

        // Update the entity's total MP used.
        addMpUsed(getMp());

        // Check for fire in the new hex.
        if (game.getBoard().getHex(getPosition()).containsTerrain(Terrains.FIRE)) {
            heat = 2;
            totalHeat += 2;
        }

        // Check for a stacking violation.
        final Entity violation =
            Compute.stackingViolation( game, entity.getId(), getPosition() );
        if ( violation != null
             && getType() != MovePath.STEP_CHARGE
             && getType() != MovePath.STEP_DFA ) {
            setStackingViolation(true);
        }

        // set moveType, illegal, trouble flags
        this.compileIllegal(game, entity, prev);
    }

    /**
     * Returns whether the two step types contain opposite turns
     */
    boolean oppositeTurn(MoveStep turn2) {
        switch (type) {
            case MovePath.STEP_TURN_LEFT :
                return turn2.getType() == MovePath.STEP_TURN_RIGHT;
            case MovePath.STEP_TURN_RIGHT :
                return turn2.getType() == MovePath.STEP_TURN_LEFT;
            default :
                return false;
        }
    }
    
    public void setElevation(int el) {
        elevation=el;
    }

    /**
     * Takes the given state as the previous state and sets flags from it.
     * 
     * @param state
     */
    public void copy(final IGame game, MoveStep prev) {
        if (prev == null) {
            setFromEntity(parent.getEntity(), game);
            return;
        }
        this.hasJustStood = prev.hasJustStood;
        this.facing = prev.getFacing();
        this.position = prev.getPosition();

        this.distance = prev.getDistance();
        this.mpUsed = prev.mpUsed;
        this.totalHeat = prev.totalHeat;
        this.isPavementStep = prev.isPavementStep;
        this.onlyPavement = prev.onlyPavement;
        this.thisStepBackwards = prev.thisStepBackwards;
        this.isProne = prev.isProne;
        this.isRunProhibited = prev.isRunProhibited;
        this.hasEverUnloaded = prev.hasEverUnloaded;
        this.elevation = prev.elevation;
    }

    /**
     * Sets this state as coming from the entity.
     * 
     * @param entity
     */
    public void setFromEntity(Entity entity, IGame game) {
        this.position = entity.getPosition();
        this.facing = entity.getFacing();
        // elevation
        this.mpUsed = entity.mpUsed;
        this.distance = entity.delta_distance;
        this.isProne = entity.isProne();
        
        this.elevation = (entity instanceof VTOL) ? ((VTOL)entity).getElevation() : -999;

        // check pavement
        if (position != null) {
            IHex curHex = game.getBoard().getHex(position);
            if (curHex.hasPavement()) {
                onlyPavement = true;
                isPavementStep = true;
            }
        }
    }

    /**
     * Adjusts facing to comply with the type of step indicated.
     * 
     * @param stepType
     */
    public void adjustFacing(int stepType) {
        facing = MovePath.getAdjustedFacing(facing, stepType);
    }

    /**
     * Moves the position one hex in the direction indicated. Does not change
     * facing.
     * 
     * @param dir
     */
    public void moveInDir(int dir) {
        position = position.translated(dir);
        if (!parent.game.getBoard().contains(position)) {
            throw new RuntimeException("Coordinate off the board.");
        }
    }

    /**
     * Adds a certain amount to the distance parameter.
     * 
     * @param increment
     */
    public void addDistance(int increment) {
        distance += increment;
    }

    /**
     * Adds a certain amount to the mpUsed parameter.
     * 
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
    public boolean isUsingMASC() {
        return isUsingMASC;
    }

    /**
     * Determine if this is a legal step.
     *
     * @return <code>true</code> if the step is legal.  <code>false</code>
     *          otherwise.
     */
    public boolean isLegal() {
        // A step is legal if it's static movement type is not illegal,
        // and it is either a valid end position, or not an end position.
        return ( movementType != IEntityMovementType.MOVE_ILLEGAL
                 && (isLegalEndPos() || !isEndPos) );
    }

    /**
     * Return this step's movement type.
     *
     * @return the <code>int</code> constant for this step's movement type.
     */
    public int getMovementType() {
        int moveType = movementType;

        // If this step's position is the end of the path, and it is not
        // a valid end postion, then the movement type is "illegal".
        if (!isLegalEndPos() && isEndPos) {
            moveType = IEntityMovementType.MOVE_ILLEGAL;
        }
        return moveType;
    }

    /**
     * Check to see if this step's position is a valid end of a path.
     *
     * @return <code>true</code> if this step's position is a legal end
     *          of a path.  If the step is not legal for an end of a path,
     *          then <code>false</code> is returned.
     */
    public boolean isLegalEndPos() {
        // Can't be a stacking violation.
        boolean legal = true;
        if (isStackingViolation) {
            legal = false;
        }

        // Can't be into invalid terrain.
        else if (terrainInvalid) {
            legal = false;
        }

        // Can't jump zero hexes.
        else if (parent.isJumping() && distance == 0) {
            legal = false;
        }
        
        // Can't be after unloading BA/inf
        else if (hasEverUnloaded && this.type != MovePath.STEP_UNLOAD) {
            legal = false;
        }
        
        return legal;
    }

    /**
     * Update this step's status as the ending position of a path.
     *
     * @param isEnd the <code>boolean</code> flag that specifies that this
     *          step's position is the end of a path.
     * @return <code>true</code> if the path needs to keep updating the steps.
     *         <code>false</code> if the update of the path is complete.
     *
     * @see <code>#isLegalEndPos()</code>
     * @see <code>#isEndPos</code>
     * @see <code>MovePath#addStep( MoveStep )</code>
     */
    public boolean setEndPos( boolean isEnd ) {
        // A step that is always illegal is always the end of the path.
        if ( IEntityMovementType.MOVE_ILLEGAL == movementType ) isEnd = true;

        // If this step didn't already know it's status as the ending
        // position of a path, then there are more updates to do.
        boolean moreUpdates = (this.isEndPos != isEnd);
        this.isEndPos = isEnd;
        return moreUpdates;
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
    public void setTurning(boolean b) {
        isTurning = b;
    }

    /**
     * @param b
     */
    public void setUnloaded(boolean b) {
        isUnloaded = b;
        if (b) {
            hasEverUnloaded=true;
        }
    }

    /**
     * @param b
     */
    public void setUsingMASC(boolean b) {
        isUsingMASC = b;
    }

    /**
     * @param i
     */
    public void setMovementType(int i) {
        movementType = i;
    }

    /**
     * @param b
     */
    public void setOnlyPavement(boolean b) {
        onlyPavement = b;
    }

    public void setTargetNumberMASC(int i) {
        targetNumberMASC = i;
    }

    public void setThisStepBackwards(boolean b) {
        thisStepBackwards = b;
    }

    public int getMp() {
        return mp;
    }

    public void setMp(int i) {
        mp = i;
    }

    void setRunProhibited(boolean isRunProhibited) {
        this.isRunProhibited = isRunProhibited;
    }

    boolean isRunProhibited() {
        return isRunProhibited;
    }

    void setStackingViolation(boolean isStackingViolation) {
        this.isStackingViolation = isStackingViolation;
    }

    boolean isStackingViolation() {
        return isStackingViolation;
    }


    void compileIllegal(final IGame game, final Entity entity,
                        final MoveStep prev) {
        final int stepType = getType();
        final boolean isInfantry = entity instanceof Infantry;

        Coords curPos = getPosition();
        Coords lastPos = prev.getPosition();
        boolean isUnjammingRAC = entity.isUnjammingRAC();

        // Infantry get a first step if all they've done is spin on the spot.
        if (isInfantry && (getMpUsed() - getMp()) == 0) {
            setFirstStep( true );

            //   getMpUsed() is the MPs used in the whole MovePath
            //   getMp() is the MPs used in the last (illegal) step (this step)
            //   if the difference between the whole path and this step is 0
            //   then this must be their first step
            // TODO : Why are these *here*???
            prevStepOnPavement = prev.isPavementStep();
            isTurning = prev.isTurning();
            isUnloaded = prev.isUnloaded();
        }

        // guilty until proven innocent
        movementType = IEntityMovementType.MOVE_ILLEGAL;

        // check for ejection (always legal?)
        if (type == MovePath.STEP_EJECT) {
            movementType = IEntityMovementType.MOVE_NONE;
        }

        // check for valid jump mp
        if ( parent.isJumping()
             && getMpUsed() <= entity.getJumpMPWithTerrain()
             && !isProne()
             && !( entity instanceof Protomech
                   && (entity.getInternal(Protomech.LOC_LEG)
                       == IArmorState.ARMOR_DESTROYED) )
             && !entity.isStuck() ) {
            movementType = IEntityMovementType.MOVE_JUMP;
        }
        
        // legged Protos may make one facing change
        if (isFirstStep()
            && entity instanceof Protomech
            && (entity.getInternal(Protomech.LOC_LEG)
                == IArmorState.ARMOR_DESTROYED)
            && (stepType == MovePath.STEP_TURN_LEFT 
                || stepType == MovePath.STEP_TURN_RIGHT)
            && !entity.isStuck()) {
            movementType = IEntityMovementType.MOVE_WALK;
        }            

        // check for valid walk/run mp
        if (!parent.isJumping()
            && !entity.isStuck()
            && entity.getWalkMP() > 0
            && (!isProne()
                || parent.contains(MovePath.STEP_GET_UP)
                || stepType == MovePath.STEP_TURN_LEFT
                || stepType == MovePath.STEP_TURN_RIGHT)) {

            if (getMpUsed() <= entity.getWalkMP()) {
                movementType = IEntityMovementType.MOVE_WALK;

            // Vehicles moving along pavement get "road bonus" of 1 MP.
            // N.B. The Ask Precentor Martial forum said that a 4/6
            //      tank on a road can move 5/7, **not** 5/8.
            } else if (entity instanceof Tank && !(entity instanceof VTOL) && isOnlyPavement()
                       && getMpUsed() == entity.getWalkMP() + 1) {
                movementType = IEntityMovementType.MOVE_WALK;
                // store if we got the pavement Bonus for end of phase
                // gravity psr
                entity.gotPavementBonus = true;
            } else if (getMpUsed() <= entity.getRunMPwithoutMASC()
                       && !isRunProhibited()) {
                movementType = IEntityMovementType.MOVE_RUN;
            } else if (getMpUsed() <= entity.getRunMP()
                       && !isRunProhibited()) {
                setUsingMASC(true);
                Mech m = (Mech) entity;
                setTargetNumberMASC(m.getMASCTarget());
                movementType = IEntityMovementType.MOVE_RUN;
            } else if (
                entity instanceof Tank
                    && !(entity instanceof VTOL)
                    && isOnlyPavement()
                    && getMpUsed() <= (entity.getRunMP() + 1)
                    && !isRunProhibited()) {
                movementType = IEntityMovementType.MOVE_RUN;
                // store if we got the pavement Bonus for end of phase
                // gravity psr
                entity.gotPavementBonus = true;
            }
        }
        /*
        if(MovePath.STEP_DOWN==stepType) {
            if(entity instanceof VTOL) {
                if(!(((VTOL)entity).canGoDown(elevation+1,getPosition()))) {
                    movementType = IMoveType.MOVE_ILLEGAL;//We can't intentionally crash.
                }
            } else {
                movementType = IMoveType.MOVE_ILLEGAL;//only VTOLs can go up and down (and subs, but we don't have any.)
            }
        }
        if(MovePath.STEP_UP==stepType) {
            if(!(entity instanceof VTOL)) {
                movementType = IMoveType.MOVE_ILLEGAL;
            }
        }*///not needed due to isMovementPossible, right?

        // Mechs with busted Gyro may make only one facing change
        if (entity.getBadCriticals(CriticalSlot.TYPE_SYSTEM,
                                   Mech.SYSTEM_GYRO, Mech.LOC_CT) > 1
            && !isFirstStep() ) {
            movementType = IEntityMovementType.MOVE_ILLEGAL;
        }

        // Mechs with 1 MP are allowed to get up, except
        // if they've used that 1MP up already
        if (MovePath.STEP_GET_UP==stepType && 1==entity.getRunMP()
            && entity.mpUsed < 1 && !entity.isStuck()) {
            movementType = IEntityMovementType.MOVE_RUN;
        }

        // amnesty for the first step
        if (isFirstStep()
            && movementType == IEntityMovementType.MOVE_ILLEGAL
            && entity.getWalkMP() > 0
            && !entity.isProne()
            && !entity.isStuck()
            && stepType == MovePath.STEP_FORWARDS) {
            movementType = IEntityMovementType.MOVE_RUN;
        }

        // Is the entity unloading passeners?
        if (stepType == MovePath.STEP_UNLOAD) {
            // Prone Meks are able to unload, if they have the MP.
            if (getMpUsed() <= entity.getRunMP() && entity.isProne()
                && movementType == IEntityMovementType.MOVE_ILLEGAL) {
                movementType = IEntityMovementType.MOVE_RUN;
                if (getMpUsed() <= entity.getWalkMP()) {
                    movementType = IEntityMovementType.MOVE_WALK;
                }
            }

            // Can't unload units into prohibited terrain
            // or into stacking violation.
            Targetable target = getTarget(game);
            if (target instanceof Entity) {
                Entity other = (Entity) target;
                if (null != Compute.stackingViolation(game, other,
                                                      curPos, entity)
                    || other.isHexProhibited(game.getBoard().getHex(curPos))) {
                    movementType = IEntityMovementType.MOVE_ILLEGAL;
                }
            } else {
                movementType = IEntityMovementType.MOVE_ILLEGAL;
            }

        }

        // Can't run or jump if unjamming a RAC.
        if (isUnjammingRAC && (movementType == IEntityMovementType.MOVE_RUN
                               || parent.isJumping())) {
            movementType = IEntityMovementType.MOVE_ILLEGAL;
        }

        // only standing mechs may go prone
        if (stepType == MovePath.STEP_GO_PRONE
            && (isProne() || !(entity instanceof Mech) || entity.isStuck())) {
            movementType = IEntityMovementType.MOVE_ILLEGAL;
        }

        // check if this movement is illegal for reasons other than points
        if (!isMovementPossible(game, lastPos) || isUnloaded) {
            System.out.println(getPosition().getBoardNum() + "Rararaara");
            movementType = IEntityMovementType.MOVE_ILLEGAL;
        }

        // If the previous step is always illegal, then so is this one
        if ( prev != null && IEntityMovementType.MOVE_ILLEGAL == prev.movementType ) {
            movementType = IEntityMovementType.MOVE_ILLEGAL;
        }

        // Don't compute danger if the step is illegal.
        if (movementType == IEntityMovementType.MOVE_ILLEGAL) {
            return;
        }

        danger
            |= Compute.isPilotingSkillNeeded(
                game,
                entity.getId(),
                lastPos,
                curPos,
                movementType,
                isTurning,
                prevStepOnPavement);

        // getting up is also danger
        if (stepType == MovePath.STEP_GET_UP) {
            danger = true;
        }

        // set past danger
        pastDanger |= danger;

        // Record if we're turning *after* check for danger,
        // because the danger lies in moving *after* turn.
        switch (stepType) {
            case MovePath.STEP_TURN_LEFT :
            case MovePath.STEP_TURN_RIGHT :
                setTurning(true);
                break;
            case MovePath.STEP_UNLOAD :
                // Unloading must be the last step.
                setUnloaded(true);
                break;
            default :
                setTurning(false);
                break;
        }

        // update prone state
        if (stepType == MovePath.STEP_GO_PRONE) {
            setProne(true);
        } else if (stepType == MovePath.STEP_GET_UP) {
            setProne(false);
        }
    }

    public int getTotalHeat() {
        return totalHeat;
    }

    public int getHeat() {
        return heat;
    }

    /**
     * Amount of movement points required to move from start to dest
     */
    protected void calcMovementCostFor(IGame game, Coords prev) {
        final int moveType = parent.getEntity().getMovementMode();
        final IHex srcHex = game.getBoard().getHex(prev);
        final IHex destHex = game.getBoard().getHex(getPosition());
        final boolean isInfantry = parent.getEntity() instanceof Infantry;

        mp = 1;

        // jumping always costs 1
        if (parent.isJumping()) {
            return;
        }
        
        // VTOLs pay 1 for everything
        if (parent.getEntity() instanceof VTOL) {
            return;
        }

        // Account for terrain, unless we're moving along a road.
        if (!isPavementStep) {

            if (destHex.terrainLevel(Terrains.ROUGH) > 0) {
                mp++;
            }
            if (destHex.terrainLevel(Terrains.RUBBLE) > 0) {
                mp++;
            }
            if (destHex.terrainLevel(Terrains.WOODS) == 1) {
                mp++;
            } else if (destHex.terrainLevel(Terrains.WOODS) > 1) {
                mp += 2;
            }

            // non-hovers check for water depth and are affected by swamp
            if (moveType != IEntityMovementMode.HOVER) {
                if (destHex.terrainLevel(Terrains.WATER) == 1) {
                    mp++;
                } else if (destHex.terrainLevel(Terrains.WATER) > 1) {
                    mp += 3;
                }
                if (destHex.containsTerrain(Terrains.SWAMP)) {
                    mp++;
                }
            }

        } // End not-along-road

        // account for elevation?
        // TODO: allow entities to occupy different levels of buildings.
        int nSrcEl = parent.getEntity().elevationOccupied(srcHex);
        int nDestEl = parent.getEntity().elevationOccupied(destHex);
        int nMove = parent.getEntity().getMovementMode();

        if (nSrcEl != nDestEl) {
            int delta_e = Math.abs(nSrcEl - nDestEl);

            // Infantry and ground vehicles are charged double.
            if (isInfantry
                || (nMove == IEntityMovementMode.TRACKED
                    || nMove == IEntityMovementMode.WHEELED
                    || nMove == IEntityMovementMode.HOVER)) {
                delta_e *= 2;
            }
            mp += delta_e;
        }

        // If we entering a building, all non-infantry pay additional MP.
        if (nDestEl < destHex.terrainLevel(Terrains.BLDG_ELEV)
            && !(isInfantry)) {
            Building bldg = game.getBoard().getBuildingAt(getPosition());
            mp += bldg.getType();
        }
    }

    /**
     * Is movement possible from a previous position to this one?
     * <p/>
     * This function does not comment on whether an overall movement path
     * is possible, just whether the <em>current</em> step is possible.
     */
    public boolean isMovementPossible(IGame game, Coords src) {
        final IHex srcHex = game.getBoard().getHex(src);
        final Coords dest = this.getPosition();
        final IHex destHex = game.getBoard().getHex(dest);
        final Entity entity = parent.getEntity();

        if (null == dest) {
            throw new IllegalStateException( "Step has no position." );
        }
        if (src.distance(dest) > 1) {
            StringBuffer buf = new StringBuffer();
            buf.append( "Coordinates " )
                .append( src.toString() )
                .append( " and " )
                .append( dest.toString() )
                .append( " are not adjacent." );
            throw new IllegalArgumentException( buf.toString() );
        }

        /* 2004-03-31 : don't look at overall movement, just this step. **
        if (movementType == IMoveType.MOVE_ILLEGAL) {
            // that was easy
            return false;
        }
        /* 2004-03-31 : don't look at overall movement, just this step. */

        // If we're a tank and immobile, check if we try to unjam
        // or eject and the crew is not unconscious
        if ( entity instanceof Tank
             && !entity.getCrew().isUnconscious()
             && ( type == MovePath.STEP_UNJAM_RAC
                  || type == MovePath.STEP_EJECT) ) {
            return true;
        }
        
        // super-easy
        if (entity.isImmobile()) {
            return false;
        }
        // another easy check
        if (!game.getBoard().contains(dest)) {
            return false;
        }

        // Can't back up across an elevation change.
        if ( !(entity instanceof VTOL) && isThisStepBackwards()
             && ( entity.elevationOccupied(destHex)
                  != entity.elevationOccupied(srcHex) ) ) {
            return false;
        }

        // Swarming entities can't move.
        if (Entity.NONE != entity.getSwarmTargetId()) {
            return false;
        }

        // The entity is trying to load. Check for a valid move.
        if (type == MovePath.STEP_LOAD) {

            // Transports can't load after the first step.
            if (!firstStep) {
                return false;
            }

            // Find the unit being loaded.
            Entity other = null;
            Enumeration entities = game.getEntities(src);
            while (entities.hasMoreElements()) {

                // Is the other unit friendly and not the current entity?
                other = (Entity) entities.nextElement();
                if (entity.getOwner() == other.getOwner()
                    && !entity.equals(other)) {

                    // The moving unit should be able to load the other unit.
                    if (!entity.canLoad(other)) {
                        return false;
                    }

                    // The other unit should be able to have a turn.
                    if (!other.isSelectableThisTurn()) {
                        return false;
                    }

                    // We can stop looking.
                    break;
                } else {
                    // Nope. Discard it.
                    other = null;
                }

            } // Check the next entity in this position.

            // We were supposed to find someone to load.
            if (other == null) {
                return false;
            }

        } // End STEP_LOAD-checks

        // mechs dumping ammo can't run
        boolean bDumping = false;
        Enumeration iter = entity.getAmmo();
        while ( iter.hasMoreElements() ) {
            if (((Mounted) iter.nextElement()).isDumping()) {
                bDumping = true;
                break;
            }
        }
        if (bDumping && (movementType == IEntityMovementType.MOVE_RUN
                         || movementType == IEntityMovementType.MOVE_JUMP)) {
            return false;
        }

        // check elevation difference > max
        int nSrcEl = entity.elevationOccupied(srcHex);
        int nDestEl = entity.elevationOccupied(destHex);
        int nMove = entity.getMovementMode();

        if ( movementType != IEntityMovementType.MOVE_JUMP
             && ( Math.abs(nSrcEl - nDestEl)
                  > entity.getMaxElevationChange() ) ) {
            return false;
        }
        // Units moving backwards may not change elevation levels.
        // (Ben thinks this rule is dumb)
        if ((type == MovePath.STEP_BACKWARDS
            || type == MovePath.STEP_LATERAL_LEFT_BACKWARDS
            || type == MovePath.STEP_LATERAL_RIGHT_BACKWARDS)
            && nSrcEl != nDestEl && !(entity instanceof VTOL)) {
            return false;
        }

        // Can't run into water unless hovering, first step, using a bridge, or fly.
        if (movementType == IEntityMovementType.MOVE_RUN
            && nMove != IEntityMovementMode.HOVER
            && destHex.terrainLevel(Terrains.WATER) > 0
            && !firstStep
            && !isPavementStep
            && !(entity instanceof VTOL)) {
            return false;
        }

        // ugh, stacking checks. well, maybe we're immune!
        if ( !parent.isJumping()
             && type != MovePath.STEP_CHARGE
             && type != MovePath.STEP_DFA ) {
            // can't move a mech into a hex with an enemy mech
            if (entity instanceof Mech
                && Compute.isEnemyIn(game, entity.getId(),
                                     dest, true)) {
                return false;
            }

            // Can't move out of a hex with an enemy unit unless we started
            // there, BUT we're allowed to turn, unload, or go prone.
            if ( Compute.isEnemyIn(game, entity.getId(), src, false)
                 && !src.equals(entity.getPosition())
                 && type != MovePath.STEP_TURN_LEFT
                 && type != MovePath.STEP_TURN_RIGHT
                 && type != MovePath.STEP_UNLOAD
                 && type != MovePath.STEP_GO_PRONE ) {
                return false;
            }

        }

        // can't jump over too-high terrain
        if ( movementType == IEntityMovementType.MOVE_JUMP
             && ( destHex.getElevation()
                  > (entity.getElevation()
                     + entity.getJumpMPWithTerrain()) ) ) {
            return false;
        }

        // Certain movement types have terrain restrictions; terrain
        // restrictions are lifted when moving along a road or bridge.
        if (entity.isHexProhibited(destHex) && !isPavementStep()) {

            // We're allowed to pass *over* invalid
            // terrain, but we can't end there.
            if (parent.isJumping()) {
                terrainInvalid = true;
            }
            else {
                // This is an illegal move.
                return false;
            }                
        }

        // If we are *in* restricted terrain, we can only leave via roads.
        if ( movementType != IEntityMovementType.MOVE_JUMP
             && entity.isHexProhibited(srcHex)
             && !isPavementStep ) {
            return false;
        }
        if( movementType == MovePath.STEP_UP) {
            //only VTOLs have Z movement.
            return (entity instanceof VTOL)? true : false;
        }
        if( movementType == MovePath.STEP_DOWN) {
            //only VTOLs have Z movement.
            if(!(entity instanceof VTOL)) {
                return false;
            }
            if(!(((VTOL)entity).canGoDown(elevation+1,getPosition()))) {
                return false;//We can't intentionally crash.
            }
        }
        if(entity instanceof VTOL) {
            if(movementType == MovePath.STEP_BACKWARDS || movementType == MovePath.STEP_FORWARDS || movementType == MovePath.STEP_TURN_LEFT || movementType == MovePath.STEP_TURN_RIGHT) {
                if(elevation==0) {//can't move on the ground.
                    return false;
                }
            }
            if(movementType == MovePath.STEP_BACKWARDS || movementType == MovePath.STEP_FORWARDS) {
                if(elevation<=(destHex.ceiling()-destHex.floor())) {
                    return false;//can't fly into woods or a cliff face
                    }
            }
        }

        return true;
    }

    //Used by BoardView to see if we can re-use an old movement sprite.
    public boolean canReuseSprite(MoveStep other) {
        // Assume that we *can't* reuse the sprite, and prove ourself wrong.
        boolean reuse = false;
        if (this.type == other.type &&
            this.facing == other.facing &&
            this.mpUsed == other.mpUsed &&
            this.movementType == other.movementType &&
            this.isProne == other.isProne &&
            this.danger == other.danger &&
            this.pastDanger == other.pastDanger &&
            this.isUsingMASC == other.isUsingMASC &&
            this.targetNumberMASC == other.targetNumberMASC &&
            this.isPavementStep == other.isPavementStep &&
            this.isLegalEndPos() &&
            other.isLegalEndPos() ) {
            reuse = true;
        }
        return reuse;
    }

    public int getElevation() {
        return elevation;
    }

}
