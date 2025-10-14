/*
 * Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2003-2025 The MegaMek Team. All Rights Reserved.
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

import java.awt.Container;
import java.awt.GridLayout;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.border.EmptyBorder;

import megamek.client.ui.Messages;
import megamek.client.ui.util.UIUtil;

/**
 * Allows the player to select the type of entity in the hexes used by the LOS tool.
 */
public class LOSDialog extends AbstractButtonDialog {

    private final boolean isMekFirst;
    private final boolean isMekSecond;
    private final JToggleButton[] toggles1 = new JToggleButton[2];
    private final JToggleButton[] toggles2 = new JToggleButton[2];

    /**
     * Allows the player to select the height of the entities in the hexes used by the LOS tool. The dialog toggles are
     * preset to the given mekInFirst and mekInSecond.
     */
    public LOSDialog(JFrame parent, boolean mekInFirst, boolean mekInSecond) {
        super(parent, "LOSDialog", "LOSDialog.title");
        isMekFirst = mekInFirst;
        isMekSecond = mekInSecond;
        initialize();
    }

    /**
     * Returns true if the unit in the first hex should be counted as having Mek height (2 levels) and false if it
     * should be counted as having Tank height (1 level).
     */
    public boolean getMekInFirst() {
        return toggles1[0].isSelected();
    }

    /**
     * Returns true if the unit in the second hex should be counted as having Mek height (2 levels) and false if it
     * should be counted as having Tank height (1 level).
     */
    public boolean getMekInSecond() {
        return toggles2[0].isSelected();
    }

    @Override
    protected Container createCenterPane() {
        toggles1[0] = new JToggleButton(Messages.getString("LOSDialog.Mek"));
        toggles1[1] = new JToggleButton(Messages.getString("LOSDialog.NonMek"));
        var firstButtonsPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        firstButtonsPanel.add(toggles1[0]);
        firstButtonsPanel.add(toggles1[1]);
        var firstLinePanel = new UIUtil.FixedYPanel();
        firstLinePanel.add(firstButtonsPanel);

        toggles2[0] = new JToggleButton(Messages.getString("LOSDialog.Mek"));
        toggles2[1] = new JToggleButton(Messages.getString("LOSDialog.NonMek"));
        var secondButtonsPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        secondButtonsPanel.add(toggles2[0]);
        secondButtonsPanel.add(toggles2[1]);
        var secondLinePanel = new JPanel();
        secondLinePanel.add(secondButtonsPanel);

        var firstHeader = new JLabel(Messages.getString("LOSDialog.inFirstHex"), JLabel.CENTER);
        firstHeader.setAlignmentX(JComponent.CENTER_ALIGNMENT);

        var secondHeader = new JLabel(Messages.getString("LOSDialog.InSecondHex"), JLabel.CENTER);
        secondHeader.setAlignmentX(JComponent.CENTER_ALIGNMENT);

        JPanel centerPanel = new JPanel();
        centerPanel.setBorder(new EmptyBorder(20, 50, 10, 50));
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.PAGE_AXIS));
        centerPanel.add(Box.createVerticalGlue());
        centerPanel.add(firstHeader);
        centerPanel.add(firstLinePanel);
        centerPanel.add(Box.createVerticalStrut(20));
        centerPanel.add(secondHeader);
        centerPanel.add(secondLinePanel);
        centerPanel.add(Box.createVerticalGlue());

        toggles1[0].setSelected(isMekFirst);
        toggles1[1].setSelected(!isMekFirst);
        toggles2[0].setSelected(isMekSecond);
        toggles2[1].setSelected(!isMekSecond);

        ButtonGroup radioGroup1 = new ButtonGroup();
        radioGroup1.add(toggles1[0]);
        radioGroup1.add(toggles1[1]);
        ButtonGroup radioGroup2 = new ButtonGroup();
        radioGroup2.add(toggles2[0]);
        radioGroup2.add(toggles2[1]);

        return centerPanel;
    }
}
