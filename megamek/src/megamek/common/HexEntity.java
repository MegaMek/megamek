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
public class HexEntity
    extends Entity
    implements Serializable
{
    private static final int[]  NUM_OF_SLOTS    = {0};
    private static final String[] LOCATION_ABBRS= { "Terrain" };
    private static final String[] LOCATION_NAMES= { "Terrain" };

    public int locations() { return 1; }

    /**
     * Generate a new, blank, hex entity
     */
    public HexEntity() {
        // Instantiate the superclass.
        super();

    }

    public boolean isImmobile() { return true; }
    public HexEntity(Coords c) {
        // Instantiate the superclass.
        super();
        setPosition(c);
    }
    /**
     * Hexes have no secondary facing.
     */
    public int getSecondaryFacing() { return -1; }
    public boolean canChangeSecondaryFacing() { return false; }
    public boolean isValidSecondaryFacing( int dir ) { return false; }
    public int clipSecondaryFacing( int dir ) { return -1; }

    /**
     * Hexes can't move.
     */
    public int getWalkMP() { 
        return 0;
    }
    public int getOriginalWalkMP() { 
        return 0;
    }
    public int getRunMP() {
        return 0;
    }

    protected int getOriginalRunMP() {
        return 0;
    }

    public int getJumpMP() {
        return 0;
    }

    protected int getOriginalJumpMP() { 
        return 0;
    }

    public int getJumpMPWithTerrain() {
        return 0;
    }

    public boolean isHexProhibited( Hex hex ) {
        return true;
    }

    public String getMovementString(int mtype) {
        return "Immobile";
    }

    public String getMovementAbbr(int mtype) {
        return "I";
    }

    /**
     * Hexes only have one hit location.
     */
    public HitData rollHitLocation( int table, int side ) {
        return new HitData( 0 );
    }

    public HitData getTransferLocation(HitData hit) { 
        return new HitData(Entity.LOC_DESTROYED);
    }

    public int getDependentLocation(int loc) {
        return Entity.LOC_NONE;
    }

    public boolean hasRearArmor(int loc) {
        return false;
    }

    public int getArmor( int loc, boolean rear ) { return Entity.ARMOR_NA; }
    public int getOArmor( int loc, boolean rear ) { return Entity.ARMOR_NA; }
    public double getArmorRemainingPercent() { return 0.0; }

    public int getInternal( int loc, boolean rear ) {
        return 1;
    }
    public int getOInternal( int loc, boolean rear ) {
        return 1;
    }

    public int calculateBattleValue() {
        return 0;
    }

    public int getMaxElevationChange()
    {
        return 0;
    }

    public PilotingRollData addEntityBonuses(PilotingRollData prd)
    {
        return prd;
    }

    public int getHeatCapacity() {
        return 0;
    }

    public int getHeatCapacityWithWater() {
        return 0;
    }

    public String[] getLocationAbbrs() { return LOCATION_ABBRS; }
    public String[] getLocationNames() { return LOCATION_NAMES; }

    public void autoSetInternal() { return; }

    public String getDisplayName() { 
      String hName = "Hex ";
      if (getPosition().x < 9) hName += "0";
      hName += (getPosition().x + 1);
      if (getPosition().y < 9) hName += "0";
      hName += (getPosition().y + 1);
      return hName; 
    }

    public int getWeaponArc(int wn) { return Compute.ARC_360; }

    public boolean isSecondaryArcWeapon(int weaponId) { return false; }

    public String victoryReport() { return ""; }

    protected int[] getNoOfSlots() { return NUM_OF_SLOTS; }

    public boolean isDestroyed() { return false; }
    public boolean isDoomed() { return false; }

} // End class HexEntity
