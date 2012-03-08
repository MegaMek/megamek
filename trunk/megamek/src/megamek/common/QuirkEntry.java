/*
 * MegaMek - Copyright (C) 2003, 2004 Ben Mazur (bmazur@sev.org)
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

import megamek.common.util.StringUtil;

/**
 * Class to store pertinent quirk information.
 *
 * @author Deric Page (dericpage@users.sourceforge.net)
 * @version %I% %G%
 * @since 2012-03-05
 */
class QuirkEntry {
    private String quirk; //The name of the quirk.

    //The following only apply to weapon quirks.
    private String location;   //The weapon's location.
    private int slot;          //The weapon's critical slot.
    private String weaponName; //The weapon's name.

    /**
     * Use this constructor for building unit quirks.
     *
     * @param quirk The quirk being created.
     * @param unitId The ID (chassis & model) of the unit to which the quirk belongs.
     */
    QuirkEntry(String quirk, String unitId) {
        if (StringUtil.isNullOrEmpty(quirk)) {
            throw new IllegalArgumentException("Quirk definition missing for " + unitId);
        }

        this.quirk = quirk;
        this.location = null;
        this.slot = -1;
        this.weaponName = null;
    }

    /**
     * Use this constructor for building weapon quirks.
     *
     * @param quirk The quirk being created.
     * @param location The weapon's location (RT, LL, FF, LW, etc)
     * @param slot The critical slot number (0-based) of the weapon's first critical.
     * @param weaponName The MegaMek name for the weapon (i.e. ISERLargeLaser)
     * @param unitId The ID (chassis & model) of the unit to which the quirk belongs.
     */
    QuirkEntry(String quirk, String location, int slot, String weaponName, String unitId) {
        if (StringUtil.isNullOrEmpty(quirk)) {
            throw new IllegalArgumentException("Quirk definition missing for " + unitId);
        }
        if (StringUtil.isNullOrEmpty(location)) {
            throw new IllegalArgumentException("No location for " + quirk + " : " + unitId);
        }
        if (StringUtil.isNullOrEmpty(weaponName)) {
            throw new IllegalArgumentException("No weapon for " + quirk + " : " + unitId);
        }
        if (slot < 0 || slot > 11) {
            throw new IllegalArgumentException("Invalid slot index (" + slot + ") for " + quirk + " : " + unitId);
        }

        this.quirk = quirk;
        this.location = location;
        this.slot = slot;
        this.weaponName = weaponName;
    }

    /**
     * Returns the chassis location of the quirk (RT, LL, FF, LW, etc).  Only applies to weapon quirks.  For unit quirks
     * this value will be null.
     *
     * @return
     */
    public String getLocation() {
        return location;
    }

    /**
     * Returns the name of the quirk.
     * @return
     */
    public String getQuirk() {
        return quirk;
    }

    /**
     * Returns the critical slot of the quirk.  Only applies to weapon quirks.  For unit quirks this value will be -1.
     *
     * @return
     */
    public int getSlot() {
        return slot;
    }

    /**
     * Returns the name of the weapon to which this quirk belongs.  If this is a unit quirk, this value will be null.
     *
     * @return
     */
    public String getWeaponName() {
        return weaponName;
    }

    /**
     * Returns a log statement describing this quirk.
     *
     * @return
     */
    public String toLog() {
        String out = getQuirk();
        if (StringUtil.isNullOrEmpty(getLocation()))
            return out;

        return out + "\t" + getLocation() + "\t" + getSlot() + "\t" + getWeaponName();
    }
}
