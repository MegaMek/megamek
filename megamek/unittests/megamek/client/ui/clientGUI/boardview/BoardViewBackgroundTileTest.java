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

package megamek.client.ui.clientGUI.boardview;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.image.BufferedImage;

import org.junit.jupiter.api.Test;

/**
 * Tests the offset {@link BoardView} uses to start tiling its background image inside a clip area. Regression for
 * #8760: a clip area that starts at a negative coordinate gave a negative offset and a {@code RasterFormatException}.
 */
class BoardViewBackgroundTileTest {
    private static final int TILE_SIZE = 447;

    @Test
    void negativeClipOriginGivesOffsetInsideTile() {
        assertEquals(TILE_SIZE - 1, BoardView.tileOffset(-1, TILE_SIZE));
        assertEquals(TILE_SIZE - 1, BoardView.tileOffset(-1 - TILE_SIZE, TILE_SIZE));
    }

    @Test
    void positiveClipOriginKeepsPlainRemainder() {
        assertEquals(0, BoardView.tileOffset(0, TILE_SIZE));
        assertEquals(20, BoardView.tileOffset(20, TILE_SIZE));
        assertEquals(20, BoardView.tileOffset(20 + TILE_SIZE, TILE_SIZE));
    }

    @Test
    void subimageFromNegativeClipOriginIsValid() {
        // The values from the logged error: clip origin (20, -1) on a 447x447 tile
        BufferedImage tile = new BufferedImage(TILE_SIZE, TILE_SIZE, BufferedImage.TYPE_INT_ARGB);
        int xOffset = BoardView.tileOffset(20, TILE_SIZE);
        int yOffset = BoardView.tileOffset(-1, TILE_SIZE);
        assertDoesNotThrow(() -> tile.getSubimage(xOffset, yOffset, TILE_SIZE - xOffset, TILE_SIZE - yOffset));
    }
}
