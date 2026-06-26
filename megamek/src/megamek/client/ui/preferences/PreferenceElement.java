/*
 * Copyright (C) 2019-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.preferences;

import megamek.codeUtilities.StringUtility;

/**
 * Represents the set of information needed to be part of the user preferences system for MegaMek.
 * <p>
 * A preference is a value that the user can set anywhere in MegaMek and that will be persisted between user sessions.
 * <p>
 * PreferenceElements are contained inside {@link PreferencesNode}s, forming a tree. You can imagine a PreferenceElement
 * as a field, and a PreferencesNode as a class.
 * <p>
 * This class is not thread-safe.
 */
public abstract class PreferenceElement {
    //region Variable Declarations
    private final String name;
    //endregion Variable Declarations

    //region Constructors
    protected PreferenceElement(final String name) throws Exception {
        if (StringUtility.isNullOrBlank(name)) {
            throw new Exception("Cannot create PreferenceElement with null or empty name");
        }

        this.name = name;
    }
    //endregion Constructors

    //region Getters/Setters

    /**
     * Name for this preference. The name has to be unique for this node.
     *
     * @return the name of the preference.
     */
    protected String getName() {
        return name;
    }
    //endregion Getters/Setters

    //region Abstract Methods

    /**
     * Gets the current value for the preference.
     *
     * @return the value of the preference.
     */
    protected abstract String getValue();

    /**
     * Sets the initial value for the preference.
     *
     * @param value initial value.
     *
     * @throws Exception if there's an error during initialization
     */
    protected abstract void initialize(final String value) throws Exception;

    /**
     * Cleans the preference resources.
     */
    protected abstract void dispose();
    //endregion Abstract Methods
}
