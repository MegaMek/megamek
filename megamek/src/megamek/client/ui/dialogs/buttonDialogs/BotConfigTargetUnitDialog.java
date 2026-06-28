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

import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashSet;
import java.util.Set;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.border.EmptyBorder;

import megamek.client.ui.Messages;
import megamek.client.ui.util.UIUtil;
import megamek.client.ui.util.UIUtil.TipTextField;

/** A dialog for entering one or more entity IDs that are to be used as strategic targets by Princess. */
public class BotConfigTargetUnitDialog extends AbstractButtonDialog {

    private static final String OK_ACTION = "Ok_Action";

    private final TipTextField unitIDField = new TipTextField(5, "..., ...");
    private final JLabel unitIDLabel = new JLabel(Messages.getString("BotConfigDialog.unitIdLabel"));
    private final JLabel noteLabel = new JLabel("<HTML><CENTER>" + Messages.getString("BotConfigDialog.noteLabel"));

    protected BotConfigTargetUnitDialog(JFrame frame) {
        super(frame, "BotConfigTargetUnitDialog", "BotConfigDialog.bctudTitle");
        initialize();
        // Catch the Enter key as "OK"
        final KeyStroke enter = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(enter, OK_ACTION);
        getRootPane().getInputMap(JComponent.WHEN_FOCUSED).put(enter, OK_ACTION);
        getRootPane().getActionMap().put(OK_ACTION, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                okButtonActionPerformed(evt);
            }
        });
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                unitIDField.requestFocus();
            }
        });
    }

    @Override
    protected Container createCenterPane() {
        JPanel result = new JPanel();
        result.setLayout(new BoxLayout(result, BoxLayout.PAGE_AXIS));
        result.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        JPanel unitIDPanel = new UIUtil.FixedYPanel();
        unitIDField.setToolTipText(Messages.getString("BotConfigDialog.unitIdTip"));
        unitIDLabel.setLabelFor(unitIDField);
        unitIDLabel.setDisplayedMnemonic(KeyEvent.VK_I);
        unitIDPanel.add(unitIDLabel);
        unitIDPanel.add(unitIDField);

        noteLabel.setBorder(new EmptyBorder(0, 20, 0, 20));
        noteLabel.setAlignmentX(JComponent.CENTER_ALIGNMENT);

        result.add(Box.createVerticalStrut(15));
        result.add(unitIDPanel);
        result.add(Box.createVerticalStrut(15));
        result.add(noteLabel);
        return result;
    }

    /** Returns a list of entered entity IDs. The list may be empty but not null. */
    public Set<Integer> getSelectedIDs() {
        Set<Integer> result = new HashSet<>();
        String[] tokens = unitIDField.getText().split(",");
        for (String token : tokens) {
            // This allows for Priority targets to be specified with ranges, as well as comma separated
            String[] inclusive = token.split("-");
            if (inclusive.length == 2) {
                try {
                    int start = Integer.parseInt(inclusive[0]);
                    int end = Integer.parseInt(inclusive[0]);
                    if (start <= end) {
                        for (int i = start; i <= end; i++) {
                            result.add(i);
                        }
                    }
                } catch (NumberFormatException e) {
                    // Unit ID could not be parsed
                }
            } else {
                try {
                    result.add(Integer.parseInt(token));
                } catch (NumberFormatException e) {
                    // No unit ID if it cannot be parsed
                }
            }
        }
        return result;
    }
}
