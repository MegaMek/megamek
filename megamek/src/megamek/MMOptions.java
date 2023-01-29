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

import megamek.common.preference.PreferenceManager;

import java.util.Locale;

public class MMOptions extends SuiteOptions {
    //region Constructors
    public MMOptions() {
        super();
    }
    //endregion Constructors

    //region Nag Tab
    public boolean getNagDialogIgnore(final String key) {
        return userPreferences.node(MMConstants.NAG_NODE).getBoolean(key, false);
    }

    public void setNagDialogIgnore(final String key, final boolean value) {
        userPreferences.node(MMConstants.NAG_NODE).putBoolean(key, value);
    }
    //endregion Nag Tab

    //region Temporary
    /**
     * This is a temporary Locale getter, which sets the stage for suite-wide localization.
     */
    @Override
    public Locale getLocale() {
        return PreferenceManager.getClientPreferences().getLocale();
    }

    /**
     * This is a temporary Locale getter for dates, which sets the stage for suite-wide localization.
     */
    @Override
    public Locale getDateLocale() {
        return PreferenceManager.getClientPreferences().getLocale();
    }
    //endregion Temporary
}
