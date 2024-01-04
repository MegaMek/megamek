/*
 * MegaMek - Copyright (C) 2003, 2004 Ben Mazur (bmazur@sev.org)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.common;

import megamek.codeUtilities.StringUtility;

import java.util.Objects;

/**
 * Class to store pertinent quirk information. This class is immutable.
 *
 * @author Deric "Netzilla" Page (deric dot page at usa dot net)
 * @author Simon (Juliez)
 */
public class QuirkEntry {

    /** The code (OptionsConstants.*) of this quirk. Not the display name. */
    private final String code;

    /** The location String ("LA", "FR" etc.) of this weapon quirk. Empty for unit quirks. */
    private final String location;

    /** The slot (0 - 11) of this weapon quirk. -1 for unit quirks */
    private final int slot;

    /** The weapon internal name (e.g. CLERLargeLaser) of this weapon quirk. Empty for unit quirks. */
    private final String weaponName; //The weapon's name.

    /**
     * Use this constructor for building unit quirks.
     *
     * @param code  The quirk being created.
     * @param unitId The ID (chassis & model) of the unit to which the quirk belongs.
     */
    @Deprecated
    public QuirkEntry(String code, String unitId) {
        if (StringUtility.isNullOrBlank(code)) {
            throw new IllegalArgumentException("Quirk definition missing for " + unitId);
        }

        this.code = code;
        this.location = "";
        this.slot = -1;
        this.weaponName = "";
    }

    /**
     * Creates a unit quirk entry. The code should be a quirk code such as
     * {@link megamek.common.options.OptionsConstants#QUIRK_POS_COMMAND_MECH}. The code may not be null or empty
     * but is not otherwise checked if it is a valid value.
     *
     * @param code The quirk
     */
    public QuirkEntry(String code) {
        if (StringUtility.isNullOrBlank(code)) {
            throw new IllegalArgumentException("Invalid quirk code!");
        }

        this.code = code;
        this.location = "";
        this.slot = -1;
        this.weaponName = "";
    }

    /**
     * Use this constructor for building weapon quirks.
     *
     * @param code      The quirk being created.
     * @param location   The weapon's location (RT, LL, FF, LW, etc)
     * @param slot       The critical slot number (0-based) of the weapon's first critical.
     * @param weaponName The MegaMek name for the weapon (i.e. ISERLargeLaser)
     * @param unitId     The ID (chassis & model) of the unit to which the quirk belongs.
     */
    @Deprecated
    public QuirkEntry(String code, String location, int slot, String weaponName, String unitId) {
        if (StringUtility.isNullOrBlank(code)) {
            throw new IllegalArgumentException("Quirk definition missing for " + unitId);
        } else if (StringUtility.isNullOrBlank(location)) {
            throw new IllegalArgumentException("No location for " + code + " : " + unitId);
        } else if (StringUtility.isNullOrBlank(weaponName)) {
            throw new IllegalArgumentException("No weapon for " + code + " : " + unitId);
        } else if (slot < 0) {
            throw new IllegalArgumentException("Invalid slot index (" + slot + ") for " + code + " : " + unitId);
        }

        this.code = code;
        this.location = location;
        this.slot = slot;
        this.weaponName = weaponName;
    }

    /**
     * Use this constructor for building weapon quirks.
     *
     * @param code      The quirk being created.
     * @param location   The weapon's location (RT, LL, FF, LW, etc)
     * @param slot       The critical slot number (0-based) of the weapon's first critical.
     * @param weaponName The MegaMek name for the weapon (i.e. ISERLargeLaser)
     */
    public QuirkEntry(String code, String location, int slot, String weaponName) {
        if (StringUtility.isNullOrBlank(code)) {
            throw new IllegalArgumentException("Invalid quirk code!");
        } else if (StringUtility.isNullOrBlank(location)) {
            throw new IllegalArgumentException("Invalid location!");
        } else if (StringUtility.isNullOrBlank(weaponName)) {
            throw new IllegalArgumentException("Invalid weapon name!");
        } else if (slot < 0) {
            throw new IllegalArgumentException("Invalid slot index!");
        }

        this.code = code;
        this.location = location;
        this.slot = slot;
        this.weaponName = weaponName;
    }

    /** @return The location String ("LA", "FR" etc.) of this weapon quirk. Empty for unit quirks. */
    public String getLocation() {
        return location;
    }

    /** @return The code (OptionsConstants.*) of this quirk. Not useful as a display name. */
    public String getQuirk() {
        return code;
    }

    /** @return The slot (0 - 11 at most) of this weapon quirk. Returns -1 for unit quirks. */
    public int getSlot() {
        return slot;
    }

    /** @return The weapon internal name (e.g. CLERLargeLaser) of this weapon quirk. Empty for unit quirks. */
    public String getWeaponName() {
        return weaponName;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if ((null == obj) || (getClass() != obj.getClass())) {
            return false;
        }
        final QuirkEntry other = (QuirkEntry) obj;
        return Objects.equals(location, other.location) && Objects.equals(weaponName, other.weaponName)
                && (slot == other.slot) && Objects.equals(code, other.code);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code, location, weaponName, slot);
    }

    public boolean isWeaponQuirk() {
        return slot >= 0;
    }

    @Override
    public String toString() {
        return getQuirk() + (isWeaponQuirk() ? " [" + location + slot + ", " + weaponName + "]" : "");
    }
}
