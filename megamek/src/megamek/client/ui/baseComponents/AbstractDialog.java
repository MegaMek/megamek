/*
 * Copyright (c) 2019-2021 - The MegaMek Team. All Rights Reserved.
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
import megamek.client.ui.preferences.JWindowPreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.client.ui.swing.GUIPreferences;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.preference.IPreferenceChangeListener;
import megamek.common.preference.PreferenceChangeEvent;
import megamek.common.util.EncodeControl;
import org.apache.logging.log4j.LogManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ResourceBundle;

/**
 * This is the base class for dialogs in MegaMek. This class handles setting the UI, managing the X
 * button, managing the escape key, and saving the dialog preferences.
 *
 * Inheriting classes must call initialize() in their constructors and override createCenterPane()
 *
 * This is directly tied to MekHQ's AbstractMHQDialog, and any changes here MUST be verified there.
 */
public abstract class AbstractDialog extends JDialog implements IPreferenceChangeListener, WindowListener {
    //region Variable Declarations
    private JFrame frame;

    protected static final String CLOSE_ACTION = "closeAction";

    protected ResourceBundle resources;
    //endregion Variable Declarations

    //region Constructors
    /**
     * This creates a non-modal AbstractDialog using the default resource bundle. This is the
     * normal constructor to use for an AbstractDialog.
     */
    protected AbstractDialog(final JFrame frame, final String name, final String title) {
        this(frame, false, name, title);
    }

    /**
     * This creates an AbstractDialog using the default resource bundle. It allows one to create
     * modal dialogs.
     */
    protected AbstractDialog(final JFrame frame, final boolean modal, final String name, final String title) {
        this(frame, modal, ResourceBundle.getBundle("megamek.client.messages", 
                MegaMek.getMMOptions().getLocale(), new EncodeControl()), name, title);
    }

    /**
     * This creates an AbstractDialog using the specified resource bundle. This is not recommended
     * by default.
     */
    protected AbstractDialog(final JFrame frame, final boolean modal, final ResourceBundle resources,
                             final String name, final String title) {
        super(frame, modal);
        setTitle(resources.getString(title));
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
     * Initializes the dialog's UI and preferences. Needs to be called by child classes for initial
     * setup.
     *
     * Anything that overrides this method MUST end by calling {@link AbstractDialog#finalizeInitialization()}
     */
    protected void initialize() {
        setLayout(new BorderLayout());
        add(createCenterPane(), BorderLayout.CENTER);
        try {
            finalizeInitialization();
        } catch (Exception ex) {
            LogManager.getLogger().error("Error finalizing the dialog. Returning the created dialog, but this is likely to cause some oddities.", ex);
        }
    }

    /**
     * This is used to create the dialog's center pane
     * @return the center pane of the dialog
     */
    protected abstract Container createCenterPane();

    /**
     * This MUST be called at the end of initialization to finalize it. This is the key method for
     * this being the abstract basis for all other dialogs.
     * @throws Exception if there's an issue finishing initialization. Normally this means there's
     * an issue setting the preferences, which normally means that a component has had its name
     * value set.
     */
    protected void finalizeInitialization() throws Exception {
        // Pack and fit only affect dialogs when shown for the absolute first time; at any later time,
        // the setPreferences() below overwrites size and position with stored values
        pack();
        fitAndCenter();

        // Escape keypress
        final KeyStroke escape = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(escape, CLOSE_ACTION);
        getRootPane().getInputMap(JComponent.WHEN_FOCUSED).put(escape, CLOSE_ACTION);
        getRootPane().getActionMap().put(CLOSE_ACTION, new AbstractAction() {
            private static final long serialVersionUID = 95171770700983453L;

            @Override
            public void actionPerformed(ActionEvent evt) {
                cancelActionPerformed(evt);
            }
        });

        adaptToGUIScale();
        GUIPreferences.getInstance().addPreferenceChangeListener(this);
        addWindowListener(this);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setPreferences();
    }

    /**
     * Re-sizes the dialog to a maximum width and height of 80% of the screen size when necessary.
     * It then centers the dialog on the screen.
     */
    protected void fitAndCenter() {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int maxWidth = (int) (screenSize.width * 0.8);
        int maxHeight = (int) (screenSize.height * 0.8);
        setSize(new Dimension(Math.min(maxWidth, getWidth()), Math.min(maxHeight, getHeight())));
        setLocation((screenSize.width - getSize().width) / 2, (screenSize.height - getSize().height) / 2);
    }

    /**
     * This is used to set preferences based on the preference node for this class. It is overridden
     * for MekHQ usage.
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
        preferences.manage(new JWindowPreference(this));
        setCustomPreferences(preferences);
    }

    /**
     * Adds custom preferences to the child dialog.
     *
     * By default, this dialog will track preferences related to the size
     * and position of the dialog. Other preferences can be added by overriding
     * this method.
     * @param preferences the preference node for this dialog
     * @throws Exception if there's an issue initializing the preferences. Normally this means
     * a component has <strong>not</strong> had its name value set.
     */
    protected void setCustomPreferences(final PreferencesNode preferences) throws Exception {

    }
    //endregion Initialization

    /**
     * Note: Cancelling a dialog should always allow one to close the dialog.
     */
    protected void cancelActionPerformed(final ActionEvent evt) {
        try {
            cancelAction();
        } catch (Exception ex) {
            LogManager.getLogger().error("", ex);
        } finally {
            setVisible(false);
        }
    }

    /**
     * Action performed when the Cancel button is clicked, the dialog is closed by the X button, or
     * the escape key is pressed
     */
    protected void cancelAction() {

    }

    //region WindowEvents
    /**
     * Note: Closing the dialog should always allow one to close the dialog.
     */
    @Override
    public void windowClosing(final WindowEvent evt) {
        try {
            cancelAction();
        } catch (Exception ex) {
            LogManager.getLogger().error("", ex);
        }
    }

    @Override
    public void windowOpened(final WindowEvent evt) {

    }

    @Override
    public void windowClosed(final WindowEvent evt) {

    }

    @Override
    public void windowIconified(final WindowEvent evt) {

    }

    @Override
    public void windowDeiconified(final WindowEvent evt) {

    }

    @Override
    public void windowActivated(final WindowEvent evt) {

    }

    @Override
    public void windowDeactivated(final WindowEvent evt) {

    }
    //endregion WindowEvents

    public void adaptToGUIScale() {
        UIUtil.adjustDialog(this,  UIUtil.FONT_SCALE1);
    }

    @Override
    public void preferenceChange(PreferenceChangeEvent e) {
        // Update the text size when the GUI scaling changes
        if (e.getName().equals(GUIPreferences.GUI_SCALE)) {
            adaptToGUIScale();
        }
    }
}
