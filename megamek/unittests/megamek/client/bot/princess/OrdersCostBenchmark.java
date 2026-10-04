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
package megamek.client.bot.princess;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import megamek.common.Hex;
import megamek.common.Player;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.enums.GamePhase;
import megamek.common.equipment.EquipmentType;
import megamek.common.game.Game;
import megamek.common.orders.ContactRule;
import megamek.common.orders.FormationOrder;
import megamek.common.orders.FormationPace;
import megamek.common.orders.FormationShape;
import megamek.common.orders.UnitOrders;
import megamek.common.units.BipedMek;
import megamek.common.units.Entity;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Times the order questions the path ranker asks for each move it scores, for one unit of a lance among nine lances of
 * four. Not a test: run on demand to see what one unit's turn costs.
 */
@Tag("on-demand")
class OrdersCostBenchmark {

    private static final int WIDTH = 32;
    private static final int HEIGHT = 34;
    private static final int LANCES = 9;
    private static final int PATHS = 3000;

    private static final class TestMek extends BipedMek {
        @Override
        public int getWalkMP() {
            return 4;
        }
    }

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    @Test
    void timeOneUnitsTurn() {
        Hex[] hexes = new Hex[WIDTH * HEIGHT];
        for (int index = 0; index < hexes.length; index++) {
            hexes[index] = new Hex();
        }
        Board board = new Board(WIDTH, HEIGHT, hexes);
        // a real bot on its own game: a Mockito spy made every call on the bot many times slower
        Princess princess = new Princess("Bot", UUID.randomUUID().toString(), 1);
        Game game = princess.getGame();
        game.setBoard(board);
        Player bot = new Player(1, "Bot");
        bot.setBot(true);
        game.addPlayer(1, bot);
        princess.setLocalPlayerNumber(1);
        List<Entity> owned = new ArrayList<>();
        int unitId = 1;
        Entity measured = null;
        for (int lance = 0; lance < LANCES; lance++) {
            int leaderId = unitId;
            princess.getUnitOrdersFollower().noteAssembled(leaderId);
            Coords waypoint = new Coords(2 + (lance * 3), 2);
            for (int slot = 0; slot < 4; slot++) {
                TestMek mek = new TestMek();
                mek.setId(unitId++);
                mek.setOwner(bot);
                game.addEntity(mek);
                mek.setPosition(new Coords(2 + (lance * 3) + (slot % 2), 28 + (slot / 2)));
                mek.setUnitOrders(UnitOrders.NONE.withRoute(List.of(waypoint)).withFormation(
                      new FormationOrder(FormationShape.WEDGE, leaderId, 2, slot, FormationPace.WALK,
                            ContactRule.BREAK)));
                owned.add(mek);
                if ((lance == 4) && (slot == 2)) {
                    measured = mek;
                }
            }
        }
        System.out.println("ORDERS_COST owned by the bot: " + princess.getEntitiesOwned().size());
        game.setPhase(GamePhase.MOVEMENT);
        UnitOrdersFollower follower = princess.getUnitOrdersFollower();
        UnitBehavior behavior = princess.getUnitBehaviorTracker();
        Coords end = measured.getPosition().translated(0, 2);

        // warm up the route fields and the JIT
        askAsTheRankerDoes(princess, follower, behavior, measured, end, 300);
        long withoutSnapshot = askAsTheRankerDoes(princess, follower, behavior, measured, end, PATHS);
        behavior.beginRanking(measured, princess);
        long withSnapshot = askAsTheRankerDoes(princess, follower, behavior, measured, end, PATHS);
        breakDown(princess, follower, behavior, measured, end);
        behavior.endRanking();
        System.out.printf("ORDERS_COST %d units, %d paths: without snapshot %d ms (%.1f us a path), with %d ms "
                    + "(%.1f us a path)%n", owned.size(), PATHS, withoutSnapshot / 1_000_000,
              withoutSnapshot / 1000.0 / PATHS, withSnapshot / 1_000_000, withSnapshot / 1000.0 / PATHS);
    }

    private static void breakDown(Princess princess, UnitOrdersFollower follower, UnitBehavior behavior, Entity unit,
          Coords end) {
        Map<String, Runnable> questions = new LinkedHashMap<>();
        questions.put("getActiveWaypoint", () -> behavior.getActiveWaypoint(unit, princess));
        questions.put("arrivalRadius", () -> follower.arrivalRadius(unit));
        questions.put("isHeadingForHold", () -> follower.isHeadingForHold(unit));
        questions.put("orderedFacing", () -> follower.orderedFacing(unit, end));
        questions.put("facingThatStandsFor", () -> follower.facingThatStandsFor(unit, 0, end, null));
        questions.put("twistAllowance", () -> follower.twistAllowance(unit, end));
        questions.put("townCoverDiscount", () -> follower.townCoverDiscount(unit, end));
        questions.put("routeWeight", () -> follower.routeWeight(unit));
        questions.put("damageWeight", () -> follower.damageWeight(unit));
        questions.put("getFormationSlot", () -> follower.getFormationSlot(unit));
        questions.put("isHolding", () -> follower.isHolding(unit));
        questions.put("getEntitiesVector", () -> princess.getGame().getEntitiesVector());
        questions.put("formationMembers", () -> follower.roster().formationMembers(unit,
              unit.getUnitOrders().getFormation().get().getLeaderId()));
        questions.put("activeFormation", () -> follower.activeFormation(unit));
        questions.put("formationLeaderOf", () -> follower.roster().formationLeaderOf(unit));
        questions.put("isAtRouteEnd", () -> follower.isAtRouteEnd(unit));
        questions.put("isEscorting", () -> follower.convoyEscorts().isEscorting(unit));
        questions.put("isWaitingForFormation", () -> follower.isWaitingForFormation(unit));
        questions.put("isWaitingAtPhaseLine", () -> follower.isWaitingAtPhaseLine(unit));
        questions.put("isHoldingAtWaypoint", () -> follower.isHoldingAtWaypoint(unit));
        questions.put("isHoldingRouteEnd", () -> follower.isHoldingRouteEnd(unit));
        questions.put("wasHitLastTurn", () -> follower.fireReaction().wasHitLastTurn(unit));
        questions.put("isOutOfAction", () -> follower.isOutOfAction(unit));
        for (Map.Entry<String, Runnable> question : questions.entrySet()) {
            long start = System.nanoTime();
            for (int index = 0; index < 1000; index++) {
                question.getValue().run();
            }
            double microsecondsEach = (System.nanoTime() - start) / 1000.0 / 1000;
            System.out.printf("ORDERS_COST   %-20s %8.1f us%n", question.getKey(), microsecondsEach);
        }
    }

    private static long askAsTheRankerDoes(Princess princess, UnitOrdersFollower follower, UnitBehavior behavior,
          Entity unit, Coords end, int paths) {
        long start = System.nanoTime();
        for (int path = 0; path < paths; path++) {
            for (int asked = 0; asked < 4; asked++) {
                behavior.getActiveWaypoint(unit, princess);
            }
            follower.arrivalRadius(unit);
            follower.isHeadingForHold(unit);
            int facing = follower.orderedFacing(unit, end);
            follower.facingThatStandsFor(unit, facing, end, null);
            follower.twistAllowance(unit, end);
            follower.townCoverDiscount(unit, end);
            follower.townCoverDiscount(unit, end);
            follower.routeWeight(unit);
            follower.damageWeight(unit);
            behavior.getActiveWaypoint(unit, princess)
                  .ifPresent(target -> follower.routeCostFrom(unit, target, end));
        }
        return System.nanoTime() - start;
    }
}
