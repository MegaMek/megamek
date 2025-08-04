/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.dialogs;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import megamek.client.ui.Messages;
import megamek.logging.MMLogger;

/**
 * a quick class for the new map dialogue box
 */
public class BoardNewDialog extends JDialog implements ActionListener {
    private final static MMLogger logger = MMLogger.create(BoardNewDialog.class);

    private int xvalue;
    private int yvalue;
    private JLabel labWidth;
    private JLabel labHeight;
    private JTextField texWidth;
    private JTextField texHeight;
    private JButton butOkay;
    private JButton butCancel;

    BoardNewDialog(JFrame frame) {
        super(frame, Messages.getString("BoardEditor.SetDimensions"), true);
        xvalue = 0;
        yvalue = 0;
        labWidth = new JLabel(Messages.getString("BoardEditor.labWidth"), SwingConstants.RIGHT);
        labHeight = new JLabel(Messages.getString("BoardEditor.labHeight"), SwingConstants.RIGHT);
        texWidth = new JTextField("16", 2);
        texHeight = new JTextField("17", 2);
        butOkay = new JButton(Messages.getString("Okay"));
        butOkay.setActionCommand("done");
        butOkay.addActionListener(this);
        butOkay.setSize(80, 24);
        butCancel = new JButton(Messages.getString("Cancel"));
        butCancel.setActionCommand("cancel");
        butCancel.addActionListener(this);
        butCancel.setSize(80, 24);
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        getContentPane().setLayout(gridbag);
        c.fill = GridBagConstraints.NONE;
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.insets = new Insets(5, 5, 1, 1);
        gridbag.setConstraints(labWidth, c);
        getContentPane().add(labWidth);
        c.gridwidth = GridBagConstraints.REMAINDER;
        gridbag.setConstraints(texWidth, c);
        getContentPane().add(texWidth);
        c.gridwidth = GridBagConstraints.RELATIVE;
        gridbag.setConstraints(labHeight, c);
        getContentPane().add(labHeight);
        c.gridwidth = GridBagConstraints.REMAINDER;
        gridbag.setConstraints(texHeight, c);
        getContentPane().add(texHeight);
        c.ipadx = 20;
        c.ipady = 5;
        c.gridwidth = GridBagConstraints.RELATIVE;
        gridbag.setConstraints(butOkay, c);
        getContentPane().add(butOkay);
        c.gridwidth = GridBagConstraints.REMAINDER;
        gridbag.setConstraints(butCancel, c);
        getContentPane().add(butCancel);
        pack();
        setResizable(false);
        setLocation(frame.getLocation().x + frame.getSize().width / 2
                    - getSize().width / 2,
              frame.getLocation().y
                    + frame.getSize().height / 2 - getSize().height / 2);
    }

    @Override
    public void actionPerformed(ActionEvent evt) {
        if (evt.getSource().equals(butOkay)) {
            try {
                xvalue = Integer.decode(texWidth.getText());
                yvalue = Integer.decode(texHeight.getText());
            } catch (Exception ex) {
                logger.error(ex, "actionPerformed");
            }
            setVisible(false);
        } else if (evt.getSource().equals(butCancel)) {
            setVisible(false);
        }
    }

    @Override
    public int getX() {
        return xvalue;
    }

    @Override
    public int getY() {
        return yvalue;
    }
}
