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
 * The random skill dialog allows the player to randomly assign skills to pilots based on overall experience level.
 *
 */
package megamek.client.ui.swing;

import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.JDialog;

import megamek.client.Client;
import megamek.client.RandomSkillsGenerator;
import megamek.client.ui.Messages;
import megamek.common.Entity;

public class RandomSkillDialog extends JDialog implements ActionListener,
        ItemListener {

    private static final long serialVersionUID = -2459992981678758743L;
    private Client client;
    private ClientGUI clientgui;
    private Vector<Entity> units;
    private RandomSkillsGenerator rsg;

    /** Creates new form RandomSkillDialog2 */
    public RandomSkillDialog(ClientDialog ui,ClientGUI clientgui) {
        super(ui, Messages.getString("RandomSkillDialog.title"),ModalityType.APPLICATION_MODAL); //$NON-NLS-1$
        this.clientgui = clientgui;
        init();
    }

    public RandomSkillDialog(ClientGUI clientgui) {
        super(clientgui.frame, Messages.getString("RandomSkillDialog.title"), true); //$NON-NLS-1$
        this.clientgui = clientgui;
        init();
    }

    private void init(){

        initComponents();

        client = clientgui.getClient();
        rsg = client.getRandomSkillsGenerator();

        butOkay.setText(Messages.getString("RandomSkillDialog.Okay")); //$NON-NLS-1$
        butSave.setText(Messages.getString("RandomSkillDialog.Save")); //$NON-NLS-1$
        butCancel.setText(Messages.getString("Cancel")); //$NON-NLS-1$
        labelMethod.setText(Messages.getString("RandomSkillDialog.labelMethod")); //$NON-NLS-1$
        labelType.setText(Messages.getString("RandomSkillDialog.labelType")); //$NON-NLS-1$
        labelLevel.setText(Messages.getString("RandomSkillDialog.labelLevel")); //$NON-NLS-1$
        labelPlayer.setText(Messages.getString("MechSelectorDialog.m_labelPlayer")); //$NON-NLS-1$
        texDesc.setText(Messages.getString("CustomMechDialog.texDesc")); //$NON-NLS-1$
        cForceClose.setText(Messages.getString("RandomSkillDialog.cForceClose"));

        texDesc.setLineWrap(true);
        texDesc.setEnabled(true);

        for(int i = 0; i < RandomSkillsGenerator.M_SIZE; i++) {
            chMethod.addItem(RandomSkillsGenerator.getMethodDisplayableName(i));
        }
        chMethod.addItemListener(this);
        texDesc.setText(Messages.getString("RandomSkillDialog.descTW"));

        for(int i = 0; i < RandomSkillsGenerator.T_SIZE; i++) {
            chType.addItem(RandomSkillsGenerator.getTypeDisplayableName(i));
        }

        for(int i = 0; i < RandomSkillsGenerator.L_SIZE; i++) {
            chLevel.addItem(RandomSkillsGenerator.getLevelDisplayableName(i));
        }

        updatePlayerChoice();

        butOkay.addActionListener(this);
        butSave.addActionListener(this);
        butCancel.addActionListener(this);
        chPlayer.addActionListener(this);
        setLocationRelativeTo(clientgui.frame);

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
        chMethod.setSelectedIndex(rsg.getMethod());
        chType.setSelectedIndex(rsg.getType());
        chLevel.setSelectedIndex(rsg.getLevel());
        cForceClose.setSelected(rsg.isClose());
    }

    private void saveSettings() {
        rsg.setMethod(chMethod.getSelectedIndex());
        rsg.setType(chType.getSelectedIndex());
        rsg.setLevel(chLevel.getSelectedIndex());
        rsg.setClose(cForceClose.isSelected());
    }

    @Override
    public void setVisible(boolean show) {
        if (show) {
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
            saveSettings();
            // go through all of the units provided for this player and assign random
            // skill levels
            Client c = null;
            if (chPlayer.getSelectedIndex() > 0) {
                String name = (String) chPlayer.getSelectedItem();
                c = clientgui.getBots().get(name);
            }
            if (c == null) {
                c = client;
            }
            for (Enumeration<Entity> e = units.elements(); e.hasMoreElements();) {
                Entity ent = e.nextElement();
                if (ent.getOwnerId() == c.getLocalPlayer().getId()) {
                    int skills[] = rsg.getRandomSkills(ent);
                    if (cForceClose.isSelected()) {
                        skills[1] = skills[0] + 1;
                    }
                    ent.getCrew().setGunnery(skills[0]);
                    ent.getCrew().setGunneryL(skills[0]);
                    ent.getCrew().setGunneryM(skills[0]);
                    ent.getCrew().setGunneryB(skills[0]);
                    ent.getCrew().setPiloting(skills[1]);
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
            rsg = c.getRandomSkillsGenerator();
            updatePlayerChoice();
        }
    }

    public void itemStateChanged(ItemEvent ie) {
        if (ie.getSource().equals(chMethod)) {
            if (chMethod.getSelectedIndex() == RandomSkillsGenerator.M_TW) {
                texDesc.setText(Messages.getString("RandomSkillDialog.descTW"));
            }
            if (chMethod.getSelectedIndex() == RandomSkillsGenerator.M_TAHARQA) {
                texDesc.setText(Messages.getString("RandomSkillDialog.descTaharqa"));
            }
            if (chMethod.getSelectedIndex() == RandomSkillsGenerator.M_CONSTANT) {
                texDesc.setText(Messages.getString("RandomSkillDialog.descConstant"));
            }

        }
    }

    public void setClient(Client client) {
        this.client = client;
    }




    private void initComponents() {

        panButtons = new javax.swing.JPanel();
        butOkay = new javax.swing.JButton();
        butSave = new javax.swing.JButton();
        butCancel = new javax.swing.JButton();
        labelPlayer = new javax.swing.JLabel();
        chPlayer = new javax.swing.JComboBox();
        jPanel2 = new javax.swing.JPanel();
        labelMethod = new javax.swing.JLabel();
        labelType = new javax.swing.JLabel();
        labelLevel = new javax.swing.JLabel();
        chMethod = new javax.swing.JComboBox();
        chType = new javax.swing.JComboBox();
        chLevel = new javax.swing.JComboBox();
        cForceClose = new javax.swing.JCheckBox();
        jScrollPane1 = new javax.swing.JScrollPane();
        texDesc = new javax.swing.JTextArea();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        butOkay.setText("Generate");
        panButtons.add(butOkay);

        butSave.setText("Save Settings");
        panButtons.add(butSave);

        butCancel.setText("Cancel");
        panButtons.add(butCancel);

        labelPlayer.setText("Player:");
        panButtons.add(labelPlayer);

        chPlayer.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        panButtons.add(chPlayer);

        getContentPane().add(panButtons, java.awt.BorderLayout.PAGE_END);

        labelMethod.setText("Method");

        labelType.setText("Pilot Type");

        labelLevel.setText("Experience");

        cForceClose.setText("Force Piloting to be Gunnery+1");

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                        .addGap(84, 84, 84)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addComponent(labelLevel)
                                .addGap(15, 15, 15))
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addComponent(labelMethod)
                                    .addComponent(labelType))
                                .addGap(18, 18, 18)))
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(chMethod, javax.swing.GroupLayout.Alignment.TRAILING, 0, 238, Short.MAX_VALUE)
                            .addComponent(chType, javax.swing.GroupLayout.Alignment.TRAILING, 0, 238, Short.MAX_VALUE)
                            .addComponent(chLevel, 0, 238, Short.MAX_VALUE)))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(cForceClose)))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(chMethod, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(labelMethod))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(chType, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(labelType))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(chLevel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(labelLevel))
                .addContainerGap(30, Short.MAX_VALUE))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                .addContainerGap(89, Short.MAX_VALUE)
                .addComponent(cForceClose)
                .addContainerGap())
        );

        getContentPane().add(jPanel2, java.awt.BorderLayout.PAGE_START);

        texDesc.setColumns(20);
        texDesc.setRows(5);
        jScrollPane1.setViewportView(texDesc);

        getContentPane().add(jScrollPane1, java.awt.BorderLayout.CENTER);

        pack();
    }

    private javax.swing.JButton butCancel;
    private javax.swing.JButton butOkay;
    private javax.swing.JButton butSave;
    private javax.swing.JCheckBox cForceClose;
    private javax.swing.JComboBox chLevel;
    private javax.swing.JComboBox chMethod;
    private javax.swing.JComboBox chPlayer;
    private javax.swing.JComboBox chType;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JLabel labelLevel;
    private javax.swing.JLabel labelMethod;
    private javax.swing.JLabel labelPlayer;
    private javax.swing.JLabel labelType;
    private javax.swing.JPanel panButtons;
    private javax.swing.JTextArea texDesc;

}
