/*
 * MegaMek -
 * Copyright (c) 2000-2005 Ben Mazur (bmazur@sev.org)
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
package megamek.common;

import megamek.common.MovePath.MoveStepType;
import megamek.common.enums.MPBoosters;
import megamek.common.options.OptionsConstants;
import megamek.common.pathfinder.CachedEntityState;
import org.apache.logging.log4j.LogManager;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.Vector;

/**
 * A single step in the entity's movement. Since the path planner uses shallow
 * copies of MovePaths, multiple paths may share the same MoveStep, so this
 * class needs to be agnostic of what path it belongs to.
 * @since Aug 28, 2003
 */
public class MoveStep implements Serializable {
    private static final long serialVersionUID = -6075640793056182285L;
    private MoveStepType type;
    private int targetId = Entity.NONE;
    private int targetType = Targetable.TYPE_ENTITY;
    private Coords targetPos;

    private Coords position;
    private int facing;

    private int mp; // this step
    private int mpUsed; // whole path

    private int heat; // this step
    private int totalHeat;

    private int distance;
    private int leapDistance;

    private int elevation = -999;
    private int altitude = -999;

    private int mineToLay = -1;

    /**
     * This step's static movement type. Additional steps in the path will not
     * change this value.
     */
    private EntityMovementType movementType;
    /**
     * The movement mode after this step completes. Mode conversions will modify it, though
     * it may not take effect until the end of movement.
     */
    private EntityMovementMode movementMode = EntityMovementMode.NONE;

    private boolean isProne;
    private boolean isFlying;
    private boolean isHullDown;
    private boolean climbMode;

    private boolean danger; // keep psr
    private boolean pastDanger;
    private boolean docking;
    private boolean isUsingMASC;
    private int targetNumberMASC; // psr
    private boolean isUsingSupercharger;
    private int targetNumberSupercharger; // psr
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
    private boolean isDiggingIn = false;
    private boolean isTakingCover = false;
    private int wigeBonus = 0;
    private int nWigeDescent = 0;

    /**
     * The Entity that is taking this MoveStep.
     */
    private Entity entity = null;

    /**
     * Determines if this MoveStep is part of a MovePath that is jumping.
     */
    private boolean isJumpingPath = false;

    /**
     * Determines if this MoveStep is part of a MovePath that is moving
     * carefully.
     */
    private boolean isCarefulPath = true;

    /*
     * Aero related stuff
     */
    private int velocity = -999;
    private int velocityN = -999;

    // also keep track of velocity left to spend
    private int velocityLeft = 0;
    // how many turns?
    private int nTurns = 0;
    private int nRolls = 0;
    // does the unit have a free turn available
    private boolean freeTurn = false;
    // how many hexes straight has the unit traveled
    private int nStraight = 0;
    // how many altitude down
    private int nDown = 0;
    // how many hexes moved in this velocity "chunk" (for aero on ground maps)
    private int nMoved = 0;
    // for Aeros, they may get pushed off board by OOC
    private boolean offBoard = false;
    // for optional vector movement
    private int[] mv;
    private int recoveryUnit = -1;
    TreeMap<Integer, Vector<Integer>> launched;
    private boolean isEvading = false;
    private boolean isShuttingDown = false;
    private boolean isStartingUp = false;
    private boolean isSelfDestructing = false;
    private boolean isRolled = false;

    // for maneuvers
    private int maneuverType = ManeuverType.MAN_NONE;
    // steps associated with maneuvers have no cost
    private boolean noCost = false;
    // is this step part of a maneuver?
    private boolean maneuver = false;

    int braceLocation = Entity.LOC_NONE;

    private Minefield mf;

    /**
     * Flag that indicates that this step is into prohibited terrain.
     * <p>
     * If the unit is jumping, this step is only invalid if it is the end of the
     * path.
     */
    private boolean terrainInvalid = false;

    /**
     * A collection of buildings that are crushed during this move step. This is
     * used for landed Aerodyne Dropships and Mobile Structures.
     */
    private ArrayList<Coords> crushedBuildingLocs;

    /**
     * Create a step of the given type.
     *
     * @param type - should match one of the MovePath constants, but this is not
     *             currently checked.
     */
    public MoveStep(MovePath path, MoveStepType type) {
        this.type = type;
        if (path != null) {
            entity = path.getEntity();
            isJumpingPath = path.isJumping();
            isCarefulPath = path.isCareful();
        }
        if ((type == MoveStepType.UNLOAD) || (type == MoveStepType.LAUNCH)
                || (type == MoveStepType.DROP) || (type == MoveStepType.UNDOCK)
                || (type == MoveStepType.DISCONNECT)) {
            hasEverUnloaded = true;
        } else {
            hasEverUnloaded = false;
        }
    }

    /**
     * Create a step with the given target and a position for that target
     *
     * @param type   - should match one of the MovePath constants, but this is not
     *               currently checked.
     * @param target - the <code>Targetable</code> that is the target of this step.
     *               For example, the enemy being charged.
     * @param pos    = the <code>Coords</code> for the target position.
     */
    public MoveStep(MovePath path, MoveStepType type, Targetable target,
                    Coords pos) {
        this(path, type);
        targetId = target.getId();
        targetType = target.getTargetType();
        targetPos = pos;
        if ((type == MoveStepType.UNLOAD) || (type == MoveStepType.LAUNCH)
                || (type == MoveStepType.DROP) || (type == MoveStepType.UNDOCK)
                || (type == MoveStepType.DISCONNECT)) {
            hasEverUnloaded = true;
        } else {
            hasEverUnloaded = false;
        }
    }

    /**
     * Create a step with the given target.
     *
     * @param type   - should match one of the MovePath constants, but this is not
     *               currently checked.
     * @param target - the <code>Targetable</code> that is the target of this step.
     *               For example, the enemy being charged.
     */
    public MoveStep(MovePath path, MoveStepType type, Targetable target) {
        this(path, type);
        targetId = target.getId();
        targetType = target.getTargetType();
        if ((type == MoveStepType.UNLOAD) || (type == MoveStepType.LAUNCH)
                || (type == MoveStepType.DROP) || (type == MoveStepType.UNDOCK)
                || (type == MoveStepType.DISCONNECT)) {
            hasEverUnloaded = true;
        } else {
            hasEverUnloaded = false;
        }
    }

    /**
     * Create a step with the given mine to lay.
     *
     * @param path
     * @param type      - should match one of the MovePath constants, but this is not
     *                  currently checked.
     * @param additionalIntData -
     *              "mineToLay" by default to retain compatibility with existing code
     *              "braceLocation" if the move step type is BRACE
     */
    public MoveStep(MovePath path, MoveStepType type, int additionalIntData) {
        this(path, type);

        if (type == MoveStepType.BRACE) {
            this.braceLocation = additionalIntData;
        } else {
            this.mineToLay = additionalIntData;
        }
    }

    /**
     * Create a step with the units to launch or drop.
     *
     * @param path
     * @param type    - should match one of the MovePath constants, but this is not
     *                currently checked.
     * @param targets - vector of integers identifying the entities to launch
     */
    public MoveStep(MovePath path, MoveStepType type,
                    TreeMap<Integer, Vector<Integer>> targets) {
        this(path, type);
        launched = targets;
        if ((type == MoveStepType.UNLOAD) || (type == MoveStepType.LAUNCH)
                || (type == MoveStepType.DROP) || (type == MoveStepType.UNDOCK)
                || (type == MoveStepType.DISCONNECT)) {
            hasEverUnloaded = true;
        } else {
            hasEverUnloaded = false;
        }
    }

    public MoveStep(MovePath path, MoveStepType type, int recovery,
                    int mineToLay) {
        this(path, type);
        recoveryUnit = recovery;
        this.mineToLay = mineToLay;
    }

    public MoveStep(MovePath path, MoveStepType type, boolean noCost) {
        this(path, type);
        this.noCost = noCost;
    }

    public MoveStep(MovePath path, MoveStepType type, boolean noCost,
                    boolean isManeuver) {
        this(path, type);
        this.noCost = noCost;
        maneuver = isManeuver;
    }

    public MoveStep(MovePath path, MoveStepType type, int recovery,
                    int mineToLay, int manType) {
        this(path, type);
        recoveryUnit = recovery;
        this.mineToLay = mineToLay;
        maneuverType = manType;
    }

    public MoveStep(MovePath path, MoveStepType type, Minefield mf) {
        this(path, type);
        this.mf = mf;
    }

    @Override
    public String toString() {
        switch (type) {
            case BACKWARDS:
                return "B";
            case CHARGE:
                return "Ch";
            case DFA:
                return "DFA";
            case FORWARDS:
                return "F";
            case CAREFUL_STAND:
            case GET_UP:
                return "Up";
            case GO_PRONE:
                return "Prone";
            case START_JUMP:
                return "StrJump";
            case TURN_LEFT:
                return "L";
            case TURN_RIGHT:
                return "R";
            case LATERAL_LEFT:
                return "ShL";
            case LATERAL_RIGHT:
                return "ShR";
            case LATERAL_LEFT_BACKWARDS:
                return "ShLB";
            case LATERAL_RIGHT_BACKWARDS:
                return "ShRB";
            case UNJAM_RAC:
                return "Unjam";
            case SEARCHLIGHT:
                return "SLight";
            case LOAD:
                return "Load";
            case UNLOAD:
                return "Unload";
            case EJECT:
                return "Eject";
            case UP:
                return "U";
            case DOWN:
                return "D";
            case HULL_DOWN:
                return "HullDown";
            case CLIMB_MODE_ON:
                return "CM+";
            case CLIMB_MODE_OFF:
                return "CM-";
            case TAKEOFF:
                return "Takeoff";
            case VTAKEOFF:
                return "Vertical Takeoff";
            case LAND:
                return "Landing";
            case VLAND:
                return "Vertical Landing";
            case ACC:
                return "Acc";
            case DEC:
                return "Dec";
            case MANEUVER:
                return "Maneuver";
            case RETURN:
                return "Fly Off (Return)";
            case OFF:
                return "Fly Off";
            case FLEE:
                return "Flee";
            case EVADE:
                return "Evade";
            case CONVERT_MODE:
                return "ConvMode";
            case TOW:
                return "Tow";
            case DISCONNECT:
                return "Disconnect";
            case THRUST:
                return "Thrust";
            case YAW:
                return "Yaw";
            case HOVER:
                return "Hover";
            case BRACE:
                return "Brace";
            case CHAFF:
                return "Chaff";
            default:
                return "???";
        }
    }

    public MoveStepType getType() {
        return type;
    }

    /**
     * Set the target of the current step.
     *
     * @param target - the <code>Targetable</code> that is the target of this step.
     *               For example, the enemy being charged. If there is no target,
     *               pass a <code>null</code>
     */
    public void setTarget(Targetable target) {
        if (target == null) {
            targetId = Entity.NONE;
            targetType = Targetable.TYPE_ENTITY;
        } else {
            targetId = target.getId();
            targetType = target.getTargetType();
        }
    }

    /**
     * Turns VTOL bombing on or off for this step.
     */
    public void setVTOLBombing(boolean bombing) {
        if (bombing) {
            setTarget(new HexTarget(getPosition(), Targetable.TYPE_HEX_AERO_BOMB));
        } else {
            setTarget(null);
        }
    }

    /**
     * Turns VTOL strafing on or off for this step.
     */
    public void setStrafing(boolean strafing) {
        if (strafing) {
            setTarget(new HexTarget(getPosition(), Targetable.TYPE_HEX_CLEAR));
        } else {
            setTarget(null);
        }
    }

    /**
     * Get the target of the current step.
     *
     * @param game The current {@link Game}
     * @return The <code>Targetable</code> that is the target of this step. For
     *         example, the enemy being charged. This value may be
     *         <code>null</code>
     */
    public Targetable getTarget(Game game) {
        if (targetId == Entity.NONE) {
            return null;
        }
        return game.getTarget(targetType, targetId);
    }

    public Coords getTargetPosition() {
        return targetPos;
    }

    public TreeMap<Integer, Vector<Integer>> getLaunched() {
        if (launched == null) {
            launched = new TreeMap<>();
        }

        return launched;
    }

    /**
     * Helper for compile(), to deal with steps that move to a new hex.
     *
     * @param game The current {@link Game}
     * @param entity
     * @param prev
     */
    private void compileMove(final Game game, final Entity entity,
                             MoveStep prev, CachedEntityState cachedEntityState) {

        Hex destHex = game.getBoard().getHex(getPosition());

        // Check for pavement movement.
        if (!entity.isAirborne() && Compute.canMoveOnPavement(game, prev.getPosition(), getPosition(), this)) {
            setPavementStep(true);
        } else {
            setPavementStep(false);
            setOnlyPavement(false);
        }

        setHasJustStood(false);
        if (prev.isThisStepBackwards() != isThisStepBackwards()) {
            setDistance(0); // start over after shifting gears
        }
        addDistance(1);

        // need to reduce velocity left for aerospace units (and also reset
        // nTurns)
        // this is handled differently by aerospace units operating on the
        // ground map and by spheroids in atmosphere
        if (entity.isAirborne() && game.getBoard().onGround()) {
            setNMoved(getNMoved() + 1);
            if ((entity.getMovementMode() != EntityMovementMode.SPHEROID)
                    && (getNMoved() >= 16)) {
                setVelocityLeft(getVelocityLeft() - 1);
                setNMoved(0);
            }
        } else if (entity.isAirborne() && !game.useVectorMove()
                && !useSpheroidAtmosphere(game, entity)) {
            setVelocityLeft(getVelocityLeft() - 1);
            setNTurns(0);
        }

        // Track number of moves straight for aero free moves in atmosphere, vehicle turn modes, and bootlegger maneuver
        setNStraight(getNStraight() + 1);
        // if in atmosphere, then I need to know if this move qualifies the unit
        // for a free turn
        if (useAeroAtmosphere(game, entity)) {
            if (game.getBoard().onGround() && (getNStraight() > 7)) {
                // if flying on ground map, then you have to fly at least 8
                // straight hexes between turns (free or not)
                // http://www.classicbattletech.com/forums/index.php/topic,37171.new.html#new
                setNTurns(0);
            }

            if (!hasFreeTurn()) {
                // check conditions
                if (dueFreeTurn()) {
                    setFreeTurn(true);
                }
            }
        }

        if (getType() == MoveStepType.DFA) {
            Hex hex = game.getBoard().getHex(getPosition());
            setElevation(Math.max(0, hex.terrainLevel(Terrains.BLDG_ELEV)));
            // If we're DFA-ing, we want to be 1 above the level of the target.
            // However, if that puts us in the ground, we're instead 1 above the
            // level of the hex right before the target.
            int otherEl = 0;
            Hex hex2 = game.getBoard().getHex(prev.getPosition());
            otherEl = Math.max(0, hex2.terrainLevel(Terrains.BLDG_ELEV));
            if (otherEl > getElevation()) {
                setElevation(otherEl);
            }
            setElevation(getElevation() + 1);
        } else if (isJumping()) {
            Hex hex = game.getBoard().getHex(getPosition());
            int maxElevation = (entity.getJumpMP() + entity.getElevation() + game
                    .getBoard().getHex(entity.getPosition()).getLevel()) - hex.getLevel();
            int building = hex.terrainLevel(Terrains.BLDG_ELEV);
            int depth = -hex.depth(true);
            // need to adjust depth for potential ice over water
            if ((hex.containsTerrain(Terrains.ICE) && hex
                    .containsTerrain(Terrains.WATER))
                    || (entity.getMovementMode() == EntityMovementMode.HOVER)) {
                depth = 0;
            }
            // grounded DropShips are treated as level 10 buildings for purposes
            // of jumping over
            boolean grdDropship = false;
            if (building < 10) {
                for (Entity inHex : game.getEntitiesVector(getPosition())) {
                    if (inHex.equals(entity)) {
                        continue;
                    }
                    if ((inHex instanceof Dropship) && !inHex.isAirborne()
                            && !inHex.isSpaceborne()) {
                        building = 10;
                        grdDropship = true;
                    }
                }
            }
            if ((entity instanceof Infantry) && !grdDropship) {
                // infantry can jump into a building
                setElevation(Math.max(depth, Math.min(building, maxElevation)));
            } else {
                setElevation(Math.max(depth, building));
            }
            if (climbMode()
                    && (maxElevation >= hex.terrainLevel(Terrains.BRIDGE_ELEV))) {
                setElevation(Math.max(getElevation(),
                        hex.terrainLevel(Terrains.BRIDGE_ELEV)));
            }
        } else {
            Building bld = game.getBoard().getBuildingAt(getPosition());

            if (bld != null) {
                Hex hex = game.getBoard().getHex(getPosition());
                int maxElevation = (entity.getElevation() + game.getBoard()
                        .getHex(entity.getPosition()).getLevel())
                        - hex.getLevel();

                // Meks can climb up level 2 walls or less while everything
                // can only climb up one level
                if (entity instanceof Mech) {
                    maxElevation += 2;
                } else {
                    maxElevation++;
                }

                if (bld.getType() == Building.WALL) {
                    if (maxElevation >= hex.terrainLevel(Terrains.BLDG_ELEV)) {
                        setElevation(Math.max(getElevation(),
                                hex.terrainLevel(Terrains.BLDG_ELEV)));
                    } else {// if the wall is taller then the unit then they
                        // cannot climb it or enter it
                        return;
                    }
                } else {
                    setElevation(entity
                            .calcElevation(
                                    game.getBoard().getHex(prev.getPosition()),
                                    game.getBoard().getHex(getPosition()),
                                    elevation,
                                    climbMode(),
                                    (entity.getMovementMode() == EntityMovementMode.WIGE)
                                            && (prev.getType() == MoveStepType.CLIMB_MODE_OFF)));
                }
            } else {
                setElevation(entity
                        .calcElevation(
                                game.getBoard().getHex(prev.getPosition()),
                                game.getBoard().getHex(getPosition()),
                                elevation,
                                climbMode(),
                                (entity.getMovementMode() == EntityMovementMode.WIGE)
                                        && (prev.getType() == MoveStepType.CLIMB_MODE_OFF)));
            }
        }

        // if this is a flying aero, then there is no MP cost for moving
        if ((prev.getAltitude() > 0) || game.getBoard().inSpace()) {
            setMp(0);
            // if this is a spheroid in atmosphere then the cost is always one
            // if it is the very first step, we prepend the cost of hovering for convenience
            if (useSpheroidAtmosphere(game, entity)) {
                if (game.getBoard().onGround()) {
                    if ((distance % 8) == 1) {
                        setMp(1);
                    }
                } else {
                    setMp(1);
                }
            }
        } else {
            calcMovementCostFor(game, prev, cachedEntityState);
        }
        // check for water
        if (!isPavementStep()
                && (destHex.terrainLevel(Terrains.WATER) > 0)
                && !(destHex.containsTerrain(Terrains.ICE) && (elevation >= 0))
                && !(destHex.terrainLevel(Terrains.BRIDGE_ELEV) == elevation)
                && (entity.getMovementMode() != EntityMovementMode.HOVER)
                && (entity.getMovementMode() != EntityMovementMode.NAVAL)
                && (entity.getMovementMode() != EntityMovementMode.HYDROFOIL)
                && (entity.getMovementMode() != EntityMovementMode.INF_UMU)
                && (entity.getMovementMode() != EntityMovementMode.SUBMARINE)
                && (entity.getMovementMode() != EntityMovementMode.VTOL)
                && (entity.getMovementMode() != EntityMovementMode.WIGE)
                && !cachedEntityState.hasWorkingMisc(MiscType.F_FULLY_AMPHIBIOUS)) {
            setRunProhibited(true);
        }
        if (entity.getMovedBackwards()
                && !entity.hasQuirk(OptionsConstants.QUIRK_POS_POWER_REVERSE)) {
            setRunProhibited(true);
        }

        int magmaLevel = destHex.terrainLevel(Terrains.MAGMA);
        if (elevation > 0) {
            magmaLevel = 0;
        }
        // Check for fire or magma crust in the new hex.
        if (destHex.containsTerrain(Terrains.FIRE) || (magmaLevel == 1)) {
            heat = 2;
            totalHeat += 2;
        }
        // Check for liquid magma
        else if (magmaLevel == 2) {
            heat = 5;
            totalHeat += 5;
        }

        // Checks for landed dropships collapsing buildings
        if ((entity instanceof Dropship) && !entity.isAirborne()) {
            ArrayList<Coords> secondaryPositions = new ArrayList<>();
            secondaryPositions.add(getPosition());
            for (int dir = 0; dir < 6; dir++) {
                secondaryPositions.add(getPosition().translated(dir));
            }
            for (Coords pos : secondaryPositions) {
                Building bld = game.getBoard().getBuildingAt(pos);
                if (bld != null) {
                    getCrushedBuildingLocs().add(pos);
                    // This is dangerous!
                    danger = true;
                }
            }
        }

        // WiGEs get bonus MP for each string of three consecutive hexes they descend.
        if (entity.getMovementMode() == EntityMovementMode.WIGE
                && getClearance() > 0
                && game.getOptions().booleanOption(OptionsConstants.ADVGRNDMOV_VEHICLE_ADVANCED_MANEUVERS)) {

            if (game.getBoard().getHex(getPosition()).ceiling()
                    < game.getBoard().getHex(prev.getPosition()).ceiling()) {
                nWigeDescent = prev.getNWigeDescent() + 1;
                if (nWigeDescent >= 3) {
                    wigeBonus++;
                    nWigeDescent = 0;
                }
            } else {
                nWigeDescent = 0;
            }
        }

    }

    /**
     * Compile the static move data for this step.
     *
     * @param game The current {@link Game}
     * @param entity the <code>Entity</code> taking this step.
     * @param prev the previous step in the path.
     */
    protected void compile(final Game game, final Entity entity, MoveStep prev, CachedEntityState cachedEntityState) {
        final boolean isInfantry = entity instanceof Infantry;
        boolean isFieldArtillery = (entity instanceof Infantry)
                && ((Infantry) entity).hasActiveFieldArtillery();
        copy(game, prev);

        // Is this the first step?
        if (prev == null) {
            prev = new MoveStep(null, MoveStepType.FORWARDS);
            prev.setFromEntity(entity, game);
            prev.isCarefulPath = isCareful();
            prev.isJumpingPath = isJumping();
            setFirstStep(prev.mpUsed == 0); // Bug 1519330 - its not a first step when continuing after a fall
        } else if (prev.isFirstStep() // Some step types don't remove first step status
                && ((prev.getType() == MoveStepType.CLIMB_MODE_ON)
                        || (prev.getType() == MoveStepType.CLIMB_MODE_OFF))) {
            setFirstStep(true);
        }
        switch (getType()) {
            case UNLOAD:
            case DISCONNECT:
                // Infantry in immobilized transporters get
                // a special "unload stranded" game turn.
                // So do trailers on an immobilized tractor
                hasEverUnloaded = true;
                setMp(0);
                break;
            case LOAD:
            case TOW:
                setMp(1);
                break;
            case MOUNT:
                setMp(0);
                break;
            case TURN_LEFT:
            case TURN_RIGHT:
                // Check for pavement movement.
                if (!entity.isAirborne() && Compute.canMoveOnPavement(game, prev.getPosition(), getPosition(), this)) {
                    setPavementStep(true);
                } else {
                    setPavementStep(false);
                    setOnlyPavement(false);
                }

                // Infantry can turn for free, except for field artillery
                setMp((isJumping() || isHasJustStood() || (isInfantry && !isFieldArtillery)) ? 0
                        : 1);
                setNStraight(0);
                if (entity.isAirborne() && (entity.isAero())) {
                    setMp(asfTurnCost(game, getType(), entity));
                    setNTurns(getNTurns() + 1);

                    if (useAeroAtmosphere(game, entity)) {
                        setFreeTurn(false);
                    }
                }

                // tripods with all their legs only pay for their first facing change
                if ((getEntity() instanceof TripodMech) && (((Mech) getEntity()).countBadLegs() < 1)
                        && ((prev.type == MoveStepType.TURN_LEFT) || (prev.type == MoveStepType.TURN_RIGHT))) {
                    setMp(0);
                }
                if (entity.isDropping()) {
                    setMp(0);
                }
                adjustFacing(getType());
                break;
            case BACKWARDS:
                moveInDir((getFacing() + 3) % 6);
                setThisStepBackwards(true);
                if (!entity.hasQuirk(OptionsConstants.QUIRK_POS_POWER_REVERSE)) {
                    setRunProhibited(true);
                }
                compileMove(game, entity, prev, cachedEntityState);
                break;
            case FORWARDS:
            case DFA:
            case SWIM:
                // step forwards or backwards
                moveInDir(getFacing());
                setThisStepBackwards(false);
                compileMove(game, entity, prev, cachedEntityState);
                break;
            case CHARGE:
                if (!(entity.isAirborne()) || !game.useVectorMove()) {
                    moveInDir(getFacing());
                    setThisStepBackwards(false);
                    compileMove(game, entity, prev, cachedEntityState);
                }
                break;
            case LATERAL_LEFT_BACKWARDS:
            case LATERAL_RIGHT_BACKWARDS:
                moveInDir((MovePath.getAdjustedFacing(getFacing(),
                        MovePath.turnForLateralShift(getType())) + 3) % 6);
                setThisStepBackwards(true);
                if (!entity.hasQuirk(OptionsConstants.QUIRK_POS_POWER_REVERSE)) {
                    setRunProhibited(true);
                }
                compileMove(game, entity, prev, cachedEntityState);
                if (entity.isAirborne()) {
                    setMp(0);
                } else if (entity.isUsingManAce()
                        & (entity instanceof QuadMech)) {
                    setMp(getMp());
                } else if (isJumping() &&
                        (entity.getJumpType() == Mech.JUMP_BOOSTER)) {
                    setMp(1);
                } else {
                    setMp(getMp() + 1); // +1 for side step
                }
                break;
            case LATERAL_LEFT:
            case LATERAL_RIGHT:
                moveInDir(MovePath.getAdjustedFacing(getFacing(),
                        MovePath.turnForLateralShift(getType())));
                setThisStepBackwards(false);
                compileMove(game, entity, prev, cachedEntityState);
                if (entity.isAirborne()) {
                    setMp(0);
                } else if (entity.isUsingManAce()
                        & (entity instanceof QuadMech)) {
                    setMp(getMp());
                } else if (isJumping() &&
                        (entity.getJumpType() == Mech.JUMP_BOOSTER)) {
                    setMp(1);
                } else {
                    setMp(getMp() + 1); // +1 for side step
                }
                break;
            case GET_UP:
                // mechs with 1 MP are allowed to get up
                setMp(cachedEntityState.getRunMP() == 1 ? 1 : 2);
                setHasJustStood(true);
                break;
            case CAREFUL_STAND:
                if (cachedEntityState.getWalkMP() <= 2) {
                    entity.setCarefulStand(false);
                    setMp(cachedEntityState.getRunMP() == 1 ? 1 : 2);
                } else {
                    setMp(cachedEntityState.getWalkMP());
                }
                setHasJustStood(true);
                break;
            case GO_PRONE:
                if (!entity.isHullDown()) {
                    setMp(1);
                }
                break;
            case START_JUMP:
                entity.setIsJumpingNow(true);
                break;
            case UP:
                if (entity.isAirborne()) {
                    setAltitude(altitude + 1);
                    setMp(2);
                } else {
                    if (entity.getMovementMode() == EntityMovementMode.WIGE) {
                        // If on the ground, pay liftoff cost. If airborne, pay 1 MP to increase elevation
                        // (LAMs and glider protomechs only)
                        if (getClearance() == 0) {
                            setMp((entity instanceof Protomech) ? 4 : 5);
                        } else {
                            setMp(1);
                        }
                    } else {
                        if (entity instanceof Protomech) {
                            setMp(isJumping() ? 0 : 2);
                        } else {
                            setMp(isJumping() ? 0 : 1);
                        }
                    }
                    setElevation(elevation + 1);
                }
                break;
            case DOWN:
                if (entity.isAirborne()) {
                    setAltitude(altitude - 1);
                    // it costs nothing (and may increase velocity)
                    setMp(0);
                    setNDown(getNDown() + 1);
                } else {
                    setElevation(elevation - 1);
                    if (entity.getMovementMode() == EntityMovementMode.WIGE) {
                        setMp(0);
                    } else {
                        if (entity instanceof Protomech) {
                            setMp(isJumping() ? 0 : 2);
                        } else {
                            setMp(isJumping() ? 0 : 1);
                        }
                    }
                }
                break;
            case HULL_DOWN:
                if (isProne() && (entity instanceof Mech)) {
                    int mpUsed = 1;
                    if (entity instanceof BipedMech) {
                        for (int location = Mech.LOC_RLEG; location <= Mech.LOC_LLEG; location++) {
                            if (entity.isLocationBad(location)) {
                                mpUsed += 99;
                                break;
                            }
                            mpUsed += ((Mech) entity)
                                    .countLegActuatorCrits(location);
                            if (((Mech) entity).legHasHipCrit(location)) {
                                mpUsed += 1;
                            }
                        }
                        setHasJustStood(true);
                    } else {
                        for (int location = Mech.LOC_RARM; location <= Mech.LOC_LLEG; location++) {
                            if (entity.isLocationBad(location)) {
                                mpUsed += 99;
                                break;
                            }
                            mpUsed += ((QuadMech) entity)
                                    .countLegActuatorCrits(location);
                            if (((QuadMech) entity).legHasHipCrit(location)) {
                                mpUsed += 1;
                            }
                        }
                    }
                    setMp(mpUsed);
                } else {
                    setMp(2);
                }
                break;
            case CLIMB_MODE_ON:
                setClimbMode(true);
                break;
            case CLIMB_MODE_OFF:
                setClimbMode(false);
                break;
            case SHAKE_OFF_SWARMERS:
                // Counts as flank move but you can only use cruise MP
                setMp(cachedEntityState.getRunMP() - cachedEntityState.getWalkMP());
                break;
            case TAKEOFF:
            case VTAKEOFF:
                setMp(0);
                break;
            case LAND:
            case VLAND:
                setMp(0);
                setAltitude(0);
                break;
            case ACCN:
                setVelocityN(getVelocityN() + 1);
                setMp(1);
                break;
            case DECN:
                setVelocityN(getVelocityN() - 1);
                setMp(1);
                break;
            case ACC:
                setVelocity(getVelocity() + 1);
                setVelocityLeft(getVelocityLeft() + 1);
                setMp(1);
                break;
            case DEC:
                setVelocity(getVelocity() - 1);
                setVelocityLeft(getVelocityLeft() - 1);
                setMp(1);
                break;
            case EVADE:
                setEvading(true);
                if (entity.isAirborne()) {
                    setMp(2);
                }
                break;
            case SHUTDOWN:
                setShuttingDown(true);
                // Do something here...
                break;
            case STARTUP:
                setStartingUp(true);
                // Do something here...
                break;
            case SELF_DESTRUCT:
                setSelfDestructing(true);
                // Do something here...
                break;
            case ROLL:
                if (prev.isRolled) {
                    isRolled = false;
                } else {
                    isRolled = true;
                }
                // doesn't cost anything if previous was a yaw
                if (prev.getType() != MoveStepType.YAW) {
                    setMp(1);
                    setNRolls(getNRolls() + 1);
                } else {
                    setMp(0);
                }
                break;
            case LAUNCH:
            case DROP:
                hasEverUnloaded = true;
                setMp(0);
                break;
            case RECOVER:
                setMp(0);
                break;
            case JOIN:
                setMp(0);
                break;
            case THRUST:
                setVectors(Compute.changeVectors(getVectors(), getFacing()));
                setMp(1);
                break;
            case YAW:
                setNRolls(getNRolls() + 1);
                reverseFacing();
                setMp(2);
                break;
            case HOVER:
                if (entity.isAero()) {
                    setMp(2);
                } else if (entity.getMovementMode() == EntityMovementMode.WIGE) {
                    if (entity instanceof LandAirMech
                            && entity.getAltitude() > 0) {
                        setMp(10);
                        setElevation(altitude * 10);
                        setAltitude(0);
                    } else {
                        setMp(entity instanceof Protomech? 4 : 5);
                    }
                }
                break;
            case MANEUVER:
                int cost = ManeuverType.getCost(getManeuverType(),
                        getVelocity());
                if (entity.isUsingManAce()) {
                    cost = Math.max(cost - 1, 0);
                }
                setMp(cost);
                break;
            case LOOP:
                setVelocityLeft(getVelocityLeft() - 4);
                setMp(0);
                break;
            case CONVERT_MODE:
                if (entity instanceof QuadVee) {
                    setMp(((QuadVee) entity).conversionCost());
                } else {
                    setMp(0);
                }
                movementMode = entity.nextConversionMode(prev.getMovementMode());
                break;
            case BOOTLEGGER:
                reverseFacing();
                setMp(2);
                break;
            case BRACE:
                setMp(entity.getBraceMPCost());
                break;
            case CHAFF:
            default:
                setMp(0);
        }

        if (noCost) {
            setMp(0);
        }

        if (type != MoveStepType.CONVERT_MODE) {
            movementMode = prev.getMovementMode();
        }

        // Tanks can just drive out of hull-down.  If we're a tank, and we moved
        //  then we are no longer hull-down.
        if ((entity instanceof Tank
                || (entity instanceof QuadVee
                        && entity.getConversionMode() == QuadVee.CONV_MODE_VEHICLE))
                && (distance > 0)) {
            setHullDown(false);
        }

        // Update the entity's total MP used.
        addMpUsed(getMp());

        // Check for a stacking violation.
        final Entity violation = Compute.stackingViolation(game,
                entity, getElevation(), getPosition(), null, climbMode);
        if ((violation != null) && (getType() != MoveStepType.CHARGE)
                && (getType() != MoveStepType.DFA)) {
            setStackingViolation(true);
        }

        // set moveType, illegal, trouble flags
        compileIllegal(game, entity, prev, cachedEntityState);
    }

    /**
     * Returns whether the two step types contain opposite turns
     */
    boolean oppositeTurn(MoveStep turn2) {
        switch (type) {
            case TURN_LEFT:
                return turn2.getType() == MoveStepType.TURN_RIGHT;
            case TURN_RIGHT:
                return turn2.getType() == MoveStepType.TURN_LEFT;
            default:
                return false;
        }
    }

    protected void setElevation(int el) {
        elevation = el;
    }

    protected void setAltitude(int alt) {
        altitude = alt;
    }

    /**
     * Takes the given state as the previous state and sets flags from it.
     *
     * @param game The current {@link Game}
     * @param prev
     */
    public void copy(final Game game, MoveStep prev) {
        if (prev == null) {
            setFromEntity(getEntity(), game);
            return;
        }
        hasJustStood = prev.hasJustStood;
        facing = prev.getFacing();
        position = prev.getPosition();

        distance = prev.getDistance();
        mpUsed = prev.mpUsed;
        totalHeat = prev.totalHeat;
        isPavementStep = prev.isPavementStep;
        onlyPavement = prev.onlyPavement;
        wigeBonus = prev.wigeBonus;
        nWigeDescent = prev.nWigeDescent;
        thisStepBackwards = prev.thisStepBackwards;
        isProne = prev.isProne;
        isFlying = prev.isFlying;
        isHullDown = prev.isHullDown;
        climbMode = prev.climbMode;
        isRunProhibited = prev.isRunProhibited;
        hasEverUnloaded = prev.hasEverUnloaded;
        elevation = prev.elevation;
        altitude = prev.altitude;
        velocity = prev.velocity;
        velocityN = prev.velocityN;
        velocityLeft = prev.velocityLeft;
        nTurns = prev.nTurns;
        isEvading = prev.isEvading;
        isShuttingDown = prev.isShuttingDown;
        isStartingUp = prev.isStartingUp;
        isSelfDestructing = prev.isSelfDestructing;
        nRolls = prev.nRolls;
        isRolled = prev.isRolled;
        mv = prev.mv.clone();
        freeTurn = prev.freeTurn;
        nStraight = prev.nStraight;
        nDown = prev.nDown;
        nMoved = prev.nMoved;
    }

    /**
     * Sets this state as coming from the entity.
     *
     * @param entity
     */
    public void setFromEntity(Entity entity, Game game) {
        this.entity = entity;
        position = entity.getPosition();
        facing = entity.getFacing();
        // elevation
        mpUsed = entity.mpUsed;
        distance = entity.delta_distance;
        isProne = entity.isProne();
        isFlying = entity.isAirborne() || entity.isAirborneVTOLorWIGE();
        isHullDown = entity.isHullDown();
        climbMode = entity.climbMode();
        thisStepBackwards = entity.inReverse;

        // Moving in reverse prohibits running
        if (thisStepBackwards) {
            isRunProhibited = true;
        }

        elevation = entity.getElevation();
        altitude = entity.getAltitude();
        movementType = entity.moved;
        movementMode = entity.getMovementMode();

        isRolled = false;
        freeTurn = false;
        nStraight = 0;
        nDown = 0;

        // for some reason, doing it directly is adjusting the entity's vector
        // itself
        // which causes problems when canceling the action
        // what a hack. but I can't figure out what is going wrong
        // this works but god is it ugly
        // TODO: figure this out
        int[] tempMv = entity.getVectors();

        mv = new int[]{0, 0, 0, 0, 0, 0};
        for (int i = 0; i < 6; i++) {
            mv[i] = tempMv[i];
        }

        // if ASF get velocity
        if (entity.isAero()) {
            IAero a = (IAero) entity;
            velocity = a.getCurrentVelocity();
            velocityN = a.getNextVelocity();
            velocityLeft = a.getCurrentVelocity() - entity.delta_distance;
            if (game.getBoard().onGround()) {
                velocityLeft = a.getCurrentVelocity() - (entity.delta_distance / 16);
            }
            isRolled = false;// a.isRolled();
            nStraight = a.getStraightMoves();
            if (dueFreeTurn()) {
                setFreeTurn(true);
            }
        }

        EntityMovementMode nMove = entity.getMovementMode();

        // tanks with stunned crew can't flank
        if ((entity instanceof Tank) && (((Tank) entity).getStunnedTurns() > 0)) {
            isRunProhibited = true;
        }

        //Cannot run while using Mek tracks
        if (entity instanceof Mech && entity.getMovementMode() == EntityMovementMode.TRACKED
                && !(entity instanceof QuadVee)) {
            isRunProhibited = true;
        }

        // check pavement & water
        if (position != null) {
            Hex curHex = game.getBoard().getHex(position);
            if (curHex.hasPavement()) {
                onlyPavement = true;
                isPavementStep = true;
                // if we previously moved, and didn't get a pavement bonus, we
                // shouldn't now get one, either (this can happen when skidding
                // onto a pavement hex
                if (!entity.gotPavementBonus
                        && (entity.delta_distance > 0)) {
                    onlyPavement = false;
                }
            }
            // if entity already moved into water it can't run now
            if (curHex.containsTerrain(Terrains.WATER)
                    && (entity.getElevation() < 0) && (distance > 0)
                    && (nMove != EntityMovementMode.NAVAL)
                    && (nMove != EntityMovementMode.HYDROFOIL)
                    && (nMove != EntityMovementMode.SUBMARINE)
                    && (nMove != EntityMovementMode.INF_UMU)) {
                isRunProhibited = true;
            }
        }
    }

    /**
     * Adjusts facing to comply with the type of step indicated.
     *
     * @param stepType
     */
    public void adjustFacing(MoveStepType stepType) {
        facing = MovePath.getAdjustedFacing(facing, stepType);
    }

    /**
     * For yaws, reverse the current facing
     */
    public void reverseFacing() {
        facing = MovePath.getAdjustedFacing(facing, MoveStepType.TURN_RIGHT);
        facing = MovePath.getAdjustedFacing(facing, MoveStepType.TURN_RIGHT);
        facing = MovePath.getAdjustedFacing(facing, MoveStepType.TURN_RIGHT);
    }

    /**
     * Moves the position one hex in the direction indicated. Does not change
     * facing.
     *
     * @param dir
     */
    public void moveInDir(int dir) {
        position = position.translated(dir);
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
    public int getLeapDistance() {
        return leapDistance;
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
     * Returns true if the entity is flying on this step.
     *
     * @return
     */
    public boolean isFlying() {
        return isFlying;
    }

    public boolean isHullDown() {
        return isHullDown;
    }

    public boolean climbMode() {
        return climbMode;
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
    public boolean isUsingSupercharger() {
        return isUsingSupercharger;
    }


    public boolean isEvading() {
        return isEvading;
    }

    public boolean isShuttingDown() {
        return isShuttingDown;
    }

    public boolean isStartingUp() {
        return isStartingUp;
    }

    public boolean isSelfDestructing() {
        return isSelfDestructing;
    }

    public boolean isRolled() {
        return isRolled;
    }

    public boolean isVTOLBombingStep() {
        return targetType == Targetable.TYPE_HEX_AERO_BOMB;
    }

    public boolean isStrafingStep() {
        return targetType == Targetable.TYPE_HEX_CLEAR;
    }

    /**
     * Determine if this is a legal step as part of the supplied MovePath.
     *
     * @param path  A MovePath that contains this step.
     * @return <code>true</code> if the step is legal. <code>false</code>
     *         otherwise.
     */
    public boolean isLegal(MovePath path) {
        // A step is legal if it's static movement type is not illegal,
        // and it is either a valid end position, or not an end position.
        return ((movementType != EntityMovementType.MOVE_ILLEGAL)
                && (isLegalEndPos() || !isEndPos(path)));
    }

    /**
     * Return this step's movement type.
     *
     * @return the <code>int</code> constant for this step's movement type.
     */
    public EntityMovementType getMovementType(boolean isLastStep) {
        EntityMovementType moveType = movementType;
        // If this step's position is the end of the path, and it is not
        // a valid end postion, then the movement type is "illegal".
        if (isLastStep && !isLegalEndPos()) {
            moveType = EntityMovementType.MOVE_ILLEGAL;
        }
        return moveType;
    }

    public EntityMovementMode getMovementMode() {
        if (movementMode == EntityMovementMode.NONE) {
            return getEntity().getMovementMode();
        }
        return movementMode;
    }

    /**
     * Check to see if this step's position is a valid end of a path.
     *
     * @return <code>true</code> if this step's position is a legal end of a
     *         path. If the step is not legal for an end of a path, then
     *         <code>false</code> is returned.
     */
    public boolean isLegalEndPos() {
        // Can't be a stacking violation.
        boolean legal = true;
        if (isStackingViolation) {
            legal = false;
        } else if (terrainInvalid) {
            // Can't be into invalid terrain.
            legal = false;
        } else if (isJumping() && (distance == 0)) {
            // Can't jump zero hexes.
            legal = false;
        } else if (hasEverUnloaded && (type != MoveStepType.UNLOAD)
                && (type != MoveStepType.LAUNCH) && (type != MoveStepType.DROP)
                && (type != MoveStepType.UNDOCK) && (type != MoveStepType.DISCONNECT)
                && (type != MoveStepType.CHAFF)
                && (getAltitude() == 0)) {
            // Can't be after unloading BA/inf
            legal = false;
        }
        return legal;
    }

    /**
     * Update this step's status as the ending position of a path.
     *
     * @param isEnd the <code>boolean</code> flag that specifies that this step's
     *              position is the end of a path.
     * @return <code>true</code> if the path needs to keep updating the steps.
     *         <code>false</code> if the update of the path is complete.
     * @see <code>#isLegalEndPos()</code>
     * @see <code>#isEndPos</code>
     * @see <code>MovePath#addStep( MoveStep )</code>
     */
    public boolean setEndPos(boolean isEnd) {
        boolean isEndPos = true;
        // A step that is always illegal is always the end of the path.
        if (EntityMovementType.MOVE_ILLEGAL == movementType) {
            isEnd = true;
        }

        // If this step didn't already know it's status as the ending
        // position of a path, then there are more updates to do.
        boolean moreUpdates = (isEndPos != isEnd);
        isEndPos = isEnd;

        // If this step isn't the end step anymore, we might not be in danger
        // after all
        Hex pos = getGame().getBoard().getHex(position);
        if (getGame().getOptions().booleanOption(OptionsConstants.ADVGRNDMOV_PSR_JUMP_HEAVY_WOODS)) {
            if (!isEnd
                    && isJumping()
                    && (pos.containsTerrain(Terrains.WOODS, 2)
                            || pos.containsTerrain(Terrains.WOODS, 3))) {
                danger = false;
                pastDanger = false;
            }
        }

        return moreUpdates;
    }

    /**
     * Returns true if a step is considered to be in an end position for the
     * given MovePath. A step is in an end position if it is the last legal
     * step, or is an illegal step past the last legal step.
     *
     * @param path
     * @return
     */
    public boolean isEndPos(MovePath path) {
        // A step that is illegal is always the end of the path.
        if (EntityMovementType.MOVE_ILLEGAL == movementType) {
            return true;
        }

        if (path == null) {
            return true;
        }

        // A step is an end position if it is the last legal step.
        Vector<MoveStep> steps = path.getStepVector();
        // Starting from the end, each step is considered the last step until
        // we find a legal last step
        boolean lastStep = true;
        for (int i = steps.size() - 1; i >= 0; i--) {
            MoveStep step = steps.get(i);
            boolean stepMatch = this.equals(step);
            if (lastStep) {
                lastStep &= step.getMovementType(true) == EntityMovementType.MOVE_ILLEGAL;
            }
            // If there is a legal step after us, we're not the end
            if ((step.getMovementType(lastStep) != EntityMovementType.MOVE_ILLEGAL)
                    && !stepMatch) {
                return false;
            // If we found the current step, no need to check the others
            } else if (stepMatch) {
                return true;
            }
        }
        // Shouldn't reach here, since this step is assumed be in the step list
        return false;
    }


    public int getMpUsed() {
        return mpUsed;
    }

    public boolean isOnlyPavement() {
        return onlyPavement;
    }

    public int getWiGEBonus() {
        return wigeBonus;
    }

    public int getNWigeDescent() {
        return nWigeDescent;
    }

    public boolean isPastDanger() {
        return pastDanger;
    }

    public void setPastDanger(boolean pastDanger) {
        this.pastDanger = pastDanger;
    }

    public boolean isDocking() {
        return docking;
    }

    public void setDocking(boolean tf) {
        docking = tf;
    }

    public Coords getPosition() {
        return position;
    }

    public boolean isPrevStepOnPavement() {
        return prevStepOnPavement;
    }

    public int getTargetNumberMASC() {
        return targetNumberMASC;
    }

    public int getTargetNumberSupercharger() {
        return targetNumberSupercharger;
    }

    public boolean isThisStepBackwards() {
        return thisStepBackwards;
    }

    public void setDanger(boolean b) {
        danger = b;
    }

    protected void setDistance(int i) {
        distance = i;
    }

    protected void setLeapDistance(int i) {
        leapDistance = i;
    }

    protected void setFacing(int i) {
        facing = i;
    }

    protected void setFirstStep(boolean b) {
        firstStep = b;
    }

    protected void setHasJustStood(boolean b) {
        hasJustStood = b;
    }

    protected void setPavementStep(boolean b) {
        isPavementStep = b;
    }

    protected void setProne(boolean b) {
        isProne = b;
    }

    protected void setFlying(boolean b) {
        isFlying = b;
    }

    protected void setHullDown(boolean b) {
        isHullDown = b;
    }

    protected void setClimbMode(boolean b) {
        climbMode = b;
    }

    protected void setTurning(boolean b) {
        isTurning = b;
    }

    protected void setUnloaded(boolean b) {
        isUnloaded = b;
        if (b) {
            hasEverUnloaded = true;
        }
    }

    protected void setUsingMASC(boolean b) {
        isUsingMASC = b;
    }

    protected void setUsingSupercharger(boolean b) {
        isUsingSupercharger = b;
    }

    public void setMovementType(EntityMovementType i) {
        movementType = i;
    }

    protected void setEvading(boolean b) {
        isEvading = b;
    }

    protected void setShuttingDown(boolean b) {
        isShuttingDown = b;
    }

    protected void setStartingUp(boolean b) {
        isStartingUp = b;
    }

    protected void setSelfDestructing(boolean b) {
        isSelfDestructing = b;
    }

    protected void setOnlyPavement(boolean b) {
        onlyPavement = b;
    }

    protected void setWiGEBonus(int i) {
        wigeBonus = i;
    }

    protected void setTargetNumberMASC(int i) {
        targetNumberMASC = i;
    }

    protected void setTargetNumberSupercharger(int i) {
        targetNumberSupercharger = i;
    }

    protected void setThisStepBackwards(boolean b) {
        thisStepBackwards = b;
    }

    /**
     * Returns the mp used for just this step.
     */
    public int getMp() {
        return mp;
    }

    /**
     * sets the mp for this step.
     *
     * @param i the mp for this step.
     */
    protected void setMp(int i) {
        mp = i;
    }

    protected void setRunProhibited(boolean isRunProhibited) {
        this.isRunProhibited = isRunProhibited;
    }

    boolean isRunProhibited() {
        return isRunProhibited;
    }

    protected void setStackingViolation(boolean isStackingViolation) {
        this.isStackingViolation = isStackingViolation;
    }

    boolean isStackingViolation() {
        return isStackingViolation;
    }

    /**
     * This function checks that a step is legal. And adjust the movement type.
     * This only checks for things that can make this step by itself illegal.
     * Things that can make a step illegal as part of a movement path are
     * considered in MovePath.addStep.
     *
     * @param game The current {@link Game}
     * @param entity
     * @param prev
     */
    private void compileIllegal(final Game game, final Entity entity,
            final MoveStep prev, CachedEntityState cachedEntityState) {
        final MoveStepType stepType = getType();
        final boolean isInfantry = entity instanceof Infantry;
        final boolean isTank = entity instanceof Tank;

        Coords curPos = getPosition();
        Coords lastPos = prev.getPosition();
        boolean isUnjammingRAC = entity.isUnjammingRAC();
        prevStepOnPavement = prev.isPavementStep();
        isTurning = prev.isTurning();
        isUnloaded = prev.isUnloaded();

        // Infantry get a first step if all they've done is spin on the spot.
        if (isInfantry && ((getMpUsed() - getMp()) == 0)) {
            setFirstStep(true);

            // getMpUsed() is the MPs used in the whole MovePath
            // getMp() is the MPs used in the last (illegal) step (this step)
            // if the difference between the whole path and this step is 0
            // then this must be their first step
        }

        // guilty until proven innocent
        movementType = EntityMovementType.MOVE_ILLEGAL;

        // Crushing buildings creates rubble, and Dropships can't drive on
        // rubble, so they get stuck
        if ((entity instanceof Dropship)
                && !prev.getCrushedBuildingLocs().isEmpty()) {
            return;
        }
        // AERO STUFF
        // I am going to put in a whole seperate section for Aeros and just
        // return from it
        // only if Aeros are airborne, otherwise they should move like other
        // units
        if (type == MoveStepType.HOVER && entity instanceof LandAirMech
                && entity.getMovementMode() == EntityMovementMode.WIGE
                && entity.getAltitude() <= 3) {
            if (mpUsed <= cachedEntityState.getWalkMP()) {
                movementType = EntityMovementType.MOVE_VTOL_WALK;
            } else if (mpUsed <= cachedEntityState.getRunMP()) {
                movementType = EntityMovementType.MOVE_VTOL_RUN;
            } else {
                movementType = EntityMovementType.MOVE_ILLEGAL;
            }
            return;
        }

        if ((prev.getAltitude() > 0) || game.getBoard().inSpace()) {
            //Ejected crew/pilots just drift or parachute, resulting in a move_none type
            if (entity instanceof EjectedCrew) {
                movementType = EntityMovementType.MOVE_NONE;
                return;
            }

            // If airborne and some other non-Aero unit then everything is illegal, except
            // turns and AirMech
            if (!entity.isAero()) {
                switch (type) {
                    case TURN_LEFT:
                    case TURN_RIGHT:
                        movementType = EntityMovementType.MOVE_WALK;
                    case CONVERT_MODE:
                        movementType = EntityMovementType.MOVE_NONE;
                    default:
                        break;
                }
                return;
            }

            int tmpSafeTh = cachedEntityState.getWalkMP();
            IAero a = (IAero) entity;

            // if the vessel is "immobile" due to shutdown or pilot black out
            // then all moves are illegal
            if (entity.isImmobile()) {
                return;
            }

            // can't let players do an illegal move and use that to go less than
            // velocity
            if (!isFirstStep()
                    && (prev.getMovementType(false) == EntityMovementType.MOVE_ILLEGAL)) {
                return;
            }

            // check the fuel requirements
            if (game.getOptions().booleanOption(OptionsConstants.ADVAERORULES_FUEL_CONSUMPTION)
                    && entity.hasEngine() && !entity.getEngine().isSolar()) {
                int fuelUsed = mpUsed + Math.max(mpUsed - cachedEntityState.getWalkMP(), 0);
                if (fuelUsed > a.getCurrentFuel()) {
                    return;
                }
            }

            // **Space turning limits**//
            if (game.getBoard().inSpace()) {
                // space stations can only turn and launch space craft
                if ((entity instanceof SpaceStation)
                        && !((type == MoveStepType.TURN_LEFT)
                                || (type == MoveStepType.TURN_RIGHT)
                                || (type == MoveStepType.LAUNCH)
                                || (type == MoveStepType.UNDOCK))) {
                    return;
                }

                // unless velocity is zero ASFs must move forward one hex before
                // making turns in space
                if (!game.useVectorMove()
                        && (distance == 0)
                        && (velocity != 0)
                        && ((type == MoveStepType.TURN_LEFT) || (type == MoveStepType.TURN_RIGHT))) {
                    return;
                }

                // no more than two turns in one hex unless velocity is zero for
                // anything except ASF in space
                if (!game.useVectorMove() && (a instanceof SmallCraft)
                        && (velocity != 0) && (getNTurns() > 2)) {
                    return;
                }

                // for warships the limit is one
                if (!game.useVectorMove() && (a instanceof Jumpship)
                        && (velocity != 0) && (getNTurns() > 1)) {
                    return;
                }
            }

            // atmosphere has its own rules about turning
            if (useAeroAtmosphere(game, entity)
                    && ((type == MoveStepType.TURN_LEFT) || (type == MoveStepType.TURN_RIGHT))
                    && !prev.canAeroTurn(game)) {
                return;
            }

            // spheroids in atmosphere can move a max of 1 hex on the low atmo map
            // and 8 hexes on the ground map, regardless of any other considerations
            // unless they're out of control, in which case, well...
            if (useSpheroidAtmosphere(game, entity) &&
                    (((IAero) entity).isOutControlTotal() ||
                    (!game.getBoard().onGround() && (this.getDistance() > 1) ||
                            (game.getBoard().onGround() && (getDistance() > 8))))) {
                return;
            }

            if ((type == MoveStepType.FORWARDS)
                    && game.getBoard().inAtmosphere() && !a.isOutControl()) {
                Hex desth = game.getBoard().getHex(getPosition());
                if (altitude <= desth.ceiling(true)) {
                    return; // can't fly into a cliff face or woods (unless out
                    // of control)
                }
            }

            /*
             * TODO: better to disable this in movement display // don't let them
             * evade more than once if (type == MoveStepType.EVADE) {
             * if (isEvading) { return; } else { setEvading(true); } }
             */

            // check for thruster damage
            if ((type == MoveStepType.TURN_LEFT)
                    && (a.getRightThrustHits() > 2)
                    && !useSpheroidAtmosphere(game, entity)) {
                return;
            }
            if ((type == MoveStepType.TURN_RIGHT)
                    && (a.getLeftThrustHits() > 2)
                    && !useSpheroidAtmosphere(game, entity)) {
                return;
            }

            // no moves after launching fighters, unless we were undocking
            if (!isFirstStep() && (prev.getType() == MoveStepType.LAUNCH) &&
                    (getType() != MoveStepType.UNDOCK)) {
                return;
            }

            // no moves after launching dropships, unless we are launching
            if (!isFirstStep() && (prev.getType() == MoveStepType.UNDOCK) &&
                    (getType() != MoveStepType.LAUNCH)) {
                return;
            }

            // no moves after being recovered
            if (!isFirstStep() && (prev.getType() == MoveStepType.RECOVER)) {
                return;
            }

            // no moves after joining
            if (!isFirstStep() && (prev.getType() == MoveStepType.JOIN)) {
                return;
            }

            // no moves after landing
            if (!isFirstStep()
                    && ((prev.getType() == MoveStepType.LAND) || (prev
                    .getType() == MoveStepType.VLAND))) {
                return;
            }

            // can only use safe thrust when ammo (or bomb) dumping
            // (unless out of control?)
            boolean bDumping = false;// a.isDumpingBombs();
            for (Mounted mo : entity.getAmmo()) {
                if (mo.isDumping()) {
                    bDumping = true;
                    break;
                }
            }

            if (bDumping && (getMpUsed() > tmpSafeTh) && !a.isRandomMove()) {
                return;
            }

            // check to make sure there is velocity left to spend
            if ((getVelocityLeft() >= 0) || useSpheroidAtmosphere(game, entity)) {
                // when aeros are flying on the ground mapsheet we need an
                // additional check
                // because velocityLeft is only decremented at intervals of 16
                // hexes
                if (useAeroAtmosphere(game, entity)
                        && game.getBoard().onGround()
                        && (getVelocityLeft() == 0) && (getNMoved() > 0)) {
                    return;
                }
                if (getMpUsed() <= tmpSafeTh) {
                    movementType = EntityMovementType.MOVE_SAFE_THRUST;
                } else if (getMpUsed() <= cachedEntityState.getRunMPwithoutMASC()) {
                    movementType = EntityMovementType.MOVE_OVER_THRUST;
                } else if (a.isRandomMove()) {
                    // if random move then allow it to be over thrust allowance
                    movementType = EntityMovementType.MOVE_OVER_THRUST;
                }
            }

            return;
        } // end AERO stuff

        if (prev.isDiggingIn) {
            isDiggingIn = true;
            if ((type != MoveStepType.TURN_LEFT)
                    && (type != MoveStepType.TURN_RIGHT)) {
                return; // can't move when digging in
            }
            movementType = EntityMovementType.MOVE_NONE;
        } else if ((type == MoveStepType.DIG_IN)
                || (type == MoveStepType.FORTIFY)) {
            if ((!isInfantry && !isTank) || !isFirstStep()) {
                return; // can't dig in
            }

            if (isInfantry) {
                Infantry inf = (Infantry) entity;
                if ((inf.getDugIn() != Infantry.DUG_IN_NONE)
                        && (inf.getDugIn() != Infantry.DUG_IN_COMPLETE)) {
                    return; // Already dug in
                }
            }

            if (game.getBoard().getHex(curPos)
                    .containsTerrain(Terrains.PAVEMENT)
                    || game.getBoard().getHex(curPos)
                    .containsTerrain(Terrains.FORTIFIED)
                    || game.getBoard().getHex(curPos)
                    .containsTerrain(Terrains.BUILDING)
                    || game.getBoard().getHex(curPos)
                    .containsTerrain(Terrains.ROAD)) {
                // already fortified - pointless, or terrain is illegal for
                // digging in
                return;
            }
            isDiggingIn = true;
            movementType = EntityMovementType.MOVE_NONE;
        }

        // Taking cover or bracing should happen as the last action
        if (prev.isTakingCover || (prev.braceLocation != Entity.LOC_NONE)) {
            return;
        }

        if (type == MoveStepType.TAKE_COVER) {
            // Only Infantry can take cover
            if (!isInfantry) {
                return;
            }
            // If there's no valid cover, it's illegal
            if (!Infantry.hasValidCover(game, getPosition(), getElevation())) {
                return;
            }
            isTakingCover = true;
            movementType = prev.getMovementType(false);
            return;
        }

        // WIGEs can take off on their first step...
        if ((type == MoveStepType.UP) && (entity.getMovementMode() == EntityMovementMode.WIGE)
                && (prev.getClearance() == 0)) {
            if (firstStep && (cachedEntityState.getRunMP() >= mp)) {
                movementType = EntityMovementType.MOVE_VTOL_WALK;
            } else {
                movementType = EntityMovementType.MOVE_ILLEGAL;
                return;
            }
        }

        // WIGEs need to be able to land too, or even descend
        if (entity.getMovementMode() == EntityMovementMode.WIGE
                && type == MoveStepType.DOWN
                && getClearance() < prev.getClearance()) { // landing
            if (prev.getMovementType(false) == EntityMovementType.MOVE_VTOL_RUN
                    || prev.getMovementType(false) == EntityMovementType.MOVE_VTOL_SPRINT) {
                movementType = prev.getMovementType(false);
            } else {
                movementType = EntityMovementType.MOVE_VTOL_WALK;
            }
        }

        // check to see if it's trying to flee and can legally do so.
        if ((type == MoveStepType.FLEE) && entity.canFlee()) {
            movementType = EntityMovementType.MOVE_LEGAL;
        }

        if ((type == MoveStepType.CLIMB_MODE_ON)
                || (type == MoveStepType.CLIMB_MODE_OFF)) {
            movementType = prev.movementType;
        }
        // check for ejection (always legal?)
        if (type == MoveStepType.EJECT) {
            movementType = EntityMovementType.MOVE_NONE;
        }
        if (type == MoveStepType.SEARCHLIGHT) {
            movementType = prev.movementType;
        }
        if (type == MoveStepType.UNJAM_RAC) {
            movementType = EntityMovementType.MOVE_NONE;
        }
        // infantry are allowed to clear mines
        if ((type == MoveStepType.CLEAR_MINEFIELD)
                && (entity instanceof Infantry)) {
            movementType = EntityMovementType.MOVE_NONE;
        }
        // check for evasion
        if (type == MoveStepType.EVADE) {
            if (entity.hasHipCrit()
                    || (entity.getMovementMode() == EntityMovementMode.WIGE
                            && (entity instanceof LandAirMech || entity instanceof Protomech)
                            && getClearance() > 0)) {
                movementType = EntityMovementType.MOVE_ILLEGAL;
                return;
            }
            // evading means running
            movementType = EntityMovementType.MOVE_RUN;
        }
        if (type == MoveStepType.SHUTDOWN) {
            movementType = EntityMovementType.MOVE_NONE;
        }
        if (type == MoveStepType.STARTUP) {
            movementType = EntityMovementType.MOVE_NONE;
        }
        if (type == MoveStepType.SELF_DESTRUCT) {
            movementType = EntityMovementType.MOVE_NONE;
        }
        if (type == MoveStepType.CONVERT_MODE) {
            movementType = EntityMovementType.MOVE_NONE;
        }
        if (type == MoveStepType.CHAFF) {
            movementType = EntityMovementType.MOVE_NONE;
        }

        // check for valid jump mp
        if (isJumping()
                && (getMpUsed() <= entity.getJumpMPWithTerrain())
                && !isProne()
                && !isHullDown()
                && !((entity instanceof Protomech) && (entity
                .getInternal(Protomech.LOC_LEG) == IArmorState.ARMOR_DESTROYED))
                && (!entity.isStuck() || entity.canUnstickByJumping())) {
            movementType = EntityMovementType.MOVE_JUMP;
        }

        // legged Protos may make one facing change
        if (isFirstStep()
                && (entity instanceof Protomech)
                && (entity.getInternal(Protomech.LOC_LEG) == IArmorState.ARMOR_DESTROYED)
                && ((stepType == MoveStepType.TURN_LEFT) || (stepType == MoveStepType.TURN_RIGHT))
                && !entity.isStuck()) {
            movementType = EntityMovementType.MOVE_WALK;
        }
        // Infantry that is first stepping and turning is legal
        if (isInfantry
                && ((stepType == MoveStepType.TURN_LEFT) || (stepType == MoveStepType.TURN_RIGHT))
                && isFirstStep()) {
            if (isJumping()) {
                movementType = EntityMovementType.MOVE_JUMP;
            } else {
                movementType = EntityMovementType.MOVE_WALK;
            }
        }

        int bonus = wigeBonus;
        entity.wigeBonus = wigeBonus;
        if (entity.isEligibleForPavementBonus()
                && isOnlyPavement()) {
            bonus++;
            entity.gotPavementBonus = true;
        }
        int tmpWalkMP = cachedEntityState.getWalkMP() + bonus;

        // For entities with neither MASC nor Supercharger, these values will be the same
        int runMPMax = cachedEntityState.getRunMP() + bonus;
        int runMPSingleBoost = cachedEntityState.getRunMPwithOneMASC() + bonus;
        int runMPNoBoost = cachedEntityState.getRunMPwithoutMASC() + bonus;

        // Sprint MP is calculated depending on the entity type
        // For those that cannot sprint, it is the same as run.
        // For those that can, it is the maximum distance it can sprint
        // For entities with neither MASC nor Supercharger, these values will be the same
        int sprintMPMax = cachedEntityState.getSprintMP() + bonus;
        int sprintMPSingleBoost = cachedEntityState.getSprintMPwithOneMASC() + bonus;
        int sprintMPNoBoost = cachedEntityState.getSprintMPwithoutMASC() + bonus;

        // Have these been used already this turn. If so, they do not require a recheck agaisnt PSR
        // This can happen as a result of interrupted turns due to failed PSRs, pointblank shots, etc.
        final boolean hasMASCBeenUsed = entity.isMASCUsed();
        final boolean hasSuperchargerBeenUsed = entity.isSuperchargerUsed();

        final boolean hasPoorPerformance = entity
                .hasQuirk(OptionsConstants.QUIRK_NEG_POOR_PERFORMANCE);

        // WiGEs, AirMechs, and glider ProtoMechs have different MP for ground and airborne movement
        if (entity.getMovementMode() == EntityMovementMode.WIGE) {
            if (getClearance() <= 0 && type != MoveStepType.UP) {
                if (entity instanceof LandAirMech) {
                    // On the ground or underwater use AirMech walk/run.
                    // Sprint can only be used on the ground, so that is already set.
                    tmpWalkMP = ((LandAirMech) entity).getAirMechWalkMP();
                    runMPNoBoost = ((LandAirMech) entity).getAirMechRunMP();
                    // LAMs cannot use hardened armor, which makes runMP a simpler calculation.
                    MPBoosters mpBoosters = ((LandAirMech) entity).getArmedMPBoosters();
                    if (!mpBoosters.isNone()) {
                        runMPMax = mpBoosters.calculateRunMP(tmpWalkMP);
                    } else {
                        runMPMax = runMPNoBoost;
                    }
                } else {
                    // Only 1 ground MP for ground effect vehicles and glider ProtoMeks
                    tmpWalkMP = runMPMax = runMPNoBoost = sprintMPMax = sprintMPNoBoost = 1;
                }
            } else if (entity instanceof LandAirMech) {
                // LAMs cannot use overdrive and MASC does not affect airborne MP.
                tmpWalkMP = ((LandAirMech) entity).getAirMechCruiseMP();
                runMPMax = runMPNoBoost = sprintMPMax = sprintMPNoBoost = ((LandAirMech) entity).getAirMechFlankMP();
            }
        }

        Hex currHex = game.getBoard().getHex(curPos);
        Hex lastHex = game.getBoard().getHex(lastPos);

        // Bootlegger ends movement
        if (prev.type == MoveStepType.BOOTLEGGER) {
            movementType = EntityMovementType.MOVE_ILLEGAL;
            return;
        }

        if (stepType == MoveStepType.CONVERT_MODE) {
            // QuadVees and LAMs cannot convert in water, and Mech tracks cannot be used in water.
            if (currHex.containsTerrain(Terrains.WATER)
                    && getClearance() < 0) {
                movementType = EntityMovementType.MOVE_ILLEGAL;
            }
            // QuadVees and LAMs cannot convert while prone. Mechs with tracks don't actually convert,
            // and can switch to track mode while prone then stand.
            if (getEntity().isProne()
                    && (getEntity() instanceof QuadVee || getEntity() instanceof LandAirMech)) {
                movementType = EntityMovementType.MOVE_ILLEGAL;
            }
            // Illegal LAM conversions due to damage have to be determined by entire path, because
            // some conversions take two convert steps and can be legal even though the first one
            // is illegal on its own.
        }

        if (isVTOLBombingStep()) {
            if (!getEntity().isBomber() || getClearance() <= 0) {
                movementType = EntityMovementType.MOVE_ILLEGAL;
            } else if (isFirstStep()) {
                movementType = EntityMovementType.MOVE_NONE;
            } else {
                movementType = prev.getMovementType(false);
            }
        }

        if ((getEntity().getMovementMode() == EntityMovementMode.INF_UMU)
                && (currHex.containsTerrain(Terrains.WATER)
                && lastHex.containsTerrain(Terrains.WATER) && (entity
                .relHeight() < currHex.getLevel()))) {
            tmpWalkMP = entity.getActiveUMUCount();
        }

        if ((getEntity().getMovementMode() == EntityMovementMode.BIPED_SWIM)
                || (getEntity().getMovementMode() == EntityMovementMode.QUAD_SWIM)
                || ((getEntity() instanceof Infantry
                        && getEntity().getMovementMode() == EntityMovementMode.SUBMARINE))) {
            tmpWalkMP = entity.getActiveUMUCount();
        }

        if ((getEntity().getMovementMode() == EntityMovementMode.VTOL)
                && getClearance() > 0
                && !(getEntity() instanceof VTOL)) {
            tmpWalkMP = entity.getJumpMP();
        }

        // check for valid walk/run mp; BRACE is a special case for protomechs
        if (!isJumping() && !entity.isStuck() && (tmpWalkMP > 0)
                && ((getMp() > 0) || (stepType == MoveStepType.BRACE))) {
            // Prone mechs can only spend MP to turn or get up
            if ((stepType != MoveStepType.TURN_LEFT)
                    && (stepType != MoveStepType.TURN_RIGHT)
                    && (stepType != MoveStepType.GET_UP)
                    && (stepType != MoveStepType.LOAD)
                    && (stepType != MoveStepType.CAREFUL_STAND)
                    && (stepType != MoveStepType.HULL_DOWN)
                    && (stepType != MoveStepType.GO_PRONE)
                    && !(entity instanceof Tank) // Tanks can drive out of
                    // hull-down
                    && (isProne() || isHullDown())) {
                movementType = EntityMovementType.MOVE_ILLEGAL;
                return;
            }

            // WiGEs that land are finished with movement
            if (entity.getMovementMode() == EntityMovementMode.WIGE
                    && prev.getType() == MoveStepType.DOWN
                    && getClearance() == 0) {
                movementType = EntityMovementType.MOVE_ILLEGAL;
                return;
            }

            if (getMpUsed() <= tmpWalkMP) {
                if ((getEntity().getMovementMode() == EntityMovementMode.VTOL
                        || getEntity().getMovementMode() == EntityMovementMode.WIGE)
                        && getClearance() > 0) {
                    movementType = EntityMovementType.MOVE_VTOL_WALK;
                } else {
                    movementType = EntityMovementType.MOVE_WALK;
                    // Vehicles moving along pavement get "road bonus" of 1 MP.
                    // N.B. The Ask Precentor Martial forum said that a 4/6
                    // tank on a road can move 5/7, **not** 5/8.
                }
            } else if ((entity instanceof Infantry)
                    && (curPos.distance(entity.getPosition()) == 1)
                    && (lastPos.equals(entity.getPosition()))) {
                // This ensures that Infantry always get their minimum 1 hex
                //  movement when TO fast infantry movement is on.
                // A movepath that consists of a single step from one hex to the
                // next should always be a walk, since it's covered under the
                // infantry's 1 free movement
                if ((getEntity().getMovementMode() == EntityMovementMode.VTOL)
                        && getClearance() > 0) {
                    movementType = EntityMovementType.MOVE_VTOL_WALK;
                } else {
                    movementType = EntityMovementType.MOVE_WALK;
                }
            } else if (hasPoorPerformance
                        && (entity.getMpUsedLastRound() < cachedEntityState.getWalkMP())) {
                // Poor performance requires spending all walk MP in the
                // previous round in order to move faster than a walk
                movementType = EntityMovementType.MOVE_ILLEGAL;
                return;
            } else if (getMpUsed() <= runMPMax && !isRunProhibited()) {
                // RUN - If we got this far, entity is moving farther than a walk
                // but within run and running is legal

                if ( getMpUsed() > runMPNoBoost ) {
                    // must be using MP booster to go this fast
                    if (isEvading) {
                        // MP Boosters like MASC and Supercharger cannot be use when evading
                        movementType = EntityMovementType.MOVE_ILLEGAL;
                        return;
                    } else if ((getMpUsed() <= runMPSingleBoost)) {
                        UseEitherMASCOrSupercharger(hasMASCBeenUsed, hasSuperchargerBeenUsed);
                    } else if ((getMpUsed() <= runMPMax)) {
                        UseBothMASCAndSupercharger(hasMASCBeenUsed, hasSuperchargerBeenUsed);
                    }
                }

                if ((entity.getMovementMode() == EntityMovementMode.VTOL
                        || entity.getMovementMode() == EntityMovementMode.WIGE)
                        && getClearance() > 0) {
                    movementType = EntityMovementType.MOVE_VTOL_RUN;
                } else {
                    movementType = EntityMovementType.MOVE_RUN;
                }
            } else if ((getMpUsed() <= sprintMPMax) && !isRunProhibited() && !isEvading() && canUseSprint(game)) {
                // SPRINT - If we got this far, entity is moving farther than a run
                // but within sprint and sprinting must be legal and the option enabled

                if (getMpUsed() > sprintMPNoBoost) {
                    // must be using MP booster to go this fast
                    if (getMpUsed() <= sprintMPSingleBoost) {
                        UseEitherMASCOrSupercharger(hasMASCBeenUsed, hasSuperchargerBeenUsed);
                    } else {
                        UseBothMASCAndSupercharger(hasMASCBeenUsed, hasSuperchargerBeenUsed);
                    }
                }

                if (entity.getMovementMode() == EntityMovementMode.VTOL
                        || (entity.getMovementMode() == EntityMovementMode.WIGE
                        && getClearance() > 0)) {
                    movementType = EntityMovementType.MOVE_VTOL_SPRINT;
                } else {
                    movementType = EntityMovementType.MOVE_SPRINT;
                }
            }
        }

        // If using vehicle acceleration restrictions, it is impossible to go from a stop to overdrive.
        // Stop to flank or cruise to overdrive is permitted with a driving check ("gunning it").
        if (game.getOptions().booleanOption(OptionsConstants.ADVGRNDMOV_VEHICLE_ACCELERATION)
                && movementType == EntityMovementType.MOVE_SPRINT
                && (entity instanceof Tank
                        || (entity instanceof QuadVee && entity.getConversionMode() == QuadVee.CONV_MODE_VEHICLE))
                && (entity.movedLastRound == EntityMovementType.MOVE_NONE
                    || entity.movedLastRound == EntityMovementType.MOVE_SKID
                    || entity.movedLastRound == EntityMovementType.MOVE_JUMP)) {
            movementType = EntityMovementType.MOVE_ILLEGAL;
        }
        // 0 MP infantry units can move 1 hex
        if (isInfantry
                && (cachedEntityState.getWalkMP() == 0)
                && getEntity().getPosition().equals(prev.getPosition())
                && (prev.getElevation() == entity.getElevation())
                && (getEntity().getPosition().distance(getPosition()) <= 1)
                && (Math.abs(entity.getElevation() - getElevation())
                        <= entity.getMaxElevationChange())
                && (movementType != EntityMovementType.MOVE_JUMP)) {
            movementType = EntityMovementType.MOVE_WALK;
        }

        // Free facing changes are legal
        if (((stepType == MoveStepType.TURN_LEFT) || (stepType == MoveStepType.TURN_RIGHT))
                && (getMp() == 0)) {
            movementType = prev.movementType;
        }

        // Mechanical Jump Boosters don't allow facing changes
        if (isJumping()
                && (entity.getJumpType() == Mech.JUMP_BOOSTER)
                && ((stepType == MoveStepType.TURN_LEFT) || (stepType == MoveStepType.TURN_RIGHT))) {
            movementType = EntityMovementType.MOVE_ILLEGAL;
        }

        // going prone from hull down is legal and costs 0
        if ((getMp() == 0) && (stepType == MoveStepType.GO_PRONE)
                && isHullDown()) {
            movementType = prev.movementType;
        }

        if ((movementType == EntityMovementType.MOVE_WALK)
                && (prev.movementType == EntityMovementType.MOVE_RUN)) {
            movementType = EntityMovementType.MOVE_RUN;
        } else if ((movementType == EntityMovementType.MOVE_VTOL_WALK)
                && (prev.movementType == EntityMovementType.MOVE_VTOL_RUN)) {
            movementType = EntityMovementType.MOVE_VTOL_RUN;
        } else if (((movementType == EntityMovementType.MOVE_WALK) || (movementType == EntityMovementType.MOVE_RUN))
                && (prev.movementType == EntityMovementType.MOVE_SPRINT)) {
            movementType = EntityMovementType.MOVE_SPRINT;
        } else if (((movementType == EntityMovementType.MOVE_VTOL_WALK)
                || (movementType == EntityMovementType.MOVE_VTOL_RUN))
                && (prev.movementType == EntityMovementType.MOVE_VTOL_SPRINT)) {
            movementType = EntityMovementType.MOVE_VTOL_SPRINT;
        }

        if (entity.isGyroDestroyed() && !((entity instanceof LandAirMech)
                && (entity.getConversionMode() == LandAirMech.CONV_MODE_FIGHTER))) {
            //A prone 'Mech with a destroyed gyro can only change a single hex side, or eject
            if (entity.isProne()) {
                if (((stepType != MoveStepType.TURN_LEFT && stepType != MoveStepType.TURN_RIGHT)
                        || getMpUsed() > 1) && stepType != MoveStepType.EJECT) {
                    movementType = EntityMovementType.MOVE_ILLEGAL;
                }
            } else {
                //Normally a 'Mech falls immediately when the gyro is destroyed and can't stand again.
                //QuadVees using vehicle mode and 'Mechs using tracks do not fall and can continue to
                //stand, but cannot use non-tracked/wheeled MP except for a QuadVee converting back to
                //vehicle mode. This also covers a 'Mech that started with a destroyed gyro but was not
                //set to deploy prone. Perhaps that should not be allowed.
                if (getMp() > 0) {
                    boolean isTracked = entity.getMovementMode() == EntityMovementMode.TRACKED
                            || entity.getMovementMode() == EntityMovementMode.WHEELED;
                    if (entity instanceof QuadVee) {
                        //We are in 'Mech/non-tracked mode if the end mode is vee and we are converting
                        //of the end mode is 'Mech and we are not converting.
                        if (isTracked == entity.isConvertingNow() && stepType != MoveStepType.CONVERT_MODE) {
                            movementType = EntityMovementType.MOVE_ILLEGAL;
                        }
                    } else if (!isTracked) {
                        //Non QuadVee tracked 'Mechs don't actually convert. They just go, so we only need to
                        //know the end mode.
                        movementType = EntityMovementType.MOVE_ILLEGAL;
                    }
                }
            }
        }

        // Mechs with no arms and a missing leg cannot attempt to stand
        if (((stepType == MoveStepType.GET_UP) ||
                (stepType == MoveStepType.CAREFUL_STAND)) &&
                (entity instanceof Mech) &&
                entity.isLocationBad(Mech.LOC_LARM) &&
                entity.isLocationBad(Mech.LOC_RARM) &&
                (entity.isLocationBad(Mech.LOC_RLEG) ||
                        entity.isLocationBad(Mech.LOC_LLEG))) {
            movementType = EntityMovementType.MOVE_ILLEGAL;
            return;
        }

        // Mechs with 1 MP are allowed to get up, except
        // if they've used that 1MP up already
        if ((MoveStepType.GET_UP == stepType) && (1 == cachedEntityState.getRunMP())
                && (entity.mpUsed < 1) && !entity.isStuck()) {
            movementType = EntityMovementType.MOVE_RUN;
        }

        if ((MoveStepType.CAREFUL_STAND == stepType) && (entity.mpUsed > 1)) {
            movementType = EntityMovementType.MOVE_ILLEGAL;
        }

        if (isFirstStep()
                && ((stepType == MoveStepType.TAKEOFF) || (stepType == MoveStepType.VTAKEOFF))) {
            movementType = EntityMovementType.MOVE_SAFE_THRUST;
        } else

            // VTOLs with a damaged flight stabiliser can't flank
            if ((entity instanceof VTOL)
                    && (movementType == EntityMovementType.MOVE_VTOL_RUN
                        || movementType == EntityMovementType.MOVE_VTOL_SPRINT)
                    && ((VTOL) entity).isStabiliserHit(VTOL.LOC_ROTOR)) {
                movementType = EntityMovementType.MOVE_ILLEGAL;
            }

        // check for UMU infantry on land
        if ((entity.getMovementMode() == EntityMovementMode.INF_UMU)
                && !game.getBoard().getHex(curPos)
                .containsTerrain(Terrains.WATER)
                && (movementType == EntityMovementType.MOVE_RUN)) {
            movementType = EntityMovementType.MOVE_ILLEGAL;
        }

        // amnesty for the first step
        if (isFirstStep() && (movementType == EntityMovementType.MOVE_ILLEGAL)
                && (cachedEntityState.getWalkMP() > 0) && !entity.isProne()
                && !entity.isHullDown() && !entity.isStuck()
                && !entity.isGyroDestroyed() && (stepType == MoveStepType.FORWARDS)) {
            movementType = EntityMovementType.MOVE_RUN;
        }

        // Bimodal LAMs cannot spend MP when converting to fighter mode on the ground.
        if (entity instanceof LandAirMech
                && ((LandAirMech) entity).getLAMType() == LandAirMech.LAM_BIMODAL
                && entity.getConversionMode() == LandAirMech.CONV_MODE_MECH
                && movementMode == EntityMovementMode.AERODYNE
                && altitude == 0
                && mp > 0) {
            movementType = EntityMovementType.MOVE_ILLEGAL;
        }

        // Is the entity unloading passengers?
        if (stepType == MoveStepType.UNLOAD) {

            if (entity instanceof Aero) {
                movementType = EntityMovementType.MOVE_NONE;
            } else {

                if (isFirstStep()) {
                    if (getMpUsed() <= cachedEntityState.getRunMP()) {
                        movementType = EntityMovementType.MOVE_RUN;
                        if (getMpUsed() <= cachedEntityState.getWalkMP()) {
                            movementType = EntityMovementType.MOVE_WALK;
                        }
                    }
                } else {
                    movementType = prev.getMovementType(false);
                }

                // Prone Meks are able to unload, if they have the MP.
                if ((getMpUsed() <= cachedEntityState.getRunMP())
                        && (entity.isProne() || entity.isHullDown())
                        && (movementType == EntityMovementType.MOVE_ILLEGAL)) {
                    movementType = EntityMovementType.MOVE_RUN;
                    if (getMpUsed() <= cachedEntityState.getWalkMP()) {
                        movementType = EntityMovementType.MOVE_WALK;
                    }
                }

                // Can't unload units into prohibited terrain
                // or into stacking violation.
                Targetable target = getTarget(game);
                if (target instanceof Entity) {
                    //Change the destination hex if an unload dialog box set it elsewhere
                    if (getTargetPosition() != null) {
                        curPos = getTargetPosition();
                    }
                    Entity other = (Entity) target;
                    if ((null != Compute.stackingViolation(game, other, curPos,
                            entity, climbMode)) || other.isLocationProhibited(curPos, getElevation())) {
                        movementType = EntityMovementType.MOVE_ILLEGAL;
                    }
                } else {
                    movementType = EntityMovementType.MOVE_ILLEGAL;
                }
            }
        }

        // Is the entity trying to drop a trailer?
        if (stepType == MoveStepType.DISCONNECT) {

            // If this isn't the first step, trailer position isn't updated by Server.processTrailerMovement()
            // before this step, so they don't drop off in the right place
            if (!isFirstStep()) {
                movementType = EntityMovementType.MOVE_ILLEGAL;
            } else {
                movementType = EntityMovementType.MOVE_WALK;
            }

            // Can't unload units into prohibited terrain
            // or into stacking violation.
            Targetable target = getTarget(game);
            if (target instanceof Entity) {
                Entity other = (Entity) target;
                if ((null != Compute.stackingViolation(game, other, curPos,
                        entity, climbMode)) || other.isLocationProhibited(curPos, getElevation())) {
                    movementType = EntityMovementType.MOVE_ILLEGAL;
                }
            } else {
                movementType = EntityMovementType.MOVE_ILLEGAL;
            }

        }

        if (stepType == MoveStepType.SHAKE_OFF_SWARMERS) {
            if ((getMp() == 0) || !(entity instanceof Tank)) {
                // Can't shake off swarmers if you can't flank
                movementType = EntityMovementType.MOVE_ILLEGAL;
            } else {
                // And its always considered to be flank movement
                if (entity.getMovementMode() == EntityMovementMode.VTOL) {
                    movementType = EntityMovementType.MOVE_VTOL_RUN;
                } else {
                    movementType = EntityMovementType.MOVE_RUN;
                }
            }
        }

        // Can't run or jump if unjamming a RAC.
        if (isUnjammingRAC
                && ((movementType == EntityMovementType.MOVE_RUN)
                || (movementType == EntityMovementType.MOVE_SPRINT)
                || (movementType == EntityMovementType.MOVE_VTOL_RUN
                || (movementType == EntityMovementType.MOVE_VTOL_SPRINT))
                || isJumping())) {
            movementType = EntityMovementType.MOVE_ILLEGAL;
        }

        // only standing mechs may go prone
        if ((stepType == MoveStepType.GO_PRONE)
                && (isProne() || !(entity instanceof Mech) || entity.isStuck())) {
            movementType = EntityMovementType.MOVE_ILLEGAL;
        }

        // Standing mechs and vehicles in fortified terrain can hull-down
        if (stepType == MoveStepType.HULL_DOWN) {
            if ((isHullDown()
                    || !((entity instanceof Mech) || (entity instanceof Tank)) || entity
                    .isStuck())) {
                movementType = EntityMovementType.MOVE_ILLEGAL;
            }
            if (entity instanceof Tank
                    || (entity instanceof QuadVee
                            && ((entity.getConversionMode() == QuadVee.CONV_MODE_VEHICLE)
                                    != entity.isConvertingNow()))) {
                //Tanks and QuadVees ending movement in vehicle mode require a fortified hex.
                if (!(game.getBoard().getHex(curPos)
                        .containsTerrain(Terrains.FORTIFIED))) {
                    movementType = EntityMovementType.MOVE_ILLEGAL;
                }
            } else if (entity.isGyroDestroyed()) {
                // Mechs need to check for valid Gyros
                movementType = EntityMovementType.MOVE_ILLEGAL;
            }
        }

        // initially prone mechs can't charge
        if (((stepType == MoveStepType.CHARGE) || (stepType == MoveStepType.DFA))
                && entity.isProne()) {
            movementType = EntityMovementType.MOVE_ILLEGAL;
        }

        // do not allow to move onto a bridge if there's no exit in lastPos's
        // direction, unless jumping
        if (!isFirstStep()
                && !curPos.equals(lastPos)
                && climbMode
                && entity.getMovementMode() != EntityMovementMode.VTOL
                && (entity.getMovementMode() != EntityMovementMode.WIGE
                    || getClearance() == 0)
                && (movementType != EntityMovementType.MOVE_JUMP)
                && game.getBoard().getHex(curPos)
                .containsTerrain(Terrains.BRIDGE)
                && !game.getBoard()
                .getHex(curPos)
                .containsTerrainExit(Terrains.BRIDGE,
                        curPos.direction(lastPos))
                && (getElevation() + entity.getHeight()
                        >= game.getBoard().getHex(curPos).terrainLevel(Terrains.BRIDGE_ELEV))) {
            movementType = EntityMovementType.MOVE_ILLEGAL;
        }

        // super heavy mechs can't climb on buildings
        if ((entity instanceof Mech)
                && ((Mech) entity).isSuperHeavy()
                && climbMode
                && game.getBoard().getHex(curPos)
                .containsTerrain(Terrains.BUILDING)) {
            movementType = EntityMovementType.MOVE_ILLEGAL;
        }

        // TO p.325 - Mine dispensers
        if ((type == MoveStepType.LAY_MINE) && !entity.canLayMine()) {
            movementType = EntityMovementType.MOVE_ILLEGAL;
            return;
        }

        if ((type == MoveStepType.LAY_MINE) && entity.canLayMine()) {
            //All units may only lay mines on its first or last step.
            //BA additionally have to use Jump or VTOL movement.
            movementType = prev.movementType;

            if (entity instanceof BattleArmor &&
                    !(isFirstStep()
                            || (prev.movementType == EntityMovementType.MOVE_JUMP)
                            || (prev.movementType == EntityMovementType.MOVE_VTOL_RUN)
                            || (prev.movementType == EntityMovementType.MOVE_VTOL_WALK))) {
                movementType = EntityMovementType.MOVE_ILLEGAL;
            }
        }
        if (prev.type == MoveStepType.LAY_MINE && !prev.isFirstStep()) {
            movementType = EntityMovementType.MOVE_ILLEGAL;
            return;
        }

        // can't brace when jumping, prone, wrong unit type or no eligible locations
        if ((stepType == MoveStepType.BRACE) && (this.isJumping() || !entity.canBrace())) {
            movementType = EntityMovementType.MOVE_ILLEGAL;
            return;
        }


        if (stepType == MoveStepType.MOUNT) {
            movementType = EntityMovementType.MOVE_WALK;
        }

        if (stepType == MoveStepType.BOOTLEGGER) {
            // Bootlegger requires three hexes straight and is illegal for tracked, WiGE, or naval.
            if (prev.nStraight < 3
                    || (entity.getMovementMode() != EntityMovementMode.WHEELED
                    && entity.getMovementMode() != EntityMovementMode.HOVER
                    && entity.getMovementMode() != EntityMovementMode.VTOL)) {
                movementType = EntityMovementType.MOVE_ILLEGAL;
            } else {
                danger = true;
            }
        }

        // check if this movement is illegal for reasons other than points
        if (!isMovementPossible(game, lastPos, prev.getElevation(), cachedEntityState)
                || (isUnloaded && type != MoveStepType.CHAFF)) {
            movementType = EntityMovementType.MOVE_ILLEGAL;
        }

        // If the previous step is always illegal, then so is this one
        if (EntityMovementType.MOVE_ILLEGAL == prev.movementType) {
            movementType = EntityMovementType.MOVE_ILLEGAL;
        }

        // Don't compute danger if the step is illegal.
        if (movementType == EntityMovementType.MOVE_ILLEGAL) {
            return;
        }

        // Danger is flagged for PSR checks by entire path when a new step is added, since turning
        // while running on pavement does cannot trigger the danger flag if the turn occurs before
        // enough MP are spent to require running.

        // getting up is also danger
        if (stepType == MoveStepType.GET_UP) {
            danger = true;
        }

        // set past danger
        pastDanger |= danger;

        // Record if we're turning *after* check for danger,
        // because the danger lies in moving *after* turn.
        switch (stepType) {
            case TURN_LEFT:
            case TURN_RIGHT:
                setTurning(true);
                break;
            case UNLOAD:
            case DISCONNECT:
                // Unloading must be the last step.
                setUnloaded(true);
                break;
            default:
                setTurning(false);
                break;
        }

        // update prone state
        if (stepType == MoveStepType.GO_PRONE) {
            setProne(true);
            setHullDown(false);
        } else if (stepType == MoveStepType.GET_UP) {
            setProne(false);
            setHullDown(false);
        } else if (stepType == MoveStepType.HULL_DOWN) {
            setProne(false);
            setHullDown(true);
        }

        if (entity.isCarefulStand()) {
            movementType = EntityMovementType.MOVE_CAREFUL_STAND;
        }

        // only walking speed in Tornados
        if (game.getPlanetaryConditions().getWindStrength() == PlanetaryConditions.WI_TORNADO_F4) {
            if (getMpUsed() > tmpWalkMP) {
                movementType = EntityMovementType.MOVE_ILLEGAL;
                return;
            }
        }

        // Vehicles carrying mechanized BA can't jump, VTOL, or WiGE
        if ((entity instanceof Tank) && !entity.getExternalUnits().isEmpty()) {
            if ((movementType == EntityMovementType.MOVE_JUMP)
                    || (movementType == EntityMovementType.MOVE_VTOL_WALK)
                    || (movementType == EntityMovementType.MOVE_VTOL_RUN)
                    || (movementType == EntityMovementType.MOVE_VTOL_SPRINT)
                    || ((entity.getMovementMode() == EntityMovementMode.WIGE)
                        && getClearance() > 0)) {
                movementType = EntityMovementType.MOVE_ILLEGAL;
            }
        }
    }

    /**
     * If the entity has both, choose the one with the lower risk, or Supercharger if they are even
     * Require a PSR if it has not been done in an earlier part of the move
     */
    private void UseEitherMASCOrSupercharger(boolean hasMASCBeenUsed, boolean hasSuperchargerBeenUsed) {
        MPBoosters mpBoosters = entity.getArmedMPBoosters();
        if (mpBoosters.isJetBooster() && !hasMASCBeenUsed) {
            setUsingMASC(true);
        } else if (!hasMASCBeenUsed && !hasSuperchargerBeenUsed) {
            int scTarget = mpBoosters.hasSupercharger() ? entity.getSuperchargerTarget() : 2000;
            int mascTarget = mpBoosters.hasMASC() ? entity.getMASCTarget() : 2000;
            if (mascTarget < scTarget) {
                setUsingMASC(true);
                setTargetNumberMASC(entity.getMASCTarget());
            } else {
                setUsingSupercharger(true);
                setTargetNumberSupercharger(entity.getSuperchargerTarget());
            }
        }
    }

    /**
     * Require a PSR if it has not been done in an earlier part of the move
     */
    private void UseBothMASCAndSupercharger(boolean hasMASCBeenUsed, boolean hasSuperchargerBeenUsed) {
        if (!hasMASCBeenUsed) {
            setUsingMASC(true);
            setTargetNumberMASC(entity.getMASCTarget());
        }
        if (!hasSuperchargerBeenUsed) {
            setUsingSupercharger(true);
            setTargetNumberSupercharger(entity.getSuperchargerTarget());
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
    protected void calcMovementCostFor(Game game, MoveStep prevStep, CachedEntityState cachedEntityState) {
        final Coords prev = prevStep.getPosition();
        final int prevEl = prevStep.getElevation();
        final EntityMovementMode moveMode = getEntity()
                .getMovementMode();
        final Entity en = getEntity();
        final Hex srcHex = game.getBoard().getHex(prev);
        final Hex destHex = game.getBoard().getHex(getPosition());
        final boolean isInfantry = getEntity() instanceof Infantry;
        final boolean isSuperHeavyMech = (getEntity() instanceof Mech)
                && ((Mech) getEntity()).isSuperHeavy();
        final boolean isMechanizedInfantry = isInfantry
                && ((Infantry) getEntity()).isMechanized();
        final boolean isProto = getEntity() instanceof Protomech;
        final boolean isMech = getEntity() instanceof Mech;
        final boolean isAmphibious = cachedEntityState.hasWorkingMisc(MiscType.F_FULLY_AMPHIBIOUS) ||
                cachedEntityState.hasWorkingMisc(MiscType.F_LIMITED_AMPHIBIOUS);
        final boolean isFogSpecialist = en.getCrew().getOptions().stringOption(OptionsConstants.MISC_ENV_SPECIALIST).equals(Crew.ENVSPC_FOG);
        final boolean isLightSpecialist = en.getCrew().getOptions().stringOption(OptionsConstants.MISC_ENV_SPECIALIST).equals(Crew.ENVSPC_LIGHT);
        int nSrcEl = srcHex.getLevel() + prevEl;
        int nDestEl = destHex.getLevel() + elevation;

        mp = 1;


        // 0 MP infantry units can move 1 hex
        if (isInfantry
                && (cachedEntityState.getWalkMP() == 0)
                && (moveMode != EntityMovementMode.SUBMARINE)
                && getEntity().getPosition().equals(prev)
                && (getEntity().getPosition().distance(getPosition()) == 1)
                && (!isJumping())) {
            mp = 0;
            return;
        }


        boolean applyNightPen =
                !game.getOptions().booleanOption(OptionsConstants.ADVGRNDMOV_NO_NIGHT_MOVE_PEN);
        boolean carefulExempt =
                (moveMode == EntityMovementMode.VTOL) || isJumping();

        // Apply careful movement MP penalties for fog and light (TO pg 63)
        if (!game.getBoard().inSpace() && isCareful() && applyNightPen
                && !carefulExempt) {
            // Fog
            switch (game.getPlanetaryConditions().getFog()) {
                case PlanetaryConditions.FOG_LIGHT:
                    if (!isFogSpecialist) {
                        mp += 1;
                    }
                    break;
                case PlanetaryConditions.FOG_HEAVY:
                    if (!isFogSpecialist) {
                        mp += 2;
                    } else {
                        mp += 1;
                    }
                    break;
            }

            // Light
            if (!entity.isNightwalker()) {
                switch (game.getPlanetaryConditions().getLight()) {
                    case PlanetaryConditions.L_FULL_MOON:
                        if (!isLightSpecialist && !en.isUsingSearchlight()) {
                            mp += 1;
                        }
                        break;
                    case PlanetaryConditions.L_MOONLESS:
                        if (en.isUsingSearchlight()) {
                            break;
                        }

                        if (!isLightSpecialist) {
                            mp += 2;
                        } else {
                            mp += 1;
                        }
                        break;
                    case PlanetaryConditions.L_PITCH_BLACK:
                        if (!isLightSpecialist) {
                            mp += 3;
                        } else {
                            mp += 1;
                        }
                        break;
                }
            } else if (game.getPlanetaryConditions().getLight() > PlanetaryConditions.L_DUSK) {
                setRunProhibited(true);
            }
        }


        // VTOLs pay 1 for everything
        if (moveMode == EntityMovementMode.VTOL) {
            return;
        }

        // jumping always costs 1, unless fog or poor light
        if (isJumping()) {
            return;
        }

        // Account for terrain, unless we're moving along a road.
        if (!isPavementStep()) {

            if ((moveMode != EntityMovementMode.BIPED_SWIM)
                    && (moveMode != EntityMovementMode.QUAD_SWIM)
                    && getClearance() == 0) {
                mp += destHex.movementCost(getEntity());
            }

            // if this is an amphibious unit crossing water, increment movement cost by 1
            if (isAmphibious && !destHex.containsTerrain(Terrains.ICE) && (destHex.terrainLevel(Terrains.WATER) > 0)) {
                mp++;

                // this is kind of a hack, but only occurs when an amphibious unit passes over mud at the bottom
                // of a body of water. We can't account for that in the hex's movement cost function
                // because it doesn't have the ability to pretend the entity is at a particular elevation
                if (destHex.containsTerrain(Terrains.MUD) && (destHex.floor() < nDestEl)) {
                    mp--;
                }
            }

            // non-hovers, non-navals and non-VTOLs check for water depth and
            // are affected by swamp
            if ((moveMode != EntityMovementMode.HOVER)
                    && (moveMode != EntityMovementMode.NAVAL)
                    && (moveMode != EntityMovementMode.HYDROFOIL)
                    && (moveMode != EntityMovementMode.SUBMARINE)
                    && (moveMode != EntityMovementMode.INF_UMU)
                    && (moveMode != EntityMovementMode.VTOL)
                    && (moveMode != EntityMovementMode.BIPED_SWIM)
                    && (moveMode != EntityMovementMode.QUAD_SWIM)
                    && (moveMode != EntityMovementMode.WIGE)) {
                // no additional cost when moving on surface of ice.
                if (!destHex.containsTerrain(Terrains.ICE)
                        || (nDestEl < destHex.getLevel())) {
                    if ((destHex.terrainLevel(Terrains.WATER) == 1) && !isAmphibious) {
                        mp++;
                    } else if ((destHex.terrainLevel(Terrains.WATER) > 1) && !isAmphibious) {
                        if (getEntity().hasAbility(OptionsConstants.PILOT_TM_FROGMAN)
                                && ((entity instanceof Mech) || (entity instanceof Protomech))) {
                            mp += 2;
                        } else {
                            mp += 3;
                        }
                    }
                }
                // if using non-careful movement on ice then reduce cost
                if (destHex.containsTerrain(Terrains.ICE)
                        && !isCareful()
                        && (nDestEl == destHex.getLevel())) {
                    mp--;
                }

            }
        } // End not-along-road

        // non-WIGEs pay for elevation differences
        if ((nSrcEl != nDestEl) && (moveMode != EntityMovementMode.WIGE)) {
            int delta_e = Math.abs(nSrcEl - nDestEl);
            if (game.getOptions().booleanOption(OptionsConstants.ADVGRNDMOV_TACOPS_LEAPING) && isMech
                    && (delta_e > 2) && (nDestEl < nSrcEl)) {
                // leaping (moving down more than 2 hexes) always costs 4 mp
                // regardless of anything else
                mp = 4;
                return;
            }
            // non-flying Infantry and ground vehicles are charged double.
            if ((isInfantry
                    && !((getMovementType(false) == EntityMovementType.MOVE_VTOL_WALK)
                            || (getMovementType(false) == EntityMovementType.MOVE_VTOL_RUN)))
                    || ((moveMode == EntityMovementMode.TRACKED)
                            || (moveMode == EntityMovementMode.WHEELED)
                            || (moveMode == EntityMovementMode.HOVER))) {
                delta_e *= 2;
            }
            if (entity.hasAbility(OptionsConstants.PILOT_TM_MOUNTAINEER)) {
                mp += delta_e - 1;
            } else {
                mp += delta_e;
            }
        }

        // WiGEs in climb mode pay 2 extra MP to stay at the same flight level
        // if more than one elevation above the underlying terrain.
        // If the destination contains a building, the WiGE must pay the extra MP if flying
        // more than one elevation above its top or if climbing a level to get above it.
        // See http://bg.battletech.com/forums/index.php?topic=51081.msg1297747#msg1297747
        if (entity.getMovementMode() == EntityMovementMode.WIGE && distance > 0 && (getClearance() > 1
                || (destHex.containsTerrain(Terrains.BLDG_ELEV)
                        && destHex.ceiling() > srcHex.ceiling()))) {
            mp += 2;
        }

        // WIGEs spend one extra MP to ascend a sheer cliff, TO p.39
        if (entity.getMovementMode() == EntityMovementMode.WIGE
                && distance > 0
                && destHex.hasCliffTopTowards(srcHex)
                && nDestEl > nSrcEl) {
            mp += 1;
        }

        // If we entering a building, all non-infantry pay additional MP.
        if (nDestEl < destHex.terrainLevel(Terrains.BLDG_ELEV)) {
            Building bldg = game.getBoard().getBuildingAt(getPosition());
            // check for inside hangar movement
            if ((null != prev)
                    && (null != bldg)
                    && bldg.isIn(prev)
                    && (bldg.getBldgClass() == Building.HANGAR)
                    && (destHex.terrainLevel(Terrains.BLDG_ELEV) > getEntity()
                            .height())) {
                mp += 0;
            } else if (!isInfantry && !isSuperHeavyMech) {
                if (!isProto) {
                    // non-protos pay extra according to the building type
                    mp += bldg.getType();
                    if (bldg.getBldgClass() == Building.HANGAR) {
                        mp--;
                    }
                    if (bldg.getBldgClass() == Building.FORTRESS) {
                        mp++;
                    }
                } else {
                    // protos pay one extra
                    mp += 1;
                }
            } else if (isMechanizedInfantry) {
                // mechanized infantry pays 1 extra
                mp += 1;
            }
        }

        // Infantry (except mechanized) pay 1 less MP to enter woods
        if (isInfantry && !isMechanizedInfantry
                && destHex.containsTerrain(Terrains.WOODS)
                && !isPavementStep) {
            mp--;

            // Ensures that Infantry always pay at least 1 mp when
            // entering woods or jungle
            if (mp <= 0) {
                mp = 1;
            }
        }
    }

    /**
     * Is movement possible from a previous position to this one?
     * <p>
     * This function does not comment on whether an overall movement path is
     * possible, just whether the <em>current</em> step is possible.
     */
    public boolean isMovementPossible(Game game, Coords src, int srcEl, CachedEntityState cachedEntityState) {
        final Hex srcHex = game.getBoard().getHex(src);
        final Coords dest = getPosition();
        final Hex destHex = game.getBoard().getHex(dest);
        final Entity entity = getEntity();

        if (null == dest) {
            var ex = new IllegalStateException("Step has no position");
            LogManager.getLogger().error("", ex);
            throw ex;
        } else if (src.distance(dest) > 1) {
            var ex = new IllegalArgumentException("Coordinates " + src + " and " + dest + " are not adjacent.");
            LogManager.getLogger().error("", ex);
            throw ex;
        }

        // Assault dropping units cannot move
        if ((entity.isAssaultDropInProgress() || entity.isDropping())
                && !((entity instanceof LandAirMech)
                && (entity.getMovementMode() == EntityMovementMode.WIGE)
                && (entity.getAltitude() <= 3))) {
            return false;
        }

        // If we're a tank and immobile, check if we try to unjam
        // or eject and the crew is not unconscious
        if ((entity instanceof Tank)
                && !entity.getCrew().isUnconscious()
                && ((type == MoveStepType.UNJAM_RAC)
                || (type == MoveStepType.EJECT) || (type == MoveStepType.SEARCHLIGHT))) {
            return true;
        }

        // We're wanting to startup our reactor and we're not unconscious
        if ((type == MoveStepType.STARTUP) && !entity.getCrew().isUnconscious()) {
            return true;
        }

        // We're wanting to self destruct our reactor and we're not unconscious
        if ((type == MoveStepType.SELF_DESTRUCT)
                && !entity.getCrew().isUnconscious()) {
            return true;
        }

        // If you want to flee, and you can flee, flee.
        if ((type == MoveStepType.FLEE) && entity.canFlee()) {
            return true;
        }

        // super-easy, but not any more
        if (entity.isImmobile() && !entity.isBracing()) {
            return false;
        }

        // Hidden units, and activating hidden units cannot move
        // unless it is the movement phase and the plan is to activate then
        // if we're in this method, we're implicitly in the movement phase
        if (entity.isHidden()
                || (!entity.getHiddenActivationPhase().isUnknown() && !entity.getHiddenActivationPhase().isMovement())) {
            return false;
        }

        // another easy check
        if (!game.getBoard().contains(dest)) {
            return false;
        }

        // can't enter impassable hex
        if (destHex.containsTerrain(Terrains.IMPASSABLE)) {
            return false;
        }

        final int srcAlt = srcEl + srcHex.getLevel();
        final int destAlt = elevation + destHex.getLevel();

        Building bld = game.getBoard().getBuildingAt(dest);

        if (bld != null) {
            // ProtoMechs that are jumping can't change the level inside a building,
            // they can only jump onto a building or out of it
            if (src.equals(dest) && (srcAlt != destAlt)
                    && (entity instanceof Protomech)
                    && (getMovementType(false) == EntityMovementType.MOVE_JUMP)) {
                return false;
            }
            Hex hex = game.getBoard().getHex(getPosition());
            int maxElevation = (2 + entity.getElevation() + game.getBoard()
                    .getHex(entity.getPosition()).getLevel()) - hex.getLevel();

            if ((bld.getType() == Building.WALL)
                    && (maxElevation < hex.terrainLevel(Terrains.BLDG_ELEV))) {
                return false;
            }

            // only infantry can enter an armored building
            if ((elevation < hex.terrainLevel(Terrains.BLDG_ELEV))
                    && (bld.getArmor(dest) > 0)
                    && !(entity instanceof Infantry)) {
                return false;
            }

            // only infantry can enter a gun emplacement
            if ((elevation < hex.terrainLevel(Terrains.BLDG_ELEV))
                    && (bld.getBldgClass() == Building.GUN_EMPLACEMENT)
                    && !(entity instanceof Infantry)) {
                return false;
            }
        }

        // Can't back up across an elevation change.
        if (!(entity instanceof VTOL)
                && isThisStepBackwards()
                && !(isJumping() && (entity.getJumpType() == Mech.JUMP_BOOSTER))
                && (((destAlt != srcAlt) && !game.getOptions().booleanOption(
                OptionsConstants.ADVGRNDMOV_TACOPS_WALK_BACKWARDS)) || (game.getOptions()
                .booleanOption(OptionsConstants.ADVGRNDMOV_TACOPS_WALK_BACKWARDS) && (Math
                .abs(destAlt - srcAlt) > 1)))) {
            return false;
        }

        // Swarming entities can't move.
        if (Entity.NONE != entity.getSwarmTargetId()) {
            return false;
        }

        if (type == MoveStepType.MOUNT) {
            return true;
        }

        // The entity is trying to load. Check for a valid move.
        if (type == MoveStepType.LOAD) {
            // Find the unit being loaded.
            Entity other = null;
            Iterator<Entity> entities = game.getEntities(src);
            while (entities.hasNext()) {

                // Is the other unit friendly and not the current entity?
                other = entities.next();
                if (!entity.getOwner().isEnemyOf(other.getOwner())
                        && !entity.equals(other)) {

                    // The moving unit should be able to load the other unit.
                    if (!entity.canLoad(other)) {
                        return false;
                    }

                    // The other unit should be able to have a turn.
                    if (!other.isLoadableThisTurn()) {
                        return false;
                    }

                    // We can stop looking.
                    break;
                }
                // Nope. Discard it.
                other = null;

            } // Check the next entity in this position.

            // We were supposed to find someone to load.
            if (other == null) {
                return false;
            }

        } // End STEP_LOAD-checks

        // The entity is trying to tow. Check for a valid move.
        if (type == MoveStepType.TOW) {

            // Find the unit being towed.
            Entity other = game.getEntity(entity.getTowing());

            // The moving unit should be able to tow the other unit.
            if (!entity.canTow(other.getId())) {
                return false;
            }
        } // End STEP_TOW-checks

        // mechs dumping ammo can't run
        boolean bDumping = false;
        for (Mounted mo : entity.getAmmo()) {
            if (mo.isDumping()) {
                bDumping = true;
                break;
            }
        }
        if (bDumping
                && ((movementType == EntityMovementType.MOVE_RUN)
                || (movementType == EntityMovementType.MOVE_SPRINT)
                || (movementType == EntityMovementType.MOVE_VTOL_RUN)
                || (movementType == EntityMovementType.MOVE_VTOL_SPRINT)
                || (movementType == EntityMovementType.MOVE_JUMP))) {
            return false;
        }

        // check elevation difference > max
        EntityMovementMode nMove = entity.getMovementMode();

        // Make sure that if it's a VTOL unit with the VTOL MP listed as jump
        // MP...
        // That it can't jump.
        if ((movementType == EntityMovementType.MOVE_JUMP)
                && (nMove == EntityMovementMode.VTOL)) {
            return false;
        }



        if ((movementType != EntityMovementType.MOVE_JUMP)
                && (nMove != EntityMovementMode.VTOL)) {
            int maxDown = entity.getMaxElevationDown(srcAlt);
            if (movementMode == EntityMovementMode.WIGE
                    && (srcEl == 0 || (srcHex.containsTerrain(Terrains.BLDG_ELEV)
                            && (srcHex.terrainLevel(Terrains.BLDG_ELEV) >= srcEl)))) {
                maxDown = entity.getMaxElevationChange();
            }
            if ((((srcAlt - destAlt) > 0) && ((srcAlt - destAlt) > maxDown))
                    || (((destAlt - srcAlt) > 0) && ((destAlt - srcAlt) > entity.getMaxElevationChange()))) {
                return false;
            }
        }

        // Sheer Cliffs, TO p.39
        // Roads over cliffs cancel the cliff effects for units that move on roads
        boolean vehicleAffectedByCliff = entity instanceof Tank
                && !entity.isAirborneVTOLorWIGE();
        boolean quadveeVehMode = entity instanceof QuadVee
                && entity.getConversionMode() == QuadVee.CONV_MODE_VEHICLE;
        int stepHeight = destAlt - srcAlt;
        // Cliffs should only exist towards 1 or 2 level drops, check just to make sure
        // Everything that does not have a 1 or 2 level drop shouldn't be handled as a cliff
        boolean isUpCliff = !src.equals(dest)
                && destHex.hasCliffTopTowards(srcHex)
                && (stepHeight == 1 || stepHeight == 2);
        boolean isDownCliff = !src.equals(dest)
                && srcHex.hasCliffTopTowards(destHex)
                && (stepHeight == -1 || stepHeight == -2);

        // For vehicles exc. VTOL, WIGE, upward Sheer Cliffs is forbidden
        // QuadVees in vehicle mode drive as vehicles, IO p.133
        if ((vehicleAffectedByCliff || quadveeVehMode)
                && isUpCliff
                && !isPavementStep) {
            return false;
        }

        // For Infantry, up or down sheer cliffs requires a climbing action
        // except for Mountain Troops across a level 1 cliff.
        // Climbing actions do not seem to be implemented, so Infantry cannot
        // cross sheer cliffs at all except for Mountain Troops across a level 1 cliff.
        if (entity instanceof Infantry
                && (isUpCliff || isDownCliff)
                && !isPavementStep) {

            boolean isMountainTroop = ((Infantry) entity).hasSpecialization(Infantry.MOUNTAIN_TROOPS);
            if (!isMountainTroop || stepHeight == 2) {
                return false;
            }
        }

        if ((entity instanceof Mech) && ((srcAlt - destAlt) > 2)) {
            setLeapDistance(srcAlt - destAlt);
        }

        // Units moving backwards may not change elevation levels.
        if (((type == MoveStepType.BACKWARDS)
                || (type == MoveStepType.LATERAL_LEFT_BACKWARDS) || (type == MoveStepType.LATERAL_RIGHT_BACKWARDS))
                && (destAlt != srcAlt)
                && !(entity instanceof VTOL)
                && !(isJumping() && (entity.getJumpType() == Mech.JUMP_BOOSTER))) {
            // Generally forbidden without TacOps Expanded Backward Movement p.22
            if (!game.getOptions().booleanOption(OptionsConstants.ADVGRNDMOV_TACOPS_WALK_BACKWARDS)) {
                return false;
            }
            // Even with Expanded Backward Movement, ...
            // May not move across a cliff (up) moving backwards at all
            if (destHex.containsTerrain(Terrains.CLIFF_TOP)
                    && destHex.getTerrain(Terrains.CLIFF_TOP).hasExitsSpecified()
                    && ((destHex.getTerrain(Terrains.CLIFF_TOP).getExits() & (1 << dest.direction(src))) != 0)
                    && (!src.equals(dest))) {
                return false;
            }
            // May not move across a cliff (down) moving backwards at all
            if (srcHex.containsTerrain(Terrains.CLIFF_TOP)
                    && srcHex.getTerrain(Terrains.CLIFF_TOP).hasExitsSpecified()
                    && ((srcHex.getTerrain(Terrains.CLIFF_TOP).getExits() & (1 << src.direction(dest))) != 0)
                    && (!src.equals(dest))) {
                return false;
            }
            // May not move across more than 1 level
            if (Math.abs(destAlt - srcAlt) > 1) {
                return false;
            }
        }

        // WiGEs can't move backwards
        if ((type == MoveStepType.BACKWARDS)
                && (nMove == EntityMovementMode.WIGE)) {
            return false;
        }

        // Can't run into water unless hovering, naval, first step, using a
        // bridge, or fly.
        if (((movementType == EntityMovementType.MOVE_RUN)
                || (movementType == EntityMovementType.MOVE_SPRINT)
                || (movementType == EntityMovementType.MOVE_VTOL_RUN)
                || (movementType == EntityMovementType.MOVE_VTOL_SPRINT))
                && (nMove != EntityMovementMode.HOVER)
                && (nMove != EntityMovementMode.NAVAL)
                && (nMove != EntityMovementMode.HYDROFOIL)
                && (nMove != EntityMovementMode.SUBMARINE)
                && (nMove != EntityMovementMode.INF_UMU)
                && (nMove != EntityMovementMode.VTOL)
                && (nMove != EntityMovementMode.WIGE)
                && !cachedEntityState.hasWorkingMisc(MiscType.F_FULLY_AMPHIBIOUS)
                && (destHex.terrainLevel(Terrains.WATER) > 0)
                && !(destHex.containsTerrain(Terrains.ICE) && (elevation >= 0))
                && !dest.equals(entity.getPosition())
                && !isFirstStep()
                && !isPavementStep()) {
            return false;
        }

        // ugh, stacking checks. well, maybe we're immune!
        if (!isJumping() && (type != MoveStepType.CHARGE)
                && (type != MoveStepType.DFA)) {
            // can't move a mech into a hex with an enemy mech
            if ((entity instanceof Mech)
                    && Compute.isEnemyIn(game, entity, dest, true, true,
                    getElevation())) {
                return false;
            }

            // Can't move out of a hex with an enemy unit unless we started
            // there, BUT we're allowed to turn, unload/Disconnect, or go prone.
            if (Compute.isEnemyIn(game, entity, src, false,
                    entity instanceof Mech, srcEl)
                    && !src.equals(entity.getPosition())
                    && (type != MoveStepType.TURN_LEFT)
                    && (type != MoveStepType.TURN_RIGHT)
                    && (type != MoveStepType.UNLOAD)
                    && (type != MoveStepType.DISCONNECT)
                    && (type != MoveStepType.GO_PRONE)) {
                return false;
            }

            // Can't move through a hex with a LargeSupportTank or a grounded DropShip unless
            // infantry or a VTOL at high enough elevation
            if (!(entity instanceof Infantry)) {
                boolean validRoadTrain = false;
                for (Entity inHex : game.getEntitiesVector(src)) {
                    if (inHex.equals(entity)) {
                        continue;
                    }

                    // Ignore the first trailer behind a non-superheavy tractor which can be in the
                    // same hex
                    if (!entity.getAllTowedUnits().isEmpty() && !entity.isSuperHeavy()) {
                        Entity firstTrailer = game.getEntity(entity.getAllTowedUnits().get(0));
                        if (inHex.equals(firstTrailer)) {
                            validRoadTrain = true;
                        }
                    }

                    if ((inHex instanceof LargeSupportTank)
                            || (!entity.getAllTowedUnits().isEmpty() && !validRoadTrain)
                            || (!inHex.getAllTowedUnits().isEmpty())
                            || ((inHex instanceof Dropship)
                            && !inHex.isAirborne() && !inHex
                            .isSpaceborne())) {
                        if (getElevation() <= inHex.height()) {
                            return false;
                        }
                    }
                }
            }
        }

        // can't jump over too-high terrain
        if ((movementType == EntityMovementType.MOVE_JUMP)
                && (destAlt > (entity.getElevation()
                + entity.game.getBoard().getHex(entity.getPosition())
                .getLevel() + entity.getJumpMPWithTerrain() + (type == MoveStepType.DFA ? 1
                : 0)))) {
            return false;
        }

        // Certain movement types have terrain restrictions; terrain
        // restrictions are lifted when moving along a road or bridge,
        // or when flying. Naval movement does not have the pavement
        // exemption.
        if (entity.isLocationProhibited(dest, getElevation())
                // Units in prohibited terran should still be able to unload/disconnect
                && (type != MoveStepType.UNLOAD)
                && (type != MoveStepType.DISCONNECT)
                // Should allow vertical takeoffs
                && (type != MoveStepType.VTAKEOFF)
                // QuadVees can convert to vehicle mode even if they cannot enter the terrain
                && (type != MoveStepType.CONVERT_MODE)
                && (!isPavementStep() || (nMove == EntityMovementMode.NAVAL)
                || (nMove == EntityMovementMode.HYDROFOIL) || (nMove == EntityMovementMode.SUBMARINE))
                && (movementType != EntityMovementType.MOVE_VTOL_WALK)
                && (movementType != EntityMovementType.MOVE_VTOL_RUN)
                && (movementType != EntityMovementType.MOVE_VTOL_SPRINT)) {

            // We're allowed to pass *over* invalid
            // terrain, but we can't end there.
            if (isJumping()) {
                terrainInvalid = true;
            } else {
                // This is an illegal move.
                return false;
            }
        }

        // We need extra checking for DropShips, due to secondary positions
        // if the DropShip is taking off, MoveType will be safe thrust
        if ((entity instanceof Dropship) && !entity.isAirborne()
                && isPavementStep() && entity.isLocationProhibited(dest, getElevation())
                && (movementType != EntityMovementType.MOVE_SAFE_THRUST)
                && (type != MoveStepType.LOAD)
                && (type != MoveStepType.UNLOAD)) {
            for (int dir = 0; dir < 6; dir++) {
                Coords secondaryCoords = dest.translated(dir);
                Hex secondaryHex = game.getBoard().getHex(secondaryCoords);
                if (!secondaryHex.hasPavement()) {
                    return false;
                }
            }
        }

        // If we're a land train with mixed motive types, use the most restrictive type
        // to determine terrain restrictions
        if (!entity.getAllTowedUnits().isEmpty()
                && (type != MoveStepType.LOAD
                    && type != MoveStepType.UNLOAD
                    && type != MoveStepType.TOW
                    && type != MoveStepType.DISCONNECT)) {
            boolean prohibitedByTrailer = false;
            // Add up the trailers
            for (int id : entity.getAllTowedUnits()) {
                Entity tr = game.getEntity(id);
                prohibitedByTrailer = tr.isLocationProhibited(dest, getElevation());
                if (prohibitedByTrailer) {
                    return false;
                }
            }
        }

        // Jumping into a building hex below the roof ends the move
        // assume this applies also to sylph vtol movement
        if (!(src.equals(dest))
                && (src != entity.getPosition())
                && (isJumping() || (entity.getMovementMode() == EntityMovementMode.VTOL))
                && (srcEl < srcHex.terrainLevel(Terrains.BLDG_ELEV))) {
            return false;
        }

        // If we are *in* restricted terrain, we can only leave via roads.
        if ((movementType != EntityMovementType.MOVE_JUMP)
                && (movementType != EntityMovementType.MOVE_VTOL_WALK)
                && (movementType != EntityMovementType.MOVE_VTOL_RUN)
                && (movementType != EntityMovementType.MOVE_VTOL_SPRINT)
                // Units in prohibited terran should still be able to unload/disconnect
                && (type != MoveStepType.UNLOAD)
                && (type != MoveStepType.DISCONNECT)
                // Should allow vertical takeoffs
                && (type != MoveStepType.VTAKEOFF)
                // QuadVees can still convert to vehicle mode in prohibited terrain, but cannot leave
                && (type != MoveStepType.CONVERT_MODE)
                && entity.isLocationProhibited(src, getElevation()) && !isPavementStep()) {
            return false;
        }
        if (type == MoveStepType.UP) {
            if (!(entity.canGoUp(elevation - 1, getPosition()))) {
                return false;
            }
        }
        if (type == MoveStepType.DOWN) {
            if (!(entity.canGoDown(elevation + 1, getPosition()))) {
                return false;// We can't intentionally crash.
            }
        }
        if (entity instanceof VTOL) {
            if ((type == MoveStepType.BACKWARDS)
                    || (type == MoveStepType.FORWARDS)
                    || (type == MoveStepType.LATERAL_LEFT)
                    || (type == MoveStepType.LATERAL_LEFT_BACKWARDS)
                    || (type == MoveStepType.LATERAL_RIGHT)
                    || (type == MoveStepType.LATERAL_RIGHT_BACKWARDS)
                    || (type == MoveStepType.TURN_LEFT)
                    || (type == MoveStepType.TURN_RIGHT)) {
                if (getClearance() == 0) {// can't move on the ground.
                    return false;
                }
            }
        }
        if ((entity instanceof VTOL || entity.getMovementMode() == EntityMovementMode.WIGE)
                && getClearance() > 0
                && ((type == MoveStepType.BACKWARDS) || (type == MoveStepType.FORWARDS)
                        || (type == MoveStepType.LATERAL_LEFT) || (type == MoveStepType.LATERAL_LEFT_BACKWARDS)
                        || (type == MoveStepType.LATERAL_RIGHT) || (type == MoveStepType.LATERAL_RIGHT_BACKWARDS))) {
            // It's possible to fly under a bridge.
            if (destHex.containsTerrain(Terrains.BRIDGE_ELEV)) {
                if (elevation == destHex.terrainLevel(Terrains.BRIDGE_ELEV)) {
                    return false;
                }
            } else if (elevation <= (destHex.ceiling() - destHex.getLevel())) {
                // VTOLs and WiGEs can fly through woods and jungle below the level of the treetops on a road.
                if (destHex.containsTerrain(Terrains.WOODS) || destHex.containsTerrain(Terrains.JUNGLE)) {
                    return destHex.containsTerrainExit(Terrains.ROAD, dest.direction(src));
                }
                return false; // can't fly into woods or a cliff face
            }
        }

        // check the elevation is valid for the type of entity and hex
        if ((type != MoveStepType.DFA)
                && !entity.isElevationValid(elevation, destHex)) {
            if (isJumping()) {
                terrainInvalid = true;
            } else {
                return false;
            }
        }

        return true;
    }

    public int getElevation() {
        return elevation;
    }

    /**
     * In hexes with buildings, returns the elevation relative to the roof. Otherwise returns the elevation
     * relative to the surface.
     */
    public int getClearance() {
        Hex hex = entity.getGame().getBoard().getHex(getPosition());
        if (hex.containsTerrain(Terrains.BLDG_ELEV)) {
            return elevation - hex.terrainLevel(Terrains.BLDG_ELEV);
        }
        return elevation;
    }

    public int getAltitude() {
        return altitude;
    }

    public int getMineToLay() {
        return mineToLay;
    }

    protected void setMineToLay(int mineId) {
        mineToLay = mineId;
    }

    public int getBraceLocation() {
        return braceLocation;
    }

    protected void setBraceLocation(int value) {
        braceLocation = value;
    }

    protected void setVelocity(int vel) {
        velocity = vel;
    }

    public int getVelocity() {
        return velocity;
    }

    protected void setVelocityN(int vel) {
        velocityN = vel;
    }

    public int getVelocityN() {
        return velocityN;
    }

    protected void setVelocityLeft(int vel) {
        velocityLeft = vel;
    }

    public int getVelocityLeft() {
        return velocityLeft;
    }

    public int asfTurnCost(Game game, MoveStepType direction, Entity entity) {

        // jumpships (but not space stations and warships) never pay
        if ((entity instanceof Jumpship) && !(entity instanceof Warship)
                && !(entity instanceof SpaceStation)) {
            return 0;
        }

        // if we're behaving like a spheroid in atmosphere, we can spin around to our heart's content
        if (useSpheroidAtmosphere(game, entity)) {
            return 0;
        }

        // if in atmosphere, the rules are different
        if (useAeroAtmosphere(game, entity)) {
            // if they have a free turn, then this move is free
            if (hasFreeTurn()) {
                return 0;
            }
            // it costs half the current velocity (rounded up)
            return (int) Math.ceil(getVelocity() / 2.0);
        }

        // first check for thruster damage
        // put illegal for more than three thruster hits in CompileIllegal
        IAero a = (IAero) entity;
        int thrustCost = 0;
        if (direction == MoveStepType.TURN_LEFT) {
            thrustCost = a.getLeftThrustHits();
        }
        if (direction == MoveStepType.TURN_RIGHT) {
            thrustCost = a.getRightThrustHits();
        }

        if (game.useVectorMove()) {
            // velocity doesn't factor into advanced movement
            return (1 + thrustCost);
        }

        // based on velocity
        if (velocity < 3) {
            return 1 + thrustCost;
        } else if ((velocity > 2) && (velocity < 6)) {
            return 2 + thrustCost;
        } else if ((velocity > 5) && (velocity < 8)) {
            return 3 + thrustCost;
        } else if ((velocity > 7) && (velocity < 10)) {
            return 4 + thrustCost;
        } else if (velocity == 10) {
            return 5 + thrustCost;
        } else if (velocity == 11) {
            return 6 + thrustCost;
        }
        return (((6 + velocity) - 11) + thrustCost);
    }

    protected void setNTurns(int turns) {
        nTurns = turns;
    }

    public int getNTurns() {
        return nTurns;
    }

    protected void setNMoved(int moved) {
        nMoved = moved;
    }

    public int getNMoved() {
        return nMoved;
    }

    protected void setNRolls(int rolls) {
        nRolls = rolls;
    }

    public int getNRolls() {
        return nRolls;
    }

    protected void setOffBoard(boolean b) {
        offBoard = b;
    }

    public boolean isOffBoard() {
        return offBoard;
    }

    public int[] getVectors() {
        return mv;
    }

    protected void setVectors(int[] v) {
        if (v.length != 6) {
            return;
        }

        mv = v;
    }

    public boolean hasFreeTurn() {
        return freeTurn;
    }

    protected void setFreeTurn(boolean b) {
        freeTurn = b;
    }

    public int getNStraight() {
        return nStraight;
    }

    protected void setNStraight(int i) {
        nStraight = i;
    }

    /**
     * can this aero turn for any reason in atmosphere?
     */
    public boolean canAeroTurn(Game game) {
        Entity en = getEntity();

        if (!en.isAero()) {
            return false;
        }

        // spheroids in atmo can spin around like a centrifuge all they want
        if (useSpheroidAtmosphere(game, en)) {
            return true;
        }

        if (dueFreeTurn()) {
            return true;
        }

        // if its part of a maneuver then you can turn
        if (isManeuver()) {
            return true;
        }

        if (en instanceof ConvFighter) {
            // conventional fighters can only turn on free turns or maneuvers
            return false;
        }

        // Can't use thrust turns in the first hex of movement (or first 8 if ground)
        if (game.getBoard().onGround()) {
            // if flying on the ground map then they need to move 8 hexes first
            if (distance < 8) {
                return false;
            }
        } else if (distance == 0) {
            return false;
        }

        // must have been no prior turns in this hex (or 8 hexes if on ground)
        return getNTurns() == 0;
    }

    public boolean dueFreeTurn() {
        Entity en = getEntity();
        int straight = getNStraight();
        int vel = getVelocity();
        int thresh = 99;

        // I will assume that small craft should be treated as DropShips?
        if (en instanceof SmallCraft) {
            if (vel > 15) {
                thresh = 6;
            } else if (vel > 12) {
                thresh = 5;
            } else if (vel > 9) {
                thresh = 4;
            } else if (vel > 6) {
                thresh = 3;
            } else if (vel > 3) {
                thresh = 2;
            } else {
                thresh = 1;
            }
        } else if (en instanceof ConvFighter) {
            if (vel > 15) {
                thresh = 4;
            } else if (vel > 12) {
                thresh = 3;
            } else if (vel > 9) {
                thresh = 2;
            } else {
                thresh = 1;
            }
        } else {
            if (vel > 15) {
                thresh = 5;
            } else if (vel > 12) {
                thresh = 4;
            } else if (vel > 9) {
                thresh = 3;
            } else if (vel > 6) {
                thresh = 2;
            } else {
                thresh = 1;
            }
        }

        // different rules if flying on the ground map
        if (en.game.getBoard().onGround() && (getElevation() > 0)) {
            if (en instanceof Dropship) {
                thresh = vel * 8;
            } else if (en instanceof SmallCraft) {
                thresh = 8 + ((vel - 1) * 6);
            } else {
                thresh = 8 + ((vel - 1) * 4);
            }
        }

        if (straight >= thresh) {
            return true;
        }

        return false;

    }

    protected void setNDown(int i) {
        nDown = i;
    }

    public int getNDown() {
        return nDown;
    }

    public int getRecoveryUnit() {
        return recoveryUnit;
    }

    protected void setRecoveryUnit(int i) {
        recoveryUnit = i;
    }

    public int getManeuverType() {
        return maneuverType;
    }

    public boolean hasNoCost() {
        return noCost;
    }

    public boolean isManeuver() {
        return maneuver;
    }

    public Minefield getMinefield() {
        return mf;
    }

    /**
     * Should we treat this movement as if it is occurring for an aerodyne unit
     * flying in atmosphere?
     */
    boolean useAeroAtmosphere(Game game, Entity en) {
        if (!en.isAero()) {
            return false;
        }
        if (((IAero) en).isSpheroid()) {
            return false;
        }
        // are we in space?
        if (game.getBoard().inSpace()) {
            return false;
        }
        // are we airborne in non-vacuum?
        return en.isAirborne() && !game.getPlanetaryConditions().isVacuum();
    }

    /**
     * Should we treat this movement as if it is occurring for a spheroid unit
     * flying in atmosphere?
     */
    public boolean useSpheroidAtmosphere(Game game, Entity en) {
        return Compute.useSpheroidAtmosphere(game, en);
    }

    /**
     * @return An {@link ArrayList} of {@link Coords} containing buildings within a dropship's landing zone.
     */
    public ArrayList<Coords> getCrushedBuildingLocs() {
        if (crushedBuildingLocs == null) {
            crushedBuildingLocs = new ArrayList<>();
        }

        return crushedBuildingLocs;
    }

    public Entity getEntity() {
        return entity;
    }

    public Game getGame() {
        if (getEntity() != null) {
            return getEntity().getGame();
        } else {
            return null;
        }
    }

    public boolean isJumping() {
        // Need to consider if our type is START_JUMP, as when adding a
        // START_JUMP step, the MovePath may not be considered jumping yet
        return isJumpingPath || (type == MoveStepType.START_JUMP);
    }

    public boolean isCareful() {
        return isCarefulPath;
    }

    /**
     * Helper function to determine whether sprint is available as a game option to the entity
     */
    public boolean canUseSprint(Game game) {
        if (!game.getOptions().booleanOption(OptionsConstants.ADVGRNDMOV_TACOPS_SPRINT)) {
            return false;
        }
        if (entity instanceof Tank
                || (entity instanceof QuadVee && entity.getConversionMode() == QuadVee.CONV_MODE_VEHICLE)) {
            return  game.getOptions().booleanOption(OptionsConstants.ADVGRNDMOV_VEHICLE_ADVANCED_MANEUVERS);
        }
        if (entity instanceof LandAirMech) {
            return entity.getConversionMode() == LandAirMech.CONV_MODE_MECH
                    || (entity.getConversionMode() == LandAirMech.CONV_MODE_AIRMECH
                            && getClearance() <= 0);
        }
        return entity instanceof Mech;
    }
}
