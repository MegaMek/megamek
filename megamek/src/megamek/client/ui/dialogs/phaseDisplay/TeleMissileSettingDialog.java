/*
 * Copyright (c) 2000-2002 - Ben Mazur (bmazur@sev.org).
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
import megamek.common.game.Game;
import megamek.common.options.OptionsConstants;
import megamek.common.weapons.bayweapons.capital.CapitalMissileBayWeapon;

/**
 * Ask for the velocity setting for a teleoperated missile.
 */
public class TeleMissileSettingDialog extends JDialog implements ActionListener {
    @Serial
    private static final long serialVersionUID = -7642946136536329067L;
    private final JButton butOk = new JButton(Messages.getString("Okay"));
    private final JTextField fldSetting = new JTextField("50", 2);
    private int setting;
    private final JFrame frame;
    private final int maxVelocity;

    public TeleMissileSettingDialog(JFrame p, Game game) {
        super(p, Messages.getString("SetTeleMissileVolcityDialog.title"), true);
        super.setResizable(false);
        maxVelocity = game.getOptions().intOption(OptionsConstants.ADVAERORULES_STRATOPS_BEARINGS_ONLY_VELOCITY);
        frame = p;
        butOk.addActionListener(this);
        JLabel labMessage = new JLabel(Messages.getString("SetTeleMissileVelocityDialog.labSetVelocity"));
        GridBagLayout gridBagLayout = new GridBagLayout();
        getContentPane().setLayout(gridBagLayout);
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.VERTICAL;
        c.insets = new Insets(1, 1, 1, 1);
        c.weightx = 1.0;
        c.weighty = 0.0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        gridBagLayout.setConstraints(labMessage, c);
        getContentPane().add(labMessage);
        c.fill = GridBagConstraints.BOTH;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weightx = 0.0;
        c.weighty = 0.0;
        gridBagLayout.setConstraints(fldSetting, c);
        getContentPane().add(fldSetting);
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.CENTER;
        c.weightx = 0.0;
        c.weighty = 0.0;
        gridBagLayout.setConstraints(butOk, c);
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
                if (s != null && !s.isBlank()) {
                    setting = Integer.parseInt(s);
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(frame,
                      Messages.getString("SetTeleMissileVelocityDialog.error.message"),
                      Messages.getString("SetTeleMissileVolcityDialog.error.title"),
                      JOptionPane.WARNING_MESSAGE);
                return;
            }
            int minimumVelocity = CapitalMissileBayWeapon.CAPITAL_MISSILE_MIN_VELOCITY;
            if ((setting < minimumVelocity) || (setting > maxVelocity)) {
                JOptionPane.showMessageDialog(frame,
                      Messages.getString("SetTeleMissileVelocityDialog.error.message", maxVelocity),
                      Messages.getString("SetTeleMissileVolcityDialog.error.title"),
                      JOptionPane.WARNING_MESSAGE);
                return;
            }
        }
        setVisible(false);
    }
}
