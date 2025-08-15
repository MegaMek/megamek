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
package megamek.client.ui.dialogs.buttonDialogs;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.util.ResourceBundle;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;

import megamek.MegaMek;
import megamek.client.ui.buttons.MMButton;
import megamek.client.ui.dialogs.abstractDialogs.AbstractDialog;
import megamek.client.ui.enums.DialogResult;
import megamek.logging.MMLogger;

/**
 * This is the Base Dialog for a dialog with buttons in MegaMek. It extends Base Dialog, and adds a button panel with
 * base Ok and Cancel buttons. It also includes an enum tracker for the result of the dialog.
 * <p>
 * Inheriting classes must call initialize() in their constructors and override createCenterPane()
 * <p>
 * The resources associated with this dialog need to contain at least the following keys: - "Ok.text" - text for the ok
 * button - "Ok.toolTipText" - toolTipText for the ok button - "Cancel.text" - text for the cancel button -
 * "Cancel.toolTipText" - toolTipText for the cancel button
 * <p>
 * This is directly tied to MekHQ's AbstractMHQButtonDialog, and any changes here MUST be verified there.
 */
public abstract class AbstractButtonDialog extends AbstractDialog {
    private static final MMLogger logger = MMLogger.create(AbstractButtonDialog.class);

    // region Variable Declarations
    private DialogResult result;
    // endregion Variable Declarations

    // region Constructors

    /**
     * This creates a modal AbstractButtonDialog using the default resource bundle. This is the normal constructor to
     * use for an AbstractButtonDialog.
     *
     * @param frame Frame to connect to.
     * @param name  Name for the button.
     * @param title Title of the dialog.
     */
    protected AbstractButtonDialog(final JFrame frame, final String name, final String title) {
        this(frame, true, name, title);
    }

    /**
     * This creates an AbstractButtonDialog using the default resource bundle. It allows one to create non-modal button
     * dialogs.
     *
     * @param frame Window frame to connect to.
     * @param modal Whether to open modally
     * @param name  Name on the button.
     * @param title Title of window
     */
    protected AbstractButtonDialog(final JFrame frame, final boolean modal, final String name,
          final String title) {
        this(frame, modal, ResourceBundle.getBundle("megamek.client.messages",
              MegaMek.getMMOptions().getLocale()), name, title);
    }

    /**
     * This creates an AbstractButtonDialog using the specified resource bundle. This is not recommended by default.
     */
    protected AbstractButtonDialog(final JFrame frame, final boolean modal, final ResourceBundle resources,
          final String name, final String title) {
        super(frame, modal, resources, name, title);
        setResult(DialogResult.CANCELLED); // Default result is cancelled
    }

    /**
     * This constructor is provided for uses cases where this dialog needs another dialog as a parent.
     */
    protected AbstractButtonDialog(final JDialog dialog, final JFrame frame, final boolean modal,
          final ResourceBundle resources,
          final String name, final String title) {
        super(dialog, frame, modal, resources, name, title);
        setResult(DialogResult.CANCELLED); // Default result is cancelled
    }
    // endregion Constructors

    // region Getters/Setters
    public DialogResult getResult() {
        return result;
    }

    public void setResult(final DialogResult result) {
        this.result = result;
    }
    // endregion Getters/Setters

    // region Initialization

    /**
     * Initializes the dialog's UI and preferences. Needs to be called by child classes for initial setup.
     * <p>
     * Anything that overrides this method MUST end by calling {@link AbstractDialog#finalizeInitialization()}
     */
    @Override
    protected void initialize() {
        setLayout(new BorderLayout());
        add(createCenterPane(), BorderLayout.CENTER);
        add(createButtonPanel(), BorderLayout.PAGE_END);
        try {
            finalizeInitialization();
        } catch (Exception ex) {
            logger.error(ex,
                  "Error finalizing the dialog. Returning the created dialog, but this is likely to cause some oddities.");
        }
    }

    /**
     * @return the created Button Panel
     */
    protected JPanel createButtonPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 2));
        panel.add(new MMButton("okButton", resources.getString("Ok.text"),
              resources.getString("Ok.toolTipText"), this::okButtonActionPerformed));
        panel.add(new MMButton("cancelButton", resources.getString("Cancel.text"),
              resources.getString("Cancel.toolTipText"), this::cancelActionPerformed));
        return panel;
    }
    // endregion Initialization

    // region Button Actions

    /**
     * This is the default Action Event Listener for the Ok Button's action. This triggers the Ok Action, sets the
     * result to confirmed, and then sets the dialog so that it is no longer visible.
     *
     * @param evt the event triggering this
     */
    protected void okButtonActionPerformed(final ActionEvent evt) {
        okAction();
        setResult(DialogResult.CONFIRMED);
        setVisible(false);
    }

    /**
     * Action performed when the Ok button is clicked.
     */
    protected void okAction() {

    }
    // endregion Button Actions

    /**
     * Sets the dialog to be visible, before returning the result
     *
     * @return the result of showing the dialog
     */
    public DialogResult showDialog() {
        setVisible(true);
        return getResult();
    }
}
