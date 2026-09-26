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
import java.util.Comparator;
import java.util.List;
import java.util.function.BiConsumer;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;

import megamek.client.AbstractClient;
import megamek.client.ui.Messages;
import megamek.client.ui.dialogs.BotCommands.BotOrdersMenuBuilder.OrderGroup;
import megamek.common.Player;
import megamek.common.annotations.Nullable;
import megamek.common.board.Coords;
import megamek.common.game.Game;
import megamek.common.orders.RouteGroups;
import megamek.common.orders.RouteGroups.RouteGroup;
import megamek.common.orders.UnitOrderAction;
import megamek.common.orders.WaypointOrder;
import megamek.common.units.Entity;
import megamek.server.commands.UnitOrderCommand;

/**
 * Builds the menu a player gets by right-clicking a waypoint flag on the map: the waypoint's facing on arrival and its
 * hold, removing it, and opening the whole route in the Move Order editor. An edit changes the route in place, so a
 * pause and a hold under way carry on.
 */
public final class BotWaypointMenuBuilder {

    /** The longest hold offered from the menu; the Move Order editor offers more. */
    private static final int MENU_HOLD_CHOICES = 5;

    /**
     * Opens the Move Order editor for the units following a route.
     */
    @FunctionalInterface
    public interface RouteEditorOpener {
        /**
         * @param botPlayer the bot
         * @param group     the units following the route
         */
        void open(Player botPlayer, OrderGroup group);
    }

    private BotWaypointMenuBuilder() {
    }

    /**
     * @param client       the client that sends the orders
     * @param hex          the hex the player right-clicked
     * @param boardId      the board it is on
     * @param opener       opens the Move Order editor, or {@code null} where there is none
     * @param acknowledger shows the player that an order was sent: the bot and a description of the order
     *
     * @return one menu for each route on the player's side with a waypoint on the hex; empty when there is none
     */
    public static List<JMenu> menusFor(AbstractClient client, Coords hex, int boardId,
          @Nullable RouteEditorOpener opener, BiConsumer<Player, String> acknowledger) {
        List<JMenu> menus = new ArrayList<>();
        if (!(client.getGame() instanceof Game game)) {
            return menus;
        }
        List<Entity> units = new ArrayList<>(game.getEntitiesVector());
        units.sort(Comparator.comparingInt(Entity::getId));
        for (RouteGroup group : RouteGroups.visibleTo(units, client.getLocalPlayer())) {
            int waypointIndex = group.guide().getUnitOrders().getRoute().indexOf(hex);
            if ((group.guide().getBoardId() == boardId) && (waypointIndex >= 0)) {
                menus.add(waypointMenu(client, group, waypointIndex, opener, acknowledger));
            }
        }
        return menus;
    }

    private static JMenu waypointMenu(AbstractClient client, RouteGroup group, int waypointIndex,
          @Nullable RouteEditorOpener opener, BiConsumer<Player, String> acknowledger) {
        List<Coords> route = group.guide().getUnitOrders().getRoute();
        List<WaypointOrder> waypointOrders = group.guide().getUnitOrders().getWaypointOrders();
        WaypointOrder current = waypointOrders.get(waypointIndex);
        boolean isEndOfRoute = waypointIndex == route.size() - 1;
        Player botPlayer = group.guide().getOwner();
        String waypointName = Messages.getString("BotCommandPanel.Waypoint.menu", waypointIndex + 1, group.label());
        JMenu menu = new JMenu(waypointName);

        JMenu facingMenu = new JMenu(Messages.getString("BotCommandPanel.MoveOrder.column.facing"));
        for (WaypointTableModel.FacingOption option : WaypointTableModel.facingOptions()) {
            JRadioButtonMenuItem item = new JRadioButtonMenuItem(option.toString(),
                  option.facing() == current.getFacing());
            WaypointOrder edited = new WaypointOrder(option.facing(), current.getHoldTurns(),
                  current.getFormation());
            item.addActionListener(event -> sendEdit(client, group, route,
                  replaced(waypointOrders, waypointIndex, edited), acknowledger, waypointName + ": " + option));
            facingMenu.add(item);
        }
        menu.add(facingMenu);

        JMenu holdMenu = new JMenu(Messages.getString("BotCommandPanel.MoveOrder.column.hold"));
        if (isEndOfRoute) {
            holdMenu.setEnabled(false);
            holdMenu.setToolTipText(Messages.getString("BotCommandPanel.MoveOrder.lastWaypointNote"));
        }
        for (int turns = 0; turns <= MENU_HOLD_CHOICES; turns++) {
            String title = (turns == 0) ? Messages.getString("BotCommandPanel.MoveOrder.passThrough")
                  : Messages.getString("BotCommandPanel.MoveOrder.holdTurns", turns);
            JRadioButtonMenuItem item = new JRadioButtonMenuItem(title, turns == current.getHoldTurns());
            final int chosenTurns = turns;
            WaypointOrder edited = new WaypointOrder(current.getFacing(), chosenTurns, current.getFormation());
            item.addActionListener(event -> sendEdit(client, group, route,
                  replaced(waypointOrders, waypointIndex, edited), acknowledger, waypointName + ": " + title));
            holdMenu.add(item);
        }
        menu.add(holdMenu);

        menu.addSeparator();
        JMenuItem removeItem = new JMenuItem(Messages.getString("BotCommandPanel.Waypoint.remove"));
        removeItem.addActionListener(event -> removeWaypoint(client, group, route, waypointOrders, waypointIndex,
              acknowledger, waypointName));
        menu.add(removeItem);
        if (opener != null) {
            JMenuItem editItem = new JMenuItem(Messages.getString("BotCommandPanel.Waypoint.edit"));
            editItem.addActionListener(event -> opener.open(botPlayer, new OrderGroup(group.label(),
                  group.unitIds())));
            menu.add(editItem);
        }
        return menu;
    }

    private static List<WaypointOrder> replaced(List<WaypointOrder> waypointOrders, int index,
          WaypointOrder replacement) {
        List<WaypointOrder> edited = new ArrayList<>(waypointOrders);
        edited.set(index, replacement);
        return edited;
    }

    private static void removeWaypoint(AbstractClient client, RouteGroup group, List<Coords> route,
          List<WaypointOrder> waypointOrders, int index, BiConsumer<Player, String> acknowledger,
          String waypointName) {
        String description = waypointName + ": " + Messages.getString("BotCommandPanel.Waypoint.remove");
        if (route.size() == 1) {
            // the route's only waypoint: taking it off leaves every other order as it was
            for (int unitId : group.unitIds()) {
                client.sendChat(UnitOrderCommand.commandText(unitId, UnitOrderAction.REMOVE_LAST));
            }
            acknowledger.accept(group.guide().getOwner(), description);
            return;
        }
        List<Coords> editedRoute = new ArrayList<>(route);
        editedRoute.remove(index);
        List<WaypointOrder> editedOrders = new ArrayList<>(waypointOrders);
        editedOrders.remove(index);
        sendEdit(client, group, editedRoute, editedOrders, acknowledger, description);
    }

    private static void sendEdit(AbstractClient client, RouteGroup group, List<Coords> route,
          List<WaypointOrder> waypointOrders, BiConsumer<Player, String> acknowledger, String description) {
        for (int unitId : group.unitIds()) {
            client.sendChat(UnitOrderCommand.commandText(unitId, UnitOrderAction.EDIT_ROUTE,
                  UnitOrderCommand.hexesArgument(route, waypointOrders)));
        }
        acknowledger.accept(group.guide().getOwner(), description);
    }
}
