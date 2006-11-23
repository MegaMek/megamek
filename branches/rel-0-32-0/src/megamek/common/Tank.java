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
import java.util.Vector;

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
    private int burningLocations = 0;
    private int movementDamage = 0;
    private boolean infernoFire = false;
    
    // locations
    public static final int        LOC_BODY               = 0;
    public static final int        LOC_FRONT              = 1;
    public static final int        LOC_RIGHT              = 2;
    public static final int        LOC_LEFT               = 3;
    public static final int        LOC_REAR               = 4;
    public static final int        LOC_TURRET             = 5;

    // tanks have no critical slot limitations
    private static final int[] NUM_OF_SLOTS = {25, 25, 25, 25, 25, 25};
    
    protected static String[] LOCATION_ABBRS = { "BD", "FR", "RS", "LS", "RR", "TU" };
    protected static String[] LOCATION_NAMES = { "Body", "Front", "Right", "Left", "Rear", "Turret" };
    
    public String[] getLocationAbbrs() { return LOCATION_ABBRS; }
    public String[] getLocationNames() { return LOCATION_NAMES; }
    
    private int armorType = 0;
    private int structureType = 0;

    public boolean hasNoTurret() 
    { 
        return m_bHasNoTurret; 
    }
    
    public void setHasNoTurret(boolean b)
    {
        m_bHasNoTurret = b;
    }
    
    /**
    * Returns this entity's walking/cruising mp, factored
    * for heat, extreme temperatures, and gravity.
    */
    public int getWalkMP(boolean gravity) {
        int i;
        int j;
        if (gravity) j = applyGravityEffectsOnMP(getOriginalWalkMP());
        else j = getOriginalWalkMP();
        if (game != null) {
            i = game.getTemperatureDifference();
            return Math.max(j - i, 0);
        }
        return j;
    }    

    public boolean isTurretLocked() {
        return m_bTurretLocked;
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

    public boolean isMovementHit() {
        return m_bImmobile;
    }

    public boolean isMovementHitPending() {
        return m_bImmobileHit;
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
        }
		return super.isImmobile() || m_bImmobile;
    }

    
    /**
     * Tanks have all sorts of prohibited terrain.
     */
    public boolean isHexProhibited(IHex hex) {
        if(hex.containsTerrain(Terrains.IMPASSABLE)) return true;
        switch(movementMode) {
            case IEntityMovementMode.TRACKED :
                return hex.terrainLevel(Terrains.WOODS) > 1 || 
                (hex.terrainLevel(Terrains.WATER) > 0 && !hex.containsTerrain(Terrains.ICE)) ||
                hex.containsTerrain(Terrains.JUNGLE) || hex.terrainLevel(Terrains.MAGMA) > 1;
            case IEntityMovementMode.WHEELED :
                return hex.containsTerrain(Terrains.WOODS) || hex.containsTerrain(Terrains.ROUGH) ||
                (hex.terrainLevel(Terrains.WATER) > 0 && !hex.containsTerrain(Terrains.ICE)) || 
                hex.containsTerrain(Terrains.RUBBLE) || hex.containsTerrain(Terrains.MAGMA) ||
                hex.containsTerrain(Terrains.JUNGLE) || hex.containsTerrain(Terrains.SNOW) ||
                hex.terrainLevel(Terrains.GEYSER) == 2;
            case IEntityMovementMode.HOVER :
                return hex.containsTerrain(Terrains.WOODS) || hex.containsTerrain(Terrains.JUNGLE) ||
                hex.terrainLevel(Terrains.MAGMA) > 1;
            case IEntityMovementMode.NAVAL:
            case IEntityMovementMode.HYDROFOIL:
                return (hex.terrainLevel(Terrains.WATER) <= 0) || hex.containsTerrain(Terrains.ICE);
            case IEntityMovementMode.SUBMARINE:
                return (hex.terrainLevel(Terrains.WATER) <= 0);
            default :
                return false;
        }
    }
    
    public void lockTurret() {
        m_bTurretLocked = true;
    }

    public int getStunnedTurns() {
        return m_nStunnedTurns;
    }

    public void setStunnedTurns( int turns ) {
        m_nStunnedTurns = turns;
        this.crew.setUnconscious(true);
    }

    public void stunCrew() {
        setStunnedTurns( 3 );
    }

    public int getJammedTurns() {
        return m_nJammedTurns;
    }

    public void setJammedTurns( int turns ) {
        // Set the jammed gun, if none are currently jammed.
        if ( null == m_jammedGun ) {
            m_jammedGun = this.getMainWeapon();
            // We *may* be in the middle of de-serializing this tank.
            if ( null != m_jammedGun ) {
                m_jammedGun.setJammed(true);
            }
        }
        m_nJammedTurns = turns;
    }

    public void applyDamage() {
        m_bImmobile |= m_bImmobileHit;
    }
    
    public void newRound(int roundNumber) {
        super.newRound(roundNumber);
        
        // check for crew stun
        if (m_nStunnedTurns > 0) {
            m_nStunnedTurns--;
            if (m_nStunnedTurns == 0) {
                this.crew.setUnconscious(false);
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
    
    /**
     * This is only used for the 'main weapon' vehicle critical result.
     * No standard for 'mainness' is given (although it's also described
     * as the 'largest', so maybe it's tonnage).  I'm going with the highest 
     * BV non-disabled weapon (even if it's out of ammo)
     */
    public Mounted getMainWeapon() {
        double fBestBV = -1;
        Mounted mBest = null;
        for (Mounted m : getWeaponList()) {
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
        case IEntityMovementType.MOVE_SKID :
            return "Skidded";
        case IEntityMovementType.MOVE_NONE :
            return "None";
        case IEntityMovementType.MOVE_WALK :
            return "Cruised";
        case IEntityMovementType.MOVE_RUN :
            return "Flanked";
        case IEntityMovementType.MOVE_JUMP :
            return "Jumped";
        default :
            return "Unknown!";
        }
    }
    
    /**
     * Returns the name of the type of movement used.
     * This is tank-specific.
     */
    public String getMovementAbbr(int mtype) {
        switch(mtype) {
        case IEntityMovementType.MOVE_SKID :
            return "S";
        case IEntityMovementType.MOVE_NONE :
            return "N";
        case IEntityMovementType.MOVE_WALK :
            return "C";
        case IEntityMovementType.MOVE_RUN :
            return "F";
        case IEntityMovementType.MOVE_JUMP :
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
        switch (mounted.getLocation()) {
            case LOC_FRONT:
            case LOC_TURRET:
            case LOC_BODY:
                // Body mounted C3Ms fire into the front arc,
                // per http://forums.classicbattletech.com/index.php/topic,9400.0.html
                return Compute.ARC_FORWARD;
            case LOC_RIGHT:
                return Compute.ARC_RIGHTSIDE;
            case LOC_LEFT:
                return Compute.ARC_LEFTSIDE;
            case LOC_REAR:
                return Compute.ARC_REAR;
            default:
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
    public HitData rollHitLocation(int table, int side, int aimedLocation, int aimingMode) {
        return rollHitLocation(table, side);
    }     
    
    public HitData rollHitLocation(int table, int side) {
        int nArmorLoc = LOC_FRONT;
        boolean bSide = false;
        if (side == ToHitData.SIDE_FRONT && isHullDown() && !m_bHasNoTurret) {
        	//on a hull down vee, all front hits go to turret if one exists.
        	nArmorLoc = LOC_TURRET;
        }
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
                if (bSide || getMovementMode() == IEntityMovementMode.HOVER || getMovementMode() == IEntityMovementMode.HYDROFOIL) {
                    return new HitData(nArmorLoc, false, HitData.EFFECT_VEHICLE_MOVE_DAMAGED);
                }
                return new HitData(nArmorLoc);
            case 6:
            case 7:
            case 8:
                return new HitData(nArmorLoc);
            case 9:
                if (bSide && ((getMovementMode() == IEntityMovementMode.HOVER) || (getMovementMode() == IEntityMovementMode.HYDROFOIL))) {
                    return new HitData(nArmorLoc, false, HitData.EFFECT_VEHICLE_MOVE_DAMAGED);
                }
                return new HitData(nArmorLoc);
            case 10:
                if (m_bHasNoTurret) {
                    return new HitData(nArmorLoc);
                }
                return new HitData(LOC_TURRET);
            case 11:
                if (m_bHasNoTurret) {
                    return new HitData(nArmorLoc);
                }
                return new HitData(LOC_TURRET, false, HitData.EFFECT_VEHICLE_TURRETLOCK);
            case 12:
                if (m_bHasNoTurret || bSide) {
                    return new HitData(nArmorLoc, false, HitData.EFFECT_CRITICAL);
                }
                return new HitData(LOC_TURRET, false, HitData.EFFECT_CRITICAL);
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
        dbv += getTotalInternal() / 2.0;

        // add defensive equipment
        double dEquipmentBV = 0;
        for (Mounted mounted : getEquipment()) {
            EquipmentType etype = mounted.getType();

            // don't count destroyed equipment
            if (mounted.isDestroyed())
                continue;

            if ((etype instanceof WeaponType && etype.hasFlag(WeaponType.F_AMS))
            || (etype instanceof AmmoType && ((AmmoType)etype).getAmmoType() == AmmoType.T_AMS)
            || etype.hasFlag(MiscType.F_ECM)) {
                dEquipmentBV += etype.getBV(this);
            }
        }
        dbv += dEquipmentBV;

        double typeModifier;
        switch (getMovementMode()) {
            case IEntityMovementMode.TRACKED:
                typeModifier = 0.8;
                break;
            case IEntityMovementMode.WHEELED:
                typeModifier = 0.7;
                break;
            case IEntityMovementMode.HOVER:
                typeModifier = 0.6;
                break;
            case IEntityMovementMode.VTOL:
                typeModifier = 0.4;
                break;
            case IEntityMovementMode.NAVAL:
                typeModifier = 0.5;
                break;
            default:
                typeModifier = 0.5;
        }
        
        dbv *= typeModifier;
        
        // adjust for target movement modifier
        int tmmRan = Compute.getTargetMovementModifier(getOriginalRunMP(), false, false, false).getValue();
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
        for (Mounted mounted : getWeaponList()) {
            WeaponType wtype = (WeaponType)mounted.getType();
            double dBV = wtype.getBV(this);

            // don't count destroyed equipment
            if (mounted.isDestroyed())
                continue;

            // don't count AMS, it's defensive
            if (wtype.hasFlag(WeaponType.F_AMS)) {
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
        for (Mounted mounted : getAmmo()) {
            AmmoType atype = (AmmoType)mounted.getType();
            
            // don't count depleted ammo
            if (mounted.getShotsLeft() == 0)
                continue;

            // don't count AMS, it's defensive
            if (atype.getAmmoType() == AmmoType.T_AMS) {
                continue;
            }

            ammoBV += atype.getBV(this);
        }
        weaponBV += ammoBV;

        // adjust further for speed factor
        double[] speedFactorTable = {0.44,0.54,0.65,0.77,0.88,1,1.12,1.24,1.37,
                                     1.5,1.63,1.76,1.89,2.02,2.16,2.3,2.44,2.58,
                                     2.72,2.86,3,3.15,3.29,3.44,3.59,3.74};
        double speedFactor = 3.74;
        if(getOriginalRunMP() < speedFactorTable.length)
            speedFactor = speedFactorTable[getOriginalRunMP()];
        /* Vehicles don't use the same speed factor calc as 'Mechs!
        double speedFactor = getOriginalRunMP() - 5;
        speedFactor /= 10;
        speedFactor++;
        speedFactor = Math.pow(speedFactor, 1.2);
        speedFactor = Math.round(speedFactor * 100) / 100.0;
        */

        obv = weaponBV * speedFactor;

        // we get extra bv from c3 networks. a valid network requires at least 2 members
        // some hackery and magic numbers here.  could be better
        // also, each 'has' loops through all equipment.  inefficient to do it 3 times
        double xbv = 0.0;
        if ((hasC3MM() && calculateFreeC3MNodes() < 2) ||
            (hasC3M() && calculateFreeC3Nodes() < 3) ||
            (hasC3S() && C3Master > NONE) ||
            (hasC3i() && calculateFreeC3Nodes() < 5) ||
            assumeLinkedC3) {
                xbv = Math.round(0.35 * weaponsBVFront + (0.5 * weaponsBVRear));
        }

        // Possibly adjust for TAG and Arrow IV.
        if (getsTagBVPenalty()) {
            dbv += 200;
        }
        if (getsHomingBVPenalty()) {
            dbv += 200;
        }

        // and then factor in pilot
        double pilotFactor = crew.getBVSkillMultiplier();

        //return (int)Math.round((dbv + obv + xbv) * pilotFactor);
        int finalBV = (int)Math.round(dbv + obv + xbv);

        int retVal = (int)Math.round(finalBV * pilotFactor);
        return retVal;
    }
    
    public PilotingRollData addEntityBonuses(PilotingRollData prd)
    {
        if(movementDamage > 0) {
            prd.addModifier(movementDamage, "Steering Damage");
        }
        return prd;
    }

    public Vector victoryReport() {
        Vector vDesc = new Vector();

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
        
        if(isDestroyed()) {
            Entity killer = game.getEntity(killerId);
            if(killer == null) {
                killer = game.getOutOfGameEntity(killerId);
            }
            if(killer != null) {
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
    
    public int[] getNoOfSlots()
    {
        return NUM_OF_SLOTS;
    }

    /**
     * Tanks don't have MASC
     */
    public int getRunMPwithoutMASC(boolean gravity) {
        return getRunMP(gravity);
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
        this.initializeInternal( IArmorState.ARMOR_NA, LOC_BODY );

        for (int x = 1; x < locations(); x++) {
            initializeInternal(nInternal, x);
        }
    }
    
    public int getMaxElevationChange()
    {
        return 1;
    }

    /**
     * Determine if the unit can be repaired, or only harvested for spares.
     *
     * @return  A <code>boolean</code> that is <code>true</code> if the unit
     *          can be repaired (given enough time and parts); if this value
     *          is <code>false</code>, the unit is only a source of spares.
     * @see     Entity#isSalvage()
     */
    public boolean isRepairable() {
        // A tank is repairable if it is salvageable,
        // and none of its body internals are gone.
        boolean retval = this.isSalvage();
        int loc = Tank.LOC_FRONT;
        while ( retval && loc < Tank.LOC_TURRET ) {
            int loc_is = this.getInternal( loc );
            loc++;
            retval = (loc_is != IArmorState.ARMOR_DOOMED) && (loc_is != IArmorState.ARMOR_DESTROYED);
        }
        return retval;
    }

    /**
     * Restores the entity after serialization
     */
    public void restore() {
        super.restore();

        // Restore our jammed gun, if necessary.
        if ( m_nJammedTurns > 0 && null == m_jammedGun ) {
            m_jammedGun = this.getMainWeapon();
        }
    }

    public boolean canCharge() {
        // Tanks can charge, except Hovers when the option is set
        return super.canCharge() && !(game.getOptions().booleanOption("no_hover_charge") && IEntityMovementMode.HOVER==getMovementMode());
    }

    public boolean canDFA() {
        // Tanks can't DFA
        return false;
    }

    public int getArmorType()
    {
        return armorType;
    }

    public void setArmorType(int type)
    {
        armorType = type;
    }

    public int getStructureType()
    {
        return structureType;
    }

    public void setStructureType(int type)
    {
        structureType = type;
    }

    /**
     * @return suspension factor of vehicle
     */
    public int getSuspensionFactor () {
        switch (movementMode) {
            case IEntityMovementMode.HOVER:
                if (weight<=10) return 40;
                if (weight<=20) return 85;
                if (weight<=30) return 130;
                if (weight<=40) return 175;
                return 235;
            case IEntityMovementMode.HYDROFOIL:
                if (weight<=10) return 60;
                if (weight<=20) return 105;
                if (weight<=30) return 150;
                if (weight<=40) return 195;
                if (weight<=50) return 255;
                if (weight<=60) return 300;
                if (weight<=70) return 345;
                if (weight<=80) return 390;
                if (weight<=90) return 435;
                return 480;
            case IEntityMovementMode.NAVAL:
            case IEntityMovementMode.SUBMARINE:
                return 30;
            case IEntityMovementMode.TRACKED:
                return 0;
            case IEntityMovementMode.WHEELED:
                return 20;
            case IEntityMovementMode.VTOL:
                if (weight<=10) return 50;
                if (weight<=20) return 95;
                return 140;
        }
        return 0;
    }
    
    public double getCost() {
        double cost = 0;
        Engine engine = getEngine();
        cost += engine.getBaseCost() * engine.getRating() * weight / 75.0;
        double controlWeight = Math.ceil(weight*0.05*2.0)/2.0; //? should be rounded up to nearest half-ton
        cost += 10000*controlWeight;
        cost += weight/10.0*10000; // IS has no variations, no Endo etc.
        double freeHeatSinks = engine.getCountEngineHeatSinks();
        int sinks=0;
        double turretWeight=0;
        double paWeight=0;
        for (Mounted m : getWeaponList()) {
            WeaponType wt = (WeaponType) m.getType();
            if(wt.hasFlag(WeaponType.F_LASER) || wt.hasFlag(WeaponType.F_PPC)) {
                sinks+=wt.getHeat();
                paWeight+=wt.getTonnage(this)/10.0;
            }
            if(!hasNoTurret() && m.getLocation()==Tank.LOC_TURRET) {
                turretWeight+=wt.getTonnage(this)/10.0;
            }
        }
        paWeight=Math.ceil(paWeight*10.0)/10;
        if (engine.isFusion()) {
            paWeight=0;
        }
        turretWeight=Math.ceil(turretWeight*2)/2;
        cost+=20000*paWeight;
        cost+=2000*Math.max(0,sinks-freeHeatSinks);
        cost+=turretWeight*5000;
        cost+=getArmorWeight()*EquipmentType.getArmorCost(armorType);//armor
        double diveTonnage;
        switch (movementMode) {
            case IEntityMovementMode.HOVER:
            case IEntityMovementMode.HYDROFOIL:
            case IEntityMovementMode.VTOL:
            case IEntityMovementMode.SUBMARINE:
                diveTonnage = weight/10.0;
                break;
            default:
                diveTonnage = 0.0;
                break;
        }
        if (movementMode!=IEntityMovementMode.VTOL) {
            cost += diveTonnage*20000;
        } else {
            cost += diveTonnage*40000;
        }
        cost += getWeaponsAndEquipmentCost();
        double multiplier = 1.0;
        switch (movementMode) {
            case IEntityMovementMode.HOVER:
            case IEntityMovementMode.SUBMARINE:
                multiplier += weight/50.0;
                break;
            case IEntityMovementMode.HYDROFOIL:
                multiplier += weight/75.0;
                break;
            case IEntityMovementMode.NAVAL:
            case IEntityMovementMode.WHEELED:
                multiplier += weight/200.0;
                break;
            case IEntityMovementMode.TRACKED:
                multiplier += weight/100.0;
                break;
            case IEntityMovementMode.VTOL:
                multiplier += weight/30.0;
                break;
        }
 
        return Math.round(cost*multiplier);
    }

    public boolean doomedInVacuum() {
        for (Mounted m : getEquipment()) {
            if (m.getType() instanceof MiscType && m.getType().hasFlag(MiscType.F_VACUUM_PROTECTION)) {
                return false;
            }
        }
        return true;
    }

    public boolean canGoHullDown () {
    	return game.getOptions().booleanOption("hull_down");
    }
    
    public void setOnFire(boolean inferno) {
        infernoFire |= inferno;
        burningLocations = (1<<locations()) - 1;
        extinguishLocation(LOC_BODY);
    }
    
    public boolean isOnFire() {
        return (burningLocations != 0) || infernos.isStillBurning();
    }
    
    public boolean isInfernoFire() {
        return infernoFire;
    }
    
    public boolean isLocationBurning(int location) {
        int flag = (1<<location);
        return (burningLocations & flag) == flag;
    }
    
    public void extinguishLocation(int location) {
        int flag = ~(1<<location);
        burningLocations &= flag;
    }
    
    public void extinguishAll() {
        burningLocations = 0;
        infernoFire = false;
        infernos.clear();
    }
    
    public void addMovementDamage(int level) {
        movementDamage += level;
    }

    public void setEngine(Engine e) {
        engine = e;
        if (e.engineValid) {
            setOriginalWalkMP(calculateWalk());
        }
    }

    protected int calculateWalk() {
        return (getEngine().getRating() + getSuspensionFactor()) / (int)this.weight;
    }

    public boolean isNuclearHardened() {
        return true;
    }
    
    protected void addEquipment(Mounted mounted, int loc, boolean rearMounted)
    throws LocationFullException {
        super.addEquipment(mounted,loc, rearMounted);
        // Add the piece equipment to our slots.
        addCritical(loc, new CriticalSlot(CriticalSlot.TYPE_EQUIPMENT,
                                           getEquipmentNum(mounted),
                                           true));
    }
}
