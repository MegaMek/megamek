/*
 * MegaMek - Copyright (C) 2017 - The MegaMek Team
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

import java.util.HashSet;
import java.util.Set;

import megamek.common.MovePath.MoveStepType;

/**
 * Methods shared by Aero and LandAirMech
 * 
 * @author Neoancient
 *
 */
public interface IAero {
    
    // bombs
    public static final String SPACE_BOMB_ATTACK = "SpaceBombAttack";
    public static final String DIVE_BOMB_ATTACK = "DiveBombAttack";
    public static final String ALT_BOMB_ATTACK = "AltBombAttack";

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
    public void resetAltLossThisRound();
    
    int getTakeOffLength();
    int getLandingLength();
    boolean canTakeOffHorizontally();
    boolean canLandHorizontally();
    
    int getNoseArmor();
    void setSI(int si);
    int getSI();
    int get0SI();
    int getAvionicsHits();
    int getSensorHits();
    boolean hasLifeSupport();
    void setGearHit(boolean hit);
    int getLandingGearMod(boolean vTakeoff);
    int getLeftThrustHits();
    int getRightThrustHits();
    boolean wasCritThresh();
    void setCritThresh(boolean b);

    int getMaxBombPoints();
    int[] getBombChoices();
    void setBombChoices(int[] bc);
    void applyBombs();
            
    int getFuel();
    void setFuel(int gas);
    double getFuelPointsPerTon();
    /**
     * Set number of fuel points based on fuel tonnage.
     *
     * @param fuelTons  The number of tons of fuel
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

    /* Entity methods needed by default methods */
    Entity getEntity();
    
    default PilotingRollData checkThrustSI(int thrust, EntityMovementType overallMoveType) {
        PilotingRollData roll = getEntity().getBasePilotingRoll(overallMoveType);

        if (thrust > getSI()) {
            // append the reason modifier
            roll.append(new PilotingRollData(getEntity().getId(), thrust - getSI(), "Thrust exceeds current SI in a single hex"));
        } else {
            roll.addModifier(TargetRoll.CHECK_FALSE, "Check false: Entity is not exceeding SI");
        }
        return roll;
    }

    default PilotingRollData checkThrustSITotal(int thrust, EntityMovementType overallMoveType) {
        PilotingRollData roll = getEntity().getBasePilotingRoll(overallMoveType);

        if (thrust > getSI()) {
            // append the reason modifier
            roll.append(new PilotingRollData(getEntity().getId(), 0, "Thrust spent this turn exceeds current SI"));
        } else {
            roll.addModifier(TargetRoll.CHECK_FALSE, "Check false: Entity is not exceeding SI");
        }
        return roll;
    }

    default PilotingRollData checkVelocityDouble(int velocity, EntityMovementType overallMoveType) {
        PilotingRollData roll = getEntity().getBasePilotingRoll(overallMoveType);

        if ((velocity > (2 * getEntity().getWalkMP())) && !getEntity().getGame().getBoard().inSpace()) {
            // append the reason modifier
            roll.append(new PilotingRollData(getEntity().getId(), 0, "Velocity greater than 2x safe thrust"));
        } else {
            roll.addModifier(TargetRoll.CHECK_FALSE, "Check false: Entity is not exceeding 2x safe thrust");
        }
        return roll;
    }

    default PilotingRollData checkDown(int drop, EntityMovementType overallMoveType) {
        PilotingRollData roll = getEntity().getBasePilotingRoll(overallMoveType);

        if (drop > 2) {
            // append the reason modifier
            roll.append(new PilotingRollData(getEntity().getId(), drop, "lost more than two altitudes"));
        } else {
            roll.addModifier(TargetRoll.CHECK_FALSE, "Check false: entity did not drop more than two altitudes");
        }
        return roll;
    }

    default PilotingRollData checkHover(MovePath md) {
        PilotingRollData roll = getEntity().getBasePilotingRoll(md.getLastStepMovementType());

        if (md.contains(MoveStepType.HOVER) && (md.getLastStepMovementType() == EntityMovementType.MOVE_OVER_THRUST)) {
            // append the reason modifier
            roll.append(new PilotingRollData(getEntity().getId(), 0, "hovering above safe thrust"));
        } else {
            roll.addModifier(TargetRoll.CHECK_FALSE, "Check false: entity did not hover");
        }
        return roll;
    }

    default PilotingRollData checkStall(MovePath md) {
        PilotingRollData roll = getEntity().getBasePilotingRoll(md.getLastStepMovementType());

        if ((md.getFinalVelocity() == 0) && !md.contains(MoveStepType.HOVER)
                && isAirborne() && !isSpheroid() && !getEntity().getGame().getBoard().inSpace()
                && !md.contains(MoveStepType.LAND)
                && !md.contains(MoveStepType.VLAND)
                && !md.contains(MoveStepType.RETURN)
                && !md.contains(MoveStepType.OFF)
                && !md.contains(MoveStepType.FLEE)) {
            // append the reason modifier
            roll.append(new PilotingRollData(getEntity().getId(), 0, "stalled out"));
        } else {
            roll.addModifier(TargetRoll.CHECK_FALSE, "Check false: entity not stalled out");
        }
        return roll;
    }

    default PilotingRollData checkRolls(MoveStep step, EntityMovementType overallMoveType) {
        PilotingRollData roll = getEntity().getBasePilotingRoll(overallMoveType);

        if (((step.getType() == MoveStepType.ROLL) || (step.getType() == MoveStepType.YAW)) && (step.getNRolls() > 1)) {
            // append the reason modifier
            roll.append(new PilotingRollData(getEntity().getId(), 0, "More than one roll in the same turn"));
        } else {
            roll.addModifier(TargetRoll.CHECK_FALSE, "Check false: Entity is not rolling more than once");
        }
        return roll;
    }

    default PilotingRollData checkVerticalTakeOff() {
        PilotingRollData roll = getEntity().getBasePilotingRoll(EntityMovementType.MOVE_SAFE_THRUST);

        if (getLandingGearMod(true) > 0) {
            roll.addModifier(+1, "landing gear damaged");
        }

        if (getLeftThrustHits() + getRightThrustHits() > 0) {
            roll.addModifier(+3, "Maneuvering thrusters damaged");
        }

        // Supposed to be -1 for lifting off from an "airfield or landing pad."
        // We will just treat this as having paved terrain
        Coords pos = getEntity().getPosition();
        IHex hex = getEntity().getGame().getBoard().getHex(pos);
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
        Set<Coords> positions = new HashSet<Coords>(getEntity().getSecondaryPositions()
                .values());
        IHex adjHex;
        for (Coords currPos : positions) {
            hex = getEntity().getGame().getBoard().getHex(currPos);
            for (int dir = 0; dir < 6; dir++) {
                Coords adj = currPos.translated(dir);
                adjHex = getEntity().getGame().getBoard().getHex(adj);
                if (!positions.contains(adj) && (adjHex != null)
                        && adjHex.getLevel() <= hex.getLevel()) {
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
     * @param velocity      Velocity when the check is to be made, this needs to
     *                      be passed as the check could happen as part of a 
     *                      Move Path
     * @param landingPos    The final position the Aero will land on.
     * @param isVertical    If this a vertical or horizontal landing
     * @return              A PilotingRollData tha represents the landing
     *                      control roll that must be passed
     */
    default PilotingRollData checkLanding(EntityMovementType moveType,
            int velocity, Coords landingPos, int face, boolean isVertical) {
        // Base piloting skill
        PilotingRollData roll = new PilotingRollData(getEntity().getId(), getEntity().getCrew()
                .getPiloting(), "Base piloting skill");
        
        
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
        double thrustPercent = ((double)getEntity().getWalkMP())/getEntity().getOriginalWalkMP();
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
        // terrain mods
        boolean lightWoods = false;
        boolean rough = false;
        boolean heavyWoods = false;
        boolean clear = false;
        boolean paved = true;
        
        Set<Coords> landingPositions = new HashSet<Coords>();
        boolean isDropship = (this instanceof Dropship);
        // Vertical landing just checks the landing hex
        if (isVertical) {
            landingPositions.add(landingPos);
            // Dropships must also check the adjacent 6 hexes
            if (isDropship) {
                for (int i = 0; i < 6; i++) {
                    landingPositions.add(landingPos.translated(i));
                }
            }
        // Horizontal landing requires checking whole landing strip
        } else {
            for (int i = 0; i < getLandingLength(); i++) {
                Coords pos = landingPos.translated(face, i);
                landingPositions.add(pos);
                // Dropships have to check the front adjacent hexes
                if (isDropship) {
                    landingPositions.add(pos.translated((face + 4) % 6));
                    landingPositions.add(pos.translated((face + 2) % 6));
                }
            }                
        }
        
        for (Coords pos : landingPositions) {
            IHex hex = getEntity().getGame().getBoard().getHex(pos);
            if (hex.containsTerrain(Terrains.ROUGH)
                    || hex.containsTerrain(Terrains.RUBBLE)) {
                rough = true;
            } else if (hex.containsTerrain(Terrains.WOODS, 2)) {
                heavyWoods = true;
            } else if (hex.containsTerrain(Terrains.WOODS, 1)) {
                lightWoods = true;
            } else if (!hex.containsTerrain(Terrains.PAVEMENT)
                    && !hex.containsTerrain(Terrains.ROAD)) {
                paved = false;
                // Landing in other terrains isn't allowed, so if we reach here
                // it must be a clear hex
                clear = true;
            } 
        }

        if (heavyWoods) {
            roll.addModifier(+5, "heavy woods in landing path");
        }
        if (lightWoods) {
            roll.addModifier(+4, "light woods in landing path");
        }
        if (rough) {
            roll.addModifier(+3, "rough/rubble in landing path");
        }
        if (paved) {
            roll.addModifier(+0, "paved/road landing strip");
        }
        if (clear) {
            roll.addModifier(+2, "clear hex in landing path");
        }

        return roll;
    }

    /**
     * Checks if a maneuver requires a control roll
     */
    default PilotingRollData checkManeuver(MoveStep step,
            EntityMovementType overallMoveType) {
        PilotingRollData roll = getEntity().getBasePilotingRoll(overallMoveType);

        if ((step == null) || (step.getType() != MoveStepType.MANEUVER)) {
            roll.addModifier(TargetRoll.CHECK_FALSE,
                    "Check false: Entity is not attempting to get up.");
            return roll;
        }
        boolean sideSlipMod = (this instanceof ConvFighter) && isVSTOL();
        roll.append(new PilotingRollData(getEntity().getId(), ManeuverType.getMod(
                step.getManeuverType(), sideSlipMod), ManeuverType
                .getTypeName(step.getManeuverType()) + " maneuver"));

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

    default void liftOff(int altitude) {
        if (isSpheroid()) {
            getEntity().setMovementMode(EntityMovementMode.SPHEROID);
        } else {
            getEntity().setMovementMode(EntityMovementMode.AERODYNE);
        }
        getEntity().setAltitude(altitude);

        HashSet<Coords> positions = getEntity().getOccupiedCoords();
        getEntity().getSecondaryPositions().clear();
        if (getEntity().getGame() != null) {
            getEntity().getGame().updateEntityPositionLookup((Entity)this, positions);
        }
    }

    default void land() {
        getEntity().setMovementMode(EntityMovementMode.WHEELED);
        getEntity().setAltitude(0);
        getEntity().setElevation(0);
        setCurrentVelocity(0);
        setNextVelocity(0);
        setOutControl(false);
        setOutCtrlHeat(false);
        setRandomMove(false);
        getEntity().delta_distance = 0;
    }

    default int getFuelUsed(int thrust) {
        int overThrust = Math.max(thrust - getEntity().getWalkMP(), 0);
        int safeThrust = thrust - overThrust;
        int used = safeThrust + (2 * overThrust);
        return used;
    }

    /***
     * use the specified amount of fuel for this Aero. The amount may be
     * adjusted by certain game options
     *
     * @param fuel  The number of fuel points to use
     */
    default void useFuel(int fuelUsed) {
        setFuel(Math.max(0, getFuel() - fuelUsed));
    }

}
