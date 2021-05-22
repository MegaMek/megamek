/**
 * MegaMek -
 * Copyright (C) 2000,2001,2002,2003,2004,2005 Ben Mazur (bmazur@sev.org)
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;

import megamek.client.bot.princess.Princess;
import megamek.common.annotations.Nullable;
import megamek.common.logging.LogLevel;
import megamek.common.options.OptionsConstants;
import megamek.common.pathfinder.AbstractPathFinder;
import megamek.common.pathfinder.CachedEntityState;
import megamek.common.pathfinder.DestructionAwareDestinationPathfinder;
import megamek.common.pathfinder.ShortestPathFinder;
import megamek.common.preference.PreferenceManager;

/**
 * Holds movement path for an entity.
 */
public class MovePath implements Cloneable, Serializable {
    private static final long serialVersionUID = -4258296679177532986L;

    private Set<Coords> coordsSet = null;
    private final transient Object COORD_SET_LOCK = new Object();
    private transient CachedEntityState cachedEntityState;

    public IGame getGame() {
        return game;
    }

    public void setGame(IGame game) {
        this.game = game;
    }

    public void setEntity(Entity entity) {
        this.entity = entity;
        cachedEntityState = new CachedEntityState(entity);
    }

    public enum MoveStepType {
        NONE, FORWARDS, BACKWARDS, TURN_LEFT, TURN_RIGHT, GET_UP, GO_PRONE, START_JUMP, CHARGE, DFA,
        FLEE, LATERAL_LEFT, LATERAL_RIGHT, LATERAL_LEFT_BACKWARDS, LATERAL_RIGHT_BACKWARDS, UNJAM_RAC,
        LOAD, UNLOAD, EJECT, CLEAR_MINEFIELD, UP, DOWN, SEARCHLIGHT, LAY_MINE, HULL_DOWN, CLIMB_MODE_ON,
        CLIMB_MODE_OFF, SWIM, DIG_IN, FORTIFY, SHAKE_OFF_SWARMERS, TAKEOFF, VTAKEOFF, LAND, ACC, DEC, EVADE,
        SHUTDOWN, STARTUP, SELF_DESTRUCT, ACCN, DECN, ROLL, OFF, RETURN, LAUNCH, THRUST, YAW, CRASH, RECOVER,
        RAM, HOVER, MANEUVER, LOOP, CAREFUL_STAND, JOIN, DROP, VLAND, MOUNT, UNDOCK, TAKE_COVER,
        CONVERT_MODE, BOOTLEGGER, TOW, DISCONNECT;
    }

    public static class Key {
        private final Coords coords;
        private final int facing;
        private final int type;

        public Key(final Coords coords, final int facing, final int type) {
            this.coords = coords;
            this.facing = facing;
            this.type = type;
        }

        @Override
        public boolean equals(final Object obj) {
            if (!(obj instanceof Key)) {
                return false;
            }
            final Key s1 = (Key) obj;
            return (type == s1.type) && (facing == s1.facing) && coords.equals(s1.coords);
        }

        @Override
        public int hashCode() {
            return type + (7 * (facing + (31 * coords.hashCode())));
        }
    }

    private Vector<MoveStep> steps = new Vector<>();

    private transient IGame game;
    private transient Entity entity;
    
    // holds the types of steps present in this movement 
    private Set<MoveStepType> containedStepTypes = new HashSet<>();
    
    // whether this movePath take us directly over an enemy unit
    // useful for aircraft
    private boolean fliesOverEnemy;

    public static final int DEFAULT_PATHFINDER_TIME_LIMIT = 500;

    // is this move path being done using careful movement?
    private boolean careful = true;

    /**
     * Generates a new, empty, movement path object.
     */
    public MovePath(final IGame game, final Entity entity) {
        this.setEntity(entity);
        this.setGame(game);
    }

    public Entity getEntity() {
        return entity;
    }
    
    public CachedEntityState getCachedEntityState() {
        return cachedEntityState;
    }

    public Key getKey() {
        return new Key(getFinalCoords(), getFinalFacing(), getFinalProne() ? 0 : isJumping() ? 1 : 2);
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer();
        sb.append("MOVE PATH:");
        sb.append(this.getKey().hashCode());
        sb.append(' '); // it's useful to know for debugging purposes which path you're looking at.
        sb.append("Length: " + this.length());
        sb.append("Final Coords: " + this.getFinalCoords());
        sb.append(System.lineSeparator());
        
        for (final Enumeration<MoveStep> i = steps.elements(); i.hasMoreElements(); ) {
            sb.append(i.nextElement().toString());
            sb.append(' ');
        }
        
        if(!getGame().getBoard().contains(this.getFinalCoords())) {
            sb.append("OUT!");
        }
        
        if(this.getFliesOverEnemy()) {
            sb.append("E! ");
        }
        
        return sb.toString();
    }

    /**
     * Returns the number of steps in this movement
     */
    public int length() {
        return steps.size();
    }

    /**
     * Add a new step to the movement path.
     *
     * @param type the type of movement.
     */
    public MovePath addStep(final MoveStepType type) {
        // TODO : detect steps off the map *here*.
        return addStep(new MoveStep(this, type));
    }

    /**
     * Add a new step to the movement path with the given target.
     *
     * @param type   the type of movement.
     * @param target the <code>Targetable</code> object that is the target of this
     *               step. For example, the enemy being charged.
     */
    public MovePath addStep(final MoveStepType type, final Targetable target) {
        return addStep(new MoveStep(this, type, target));
    }

    public MovePath addStep(final MoveStepType type, final Targetable target, final Coords pos) {
        return addStep(new MoveStep(this, type, target, pos));
    }

    public MovePath addStep(final MoveStepType type, final int mineToLay) {
        return addStep(type, -1, mineToLay);
    }

    public MovePath addStep(final MoveStepType type, final int recover, final int mineToLay) {
        return addStep(new MoveStep(this, type, recover, mineToLay));
    }

    public MovePath addStep(MoveStepType type, TreeMap<Integer, Vector<Integer>> targets) {
        return addStep(new MoveStep(this, type, targets));
    }

    public MovePath addStep(final MoveStepType type, final boolean noCost) {
        return addStep(new MoveStep(this, type, noCost));
    }

    public MovePath addStep(final MoveStepType type, final boolean noCost, final boolean isManeuver) {
        return addStep(new MoveStep(this, type, noCost, isManeuver));
    }

    public MovePath addStep(final MoveStepType type, final Minefield mf) {
        return addStep(new MoveStep(this, type, mf));
    }

    public MovePath addManeuver(final int manType) {
        return addStep(new MoveStep(this, MoveStepType.MANEUVER, -1, -1, manType));
    }

    public boolean canShift() {
        return ((getEntity() instanceof QuadMech
                // QuadVee cannot shift in vee mode
                && !(getEntity() instanceof QuadVee
                        && (entity.getConversionMode() == QuadVee.CONV_MODE_VEHICLE
                            || getEntity().isConvertingNow())))
                // Maneuvering Ace allows Bipeds and VTOLs moving at cruise
                //  speed to perform a lateral shift
                || (getEntity().isUsingManAce()
                    && ((getEntity() instanceof BipedMech)
                        || ((getEntity() instanceof VTOL)
                        && (getMpUsed() <= getCachedEntityState().getWalkMP()))))
                || (game.getOptions().booleanOption(OptionsConstants.ADVGRNDMOV_VEHICLE_ADVANCED_MANEUVERS)
                        && getEntity() instanceof Tank
                        && (getEntity().getMovementMode() == EntityMovementMode.VTOL
                        || getEntity().getMovementMode() == EntityMovementMode.HOVER))
                || ((getEntity() instanceof TripodMech)
                    && (((Mech) getEntity()).countBadLegs() == 0)))
                && !isJumping();
    }

    /**
     * Returns true if this MovePath contains a lateral shift
     * @return
     */
    public boolean containsLateralShift() {
        return this.contains(MoveStepType.LATERAL_LEFT)
                || this.contains(MoveStepType.LATERAL_RIGHT)
                || this.contains(MoveStepType.LATERAL_LEFT_BACKWARDS)
                || this.contains(MoveStepType.LATERAL_RIGHT_BACKWARDS);
    }

    public boolean containsVTOLBomb() {
        for (MoveStep step : steps) {
            if (step.isVTOLBombingStep()) {
                return true;
            }
        }
        return false;
    }

    protected MovePath addStep(final MoveStep step) {
        return addStep(step, true);
    }

    public Set<Coords> getCoordsSet() {
        if (coordsSet != null) {
            return coordsSet;
        }

        synchronized (COORD_SET_LOCK) {
            if (coordsSet != null) {
                return coordsSet;
            }

            coordsSet = new HashSet<>();
            for (MoveStep step : getStepVector()) {
                if (step.getPosition() == null) {
                    continue;
                }
                coordsSet.add(step.getPosition());
            }
        }
        return coordsSet;
    }

    /**
     * Initializes a step as part of this movement path. Then adds it to the
     * list.
     *
     * @param step
     */
    protected MovePath addStep(final MoveStep step, boolean compile) {
        if (step == null) {
            System.err.println(new RuntimeException("Received NULL MoveStep"));
            return this;
        }

        steps.addElement(step);
        containedStepTypes.add(step.getType());
        
        final MoveStep prev = getStep(steps.size() - 2);

        if (compile) {
            try {
                step.compile(getGame(), getEntity(), prev, getCachedEntityState());
            } catch (final RuntimeException re) {
                // // N.B. the pathfinding will try steps off the map.
                // re.printStackTrace();
                step.setMovementType(EntityMovementType.MOVE_ILLEGAL);
            }
        }


        // check for illegal jumps
        final Coords start = getEntity().getPosition();
        final Coords land = step.getPosition();
        if ((start == null) || (land == null)) {
            // If we have null for either coordinate then we know the step
            // isn't legal.
            step.setMovementType(EntityMovementType.MOVE_ILLEGAL);
        } else {
            // if we're jumping without a mechanical jump booster (?)
            // or we're acting like a spheroid dropship in the atmosphere
            if ((isJumping() && (getEntity().getJumpType() != Mech.JUMP_BOOSTER)) ||
                    (Compute.useSpheroidAtmosphere(game, getEntity()) && (step.getType() != MoveStepType.HOVER))) {
                int distance = start.distance(land);
                
                if (step.isThisStepBackwards() || (step.getDistance() > distance)) {
                    step.setMovementType(EntityMovementType.MOVE_ILLEGAL);
                }
            }
        }
        
        // Can't move backwards and Evade
        if (!entity.isAirborne() && contains(MoveStepType.BACKWARDS)
                && contains(MoveStepType.EVADE)) {
            step.setMovementType(EntityMovementType.MOVE_ILLEGAL);
        }
        
        // If jumpships turn, they can't do anything else 
        if (game.getBoard().inSpace()
                && (entity instanceof Jumpship)
                && !(entity instanceof Warship)
                && !step.isFirstStep()
                && (contains(MoveStepType.TURN_LEFT) 
                        || contains(MoveStepType.TURN_RIGHT))) {
            step.setMovementType(EntityMovementType.MOVE_ILLEGAL);
        }
        
        // Ensure we only lay one mine
        if ((step.getType() == MoveStepType.LAY_MINE)) {
            boolean containsOtherLayMineStep = false;
            for (int i = 0; i < steps.size() - 1; i++) {
                if (steps.get(i).getType() == MoveStepType.LAY_MINE) {
                    containsOtherLayMineStep = true;
                }
            }
            if (containsOtherLayMineStep) {
                step.setMovementType(EntityMovementType.MOVE_ILLEGAL);
            }
        }

        // Ensure we only bomb one hex
        if (step.isVTOLBombingStep()) {
            boolean containsOtherBombStep = false;
            for (int i = 0; i < steps.size() - 1; i++) {
                if (steps.get(i).isVTOLBombingStep()) {
                    containsOtherBombStep = true;
                }
            }
            if (containsOtherBombStep) {
                step.setMovementType(EntityMovementType.MOVE_ILLEGAL);
            }
        }
        
        // Make sure we are not turning or changing elevation while strafing, and that we are not
        // starting a second group of hexes during the same round
        if (step.isStrafingStep() && steps.size() > 1) {
            MoveStep last = steps.get(steps.size() - 2);
            // If the previous step is a strafing step, make sure we have the same facing and elevation
            // and we are not exceeding the maximum five hexes.
            if (last.isStrafingStep()) {
                if (step.getFacing() != last.getFacing()
                        || (step.getElevation() + getGame().getBoard().getHex(step.getPosition()).floor()
                            != last.getElevation() + getGame().getBoard().getHex(last.getPosition()).floor())
                        || steps.stream().filter(s -> s.isStrafingStep()).count() > 5) {
                    step.setMovementType(EntityMovementType.MOVE_ILLEGAL);
                }
            } else {
                // If the previous step is not a strafing step, make sure that the new step is the only strafing
                // step we have in the path.
                for (int i = 0; i < steps.size() - 2; i++) {
                    if (steps.get(i).isStrafingStep()) {
                        step.setMovementType(EntityMovementType.MOVE_ILLEGAL);
                    }
                }
            }
        }

        // jumping into heavy woods is danger
        if (game.getOptions().booleanOption(OptionsConstants.ADVGRNDMOV_PSR_JUMP_HEAVY_WOODS)) {
            IHex hex = game.getBoard().getHex(step.getPosition());
            if ((hex != null) && isJumping() && step.isEndPos(this)) {
                PilotingRollData psr = entity.checkLandingInHeavyWoods(step.getMovementType(false), hex);
                if (psr.getValue() != PilotingRollData.CHECK_FALSE) {
                    step.setDanger(true);
                }
            }
        }

        // VTOLs using maneuvering ace to make lateral shifts can't flank
        // unless using controlled sideslip
        if (containsLateralShift() && getEntity().isUsingManAce()
                && (getEntity() instanceof VTOL)
                && getMpUsed() > getCachedEntityState().getWalkMP()
                && !game.getOptions().booleanOption(OptionsConstants.ADVGRNDMOV_VEHICLE_ADVANCED_MANEUVERS)) {
            step.setMovementType(EntityMovementType.MOVE_ILLEGAL);
        }
        
        if (shouldMechanicalJumpCauseFallDamage()) {
            step.setDanger(true);
        }
       
        // If a tractor connects a new trailer this round, it can't do anything but add more trailers
        // This prevents the tractor from moving before its MP, stacking limitations and prohibited terrain can be updated by its trailers
        // It makes sense, too. You can't just connect a trailer and drive off with it in <10 seconds. 
        if (contains(MoveStepType.TOW) && !(step.getType() == MoveStepType.TOW)) {
            step.setMovementType(EntityMovementType.MOVE_ILLEGAL);
        }
        
        // If the new step is legal and is a different position than
        // the previous step, then update the older steps, letting
        // them know that they are no longer the end of the path.
        if (step.isLegal(this) && (null != prev) && !land.equals(prev.getPosition())) {

            // Loop through the steps from back to front.
            // Stop looping when the step says to, or we run out of steps.
            int index = steps.size() - 2;
            while ((index >= 0) && getStep(index).setEndPos(false)) {
                index--;
            }

        } // End step-is-legal

        // If using TacOps reverse gear option, cannot mix forward and backward movement
        // in the same round except VTOLs.
        if (game.getOptions().booleanOption(OptionsConstants.ADVGRNDMOV_REVERSE_GEAR)
                && ((entity instanceof Tank && !(entity instanceof VTOL))
                        || (entity instanceof QuadVee
                                && entity.getConversionMode() == QuadVee.CONV_MODE_VEHICLE))) {
            boolean fwd = false;
            boolean rev = false;
            for (MoveStep s : steps) {
                fwd |=  s.getType() == MoveStepType.FORWARDS
                        || s.getType() == MoveStepType.LATERAL_LEFT
                        || s.getType() == MoveStepType.LATERAL_RIGHT;
                rev |=  s.getType() == MoveStepType.BACKWARDS
                        || s.getType() == MoveStepType.LATERAL_LEFT_BACKWARDS
                        || s.getType() == MoveStepType.LATERAL_RIGHT_BACKWARDS;
            }
            if (fwd && rev) {
                step.setMovementType(EntityMovementType.MOVE_ILLEGAL);
            }
        }
                
        // If we are using turn modes, go back through the path and mark danger for any turn
        // that now exceeds turn mode requirement. We want to show danger on the previous step
        // so the StepSprite will show danger. Hiding the previous step instead would make turning costs
        // show in the turning hex for units tracking turn mode, unlike other units.
        if (entity.usesTurnMode() && getMpUsed() > 5) {
            int turnMode = getMpUsed() / 5;
            int nStraight = 0;
            MoveStep prevStep = steps.get(0);
            for (MoveStep s : steps) {
                if (s.isTurning() && nStraight < turnMode) {
                    prevStep.setDanger(true);
                }
                nStraight = s.getNStraight();
                prevStep = s;
            }
        }
        
        // If running on pavement we don't know to mark the danger steps if we turn before expending
        // enough MP to require running movement.
        if (steps.size() > 1) {
            MoveStep lastStep = steps.get(steps.size() - 1);
            MoveStep prevStep = steps.get(0);
            for (MoveStep s : steps) {
                if (s.getMovementType(false) == EntityMovementType.MOVE_ILLEGAL) {
                    break;
                }
                s.setDanger(s.isDanger() || Compute.isPilotingSkillNeeded(game, entity.getId(),
                        prevStep.getPosition(), s.getPosition(), lastStep.getMovementType(true),
                        prevStep.isTurning(), prevStep.isPavementStep(), prevStep.getElevation(),
                                s.getElevation(), s));
                s.setPastDanger(s.isPastDanger() || s.isDanger());
                prevStep = s;
            }
        }
        
        if(step.useAeroAtmosphere(game, entity) 
        		&& game.getBoard().onGround()											//we're an aerospace unit on a ground map
        		&& step.getPosition() != null  											//null
        		&& game.getFirstEnemyEntity(step.getPosition(), entity) != null) {
        	fliesOverEnemy = true;
        }

        return this;
    }

    public void compile(final IGame g, final Entity en) {
        compile(g, en, true);
    }

    public void compile(final IGame g, final Entity en, boolean clip) {
        setGame(g);
        setEntity(en);
        final Vector<MoveStep> temp = new Vector<MoveStep>(steps);
        steps.removeAllElements();
        for (int i = 0; i < temp.size(); i++) {
            MoveStep step = temp.elementAt(i);
            if ((step.getTargetPosition() != null) && (step.getTarget(getGame()) != null)) {
                step = new MoveStep(this, step.getType(), step.getTarget(getGame()), step.getTargetPosition());
            } else if (step.getTarget(getGame()) != null) {
                step = new MoveStep(this, step.getType(), step.getTarget(getGame()));
            } else if (step.getRecoveryUnit() != -1) {
                step = new MoveStep(this, step.getType(), step.getRecoveryUnit(), -1);
            } else if (step.getMineToLay() != -1) {
                step = new MoveStep(this, step.getType(), step.getMineToLay());
            } else if (step.getLaunched().size() > 0) {
                step = new MoveStep(this, step.getType(), step.getLaunched());
            } else if (step.getManeuverType() != ManeuverType.MAN_NONE) {
                step = new MoveStep(this, step.getType(), -1, -1, step.getManeuverType());
            } else if (step.isManeuver()) {
                step = new MoveStep(this, step.getType(), step.hasNoCost(), step.isManeuver());
            } else if (step.hasNoCost()) {
                step = new MoveStep(this, step.getType(), step.hasNoCost());
            } else if (null != step.getMinefield()) {
                step = new MoveStep(this, step.getType(), step.getMinefield());
            } else {
                step = new MoveStep(this, step.getType());
            }
            this.addStep(step);
        }

        // Can't move out of a hex with an enemy unit unless we started
        // there, BUT we're allowed to turn, unload/disconnect, or go prone.
        Coords pos = getEntity().getPosition();
        boolean isMech = getEntity() instanceof Mech;
        int elev = getEntity().getElevation();
        if (Compute.isEnemyIn(getGame(), getEntity(), pos, false, isMech, elev)) {
            // There is an enemy, can't go out and back in, and go out again
            boolean left = false;
            boolean returned = false;
            for (MoveStep step : steps) {
                if (!left) {
                    if (!step.getPosition().equals(getEntity().getPosition())
                        || !(step.getElevation() == getEntity().getElevation())) {
                        // we left the location
                        left = true;
                        continue;
                    }
                    continue;
                }
                if (!returned) {
                    if (step.getPosition().equals(getEntity().getPosition())
                        && (step.getElevation() == getEntity().getElevation())) {
                        // we returned to the location
                        returned = true;
                        continue;
                    }
                    continue;
                }
                // We've returned, only following 5 types are legal
                if ((step.getType() != MovePath.MoveStepType.TURN_LEFT)
                        && (step.getType() != MovePath.MoveStepType.TURN_RIGHT)
                        && (step.getType() != MovePath.MoveStepType.UNLOAD)
                        && (step.getType() != MovePath.MoveStepType.DISCONNECT)
                        && (step.getType() != MovePath.MoveStepType.GO_PRONE)) {
                    // we only need to identify the first illegal move
                    step.setMovementType(EntityMovementType.MOVE_ILLEGAL);
                    break;
                }
            }
        }
        
        if (getEntity() instanceof LandAirMech
                && !((LandAirMech)getEntity()).canConvertTo(getFinalConversionMode())) {
            steps.forEach(s -> {
                if (s.getType() == MoveStepType.CONVERT_MODE) {
                    s.setMovementType(EntityMovementType.MOVE_ILLEGAL);
                }
            });
        }

        if (clip) {
            clipToPossible();
        }
    }

    public void removeLastStep() {
        if (steps.size() > 0) {
            final MoveStep step1 = getStep(steps.size() - 1);

            if (step1.getType() == MovePath.MoveStepType.START_JUMP) {
                getEntity().setIsJumpingNow(false);
            }
            
            steps.removeElementAt(steps.size() - 1);
            
            if (getEntity().isConvertingNow() && !this.contains(MovePath.MoveStepType.CONVERT_MODE)) {
                getEntity().setConvertingNow(false);
                //Mechs using tracks have the movement mode set at the beginning of the turn, so
                //it will need to be reset.
                if (getEntity() instanceof Mech && ((Mech)getEntity()).hasTracks()) {
                    getEntity().toggleConversionMode();
                }
            }
            
            //Treat multiple convert steps as a single command
            if (step1.getType() == MovePath.MoveStepType.CONVERT_MODE)
                while (steps.size() > 0
                    && steps.get(steps.size() - 1).getType() == MovePath.MoveStepType.CONVERT_MODE) {
                steps.removeElementAt(steps.size() - 1);
            }
            
            // if this step is part of a manuever, undo the whole manuever, all the way to the beginning.
            if(step1.isManeuver()) {
                int stepIndex = steps.size() - 1;
                
                while (steps.size() > 0 && steps.get(stepIndex).isManeuver()) {
                    steps.removeElementAt(stepIndex);
                    stepIndex--;
                }
                
                // a maneuver begins with a "maneuver" step, so get rid of that as well
                steps.removeElementAt(stepIndex);
            }
        }

        // Find the new last step in the path.
        int index = steps.size() - 1;
        while ((index >= 0) && getStep(index).setEndPos(true)
                && !getStep(index).isLegal(this)) {
            index--;
        }
        
        // we may have removed a lot of steps - recalculate the contained step types
        regenerateStepTypes();
    }

    public void clear() {
        steps.removeAllElements();
    }

    public Enumeration<MoveStep> getSteps() {
        return steps.elements();
    }

    public MoveStep getStep(final int index) {
        if ((index < 0) || (index >= steps.size())) {
            return null;
        }
        return steps.elementAt(index);
    }

    /**
     * Helper function that rebuilds the "contained step types" from scratch.
     * Loops over all the steps in the path, so should only be used when removing or replacing steps.
     */
    private void regenerateStepTypes() {
        containedStepTypes.clear();
        for(MoveStep step : steps) {
            containedStepTypes.add(step.getType());
        }
    }
    
    /**
     * Check for any of the specified type of step in the path
     * @param type The step type to check for
     * @return Whether or not this step type is contained within this path 
     */
    public boolean contains(final MoveStepType type) {
        return containedStepTypes.contains(type);
    }
    
    /**
     * Convenience function to determine whether this path results in the unit explicitly moving off board
     * More relevant for aircraft
     * @return Whether or not this path will result in the unit moving off board
     */
    public boolean fliesOffBoard() {
    	return contains(MoveStepType.OFF) || 
    	        contains(MoveStepType.RETURN) || 
    	        contains(MoveStepType.FLEE);
    }
    
    /**
     * Whether any of the steps in the path (except for the last one, based on experimentation)
     * pass over an enemy unit eligible for targeting. Useful for aerotech units.
     * Note that this is only useful for debugging against stationary targets, as it "may" become inaccurate
     * if the target moves.
     * @return Whether or not this flight path takes us over an enemy unit
     */
    public boolean getFliesOverEnemy() {
    	return fliesOverEnemy;
    }
    
    /**
     * Method that determines whether a given path goes through a given set of x/y coordinates
     * Useful for debugging mainly.
     * Note that battletech map coordinates begin at 1, while the internal representation begins at 0
     * so subtract 1 from each axis to get the actual coordinates.
     * @param x X-coordinate
     * @param y Y-coordinate
     * @return Whether this path goes through the set of coordinates.
     */
    public boolean goesThroughCoords(int x, int y) {
        Enumeration<MoveStep> steps = getSteps();
        while(steps.hasMoreElements()) {
            MoveStep step = steps.nextElement();
            if(step.getPosition().getX() == x && step.getPosition().getY() == y) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Check for the presence of any step type that's not the specified step type in the move path
     * @param type The step type to check for
     * @return Whether or not there are any other step types 
     */
    public boolean containsAnyOther(final MoveStepType type) {
    	for(Iterator<MoveStepType> iter = containedStepTypes.iterator(); iter.hasNext();) {
    		if(iter.next() != type)
				return true;
    	}
    	
    	return false;
    }

    /**
     * Check for MASC use
     */
    public boolean hasActiveMASC() {
        for (final Enumeration<MoveStep> i = getSteps(); i.hasMoreElements(); ) {
            final MoveStep step = i.nextElement();
            if (step.isUsingMASC()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the final coordinates if a mech were to perform all the steps in
     * this path.
     */
    public Coords getFinalCoords() {
        if(getGame().useVectorMove()) {
            return Compute.getFinalPosition(getEntity().getPosition(), getFinalVectors());
        }
        
        if (getLastStep() != null) {
            return getLastStep().getPosition();
        }
        
        return getEntity().getPosition();
    }

    /**
     * Returns the starting {@link Coords} of this path.
     */
    @Nullable
    public Coords getStartCoords() {
        for (final Enumeration<MoveStep> e = getSteps(); e.hasMoreElements(); ) {
            final MoveStep step = e.nextElement();
            final Coords coords = step.getPosition();
            if (coords != null) {
                return coords;
            }
        }
        return null;
    }

    /**
     * Returns the final facing if a mech were to perform all the steps in this
     * path.
     */
    public int getFinalFacing() {
        MoveStep last = getLastStep();
        if (last != null) {
            return last.getFacing();
        }
        return getEntity().getFacing();
    }

    /**
     * Returns whether or not a unit would end up prone after all of the steps
     */
    public boolean getFinalProne() {
        if (getLastStep() != null) {
            return getLastStep().isProne();
        }
        if (getEntity() == null) {
            return false;
        }
        return getEntity().isProne();
    }

    /**
     * Returns whether or not a unit would end up hull-down after all of the steps
     */
    public boolean getFinalHullDown() {
        if (getLastStep() != null) {
            return getLastStep().isHullDown();
        }
        if (getEntity() == null) {
            return false;
        }
        return getEntity().isHullDown();
    }

    /**
     * Returns whether or not a unit would be in climb mode after all the steps
     */
    public boolean getFinalClimbMode() {
        if (getLastStep() != null) {
            return getLastStep().climbMode();
        }
        if (getEntity() == null) {
            return false;
        }
        return getEntity().climbMode();
    }

    /**
     * get final elevation relative to the hex.
     */
    public int getFinalElevation() {
        if (getLastStep() != null) {
            return getLastStep().getElevation();
        }
        return getEntity().getElevation();
    }
    
    /**
     * get final elevation relative to the tops of any buildings in the hex
     * @return
     */
    public int getFinalClearance() {
        if (getLastStep() != null) {
            return getLastStep().getClearance();
        }
        IHex hex = entity.getGame().getBoard().getHex(getEntity().getPosition());
        if (hex.containsTerrain(Terrains.BLDG_ELEV)) {
            return getEntity().getElevation() - hex.terrainLevel(Terrains.BLDG_ELEV);
        }
        return getEntity().getElevation();
    }

    /**
     * Returns the highest elevation in the current path
     *
     * @return
     */
    public int getMaxElevation() {
        int maxElev = 0;
        for (MoveStep step : steps) {
            maxElev = Math.max(maxElev,
                    getGame().getBoard().getHex(step.getPosition()).getLevel());
        }
        return maxElev;
    }

    /**
     * get final altitude
     */
    public int getFinalAltitude() {
        if (getLastStep() != null) {
            return getLastStep().getAltitude();
        }
        return getEntity().getAltitude();
    }

    public int getFinalVelocity() {
        if (getLastStep() != null) {
            return getLastStep().getVelocity();
        }
        if (getEntity().isAero()) {
            return ((IAero) getEntity()).getCurrentVelocity();
        }
        return 0;
    }
    
    public int getFinalVelocityLeft() {
        if (getLastStep() != null) {
            return getLastStep().getVelocityLeft();
        }
        if (getEntity().isAero()) {
            return ((IAero) getEntity()).getCurrentVelocity();
        }
        return 0;
    }

    public int getFinalNDown() {
        if (getLastStep() != null) {
            return getLastStep().getNDown();
        }

        return 0;
    }
    
    /**
     * If the path contains mode conversions, this will determine the movement mode at the end
     * of movement. Note that LAMs converting from AirMech to Biped mode require two convert commands.
     * 
     * @return The movement mode resulting from any mode conversions in the path.
     */
    public EntityMovementMode getFinalConversionMode() {
        if (getLastStep() != null) {
            return getLastStep().getMovementMode();
        }
        return getEntity().getMovementMode();
    }

    /**
     * Returns the final vector for advanced movement
     */
    public int[] getFinalVectors() {
        if (getLastStep() != null) {
            return getLastStep().getVectors();
        }
        return getEntity().getVectors();
    }

    public EntityMovementType getLastStepMovementType() {
        if (getLastStep() == null) {
            return EntityMovementType.MOVE_NONE;
        }
        return getLastStep().getMovementType(true);
    }

    public Vector<MoveStep> getStepVector() {
        return steps;
    }

    public MoveStep getLastStep() {
        for (int i = getStepVector().size() - 1; i >= 0; i--) {
            MoveStep last = getStepVector().get(i);
            if (last != null) {
                return last;
            }
        }
        return null;
    }

    public MoveStep getSecondLastStep() {
        if (steps.size() > 1) {
            return getStep(steps.size() - 2);
        }
        return getLastStep();
    }

    /* Debug method */
    public void printAllSteps() {
        System.out.println("*Steps*");
        for (int i = 0; i < steps.size(); i++) {
            System.out.println("  " + i + ": " + getStep(i) + ", " + getStep(i).getMovementType(i == (steps.size() - 1)));
        }
    }

    /**
     * Removes impossible steps.
     */
    public void clipToPossible() {
        if (steps.size() == 0) {
            // nothing to clip
            return;
        }
        // Do final check for bad moves, and clip movement after first bad one
        // also clear and re-constitute "contained steps" cache
        containedStepTypes = new HashSet<>();
        final Vector<MoveStep> goodSteps = new Vector<>();
        for (MoveStep step : steps) {
            if (step.getMovementType(isEndStep(step)) != EntityMovementType.MOVE_ILLEGAL) {
                containedStepTypes.add(step.getType());
                goodSteps.addElement(step);
            } else {
                break;
            }

        }
        steps = goodSteps;
    }

    /**
     * Changes turn-forwards-opposite-turn sequences into quad lateral shifts.
     * <p/>
     * Finds the sequence of three steps that can be transformed, then removes
     * all three and replaces them with the lateral shift step.
     */
    @SuppressWarnings("unused")
    private void transformLateralShift() {
        if (steps.size() < 3) {
            return;
        }
        final int index = steps.size() - 3;
        final MoveStep step1 = getStep(index);
        final MoveStep step2 = getStep(index + 1);
        final MoveStep step3 = getStep(index + 2);

        if (step1.oppositeTurn(step3)
                && ((step2.getType() == MovePath.MoveStepType.BACKWARDS) || (step2.getType() == MovePath.MoveStepType.FORWARDS))) {
            final MoveStepType stepType = step1.getType();
            final MoveStepType direction = step2.getType();
            // remove all old steps
            steps.removeElementAt(index);
            steps.removeElementAt(index);
            steps.removeElementAt(index);
            // add new step
            final MoveStep shift = new MoveStep(this, lateralShiftForTurn(stepType, direction));
            addStep(shift);
        }
    }

    /**
     * Returns the lateral shift that corresponds to the turn direction
     */
    public static MoveStepType lateralShiftForTurn(final MoveStepType turn, final MoveStepType direction) {
        if (direction == MoveStepType.FORWARDS) {
            switch (turn) {
                case TURN_LEFT:
                    return MoveStepType.LATERAL_LEFT;
                case TURN_RIGHT:
                    return MoveStepType.LATERAL_RIGHT;
                default:
                    return turn;
            }
        }
        switch (turn) {
            case TURN_LEFT:
                return MoveStepType.LATERAL_LEFT_BACKWARDS;
            case TURN_RIGHT:
                return MoveStepType.LATERAL_RIGHT_BACKWARDS;
            default:
                return turn;
        }
    }

    /**
     * Returns the turn direction that corresponds to the lateral shift
     */
    static MoveStepType turnForLateralShift(final MoveStepType shift) {
        switch (shift) {
            case LATERAL_LEFT:
                return MoveStepType.TURN_LEFT;
            case LATERAL_RIGHT:
                return MoveStepType.TURN_RIGHT;
            case LATERAL_LEFT_BACKWARDS:
                return MoveStepType.TURN_LEFT;
            case LATERAL_RIGHT_BACKWARDS:
                return MoveStepType.TURN_RIGHT;
            default:
                return shift;
        }
    }

    /**
     * Returns the direction (either MovePath.MoveStepType.TURN_LEFT or
     * MoveStepType.TURN_RIGHT) that the destination facing lies in.
     */
    public static MoveStepType getDirection(final int facing, final int destFacing) {
        final int rotate = (destFacing + (6 - facing)) % 6;
        return rotate >= 3 ? MoveStepType.TURN_LEFT : MoveStepType.TURN_RIGHT;
    }

    /**
     * Returns the adjusted facing, given the start facing.
     */
    public static int getAdjustedFacing(final int facing, final MoveStepType movement) {
        if (movement == MoveStepType.TURN_RIGHT) {
            return (facing + 1) % 6;
        } else if (movement == MoveStepType.TURN_LEFT) {
            return (facing + 5) % 6;
        }
        return facing;
    }

    /**
     * Returns the number of MPs used in the path
     */
    public int getMpUsed() {
        if (getLastStep() != null) {
            return getLastStep().getMpUsed();
        }
        return 0;
    }

    /**
     * Returns the logical number of hexes moved the path (does not count turns,
     * etc).
     */
    public int getHexesMoved() {
        if (getLastStep() == null) {
            return 0;
        }
        return getLastStep().getDistance();
    }

    /**
     * Returns true if the entity is jumping or if it's a flying lam.
     */
    public boolean isJumping() {
        return contains(MoveStepType.START_JUMP);
    }
    
    /**
     * @return true if the entity is a QuadVee or LAM changing movement mode
     */
    public boolean isChangingMode() {
        return contains(MoveStepType.CONVERT_MODE);
    }

    /**
     * Extend the current path to the destination <code>Coords</code>, moving
     * only in one direction.  This method works by applying the supplied move
     * step as long as it moves closer to the destination.  If the destination
     * cannot be reached solely by the provided movestep, the pathfinder will
     * quit once it gets as closer as it can.
     *
     * @param dest      the destination <code>Coords</code> of the move.
     * @param type      the type of movement step required.
     * @param direction the direction of movement.
     */
    public void findSimplePathTo(final Coords dest, final MoveStepType type,
                                 int direction, int facing) {
        Coords src = getFinalCoords();
        Coords currStep = src;
        Coords nextStep = currStep.translated(direction);
        while (dest.distance(nextStep) < dest.distance(currStep)) {
            addStep(type);
            currStep = nextStep;
            nextStep = currStep.translated(direction);
        }

        // Did we reach the destination?  If not, try another direction
        if (!currStep.equals(dest)) {
            int dir = currStep.direction(dest);
            // Java does mod different from how we want...
            dir = (((dir - facing) % 6) + 6) % 6;
            switch (dir) {
                case 0:
                    findSimplePathTo(dest, MoveStepType.FORWARDS, currStep.direction(dest), facing);
                    break;
                case 1:
                    findSimplePathTo(dest, MoveStepType.LATERAL_RIGHT, currStep.direction(dest), facing);
                    break;
                case 2:
                    // TODO: backwards lateral shifts are switched:
                    //  LATERAL_LEFT_BACKWARDS moves back+right and vice-versa
                    findSimplePathTo(dest, MoveStepType.LATERAL_LEFT_BACKWARDS, currStep.direction(dest), facing);
                    break;
                case 3:
                    findSimplePathTo(dest, MoveStepType.BACKWARDS, currStep.direction(dest), facing);
                    break;
                case 4:
                    // TODO: backwards lateral shifts are switched:
                    //  LATERAL_LEFT_BACKWARDS moves back+right and vice-versa
                    findSimplePathTo(dest, MoveStepType.LATERAL_RIGHT_BACKWARDS, currStep.direction(dest), facing);
                    break;
                case 5:
                    findSimplePathTo(dest, MoveStepType.LATERAL_LEFT, currStep.direction(dest), facing);
                    break;
            }
        }
    }

    /**
     * Extend the current path to the destination <code>Coords</code>.
     *
     * @param dest the destination <code>Coords</code> of the move.
     * @param type the type of movement step required.
     */
    public void findPathTo(final Coords dest, final MoveStepType type) {
        final int timeLimit = PreferenceManager.getClientPreferences().getMaxPathfinderTime();

        ShortestPathFinder pf = ShortestPathFinder.newInstanceOfAStar(dest, type, game);

        AbstractPathFinder.StopConditionTimeout<MovePath> timeoutCondition = new AbstractPathFinder.StopConditionTimeout<>(timeLimit);
        pf.addStopCondition(timeoutCondition);

        pf.run(this.clone());
        MovePath finPath = pf.getComputedPath(dest);
        // this can be used for debugging the "destruction aware pathfinder"
        //MovePath finPath = calculateDestructionAwarePath(dest);

        if (timeoutCondition.timeoutEngaged || finPath == null) {
            /*
             * Either we have forced searcher to end prematurely or no path was
             * found. Lets try to fix it by taking the path that ended closest
             * to the target and greedily extend it.
             */
            MovePath bestMp = Collections.min(pf.getAllComputedPaths().values(), new ShortestPathFinder.MovePathGreedyComparator(dest));
            pf = ShortestPathFinder.newInstanceOfGreedy(dest, type, game);
            pf.run(bestMp);
            finPath = pf.getComputedPath(dest);
            // If no path could be found, use the best one returned by A*
            if (finPath == null) {
                finPath = bestMp;
            }
        }
        if (finPath != null) {
            finPath.compile(game, entity, false);
            this.steps = finPath.steps;
        } else {
            System.out.println("Error: " +
                    "Unable to find a path to the destination hex!");
            System.out.println("\tMoving " + getEntity() + "from "
                    + getFinalCoords() + " to " + dest);
        }
    }

    public boolean isMoveLegal() {
        // Moves which end up off of the board are not legal.
        if (!getGame().getBoard().contains(getFinalCoords())) {
            return false;
        }

        // for aero units move must use up all their velocity
        if (getEntity().isAero()) {
            IAero a = (IAero) getEntity();
            if (getLastStep() == null) {
                if ((a.getCurrentVelocity() > 0) && !getGame().useVectorMove()) {
                    return false;
                }
            } else {
                if ((getLastStep().getVelocityLeft() > 0) && !getGame().useVectorMove()
                        && !(getLastStep().getType() == MovePath.MoveStepType.FLEE
                        || getLastStep().getType() == MovePath.MoveStepType.EJECT)) {
                    return false;
                }
            }
        }

        if (getLastStep() == null) {
            return true;
        }

        if (getLastStep().getType() == MoveStepType.CHARGE) {
            return getSecondLastStep().isLegal(this);
        }
        if (getLastStep().getType() == MoveStepType.RAM) {
            return getSecondLastStep().isLegal(this);
        }
        return getLastStep().isLegal(this);
    }

    /**
     * An A* pathfinder to get from the end of the current path (or entity's
     * position if empty) to the destination.
     *
     * @param dest      The goal hex
     * @param type      The type of move we want to do
     * @param timeLimit the maximum <code>int</code> number of milliseconds to take
     *                  hunting for an ideal path.
     */
    @SuppressWarnings("unused")
    private void notSoLazyPathfinder(final Coords dest, final MoveStepType type,
                                     final int timeLimit) {
        final int MAX_CANDIDATES = 100;
        final long endTime = System.currentTimeMillis() + timeLimit;

        MoveStepType step = type;
        if (step != MoveStepType.BACKWARDS) {
            step = MoveStepType.FORWARDS;
        }

        final MovePathComparator mpc =
                new MovePathComparator(dest, step == MoveStepType.BACKWARDS);

        MovePath bestPath = clone();

        // A collection of paths we have already explored
        final HashMap<MovePath.Key, MovePath> discovered =
                new HashMap<MovePath.Key, MovePath>();
        discovered.put(bestPath.getKey(), bestPath);

        // A collection of hte possible next-moves
        final PriorityQueue<MovePath> candidates =
                new PriorityQueue<MovePath>(110, mpc);
        candidates.add(bestPath);

        boolean keepLooping = getFinalCoords().distance(dest) > 1;
        int loopcount = 0;

        // Keep looping while we have candidates to explore, and certain stop
        //  conditions aren't met (time-limit, destination found, etc)
        while ((candidates.size() > 0) && keepLooping) {
            final MovePath candidatePath = candidates.poll();
            final Coords startingPos = candidatePath.getFinalCoords();
            final int startingElev = candidatePath.getFinalElevation();

            // Check to see if we have found the destination
            if (candidatePath.getFinalCoords().distance(dest) == 0) {
                bestPath = candidatePath;
                keepLooping = false;
                break;
            }

            // Get next possible steps
            final Iterator<MovePath> adjacent =
                    candidatePath.getNextMoves(step == MoveStepType.BACKWARDS,
                            step == MoveStepType.FORWARDS).iterator();
            // Evaluate possible next steps
            while (adjacent.hasNext()) {
                final MovePath expandedPath = adjacent.next();

                if (expandedPath.getLastStep().isMovementPossible(getGame(),
                        startingPos, startingElev, getCachedEntityState())) {

                    if (discovered.containsKey(expandedPath.getKey())) {
                        continue;
                    }
                    // Make sure the candidate list doesn't get too big
                    if (candidates.size() <= MAX_CANDIDATES) {
                        candidates.add(expandedPath);
                    }
                    discovered.put(expandedPath.getKey(), expandedPath);
                }
            }
            // If we're doing a special movement, like charging or DFA, we will
            //  have to take extra steps to see if we can finish off the move
            //  this is because getNextMoves only considers turning and
            //  forward/backward movement
            if (type == MoveStepType.CHARGE ||
                    type == MoveStepType.DFA){
                MovePath expandedPath = candidatePath.clone();
                expandedPath.addStep(type);
                if (expandedPath.getLastStep().isMovementPossible(getGame(),
                        startingPos, startingElev, getCachedEntityState())) {

                    if (discovered.containsKey(expandedPath.getKey())) {
                        continue;
                    }
                    // Make sure the candidate list doesn't get too big
                    if (candidates.size() <= MAX_CANDIDATES) {
                        candidates.add(expandedPath);
                    }
                    discovered.put(expandedPath.getKey(), expandedPath);
                }
            }


            loopcount++;
            if (((loopcount % 256) == 0) && keepLooping
                    && (candidates.size() > 0)) {
                final MovePath front = candidates.peek();
                if (front.getFinalCoords().distance(dest) < bestPath
                        .getFinalCoords().distance(dest)) {
                    bestPath = front;
                }
                if (System.currentTimeMillis() > endTime) {
                    keepLooping = false;
                    System.out.println("Time limit reached searching " +
                            "for path!");
                }
            }
        } // end while
        //System.out.println("iteration count: " + loopcount);
        if (getFinalCoords().distance(dest) > bestPath.getFinalCoords().distance(dest)) {
            // Make the path we found, this path.
            steps = bestPath.steps;
        }
        if (!getFinalCoords().equals(dest)) {
            lazyPathfinder(dest, type);
        }
    }

    /**
     * Find the shortest path to the destination <code>Coords</code> by hex
     * count. This right choice <em>only</em> when making a simple move like a
     * straight line or one with a single turn.
     *
     * @param dest the destination <code>Coords</code> of the move.
     * @param type the type of movment step required.
     */
    private void lazyPathfinder(final Coords dest, final MoveStepType type) {
        MoveStepType step = MoveStepType.FORWARDS;
        if (type == MoveStepType.BACKWARDS) {
            step = MoveStepType.BACKWARDS;
        }
        Coords subDest = dest;
        if (!dest.equals(getFinalCoords())) {
            subDest = dest.translated(dest.direction(getFinalCoords()));
        }

        while (!getFinalCoords().equals(subDest)) {
            // adjust facing
            rotatePathfinder((getFinalCoords().direction(subDest) + (step == MoveStepType.BACKWARDS ? 3 : 0)) % 6,
                    false);
            // step forwards
            addStep(step);
        }
        rotatePathfinder((getFinalCoords().direction(dest) + (step == MoveStepType.BACKWARDS ? 3 : 0)) % 6, false);
        if (!dest.equals(getFinalCoords())) {
            addStep(type);
        }
    }

    /**
     * Returns a list of possible moves that result in a
     * facing/position/(jumping|prone) change, special steps (mine clearing and
     * such) must be handled elsewhere.
     */
    public List<MovePath> getNextMoves(boolean backward, boolean forward) {
        final ArrayList<MovePath> result = new ArrayList<MovePath>();
        final MoveStep last = getLastStep();
//        if (isJumping()) {
//            final MovePath left = clone();
//            final MovePath right = clone();
//
//            // From here, we can move F, LF, RF, LLF, RRF, and RRRF.
//            result.add(clone().addStep(MovePath.MoveStepType.FORWARDS));
//            for (int turn = 0; turn < 2; turn++) {
//                left.addStep(MovePath.MoveStepType.TURN_LEFT);
//                right.addStep(MovePath.MoveStepType.TURN_RIGHT);
//                result.add(left.clone().addStep(MovePath.MoveStepType.FORWARDS));
//                result.add(right.clone().addStep(MovePath.MoveStepType.FORWARDS));
//            }
//            right.addStep(MovePath.MoveStepType.TURN_RIGHT);
//            result.add(right.addStep(MovePath.MoveStepType.FORWARDS));
//
//            // We've got all our next steps.
//            return result;
//        }

        // need to do a separate section here for Aeros.
        // just like jumping for now, but I could add some other stuff
        // here later
        if (getEntity().isAero()) {
            MovePath left = clone();
            MovePath right = clone();

            // From here, we can move F, LF, RF, LLF, RRF, and RRRF.
            result.add((clone()).addStep(MovePath.MoveStepType.FORWARDS));
            for (int turn = 0; turn < 2; turn++) {
                left.addStep(MovePath.MoveStepType.TURN_LEFT);
                right.addStep(MovePath.MoveStepType.TURN_RIGHT);
                result.add(left.clone().addStep(MovePath.MoveStepType.FORWARDS));
                result.add(right.clone().addStep(MovePath.MoveStepType.FORWARDS));
            }
            right.addStep(MovePath.MoveStepType.TURN_RIGHT);
            result.add(right.addStep(MovePath.MoveStepType.FORWARDS));

            // We've got all our next steps.
            return result;
        }

        // If the unit is prone or hull-down it limits movement options, unless
        //  it's a tank; tanks can just drive out of hull-down and they cannot
        //  be prone.
        if (getFinalProne() || (getFinalHullDown() && !(getEntity() instanceof Tank))) {
            if ((last != null) && (last.getType() != MoveStepType.TURN_RIGHT)) {
                result.add(clone().addStep(MovePath.MoveStepType.TURN_LEFT));
            }
            if ((last != null) && (last.getType() != MoveStepType.TURN_LEFT)) {
                result.add(clone().addStep(MovePath.MoveStepType.TURN_RIGHT));
            }

            if (getEntity().isCarefulStand()) {
                result.add(clone().addStep(MovePath.MoveStepType.CAREFUL_STAND));
            } else {
                result.add(clone().addStep(MovePath.MoveStepType.GET_UP));
            }
            return result;
        }
        if (canShift()) {
            if (forward && (!backward || ((last == null) || (last.getType() != MovePath.MoveStepType.LATERAL_LEFT)))) {
                result.add(clone().addStep(MoveStepType.LATERAL_RIGHT));
            }
            if (forward && (!backward || ((last == null) || (last.getType() != MovePath.MoveStepType.LATERAL_RIGHT)))) {
                result.add(clone().addStep(MovePath.MoveStepType.LATERAL_LEFT));
            }
            if (backward
                    && (!forward || ((last == null) || (last.getType() != MovePath.MoveStepType.LATERAL_LEFT_BACKWARDS)))) {
                result.add(clone().addStep(MovePath.MoveStepType.LATERAL_RIGHT_BACKWARDS));
            }
            if (backward
                    && (!forward || ((last == null) || (last.getType() != MovePath.MoveStepType.LATERAL_RIGHT_BACKWARDS)))) {
                result.add(clone().addStep(MovePath.MoveStepType.LATERAL_LEFT_BACKWARDS));
            }
        }
        if (forward && (!backward || ((last == null) || (last.getType() != MovePath.MoveStepType.BACKWARDS)))) {
            result.add(clone().addStep(MovePath.MoveStepType.FORWARDS));
        }
        if ((last == null) || (last.getType() != MovePath.MoveStepType.TURN_LEFT)) {
            result.add(clone().addStep(MovePath.MoveStepType.TURN_RIGHT));
        }
        if ((last == null) || (last.getType() != MovePath.MoveStepType.TURN_RIGHT)) {
            result.add(clone().addStep(MovePath.MoveStepType.TURN_LEFT));
        }
        if (backward && (!forward || ((last == null) || (last.getType() != MovePath.MoveStepType.FORWARDS)))) {
            result.add(clone().addStep(MovePath.MoveStepType.BACKWARDS));
        }
        return result;
    }

    /**
     * Clones this path, will contain a new clone of the steps so that the clone
     * is independent from the original.
     *
     * @return the cloned MovePath
     */
    @Override
    public MovePath clone() {
        final MovePath copy = new MovePath(getGame(), getEntity());
        copyFields(copy);
        return copy;
    }
    
    protected void copyFields(MovePath copy) {
        copy.steps = new Vector<MoveStep>(steps);
        copy.careful = careful;
        copy.containedStepTypes = new HashSet<>(containedStepTypes);
        copy.fliesOverEnemy = fliesOverEnemy;
        copy.cachedEntityState = cachedEntityState; // intentional pointer copy
    }

    /**
     * Rotate from the current facing to the destination facing.
     */
    public void rotatePathfinder(final int destFacing, final boolean isManeuver) {
        while (getFinalFacing() != destFacing) {
            final MoveStepType stepType = getDirection(getFinalFacing(), destFacing);
            addStep(stepType, isManeuver, isManeuver);
        }
    }

    /**
     * Returns true if a jump using mechanical jump boosters would cause falling
     * damage.  Mechanical jump boosters are only designed to handle the stress
     * of falls from a height equal to their jumpMP; if a jump has a fall that
     * is further than the jumpMP of the unit, fall damage applies.
     *
     * @return
     */
    public boolean shouldMechanicalJumpCauseFallDamage() {
        if (isJumping() && (getEntity().getJumpType() == Mech.JUMP_BOOSTER) &&
            (getJumpMaxElevationChange() > getEntity().getJumpMP())) {
            return true;
        }
        return false;
    }

    /**
     * Returns the highest elevation along a jump path.
     *
     * @return
     */
    public Coords getJumpPathHighestPoint() {
        Coords highestCoords = null;
        int highestElevation = 0;
        for (MoveStep step : steps) {
            if (getGame().getBoard().getHex(step.getPosition()).getLevel() > highestElevation) {
                highestElevation = step.getElevation();
                highestCoords = step.getPosition();
            }
        }
        return highestCoords;
    }

    /**
     * Returns the distance between the highest elevation in the jump path and
     * the elevation at the landing point.  This gives the largest distance the
     * unit has fallen during the jump.
     */
    public int getJumpMaxElevationChange() {
        return getMaxElevation() -
               getGame().getBoard().getHex(getFinalCoords()).getLevel();
    }

    /**
     * @return TRUE if there are any buildings in a dropship's landing zone.
     */
    public boolean willCrushBuildings() {
        for (MoveStep step : steps) {
            if (!step.getCrushedBuildingLocs().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Airborne WiGEs that move less than five hexes (four for glider protomech) in a movement phase must
     * land unless it has taken off in the same phase or it is a LAM or glider ProtoMech that is using hover
     * movement.
     * 
     * @param includeMovePathHexes  Whether to include the hexes plotted in this MovePath in the total distance
     *                              moved. This should be true when plotting movement in the client and
     *                              false when the server checks for automatic landing at the end of movement. 
     * @return whether the unit is an airborne WiGE that must land at the end of movement.
     */
    public boolean automaticWiGELanding(boolean includeMovePathHexes) {
        if (getEntity().getMovementMode() != EntityMovementMode.WIGE
                || getEntity().isAirborne()) {
            return false;
        }
        // A LAM converting from AirMech to Mech mode automatically lands at the end of movement.
        if ((getEntity() instanceof LandAirMech)
                && (((LandAirMech)getEntity()).getConversionModeFor(getFinalConversionMode()) == LandAirMech.CONV_MODE_MECH)){
            if (getLastStep() != null) {
                return getLastStep().getClearance() > 0;
            } else {
                return getEntity().isAirborneVTOLorWIGE();
            }
        }
        // If movement has been interrupted (such as by a sideslip) and remaining movement points have
        // been spent, this MovePath only contains the hexes moved after the interruption. The hexes already
        // moved this turn are in delta_distance. WHen the server checks at the end of movement, delta_distance
        // already includes the hexes in this MovePath.
        int moved = getEntity().delta_distance;
        if (includeMovePathHexes) {
            moved += getHexesMoved();
        }
        if ((moved >= 5)
                || (getEntity().hasETypeFlag(Entity.ETYPE_PROTOMECH)
                        && moved == 4)) {
            return false;
        }
        if (getEntity().wigeLiftoffHover() || steps.stream().map(s -> s.getType())
                .anyMatch(st -> st == MoveStepType.UP
                        || st == MoveStepType.HOVER)) {
            return false;
        }
        if (getLastStep() != null) {
            return getLastStep().getClearance() > 0;
        } else {
            return getEntity().isAirborneVTOLorWIGE();
        }
    }
    
    protected static class MovePathComparator implements Comparator<MovePath> {
        private final Coords destination;
        boolean backward;

        public MovePathComparator(final Coords destination, final boolean backward) {
            this.destination = destination;
            this.backward = backward;
        }

        public int compare(final MovePath first, final MovePath second) {
            final int firstDist = first.getMpUsed() + first.getFinalCoords().distance(destination)
                    + getFacingDiff(first);
            final int secondDist = second.getMpUsed() + second.getFinalCoords().distance(destination)
                    + getFacingDiff(second);
            return firstDist - secondDist;
        }

        private int getFacingDiff(final MovePath first) {
            if (first.isJumping()) {
                return 0;
            }
            if (first.getFinalCoords().equals(destination)) {
                return 0;
            }
            int firstFacing = Math.abs(((first.getFinalCoords().direction(destination) + (backward ? 3 : 0)) % 6)
                    - first.getFinalFacing());
            if (firstFacing > 3) {
                firstFacing = 6 - firstFacing;
            }
            if (first.canShift()) {
                firstFacing = Math.max(0, firstFacing - 1);
            }
            if ((first.getFinalCoords().degree(destination) % 60) != 0) {
                firstFacing++;
            }
            return firstFacing;
        }
    }

    /*
     * Get the position in the step immediately prior to the final position
     */
    public Coords getSecondFinalPosition(Coords startPos) {

        Coords priorPos = startPos;
        Coords finalPos = getFinalCoords();

        // if we moved one or fewer hexes, then just return starting position
        if (getHexesMoved() < 2) {
            return priorPos;
        }

        for (final Enumeration<MoveStep> i = getSteps(); i.hasMoreElements(); ) {
            final MoveStep step = i.nextElement();
            if (!step.getPosition().equals(finalPos)) {
                priorPos = step.getPosition();
            }
        }
        return priorPos;

    }

    public boolean isCareful() {
        return careful;
    }

    public void setCareful(boolean b) {
        careful = b;
    }

    public int countMp(boolean jumping) {
        int mp = 0;
        for (MoveStep step : steps) {
            if (jumping && (step.getType() != MoveStepType.TURN_LEFT) &&
                    (step.getType() != MoveStepType.TURN_RIGHT)) {
                mp += step.getMp();
            } else if (!jumping) {
                mp += step.getMp();
            }
        }
        return mp;
    }

    public void addSteps(Vector<MoveStep> path, boolean compile) {
        for (MoveStep step : path) {
            addStep(step, compile);
        }
    }

    public void replaceSteps(Vector<MoveStep> path) {
        steps.clear();
        addSteps(path, true);
    }
    
    public boolean isEndStep(MoveStep step) {
        if (step == null) {
            return false;
        }
        return step.isEndPos(this);
    }
    
    /**
     * Convenience method to determine whether this path is happening on a ground map with an atmosphere
     */
    public boolean isOnAtmosphericGroundMap() {
    	return getEntity().isOnAtmosphericGroundMap(); 
    }
    
    /**
     * Searches the movement path for the first step that has the given position and sets it as
     * a VTOL bombing step. If found, any previous bombing step is cleared. If the coordinates are not
     * part of the path nothing is changed.
     * 
     * @param pos The <code>Coords</code> of the hex to be bombed.
     * @return Whether the position was found in the movement path
     */
    public boolean setVTOLBombStep(Coords pos) {
        boolean foundPos = false;
        MoveStep prevBombing = null;
        for (MoveStep step : steps) {
            if (step.getPosition().equals(pos)) {
                if (step.isVTOLBombingStep()) {
                    return true;
                } else {
                    step.setVTOLBombing(true);
                    foundPos = true;
                }
            } else if (step.isVTOLBombingStep()) {
                prevBombing = step;
            }
        }
        if (foundPos && prevBombing != null) {
            prevBombing.setVTOLBombing(false);
        }
        return foundPos;
    }
    
    /**
     * Searches the path for the first <code>MoveStep</code> that matches the given position and sets it
     * as a strafing step. In cases where there are multiple steps with the same coordinates, we want the
     * first one because it is the one that enters the hex. In the rare case where the path crosses
     * itself, select the one closest to the end of the path.
     * 
     * FIXME: this does not deal with paths that cross themselves
     * 
     * @param pos The <code>Coords</code> of the hex to be strafed
     * @return Whether the position was found in the path
     */
    public boolean setStrafingStep(Coords pos) {
        MoveStep found = null;
        for (int i = steps.size() - 1; i >= 0; i--) {
            if (steps.get(i).getPosition().equals(pos)) {
                found = steps.get(i);
            } else if (found != null) {
                found.setStrafing(true);
                return true;
            }
        }
        if (found != null) {
            found.setStrafing(true);
            return true;
        }
        return false;
    }
    
    /**
     * @return A list of entity ids for all units that have previously be plotted to be dropped/launched.
     */
    public Set<Integer> getDroppedUnits() {
        Set<Integer> dropped = new HashSet<>();
        for (MoveStep s : steps) {
            for (Vector<Integer> ids : s.getLaunched().values()) {
                dropped.addAll(ids);
            }
        }
        return dropped;
    }
    
    /**
     * Convenience function encapsulating logic for whether, if we continue forward 
     * along the current path in the current direction, we will run off the board
     * @return
     */
    public boolean nextForwardStepOffBoard() {
        return !game.getBoard().contains(getFinalCoords().translated(getFinalFacing()));
    }
    
    /**
     * Debugging method that calculates a destruction-aware move path to the destination coordinates
     */
    @SuppressWarnings("unused")
    public MovePath calculateDestructionAwarePath(Coords dest) {
        // code that's useful to test the destruction-aware pathfinder
        DestructionAwareDestinationPathfinder dpf = new DestructionAwareDestinationPathfinder();
        // the destruction aware pathfinder takes either a CardinalEdge or an explicit set of coordinates
        Set<Coords> destinationSet = new HashSet<Coords>();
        destinationSet.add(dest);
        
        // debugging code that can be used to find a path to a specific edge
        Princess princess = new Princess("test", "test", 2020, LogLevel.OFF);
        //Set<Coords> destinationSet = princess.getClusterTracker().getDestinationCoords(entity, CardinalEdge.WEST, true);
        
        long marker1 = System.currentTimeMillis();
        MovePath finPath = dpf.findPathToCoords(entity, destinationSet, false, princess.getClusterTracker());
        long marker2 = System.currentTimeMillis();
        long marker3 = marker2 - marker1;
        
        return finPath;
    }
}
