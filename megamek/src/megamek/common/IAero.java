/*
* MegaMek -
* Copyright (C) 2017 The MegaMek Team
*
* This program is free software; you can redistribute it and/or modify it under
* the terms of the GNU General Public License as published by the Free Software
* Foundation; either version 2 of the License, or (at your option) any later
* version.
*
* This program is distributed in the hope that it will be useful, but WITHOUT
* ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
* FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
* details.
*/

package megamek.common;

import java.util.*;

import megamek.common.MovePath.MoveStepType;
import org.apache.logging.log4j.LogManager;

/**
 * Methods shared by Aero and LandAirMech
 *
 * @author Neoancient
 *
 */
public interface IAero {

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

    void setOutControl(boolean ocontrol);

    boolean isOutCtrlHeat();

    void setOutCtrlHeat(boolean octrlheat);

    boolean isRandomMove();

    void setRandomMove(boolean randmove);

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

    int get0SI();

    int getAvionicsHits();

    int getSensorHits();

    int getFCSHits();

    int getLandingGearPartialRepairs();

    int getAvionicsMisreplaced();

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

    boolean wasCritThresh();

    void setCritThresh(boolean b);

    int getFuel();

    void setFuel(int gas);

    int getCurrentFuel();

    void setCurrentFuel(int gas);

    double getFuelPointsPerTon();

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
     * Iterate through current weapons and count the number in each capital
     * fighter location.
     *
     * @return A map with keys in the format "weaponName:loc", with the number
     *         of weapons of that type in that location as the value.
     */
    Map<String, Integer> groupWeaponsByLocation();

    Map<String, Integer> getWeaponGroups();

    /**
     * Refresh the capital fighter weapons groups.
     */
    default void updateWeaponGroups() {
        // first we need to reset all the weapons in our existing mounts to zero
        // until proven otherwise
        Set<String> set = getWeaponGroups().keySet();
        Iterator<String> iter = set.iterator();
        while (iter.hasNext()) {
            String key = iter.next();
            ((Entity) this).getEquipment(getWeaponGroups().get(key)).setNWeapons(0);
        }
        // now collect a hash of all the same weapons in each location by id
        Map<String, Integer> groups = groupWeaponsByLocation();
        // now we just need to traverse the hash and either update our existing
        // equipment or add new ones if there is none
        Set<String> newSet = groups.keySet();
        Iterator<String> newIter = newSet.iterator();
        while (newIter.hasNext()) {
            String key = newIter.next();
            if (null != getWeaponGroups().get(key)) {
                // then this equipment is already loaded, so we just need to
                // correctly update the number of weapons
                ((Entity) this).getEquipment(getWeaponGroups().get(key)).setNWeapons(groups.get(key));
            } else {
                // need to add a new weapon
                String name = key.split(":")[0];
                int loc = Integer.parseInt(key.split(":")[1]);
                EquipmentType etype = EquipmentType.get(name);
                Mounted newmount;
                if (etype != null) {
                    try {
                        newmount = ((Entity) this).addWeaponGroup(etype, loc);
                        newmount.setNWeapons(groups.get(key));
                        getWeaponGroups().put(key, ((Entity) this).getEquipmentNum(newmount));
                    } catch (LocationFullException ex) {
                        LogManager.getLogger().error("Unable to compile weapon groups", ex);
                        return;
                    }
                } else if (!"0".equals(name)) {
                    ((Entity) this).addFailedEquipment(name);
                }
            }
        }
    }

    /**
     * Set number of fuel points based on fuel tonnage.
     *
     * @param fuelTons
     *            The number of tons of fuel
     */
    void setFuelTonnage(double fuelTons);

    /**
     * Gets the fuel for this Aero in terms of tonnage.
     *
     * @return The number of tons of fuel on this Aero.
     */
    double getFuelTonnage();

    /*
     * Default methods that are implemented the same for Aero and LandAirMech
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

    default PilotingRollData checkThrustSITotal(int thrust, EntityMovementType overallMoveType) {
        PilotingRollData roll = ((Entity) this).getBasePilotingRoll(overallMoveType);

        if (thrust > getSI()) {
            // append the reason modifier
            roll.append(new PilotingRollData(((Entity) this).getId(), 0, "Thrust spent this turn exceeds current SI"));
        } else {
            roll.addModifier(TargetRoll.CHECK_FALSE, "Check false: Entity is not exceeding SI");
        }
        return roll;
    }

    default PilotingRollData checkVelocityDouble(int velocity, EntityMovementType overallMoveType) {
        PilotingRollData roll = ((Entity) this).getBasePilotingRoll(overallMoveType);

        if ((velocity > (2 * ((Entity) this).getWalkMP())) && !((Entity) this).getGame().getBoard().inSpace()) {
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
        PilotingRollData roll = ((Entity) this).getBasePilotingRoll(md.getLastStepMovementType());

        // if the entity has already moved, its movement got interrupted (probably by a hidden unit, not much else can interrupt an aero unit)
        // in which case, the movement is complete. We just need to allow the user to hit 'done'.
        if (((Entity) this).delta_distance > 0) {
            roll.addModifier(TargetRoll.CHECK_FALSE, "Check false: aero has already moved");
        // an airborne, aerodyne aero is considered to "stall" if it's not moving anywhere,
        // hovering, landing, or going off board
        } else if ((md.getFinalVelocity() == 0) && !md.contains(MoveStepType.HOVER) && isAirborne() && !isSpheroid()
                && !((Entity) this).getGame().getBoard().inSpace() && !md.contains(MoveStepType.LAND)
                && !md.contains(MoveStepType.VLAND) && !md.contains(MoveStepType.RETURN)
                && !md.contains(MoveStepType.OFF) && !md.contains(MoveStepType.FLEE)) {
            roll.append(new PilotingRollData(((Entity) this).getId(), 0, "stalled out"));
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
        Hex hex = ((Entity) this).getGame().getBoard().getHex(pos);
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
            hex = ((Entity) this).getGame().getBoard().getHex(currPos);
            for (int dir = 0; dir < 6; dir++) {
                Coords adj = currPos.translated(dir);
                adjHex = ((Entity) this).getGame().getBoard().getHex(adj);
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
     * Compute the PilotingRollData for a landing control roll (see TW pg 86).
     *
     * @param moveType
     * @param velocity
     *            Velocity when the check is to be made, this needs to be passed
     *            as the check could happen as part of a Move Path
     * @param landingPos
     *            The final position the Aero will land on.
     * @param isVertical
     *            If this a vertical or horizontal landing
     * @return A PilotingRollData tha represents the landing control roll that
     *         must be passed
     */
    default PilotingRollData checkLanding(EntityMovementType moveType, int velocity, Coords landingPos, int face,
            boolean isVertical) {
        // Base piloting skill
        PilotingRollData roll = new PilotingRollData(((Entity) this).getId(), ((Entity) this).getCrew().getPiloting(),
                "Base piloting skill");

        // Apply critical hit effects, TW pg 239
        int avihits = getAvionicsHits();
        if ((avihits > 0) && (avihits < 3)) {
            roll.addModifier(avihits, "Avionics Damage");
        }

        // this should probably be replaced with some kind of AVI_DESTROYED
        // boolean
        if (avihits >= 3) {
            roll.addModifier(5, "Avionics Destroyed");
        }

        if (!hasLifeSupport()) {
            roll.addModifier(+2, "No life support");
        }

        // Landing Gear Partial Repairs, only apply if the landing gear isn't currently damaged
        if (getLandingGearMod(false) == 0) {
            if (getLandingGearPartialRepairs() == 2) {
                roll.addModifier(getLandingGearPartialRepairs(), "landing gear misrepaired");
            } else if (getLandingGearPartialRepairs() == 1) {
                roll.addModifier(getLandingGearPartialRepairs(), "landing gear misreplaced");
            }
        }

        // Avionics Partial Repairs, only apply if the Avionics package isn't destroyed
        if (avihits < 3) {
            if (getAvionicsMisrepaired() == 1) {
                roll.addModifier(1, "misrepaired avionics");
            } if (getAvionicsMisreplaced() == 1) {
                roll.addModifier(1, "misreplaced avionics");
            }
        }
        // Landing Modifiers table, TW pg 86
        int velmod;
        if (isVertical) {
            velmod = Math.max(0, velocity - 1);
        } else {
            velmod = Math.max(0, velocity - 2);
        }
        if (velmod > 0) {
            roll.addModifier(velmod, "excess velocity");
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
        for (Coords pos : landingPositions) {
            Hex hex = ((Entity) this).getGame().getBoard().getHex(pos);
            if ((hex == null) || hex.hasPavement()) {
                continue;
            }
            if (hex.isClearHex()) {
                clear = true;
            } else {
                for (int terrain : hex.getTerrainTypes()) {
                    if ((terrain == Terrains.WATER) && hex.containsTerrain(Terrains.ICE)) {
                        continue;
                    }
                    if (Terrains.landingModifier(terrain, hex.terrainLevel(terrain)) > 0) {
                        terrains.add(List.of(terrain, hex.terrainLevel(terrain)));
                    }
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
        Set<Coords> landingPositions = new HashSet<Coords>();
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
            if (arc == Compute.ARC_LWING) {
                return Compute.ARC_RWING;
            } else if (arc == Compute.ARC_RWING) {
                return Compute.ARC_LWING;
            } else if (arc == Compute.ARC_LWINGA) {
                return Compute.ARC_RWINGA;
            } else if (arc == Compute.ARC_RWINGA) {
                return Compute.ARC_LWINGA;
            } else if (arc == Compute.ARC_LEFTSIDE_SPHERE) {
                return Compute.ARC_RIGHTSIDE_SPHERE;
            } else if (arc == Compute.ARC_RIGHTSIDE_SPHERE) {
                return Compute.ARC_LEFTSIDE_SPHERE;
            } else if (arc == Compute.ARC_LEFTSIDEA_SPHERE) {
                return Compute.ARC_RIGHTSIDEA_SPHERE;
            } else if (arc == Compute.ARC_RIGHTSIDEA_SPHERE) {
                return Compute.ARC_LEFTSIDEA_SPHERE;
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
        Hex hex = ((Entity) this).getGame().getBoard().getHex(((Entity) this).getPosition());
        int elev = hex.getLevel();
        int facing = ((Entity) this).getFacing();
        String lenString = " (" + getTakeOffLength() + " hexes required)";
        // dropships need a strip three hexes wide
        Vector<Coords> startingPos = new Vector<>();
        startingPos.add(((Entity) this).getPosition());
        if (this instanceof Dropship) {
            startingPos.add(((Entity) this).getPosition().translated((facing + 4) % 6));
            startingPos.add(((Entity) this).getPosition().translated((facing + 2) % 6));
        }
        for (Coords pos : startingPos) {
            for (int i = 0; i < getTakeOffLength(); i++) {
                pos = pos.translated(facing);
                // check for buildings
                if (((Entity) this).getGame().getBoard().getBuildingAt(pos) != null) {
                    return "Buildings in the way" + lenString;
                }
                // no units in the way
                for (Entity en : ((Entity) this).getGame().getEntitiesVector(pos)) {
                    if (en.equals(this)) {
                        continue;
                    }

                    if (!en.isAirborne()) {
                        return "Ground units in the way" + lenString;
                    }
                }
                hex = ((Entity) this).getGame().getBoard().getHex(pos);
                // if the hex is null, then we are offboard. Don't let units
                // take off offboard.
                if (null == hex) {
                    return "Not enough room on map" + lenString;
                }
                if (!hex.isClearForTakeoff()) {
                    return "Unacceptable terrain for landing" + lenString;
                }
                if (hex.getLevel() != elev) {
                    return "Runway must contain no elevation change" + lenString;
                }
            }
        }

        return null;
    }

    default String hasRoomForHorizontalLanding() {
        // walk along the hexes in the facing of the unit
        Hex hex = ((Entity) this).getGame().getBoard().getHex(((Entity) this).getPosition());
        int elev = hex.getLevel();
        int facing = ((Entity) this).getFacing();
        String lenString = " (" + getLandingLength() + " hexes required)";
        // dropships need a a landing strip three hexes wide
        Vector<Coords> startingPos = new Vector<>();
        startingPos.add(((Entity) this).getPosition());
        if (this instanceof Dropship) {
            startingPos.add(((Entity) this).getPosition().translated((facing + 5) % 6));
            startingPos.add(((Entity) this).getPosition().translated((facing + 1) % 6));
        }
        for (Coords pos : startingPos) {
            for (int i = 0; i < getLandingLength(); i++) {
                pos = pos.translated(facing);
                // check for buildings
                if (((Entity) this).getGame().getBoard().getBuildingAt(pos) != null) {
                    return "Buildings in the way" + lenString;
                }
                // no units in the way
                for (Entity en : ((Entity) this).getGame().getEntitiesVector(pos)) {
                    if (!en.isAirborne()) {
                        return "Ground units in the way" + lenString;
                    }
                }
                hex = ((Entity) this).getGame().getBoard().getHex(pos);
                // if the hex is null, then we are offboard. Don't let units
                // land offboard.
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
        Coords pos = ((Entity) this).getPosition();
        Hex hex = ((Entity) this).getGame().getBoard().getHex(((Entity) this).getPosition());
        if (((Entity) this).getGame().getBoard().getBuildingAt(pos) != null) {
            return "Buildings in the way";
        }
        // no units in the way
        for (Entity en : ((Entity) this).getGame().getEntitiesVector(pos)) {
            if (!en.isAirborne()) {
                return "Ground units in the way";
            }
        }
        hex = ((Entity) this).getGame().getBoard().getHex(pos);
        // if the hex is null, then we are offboard. Don't let units
        // land offboard.
        if (null == hex) {
            return "landing area not on the map";
        }
        // landing must contain only acceptable terrain
        if (!hex.isClearForLanding()) {
            return "Unacceptable terrain for landing";
        }
        // Aerospace units are destroyed by water landings except for those that have flotation hulls.
        // LAMs are not.
        if (hex.containsTerrain(Terrains.WATER) && !hex.containsTerrain(Terrains.ICE)
                && (hex.terrainLevel(Terrains.WATER) > 0)
                && (this instanceof Aero)
                && !((Entity) this).hasWorkingMisc(MiscType.F_FLOTATION_HULL)) {
            return "cannot land on water";
        }

        return null;
    }

    default void liftOff(int altitude) {
        if (isSpheroid()) {
            ((Entity) this).setMovementMode(EntityMovementMode.SPHEROID);
        } else {
            ((Entity) this).setMovementMode(EntityMovementMode.AERODYNE);
        }
        ((Entity) this).setAltitude(altitude);

        HashSet<Coords> positions = ((Entity) this).getOccupiedCoords();
        ((Entity) this).getSecondaryPositions().clear();
        if (((Entity) this).getGame() != null) {
            ((Entity) this).getGame().updateEntityPositionLookup((Entity) this, positions);
        }
    }

    default void land() {
        ((Entity) this).setMovementMode(EntityMovementMode.WHEELED);
        ((Entity) this).setAltitude(0);
        ((Entity) this).setElevation(0);
        setCurrentVelocity(0);
        setNextVelocity(0);
        setOutControl(false);
        setOutCtrlHeat(false);
        setRandomMove(false);
        ((Entity) this).delta_distance = 0;
    }

    default int getFuelUsed(int thrust) {
        Entity entity = (Entity) this;
        if (entity.hasEngine() && entity.getEngine().isSolar()) {
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
     *            The number of fuel points to use
     */
    default void useFuel(int fuelUsed) {
        setCurrentFuel(Math.max(0, getCurrentFuel() - fuelUsed));
    }

    /**
     * A method to add/remove sensors that only work in space as we transition in and out of an atmosphere
     */
    default void updateSensorOptions() {

    }
}
