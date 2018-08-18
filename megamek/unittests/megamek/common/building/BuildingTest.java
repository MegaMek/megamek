/*
 * Copyright (c) 2018 The MegaMek Team. All rights reserved.
 *
 * This file is part of MegaMek.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.common.building;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.util.Collections;

import org.junit.Assert;
import org.junit.Test;

import megamek.common.Board;
import megamek.common.Coords;
import megamek.common.IBoard;
import megamek.common.ITerrain;
import megamek.common.Terrains;
import megamek.test.TestUtilities;

public class BuildingTest {

    // These are actually integration tests on board loading.
    // For now it serves as a smoke test to catch bugs introduced while
    // refactoring Building.
    // LATER move somewhere more appropriate

    @Test
    public void test16x17CityHillsResidential2() {
        IBoard board = loadBoard("16x17 City Hills Residential 2.board"); //$NON-NLS-1$

        // the board (from data/boards/MapSet3) contains 7 bridges
        Assert.assertEquals(7, Collections.list(board.getBuildings()).size());

        checkBuilding(board, 8,  1, Terrains.BRIDGE, 1); // hex 0902
        checkBuilding(board, 9,  3, Terrains.BRIDGE, 1); // hex 1004
        checkBuilding(board, 9,  6, Terrains.BRIDGE, 1); // hex 1007
        checkBuilding(board, 8,  8, Terrains.BRIDGE, 1); // hex 0909
        checkBuilding(board, 6,  9, Terrains.BRIDGE, 1); // hex 0710
        checkBuilding(board, 2, 11, Terrains.BRIDGE, 1); // hex 0312
        checkBuilding(board, 5, 14, Terrains.BRIDGE, 1); // hex 0615
    }

    @Test
    public void test16x17CityDownTown() {
        IBoard board = loadBoard("16x17 City (Downtown).board"); //$NON-NLS-1$

        // the board (from data/boards/MapSet6) contains 27 buildings
        Assert.assertEquals(27, Collections.list(board.getBuildings()).size());

        // top-left building at 0101 and 0102
        checkBuilding(board, 0, 0, Terrains.BUILDING, 2);
        checkBuilding(board, 0, 1, Terrains.BUILDING, 2);

        // 4 separate adjacent buildings at 0301 0302 0401 0501
        checkBuilding(board, 2, 0, Terrains.BUILDING, 1);
        checkBuilding(board, 2, 1, Terrains.BUILDING, 1);
        checkBuilding(board, 3, 0, Terrains.BUILDING, 1);
        checkBuilding(board, 4, 0, Terrains.BUILDING, 1);

        // huge building spanning 1406, 1506, 1507, 1508, 1509, 1510, 1511,
        //                        1512, 1411, 1410, 1409, 1408, 1407, 1308,
        //                        1307, 1207, 1208, 1309, 1310, 1311, 1210,
        //                        1209, 1110, 1109, 1108, 1008, 1009
        checkBuilding(board, 13, 5, Terrains.BUILDING, 27); // hex 1406

    }

    @Test @SuppressWarnings("deprecation")
    public void testModCan1916x17() {
        IBoard board = loadBoard("ModCan19 16x17.board"); //$NON-NLS-1$

        // the board (from data/boards/unofficial) contains 6 buildings
        Assert.assertEquals(6, Collections.list(board.getBuildings()).size());

        // four fuel tanks at 0208 0408 0210 0410
        checkBuilding(board, 1, 7, Terrains.FUEL_TANK, 1);
        checkBuilding(board, 3, 7, Terrains.FUEL_TANK, 1);
        checkBuilding(board, 1, 9, Terrains.FUEL_TANK, 1);
        checkBuilding(board, 3, 9, Terrains.FUEL_TANK, 1);

        // let's check all about tank at 0208
        {
            Coords c = new Coords(1, 7);
            Building ft = board.getBuildingAt(c);
            assertEquals(1, ft.getAbsorbtion(c));
            assertEquals(0, ft.getArmor(c));
            assertEquals(BasementType.NONE, ft.getBasement(c));
            assertEquals(false, ft.getBasementCollapsed(c));
            assertEquals(ITerrain.LEVEL_NONE, ft.getBldgClass());
            assertFalse(ft.getBuildingClass().isPresent());
            assertEquals(0, ft.getCollapsedHexCount());
            assertEquals(ConstructionType.LIGHT, ft.getConstructionType());
            assertEquals(1, Collections.list(ft.getCoords()).size());
            assertTrue(Collections.list(ft.getCoords()).contains(c));
            assertEquals(1, ft.getCurrentCF(c));
            assertEquals(1, ft.getDamageFromScale(), .0000001);
            assertEquals(1, ft.getDamageToScale(),   .0000001);
            assertEquals(.75, ft.getDamageReductionFromOutside(), .0000001);
            assertTrue(ft.getDemolitionCharges().isEmpty());
            assertEquals(0, ft.getInfDmgFromInside(), .0000001);
            assertEquals("Fuel Tank #163180131", ft.getName()); //$NON-NLS-1$
            assertEquals(1, ft.getOriginalHexCount());
            assertEquals(1, ft.getPhaseCF(c));
            assertEquals(Terrains.FUEL_TANK, ft.getStructureType());
            assertEquals(ConstructionType.LIGHT.getId(), ft.getType());
        }

        // small building at 0513
        checkBuilding(board, 4, 12, Terrains.BUILDING, 1);

        // let's check all about it
        {
            Coords c = new Coords(4, 12);
            Building sb = board.getBuildingAt(c);
            assertEquals(2, sb.getAbsorbtion(c));
            assertEquals(0, sb.getArmor(c));
            assertEquals(BasementType.UNKNOWN, sb.getBasement(c));
            assertEquals(false, sb.getBasementCollapsed(c));
            assertEquals(ITerrain.LEVEL_NONE, sb.getBldgClass());
            assertFalse(sb.getBuildingClass().isPresent());
            assertEquals(0, sb.getCollapsedHexCount());
            assertEquals(ConstructionType.HEAVY, sb.getConstructionType());
            assertEquals(1, Collections.list(sb.getCoords()).size());
            assertTrue(Collections.list(sb.getCoords()).contains(c));
            assertEquals(15, sb.getCurrentCF(c));
            assertEquals(1, sb.getDamageFromScale(), .0000001);
            assertEquals(1, sb.getDamageToScale(),   .0000001);
            assertEquals(.25, sb.getDamageReductionFromOutside(), .0000001);
            assertTrue(sb.getDemolitionCharges().isEmpty());
            assertEquals(.5, sb.getInfDmgFromInside(), .0000001);
            assertEquals("Building #1723808560", sb.getName()); //$NON-NLS-1$
            assertEquals(1, sb.getOriginalHexCount());
            assertEquals(15, sb.getPhaseCF(c));
            assertEquals(Terrains.BUILDING, sb.getStructureType());
            assertEquals(ConstructionType.HEAVY.getId(), sb.getType());
        }

        // big building centered at 0909
        checkBuilding(board, 8, 8, Terrains.BUILDING, 23);

    }

    private IBoard loadBoard(String name) {
        InputStream is = getClass().getResourceAsStream(name);
        IBoard board = new Board(16, 17);
        board.load(is);
        return board;
    }

    private static void checkBuilding(IBoard board, int x, int y, int structureType, int hexCount) {
        Building b = board.getBuildingAt(new Coords(x, y));
        assertNotNull(b);
        Assert.assertEquals(structureType, b.getStructureType());
        Assert.assertEquals(hexCount, b.getOriginalHexCount());
        TestUtilities.checkSerializable(b);
    }

}
