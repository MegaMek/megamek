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

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Container;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import javax.swing.*;

import com.fasterxml.jackson.databind.node.ObjectNode;
import megamek.client.event.BoardViewEvent;
import megamek.client.event.BoardViewListenerAdapter;
import megamek.client.ui.Messages;
import megamek.client.ui.clientGUI.ClientGUI;
import megamek.client.ui.clientGUI.DialogOptionListener;
import megamek.client.ui.clientGUI.boardview.BoardView;
import megamek.client.ui.clientGUI.boardview.sprite.FieldOfFireSprite;
import megamek.client.ui.dialogs.buttonDialogs.AbstractButtonDialog;
import megamek.client.ui.panels.DialogOptionComponentYPanel;
import megamek.client.ui.util.UIUtil;
import megamek.client.ui.util.UIUtil.FixedYPanel;
import megamek.common.Player;
import megamek.common.RangeType;
import megamek.common.annotations.Nullable;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.enums.GamePhase;
import megamek.common.equipment.ICarryable;
import megamek.common.equipment.ObjectiveMarker;
import megamek.common.game.Game;
import megamek.common.hexArea.CircleHexArea;
import megamek.common.jacksonAdapters.VictoryConditionsBuilder;
import megamek.common.jacksonAdapters.VictoryConditionsBuilder.ConditionToken;
import megamek.common.jacksonAdapters.VictoryConditionsBuilder.OperatorToken;
import megamek.common.jacksonAdapters.VictoryConditionsBuilder.Token;
import megamek.common.loaders.MapSettings;
import megamek.common.options.IBasicOption;
import megamek.common.options.IOption;
import megamek.common.options.IOptionGroup;
import megamek.common.options.OptionsConstants;
import megamek.logging.MMLogger;
import megamek.server.ServerBoardHelper;

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

    private static final MMLogger LOGGER = MMLogger.create(VictoryConditionsDialog.class);

    // bitmask for drawing all six hex edges of a highlight sprite (filled hex)
    private static final int ALL_HEX_BORDERS = 63;

    private final JPanel victoryOptionsPanel = new JPanel();
    private final List<DialogOptionComponentYPanel> victoryOptionComps = new ArrayList<>();

    // the permanent board backdrop (layout: board left, authoring tools right)
    private final Game boardPreviewGame = new Game();
    private final JPanel boardContainer = new JPanel(new BorderLayout());
    // banner along the bottom of the board: the board name, or the generated-map caveat
    private final JLabel labelBoardBanner = new JLabel("", JLabel.CENTER);
    private BoardView boardView;
    private final List<FieldOfFireSprite> objectiveSprites = new ArrayList<>();

    /** The order catalog (UI design doc: goal-first, flat list; Hold Line and Destroy Count are not built yet). */
    private enum OrderType {
        CONTROL("orderControl", true),
        SCAN("orderScan", true),
        SURVEY("orderSurvey", true),
        DESTROY_TARGET("orderDestroyTarget", true),
        HOLD_LINE("orderHoldLine", false),
        DESTROY_COUNT("orderDestroyCount", false);

        private final String messageKey;
        final boolean available;

        OrderType(String messageKey, boolean available) {
            this.messageKey = messageKey;
            this.available = available;
        }

        @Override
        public String toString() {
            return Messages.getString("VictoryConditionsDialog." + messageKey)
                  + (available ? "" : " " + Messages.getString("VictoryConditionsDialog.comingSoon"));
        }
    }

    /** The reward choices of a Control order - each maps to a scoring option or an auto-built condition. */
    private enum ControlReward {
        PER_TURN("rewardPerTurn"),
        AT_END("rewardAtEnd"),
        WINS("rewardWins");

        private final String messageKey;

        ControlReward(String messageKey) {
            this.messageKey = messageKey;
        }

        @Override
        public String toString() {
            return Messages.getString("VictoryConditionsDialog." + messageKey);
        }
    }

    /**
     * A mission order added via the catalog: everything it created (marker, auto-built condition, scan mission option),
     * so removing the order from the mission summary removes all of it atomically.
     *
     * @param type               The catalog order type
     * @param marker             The objective marker the order placed, or {@code null} for placeless orders
     * @param condition          The auto-built victory condition, or {@code null} when the order has none
     * @param enablesScanMission {@code true} when the order turned on the Sensor Check mission option
     * @param description        The one-line mission summary text
     */
    private record MissionOrder(OrderType type, @Nullable ObjectiveMarker marker,
          @Nullable ConditionEntry condition, boolean enablesScanMission, String description) {}

    private final List<MissionOrder> missionOrders = new ArrayList<>();
    private final DefaultListModel<String> missionSummaryModel = new DefaultListModel<>();
    private final JList<String> missionSummaryList = new JList<>(missionSummaryModel);
    private final JButton butRemoveOrder = new JButton(Messages.getString("VictoryConditionsDialog.removeOrder"));

    private final JList<OrderType> ordersList = new JList<>(OrderType.values());
    private final JPanel orderEditorCards = new JPanel(new CardLayout());
    private Coords pendingOrderPosition = null;
    private final JLabel labelControlStep = new JLabel();
    private final JLabel labelDestroyStep = new JLabel();
    private final JLabel labelSurveyStep = new JLabel();

    private final List<ObjectiveMarker> objectiveMarkersInList = new ArrayList<>();
    private final JTextField fieldNewObjectiveName = new JTextField(10);
    private final JComboBox<String> comboObjectiveOwner = new JComboBox<>();
    private final JSpinner spinnerObjectiveRadius =
          new JSpinner(new SpinnerNumberModel(0, 0, ObjectiveMarker.MAX_CONTROL_RADIUS, 1));
    private final JSpinner spinnerObjectiveVP = new JSpinner(new SpinnerNumberModel(1, 1, 99, 1));
    private final JComboBox<ControlReward> comboControlReward = new JComboBox<>(ControlReward.values());

    private final JTextField fieldDestroyName = new JTextField(10);
    private final JComboBox<String> comboDestroyOwner = new JComboBox<>();
    private final JComboBox<String> comboDestroyAttacker = new JComboBox<>();
    private final JCheckBox checkDestroyEndsGame =
          new JCheckBox(Messages.getString("VictoryConditionsDialog.endsGame"), true);
    private final JButton butAddDestroyOrder =
          new JButton(Messages.getString("VictoryConditionsDialog.addDestroyOrder"));

    private final JButton butAddScanOrder =
          new JButton(Messages.getString("VictoryConditionsDialog.addScanOrder"));

    private final JTextField fieldSurveyName = new JTextField(10);
    private final JComboBox<String> comboSurveyOwner = new JComboBox<>();
    private final JComboBox<String> comboSurveyScout = new JComboBox<>();
    private final JCheckBox checkSurveyWins =
          new JCheckBox(Messages.getString("VictoryConditionsDialog.confirmWins"), true);
    private final JButton butAddSurveyOrder =
          new JButton(Messages.getString("VictoryConditionsDialog.addSurveyOrder"));

    private final JButton butToggleAdvanced =
          new JButton(Messages.getString("VictoryConditionsDialog.showAdvanced"));
    private final JPanel advancedPanel = new JPanel();
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

    public VictoryConditionsDialog(ClientGUI clientGui) {
        super(clientGui.getFrame(), "VictoryConditionsDialog", "VictoryConditionsDialog.title");
        this.clientGui = clientGui;
        boardPreviewGame.setPhase(GamePhase.LOUNGE);
        refreshLobbyState();
        initialize();
        refreshFormulaControls();
        setSize(UIUtil.scaleForGUI(1400, 900));
        setLocationRelativeTo(clientGui.getFrame());
    }

    @Override
    public void dispose() {
        if (boardView != null) {
            boardView.dispose();
        }
        super.dispose();
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
        comboDestroyOwner.removeAllItems();
        comboDestroyAttacker.removeAllItems();
        comboSurveyOwner.removeAllItems();
        comboSurveyScout.removeAllItems();
        for (Player player : clientGui.getClient().getGame().getPlayersList()) {
            comboLeafPlayer.addItem(player.getName());
            comboWinner.addItem(player.getName());
            comboObjectiveOwner.addItem(player.getName());
            comboDestroyOwner.addItem(player.getName());
            comboDestroyAttacker.addItem(player.getName());
            comboSurveyOwner.addItem(player.getName());
            comboSurveyScout.addItem(player.getName());
        }
        refreshVictoryOptions();
        refreshBoardBackdrop();
        refreshMissionSummary();
    }

    /**
     * Rebuilds the board backdrop from the current lobby map settings: the same board the game will use for fixed
     * boards (a generated map previews a different random instance). Shows a message instead when no board is available
     * yet.
     */
    private void refreshBoardBackdrop() {
        if (clientGui.getClient().getMapSettings() == null) {
            LOGGER.debug("[VictoryUI] No map settings available yet - showing the no-board message");
            showNoBoardMessage();
            return;
        }
        Board board = ServerBoardHelper.getPossibleGameBoard(clientGui.getClient().getMapSettings(), false);
        if (board == null) {
            LOGGER.debug("[VictoryUI] Map settings produced no board - showing the no-board message");
            showNoBoardMessage();
            return;
        }
        LOGGER.debug("[VictoryUI] Board backdrop refresh: {}x{} board", board.getWidth(), board.getHeight());
        boardPreviewGame.setBoard(board);
        // a generated map previews a different random instance than the server will create: positions
        // stay exact (dimensions match) but the terrain shown is only an example
        List<String> boardNames = clientGui.getClient().getMapSettings().getBoardsSelectedVector().stream()
              .filter(boardName -> boardName != null)
              .toList();
        boolean hasGeneratedBoards = boardNames.stream()
              .anyMatch(boardName -> boardName.startsWith(MapSettings.BOARD_GENERATED)
                    || boardName.startsWith(MapSettings.BOARD_RANDOM)
                    || boardName.startsWith(MapSettings.BOARD_SURPRISE));
        if (hasGeneratedBoards) {
            labelBoardBanner.setText(Messages.getString("VictoryConditionsDialog.generatedMapWarning"));
            LOGGER.debug("[VictoryUI] Generated boards selected - terrain preview will not match the real board");
        } else {
            String displayedNames = String.join(", ", boardNames.subList(0, Math.min(3, boardNames.size())))
                  + ((boardNames.size() > 3) ? " +" + (boardNames.size() - 3) : "");
            labelBoardBanner.setText(Messages.getString("VictoryConditionsDialog.boardBanner", displayedNames));
        }
        if (boardView == null) {
            try {
                boardView = new BoardView(boardPreviewGame, null, null, 0);
                boardView.setDisplayInvalidFields(false);
                boardView.setUseLosTool(false);
                boardView.addBoardViewListener(new BoardViewListenerAdapter() {
                    @Override
                    public void hexMoused(BoardViewEvent event) {
                        if ((event.getType() != BoardViewEvent.BOARD_HEX_CLICKED)
                              || (event.getButton() != MouseEvent.BUTTON1)
                              || (event.getCoords() == null)) {
                            return;
                        }
                        boardHexClicked(event.getCoords());
                    }
                });
                boardContainer.removeAll();
                boardContainer.add(labelBoardBanner, BorderLayout.SOUTH);
                // getComponent() must come before zooming: zoom adjusts the scroll pane it creates
                boardContainer.add(boardView.getComponent(true), BorderLayout.CENTER);
                for (int zoomStep = 0; zoomStep < 4; zoomStep++) {
                    boardView.zoomOut();
                }
                boardContainer.revalidate();
                LOGGER.info("[VictoryUI] Board view created and embedded (zoomed out 4 steps)");
            } catch (Exception exception) {
                LOGGER.error(exception, "[VictoryUI] Could not create the board view - the board pane stays empty");
                boardView = null;
                showNoBoardMessage();
            }
        }
        refreshBoardOverlays();
    }

    private void showNoBoardMessage() {
        boardContainer.removeAll();
        boardContainer.add(new JLabel(Messages.getString("VictoryConditionsDialog.noBoard"),
              JLabel.CENTER), BorderLayout.CENTER);
        boardContainer.revalidate();
    }

    /** Builds the per-order editor cards (guided steps: the prompt tells the player what to do next). */
    private void buildOrderEditorCards() {
        JPanel idleCard = new JPanel(new BorderLayout());
        idleCard.add(new JLabel(Messages.getString("VictoryConditionsDialog.pickAnOrder"), JLabel.CENTER),
              BorderLayout.CENTER);

        JPanel controlCard = new JPanel();
        controlCard.setLayout(new BoxLayout(controlCard, BoxLayout.PAGE_AXIS));
        JPanel controlStepRow = new FixedYPanel();
        controlStepRow.add(labelControlStep);
        JPanel controlFieldsRow = new FixedYPanel();
        controlFieldsRow.add(new JLabel(Messages.getString("VictoryConditionsDialog.objective")));
        controlFieldsRow.add(fieldNewObjectiveName);
        controlFieldsRow.add(new JLabel(Messages.getString("VictoryConditionsDialog.owner")));
        controlFieldsRow.add(comboObjectiveOwner);
        controlFieldsRow.add(new JLabel(Messages.getString("VictoryConditionsDialog.radius")));
        controlFieldsRow.add(spinnerObjectiveRadius);
        controlFieldsRow.add(new JLabel(Messages.getString("VictoryConditionsDialog.vp")));
        controlFieldsRow.add(spinnerObjectiveVP);
        controlFieldsRow.add(new JLabel(Messages.getString("VictoryConditionsDialog.reward")));
        controlFieldsRow.add(comboControlReward);
        JPanel controlVariantsRow = new FixedYPanel();
        controlVariantsRow.add(checkObjectivePotential);
        controlVariantsRow.add(checkObjectiveFalse);
        controlVariantsRow.add(checkObjectiveFragile);
        controlVariantsRow.add(checkObjectiveMobile);
        controlVariantsRow.add(checkObjectiveDestructible);
        controlVariantsRow.add(butAddObjective);
        controlCard.add(controlStepRow);
        controlCard.add(controlFieldsRow);
        controlCard.add(controlVariantsRow);

        JPanel destroyCard = new JPanel();
        destroyCard.setLayout(new BoxLayout(destroyCard, BoxLayout.PAGE_AXIS));
        JPanel destroyStepRow = new FixedYPanel();
        destroyStepRow.add(labelDestroyStep);
        JPanel destroyFieldsRow = new FixedYPanel();
        destroyFieldsRow.add(new JLabel(Messages.getString("VictoryConditionsDialog.objective")));
        destroyFieldsRow.add(fieldDestroyName);
        destroyFieldsRow.add(new JLabel(Messages.getString("VictoryConditionsDialog.owner")));
        destroyFieldsRow.add(comboDestroyOwner);
        JPanel destroyWinnerRow = new FixedYPanel();
        destroyWinnerRow.add(new JLabel(Messages.getString("VictoryConditionsDialog.attacker")));
        destroyWinnerRow.add(comboDestroyAttacker);
        destroyWinnerRow.add(checkDestroyEndsGame);
        destroyWinnerRow.add(butAddDestroyOrder);
        destroyCard.add(destroyStepRow);
        destroyCard.add(destroyFieldsRow);
        destroyCard.add(destroyWinnerRow);

        JPanel scanCard = new JPanel();
        scanCard.setLayout(new BoxLayout(scanCard, BoxLayout.PAGE_AXIS));
        JPanel scanInfoRow = new FixedYPanel();
        scanInfoRow.add(new JLabel(Messages.getString("VictoryConditionsDialog.scanInfo")));
        JPanel scanAddRow = new FixedYPanel();
        scanAddRow.add(butAddScanOrder);
        scanCard.add(scanInfoRow);
        scanCard.add(scanAddRow);

        JPanel surveyCard = new JPanel();
        surveyCard.setLayout(new BoxLayout(surveyCard, BoxLayout.PAGE_AXIS));
        JPanel surveyStepRow = new FixedYPanel();
        surveyStepRow.add(labelSurveyStep);
        JPanel surveyFieldsRow = new FixedYPanel();
        surveyFieldsRow.add(new JLabel(Messages.getString("VictoryConditionsDialog.objective")));
        surveyFieldsRow.add(fieldSurveyName);
        surveyFieldsRow.add(new JLabel(Messages.getString("VictoryConditionsDialog.owner")));
        surveyFieldsRow.add(comboSurveyOwner);
        JPanel surveyScoutRow = new FixedYPanel();
        surveyScoutRow.add(new JLabel(Messages.getString("VictoryConditionsDialog.scout")));
        surveyScoutRow.add(comboSurveyScout);
        surveyScoutRow.add(checkSurveyWins);
        surveyScoutRow.add(butAddSurveyOrder);
        surveyCard.add(surveyStepRow);
        surveyCard.add(surveyFieldsRow);
        surveyCard.add(surveyScoutRow);

        JPanel comingSoonCard = new JPanel(new BorderLayout());
        comingSoonCard.add(new JLabel(Messages.getString("VictoryConditionsDialog.orderNotBuilt"), JLabel.CENTER),
              BorderLayout.CENTER);

        orderEditorCards.add(idleCard, "IDLE");
        orderEditorCards.add(controlCard, OrderType.CONTROL.name());
        orderEditorCards.add(scanCard, OrderType.SCAN.name());
        orderEditorCards.add(surveyCard, OrderType.SURVEY.name());
        orderEditorCards.add(destroyCard, OrderType.DESTROY_TARGET.name());
        orderEditorCards.add(comingSoonCard, "COMING_SOON");
        refreshOrderStepLabels();
    }

    private void showOrderCard(String cardName) {
        ((CardLayout) orderEditorCards.getLayout()).show(orderEditorCards, cardName);
    }

    private void orderSelected() {
        OrderType selectedOrder = ordersList.getSelectedValue();
        LOGGER.debug("[VictoryUI] Order selected: {}", selectedOrder);
        pendingOrderPosition = null;
        refreshOrderStepLabels();
        if (selectedOrder == null) {
            showOrderCard("IDLE");
        } else if (!selectedOrder.available) {
            showOrderCard("COMING_SOON");
        } else {
            showOrderCard(selectedOrder.name());
        }
        refreshBoardOverlays();
    }

    /** Updates the guided-step prompts and the add buttons per the pending board click. */
    private void refreshOrderStepLabels() {
        if (pendingOrderPosition == null) {
            labelControlStep.setText(Messages.getString("VictoryConditionsDialog.stepClickCenter"));
            labelDestroyStep.setText(Messages.getString("VictoryConditionsDialog.stepClickTarget"));
            labelSurveyStep.setText(Messages.getString("VictoryConditionsDialog.stepClickSurvey"));
        } else {
            String position = pendingOrderPosition.toFriendlyString();
            labelControlStep.setText(Messages.getString("VictoryConditionsDialog.stepControlDetails", position));
            labelDestroyStep.setText(Messages.getString("VictoryConditionsDialog.stepDestroyDetails", position));
            labelSurveyStep.setText(Messages.getString("VictoryConditionsDialog.stepSurveyDetails", position));
        }
        butAddObjective.setEnabled(pendingOrderPosition != null);
        butAddDestroyOrder.setEnabled(pendingOrderPosition != null);
        butAddSurveyOrder.setEnabled(pendingOrderPosition != null);
    }

    /** @return {@code true} when the order needs a board position (picking it arms the board for a click) */
    private boolean isPlacedOrder(@Nullable OrderType orderType) {
        return (orderType == OrderType.CONTROL) || (orderType == OrderType.DESTROY_TARGET)
              || (orderType == OrderType.SURVEY);
    }

    /** A left click on the board sets the position of the placed order being built. */
    private void boardHexClicked(Coords coords) {
        OrderType selectedOrder = ordersList.getSelectedValue();
        if (isPlacedOrder(selectedOrder)) {
            LOGGER.debug("[VictoryUI] Board click at {} sets the position of the {} order being built",
                  coords.getBoardNum(), selectedOrder);
            pendingOrderPosition = coords;
            refreshOrderStepLabels();
            refreshBoardOverlays();
        } else {
            LOGGER.debug("[VictoryUI] Board click at {} ignored - no placed order is selected (selected: {})",
                  coords.getBoardNum(), selectedOrder);
        }
    }

    /**
     * Redraws the board overlays: the control-radius blob of every configured objective, and the blob of the
     * position/radius currently in the editor.
     */
    private void refreshBoardOverlays() {
        if (boardView == null) {
            return;
        }
        boardView.removeSprites(objectiveSprites);
        objectiveSprites.clear();
        Board board = boardPreviewGame.getBoard();
        if (board == null) {
            return;
        }
        for (ObjectiveMarker marker : objectiveMarkersInList) {
            if (marker.getLobbyPosition() != null) {
                addBlobSprites(board, marker.getLobbyPosition(), marker.getControlRadius(), RangeType.RANGE_MEDIUM);
            }
        }
        if ((pendingOrderPosition != null) && board.contains(pendingOrderPosition)) {
            int previewRadius = (ordersList.getSelectedValue() == OrderType.CONTROL)
                  ? (Integer) spinnerObjectiveRadius.getValue()
                  : 0;
            addBlobSprites(board, pendingOrderPosition, previewRadius, RangeType.RANGE_SHORT);
        }
        boardView.addSprites(objectiveSprites);
        LOGGER.debug("[VictoryUI] Board overlays refreshed: {} highlight sprite(s) for {} objective(s){}",
              objectiveSprites.size(), objectiveMarkersInList.size(),
              (pendingOrderPosition == null) ? "" : " plus the pending order at " + pendingOrderPosition.getBoardNum());
    }

    /** Adds the filled-hex sprites of a control-radius blob (the hex and every hex within the radius). */
    private void addBlobSprites(Board board, Coords center, int radius, int rangeColor) {
        for (Coords blobHex : new CircleHexArea(center, radius).getCoords(board)) {
            objectiveSprites.add(new FieldOfFireSprite(boardView, rangeColor, blobHex, ALL_HEX_BORDERS));
        }
    }

    /**
     * Rebuilds the mission summary and the marker overlays. Markers found on the local player without a mission order
     * (e.g. from an earlier lobby session) are wrapped as plain Control orders so they show and can be removed; orders
     * whose marker vanished externally are dropped.
     */
    private void refreshMissionSummary() {
        objectiveMarkersInList.clear();
        for (ICarryable groundObject : clientGui.getClient().getLocalPlayer().getGroundObjectsToPlace()) {
            if (groundObject instanceof ObjectiveMarker marker) {
                objectiveMarkersInList.add(marker);
            }
        }
        missionOrders.removeIf(missionOrder -> (missionOrder.marker() != null)
              && !objectiveMarkersInList.contains(missionOrder.marker()));
        for (ObjectiveMarker marker : objectiveMarkersInList) {
            boolean hasOrder = missionOrders.stream()
                  .anyMatch(missionOrder -> missionOrder.marker() == marker);
            if (!hasOrder) {
                missionOrders.add(new MissionOrder(OrderType.CONTROL, marker, null, false, describeMarker(marker)));
            }
        }
        missionSummaryModel.clear();
        for (MissionOrder missionOrder : missionOrders) {
            missionSummaryModel.addElement(missionOrder.description());
        }
        refreshBoardOverlays();
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
        // layout A: the board is a permanent backdrop on the left, the authoring tools column on the right
        JPanel toolsColumn = new JPanel();
        toolsColumn.setLayout(new BoxLayout(toolsColumn, BoxLayout.PAGE_AXIS));
        toolsColumn.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JPanel result = toolsColumn;

        victoryOptionsPanel.setLayout(new BoxLayout(victoryOptionsPanel, BoxLayout.PAGE_AXIS));
        JScrollPane victoryOptionsScroll = new JScrollPane(victoryOptionsPanel);
        victoryOptionsScroll.setBorder(BorderFactory.createTitledBorder(
              Messages.getString("VictoryConditionsDialog.victoryOptions")));

        // the goal-first order catalog: pick an order, the editor below walks through its steps
        ordersList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        ordersList.setVisibleRowCount(OrderType.values().length);
        JPanel ordersPanel = new JPanel(new BorderLayout());
        ordersPanel.setBorder(BorderFactory.createTitledBorder(
              Messages.getString("VictoryConditionsDialog.orders")));
        ordersPanel.add(new JScrollPane(ordersList), BorderLayout.CENTER);

        buildOrderEditorCards();
        JPanel editorPanel = new JPanel(new BorderLayout());
        editorPanel.setBorder(BorderFactory.createTitledBorder(
              Messages.getString("VictoryConditionsDialog.orderEditor")));
        editorPanel.add(orderEditorCards, BorderLayout.CENTER);

        // the mission summary: one human-readable line per added order; removing removes the whole order
        missionSummaryList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        missionSummaryList.setVisibleRowCount(5);
        JPanel missionSummaryPanel = new JPanel();
        missionSummaryPanel.setLayout(new BoxLayout(missionSummaryPanel, BoxLayout.PAGE_AXIS));
        missionSummaryPanel.setBorder(BorderFactory.createTitledBorder(
              Messages.getString("VictoryConditionsDialog.missionSummary")));
        missionSummaryPanel.add(new JScrollPane(missionSummaryList));
        JPanel removeOrderPanel = new FixedYPanel();
        removeOrderPanel.add(butRemoveOrder);
        missionSummaryPanel.add(removeOrderPanel);

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

        // the power-user layer, collapsed by default: the raw conditions list, the formula builder and the
        // victory options; the normal flow never needs them (orders emit all of this automatically)
        advancedPanel.setLayout(new BoxLayout(advancedPanel, BoxLayout.PAGE_AXIS));
        advancedPanel.add(builderPanel);
        advancedPanel.add(Box.createVerticalStrut(5));
        advancedPanel.add(conditionsPanel);
        advancedPanel.add(Box.createVerticalStrut(5));
        advancedPanel.add(victoryOptionsScroll);
        advancedPanel.setVisible(false);
        JPanel advancedTogglePanel = new FixedYPanel();
        advancedTogglePanel.add(butToggleAdvanced);

        result.add(ordersPanel);
        result.add(Box.createVerticalStrut(5));
        result.add(editorPanel);
        result.add(Box.createVerticalStrut(5));
        result.add(missionSummaryPanel);
        result.add(Box.createVerticalStrut(5));
        result.add(advancedTogglePanel);
        result.add(advancedPanel);
        result.add(Box.createVerticalStrut(5));
        result.add(passwordPanel);

        wireActions();

        JScrollPane toolsScroll = new JScrollPane(toolsColumn);
        toolsScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, boardContainer, toolsScroll);
        splitPane.setResizeWeight(0.62);
        return splitPane;
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
        butAddObjective.addActionListener(event -> addControlOrder());
        butAddDestroyOrder.addActionListener(event -> addDestroyOrder());
        butAddScanOrder.addActionListener(event -> addScanOrder());
        butAddSurveyOrder.addActionListener(event -> addSurveyOrder());
        butRemoveOrder.addActionListener(event -> removeSelectedOrder());
        butToggleAdvanced.addActionListener(event -> toggleAdvanced());
        ordersList.addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                orderSelected();
            }
        });
        spinnerObjectiveRadius.addChangeListener(event -> refreshBoardOverlays());
        refreshLeafFields();
    }

    private void toggleAdvanced() {
        boolean showAdvanced = !advancedPanel.isVisible();
        advancedPanel.setVisible(showAdvanced);
        butToggleAdvanced.setText(Messages.getString(
              showAdvanced ? "VictoryConditionsDialog.hideAdvanced" : "VictoryConditionsDialog.showAdvanced"));
        LOGGER.debug("[VictoryUI] Advanced section {}", showAdvanced ? "expanded" : "collapsed");
    }

    /** Sets a boolean victory game option by name (kept in sync with its option row in the Advanced section). */
    private void setVictoryOption(String optionName, boolean enabled) {
        LOGGER.debug("[VictoryUI] Setting the victory option {} to {}", optionName, enabled);
        for (DialogOptionComponentYPanel optionComponent : victoryOptionComps) {
            if (optionName.equals(optionComponent.getOption().getName())) {
                optionComponent.setSelected(enabled);
            }
        }
    }

    /** Adds a Control order: an objective marker with the clicked position and the card's blob/variant values. */
    private void addControlOrder() {
        String objectiveName = fieldNewObjectiveName.getText().trim();
        if (objectiveName.isBlank()) {
            LOGGER.debug("[VictoryUI] Add Control order rejected - no objective name entered");
            return;
        }
        if (pendingOrderPosition == null) {
            LOGGER.debug("[VictoryUI] Add Control order rejected - no board position clicked yet");
            return;
        }
        ObjectiveMarker marker = new ObjectiveMarker();
        marker.setName(objectiveName);
        Player owner = (comboObjectiveOwner.getSelectedItem() == null)
              ? clientGui.getClient().getLocalPlayer()
              : findPlayerByName((String) comboObjectiveOwner.getSelectedItem());
        marker.setOwnerId(owner.getId());
        marker.setLobbyPosition(pendingOrderPosition);
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

        // the reward choice wires up the scoring: a mission option or an auto-built win condition
        ControlReward reward = (ControlReward) comboControlReward.getSelectedItem();
        ConditionEntry condition = null;
        if (reward == ControlReward.AT_END) {
            setVictoryOption(OptionsConstants.VICTORY_OBJECTIVE_RAID, true);
        } else if (reward == ControlReward.WINS) {
            condition = new ConditionEntry(
                  List.of(VictoryConditionsBuilder.objectiveControlled(objectiveName, owner.getName())),
                  owner.getName(), true);
            conditionEntries.add(condition);
            conditionListModel.addElement(describe(condition));
        } else {
            setVictoryOption(OptionsConstants.VICTORY_USE_OBJECTIVES, true);
        }
        missionOrders.add(new MissionOrder(OrderType.CONTROL, marker, condition, false,
              Messages.getString("VictoryConditionsDialog.summaryControl",
                    objectiveName, pendingOrderPosition.toFriendlyString(), marker.getControlRadius(),
                    owner.getName(), String.valueOf(reward))));
        LOGGER.info("[VictoryUI] Control order added: {} at {} radius {} for owner ID {} (reward: {})",
              marker.generalName(), marker.getLobbyPosition(), marker.getControlRadius(), marker.getOwnerId(),
              reward);
        fieldNewObjectiveName.setText("");
        pendingOrderPosition = null;
        refreshOrderStepLabels();
        refreshMissionSummary();
    }

    /** Adds the Scan order: enables the Sensor Check mission (scan enemy units, VP paid on exfiltration). */
    private void addScanOrder() {
        boolean alreadyAdded = missionOrders.stream()
              .anyMatch(missionOrder -> missionOrder.type() == OrderType.SCAN);
        if (alreadyAdded) {
            LOGGER.debug("[VictoryUI] Add Scan order rejected - the scan mission is already in the summary");
            return;
        }
        setVictoryOption(OptionsConstants.VICTORY_USE_SENSOR_CHECK, true);
        missionOrders.add(new MissionOrder(OrderType.SCAN, null, null, true,
              Messages.getString("VictoryConditionsDialog.summaryScan")));
        LOGGER.info("[VictoryUI] Scan order added - Sensor Check mission enabled");
        refreshMissionSummary();
    }

    /**
     * Adds a Survey order: a Potential Objective candidate at the clicked position that units must approach and confirm
     * with a sensor check, plus (optionally) a condition awarding the scout the win on confirmation.
     */
    private void addSurveyOrder() {
        String objectiveName = fieldSurveyName.getText().trim();
        if (objectiveName.isBlank()) {
            LOGGER.debug("[VictoryUI] Add Survey order rejected - no objective name entered");
            return;
        }
        if (pendingOrderPosition == null) {
            LOGGER.debug("[VictoryUI] Add Survey order rejected - no board position clicked yet");
            return;
        }
        ObjectiveMarker marker = new ObjectiveMarker();
        marker.setName(objectiveName);
        Player owner = (comboSurveyOwner.getSelectedItem() == null)
              ? clientGui.getClient().getLocalPlayer()
              : findPlayerByName((String) comboSurveyOwner.getSelectedItem());
        marker.setOwnerId(owner.getId());
        marker.setLobbyPosition(pendingOrderPosition);
        marker.setPotential(true);
        clientGui.getClient().getLocalPlayer().getGroundObjectsToPlace().add(marker);

        String scoutName = (String) comboSurveyScout.getSelectedItem();
        ConditionEntry condition = null;
        if (checkSurveyWins.isSelected() && (scoutName != null)) {
            condition = new ConditionEntry(
                  List.of(VictoryConditionsBuilder.objectiveConfirmed(objectiveName)), scoutName, false);
            conditionEntries.add(condition);
            conditionListModel.addElement(describe(condition));
        } else {
            // without a win condition the confirmed objective scores control VP instead
            setVictoryOption(OptionsConstants.VICTORY_USE_OBJECTIVES, true);
        }
        missionOrders.add(new MissionOrder(OrderType.SURVEY, marker, condition, false,
              Messages.getString("VictoryConditionsDialog.summarySurvey",
                    objectiveName, pendingOrderPosition.toFriendlyString(), scoutName)));
        LOGGER.info("[VictoryUI] Survey order added: {} at {} (scout: {}, confirming wins: {})",
              objectiveName, marker.getLobbyPosition(), scoutName, checkSurveyWins.isSelected());
        fieldSurveyName.setText("");
        pendingOrderPosition = null;
        refreshOrderStepLabels();
        refreshMissionSummary();
    }

    /**
     * Adds a Destroy Target order: a destructible marker at the clicked position plus a victory condition awarding the
     * attacker the win when it is destroyed.
     */
    private void addDestroyOrder() {
        String objectiveName = fieldDestroyName.getText().trim();
        if (objectiveName.isBlank()) {
            LOGGER.debug("[VictoryUI] Add Destroy order rejected - no objective name entered");
            return;
        }
        if (pendingOrderPosition == null) {
            LOGGER.debug("[VictoryUI] Add Destroy order rejected - no board position clicked yet");
            return;
        }
        ObjectiveMarker marker = new ObjectiveMarker();
        marker.setName(objectiveName);
        Player owner = (comboDestroyOwner.getSelectedItem() == null)
              ? clientGui.getClient().getLocalPlayer()
              : findPlayerByName((String) comboDestroyOwner.getSelectedItem());
        marker.setOwnerId(owner.getId());
        marker.setLobbyPosition(pendingOrderPosition);
        marker.setInvulnerable(false);
        clientGui.getClient().getLocalPlayer().getGroundObjectsToPlace().add(marker);

        String attackerName = (String) comboDestroyAttacker.getSelectedItem();
        ConditionEntry condition = null;
        if (attackerName != null) {
            condition = new ConditionEntry(
                  List.of(VictoryConditionsBuilder.objectiveDestroyed(objectiveName)),
                  attackerName, !checkDestroyEndsGame.isSelected());
            conditionEntries.add(condition);
            conditionListModel.addElement(describe(condition));
        }
        missionOrders.add(new MissionOrder(OrderType.DESTROY_TARGET, marker, condition, false,
              Messages.getString("VictoryConditionsDialog.summaryDestroy",
                    objectiveName, pendingOrderPosition.toFriendlyString(), attackerName)));
        LOGGER.info("[VictoryUI] Destroy Target order added: {} at {} (attacker: {}, ends game: {})",
              objectiveName, marker.getLobbyPosition(), attackerName, checkDestroyEndsGame.isSelected());

        fieldDestroyName.setText("");
        pendingOrderPosition = null;
        refreshOrderStepLabels();
        refreshMissionSummary();
    }

    /** Removes the selected order and everything it created: its marker, its condition, its mission option. */
    private void removeSelectedOrder() {
        int selectedIndex = missionSummaryList.getSelectedIndex();
        if ((selectedIndex < 0) || (selectedIndex >= missionOrders.size())) {
            LOGGER.debug("[VictoryUI] Remove order rejected - nothing selected in the mission summary");
            return;
        }
        MissionOrder order = missionOrders.remove(selectedIndex);
        if (order.marker() != null) {
            clientGui.getClient().getLocalPlayer().getGroundObjectsToPlace().remove(order.marker());
        }
        if (order.condition() != null) {
            int conditionIndex = conditionEntries.indexOf(order.condition());
            if (conditionIndex >= 0) {
                conditionEntries.remove(conditionIndex);
                conditionListModel.remove(conditionIndex);
            }
        }
        if (order.enablesScanMission()) {
            setVictoryOption(OptionsConstants.VICTORY_USE_SENSOR_CHECK, false);
        }
        LOGGER.info("[VictoryUI] Order removed from the mission: {}", order.description());
        refreshMissionSummary();
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
