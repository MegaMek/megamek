/*
 * MegaMek - Copyright (C) 2002, 2003 Ben Mazur (bmazur@sev.org)
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

/**
 * The random names dialog allows the player to randomly assign names to pilots based on faction and gender.
 *
 */
package megamek.client.ui.swing;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionListener;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingConstants;

import megamek.client.Client;
import megamek.client.RandomNameGenerator;
import megamek.client.ui.Messages;
import megamek.common.Entity;

public class RandomNameDialog extends JDialog implements ActionListener {

    private static final long serialVersionUID = -2459992981678758743L;
    private Client client;
    private ClientGUI clientgui;
    private Vector<Entity> units;
    private RandomNameGenerator rng;

    private JLabel lblFaction;
    private JLabel lblGender;
    private JComboBox comboFaction;
    private JSlider sldGender;

    private JButton butOkay;
    private JButton butSave;
    private JButton butCancel;

    private JPanel panButtons;
    private JPanel panMain;

    private JComboBox chPlayer;

    public RandomNameDialog(ClientGUI clientgui) {
        super(clientgui.frame, Messages.getString("RandomNameDialog.title"), true); //$NON-NLS-1$
        this.clientgui = clientgui;
        init();
    }

    private void init() {

        initComponents();

        client = clientgui.getClient();
        rng = client.getRandomNameGenerator();

        updateFactions();

        updatePlayerChoice();

        butOkay.addActionListener(this);
        butSave.addActionListener(this);
        butCancel.addActionListener(this);
        chPlayer.addActionListener(this);
        setLocationRelativeTo(clientgui.frame);

    }

    private void updateFactions() {
        //Fill the combobox with choices
        Iterator<String> factions = rng.getFactions();
        if(null == factions) {
            return;
        }
        while(factions.hasNext()) {
            String faction = factions.next();
            comboFaction.addItem(faction);
        }
        comboFaction.setSelectedItem(rng.getChosenFaction());

    }

    private void updatePlayerChoice() {
        String lastChoice = (String) chPlayer.getSelectedItem();
        String clientName = clientgui.getClient().getName();
        chPlayer.removeAllItems();
        chPlayer.setEnabled(true);
        chPlayer.addItem(clientName);
        for (Iterator<Client> i = clientgui.getBots().values().iterator(); i.hasNext();) {
            chPlayer.addItem(i.next().getName());
        }
        if (chPlayer.getItemCount() == 1) {
            chPlayer.setEnabled(false);
        }
        chPlayer.setSelectedItem(lastChoice);
        if (chPlayer.getSelectedIndex() < 0) {
            chPlayer.setSelectedIndex(0);
        }
        comboFaction.setSelectedItem(rng.getChosenFaction());
        sldGender.setValue(rng.getPercentFemale());
    }

    private void saveSettings() {
        rng.setChosenFaction((String)comboFaction.getSelectedItem());
        rng.setPerentFemale(sldGender.getValue());
    }

    @Override
    public void setVisible(boolean show) {
        if (show) {
            updateFactions();
            updatePlayerChoice();
        }
        super.setVisible(show);
    }

    public  void showDialog(Vector<Entity> units){
        this.units=units;
        setVisible(true);
    }

    public  void showDialog(Entity unit){
         Vector<Entity> units=new Vector<Entity>();
         units.add(unit);
         showDialog(units);
    }

    public void actionPerformed(java.awt.event.ActionEvent ev) {
        if (ev.getSource() == butOkay) {
            Client c = null;
            if (chPlayer.getSelectedIndex() > 0) {
                String name = (String) chPlayer.getSelectedItem();
                c = clientgui.getBots().get(name);
            }
            if (c == null) {
                c = client;
            }
            saveSettings();
            // go through all of the units provided for this player and assign random names
            for (Enumeration<Entity> e = units.elements(); e.hasMoreElements();) {
                Entity ent = e.nextElement();
                if (ent.getOwnerId() == c.getLocalPlayer().getId()) {
                    ent.getCrew().setName(rng.generate());
                    c.sendUpdateEntity(ent);
                }
            }
            clientgui.chatlounge.refreshEntities();
            // need to notify about customization
            // not updating entities in server
            setVisible(false);
        }
        if(ev.getSource() == butSave) {
            saveSettings();
            setVisible(false);
        }

        if (ev.getSource() == butCancel) {
            setVisible(false);
        }
        if(ev.getSource() == chPlayer) {
            Client c = client;
            if (chPlayer.getSelectedIndex() > 0) {
                String name = (String) chPlayer.getSelectedItem();
                c = clientgui.getBots().get(name);
            }
            if(null == c) {
                c = client;
            }
            rng = c.getRandomNameGenerator();
            updatePlayerChoice();
        }
    }

    public void setClient(Client client) {
        this.client = client;
    }

    private void initComponents() {

        panButtons = new JPanel();
        butOkay = new JButton(Messages.getString("RandomSkillDialog.Okay"));
        butSave = new JButton(Messages.getString("RandomSkillDialog.Save"));
        butCancel = new JButton(Messages.getString("Cancel"));
        panMain = new JPanel();
        lblFaction = new JLabel(Messages.getString("RandomNameDialog.lblFaction"));
        lblGender = new JLabel(Messages.getString("RandomNameDialog.lblGender"));
        comboFaction = new JComboBox();
        sldGender = new JSlider(SwingConstants.HORIZONTAL, 0, 100, 50);
        sldGender.setMajorTickSpacing(25);
        sldGender.setPaintTicks(true);
        sldGender.setPaintLabels(true);
        chPlayer = new JComboBox();
        chPlayer.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        panButtons.add(butOkay);
        panButtons.add(butSave);
        panButtons.add(butCancel);
        panButtons.add(chPlayer);

        getContentPane().add(panButtons, java.awt.BorderLayout.PAGE_END);

        panMain.setLayout(new GridBagLayout());

        GridBagConstraints c;
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.weightx = 1.0;
        c.weighty = 1.0;
        panMain.add(lblFaction, c);

        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 0;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.weightx = 1.0;
        c.weighty = 1.0;
        panMain.add(comboFaction, c);

        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.weightx = 1.0;
        c.weighty = 1.0;
        panMain.add(lblGender, c);

        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 1;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.weightx = 1.0;
        c.weighty = 1.0;
        panMain.add(sldGender, c);

        getContentPane().add(panMain, java.awt.BorderLayout.PAGE_START);

        pack();
    }
}
