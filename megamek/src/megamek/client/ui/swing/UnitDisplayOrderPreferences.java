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

        store.setDefault("NonTabbedA1", "General");
        store.setDefault("NonTabbedA2", "Pilot");
        store.setDefault("NonTabbedB1", "Weapon");
        store.setDefault("NonTabbedB2", "System");
        store.setDefault("NonTabbedC1", "Extra");
        store.setDefault("NonTabbedC2", "Armor");
    }

    @Override
    public String[] getAdvancedProperties() {
        return null;
    }

}
