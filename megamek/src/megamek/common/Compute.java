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
 */

package megamek.common;

import java.util.*;

import megamek.common.actions.*;
import megamek.client.*;

/**
 * The compute class is designed to provide static methods for mechs
 * and other entities moving, firing, etc.
 */
public class Compute
{
  public static final int        RANGE_MINIMUM    = 0;
  public static final int        RANGE_SHORT      = 1;
  public static final int        RANGE_MEDIUM     = 2;
  public static final int        RANGE_LONG       = 3;
  public static final int        RANGE_OUT_OF     = Integer.MAX_VALUE;
    
    public static final int        ARC_360          = 0;
    public static final int        ARC_FORWARD      = 1;
    public static final int        ARC_LEFTARM      = 2;
    public static final int        ARC_RIGHTARM     = 3;
    public static final int        ARC_REAR         = 4;
    public static final int        ARC_LEFTSIDE     = 5;
    public static final int        ARC_RIGHTSIDE    = 6;
    
    public static final int        GEAR_LAND        = 0;
    public static final int        GEAR_BACKUP      = 1;
    public static final int        GEAR_JUMP        = 2;
    public static final int        GEAR_CHARGE      = 3;
    public static final int        GEAR_DFA         = 4;
    public static final int        GEAR_TURN        = 5;

    private static MMRandom random = MMRandom.generate(MMRandom.R_DEFAULT);

    /** Wrapper to random#d6(n) */
    public static int d6(int dice) {
        return random.d6(dice);
    }
    
    /** Wrapper to random#d6() */
    public static int d6() {
        return random.d6();
    }

    /** Wrapper to random#randomInt(n) */
    public static int randomInt( int maxValue ) {
        return random.randomInt(maxValue);
    }
    
    /**
     * Sets the RNG to the desired type
     */
    public static void setRNG(int type) {
        random = MMRandom.generate(type);
    }

    /**
     * Returns the odds that a certain number or above 
     * will be rolled on 2d6.
     */
    public static double oddsAbove(int n) {
        if (n <= 2) {
            return 100.0;
        } else if (n > 12) {
            return 0;
        }
        final double[] odds = {100.0, 100.0,
                100.0, 97.2, 91.6, 83.3, 72.2, 58.3, 
                41.6, 27.7, 16.6, 8.3, 2.78, 0};
        return odds[n];
    }
    
    /** Returns the odds of rolling n exactly on 2d6
     */
    public static double oddsOf(int n) {
        switch (n) {
        case 2:return 1.0/36;
        case 3:return 2.0/36;
        case 4:return 3.0/36;
        case 5:return 4.0/36;
        case 6:return 5.0/36;
        case 7:return 6.0/36;
        case 8:return 5.0/36;
        case 9:return 4.0/36;
        case 10:return 3.0/36;
        case 11:return 2.0/36;
        case 12:return 1.0/36;
        }
        return 0;
    } 
     
    /**
     * Generates a MovePath to rotate the entity to it's new facing
     */
    public static MovePath rotatePathfinder(Game game, int entityId, 
                                                int destFacing) {
        final Entity entity = game.getEntity(entityId);
        return rotatePathfinder(entity.getFacing(), destFacing);
    }
    
    /**
     * Generates a MovePath object to rotate from the start facing to the
     * destination facing.
     */
    public static MovePath rotatePathfinder(int facing, int destFacing) {
        MovePath md = new MovePath();

        // adjust facing
        while (facing != destFacing) {
            int stepType = MovePath.getDirection(facing, destFacing);
            md.addStep(stepType);
            facing = MovePath.getAdjustedFacing(facing, stepType);
        }
        
        return md;
    }
    
    /**
     * Generates MovePath for a mech to move from its current position
     * to the destination.
     */
    public static MovePath lazyPathfinder(Game game, int entityId, 
                                                  Coords dest) {
        final Entity entity = game.getEntity(entityId);
        return lazyPathfinder(entity.getPosition(), entity.getFacing(), dest);
    }
    
    /**
     * Generates MovePath for a mech to move from the start position and
     * facing to the destination
     */
    public static MovePath lazyPathfinder(Coords src, int facing, Coords dest) {
        MovePath md = new MovePath();
        
        int curFacing = facing;
        Coords curPos = new Coords(src);
        
        while(!curPos.equals(dest)) {
            // adjust facing
            md.append(rotatePathfinder(curFacing, curPos.direction1(dest)));
            // step forwards
            md.addStep(MovePath.STEP_FORWARDS);

            curFacing = curPos.direction1(dest);
            curPos = curPos.translated(curFacing);
        }
        
        return md;
    }
    
    /**
     * Backwards walking pathfinder.  Note that this will let you do impossible
     * things, like run backwards.
     */
    public static MovePath backwardsLazyPathfinder(Coords src, int facing, Coords dest) {
        MovePath md = new MovePath();
        
        int curFacing = facing;
        Coords curPos = new Coords(src);
        
        while(!curPos.equals(dest)) {
            // adjust facing
            int destFacing = (curPos.direction1(dest) + 3) % 6;
            md.append(rotatePathfinder(curFacing, destFacing));
            
            // step backwards
            md.addStep(MovePath.STEP_BACKWARDS);

            curFacing = destFacing;
            curPos = curPos.translated((destFacing + 3) % 6);
        }
        
        return md;
    }
    
    /**
     * Charge pathfinder.  Finds a path up to the hex before the target,
     * then charges
     */
    public static MovePath chargeLazyPathfinder(Coords src, int facing,
                                                        Coords dest) {
        MovePath md = new MovePath();
        
        int curFacing = facing;
        Coords curPos = new Coords(src);
        
        Coords subDest = dest.translated(dest.direction1(src));
        
        while(!curPos.equals(subDest)) {
            // adjust facing
            md.append(rotatePathfinder(curFacing, curPos.direction1(subDest)));
            // step forwards
            md.addStep(MovePath.STEP_FORWARDS);

            curFacing = curPos.direction1(subDest);
            curPos = curPos.translated(curFacing);
        }
        
        // adjust facing
        md.append(rotatePathfinder(curFacing, curPos.direction1(dest)));
        // charge!
        md.addStep(MovePath.STEP_CHARGE);
        
        return md;
    }
    
    /**
     * Death from above pathfinder.  Finds a path up to the hex before the target,
     * then charges
     */
    public static MovePath dfaLazyPathfinder(Coords src, int facing,
                                                        Coords dest) {
        MovePath md = new MovePath();
        
        int curFacing = facing;
        Coords curPos = new Coords(src);
        
        Coords subDest = dest.translated(dest.direction1(src));
        
        while(!curPos.equals(subDest)) {
            // adjust facing
            md.append(rotatePathfinder(curFacing, curPos.direction1(subDest)));
            // step forwards
            md.addStep(MovePath.STEP_FORWARDS);

            curFacing = curPos.direction1(subDest);
            curPos = curPos.translated(curFacing);
        }
        
        // adjust facing
        md.append(rotatePathfinder(curFacing, curPos.direction1(dest)));
        // charge!
        md.addStep(MovePath.STEP_DFA);
        
        return md;
    }
    
    
    /**
     * "Compiles" some movement data by setting all the flags.  Determines which
     * steps are possible, how many movement points each uses, and where they
     * occur.
     */
    public static void compile(Game game, int entityId, MovePath md) {
        final Entity entity = game.getEntity(entityId);
        
        // some flags
        boolean isJumping = false;
        boolean isInfantry = (entity instanceof Infantry);
        boolean isUsingManAce = false;
        
        // check for jumping
        if (md.contains(MovePath.STEP_START_JUMP)) {
            isJumping = true;
        } else {
            isUsingManAce = entity.getCrew().getOptions().booleanOption("maneuvering_ace");
        }
        
        // transform lateral shifts for quads or maneuverability aces
        if (((entity instanceof QuadMech) || isUsingManAce) && !isJumping) {
            md.transformLateralShifts();
            md.transformLateralShiftsBackwards();
        }

        // first pass: set position, facing and mpUsed; figure out overallMoveType
        for (final Enumeration i = md.getSteps(); i.hasMoreElements();) {
            MoveStep step = (MoveStep)i.nextElement();
            MoveStep prev = step.getPrev();
            
            MoveState state = step.getState();
            MoveState prevState;
            if (prev == null) {
                prevState = new MoveState();
                prevState.setFromEntity(entity, game); 
            } else {
                prevState = prev.getState();
            }
            
            state.setFromPrev(prevState);

            // 
            switch(step.getType()) {
            case MovePath.STEP_UNLOAD:
            // TODO: Can immobilized transporters unload?
            case MovePath.STEP_LOAD:
                state.setMp(1);
                break;
            case MovePath.STEP_TURN_LEFT :
            case MovePath.STEP_TURN_RIGHT :
                // Check for pavement movement.
                if (canMoveOnPavement(game, prevState.getPosition(), state.getPosition())) {
                    state.setPavementStep(true);
                } else {
                    state.setPavementStep(false);
                    state.setOnlyPavement(false);
                }

                // Infantry can turn for free.
                state.setMp((state.isJumping() || state.isHasJustStood() || isInfantry) ? 0 : 1);
                state.adjustFacing(step.getType());
                break;
            case MovePath.STEP_FORWARDS :
            case MovePath.STEP_BACKWARDS :
            case MovePath.STEP_CHARGE :
            case MovePath.STEP_DFA :
                // step forwards or backwards
                if (step.getType() == MovePath.STEP_BACKWARDS) {
                    state.moveInDir((state.getFacing() + 3) % 6);
                    state.setThisStepBackwards(true);
                    state.setRunProhibited(true);
                } else {
                    state.moveInDir(state.getFacing());
                    state.setThisStepBackwards(false);
                }

                // Check for pavement movement.
                if (canMoveOnPavement(game, prevState.getPosition(), state.getPosition())) {
                    state.setPavementStep(true);
                } else {
                    state.setPavementStep(false);
                    state.setOnlyPavement(false);
                }

                state.setMp(getMovementCostFor(game, entityId, prevState.getPosition(), state.getPosition(), state.isJumping()));

                // check for water
                if (!state.isPavementStep()
                    && game.board.getHex(state.getPosition()).levelOf(Terrain.WATER) > 0
                    && entity.getMovementType() != Entity.MovementType.HOVER) {
                    state.setRunProhibited(true);
                }
                state.setHasJustStood(false);
                if (prevState.isThisStepBackwards() != state.isThisStepBackwards()) {
                    state.setDistance(0); //start over after shifting gears
                }
                state.addDistance(1);
                break;
            case MovePath.STEP_LATERAL_LEFT :
            case MovePath.STEP_LATERAL_RIGHT :
            case MovePath.STEP_LATERAL_LEFT_BACKWARDS :
            case MovePath.STEP_LATERAL_RIGHT_BACKWARDS :
                if (step.getType() == MovePath.STEP_LATERAL_LEFT_BACKWARDS 
                    || step.getType() == MovePath.STEP_LATERAL_RIGHT_BACKWARDS) {
                    state.moveInDir((MovePath.getAdjustedFacing(state.getFacing(), MovePath.turnForLateralShiftBackwards(step.getType())) + 3) % 6);
                    state.setThisStepBackwards(true);
                    state.setRunProhibited(true);
                } else {
                    state.moveInDir(MovePath.getAdjustedFacing(state.getFacing(), MovePath.turnForLateralShift(step.getType())));
                    state.setThisStepBackwards(false);
                }

                // Check for pavement movement.
                if ( canMoveOnPavement(game, prevState.getPosition(), state.getPosition()) ) {
                    state.setPavementStep(true);
                } else { 
                    state.setPavementStep(false);
                    state.setOnlyPavement(false);
                }

                state.setMp(getMovementCostFor(game, entityId, prevState.getPosition(), state.getPosition(),
                state.isJumping()) + 1);
                // check for water
                if (!state.isPavementStep()
                    && game.board.getHex(state.getPosition()).levelOf(Terrain.WATER) > 0) {
                        state.setRunProhibited(true);
                }
                state.setHasJustStood(false);
                if (prevState.isThisStepBackwards() != state.isThisStepBackwards()) {
                    state.setDistance(0); //start over after shifting gears
                }
                state.addDistance(1);
                break;
            case MovePath.STEP_GET_UP :
                // mechs with 1 MP are allowed to get up
                state.setMp(entity.getRunMP() == 1 ? 1 : 2);
                state.setHasJustStood(true);
                break;
            case MovePath.STEP_GO_PRONE :
                state.setMp(1);
                break;
            case MovePath.STEP_START_JUMP :
                state.setJumping(true);
                break;
            default :
                state.setMp(0);
            }
            
            state.addMpUsed(state.getMp());

        }
        
        // running with gyro or hip hit is dangerous!
        // but we can't show it right now.  check officially moved anyhow.
//        if (overallMoveType == Entity.MOVE_RUN
//            && (entity.getDestroyedCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_GYRO,Mech.LOC_CT) > 0
//                || entity.hasHipCrit())) {
//            md.getStep(0).setDanger(true);
//        }
        
        // set moveType, illegal, trouble flags
        compileIllegal(game, entityId, md);

        // Check the last step for legality.
        compileLastStep(game, entityId, md);
        
        // check for illegal jumps
        if (isJumping) {
            compileJumpCheck(game, entityId, md);
        }
        
        md.setCompiled(true);
    }
    
    /**
     * Go thru movement data and set the moveType, illegal and danger flags.
     */
    private static void compileIllegal(Game game, int entityId, MovePath md) {
        final Entity entity = game.getEntity(entityId);

        Coords curPos = new Coords(entity.getPosition());
        boolean legal = true;
        boolean danger = false;
        boolean pastDanger = false;
        boolean firstStep = true;
        boolean isInfantry = (entity instanceof Infantry);
        boolean isTurning = false;
        boolean isUnloaded = false;
        boolean prevStepOnPavement = false;
        boolean isProne = entity.isProne();
        boolean isUnjammingRAC = entity.isUnjammingRAC();

        for (final Enumeration i = md.getSteps(); i.hasMoreElements();) {
            final MoveStep step = (MoveStep)i.nextElement();
            final int stepType = step.getType();
            
            Coords lastPos = new Coords(curPos);
            curPos = step.getPosition();

            // guilty until proven innocent
            int moveType = Entity.MOVE_ILLEGAL;
            
            // check for valid jump mp
            if (step.isJumping() 
                && step.getMpUsed() <= entity.getJumpMPWithTerrain()
                && !isProne) {
                moveType = Entity.MOVE_JUMP;
            }
            
            // check for valid walk/run mp
            if ( !step.isJumping()
                && (!isProne || md.contains(MovePath.STEP_GET_UP)
                    || stepType == MovePath.STEP_TURN_LEFT 
                    || stepType == MovePath.STEP_TURN_RIGHT)) {

                // Vehicles moving along pavement get "road bonus" of 1 MP.
                // ASSUMPTION : bonus MP is to walk, which may me 2 MP to run.
                if (step.getMpUsed() <= entity.getWalkMP()) {
                    moveType = Entity.MOVE_WALK;
                } else if ( entity instanceof Tank && step.isOnlyPavement() &&
                            step.getMpUsed() == entity.getWalkMP() + 1 ) {
                    moveType = Entity.MOVE_WALK;
                } else if ( step.getMpUsed() <= entity.getRunMPwithoutMASC() &&
                            !step.isRunProhibited() ) {
                    moveType = Entity.MOVE_RUN;
                } else if ( step.getMpUsed() <= entity.getRunMP() &&
                            !step.isRunProhibited() ) {
                    step.setUsingMASC(true);
                    Mech m = (Mech)entity;
                    step.setTargetNumberMASC(m.getMASCTarget());
                    moveType = Entity.MOVE_RUN;
                } else if ( entity instanceof Tank && step.isOnlyPavement() &&
                            step.getMpUsed() <= entity.getRunMP() + 
                            (isOdd(entity.getWalkMP()) ? 1 : 2) &&
                            !step.isRunProhibited() ) {
                    moveType = Entity.MOVE_RUN;
                }
            }
            
            // mechs with 1 MP are allowed to get up
            if ( stepType == MovePath.STEP_GET_UP &&
                 entity.getRunMP() == 1 ) {
                moveType = Entity.MOVE_RUN;
            }
            
            // amnesty for the first step
            if ( firstStep && moveType == Entity.MOVE_ILLEGAL &&
                 entity.getWalkMP() > 0 && !entity.isProne() &&
                 stepType == MovePath.STEP_FORWARDS ) {
                moveType = Entity.MOVE_RUN;
            }

            // Is the entity unloading passeners?
            if ( stepType == MovePath.STEP_UNLOAD ) {
                // Prone Meks are able to unload, if they have the MP.
                if ( step.getMpUsed() <= entity.getRunMP() &&
                     entity.isProne() && moveType == Entity.MOVE_ILLEGAL ) {
                    moveType = Entity.MOVE_RUN;
                    if ( step.getMpUsed() <= entity.getWalkMP() ) {
                        moveType = Entity.MOVE_WALK;
                    }
                }

                // Can't unload units into prohibited terrain
                // or into stacking violation.
                Targetable target = step.getTarget( game );
                if ( target instanceof Entity ) {
                    Entity other = (Entity) target;
                    if ( null != stackingViolation(game, other, curPos, entity)
                         || other.isHexProhibited(game.board.getHex(curPos))) {
                        moveType = Entity.MOVE_ILLEGAL;
                    }
                } else {
                    System.err.print( "Trying to unload " );
                    System.err.print( target.getDisplayName() );
                    System.err.print( " from " );
                    System.err.print( entity.getDisplayName() );
                    System.err.println( "." );
                    moveType = Entity.MOVE_ILLEGAL;
                }

            }

            // Can't run or jump if unjamming a RAC.
            if (isUnjammingRAC
                && (moveType == Entity.MOVE_RUN || step.isJumping())) {
                moveType = Entity.MOVE_ILLEGAL;
            }

            // only standing mechs may go prone
            if (stepType == MovePath.STEP_GO_PRONE 
            && (isProne || !(entity instanceof Mech))) {
                moveType = Entity.MOVE_ILLEGAL;
            }
            
            // check if this movement is illegal for reasons other than points
            if ( !isMovementPossible(game, entityId, lastPos, curPos,
                                     moveType, stepType, firstStep)
                 || isUnloaded ) {
                moveType = Entity.MOVE_ILLEGAL;
            }
            

            // no legal moves past an illegal one
            if (moveType == Entity.MOVE_ILLEGAL) {
                legal = false;
            }

            // check for danger
            danger = step.isDanger();
            danger |= isPilotingSkillNeeded( game, entityId, lastPos, 
                                             curPos, moveType,
                                             isTurning,
                                             prevStepOnPavement );

            // getting up is also danger
            if (stepType == MovePath.STEP_GET_UP) {
                danger = true;
            }
            
            // set flags
            step.setDanger(danger);
            step.setPastDanger(pastDanger);
            step.setMovementType(legal ? moveType : Entity.MOVE_ILLEGAL);
            
            // set past danger
            pastDanger |= danger;

            // Record if we're turning *after* check for danger,
            // because the danger lies in moving *after* turn.
            switch(stepType) {
            case MovePath.STEP_TURN_LEFT :
            case MovePath.STEP_TURN_RIGHT :
                isTurning = true;
                break;
            case MovePath.STEP_UNLOAD:
                // Unloading must be the last step.
                isUnloaded = true;
                break;
            default:
                isTurning = false;
                break;
            }

            firstStep = false;
 
                                          
            /* Bug 754610: Revert fix for bug 702735. */
            // Record if the step just taken was along pavement or a road.
            prevStepOnPavement = step.isPavementStep();

            // Infantry can always move one hex in *any* direction.
            if ( isInfantry && step.getMpUsed() == 0 ) {
                firstStep = true;
            }

            // update prone state
            if (stepType == MovePath.STEP_GO_PRONE) {
                isProne = true;
            } else if (stepType == MovePath.STEP_GET_UP) {
                isProne = false;
            }
        }
    }
    
    /**
     * Check the last steps for stacking violations or other problems.
     * MoveStep backwards until we get to the first (last) legal step.  Then check 
     * if we have a violation.  If we do, then that step's illegal.  Check the 
     * next step backwards.  Otherwise, stop checking.
     */
    private static void compileLastStep(Game game, int entityId, MovePath md) {
        final Entity entity = game.getEntity(entityId);
        
        for (int i = md.length() - 1; i >= 0; i--) {
            final MoveStep step = md.getStep(i);
            final Hex destHex = game.board.getHex(step.getPosition());
            
            // skip steps that are not the last step
            if (step.getMovementType() == Entity.MOVE_ILLEGAL) {
                continue;
            }
            
            // check for stacking violations
            final Entity violation = stackingViolation(game, entityId, step.getPosition());
            if (violation != null
                    && step.getType() != MovePath.STEP_CHARGE
                    && step.getType() != MovePath.STEP_DFA) {
                // can't move here
                step.setMovementType(Entity.MOVE_ILLEGAL);
                continue;
            }
            
            // Check again for illegal terrain, in case of jumping.  We're
            // allowed to enter prohibited terrain via a road or bridge.
            if ( entity.isHexProhibited(destHex) && !step.isPavementStep() ) {
                step.setMovementType(Entity.MOVE_ILLEGAL);
                continue;
            }
            
            // we've found the last step and it was legal, so stop checking
            break;
        }
    }
    
    /**
     * Returns an entity if the specified entity would cause a stacking
     * violation entering a hex, or returns null if it would not.
     *
     * The returned entity is the entity causing the violation.
     */
    public static Entity stackingViolation(Game game, int enteringId, Coords coords) {
        Entity entering = game.getEntity(enteringId);
        return stackingViolation( game, entering, coords, null );
    }

    /**
     * When compiling an unloading step, both the transporter and the unloaded
     * unit probably occupy some other position on the board.
     */
    private static Entity stackingViolation( Game game,
                                             Entity entering,
                                             Coords coords,
                                             Entity transport ) {
        boolean isMech = entering instanceof Mech;
        Entity firstEntity = transport;

        // Walk through the entities in the given hex.
        for (Enumeration i = game.getEntities(coords); i.hasMoreElements();) {
            final Entity inHex = (Entity)i.nextElement();
            
            // Don't compare the entering entity to itself.
            if (inHex.equals(entering)) {
                continue;
            }

            // Ignore the transport of the entering entity.
            if ( inHex.equals(transport) ) {
                continue;
            }
            
            // DFAing units don't count towards stacking
            if (inHex.isMakingDfa()) {
                continue;
            }
            
            // If the entering entity is a mech,
            // then any other mech in the hex is a violation.
            if (isMech && (inHex instanceof Mech)) {
                return inHex;
            }
            
            // Otherwise, if there are two present entities controlled
            // by this player, returns a random one of the two.
            // Somewhat arbitrary, but how else should we resolve it?
            if ( !inHex.getOwner().isEnemyOf(entering.getOwner()) ) {
                if (firstEntity == null) {
                    firstEntity = inHex;
                } else {
                    return d6() > 3 ? firstEntity : inHex;
                }
            }

        }
        
        // okay, all clear
        return null;
    }

    /**
     * Checks to make sure that the jump as a whole is legal, and marks
     * all steps as illegal if it is not.
     *
     * An illegal jump either does not go anywhere, or takes a longer path
     * than is necessary to the destination.
     *
     * This function assumes that at least the position and mpUsed for each
     * step have been properly calculated and that the movement it is given
     * is jumping movement.
     */
    private static void compileJumpCheck(Game game, int entityId, MovePath md) {
        final Entity entity = game.getEntity(entityId);
        Coords start = entity.getPosition();
        Coords land = md.getStep(md.length() - 1).getPosition();
        int distance = start.distance(land);
        int mp = md.getStep(md.length() - 1).getMpUsed();
        
        if (distance < 1 || mp > distance) {
            // whole movement illegal
            for (Enumeration i = md.getSteps(); i.hasMoreElements();) {
                MoveStep step = (MoveStep)i.nextElement();
                step.setMovementType(Entity.MOVE_ILLEGAL);
            }
        }
    }
    
    /**
     * Amount of movement points required to move from start to dest
     */
    public static int getMovementCostFor(Game game, int entityId, 
                                             Coords src, Coords dest,
                                             boolean isJumping) {
        final Entity entity = game.getEntity(entityId);
        final int moveType = entity.getMovementType();
        final Hex srcHex = game.board.getHex(src);
        final Hex destHex = game.board.getHex(dest);
        final boolean isInfantry = (entity instanceof Infantry);
        final boolean isPavementStep = canMoveOnPavement( game, src, dest );
        
        // arguments valid?
        if (entity == null) {
            throw new IllegalArgumentException("Entity invalid.");
        }
        if (src.distance(dest) > 1) {
            throw new IllegalArgumentException("Coordinates must be adjacent.");
        }
        if (srcHex == null || destHex == null) {
            throw new IllegalArgumentException("Coordinates must be on the board.");
        }
        
        // jumping always costs 1
        if (isJumping) {
            return 1;
        }
        
        int mp = 1;

        // Account for terrain, unless we're moving along a road.
        if ( !isPavementStep ) {

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
            /* TODO: uncomment me in v0.29.1
            if ( destHex.contains(Terrain.SWAMP) ) {
                mp += 1;
            }
            */
        } // End not-along-road
        
        // account for elevation?
        // TODO: allow entities to occupy different levels of buildings.
        int nSrcEl = entity.elevationOccupied(srcHex);
        int nDestEl = entity.elevationOccupied(destHex);
        int nMove = entity.getMovementType();

        if (nSrcEl != nDestEl) {
            int delta_e = Math.abs(nSrcEl - nDestEl);
            
            // Infantry and ground vehicles are charged double.
            if (isInfantry
                || (nMove == Entity.MovementType.TRACKED
                    || nMove == Entity.MovementType.WHEELED
                    || nMove == Entity.MovementType.HOVER)) {
                delta_e *= 2;
            }
            mp += delta_e;
        }

        // If we entering a building, all non-infantry pay additional MP.
        if ( nDestEl < destHex.levelOf( Terrain.BLDG_ELEV ) &&
             !(entity instanceof Infantry) ) {
            Building bldg = game.board.getBuildingAt( dest );
            mp += bldg.getType();
        }

        return mp;
    }
    
    /**
     * Is movement possible from start to dest?
     * 
     * This makes the assumtion that entity.getPosition() returns the 
     * position the movement started in.
     *
     * This method is called from compileIllegal.
     */
    public static boolean isMovementPossible(Game game, int entityId, 
                                             Coords src, Coords dest,
                                             int entityMoveType,
                                             int stepType, boolean firstStep) {
        final Entity entity = game.getEntity(entityId);
        final Hex srcHex = game.board.getHex(src);
        final Hex destHex = game.board.getHex(dest);
        final boolean isPavementStep = canMoveOnPavement( game, src, dest );
        
        // arguments valid?
        if (entity == null) {
            throw new IllegalArgumentException("Entity invalid.");
        }
        if (src.distance(dest) > 1) {
            throw new IllegalArgumentException("Coordinates must be adjacent.");
        }
        
        if (entityMoveType == Entity.MOVE_ILLEGAL) {
            // that was easy
            return false;
        }
        // super-easy
        if (entity.isImmobile()) {
            return false;
        }
        // another easy check
        if (!game.board.contains(dest)) {
            return false;
        }

        // Swarming entities can't move.
        if ( Entity.NONE != entity.getSwarmTargetId() ) {
            return false;
        }

        // The entity is trying to load.  Check for a valid move.
        if ( stepType == MovePath.STEP_LOAD ) {

            // Transports can't load after the first step.
            if ( !firstStep ) {
                return false;
            }

            // Find the unit being loaded.
            Entity other = null;
            Enumeration entities = game.getEntities( src );
            while ( entities.hasMoreElements() ) {

                // Is the other unit friendly and not the current entity?
                other = (Entity)entities.nextElement();
                if ( entity.getOwner() == other.getOwner() &&
                     !entity.equals(other) ) {

                    // The moving unit should be able to load the other unit.
                    if ( !entity.canLoad(other) ) {
                        return false;
                    }

                    // The other unit should be able to have a turn.
                    if ( !other.isSelectableThisTurn(game) ) {
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
            if ( other == null ) {
                return false;
            }

        } // End STEP_LOAD-checks

        // mechs dumping ammo can't run
        boolean bDumping = false;
        for (Enumeration e = entity.getAmmo(); e.hasMoreElements(); ) {
            if (((Mounted)e.nextElement()).isDumping()) {
                bDumping = true;
                break;
            }
        }
        if ( bDumping && ( entityMoveType == Entity.MOVE_RUN ||
                           entityMoveType == Entity.MOVE_JUMP ) ) {
            return false;
        }
        
        // check elevation difference > max
        int nSrcEl = entity.elevationOccupied(srcHex);
        int nDestEl = entity.elevationOccupied(destHex);
        int nMove = entity.getMovementType();
        
        if (entityMoveType != Entity.MOVE_JUMP 
            && Math.abs(nSrcEl - nDestEl) > entity.getMaxElevationChange()) {
            return false;
        }
        // units moving backwards may not change elevation levels (I think this rule's dumb)
        if ((stepType == MovePath.STEP_BACKWARDS || stepType == MovePath.STEP_LATERAL_LEFT_BACKWARDS
            || stepType == MovePath.STEP_LATERAL_RIGHT_BACKWARDS) && nSrcEl != nDestEl) {
            return false;
        }

        // Can't run into water unless hovering, or using a bridge.
        if (entityMoveType == Entity.MOVE_RUN 
            && nMove != Entity.MovementType.HOVER
            && destHex.levelOf(Terrain.WATER) > 0 && 
            !firstStep && !isPavementStep) {
            return false;
        }
        
        // ugh, stacking checks.  well, maybe we're immune!
        if (entityMoveType != Entity.MOVE_JUMP
        && stepType != MovePath.STEP_CHARGE
        && stepType != MovePath.STEP_DFA) {
            // can't move a mech into a hex with an enemy mech
            if (entity instanceof Mech && isEnemyMechIn(game, entityId, dest)) {
                return false;
            }

            // Can't move out of a hex with an enemy unit unless we started
            // there, BUT we're allowed to turn, unload, or go prone.
            if ( isEnemyUnitIn(game, entityId, src) &&
                 !src.equals(entity.getPosition()) &&
                 stepType != MovePath.STEP_TURN_LEFT &&
                 stepType != MovePath.STEP_TURN_RIGHT &&
                 stepType != MovePath.STEP_UNLOAD &&
                 stepType != MovePath.STEP_GO_PRONE ) {
                return false;
            }
            
        }

        // can't jump over too-high terrain
        if (entityMoveType == Entity.MOVE_JUMP
            && destHex.getElevation() 
               > (entity.getElevation() +
                  entity.getJumpMPWithTerrain())) {
            return false;
        }
        
        // Certain movement types have terrain restrictions; terrain
        // restrictions are lifted when moving along a road or bridge.
        if (entityMoveType != Entity.MOVE_JUMP
            && entity.isHexProhibited(destHex) 
            && !isPavementStep) {
            return false;
        }

        // If we are *in* restricted terrain, we can only leave via roads.
        if ( entityMoveType != Entity.MOVE_JUMP
             && entity.isHexProhibited(srcHex)
             && !isPavementStep ) {
            return false;
        }

        return true;
    }
    
    /**
     * Returns true if there is a mech that is an enemy of the specified unit
     * in the specified hex.  This is only called for stacking purposes, and
     * so does not return true if the enemy mech is currenly making a DFA.
     */
    public static boolean isEnemyMechIn(Game game, int entityId, Coords coords) {
        Entity entity = game.getEntity(entityId);
        for (Enumeration i = game.getEntities(coords); i.hasMoreElements();) {
            final Entity inHex = (Entity)i.nextElement();
            if (inHex instanceof Mech && inHex.isEnemyOf(entity)
            && !inHex.isMakingDfa()) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Returns true if there is any unit that is an enemy of the specified unit
     * in the specified hex.  This is only called for stacking purposes, and
     * so does not return true if the enemy unit is currenly making a DFA.
     */
    public static boolean isEnemyUnitIn(Game game, int entityId, Coords coords) {
        Entity entity = game.getEntity(entityId);
        for (Enumeration i = game.getEntities(coords); i.hasMoreElements();) {
            final Entity inHex = (Entity)i.nextElement();
            if (inHex.isEnemyOf(entity) && !inHex.isMakingDfa()) {
                return true;
            }
        }
        return false;
    }
    
    /** 
     * @return true if a piloting skill roll is needed to traverse the terrain
     */
    public static boolean isPilotingSkillNeeded(Game game, int entityId,
                                                Coords src, Coords dest,
                                                int movementType,
                                                boolean isTurning,
                                                boolean prevStepIsOnPavement) {
        final Entity entity = game.getEntity(entityId);
        final Hex srcHex = game.board.getHex(src);
        final Hex destHex = game.board.getHex(dest);
        final boolean isInfantry = ( entity instanceof Infantry );
        final boolean isPavementStep = canMoveOnPavement( game, src, dest );

        // arguments valid?
        if (entity == null) {
            throw new IllegalArgumentException("Entity invalid.");
        }
        if (src.distance(dest) > 1) {
            throw new IllegalArgumentException("Coordinates must be adjacent.");
        }
        
        // let's only worry about actual movement, please
        if (src.equals(dest)) {
            return false;
        }
        
        // check for rubble
        if (movementType != Entity.MOVE_JUMP
            && destHex.levelOf(Terrain.RUBBLE) > 0
            && !isInfantry) {
            return true;
        }
        
        // Check for water unless we're a hovercraft or using a bridge.
        if (movementType != Entity.MOVE_JUMP
            && entity.getMovementType() != Entity.MovementType.HOVER
            && destHex.levelOf(Terrain.WATER) > 0
            && !isPavementStep) {
            return true;
        }

        // Check for skid.  Please note, the skid will be rolled on the
        // current step, but starts from the previous step's location.
        // TODO: add check for elevation of pavement, road,
        //       or bridge matches entity elevation.
        /* Bug 754610: Revert fix for bug 702735.
        if ( ( srcHex.contains(Terrain.PAVEMENT) ||
               srcHex.contains(Terrain.ROAD) ||
               srcHex.contains(Terrain.BRIDGE) ) 
        */
        if ( prevStepIsOnPavement
        //   && overallMoveType == Entity.MOVE_RUN
             && movementType == Entity.MOVE_RUN
             && isTurning
             && !isInfantry ) {
            return true;
        }

        // If we entering or leaving a building, all non-infantry
        // need to make a piloting check to avoid damage.
        // TODO: allow entities to occupy different levels of buildings.
        int nSrcEl = entity.elevationOccupied(srcHex);
        int nDestEl = entity.elevationOccupied(destHex);
        if ( ( nSrcEl < srcHex.levelOf( Terrain.BLDG_ELEV ) ||
               nDestEl < destHex.levelOf( Terrain.BLDG_ELEV ) ) &&
             !(entity instanceof Infantry) ) {
            return true;
        }

        return false;
    }

    /**
     * Can the defending unit be displaced from the source to the destination?
     */
    public static boolean isValidDisplacement(Game game, int entityId, 
                                              Coords src, int direction) {
        return isValidDisplacement(game, entityId, src, 
                                   src.translated(direction));
    }
    /**
     * Can the defending unit be displaced from the source to the destination?
     */
    public static boolean isValidDisplacement(Game game, int entityId, 
                                              Coords src, Coords dest) {
        final Entity entity = game.getEntity(entityId);
        final Hex srcHex = game.board.getHex(src);
        final Hex destHex = game.board.getHex(dest);
        final Coords[] intervening = intervening(src, dest);
        final int direction = src.direction(dest);
        
        // arguments valid?
        if (entity == null) {
            throw new IllegalArgumentException("Entity invalid.");
        }
        
        // an easy check
        if (!game.board.contains(dest)) {
            return false;
        }

        // can't be displaced into prohibited terrain
        if (entity.isHexProhibited(destHex)) {
            return false;
        }
        
        // can't go up more levels than normally possible
        for (int i = 0; i < intervening.length; i++) {
            final Hex hex = game.board.getHex(intervening[i]);
            int change = entity.elevationOccupied(hex) - entity.elevationOccupied(srcHex);
            if (change > entity.getMaxElevationChange()) {
                return false;
            }
        }
        
        // if there's an entity in the way, can they be displaced in that direction?
        Entity inTheWay = stackingViolation(game, entityId, dest);
        if (inTheWay != null) {
            return isValidDisplacement(game, inTheWay.getId(), inTheWay.getPosition(), direction);
        }
        
        // okay, that's about all the checks
        return true;
    }
    
    /**
     * Gets a valid displacement, from the hexes around src, as close to the
     * original direction as is possible.
     * 
     * @return valid displacement coords, or null if none
     */
    public static Coords getValidDisplacement(Game game, int entityId, 
                                              Coords src, int direction) {
        // check the surrounding hexes, nearest to the original direction first
        int[] offsets = {0, 1, 5, 2, 4, 3};
        for (int i = 0; i < offsets.length; i++) {
            Coords dest = src.translated((direction + offsets[i]) % 6);
            if (isValidDisplacement(game, entityId, src, dest)) {
                return dest;
            }
        }
        // have fun being insta-killed!
        return null;
    }
    
    /**
     * Gets a preferred displacement.  Right now this picks the surrounding hex
     * with the highest elevation that is a valid displacement.
     * 
     * @return valid displacement coords, or null if none
     */
    public static Coords getPreferredDisplacement(Game game, int entityId, 
                                              Coords src, int direction) {
        final Entity entity = game.getEntity(entityId);
        int highestElev = Integer.MIN_VALUE;
        Coords highest = null;
        
        // check the surrounding hexes, nearest to the original direction first
        int[] offsets = {0, 1, 5, 2, 4, 3};
        for (int i = 0; i < offsets.length; i++) {
            Coords dest = src.translated((direction + offsets[i]) % 6);
            if (isValidDisplacement(game, entityId, src, dest)) {
                 // assume that if the displacement's valid, hex is !null
                Hex hex = game.board.getHex(dest);
                int elevation = entity.elevationOccupied(hex);
                if (elevation > highestElev) {
                    highestElev = elevation;
                    highest = dest;
                }
            }
        }
        return highest;
    }
    
    /**
     * Gets a hex to displace a missed charge to.  Picks left or right, first 
     * preferring higher hexes, then randomly, or returns the base hex if 
     * they're impassible.
     */
    public static Coords getMissedChargeDisplacement(Game game, int entityId, Coords src, int direction) {
        Coords first = src.translated((direction + 1) % 6);
        Coords second = src.translated((direction + 5) % 6);
        Hex firstHex = game.board.getHex(first);
        Hex secondHex = game.board.getHex(second);
        Entity entity = game.getEntity(entityId);
        
        if (firstHex == null || secondHex == null) {
            // leave it, will be handled
        } else if (entity.elevationOccupied(firstHex) > entity.elevationOccupied(secondHex)) {
            // leave it
        } else if (entity.elevationOccupied(firstHex) < entity.elevationOccupied(secondHex)) {
            // switch
            Coords temp = first;
            first = second;
            second = temp;
        } else if (random.d6() > 3) {
            // switch randomly
            Coords temp = first;
            first = second;
            second = temp;
        }
        
        if (isValidDisplacement(game, entityId, src, src.direction(first))) {
            return first;
        } else if (isValidDisplacement(game, entityId, src, src.direction(second))) {
            return second;
        } else {
            return src;
        }
    }
    
    /**
     * @deprecated no more prevattacks
     */
    public static ToHitData toHitWeapon(Game game, WeaponAttackAction waa, Vector prevAttacks) {
        return toHitWeapon(game, waa.getEntityId(), game.getTarget(waa.getTargetType(), waa.getTargetId()),
                           waa.getWeaponId(), prevAttacks);
    }
    public static ToHitData toHitWeapon(Game game, WeaponAttackAction waa) {
        return toHitWeapon(game, waa.getEntityId(), 
                           game.getTarget(waa.getTargetType(), waa.getTargetId()),
                           waa.getWeaponId(),
                           waa.getAimedLocation(),
                           waa.getAimingMode());
    }
    
    /**
     * To-hit number for attacker firing a weapon at the target.
     * 
     * @deprecated no more prevattacks
     */
    public static ToHitData toHitWeapon(Game game, int attackerId, Targetable target, int weaponId, Vector prevAttacks) {
         // ignore prevAttacks
    return toHitWeapon(game, attackerId, target, weaponId, Mech.LOC_NONE, FiringDisplay.AIM_MODE_NONE);
  }

    public static ToHitData toHitWeapon(Game game, int attackerId, Targetable target, int weaponId) {
    return toHitWeapon(game, attackerId, target, weaponId, Mech.LOC_NONE, FiringDisplay.AIM_MODE_NONE);
  }
    /**
     * To-hit number for attacker firing a weapon at the target.
     */
    public static ToHitData toHitWeapon(Game game, int attackerId, Targetable target, int weaponId, int aimingAt, int aimingMode) {
        final Entity ae = game.getEntity(attackerId);
        Entity te = null;
        if (target.getTargetType() == Targetable.TYPE_ENTITY) {
            te = (Entity) target;
        }
        final Mounted weapon = ae.getEquipment(weaponId);
        final WeaponType wtype = (WeaponType)weapon.getType();
        boolean isAttackerInfantry = (ae instanceof Infantry);
        boolean isWeaponInfantry = wtype.hasFlag(WeaponType.F_INFANTRY);
        // 2003-01-02 BattleArmor MG and Small Lasers have unlimited ammo.
        // 2002-09-16 Infantry weapons have unlimited ammo.
        final boolean usesAmmo = wtype.getAmmoType() != AmmoType.T_NA &&
            wtype.getAmmoType() != AmmoType.T_BA_MG &&
            wtype.getAmmoType() != AmmoType.T_BA_SMALL_LASER &&
            !isWeaponInfantry;
        final Mounted ammo = usesAmmo ? weapon.getLinked() : null;
        final AmmoType atype = ammo == null ? null : (AmmoType)ammo.getType();
        final boolean targetInBuilding = isInBuilding( game, te );
        boolean isIndirect = wtype.getAmmoType() == AmmoType.T_LRM
            && weapon.curMode().equals("Indirect");
        boolean isInferno =
            ( atype != null &&
              atype.getMunitionType() == AmmoType.M_INFERNO ) ||
            ( isWeaponInfantry &&
              wtype.hasFlag(WeaponType.F_INFERNO) );

        ToHitData toHit = null;
        
        // make sure weapon can deliver minefield
        if (target.getTargetType() == Targetable.TYPE_MINEFIELD_DELIVER &&
        	!AmmoType.canDeliverMinefield(atype)) {
			return new ToHitData(ToHitData.IMPOSSIBLE, "Weapon can't deliver minefields");
        }
        
        if (atype != null && 
        	atype.getAmmoType() == AmmoType.T_LRM &&
        	atype.getMunitionType() == AmmoType.M_THUNDER &&
        	target.getTargetType() != Targetable.TYPE_MINEFIELD_DELIVER) {
			return new ToHitData(ToHitData.IMPOSSIBLE, "Weapon can only deliver minefields");        	
        }
        
        // make sure weapon can clear minefield
		if (target instanceof MinefieldTarget && 
			!AmmoType.canClearMinefield(atype)) {
			return new ToHitData(ToHitData.IMPOSSIBLE, "Weapon can't clear minefields");
		}
		
        // can't target yourself
        if (ae.equals(te)) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "You can't target yourself");
        }
        
        // weapon operational?
        if (weapon.isDestroyed() || weapon.isBreached()) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Weapon not operational.");
        }
        
        // got ammo?
        if ( usesAmmo && (ammo == null || ammo.getShotsLeft() == 0) ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Weapon out of ammo.");
        }

        // Are we dumping that ammo?
        if ( usesAmmo && ammo.isDumping() ) {
            ae.loadWeapon( weapon );
            if ( ammo.getShotsLeft() == 0 || ammo.isDumping() ) {
                return new ToHitData( ToHitData.IMPOSSIBLE,
                                      "Dumping remaining ammo." );
            }
        }
        
        // is the attacker even active?
        if (ae.isShutDown() || !ae.getCrew().isActive()) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Attacker is in no condition to fire weapons.");
        }

        // sensors operational?
        final int sensorHits = ae.getDestroyedCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_SENSORS, Mech.LOC_HEAD);
        if (sensorHits > 1) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Attacker sensors destroyed.");
        }

        // Is the weapon blocked by a passenger?
        if ( ae.isWeaponBlockedAt(weapon.getLocation(), weapon.isRearMounted()) ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Weapon blocked by passenger.");
        }

        // Can't target a entity conducting a swarm attack.
        if ( te != null && Entity.NONE != te.getSwarmTargetId() ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target is swarming a Mek.");
        }

        // Infantry can't clear woods.
        if ( isAttackerInfantry &&
             Targetable.TYPE_HEX_CLEAR == target.getTargetType() ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Infantry can not clear woods.");
        }

        // Some weapons can't cause fires, but Infernos always can.
        if ( wtype.hasFlag(WeaponType.F_NO_FIRES) && !isInferno &&
             Targetable.TYPE_HEX_IGNITE == target.getTargetType() ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Weapon can not cause fires.");
        }

        // Can't target infantry with Inferno rounds (BMRr, pg. 141).
        if ( te instanceof Infantry && isInferno ) {
            return new ToHitData( ToHitData.IMPOSSIBLE,
                  "Can not target infantry with Inferno rounds." );
        }

        // Can't raise the heat of infantry or tanks.
        if ( wtype.hasFlag(WeaponType.F_FLAMER) &&
             wtype.hasModes() &&
             weapon.curMode().equals("Heat") &&
             !(te instanceof Mech) ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Can only raise the heat level of Meks.");
        }

        // Handle solo attack weapons.
        if ( wtype.hasFlag(WeaponType.F_SOLO_ATTACK) ) {
            for ( Enumeration i = game.getActions();
                  i.hasMoreElements(); ) {
                Object o = i.nextElement();
                if (!(o instanceof WeaponAttackAction)) {
                    continue;
                }
                WeaponAttackAction prevAttack = (WeaponAttackAction)o;
                if (prevAttack.getEntityId() == attackerId) {

                    // If the attacker fires another weapon, this attack fails.
                    if ( weaponId != prevAttack.getWeaponId() ) {
                        return new ToHitData(ToHitData.IMPOSSIBLE,
                                             "Other weapon attacks declared.");
                    }
                }
            }
        } // End current-weapon-is-solo
        
        int attEl = ae.absHeight();
        int targEl;
        
        if (te == null) {
            targEl = game.board.getHex(target.getPosition()).floor();
        } else {
            targEl = te.absHeight();
        }
        
        //TODO: mech making DFA could be higher if DFA target hex is higher
        //      BMRr pg. 43, "attacking unit is considered to be in the air
        //      above the hex, standing on an elevation 1 level higher than
        //      the target hex or the elevation of the hex the attacker is
        //      in, whichever is higher."
        
        // check if indirect fire is valid
        if (isIndirect && !game.getOptions().booleanOption("indirect_fire")) {
      return new ToHitData(ToHitData.IMPOSSIBLE, "Indirect fire option not enabled");
        }
        
        // if we're doing indirect fire, find a spotter
        Entity spotter = null;
        if (isIndirect) {
            spotter = findSpotter(game, ae, target);
            if (spotter == null) {
                return new ToHitData( ToHitData.IMPOSSIBLE, "No available spotter");
            }
        }
        
        // check LOS (indirect LOS is from the spotter)
        LosEffects los;
        ToHitData losMods;
        if (!isIndirect) {
            los = calculateLos(game, attackerId, target);
            losMods = losModifiers(los);
        } else {
            los = calculateLos(game, spotter.getId(), target);
            // do not count attacker partial cover in indirect fire
            los.setAttackerCover(false);
            losMods = losModifiers(los);
        }

        // if LOS is blocked, block the shot
        if (losMods.getValue() == ToHitData.IMPOSSIBLE) {
            return losMods;
        }

        // Must target infantry in buildings from the inside.
        if ( targetInBuilding &&
             te instanceof Infantry &&
             null == los.getThruBldg() ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Attack on infantry crosses building exterior wall.");
        }

        // attacker partial cover means no leg weapons
        if (los.attackerCover && ae.locationIsLeg(weapon.getLocation())) {
            return new ToHitData(ToHitData.IMPOSSIBLE,
                                 "Nearby terrain blocks leg weapons.");
        }

        // Weapon in arc?
        if (!isInArc(game, attackerId, weaponId, target)) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target not in arc.");
        }

        // Leg attacks, Swarm attacks, and
        // Mine Launchers don't use gunnery.
        if ( Infantry.LEG_ATTACK.equals( wtype.getInternalName() ) ) {
            toHit = Compute.getLegAttackBaseToHit( ae, te );

            // Return if the attack is impossible.
            if ( ToHitData.IMPOSSIBLE == toHit.getValue() ) {
                return toHit;
            }

            // If the attacker has Assault claws, give a -1 modifier.
            // We can stop looking when we find our first match.
            for ( Enumeration iter = ae.getMisc(); iter.hasMoreElements(); ) {
                Mounted mount = (Mounted) iter.nextElement();
                EquipmentType equip = mount.getType();
                if ( BattleArmor.ASSAULT_CLAW.equals
                     (equip.getInternalName()) ) {
                    toHit.addModifier( -1, "attacker has assault claws" );
                    break;
                }
            }
        }
        else if ( Infantry.SWARM_MEK.equals( wtype.getInternalName() ) ) {
            toHit = Compute.getSwarmMekBaseToHit( ae, te );

            // Return if the attack is impossible.
            if ( ToHitData.IMPOSSIBLE == toHit.getValue() ) {
                return toHit;
            }

            // If the attacker has Assault claws, give a -1 modifier.
            // We can stop looking when we find our first match.
            for ( Enumeration iter = ae.getMisc(); iter.hasMoreElements(); ) {
                Mounted mount = (Mounted) iter.nextElement();
                EquipmentType equip = mount.getType();
                if ( BattleArmor.ASSAULT_CLAW.equals
                     (equip.getInternalName()) ) {
                    toHit.addModifier( -1, "attacker has assault claws" );
                    break;
                }
            }
        }
        else if ( Infantry.STOP_SWARM.equals( wtype.getInternalName() ) ) {
            // Can't stop if we're not swarming, otherwise automatic.
            if ( Entity.NONE == ae.getSwarmTargetId() ) {
                return new ToHitData( ToHitData.IMPOSSIBLE,
                                      "Not swarming a Mek." );
            } else {
                return new ToHitData( ToHitData.AUTOMATIC_SUCCESS,
                                      "End swarm attack." );
            }
        }
        else if ( BattleArmor.MINE_LAUNCHER.equals(wtype.getInternalName()) ) {
            // Mine launchers can not hit infantry.
            if ( te instanceof Infantry ) {
                return new ToHitData( ToHitData.IMPOSSIBLE, 
                                      "Can not attack infantry." );
            } else {
                toHit = new ToHitData(8, "magnetic mine attack");
            }
        }
        // Swarming infantry always hit their target, but
        // they can only target the Mek they're swarming.
        else if ( te != null && ae.getSwarmTargetId() == te.getId() ) {
            // Only certain weapons can be used in a swarm attack.
            if ( wtype.getDamage() == 0 ) {
                return new ToHitData( ToHitData.IMPOSSIBLE,
                                      "Weapon causes no damage." );
            } else {
                return new ToHitData( ToHitData.AUTOMATIC_SUCCESS,
                                      "Attack during swarm.",
                                      ToHitData.HIT_SWARM,
                                      ToHitData.SIDE_FRONT );
            }
        }
        else if ( Entity.NONE != ae.getSwarmTargetId() ) {
            return new ToHitData( ToHitData.IMPOSSIBLE,
                                  "Must target the Mek being swarmed." );
        }
        else {
            toHit = new ToHitData(ae.crew.getGunnery(), "gunnery skill");
        }
        
        // determine some more variables
        int aElev = ae.getElevation();
        int tElev = target.getElevation();
        int distance = effectiveDistance(game, ae, target);

        // Attacks against adjacent buildings automatically hit.
        if ( distance == 1 &&
             ( target.getTargetType() == Targetable.TYPE_BUILDING ||
               target.getTargetType() == Targetable.TYPE_BLDG_IGNITE ) ) {
            return new ToHitData( ToHitData.AUTOMATIC_SUCCESS, 
                                  "Targeting adjacent building." );
        }

        // Attacks against buildings from inside automatically hit.
        if ( null != los.getThruBldg() &&
             ( target.getTargetType() == Targetable.TYPE_BUILDING ||
               target.getTargetType() == Targetable.TYPE_BLDG_IGNITE ) ) {
            return new ToHitData( ToHitData.AUTOMATIC_SUCCESS, 
                                  "Targeting building from inside (are you SURE this is a good idea?)." );
        }

        // add range mods
        toHit.append(getRangeMods(game, ae, weaponId, target));

        // Battle Armor targets are hard for Meks and Tanks to hit.
        if ( !isAttackerInfantry && te != null && te instanceof BattleArmor ) {
            toHit.addModifier( 1, "battle armor target" );
        }

        // Indirect fire has a +1 mod
        if (isIndirect) {
            toHit.addModifier( 1, "indirect fire" );
        }

        // attacker movement
        toHit.append(getAttackerMovementModifier(game, attackerId));

        // target movement
        if (te != null) {
            ToHitData thTemp = getTargetMovementModifier(game, target.getTargetId());
            toHit.append(thTemp);

            // precision ammo reduces this modifier
            if (atype != null && atype.getAmmoType() == AmmoType.T_AC && 
                atype.getMunitionType() == AmmoType.M_PRECISION) {
                int nAdjust = Math.min(2, thTemp.getValue());
                if (nAdjust > 0) {
                    toHit.append(new ToHitData(-nAdjust, "Precision Ammo"));
                }
            }
        }

        // spotter movement, if applicable
        if (isIndirect) {
            toHit.append(getAttackerMovementModifier(game, spotter.getId()));
        }

        // attacker terrain
        toHit.append(getAttackerTerrainModifier(game, attackerId));
        
        // target terrain, not applicable when delivering minefields
        if (target.getTargetType() != Targetable.TYPE_MINEFIELD_DELIVER) {
            toHit.append(getTargetTerrainModifier(game, target));
        }
        
        // target in water?
        Hex attHex = game.board.getHex(ae.getPosition());
        Hex targHex = game.board.getHex(target.getPosition());
        if (targHex.contains(Terrain.WATER) && targHex.surface() == targEl && te.height() > 0) { //target in partial water
            los.targetCover = true;
            losMods = losModifiers(los);
        }

        // add in LOS mods that we've been keeping
        toHit.append(losMods);
        
        // secondary targets modifier...
    toHit.append(getSecondaryTargetMod(game, ae, target));

        // heat
        if (ae.getHeatFiringModifier() != 0) {
            toHit.addModifier(ae.getHeatFiringModifier(), "heat");
        }

        // actuator & sensor damage to attacker
    toHit.append(getDamageWeaponMods(ae, weapon));
        
        // target immobile
    toHit.append(getImmobileMod(target, aimingAt, aimingMode));
        
        // attacker prone
    toHit.append(getProneMods(game, ae, weaponId));

        // target prone
        if (te != null && te.isProne()) {
            // easier when point-blank
            if (distance <= 1) {
                // BMRr, pg. 72: Swarm Mek attacks get "an additional -4
                // if the BattleMech is prone or immoble."  I interpret
                // this to mean that the bonus gets applied *ONCE*.
                if ( Infantry.SWARM_MEK.equals( wtype.getInternalName() ) ) {
                    // If the target is immoble, don't give a bonus for prone.
                    if ( !te.isImmobile() ) {
                        toHit.addModifier(-4, "swarm prone target");
                    }
                } else {
                    toHit.addModifier(-2, "target prone and adjacent");
                }
            }
            // harder at range
            else {
                toHit.addModifier(1, "target prone and at range");
            }
        }
        
        // weapon to-hit modifier
        if (wtype.getToHitModifier() != 0) {
            toHit.addModifier(wtype.getToHitModifier(), "weapon to-hit modifier");
        }        
        
        // ammo to-hit modifier
        if (usesAmmo && atype.getToHitModifier() != 0) {
            toHit.addModifier(atype.getToHitModifier(), "ammunition to-hit modifier");
        }        
        
        // add targeting computer (except with LBX cluster ammo)
        if (aimingMode == FiringDisplay.AIM_MODE_TARG_COMP &&
          aimingAt != Mech.LOC_NONE) {
          toHit.addModifier(3, "aiming with targeting computer");
        } else {
          if ( ae.hasTargComp() && wtype.hasFlag(WeaponType.F_DIRECT_FIRE) &&
               (!usesAmmo || atype.getMunitionType() != AmmoType.M_CLUSTER) ) {
              toHit.addModifier(-1, "targeting computer");
          }
      }

        // Change hit table for elevation differences inside building.
        if ( null != los.getThruBldg() && aElev != tElev ) {

            // Tanks get hit in a random side.
            if ( target instanceof Tank ) {
                toHit.setSideTable( ToHitData.SIDE_RANDOM );
            }

            // Meks have special tables for shots from above and below.
            else if ( target instanceof Mech ) {
                if ( aElev > tElev ) {
                    toHit.setHitTable( ToHitData.HIT_ABOVE );
                } else {
                    toHit.setHitTable( ToHitData.HIT_BELOW );
                }
            }

        }

        // Change hit table for partial cover, accomodate for partial underwater(legs)
        if (los.targetCover) {
            if ( ae.getLocationStatus(weapon.getLocation()) == Entity.LOC_WET && (targHex.contains(Terrain.WATER) && targHex.surface() == targEl && te.height() > 0) ) {
            //weapon underwater, target in partial water
                toHit.setHitTable(ToHitData.HIT_KICK);
            } else {
                toHit.setHitTable(ToHitData.HIT_PUNCH);
            }
        }
        
        // factor in target side
        if ( isAttackerInfantry && 0 == distance ) {
            // Infantry attacks from the same hex are resolved against the front.
            toHit.setSideTable( ToHitData.SIDE_FRONT );
        } else {
            toHit.setSideTable( targetSideTable(ae, target) );
        }
        
        // okay!
        return toHit;
    }
    
    /**
     * Finds the best spotter for the attacker.  The best spotter is the one
     * with the lowest attack modifiers, of course.  LOS modifiers and
     * movement are considered.
     */
    public static Entity findSpotter(Game game, Entity attacker, Targetable target) {
      Entity spotter = null;
      ToHitData bestMods = new ToHitData(ToHitData.IMPOSSIBLE, "");
      
        for (java.util.Enumeration i = game.getEntities(); i.hasMoreElements();) {
            Entity other = (Entity)i.nextElement();
            if (!other.isSpotting() || attacker.isEnemyOf(other)) {
                continue; // useless to us...
            }
            // what are this guy's mods to the attack?
            LosEffects los = calculateLos(game, other.getId(), target);
            ToHitData mods = losModifiers(los);
            los.setTargetCover(false);
            mods.append(getAttackerMovementModifier(game, other.getId()));
            // is this guy a better spotter?
            if (true || mods.getValue() < bestMods.getValue()) {
                spotter = other;
                bestMods = mods;
            }
        }
    
        return spotter;
    }
  
	private static ToHitData getImmobileMod(Targetable target) {
		return getImmobileMod(target, Mech.LOC_NONE, FiringDisplay.AIM_MODE_NONE);
	}

	private static ToHitData getImmobileMod(Targetable target, int aimingAt, int aimingMode) {
		if (target.isImmobile()) {
			if ((aimingAt == Mech.LOC_HEAD) && 
				(aimingMode == FiringDisplay.AIM_MODE_IMMOBILE)) {
				return new ToHitData(3, "aiming at head");
			} else {
				return new ToHitData(-4, "target immobile");
			}
		} else {
			return null;
		}
	}

    /**
     * Determines the to-hit modifier due to range for an attack with the 
     * specified parameters. Includes minimum range, infantry 0-range 
     * mods, and target stealth mods.  Accounts for friendly C3 units.
     * 
     * @return the modifiers
     */
    private static ToHitData getRangeMods(Game game, Entity ae, int weaponId, Targetable target) {
        Mounted weapon = ae.getEquipment(weaponId);
        WeaponType wtype = (WeaponType)weapon.getType();
        int[] weaponRanges = wtype.getRanges();
        boolean isAttackerInfantry = (ae instanceof Infantry);
        boolean isWeaponInfantry = wtype.hasFlag(WeaponType.F_INFANTRY);
        boolean isLRMInfantry = isWeaponInfantry && wtype.getAmmoType() == AmmoType.T_LRM;
        boolean isIndirect = wtype.getAmmoType() == AmmoType.T_LRM
            && weapon.curMode().equals("Indirect");
    
        ToHitData mods = new ToHitData();
    
        // modify the ranges for ATM missile systems based on the ammo selected
        // TODO: this is not the right place to hardcode these
        if (wtype.getAmmoType() == AmmoType.T_ATM) {
            AmmoType atype = (AmmoType)weapon.getLinked().getType();
            if (atype.getMunitionType() == AmmoType.M_EXTENDED_RANGE) {
                weaponRanges = new int[] {4, 9, 18, 27};
            }
            else if (atype.getMunitionType() == AmmoType.M_HIGH_EXPLOSIVE) {
                weaponRanges = new int[] {0, 3, 6, 9};
            }
        }

        //is water involved?
        Hex attHex = game.board.getHex(ae.getPosition());
        Hex targHex = game.board.getHex(target.getPosition());
        int targEl;
        if (target == null) {
            targEl = game.board.getHex(target.getPosition()).floor();
        } else {

            targEl = target.absHeight();
        }

        if (ae.getLocationStatus(weapon.getLocation()) == Entity.LOC_WET) {
            weaponRanges = wtype.getWRanges();
            //HACK on ranges: for those without underwater range,
            // long == medium; iteration in rangeBracket() allows this
            if (weaponRanges[RANGE_SHORT] == 0) {
                return new ToHitData(ToHitData.IMPOSSIBLE,
                                     "Weapon cannot fire underwater."); 
            }
            if (!(targHex.contains(Terrain.WATER)) ||
                targHex.surface() <= target.getElevation()) {
                //target on land or over water
                return new ToHitData(ToHitData.IMPOSSIBLE,
                                     "Weapon underwater, but not target.");
            }
        } else if (targHex.contains(Terrain.WATER) &&
                   targHex.surface() > targEl) {
            //target completely underwater, weapon not
            return new ToHitData(ToHitData.IMPOSSIBLE,
                                 "Target underwater, but not weapon.");
        }

        // determine base distance & range bracket
        int distance = effectiveDistance(game, ae, target);
        int range = rangeBracket(distance, weaponRanges);
        
        // short circuit if at zero range or out of range
        if (range == RANGE_OUT_OF) {
            return new ToHitData(ToHitData.AUTOMATIC_FAIL, "Target out of range");
        }
        if (distance == 0 && !isAttackerInfantry) {
            return new ToHitData(ToHitData.AUTOMATIC_FAIL, "Only infantry shoot at zero range");
        }

        // find any c3 spotters that could help
        Entity c3spotter = findC3Spotter(game, ae, target);
        if (isIndirect) {
            c3spotter = ae; // no c3 when using indirect fire
        }
        int c3dist = effectiveDistance(game, c3spotter, target);
        int c3range = rangeBracket(c3dist, weaponRanges);
    
        // determine which range we're using
        int usingRange = Math.min(range, c3range);

    // add range modifier
    if (usingRange == range) {
      // no c3 adjustment
      if (range == RANGE_MEDIUM) {
        mods.addModifier(2, "medium range");
      }
      else if (range == RANGE_LONG) {
        mods.addModifier(4, "long range");
      }
    }
    else {
      // report c3 adjustment
      if (c3range == RANGE_SHORT) {
        mods.addModifier(0, "short range due to C3 spotter");
      }
      else if (c3range == RANGE_MEDIUM) {
        mods.addModifier(2, "medium range due to C3 spotter");
      }
    }
        
    // add infantry LRM maximum range penalty
    if (isLRMInfantry && distance == weaponRanges[RANGE_LONG]) {
      mods.addModifier(1, "infantry LRM maximum range");
    }
        
    // add infantry zero-range modifier
    // TODO: this is not the right place to hardcode these
    if (isWeaponInfantry && distance == 0) {
      // Infantry platoons attacking with infantry weapons can attack
      // in the same hex with a base of 2, except for flamers and
      // SRMs, which have a base of 3 and LRMs, which suffer badly.
      if (wtype.hasFlag(WeaponType.F_FLAMER)) {
        mods.addModifier(-1, "infantry flamer assault");
      } else if (wtype.getAmmoType() == AmmoType.T_SRM ) {
        mods.addModifier(-1, "infantry SRM assault");
      } else if (wtype.getAmmoType() != AmmoType.T_LRM) {
        mods.addModifier(-2, "infantry assault");
      }
    }
        
    // add minumum range modifier
    int minRange = weaponRanges[RANGE_MINIMUM];
    if (minRange > 0 && distance <= minRange) {
      int minPenalty = (minRange - distance) + 1;
      // Infantry LRMs suffer double minimum range penalties.
      if (isLRMInfantry) {
        mods.addModifier(minPenalty * 2, "infantry LRM minumum range");
      } else {
        mods.addModifier(minPenalty, "minumum range");
      }
    }
        
    // add any target stealth modifier
    if ((target instanceof Entity) && ((Entity)target).isStealthActive()) {
      mods.append(((Entity)target).getStealthModifier(usingRange));
    }

    return mods;
  }
  
  /**
   * Finds the effective distance between an attacker and a target.
   * Includes the distance bonus if the attacker and target are in the
   * same building and on different levels.
   * 
   * @return the effective distance
   */
  public static int effectiveDistance(Game game, Entity attacker, Targetable target) {
    int distance = attacker.getPosition().distance(target.getPosition());
    
    // If the attack is completely inside a building, add the difference
    // in elevations between the attacker and target to the range.
    // TODO: should the player be explcitly notified?
    if ( isInSameBuilding(game, attacker, target) ) {
      int aElev = attacker.getElevation();
      int tElev = target.getElevation();
      distance += Math.abs(aElev - tElev);
    }

    return distance;
  }

  /**
   * Returns the range bracket a distance falls into.
   */
  public static int rangeBracket(int distance, int[] ranges) {
    if (null == ranges || distance > ranges[RANGE_LONG]) {
      return RANGE_OUT_OF;
    }
    else if (distance > ranges[RANGE_MEDIUM]) {
      return RANGE_LONG;
    }
    else if (distance > ranges[RANGE_SHORT]) {
      return RANGE_MEDIUM;
    }
    else {
      return RANGE_SHORT;
    }
  }

    /**
     * Attempts to find a C3 spotter that is closer to the target than the
     * attacker.
     * @return A closer C3 spotter, or the attack if no spotters are found
     */
    private static Entity findC3Spotter(Game game, Entity attacker, Targetable target) {
    if (!attacker.hasC3() && !attacker.hasC3i()) {
      return attacker;
    }
    Entity c3spotter = attacker;
    int c3range = attacker.getPosition().distance(target.getPosition());

    for (java.util.Enumeration i = game.getEntities(); i.hasMoreElements();) {
      Entity friend = (Entity)i.nextElement();

      // TODO : can units being transported be used for C3 spotting?
      if ( attacker.equals(friend) ||
           !friend.isActive() ||
           !attacker.onSameC3NetworkAs(friend) ||
           !canSee(game, friend, target) ) {
        continue; // useless to us...
      }

      int buddyRange = effectiveDistance(game, friend, target);
      if(buddyRange < c3range) {
        c3range = buddyRange;
        c3spotter = friend;
      }

    }
    return c3spotter;
  }

  /**
     * Gets the modifiers, if any, that the mech receives from being prone.
     * @return any applicable modifiers due to being prone
     */
    private static ToHitData getProneMods(Game game, Entity attacker, int weaponId) {
    if (!attacker.isProne()) {
      return null; // no modifier
    }

    ToHitData mods = new ToHitData();
    Mounted weapon = attacker.getEquipment(weaponId);
        if ( attacker.entityIsQuad() ) {
            int legsDead = ((Mech)attacker).countDestroyedLegs();
            if (legsDead == 0) {
        // No legs destroyed: no penalty and can fire all weapons
              return null; // no modifier
            } else if ( legsDead >= 3 ) {
        return new ToHitData(ToHitData.IMPOSSIBLE, "Prone with three or more legs destroyed.");
      }
      // we have one or two dead legs...
      
            // Need an intact front leg
            if (attacker.isLocationDestroyed(Mech.LOC_RARM) && attacker.isLocationDestroyed(Mech.LOC_LARM)) {
                return new ToHitData(ToHitData.IMPOSSIBLE, "Prone with both front legs destroyed.");
            }
            
            // front leg-mounted weapons have addidional trouble
            if (weapon.getLocation() == Mech.LOC_RARM || weapon.getLocation() == Mech.LOC_LARM) {
                int otherArm = weapon.getLocation() == Mech.LOC_RARM ? Mech.LOC_LARM : Mech.LOC_RARM;
                // check previous attacks for weapons fire from the other arm
                if (isFiringFromArmAlready(game, weaponId, attacker, otherArm)) {
          return new ToHitData(ToHitData.IMPOSSIBLE, "Prone and firing from other front leg already.");
                }
            }
            // can't fire rear leg weapons
            if (weapon.getLocation() == Mech.LOC_LLEG || weapon.getLocation() == Mech.LOC_RLEG) {
                return new ToHitData(ToHitData.IMPOSSIBLE, "Can't fire rear leg-mounted weapons while prone with destroyed legs.");
            }
            mods.addModifier(2, "attacker prone");
        } else {
            int l3ProneFiringArm = Entity.LOC_NONE;
            
            if (attacker.isLocationDestroyed(Mech.LOC_RARM) || attacker.isLocationDestroyed(Mech.LOC_LARM)) {
              if ( game.getOptions().booleanOption("maxtech_prone_fire") ) {
                //Can fire with only one arm
                if (attacker.isLocationDestroyed(Mech.LOC_RARM) && attacker.isLocationDestroyed(Mech.LOC_LARM)) {
                    return new ToHitData(ToHitData.IMPOSSIBLE, "Prone with both arms destroyed.");
                }
                
                l3ProneFiringArm = attacker.isLocationDestroyed(Mech.LOC_RARM) ? Mech.LOC_LARM : Mech.LOC_RARM;
              } else {
                // must have an arm intact
                return new ToHitData(ToHitData.IMPOSSIBLE, "Prone with one or both arms destroyed.");
              }
            }

            // arm-mounted weapons have addidional trouble
            if (weapon.getLocation() == Mech.LOC_RARM || weapon.getLocation() == Mech.LOC_LARM) {
              if ( l3ProneFiringArm == weapon.getLocation() ) {
                return new ToHitData(ToHitData.IMPOSSIBLE, "Prone and propping up with this arm.");
              }
              
                int otherArm = weapon.getLocation() == Mech.LOC_RARM ? Mech.LOC_LARM : Mech.LOC_RARM;
        // check previous attacks for weapons fire from the other arm
        if (isFiringFromArmAlready(game, weaponId, attacker, otherArm)) {
          return new ToHitData(ToHitData.IMPOSSIBLE, "Prone and firing from other arm already.");
        }
            }
            // can't fire leg weapons
            if (weapon.getLocation() == Mech.LOC_LLEG || weapon.getLocation() == Mech.LOC_RLEG) {
                return new ToHitData(ToHitData.IMPOSSIBLE, "Can't fire leg-mounted weapons while prone.");
            }
            mods.addModifier(2, "attacker prone");
            
            if ( l3ProneFiringArm != Entity.LOC_NONE ) {
              mods.addModifier(1, "attacker propping on single arm");
            }
        }
        return mods;
  }

  /**
   * Checks to see if there is an attack previous to the one with this
   * weapon from the specified arm.
   * @return true if there is a previous attack from this arm
   */
  private static boolean isFiringFromArmAlready(Game game, int weaponId, final Entity attacker, int armLoc) {
    for (Enumeration i = game.getActions(); i.hasMoreElements();) {
        Object o = i.nextElement();
        if (!(o instanceof WeaponAttackAction)) {
            continue;
        }
        WeaponAttackAction prevAttack = (WeaponAttackAction)o;
        // stop when we get to this weaponattack (does this always work?)
        if (prevAttack.getEntityId() == attacker.getId() && prevAttack.getWeaponId() == weaponId) {
            break;
        }
        if (prevAttack.getEntityId() == attacker.getId() && attacker.getEquipment(prevAttack.getWeaponId()).getLocation() == armLoc) {
          return true;
        }
    }
    return false;
  }

  /**
     * Adds any damage modifiers from arm critical hits or sensor damage.
   * @return Any applicable damage modifiers
   */
  private static ToHitData getDamageWeaponMods(Entity attacker, Mounted weapon) {
    ToHitData mods = new ToHitData();
    
    if (attacker.getDestroyedCriticals(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_SHOULDER, weapon.getLocation()) > 0) {
            mods.addModifier(4, "shoulder actuator destroyed");
        } else {
          // no shoulder hits, add other arm hits
          int actuatorHits = 0;
          if (attacker.getDestroyedCriticals(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_UPPER_ARM, weapon.getLocation()) > 0) {
              actuatorHits++;
          }
          if (attacker.getDestroyedCriticals(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_LOWER_ARM, weapon.getLocation()) > 0) {
              actuatorHits++;
          }
          if (actuatorHits > 0) {
        mods.addModifier(actuatorHits, actuatorHits + " destroyed arm actuators");
          }
    }
        
    // sensors critical hit to attacker
    int sensorHits = attacker.getDestroyedCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_SENSORS, Mech.LOC_HEAD);
    if (sensorHits > 0) {
      mods.addModifier(2, "attacker sensors damaged");
    }
    
        return mods;
  }

  /**
     * Determines if the current target is a secondary target, and if so,
     * returns the appropriate modifier.
     * 
     * @return The secondary target modifier.
     * @author Ben
     */
    private static ToHitData getSecondaryTargetMod(Game game, Entity attacker, Targetable target) {
    boolean curInFrontArc = isInArc(attacker.getPosition(), attacker.getSecondaryFacing(), target.getPosition(), ARC_FORWARD);

    int primaryTarget = Entity.NONE;
        for (Enumeration i = game.getActions(); i.hasMoreElements();) {
            Object o = i.nextElement();
            if (!(o instanceof WeaponAttackAction)) {
                continue;
            }
            WeaponAttackAction prevAttack = (WeaponAttackAction)o;
            if (prevAttack.getEntityId() == attacker.getId()) {
                // first front arc target is our primary.
                // if first target is non-front, and either a later target or
                // the current one is in front, use that instead.
                Targetable pte = game.getTarget(prevAttack.getTargetType(), prevAttack.getTargetId());
                if (isInArc(attacker.getPosition(), attacker.getSecondaryFacing(), pte.getPosition(), ARC_FORWARD)) {
                    primaryTarget = prevAttack.getTargetId();
                    break;
                } else if (primaryTarget == Entity.NONE && !curInFrontArc) {
                    primaryTarget = prevAttack.getTargetId();
                }
            }
        }
        
        if (primaryTarget == Entity.NONE || primaryTarget == target.getTargetId()) {
          // current target is primary target
          return null; // no modifier
        }
        
        // current target is secondary

        // Infantry can't attack secondary targets (BMRr, pg. 32).
        if (attacker instanceof Infantry) {
          return new ToHitData(ToHitData.IMPOSSIBLE, "Can't have multiple targets.");
        }

        if (curInFrontArc) {
      return new ToHitData(1, "secondary target modifier");
        } else {
      return new ToHitData(2, "secondary target modifier");
        }
  }

  /**
     * Returns ToHitData indicating the modifiers to fire for the specified
     * LOS effects data.
     */
    public static ToHitData losModifiers(LosEffects los) {
        ToHitData modifiers = new ToHitData();
        if (los.blocked) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "LOS blocked by terrain.");
        }
        
        if (los.lightWoods + (los.heavyWoods * 2) > 2) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "LOS blocked by woods.");
        }
        
        if (los.smoke > 1) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "LOS blocked by smoke.");
        }
        
        if (los.smoke == 1) {
            if (los.lightWoods + los.heavyWoods > 0) {
                return new ToHitData(ToHitData.IMPOSSIBLE, "LOS blocked by smoke and woods.");
            } else {
                modifiers.addModifier(2, "intervening smoke");
            }
        }
        
        if (los.lightWoods > 0) {
            modifiers.addModifier(los.lightWoods, los.lightWoods + " intervening light woods");
        }
        
        if (los.heavyWoods > 0) {
            modifiers.addModifier(los.heavyWoods * 2, los.heavyWoods + " intervening heavy woods");
        }
        
        if (los.targetCover) {
            modifiers.addModifier(3, "target has partial cover");
        }
        
        return modifiers;
    }
    
    /**
     * Returns a LosEffects object representing the LOS effects of interveing
     * terrain between the attacker and target.
     *
     * Checks to see if the attacker and target are at an angle where the LOS
     * line will pass between two hexes.  If so, calls losDivided, otherwise 
     * calls losStraight.
     */
    public static LosEffects calculateLos(Game game, int attackerId, Targetable target) {
        final Entity ae = game.getEntity(attackerId);
        
        // good time to ensure hex cache
        IdealHex.ensureCacheSize(game.board.width + 1, game.board.height + 1);
        
        // LOS fails if one of the entities is not deployed.
        if (null == ae.getPosition() || null == target.getPosition()) {
            LosEffects los = new LosEffects();
            los.blocked = true; // TODO: come up with a better "impossible"
            return los;
        }
        
        Hex attHex = game.board.getHex(ae.getPosition());
        Hex targetHex = game.board.getHex(target.getPosition());
        
        int attEl = ae.absHeight();
        int targEl;
        if ( target.getTargetType() == Targetable.TYPE_ENTITY ||
             target.getTargetType() == Targetable.TYPE_BUILDING ||
             target.getTargetType() == Targetable.TYPE_BLDG_IGNITE ) {
            targEl = target.absHeight();
        } else {
            targEl = game.board.getHex(target.getPosition()).floor();
        }
        
        boolean attUnderWater = attHex.contains(Terrain.WATER) && 
        						attHex.depth() > 0 && 
        						attEl < attHex.surface();
        boolean attInWater = attHex.contains(Terrain.WATER) &&
        						attHex.depth() > 0 && 
        						attEl == attHex.surface();
        boolean attOnLand = !(attUnderWater || attInWater);
        
        boolean targetUnderWater = targetHex.contains(Terrain.WATER) && 
        						targetHex.depth() > 0 && 
        						targEl < targetHex.surface();
        boolean targetInWater = targetHex.contains(Terrain.WATER) &&
        						targetHex.depth() > 0 && 
        						targEl == targetHex.surface();
        boolean targetOnLand = !(targetUnderWater || targetInWater);
        
        if (attOnLand && targetUnderWater ||
        	attUnderWater && targetOnLand) {
            LosEffects los = new LosEffects();
            los.blocked = true;
            return los;        	
        }
        
        boolean underWaterCombat = targetUnderWater || attUnderWater;
        
        double degree = ae.getPosition().degree(target.getPosition());
        if (degree % 60 == 30) {
            return losDivided(game, attackerId, target, underWaterCombat);
        } else {
            return losStraight(game, attackerId, target, underWaterCombat);
        }
    }
    
    /**
     * Returns LosEffects for a line that never passes exactly between two 
     * hexes.  Since intervening() returns all the coordinates, we just
     * add the effects of all those hexes.
     */
    public static LosEffects losStraight(Game game, int attackerId, Targetable target,
    									boolean underWaterCombat) {
        final Entity ae = game.getEntity(attackerId);
        Coords[] in = intervening(ae.getPosition(), target.getPosition());
        LosEffects los = new LosEffects();
        boolean targetInBuilding = false;
        if ( target instanceof Entity ) {
            targetInBuilding = isInBuilding(game, (Entity) target);
        }

        // If the target and attacker are both in a
        // building, set that as the first LOS effect.
        if ( targetInBuilding && isInBuilding( game, ae ) ) {
            los.setThruBldg( game.board.getBuildingAt( in[0] ) );
        }

        for (int i = 0; i < in.length; i++) {
            los.add( losForCoords(game, attackerId, target,
                                  in[i], los.getThruBldg(), underWaterCombat) );
        }

        // Infantry inside a building can only be
        // targeted by units in the same building.
        if ( target instanceof Infantry &&
             targetInBuilding &&
             null == los.getThruBldg() ) {
            los.blocked = true;
        }


        // If a target Entity is at a different elevation as its
        // attacker, and if the attack is through a building, the
        // target has cover.
        if ( null != los.getThruBldg() &&
             ae.getElevation() != target.getElevation() ) {
            los.setTargetCover( true );
        }

        return los;
    }
    
    /**
     * Returns LosEffects for a line that passes between two hexes at least
     * once.  The rules say that this situation is resolved in favor of the
     * defender.
     *
     * The intervening() function returns both hexes in these circumstances,
     * and, when they are in line order, it's not hard to figure out which hexes 
     * are split and which are not.
     *
     * The line always looks like:
     *       ___     ___
     *   ___/ 1 \___/...\___
     *  / 0 \___/ 3 \___/etc\
     *  \___/ 2 \___/...\___/
     *      \___/   \___/
     *
     * We go thru and figure out the modifiers for the non-split hexes first.
     * Then we go to each of the two split hexes and determine which gives us
     * the bigger modifier.  We use the bigger modifier.
     *
     * This is not perfect as it takes partial cover as soon as it can, when
     * perhaps later might be better.
     * Also, it doesn't account for the fact that attacker partial cover blocks
     * leg weapons, as we want to return the same sequence regardless of
     * what weapon is attacking.
     */
    public static LosEffects losDivided(Game game, int attackerId, Targetable target,
    									boolean underWaterCombat) {
        final Entity ae = game.getEntity(attackerId);
        Coords[] in = intervening(ae.getPosition(), target.getPosition());
        LosEffects los = new LosEffects();
        boolean targetInBuilding = false;
        if ( target instanceof Entity ) {
            targetInBuilding = isInBuilding(game, (Entity) target);
        }
        final boolean isElevDiff = 
            ( ae.getElevation() != target.getElevation() );

        // If the target and attacker are both in a
        // building, set that as the first LOS effect.
        if ( targetInBuilding && isInBuilding( game, ae ) ) {
            los.setThruBldg( game.board.getBuildingAt( in[0] ) );
        }

        // add non-divided line segments
        for (int i = 3; i < in.length - 2; i += 3) {
            los.add( losForCoords(game, attackerId, target,
                                  in[i], los.getThruBldg(), underWaterCombat) );
        }
        
        // if blocked already, return that
        if (losModifiers(los).getValue() == ToHitData.IMPOSSIBLE) {
            return los;
        }
        
        // go through divided line segments
        for (int i = 1; i < in.length - 2; i += 3) {
            // get effects of each side
            LosEffects left = losForCoords( game, attackerId, target, 
                                            in[i], los.getThruBldg(), underWaterCombat);
            LosEffects right = losForCoords( game, attackerId, target,
                                             in[i+1], los.getThruBldg(), underWaterCombat);

            // If a target Entity is at a different elevation as its
            // attacker, and if the attack is through a building, the
            // target has cover.
            if ( targetInBuilding && isElevDiff ) {
                 if ( null != left.getThruBldg() ) {
                     left.setTargetCover(true);
                 }
                 if ( null != right.getThruBldg() ) {
                     right.setTargetCover(true);
                 }
            }

            // Include all previous LOS effects.
            left.add(los);
            right.add(los);

            // Infantry inside a building can only be
            // targeted by units in the same building.
            if ( target instanceof Infantry &&
                 targetInBuilding ) {
                if ( null == left.getThruBldg() ) {
                    left.blocked = true;
                }
                else if ( null == right.getThruBldg() ) {
                    right.blocked = true;
                }
            }

            // which is better?
            int lVal = losModifiers(left).getValue();
            int rVal = losModifiers(right).getValue();
            if (lVal > rVal || (lVal == rVal && left.isAttackerCover())) {
                los = left;
            } else {
                los = right;
            }
        }
        
        return los;
    }
    
    /**
     * Returns a LosEffects object representing the LOS effects of anything at
     * the specified coordinate.  
     */
    private static LosEffects losForCoords(Game game, int attackerId,
                                           Targetable target, Coords coords,
                                           Building thruBldg, boolean underWaterCombat) {
        LosEffects los = new LosEffects();        
        // ignore hexes not on board
        if (!game.board.contains(coords)) {
            return los;
        }

        // Is there a building in this hex?
        Building bldg = game.board.getBuildingAt(coords);

        // We're only tracing thru a single building if there
        // is a building in this hex, and if it isn't the same
        // building that we'be been tracing LOS thru.
        if ( bldg != null && bldg.equals(thruBldg) ) {
            los.setThruBldg( thruBldg );
        }

        final Entity ae = game.getEntity(attackerId);
        // ignore hexes the attacker or target are in
        if ( coords.equals(ae.getPosition()) ||
             coords.equals(target.getPosition()) ) {
            return los;
        }
        int attEl = ae.absHeight();
        int targEl;
        if ( target.getTargetType() == Targetable.TYPE_ENTITY ||
             target.getTargetType() == Targetable.TYPE_BUILDING ||
             target.getTargetType() == Targetable.TYPE_BLDG_IGNITE ) {
            targEl = target.absHeight();
        } else {
            targEl = game.board.getHex(target.getPosition()).floor();
        }
        Hex hex = game.board.getHex(coords);
        int hexEl = underWaterCombat ? hex.floor() : hex.surface();

        // Handle building elevation.
        // Attacks thru a building are not blocked by that building.
        // ASSUMPTION: bridges don't block LOS.
        int bldgEl = 0;
        if ( null == los.getThruBldg() &&
             hex.contains( Terrain.BLDG_ELEV ) ) {
            bldgEl = hex.levelOf( Terrain.BLDG_ELEV );
        }

        // TODO: Identify when LOS travels *above* a building's hex.
        //       Alternatively, force all building hexes to be same height.

        // check for block by terrain
        if ((hexEl + bldgEl > attEl && hexEl + bldgEl > targEl)
        || (hexEl + bldgEl > attEl && ae.getPosition().distance(coords) == 1)
        || (hexEl + bldgEl > targEl && target.getPosition().distance(coords) == 1)) {
            los.blocked = true;
        }

        // check for woods or smoke only if not under water
        if (!underWaterCombat) {
	        if ((hexEl + 2 > attEl && hexEl + 2 > targEl)
	        || (hexEl + 2 > attEl && ae.getPosition().distance(coords) == 1)
	        || (hexEl + 2 > targEl && target.getPosition().distance(coords) == 1)) {
	            // smoke overrides any woods in the hex
	            if (hex.contains(Terrain.SMOKE)) {
	                los.smoke++;
	            } else if (hex.levelOf(Terrain.WOODS) == 1) {
	                los.lightWoods++;
	            } else if (hex.levelOf(Terrain.WOODS) > 1) {
	                los.heavyWoods++;
	            }
	        }
	    }
        
        // check for target partial cover
        if ( target.getPosition().distance(coords) == 1 &&
             hexEl + bldgEl == targEl &&
             attEl <= targEl && target.getHeight() > 0) {
            los.targetCover = true;
        }

        // check for attacker partial cover
        if (ae.getPosition().distance(coords) == 1 &&
            hexEl + bldgEl == attEl &&
            attEl >= targEl && ae.height() > 0) {
            los.attackerCover = true;
        }
        
        return los;
    }

    /**
     * Returns a LosEffects object representing the LOS effects of two
     * specified coordinates.
     * <P>
     * Assumes entities at ground level.
     *
     * @see #calculateLos(Game, int, Targetable)
     */
  public static LosEffects calculateLos(Game game, Coords c1, Coords c2, boolean mechInFirst, boolean mechInSecond) {
        // good time to ensure hex cache
        IdealHex.ensureCacheSize(game.board.width + 1, game.board.height + 1);
         
        double degree = c1.degree(c2);
        if (degree % 60 == 30) {
            return losDivided(game, c1, c2, mechInFirst, mechInSecond);
        } else {
            return losStraight(game, c1, c2, mechInFirst, mechInSecond);
        }
    }

    /**
     * Returns LosEffects for a line that never passes exactly between two 
     * specified coordinates.  Since intervening() returns all the coordinates,
     * we just add the effects of all those hexes.
     * <P>
     * Assumes entities at ground level.
     *
     * @see #losStraight(Game, int, Targetable)
     */
    public static LosEffects losStraight(Game game, Coords c1, Coords c2, boolean mechInFirst, boolean mechInSecond) {
        Coords[] in = intervening(c1, c2);
        LosEffects los = new LosEffects();
        boolean targetInBuilding = false;
 
        for (int i = 0; i < in.length; i++) {
            los.add(losForCoords(game, c1, c2, in[i], mechInFirst, mechInSecond));
        }
 
        return los;
    }

    /**
     * Returns LosEffects for a line that passes between two specified
     * coordinates at least once.  The rules say that this situation is
     * resolved in favor of the defender.
     *
     * The intervening() function returns both hexes in these circumstances,
     * and, when they are in line order, it's not hard to figure out which
     * hexes are split and which are not.
     * <P>
     * Assumes entities at ground level.
     *
     * @see #losDivided(Game, int, Targetable)
     */
    public static LosEffects losDivided(Game game, Coords c1, Coords c2, boolean mechInFirst, boolean mechInSecond) {
        Coords[] in = intervening(c1, c2);
        LosEffects los = new LosEffects();
 
        // add non-divided line segments
        for (int i = 3; i < in.length - 2; i += 3) {
            los.add( losForCoords(game, c1, c2, in[i], mechInFirst, mechInSecond));
        }
         
        // if blocked already, return that
        if (losModifiers(los).getValue() == ToHitData.IMPOSSIBLE) {
            return los;
        }
         
        // go through divided line segments
        for (int i = 1; i < in.length - 2; i += 3) {
            // get effects of each side
            LosEffects left = losForCoords(game, c1, c2, in[i], mechInFirst, mechInSecond);
            LosEffects right = losForCoords(game, c1, c2, in [i + 1], mechInFirst, mechInSecond);
 
            // Include all previous LOS effects.
            left.add(los);
            right.add(los);
 
            // which is better?
            int lVal = losModifiers(left).getValue();
            int rVal = losModifiers(right).getValue();
            if (lVal > rVal || (lVal == rVal && left.isAttackerCover())) {
                los = left;
            } else {
                los = right;
            }
        }
         
        return los;
    }

    /**
     * Returns a LosEffects object representing the LOS effects of anything at
     * the specified coordinates.  
     *
     * @see #losForCoords(Game, int, Targetable, Coords, Building)
     */
    private static LosEffects losForCoords(Game game, Coords c1, Coords c2, Coords c3, boolean mechInFirst, boolean mechInSecond){
        LosEffects los = new LosEffects();        
        // ignore hexes not on board
        if (!game.board.contains(c3)) {
            return los;
        }

        // ignore hexes the attacker or target are in
        if ( c3.equals(c1) || c3.equals(c2) ) {
            return los;
        }
        int attEl = game.board.getHex(c1).floor();
        if (mechInFirst) {
          attEl++;
        }
        int targEl = game.board.getHex(c2).floor();
        if (mechInSecond) {
          targEl++;
        }
 
        Hex hex = game.board.getHex(c3);
        int hexEl = hex.surface();
 
        // Handle building elevation.
        // Attacks thru a building are not blocked by that building.
        // ASSUMPTION: bridges don't block LOS.
        int bldgEl = 0;
        if ( null == los.getThruBldg() &&
             hex.contains( Terrain.BLDG_ELEV ) ) {
            bldgEl = hex.levelOf( Terrain.BLDG_ELEV );
        }
 
        // TODO: Identify when LOS travels *above* a building's hex.
        //       Alternatively, force all building hexes to be same height.
 
        // check for block by terrain
        if ((hexEl + bldgEl > attEl && hexEl + bldgEl > targEl)
            || (hexEl + bldgEl > attEl && c1.distance(c3) == 1)
            || (hexEl + bldgEl > targEl && c2.distance(c3) == 1)) {
            los.blocked = true;
        }
 
        // check for woods or smoke
        if ((hexEl + 2 > attEl && hexEl + 2 > targEl)
            || (hexEl + 2 > attEl && c1.distance(c3) == 1)
            || (hexEl + 2 > targEl && c2.distance(c3) == 1)) {
            // smoke overrides any woods in the hex
            if (hex.contains(Terrain.SMOKE)) {
                los.smoke++;
            } else if (hex.levelOf(Terrain.WOODS) == 1) {
                los.lightWoods++;
            } else if (hex.levelOf(Terrain.WOODS) > 1) {
                los.heavyWoods++;
            }
        }
         
        // check for target partial cover
        if ( c2.distance(c3) == 1 &&
             hexEl + bldgEl == targEl &&
             attEl <= targEl &&
             mechInSecond) {
            los.targetCover = true;
        }
 
        // check for attacker partial cover
        if (c1.distance(c3) == 1 &&
            hexEl + bldgEl == attEl &&
            attEl >= targEl &&
            mechInFirst) {
            los.attackerCover = true;
        }
         
        return los;
    }
    

    public static ToHitData toHitPunch(Game game, PunchAttackAction paa) {
        return toHitPunch(game, paa.getEntityId(),
                          game.getTarget(paa.getTargetType(), paa.getTargetId() ),
                          paa.getArm());
    }
    
    
    
    /**
     * To-hit number for the specified arm to punch
     * <p/>
     * This version is kept for backwards compatability.
     */
    public static ToHitData toHitPunch(Game game, int attackerId, 
                                       int targetId, int arm) {
        return toHitPunch( game, attackerId, game.getEntity(targetId), arm );
    }
    
    /**
     * To-hit number for the specified arm to punch
     */
    public static ToHitData toHitPunch(Game game, int attackerId,
                                       Targetable target, int arm) {
        final Entity ae = game.getEntity(attackerId);
        int targetId = Entity.NONE;
        Entity te = null;
        if ( target.getTargetType() == Targetable.TYPE_ENTITY ) {
            te = (Entity) target;
            targetId = target.getTargetId();
        }
        final int attackerHeight = ae.absHeight();
        final int targetHeight = target.absHeight();
        final int targetElevation = target.getElevation();
        final int armLoc = (arm == PunchAttackAction.RIGHT)
                           ? Mech.LOC_RARM : Mech.LOC_LARM;
        final int armArc = (arm == PunchAttackAction.RIGHT)
                           ? Compute.ARC_RIGHTARM : Compute.ARC_LEFTARM;
        final boolean targetInBuilding = isInBuilding( game, te );
        Building bldg = null;
        if ( targetInBuilding ) {
            bldg = game.board.getBuildingAt( te.getPosition() );
        }
        ToHitData toHit;

        // arguments legal?
        if (arm != PunchAttackAction.RIGHT && arm != PunchAttackAction.LEFT) {
            throw new IllegalArgumentException("Arm must be LEFT or RIGHT");
        }
        if (ae == null || target == null) {
            throw new IllegalArgumentException("Attacker or target not valid");
        }

        // can't target yourself
        if (ae.equals(te)) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "You can't target yourself");
        }
        
  // non-mechs can't punch
  if (!(ae instanceof Mech)) {
      return new ToHitData(ToHitData.IMPOSSIBLE, "Non-mechs can't punch");
  }

        // Can't target a transported entity.
        if ( te != null && Entity.NONE != te.getTransportId() ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target is a passenger.");
        }

        // Can't target a entity conducting a swarm attack.
        if ( te != null && Entity.NONE != te.getSwarmTargetId() ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target is swarming a Mek.");
        }
        
        // can't make physical attacks while spotting
        if (ae.isSpotting()) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Attacker is spotting this turn");
        }

        //Quads can't punch
        if ( ae.entityIsQuad() ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Attacker is a quad");
        }

        //Can't punch with flipped arms
        if (ae.getArmsFlipped()) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Arms are flipped to the rear. Can not punch.");
        }

        // check if arm is present
        if (ae.isLocationDestroyed(armLoc)) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Arm missing");
        }
        
        // check if shoulder is functional
        if (!ae.hasWorkingSystem(Mech.ACTUATOR_SHOULDER, armLoc)) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Shoulder destroyed");
        }  
        
        // check if attacker has fired arm-mounted weapons
        if (ae.weaponFiredFrom(armLoc)) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Weapons fired from arm this turn");
        }
        
        // check range
        if (ae.getPosition().distance(target.getPosition()) > 1 ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target not in range");
        }
        
        // check elevation
        if (attackerHeight < targetElevation || attackerHeight > targetHeight) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target elevation not in range");
        }
        
        // can't physically attack mechs making dfa attacks
        if ( te != null && te.isMakingDfa() ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target is making a DFA attack");
        }

        // Can't target units in buildings (from the outside).
        if ( targetInBuilding ) {
            if ( !isInBuilding(game, ae) ) {
                return new ToHitData(ToHitData.IMPOSSIBLE, "Target is inside building" );
            } 
            else if ( !game.board.getBuildingAt( ae.getPosition() )
                      .equals( bldg ) ) {
                return new ToHitData(ToHitData.IMPOSSIBLE, "Target is inside differnt building" );
            }
        }

    
        //Set the base BTH
          int base = 4;
          
          if ( game.getOptions().booleanOption("maxtech_physical_BTH") ) {
            base = ae.getCrew().getPiloting() - 1;
          }
          
          toHit = new ToHitData(base, "base");

        // Prone Meks can only punch vehicles in the same hex.
        if (ae.isProne()) {
            // The Mek must have both arms, the target must
            // be a tank, and both must be in the same hex.
            if ( !ae.isLocationDestroyed(Mech.LOC_RARM) &&
                 !ae.isLocationDestroyed(Mech.LOC_LARM) &&
                 te instanceof Tank &&
                 ae.getPosition().distance( target.getPosition() ) == 0 ) {
                toHit.addModifier( 2, "attacker is prone" );
            } else {
                return new ToHitData(ToHitData.IMPOSSIBLE,"Attacker is prone");
            }
        }

        // Check facing if the Mek is not prone.
        else if ( !isInArc(ae.getPosition(), ae.getSecondaryFacing(), 
                           target.getPosition(), armArc) ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target not in arc");
        }

        // Attacks against adjacent buildings automatically hit.
        if ( target.getTargetType() == Targetable.TYPE_BUILDING ) {
            return new ToHitData( ToHitData.AUTOMATIC_SUCCESS, 
                                  "Targeting adjacent building." );
        }

        // Can't target woods or ignite a building with a physical.
        if ( target.getTargetType() == Targetable.TYPE_BLDG_IGNITE ||
             target.getTargetType() == Targetable.TYPE_HEX_CLEAR ||
             target.getTargetType() == Targetable.TYPE_HEX_IGNITE ) {
            return new ToHitData( ToHitData.IMPOSSIBLE, "Invalid attack");
        }

        // Battle Armor targets are hard for Meks and Tanks to hit.
        if ( te instanceof BattleArmor ) {
            toHit.addModifier( 1, "battle armor target" );
        }
        
        
        // attacker movement
        toHit.append(getAttackerMovementModifier(game, attackerId));
        
        // target movement
        toHit.append(getTargetMovementModifier(game, targetId));
        
        // attacker terrain
        toHit.append(getAttackerTerrainModifier(game, attackerId));
        
        // target terrain
        toHit.append(getTargetTerrainModifier(game, te));
        
        // damaged or missing actuators
        if (!ae.hasWorkingSystem(Mech.ACTUATOR_UPPER_ARM, armLoc)) {
            toHit.addModifier(2, "Upper arm actuator destroyed");
        }
        if (!ae.hasWorkingSystem(Mech.ACTUATOR_LOWER_ARM, armLoc)) {
            toHit.addModifier(2, "Lower arm actuator missing or destroyed");
        }
        if (!ae.hasWorkingSystem(Mech.ACTUATOR_HAND, armLoc)) {
            toHit.addModifier(1, "Hand actuator missing or destroyed");
        }

        // target prone
        if (te.isProne()) {
            toHit.addModifier(-2, "target prone and adjacent");
        }
        
        // water partial cover?
        Hex targHex = game.board.getHex(te.getPosition());
        if (te.height() > 0 && targHex.levelOf(Terrain.WATER) == te.height()) {
            toHit.addModifier(3, "target has partial cover");
        }

    // target immobile
    toHit.append(getImmobileMod(te));
        
        modifyPhysicalBTHForAdvantages(ae, te, toHit);
        
        // elevation
        if (attackerHeight == targetElevation) {
            if (te.height() == 0) {
                toHit.setHitTable(ToHitData.HIT_NORMAL);
            } else {
                toHit.setHitTable(ToHitData.HIT_KICK);
            }
        } else {
            toHit.setHitTable(ToHitData.HIT_PUNCH);
        }
        
        // factor in target side
        toHit.setSideTable(targetSideTable(ae,te));

        // done!
        return toHit;
    }
    
    /**
     * Damage that the specified mech does with a punch.
     */
    public static int getPunchDamageFor(Entity entity, int arm) {
        final int armLoc = (arm == PunchAttackAction.RIGHT)
                           ? Mech.LOC_RARM : Mech.LOC_LARM;
        int damage = (int)Math.ceil(entity.getWeight() / 10.0);
        float multiplier = 1.0f;
        
        if (!entity.hasWorkingSystem(Mech.ACTUATOR_UPPER_ARM, armLoc)) {
            multiplier /= 2.0f;
        }
        if (!entity.hasWorkingSystem(Mech.ACTUATOR_LOWER_ARM, armLoc)) {
            multiplier /= 2.0f;
        }
        if (!entity.hasWorkingSystem(Mech.ACTUATOR_SHOULDER, armLoc)) {
            damage = 0;
        }
        if (entity.heat >= 9 && ((Mech)entity).hasTSM()) {
            multiplier *= 2.0f;
        }
        if (entity.getLocationStatus(armLoc) == Entity.LOC_WET) {
            multiplier /= 2.0f;
        }
        return (int)Math.floor(damage * multiplier) + modifyPhysicalDamagaForMeleeSpecialist(entity);
    }
    
    public static ToHitData toHitKick(Game game, KickAttackAction kaa) {
        return toHitKick(game, kaa.getEntityId(), 
                         game.getTarget(kaa.getTargetType(), kaa.getTargetId()),
                         kaa.getLeg());
    }
    
    /**
     * To-hit number for the specified leg to kick
     * <p/>
     * This version is kept for backwards compatability.
     */
    public static ToHitData toHitKick(Game game, int attackerId, 
                                       int targetId, int leg) {
        return toHitKick( game, attackerId, game.getEntity(targetId), leg );
    }
    
    /**
     * To-hit number for the specified leg to kick
     */
    public static ToHitData toHitKick(Game game, int attackerId,
                                      Targetable target, int leg) {
        final Entity ae = game.getEntity(attackerId);
        int targetId = Entity.NONE;
        Entity te = null;
        if ( target.getTargetType() == Targetable.TYPE_ENTITY ) {
            te = (Entity) target;
            targetId = target.getTargetId();
        }
        final int attackerElevation = ae.getElevation();
        final int targetHeight = target.absHeight();
        final int targetElevation = target.getElevation();
        final boolean targetInBuilding = isInBuilding( game, te );
        Building bldg = null;
        if ( targetInBuilding ) {
            bldg = game.board.getBuildingAt( te.getPosition() );
        }

        int[] kickLegs = new int[2];
        if ( ae.entityIsQuad() ) {
          kickLegs[0] = Mech.LOC_RARM;
          kickLegs[1] = Mech.LOC_LARM;
        } else {
          kickLegs[0] = Mech.LOC_RLEG;
          kickLegs[1] = Mech.LOC_LLEG;
        }
        final int legLoc = 
            (leg == KickAttackAction.RIGHT) ? kickLegs[0] : kickLegs[1];
        
        ToHitData toHit;

        // arguments legal?
        if (leg != KickAttackAction.RIGHT && leg != KickAttackAction.LEFT) {
            throw new IllegalArgumentException("Leg must be LEFT or RIGHT");
        }
        if (ae == null || target == null) {
            throw new IllegalArgumentException("Attacker or target not valid");
        }

        // can't target yourself
        if (ae.equals(te)) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "You can't target yourself");
        }
        
        // non-mechs can't kick
        if (!(ae instanceof Mech)) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Non-mechs can't kick");
        }
        
        // can't make physical attacks while spotting
        if (ae.isSpotting()) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Attacker is spotting this turn");
        }

        // Can't target a transported entity.
        if ( te != null && Entity.NONE != te.getTransportId() ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target is a passenger.");
        }

        // Can't target a entity conducting a swarm attack.
        if ( te != null && Entity.NONE != te.getSwarmTargetId() ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target is swarming a Mek.");
        }
        
        // check if both legs are present
        if (ae.isLocationDestroyed(kickLegs[0])
            || ae.isLocationDestroyed(kickLegs[1])) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Leg missing");
        }
        
        // check if both hips are operational
        if (!ae.hasWorkingSystem(Mech.ACTUATOR_HIP, kickLegs[0])
            || !ae.hasWorkingSystem(Mech.ACTUATOR_HIP, kickLegs[1])) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Hip destroyed");
        }  
        
        // check if attacker has fired leg-mounted weapons
        for (Enumeration i = ae.getWeapons(); i.hasMoreElements();) {
            Mounted mounted = (Mounted)i.nextElement();
            if (mounted.isUsedThisRound() && mounted.getLocation() == legLoc) {
                return new ToHitData(ToHitData.IMPOSSIBLE, "Weapons fired from leg this turn");
            }
        }
        
        // check range
        final int range = ae.getPosition().distance(target.getPosition());
        if ( range > 1 ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target not in range");
        }
        
        // check elevation 
        if (attackerElevation < targetElevation || attackerElevation > targetHeight) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target elevation not in range");
        }
        
        // can't physically attack mechs making dfa attacks
        if ( te != null && te.isMakingDfa() ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target is making a DFA attack");
        }

        // check facing
  // Don't check arc for stomping infantry or tanks.
        if (0 != range &&
      !isInArc(ae.getPosition(), ae.getFacing(), 
                     target.getPosition(), Compute.ARC_FORWARD)) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target not in arc");
        }
        
        // can't kick while prone
        if (ae.isProne()) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Attacker is prone");
        }
        
        // Can't target units in buildings (from the outside).
        if ( 0 != range && targetInBuilding ) {
            if ( !isInBuilding(game, ae) ) {
                return new ToHitData(ToHitData.IMPOSSIBLE, "Target is inside building" );
            } 
            else if ( !game.board.getBuildingAt( ae.getPosition() )
                      .equals( bldg ) ) {
                return new ToHitData(ToHitData.IMPOSSIBLE, "Target is inside differnt building" );
            }
        }

        // Attacks against adjacent buildings automatically hit.
        if ( target.getTargetType() == Targetable.TYPE_BUILDING ) {
            return new ToHitData( ToHitData.AUTOMATIC_SUCCESS, 
                                  "Targeting adjacent building." );
        }

        // Can't target woods or ignite a building with a physical.
        if ( target.getTargetType() == Targetable.TYPE_BLDG_IGNITE ||
             target.getTargetType() == Targetable.TYPE_HEX_CLEAR ||
             target.getTargetType() == Targetable.TYPE_HEX_IGNITE ) {
            return new ToHitData( ToHitData.IMPOSSIBLE, "Invalid attack");
        }

        //Set the base BTH
          int base = 3;
          
          if ( game.getOptions().booleanOption("maxtech_physical_BTH") ) {
            base = ae.getCrew().getPiloting() - 2;
          }
          
          toHit = new ToHitData(base, "base");

  // BMR(r), page 33. +3 modifier for kicking infantry.
  if ( te instanceof Infantry ) {
      toHit.addModifier( 3, "Stomping Infantry" );
  }

        // Battle Armor targets are hard for Meks and Tanks to hit.
        if ( te instanceof BattleArmor ) {
            toHit.addModifier( 1, "battle armor target" );
        }
        
        // attacker movement
        toHit.append(getAttackerMovementModifier(game, attackerId));
        
        // target movement
        toHit.append(getTargetMovementModifier(game, targetId));
        
        // attacker terrain
        toHit.append(getAttackerTerrainModifier(game, attackerId));
        
        // target terrain
        toHit.append(getTargetTerrainModifier(game, te));
        
        // damaged or missing actuators
        if (!ae.hasWorkingSystem(Mech.ACTUATOR_UPPER_LEG, legLoc)) {
            toHit.addModifier(2, "Upper leg actuator destroyed");
        }
        if (!ae.hasWorkingSystem(Mech.ACTUATOR_LOWER_LEG, legLoc)) {
            toHit.addModifier(2, "Lower leg actuator destroyed");
        }
        if (!ae.hasWorkingSystem(Mech.ACTUATOR_FOOT, legLoc)) {
            toHit.addModifier(1, "Foot actuator destroyed");
        }

        // target prone
        if (te.isProne()) {
            toHit.addModifier(-2, "target prone and adjacent");
        }
        
        // water partial cover?
        Hex targHex = game.board.getHex(te.getPosition());
        if (te.height() > 0 && targHex.levelOf(Terrain.WATER) == te.height()) {
            toHit.addModifier(3, "target has partial cover");
        }

    // target immobile
    toHit.append(getImmobileMod(te));
        
        modifyPhysicalBTHForAdvantages(ae, te, toHit);
        
        // elevation
        if (attackerElevation < targetHeight) {
            toHit.setHitTable(ToHitData.HIT_KICK);
        } else if (te.height() > 0) {
            toHit.setHitTable(ToHitData.HIT_PUNCH);
        } else {
            toHit.setHitTable(ToHitData.HIT_NORMAL);
        }
        
        // factor in target side
        toHit.setSideTable(targetSideTable(ae,te));

        // BMRr pg. 42, "The side on which a vehicle takes damage is determined
        // randomly if the BattleMech is attacking from the same hex."
        if ( 0 == range && te instanceof Tank ) {
            toHit.setSideTable( ToHitData.SIDE_RANDOM );
        }

        // done!
        return toHit;
    }
    
    
    /**
     * Damage that the specified mech does with a kick
     */
    public static int getKickDamageFor(Entity entity, int leg) {
        int[] kickLegs = new int[2];
        if ( entity.entityIsQuad() ) {
          kickLegs[0] = Mech.LOC_RARM;
          kickLegs[1] = Mech.LOC_LARM;
        } else {
          kickLegs[0] = Mech.LOC_RLEG;
          kickLegs[1] = Mech.LOC_LLEG;
        }

        final int legLoc = (leg == KickAttackAction.RIGHT) ? kickLegs[0] : kickLegs[1];
        int damage = (int)Math.floor(entity.getWeight() / 5.0);
        float multiplier = 1.0f;
        
        if (!entity.hasWorkingSystem(Mech.ACTUATOR_UPPER_LEG, legLoc)) {
            multiplier /= 2.0f;
        }
        if (!entity.hasWorkingSystem(Mech.ACTUATOR_LOWER_LEG, legLoc)) {
            multiplier /= 2.0f;
        }
        if (!entity.hasWorkingSystem(Mech.ACTUATOR_HIP, legLoc)) {
            damage = 0;
        }
        if (entity.heat >= 9 && ((Mech)entity).hasTSM()) {
            multiplier *= 2.0f;
        }
        if (entity.getLocationStatus(legLoc) == Entity.LOC_WET) {
            multiplier /= 2.0f;
        }
        return (int)Math.floor(damage * multiplier) + modifyPhysicalDamagaForMeleeSpecialist(entity);
    }
    
    public static ToHitData toHitClub(Game game, ClubAttackAction caa) {
        return toHitClub(game, caa.getEntityId(), 
                         game.getTarget(caa.getTargetType(), caa.getTargetId()),
                         caa.getClub());
    }

    /**
     * To-hit number for the specified club to hit
     * <p/>
     * This version is kept for backwards compatability.
     */
    public static ToHitData toHitClub(Game game, int attackerId, int targetId, Mounted club) {
        return toHitClub( game, attackerId, game.getEntity(targetId), club );
    }

    /**
     * To-hit number for the specified club to hit
     */
    public static ToHitData toHitClub(Game game, int attackerId, Targetable target, Mounted club) {
        final Entity ae = game.getEntity(attackerId);
        int targetId = Entity.NONE;
        Entity te = null;
        if ( target.getTargetType() == Targetable.TYPE_ENTITY ) {
            te = (Entity) target;
            targetId = target.getTargetId();
        }
        final int attackerElevation = ae.getElevation();
        final int attackerHeight = ae.absHeight();
        final int targetHeight = target.absHeight();
        final int targetElevation = target.getElevation();
        final boolean bothArms = club.getType().hasFlag(MiscType.F_CLUB);
        final boolean targetInBuilding = isInBuilding( game, te );
        Building bldg = null;
        if ( targetInBuilding ) {
            bldg = game.board.getBuildingAt( te.getPosition() );
        }
        ToHitData toHit;

        // arguments legal?
        if (ae == null || target == null) {
            throw new IllegalArgumentException("Attacker or target not valid");
        }

        // can't target yourself
        if (ae.equals(te)) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "You can't target yourself");
        }
        
  // non-mechs can't club
  if (!(ae instanceof Mech)) {
      return new ToHitData(ToHitData.IMPOSSIBLE, "Non-mechs can't club");
  }

        //Quads can't club
        if ( ae.entityIsQuad() ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Attacker is a quad");
        }

        // can't make physical attacks while spotting
        if (ae.isSpotting()) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Attacker is spotting this turn");
        }

        // Can't target a transported entity.
        if ( te != null && Entity.NONE != te.getTransportId() ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target is a passenger.");
        }

        // Can't target a entity conducting a swarm attack.
        if ( te != null && Entity.NONE != te.getSwarmTargetId() ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target is swarming a Mek.");
        }

        // Can't target units in buildings (from the outside).
        // TODO: you can target units from within the *same* building.
        if ( targetInBuilding ) {
            if ( !isInBuilding(game, ae) ) {
                return new ToHitData(ToHitData.IMPOSSIBLE, "Target is inside building" );
            } 
            else if ( !game.board.getBuildingAt( ae.getPosition() )
                      .equals( bldg ) ) {
                return new ToHitData(ToHitData.IMPOSSIBLE, "Target is inside differnt building" );
            }
        }

        if (bothArms) {
            // check if both arms are present
            if (ae.isLocationDestroyed(Mech.LOC_RARM)
                || ae.isLocationDestroyed(Mech.LOC_LARM)) {
                return new ToHitData(ToHitData.IMPOSSIBLE, "Arm missing");
            }
            // check if attacker has fired arm-mounted weapons
            if (ae.weaponFiredFrom(Mech.LOC_RARM) 
            || ae.weaponFiredFrom(Mech.LOC_LARM)) {
                return new ToHitData(ToHitData.IMPOSSIBLE, "Weapons fired from arm this turn");
            }
            // need shoulder and hand actuators
            if (!ae.hasWorkingSystem(Mech.ACTUATOR_SHOULDER, Mech.LOC_RARM)
            || !ae.hasWorkingSystem(Mech.ACTUATOR_SHOULDER, Mech.LOC_LARM)) {
                return new ToHitData(ToHitData.IMPOSSIBLE, "Shoulder actuator destroyed");
            }
            if (!ae.hasWorkingSystem(Mech.ACTUATOR_HAND, Mech.LOC_RARM)
            || !ae.hasWorkingSystem(Mech.ACTUATOR_HAND, Mech.LOC_LARM)) {
                return new ToHitData(ToHitData.IMPOSSIBLE, "Hand actuator destroyed");
            }
        } else {
            // check if arm is present
            if (ae.isLocationDestroyed(club.getLocation())) {
                return new ToHitData(ToHitData.IMPOSSIBLE, "Arm missing");
            }
            // check if attacker has fired arm-mounted weapons
            if (ae.weaponFiredFrom(club.getLocation())) {
                return new ToHitData(ToHitData.IMPOSSIBLE, "Weapons fired from arm this turn");
            }
            // need shoulder and hand actuators
            if (!ae.hasWorkingSystem(Mech.ACTUATOR_SHOULDER, club.getLocation())) {
                return new ToHitData(ToHitData.IMPOSSIBLE, "Shoulder actuator destroyed");
            }
            if (!ae.hasWorkingSystem(Mech.ACTUATOR_HAND, club.getLocation())) {
                return new ToHitData(ToHitData.IMPOSSIBLE, "Hand actuator destroyed");
            }
        }
        
        // club must not be damaged
        if (ae.getDestroyedCriticals(CriticalSlot.TYPE_EQUIPMENT, ae.getEquipmentNum(club), club.getLocation()) > 0) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Club is damaged");
        }
        
        // check range
        if (ae.getPosition().distance(target.getPosition()) > 1 ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target not in range");
        }
        
        // check elevation (target must be within one level)
        if (targetHeight < attackerElevation || targetElevation > attackerHeight) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target elevation not in range");
        }
        
        // can't physically attack mechs making dfa attacks
        if ( te != null && te.isMakingDfa() ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target is making a DFA attack");
        }

        // check facing
        int clubArc = bothArms ? Compute.ARC_FORWARD : (club.getLocation() == Mech.LOC_LARM ? Compute.ARC_LEFTARM : Compute.ARC_RIGHTARM);
        if ( !isInArc( ae.getPosition(), ae.getSecondaryFacing(),
                       target.getPosition(), clubArc ) ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target not in arc");
        }
        
        // can't club while prone
        if (ae.isProne()) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Attacker is prone");
        }

        // Attacks against adjacent buildings automatically hit.
        if ( target.getTargetType() == Targetable.TYPE_BUILDING ) {
            return new ToHitData( ToHitData.AUTOMATIC_SUCCESS, 
                                  "Targeting adjacent building." );
        }

        // Can't target woods or ignite a building with a physical.
        if ( target.getTargetType() == Targetable.TYPE_BLDG_IGNITE ||
             target.getTargetType() == Targetable.TYPE_HEX_CLEAR ||
             target.getTargetType() == Targetable.TYPE_HEX_IGNITE ) {
            return new ToHitData( ToHitData.IMPOSSIBLE, "Invalid attack");
        }

        //Set the base BTH
          int base = 4;
          
          if ( game.getOptions().booleanOption("maxtech_physical_BTH") ) {
            base = ae.getCrew().getPiloting() - 1;
          }
          
          if (club.getType().hasFlag(MiscType.F_SWORD)) {
            base--;
          }

          toHit = new ToHitData(base, "base");

        // Battle Armor targets are hard for Meks and Tanks to hit.
        if ( te instanceof BattleArmor ) {
            toHit.addModifier( 1, "battle armor target" );
        }
        
        // attacker movement
        toHit.append(getAttackerMovementModifier(game, attackerId));
        
        // target movement
        toHit.append(getTargetMovementModifier(game, targetId));
        
        // attacker terrain
        toHit.append(getAttackerTerrainModifier(game, attackerId));
        
        // target terrain
        toHit.append(getTargetTerrainModifier(game, te));
        
        // damaged or missing actuators
        if (bothArms) {
            if (!ae.hasWorkingSystem(Mech.ACTUATOR_UPPER_ARM, Mech.LOC_RARM)) {
                toHit.addModifier(2, "Upper arm actuator destroyed");
            }
            if (!ae.hasWorkingSystem(Mech.ACTUATOR_UPPER_ARM, Mech.LOC_LARM)) {
                toHit.addModifier(2, "Upper arm actuator destroyed");
            }
            if (!ae.hasWorkingSystem(Mech.ACTUATOR_LOWER_ARM, Mech.LOC_RARM)) {
                toHit.addModifier(2, "Lower arm actuator missing or destroyed");
            }
            if (!ae.hasWorkingSystem(Mech.ACTUATOR_LOWER_ARM, Mech.LOC_LARM)) {
                toHit.addModifier(2, "Lower arm actuator missing or destroyed");
            }
        } else {
            if (!ae.hasWorkingSystem(Mech.ACTUATOR_UPPER_ARM, club.getLocation())) {
                toHit.addModifier(2, "Upper arm actuator destroyed");
            }
            if (!ae.hasWorkingSystem(Mech.ACTUATOR_LOWER_ARM, club.getLocation())) {
                toHit.addModifier(2, "Lower arm actuator missing or destroyed");
            }
        }

        // target prone
        if (te.isProne()) {
            toHit.addModifier(-2, "target prone and adjacent");
        }
        
        // water partial cover?
        Hex targHex = game.board.getHex(te.getPosition());
        if (te.height() > 0 && targHex.levelOf(Terrain.WATER) == te.height()) {
            toHit.addModifier(3, "target has partial cover");
        }

    // target immobile
    toHit.append(getImmobileMod(te));
        
        modifyPhysicalBTHForAdvantages(ae, te, toHit);
        
        // elevation
        if (attackerElevation == targetElevation) {
            toHit.setHitTable(ToHitData.HIT_NORMAL);
        } else if (attackerElevation < targetElevation) {
            if (te.height() == 0) {
                toHit.setHitTable(ToHitData.HIT_NORMAL);
            } else {
                toHit.setHitTable(ToHitData.HIT_KICK);
            }
        } else {
            toHit.setHitTable(ToHitData.HIT_PUNCH);
        }
        
        // factor in target side
        toHit.setSideTable(targetSideTable(ae,te));

        // done!
        return toHit;
    }
    
    /**
     * Damage that the specified mech does with a club attack
     */
    public static int getClubDamageFor(Entity entity, Mounted club) {
        
        int nDamage = (int)Math.floor(entity.getWeight() / 5.0);
        if (club.getType().hasFlag(MiscType.F_SWORD)) {
            nDamage = (int)(Math.ceil(entity.getWeight() / 10.0) + 1.0);
        }
        if (entity.heat >= 9 && ((Mech)entity).hasTSM()) {
            nDamage *= 2;
        }
        int clubLocation = club.getLocation();
        // tree clubs don't have a location--use right arm (is this okay?)
        if (clubLocation == Entity.LOC_NONE) {
            clubLocation = Mech.LOC_RARM;
        }
        if (entity.getLocationStatus(clubLocation) == Entity.LOC_WET) {
            nDamage /= 2.0f;
        }

        return nDamage + modifyPhysicalDamagaForMeleeSpecialist(entity);
    }
    
    public static ToHitData toHitPush(Game game, PushAttackAction paa) {
        return toHitPush(game, paa.getEntityId(), 
                         game.getTarget(paa.getTargetType(), paa.getTargetId()));
    }
    
    /**
     * To-hit number for the mech to push another mech
     */
    public static ToHitData toHitPush(Game game, int attackerId, int targetId) {
        return toHitPush( game, attackerId, game.getEntity(targetId) );
    }

    /**
     * To-hit number for the mech to push another mech
     */
    public static ToHitData toHitPush(Game game,
                                      int attackerId,
                                      Targetable target) {
        final Entity ae = game.getEntity(attackerId);
        int targetId = Entity.NONE;
        Entity te = null;
        if ( target.getTargetType() == Targetable.TYPE_ENTITY ) {
            te = (Entity) target;
            targetId = target.getTargetId();
        }
        final int attackerElevation = ae.getElevation();
        final int targetElevation = target.getElevation();
        final boolean targetInBuilding = Compute.isInBuilding( game, te );
        Building bldg = null;
        if ( targetInBuilding ) {
            bldg = game.board.getBuildingAt( te.getPosition() );
        }
        ToHitData toHit = null;

        // arguments legal?
        if (ae == null || target == null) {
            throw new IllegalArgumentException("Attacker or target not valid");
        }
        
        // can't target yourself
        if (ae.equals(te)) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "You can't target yourself");
        }
        
  // non-mechs can't push
  if (!(ae instanceof Mech)) {
      return new ToHitData(ToHitData.IMPOSSIBLE, "Non-mechs can't push");
  }

        //Quads can't push
        if ( ae.entityIsQuad() ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Attacker is a quad");
        }
        
        // can't make physical attacks while spotting
        if (ae.isSpotting()) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Attacker is spotting this turn");
        }

        //Can only push mechs
        if ( te !=null && !(te instanceof Mech) ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target is not a mech");
        }

        //Can't push with flipped arms
        if (ae.getArmsFlipped()) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Arms are flipped to the rear. Can not push.");
        }

        // Can't target a transported entity.
        if ( te != null && Entity.NONE != te.getTransportId() ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target is a passenger.");
        }

        // Can't target a entity conducting a swarm attack.
        if ( te != null && Entity.NONE != te.getSwarmTargetId() ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target is swarming a Mek.");
        }

        // check if both arms are present
        if (ae.isLocationDestroyed(Mech.LOC_RARM)
            || ae.isLocationDestroyed(Mech.LOC_LARM)) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Arm missing");
        }
        
        // check if attacker has fired arm-mounted weapons
        if (ae.weaponFiredFrom(Mech.LOC_RARM) 
        || ae.weaponFiredFrom(Mech.LOC_LARM)) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Weapons fired from arm this turn");
        }
        
        // check range
        if (ae.getPosition().distance(target.getPosition()) > 1 ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target not in range");
        }
        
        // target must be at same elevation
        if (attackerElevation != targetElevation) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target not at same elevation");
        }
        
        // can't push mech making non-pushing displacement attack
        if ( te != null && te.hasDisplacementAttack() && !te.isPushing() ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target is making a charge/DFA attack");
        }
        
        // can't push mech pushing another, different mech
        if ( te != null && te.isPushing() &&
             te.getDisplacementAttack().getTargetId() != ae.getId() ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target is pushing another mech");
        }
        
        // can't do anything but counter-push if the target of another attack
        if (ae.isTargetOfDisplacementAttack() && ae.findTargetedDisplacement().getEntityId() != target.getTargetId()) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Attacker is the target of another push/charge/DFA");
        }
        
        // can't attack the target of another displacement attack
        if ( te != null && te.isTargetOfDisplacementAttack() &&
             te.findTargetedDisplacement().getEntityId() != ae.getId() ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target is the target of another push/charge/DFA");
        }
        
        // check facing
        if (!target.getPosition().equals(ae.getPosition().translated(ae.getFacing()))) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target not directly ahead of feet");
        }
        
        // can't push while prone
        if (ae.isProne()) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Attacker is prone");
        }
        
        // can't push prone mechs
        if ( te != null && te.isProne()) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target is prone");
        }

        // Can't target units in buildings (from the outside).
        if ( targetInBuilding ) {
            if ( !isInBuilding(game, ae) ) {
                return new ToHitData(ToHitData.IMPOSSIBLE, "Target is inside building" );
            } 
            else if ( !game.board.getBuildingAt( ae.getPosition() )
                      .equals( bldg ) ) {
                return new ToHitData(ToHitData.IMPOSSIBLE, "Target is inside differnt building" );
            }
        }

        // Attacks against adjacent buildings automatically hit.
        if ( target.getTargetType() == Targetable.TYPE_BUILDING ) {
            return new ToHitData( ToHitData.IMPOSSIBLE, 
                                  "You can not push a building (well, you can, but it won't do anything)." );
        }
        
        // Can't target woods or ignite a building with a physical.
        if ( target.getTargetType() == Targetable.TYPE_BLDG_IGNITE ||
             target.getTargetType() == Targetable.TYPE_HEX_CLEAR ||
             target.getTargetType() == Targetable.TYPE_HEX_IGNITE ) {
            return new ToHitData( ToHitData.IMPOSSIBLE, "Invalid attack");
        }

        //Set the base BTH
          int base = 4;
          
          if ( game.getOptions().booleanOption("maxtech_physical_BTH") ) {
            base = ae.getCrew().getPiloting() - 1;
          }
          
          toHit = new ToHitData(base, "base");

        // attacker movement
        toHit.append(getAttackerMovementModifier(game, attackerId));
        
        // target movement
        toHit.append(getTargetMovementModifier(game, targetId));
        
        // attacker terrain
        toHit.append(getAttackerTerrainModifier(game, attackerId));
        
        // target terrain
        toHit.append(getTargetTerrainModifier(game, te));
        
        // damaged or missing actuators
        if (!ae.hasWorkingSystem(Mech.ACTUATOR_SHOULDER, Mech.LOC_RARM)) {
            toHit.addModifier(2, "Right Shoulder destroyed");
        }
        if (!ae.hasWorkingSystem(Mech.ACTUATOR_SHOULDER, Mech.LOC_LARM)) {
            toHit.addModifier(2, "Left Shoulder destroyed");
        }

        // water partial cover?
        Hex targHex = game.board.getHex(target.getPosition());
        if (te.height() > 0 && targHex.levelOf(Terrain.WATER) == te.height()) {
            toHit.addModifier(3, "target has partial cover");
        }

    // target immobile
    toHit.append(getImmobileMod(te));
        
        modifyPhysicalBTHForAdvantages(ae, te, toHit);
        
        // side and elevation shouldn't matter

        // done!
        return toHit;
    }
    
    /**
    * Checks if a charge can hit the target, including movement
    */
    public static ToHitData toHitCharge(Game game, int attackerId, int targetId, MovePath md) {
            // the attacker must have at least walked...
            int movement=Entity.MOVE_WALK;
            // ...if they moved more than their walk MPs, they must've Run
            if (md.getMpUsed() > game.getEntity(attackerId).walkMP) {
                    movement=Entity.MOVE_RUN;
            };
            // ...and if they have a jump step, they must've jumped!
            if (md.contains(MovePath.STEP_START_JUMP)) {
                    movement=Entity.MOVE_JUMP;
            };

            return toHitCharge(game, attackerId, targetId, md, movement);
  };

  /**
  * Checks if a charge can hit the target, taking account of movement
  */
  public static ToHitData toHitCharge(Game game, int attackerId, int targetId, MovePath md, int movement) {
            final Entity target = game.getEntity( targetId );
            return toHitCharge( game, attackerId, target, md, movement );
        }

  /**
  * Checks if a charge can hit the target, including movement
  */
  public static ToHitData toHitCharge(Game game, int attackerId, Targetable target, MovePath md) {
    // the attacker must have at least walked...
    int movement=Entity.MOVE_WALK;
    // ...if they moved more than their walk MPs, they must've Run
    if (md.getMpUsed() > game.getEntity(attackerId).walkMP) {
      movement=Entity.MOVE_RUN;
    };
    // ...and if they have a jump step, they must've jumped!
    if (md.contains(MovePath.STEP_START_JUMP)) {
      movement=Entity.MOVE_JUMP;
    };

    return toHitCharge(game, attackerId, target, md, movement);
  };

  /**
  * Checks if a charge can hit the target, taking account of movement
  */
  public static ToHitData toHitCharge(Game game, int attackerId, Targetable target, MovePath md, int movement) {
        final Entity ae = game.getEntity(attackerId);
        Entity te = null;
        if ( target.getTargetType() == Targetable.TYPE_ENTITY ) {
            te = (Entity) target;
        }
        Coords chargeSrc = ae.getPosition();
        MoveStep chargeStep = null;

  // Infantry CAN'T charge!!!
  if ( ae instanceof Infantry ) {
      return new ToHitData(ToHitData.IMPOSSIBLE, "Infantry can't charge");
  }

        // let's just check this
        if (!md.contains(MovePath.STEP_CHARGE)) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Charge action not found in movment path");
        }

        // no jumping
        if (md.contains(MovePath.STEP_START_JUMP)) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "No jumping allowed while charging");
        }

        // no backwards
        if (md.contains(MovePath.STEP_BACKWARDS) 
            || md.contains(MovePath.STEP_LATERAL_LEFT_BACKWARDS)
            || md.contains(MovePath.STEP_LATERAL_RIGHT_BACKWARDS)) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "No backwards movement allowed while charging");
        }

        // Can't target a transported entity.
        if ( te != null && Entity.NONE != te.getTransportId() ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target is a passenger.");
        }

        // Can't target a entity conducting a swarm attack.
        if ( te != null && Entity.NONE != te.getSwarmTargetId() ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target is swarming a Mek.");
        }

        // determine last valid step
        compile(game, attackerId, md);
        for (final Enumeration i = md.getSteps(); i.hasMoreElements();) {
            final MoveStep step = (MoveStep)i.nextElement();
            if (step.getMovementType() == Entity.MOVE_ILLEGAL) {
                break;
            } else {
                if (step.getType() == MovePath.STEP_CHARGE) {
                    chargeStep = step;
                } else {
                    chargeSrc = step.getPosition();
                }
            }
        }

        // need to reach target
        if (chargeStep == null || !target.getPosition().equals(chargeStep.getPosition())) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Could not reach target with movement");
        }

        // target must have moved already
        if ( te != null && !te.isDone()) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target must be done with movement");
        }

  return toHitCharge(game, attackerId, target, chargeSrc, movement);
    }

    /**
     * To-hit number for a charge, assuming that movement has been handled
     */
    public static ToHitData toHitCharge(Game game, ChargeAttackAction caa) {
        final Entity entity = game.getEntity(caa.getEntityId());
        return toHitCharge(game, caa.getEntityId(), 
                           game.getTarget(caa.getTargetType(), caa.getTargetId()),
                           entity.getPosition(),entity.moved);
    }

    /**
     * To-hit number for a charge, assuming that movement has been handled
     * <p/>
     * This version is kept for backwards compatability.
     */
    public static ToHitData toHitCharge(Game game, int attackerId, int targetId, Coords src, int movement) {
        return toHitCharge( game, attackerId, game.getEntity(targetId), src, movement );
    }

    /**
     * To-hit number for a charge, assuming that movement has been handled
     */
    public static ToHitData toHitCharge(Game game, int attackerId, Targetable target, Coords src, int movement) {
        final Entity ae = game.getEntity(attackerId);
        int targetId = Entity.NONE;
        Entity te = null;
        if ( target.getTargetType() == Targetable.TYPE_ENTITY ) {
            te = (Entity) target;
            targetId = target.getTargetId();
        }
        final int attackerElevation = ae.elevationOccupied(game.board.getHex(src));
        final int attackerHeight = attackerElevation + ae.height();
        final int targetElevation = target.getElevation();
        final int targetHeight = target.absHeight();
        final boolean targetInBuilding = Compute.isInBuilding( game, te );
        Building bldg = null;
        if ( targetInBuilding ) {
            bldg = game.board.getBuildingAt( te.getPosition() );
        }
        ToHitData toHit = null;
        
        // arguments legal?
        if ( ae == null || target == null ) {
            throw new IllegalArgumentException("Attacker or target not valid");
        }
        
        // can't target yourself
        if (ae.equals(te)) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "You can't target yourself");
        }
        
  // Infantry CAN'T charge!!!
  if ( ae instanceof Infantry ) {
      return new ToHitData(ToHitData.IMPOSSIBLE, "Infantry can't charge");
  }

        // Can't target a transported entity.
        if ( te != null && Entity.NONE != te.getTransportId() ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target is a passenger.");
        }

        // can't make physical attacks while spotting
        if (ae.isSpotting()) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Attacker is spotting this turn");
        }

        // Can't target a entity conducting a swarm attack.
        if ( te != null && Entity.NONE != te.getSwarmTargetId() ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target is swarming a Mek.");
        }

        // check range
        if (src.distance(target.getPosition()) > 1 ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target not in range");
        }
        
        // mechs can only charge standing mechs
        if (ae instanceof Mech) {
            if ( te != null && !(te instanceof Mech) ) {
                return new ToHitData(ToHitData.IMPOSSIBLE, "Target is not a mech");
            }
            if ( te != null && te.isProne() ) {
                return new ToHitData(ToHitData.IMPOSSIBLE, "Target is prone");
            }
        }
        else if ( te instanceof Infantry ) {
            // Can't charge infantry.
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target is infantry/");
        }
        
        // target must be within 1 elevation level
        if ( attackerElevation > targetHeight ||
             attackerHeight < targetElevation ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target must be within 1 elevation level");
        }
        
        // can't charge while prone
        if (ae.isProne()) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Attacker is prone");
        }        
        
        // can't attack mech making a different displacement attack
        if ( te != null && te.hasDisplacementAttack() ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target is already making a charge/DFA attack");
        }
        
        // can't attack the target of another displacement attack
        if ( te != null && te.isTargetOfDisplacementAttack() && te.findTargetedDisplacement().getEntityId() != ae.getId() ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target is the target of another charge/DFA");
        }

        // Can't target units in buildings (from the outside).
        if ( targetInBuilding ) {
            if ( !isInBuilding(game, ae) ) {
                return new ToHitData(ToHitData.IMPOSSIBLE, "Target is inside building" );
            } 
            else if ( !game.board.getBuildingAt( ae.getPosition() )
                      .equals( bldg ) ) {
                return new ToHitData(ToHitData.IMPOSSIBLE, "Target is inside differnt building" );
            }
        }

        // Attacks against adjacent buildings automatically hit.
        if ( target.getTargetType() == Targetable.TYPE_BUILDING ) {
            return new ToHitData( ToHitData.AUTOMATIC_SUCCESS, 
                                  "Targeting adjacent building." );
        }
        
        // Can't target woods or ignite a building with a physical.
        if ( target.getTargetType() == Targetable.TYPE_BLDG_IGNITE ||
             target.getTargetType() == Targetable.TYPE_HEX_CLEAR ||
             target.getTargetType() == Targetable.TYPE_HEX_IGNITE ) {
            return new ToHitData( ToHitData.IMPOSSIBLE, "Invalid attack");
        }

        //Set the base BTH
          int base = 5;
          
          if ( game.getOptions().booleanOption("maxtech_physical_BTH") ) {
            base = ae.getCrew().getPiloting();
          }
          
          toHit = new ToHitData(base, "base");

        // attacker movement
        toHit.append(getAttackerMovementModifier(game, attackerId, movement));
        
        // target movement
        toHit.append(getTargetMovementModifier(game, targetId));
        
        // attacker terrain
        toHit.append(getAttackerTerrainModifier(game, attackerId));
        
        // target terrain
        toHit.append(getTargetTerrainModifier(game, te));
        
        // piloting skill differential
        if (ae.getCrew().getPiloting() != te.getCrew().getPiloting()) {
            toHit.addModifier(ae.getCrew().getPiloting() - te.getCrew().getPiloting(), "piloting skill differential");
        }

        // target prone
        if (te.isProne()) {
            toHit.addModifier(-2, "target prone and adjacent");
        }
        
        // water partial cover?
        Hex targHex = game.board.getHex(te.getPosition());
        if (te.height() > 0 && targHex.levelOf(Terrain.WATER) == te.height()) {
            toHit.addModifier(3, "target has partial cover");
        }

    // target immobile
    toHit.append(getImmobileMod(te));
        
        modifyPhysicalBTHForAdvantages(ae, te, toHit);
        
        // determine hit direction
        toHit.setSideTable(targetSideTable(src, te.getPosition(),
                                            te.getFacing(), te instanceof Tank));
                   
        // all charges resolved against full-body table, except vehicles.
        if (ae.getHeight() < target.getHeight()) {
            toHit.setHitTable(ToHitData.HIT_KICK);
        } else {
            toHit.setHitTable(ToHitData.HIT_NORMAL);
        }
        
        // done!
        return toHit;
    }

  /**
  * Damage that a mech does with a successful charge.  Assumes that 
  * delta_distance is correct.
  */
  public static int getChargeDamageFor(Entity entity) {
    return getChargeDamageFor(entity, entity.delta_distance);
  };

  /**
  * Damage that a mech does with a successful charge, given it will move a certain way
  */
  public static int getChargeDamageFor(Entity entity, int hexesMoved) {
         return (int)Math.ceil((entity.getWeight() / 10.0) * (hexesMoved - 1) * (entity.getLocationStatus(1)==Entity.LOC_WET ? 0.5 : 1) );
  };

    /**
     * Damage that a mech suffers after a successful charge.
     */
    public static int getChargeDamageTakenBy(Entity entity, Entity target) {
        return (int) Math.ceil( target.getWeight() / 10.0 * (entity.getLocationStatus(1)==Entity.LOC_WET ? 0.5 : 1) );
    }

    /**
     * Damage that a mech suffers after a successful charge.
     */
    public static int getChargeDamageTakenBy(Entity entity, Building bldg) {
        // ASSUMPTION: 10% of buildings CF at start of phase, round up.
        return (int) Math.ceil( bldg.getPhaseCF() / 10.0 );
    }
    
    /**
     * Checks if a death from above attack can hit the target, including movement
     */
    public static ToHitData toHitDfa(Game game, int attackerId, int targetId, MovePath md) {
        final Entity target = game.getEntity( targetId );
        return toHitDfa( game, attackerId, target, md );
    }

    /**
     * Checks if a death from above attack can hit the target, including movement
     */
    public static ToHitData toHitDfa(Game game, int attackerId, Targetable target, MovePath md) {
        final Entity ae = game.getEntity(attackerId);
        Entity te = null;
        if ( target.getTargetType() == Targetable.TYPE_ENTITY ) {
            te = (Entity) target;
        }
        Coords chargeSrc = ae.getPosition();
        MoveStep chargeStep = null;
        
        // Infantry CAN'T dfa!!!
  if ( ae instanceof Infantry ) {
      return new ToHitData(ToHitData.IMPOSSIBLE, "Infantry can't D.F.A.");
  }

        // let's just check this
        if (!md.contains(MovePath.STEP_DFA)) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "D.F.A. action not found in movment path");
        }
        
        // have to jump
        if (!md.contains(MovePath.STEP_START_JUMP)) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "D.F.A. must involve jumping");
        }

        // Can't target a transported entity.
        if ( te != null && Entity.NONE != te.getTransportId() ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target is a passenger.");
        }

        // can't make physical attacks while spotting
        if (ae.isSpotting()) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Attacker is spotting this turn");
        }

        // Can't target a entity conducting a swarm attack.
        if ( te != null && Entity.NONE != te.getSwarmTargetId() ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target is swarming a Mek.");
        }
        
        // determine last valid step
        compile(game, attackerId, md);
        for (final Enumeration i = md.getSteps(); i.hasMoreElements();) {
            final MoveStep step = (MoveStep)i.nextElement();
            if (step.getMovementType() == Entity.MOVE_ILLEGAL) {
                break;
            } else {
                if (step.getType() == MovePath.STEP_DFA) {
                    chargeStep = step;
                } else {
                    chargeSrc = step.getPosition();
                }
            }
        }
        
        // need to reach target
        if (chargeStep == null || !target.getPosition().equals(chargeStep.getPosition())) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Could not reach target with movement");
        }
        
        // target must have moved already
        if ( te != null && !te.isDone() ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target must be done with movement");
        }
        
  return toHitDfa(game, attackerId, target, chargeSrc);
    }
    
    public static ToHitData toHitDfa(Game game, DfaAttackAction daa) {
        final Entity entity = game.getEntity(daa.getEntityId());
        return toHitDfa(game, daa.getEntityId(),
                        game.getTarget(daa.getTargetType(), daa.getTargetId()),
                        entity.getPosition());
    }
    
    /**
     * To-hit number for a death from above attack, assuming that movement has 
     * been handled
     * <p/>
     * This version is kept for backwards compatability.
     */
    public static ToHitData toHitDfa(Game game, int attackerId, int targetId, Coords src) {
        return toHitDfa( game, attackerId, game.getEntity(targetId), src );
    }

    /**
     * To-hit number for a death from above attack, assuming that movement has 
     * been handled
     */
    public static ToHitData toHitDfa(Game game, int attackerId, Targetable target, Coords src) {
        final Entity ae = game.getEntity(attackerId);
        int targetId = Entity.NONE;
        Entity te = null;
        if ( target.getTargetType() == Targetable.TYPE_ENTITY ) {
            te = (Entity) target;
            targetId = target.getTargetId();
        }
        final boolean targetInBuilding = isInBuilding( game, te );
        ToHitData toHit = null;
        
        // arguments legal?
        if ( ae == null || target == null ) {
            throw new IllegalArgumentException("Attacker or target not valid");
        }
        
        // can't target yourself
        if (ae.equals(te)) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "You can't target yourself");
        }
        
  // Infantry CAN'T dfa!!!
  if ( ae instanceof Infantry ) {
      return new ToHitData(ToHitData.IMPOSSIBLE, "Infantry can't dfa");
  }

        // Can't target a transported entity.
        if ( te != null && Entity.NONE != te.getTransportId() ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target is a passenger.");
        }

        // Can't target a entity conducting a swarm attack.
        if ( te != null && Entity.NONE != te.getSwarmTargetId() ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target is swarming a Mek.");
        }

        // check range
        if (src.distance(target.getPosition()) > 1 ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target not in range");
        }
        
        // can't dfa while prone, even if you somehow did manage to jump
        if (ae.isProne()) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Attacker is prone");
        }
        
        // can't attack mech making a different displacement attack
        if ( te != null && te.hasDisplacementAttack() ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target is already making a charge/DFA attack");
        }
        
        // can't attack the target of another displacement attack
        if ( te != null && te.isTargetOfDisplacementAttack() &&
             te.findTargetedDisplacement().getEntityId() != ae.getId() ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target is the target of another charge/DFA");
        }        

        // Can't target units in buildings (from the outside).
        if ( targetInBuilding ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target is inside building" );
        }

        // Attacks against adjacent buildings automatically hit.
        if ( target.getTargetType() == Targetable.TYPE_BUILDING ) {
            return new ToHitData( ToHitData.AUTOMATIC_SUCCESS, 
                                  "Targeting adjacent building." );
        }
        
        // Can't target woods or ignite a building with a physical.
        if ( target.getTargetType() == Targetable.TYPE_BLDG_IGNITE ||
             target.getTargetType() == Targetable.TYPE_HEX_CLEAR ||
             target.getTargetType() == Targetable.TYPE_HEX_IGNITE ) {
            return new ToHitData( ToHitData.IMPOSSIBLE, "Invalid attack");
        }

        //Set the base BTH
          int base = 5;
          
          if ( game.getOptions().booleanOption("maxtech_physical_BTH") ) {
            base = ae.getCrew().getPiloting();
          }
          
          toHit = new ToHitData(base, "base");

  // BMR(r), page 33. +3 modifier for DFA on infantry.
  if ( te instanceof Infantry ) {
      toHit.addModifier( 3, "Infantry target" );
  }

        // Battle Armor targets are hard for Meks and Tanks to hit.
        if ( te instanceof BattleArmor ) {
            toHit.addModifier( 1, "battle armor target" );
        }
        
        // attacker movement
        toHit.append(getAttackerMovementModifier(game, attackerId, Entity.MOVE_JUMP));
        
        // target movement
        toHit.append(getTargetMovementModifier(game, targetId));
        
        // piloting skill differential
        if (ae.getCrew().getPiloting() != te.getCrew().getPiloting()) {
            toHit.addModifier(ae.getCrew().getPiloting() - te.getCrew().getPiloting(), "piloting skill differential");
        }

        // target prone
        if (te.isProne()) {
            toHit.addModifier(-2, "target prone and adjacent");
        }
        
        // water partial cover?
        Hex targHex = game.board.getHex(te.getPosition());
        if (te.height() > 0 && targHex.levelOf(Terrain.WATER) == te.height()) {
            toHit.addModifier(3, "target has partial cover");
        }

    // target immobile
    toHit.append(getImmobileMod(te));
        
        modifyPhysicalBTHForAdvantages(ae, te, toHit);
        
        if (te instanceof Tank) {
            toHit.setSideTable(ToHitData.SIDE_FRONT);
            toHit.setHitTable(ToHitData.HIT_NORMAL);
        }
        else if (te.isProne()) {
            toHit.setSideTable(ToHitData.SIDE_REAR);
            toHit.setHitTable(ToHitData.HIT_NORMAL);
        }
        else {
            toHit.setSideTable(targetSideTable(src, te.getPosition(),
                                            te.getFacing(), te instanceof Tank));
            toHit.setHitTable(ToHitData.HIT_PUNCH);
        }
        
        // done!
        return toHit;
    }
        
    /**
     * Damage that a mech does with a successful DFA.
     */
    public static int getDfaDamageFor(Entity entity) {
        return (int)Math.ceil((entity.getWeight() / 10.0) * 3.0);
    }
    
    /**
     * Damage done to a mech after a successful DFA.
     */
    public static int getDfaDamageTakenBy(Entity entity) {
        return (int)Math.ceil(entity.getWeight() / 5.0);
    }
    
    
    /**
     * Modifier to attacks due to attacker movement
     */
    public static ToHitData getAttackerMovementModifier(Game game, int entityId) {
        return getAttackerMovementModifier(game, entityId, game.getEntity(entityId).moved);
    }
    
    /**
     * Modifier to attacks due to attacker movement
     */
    public static ToHitData getAttackerMovementModifier(Game game, int entityId, int movement) {
        final Entity entity = game.getEntity(entityId);
        ToHitData toHit = new ToHitData();
        
        // infantry aren't affected by their own movement
        if (entity instanceof Infantry) {
            return toHit;
        }
        
        if (movement == Entity.MOVE_WALK) {
            toHit.addModifier(1, "attacker walked");
        } else if (movement == Entity.MOVE_RUN ||
                   movement == Entity.MOVE_SKID) {
            toHit.addModifier(2, "attacker ran");
        } else if (movement == Entity.MOVE_JUMP) {
            toHit.addModifier(3, "attacker jumped");
        }
        
        return toHit;
    }
    
    /**
     * Modifier to physical attack BTH due to pilot advantages
     */
      public static void modifyPhysicalBTHForAdvantages(Entity attacker, Entity target, ToHitData toHit) {
        int movement = attacker.moved;
        
        if ( attacker.getCrew().getOptions().booleanOption("melee_specialist") && attacker instanceof Mech ) {
          int mod = 0;
          
          if (movement == Entity.MOVE_WALK) {
            mod = 1;
          } else if (movement == Entity.MOVE_RUN || movement == Entity.MOVE_SKID) {
            mod = 2;
          } else if (movement == Entity.MOVE_JUMP) {
            mod = 3;
          }
          
          if ( mod == 0 )
            return;
          
          toHit.addModifier(-1, "melee specialist");
        }
        
        if (  (target instanceof Mech) && target.getCrew().getOptions().booleanOption("dodge_maneuver") && target.dodging ) {
          toHit.addModifier(2, "target is dodging");
        }
      }
     
    /**
     * Modifier to physical attack damage due to melee specialist
     */
      public static int modifyPhysicalDamagaForMeleeSpecialist(Entity entity) {
        if ( !entity.getCrew().getOptions().booleanOption("melee_specialist") )
          return 0;
        
        return 1;
      }
     
    /**
     * Modifier to attacks due to target movement
     */
    public static ToHitData getTargetMovementModifier(Game game, int entityId) {
        Entity entity = game.getEntity(entityId);
        ToHitData toHit = getTargetMovementModifier
            ( entity.delta_distance, entity.moved == Entity.MOVE_JUMP, game.getOptions().booleanOption("maxtech_target_modifiers") ); 

        // Did the target skid this turn?
        if ( entity.moved == Entity.MOVE_SKID ) {
            toHit.addModifier( 2, "target skidded" );
        }

        return toHit;
    }
  
    /**
     * Target movement modifer for the specified delta_distance
     */
    public static ToHitData getTargetMovementModifier(int distance, boolean jumped) {
    	return getTargetMovementModifier(distance, jumped, false);
    }

    public static ToHitData getTargetMovementModifier(int distance, boolean jumped, boolean maxtech) {
        ToHitData toHit = new ToHitData();
      
		if (!maxtech) {
			if (distance >= 3 && distance <= 4) {
	            toHit.addModifier(1, "target moved 3-4 hexes");
	        } else if (distance >= 5 && distance <= 6) {
	            toHit.addModifier(2, "target moved 5-6 hexes");
	        } else if (distance >= 7 && distance <= 9) {
	            toHit.addModifier(3, "target moved 7-9 hexes");
	        } else if (distance >= 10) {
	            toHit.addModifier(4, "target moved 10+ hexes");
	        }
	    } else {
			if (distance >= 3 && distance <= 4) {
			   toHit.addModifier(1, "target moved 3-4 hexes");
			} else if (distance >= 5 && distance <= 6) {
			   toHit.addModifier(2, "target moved 5-6 hexes");
			} else if (distance >= 7 && distance <= 9) {
			   toHit.addModifier(3, "target moved 7-9 hexes");
			} else if (distance >= 10 && distance <= 13) {
			   toHit.addModifier(4, "target moved 10-13 hexes");
			} else if (distance >= 14 && distance <=18) {
			   toHit.addModifier(5, "target moved 14-18 hexes");
			} else if (distance >= 19 && distance <=24) {
			   toHit.addModifier(6, "target moved 19-24 hexes");
			} else if (distance >= 25) {
			   toHit.addModifier(7, "target moved 25+ hexes");
			}
        }
	    	
        if (jumped) {
            toHit.addModifier(1, "target jumped");
        }
      
        return toHit;
    }
    
    /**
     * Modifier to attacks due to attacker terrain
     */
    public static ToHitData getAttackerTerrainModifier(Game game, int entityId) {
        final Entity attacker = game.getEntity(entityId);
        final Hex hex = game.board.getHex(attacker.getPosition());
        ToHitData toHit = new ToHitData();

        if (hex.levelOf(Terrain.WATER) > 0 
        && attacker.getMovementType() != Entity.MovementType.HOVER) {
            toHit.addModifier(1, "attacker in water");
        }
        
        return toHit;
    }
    
    /**
     * Modifier to attacks due to target terrain
     */
    public static ToHitData getTargetTerrainModifier(Game game, Targetable t) {
        Entity entityTarget = null;
        if (t.getTargetType() == Targetable.TYPE_ENTITY) {
            entityTarget = (Entity) t;
        }
        final Hex hex = game.board.getHex(t.getPosition());
        
        // you don't get terrain modifiers in midair
        // should be abstracted more into a 'not on the ground' flag for vtols and such
        if (entityTarget != null && entityTarget.isMakingDfa()) {
            return null;
        }
        
        ToHitData toHit = new ToHitData();
        
        // only entities get terrain bonuses 
        // TODO: should this be changed for buildings???
        if (entityTarget == null) {
            return toHit;
        }

        if (hex.levelOf(Terrain.WATER) > 0
        && entityTarget.getMovementType() != Entity.MovementType.HOVER) {
            toHit.addModifier(-1, "target in water");
        }

        if (hex.contains(Terrain.SMOKE)) {
            toHit.addModifier(2, "target in smoke");
        }
        else if (hex.levelOf(Terrain.WOODS) == 1) {
            toHit.addModifier(1, "target in light woods");
        } else if (hex.levelOf(Terrain.WOODS) > 1) {
            toHit.addModifier(2, "target in heavy woods");
        }

        
        return toHit;
    }
    
    /**
     * Returns the weapon attack out of a list that has the highest expected damage
     */
    public static WeaponAttackAction getHighestExpectedDamage(Game g, Vector vAttacks)
    {
    float fHighest = -1.0f;
        WeaponAttackAction waaHighest = null;
        for (int x = 0, n = vAttacks.size(); x < n; x++) {
            WeaponAttackAction waa = (WeaponAttackAction)vAttacks.elementAt(x);
            float fDanger = getExpectedDamage(g, waa);
            if (fDanger > fHighest) {
                fHighest = fDanger;
                waaHighest = waa;
            }
        }
        return waaHighest;
    }
    
    // store these as constants since the tables will never change
    private static float[] expectedHitsByRackSize = { 0.0f, 1.0f, 1.58f, 2.0f, 2.63f, 3.17f, 
            4.0f, 0.0f, 0.0f, 5.47f, 6.31f, 0.0f, 8.14f, 0.0f, 0.0f, 9.5f, 0.0f, 0.0f, 0.0f, 
            0.0f, 12.7f };
        
    /**
     * Determines the expected damage of a weapon attack, based on to-hit, salvo sizes, etc.
     */
    public static float getExpectedDamage(Game g, WeaponAttackAction waa)
    {
        Entity attacker = g.getEntity(waa.getEntityId());
        Mounted weapon = attacker.getEquipment(waa.getWeaponId());
        System.out.println("Computing expected damage for " + attacker.getShortName() + " " + 
                weapon.getName());
        ToHitData hitData = Compute.toHitWeapon(g, waa);
        if (hitData.getValue() == ToHitData.IMPOSSIBLE || hitData.getValue() == ToHitData.AUTOMATIC_FAIL) {
            return 0.0f;
        }
        
        float fChance = 0.0f;
        if (hitData.getValue() == ToHitData.AUTOMATIC_SUCCESS) {
            fChance = 1.0f;
        }
        else {
            fChance = (float)oddsAbove(hitData.getValue()) / 100.0f;
        }
        System.out.println("\tHit Chance: " + fChance);

        // TODO : update for BattleArmor.
        
        float fDamage = 0.0f;
        WeaponType wt = (WeaponType)weapon.getType();
        if (wt.getDamage() == WeaponType.DAMAGE_MISSILE) {
            if (weapon.getLinked() == null) return 0.0f;
            AmmoType at = (AmmoType)weapon.getLinked().getType();
            
            float fHits = 0.0f;
            if (wt.getAmmoType() == AmmoType.T_SRM_STREAK) {
                fHits = wt.getRackSize();
            }
            else if (wt.getRackSize() == 40 || wt.getRackSize() == 30) {
                fHits = 2.0f * expectedHitsByRackSize[wt.getRackSize() / 2];
            }
            else {
                fHits = expectedHitsByRackSize[wt.getRackSize()];
            }
            // adjust for previous AMS
            Vector vCounters = waa.getCounterEquipment();
            if (vCounters != null) {
                for (int x = 0; x < vCounters.size(); x++) {
                    Mounted counter = (Mounted)vCounters.elementAt(x);
                    if (counter.getType() instanceof WeaponType && 
                            ((WeaponType)counter.getType()).getAmmoType() == AmmoType.T_AMS) {
                        float fAMS = 3.5f * ((WeaponType)counter.getType()).getDamage();
                        fHits = Math.max(0.0f, fHits - fAMS);
                    }
                }
            }
            System.out.println("\tExpected Hits: " + fHits);
            // damage is expected missiles * damage per missile
            fDamage = fHits * (float)at.getDamagePerShot();
        }
        else {
            fDamage = (float)wt.getDamage();
        }
        
        fDamage *= fChance;
        System.out.println("\tExpected Damage: " + fDamage);
        return fDamage;
    }
    
    /**
     * Checks to see if a target is in arc of the specified
     * weapon, on the specified entity
     */
    public static boolean isInArc(Game game, int attackerId, int weaponId, Targetable t) {
        Entity ae = game.getEntity(attackerId);
        int facing = ae.isSecondaryArcWeapon(weaponId) ? ae.getSecondaryFacing() : ae.getFacing();
        return isInArc(ae.getPosition(), facing, t.getPosition(), ae.getWeaponArc(weaponId));
    }
    
    /**
     * Returns true if the target is in the specified arc.
     * @param src the attacker coordinate
     * @param facing the appropriate attacker sfacing
     * @param dest the target coordinate
     * @param arc the arc
     */
    public static boolean isInArc(Coords src, int facing, Coords dest, int arc) {
        // calculate firing angle
        int fa = src.degree(dest) - facing * 60;
        if (fa < 0) {
            fa += 360;
        }
        // is it in the specifed arc?
        switch(arc) {
        case ARC_FORWARD :
            return fa >= 300 || fa <= 60;
        case Compute.ARC_RIGHTARM :
            return fa >= 300 || fa <= 120;
        case Compute.ARC_LEFTARM :
            return fa >= 240 || fa <= 60;
        case ARC_REAR :
            return fa > 120 && fa < 240;
        case ARC_RIGHTSIDE :
            return fa > 60 && fa <= 120;
        case ARC_LEFTSIDE :
            return fa < 300 && fa >= 240;
        case ARC_360 :
            return true;
        default:
            return false;
        }
    }
    
    /**
     * LOS check from ae to te.
     */
    public static boolean canSee(Game game, Entity ae, Targetable target)
    {
        LosEffects los = calculateLos(game, ae.getId(), target);
        return !los.blocked && los.lightWoods + ((los.heavyWoods + los.smoke) * 2) < 3;
    }
    
    /**
     * Returns an array of the Coords of hexes that are crossed by a straight 
     * line from the center of src to the center of dest, including src & dest.
     *
     * The returned coordinates are in line order, and if the line passes
     * directly between two hexes, it returns them both.
     *
     * Based on the degree of the angle, the next hex is going to be one of
     * three hexes.  We check those three hexes, sides first, add the first one 
     * that intersects and continue from there.
     *
     * Based off of some of the formulas at Amit's game programming site.
     * (http://www-cs-students.stanford.edu/~amitp/gameprog.html)
     */
    public static Coords[] intervening(Coords src, Coords dest) {
        IdealHex iSrc = IdealHex.get(src);
        IdealHex iDest = IdealHex.get(dest);
        
        int[] directions = new int[3];
        directions[2] = src.direction(dest); // center last
        directions[1] = (directions[2] + 5) % 6;
        directions[0] = (directions[2] + 1) % 6;
        
        Vector hexes = new Vector();
        Coords current = src;
        
        hexes.addElement(current);
        while(!dest.equals(current)) {
            current = nextHex(current, iSrc, iDest, directions);
            hexes.addElement(current);
        }

        Coords[] hexArray = new Coords[hexes.size()];
        hexes.copyInto(hexArray);
        return hexArray;
    }
    
    /**
     * Returns the first further hex found along the line from the centers of
     * src to dest.  Checks the three directions given and returns the closest.
     *
     * This relies on the side directions being given first.  If it checked the
     * center first, it would end up missing the side hexes sometimes.
     *
     * Not the most elegant solution, but it works.
     */
    public static Coords nextHex(Coords current, IdealHex iSrc, IdealHex iDest, int[] directions) {
        for (int i = 0; i < directions.length; i++) {
            Coords testing = current.translated(directions[i]);
            if (IdealHex.get(testing).isIntersectedBy(iSrc.cx, iSrc.cy, iDest.cx, iDest.cy)) {
                return testing;
            }
        }
        // if we're here then something's fishy!
        throw new RuntimeException("Couldn't find the next hex!");
    }

    public static int targetSideTable(Entity attacker, Targetable target) {
        if (target.getTargetType() != Targetable.TYPE_ENTITY) {
            return ToHitData.SIDE_FRONT;
        }
        Entity te = (Entity)target;
        return targetSideTable(attacker.getPosition(), te.getPosition(), te.getFacing(), te instanceof Tank);
    }
    
    /**
     * Returns the side location table that you should be using
     */
    public static int targetSideTable(Coords src, Coords dest, int targetFacing, boolean targetIsTank) {
        // calculate firing angle
        int fa = (dest.degree(src) + (6 - targetFacing) * 60) % 360;

        if (targetIsTank) {
            if (fa > 30 && fa <= 150) {
                return ToHitData.SIDE_RIGHT;
            } else if (fa > 150 && fa < 210) {
                return ToHitData.SIDE_REAR;
            } else if (fa >= 210 && fa < 330) {
                return ToHitData.SIDE_LEFT;
            } else {
                return ToHitData.SIDE_FRONT;
            }
        } else {
            if (fa > 90 && fa <= 150) {
                return ToHitData.SIDE_RIGHT;
            } else if (fa > 150 && fa < 210) {
                return ToHitData.SIDE_REAR;
            } else if (fa >= 210 && fa < 270) {
                return ToHitData.SIDE_LEFT;
            } else {
                return ToHitData.SIDE_FRONT;
            }
        }
    }
    
    /**
     * Returns whether an entity can find a club in its current location
     */
    public static boolean canMechFindClub(Game game, int entityId) {
        final Entity entity = game.getEntity(entityId);
        if ( null == entity.getPosition() ) {
            return false;
        }
        final Hex hex = game.board.getHex(entity.getPosition());
        
        //Non bipeds can't
        if ( entity.getMovementType() != Entity.MovementType.BIPED ) {
            return false;
        }

        // The hex must contain woods or rubble from
        // a medium, heavy, or hardened building.
        //TODO: missing limbs are clubs.
        if ( hex.levelOf(Terrain.WOODS) < 1 &&
             hex.levelOf(Terrain.RUBBLE) < Building.MEDIUM ) {
            return false;
        }
        
        // also, need shoulders and hands
        if (!entity.hasWorkingSystem(Mech.ACTUATOR_SHOULDER, Mech.LOC_RARM)
        || !entity.hasWorkingSystem(Mech.ACTUATOR_SHOULDER, Mech.LOC_LARM)
        || !entity.hasWorkingSystem(Mech.ACTUATOR_HAND, Mech.LOC_RARM)
        || !entity.hasWorkingSystem(Mech.ACTUATOR_HAND, Mech.LOC_LARM)) {
            return false;
        }
        
        // and last, check if you already have a club, greedy
        if (clubMechHas(entity) != null) {
            return false;
        }
        
        return true;
    }
    
    
    /**
     * Returns the club a mech possesses, or null if none.
     */
    public static Mounted clubMechHas(Entity entity) {
        for (Enumeration i = entity.getMisc(); i.hasMoreElements();) {
            Mounted mounted = (Mounted)i.nextElement();
            if (mounted.getType().hasFlag(MiscType.F_CLUB) || mounted.getType().hasFlag(MiscType.F_HATCHET) || mounted.getType().hasFlag(MiscType.F_SWORD)) {
                return mounted;
            }
        }
        return null;
    }
    
    /**
     * Returns whether an entity can flee from its current position.  Currently
     * returns true if the entity is on the edge of the board.
     */
    public static boolean canEntityFlee(Game game, int entityId) {
        Entity entity = game.getEntity(entityId);
        Coords pos = entity.getPosition();
        return pos != null && entity.getWalkMP() > 0 && !entity.isProne()
            && (pos.x == 0 || pos.x == game.board.width - 1 
                || pos.y == 0 || pos.y == game.board.height - 1);
    }

    /**
     * Maintain backwards compatability.
     *
     * @param   missiles - the <code>int</code> number of missiles in the pack.
     */
    public static int missilesHit(int missiles) {
        return missilesHit(missiles, 0);
    }

    /**
     * Roll the number of missiles (or whatever) on the missile
     * hit table, with the specified mod to the roll.
     *
     * @param   missiles - the <code>int</code> number of missiles in the pack.
     * @param   nMod - the <code>int</code> modifier to the roll for number
     *          of missiles that hit.
     */
    public static int missilesHit(int missiles, int nMod) {
        int nRoll = d6(2) + nMod;
        nRoll = Math.min(Math.max(nRoll, 2), 12);

        if (missiles == 2) {
            switch(nRoll) {
            case 2 :
            case 3 :
            case 4 :
            case 5 :
            case 6 :
            case 7 :
                return 1;
            case 8 :
            case 9 :
            case 10 :
            case 11 :
            case 12 :
                return 2;
            }
        }

        else if (missiles == 3) {
            switch(nRoll) {
            case 2 :
            case 3 :
            case 4 :
                return 1;
            case 5 :
            case 6 :
            case 7 :
            case 8 :
            case 9 :
                return 2;
            case 10 :
            case 11 :
            case 12 :
                return 3;
            }
        }

        else if (missiles == 4) {
            switch(nRoll) {
            case 2 :
                return 1;
            case 3 :
            case 4 :
            case 5 :
            case 6 :
                return 2;
            case 7 :
            case 8 :
            case 9 :
            case 10 :
                return 3;
            case 11 :
            case 12 :
                return 4;
            }
        }

        else if (missiles == 5) {
            switch(nRoll) {
            case 2 :
                return 1;
            case 3 :
            case 4 :
                return 2;
            case 5 :
            case 6 :
            case 7 :
            case 8 :
                return 3;
            case 9 :
            case 10 :
                return 4;
            case 11 :
            case 12 :
                return 5;
            }
        }

        else if (missiles == 6) {
            switch(nRoll) {
            case 2 :
            case 3 :
                return 2;
            case 4 :
            case 5 :
                return 3;
            case 6 :
            case 7 :
            case 8 :
                return 4;
            case 9 :
            case 10 :
                return 5;
            case 11 :
            case 12 :
                return 6;
            }
        }

        else if (missiles == 8) {
            switch(nRoll) {
            case 2 :
                return 2;
            case 3 :
            case 4 :
                return 3;
            case 5 :
            case 6 :
                return 4;
            case 7 :
            case 8 :
                return 5;
            case 9 :
                return 6;
            case 10 :
                return 7;
            case 11 :
            case 12 :
                return 8;
            }
        }

        else if (missiles == 9) {
            switch(nRoll) {
            case 2 :
            case 3 :
                return 3;
            case 4 :
                return 4;
            case 5 :
            case 6 :
            case 7 :
            case 8 :
                return 5;
            case 9 :
            case 10 :
                return 7;
            case 11 :
            case 12 :
                return 9;
            }
        }

        else if (missiles == 10) {
            switch(nRoll) {
            case 2 :
            case 3 :
                return 3;
            case 4 :
                return 4;
            case 5 :
            case 6 :
            case 7 :
            case 8 :
                return 6;
            case 9 :
            case 10 :
                return 8;
            case 11 :
            case 12 :
                return 10;
            }
        }

        else if (missiles == 12) {
            switch(nRoll) {
            case 2 :
            case 3 :
                return 4;
            case 4 :
                return 5;
            case 5 :
            case 6 :
            case 7 :
            case 8 :
                return 8;
            case 9 :
            case 10 :
                return 10;
            case 11 :
            case 12 :
                return 12;
            }
        }

        else if (missiles == 15) {
            switch(nRoll) {
            case 2 :
            case 3 :
                return 5;
            case 4 :
                return 6;
            case 5 :
            case 6 :
            case 7 :
            case 8 :
                return 9;
            case 9 :
            case 10 :
                return 12;
            case 11 :
            case 12 :
                return 15;
            }
        }

        else if (missiles == 20) {
            switch(nRoll) {
            case 2 :
            case 3 :
                return 6;
            case 4 :
                return 9;
            case 5 :
            case 6 :
            case 7 :
            case 8 :
                return 12;
            case 9 :
            case 10 :
                return 16;
            case 11 :
            case 12 :
                return 20;
            }
        }

        return 0;
    }
    
    /**
     * Returns the conciousness roll number
     * 
     * @param hit - the <code>int</code> number of the crew hit currently
     *    being rolled.
     * @return  The <code>int</code> number that must be rolled on 2d6
     *    for the crew to stay concious.
     */
    public static int getConciousnessNumber(int hit) {
        switch(hit) {
        case 0:
            return 2;
        case 1:
            return 3;
        case 2:
            return 5;
        case 3:
            return 7;
        case 4:
            return 10;
        case 5:
            return 11;
        default:
            return Integer.MAX_VALUE;
        }
    }
    
    /**
     * Checks to see if a line passing from Coords A to Coords B is intercepted
     * by an ECM field generated by an enemy of Entity AE.
     */
    public static boolean isAffectedByECM(Entity ae, Coords a, Coords b) {
        
        if (a == null || b == null) return false;

        // Only grab enemies with active ECM
        Vector vEnemyCoords = new Vector(16);
        Vector vECMRanges = new Vector(16);
        for (Enumeration e = ae.game.getEntities(); e.hasMoreElements(); ) {
            Entity ent = (Entity)e.nextElement();
            Coords entPos = ent.getPosition();
            if (ent.isEnemyOf(ae) && ent.hasActiveECM() && entPos != null) {
                // TODO : only use the best ECM range in a given Coords.
                vEnemyCoords.addElement( entPos );
                vECMRanges.addElement( new Integer(ent.getECMRange()) );
            }

            // Check the ECM effects of the entity's passengers.
            Vector passengers = ent.getLoadedUnits();
            Enumeration iter = passengers.elements();
            while ( iter.hasMoreElements() ) {
                Entity other = (Entity) iter.nextElement();
                if (other.isEnemyOf(ae) && other.hasActiveECM() && entPos != null) {
                    // TODO : only use the best ECM range in a given Coords.
                    vEnemyCoords.addElement( entPos );
                    vECMRanges.addElement( new Integer(other.getECMRange()) );
                }
            }

        }
        
        // none?  get out of here
        if (vEnemyCoords.size() == 0) return false;
        
        // get intervening Coords.  See the comments for intervening() and losDivided()
        Coords[] coords = intervening(a, b);
        boolean bDivided = (a.degree(b) % 60 == 30);
        Enumeration ranges = vECMRanges.elements();
        for (Enumeration e = vEnemyCoords.elements(); e.hasMoreElements(); ) {
            Coords c = (Coords)e.nextElement();
            int range = ( (Integer) ranges.nextElement() ).intValue();
            int nLastDist = -1;
            
            // loop through intervening hexes and see if any of them are within range
            for (int x = 0; x < coords.length; x++) {
                int nDist = c.distance(coords[x]);

                if ( nDist <= range ) return true;
                
                // optimization.  if we're getting farther away, forget it
                // but ignore the doubled-up hexes intervening() adds along hexlines
                if (!bDivided || (x % 3 == 0)) {
                    if (nLastDist == -1) {
                        nLastDist = nDist;
                    }
                    else if (nDist > nLastDist) {
                        break;
                    }
                }
            }
        }
        return false;
    }    

    /**
     * Get the base to-hit number of a Leg Attack by the given attacker upon
     * the given defender
     *
     * @param   attacker - the <code>Entity</code> conducting the leg attack.
     * @param   defender - the <code>Entity</code> being attacked.
     * @return  The base <code>ToHitData</code> of the attack.
     */
    public static ToHitData getLegAttackBaseToHit( Entity attacker,
                                                   Entity defender ) {
        int men = 0;
        int base = ToHitData.IMPOSSIBLE;
        StringBuffer reason = new StringBuffer();

        // Can only attack a Mek's legs.
        if ( !(defender instanceof Mech) ) {
            reason.append( "Defender is not a Mek." );
        }

        // Can't target a transported entity.
        else if ( Entity.NONE != defender.getTransportId() ) {
            reason.append( "Target is a passenger." );
        }

        // Can't target a entity conducting a swarm attack.
        else if ( Entity.NONE != defender.getSwarmTargetId() ) {
            reason.append("Target is swarming a Mek.");
        }

        // Attacker can't be swarming.
        else if ( Entity.NONE != attacker.getSwarmTargetId() ) {
            reason.append( "Attacker is swarming." );
        }

        // Handle BattleArmor attackers.
        else if ( attacker instanceof BattleArmor ) {
            BattleArmor inf = (BattleArmor) attacker;

            // Battle Armor units can't Leg Attack if they're burdened.
            if ( inf.isBurdened() ) {
                reason.append( "Launcher not jettisoned." );
            } else {
                men = inf.getShootingStrength();
                if ( men >= 4 ) base = 4;
                else if ( men >= 3 ) base = 7;
                else if ( men >= 2 ) base = 10;
                else if ( men >= 1 ) base = 12;
                reason.append( men );
                reason.append( " trooper(s) active" );
            }
        }

        // Non-BattleArmor infantry need many more men.
        else if ( attacker instanceof Infantry ) {
            Infantry inf = (Infantry) attacker;
            men = inf.getShootingStrength();
            if ( men >= 22 ) base = 4;
            else if ( men >= 16 ) base = 7;
            else if ( men >= 10 ) base = 10;
            else if ( men >= 5 ) base = 12;
            reason.append( men );
            reason.append( " men alive" );
        }

        // No one else can conduct leg attacks.
        else {
            reason.append( "Attacker is not infantry." );
        }

        // Return the ToHitData for this attack.
        // N.B. we attack the legs.
        return new ToHitData( base, reason.toString(), 
                              ToHitData.HIT_KICK, ToHitData.SIDE_FRONT );
    }

    /**
     * Get the base to-hit number of a Swarm Mek by the given attacker upon
     * the given defender.
     *
     * @param   attacker - the <code>Entity</code> swarming.
     * @param   defender - the <code>Entity</code> being swarmed.
     * @return  The base <code>ToHitData</code> of the mek.
     */
    public static ToHitData getSwarmMekBaseToHit( Entity attacker,
                                                  Entity defender ) {
        int men = 0;
        int base = ToHitData.IMPOSSIBLE;
        StringBuffer reason = new StringBuffer();

        // Can only swarm a Mek.
        if ( !(defender instanceof Mech) ) {
            reason.append( "Defender is not a Mek." );
        }

        // Can't target a transported entity.
        else if ( Entity.NONE != defender.getTransportId() ) {
            reason.append("Target is a passenger.");
        }

        // Attacker can't be swarming.
        else if ( Entity.NONE != attacker.getSwarmTargetId() ) {
            reason.append( "Attacker is swarming." );
        }

        // Can't target a entity invloved in a swarm attack.
        else if ( Entity.NONE != defender.getSwarmAttackerId() ) {
            reason.append("Target is already being swarmed.");
        } 

        // Can't target a entity conducting a swarm attack.
        else if ( Entity.NONE != defender.getSwarmTargetId() ) {
            reason.append("Target is swarming a Mek.");
        }

        // Handle BattleArmor attackers.
        else if ( attacker instanceof BattleArmor ) {
            BattleArmor inf = (BattleArmor) attacker;

            // Battle Armor units can't Leg Attack if they're burdened.
            if ( inf.isBurdened() ) {
                reason.append( "Launcher not jettisoned." );
            } else {
                men = inf.getShootingStrength();
                if ( men >= 4 ) base = 7;
                else if ( men >= 1 ) base = 10;
                reason.append( men );
                reason.append( " trooper(s) active" );
            }
        }

        // Non-BattleArmor infantry need many more men.
        else if ( attacker instanceof Infantry ) {
            Infantry inf = (Infantry) attacker;
            men = inf.getShootingStrength();
            if ( men >= 22 ) base = 7;
            else if ( men >= 16 ) base = 10;
            reason.append( men );
            reason.append( " men alive" );
        }

        // No one else can conduct leg attacks.
        else {
            reason.append( "Attacker is not infantry." );
        }

        // Return the ToHitData for this attack.
        return new ToHitData( base, reason.toString() );
    }

    /**
     * Determine the number of shots from a Battle Armor unit's attack hit.
     *
     * @param   shots - the <code>int</code> number of shots from the unit.
     * @return  the <code>int</code> number of shots that hit the target.
     */
    public static int getBattleArmorHits( int shots ) {
        // 2003-01-02 : Battle Armor attacks don't get modifiers.
        int nMod = 0;

        int nRoll = d6(2) + nMod;
        nRoll = Math.min(Math.max(nRoll, 2), 12);

        if (shots == 1) {
            return 1;
        }

        else if (shots == 2) {
            switch(nRoll) {
            case 2 :
            case 3 :
            case 4 :
            case 5 :
            case 6 :
                return 1;
            case 7 :
            case 8 :
            case 9 :
            case 10 :
            case 11 :
            case 12 :
                return 2;
            }
        }

        else if (shots == 3) {
            switch(nRoll) {
            case 2 :
            case 3 :
                return 1;
            case 4 :
            case 5 :
            case 6 :
            case 7 :
            case 8 :
                return 2;
            case 9 :
            case 10 :
            case 11 :
            case 12 :
                return 3;
            }
        }

        else if (shots == 4) {
            switch(nRoll) {
            case 2 :
                return 1;
            case 3 :
            case 4 :
            case 5 :
            case 6 :
                return 2;
            case 7 :
            case 8 :
            case 9 :
                return 3;
            case 10 :
            case 11 :
            case 12 :
                return 4;
            }
        }

        else if (shots == 5) {
            switch(nRoll) {
            case 2 :
                return 1;
            case 3 :
            case 4 :
                return 2;
            case 5 :
            case 6 :
            case 7 :
                return 3;
            case 8 :
            case 9 :
            case 10 :
                return 4;
            case 11 :
            case 12 :
                return 5;
            }
        }

        return 0;
    }

    /**
     * To-hit number for thrashing attack.  This attack can only be made
     * by a prone Mek in a clear or pavement terrain hex that contains
     * infantry.  This attack will force a PSR check for the prone Mek;
     * if the PSR is missed, the Mek takes normal falling damage.
     *
     * @param   game - the <code>Game</code> object containing all entities.
     * @param   act - the <code>ThrashAttackAction</code> for the attack.
     * @return  the <code>ToHitData</code> containing the target roll.
     */
    public static ToHitData toHitThrash(Game game, ThrashAttackAction act){
        return toHitThrash( game, act.getEntityId(),
                            game.getTarget(act.getTargetType(),
                                           act.getTargetId()) );
    }

    /**
     * To-hit number for thrashing attack.  This attack can only be made
     * by a prone Mek in a clear or pavement terrain hex that contains
     * infantry.  This attack will force a PSR check for the prone Mek;
     * if the PSR is missed, the Mek takes normal falling damage.
     * <p/>
     * This version is kept for backwards compatability.
     *
     * @param   game - the <code>Game</code> object containing all entities.
     * @param   attackerId - the <code>int</code> ID of the attacking unit.
     * @param   targetId - the <code>int</code> ID of the attack's target.
     * @return  the <code>ToHitData</code> containing the target roll.
     */
    public static ToHitData toHitThrash( Game game, int attackerId, 
                                         int targetId ) {
        return toHitThrash( game, attackerId, game.getEntity(targetId) );
    }

    /**
     * To-hit number for thrashing attack.  This attack can only be made
     * by a prone Mek in a clear or pavement terrain hex that contains
     * infantry.  This attack will force a PSR check for the prone Mek;
     * if the PSR is missed, the Mek takes normal falling damage.
     *
     * @param   game - the <code>Game</code> object containing all entities.
     * @param   attackerId - the <code>int</code> ID of the attacking unit.
     * @param   target - the <code>Targetable</code> unit being targeted.
     * @return  the <code>ToHitData</code> containing the target roll.
     */
    public static ToHitData toHitThrash( Game game, int attackerId,
                                         Targetable target) {
        final Entity ae = game.getEntity(attackerId);
        Entity te = null;
        if ( target.getTargetType() == Targetable.TYPE_ENTITY ) {
            te = (Entity) target;
        }

        // arguments legal?
        if (ae == null || target == null) {
            throw new IllegalArgumentException
                ("Attacker or target not valid");
        }
        
        // Non-mechs can't thrash.
        if (!(ae instanceof Mech)) {
            return new ToHitData(ToHitData.IMPOSSIBLE,
                                 "Only mechs can thrash at infantry");
        }

        // Mech must be prone.
        if ( !ae.isProne() ) {
            return new ToHitData(ToHitData.IMPOSSIBLE,
                                 "Only prone mechs can thrash at infantry");
        }
        
        // Can't thrash against non-infantry
        if ( te == null && !(te instanceof Infantry) ) {
            return new ToHitData(ToHitData.IMPOSSIBLE,
                                 "Can only thrash at infantry");
        }

        // Can't thrash against swarming infantry.
        else if ( te != null && Entity.NONE != te.getSwarmTargetId() ) {
            return new ToHitData( ToHitData.IMPOSSIBLE,
                                  "Can't thrash at swarming infantry" );
        }

        // Check range.
        if ( target.getPosition() == null ||
             ae.getPosition().distance(target.getPosition()) > 0 ) {
            return new ToHitData(ToHitData.IMPOSSIBLE,
                                 "Target not in same hex");
        }

        // Check terrain.
        Hex hex = game.board.getHex( ae.getPosition() );
        if ( hex.contains( Terrain.WOODS ) ||
       hex.contains( Terrain.ROUGH ) ||
       hex.contains( Terrain.RUBBLE ) ||
       hex.contains( Terrain.BUILDING ) ) {
            return new ToHitData( ToHitData.IMPOSSIBLE,
                                  "Not a clear or pavement hex." );
        }

        // Can't target woods or a building with a thrash attack.
        if ( target.getTargetType() == Targetable.TYPE_BUILDING ||
             target.getTargetType() == Targetable.TYPE_BLDG_IGNITE ||
             target.getTargetType() == Targetable.TYPE_HEX_CLEAR ||
             target.getTargetType() == Targetable.TYPE_HEX_IGNITE ) {
            return new ToHitData( ToHitData.IMPOSSIBLE, "Invalid attack");
        }

        // The Mech can't have fired a weapon this round.
        for ( int loop = 0; loop < ae.locations(); loop++ ) {
            if ( ae.weaponFiredFrom(loop) ) {
                return new ToHitData( ToHitData.IMPOSSIBLE,
                                      "Weapons fired from " + 
                                      ae.getLocationName(loop) +
                                      " this turn" );
            }
        }

        // Mech must have at least one arm or leg.
        if ( ae.isLocationDestroyed(Mech.LOC_RARM) &&
             ae.isLocationDestroyed(Mech.LOC_LARM) &&
             ae.isLocationDestroyed(Mech.LOC_RLEG) &&
             ae.isLocationDestroyed(Mech.LOC_LLEG) ) {
            return new ToHitData( ToHitData.IMPOSSIBLE,
                                  "Mech has no arms or legs to thrash" );
        }

        // If the attack isn't impossible, it's automatically successful.
        return new ToHitData( ToHitData.AUTOMATIC_SUCCESS,
                              "thrash attacks always hit" );
    }

    /**
     * Damage caused by a successfull thrashing attack.
     *
     * @param   entity - the <code>Entity</code> conducting the thrash attack.
     * @return  The <code>int</code> amount of damage caused by this attack.
     */
    public static int getThrashDamageFor( Entity entity ) {
        int nDamage = Math.round( entity.getWeight() / 3.0f );
        return nDamage;
    }

    /**
     * To-hit number for the specified arm to brush off swarming infantry.
     * If this attack misses, the Mek will suffer punch damage.  This same
     * action is used to remove iNARC pods.
     *
     * @param   game - the <code>Game</code> object containing all entities.
     * @param   act - the <code>BrushOffAttackAction</code> for the attack.
     * @return  the <code>ToHitData</code> containing the target roll.
     */
    public static ToHitData toHitBrushOff(Game game, BrushOffAttackAction act){
        return toHitBrushOff( game, act.getEntityId(),
                              game.getTarget(act.getTargetType(),
                                             act.getTargetId()),
                              act.getArm() );
    }

    /**
     * To-hit number for the specified arm to brush off swarming infantry.
     * If this attack misses, the Mek will suffer punch damage.  This same
     * action is used to remove iNARC pods.
     * <p/>
     * This version is kept for backwards compatability.
     *
     * @param   game - the <code>Game</code> object containing all entities.
     * @param   attackerId - the <code>int</code> ID of the attacking unit.
     * @param   targetId - the <code>int</code> ID of the attack's target.
     * @param   arm - the <code>int</code> of the arm making the attack;
     *          this value must be <code>BrushOffAttackAction.RIGHT</code>
     *          or <code>BrushOffAttackAction.LEFT</code>.
     * @return  the <code>ToHitData</code> containing the target roll.
     */
    public static ToHitData toHitBrushOff(Game game, int attackerId, 
                                          int targetId, int arm) {
        return toHitBrushOff( game, attackerId, game.getEntity(targetId), arm );
    }

    /**
     * To-hit number for the specified arm to brush off swarming infantry.
     * If this attack misses, the Mek will suffer punch damage.  This same
     * action is used to remove iNARC pods.
     *
     * @param   game - the <code>Game</code> object containing all entities.
     * @param   attackerId - the <code>int</code> ID of the attacking unit.
     * @param   target - the <code>Targetable</code> object being targeted.
     * @param   arm - the <code>int</code> of the arm making the attack;
     *          this value must be <code>BrushOffAttackAction.RIGHT</code>
     *          or <code>BrushOffAttackAction.LEFT</code>.
     * @return  the <code>ToHitData</code> containing the target roll.
     */
    public static ToHitData toHitBrushOff(Game game, int attackerId,
                                          Targetable target, int arm) {
        final Entity ae = game.getEntity(attackerId);
        int targetId = Entity.NONE;
        Entity te = null;
        if ( target.getTargetType() == Targetable.TYPE_ENTITY ) {
            te = (Entity) target;
            targetId = target.getTargetId();
        }
        final int armLoc = (arm == BrushOffAttackAction.RIGHT)
                           ? Mech.LOC_RARM : Mech.LOC_LARM;
        ToHitData toHit;

        // non-mechs can't BrushOff
        if (!(ae instanceof Mech)) {
            return new ToHitData(ToHitData.IMPOSSIBLE,
                                 "Only mechs can brush off swarming infantry");
        }

        // arguments legal?
        if ( arm != BrushOffAttackAction.RIGHT &&
             arm != BrushOffAttackAction.LEFT ) {
            throw new IllegalArgumentException("Arm must be LEFT or RIGHT");
        }
        if (ae == null || target == null) {
            throw new IllegalArgumentException("Attacker or target not valid");
        }
        if ( targetId != ae.getSwarmAttackerId() ||
             te == null || !(te instanceof Infantry) ) {
            return new ToHitData(ToHitData.IMPOSSIBLE,
                                 "Can only brush off swarming infantry" );
        }

        // Quads can't brush off.
        if ( ae.entityIsQuad() ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Attacker is a quad");
        }

        // Can't brush off with flipped arms
        if (ae.getArmsFlipped()) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Arms are flipped to the rear. Can not punch.");
        }

        // check if arm is present
        if (ae.isLocationDestroyed(armLoc)) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Arm missing");
        }
        
        // check if shoulder is functional
        if (!ae.hasWorkingSystem(Mech.ACTUATOR_SHOULDER, armLoc)) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Shoulder destroyed");
        }  
        
        // check if attacker has fired arm-mounted weapons
        if (ae.weaponFiredFrom(armLoc)) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Weapons fired from arm this turn");
        }

        // can't physically attack mechs making dfa attacks
        if ( te != null && te.isMakingDfa() ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target is making a DFA attack");
        }
        
        // Can't brush off while prone.
        if (ae.isProne()) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Attacker is prone");
        }

        // Can't target woods or a building with a brush off attack.
        if ( target.getTargetType() == Targetable.TYPE_BUILDING ||
             target.getTargetType() == Targetable.TYPE_BLDG_IGNITE ||
             target.getTargetType() == Targetable.TYPE_HEX_CLEAR ||
             target.getTargetType() == Targetable.TYPE_HEX_IGNITE ) {
            return new ToHitData( ToHitData.IMPOSSIBLE, "Invalid attack");
        }
        
        // okay, modifiers...
        toHit = new ToHitData(ae.getCrew().getPiloting(), "base PSR");
        toHit.addModifier( 4, "brush off swarming infantry" );
        
        // damaged or missing actuators
        if (!ae.hasWorkingSystem(Mech.ACTUATOR_UPPER_ARM, armLoc)) {
            toHit.addModifier(2, "Upper arm actuator destroyed");
        }
        if (!ae.hasWorkingSystem(Mech.ACTUATOR_LOWER_ARM, armLoc)) {
            toHit.addModifier(2, "Lower arm actuator missing or destroyed");
        }
        if (!ae.hasWorkingSystem(Mech.ACTUATOR_HAND, armLoc)) {
            toHit.addModifier(1, "Hand actuator missing or destroyed");
        }
        
        modifyPhysicalBTHForAdvantages(ae, te, toHit);
        
        // If the target has Assault claws, give a 1 modifier.
        // We can stop looking when we find our first match.
        for ( Enumeration iter = te.getMisc(); iter.hasMoreElements(); ) {
            Mounted mount = (Mounted) iter.nextElement();
            EquipmentType equip = mount.getType();
            if ( BattleArmor.ASSAULT_CLAW.equals
                 (equip.getInternalName()) ) {
                toHit.addModifier( 1, "defender has assault claws" );
                break;
            }
        }

        // done!
        return toHit;
    }

    /**
     * Damage that the specified mech does with a brush off attack.
     * This equals the damage done by a punch from the same arm.
     *
     * @param   entity - the <code>Entity</code> brushing off the swarm.
     * @param   arm - the <code>int</code> of the arm making the attack;
     *          this value must be <code>BrushOffAttackAction.RIGHT</code>
     *          or <code>BrushOffAttackAction.LEFT</code>.
     * @return  the <code>int</code> amount of damage caused by the attack.
     *          If the attack hits, the swarming infantry takes the damage;
     *          if the attack misses, the entity deals the damage to themself.
     */
    public static int getBrushOffDamageFor(Entity entity, int arm) {
        return getPunchDamageFor( entity, arm );
    }

    /**
     * Can movement between the two coordinates be on pavement (which includes
     * roads and bridges)?  If so it will override prohibited terrain, it may
     * change movement costs, and it may lead to skids.
     *
     * @param   game - the <code>Game</code> object.
     * @param   src - the <code>Coords</code> being left.
     * @param   dest - the <code>Coords</code> being entered.
     * @return  <code>true</code> if movement between <code>src</code> and
     *          <code>dest</code> can be on pavement; <code>false</code>
     *          otherwise.
     */
    public static boolean canMoveOnPavement( Game game,
                                             Coords src, Coords dest ) {
        final Hex srcHex = game.board.getHex(src);
        final Hex destHex = game.board.getHex(dest);
        final int src2destDir = src.direction1(dest);
        final int dest2srcDir = (src2destDir + 3) % 6;
        boolean result = false;

        // We may be moving in the same hex.
        if ( src.equals(dest) &&
             ( srcHex.contains(Terrain.PAVEMENT) ||
               srcHex.contains(Terrain.ROAD) ||
               srcHex.contains(Terrain.BRIDGE) ) ) {
            result = true;
        }

        // If the source is a pavement hex, then see if the destination
        // hex is also a pavement hex or has a road or bridge that exits
        // into the source hex.
        else if ( srcHex.contains(Terrain.PAVEMENT) &&
             ( destHex.contains(Terrain.PAVEMENT) ||
               destHex.containsTerrainExit(Terrain.ROAD, dest2srcDir) ||
               destHex.containsTerrainExit(Terrain.BRIDGE, dest2srcDir) ) ) {
            result = true;
        }

        // See if the source hex has a road or bridge that exits into the
        // destination hex.
        else if ( srcHex.containsTerrainExit(Terrain.ROAD, src2destDir) ||
                  srcHex.containsTerrainExit(Terrain.BRIDGE, src2destDir) ) {
            result = true;
        }

        return result;
    }

    /**
     * Determine if the passed numer is odd.
     *
     * @param   number - the <code>int</code> to be checked.
     * @return  <code>true</code> if the number is odd, <code>false</code> if
     *          it is even.
     */
    public static boolean isOdd( int number ) {
        return ( (number & 1) == 1 );
    }

  /**
   * Determines whether the attacker and the target are in the same
   * building.  
   * @return true if the target can and does occupy the same building,
   * false otherwise. 
   */
  public static boolean isInSameBuilding(Game game, Entity attacker, Targetable target) {
    if (!(target instanceof Entity)) {
      return false;
    }
    Entity targetEntity = (Entity)target;
    if (!isInBuilding(game, attacker) || !isInBuilding(game, targetEntity)) {
      return false;
    }
    
    Building attkBldg = game.board.getBuildingAt(attacker.getPosition());
    Building targBldg = game.board.getBuildingAt(target.getPosition());
    
    return attkBldg.equals(targBldg);
  }

    /**
     * Determine if the given unit is inside of a building at the given
     * coordinates.
     *
     * @param   game - the <code>Game</code> object.
     *          This value may be <code>null</code>.
     * @param   entity - the <code>Entity</code> to be checked.
     *          This value may be <code>null</code>.
     * @return  <code>true</code> if the entity is inside of the building
     *          at those coordinates.  <code>false</code> if there is no
     *          building at those coordinates or if the entity is on the
     *          roof or in the air above the building, or if any input
     *          argument is <code>null</code>.
     */
    public static boolean isInBuilding( Game game,
                                        Entity entity ) {

        // No game, no building.
        if ( game == null ) {
            return false;
        }

        // Null entities can't be in a building.
        if ( entity == null ) {
            return false;
        }

        // Call the version of the function that requires coordinates.
        return isInBuilding( game, entity, entity.getPosition() );
    }

    /**
     * Determine if the given unit is inside of a building at the given
     * coordinates.
     *
     * @param   game - the <code>Game</code> object.
     *          This value may be <code>null</code>.
     * @param   entity - the <code>Entity</code> to be checked.
     *          This value may be <code>null</code>.
     * @param   coords - the <code>Coords</code> of the building hex.
     *          This value may be <code>null</code>.
     * @return  <code>true</code> if the entity is inside of the building
     *          at those coordinates.  <code>false</code> if there is no
     *          building at those coordinates or if the entity is on the
     *          roof or in the air above the building, or if any input
     *          argument is <code>null</code>.
     */
    public static boolean isInBuilding( Game game,
                                        Entity entity,
                                        Coords coords ) {

        // No game, no building.
        if ( game == null ) {
            return false;
        }

        // Null entities can't be in a building.
        if ( entity == null ) {
            return false;
        }

        // Null coordinates can't have buildings.
        if ( coords == null ) {
            return false;
        }

        // Get the Hex at those coordinates.
        final Hex curHex = game.board.getHex( coords );

        // The entity can't be inside of a building that isn't there.
        if ( !curHex.contains( Terrain.BLDG_ELEV ) ) {
            return false;
        }

        // Get the elevations occupied by the building.
        int surface = curHex.surface();
        int bldgHeight = curHex.levelOf( Terrain.BLDG_ELEV );
        int basement = 0;
        if ( curHex.contains( Terrain.BLDG_BASEMENT ) ) {
            basement = curHex.levelOf( Terrain.BLDG_BASEMENT );
        }

        // Get the elevation occupied by the entity in that hex.
        int entityElev = entity.elevationOccupied( curHex );

        // Return true if the entity is in the range of building elevations.
        if ( entityElev >= (surface - basement) &&
             entityElev < (surface + bldgHeight) ) {
            return true;
        }

        // Entity is not *inside* of the building.
        return false;
    }
    
    public static Coords scatter(Coords coords) {
    	int scatterDirection = d6(1) - 1;
    	int scatterDistance = d6(1);
    	
    	for (int i = 0; i < scatterDistance; i++) {
    		coords = coords.translated(scatterDirection);
    	}
    	return coords;
    }

    // added by kenn
    /**
     * Returns a LosEffects object representing the LOS effects of interveing
     * terrain between the attacker and target.
     *
     * Checks to see if the attacker and target are at an angle where the LOS
     * line will pass between two hexes.  If so, calls losDivided, otherwise
     * calls losStraight.
     */
    public static LosEffects calculateLosTheoretical(Game game, Coords attCoord, Coords targCoord, int attEl, int targEl) {
        // good time to ensure hex cache
        IdealHex.ensureCacheSize(game.board.width + 1, game.board.height + 1);

        double degree = attCoord.degree(targCoord);
        if (degree % 60 == 30) {
            return losDividedTheoretical(game, attCoord, targCoord, attEl, targEl);
        } else {
            return losStraightTheoretical(game, attCoord, targCoord, attEl, targEl);
        }
    }

    /**
     * Returns LosEffects for a line that never passes exactly between two
     * hexes.  Since intervening() returns all the coordinates, we just
     * add the effects of all those hexes.
     */
    public static LosEffects losStraightTheoretical(Game game, Coords attCoord, Coords targCoord, int attHeight, int targHeight) {
        Coords[] in = intervening(attCoord, targCoord);
        LosEffects los = new LosEffects();

        for (int i = 0; i < in.length; i++) {
            los.add( losForCoordsTheoretical(game, attCoord, targCoord, attHeight, targHeight, in[i]) );
        }
        return los;
    }

    /**
     * Returns LosEffects for a line that passes between two hexes at least
     * once.  The rules say that this situation is resolved in favor of the
     * defender.
     *
     * The intervening() function returns both hexes in these circumstances,
     * and, when they are in line order, it's not hard to figure out which hexes
     * are split and which are not.
     *
     * The line always looks like:
     *       ___     ___
     *   ___/ 1 \___/...\___
     *  / 0 \___/ 3 \___/etc\
     *  \___/ 2 \___/...\___/
     *      \___/   \___/
     *
     * We go thru and figure out the modifiers for the non-split hexes first.
     * Then we go to each of the two split hexes and determine which gives us
     * the bigger modifier.  We use the bigger modifier.
     *
     * This is not perfect as it takes partial cover as soon as it can, when
     * perhaps later might be better.
     * Also, it doesn't account for the fact that attacker partial cover blocks
     * leg weapons, as we want to return the same sequence regardless of
     * what weapon is attacking.
     */
    public static LosEffects losDividedTheoretical(Game game, Coords attCoord, Coords targCoord, int attHeight, int targHeight) {
        Coords[] in = intervening(attCoord, targCoord);
        LosEffects los = new LosEffects();

        int attEl = attHeight + game.board.getHex(attCoord).getElevation();
        int targEl = targHeight + game.board.getHex(targCoord).getElevation();
        final boolean isElevDiff =
            ( attEl != targEl );

        // add non-divided line segments
        for (int i = 3; i < in.length - 2; i += 3) {
            los.add( losForCoordsTheoretical(game, attCoord, targCoord, attHeight, targHeight, in[i]) );
        }

        // if blocked already, return that
        if (losModifiers(los).getValue() == ToHitData.IMPOSSIBLE) {
            return los;
        }

        // go through divided line segments
        for (int i = 1; i < in.length - 2; i += 3) {
            // get effects of each side
            LosEffects left = losForCoordsTheoretical( game, attCoord, targCoord, attHeight, targHeight, in[i]);
            LosEffects right = losForCoordsTheoretical( game, attCoord, targCoord, attHeight, targHeight, in[i+1]);

            // Include all previous LOS effects.
            left.add(los);
            right.add(los);

            // which is better?
            int lVal = losModifiers(left).getValue();
            int rVal = losModifiers(right).getValue();
            if (lVal > rVal || (lVal == rVal && left.isAttackerCover())) {
                los = left;
            } else {
                los = right;
            }
        }

        return los;
    }

    /**
     * Returns a LosEffects object representing the LOS effects of anything at
     * the specified coordinate.
     */
    private static LosEffects losForCoordsTheoretical(Game game, Coords attCoord, Coords targCoord,
                                                      int attHeight, int targHeight, Coords coords) {
        LosEffects los = new LosEffects();
        // ignore hexes not on board
        if (!game.board.contains(coords)) {
            return los;
        }

        // ignore hexes the attacker or target are in
        if ( coords.equals(attCoord) ||
             coords.equals(targCoord) ) {
            return los;
        }

        int attEl = attHeight + game.board.getHex(attCoord).getElevation();
        int targEl = targHeight + game.board.getHex(targCoord).getElevation();

        Hex hex = game.board.getHex(coords);
        int hexEl = hex.surface();

        // Handle building elevation.
        // Attacks thru a building are not blocked by that building.
        // ASSUMPTION: bridges don't block LOS.
        int bldgEl = 0;
        if ( null == los.getThruBldg() &&
             hex.contains( Terrain.BLDG_ELEV ) ) {
            bldgEl = hex.levelOf( Terrain.BLDG_ELEV );
        }

        // TODO: Identify when LOS travels *above* a building's hex.
        //       Alternatively, force all building hexes to be same height.

        // check for block by terrain
        if ((hexEl + bldgEl > attEl && hexEl + bldgEl > targEl)
            || (hexEl + bldgEl > attEl && attCoord.distance(coords) == 1)
            || (hexEl + bldgEl > targEl && targCoord.distance(coords) == 1)) {
            los.blocked = true;
        }

        // check for woods or smoke
        if ((hexEl + 2 > attEl && hexEl + 2 > targEl)
        || (hexEl + 2 > attEl && attCoord.distance(coords) == 1)
        || (hexEl + 2 > targEl && targCoord.distance(coords) == 1)) {
            // smoke overrides any woods in the hex
            if (hex.contains(Terrain.SMOKE)) {
                los.smoke++;
            } else if (hex.levelOf(Terrain.WOODS) == 1) {
                los.lightWoods++;
            } else if (hex.levelOf(Terrain.WOODS) > 1) {
                los.heavyWoods++;
            }
        }

        // check for target partial cover
        if ( targCoord.distance(coords) == 1 &&
             hexEl == targEl &&
             attEl <= targEl && targHeight > 0) {
            los.targetCover = true;
        }

        // check for attacker partial cover
        if (attCoord.distance(coords) == 1 &&
            hexEl == attEl &&
            attEl >= targEl && attHeight > 0) {
            los.attackerCover = true;
        }

        return los;
    }
    // end kenn

} // End public class Compute
