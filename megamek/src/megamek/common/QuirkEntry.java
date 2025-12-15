/*
 * Copyright (C) 2003, 2004 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2012-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package megamek.common;

import java.util.Objects;

import jakarta.annotation.Nonnull;
import megamek.codeUtilities.StringUtility;
import megamek.common.options.OptionsConstants;

/**
 * Class to store pertinent quirk information. This class is immutable.
 *
 * @param code       The code (OptionsConstants.*) of this quirk. Not the display name.
 * @param value      Optional value for quirks that require parameters (e.g., "2750" for obsolete quirk). Empty for
 *                   boolean quirks.
 * @param location   The location String ("LA", "FR" etc.) of this weapon quirk. Empty for unit quirks.
 * @param slot       The slot (0 - 11) of this weapon quirk. -1 for unit quirks
 * @param weaponName The weapon internal name (e.g. CLERLargeLaser) of this weapon quirk. Empty for unit quirks. The
 *                   weapon's name.
 *
 * @author Deric "Netzilla" Page (deric dot page at usa dot net)
 * @author Simon (Juliez)
 */
public record QuirkEntry(String code, String value, String location, int slot, String weaponName) {

    /**
     * Creates a unit quirk entry from a string that may contain a value (e.g., "obsolete:2750" or just "command_mek").
     * The code should be a quirk code such as {@link OptionsConstants#QUIRK_POS_COMMAND_MEK}. The code may not be null
     * or empty but is not otherwise checked if it is a valid value.
     *
     * @param quirkString The quirk string, optionally with a colon-separated value (e.g., "obsolete:2750")
     */
    public QuirkEntry(String quirkString) {
        this(validateAndParseCode(quirkString), parseValue(quirkString), "", -1, "");
    }

    /**
     * Creates a unit quirk entry with a specific code and value.
     *
     * @param code  The quirk code
     * @param value The quirk value (empty string for boolean quirks)
     */
    public QuirkEntry(String code, String value) {
        this(code, value, "", -1, "");

        if (StringUtility.isNullOrBlank(code)) {
            throw new IllegalArgumentException("Invalid quirk code!");
        }
    }

    /**
     * Use this constructor for building weapon quirks.
     *
     * @param code       The quirk being created.
     * @param location   The weapon's location (RT, LL, FF, LW, etc.)
     * @param slot       The critical slot number (0-based) of the weapon's first critical.
     * @param weaponName The MegaMek name for the weapon (i.e. ISERLargeLaser)
     */
    public QuirkEntry(String code, String location, int slot, String weaponName) {
        this(code, "", location, slot, weaponName);
    }

    /**
     * Canonical constructor for weapon quirks and full specification.
     */
    public QuirkEntry {
        if (StringUtility.isNullOrBlank(code)) {
            throw new IllegalArgumentException("Invalid quirk code!");
        } else if (value == null) {
            throw new IllegalArgumentException("Invalid value (use empty string for boolean quirks)!");
        } else if (location == null) {
            throw new IllegalArgumentException("Invalid location!");
        } else if (weaponName == null) {
            throw new IllegalArgumentException("Invalid weapon name!");
        } else if (slot < -1) {
            throw new IllegalArgumentException("Invalid slot index!");
        }
    }

    private static String validateAndParseCode(String quirkString) {
        if (StringUtility.isNullOrBlank(quirkString)) {
            throw new IllegalArgumentException("Invalid quirk string!");
        }
        int colonIndex = quirkString.indexOf(':');
        return colonIndex >= 0 ? quirkString.substring(0, colonIndex) : quirkString;
    }

    private static String parseValue(String quirkString) {
        if (quirkString == null) {
            return "";
        }
        int colonIndex = quirkString.indexOf(':');
        return colonIndex >= 0 ? quirkString.substring(colonIndex + 1) : "";
    }

    /**
     * @return The location String ("LA", "FR" etc.) of this weapon quirk. Empty for unit quirks.
     */
    @Override
    public String location() {
        return location;
    }

    /**
     * @return The code (OptionsConstants.*) of this quirk. Not useful as a display name.
     */
    public String getQuirk() {
        return code;
    }

    /**
     * @return The slot (0 - 11 at most) of this weapon quirk. Returns -1 for unit quirks.
     */
    @Override
    public int slot() {
        return slot;
    }

    /**
     * @return The weapon internal name (e.g. CLERLargeLaser) of this weapon quirk. Empty for unit quirks.
     */
    @Override
    public String weaponName() {
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
        return Objects.equals(location, other.location) &&
              Objects.equals(weaponName, other.weaponName) &&
              (slot == other.slot) &&
              Objects.equals(code, other.code) &&
              Objects.equals(value, other.value);
    }

    /**
     * @return true if this quirk has a value (e.g., obsolete:2750), false for boolean quirks
     */
    public boolean hasValue() {
        return !StringUtility.isNullOrBlank(value);
    }

    public boolean isWeaponQuirk() {
        return slot >= 0;
    }

    @Override
    @Nonnull
    public String toString() {
        return getQuirk() + (isWeaponQuirk() ? " [" + location + slot + ", " + weaponName + "]" : "");
    }
}
