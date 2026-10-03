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
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.swing.AbstractAction;
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
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import megamek.MegaMek;
import megamek.SuiteConstants;
import megamek.client.bot.princess.RoutePlanner;
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
import megamek.client.ui.util.UIUtil;
import megamek.common.Player;
import megamek.common.RangeType;
import megamek.common.annotations.Nullable;
import megamek.common.board.Coords;
import megamek.common.force.Force;
import megamek.common.orders.ContactRule;
import megamek.common.orders.FormationOrder;
import megamek.common.orders.FormationPace;
import megamek.common.orders.LanceRole;
import megamek.common.orders.LanceRoles;
import megamek.common.orders.OrderPriority;
import megamek.common.orders.PhaseLine;
import megamek.common.orders.RouteStyle;
import megamek.common.orders.UnitOrders;
import megamek.common.orders.WaypointFormation;
import megamek.common.orders.WaypointOrder;
import megamek.common.units.Entity;
import megamek.common.util.Distractable;
import megamek.logging.MMLogger;

/**
 * The Move Order editor: one window to give a group of a bot's units a route, each waypoint with the formation to
 * travel to it in, the facing on arrival and the turns to hold there, and to set how hard they push. It stays open
 * while the player clicks the board, and each click adds a waypoint; the draft route is numbered on the board as it
 * grows.
 *
 * <p>Sending turns the whole order into {@code /unitOrder} commands for every unit of the group (see
 * {@link MoveOrderCommands}), so the orders are stored on the units and saved with the game.</p>
 */
public class BotMoveOrderDialog extends AbstractButtonDialog {

    private static final MMLogger LOGGER = MMLogger.create(BotMoveOrderDialog.class);

    // bitmask for drawing all six hex edges of the highlight sprite
    private static final int ALL_HEX_BORDERS = 63;
    private static final int GAP = 8;
    private static final String REMOVE_WAYPOINT_ACTION = "removeWaypoint";
    // the columns' widths added up, and a header and four rows: the window opens wide and short (HammerGS, 2026-09-27)
    private static final int TABLE_WIDTH = 1150;
    private static final int TABLE_HEIGHT = 132;
    private static final int ROW_HEIGHT = 26;
    // narrowed to make room for the phase line column, as the approved mockup has them (HammerGS, 2026-10-02)
    private static final int NUMBER_COLUMN_WIDTH = 32;
    private static final int HEX_COLUMN_WIDTH = 56;
    private static final int SHAPE_COLUMN_WIDTH = 110;
    private static final int CHANGE_COLUMN_WIDTH = 112;
    private static final int SPACING_COLUMN_WIDTH = 80;
    private static final int PACE_COLUMN_WIDTH = 72;
    private static final int CONTACT_COLUMN_WIDTH = 160;
    private static final int TOGETHER_COLUMN_WIDTH = 90;
    private static final int FACING_COLUMN_WIDTH = 130;
    private static final int THEN_COLUMN_WIDTH = 136;
    private static final int TURNS_COLUMN_WIDTH = 56;
    private static final int PHASE_LINE_COLUMN_WIDTH = 110;
    private static final int ROUTE_TAB = 0;
    private static final int ROLE_TAB = 1;

    private final ClientGUI clientGUI;
    private final BoardView boardView;
    private final Player botPlayer;
    private final Supplier<Map<String, List<OrderGroup>>> unitsByLance;
    private final BiConsumer<Player, String> acknowledger;
    private OrderGroup group;

    private final WaypointTableModel waypoints = new WaypointTableModel();
    private final List<Sprite> routeSprites = new ArrayList<>();
    private JTable waypointTable;
    private JLabel unitsLabel;
    private JComboBox<UnitOption> leaderCombo;
    private JToggleButton pickButton;
    private JLabel helpLabel;
    private JComboBox<OrderPriority> priorityCombo;
    private JTabbedPane tabs;
    private LanceRolePanel rolePanel;
    private JButton roleButton;
    private JCheckBox planRouteBox;
    private JToggleButton autoRouteButton;
    private final Map<RouteStyle, JToggleButton> styleButtons = new EnumMap<>(RouteStyle.class);
    // true while a clicked hex is planned to rather than added as it is
    private boolean isAutoRouting;
    // dragging a waypoint on the board: its row, or -1; whether it has moved; the display held still while it drags,
    // when picking was not already holding it; and the release that ended the last drag, which is no click
    private final BoardViewListenerAdapter dragListener = new BoardViewListenerAdapter() {
        @Override
        public void hexMoused(BoardViewEvent event) {
            dragWaypoint(event);
        }
    };
    private int draggedRow = -1;
    private boolean hasDragMoved;
    private Distractable dragHeldDisplay;
    private BoardViewEvent dragEndingClick;
    // the role kind last shown, so picking Convoy can tick Plan the route
    private String shownRoleKind = "";
    private BoardViewListenerAdapter hexClickListener;
    private Distractable suppressedDisplay;

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
                // window sizes and positions are otherwise saved only on a clean exit from the main menu, and a game
                // is often left some other way; save now so the editor opens where the player left it
                MegaMek.getMMPreferences().saveToFile(SuiteConstants.MM_PREFERENCES_FILE);
            }
        });
        loadCurrentOrders();
        waypoints.addTableModelListener(event -> {
            refreshRouteSprites();
            // the line under the table follows the selected waypoint's settings as they are changed
            loadDetail();
        });
        refreshRouteSprites();
        // a waypoint's flag can be dragged to another hex while the editor is open (HammerGS, 2026-10-03)
        boardView.addBoardViewListener(dragListener);
        if ((waypoints.getRowCount() == 0) && !rolePanel.isEscortChosen()) {
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
        // the units, leader and priority above the tabs belong to both: the route and the role are sent to the same
        // units together (HammerGS, 2026-10-02)
        tabs = new JTabbedPane();
        tabs.setAlignmentX(Component.LEFT_ALIGNMENT);
        tabs.addTab(Messages.getString("BotCommandPanel.MoveOrder.tab.route"), createWaypointsPanel());
        tabs.addTab(Messages.getString("BotCommandPanel.MoveOrder.tab.role"), createRolePanel());
        content.add(tabs);
        return content;
    }

    private JPanel createRolePanel() {
        Entity first = firstUnit();
        int forceId = (first == null) ? Force.NO_FORCE : first.getForceId();
        rolePanel = new LanceRolePanel(group.label(), LanceRoles.convoyChoices(clientGUI.getClient().getGame(),
              botPlayer, forceId), LanceRoles.defaultExitEdge(first));
        rolePanel.addChangeListener(this::updateRouteTab);
        rolePanel.addChangeListener(this::loadDetail);
        return rolePanel;
    }

    /**
     * @return the role chosen on the Role tab, as its message key ends: NONE, CONVOY or ESCORT
     */
    private String roleChoiceName() {
        if (rolePanel.isEscortChosen()) {
            return LanceRole.Kind.ESCORT.name();
        }
        LanceRole role = rolePanel.getRole();
        return (role == null) ? LanceRole.NONE_TEXT : role.getKind().name();
    }

    /** An escort keeps its places round its convoy, so it has no route of its own to set. */
    private void updateRouteTab() {
        String roleKind = roleChoiceName();
        if (!roleKind.equals(shownRoleKind) && LanceRole.Kind.CONVOY.name().equals(roleKind)
              && (waypoints.getRowCount() == 0)) {
            // a convoy plans its route unless the player says otherwise
            planRouteBox.setSelected(true);
        }
        shownRoleKind = roleKind;
        String roleName = Messages.getString("BotCommandPanel.Role." + roleKind);
        roleButton.setText(Messages.getString("BotCommandPanel.MoveOrder.roleButton", roleName));
        tabs.setTitleAt(ROLE_TAB, Messages.getString("BotCommandPanel.MoveOrder.tab.roleNamed", roleName));
        boolean isEscort = rolePanel.isEscortChosen();
        tabs.setEnabledAt(ROUTE_TAB, !isEscort);
        tabs.setToolTipTextAt(ROUTE_TAB, isEscort ? Messages.getString("BotCommandPanel.MoveOrder.tab.routeEscort")
              : null);
        if (isEscort) {
            pickButton.setSelected(false);
            stopPicking();
        }
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
        leaderCombo = new JComboBox<>();
        leaderCombo.setToolTipText(Messages.getString("BotCommandPanel.MoveOrder.leader.tooltip"));
        priorityCombo = new JComboBox<>(OrderPriority.values());
        priorityCombo.setRenderer(labelRenderer("BotCommandPanel.Orders.priority."));
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.TRAILING, UIUtil.scaleForGUI(GAP), 0));
        controls.add(new JLabel(Messages.getString("BotCommandPanel.Formations.leader")));
        controls.add(leaderCombo);
        controls.add(new JLabel(Messages.getString("BotCommandPanel.Orders.priority")));
        controls.add(priorityCombo);
        // the role, in the row everyone reads first: the Role tab alone was easy to miss (HammerGS, 2026-10-03)
        roleButton = new JButton();
        roleButton.setToolTipText(Messages.getString("BotCommandPanel.MoveOrder.roleButton.tooltip"));
        roleButton.addActionListener(event -> tabs.setSelectedIndex(ROLE_TAB));
        controls.add(roleButton);
        // the bot works out the whole way to each waypoint and adds the turning points (HammerGS, 2026-10-03)
        planRouteBox = new JCheckBox(Messages.getString("BotCommandPanel.MoveOrder.planRoute"));
        planRouteBox.setToolTipText(Messages.getString("BotCommandPanel.MoveOrder.planRoute.tooltip"));
        controls.add(planRouteBox);
        controls.add(chooseButton);
        panel.add(controls, BorderLayout.LINE_END);
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

    private JPanel createWaypointsPanel() {
        JPanel panel = section("BotCommandPanel.MoveOrder.waypoints");

        JPanel toolbar = new JPanel(new BorderLayout(UIUtil.scaleForGUI(GAP), 0));
        pickButton = new JToggleButton(Messages.getString("BotCommandPanel.MoveOrder.pick"));
        pickButton.addActionListener(event -> pickMode(pickButton.isSelected(), false));
        // click a hex and the bot plans the way there in the chosen style, the turning points filled in as waypoints
        // (HammerGS, 2026-10-03)
        autoRouteButton = new JToggleButton(Messages.getString("BotCommandPanel.MoveOrder.autoRoute"));
        autoRouteButton.setToolTipText(Messages.getString("BotCommandPanel.MoveOrder.autoRoute.tooltip"));
        autoRouteButton.addActionListener(event -> pickMode(autoRouteButton.isSelected(), true));
        JPanel pickControls = new JPanel(new FlowLayout(FlowLayout.LEADING, UIUtil.scaleForGUI(GAP), 0));
        pickControls.add(pickButton);
        pickControls.add(autoRouteButton);
        pickControls.add(new JLabel(Messages.getString("BotCommandPanel.MoveOrder.routeStyle")));
        ButtonGroup styleGroup = new ButtonGroup();
        JPanel styles = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        for (RouteStyle style : RouteStyle.values()) {
            JToggleButton styleButton = new JToggleButton(Messages.getString("BotCommandPanel.MoveOrder.routeStyle."
                  + style.name()));
            styleButton.setToolTipText(Messages.getString("BotCommandPanel.MoveOrder.routeStyle." + style.name()
                  + ".tooltip"));
            styleGroup.add(styleButton);
            styleButtons.put(style, styleButton);
            styles.add(styleButton);
        }
        styleButtons.get(RouteStyle.FASTEST).setSelected(true);
        pickControls.add(styles);
        toolbar.add(pickControls, BorderLayout.LINE_START);
        toolbar.add(new JLabel(Messages.getString("BotCommandPanel.MoveOrder.pickHint")), BorderLayout.CENTER);
        JButton clearButton = new JButton(Messages.getString("BotCommandPanel.MoveOrder.clear"));
        clearButton.addActionListener(event -> waypoints.clear());
        toolbar.add(clearButton, BorderLayout.LINE_END);
        panel.add(toolbar, BorderLayout.PAGE_START);

        waypointTable = new JTable(waypoints);
        waypointTable.setRowHeight(UIUtil.scaleForGUI(ROW_HEIGHT));
        waypointTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setUpColumns();
        waypointTable.getColumnModel().getColumn(WaypointTableModel.COLUMN_THEN).setCellEditor(new ThenCellEditor());
        waypointTable.getColumnModel().getColumn(WaypointTableModel.COLUMN_THEN)
              .setCellRenderer(new ComboCellRenderer(String::valueOf));
        waypointTable.getColumnModel().getColumn(WaypointTableModel.COLUMN_TURNS).setCellEditor(new TurnsCellEditor());
        waypointTable.getColumnModel().getColumn(WaypointTableModel.COLUMN_TURNS)
              .setCellRenderer(new TurnsCellRenderer());
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
        removeButton.addActionListener(event -> removeSelectedWaypoint());
        // Delete or Backspace on the table removes the selected waypoint too; a cell being edited keeps its own keys,
        // since the key then goes to the editor rather than the table
        waypointTable.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0),
              REMOVE_WAYPOINT_ACTION);
        waypointTable.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0),
              REMOVE_WAYPOINT_ACTION);
        waypointTable.getActionMap().put(REMOVE_WAYPOINT_ACTION, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                removeSelectedWaypoint();
            }
        });
        rowButtons.add(upButton);
        rowButtons.add(downButton);
        rowButtons.add(removeButton);
        below.add(rowButtons, BorderLayout.PAGE_START);
        // one line on the selected waypoint, as wide as the table; the HTML wraps it when the text runs long
        helpLabel = new JLabel();
        below.add(helpLabel, BorderLayout.CENTER);
        panel.add(below, BorderLayout.PAGE_END);
        return panel;
    }

    /**
     * Sets each column's width, and the dropdowns, spinner and tick box that edit it. Every editable cell draws as its
     * control even when not being edited, so the player can see what can be changed.
     */
    private void setUpColumns() {
        TableColumnModel columns = waypointTable.getColumnModel();
        int[] widths = {NUMBER_COLUMN_WIDTH, HEX_COLUMN_WIDTH, SHAPE_COLUMN_WIDTH, CHANGE_COLUMN_WIDTH,
              SPACING_COLUMN_WIDTH, PACE_COLUMN_WIDTH, CONTACT_COLUMN_WIDTH, TOGETHER_COLUMN_WIDTH, FACING_COLUMN_WIDTH,
              THEN_COLUMN_WIDTH, TURNS_COLUMN_WIDTH, PHASE_LINE_COLUMN_WIDTH};
        for (int column = 0; column < widths.length; column++) {
            columns.getColumn(column).setPreferredWidth(UIUtil.scaleForGUI(widths[column]));
        }
        List<Integer> spacings = new ArrayList<>();
        for (int spacing = FormationOrder.MINIMUM_SPACING; spacing <= FormationOrder.MAXIMUM_SPACING; spacing++) {
            spacings.add(spacing);
        }
        setComboColumn(columns.getColumn(WaypointTableModel.COLUMN_SHAPE),
              WaypointTableModel.shapeOptions().toArray(), String::valueOf);
        setComboColumn(columns.getColumn(WaypointTableModel.COLUMN_CHANGE), WaypointTableModel.Change.values(),
              String::valueOf);
        setComboColumn(columns.getColumn(WaypointTableModel.COLUMN_SPACING), spacings.toArray(),
              value -> Messages.getString("BotCommandPanel.Formations.spacing.hexes", value));
        setComboColumn(columns.getColumn(WaypointTableModel.COLUMN_PACE), FormationPace.values(),
              value -> Messages.getString("BotCommandPanel.Formations.pace." + ((Enum<?>) value).name()));
        setComboColumn(columns.getColumn(WaypointTableModel.COLUMN_CONTACT), ContactRule.values(),
              value -> Messages.getString("BotCommandPanel.Formations.contact." + ((Enum<?>) value).name()));
        setComboColumn(columns.getColumn(WaypointTableModel.COLUMN_FACING),
              WaypointTableModel.facingOptions().toArray(), String::valueOf);
        columns.getColumn(WaypointTableModel.COLUMN_PHASE_LINE).setCellEditor(new PhaseLineCellEditor());
        columns.getColumn(WaypointTableModel.COLUMN_PHASE_LINE).setCellRenderer(new ComboCellRenderer(String::valueOf));
    }

    /**
     * Edits a waypoint's phase line: none, one already used by any of the player's lances, or a new one, named with
     * the next ICAO name or one the player types.
     */
    private final class PhaseLineCellEditor extends AbstractCellEditor implements TableCellEditor {
        private final JComboBox<Object> combo = new JComboBox<>();
        private boolean isFilling;

        private PhaseLineCellEditor() {
            // the player may also type a name of their own
            combo.setEditable(true);
            // a pick, or Enter after typing, takes effect at once rather than when the player clicks elsewhere
            combo.addActionListener(event -> {
                if (!isFilling) {
                    stopCellEditing();
                }
            });
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row,
              int column) {
            isFilling = true;
            combo.removeAllItems();
            combo.addItem(WaypointTableModel.PhaseLineOption.NONE);
            List<String> known = knownPhaseLines();
            for (String name : known) {
                combo.addItem(new WaypointTableModel.PhaseLineOption(name, false));
            }
            // the next free ICAO name, ready to pick: the first lance's order has nothing to join yet
            combo.addItem(new WaypointTableModel.PhaseLineOption(PhaseLine.nextName(known), true));
            combo.setSelectedItem(value);
            isFilling = false;
            return combo;
        }

        @Override
        public Object getCellEditorValue() {
            Object selected = combo.getSelectedItem();
            if (selected instanceof WaypointTableModel.PhaseLineOption option) {
                return option;
            }
            return WaypointTableModel.PhaseLineOption.typed((selected == null) ? "" : selected.toString());
        }
    }

    /**
     * @return the phase lines already on any route of the player's side and on this one, so a second lance joins the
     *       same line by picking it
     */
    private List<String> knownPhaseLines() {
        List<String> names = new ArrayList<>(waypoints.phaseLinesInUse());
        for (String name : PhaseLine.namesInUse(clientGUI.getClient().getGame().getEntitiesVector(), botPlayer)) {
            if (!containsIgnoringCase(names, name)) {
                names.add(name);
            }
        }
        return names;
    }

    private static boolean containsIgnoringCase(List<String> names, String name) {
        for (String candidate : names) {
            if (candidate.equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    private static void setComboColumn(TableColumn column, Object[] choices, Function<Object, String> label) {
        JComboBox<Object> editor = new JComboBox<>(choices);
        editor.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                  boolean isSelected, boolean hasFocus) {
                return super.getListCellRendererComponent(list, (value == null) ? "" : label.apply(value), index,
                      isSelected, hasFocus);
            }
        });
        column.setCellEditor(new DefaultCellEditor(editor));
        column.setCellRenderer(new ComboCellRenderer(label));
    }

    /** Draws a cell as a dropdown showing its value, greyed out where the cell cannot be changed. */
    private static final class ComboCellRenderer implements TableCellRenderer {
        private final JComboBox<String> combo = new JComboBox<>();
        private final Function<Object, String> label;

        private ComboCellRenderer(Function<Object, String> label) {
            this.label = label;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
              boolean hasFocus, int row, int column) {
            combo.removeAllItems();
            if (value != null) {
                combo.addItem(label.apply(value));
            }
            combo.setEnabled(table.getModel().isCellEditable(row, table.convertColumnIndexToModel(column)));
            return combo;
        }
    }

    /**
     * Draws the turns as a spinner on every row, greyed out where the waypoint neither holds nor waits to assemble.
     */
    private final class TurnsCellRenderer implements TableCellRenderer {
        private final JSpinner spinner = new JSpinner(new SpinnerNumberModel(0, 0,
              WaypointTableModel.MAXIMUM_HOLD_TURNS, 1));

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
              boolean hasFocus, int row, int column) {
            spinner.setValue(waypoints.getHoldTurns(row));
            spinner.setEnabled(waypoints.isCellEditable(row, WaypointTableModel.COLUMN_TURNS));
            return spinner;
        }
    }

    /** Edits the turns with a spinner, 0 to {@link WaypointTableModel#MAXIMUM_HOLD_TURNS}. */
    private final class TurnsCellEditor extends AbstractCellEditor implements TableCellEditor {
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

    /**
     * Edits what the units do on reaching a waypoint, offering pass, hold or assemble part-way along the route and
     * stay or exit at its end.
     */
    private final class ThenCellEditor extends AbstractCellEditor implements TableCellEditor {
        private final JComboBox<WaypointTableModel.Then> combo = new JComboBox<>();

        private ThenCellEditor() {
            combo.addActionListener(event -> stopCellEditing());
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row,
              int column) {
            combo.removeAllItems();
            for (WaypointTableModel.Then then : waypoints.thenOptions(row)) {
                combo.addItem(then);
            }
            combo.setSelectedItem(waypoints.getThen(row));
            return combo;
        }

        @Override
        public Object getCellEditorValue() {
            return combo.getSelectedItem();
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
        updateCanForm();
        // a leg that sets no formation of its own travels in the units' formation, if they have one
        WaypointFormation unitsFormation = orders.getFormation().map(unitsOrder -> new WaypointFormation(
              unitsOrder.getShape(), unitsOrder.getSpacing(), unitsOrder.getPace(), unitsOrder.getContactRule(),
              unitsOrder.isKeepTogether())).orElse(WaypointFormation.NONE);
        // the whole route the units follow, the turning points the bot planned among it, marked as planned
        // (HammerGS, 2026-10-03: a route the bot made for itself should show here)
        boolean isPlanned = false;
        for (int index = 0; index < orders.getRoute().size(); index++) {
            WaypointOrder order = orders.getWaypointOrder(index);
            if (order.getRoutePlan() == WaypointOrder.RoutePlan.PLAN_LEG) {
                isPlanned = true;
                styleButtons.get(order.getRouteStyle()).setSelected(true);
            }
        }
        waypoints.setRoute(orders.getRoute(), orders.getWaypointOrders(), unitsFormation);
        planRouteBox.setSelected(isPlanned);
        priorityCombo.setSelectedItem(orders.getPriority());
        rolePanel.setRole(LanceRoles.roleOf(units()));
        updateRouteTab();
        if (rolePanel.isEscortChosen()) {
            tabs.setSelectedIndex(ROLE_TAB);
        }
        Optional<FormationOrder> formation = orders.getFormation();
        int leaderId = formation.map(FormationOrder::getLeaderId).orElse(group.unitIds().get(0));
        for (int index = 0; index < leaderCombo.getItemCount(); index++) {
            if (leaderCombo.getItemAt(index).unitId() == leaderId) {
                leaderCombo.setSelectedIndex(index);
            }
        }
        selectRow(waypoints.getRowCount() - 1);
    }

    private List<Entity> units() {
        List<Entity> units = new ArrayList<>();
        for (int unitId : group.unitIds()) {
            Entity unit = clientGUI.getClient().getGame().getEntity(unitId);
            if (unit != null) {
                units.add(unit);
            }
        }
        return units;
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

    /**
     * @return {@code true} if any of the units being ordered is in a formation now, so taking them out of formation
     *       has to be sent; read when sending, since the units can be changed with Choose units
     */
    private boolean isAnyUnitInFormation() {
        for (int unitId : group.unitIds()) {
            Entity unit = clientGUI.getClient().getGame().getEntity(unitId);
            if ((unit != null) && unit.getUnitOrders().getFormation().isPresent()) {
                return true;
            }
        }
        return false;
    }

    /** A group of one unit travels out of formation: it has no one to form on. */
    private void updateCanForm() {
        boolean canForm = group.unitIds().size() >= 2;
        waypoints.setCanForm(canForm);
        leaderCombo.setEnabled(canForm);
    }

    private void chooseUnits() {
        BotUnitChooserDialog dialog = new BotUnitChooserDialog(clientGUI.getFrame(), unitsByLance.get());
        if ((dialog.showDialog() == DialogResult.CONFIRMED) && (dialog.getChosenGroup() != null)) {
            group = dialog.getChosenGroup();
            updateUnits();
            updateCanForm();
        }
    }

    private void removeSelectedWaypoint() {
        int row = waypointTable.getSelectedRow();
        if (row < 0) {
            return;
        }
        waypoints.removeWaypoint(row);
        selectRow(Math.min(row, waypoints.getRowCount() - 1));
    }

    private void selectRow(int row) {
        if ((row >= 0) && (row < waypoints.getRowCount())) {
            waypointTable.getSelectionModel().setSelectionInterval(row, row);
        } else {
            waypointTable.clearSelection();
        }
        loadDetail();
    }

    /** Shows what the units do at the selected waypoint, in one line under the table. */
    private void loadDetail() {
        int row = waypointTable.getSelectedRow();
        String text = (row < 0) ? noWaypointText() : waypoints.describe(row);
        helpLabel.setText("<html><div style='width:" + UIUtil.scaleForGUI(TABLE_WIDTH - (2 * GAP)) + "px'>"
              + text + "</div></html>");
    }

    /**
     * @return what the line under the table says with no waypoint picked: for a convoy with no route, what it will do
     *       on its own once on the board
     */
    private String noWaypointText() {
        LanceRole role = (rolePanel == null) ? null : rolePanel.getRole();
        if ((waypoints.getRowCount() == 0) && (role != null) && role.isConvoy()) {
            return role.isWaitingAtRouteEnd() ? Messages.getString("BotCommandPanel.MoveOrder.convoyWaits")
                  : Messages.getString("BotCommandPanel.MoveOrder.convoyHeadsOut",
                        Messages.getString("BotCommandPanel.Role.edge." + role.getExitEdge().name()));
        }
        return Messages.getString("BotCommandPanel.MoveOrder.noneSelected");
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
                if ((event == dragEndingClick) || ((draggedRow >= 0) && hasDragMoved)) {
                    // the release at the end of a drag moved a waypoint: it adds none
                    return;
                }
                if (isAutoRouting) {
                    autoRouteTo(event.getCoords());
                } else {
                    waypoints.addWaypoint(event.getCoords());
                }
                selectRow(waypoints.getRowCount() - 1);
            }
        };
        boardView.addBoardViewListener(hexClickListener);
        if (isAutoRouting) {
            autoRouteButton.setText(Messages.getString("BotCommandPanel.MoveOrder.autoRouting"));
        } else {
            pickButton.setText(Messages.getString("BotCommandPanel.MoveOrder.picking"));
        }
    }

    /**
     * Starts or stops taking clicked hexes, as waypoints or as places to plan a route to; the two buttons are one
     * or the other.
     */
    private void pickMode(boolean isOn, boolean isAuto) {
        isAutoRouting = isOn && isAuto;
        pickButton.setSelected(isOn && !isAuto);
        autoRouteButton.setSelected(isOn && isAuto);
        if (isOn) {
            startPicking();
        } else {
            stopPicking();
        }
    }

    /**
     * Plans the way from the last waypoint, or from the leader where it stands, to the hex clicked, and adds the
     * turning points and the hex as waypoints. A leader not yet on the board has nowhere to plan from: the hex is
     * added alone, and the bot plans the way once it is down.
     */
    private void autoRouteTo(Coords target) {
        UnitOption leader = (UnitOption) leaderCombo.getSelectedItem();
        Entity leaderUnit = (leader == null) ? firstUnit() : clientGUI.getClient().getGame().getEntity(leader.unitId());
        Coords from = (waypoints.getRowCount() > 0) ? waypoints.getHex(waypoints.getRowCount() - 1)
              : ((leaderUnit == null) ? null : leaderUnit.getPosition());
        RouteStyle style = routeStyle();
        if ((leaderUnit == null) || (from == null)) {
            waypoints.addWaypoint(target);
            planRouteBox.setSelected(true);
            helpLabel.setText(Messages.getString("BotCommandPanel.MoveOrder.autoRoute.notDeployed"));
            return;
        }
        List<Coords> route = RoutePlanner.plan(leaderUnit, from, target, style);
        for (int index = 0; index < route.size(); index++) {
            // the turning points are the bot's; the hex clicked is the player's
            waypoints.addWaypoint(route.get(index), index < (route.size() - 1));
        }
        LOGGER.info("[BotOrders] auto route for {} from {} to {} ({}): {} waypoint(s)", group.label(),
              from.getBoardNum(), target.getBoardNum(), style, route.size());
    }

    /**
     * @return the route style picked in the toolbar
     */
    private RouteStyle routeStyle() {
        for (Map.Entry<RouteStyle, JToggleButton> entry : styleButtons.entrySet()) {
            if (entry.getValue().isSelected()) {
                return entry.getKey();
            }
        }
        return RouteStyle.FASTEST;
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
        autoRouteButton.setText(Messages.getString("BotCommandPanel.MoveOrder.autoRoute"));
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

    /**
     * Drags a waypoint: pressed on one of the route's hexes, it follows the mouse hex by hex, and the release leaves
     * it there. The display under the board is held still while it drags, so the drag selects and moves nothing.
     */
    private void dragWaypoint(BoardViewEvent event) {
        Coords hex = event.getCoords();
        if (hex == null) {
            return;
        }
        if (event.getType() == BoardViewEvent.BOARD_HEX_DRAGGED) {
            if (draggedRow < 0) {
                // the press: a drag starts only on one of the route's hexes, with the left button
                if (event.getButton() != MouseEvent.BUTTON1) {
                    // the right button pans the board; only the left drags a waypoint
                    return;
                }
                int row = waypoints.rowAt(hex);
                LOGGER.info("[BotOrders] Move Order editor: press at {} - {}", hex.getBoardNum(), (row >= 0)
                      ? "picked up waypoint " + (row + 1) + ", drag it and release"
                      : "not one of the route's " + waypoints.getRowCount() + " waypoint(s), nothing to drag");
                if (row >= 0) {
                    draggedRow = row;
                    hasDragMoved = false;
                    if ((suppressedDisplay == null) && (clientGUI.getCurrentPanel() instanceof Distractable display)) {
                        dragHeldDisplay = display;
                        dragHeldDisplay.setIgnoringEvents(true);
                    }
                }
                return;
            }
            if (!hex.equals(waypoints.getHex(draggedRow))) {
                waypoints.moveWaypoint(draggedRow, hex);
                hasDragMoved = true;
                selectRow(draggedRow);
            }
        } else if ((event.getType() == BoardViewEvent.BOARD_HEX_CLICKED) && (draggedRow >= 0)) {
            if (hasDragMoved) {
                dragEndingClick = event;
                LOGGER.info("[BotOrders] Move Order editor: waypoint {} dragged to {}", draggedRow + 1,
                      hex.getBoardNum());
            }
            endDrag();
        }
    }

    private void endDrag() {
        draggedRow = -1;
        hasDragMoved = false;
        if (dragHeldDisplay != null) {
            dragHeldDisplay.setIgnoringEvents(false);
            dragHeldDisplay = null;
        }
    }

    private void cleanUp() {
        boardView.removeBoardViewListener(dragListener);
        endDrag();
        stopPicking();
        boardView.removeSprites(routeSprites);
        routeSprites.clear();
    }

    /**
     * Sends the whole order - the route from the Route tab and the role from the Role tab - to every unit of the group
     * and closes the editor. An escort gets only its role: it keeps round its convoy instead of following a route.
     */
    private void sendOrders() {
        if (waypointTable.isEditing()) {
            waypointTable.getCellEditor().stopCellEditing();
        }
        if (!rolePanel.isComplete()) {
            tabs.setSelectedIndex(ROLE_TAB);
            return;
        }
        LanceRole role = rolePanel.getRole();
        for (String command : MoveOrderCommands.roleCommands(units(), role)) {
            clientGUI.getClient().sendChat(command);
        }
        if (rolePanel.lanceToMakeConvoy().isPresent()) {
            LanceRoles.ConvoyChoice lance = rolePanel.lanceToMakeConvoy().get();
            for (String command : MoveOrderCommands.makeConvoyCommands(
                  LanceRoles.unitsOf(clientGUI.getClient().getGame(), lance.forceId()), lance.newExitEdge())) {
                clientGUI.getClient().sendChat(command);
            }
            LOGGER.info("[BotOrders] {} made a convoy, leaving by the {} edge, to be escorted by {}", lance.name(),
                  lance.newExitEdge(), group.label());
        }
        if (rolePanel.isEscortChosen()) {
            LOGGER.info("[BotOrders] escort role sent to {} of {}: {}", group.label(), botPlayer.getName(), role);
            acknowledger.accept(botPlayer, Messages.getString("BotCommandPanel.MoveOrder.toastEscort", group.label()));
            setResult(DialogResult.CONFIRMED);
            dispose();
            return;
        }
        UnitOption leader = (UnitOption) leaderCombo.getSelectedItem();
        int leaderId = (leader == null) ? group.unitIds().get(0) : leader.unitId();
        List<WaypointOrder> sentOrders = MoveOrderCommands.withRoutePlan(waypoints.getWaypointOrders(),
              planRouteBox.isSelected(), routeStyle());
        List<String> commands = MoveOrderCommands.commands(group.unitIds(), leaderId, isAnyUnitInFormation(),
              waypoints.getHexes(), sentOrders, (OrderPriority) priorityCombo.getSelectedItem());
        for (String command : commands) {
            clientGUI.getClient().sendChat(command);
        }
        boolean isConvoyColumn = (role != null) && role.isConvoy() && (group.unitIds().size() >= 2)
              && !MoveOrderCommands.setsFormation(waypoints.getWaypointOrders());
        if (isConvoyColumn) {
            List<Integer> columnOrder = new ArrayList<>();
            columnOrder.add(leaderId);
            for (int unitId : group.unitIds()) {
                if (unitId != leaderId) {
                    columnOrder.add(unitId);
                }
            }
            for (String command : MoveOrderCommands.columnCommands(columnOrder)) {
                clientGUI.getClient().sendChat(command);
            }
        }
        LOGGER.info("[BotOrders] move order sent to {} of {}: {} waypoint(s) {}, leader {}, role {}{}", group.label(),
              botPlayer.getName(), waypoints.getRowCount(), waypoints.getWaypointOrders(), leaderId,
              (role == null) ? LanceRole.NONE_TEXT : role, isConvoyColumn ? ", travelling in a Column" : "");
        acknowledger.accept(botPlayer, Messages.getString("BotCommandPanel.MoveOrder.toast", waypoints.getRowCount(),
              group.label()));
        setResult(DialogResult.CONFIRMED);
        dispose();
    }
}
