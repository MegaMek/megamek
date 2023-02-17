/*
 * Copyright (c) 2023 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.client.ui.baseComponents;

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
            setResult(DialogResult.CONFIRMED);
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
