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
package megamek.client.ui.AWT;

import java.awt.Button;
import java.awt.Checkbox;
import java.awt.Choice;
import java.awt.Dialog;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextArea;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Iterator;

import megamek.client.Client;
import megamek.client.RandomSkillsGenerator;
import megamek.client.ui.Messages;

public class RandomSkillDialog extends Dialog implements
        ActionListener, ItemListener {

    /**
     * 
     */
    private static final long serialVersionUID = -2459992981678758743L;
    private Client client;
    private ClientGUI clientgui;

    private Panel panButtons = new Panel();
    private Button butOkay = new Button(Messages.getString("Okay")); //$NON-NLS-1$
    private Button butCancel = new Button(Messages.getString("Cancel")); //$NON-NLS-1$

    private Label labelMethod = new Label(Messages
            .getString("RandomSkillDialog.labelMethod"), Label.RIGHT); //$NON-NLS-1$
    private Choice chMethod = new Choice();
    private Label labelType = new Label(Messages
            .getString("RandomSkillDialog.labelType"), Label.RIGHT); //$NON-NLS-1$
    private Choice chType = new Choice();
    private Label labelLevel = new Label(Messages
            .getString("RandomSkillDialog.labelLevel"), Label.RIGHT); //$NON-NLS-1$
    private Choice chLevel = new Choice();

    private Label labelPlayer = new Label(Messages
            .getString("MechSelectorDialog.m_labelPlayer"), Label.RIGHT); //$NON-NLS-1$
    private Choice chPlayer = new Choice();

    private TextArea texDesc = new TextArea(
            Messages.getString("CustomMechDialog.texDesc"), 3, 35, TextArea.SCROLLBARS_VERTICAL_ONLY); //$NON-NLS-1$

    private Checkbox cForceClose = new Checkbox(Messages
            .getString("RandomSkillDialog.cForceClose"));

    /** Creates a new instance of StartingPositionDialog */
    public RandomSkillDialog(ClientGUI clientgui) {
        super(clientgui.frame,
                Messages.getString("RandomSkillDialog.title"), true); //$NON-NLS-1$
        this.client = clientgui.getClient();
        this.clientgui = clientgui;

        updatePlayerChoice();

        cForceClose.setState(false);

        texDesc.setEnabled(true);

        chMethod.add(Messages.getString("RandomSkillDialog.MethodTW")); //$NON-NLS-1$
        chMethod.add(Messages.getString("RandomSkillDialog.MethodTaharqa")); //$NON-NLS-1$
        chMethod.add(Messages.getString("RandomSkillDialog.MethodConstant")); //$NON-NLS-1$

        chMethod.select(RandomSkillsGenerator.M_TW);
        chMethod.addItemListener(this);
        texDesc.setText(Messages.getString("RandomSkillDialog.descTW"));

        chType.add(Messages.getString("RandomSkillDialog.InnerSphere")); //$NON-NLS-1$
        chType.add(Messages.getString("RandomSkillDialog.Clan")); //$NON-NLS-1$
        chType.add(Messages.getString("RandomSkillDialog.ManeiDomini")); //$NON-NLS-1$

        chType.select(RandomSkillsGenerator.T_IS);

        chLevel.add(Messages.getString("RandomSkillDialog.Green")); //$NON-NLS-1$
        chLevel.add(Messages.getString("RandomSkillDialog.Regular")); //$NON-NLS-1$
        chLevel.add(Messages.getString("RandomSkillDialog.Veteran")); //$NON-NLS-1$
        chLevel.add(Messages.getString("RandomSkillDialog.Elite")); //$NON-NLS-1$

        chLevel.select(RandomSkillsGenerator.L_REG);

        setupButtons();

        // layout
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        this.setLayout(gridbag);

        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(4, 4, 4, 4);
        c.weightx = 1.0;
        c.weighty = 1.0;
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.EAST;
        gridbag.setConstraints(labelMethod, c);
        this.add(labelMethod);

        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.WEST;
        gridbag.setConstraints(chMethod, c);
        this.add(chMethod);

        c.gridwidth = 1;
        c.anchor = GridBagConstraints.EAST;
        gridbag.setConstraints(labelType, c);
        this.add(labelType);

        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.WEST;
        gridbag.setConstraints(chType, c);
        this.add(chType);

        c.gridwidth = 1;
        c.anchor = GridBagConstraints.EAST;
        gridbag.setConstraints(labelLevel, c);
        this.add(labelLevel);

        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.WEST;
        gridbag.setConstraints(chLevel, c);
        this.add(chLevel);

        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.CENTER;
        gridbag.setConstraints(cForceClose, c);
        this.add(cForceClose);

        c.weightx = 1.0;
        c.weighty = 0.0;
        gridbag.setConstraints(texDesc, c);
        this.add(texDesc);

        c.fill = GridBagConstraints.VERTICAL;
        gridbag.setConstraints(panButtons, c);
        this.add(panButtons);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                setVisible(false);
            }
        });

        pack();
        setResizable(false);
        setLocation(clientgui.frame.getLocation().x
                + clientgui.frame.getSize().width / 2 - getSize().width / 2,
                clientgui.frame.getLocation().y
                        + clientgui.frame.getSize().height / 2
                        - getSize().height / 2);
    }

    private void setupButtons() {
        butOkay.addActionListener(this);
        butCancel.addActionListener(this);

        // layout
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        panButtons.setLayout(gridbag);

        c.insets = new Insets(5, 5, 0, 0);
        c.weightx = 1.0;
        c.weighty = 1.0;
        c.fill = GridBagConstraints.VERTICAL;
        c.ipadx = 20;
        c.ipady = 5;
        c.gridwidth = 1;
        gridbag.setConstraints(butOkay, c);
        panButtons.add(butOkay);

        c.gridwidth = 1;
        gridbag.setConstraints(butCancel, c);
        panButtons.add(butCancel);
        c.gridwidth = 1;
        gridbag.setConstraints(labelPlayer, c);
        panButtons.add(labelPlayer);
        c.gridwidth = GridBagConstraints.REMAINDER;
        gridbag.setConstraints(chPlayer, c);
        panButtons.add(chPlayer);
    }

    private void updatePlayerChoice() {
        String lastChoice = chPlayer.getSelectedItem();
        chPlayer.removeAll();
        chPlayer.setEnabled(true);
        chPlayer.addItem(clientgui.getClient().getName());
        for (Iterator<Client> i = clientgui.getBots().values().iterator(); i.hasNext();) {
            chPlayer.addItem(i.next().getName());
        }
        if (chPlayer.getItemCount() == 1) {
            chPlayer.setEnabled(false);
        } else {
            chPlayer.select(lastChoice);
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
            /*
            // go through all of the units for this player and assign random
            // skill levels
            Client c = null;
            if (chPlayer.getSelectedIndex() > 0) {
                String name = chPlayer.getSelectedItem();
                c = clientgui.getBots().get(name);
            }
            if (c == null) {
                c = client;
            }
            for (Enumeration<Entity> e = c.game.getEntities(); e
                    .hasMoreElements();) {
                Entity ent = e.nextElement();
                if (ent.getOwnerId() == c.getLocalPlayer().getId()) {
                    int skills[] = Compute.getRandomSkills(chMethod
                            .getSelectedIndex(), chType.getSelectedIndex(),
                            chLevel.getSelectedIndex(), ent instanceof Tank
                                    || ent instanceof VTOL);
                    if (cForceClose.getState()) {
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
            */
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
        if (ie.getSource() == chMethod) {
            if (chMethod.getSelectedIndex() == RandomSkillsGenerator.M_TW) {
                texDesc.setText(Messages.getString("RandomSkillDialog.descTW"));
            }
            if (chMethod.getSelectedIndex() == RandomSkillsGenerator.M_TAHARQA) {
                texDesc.setText(Messages
                        .getString("RandomSkillDialog.descTaharqa"));
            }
            if (chMethod.getSelectedIndex() == RandomSkillsGenerator.M_CONSTANT) {
                texDesc.setText(Messages
                        .getString("RandomSkillDialog.descConstant"));
            }

        }
    }

    public void setClient(Client client) {
        this.client = client;
    }

}
