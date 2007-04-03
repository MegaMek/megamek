/*
 * MegaMek -
 * Copyright (C) 2000,2001,2002,2003,2004,2005 Ben Mazur (bmazur@sev.org)
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

public abstract class AeroDyne extends AeroSpace {
    /**
     * This is kinda less meaningful for AeroSpace units.
     * They can power-climb and dive for impressive elevation changes.
     * However, GROUNDED Aerodynes can maneuver as if wheeled vehicles.
     * Therefore, we'll just use their value in here.
     */
    public int getMaxElevationChange() { return 1; }

    public PilotingRollData addEntityBonuses(PilotingRollData roll) {
        //FIXME
        return roll;
    }

    //FIXME
    public boolean canChangeSecondaryFacing(){ return false; }

    public boolean isValidSecondaryFacing(int dir) {
        //FIXME
        return false;
    }

    //FIXME
    public boolean isSecondaryArcWeapon(int weaponId) {return false;}

    //FIXME
    public int getWeaponArc(int wn) {return -1;}

    //FIXME
    public boolean hasRearArmor(int loc) {return false;}

    //FIXME
    public HitData getTransferLocation(HitData hit) {return null;}

    //FIXME
    public HitData rollHitLocation(int table, int side, int aimedLocation, int aimingMode) {return null;}
    public HitData rollHitLocation(int table, int side) {return null;}

    //FIXME
    protected String[] getLocationAbbrs() {return null;}

    //FIXME
    protected String[] getLocationNames() {return null;}

    //FIXME
    public int locations() {return 0;}
}
