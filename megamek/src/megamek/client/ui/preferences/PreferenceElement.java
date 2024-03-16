/*
 * Copyright (c) 2019-2021 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.client.ui.preferences;

import megamek.codeUtilities.StringUtility;

/**
 * Represents the set of information needed to be part of the user preferences system for MegaMek.
 *
 * A preference is a value that the user can set anywhere in MegaMek and that will be persisted
 * between user sessions.
 *
 * PreferenceElements are contained inside {@link PreferencesNode}s, forming a tree. You can
 * imagine a PreferenceElement as a field, and a PreferencesNode as a class.
 *
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
     * Name for this preference.
     * The name has to be unique for this node.
     * @return the name of the preference.
     */
    protected String getName() {
        return name;
    }
    //endregion Getters/Setters

    //region Abstract Methods
    /**
     * Gets the current value for the preference.
     * @return the value of the preference.
     */
    protected abstract String getValue();

    /**
     * Sets the initial value for the preference.
     * @param value initial value.
     * @throws Exception if there's an error during initialization
     */
    protected abstract void initialize(final String value) throws Exception;

    /**
     * Cleans the preference resources.
     */
    protected abstract void dispose();
    //endregion Abstract Methods
}
