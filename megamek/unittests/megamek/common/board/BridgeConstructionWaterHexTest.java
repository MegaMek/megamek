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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.Hex;
import megamek.common.units.Terrain;
import megamek.common.units.Terrains;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link BridgeConstruction#isOverWater(Hex)}: a water hex (Total Warfare p.32 - covered by a stream, river,
 * swamp, pond or lake) is modeled as water of any depth, swamp, or rapids, while dry land is not.
 *
 * @author Claude Code (Opus 4.8)
 */
class BridgeConstructionWaterHexTest {

    private static Hex hexWith(int terrainType, int level) {
        Hex hex = new Hex();
        hex.addTerrain(new Terrain(terrainType, level));
        return hex;
    }

    @Test
    @DisplayName("deep water (ponds/lakes/rivers) is a water hex")
    void deepWaterIsWater() {
        assertTrue(BridgeConstruction.isOverWater(hexWith(Terrains.WATER, 2)));
        assertTrue(BridgeConstruction.isOverWater(hexWith(Terrains.WATER, 1)));
    }

    @Test
    @DisplayName("a shallow stream (depth-0 water) is a water hex")
    void shallowStreamIsWater() {
        assertTrue(BridgeConstruction.isOverWater(hexWith(Terrains.WATER, 0)));
    }

    @Test
    @DisplayName("a swamp is a water hex")
    void swampIsWater() {
        assertTrue(BridgeConstruction.isOverWater(hexWith(Terrains.SWAMP, 1)));
    }

    @Test
    @DisplayName("a rapids hex is a water hex")
    void rapidsIsWater() {
        assertTrue(BridgeConstruction.isOverWater(hexWith(Terrains.RAPIDS, 1)));
    }

    @Test
    @DisplayName("dry land is not a water hex")
    void dryLandIsNotWater() {
        assertFalse(BridgeConstruction.isOverWater(new Hex()));
        assertFalse(BridgeConstruction.isOverWater(hexWith(Terrains.ROUGH, 1)));
        assertFalse(BridgeConstruction.isOverWater(hexWith(Terrains.WOODS, 1)));
    }

    @Test
    @DisplayName("over a dry gap, a bank anchors only if it is a rim higher than the gap floor")
    void dryGapAnchorRequiresHigherRim() {
        Hex gapFloor = new Hex(-1);   // dry hex at level -1
        assertTrue(BridgeConstruction.isAnchoringBank(new Hex(0), gapFloor),
              "a level-0 rim is higher than the level-(-1) floor and anchors");
        assertFalse(BridgeConstruction.isAnchoringBank(new Hex(-1), gapFloor),
              "a bank level with the floor is not a rim and does not anchor (the AVLB case 2)");
    }

    @Test
    @DisplayName("over water, a bank anchors only if it is itself land (not water)")
    void waterTargetAnchorRequiresLandBank() {
        Hex waterTarget = hexWith(Terrains.WATER, 1);
        assertTrue(BridgeConstruction.isAnchoringBank(new Hex(0), waterTarget),
              "dry land beside the water anchors");
        assertFalse(BridgeConstruction.isAnchoringBank(hexWith(Terrains.WATER, 1), waterTarget),
              "another deep-water hex does not anchor");
    }
}
