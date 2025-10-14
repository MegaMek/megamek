/*
 * Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2014-2025 The MegaMek Team. All Rights Reserved.
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

import megamek.client.ui.panels.phaseDisplay.DeploymentDisplay;
import megamek.client.ui.panels.phaseDisplay.FiringDisplay;
import megamek.client.ui.panels.phaseDisplay.PhysicalDisplay;
import megamek.client.ui.panels.phaseDisplay.TargetingPhaseDisplay;
import megamek.client.ui.panels.phaseDisplay.commands.MoveCommand;
import megamek.common.preference.PreferenceManager;
import megamek.common.preference.PreferenceStoreProxy;

public class ButtonOrderPreferences extends PreferenceStoreProxy {
    protected static ButtonOrderPreferences instance = new ButtonOrderPreferences();

    public static ButtonOrderPreferences getInstance() {
        return instance;
    }

    protected ButtonOrderPreferences() {
        store = PreferenceManager.getInstance().getPreferenceStore("ButtonOrderPreferences",
              getClass().getName(), "megamek.client.ui.swing.ButtonOrderPreferences");

        for (MoveCommand cmd : MoveCommand.values()) {
            store.setDefault(cmd.getCmd(), cmd.ordinal());
        }

        for (FiringDisplay.FiringCommand cmd : FiringDisplay.FiringCommand.values()) {
            store.setDefault(cmd.getCmd(), cmd.ordinal());
        }

        for (PhysicalDisplay.PhysicalCommand cmd : PhysicalDisplay.PhysicalCommand.values()) {
            store.setDefault(cmd.getCmd(), cmd.ordinal());
        }

        for (TargetingPhaseDisplay.TargetingCommand cmd : TargetingPhaseDisplay.TargetingCommand.values()) {
            store.setDefault(cmd.getCmd(), cmd.ordinal());
        }

        for (DeploymentDisplay.DeployCommand cmd : DeploymentDisplay.DeployCommand.values()) {
            store.setDefault(cmd.getCmd(), cmd.ordinal());
        }
    }

    @Override
    public String[] getAdvancedProperties() {
        return null;
    }

    public void setButtonPriorities() {
        for (MoveCommand cmd : MoveCommand.values()) {
            int priority = store.getInt(cmd.getCmd());
            cmd.setPriority(priority);
        }

        for (FiringDisplay.FiringCommand cmd : FiringDisplay.FiringCommand.values()) {
            int priority = store.getInt(cmd.getCmd());
            cmd.setPriority(priority);
        }

        for (PhysicalDisplay.PhysicalCommand cmd : PhysicalDisplay.PhysicalCommand.values()) {
            int priority = store.getInt(cmd.getCmd());
            cmd.setPriority(priority);
        }

        for (TargetingPhaseDisplay.TargetingCommand cmd : TargetingPhaseDisplay.TargetingCommand.values()) {
            int priority = store.getInt(cmd.getCmd());
            cmd.setPriority(priority);
        }

        for (DeploymentDisplay.DeployCommand cmd : DeploymentDisplay.DeployCommand.values()) {
            int priority = store.getInt(cmd.getCmd());
            cmd.setPriority(priority);
        }
    }
}
