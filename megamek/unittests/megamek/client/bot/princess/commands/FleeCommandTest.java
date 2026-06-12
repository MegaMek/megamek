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
package megamek.client.bot.princess.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import megamek.client.bot.princess.BehaviorSettings;
import megamek.client.bot.princess.CardinalEdge;
import megamek.client.bot.princess.Princess;
import megamek.server.commands.arguments.Arguments;
import megamek.server.commands.arguments.ArgumentsParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Regression tests for {@link FleeCommand}. A flee order must enable auto-flee and the flee-board flag, otherwise
 * healthy units never actually retreat (the movement logic in UnitBehavior requires
 * {@code shouldAutoFlee() && destinationEdge != NONE}).
 *
 * @author HammerGS
 */
class FleeCommandTest {

    private FleeCommand fleeCommand;
    private Princess mockPrincess;
    private BehaviorSettings behaviorSettings;

    @BeforeEach
    void beforeEach() {
        fleeCommand = new FleeCommand();
        behaviorSettings = new BehaviorSettings();
        mockPrincess = mock(Princess.class);
        when(mockPrincess.getBehaviorSettings()).thenReturn(behaviorSettings);
    }

    private Arguments parseArguments(String edgeArgument) {
        return ArgumentsParser.parse(new String[] { "fl", edgeArgument }, fleeCommand.defineArguments());
    }

    @Test
    void testFleeOrderByIndexEnablesRetreat() {
        // The Bot Commands panel and MapMenu send the CardinalEdge index, e.g. "fl : 0" for NORTH
        fleeCommand.execute(mockPrincess, parseArguments("0"));

        assertEquals(CardinalEdge.NORTH, behaviorSettings.getDestinationEdge());
        assertTrue(behaviorSettings.shouldAutoFlee(),
              "Flee order must enable auto-flee or units will not retreat");
        verify(mockPrincess).setFallBack(eq(true), anyString());
        verify(mockPrincess).setFleeBoard(eq(true), anyString());
    }

    @Test
    void testFleeOrderByNameEnablesRetreat() {
        fleeCommand.execute(mockPrincess, parseArguments("south"));

        assertEquals(CardinalEdge.SOUTH, behaviorSettings.getDestinationEdge());
        assertTrue(behaviorSettings.shouldAutoFlee(),
              "Flee order must enable auto-flee or units will not retreat");
        verify(mockPrincess).setFallBack(eq(true), anyString());
        verify(mockPrincess).setFleeBoard(eq(true), anyString());
    }

    @Test
    void testFleeOrderToNearestEdgeEnablesRetreat() {
        fleeCommand.execute(mockPrincess, parseArguments("4"));

        assertEquals(CardinalEdge.NEAREST, behaviorSettings.getDestinationEdge());
        assertTrue(behaviorSettings.shouldAutoFlee(),
              "Flee order must enable auto-flee or units will not retreat");
        verify(mockPrincess).setFallBack(eq(true), anyString());
        verify(mockPrincess).setFleeBoard(eq(true), anyString());
    }

    @Test
    void testNoneEdgeCancelsFleeOrder() {
        // First issue a flee order, then cancel it with NONE
        fleeCommand.execute(mockPrincess, parseArguments("0"));
        fleeCommand.execute(mockPrincess, parseArguments("5"));

        assertEquals(CardinalEdge.NONE, behaviorSettings.getDestinationEdge());
        assertFalse(behaviorSettings.shouldAutoFlee(), "Canceling a flee order must disable auto-flee");
        verify(mockPrincess).setFallBack(eq(false), anyString());
        verify(mockPrincess).setFleeBoard(eq(false), anyString());
    }
}
