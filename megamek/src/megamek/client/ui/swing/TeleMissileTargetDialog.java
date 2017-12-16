/*
 * MegaMek - Copyright (C) 2000-2003 Ben Mazur (bmazur@sev.org)
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

package megamek.client.ui.swing;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;

import javax.swing.Box;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;

import megamek.client.Client;
import megamek.client.ui.Messages;
import megamek.common.IPlayer;
import megamek.common.Team;

/**
 * Target selector for a bearings-only teleoperated missile.
 */
public class TeleMissileTargetDialog extends JDialog implements ActionListener {
    /**
     *
     */
    private static final long serialVersionUID = 6527373019065650613L;
    private JButton butOk = new JButton(
            Messages.getString("TeleMissileTargetDialog.butOk")); //$NON-NLS-1$
    private JList<String> targetList = new JList<String>(
            new DefaultListModel<String>());

    private ClientGUI clientgui;

    public TeleMissileTargetDialog(ClientGUI cg, List<String> targets) {
        super(cg.frame, Messages.getString("TeleMissileTargetDialog.title"), false); //$NON-NLS-1$
        clientgui = cg;

        butOk.addActionListener(this);

        // layout
        getContentPane().setLayout(new BorderLayout());

        JPanel listPan = new JPanel();
        listPan.setBackground(Color.white);
        targetList.setBackground(Color.white);
        listPan.add(targetList, BorderLayout.NORTH);
        listPan.add(Box.createVerticalStrut(40), BorderLayout.SOUTH);

        getContentPane().add(listPan, BorderLayout.NORTH);
        getContentPane().add(Box.createHorizontalStrut(20), BorderLayout.WEST);
        getContentPane().add(Box.createHorizontalStrut(20), BorderLayout.EAST);
        getContentPane().add(butOk, BorderLayout.SOUTH);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                setVisible(false);
            }
        });

        pack();
        setResizable(false);
        setLocation((cg.getLocation().x + (cg.getSize().width / 2))
                - (getSize().width / 2),
                (cg.getLocation().y + (cg.getSize().height / 2))
                        - (getSize().height / 2));
    }

    public void actionPerformed(ActionEvent e) {
        setVisible(false);
    }

}