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
import java.util.Objects;

import megamek.common.OffBoardDirection;
import megamek.common.annotations.Nullable;
import megamek.common.board.Coords;
import megamek.common.orders.ContactRule;
import megamek.common.orders.FormationOrder;
import megamek.common.orders.FormationPace;
import megamek.common.orders.FormationShape;
import megamek.common.orders.LanceRole;
import megamek.common.orders.OrderPriority;
import megamek.common.orders.RouteStyle;
import megamek.common.orders.UnitOrderAction;
import megamek.common.orders.UnitOrders;
import megamek.common.orders.WaypointFormation;
import megamek.common.orders.WaypointOrder;
import megamek.common.units.Entity;
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
        // the formation order carries the leader and slots; the first shape on the route, on a leg or re-formed into at
        // a waypoint, sets its starting shape
        WaypointFormation firstFormation = null;
        for (WaypointOrder order : waypointOrders) {
            if ((order.getFormation() != null) && !order.getFormation().isNone()) {
                firstFormation = order.getFormation();
                break;
            }
            if ((order.getArrivalFormation() != null) && !order.getArrivalFormation().isNone()) {
                firstFormation = order.getArrivalFormation();
                break;
            }
        }
        boolean isInFormation = (firstFormation != null) && (unitIds.size() >= 2);
        List<String> commands = new ArrayList<>();
        int nextSlot = 1;
        for (int unitId : unitIds) {
            if (isInFormation) {
                int slot = (unitId == leaderId) ? 0 : nextSlot++;
                commands.add(formationCommand(unitId, leaderId, slot, firstFormation.getShape().name(),
                      firstFormation.getSpacing(), firstFormation.getPace().name(),
                      firstFormation.getContactRule().name(), firstFormation.isKeepTogether()));
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
     * Puts a convoy into a Column when nothing else gives it a shape: a convoy that deploys or drives scattered is
     * never what the player wants (HammerGS, 2026-10-02). It runs, and pushes through fire rather than turning to
     * fight. Left out when the role is not a convoy, when there is only one unit, or when any unit is already in a
     * formation - a shape the player set is kept.
     *
     * @param units the units ordered, the first leading
     * @param role  the role being given
     *
     * @return the formation commands, or empty
     */
    static List<String> convoyColumnCommands(List<Entity> units, @Nullable LanceRole role) {
        List<String> commands = new ArrayList<>();
        if ((role == null) || !role.isConvoy() || (units.size() < 2)) {
            return commands;
        }
        List<Integer> unitIds = new ArrayList<>();
        for (Entity unit : units) {
            if (unit.getUnitOrders().getFormation().isPresent()) {
                return commands;
            }
            unitIds.add(unit.getId());
        }
        return columnCommands(unitIds);
    }

    /**
     * @param unitIds the convoy's units, the first leading
     *
     * @return the commands that put them in a Column that runs and pushes through fire
     */
    static List<String> columnCommands(List<Integer> unitIds) {
        List<String> commands = new ArrayList<>();
        int slot = 0;
        for (int unitId : unitIds) {
            commands.add(formationCommand(unitId, unitIds.get(0), slot++, FormationShape.COLUMN.name(),
                  FormationOrder.DEFAULT_SPACING, FormationPace.RUN.name(), ContactRule.HOLD.name(), true));
        }
        return commands;
    }

    /**
     * @param waypointOrders a route's orders
     *
     * @return {@code true} if any leg of the route, or any waypoint, sets a formation
     */
    static boolean setsFormation(List<WaypointOrder> waypointOrders) {
        for (WaypointOrder order : waypointOrders) {
            if (((order.getFormation() != null) && !order.getFormation().isNone())
                  || ((order.getArrivalFormation() != null) && !order.getArrivalFormation().isNone())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Makes a lance a convoy because an escort was given it: the convoy role on each of its bot units, and a Column
     * unless it has a shape already.
     *
     * @param units the lance's units
     * @param edge  the edge it leaves by
     *
     * @return the commands to send, in order
     */
    static List<String> makeConvoyCommands(List<Entity> units, OffBoardDirection edge) {
        List<Entity> botUnits = new ArrayList<>();
        for (Entity unit : units) {
            if ((unit.getOwner() != null) && unit.getOwner().isBot()) {
                botUnits.add(unit);
            }
        }
        LanceRole convoy = LanceRole.convoy(edge);
        List<String> commands = new ArrayList<>(roleCommands(botUnits, convoy));
        commands.addAll(convoyColumnCommands(botUnits, convoy));
        return commands;
    }

    /**
     * @param waypointOrders the orders at each waypoint, as the editor holds them
     * @param isPlanning     {@code true} to have the bot plan the way to each waypoint
     * @param style          how it plans it
     *
     * @return the same orders, each set to have the way to it planned in that style, or not
     */
    static List<WaypointOrder> withRoutePlan(List<WaypointOrder> waypointOrders, boolean isPlanning,
          RouteStyle style) {
        List<WaypointOrder> marked = new ArrayList<>();
        for (WaypointOrder order : waypointOrders) {
            if (order.isPlannedTurn()) {
                // a turning point the bot planned stays one: passed straight through
                marked.add(order.withRouteStyle(style));
                continue;
            }
            marked.add(order.withRoutePlan(isPlanning ? WaypointOrder.RoutePlan.PLAN_LEG
                  : WaypointOrder.RoutePlan.NONE).withRouteStyle(style));
        }
        return marked;
    }

    /**
     * Sets the lance role on each unit that does not have it already, so resending an unchanged order adds nothing.
     *
     * @param units the units ordered
     * @param role  the role, or {@code null} for none
     *
     * @return the commands to send, in order
     */
    static List<String> roleCommands(List<Entity> units, @Nullable LanceRole role) {
        List<String> commands = new ArrayList<>();
        for (Entity unit : units) {
            if (!Objects.equals(unit.getLanceRole(), role)) {
                commands.add(UnitOrderCommand.commandText(unit.getId(), UnitOrderAction.SET_ROLE,
                      UnitOrderCommand.roleArgument(role)));
            }
        }
        return commands;
    }

    /**
     * Puts a unit into an existing formation: it takes the place after the last unit and the same route as the
     * leader, with the leader's formation, facing and hold at every waypoint and its priority.
     *
     * @param unitId        the unit joining
     * @param leaderOrders  the orders of the formation's leader
     * @param leaderId      the formation's leader
     * @param highestSlot   the highest slot taken in the formation now
     *
     * @return the commands to send, in order; empty when the leader is not in a formation
     */
    static List<String> joinCommands(int unitId, UnitOrders leaderOrders, int leaderId, int highestSlot) {
        List<String> commands = new ArrayList<>();
        if (leaderOrders.getFormation().isEmpty()) {
            return commands;
        }
        FormationOrder formation = leaderOrders.getFormation().get();
        commands.add(formationCommand(unitId, leaderId, highestSlot + 1, formation.getShape().name(),
              formation.getSpacing(), formation.getPace().name(), formation.getContactRule().name(),
              formation.isKeepTogether()));
        if (leaderOrders.hasRoute()) {
            commands.add(UnitOrderCommand.commandText(unitId, UnitOrderAction.ROUTE,
                  UnitOrderCommand.hexesArgument(leaderOrders.getRoute(), leaderOrders.getWaypointOrders()),
                  UnitOrderCommand.PRIORITY + '=' + leaderOrders.getPriority().name()));
        }
        return commands;
    }

    /**
     * Sets units to follow a player's unit: their own orders are cleared and they form up on it in the default
     * formation, a Wedge that breaks to fight when the enemy comes near and forms up again after.
     *
     * @param unitIds  the units that follow, in slot order
     * @param leaderId the player's unit they follow
     *
     * @return the commands to send, in order
     */
    static List<String> followCommands(List<Integer> unitIds, int leaderId) {
        WaypointFormation formation = WaypointTableModel.DEFAULT_FORMATION;
        List<String> commands = new ArrayList<>();
        int slot = 1;
        for (int unitId : unitIds) {
            commands.add(UnitOrderCommand.commandText(unitId, UnitOrderAction.CLEAR));
            commands.add(formationCommand(unitId, leaderId, slot++, formation.getShape().name(),
                  formation.getSpacing(), formation.getPace().name(), formation.getContactRule().name(),
                  formation.isKeepTogether()));
        }
        return commands;
    }

    private static String formationCommand(int unitId, int leaderId, int slot, String shape, int spacing,
          String pace, String contact, boolean keepTogether) {
        return UnitOrderCommand.commandText(unitId, UnitOrderAction.FORMATION,
              UnitOrderCommand.SHAPE + '=' + shape,
              UnitOrderCommand.LEADER + '=' + leaderId,
              UnitOrderCommand.SPACING + '=' + spacing,
              UnitOrderCommand.SLOT + '=' + slot,
              UnitOrderCommand.PACE + '=' + pace,
              UnitOrderCommand.CONTACT + '=' + contact,
              UnitOrderCommand.TOGETHER + '=' + keepTogether);
    }

    /**
     * @return the orders with no formation on any leg, for units that travel out of formation throughout
     */
    private static List<WaypointOrder> withoutFormations(List<WaypointOrder> waypointOrders) {
        List<WaypointOrder> plain = new ArrayList<>();
        for (WaypointOrder order : waypointOrders) {
            // only the formation goes: a lone unit still exits at the end or waits there as ordered
            plain.add(new WaypointOrder(order.getFacing(), order.getHoldMode(), order.getHoldTurns(), null,
                  order.isExitBoard()).withNavNumber(order.getNavNumber()));
        }
        return plain;
    }
}
