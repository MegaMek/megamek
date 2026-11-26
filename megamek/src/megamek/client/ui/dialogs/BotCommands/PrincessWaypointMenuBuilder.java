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
import java.util.List;
import java.util.function.BiConsumer;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

import jakarta.annotation.Nullable;
import megamek.client.AbstractClient;
import megamek.client.bot.princess.CardinalEdge;
import megamek.client.bot.princess.ChatCommands;
import megamek.client.ui.Messages;
import megamek.client.ui.util.MenuScroller;
import megamek.common.Player;
import megamek.common.game.Game;
import megamek.common.orders.OrderEligibility;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;

/**
 * Builds the Move Order and Quick Orders menus for a Princess bot.
 *
 * <p>Only CASPAR bots take unit orders (see {@link OrderEligibility}). Princess keeps her own waypoints, which she
 * takes by chat command. These menus send those chat commands, so a Princess bot's buttons offer what she can follow
 * instead of doing nothing: set, add, remove and clear a unit's waypoints, clear them all, hold position or resume,
 * and retreat to a board edge.</p>
 *
 * <p>Princess answers each command in the chat, so the player sees the result even in the Commander window, which
 * has no toasts.</p>
 */
public class PrincessWaypointMenuBuilder {

    private static final MMLogger LOGGER = MMLogger.create(PrincessWaypointMenuBuilder.class);

    /** How many units a menu shows before it scrolls. */
    private static final int UNIT_SCROLL_THRESHOLD = 20;

    /** The edges of the Retreat menu, in menu order. {@link CardinalEdge#NONE} is its own item: Cancel retreat. */
    private static final List<CardinalEdge> RETREAT_EDGES = List.of(CardinalEdge.NORTH, CardinalEdge.EAST,
          CardinalEdge.SOUTH, CardinalEdge.WEST, CardinalEdge.NEAREST);

    private final AbstractClient client;
    private final BotOrdersMenuBuilder.HexPicker hexPicker;
    private final BiConsumer<Player, String> acknowledger;

    /**
     * @param client       the client that sends the chat commands
     * @param hexPicker    how the player picks the waypoint hexes
     * @param acknowledger shows the player that a command was sent: the bot and a description of the command
     */
    public PrincessWaypointMenuBuilder(AbstractClient client, BotOrdersMenuBuilder.HexPicker hexPicker,
          BiConsumer<Player, String> acknowledger) {
        this.client = client;
        this.hexPicker = hexPicker;
        this.acknowledger = acknowledger;
    }

    /**
     * Adds one submenu per unit of the bot, each with Set waypoints, Add waypoint, Remove last waypoint and Clear
     * waypoints, then a Clear all waypoints item for the whole bot.
     *
     * @param botMenu   the menu to fill
     * @param botPlayer the Princess bot
     */
    public void populateMoveOrders(JMenu botMenu, Player botPlayer) {
        List<Entity> units = unitsFor(botPlayer);
        if (units.isEmpty()) {
            return;
        }
        for (Entity unit : units) {
            JMenu unitMenu = new JMenu(Messages.getString("BotCommandPanel.Orders.unit", unit.getId(),
                  unit.getDisplayName()));
            addWaypointPickItem(unitMenu, botPlayer, unit, "setWaypoints", ChatCommands.SET_WAYPOINT);
            addWaypointPickItem(unitMenu, botPlayer, unit, "addWaypoint", ChatCommands.ADD_WAYPOINT);
            addCommandItem(unitMenu, botPlayer, "removeLastWaypoint", ChatCommands.REMOVE_WAYPOINT,
                  String.valueOf(unit.getId()));
            addCommandItem(unitMenu, botPlayer, "clearWaypoints", ChatCommands.CLEAR_WAYPOINT,
                  String.valueOf(unit.getId()));
            botMenu.add(unitMenu);
        }
        botMenu.addSeparator();
        addCommandItem(botMenu, botPlayer, "clearAllWaypoints", ChatCommands.CLEAR_ALL_WAYPOINTS, null);
        if (botMenu.getItemCount() > UNIT_SCROLL_THRESHOLD) {
            MenuScroller.setScrollerFor(botMenu, UNIT_SCROLL_THRESHOLD);
        }
    }

    /**
     * Adds Hold position and Resume moving, then a Retreat menu with the board edges and Cancel retreat. Princess
     * holds, moves and retreats as a whole bot, so these are not per unit.
     *
     * @param botMenu   the menu to fill
     * @param botPlayer the Princess bot
     */
    public void populateQuickOrders(JMenu botMenu, Player botPlayer) {
        if (unitsFor(botPlayer).isEmpty()) {
            return;
        }
        addCommandItem(botMenu, botPlayer, "holdPosition", ChatCommands.HOLD_POSITION, "true");
        addCommandItem(botMenu, botPlayer, "resume", ChatCommands.HOLD_POSITION, "false");
        botMenu.addSeparator();
        botMenu.add(createRetreatMenu(botPlayer));
        addCommandItem(botMenu, botPlayer, "retreat.NONE", ChatCommands.FLEE, CardinalEdge.NONE.name());
    }

    /**
     * Creates the Retreat menu: one item per board edge, and the nearest edge. Each sends Princess's flee command, on
     * which her units head for that edge and leave the board.
     *
     * @param botPlayer the Princess bot
     *
     * @return the menu
     */
    private JMenu createRetreatMenu(Player botPlayer) {
        JMenu retreatMenu = new JMenu(Messages.getString("BotCommandPanel.PrincessOrders.retreat"));
        retreatMenu.setToolTipText(Messages.getString("BotCommandPanel.PrincessOrders.retreat.tooltip"));
        for (CardinalEdge retreatEdge : RETREAT_EDGES) {
            addCommandItem(retreatMenu, botPlayer, "retreat." + retreatEdge.name(), ChatCommands.FLEE,
                  retreatEdge.name());
        }
        return retreatMenu;
    }

    /**
     * Lists the bot's units that can be given waypoints: alive, not off-board, of a kind that follows a route, and
     * either on the board or still to deploy.
     *
     * @param botPlayer the bot
     *
     * @return the units, in game order; empty when it has none
     */
    List<Entity> unitsFor(Player botPlayer) {
        List<Entity> units = new ArrayList<>();
        if (!(client.getGame() instanceof Game game)) {
            return units;
        }
        for (Entity entity : game.getPlayerEntities(botPlayer, false)) {
            if (canTakeWaypoints(entity)) {
                units.add(entity);
            }
        }
        return units;
    }

    /**
     * Builds the chat line that sends a command to a bot, in the form Princess's chat parser reads, e.g.
     * {@code Lyran Allies: sw : 20 1508-1504}.
     *
     * @param botPlayer   the bot the command is for
     * @param chatCommand the command
     * @param arguments   the command's arguments separated by spaces, or {@code null} for none
     *
     * @return the chat line
     */
    static String commandText(Player botPlayer, ChatCommands chatCommand, @Nullable String arguments) {
        String text = botPlayer.getName() + ": " + chatCommand.getAbbreviation();
        if (arguments != null) {
            text += " : " + arguments;
        }
        return text;
    }

    private static boolean canTakeWaypoints(Entity entity) {
        if (entity.isDestroyed() || entity.isDoomed() || entity.isOffBoard()
              || !OrderEligibility.isOrderableKind(entity)) {
            return false;
        }
        boolean isStillToDeploy = !entity.isDeployed() && (entity.getTransportId() == Entity.NONE);
        return isStillToDeploy || (entity.getPosition() != null);
    }

    /**
     * Adds an item that asks for hexes, then sends the command with the unit and the picked hexes.
     */
    private void addWaypointPickItem(JMenu menu, Player botPlayer, Entity unit, String key, ChatCommands chatCommand) {
        JMenuItem item = createItem(key);
        item.addActionListener(event -> hexPicker.pickHexes(item.getText(), false,
              hexes -> send(botPlayer, key, chatCommand, unit.getId() + " " + hexes)));
        menu.add(item);
    }

    /**
     * Adds an item that sends the command at once.
     */
    private void addCommandItem(JMenu menu, Player botPlayer, String key, ChatCommands chatCommand,
          @Nullable String arguments) {
        JMenuItem item = createItem(key);
        item.addActionListener(event -> send(botPlayer, key, chatCommand, arguments));
        menu.add(item);
    }

    private static JMenuItem createItem(String key) {
        JMenuItem item = new JMenuItem(Messages.getString("BotCommandPanel.PrincessOrders." + key));
        item.setToolTipText(Messages.getString("BotCommandPanel.PrincessOrders." + key + ".tooltip"));
        return item;
    }

    private void send(Player botPlayer, String key, ChatCommands chatCommand, @Nullable String arguments) {
        String text = commandText(botPlayer, chatCommand, arguments);
        LOGGER.info("[BotOrders] Princess order to {}: {}", botPlayer.getName(), text);
        client.sendChat(text);
        acknowledger.accept(botPlayer, Messages.getString("BotCommandPanel.PrincessOrders." + key));
    }
}
