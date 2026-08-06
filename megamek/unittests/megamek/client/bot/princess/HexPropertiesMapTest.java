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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.Hex;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.game.Game;
import megamek.common.units.Terrain;
import megamek.common.units.Terrains;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Built on a real board with real terrain, like FormationSideTest: the point of these labels is what the
 * rules engine says about real hexes, and a mocked board would only assert that the test agrees with
 * itself.
 */
class HexPropertiesMapTest {

    private static final int WIDTH = 10;
    private static final int HEIGHT = 10;

    private static final Coords OPEN_GROUND = new Coords(0, 0);
    private static final Coords LONE_WOODS = new Coords(2, 2);
    private static final Coords DEEP_WOODS_CENTRE = new Coords(5, 5);
    private static final Coords FORD = new Coords(8, 2);
    private static final Coords HILLTOP = new Coords(2, 7);

    private static HexPropertiesMap map;

    @BeforeAll
    static void buildMap() {
        Hex[] hexes = new Hex[WIDTH * HEIGHT];
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                hexes[y * WIDTH + x] = new Hex();
            }
        }
        // A lone woods hex in the open, and a woods clump whose centre has no open neighbor.
        addTerrain(hexes, LONE_WOODS, Terrains.WOODS, 1);
        addTerrain(hexes, DEEP_WOODS_CENTRE, Terrains.WOODS, 1);
        for (Coords neighbor : DEEP_WOODS_CENTRE.allAdjacent()) {
            addTerrain(hexes, neighbor, Terrains.WOODS, 1);
        }
        // A ford, and a hill dominating flat ground.
        addTerrain(hexes, FORD, Terrains.WATER, 1);
        hexes[HILLTOP.getY() * WIDTH + HILLTOP.getX()].setLevel(3);

        Board board = new Board(WIDTH, HEIGHT, hexes);
        // The board load path runs this per hex; among other things it adds the foliage elevation
        // that woods concealment depends on. The array constructor does not, so run it here.
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                board.initializeHex(x, y);
            }
        }
        Game game = new Game();
        game.setBoard(board);
        map = HexPropertiesMap.of(game, board, 0);
    }

    private static void addTerrain(Hex[] hexes, Coords coords, int terrainType, int level) {
        hexes[coords.getY() * WIDTH + coords.getX()].addTerrain(new Terrain(terrainType, level));
    }

    @Test
    void openGroundOffersNothing() {
        HexProperties properties = map.at(OPEN_GROUND);
        assertEquals(0, properties.concealment());
        assertFalse(properties.partialCover());
        assertFalse(properties.heatSink());
        assertFalse(properties.overlooks());
        assertTrue(properties.bank() != BankRegions.WATER, "open ground is on a bank");
    }

    @Test
    void woodsConcealPerTheRulesEngineNotByName() {
        assertTrue(map.at(LONE_WOODS).concealment() > 0,
              "the engine's terrain modifier makes woods concealing without this code naming them");
    }

    @Test
    void aWoodlineEdgeFiresOutButTheDeepWoodsAreBlind() {
        assertTrue(map.at(LONE_WOODS).concealmentEdge(), "a concealing hex beside open ground is an edge");
        assertFalse(map.at(DEEP_WOODS_CENTRE).concealmentEdge(),
              "a concealing hex with no open neighbor is deep woods, not a firing position");
    }

    @Test
    void aFordGivesCoverAndSinksHeatAndIsNotABank() {
        HexProperties ford = map.at(FORD);
        assertTrue(ford.partialCover(), "a standing Mek in depth 1 has partial cover");
        assertTrue(ford.heatSink(), "water doubles heat dissipation");
        assertEquals(BankRegions.WATER, ford.bank(), "water is not a bank");
    }

    @Test
    void aHillOverlooksTheFlatAroundIt() {
        assertTrue(map.at(HILLTOP).overlooks());
        assertFalse(map.at(OPEN_GROUND).overlooks());
        assertEquals(3, map.at(HILLTOP).elevation());
    }

    @Test
    void offBoardReportsNothing() {
        assertEquals(HexProperties.NOTHING, map.at(new Coords(-1, 5)));
        assertEquals(HexProperties.NOTHING, map.at(null));
    }
}
