/*
 * MegaMek - Copyright (C) 2000-2003 Ben Mazur (bmazur@sev.org)
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

                setMp(
                    Compute.getMovementCostFor(
                        game,
                        entity.getId(),
                        prev.getPosition(),
                        getPosition(),
                        parent.isJumping()));

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
                                MovePath.turnForLateralShift(getType(), MovePath.STEP_FORWARDS))
                            + 3)
                            % 6);
                    setThisStepBackwards(true);
                    setRunProhibited(true);
                } else {
                    moveInDir(
                        MovePath.getAdjustedFacing(
                            getFacing(),
                            MovePath.turnForLateralShift(getType(), MovePath.STEP_FORWARDS)));
                    setThisStepBackwards(false);
                }

                // Check for pavement movement.
                if (Compute.canMoveOnPavement(game, prev.getPosition(), getPosition())) {
                    setPavementStep(true);
                } else {
                    setPavementStep(false);
                    setOnlyPavement(false);
                }

                setMp(
                    Compute.getMovementCostFor(
                        game,
                        entity.getId(),
                        prev.getPosition(),
                        getPosition(),
                        parent.isJumping())
                        + 1);
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

    public boolean checkIllegal(final Game game, final Entity entity) {
        final Hex destHex = game.board.getHex(getPosition());

        // skip steps that are not the last step
        if (getMovementType() == Entity.MOVE_ILLEGAL) {
            return true;
        }

        // check for stacking violations
        final Entity violation = Compute.stackingViolation(game, entity.getId(), getPosition());
        if (violation != null && getType() != MovePath.STEP_CHARGE && getType() != MovePath.STEP_DFA) {
            // can't move here
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

    public void setPastDanger(boolean b) {
        pastDanger = b;
    }

    public void setPrevStepOnPavement(boolean b) {
        prevStepOnPavement = b;
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

    void compileIllegal(final Game game, final Entity entity, final MoveStep prev) {
        final int stepType = getType();

        Coords curPos = getPosition();
        Coords lastPos = getPosition();
        boolean isUnjammingRAC = entity.isUnjammingRAC();

        if (prev != null) {
            setFirstStep(parent.isInfantry() && getMpUsed() == 0);
            prevStepOnPavement = prev.isPavementStep();
            isTurning = prev.isTurning();
            isUnloaded = prev.isUnloaded();
            lastPos = prev.getPosition();
        } else {
            setFirstStep(true);
        }

        // guilty until proven innocent
        movementType = Entity.MOVE_ILLEGAL;

        // check for valid jump mp
        if (parent.isJumping() && getMpUsed() <= entity.getJumpMPWithTerrain() && !isProne()) {
            movementType = Entity.MOVE_JUMP;
        }

        // check for valid walk/run mp
        if (!parent.isJumping()
            && (!isProne()
                || parent.contains(MovePath.STEP_GET_UP)
                || stepType == MovePath.STEP_TURN_LEFT
                || stepType == MovePath.STEP_TURN_RIGHT)) {

            // Vehicles moving along pavement get "road bonus" of 1 MP.
            // ASSUMPTION : bonus MP is to walk, which may me 2 MP to run.
            if (getMpUsed() <= entity.getWalkMP()) {
                movementType = Entity.MOVE_WALK;
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
                    && getMpUsed() <= entity.getRunMP() + (Compute.isOdd(entity.getWalkMP()) ? 1 : 2)
                    && !isRunProhibited()) {
                movementType = Entity.MOVE_RUN;
            }
        }

        // mechs with 1 MP are allowed to get up
        if (stepType == MovePath.STEP_GET_UP && entity.getRunMP() == 1) {
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
        if (!Compute.isMovementPossible(game, entity.getId(), lastPos, curPos, movementType, stepType, isFirstStep())
            || isUnloaded) {
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

}