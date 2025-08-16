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
package megamek.client.ui.panes;

import java.util.ResourceBundle;
import javax.swing.JFrame;
import javax.swing.JScrollPane;

import megamek.MegaMek;
import megamek.client.ui.preferences.PreferencesNode;

/**
 * This is the default ScrollPane. It handles preferences, resources, and the frame.
 * <p>
 * Inheriting classes must call initialize() in their constructor and override initialize().
 * <p>
 * This is directly tied to MekHQ's AbstractMHQScrollPane, and any changes here MUST be verified there.
 */
public abstract class AbstractScrollPane extends JScrollPane {
    //region Variable Declarations
    private JFrame frame;

    protected final ResourceBundle resources;
    //endregion Variable Declarations

    //region Constructors

    /**
     * This creates an AbstractScrollPane using the default resource bundle and using the default scrollbar policies of
     * vertical and horizontal scrollbars as required.
     */
    protected AbstractScrollPane(final JFrame frame, final String name) {
        this(frame, name, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    }

    /**
     * This creates an AbstractScrollPane using the default resource bundle and using the specified scrollbar policies.
     */
    protected AbstractScrollPane(final JFrame frame, final String name,
          final int verticalScrollBarPolicy, final int horizontalScrollBarPolicy) {
        this(frame, ResourceBundle.getBundle("megamek.client.messages",
                    MegaMek.getMMOptions().getLocale()), name,
              verticalScrollBarPolicy, horizontalScrollBarPolicy);
    }

    /**
     * This creates an AbstractScrollPane using the specified resource bundle and using the default of vertical and
     * horizontal scrollbars as required. This is not recommended by default.
     */
    protected AbstractScrollPane(final JFrame frame, final ResourceBundle resources, final String name,
          final int verticalScrollBarPolicy, final int horizontalScrollBarPolicy) {
        super(verticalScrollBarPolicy, horizontalScrollBarPolicy);
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
     * This initializes the scroll pane, and must be called by inheriting constructors. This MUST end with a call to
     * setPreferences() for this class to work properly.
     */
    protected abstract void initialize();

    /**
     * This is used to set preferences based on the preference node for this class. It is overridden for MekHQ usage.
     *
     * @throws Exception if there's an issue initializing the preferences. Normally this means a component has
     *                   <strong>not</strong> had its name value set.
     */
    protected void setPreferences() throws Exception {
        setPreferences(MegaMek.getMMPreferences().forClass(getClass()));
    }

    /**
     * This sets the base preferences for this class, and calls the custom preferences method
     *
     * @throws Exception if there's an issue initializing the preferences. Normally this means a component has
     *                   <strong>not</strong> had its name value set.
     */
    protected void setPreferences(final PreferencesNode preferences) throws Exception {
        setCustomPreferences(preferences);
    }

    /**
     * Adds custom preferences to the child pane.
     * <p>
     * By default, this pane will track no preferences Other preferences can be added by overriding this method.
     *
     * @param preferences the preference node for this pane
     *
     * @throws Exception if there's an issue initializing the preferences. Normally this means a component has
     *                   <strong>not</strong> had its name value set.
     */
    protected void setCustomPreferences(final PreferencesNode preferences) throws Exception {

    }
    //endregion Initialization
}
