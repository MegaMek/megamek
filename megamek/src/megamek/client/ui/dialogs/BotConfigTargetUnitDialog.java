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
package megamek.client.ui.dialogs;

import java.awt.Container;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;

import megamek.client.ui.Messages;
import megamek.client.ui.baseComponents.AbstractButtonDialog;
import megamek.client.ui.swing.util.UIUtil;
import megamek.client.ui.swing.util.UIUtil.TipTextField;

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
        adaptToGUIScale();
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
            try {
                result.add(Integer.parseInt(token));
            } catch (NumberFormatException e) {
                // No unit ID if it cannot be parsed
            }
        }
        return result;
    }

    private void adaptToGUIScale() {
        UIUtil.adjustDialog(this,  UIUtil.FONT_SCALE1);
    }
}
