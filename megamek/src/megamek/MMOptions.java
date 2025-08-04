/*
 * Copyright (C) 2021-2025 The MegaMek Team. All Rights Reserved.
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

package megamek;

import java.util.Locale;

import megamek.common.preference.PreferenceManager;

public class MMOptions extends SuiteOptions {
    // region Constructors
    public MMOptions() {
        super();
    }
    // endregion Constructors

    // region Nag Tab
    public boolean getNagDialogIgnore(final String key) {
        return userPreferences.node(MMConstants.NAG_NODE).getBoolean(key, false);
    }

    public void setNagDialogIgnore(final String key, final boolean value) {
        userPreferences.node(MMConstants.NAG_NODE).putBoolean(key, value);
    }
    // endregion Nag Tab

    // region Temporary

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
    // endregion Temporary
}
