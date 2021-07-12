/*
 * Copyright (c) 2020-2021 - The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.dialogs;

import megamek.client.ui.baseComponents.AbstractButtonDialog;
import megamek.client.ui.baseComponents.MMButton;
import megamek.client.ui.enums.DialogResult;
import megamek.client.ui.panels.AbstractIconChooser;
import megamek.client.ui.preferences.JSplitPanePreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.common.annotations.Nullable;
import megamek.common.icons.AbstractIcon;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Creates a dialog that allows players to select a directory from a directory tree and choose an
 * image from the images in that directory. Subclasses must provide the getItems() method that
 * translates a given category (directory) selected in the tree to a list of items (images) to show
 * in the list.
 * Subclasses can provide getSearchedItems() that translates a given search String to the list of
 * "found" items. If this is provided, showSearch(true) should be called in the constructor to show
 * the search panel.
 */
public abstract class AbstractIconChooserDialog extends AbstractButtonDialog {
    //region Variable Declarations
    private AbstractIconChooser chooser;
    //endregion Variable Declarations

    //region Constructors
    /**
     * Creates a dialog that allows players to choose a directory from a directory tree and an image
     * from the images in that directory.
     *
     * @param frame The frame hosting this dialog
     * @param name the dialog's name
     * @param title the dialog title resource string
     * @param chooser the icon chooser display panel
     */
    public AbstractIconChooserDialog(final JFrame frame, final String name, final String title,
                                     final AbstractIconChooser chooser, final boolean doubleClick) {
        super(frame, name, title);
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
    protected AbstractIconChooser getChooser() {
        return chooser;
    }

    public void setChooser(final AbstractIconChooser chooser) {
        this.chooser = chooser;
    }

    public AbstractIcon getSelectedItem() {
        return getChooser().getSelectedItem();
    }

    public int getSelectedIndex() {
        return getChooser().getSelectedIndex();
    }
    //endregion Getters/Setters

    //region Initialization
    @Override
    protected Container createCenterPane() {
        return getChooser();
    }

    @Override
    protected JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 2));
        panel.setBorder(new EmptyBorder(5, 5, 5, 5));

        panel.add(new MMButton("btnOk", resources, "Ok.text", "Ok.toolTipText", this::okButtonActionPerformed));
        panel.add(new MMButton("btnCancel", resources, "Cancel.text", "Cancel.toolTipText", this::cancelActionPerformed));
        panel.add(new MMButton("btnRefresh", resources, "refreshDirectory.text", "refreshDirectory.toolTipText",
                evt -> getChooser().refreshDirectory()));

        return panel;
    }

    @Override
    protected void setCustomPreferences(final PreferencesNode preferences) {
        preferences.manage(new JSplitPanePreference(getChooser().getSplitPane()));
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
