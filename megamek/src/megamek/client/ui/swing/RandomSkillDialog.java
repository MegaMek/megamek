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
 * @author  Ben
 * @author  JasonSmyr
 */
package megamek.client.ui.swing;

import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Enumeration;
import java.util.Iterator;

import javax.swing.JDialog;

import megamek.client.Client;
import megamek.client.ui.Messages;
import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.Tank;
import megamek.common.VTOL;

public class RandomSkillDialog extends JDialog implements ActionListener,
        ItemListener {

    private static final long serialVersionUID = -2459992981678758743L;
    private Client client;
    private ClientGUI clientgui;

    /** Creates new form RandomSkillDialog2 */
    public RandomSkillDialog(ClientGUI clientgui) {
        super(clientgui.frame, Messages.getString("RandomSkillDialog.title"), true); //$NON-NLS-1$
        initComponents();

        this.client = clientgui.getClient();
        this.clientgui = clientgui;

        updatePlayerChoice();


        butOkay.setText(Messages.getString("Okay")); //$NON-NLS-1$
        butCancel.setText(Messages.getString("Cancel")); //$NON-NLS-1$
        labelMethod.setText(Messages.getString("RandomSkillDialog.labelMethod")); //$NON-NLS-1$
        labelType.setText(Messages.getString("RandomSkillDialog.labelType")); //$NON-NLS-1$
        labelLevel.setText(Messages.getString("RandomSkillDialog.labelLevel")); //$NON-NLS-1$
        labelPlayer.setText(Messages.getString("MechSelectorDialog.m_labelPlayer")); //$NON-NLS-1$
        texDesc.setText(Messages.getString("CustomMechDialog.texDesc")); //$NON-NLS-1$
        cForceClose.setText(Messages.getString("RandomSkillDialog.cForceClose"));


        cForceClose.setSelected(false);

        texDesc.setLineWrap(true);

        texDesc.setEnabled(true);

        chMethod.addItem(Messages.getString("RandomSkillDialog.MethodTW")); //$NON-NLS-1$
        chMethod.addItem(Messages.getString("RandomSkillDialog.MethodTaharqa")); //$NON-NLS-1$
        chMethod.addItem(Messages.getString("RandomSkillDialog.MethodConstant")); //$NON-NLS-1$

        chMethod.setSelectedIndex(Compute.METHOD_TW);
        chMethod.addItemListener(this);
        texDesc.setText(Messages.getString("RandomSkillDialog.descTW"));

        chType.addItem(Messages.getString("RandomSkillDialog.InnerSphere")); //$NON-NLS-1$
        chType.addItem(Messages.getString("RandomSkillDialog.Clan")); //$NON-NLS-1$
        chType.addItem(Messages.getString("RandomSkillDialog.ManeiDomini")); //$NON-NLS-1$

        chType.setSelectedIndex(Compute.TYPE_IS);

        chLevel.addItem(Messages.getString("RandomSkillDialog.Green")); //$NON-NLS-1$
        chLevel.addItem(Messages.getString("RandomSkillDialog.Regular")); //$NON-NLS-1$
        chLevel.addItem(Messages.getString("RandomSkillDialog.Veteran")); //$NON-NLS-1$
        chLevel.addItem(Messages.getString("RandomSkillDialog.Elite")); //$NON-NLS-1$

        chLevel.setSelectedIndex(Compute.LEVEL_REGULAR);

        butOkay.addActionListener(this);
        butCancel.addActionListener(this);

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
    }

    @Override
    public void setVisible(boolean show) {
        if (show) {
            updatePlayerChoice();
        }
        super.setVisible(show);
    }

    public void actionPerformed(java.awt.event.ActionEvent ev) {
        if (ev.getSource() == butOkay) {
            // go through all of the units for this player and assign random
            // skill levels
            Client c = null;
            if (chPlayer.getSelectedIndex() > 0) {
                String name = (String) chPlayer.getSelectedItem();
                c = clientgui.getBots().get(name);
            }
            if (c == null) {
                c = client;
            }
            for (Enumeration<Entity> e = c.game.getEntities(); e.hasMoreElements();) {
                Entity ent = e.nextElement();
                if (ent.getOwnerId() == c.getLocalPlayer().getId()) {
                    int skills[] = Compute.getRandomSkills(chMethod.getSelectedIndex(), chType.getSelectedIndex(),
                            chLevel.getSelectedIndex(), ent instanceof Tank
                            || ent instanceof VTOL);
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
            this.setVisible(false);
        }
        if (ev.getSource() == butCancel) {
            this.setVisible(false);
        }
    }

    public void itemStateChanged(ItemEvent ie) {
        if (ie.getSource().equals(chMethod)) {
            if (chMethod.getSelectedIndex() == Compute.METHOD_TW) {
                texDesc.setText(Messages.getString("RandomSkillDialog.descTW"));
            }
            if (chMethod.getSelectedIndex() == Compute.METHOD_TAHARQA) {
                texDesc.setText(Messages.getString("RandomSkillDialog.descTaharqa"));
            }
            if (chMethod.getSelectedIndex() == Compute.METHOD_CONSTANT) {
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

        butOkay.setText("Okay");
        panButtons.add(butOkay);

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
