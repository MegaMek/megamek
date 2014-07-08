/*
 * MegaMek - Copyright (C) 2000,2001,2002,2003,2004 Ben Mazur (bmazur@sev.org)
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

/*
 * CustomMechDialog.java
 *
 * Created on March 18, 2002, 2:56 PM
 */

package megamek.client.ui.swing;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import megamek.client.Client;
import megamek.client.ui.Messages;
import megamek.common.IPlayer;

/**
 * A dialog that can be used to adjust advanced player settings like initiative,
 * minefields, and maybe other things in the future like force abilities.
 * 
 * @author Jay Lawson
 */
public class PlayerSettingsDialog extends ClientDialog implements ActionListener {

    /**
     * 
     */
    private static final long serialVersionUID = -4597870528499580517L;

    private Client client;
    private ClientGUI clientgui;

    private JPanel panMain = new JPanel();
    private JPanel panButtons = new JPanel();

    private JLabel labInit = new JLabel(Messages.getString("PlayerSettingsDialog.ConstantBonus"));
    private JTextField texInit = new JTextField(3);
    private JLabel labMines = new JLabel(Messages.getString("PlayerSettingsDialog.Minefields"));
    private JLabel labConventional = new JLabel(Messages.getString("PlayerSettingsDialog.labConventional"), SwingConstants.RIGHT); //$NON-NLS-1$
    //private JLabel labCommandDetonated = new JLabel(Messages.getString("PlayerSettingsDialog.labCommandDetonated"), SwingConstants.RIGHT); //$NON-NLS-1$
    private JLabel labVibrabomb = new JLabel(Messages.getString("PlayerSettingsDialog.labVibrabomb"), SwingConstants.RIGHT); //$NON-NLS-1$
    private JLabel labActive = new JLabel(Messages.getString("PlayerSettingsDialog.labActive"), SwingConstants.RIGHT); //$NON-NLS-1$
    private JLabel labInferno = new JLabel(Messages.getString("PlayerSettingsDialog.labInferno"), SwingConstants.RIGHT); //$NON-NLS-1$

    private JTextField fldConventional = new JTextField(3);
    // private JTextField fldCommandDetonated = new JTextField(1);
    private JTextField fldVibrabomb = new JTextField(3);
    private JTextField fldActive = new JTextField(3);
    private JTextField fldInferno = new JTextField(3);

    private JButton butOkay = new JButton(Messages.getString("Okay")); //$NON-NLS-1$
    private JButton butCancel = new JButton(Messages.getString("Cancel")); //$NON-NLS-1$

    public PlayerSettingsDialog(ClientGUI clientgui, Client client) {

        super(clientgui.frame, Messages.getString("PlayerSettingsDialog.title"), true); //$NON-NLS-1$

        this.client = client;
        this.clientgui = clientgui;

        setUpMain();

        panButtons.setLayout(new FlowLayout(FlowLayout.CENTER));
        panButtons.add(butOkay);
        butOkay.addActionListener(this);
        panButtons.add(butCancel);
        butCancel.addActionListener(this);

        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        getContentPane().setLayout(gridbag);

        c.fill = GridBagConstraints.NONE;
        c.insets = new Insets(5, 5, 5, 5);
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 1.0;
        c.weighty = 1.0;
        c.anchor = GridBagConstraints.CENTER;
        gridbag.setConstraints(panMain, c);
        getContentPane().add(panMain);

        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(1, 1, 1, 1);
        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 1.0;
        c.weighty = 0.0;
        c.anchor = GridBagConstraints.CENTER;
        gridbag.setConstraints(panButtons, c);
        getContentPane().add(panButtons);

        setResizable(true);
        validate();
        pack();

    }

    private void setUpMain() {

        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        panMain.setLayout(gridbag);

        refreshValues();

        c.fill = GridBagConstraints.NONE;
        c.insets = new Insets(1, 1, 1, 1);
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.anchor = GridBagConstraints.WEST;
        gridbag.setConstraints(labInit, c);
        panMain.add(labInit);

        c.fill = GridBagConstraints.NONE;
        c.insets = new Insets(1, 1, 1, 1);
        c.gridx = 1;
        c.gridy = 0;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.anchor = GridBagConstraints.NORTHWEST;
        gridbag.setConstraints(texInit, c);
        panMain.add(texInit);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(1, 1, 1, 1);
        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 2;
        c.gridheight = 1;
        c.weightx = 1.0;
        c.weighty = 0.0;
        c.anchor = GridBagConstraints.WEST;
        gridbag.setConstraints(labMines, c);
        panMain.add(labMines);

        c.fill = GridBagConstraints.NONE;
        c.insets = new Insets(1, 1, 1, 1);
        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.anchor = GridBagConstraints.EAST;
        gridbag.setConstraints(labConventional, c);
        panMain.add(labConventional);

        c.fill = GridBagConstraints.NONE;
        c.insets = new Insets(1, 1, 1, 1);
        c.gridx = 1;
        c.gridy = 2;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.anchor = GridBagConstraints.NORTHWEST;
        gridbag.setConstraints(fldConventional, c);
        panMain.add(fldConventional);

        c.fill = GridBagConstraints.NONE;
        c.insets = new Insets(1, 1, 1, 1);
        c.gridx = 0;
        c.gridy = 3;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.anchor = GridBagConstraints.EAST;
        gridbag.setConstraints(labVibrabomb, c);
        panMain.add(labVibrabomb);

        c.fill = GridBagConstraints.NONE;
        c.insets = new Insets(1, 1, 1, 1);
        c.gridx = 1;
        c.gridy = 3;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.anchor = GridBagConstraints.NORTHWEST;
        gridbag.setConstraints(fldVibrabomb, c);
        panMain.add(fldVibrabomb);

        c.fill = GridBagConstraints.NONE;
        c.insets = new Insets(1, 1, 1, 1);
        c.gridx = 0;
        c.gridy = 4;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.anchor = GridBagConstraints.EAST;
        gridbag.setConstraints(labActive, c);
        panMain.add(labActive);

        c.fill = GridBagConstraints.NONE;
        c.insets = new Insets(1, 1, 1, 1);
        c.gridx = 1;
        c.gridy = 4;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.anchor = GridBagConstraints.NORTHWEST;
        gridbag.setConstraints(fldActive, c);
        panMain.add(fldActive);

        c.fill = GridBagConstraints.NONE;
        c.insets = new Insets(1, 1, 1, 1);
        c.gridx = 0;
        c.gridy = 5;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.anchor = GridBagConstraints.EAST;
        gridbag.setConstraints(labInferno, c);
        panMain.add(labInferno);

        c.fill = GridBagConstraints.NONE;
        c.insets = new Insets(1, 1, 1, 1);
        c.gridx = 1;
        c.gridy = 5;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.anchor = GridBagConstraints.NORTHWEST;
        gridbag.setConstraints(fldInferno, c);
        panMain.add(fldInferno);

    }

    private void refreshValues() {
        IPlayer player = client.getLocalPlayer();
        texInit.setText(Integer.toString(player.getConstantInitBonus()));
        fldConventional.setText(Integer.toString(player.getNbrMFConventional()));
        fldVibrabomb.setText(Integer.toString(player.getNbrMFVibra()));
        fldActive.setText(Integer.toString(player.getNbrMFActive()));
        fldInferno.setText(Integer.toString(player.getNbrMFInferno()));
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource().equals(butOkay)) {

            String init = texInit.getText();
            int initB = 0;
            try {
                if ((init != null) && (init.length() != 0)) {
                    initB = Integer.parseInt(init);
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(clientgui.frame, Messages.getString("PlayerSettingsDialog.ConstantInitAlert.message"), Messages.getString("PlayerSettingsDialog.ConstantInitAlert.title"), JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$ //$NON-NLS-2$
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
                JOptionPane.showMessageDialog(clientgui.frame, Messages.getString("PlayerSettingsDialog.MinefieldAlert.message"), Messages.getString("PlayerSettingsDialog.MinefieldAlert.title"), JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$ //$NON-NLS-2$
                return;
            }

            if ((nbrConv < 0) || (nbrVibra < 0) || (nbrActive < 0) || (nbrInferno < 0)) {
                JOptionPane.showMessageDialog(clientgui.frame, Messages.getString("PlayerSettingsDialog.MinefieldAlert.message"), Messages.getString("PlayerSettingsDialog.MinefieldAlert.title"), JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$ //$NON-NLS-2$
                return;
            }
            client.getLocalPlayer().setNbrMFConventional(nbrConv);
            client.getLocalPlayer().setNbrMFVibra(nbrVibra);
            client.getLocalPlayer().setNbrMFActive(nbrActive);
            client.getLocalPlayer().setNbrMFInferno(nbrInferno);

            client.sendPlayerInfo();
            setVisible(false);
        } else if (e.getSource().equals(butCancel)) {
            setVisible(false);
        }

    }
}