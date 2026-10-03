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
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

import megamek.client.AbstractClient;
import megamek.client.ui.Messages;
import megamek.client.ui.enums.DialogResult;
import megamek.common.OffBoardDirection;
import megamek.common.Player;
import megamek.common.annotations.Nullable;
import megamek.common.force.Force;
import megamek.common.game.Game;
import megamek.common.orders.FormationOrder;
import megamek.common.orders.FormationShape;
import megamek.common.orders.LanceRole;
import megamek.common.orders.LanceRoles;
import megamek.common.orders.UnitOrderAction;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;
import megamek.server.commands.UnitOrderCommand;

/**
 * Builds the lobby's formation menu for a bot lance, so the lance starts the game in formation. In game, formations
 * are set in the Move Order editor.
 *
 * <p>Every member carries the formation in its own orders, with its slot: the leader is slot 0 and the others follow
 * in the lance's order.</p>
 */
public final class BotFormationsMenuBuilder {

    private static final MMLogger LOGGER = MMLogger.create(BotFormationsMenuBuilder.class);

    private BotFormationsMenuBuilder() {
    }

    /**
     * The lobby's Formation menu for one bot lance, so a lance can start the game already in formation: one item per
     * shape, spacing {@link FormationOrder#DEFAULT_SPACING}, the lance's first unit leading, Walk pace, Break on
     * contact and keeping together, plus Formation off. The rest can be changed in game from the Move Order editor.
     *
     * @param client  the client that sends the orders
     * @param lance   the lance
     * @param unitIds the lance's units, in lance order
     *
     * @return the menu
     */
    public static JMenu lobbyFormationMenu(AbstractClient client, Force lance, List<Integer> unitIds) {
        JMenu menu = new JMenu(Messages.getString("BotCommandPanel.Formations.lobby"));
        menu.setEnabled(unitIds.size() >= 2);
        for (FormationShape shape : FormationShape.values()) {
            JMenuItem item = new JMenuItem(Messages.getString("BotCommandPanel.Formations.shape." + shape));
            item.addActionListener(event -> {
                for (int slot = 0; slot < unitIds.size(); slot++) {
                    client.sendChat(UnitOrderCommand.commandText(unitIds.get(slot), UnitOrderAction.FORMATION,
                          UnitOrderCommand.SHAPE + '=' + shape.name(),
                          UnitOrderCommand.LEADER + '=' + unitIds.get(0),
                          UnitOrderCommand.SPACING + '=' + FormationOrder.DEFAULT_SPACING,
                          UnitOrderCommand.SLOT + '=' + slot,
                          UnitOrderCommand.TOGETHER + "=true"));
                }
                LOGGER.info("[BotOrders] lobby formation {} for {} ({} units)", shape, lance.getName(),
                      unitIds.size());
            });
            menu.add(item);
        }
        menu.addSeparator();
        JMenuItem offItem = new JMenuItem(Messages.getString("BotCommandPanel.Formations.off"));
        offItem.addActionListener(event -> {
            for (int unitId : unitIds) {
                client.sendChat(UnitOrderCommand.commandText(unitId, UnitOrderAction.FORMATION_OFF));
            }
        });
        menu.add(offItem);
        return menu;
    }

    /**
     * The lobby's Role menu for one bot lance, beside Formation: None, Convoy... or Escort..., the last two opening
     * the Role dialog on that choice. Roles are set in the lobby so a convoy and its escorts start the game knowing
     * their jobs (HammerGS, 2026-10-02).
     *
     * @param client  the client that sends the orders
     * @param frame   the owner of the Role dialog
     * @param game    the game
     * @param owner   the bot that owns the lance
     * @param lance   the lance
     * @param unitIds the lance's units
     *
     * @return the menu
     */
    public static JMenu lobbyRoleMenu(AbstractClient client, JFrame frame, Game game, Player owner, Force lance,
          List<Integer> unitIds) {
        List<Entity> units = new ArrayList<>();
        for (int unitId : unitIds) {
            Entity unit = game.getEntity(unitId);
            if (unit != null) {
                units.add(unit);
            }
        }
        LanceRole current = LanceRoles.roleOf(units);
        String currentName = Messages.getString("BotCommandPanel.Role." + ((current == null) ? "NONE"
              : current.getKind().name()));
        JMenu menu = new JMenu(Messages.getString("BotCommandPanel.Role.lobby", currentName));
        menu.setEnabled(!units.isEmpty());

        JMenuItem noneItem = new JMenuItem(Messages.getString("BotCommandPanel.Role.NONE"));
        noneItem.addActionListener(event -> sendRole(client, lance, units, null));
        menu.add(noneItem);

        OffBoardDirection defaultExitEdge = LanceRoles.defaultExitEdge(units.isEmpty() ? null : units.get(0));
        JMenuItem convoyItem = new JMenuItem(Messages.getString("BotCommandPanel.Role.lobbyConvoy"));
        convoyItem.addActionListener(event -> chooseRole(client, frame, game, lance, units, List.of(),
              defaultExitEdge, ((current != null) && current.isConvoy()) ? current
                    : LanceRole.convoy(defaultExitEdge)));
        menu.add(convoyItem);

        List<LanceRoles.ConvoyChoice> convoys = LanceRoles.convoyChoices(game, owner, lance.getId());
        JMenuItem escortItem = new JMenuItem(Messages.getString("BotCommandPanel.Role.lobbyEscort"));
        escortItem.setEnabled(!convoys.isEmpty());
        if (convoys.isEmpty()) {
            escortItem.setToolTipText(Messages.getString("BotCommandPanel.Role.escort.noLance"));
        }
        escortItem.addActionListener(event -> chooseRole(client, frame, game, lance, units, convoys,
              defaultExitEdge, ((current != null) && current.isEscort()) ? current
                    : LanceRole.defaultEscort(convoys.get(0).forceId())));
        menu.add(escortItem);
        return menu;
    }

    private static void chooseRole(AbstractClient client, JFrame frame, Game game, Force lance, List<Entity> units,
          List<LanceRoles.ConvoyChoice> convoys, OffBoardDirection defaultExitEdge, LanceRole shown) {
        LanceRoleDialog dialog = new LanceRoleDialog(frame, lance.getName(), convoys, defaultExitEdge, shown);
        if (dialog.showDialog() != DialogResult.CONFIRMED) {
            return;
        }
        if (dialog.lanceToMakeConvoy().isPresent()) {
            // the lance given an escort that was not a convoy yet becomes one
            LanceRoles.ConvoyChoice convoy = dialog.lanceToMakeConvoy().get();
            for (String command : MoveOrderCommands.makeConvoyCommands(LanceRoles.unitsOf(game, convoy.forceId()),
                  convoy.newExitEdge())) {
                client.sendChat(command);
            }
            LOGGER.info("[BotOrders] lobby: {} made a convoy, leaving by the {} edge, to be escorted by {}",
                  convoy.name(), convoy.newExitEdge(), lance.getName());
        }
        sendRole(client, lance, units, dialog.getRole());
    }

    private static void sendRole(AbstractClient client, Force lance, List<Entity> units, @Nullable LanceRole role) {
        for (String command : MoveOrderCommands.roleCommands(units, role)) {
            client.sendChat(command);
        }
        List<String> columnCommands = MoveOrderCommands.convoyColumnCommands(units, role);
        for (String command : columnCommands) {
            client.sendChat(command);
        }
        LOGGER.info("[BotOrders] lobby role {} for {} ({} units){}", (role == null) ? LanceRole.NONE_TEXT : role,
              lance.getName(), units.size(), columnCommands.isEmpty() ? "" : ", put in a Column to travel");
    }
}
