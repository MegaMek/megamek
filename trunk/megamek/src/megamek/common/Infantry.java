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
import java.util.Enumeration;

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
     * The MP of this platoon.
     */
    private int         movePoints   = 0;

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
            throw new IllegalArgumentException
                ( "LRM infantry not yet implemented." );
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

    /**
     * Set the MPs for this platoon based upon its type and weapons.
     *
     * @param   type - the movement type of infantry in this platoon.
     * @param   weapon - the type of weapons used by this platoon.
     * @exception <code>IllegalArgumentException</code> if a bad mobility
     *          type or weapon type is supplied.
     */
    private void setMovePoints( int type, int weapon ) {
        // Handle the various weapons types.
        switch ( weapon ) {
        case INF_RIFLE:
            if ( INF_LEG == type )
                this.movePoints= 1;
            else if ( INF_MOTORIZED == type )
                this.movePoints= 3;
            else if ( INF_JUMP == type )
                this.movePoints= 4;
            else
                throw new IllegalArgumentException
                    ( "Unknown movement type: " + type );
            break;
        case INF_MG:
        case INF_FLAMER:
            if ( INF_LEG == type )
                this.movePoints= 1;
            else if ( INF_MOTORIZED == type )
                this.movePoints= 3;
            else if ( INF_JUMP == type )
                this.movePoints= 3;
            else
                throw new IllegalArgumentException
                    ( "Unknown movement type: " + type );
            break;
        case INF_LASER:
        case INF_SRM:
            if ( INF_LEG == type )
                this.movePoints= 1;
            else if ( INF_MOTORIZED == type )
                this.movePoints= 2;
            else if ( INF_JUMP == type )
                this.movePoints= 2;
            else
                throw new IllegalArgumentException
                    ( "Unknown movement type: " + type );
            break;
        case INF_LRM:
            throw new IllegalArgumentException
                ( "LRM infantry not yet implemented." );
        default:
            throw new IllegalArgumentException
                ( "Unknown infantry weapon: " + weapon );
        }

    } // end private void setMovePoints( int, int )

    // Public constants, constructors, and methods.

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
    public static final int     INF_PLT_CLAN_LEG_MAX_MEN = 25;

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
        setMovementType(INF_LEG);

        // Populate the damage array.
        this.setDamage(this.weapons);

        // Determine the number of MPs.
        this.setMovePoints(this.getMovementType(), this.weapons);
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
     * Infantry have only one speed.
     */
    public int getWalkMP() { 
	// Jump infantry hava a walk of 1.
        return (INF_JUMP == this.getMovementType() ? 1 : this.movePoints );
    }

    /**
     * Infantry have only one speed.
     */
    public int getOriginalWalkMP() { 
	// Jump infantry hava a walk of 1.
        return (INF_JUMP == this.getMovementType() ? 1 : this.movePoints );
    }

    /**
     * Infantry have only one speed.
     */
    public int getRunMP() {
	// Jump infantry hava a walk of 1.
        return (INF_JUMP == this.getMovementType() ? 1 : this.movePoints );
    }

    /**
     * Infantry have only one speed.
     */
    protected int getOriginalRunMP() {
	// Jump infantry hava a walk of 1.
        return (INF_JUMP == this.getMovementType() ? 1 : this.movePoints );
    }

    /**
     * Infantry have only one speed.  They can only jump if they are jump inf.
     */
    public int getJumpMP() {
        return (INF_JUMP == this.getMovementType() ? this.movePoints: 0);
    }

    /**
     * Infantry have only one speed.  They can only jump if they are jump inf.
     */
    protected int getOriginalJumpMP() { 
        return (INF_JUMP == this.getMovementType() ? this.movePoints: 0);
    }

    /**
     * Infantry have only one speed.  They can only jump if they are jump inf.
     */
    public int getJumpMPWithTerrain() {
        return (INF_JUMP == this.getMovementType() ? this.movePoints: 0);
    }

    /**
     * Infantry can not enter water.
     */
    public boolean isHexProhibited( Hex hex ) {
        return (hex.levelOf(Terrain.WATER) > 0);
    }

    /**
     * Returns the name of the type of movement used.
     * This is Infantry-specific.
     */
    public String getMovementString(int mtype) {
        switch(mtype) {
        case MOVE_NONE :
            return "None";
        case MOVE_WALK :
        case MOVE_RUN :
        case MOVE_JUMP :
            switch (this.getMovementType()) {
            case INF_LEG:
                return "Walked";
            case INF_MOTORIZED:
                return "Biked";
            case INF_JUMP:
                return "Jumped";
            default :
                return "Unknown!";
            }
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
        case MOVE_NONE :
            return "N";
        case MOVE_WALK :
        case MOVE_RUN :
        case MOVE_JUMP :
            switch (this.getMovementType()) {
            case INF_LEG:
                return "W";
            case INF_MOTORIZED:
                return "B";
            case INF_JUMP:
                return "J";
            default :
                return "?";
            }
        default :
            return "?";
        }
    }

    /**
     * Infantry only have one hit location.
     */
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
     * Infantry *have* no armor (that's why they're PBI :).
     */
    public int getArmor( int loc, boolean rear ) { return Entity.ARMOR_NA; }

    /**
     * Infantry *have* no armor (that's why they're PBI :).
     */
    public int getOArmor( int loc, boolean rear ) { return Entity.ARMOR_NA; }

    /**
     * Infantry *have* no armor (that's why they're PBI :).
     */
    public double getArmorRemainingPercent() { return 0.0; }

    /**
     * Returns the number of men left in the platoon, or Entity.ARMOR_DESTROYED.
     */
    public int getInternal( int loc, boolean rear ) {
        return ( this.men > 0 ? this.men : Entity.ARMOR_DESTROYED );
    }

    /**
     * Returns the number of men originally the platoon.
     */
    public int getOInternal( int loc, boolean rear ) {
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
        setInternal( val, loc );
      }

    /**
     * Set the men in the platoon to the appropriate value for the 
     * platoon's movement type.
     */
    public void autoSetInternal() {
        switch (this.getMovementType()) {
        case INF_LEG:
            // Clan leg platoons have less men (???)...
            if ( this.isClan() ) {
                this.initializeInternal( INF_PLT_CLAN_LEG_MAX_MEN,
					 LOC_INFANTRY );
                return;
            }
            // ... otherwise, fall through.
        case INF_MOTORIZED:
            this.initializeInternal( INF_PLT_MAX_MEN, LOC_INFANTRY );
            break;
        case INF_JUMP:
            this.initializeInternal( INF_PLT_JUMP_MAX_MEN, LOC_INFANTRY );
            break;
        default:
            throw new IllegalArgumentException
                ( "Unknown movement type: " + this.getMovementType() );
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
	WeaponType weapon;
	int weaponType;

	// Infantry can only mount an infantry weapon.
	if ( !(mounted.getType() instanceof WeaponType) ) {
	    throw new LocationFullException
		( "Infantry can not be equiped with a " + mounted.getName() );
	}
	else {
	    weapon = (WeaponType) mounted.getType();
	    // Make certain the weapon is for infantry.
	    if ( (weapon.getFlags() & WeaponType.F_INFANTRY) != 
		 WeaponType.F_INFANTRY ) {
		throw new LocationFullException
		    ( "A " + weapon.getName() + " is too big for infantry" );
	    }
	}

        // The PBI can only mount one weapon.
        if (this.getEquipment().hasMoreElements()) {
	    weapon = (WeaponType) this.getEquipment(0).getType();
	    throw new LocationFullException
		( "Platoon is already equiped with an " + weapon.getName() +
		  " and does not need a " + mounted.getType().getName() );
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

        // Update our movement points.
	this.setMovePoints( this.getMovementType(), weaponType );

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
     * Infantry have no critical slots.
     */
    protected int[] getNoOfSlots() { return NUM_OF_SLOTS; }

    /**
     * Calculates the battle value of this platoon.
     */
    public int calculateBattleValue() {
        // BV is factor of movement type and weapon type.
        switch ( this.weapons ) {
        case INF_RIFLE:
            if ( INF_LEG == this.getMovementType() )
                return 23;
            else if ( INF_MOTORIZED == this.getMovementType() )
                return 28;
            else if ( INF_JUMP == this.getMovementType() )
                return 29;
            else
                throw new IllegalArgumentException
                    ( "Unknown movement type: " + this.getMovementType() );
        case INF_MG:
            if ( INF_LEG == this.getMovementType() )
                return 31;
            else if ( INF_MOTORIZED == this.getMovementType() )
                return 39;
            else if ( INF_JUMP == this.getMovementType() )
                return 37;
            else
                throw new IllegalArgumentException
                    ( "Unknown movement type: " + this.getMovementType() );
        case INF_FLAMER:
            if ( INF_LEG == this.getMovementType() )
                return 28;
            else if ( INF_MOTORIZED == this.getMovementType() )
                return 35;
            else if ( INF_JUMP == this.getMovementType() )
                return 32;
            else
                throw new IllegalArgumentException
                    ( "Unknown movement type: " + this.getMovementType() );
        case INF_LASER:
            if ( INF_LEG == this.getMovementType() )
                return 37;
            else if ( INF_MOTORIZED == this.getMovementType() )
                return 42;
            else if ( INF_JUMP == this.getMovementType() )
                return 41;
            else
                throw new IllegalArgumentException
                    ( "Unknown movement type: " + this.getMovementType() );
        case INF_SRM:
            if ( INF_LEG == this.getMovementType() )
                return 60;
            else if ( INF_MOTORIZED == this.getMovementType() )
                return 70;
            else if ( INF_JUMP == this.getMovementType() )
                return 71;
            else
                throw new IllegalArgumentException
                    ( "Unknown movement type: " + this.getMovementType() );
        case INF_LRM:
            /*
            if ( INF_LEG == this.getMovementType() )
                return 56;
            else if ( INF_MOTORIZED == this.getMovementType() )
                return 75;
            else if ( INF_JUMP == this.getMovementType() )
                return 87;
            else
                throw new IllegalArgumentException
                    ( "Unknown movement type: " + this.getMovementType() );
            */
            throw new IllegalArgumentException
                ( "LRM infantry not yet implemented." );
        default:
            throw new IllegalArgumentException
                ( "Unknown infantry weapon: " + this.weapons );
        }

    } // End public int calculateBattleValue()

    /**
     * Generates a string containing a report on all useful information about
     * this entity.
     */
    public String victoryReport() { return getDisplayName(); }

    /**
     * Set the movement type of the entity
     */
    public void setMovementType(int movementType) {
	// Call the base class' method.
	super.setMovementType( movementType );

	// Update this platoon's movement points.
	this.setMovePoints( this.getMovementType(), this.weapons );
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
    public void applyDamage() { menShooting = men; }

    // The methods below aren't in the Entity interface.

    /**
     * Determine the amount of damage that the infantry can produce,
     * given a number of men left in the platoon.
     *
     * @param menLeft - the number of men in the platoon capable of shooting.
     * @return <code>int</code> - the amount of damage done when the platoon
     *		hits its target.
     */
    public int getDamage( int menLeft ) { return damage[menLeft]; }

    /**
     * Get the number of men in the platoon (before damage is applied).
     */
    public int getShootingStrength() { return menShooting; }

} // End class Infantry
