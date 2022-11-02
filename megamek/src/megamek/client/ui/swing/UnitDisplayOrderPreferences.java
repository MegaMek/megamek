/*
 * MegaMek - Copyright (C)
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

package megamek.client.ui.swing;

import megamek.client.ui.swing.unitDisplay.UnitDisplay;
import megamek.common.preference.PreferenceManager;
import megamek.common.preference.PreferenceStoreProxy;

public class UnitDisplayOrderPreferences extends PreferenceStoreProxy {


    protected static UnitDisplayOrderPreferences instance =
            new UnitDisplayOrderPreferences();

    public static UnitDisplayOrderPreferences getInstance() {
        return instance;
    }

    protected UnitDisplayOrderPreferences() {

        store = PreferenceManager.getInstance().getPreferenceStore(
                getClass().getName());

        store.setDefault(UnitDisplay.NON_TABBED_A1, UnitDisplay.NON_TABBED_GENERAL);
        store.setDefault(UnitDisplay.NON_TABBED_A2, UnitDisplay.NON_TABBED_PILOT);
        store.setDefault(UnitDisplay.NON_TABBED_B1, UnitDisplay.NON_TABBED_WEAPON);
        store.setDefault(UnitDisplay.NON_TABBED_B2, UnitDisplay.NON_TABBED_SYSTEM);
        store.setDefault(UnitDisplay.NON_TABBED_C1, UnitDisplay.NON_TABBED_EXTRA);
        store.setDefault(UnitDisplay.NON_TABBED_C2, UnitDisplay.NON_TABBED_ARMOR);
    }

    @Override
    public String[] getAdvancedProperties() {
        return null;
    }

}
