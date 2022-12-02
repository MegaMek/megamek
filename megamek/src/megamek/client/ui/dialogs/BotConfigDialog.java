/*
 * Copyright (c) 2000-2011 Ben Mazur (bmazur@sev.org)
 * Copyright (c) 2021 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.client.ui.dialogs;

import megamek.client.Client;
import megamek.client.bot.princess.*;
import megamek.client.ui.Messages;
import megamek.client.ui.baseComponents.AbstractButtonDialog;
import megamek.client.ui.baseComponents.MMComboBox;
import megamek.client.ui.dialogs.helpDialogs.PrincessHelpDialog;
import megamek.client.ui.enums.DialogResult;
import megamek.client.ui.swing.ClientGUI;
import megamek.client.ui.swing.GUIPreferences;
import megamek.client.ui.swing.MMToggleButton;
import megamek.client.ui.swing.util.ScalingPopup;
import megamek.client.ui.swing.util.UIUtil;
import megamek.client.ui.swing.util.UIUtil.*;
import megamek.common.*;
import megamek.common.annotations.Nullable;
import megamek.common.enums.GamePhase;
import org.apache.logging.log4j.LogManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

import static megamek.common.Terrains.*;

/** A dialog box to configure (Princess) bot properties. */
public class BotConfigDialog extends AbstractButtonDialog implements ActionListener, 
        ListSelectionListener, ChangeListener {

    private static final String SAVEGAME_CONFIG = Messages.getString("BotConfigDialog.previousConfig");
    private static final String OK_ACTION = "Ok_Action";

    private BehaviorSettingsFactory behaviorSettingsFactory = BehaviorSettingsFactory.getInstance();
    private BehaviorSettings princessBehavior;

    private JLabel nameLabel = new JLabel(Messages.getString("BotConfigDialog.nameLabel"));
    private TipTextField nameField = new TipTextField("", 16);
    
    private JButton addTargetButton = new TipButton(Messages.getString("BotConfigDialog.addHexTarget"));
    private JButton addUnitButton = new TipButton(Messages.getString("BotConfigDialog.addUnitTarget"));
    private JButton removeTargetButton = new TipButton(Messages.getString("BotConfigDialog.removeTarget"));
    private DefaultListModel<Object> targetsListModel = new DefaultListModel<>();
    private TipList<Object> targetsList = new TipList<>(targetsListModel);
    
    private MMToggleButton forcedWithdrawalCheck = new TipMMToggleButton(Messages.getString("BotConfigDialog.forcedWithdrawalCheck"));
    private JLabel withdrawEdgeLabel = new JLabel(Messages.getString("BotConfigDialog.retreatEdgeLabel"));
    private MMComboBox<CardinalEdge> withdrawEdgeCombo = new TipCombo<>("EdgeToWithdraw", CardinalEdge.values());
    private MMToggleButton autoFleeCheck = new TipMMToggleButton(Messages.getString("BotConfigDialog.autoFleeCheck"));
    private JLabel fleeEdgeLabel = new JLabel(Messages.getString("BotConfigDialog.homeEdgeLabel"));
    private MMComboBox<CardinalEdge> fleeEdgeCombo = new TipCombo<>("EdgeToFlee", CardinalEdge.values());
    
    private TipSlider aggressionSlidebar = new TipSlider(SwingConstants.HORIZONTAL, 0, 10, 5);
    private TipSlider fallShameSlidebar = new TipSlider(SwingConstants.HORIZONTAL, 0, 10, 5);
    private TipSlider herdingSlidebar = new TipSlider(SwingConstants.HORIZONTAL, 0, 10, 5);
    private TipSlider selfPreservationSlidebar = new TipSlider(SwingConstants.HORIZONTAL, 0, 10, 5);
    private TipSlider braverySlidebar = new TipSlider(SwingConstants.HORIZONTAL, 0, 10, 5);
    private TipButton savePreset = new TipButton(Messages.getString("BotConfigDialog.save"));
    private TipButton saveNewPreset = new TipButton(Messages.getString("BotConfigDialog.saveNew"));
    
    private JButton princessHelpButton = new JButton(Messages.getString("BotConfigDialog.help"));
            
    private JPanel presetsPanel;
    private JLabel chooseLabel = new JLabel(Messages.getString("BotConfigDialog.behaviorNameLabel"));
    /** A copy of the current presets. Modifications will only be saved when accepted. */
    private List<String> presets;
    private PresetsModel presetsModel = new PresetsModel();
    private JList<String> presetsList = new JList<>(presetsModel);
    
    private final JButton butOK = new JButton(Messages.getString("Okay"));
    private final JButton butCancel = new JButton(Messages.getString("Cancel"));

    /** The pre-existing bot player name that is affected by the dialog, if there is one. Null otherwise. */
    private final String fixedBotPlayerName;
    private final boolean isNewBot;
    
    /** Stores the currently chosen preset. Used to detect if the player has changed the sliders. */
    private BehaviorSettings chosenPreset;
    
    /** Stores the original Behavior if one was given in the constructor (= a save game Behavior). */
    private final BehaviorSettings saveGameBehavior;
    
    /** A ClientGUI given to the dialog. */
    private final ClientGUI clientGui;
    
    /** Convenience field for clientGui.getClient(). */
    private final Client client;
    
    public BotConfigDialog(JFrame parent, @Nullable String botName) {
        this(parent, botName, null, null);
    } 
    
    public BotConfigDialog(JFrame parent, @Nullable String botName, 
            @Nullable BehaviorSettings behavior, @Nullable ClientGUI cg) {
        super(parent, "BotConfigDialog", "BotConfigDialog.title");
        fixedBotPlayerName = botName;
        isNewBot = botName == null;
        clientGui = cg;
        client = cg != null ? cg.getClient() : null;
        princessBehavior = (behavior != null) ? behavior : new BehaviorSettings();
        saveGameBehavior = behavior;
        updatePresets();
        initialize();
        updateDialogFields();
    }
    
    @Override
    protected void initialize() {
        // Make Enter confirm and close the dialog
        final KeyStroke enter = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(enter, OK_ACTION);
        getRootPane().getInputMap(JComponent.WHEN_FOCUSED).put(enter, OK_ACTION);
        getRootPane().getActionMap().put(OK_ACTION, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                okButtonActionPerformed(evt);
            }
        });
        super.initialize();
    }
    
    @Override
    protected Container createCenterPane() {
        JPanel result = new JPanel();
        result.setLayout(new BoxLayout(result, BoxLayout.PAGE_AXIS));
        result.add(nameSection());
        result.add(settingSection());
        return result;
    }
    
    /** The setting section contains the presets list on the left side and the princess settings on the right. */
    private JPanel settingSection() {
        var princessScroll = new JScrollPane(princessPanel());
        princessScroll.getVerticalScrollBar().setUnitIncrement(16);
        princessScroll.setBorder(null);
        presetsPanel = presetsPanel();
        
        var result = new JPanel(new BorderLayout(0, 0));
        result.setAlignmentX(LEFT_ALIGNMENT);
        result.add(princessScroll, BorderLayout.CENTER);
        result.add(presetsPanel, BorderLayout.LINE_START);
        return result;
    }
    
    /** The princess panel contains the individual princess settings. */
    private JPanel princessPanel() {
        JPanel result = new JPanel();
        result.setLayout(new BoxLayout(result, BoxLayout.PAGE_AXIS));
        result.add(behaviorSection());
        result.add(retreatSection());
        result.add(targetsSection());
        return result;
    }
    
    private JPanel nameSection() {
        JPanel result = new JPanel();
        result.setLayout(new BoxLayout(result, BoxLayout.PAGE_AXIS));
        result.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        Content panContent = new Content();
        panContent.setLayout(new BoxLayout(panContent, BoxLayout.PAGE_AXIS));
        result.add(panContent);
        
        var namePanel = new JPanel();
        nameField.setToolTipText(Messages.getString("BotConfigDialog.namefield.tooltip"));
        // When the dialog configures an existing player, the name must not be changed
        nameField.setText((fixedBotPlayerName == null) ? getFreePrincessName() : fixedBotPlayerName);
        nameField.setEnabled(fixedBotPlayerName == null);
        nameLabel.setLabelFor(nameField);
        nameLabel.setDisplayedMnemonic(KeyEvent.VK_N);
        namePanel.add(nameLabel);
        namePanel.add(nameField);
        
        panContent.add(namePanel);
        return result;
    }
    
    /** 
     * Returns a name that is not used by another player. Starts with "Princess"
     * and subsequently adds numbers (Princess1 etc.) until a free one is found. 
     */
    private String getFreePrincessName() {
        String baseName = "Princess";
        String name = baseName;
        if (client != null) {
            int counter = 0;
            Set<String> playerNames = client.getGame().getPlayersVector().stream().map(Player::getName).collect(Collectors.toSet());
            while (playerNames.contains(name) && counter < 1000) {
                counter++;
                name = baseName + counter;
            }
        }
        return name;
    }
    
    /** The presets panel has a list of behavior presets for Princess. */
    private JPanel presetsPanel() {
        var result = new JPanel();
        result.setLayout(new BoxLayout(result, BoxLayout.PAGE_AXIS));
        result.setBorder(new EmptyBorder(0, 10, 0, 20));
        
        chooseLabel.setAlignmentX(CENTER_ALIGNMENT);
        chooseLabel.setDisplayedMnemonic(KeyEvent.VK_P);
        chooseLabel.setLabelFor(presetsList);
        var headerPanel = new FixedYPanel();
        headerPanel.add(chooseLabel);
        
        presetsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        presetsList.addListSelectionListener(this);
        presetsList.setCellRenderer(new PresetsRenderer());
        presetsList.addMouseListener(presetsMouseListener);

        result.add(headerPanel);
        result.add(Box.createVerticalStrut(10));
        result.add(presetsList);
        
        return result;
    }
    
    private JPanel behaviorSection() {
        JPanel result = new OptionPanel("BotConfigDialog.behaviorSection");
        Content panContent = new Content();
        panContent.setLayout(new BoxLayout(panContent, BoxLayout.PAGE_AXIS));
        result.add(panContent);

        panContent.add(buildSlider(braverySlidebar, Messages.getString("BotConfigDialog.braverySliderMin"),
                Messages.getString("BotConfigDialog.braverySliderMax"),
                Messages.getString("BotConfigDialog.braveryTooltip"),
                Messages.getString("BotConfigDialog.braverySliderTitle")));
        panContent.add(Box.createVerticalStrut(7));

        panContent.add(buildSlider(selfPreservationSlidebar, Messages.getString("BotConfigDialog.selfPreservationSliderMin"),
                Messages.getString("BotConfigDialog.selfPreservationSliderMax"),
                Messages.getString("BotConfigDialog.selfPreservationTooltip"),
                Messages.getString("BotConfigDialog.selfPreservationSliderTitle")));
        panContent.add(Box.createVerticalStrut(7));

        panContent.add(buildSlider(aggressionSlidebar, Messages.getString("BotConfigDialog.aggressionSliderMin"),
                Messages.getString("BotConfigDialog.aggressionSliderMax"),
                Messages.getString("BotConfigDialog.aggressionTooltip"),
                Messages.getString("BotConfigDialog.aggressionSliderTitle")));
        panContent.add(Box.createVerticalStrut(7));

        panContent.add(buildSlider(herdingSlidebar, Messages.getString("BotConfigDialog.herdingSliderMin"),
                Messages.getString("BotConfigDialog.herdingSliderMax"),
                Messages.getString("BotConfigDialog.herdingToolTip"),
                Messages.getString("BotConfigDialog.herdingSliderTitle")));
        panContent.add(Box.createVerticalStrut(7));

        panContent.add(buildSlider(fallShameSlidebar, Messages.getString("BotConfigDialog.fallShameSliderMin"),
                Messages.getString("BotConfigDialog.fallShameSliderMax"),
                Messages.getString("BotConfigDialog.fallShameToolTip"),
                Messages.getString("BotConfigDialog.fallShameSliderTitle")));
        
        var buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        buttonPanel.setAlignmentX(SwingConstants.CENTER);
        result.add(buttonPanel);
        
        savePreset.addActionListener(this);
        savePreset.setMnemonic(KeyEvent.VK_S);
        savePreset.setToolTipText(Messages.getString("BotConfigDialog.saveTip"));
        buttonPanel.add(savePreset);
        saveNewPreset.addActionListener(this);
        saveNewPreset.setMnemonic(KeyEvent.VK_A);
        saveNewPreset.setToolTipText(Messages.getString("BotConfigDialog.saveNewTip"));
        buttonPanel.add(saveNewPreset);

        return result;
    }
    
    private JPanel targetsSection() {
        JPanel result = new OptionPanel("BotConfigDialog.targetsSection");
        Content panContent = new Content();
        panContent.setLayout(new BoxLayout(panContent, BoxLayout.PAGE_AXIS));
        result.add(panContent);
        
        addTargetButton.setMnemonic(KeyEvent.VK_X);
        addTargetButton.addActionListener(this);
        
        addUnitButton.setMnemonic(KeyEvent.VK_U);
        addUnitButton.addActionListener(this);
        
        removeTargetButton.setMnemonic(KeyEvent.VK_R);
        removeTargetButton.setFont(UIUtil.getScaledFont());
        removeTargetButton.setForeground(GUIPreferences.getInstance().getWarningColor());
        removeTargetButton.addActionListener(this);
        
        JPanel removeButtonPanel = new FixedXPanel(new FlowLayout(FlowLayout.RIGHT));
        removeButtonPanel.add(removeTargetButton);
        
        var addButtonPanel = new FixedXPanel(new FlowLayout(FlowLayout.LEFT));
        addButtonPanel.add(addUnitButton);
        addButtonPanel.add(addTargetButton);
        
        var buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.LINE_AXIS));
        buttonPanel.add(addButtonPanel);
        buttonPanel.add(Box.createHorizontalGlue());
        buttonPanel.add(removeButtonPanel);

        targetsList.setToolTipText(Messages.getString("BotConfigDialog.targetsListTip"));
        targetsList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        targetsList.getSelectionModel().addListSelectionListener(this);
        targetsList.setLayoutOrientation(JList.VERTICAL);
        targetsList.setCellRenderer(new TargetsRenderer());
        targetsList.setFont(UIUtil.getScaledFont());
        targetsList.setVisibleRowCount(6);
        targetsList.setPrototypeCellValue(new Coords(21, 22));
        JScrollPane targetsListScroller = new JScrollPane(targetsList);
        
        panContent.add(buttonPanel);
        panContent.add(targetsListScroller);
        
        return result;
    }
    
    private JPanel retreatSection() {
        JPanel result = new OptionPanel("BotConfigDialog.retreatSection");
        Content panContent = new Content();
        panContent.setLayout(new BoxLayout(panContent, BoxLayout.PAGE_AXIS));
        result.add(panContent);

        autoFleeCheck.setToolTipText(Messages.getString("BotConfigDialog.autoFleeTooltip"));
        autoFleeCheck.addActionListener(this);
        autoFleeCheck.setMnemonic(KeyEvent.VK_F);
        
        fleeEdgeCombo.removeItem(CardinalEdge.NONE);
        fleeEdgeCombo.setToolTipText(Messages.getString("BotConfigDialog.homeEdgeTooltip"));
        fleeEdgeCombo.setSelectedIndex(0);
        fleeEdgeCombo.addActionListener(this);
        
        forcedWithdrawalCheck.setToolTipText(Messages.getString("BotConfigDialog.forcedWithdrawalTooltip"));
        forcedWithdrawalCheck.addActionListener(this);
        forcedWithdrawalCheck.setMnemonic(KeyEvent.VK_W);

        withdrawEdgeCombo.removeItem(CardinalEdge.NONE);
        withdrawEdgeCombo.setToolTipText(Messages.getString("BotConfigDialog.retreatEdgeTooltip"));
        withdrawEdgeCombo.setSelectedIndex(0);

        var firstLine = new JPanel(new FlowLayout(FlowLayout.LEFT));
        var secondLine = new JPanel(new FlowLayout(FlowLayout.LEFT));
        firstLine.add(forcedWithdrawalCheck);
        firstLine.add(Box.createHorizontalStrut(20));
        firstLine.add(withdrawEdgeLabel);
        firstLine.add(withdrawEdgeCombo);
        secondLine.add(autoFleeCheck);
        secondLine.add(Box.createHorizontalStrut(20));
        secondLine.add(fleeEdgeLabel);
        secondLine.add(fleeEdgeCombo);
        panContent.add(firstLine);
        panContent.add(Box.createVerticalStrut(5));
        panContent.add(secondLine);
        
        return result;
    }
    
    /** Returns the full behavior setting that the player entered in this dialog. */
    public BehaviorSettings getBehaviorSettings() {
        savePrincessProperties();
        return princessBehavior;
    }

    protected void updatePresetFields() {
        selfPreservationSlidebar.setValue(princessBehavior.getSelfPreservationIndex());
        aggressionSlidebar.setValue(princessBehavior.getHyperAggressionIndex());
        fallShameSlidebar.setValue(princessBehavior.getFallShameIndex());
        herdingSlidebar.setValue(princessBehavior.getHerdMentalityIndex());
        braverySlidebar.setValue(princessBehavior.getBraveryIndex());
    }
    
    private void updateDialogFields() {
        updatePresetFields();

        forcedWithdrawalCheck.setSelected(princessBehavior.isForcedWithdrawal());
        withdrawEdgeCombo.setSelectedItem(princessBehavior.getRetreatEdge());
        
        autoFleeCheck.setSelected(princessBehavior.shouldAutoFlee());
        fleeEdgeCombo.setSelectedItem(princessBehavior.getDestinationEdge());
        
        targetsListModel.clear();
        for (String t : princessBehavior.getStrategicBuildingTargets()) {
            int begin = t.indexOf("(") + 1;
            int end = t.indexOf(")");
            String[] tokens = t.substring(begin, end).split(",");
            if (tokens.length == 2) {
                int x = Integer.parseInt(tokens[0].strip());
                int y = Integer.parseInt(tokens[1].strip());
                targetsListModel.addElement(new Coords(x, y));
            }
        }
        targetsListModel.addAll(princessBehavior.getPriorityUnitTargets());
        updateEnabledStates();
    }
    
    /** Updates all necessary enabled states of buttons/dropdowns. */
    private void updateEnabledStates() {
        fleeEdgeLabel.setEnabled(autoFleeCheck.isSelected());
        fleeEdgeCombo.setEnabled(autoFleeCheck.isSelected());
        withdrawEdgeLabel.setEnabled(forcedWithdrawalCheck.isSelected());
        withdrawEdgeCombo.setEnabled(forcedWithdrawalCheck.isSelected());
        savePreset.setEnabled(isChangedPreset());
        removeTargetButton.setEnabled(!targetsList.isSelectionEmpty());
    }
    
    /** Returns true if a preset is selected and is different from the current slider settings. */
    private boolean isChangedPreset() {
        return (chosenPreset != null) 
                && (chosenPreset.getSelfPreservationIndex() != selfPreservationSlidebar.getValue()
                || chosenPreset.getHyperAggressionIndex() != aggressionSlidebar.getValue()
                || chosenPreset.getFallShameIndex() != fallShameSlidebar.getValue()
                || chosenPreset.getHerdMentalityIndex() != herdingSlidebar.getValue()
                || chosenPreset.getBraveryIndex() != braverySlidebar.getValue());
    }

    private JPanel buildSlider(JSlider thisSlider, String minMsgProperty, 
            String maxMsgProperty, String toolTip, String title) {
        var result = new TipPanel();
        result.setLayout(new BoxLayout(result, BoxLayout.PAGE_AXIS));
        result.setToolTipText(toolTip);
        thisSlider.setToolTipText(toolTip);
        thisSlider.setPaintLabels(false);
        thisSlider.setSnapToTicks(true);
        thisSlider.addChangeListener(this);
        
        var panLabels = new JPanel();
        panLabels.setLayout(new BoxLayout(panLabels, BoxLayout.LINE_AXIS));
        panLabels.add(new JLabel(minMsgProperty, SwingConstants.LEFT));
        panLabels.add(Box.createHorizontalGlue());
        panLabels.add(new JLabel(maxMsgProperty, SwingConstants.RIGHT));
        
        result.add(panLabels);
        result.add(thisSlider);
        return result;
    }

    @Override
    protected JPanel createButtonPanel() {
        JPanel result = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));

        butOK.addActionListener(this::okButtonActionPerformed);
        butOK.setMnemonic(KeyEvent.VK_K);
        result.add(butOK);
        
        butCancel.addActionListener(this::cancelActionPerformed);
        butCancel.setMnemonic(KeyEvent.VK_C);
        result.add(butCancel);
        
        princessHelpButton.addActionListener(this);
        princessHelpButton.setMnemonic(KeyEvent.VK_H);
        result.add(princessHelpButton);

        return result;
    }
    
    private void showPrincessHelp() {
        new PrincessHelpDialog(getFrame()).setVisible(true);
    }
    
    @Override
    protected void okButtonActionPerformed(ActionEvent evt) {
        // Only allow adding the bot if its name is unique. It does not strictly have 
        // to be unique among all players as the server will make it unique by
        // adding ".2" when necessary. But it has to be unique among local bots as otherwise
        // the connection between the client.bots stored name and the server-given name 
        // gets lost. It doesn't hurt to check against all players though.
        boolean playerNameTaken = (client != null) && client.getGame().getPlayersVector().stream()
                .anyMatch(p -> p.getName().equals(nameField.getText()));
        if (isNewBot && playerNameTaken) {
            JOptionPane.showMessageDialog(getFrame(), Messages.getString("ChatLounge.AlertExistsBot.message"));
        } else {
            super.okButtonActionPerformed(evt);
        }
    }
    
    @Override
    protected void okAction() {
        savePrincessProperties();
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == addTargetButton) {
            var dlg = new BotConfigTargetHexDialog(getFrame(), clientGui);
            dlg.setVisible(true);
            if (dlg.getResult() == DialogResult.CONFIRMED) {
                dlg.getSelectedCoords().stream().filter(c -> !targetsListModel.contains(c)).forEach(targetsListModel::addElement);
            }

        } else if (e.getSource() == addUnitButton) {
            var dlg = new BotConfigTargetUnitDialog(getFrame());
            dlg.setVisible(true);
            if (dlg.getResult() == DialogResult.CONFIRMED) {
                dlg.getSelectedIDs().stream().filter(c -> !targetsListModel.contains(c)).forEach(targetsListModel::addElement);
            }

        } else if (e.getSource() == removeTargetButton) {
            for (Object target : targetsList.getSelectedValuesList()) {
                targetsListModel.removeElement(target);
            }

        } else if (e.getSource() == autoFleeCheck) {
            updateEnabledStates();
            
        } else if (e.getSource() == forcedWithdrawalCheck) {
            updateEnabledStates();
            
        } else if (e.getSource() == princessHelpButton) {
            showPrincessHelp();
            
        } else if (e.getSource() == savePreset) {
            savePreset();
            
        } else if (e.getSource() == saveNewPreset) {
            saveAsNewPreset();
            
        }
    }

    /** Asks for a name and adds the current Behavior as a new Behavior Preset. */
    private void saveAsNewPreset() {
        while (true) {
            String name = JOptionPane.showInputDialog(getFrame(), Messages.getString("BotConfigDialog.saveNewPrompt"));
            if (name == null || name.isBlank()) {
                return;
            }
            if (!behaviorSettingsFactory.getBehaviorNameList().contains(name)) {
                // OK: this name is not taken. Save the preset 
                writePreset(name);
                updatePresets();
                presetsList.setSelectedValue(name, true);
                return;
            }
            // Incorrect name: notify the player and ask again
            JOptionPane.showMessageDialog(getFrame(), Messages.getString("BotConfigDialog.saveNewTaken"));
        }
    }
    
    /** Saves the current Behavior to the currently selected Behavior Preset. */
    private void savePreset() {
        if (!presetsList.isSelectionEmpty()) {
            writePreset(presetsList.getSelectedValue());
            updatePresets();
        }
    }
    
    /** 
     * Writes the current Behavior under the given name to the stored Behavior Presets.
     * Will overwrite a Behavior Preset if there is one with the same name.
     */
    private void writePreset(String name) {
        BehaviorSettings newBehavior = new BehaviorSettings();
        try {
            newBehavior.setDescription(name);
        } catch (PrincessException e1) {
            return;
        }
        newBehavior.setFallShameIndex(fallShameSlidebar.getValue());
        newBehavior.setHyperAggressionIndex(aggressionSlidebar.getValue());
        newBehavior.setSelfPreservationIndex(selfPreservationSlidebar.getValue());
        newBehavior.setHerdMentalityIndex(herdingSlidebar.getValue());
        newBehavior.setBraveryIndex(braverySlidebar.getValue());
        behaviorSettingsFactory.addBehavior(newBehavior);
        behaviorSettingsFactory.saveBehaviorSettings(false);
    }

    /** Removes the given Behavior Preset. */
    private void removePreset(String name) {
        behaviorSettingsFactory.removeBehavior(name);
        behaviorSettingsFactory.saveBehaviorSettings(false);
        updatePresets();
    }
    
    /** Copies the Configuration from another local bot player. */
    private void copyFromOtherBot(String botName) {
        var bc = client.bots.get(botName);
        if (bc instanceof Princess) {
            try {
                princessBehavior = ((Princess) bc).getBehaviorSettings().getCopy();
                updateDialogFields();
            } catch (Exception e) {
                LogManager.getLogger().error("", e);
            }
        }
    }
    
    private void savePrincessProperties() {
        BehaviorSettings tempBehavior = new BehaviorSettings();
        tempBehavior.setFallShameIndex(fallShameSlidebar.getValue());
        tempBehavior.setForcedWithdrawal(forcedWithdrawalCheck.isSelected());
        tempBehavior.setAutoFlee(autoFleeCheck.isSelected());
        tempBehavior.setDestinationEdge(fleeEdgeCombo.getSelectedItem());
        tempBehavior.setRetreatEdge(withdrawEdgeCombo.getSelectedItem());
        tempBehavior.setHyperAggressionIndex(aggressionSlidebar.getValue());
        tempBehavior.setSelfPreservationIndex(selfPreservationSlidebar.getValue());
        tempBehavior.setHerdMentalityIndex(herdingSlidebar.getValue());
        tempBehavior.setBraveryIndex(braverySlidebar.getValue());
        for (int i = 0; i < targetsListModel.getSize(); i++) {
            if (targetsListModel.get(i) instanceof Coords) {
                tempBehavior.addStrategicTarget(targetsListModel.get(i).toString());
            } else {
                tempBehavior.addPriorityUnit(Integer.toString((int) targetsListModel.get(i)));
            }
        }
        princessBehavior = tempBehavior;
    }

    public String getBotName() {
        return nameField.getText();
    }
    
    public void setBotName(String value) {
        nameField.setText(value);
    }
    
    @Override
    public void valueChanged(ListSelectionEvent event) {
        if (event.getValueIsAdjusting()) {
            return;
        }
        
        if (event.getSource() == targetsList.getSelectionModel()) {
            updateEnabledStates();
            
        } else if (event.getSource() == presetsList) {
            presetSelected();
        }
    }
    
    /** Shows a popup menu for a behavior preset, allowing to delete it. */
    private MouseListener presetsMouseListener = new MouseAdapter() {
        
        @Override
        public void mouseReleased(MouseEvent e) {
            int row = presetsList.locationToIndex(e.getPoint());
            if (e.isPopupTrigger() && (row != -1)) {
                ScalingPopup popup = new ScalingPopup();
                String behavior = presetsList.getModel().getElementAt(row);
                var deleteItem = new JMenuItem("Delete " + behavior);
                deleteItem.addActionListener(event -> removePreset(behavior));
                popup.add(deleteItem);
                popup.show(e.getComponent(), e.getX(), e.getY());
            }
        }
        
        @Override
        public void mouseClicked(MouseEvent evt) {
            if (SwingUtilities.isLeftMouseButton(evt) && evt.getClickCount() == 1) {
                presetSelected();
            }
        }
    };
    
    /** 
     * Called when a Preset is selected. This will often be called twice when clicking
     * with the mouse (by the listselectionlistener and the mouselistener). In this way
     * the list will react when copying a Preset from another bot and then clicking the 
     * already selected Preset again. And it will also react to keyboard navigation.
     */
    private void presetSelected() {
        if (presetsList.isSelectionEmpty()) {
            chosenPreset = null;
        } else {
            if (presetsList.getSelectedValue().equals(SAVEGAME_CONFIG)) {
                princessBehavior = saveGameBehavior;
                // A savegame Configuration cannot be saved to:
                chosenPreset = null;
                updateDialogFields();
            } else if (presetsList.getSelectedValue().startsWith(UIUtil.BOT_MARKER)) {
                String name = presetsList.getSelectedValue().substring(UIUtil.BOT_MARKER.length());
                copyFromOtherBot(name);
                // Another bot's Configuration cannot be saved to:
                chosenPreset = null;
            } else {
                princessBehavior = behaviorSettingsFactory.getBehavior(presetsList.getSelectedValue());
                chosenPreset = behaviorSettingsFactory.getBehavior(presetsList.getSelectedValue());
            }
            if (princessBehavior == null) {
                princessBehavior = new BehaviorSettings();
            }
            updatePresetFields();
        }
        updateEnabledStates();
    }
    

    /** Sets up/Updates the displayed preset list (e.g. after adding or deleting a preset) */
    private void updatePresets() {
        presets = new ArrayList<>(Arrays.asList(behaviorSettingsFactory.getBehaviorNames()));
        
        // Add the Configuration from a save game, if any to the top of the list
        if (saveGameBehavior != null) {
            presets.add(0, SAVEGAME_CONFIG);
        }
        
        // Other local bot Configurations
        if (client != null) {
            // Find if there actually are other bots 
            Set<String> otherBots = new HashSet<>(client.bots.keySet());
            if (fixedBotPlayerName != null) {
                otherBots.remove(fixedBotPlayerName);
            }
            // If there are, add a list entry for each
            for (String otherName : otherBots) {
                presets.add(UIUtil.BOT_MARKER + otherName);
            }
        }
        ((PresetsModel) presetsList.getModel()).fireUpdate();
    }
    
    private class PresetsModel extends DefaultListModel<String> {
        
        @Override
        public int getSize() {
            return presets.size();
        }

        @Override
        public String getElementAt(int index) {
            return presets.get(index);
        }
        
        /** Call when elements of the list change. */
        private void fireUpdate() {
            fireContentsChanged(this, 0, getSize() - 1);
        }
    }

    /** 
     * A renderer for the Behavior Presets list. Adapts the font size to the gui scaling and
     * colors the special list elements (other bot Configurations and original Config). 
     */
    private class PresetsRenderer extends DefaultListCellRenderer {

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
                boolean cellHasFocus) {
            Component comp = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            comp.setFont(UIUtil.getScaledFont());
            String preset = (String) value;
            if (preset.startsWith(UIUtil.BOT_MARKER)) {
                comp.setForeground(UIUtil.uiLightBlue());
            }
            if (preset.equals(SAVEGAME_CONFIG)) {
                comp.setForeground(UIUtil.uiGreen());
            }
            return comp;
        }
    }
    
    /** A renderer for the strategic targets list. */
    private class TargetsRenderer extends DefaultListCellRenderer {

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
                boolean cellHasFocus) {
            
            String content;
            boolean invalid = true;
            if (value instanceof Coords) {
                Coords coords = (Coords) value;
                content = Messages.getString("BotConfigDialog.hexListIntro", coords.getX() + 1, coords.getY() + 1);
                if (client != null) {
                    Board board = client.getBoard();
                    if (client.getGame().getPhase() == GamePhase.LOUNGE) {
                        board = clientGui.chatlounge.getPossibleGameBoard(true);
                    }
                    if (board == null) {
                        content += Messages.getString("BotConfigDialog.hexListNoMp");
                    } else if (!board.contains(coords)) {
                        content += Messages.getString("BotConfigDialog.hexListOuts");
                    } else if (!board.getHex(coords).containsAnyTerrainOf(BUILDING, FUEL_TANK, BRIDGE)) {
                        content += Messages.getString("BotConfigDialog.hexListNoBg");
                    } else {
                        final Hex hex = board.getHex(coords); 
                        final Building bldg = board.getBuildingAt(coords);
                        if (hex.containsTerrain(BUILDING)) {
                            content += Messages.getString("BotConfigDialog.hexListBldg", Building.typeName(bldg.getType()),
                                    Building.className(bldg.getBldgClass()), hex.terrainLevel(BLDG_ELEV), hex.terrainLevel(BLDG_CF));
                        } else if (hex.containsTerrain(FUEL_TANK)) {
                            content += Messages.getString("BotConfigDialog.hexListFuel", 
                                    hex.terrainLevel(FUEL_TANK_CF), hex.terrainLevel(FUEL_TANK_MAGN));
                        } else {
                            content += Messages.getString("BotConfigDialog.hexListBrdg", Building.typeName(bldg.getType()),
                                    hex.terrainLevel(BRIDGE_ELEV), hex.terrainLevel(BRIDGE_CF));
                        }
                        invalid = false;
                    }
                } else {
                    content += Messages.getString("BotConfigDialog.hexListNoMp");
                }
            } else {
                int unitID = (int) value;
                if (client != null && client.getGame().getEntity(unitID) != null) {
                    Entity entity = client.getGame().getEntity(unitID);
                    content = Messages.getString("BotConfigDialog.unitListEntry", unitID, entity.getShortNameRaw());
                    invalid = false;
                } else {
                    content = Messages.getString("BotConfigDialog.unitListEntryNone", unitID);
                }
            }
            Component comp = super.getListCellRendererComponent(list, content, index, isSelected, cellHasFocus);
            comp.setForeground(invalid ? UIUtil.uiGray() : null);
            return comp;
        }
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        updateEnabledStates();
    }
}