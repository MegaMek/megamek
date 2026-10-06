/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.dialogs.phaseDisplay;

import java.awt.Container;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.border.EmptyBorder;

import megamek.client.ui.Messages;
import megamek.client.ui.dialogs.buttonDialogs.AbstractButtonDialog;
import megamek.client.ui.util.UIUtil;

/**
 * Asks the player where to aim a called blow with a physical weapon: uncalled (full body table), high
 * (punch table) or low (kick table). The choices are shown as radio buttons with their to-hit numbers.
 * The dialog is resizable, and its size and position are persisted between uses via the dialog
 * preferences mechanism of {@link AbstractButtonDialog}.
 *
 * <p>Use {@link #showDialog()} to run the dialog and {@link #getSelectedIndex()} to read the chosen
 * option afterwards.</p>
 */
public class CalledBlowDialog extends AbstractButtonDialog {

    private final String weaponName;
    private final String[] choices;
    private JRadioButton[] choiceButtons;

    /**
     * Constructs a modal called-blow dialog for the given physical weapon.
     *
     * @param frame      the dialog's parent frame
     * @param weaponName the display name of the physical weapon being swung
     * @param choices    the choice labels, first entry pre-selected
     */
    public CalledBlowDialog(JFrame frame, String weaponName, String[] choices) {
        super(frame, "CalledBlowDialog", "PhysicalDisplay.CalledBlowDialog.title");
        this.weaponName = weaponName;
        this.choices = choices.clone();
        initialize();
        setTitle(Messages.getString("PhysicalDisplay.CalledBlowDialog.title", weaponName));
    }

    @Override
    protected Container createCenterPane() {
        int verticalPadding = UIUtil.scaleForGUI(10);
        int horizontalPadding = UIUtil.scaleForGUI(15);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
        panel.setBorder(new EmptyBorder(verticalPadding, horizontalPadding, verticalPadding, horizontalPadding));

        JLabel message = new JLabel(Messages.getString("PhysicalDisplay.CalledBlowDialog.message", weaponName));
        message.setAlignmentX(LEFT_ALIGNMENT);
        panel.add(message);
        panel.add(Box.createVerticalStrut(verticalPadding));

        ButtonGroup radioGroup = new ButtonGroup();
        choiceButtons = new JRadioButton[choices.length];
        for (int index = 0; index < choices.length; index++) {
            choiceButtons[index] = new JRadioButton(choices[index], index == 0);
            choiceButtons[index].setAlignmentX(LEFT_ALIGNMENT);
            radioGroup.add(choiceButtons[index]);
            panel.add(choiceButtons[index]);
        }
        return panel;
    }

    /**
     * @return the index into the choices array of the selected radio button; {@code 0} (the first,
     *       pre-selected choice) when nothing else was selected
     */
    public int getSelectedIndex() {
        for (int index = 0; index < choiceButtons.length; index++) {
            if (choiceButtons[index].isSelected()) {
                return index;
            }
        }
        return 0;
    }
}
