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

import java.io.*;
import java.util.Enumeration;

/**
 * You know what tanks are, silly.
 */
public class Tank
    extends Entity
    implements Serializable
{
    private boolean m_bHasNoTurret = false;
    private boolean m_bTurretLocked = false;
    private int m_nTurretOffset = 0;
    private int m_nStunnedTurns = 0;
    private int m_nJammedTurns = 0;
    private Mounted m_jammedGun = null;
    private boolean m_bImmobile = false;
    private boolean m_bImmobileHit = false;
    
    // locations
    public static final int        LOC_BODY               = 0;
    public static final int        LOC_FRONT              = 1;
    public static final int        LOC_RIGHT              = 2;
    public static final int        LOC_LEFT               = 3;
    public static final int        LOC_REAR               = 4;
    public static final int        LOC_TURRET             = 5;
    
    // tanks have no critical slot limitations
    private static final int[] NUM_OF_SLOTS = {0, 0, 0, 0, 0, 0};
    
    private static final String[] LOCATION_ABBRS = { "BD", "FR", "RS", "LS", "RR", "TU" };
    private static final String[] LOCATION_NAMES = { "Body", "Front", "Right", "Left", "Rear", "Turret" };
    
    public String[] getLocationAbbrs() { return LOCATION_ABBRS; }
    public String[] getLocationNames() { return LOCATION_NAMES; }
    
    
    public boolean hasNoTurret() 
    { 
        return m_bHasNoTurret; 
    }
    
    public void setHasNoTurret(boolean b)
    {
        m_bHasNoTurret = b;
    }

    /**
     * Returns the number of locations in the entity
     */
    public int locations() {
        return m_bHasNoTurret ? 5 : 6;
        //return 6;
    }
    
    public boolean canChangeSecondaryFacing() {
        return !m_bHasNoTurret && !m_bTurretLocked;
    }
    
    public boolean isValidSecondaryFacing(int n) {
        return !m_bTurretLocked;
    }
    
    public int clipSecondaryFacing(int n) {
        return n;
    }

    public void setSecondaryFacing(int sec_facing) {
        if (!m_bTurretLocked) {
            super.setSecondaryFacing(sec_facing);
            if (!m_bHasNoTurret) {
                m_nTurretOffset = sec_facing - getFacing();
            }
        }
    }
    
    public void setFacing(int facing) {
        super.setFacing(facing);
        if (m_bTurretLocked) {
            int nTurretFacing = (facing + m_nTurretOffset + 6) % 6;
            super.setSecondaryFacing(nTurretFacing);
        }
    }
    
    public void immobilize()
    {
        m_bImmobileHit = true;
        setOriginalWalkMP(0);
    }
    
    public boolean isImmobile()
    {
        if (game.getOptions().booleanOption("no_immobile_vehicles")) {
            return super.isImmobile();
        } else {
            return super.isImmobile() || m_bImmobile;
        }
    }
    
    /**
     * Hovercraft move on the surface of the water
     */
    public int elevationOccupied(Hex hex) {
       if (movementType == MovementType.HOVER && hex.contains(Terrain.WATER)) {
           return hex.surface();
       } else {
           return hex.floor();
       }
    }
    
    /**
     * Tanks have all sorts of prohibited terrain.
     */
    public boolean isHexProhibited(Hex hex) {
        switch(movementType) {
            case MovementType.TRACKED :
                return hex.levelOf(Terrain.WOODS) > 1 || hex.levelOf(Terrain.WATER) > 0;
            case MovementType.WHEELED :
                return hex.contains(Terrain.WOODS) || hex.contains(Terrain.ROUGH) ||
                    hex.levelOf(Terrain.WATER) > 0 || hex.contains(Terrain.RUBBLE);
            case MovementType.HOVER :
                return hex.contains(Terrain.WOODS);
            default :
                return false;
        }
    }
    
    public void lockTurret()
    {
        m_bTurretLocked = true;
    }
    
    public void stunCrew()
    {
        m_nStunnedTurns = 3;
        this.crew.setUnconcious(true);
    }
    
    public void setJammedWeapon(Mounted m)
    {
        m_jammedGun = m;
        m_jammedGun.setJammed(true);
        m_nJammedTurns = 1;
    }
    
    public void applyDamage() {
        m_bImmobile |= m_bImmobileHit;
    }
    
    public void newRound()
    {
        super.newRound();
        
        // check for crew stun
        if (m_nStunnedTurns > 0) {
            m_nStunnedTurns--;
            if (m_nStunnedTurns == 0) {
                this.crew.setUnconcious(false);
            }
        }
        
        // check for weapon jam
        if (m_jammedGun != null) {
            if (m_nJammedTurns > 0) {
                m_nJammedTurns--;
            } else {
                m_jammedGun.setJammed(false);
                m_jammedGun = null;
            }
        }
        
        // reset turret facing, if not jammed
        if (!m_bTurretLocked) {
            setSecondaryFacing(getFacing());
        }
    }
    
    /*
     * This is only used for the 'main weapon' vehicle critical result.
     * No standard for 'mainness' is given (although it's also described
     * as the 'largest', so maybe it's tonnage).  I'm going with the highest 
     * BV non-disabled weapon (even if it's out of ammo)
     */
    public Mounted getMainWeapon()
    {
        double fBestBV = -1;
        Mounted mBest = null;
        for (Enumeration e = getWeapons(); e.hasMoreElements(); ) {
            Mounted m = (Mounted)e.nextElement();
            if (m.isDestroyed()) continue;
            
            double fValue = m.getType().getBV(this);
            if (fValue > fBestBV) {
                fBestBV = fValue;
                mBest = m;
            }
        }
        return mBest;
    }
        

    /**
     * Returns the name of the type of movement used.
     * This is tank-specific.
     */
    public String getMovementString(int mtype) {
        switch(mtype) {
        case MOVE_SKID :
            return "Skidded";
        case MOVE_NONE :
            return "None";
        case MOVE_WALK :
            return "Cruised";
        case MOVE_RUN :
            return "Flanked";
        case MOVE_JUMP :
            return "Jumped";
        default :
            return "Unknown!";
        }
    }
    
    /**
     * Returns the name of the type of movement used.
     * This is mech-specific.
     */
    public String getMovementAbbr(int mtype) {
        switch(mtype) {
        case MOVE_SKID :
            return "S";
        case MOVE_NONE :
            return "N";
        case MOVE_WALK :
            return "C";
        case MOVE_RUN :
            return "F";
        case MOVE_JUMP :
            return "J";
        default :
            return "?";
        }
    }
    
    public boolean hasRearArmor(int loc) {
        return false;
    }
    
      
    /**
     * Returns the Compute.ARC that the weapon fires into.
     */
    public int getWeaponArc(int wn) {
        final Mounted mounted = getEquipment(wn);
        if (mounted.getLocation() == LOC_FRONT) {
            return Compute.ARC_FORWARD;
        }
        else if (mounted.getLocation() == LOC_RIGHT) {
            return Compute.ARC_RIGHTSIDE;
        }
        else if (mounted.getLocation() == LOC_LEFT) {
            return Compute.ARC_LEFTSIDE;
        }
        else if (mounted.getLocation() == LOC_REAR) {
            return Compute.ARC_REAR;
        }
        else if (mounted.getLocation() == LOC_TURRET) {
            return Compute.ARC_FORWARD;
        }
        else {
            return Compute.ARC_360;
        }
    }

    /**
     * Returns true if this weapon fires into the secondary facing arc.  If
     * false, assume it fires into the primary.
     */
    public boolean isSecondaryArcWeapon(int weaponId) {
        if (getEquipment(weaponId).getLocation() == LOC_TURRET) {
            return true;
        }
        return false;
    }
    
    /**
     * Rolls up a hit location
     */
    public HitData rollHitLocation(int table, int side) {
        int nArmorLoc = LOC_FRONT;
        boolean bSide = false;
        if (side == ToHitData.SIDE_LEFT) {
            nArmorLoc = LOC_LEFT;
            bSide = true;
        }
        else if (side == ToHitData.SIDE_RIGHT) {
            nArmorLoc = LOC_RIGHT;
            bSide = true;
        }
        else if (side == ToHitData.SIDE_REAR) {
            nArmorLoc = LOC_REAR;
        }
        switch (Compute.d6(2)) {
            case 2:
                return new HitData(nArmorLoc, false, HitData.EFFECT_CRITICAL);
            case 3:
                return new HitData(nArmorLoc, false, HitData.EFFECT_VEHICLE_MOVE_DESTROYED);
            case 4:
                return new HitData(nArmorLoc, false, HitData.EFFECT_VEHICLE_MOVE_DAMAGED);
            case 5:
                if (bSide || getMovementType() == Entity.MovementType.HOVER) {
                    return new HitData(nArmorLoc, false, HitData.EFFECT_VEHICLE_MOVE_DAMAGED);
                }
                else {
                    return new HitData(nArmorLoc);
                }
            case 6:
            case 7:
            case 8:
                return new HitData(nArmorLoc);
            case 9:
                if (bSide && getMovementType() == Entity.MovementType.HOVER) {
                    return new HitData(nArmorLoc, false, HitData.EFFECT_VEHICLE_MOVE_DAMAGED);
                }
                else {
                    return new HitData(nArmorLoc);
                }
            case 10:
                if (m_bHasNoTurret) {
                    return new HitData(nArmorLoc);
                }
                else {
                    return new HitData(LOC_TURRET);
                }
            case 11:
                if (m_bHasNoTurret) {
                    return new HitData(nArmorLoc);
                }
                else {
                    return new HitData(LOC_TURRET, false, HitData.EFFECT_VEHICLE_TURRETLOCK);
                }
            case 12:
                if (m_bHasNoTurret || bSide) {
                    return new HitData(nArmorLoc, false, HitData.EFFECT_CRITICAL);
                }
                else {
                    return new HitData(LOC_TURRET, false, HitData.EFFECT_CRITICAL);
                }
        }
        return null;
    }
        
    /**
     * Gets the location that excess damage transfers to
     */
    public HitData getTransferLocation(HitData hit) {
        return new HitData(LOC_DESTROYED);
    }
    
    /**
     * Gets the location that is destroyed recursively
     */
    public int getDependentLocation(int loc) {
        return LOC_NONE;
    }

    /**
     * Calculates the battle value of this mech
     */
    public int calculateBattleValue() {
        return calculateBattleValue(false);
    }

    /**
     * Calculates the battle value of this tank
     */
    public int calculateBattleValue(boolean assumeLinkedC3) {
        double dbv = 0; // defensive battle value
        double obv = 0; // offensive bv
        
        // total armor points
        dbv += getTotalArmor();
        
        // total internal structure        
        dbv += getTotalInternal() / 2;
        
        // add defensive equipment
        double dEquipmentBV = 0;
        for (Enumeration i = equipmentList.elements(); i.hasMoreElements();) {
            Mounted mounted = (Mounted)i.nextElement();
            EquipmentType etype = mounted.getType();
            if ((etype instanceof WeaponType && ((WeaponType)etype).getAmmoType() == AmmoType.T_AMS)
            || (etype instanceof AmmoType && ((AmmoType)etype).getAmmoType() == AmmoType.T_AMS)
            || etype.hasFlag(MiscType.F_ECM)) {
                dEquipmentBV += etype.getBV(this);
            }
        }
        dbv += dEquipmentBV;
        
        double typeModifier;
        switch (getMovementType()) {
            case Entity.MovementType.TRACKED:
                typeModifier = 0.8;
                break;
            case Entity.MovementType.WHEELED:
                typeModifier = 0.7;
                break;
            case Entity.MovementType.HOVER:
                typeModifier = 0.6;
                break;
            // vtol and naval to come
            default:
                typeModifier = 0.5;
        }
        
        dbv *= typeModifier;
        
        // adjust for target movement modifier
        int tmmRan = Compute.getTargetMovementModifier(getRunMP(), false).getValue();
        if (tmmRan > 5) {
            tmmRan = 5;
        }
        double[] tmmFactors = { 1.0, 1.1, 1.2, 1.3, 1.4, 1.5 };
        dbv *= tmmFactors[tmmRan];
        
        double weaponBV = 0;
        
        // figure out base weapon bv
        double weaponsBVFront = 0;
        double weaponsBVRear = 0;
        boolean hasTargComp = hasTargComp();
        for (Enumeration i = weaponList.elements(); i.hasMoreElements();) {
            Mounted mounted = (Mounted)i.nextElement();
            WeaponType wtype = (WeaponType)mounted.getType();
            double dBV = wtype.getBV(this);

            // don't count AMS, it's defensive
            if (wtype.getAmmoType() == AmmoType.T_AMS) {
                continue;
            }
            
            // artemis bumps up the value
            if (mounted.getLinkedBy() != null) {
                Mounted mLinker = mounted.getLinkedBy();
                if (mLinker.getType() instanceof MiscType && 
                        mLinker.getType().hasFlag(MiscType.F_ARTEMIS)) {
                    dBV *= 1.2;
                }
            } 
            
            // and we'll add the tcomp here too
            if (wtype.hasFlag(WeaponType.F_DIRECT_FIRE) && hasTargComp) {
                dBV *= 1.2;
            }
            
            if (mounted.getLocation() == LOC_REAR) {
                weaponsBVRear += dBV;
            } else {
                weaponsBVFront += dBV;
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
        for (Enumeration i = ammoList.elements(); i.hasMoreElements();) {
            Mounted mounted = (Mounted)i.nextElement();
            AmmoType atype = (AmmoType)mounted.getType();
            
            // don't count AMS, it's defensive
            if (atype.getAmmoType() == AmmoType.T_AMS) {
                continue;
            }

            ammoBV += atype.getBV(this);
        }
        weaponBV += ammoBV;
        
        // adjust further for speed factor
        double speedFactor = getRunMP() - 5;
        speedFactor /= 10;
        speedFactor++;
        speedFactor = Math.pow(speedFactor, 1.2);
        speedFactor = Math.round(speedFactor * 100) / 100.0;
        
        obv = weaponBV * speedFactor;

        // we get extra bv from c3 networks. a valid network requires at least 2 members
        // some hackery and magic numbers here.  could be better
        // also, each 'has' loops through all equipment.  inefficient to do it 3 times
        double xbv = 0.0;
        if ((hasC3M() && calculateFreeC3Nodes() < 3) ||
            (hasC3S() && C3Master > NONE) ||
            (hasC3i() && calculateFreeC3Nodes() < 5) ||
            assumeLinkedC3) {
                xbv = (double)(Math.round(0.35 * weaponsBVFront + (0.5 * weaponsBVRear)));
        }
        
        // and then factor in pilot
        double pilotFactor = crew.getBVSkillMultiplier();

        return (int)Math.round((dbv + obv + xbv) * pilotFactor);
    }
    
    public PilotingRollData addEntityBonuses(PilotingRollData prd)
    {
        return prd;
    }
    
    /**
     * Returns an end-of-battle report for this mech
     */
    public String victoryReport() {
        StringBuffer report = new StringBuffer();
        
        report.append(getDisplayName());
        report.append('\n');
        report.append("Driver : " + crew.getDesc());
        report.append('\n');
        
        return report.toString();
    }
    
    public int[] getNoOfSlots()
    {
        return NUM_OF_SLOTS;
    }

    /**
     * Tanks don't have MASC
     */
    public int getRunMPwithoutMASC() {
        return getRunMP();
    }
    
    public int getHeatCapacity() {
        return 999;
    }
    
    
    public int getHeatCapacityWithWater() {
        return getHeatCapacity();
    }
    
    public int getEngineCritHeat() {
        return 0;
    }
    
    public void autoSetInternal()
    {
        int nInternal = (int)Math.ceil(weight / 10.0);

        // No internals in the body location.
        this.initializeInternal( Entity.ARMOR_NA, LOC_BODY );

        for (int x = 1; x < locations(); x++) {
            initializeInternal(nInternal, x);
        }
    }
    
    public int getMaxElevationChange()
    {
        return 1;
    }
}
