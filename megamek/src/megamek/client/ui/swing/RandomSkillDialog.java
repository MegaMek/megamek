/*
 * MegaMek - Copyright (C) 2002, 2003 Ben Mazur (bmazur@sev.org)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.client.ui.swing;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.List;
import java.util.Vector;

import javax.swing.*;

import megamek.MegaMek;
import megamek.client.Client;
import megamek.client.generator.enums.SkillGeneratorMethod;
import megamek.client.generator.enums.SkillGeneratorType;
import megamek.client.generator.skillGenerators.AbstractSkillGenerator;
import megamek.client.ui.Messages;
import megamek.common.Entity;
import megamek.common.annotations.Nullable;
import megamek.common.enums.SkillLevel;

/**
 * The random skill dialog allows the player to randomly assign skills to pilots based on overall experience level.
 */
public class RandomSkillDialog extends JDialog implements WindowListener {
    //region Variable Declarations
    protected static final String CLOSE_ACTION = "closeAction";

    private ClientGUI clientGUI;
    private List<Entity> units;
    private AbstractSkillGenerator skillGenerator;

    private JButton butOkay;
    private JCheckBox cForceClose;
    private JComboBox<SkillLevel> comboSkillLevel;
    private JComboBox<SkillGeneratorMethod> comboMethod;
    private JComboBox<String> comboPlayer;
    private JComboBox<SkillGeneratorType> comboType;
    //endregion Variable Declarations

    public RandomSkillDialog(final JFrame frame, final ClientGUI clientGUI) {
        super(frame, Messages.getString("RandomSkillDialog.title"), true);
        setClientGUI(clientGUI);
        setSkillGenerator(clientGUI.getClient().getSkillGenerator());
        initComponents();
        updatePlayerChoice();
    }
    //endregion Constructors

    //region Getters/Setters
    public ClientGUI getClientGUI() {
        return clientGUI;
    }

    public void setClientGUI(ClientGUI clientGUI) {
        this.clientGUI = clientGUI;
    }

    public List<Entity> getUnits() {
        return units;
    }

    public void setUnits(List<Entity> units) {
        this.units = units;
    }

    public AbstractSkillGenerator getSkillGenerator() {
        return skillGenerator;
    }

    public void setSkillGenerator(AbstractSkillGenerator skillGenerator) {
        this.skillGenerator = skillGenerator;
    }
    //endregion Getters/Setters

    //region Initialization
    protected void initialize() {
        setLayout(new BorderLayout());
        add(createCenterPane(), BorderLayout.CENTER);
        finalizeInitialization();
    }

    protected Container createCenterPane() {

    }

    private JPanel createButtonsPanel() {
        JPanel panButtons = new JPanel();

        butOkay = new JButton(Messages.getString("RandomSkillDialog.Okay"));
        butOkay.addActionListener(evt -> {
            saveSettings();
            // go through all of the units provided for this player and assign
            // random skill levels
            Client c = getClient();
            if (c == null) {
                return;
            }

            for (Entity ent : units) {
                if (ent.getOwnerId() == c.getLocalPlayer().getId()) {
                    for (int i = 0; i < ent.getCrew().getSlotCount(); i++) {
                        int[] skills = rsg.generateRandomSkills(ent);
                        if (cForceClose.isSelected()) {
                            skills[1] = skills[0] + 1;
                        }
                        ent.getCrew().setGunnery(skills[0], i);
                        ent.getCrew().setGunneryL(skills[0], i);
                        ent.getCrew().setGunneryM(skills[0], i);
                        ent.getCrew().setGunneryB(skills[0], i);
                        ent.getCrew().setPiloting(skills[1], i);
                    }
                    ent.getCrew().sortRandomSkills();
                    c.sendUpdateEntity(ent);
                }
            }
            clientGUI.chatlounge.refreshEntities();
            setVisible(false);
        });
        panButtons.add(butOkay);

        JButton butSave = new JButton(Messages.getString("RandomSkillDialog.Save"));
        butSave.addActionListener(evt -> {
            saveSettings();
            setVisible(false);
        });
        panButtons.add(butSave);

        JButton butCancel = new JButton(Messages.getString("Cancel"));
        butCancel.addActionListener(evt -> setVisible(false));
        panButtons.add(butCancel);

        JLabel labelPlayer = new JLabel(Messages.getString("MechSelectorDialog.m_labelPlayer"));
        panButtons.add(labelPlayer);

        comboPlayer = new JComboBox<>();
        comboPlayer.setModel(new DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        comboPlayer.addActionListener(evt -> {
            Client client = getClient();
            if (client == null) {
                return;
            }
            rsg = client.getSkillGenerator();
            updatePlayerChoice();
        });
        panButtons.add(comboPlayer);

        return panButtons;
    }

    protected void finalizeInitialization() {
        pack();

        // Escape keypress
        final KeyStroke escape = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(escape, CLOSE_ACTION);
        getRootPane().getInputMap(JComponent.WHEN_FOCUSED).put(escape, CLOSE_ACTION);
        getRootPane().getActionMap().put(CLOSE_ACTION, new AbstractAction() {
            private static final long serialVersionUID = 95171770700983453L;

            @Override
            public void actionPerformed(ActionEvent evt) {
                cancelActionPerformed(evt);
            }
        });

        addWindowListener(this);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    }
    //endregion Initialization

    //region Button Actions
    /**
     * This is the default Action Event Listener for the Ok Button's action. This triggers the Ok Action,
     * and then sets the dialog so that it is no longer visible.
     * @param evt the event triggering this
     */
    protected void okButtonActionPerformed(final ActionEvent evt) {
        okAction();
        setVisible(false);
    }

    /**
     * Action performed when the Ok button is clicked.
     */
    protected void okAction() {

    }

    /**
     * Note: Cancelling a dialog should always allow one to close the dialog.
     */
    protected void cancelActionPerformed(final ActionEvent evt) {
        try {
            cancelAction();
        } catch (Exception e) {
            MegaMek.getLogger().error(e);
        } finally {
            setVisible(false);
        }
    }

    /**
     * Action performed when the Cancel button is clicked, the dialog is closed by the X button, or
     * the escape key is pressed
     */
    protected void cancelAction() {

    }
    //endregion Button Actions

    //region WindowEvents
    /**
     * Note: Closing the dialog should always allow one to close the dialog.
     */
    @Override
    public void windowClosing(final WindowEvent evt) {
        try {
            cancelAction();
        } catch (Exception e) {
            MegaMek.getLogger().error(e);
        }
    }

    @Override
    public void windowOpened(final WindowEvent evt) {

    }

    @Override
    public void windowClosed(final WindowEvent evt) {

    }

    @Override
    public void windowIconified(final WindowEvent evt) {

    }

    @Override
    public void windowDeiconified(final WindowEvent evt) {

    }

    @Override
    public void windowActivated(final WindowEvent evt) {

    }

    @Override
    public void windowDeactivated(final WindowEvent evt) {

    }
    //endregion WindowEvents

    //region Old Code
    private void updatePlayerChoice() {
        String lastChoice = (String) comboPlayer.getSelectedItem();
        String clientName = clientGUI.getClient().getName();
        comboPlayer.removeAllItems();
        comboPlayer.setEnabled(true);
        comboPlayer.addItem(clientName);
        for (Client value : clientGUI.getBots().values()) {
            comboPlayer.addItem(value.getName());
        }
        if (comboPlayer.getItemCount() == 1) {
            comboPlayer.setEnabled(false);
        }
        comboPlayer.setSelectedItem(lastChoice);
        if (comboPlayer.getSelectedIndex() < 0) {
            comboPlayer.setSelectedIndex(0);
        }
        comboMethod.setSelectedItem(skillGenerator.getMethod());
        comboType.setSelectedItem(skillGenerator.getType());
        comboSkillLevel.setSelectedItem(skillGenerator.getLevel());
        cForceClose.setSelected(skillGenerator.isForceClose());
        pack();
    }

    private void saveSettings() {
        rsg.setType((SkillGeneratorType) comboType.getSelectedItem());
        rsg.setLevel((SkillLevel) comboSkillLevel.getSelectedItem());
        rsg.setClose(cForceClose.isSelected());
    }

    private @Nullable Client getClient() {
        Client client = null;
        if (comboPlayer.getSelectedIndex() > 0) {
            String name = (String) comboPlayer.getSelectedItem();
            client = clientGUI.getBots().get(name);
        }
        if (client == null) {
            client = clientGUI.getClient();
        }
        return client;
    }

    @Override
    public void setVisible(boolean show) {
        super.setVisible(show);
    }

    public void showDialog() {
        this.units = null;
        butOkay.setEnabled(false);
        setVisible(true);
    }

    public void showDialog(List<Entity> units) {
        this.units = units;
        butOkay.setEnabled(true);
        setVisible(true);
    }

    public void showDialog(Entity unit) {
         Vector<Entity> units = new Vector<>();
         units.add(unit);
         showDialog(units);
    }

    private void initComponents() {
        JPanel jPanel2 = new JPanel();

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        JLabel labelMethod = new JLabel(Messages.getString("RandomSkillDialog.labelMethod"));

        comboMethod = new JComboBox<>(SkillGeneratorMethod.values());

        JLabel labelType = new JLabel(Messages.getString("RandomSkillDialog.labelType"));

        comboType = new JComboBox<>(SkillGeneratorType.values());

        JLabel labelLevel = new JLabel(Messages.getString("RandomSkillDialog.labelLevel"));

        comboSkillLevel = new JComboBox<>(SkillLevel.values());
        comboSkillLevel.removeItem(SkillLevel.NONE);

        cForceClose = new JCheckBox(Messages.getString("RandomSkillDialog.cForceClose"));

        GroupLayout jPanel2Layout = new GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGroup(jPanel2Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                    .addGroup(GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                        .addGap(84, 84, 84)
                        .addGroup(jPanel2Layout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addComponent(labelLevel)
                                .addGap(15, 15, 15))
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addGroup(jPanel2Layout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                                    .addComponent(labelMethod)
                                    .addComponent(labelType))
                                .addGap(18, 18, 18)))
                        .addGroup(jPanel2Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                            .addComponent(comboMethod, GroupLayout.Alignment.TRAILING, 0, 238, Short.MAX_VALUE)
                            .addComponent(comboType, GroupLayout.Alignment.TRAILING, 0, 238, Short.MAX_VALUE)
                            .addComponent(comboSkillLevel, 0, 238, Short.MAX_VALUE)))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(cForceClose)))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(comboMethod, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addComponent(labelMethod))
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(comboType, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addComponent(labelType))
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(comboSkillLevel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addComponent(labelLevel))
                .addContainerGap(30, Short.MAX_VALUE))
            .addGroup(GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                .addContainerGap(89, Short.MAX_VALUE)
                .addComponent(cForceClose)
                .addContainerGap())
        );

        getContentPane().add(jPanel2, BorderLayout.CENTER);
        getContentPane().add(createButtonsPanel(), BorderLayout.PAGE_END);
    }
    //endregion Old Code
}
