/*
 * Copyright (c) 2000-2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2003-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package megamek.common.moves;

import java.io.Serial;
import java.io.Serializable;
import java.util.*;

import megamek.client.bot.princess.Princess;
import megamek.common.Hex;
import megamek.common.ManeuverType;
import megamek.common.annotations.Nullable;
import megamek.common.board.Board;
import megamek.common.board.BoardLocation;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.enums.MoveStepType;
import megamek.common.equipment.Minefield;
import megamek.common.game.Game;
import megamek.common.options.OptionsConstants;
import megamek.common.pathfinder.CachedEntityState;
import megamek.common.pathfinder.DestructionAwareDestinationPathfinder;
import megamek.common.pathfinder.ShortestPathFinder;
import megamek.common.pathfinder.StopConditionTimeout;
import megamek.common.pathfinder.comparators.MovePathGreedyComparator;
import megamek.common.preference.PreferenceManager;
import megamek.common.rolls.PilotingRollData;
import megamek.common.units.*;
import megamek.logging.MMLogger;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * Holds movement path for an entity.
 */
public class MovePath implements Cloneable, Serializable {
    private static final MMLogger LOGGER = MMLogger.create(MovePath.class);

    @Serial
    private static final long serialVersionUID = -4258296679177532986L;

    private Set<Coords> coordsSet = null;
    private final transient Object COORD_SET_LOCK = new Object();
    private transient CachedEntityState cachedEntityState;
    private final Coords waypoint;

    public Game getGame() {
        return game;
    }

    public void setGame(Game game) {
        this.game = game;
    }

    public void setEntity(Entity entity) {
        this.entity = entity;
        cachedEntityState = new CachedEntityState(entity);
    }

    private Vector<MoveStep> steps = new Vector<>();

    private transient Game game;
    private transient Entity entity;

    // holds the types of steps present in this movement
    private Set<MoveStepType> containedStepTypes = new HashSet<>();

    // whether this movePath take us directly over an enemy unit
    // useful for debugging aircraft on ground maps
    // private boolean fliesOverEnemy;

    public static final int DEFAULT_PATHFINDER_TIME_LIMIT = 500;

    // is this move path being done using careful movement?
    private boolean careful = true;
    private boolean gravityConcern = false;
    private final float gravity;

    /**
     * Denotes a hex that a ground board flight path of an aero on an atmospheric board crosses
     */
    private BoardLocation flightPathHex = BoardLocation.NO_LOCATION;

    /**
     * Generates a new, empty, movement path object.
     */
    public MovePath(final Game game, final Entity entity) {
        this(game, entity, null);
    }

    /**
     * Generates a new, empty, movement path object.
     */
    public MovePath(Game game, Entity entity, @Nullable Coords waypoint) {
        setEntity(entity);
        setGame(game);
        this.waypoint = waypoint;
        // Do we care about gravity when adding steps?
        gravity = game.getPlanetaryConditions().getGravity();
        gravityConcern = ((gravity > 1.0F && cachedEntityState.getJumpMPNoGravity() > 0 ||
              (gravity < 1.0F &&
                    cachedEntityState.getRunMP() > cachedEntityState.getRunMPNoGravity())) &&
              game.getBoard(entity.getBoardId()).isGround() &&
              !entity.isAirborne());
    }

    /**
     * Checks if there is a waypoint referenced by this MovePath.
     *
     * @return true if there is a waypoint, false otherwise.
     */
    public boolean hasWaypoint() {
        return waypoint != null;
    }

    /**
     * Returns the waypoint referenced by this MovePath.
     *
     * @return the waypoint, or null if there is none.
     */
    public @Nullable Coords getWaypoint() {
        return waypoint;
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
        final StringBuilder result = new StringBuilder();
        result.append("[MovePath #").append(getKey().hashCode())
              .append("] Length: ").append(length())
              .append("; To: ").append(getFinalCoords())
              .append(", Board: ").append(getFinalBoardId())
              .append("; ");

        steps.forEach(step -> result.append(step.toString()).append(" "));
        if (game.getBoard(getFinalBoardId()) == null) {
            result.append("Invalid Board!");
        } else if (!getGame().getBoard(getFinalBoardId()).contains(getFinalCoords())) {
            result.append("OUT!");
        }

        return result.toString();
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
     * @param target the <code>Targetable</code> object that is the target of this step. For example, the enemy being
     *               charged.
     */
    public MovePath addStep(final MoveStepType type, final Targetable target) {
        return addStep(new MoveStep(this, type, target));
    }

    public MovePath addStep(final MoveStepType type, final Targetable target, final Coords pos) {
        return addStep(new MoveStep(this, type, target, pos));
    }

    public MovePath addStep(final MoveStepType type, final int additionalIntData) {
        return addStep(new MoveStep(this, type, additionalIntData));
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

    public MovePath addStep(final MoveStepType type, final Map<Integer, Integer> additionalIntData) {
        return addStep(new MoveStep(this, type, additionalIntData));
    }

    public MovePath addStep(final MoveStepType type, final boolean noCost, final boolean isManeuver,
          final int maneuverType) {
        return addStep(new MoveStep(this, type, noCost, isManeuver, maneuverType));
    }

    public MovePath addStep(final MoveStepType type, final Minefield mf) {
        return addStep(new MoveStep(this, type, mf));
    }

    public void addManeuver(final int manType) {
        addStep(new MoveStep(this, MoveStepType.MANEUVER, -1, -1, manType));
    }

    public boolean canShift() {
        return ((getEntity() instanceof QuadMek
              // QuadVee cannot shift in vee mode
              &&
              !(getEntity() instanceof QuadVee &&
                    (entity.getConversionMode() == QuadVee.CONV_MODE_VEHICLE ||
                          getEntity().isConvertingNow())))
              // Maneuvering Ace allows Bipeds and VTOLs moving at cruise
              // speed to perform a lateral shift
              ||
              (getEntity().isUsingManAce() &&
                    ((getEntity() instanceof BipedMek) ||
                          ((getEntity() instanceof VTOL) &&
                                (getMpUsed() <= getCachedEntityState().getWalkMP())))) ||
              (game.getOptions().booleanOption(OptionsConstants.ADVANCED_GROUND_MOVEMENT_VEHICLE_ADVANCED_MANEUVERS) &&
                    getEntity() instanceof Tank &&
                    (getEntity().getMovementMode() == EntityMovementMode.VTOL ||
                          getEntity().getMovementMode() == EntityMovementMode.HOVER)) ||
              ((getEntity() instanceof TripodMek) && (((Mek) getEntity()).countBadLegs() == 0))) &&
              !isJumping();
    }

    /**
     * Returns true if this MovePath contains a lateral shift
     *
     */
    public boolean containsLateralShift() {
        return this.contains(MoveStepType.LATERAL_LEFT) ||
              this.contains(MoveStepType.LATERAL_RIGHT) ||
              this.contains(MoveStepType.LATERAL_LEFT_BACKWARDS) ||
              this.contains(MoveStepType.LATERAL_RIGHT_BACKWARDS);
    }

    public boolean containsVTOLBomb() {
        for (MoveStep step : steps) {
            if (step.isVTOLBombingStep()) {
                return true;
            }
        }
        return false;
    }

    public MovePath addStep(final MoveStep step) {
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
     * Initializes a step as part of this movement path. Then adds it to the list.
     *
     */
    protected MovePath addStep(final MoveStep step, boolean compile) {
        if (step == null) {
            LOGGER.error("", new RuntimeException("Received NULL MoveStep"));
            return this;
        }

        steps.addElement(step);

        final MoveStep prev = getStep(steps.size() - 2);

        if (compile) {
            try {
                step.compile(getGame(), getEntity(), prev, getCachedEntityState());
            } catch (Exception ignored) {
                // N.B. the pathfinding will try steps off the map.
                step.setMovementType(EntityMovementType.MOVE_ILLEGAL);
            }
        }

        // jumping into heavy woods is danger
        if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_GROUND_MOVEMENT_PSR_JUMP_HEAVY_WOODS)) {
            Hex hex = game.getBoard(step.getBoardId()).getHex(step.getPosition());
            if ((hex != null) && isJumping() && step.isEndPos(this)) {
                PilotingRollData psr = entity.checkLandingInHeavyWoods(step.getMovementType(false), hex);
                if (psr.getValue() != PilotingRollData.CHECK_FALSE) {
                    step.setDanger(true);
                }
            }
        }

        final Coords start = getEntity().getPosition();
        final Coords land = step.getPosition();

        if (step.getMovementType(false) != EntityMovementType.MOVE_ILLEGAL) {
            performIllegalCheck(step, start, land);
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
        }

        if (shouldMechanicalJumpCauseFallDamage()) {
            step.setDanger(true);
        }

        // If we are using turn modes, go back through the path and mark danger for any
        // turn
        // that now exceeds turn mode requirement. We want to show danger on the
        // previous step
        // so the StepSprite will show danger. Hiding the previous step instead would
        // make turning costs
        // show in the turning hex for units tracking turn mode, unlike other units.
        if (entity.usesTurnMode() && (getMpUsed() > 5)) {
            int turnMode = getMpUsed() / 5;
            int nStraight = 0;
            MoveStep prevStep = steps.get(0);
            for (MoveStep s : steps) {
                if (s.isTurning() && (nStraight < turnMode)) {
                    prevStep.setDanger(true);
                }
                nStraight = s.getNStraight();
                prevStep = s;
            }
        }

        // If running on pavement we don't know to mark the danger steps if we turn
        // before expending
        // enough MP to require running movement.
        if (steps.size() > 1) {
            MoveStep lastStep = steps.get(steps.size() - 1);
            MoveStep prevStep = steps.get(0);
            for (MoveStep s : steps) {
                if (s.getType() == MoveStepType.CHANGE_BOARD) {
                    // these steps never require rolls
                    continue;
                }
                if (s.getMovementType(false) == EntityMovementType.MOVE_ILLEGAL) {
                    break;
                }
                s.setDanger(s.isDanger() ||
                      Compute.isPilotingSkillNeeded(game,
                            entity.getId(),
                            prevStep.getPosition(),
                            s.getPosition(),
                            lastStep.getMovementType(true),
                            prevStep.isTurning(),
                            prevStep.isPavementStep(),
                            prevStep.getElevation(),
                            s.getElevation(),
                            s));
                s.setPastDanger(s.isPastDanger() || s.isDanger());
                prevStep = s;
            }
        }

        // Gravity check: only applies to ground moves by ground units
        if (gravityConcern && getMpUsed() != 0) {
            int usedMP = getMpUsed();
            int runMP = cachedEntityState.getRunMPNoGravity();
            int jumpMP = cachedEntityState.getJumpMPNoGravity();
            if (gravity < 1.0) {
                // Only dangerous if we move too far
                step.setDanger(step.isDanger() || (usedMP > runMP || (step.isJumping() && usedMP > jumpMP)));
            } else {
                // Dangerous if we jump _at all_
                step.setDanger(step.isDanger() || (step.isJumping()));
            }
        }

        // if we're an aerospace unit on a ground map and have passed over a hostile
        // unit
        // record this fact - it is useful for debugging thus we leave the commented out
        // code here
        // but for performance reasons, we don't actually do it.
        /*
         * if (step.useAeroAtmosphere(game, entity)
         * && game.getBoard().onGround()
         * && (step.getPosition() != null)
         * && (game.getFirstEnemyEntity(step.getPosition(), entity) != null)) {
         * fliesOverEnemy = true;
         * }
         */

        // having checked for illegality and other things, add it to the set of
        // contained step types
        containedStepTypes.add(step.getType());

        return this;
    }

    /**
     * Perform all the possible "is this illegal" checks. Short-circuits to omit unnecessary checks once the move has
     * been declared illegal
     */
    private void performIllegalCheck(MoveStep step, Coords start, Coords land) {
        // can't do anything after loading except loading again (if MPs exist)
        if (contains(MoveStepType.LOAD) && !(getLastStep().getType() == MoveStepType.LOAD)) {
            step.setMovementType(EntityMovementType.MOVE_ILLEGAL);
            return;
        }
        // can't do anything after unloading except unloading again
        if (contains(MoveStepType.UNLOAD) && !(getLastStep().getType() == MoveStepType.UNLOAD)) {
            step.setMovementType(EntityMovementType.MOVE_ILLEGAL);
            return;
        }

        // check for illegal jumps
        if ((start == null) || (land == null)) {
            // If we have null for either coordinate then we know the step
            // isn't legal.
            step.setMovementType(EntityMovementType.MOVE_ILLEGAL);
            return;
        } else {
            // if we're jumping without a mechanical jump booster (?)
            // or we're acting like a spheroid DropShip in the atmosphere
            if ((isJumping() && !contains(MoveStepType.JUMP_MEK_MECHANICAL_BOOSTER)) ||
                  (Compute.useSpheroidAtmosphere(game, getEntity()) && (step.getType() != MoveStepType.HOVER))) {
                int distance = start.distance(land);

                if (step.isThisStepBackwards() || (step.getDistance() > distance)) {
                    step.setMovementType(EntityMovementType.MOVE_ILLEGAL);
                    return;
                }
            }
        }

        // Check if jumping entity has enough MPs to reach building elevation
        if (isJumping() && !(entity instanceof Infantry)) {
            Hex destHex = game.getBoard(step.getBoardId()).getHex(step.getPosition());
            int building = destHex.terrainLevel(Terrains.BLDG_ELEV);
            if (building > 0) {
                int maxElevation = (entity.getJumpMP() +
                      entity.getElevation() +
                      game.getBoard(entity.getBoardId()).getHex(entity.getPosition()).getLevel()) -
                      destHex.getLevel();
                if (building > maxElevation) {
                    step.setMovementType(EntityMovementType.MOVE_ILLEGAL);
                    return;
                }
            }
        }

        // Can't move backwards and Evade
        if (!entity.isAirborne() && contains(MoveStepType.BACKWARDS) && contains(MoveStepType.EVADE)) {
            step.setMovementType(EntityMovementType.MOVE_ILLEGAL);
            return;
        }

        // If JumpShips turn, they can't do anything else
        if (entity.isJumpShip() && !step.isFirstStep() &&
              (contains(MoveStepType.TURN_LEFT) || contains(MoveStepType.TURN_RIGHT))) {
            step.setMovementType(EntityMovementType.MOVE_ILLEGAL);
            return;
        }

        // Ensure we only lay one mine
        if ((step.getType() == MoveStepType.LAY_MINE)) {
            boolean containsOtherLayMineStep = false;
            for (int i = 0; i < steps.size() - 1; i++) {
                if (steps.get(i).getType() == MoveStepType.LAY_MINE) {
                    containsOtherLayMineStep = true;
                    break;
                }
            }

            if (containsOtherLayMineStep) {
                step.setMovementType(EntityMovementType.MOVE_ILLEGAL);
                return;
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
                return;
            }
        }

        // Make sure we are not turning or changing elevation while strafing, and that
        // we are not
        // starting a second group of hexes during the same round
        if (step.isStrafingStep() && steps.size() > 1) {
            MoveStep last = steps.get(steps.size() - 2);
            // If the previous step is a strafing step, make sure we have the same facing
            // and elevation,
            // and we are not exceeding the maximum five hexes.
            if (last.isStrafingStep()) {
                if (step.getFacing() != last.getFacing() ||
                      (step.getElevation() + getGame().getBoard(step.getBoardId()).getHex(step.getPosition()).floor() !=
                            last.getElevation() + getGame().getBoard(step.getBoardId())
                                  .getHex(last.getPosition())
                                  .floor()) ||
                      steps.stream().filter(MoveStep::isStrafingStep).count() > 5) {
                    step.setMovementType(EntityMovementType.MOVE_ILLEGAL);
                    return;
                }
            } else {
                // If the previous step is not a strafing step, make sure that the new step is
                // the only strafing
                // step we have in the path.
                for (int i = 0; i < steps.size() - 2; i++) {
                    if (steps.get(i).isStrafingStep()) {
                        step.setMovementType(EntityMovementType.MOVE_ILLEGAL);
                        return;
                    }
                }
            }
        }

        // VTOLs using maneuvering ace to make lateral shifts can't flank
        // unless using controlled sideslip
        if (containsLateralShift() &&
              getEntity().isUsingManAce() &&
              (getEntity() instanceof VTOL) &&
              getMpUsed() > getCachedEntityState().getWalkMP() &&
              !game.getOptions().booleanOption(OptionsConstants.ADVANCED_GROUND_MOVEMENT_VEHICLE_ADVANCED_MANEUVERS)) {
            step.setMovementType(EntityMovementType.MOVE_ILLEGAL);
            return;
        }

        // If a tractor connects a new trailer this round, it can't do anything but add
        // more trailers
        // This prevents the tractor from moving before its MP, stacking limitations and
        // prohibited terrain can be updated by its trailers
        // It makes sense, too. You can't just connect a trailer and drive off with it
        // in <10 seconds.
        if (contains(MoveStepType.TOW) && !(step.getType() == MoveStepType.TOW)) {
            step.setMovementType(EntityMovementType.MOVE_ILLEGAL);
        }

        if ((step.getType() == MoveStepType.BRACE) && !isValidPositionForBrace(step)) {
            step.setMovementType(EntityMovementType.MOVE_ILLEGAL);
            return;
        }

        // If using TacOps reverse gear option, cannot mix forward and backward movement
        // in the same round except VTOLs.
        if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_GROUND_MOVEMENT_REVERSE_GEAR) &&
              ((entity instanceof Tank && !(entity instanceof VTOL)) ||
                    (entity instanceof QuadVee && entity.getConversionMode() == QuadVee.CONV_MODE_VEHICLE))) {
            boolean fwd = false;
            boolean rev = false;
            for (MoveStep s : steps) {
                fwd |= s.getType() == MoveStepType.FORWARDS ||
                      s.getType() == MoveStepType.LATERAL_LEFT ||
                      s.getType() == MoveStepType.LATERAL_RIGHT;
                rev |= s.getType() == MoveStepType.BACKWARDS ||
                      s.getType() == MoveStepType.LATERAL_LEFT_BACKWARDS ||
                      s.getType() == MoveStepType.LATERAL_RIGHT_BACKWARDS;
            }

            if (fwd && rev) {
                step.setMovementType(EntityMovementType.MOVE_ILLEGAL);
            }
        }

        // if we have a PICKUP, then we can't do anything else after it
        if (contains(MoveStepType.PICKUP_CARGO)) {
            step.setMovementType(EntityMovementType.MOVE_ILLEGAL);
        }
    }

    public void compile(final Game g, final Entity en) {
        compile(g, en, true);
    }

    public void compile(final Game g, final Entity en, boolean clip) {
        setGame(g);
        setEntity(en);
        final Vector<MoveStep> temp = new Vector<>(steps);
        steps.removeAllElements();
        containedStepTypes.clear();
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
            } else if (step.getBraceLocation() != Entity.LOC_NONE) {
                step = new MoveStep(this, step.getType(), step.getBraceLocation());
            } else if (!step.getLaunched().isEmpty()) {
                step = new MoveStep(this, step.getType(), step.getLaunched());
            } else if (step.isManeuver()) {
                step = new MoveStep(this, step.getType(), step.hasNoCost(), step.isManeuver(), step.getManeuverType());
            } else if (step.getManeuverType() != ManeuverType.MAN_NONE) {
                step = new MoveStep(this, step.getType(), -1, -1, step.getManeuverType());
            } else if (step.hasNoCost()) {
                step = new MoveStep(this, step.getType(), step.hasNoCost());
            } else if (null != step.getMinefield()) {
                step = new MoveStep(this, step.getType(), step.getMinefield());
            } else if (null != step.getAdditionalData() && !step.getAdditionalData().isEmpty()) {
                step = new MoveStep(this, step.getType(), step.getAdditionalData());
            } else {
                step = new MoveStep(this, step.getType());
            }
            this.addStep(step);
        }

        // Can't move out of a hex with an enemy unit unless we started
        // there, BUT we're allowed to turn, unload/disconnect, or go prone.
        Coords pos = getEntity().getPosition();
        boolean isMek = getEntity() instanceof Mek;
        int elev = getEntity().getElevation();
        if (Compute.isEnemyIn(getGame(), getEntity(), pos, false, isMek, elev, true)) {
            // There is an enemy, can't go out and back in, and go out again
            boolean left = false;
            boolean returned = false;
            for (MoveStep step : steps) {
                if (!left) {
                    if (!step.getPosition().equals(getEntity().getPosition()) ||
                          !(step.getElevation() == getEntity().getElevation())) {
                        // we left the location
                        left = true;
                        continue;
                    }
                    continue;
                }
                if (!returned) {
                    if (step.getPosition().equals(getEntity().getPosition()) &&
                          (step.getElevation() == getEntity().getElevation())) {
                        // we returned to the location
                        returned = true;
                        continue;
                    }
                    continue;
                }
                // We've returned, only following 5 types are legal
                if ((step.getType() != MoveStepType.TURN_LEFT) &&
                      (step.getType() != MoveStepType.TURN_RIGHT) &&
                      (step.getType() != MoveStepType.UNLOAD) &&
                      (step.getType() != MoveStepType.DISCONNECT) &&
                      (step.getType() != MoveStepType.GO_PRONE)) {
                    // we only need to identify the first illegal move
                    step.setMovementType(EntityMovementType.MOVE_ILLEGAL);
                    break;
                }
            }
        }

        if (getEntity() instanceof LandAirMek && !((LandAirMek) getEntity()).canConvertTo(getFinalConversionMode())) {
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
        if (!steps.isEmpty()) {
            final MoveStep step1 = getStep(steps.size() - 1);

            if (step1.getType() == MoveStepType.START_JUMP) {
                getEntity().setIsJumpingNow(false);
            }

            steps.removeElementAt(steps.size() - 1);

            if (getEntity().isConvertingNow() && !this.contains(MoveStepType.CONVERT_MODE)) {
                getEntity().setConvertingNow(false);
                // Meks using tracks have the movement mode set at the beginning of the turn, so
                // it will need to be reset.
                if (getEntity() instanceof Mek && ((Mek) getEntity()).hasTracks()) {
                    getEntity().toggleConversionMode();
                }
            }

            // Treat multiple convert steps as a single command
            if (step1.getType() == MoveStepType.CONVERT_MODE) {
                while (!steps.isEmpty() && steps.get(steps.size() - 1).getType() == MoveStepType.CONVERT_MODE) {
                    steps.removeElementAt(steps.size() - 1);
                }
            }

            // if this step is part of a maneuver, undo the whole maneuver, all the way to
            // the beginning.
            if (step1.isManeuver()) {
                int stepIndex = steps.size() - 1;

                while (!steps.isEmpty() && steps.get(stepIndex).isManeuver()) {
                    steps.removeElementAt(stepIndex);
                    stepIndex--;
                }

                // a maneuver begins with a "maneuver" step, so get rid of that as well
                steps.removeElementAt(stepIndex);
            }
        }

        // Find the new last step in the path.
        int index = steps.size() - 1;
        while ((index >= 0) && getStep(index).setEndPos(true) && !getStep(index).isLegal(this)) {
            index--;
        }

        // we may have removed a lot of steps - recalculate the contained step types
        regenerateStepTypes();
    }

    public void clear() {
        steps.removeAllElements();
    }

    public boolean isValidPositionForBrace(MoveStep step) {
        return isValidPositionForBrace(step.getPosition(), step.getBoardId(), step.getFacing());
    }

    /**
     * Given a set of coordinates and a facing, is the entity taking this path in a valid position to execute a brace?
     */
    public boolean isValidPositionForBrace(Coords coords, int boardId, int facing) {
        // situation: can't brace off of jumps; can't brace if you're not a mek with
        // arms/protomek
        if (isJumping() || contains(MoveStepType.GO_PRONE) || !getEntity().canBrace()
              || !game.hasBoardLocation(coords, boardId)) {
            return false;
        }

        // for meks, the check is complicated - you have to be directly in front of a
        // hex with either
        // a) level 1 level higher than your hex level
        // b) building/bridge ceiling 1 level higher than your hex level (?)
        if (getEntity() instanceof Mek) {
            Board board = game.getBoard(boardId);
            Coords nextPosition = coords.translated(facing);
            boolean nextHexOnBoard = board.contains(nextPosition);

            if (!nextHexOnBoard) {
                return false;
            }

            Hex nextHex = board.getHex(nextPosition);
            Hex currentHex = board.getHex(coords);

            int curHexLevel = currentHex.containsAnyTerrainOf(Terrains.BLDG_ELEV, Terrains.BRIDGE_ELEV) ?
                  currentHex.ceiling() :
                  currentHex.floor();
            int nextHexLevel = nextHex.containsAnyTerrainOf(Terrains.BLDG_ELEV, Terrains.BRIDGE_ELEV) ?
                  nextHex.ceiling() :
                  nextHex.floor();

            return nextHexLevel == curHexLevel + 1;
        }

        return true;
    }

    public ListIterator<MoveStep> getSteps() {
        // Create shallow copy for iterator thread safety.
        return new Vector<MoveStep>(steps).listIterator();
    }

    public @Nullable MoveStep getStep(final int index) {
        if ((index < 0) || (index >= steps.size())) {
            return null;
        }
        return steps.elementAt(index);
    }

    /**
     * Helper function that rebuilds the "contained step types" from scratch. Loops over all the steps in the path, so
     * should only be used when removing or replacing steps.
     */
    private void regenerateStepTypes() {
        containedStepTypes.clear();
        for (MoveStep step : steps) {
            containedStepTypes.add(step.getType());
        }
    }

    /**
     * Check for any of the specified type of step in the path
     *
     * @param type The step type to check for
     *
     * @return Whether this step type is contained within this path
     */
    public boolean contains(final MoveStepType type) {
        return containedStepTypes.contains(type);
    }

    /**
     * Convenience function to determine whether this path results in the unit explicitly moving off board More relevant
     * for aircraft
     *
     * @return Whether this path will result in the unit moving off board
     */
    public boolean fliesOffBoard() {
        return contains(MoveStepType.OFF) || contains(MoveStepType.RETURN) || contains(MoveStepType.FLEE);
    }

    /**
     * Check for MASC use
     */
    public boolean hasActiveMASC() {
        for (final ListIterator<MoveStep> i = getSteps(); i.hasNext(); ) {
            final MoveStep step = i.next();
            if (step.isUsingMASC()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check for Supercharger use
     */
    public boolean hasActiveSupercharger() {
        for (final ListIterator<MoveStep> i = getSteps(); i.hasNext(); ) {
            final MoveStep step = i.next();
            if (step.isUsingSupercharger()) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return the final coordinates if a mek were to perform all the steps in this path, or null if there's an issue
     *       with determining the coords
     */
    public @Nullable Coords getFinalCoords() {
        if (getGame().useVectorMove()) {
            return Compute.getFinalPosition(getEntity().getPosition(), getFinalVectors());
        } else if (getLastStep() != null) {
            return getLastStep().getPosition();
        } else {
            return getEntity().getPosition();
        }
    }

    /**
     * @return the board ID of the final position of this move path or the unit's own board ID if the path has no steps.
     */
    public int getFinalBoardId() {
        if (getGame().useVectorMove()) {
            // legacy; vector movement will not carry over to atmosphere or ground maps (?)
            return entity.getBoardId();
        } else if (getLastStep() != null) {
            return getLastStep().getBoardId();
        } else {
            return entity.getBoardId();
        }
    }

    /**
     * Returns the starting {@link Coords} of this path.
     */
    public @Nullable Coords getStartCoords() {
        for (final ListIterator<MoveStep> e = getSteps(); e.hasNext(); ) {
            final MoveStep step = e.next();
            final Coords coords = step.getPosition();
            if (coords != null) {
                return coords;
            }
        }
        return null;
    }

    /**
     * Returns the final facing if a mek were to perform all the steps in this path.
     */
    public int getFinalFacing() {
        MoveStep last = getLastStep();
        if (last != null) {
            return last.getFacing();
        }
        return getEntity().getFacing();
    }

    /**
     * Returns whether a unit would end up prone after all the steps
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
     * Returns whether a unit would end up hull-down after all the steps
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
     * Returns whether a unit would be in climb mode after all the steps
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
     * Returns the highest elevation in the current path
     *
     */
    public int getMaxElevation() {
        int maxElev = 0;
        for (MoveStep step : steps) {
            maxElev = Math.max(maxElev, getGame().getBoard(step.getBoardId()).getHex(step.getPosition()).getLevel());
        }
        return maxElev;
    }

    /**
     * returns if the unit had any altitude above 0 during the movement path
     */
    public boolean isAirborne() {
        for (final ListIterator<MoveStep> i = getSteps(); i.hasNext(); ) {
            final MoveStep step = i.next();
            if (step.getAltitude() > 0) {
                return true;
            }
        }
        return false;
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
     * If the path contains mode conversions, this will determine the movement mode at the end of movement. Note that
     * LAMs converting from AirMek to Biped mode require two convert commands.
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

    public @Nullable MoveStep getLastStep() {
        for (int i = getStepVector().size() - 1; i >= 0; i--) {
            MoveStep last = getStepVector().get(i);
            if (last != null) {
                return last;
            }
        }
        return null;
    }

    public @Nullable MoveStep getSecondLastStep() {
        if (steps.size() > 1) {
            return getStep(steps.size() - 2);
        }
        return getLastStep();
    }

    /* Debug method */
    public void printAllSteps() {
        LOGGER.debug("*Steps*");
        for (int i = 0; i < steps.size(); i++) {
            LOGGER.debug("  {}: {}, {}", i, getStep(i), getStep(i).getMovementType(i == (steps.size() - 1)));
        }
    }

    /**
     * Removes impossible steps.
     */
    public void clipToPossible() {
        if (steps.isEmpty()) {
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
     * <p>
     * Finds the sequence of three steps that can be transformed, then removes all three and replaces them with the
     * lateral shift step.
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

        if (step1.oppositeTurn(step3) &&
              ((step2.getType() == MoveStepType.BACKWARDS) ||
                    (step2.getType() == MoveStepType.FORWARDS))) {
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
            return switch (turn) {
                case TURN_LEFT -> MoveStepType.LATERAL_LEFT;
                case TURN_RIGHT -> MoveStepType.LATERAL_RIGHT;
                default -> turn;
            };
        }
        return switch (turn) {
            case TURN_LEFT -> MoveStepType.LATERAL_LEFT_BACKWARDS;
            case TURN_RIGHT -> MoveStepType.LATERAL_RIGHT_BACKWARDS;
            default -> turn;
        };
    }

    /**
     * Returns the turn direction that corresponds to the lateral shift
     */
    static MoveStepType turnForLateralShift(final MoveStepType shift) {
        return switch (shift) {
            case LATERAL_LEFT, LATERAL_LEFT_BACKWARDS -> MoveStepType.TURN_LEFT;
            case LATERAL_RIGHT, LATERAL_RIGHT_BACKWARDS -> MoveStepType.TURN_RIGHT;
            default -> shift;
        };
    }

    /**
     * Returns the direction (either MovePath.MoveStepType.TURN_LEFT or MoveStepType.TURN_RIGHT) that the destination
     * facing lies in.
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
     * Returns the logical number of hexes moved the path (does not count turns, etc.).
     */
    public int getHexesMoved() {
        if (getLastStep() == null) {
            return 0;
        }
        return getLastStep().getDistance();
    }

    /**
     * Returns the linear distance between the first and last hexes in the path.
     */
    public int getDistanceTravelled() {
        var currentEntityPosition = getEntity().getPosition();
        if (currentEntityPosition == null) {
            return 0;
        }
        var finalCoords = getFinalCoords();
        if (finalCoords == null) {
            return 0;
        }
        return currentEntityPosition.distance(finalCoords);
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
     * Extend the current path to the destination <code>Coords</code>, moving only in one direction. This method works
     * by applying the supplied move step as long as it moves closer to the destination. If the destination cannot be
     * reached solely by the provided move step, the pathfinder will quit once it gets as closer as it can. Only used
     * for Mek Mechanical Jump Boosters.
     *
     * @param dest      the destination <code>Coords</code> of the move.
     * @param type      the type of movement step required.
     * @param direction the direction of movement.
     */
    public void findSimplePathTo(Coords dest, MoveStepType type, int direction, int facing) {
        Coords currStep = getFinalCoords();
        Coords nextStep = currStep.translated(direction);
        while (dest.distance(nextStep) < dest.distance(currStep)) {
            MoveStepType finalType = type;
            if (!game.hasBoardLocation(nextStep, getFinalBoardId())) {
                // When the path hits a hex outside the board, currStep is on a north or south board edge and the
                // current direction cannot be used for the next hex. Use any of the two adjacent directions instead
                // that stays on the board
                // FIXME: Unfortunately this problem at board edges makes the "simple" path finder rather complicated
                //  It would probably be better to search Mek Mechanical Jump Booster paths with the standard path
                //  finder. Normally units can use turns but must move forward into other hexes. For MMJB, the Mek
                //  cannot turn but enter hexes in any direction. isMovementPossible() in MoveStep would have to reflect
                //  that in order for the path finder to find suitable paths.
                int adjacentDirection = (direction + 1) % 6;
                nextStep = currStep.translated(adjacentDirection);
                if (game.hasBoardLocation(nextStep, getFinalBoardId())) {
                    finalType = MoveStepType.stepTypeForRelativeDirection(adjacentDirection, facing);
                } else {
                    adjacentDirection = (direction + 5) % 6;
                    nextStep = currStep.translated(adjacentDirection);
                    if (game.hasBoardLocation(nextStep, getFinalBoardId())) {
                        finalType = MoveStepType.stepTypeForRelativeDirection(adjacentDirection, facing);
                    }
                }
            }
            addStep(finalType);
            currStep = nextStep;
            nextStep = currStep.translated(direction);
        }

        // Did we reach the destination? If not, try another direction
        if (!currStep.equals(dest)) {
            MoveStepType moveStepType = MoveStepType.stepTypeForRelativeDirection(currStep.direction(dest), facing);
            findSimplePathTo(dest, moveStepType, currStep.direction(dest), facing);
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

        ShortestPathFinder pf = ShortestPathFinder.newInstanceOfAStar(dest, type, game, getFinalBoardId());

        StopConditionTimeout<MovePath> timeoutCondition = new StopConditionTimeout<>(
              timeLimit);
        pf.addStopCondition(timeoutCondition);

        pf.run(clone());
        MovePath finPath = pf.getComputedPath(dest);
        // this can be used for debugging the "destruction aware pathfinder"
        // MovePath finPath = calculateDestructionAwarePath(dest);

        if (timeoutCondition.timeoutEngaged || finPath == null) {
            /*
             * Either we have forced searcher to end prematurely or no path was
             * found. Let's try to fix it by taking the path that ended closest
             * to the target and greedily extend it.
             */
            MovePath bestMp = Collections.min(pf.getAllComputedPaths().values(),
                  new MovePathGreedyComparator(dest));
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
            LOGGER.error("Unable to find a path to the destination hex! \tMoving {}from {} to {}",
                  getEntity(),
                  getFinalCoords(),
                  dest);
        }
    }

    public boolean isMoveLegal() {
        // Moves which end up off of the board are not legal.
        if (!getGame().getBoard(getFinalBoardId()).contains(getFinalCoords())) {
            return false;
        }

        // for aero units move must use up all their velocity
        // but only if it is actually IAero, because anything could return isAero() == true but not implement IAero
        if (getEntity().isAero() && getEntity() instanceof IAero aero) {
            if (getLastStep() == null) {
                if ((aero.getCurrentVelocity() > 0) && !getGame().useVectorMove()) {
                    return false;
                }
            } else {
                if ((getLastStep().getVelocityLeft() > 0) &&
                      !getGame().useVectorMove() &&
                      !(getLastStep().getType() == MoveStepType.FLEE ||
                            getLastStep().getType() == MoveStepType.EJECT)) {
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
     * An A* pathfinder to get from the end of the current path (or entity's position if empty) to the destination.
     *
     * @param dest      The goal hex
     * @param type      The type of move we want to do
     * @param timeLimit the maximum <code>int</code> number of milliseconds to take hunting for an ideal path.
     */
    @SuppressWarnings("unused")
    private void notSoLazyPathfinder(final Coords dest, final MoveStepType type, final int timeLimit) {
        final int MAX_CANDIDATES = 100;
        final long endTime = java.lang.System.currentTimeMillis() + timeLimit;

        MoveStepType step = type;
        if (step != MoveStepType.BACKWARDS) {
            step = MoveStepType.FORWARDS;
        }

        final MovePathComparator mpc = new MovePathComparator(dest, step == MoveStepType.BACKWARDS);

        MovePath bestPath = clone();

        // A collection of paths we have already explored
        final HashMap<Key, MovePath> discovered = new HashMap<>();
        discovered.put(bestPath.getKey(), bestPath);

        // A collection of hte possible next-moves
        final PriorityQueue<MovePath> candidates = new PriorityQueue<>(110, mpc);
        candidates.add(bestPath);

        boolean keepLooping = getFinalCoords().distance(dest) > 1;
        int loopCount = 0;

        // Keep looping while we have candidates to explore, and certain stop
        // conditions aren't met (time-limit, destination found, etc.)
        while (!candidates.isEmpty() && keepLooping) {
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
            // Evaluate possible next steps
            for (MovePath expandedPath : candidatePath.getNextMoves(step == MoveStepType.BACKWARDS,
                  step == MoveStepType.FORWARDS)) {
                if (expandedPath.getLastStep()
                      .isMovementPossible(getGame(), startingPos, startingElev, getCachedEntityState())) {

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
            // have to take extra steps to see if we can finish off the move
            // this is because getNextMoves only considers turning and
            // forward/backward movement
            if (type == MoveStepType.CHARGE || type == MoveStepType.DFA) {
                MovePath expandedPath = candidatePath.clone();
                expandedPath.addStep(type);
                if (expandedPath.getLastStep()
                      .isMovementPossible(getGame(), startingPos, startingElev, getCachedEntityState())) {

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

            loopCount++;
            if (((loopCount % 256) == 0) && keepLooping && !candidates.isEmpty()) {
                final MovePath front = candidates.peek();
                if (front.getFinalCoords().distance(dest) < bestPath.getFinalCoords().distance(dest)) {
                    bestPath = front;
                }

                if (java.lang.System.currentTimeMillis() > endTime) {
                    keepLooping = false;
                    LOGGER.warn("Time limit reached searching for path!");
                }
            }
        }

        if (getFinalCoords().distance(dest) > bestPath.getFinalCoords().distance(dest)) {
            // Make the path we found, this path.
            steps = bestPath.steps;
        }

        if (!getFinalCoords().equals(dest)) {
            lazyPathfinder(dest, type);
        }
    }

    /**
     * Find the shortest path to the destination <code>Coords</code> by hex count. This right choice <em>only</em> when
     * making a simple move like a straight line or one with a single turn.
     *
     * @param dest the destination <code>Coords</code> of the move.
     * @param type the type of movement step required.
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
                  false,
                  ManeuverType.MAN_NONE);
            // step forwards
            addStep(step);
        }
        rotatePathfinder((getFinalCoords().direction(dest) + (step == MoveStepType.BACKWARDS ? 3 : 0)) % 6,
              false,
              ManeuverType.MAN_NONE);
        if (!dest.equals(getFinalCoords())) {
            addStep(type);
        }
    }

    /**
     * Returns a list of possible moves that result in a facing/position/(jumping|prone) change, special steps (mine
     * clearing and such) must be handled elsewhere.
     */
    public List<MovePath> getNextMoves(boolean backward, boolean forward) {
        final ArrayList<MovePath> result = new ArrayList<>();
        final MoveStep last = getLastStep();

        // need to do a separate section here for Aerospace.
        // just like jumping for now, but I could add some other stuff here later
        if (getEntity().isAero()) {
            MovePath left = clone();
            MovePath right = clone();

            // From here, we can move F, LF, RF, LLF, RRF, and RRRF.
            result.add((clone()).addStep(MoveStepType.FORWARDS));
            for (int turn = 0; turn < 2; turn++) {
                left.addStep(MoveStepType.TURN_LEFT);
                right.addStep(MoveStepType.TURN_RIGHT);
                result.add(left.clone().addStep(MoveStepType.FORWARDS));
                result.add(right.clone().addStep(MoveStepType.FORWARDS));
            }
            right.addStep(MoveStepType.TURN_RIGHT);
            result.add(right.addStep(MoveStepType.FORWARDS));

            // We've got all our next steps.
            return result;
        }

        // If the unit is prone or hull-down it limits movement options, unless
        // it's a tank; tanks can just drive out of hull-down, and they cannot
        // be prone.
        if (getFinalProne() || (getFinalHullDown() && !(getEntity() instanceof Tank))) {
            if ((last != null) && (last.getType() != MoveStepType.TURN_RIGHT)) {
                result.add(clone().addStep(MoveStepType.TURN_LEFT));
            }
            if ((last != null) && (last.getType() != MoveStepType.TURN_LEFT)) {
                result.add(clone().addStep(MoveStepType.TURN_RIGHT));
            }

            if (getEntity().isCarefulStand()) {
                result.add(clone().addStep(MoveStepType.CAREFUL_STAND));
            } else {
                result.add(clone().addStep(MoveStepType.GET_UP));
            }
            return result;
        }
        if (canShift()) {
            if (forward && (!backward || ((last == null) || (last.getType() != MoveStepType.LATERAL_LEFT)))) {
                result.add(clone().addStep(MoveStepType.LATERAL_RIGHT));
            }
            if (forward && (!backward || ((last == null) || (last.getType() != MoveStepType.LATERAL_RIGHT)))) {
                result.add(clone().addStep(MoveStepType.LATERAL_LEFT));
            }
            if (backward &&
                  (!forward ||
                        ((last == null) || (last.getType() != MoveStepType.LATERAL_LEFT_BACKWARDS)))) {
                result.add(clone().addStep(MoveStepType.LATERAL_RIGHT_BACKWARDS));
            }
            if (backward &&
                  (!forward ||
                        ((last == null) || (last.getType() != MoveStepType.LATERAL_RIGHT_BACKWARDS)))) {
                result.add(clone().addStep(MoveStepType.LATERAL_LEFT_BACKWARDS));
            }
        }
        if (forward && (!backward || ((last == null) || (last.getType() != MoveStepType.BACKWARDS)))) {
            result.add(clone().addStep(MoveStepType.FORWARDS));
        }
        if ((last == null) || (last.getType() != MoveStepType.TURN_LEFT)) {
            result.add(clone().addStep(MoveStepType.TURN_RIGHT));
        }
        if ((last == null) || (last.getType() != MoveStepType.TURN_RIGHT)) {
            result.add(clone().addStep(MoveStepType.TURN_LEFT));
        }
        if (backward && (!forward || ((last == null) || (last.getType() != MoveStepType.FORWARDS)))) {
            result.add(clone().addStep(MoveStepType.BACKWARDS));
        }
        return result;
    }

    /**
     * Clones this path, will contain a new clone of the steps so that the clone is independent of the original.
     *
     * @return the cloned MovePath
     */
    @Override
    public MovePath clone() {
        final MovePath copy = new MovePath(getGame(), getEntity(), getWaypoint());
        copyFields(copy);
        return copy;
    }

    protected void copyFields(MovePath copy) {
        copy.steps = new Vector<>(steps);
        copy.careful = careful;
        copy.containedStepTypes = new HashSet<>(containedStepTypes);
        // copy.fliesOverEnemy = fliesOverEnemy;
        copy.cachedEntityState = cachedEntityState; // intentional pointer copy
    }

    /**
     * Rotate from the current facing to the destination facing.
     */
    public void rotatePathfinder(final int destFacing, final boolean isManeuver, int maneuverType) {
        while (getFinalFacing() != destFacing) {
            final MoveStepType stepType = getDirection(getFinalFacing(), destFacing);
            addStep(stepType, isManeuver, isManeuver, maneuverType);
        }
    }

    /**
     * @return true if a jump using mechanical jump boosters would cause falling damage. Mechanical jump boosters are
     *       only designed to handle the stress of falls from a height equal to their jumpMP; if a jump has a fall that
     *       is further than the jumpMP of the unit, fall damage applies.
     */
    public boolean shouldMechanicalJumpCauseFallDamage() {
        return isJumping() &&
              contains(MoveStepType.JUMP_MEK_MECHANICAL_BOOSTER) &&
              (getJumpMaxElevationChange() > getEntity().getMechanicalJumpBoosterMP());
    }

    /**
     * Returns the highest elevation along a jump path.
     *
     */
    public Coords getJumpPathHighestPoint() {
        Coords highestCoords = null;
        int highestElevation = 0;
        for (MoveStep step : steps) {
            if (getGame().getBoard(step.getBoardId()).getHex(step.getPosition()).getLevel() > highestElevation) {
                highestElevation = step.getElevation();
                highestCoords = step.getPosition();
            }
        }
        return highestCoords;
    }

    /**
     * Returns the distance between the highest elevation in the jump path and the elevation at the landing point. This
     * gives the largest distance the unit has fallen during the jump.
     */
    public int getJumpMaxElevationChange() {
        return getMaxElevation() - getGame().getBoard(getFinalBoardId()).getHex(getFinalCoords()).getLevel();
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
     * Airborne WiGEs that move less than five hexes (four for glider protomek) in a movement phase must land unless it
     * has taken off in the same phase, jumped, or it is a LAM or glider ProtoMek that is using hover movement.
     *
     * @param includeMovePathHexes Whether to include the hexes plotted in this MovePath in the total distance moved.
     *                             This should be true when plotting movement in the client and false when the server
     *                             checks for automatic landing at the end of movement.
     *
     * @return whether the unit is an airborne WiGE that must land at the end of movement.
     */
    public boolean automaticWiGELanding(boolean includeMovePathHexes) {
        if (getEntity().getMovementMode() != EntityMovementMode.WIGE || getEntity().isAirborne()) {
            return false;
        }
        // A LAM converting from AirMek to Mek mode automatically lands at the end of
        // movement.
        if ((getEntity() instanceof LandAirMek) &&
              (((LandAirMek) getEntity()).getConversionModeFor(getFinalConversionMode()) ==
                    LandAirMek.CONV_MODE_MEK)) {
            if (getLastStep() != null) {
                return getLastStep().getClearance() > 0;
            } else {
                return getEntity().isAirborneVTOLorWIGE();
            }
        }
        // A WiGE that jumped A) has to have been flying and B) cannot land this turn.
        if (isJumping()) {
            return false;
        }
        // If movement has been interrupted (such as by a sideslip) and remaining
        // movement points have
        // been spent, this MovePath only contains the hexes moved after the
        // interruption. The hexes already
        // moved this turn are in delta_distance. WHen the server checks at the end of
        // movement, delta_distance
        // already includes the hexes in this MovePath.
        int moved = getEntity().delta_distance;
        if (includeMovePathHexes) {
            moved += getHexesMoved();
        }
        if ((moved >= 5) || (getEntity().hasETypeFlag(Entity.ETYPE_PROTOMEK) && moved == 4)) {
            return false;
        }
        if (getEntity().wigeLiftoffHover() ||
              steps.stream()
                    .map(MoveStep::getType)
                    .anyMatch(st -> st == MoveStepType.UP || st == MoveStepType.HOVER)) {
            return false;
        }
        if (getLastStep() != null) {
            return getLastStep().getClearance() > 0;
        } else {
            return getEntity().isAirborneVTOLorWIGE();
        }
    }

    /**
     * @return Whether the entire path is submerged. A unit is only considered submerged when entirely underwater.
     */
    public boolean isAllUnderwater(Game game) {
        for (MoveStep step : steps) {
            Hex hex = game.getBoard(getFinalBoardId()).getHex(step.getPosition());
            if (!hex.containsTerrain(Terrains.WATER) || (step.getElevation() >= -entity.height())) {
                return false;
            }
        }
        return game.getBoard(entity.getBoardId()).getHex(entity.getPosition()).containsTerrain(Terrains.WATER)
              && entity.relHeight() < 0;
    }

    protected static class MovePathComparator implements Comparator<MovePath> {
        private final Coords destination;
        boolean backward;

        public MovePathComparator(final Coords destination, final boolean backward) {
            this.destination = destination;
            this.backward = backward;
        }

        @Override
        public int compare(final MovePath first, final MovePath second) {
            final int firstDist = first.getMpUsed() +
                  first.getFinalCoords().distance(destination) +
                  getFacingDiff(first);
            final int secondDist = second.getMpUsed() +
                  second.getFinalCoords().distance(destination) +
                  getFacingDiff(second);
            return firstDist - secondDist;
        }

        private int getFacingDiff(final MovePath first) {
            if (first.isJumping()) {
                return 0;
            }
            if (first.getFinalCoords().equals(destination)) {
                return 0;
            }
            int firstFacing = Math.abs(((first.getFinalCoords().direction(destination) + (backward ? 3 : 0)) % 6) -
                  first.getFinalFacing());
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
     * Get the position with the step immediately prior to the final position
     */
    public Coords getSecondFinalPosition(Coords startPos) {

        Coords priorPos = startPos;
        Coords finalPos = getFinalCoords();

        // if we moved one or fewer hexes, then just return starting position
        if (getHexesMoved() < 2) {
            return priorPos;
        }

        for (final ListIterator<MoveStep> i = getSteps(); i.hasNext(); ) {
            final MoveStep step = i.next();
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
            if (jumping &&
                  (step.getType() != MoveStepType.TURN_LEFT) &&
                  (step.getType() != MoveStepType.TURN_RIGHT) &&
                  (step.getType() != MoveStepType.JUMP_MEK_MECHANICAL_BOOSTER)) {
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
     * Searches the movement path for the first step that has the given position and sets it as a VTOL bombing step. If
     * found, any previous bombing step is cleared. If the coordinates are not part of the path nothing is changed.
     *
     * @param pos The <code>Coords</code> of the hex to be bombed.
     */
    public void setVTOLBombStep(Coords pos) {
        boolean foundPos = false;
        MoveStep prevBombing = null;
        for (MoveStep step : steps) {
            if (step.getPosition().equals(pos)) {
                if (step.isVTOLBombingStep()) {
                    return;
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
    }

    /**
     * Searches the path for the first <code>MoveStep</code> that matches the given position and sets it as a strafing
     * step. In cases where there are multiple steps with the same coordinates, we want the first one because it is the
     * one that enters the hex. In the rare case where the path crosses itself, select the one closest to the end of the
     * path.
     * <p>
     * FIXME: this does not deal with paths that cross themselves
     *
     * @param pos The <code>Coords</code> of the hex to be strafed
     */
    public void setStrafingStep(Coords pos) {
        MoveStep found = null;
        for (int i = steps.size() - 1; i >= 0; i--) {
            if (steps.get(i).getPosition().equals(pos)) {
                found = steps.get(i);
            } else if (found != null) {
                found.setStrafing(true);
                return;
            }
        }
        if (found != null) {
            found.setStrafing(true);
        }
    }

    /**
     * @return A list of entity ids for all units that have previously been plotted to be dropped/launched.
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
     * Convenience function encapsulating logic for whether, if we continue forward along the current path in the
     * current direction, we will run off the board
     *
     */
    public boolean nextForwardStepOffBoard() {
        return !game.getBoard(getFinalBoardId()).contains(getFinalCoords().translated(getFinalFacing()));
    }

    /**
     * Worker function that counts the number of steps of the given type at the end of the given path before another
     * step type occurs.
     */
    public int getEndStepCount(MoveStepType stepType) {
        int stepCount = 0;

        for (int index = getStepVector().size() - 1; index >= 0; index--) {
            if (getStepVector().get(index).getType() == stepType) {
                stepCount++;
            } else {
                break;
            }
        }

        return stepCount;
    }

    /**
     * Debugging method that calculates a destruction-aware move path to the destination coordinates
     */
    @SuppressWarnings("unused")
    public MovePath calculateDestructionAwarePath(Coords dest) {
        // code that's useful to test the destruction-aware pathfinder
        DestructionAwareDestinationPathfinder dpf = new DestructionAwareDestinationPathfinder();
        // the destruction aware pathfinder takes either a CardinalEdge or an explicit
        // set of coordinates
        Set<Coords> destinationSet = new HashSet<>();
        destinationSet.add(dest);

        // debugging code that can be used to find a path to a specific edge
        Princess princess = new Princess("test", "test", 2020);
        princess.startPrecognition();

        long marker1 = java.lang.System.currentTimeMillis();
        MovePath finPath = dpf.findPathToCoords(entity, destinationSet, false, princess.getClusterTracker());
        long marker2 = java.lang.System.currentTimeMillis();
        long marker3 = marker2 - marker1;

        return finPath;
    }

    /**
     * @return The maximum MP based on the current movement type of this path, including sprint if available
     */
    public int getMaxMP() {
        if (contains(MoveStepType.DFA)) {
            return getEntity().getJumpMP();
        } else if (contains(MoveStepType.JUMP_MEK_MECHANICAL_BOOSTER)) {
            return (getEntity() instanceof Mek mek) ? mek.getMechanicalJumpBoosterMP() : 0;
        } else if (contains(MoveStepType.START_JUMP)) {
            return getEntity().getJumpMP();
        } else if (contains(MoveStepType.BACKWARDS)) {
            return getEntity().getWalkMP();
        } else {
            if ((getLastStep() != null) && getLastStep().canUseSprint(game)) {
                return getEntity().getSprintMP();
            } else {
                return getEntity().getRunMP();
            }
        }
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof MovePath movePath)) {
            return false;
        }

        return new EqualsBuilder().append(entity, movePath.entity)
              .append(careful, movePath.careful)
              .append(gravityConcern, movePath.gravityConcern)
              .append(gravity, movePath.gravity)
              .append(steps, movePath.steps)
              .append(containedStepTypes, movePath.containedStepTypes)
              .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).append(entity)
              .append(steps)
              .append(containedStepTypes)
              .append(careful)
              .append(gravityConcern)
              .append(gravity)
              .toHashCode();
    }

    public void setFlightPathHex(BoardLocation flightPathHex) {
        this.flightPathHex = flightPathHex;
    }

    public BoardLocation getFlightPathHex() {
        return flightPathHex;
    }
}
