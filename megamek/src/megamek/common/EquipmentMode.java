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

/**
 * <p>
 * Class EquipmentMode describes a Equipment's particular mode.
 * <p>
 * The <code>getDisplayableName</code> method allows you to obtain the
 * localized string from a predefined <code>ResourceBundle</code>.
 * <p>
 * The <code>equals</code> function allows to check if the mode is equivalent
 * to the mode identified by the given name.
 * <p>
 * There is no way to create the instance of the <code>EquipmentMode</code>
 * directly, use </code>EquipmentMode#getMode</code> instead.
 * 
 * @see megamek.common.EquipmentType
 * @see megamek.common.Mounted
 */
public class EquipmentMode {

    /**
     * Hash of all modes
     */
    protected static Hashtable<String, EquipmentMode> modesHash = new Hashtable<String, EquipmentMode>();

    /**
     * Unique internal mode identifier. Used as the part of the key to look for
     * the displayable name presented to user.
     */
    protected String name;

    /**
     * <p>
     * Protected constructor since we don't allow direct creation of the mode.
     * Modes available via <code>getMode</code>
     * <p>
     * Contructs the new mode denoted by the given name.
     * 
     * @param name unique mode identifier
     */
    protected EquipmentMode(String name) {
        megamek.debug.Assert.assertTrue(name != null, "Name must not be null");
        this.name = name;
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
        String result = EquipmentMessages.getString("EquipmentMode." + name);
        if (result != null)
            return result;
        return name;
    }

    /**
     * @param name mode name
     * @return unique mode that corresponds to the given name
     */
    public static EquipmentMode getMode(String name) {
        EquipmentMode mode = modesHash.get(name);
        if (mode == null) {
            mode = new EquipmentMode(name);
            modesHash.put(name, mode);
        }
        return mode;
    }

    /**
     * @param modeName The name of the mode to compare with
     * @return <code>true</code> if this mode equals to the mode denoted by
     *         the given name
     */
    public boolean equals(String modeName) {
        return name.equals(modeName);
    }
}
