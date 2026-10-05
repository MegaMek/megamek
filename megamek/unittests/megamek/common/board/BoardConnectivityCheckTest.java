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
package megamek.common.board;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import megamek.common.GameBoardTestCase;
import org.junit.jupiter.api.Test;

class BoardConnectivityCheckTest extends GameBoardTestCase {

    static {
        initializeBoard("ROADS_JOIN", """
              size 1 3
              hex 0101 0 "road:1" ""
              hex 0102 0 "road:1" ""
              hex 0103 0 "pavement:1" ""
              end""");
        initializeBoard("ROAD_INTO_NOTHING", """
              size 1 2
              hex 0101 0 "road:1:8" ""
              hex 0102 0 "rough:1" ""
              end""");
        initializeBoard("ROAD_ONE_WAY", """
              size 1 2
              hex 0101 0 "road:1:8" ""
              hex 0102 0 "road:1:8" ""
              end""");
        initializeBoard("BRIDGE_TWO_ABOVE_ROAD", """
              size 1 3
              hex 0101 0 "road:1" ""
              hex 0102 0 "water:1;bridge:1:9;bridge_cf:100;bridge_elev:2" ""
              hex 0103 0 "road:1" ""
              end""");
        initializeBoard("BRIDGE_INTO_ROUGH", """
              size 1 3
              hex 0101 0 "road:1" ""
              hex 0102 0 "water:1;bridge:1:9;bridge_cf:100;bridge_elev:0" ""
              hex 0103 0 "rough:1" ""
              end""");
        initializeBoard("BRIDGE_FLOATING_OVER_ROUGH", """
              size 1 3
              hex 0101 0 "road:1" ""
              hex 0102 0 "water:1;bridge:1:9;bridge_cf:100;bridge_elev:2" ""
              hex 0103 0 "rough:1" ""
              end""");
        initializeBoard("ROAD_ONTO_HIGH_BRIDGE", """
              size 1 3
              hex 0101 0 "road:1:8" ""
              hex 0102 0 "water:1;bridge:1;bridge_cf:100;bridge_elev:2" ""
              hex 0103 0 "" ""
              end""");
        initializeBoard("ROAD_UNDER_OVERPASS", """
              size 1 3
              hex 0101 0 "road:1" ""
              hex 0102 0 "road:1;bridge:1;bridge_cf:100;bridge_elev:3" ""
              hex 0103 0 "road:1" ""
              end""");
        initializeBoard("HIGH_ROAD_ONTO_BRIDGE_OVER_ROAD", """
              size 1 2
              hex 0101 3 "road:1:8" ""
              hex 0102 0 "road:1;bridge:1;bridge_cf:100;bridge_elev:3" ""
              end""");
        initializeBoard("BRIDGE_ONE_ABOVE_ROAD", """
              size 1 3
              hex 0101 0 "road:1" ""
              hex 0102 0 "water:1;bridge:1:9;bridge_cf:100;bridge_elev:1" ""
              hex 0103 0 "road:1" ""
              end""");
    }

    private List<BoardIssue> problemsOn(String boardName) {
        setBoard(boardName);
        return BoardConnectivityCheck.findProblems(getGame().getBoard());
    }

    @Test
    void roadsThatJoinUpHaveNoProblems() {
        assertTrue(problemsOn("ROADS_JOIN").isEmpty());
    }

    @Test
    void roadExitIntoHexWithoutRoadIsReported() {
        List<BoardIssue> problems = problemsOn("ROAD_INTO_NOTHING");

        assertEquals(1, problems.size());
        assertTrue(problems.get(0).message().contains("0101"), problems.get(0).message());
        assertEquals(new Coords(0, 0), problems.get(0).coords(), "The problem points the editor at hex 0101");
        assertTrue(problems.get(0).fix().contains("Remove this road's S exit"), problems.get(0).fix());
    }

    @Test
    void roadExitWithNoExitBackIsReported() {
        List<BoardIssue> problems = problemsOn("ROAD_ONE_WAY");

        assertEquals(1, problems.size(), "Only 0101's exit south lacks a match; 0102's exit runs off the board");
        assertTrue(problems.get(0).message().contains("no exit back"), problems.get(0).message());
    }

    @Test
    void bridgeDeckTwoLevelsAboveItsRoadIsReportedAtBothEnds() {
        assertEquals(2, problemsOn("BRIDGE_TWO_ABOVE_ROAD").size());
    }

    @Test
    void bridgeEndingOnGroundWithoutARoadIsOnlyANote() {
        assertTrue(problemsOn("BRIDGE_INTO_ROUGH").isEmpty(), "The deck meets the rough at the same level");

        List<BoardIssue> notes = BoardConnectivityCheck.findNotes(getGame().getBoard());
        assertEquals(1, notes.size(), notes.toString());
        assertTrue(notes.get(0).message().contains("TO:AR p.115"), notes.get(0).message());
    }

    @Test
    void floatingBridgeEndIsReported() {
        List<BoardIssue> problems = problemsOn("BRIDGE_FLOATING_OVER_ROUGH");

        assertEquals(2, problems.size(), "Both ends are two levels off: the road end and the floating rough end");
        assertTrue(problems.get(1).message().contains("floating") || problems.get(0).message().contains("floating"),
              problems.toString());
    }

    @Test
    void roadRunningOntoBridgeAtTheWrongHeightIsReported() {
        List<BoardIssue> problems = problemsOn("ROAD_ONTO_HIGH_BRIDGE");

        assertEquals(1, problems.size(), problems.toString());
        assertTrue(problems.get(0).message().contains("onto the bridge at 0102"), problems.get(0).message());
        assertTrue(problems.get(0).message().contains("out of reach"), problems.get(0).message());
    }

    @Test
    void roadUnderAnOverpassIsNotAJoin() {
        assertTrue(problemsOn("ROAD_UNDER_OVERPASS").isEmpty(), "The road runs under the bridge, not onto it");
    }

    @Test
    void roadMeetingTheDeckAboveAGroundRoadIsAJoinNotAnOverpass() {
        assertTrue(problemsOn("HIGH_ROAD_ONTO_BRIDGE_OVER_ROAD").isEmpty(),
              "The level 3 road meets the level 3 deck; the road below is not its neighbour");
    }

    @Test
    void issuesAtAHexListOnlyThatHex() {
        setBoard("BRIDGE_TWO_ABOVE_ROAD");

        assertEquals(2, BoardConnectivityCheck.findIssuesAt(getGame().getBoard(), new Coords(0, 1)).size(),
              "The bridge hex reports both of its ends");
        assertTrue(BoardConnectivityCheck.findIssuesAt(getGame().getBoard(), new Coords(0, 0)).isEmpty(),
              "The road hex is not where the bridge problem is reported");
    }

    @Test
    void bridgeDeckOneLevelAboveItsRoadIsOnlyACosmeticStep() {
        assertTrue(problemsOn("BRIDGE_ONE_ABOVE_ROAD").isEmpty(), "A ground vehicle can climb one level");
        assertEquals(2, BoardConnectivityCheck.findNotes(getGame().getBoard()).size(),
              "Both ends are drawn with a step in the isometric view");
    }

    @Test
    void bridgeDeckTwoLevelsAboveItsRoadIsNotListedAsANote() {
        setBoard("BRIDGE_TWO_ABOVE_ROAD");

        assertTrue(BoardConnectivityCheck.findNotes(getGame().getBoard()).isEmpty());
    }
}
