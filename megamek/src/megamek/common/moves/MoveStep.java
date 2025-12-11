/*
 * Copyright (c) 2000-2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
import java.lang.System;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.Vector;

import megamek.common.Hex;
import megamek.common.HexTarget;
import megamek.common.LosEffects;
import megamek.common.ManeuverType;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.board.Coords;
import megamek.common.board.FloorTarget;
import megamek.common.compute.Compute;
import megamek.common.enums.BuildingType;
import megamek.common.enums.MPBoosters;
import megamek.common.enums.MoveStepType;
import megamek.common.equipment.IArmorState;
import megamek.common.equipment.Minefield;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.Mounted;
import megamek.common.game.Game;
import megamek.common.options.OptionsConstants;
import megamek.common.pathfinder.CachedEntityState;
import megamek.common.planetaryConditions.Atmosphere;
import megamek.common.planetaryConditions.PlanetaryConditions;
import megamek.common.units.*;
import megamek.logging.MMLogger;

/**
 * A single step in the entity's movement. Since the path planner uses shallow copies of MovePaths, multiple paths may
 * share the same MoveStep, so this class needs to be agnostic of what path it belongs to.
 *
 * @since Aug 28, 2003
 */
public class MoveStep implements Serializable {
    private static final MMLogger LOGGER = MMLogger.create(MoveStep.class);

    @Serial
    private static final long serialVersionUID = -6075640793056182285L;
    /**
     * When supplying additional int data, use this to key the index of the cargo being picked up
     */
    public static final int CARGO_PICKUP_KEY = 0;

    /**
     * When supplying additional int data, use this to key the location of the cargo being picked up (i.e. mek left
     * arm/right arm, vehicle body, etc.)
     */
    public static final int CARGO_LOCATION_KEY = 1;

    private final MoveStepType type;
    private int targetId = Entity.NONE;
    private int targetType = Targetable.TYPE_ENTITY;
    private Coords targetPos;

    private Coords position;
    private int boardId = 0;
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
     * This step's static movement type. Additional steps in the path will not change this value.
     */
    private EntityMovementType movementType;
    /**
     * The movement mode after this step completes. Mode conversions will modify it, though it may not take effect until
     * the end of movement.
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
    private boolean onlyPavementOrRoad; // additive
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
    private boolean isUsingMekJumpBooster = false;

    /**
     * Determines if this MoveStep is part of a MovePath that is moving carefully.
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
    // for Aerospace, they may get pushed off board by OOC
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

    /**
     * A map used to hold any additional data that this move step requires. Preferable to constantly adding new fields
     * for low-usage one-shot data
     */
    Map<Integer, Integer> additionalData = new HashMap<>();

    private Minefield mf;

    /**
     * Flag that indicates that this step is into prohibited terrain.
     * <p>
     * If the unit is jumping, this step is only invalid if it is the end of the path.
     */
    private boolean terrainInvalid = false;

    /**
     * A collection of buildings that are crushed during this move step. This is used for landed Aerodyne Dropships and
     * Mobile Structures.
     */
    private ArrayList<Coords> crushedBuildingLocs;

    /**
     * Create a step of the given type.
     *
     * @param type - should match one of the MovePath constants, but this is not currently checked.
     */
    public MoveStep(MovePath path, MoveStepType type) {
        this.type = type;
        if (path != null) {
            entity = path.getEntity();
            isJumpingPath = path.isJumping();
            isUsingMekJumpBooster = path.contains(MoveStepType.JUMP_MEK_MECHANICAL_BOOSTER);
            isCarefulPath = path.isCareful();
            boardId = path.getFinalBoardId();
        }

        hasEverUnloaded = (type == MoveStepType.UNLOAD) ||
              (type == MoveStepType.LAUNCH) ||
              (type == MoveStepType.DROP) ||
              (type == MoveStepType.UNDOCK) ||
              (type == MoveStepType.DISCONNECT);
    }

    /**
     * Create a step with the given target and a position for that target
     *
     * @param type   - should match one of the MovePath constants, but this is not currently checked.
     * @param target - the <code>Targetable</code> that is the target of this step. For example, the enemy being
     *               charged.
     * @param pos    = the <code>Coords</code> for the target position.
     */
    public MoveStep(MovePath path, MoveStepType type, Targetable target, Coords pos) {
        this(path, type);
        targetId = target.getId();
        targetType = target.getTargetType();
        targetPos = pos;
        hasEverUnloaded = (type == MoveStepType.UNLOAD) ||
              (type == MoveStepType.LAUNCH) ||
              (type == MoveStepType.DROP) ||
              (type == MoveStepType.UNDOCK) ||
              (type == MoveStepType.DISCONNECT);
    }

    /**
     * Create a step with the given target.
     *
     * @param type   - should match one of the MovePath constants, but this is not currently checked.
     * @param target - the <code>Targetable</code> that is the target of this step. For example, the enemy being
     *               charged.
     */
    public MoveStep(MovePath path, MoveStepType type, Targetable target) {
        this(path, type);
        targetId = target.getId();
        targetType = target.getTargetType();
        hasEverUnloaded = (type == MoveStepType.UNLOAD) ||
              (type == MoveStepType.LAUNCH) ||
              (type == MoveStepType.DROP) ||
              (type == MoveStepType.UNDOCK) ||
              (type == MoveStepType.DISCONNECT);
    }

    /**
     * Create a step with the given mine to lay.
     *
     * @param path              {@link MovePath} to work through.
     * @param type              should match one of the MovePath constants, but this is not currently checked.
     * @param additionalIntData "mineToLay" by default to retain compatibility with existing code "braceLocation" if the
     *                          move step type is BRACE
     */
    public MoveStep(MovePath path, MoveStepType type, int additionalIntData) {
        this(path, type);

        if (type == MoveStepType.BRACE) {
            this.braceLocation = additionalIntData;
        } else if (type == MoveStepType.LAY_MINE) {
            this.mineToLay = additionalIntData;
        } else if (type == MoveStepType.PICKUP_CARGO) {
            this.additionalData.put(CARGO_PICKUP_KEY, additionalIntData);
        } else if (type == MoveStepType.DROP_CARGO) {
            this.additionalData.put(CARGO_LOCATION_KEY, additionalIntData);
        }
    }

    /**
     * Creates a step with an arbitrary int-to-int mapping of additional data.
     */
    public MoveStep(MovePath path, MoveStepType type, Map<Integer, Integer> additionalIntData) {
        this(path, type);

        additionalData.putAll(additionalIntData);
    }

    /**
     * Create a step with the units to launch or drop.
     *
     * @param path    {@link MovePath} to work with.
     * @param type    should match one of the {@link MovePath} constants, but this is not currently checked.
     * @param targets vector of integers identifying the entities to launch
     */
    public MoveStep(MovePath path, MoveStepType type, TreeMap<Integer, Vector<Integer>> targets) {
        this(path, type);
        launched = targets;
        hasEverUnloaded = (type == MoveStepType.UNLOAD) ||
              (type == MoveStepType.LAUNCH) ||
              (type == MoveStepType.DROP) ||
              (type == MoveStepType.UNDOCK) ||
              (type == MoveStepType.DISCONNECT);
    }

    public MoveStep(MovePath path, MoveStepType type, int recovery, int mineToLay) {
        this(path, type);
        recoveryUnit = recovery;
        this.mineToLay = mineToLay;
    }

    public MoveStep(MovePath path, MoveStepType type, boolean noCost) {
        this(path, type);
        this.noCost = noCost;
    }

    public MoveStep(MovePath path, MoveStepType type, boolean noCost, boolean isManeuver, int maneuverType) {
        this(path, type);
        this.noCost = noCost;
        maneuver = isManeuver;
        this.maneuverType = maneuverType;
    }

    public MoveStep(MovePath path, MoveStepType type, int recovery, int mineToLay, int manType) {
        this(path, type);
        recoveryUnit = recovery;
        this.mineToLay = mineToLay;
        maneuverType = manType;
    }

    public MoveStep(MovePath path, MoveStepType type, Minefield mf) {
        this(path, type);
        this.mf = mf;
    }

    public static MoveStep createChangeBoardMoveStep(MovePath path, Coords finalPosition, int finalBoardId) {
        MoveStep newStep = new MoveStep(path, MoveStepType.CHANGE_BOARD);
        newStep.boardId = finalBoardId;
        newStep.position = finalPosition;
        return newStep;
    }

    @Override
    public String toString() {
        return type.getHumanReadableLabel();
    }

    public MoveStepType getType() {
        return type;
    }

    /**
     * Set the target of the current step.
     *
     * @param target - the <code>Targetable</code> that is the target of this step. For example, the enemy being
     *               charged. If there is no target, pass a <code>null</code>
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
     *
     * @return The <code>Targetable</code> that is the target of this step. For example, the enemy being charged. This
     *       value may be
     *       <code>null</code>
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

    public Integer getAdditionalData(int key) {
        return additionalData.getOrDefault(key, null);
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
     * @param game              The current {@link Game}
     * @param entity            {@link Entity} to work with.
     * @param prev              {@link MoveStep} Previous step.
     * @param cachedEntityState {@link CachedEntityState} the Cached Entity State
     */
    void compileMove(final Game game, final Entity entity, MoveStep prev, CachedEntityState cachedEntityState) {

        Hex destHex = game.getBoard(boardId).getHex(getPosition());

        // Check for pavement movement.
        if (!entity.isAirborne() && Compute.canMoveOnPavement(game, prev.getPosition(), getPosition(), this)) {
            setPavementStep(true);
        } else {
            setPavementStep(false);
            setOnlyPavementOrRoad(false);
        }

        setHasJustStood(false);
        if (prev.isThisStepBackwards() != isThisStepBackwards()) {
            setDistance(0); // start over after shifting gears
        }

        addDistance(1);

        // need to reduce velocity left for aerospace units (and also reset nTurns) this is handled differently by
        // aerospace units operating on the ground map and by spheroids in atmosphere
        if (entity.isAirborne() && game.getBoard(boardId).isGround()) {
            setNMoved(getNMoved() + 1);
            if ((entity.getMovementMode() != EntityMovementMode.SPHEROID) && (getNMoved() >= 16)) {
                setVelocityLeft(getVelocityLeft() - 1);
                setNMoved(0);
            }
        } else if (entity.isAirborne() && !game.useVectorMove() && !useSpheroidAtmosphere(game, entity)) {
            setVelocityLeft(getVelocityLeft() - 1);
            setNTurns(0);
        }

        // Track number of moves straight for aero free moves in atmosphere, vehicle turn modes, and bootlegger maneuver
        setNStraight(getNStraight() + 1);
        // if in atmosphere, then I need to know if this move qualifies the unit for a free turn
        if (useAeroAtmosphere(game, entity)) {
            if (game.getBoard(boardId).isGround() && (getNStraight() > 7)) {
                // if flying on ground map, then you have to fly at least 8 straight hexes between turns (free or not)
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
            Hex hex = game.getBoard(boardId).getHex(getPosition());
            setElevation(Math.max(0, hex.terrainLevel(Terrains.BLDG_ELEV)));
            // If we're DFA-ing, we want to be 1 above the level of the target. However, if that puts us in the
            // ground, we're instead 1 above the level of the hex right before the target.
            Hex hex2 = game.getBoard(boardId).getHex(prev.getPosition());
            int otherEl = Math.max(0, hex2.terrainLevel(Terrains.BLDG_ELEV));
            if (otherEl > getElevation()) {
                setElevation(otherEl);
            }
            setElevation(getElevation() + 1);
        } else if (isJumping()) {
            Hex hex = game.getBoard(boardId).getHex(getPosition());
            Optional<IBuilding> optionalBuilding = game.getBuildingAt(entity.getPosition(), boardId);

            boolean isInsideTheSameBuilding = false;
            if (optionalBuilding.isPresent()) {
                Optional<IBuilding> optionalBuildingAtCurrentStep = game.getBuildingAt(getPosition(), boardId);
                if (optionalBuildingAtCurrentStep.isPresent()) {
                    isInsideTheSameBuilding = optionalBuildingAtCurrentStep.get().equals(optionalBuilding.get());
                }
            }

            int maxElevation = entity.getElevation();
            if (!isInsideTheSameBuilding) {
                maxElevation = (getAvailableJumpMP(entity) +
                      entity.getElevation() +
                      game.getBoard(boardId).getHex(entity.getPosition()).getLevel()) - hex.getLevel();
            }
            int building = hex.terrainLevel(Terrains.BLDG_ELEV);
            int depth = -hex.depth(true);
            int ceiling = hex.ceiling();

            // Set depth to 0 (surface level) in several cases:
            // 1. Jumping onto ice-covered water hex,
            // 2. Jumping onto water with Hover move mode,
            // 3. Jumping onto water with WiGE move mode
            if (hex.containsTerrain(Terrains.WATER)) {
                if (hex.containsTerrain(Terrains.ICE) ||
                      entity.getMovementMode() == EntityMovementMode.HOVER ||
                      entity.getMovementMode() == EntityMovementMode.WIGE) {
                    depth = 0;
                }
            }

            // grounded DropShips are treated as level 10 buildings for purposes of jumping over
            boolean grdDropship = false;
            if (building < 10) {
                for (Entity inHex : game.getEntitiesVector(getPosition())) {
                    if (inHex.equals(entity)) {
                        continue;
                    }
                    if ((inHex instanceof Dropship) && !inHex.isAirborne() && !inHex.isSpaceborne()) {
                        building = 10;
                        grdDropship = true;
                    }
                }
            }
            if ((entity instanceof Infantry) && !grdDropship) {
                // infantry can jump into a building
                // Maybe this line is a bit too much, but it seems to work by coincidence
                setElevation(Math.max(depth, Math.min(building, maxElevation)));
            } else {
                int subDepth = Math.max(depth, building);

                switch (entity.getMovementMode()) {
                    // WiGE ends the jump at 1 elevation
                    case WIGE -> setElevation(ceiling + 1);
                    // Hover ends the jump above the water
                    case HOVER -> setElevation(ceiling);
                    default -> setElevation(subDepth);
                }
            }
            // Handle bridge elevation for jumping
            if (hex.containsTerrain(Terrains.BRIDGE)) {
                int bridgeElev = hex.terrainLevel(Terrains.BRIDGE_ELEV);
                if (climbMode() && (maxElevation >= bridgeElev)) {
                    // Climb mode ON - go onto bridge if reachable
                    setElevation(Math.max(getElevation(), bridgeElev));
                } else if (!entity.isElevationValid(getElevation(), hex)) {
                    // Can't fit under bridge - force onto bridge (TO:AR 115)
                    setElevation(bridgeElev);
                }
            }
        } else {
            IBuilding bld = game.getBoard(boardId).getBuildingAt(getPosition());

            if (bld != null) {
                Hex hex = game.getBoard(boardId).getHex(getPosition());
                int maxElevation = (entity.getElevation() + game.getBoard(boardId)
                      .getHex(entity.getPosition())
                      .getLevel()) -
                      hex.getLevel();

                // Meks can climb up level 2 walls or fewer while everything can only climb up one level
                if (entity instanceof Mek) {
                    maxElevation += 2;
                } else {
                    maxElevation++;
                }

                if (bld.getBuildingType() == BuildingType.WALL) {
                    if (maxElevation >= hex.terrainLevel(Terrains.BLDG_ELEV)) {
                        setElevation(Math.max(getElevation(), hex.terrainLevel(Terrains.BLDG_ELEV)));
                    } else {
                        // if the wall is taller than the unit then they cannot climb it or enter it
                        return;
                    }
                } else {
                    setElevation(entity.calcElevation(game.getBoard(boardId).getHex(prev.getPosition()),
                          game.getBoard(boardId).getHex(getPosition()),
                          elevation,
                          climbMode()));
                }
            } else {
                setElevation(entity.calcElevation(game.getBoard(boardId).getHex(prev.getPosition()),
                      game.getBoard(boardId).getHex(getPosition()),
                      elevation,
                      climbMode()));
            }
        }

        // if this is a flying aero, then there is no MP cost for moving
        if ((prev.getAltitude() > 0) || game.getBoard(boardId).isSpace()) {
            setMp(0);
            // if this is a spheroid in atmosphere then the cost is always one
            // if it is the very first step, we prepend the cost of hovering for convenience
            if (useSpheroidAtmosphere(game, entity)) {
                if (game.getBoard(boardId).isGround()) {
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
        // PLAYTEST2 water changes
        if (game.getOptions().booleanOption(OptionsConstants.PLAYTEST_2)) {
            if (!isPavementStep() &&
                  (destHex.terrainLevel(Terrains.WATER) > 0) &&
                  !(destHex.containsTerrain(Terrains.ICE) && (elevation >= 0)) &&
                  !(destHex.terrainLevel(Terrains.BRIDGE_ELEV) == elevation) &&
                  (entity.getMovementMode() != EntityMovementMode.HOVER) &&
                  (entity.getMovementMode() != EntityMovementMode.NAVAL) &&
                  (entity.getMovementMode() != EntityMovementMode.HYDROFOIL) &&
                  (entity.getMovementMode() != EntityMovementMode.INF_UMU) &&
                  (entity.getMovementMode() != EntityMovementMode.SUBMARINE) &&
                  (entity.getMovementMode() != EntityMovementMode.VTOL) &&
                  (entity.getMovementMode() != EntityMovementMode.WIGE) &&
                  (entity.getMovementMode() != EntityMovementMode.BIPED) &&
                  (entity.getMovementMode() != EntityMovementMode.QUAD) &&
                  (entity.getMovementMode() != EntityMovementMode.TRIPOD) &&
                  !cachedEntityState.hasWorkingMisc(MiscType.F_FULLY_AMPHIBIOUS)) {
                setRunProhibited(true);
            }
        } else {
            if (!isPavementStep() &&
                  (destHex.terrainLevel(Terrains.WATER) > 0) &&
                  !(destHex.containsTerrain(Terrains.ICE) && (elevation >= 0)) &&
                  !(destHex.terrainLevel(Terrains.BRIDGE_ELEV) == elevation) &&
                  (entity.getMovementMode() != EntityMovementMode.HOVER) &&
                  (entity.getMovementMode() != EntityMovementMode.NAVAL) &&
                  (entity.getMovementMode() != EntityMovementMode.HYDROFOIL) &&
                  (entity.getMovementMode() != EntityMovementMode.INF_UMU) &&
                  (entity.getMovementMode() != EntityMovementMode.SUBMARINE) &&
                  (entity.getMovementMode() != EntityMovementMode.VTOL) &&
                  (entity.getMovementMode() != EntityMovementMode.WIGE) &&
                  !cachedEntityState.hasWorkingMisc(MiscType.F_FULLY_AMPHIBIOUS)) {
                setRunProhibited(true);
            }
        }

        if (entity.getMovedBackwards() && !entity.hasQuirk(OptionsConstants.QUIRK_POS_POWER_REVERSE)) {
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
        } else if (magmaLevel == 2) {
            // Check for liquid magma
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
                IBuilding bld = game.getBoard(boardId).getBuildingAt(pos);
                if (bld != null) {
                    getCrushedBuildingLocs().add(pos);
                    // This is dangerous!
                    danger = true;
                }
            }
        }

        // WiGEs get bonus MP for each string of three consecutive hexes they descend.
        if (entity.getMovementMode() == EntityMovementMode.WIGE &&
              getClearance() > 0 &&
              game.getOptions().booleanOption(OptionsConstants.ADVANCED_GROUND_MOVEMENT_VEHICLE_ADVANCED_MANEUVERS)) {

            if (game.getBoard(boardId).getHex(getPosition()).ceiling() <
                  game.getBoard(boardId).getHex(prev.getPosition()).ceiling()) {
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
     * @param game   The current {@link Game}
     * @param entity the <code>Entity</code> taking this step.
     * @param prev   the previous step in the path.
     */
    protected void compile(final Game game, final Entity entity, MoveStep prev, CachedEntityState cachedEntityState) {
        // set up the current move step using the state of the previous step
        copy(game, prev);

        // Is this the first step?
        prev = evaluateFirstStep(game, entity, prev);

        PhasePass phasePass = PhasePassSelector.getPhasePass(getType());
        phasePass.execute(this, game, entity, prev, cachedEntityState);

        if (noCost) {
            setMp(0);
        }

        if (type != MoveStepType.CONVERT_MODE) {
            movementMode = prev.getMovementMode();
        }

        // Tanks can just drive out of hull-down. If we're a tank, and we moved
        // then we are no longer hull-down.
        if ((entity instanceof Tank ||
              (entity instanceof QuadVee && entity.getConversionMode() == QuadVee.CONV_MODE_VEHICLE)) &&
              (distance > 0)) {
            setHullDown(false);
        }

        // Update the entity's total MP used.
        addMpUsed(getMp());

        // Check for a stacking violation.
        final Entity violation = Compute.stackingViolation(game,
              entity,
              getElevation(),
              getPosition(), boardId,
              null,
              climbMode,
              true);
        if ((violation != null) && (getType() != MoveStepType.CHARGE) && (getType() != MoveStepType.DFA)) {
            setStackingViolation(true);
        }

        // set moveType, illegal, trouble flags
        compileIllegal(game, entity, prev, cachedEntityState);
    }

    /**
     * Checks if this is counted to be the first step in a move path, which is the case when the given previous move
     * step is null or does not count as an actual action. Returns the previous move step if it is not null and a fake
     * "F" (forward move) first move step otherwise.
     *
     * @param prev The previous move step in the path, if any
     *
     * @return The previous move step if it is not null, a fake forward move step otherwise
     */
    private MoveStep evaluateFirstStep(Game game, Entity entity, MoveStep prev) {
        if (prev == null) {
            setFirstStep();
            return createFakeFirstStep(game, entity);

        } else if (prev.isFirstStep() && prev.isClimbMode()) {
            // A climb mode change is only meta info and does not count as an action
            setFirstStep();

        } else if (prev.isFirstStep()
              && prev.isTurning
              && entity instanceof Infantry infantry
              && !entity.isBattleArmor()
              && !infantry.hasActiveFieldArtillery()) {
            // For CI, turning is only a graphical distinction unless they are field artillery
            setFirstStep();
        }
        return prev;
    }

    private MoveStep createFakeFirstStep(Game game, Entity entity) {
        MoveStep prev = new MoveStep(null, MoveStepType.FORWARDS);
        prev.setFromEntity(entity, game);
        prev.isCarefulPath = isCareful();
        prev.isJumpingPath = isJumping();
        return prev;
    }

    /**
     * Returns whether the two-step types contain opposite turns
     */
    boolean oppositeTurn(MoveStep turn2) {
        return switch (type) {
            case TURN_LEFT -> turn2.getType() == MoveStepType.TURN_RIGHT;
            case TURN_RIGHT -> turn2.getType() == MoveStepType.TURN_LEFT;
            default -> false;
        };
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
     * @param prev Previous {@link MoveStep}
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
        onlyPavementOrRoad = prev.onlyPavementOrRoad;
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
        additionalData = new HashMap<>(additionalData);
    }

    /**
     * Sets this state as coming from the entity.
     *
     * @param entity Set local {@link Entity} with passed in one.
     */
    public void setFromEntity(Entity entity, Game game) {
        this.entity = entity;
        position = entity.getPosition();
        boardId = entity.getBoardId();
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

        mv = new int[] { 0, 0, 0, 0, 0, 0 };
        System.arraycopy(tempMv, 0, mv, 0, 6);

        // if ASF get velocity
        if (entity.isAero()) {
            IAero a = (IAero) entity;
            velocity = a.getCurrentVelocity();
            velocityN = a.getNextVelocity();
            velocityLeft = a.getCurrentVelocity() - entity.delta_distance;
            if (game.getBoard(boardId).isGround()) {
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

        // Cannot run while using Mek tracks
        if (entity instanceof Mek &&
              entity.getMovementMode() == EntityMovementMode.TRACKED &&
              !(entity instanceof QuadVee)) {
            isRunProhibited = true;
        }

        // check pavement & water
        if (position != null) {
            Hex curHex = game.getBoard(boardId).getHex(position);
            if (curHex.hasPavementOrRoad()) {
                if (curHex.hasPavement()) {
                    isPavementStep = true;
                    onlyPavementOrRoad = true;
                } else if (curHex.containsTerrain(Terrains.ROAD, Terrains.ROAD_LVL_DIRT)) {
                    if (entity.getMovementMode().isHover()) {
                        onlyPavementOrRoad = true;
                    }
                } else if (curHex.containsTerrain(Terrains.ROAD, Terrains.ROAD_LVL_GRAVEL)) {
                    if (entity.getMovementMode().isHover() || entity.getMovementMode().isTracked()) {
                        onlyPavementOrRoad = true;
                    }
                }
                // if we previously moved, and didn't get a pavement bonus, we
                // shouldn't now get one, either (this can happen when skidding
                // onto a pavement hex
                if (!entity.gotPavementOrRoadBonus && (entity.delta_distance > 0)) {
                    onlyPavementOrRoad = false;
                }
            }
            // if entity already moved into water it can't run now
            if (curHex.containsTerrain(Terrains.WATER) &&
                  (entity.getElevation() < 0) &&
                  (distance > 0) &&
                  (nMove != EntityMovementMode.NAVAL) &&
                  (nMove != EntityMovementMode.HYDROFOIL) &&
                  (nMove != EntityMovementMode.SUBMARINE) &&
                  (nMove != EntityMovementMode.INF_UMU)) {
                // PLAYTEST2 Water changes
                if (game.getOptions().booleanOption(OptionsConstants.PLAYTEST_2)) {
                    if (nMove != EntityMovementMode.BIPED && nMove != EntityMovementMode.QUAD && nMove !=
                          EntityMovementMode.TRIPOD) {
                        isRunProhibited = true;
                    }
                } else {
                    isRunProhibited = true;
                }
            }
        }
    }

    /**
     * Adjusts facing to comply with the type of step indicated.
     *
     * @param stepType {@link MoveStepType} Step to adjust facing to.
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
     * @param dir Moves the position one hex in the direction indicated. Does not change facing.
     */
    public void moveInDir(int dir) {
        position = position.translated(dir);
    }

    /**
     * @param increment Adds a certain amount to the distance parameter.
     */
    public void addDistance(int increment) {
        distance += increment;
    }

    /**
     * @param increment Adds a certain amount to the mpUsed parameter.
     */
    public void addMpUsed(int increment) {
        mpUsed += increment;
    }

    public boolean isDanger() {
        return danger;
    }

    public int getDistance() {
        return distance;
    }

    public int getFacing() {
        return facing;
    }

    public boolean isFirstStep() {
        return firstStep;
    }

    public boolean isHasJustStood() {
        return hasJustStood;
    }

    public boolean isPavementStep() {
        return isPavementStep;
    }

    public boolean isProne() {
        return isProne;
    }

    public boolean isFlying() {
        return isFlying;
    }

    public boolean isHullDown() {
        return isHullDown;
    }

    public boolean climbMode() {
        return climbMode;
    }

    public boolean isTurning() {
        return isTurning;
    }

    public boolean isUnloaded() {
        return isUnloaded;
    }

    public boolean isUsingMASC() {
        return isUsingMASC;
    }

    public boolean isUsingSupercharger() {
        return isUsingSupercharger;
    }

    public boolean isUsingMekJumpBooster() {
        return isUsingMekJumpBooster;
    }

    public boolean isEvading() {
        return isEvading;
    }

    public boolean isSelfDestructing() {
        return isSelfDestructing;
    }

    public boolean isRolled() {
        return isRolled;
    }

    void setRolled(boolean roll) {
        isRolled = roll;
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
     * @param path A MovePath that contains this step.
     *
     * @return <code>true</code> if the step is legal. <code>false</code>
     *       otherwise.
     */
    public boolean isLegal(MovePath path) {
        // A step is legal if it's static movement type is not illegal, and it is either a valid end position, or not
        // an end position.
        return ((movementType != EntityMovementType.MOVE_ILLEGAL) && (isLegalEndPos() || !isEndPos(path)));
    }

    /**
     * Return this step's movement type.
     *
     * @return the <code>int</code> constant for this step's movement type.
     */
    public EntityMovementType getMovementType(boolean isLastStep) {
        EntityMovementType moveType = movementType;
        // If this step's position is the end of the path, and it is not a valid end position, then the movement type
        // is "illegal".
        if (isLastStep && !isLegalEndPos()) {
            moveType = EntityMovementType.MOVE_ILLEGAL;
        }
        return moveType;
    }

    void setMovementMode(EntityMovementMode movementMode) {
        this.movementMode = movementMode;
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
     *       path. If the step is not legal for an end of a path, then
     *       <code>false</code> is returned.
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
        } else if (hasEverUnloaded &&
              (type != MoveStepType.UNLOAD) &&
              (type != MoveStepType.LAUNCH) &&
              (type != MoveStepType.DROP) &&
              (type != MoveStepType.UNDOCK) &&
              (type != MoveStepType.DISCONNECT) &&
              (type != MoveStepType.CHAFF) &&
              (type != MoveStepType.DROP_CARGO) &&
              (getAltitude() == 0)) {
            // Can't be after unloading BA/inf
            legal = false;
        }

        return legal;
    }

    /**
     * Update this step's status as the ending position of a path. See {@link #isLegalEndPos()},
     * {@link #isEndPos(MovePath)}, and {@link MovePath#addStep(MoveStep)} for additional information.
     *
     * @param isEnd the <code>boolean</code> flag that specifies that this step's position is the end of a path.
     *
     * @return <code>true</code> if the path needs to keep updating the steps.
     *       <code>false</code> if the update of the path is complete.
     *       <p>
     */
    public boolean setEndPos(boolean isEnd) {
        boolean isEndPos = true;
        // A step that is always illegal is always the end of the path.
        if (EntityMovementType.MOVE_ILLEGAL == movementType) {
            isEnd = true;
        }

        // If this step didn't already know its status as the ending position of a path, then there are more updates
        // to do
        boolean moreUpdates = (isEndPos != isEnd);

        // If this step isn't the end step anymore, we might not be in danger after all
        Hex pos = getGame().getBoard(boardId).getHex(position);
        if (getGame().getOptions().booleanOption(OptionsConstants.ADVANCED_GROUND_MOVEMENT_PSR_JUMP_HEAVY_WOODS)) {
            if (!isEnd &&
                  isJumping() &&
                  (pos.containsTerrain(Terrains.WOODS, 2) || pos.containsTerrain(Terrains.WOODS, 3))) {
                danger = false;
                pastDanger = false;
            }
        }

        return moreUpdates;
    }

    /**
     * A step is in an end position if it is the last legal step, or is an illegal step past the last legal step.
     *
     * @param path {@link MovePath} to check
     *
     * @return true if a step is considered to be in an end position for the given MovePath.
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
        // Starting from the end, each step is considered the last step until we find a legal last step
        boolean lastStep = true;
        for (int i = steps.size() - 1; i >= 0; i--) {
            MoveStep step = steps.get(i);
            boolean stepMatch = this.equals(step);

            if (lastStep) {
                lastStep = step.getMovementType(true) == EntityMovementType.MOVE_ILLEGAL;
            }

            // If there is a legal step after us, we're not the end
            if ((step.getMovementType(lastStep) != EntityMovementType.MOVE_ILLEGAL) && !stepMatch) {
                return false;
            } else if (stepMatch) {
                // If we found the current step, no need to check the others
                return true;
            }
        }
        // Shouldn't reach here, since this step is assumed be in the step list
        return false;
    }

    public int getMpUsed() {
        return mpUsed;
    }

    public boolean isOnlyPavementOrRoad() {
        return onlyPavementOrRoad;
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

    public void setPosition(Coords c) {
        position = c;
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

    protected void setFirstStep() {
        firstStep = true;
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

    /**
     * This function is POSSIBLY A HACK!
     * <p>
     * DO NOT CALL THIS FUNCTION! I need to find out why this value is being set directly through a couple off functions
     * in the MoveStep compilation process. It should use the {@link MoveStep#setUnloaded} but I don't know why it does
     * this instead.
     *
     * @param b sets hasEverUnloaded to this value
     */
    @Deprecated(since = "0.50.07", forRemoval = true)
    protected void setHasEverUnloaded(boolean b) {
        hasEverUnloaded = b;
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

    protected void setOnlyPavementOrRoad(boolean b) {
        onlyPavementOrRoad = b;
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

    boolean isRunAllowed() {
        return !isRunProhibited;
    }

    protected void setStackingViolation(boolean isStackingViolation) {
        this.isStackingViolation = isStackingViolation;
    }

    /**
     * This function checks that a step is legal. And adjust the movement type. This only checks for things that can
     * make this step by itself illegal. Things that can make a step illegal as part of a movement path are considered
     * in MovePath.addStep.
     *
     * @param game   The current {@link Game}
     * @param entity The {@link Entity} taking this step.
     * @param prev   The {@link MoveStep} previous step in the path.
     */
    private void compileIllegal(final Game game, final Entity entity, final MoveStep prev,
          CachedEntityState cachedEntityState) {
        final MoveStepType stepType = getType();
        final boolean isInfantry = entity instanceof Infantry;
        final boolean isTank = entity instanceof Tank;

        Coords curPos = getPosition();
        Coords lastPos = prev.getPosition();
        boolean isUnjammingRAC = entity.isUnjammingRAC();
        prevStepOnPavement = prev.isPavementStep();
        isTurning = prev.isTurning();
        isUnloaded = prev.isUnloaded();

        // guilty until proven innocent
        movementType = EntityMovementType.MOVE_ILLEGAL;

        // Crushing buildings creates rubble, and Dropships can't drive on
        // rubble, so they get stuck
        if ((entity instanceof Dropship) && !prev.getCrushedBuildingLocs().isEmpty()) {
            return;
        }
        // AERO STUFF
        // I am going to put in a whole separate section for Aerospace and just return from it only if Aerospace are
        // airborne, otherwise they should move like other units
        if (type == MoveStepType.HOVER &&
              entity instanceof LandAirMek &&
              entity.getMovementMode() == EntityMovementMode.WIGE &&
              entity.getAltitude() <= 3) {
            if (mpUsed <= cachedEntityState.getWalkMP()) {
                movementType = EntityMovementType.MOVE_VTOL_WALK;
            } else if (mpUsed <= cachedEntityState.getRunMP()) {
                movementType = EntityMovementType.MOVE_VTOL_RUN;
            } else {
                movementType = EntityMovementType.MOVE_ILLEGAL;
            }
            return;
        }

        if (type == MoveStepType.CHANGE_BOARD && entity.isAirborne()) {
            movementType = EntityMovementType.MOVE_NONE;
            return;
        }

        if ((prev.getAltitude() > 0) || game.getBoard(boardId).isSpace()) {
            // Ejected crew/pilots just drift or parachute, resulting in a move_none type
            if (entity instanceof EjectedCrew) {
                movementType = EntityMovementType.MOVE_NONE;
                return;
            }

            // If airborne and some other non-Aero unit then everything is illegal, except
            // turns and conversion to AirMek
            if (!entity.isAero()) {
                switch (type) {
                    case TURN_LEFT:
                    case TURN_RIGHT:
                        movementType = EntityMovementType.MOVE_WALK;
                        break;
                    case CONVERT_MODE:
                        movementType = EntityMovementType.MOVE_NONE;
                    default:
                        break;
                }
                return;
            }

            int tmpSafeTh = cachedEntityState.getWalkMP();
            IAero a = (IAero) entity;

            // if the vessel is "immobile" due to shut down or pilot black out
            // then all moves are illegal
            if (entity.isImmobile()) {
                return;
            }

            // can't let players do an illegal move and use that to go less than
            // velocity
            if (!isFirstStep() && (prev.getMovementType(false) == EntityMovementType.MOVE_ILLEGAL)) {
                return;
            }

            // check the fuel requirements
            if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_AERO_RULES_FUEL_CONSUMPTION) &&
                  entity.hasEngine() &&
                  a.requiresFuel()) {
                int fuelUsed = mpUsed + Math.max(mpUsed - cachedEntityState.getWalkMP(), 0);
                if (fuelUsed > a.getCurrentFuel()) {
                    return;
                }
            }

            // **Space turning limits**//
            if (game.getBoard(boardId).isSpace()) {
                // space stations can only turn and launch spacecraft
                if ((entity instanceof SpaceStation) &&
                      !((type == MoveStepType.TURN_LEFT) ||
                            (type == MoveStepType.TURN_RIGHT) ||
                            (type == MoveStepType.LAUNCH) ||
                            (type == MoveStepType.UNDOCK))) {
                    return;
                }

                // unless velocity is zero ASFs must move forward one hex before
                // making turns in space
                if (!game.useVectorMove() &&
                      (distance == 0) &&
                      (velocity != 0) &&
                      ((type == MoveStepType.TURN_LEFT) || (type == MoveStepType.TURN_RIGHT))) {
                    return;
                }

                // no more than two turns in one hex unless velocity is zero for
                // anything except ASF in space
                if (!game.useVectorMove() && (a instanceof SmallCraft) && (velocity != 0) && (getNTurns() > 2)) {
                    return;
                }

                // for warships the limit is one
                if (!game.useVectorMove() && (a instanceof Jumpship) && (velocity != 0) && (getNTurns() > 1)) {
                    return;
                }

                // Jump ships cannot change velocity and use attitude jets in the same turn.
                if ((a instanceof Jumpship) &&
                      ((Jumpship) a).hasStationKeepingDrive() &&
                      (prev.getMovementType(false) == EntityMovementType.MOVE_OVER_THRUST) &&
                      ((type == MoveStepType.TURN_LEFT) || (type == MoveStepType.TURN_RIGHT))) {
                    return;
                }
            }

            // atmosphere has its own rules about turning
            if (useAeroAtmosphere(game, entity) &&
                  ((type == MoveStepType.TURN_LEFT) || (type == MoveStepType.TURN_RIGHT)) &&
                  !prev.canAeroTurn(game)) {
                return;
            }

            // spheroids in atmosphere can move a max of 1 hex on the low atmosphere map and 8 hexes on the ground
            // map, regardless of any other considerations unless they're out of control, in which case, well...
            if (useSpheroidAtmosphere(game, entity) &&
                  (((IAero) entity).isOutControlTotal() ||
                        (!game.getBoard(boardId).isGround() && (this.getDistance() > 1) ||
                              (game.getBoard(boardId).isGround() && (getDistance() > 8))))) {
                return;
            }

            if ((type == MoveStepType.FORWARDS) && game.getBoard(boardId).isLowAltitude() && !a.isOutControl()) {
                Hex destinationHex = game.getBoard(boardId).getHex(getPosition());
                if (altitude <= destinationHex.ceiling(true)) {
                    // can't fly into a cliff face or woods (unless out of control)
                    return;
                }
            }

            /*
             * TODO: better to disable this in movement display // don't let them
             * evade more than once if (type == MoveStepType.EVADE) {
             * if (isEvading) { return; } else { setEvading(true); } }
             */

            // check for thruster damage
            if ((type == MoveStepType.TURN_LEFT) &&
                  (a.getRightThrustHits() > 2) &&
                  !useSpheroidAtmosphere(game, entity)) {
                return;
            }
            if ((type == MoveStepType.TURN_RIGHT) &&
                  (a.getLeftThrustHits() > 2) &&
                  !useSpheroidAtmosphere(game, entity)) {
                return;
            }

            // no moves after launching fighters, unless we were undocking
            if (!isFirstStep() && (prev.getType() == MoveStepType.LAUNCH) && (getType() != MoveStepType.UNDOCK)) {
                return;
            }

            // no moves after launching dropships, unless we are launching
            if (!isFirstStep() && (prev.getType() == MoveStepType.UNDOCK) && (getType() != MoveStepType.LAUNCH)) {
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
            if (!isFirstStep() && ((prev.getType() == MoveStepType.LAND) || (prev.getType()
                  == MoveStepType.VERTICAL_LAND))) {
                return;
            }

            // can only use safe thrust when ammo (or bomb) dumping
            // (unless out of control?)
            boolean bDumping = false;// a.isDumpingBombs();
            for (Mounted<?> mo : entity.getAmmo()) {
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
                // when Aerospace are flying on the ground map sheet we need an additional check because velocityLeft
                // is only decremented at intervals of 16 hexes
                if (useAeroAtmosphere(game, entity) &&
                      game.getBoard(boardId).isGround() &&
                      (getVelocityLeft() == 0) &&
                      (getNMoved() > 0)) {
                    return;
                }
                if (getMpUsed() <= tmpSafeTh) {
                    movementType = EntityMovementType.MOVE_SAFE_THRUST;
                } else if (getMpUsed() <= cachedEntityState.getRunMPWithoutMASC()) {
                    movementType = EntityMovementType.MOVE_OVER_THRUST;
                } else if (a.isRandomMove()) {
                    // if random move then allow it to be over thrust allowance
                    movementType = EntityMovementType.MOVE_OVER_THRUST;
                }
            }

            return;
        } // end AERO stuff

        if (isInfantry && isJumping() && stepType == MoveStepType.DOWN) {
            if (game.getBoard(boardId).getHex(curPos).containsTerrain(Terrains.BUILDING)) {
                Coords startingPosition = entity.getPosition();
                Coords adjacentCoords = curPos.translated(curPos.direction(startingPosition));
                Hex adjacentHex = game.getHex(adjacentCoords, boardId);

                boolean hasLOS = LosEffects.calculateLOS(game,
                      entity,
                      new FloorTarget(curPos, game.getBoard(boardId), getElevation())).canSee();

                if (adjacentHex.ceiling() >= getElevation() || !hasLOS) {
                    return; // can't enter the building from this direction
                } else {
                    // we can enter the building, but we need to roll anti-mek skill
                    danger = true;
                }
            }
        }

        if (prev.isDiggingIn) {
            isDiggingIn = true;
            if ((type != MoveStepType.TURN_LEFT) && (type != MoveStepType.TURN_RIGHT)) {
                return; // can't move when digging in
            }
            movementType = EntityMovementType.MOVE_NONE;
        } else if ((type == MoveStepType.DIG_IN) || (type == MoveStepType.FORTIFY)) {
            if ((!isInfantry && !isTank) || !isFirstStep()) {
                return; // can't dig in
            }

            if (isInfantry) {
                Infantry inf = (Infantry) entity;
                if ((inf.getDugIn() != Infantry.DUG_IN_NONE) && (inf.getDugIn() != Infantry.DUG_IN_COMPLETE)) {
                    return; // Already dug in
                }
            }

            if (game.getBoard(boardId).getHex(curPos).containsTerrain(Terrains.PAVEMENT) ||
                  game.getBoard(boardId).getHex(curPos).containsTerrain(Terrains.FORTIFIED) ||
                  game.getBoard(boardId).getHex(curPos).containsTerrain(Terrains.BUILDING) ||
                  game.getBoard(boardId).getHex(curPos).containsTerrain(Terrains.ROAD)) {
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
        if ((type == MoveStepType.UP) &&
              (entity.getMovementMode() == EntityMovementMode.WIGE) &&
              (prev.getClearance() == 0)) {
            if (firstStep && (cachedEntityState.getRunMP() >= mp)) {
                movementType = EntityMovementType.MOVE_VTOL_WALK;
            } else {
                movementType = EntityMovementType.MOVE_ILLEGAL;
                return;
            }
        }

        // WIGEs need to be able to land too, or even descend
        if (entity.getMovementMode() == EntityMovementMode.WIGE &&
              type == MoveStepType.DOWN &&
              getClearance() < prev.getClearance()) { // landing
            if (prev.getMovementType(false) == EntityMovementType.MOVE_VTOL_RUN ||
                  prev.getMovementType(false) == EntityMovementType.MOVE_VTOL_SPRINT) {
                movementType = prev.getMovementType(false);
            } else {
                movementType = EntityMovementType.MOVE_VTOL_WALK;
            }
        }

        // check to see if it's trying to flee and can legally do so.
        if ((type == MoveStepType.FLEE) && entity.canFlee(curPos)) {
            movementType = EntityMovementType.MOVE_LEGAL;
        }

        if ((type == MoveStepType.CLIMB_MODE_ON) || (type == MoveStepType.CLIMB_MODE_OFF)) {
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
        if ((type == MoveStepType.CLEAR_MINEFIELD) && (entity instanceof Infantry)) {
            movementType = EntityMovementType.MOVE_NONE;
        }
        // check for evasion
        if (type == MoveStepType.EVADE) {
            if (entity.hasHipCrit() ||
                  (entity.getMovementMode() == EntityMovementMode.WIGE &&
                        (entity instanceof LandAirMek || entity instanceof ProtoMek) &&
                        getClearance() > 0)) {
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
        if (isJumping() &&
              (getMpUsed() <= getAvailableJumpMP(entity)) &&
              !isProne() &&
              !isHullDown() &&
              !((entity instanceof ProtoMek) &&
                    (entity.getInternal(ProtoMek.LOC_LEG) == IArmorState.ARMOR_DESTROYED)) &&
              (!entity.isStuck() || entity.canUnstickByJumping())) {

            movementType = EntityMovementType.MOVE_JUMP;
        }

        // legged ProtoMeks may make one facing change
        if (isFirstStep() &&
              (entity instanceof ProtoMek) &&
              (entity.getInternal(ProtoMek.LOC_LEG) == IArmorState.ARMOR_DESTROYED) &&
              ((stepType == MoveStepType.TURN_LEFT) || (stepType == MoveStepType.TURN_RIGHT)) &&
              !entity.isStuck()) {
            movementType = EntityMovementType.MOVE_WALK;
        }
        // Infantry that is first stepping and turning is legal
        if (isInfantry &&
              ((stepType == MoveStepType.TURN_LEFT) || (stepType == MoveStepType.TURN_RIGHT)) &&
              isFirstStep()) {
            if (isJumping()) {
                movementType = EntityMovementType.MOVE_JUMP;
            } else {
                movementType = EntityMovementType.MOVE_WALK;
            }
        }

        int bonus = wigeBonus;
        entity.wigeBonus = wigeBonus;
        if (entity.isEligibleForPavementOrRoadBonus() && isOnlyPavementOrRoad()) {
            bonus++;
            entity.gotPavementOrRoadBonus = true;
        }
        int tmpWalkMP = cachedEntityState.getWalkMP() + bonus;

        // For entities with neither MASC nor Supercharger, these values will be the
        // same
        int runMPMax = cachedEntityState.getRunMP() + bonus;
        int runMPSingleBoost = cachedEntityState.getRunMPWithOneMASC() + bonus;
        int runMPNoBoost = cachedEntityState.getRunMPWithoutMASC() + bonus;

        // Sprint MP is calculated depending on the entity type
        // For those that cannot sprint, it is the same as run.
        // For those that can, it is the maximum distance it can sprint
        // For entities with neither MASC nor Supercharger, these values will be the
        // same
        int sprintMPMax = cachedEntityState.getSprintMP() + bonus;
        int sprintMPSingleBoost = cachedEntityState.getSprintMPWithOneMASC() + bonus;
        int sprintMPNoBoost = cachedEntityState.getSprintMPWithoutMASC() + bonus;

        // Have these been used already this turn. If so, they do not require a recheck against PSR. This can happen
        // as a result of interrupted turns due to failed PSRs, pointblank shots, etc.
        final boolean hasMASCBeenUsed = entity.isMASCUsed();
        final boolean hasSuperchargerBeenUsed = entity.isSuperchargerUsed();

        final boolean hasPoorPerformance = entity.hasQuirk(OptionsConstants.QUIRK_NEG_POOR_PERFORMANCE);

        // WiGEs, AirMeks, and glider ProtoMeks have different MP for ground and
        // airborne movement
        if (entity.getMovementMode() == EntityMovementMode.WIGE) {
            if (getClearance() <= 0 && type != MoveStepType.UP) {
                if (entity instanceof LandAirMek landAirMek) {
                    // On the ground or underwater use AirMek walk/run. Sprint can only be used on the ground, so
                    // that is already set.
                    tmpWalkMP = landAirMek.getAirMekWalkMP();
                    runMPNoBoost = landAirMek.getAirMekRunMP();

                    // LAMs cannot use hardened armor, which makes runMP a simpler calculation.
                    MPBoosters mpBoosters = landAirMek.getArmedMPBoosters();

                    if (!mpBoosters.isNone()) {
                        runMPMax = mpBoosters.calculateRunMP(tmpWalkMP);
                    } else {
                        runMPMax = runMPNoBoost;
                    }
                } else {
                    // Only 1 ground MP for ground effect vehicles and glider ProtoMeks
                    tmpWalkMP = runMPMax = runMPNoBoost = sprintMPMax = sprintMPNoBoost = 1;
                }
            } else if (entity instanceof LandAirMek) {
                // LAMs cannot use overdrive and MASC does not affect airborne MP.
                tmpWalkMP = ((LandAirMek) entity).getAirMekCruiseMP();
                runMPMax = runMPNoBoost = sprintMPMax = sprintMPNoBoost = ((LandAirMek) entity).getAirMekFlankMP();
            }
        }

        Hex currHex = game.getBoard(boardId).getHex(curPos);
        Hex lastHex = game.getBoard(boardId).getHex(lastPos);

        // Bootlegger ends movement
        if (prev.type == MoveStepType.BOOTLEGGER) {
            movementType = EntityMovementType.MOVE_ILLEGAL;
            return;
        }

        if (stepType == MoveStepType.CONVERT_MODE) {
            // QuadVees and LAMs cannot convert in water, and Mek tracks cannot be used in
            // water.
            if (currHex.containsTerrain(Terrains.WATER) && getClearance() < 0) {
                movementType = EntityMovementType.MOVE_ILLEGAL;
            }
            // QuadVees and LAMs cannot convert while prone. Meks with tracks don't actually
            // convert,
            // and can switch to track mode while prone then stand.
            if (getEntity().isProne() && (getEntity() instanceof QuadVee || getEntity() instanceof LandAirMek)) {
                movementType = EntityMovementType.MOVE_ILLEGAL;
            }
            // Illegal LAM conversions due to damage have to be determined by entire path,
            // because
            // some conversions take two convert steps and can be legal even though the
            // first one
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

        if ((getEntity().getMovementMode() == EntityMovementMode.INF_UMU) &&
              (currHex.containsTerrain(Terrains.WATER) &&
                    lastHex.containsTerrain(Terrains.WATER) &&
                    (entity.relHeight() < currHex.getLevel()))) {
            tmpWalkMP = entity.getActiveUMUCount();
        }

        if ((getEntity().getMovementMode() == EntityMovementMode.BIPED_SWIM) ||
              (getEntity().getMovementMode() == EntityMovementMode.QUAD_SWIM) ||
              ((getEntity() instanceof Infantry &&
                    getEntity().getMovementMode().isSubmarine() &&
                    (currHex.terrainLevel(Terrains.WATER) > 0)))) {
            tmpWalkMP = entity.getActiveUMUCount();
        }

        if ((getEntity().getMovementMode() == EntityMovementMode.VTOL) &&
              getClearance() > 0 &&
              !(getEntity() instanceof VTOL)) {
            tmpWalkMP = entity.getJumpMP();
        }

        // check for valid walk/run mp; BRACE is a special case for ProtoMeks
        if (!isJumping() &&
              !entity.isStuck() &&
              (tmpWalkMP > 0) &&
              ((getMp() > 0) || (stepType == MoveStepType.BRACE))) {
            // Prone meks can only spend MP to turn or get up
            if ((stepType != MoveStepType.TURN_LEFT) &&
                  (stepType != MoveStepType.TURN_RIGHT) &&
                  (stepType != MoveStepType.GET_UP) &&
                  (stepType != MoveStepType.LOAD) &&
                  (stepType != MoveStepType.CAREFUL_STAND) &&
                  (stepType != MoveStepType.HULL_DOWN) &&
                  (stepType != MoveStepType.GO_PRONE) &&
                  (stepType != MoveStepType.DROP_CARGO) &&
                  !(entity instanceof Tank)
                  // Tanks can drive out of hull-down
                  &&
                  (isProne() || isHullDown())) {
                movementType = EntityMovementType.MOVE_ILLEGAL;
                return;
            }

            // WiGEs that land is finished with movement
            if (entity.getMovementMode() == EntityMovementMode.WIGE &&
                  prev.getType() == MoveStepType.DOWN &&
                  getClearance() == 0) {
                movementType = EntityMovementType.MOVE_ILLEGAL;
                return;
            }

            if (getMpUsed() <= tmpWalkMP) {
                if ((getEntity().getMovementMode() == EntityMovementMode.VTOL ||
                      getEntity().getMovementMode() == EntityMovementMode.WIGE) && getClearance() > 0) {
                    movementType = EntityMovementType.MOVE_VTOL_WALK;
                } else if ((getEntity().getMovementMode() == EntityMovementMode.SUBMARINE) && getElevation() < 0) {
                    movementType = EntityMovementType.MOVE_SUBMARINE_WALK;
                } else {
                    movementType = EntityMovementType.MOVE_WALK;
                    // Vehicles moving along pavement get "road bonus" of 1 MP.
                    // N.B. The Ask Precentor Martial forum said that a 4/6
                    // tank on a road can move 5/7, **not** 5/8.
                }
            } else if ((entity instanceof Infantry) &&
                  (curPos.distance(entity.getPosition()) == 1) &&
                  (lastPos.equals(entity.getPosition()))) {
                // This ensures that Infantry always get their minimum 1 hex movement when TO fast infantry movement
                // is on. A MovePath that consists of a single step from one hex to the next should always be a walk,
                // since it's covered under the infantry's 1 free movement
                if ((getEntity().getMovementMode() == EntityMovementMode.VTOL) && getClearance() > 0) {
                    movementType = EntityMovementType.MOVE_VTOL_WALK;
                } else {
                    movementType = EntityMovementType.MOVE_WALK;
                }
            } else if (hasPoorPerformance && (entity.getMpUsedLastRound() < cachedEntityState.getWalkMP())) {
                // Poor performance requires spending all walk MP in the
                // previous round in order to move faster than a walk
                movementType = EntityMovementType.MOVE_ILLEGAL;
                return;
            } else if (getMpUsed() <= runMPMax && isRunAllowed()) {
                // RUN - If we got this far, entity is moving farther than a walk
                // but within run and running is legal

                if (getMpUsed() > runMPNoBoost) {
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

                if ((entity.getMovementMode() == EntityMovementMode.VTOL ||
                      entity.getMovementMode() == EntityMovementMode.WIGE) && getClearance() > 0) {
                    movementType = EntityMovementType.MOVE_VTOL_RUN;
                } else if ((entity.getMovementMode() == EntityMovementMode.SUBMARINE) && getElevation() < 0) {
                    movementType = EntityMovementType.MOVE_SUBMARINE_RUN;
                } else {
                    movementType = EntityMovementType.MOVE_RUN;
                }
            } else if ((getMpUsed() <= sprintMPMax) && isRunAllowed() && !isEvading() && canUseSprint(game)) {
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

                if (entity.getMovementMode() == EntityMovementMode.VTOL ||
                      (entity.getMovementMode() == EntityMovementMode.WIGE && getClearance() > 0)) {
                    movementType = EntityMovementType.MOVE_VTOL_SPRINT;
                } else {
                    movementType = EntityMovementType.MOVE_SPRINT;
                }
            }
        }

        // Submarines at seafloor cannot move horizontally or change facing (TW p.56)
        // They can only ascend vertically
        if (entity.getMovementMode() == EntityMovementMode.SUBMARINE) {
            final Hex prevHex = game.getBoard(boardId).getHex(prev.getPosition());
            if ((prev.getElevation() == -prevHex.depth()) && (type != MoveStepType.UP)) {
                movementType = EntityMovementType.MOVE_ILLEGAL;
                return;
            }
        }

        // If using vehicle acceleration restrictions, it is impossible to go from a
        // stop to overdrive.
        // Stop to flank or cruise to overdrive is permitted with a driving check
        // ("gunning it").
        if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_GROUND_MOVEMENT_VEHICLE_ACCELERATION) &&
              movementType == EntityMovementType.MOVE_SPRINT &&
              (entity instanceof Tank ||
                    (entity instanceof QuadVee && entity.getConversionMode() == QuadVee.CONV_MODE_VEHICLE)) &&
              (entity.movedLastRound == EntityMovementType.MOVE_NONE ||
                    entity.movedLastRound == EntityMovementType.MOVE_SKID ||
                    entity.movedLastRound == EntityMovementType.MOVE_JUMP)) {
            movementType = EntityMovementType.MOVE_ILLEGAL;
        }
        // 0 MP infantry units can move 1 hex
        if (isInfantry &&
              (cachedEntityState.getWalkMP() == 0) &&
              getEntity().getPosition().equals(prev.getPosition()) &&
              (prev.getElevation() == entity.getElevation()) &&
              (getEntity().getPosition().distance(getPosition()) <= 1) &&
              (Math.abs(entity.getElevation() - getElevation()) <= entity.getMaxElevationChange()) &&
              (movementType != EntityMovementType.MOVE_JUMP)) {
            movementType = EntityMovementType.MOVE_WALK;
        }

        // Free facing changes are legal
        if (((stepType == MoveStepType.TURN_LEFT) || (stepType == MoveStepType.TURN_RIGHT)) && (getMp() == 0)) {
            movementType = prev.movementType;
        }

        // Mechanical Jump Boosters don't allow facing changes
        if (isJumping() &&
              isUsingMekJumpBooster &&
              ((stepType == MoveStepType.TURN_LEFT) || (stepType == MoveStepType.TURN_RIGHT))) {
            movementType = EntityMovementType.MOVE_ILLEGAL;
        }

        // going prone from hull down is legal and costs 0
        if ((getMp() == 0) && (stepType == MoveStepType.GO_PRONE) && isHullDown()) {
            movementType = prev.movementType;
        }

        if ((movementType == EntityMovementType.MOVE_WALK) && (prev.movementType == EntityMovementType.MOVE_RUN)) {
            movementType = EntityMovementType.MOVE_RUN;
        } else if ((movementType == EntityMovementType.MOVE_VTOL_WALK) &&
              (prev.movementType == EntityMovementType.MOVE_VTOL_RUN)) {
            movementType = EntityMovementType.MOVE_VTOL_RUN;
        } else if (((movementType == EntityMovementType.MOVE_WALK) || (movementType == EntityMovementType.MOVE_RUN)) &&
              (prev.movementType == EntityMovementType.MOVE_SPRINT)) {
            movementType = EntityMovementType.MOVE_SPRINT;
        } else if (((movementType == EntityMovementType.MOVE_VTOL_WALK) ||
              (movementType == EntityMovementType.MOVE_VTOL_RUN)) &&
              (prev.movementType == EntityMovementType.MOVE_VTOL_SPRINT)) {
            movementType = EntityMovementType.MOVE_VTOL_SPRINT;
        }

        if (entity.isGyroDestroyed() &&
              !((entity instanceof LandAirMek) && (entity.getConversionMode() == LandAirMek.CONV_MODE_FIGHTER))) {
            // A prone `Mek with a destroyed gyro can only change a single hex side, or eject
            if (entity.isProne()) {
                if (((stepType != MoveStepType.TURN_LEFT && stepType != MoveStepType.TURN_RIGHT) || getMpUsed() > 1) &&
                      stepType != MoveStepType.EJECT) {
                    movementType = EntityMovementType.MOVE_ILLEGAL;
                }
            } else {
                // Normally a `Mek falls immediately when the gyro is destroyed and can't stand again. QuadVees using
                // vehicle mode and `Meks using tracks do not fall and can continue to stand, but cannot use
                // non-tracked/wheeled MP except for a QuadVee converting back to vehicle mode. This also covers a
                // `Mek that started with a destroyed gyro but was not set to deploy prone. Perhaps that should not
                // be allowed.
                if (getMp() > 0) {
                    boolean isTracked = entity.getMovementMode() == EntityMovementMode.TRACKED ||
                          entity.getMovementMode() == EntityMovementMode.WHEELED;
                    if (entity instanceof QuadVee) {
                        // We are in `Mek/non-tracked mode if the end mode is vee, and we are converting of the end
                        // mode is `Mek, and we are not converting.
                        if (isTracked == entity.isConvertingNow() && stepType != MoveStepType.CONVERT_MODE) {
                            movementType = EntityMovementType.MOVE_ILLEGAL;
                        }
                    } else if (!isTracked) {
                        // Non QuadVee tracked 'Meks don't actually convert. They just go, so we only need to know
                        // the end mode.
                        movementType = EntityMovementType.MOVE_ILLEGAL;
                    }
                }
            }
        }

        // Meks with no arms and a missing leg cannot attempt to stand
        if (((stepType == MoveStepType.GET_UP) || (stepType == MoveStepType.CAREFUL_STAND)) &&
              (entity instanceof Mek) &&
              entity.isLocationBad(Mek.LOC_LEFT_ARM) &&
              entity.isLocationBad(Mek.LOC_RIGHT_ARM) &&
              (entity.isLocationBad(Mek.LOC_RIGHT_LEG) || entity.isLocationBad(Mek.LOC_LEFT_LEG))) {
            movementType = EntityMovementType.MOVE_ILLEGAL;
            return;
        }

        // Meks with 1 MP are allowed to get up, except
        // if they've used that 1MP up already
        if ((MoveStepType.GET_UP == stepType) &&
              (1 == cachedEntityState.getRunMP()) &&
              (entity.mpUsed < 1) &&
              !entity.isStuck()) {
            movementType = EntityMovementType.MOVE_RUN;
        }

        if ((MoveStepType.CAREFUL_STAND == stepType) && (entity.mpUsed > 1)) {
            movementType = EntityMovementType.MOVE_ILLEGAL;
        }

        if (isFirstStep() && ((stepType == MoveStepType.TAKEOFF) || (stepType == MoveStepType.VERTICAL_TAKE_OFF))) {
            movementType = EntityMovementType.MOVE_SAFE_THRUST;
        } else

            // VTOLs with a damaged flight stabiliser can't flank
            if ((entity instanceof VTOL) &&
                  (movementType == EntityMovementType.MOVE_VTOL_RUN ||
                        movementType == EntityMovementType.MOVE_VTOL_SPRINT) &&
                  ((VTOL) entity).isStabiliserHit(VTOL.LOC_ROTOR)) {
                movementType = EntityMovementType.MOVE_ILLEGAL;
            }

        // check for UMU infantry on land
        if ((entity.getMovementMode() == EntityMovementMode.INF_UMU) &&
              !game.getBoard(boardId).getHex(curPos).containsTerrain(Terrains.WATER) &&
              (movementType == EntityMovementType.MOVE_RUN)) {
            movementType = EntityMovementType.MOVE_ILLEGAL;
        }

        // amnesty for the first step
        if (isFirstStep() &&
              (movementType == EntityMovementType.MOVE_ILLEGAL) &&
              (cachedEntityState.getWalkMP() > 0) &&
              !entity.isProne() &&
              !entity.isHullDown() &&
              !entity.isStuck() &&
              !entity.isGyroDestroyed() &&
              (stepType == MoveStepType.FORWARDS)) {
            movementType = EntityMovementType.MOVE_RUN;
        }

        // Bimodal LAMs cannot spend MP when converting to fighter mode on the ground.
        if (entity instanceof LandAirMek &&
              ((LandAirMek) entity).getLAMType() == LandAirMek.LAM_BIMODAL &&
              entity.getConversionMode() == LandAirMek.CONV_MODE_MEK &&
              movementMode == EntityMovementMode.AERODYNE &&
              altitude == 0 &&
              mp > 0) {
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
                if ((getMpUsed() <= cachedEntityState.getRunMP()) &&
                      (entity.isProne() || entity.isHullDown()) &&
                      (movementType == EntityMovementType.MOVE_ILLEGAL)) {
                    movementType = EntityMovementType.MOVE_RUN;
                    if (getMpUsed() <= cachedEntityState.getWalkMP()) {
                        movementType = EntityMovementType.MOVE_WALK;
                    }
                }

                // Can't unload units into prohibited terrain
                // or into stacking violation.
                Targetable target = getTarget(game);
                if (target instanceof Entity other) {
                    // Change the destination hex if an unload dialog box set it elsewhere
                    if (getTargetPosition() != null) {
                        curPos = getTargetPosition();
                    }
                    if ((null != Compute.stackingViolation(game, other, curPos, entity, climbMode, true)) ||
                          other.isLocationProhibited(curPos, getElevation())) {
                        movementType = EntityMovementType.MOVE_ILLEGAL;
                    }
                } else {
                    movementType = EntityMovementType.MOVE_ILLEGAL;
                }
            }
        }

        // Handle loading steps for various unit types.
        // Eventually MoveStepType.LOAD will get a target and position passed to it, so we will always load the right
        // entities in the right order, and from the right place.
        if (stepType == MoveStepType.LOAD) {
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
            }
        }

        // Is the entity trying to drop a trailer?
        if (stepType == MoveStepType.DISCONNECT) {

            // If this isn't the first step, trailer position isn't updated by
            // Server.processTrailerMovement()
            // before this step, so they don't drop off in the right place
            if (!isFirstStep()) {
                movementType = EntityMovementType.MOVE_ILLEGAL;
            } else {
                movementType = EntityMovementType.MOVE_WALK;
            }

            // Can't unload units into prohibited terrain
            // or into stacking violation.
            Targetable target = getTarget(game);
            if (target instanceof Entity other) {
                if ((null != Compute.stackingViolation(game, other, curPos, entity, climbMode, true)) ||
                      other.isLocationProhibited(curPos, getElevation())) {
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
                // And it's always considered to be flank movement
                if (entity.getMovementMode() == EntityMovementMode.VTOL) {
                    movementType = EntityMovementType.MOVE_VTOL_RUN;
                } else {
                    movementType = EntityMovementType.MOVE_RUN;
                }
            }
        }

        // Can't run or jump if unjamming a RAC.
        if (isUnjammingRAC &&
              ((movementType == EntityMovementType.MOVE_RUN) ||
                    (movementType == EntityMovementType.MOVE_SPRINT) ||
                    (movementType == EntityMovementType.MOVE_VTOL_RUN ||
                          (movementType == EntityMovementType.MOVE_VTOL_SPRINT)) ||
                    isJumping())) {
            movementType = EntityMovementType.MOVE_ILLEGAL;
        }

        // only standing meks may go prone
        if ((stepType == MoveStepType.GO_PRONE) && (isProne() || !(entity instanceof Mek) || entity.isStuck())) {
            movementType = EntityMovementType.MOVE_ILLEGAL;
        }

        // Standing meks and vehicles in fortified terrain can hull-down
        if (stepType == MoveStepType.HULL_DOWN) {
            if ((isHullDown() || !((entity instanceof Mek) || (entity instanceof Tank)) || entity.isStuck())) {
                movementType = EntityMovementType.MOVE_ILLEGAL;
            }
            if (entity instanceof Tank ||
                  (entity instanceof QuadVee &&
                        ((entity.getConversionMode() == QuadVee.CONV_MODE_VEHICLE) != entity.isConvertingNow()))) {
                // Tanks and QuadVees ending movement in vehicle mode require a fortified hex.
                if (!(game.getBoard(boardId).getHex(curPos).containsTerrain(Terrains.FORTIFIED))) {
                    movementType = EntityMovementType.MOVE_ILLEGAL;
                }
            } else if (entity.isGyroDestroyed()) {
                // Meks need to check for valid Gyros
                movementType = EntityMovementType.MOVE_ILLEGAL;
            }
        }

        // initially prone meks can't charge
        if (((stepType == MoveStepType.CHARGE) || (stepType == MoveStepType.DFA)) && entity.isProne()) {
            movementType = EntityMovementType.MOVE_ILLEGAL;
        }

        // do not allow to move onto a bridge if there's no exit in lastPos's
        // direction, unless jumping or already at/above bridge level
        if (!isFirstStep() &&
              !curPos.equals(lastPos) &&
              climbMode &&
              entity.getMovementMode() != EntityMovementMode.VTOL &&
              (entity.getMovementMode() != EntityMovementMode.WIGE || getClearance() == 0) &&
              (movementType != EntityMovementType.MOVE_JUMP) &&
              game.getBoard(boardId).getHex(curPos).containsTerrain(Terrains.BRIDGE) &&
              !game.getBoard(boardId).getHex(curPos).containsTerrainExit(Terrains.BRIDGE, curPos.direction(lastPos)) &&
              (getElevation() < game.getBoard(boardId).getHex(curPos).terrainLevel(Terrains.BRIDGE_ELEV)) &&
              (getElevation() + entity.getHeight() >=
                    game.getBoard(boardId).getHex(curPos).terrainLevel(Terrains.BRIDGE_ELEV))) {
            movementType = EntityMovementType.MOVE_ILLEGAL;
        }

        // Walking under a bridge: check if entity fits (TO:AR 115)
        // If entity is under the bridge and can't fit, move is illegal (unless jumping)
        if (!isFirstStep() &&
              !curPos.equals(lastPos) &&
              (movementType != EntityMovementType.MOVE_JUMP) &&
              game.getBoard(boardId).getHex(curPos).containsTerrain(Terrains.BRIDGE) &&
              (getElevation() < game.getBoard(boardId).getHex(curPos).terrainLevel(Terrains.BRIDGE_ELEV)) &&
              (getElevation() + entity.getHeight() >=
                    game.getBoard(boardId).getHex(curPos).terrainLevel(Terrains.BRIDGE_ELEV))) {
            movementType = EntityMovementType.MOVE_ILLEGAL;
        }

        // super heavy meks can't climb on buildings
        if ((entity instanceof Mek mek) &&
              mek.isSuperHeavy() &&
              climbMode &&
              game.getBoard(boardId).getHex(curPos).containsTerrain(Terrains.BUILDING)) {
            movementType = EntityMovementType.MOVE_ILLEGAL;
        }

        // Check elevation change when climbing onto a building
        if (climbMode && !isJumping()) {
            Hex curHex = game.getBoard(boardId).getHex(curPos);
            if (curHex.containsTerrain(Terrains.BUILDING)) {
                Hex prevHex = game.getBoard(boardId).getHex(lastPos);
                // Check if we're climbing from outside/below onto the building top
                if (!prevHex.containsTerrain(Terrains.BUILDING) ||
                    prevHex.terrainLevel(Terrains.BLDG_ELEV) < curHex.terrainLevel(Terrains.BLDG_ELEV)) {
                    int prevAbsoluteElev = prevHex.getLevel() + prev.getElevation();
                    int curAbsoluteElev = curHex.getLevel() + getElevation();
                    int elevChange = curAbsoluteElev - prevAbsoluteElev;

                    int maxAllowed = entity.getMaxElevationChange();
                    // Infantry changing terrain levels can exceed their normal max by 1
                    if (entity instanceof Infantry && curHex.getLevel() != prevHex.getLevel()) {
                        maxAllowed += 1;
                    }

                    if (elevChange > maxAllowed) {
                        movementType = EntityMovementType.MOVE_ILLEGAL;
                    }
                }
            }
        }

        // TO p.325 - Mine dispensers
        if ((type == MoveStepType.LAY_MINE) && !entity.canLayMine()) {
            movementType = EntityMovementType.MOVE_ILLEGAL;
            return;
        }

        if ((type == MoveStepType.LAY_MINE) && entity.canLayMine()) {
            // All units may only lay mines on its first or last step.
            // BA additionally have to use Jump or VTOL movement.
            movementType = prev.movementType;

            if (entity instanceof BattleArmor &&
                  !(isFirstStep() ||
                        (prev.movementType == EntityMovementType.MOVE_JUMP) ||
                        (prev.movementType == EntityMovementType.MOVE_VTOL_RUN) ||
                        (prev.movementType == EntityMovementType.MOVE_VTOL_WALK))) {
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
            // Bootlegger requires three hexes straight and is illegal for tracked, WiGE, or
            // naval.
            if (prev.nStraight < 3 ||
                  (entity.getMovementMode() != EntityMovementMode.WHEELED &&
                        entity.getMovementMode() != EntityMovementMode.HOVER &&
                        entity.getMovementMode() != EntityMovementMode.VTOL)) {
                movementType = EntityMovementType.MOVE_ILLEGAL;
            } else {
                danger = true;
            }
        }

        if (stepType == MoveStepType.PICKUP_CARGO || stepType == MoveStepType.DROP_CARGO) {
            movementType = EntityMovementType.MOVE_NONE;
        }

        // check if this movement is illegal for reasons other than points
        // Only a CHAFF step or another unloading step can follow an existing unloading step
        if (!isMovementPossible(game, lastPos, prev.getElevation(), cachedEntityState) ||
              (isUnloaded && !(type == MoveStepType.CHAFF || type == MoveStepType.UNLOAD))
        ) {
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

        // Danger is flagged for PSR checks by entire path when a new step is added,
        // since turning
        // while running on pavement does cannot trigger the danger flag if the turn
        // occurs before
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

        // only walking speed in Tornado's
        PlanetaryConditions conditions = game.getPlanetaryConditions();
        if (conditions.getWind().isTornadoF4()) {
            if (getMpUsed() > tmpWalkMP) {
                movementType = EntityMovementType.MOVE_ILLEGAL;
                return;
            }
        }

        // Vehicles carrying mechanized BA can't jump, VTOL, or WiGE
        if ((entity instanceof Tank) && !entity.getExternalUnits().isEmpty()) {
            if ((movementType == EntityMovementType.MOVE_JUMP) ||
                  (movementType == EntityMovementType.MOVE_VTOL_WALK) ||
                  (movementType == EntityMovementType.MOVE_VTOL_RUN) ||
                  (movementType == EntityMovementType.MOVE_VTOL_SPRINT) ||
                  ((entity.getMovementMode() == EntityMovementMode.WIGE) && getClearance() > 0)) {
                movementType = EntityMovementType.MOVE_ILLEGAL;
            }
        }
    }

    /**
     * If the entity has both, choose the one with the lower risk, or Supercharger if they are even Require a PSR if it
     * has not been done in an earlier part of the move
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

    public int getHeat() {
        return heat;
    }

    /**
     * Amount of movement points required to move from start to dest
     */
    protected void calcMovementCostFor(Game game, MoveStep prevStep, CachedEntityState cachedEntityState) {
        final Coords prev = prevStep.getPosition();
        final int prevEl = prevStep.getElevation();
        final EntityMovementMode moveMode = getEntity().getMovementMode();
        final Entity en = getEntity();
        final Hex srcHex = game.getBoard(boardId).getHex(prev);
        final Hex destHex = game.getBoard(boardId).getHex(getPosition());
        final boolean isInfantry = getEntity() instanceof Infantry;
        final boolean isSuperHeavyMek = (getEntity() instanceof Mek mek) && mek.isSuperHeavy();
        final boolean isMechanizedInfantry = isInfantry && ((Infantry) getEntity()).isMechanized();
        final boolean isProto = getEntity() instanceof ProtoMek;
        final boolean isMek = getEntity() instanceof Mek;
        final boolean isAmphibious = cachedEntityState.hasWorkingMisc(MiscType.F_FULLY_AMPHIBIOUS) ||
              cachedEntityState.hasWorkingMisc(MiscType.F_LIMITED_AMPHIBIOUS);
        final boolean isFogSpecialist = en.getCrew()
              .getOptions()
              .stringOption(OptionsConstants.MISC_ENV_SPECIALIST)
              .equals(Crew.ENVIRONMENT_SPECIALIST_FOG);
        final boolean isLightSpecialist = en.getCrew()
              .getOptions()
              .stringOption(OptionsConstants.MISC_ENV_SPECIALIST)
              .equals(Crew.ENVIRONMENT_SPECIALIST_LIGHT);
        int nSrcEl = srcHex.getLevel() + prevEl;
        int nDestEl = destHex.getLevel() + elevation;
        PlanetaryConditions conditions = game.getPlanetaryConditions();

        mp = 1;

        // 0 MP infantry units can move 1 hex
        if (isInfantry &&
              (cachedEntityState.getWalkMP() == 0) &&
              !moveMode.isSubmarine() &&
              !moveMode.isVTOL() &&
              getEntity().getPosition().equals(prev) &&
              (getEntity().getPosition().distance(getPosition()) == 1) &&
              (!isJumping())) {
            mp = 0;
            return;
        }

        boolean applyNightPen = !game.getOptions()
              .booleanOption(OptionsConstants.ADVANCED_GROUND_MOVEMENT_NO_NIGHT_MOVE_PEN);
        boolean carefulExempt = (moveMode == EntityMovementMode.VTOL) || isJumping();

        // Apply careful movement MP penalties for fog and light (TO pg 63)
        if (!game.getBoard(boardId).isSpace() && isCareful() && applyNightPen && !carefulExempt) {
            // Fog
            switch (conditions.getFog()) {
                case FOG_LIGHT:
                    if (!isFogSpecialist) {
                        mp += 1;
                    }
                    break;
                case FOG_HEAVY:
                    if (!isFogSpecialist) {
                        mp += 2;
                    } else {
                        mp += 1;
                    }
                    break;
                default:
            }

            // Light TO:AR 6th ed. p. 34
            if (!entity.isNightwalker()) {
                switch (conditions.getLight()) {
                    case FULL_MOON:
                        if (!isLightSpecialist && !en.isUsingSearchlight()) {
                            mp += 1;
                        }
                        break;
                    case GLARE:
                        if (!isLightSpecialist) {
                            mp += 1;
                        }
                        break;
                    case MOONLESS:
                        if (en.isUsingSearchlight()) {
                            break;
                        }

                        if (!isLightSpecialist) {
                            mp += 2;
                        } else {
                            mp += 1;
                        }
                        break;
                    case SOLAR_FLARE:
                        if (!isLightSpecialist) {
                            mp += 2;
                        } else {
                            mp += 1;
                        }
                        break;
                    case PITCH_BLACK:
                        if (!isLightSpecialist) {
                            mp += 3;
                        } else {
                            mp += 1;
                        }
                        break;
                    default:
                }
            } else if (conditions.getLight().isFullMoonOrGlareOrMoonlessOrSolarFlareOrPitchBack()) {
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

        // Be careful on pavement during cold weather, there may be black ice.
        boolean useBlackIce = game.getOptions().booleanOption(OptionsConstants.ADVANCED_BLACK_ICE);
        boolean goodTemp = conditions.getTemperature() <= PlanetaryConditions.BLACK_ICE_TEMP;
        boolean goodWeather = conditions.getWeather().isIceStorm();

        if (isPavementStep && ((useBlackIce && goodTemp) || goodWeather)) {
            if (destHex.containsTerrain(Terrains.BLACK_ICE)) {
                mp++;
            }
            if (destHex.containsTerrain(Terrains.BLACK_ICE) && !isCareful() && (nDestEl == destHex.getLevel())) {
                mp--;
            }
            if (isPavementStep && !destHex.containsTerrain(Terrains.BLACK_ICE) && isCareful()) {
                mp++;
            }

        }

        // Account for terrain, unless we're moving along a road.
        if (!isPavementStep()) {

            if ((moveMode != EntityMovementMode.BIPED_SWIM) &&
                  (moveMode != EntityMovementMode.QUAD_SWIM) &&
                  getClearance() == 0) {
                mp += destHex.movementCost(getEntity());
            }

            // if this is an amphibious unit crossing water, increment movement cost by 1
            if (isAmphibious && !destHex.containsTerrain(Terrains.ICE) && (destHex.terrainLevel(Terrains.WATER) > 0)) {
                mp++;

                // this is kind of a hack, but only occurs when an amphibious unit passes over
                // mud at the bottom
                // of a body of water. We can't account for that in the hex's movement cost
                // function
                // because it doesn't have the ability to pretend the entity is at a particular
                // elevation
                if (destHex.containsTerrain(Terrains.MUD) && (destHex.floor() < nDestEl)) {
                    mp--;
                }
            }

            // non-hovers, non-naval and non-VTOLs check for water depth and
            // are affected by swamp
            if ((moveMode != EntityMovementMode.HOVER) &&
                  (moveMode != EntityMovementMode.NAVAL) &&
                  (moveMode != EntityMovementMode.HYDROFOIL) &&
                  (moveMode != EntityMovementMode.SUBMARINE) &&
                  (moveMode != EntityMovementMode.INF_UMU) &&
                  (moveMode != EntityMovementMode.VTOL) &&
                  (moveMode != EntityMovementMode.BIPED_SWIM) &&
                  (moveMode != EntityMovementMode.QUAD_SWIM) &&
                  (moveMode != EntityMovementMode.WIGE)) {
                // no additional cost when moving on surface of ice.
                if (!destHex.containsTerrain(Terrains.ICE) || (nDestEl < destHex.getLevel())) {
                    if ((destHex.terrainLevel(Terrains.WATER) == 1) && !isAmphibious) {
                        mp++;
                    } else if ((destHex.terrainLevel(Terrains.WATER) > 1) && !isAmphibious) {
                        if (getEntity().hasAbility(OptionsConstants.PILOT_TM_FROGMAN) &&
                              ((entity instanceof Mek) || (entity instanceof ProtoMek))) {
                            mp += 2;
                        } else {
                            // PLAYTEST2 Water changes - MP values
                            if (game.getOptions().booleanOption(OptionsConstants.PLAYTEST_2)) {
                                mp += 2;
                            } else {
                                mp += 3;
                            }
                        }
                    }
                }
                // if using non-careful movement on ice then reduce cost
                if (destHex.containsTerrain(Terrains.ICE) && !isCareful() && (nDestEl == destHex.getLevel())) {
                    mp--;
                }

            }
        } // End not-along-road

        // non-WIGEs pay for elevation differences
        if ((nSrcEl != nDestEl) && (moveMode != EntityMovementMode.WIGE)) {
            int delta_e = Math.abs(nSrcEl - nDestEl);
            if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_GROUND_MOVEMENT_TAC_OPS_LEAPING) &&
                  isMek &&
                  (delta_e > 2) &&
                  (nDestEl < nSrcEl)) {
                // leaping (moving down more than 2 hexes) always costs 4 mp
                // regardless of anything else
                mp = 4;
                return;
            }
            // non-flying Infantry and ground vehicles are charged double.
            if ((isInfantry &&
                  !((getMovementType(false) == EntityMovementType.MOVE_VTOL_WALK) ||
                        (getMovementType(false) == EntityMovementType.MOVE_VTOL_RUN))) ||
                  ((moveMode == EntityMovementMode.TRACKED) ||
                        (moveMode == EntityMovementMode.WHEELED) ||
                        (moveMode == EntityMovementMode.HOVER))) {
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
        // If the destination contains a building, the WiGE must pay the extra MP if
        // flying
        // more than one elevation above its top or if climbing a level to get above it.
        // See
        // http://bg.battletech.com/forums/index.php?topic=51081.msg1297747#msg1297747
        if (entity.getMovementMode() == EntityMovementMode.WIGE &&
              distance > 0 &&
              (getClearance() > 1 ||
                    (destHex.containsTerrain(Terrains.BLDG_ELEV) && destHex.ceiling() > srcHex.ceiling()))) {
            mp += 2;
        }

        // WIGEs spend one extra MP to ascend a sheer cliff, TO p.39
        if (entity.getMovementMode() == EntityMovementMode.WIGE &&
              distance > 0 &&
              destHex.hasCliffTopTowards(srcHex) &&
              nDestEl > nSrcEl) {
            mp += 1;
        }

        // If we're entering a building, all non-infantry pay additional MP.
        if (nDestEl < destHex.terrainLevel(Terrains.BLDG_ELEV)) {
            IBuilding bldg = game.getBoard(boardId).getBuildingAt(getPosition());
            // check for inside hangar movement
            if (!isInfantry && !isSuperHeavyMek) {
                if (!isProto) {
                    // non-ProtoMeks pay extra according to the building type
                    mp += bldg.getBuildingType().getTypeValue();
                    if (bldg.getBldgClass() == IBuilding.HANGAR) {
                        mp--;
                    }
                    if (bldg.getBldgClass() == IBuilding.FORTRESS) {
                        mp++;
                    }
                } else {
                    // ProtoMeks pay one extra
                    mp += 1;
                }
            } else if (isMechanizedInfantry) {
                // mechanized infantry pays 1 extra
                mp += 1;
            } else if (isInfantry && (((Infantry) entity).getMount() != null)) {
                mp += ((Infantry) entity).getMount().size().buildingMP;
            }
        }

        // Infantry (except mechanized) pay 1 less MP to enter woods
        if (isInfantry && !isMechanizedInfantry && destHex.containsTerrain(Terrains.WOODS) && !isPavementStep) {
            mp--;

            // Ensures that Infantry always pay at least 1 mp when entering woods or jungle
            if (mp <= 0) {
                mp = 1;
            }
        }
    }

    /**
     * Is movement possible from a previous position to this one?
     * <p>
     * This function does not comment on whether an overall movement path is possible, just whether the <em>current</em>
     * step is possible.
     */
    public boolean isMovementPossible(Game game, Coords src, int srcEl, CachedEntityState cachedEntityState) {
        final Hex srcHex = game.getBoard(boardId).getHex(src);
        final Coords dest = getPosition();
        final Hex destHex = game.getBoard(boardId).getHex(dest);
        final Entity entity = getEntity();
        if (destHex == null) {
            return false;
        }
        if (null == dest) {
            var ex = new IllegalStateException("Step has no position");
            LOGGER.error("", ex);
            throw ex;
        } else if (src.distance(dest) > 1) {
            var ex = new IllegalArgumentException("Coordinates " + src + " and " + dest + " are not adjacent.");
            LOGGER.error("", ex);
            throw ex;
        }

        if ((type == MoveStepType.CLIMB_MODE_ON) || (type == MoveStepType.CLIMB_MODE_OFF)) {
            return true;
        }

        // Assault dropping units cannot move
        if ((entity.isAssaultDropInProgress() || entity.isDropping()) &&
              !((entity instanceof LandAirMek) &&
                    (entity.getMovementMode() == EntityMovementMode.WIGE) &&
                    (entity.getAltitude() <= 3))) {
            return false;
        }

        // If we're a tank and immobile, check if we try to unjam
        // or eject and the crew is not unconscious
        if ((entity instanceof Tank) &&
              !entity.getCrew().isUnconscious() &&
              ((type == MoveStepType.UNJAM_RAC) ||
                    (type == MoveStepType.EJECT) ||
                    (type == MoveStepType.SEARCHLIGHT))) {
            return true;
        }

        // We're wanting to start up our reactor and we're not unconscious
        if ((type == MoveStepType.STARTUP) && !entity.getCrew().isUnconscious()) {
            return true;
        }

        // We're wanting to self-destruct our reactor and we're not unconscious
        if ((type == MoveStepType.SELF_DESTRUCT) && !entity.getCrew().isUnconscious()) {
            return true;
        }

        // If you want to flee, and you can flee, flee.
        if ((type == MoveStepType.FLEE) && entity.canFlee(dest)) {
            return true;
        }

        // Motive hit has immobilized CV, but it still wants to (and can) jump: okay!
        if (movementType == EntityMovementType.MOVE_JUMP && (entity instanceof Tank) && !entity.isImmobileForJump()) {
            return true;
        }

        // super-easy, but not anymore
        if (entity.isImmobile() && !entity.isBracing()) {
            return false;
        }

        // Hidden units, and activating hidden units cannot move
        // unless it is the movement phase and the plan is to activate then
        // if we're in this method, we're implicitly in the movement phase
        if (entity.isHidden() ||
              (!entity.getHiddenActivationPhase().isUnknown() && !entity.getHiddenActivationPhase().isMovement())) {
            return false;
        }

        // another easy check
        if (!game.getBoard(boardId).contains(dest)) {
            return false;
        }

        // can't enter impassable hex
        if (destHex.containsTerrain(Terrains.IMPASSABLE)) {
            return false;
        }

        final int srcAlt = srcEl + srcHex.getLevel();

        IBuilding bld = game.getBoard(boardId).getBuildingAt(dest);

        final int destAlt;
        // For buildings (but NOT bridges), when entering from ground level in climbMode,
        // use floor elevation. Bridges should use the bridge elevation instead.
        if (bld != null && getEntity().getElevation() == 0 && climbMode
              && !destHex.containsTerrain(Terrains.BRIDGE)) {
            destAlt = destHex.floor();
        } else {
            destAlt = elevation + destHex.getLevel();
        }

        if (bld != null) {
            // ProtoMeks that are jumping can't change the level inside a building,
            // they can only jump onto a building or out of it
            if (src.equals(dest) &&
                  (srcAlt != destAlt) &&
                  (entity instanceof ProtoMek) &&
                  (getMovementType(false) == EntityMovementType.MOVE_JUMP)) {
                return false;
            }
            Hex hex = game.getBoard(boardId).getHex(getPosition());
            int maxElevation = (2 + entity.getElevation() + game.getBoard(boardId)
                  .getHex(entity.getPosition())
                  .getLevel()) -
                  hex.getLevel();

            if ((bld.getBuildingType() == BuildingType.WALL) && (maxElevation < hex.terrainLevel(Terrains.BLDG_ELEV))) {
                return false;
            }

            // only infantry can enter an armored building
            if ((elevation < hex.terrainLevel(Terrains.BLDG_ELEV)) &&
                  (bld.getArmor(dest) > 0) &&
                  !(entity instanceof Infantry)) {
                return false;
            }

            // only infantry can enter a gun emplacement
            if ((elevation < hex.terrainLevel(Terrains.BLDG_ELEV)) &&
                  (bld.getBldgClass() == IBuilding.GUN_EMPLACEMENT) &&
                  !(entity instanceof Infantry)) {
                return false;
            }
        }

        // Can't back up across an elevation change.
        // PLAYTEST2 Enabling backwards up elevation changes
        if (!(entity instanceof VTOL) &&
              isThisStepBackwards() &&
              !(isJumping() && isUsingMekJumpBooster) &&
              (((destAlt != srcAlt)
                    &&
                    !game.getOptions().booleanOption(OptionsConstants.ADVANCED_GROUND_MOVEMENT_TAC_OPS_WALK_BACKWARDS)
                    && !game.getOptions().booleanOption(OptionsConstants.PLAYTEST_2))
                    ||
                    ((game.getOptions().booleanOption(OptionsConstants.ADVANCED_GROUND_MOVEMENT_TAC_OPS_WALK_BACKWARDS)
                          || game.getOptions().booleanOption(OptionsConstants.PLAYTEST_2))
                          &&
                          (Math.abs(destAlt - srcAlt) > 1)))) {
            return false;
        }

        // Swarming entities can't move.
        if (Entity.NONE != entity.getSwarmTargetId()) {
            return false;
        }

        if (type == MoveStepType.MOUNT) {
            return true;
        }

        if (type == MoveStepType.PICKUP_CARGO || type == MoveStepType.DROP_CARGO) {
            return !isProne();
        }

        // The entity is trying to load. Check for a valid move.
        // In the future, we need to record the target of a LOAD step in the same way we do UNLOAD targets, and
        // remove the loop below completely.
        if (type == MoveStepType.LOAD) {
            // Find the unit being loaded.
            Entity other = null;
            Iterator<Entity> entities = game.getEntities(Compute.getLoadableCoords(entity, src, boardId));
            while (entities.hasNext()) {

                // Is the other unit friendly and not the current entity?
                other = entities.next();
                if (!entity.getOwner().isEnemyOf(other.getOwner()) && !entity.equals(other)) {

                    // The moving unit should be able to load the other unit.
                    if (!entity.canLoad(other, true, getElevation())) {
                        continue;
                    }

                    // The other unit should be able to have a turn.
                    if (!other.isLoadableThisTurn()) {
                        continue;
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
            if (other == null || !entity.canTow(other.getId())) {
                return false;
            }
        } // End STEP_TOW-checks

        // meks dumping ammo can't run
        boolean bDumping = false;
        for (Mounted<?> mo : entity.getAmmo()) {
            if (mo.isDumping()) {
                bDumping = true;
                break;
            }
        }
        if (bDumping &&
              ((movementType == EntityMovementType.MOVE_RUN) ||
                    (movementType == EntityMovementType.MOVE_SPRINT) ||
                    (movementType == EntityMovementType.MOVE_VTOL_RUN) ||
                    (movementType == EntityMovementType.MOVE_VTOL_SPRINT) ||
                    (movementType == EntityMovementType.MOVE_JUMP))) {
            return false;
        }

        // check elevation difference > max
        EntityMovementMode nMove = entity.getMovementMode();

        // Make sure that if it's a VTOL unit with the VTOL MP listed as jump
        // MP...
        // That it can't jump.
        if ((movementType == EntityMovementType.MOVE_JUMP) && (nMove == EntityMovementMode.VTOL)) {
            return false;
        }

        if ((movementType != EntityMovementType.MOVE_JUMP) && (nMove != EntityMovementMode.VTOL)) {
            int maxDown = entity.getMaxElevationDown(srcAlt);
            if (movementMode == EntityMovementMode.WIGE &&
                  (srcEl == 0 ||
                        (srcHex.containsTerrain(Terrains.BLDG_ELEV) &&
                              (srcHex.terrainLevel(Terrains.BLDG_ELEV) >= srcEl)))) {
                maxDown = entity.getMaxElevationChange();
            }
            if ((((srcAlt - destAlt) > 0) && ((srcAlt - destAlt) > maxDown)) ||
                  (((destAlt - srcAlt) > 0) && ((destAlt - srcAlt) > entity.getMaxElevationChange()))) {
                return false;
            }
        }

        // Sheer Cliffs, TO p.39
        // Roads over cliffs cancel the cliff effects for units that move on roads
        boolean vehicleAffectedByCliff = entity instanceof Tank && !entity.isAirborneVTOLorWIGE();
        boolean quadVeeVehicleMode = entity instanceof QuadVee &&
              entity.getConversionMode() == QuadVee.CONV_MODE_VEHICLE;
        int stepHeight = destAlt - srcAlt;
        // Cliffs should only exist towards 1 or 2 level drops, check just to make sure
        // Everything that does not have a 1 or 2 level drop shouldn't be handled as a
        // cliff
        boolean isUpCliff = !src.equals(dest) &&
              destHex.hasCliffTopTowards(srcHex) &&
              (stepHeight == 1 || stepHeight == 2);
        boolean isDownCliff = !src.equals(dest) &&
              srcHex.hasCliffTopTowards(destHex) &&
              (stepHeight == -1 || stepHeight == -2);

        // For vehicles exc. VTOL, WIGE, upward Sheer Cliffs is forbidden
        // QuadVees in vehicle mode drive as vehicles, IO p.133
        if ((vehicleAffectedByCliff || quadVeeVehicleMode) && isUpCliff && !isPavementStep) {
            return false;
        }

        // For Infantry, up or down sheer cliffs requires a climbing action
        // except for Mountain Troops across a level 1 cliff.
        // Climbing actions do not seem to be implemented, so Infantry cannot
        // cross sheer cliffs at all except for Mountain Troops across a level 1 cliff.
        if (entity instanceof Infantry && (isUpCliff || isDownCliff) && !isPavementStep) {

            boolean isMountainTroop = ((Infantry) entity).hasSpecialization(Infantry.MOUNTAIN_TROOPS);
            if (!isMountainTroop || stepHeight == 2) {
                return false;
            }
        }

        if ((entity instanceof Mek) && ((srcAlt - destAlt) > 2)) {
            setLeapDistance(srcAlt - destAlt);
        }

        // Units moving backwards may not change elevation levels.
        if (((type == MoveStepType.BACKWARDS) ||
              (type == MoveStepType.LATERAL_LEFT_BACKWARDS) ||
              (type == MoveStepType.LATERAL_RIGHT_BACKWARDS)) &&
              (destAlt != srcAlt) &&
              !(entity instanceof VTOL) &&
              !(isJumping() && isUsingMekJumpBooster)) {
            // Generally forbidden without TacOps Expanded Backward Movement p.22
            // PLAYTEST2 allow backwards up elevation changes
            if (!game.getOptions().booleanOption(OptionsConstants.ADVANCED_GROUND_MOVEMENT_TAC_OPS_WALK_BACKWARDS)
                  && !game.getOptions().booleanOption(OptionsConstants.PLAYTEST_2)) {
                return false;
            }
            // Even with Expanded Backward Movement, ...
            // May not move across a cliff (up) moving backwards at all
            if (destHex.containsTerrain(Terrains.CLIFF_TOP) &&
                  destHex.getTerrain(Terrains.CLIFF_TOP).hasExitsSpecified() &&
                  ((destHex.getTerrain(Terrains.CLIFF_TOP).getExits() & (1 << dest.direction(src))) != 0) &&
                  (!src.equals(dest))) {
                return false;
            }
            // May not move across a cliff (down) moving backwards at all
            if (srcHex.containsTerrain(Terrains.CLIFF_TOP) &&
                  srcHex.getTerrain(Terrains.CLIFF_TOP).hasExitsSpecified() &&
                  ((srcHex.getTerrain(Terrains.CLIFF_TOP).getExits() & (1 << src.direction(dest))) != 0) &&
                  (!src.equals(dest))) {
                return false;
            }
            // May not move across more than 1 level
            if (Math.abs(destAlt - srcAlt) > 1) {
                return false;
            }
        }

        // WiGEs can't move backwards
        if ((type == MoveStepType.BACKWARDS) && (nMove == EntityMovementMode.WIGE)) {
            return false;
        }

        // Can't run into water unless hovering, naval, first step, using a
        // bridge, or fly.
        // PLAYTEST2 water changes
        if (game.getOptions().booleanOption(OptionsConstants.PLAYTEST_2)) {
            if (((movementType == EntityMovementType.MOVE_RUN) ||
                  (movementType == EntityMovementType.MOVE_SPRINT) ||
                  (movementType == EntityMovementType.MOVE_VTOL_RUN) ||
                  (movementType == EntityMovementType.MOVE_VTOL_SPRINT)) &&
                  (nMove != EntityMovementMode.HOVER) &&
                  (nMove != EntityMovementMode.NAVAL) &&
                  (nMove != EntityMovementMode.HYDROFOIL) &&
                  (nMove != EntityMovementMode.SUBMARINE) &&
                  (nMove != EntityMovementMode.INF_UMU) &&
                  (nMove != EntityMovementMode.VTOL) &&
                  (nMove != EntityMovementMode.WIGE) &&
                  (nMove != EntityMovementMode.BIPED) &&
                  (nMove != EntityMovementMode.QUAD) &&
                  (nMove != EntityMovementMode.TRIPOD) &&
                  !cachedEntityState.hasWorkingMisc(MiscType.F_FULLY_AMPHIBIOUS) &&
                  (destHex.terrainLevel(Terrains.WATER) > 0) &&
                  !(destHex.containsTerrain(Terrains.ICE) && (elevation >= 0)) &&
                  !dest.equals(entity.getPosition()) &&
                  !isFirstStep() &&
                  !isPavementStep()) {
                return false;
            }
        } else {
            if (((movementType == EntityMovementType.MOVE_RUN) ||
                  (movementType == EntityMovementType.MOVE_SPRINT) ||
                  (movementType == EntityMovementType.MOVE_VTOL_RUN) ||
                  (movementType == EntityMovementType.MOVE_VTOL_SPRINT)) &&
                  (nMove != EntityMovementMode.HOVER) &&
                  (nMove != EntityMovementMode.NAVAL) &&
                  (nMove != EntityMovementMode.HYDROFOIL) &&
                  (nMove != EntityMovementMode.SUBMARINE) &&
                  (nMove != EntityMovementMode.INF_UMU) &&
                  (nMove != EntityMovementMode.VTOL) &&
                  (nMove != EntityMovementMode.WIGE) &&
                  !cachedEntityState.hasWorkingMisc(MiscType.F_FULLY_AMPHIBIOUS) &&
                  (destHex.terrainLevel(Terrains.WATER) > 0) &&
                  !(destHex.containsTerrain(Terrains.ICE) && (elevation >= 0)) &&
                  !dest.equals(entity.getPosition()) &&
                  !isFirstStep() &&
                  !isPavementStep()) {
                return false;
            }
        }

        // ugh, stacking checks. well, maybe we're immune! Also, note that these stacking checks are for moving
        // through a hex
        if (!isJumping() && (type != MoveStepType.CHARGE) && (type != MoveStepType.DFA)) {
            // can't move a mek into a hex with an enemy mek
            if ((entity instanceof Mek) &&
                  Compute.isEnemyIn(game, entity, dest, true, true, getElevation(), true)
            ) {
                return false;
            }

            // Can't move out of a hex with an enemy unit unless we started
            // there, BUT we're allowed to turn, unload/Disconnect, or go prone.
            if (Compute.isEnemyIn(game, entity, src, false, entity instanceof Mek, srcEl, true) &&
                  !src.equals(entity.getPosition()) &&
                  (type != MoveStepType.TURN_LEFT) &&
                  (type != MoveStepType.TURN_RIGHT) &&
                  (type != MoveStepType.UNLOAD) &&
                  (type != MoveStepType.DISCONNECT) &&
                  (type != MoveStepType.GO_PRONE)) {
                return false;
            }

            // Can't move through a hex with a LargeSupportTank or a grounded DropShip unless infantry or a VTOL at
            // high enough elevation
            if (!(entity instanceof Infantry)) {
                for (Entity inHex : game.getEntitiesVector(src)) {
                    if (inHex.equals(entity) || inHex.isHidden()) {
                        continue;
                    }

                    // TW p.57
                    if ((inHex instanceof LargeSupportTank)
                          || ((inHex instanceof Dropship dropship) && dropship.isAeroLandedOnGroundMap())) {
                        if (getElevation() <= inHex.height()) {
                            return false;
                        }
                    }

                    // TW p.57. The rules aren't entirely clear but this assumes that Large SV stacking is
                    // symmetrical - they themselves cannot move through a hex with another unit unless it's infantry
                    if ((entity instanceof LargeSupportTank) && !(inHex instanceof Infantry)) {
                        return false;
                    }
                }
            }
        }

        // can't jump over too-high terrain
        if ((movementType == EntityMovementType.MOVE_JUMP) &&
              (destAlt >
                    (entity.getElevation() +
                          game.getBoard(boardId).getHex(entity.getPosition()).getLevel() +
                          getAvailableJumpMP(entity) +
                          (type == MoveStepType.DFA ? 1 : 0)))) {
            return false;
        }

        // Certain movement types have terrain restrictions; terrain
        // restrictions are lifted when moving along a road or bridge,
        // or when flying. Naval movement does not have the pavement
        // exemption.
        if (entity.isLocationProhibited(dest, boardId, getElevation())
              // Units in prohibited terran should still be able to unload/disconnect
              &&
              (type != MoveStepType.UNLOAD) &&
              (type != MoveStepType.DISCONNECT)
              // Should allow vertical takeoffs
              &&
              (type != MoveStepType.VERTICAL_TAKE_OFF)
              // QuadVees can convert to vehicle mode even if they cannot enter the terrain
              &&
              (type != MoveStepType.CONVERT_MODE) &&
              (!isPavementStep() ||
                    (nMove == EntityMovementMode.NAVAL) ||
                    (nMove == EntityMovementMode.HYDROFOIL) ||
                    (nMove == EntityMovementMode.SUBMARINE)) &&
              (movementType != EntityMovementType.MOVE_VTOL_WALK) &&
              (movementType != EntityMovementType.MOVE_VTOL_RUN) &&
              (movementType != EntityMovementType.MOVE_VTOL_SPRINT)) {

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
        if ((entity instanceof Dropship) &&
              !entity.isAirborne() &&
              isPavementStep() &&
              entity.isLocationProhibited(dest, boardId, getElevation()) &&
              (movementType != EntityMovementType.MOVE_SAFE_THRUST) &&
              (type != MoveStepType.LOAD) &&
              (type != MoveStepType.UNLOAD)) {
            for (int dir = 0; dir < 6; dir++) {
                Coords secondaryCoords = dest.translated(dir);
                Hex secondaryHex = game.getBoard(boardId).getHex(secondaryCoords);
                if (!secondaryHex.hasPavement()) {
                    return false;
                }
            }
        }

        // If we're a land train with mixed motive types, use the most restrictive type
        // to determine terrain restrictions
        if (!entity.getAllTowedUnits().isEmpty() &&
              (type != MoveStepType.LOAD &&
                    type != MoveStepType.UNLOAD &&
                    type != MoveStepType.TOW &&
                    type != MoveStepType.DISCONNECT)) {
            boolean prohibitedByTrailer;
            // Add up the trailers
            for (int id : entity.getAllTowedUnits()) {
                Entity tr = game.getEntity(id);

                if (tr == null) {
                    continue;
                }

                prohibitedByTrailer = tr.isLocationProhibited(dest, boardId, getElevation());
                if (prohibitedByTrailer) {
                    return false;
                }
            }
        }

        // Jumping into a building hex below the roof ends the move
        // assume this applies also to sylph vtol movement
        if (!(src.equals(dest)) &&
              (src != entity.getPosition()) &&
              (isJumping() || (entity.getMovementMode() == EntityMovementMode.VTOL)) &&
              (srcEl < srcHex.terrainLevel(Terrains.BLDG_ELEV))) {
            return false;
        }

        // Jumping inside a building to another hex of the same building is illegal
        Coords startingPosition = getEntity().getPosition();
        Hex startingHex = game.getHexOf(getEntity());
        if (!destHex.getCoords().equals(startingPosition) &&
              isJumping() &&
              startingHex.containsTerrain(Terrains.BUILDING) &&
              destHex.containsTerrain(Terrains.BUILDING) &&
              srcEl < srcHex.terrainLevel(Terrains.BLDG_ELEV) &&
              (game.getBoard(getEntity())
                    .getBuildingAt(startingPosition)
                    .equals(game.getBoard(getEntity()).getBuildingAt(getPosition())))) {
            return false;
        }

        // If we are *in* restricted terrain, we can only leave via roads.
        if ((movementType != EntityMovementType.MOVE_JUMP) &&
              (movementType != EntityMovementType.MOVE_VTOL_WALK) &&
              (movementType != EntityMovementType.MOVE_VTOL_RUN) &&
              (movementType != EntityMovementType.MOVE_VTOL_SPRINT)
              // Units in prohibited terran should still be able to unload/disconnect
              &&
              (type != MoveStepType.UNLOAD) &&
              (type != MoveStepType.DISCONNECT)
              // Should allow vertical takeoffs
              &&
              (type != MoveStepType.VERTICAL_TAKE_OFF)
              // QuadVees can still convert to vehicle mode in prohibited terrain, but cannot
              // leave
              &&
              (type != MoveStepType.CONVERT_MODE) &&
              entity.isLocationProhibited(src, boardId, srcEl) &&
              !isPavementStep()) {
            return false;
        }
        if (type == MoveStepType.UP) {
            if (!(entity.canGoUp(elevation - 1, getPosition(), getBoardId()))) {
                return false;
            }
        }
        if (type == MoveStepType.DOWN) {
            if (!(entity.canGoDown(elevation + 1, getPosition(), getBoardId()))) {
                return false;// We can't intentionally crash.
            }
        }
        if (entity instanceof VTOL) {
            if ((type == MoveStepType.BACKWARDS) ||
                  (type == MoveStepType.FORWARDS) ||
                  (type == MoveStepType.LATERAL_LEFT) ||
                  (type == MoveStepType.LATERAL_LEFT_BACKWARDS) ||
                  (type == MoveStepType.LATERAL_RIGHT) ||
                  (type == MoveStepType.LATERAL_RIGHT_BACKWARDS) ||
                  (type == MoveStepType.TURN_LEFT) ||
                  (type == MoveStepType.TURN_RIGHT)) {
                if (getClearance() == 0) {// can't move on the ground.
                    return false;
                }
            }
        }
        if ((entity instanceof VTOL || entity.getMovementMode() == EntityMovementMode.WIGE) &&
              getClearance() > 0 &&
              ((type == MoveStepType.BACKWARDS) ||
                    (type == MoveStepType.FORWARDS) ||
                    (type == MoveStepType.LATERAL_LEFT) ||
                    (type == MoveStepType.LATERAL_LEFT_BACKWARDS) ||
                    (type == MoveStepType.LATERAL_RIGHT) ||
                    (type == MoveStepType.LATERAL_RIGHT_BACKWARDS))) {
            // It's possible to fly under a bridge.
            if (destHex.containsTerrain(Terrains.BRIDGE_ELEV)) {
                if (elevation == destHex.terrainLevel(Terrains.BRIDGE_ELEV)) {
                    return false;
                }
            } else if (elevation <= (destHex.ceiling() - destHex.getLevel())) {
                // WiGE are not prohibited from flying over planted fields.
                if ((entity.getMovementMode() == EntityMovementMode.WIGE) && destHex.containsTerrain(Terrains.FIELDS)) {
                    return true;
                }

                // VTOLs and WiGEs can fly through woods and jungle below the level of the
                // treetops on a road.
                if (destHex.containsTerrain(Terrains.WOODS) || destHex.containsTerrain(Terrains.JUNGLE)) {
                    return destHex.containsTerrainExit(Terrains.ROAD, dest.direction(src));
                }
                return false; // can't fly into woods or a cliff face
            }
        }

        // check the elevation is valid for the type of entity and hex
        if ((type != MoveStepType.DFA) && !entity.isElevationValid(elevation, destHex)) {
            if (isJumping()) {
                terrainInvalid = true;
            } else {
                return false;
            }
        }

        return true;
    }

    private int getAvailableJumpMP(Entity entity) {
        return isUsingMekJumpBooster ? entity.getMechanicalJumpBoosterMP() : entity.getJumpMPWithTerrain();
    }

    public int getElevation() {
        return elevation;
    }

    /**
     * In hexes with buildings, returns the elevation relative to the roof. Otherwise, returns the elevation relative to
     * the surface.
     */
    public int getClearance() {
        Hex hex = entity.getGame().getBoard(boardId).getHex(getPosition());
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

        // jump ships (but not space stations and warships) never pay
        if ((entity instanceof Jumpship) && !(entity instanceof Warship) && !(entity instanceof SpaceStation)) {
            return 0;
        }

        // if we're behaving like a spheroid in atmosphere, we can spin around to our
        // heart's content
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
        } else if (velocity < 6) {
            return 2 + thrustCost;
        } else if (velocity < 8) {
            return 3 + thrustCost;
        } else if (velocity < 10) {
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

        // spheroids in atmosphere can spin around like a centrifuge all they want
        if (useSpheroidAtmosphere(game, en)) {
            return true;
        }

        if (dueFreeTurn()) {
            return true;
        }

        // if its part of a maneuver then you can turn
        if (isFacingChangeManeuver()) {
            return true;
        }

        if (en instanceof ConvFighter) {
            // conventional fighters can only turn on free turns or maneuvers
            return false;
        }

        // Can't use thrust turns in the first hex of movement (or first 8 if ground)
        if (game.getBoard(boardId).isGround()) {
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
        if (en.isSpaceborne()) {
            return false;
        }
        int straight = getNStraight();
        int vel = getVelocity();
        int thresh;

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
        if (en.getGame().getBoard(boardId).isGround() && (getElevation() > 0)) {
            if (en instanceof Dropship) {
                thresh = vel * 8;
            } else if (en instanceof SmallCraft) {
                thresh = 8 + ((vel - 1) * 6);
            } else {
                thresh = 8 + ((vel - 1) * 4);
            }
        }

        return straight >= thresh;

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

    public int getManeuverType() {
        return maneuverType;
    }

    public boolean hasNoCost() {
        return noCost;
    }

    public boolean isManeuver() {
        return maneuver;
    }

    /**
     * @return Whether this step is a maneuver that allows a free facing change.
     */
    public boolean isFacingChangeManeuver() {
        return maneuver && (maneuverType == ManeuverType.MAN_IMMELMAN || maneuverType == ManeuverType.MAN_SPLIT_S);
    }

    public Minefield getMinefield() {
        return mf;
    }

    /**
     * For serialization purposes
     */
    public Map<Integer, Integer> getAdditionalData() {
        return additionalData;
    }

    /**
     * Should we treat this movement as if it is occurring for an aerodyne unit flying in atmosphere?
     */
    boolean useAeroAtmosphere(Game game, Entity en) {
        if (!en.isAero()) {
            return false;
        }
        if (((IAero) en).isSpheroid()) {
            return false;
        }
        // are we in space?
        if (game.getBoard(boardId).isSpace()) {
            return false;
        }
        // are we airborne in non-vacuum?
        PlanetaryConditions conditions = game.getPlanetaryConditions();
        return en.isAirborne() && !conditions.getAtmosphere().isLighterThan(Atmosphere.THIN);
    }

    /**
     * Should we treat this movement as if it is occurring for a spheroid unit flying in atmosphere?
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
        if (!game.getOptions().booleanOption(OptionsConstants.ADVANCED_GROUND_MOVEMENT_TAC_OPS_SPRINT)) {
            return false;
        }
        if (entity instanceof Tank ||
              (entity instanceof QuadVee && entity.getConversionMode() == QuadVee.CONV_MODE_VEHICLE)) {
            return game.getOptions()
                  .booleanOption(OptionsConstants.ADVANCED_GROUND_MOVEMENT_VEHICLE_ADVANCED_MANEUVERS);
        }
        if (entity instanceof LandAirMek) {
            return entity.getConversionMode() == LandAirMek.CONV_MODE_MEK ||
                  (entity.getConversionMode() == LandAirMek.CONV_MODE_AIR_MEK && getClearance() <= 0);
        }
        return entity instanceof Mek;
    }

    public int getBoardId() {
        return boardId;
    }

    /**
     * @return True if this move step is a climb mode change (on or off)
     */
    public boolean isClimbMode() {
        return (type == MoveStepType.CLIMB_MODE_ON) || (type == MoveStepType.CLIMB_MODE_OFF);
    }
}
