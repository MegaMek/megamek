/*
 * Copyright (C) 2022-2025 The MegaMek Team. All Rights Reserved.
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
