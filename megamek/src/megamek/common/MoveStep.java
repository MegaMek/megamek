/*
 * MegaMek - Copyright (C) 2000,2001,2002,2003,2004 Ben Mazur (bmazur@sev.org)
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
    private int movementType;
    private boolean isProne;
    //
    private boolean danger; // keep psr
    private boolean pastDanger;
    private boolean isUsingMASC;
    private int targetNumberMASC; // psr
    //
    private boolean firstStep; // check if no previous
    private boolean isTurning; // method
    private boolean isUnloaded;
    private boolean prevStepOnPavement; // prev
    private boolean hasJustStood;
    private boolean thisStepBackwards;
    private boolean onlyPavement; // additive
    private boolean isPavementStep;
    private boolean isRunProhibited = false;
    private boolean isStackingViolation = false;
    private MovePath parent = null;

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
    public Targetable getTarget(Game game) {
        if (this.targetId == Entity.NONE) {
            return null;
        }
        return game.getTarget(this.targetType, this.targetId);
    }

    protected void compile(final Game game, final Entity entity, MoveStep prev) {
        copy(game, prev);

        if (prev == null) {
            prev = new MoveStep(parent, MovePath.STEP_FORWARDS);
            prev.setFromEntity(entity, game);
        }

        switch (getType()) {
            case MovePath.STEP_UNLOAD :
                // TODO: Can immobilized transporters unload?
            case MovePath.STEP_LOAD :
                setMp(1);
                break;
            case MovePath.STEP_TURN_LEFT :
            case MovePath.STEP_TURN_RIGHT :
                // Check for pavement movement.
                if (Compute.canMoveOnPavement(game, prev.getPosition(), getPosition())) {
                    setPavementStep(true);
                } else {
                    setPavementStep(false);
                    setOnlyPavement(false);
                }

                // Infantry can turn for free.
                setMp((parent.isJumping() || isHasJustStood() || parent.isInfantry()) ? 0 : 1);
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
                if (Compute.canMoveOnPavement(game, prev.getPosition(), getPosition())) {
                    setPavementStep(true);
                } else {
                    setPavementStep(false);
                    setOnlyPavement(false);
                }

                calcMovementCostFor(game, prev.getPosition());

                // check for water
                if (!isPavementStep()
                    && game.board.getHex(getPosition()).levelOf(Terrain.WATER) > 0
                    && entity.getMovementType() != Entity.MovementType.HOVER) {
                    setRunProhibited(true);
                }
                setHasJustStood(false);
                if (prev.isThisStepBackwards() != isThisStepBackwards()) {
                    setDistance(0); //start over after shifting gears
                }
                addDistance(1);
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
                if (Compute.canMoveOnPavement(game, prev.getPosition(), getPosition())) {
                    setPavementStep(true);
                } else {
                    setPavementStep(false);
                    setOnlyPavement(false);
                }

                calcMovementCostFor(game, prev.getPosition());
                setMp(getMp() + 1);
                // check for water
                if (!isPavementStep() && game.board.getHex(getPosition()).levelOf(Terrain.WATER) > 0) {
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
            default :
                setMp(0);
        }
        addMpUsed(getMp());
        if (game.board.getHex(getPosition()).contains(Terrain.FIRE)) {
            heat = 2;
            totalHeat += 2;
        }
    }

    /**
     * Check the step for stacking violations, jumping into illegal
     * terrain, and jumping into the same hex you started in.
     * 
     * @param game - The <code>Game</code> object.
     * @param entity - The <code>Entity</code> object.
     * @return True if the step was already illegal or has now been
     *         determined to be illegal by this method.
     */
    public boolean checkAndSetIllegal(final Game game, final Entity entity) {
        final Hex destHex = game.board.getHex(getPosition());

        // skip steps that are not the last step
        if (getMovementType() == Entity.MOVE_ILLEGAL) {
            return true;
        }

        // check for stacking violations
        final Entity violation = Compute.stackingViolation(game, entity.getId(), getPosition());
        if (violation != null && getType() != MovePath.STEP_CHARGE && getType() != MovePath.STEP_DFA) {
            // can't move here
            setStackingViolation(true);
            setMovementType(Entity.MOVE_ILLEGAL);
            return true;
        }

        // Check again for illegal terrain, in case of jumping. We're
        // allowed to enter prohibited terrain via a road or bridge.
        if (entity.isHexProhibited(destHex) && !isPavementStep()) {
            setMovementType(Entity.MOVE_ILLEGAL);
            return true;
        }

        if (parent.isJumping()) {
            Coords start = entity.getPosition();
            Coords land = getPosition();

            if (start.distance(land) == 0) {
                setMovementType(Entity.MOVE_ILLEGAL);
                return true;
            }
        }
        return false;
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

    /**
	 * Takes the given state as the previous state and sets flags from it.
	 * 
	 * @param state
	 */
    public void copy(final Game game, MoveStep prev) {
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
    }

    /**
	 * Sets this state as coming from the entity.
	 * 
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
	 * @return
	 */
    public boolean isLegal() {
        return movementType != Entity.MOVE_ILLEGAL;
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

    void compileIllegal(final Game game, final Entity entity, final MoveStep prev) {
        final int stepType = getType();

        Coords curPos = getPosition();
        Coords lastPos = entity.getPosition();
        boolean isUnjammingRAC = entity.isUnjammingRAC();

        // Is this the first step?
        if (null == prev) {
            setFirstStep(true);
        } else {
            // infantry get a first step if all they've done is spin on the
            // spot:
            //   getMpUsed() is the MPs used in the whole MovePath
            //   getMp() is the MPs used in the last (illegal) step (this step)
            //   if the difference between the whole path and this step is 0
            //   then this must be their first step
            setFirstStep(parent.isInfantry() && (getMpUsed() - getMp()) == 0);

            prevStepOnPavement = prev.isPavementStep();
            isTurning = prev.isTurning();
            isUnloaded = prev.isUnloaded();
            lastPos = prev.getPosition();
        }

        // guilty until proven innocent
        movementType = Entity.MOVE_ILLEGAL;

        // check for ejection (always legal?)
        if (type == MovePath.STEP_EJECT) {
            movementType = Entity.MOVE_NONE;
        }

        // check for valid jump mp
        if (parent.isJumping() && getMpUsed() <= entity.getJumpMPWithTerrain() && !isProne()
            && !(entity instanceof Protomech && entity.getInternal(Protomech.LOC_LEG) == Protomech.ARMOR_DESTROYED)) {
            movementType = Entity.MOVE_JUMP;
        }
        
        // legged Protos may make one facing change
        if (isFirstStep()
            && entity instanceof Protomech
            && entity.getInternal(Protomech.LOC_LEG) == Protomech.ARMOR_DESTROYED
            && (stepType == MovePath.STEP_TURN_LEFT 
                || stepType == MovePath.STEP_TURN_RIGHT)) {
            movementType = Entity.MOVE_WALK;
        }            

        // check for valid walk/run mp
        if (!parent.isJumping()
            && (!isProne()
                || parent.contains(MovePath.STEP_GET_UP)
                || stepType == MovePath.STEP_TURN_LEFT
                || stepType == MovePath.STEP_TURN_RIGHT)) {

            if (getMpUsed() <= entity.getWalkMP()) {
                movementType = Entity.MOVE_WALK;

            // Vehicles moving along pavement get "road bonus" of 1 MP.
            // ASSUMPTION : bonus MP is to walk, which may me 2 MP to run.                
            } else if (entity instanceof Tank && isOnlyPavement() && getMpUsed() == entity.getWalkMP() + 1) {
                movementType = Entity.MOVE_WALK;
            } else if (getMpUsed() <= entity.getRunMPwithoutMASC() && !isRunProhibited()) {
                movementType = Entity.MOVE_RUN;
            } else if (getMpUsed() <= entity.getRunMP() && !isRunProhibited()) {
                setUsingMASC(true);
                Mech m = (Mech) entity;
                setTargetNumberMASC(m.getMASCTarget());
                movementType = Entity.MOVE_RUN;
            } else if (
                entity instanceof Tank
                    && isOnlyPavement()
                    && getMpUsed() <= entity.getRunMP() + (entity.getWalkMP() % 2 == 1 ? 1 : 2)
                    && !isRunProhibited()) {
                movementType = Entity.MOVE_RUN;
            }
        }
        
        // Mechs with busted Gyro may make only one facing change
        if (entity.getBadCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_GYRO, Mech.LOC_CT) > 1
    	    && !isFirstStep() ) {
            movementType = Entity.MOVE_ILLEGAL;
        }

        // mechs with 1 MP are allowed to get up, except if they've used that 1MP up already
        if (MovePath.STEP_GET_UP==stepType && 1==entity.getRunMP() && entity.mpUsed<1) {
            movementType = Entity.MOVE_RUN;
        }

        // amnesty for the first step
        if (isFirstStep()
            && movementType == Entity.MOVE_ILLEGAL
            && entity.getWalkMP() > 0
            && !entity.isProne()
            && stepType == MovePath.STEP_FORWARDS) {
            movementType = Entity.MOVE_RUN;
        }
        

        // Is the entity unloading passeners?
        if (stepType == MovePath.STEP_UNLOAD) {
            // Prone Meks are able to unload, if they have the MP.
            if (getMpUsed() <= entity.getRunMP() && entity.isProne() && movementType == Entity.MOVE_ILLEGAL) {
                movementType = Entity.MOVE_RUN;
                if (getMpUsed() <= entity.getWalkMP()) {
                    movementType = Entity.MOVE_WALK;
                }
            }

            // Can't unload units into prohibited terrain
            // or into stacking violation.
            Targetable target = getTarget(game);
            if (target instanceof Entity) {
                Entity other = (Entity) target;
                if (null != Compute.stackingViolation(game, other, curPos, entity)
                    || other.isHexProhibited(game.board.getHex(curPos))) {
                    movementType = Entity.MOVE_ILLEGAL;
                }
            } else {
                System.err.print("Trying to unload ");
                System.err.print(target.getDisplayName());
                System.err.print(" from ");
                System.err.print(entity.getDisplayName());
                System.err.println(".");
                movementType = Entity.MOVE_ILLEGAL;
            }

        }

        // Can't run or jump if unjamming a RAC.
        if (isUnjammingRAC && (movementType == Entity.MOVE_RUN || parent.isJumping())) {
            movementType = Entity.MOVE_ILLEGAL;
        }

        // only standing mechs may go prone
        if (stepType == MovePath.STEP_GO_PRONE && (isProne() || !(entity instanceof Mech))) {
            movementType = Entity.MOVE_ILLEGAL;
        }

        // check if this movement is illegal for reasons other than points
        if (!isMovementPossible(game, lastPos) || isUnloaded) {
            movementType = Entity.MOVE_ILLEGAL;
        }

        if (prev != null && !prev.isLegal()) {
            movementType = Entity.MOVE_ILLEGAL;
        }

        if (movementType == Entity.MOVE_ILLEGAL) {
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
    protected void calcMovementCostFor(Game game, Coords prev) {
        final int moveType = parent.getEntity().getMovementType();
        final Hex srcHex = game.board.getHex(prev);
        final Hex destHex = game.board.getHex(getPosition());

        mp = 1;

        // jumping always costs 1
        if (parent.isJumping()) {
            return;
        }

        // Account for terrain, unless we're moving along a road.
        if (!isPavementStep) {

            if (destHex.levelOf(Terrain.ROUGH) > 0) {
                mp++;
            }
            if (destHex.levelOf(Terrain.RUBBLE) > 0) {
                mp++;
            }
            if (destHex.levelOf(Terrain.WOODS) == 1) {
                mp++;
            } else if (destHex.levelOf(Terrain.WOODS) > 1) {
                mp += 2;
            }

            // non-hovers check for water depth
            if (moveType != Entity.MovementType.HOVER) {
                if (destHex.levelOf(Terrain.WATER) == 1) {
                    mp++;
                } else if (destHex.levelOf(Terrain.WATER) > 1) {
                    mp += 3;
                }
            }

            // Swamp adds to movement cost and force a "get stuck" check.
            /*
			 * TODO: uncomment me in v0.29.1 if (
			 * destHex.contains(Terrain.SWAMP) ) { mp += 1; }
			 */
        } // End not-along-road

        // account for elevation?
        // TODO: allow entities to occupy different levels of buildings.
        int nSrcEl = parent.getEntity().elevationOccupied(srcHex);
        int nDestEl = parent.getEntity().elevationOccupied(destHex);
        int nMove = parent.getEntity().getMovementType();

        if (nSrcEl != nDestEl) {
            int delta_e = Math.abs(nSrcEl - nDestEl);

            // Infantry and ground vehicles are charged double.
            if (parent.isInfantry()
                || (nMove == Entity.MovementType.TRACKED
                    || nMove == Entity.MovementType.WHEELED
                    || nMove == Entity.MovementType.HOVER)) {
                delta_e *= 2;
            }
            mp += delta_e;
        }

        // If we entering a building, all non-infantry pay additional MP.
        if (nDestEl < destHex.levelOf(Terrain.BLDG_ELEV) && !(parent.isInfantry())) {
            Building bldg = game.board.getBuildingAt(getPosition());
            mp += bldg.getType();
        }
    }

    /**
     * Is movement possible from a previous position to this one?
     * <p/>
     * This function does not comment on whether an overall movement path
     * is possible, just whether the <em>current</em> step is possible.
     */
    public boolean isMovementPossible(Game game, Coords src) {
        final Hex srcHex = game.board.getHex(src);
        final Coords dest = this.getPosition();
        final Hex destHex = game.board.getHex(dest);

        if (src.distance(dest) > 1) {
            throw new IllegalArgumentException("Coordinates must be adjacent.");
        }

        /* 2004-03-31 : don't look at overall movement, just this step. **
        if (movementType == Entity.MOVE_ILLEGAL) {
            // that was easy
            return false;
        }
        /* 2004-03-31 : don't look at overall movement, just this step. */

        // super-easy
        if (parent.getEntity().isImmobile()) {
            return false;
        }
        // another easy check
        if (!game.board.contains(dest)) {
            return false;
        }

        // Can't back up across an elevation change.
        if (isThisStepBackwards() && parent.getEntity().elevationOccupied(destHex) != parent.getEntity().elevationOccupied(srcHex)) {
            return false;
        }

        // Swarming entities can't move.
        if (Entity.NONE != parent.getEntity().getSwarmTargetId()) {
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
                if (parent.getEntity().getOwner() == other.getOwner() && !parent.getEntity().equals(other)) {

                    // The moving unit should be able to load the other unit.
                    if (!parent.getEntity().canLoad(other)) {
                        return false;
                    }

                    // The other unit should be able to have a turn.
                    if (!other.isSelectableThisTurn(game)) {
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
        for (Enumeration e = parent.getEntity().getAmmo(); e.hasMoreElements();) {
            if (((Mounted) e.nextElement()).isDumping()) {
                bDumping = true;
                break;
            }
        }
        if (bDumping && (movementType == Entity.MOVE_RUN || movementType == Entity.MOVE_JUMP)) {
            return false;
        }

        // check elevation difference > max
        int nSrcEl = parent.getEntity().elevationOccupied(srcHex);
        int nDestEl = parent.getEntity().elevationOccupied(destHex);
        int nMove = parent.getEntity().getMovementType();

        if (movementType != Entity.MOVE_JUMP && Math.abs(nSrcEl - nDestEl) > parent.getEntity().getMaxElevationChange()) {
            return false;
        }
        // units moving backwards may not change elevation levels (I think this
        // rule's dumb)
        if ((type == MovePath.STEP_BACKWARDS
            || type == MovePath.STEP_LATERAL_LEFT_BACKWARDS
            || type == MovePath.STEP_LATERAL_RIGHT_BACKWARDS)
            && nSrcEl != nDestEl) {
            return false;
        }

        // Can't run into water unless hovering, or using a bridge.
        if (movementType == Entity.MOVE_RUN
            && nMove != Entity.MovementType.HOVER
            && destHex.levelOf(Terrain.WATER) > 0
            && !firstStep
            && !isPavementStep) {
            return false;
        }

        // ugh, stacking checks. well, maybe we're immune!
        if (movementType != Entity.MOVE_JUMP && type != MovePath.STEP_CHARGE && type != MovePath.STEP_DFA) {
            // can't move a mech into a hex with an enemy mech
            if (parent.getEntity() instanceof Mech && Compute.isEnemyIn(game, parent.getEntity().getId(), dest, true)) {
                return false;
            }

            // Can't move out of a hex with an enemy unit unless we started
            // there, BUT we're allowed to turn, unload, or go prone.
            if (Compute.isEnemyIn(game, parent.getEntity().getId(), src, false)
                && !src.equals(parent.getEntity().getPosition())
                && type != MovePath.STEP_TURN_LEFT
                && type != MovePath.STEP_TURN_RIGHT
                && type != MovePath.STEP_UNLOAD
                && type != MovePath.STEP_GO_PRONE) {
                return false;
            }

        }

        // can't jump over too-high terrain
        if (movementType == Entity.MOVE_JUMP
            && destHex.getElevation() > (parent.getEntity().getElevation() + parent.getEntity().getJumpMPWithTerrain())) {
            return false;
        }

        // Certain movement types have terrain restrictions; terrain
        // restrictions are lifted when moving along a road or bridge.
        if (movementType != Entity.MOVE_JUMP && parent.getEntity().isHexProhibited(destHex) && !isPavementStep) {
            return false;
        }

        // If we are *in* restricted terrain, we can only leave via roads.
        if (movementType != Entity.MOVE_JUMP && parent.getEntity().isHexProhibited(srcHex) && !isPavementStep) {
            return false;
        }

        return true;
    }

    //Used by BoardView to see if we can re-use an old movement sprite.
    public boolean canReuseSprite(MoveStep other) {
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
            !this.isStackingViolation &&
            !other.isStackingViolation) {
            return true;
        } else {
            return false;
        }
    }

}
