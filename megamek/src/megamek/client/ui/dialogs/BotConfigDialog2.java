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

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
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
import megamek.client.ui.swing.util.UIUtil;
import megamek.client.ui.swing.util.UIUtil.Content;
import megamek.client.ui.swing.util.UIUtil.FixedYPanel;
import megamek.client.ui.swing.util.UIUtil.OptionPanel;
import megamek.client.ui.swing.util.UIUtil.TipCombo;
import megamek.client.ui.swing.util.UIUtil.TipTextField;
import megamek.common.IPlayer;
import megamek.common.logging.LogLevel;

/** A dialog box to configure (Princess) bot properties. */
public class BotConfigDialog2 extends AbstractButtonDialog implements ActionListener, ListSelectionListener {

    private static final String PRINCESS_PANEL = "princess_config";
    private static final String TESTBOT_PANEL = "testbot_config";
    private static final String UNIT_TARGET = Messages.getString("BotConfigDialog.targetUnit");
    private static final String BUILDING_TARGET = Messages.getString("BotConfigDialog.targetBuilding");
    private static final int TOOLTIP_WIDTH = 300;
    private static final long serialVersionUID = -544663266637225925L;

    private JList<String> playersToReplace;

    private BehaviorSettingsFactory behaviorSettingsFactory = BehaviorSettingsFactory.getInstance();
    protected BehaviorSettings princessBehavior;

    // Items for princess config here
    protected JComboBox<String> verbosityCombo;
    private JTextField targetId;
    private JComboBox<String> targetTypeCombo;
    private JButton addTargetButton;
    private JButton removeTargetButton;
    private JButton princessHelpButton = new JButton(Messages.getString("BotConfigDialog.princessHelpButtonCaption"));
    private JList<String> targetsList;
    private DefaultListModel<String> targetsListModel = new DefaultListModel<>();
    protected MMToggleButton forcedWithdrawalCheck;
    protected MMToggleButton autoFleeCheck;
    protected JComboBox<CardinalEdge> homeEdgeCombo; // The board edge to to which the bot will attempt to move.
    protected JComboBox<CardinalEdge> retreatEdgeCombo; // The board edge to be used in a forced withdrawal.
    protected JSlider aggressionSlidebar;
    protected JSlider fallShameSlidebar;
    protected JSlider herdingSlidebar;
    protected JSlider selfPreservationSlidebar;
    protected JSlider braverySlidebar;
    private TipCombo<String> princessBehaviorNames;

    protected TipTextField nameField;
    public boolean dialogAborted = true; // did user not click Ok button?

    // Are we replacing an existing player?
    private final Set<IPlayer> ghostPlayers = new HashSet<>();
    private final boolean replacePlayer;
    private final boolean replaceSinglePlayer;

    protected final JButton butOK = new JButton(Messages.getString("Okay")); //$NON-NLS-1$

    private JPanel botSpecificCardsPanel;

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
        botSpecificCardsPanel = new JPanel(new CardLayout());
        botSpecificCardsPanel.add(new JPanel(), TESTBOT_PANEL);
        var princessScroll = new JScrollPane(princessPanel());
        princessScroll.getVerticalScrollBar().setUnitIncrement(16);
        botSpecificCardsPanel.add(princessScroll, PRINCESS_PANEL);
        CardLayout cardlayout = (CardLayout) (botSpecificCardsPanel.getLayout());
        cardlayout.show(botSpecificCardsPanel, PRINCESS_PANEL);
        return botSpecificCardsPanel;
    }
    
    private JPanel princessPanel() {
        JPanel result = new JPanel();
        result.setLayout(new BoxLayout(result, BoxLayout.PAGE_AXIS));
        result.add(headerSection());
        result.add(behaviorSection());
        result.add(targetsSection());
        result.add(retreatSection());
        result.add(devSection());
        return result;
    }
    
    private JPanel behaviorSection() {
        JPanel result = new OptionPanel("BotConfigDialog.behaviorSection");
        Content panContent = new Content(new GridLayout(6, 1, 0, 5));
        result.add(panContent);
        
        var choosePanel = new JPanel();
        princessBehaviorNames = new TipCombo<>(behaviorSettingsFactory.getBehaviorNames(), this);
        princessBehaviorNames.setSelectedItem(BehaviorSettingsFactory.DEFAULT_BEHAVIOR_DESCRIPTION);
        princessBehaviorNames.setToolTipText(formatTooltip(Messages.getString("BotConfigDialog.behaviorToolTip")));
        princessBehaviorNames.addActionListener(this);
//        princessBehaviorNames.setEditable(true);
        
        JLabel behaviorNameLabel = new JLabel(Messages.getString("BotConfigDialog.behaviorNameLabel"));
        choosePanel.add(behaviorNameLabel);
        choosePanel.add(princessBehaviorNames);
        panContent.add(choosePanel);

        braverySlidebar = buildSlider(Messages.getString("BotConfigDialog.braverySliderMin"),
                                      Messages.getString("BotConfigDialog.braverySliderMax"),
                                      Messages.getString("BotConfigDialog.braveryTooltip"),
                                      Messages.getString("BotConfigDialog.braverySliderTitle"));
        panContent.add(braverySlidebar);

        selfPreservationSlidebar = buildSlider(Messages.getString("BotConfigDialog.selfPreservationSliderMin"),
                                               Messages.getString("BotConfigDialog.selfPreservationSliderMax"),
                                               Messages.getString("BotConfigDialog.selfPreservationTooltip"),
                                               Messages.getString("BotConfigDialog.selfPreservationSliderTitle"));
        panContent.add(selfPreservationSlidebar);

        aggressionSlidebar = buildSlider(Messages.getString("BotConfigDialog.aggressionSliderMin"),
                                         Messages.getString("BotConfigDialog.aggressionSliderMax"),
                                         Messages.getString("BotConfigDialog.aggressionTooltip"),
                                         Messages.getString("BotConfigDialog.aggressionSliderTitle"));
        panContent.add(aggressionSlidebar);

        herdingSlidebar = buildSlider(Messages.getString("BotConfigDialog.herdingSliderMin"),
                                      Messages.getString("BotConfigDialog.herdingSliderMax"),
                                      Messages.getString("BotConfigDialog.herdingToolTip"),
                                      Messages.getString("BotConfigDialog.herdingSliderTitle"));
        panContent.add(herdingSlidebar);

        fallShameSlidebar = buildSlider(Messages.getString("BotConfigDialog.fallShameSliderMin"),
                                        Messages.getString("BotConfigDialog.fallShameSliderMax"),
                                        Messages.getString("BotConfigDialog.fallShameToolTip"),
                                        Messages.getString("BotConfigDialog.fallShameSliderTitle"));
        panContent.add(fallShameSlidebar);
        return result;
    }
    
    private JPanel targetsSection() {
        JPanel result = new OptionPanel("BotConfigDialog.targetsSection");
        Content panContent = new Content();
        panContent.setLayout(new BoxLayout(panContent, BoxLayout.PAGE_AXIS));
        result.add(panContent);

        JPanel typePanel = new JPanel(new GridLayout(1, 3, 20, 0));
        panContent.add(typePanel);
        
        targetTypeCombo = new JComboBox<>(new String[] { BUILDING_TARGET, UNIT_TARGET });
        targetTypeCombo.setToolTipText(formatTooltip(Messages.getString("BotConfigDialog.targetTypeTooltip")));
        targetTypeCombo.setSelectedIndex(0);
        typePanel.add(targetTypeCombo);

        targetId = new JTextField();
        targetId.setToolTipText(formatTooltip(Messages.getString("BotConfigDialog.princessTargetIdToolTip")));
        targetId.setColumns(4);
        typePanel.add(targetId);
        
        addTargetButton = new JButton(Messages.getString("BotConfigDialog.princessAddTargetButtonCaption"));
        addTargetButton.setToolTipText(formatTooltip(Messages.getString("BotConfigDialog.princessAddTargetButtonToolTip")));
        addTargetButton.addActionListener(this);
        typePanel.add(addTargetButton);

        JPanel listPanel = new JPanel(new GridLayout(1, 3, 20, 0));
        panContent.add(listPanel);
        
        targetsList = new JList<>(targetsListModel);
        targetsList.setToolTipText(formatTooltip(Messages.getString("BotConfigDialog.princessStrategicTargetsToolTip")));
        targetsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        targetsList.getSelectionModel().addListSelectionListener(this);
        targetsList.setLayoutOrientation(JList.VERTICAL);
        JScrollPane targetScroller = new JScrollPane(targetsList);
        listPanel.add(targetScroller);
        
        removeTargetButton = new JButton(Messages.getString("BotConfigDialog.princessRemoveTargetButtonCaption"));
        removeTargetButton.setToolTipText(formatTooltip(Messages.getString("BotConfigDialog.princessRemoveTargetButtonToolTip")));
        removeTargetButton.addActionListener(this);
        listPanel.add(removeTargetButton);
        
        return result;
    }
    
    private JPanel retreatSection() {
        JPanel result = new OptionPanel("BotConfigDialog.retreatSection");
        Content panContent = new Content();
        panContent.setLayout(new BoxLayout(panContent, BoxLayout.PAGE_AXIS));
        result.add(panContent);

        autoFleeCheck = new MMToggleButton(Messages.getString("BotConfigDialog.autoFleeCheck"));
        autoFleeCheck.setToolTipText(formatTooltip(formatTooltip(Messages.getString("BotConfigDialog.autoFleeTooltip"))));
        autoFleeCheck.addActionListener(this);
        autoFleeCheck.setEnabled(false);
        
        var homeEdgeLabel = new JLabel(Messages.getString("BotConfigDialog.homeEdgeLabel"));

        homeEdgeCombo = new JComboBox<>(CardinalEdge.values());
        homeEdgeCombo.setToolTipText(formatTooltip(Messages.getString("BotConfigDialog.homeEdgeTooltip")));
        homeEdgeCombo.setSelectedIndex(0);
        homeEdgeCombo.addActionListener(this);

        forcedWithdrawalCheck = new MMToggleButton(Messages.getString("BotConfigDialog.forcedWithdrawalCheck"));
        forcedWithdrawalCheck.setToolTipText(formatTooltip(Messages.getString("BotConfigDialog.forcedWithdrawalTooltip")));

        var retreatEdgeLabel = new JLabel(Messages.getString("BotConfigDialog.retreatEdgeLabel"));
        
        retreatEdgeCombo = new JComboBox<>(CardinalEdge.values());
        retreatEdgeCombo.setToolTipText(formatTooltip(Messages.getString("BotConfigDialog.retreatEdgeTooltip")));
        retreatEdgeCombo.setSelectedIndex(0);

        var firstLine = new JPanel(new FlowLayout(FlowLayout.LEFT));
        var secondLine = new JPanel(new FlowLayout(FlowLayout.LEFT));
        firstLine.add(autoFleeCheck);
        firstLine.add(Box.createHorizontalStrut(20));
        firstLine.add(homeEdgeLabel);
        firstLine.add(homeEdgeCombo);
        secondLine.add(forcedWithdrawalCheck);
        secondLine.add(Box.createHorizontalStrut(20));
        secondLine.add(retreatEdgeLabel);
        secondLine.add(retreatEdgeCombo);
        panContent.add(firstLine);
        panContent.add(Box.createVerticalStrut(5));
        panContent.add(secondLine);
        
        return result;
    }
    
    private JPanel devSection() {
        JPanel result = new OptionPanel("BotConfigDialog.debugSection");
        Content panContent = new Content(new FlowLayout(FlowLayout.LEFT));
        result.add(panContent);

        var verbosityLabel = new JLabel(Messages.getString("BotConfigDialog.verbosityLabel"), SwingConstants.RIGHT);
        panContent.add(verbosityLabel);

        verbosityCombo = new JComboBox<>(LogLevel.getLogLevelNames());
        verbosityCombo.setToolTipText(formatTooltip(Messages.getString("BotConfigDialog.verbosityToolTip")));
        verbosityCombo.setSelectedIndex(0);
        panContent.add(verbosityCombo);
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
        princessBehavior = behaviorSettingsFactory.getBehavior((String) princessBehaviorNames.getSelectedItem());
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
        forcedWithdrawalCheck.setSelected(princessBehavior.isForcedWithdrawal());
        autoFleeCheck.setSelected(princessBehavior.shouldAutoFlee());
        selfPreservationSlidebar.setValue(princessBehavior.getSelfPreservationIndex());
        aggressionSlidebar.setValue(princessBehavior.getHyperAggressionIndex());
        fallShameSlidebar.setValue(princessBehavior.getFallShameIndex());
        homeEdgeCombo.setSelectedItem(princessBehavior.getDestinationEdge());
        retreatEdgeCombo.setSelectedItem(princessBehavior.getRetreatEdge());
        herdingSlidebar.setValue(princessBehavior.getHerdMentalityIndex());
        braverySlidebar.setValue(princessBehavior.getBraveryIndex());
        targetsListModel.clear();
        for (String t : princessBehavior.getStrategicBuildingTargets()) {
            //noinspection unchecked
            targetsListModel.addElement(BUILDING_TARGET + ": " + t);            
        }
        for (int id : princessBehavior.getPriorityUnitTargets()) {
            targetsListModel.addElement(UNIT_TARGET + ": " + id);
        }
    }

    private JLabel buildSliderLabel(String caption) {
        JLabel label = new JLabel(caption);
        label.setFont(UIUtil.getScaledFont());
        return label;
    }

    private JSlider buildSlider(String minMsgProperty, String maxMsgProperty, String toolTip, String title) {
        JSlider thisSlider = new JSlider(SwingConstants.HORIZONTAL, 0, 10, 5);
        Hashtable<Integer, JLabel> sliderLabels = new Hashtable<Integer, JLabel>(3);
        sliderLabels.put(0, buildSliderLabel("0 - " + minMsgProperty));
        sliderLabels.put(10, buildSliderLabel("10 - " + maxMsgProperty));
        sliderLabels.put(5, buildSliderLabel("5"));
        thisSlider.setToolTipText(formatTooltip(toolTip));
        thisSlider.setLabelTable(sliderLabels);
        thisSlider.setPaintLabels(true);
        thisSlider.setMinorTickSpacing(1);
        thisSlider.setMajorTickSpacing(2);
        thisSlider.setSnapToTicks(true);
        var border = new TitledBorder(new LineBorder(Color.black), title);
        border.setTitleFont(UIUtil.getScaledFont());
        thisSlider.setBorder(border);
        return thisSlider;
    }

    @Override
    protected JPanel createButtonPanel() {
        JPanel result = new JPanel(new FlowLayout(FlowLayout.CENTER, 2, 2));

        var namepanel = new JPanel();
        namepanel.add(new JLabel(Messages.getString("BotConfigDialog.nameLabel")));
        nameField = new TipTextField("Princess", 12, this);
        nameField.setToolTipText(formatTooltip(Messages.getString("BotConfigDialog.namefield.tooltip")));
        namepanel.add(nameField);
        result.add(namepanel);
        if (!replacePlayer && !replaceSinglePlayer) {
            nameField.setText("Princess");
        } else if (replaceSinglePlayer) {
            nameField.setText(ghostPlayers.stream().findFirst().get().getName());
            nameField.setEnabled(false);
        }

        butOK.addActionListener(this);
        result.add(butOK);
        
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
        if (butOK.equals(e.getSource())) {
            dialogAborted = false;
            savePrincessProperties();
            setVisible(false);

        } else if (addTargetButton.equals(e.getSource())) {
            String data = targetTypeCombo.getSelectedItem() + ": " + targetId.getText();
            targetsListModel.addElement(data);

        } else if (removeTargetButton.equals(e.getSource())) {
            targetsListModel.removeElementAt(targetsList.getSelectedIndex());

        } else if (princessBehaviorNames.equals(e.getSource())) {
            getPresetPrincessBehavior();
            setPrincessFields();
            
        } else if (homeEdgeCombo.equals(e.getSource())) {
            if (homeEdgeCombo.getSelectedItem() == CardinalEdge.NONE) {
                autoFleeCheck.setSelected(false);
                autoFleeCheck.setEnabled(false);
            } else {
                autoFleeCheck.setEnabled(true);
                autoFleeCheck.setSelected(true);
            }
            
        } else if (princessHelpButton.equals(e.getSource())) {
            showPrincessHelp();
        }
    }

    private void handleError(Throwable t) {
        JOptionPane.showMessageDialog(this, t.getMessage(), "ERROR", JOptionPane.ERROR_MESSAGE);
        MegaMek.getLogger().error(t);
    }

    private void savePrincessProperties() {
        BehaviorSettings tempBehavior = new BehaviorSettings();

        try {
            tempBehavior.setDescription((String) princessBehaviorNames.getSelectedItem());
        } catch (PrincessException e) {
            handleError(e);
        }
        tempBehavior.setFallShameIndex(fallShameSlidebar.getValue());
        tempBehavior.setForcedWithdrawal(forcedWithdrawalCheck.isSelected());
        tempBehavior.setAutoFlee(autoFleeCheck.isSelected());
        tempBehavior.setDestinationEdge((CardinalEdge) homeEdgeCombo.getSelectedItem());
        tempBehavior.setRetreatEdge((CardinalEdge) retreatEdgeCombo.getSelectedItem());
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
        
        if (event.getSource().equals(targetsList.getSelectionModel())) {
            removeTargetButton.setEnabled(!targetsList.isSelectionEmpty());
        }
    }
}

