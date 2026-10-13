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
package megamek.common.equipment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;

import megamek.common.Hex;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.game.Game;
import megamek.common.planetaryConditions.IlluminationLevel;
import megamek.server.totalWarfare.TWGameManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Illumination rounds (TO:AR p.152): an artillery flare lights the hexes within its radius on its own board, from the
 * turn it arrives, for five turns, drifting as smoke does.
 */
class FlareRulesTest {
    private TWGameManager gameManager;
    private Game game;

    @BeforeEach
    void setUp() {
        gameManager = new TWGameManager();
        game = gameManager.getGame();
        game.setBoard(0, board());
        game.setBoard(1, board());
    }

    /** A flare fired at the second board lights that board, within its radius, and not the first. */
    @Test
    void aFlareLightsOnlyTheBoardItWasFiredAt() {
        Coords centre = new Coords(8, 8);
        gameManager.deliverArtilleryFlare(centre, 1, 2);
        game.ageFlares();
        assertEquals(IlluminationLevel.FLARE, level(1, new Coords(8, 10)), "Inside its radius 2 on its board");
        assertEquals(IlluminationLevel.NONE, level(1, new Coords(8, 11)), "Beyond its radius");
        assertEquals(IlluminationLevel.NONE, level(0, centre), "Not on the other board");
    }

    /**
     * The flare lights in the turn it arrives (its Firing phase comes before the End phase that ages it) and in each of
     * the four turns after: five turns, then it is gone.
     */
    @Test
    void aFlareLightsFromTheTurnItArrivesForFiveTurns() {
        Coords centre = new Coords(8, 8);
        gameManager.deliverArtilleryFlare(centre, 0, 3);
        assertEquals(IlluminationLevel.FLARE, level(0, centre), "Lit in the turn it arrives");
        for (int turn = 2; turn <= 5; turn++) {
            game.ageFlares();
            assertEquals(IlluminationLevel.FLARE, level(0, centre), "Still lit in turn " + turn);
        }
        game.ageFlares();
        assertEquals(IlluminationLevel.NONE, level(0, centre), "Gone after its fifth turn");
    }

    private IlluminationLevel level(int boardId, Coords coords) {
        return IlluminationLevel.determineIlluminationLevel(game, boardId, coords);
    }

    private static Board board() {
        return new Board(16, 17, Stream.generate(Hex::new).limit(16 * 17).toArray(Hex[]::new));
    }
}
