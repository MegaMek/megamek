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
import megamek.client.ui.enums.DialogResult;
import megamek.common.util.EncodeControl;
import org.apache.logging.log4j.LogManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ResourceBundle;

/**
 * This is the Base Dialog for a dialog with buttons in MegaMek. It extends Base Dialog, and adds a
 * button panel with base Ok and Cancel buttons. It also includes an enum tracker for the result of
 * the dialog.
 *
 * Inheriting classes must call initialize() in their constructors and override createCenterPane()
 *
 * The resources associated with this dialog need to contain at least the following keys:
 * - "Ok.text" - text for the ok button
 * - "Ok.toolTipText" - toolTipText for the ok button
 * - "Cancel.text" - text for the cancel button
 * - "Cancel.toolTipText" - toolTipText for the cancel button
 *
 * This is directly tied to MekHQ's AbstractMHQButtonDialog, and any changes here MUST be verified
 * there.
 */
public abstract class AbstractButtonDialog extends AbstractDialog {
    //region Variable Declarations
    private DialogResult result;
    //endregion Variable Declarations

    //region Constructors
    /**
     * This creates a modal AbstractButtonDialog using the default resource bundle. This is
     * the normal constructor to use for an AbstractButtonDialog.
     */
    protected AbstractButtonDialog(final JFrame frame, final String name, final String title) {
        this(frame, true, name, title);
    }

    /**
     * This creates an AbstractButtonDialog using the default resource bundle. It allows one
     * to create non-modal button dialogs.
     */
    protected AbstractButtonDialog(final JFrame frame, final boolean modal, final String name,
                                   final String title) {
        this(frame, modal, ResourceBundle.getBundle("megamek.client.messages", 
                MegaMek.getMMOptions().getLocale(), new EncodeControl()), name, title);
    }

    /**
     * This creates an AbstractButtonDialog using the specified resource bundle. This is not
     * recommended by default.
     */
    protected AbstractButtonDialog(final JFrame frame, final boolean modal, final ResourceBundle resources,
                                   final String name, final String title) {
        super(frame, modal, resources, name, title);
        setResult(DialogResult.CANCELLED); // Default result is cancelled
    }
    //endregion Constructors

    //region Getters/Setters
    public DialogResult getResult() {
        return result;
    }

    public void setResult(final DialogResult result) {
        this.result = result;
    }
    //endregion Getters/Setters

    //region Initialization
    /**
     * Initializes the dialog's UI and preferences. Needs to be called by child classes for initial
     * setup.
     *
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
            LogManager.getLogger().error("Error finalizing the dialog. Returning the created dialog, but this is likely to cause some oddities.", ex);
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
    //endregion Initialization

    //region Button Actions
    /**
     * This is the default Action Event Listener for the Ok Button's action. This triggers the Ok
     * Action, sets the result to confirmed, and then sets the dialog so that it is no longer visible.
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
    //endregion Button Actions

    /**
     * Sets the dialog to be visible, before returning the result
     * @return the result of showing the dialog
     */
    public DialogResult showDialog() {
        setVisible(true);
        return getResult();
    }
}
