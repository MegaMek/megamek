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
package megamek.utilities.botorders;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import megamek.client.bot.princess.CardinalEdge;
import megamek.client.bot.princess.Princess;
import megamek.common.OffBoardDirection;
import megamek.common.board.Coords;
import megamek.common.orders.ContactRule;
import megamek.common.orders.FormationOrder;
import megamek.common.orders.FormationPace;
import megamek.common.orders.FormationShape;
import megamek.common.orders.OrderPriority;
import megamek.common.orders.UnitOrderAction;
import megamek.common.orders.UnitOrders;
import megamek.common.units.Entity;
import megamek.server.totalWarfare.TWGameManager;

/**
 * {@link OrderApplier} over the per-unit orders model: orders are stored on the unit ({@link Entity#getUnitOrders()})
 * and changed through {@link UnitOrderAction#apply}, the same definition the {@code /unitOrder} server command uses.
 *
 * <p>The new orders are set on the server's copy of the unit and sent to every client, as the server command does,
 * and also set on the bot's own copy at once. Orders are applied on the bot's thread as its Movement phase starts,
 * so the entity update from the server could otherwise arrive after the bot has already planned its move.</p>
 */
public class UnitOrdersApplier implements OrderApplier {

    private final TWGameManager gameManager;

    /**
     * @param gameManager the server's game manager, used to send the changed unit to every client
     */
    public UnitOrdersApplier(TWGameManager gameManager) {
        this.gameManager = gameManager;
    }

    @Override
    public String apply(Princess bot, Entity botUnit, Entity serverUnit, ScriptedOrder order, int round) {
        List<String> arguments = order.arguments();
        UnitOrderAction action;
        List<Coords> hexes = List.of();
        OffBoardDirection edge = OffBoardDirection.NONE;
        int facingMoving = UnitOrders.FACING_AUTO;
        int facingStopped = UnitOrders.FACING_AUTO;
        OrderPriority priority = null;
        FormationOrder formation = null;
        switch (order.action()) {
            case WAYPOINTS -> {
                action = UnitOrderAction.ROUTE;
                hexes = hexes(arguments);
                String keyword = arguments.isEmpty() ? null : ScenarioOrderScript.priorityKeyword(arguments.getFirst());
                priority = (keyword == null) ? null : OrderPriority.valueOf(keyword);
            }
            case ADD_WAYPOINTS -> {
                action = UnitOrderAction.ADD;
                hexes = hexes(arguments);
            }
            case CLEAR -> action = UnitOrderAction.CLEAR;
            case PAUSE -> action = UnitOrderAction.PAUSE;
            case RESUME -> action = UnitOrderAction.RESUME;
            case STOP -> action = UnitOrderAction.STOP;
            case MOVE_TO_EDGE -> {
                action = UnitOrderAction.MOVE_TO_EDGE;
                edge = OffBoardDirection.valueOf(arguments.getFirst().toUpperCase(Locale.ROOT));
            }
            case EXIT_BY_EDGE -> {
                action = UnitOrderAction.EXIT_BY_EDGE;
                edge = OffBoardDirection.valueOf(arguments.getFirst().toUpperCase(Locale.ROOT));
            }
            case FACING -> {
                action = UnitOrderAction.FACING;
                facingMoving = Integer.parseInt(arguments.get(0));
                facingStopped = Integer.parseInt(arguments.get(1));
            }
            case PRIORITY -> {
                action = UnitOrderAction.PRIORITY;
                priority = OrderPriority.valueOf(ScenarioOrderScript.priorityKeyword(arguments.getFirst()));
            }
            case FORMATION -> {
                action = UnitOrderAction.FORMATION;
                formation = formationFor(order, serverUnit);
            }
            case FORMATION_OFF -> action = UnitOrderAction.FORMATION_OFF;
            default -> {
                return "not a unit order: " + order.action();
            }
        }
        UnitOrders newOrders = action.apply(serverUnit.getUnitOrders(), hexes, edge, facingMoving, facingStopped,
              priority, round, formation);
        serverUnit.setUnitOrders(newOrders);
        botUnit.setUnitOrders(newOrders);
        gameManager.entityUpdate(serverUnit.getId());
        return action + " -> " + newOrders;
    }

    @Override
    public void orderFlee(Princess bot, CardinalEdge edge) {
        // the same settings FleeCommand changes
        boolean fleeing = edge != CardinalEdge.NONE;
        String reason = fleeing ? "Scripted flee order - " + edge.name() : "Scripted cancel flee order";
        bot.getBehaviorSettings().setDestinationEdge(edge);
        bot.getBehaviorSettings().setAutoFlee(fleeing);
        bot.setFallBack(fleeing, reason);
        bot.setFleeBoard(fleeing, reason);
    }

    @Override
    public OrderSnapshot snapshot(Princess bot, Entity unit) {
        UnitOrders orders = unit.getUnitOrders();
        List<Coords> route = new ArrayList<>(orders.getRoute());
        return new OrderSnapshot(route.isEmpty() ? null : route.getFirst(), route, orders.getPriority().name(),
              orders.isPaused(), orders.isStoppedInRound(bot.getGame().getCurrentRound()),
              orders.getEdgeOrder().name(), orders.getEdge().name(), orders.getFacingWhileMoving(),
              orders.getFacingWhenStopped(), orders.getFormation().map(FormationOrder::toString).orElse(""));
    }

    /**
     * Builds a unit's place in a formation. With a {@code units a b c} selector the first unit leads and the others
     * take slots 1, 2, ... in the order listed; otherwise the unit leads itself.
     */
    private static FormationOrder formationFor(ScriptedOrder order, Entity unit) {
        List<String> arguments = order.arguments();
        FormationShape shape = FormationShape.valueOf(arguments.getFirst().toUpperCase(Locale.ROOT));
        int spacing = FormationOrder.DEFAULT_SPACING;
        FormationPace pace = FormationPace.WALK;
        ContactRule contactRule = ContactRule.BREAK;
        for (int index = 1; index + 1 < arguments.size(); index += 2) {
            String value = arguments.get(index + 1).toUpperCase(Locale.ROOT);
            switch (arguments.get(index).toLowerCase(Locale.ROOT)) {
                case "spacing" -> spacing = Integer.parseInt(value);
                case "pace" -> pace = FormationPace.valueOf(value);
                case "contact" -> contactRule = ContactRule.valueOf(value);
                default -> throw new IllegalArgumentException("Unknown formation setting " + arguments.get(index));
            }
        }
        int leaderId = unit.getId();
        int slot = 0;
        if (order.targetKind() == ScriptedOrder.TargetKind.UNIT_IDS) {
            List<Integer> unitIds = ScriptedOrderDirector.listedUnitIds(order);
            leaderId = unitIds.getFirst();
            slot = Math.max(0, unitIds.indexOf(unit.getId()));
        }
        return new FormationOrder(shape, leaderId, spacing, slot, pace, contactRule);
    }

    private static List<Coords> hexes(List<String> arguments) {
        List<Coords> hexes = new ArrayList<>();
        for (String hexNumber : ScenarioOrderScript.hexArguments(arguments)) {
            hexes.add(ScenarioOrderScript.parseHexNumber(hexNumber));
        }
        return hexes;
    }
}
