/*
 * MegaMek - Copyright (C) 2000-2011 Ben Mazur (bmazur@sev.org)
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
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import megamek.client.ui.Messages;
import megamek.common.Player;
import megamek.common.PlayerImpl;

/**
 * Allow a user to set types and colors for scenario players
 */
public class ScenarioDialog extends JDialog implements ActionListener {

    private static final long serialVersionUID = -5682593522064612790L;
    private static final int T_ME = 0;
    public static final int T_BOT = 2;
    public static final int T_OBOT = 3;
    private PlayerImpl[] m_players;
    private JLabel[] m_labels;
    private JComboBox[] m_typeChoices;
    private JButton[] m_camoButtons;
    private JFrame m_frame;

    public boolean bSet;
    public int[] playerTypes;
    public String localName = ""; //$NON-NLS-1$

    public ScenarioDialog(final JFrame frame, PlayerImpl[] pa) {
        super(frame, Messages.getString("MegaMek.ScenarioDialog.title"), true); //$NON-NLS-1$
        m_frame = frame;
        m_players = pa;
        m_labels = new JLabel[pa.length];
        m_typeChoices = new JComboBox[pa.length];
        m_camoButtons = new JButton[pa.length];
        playerTypes = new int[pa.length];
        for (int x = 0; x < pa.length; x++) {
            final Player curPlayer = m_players[x];
            curPlayer.setColorIndex(x);
            m_labels[x] = new JLabel(pa[x].getName(), SwingConstants.LEFT);
            m_typeChoices[x] = new JComboBox();
            m_typeChoices[x].addItem(Messages
                    .getString("MegaMek.ScenarioDialog.me")); //$NON-NLS-1$
            m_typeChoices[x].addItem(Messages
                    .getString("MegaMek.ScenarioDialog.otherh")); //$NON-NLS-1$
            m_typeChoices[x].addItem(Messages
                    .getString("MegaMek.ScenarioDialog.bot")); //$NON-NLS-1$
            m_typeChoices[x].addItem(Messages
                    .getString("MegaMek.ScenarioDialog.otherbot")); //$NON-NLS-1$
            m_camoButtons[x] = new JButton();
            final JButton curButton = m_camoButtons[x];
            curButton.setText(Messages.getString("MegaMek.NoCamoBtn")); //$NON-NLS-1$
            curButton.setPreferredSize(new Dimension(84, 72));
            final CamoChoiceDialog dialog = new CamoChoiceDialog(frame,
                    curButton);
            dialog.setPlayer(curPlayer);
            curButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    dialog.setVisible(true);
                }
            });
        }
        getContentPane().setLayout(new BorderLayout());
        JPanel choicePanel = new JPanel();
        choicePanel.setLayout(new GridLayout(pa.length + 1, 0));
        choicePanel.add(new JLabel(Messages
                .getString("MegaMek.ScenarioDialog.pNameType"))); //$NON-NLS-1$
        choicePanel.add(new JLabel(Messages
                .getString("MegaMek.ScenarioDialog.Camo"))); //$NON-NLS-1$
        for (int x = 0; x < pa.length; x++) {
            JPanel typePanel = new JPanel();
            typePanel.setLayout(new GridLayout(0, 1));
            typePanel.add(m_labels[x]);
            typePanel.add(m_typeChoices[x]);
            choicePanel.add(typePanel);
            choicePanel.add(m_camoButtons[x]);
        }
        getContentPane().add(choicePanel, BorderLayout.CENTER);
        JPanel butPanel = new JPanel();
        butPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        JButton bOkay = new JButton(Messages.getString("Okay")); //$NON-NLS-1$
        bOkay.setActionCommand("okay"); //$NON-NLS-1$
        bOkay.addActionListener(this);
        JButton bCancel = new JButton(Messages.getString("Cancel")); //$NON-NLS-1$
        bCancel.setActionCommand("cancel"); //$NON-NLS-1$
        bCancel.addActionListener(this);
        butPanel.add(bOkay);
        butPanel.add(bCancel);
        getContentPane().add(butPanel, BorderLayout.SOUTH);
        pack();
        setResizable(false);
        setLocation((frame.getLocation().x + (frame.getSize().width / 2))
                - (getSize().width / 2), (frame.getLocation().y
                + (frame.getSize().height / 2)) - (getSize().height / 2));
    }

    public void actionPerformed(ActionEvent e) {
        if ("okay".equals(e.getActionCommand())) { //$NON-NLS-1$
            boolean bMeSet = false;
            for (int x = 0; x < m_players.length; x++) {
                playerTypes[x] = m_typeChoices[x].getSelectedIndex();
                if (playerTypes[x] == T_ME) {
                    if (bMeSet) {
                        JOptionPane
                                .showMessageDialog(
                                        m_frame,
                                        Messages
                                                .getString("MegaMek.ScenarioErrorAlert.message"),
                                        Messages
                                                .getString("MegaMek.ScenarioErrorAlert.title"),
                                        JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    bMeSet = true;
                    localName = m_players[x].getName();
                }
            }
            bSet = true;
            setVisible(false);
        } else if ("cancel".equals(e.getActionCommand())) { //$NON-NLS-1$
            setVisible(false);
        }
    }
}