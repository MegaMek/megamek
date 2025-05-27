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

package megamek.client.ui.clientGUI;

import megamek.client.ui.dialogs.unitDisplay.UnitDisplayPanel;
import megamek.common.preference.PreferenceManager;
import megamek.common.preference.PreferenceStoreProxy;

public class UnitDisplayOrderPreferences extends PreferenceStoreProxy {


    protected static UnitDisplayOrderPreferences instance =
            new UnitDisplayOrderPreferences();

    public static UnitDisplayOrderPreferences getInstance() {
        return instance;
    }

    protected UnitDisplayOrderPreferences() {

        store = PreferenceManager.getInstance().getPreferenceStore("UnitDisplayOrderPreferences",
                getClass().getName(), "megamek.client.ui.swing.UnitDisplayOrderPreferences");

        store.setDefault(UnitDisplayPanel.NON_TABBED_A1, UnitDisplayPanel.NON_TABBED_GENERAL);
        store.setDefault(UnitDisplayPanel.NON_TABBED_A2, UnitDisplayPanel.NON_TABBED_PILOT);
        store.setDefault(UnitDisplayPanel.NON_TABBED_B1, UnitDisplayPanel.NON_TABBED_WEAPON);
        store.setDefault(UnitDisplayPanel.NON_TABBED_B2, UnitDisplayPanel.NON_TABBED_SYSTEM);
        store.setDefault(UnitDisplayPanel.NON_TABBED_C1, UnitDisplayPanel.NON_TABBED_EXTRA);
        store.setDefault(UnitDisplayPanel.NON_TABBED_C2, UnitDisplayPanel.NON_TABBED_ARMOR);
    }

    @Override
    public String[] getAdvancedProperties() {
        return null;
    }

}
