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

package megamek.client.ui.clientGUI.tooltip;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import megamek.common.Hex;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.units.BuildingTarget;
import megamek.common.units.IBuilding;
import megamek.common.units.Terrain;
import megamek.common.units.Terrains;
import org.junit.jupiter.api.Test;

/**
 * Checks that the Choose Target dialog's structure text reads the height from the terrain that matches the
 * structure type. Bridges and fuel tanks have no BLDG_ELEV, which used to show as a height of -2,147,483,648
 * (issue #9557).
 */
class BuildingTargetTooltipHeightTest {

    private static final Coords POSITION = new Coords(5, 5);
    private static final String NO_TERRAIN_HEIGHT = String.valueOf(Terrain.LEVEL_NONE);

    private String oneLineSummaryFor(Hex hex) {
        Board board = mock(Board.class);
        IBuilding structure = mock(IBuilding.class);
        BuildingTarget target = mock(BuildingTarget.class);
        when(target.getPosition()).thenReturn(POSITION);
        when(board.getHex(POSITION)).thenReturn(hex);
        when(board.getBuildingAt(POSITION)).thenReturn(structure);
        when(structure.getCurrentCF(POSITION)).thenReturn(80);
        when(structure.getArmor(POSITION)).thenReturn(0);
        return HexTooltip.getOneLineSummary(target, board);
    }

    private static Hex hexWith(int structureType, int elevationType, int elevation) {
        Hex hex = new Hex(-1);
        hex.addTerrain(new Terrain(structureType, 1));
        hex.addTerrain(new Terrain(elevationType, elevation));
        return hex;
    }

    @Test
    void bridgeUsesBridgeElevation() {
        String summary = oneLineSummaryFor(hexWith(Terrains.BRIDGE, Terrains.BRIDGE_ELEV, 2));

        assertTrue(summary.contains("Height: 2;"), summary);
        assertFalse(summary.contains(NO_TERRAIN_HEIGHT), summary);
    }

    @Test
    void fuelTankUsesFuelTankElevation() {
        String summary = oneLineSummaryFor(hexWith(Terrains.FUEL_TANK, Terrains.FUEL_TANK_ELEV, 3));

        assertTrue(summary.contains("Height: 3;"), summary);
    }

    @Test
    void buildingUsesBuildingElevation() {
        String summary = oneLineSummaryFor(hexWith(Terrains.BUILDING, Terrains.BLDG_ELEV, 4));

        assertTrue(summary.contains("Height: 4;"), summary);
    }
}
