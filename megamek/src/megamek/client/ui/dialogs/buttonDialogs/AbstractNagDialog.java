/*
 * Copyright (C) 2023-2025 The MegaMek Team. All Rights Reserved.
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

import megamek.MegaMek;
import megamek.client.ui.enums.DialogResult;

import javax.swing.*;
import javax.swing.GroupLayout.Alignment;
import java.awt.*;
import java.util.ResourceBundle;

public abstract class AbstractNagDialog extends AbstractButtonDialog {
    //region Variable Declarations
    private final String key;
    protected boolean show;
    private String description;
    private JCheckBox chkIgnore;
    //endregion Variable Declarations

    //region Constructors
    protected AbstractNagDialog(final JFrame frame, final String name, final String title,
                                final String description, final String key) {
        super(frame, name, title);
        this.key = key;
        setDescription(description.isBlank() ? description : resources.getString(description));
        setShow(checkNag());
        if (isShow()) {
            initialize();
        } else {
            setResult(DialogResult.CANCELLED);
        }
    }

    protected AbstractNagDialog(final JFrame frame, final ResourceBundle resources,
                                final String name, final String title, final String key) {
        super(frame, true, resources, name, title);
        this.key = key;
    }
    //endregion Constructors

    //region Getters
    public String getKey() {
        return key;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public boolean isShow() {
        return show;
    }

    public void setShow(final boolean show) {
        this.show = show;
    }

    public JCheckBox getChkIgnore() {
        return chkIgnore;
    }

    public void setChkIgnore(final JCheckBox chkIgnore) {
        this.chkIgnore = chkIgnore;
    }
    //endregion Getters

    //region Initialization
    @Override
    protected Container createCenterPane() {
        // Create Panel Components
        final JTextArea txtDescription = new JTextArea(getDescription());
        txtDescription.setName("txtDescription");
        txtDescription.setEditable(false);
        txtDescription.setOpaque(false);

        setChkIgnore(new JCheckBox(resources.getString("chkIgnore.text")));
        getChkIgnore().setToolTipText(resources.getString("chkIgnore.toolTipText"));
        getChkIgnore().setName("chkIgnore");

        // Layout the UI
        final JPanel panel = new JPanel();
        panel.setName("nagPanel");
        final GroupLayout layout = new GroupLayout(panel);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        panel.setLayout(layout);

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addComponent(txtDescription)
                        .addComponent(getChkIgnore())
        );

        layout.setHorizontalGroup(
                layout.createParallelGroup(Alignment.LEADING)
                        .addComponent(txtDescription)
                        .addComponent(getChkIgnore())
        );

        return panel;
    }
    //endregion Initialization

    //region Button Actions
    @Override
    protected void okAction() {
        MegaMek.getMMOptions().setNagDialogIgnore(getKey(), getChkIgnore().isSelected());
    }

    @Override
    protected void cancelAction() {
        MegaMek.getMMOptions().setNagDialogIgnore(getKey(), getChkIgnore().isSelected());
    }
    //endregion Button Actions

    protected abstract boolean checkNag();

    @Override
    public void setVisible(final boolean visible) {
        super.setVisible(visible && isShow());
    }
}
