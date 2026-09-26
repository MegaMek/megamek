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

import megamek.common.annotations.Nullable;
import megamek.common.board.Coords;
import megamek.common.orders.ContactRule;
import megamek.common.orders.FormationPace;
import megamek.common.orders.FormationShape;
import megamek.common.orders.OrderPriority;
import megamek.common.orders.UnitOrderAction;
import megamek.common.orders.WaypointOrder;
import megamek.server.commands.UnitOrderCommand;

/**
 * Turns a move order set up in the Move Order editor into the {@code /unitOrder} commands that carry it to each unit:
 * its place in the formation, then its route with a facing and hold at each waypoint, and the route's priority.
 */
final class MoveOrderCommands {

    /**
     * The formation a move order puts its units in.
     *
     * @param shape       the shape
     * @param leaderId    the unit the others form on
     * @param spacing     hexes between neighbouring slots
     * @param pace        how the units move
     * @param contactRule what the formation does on contact
     */
    record FormationChoice(FormationShape shape, int leaderId, int spacing, FormationPace pace,
          ContactRule contactRule) {}

    private MoveOrderCommands() {
    }

    /**
     * @param unitIds         the units ordered, in the order they take formation slots after the leader
     * @param formation       the formation to put them in, or {@code null} for none
     * @param leaveFormation  {@code true} to take units out of a formation they are in, when no formation is chosen
     * @param hexes           the route, or empty to keep each unit's route and only set the priority
     * @param waypointOrders  what to do at each hex of the route, in the same order
     * @param priority        how hard to push for the route
     *
     * @return the commands to send, in order
     */
    static List<String> commands(List<Integer> unitIds, @Nullable FormationChoice formation, boolean leaveFormation,
          List<Coords> hexes, List<WaypointOrder> waypointOrders, OrderPriority priority) {
        List<String> commands = new ArrayList<>();
        boolean isInFormation = (formation != null) && (unitIds.size() >= 2);
        int nextSlot = 1;
        for (int unitId : unitIds) {
            if (isInFormation) {
                int slot = (unitId == formation.leaderId()) ? 0 : nextSlot++;
                commands.add(UnitOrderCommand.commandText(unitId, UnitOrderAction.FORMATION,
                      UnitOrderCommand.SHAPE + '=' + formation.shape().name(),
                      UnitOrderCommand.LEADER + '=' + formation.leaderId(),
                      UnitOrderCommand.SPACING + '=' + formation.spacing(),
                      UnitOrderCommand.SLOT + '=' + slot,
                      UnitOrderCommand.PACE + '=' + formation.pace().name(),
                      UnitOrderCommand.CONTACT + '=' + formation.contactRule().name()));
            } else if (leaveFormation) {
                commands.add(UnitOrderCommand.commandText(unitId, UnitOrderAction.FORMATION_OFF));
            }
            if (hexes.isEmpty()) {
                commands.add(UnitOrderCommand.commandText(unitId, UnitOrderAction.PRIORITY,
                      UnitOrderCommand.PRIORITY + '=' + priority.name()));
            } else {
                commands.add(UnitOrderCommand.commandText(unitId, UnitOrderAction.ROUTE,
                      UnitOrderCommand.hexesArgument(hexes, waypointOrders),
                      UnitOrderCommand.PRIORITY + '=' + priority.name()));
            }
        }
        return commands;
    }
}
