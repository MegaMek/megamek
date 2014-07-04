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
 * @author Deric "Netzilla" Page (deric dot page at usa dot net)
 * @version %I% %G%
 * @since 2012-03-05
 */
class QuirkEntry {
    private String quirk; //The name of the quirk.

    //The following only apply to weapon quirks.
    private String location;   //The weapon's location.
    private int slot;          //The weapon's critical slot.
    private String weaponName; //The weapon's name.

    private QuirkEntry() {
    }

    /**
     * Use this constructor for building unit quirks.
     *
     * @param quirk  The quirk being created.
     * @param unitId The ID (chassis & model) of the unit to which the quirk belongs.
     */
    QuirkEntry(String quirk, String unitId) {
        if (StringUtil.isNullOrEmpty(quirk)) {
            throw new IllegalArgumentException("Quirk definition missing for " + unitId);
        }

        setQuirk(quirk);
        setLocation(null);
        setSlot(-1);
        setWeaponName(null);
    }

    /**
     * Use this constructor for building weapon quirks.
     *
     * @param quirk      The quirk being created.
     * @param location   The weapon's location (RT, LL, FF, LW, etc)
     * @param slot       The critical slot number (0-based) of the weapon's first critical.
     * @param weaponName The MegaMek name for the weapon (i.e. ISERLargeLaser)
     * @param unitId     The ID (chassis & model) of the unit to which the quirk belongs.
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

        setQuirk(quirk);
        setLocation(location);
        setSlot(slot);
        setWeaponName(weaponName);
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

    private void setLocation(String location) {
        this.location = location;
    }

    /**
     * Returns the name of the quirk.
     *
     * @return
     */
    public String getQuirk() {
        return quirk;
    }

    private void setQuirk(String quirk) {
        this.quirk = quirk;
    }

    /**
     * Returns the critical slot of the quirk.  Only applies to weapon quirks.  For unit quirks this value will be -1.
     *
     * @return
     */
    public int getSlot() {
        return slot;
    }

    private void setSlot(int slot) {
        this.slot = slot;
    }

    /**
     * Returns the name of the weapon to which this quirk belongs.  If this is a unit quirk, this value will be null.
     *
     * @return
     */
    public String getWeaponName() {
        return weaponName;
    }

    private void setWeaponName(String weaponName) {
        this.weaponName = weaponName;
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

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof QuirkEntry) && equals((QuirkEntry) obj);
    }

    public boolean equals(QuirkEntry quirk) {
        if (!getQuirk().equalsIgnoreCase(quirk.getQuirk())) {
            return false;
        } else if (StringUtil.isNullOrEmpty(getLocation()) && !StringUtil.isNullOrEmpty(quirk.getLocation())) {
            return false;
        } else if (!StringUtil.isNullOrEmpty(getLocation()) && StringUtil.isNullOrEmpty(quirk.getLocation())) {
            return false;
        } else if (!StringUtil.isNullOrEmpty(getLocation()) && !getLocation().equals(quirk.getLocation())) {
            return false;
        } else if (StringUtil.isNullOrEmpty(getWeaponName()) && !StringUtil.isNullOrEmpty(quirk.getWeaponName())) {
            return false;
        } else if (!StringUtil.isNullOrEmpty(getWeaponName()) && StringUtil.isNullOrEmpty(quirk.getWeaponName())) {
            return false;
        } else if (!StringUtil.isNullOrEmpty(getWeaponName()) && !getLocation().equals(quirk.getWeaponName())) {
            return false;
        } else if (getSlot() != quirk.getSlot()) {
            return false;
        }
        return true;
    }

    public QuirkEntry copy() {
        QuirkEntry copy = new QuirkEntry();
        copy.setQuirk(String.copyValueOf(getQuirk().toCharArray()));

        // If location is empty, then this is not a weapon quirk.
        if (StringUtil.isNullOrEmpty(getLocation())) {
            return copy;
        }

        copy.setWeaponName(String.copyValueOf(getWeaponName().toCharArray()));
        copy.setSlot(getSlot());
        copy.setLocation(String.copyValueOf(getLocation().toCharArray()));
        return copy;
    }
}
