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
package megamek.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

import java.util.Vector;

import megamek.common.Report;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.game.Game;
import megamek.common.net.packets.Packet;
import megamek.common.units.IBuilding;
import megamek.common.units.Terrains;
import megamek.server.totalWarfare.TWGameManager;
import megamek.utils.BoardLoader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

/**
 * Tests for what a burning building does during the End Phase (TO:AR p.41): it loses 2 Construction Factor per burning
 * hex, that loss is reported and sent to the clients, and the hex comes down when the Construction Factor reaches 0.
 */
class BurningBuildingTest {

    /** Report ID for the per-turn burn line. */
    private static final int BURN_REPORT = 5121;

    /** Report ID for "has burned to the ground!". */
    private static final int BURNED_DOWN_REPORT = 5120;

    private static final Coords BUILDING_HEX = new Coords(0, 0);

    /**
     * A Medium building of CF 40, alone on an otherwise empty board. The board loader ignores the coordinates in the
     * hex lines and fills the board in file order, so this hex is the first one: 0101.
     */
    private static final String BOARD_DATA = """
          size 5 5
          hex 0101 0 "bldg_elev:2;building:2;bldg_class:1;bldg_cf:40" ""
          end""";

    private TWGameManager gameManager;
    private Board board;
    private Vector<Report> reports;
    private MockedStatic<Server> mockedServer;

    @BeforeEach
    void beforeEach() {
        // The collapse handler held inside the game manager sends packets of its own, so stand in a mock Server
        // rather than stubbing single methods on the spy below.
        mockedServer = mockStatic(Server.class);
        mockedServer.when(Server::getServerInstance).thenReturn(mock(Server.class));

        gameManager = Mockito.spy(new TWGameManager());

        // Mock the methods that need a running Server.
        Mockito.doNothing().when(gameManager).send(any(Packet.class));
        Mockito.doNothing().when(gameManager).sendChangedHex(any(Coords.class), any(int.class));
        Mockito.doNothing().when(gameManager).entityUpdate(any(int.class));
        Mockito.doNothing().when(gameManager).sendChangedBuildings(any());

        Game game = gameManager.getGame();
        // A fresh board per test: these tests burn the building down, which would leave a shared board collapsed.
        board = BoardLoader.initializeBoard(BOARD_DATA);
        game.setBoard(board);

        reports = new Vector<>();
    }

    @AfterEach
    void afterEach() {
        mockedServer.close();
    }

    /** Sets the building hex alight and runs one End Phase of fire processing. */
    private IBuilding burnForOnePhase(int startingConstructionFactor) {
        IBuilding building = board.getBuildingAt(BUILDING_HEX);
        assertNotNull(building, "The test board should have a building at 0101");
        building.setCurrentCF(startingConstructionFactor, BUILDING_HEX);
        building.setPhaseCF(startingConstructionFactor, BUILDING_HEX);
        building.setBurning(true, BUILDING_HEX);

        new FireProcessor(gameManager).doEndPhaseChanges(reports);
        return building;
    }

    private Report findReport(int messageId) {
        for (Report report : reports) {
            if (report.messageId == messageId) {
                return report;
            }
        }
        return null;
    }

    private boolean hasReport(int messageId) {
        return findReport(messageId) != null;
    }

    @Test
    @DisplayName("A burning building hex loses 2 CF and says so, naming the hex and what is left")
    void burningHexReportsItsDamageAndRemainingConstructionFactor() {
        IBuilding building = burnForOnePhase(40);

        assertEquals(38, building.getCurrentCF(BUILDING_HEX), "A burning hex loses 2 CF per turn (TO:AR p.41)");

        Report burnReport = findReport(BURN_REPORT);
        assertNotNull(burnReport, "The burn should be reported every turn, not only when the building falls");
        String text = burnReport.text();
        assertTrue(text.contains(building.getName()), "The report should name the building: " + text);
        assertTrue(text.contains(BUILDING_HEX.getBoardNum()), "The report should name the hex: " + text);
        assertTrue(text.contains("2 damage"), "The report should state the damage: " + text);
        assertTrue(text.contains("38 Construction Factor remaining"),
              "The report should state what is left: " + text);
    }

    @Test
    @DisplayName("A building that is not burning is not damaged and not reported")
    void buildingThatIsNotBurningIsLeftAlone() {
        IBuilding building = board.getBuildingAt(BUILDING_HEX);
        assertNotNull(building);
        building.setCurrentCF(40, BUILDING_HEX);
        building.setPhaseCF(40, BUILDING_HEX);

        new FireProcessor(gameManager).doEndPhaseChanges(reports);

        assertEquals(40, building.getCurrentCF(BUILDING_HEX));
        assertFalse(hasReport(BURN_REPORT), "A building that is not on fire should produce no burn report");
    }

    @Test
    @DisplayName("The clients are told about the CF the fire took off")
    void burningHexSendsTheNewConstructionFactorToClients() {
        burnForOnePhase(40);

        verify(gameManager, atLeastOnce()).sendChangedBuildings(any());
    }

    @Test
    @DisplayName("A building hex burned down to CF 0 comes down in the same phase")
    void hexBurnedToZeroCollapsesImmediately() {
        IBuilding building = burnForOnePhase(2);

        assertEquals(0, building.getCurrentCF(BUILDING_HEX));
        assertTrue(hasReport(BURN_REPORT), "The final burn should be reported too");
        assertTrue(hasReport(BURNED_DOWN_REPORT), "The building should announce that it burned down");

        // The hex must actually come down: no building there any more, and the building terrain is replaced by rubble.
        assertNull(board.getBuildingAt(BUILDING_HEX), "A building at CF 0 cannot keep standing");
        assertFalse(board.getHex(BUILDING_HEX).containsTerrain(Terrains.BUILDING),
              "The collapsed hex should no longer hold building terrain");
        assertTrue(board.getHex(BUILDING_HEX).containsTerrain(Terrains.RUBBLE),
              "A collapsed building leaves rubble");
    }

    @Test
    @DisplayName("A building burns down on a board with no units on it")
    void hexBurnedToZeroCollapsesWithNoUnitsOnTheBoard() {
        // The game has no entities, so the position map handed to the collapse check is empty. That is a legal
        // state - a quiet map - and must not stop the building from coming down.
        assertTrue(gameManager.getGame().getEntitiesVector().isEmpty(), "This test needs an empty board");

        burnForOnePhase(2);

        assertNull(board.getBuildingAt(BUILDING_HEX), "A building at CF 0 falls even with nothing standing near it");
    }
}
