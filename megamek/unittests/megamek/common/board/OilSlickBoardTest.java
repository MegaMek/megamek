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
 * Verifies the board-level Oil Slick hex marking used by Oil Slick Ammo (TO:AUE p.174). The marker is
 * stored as an {@link Terrains#OIL_SLICK} terrain on the hex so the tileset renders it and it serializes
 * with the board.
 */
class OilSlickBoardTest {

    /** A board whose hexes are all real (empty) hexes, so terrain can be added to them. */
    private static Board boardWithHexes(int width, int height) {
        Hex[] hexes = new Hex[width * height];
        for (int i = 0; i < hexes.length; i++) {
            hexes[i] = new Hex();
        }
        return new Board(width, height, hexes);
    }

    @Test
    void marksAndClearsOilSlickHexes() {
        Board board = boardWithHexes(5, 5);
        Coords coords = new Coords(2, 2);

        assertFalse(board.isOilSlick(coords), "A fresh hex is not oiled");

        board.markOilSlick(coords);
        assertTrue(board.isOilSlick(coords), "A doused hex reads as an oil slick");
        assertTrue(board.getHex(coords).containsTerrain(Terrains.OIL_SLICK),
              "The slick is stored as an OIL_SLICK terrain so the tileset can render it");

        board.removeOilSlick(coords);
        assertFalse(board.isOilSlick(coords), "Clearing removes the oil slick marker");
        assertFalse(board.getHex(coords).containsTerrain(Terrains.OIL_SLICK),
              "Clearing removes the OIL_SLICK terrain from the hex");
    }

    @Test
    void ignoresOffBoardCoords() {
        Board board = boardWithHexes(3, 3);
        Coords offBoard = new Coords(10, 10);

        board.markOilSlick(offBoard);
        assertFalse(board.isOilSlick(offBoard), "Marking off-board coordinates has no effect");
    }

    @Test
    void terrainNameMatchesTilesetEntry() {
        // The tileset overlay (StandardFluidCoatings.tileinc) references "oil_slick"; keep the name in sync.
        assertEquals(Terrains.OIL_SLICK, Terrains.getType("oil_slick"),
              "The OIL_SLICK terrain name must match the tileset 'oil_slick' super entry");
    }
}
