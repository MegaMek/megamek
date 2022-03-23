/*
 * Copyright (c) 2000-2002 - Ben Mazur (bmazur@sev.org).
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.client.ui.swing;

import megamek.client.ui.Messages;
import megamek.common.Game;
import megamek.common.options.OptionsConstants;
import megamek.common.weapons.bayweapons.CapitalMissileBayWeapon;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Ask for the velocity setting for a teleoperated missile.
 */
public class TeleMissileSettingDialog extends JDialog implements ActionListener {
    private static final long serialVersionUID = -7642946136536329067L;
    private GridBagLayout gridbag = new GridBagLayout();
    private GridBagConstraints c = new GridBagConstraints();
    private JButton butOk = new JButton(Messages.getString("Okay"));
    private JTextField fldSetting = new JTextField("50", 2);
    private int setting;
    private JFrame frame;
    private int minimumVelocity = CapitalMissileBayWeapon.CAPITAL_MISSILE_MIN_VELOCITY;
    private int maxVelocity = CapitalMissileBayWeapon.CAPITAL_MISSILE_DEFAULT_VELOCITY;

    public TeleMissileSettingDialog(JFrame p, Game game) {
        super(p, Messages.getString("SetTeleMissileVolcityDialog.title"), true);
        super.setResizable(false);
        maxVelocity = game.getOptions().intOption(OptionsConstants.ADVAERORULES_STRATOPS_BEARINGS_ONLY_VELOCITY);
        frame = p;
        butOk.addActionListener(this);
        JLabel labMessage = new JLabel(Messages.getString("SetTeleMissileVelocityDialog.labSetVelocity"));
        getContentPane().setLayout(gridbag);
        c.fill = GridBagConstraints.VERTICAL;
        c.insets = new Insets(1, 1, 1, 1);
        c.weightx = 1.0;
        c.weighty = 0.0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        gridbag.setConstraints(labMessage, c);
        getContentPane().add(labMessage);
        c.fill = GridBagConstraints.BOTH;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weightx = 0.0;
        c.weighty = 0.0;
        gridbag.setConstraints(fldSetting, c);
        getContentPane().add(fldSetting);
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.CENTER;
        c.weightx = 0.0;
        c.weighty = 0.0;
        gridbag.setConstraints(butOk, c);
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
