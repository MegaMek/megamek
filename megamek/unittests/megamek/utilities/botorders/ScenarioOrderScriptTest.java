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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import megamek.common.board.Coords;
import megamek.utilities.botorders.ScriptedOrder.OrderAction;
import megamek.utilities.botorders.ScriptedOrder.TargetKind;
import org.junit.jupiter.api.Test;

/**
 * Tests the bot orders script parser used by the headless scenario harness.
 */
class ScenarioOrderScriptTest {

    @Test
    void parsesWaypointsForNamedUnit() {
        ScriptedOrder order = ScenarioOrderScript.parseLine(
              "round 2 | unit \"Champion CHP-2N\" | waypoints 1501 1305", 4);

        assertEquals(2, order.round());
        assertEquals(TargetKind.UNIT_NAME, order.targetKind());
        assertEquals("Champion CHP-2N", order.targetValue());
        assertEquals(OrderAction.WAYPOINTS, order.action());
        assertEquals(List.of("1501", "1305"), order.arguments());
        assertEquals(4, order.lineNumber());
    }

    @Test
    void parsesBotFleeAndUnitIdClear() {
        ScriptedOrder flee = ScenarioOrderScript.parseLine("round 3 | bot \"Lyran\" | flee NORTH", 1);
        ScriptedOrder clear = ScenarioOrderScript.parseLine("round 1 | unit id 5 | clear", 2);

        assertEquals(TargetKind.BOT, flee.targetKind());
        assertEquals(OrderAction.FLEE, flee.action());
        assertEquals(List.of("NORTH"), flee.arguments());
        assertEquals(TargetKind.UNIT_ID, clear.targetKind());
        assertEquals("5", clear.targetValue());
        assertEquals(OrderAction.CLEAR, clear.action());
    }

    @Test
    void parsesDamageAndAddWaypoints() {
        ScriptedOrder damage = ScenarioOrderScript.parseLine("round 2 | unit id 7 | damage internal 60%", 1);
        ScriptedOrder add = ScenarioOrderScript.parseLine("round 4 | all | add waypoints 1610", 2);

        assertEquals(OrderAction.DAMAGE_INTERNAL, damage.action());
        assertEquals(60, ScenarioOrderScript.damagePercent(damage));
        assertEquals(TargetKind.ALL, add.targetKind());
        assertEquals(OrderAction.ADD_WAYPOINTS, add.action());
        assertEquals(List.of("1610"), add.arguments());
    }

    @Test
    void hexNumbersAreOneBasedColumnThenRow() {
        Coords coords = ScenarioOrderScript.parseHexNumber("1501");

        assertEquals(14, coords.getX());
        assertEquals(0, coords.getY());
        assertEquals("1501", ScenarioOrderScript.toHexNumber(coords));
    }

    @Test
    void parsesUnitOrdersModelActions() {
        ScriptedOrder route = ScenarioOrderScript.parseLine("round 1 | unit id 3 | route imperative 1210 0805", 1);
        ScriptedOrder edge = ScenarioOrderScript.parseLine("round 2 | unit id 3 | move to edge NORTH", 2);
        ScriptedOrder exit = ScenarioOrderScript.parseLine("round 2 | unit id 3 | exit by edge WEST", 3);
        ScriptedOrder facing = ScenarioOrderScript.parseLine("round 2 | unit id 3 | facing moving N stopped SE", 4);
        ScriptedOrder stoppedOnly = ScenarioOrderScript.parseLine("round 2 | unit id 3 | facing stopped S", 5);
        ScriptedOrder pause = ScenarioOrderScript.parseLine("round 3 | unit id 3 | pause", 6);

        assertEquals(OrderAction.WAYPOINTS, route.action());
        assertEquals(List.of("1210", "0805"), ScenarioOrderScript.hexArguments(route.arguments()));
        assertEquals("IMPERATIVE", ScenarioOrderScript.priorityKeyword(route.arguments().getFirst()));
        assertEquals(OrderAction.MOVE_TO_EDGE, edge.action());
        assertEquals(List.of("NORTH"), edge.arguments());
        assertEquals(OrderAction.EXIT_BY_EDGE, exit.action());
        assertEquals(List.of("WEST"), exit.arguments());
        assertEquals(List.of("0", "2"), facing.arguments());
        assertEquals(List.of("-1", "3"), stoppedOnly.arguments());
        assertEquals(OrderAction.PAUSE, pause.action());
    }

    @Test
    void parsesUnitListAndFormation() {
        ScriptedOrder formation = ScenarioOrderScript.parseLine(
              "round 0 | units 101 102 103 | formation WEDGE spacing 2 contact HOLD", 1);
        ScriptedOrder off = ScenarioOrderScript.parseLine("round 3 | units 101 102 | formation off", 2);

        assertEquals(TargetKind.UNIT_IDS, formation.targetKind());
        assertEquals("101 102 103", formation.targetValue());
        assertEquals(OrderAction.FORMATION, formation.action());
        assertEquals(List.of("WEDGE", "spacing", "2", "contact", "HOLD"), formation.arguments());
        assertEquals(List.of(101, 102, 103), ScriptedOrderDirector.listedUnitIds(formation));
        assertEquals(OrderAction.FORMATION_OFF, off.action());
    }

    @Test
    void rejectsMalformedLines() {
        assertThrows(IllegalArgumentException.class,
              () -> ScenarioOrderScript.parseLine("round x | unit id 5 | clear", 1));
        assertThrows(IllegalArgumentException.class,
              () -> ScenarioOrderScript.parseLine("round 1 | somebody | clear", 1));
        assertThrows(IllegalArgumentException.class,
              () -> ScenarioOrderScript.parseLine("round 1 | unit id 5 | waypoints 15", 1));
        assertThrows(IllegalArgumentException.class,
              () -> ScenarioOrderScript.parseLine("round 1 | unit id 5 | dance", 1));
    }
}
