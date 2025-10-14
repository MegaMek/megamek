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
package megamek.client.ui.dialogs.phaseDisplay;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serial;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

import megamek.client.ui.Messages;
import megamek.codeUtilities.StringUtility;

/**
 * Ask for the setting for a vibrabomb.
 */
public class VibrabombSettingDialog extends JDialog implements ActionListener {
    @Serial
    private static final long serialVersionUID = -7642956136536119067L;
    private final JButton butOk = new JButton(Messages.getString("Okay"));
    private final JTextField fldSetting = new JTextField("20", 2);
    private int setting;
    private final JFrame frame;

    public VibrabombSettingDialog(JFrame p) {
        super(p, Messages.getString("VibrabombSettingDialog.title"), true);
        super.setResizable(false);
        frame = p;
        butOk.addActionListener(this);
        JLabel labMessage = new JLabel(Messages.getString("VibrabombSettingDialog.selectSetting"));
        GridBagLayout gridBagLayout = new GridBagLayout();
        getContentPane().setLayout(gridBagLayout);
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.fill = GridBagConstraints.VERTICAL;
        gridBagConstraints.insets = new Insets(1, 1, 1, 1);
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER;
        gridBagLayout.setConstraints(labMessage, gridBagConstraints);
        getContentPane().add(labMessage);
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagLayout.setConstraints(fldSetting, gridBagConstraints);
        getContentPane().add(fldSetting);
        gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = GridBagConstraints.CENTER;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagLayout.setConstraints(butOk, gridBagConstraints);
        getContentPane().add(butOk);
        pack();
        setLocation(p.getLocation().x + p.getSize().width / 2 - getSize().width
              / 2, p.getLocation().y + p.getSize().height / 2
              - getSize().height / 2);
    }

    public int getSetting() {
        return setting;
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        if (actionEvent.getSource().equals(butOk)) {
            String s = fldSetting.getText();
            try {
                if (!StringUtility.isNullOrBlank(s)) {
                    setting = Integer.parseInt(s);
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(frame,
                      Messages.getString("VibrabombSettingDialog.alert.Message"),
                      Messages.getString("VibrabombSettingDialog.alert.Title"),
                      JOptionPane.WARNING_MESSAGE);
                return;
            }
            if ((setting < 20) || (setting > 100)) {
                JOptionPane.showMessageDialog(frame,
                      Messages.getString("VibrabombSettingDialog.alert.Message"),
                      Messages.getString("VibrabombSettingDialog.alert.Title"),
                      JOptionPane.WARNING_MESSAGE);
                return;
            }
        }
        setVisible(false);
    }
}
