/*
 * Copyright (C) 2000-2011 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2006-2025 The MegaMek Team. All Rights Reserved.
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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serial;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import megamek.client.ui.Messages;
import megamek.client.ui.dialogs.iconChooser.CamoChooserDialog;
import megamek.client.ui.util.PlayerColour;
import megamek.common.Player;

/**
 * Allow a user to set types and colors for scenario players
 */
public class ScenarioDialog extends JDialog implements ActionListener {

    @Serial
    private static final long serialVersionUID = -5682593522064612790L;
    private static final int T_ME = 0;
    public static final int T_BOT = 2;
    public static final int T_OTHER_BOT = 3;
    private final Player[] m_players;
    @SuppressWarnings("rawtypes")
    private final JComboBox[] m_typeChoices;
    private final JFrame m_frame;

    public boolean bSet;
    public int[] playerTypes;
    public String localName = "";

    @SuppressWarnings("unchecked")
    public ScenarioDialog(final JFrame frame, Player[] pa) {
        super(frame, Messages.getString("MegaMek.ScenarioDialog.title"), true);
        m_frame = frame;
        m_players = pa;
        JLabel[] m_labels = new JLabel[pa.length];
        m_typeChoices = new JComboBox[pa.length];
        JButton[] m_camoButtons = new JButton[pa.length];
        playerTypes = new int[pa.length];
        final PlayerColour[] colours = PlayerColour.values();
        for (int x = 0; x < pa.length; x++) {
            final Player curPlayer = m_players[x];
            curPlayer.setColour(colours[x % colours.length]);
            m_labels[x] = new JLabel(pa[x].getName(), SwingConstants.LEFT);
            m_typeChoices[x] = new JComboBox<String>();
            m_typeChoices[x].addItem(Messages.getString("MegaMek.ScenarioDialog.me"));
            m_typeChoices[x].addItem(Messages.getString("MegaMek.ScenarioDialog.otherh"));
            m_typeChoices[x].addItem(Messages.getString("MegaMek.ScenarioDialog.bot"));
            //            m_typeChoices[x].addItem(Messages.getString("MegaMek.ScenarioDialog.otherbot"));
            m_camoButtons[x] = new JButton();
            final JButton curButton = m_camoButtons[x];
            curButton.setText(Messages.getString("MegaMek.NoCamoBtn"));
            curButton.setPreferredSize(new Dimension(84, 72));
            curButton.addActionListener(e -> {
                final CamoChooserDialog ccd = new CamoChooserDialog(frame, curPlayer.getCamouflage());
                try {
                    if (ccd.showDialog().isConfirmed()) {
                        curPlayer.setCamouflage(ccd.getSelectedItem());
                        curButton.setIcon(curPlayer.getCamouflage().getImageIcon());
                    }
                } finally {
                    ccd.dispose();
                }
            });
        }
        getContentPane().setLayout(new BorderLayout());
        JPanel choicePanel = new JPanel();
        choicePanel.setLayout(new GridLayout(pa.length + 1, 0));
        choicePanel.add(new JLabel(Messages.getString("MegaMek.ScenarioDialog.pNameType")));
        choicePanel.add(new JLabel(Messages.getString("MegaMek.ScenarioDialog.Camo")));
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
        JButton bOkay = new JButton(Messages.getString("Okay"));
        bOkay.setActionCommand("okay");
        bOkay.addActionListener(this);
        JButton bCancel = new JButton(Messages.getString("Cancel"));
        bCancel.setActionCommand("cancel");
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

    @Override
    public void actionPerformed(ActionEvent e) {
        if ("okay".equals(e.getActionCommand())) {
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
        } else if ("cancel".equals(e.getActionCommand())) {
            setVisible(false);
        }
    }
}
