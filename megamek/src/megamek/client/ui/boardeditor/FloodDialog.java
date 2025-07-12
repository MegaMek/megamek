/*
 * Copyright (C) 2021, 2025 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.boardeditor;

import java.awt.Container;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

import megamek.client.ui.buttons.MMToggleButton;
import megamek.client.ui.dialogs.buttonDialogs.AbstractButtonDialog;

import static megamek.client.ui.util.UIUtil.*;
import static megamek.client.ui.Messages.*;

final class FloodDialog extends AbstractButtonDialog {

    private final EditorTextField txtLevelChange = new EditorTextField("0", 5, -5, 15);
    private final MMToggleButton butRemove = new MMToggleButton(getString("FloodDialog.removeButton"));

    /** Constructs a modal LevelChangeDialog with frame as parent. */
    FloodDialog(JFrame frame) {
        super(frame, "FloodDialog.name", "FloodDialog.title");
        initialize();
    }

    @Override
    protected Container createCenterPane() {
        JPanel result = new JPanel();
        result.setLayout(new BoxLayout(result, BoxLayout.PAGE_AXIS));
        result.setBorder(new EmptyBorder(10, 30, 10, 30));

        JPanel textFieldPanel = new FixedYPanel();
        textFieldPanel.add(txtLevelChange);

        JPanel toggleButtonPanel = new FixedYPanel();
        toggleButtonPanel.add(butRemove);

        JLabel labInfo = new JLabel("<HTML><CENTER>" + getString("FloodDialog.info"), SwingConstants.CENTER);
        labInfo.setAlignmentX(CENTER_ALIGNMENT);
        JLabel labRemoveInfo = new JLabel("<HTML><CENTER>" + getString("FloodDialog.removeInfo"), SwingConstants.CENTER);
        labRemoveInfo.setAlignmentX(CENTER_ALIGNMENT);

        result.add(Box.createVerticalGlue());
        result.add(labInfo);
        result.add(Box.createVerticalStrut(5));
        result.add(textFieldPanel);
        result.add(Box.createVerticalStrut(5));
        result.add(labRemoveInfo);
        result.add(Box.createVerticalStrut(5));
        result.add(toggleButtonPanel);
        result.add(Box.createVerticalGlue());

        return result;
    }

    /** Returns the level change entered by the user or 0, if it cannot be parsed. */
    public int getLevelChange() {
        try {
            return Integer.parseInt(txtLevelChange.getText());
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    /** Returns if all newly submerged hexes should have their terrain removed. */
    public boolean getRemoveTerrain() {
        return butRemove.isSelected();
    }

}
