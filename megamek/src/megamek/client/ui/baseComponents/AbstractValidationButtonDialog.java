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
import megamek.client.ui.enums.ValidationState;
import megamek.common.annotations.Nullable;
import org.apache.logging.log4j.LogManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ResourceBundle;

/**
 * This is the Base Dialog for a dialog with buttons where the Ok button requires data validation
 * in MegaMek. It extends Base Button Dialog, and adds a Validate button to the button panel. It
 * also includes an enum tracker for the result of the validation.
 *
 * Inheriting classes must call initialize() in their constructors and override createCenterPane()
 * and validateAction();
 *
 * The resources associated with this dialog need to contain at least the following keys:
 * - "Ok.text" - text for the Ok button
 * - "Ok.toolTipText" - toolTipText for the Ok button
 * - "Validate.text" - text for the Validate button
 * - "Validate.toolTipText" - toolTipText for the Validate button
 * - "Cancel.text" - text for the Cancel button
 * - "Cancel.toolTipText" - toolTipText for the Cancel button
 *
 * This is directly tied to MekHQ's AbstractMHQValidationButtonDialog, and any changes here MUST be
 * verified there.
 */
public abstract class AbstractValidationButtonDialog extends AbstractButtonDialog {
    //region Variable Declarations
    private ValidationState state;
    private JButton okButton;
    //endregion Variable Declarations

    //region Constructors
    /**
     * This creates a modal AbstractValidationButtonDialog using the default resource bundle. This
     * is the normal constructor to use for an AbstractValidationButtonDialog.
     */
    protected AbstractValidationButtonDialog(final JFrame frame, final String name, final String title) {
        this(frame, true, name, title);
    }

    /**
     * This creates an AbstractValidationButtonDialog using the default resource bundle. It allows
     * one to create non-modal button dialogs.
     */
    protected AbstractValidationButtonDialog(final JFrame frame, final boolean modal,
                                             final String name, final String title) {
        this(frame, modal, ResourceBundle.getBundle("megamek.client.messages", 
                MegaMek.getMMOptions().getLocale()), name, title);
    }

    /**
     * This creates an AbstractValidationButtonDialog using the specified resource bundle. This is
     * not recommended by default.
     */
    protected AbstractValidationButtonDialog(final JFrame frame, final boolean modal,
                                             final ResourceBundle resources, final String name,
                                             final String title) {
        super(frame, modal, resources, name, title);
        setState(ValidationState.PENDING);
    }

    /**
     * Allows a dialog to be passed in as the owner
     */
    protected AbstractValidationButtonDialog(final JDialog owner, final JFrame frame, final boolean modal,
                                             final ResourceBundle resources, final String name,
                                             final String title) {
        super(owner, frame, modal, resources, name, title);
        setState(ValidationState.PENDING);
    }
    //endregion Constructors

    //region Getters/Setters
    public ValidationState getState() {
        return state;
    }

    public void setState(final ValidationState state) {
        this.state = state;
    }

    public JButton getOkButton() {
        return okButton;
    }

    public void setOkButton(final JButton okButton) {
        this.okButton = okButton;
    }
    //endregion Getters/Setters

    //region Initialization
    /**
     * @return the created Button Panel
     */
    @Override
    protected JPanel createButtonPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 2));
        setOkButton(new MMButton("okButton", resources.getString("Ok.text"),
                resources.getString("Ok.toolTipText"), this::okButtonActionPerformed));
        panel.add(getOkButton());
        panel.add(new MMButton("validateButton", resources.getString("Validate.text"),
                resources.getString("Validate.toolTipText"), this::validateButtonActionPerformed));
        panel.add(new MMButton("cancelButton", resources.getString("Cancel.text"),
                resources.getString("Cancel.toolTipText"), this::cancelActionPerformed));
        return panel;
    }
    //endregion Initialization

    //region Button Actions
    /**
     * This is the default Action Event Listener for the Ok Button's action. It performs data
     * validation, then only triggers the AbstractButtonDialog's Ok Button's action if the data
     * was successfully validated
     * @param evt the event triggering this
     */
    @Override
    protected void okButtonActionPerformed(final ActionEvent evt) {
        validateButtonActionPerformed(evt);

        if (getState().isSuccess()) {
            super.okButtonActionPerformed(evt);
        } else if (getState().isPending()) {
            LogManager.getLogger().error("Received a Pending validation state after performing validation, returning without closing the dialog.");
        }
    }

    /**
     * This runs revalidation on the dialog, which sets the validation state to pending before calling
     * validateButtonActionPerformed to perform the data validation.
     *
     * @param evt the event triggering this, or null if you want to put the output into a popup, if
     *            applicable
     */
    protected void revalidateAction(final @Nullable ActionEvent evt) {
        setState(ValidationState.PENDING);
        validateButtonActionPerformed(evt);
    }

    /**
     * This performs data validation on the dialog if the current validation state is pending or
     * warning, or if this is manually triggered.
     * @param evt the event triggering this, or null if you want to put the output into a popup, if
     *            applicable
     */
    protected void validateButtonActionPerformed(final @Nullable ActionEvent evt) {
        if (getState().isPending() || getState().isWarning() || (evt != null)) {
            setState(validateAction(evt != null));
        }
    }

    /**
     * This validates the data contained within this dialog, returning the determined state (with
     * SUCCESS being returned if all warnings are skipped). The Ok Button can be accessed through
     * a getter, so the tooltip can be changed and it can be enabled/disabled.
     *
     * @param display put any outputs into a popup, if applicable
     * @return the determined validation state of the dialog.
     */
    protected abstract ValidationState validateAction(final boolean display);
    //endregion Button Actions
}
