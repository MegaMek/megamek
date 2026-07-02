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

package megamek.server.victory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;

import megamek.common.game.Game;
import megamek.server.victory.VictoryPointTracker.Recipient;
import megamek.server.victory.VictoryPointTracker.VictoryPointAward;
import org.junit.jupiter.api.Test;

class VictoryPointTrackerTest {

    @Test
    void testAwardToPlayerAccumulates() {
        VictoryPointTracker tracker = new VictoryPointTracker();
        tracker.awardToPlayer(3, 1, 1, "controls Objective 1");
        tracker.awardToPlayer(3, 2, 2, "controls Objectives 1 and 2");

        assertEquals(3, tracker.getPlayerVictoryPoints(3));
        assertEquals(0, tracker.getPlayerVictoryPoints(99));
    }

    @Test
    void testAwardToTeamAccumulates() {
        VictoryPointTracker tracker = new VictoryPointTracker();
        tracker.awardToTeam(1, 1, 1, "controls Objective 1");
        tracker.awardToTeam(1, 1, 2, "controls Objective 1");
        tracker.awardToTeam(2, 2, 2, "controls Objectives 3 and 4");

        assertEquals(2, tracker.getTeamVictoryPoints(1));
        assertEquals(2, tracker.getTeamVictoryPoints(2));
        assertEquals(0, tracker.getTeamVictoryPoints(3));
    }

    @Test
    void testHasAnyScore() {
        VictoryPointTracker tracker = new VictoryPointTracker();
        assertFalse(tracker.hasAnyScore());

        tracker.awardToTeam(1, 1, 1, "controls Objective 1");
        assertTrue(tracker.hasAnyScore());
    }

    @Test
    void testAwardLogRecordsAwards() {
        VictoryPointTracker tracker = new VictoryPointTracker();
        tracker.awardToTeam(2, 1, 4, "controls Objective 1");
        tracker.awardToPlayer(5, 3, 6, "successful scans exfiltrated");

        assertEquals(2, tracker.getAwardLog().size());

        VictoryPointAward firstAward = tracker.getAwardLog().getFirst();
        assertEquals(4, firstAward.gameRound());
        assertEquals(Recipient.TEAM, firstAward.recipient());
        assertEquals(2, firstAward.recipientId());
        assertEquals(1, firstAward.points());
        assertEquals("controls Objective 1", firstAward.reason());

        VictoryPointAward secondAward = tracker.getAwardLog().getLast();
        assertEquals(Recipient.PLAYER, secondAward.recipient());
        assertEquals(5, secondAward.recipientId());
        assertEquals(3, secondAward.points());
    }

    @Test
    void testGetTrackerCreatesAndStoresInVictoryContext() {
        Game game = new Game();
        game.setVictoryContext(new HashMap<>());

        VictoryPointTracker tracker = VictoryPointTracker.getTracker(game);

        assertNotNull(tracker);
        assertSame(tracker, game.getVictoryContext().get(VictoryPointTracker.VICTORY_CONTEXT_KEY));
    }

    @Test
    void testGetTrackerInitializesMissingVictoryContext() {
        Game game = new Game();
        assertNull(game.getVictoryContext());

        VictoryPointTracker tracker = VictoryPointTracker.getTracker(game);

        assertNotNull(tracker);
        assertNotNull(game.getVictoryContext());
        assertSame(tracker, game.getVictoryContext().get(VictoryPointTracker.VICTORY_CONTEXT_KEY));
    }

    @Test
    void testGetTrackerReturnsSameInstance() {
        Game game = new Game();
        game.setVictoryContext(new HashMap<>());

        VictoryPointTracker firstCall = VictoryPointTracker.getTracker(game);
        firstCall.awardToTeam(1, 2, 1, "controls Objective 1");
        VictoryPointTracker secondCall = VictoryPointTracker.getTracker(game);

        assertSame(firstCall, secondCall);
        assertEquals(2, secondCall.getTeamVictoryPoints(1));
    }

    @Test
    void testFindTrackerWithoutContextOrTracker() {
        assertNull(VictoryPointTracker.findTracker(null));
        assertNull(VictoryPointTracker.findTracker(new HashMap<>()));
    }

    @Test
    @SuppressWarnings("unchecked")
    void testSerializationRoundTrip() throws Exception {
        Game game = new Game();
        game.setVictoryContext(new HashMap<>());
        VictoryPointTracker tracker = VictoryPointTracker.getTracker(game);
        tracker.awardToTeam(1, 2, 3, "controls Objectives 1 and 2");
        tracker.awardToPlayer(4, 1, 5, "successful scan exfiltrated");

        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteStream)) {
            objectOutputStream.writeObject(game.getVictoryContext());
        }

        HashMap<String, Object> restoredContext;
        try (ObjectInputStream objectInputStream =
              new ObjectInputStream(new ByteArrayInputStream(byteStream.toByteArray()))) {
            restoredContext = (HashMap<String, Object>) objectInputStream.readObject();
        }

        VictoryPointTracker restoredTracker = VictoryPointTracker.findTracker(restoredContext);
        assertNotNull(restoredTracker);
        assertEquals(2, restoredTracker.getTeamVictoryPoints(1));
        assertEquals(1, restoredTracker.getPlayerVictoryPoints(4));
        assertEquals(2, restoredTracker.getAwardLog().size());
        assertEquals("controls Objectives 1 and 2", restoredTracker.getAwardLog().getFirst().reason());
    }
}
