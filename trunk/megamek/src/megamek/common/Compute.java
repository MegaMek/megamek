/*
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

import java.awt.Polygon;
import java.util.*;

import megamek.common.actions.*;
import megamek.common.util.*;

/**
 * The compute class is designed to provide static methods for mechs
 * and other entities moving, firing, etc.
 */
public class Compute
{
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

    /**
     * Move the RNG out of the public interface.  Start with a good RNG
     * and try to get a cryptographically strong one to replace it.
     * Please note, there have been many complaints about the default RNG,
     * but NO ONE has been able to demonstrate a bias with more than
     * anecdotal evidence.  Still, the people are demanding a change.
     */
    private static final com.sun.java.util.collections.Random random = new com.sun.java.util.collections.Random();
    private static StrongRNG strongRNG = null;
    static {
        // Initialize the cryptographically strong RNG in its own thread.
        Thread makeRNG = new Thread( new Runnable() {
                public void run() {
                    try {
                        Date start = new Date();
                        StrongRNG newRNG = new StrongRNG();
                        Date end = new Date();
                        Compute.strongRNG = newRNG;
                        Compute.d6(); // Initialize the RNG.
                        System.out.println( "Using a cryptographic RNG." );System.out.flush();//killme
                    }
                    catch ( Exception exp ) {
                        // Report it.
                        System.err.println
                            ( "Compute could not generate a stong RNG." );
                    }
                }
            } );
        makeRNG.start();
    }

    /**
     * Provide a local sub-class of a cryptographically strong RNG.  We need
     * this to gain access to the protected <code>next(int)</code> method.
     */
    private static class MyCryptoRNG extends java.security.SecureRandom {
        public MyCryptoRNG() { super(); }
        public int myNext( int input ) { return next(input); }
    } // End private static class MyCryptoRNG

    /**
     * Provide a wrapper for a cryptographically strong RNG that uses the
     * same methods as the default RNG.
     */
    private static class StrongRNG
        extends com.sun.java.util.collections.Random {
        // The cryptographic RNG that will produce our random values.
        private MyCryptoRNG crypto = null;

        // Default constructor.  Create the cryptographic RNG.
        public StrongRNG() { super(); crypto = new MyCryptoRNG(); }

        // Redirect all calls for new values to the cryptographic RNG.
        protected synchronized int next( int input ) {
            return crypto.myNext( input );
        }
    } // End private static class StrongRNG
    
    /**
     * Simulates six-sided die rolls.
     */
    public static int d6(int dice) {
        int total = 0;
        for (int i = 0; i < dice; i++) {
            total += randomInt(6) + 1;
        }
        return total;
    }
    
    /**
     * A single die
     */
    public static int d6() {
        return d6(1);
    }

    /**
     * Returns a random <code>int</code> in the range from 0 to one
     * less than the supplied max value.
     *
     * @param   maxValue - the smallest <code>int</code> value which
     *          will exceed any random number returned by this method.
     * @return  a random <code>int</code> from the value set [0, maxValue).
     */
    public static int randomInt( int maxValue ) {
        int result = -1;
        // Use the strong RNG if we have one.
        if ( null != strongRNG ) {
            result = strongRNG.nextInt( maxValue );
        } else {
            result = random.nextInt( maxValue );
        }
        return result;
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
     * Generates a MovementData to rotate the entity to it's new facing
     */
    public static MovementData rotatePathfinder(Game game, int entityId, 
                                                int destFacing) {
        final Entity entity = game.getEntity(entityId);
        return rotatePathfinder(entity.getFacing(), destFacing);
    }
    
    /**
     * Generates a MovementData object to rotate from the start facing to the
     * destination facing.
     */
    public static MovementData rotatePathfinder(int facing, int destFacing) {
        MovementData md = new MovementData();

        // adjust facing
        while (facing != destFacing) {
            int stepType = MovementData.getDirection(facing, destFacing);
            md.addStep(stepType);
            facing = MovementData.getAdjustedFacing(facing, stepType);
        }
        
        return md;
    }
    
    /**
     * Generates MovementData for a mech to move from its current position
     * to the destination.
     */
    public static MovementData lazyPathfinder(Game game, int entityId, 
                                                  Coords dest) {
        final Entity entity = game.getEntity(entityId);
        return lazyPathfinder(entity.getPosition(), entity.getFacing(), dest);
    }
    
    /**
     * Generates MovementData for a mech to move from the start position and
     * facing to the destination
     */
    public static MovementData lazyPathfinder(Coords src, int facing, Coords dest) {
        MovementData md = new MovementData();
        
        int curFacing = facing;
        Coords curPos = new Coords(src);
        
        while(!curPos.equals(dest)) {
            // adjust facing
            md.append(rotatePathfinder(curFacing, curPos.direction1(dest)));
            // step forwards
            md.addStep(MovementData.STEP_FORWARDS);

            curFacing = curPos.direction1(dest);
            curPos = curPos.translated(curFacing);
        }
        
        return md;
    }
    
    /**
     * Backwards walking pathfinder.  Note that this will let you do impossible
     * things, like run backwards.
     */
    public static MovementData backwardsLazyPathfinder(Coords src, int facing, Coords dest) {
        MovementData md = new MovementData();
        
        int curFacing = facing;
        Coords curPos = new Coords(src);
        
        while(!curPos.equals(dest)) {
            // adjust facing
            int destFacing = (curPos.direction1(dest) + 3) % 6;
            md.append(rotatePathfinder(curFacing, destFacing));
            
            // step backwards
            md.addStep(MovementData.STEP_BACKWARDS);

            curFacing = destFacing;
            curPos = curPos.translated((destFacing + 3) % 6);
        }
        
        return md;
    }
    
    /**
     * Charge pathfinder.  Finds a path up to the hex before the target,
     * then charges
     */
    public static MovementData chargeLazyPathfinder(Coords src, int facing,
                                                        Coords dest) {
        MovementData md = new MovementData();
        
        int curFacing = facing;
        Coords curPos = new Coords(src);
        
        Coords subDest = dest.translated(dest.direction1(src));
        
        while(!curPos.equals(subDest)) {
            // adjust facing
            md.append(rotatePathfinder(curFacing, curPos.direction1(subDest)));
            // step forwards
            md.addStep(MovementData.STEP_FORWARDS);

            curFacing = curPos.direction1(subDest);
            curPos = curPos.translated(curFacing);
        }
        
        // adjust facing
        md.append(rotatePathfinder(curFacing, curPos.direction1(dest)));
        // charge!
        md.addStep(MovementData.STEP_CHARGE);
        
        return md;
    }
    
    /**
     * Death from above pathfinder.  Finds a path up to the hex before the target,
     * then charges
     */
    public static MovementData dfaLazyPathfinder(Coords src, int facing,
                                                        Coords dest) {
        MovementData md = new MovementData();
        
        int curFacing = facing;
        Coords curPos = new Coords(src);
        
        Coords subDest = dest.translated(dest.direction1(src));
        
        while(!curPos.equals(subDest)) {
            // adjust facing
            md.append(rotatePathfinder(curFacing, curPos.direction1(subDest)));
            // step forwards
            md.addStep(MovementData.STEP_FORWARDS);

            curFacing = curPos.direction1(subDest);
            curPos = curPos.translated(curFacing);
        }
        
        // adjust facing
        md.append(rotatePathfinder(curFacing, curPos.direction1(dest)));
        // charge!
        md.addStep(MovementData.STEP_DFA);
        
        return md;
    }
    
    
    /**
     * "Compiles" some movement data by setting all the flags.  Determines which
     * steps are possible, how many movement points each uses, and where they
     * occur.
     */
    public static void compile(Game game, int entityId, MovementData md) {
        final Entity entity = game.getEntity(entityId);
        
        // some flags
        int curFacing = entity.getFacing();
        Coords lastPos;
        Coords curPos = new Coords(entity.getPosition());
        int mpUsed = entity.mpUsed;
        int distance = entity.delta_distance;
        boolean isProne = entity.isProne();
        boolean hasJustStood = false;
        boolean firstStep = true;
        boolean lastWasBackwards = false;
        boolean thisStepBackwards = false;
        int overallMoveType = Entity.MOVE_WALK;
        boolean isJumping = false;
        boolean isRunProhibited = false;
	boolean isInfantry = (entity instanceof Infantry);
	MovementData.Step prevStep = null;

        // check for jumping
        if (md.contains(MovementData.STEP_START_JUMP)) {
            overallMoveType = Entity.MOVE_JUMP;
            isJumping = true;
        }
        
        if (entity instanceof QuadMech && !isJumping) {
            md.transformLateralShifts();
            md.transformLateralShiftsBackwards();
        }

        // check for backwards movement
        if (md.contains(MovementData.STEP_BACKWARDS)
            || md.contains(MovementData.STEP_LATERAL_LEFT_BACKWARDS)
            || md.contains(MovementData.STEP_LATERAL_RIGHT_BACKWARDS)) {
            isRunProhibited = true;
        }
        
        // first pass: set position, facing and mpUsed; figure out overallMoveType
        for (final Enumeration i = md.getSteps(); i.hasMoreElements();) {
            final MovementData.Step step = (MovementData.Step)i.nextElement();
            
            int stepMp = 0;
            
            lastPos = new Coords(curPos);

            // 
            switch(step.getType()) {
	    case MovementData.STEP_UNLOAD:
		// TODO: Can immobilized transporters unload?
	    case MovementData.STEP_LOAD:
		stepMp = 1;
		break;
            case MovementData.STEP_TURN_LEFT :
            case MovementData.STEP_TURN_RIGHT :
		// Infantry can turn for free.
                stepMp = (isJumping || hasJustStood || isInfantry) ? 0 : 1;
                curFacing = MovementData.getAdjustedFacing(curFacing, step.getType());
                break;
            case MovementData.STEP_FORWARDS :
            case MovementData.STEP_BACKWARDS :
            case MovementData.STEP_CHARGE :
            case MovementData.STEP_DFA :
                // step forwards or backwards
                if (step.getType() == MovementData.STEP_BACKWARDS) {
                    curPos = curPos.translated((curFacing + 3) % 6);
                    thisStepBackwards = true;
                } else {
                    curPos = curPos.translated(curFacing);
                    thisStepBackwards = false;
                }

                stepMp = getMovementCostFor(game, entityId, lastPos, curPos,
                                            overallMoveType);
                // check for water
                if (game.board.getHex(curPos).levelOf(Terrain.WATER) > 0 
                        && entity.getMovementType() != Entity.MovementType.HOVER) {
                    isRunProhibited = true;
                }
                hasJustStood = false;
                if (lastWasBackwards != thisStepBackwards) {
                    distance = 0; //start over after shifting gears
                }
                distance += 1;
                break;
            case MovementData.STEP_LATERAL_LEFT :
            case MovementData.STEP_LATERAL_RIGHT :
            case MovementData.STEP_LATERAL_LEFT_BACKWARDS :
            case MovementData.STEP_LATERAL_RIGHT_BACKWARDS :
                if (step.getType() == MovementData.STEP_LATERAL_LEFT_BACKWARDS 
                    || step.getType() == MovementData.STEP_LATERAL_RIGHT_BACKWARDS) {
                    curPos = curPos.translated((MovementData.getAdjustedFacing(curFacing, MovementData.turnForLateralShiftBackwards(step.getType())) + 3) % 6);
                    thisStepBackwards = true;
                } else {
                    curPos = curPos.translated(MovementData.getAdjustedFacing(curFacing, MovementData.turnForLateralShift(step.getType())));
                    thisStepBackwards = false;
                }

                stepMp = getMovementCostFor(game, entityId, lastPos, curPos,
                                            overallMoveType) + 1;
                // check for water
                if (game.board.getHex(curPos).levelOf(Terrain.WATER) > 0) {
                    isRunProhibited = true;
                }
                hasJustStood = false;
                if (lastWasBackwards != thisStepBackwards) {  
                    distance = 0;  //start over after shifting gears
                }
                distance += 1;
                break;
            case MovementData.STEP_GET_UP :
                // mechs with 1 MP are allowed to get up
                stepMp = entity.getWalkMP() == 1 ? 1 : 2;
                hasJustStood = true;
                break;
            default :
                stepMp = 0;
            }
            
            mpUsed += stepMp;
            
            // check for running
            if (overallMoveType == Entity.MOVE_WALK 
                && mpUsed > entity.getWalkMP()) {
                overallMoveType = Entity.MOVE_RUN;
            }
            
            // set flags
            step.setPosition(curPos);
            step.setFacing(curFacing);
            step.setMpUsed(mpUsed);
            step.setDistance(distance);
            lastWasBackwards = thisStepBackwards;
        }
        
        // running with gyro or hip hit is dangerous!
        if (!isRunProhibited && overallMoveType == Entity.MOVE_RUN
            && (entity.getDestroyedCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_GYRO,Mech.LOC_CT) > 0
                || entity.hasHipCrit())) {
            md.getStep(0).setDanger(true);
        }
        
        // set moveType, illegal, trouble flags
        compileIllegal(game, entityId, md, overallMoveType, isRunProhibited);

        // check the last step for legality
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
    private static void compileIllegal( Game game, int entityId,
                                        MovementData md,
                                        int overallMoveType,
                                        boolean runProhibited ) {
        final Entity entity = game.getEntity(entityId);

        Coords curPos = new Coords(entity.getPosition());
        boolean legal = true;
        boolean danger = false;
        boolean pastDanger = false;
        boolean firstStep = true;
	boolean isInfantry = (entity instanceof Infantry);
        boolean isTurning = false;
        boolean isUnloaded = false;
	MovementData.Step prevStep = null;
        
        for (final Enumeration i = md.getSteps(); i.hasMoreElements();) {
            final MovementData.Step step = (MovementData.Step)i.nextElement();
            final int stepType = step.getType();
            
            Coords lastPos = new Coords(curPos);
            curPos = step.getPosition();
            
            // guilty until proven innocent
            int moveType = Entity.MOVE_ILLEGAL;
            
            // check for valid jump mp
            if (overallMoveType == Entity.MOVE_JUMP 
                && step.getMpUsed() <= entity.getJumpMPWithTerrain()
                && !entity.isProne()) {
                moveType = Entity.MOVE_JUMP;
            }
            
            // check for valid walk/run mp
            if ( (overallMoveType == Entity.MOVE_WALK ||
                  overallMoveType == Entity.MOVE_RUN)
                && (!entity.isProne() || md.contains(MovementData.STEP_GET_UP)
                    || stepType == MovementData.STEP_TURN_LEFT 
                    || stepType == MovementData.STEP_TURN_RIGHT)) {
                if (step.getMpUsed() <= entity.getWalkMP()) {
                    moveType = Entity.MOVE_WALK;
                } else if ( step.getMpUsed() <= entity.getRunMP() &&
                            !runProhibited ) {
                    moveType = Entity.MOVE_RUN;
                }
            }
            
            // mechs with 1 MP are allowed to get up
            if ( stepType == MovementData.STEP_GET_UP &&
                 entity.getWalkMP() == 1 ) {
                moveType = Entity.MOVE_RUN;
            }
            
            // amnesty for the first step
            if ( firstStep && moveType == Entity.MOVE_ILLEGAL &&
                 entity.getWalkMP() > 0 && !entity.isProne() &&
                 stepType == MovementData.STEP_FORWARDS ) {
                moveType = Entity.MOVE_RUN;
            }

            // Is the entity unloading passeners?
            if ( stepType == MovementData.STEP_UNLOAD ) {
                // Prone Meks are able to unload.
                if ( entity.isProne() && moveType == Entity.MOVE_ILLEGAL ) {
                    moveType = Entity.MOVE_WALK;
                }

                // Can't unload units into prohibited terrain
                // or into stacking violation.
                Entity other = step.getTarget( game );
                if ( null != stackingViolation(game, other.getId(), curPos)
                     || other.isHexProhibited(game.board.getHex(curPos)) ) {
                    moveType = Entity.MOVE_ILLEGAL;
                }
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
            // BUG: skid danger should really go on the prevStep, not on step.
            danger = step.isDanger();
            danger |= isPilotingSkillNeeded( game, entityId, lastPos, 
                                             curPos, moveType,
                                             isTurning, overallMoveType );
            
            // getting up is also danger
            if (stepType == MovementData.STEP_GET_UP) {
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
            case MovementData.STEP_TURN_LEFT :
            case MovementData.STEP_TURN_RIGHT :
                isTurning = true;
                break;
            case MovementData.STEP_UNLOAD:
                // Unloading must be the last step.
                isUnloaded = true;
                break;
            default:
                isTurning = false;
                break;
            }

            firstStep = false;

	    // Infantry can always move one hex in *any* direction.
	    if ( isInfantry && step.getMpUsed() == 0 ) {
		firstStep = true;
	    }

	    // Record the step just taken.
	    prevStep = step;
        }
    }
    
    /**
     * Check the last steps for stacking violations or other problems.
     * Step backwards until we get to the first (last) legal step.  Then check 
     * if we have a violation.  If we do, then that step's illegal.  Check the 
     * next step backwards.  Otherwise, stop checking.
     */
    private static void compileLastStep(Game game, int entityId, MovementData md) {
        final Entity entity = game.getEntity(entityId);
        
        for (int i = md.length() - 1; i >= 0; i--) {
            final MovementData.Step step = md.getStep(i);
            final Hex destHex = game.board.getHex(step.getPosition());
            
            // skip steps that are not the last step
            if (step.getMovementType() == Entity.MOVE_ILLEGAL) {
                continue;
            }
            
            // check for stacking violations
            final Entity violation = stackingViolation(game, entityId, step.getPosition());
            if (violation != null
                    && step.getType() != MovementData.STEP_CHARGE
                    && step.getType() != MovementData.STEP_DFA) {
                // can't move here
                step.setMovementType(Entity.MOVE_ILLEGAL);
                continue;
            }
            
            // check again for illegal terrain, in case of jumping
            if (entity.isHexProhibited(destHex)) {
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
	boolean isInfantry = entering instanceof Infantry;
	boolean isMech = entering instanceof Mech;
        Entity firstEntity = null;

	// Walk through the entities in the given hex.
        for (Enumeration i = game.getEntities(coords); i.hasMoreElements();) {
            final Entity inHex = (Entity)i.nextElement();

	    // Don't compare the entering entity to itself.
	    if ( !(inHex.equals(entering)) ) {

		// If the entering entity is a mech,
		// then any other mech in the hex is a violation.
		if ( isMech && (inHex instanceof Mech) ) {
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

	    } // End different-entity

	} // Check the next entity
        
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
    private static void compileJumpCheck(Game game, int entityId, MovementData md) {
        final Entity entity = game.getEntity(entityId);
        Coords start = entity.getPosition();
        Coords land = md.getStep(md.length() - 1).getPosition();
        int distance = start.distance(land);
        int mp = md.getStep(md.length() - 1).getMpUsed();
        
        if (distance < 1 || mp > distance) {
            // whole movement illegal
            for (Enumeration i = md.getSteps(); i.hasMoreElements();) {
                MovementData.Step step = (MovementData.Step)i.nextElement();
                step.setMovementType(Entity.MOVE_ILLEGAL);
            }
        }
    }
    
    /**
     * Amount of movement points required to move from start to dest
     */
    public static int getMovementCostFor(Game game, int entityId, 
                                             Coords src, Coords dest,
                                             int movementType) {
        final Entity entity = game.getEntity(entityId);
        final int moveType = entity.getMovementType();
        final Hex srcHex = game.board.getHex(src);
        final Hex destHex = game.board.getHex(dest);
	final boolean isInfantry = (entity instanceof Infantry);
        
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
        if (movementType == Entity.MOVE_JUMP) {
            return 1;
        }
        
        int mp = 1;
        
        // account for terrain
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
        
        // account for elevation?
        int nSrcEl = entity.elevationOccupied(srcHex);
        int nDestEl = entity.elevationOccupied(destHex);
        int nMove = entity.getMovementType();

        if (nSrcEl != nDestEl) {
            int delta_e = Math.abs(nSrcEl - nDestEl);
            
            // ground vehicles are charged double            
	    // HACK!!!  Infantry movement reuses Mech and Vehicle movement.
            if ( !isInfantry &&
		 (nMove == Entity.MovementType.TRACKED ||
		  nMove == Entity.MovementType.WHEELED ||
		  nMove == Entity.MovementType.HOVER) ) {
                delta_e *= 2;
            }
            mp += delta_e;
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
        // another easy check
        if (!game.board.contains(dest)) {
            return false;
        }

        // The entity is trying to load.  Check for a valid move.
        if ( stepType == MovementData.STEP_LOAD ) {

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
                    if ( !other.isSelectable() ) {
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
        if (bDumping && entityMoveType == Entity.MOVE_RUN) {
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
        if ((stepType == MovementData.STEP_BACKWARDS || stepType == MovementData.STEP_LATERAL_LEFT_BACKWARDS
            || stepType == MovementData.STEP_LATERAL_RIGHT_BACKWARDS) && nSrcEl != nDestEl) {
            return false;
        }
        // can't run into water (unless hovering)
        if (entityMoveType == Entity.MOVE_RUN 
            && nMove != Entity.MovementType.HOVER
            && destHex.levelOf(Terrain.WATER) > 0 && !firstStep) {
            return false;
        }
        
        // ugh, stacking checks.  well, maybe we're immune!
        if (entityMoveType != Entity.MOVE_JUMP
        && stepType != MovementData.STEP_CHARGE
        && stepType != MovementData.STEP_DFA) {
            // can't move a mech into a hex with an enemy mech
            if (entity instanceof Mech && isEnemyMechIn(game, entityId, dest)) {
                return false;
            }
            // can't move out of a hex with an enemy unit unless we started there
            if (!src.equals(entity.getPosition()) && isEnemyUnitIn(game, entityId, src)) {
                return false;
            }
            
        }

        // can't jump over too-high terrain
        if (entityMoveType == Entity.MOVE_JUMP
            && destHex.getElevation() 
               > (entity.elevation() +
                  entity.getJumpMPWithTerrain())) {
            return false;
        }
        
        // certain movement types have terrain restrictions
        if (entityMoveType != Entity.MOVE_JUMP 
        && entity.isHexProhibited(destHex)) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Returns true if there is a mech that is an enemy of the specified unit
     * in the specified hex
     */
    public static boolean isEnemyMechIn(Game game, int entityId, Coords coords) {
        Entity entity = game.getEntity(entityId);
        for (Enumeration i = game.getEntities(coords); i.hasMoreElements();) {
            final Entity inHex = (Entity)i.nextElement();
            if (inHex instanceof Mech && inHex.getOwner().isEnemyOf(entity.getOwner())) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Returns true if there is any unit that is an enemy of the specified unit
     * in the specified hex
     */
    public static boolean isEnemyUnitIn(Game game, int entityId, Coords coords) {
        Entity entity = game.getEntity(entityId);
        for (Enumeration i = game.getEntities(coords); i.hasMoreElements();) {
            final Entity inHex = (Entity)i.nextElement();
            if (inHex.getOwner().isEnemyOf(entity.getOwner())) {
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
						int overallMoveType) {
        final Entity entity = game.getEntity(entityId);
        final Hex srcHex = game.board.getHex(src);
        final Hex destHex = game.board.getHex(dest);
	final boolean isInfantry = ( entity instanceof Infantry );
        
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
        
        // check for water
        if (movementType != Entity.MOVE_JUMP
            && entity.getMovementType() != Entity.MovementType.HOVER
            && destHex.levelOf(Terrain.WATER) > 0) {
            return true;
        }

        // Check for skids.
	if ( movementType != Entity.MOVE_JUMP
	     && srcHex.contains(Terrain.PAVEMENT)
	     && overallMoveType == Entity.MOVE_RUN
             && isTurning
	     && !isInfantry ) {
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
     * Gets a valid displacement, preferably in the direction indicated.
     * 
     * @return valid displacement coords, or null if none
     */
    public static Coords getValidDisplacement(Game game, int entityId, 
                                              Coords src, int direction) {
        final Entity entity = game.getEntity(entityId);
        // check the surrounding hexes
        if (isValidDisplacement(game, entityId, src, direction)) {
            // direction already valid?  (probably)
            return src.translated(direction);
        } else if (isValidDisplacement(game, entityId, src, (direction + 1) % 6)) {
            // 1 right?
            return src.translated((direction + 1) % 6);
        } else if (isValidDisplacement(game, entityId, src, (direction + 5) % 6)) {
            // 1 left?
            return src.translated((direction + 5) % 6);
        } else if (isValidDisplacement(game, entityId, src, (direction + 2) % 6)) {
            // 2 right?
            return src.translated((direction + 2) % 6);
        } else if (isValidDisplacement(game, entityId, src, (direction + 4) % 6)) {
            // 2 left?
            return src.translated((direction + 4) % 6);
        } else if (isValidDisplacement(game, entityId, src, (direction + 3) % 6)) {
            // opposite?
            return src.translated((direction + 3) % 6);
        } else {
            // well, tried to accomodate you... too bad.
            return null;
        }
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
        // check the surrounding hexes
        if (isValidDisplacement(game, entityId, src, direction)) {
            // direction already valid?  (probably)
            return src.translated(direction);
        } else if (isValidDisplacement(game, entityId, src, (direction + 1) % 6)) {
            // 1 right?
            return src.translated((direction + 1) % 6);
        } else if (isValidDisplacement(game, entityId, src, (direction + 5) % 6)) {
            // 1 left?
            return src.translated((direction + 5) % 6);
        } else if (isValidDisplacement(game, entityId, src, (direction + 2) % 6)) {
            // 2 right?
            return src.translated((direction + 2) % 6);
        } else if (isValidDisplacement(game, entityId, src, (direction + 4) % 6)) {
            // 2 left?
            return src.translated((direction + 4) % 6);
        } else if (isValidDisplacement(game, entityId, src, (direction + 3) % 6)) {
            // opposite?
            return src.translated((direction + 3) % 6);
        } else {
            // well, tried to accomodate you... too bad.
            return null;
        }
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
        } else if (random.nextFloat() > 0.5) {
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
     * Returns an entity's base piloting skill roll needed
     */
    public static PilotingRollData getBasePilotingRoll(Game game, int entityId) {
        final Entity entity = game.getEntity(entityId);
        
        PilotingRollData roll;
        
        // gyro operational?
        if (entity.getDestroyedCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_GYRO, Mech.LOC_CT) > 1) {
            return new PilotingRollData(entityId, PilotingRollData.AUTOMATIC_FAIL, 3, "Gyro destroyed");
        }
        // both legs present?
        if ( entity instanceof BipedMech ) {
          if ( ((BipedMech)entity).countDestroyedLegs() == 2 )
            return new PilotingRollData(entityId, PilotingRollData.AUTOMATIC_FAIL, 10, "Both legs destroyed");
        } else if ( entity instanceof QuadMech ) {
          if ( ((QuadMech)entity).countDestroyedLegs() >= 3 )
            return new PilotingRollData(entityId, PilotingRollData.AUTOMATIC_FAIL, 10, ((Mech)entity).countDestroyedLegs() + " legs destroyed");
        }
        // entity shut down?
        if (entity.isShutDown()) {
            return new PilotingRollData(entityId, PilotingRollData.AUTOMATIC_FAIL, 3, "Reactor shut down");
        }
        // pilot awake?
        if (!entity.getCrew().isActive()) {
            return new PilotingRollData(entityId, PilotingRollData.IMPOSSIBLE, "Pilot unconcious");
        }
        
        // okay, let's figure out the stuff then
        roll = new PilotingRollData(entityId, entity.getCrew().getPiloting(), "Base piloting skill");
        
        //Let's see if we have a modifier to our piloting skill roll. We'll pass in the roll
        //object and adjust as necessary
          roll = entity.addEntityBonuses(roll);
        
        return roll;
    }
    
    public static ToHitData toHitWeapon(Game game, WeaponAttackAction waa, Vector prevAttacks) {
        return toHitWeapon(game, waa.getEntityId(), waa.getTargetId(), 
                           waa.getWeaponId(), prevAttacks);
    }
    
    /**
     * To-hit number for attacker firing a weapon at the target.
     * 
     * @param game          the game
     * @param attackerId    the attacker id number
     * @param targetId      the target id number
     * @param weaponId      the weapon id number
     */
    public static ToHitData toHitWeapon(Game game, int attackerId, 
                                        int targetId, int weaponId, 
                                        Vector prevAttacks) {
        final Entity ae = game.getEntity(attackerId);
        final Entity te = game.getEntity(targetId);
        final Mounted weapon = ae.getEquipment(weaponId);
        final WeaponType wtype = (WeaponType)weapon.getType();
        Coords[] in = intervening(ae.getPosition(), te.getPosition());
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
        
        ToHitData toHit = null;
        boolean pc = false; // partial cover
        boolean apc = false; // attacker partial cover
        
        // weapon operational?
        if (weapon.isDestroyed()) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Weapon not operational.");
        }
        
        // got ammo?
        if (usesAmmo && (ammo == null || ammo.getShotsLeft() == 0)) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Weapon out of ammo.");
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

        // weapon in arc?
        // TODO? : move to after range calculation?
        if (!isInArc(game, attackerId, weaponId, targetId)) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target not in arc.");
        }

        // Handle solo attack weapons.
        if ( wtype.hasFlag(WeaponType.F_SOLO_ATTACK) ) {
            for ( Enumeration i = prevAttacks.elements();
                  i.hasMoreElements(); ) {
                Object o = i.nextElement();
                if (!(o instanceof WeaponAttackAction)) {
                    continue;
                }
                WeaponAttackAction prevAttack = (WeaponAttackAction)o;
                if (prevAttack.getEntityId() == attackerId) {

                    // If the attacker fires another weapon, this attack fails.
                    if ( weaponId != prevAttack.getWeaponId() ) {
                        return new ToHitData(ToHitData.IMPOSSIBLE, "Other weapon attacks declared.");
                    }
                }
            }
        } // End current-weapon-is-solo
        
        int attEl = ae.elevation() + ae.height();
        int targEl = te.elevation() + te.height();
        
        //TODO: mech making DFA could be higher if DFA target hex is higher
        //      BMRr pg. 43, "attacking unit is considered to be in the air
        //      above the hex, standing on an elevation 1 level higher than
        //      the target hex or the elevation of the hex the attacker is
        //      in, whichever is higher."
        
        // check LOS
        LosEffects los = calculateLos(game, attackerId, targetId);
        ToHitData losMods = losModifiers(los);
        
        // if LOS is blocked, block the shot
        if (losMods.getValue() == ToHitData.IMPOSSIBLE) {
            return losMods;
        }

        // attacker partial cover means no leg weapons
        if (los.attackerCover && ae.locationIsLeg(weapon.getLocation())) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Nearby terrain blocks leg weapons.");
        }

        // Leg attacks, Swarm attacks, and
        // Mine Launchers don't use gunnery.
        if ( Infantry.LEG_ATTACK.equals( wtype.getInternalName() ) ) {
            toHit = Compute.getLegAttackBaseToHit( ae, te );
        }
        else if ( Infantry.SWARM_MEK.equals( wtype.getInternalName() ) ) {
            toHit = Compute.getSwarmMekBaseToHit( ae, te );
            // TODO : implement SwarmMek
            return new ToHitData(ToHitData.IMPOSSIBLE, "Not implemented yet.");
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
        else {
            toHit = new ToHitData(ae.crew.getGunnery(), "gunnery skill");
        }
        
        // determine range
        final int range = ae.getPosition().distance(te.getPosition());
        // if out of range, short circuit logic

        int c3range = range;
        Entity c3spotter = ae;
        if (ae.hasC3() || ae.hasC3i()) {
            for (java.util.Enumeration i = game.getEntities(); i.hasMoreElements();) {
                final Entity fe = (Entity)i.nextElement();
                if (ae.onSameC3NetworkAs(fe) && canSee(game, fe, te)) {
                    final int buddyrange = fe.getPosition().distance(te.getPosition());
                    if(buddyrange < c3range) {
                        c3range = buddyrange;
                        c3spotter = fe;
                    }
                }
            }
        }

        // if out of range, short circuit logic
        if (range > wtype.getLongRange()) {
            return new ToHitData(ToHitData.AUTOMATIC_FAIL, "Target out of range");
        }
        if (range > wtype.getMediumRange()) {
            // Infantry LRMs suffer an additional maximum range penaltie.
            if ( isWeaponInfantry &&
                 wtype.getAmmoType() == AmmoType.T_LRM &&
		 range == wtype.getLongRange() ) {
                toHit.addModifier(5, "infantry LRM maximum range");
            } else {
		// long range, add +4
		toHit.addModifier(4, "long range");
            }
            // reduce range modifier back to short if a C3 spotter is at short range
            if (c3range <= wtype.getShortRange()) {
                toHit.addModifier(-4, "c3: " + c3spotter.getDisplayName() +
                                  " at short range");
                if ( te.isStealthActive() ) {
                    toHit.append( te.getStealthModifier(Entity.RANGE_SHORT) );
                }
            }
            // reduce range modifier back to medium if a C3 spotter is at medium range
            else if (c3range <= wtype.getMediumRange()) {
                toHit.addModifier(-2, "c3: " + c3spotter.getDisplayName() +
                                  " at medium range");
                if ( te.isStealthActive() ) {
                    toHit.append( te.getStealthModifier(Entity.RANGE_MEDIUM) );
                }
            }
            else if ( te.isStealthActive() ) {
                toHit.append( te.getStealthModifier(Entity.RANGE_LONG) );
            }
        } else if (range > wtype.getShortRange()) {
            // medium range, add +2
            toHit.addModifier(2, "medium range");
            // reduce range modifier back to short if a C3 spotter is at short range
            if (c3range <= wtype.getShortRange()) {
                toHit.addModifier(-2, "c3: " + c3spotter.getDisplayName() +
                                  " at short range");
                if ( te.isStealthActive() ) {
                    toHit.append( te.getStealthModifier(Entity.RANGE_SHORT) );
                }
            }
            else if ( te.isStealthActive() ) {
                toHit.append( te.getStealthModifier(Entity.RANGE_MEDIUM) );
            }


	} else {
            // short range

            if ( 0 == range ) {
                // Only Infantry shoot at zero range.
                if ( !isAttackerInfantry ) {
                    return new ToHitData(ToHitData.IMPOSSIBLE, "Only Infantry shoot at zero range.");
                }

                // Only infantry weapons get zero-range attack bonuses.
                if ( isWeaponInfantry ) {

                    // Infantry platoons attacking with infantry weapons can attack
                    // in the same hex with a base of 2, except for flamers and
                    // SRMs, which have a base of 3 and LRMs, which suffer badly. 
                    if ( wtype.hasFlag(WeaponType.F_FLAMER) ) {
                        toHit.addModifier(-1, "infantry flamer assault");
                    } else if ( wtype.getAmmoType() == AmmoType.T_SRM ) {
                        toHit.addModifier(-1, "infantry SRM assault");
                    } else if ( isWeaponInfantry &&
                                wtype.getAmmoType() != AmmoType.T_LRM ) {
                        toHit.addModifier(-2, "infantry assault");
                    }

                } // End infantry-weapon

            } // End zero-range
            if ( te.isStealthActive() ) {
                toHit.append( te.getStealthModifier(Entity.RANGE_SHORT) );
            }

        } // End short-range

        // also check for minimum range
        if (range <= wtype.getMinimumRange()) {
            int minPenalty = wtype.getMinimumRange() - range + 1;
            // Infantry LRMs suffer double minimum range penalties.
            if ( isWeaponInfantry &&
                 wtype.getAmmoType() == AmmoType.T_LRM ) {
                toHit.addModifier(minPenalty*2, "infantry LRM minumum range");
            } else {
                toHit.addModifier(minPenalty, "minumum range");
            }
        }

        // Battle Armor targets are hard for Meks and Tanks to hit.
        if ( !isAttackerInfantry && te instanceof BattleArmor ) {
            toHit.addModifier( 1, "battle armor target" );
        }

        // attacker movement
        toHit.append(getAttackerMovementModifier(game, attackerId));
        
        // target movement
        toHit.append(getTargetMovementModifier(game, targetId));
        
        // attacker terrain
        toHit.append(getAttackerTerrainModifier(game, attackerId));
        
        // attacker in water?
        Hex attHex = game.board.getHex(ae.getPosition());
        if (attHex.contains(Terrain.WATER) && attHex.surface() > attEl) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Attacker underwater.");
        }
        
        // target terrain
        toHit.append(getTargetTerrainModifier(game, targetId));
        
        // target in water?
        Hex targHex = game.board.getHex(te.getPosition());
        if (targHex.contains(Terrain.WATER)) {
            if (targHex.surface() == targEl && te.height() > 0) {
                // force partial cover
                los.targetCover = true;
                losMods = losModifiers(los);
            } else if (targHex.surface() > targEl) {
                return new ToHitData(ToHitData.IMPOSSIBLE, "Target underwater.");
            }
        }

        // add in LOS mods that we've been keeping
        toHit.append(losMods);
        
        // secondary targets modifier...
        int primaryTarget = Entity.NONE;
        boolean curInFrontArc = isInArc(ae.getPosition(), ae.getSecondaryFacing(), te.getPosition(), ARC_FORWARD);
        for (Enumeration i = prevAttacks.elements(); i.hasMoreElements();) {
            Object o = i.nextElement();
            if (!(o instanceof WeaponAttackAction)) {
                continue;
            }
            WeaponAttackAction prevAttack = (WeaponAttackAction)o;
            if (prevAttack.getEntityId() == attackerId) {
                // first front arc target is our primary.
                // if first target is non-front, and either a later target or
                // the current one is in front, use that instead.
                Entity pte = game.getEntity(prevAttack.getTargetId());
                if (isInArc(ae.getPosition(), ae.getSecondaryFacing(), pte.getPosition(), ARC_FORWARD)) {
                    primaryTarget = prevAttack.getTargetId();
                    break;
                } else if (primaryTarget == Entity.NONE && !curInFrontArc) {
                    primaryTarget = prevAttack.getTargetId();
                }
            }
        }
        
        if (primaryTarget != Entity.NONE && primaryTarget != targetId) {

            // Infantry can't attack secondary targets (BMRr, pg. 32).
            if ( isAttackerInfantry ) {
                return new ToHitData( ToHitData.IMPOSSIBLE, 
                                      "Can't have multiple targets." );
            }

            if (curInFrontArc) {
                toHit.addModifier(1, "secondary target modifier");
            } else {
                toHit.addModifier(2, "secondary target modifier");
            }
        }

        // heat
        if (ae.getHeatFiringModifier() != 0) {
            toHit.addModifier(ae.getHeatFiringModifier(), "heat");
        }

        // arm critical hits to attacker
        if (ae.getDestroyedCriticals(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_SHOULDER, weapon.getLocation()) > 0) {
            toHit.addModifier(4, "shoulder actuator destroyed");
        } else {
            int actuatorHits = 0;
            if (ae.getDestroyedCriticals(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_UPPER_ARM, weapon.getLocation()) > 0) {
                actuatorHits++;
            }
            if (ae.getDestroyedCriticals(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_LOWER_ARM, weapon.getLocation()) > 0) {
                actuatorHits++;
            }
            if (actuatorHits > 0) {
                toHit.addModifier(actuatorHits, actuatorHits + " destroyed arm actuators");
            }
        }
        
        // sensors critical hit to attacker
        if (sensorHits > 0) {
            toHit.addModifier(2, "attacker sensors damaged");
        }
        
        // target immobile
        if (te.isImmobile()) {
            toHit.addModifier(-4, "target immobile");
        }
        
        // attacker prone
        if (ae.isProne()) {
            if ( ae.entityIsQuad() ) {
                int legsDead = ((Mech)ae).countDestroyedLegs();
                
                //No legs destroyed: no penalty and can fire all weapons
                if ( (legsDead == 1) || (legsDead == 2) ) {
                    // Need an intact front leg
                    if (ae.isLocationDestroyed(Mech.LOC_RARM) && ae.isLocationDestroyed(Mech.LOC_LARM)) {
                        return new ToHitData(ToHitData.IMPOSSIBLE, "Prone with both front legs destroyed.");
                    }
                    
                    // arm-mounted weapons have addidional trouble
                    if (weapon.getLocation() == Mech.LOC_RARM || weapon.getLocation() == Mech.LOC_LARM) {
                        int otherArm = weapon.getLocation() == Mech.LOC_RARM ? Mech.LOC_LARM : Mech.LOC_RARM;
                        // check previous attacks for weapons fire from the other arm
                        for (Enumeration i = prevAttacks.elements(); i.hasMoreElements();) {
                            Object o = i.nextElement();
                            if (!(o instanceof WeaponAttackAction)) {
                                continue;
                            }
                            WeaponAttackAction prevAttack = (WeaponAttackAction)o;
                            // stop when we get to this weaponattack (does this always work?)
                            if (prevAttack.getEntityId() == attackerId && prevAttack.getWeaponId() == weaponId) {
                                break;
                            }
                            if (prevAttack.getEntityId() == attackerId && ae.getEquipment(prevAttack.getWeaponId()).getLocation() == otherArm) {
                                return new ToHitData(ToHitData.IMPOSSIBLE, "Prone and firing from other front leg already.");
                            }
                        }
                    }
                    // can't fire leg weapons
                    if (weapon.getLocation() == Mech.LOC_LLEG || weapon.getLocation() == Mech.LOC_RLEG) {
                        return new ToHitData(ToHitData.IMPOSSIBLE, "Can't fire rear leg-mounted weapons while prone with destroyed legs.");
                    }
                    toHit.addModifier(2, "attacker prone");
                } else if ( legsDead >= 3 ) {
                    return new ToHitData(ToHitData.IMPOSSIBLE, "Prone with three or more legs destroyed.");
                }
            } else {
                // must have an arm intact
                if (ae.isLocationDestroyed(Mech.LOC_RARM) || ae.isLocationDestroyed(Mech.LOC_LARM)) {
                    return new ToHitData(ToHitData.IMPOSSIBLE, "Prone with one or both arms destroyed.");
                }
                // arm-mounted weapons have addidional trouble
                if (weapon.getLocation() == Mech.LOC_RARM || weapon.getLocation() == Mech.LOC_LARM) {
                    int otherArm = weapon.getLocation() == Mech.LOC_RARM ? Mech.LOC_LARM : Mech.LOC_RARM;
                    // check previous attacks for weapons fire from the other arm
                    for (Enumeration i = prevAttacks.elements(); i.hasMoreElements();) {
                        Object o = i.nextElement();
                        if (!(o instanceof WeaponAttackAction)) {
                            continue;
                        }
                        WeaponAttackAction prevAttack = (WeaponAttackAction)o;
                        // stop when we get to this weaponattack (does this always work?)
                        if (prevAttack.getEntityId() == attackerId && prevAttack.getWeaponId() == weaponId) {
                            break;
                        }
                        if (prevAttack.getEntityId() == attackerId
                        && ae.getEquipment(prevAttack.getWeaponId()).getLocation() == otherArm) {
                            return new ToHitData(ToHitData.IMPOSSIBLE, "Prone and firing from other arm already.");
                        }
                    }
                }
                // can't fire leg weapons
                if (weapon.getLocation() == Mech.LOC_LLEG || weapon.getLocation() == Mech.LOC_RLEG) {
                    return new ToHitData(ToHitData.IMPOSSIBLE, "Can't fire leg-mounted weapons while prone.");
                }
                toHit.addModifier(2, "attacker prone");
            }
        }

        // target prone
        if (te.isProne()) {
            // easier when point-blank
            if (range <= 1) {
                toHit.addModifier(-2, "target prone and adjacent");
            }
            // harder at range
            if (range > 1) {
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
        
        // targeting computer
        if (ae.hasTargComp() && wtype.hasFlag(WeaponType.F_DIRECT_FIRE)) {
            toHit.addModifier(-1, "targeting computer");
        }
        
        // change hit table for partial cover
        if (los.targetCover) {
            toHit.setHitTable(ToHitData.HIT_PUNCH);
        }
        
        // factor in target side
	if ( isAttackerInfantry && 0 == range ) {
	    // Infantry attacks from the same hex
	    // are resolved against the front.
	    toHit.setSideTable( ToHitData.SIDE_FRONT );
	} else {
            toHit.setSideTable( targetSideTable(ae, te) );
	}
        
        // okay!
        return toHit;
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
    public static LosEffects calculateLos(Game game, int attackerId, int targetId) {
        final Entity ae = game.getEntity(attackerId);
        final Entity te = game.getEntity(targetId);
        
        // LOS fails if one of the entities is not deployed.
        if (null == ae.getPosition() || null == te.getPosition()) {
            LosEffects los = new LosEffects();
            los.blocked = true; // TODO: come up with a better "impossible"
            return los;
        }
        
        double degree = ae.getPosition().degree(te.getPosition());
        if (degree % 60 == 30) {
            return losDivided(game, attackerId, targetId);
        } else {
            return losStraight(game, attackerId, targetId);
        }
    }
    
    /**
     * Returns LosEffects for a line that never passes exactly between two 
     * hexes.  Since intervening() returns all the coordinates, we just
     * add the effects of all those hexes.
     */
    public static LosEffects losStraight(Game game, int attackerId, int targetId) {
        final Entity ae = game.getEntity(attackerId);
        final Entity te = game.getEntity(targetId);
        Coords[] in = intervening(ae.getPosition(), te.getPosition());
        
        LosEffects los = new LosEffects();
        
        for (int i = 0; i < in.length; i++) {
            los.add(losForCoords(game, attackerId, targetId, in[i]));
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
    public static LosEffects losDivided(Game game, int attackerId, int targetId) {
        final Entity ae = game.getEntity(attackerId);
        final Entity te = game.getEntity(targetId);
        Coords[] in = intervening(ae.getPosition(), te.getPosition());
        LosEffects los = new LosEffects();
        
        // add non-divided line segments
        for (int i = 3; i < in.length - 2; i += 3) {
            los.add(losForCoords(game, attackerId, targetId, in[i]));
        }
        
        // if blocked already, return that
        if (losModifiers(los).getValue() == ToHitData.IMPOSSIBLE) {
            return los;
        }
        
        // go through divided line segments
        for (int i = 1; i < in.length - 2; i += 3) {
            // get effects of each side
            LosEffects left = losForCoords(game, attackerId, targetId, in[i]);
            LosEffects right = losForCoords(game, attackerId, targetId, in[i+1]);
            left.add(los);
            right.add(los);
            
            // which is better?
            if (losModifiers(left).getValue() > losModifiers(right).getValue()) {
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
    public static LosEffects losForCoords(Game game, int attackerId, int targetId, Coords coords) {
        LosEffects los = new LosEffects();
        // ignore hexes not on board
        if (!game.board.contains(coords)) {
            return los;
        }
        final Entity ae = game.getEntity(attackerId);
        final Entity te = game.getEntity(targetId);
        // ignore hexes the attacker or target are in
        if (coords.equals(ae.getPosition()) || coords.equals(te.getPosition())) {
            return los;
        }
        int attEl = ae.elevation() + ae.height();
        int targEl = te.elevation() + te.height();
        Hex hex = game.board.getHex(coords);
        int hexEl = hex.surface();
        
        
        // check for block by terrain
        if ((hexEl > attEl && hexEl > targEl)
        || (hexEl > attEl && ae.getPosition().distance(coords) == 1)
        || (hexEl > targEl && te.getPosition().distance(coords) == 1)) {
            los.blocked = true;
        }
        
        // check for woods or smoke
        if ((hexEl + 2 > attEl && hexEl + 2 > targEl)
        || (hexEl + 2 > attEl && ae.getPosition().distance(coords) == 1)
        || (hexEl + 2 > targEl && te.getPosition().distance(coords) == 1)) {
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
        if (te.getPosition().distance(coords) == 1 && hexEl == targEl 
        && attEl <= targEl && te.height() > 0) {
            los.targetCover = true;
        }

        // check for attacker partial cover
        if (ae.getPosition().distance(coords) == 1 && hexEl == attEl 
        && attEl >= targEl && ae.height() > 0) {
            los.attackerCover = true;
        }
        
        return los;
    }
    

    public static ToHitData toHitPunch(Game game, PunchAttackAction paa) {
        return toHitPunch(game, paa.getEntityId(), paa.getTargetId(), 
                          paa.getArm());
    }
    
    
    
    /**
     * To-hit number for the specified arm to punch
     */
    public static ToHitData toHitPunch(Game game, int attackerId, 
                                       int targetId, int arm) {
        final Entity ae = game.getEntity(attackerId);
        final Entity te = game.getEntity(targetId);
        final int attackerHeight = ae.absHeight();
        final int targetHeight = te.absHeight();
        final int targetElevation = te.elevation();
        final int armLoc = (arm == PunchAttackAction.RIGHT)
                           ? Mech.LOC_RARM : Mech.LOC_LARM;
        final int armArc = (arm == PunchAttackAction.RIGHT)
                           ? Compute.ARC_RIGHTARM : Compute.ARC_LEFTARM;
        ToHitData toHit;

	// non-mechs can't punch
	if (!(ae instanceof Mech)) {
	    return new ToHitData(ToHitData.IMPOSSIBLE, "Non-mechs can't punch");
	}

        // arguments legal?
        if (arm != PunchAttackAction.RIGHT && arm != PunchAttackAction.LEFT) {
            throw new IllegalArgumentException("Arm must be LEFT or RIGHT");
        }
        if (ae == null || te == null) {
            throw new IllegalArgumentException("Attacker or target id not valid");
        }

        // Can't target a transported entity.
        if ( Entity.NONE != te.getTransportId() ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target is a passenger.");
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
        if (ae.getPosition().distance(te.getPosition()) > 1 ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target not in range");
        }
        
        // check elevation
        if (attackerHeight < targetElevation || attackerHeight > targetHeight) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target elevation not in range");
        }
        
        // can't physically attack mechs making dfa attacks
        if (te.isMakingDfa()) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target is making a DFA attack");
        }

        // check facing
        if (!isInArc(ae.getPosition(), ae.getSecondaryFacing(), 
                     te.getPosition(), armArc)) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target not in arc");
        }
        
        // can't punch while prone (except vehicles, but they're not in the game yet)
        if (ae.isProne()) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Attacker is prone");
        }
        
        // okay, modifiers...
        toHit = new ToHitData(4, "base");

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
        toHit.append(getTargetTerrainModifier(game, targetId));
        
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
        if (te.isImmobile()) {
            toHit.addModifier(-4, "target immobile");
        }
        
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
        return (int)Math.floor(damage * multiplier);
    }
    
    public static ToHitData toHitKick(Game game, KickAttackAction kaa) {
        return toHitKick(game, kaa.getEntityId(), kaa.getTargetId(), 
                         kaa.getLeg());
    }
    
    /**
     * To-hit number for the specified leg to kick
     */
    public static ToHitData toHitKick(Game game, int attackerId, 
                                       int targetId, int leg) {
        final Entity ae = game.getEntity(attackerId);
        final Entity te = game.getEntity(targetId);
        final int attackerElevation = ae.elevation();
        final int targetHeight = te.absHeight();
        final int targetElevation = te.elevation();
        int[] kickLegs = new int[2];

	// non-mechs can't kick
	if (!(ae instanceof Mech)) {
	    return new ToHitData(ToHitData.IMPOSSIBLE, "Non-mechs can't kick");
	}

        if ( ae.entityIsQuad() ) {
          kickLegs[0] = Mech.LOC_RARM;
          kickLegs[1] = Mech.LOC_LARM;
        } else {
          kickLegs[0] = Mech.LOC_RLEG;
          kickLegs[1] = Mech.LOC_LLEG;
        }

        final int legLoc = (leg == KickAttackAction.RIGHT) ? kickLegs[0] : kickLegs[1];
        
        ToHitData toHit;

        // arguments legal?
        if (leg != KickAttackAction.RIGHT && leg != KickAttackAction.LEFT) {
            throw new IllegalArgumentException("Leg must be LEFT or RIGHT");
        }
        if (ae == null || te == null) {
            throw new IllegalArgumentException("Attacker or target id not valid");
        }

        // Can't target a transported entity.
        if ( Entity.NONE != te.getTransportId() ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target is a passenger.");
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
        final int range = ae.getPosition().distance(te.getPosition());
        if ( range > 1 ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target not in range");
        }
        
        // check elevation 
        if (attackerElevation < targetElevation || attackerElevation > targetHeight) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target elevation not in range");
        }
        
        // can't physically attack mechs making dfa attacks
        if (te.isMakingDfa()) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target is making a DFA attack");
        }

        // check facing
	// Don't check arc for stomping infantry or tanks.
        if (0 != range &&
	    !isInArc(ae.getPosition(), ae.getFacing(), 
                     te.getPosition(), Compute.ARC_FORWARD)) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target not in arc");
        }
        
        // can't kick while prone
        if (ae.isProne()) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Attacker is prone");
        }
        
        // okay, modifiers...
        toHit = new ToHitData(3, "base");

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
        toHit.append(getTargetTerrainModifier(game, targetId));
        
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
        if (te.isImmobile()) {
            toHit.addModifier(-4, "target immobile");
        }
        
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

        return (int)Math.floor(damage * multiplier);
    }
    
    public static ToHitData toHitClub(Game game, ClubAttackAction caa) {
        return toHitClub(game, caa.getEntityId(), caa.getTargetId(), 
                         caa.getClub());
    }
    
    /**
     * To-hit number for the specified club to hit
     */
    public static ToHitData toHitClub(Game game, int attackerId, int targetId, Mounted club) {
        final Entity ae = game.getEntity(attackerId);
        final Entity te = game.getEntity(targetId);
        final int attackerElevation = ae.elevation();
        final int attackerHeight = ae.absHeight();
        final int targetHeight = te.absHeight();
        final int targetElevation = te.elevation();
        final boolean bothArms = club.getType().hasFlag(MiscType.F_CLUB);
        ToHitData toHit;

        // arguments legal?
        if (ae == null || te == null) {
            throw new IllegalArgumentException("Attacker or target id not valid");
        }

	// non-mechs can't club
	if (!(ae instanceof Mech)) {
	    return new ToHitData(ToHitData.IMPOSSIBLE, "Non-mechs can't club");
	}

        //Quads can't club
        if ( ae.entityIsQuad() ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Attacker is a quad");
        }

        // Can't target a transported entity.
        if ( Entity.NONE != te.getTransportId() ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target is a passenger.");
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
        if (ae.getPosition().distance(te.getPosition()) > 1 ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target not in range");
        }
        
        // check elevation (target must be within one level)
        if (targetHeight < attackerElevation || targetElevation > attackerHeight) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target elevation not in range");
        }
        
        // can't physically attack mechs making dfa attacks
        if (te.isMakingDfa()) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target is making a DFA attack");
        }

        // check facing
        int clubArc = bothArms ? Compute.ARC_FORWARD : (club.getLocation() == Mech.LOC_LARM ? Compute.ARC_LEFTARM : Compute.ARC_RIGHTARM);
        if (!isInArc(ae.getPosition(), ae.getSecondaryFacing(), te.getPosition(), clubArc)) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target not in arc");
        }
        
        // can't club while prone
        if (ae.isProne()) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Attacker is prone");
        }
        
        // okay, modifiers...
        toHit = new ToHitData(4, "base");

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
        toHit.append(getTargetTerrainModifier(game, targetId));
        
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
        if (te.isImmobile()) {
            toHit.addModifier(-4, "target immobile");
        }
        
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
        if (entity.heat >= 9 && ((Mech)entity).hasTSM()) {
            nDamage *= 2;
        }

        return nDamage;
    }
    
    public static ToHitData toHitPush(Game game, PushAttackAction paa) {
        return toHitPush(game, paa.getEntityId(), paa.getTargetId());
    }
    
    /**
     * To-hit number for the mech to push another mech
     */
    public static ToHitData toHitPush(Game game, int attackerId, int targetId) {
        final Entity ae = game.getEntity(attackerId);
        final Entity te = game.getEntity(targetId);
        final int attackerElevation = ae.elevation();
        final int targetElevation = te.elevation();
        ToHitData toHit = null;

        // arguments legal?
        if (ae == null || te == null || ae == te) {
            throw new IllegalArgumentException("Attacker or target id not valid");
        }
        
	// non-mechs can't push
	if (!(ae instanceof Mech)) {
	    return new ToHitData(ToHitData.IMPOSSIBLE, "Non-mechs can't push");
	}

        //Quads can't push
        if ( ae.entityIsQuad() ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Attacker is a quad");
        }
        
        //Can only push mechs
        if (! (te instanceof Mech)) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target is not a mech");
        }

        //Can't push with flipped arms
        if (ae.getArmsFlipped()) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Arms are flipped to the rear. Can not push.");
        }

        // Can't target a transported entity.
        if ( Entity.NONE != te.getTransportId() ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target is a passenger.");
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
        if (ae.getPosition().distance(te.getPosition()) > 1 ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target not in range");
        }
        
        // target must be at same elevation
        if (attackerElevation != targetElevation) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target not at same elevation");
        }
        
        // can't push mech making non-pushing displacement attack
        if (te.hasDisplacementAttack() && !te.isPushing()) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target is making a charge/DFA attack");
        }
        
        // can't push mech pushing another, different mech
        if (te.isPushing() && te.getDisplacementAttack().getTargetId() != ae.getId()) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target is pushing another mech");
        }
        
        // can't do anything but counter-push if the target of another attack
        if (ae.isTargetOfDisplacementAttack() && ae.findTargetedDisplacement().getEntityId() != te.getId()) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Attacker is the target of another push/charge/DFA");
        }
        
        // can't attack the target of another displacement attack
        if (te.isTargetOfDisplacementAttack() && te.findTargetedDisplacement().getEntityId() != ae.getId()) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target is the target of another push/charge/DFA");
        }
        
        // check facing
        if (!te.getPosition().equals(ae.getPosition().translated(ae.getFacing()))) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target not directly ahead of feet");
        }
        
        // can't push while prone
        if (ae.isProne()) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Attacker is prone");
        }
        
        // can't push prone mechs
        if (te.isProne()) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target is prone");
        }
        
        // okay, modifiers...
        toHit = new ToHitData(4, "base");
        
        // attacker movement
        toHit.append(getAttackerMovementModifier(game, attackerId));
        
        // target movement
        toHit.append(getTargetMovementModifier(game, targetId));
        
        // attacker terrain
        toHit.append(getAttackerTerrainModifier(game, attackerId));
        
        // target terrain
        toHit.append(getTargetTerrainModifier(game, targetId));
        
        // damaged or missing actuators
        if (!ae.hasWorkingSystem(Mech.ACTUATOR_SHOULDER, Mech.LOC_RARM)) {
            toHit.addModifier(2, "Right Shoulder destroyed");
        }
        if (!ae.hasWorkingSystem(Mech.ACTUATOR_SHOULDER, Mech.LOC_LARM)) {
            toHit.addModifier(2, "Left Shoulder destroyed");
        }

        // water partial cover?
        Hex targHex = game.board.getHex(te.getPosition());
        if (te.height() > 0 && targHex.levelOf(Terrain.WATER) == te.height()) {
            toHit.addModifier(3, "target has partial cover");
        }

        // target immobile
        if (te.isImmobile()) {
            toHit.addModifier(-4, "target immobile");
        }
        
        // side and elevation shouldn't matter

        // done!
        return toHit;
    }
    
	/**
	* Checks if a charge can hit the target, including movement
	*/
	public static ToHitData toHitCharge(Game game, int attackerId, int targetId, MovementData md) {
		// the attacker must have at least walked...
		int movement=Entity.MOVE_WALK;
		// ...if they moved more than their walk MPs, they must've Run
		if (md.getMpUsed() > game.getEntity(attackerId).walkMP) {
			movement=Entity.MOVE_RUN;
		};
		// ...and if they have a jump step, they must've jumped!
		if (md.contains(MovementData.STEP_START_JUMP)) {
			movement=Entity.MOVE_JUMP;
		};

		return toHitCharge(game, attackerId, targetId, md, movement);
	};

	/**
	* Checks if a charge can hit the target, taking account of movement
	*/
	public static ToHitData toHitCharge(Game game, int attackerId, int targetId, MovementData md, int movement) {
        final Entity ae = game.getEntity(attackerId);
        final Entity te = game.getEntity(targetId);
        Coords chargeSrc = ae.getPosition();
        MovementData.Step chargeStep = null;

	// Infantry CAN'T charge!!!
	if ( ae instanceof Infantry ) {
	    return new ToHitData(ToHitData.IMPOSSIBLE, "Infantry can't charge");
	}

        // let's just check this
        if (!md.contains(MovementData.STEP_CHARGE)) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Charge action not found in movment path");
        }

        // no jumping
        if (md.contains(MovementData.STEP_START_JUMP)) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "No jumping allowed while charging");
        }

        // no backwards
        if (md.contains(MovementData.STEP_BACKWARDS) 
            || md.contains(MovementData.STEP_LATERAL_LEFT_BACKWARDS)
            || md.contains(MovementData.STEP_LATERAL_RIGHT_BACKWARDS)) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "No backwards movement allowed while charging");
        }

        // Can't target a transported entity.
        if ( Entity.NONE != te.getTransportId() ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target is a passenger.");
        }

        // determine last valid step
        compile(game, attackerId, md);
        for (final Enumeration i = md.getSteps(); i.hasMoreElements();) {
            final MovementData.Step step = (MovementData.Step)i.nextElement();
            if (step.getMovementType() == Entity.MOVE_ILLEGAL) {
                break;
            } else {
                if (step.getType() == MovementData.STEP_CHARGE) {
                    chargeStep = step;
                } else {
                    chargeSrc = step.getPosition();
                }
            }
        }

        // need to reach target
        if (chargeStep == null || !te.getPosition().equals(chargeStep.getPosition())) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Could not reach target with movement");
        }

        // target must have moved already
        if (te.ready) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target must be done with movement");
        }

	return toHitCharge(game, attackerId, targetId, chargeSrc, movement);
    }

    /**
     * To-hit number for a charge, assuming that movement has been handled
     */
    public static ToHitData toHitCharge(Game game, ChargeAttackAction caa) {
        final Entity entity = game.getEntity(caa.getEntityId());
        return toHitCharge(game, caa.getEntityId(), caa.getTargetId(), entity.getPosition(),entity.moved);
    }

    /**
     * To-hit number for a charge, assuming that movement has been handled
     */
    public static ToHitData toHitCharge(Game game, int attackerId, int targetId, Coords src, int movement) {
        final Entity ae = game.getEntity(attackerId);
        final Entity te = game.getEntity(targetId);
        final int attackerElevation = ae.elevationOccupied(game.board.getHex(src));
        final int attackerHeight = attackerElevation + ae.height();
        final int targetElevation = te.elevation();
        final int targetHeight = te.absHeight();
        ToHitData toHit = null;
        
        // arguments legal?
        if (ae == null || te == null || ae == te) {
            throw new IllegalArgumentException("Attacker or target id not valid");
        }
        
	// Infantry CAN'T charge!!!
	if ( ae instanceof Infantry ) {
	    return new ToHitData(ToHitData.IMPOSSIBLE, "Infantry can't charge");
	}

        // Can't target a transported entity.
        if ( Entity.NONE != te.getTransportId() ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target is a passenger.");
        }

        // check range
        if (src.distance(te.getPosition()) > 1 ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target not in range");
        }
        
        // mechs can only charge standing mechs
        if (ae instanceof Mech) {
            if (! (te instanceof Mech)) {
                return new ToHitData(ToHitData.IMPOSSIBLE, "Target is not a mech");
            }
            if (te.isProne()) {
                return new ToHitData(ToHitData.IMPOSSIBLE, "Target is prone");
            }
        }
        else if ( te instanceof Infantry ) {
            // Can't charge infantry.
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target is infantry/");
        }
        
        // target must be within 1 elevation level
        if (attackerElevation > targetHeight || attackerHeight < targetElevation) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target must be within 1 elevation level");
        }
        
        // can't charge while prone
        if (ae.isProne()) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Attacker is prone");
        }        
        
        // can't attack mech making a different displacement attack
        if (te.hasDisplacementAttack()) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target is already making a charge/DFA attack");
        }
        
        // can't attack the target of another displacement attack
        if (te.isTargetOfDisplacementAttack()) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target is the target of another charge/DFA");
        }
        
        // okay, modifiers...
        toHit = new ToHitData(5, "base");
        
        // attacker movement
        toHit.append(getAttackerMovementModifier(game, attackerId, movement));
        
        // target movement
        toHit.append(getTargetMovementModifier(game, targetId));
        
        // attacker terrain
        toHit.append(getAttackerTerrainModifier(game, attackerId));
        
        // target terrain
        toHit.append(getTargetTerrainModifier(game, targetId));
        
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
        if (te.isImmobile()) {
            toHit.addModifier(-4, "target immobile");
        }

        // determine hit direction
        toHit.setSideTable(targetSideTable(src, te.getPosition(),
                                            te.getFacing(), te instanceof Tank));
                                            
        // elevation
        if (attackerHeight == targetHeight || te.isProne()) {
            toHit.setHitTable(ToHitData.HIT_NORMAL);
        } else if (attackerHeight < targetHeight) {
            toHit.setHitTable(ToHitData.HIT_KICK);
        } else {
            toHit.setHitTable(ToHitData.HIT_PUNCH);
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
		return (int)Math.ceil((entity.getWeight() / 10.0) * (hexesMoved - 1));
	};
    
    /**
     * Damage that a mech suffers after a successful charge.
     */
    public static int getChargeDamageTakenBy(Entity entity, Entity target) {
        return (int)Math.ceil(target.getWeight() / 10.0);
    }
    
    /**
     * Checks if a death from above attack can hit the target, including movement
     */
    public static ToHitData toHitDfa(Game game, int attackerId, int targetId, MovementData md) {
        final Entity ae = game.getEntity(attackerId);
        final Entity te = game.getEntity(targetId);
        Coords chargeSrc = ae.getPosition();
        MovementData.Step chargeStep = null;
        
        // Infantry CAN'T dfa!!!
	if ( ae instanceof Infantry ) {
	    return new ToHitData(ToHitData.IMPOSSIBLE, "Infantry can't D.F.A.");
	}

        // let's just check this
        if (!md.contains(MovementData.STEP_DFA)) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "D.F.A. action not found in movment path");
        }
        
        // have to jump
        if (!md.contains(MovementData.STEP_START_JUMP)) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "D.F.A. must involve jumping");
        }

        // Can't target a transported entity.
        if ( Entity.NONE != te.getTransportId() ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target is a passenger.");
        }
        
        // determine last valid step
        compile(game, attackerId, md);
        for (final Enumeration i = md.getSteps(); i.hasMoreElements();) {
            final MovementData.Step step = (MovementData.Step)i.nextElement();
            if (step.getMovementType() == Entity.MOVE_ILLEGAL) {
                break;
            } else {
                if (step.getType() == MovementData.STEP_DFA) {
                    chargeStep = step;
                } else {
                    chargeSrc = step.getPosition();
                }
            }
        }
        
        // need to reach target
        if (chargeStep == null || !te.getPosition().equals(chargeStep.getPosition())) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Could not reach target with movement");
        }
        
        // target must have moved already
        if (te.ready) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target must be done with movement");
        }
        
	return toHitDfa(game, attackerId, targetId, chargeSrc);
    }
    
    public static ToHitData toHitDfa(Game game, DfaAttackAction daa) {
        final Entity entity = game.getEntity(daa.getEntityId());
        return toHitDfa(game, daa.getEntityId(), daa.getTargetId(), entity.getPosition());
    }
    
    /**
     * To-hit number for a death from above attack, assuming that movement has 
     * been handled
     */
    public static ToHitData toHitDfa(Game game, int attackerId, int targetId, Coords src) {
        final Entity ae = game.getEntity(attackerId);
        final Entity te = game.getEntity(targetId);
        ToHitData toHit = null;
        
        // arguments legal?
        if (ae == null || te == null || ae == te) {
            throw new IllegalArgumentException("Attacker or target id not valid");
        }
        
	// Infantry CAN'T dfa!!!
	if ( ae instanceof Infantry ) {
	    return new ToHitData(ToHitData.IMPOSSIBLE, "Infantry can't dfa");
	}

        // Can't target a transported entity.
        if ( Entity.NONE != te.getTransportId() ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target is a passenger.");
        }

        // check range
        if (src.distance(te.getPosition()) > 1 ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target not in range");
        }
        
        // can't dfa while prone, even if you somehow did manage to jump
        if (ae.isProne()) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Attacker is prone");
        }
        
        // can't attack mech making a different displacement attack
        if (te.hasDisplacementAttack()) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target is already making a charge/DFA attack");
        }
        
        // can't attack the target of another displacement attack
        if (te.isTargetOfDisplacementAttack()) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target is the target of another charge/DFA");
        }        
        
        // okay, modifiers...
        toHit = new ToHitData(5, "base");

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
        if (te.isImmobile()) {
            toHit.addModifier(-4, "target immobile");
        }
        
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
        } else if (movement == Entity.MOVE_RUN) {
            toHit.addModifier(2, "attacker ran");
        } else if (movement == Entity.MOVE_JUMP) {
            toHit.addModifier(3, "attacker jumped");
        }
        
        return toHit;
    }
    
    /**
     * Modifier to attacks due to target movement
     */
    public static ToHitData getTargetMovementModifier(Game game, int entityId) {
        Entity entity = game.getEntity(entityId);
        return getTargetMovementModifier(entity.delta_distance, 
                                         entity.moved == Entity.MOVE_JUMP);
    }
  
    /**
     * Target movement modifer for the specified delta_distance
     */
    public static ToHitData getTargetMovementModifier(int distance, boolean jumped) {
        ToHitData toHit = new ToHitData();
      
        if (distance >= 3 && distance <= 4) {
            toHit.addModifier(1, "target moved 3-4 hexes");
        } else if (distance >= 5 && distance <= 6) {
            toHit.addModifier(2, "target moved 5-6 hexes");
        } else if (distance >= 7 && distance <= 9) {
            toHit.addModifier(3, "target moved 7-9 hexes");
        } else if (distance >= 10) {
            toHit.addModifier(4, "target moved 10+ hexes");
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
    public static ToHitData getTargetTerrainModifier(Game game, int entityId) {
        final Entity target = game.getEntity(entityId);
        final Hex hex = game.board.getHex(target.getPosition());
        
        // you don't get terrain modifiers in midair
        if (target.isMakingDfa()) {
            return new ToHitData();
        }
        
        ToHitData toHit = new ToHitData();
        if (target instanceof HexEntity) {
            if (hex.contains(Terrain.SMOKE)) {
                toHit.addModifier(2, "target in smoke");
            }
            return toHit; // terrain doesn't get target modifiers for itself
        }

        if (hex.levelOf(Terrain.WATER) > 0
        && target.getMovementType() != Entity.MovementType.HOVER) {
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
    public static WeaponAttackAction getHighestExpectedDamage(Game g, Vector vAttacks, Vector vOtherAttacks)
    {
    float fHighest = -1.0f;
        WeaponAttackAction waaHighest = null;
        for (int x = 0, n = vAttacks.size(); x < n; x++) {
            WeaponAttackAction waa = (WeaponAttackAction)vAttacks.elementAt(x);
            float fDanger = getExpectedDamage(g, waa, vOtherAttacks);
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
    public static float getExpectedDamage(Game g, WeaponAttackAction waa, Vector vOtherAttacks)
    {
        Entity attacker = g.getEntity(waa.getEntityId());
        Mounted weapon = attacker.getEquipment(waa.getWeaponId());
        System.out.println("Computing expected damage for " + attacker.getShortName() + " " + 
                weapon.getName());
        ToHitData hitData = Compute.toHitWeapon(g, waa, vOtherAttacks);
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
        if (wt.getDamage() == wt.DAMAGE_MISSILE) {
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
     * Checks to see if a target entity is in arc of the specified
     * weapon, on the specified entity
     */
    public static boolean isInArc(Game game, int attackerId, int weaponId, int targetId) {
        Entity ae = game.getEntity(attackerId);
        Entity te = game.getEntity(targetId);
        int facing = ae.isSecondaryArcWeapon(weaponId) ? ae.getSecondaryFacing() : ae.getFacing();
        return isInArc(ae.getPosition(), facing, te.getPosition(), ae.getWeaponArc(weaponId));
        
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
    public static boolean canSee(Game game, Entity ae, Entity te)
    {
        LosEffects los = calculateLos(game, ae.getId(), te.getId());
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
        IdealHex iSrc = new IdealHex(src);
        IdealHex iDest = new IdealHex(dest);
        
        int[] directions = new int[3];
        directions[2] = (int)Math.round((double)src.degree(dest) / 60.0) % 6; // center last
        directions[1] = (directions[2] + 5) % 6;
        directions[0] = (directions[2] + 1) % 6;
        
        Vector hexes = new Vector();
        Coords current = src;
        
        hexes.addElement(current);
        while(!dest.equals(current)) {
            current = nextHex(current, src, iSrc, iDest, directions);
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
    public static Coords nextHex(Coords current, Coords src, IdealHex iSrc, IdealHex iDest, int[] directions) {
        for (int i = 0; i < directions.length; i++) {
            Coords testing = current.translated(directions[i]);
            if (new IdealHex(testing).isIntersectedBy(iSrc.cx, iSrc.cy, iDest.cx, iDest.cy)) {
                return testing;
            }
        }
        // if we're here then something's fishy!
        throw new RuntimeException("Couldn't find the next hex!");
    }

    public static int targetSideTable(Entity attacker, Entity target) {
        return targetSideTable(attacker.getPosition(), target.getPosition(), target.getFacing(), target instanceof Tank);
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
          
        // need woods for now
        //TODO: building rubble, possibly missing limbs
        if (hex.levelOf(Terrain.WOODS) < 1) {
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
            if (mounted.getType().hasFlag(MiscType.F_CLUB) || mounted.getType().hasFlag(MiscType.F_HATCHET)) {
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
     * @param   missles - the <code>int</code> number of missles in the pack.
     */
    public static int missilesHit(int missiles) {
        return missilesHit(missiles, 0);
    }

    /**
     * Roll the number of missiles (or whatever) on the missile
     * hit table, with the specified mod to the roll.
     *
     * @param   missles - the <code>int</code> number of missles in the pack.
     * @param   nMod - the <code>int</code> modifier to the roll for number
     *          of missles that hit.
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
     * @param	hit - the <code>int</code> number of the crew hit currently
     *		being rolled.
     * @return	The <code>int</code> number that must be rolled on 2d6
     *		for the crew to stay concious.
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
        for (Enumeration e = ae.game.getEntities(); e.hasMoreElements(); ) {
            Entity ent = (Entity)e.nextElement();
            if (ent.isEnemyOf(ae) && ent.hasActiveECM() && ent.getPosition() != null) {
                vEnemyCoords.addElement(ent.getPosition());
            }
        }
        
        // none?  get out of here
        if (vEnemyCoords.size() == 0) return false;
        
        // get intervening Coords.  See the comments for intervening() and losDivided()
        Coords[] coords = intervening(a, b);
        boolean bDivided = (a.degree(b) % 60 == 30);
        for (Enumeration e = vEnemyCoords.elements(); e.hasMoreElements(); ) {
            Coords c = (Coords)e.nextElement();
            int nLastDist = -1;
            
            // loop through intervening hexes and see if any of them are within range
            for (int x = 0; x < coords.length; x++) {
                int nDist = c.distance(coords[x]);
                
                if (nDist <= 6) return true;
                
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
        if ( Entity.NONE != defender.getTransportId() ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target is a passenger.");
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
        if ( Entity.NONE != defender.getTransportId() ) {
            return new ToHitData(ToHitData.IMPOSSIBLE, "Target is a passenger.");
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

} // End public class Compute
