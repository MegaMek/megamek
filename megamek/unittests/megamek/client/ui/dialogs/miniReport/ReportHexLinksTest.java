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
package megamek.client.ui.dialogs.miniReport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import megamek.common.board.Board;
import megamek.common.board.BoardLocation;
import megamek.common.board.Coords;
import megamek.common.game.Game;
import org.junit.jupiter.api.Test;

class ReportHexLinksTest {

    @Test
    void linksHexNumberAfterTheWordHex() {
        String linked = ReportHexLinks.linkHexes("Skids into Woods in hex 0612.");
        assertEquals("Skids into Woods in hex <a href='#hex:0612'>0612</a>.", linked);
    }

    @Test
    void linksHexTargetName() {
        String linked = ReportHexLinks.linkHexes("fires at Hex: 0808 (Ignite)");
        assertEquals("fires at Hex: <a href='#hex:0808'>0808</a> (Ignite)", linked);
    }

    @Test
    void carriesBoardIdIntoTheLink() {
        String linked = ReportHexLinks.linkHexes("lands in hex 1005 (Board Id: 2).");
        assertEquals("lands in hex <a href='#hex:1005:2'>1005</a> (Board Id: 2).", linked);
    }

    @Test
    void leavesExistingLinksAndTagsAlone() {
        String html = "<a href='#tooltip:Roll for hex 0101'>hex 0202</a> <span class='hex 0303'>x</span>";
        assertEquals(html, ReportHexLinks.linkHexes(html));
    }

    @Test
    void leavesNumbersWithoutTheWordHexAlone() {
        String html = "takes 1234 damage, hexagon 0101, rolls 12";
        assertEquals(html, ReportHexLinks.linkHexes(html));
    }

    @Test
    void parsesLinkOnTheBoard() {
        Game game = new Game();
        game.setBoard(new Board(16, 17));
        Optional<BoardLocation> location = ReportHexLinks.parseHexLink("0612", game);
        assertEquals(Optional.of(BoardLocation.of(new Coords(5, 11), 0)), location);
    }

    @Test
    void rejectsHexOffTheBoard() {
        Game game = new Game();
        game.setBoard(new Board(16, 17));
        assertTrue(ReportHexLinks.parseHexLink("3834", game).isEmpty());
    }

    @Test
    void picksTheSplitThatFitsAWideBoard() {
        // "10106" reads as column 101 row 06 on a wide board, or column 10 row 106 on a tall one
        Game wideGame = new Game();
        wideGame.setBoard(new Board(120, 20));
        assertEquals(Optional.of(BoardLocation.of(new Coords(100, 5), 0)),
              ReportHexLinks.parseHexLink("10106", wideGame));

        Game tallGame = new Game();
        tallGame.setBoard(new Board(20, 120));
        assertEquals(Optional.of(BoardLocation.of(new Coords(9, 105), 0)),
              ReportHexLinks.parseHexLink("10106", tallGame));
    }

    @Test
    void usesBoardIdFromTheLink() {
        Game game = new Game();
        game.setBoard(new Board(16, 17));
        game.setBoard(2, new Board(30, 30));
        assertEquals(Optional.of(BoardLocation.of(new Coords(24, 19), 2)),
              ReportHexLinks.parseHexLink("2520:2", game));
        assertTrue(ReportHexLinks.parseHexLink("2520", game).isEmpty());
    }
}
