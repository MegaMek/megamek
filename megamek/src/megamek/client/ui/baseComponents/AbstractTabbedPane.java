/*
 * Copyright (c) 2021 - The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.baseComponents;

import megamek.MegaMek;
import megamek.common.util.EncodeControl;
import megamek.client.ui.preferences.JTabbedPanePreference;
import megamek.client.ui.preferences.PreferencesNode;

import javax.swing.*;
import java.util.ResourceBundle;

/**
 * This is the default TabbedPane. It handles preferences, resources, and the frame.
 *
 * Inheriting classes must call initialize() in their constructors and override initialize()
 *
 * This is directly tied to MekHQ's AbstractMHQTabbedPane, and any changes here MUST be verified there.
 */
public abstract class AbstractTabbedPane extends JTabbedPane {
    //region Variable Declarations
    private JFrame frame;

    protected final ResourceBundle resources;
    //endregion Variable Declarations

    //region Constructors
    /**
     * This creates an AbstractTabbedPane using the default resource bundle. This is the normal
     * constructor to use for an AbstractTabbedPane.
     */
    protected AbstractTabbedPane(final JFrame frame, final String name) {
        this(frame, ResourceBundle.getBundle("megamek.client.messages", 
                MegaMek.getMMOptions().getLocale(), new EncodeControl()), name);
    }

    /**
     * This creates an AbstractTabbedPane using the specified resource bundle. This is not recommended
     * by default.
     */
    protected AbstractTabbedPane(final JFrame frame, final ResourceBundle resources, final String name) {
        super();
        setName(name);
        setFrame(frame);
        this.resources = resources;
    }
    //endregion Constructors

    //region Getters/Setters
    public JFrame getFrame() {
        return frame;
    }

    public void setFrame(final JFrame frame) {
        this.frame = frame;
    }
    //endregion Getters/Setters

    //region Initialization
    /**
     * This initializes the tabbed pane, and must be called by inheriting constructors.
     * This MUST end with a call to setPreferences() for this class to work properly.
     */
    protected abstract void initialize();

    /**
     * This is used to set preferences based on the preference node for this class. It is overridden
     * for MekHQ usage
     */
    protected void setPreferences() {
        setPreferences(MegaMek.getMMPreferences().forClass(getClass()));
    }

    /**
     * This sets the base preferences for this class, and calls the custom preferences method
     */
    protected void setPreferences(final PreferencesNode preferences) {
        preferences.manage(new JTabbedPanePreference(this));
        setCustomPreferences(preferences);
    }

    /**
     * Adds custom preferences to the child pane.
     *
     * By default, this pane will track preferences related to the previously selected tab.
     * Other preferences can be added by overriding this method.
     * @param preferences the preference node for this pane
     */
    protected void setCustomPreferences(final PreferencesNode preferences) {

    }
    //endregion Initialization
}
