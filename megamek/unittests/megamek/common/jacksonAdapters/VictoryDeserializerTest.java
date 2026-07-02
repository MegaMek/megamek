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

package megamek.common.jacksonAdapters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import megamek.server.scriptedEvents.DrawTriggeredEvent;
import megamek.server.scriptedEvents.TriggeredEvent;
import megamek.server.scriptedEvents.VictoryTriggeredEvent;
import megamek.server.trigger.AndTrigger;
import megamek.server.trigger.VictoryPointsTrigger;
import org.junit.jupiter.api.Test;

class VictoryDeserializerTest {

    @Test
    void testParseListWithVictoryAndDrawEntries() throws Exception {
        List<TriggeredEvent> events = VictoryDeserializer.parseList("""
              - player: Alice
                trigger:
                  type: victorypoints
                  points: 5
              - trigger:
                  type: roundend
                  round: 12
              """);

        assertEquals(2, events.size());
        assertInstanceOf(VictoryTriggeredEvent.class, events.get(0));
        VictoryTriggeredEvent victoryEvent = (VictoryTriggeredEvent) events.get(0);
        assertInstanceOf(VictoryPointsTrigger.class, victoryEvent.trigger());
        assertInstanceOf(DrawTriggeredEvent.class, events.get(1));
    }

    @Test
    void testParseListWithComposedConditions() throws Exception {
        List<TriggeredEvent> events = VictoryDeserializer.parseList("""
              - player: Alice
                trigger:
                  type: and
                  triggers:
                    - type: objectivecontrol
                      objective: Relay Station
                      player: Alice
                    - type: roundend
                      round: 8
              """);

        assertEquals(1, events.size());
        VictoryTriggeredEvent victoryEvent = (VictoryTriggeredEvent) events.getFirst();
        assertInstanceOf(AndTrigger.class, victoryEvent.trigger());
    }

    @Test
    void testParseListRejectsNonListInput() {
        assertThrows(IllegalArgumentException.class, () -> VictoryDeserializer.parseList("just some text"));
    }

    @Test
    void testParseListRejectsUnknownTriggerType() {
        assertThrows(Exception.class, () -> VictoryDeserializer.parseList("""
              - player: Alice
                trigger:
                  type: nosuchtrigger
              """));
    }
}
