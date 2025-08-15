/*
 * Copyright (C) 2020-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.dialogs.iconChooser;

import java.awt.Container;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ResourceBundle;
import javax.swing.JFrame;
import javax.swing.JPanel;

import megamek.MegaMek;
import megamek.client.ui.buttons.MMButton;
import megamek.client.ui.dialogs.buttonDialogs.AbstractButtonDialog;
import megamek.client.ui.enums.DialogResult;
import megamek.client.ui.panels.abstractPanels.abstractIconChooserPanel;
import megamek.common.annotations.Nullable;
import megamek.common.icons.AbstractIcon;

/**
 * Creates a dialog that allows players to select a directory from a directory tree and choose an AbstractIcon from the
 * AbstractIcons in that directory. Subclasses must provide the getItems() method that translates a given category
 * (directory) selected in the tree to a list of items (AbstractIcons) to show in the list. Subclasses can provide
 * getSearchedItems() that translates a given search String to the list of "found" items. If this is provided,
 * showSearch(true) should be called in the constructor to show the search panel.
 */
public abstract class AbstractIconChooserDialog extends AbstractButtonDialog {
    //region Variable Declarations
    private abstractIconChooserPanel chooser;
    //endregion Variable Declarations

    //region Constructors
    public AbstractIconChooserDialog(final JFrame frame, final String name, final String title,
          final abstractIconChooserPanel chooser, final boolean doubleClick) {
        this(frame, true, ResourceBundle.getBundle("megamek.client.messages",
                    MegaMek.getMMOptions().getLocale()), name,
              title, chooser, doubleClick);
    }

    /**
     * Creates a dialog that allows players to choose a directory from a directory tree and an AbstractIcon from the
     * AbstractIcons in that directory.
     *
     * @param frame       The frame hosting this dialog
     * @param modal       the modality of this dialog
     * @param resources   the resource bundle to use
     * @param name        the dialog's name
     * @param title       the dialog title resource string
     * @param chooser     the icon chooser display panel
     * @param doubleClick whether double clicking closes the dialog or not
     */
    public AbstractIconChooserDialog(final JFrame frame, final boolean modal,
          final ResourceBundle resources, final String name,
          final String title, final abstractIconChooserPanel chooser,
          final boolean doubleClick) {
        super(frame, modal, resources, name, title);
        setChooser(chooser);
        if (doubleClick) {
            getChooser().getImageList().addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(final MouseEvent evt) {
                    if (evt.getClickCount() == 2) {
                        okButtonActionPerformed(null);
                    }
                }
            });
        }
        initialize();
    }
    //endregion Constructors

    //region Getters/Setters
    protected abstractIconChooserPanel getChooser() {
        return chooser;
    }

    public void setChooser(final abstractIconChooserPanel chooser) {
        this.chooser = chooser;
    }

    /**
     * @return the selected item, which is non-nullable when the dialog is confirmed.
     */
    public @Nullable AbstractIcon getSelectedItem() {
        return getChooser().getSelectedItem();
    }
    //endregion Getters/Setters

    //region Initialization
    @Override
    protected Container createCenterPane() {
        return getChooser();
    }

    @Override
    protected JPanel createButtonPanel() {
        final JPanel panel = new JPanel(new GridLayout(1, 3));
        panel.setName("buttonPanel");

        panel.add(new MMButton("btnOk", resources, "Ok.text", "Ok.toolTipText",
              this::okButtonActionPerformed));
        panel.add(new MMButton("btnCancel", resources, "Cancel.text", "Cancel.toolTipText",
              this::cancelActionPerformed));
        panel.add(new MMButton("btnRefresh", resources, "RefreshDirectory.text",
              "RefreshDirectory.toolTipText", evt -> getChooser().refreshDirectory()));

        return panel;
    }
    //endregion Initialization

    //region Button Actions
    @Override
    protected void okButtonActionPerformed(final @Nullable ActionEvent evt) {
        okAction();
        setResult((getChooser().getSelectedItem() == null) ? DialogResult.CANCELLED : DialogResult.CONFIRMED);
        setVisible(false);
    }
    //endregion Button Actions
}
