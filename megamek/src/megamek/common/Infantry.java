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

import java.io.*;

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

    /**
     * Set up the damage array for this platoon for the given weapon type.
     *
     * @param   weapon - the type of weapon used by this platoon.
     * @exception <code>IllegalArgumentException</code> if a bad weapon
     *          type is passed.
     */
    private void setDamage( int weapon )
    {
        int man;
        int points; // MGs and Flamers have wierd damage profiles.
        double men_per_point;

        // The platoon does no damage when its out of men.
        this.damage[0] = 0;

        // The various weapons require different amounts of
        // men to cause additional points of damage.
        switch ( weapon ) {
        case INF_RIFLE:
            men_per_point = 4.0;
            break;
        case INF_MG:
        case INF_FLAMER:
            men_per_point = 3.0;
            break;
        case INF_LASER:
        case INF_SRM:
            men_per_point = 2.0;
            break;
        case INF_LRM:
            men_per_point = 4.0;
            break;
        default:
            throw new IllegalArgumentException
                ( "Unknown infantry weapon: " + weapon );
        }

        // Loop through the men in the platoon, and assign damage based
        // upon the number of men it takes to do a point of damage.
        for ( man = 1, points = 1;
              man <= INF_PLT_MAX_MEN; 
              man++, points++ ) {

            // Round all fractions up.
            this.damage[man] = (int) Math.ceil( points / men_per_point );

            // MGs and Flamers do something wierd for the first point of damage
            if ( 1 == man && ( INF_MG == weapon || INF_FLAMER == weapon ) ) {
                points--;
            }

        } // Handle the next man in the platoon.

        // MGs and Flamers do something wierd for the last point of damage
        if ( INF_MG == weapon || INF_FLAMER == weapon ) {
            this.damage[INF_PLT_MAX_MEN] =
                (int) Math.ceil( INF_PLT_MAX_MEN / men_per_point );
        }

    } // End private void setDamage( int ) throws Exception

    // Public and Protected constants, constructors, and methods.

    /**
     * The maximum number of men in an infantry platoon.
     */
    public static final int     INF_PLT_MAX_MEN = 28;

    /**
     * The maximum number of men in an infantry platoon.
     */
    public static final int     INF_PLT_JUMP_MAX_MEN = 21;

    /**
     * The maximum number of men in an infantry platoon.
     */
    public static final int     INF_PLT_CLAN_MAX_MEN = 25;

    /*
     * The kinds of infantry platoons available.
     */
    // HACK!!! Reuse movement types from Mechs and Vehicles.
    public static final int     INF_LEG         = 1; // Entity.BIPED;
    public static final int     INF_MOTORIZED   = 4; // Entity.WHEELED;
    public static final int     INF_JUMP        = 5; // Entity.HOVER;

    /*
     * The kinds of weapons available to the PBI.
     *
     * By incredible luck, the AmmoType and WeaponType constants
     * do not overlap for these six weapons.
     */
    public static final int     INF_UNKNOWN     = -1;// T_NA
    public static final int     INF_RIFLE       = 1; // T_AC
    public static final int     INF_MG          = 3; // T_MG
    public static final int     INF_FLAMER      = 2; // F_FLAMER
    public static final int     INF_LASER       = 4; // F_LASER
    public static final int     INF_SRM         = 9; // T_SRM
    public static final int     INF_LRM         = 7; // T_LRM

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
        setMovementMode(INF_LEG);

        // Populate the damage array.
        this.setDamage(this.weapons);

        // Determine the number of MPs.
        this.setOriginalWalkMP(1);

        // Clear the weapon type to be set later.
        this.weapons = INF_UNKNOWN;
    }

    /**
     * Infantry have no secondary facing.
     */
    public int getSecondaryFacing() { return -1; }

    /**
     * Infantry have no secondary facing.
     */
    public boolean canChangeSecondaryFacing() { return false; }

    /**
     * Infantry have no secondary facing.
     */
    public boolean isValidSecondaryFacing( int dir ) { return false; }

    /**
     * Infantry have no secondary facing.
     */
    public int clipSecondaryFacing( int dir ) { return -1; }
    
    /**
     * Infantry have no piloting skill (set to 5 for BV purposes)
     */
    public void setCrew(Pilot p) {
        super.setCrew(new Pilot(p.getName(), p.getGunnery(), 5));
        this.getCrew().setHits(p.getHits());
    }

    /**
     * Return this Infantry's run MP.
     */
    public int getRunMP(boolean gravity) {
        if (gravity) return applyGravityEffectsOnMP(this.getOriginalRunMP());
        else return this.getOriginalRunMP();
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
     * Infantry can not enter water.
     */
    public boolean isHexProhibited( IHex hex ) {
        return (hex.terrainLevel(Terrains.WATER) > 0);
    }

    /**
     * Override the <code>Entity#getMovementTypeAsString()</code> method.
     */
    public String getMovementModeAsString() {
        switch (this.getMovementMode()) {
        case INF_LEG:
            return "Leg";
        case INF_MOTORIZED:
            return "Motorized";
        case INF_JUMP:
            return "Jump";
        default :
            return "Unknown!";
        }
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
            case INF_LEG:
                return "Walked";
            case INF_MOTORIZED:
                return "Biked";
            case INF_JUMP:
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
            case INF_LEG:
                return "R";
            case INF_MOTORIZED:
                return "B";
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
     * Infantry platoons *have* no armor (that's why they're PBI :).
     */
    public int getArmor( int loc, boolean rear ) {
        if ( !this.isPlatoon() ) {
            return super.getArmor( loc, rear );
        }
        return IArmorState.ARMOR_NA;
    }

    /**
     * Infantry platoons *have* no armor (that's why they're PBI :).
     */
    public int getOArmor( int loc, boolean rear ) {
        if ( !this.isPlatoon() ) {
            return super.getOArmor( loc, rear );
        }
        return IArmorState.ARMOR_NA;
    }

    /**
     * Infantry platoons *have* no armor (that's why they're PBI :).
     */
    public double getArmorRemainingPercent() {
        if ( !this.isPlatoon() ) {
            return super.getArmorRemainingPercent();
        }
        return IArmorState.ARMOR_NA;
    }


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
        case INF_LEG:
        case INF_MOTORIZED:
            this.initializeInternal( INF_PLT_MAX_MEN, LOC_INFANTRY );
            break;
        case INF_JUMP:
            this.initializeInternal( INF_PLT_JUMP_MAX_MEN, LOC_INFANTRY );
            break;
        default:
            throw new IllegalArgumentException
                ( "Unknown movement type: " + this.getMovementMode() );
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
            int weaponType;
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
                    (WeaponType.F_LASER + WeaponType.F_FLAMER );
            }
            this.weapons = weaponType;

            // Update our damage profile.
            this.setDamage( weaponType );

            // Inferno SRMs do half damage (rounded down).
            if ( weapon.hasFlag(WeaponType.F_INFERNO) ) {
                for ( int loop = 1; loop < damage.length; loop++ ) {
                    damage[loop] = (int) Math.floor( damage[loop] / 2.0 );
                }
            }

        }
        // Infantry platoons can't carry big equipment.
        else if ( this.isPlatoon() ) {
            throw new LocationFullException
                ( "Infantry platoons can not be equiped with a " +
                  mounted.getName() );
        }

        // Update our superclass.
        super.addEquipment( mounted, loc, rearMounted );
    }

    /**
     * Infantry can fire all around themselves.
     */
    public int getWeaponArc(int wn) { return Compute.ARC_360; }

    /**
     * Infantry have no secondary facing.
     */
    public boolean isSecondaryArcWeapon(int weaponId) { return false; }

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

        // BV is factor of anti-Mek training, movement type and weapon type.
        if ( this.antiMek ) {

            switch ( this.weapons ) {
            case INF_RIFLE:
                if ( INF_LEG == this.getMovementMode() )
                    dBV = 32;
                else if ( INF_MOTORIZED == this.getMovementMode() )
                    dBV = 42;
                else if ( INF_JUMP == this.getMovementMode() )
                    dBV = 46;
                else
                    throw new IllegalArgumentException
                        ( "Unknown movement type: " + this.getMovementMode() );
                break;
            case INF_MG:
                if ( INF_LEG == this.getMovementMode() )
                    dBV = 47;
                else if ( INF_MOTORIZED == this.getMovementMode() )
                    dBV = 63;
                else if ( INF_JUMP == this.getMovementMode() )
                    dBV = 62;
                else
                    throw new IllegalArgumentException
                        ( "Unknown movement type: " + this.getMovementMode() );
                break;
            case INF_FLAMER:
                if ( INF_LEG == this.getMovementMode() )
                    dBV = 41;
                else if ( INF_MOTORIZED == this.getMovementMode() )
                    dBV = 54;
                else if ( INF_JUMP == this.getMovementMode() )
                    dBV = 51;
                else
                    throw new IllegalArgumentException
                        ( "Unknown movement type: " + this.getMovementMode() );
                break;
            case INF_LASER:
                if ( INF_LEG == this.getMovementMode() )
                    dBV = 60;
                else if ( INF_MOTORIZED == this.getMovementMode() )
                    dBV = 70;
                else if ( INF_JUMP == this.getMovementMode() )
                    dBV = 71;
                else
                    throw new IllegalArgumentException
                        ( "Unknown movement type: " + this.getMovementMode() );
                break;
            case INF_SRM:
                if ( INF_LEG == this.getMovementMode() )
                    dBV = 60;
                else if ( INF_MOTORIZED == this.getMovementMode() )
                    dBV = 70;
                else if ( INF_JUMP == this.getMovementMode() )
                    dBV = 71;
                else
                    throw new IllegalArgumentException
                        ( "Unknown movement type: " + this.getMovementMode() );
                break;
            case INF_LRM:
                if ( INF_LEG == this.getMovementMode() )
                    dBV = 56;
                else if ( INF_MOTORIZED == this.getMovementMode() )
                    dBV = 75;
                else if ( INF_JUMP == this.getMovementMode() )
                    dBV = 87;
                else
                    throw new IllegalArgumentException
                        ( "Unknown movement type: " + this.getMovementMode() );
                break;
            default:
                throw new IllegalArgumentException
                    ( "Unknown infantry weapon: " + this.weapons );
            }
        } // End anti-Mek-trained
        else {
            switch ( this.weapons ) {
            case INF_RIFLE:
                if ( INF_LEG == this.getMovementMode() )
                    dBV = 23;
                else if ( INF_MOTORIZED == this.getMovementMode() )
                    dBV = 28;
                else if ( INF_JUMP == this.getMovementMode() )
                    dBV = 29;
                else
                    throw new IllegalArgumentException
                        ( "Unknown movement type: " + this.getMovementMode() );
                break;
            case INF_MG:
                if ( INF_LEG == this.getMovementMode() )
                    dBV = 31;
                else if ( INF_MOTORIZED == this.getMovementMode() )
                    dBV = 39;
                else if ( INF_JUMP == this.getMovementMode() )
                    dBV = 37;
                else
                    throw new IllegalArgumentException
                        ( "Unknown movement type: " + this.getMovementMode() );
                break;
            case INF_FLAMER:
                if ( INF_LEG == this.getMovementMode() )
                    dBV = 28;
                else if ( INF_MOTORIZED == this.getMovementMode() )
                    dBV = 35;
                else if ( INF_JUMP == this.getMovementMode() )
                    dBV = 32;
                else
                    throw new IllegalArgumentException
                        ( "Unknown movement type: " + this.getMovementMode() );
                break;
            case INF_LASER:
                if ( INF_LEG == this.getMovementMode() )
                    dBV = 37;
                else if ( INF_MOTORIZED == this.getMovementMode() )
                    dBV = 42;
                else if ( INF_JUMP == this.getMovementMode() )
                    dBV = 41;
                else
                    throw new IllegalArgumentException
                        ( "Unknown movement type: " + this.getMovementMode() );
                break;
            case INF_SRM:
                if ( INF_LEG == this.getMovementMode() )
                    dBV = 60;
                else if ( INF_MOTORIZED == this.getMovementMode() )
                    dBV = 70;
                else if ( INF_JUMP == this.getMovementMode() )
                    dBV = 71;
                else
                    throw new IllegalArgumentException
                        ( "Unknown movement type: " + this.getMovementMode() );
                break;
            case INF_LRM:
                if ( INF_LEG == this.getMovementMode() )
                    dBV = 56;
                else if ( INF_MOTORIZED == this.getMovementMode() )
                    dBV = 75;
                else if ( INF_JUMP == this.getMovementMode() )
                    dBV = 87;
                else
                    throw new IllegalArgumentException
                        ( "Unknown movement type: " + this.getMovementMode() );
                break;
            default:
                throw new IllegalArgumentException
                    ( "Unknown infantry weapon: " + this.weapons );
            }

        } // End not-anti-Mek

        // Adjust for missing troopers
        dBV = dBV * getInternalRemainingPercent();

        // Possibly adjust for TAG and Arrow IV.
        if (getsTagBVPenalty()) {
            dBV += 200;
        }
        if (getsHomingBVPenalty()) {
            dBV += 200;
        }

        // adjust for crew
        double pilotFactor = crew.getBVSkillMultiplier();
        dBV = dBV * pilotFactor;

        return (int)Math.round(dBV);

    } // End public int calculateBattleValue()

    /**
     * Generates a string containing a report on all useful information about
     * this entity.
     */
    public String victoryReport() {
        StringBuffer report = new StringBuffer();
        
        report.append(getDisplayName());
        report.append('\n');
        report.append("Gunnery skill : " + crew.getGunnery());
        report.append('\n');
        report.append("Kills: " + getKillNumber());
        report.append('\n');
        if (isCaptured()) {
            report.append("  *** CAPTURED BY THE ENEMY ***");
            report.append('\n');
        }        
        return report.toString();
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
     * Sets this entity's original walking movement points
     */
    public void setOriginalRunMP(int runMP) {
        this.runMP = runMP;
    }


    /**
     * @return The cost in C-Bills of the 'Mech in question.
     */
    public double getCost() {
        // FIXME
        // There should be an implementation here!
        double cost = 0;
        double multiplier = 0;
        
        if ( this.antiMek ) {
            multiplier = 5;
        } else {
            multiplier = 1;
        }
        switch ( this.weapons ) {
            case INF_RIFLE:
                if ( INF_LEG == this.getMovementMode() )
                    cost = 600000;
                else if ( INF_MOTORIZED == this.getMovementMode() )
                    cost = 960000;
                else if ( INF_JUMP == this.getMovementMode() )
                    cost = 1200000;
                else
                    throw new IllegalArgumentException
                        ( "Unknown movement type: " + this.getMovementMode() );
                break;
            case INF_MG:
                if ( INF_LEG == this.getMovementMode() )
                    cost = 800000;
                else if ( INF_MOTORIZED == this.getMovementMode() )
                    cost = 1280000;
                else if ( INF_JUMP == this.getMovementMode() )
                    cost = 1600000;
                else
                    throw new IllegalArgumentException
                        ( "Unknown movement type: " + this.getMovementMode() );
                break;
            case INF_FLAMER:
                if ( INF_LEG == this.getMovementMode() )
                    cost = 800000;
                else if ( INF_MOTORIZED == this.getMovementMode() )
                    cost = 1280000;
                else if ( INF_JUMP == this.getMovementMode() )
                    cost = 1600000;
                else
                    throw new IllegalArgumentException
                        ( "Unknown movement type: " + this.getMovementMode() );
                break;
            case INF_LASER:
                if ( INF_LEG == this.getMovementMode() )
                    cost = 1200000;
                else if ( INF_MOTORIZED == this.getMovementMode() )
                    cost = 1920000;
                else if ( INF_JUMP == this.getMovementMode() )
                    cost = 2400000;
                else
                    throw new IllegalArgumentException
                        ( "Unknown movement type: " + this.getMovementMode() );
                break;
            case INF_SRM:
                if ( INF_LEG == this.getMovementMode() )
                    cost = 1400000;
                else if ( INF_MOTORIZED == this.getMovementMode() )
                    cost = 2240000;
                else if ( INF_JUMP == this.getMovementMode() )
                    cost = 2800000;
                else
                    throw new IllegalArgumentException
                        ( "Unknown movement type: " + this.getMovementMode() );
                break;
            case INF_LRM:
                if ( INF_LEG == this.getMovementMode() )
                    cost = 1400000;
                else if ( INF_MOTORIZED == this.getMovementMode() )
                    cost = 2240000;
                else if ( INF_JUMP == this.getMovementMode() )
                    cost = 2800000;
                else
                    throw new IllegalArgumentException
                        ( "Unknown movement type: " + this.getMovementMode() );
                break;
            default:
                throw new IllegalArgumentException
                    ( "Unknown infantry weapon: " + this.weapons );
        } // End not-anti-Mek

        return cost*multiplier;
    }

    public boolean doomedInVacuum() {
        // FIXME
        // We're assuming that infantry have environmental suits of some sort.
        // Vac suits, battle armor, whatever.
        // This isn't necessarily a true assumption.
        return false;
    }
} // End class Infantry
