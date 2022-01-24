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
import megamek.client.ui.preferences.PreferencesNode;
import megamek.common.util.EncodeControl;

import javax.swing.*;
import java.awt.*;
import java.util.ResourceBundle;

/**
 * This is the default Panel. It handles preferences, resources, and the frame.
 *
 * Inheriting classes must call initialize() in their constructor and override initialize().
 *
 * This is directly tied to MekHQ's AbstractMHQPanel, and any changes here MUST be verified there.
 */
public abstract class AbstractPanel extends JPanel {
    //region Variable Declarations
    private final JFrame frame;

    protected final ResourceBundle resources;
    //endregion Variable Declarations

    //region Constructors
    /**
     * This creates an AbstractPanel using the default resource bundle.
     */
    protected AbstractPanel(final JFrame frame, final String name) {
        this(frame, name, true);
    }

    /**
     * This creates an AbstractPanel using the default resource bundle and specified double
     * buffered boolean.
     */
    protected AbstractPanel(final JFrame frame, final String name, final boolean isDoubleBuffered) {
        this(frame, ResourceBundle.getBundle("megamek.client.messages",
                MegaMek.getMMOptions().getLocale(), new EncodeControl()),
                name, new FlowLayout(), isDoubleBuffered);
    }

    /**
     * This creates an AbstractPanel using the default resource bundle and specified layout
     * manager.
     */
    protected AbstractPanel(final JFrame frame, final String name, final LayoutManager layoutManager) {
        this(frame, name, layoutManager, true);
    }

    /**
     * This creates an AbstractPanel using the default resource bundle and specified layout
     * manager and double buffered boolean.
     */
    protected AbstractPanel(final JFrame frame, final String name,
                            final LayoutManager layoutManager, final boolean isDoubleBuffered) {
        this(frame, ResourceBundle.getBundle("megamek.client.messages",
                MegaMek.getMMOptions().getLocale(), new EncodeControl()),
                name, layoutManager, isDoubleBuffered);
    }

    /**
     * This creates an AbstractPanel using the specified resource bundle, layout manager, and
     * double buffered boolean. This is not recommended by default.
     */
    protected AbstractPanel(final JFrame frame, final ResourceBundle resources, final String name,
                            final LayoutManager layoutManager, final boolean isDoubleBuffered) {
        super(layoutManager, isDoubleBuffered);
        setName(name);
        this.frame = frame;
        this.resources = resources;
    }
    //endregion Constructors

    //region Getters/Setters
    public JFrame getFrame() {
        return frame;
    }
    //endregion Getters/Setters

    //region Initialization
    /**
     * This initializes the panel, and must be called by inheriting constructors.
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
        setCustomPreferences(preferences);
    }

    /**
     * Adds custom preferences to the child panel.
     *
     * By default, this panel will track no preferences
     * Other preferences can be added by overriding this method.
     * @param preferences the preference node for this panel
     */
    protected void setCustomPreferences(final PreferencesNode preferences) {

    }
    //endregion Initialization
}
