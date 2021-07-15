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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.*;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import megamek.MegaMek;
import megamek.client.bot.BotClient;
import megamek.client.bot.princess.*;
import megamek.client.ui.Messages;
import megamek.client.ui.baseComponents.AbstractButtonDialog;
import megamek.client.ui.swing.*;
import megamek.client.ui.swing.util.ScalingPopup;
import megamek.client.ui.swing.util.UIUtil;
import megamek.client.ui.swing.util.UIUtil.*;
import megamek.common.Configuration;
import megamek.common.IPlayer;
import megamek.common.logging.LogLevel;
import megamek.common.util.ImageUtil;
import megamek.common.util.fileUtils.MegaMekFile;

/** A dialog box to configure (Princess) bot properties. */
public class BotConfigDialog2 extends AbstractButtonDialog implements ActionListener, ListSelectionListener {

    private static final String UNIT_TARGET = Messages.getString("BotConfigDialog.targetUnit");
    private static final String BUILDING_TARGET = Messages.getString("BotConfigDialog.targetBuilding");
    private static final int TOOLTIP_WIDTH = 300;
    private static final long serialVersionUID = -544663266637225925L;

    private JList<String> playersToReplace;

    private BehaviorSettingsFactory behaviorSettingsFactory = BehaviorSettingsFactory.getInstance();
    private BehaviorSettings princessBehavior;

    // Items for princess config here
    private JComboBox<String> verbosityCombo;
    private JTextField targetId;
    private JComboBox<String> targetTypeCombo;
    private JButton addTargetButton;
    private JButton addUnitButton;
    private JButton removeTargetButton;
    private JButton princessHelpButton = new JButton(Messages.getString("BotConfigDialog.princessHelpButtonCaption"));
    private JButton savePreset = new JButton("Save");
    private JButton saveNewPreset = new JButton("Save as New Preset...");
    private JList<String> targetsList;
    private DefaultListModel<String> targetsListModel = new DefaultListModel<>();
    private MMToggleButton forcedWithdrawalCheck;
    private MMToggleButton autoFleeCheck;
    private JComboBox<CardinalEdge> homeEdgeCombo; // The board edge to to which the bot will attempt to move.
    private JComboBox<CardinalEdge> retreatEdgeCombo; // The board edge to be used in a forced withdrawal.
    private TipSlider aggressionSlidebar = new TipSlider(SwingConstants.HORIZONTAL, 0, 10, 5);
    private TipSlider fallShameSlidebar = new TipSlider(SwingConstants.HORIZONTAL, 0, 10, 5);
    private TipSlider herdingSlidebar = new TipSlider(SwingConstants.HORIZONTAL, 0, 10, 5);
    private TipSlider selfPreservationSlidebar = new TipSlider(SwingConstants.HORIZONTAL, 0, 10, 5);
    private TipSlider braverySlidebar = new TipSlider(SwingConstants.HORIZONTAL, 0, 10, 5);
    private TipCombo<String> princessBehaviorNames;
    private JPanel presetsPanel;
    
    /** A copy of the current presets. Modifications will only be saved when accepted. */
    private List<String> presets = new ArrayList<String>(Arrays.asList(behaviorSettingsFactory.getBehaviorNames()));
            
    private PresetsModel presetsModel = new PresetsModel();
    private JList<String> presetsList = new JList<>(presetsModel);

    protected TipTextField nameField;
    public boolean dialogAborted = true; // did user not click Ok button?

    // Are we replacing an existing player?
    private final Set<IPlayer> ghostPlayers = new HashSet<>();
    private final boolean replacePlayer;
    private final boolean replaceSinglePlayer;

    private final JButton butOK = new JButton(Messages.getString("Okay"));
    private final JButton butCancel = new JButton(Messages.getString("Cancel"));
    
    /** True when the player has added/removed to any of the presets. */
    private boolean presetsChanged = false;

    public BotConfigDialog2(JFrame parent) {
        this(parent, new HashSet<>());
    }
    
    public BotConfigDialog2(JFrame parent, BotClient existingBot, boolean allowNameEdit) {
        this(parent, new HashSet<>());
        
        if (existingBot instanceof Princess) {
            this.princessBehavior = ((Princess) existingBot).getBehaviorSettings();
            nameField.setText(existingBot.getName());
            nameField.setEnabled(allowNameEdit);
            setPrincessFields();
        }
    }

    public BotConfigDialog2(JFrame parent, Set<IPlayer> ghosts) {
        super(parent, "Ok.text", "Ok.text");

        if (ghosts != null) {
            ghostPlayers.addAll(ghosts);
        }
        replacePlayer = !ghostPlayers.isEmpty();
        replaceSinglePlayer = false;
        initialize();
        UIUtil.adjustDialog(getContentPane());
    }
    
    public BotConfigDialog2(JFrame parent, IPlayer toReplace) {
        super(parent, "Ok.text", "Ok.text");
        Objects.requireNonNull(toReplace);
        replacePlayer = false;
        replaceSinglePlayer = true;
        ghostPlayers.add(toReplace);
        initialize();
        UIUtil.adjustDialog(getContentPane());
    }
    
    @Override
    protected void initialize() {
        super.initialize();
        // preset a default behavior
        getPresetPrincessBehavior();
        setPrincessFields();
    }

    @Override
    protected Container createCenterPane() {
        JPanel result = new JPanel();
        result.setLayout(new BoxLayout(result, BoxLayout.PAGE_AXIS));
        result.add(headerSection());
        result.add(nameSection());
        result.add(settingSection());
        return result;
    }
    
    private JPanel princessPanel() {
        JPanel result = new JPanel();
        result.setLayout(new BoxLayout(result, BoxLayout.PAGE_AXIS));
        result.add(behaviorSection());
        result.add(retreatSection());
        result.add(targetsSection());
        return result;
    }
    
    private JPanel settingSection() {
        var princessScroll = new JScrollPane(princessPanel());
        princessScroll.getVerticalScrollBar().setUnitIncrement(16);
        presetsPanel = presetsPanel();
        
        var result = new JPanel(new BorderLayout(0, 0));
        result.setAlignmentX(LEFT_ALIGNMENT);
        result.add(princessScroll, BorderLayout.CENTER);
        result.add(presetsPanel, BorderLayout.LINE_START);
        return result;
    }
    
    private JPanel nameSection() {
        JPanel result = new OptionPanel("BotConfigDialog.nameSection");
        Content panContent = new Content();
        panContent.setLayout(new BoxLayout(panContent, BoxLayout.PAGE_AXIS));
        result.add(panContent);

        
        var namePanel = new JPanel();
        nameField = new TipTextField("Princess", 12, this);
        nameField.setToolTipText(formatTooltip(Messages.getString("BotConfigDialog.namefield.tooltip")));
        if (!replacePlayer && !replaceSinglePlayer) {
            nameField.setText("Princess");
        } else if (replaceSinglePlayer) {
            nameField.setText(ghostPlayers.stream().findFirst().get().getName());
            nameField.setEnabled(false);
        }
        namePanel.add(new JLabel(Messages.getString("BotConfigDialog.nameLabel")));
        namePanel.add(nameField);

        var verbosityPanel = new JPanel();

        verbosityCombo = new TipCombo<String>(LogLevel.getLogLevelNames());
        verbosityCombo.setToolTipText(formatTooltip(Messages.getString("BotConfigDialog.verbosityToolTip")));
        verbosityCombo.setSelectedIndex(0);
        verbosityPanel.add(new JLabel(Messages.getString("BotConfigDialog.verbosityLabel"), SwingConstants.RIGHT));
        verbosityPanel.add(verbosityCombo);
        
        panContent.add(namePanel);
        panContent.add(verbosityPanel);
        return result;
    }
    
    private JPanel presetsPanel() {
        var result = new JPanel();
        result.setLayout(new BoxLayout(result, BoxLayout.PAGE_AXIS));
        result.setBorder(new EmptyBorder(0, 10, 0, 20));
        
        princessBehaviorNames = new TipCombo<>(behaviorSettingsFactory.getBehaviorNames());
//        princessBehaviorNames.setSelectedItem(BehaviorSettingsFactory.DEFAULT_BEHAVIOR_DESCRIPTION);
//        princessBehaviorNames.setToolTipText(formatTooltip(Messages.getString("BotConfigDialog.behaviorToolTip")));
//        princessBehaviorNames.addActionListener(this);
//        princessBehaviorNames.setMaximumRowCount(30);
//        princessBehaviorNames.setEditable(true);
        
        JLabel behaviorNameLabel = new JLabel(Messages.getString("BotConfigDialog.behaviorNameLabel"));
//        choosePanel.add(behaviorNameLabel);
//        choosePanel.add(princessBehaviorNames);
//        panContent.add(choosePanel);
        
        var chooseLabel = new JLabel("Choose Preset:");
        chooseLabel.setAlignmentX(CENTER_ALIGNMENT);
        
        presetsList.addListSelectionListener(this);
        presetsList.setCellRenderer(new PresetsRenderer());
        presetsList.addMouseListener(presetsMouseListener);
        result.add(chooseLabel);
        result.add(Box.createVerticalStrut(10));
        result.add(presetsList);
        
        return result;
    }
    
    private JPanel behaviorSection() {
        JPanel result = new OptionPanel("BotConfigDialog.behaviorSection");
        Content panContent = new Content();
        panContent.setLayout(new BoxLayout(panContent, BoxLayout.PAGE_AXIS));
        result.add(panContent);
        
        var choosePanel = new JPanel();
        princessBehaviorNames = new TipCombo<>(behaviorSettingsFactory.getBehaviorNames());
        princessBehaviorNames.setSelectedItem(BehaviorSettingsFactory.DEFAULT_BEHAVIOR_DESCRIPTION);
        princessBehaviorNames.setToolTipText(formatTooltip(Messages.getString("BotConfigDialog.behaviorToolTip")));
        princessBehaviorNames.addActionListener(this);
        princessBehaviorNames.setMaximumRowCount(400);
//        princessBehaviorNames.setEditable(true);
        
        JLabel behaviorNameLabel = new JLabel(Messages.getString("BotConfigDialog.behaviorNameLabel"));
        choosePanel.add(behaviorNameLabel);
        choosePanel.add(princessBehaviorNames);
//        panContent.add(choosePanel);
        
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
        
//        var buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        var buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        buttonPanel.setAlignmentX(SwingConstants.CENTER);
        result.add(buttonPanel);
        
        savePreset.addActionListener(this);
        buttonPanel.add(savePreset);
        savePreset.addActionListener(this);
        buttonPanel.add(saveNewPreset);

        return result;
    }
    
    private JPanel targetsSection() {
        JPanel result = new OptionPanel("BotConfigDialog.targetsSection");
        Content panContent = new Content();
        panContent.setLayout(new BoxLayout(panContent, BoxLayout.LINE_AXIS));
        result.add(panContent);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.PAGE_AXIS));
        
        targetTypeCombo = new JComboBox<>(new String[] { BUILDING_TARGET, UNIT_TARGET });
        targetTypeCombo.setToolTipText(formatTooltip(Messages.getString("BotConfigDialog.targetTypeTooltip")));
        targetTypeCombo.setSelectedIndex(0);
        buttonPanel.add(targetTypeCombo);

        targetId = new JTextField();
        targetId.setToolTipText(formatTooltip(Messages.getString("BotConfigDialog.princessTargetIdToolTip")));
        targetId.setColumns(4);
        buttonPanel.add(targetId);
        
//        addTargetButton = new JButton(Messages.getString("BotConfigDialog.princessAddTargetButtonCaption"));
        addTargetButton = new TipButton("+H");
        addTargetButton.setToolTipText(formatTooltip(Messages.getString("BotConfigDialog.princessAddTargetButtonToolTip")));
        addTargetButton.addActionListener(this);
        buttonPanel.add(addTargetButton);
        
        addUnitButton = new TipButton("+U");
        addUnitButton.setToolTipText(formatTooltip(Messages.getString("BotConfigDialog.princessAddTargetButtonToolTip")));
        addUnitButton.addActionListener(this);
        buttonPanel.add(addUnitButton);

        targetsList = new JList<>(targetsListModel);
        targetsList.setToolTipText(formatTooltip(Messages.getString("BotConfigDialog.princessStrategicTargetsToolTip")));
        targetsList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        targetsList.getSelectionModel().addListSelectionListener(this);
        targetsList.setLayoutOrientation(JList.VERTICAL);
        targetsList.setCellRenderer(new TargetsRenderer());
        JScrollPane targetScroller = new JScrollPane(targetsList);
        panContent.add(targetScroller);
        
//        removeTargetButton = prepareButton("buttonRemT", "Delete Terrain");
        removeTargetButton = new JButton("\u274C");
        removeTargetButton.setFont(UIUtil.getScaledFont());
        removeTargetButton.setForeground(GUIPreferences.getInstance().getWarningColor());
        removeTargetButton.setToolTipText(formatTooltip(Messages.getString("BotConfigDialog.princessRemoveTargetButtonToolTip")));
        removeTargetButton.addActionListener(this);
        buttonPanel.add(removeTargetButton);
        
        panContent.add(buttonPanel);
        return result;
    }
    
    private JPanel retreatSection() {
        JPanel result = new OptionPanel("BotConfigDialog.retreatSection");
        Content panContent = new Content();
        panContent.setLayout(new BoxLayout(panContent, BoxLayout.PAGE_AXIS));
        result.add(panContent);

        autoFleeCheck = new TipMMToggleButton(Messages.getString("BotConfigDialog.autoFleeCheck"));
        autoFleeCheck.setToolTipText(formatTooltip(formatTooltip(Messages.getString("BotConfigDialog.autoFleeTooltip"))));
        autoFleeCheck.addActionListener(this);
        
        var homeEdgeLabel = new JLabel(Messages.getString("BotConfigDialog.homeEdgeLabel"));

        homeEdgeCombo = new TipCombo<>(CardinalEdge.values());
        homeEdgeCombo.removeItem(CardinalEdge.NONE);
        homeEdgeCombo.setToolTipText(formatTooltip(Messages.getString("BotConfigDialog.homeEdgeTooltip")));
        homeEdgeCombo.setSelectedIndex(0);
        homeEdgeCombo.addActionListener(this);

        forcedWithdrawalCheck = new TipMMToggleButton(Messages.getString("BotConfigDialog.forcedWithdrawalCheck"));
        forcedWithdrawalCheck.setToolTipText(formatTooltip(Messages.getString("BotConfigDialog.forcedWithdrawalTooltip")));
        forcedWithdrawalCheck.addActionListener(this);

        var retreatEdgeLabel = new JLabel(Messages.getString("BotConfigDialog.retreatEdgeLabel"));
        
        retreatEdgeCombo = new TipCombo<>(CardinalEdge.values());
        retreatEdgeCombo.removeItem(CardinalEdge.NONE);
        retreatEdgeCombo.setToolTipText(formatTooltip(Messages.getString("BotConfigDialog.retreatEdgeTooltip")));
        retreatEdgeCombo.setSelectedIndex(0);

        var firstLine = new JPanel(new FlowLayout(FlowLayout.LEFT));
        var secondLine = new JPanel(new FlowLayout(FlowLayout.LEFT));
        firstLine.add(forcedWithdrawalCheck);
        firstLine.add(Box.createHorizontalStrut(20));
        firstLine.add(retreatEdgeLabel);
        firstLine.add(retreatEdgeCombo);
        secondLine.add(autoFleeCheck);
        secondLine.add(Box.createHorizontalStrut(20));
        secondLine.add(homeEdgeLabel);
        secondLine.add(homeEdgeCombo);
        panContent.add(firstLine);
        panContent.add(Box.createVerticalStrut(5));
        panContent.add(secondLine);
        
        return result;
    }
    
    private JPanel headerSection() {
        JPanel result = new FixedYPanel();
        result.setAlignmentX(Component.LEFT_ALIGNMENT);
//        Icon playerIcon = client.getLocalPlayer().getCamouflage().getImageIcon(UIUtil.scaleForGUI(40));
        JLabel playerLabel = new JLabel("Configure Princess", SwingConstants.CENTER);
        playerLabel.setIconTextGap(UIUtil.scaleForGUI(12));
        playerLabel.setBorder(new EmptyBorder(15, 0, 10, 0));
        result.add(playerLabel);
        return result;
    }


    private JPanel selectPlayerToReplacePanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        String title = Messages.getString("BotConfigDialog.replacePlayerLabel") + "  ";
        TitledBorder border = new TitledBorder(new LineBorder(Color.black, 1),
                                               Messages.getString("BotConfigDialog.replacePlayerLabel"),
                                               TitledBorder.LEFT,
                                               TitledBorder.DEFAULT_POSITION);
        panel.setBorder(border);
        String longestEntry = title;

        Vector<String> playerList = new Vector<>(ghostPlayers.size());
        for (IPlayer p : ghostPlayers) {
            playerList.add(p.getName());
            if (p.getName().length() > longestEntry.length()) {
                longestEntry = p.getName() + "  ";
            }
        }
        playersToReplace = new JList<>(playerList);
        int minWidth = (int) playersToReplace
                .getFontMetrics(playersToReplace.getFont())
                .getStringBounds(longestEntry, null).getWidth();
        playersToReplace.setSelectedIndex(0);
        playersToReplace.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        playersToReplace.setPreferredSize(new Dimension(minWidth, playersToReplace.getPreferredSize().height));
        panel.add(playersToReplace);

        panel.add(Box.createVerticalGlue());

        return panel;
    }

    private boolean getPresetPrincessBehavior() {
        princessBehavior = behaviorSettingsFactory.getBehavior(presetsList.getSelectedValue());
        if (princessBehavior == null) {
            princessBehavior = new BehaviorSettings();
            return false;
        }
        return true;
    }
    
    public BehaviorSettings getBehaviorSettings() {
        return princessBehavior;
    }

    protected void setPrincessFields() {
        verbosityCombo.setSelectedIndex(0);
        selfPreservationSlidebar.setValue(princessBehavior.getSelfPreservationIndex());
        aggressionSlidebar.setValue(princessBehavior.getHyperAggressionIndex());
        fallShameSlidebar.setValue(princessBehavior.getFallShameIndex());
        herdingSlidebar.setValue(princessBehavior.getHerdMentalityIndex());
        braverySlidebar.setValue(princessBehavior.getBraveryIndex());
//        forcedWithdrawalCheck.setSelected(princessBehavior.isForcedWithdrawal());
//        autoFleeCheck.setSelected(princessBehavior.shouldAutoFlee());
//        homeEdgeCombo.setSelectedItem(princessBehavior.getDestinationEdge());
//        homeEdgeCombo.setEnabled(autoFleeCheck.isSelected());
//        retreatEdgeCombo.setSelectedItem(princessBehavior.getRetreatEdge());
//        retreatEdgeCombo.setEnabled(forcedWithdrawalCheck.isSelected());
//        targetsListModel.clear();
//        for (String t : princessBehavior.getStrategicBuildingTargets()) {
//            //noinspection unchecked
//            targetsListModel.addElement(BUILDING_TARGET + ": " + t);            
//        }
//        for (int id : princessBehavior.getPriorityUnitTargets()) {
//            targetsListModel.addElement(UNIT_TARGET + ": " + id);
//        }
    }

    private JPanel buildSlider(JSlider thisSlider, String minMsgProperty, String maxMsgProperty, String toolTip, String title) {
        var result = new TipPanel();
        result.setLayout(new BoxLayout(result, BoxLayout.PAGE_AXIS));
        result.setToolTipText(formatTooltip(toolTip));
        thisSlider.setToolTipText(formatTooltip(toolTip));
        thisSlider.setPaintLabels(false);
        thisSlider.setSnapToTicks(true);
        
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
        JPanel result = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 2));

        butOK.addActionListener(this);
        result.add(butOK);
        
        butCancel.addActionListener(this);
        result.add(butCancel);
        
        princessHelpButton.addActionListener(this);
        result.add(princessHelpButton);


        return result;
    }
    
    private void showPrincessHelp() {
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
            handleError(e);
        }
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == butOK) {
            dialogAborted = false;
            savePrincessProperties();
            setVisible(false);

        } else if (e.getSource() == butCancel) {
            if (presetsChanged) {
                JOptionPane.showOptionDialog(this, "Keep changes to the presets?", 
                        "Presets changed", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, 
                        null, new Object[] {"Keep","Discard"}, "Discard");
            }
            dialogAborted = true;
            setVisible(false);

        } else if (e.getSource() == addTargetButton) {
            String data = targetTypeCombo.getSelectedItem() + ": " + targetId.getText();
            targetsListModel.addElement(data);

        } else if (e.getSource() == removeTargetButton) {
            targetsListModel.removeElementAt(targetsList.getSelectedIndex());

        } else if (e.getSource() == autoFleeCheck) {
            homeEdgeCombo.setEnabled(autoFleeCheck.isSelected());
            
        } else if (e.getSource() == forcedWithdrawalCheck) {
            retreatEdgeCombo.setEnabled(forcedWithdrawalCheck.isSelected());
            
        } else if (e.getSource() == princessHelpButton) {
            showPrincessHelp();
            
        } else if (e.getSource() == savePreset) {
            addBehaviorPreset();
            
        }
    }

    private void handleError(Throwable t) {
        JOptionPane.showMessageDialog(this, t.getMessage(), "ERROR", JOptionPane.ERROR_MESSAGE);
        MegaMek.getLogger().error(t);
    }

    private void savePrincessProperties() {
        BehaviorSettings tempBehavior = new BehaviorSettings();

        try {
            tempBehavior.setDescription(presetsList.getSelectedValue());
        } catch (PrincessException e) {
            handleError(e);
        }
        tempBehavior.setFallShameIndex(fallShameSlidebar.getValue());
//        tempBehavior.setForcedWithdrawal(forcedWithdrawalCheck.isSelected());
//        tempBehavior.setAutoFlee(autoFleeCheck.isSelected());
//        tempBehavior.setDestinationEdge((CardinalEdge) homeEdgeCombo.getSelectedItem());
//        tempBehavior.setRetreatEdge((CardinalEdge) retreatEdgeCombo.getSelectedItem());
        tempBehavior.setHyperAggressionIndex(aggressionSlidebar.getValue());
        tempBehavior.setSelfPreservationIndex(selfPreservationSlidebar.getValue());
        tempBehavior.setHerdMentalityIndex(herdingSlidebar.getValue());
        tempBehavior.setBraveryIndex(braverySlidebar.getValue());
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

    /**
     * gets the selected, configured bot from the dialog
     *
     * @param host The game server's host address.
     * @param port The gme server's host port.
     * @return A new bot-controlled client.
     */
    public BotClient getSelectedBot(String host, int port) {
        Princess result = new Princess(getBotName(), host, port,
                LogLevel.getLogLevel((String) verbosityCombo.getSelectedItem()));
        result.setBehaviorSettings(princessBehavior);
        result.getLogger().debug(result.getBehaviorSettings().toLog());
        return result;
    }

    public String getBotName() {
        if (replacePlayer) {
            return playersToReplace.getSelectedValuesList().get(0);
        }
        return nameField.getText();
    }

    Collection<String> getPlayerToReplace() {
        if (!replacePlayer) {
            return new HashSet<>(0);
        }
        return playersToReplace.getSelectedValuesList();
    }
    
    /** Completes the tooltip for this dialog, setting its width and adding HTML tags. */
    private String formatTooltip(String text) {
        String result = "<P WIDTH=" + UIUtil.scaleForGUI(TOOLTIP_WIDTH) + " style=padding:5>" + text;
        return UIUtil.scaleStringForGUI(result);
    }

    @Override
    public void valueChanged(ListSelectionEvent event) {
        if (event.getValueIsAdjusting()) {
            return;
        }
        
        if (event.getSource() == targetsList.getSelectionModel()) {
            removeTargetButton.setEnabled(!targetsList.isSelectionEmpty());
            
        } else if (event.getSource() == presetsList) {
            if (presetsList.isSelectionEmpty()) { // OR selectedBehavior == chosenBehavior
                savePreset.setEnabled(false);
            } else {
                savePreset.setEnabled(true);
                getPresetPrincessBehavior();
                setPrincessFields();
            }
        }
    }

    /** Returns a 84x72 icon with the text "No Camo". */
//    private Icon noCamoIcon() {
//        var result = new BufferedImage(84, 72, BufferedImage.TYPE_INT_RGB);
//        Graphics2D graphics = result.createGraphics();
//        GUIPreferences.AntiAliasifSet(graphics);
//        graphics.setFont(new Font("Dialog", Font.PLAIN, 18));
//        BoardView1.drawCenteredText(graphics, "No", 42, 23, new Color(180, 180, 200, 120), true);
//        BoardView1.drawCenteredText(graphics, "Camo", 42, 44, new Color(180, 180, 200, 120), true);
//        return new ImageIcon(result);
//    }
    
    /**
     * Sets up Scaling Icon Buttons
     */
    private JButton prepareButton(String iconName, String buttonName) {
        // Get the normal icon
        File file = new MegaMekFile(Configuration.widgetsDir(), "/MapEditor/"+iconName+".png").getFile();
        Image imageButton = ImageUtil.loadImageFromFile(file.getAbsolutePath());
        if (imageButton == null) {
            imageButton = ImageUtil.failStandardImage();
        }
        JButton button = new JButton(new ImageIcon(imageButton));

        // Get the hover icon
        file = new MegaMekFile(Configuration.widgetsDir(), "/MapEditor/"+iconName+"_H.png").getFile();
        imageButton = ImageUtil.loadImageFromFile(file.getAbsolutePath());
        button.setRolloverIcon(new ImageIcon(imageButton));
        
        // Get the disabled icon, if any
        file = new MegaMekFile(Configuration.widgetsDir(), "/MapEditor/"+iconName+"_G.png").getFile();
        imageButton = ImageUtil.loadImageFromFile(file.getAbsolutePath());
        button.setDisabledIcon(new ImageIcon(imageButton));

        String tt = Messages.getString("BoardEditor."+iconName+"TT");
        if (tt.length() != 0) {
            button.setToolTipText(tt);
        }
        button.setMargin(new Insets(0,0,0,0));
        button.addActionListener(this);
        return button;
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
                deleteItem.addActionListener(event -> removeBehaviorPreset(behavior));
                popup.add(deleteItem);
                popup.show(e.getComponent(), e.getX(), e.getY());
            }
        };
    };

    private void removeBehaviorPreset(String name) {
        if (presets.remove(name)) {
            presetsChanged = true;
            updatePresets();
        }
    }
    
    private void addBehaviorPreset() {
        savePrincessProperties();
        updatePresets();
        presetsChanged = true;
    }

    private void updatePresets() {
         ((PresetsModel)presetsList.getModel()).fireUpdate();
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

    /** A renderer for the behavior presets list adapting the font size to the gui scaling. */
    private class PresetsRenderer extends DefaultListCellRenderer {

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
                boolean cellHasFocus) {
            Component comp = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            comp.setFont(UIUtil.getScaledFont());
            return comp;
        }
    }
    
    /** A renderer for the strategic targets list. */
    private class TargetsRenderer extends DefaultListCellRenderer {

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
                boolean cellHasFocus) {
            Component comp = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            comp.setFont(UIUtil.getScaledFont());
            return comp;
        }
    }
    
}

