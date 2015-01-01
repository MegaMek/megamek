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

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Hashtable;

import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;

import megamek.client.bot.BotClient;
import megamek.client.bot.TestBot;
import megamek.client.bot.princess.BehaviorSettings;
import megamek.client.bot.princess.BehaviorSettingsFactory;
import megamek.client.bot.princess.Princess;
import megamek.client.bot.princess.PrincessException;
import megamek.client.ui.Messages;
import megamek.common.logging.LogLevel;
import megamek.common.logging.Logger;

/**
 * BotConfigDialog is a dialog box that configures bot properties
 */
public class BotConfigDialog extends JDialog implements ActionListener, KeyListener {

    private static final String PRINCESS_PANEL = "princess_config";
    private static final String TESTBOT_PANEL = "testbot_config";
    private static final String UNIT_TARGET = Messages.getString("BotConfigDialog.targetUnit");
    private static final String BUILDING_TARGET = Messages.getString("BotConfigDialog.targetBuilding");

    private static final long serialVersionUID = -544663266637225925L;
    private JRadioButton testBotRadiobutton;
    private JRadioButton princessRadiobutton;
    private ButtonGroup selectBotGroup = new ButtonGroup();

    private BehaviorSettingsFactory behaviorSettingsFactory = BehaviorSettingsFactory.getInstance();
    private BehaviorSettings princessBehavior;

    // Items for princess config here
    private JComboBox<String> verbosityCombo;
    private JTextField targetId;
    private JComboBox<String> targetTypeCombo;
    private JButton addTargetButton;
    private JButton removeTargetButton;
    private JButton princessHelpButton;
    private JList<String> targetsList;
    private DefaultListModel<String> targetsListModel = new DefaultListModel<>();
    private JCheckBox forcedWithdrawalCheck;
    private JCheckBox goHomeCheck;
    private JCheckBox autoFleeCheck;
    private JComboBox<String> homeEdgeCombo; //The board edge to be used in a forced withdrawal.
    private JSlider aggressionSlidebar;
    private JSlider fallShameSlidebar;
    private JSlider herdingSlidebar;
    private JSlider selfPreservationSlidebar;
    private JSlider braverySlidebar;
    private JComboBox<String> princessBehaviorNames;

    private JTextField nameField;
    private boolean customName = false; // did user not use default name?
    public boolean dialogAborted = true; // did user not click Ok button?

    private final JButton butOK = new JButton(Messages.getString("Okay")); //$NON-NLS-1$

    private final JPanel botSpecificCardsPanel;

    public BotConfigDialog(JFrame parent) {
        super(parent, "Configure Bot", true);

        //        setLocationRelativeTo(parent);

        setLayout(new BorderLayout());
        add(switchBotPanel(), BorderLayout.NORTH);
        botSpecificCardsPanel = new JPanel(new CardLayout());
        botSpecificCardsPanel.add(new JPanel(), TESTBOT_PANEL);
        JScrollPane princessScroll = new JScrollPane(princessPanel());
        botSpecificCardsPanel.add(princessScroll, PRINCESS_PANEL);
        add(botSpecificCardsPanel, BorderLayout.CENTER);
        butOK.addActionListener(this);

        add(okayPanel(), BorderLayout.SOUTH);

        validate();
        pack();
        setSize(new Dimension(600, 600));
        setLocationRelativeTo(parent);
    }

    private JPanel switchBotPanel() {
        JPanel panel = new JPanel(new FlowLayout());
        testBotRadiobutton = new JRadioButton(Messages.getString("BotConfigDialog.testBotRadioButton"));
        testBotRadiobutton.addActionListener(this);
        princessRadiobutton = new JRadioButton(Messages.getString("BotConfigDialog.princessRadioButton"));
        princessRadiobutton.addActionListener(this);
        selectBotGroup.add(testBotRadiobutton);
        selectBotGroup.add(princessRadiobutton);
        testBotRadiobutton.setSelected(true);
        panel.add(testBotRadiobutton);
        panel.add(princessRadiobutton);
        return panel;
    }

    private boolean getPrincessBehavior() {
        princessBehavior = behaviorSettingsFactory.getBehavior((String) princessBehaviorNames.getSelectedItem());
        if (princessBehavior == null) {
            princessBehavior = new BehaviorSettings();
            return false;
        }
        return true;
    }

    private void setPrincessFields() {
        if (!getPrincessBehavior()) {
            return;
        }
        verbosityCombo.setSelectedIndex(0);
        forcedWithdrawalCheck.setSelected(princessBehavior.isForcedWithdrawal());
        goHomeCheck.setSelected(princessBehavior.shouldGoHome());
        autoFleeCheck.setSelected(princessBehavior.shouldAutoFlee());
        selfPreservationSlidebar.setValue(princessBehavior.getSelfPreservationIndex());
        aggressionSlidebar.setValue(princessBehavior.getHyperAggressionIndex());
        fallShameSlidebar.setValue(princessBehavior.getFallShameIndex());
        homeEdgeCombo.setSelectedIndex(princessBehavior.getHomeEdge().getIndex());
        herdingSlidebar.setValue(princessBehavior.getHerdMentalityIndex());
        braverySlidebar.setValue(princessBehavior.getBraveryIndex());
        targetsListModel.clear();
        for (String t : princessBehavior.getStrategicBuildingTargets()) {
            //noinspection unchecked
            targetsListModel.addElement(t);
        }
        repaint();
    }

    private JLabel buildSliderLabel(String caption) {
        Font slideFont = new Font(Font.SANS_SERIF, Font.PLAIN, 8);
        JLabel label = new JLabel(caption);
        label.setFont(slideFont);
        return label;
    }

    private JSlider buildSlider(String minMsgProperty, String maxMsgProperty, String toolTip, String title) {
        JSlider thisSlider = new JSlider(SwingConstants.HORIZONTAL, 0, 10, 5);
        Hashtable<Integer, JLabel> sliderLabels = new Hashtable<>(3);
        sliderLabels.put(0, buildSliderLabel("0 - " + minMsgProperty));
        sliderLabels.put(10, buildSliderLabel("10 - " + maxMsgProperty));
        sliderLabels.put(5, buildSliderLabel("5"));
        thisSlider.setToolTipText(toolTip);
        thisSlider.setLabelTable(sliderLabels);
        thisSlider.setPaintLabels(true);
        thisSlider.setMinorTickSpacing(1);
        thisSlider.setMajorTickSpacing(2);
        thisSlider.setSnapToTicks(true);
        thisSlider.setBorder(new TitledBorder(new LineBorder(Color.black), title));
        thisSlider.setEnabled(true);

        return thisSlider;
    }

    private JPanel okayPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 2, 2));

        JPanel namepanel = new JPanel(new FlowLayout());
        namepanel.add(new JLabel(Messages.getString("BotConfigDialog.nameLabel")));
        nameField = new JTextField();
        nameField.setText(Messages.getString("BotConfigDialog.namefield.default"));
        nameField.setColumns(12);
        nameField.setToolTipText(Messages.getString("BotConfigDialog.namefield.tooltip"));
        nameField.addKeyListener(this);
        namepanel.add(nameField);
        panel.add(namepanel);

        butOK.addActionListener(this);
        panel.add(butOK);

        panel.validate();
        return panel;
    }

    private JPanel buildSliderPanel() {
        JPanel panel = new JPanel();

        GridBagConstraints constraints = new GridBagConstraints();
        GridBagLayout layout = new GridBagLayout();
        panel.setLayout(layout);

        //Initialize constraints.
        constraints.gridheight = 1;
        constraints.gridwidth = 0;
        constraints.weightx = 1;
        constraints.weighty = 1;
        constraints.insets = new Insets(0, 0, 0, 0);
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;

        //Row 1
        constraints.gridy = 0;
        constraints.gridx = 0;
        braverySlidebar = buildSlider(Messages.getString("BotConfigDialog.braverySliderMin"),
                                      Messages.getString("BotConfigDialog.braverySliderMax"),
                                      Messages.getString("BotConfigDialog.braveryTooltip"),
                                      Messages.getString("BotConfigDialog.braverySliderTitle"));
        panel.add(braverySlidebar, constraints);

        //Row 2
        constraints.gridy++;
        selfPreservationSlidebar = buildSlider(Messages.getString("BotConfigDialog.selfPreservationSliderMin"),
                                               Messages.getString("BotConfigDialog.selfPreservationSliderMax"),
                                               Messages.getString("BotConfigDialog.selfPreservationTooltip"),
                                               Messages.getString("BotConfigDialog.selfPreservationSliderTitle"));
        panel.add(selfPreservationSlidebar, constraints);

        //Row 3
        constraints.gridy++;
        aggressionSlidebar = buildSlider(Messages.getString("BotConfigDialog.aggressionSliderMin"),
                                         Messages.getString("BotConfigDialog.aggressionSliderMax"),
                                         Messages.getString("BotConfigDialog.aggressionTooltip"),
                                         Messages.getString("BotConfigDialog.aggressionSliderTitle"));
        panel.add(aggressionSlidebar, constraints);

        //Row 4
        constraints.gridy++;
        herdingSlidebar = buildSlider(Messages.getString("BotConfigDialog.herdingSliderMin"),
                                      Messages.getString("BotConfigDialog.herdingSliderMax"),
                                      Messages.getString("BotConfigDialog.herdingToolTip"),
                                      Messages.getString("BotConfigDialog.herdingSliderTitle"));
        panel.add(herdingSlidebar, constraints);

        //Row 5
        constraints.gridy++;
        fallShameSlidebar = buildSlider(Messages.getString("BotConfigDialog.fallShameSliderMin"),
                                        Messages.getString("BotConfigDialog.fallShameSliderMax"),
                                        Messages.getString("BotConfigDialog.fallShameToolTip"),
                                        Messages.getString("BotConfigDialog.fallShameSliderTitle"));
        panel.add(fallShameSlidebar, constraints);

        panel.setBorder(new TitledBorder(new LineBorder(Color.black, 1), "Behavior Settings"));

        return panel;
    }

    private JPanel princessPanel() {

        //Setup layout.
        JPanel panel = new JPanel();
        GridBagConstraints constraints = new GridBagConstraints();
        GridBagLayout layout = new GridBagLayout();
        panel.setLayout(layout);

        //Initialize constraints.
        constraints.gridheight = 1;
        constraints.gridwidth = 1;
        constraints.insets = new Insets(2, 2, 2, 2);
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;

        //Row 1 Column 1
        constraints.gridy = 0;
        constraints.gridx = 0;
        JLabel behaviorNameLabel = new JLabel(Messages.getString("BotConfigDialog.behaviorNameLabel"));
        panel.add(behaviorNameLabel, constraints);

        //Row 1 Column 2
        constraints.gridx++;
        constraints.gridwidth = 2;
        princessBehaviorNames = new JComboBox<>(behaviorSettingsFactory.getBehaviorNames());
        princessBehaviorNames.setSelectedItem(BehaviorSettingsFactory.DEFAULT_BEHAVIOR_DESCRIPTION);
        princessBehaviorNames.setToolTipText(Messages.getString("BotConfigDialog.behaviorToolTip"));
        princessBehaviorNames.addActionListener(this);
        princessBehaviorNames.setEditable(true);
        panel.add(princessBehaviorNames, constraints);

        //Row 2 Column 1
        constraints.gridx = 0;
        constraints.gridwidth = 1;
        constraints.gridy++;
        JLabel verbosityLabel = new JLabel(Messages.getString("BotConfigDialog.verbosityLabel"));
        panel.add(verbosityLabel, constraints);

        //Row 2 Column 2;
        constraints.gridx++;
        constraints.gridwidth = 2;
        verbosityCombo = new JComboBox<>(LogLevel.getLogLevelNames());
        verbosityCombo.setToolTipText(Messages.getString("BotConfigDialog.verbosityToolTip"));
        verbosityCombo.setSelectedIndex(0);
        panel.add(verbosityCombo, constraints);

        //Row 3 Column 1.
        constraints.gridy++;
        constraints.gridx = 0;
        constraints.gridwidth = 1;
        forcedWithdrawalCheck = new JCheckBox(Messages.getString("BotConfigDialog.forcedWithdrawalCheck"));
        forcedWithdrawalCheck.setToolTipText(Messages.getString("BotConfigDialog.forcedWithdrawalTooltip"));
        panel.add(forcedWithdrawalCheck, constraints);

        //Row 3 Column 2.
        constraints.gridx++;
        goHomeCheck = new JCheckBox(Messages.getString("BotConfigDialog.goHomeCheck"));
        goHomeCheck.setToolTipText(Messages.getString("BotConfigDialog.goHomeTooltip"));
        goHomeCheck.addActionListener(this);
        panel.add(goHomeCheck, constraints);

        //Row 3 Column 3.
        constraints.gridx++;
        autoFleeCheck = new JCheckBox(Messages.getString("BotConfigDialog.autoFleeCheck"));
        autoFleeCheck.setToolTipText(Messages.getString("BotConfigDialog.autoFleeTooltip"));
        autoFleeCheck.addActionListener(this);
        autoFleeCheck.setEnabled(false);
        panel.add(autoFleeCheck, constraints);

        //Row 4 Column 1.
        constraints.gridy++;
        constraints.gridx = 0;
        JLabel homeEdgeLabel = new JLabel(Messages.getString("BotConfigDialog.homeEdgeLabel"));
        layout.setConstraints(homeEdgeLabel, constraints);
        panel.add(homeEdgeLabel);

        //Row 4 Column 2.
        constraints.gridx++;
        constraints.gridwidth = 2;
        homeEdgeCombo = new JComboBox<>(new String[]{Messages.getString("BotConfigDialog.northEdge"),
                                                     Messages.getString("BotConfigDialog.southEdge"),
                                                     Messages.getString("BotConfigDialog.westEdge"),
                                                     Messages.getString("BotConfigDialog.eastEdge")});
        homeEdgeCombo.setToolTipText(Messages.getString("BotConfigDialog.homeEdgeTooltip"));
        homeEdgeCombo.setSelectedIndex(0);
        panel.add(homeEdgeCombo, constraints);

        //Row 5.
        constraints.gridx = 0;
        constraints.gridy++;
        constraints.gridwidth = 3;
        JPanel sliderPanel = buildSliderPanel();
        layout.setConstraints(sliderPanel, constraints);
        panel.add(sliderPanel);

        //Row 6 Column 1.
        constraints.gridy++;
        constraints.gridx = 0;
        constraints.gridwidth = 1;
        JLabel targetsLabel = new JLabel(Messages.getString("BotConfigDialog.targetsLabel"));
        panel.add(targetsLabel, constraints);

        //Row 6 Column 2.
        constraints.gridx++;
        targetTypeCombo = new JComboBox<>(new String[]{BUILDING_TARGET, UNIT_TARGET});
        targetTypeCombo.setToolTipText(Messages.getString("BotConfigDialog.targetTypeTooltip"));
        targetTypeCombo.setSelectedIndex(0);
        panel.add(targetTypeCombo, constraints);

        // Row 6 Column 3.
        constraints.gridx++;
        targetId = new JTextField();
        targetId.setToolTipText(Messages.getString("BotConfigDialog.princessTargetIdToolTip"));
        targetId.setColumns(4);
        panel.add(targetId, constraints);

        //Row 7.
        constraints.gridy++;
        constraints.gridx = 0;
        constraints.gridwidth = 3;
        panel.add(buildStrategicTargetsButtonPanel(), constraints);

        //Row 8
        constraints.gridy++;
        constraints.gridx = 0;
        constraints.gridwidth = 3;
        targetsList = new JList<>(targetsListModel);
        targetsList.setToolTipText(Messages.getString("BotConfigDialog.princessStrategicTargetsToolTip"));
        targetsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        targetsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        targetsList.setLayoutOrientation(JList.VERTICAL);
        JScrollPane targetScroller = new JScrollPane(targetsList);
        targetScroller.setAlignmentX(LEFT_ALIGNMENT);
        panel.add(targetScroller, constraints);

        //Row 9, Column 2
        constraints.gridy++;
        constraints.gridx = 1;
        constraints.gridwidth = 1;
        princessHelpButton = new JButton(Messages.getString("BotConfigDialog.princessHelpButtonCaption"));
        princessHelpButton.addActionListener(this);
        panel.add(princessHelpButton, constraints);

        setPrincessFields();
        panel.validate();
        return panel;
    }

    private JPanel buildStrategicTargetsButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 1, 1));
        addTargetButton = new JButton(Messages.getString("BotConfigDialog.princessAddTargetButtonCaption"));
        addTargetButton.setToolTipText(Messages.getString("BotConfigDialog.princessAddTargetButtonToolTip"));
        addTargetButton.addActionListener(this);
        panel.add(addTargetButton);

        removeTargetButton = new JButton(Messages.getString("BotConfigDialog.princessRemoveTargetButtonCaption"));
        removeTargetButton.setToolTipText(Messages.getString("BotConfigDialog.princessRemoveTargetButtonToolTip"));
        removeTargetButton.addActionListener(this);
        panel.add(removeTargetButton);

        return panel;
    }

    private void launchPrincessHelp() {
        try {
            // Get the correct help file.
            StringBuilder helpPath = new StringBuilder("file:///");
            helpPath.append(System.getProperty("user.dir"));
            if (!helpPath.toString().endsWith(File.separator)) {
                helpPath.append(File.separator);
            }
            helpPath.append(Messages.getString("BotConfigDialog.princessHelpPath"));
            URL helpUrl = new URL(helpPath.toString());

            // Launch the help dialog.
            HelpDialog helpDialog = new HelpDialog(Messages.getString("BotConfigDialog.princessHelp.title"), helpUrl);
            helpDialog.setVisible(true);
        } catch (MalformedURLException e) {
            handleError("launchPrincessHelp", e);
        }
    }

    public void actionPerformed(ActionEvent e) {
        CardLayout cardlayout = (CardLayout) (botSpecificCardsPanel.getLayout());
        if (testBotRadiobutton.equals(e.getSource())) {
            if (!customName) {
                nameField.setText("TestBot");
            }
            cardlayout.show(botSpecificCardsPanel, TESTBOT_PANEL);

        } else if (princessRadiobutton.equals(e.getSource())) {
            if (!customName) {
                nameField.setText("Princess");
            }
            cardlayout.show(botSpecificCardsPanel, PRINCESS_PANEL);

        } else if (butOK.equals(e.getSource())) {
            dialogAborted = false;
            savePrincessProperties();
            setVisible(false);

        } else if (addTargetButton.equals(e.getSource())) {
            String data = targetTypeCombo.getSelectedItem() + ": " + targetId.getText();
            targetsListModel.addElement(data);

        } else if (removeTargetButton.equals(e.getSource())) {
            targetsListModel.removeElementAt(targetsList.getSelectedIndex());

        } else if (princessBehaviorNames.equals(e.getSource())) {
            setPrincessFields();

        } else if (goHomeCheck.equals(e.getSource())) {
            if (!goHomeCheck.isSelected()) {
                autoFleeCheck.setSelected(false);
            }
            autoFleeCheck.setEnabled(goHomeCheck.isSelected());
        } else if (princessHelpButton.equals(e.getSource())) {
            launchPrincessHelp();
        }
    }

    private void handleError(String method, Throwable t) {
        JOptionPane.showMessageDialog(this, t.getMessage(), "ERROR", JOptionPane.ERROR_MESSAGE);
        new Logger().log(getClass(), method, t);
    }

    private void savePrincessProperties() {
        BehaviorSettings tempBehavior = new BehaviorSettings();

        try {
            tempBehavior.setDescription((String) princessBehaviorNames.getSelectedItem());
        } catch (PrincessException e) {
            handleError("savePrincessProperties", e);
        }
        tempBehavior.setFallShameIndex(fallShameSlidebar.getValue());
        tempBehavior.setForcedWithdrawal(forcedWithdrawalCheck.isSelected());
        tempBehavior.setAutoFlee(autoFleeCheck.isSelected());
        tempBehavior.setGoHome(goHomeCheck.isSelected());
        tempBehavior.setHomeEdge(homeEdgeCombo.getSelectedIndex());
        tempBehavior.setHyperAggressionIndex(aggressionSlidebar.getValue());
        tempBehavior.setSelfPreservationIndex(selfPreservationSlidebar.getValue());
        tempBehavior.setHerdMentalityIndex(herdingSlidebar.getValue());
        tempBehavior.setBraveryIndex(braverySlidebar.getValue());
        tempBehavior.setFallShameIndex(fallShameSlidebar.getValue());
        for (int i = 0; i < targetsListModel.getSize(); i++) {
            String[] target = targetsListModel.get(i).split(":");
            if (BUILDING_TARGET.equalsIgnoreCase(target[0].trim())) {
                tempBehavior.addStrategicTarget(target[1].trim());
            } else {
                tempBehavior.addPriorityUnit(target[1].trim());
            }
        }
        boolean save = false;
        boolean saveTargets = false;
        if (!tempBehavior.equals(princessBehavior)) {
            SavePrincessDialog dialog = new SavePrincessDialog(this);
            dialog.setVisible(true);
            save = dialog.doSave();
            saveTargets = dialog.doSaveTargets();
            dialog.dispose();
        }
        princessBehavior = tempBehavior;

        if (save) {
            behaviorSettingsFactory.addBehavior(princessBehavior);
            behaviorSettingsFactory.saveBehaviorSettings(saveTargets);
        }
    }

    public void keyTyped(KeyEvent e) {
        customName = true;
    }

    public void keyReleased(KeyEvent e) {
    }

    public void keyPressed(KeyEvent e) {
    }

    /**
     * gets the selected, configured bot from the dialog
     *
     * @param host The game server's host address.
     * @param port The gme server's host port.
     * @return A new bot-controlled client.
     */
    BotClient getSelectedBot(String host, int port) {
        if (testBotRadiobutton.isSelected()) {
            return new TestBot(getBotName(), host, port);

        } else if (princessRadiobutton.isSelected()) {
            Princess toReturn = new Princess(getBotName(), host, port,
                                             LogLevel.getLogLevel(verbosityCombo.getSelectedIndex()));
            toReturn.setBehaviorSettings(princessBehavior);
            toReturn.log(getClass(), "getSelectedBot(String, int)", LogLevel.DEBUG,
                         toReturn.getBehaviorSettings().toLog());
            return toReturn;
        }
        return null; // shouldn't happen
    }

    String getBotName() {
        return nameField.getText();
    }
}
