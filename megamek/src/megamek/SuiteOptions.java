/*
 * Copyright (c) 2021-2022 - The MegaMek Team. All Rights Reserved.
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
package megamek;

import java.util.Locale;
import java.util.prefs.Preferences;

public class SuiteOptions {
    //region Variable Declarations
    protected static final Preferences userPreferences = Preferences.userRoot();
    //endregion Variable Declarations

    //region Constructors
    protected SuiteOptions() {

    }
    //endregion Constructors

    //region Temporary
    /**
     * This is a temporary Locale getter, which sets the stage for suite-wide localization.
     */
    public Locale getLocale() {
        return Locale.US;
    }

    /**
     * This is a temporary Locale getter for dates, which sets the stage for suite-wide localization.
     */
    public Locale getDateLocale() {
        return Locale.US;
    }
    //endregion Temporary
}
