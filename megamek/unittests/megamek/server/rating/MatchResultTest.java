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
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 */
package megamek.server.rating;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import megamek.common.Player;
import org.junit.jupiter.api.Test;

class MatchResultTest {

    @Test
    void participantsListIsDefensivelyCopied() {
        Player p1 = new Player(0, "P1");
        Player p2 = new Player(1, "P2");
        ArrayList<Player> mutableList = new ArrayList<>(List.of(p1, p2));

        MatchResult result = new MatchResult(mutableList, List.of(), true);
        mutableList.add(new Player(2, "P3"));

        assertEquals(2, result.participants().size());
    }

    @Test
    void winnersListIsDefensivelyCopied() {
        Player p1 = new Player(0, "P1");
        Player p2 = new Player(1, "P2");
        ArrayList<Player> mutableWinners = new ArrayList<>(List.of(p1));

        MatchResult result = new MatchResult(List.of(p1, p2), mutableWinners, false);
        mutableWinners.add(p2);

        assertEquals(1, result.winners().size());
    }

    @Test
    void participantsListIsUnmodifiable() {
        Player p1 = new Player(0, "P1");
        Player p2 = new Player(1, "P2");
        MatchResult result = new MatchResult(List.of(p1, p2), List.of(), true);

        assertThrows(UnsupportedOperationException.class,
            () -> result.participants().add(new Player(99, "X")));
    }

    @Test
    void winnersListIsUnmodifiable() {
        Player p1 = new Player(0, "P1");
        Player p2 = new Player(1, "P2");
        MatchResult result = new MatchResult(List.of(p1, p2), List.of(p1), false);

        assertThrows(UnsupportedOperationException.class,
            () -> result.winners().add(new Player(99, "X")));
    }

    @Test
    void drawFieldReturnsConstructorValue() {
        Player p1 = new Player(0, "P1");
        Player p2 = new Player(1, "P2");

        MatchResult drawResult = new MatchResult(List.of(p1, p2), List.of(), true);
        assertTrue(drawResult.draw());

        MatchResult winResult = new MatchResult(List.of(p1, p2), List.of(p1), false);
        assertFalse(winResult.draw());
    }

    @Test
    void nullParticipantsThrowsNullPointerException() {
        assertThrows(NullPointerException.class,
            () -> new MatchResult(null, List.of(), false));
    }

    @Test
    void nullWinnersThrowsNullPointerException() {
        Player p1 = new Player(0, "P1");
        assertThrows(NullPointerException.class,
            () -> new MatchResult(List.of(p1), null, false));
    }
}
