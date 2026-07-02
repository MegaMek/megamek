/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */

package megamek.client.ui.panels.phaseDisplay.lobby;

import java.awt.Container;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;

import java.util.Enumeration;
import java.util.Set;
import java.util.Vector;

import com.fasterxml.jackson.databind.node.ObjectNode;
import megamek.client.ui.Messages;
import megamek.client.ui.clientGUI.ClientGUI;
import megamek.client.ui.clientGUI.DialogOptionListener;
import megamek.client.ui.dialogs.buttonDialogs.AbstractButtonDialog;
import megamek.client.ui.panels.DialogOptionComponentYPanel;
import megamek.client.ui.util.UIUtil.FixedYPanel;
import megamek.common.Player;
import megamek.common.annotations.Nullable;
import megamek.common.board.Coords;
import megamek.common.equipment.ICarryable;
import megamek.common.equipment.ObjectiveMarker;
import megamek.common.jacksonAdapters.VictoryConditionsBuilder;
import megamek.common.jacksonAdapters.VictoryConditionsBuilder.ConditionToken;
import megamek.common.jacksonAdapters.VictoryConditionsBuilder.OperatorToken;
import megamek.common.jacksonAdapters.VictoryConditionsBuilder.Token;
import megamek.common.options.IBasicOption;
import megamek.common.options.IOption;
import megamek.common.options.IOptionGroup;
import megamek.common.options.OptionsConstants;

/**
 * The unified lobby victory conditions dialog. It holds the classic victory game options (formerly the Victory tab
 * of the game options dialog - turn limit, BV thresholds, kill count, objective scoring etc.) and the victory
 * condition builder: conditions strung together as a formula of condition leaves, {@code and}, {@code or},
 * {@code not} and parentheses, like the advanced search formula builder. Each completed formula is added to the
 * conditions list with the side it awards victory to (or a draw) and whether it ends the game. On OK, the caller
 * sends the changed options (see {@link megamek.client.Client#sendGameOptions(String, Vector)}) and the compiled
 * conditions (see {@link megamek.client.Client#sendVictoryConditions(String, String)}).
 */
public class VictoryConditionsDialog extends AbstractButtonDialog implements DialogOptionListener {

    /**
     * The name of the victory game options group (see {@link megamek.common.options.GameOptions}). Note that option
     * groups added with the single-argument {@code addGroup} have this as their name; their key is empty.
     */
    public static final String VICTORY_OPTIONS_GROUP_NAME = "victory";

    /**
     * The victory options NOT shown in this dialog: these condition-style options are superseded by the condition
     * formula builder (their trigger equivalents can be freely combined with and/or/not), leaving only true settings
     * in the options section. The options themselves remain functional for the legacy achieve-N-conditions path.
     */
    private static final Set<String> OPTIONS_SUPERSEDED_BY_FORMULAS = Set.of(
          OptionsConstants.VICTORY_CHECK_VICTORY,
          OptionsConstants.VICTORY_ACHIEVE_CONDITIONS,
          OptionsConstants.VICTORY_USE_BV_DESTROYED,
          OptionsConstants.VICTORY_BV_DESTROYED_PERCENT,
          OptionsConstants.VICTORY_USE_BV_RATIO,
          OptionsConstants.VICTORY_BV_RATIO_PERCENT,
          OptionsConstants.VICTORY_USE_KILL_COUNT,
          OptionsConstants.VICTORY_GAME_KILL_COUNT,
          OptionsConstants.VICTORY_COMMANDER_KILLED);

    /** The formula leaf types offered by the builder. */
    private enum ConditionType {
        OBJECTIVE_CONTROLLED("objectiveControlled", true, true, false),
        OBJECTIVE_DESTROYED("objectiveDestroyed", true, false, false),
        OBJECTIVE_CONFIRMED("objectiveConfirmed", true, false, false),
        OBJECTIVE_CAPTURED("objectiveCaptured", true, true, false),
        VICTORY_POINTS("victoryPoints", false, true, true),
        BV_DESTROYED("bvDestroyed", false, true, true),
        BV_RATIO("bvRatio", false, true, true),
        KILL_COUNT("killCount", false, true, true),
        COMMANDERS_KILLED("commandersKilled", false, true, false),
        ROUND_END("roundEnd", false, false, true),
        UNITS_KILLED("unitsKilled", false, true, true),
        UNITS_FLED("unitsFled", false, true, true),
        BATTLEFIELD_CONTROL("battlefieldControl", false, false, false);

        private final String messageKey;
        private final boolean needsObjective;
        private final boolean needsPlayer;
        private final boolean needsNumber;

        ConditionType(String messageKey, boolean needsObjective, boolean needsPlayer, boolean needsNumber) {
            this.messageKey = messageKey;
            this.needsObjective = needsObjective;
            this.needsPlayer = needsPlayer;
            this.needsNumber = needsNumber;
        }

        @Override
        public String toString() {
            return Messages.getString("VictoryConditionsDialog.type." + messageKey);
        }

        ConditionToken buildToken(String objectiveName, @Nullable String playerName, int number) {
            return switch (this) {
                case OBJECTIVE_CONTROLLED -> VictoryConditionsBuilder.objectiveControlled(objectiveName, playerName);
                case OBJECTIVE_DESTROYED -> VictoryConditionsBuilder.objectiveDestroyed(objectiveName);
                case OBJECTIVE_CONFIRMED -> VictoryConditionsBuilder.objectiveConfirmed(objectiveName);
                case OBJECTIVE_CAPTURED -> VictoryConditionsBuilder.objectiveCaptured(objectiveName, playerName);
                case VICTORY_POINTS -> VictoryConditionsBuilder.victoryPointsReached(playerName, number);
                case BV_DESTROYED -> VictoryConditionsBuilder.enemyBvDestroyed(playerName, number);
                case BV_RATIO -> VictoryConditionsBuilder.bvRatioReached(playerName, number);
                case KILL_COUNT -> VictoryConditionsBuilder.killCountReached(playerName, number);
                case COMMANDERS_KILLED -> VictoryConditionsBuilder.enemyCommandersKilled(playerName);
                case ROUND_END -> VictoryConditionsBuilder.roundEndReached(number);
                case UNITS_KILLED -> VictoryConditionsBuilder.unitsKilled(playerName, number);
                case UNITS_FLED -> VictoryConditionsBuilder.unitsFled(playerName, number);
                case BATTLEFIELD_CONTROL -> VictoryConditionsBuilder.battlefieldControl();
            };
        }
    }

    /**
     * A completed victory condition: its formula, the winning player ({@code null} = draw condition) and whether it
     * is only checked when the game has ended for another reason.
     */
    private record ConditionEntry(List<Token> formula, @Nullable String winnerName, boolean onlyAtEnd) {}

    private final ClientGUI clientGui;

    private final List<ConditionEntry> conditionEntries = new ArrayList<>();
    private final DefaultListModel<String> conditionListModel = new DefaultListModel<>();
    private final JList<String> conditionsList = new JList<>(conditionListModel);
    private final JButton butRemoveSelected =
          new JButton(Messages.getString("VictoryConditionsDialog.removeSelected"));

    private final List<Token> currentFormula = new ArrayList<>();
    private final JTextArea formulaDisplay = new JTextArea("", 2, 40);

    private final JComboBox<ConditionType> comboConditionType = new JComboBox<>(ConditionType.values());
    private final JTextField fieldObjectiveName = new JTextField(10);
    private final JComboBox<String> comboLeafPlayer = new JComboBox<>();
    private final JSpinner spinnerNumber = new JSpinner(new SpinnerNumberModel(1, 1, 999, 1));
    private final JButton butAddLeaf = new JButton(Messages.getString("VictoryConditionsDialog.addCondition"));

    private final JButton butAnd = new JButton(Messages.getString("VictoryConditionsDialog.and"));
    private final JButton butOr = new JButton(Messages.getString("VictoryConditionsDialog.or"));
    private final JButton butNot = new JButton(Messages.getString("VictoryConditionsDialog.not"));
    private final JButton butLeftParen = new JButton("(");
    private final JButton butRightParen = new JButton(")");
    private final JButton butBackspace = new JButton(Messages.getString("VictoryConditionsDialog.backspace"));
    private final JButton butClearFormula = new JButton(Messages.getString("VictoryConditionsDialog.clear"));

    private final JComboBox<String> comboWinner = new JComboBox<>();
    private final JCheckBox checkOnlyAtEnd = new JCheckBox(Messages.getString("VictoryConditionsDialog.onlyAtEnd"));
    private final JButton butAddToList = new JButton(Messages.getString("VictoryConditionsDialog.addToList"));

    private final JTextField fieldPassword = new JTextField(15);

    private final JPanel victoryOptionsPanel = new JPanel();
    private final List<DialogOptionComponentYPanel> victoryOptionComps = new ArrayList<>();

    private final List<ObjectiveMarker> objectiveMarkersInList = new ArrayList<>();
    private final DefaultListModel<String> objectivesListModel = new DefaultListModel<>();
    private final JList<String> objectivesList = new JList<>(objectivesListModel);
    private final JTextField fieldNewObjectiveName = new JTextField(10);
    private final JComboBox<String> comboObjectiveOwner = new JComboBox<>();
    private final JSpinner spinnerObjectiveX = new JSpinner(new SpinnerNumberModel(1, 0, 999, 1));
    private final JSpinner spinnerObjectiveY = new JSpinner(new SpinnerNumberModel(1, 0, 999, 1));
    private final JSpinner spinnerObjectiveRadius =
          new JSpinner(new SpinnerNumberModel(0, 0, ObjectiveMarker.MAX_CONTROL_RADIUS, 1));
    private final JSpinner spinnerObjectiveVP = new JSpinner(new SpinnerNumberModel(1, 1, 99, 1));
    private final JCheckBox checkObjectivePotential =
          new JCheckBox(Messages.getString("VictoryConditionsDialog.variantPotential"));
    private final JCheckBox checkObjectiveFalse =
          new JCheckBox(Messages.getString("VictoryConditionsDialog.variantFalse"));
    private final JCheckBox checkObjectiveFragile =
          new JCheckBox(Messages.getString("VictoryConditionsDialog.variantFragile"));
    private final JCheckBox checkObjectiveMobile =
          new JCheckBox(Messages.getString("VictoryConditionsDialog.variantMobile"));
    private final JCheckBox checkObjectiveDestructible =
          new JCheckBox(Messages.getString("VictoryConditionsDialog.destructible"), true);
    private final JButton butAddObjective = new JButton(Messages.getString("VictoryConditionsDialog.addObjective"));
    private final JButton butRemoveObjective =
          new JButton(Messages.getString("VictoryConditionsDialog.removeObjective"));

    public VictoryConditionsDialog(ClientGUI clientGui) {
        super(clientGui.getFrame(), "VictoryConditionsDialog", "VictoryConditionsDialog.title");
        this.clientGui = clientGui;
        refreshLobbyState();
        initialize();
        refreshFormulaControls();
    }

    /** Refreshes the player choices and victory option values from the game; call before showing the dialog. */
    public void refreshLobbyState() {
        String anySide = Messages.getString("VictoryConditionsDialog.anySide");
        String draw = Messages.getString("VictoryConditionsDialog.draw");
        comboLeafPlayer.removeAllItems();
        comboLeafPlayer.addItem(anySide);
        comboWinner.removeAllItems();
        comboWinner.addItem(draw);
        comboObjectiveOwner.removeAllItems();
        for (Player player : clientGui.getClient().getGame().getPlayersList()) {
            comboLeafPlayer.addItem(player.getName());
            comboWinner.addItem(player.getName());
            comboObjectiveOwner.addItem(player.getName());
        }
        refreshVictoryOptions();
        refreshObjectivesList();
    }

    /** Rebuilds the objectives list from the local player's not-yet-placed objective markers. */
    private void refreshObjectivesList() {
        objectiveMarkersInList.clear();
        objectivesListModel.clear();
        for (ICarryable groundObject : clientGui.getClient().getLocalPlayer().getGroundObjectsToPlace()) {
            if (groundObject instanceof ObjectiveMarker marker) {
                objectiveMarkersInList.add(marker);
                objectivesListModel.addElement(describeMarker(marker));
            }
        }
    }

    private String describeMarker(ObjectiveMarker marker) {
        StringBuilder description = new StringBuilder(marker.generalName());
        Player owner = clientGui.getClient().getGame().getPlayer(marker.getOwnerId());
        if (owner != null) {
            description.append(" (").append(owner.getName()).append(")");
        }
        if (marker.getLobbyPosition() != null) {
            description.append(" at ").append(marker.getLobbyPosition().toFriendlyString());
        }
        description.append(", radius ").append(marker.getControlRadius());
        description.append(", ").append(marker.getVictoryPointValue()).append(" VP");
        if (marker.isPotential()) {
            description.append(", potential");
        }
        if (marker.isFalseObjective()) {
            description.append(", false");
        }
        if (marker.isFragile()) {
            description.append(", fragile");
        }
        if (marker.isMobile()) {
            description.append(", mobile");
        }
        if (marker.isInvulnerable()) {
            description.append(", indestructible");
        }
        return description.toString();
    }

    /** Rebuilds the victory game option rows (the former Victory tab contents) from the current game options. */
    private void refreshVictoryOptions() {
        victoryOptionsPanel.removeAll();
        victoryOptionComps.clear();
        for (Enumeration<IOptionGroup> groups = clientGui.getClient().getGame().getOptions().getGroups();
              groups.hasMoreElements(); ) {
            IOptionGroup group = groups.nextElement();
            if (!VICTORY_OPTIONS_GROUP_NAME.equals(group.getName())) {
                continue;
            }
            for (Enumeration<IOption> optionsEnumeration = group.getOptions();
                  optionsEnumeration.hasMoreElements(); ) {
                IOption option = optionsEnumeration.nextElement();
                if (OPTIONS_SUPERSEDED_BY_FORMULAS.contains(option.getName())) {
                    continue;
                }
                DialogOptionComponentYPanel optionComponent = new DialogOptionComponentYPanel(this, option, true);
                victoryOptionComps.add(optionComponent);
                victoryOptionsPanel.add(optionComponent);
            }
        }
        victoryOptionsPanel.revalidate();
    }

    @Override
    protected Container createCenterPane() {
        JPanel result = new JPanel();
        result.setLayout(new BoxLayout(result, BoxLayout.PAGE_AXIS));
        result.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        victoryOptionsPanel.setLayout(new BoxLayout(victoryOptionsPanel, BoxLayout.PAGE_AXIS));
        JScrollPane victoryOptionsScroll = new JScrollPane(victoryOptionsPanel);
        victoryOptionsScroll.setBorder(BorderFactory.createTitledBorder(
              Messages.getString("VictoryConditionsDialog.victoryOptions")));

        objectivesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        objectivesList.setVisibleRowCount(4);
        JPanel objectivesPanel = new JPanel();
        objectivesPanel.setLayout(new BoxLayout(objectivesPanel, BoxLayout.PAGE_AXIS));
        objectivesPanel.setBorder(BorderFactory.createTitledBorder(
              Messages.getString("VictoryConditionsDialog.objectives")));
        objectivesPanel.add(new JScrollPane(objectivesList));
        JPanel objectiveEditorPanel = new FixedYPanel();
        objectiveEditorPanel.add(new JLabel(Messages.getString("VictoryConditionsDialog.objective")));
        objectiveEditorPanel.add(fieldNewObjectiveName);
        objectiveEditorPanel.add(new JLabel(Messages.getString("VictoryConditionsDialog.owner")));
        objectiveEditorPanel.add(comboObjectiveOwner);
        objectiveEditorPanel.add(new JLabel("X:"));
        objectiveEditorPanel.add(spinnerObjectiveX);
        objectiveEditorPanel.add(new JLabel("Y:"));
        objectiveEditorPanel.add(spinnerObjectiveY);
        objectiveEditorPanel.add(new JLabel(Messages.getString("VictoryConditionsDialog.radius")));
        objectiveEditorPanel.add(spinnerObjectiveRadius);
        objectiveEditorPanel.add(new JLabel(Messages.getString("VictoryConditionsDialog.vp")));
        objectiveEditorPanel.add(spinnerObjectiveVP);
        JPanel objectiveVariantsPanel = new FixedYPanel();
        objectiveVariantsPanel.add(checkObjectivePotential);
        objectiveVariantsPanel.add(checkObjectiveFalse);
        objectiveVariantsPanel.add(checkObjectiveFragile);
        objectiveVariantsPanel.add(checkObjectiveMobile);
        objectiveVariantsPanel.add(checkObjectiveDestructible);
        objectiveVariantsPanel.add(butAddObjective);
        objectiveVariantsPanel.add(butRemoveObjective);
        objectivesPanel.add(objectiveEditorPanel);
        objectivesPanel.add(objectiveVariantsPanel);

        conditionsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        conditionsList.setVisibleRowCount(5);
        JPanel conditionsPanel = new JPanel();
        conditionsPanel.setLayout(new BoxLayout(conditionsPanel, BoxLayout.PAGE_AXIS));
        conditionsPanel.setBorder(BorderFactory.createTitledBorder(
              Messages.getString("VictoryConditionsDialog.conditions")));
        conditionsPanel.add(new JScrollPane(conditionsList));
        JPanel removePanel = new FixedYPanel();
        removePanel.add(butRemoveSelected);
        conditionsPanel.add(removePanel);

        JPanel builderPanel = new JPanel();
        builderPanel.setLayout(new BoxLayout(builderPanel, BoxLayout.PAGE_AXIS));
        builderPanel.setBorder(BorderFactory.createTitledBorder(
              Messages.getString("VictoryConditionsDialog.buildCondition")));

        JPanel leafPanel = new FixedYPanel();
        leafPanel.add(comboConditionType);
        leafPanel.add(new JLabel(Messages.getString("VictoryConditionsDialog.objective")));
        leafPanel.add(fieldObjectiveName);
        leafPanel.add(new JLabel(Messages.getString("VictoryConditionsDialog.player")));
        leafPanel.add(comboLeafPlayer);
        leafPanel.add(new JLabel(Messages.getString("VictoryConditionsDialog.number")));
        leafPanel.add(spinnerNumber);
        leafPanel.add(butAddLeaf);

        JPanel operatorPanel = new FixedYPanel();
        operatorPanel.add(butAnd);
        operatorPanel.add(butOr);
        operatorPanel.add(butNot);
        operatorPanel.add(butLeftParen);
        operatorPanel.add(butRightParen);
        operatorPanel.add(butBackspace);
        operatorPanel.add(butClearFormula);

        formulaDisplay.setEditable(false);
        formulaDisplay.setLineWrap(true);
        formulaDisplay.setWrapStyleWord(true);

        JPanel finishPanel = new FixedYPanel();
        finishPanel.add(new JLabel(Messages.getString("VictoryConditionsDialog.winner")));
        finishPanel.add(comboWinner);
        finishPanel.add(checkOnlyAtEnd);
        finishPanel.add(butAddToList);

        builderPanel.add(leafPanel);
        builderPanel.add(operatorPanel);
        builderPanel.add(new JScrollPane(formulaDisplay));
        builderPanel.add(finishPanel);

        JPanel passwordPanel = new FixedYPanel();
        passwordPanel.add(new JLabel(Messages.getString("VictoryConditionsDialog.password")));
        passwordPanel.add(fieldPassword);

        result.add(victoryOptionsScroll);
        result.add(Box.createVerticalStrut(5));
        result.add(objectivesPanel);
        result.add(Box.createVerticalStrut(5));
        result.add(conditionsPanel);
        result.add(Box.createVerticalStrut(5));
        result.add(builderPanel);
        result.add(Box.createVerticalStrut(5));
        result.add(passwordPanel);

        wireActions();
        return result;
    }

    private void wireActions() {
        comboConditionType.addActionListener(event -> refreshLeafFields());
        butAddLeaf.addActionListener(event -> appendToken(buildLeafToken()));
        butAnd.addActionListener(event -> appendToken(OperatorToken.AND));
        butOr.addActionListener(event -> appendToken(OperatorToken.OR));
        butNot.addActionListener(event -> appendToken(OperatorToken.NOT));
        butLeftParen.addActionListener(event -> appendToken(OperatorToken.LEFT_PAREN));
        butRightParen.addActionListener(event -> appendToken(OperatorToken.RIGHT_PAREN));
        butBackspace.addActionListener(event -> removeLastToken());
        butClearFormula.addActionListener(event -> clearFormula());
        butAddToList.addActionListener(event -> addConditionToList());
        butRemoveSelected.addActionListener(event -> removeSelectedCondition());
        butAddObjective.addActionListener(event -> addObjectiveMarker());
        butRemoveObjective.addActionListener(event -> removeSelectedObjectiveMarker());
        refreshLeafFields();
    }

    /** Creates an objective marker from the editor fields and adds it to the local player's markers to place. */
    private void addObjectiveMarker() {
        String objectiveName = fieldNewObjectiveName.getText().trim();
        if (objectiveName.isBlank()) {
            return;
        }
        ObjectiveMarker marker = new ObjectiveMarker();
        marker.setName(objectiveName);
        Player owner = (comboObjectiveOwner.getSelectedItem() == null)
              ? clientGui.getClient().getLocalPlayer()
              : findPlayerByName((String) comboObjectiveOwner.getSelectedItem());
        marker.setOwnerId(owner.getId());
        marker.setLobbyPosition(new Coords((Integer) spinnerObjectiveX.getValue(),
              (Integer) spinnerObjectiveY.getValue()));
        marker.setControlRadius((Integer) spinnerObjectiveRadius.getValue());
        marker.setVictoryPointValue((Integer) spinnerObjectiveVP.getValue());
        marker.setPotential(checkObjectivePotential.isSelected());
        marker.setFalseObjective(checkObjectiveFalse.isSelected());
        marker.setFragile(checkObjectiveFragile.isSelected());
        marker.setMobile(checkObjectiveMobile.isSelected());
        marker.setInvulnerable(!checkObjectiveDestructible.isSelected());
        if (marker.isPotential() && marker.isFalseObjective()) {
            // RAW: Potential Objectives cannot be used in conjunction with False Objectives
            marker.setFalseObjective(false);
        }
        clientGui.getClient().getLocalPlayer().getGroundObjectsToPlace().add(marker);
        refreshObjectivesList();
        fieldNewObjectiveName.setText("");
    }

    private void removeSelectedObjectiveMarker() {
        int selectedIndex = objectivesList.getSelectedIndex();
        if ((selectedIndex >= 0) && (selectedIndex < objectiveMarkersInList.size())) {
            clientGui.getClient().getLocalPlayer().getGroundObjectsToPlace()
                  .remove(objectiveMarkersInList.get(selectedIndex));
            refreshObjectivesList();
        }
    }

    private Player findPlayerByName(String playerName) {
        for (Player player : clientGui.getClient().getGame().getPlayersList()) {
            if (playerName.equals(player.getName())) {
                return player;
            }
        }
        return clientGui.getClient().getLocalPlayer();
    }

    private ConditionToken buildLeafToken() {
        ConditionType conditionType = (ConditionType) comboConditionType.getSelectedItem();
        String playerName = (comboLeafPlayer.getSelectedIndex() <= 0)
              ? null
              : (String) comboLeafPlayer.getSelectedItem();
        return conditionType.buildToken(fieldObjectiveName.getText().trim(), playerName,
              (Integer) spinnerNumber.getValue());
    }

    private void appendToken(Token token) {
        if (VictoryConditionsBuilder.canAppend(currentFormula, token)) {
            currentFormula.add(token);
            refreshFormulaControls();
        }
    }

    private void removeLastToken() {
        if (!currentFormula.isEmpty()) {
            currentFormula.removeLast();
            refreshFormulaControls();
        }
    }

    private void clearFormula() {
        currentFormula.clear();
        refreshFormulaControls();
    }

    private void addConditionToList() {
        if (!VictoryConditionsBuilder.isCompleteFormula(currentFormula)) {
            return;
        }
        String winnerName = (comboWinner.getSelectedIndex() <= 0) ? null : (String) comboWinner.getSelectedItem();
        ConditionEntry conditionEntry =
              new ConditionEntry(List.copyOf(currentFormula), winnerName, checkOnlyAtEnd.isSelected());
        conditionEntries.add(conditionEntry);
        conditionListModel.addElement(describe(conditionEntry));
        clearFormula();
    }

    private void removeSelectedCondition() {
        int selectedIndex = conditionsList.getSelectedIndex();
        if (selectedIndex >= 0) {
            conditionEntries.remove(selectedIndex);
            conditionListModel.remove(selectedIndex);
        }
    }

    private String describe(ConditionEntry conditionEntry) {
        String winner = (conditionEntry.winnerName() == null)
              ? Messages.getString("VictoryConditionsDialog.draw")
              : conditionEntry.winnerName();
        String onlyAtEnd = conditionEntry.onlyAtEnd()
              ? " " + Messages.getString("VictoryConditionsDialog.onlyAtEndTag")
              : "";
        return winner + onlyAtEnd + ": " + VictoryConditionsBuilder.toDisplayText(conditionEntry.formula());
    }

    /** Enables/disables the token buttons per the formula grammar and refreshes the formula display. */
    private void refreshFormulaControls() {
        formulaDisplay.setText(VictoryConditionsBuilder.toDisplayText(currentFormula));
        // any condition leaf behaves identically in the grammar; probe with a parameterless one
        Token leafProbe = VictoryConditionsBuilder.battlefieldControl();
        butAddLeaf.setEnabled(VictoryConditionsBuilder.canAppend(currentFormula, leafProbe));
        butAnd.setEnabled(VictoryConditionsBuilder.canAppend(currentFormula, OperatorToken.AND));
        butOr.setEnabled(VictoryConditionsBuilder.canAppend(currentFormula, OperatorToken.OR));
        butNot.setEnabled(VictoryConditionsBuilder.canAppend(currentFormula, OperatorToken.NOT));
        butLeftParen.setEnabled(VictoryConditionsBuilder.canAppend(currentFormula, OperatorToken.LEFT_PAREN));
        butRightParen.setEnabled(VictoryConditionsBuilder.canAppend(currentFormula, OperatorToken.RIGHT_PAREN));
        butBackspace.setEnabled(!currentFormula.isEmpty());
        butClearFormula.setEnabled(!currentFormula.isEmpty());
        butAddToList.setEnabled(VictoryConditionsBuilder.isCompleteFormula(currentFormula));
    }

    /** Enables/disables the leaf parameter fields per the selected condition type. */
    private void refreshLeafFields() {
        ConditionType conditionType = (ConditionType) comboConditionType.getSelectedItem();
        if (conditionType == null) {
            return;
        }
        fieldObjectiveName.setEnabled(conditionType.needsObjective);
        comboLeafPlayer.setEnabled(conditionType.needsPlayer);
        spinnerNumber.setEnabled(conditionType.needsNumber);
    }

    /** @return The server password entered by the user (empty when the server has none) */
    public String getPassword() {
        return fieldPassword.getText();
    }

    /**
     * @return The victory game options the user changed, for
     *       {@link megamek.client.Client#sendGameOptions(String, Vector)}; empty when nothing changed
     */
    public Vector<IBasicOption> getChangedVictoryOptions() {
        Vector<IBasicOption> changedOptions = new Vector<>();
        for (DialogOptionComponentYPanel optionComponent : victoryOptionComps) {
            if (optionComponent.hasChanged()) {
                changedOptions.addElement(optionComponent.changedOption());
                optionComponent.setOptionChanged(false);
            }
        }
        return changedOptions;
    }

    @Override
    public void optionClicked(DialogOptionComponentYPanel comp, IOption option, boolean state) {
        // no dependent-option handling needed for the victory options
    }

    @Override
    public void optionSwitched(DialogOptionComponentYPanel comp, IOption option, int i) {
        // no dependent-option handling needed for the victory options
    }

    /**
     * @return The built conditions compiled to the {@code victory:} YAML schema for
     *       {@link megamek.client.Client#sendVictoryConditions(String, String)}; an empty string when no conditions
     *       are set (the server then clears previously lobby-set conditions)
     */
    public String getVictoryConditionsYaml() {
        if (conditionEntries.isEmpty()) {
            return "";
        }
        List<ObjectNode> victoryEntries = new ArrayList<>();
        for (ConditionEntry conditionEntry : conditionEntries) {
            victoryEntries.add(VictoryConditionsBuilder.compileCondition(conditionEntry.formula(),
                  conditionEntry.winnerName(), conditionEntry.onlyAtEnd()));
        }
        return VictoryConditionsBuilder.toYaml(victoryEntries);
    }
}
