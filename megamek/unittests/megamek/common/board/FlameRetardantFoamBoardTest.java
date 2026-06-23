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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.Hex;
import megamek.common.units.Terrains;
import org.junit.jupiter.api.Test;

/**
 * Verifies the board-level Flame-Retardant Foam hex marking used by Foam Ammo (TO:AUE p.173). The marker
 * is stored as a {@link Terrains#FLAME_RETARDANT_FOAM} terrain on the hex so the tileset renders it and it
 * serializes with the board.
 */
class FlameRetardantFoamBoardTest {

    /** A board whose hexes are all real (empty) hexes, so terrain can be added to them. */
    private static Board boardWithHexes(int width, int height) {
        Hex[] hexes = new Hex[width * height];
        for (int i = 0; i < hexes.length; i++) {
            hexes[i] = new Hex();
        }
        return new Board(width, height, hexes);
    }

    @Test
    void marksAndClearsFoamedHexes() {
        Board board = boardWithHexes(5, 5);
        Coords coords = new Coords(1, 1);

        assertFalse(board.isFlameRetardantFoam(coords), "A fresh hex is not foamed");

        board.markFlameRetardantFoam(coords);
        assertTrue(board.isFlameRetardantFoam(coords), "A coated hex reads as foamed");
        assertTrue(board.getHex(coords).containsTerrain(Terrains.FLAME_RETARDANT_FOAM),
              "The coating is stored as a FLAME_RETARDANT_FOAM terrain so the tileset can render it");

        board.removeFlameRetardantFoam(coords);
        assertFalse(board.isFlameRetardantFoam(coords), "Clearing removes the foam marker");
        assertFalse(board.getHex(coords).containsTerrain(Terrains.FLAME_RETARDANT_FOAM),
              "Clearing removes the FLAME_RETARDANT_FOAM terrain from the hex");
    }

    @Test
    void ignoresOffBoardCoords() {
        Board board = boardWithHexes(3, 3);
        Coords offBoard = new Coords(10, 10);

        board.markFlameRetardantFoam(offBoard);
        assertFalse(board.isFlameRetardantFoam(offBoard), "Marking off-board coordinates has no effect");
    }

    @Test
    void terrainNameMatchesTilesetEntry() {
        // The tileset overlay (StandardFluidCoatings.tileinc) references "flame_retardant_foam".
        assertEquals(Terrains.FLAME_RETARDANT_FOAM, Terrains.getType("flame_retardant_foam"),
              "The FLAME_RETARDANT_FOAM terrain name must match the tileset 'flame_retardant_foam' super entry");
    }
}
