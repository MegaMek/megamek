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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

import megamek.client.AbstractClient;
import megamek.client.ui.Messages;
import megamek.client.ui.util.MenuScroller;
import megamek.common.OffBoardDirection;
import megamek.common.Player;
import megamek.common.annotations.Nullable;
import megamek.common.board.Coords;
import megamek.common.force.Force;
import megamek.common.game.Game;
import megamek.common.orders.OrderPriority;
import megamek.common.orders.UnitOrderAction;
import megamek.common.orders.UnitOrders;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;
import megamek.server.commands.UnitOrderCommand;

/**
 * Builds the Orders menu for one bot: the per-unit orders a player can give its units - a route, an edge to move to
 * or exit by, facing, route priority, pause, resume, stop and clear.
 *
 * <p>The Bot Commands panel and the map's right-click menu both build their Orders menus here, so the two always
 * offer the same orders. Each order goes to every unit of the chosen group - all units, one lance, or one unit - as
 * a {@code /unitOrder} command; the orders are stored on the units, so they are saved with the game.</p>
 *
 * <p>MegaMek selects one unit at a time on the board, so groups come from the force tree: set up lances in the lobby
 * to order them together.</p>
 */
public class BotOrdersMenuBuilder {

    private static final MMLogger LOGGER = MMLogger.create(BotOrdersMenuBuilder.class);

    /** How many groups a menu shows before it scrolls, so a bot with thirty lances still fits on screen. */
    private static final int GROUP_SCROLL_THRESHOLD = 20;

    private static final int FACING_COUNT = 6;

    /**
     * Lets the player pick hexes on the board, or type them where there is no board view.
     */
    @FunctionalInterface
    public interface HexPicker {
        /**
         * @param orderDescription what the hexes are for, shown while picking
         * @param singleHex        {@code true} to stop after one hex
         * @param onPicked         receives the hexes as hex numbers joined with dashes, e.g. {@code 1508-1504}
         */
        void pickHexes(String orderDescription, boolean singleHex, Consumer<String> onPicked);
    }

    /**
     * A named set of one bot's units that an order goes to.
     *
     * @param label   the menu label, e.g. "Command Lance (3)"
     * @param unitIds the units
     */
    public record OrderGroup(String label, List<Integer> unitIds) {}

    /**
     * Lets the player set the facing while moving and the facing when stopped in one go.
     */
    @FunctionalInterface
    public interface FacingChooser {
        /**
         * @param group             the units being ordered
         * @param facingWhileMoving the facing while moving now, 0-5 or {@link UnitOrders#FACING_AUTO}
         * @param facingWhenStopped the facing when stopped now, 0-5 or {@link UnitOrders#FACING_AUTO}
         * @param onChosen          receives the facing while moving and the facing when stopped
         */
        void chooseFacings(OrderGroup group, int facingWhileMoving, int facingWhenStopped,
              BiConsumer<Integer, Integer> onChosen);
    }

    private final AbstractClient client;
    private final HexPicker hexPicker;
    private final BiConsumer<Player, String> acknowledger;
    // null when the host has no facing dialog; facings are then set from submenus
    private FacingChooser facingChooser;

    /**
     * @param client       the client that sends the orders
     * @param hexPicker    how the player picks hexes for a route
     * @param acknowledger shows the player that an order was sent: the bot and a description of the order
     */
    public BotOrdersMenuBuilder(AbstractClient client, HexPicker hexPicker, BiConsumer<Player, String> acknowledger) {
        this.client = client;
        this.hexPicker = hexPicker;
        this.acknowledger = acknowledger;
    }

    /**
     * @param chooser the facing dialog; without one, facings are set from submenus
     *
     * @return this builder
     */
    public BotOrdersMenuBuilder withFacingChooser(FacingChooser chooser) {
        facingChooser = chooser;
        return this;
    }

    /**
     * Adds one submenu per group of the bot's units to the given menu, each holding every order.
     *
     * @param botMenu    the menu to fill
     * @param botPlayer  the bot
     * @param clickedHex the hex the player right-clicked, which routes go to directly; {@code null} in the Bot
     *                   Commands panel, where the player picks hexes instead
     */
    public void populate(JMenu botMenu, Player botPlayer, @Nullable Coords clickedHex) {
        for (OrderGroup group : groupsFor(botPlayer)) {
            JMenu groupMenu = new JMenu(group.label());
            addOrders(groupMenu, botPlayer, group, clickedHex);
            botMenu.add(groupMenu);
        }
        if (botMenu.getItemCount() > GROUP_SCROLL_THRESHOLD) {
            MenuScroller.setScrollerFor(botMenu, GROUP_SCROLL_THRESHOLD);
        }
    }

    /**
     * Adds one submenu per group of the bot's units holding the one-click orders: pause, resume, stop, clear, an edge
     * to move to or exit by, the route's priority and leaving a formation. Routes, facings and formations are set up
     * in the Move Order editor.
     *
     * @param botMenu   the menu to fill
     * @param botPlayer the bot
     */
    public void populateQuick(JMenu botMenu, Player botPlayer) {
        for (OrderGroup group : groupsFor(botPlayer)) {
            JMenu groupMenu = new JMenu(group.label());
            addOrder(groupMenu, botPlayer, group, "pause", UnitOrderAction.PAUSE);
            addOrder(groupMenu, botPlayer, group, "resume", UnitOrderAction.RESUME);
            addOrder(groupMenu, botPlayer, group, "stop", UnitOrderAction.STOP);
            addOrder(groupMenu, botPlayer, group, "clear", UnitOrderAction.CLEAR);
            groupMenu.addSeparator();
            groupMenu.add(createEdgeMenu(botPlayer, group));
            groupMenu.add(createPriorityMenu(botPlayer, group));
            groupMenu.addSeparator();
            addOrder(groupMenu, botPlayer, group, "formationOff", UnitOrderAction.FORMATION_OFF);
            botMenu.add(groupMenu);
        }
        if (botMenu.getItemCount() > GROUP_SCROLL_THRESHOLD) {
            MenuScroller.setScrollerFor(botMenu, GROUP_SCROLL_THRESHOLD);
        }
    }

    /**
     * Lists the groups orders can go to: all the bot's units on the board, each lance holding any of them, and each
     * unit on its own.
     *
     * @param botPlayer the bot
     *
     * @return the groups; empty when the bot has no units on the board
     */
    List<OrderGroup> groupsFor(Player botPlayer) {
        List<OrderGroup> groups = new ArrayList<>();
        if (!(client.getGame() instanceof Game game)) {
            return groups;
        }
        List<Entity> units = new ArrayList<>();
        for (Entity entity : game.getPlayerEntities(botPlayer, false)) {
            if (canTakeOrders(entity)) {
                units.add(entity);
            }
        }
        if (units.isEmpty()) {
            return groups;
        }
        groups.add(new OrderGroup(Messages.getString("BotCommandPanel.Orders.allUnits", units.size()),
              idsOf(units)));
        for (Force force : game.getForces().getAllForces()) {
            if (force.getOwnerId() != botPlayer.getId()) {
                continue;
            }
            List<Integer> lanceUnitIds = new ArrayList<>();
            for (Entity unit : units) {
                if (force.containsEntity(unit.getId())) {
                    lanceUnitIds.add(unit.getId());
                }
            }
            if (!lanceUnitIds.isEmpty() && (lanceUnitIds.size() < units.size())) {
                groups.add(new OrderGroup(force.getName() + " (" + lanceUnitIds.size() + ")", lanceUnitIds));
            }
        }
        for (Entity unit : units) {
            groups.add(new OrderGroup(Messages.getString("BotCommandPanel.Orders.unit", unit.getId(),
                  unit.getDisplayName()), List.of(unit.getId())));
        }
        return groups;
    }

    /**
     * @param botPlayer the bot
     *
     * @return each of the bot's units as a one-unit group, keyed by the name of its lance in the force tree; units in
     *       no lance come last
     */
    public Map<String, List<OrderGroup>> unitsByLance(Player botPlayer) {
        Map<String, List<OrderGroup>> unitsByLance = new LinkedHashMap<>();
        if (!(client.getGame() instanceof Game game)) {
            return unitsByLance;
        }
        List<OrderGroup> withoutLance = new ArrayList<>();
        for (Entity unit : game.getPlayerEntities(botPlayer, false)) {
            if (!canTakeOrders(unit)) {
                continue;
            }
            OrderGroup unitGroup = new OrderGroup(Messages.getString("BotCommandPanel.Orders.unit", unit.getId(),
                  unit.getDisplayName()), List.of(unit.getId()));
            Force lance = game.getForces().getForce(unit);
            if (lance == null) {
                withoutLance.add(unitGroup);
            } else {
                unitsByLance.computeIfAbsent(lance.getName(), name -> new ArrayList<>()).add(unitGroup);
            }
        }
        if (!withoutLance.isEmpty()) {
            unitsByLance.put(Messages.getString("BotCommandPanel.Orders.noLance"), withoutLance);
        }
        return unitsByLance;
    }

    /**
     * @return {@code true} for a unit the orders apply to: on the board and not airborne, since airborne units get
     *       orders in a later version
     */
    private static boolean canTakeOrders(Entity entity) {
        return entity.isDeployed() && !entity.isDestroyed() && !entity.isDoomed() && (entity.getPosition() != null)
              && !entity.isOffBoard() && !entity.isAirborne();
    }

    private static List<Integer> idsOf(List<Entity> units) {
        List<Integer> unitIds = new ArrayList<>(units.size());
        for (Entity unit : units) {
            unitIds.add(unit.getId());
        }
        return unitIds;
    }

    private void addOrders(JMenu groupMenu, Player botPlayer, OrderGroup group, @Nullable Coords clickedHex) {
        if (clickedHex != null) {
            String hexes = "hexes=" + clickedHex.getBoardNum();
            addOrder(groupMenu, botPlayer, group, "moveHere", UnitOrderAction.ROUTE, hexes);
            addOrder(groupMenu, botPlayer, group, "addWaypointHere", UnitOrderAction.ADD, hexes);
        } else {
            addPickedHexOrder(groupMenu, botPlayer, group, "moveTo", UnitOrderAction.ROUTE);
            addPickedHexOrder(groupMenu, botPlayer, group, "addWaypoint", UnitOrderAction.ADD);
        }
        groupMenu.add(createPriorityMenu(botPlayer, group));
        groupMenu.add(createFacingMenu(botPlayer, group));
        groupMenu.add(createEdgeMenu(botPlayer, group));
        groupMenu.addSeparator();
        addOrder(groupMenu, botPlayer, group, "pause", UnitOrderAction.PAUSE);
        addOrder(groupMenu, botPlayer, group, "resume", UnitOrderAction.RESUME);
        addOrder(groupMenu, botPlayer, group, "stop", UnitOrderAction.STOP);
        addOrder(groupMenu, botPlayer, group, "removeLast", UnitOrderAction.REMOVE_LAST);
        addOrder(groupMenu, botPlayer, group, "clear", UnitOrderAction.CLEAR);
    }

    private void addOrder(JMenu menu, Player botPlayer, OrderGroup group, String key, UnitOrderAction action,
          String... namedArguments) {
        JMenuItem item = new JMenuItem(Messages.getString("BotCommandPanel.Orders." + key));
        item.addActionListener(event -> sendToGroup(botPlayer, group, Messages.getString("BotCommandPanel.Orders." + key),
              action, namedArguments));
        menu.add(item);
    }

    private void addPickedHexOrder(JMenu menu, Player botPlayer, OrderGroup group, String key,
          UnitOrderAction action) {
        String title = Messages.getString("BotCommandPanel.Orders." + key);
        JMenuItem item = new JMenuItem(title);
        item.addActionListener(event -> hexPicker.pickHexes(title + " - " + group.label(), false, hexes -> {
            sendToGroup(botPlayer, group, title + " " + hexes, action, "hexes=" + hexes);
            if ((action == UnitOrderAction.ROUTE) && (facingChooser != null)) {
                // a new route is the moment to say which way to face; cancelling keeps the facings as they were
                chooseFacings(botPlayer, group);
            }
        }));
        menu.add(item);
    }

    private JMenu createPriorityMenu(Player botPlayer, OrderGroup group) {
        JMenu menu = new JMenu(Messages.getString("BotCommandPanel.Orders.priority"));
        for (OrderPriority priority : OrderPriority.values()) {
            String title = Messages.getString("BotCommandPanel.Orders.priority." + priority.name());
            JMenuItem item = new JMenuItem(title);
            item.setToolTipText(Messages.getString("BotCommandPanel.Orders.priority." + priority.name() + ".tooltip"));
            item.addActionListener(event -> sendToGroup(botPlayer, group, title, UnitOrderAction.PRIORITY,
                  "priority=" + priority.name()));
            menu.add(item);
        }
        return menu;
    }

    private JMenuItem createFacingMenu(Player botPlayer, OrderGroup group) {
        if (facingChooser != null) {
            JMenuItem item = new JMenuItem(Messages.getString("BotCommandPanel.Orders.facing.dialog"));
            item.addActionListener(event -> chooseFacings(botPlayer, group));
            return item;
        }
        JMenu menu = new JMenu(Messages.getString("BotCommandPanel.Orders.facing"));
        menu.add(createFacingChoiceMenu(botPlayer, group, true));
        menu.add(createFacingChoiceMenu(botPlayer, group, false));
        return menu;
    }

    private JMenu createFacingChoiceMenu(Player botPlayer, OrderGroup group, boolean whileMoving) {
        String key = whileMoving ? "BotCommandPanel.Orders.facing.moving" : "BotCommandPanel.Orders.facing.stopped";
        JMenu menu = new JMenu(Messages.getString(key));
        for (int facing = UnitOrders.FACING_AUTO; facing < FACING_COUNT; facing++) {
            String title = Messages.getString("BotCommandPanel.Orders.facing." + facingKey(facing));
            JMenuItem item = new JMenuItem(title);
            final int chosenFacing = facing;
            item.addActionListener(event -> sendFacing(botPlayer, group, whileMoving, chosenFacing,
                  Messages.getString(key) + ": " + title));
            menu.add(item);
        }
        return menu;
    }

    private static String facingKey(int facing) {
        return (facing == UnitOrders.FACING_AUTO) ? "auto" : String.valueOf(facing);
    }

    /**
     * Sets one of the two facings for every unit of the group, keeping each unit's other facing as it was.
     */
    private void sendFacing(Player botPlayer, OrderGroup group, boolean whileMoving, int facing, String description) {
        for (int unitId : group.unitIds()) {
            Entity unit = (client.getGame() instanceof Game game) ? game.getEntity(unitId) : null;
            UnitOrders current = (unit == null) ? UnitOrders.NONE : unit.getUnitOrders();
            int moving = whileMoving ? facing : current.getFacingWhileMoving();
            int stopped = whileMoving ? current.getFacingWhenStopped() : facing;
            client.sendChat(UnitOrderCommand.commandText(unitId, UnitOrderAction.FACING,
                  UnitOrderCommand.FACING_WHILE_MOVING + '=' + moving,
                  UnitOrderCommand.FACING_WHEN_STOPPED + '=' + stopped));
        }
        acknowledge(botPlayer, group, description);
    }

    /**
     * Opens the facing dialog for the group, starting from its first unit's facings, and sends both facings.
     */
    private void chooseFacings(Player botPlayer, OrderGroup group) {
        Entity firstUnit = (client.getGame() instanceof Game game) ? game.getEntity(group.unitIds().get(0)) : null;
        UnitOrders current = (firstUnit == null) ? UnitOrders.NONE : firstUnit.getUnitOrders();
        facingChooser.chooseFacings(group, current.getFacingWhileMoving(), current.getFacingWhenStopped(),
              (moving, stopped) -> {
                  for (int unitId : group.unitIds()) {
                      client.sendChat(UnitOrderCommand.commandText(unitId, UnitOrderAction.FACING,
                            UnitOrderCommand.FACING_WHILE_MOVING + '=' + moving,
                            UnitOrderCommand.FACING_WHEN_STOPPED + '=' + stopped));
                  }
                  acknowledge(botPlayer, group, Messages.getString("BotCommandPanel.Orders.facing"));
              });
    }

    private JMenu createEdgeMenu(Player botPlayer, OrderGroup group) {
        JMenu menu = new JMenu(Messages.getString("BotCommandPanel.Orders.edge"));
        menu.add(createEdgeChoiceMenu(botPlayer, group, "moveToEdge", UnitOrderAction.MOVE_TO_EDGE));
        menu.add(createEdgeChoiceMenu(botPlayer, group, "exitByEdge", UnitOrderAction.EXIT_BY_EDGE));
        return menu;
    }

    private JMenu createEdgeChoiceMenu(Player botPlayer, OrderGroup group, String key, UnitOrderAction action) {
        String menuTitle = Messages.getString("BotCommandPanel.Orders." + key);
        JMenu menu = new JMenu(menuTitle);
        for (OffBoardDirection edge : List.of(OffBoardDirection.NORTH, OffBoardDirection.EAST, OffBoardDirection.SOUTH,
              OffBoardDirection.WEST)) {
            String title = Messages.getString("BotCommandPanel.Orders.edge." + edge.name());
            JMenuItem item = new JMenuItem(title);
            item.addActionListener(event -> sendToGroup(botPlayer, group, menuTitle + " " + title, action,
                  UnitOrderCommand.EDGE + '=' + edge.name()));
            menu.add(item);
        }
        return menu;
    }

    private void sendToGroup(Player botPlayer, OrderGroup group, String description, UnitOrderAction action,
          String... namedArguments) {
        for (int unitId : group.unitIds()) {
            client.sendChat(UnitOrderCommand.commandText(unitId, action, namedArguments));
        }
        acknowledge(botPlayer, group, description);
    }

    private void acknowledge(Player botPlayer, OrderGroup group, String description) {
        LOGGER.info("[BotOrders] {} sent to {} of {} ({} units)", description, group.label(), botPlayer.getName(),
              group.unitIds().size());
        acknowledger.accept(botPlayer, Messages.getString("BotCommandPanel.Orders.toast", description, group.label()));
    }
}
