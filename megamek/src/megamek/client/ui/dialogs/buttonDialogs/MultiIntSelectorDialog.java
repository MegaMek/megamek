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

import static megamek.client.ui.Messages.getString;

import java.awt.Container;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

import megamek.client.ui.util.UIUtil.FixedYPanel;

/**
 * A dialog that allows the user to select some count of ints within some range.
 */
public class MultiIntSelectorDialog extends AbstractButtonDialog {
    private final String description;
    private final JList<Integer> list;

    /** Constructs a modal dialog with frame as parent. */
    public MultiIntSelectorDialog(JFrame frame, String nameResourceID, String titleResourceID,
          String descriptionResourceID, int maxValue, List<Integer> selectedItems) {
        super(frame, nameResourceID, titleResourceID);
        this.setResizable(false);

        description = getString(descriptionResourceID);
        ListModel<Integer> listData = new AbstractListModel<>() {
            @Override
            public int getSize() {
                return maxValue;
            }

            @Override
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

        JLabel labInfo = new JLabel("<CENTER>" + description, SwingConstants.CENTER);
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
