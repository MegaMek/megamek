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

import megamek.common.board.Coords;
import megamek.common.orders.OrderPriority;
import megamek.common.orders.UnitOrderAction;
import megamek.common.orders.WaypointFormation;
import megamek.common.orders.WaypointOrder;
import megamek.server.commands.UnitOrderCommand;

/**
 * Turns a move order set up in the Move Order editor into the {@code /unitOrder} commands that carry it to each unit:
 * its leader and slot in the formation, then its route with each waypoint's formation, facing and hold, and the
 * route's priority.
 */
final class MoveOrderCommands {

    private MoveOrderCommands() {
    }

    /**
     * @param unitIds        the units ordered, in the order they take formation slots after the leader
     * @param leaderId       the unit the others form on
     * @param wasInFormation {@code true} if any of the units is in a formation now, so leaving formation is sent
     * @param hexes          the route, or empty to keep each unit's route and only set the priority
     * @param waypointOrders what to do at each hex of the route, each with the formation for the leg ending there
     * @param priority       how hard to push for the route
     *
     * @return the commands to send, in order
     */
    static List<String> commands(List<Integer> unitIds, int leaderId, boolean wasInFormation, List<Coords> hexes,
          List<WaypointOrder> waypointOrders, OrderPriority priority) {
        // the formation order carries the leader and slots; the first leg in formation sets its starting shape
        WaypointFormation firstFormation = null;
        for (WaypointOrder order : waypointOrders) {
            if ((order.getFormation() != null) && !order.getFormation().isNone()) {
                firstFormation = order.getFormation();
                break;
            }
        }
        boolean isInFormation = (firstFormation != null) && (unitIds.size() >= 2);
        List<String> commands = new ArrayList<>();
        int nextSlot = 1;
        for (int unitId : unitIds) {
            if (isInFormation) {
                int slot = (unitId == leaderId) ? 0 : nextSlot++;
                commands.add(UnitOrderCommand.commandText(unitId, UnitOrderAction.FORMATION,
                      UnitOrderCommand.SHAPE + '=' + firstFormation.getShape().name(),
                      UnitOrderCommand.LEADER + '=' + leaderId,
                      UnitOrderCommand.SPACING + '=' + firstFormation.getSpacing(),
                      UnitOrderCommand.SLOT + '=' + slot,
                      UnitOrderCommand.PACE + '=' + firstFormation.getPace().name(),
                      UnitOrderCommand.CONTACT + '=' + firstFormation.getContactRule().name(),
                      UnitOrderCommand.TOGETHER + '=' + firstFormation.isKeepTogether()));
            } else if (wasInFormation) {
                commands.add(UnitOrderCommand.commandText(unitId, UnitOrderAction.FORMATION_OFF));
            }
            if (hexes.isEmpty()) {
                commands.add(UnitOrderCommand.commandText(unitId, UnitOrderAction.PRIORITY,
                      UnitOrderCommand.PRIORITY + '=' + priority.name()));
            } else {
                commands.add(UnitOrderCommand.commandText(unitId, UnitOrderAction.ROUTE,
                      UnitOrderCommand.hexesArgument(hexes, isInFormation ? waypointOrders
                            : withoutFormations(waypointOrders)),
                      UnitOrderCommand.PRIORITY + '=' + priority.name()));
            }
        }
        return commands;
    }

    /**
     * @return the orders with no formation on any leg, for units that travel out of formation throughout
     */
    private static List<WaypointOrder> withoutFormations(List<WaypointOrder> waypointOrders) {
        List<WaypointOrder> plain = new ArrayList<>();
        for (WaypointOrder order : waypointOrders) {
            plain.add(new WaypointOrder(order.getFacing(), order.getHoldTurns()));
        }
        return plain;
    }
}
