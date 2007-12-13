/*
 * MegaMek - Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
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
 * This class represents the lowest of the low, the ground pounders, 
 * the city rats, the PBI (Poor Bloody Infantry).  
 * <p/>
 * PLEASE NOTE!!!  This class just represents unarmored infantry platoons
 * as described by CitiTech (c) 1986.  I've never seen the rules for
 * powered armor, "anti-mech" troops, or Immortals.
 *
 * @author  Suvarov454@sourceforge.net (James A. Damour )
 * @version $revision:$
 */
/*
 *   PLEASE NOTE!!!  My programming style is to put constants first in
 *                   tests so the compiler catches my "= for ==" errors.
 */
public class Infantry
    extends Entity
    implements Serializable
{
    // Private attributes and helper functions.

    /**
     * 
     */
    private static final long serialVersionUID = -8706716079307721282L;

    /**
     * The number of men originally in this platoon.
     */
    private int         menStarting = 0;
 
    /**
     * The number of men alive in this platoon at the beginning of the phase,
     * before it begins to take damage.
     */
    private int         menShooting = 0;

    /**
     * The number of men left alive in this platoon.
     */
    private int         men     = 0;

    /**
     * The kind of weapons used by this platoon.
     */
    private int         weapons = 0;

    /**
     * The amount of damage the platoon can do if it hits.
     */
    private int[]       damage  = new int[INF_PLT_MAX_MEN + 1];

    /*
     * Infantry have no critical slot limitations or locations.
     */
    private static final int[]  NUM_OF_SLOTS    = {0};
    private static final String[] LOCATION_ABBRS= { "Men" };
    private static final String[] LOCATION_NAMES= { "Men" };

    /**
     * Identify this platoon as anti-mek trained.
     */
    private boolean     antiMek = false;
    
    protected int       runMP = 1;

    public int turnsLayingExplosives = -1;    
    
    public static final int DUG_IN_NONE = 0;
    public static final int DUG_IN_WORKING = 1; //no protection, can't attack
    public static final int DUG_IN_COMPLETE = 2; //protected, restricted arc
    public static final int DUG_IN_FORTIFYING1 = 3; //no protection, can't attack
    public static final int DUG_IN_FORTIFYING2 = 4; //no protection, can't attack
    private int dugIn = DUG_IN_NONE;
    
    private static final int DAMAGE_BALLISTIC_RIFLE[] =
    { 0,1,1,2,2,3,3,4,4,5,5,6,6,7,7,8,8,9,9,10,10,11,11,12,12,13,14,14,15,15,16 };
    private static final int DAMAGE_ENERGY_RIFLE[] =
    { 0,0,1,1,1,1,2,2,2,3,3,3,3,4,4,4,4,5,5,5,6,6,6,6,7,7,7,8,8,8,8 };
    private static final int DAMAGE_MACHINE_GUN[] =
    { 0,1,1,2,2,3,3,4,4,5,6,6,7,7,8,8,9,10,10,11,11,12,12,13,13,14,15,15,16,16,17 };
    private static final int DAMAGE_SRM[] =
    { 0,0,1,1,2,2,3,3,4,4,5,5,6,6,7,7,8,8,9,9,10,10,11,11,12,12,13,13,14,14,15 };
    // Inferno SRMs do half damage (rounded down).
    private static final int DAMAGE_INFERNO_SRM[] =
    { 0,0,0,0,1,1,1,1,2,2,2,2,3,3,3,3,4,4,4,4,5,5,5,5,6,6,6,6,7,7,7 };
    private static final int DAMAGE_LRM[] =
    { 0,0,1,1,2,2,3,3,3,4,4,5,5,6,6,6,7,7,8,8,9,9,9,10,10,11,11,11,12,12,13 };
    private static final int DAMAGE_FLAMER[] =
    { 0,0,1,1,2,2,3,3,4,4,5,5,6,6,7,7,8,8,9,9,10,10,11,11,12,12,12,13,13,14,14 };
    
    /**
     * Set up the damage array for this platoon for the given weapon type.
     *
     * @param   weapon - the type of weapon used by this platoon.
     * @exception IllegalArgumentException if a bad weapon
     *          type is passed.
     */
    private void setDamage( int weapon )
    {
        switch(weapon) {
            case INF_RIFLE:
                damage = DAMAGE_BALLISTIC_RIFLE;
                break;
            case INF_MG:
                damage = DAMAGE_MACHINE_GUN;
                break;
            case INF_FLAMER:
                damage = DAMAGE_FLAMER;
                break;
            case INF_LASER:
                damage = DAMAGE_ENERGY_RIFLE;
                break;
            case INF_SRM:
                damage = DAMAGE_SRM;
                break;
            case INF_INFERNO_SRM:
                damage = DAMAGE_INFERNO_SRM;
                break;
            case INF_LRM:        
                damage = DAMAGE_LRM;
                break;
            default:
                throw new IllegalArgumentException("Unknown infantry weapon");
        }
        assert(damage.length == INF_PLT_MAX_MEN+1);
    } // End private void setDamage( int ) throws Exception

    // Public and Protected constants, constructors, and methods.

    /**
     * The maximum number of men in an infantry platoon.
     */
    public static final int     INF_PLT_MAX_MEN = 30;

    /**
     * The maximum number of men in an infantry platoon.
     */
    public static final int     INF_PLT_FOOT_MAX_MEN = 21;

    /**
     * The maximum number of men in an infantry platoon.
     */
    public static final int     INF_PLT_JUMP_MAX_MEN = 21;

    /**
     * The maximum number of men in an infantry platoon.
     */
    public static final int     INF_PLT_CLAN_MAX_MEN = 25;   

    /*
     * The kinds of weapons available to the PBI.
     *
     * By incredible luck, the AmmoType and WeaponType constants
     * do not overlap for these six weapons.
     */
    public static final int     INF_UNKNOWN     = -1;// T_NA
    public static final int     INF_RIFLE       = AmmoType.T_AC;
    public static final int     INF_MG          = AmmoType.T_MG;
    public static final int     INF_FLAMER      = (int)WeaponType.F_FLAMER; 
    public static final int     INF_LASER       = (int)WeaponType.F_LASER;
    public static final int     INF_SRM         = AmmoType.T_SRM;
    public static final int     INF_LRM         = AmmoType.T_LRM;
    
    public static final int     INF_INFERNO_SRM = (int)WeaponType.F_INFERNO;

    /**
     * The location for infantry equipment.
     */
    public static final int     LOC_INFANTRY    = 0;

    /**
     * The internal names of the anti-Mek attacks.
     */
    public static final String  LEG_ATTACK      = "LegAttack";
    public static final String  SWARM_MEK       = "SwarmMek";
    public static final String  STOP_SWARM      = "StopSwarm";

    public String[] getLocationAbbrs() { return LOCATION_ABBRS; }
    public String[] getLocationNames() { return LOCATION_NAMES; }

    /**
     * Returns the number of locations in this platoon (i.e. one).
     */
    public int locations() { return 1; }

    /**
     * Generate a new, blank, infantry platoon.  
     * Hopefully, we'll be loaded from somewhere.
     */
    public Infantry() {
        // Instantiate the superclass.
        super();

        // Create a "dead" leg rifle platoon.
        this.menStarting = 0;
        this.menShooting = 0;
        this.men = 0;
        this.weapons = INF_RIFLE;
        setMovementMode(IEntityMovementMode.INF_LEG);

        // Populate the damage array.
        this.setDamage(this.weapons);

        // Determine the number of MPs.
        this.setOriginalWalkMP(1);

        // Clear the weapon type to be set later.
        this.weapons = INF_UNKNOWN;
    }

    /**
     * Infantry can face freely (except when dug in)
     */
    public boolean canChangeSecondaryFacing() { return (dugIn == DUG_IN_NONE); }

    /**
     * Infantry can face freely
     */
    public boolean isValidSecondaryFacing( int dir ) { return true; }

    /**
     * Infantry can face freely
     */
    public int clipSecondaryFacing( int dir ) { return dir; }
    
    /**
     * Return this Infantry's run MP.
     */
    public int getRunMP(boolean gravity) {
        if (gravity) return applyGravityEffectsOnMP(this.getOriginalRunMP());
		return this.getOriginalRunMP();
    }


    /**
     * Infantry don't have MASC
     */
    public int getRunMPwithoutMASC(boolean gravity) {
        return getRunMP(gravity);
    }

    /**
     * Get this infantry's orignal Run MP
     */
    protected int getOriginalRunMP() {
        return this.runMP;
    }

    /**
     * Infantry can not enter water unless they have UMU mp or hover.
     */
    public boolean isHexProhibited( IHex hex ) {
        if(hex.containsTerrain(Terrains.IMPASSABLE)) return true;
        if(hex.containsTerrain(Terrains.MAGMA))
            return true;
        if(hex.terrainLevel(Terrains.WATER) > 0 && !hex.containsTerrain(Terrains.ICE)) {
            if(getMovementMode() == IEntityMovementMode.HOVER
                    || getMovementMode() == IEntityMovementMode.INF_UMU)
                return false;
            return true;
        }
        return false;
    }

    /**
     * Returns the name of the type of movement used.
     * This is Infantry-specific.
     */
    public String getMovementString(int mtype) {
        switch(mtype) {
        case IEntityMovementType.MOVE_NONE :
            return "None";
        case IEntityMovementType.MOVE_WALK :
        case IEntityMovementType.MOVE_RUN :
            switch (this.getMovementMode()) {
            case IEntityMovementMode.INF_LEG:
                return "Walked";
            case IEntityMovementMode.INF_MOTORIZED:
                return "Biked";
            case IEntityMovementMode.HOVER:
            case IEntityMovementMode.TRACKED:
            case IEntityMovementMode.WHEELED:
                return "Drove";
            case IEntityMovementMode.INF_JUMP:
            default :
                return "Unknown!";
            }
        case IEntityMovementType.MOVE_VTOL_WALK:
        case IEntityMovementType.MOVE_VTOL_RUN:
            return "Flew";
        case IEntityMovementType.MOVE_JUMP :
            return "Jumped";
        default :
            return "Unknown!";
        }
    }

    /**
     * Returns the abbreviation of the type of movement used.
     * This is Infantry-specific.
     */
    public String getMovementAbbr(int mtype) {
        switch(mtype) {
        case IEntityMovementType.MOVE_NONE :
            return "N";
        case IEntityMovementType.MOVE_WALK :
            return "W";
        case IEntityMovementType.MOVE_RUN :
            switch (this.getMovementMode()) {
            case IEntityMovementMode.INF_LEG:
                return "R";
            case IEntityMovementMode.INF_MOTORIZED:
                return "B";
            case IEntityMovementMode.HOVER:
            case IEntityMovementMode.TRACKED:
            case IEntityMovementMode.WHEELED:
                return "D";
            default :
                return "?";
            }
        case IEntityMovementType.MOVE_JUMP :
            return "J";
        default :
            return "?";
        }
    }

    /**
     * Infantry only have one hit location.
     */
    public HitData rollHitLocation(int table, int side, int aimedLocation, int aimingMode) {
        return rollHitLocation(table, side);
    }     
     
    public HitData rollHitLocation( int table, int side ) {
        return new HitData( 0 );
    }

    /**
     * Infantry only have one hit location.
     */
    public HitData getTransferLocation(HitData hit) { 
        return new HitData(Entity.LOC_DESTROYED);
    }

    /**
     * Gets the location that is destroyed recursively.
     */
    public int getDependentLocation(int loc) {
        return Entity.LOC_NONE;
    }

    /**
     * Infantry have no rear armor.
     */
    public boolean hasRearArmor(int loc) {
        return false;
    }

    /**
     * Infantry platoons do wierd and wacky things with armor
     * and internals, but not all Infantry objects are platoons.
     *
     * @see     megamek.common.BattleArmor#isPlatoon()
     */
    protected boolean isPlatoon() { return true; }

    /**
     * Returns the number of men left in the platoon, or IArmorState.ARMOR_DESTROYED.
     */
    public int getInternal( int loc ) {
        if ( !this.isPlatoon() ) {
            return super.getInternal( loc );
        }
        return ( this.men > 0 ? this.men : IArmorState.ARMOR_DESTROYED );
    }

    /**
     * Returns the number of men originally the platoon.
     */
    public int getOInternal( int loc ) {
        if ( !this.isPlatoon() ) {
            return super.getOInternal( loc );
        }
        return this.menStarting;
    }

    /**
     * Sets the amount of men remaining in the platoon.
     */
    public void setInternal(int val, int loc) {
    super.setInternal( val, loc );
        this.men = val;
    }

    /**
     * Returns the percent of the men remaining in the platoon.
     */
    public double getInternalRemainingPercent() {
        if ( !this.isPlatoon() ) {
            return super.getInternalRemainingPercent();
        }
    int menTotal = this.men > 0 ? this.men : 0; // Handle "DESTROYED"
        return ((double) menTotal / this.menStarting);
    }

    /**
     * Initializes the number of men in the platoon. Sets the original and
     * starting  point of the platoon to the same number.
     */
      public void initializeInternal(int val, int loc) {
        this.menStarting = val;
        this.menShooting = val;
        super.initializeInternal( val, loc );
      }
      
    /**
     * Set the men in the platoon to the appropriate value for the 
     * platoon's movement type.
     */
    public void autoSetInternal() {

    // Clan platoons have 25 men.
    if ( this.isClan() ) {
        this.initializeInternal( INF_PLT_CLAN_MAX_MEN,
                     LOC_INFANTRY );
        return;
    }

    // IS platoon strength is based upon movement type.
        switch (this.getMovementMode()) {
        case IEntityMovementMode.INF_LEG:
        case IEntityMovementMode.INF_MOTORIZED:
            this.initializeInternal( INF_PLT_FOOT_MAX_MEN, LOC_INFANTRY );
            break;
        case IEntityMovementMode.INF_JUMP:
            this.initializeInternal( INF_PLT_JUMP_MAX_MEN, LOC_INFANTRY );
            break;
        default:
            throw new IllegalArgumentException
                ( "Unknown movement type: " + this.getMovementMode() );
        }
        
        if(hasWorkingMisc(MiscType.F_TOOLS, MiscType.S_HEAVY_ARMOR)) {
            initializeArmor( getOInternal(LOC_INFANTRY), LOC_INFANTRY );
        }
        return;
    }

    /**
     * Infantry weapons are dictated by their type.
     */
    protected void addEquipment( Mounted mounted,
                                 int loc,
                                 boolean rearMounted )
        throws LocationFullException 
    {
        EquipmentType equip = mounted.getType();

        // If the infantry can swarm, they're anti-mek infantry.
        if ( Infantry.SWARM_MEK.equals( equip.getInternalName() ) ) {
            this.antiMek = true;
        }
        // N.B. Clan Undine BattleArmor can leg attack, but aren't
        //          classified as "anti-mek" in the BMRr, pg. 155).
        else if ( Infantry.LEG_ATTACK.equals( equip.getInternalName() ) ||
                  Infantry.STOP_SWARM.equals( equip.getInternalName() ) ) {
            // Do nothing.
        }
        // Handle infantry weapons.
        else if ( (mounted.getType() instanceof WeaponType) &&
                  equip.hasFlag(WeaponType.F_INFANTRY) ) {

            // Infantry can only mount one kind of infantry weapon.
            WeaponType weapon = (WeaponType) mounted.getType();
            long weaponType;
            if ( this.weapons != INF_UNKNOWN ) {
                throw new LocationFullException
                    ( "Unit is already equiped with an infantry weapon" +
                      " and does not need a " + weapon.getName() );
            }

            // If the weapon uses ammo, then *that* is our weapon type,
            // otherwise it's a laser or flamer (get from equipment flags).
            if ( weapon.getAmmoType() != AmmoType.T_NA ) {
                weaponType = weapon.getAmmoType();
            }
            else {
                weaponType = weapon.getFlags() & 
                    (WeaponType.F_LASER + WeaponType.F_FLAMER + WeaponType.F_INFERNO);
            }
            this.weapons = (int)weaponType;
            
            // Update our damage profile.
            this.setDamage( weapons );

        }
/*        // Infantry platoons can't carry big equipment.
        else if ( this.isPlatoon() ) {
            throw new LocationFullException
                ( "Infantry platoons can not be equiped with a " +
                  mounted.getName() );
        }*/

        // Update our superclass.
        super.addEquipment( mounted, loc, rearMounted );
    }

    /**
     * Infantry can fire all around themselves.
     * But field guns are set up to a facing
     */
    public int getWeaponArc(int wn) { 
        if(this instanceof BattleArmor && dugIn == DUG_IN_NONE)
            return Compute.ARC_360; 
        Mounted mounted = getEquipment(wn);
        WeaponType wtype = (WeaponType)mounted.getType();
        if((wtype.hasFlag(WeaponType.F_INFANTRY)
             || wtype.hasFlag(WeaponType.F_EXTINGUISHER)
             || wtype.getInternalName() == LEG_ATTACK
             || wtype.getInternalName() == SWARM_MEK
             || wtype.getInternalName() == STOP_SWARM)
             && dugIn == DUG_IN_NONE)
            return Compute.ARC_360;
		return Compute.ARC_FORWARD;
    }

    /**
     * Infantry can fire all around themselves.
     * But field guns act like turret mounted on a tank
     */
    public boolean isSecondaryArcWeapon(int wn) { 
        if (this instanceof BattleArmor)
            return false; 
        Mounted mounted = getEquipment(wn);
        WeaponType wtype = (WeaponType)mounted.getType();
        if (wtype.hasFlag(WeaponType.F_INFANTRY))
            return false;
		return true;
    }

    /**
     * Infantry build no heat.
     */
    public int getHeatCapacity() {
        return 999;
    }

    /**
     * Infantry build no heat.
     */
    public int getHeatCapacityWithWater() {
        return getHeatCapacity();
    }

    /**
     * Infantry build no heat.
     */
    public int getEngineCritHeat() {
        return 0;
    }

    /**
     * Infantry have no critical slots.
     */
    protected int[] getNoOfSlots() { return NUM_OF_SLOTS; }
    
    /**
     * Infantry criticals can't be hit.
     */
    public boolean hasHittableCriticals(int loc) { return false; }

    /**
     * Calculates the battle value of this platoon.
     */
    public int calculateBattleValue() {

        double dBV = 0;
        
        int mm = this.getMovementMode();
        
        if(IEntityMovementMode.WHEELED == mm
                || IEntityMovementMode.TRACKED == mm
                || IEntityMovementMode.HOVER == mm
                || IEntityMovementMode.INF_UMU == mm) {
//          FIXME, when techmanual comes out
            mm = IEntityMovementMode.INF_MOTORIZED; 
        }

        // BV is factor of anti-Mek training, movement type and weapon type.
        if ( this.antiMek ) {

            if (this.weapons == INF_RIFLE) {
                if ( IEntityMovementMode.INF_LEG == mm )
                    dBV = 32;
                else if ( IEntityMovementMode.INF_MOTORIZED == mm )
                    dBV = 42;
                else if ( IEntityMovementMode.INF_JUMP == mm )
                    dBV = 46;
                else throw new IllegalArgumentException
                        ( "Unknown movement type: " + mm );
            } else if (this.weapons == INF_MG) {
                if ( IEntityMovementMode.INF_LEG == mm )
                    dBV = 47;
                else if ( IEntityMovementMode.INF_MOTORIZED == mm )
                    dBV = 63;
                else if ( IEntityMovementMode.INF_JUMP == mm )
                    dBV = 62;
                else throw new IllegalArgumentException
                        ( "Unknown movement type: " + mm );
            } else if (this.weapons == INF_FLAMER) {
                if ( IEntityMovementMode.INF_LEG == mm )
                    dBV = 41;
                else if ( IEntityMovementMode.INF_MOTORIZED == mm )
                    dBV = 54;
                else if ( IEntityMovementMode.INF_JUMP == mm )
                    dBV = 51;
                else throw new IllegalArgumentException
                        ( "Unknown movement type: " + mm );
            } else if (this.weapons == INF_LASER) {
                if ( IEntityMovementMode.INF_LEG == mm )
                    dBV = 60;
                else if ( IEntityMovementMode.INF_MOTORIZED == mm )
                    dBV = 70;
                else if ( IEntityMovementMode.INF_JUMP == mm )
                    dBV = 71;
                else throw new IllegalArgumentException
                        ( "Unknown movement type: " + mm );
            } else if (this.weapons == INF_SRM || weapons == INF_INFERNO_SRM) {
                if ( IEntityMovementMode.INF_LEG == mm )
                    dBV = 60;
                else if ( IEntityMovementMode.INF_MOTORIZED == mm )
                    dBV = 70;
                else if ( IEntityMovementMode.INF_JUMP == mm )
                    dBV = 71;
                else throw new IllegalArgumentException
                        ( "Unknown movement type: " + mm );
            } else if (this.weapons == INF_LRM) {
                if ( IEntityMovementMode.INF_LEG == mm )
                    dBV = 56;
                else if ( IEntityMovementMode.INF_MOTORIZED == mm )
                    dBV = 75;
                else if ( IEntityMovementMode.INF_JUMP == mm )
                    dBV = 87;
                else throw new IllegalArgumentException
                        ( "Unknown movement type: " + mm );
            } else throw new IllegalArgumentException
                    ( "Unknown infantry weapon: " + this.weapons );
        } // End anti-Mek-trained
        else {
            if (this.weapons == INF_RIFLE) {
                if ( IEntityMovementMode.INF_LEG == mm )
                    dBV = 23;
                else if ( IEntityMovementMode.INF_MOTORIZED == mm )
                    dBV = 28;
                else if ( IEntityMovementMode.INF_JUMP == mm )
                    dBV = 29;
                else throw new IllegalArgumentException
                        ( "Unknown movement type: " + mm );
            } else if (this.weapons == INF_MG) {
                if ( IEntityMovementMode.INF_LEG == mm )
                    dBV = 31;
                else if ( IEntityMovementMode.INF_MOTORIZED == mm )
                    dBV = 39;
                else if ( IEntityMovementMode.INF_JUMP == mm )
                    dBV = 37;
                else throw new IllegalArgumentException
                        ( "Unknown movement type: " + mm );
            } else if (this.weapons == INF_FLAMER) {
                if ( IEntityMovementMode.INF_LEG == mm )
                    dBV = 28;
                else if ( IEntityMovementMode.INF_MOTORIZED == mm )
                    dBV = 35;
                else if ( IEntityMovementMode.INF_JUMP == mm )
                    dBV = 32;
                else throw new IllegalArgumentException
                        ( "Unknown movement type: " + mm );
            } else if (this.weapons == INF_LASER) {  
                if ( IEntityMovementMode.INF_LEG == mm )
                    dBV = 37;
                else if ( IEntityMovementMode.INF_MOTORIZED == mm )
                    dBV = 42;
                else if ( IEntityMovementMode.INF_JUMP == mm )
                    dBV = 41;
                else throw new IllegalArgumentException
                        ( "Unknown movement type: " + mm );
            } else if (this.weapons == INF_SRM || weapons == INF_INFERNO_SRM) {
                if ( IEntityMovementMode.INF_LEG == mm )
                    dBV = 60;
                else if ( IEntityMovementMode.INF_MOTORIZED == mm )
                    dBV = 70;
                else if ( IEntityMovementMode.INF_JUMP == mm )
                    dBV = 71;
                else throw new IllegalArgumentException
                        ( "Unknown movement type: " + mm );
            } else if (this.weapons == INF_LRM) {
                if ( IEntityMovementMode.INF_LEG == mm )
                    dBV = 56;
                else if ( IEntityMovementMode.INF_MOTORIZED == mm )
                    dBV = 75;
                else if ( IEntityMovementMode.INF_JUMP == mm )
                    dBV = 87;
                else throw new IllegalArgumentException
                        ( "Unknown movement type: " + mm );
            } else throw new IllegalArgumentException
                    ( "Unknown infantry weapon: " + this.weapons );

        } // End not-anti-Mek

        // add BV of field guns
        for(Mounted mounted : getWeaponList()) {
            WeaponType wtype = (WeaponType)mounted.getType();
            if(!wtype.hasFlag(WeaponType.F_INFANTRY))
                dBV += wtype.getBV(this);
        }

        // Adjust for missing troopers
        dBV = dBV * getInternalRemainingPercent();

        // adjust for crew
        double pilotFactor = crew.getBVSkillMultiplier();

        int finalBV = (int)Math.round(dBV);

        int retVal = (int)Math.round((finalBV) * pilotFactor);
        return retVal;
    } // End public int calculateBattleValue()

    public Vector<Report> victoryReport() {
        Vector<Report> vDesc = new Vector<Report>();

        Report r = new Report(7025);
        r.type = Report.PUBLIC;
        r.addDesc(this);
        vDesc.addElement(r);

        r = new Report(7040);
        r.type = Report.PUBLIC;
        r.newlines = 0;
        vDesc.addElement(r);
        vDesc.addAll(crew.getDescVector(true));
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

    /**
     * Infantry don't need piloting rolls.
     */
    public PilotingRollData addEntityBonuses(PilotingRollData prd)
    {
        return prd;
    }

    /**
     * Infantry can only change 1 elevation level at a time.
     */
    public int getMaxElevationChange()
    {
        return 1;
    }

    /**
     * Update the platoon to reflect damages taken in this phase.
     */
    public void applyDamage() { 
        super.applyDamage();
        menShooting = men;
    }

    // The methods below aren't in the Entity interface.

    /**
     * Determine the amount of damage that the infantry can produce,
     * given a number of men left in the platoon.
     *
     * @param menLeft - the number of men in the platoon capable of shooting.
     * @return <code>int</code> - the amount of damage done when the platoon
     *      hits its target.
     */
    public int getDamage( int menLeft ) { return damage[menLeft]; }

    /**
     * Get the number of men in the platoon (before damage is applied).
     */
    public int getShootingStrength() { return menShooting; }

    public boolean canCharge() {
        // Infantry can't Charge
        return false;
    }

    public boolean canDFA() {
        // Infantry can't DFA
        return false;
    }
    
    /** 
     * Checks if the entity is moving into a swamp. If so, returns
     *  the target roll for the piloting skill check.
     *  now includes the level 3 terains which can bog down
     */
    public PilotingRollData checkSwampMove(MoveStep step, IHex curHex,
            Coords lastPos, Coords curPos, boolean isPavementStep) {
        PilotingRollData roll = new PilotingRollData(this.getId(), 5, "entering boggy terrain");
        //DO NOT add terrain modifier, or the example in maxtech would have the wrong target number

        if (!lastPos.equals(curPos)
            && step.getMovementType() != IEntityMovementType.MOVE_JUMP
            && (this.getMovementMode() != IEntityMovementMode.HOVER) 
            && (this.getMovementMode() != IEntityMovementMode.VTOL)
            && step.getElevation() == 0
            && !isPavementStep) {
            // non-hovers need a simple PSR
            if (curHex.containsTerrain(Terrains.SWAMP)) {
                // append the reason modifier
                roll.append(new PilotingRollData(getId(), 0, "entering Swamp"));
            } else if (curHex.containsTerrain(Terrains.MAGMA) ||
                    curHex.containsTerrain(Terrains.MUD) ||
                    curHex.containsTerrain(Terrains.SNOW) ||
                    curHex.containsTerrain(Terrains.TUNDRA)) {
                roll.append(new PilotingRollData(getId(), -1, "avoid bogging down"));
            } else {
                roll.addModifier(TargetRoll.CHECK_FALSE,"Check false: no swamp-like terrain present");                
            }
        } else {
            roll.addModifier(TargetRoll.CHECK_FALSE,"Check false: Not entering swamp, or jumping/hovering over the swamp");
        }
        return roll;
    }    

    /**
     * Sets this entity's original walking movement points
     */
    public void setOriginalRunMP(int runMP) {
        this.runMP = runMP;
    }


    /**
     * @return The cost in C-Bills of the 'Mech in question.
     */
    public double getCost() {
        double cost = 0;
        double multiplier = 0;
        
        int mm = this.getMovementMode();
        
        if(IEntityMovementMode.WHEELED == mm
                || IEntityMovementMode.TRACKED == mm
                || IEntityMovementMode.HOVER == mm) {
//          FIXME, when techmanual comes out
            mm = IEntityMovementMode.INF_MOTORIZED; 
        }

        if ( this.antiMek ) {
            multiplier = 5;
        } else {
            multiplier = 1;
        }
        
        if(IEntityMovementMode.INF_UMU == mm) {
            mm = IEntityMovementMode.INF_LEG;
            multiplier *= 2;
        }
        if (this.weapons == INF_RIFLE) {
            if ( IEntityMovementMode.INF_LEG == mm )
                cost = 600000;
            else if ( IEntityMovementMode.INF_MOTORIZED == mm )
                cost = 960000;
            else if ( IEntityMovementMode.INF_JUMP == mm )
                cost = 1200000;
            else throw new IllegalArgumentException
                    ( "Unknown movement type: " + mm );
        } else if (this.weapons == INF_MG) {
            if ( IEntityMovementMode.INF_LEG == mm )
                cost = 800000;
            else if ( IEntityMovementMode.INF_MOTORIZED == mm )
                cost = 1280000;
            else if ( IEntityMovementMode.INF_JUMP == mm )
                cost = 1600000;
            else throw new IllegalArgumentException
                    ( "Unknown movement type: " + mm );
        } else if (this.weapons == INF_FLAMER) {
            if ( IEntityMovementMode.INF_LEG == mm )
                cost = 800000;
            else if ( IEntityMovementMode.INF_MOTORIZED == mm )
                cost = 1280000;
            else if ( IEntityMovementMode.INF_JUMP == mm )
                cost = 1600000;
            else throw new IllegalArgumentException
                    ( "Unknown movement type: " + mm );
        } else if (this.weapons == INF_LASER) {
            if ( IEntityMovementMode.INF_LEG == mm )
                cost = 1200000;
            else if ( IEntityMovementMode.INF_MOTORIZED == mm )
                cost = 1920000;
            else if ( IEntityMovementMode.INF_JUMP == mm )
                cost = 2400000;
            else throw new IllegalArgumentException
                    ( "Unknown movement type: " + mm );
        } else if (this.weapons == INF_SRM || weapons == INF_INFERNO_SRM) {
            if ( IEntityMovementMode.INF_LEG == mm )
                cost = 1400000;
            else if ( IEntityMovementMode.INF_MOTORIZED == mm )
                cost = 2240000;
            else if ( IEntityMovementMode.INF_JUMP == mm )
                cost = 2800000;
            else
                throw new IllegalArgumentException
                    ( "Unknown movement type: " + mm );
        } else if (this.weapons == INF_LRM) {
            if ( IEntityMovementMode.INF_LEG == mm )
                cost = 1400000;
            else if ( IEntityMovementMode.INF_MOTORIZED == mm )
                cost = 2240000;
            else if ( IEntityMovementMode.INF_JUMP == mm )
                cost = 2800000;
            else
                throw new IllegalArgumentException
                    ( "Unknown movement type: " + mm );
        } else throw new IllegalArgumentException
                    ( "Unknown infantry weapon: " + this.weapons );
        // End not-anti-Mek

        return cost*multiplier;
    }

    public boolean doomedInVacuum() {
        // We're assuming that infantry have environmental suits of some sort.
        // Vac suits, battle armor, whatever.
        // This isn't necessarily a true assumption.
        // FIXME
        return false;
    }

    public boolean canAssaultDrop() {
        return game.getOptions().booleanOption("paratroopers");
    }

    public boolean isEligibleFor(int phase) {
        if(turnsLayingExplosives > 0 && phase != IGame.PHASE_PHYSICAL)
            return false;
        if(dugIn != DUG_IN_COMPLETE
        		&& dugIn != DUG_IN_NONE) {
        	return false;
        }
        return super.isEligibleFor(phase);
    }

    public void newRound(int roundNumber) {
        if(turnsLayingExplosives >= 0) {
            turnsLayingExplosives++;
            if(!(Compute.isInBuilding(game, this)))
                turnsLayingExplosives = -1; //give up if no longer in a building
        }
        if(dugIn != DUG_IN_COMPLETE
        		&& dugIn != DUG_IN_NONE) {
        	dugIn++;
        	if(dugIn > DUG_IN_FORTIFYING2) {
        		dugIn = DUG_IN_NONE;
        	}
        }
        super.newRound(roundNumber);
    }
        
    public boolean loadWeapon(Mounted mounted, Mounted mountedAmmo) {
        if(!(this instanceof BattleArmor)) {
            //field guns don't share ammo, and infantry weapons dont have ammo
            if(mounted.getLinked() != null || mountedAmmo.getLinkedBy() != null)
                return false;
        }
        return super.loadWeapon(mounted, mountedAmmo);
    }

    public boolean loadWeaponWithSameAmmo(Mounted mounted, Mounted mountedAmmo) {
        if(!(this instanceof BattleArmor)) {
            //field guns don't share ammo, and infantry weapons dont have ammo
            if(mounted.getLinked() != null || mountedAmmo.getLinkedBy() != null)
                return false;
        }
        return super.loadWeaponWithSameAmmo(mounted, mountedAmmo);
    }

    public void setDugIn(int i) {
    	dugIn = i;
    }
    
    public int getDugIn() {
    	return dugIn;
    }

    public boolean isNuclearHardened() {
        return false;
    }
    
    /**
     * This function is called when loading a unit into a transport.
     * 
     * This is overridden to ensure infantry are no longer considered dug in when they are being transported.
     * @param transportID
     */
    public void setTransportID(int transportID) {
        super.setTransportId(transportID);
        
        setDugIn(DUG_IN_NONE);
    }
} // End class Infantry
