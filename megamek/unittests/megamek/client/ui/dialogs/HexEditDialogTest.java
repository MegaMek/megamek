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
package megamek.client.ui.dialogs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import megamek.common.Hex;
import megamek.common.units.Terrain;
import megamek.common.units.Terrains;
import org.junit.jupiter.api.Test;

/**
 * Verifies which terrain the Change Terrain brush starts from when the dialog is opened on a hex.
 *
 * <p>The dialog opens on a hex the gamemaster right-clicked, and the brush is meant to start from what that hex
 * actually holds - including how far the terrain has already been knocked down. Woods shelled to a terrain factor of
 * 15 that open reading 50 invite a gamemaster to press Apply and quietly restore them.</p>
 */
class HexEditDialogTest {

    /** @return a hex holding the given terrain at the given level, at the factor the rules give it when new */
    private static Hex hexHolding(int terrainType, int level) {
        Hex hex = new Hex();
        hex.addTerrain(new Terrain(terrainType, level));
        return hex;
    }

    @Test
    void theBrushStartsFromTheTerrainTheHexHolds() {
        Terrain starting = HexEditDialog.brushableTerrainIn(hexHolding(Terrains.WOODS, 1));

        assertNotNull(starting, "a hex of woods has something for the brush to start from");
        assertEquals(Terrains.WOODS, starting.getType(), "the brush should start on woods");
        assertEquals(1, starting.getLevel(), "at the level the hex holds them");
    }

    @Test
    void theBrushStartsFromTheTerrainFactorTheHexHolds() {
        // the reported fault: light woods knocked down to 15 opened the dialog reading 50, the value the rules give
        // new light woods, so applying without touching anything put the woods back up again
        Hex hex = hexHolding(Terrains.WOODS, 1);
        hex.getTerrain(Terrains.WOODS).setTerrainFactor(15);

        Terrain starting = HexEditDialog.brushableTerrainIn(hex);

        assertNotNull(starting);
        assertEquals(15, starting.getTerrainFactor(),
              "the brush should start from what is left of the woods, not from what new woods would have");
    }

    @Test
    void aHexHoldingNothingTheBrushPaintsStartsFromNothing() {
        assertNull(HexEditDialog.brushableTerrainIn(new Hex()),
              "bare ground gives the brush nothing to start from, so it stays on its own first choice");
    }

    @Test
    void aStructureIsNotSomethingTheBrushStartsFrom() {
        Hex hex = new Hex();
        hex.addTerrain(new Terrain(Terrains.BUILDING, 2));
        hex.addTerrain(new Terrain(Terrains.BLDG_CF, 40));

        assertNull(HexEditDialog.brushableTerrainIn(hex),
              "a building is changed in its own dialog, so the terrain brush does not open on it");
    }

    @Test
    void waterIsSomethingTheBrushStartsFrom() {
        Terrain starting = HexEditDialog.brushableTerrainIn(hexHolding(Terrains.WATER, 2));

        assertNotNull(starting, "water is terrain the brush paints");
        assertEquals(2, starting.getLevel(), "and it should start at the depth the hex holds");
    }
}
