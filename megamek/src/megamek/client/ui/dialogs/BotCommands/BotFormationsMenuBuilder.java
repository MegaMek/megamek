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

import java.util.List;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

import megamek.client.AbstractClient;
import megamek.client.ui.Messages;
import megamek.common.force.Force;
import megamek.common.orders.FormationOrder;
import megamek.common.orders.FormationShape;
import megamek.common.orders.UnitOrderAction;
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
}
