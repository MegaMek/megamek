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
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

import megamek.client.ui.Messages;
import megamek.codeUtilities.MathUtility;

/**
 * Ask for the setting for a vibrabomb.
 */
public class MineDensityDialog extends JDialog implements ActionListener {
    @Serial
    private static final long serialVersionUID = -7642956136536119067L;
    private final JButton butOk = new JButton(Messages.getString("Okay"));
    private final JComboBox<String> choDensity = new JComboBox<>();
    private int density = -1;
    //private JFrame frame;

    public MineDensityDialog(JFrame frame) {
        super(frame, Messages.getString("MineDensityDialog.title"), true);
        super.setResizable(false);
        //frame = frame;
        butOk.addActionListener(this);

        choDensity.removeAllItems();
        for (int i = 5; i < 35; i = i + 5) {
            choDensity.addItem(Integer.toString(i));
        }
        choDensity.setSelectedIndex(0);

        GridBagLayout gridBagLayout = new GridBagLayout();
        getContentPane().setLayout(gridBagLayout);
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.fill = GridBagConstraints.VERTICAL;
        gridBagConstraints.insets = new Insets(1, 1, 1, 1);
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER;
        JLabel labDensity = new JLabel(Messages.getString("MineDensityDialog.labDensity"), SwingConstants.RIGHT);
        gridBagLayout.setConstraints(labDensity, gridBagConstraints);
        getContentPane().add(labDensity);
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagLayout.setConstraints(choDensity, gridBagConstraints);
        getContentPane().add(choDensity);
        gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = GridBagConstraints.CENTER;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagLayout.setConstraints(butOk, gridBagConstraints);
        getContentPane().add(butOk);
        pack();
        setLocation(frame.getLocation().x + frame.getSize().width / 2 - getSize().width / 2,
              frame.getLocation().y + frame.getSize().height / 2 - getSize().height / 2);
    }

    public int getDensity() {
        return density;
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        if (actionEvent.getSource() == butOk) {
            density = MathUtility.parseInt((String) choDensity.getSelectedItem(), 5);
        }
        this.setVisible(false);
    }
}
