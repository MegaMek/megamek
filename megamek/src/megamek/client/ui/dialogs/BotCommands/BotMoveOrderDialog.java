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
package megamek.client.ui.dialogs.BotCommands;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import javax.swing.AbstractCellEditor;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultCellEditor;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JToggleButton;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.table.TableCellEditor;

import megamek.client.event.BoardViewEvent;
import megamek.client.event.BoardViewListenerAdapter;
import megamek.client.ui.Messages;
import megamek.client.ui.clientGUI.ClientGUI;
import megamek.client.ui.clientGUI.boardview.BoardView;
import megamek.client.ui.clientGUI.boardview.sprite.FieldOfFireSprite;
import megamek.client.ui.clientGUI.boardview.sprite.Sprite;
import megamek.client.ui.clientGUI.boardview.sprite.TextMarkerSprite;
import megamek.client.ui.dialogs.BotCommands.BotOrdersMenuBuilder.OrderGroup;
import megamek.client.ui.dialogs.buttonDialogs.AbstractButtonDialog;
import megamek.client.ui.enums.DialogResult;
import megamek.client.ui.panels.FacingPickerPanel;
import megamek.client.ui.util.UIUtil;
import megamek.common.Player;
import megamek.common.RangeType;
import megamek.common.annotations.Nullable;
import megamek.common.board.Coords;
import megamek.common.orders.ContactRule;
import megamek.common.orders.FormationOrder;
import megamek.common.orders.FormationPace;
import megamek.common.orders.FormationShape;
import megamek.common.orders.OrderPriority;
import megamek.common.orders.UnitOrders;
import megamek.common.units.Entity;
import megamek.common.util.Distractable;
import megamek.logging.MMLogger;

/**
 * The Move Order editor: one window to give a group of a bot's units a route with a facing and a hold at each
 * waypoint, put them in a formation, and set how hard they push. It stays open while the player clicks the board, and
 * each click adds a waypoint; the draft route is numbered on the board as it grows.
 *
 * <p>Sending turns the whole order into {@code /unitOrder} commands for every unit of the group (see
 * {@link MoveOrderCommands}), so the orders are stored on the units and saved with the game.</p>
 */
public class BotMoveOrderDialog extends AbstractButtonDialog {

    private static final MMLogger LOGGER = MMLogger.create(BotMoveOrderDialog.class);

    // bitmask for drawing all six hex edges of the highlight sprite
    private static final int ALL_HEX_BORDERS = 63;
    private static final int GAP = 8;
    private static final int TABLE_WIDTH = 520;
    private static final int TABLE_HEIGHT = 170;
    private static final int ROW_HEIGHT = 26;
    private static final int NUMBER_COLUMN_WIDTH = 36;
    private static final int HEX_COLUMN_WIDTH = 64;
    private static final int NOTE_WIDTH = 260;

    private final ClientGUI clientGUI;
    private final BoardView boardView;
    private final Player botPlayer;
    private final Supplier<Map<String, List<OrderGroup>>> unitsByLance;
    private final BiConsumer<Player, String> acknowledger;
    private OrderGroup group;

    private final WaypointTableModel waypoints = new WaypointTableModel();
    private final List<JRadioButton> facingButtons = new ArrayList<>();
    private final ButtonGroup facingButtonGroup = new ButtonGroup();
    private final List<Sprite> routeSprites = new ArrayList<>();
    private JTable waypointTable;
    private JLabel unitsLabel;
    private JCheckBox formationBox;
    private JComboBox<FormationShape> shapeCombo;
    private JComboBox<UnitOption> leaderCombo;
    private JComboBox<Integer> spacingCombo;
    private JComboBox<FormationPace> paceCombo;
    private JComboBox<ContactRule> contactCombo;
    private JToggleButton pickButton;
    private JLabel selectedLabel;
    private JCheckBox autoFacingBox;
    private JSpinner holdSpinner;
    private JComboBox<OrderPriority> priorityCombo;
    private BoardViewListenerAdapter hexClickListener;
    private Distractable suppressedDisplay;
    private boolean isLoadingDetail;
    private boolean wasInFormation;

    /**
     * A unit as the leader list shows it.
     *
     * @param unitId the unit
     * @param label  its name
     */
    private record UnitOption(int unitId, String label) {
        @Override
        public String toString() {
            return label;
        }
    }

    /**
     * @param clientGUI     the client GUI
     * @param boardView     the board the player clicks to add waypoints
     * @param botPlayer     the bot whose units are ordered
     * @param group         the units ordered
     * @param unitsByLance  the bot's units by lance, for choosing other units
     * @param acknowledger  shows the player that the order was sent
     */
    public BotMoveOrderDialog(ClientGUI clientGUI, BoardView boardView, Player botPlayer, OrderGroup group,
          Supplier<Map<String, List<OrderGroup>>> unitsByLance, BiConsumer<Player, String> acknowledger) {
        super(clientGUI.getFrame(), false, "BotMoveOrderDialog", "BotCommandPanel.MoveOrder.dialogTitle");
        this.clientGUI = clientGUI;
        this.boardView = boardView;
        this.botPlayer = botPlayer;
        this.group = group;
        this.unitsByLance = unitsByLance;
        this.acknowledger = acknowledger;
        initialize();
        setTitle(Messages.getString("BotCommandPanel.MoveOrder.dialogTitleFor", botPlayer.getName()));
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent event) {
                cleanUp();
            }
        });
        loadCurrentOrders();
        waypoints.addTableModelListener(event -> refreshRouteSprites());
        refreshRouteSprites();
        if (waypoints.getRowCount() == 0) {
            pickButton.setSelected(true);
            startPicking();
        }
    }

    @Override
    protected Container createCenterPane() {
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.PAGE_AXIS));
        int gap = UIUtil.scaleForGUI(GAP);
        content.setBorder(new EmptyBorder(gap, gap, gap, gap));
        content.add(createUnitsPanel());
        content.add(Box.createVerticalStrut(gap));
        content.add(createFormationPanel());
        content.add(Box.createVerticalStrut(gap));
        content.add(createWaypointsPanel());
        content.add(Box.createVerticalStrut(gap));
        content.add(createRoutePanel());
        return content;
    }

    @Override
    protected JPanel createButtonPanel() {
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.TRAILING, UIUtil.scaleForGUI(GAP),
              UIUtil.scaleForGUI(GAP)));
        JButton cancelButton = new JButton(Messages.getString("Cancel.text"));
        cancelButton.addActionListener(event -> dispose());
        JButton sendButton = new JButton(Messages.getString("BotCommandPanel.MoveOrder.send"));
        sendButton.addActionListener(event -> sendOrders());
        buttons.add(cancelButton);
        buttons.add(sendButton);
        getRootPane().setDefaultButton(sendButton);
        return buttons;
    }

    private static JPanel section(String titleKey) {
        JPanel panel = new JPanel(new BorderLayout(UIUtil.scaleForGUI(GAP), UIUtil.scaleForGUI(GAP)));
        panel.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createTitledBorder(Messages.getString(titleKey)),
              new EmptyBorder(UIUtil.scaleForGUI(4), UIUtil.scaleForGUI(GAP), UIUtil.scaleForGUI(GAP),
                    UIUtil.scaleForGUI(GAP))));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        return panel;
    }

    private JPanel createUnitsPanel() {
        JPanel panel = section("BotCommandPanel.MoveOrder.units");
        unitsLabel = new JLabel();
        panel.add(unitsLabel, BorderLayout.CENTER);
        JButton chooseButton = new JButton(Messages.getString("BotCommandPanel.Orders.chooseUnits"));
        chooseButton.addActionListener(event -> chooseUnits());
        panel.add(chooseButton, BorderLayout.LINE_END);
        return panel;
    }

    private JPanel createFormationPanel() {
        JPanel panel = section("BotCommandPanel.MoveOrder.formation");
        formationBox = new JCheckBox(Messages.getString("BotCommandPanel.MoveOrder.inFormation"));
        formationBox.addActionListener(event -> updateFormationEnabled());
        panel.add(formationBox, BorderLayout.PAGE_START);

        shapeCombo = new JComboBox<>(FormationShape.values());
        shapeCombo.setRenderer(labelRenderer("BotCommandPanel.Formations.shape."));
        leaderCombo = new JComboBox<>();
        spacingCombo = new JComboBox<>();
        for (int spacing = FormationOrder.MINIMUM_SPACING; spacing <= FormationOrder.MAXIMUM_SPACING; spacing++) {
            spacingCombo.addItem(spacing);
        }
        spacingCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                  boolean isSelected, boolean hasFocus) {
                return super.getListCellRendererComponent(list,
                      Messages.getString("BotCommandPanel.Formations.spacing.hexes", value), index, isSelected,
                      hasFocus);
            }
        });
        paceCombo = new JComboBox<>(FormationPace.values());
        paceCombo.setRenderer(labelRenderer("BotCommandPanel.Formations.pace."));
        contactCombo = new JComboBox<>(ContactRule.values());
        contactCombo.setRenderer(labelRenderer("BotCommandPanel.Formations.contact."));

        JPanel choices = new JPanel(new GridLayout(2, 3, UIUtil.scaleForGUI(GAP), UIUtil.scaleForGUI(GAP)));
        choices.add(labelled("BotCommandPanel.Formations.shape", shapeCombo));
        choices.add(labelled("BotCommandPanel.Formations.leader", leaderCombo));
        choices.add(labelled("BotCommandPanel.Formations.spacing", spacingCombo));
        choices.add(labelled("BotCommandPanel.Formations.pace", paceCombo));
        choices.add(labelled("BotCommandPanel.Formations.contact", contactCombo));
        panel.add(choices, BorderLayout.CENTER);
        return panel;
    }

    private static ListCellRenderer<Object> labelRenderer(String keyPrefix) {
        return new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                  boolean isSelected, boolean hasFocus) {
                Object shown = (value instanceof Enum<?> choice) ? Messages.getString(keyPrefix + choice.name())
                      : value;
                return super.getListCellRendererComponent(list, shown, index, isSelected, hasFocus);
            }
        };
    }

    private static JPanel labelled(String titleKey, Component control) {
        JPanel panel = new JPanel(new BorderLayout(0, UIUtil.scaleForGUI(2)));
        panel.add(new JLabel(Messages.getString(titleKey)), BorderLayout.PAGE_START);
        panel.add(control, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createWaypointsPanel() {
        JPanel panel = section("BotCommandPanel.MoveOrder.waypoints");

        JPanel toolbar = new JPanel(new BorderLayout(UIUtil.scaleForGUI(GAP), 0));
        pickButton = new JToggleButton(Messages.getString("BotCommandPanel.MoveOrder.pick"));
        pickButton.addActionListener(event -> {
            if (pickButton.isSelected()) {
                startPicking();
            } else {
                stopPicking();
            }
        });
        toolbar.add(pickButton, BorderLayout.LINE_START);
        toolbar.add(new JLabel(Messages.getString("BotCommandPanel.MoveOrder.pickHint")), BorderLayout.CENTER);
        JButton clearButton = new JButton(Messages.getString("BotCommandPanel.MoveOrder.clear"));
        clearButton.addActionListener(event -> waypoints.clear());
        toolbar.add(clearButton, BorderLayout.LINE_END);
        panel.add(toolbar, BorderLayout.PAGE_START);

        waypointTable = new JTable(waypoints);
        waypointTable.setRowHeight(UIUtil.scaleForGUI(ROW_HEIGHT));
        waypointTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        waypointTable.getColumnModel().getColumn(WaypointTableModel.COLUMN_NUMBER)
              .setMaxWidth(UIUtil.scaleForGUI(NUMBER_COLUMN_WIDTH));
        waypointTable.getColumnModel().getColumn(WaypointTableModel.COLUMN_HEX)
              .setMaxWidth(UIUtil.scaleForGUI(HEX_COLUMN_WIDTH));
        waypointTable.getColumnModel().getColumn(WaypointTableModel.COLUMN_FACING)
              .setCellEditor(new DefaultCellEditor(new JComboBox<>(WaypointTableModel.facingOptions()
                    .toArray(new WaypointTableModel.FacingOption[0]))));
        waypointTable.getColumnModel().getColumn(WaypointTableModel.COLUMN_HOLD).setCellEditor(new HoldCellEditor());
        waypointTable.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                loadDetail();
            }
        });
        JScrollPane tableScroll = new JScrollPane(waypointTable);
        tableScroll.setPreferredSize(UIUtil.scaleForGUI(TABLE_WIDTH, TABLE_HEIGHT));
        panel.add(tableScroll, BorderLayout.CENTER);

        JPanel below = new JPanel(new BorderLayout(0, UIUtil.scaleForGUI(GAP)));
        JPanel rowButtons = new JPanel(new FlowLayout(FlowLayout.LEADING, UIUtil.scaleForGUI(4), 0));
        JButton upButton = new JButton(Messages.getString("BotCommandPanel.MoveOrder.up"));
        upButton.addActionListener(event -> selectRow(waypoints.moveUp(waypointTable.getSelectedRow())));
        JButton downButton = new JButton(Messages.getString("BotCommandPanel.MoveOrder.down"));
        downButton.addActionListener(event -> selectRow(waypoints.moveDown(waypointTable.getSelectedRow())));
        JButton removeButton = new JButton(Messages.getString("BotCommandPanel.MoveOrder.remove"));
        removeButton.addActionListener(event -> {
            int row = waypointTable.getSelectedRow();
            waypoints.removeWaypoint(row);
            selectRow(Math.min(row, waypoints.getRowCount() - 1));
        });
        rowButtons.add(upButton);
        rowButtons.add(downButton);
        rowButtons.add(removeButton);
        below.add(rowButtons, BorderLayout.PAGE_START);
        below.add(createDetailPanel(), BorderLayout.CENTER);
        panel.add(below, BorderLayout.PAGE_END);
        return panel;
    }

    private JPanel createDetailPanel() {
        JPanel detail = new JPanel(new BorderLayout(UIUtil.scaleForGUI(GAP * 2), 0));
        for (int facing = 0; facing < 6; facing++) {
            JRadioButton button = new JRadioButton();
            final int chosenFacing = facing;
            button.addActionListener(event -> setSelectedFacing(chosenFacing));
            facingButtons.add(button);
            facingButtonGroup.add(button);
        }
        autoFacingBox = new JCheckBox(Messages.getString("BotCommandPanel.Orders.facing.auto"));
        autoFacingBox.addActionListener(event -> {
            if (autoFacingBox.isSelected()) {
                setSelectedFacing(UnitOrders.FACING_AUTO);
            }
        });
        JPanel dial = new JPanel(new BorderLayout(0, UIUtil.scaleForGUI(4)));
        Entity previewUnit = firstUnit();
        if (previewUnit != null) {
            dial.add(new FacingPickerPanel(facingButtons, FacingPickerPanel.previewOnHex(clientGUI, previewUnit, 0)),
                  BorderLayout.CENTER);
        }
        dial.add(autoFacingBox, BorderLayout.PAGE_END);
        detail.add(dial, BorderLayout.LINE_START);

        JPanel text = new JPanel();
        text.setLayout(new BoxLayout(text, BoxLayout.PAGE_AXIS));
        selectedLabel = new JLabel();
        selectedLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        text.add(selectedLabel);
        text.add(Box.createVerticalStrut(UIUtil.scaleForGUI(GAP)));
        JPanel holdRow = new JPanel(new FlowLayout(FlowLayout.LEADING, UIUtil.scaleForGUI(4), 0));
        holdRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        holdSpinner = new JSpinner(new SpinnerNumberModel(0, 0, WaypointTableModel.MAXIMUM_HOLD_TURNS, 1));
        holdSpinner.addChangeListener(event -> {
            int row = waypointTable.getSelectedRow();
            if (!isLoadingDetail && (row >= 0)) {
                waypoints.setHoldTurns(row, (Integer) holdSpinner.getValue());
            }
        });
        holdRow.add(new JLabel(Messages.getString("BotCommandPanel.MoveOrder.holdFor")));
        holdRow.add(holdSpinner);
        holdRow.add(new JLabel(Messages.getString("BotCommandPanel.MoveOrder.holdAfter")));
        text.add(holdRow);
        text.add(Box.createVerticalStrut(UIUtil.scaleForGUI(GAP)));
        JLabel note = new JLabel("<html><div style='width:" + UIUtil.scaleForGUI(NOTE_WIDTH) + "px'>"
              + Messages.getString("BotCommandPanel.MoveOrder.holdNote") + "</div></html>");
        note.setAlignmentX(Component.LEFT_ALIGNMENT);
        text.add(note);
        detail.add(text, BorderLayout.CENTER);
        return detail;
    }

    private JPanel createRoutePanel() {
        JPanel panel = section("BotCommandPanel.MoveOrder.route");
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEADING, UIUtil.scaleForGUI(GAP), 0));
        priorityCombo = new JComboBox<>(OrderPriority.values());
        priorityCombo.setRenderer(labelRenderer("BotCommandPanel.Orders.priority."));
        row.add(new JLabel(Messages.getString("BotCommandPanel.Orders.priority")));
        row.add(priorityCombo);
        row.add(new JLabel(Messages.getString("BotCommandPanel.MoveOrder.lastWaypointNote")));
        panel.add(row, BorderLayout.CENTER);
        return panel;
    }

    /** Edits a hold with a spinner, 0 to {@link WaypointTableModel#MAXIMUM_HOLD_TURNS} turns. */
    private final class HoldCellEditor extends AbstractCellEditor implements TableCellEditor {
        private final JSpinner spinner = new JSpinner(new SpinnerNumberModel(0, 0,
              WaypointTableModel.MAXIMUM_HOLD_TURNS, 1));

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row,
              int column) {
            spinner.setValue(waypoints.getHoldTurns(row));
            return spinner;
        }

        @Override
        public Object getCellEditorValue() {
            return spinner.getValue();
        }
    }

    private @Nullable Entity firstUnit() {
        return group.unitIds().isEmpty() ? null : clientGUI.getClient().getGame().getEntity(group.unitIds().get(0));
    }

    /**
     * Fills the editor from the group's first unit: its route with each waypoint's facing and hold, its formation and
     * its priority, so reopening the editor shows the order the units are following.
     */
    private void loadCurrentOrders() {
        updateUnits();
        Entity first = firstUnit();
        UnitOrders orders = (first == null) ? UnitOrders.NONE : first.getUnitOrders();
        waypoints.setRoute(orders.getRoute(), orders.getWaypointOrders());
        priorityCombo.setSelectedItem(orders.getPriority());
        Optional<FormationOrder> formation = orders.getFormation();
        wasInFormation = false;
        for (int unitId : group.unitIds()) {
            Entity unit = clientGUI.getClient().getGame().getEntity(unitId);
            if ((unit != null) && unit.getUnitOrders().getFormation().isPresent()) {
                wasInFormation = true;
            }
        }
        formationBox.setSelected(formation.isPresent() && (group.unitIds().size() >= 2));
        shapeCombo.setSelectedItem(formation.map(FormationOrder::getShape).orElse(FormationShape.WEDGE));
        spacingCombo.setSelectedItem(formation.map(FormationOrder::getSpacing).orElse(FormationOrder.DEFAULT_SPACING));
        paceCombo.setSelectedItem(formation.map(FormationOrder::getPace).orElse(FormationPace.WALK));
        contactCombo.setSelectedItem(formation.map(FormationOrder::getContactRule).orElse(ContactRule.BREAK));
        int leaderId = formation.map(FormationOrder::getLeaderId).orElse(group.unitIds().get(0));
        for (int index = 0; index < leaderCombo.getItemCount(); index++) {
            if (leaderCombo.getItemAt(index).unitId() == leaderId) {
                leaderCombo.setSelectedIndex(index);
            }
        }
        updateFormationEnabled();
        selectRow(waypoints.getRowCount() - 1);
    }

    /** Shows the group's units and offers them as leaders. */
    private void updateUnits() {
        StringBuilder names = new StringBuilder();
        leaderCombo.removeAllItems();
        for (int unitId : group.unitIds()) {
            Entity unit = clientGUI.getClient().getGame().getEntity(unitId);
            String name = (unit == null) ? String.valueOf(unitId) : unit.getShortName();
            if (!names.isEmpty()) {
                names.append(", ");
            }
            names.append(name);
            leaderCombo.addItem(new UnitOption(unitId, Messages.getString("BotCommandPanel.Orders.unit", unitId,
                  name)));
        }
        unitsLabel.setText("<html><b>" + group.label() + "</b><br>" + names + "</html>");
    }

    private void updateFormationEnabled() {
        boolean canForm = group.unitIds().size() >= 2;
        formationBox.setEnabled(canForm);
        if (!canForm) {
            formationBox.setSelected(false);
            formationBox.setToolTipText(Messages.getString("BotCommandPanel.Formations.needsTwoUnits"));
        } else {
            formationBox.setToolTipText(null);
        }
        boolean isInFormation = formationBox.isSelected();
        for (JComboBox<?> combo : List.of(shapeCombo, leaderCombo, spacingCombo, paceCombo, contactCombo)) {
            combo.setEnabled(isInFormation);
        }
    }

    private void chooseUnits() {
        BotUnitChooserDialog dialog = new BotUnitChooserDialog(clientGUI.getFrame(), unitsByLance.get());
        if ((dialog.showDialog() == DialogResult.CONFIRMED) && (dialog.getChosenGroup() != null)) {
            group = dialog.getChosenGroup();
            updateUnits();
            updateFormationEnabled();
        }
    }

    private void selectRow(int row) {
        if ((row >= 0) && (row < waypoints.getRowCount())) {
            waypointTable.getSelectionModel().setSelectionInterval(row, row);
        } else {
            waypointTable.clearSelection();
        }
        loadDetail();
    }

    /** Shows the selected waypoint's facing on the dial and its hold on the spinner. */
    private void loadDetail() {
        int row = waypointTable.getSelectedRow();
        isLoadingDetail = true;
        try {
            boolean hasRow = row >= 0;
            for (JRadioButton button : facingButtons) {
                button.setEnabled(hasRow);
            }
            autoFacingBox.setEnabled(hasRow);
            holdSpinner.setEnabled(hasRow && !waypoints.isEndOfRoute(row));
            if (!hasRow) {
                selectedLabel.setText(Messages.getString("BotCommandPanel.MoveOrder.noneSelected"));
                facingButtonGroup.clearSelection();
                return;
            }
            selectedLabel.setText(Messages.getString("BotCommandPanel.MoveOrder.selected", row + 1,
                  waypoints.getHex(row).getBoardNum()));
            int facing = waypoints.getFacing(row);
            autoFacingBox.setSelected(facing == UnitOrders.FACING_AUTO);
            if (facing == UnitOrders.FACING_AUTO) {
                facingButtonGroup.clearSelection();
            } else {
                facingButtons.get(facing).setSelected(true);
            }
            holdSpinner.setValue(waypoints.getHoldTurns(row));
        } finally {
            isLoadingDetail = false;
        }
    }

    private void setSelectedFacing(int facing) {
        int row = waypointTable.getSelectedRow();
        if (isLoadingDetail || (row < 0)) {
            return;
        }
        waypoints.setFacing(row, facing);
        loadDetail();
    }

    /** Starts adding a waypoint at each hex the player clicks on the board. */
    private void startPicking() {
        if (hexClickListener != null) {
            return;
        }
        if (clientGUI.getCurrentPanel() instanceof Distractable distractable) {
            suppressedDisplay = distractable;
            suppressedDisplay.setIgnoringEvents(true);
        }
        hexClickListener = new BoardViewListenerAdapter() {
            @Override
            public void hexMoused(BoardViewEvent event) {
                if ((event.getType() != BoardViewEvent.BOARD_HEX_CLICKED) || (event.getButton() != MouseEvent.BUTTON1)
                      || (event.getCoords() == null)) {
                    return;
                }
                waypoints.addWaypoint(event.getCoords());
                selectRow(waypoints.getRowCount() - 1);
            }
        };
        boardView.addBoardViewListener(hexClickListener);
        pickButton.setText(Messages.getString("BotCommandPanel.MoveOrder.picking"));
    }

    private void stopPicking() {
        if (hexClickListener != null) {
            boardView.removeBoardViewListener(hexClickListener);
            hexClickListener = null;
        }
        if (suppressedDisplay != null) {
            suppressedDisplay.setIgnoringEvents(false);
            suppressedDisplay = null;
        }
        pickButton.setText(Messages.getString("BotCommandPanel.MoveOrder.pick"));
    }

    /** Draws the draft route on the board: each waypoint outlined and numbered in route order. */
    private void refreshRouteSprites() {
        boardView.removeSprites(routeSprites);
        routeSprites.clear();
        for (int row = 0; row < waypoints.getRowCount(); row++) {
            Coords hex = waypoints.getHex(row);
            routeSprites.add(new FieldOfFireSprite(boardView, RangeType.RANGE_SHORT, hex, ALL_HEX_BORDERS));
            routeSprites.add(new TextMarkerSprite(boardView, hex, String.valueOf(row + 1), Color.WHITE));
        }
        boardView.addSprites(routeSprites);
    }

    private void cleanUp() {
        stopPicking();
        boardView.removeSprites(routeSprites);
        routeSprites.clear();
    }

    /** Sends the whole order to every unit of the group and closes the editor. */
    private void sendOrders() {
        if (waypointTable.isEditing()) {
            waypointTable.getCellEditor().stopCellEditing();
        }
        MoveOrderCommands.FormationChoice formation = null;
        if (formationBox.isSelected() && (group.unitIds().size() >= 2)) {
            UnitOption leader = (UnitOption) leaderCombo.getSelectedItem();
            formation = new MoveOrderCommands.FormationChoice((FormationShape) shapeCombo.getSelectedItem(),
                  (leader == null) ? group.unitIds().get(0) : leader.unitId(), (Integer) spacingCombo.getSelectedItem(),
                  (FormationPace) paceCombo.getSelectedItem(), (ContactRule) contactCombo.getSelectedItem());
        }
        List<String> commands = MoveOrderCommands.commands(group.unitIds(), formation, wasInFormation,
              waypoints.getHexes(), waypoints.getWaypointOrders(), (OrderPriority) priorityCombo.getSelectedItem());
        for (String command : commands) {
            clientGUI.getClient().sendChat(command);
        }
        LOGGER.info("[BotOrders] move order sent to {} of {}: {} waypoint(s), formation {}", group.label(),
              botPlayer.getName(), waypoints.getRowCount(), (formation == null) ? "none" : formation.shape());
        acknowledger.accept(botPlayer, Messages.getString("BotCommandPanel.MoveOrder.toast", waypoints.getRowCount(),
              group.label()));
        setResult(DialogResult.CONFIRMED);
        dispose();
    }
}
