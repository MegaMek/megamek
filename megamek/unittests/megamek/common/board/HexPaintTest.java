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
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import megamek.common.units.Terrains;
import org.junit.jupiter.api.Test;

/**
 * Verifies when two brush strokes count as the same one.
 *
 * <p>This is what decides whether clicking a hex a second time takes it back out of a gamemaster's edit or paints
 * over it. Clicking with the brush unchanged means the hex was picked by mistake and should come out; clicking after
 * changing the brush means it should be given the new terrain instead.</p>
 */
class HexPaintTest {

    /** @return a paint holding the given terrain at the given level */
    private static HexEditSpec.HexPaint paintOf(int terrainType, int level) {
        HexEditSpec.HexPaint paint = new HexEditSpec.HexPaint();
        paint.setTerrain(terrainType, level);
        return paint;
    }

    @Test
    void theSameBrushTwiceIsTheSameStroke() {
        assertEquals(paintOf(Terrains.WATER, 1), paintOf(Terrains.WATER, 1),
              "clicking a hex again with the brush unchanged should be recognised as the same stroke");
    }

    @Test
    void aDifferentDepthIsADifferentStroke() {
        assertNotEquals(paintOf(Terrains.WATER, 1), paintOf(Terrains.WATER, 2),
              "deepening the water is a change to the hex, not a click to be taken back");
    }

    @Test
    void aDifferentTerrainIsADifferentStroke() {
        assertNotEquals(paintOf(Terrains.WATER, 1), paintOf(Terrains.WOODS, 1),
              "painting woods over water is a change to the hex, not a click to be taken back");
    }

    @Test
    void twoBareGroundStrokesAreTheSame() {
        assertEquals(new HexEditSpec.HexPaint(), new HexEditSpec.HexPaint(),
              "bare ground is a stroke like any other and must be undoable by clicking again");
    }

    @Test
    void bareGroundIsNotTheSameAsAnyTerrain() {
        assertNotEquals(new HexEditSpec.HexPaint(), paintOf(Terrains.WATER, 1),
              "clearing a hex and flooding it are different things to do to it");
    }

    @Test
    void changingTheGroundLevelMakesItADifferentStroke() {
        HexEditSpec.HexPaint raised = paintOf(Terrains.WATER, 1);
        raised.setLevel(3);

        assertNotEquals(paintOf(Terrains.WATER, 1), raised,
              "the ground level is part of what the hex ends up as, so changing it is a new stroke");
    }
}
