/*
 * MegaMek - Copyright (C) 2000,2001,2002,2003,2004 Ben Mazur (bmazur@sev.org)
 * MegaMek - Copyright (C) 2020 - The MegaMek Team 
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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import megamek.client.Client;
import megamek.client.ui.Messages;
import megamek.common.IPlayer;
import megamek.common.IGame.Phase;

/**
 * A dialog that can be used to adjust advanced player settings like initiative,
 * minefields, and maybe other things in the future like force abilities.
 * 
 * @author Jay Lawson
 */
public class PlayerSettingsDialog extends ClientDialog implements ActionListener {

    private static final long serialVersionUID = -4597870528499580517L;

    private Client client;
    private ClientGUI clientgui;

    private JLabel labInit = new JLabel(
            Messages.getString("PlayerSettingsDialog.ConstantBonus")); //$NON-NLS-1$
    private JTextField texInit = new JTextField(3);
    private JLabel labMines = new JLabel(
            Messages.getString("PlayerSettingsDialog.Minefields"), //$NON-NLS-1$
            SwingConstants.CENTER);
    private JLabel labConventional = new JLabel(
            Messages.getString("PlayerSettingsDialog.labConventional"), //$NON-NLS-1$ 
            SwingConstants.RIGHT); 
    private JLabel labVibrabomb = new JLabel(
            Messages.getString("PlayerSettingsDialog.labVibrabomb"), //$NON-NLS-1$
            SwingConstants.RIGHT); 
    private JLabel labActive = new JLabel(
            Messages.getString("PlayerSettingsDialog.labActive"), //$NON-NLS-1$
            SwingConstants.RIGHT); 
    private JLabel labInferno = new JLabel(
            Messages.getString("PlayerSettingsDialog.labInferno"), //$NON-NLS-1$
            SwingConstants.RIGHT); 

    private JTextField fldConventional = new JTextField(3);
    private JTextField fldVibrabomb = new JTextField(3);
    private JTextField fldActive = new JTextField(3);
    private JTextField fldInferno = new JTextField(3);

    public PlayerSettingsDialog(ClientGUI clientgui, Client client) {

        super(clientgui.frame, 
                Messages.getString("PlayerSettingsDialog.title"), true); //$NON-NLS-1$

        this.client = client;
        this.clientgui = clientgui;
        
        fillInValues();

        // The main options 
        JPanel panMain = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        
        JLabel labPlayer = new JLabel(
                Messages.getString(
                        "PlayerSettingsDialog.Player",
                        client.getLocalPlayer().getName()), 
                SwingConstants.CENTER);
        labPlayer.setBorder(BorderFactory.createEmptyBorder(15, 5, 5, 5));
        add(labPlayer, BorderLayout.PAGE_START);
        
        c.fill = GridBagConstraints.NONE;
        c.insets = new Insets(5, 5, 5, 5);
        c.gridheight = 1;
        c.weightx = 0.0;
        c.weighty = 0.0;
        
        addOptionRow(panMain, c, labInit, texInit);
        
        c.gridwidth = 2;
        c.anchor = GridBagConstraints.CENTER;
        c.gridwidth = GridBagConstraints.REMAINDER;
        panMain.add(labMines, c);
        
        addOptionRow(panMain, c, labConventional, fldConventional);
        addOptionRow(panMain, c, labVibrabomb, fldVibrabomb);
        addOptionRow(panMain, c, labActive, fldActive);
        addOptionRow(panMain, c, labInferno, fldInferno);
        
        // Disable changing minefields mid-game
        if (client.getGame().getPhase() != Phase.PHASE_LOUNGE) {
            fldConventional.setEnabled(false);
            fldVibrabomb.setEnabled(false);
            fldActive.setEnabled(false);
            fldInferno.setEnabled(false);
        }
        add(panMain, BorderLayout.CENTER);

        // Buttons
        JPanel panButtons = new JPanel(new FlowLayout());
        panButtons.add(new JButton(new OkayAction(this)));
        panButtons.add(new ButtonEsc(new CancelAction(this)));
        add(panButtons, BorderLayout.PAGE_END);

        setMinimumSize(new Dimension(300,260));
        setResizable(false);
        center();
        validate();
        pack();
    }

    private void fillInValues() {
        IPlayer player = client.getLocalPlayer();
        texInit.setText(Integer.toString(player.getConstantInitBonus()));
        fldConventional.setText(Integer.toString(player.getNbrMFConventional()));
        fldVibrabomb.setText(Integer.toString(player.getNbrMFVibra()));
        fldActive.setText(Integer.toString(player.getNbrMFActive()));
        fldInferno.setText(Integer.toString(player.getNbrMFInferno()));
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().equals(OkayAction.OKAY)) {
            String init = texInit.getText();
            int initB = 0;
            try {
                if ((init != null) && (init.length() != 0)) {
                    initB = Integer.parseInt(init);
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(clientgui.frame, 
                        Messages.getString("PlayerSettingsDialog.ConstantInitAlert.message"), //$NON-NLS-1$
                        Messages.getString("PlayerSettingsDialog.ConstantInitAlert.title"), //$NON-NLS-1$
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
            client.getLocalPlayer().setConstantInitBonus(initB);

            String conv = fldConventional.getText();
            String vibra = fldVibrabomb.getText();
            String active = fldActive.getText();
            String inferno = fldInferno.getText();

            int nbrConv = 0;
            int nbrVibra = 0;
            int nbrActive = 0;
            int nbrInferno = 0;

            try {
                if ((conv != null) && (conv.length() != 0)) {
                    nbrConv = Integer.parseInt(conv);
                }
                if ((vibra != null) && (vibra.length() != 0)) {
                    nbrVibra = Integer.parseInt(vibra);
                }
                if ((active != null) && (active.length() != 0)) {
                    nbrActive = Integer.parseInt(active);
                }
                if ((inferno != null) && (inferno.length() != 0)) {
                    nbrInferno = Integer.parseInt(inferno);
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(clientgui.frame, 
                        Messages.getString("PlayerSettingsDialog.MinefieldAlert.message"), //$NON-NLS-1$
                        Messages.getString("PlayerSettingsDialog.MinefieldAlert.title"), //$NON-NLS-1$
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            if ((nbrConv < 0) || (nbrVibra < 0) || (nbrActive < 0) || (nbrInferno < 0)) {
                JOptionPane.showMessageDialog(clientgui.frame, 
                        Messages.getString("PlayerSettingsDialog.MinefieldAlert.message"), //$NON-NLS-1$
                        Messages.getString("PlayerSettingsDialog.MinefieldAlert.title"), //$NON-NLS-1$
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
            client.getLocalPlayer().setNbrMFConventional(nbrConv);
            client.getLocalPlayer().setNbrMFVibra(nbrVibra);
            client.getLocalPlayer().setNbrMFActive(nbrActive);
            client.getLocalPlayer().setNbrMFInferno(nbrInferno);

            client.sendPlayerInfo();
            setVisible(false);
        } 
    }
}