/*
 * MegaMek - Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
 * 
 *  This program is free software; you can redistribute it and/or modify it 
 *  under the terms of the GNU General Public License as published by the Free 
 *  Software Foundation; either version 2 of the License, or (at your option) 
 *  any later version.
 * 
 *  This program is distributed in the hope that it will be useful, but 
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY 
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License 
 *  for more details.
 */

package megamek.client.ui.AWT;

import java.awt.Button;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Label;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import megamek.client.ui.Messages;

/**
 * Ask for the setting for a vibrabomb.
 */
public class VibrabombSettingDialog extends Dialog implements ActionListener {

    /**
     * 
     */
    private static final long serialVersionUID = 1432135839873581337L;
    private GridBagLayout gridbag = new GridBagLayout();
    private GridBagConstraints c = new GridBagConstraints();

    private Button butOk = new Button(Messages.getString("Okay")); //$NON-NLS-1$
    private TextField fldSetting = new TextField("20", 2); //$NON-NLS-1$

    private int setting = 0;
    private Frame frame = null;

    public VibrabombSettingDialog(Frame p) {
        super(p, Messages.getString("VibrabombSettingDialog.title"), true); //$NON-NLS-1$
        super.setResizable(false);
        frame = p;

        butOk.addActionListener(this);

        Label labMessage = new Label(Messages
                .getString("VibrabombSettingDialog.selectSetting")); //$NON-NLS-1$
        setLayout(gridbag);

        c.fill = GridBagConstraints.VERTICAL;
        c.insets = new Insets(1, 1, 1, 1);
        c.weightx = 1.0;
        c.weighty = 0.0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        gridbag.setConstraints(labMessage, c);
        add(labMessage);

        c.fill = GridBagConstraints.BOTH;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weightx = 0.0;
        c.weighty = 0.0;
        gridbag.setConstraints(fldSetting, c);
        add(fldSetting);

        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.CENTER;
        c.weightx = 0.0;
        c.weighty = 0.0;
        gridbag.setConstraints(butOk, c);
        add(butOk);

        pack();
        setLocation(p.getLocation().x + p.getSize().width / 2 - getSize().width
                / 2, p.getLocation().y + p.getSize().height / 2
                - getSize().height / 2);
    }

    public int getSetting() {
        return setting;
    }

    public void actionPerformed(ActionEvent actionEvent) {
        if (actionEvent.getSource() == butOk) {
            String s = fldSetting.getText();
            try {
                if (s != null && s.length() != 0) {
                    setting = Integer.parseInt(s);
                }
            } catch (NumberFormatException e) {
                AlertDialog ad = new AlertDialog(
                        frame,
                        Messages
                                .getString("VibrabombSettingDialog.alert.Title"), //$NON-NLS-1$
                        Messages
                                .getString("VibrabombSettingDialog.alert.Message")); //$NON-NLS-1$
                ad.setVisible(true);
                return;
            }

            if ((setting < 20) || (setting > 100)) {
                AlertDialog ad = new AlertDialog(
                        frame,
                        Messages
                                .getString("VibrabombSettingDialog.alert.Title"), //$NON-NLS-1$
                        Messages
                                .getString("VibrabombSettingDialog.alert.Message")); //$NON-NLS-1$
                ad.setVisible(true);
                return;
            }
        }
        this.setVisible(false);
    }
}
