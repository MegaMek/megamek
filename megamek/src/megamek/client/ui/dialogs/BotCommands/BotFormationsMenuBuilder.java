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

import jakarta.annotation.Nullable;
import megamek.client.AbstractClient;
import megamek.client.ui.Messages;
import megamek.client.ui.enums.DialogResult;
import megamek.common.OffBoardDirection;
import megamek.common.Player;
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
     * shape, spacing {@link FormationOrder#DEFAULT_SPACING}, the lance's first unit leading and keeping together, plus
     * Formation off. A convoy keeps its Run pace and Push through; any other lance takes the default pace and contact
     * rule (see {@link MoveOrderCommands#lobbyFormationCommands}). The rest can be changed in game from the Move Order
     * editor.
     *
     * @param client  the client that sends the orders
     * @param game    the game
     * @param lance   the lance
     * @param unitIds the lance's units, in lance order
     *
     * @return the menu
     */
    public static JMenu lobbyFormationMenu(AbstractClient client, Game game, Force lance, List<Integer> unitIds) {
        return formationMenu(client, game, lance.getName(), List.of(unitIds));
    }

    /**
     * The lobby's Formation menu for a bot's parent force, a company with lances under it: the chosen shape, or
     * Formation off, goes to every lance under it, each on its own leader with its own slots and by the same rules as
     * {@link #lobbyFormationMenu}. Right-clicking the parent force offered no formation at all, and the tester
     * concluded there was none (QA playtest, #9526).
     *
     * @param client the client that sends the orders
     * @param game   the game
     * @param parent the parent force
     * @param lances the units of each lance under it that take orders, in lance order; lances of fewer than two units
     *               are left out
     *
     * @return the menu
     */
    public static JMenu lobbyParentFormationMenu(AbstractClient client, Game game, Force parent,
          List<List<Integer>> lances) {
        return formationMenu(client, game, parent.getName(), formableLances(lances));
    }

    /**
     * The lobby's Role item for a bot's parent force: disabled, saying to set the role on each lance, since a role is a
     * lance's job and a company holds lances with different ones.
     *
     * @return the item
     */
    public static JMenuItem lobbyParentRoleItem() {
        JMenuItem item = new JMenuItem(Messages.getString("BotCommandPanel.Role.lobbyParent"));
        item.setEnabled(false);
        return item;
    }

    /**
     * @param lances the units of each lance
     *
     * @return the lances of two units or more, the only ones a formation can be given
     */
    static List<List<Integer>> formableLances(List<List<Integer>> lances) {
        List<List<Integer>> formable = new ArrayList<>();
        for (List<Integer> lance : lances) {
            if (lance.size() >= 2) {
                formable.add(lance);
            }
        }
        return formable;
    }

    /**
     * The commands that give each lance a shape, or take every lance out of formation.
     *
     * @param game   the game the units are in
     * @param lances the units of each lance, in lance order, the first leading
     * @param shape  the shape, or {@code null} for Formation off
     *
     * @return the commands to send, in order
     */
    static List<String> formationCommands(Game game, List<List<Integer>> lances, @Nullable FormationShape shape) {
        List<String> commands = new ArrayList<>();
        for (List<Integer> unitIds : lances) {
            if (shape == null) {
                for (int unitId : unitIds) {
                    commands.add(UnitOrderCommand.commandText(unitId, UnitOrderAction.FORMATION_OFF));
                }
                continue;
            }
            List<Entity> units = new ArrayList<>();
            for (int unitId : unitIds) {
                Entity unit = game.getEntity(unitId);
                if (unit != null) {
                    units.add(unit);
                }
            }
            if (units.size() >= 2) {
                commands.addAll(MoveOrderCommands.lobbyFormationCommands(units, shape));
            }
        }
        return commands;
    }

    private static JMenu formationMenu(AbstractClient client, Game game, String forceName,
          List<List<Integer>> lances) {
        JMenu menu = new JMenu(Messages.getString("BotCommandPanel.Formations.lobby"));
        boolean isAnyFormable = !formableLances(lances).isEmpty();
        menu.setEnabled(isAnyFormable);
        for (FormationShape shape : FormationShape.values()) {
            JMenuItem item = new JMenuItem(Messages.getString("BotCommandPanel.Formations.shape." + shape));
            item.addActionListener(event -> {
                // the role is read when the shape is picked, so a role set after the menu opened still counts
                for (String command : formationCommands(game, lances, shape)) {
                    client.sendChat(command);
                }
                LOGGER.info("[BotOrders] lobby formation {} for {} ({} lance(s))", shape, forceName, lances.size());
            });
            menu.add(item);
        }
        menu.addSeparator();
        JMenuItem offItem = new JMenuItem(Messages.getString("BotCommandPanel.Formations.off"));
        offItem.addActionListener(event -> {
            for (String command : formationCommands(game, lances, null)) {
                client.sendChat(command);
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
        boolean isAnyInFormation = false;
        for (Entity unit : units) {
            isAnyInFormation |= unit.getUnitOrders().getFormation().isPresent();
        }
        List<String> columnCommands = MoveOrderCommands.convoyColumnCommands(units, role);
        for (String command : columnCommands) {
            client.sendChat(command);
        }
        String formationNote = "";
        if (!columnCommands.isEmpty()) {
            formationNote = isAnyInFormation ? ", keeping its shape at a convoy's Run pace and Push through"
                  : ", put in a Column to travel";
        }
        LOGGER.info("[BotOrders] lobby role {} for {} ({} units){}", (role == null) ? LanceRole.NONE_TEXT : role,
              lance.getName(), units.size(), formationNote);
    }
}
