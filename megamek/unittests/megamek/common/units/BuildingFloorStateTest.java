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

package megamek.common.units;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.board.CubeCoords;
import megamek.common.enums.BasementType;
import megamek.common.enums.BuildingType;
import org.junit.jupiter.api.Test;

class BuildingFloorStateTest {
    @Test
    void floorsReceiveFullCFAndArmorAndTrackDamageIndependently() {
        var floors = new BuildingFloorState(4, 40, 20);
        floors.setArmor(2, 0);
        floors.setCF(2, 15);
        assertEquals(40, floors.getCF(1));
        assertEquals(20, floors.getArmor(1));
        assertEquals(15, floors.getCF(2));
        assertEquals(40, floors.getPhaseCF(2));
        floors.newPhase();
        assertEquals(15, floors.getPhaseCF(2));
        assertEquals(4, floors.getCriticalThreshold(2));
        floors.newRound();
        assertEquals(2, floors.getCriticalThreshold(2));
        assertFalse(floors.hasChanges());
    }

    @Test
    void topFloorCollapsePreservesTheRestOfTheHex() {
        var floors = new BuildingFloorState(4, 40, 20);
        floors.setCF(3, 0);
        floors.resolveCollapse();
        assertEquals(3, floors.height());
        assertEquals(40, floors.getCF(2));
        assertEquals(-1, floors.getLevel(3));
        assertEquals(0, floors.getArmor(3));
    }

    @Test
    void lowestFloorCollapseDestroysTheWholeHex() {
        var floors = new BuildingFloorState(4, 40, 20);
        floors.setCF(0, 0);
        floors.resolveCollapse();
        assertEquals(0, floors.height());
        assertEquals(0, floors.maximumCF());
        assertArrayEquals(new int[] { -1, -1, -1, -1 }, floors.levels());
    }

    @Test
    void topDownCollapseTransfersExcessAndSettlesSurvivingFloors() {
        // TO:AR pp.120–121 example, using the stated arithmetic: 50 - 6 - 18 = 26 on the lowest floor.
        var floors = new BuildingFloorState(7, 50, 0);
        floors.setCF(1, 40);
        floors.setCF(2, 20);
        floors.setCF(3, 0);
        floors.setCF(4, 30);
        floors.setCF(5, 40);
        floors.resolveCollapse();
        assertEquals(26, floors.getCF(0));
        assertEquals(36, floors.getCF(6));
        assertEquals(2, floors.height());
        assertArrayEquals(new int[] { 0, -1, -1, -1, -1, -1, 1 }, floors.levels());
        assertEquals(6, floors.floorAtLevel(1));
        assertTrue(floors.hasChanges());

        floors.newPhase();
        floors.resolveCollapse();
        assertEquals(26, floors.getCF(0), "resolved collapses do not deal damage again next phase");
        assertEquals(36, floors.getCF(6));
        assertFalse(floors.hasChanges());
    }

    @Test
    void synchronizingAFloorCannotRestoreARemovedHex() {
        var building = new Building(BuildingType.MEDIUM, 1, 0, Terrains.BUILDING);
        building.setBuildingHeight(3);
        building.addHex(CubeCoords.ZERO, 40, 0, BasementType.NONE, false);
        building.enableExpandedCF();
        building.removeHex(CubeCoords.ZERO);
        building.synchronizeFloorState(CubeCoords.ZERO);

        assertFalse(building.isIn(CubeCoords.ZERO));
        assertEquals(0, building.getCurrentCF(CubeCoords.ZERO));
        assertEquals(0, building.getPhaseCF(CubeCoords.ZERO));
    }

    @Test
    void multipleDestroyedLevelsDoNotSkipASecondCollapse() {
        var floors = new BuildingFloorState(6, 30, 0);
        floors.setCF(1, 0);
        floors.setCF(4, 0);
        floors.resolveCollapse();
        assertEquals(0, floors.height(), "the upper floors destroy the already weakened lowest floor");
    }
}
