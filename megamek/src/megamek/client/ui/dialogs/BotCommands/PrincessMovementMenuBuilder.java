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
import java.util.function.Consumer;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

import jakarta.annotation.Nullable;
import megamek.client.AbstractClient;
import megamek.client.bot.princess.CardinalEdge;
import megamek.client.bot.princess.ChatCommands;
import megamek.client.ui.Messages;
import megamek.client.ui.util.MenuScroller;
import megamek.common.Player;
import megamek.common.game.InGameObject;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;

/**
 * Builds the Bot Commands panel's Waypoints and Movement menus for one Princess bot. Each entry sends one of
 * Princess's chat commands, so the player does not have to type them: a route for one unit ({@code sw}, {@code aw},
 * {@code rw}, {@code cw}, {@code nw}), and hold position or a retreat to a board edge for the whole bot ({@code hp},
 * {@code fl}).
 *
 * <p>These menus are for Princess bots only - see
 * {@link megamek.common.orders.OrderEligibility#takesChatWaypoints}. A CASPAR bot's units are ordered with Move Order
 * and Quick Orders, built by {@link BotOrdersMenuBuilder}; the right-click menu on a CASPAR route's waypoint flag is
 * {@link BotWaypointMenuBuilder}.</p>
 */
public class PrincessMovementMenuBuilder {

    private static final MMLogger LOGGER = MMLogger.create(PrincessMovementMenuBuilder.class);

    /** How many units a menu shows before it scrolls, so a bot with a company or more still fits on screen. */
    private static final int UNIT_SCROLL_THRESHOLD = 15;

    private static final String KEY_PREFIX = "BotCommandPanel.";

    /** The edges of the Movement menu, in menu order; {@link CardinalEdge#NONE} cancels a retreat. */
    private static final List<CardinalEdge> RETREAT_EDGES = List.of(CardinalEdge.NORTH, CardinalEdge.EAST,
          CardinalEdge.SOUTH, CardinalEdge.WEST, CardinalEdge.NEAREST, CardinalEdge.NONE);

    /**
     * Sends a chat command to a bot.
     */
    @FunctionalInterface
    public interface ChatCommandSender {
        /**
         * @param botPlayer   the bot the command is for
         * @param chatCommand the command
         * @param arguments   the command's arguments, or {@code null} for a command sent without any
         */
        void send(Player botPlayer, ChatCommands chatCommand, @Nullable String arguments);
    }

    private final AbstractClient client;
    private final ChatCommandSender chatCommandSender;
    private final BotOrdersMenuBuilder.HexPicker hexPicker;
    private final BiConsumer<Player, String> acknowledger;

    /**
     * @param client            the client whose game holds the bots' units
     * @param chatCommandSender sends a chat command to a bot
     * @param hexPicker         how the player picks the hexes of a route
     * @param acknowledger      shows the player that an order was sent: the bot and a description of the order
     */
    public PrincessMovementMenuBuilder(AbstractClient client, ChatCommandSender chatCommandSender,
          BotOrdersMenuBuilder.HexPicker hexPicker, BiConsumer<Player, String> acknowledger) {
        this.client = client;
        this.chatCommandSender = chatCommandSender;
        this.hexPicker = hexPicker;
        this.acknowledger = acknowledger;
    }

    /**
     * Fills a bot's Waypoints menu: set or extend one unit's route, remove its last waypoint, clear its route, or
     * clear every unit's route.
     *
     * @param botMenu   the menu to fill
     * @param botPlayer the Princess bot
     */
    public void populateWaypoints(JMenu botMenu, Player botPlayer) {
        List<Entity> units = onBoardUnitsOf(botPlayer);
        if (units.isEmpty()) {
            LOGGER.info("[PrincessWaypoints] {} has no on-board units: only Clear All Waypoints is offered",
                  botPlayer.getName());
        } else {
            botMenu.add(createHexOrderMenu(botPlayer, units, "SetWaypoints", ChatCommands.SET_WAYPOINT));
            botMenu.add(createHexOrderMenu(botPlayer, units, "AddWaypoint", ChatCommands.ADD_WAYPOINT));
            botMenu.addSeparator();
            botMenu.add(createUnitOrderMenu(botPlayer, units, "RemoveWaypoint", ChatCommands.REMOVE_WAYPOINT));
            botMenu.add(createUnitOrderMenu(botPlayer, units, "ClearWaypoints", ChatCommands.CLEAR_WAYPOINT));
        }
        addBotWideOrder(botMenu, botPlayer, "ClearAllWaypoints", ChatCommands.CLEAR_ALL_WAYPOINTS, null);
    }

    /**
     * Fills a bot's Movement menu: retreat to a board edge or cancel the retreat, hold position, and resume movement.
     * All of these apply to the whole bot, not to one unit.
     *
     * @param botMenu   the menu to fill
     * @param botPlayer the Princess bot
     */
    public void populateMovement(JMenu botMenu, Player botPlayer) {
        for (CardinalEdge retreatEdge : RETREAT_EDGES) {
            addBotWideOrder(botMenu, botPlayer, "Retreat." + retreatEdge.name(), ChatCommands.FLEE,
                  String.valueOf(retreatEdge.getIndex()));
        }
        botMenu.addSeparator();
        addBotWideOrder(botMenu, botPlayer, "HoldPosition", ChatCommands.HOLD_POSITION, null);
        addBotWideOrder(botMenu, botPlayer, "ResumeMovement", ChatCommands.HOLD_POSITION, String.valueOf(false));
    }

    /**
     * @param botPlayer the bot
     *
     * @return the bot's units that are on a board, which are the ones a route or a hold makes sense for
     */
    private List<Entity> onBoardUnitsOf(Player botPlayer) {
        List<Entity> units = new ArrayList<>();
        for (InGameObject unit : client.getInGameObjects()) {
            if ((unit instanceof Entity entity) && (entity.getOwnerId() == botPlayer.getId())
                  && !entity.isOffBoard()) {
                units.add(entity);
            }
        }
        return units;
    }

    /**
     * Adds an order that applies to the whole bot. The item's title and tooltip come from the order's resource keys.
     *
     * @param botMenu     the menu to add the item to
     * @param botPlayer   the bot the order is for
     * @param orderKey    the resource key of the order, without the panel prefix or the title and tooltip suffixes
     * @param chatCommand the chat command to send
     * @param arguments   the command's arguments, or {@code null} for none
     */
    private void addBotWideOrder(JMenu botMenu, Player botPlayer, String orderKey, ChatCommands chatCommand,
          @Nullable String arguments) {
        String title = Messages.getString(KEY_PREFIX + orderKey + ".title");
        JMenuItem orderItem = new JMenuItem(title);
        orderItem.setToolTipText(Messages.getString(KEY_PREFIX + orderKey + ".tooltip"));
        orderItem.addActionListener(event -> send(botPlayer, chatCommand, arguments, title));
        botMenu.add(orderItem);
    }

    /**
     * Creates a menu of the bot's units for an order that needs hexes: the player picks a unit, then the hexes.
     *
     * @param botPlayer   the bot the order is for
     * @param units       the bot's on-board units
     * @param orderKey    the resource key of the order, without the panel prefix or the title and tooltip suffixes
     * @param chatCommand the chat command to send: set waypoints or add waypoint
     *
     * @return the menu
     */
    private JMenu createHexOrderMenu(Player botPlayer, List<Entity> units, String orderKey,
          ChatCommands chatCommand) {
        String title = Messages.getString(KEY_PREFIX + orderKey + ".title");
        return createUnitMenu(units, orderKey, unit -> {
            String pickerDescription = Messages.getString("BotCommandPanel.Orders.toast", title, unit.generalName());
            hexPicker.pickHexes(pickerDescription, false,
                  hexes -> send(botPlayer, chatCommand, unit.getId() + " " + hexes, describe(title, unit)));
        });
    }

    /**
     * Creates a menu of the bot's units for an order that needs no hexes: the order is sent as soon as a unit is
     * picked.
     *
     * @param botPlayer   the bot the order is for
     * @param units       the bot's on-board units
     * @param orderKey    the resource key of the order, without the panel prefix or the title and tooltip suffixes
     * @param chatCommand the chat command to send: remove the last waypoint or clear the unit's waypoints
     *
     * @return the menu
     */
    private JMenu createUnitOrderMenu(Player botPlayer, List<Entity> units, String orderKey,
          ChatCommands chatCommand) {
        String title = Messages.getString(KEY_PREFIX + orderKey + ".title");
        return createUnitMenu(units, orderKey,
              unit -> send(botPlayer, chatCommand, String.valueOf(unit.getId()), describe(title, unit)));
    }

    /**
     * Creates a menu listing units; picking one runs the given action with it.
     *
     * @param units      the units to list
     * @param orderKey   the resource key of the order, without the panel prefix or the title and tooltip suffixes
     * @param unitAction what to do with the picked unit
     *
     * @return the menu
     */
    private JMenu createUnitMenu(List<Entity> units, String orderKey, Consumer<Entity> unitAction) {
        JMenu unitMenu = new JMenu(Messages.getString(KEY_PREFIX + orderKey + ".title"));
        unitMenu.setToolTipText(Messages.getString(KEY_PREFIX + orderKey + ".tooltip"));
        for (Entity unit : units) {
            JMenuItem unitItem = new JMenuItem(Messages.getString("BotCommandPanel.Orders.unit",
                  String.valueOf(unit.getId()), unit.generalName()));
            unitItem.addActionListener(event -> unitAction.accept(unit));
            unitMenu.add(unitItem);
        }
        if (unitMenu.getItemCount() > UNIT_SCROLL_THRESHOLD) {
            MenuScroller.setScrollerFor(unitMenu, UNIT_SCROLL_THRESHOLD);
        }
        return unitMenu;
    }

    /**
     * @param orderTitle the order's name as the menu shows it
     * @param unit       the unit the order is for
     *
     * @return the order and its unit, for the toast that confirms the order was sent
     */
    private String describe(String orderTitle, Entity unit) {
        return Messages.getString("BotCommandPanel.toast.unitOrder", orderTitle, String.valueOf(unit.getId()),
              unit.generalName());
    }

    /**
     * Sends a chat command to a bot, logs it and confirms it to the player.
     *
     * @param botPlayer   the bot the command is for
     * @param chatCommand the chat command
     * @param arguments   the command's arguments, or {@code null} for none
     * @param description what was ordered, for the confirming toast
     */
    private void send(Player botPlayer, ChatCommands chatCommand, @Nullable String arguments, String description) {
        LOGGER.info("[PrincessWaypoints] sending {} to {}: command '{}', arguments '{}'", description,
              botPlayer.getName(), chatCommand.getAbbreviation(), (arguments == null) ? "" : arguments);
        chatCommandSender.send(botPlayer, chatCommand, arguments);
        acknowledger.accept(botPlayer, description);
    }
}
