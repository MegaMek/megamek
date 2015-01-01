/**
 * MegaMek - Copyright (C) 2004 Ben Mazur (bmazur@sev.org)
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
/*
 * Created on Jun 1, 2005
 *
 */
package megamek.common;

/**
 * @author Andrew Hunter VTOLs are helicopters (more or less.) They don't really
 *         work properly yet. Don't use them.
 */
public class VTOL extends Tank {

    /**
     * 
     */
    private static final long serialVersionUID = -7406911547399249173L;

    public static final int LOC_ROTOR = 5; // will this cause problems w/r/t
                                            // turrets?

    protected static String[] LOCATION_ABBRS = { "BD", "FR", "RS", "LS", "RR",
            "RO" };
    protected static String[] LOCATION_NAMES = { "Body", "Front", "Right",
            "Left", "Rear", "Rotor" };

    // critical hits
    public static final int CRIT_COPILOT = 15;
    public static final int CRIT_PILOT = 16;
    public static final int CRIT_ROTOR_DAMAGE = 17;
    public static final int CRIT_ROTOR_DESTROYED = 18;
    public static final int CRIT_FLIGHT_STABILIZER = 19;

    public VTOL() {
        super();
    }

    public String[] getLocationAbbrs() {
        return LOCATION_ABBRS;
    }

    public String[] getLocationNames() {
        return LOCATION_NAMES;
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.Entity#checkSkid(int, megamek.common.IHex, int,
     *      megamek.common.MoveStep, int, int, megamek.common.Coords,
     *      megamek.common.Coords, boolean, int)
     */
    public PilotingRollData checkSkid(int moveType, IHex prevHex,
            int overallMoveType, MoveStep prevStep, int prevFacing,
            int curFacing, Coords lastPos, Coords curPos, boolean isInfantry,
            int distance) {
        PilotingRollData roll = getBasePilotingRoll(overallMoveType);
        roll.addModifier(TargetRoll.CHECK_FALSE,
                "Check false: VTOLs can't skid");
        return roll;
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.Tank#canCharge()
     */
    public boolean canCharge() {
        return false;
    }

    /**
     * Returns the name of the type of movement used. This is VTOL-specific.
     */
    public String getMovementString(int mtype) {
        switch (mtype) {
            case IEntityMovementType.MOVE_VTOL_WALK:
                return "Cruised";
            case IEntityMovementType.MOVE_VTOL_RUN:
                return "Flanked";
            case IEntityMovementType.MOVE_NONE:
                return "None";
            default:
                return "Unknown!";
        }
    }

    /**
     * Returns the name of the type of movement used. This is tank-specific.
     */
    public String getMovementAbbr(int mtype) {
        switch (mtype) {
            case IEntityMovementType.MOVE_VTOL_WALK:
                return "C";
            case IEntityMovementType.MOVE_VTOL_RUN:
                return "F";
            case IEntityMovementType.MOVE_NONE:
                return "N";
            default:
                return "?";
        }
    }

    public int getMaxElevationChange() {
        return 999;
    }

    public boolean isHexProhibited(IHex hex) {
        if (hex.containsTerrain(Terrains.IMPASSABLE))
            return true;
        
        if(hex.containsTerrain(Terrains.SPACE) && doomedInSpace())
            return true;
        
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.Tank#isRepairable()
     */
    public boolean isRepairable() {
        boolean retval = this.isSalvage();
        int loc = Tank.LOC_FRONT;
        while (retval && loc < VTOL.LOC_ROTOR) {
            int loc_is = this.getInternal(loc);
            loc++;
            retval = (loc_is != IArmorState.ARMOR_DOOMED)
                    && (loc_is != IArmorState.ARMOR_DESTROYED);
        }
        return retval;
    }

    /*
     * (non-Javadoc) This really, really isn't right.
     */
    public HitData rollHitLocation(int table, int side, int aimedLocation,
            int aimingMode) {
        int nArmorLoc = LOC_FRONT;
        boolean bSide = false;
        if (side == ToHitData.SIDE_LEFT) {
            nArmorLoc = LOC_LEFT;
            bSide = true;
        } else if (side == ToHitData.SIDE_RIGHT) {
            nArmorLoc = LOC_RIGHT;
            bSide = true;
        } else if (side == ToHitData.SIDE_REAR) {
            nArmorLoc = LOC_REAR;
        }
        HitData rv = new HitData(nArmorLoc);
        boolean bHitAimed = false;
        if ((aimedLocation != LOC_NONE)
                && (aimingMode != IAimingModes.AIM_MODE_NONE)) {
            
            int roll = Compute.d6(2);

            if ((5 < roll) && (roll < 9)) {
                rv = new HitData(aimedLocation, side == ToHitData.SIDE_REAR,
                        true);
                bHitAimed = true;
            }
        }
        if (!bHitAimed) {
            switch (Compute.d6(2)) {
            case 2:
                rv.setEffect(HitData.EFFECT_CRITICAL);
                break;
            case 3:
            case 4:
                rv = new HitData(LOC_ROTOR, false,
                        HitData.EFFECT_VEHICLE_MOVE_DAMAGED);
                break;
            case 5:
                if (bSide)
                    rv = new HitData(LOC_FRONT);
                else
                    rv = new HitData(LOC_RIGHT);
                break;
            case 6:
            case 7:
                break;
            case 8:
                if (bSide && !game.getOptions().booleanOption("tacops_vehicle_effective")) {
                    rv.setEffect(HitData.EFFECT_CRITICAL);
                }
                break;
            case 9:
                if (bSide)
                    rv = new HitData(LOC_REAR);
                else
                    rv = new HitData(LOC_LEFT);
                break;
            case 10:
            case 11:
                rv = new HitData(LOC_ROTOR, false,
                        HitData.EFFECT_VEHICLE_MOVE_DAMAGED);
                break;
            case 12:
                rv = new HitData(LOC_ROTOR, false, HitData.EFFECT_CRITICAL
                        | HitData.EFFECT_VEHICLE_MOVE_DAMAGED);
            }
        }
        if (table == ToHitData.HIT_SWARM)
            rv.setEffect(rv.getEffect() | HitData.EFFECT_CRITICAL);
        return rv;
    }

    public boolean doomedInVacuum() {
        return true;
    }
    
    public boolean doomedInAtmosphere() {
        return true;
    }
    
    public boolean doomedInSpace() {
        return true;
    }

    public void setOnFire(boolean inferno) {
        super.setOnFire(inferno);
        extinguishLocation(LOC_ROTOR);
    }

    /**
     * get the type of critical caused by a critical roll, taking account of
     * existing damage
     * 
     * @param roll the final dice roll
     * @param loc the hit location
     * @return a critical type
     */
    public int getCriticalEffect(int roll, int loc) {
        if (roll > 12)
            roll = 12;
        if (roll < 6)
            return CRIT_NONE;
        for (int i = 0; i < 2; i++) {
            if (i > 0)
                roll = 6;
            if (loc == LOC_FRONT) {
                switch (roll) {
                    case 6:
                        if (!isDriverHit())
                            return CRIT_COPILOT;
                        else if (!crew.isDead() && !crew.isDoomed())
                            return CRIT_CREW_KILLED;
                    case 7:
                        for (Mounted m : getWeaponList()) {
                            if (m.getLocation() == loc && !m.isDestroyed()
                                    && !m.isJammed() && !m.isHit()) {
                                return CRIT_WEAPON_JAM;
                            }
                        }
                    case 8:
                        if (!isStabiliserHit(loc)) {
                            for (Mounted m : getWeaponList()) {
                                if (m.getLocation() == loc) {
                                    return CRIT_STABILIZER;
                                }
                            }
                        }
                    case 9:
                        if (getSensorHits() < 4)
                            return CRIT_SENSOR;
                    case 10:
                        if (!isCommanderHit())
                            return CRIT_PILOT;
                        else if (!crew.isDead() && !crew.isDoomed()) {
                            return CRIT_CREW_KILLED;
                        }
                    case 11:
                        for (Mounted m : getWeaponList()) {
                            if (m.getLocation() == loc && !m.isDestroyed()
                                    && !m.isHit()) {
                                return CRIT_WEAPON_DESTROYED;
                            }
                        }
                    case 12:
                        if (!crew.isDead() && !crew.isDoomed())
                            return CRIT_CREW_KILLED;
                }
            } else if (loc == LOC_REAR) {
                switch (roll) {
                    case 6:
                        if (getLoadedUnits().size() > 0)
                            return CRIT_CARGO;
                    case 7:
                        for (Mounted m : getWeaponList()) {
                            if (m.getLocation() == loc && !m.isDestroyed()
                                    && !m.isJammed() && !m.isHit()) {
                                return CRIT_WEAPON_JAM;
                            }
                        }
                    case 8:
                        if (!isStabiliserHit(loc)) {
                            for (Mounted m : getWeaponList()) {
                                if (m.getLocation() == loc) {
                                    return CRIT_STABILIZER;
                                }
                            }
                        }
                    case 9:
                        for (Mounted m : getWeaponList()) {
                            if (m.getLocation() == loc && !m.isDestroyed()
                                    && !m.isHit()) {
                                return CRIT_WEAPON_DESTROYED;
                            }
                        }
                    case 10:
                        if (getSensorHits() < 4)
                            return CRIT_SENSOR;
                    case 11:
                        if (!engineHit)
                            return CRIT_ENGINE;
                    case 12:
                        if (getEngine().isFusion() && !engineHit)
                            return CRIT_ENGINE;
                        else if (!getEngine().isFusion())
                            return CRIT_FUEL_TANK;
                }
            } else if (loc == LOC_ROTOR) {
                switch (roll) {
                    case 6:
                    case 7:
                    case 8:
                        if (!isImmobile())
                            return CRIT_ROTOR_DAMAGE;
                    case 9:
                    case 10:
                        if (!isStabiliserHit(loc))
                            return CRIT_FLIGHT_STABILIZER;
                    case 11:
                    case 12:
                        return CRIT_ROTOR_DESTROYED;
                }
            } else {
                switch (roll) {
                    case 6:
                        for (Mounted m : getWeaponList()) {
                            if (m.getLocation() == loc && !m.isDestroyed()
                                    && !m.isJammed() && !m.isHit()) {
                                return CRIT_WEAPON_JAM;
                            }
                        }
                    case 7:
                        if (getLoadedUnits().size() > 0)
                            return CRIT_CARGO;
                    case 8:
                        if (!isStabiliserHit(loc)) {
                            for (Mounted m : getWeaponList()) {
                                if (m.getLocation() == loc) {
                                    return CRIT_STABILIZER;
                                }
                            }
                        }
                    case 9:
                        // TODO: fix for new TW rules
                        // roll 1d6, 1-3, defending player
                        // chooses which weapon gets destroyed
                        // 4-6: attacker chooses which weapon gets destroyed
                        for (Mounted m : getWeaponList()) {
                            if (m.getLocation() == loc && !m.isDestroyed()
                                    && !m.isHit()) {
                                return CRIT_WEAPON_DESTROYED;
                            }
                        }
                    case 10:
                        if (!engineHit)
                            return CRIT_ENGINE;
                    case 11:
                        for (Mounted m : getAmmo()) {
                            if (!m.isDestroyed() && !m.isHit()) {
                                return CRIT_AMMO;
                            }
                        }
                    case 12:
                        if (getEngine().isFusion() && !engineHit)
                            return CRIT_ENGINE;
                        else if (!getEngine().isFusion())
                            return CRIT_FUEL_TANK;
                }
            }
        }
        return CRIT_NONE;
    }

    public PilotingRollData addEntityBonuses(PilotingRollData prd) {
        if (movementDamage > 0) {
            prd.addModifier(movementDamage, "Steering Damage");
        }
        if (isDriverHit())
            prd.addModifier(2, "pilot injured");
        if (isStabiliserHit(LOC_ROTOR))
            prd.addModifier(3, "flight stabiliser damaged");

        // VDNI bonus?
        if (getCrew().getOptions().booleanOption("vdni")
                && !getCrew().getOptions().booleanOption("bvdni")) {
            prd.addModifier(-1, "VDNI");
        }
     
        return prd;
    }
}
