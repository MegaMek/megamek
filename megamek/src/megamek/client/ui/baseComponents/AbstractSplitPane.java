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
import megamek.client.ui.preferences.JSplitPanePreference;
import megamek.client.ui.preferences.PreferencesNode;
import org.apache.logging.log4j.LogManager;

import javax.swing.*;
import java.awt.*;
import java.util.ResourceBundle;

/**
 * This is the default SplitPane. It handles preferences, resources, the frame, and setup.
 *
 * Inheriting classes must call initialize() in their constructor and override createLeftComponent()
 * and createRightComponent()
 *
 * This is directly tied to MekHQ's AbstractMHQSplitPane, and any changes here MUST be verified there.
 */
public abstract class AbstractSplitPane extends JSplitPane {
    //region Variable Declarations
    private JFrame frame;

    protected final ResourceBundle resources;
    //endregion Variable Declarations

    //region Constructors
    /**
     * This creates an AbstractSplitPane using the default resource bundle. This is the normal
     * constructor to use for an AbstractSplitPane.
     */
    protected AbstractSplitPane(final JFrame frame, final String name) {
        this(frame, ResourceBundle.getBundle("megamek.client.messages", 
                MegaMek.getMMOptions().getLocale()), name);
    }

    /**
     * This creates an AbstractSplitPane using the specified resource bundle. This is not recommended
     * by default.
     */
    protected AbstractSplitPane(final JFrame frame, final ResourceBundle resources, final String name) {
        super(JSplitPane.HORIZONTAL_SPLIT);
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
     * This initializes the Split Pane. It must be called by inheriting classes during their constructors
     */
    protected void initialize() {
        setLeftComponent(createLeftComponent());
        setRightComponent(createRightComponent());
        try {
            finalizeInitialization();
        } catch (Exception ex) {
            LogManager.getLogger().error("Error finalizing the split pane. Returning the created dialog, but this is likely to cause some oddities.", ex);
        }
    }

    /**
     * @return the created left component
     */
    protected abstract Component createLeftComponent();

    /**
     * @return the created right component
     */
    protected abstract Component createRightComponent();

    /**
     * This MUST be called at the end of initialization to finalize it. This is the key method for
     * this being the abstract basis for all other split panes
     * @throws Exception if there's an issue finishing initialization. Normally this means there's
     * an issue setting the preferences, which normally means that a component has had its name
     * value set.
     */
    protected void finalizeInitialization() throws Exception {
        setOneTouchExpandable(true);
        setPreferences();
    }

    /**
     * This is used to set preferences based on the preference node for this class. It is overridden
     * for MekHQ usage
     * @throws Exception if there's an issue initializing the preferences. Normally this means
     * a component has <strong>not</strong> had its name value set.
     */
    protected void setPreferences() throws Exception {
        setPreferences(MegaMek.getMMPreferences().forClass(getClass()));
    }

    /**
     * This sets the base preferences for this class, and calls the custom preferences method
     * @throws Exception if there's an issue initializing the preferences. Normally this means
     * a component has <strong>not</strong> had its name value set.
     */
    protected void setPreferences(final PreferencesNode preferences) throws Exception {
        try {
            preferences.manage(new JSplitPanePreference(this));
            setCustomPreferences(preferences);
        } catch (Exception ex) {
            LogManager.getLogger().error("Failed to set preferences", ex);
        }
    }

    /**
     * Adds custom preferences to the child pane.
     *
     * By default, this pane will track preferences related to the location of the split
     * Other preferences can be added by overriding this method.
     * @param preferences the preference node for this pane
     * @throws Exception if there's an issue initializing the preferences. Normally this means
     * a component has <strong>not</strong> had its name value set.
     */
    protected void setCustomPreferences(final PreferencesNode preferences) throws Exception {

    }
    //endregion Initialization
}
