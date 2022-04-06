/*
 * MegaMek - Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
 * MegaMek - Copyright (C) 2020, 2021 - The MegaMek Team
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
package megamek.client.ui.swing;

import megamek.client.ui.Messages;
import megamek.client.ui.baseComponents.AbstractButtonDialog;
import megamek.client.ui.swing.util.UIUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/** Allows the player to select the type of entity in the hexes used by the LOS tool. */
public class LOSDialog extends AbstractButtonDialog {

    private final boolean isMechFirst;
    private final boolean isMechSecond;
    private final JToggleButton[] toggles1 = new JToggleButton[2];
    private final JToggleButton[] toggles2 = new JToggleButton[2];

    /**
     * Allows the player to select the height of the entities in the hexes used by the LOS tool.
     * The dialog toggles are preset to the given mechInFirst and mechInSecond.
     */
    public LOSDialog(JFrame parent, boolean mechInFirst, boolean mechInSecond) {
        super(parent, "LOSDialog", "LOSDialog.title");
        isMechFirst = mechInFirst;
        isMechSecond = mechInSecond;
        initialize();
    }

    /**
     * Returns true if the unit in the first hex should be counted as having Mech height (2 levels) and
     * false if it should be counted as having Tank height (1 level).
     */
    public boolean getMechInFirst() {
        return toggles1[0].isSelected();
    }

    /**
     * Returns true if the unit in the second hex should be counted as having Mech height (2 levels) and
     * false if it should be counted as having Tank height (1 level).
     */
    public boolean getMechInSecond() {
        return toggles2[0].isSelected();
    }

    @Override
    protected Container createCenterPane() {
        toggles1[0] = new JToggleButton(Messages.getString("LOSDialog.Mech"));
        toggles1[1] = new JToggleButton(Messages.getString("LOSDialog.NonMech"));
        var firstButtonsPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        firstButtonsPanel.add(toggles1[0]);
        firstButtonsPanel.add(toggles1[1]);
        var firstLinePanel = new UIUtil.FixedYPanel();
        firstLinePanel.add(firstButtonsPanel);

        toggles2[0] = new JToggleButton(Messages.getString("LOSDialog.Mech"));
        toggles2[1] = new JToggleButton(Messages.getString("LOSDialog.NonMech"));
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

        toggles1[0].setSelected(isMechFirst);
        toggles1[1].setSelected(!isMechFirst);
        toggles2[0].setSelected(isMechSecond);
        toggles2[1].setSelected(!isMechSecond);

        ButtonGroup radioGroup1 = new ButtonGroup();
        radioGroup1.add(toggles1[0]);
        radioGroup1.add(toggles1[1]);
        ButtonGroup radioGroup2 = new ButtonGroup();
        radioGroup2.add(toggles2[0]);
        radioGroup2.add(toggles2[1]);

        return centerPanel;
    }
}
