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
import megamek.client.ui.swing.dialog.imageChooser.AbstractIconChooser;
import megamek.common.icons.AbstractIcon;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Creates a dialog that allows players to select a directory from
 * a directory tree and choose an image from the images in that directory.
 * Subclasses must provide the getItems() method that translates
 * a given category (directory) selected in the tree to a list
 * of items (images) to show in the list.
 * Subclasses can provide getSearchedItems() that translates a given search
 * String to the list of "found" items. If this is provided, showSearch(true)
 * should be called in the constructor to show the search panel.
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
                                     final AbstractIconChooser chooser) {
        super(frame, name, title);
        setChooser(chooser);
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

    /**
     * Constructs the bottom panel with the Okay and Cancel buttons.
     */
    @Override
    protected JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 2));
        panel.setBorder(new EmptyBorder(5, 5, 5, 5));

        panel.add(createButton("Ok.text", "Ok.toolTipText", "okButton", this::okButtonActionPerformed));
        panel.add(createButton("Cancel.text", "Cancel.toolTipText", "cancelButton", this::cancelActionPerformed));
        panel.add(createButton("refreshDirectory.text", "refreshDirectory.toolTipText",
                "refreshButton", evt -> getChooser().refreshDirectory()));

        return panel;
    }
    //endregion Initialization
}
