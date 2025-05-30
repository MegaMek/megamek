/*
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
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

import java.util.Hashtable;
import java.util.Objects;

import megamek.common.weapons.Weapon;

/**
 * <p>
 * Class EquipmentMode describes a Equipment's particular mode.
 * <p>
 * The <code>getDisplayableName</code> method allows you to obtain the localized string from a predefined
 * <code>ResourceBundle</code>.
 * <p>
 * The <code>equals</code> function allows to check if the mode is equivalent to the mode identified by the given name.
 * <p>
 * There is no way to create the instance of the <code>EquipmentMode</code> directly, use
 * <code>EquipmentMode#getMode</code> instead.
 *
 * @see megamek.common.EquipmentType
 * @see megamek.common.Mounted
 */
public class EquipmentMode {

    /**
     * Hash of all modes
     */
    protected static Hashtable<String, EquipmentMode> modesHash = new Hashtable<>();

    /**
     * Unique internal mode identifier. Used as the part of the key to look for the displayable name presented to user.
     */
    protected String name;

    /**
     * <p>
     * Protected constructor since we don't allow direct creation of the mode. Modes available via <code>getMode</code>
     * <p>
     * Constructs the new mode denoted by the given name.
     *
     * @param name unique mode identifier
     */
    protected EquipmentMode(String name) {
        this.name = Objects.requireNonNull(name);
    }

    /**
     * @return mode name/identifier
     */
    public String getName() {
        return name;
    }

    /**
     * @return the localized displayable name presented by the GUI to the user.
     */
    public String getDisplayableName() {
        return getDisplayableName(false);
    }

    /**
     * @return the localized displayable name presented by the GUI to the user.
     */
    public String getDisplayableName(boolean wantNormal) {
        String result = EquipmentMessages.getString("EquipmentMode." + name);
        if ((result != null) && (!wantNormal || !result.isBlank())) {
            return result;
        }

        if (wantNormal) {
            return EquipmentMessages.getString("EquipmentMode.Normal");
        } else {
            return name;
        }
    }

    /**
     * @param name mode name
     *
     * @return unique mode that corresponds to the given name
     */
    public static EquipmentMode getMode(String name) {
        return modesHash.computeIfAbsent(name, EquipmentMode::new);
    }

    /**
     * @param modeName The name of the mode to compare with. Overloaded just for {@code String} classes.
     *
     * @return <code>true</code> if this mode equals to the mode denoted by the given name
     */
    @Override
    public boolean equals(Object modeName) {
        if (modeName instanceof EquipmentMode) {
            return this.hashCode() == modeName.hashCode();
        }

        return false;
    }

    /**
     * @param modeName The name of the mode to compare with. Overloaded just for {@code String} classes.
     *
     * @return <code>true</code> if this mode equals to the mode denoted by the given name
     */
    public boolean equals(String modeName) {
        return name.equals(modeName);
    }

    @Override
    public String toString() {
        return getName();
    }

    public boolean isHeat() {
        return name.equals(Weapon.MODE_FLAMER_HEAT) || name.equals(Weapon.MODE_INDIRECT_HEAT);
    }

    public boolean isIndirect() {
        return name.equals(Weapon.MODE_MISSILE_INDIRECT) || name.equals(Weapon.MODE_INDIRECT_HEAT);
    }

    public boolean isArmed() {
        return name.equalsIgnoreCase("armed");
    }

    public boolean isOff() {
        return name.equalsIgnoreCase("off");
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
