/*
 * Copyright (C) 2017-2025 The MegaMek Team. All Rights Reserved.
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


package megamek.common.units;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import megamek.common.Hex;
import megamek.common.LandingDirection;
import megamek.common.ManeuverType;
import megamek.common.OffBoardDirection;
import megamek.common.annotations.Nullable;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.enums.MoveStepType;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.Mounted;
import megamek.common.exceptions.LocationFullException;
import megamek.common.game.Game;
import megamek.common.moves.MovePath;
import megamek.common.moves.MoveStep;
import megamek.common.options.OptionsConstants;
import megamek.common.rolls.PilotingRollData;
import megamek.common.rolls.TargetRoll;
import megamek.logging.MMLogger;

/**
 * Methods shared by Aero and LandAirMek
 *
 * @author Neoancient
 */
public interface IAero {
    MMLogger LOGGER = MMLogger.create(IAero.class);

    int getCurrentThrust();

    int getCurrentVelocity();

    void setCurrentVelocity(int velocity);

    int getNextVelocity();

    void setNextVelocity(int velocity);

    int getCurrentVelocityActual();

    boolean isVSTOL();

    boolean isSTOL();

    boolean isSpheroid();

    boolean isAirborne();

    boolean isSpaceborne();

    boolean isRolled();

    void setRolled(boolean roll);

    boolean isOutControlTotal();

    boolean isOutControl();

    void setOutControl(boolean outControl);

    boolean isOutControlHeat();

    void setOutControlHeat(boolean outControlHeat);

    boolean isRandomMove();

    void setRandomMove(boolean randomMove);

    boolean didAccLast();

    void setAccLast(boolean b);

    boolean didFailManeuver();

    void setFailedManeuver(boolean b);

    void setAccDecNow(boolean b);

    boolean didAccDecNow();

    int getStraightMoves();

    void setStraightMoves(int straightMoves);

    int getAltLoss();

    void setAltLoss(int i);

    void resetAltLoss();

    int getAltLossThisRound();

    void setAltLossThisRound(int i);

    void resetAltLossThisRound();

    int getNoseArmor();

    void setSI(int si);

    int getSI();

    int getOSI();

    int getAvionicsHits();

    int getSensorHits();

    int getFCSHits();

    int getLandingGearPartialRepairs();

    int getAvionicsMisReplaced();

    int getAvionicsMisrepaired();

    default int getClusterMods() {
        return -1 * (getFCSHits() + getSensorHits());
    }

    boolean hasLifeSupport();

    void setGearHit(boolean hit);

    int getLandingGearMod(boolean vTakeoff);

    int getLeftThrustHits();

    int getRightThrustHits();

    int getThresh(int loc);

    int getHighestThresh();

    boolean wasCritThresh();

    void setCritThresh(boolean b);

    int getFuel();

    void setFuel(int gas);

    int getCurrentFuel();

    void setCurrentFuel(int gas);

    double getFuelPointsPerTon();

    boolean isFlyingOff();

    void setFlyingOff(OffBoardDirection obd);

    OffBoardDirection getFlyingOffDirection();

    /**
     * Gets the altitude at which this unit climbed out of the atmosphere. Used for returning units that left via
     * vertical climb out at altitude 10.
     *
     * @return The exit altitude (0 if not a climb out, typically 10 for climb outs)
     */
    int getExitAltitude();

    /**
     * Sets the altitude at which this unit is climbing out of the atmosphere. Should be set when a unit leaves the map
     * vertically at altitude 10.
     *
     * @param altitude The exit altitude (typically 10 for climb out, 0 to clear)
     */
    void setExitAltitude(int altitude);

    /**
     * @return True when this aero requires fuel to move. Note that the result is undefined when the unit has no engine.
     *       Callers should consider this case themselves. Also note that this method does not check whether fuel use as
     *       a game option is active, only if the unit technically requires fuel to move. For example, returns false for
     *       solar-powered prop-driven fixed wing support (TM p129).
     */
    default boolean requiresFuel() {
        return true;
    }

    // Capital fighters
    int getCapArmor();

    void setCapArmor(int i);

    int getCap0Armor();

    int getFatalThresh();

    int getCurrentDamage();

    void setCurrentDamage(int i);

    int getHeatSinks();

    void doDisbandDamage();

    void autoSetCapArmor();

    void autoSetFatalThresh();

    int getAltitude();

    /**
     * Iterate through current weapons and count the number in each capital fighter location.
     *
     * @return A map with keys in the format "weaponName:loc", with the number of weapons of that type in that location
     *       as the value.
     */
    Map<String, Integer> groupWeaponsByLocation();

    Map<String, Integer> getWeaponGroups();

    /**
     * Refresh the capital fighter weapons groups.
     */
    default void updateWeaponGroups() {
        if ((this instanceof Entity entity) && (entity.game != null)
              && entity.gameOptions()
              .booleanOption(OptionsConstants.ADVANCED_AERO_RULES_STRATOPS_CAPITAL_FIGHTER)) {
            // first we need to reset all the weapons in our existing mounts to zero
            // until proven otherwise
            Set<String> set = getWeaponGroups().keySet();
            for (String key : set) {
                ((Entity) this).getEquipment(getWeaponGroups().get(key)).setNWeapons(0);
            }
            // now collect a hash of all the same weapons in each location by id
            Map<String, Integer> groups = groupWeaponsByLocation();
            // now we just need to traverse the hash and either update our existing
            // equipment or add new ones if there is none
            Set<String> newSet = groups.keySet();
            for (String key : newSet) {
                if (null != getWeaponGroups().get(key)) {
                    // then this equipment is already loaded, so we just need to
                    // correctly update the number of weapons
                    ((Entity) this).getEquipment(getWeaponGroups().get(key)).setNWeapons(groups.get(key));
                } else {
                    // need to add a new weapon
                    String name = key.split(":")[0];
                    int loc = Integer.parseInt(key.split(":")[1]);
                    EquipmentType etype = EquipmentType.get(name);
                    Mounted<?> newMount;
                    if (etype != null) {
                        try {
                            newMount = ((Entity) this).addWeaponGroup(etype, loc);
                            newMount.setNWeapons(groups.get(key));
                            getWeaponGroups().put(key, ((Entity) this).getEquipmentNum(newMount));
                        } catch (LocationFullException ex) {
                            LOGGER.error("Unable to compile weapon groups", ex);
                            return;
                        }
                    } else if (!"0".equals(name)) {
                        ((Entity) this).addFailedEquipment(name);
                    }
                }
            }
        }
    }

    /**
     * Set number of fuel points based on fuel tonnage.
     *
     * @param fuelTons The number of tons of fuel
     */
    void setFuelTonnage(double fuelTons);

    /**
     * Gets the fuel for this Aero in terms of tonnage.
     *
     * @return The number of tons of fuel on this Aero.
     */
    double getFuelTonnage();

    /*
     * Default methods that are implemented the same for Aero and LandAirMek
     */

    default PilotingRollData checkThrustSI(int thrust, EntityMovementType overallMoveType) {
        PilotingRollData roll = ((Entity) this).getBasePilotingRoll(overallMoveType);

        if (thrust > getSI()) {
            // append the reason modifier
            roll.append(new PilotingRollData(((Entity) this).getId(), thrust - getSI(),
                  "Thrust exceeds current SI in a single hex"));
        } else {
            roll.addModifier(TargetRoll.CHECK_FALSE, "Check false: Entity is not exceeding SI");
        }
        return roll;
    }

    /**
     * Checks if the specified thrust value exceeds the current structural integrity (SI) of the entity and computes any
     * necessary modifiers or notes for the resulting piloting roll.
     *
     * <p>If the entity has the "G Tolerance" ability, a bonus modifier is applied.</p>
     * <ul>
     *   <li>If the thrust used is greater than the entity's SI, a modifier indicating "Thrust spent this turn
     *   exceeds current SI" is appended to the result.</li>
     *   <li>Otherwise, an indicator is added to show the SI was not exceeded.</li>
     * </ul>
     *
     * @param thrust          the thrust value used this turn
     * @param overallMoveType the overall movement type for this piloting check
     *
     * @return a {@link PilotingRollData} object with all appropriate modifiers and notes
     */
    default PilotingRollData checkThrustSITotal(int thrust, EntityMovementType overallMoveType) {
        PilotingRollData roll = ((Entity) this).getBasePilotingRoll(overallMoveType);

        if (thrust > getSI()) {
            // append the reason modifier
            roll.append(new PilotingRollData(((Entity) this).getId(), 0, "Thrust spent this turn exceeds current SI"));

            boolean hasGTolerance = ((Entity) this).hasAbility(OptionsConstants.PILOT_ATOW_G_TOLERANCE);
            if (hasGTolerance) {
                roll.addModifier(-1, "G-Tolerance");
            }
        } else {
            roll.addModifier(TargetRoll.CHECK_FALSE, "Check false: Entity is not exceeding SI");
        }
        return roll;
    }

    default PilotingRollData checkVelocityDouble(int velocity, EntityMovementType overallMoveType) {
        PilotingRollData roll = ((Entity) this).getBasePilotingRoll(overallMoveType);

        if ((velocity > (2 * ((Entity) this).getWalkMP())) && !isSpaceborne()) {
            // append the reason modifier
            roll.append(new PilotingRollData(((Entity) this).getId(), 0, "Velocity greater than 2x safe thrust"));
        } else {
            roll.addModifier(TargetRoll.CHECK_FALSE, "Check false: Entity is not exceeding 2x safe thrust");
        }
        return roll;
    }

    default PilotingRollData checkDown(int drop, EntityMovementType overallMoveType) {
        PilotingRollData roll = ((Entity) this).getBasePilotingRoll(overallMoveType);

        if (drop > 2) {
            // append the reason modifier
            roll.append(new PilotingRollData(((Entity) this).getId(), drop, "lost more than two altitudes"));
        } else {
            roll.addModifier(TargetRoll.CHECK_FALSE, "Check false: entity did not drop more than two altitudes");
        }
        return roll;
    }

    default PilotingRollData checkHover(MovePath md) {
        PilotingRollData roll = ((Entity) this).getBasePilotingRoll(md.getLastStepMovementType());

        if (md.contains(MoveStepType.HOVER) && (md.getLastStepMovementType() == EntityMovementType.MOVE_OVER_THRUST)) {
            // append the reason modifier
            roll.append(new PilotingRollData(((Entity) this).getId(), 0, "hovering above safe thrust"));
        } else {
            roll.addModifier(TargetRoll.CHECK_FALSE, "Check false: entity did not hover");
        }
        return roll;
    }

    default PilotingRollData checkStall(MovePath md) {
        Entity thisEntity = (Entity) this;
        PilotingRollData roll = thisEntity.getBasePilotingRoll(md.getLastStepMovementType());

        // if the entity has already moved, its movement got interrupted (probably by a
        // hidden unit, not much else can interrupt an aero unit)
        // in which case, the movement is complete. We just need to allow the user to
        // hit 'done'.
        if (thisEntity.delta_distance > 0) {
            roll.addModifier(TargetRoll.CHECK_FALSE, "Check false: aero has already moved");
            // an airborne, aerodyne aero is considered to "stall" if it's not moving anywhere, hovering, landing, or
            // going off board
        } else if ((md.getFinalVelocity() == 0) && !md.contains(MoveStepType.HOVER) && isAirborne() && !isSpheroid()
              && !thisEntity.getGame().getBoard(md.getFinalBoardId()).isSpace() && !md.contains(MoveStepType.LAND)
              && !md.contains(MoveStepType.VERTICAL_LAND) && !md.contains(MoveStepType.RETURN)
              && !md.contains(MoveStepType.OFF) && !md.contains(MoveStepType.FLEE)) {
            roll.append(new PilotingRollData(thisEntity.getId(), 0, "stalled out"));
        } else {
            roll.addModifier(TargetRoll.CHECK_FALSE, "Check false: entity not stalled out");
        }
        return roll;
    }

    default PilotingRollData checkRolls(MoveStep step, EntityMovementType overallMoveType) {
        PilotingRollData roll = ((Entity) this).getBasePilotingRoll(overallMoveType);

        if (((step.getType() == MoveStepType.ROLL) || (step.getType() == MoveStepType.YAW)) && (step.getNRolls() > 1)) {
            // append the reason modifier
            roll.append(new PilotingRollData(((Entity) this).getId(), 0, "More than one roll in the same turn"));
        } else {
            roll.addModifier(TargetRoll.CHECK_FALSE, "Check false: Entity is not rolling more than once");
        }
        return roll;
    }

    default PilotingRollData checkVerticalTakeOff() {
        PilotingRollData roll = ((Entity) this).getBasePilotingRoll(EntityMovementType.MOVE_SAFE_THRUST);

        if (getLandingGearMod(true) > 0) {
            roll.addModifier(+1, "landing gear damaged");
        }

        if (getLeftThrustHits() + getRightThrustHits() > 0) {
            roll.addModifier(+3, "Maneuvering thrusters damaged");
        }

        // Supposed to be -1 for lifting off from an "airfield or landing pad."
        // We will just treat this as having paved terrain
        Coords pos = ((Entity) this).getPosition();
        Hex hex = ((Entity) this).getGame().getHexOf((Entity) this);
        if ((null != hex) && hex.containsTerrain(Terrains.PAVEMENT) && !hex.containsTerrain(Terrains.RUBBLE)) {
            roll.addModifier(-1, "on landing pad");
        }

        if (!(this instanceof SmallCraft)) {
            roll.addModifier(+2, "Fighter making vertical liftoff");
        }

        // Taking off from a crater
        // TW doesn't define what a crater is, assume it means that the hex
        // level of all surrounding hexes is greater than what we are sitting on
        boolean allAdjacentHigher = true;
        Set<Coords> positions = new HashSet<>(((Entity) this).getSecondaryPositions().values());
        positions.add(pos);
        Hex adjHex;
        for (Coords currPos : positions) {
            hex = ((Entity) this).getGame().getHex(currPos, ((Entity) this).getBoardId());
            for (int dir = 0; dir < 6; dir++) {
                Coords adj = currPos.translated(dir);
                adjHex = ((Entity) this).getGame().getHex(adj, ((Entity) this).getBoardId());
                if (!positions.contains(adj) && (adjHex != null) && adjHex.getLevel() <= hex.getLevel()) {
                    allAdjacentHigher = false;
                    break;
                }
            }
            if (!allAdjacentHigher) {
                break;
            }
        }
        if (allAdjacentHigher) {
            roll.addModifier(+3, "Taking off from crater");
        }

        return roll;
    }

    /**
     * Computes the PilotingRollData for a landing control roll (see TW pg 86).
     *
     * @param path The landing move path to process (must contain a LAND or VERTICAL_LAND move step)
     *
     * @return A PilotingRollData tha represents the landing control roll that must be passed
     */
    default PilotingRollData getLandingControlRoll(MovePath path) {
        return getLandingControlRoll(path.getFinalVelocity(),
              path.getFinalCoords(),
              path.getFinalFacing(),
              path.contains(MoveStepType.VERTICAL_LAND));
    }

    /**
     * Computes the PilotingRollData for a landing control roll (see TW pg 86).
     *
     * @param velocity   Velocity when the check is to be made, this needs to be passed as the check could happen as
     *                   part of a Move Path
     * @param landingPos The touch-down position (for a horizontal landing, that is not the final position)
     * @param isVertical If this a vertical or horizontal landing
     *
     * @return A PilotingRollData tha represents the landing control roll that must be passed
     */
    default PilotingRollData getLandingControlRoll(int velocity, Coords landingPos, int face,
          boolean isVertical) {
        // Base piloting skill
        PilotingRollData roll = new PilotingRollData(((Entity) this).getId(), ((Entity) this).getCrew().getPiloting(),
              "Base piloting skill");

        // Apply critical hit effects, TW pg 239
        int aviationHits = getAvionicsHits();
        if ((aviationHits > 0) && (aviationHits < 3)) {
            roll.addModifier(aviationHits, "Avionics Damage");
        }

        // this should probably be replaced with some kind of AVI_DESTROYED
        // boolean
        if (aviationHits >= 3) {
            roll.addModifier(5, "Avionics Destroyed");
        }

        if (!hasLifeSupport()) {
            roll.addModifier(+2, "No life support");
        }

        // Landing Gear Partial Repairs, only apply if the landing gear isn't currently
        // damaged
        if (getLandingGearMod(false) == 0) {
            if (getLandingGearPartialRepairs() == 2) {
                roll.addModifier(getLandingGearPartialRepairs(), "landing gear misrepaired");
            } else if (getLandingGearPartialRepairs() == 1) {
                roll.addModifier(getLandingGearPartialRepairs(), "landing gear mis-replaced");
            }
        }

        // Avionics Partial Repairs, only apply if the Avionics package isn't destroyed
        if (aviationHits < 3) {
            if (getAvionicsMisrepaired() == 1) {
                roll.addModifier(1, "misrepaired avionics");
            }
            if (getAvionicsMisReplaced() == 1) {
                roll.addModifier(1, "mis-replaced avionics");
            }
        }
        // Landing Modifiers table, TW pg 86
        int velocityModifiers;
        if (isVertical) {
            velocityModifiers = Math.max(0, velocity - 1);
        } else {
            velocityModifiers = Math.max(0, velocity - 2);
        }
        if (velocityModifiers > 0) {
            roll.addModifier(velocityModifiers, "excess velocity");
        }
        if (getLeftThrustHits() + getRightThrustHits() > 0) {
            roll.addModifier(+4, "Maneuvering thrusters damaged");
        }
        if (getLandingGearMod(false) > 0) {
            roll.addModifier(getLandingGearMod(false), "landing gear damaged");
        }
        if (getNoseArmor() <= 0) {
            roll.addModifier(+2, "nose armor destroyed");
        }
        // Unit reduced to 50% or less of starting thrust
        double thrustPercent = ((double) ((Entity) this).getWalkMP()) / ((Entity) this).getOriginalWalkMP();
        if (thrustPercent <= .5) {
            roll.addModifier(+2, "thrust reduced to 50% or less of original");
        }
        if (getCurrentThrust() <= 0) {
            if (isSpheroid()) {
                roll.addModifier(+8, "no thrust");
            } else {
                roll.addModifier(+4, "no thrust");
            }
        }

        // Per TW p. 87, the modifier is added once for each terrain type
        Set<List<Integer>> terrains = new HashSet<>();
        Set<Coords> landingPositions = getLandingCoords(isVertical, landingPos, face);
        // Any hex without terrain is clear, which is a +2 modifier.
        boolean clear = false;
        // FIXME I suspect this going to fail when an aero flies in from an atmosphere board into a ground board and
        //  lands
        //  in a single movement.
        Board board = ((Entity) this).getGame().getBoard(((Entity) this).getBoardId());
        for (Coords pos : landingPositions) {
            Hex hex = board.getHex(pos);
            if (hex == null) {
                continue;
            }
            if (hex.hasPavementOrRoad()) {
                if (hex.containsTerrain(Terrains.ROAD)) { //Check for dirt or gravel road, they're harder to land on
                    if (Terrains.landingModifier(Terrains.ROAD, hex.terrainLevel(Terrains.ROAD)) > 0) {
                        terrains.add(List.of(Terrains.ROAD, hex.terrainLevel(Terrains.ROAD)));
                    }
                }
                continue;
            }
            if (hex.getBaseTerrainType() == 0) {
                clear = true;
            }
            for (int terrain : hex.getTerrainTypes()) {
                if ((terrain == Terrains.WATER) && hex.containsTerrain(Terrains.ICE)) {
                    continue;
                }
                if (Terrains.landingModifier(terrain, hex.terrainLevel(terrain)) > 0) {
                    terrains.add(List.of(terrain, hex.terrainLevel(terrain)));
                }
            }
        }
        if (clear) {
            roll.addModifier(isVertical ? 1 : 2, "Clear terrain in landing path");
        }
        for (List<Integer> terrain : terrains) {
            int mod = Terrains.landingModifier(terrain.get(0), terrain.get(1));
            if (isVertical) {
                mod = mod / 2 + mod % 2;
            }
            roll.addModifier(mod, Terrains.getDisplayName(terrain.get(0), terrain.get(1)) + " in landing path");
        }

        return roll;
    }

    default Set<Coords> getLandingCoords(boolean isVertical, Coords landingPos, int facing) {
        Set<Coords> landingPositions = new HashSet<>();
        if (isVertical) {
            landingPositions.add(landingPos);
            // Dropships must also check the adjacent 6 hexes
            if (this instanceof Dropship) {
                for (int i = 0; i < 6; i++) {
                    landingPositions.add(landingPos.translated(i));
                }
            }
            // Horizontal landing requires checking whole landing strip
        } else {
            for (int i = 0; i < getLandingLength(); i++) {
                Coords pos = landingPos.translated(facing, i);
                landingPositions.add(pos);
                // Dropships have to check the front adjacent hexes
                if (this instanceof Dropship) {
                    landingPositions.add(pos.translated((facing + 4) % 6));
                    landingPositions.add(pos.translated((facing + 2) % 6));
                }
            }
        }
        return landingPositions;
    }

    /**
     * Checks if a maneuver requires a control roll
     */
    default PilotingRollData checkManeuver(MoveStep step, EntityMovementType overallMoveType) {
        PilotingRollData roll = ((Entity) this).getBasePilotingRoll(overallMoveType);

        if ((step == null) || (step.getType() != MoveStepType.MANEUVER)) {
            roll.addModifier(TargetRoll.CHECK_FALSE, "Check false: Entity is not attempting to get up.");
            return roll;
        }
        boolean sideSlipMod = (this instanceof ConvFighter) && isVSTOL();
        roll.append(
              new PilotingRollData(((Entity) this).getId(), ManeuverType.getMod(step.getManeuverType(), sideSlipMod),
                    ManeuverType.getTypeName(step.getManeuverType()) + " maneuver"));

        return roll;

    }

    /**
     * switches certain arcs due to rolling
     */
    default int rollArcs(int arc) {
        if (isRolled()) {
            if (arc == Compute.ARC_LEFT_WING) {
                return Compute.ARC_RIGHT_WING;
            } else if (arc == Compute.ARC_RIGHT_WING) {
                return Compute.ARC_LEFT_WING;
            } else if (arc == Compute.ARC_LEFT_WING_AFT) {
                return Compute.ARC_RIGHT_WING_AFT;
            } else if (arc == Compute.ARC_RIGHT_WING_AFT) {
                return Compute.ARC_LEFT_WING_AFT;
            } else if (arc == Compute.ARC_LEFT_SIDE_SPHERE) {
                return Compute.ARC_RIGHT_SIDE_SPHERE;
            } else if (arc == Compute.ARC_RIGHT_SIDE_SPHERE) {
                return Compute.ARC_LEFT_SIDE_SPHERE;
            } else if (arc == Compute.ARC_LEFT_SIDE_AFT_SPHERE) {
                return Compute.ARC_RIGHT_SIDE_AFT_SPHERE;
            } else if (arc == Compute.ARC_RIGHT_SIDE_AFT_SPHERE) {
                return Compute.ARC_LEFT_SIDE_AFT_SPHERE;
            } else if (arc == Compute.ARC_LEFT_BROADSIDE) {
                return Compute.ARC_RIGHT_BROADSIDE;
            } else if (arc == Compute.ARC_RIGHT_BROADSIDE) {
                return Compute.ARC_LEFT_BROADSIDE;
            }
        }
        return arc;
    }

    default int getTakeOffLength() {
        if (isVSTOL() || isSTOL()) {
            return 10;
        }
        return 20;
    }

    default int getLandingLength() {
        if (isVSTOL() || isSTOL()) {
            return 5;
        }
        return 8;
    }

    default boolean canTakeOffHorizontally() {
        return !isSpheroid() && (getCurrentThrust() > 0);
    }

    default boolean canLandHorizontally() {
        return !isSpheroid();
    }

    default boolean canTakeOffVertically() {
        return (isVSTOL() || isSpheroid()) && (getCurrentThrust() > 2);
    }

    default boolean canLandVertically() {
        return (isVSTOL() || isSpheroid());
    }

    default String hasRoomForHorizontalTakeOff() {
        // walk along the hexes in the facing of the unit
        Entity thisAero = ((Entity) this);
        if (!thisAero.game.hasBoardLocationOf(thisAero)) {
            return "Unit is not on a board";
        }
        Board board = thisAero.game.getBoard(thisAero);
        Hex hex = board.getHex(thisAero.getPosition());
        int elev = hex.getLevel();
        int facing = thisAero.getFacing();
        String lenString = " (" + getTakeOffLength() + " hexes required)";
        // dropships need a strip three hexes wide
        Vector<Coords> startingPos = new Vector<>();
        startingPos.add(thisAero.getPosition());
        if (this instanceof Dropship) {
            startingPos.add(thisAero.getPosition().translated((facing + 4) % 6));
            startingPos.add(thisAero.getPosition().translated((facing + 2) % 6));
        }
        for (Coords pos : startingPos) {
            for (int i = 0; i < getTakeOffLength(); i++) {
                pos = pos.translated(facing);
                // check for buildings
                if (board.getBuildingAt(pos) != null) {
                    return "Buildings in the way" + lenString;
                }
                // no units in the way
                for (Entity en : thisAero.game.getEntitiesVector(pos, board.getBoardId())) {
                    if (en.equals(this)) {
                        continue;
                    }

                    if (!en.isAirborne()) {
                        return "Ground units in the way" + lenString;
                    }
                }
                hex = board.getHex(pos);
                // if the hex is null, then we are offboard. Don't let units
                // take off offboard.
                if (null == hex) {
                    return "Not enough room on map" + lenString;
                }
                if (!hex.isClearForTakeoff()) {
                    return "Unacceptable terrain for take off" + lenString;
                }
                if (hex.getLevel() != elev) {
                    return "Runway must contain no elevation change" + lenString;
                }
            }
        }

        return null;
    }

    default String hasRoomForLanding(LandingDirection landingDirection) {
        return landingDirection.isHorizontal() ? hasRoomForHorizontalLanding() : hasRoomForVerticalLanding();
    }

    default String hasRoomForHorizontalLanding() {
        return hasRoomForHorizontalLanding(((Entity) this).getBoardId(), ((Entity) this).getPosition());
    }

    default String hasRoomForLanding(int assumedBoardId, Coords assumedPosition, LandingDirection landingDirection) {
        return landingDirection == LandingDirection.HORIZONTAL ?
              hasRoomForHorizontalLanding(assumedBoardId, assumedPosition) :
              hasRoomForVerticalLanding(assumedBoardId, assumedPosition);
    }

    default String hasRoomForHorizontalLanding(int assumedBoardId, Coords assumedPosition) {
        Entity thisEntity = ((Entity) this);
        Game game = thisEntity.getGame();
        if (game == null || !game.hasBoardLocationOf(thisEntity)) {
            return "Unit is not on a board.";
        }
        // walk along the hexes in the facing of the unit
        Board board = game.getBoard(assumedBoardId);
        Hex hex = board.getHex(assumedPosition);
        int elev = hex.getLevel();
        int facing = thisEntity.getFacing();
        String lenString = " (" + getLandingLength() + " hexes required)";
        // dropships need a landing strip three hexes wide
        Vector<Coords> startingPos = new Vector<>();
        startingPos.add(assumedPosition);
        if (this instanceof Dropship) {
            startingPos.add(assumedPosition.translated((facing + 5) % 6));
            startingPos.add(assumedPosition.translated((facing + 1) % 6));
        }
        for (Coords position : startingPos) {
            for (int i = 0; i < getLandingLength(); i++) {
                position = position.translated(facing);
                // check for buildings
                if (board.getBuildingAt(position) != null) {
                    return "Buildings in the way" + lenString;
                }
                // no units in the way
                for (Entity en : thisEntity.getGame().getEntitiesVector(position, assumedBoardId)) {
                    if (!en.isAirborne()) {
                        return "Ground units in the way" + lenString;
                    }
                }
                hex = board.getHex(position);
                // if the hex is null, then we are offboard. Don't let units land offboard.
                if (null == hex) {
                    return "Not enough room on map" + lenString;
                }
                // landing must contain only acceptable terrain
                if (!hex.isClearForLanding()) {
                    return "Unacceptable terrain for landing" + lenString;
                }

                if (hex.getLevel() != elev) {
                    return "Landing strip must contain no elevation change" + lenString;
                }
            }
        }
        return null;
    }

    default String hasRoomForVerticalLanding() {
        return hasRoomForVerticalLanding(((Entity) this).getBoardId(), ((Entity) this).getPosition());
    }

    default String hasRoomForVerticalLanding(int assumedBoardId, Coords assumedPosition) {
        Hex hex = ((Entity) this).getGame().getHex(assumedPosition, assumedBoardId);
        if (((Entity) this).getGame().getBuildingAt(assumedPosition, ((Entity) this).getBoardId()).isPresent()) {
            return "Buildings in the way";
        }
        // no units in the way
        for (Entity en : ((Entity) this).getGame().getEntitiesVector(assumedPosition, assumedBoardId)) {
            if (!en.isAirborne()) {
                return "Ground units in the way";
            }
        }
        // if the hex is null, then we are offboard. Don't let units
        // land offboard.
        if (null == hex) {
            return "landing area not on the map";
        }
        // landing must contain only acceptable terrain
        if (!hex.isClearForLanding()) {
            return "Unacceptable terrain for landing";
        }
        // Aerospace units are destroyed by water landings except for those that have
        // flotation hulls.
        // LAMs are not.
        if (hex.containsTerrain(Terrains.WATER) && !hex.containsTerrain(Terrains.ICE)
              && (hex.terrainLevel(Terrains.WATER) > 0)
              && (this instanceof Aero)
              && !((Entity) this).hasWorkingMisc(MiscType.F_FLOTATION_HULL)) {
            return "cannot land on water";
        }

        return null;
    }

    /**
     * Performs necessary changes to this aero unit to place it in airborne mode. Sets the altitude to the given
     * altitude, changes the movement mode to aerodyne/spheroid movement and clears secondary positions for DS. Note
     * that altitude should not be 0 but this is not checked.
     *
     * @param altitude The altitude to lift off to
     */
    default void liftOff(int altitude) {
        Entity aero = (Entity) this;
        aero.setMovementMode(isSpheroid() ? EntityMovementMode.SPHEROID : EntityMovementMode.AERODYNE);
        aero.setAltitude(altitude);

        HashSet<Coords> positions = aero.getOccupiedCoords();
        aero.getSecondaryPositions().clear();
        if (aero.getGame() != null) {
            aero.getGame().updateEntityPositionLookup(aero, positions);
        }
    }

    /**
     * Performs necessary changes to this aero unit to place it in grounded mode. Sets altitude and elevation, velocity
     * and next velocity to 0, OOC and related effects to false and the movement mode to WHEELED.
     */
    default void land() {
        Entity aero = (Entity) this;
        aero.setMovementMode(EntityMovementMode.WHEELED);
        aero.setAltitude(0);
        aero.setElevation(0);
        setCurrentVelocity(0);
        setNextVelocity(0);
        setOutControl(false);
        setOutControlHeat(false);
        setRandomMove(false);
        aero.delta_distance = 0;
    }

    default int getFuelUsed(int thrust) {
        Entity entity = (Entity) this;
        if (!entity.hasEngine() || !requiresFuel()) {
            return 0;
        } else {
            int overThrust = Math.max(thrust - entity.getWalkMP(), 0);
            int safeThrust = thrust - overThrust;
            return safeThrust + (2 * overThrust);
        }
    }

    /***
     * use the specified amount of fuel for this Aero. The amount may be
     * adjusted by certain game options
     *
     * @param fuelUsed
     *                 The number of fuel points to use
     */
    default void useFuel(int fuelUsed) {
        setCurrentFuel(Math.max(0, getCurrentFuel() - fuelUsed));
    }

    /**
     * A method to add/remove sensors that only work in space as we transition in and out of an atmosphere
     */
    default void updateSensorOptions() {

    }

    /**
     * Check which turn engines were destroyed in.
     */
    default int getEnginesLostRound() {
        return Integer.MAX_VALUE;
    }

    /**
     * Set round that engines were completely destroyed; needed for crash-landing check
     *
     */
    void setEnginesLostRound(int round);

    /**
     * Check if the specified hex is a prohibited terrain for Aero units to taxi into
     *
     * @param hex the hex to check
     *
     * @return true if the hex is a prohibited terrain for Aero units to taxi into
     */
    default boolean taxingAeroProhibitedTerrains(@Nullable Hex hex) {
        return (hex == null) || // It is illegal to taxi offboard
              hex.containsTerrain(Terrains.WOODS) ||
              hex.containsTerrain(Terrains.ROUGH) ||
              ((hex.terrainLevel(Terrains.WATER) > 0) && !hex.containsTerrain(Terrains.ICE)) ||
              hex.containsTerrain(Terrains.RUBBLE) ||
              hex.containsTerrain(Terrains.MAGMA) ||
              hex.containsTerrain(Terrains.JUNGLE) ||
              (hex.terrainLevel(Terrains.SNOW) > 1) ||
              (hex.terrainLevel(Terrains.GEYSER) == 2);
    }

}
