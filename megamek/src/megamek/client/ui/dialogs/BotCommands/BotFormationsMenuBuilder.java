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

import java.util.function.BiConsumer;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

import megamek.client.AbstractClient;
import megamek.client.ui.Messages;
import megamek.client.ui.util.MenuScroller;
import megamek.common.Player;
import megamek.common.annotations.Nullable;
import megamek.common.game.Game;
import megamek.common.orders.ContactRule;
import megamek.common.orders.FormationOrder;
import megamek.common.orders.FormationPace;
import megamek.common.orders.FormationShape;
import megamek.common.orders.UnitOrderAction;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;
import megamek.server.commands.UnitOrderCommand;

/**
 * Builds the Formations menu for one bot: for each lance, or all units, the formation's shape, spacing, leader, pace
 * and what it does on contact, and a way to end the formation. The group's units then travel in that shape whenever
 * they are given a route.
 *
 * <p>Every member carries the formation in its own orders, with its slot: the leader is slot 0 and the others follow
 * in the lance's order. Changing one setting sends the whole formation again with that setting changed.</p>
 */
public class BotFormationsMenuBuilder {

    private static final MMLogger LOGGER = MMLogger.create(BotFormationsMenuBuilder.class);

    private static final int GROUP_SCROLL_THRESHOLD = 20;
    private static final FormationShape DEFAULT_SHAPE = FormationShape.WEDGE;

    private final AbstractClient client;
    private final BotOrdersMenuBuilder ordersMenuBuilder;
    private final BiConsumer<Player, String> acknowledger;

    /**
     * @param client            the client that sends the orders
     * @param ordersMenuBuilder supplies the groups of each bot's units
     * @param acknowledger      shows the player that an order was sent
     */
    public BotFormationsMenuBuilder(AbstractClient client, BotOrdersMenuBuilder ordersMenuBuilder,
          BiConsumer<Player, String> acknowledger) {
        this.client = client;
        this.ordersMenuBuilder = ordersMenuBuilder;
        this.acknowledger = acknowledger;
    }

    /**
     * Adds one submenu per group of two or more of the bot's units.
     *
     * @param botMenu   the menu to fill
     * @param botPlayer the bot
     */
    public void populate(JMenu botMenu, Player botPlayer) {
        for (BotOrdersMenuBuilder.OrderGroup group : ordersMenuBuilder.groupsFor(botPlayer)) {
            if (group.unitIds().size() < 2) {
                continue;
            }
            FormationOrder current = currentFormation(group);
            String label = (current == null) ? group.label()
                  : group.label() + " - " + Messages.getString("BotCommandPanel.Formations.shape." + current.getShape());
            JMenu groupMenu = new JMenu(label);
            addFormationItems(groupMenu, botPlayer, group, current);
            botMenu.add(groupMenu);
        }
        if (botMenu.getItemCount() > GROUP_SCROLL_THRESHOLD) {
            MenuScroller.setScrollerFor(botMenu, GROUP_SCROLL_THRESHOLD);
        }
    }

    /**
     * @return the formation the group's first unit is in, if all of the group shares it, else {@code null}
     */
    private @Nullable FormationOrder currentFormation(BotOrdersMenuBuilder.OrderGroup group) {
        if (!(client.getGame() instanceof Game game)) {
            return null;
        }
        FormationOrder first = null;
        for (int unitId : group.unitIds()) {
            Entity unit = game.getEntity(unitId);
            FormationOrder formation = (unit == null) ? null : unit.getUnitOrders().getFormation().orElse(null);
            if (formation == null) {
                return null;
            }
            if (first == null) {
                first = formation;
            } else if (!first.sharesLeader(formation.getLeaderId())) {
                return null;
            }
        }
        return first;
    }

    private void addFormationItems(JMenu groupMenu, Player botPlayer, BotOrdersMenuBuilder.OrderGroup group,
          @Nullable FormationOrder current) {
        FormationShape shape = (current == null) ? DEFAULT_SHAPE : current.getShape();
        int spacing = (current == null) ? FormationOrder.DEFAULT_SPACING : current.getSpacing();
        int leaderId = (current == null) ? group.unitIds().get(0) : current.getLeaderId();
        FormationPace pace = (current == null) ? FormationPace.WALK : current.getPace();
        ContactRule contactRule = (current == null) ? ContactRule.BREAK : current.getContactRule();

        JMenu shapeMenu = new JMenu(Messages.getString("BotCommandPanel.Formations.shape"));
        for (FormationShape choice : FormationShape.values()) {
            addChoice(shapeMenu, Messages.getString("BotCommandPanel.Formations.shape." + choice),
                  (current != null) && (choice == shape),
                  () -> sendFormation(botPlayer, group, choice, spacing, leaderId, pace, contactRule));
        }
        groupMenu.add(shapeMenu);

        JMenu spacingMenu = new JMenu(Messages.getString("BotCommandPanel.Formations.spacing"));
        for (int choice = FormationOrder.MINIMUM_SPACING; choice <= FormationOrder.MAXIMUM_SPACING; choice++) {
            final int chosenSpacing = choice;
            addChoice(spacingMenu, Messages.getString("BotCommandPanel.Formations.spacing.hexes", choice),
                  choice == spacing,
                  () -> sendFormation(botPlayer, group, shape, chosenSpacing, leaderId, pace, contactRule));
        }
        groupMenu.add(spacingMenu);

        JMenu leaderMenu = new JMenu(Messages.getString("BotCommandPanel.Formations.leader"));
        for (int unitId : group.unitIds()) {
            Entity unit = (client.getGame() instanceof Game game) ? game.getEntity(unitId) : null;
            String name = (unit == null) ? String.valueOf(unitId) : unit.getDisplayName();
            addChoice(leaderMenu, Messages.getString("BotCommandPanel.Orders.unit", unitId, name), unitId == leaderId,
                  () -> sendFormation(botPlayer, group, shape, spacing, unitId, pace, contactRule));
        }
        groupMenu.add(leaderMenu);

        JMenu paceMenu = new JMenu(Messages.getString("BotCommandPanel.Formations.pace"));
        for (FormationPace choice : FormationPace.values()) {
            addChoice(paceMenu, Messages.getString("BotCommandPanel.Formations.pace." + choice), choice == pace,
                  () -> sendFormation(botPlayer, group, shape, spacing, leaderId, choice, contactRule));
        }
        groupMenu.add(paceMenu);

        JMenu contactMenu = new JMenu(Messages.getString("BotCommandPanel.Formations.contact"));
        for (ContactRule choice : ContactRule.values()) {
            addChoice(contactMenu, Messages.getString("BotCommandPanel.Formations.contact." + choice),
                  choice == contactRule,
                  () -> sendFormation(botPlayer, group, shape, spacing, leaderId, pace, choice));
        }
        groupMenu.add(contactMenu);

        groupMenu.addSeparator();
        JMenuItem offItem = new JMenuItem(Messages.getString("BotCommandPanel.Formations.off"));
        offItem.setEnabled(current != null);
        offItem.addActionListener(event -> {
            for (int unitId : group.unitIds()) {
                client.sendChat(UnitOrderCommand.commandText(unitId, UnitOrderAction.FORMATION_OFF));
            }
            acknowledge(botPlayer, group, Messages.getString("BotCommandPanel.Formations.off"));
        });
        groupMenu.add(offItem);
    }

    private static void addChoice(JMenu menu, String title, boolean isCurrent, Runnable action) {
        JMenuItem item = new JMenuItem(isCurrent ? Messages.getString("BotCommandPanel.Formations.current", title)
              : title);
        item.addActionListener(event -> action.run());
        menu.add(item);
    }

    /**
     * Sends the whole formation to every unit of the group: the leader gets slot 0, the others slots 1, 2, ... in the
     * group's order.
     */
    private void sendFormation(Player botPlayer, BotOrdersMenuBuilder.OrderGroup group, FormationShape shape,
          int spacing, int leaderId, FormationPace pace, ContactRule contactRule) {
        int nextSlot = 1;
        for (int unitId : group.unitIds()) {
            int slot = (unitId == leaderId) ? 0 : nextSlot++;
            client.sendChat(UnitOrderCommand.commandText(unitId, UnitOrderAction.FORMATION,
                  UnitOrderCommand.SHAPE + '=' + shape.name(),
                  UnitOrderCommand.LEADER + '=' + leaderId,
                  UnitOrderCommand.SPACING + '=' + spacing,
                  UnitOrderCommand.SLOT + '=' + slot,
                  UnitOrderCommand.PACE + '=' + pace.name(),
                  UnitOrderCommand.CONTACT + '=' + contactRule.name()));
        }
        acknowledge(botPlayer, group, Messages.getString("BotCommandPanel.Formations.toast",
              Messages.getString("BotCommandPanel.Formations.shape." + shape), spacing));
    }

    private void acknowledge(Player botPlayer, BotOrdersMenuBuilder.OrderGroup group, String description) {
        LOGGER.info("[BotOrders] formation {} sent to {} of {}", description, group.label(), botPlayer.getName());
        acknowledger.accept(botPlayer, Messages.getString("BotCommandPanel.Orders.toast", description, group.label()));
    }
}
