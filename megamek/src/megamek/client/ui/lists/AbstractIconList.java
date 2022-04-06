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
package megamek.client.ui.lists;

import megamek.common.annotations.Nullable;
import megamek.common.icons.AbstractIcon;

import javax.swing.*;
import java.util.List;

/**
 * A specialized JList to display a list of AbstractIcons for the AbstractIconChooserDialog,
 * e.g. camos or portraits. The images will be displayed with horizontal wrap. This is best
 * embedded in a JScrollPane.
 * Using any of the renderers in the package the images can be displayed with or without the filename.
 */
public class AbstractIconList extends JList<AbstractIcon> {
    //region Variable Declarations
    private static final long serialVersionUID = -8060324139099113292L;

    private final DefaultListModel<AbstractIcon> iconModel;
    //endregion Variable Declarations

    //region Constructors
    public AbstractIconList(final ListCellRenderer<AbstractIcon> renderer) {
        super(); 
        iconModel = new DefaultListModel<>();
        setModel(iconModel);
        setOpaque(true);
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setLayoutOrientation(JList.HORIZONTAL_WRAP);
        setFixedCellWidth(100);
        setFixedCellHeight(110);
        setVisibleRowCount(-1);
        setCellRenderer(renderer);
    }
    //endregion Constructors

    /**
     * @param icons the icons to select on this list
     */
    public void setSelectedValues(final AbstractIcon... icons) {
        clearSelection();
        for (final AbstractIcon icon : icons) {
            final int index = getIndex(icon);
            if (index >= 0) {
                addSelectionInterval(index, index);
            }
        }
    }

    /**
     * @param icon the specified AbstractIcon, which may be null
     * @return the index of the specified icon, or -1 if it is null or can't be found
     */
    public int getIndex(final @Nullable AbstractIcon icon) {
        if (icon == null) {
            return -1;
        } else if (getModel() instanceof DefaultListModel) {
            return ((DefaultListModel<AbstractIcon>) getModel()).indexOf(icon);
        }

        for (int i = 0; i < getModel().getSize(); i++) {
            if (icon.equals(getModel().getElementAt(i))) {
                return i;
            }
        }

        return -1;
    }

    /** 
     * Updates the list to only show the given icons.
     */
    public void updateImages(final List<AbstractIcon> icons) {
        iconModel.clear();
        iconModel.addAll(icons);
    }
}
