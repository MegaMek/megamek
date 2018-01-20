/*
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
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

public class ButtonOrderPreferences extends PreferenceStoreProxy {


    protected static ButtonOrderPreferences instance =
            new ButtonOrderPreferences();

    public static ButtonOrderPreferences getInstance() {
        return instance;
    }

    protected ButtonOrderPreferences() {

        store = PreferenceManager.getInstance().getPreferenceStore(
                getClass().getName());

        for (MovementDisplay.MoveCommand cmd :
                MovementDisplay.MoveCommand.values()) {
            store.setDefault(cmd.getCmd(), cmd.ordinal());
        }

        for (FiringDisplay.FiringCommand cmd :
                FiringDisplay.FiringCommand.values()) {
            store.setDefault(cmd.getCmd(), cmd.ordinal());
        }

        for (PhysicalDisplay.PhysicalCommand cmd :
                PhysicalDisplay.PhysicalCommand.values()) {
            store.setDefault(cmd.getCmd(), cmd.ordinal());
        }

        for (TargetingPhaseDisplay.TargetingCommand cmd :
                TargetingPhaseDisplay.TargetingCommand.values()) {
            store.setDefault(cmd.getCmd(), cmd.ordinal());
        }

        for (DeploymentDisplay.DeployCommand cmd :
                DeploymentDisplay.DeployCommand.values()) {
            store.setDefault(cmd.getCmd(), cmd.ordinal());
        }

    }

    @Override
    public String[] getAdvancedProperties() {
        return null;
    }

    public void setButtonPriorities() {
        for (MovementDisplay.MoveCommand cmd :
                MovementDisplay.MoveCommand.values()) {
            Integer priority = store.getInt(cmd.getCmd());
            if (priority != null) {
                cmd.setPriority(priority);
            }
        }

        for (FiringDisplay.FiringCommand cmd :
                FiringDisplay.FiringCommand.values()) {
            Integer priority = store.getInt(cmd.getCmd());
            if (priority != null) {
                cmd.setPriority(priority);
            }
        }

        for (PhysicalDisplay.PhysicalCommand cmd :
                PhysicalDisplay.PhysicalCommand.values()) {
            Integer priority = store.getInt(cmd.getCmd());
            if (priority != null) {
                cmd.setPriority(priority);
            }
        }

        for (TargetingPhaseDisplay.TargetingCommand cmd :
                TargetingPhaseDisplay.TargetingCommand.values()) {
            Integer priority = store.getInt(cmd.getCmd());
            if (priority != null) {
                cmd.setPriority(priority);
            }
        }

        for (DeploymentDisplay.DeployCommand cmd :
                DeploymentDisplay.DeployCommand.values()) {
            Integer priority = store.getInt(cmd.getCmd());
            if (priority != null) {
                cmd.setPriority(priority);
            }
        }
    }


}
