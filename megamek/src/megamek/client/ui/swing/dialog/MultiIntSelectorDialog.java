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

package megamek.client.ui.swing.dialog;

import static megamek.client.ui.Messages.getString;
import static megamek.client.ui.swing.util.UIUtil.scaleStringForGUI;

import java.awt.Container;
import java.util.List;

import javax.swing.*;
import javax.swing.border.EmptyBorder;

import megamek.client.ui.baseComponents.AbstractButtonDialog;
import megamek.client.ui.swing.util.UIUtil.FixedYPanel;

/**
 * A dialog that allows the user to select some count of ints within some range.
 */
public class MultiIntSelectorDialog extends AbstractButtonDialog {
    private String description;
    private JList<Integer> list;

    /** Constructs a modal dialog with frame as parent. */
    public MultiIntSelectorDialog(JFrame frame, String nameResourceID, String titleResourceID,
            String descriptionResourceID, int maxValue, List<Integer> selectedItems) {
        super(frame, nameResourceID, titleResourceID);
        this.setResizable(false);

        description = getString(descriptionResourceID);
        ListModel<Integer> listData = new AbstractListModel<Integer>() {
            public int getSize() {
                return maxValue;
            }

            public Integer getElementAt(int index) {
                return index;
            }
        };

        list = new JList<>(listData);
        list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        int[] selectedItemArray = new int[selectedItems.size()];
        for (int x = 0; x < selectedItems.size(); x++) {
            selectedItemArray[x] = selectedItems.get(x);
        }

        list.setSelectedIndices(selectedItemArray);

        initialize();
    }

    @Override
    protected Container createCenterPane() {
        JPanel result = new JPanel();
        result.setLayout(new BoxLayout(result, BoxLayout.PAGE_AXIS));
        result.setBorder(new EmptyBorder(10, 30, 10, 30));

        JPanel listFieldPanel = new FixedYPanel();
        listFieldPanel.add(list);

        JLabel labInfo = new JLabel(scaleStringForGUI("<CENTER>" + description),
                SwingConstants.CENTER);
        labInfo.setAlignmentX(CENTER_ALIGNMENT);

        result.add(Box.createVerticalGlue());
        result.add(labInfo);
        result.add(Box.createVerticalStrut(5));
        result.add(listFieldPanel);
        result.add(Box.createVerticalGlue());

        return result;
    }

    /**
     * Returns the level change entered by the user or 0, if it cannot be parsed.
     */
    public List<Integer> getSelectedItems() {
        return list.getSelectedValuesList();
    }
}
