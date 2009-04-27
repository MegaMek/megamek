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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

/**
 * You know what tanks are, silly.
 */
public class Tank extends Entity implements Serializable {
    /**
     *
     */
    private static final long serialVersionUID = -857210851169206264L;
    protected boolean m_bHasNoTurret = false;
    protected boolean m_bTurretLocked = false;
    protected boolean m_bTurretJammed = false;
    protected boolean m_bTurretEverJammed = false;
    private int m_nTurretOffset = 0;
    private int m_nStunnedTurns = 0;
    private boolean m_bImmobile = false;
    private boolean m_bImmobileHit = false;
    private int burningLocations = 0;
    protected int movementDamage = 0;
    private boolean minorMovementDamage = false;
    private boolean moderateMovementDamage = false;
    private boolean heavyMovementDamage = false;
    private boolean infernoFire = false;
    private ArrayList<Mounted> jammedWeapons = new ArrayList<Mounted>();
    protected boolean engineHit = false;

    // locations
    public static final int LOC_BODY = 0;
    public static final int LOC_FRONT = 1;
    public static final int LOC_RIGHT = 2;
    public static final int LOC_LEFT = 3;
    public static final int LOC_REAR = 4;
    public static final int LOC_TURRET = 5;

    // critical hits
    public static final int CRIT_NONE = -1;
    public static final int CRIT_DRIVER = 0;
    public static final int CRIT_WEAPON_JAM = 1;
    public static final int CRIT_WEAPON_DESTROYED = 2;
    public static final int CRIT_STABILIZER = 3;
    public static final int CRIT_SENSOR = 4;
    public static final int CRIT_COMMANDER = 5;
    public static final int CRIT_CREW_KILLED = 6;
    public static final int CRIT_CREW_STUNNED = 7;
    public static final int CRIT_CARGO = 8;
    public static final int CRIT_ENGINE = 9;
    public static final int CRIT_FUEL_TANK = 10;
    public static final int CRIT_AMMO = 11;
    public static final int CRIT_TURRET_JAM = 12;
    public static final int CRIT_TURRET_LOCK = 13;
    public static final int CRIT_TURRET_DESTROYED = 14;

    // tanks have no critical slot limitations
    private static final int[] NUM_OF_SLOTS = { 25, 25, 25, 25, 25, 25 };

    protected static String[] LOCATION_ABBRS = { "BD", "FR", "RS", "LS", "RR",
            "TU" };
    public static String[] LOCATION_NAMES = { "Body", "Front", "Right",
            "Left", "Rear", "Turret" };

    @Override
    public String[] getLocationAbbrs() {
        return LOCATION_ABBRS;
    }

    @Override
    public String[] getLocationNames() {
        return LOCATION_NAMES;
    }

    private int sensorHits = 0;
    private int stabiliserHits = 0;
    private boolean driverHit = false;
    private boolean commanderHit = false;

    // pain-shunted units can take two driver and commander hits and two hits
    // before being killed
    private boolean driverHitPS = false;
    private boolean commanderHitPS = false;
    private boolean crewHitPS = false;

    public boolean hasNoTurret() {
        return m_bHasNoTurret;
    }

    public void setHasNoTurret(boolean b) {
        m_bHasNoTurret = b;
    }

    /**
     * Returns this entity's walking/cruising mp, factored for heat, extreme
     * temperatures, and gravity.
     */
    @Override
    public int getWalkMP(boolean gravity, boolean ignoreheat) {
        int j = getOriginalWalkMP();
        j = Math.max(0, j - getCargoMpReduction());
        if(null != game) {
            int weatherMod = game.getPlanetaryConditions().getMovementMods(this);
            if(weatherMod != 0) {
                j = Math.max(j + weatherMod, 0);
            }
        }

        if ( hasModularArmor() ) {
            j--;
        }

        if (gravity) {
            j = applyGravityEffectsOnMP(j);
        }


        return j;


    }

    public boolean isTurretLocked() {
        return m_bTurretLocked || m_bTurretJammed;
    }

    public boolean isTurretJammed() {
        return m_bTurretJammed;
    }

    /**
     * Returns the number of locations in the entity
     */
    @Override
    public int locations() {
        return m_bHasNoTurret ? 5 : 6;
        // return 6;
    }

    @Override
    public boolean canChangeSecondaryFacing() {
        return !m_bHasNoTurret && !isTurretLocked();
    }

    @Override
    public boolean isValidSecondaryFacing(int n) {
        return !isTurretLocked();
    }

    @Override
    public int clipSecondaryFacing(int n) {
        return n;
    }

    @Override
    public void setSecondaryFacing(int sec_facing) {
        if (!isTurretLocked()) {
            super.setSecondaryFacing(sec_facing);
            if (!m_bHasNoTurret) {
                m_nTurretOffset = sec_facing - getFacing();
            }
        }
    }

    @Override
    public void setFacing(int facing) {
        super.setFacing(facing);
        if (isTurretLocked()) {
            int nTurretFacing = (facing + m_nTurretOffset + 6) % 6;
            super.setSecondaryFacing(nTurretFacing);
        }
    }

    public boolean isStabiliserHit(int loc) {
        return (stabiliserHits & (1 << loc)) == (1 << loc);
    }

    public void setStabiliserHit(int loc) {
        stabiliserHits |= (1 << loc);
    }

    public int getSensorHits() {
        return sensorHits;
    }

    public void setSensorHits(int hits) {
        sensorHits = hits;
    }

    public boolean isDriverHit() {
        return driverHit;
    }

    public void setDriverHit(boolean hit) {
        driverHit = hit;
    }

    public boolean isCommanderHit() {
        return commanderHit;
    }

    public void setCommanderHit(boolean hit) {
        commanderHit = hit;
    }

    public boolean isDriverHitPS() {
        return driverHitPS;
    }

    public void setDriverHitPS(boolean hit) {
        driverHitPS = hit;
    }

    public boolean isCommanderHitPS() {
        return commanderHitPS;
    }

    public void setCommanderHitPS(boolean hit) {
        commanderHitPS = hit;
    }

    public boolean isCrewHitPS() {
        return crewHitPS;
    }

    public void setCrewHitPS(boolean hit) {
        crewHitPS = hit;
    }

    public boolean isMovementHit() {
        return m_bImmobile;
    }

    public boolean isMovementHitPending() {
        return m_bImmobileHit;
    }

    public void immobilize() {
        m_bImmobileHit = true;
        setOriginalWalkMP(0);
    }

    @Override
    public boolean isImmobile() {
        if (game.getOptions().booleanOption("no_immobile_vehicles")) {
            return super.isImmobile();
        }
        return super.isImmobile() || m_bImmobile;
    }

    /**
     * Tanks have all sorts of prohibited terrain.
     */
    @Override
    public boolean isHexProhibited(IHex hex) {
        if (hex.containsTerrain(Terrains.IMPASSABLE)) {
            return true;
        }

        if(hex.containsTerrain(Terrains.SPACE) && doomedInSpace()) {
            return true;
        }

        switch (movementMode) {
            case IEntityMovementMode.TRACKED:
                return (hex.terrainLevel(Terrains.WOODS) > 1)
                        || ((hex.terrainLevel(Terrains.WATER) > 0) && !hex
                                .containsTerrain(Terrains.ICE))
                        || hex.containsTerrain(Terrains.JUNGLE)
                        || (hex.terrainLevel(Terrains.MAGMA) > 1)
                        || (hex.terrainLevel(Terrains.ROUGH) > 1);
            case IEntityMovementMode.WHEELED:
                return hex.containsTerrain(Terrains.WOODS)
                        || hex.containsTerrain(Terrains.ROUGH)
                        || ((hex.terrainLevel(Terrains.WATER) > 0) && !hex
                                .containsTerrain(Terrains.ICE))
                        || hex.containsTerrain(Terrains.RUBBLE)
                        || hex.containsTerrain(Terrains.MAGMA)
                        || hex.containsTerrain(Terrains.JUNGLE)
                        || (hex.terrainLevel(Terrains.SNOW) > 1)
                        || (hex.terrainLevel(Terrains.GEYSER) == 2);
            case IEntityMovementMode.HOVER:
                return hex.containsTerrain(Terrains.WOODS)
                        || hex.containsTerrain(Terrains.JUNGLE)
                        || (hex.terrainLevel(Terrains.MAGMA) > 1)
                        || (hex.terrainLevel(Terrains.ROUGH) > 1);
            case IEntityMovementMode.NAVAL:
            case IEntityMovementMode.HYDROFOIL:
                return (hex.terrainLevel(Terrains.WATER) <= 0)
                        || hex.containsTerrain(Terrains.ICE);
            case IEntityMovementMode.SUBMARINE:
                return (hex.terrainLevel(Terrains.WATER) <= 0);
            case IEntityMovementMode.WIGE:
                return (hex.containsTerrain(Terrains.WOODS)
                        || hex.containsTerrain(Terrains.BUILDING));
            default:
                return false;
        }
    }

    public void lockTurret() {
        m_bTurretLocked = true;
    }

    public void jamTurret() {
        m_bTurretEverJammed = true;
        m_bTurretJammed = true;
    }

    public void unjamTurret() {
        m_bTurretJammed = false;
    }

    public boolean isTurretEverJammed() {
        return m_bTurretEverJammed;
    }

    public int getStunnedTurns() {
        return m_nStunnedTurns;
    }

    public void setStunnedTurns(int turns) {
        m_nStunnedTurns = turns;
    }

    public void stunCrew() {
        if (m_nStunnedTurns == 0) {
            m_nStunnedTurns = 2;
        } else {
            m_nStunnedTurns++;
        }
    }

    @Override
    public void applyDamage() {
        m_bImmobile |= m_bImmobileHit;
    }

    @Override
    public void newRound(int roundNumber) {
        super.newRound(roundNumber);

        // check for crew stun
        if (m_nStunnedTurns > 0) {
            m_nStunnedTurns--;
        }

        // reset turret facing, if not jammed
        if (!m_bTurretLocked) {
            setSecondaryFacing(getFacing());
        }
    }

    /**
     * Returns the name of the type of movement used. This is tank-specific.
     */
    @Override
    public String getMovementString(int mtype) {
        switch (mtype) {
            case IEntityMovementType.MOVE_SKID:
                return "Skidded";
            case IEntityMovementType.MOVE_NONE:
                return "None";
            case IEntityMovementType.MOVE_WALK:
                return "Cruised";
            case IEntityMovementType.MOVE_RUN:
                return "Flanked";
            case IEntityMovementType.MOVE_JUMP:
                return "Jumped";
            default:
                return "Unknown!";
        }
    }

    /**
     * Returns the name of the type of movement used. This is tank-specific.
     */
    @Override
    public String getMovementAbbr(int mtype) {
        switch (mtype) {
            case IEntityMovementType.MOVE_SKID:
                return "S";
            case IEntityMovementType.MOVE_NONE:
                return "N";
            case IEntityMovementType.MOVE_WALK:
                return "C";
            case IEntityMovementType.MOVE_RUN:
                return "F";
            case IEntityMovementType.MOVE_JUMP:
                return "J";
            default:
                return "?";
        }
    }

    @Override
    public boolean hasRearArmor(int loc) {
        return false;
    }

    /**
     * Returns the Compute.ARC that the weapon fires into.
     */
    @Override
    public int getWeaponArc(int wn) {
        final Mounted mounted = getEquipment(wn);

        // B-Pods need to be special-cased, the have 360 firing arc
        if ((mounted.getType() instanceof WeaponType) &&
                mounted.getType().hasFlag(WeaponType.F_B_POD)) {
            return Compute.ARC_360;
        }
        switch (mounted.getLocation()) {
            case LOC_BODY:
            // Body mounted C3Ms fire into the front arc,
            // per
            // http://forums.classicbattletech.com/index.php/topic,9400.0.html
            case LOC_FRONT:
                if(game.getOptions().booleanOption("tacops_vehicle_arcs")) {
                    return Compute.ARC_NOSE;
                }
            case LOC_TURRET:
                if(game.getOptions().booleanOption("tacops_vehicle_arcs")) {
                    return Compute.ARC_TURRET;
                }
                return Compute.ARC_FORWARD;
            case LOC_RIGHT:
                if(game.getOptions().booleanOption("tacops_vehicle_arcs")) {
                    return Compute.ARC_RIGHT_BROADSIDE;
                }
                return Compute.ARC_RIGHTSIDE;
            case LOC_LEFT:
                if(game.getOptions().booleanOption("tacops_vehicle_arcs")) {
                    return Compute.ARC_LEFT_BROADSIDE;
                }
                return Compute.ARC_LEFTSIDE;
            case LOC_REAR:
                if(game.getOptions().booleanOption("tacops_vehicle_arcs")) {
                    return Compute.ARC_AFT;
                }
                return Compute.ARC_REAR;
            default:
                return Compute.ARC_360;
        }
    }

    /**
     * Returns true if this weapon fires into the secondary facing arc. If
     * false, assume it fires into the primary.
     */
    @Override
    public boolean isSecondaryArcWeapon(int weaponId) {
        if (getEquipment(weaponId).getLocation() == LOC_TURRET) {
            return true;
        }
        return false;
    }
    
    /**
     * Rolls up a hit location
     */
    @Override
    public HitData rollHitLocation(int table, int side, int aimedLocation,
            int aimingMode) {
        int nArmorLoc = LOC_FRONT;
        boolean bSide = false;
        boolean bRear = false;
        boolean ignoreTurret = m_bHasNoTurret || table == ToHitData.HIT_UNDERWATER;
        int motiveMod = 0;
        if ((side == ToHitData.SIDE_FRONT) && isHullDown() && !ignoreTurret) {
            // on a hull down vee, all front hits go to turret if one exists.
            nArmorLoc = LOC_TURRET;
        }
        if (side == ToHitData.SIDE_LEFT) {
            nArmorLoc = LOC_LEFT;
            bSide = true;
            motiveMod = 2;
        } else if (side == ToHitData.SIDE_RIGHT) {
            nArmorLoc = LOC_RIGHT;
            bSide = true;
            motiveMod = 2;
        } else if (side == ToHitData.SIDE_REAR) {
            nArmorLoc = LOC_REAR;
            motiveMod = 1;
            bRear = true;
        }
        if(game.getOptions().booleanOption("tacops_vehicle_effective")) {
            motiveMod = 0;
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
                rv.setEffect(HitData.EFFECT_VEHICLE_MOVE_DAMAGED);
                rv.setMotiveMod(motiveMod);
                break;
            case 4:
                rv.setEffect(HitData.EFFECT_VEHICLE_MOVE_DAMAGED);
                rv.setMotiveMod(motiveMod);
                break;
            case 5:
                if (bSide) {
                    rv = new HitData(LOC_FRONT, false,
                            HitData.EFFECT_VEHICLE_MOVE_DAMAGED);
                } else if (bRear) {
                    rv = new HitData(LOC_LEFT, false,
                            HitData.EFFECT_VEHICLE_MOVE_DAMAGED);
                } else {
                    rv = new HitData(LOC_LEFT, false,
                            HitData.EFFECT_VEHICLE_MOVE_DAMAGED);
                }
                rv.setMotiveMod(motiveMod);
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
                if(game.getOptions().booleanOption("tacops_vehicle_effective")) {
                    if (bSide) {
                        rv = new HitData(LOC_REAR);
                    } else if (bRear) {
                        rv = new HitData(LOC_RIGHT);
                    } else {
                        rv = new HitData(LOC_LEFT);
                    }
                } else {
                    if (bSide) {
                        rv = new HitData(LOC_REAR, false,
                                HitData.EFFECT_VEHICLE_MOVE_DAMAGED);
                    } else if (bRear) {
                        rv = new HitData(LOC_RIGHT, false,
                            HitData.EFFECT_VEHICLE_MOVE_DAMAGED);
                    } else {
                        rv = new HitData(LOC_LEFT, false,
                                HitData.EFFECT_VEHICLE_MOVE_DAMAGED);
                    }
                    rv.setMotiveMod(motiveMod);
                }
                break;
            case 10:
                if (!ignoreTurret) {
                    rv = new HitData(LOC_TURRET);
                }
                break;
            case 11:
                if (!ignoreTurret) {
                    rv = new HitData(LOC_TURRET);
                }
                break;
            case 12:
                if (ignoreTurret) {
                    rv.setEffect(HitData.EFFECT_CRITICAL);
                } else {
                    rv = new HitData(LOC_TURRET, false, HitData.EFFECT_CRITICAL);
                }
            }
        }
        if (table == ToHitData.HIT_SWARM) {
            rv.setEffect(rv.getEffect() | HitData.EFFECT_CRITICAL);
        }
        return rv;
    }

    @Override
    public HitData rollHitLocation(int table, int side) {
        return rollHitLocation(table, side, LOC_NONE,
                IAimingModes.AIM_MODE_NONE);
    }

    /**
     * Gets the location that excess damage transfers to
     */
    @Override
    public HitData getTransferLocation(HitData hit) {
        return new HitData(LOC_DESTROYED);
    }

    /**
     * Gets the location that is destroyed recursively
     */
    @Override
    public int getDependentLocation(int loc) {
        return LOC_NONE;
    }

    /**
     * Calculates the battle value of this mech
     */
    @Override
    public int calculateBattleValue() {
        return calculateBattleValue(false, false);
    }

    /**
     * Calculates the battle value of this tank
     */
    @Override
    public int calculateBattleValue(boolean ignoreC3, boolean ignorePilot) {
        double dbv = 0; // defensive battle value
        double obv = 0; // offensive bv

        int modularArmor = 0;
        for (Mounted mounted : getEquipment()) {
            if ((mounted.getType() instanceof MiscType) && mounted.getType().hasFlag(MiscType.F_MODULAR_ARMOR)) {
                modularArmor += mounted.getBaseDamageCapacity() - mounted.getDamageTaken();
            }
        }

        // total armor points
        dbv += (getTotalArmor()+modularArmor) * 2.5;

        // total internal structure
        dbv += getTotalInternal() * 1.5;

        // add defensive equipment
        double dEquipmentBV = 0;
        for (Mounted mounted : getEquipment()) {
            EquipmentType etype = mounted.getType();

            // don't count destroyed equipment
            if (mounted.isDestroyed()) {
                continue;
            }

            if (((etype instanceof WeaponType) && (etype.hasFlag(WeaponType.F_AMS) || etype.hasFlag(WeaponType.F_B_POD)))
                    || ((etype instanceof AmmoType) && (((AmmoType) etype)
                            .getAmmoType() == AmmoType.T_AMS))
                    || ((etype instanceof MiscType) && (etype
                            .hasFlag(MiscType.F_ECM)
                            || etype.hasFlag(MiscType.F_AP_POD)
                            // not yet coded: ||
                            // etype.hasFlag(MiscType.F_BRIDGE_LAYING)
                            || etype.hasFlag(MiscType.F_BAP)))) {
                dEquipmentBV += etype.getBV(this);
            }
        }
        dbv += dEquipmentBV;

        double typeModifier;
        switch (getMovementMode()) {
            case IEntityMovementMode.TRACKED:
                typeModifier = 0.9;
                break;
            case IEntityMovementMode.WHEELED:
                typeModifier = 0.8;
                break;
            case IEntityMovementMode.HOVER:
            case IEntityMovementMode.VTOL:
            case IEntityMovementMode.WIGE:
                typeModifier = 0.7;
                break;
            case IEntityMovementMode.NAVAL:
                typeModifier = 0.6;
                break;
            default:
                typeModifier = 0.6;
        }

        dbv *= typeModifier;

        // adjust for target movement modifier
        double tmmRan = Compute.getTargetMovementModifier(
                getRunMP(false, true), this instanceof VTOL,
                this instanceof VTOL).getValue();
        // for the future, when we implement jumping tanks
        int tmmJumped = Compute.getTargetMovementModifier(getJumpMP(), true,
                false).getValue();
        double tmmFactor = 1 + (Math.max(tmmRan, tmmJumped) / 10);
        dbv *= tmmFactor;

        double weaponBV = 0;

        // figure out base weapon bv
        double weaponsBVFront = 0;
        double weaponsBVRear = 0;
        boolean hasTargComp = hasTargComp();
        // and add up BVs for ammo-using weapon types for excessive ammo rule
        Map<String, Double> weaponsForExcessiveAmmo = new HashMap<String, Double>();
        for (Mounted mounted : getWeaponList()) {
            WeaponType wtype = (WeaponType) mounted.getType();
            double dBV = wtype.getBV(this);

            // don't count destroyed equipment
            if (mounted.isDestroyed()) {
                continue;
            }

            // don't count AMS, it's defensive
            if (wtype.hasFlag(WeaponType.F_AMS)) {
                continue;
            }
            if (wtype.hasFlag(WeaponType.F_B_POD)) {
                continue;
            }

            // artemis bumps up the value
            if (mounted.getLinkedBy() != null) {
                Mounted mLinker = mounted.getLinkedBy();
                if ((mLinker.getType() instanceof MiscType)
                        && mLinker.getType().hasFlag(MiscType.F_ARTEMIS)) {
                    dBV *= 1.2;
                }
            }

            if (mounted.getLinkedBy() != null) {
                Mounted mLinker = mounted.getLinkedBy();
                if ((mLinker.getType() instanceof MiscType) && mLinker.getType().hasFlag(MiscType.F_APOLLO)) {
                    dBV *= 1.15;
                }
            }

            // and we'll add the tcomp here too
            if (wtype.hasFlag(WeaponType.F_DIRECT_FIRE) && hasTargComp) {
                dBV *= 1.25;
            }
            if (mounted.getLocation() == LOC_REAR) {
                weaponsBVRear += dBV;
            } else if (mounted.getLocation() == LOC_FRONT) {
                weaponsBVFront += dBV;
            } else {
                weaponBV += dBV;
            }
            // add up BV of ammo-using weapons for each type of weapon,
            // to compare with ammo BV later for excessive ammo BV rule
            if (!((wtype.hasFlag(WeaponType.F_ENERGY) && !(wtype.getAmmoType() == AmmoType.T_PLASMA))
                    || wtype.hasFlag(WeaponType.F_ONESHOT)
                    || wtype.hasFlag(WeaponType.F_INFANTRY) || (wtype
                    .getAmmoType() == AmmoType.T_NA))) {
                String key = wtype.getAmmoType() + ":" + wtype.getRackSize();
                if (!weaponsForExcessiveAmmo.containsKey(key)) {
                    weaponsForExcessiveAmmo.put(key, wtype.getBV(this));
                } else {
                    weaponsForExcessiveAmmo.put(key, wtype.getBV(this)
                            + weaponsForExcessiveAmmo.get(key));
                }
            }
        }
        if (weaponsBVFront > weaponsBVRear) {
            weaponBV += weaponsBVFront;
            weaponBV += (weaponsBVRear * 0.5);
        } else {
            weaponBV += weaponsBVRear;
            weaponBV += (weaponsBVFront * 0.5);
        }

        // add ammo bv
        double ammoBV = 0;
        // extra BV for when we have semiguided LRMs and someone else has TAG on
        // our team
        double tagBV = 0;
        Map<String, Double> ammo = new HashMap<String, Double>();
        ArrayList<String> keys = new ArrayList<String>();
        for (Mounted mounted : getAmmo()) {
            AmmoType atype = (AmmoType) mounted.getType();

            // don't count depleted ammo
            if (mounted.getShotsLeft() == 0) {
                continue;
            }

            // don't count AMS, it's defensive
            if (atype.getAmmoType() == AmmoType.T_AMS) {
                continue;
            }

            // don't count oneshot ammo, it's considered part of the launcher.
            if (mounted.getLocation() == Entity.LOC_NONE) {
                // assumption: ammo without a location is for a oneshot weapon
                continue;
            }
            // semiguided or homing ammo might count double
            if ((atype.getMunitionType() == AmmoType.M_SEMIGUIDED)
                    || (atype.getMunitionType() == AmmoType.M_HOMING)) {
                Player tmpP = getOwner();
                // Okay, actually check for friendly TAG.
                if (tmpP != null) {
                    if (tmpP.hasTAG()) {
                        tagBV += atype.getBV(this);
                    } else if ((tmpP.getTeam() != Player.TEAM_NONE) && (game != null)) {
                        for (Enumeration<Team> e = game.getTeams(); e
                                .hasMoreElements();) {
                            Team m = e.nextElement();
                            if (m.getId() == tmpP.getTeam()) {
                                if (m.hasTAG(game)) {
                                    tagBV += atype.getBV(this);
                                }
                                // A player can't be on two teams.
                                // If we check his team and don't give the
                                // penalty, that's it.
                                break;
                            }
                        }
                    }
                }
            }
            String key = atype.getAmmoType() + ":" + atype.getRackSize();
            if (!keys.contains(key)) {
                keys.add(key);
            }
            if (!ammo.containsKey(key)) {
                ammo.put(key, atype.getBV(this));
            } else {
                ammo.put(key, atype.getBV(this) + ammo.get(key));
            }
        }
        // excessive ammo rule:
        // only count BV for ammo for a weapontype until the BV of all weapons
        // of that
        // type on the mech is reached
        for (String key : keys) {
            // They dont exist in either hash then dont bother adding nulls.
            if (!ammo.containsKey(key)
                    || !weaponsForExcessiveAmmo.containsKey(key)) {
                continue;
            }
            if (ammo.get(key) > weaponsForExcessiveAmmo.get(key)) {
                ammoBV += weaponsForExcessiveAmmo.get(key);
            } else {
                ammoBV += ammo.get(key);
            }
        }
        weaponBV += ammoBV;

        // add offensive misc. equipment BV (everything except AMS, A-Pod, ECM -
        // BMR p152)
        double oEquipmentBV = 0;
        for (Mounted mounted : getMisc()) {
            MiscType mtype = (MiscType) mounted.getType();

            // don't count destroyed equipment
            if (mounted.isDestroyed()) {
                continue;
            }

            if (mtype.hasFlag(MiscType.F_ECM)
                    || mtype.hasFlag(MiscType.F_AP_POD)
                    // not yet coded: || mtype.hasFlag(MiscType.F_BRIDGE_LAYING)
                    || mtype.hasFlag(MiscType.F_BAP)
                    || mtype.hasFlag(MiscType.F_TARGCOMP)) {
                // weapons
                continue;
            }
            oEquipmentBV += mtype.getBV(this);
        }

        weaponBV += oEquipmentBV;

        weaponBV += getWeight() / 2;

        // adjust further for speed factor
        double speedFactor = Math.pow(1 + (((double) getRunMP(false, true)
                + (Math.round((double) getJumpMP(false) / 2)) - 5) / 10), 1.2);
        speedFactor = Math.round(speedFactor * 100) / 100.0;

        obv = weaponBV * speedFactor;

        // we get extra bv from some stuff
        double xbv = 0.0;
        // extra BV for semi-guided lrm when TAG in our team
        xbv += tagBV;
        // extra from c3 networks. a valid network requires at least 2 members
        // some hackery and magic numbers here. could be better
        // also, each 'has' loops through all equipment. inefficient to do it 3
        // times
        if (((hasC3MM() && (calculateFreeC3MNodes() < 2))
                || (hasC3M() && (calculateFreeC3Nodes() < 3))
                || (hasC3S() && (c3Master > NONE)) || (hasC3i() && (calculateFreeC3Nodes() < 5)))
                && !ignoreC3 && (game != null)) {
            int totalForceBV = 0;
            totalForceBV += this.calculateBattleValue(true, true);
            for (Entity e : game.getC3NetworkMembers(this)) {
                if (!equals(e) && onSameC3NetworkAs(e)) {
                    totalForceBV += e.calculateBattleValue(true, true);
                }
            }
            xbv += totalForceBV *= 0.05;
        }

        int finalBV = (int) Math.round(dbv + obv + xbv);

         // and then factor in pilot
        double pilotFactor = 1;
        if (!ignorePilot) {
            pilotFactor = crew.getBVSkillMultiplier();
        }

        int retVal = (int) Math.round((finalBV) * pilotFactor);

        // don't factor pilot in if we are just calculating BV for C3 extra BV
        if (ignoreC3) {
            return finalBV;
        }
        return retVal;
    }

    @Override
    public PilotingRollData addEntityBonuses(PilotingRollData prd) {
        if (movementDamage > 0) {
            prd.addModifier(movementDamage, "Steering Damage");
        }
        if (commanderHit) {
            prd.addModifier(1, "commander injured");
        }
        if (driverHit) {
            prd.addModifier(2, "driver injured");
        }

        //are we wheeled and in light snow?
        IHex hex = game.getBoard().getHex(getPosition());
        if((null != hex) && (getMovementMode() == IEntityMovementMode.WHEELED) && (hex.terrainLevel(Terrains.SNOW) == 1)) {
            prd.addModifier(1, "thin snow");
        }

        // VDNI bonus?
        if (getCrew().getOptions().booleanOption("vdni")
                && !getCrew().getOptions().booleanOption("bvdni")) {
            prd.addModifier(-1, "VDNI");
        }

        if ( hasModularArmor() ) {
            prd.addModifier(1,"Modular Armor");
        }

        return prd;
    }

    @Override
    public Vector<Report> victoryReport() {
        Vector<Report> vDesc = new Vector<Report>();

        Report r = new Report(7025);
        r.type = Report.PUBLIC;
        r.addDesc(this);
        vDesc.addElement(r);

        r = new Report(7035);
        r.type = Report.PUBLIC;
        r.newlines = 0;
        vDesc.addElement(r);
        vDesc.addAll(crew.getDescVector(false));
        r = new Report(7070, Report.PUBLIC);
        r.add(getKillNumber());
        vDesc.addElement(r);

        if (isDestroyed()) {
            Entity killer = game.getEntity(killerId);
            if (killer == null) {
                killer = game.getOutOfGameEntity(killerId);
            }
            if (killer != null) {
                r = new Report(7072, Report.PUBLIC);
                r.addDesc(killer);
            } else {
                r = new Report(7073, Report.PUBLIC);
            }
            vDesc.addElement(r);
        }
        r.newlines = 2;

        return vDesc;
    }

    @Override
    public int[] getNoOfSlots() {
        return NUM_OF_SLOTS;
    }

    /**
     * Tanks don't have MASC
     */
    @Override
    public int getRunMPwithoutMASC(boolean gravity, boolean ignoreheat) {
        return getRunMP(gravity, ignoreheat);
    }

    @Override
    public int getHeatCapacity() {
        return 999;
    }

    @Override
    public int getHeatCapacityWithWater() {
        return getHeatCapacity();
    }

    @Override
    public int getEngineCritHeat() {
        return 0;
    }

    @Override
    public void autoSetInternal() {
        int nInternal = (int) Math.ceil(weight / 10.0);

        // No internals in the body location.
        initializeInternal(IArmorState.ARMOR_NA, LOC_BODY);

        for (int x = 1; x < locations(); x++) {
            initializeInternal(nInternal, x);
        }
    }

    @Override
    public int getMaxElevationChange() {
        return 1;
    }

    @Override
    public int getMaxElevationDown() {
        // WIGEs can go down as far as they want
        // 50 is a pretty arbitrary max amount, but that's also the
        // highest elevation for VTOLs, so I'll just use that
        if ((getElevation() > 0) && (getMovementMode() == IEntityMovementMode.WIGE)) {
            return 50;
        }
        return super.getMaxElevationDown();
    }

    /**
     * Determine if the unit can be repaired, or only harvested for spares.
     *
     * @return A <code>boolean</code> that is <code>true</code> if the unit
     *         can be repaired (given enough time and parts); if this value is
     *         <code>false</code>, the unit is only a source of spares.
     * @see Entity#isSalvage()
     */
    @Override
    public boolean isRepairable() {
        // A tank is repairable if it is salvageable,
        // and none of its body internals are gone.
        boolean retval = isSalvage();
        int loc = Tank.LOC_FRONT;
        while (retval && (loc < Tank.LOC_TURRET)) {
            int loc_is = this.getInternal(loc);
            loc++;
            retval = (loc_is != IArmorState.ARMOR_DOOMED)
                    && (loc_is != IArmorState.ARMOR_DESTROYED);
        }
        return retval;
    }

    /**
     * Restores the entity after serialization
     */
    @Override
    public void restore() {
        super.restore();
    }

    @Override
    public boolean canCharge() {
        // Tanks can charge, except Hovers when the option is set, and WIGEs
        return super.canCharge()
                && !(game.getOptions().booleanOption("no_hover_charge") && (IEntityMovementMode.HOVER == getMovementMode()))
                && !(IEntityMovementMode.WIGE == getMovementMode())
                && !(getStunnedTurns() > 0);
    }

    @Override
    public boolean canDFA() {
        // Tanks can't DFA
        return false;
    }

    @Override
    public int getArmorType() {
        return armorType;
    }

    @Override
    public void setArmorType(int type) {
        armorType = type;
    }

    @Override
    public int getStructureType() {
        return structureType;
    }

    @Override
    public void setStructureType(int type) {
        structureType = type;
    }

    /**
     * @return suspension factor of vehicle
     */
    public int getSuspensionFactor() {
        switch (movementMode) {
            case IEntityMovementMode.HOVER:
                if (weight <= 10) {
                    return 40;
                }
                if (weight <= 20) {
                    return 85;
                }
                if (weight <= 30) {
                    return 130;
                }
                if (weight <= 40) {
                    return 175;
                }
                return 235;
            case IEntityMovementMode.HYDROFOIL:
                if (weight <= 10) {
                    return 60;
                }
                if (weight <= 20) {
                    return 105;
                }
                if (weight <= 30) {
                    return 150;
                }
                if (weight <= 40) {
                    return 195;
                }
                if (weight <= 50) {
                    return 255;
                }
                if (weight <= 60) {
                    return 300;
                }
                if (weight <= 70) {
                    return 345;
                }
                if (weight <= 80) {
                    return 390;
                }
                if (weight <= 90) {
                    return 435;
                }
                return 480;
            case IEntityMovementMode.NAVAL:
            case IEntityMovementMode.SUBMARINE:
                return 30;
            case IEntityMovementMode.TRACKED:
                return 0;
            case IEntityMovementMode.WHEELED:
                return 20;
            case IEntityMovementMode.VTOL:
                if (weight <= 10) {
                    return 50;
                }
                if (weight <= 20) {
                    return 95;
                }
                return 140;
            case IEntityMovementMode.WIGE:
                if (weight <= 15) {
                    return 45;
                }
                if (weight <= 30) {
                    return 80;
                }
                if (weight <= 45) {
                    return 115;
                }
                if (weight <= 80) {
                    return 140;
                }
        }
        return 0;
    }

    @Override
    public double getCost() {
        double cost = 0;
        Engine engine = getEngine();
        cost += engine.getBaseCost() * engine.getRating() * weight / 75.0;
        double controlWeight = Math.ceil(weight * 0.05 * 2.0) / 2.0; // ?
                                                                        // should
                                                                        // be
                                                                        // rounded
                                                                        // up to
                                                                        // nearest
                                                                        // half-ton
        cost += 10000 * controlWeight;
        cost += weight / 10.0 * 10000; // IS has no variations, no Endo etc.
        double freeHeatSinks = engine.getWeightFreeEngineHeatSinks();
        int sinks = 0;
        double turretWeight = 0;
        double paWeight = 0;
        for (Mounted m : getWeaponList()) {
            WeaponType wt = (WeaponType) m.getType();
            if (wt.hasFlag(WeaponType.F_LASER) || wt.hasFlag(WeaponType.F_PPC)) {
                sinks += wt.getHeat();
                paWeight += wt.getTonnage(this) / 10.0;
            }
            if (!hasNoTurret() && (m.getLocation() == Tank.LOC_TURRET)) {
                turretWeight += wt.getTonnage(this) / 10.0;
            }
        }
        paWeight = Math.ceil(paWeight * 10.0) / 10;
        if (engine.isFusion()) {
            paWeight = 0;
        }
        turretWeight = Math.ceil(turretWeight * 2) / 2;
        cost += 20000 * paWeight;
        cost += 2000 * Math.max(0, sinks - freeHeatSinks);
        cost += turretWeight * 5000;
        cost += getArmorWeight() * EquipmentType.getArmorCost(armorType);// armor
        double diveTonnage;
        switch (movementMode) {
            case IEntityMovementMode.HOVER:
            case IEntityMovementMode.HYDROFOIL:
            case IEntityMovementMode.VTOL:
            case IEntityMovementMode.SUBMARINE:
                diveTonnage = weight / 10.0;
                break;
            default:
                diveTonnage = 0.0;
                break;
        }
        if (movementMode != IEntityMovementMode.VTOL) {
            cost += diveTonnage * 20000;
        } else {
            cost += diveTonnage * 40000;
        }
        cost += getWeaponsAndEquipmentCost();
        double multiplier = 1.0;
        switch (movementMode) {
            case IEntityMovementMode.HOVER:
            case IEntityMovementMode.SUBMARINE:
                multiplier += weight / 50.0;
                break;
            case IEntityMovementMode.HYDROFOIL:
                multiplier += weight / 75.0;
                break;
            case IEntityMovementMode.NAVAL:
            case IEntityMovementMode.WHEELED:
                multiplier += weight / 200.0;
                break;
            case IEntityMovementMode.TRACKED:
                multiplier += weight / 100.0;
                break;
            case IEntityMovementMode.VTOL:
                multiplier += weight / 30.0;
                break;
        }

        return Math.round(cost * multiplier);
    }

    @Override
    public boolean doomedInVacuum() {
        for (Mounted m : getEquipment()) {
            if ((m.getType() instanceof MiscType)
                    && m.getType().hasFlag(MiscType.F_VACUUM_PROTECTION)) {
                return false;
            }
            if ((m.getType() instanceof MiscType)
                    && m.getType().hasFlag(MiscType.F_ENVIRONMENTAL_SEALING)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean doomedOnGround() {
        return false;
    }

    @Override
    public boolean doomedInAtmosphere() {
        return true;
    }

    @Override
    public boolean doomedInSpace() {
        return true;
    }

    @Override
    public boolean canGoHullDown() {
        return game.getOptions().booleanOption("tacops_hull_down");
    }

    public void setOnFire(boolean inferno) {
        infernoFire |= inferno;
        burningLocations = (1 << locations()) - 1;
        extinguishLocation(LOC_BODY);
    }

    public boolean isOnFire() {
        return (burningLocations != 0) || infernos.isStillBurning();
    }

    public boolean isInfernoFire() {
        return infernoFire;
    }

    public boolean isLocationBurning(int location) {
        int flag = (1 << location);
        return (burningLocations & flag) == flag;
    }

    public void extinguishLocation(int location) {
        int flag = ~(1 << location);
        burningLocations &= flag;
    }

    /**
     * extinguish all inferno fire on this Tank
     */
    public void extinguishAll() {
        burningLocations = 0;
        infernoFire = false;
        infernos.clear();
    }

    /**
     * adds minor, moderate or heavy movement system damage
     * @param level a <code>int</code> representing minor damage (1),
     * moderate damage (2), or heavy damage (3)
     */
    public void addMovementDamage(int level) {
        switch (level) {
        case 1:
            if (!minorMovementDamage) {
                minorMovementDamage = true;
                movementDamage += level;
            }
            break;
        case 2:
            if (!moderateMovementDamage) {
                moderateMovementDamage = true;
                movementDamage += level;
            }
            break;
        case 3:
            if (!heavyMovementDamage) {
                heavyMovementDamage = true;
                movementDamage += level;
            }
        }
    }



    public void setEngine(Engine e) {
        engine = e;
        if (e.engineValid) {
            setOriginalWalkMP(calculateWalk());
        }
    }

    protected int calculateWalk() {
        return (getEngine().getRating() + getSuspensionFactor())
                / (int) weight;
    }

    @Override
    public boolean isNuclearHardened() {
        return true;
    }

    @Override
    protected void addEquipment(Mounted mounted, int loc, boolean rearMounted)
            throws LocationFullException {
        super.addEquipment(mounted, loc, rearMounted);
        // Add the piece equipment to our slots.
        addCritical(loc, new CriticalSlot(CriticalSlot.TYPE_EQUIPMENT,
                getEquipmentNum(mounted), true));
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
        if (roll > 12) {
            roll = 12;
        }
        if (roll < 6) {
            return CRIT_NONE;
        }
        for (int i = 0; i < 2; i++) {
            if (i > 0) {
                roll = 6;
            }
            if (loc == LOC_FRONT) {
                switch (roll) {
                    case 6:
                        if (!crew.isDead() && !crew.isDoomed()) {
                            if (!isDriverHit()) {
                                return CRIT_DRIVER;
                            }
                            else if (!isCommanderHit()){
                                return CRIT_CREW_STUNNED;
                            } else {
                                return CRIT_CREW_KILLED;
                            }
                        }
                    case 7:
                        for (Mounted m : getWeaponList()) {
                            if ((m.getLocation() == loc) && !m.isDestroyed()
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
                        if (getSensorHits() < 4) {
                            return CRIT_SENSOR;
                        }
                    case 10:
                        if (!crew.isDead() && !crew.isDoomed()) {
                            if (!isCommanderHit()) {
                                return CRIT_COMMANDER;
                            }
                            else if (!isDriverHit()) {
                                return CRIT_CREW_STUNNED;
                            }
                            else {
                                return CRIT_CREW_KILLED;
                            }
                        }
                    case 11:
                        for (Mounted m : getWeaponList()) {
                            if ((m.getLocation() == loc) && !m.isDestroyed()
                                    && !m.isHit()) {
                                return CRIT_WEAPON_DESTROYED;
                            }
                        }
                    case 12:
                        if (!crew.isDead() && !crew.isDoomed()) {
                            return CRIT_CREW_KILLED;
                        }
                }
            } else if (loc == LOC_REAR) {
                switch (roll) {
                    case 6:
                        for (Mounted m : getWeaponList()) {
                            if ((m.getLocation() == loc) && !m.isDestroyed()
                                    && !m.isJammed() && !m.isHit()) {
                                return CRIT_WEAPON_JAM;
                            }
                        }
                    case 7:
                        if (getLoadedUnits().size() > 0) {
                            return CRIT_CARGO;
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
                            if ((m.getLocation() == loc) && !m.isDestroyed()
                                    && !m.isHit()) {
                                return CRIT_WEAPON_DESTROYED;
                            }
                        }
                    case 10:
                        if (!engineHit) {
                            return CRIT_ENGINE;
                        }
                    case 11:
                        for (Mounted m : getAmmo()) {
                            if (!m.isDestroyed() && !m.isHit()) {
                                return CRIT_AMMO;
                            }
                        }
                    case 12:
                        if (getEngine().isFusion() && !engineHit) {
                            return CRIT_ENGINE;
                        } else if (!getEngine().isFusion()) {
                            return CRIT_FUEL_TANK;
                        }
                }
            } else if (loc == LOC_TURRET) {
                switch (roll) {
                    case 6:
                        if (!isStabiliserHit(loc)) {
                            for (Mounted m : getWeaponList()) {
                                if (m.getLocation() == loc) {
                                    return CRIT_STABILIZER;
                                }
                            }
                        }
                    case 7:
                        if (!isTurretLocked()) {
                            return CRIT_TURRET_JAM;
                        }
                    case 8:
                        for (Mounted m : getWeaponList()) {
                            if ((m.getLocation() == loc) && !m.isDestroyed()
                                    && !m.isJammed()) {
                                return CRIT_WEAPON_JAM;
                            }
                        }
                    case 9:
                        if (!isTurretLocked()) {
                            return CRIT_TURRET_LOCK;
                        }
                    case 10:
                        for (Mounted m : getWeaponList()) {
                            if ((m.getLocation() == loc) && !m.isDestroyed()
                                    && !m.isHit()) {
                                return CRIT_WEAPON_DESTROYED;
                            }
                        }
                    case 11:
                        for (Mounted m : getAmmo()) {
                            if (!m.isDestroyed() && !m.isHit()) {
                                return CRIT_AMMO;
                            }
                        }
                    case 12:
                        return CRIT_TURRET_DESTROYED;
                }
            } else {
                switch (roll) {
                    case 6:
                        if (getLoadedUnits().size() > 0) {
                            return CRIT_CARGO;
                        }
                    case 7:
                        for (Mounted m : getWeaponList()) {
                            if ((m.getLocation() == loc) && !m.isDestroyed()
                                    && !m.isJammed() && !m.isHit()) {
                                return CRIT_WEAPON_JAM;
                            }
                        }
                    case 8:
                        if (!crew.isDead() && !crew.isDoomed()) {
                            if (isCommanderHit() && isDriverHit()) {
                                return CRIT_CREW_KILLED;
                            }
                            return CRIT_CREW_STUNNED;
                        }
                    case 9:
                        if (!isStabiliserHit(loc)) {
                            for (Mounted m : getWeaponList()) {
                                if (m.getLocation() == loc) {
                                    return CRIT_STABILIZER;
                                }
                            }
                        }
                    case 10:
                        for (Mounted m : getWeaponList()) {
                            if ((m.getLocation() == loc) && !m.isDestroyed()
                                    && !m.isHit()) {
                                return CRIT_WEAPON_DESTROYED;
                            }
                        }
                    case 11:
                        if (!engineHit) {
                            return CRIT_ENGINE;
                        }
                    case 12:
                        if (getEngine().isFusion() && !engineHit) {
                            return CRIT_ENGINE;
                        } else if (!getEngine().isFusion()) {
                            return CRIT_FUEL_TANK;
                        }
                }
            }
        }
        return CRIT_NONE;
    }

    /**
     * OmniVehicles have handles for Battle Armor squads to latch onto. Please
     * note, this method should only be called during this Tank's construction.
     * <p/> Overrides <code>Entity#setOmni(boolean)</code>
     */
    @Override
    public void setOmni(boolean omni) {

        // Perform the superclass' action.
        super.setOmni(omni);

        // Add BattleArmorHandles to OmniMechs.
        if (omni && !hasBattleArmorHandles()) {
            addTransporter(new BattleArmorHandlesTank());
        }
    }

    /**
     * Tanks can't spot when stunned.
     */
    @Override
    public boolean canSpot() {
        return super.canSpot() && (getStunnedTurns() == 0);
    }

    public void addJammedWeapon(Mounted weapon) {
        jammedWeapons.add(weapon);
    }

    public ArrayList<Mounted> getJammedWeapons() {
        return jammedWeapons;
    }

    /**
     * apply the effects of an "engine hit" crit
     */
    public void engineHit() {
        engineHit = true;
        immobilize();
        lockTurret();
        for (Mounted m : getWeaponList()) {
            WeaponType wtype = (WeaponType) m.getType();
            if (wtype.hasFlag(WeaponType.F_ENERGY)) {
                m.setBreached(true); // not destroyed, just
                                        // unpowered
            }
        }
    }

    @Override
    public boolean hasModularArmor() {

        for (Mounted mount : this.getEquipment()) {
            if (!mount.isDestroyed()
                    && (mount.getType() instanceof MiscType)
                    && ((MiscType) mount.getType()).hasFlag(MiscType.F_MODULAR_ARMOR)) {
                return true;
            }
        }

        return false;

    }

    @Override
    public boolean hasModularArmor(int loc) {

        for (Mounted mount : this.getEquipment()) {
            if ((mount.getLocation() == loc)
                    && (mount.getType() instanceof MiscType)
                    && ((MiscType) mount.getType()).hasFlag(MiscType.F_MODULAR_ARMOR)) {
                return true;
            }
        }

        return false;
    }

    /*
     * (non-Javadoc)
     * @see megamek.common.Entity#getTotalCommGearTons()
     */
    @Override
    public int getTotalCommGearTons() {
        return 1 + getExtraCommGearTons();
    }

    /*
     * (non-Javadoc)
     * @see megamek.common.Entity#getIniBonus()
     */
    @Override
    public int getHQIniBonus() {
        int bonus = super.getHQIniBonus();
        if (((stabiliserHits > 0) && (mpUsedLastRound > 0))
                || commanderHit) {
            return 0;
        }
        return bonus;
    }

    @Override
    public boolean hasArmoredEngine() {
        for (int slot = 0; slot < getNumberOfCriticals(LOC_BODY); slot++) {
            CriticalSlot cs = getCritical(LOC_BODY, slot);
            if ((cs != null) && (cs.getType() == CriticalSlot.TYPE_SYSTEM) && (cs.getIndex() == Mech.SYSTEM_ENGINE)) {
                return cs.isArmored();
            }
        }
        return false;
    }
}
