/*
 * MegaMek - Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
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
public class SeaMineDepthDialog extends JDialog implements ActionListener {
    @Serial
    private static final long serialVersionUID = -7642956136536119067L;
    private final JButton butOk = new JButton(Messages.getString("Okay"));
    private final JComboBox<String> choDepth = new JComboBox<>();
    private int depth;

    public SeaMineDepthDialog(JFrame p, int totalDepth) {
        super(p, Messages.getString("MineDensityDialog.title"), true);
        super.setResizable(false);
        butOk.addActionListener(this);

        choDepth.removeAllItems();
        for (int i = 0; i < (totalDepth + 1); i++) {
            choDepth.addItem(Integer.toString(i));
        }
        choDepth.setSelectedIndex(0);

        GridBagLayout gridBagLayout = new GridBagLayout();
        getContentPane().setLayout(gridBagLayout);
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.fill = GridBagConstraints.VERTICAL;
        gridBagConstraints.insets = new Insets(1, 1, 1, 1);
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER;
        JLabel labDepth = new JLabel(Messages.getString("SeaMineDepthDialog.labDepth"), SwingConstants.RIGHT);
        gridBagLayout.setConstraints(labDepth, gridBagConstraints);
        getContentPane().add(labDepth);
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagLayout.setConstraints(choDepth, gridBagConstraints);
        getContentPane().add(choDepth);
        gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = GridBagConstraints.CENTER;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagLayout.setConstraints(butOk, gridBagConstraints);
        getContentPane().add(butOk);
        pack();
        setLocation(p.getLocation().x + p.getSize().width / 2 - getSize().width / 2,
              p.getLocation().y + p.getSize().height / 2 - getSize().height / 2);
    }

    public int getDepth() {
        return depth;
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        if (actionEvent.getSource() == butOk) {
            depth = MathUtility.parseInt((String) choDepth.getSelectedItem(), 0);
        }
        this.setVisible(false);
    }
}
