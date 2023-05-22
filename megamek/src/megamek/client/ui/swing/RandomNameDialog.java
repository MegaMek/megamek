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
package megamek.client.ui.swing;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import javax.swing.*;

import megamek.client.Client;
import megamek.client.generator.RandomGenderGenerator;
import megamek.client.generator.RandomNameGenerator;
import megamek.client.ui.Messages;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.Entity;
import megamek.common.enums.Gender;

/**
 * The random names dialog allows the player to randomly assign names to pilots based on faction and gender.
 */
public class RandomNameDialog extends JDialog implements ActionListener {
    private static final long serialVersionUID = -2459992981678758743L;
    private Client client;
    private ClientGUI clientgui;
    private List<Entity> units;

    private JComboBox<String> comboFaction;
    private JSlider sldGender;
    private JComboBox<String> comboHistoricalEthnicity;

    private JButton butOkay;
    private JButton butSave;
    private JButton butCancel;

    private JComboBox<String> chPlayer;

    public RandomNameDialog(ClientGUI clientgui) {
        super(clientgui.frame, Messages.getString("RandomNameDialog.title"), true);
        this.clientgui = clientgui;
        init();
    }

    private void init() {
        initComponents();

        client = clientgui.getClient();

        updateFactions();

        updateHistoricalEthnicities();

        updatePlayerChoice();

        butOkay.addActionListener(this);
        butSave.addActionListener(this);
        butCancel.addActionListener(this);
        chPlayer.addActionListener(this);
        setLocationRelativeTo(clientgui.frame);
    }

    private void updateFactions() {
        //Fill the combobox with choices
        Set<String> factions = RandomNameGenerator.getInstance().getFactions();
        if (null == factions) {
            return;
        }
        comboFaction.removeAllItems();
        for (String faction : factions) {
            comboFaction.addItem(faction);
        }
        comboFaction.setSelectedItem(RandomNameGenerator.getInstance().getChosenFaction());
    }

    private void updateHistoricalEthnicities() {
        DefaultComboBoxModel<String> historicalEthnicityModel = new DefaultComboBoxModel<>();
        historicalEthnicityModel.addElement("Faction Weighted");
        for (String value : RandomNameGenerator.getInstance().getHistoricalEthnicity().values()) {
            historicalEthnicityModel.addElement(value);
        }
        comboHistoricalEthnicity.setModel(historicalEthnicityModel);
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

        comboFaction.setSelectedItem(RandomNameGenerator.getInstance().getChosenFaction());
        sldGender.setValue(RandomGenderGenerator.getPercentFemale());
        comboHistoricalEthnicity.setSelectedIndex(0);
    }

    private void saveSettings() {
        RandomNameGenerator.getInstance().setChosenFaction((String) comboFaction.getSelectedItem());
        RandomGenderGenerator.setPercentFemale(sldGender.getValue());
    }

    public void showDialog(List<Entity> units) {
        this.units = units;
        setVisible(true);
    }

    public void showDialog(Entity unit) {
         Vector<Entity> units = new Vector<>();
         units.add(unit);
         showDialog(units);
    }

    @Override
    public void actionPerformed(ActionEvent ev) {
        if (ev.getSource().equals(butOkay)) {
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
            for (Entity ent : units) {
                if (ent.getOwnerId() == c.getLocalPlayer().getId()) {
                    for (int i = 0; i < ent.getCrew().getSlotCount(); i++) {
                        Gender gender = RandomGenderGenerator.generate();
                        ent.getCrew().setGender(gender, i);
                        if (comboHistoricalEthnicity.getSelectedIndex() == 0) {
                            ent.getCrew().setName(RandomNameGenerator.getInstance().generate(gender,
                                    ent.getCrew().isClanPilot(i)), i);
                        } else {
                            ent.getCrew().setName(RandomNameGenerator.getInstance().generateWithEthnicCode(
                                    gender, ent.getCrew().isClanPilot(i),
                                    comboHistoricalEthnicity.getSelectedIndex()), i);
                        }
                    }
                    c.sendUpdateEntity(ent);
                }
            }
            clientgui.chatlounge.refreshEntities();
            // need to notify about customization not updating entities in server
            setVisible(false);
        } else if (ev.getSource().equals(butSave)) {
            saveSettings();
            setVisible(false);
        } else if (ev.getSource().equals(butCancel)) {
            setVisible(false);
        } else if (ev.getSource().equals(chPlayer)) {
            updatePlayerChoice();
        }
    }

    public void setClient(Client client) {
        this.client = client;
    }

    private void initComponents() {
        JPanel panButtons = new JPanel();
        butOkay = new JButton(Messages.getString("Randomize.text"));
        butSave = new JButton(Messages.getString("Save.text"));
        butCancel = new JButton(Messages.getString("Cancel.text"));
        JPanel panMain = new JPanel();
        JLabel lblFaction = new JLabel(Messages.getString("RandomNameDialog.lblFaction"));
        JLabel lblGender = new JLabel(Messages.getString("RandomNameDialog.lblGender"));
        comboFaction = new JComboBox<>();
        sldGender = new JSlider(SwingConstants.HORIZONTAL, 0, 100, 50);
        sldGender.setMajorTickSpacing(25);
        sldGender.setPaintTicks(true);
        sldGender.setPaintLabels(true);
        comboHistoricalEthnicity = new JComboBox<>();
        JLabel lblHistoricalEthnicity = new JLabel(Messages.getString("RandomNameDialog.lblHistoricalEthnicity"));
        lblHistoricalEthnicity.setToolTipText(Messages.getString("RandomNameDialog.lblHistoricalEthnicity.toolTipText"));

        chPlayer = new JComboBox<>();
        chPlayer.setModel(new javax.swing.DefaultComboBoxModel<>(new String[]{"Item 1", "Item 2", "Item 3", "Item 4"}));

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

        c.gridx = 1;
        panMain.add(comboFaction, c);

        c.gridx = 0;
        c.gridy = 1;
        panMain.add(lblGender, c);

        c.gridx = 1;
        panMain.add(sldGender, c);

        c.gridx = 0;
        c.gridy = 2;
        panMain.add(lblHistoricalEthnicity, c);

        c.gridx = 1;
        panMain.add(comboHistoricalEthnicity, c);

        getContentPane().add(panMain, java.awt.BorderLayout.PAGE_START);

        adaptToGUIScale();

        pack();
    }

    private void adaptToGUIScale() {
        UIUtil.adjustDialog(this, UIUtil.FONT_SCALE1);
    }
}
