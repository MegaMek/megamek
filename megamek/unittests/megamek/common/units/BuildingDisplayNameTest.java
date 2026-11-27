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

import static org.junit.jupiter.api.Assertions.assertEquals;

import megamek.common.enums.BuildingType;
import org.junit.jupiter.api.Test;

/**
 * Issue #9784: the building name shown in hex tooltips, attack targets and game reports read "Heavy Building
 * Building #12", because the class word "Building" was added to a name that already starts with "Building #".
 */
class BuildingDisplayNameTest {

    @Test
    void standardBuildingNamesBuildingOnce() {
        Building building = new Building(BuildingType.HEAVY, IBuilding.STANDARD, 12, Terrains.BUILDING);
        assertEquals("Heavy Building #12", building.toString());
    }

    @Test
    void hangarKeepsItsClassWord() {
        Building building = new Building(BuildingType.MEDIUM, IBuilding.HANGAR, 7, Terrains.BUILDING);
        assertEquals("Medium Hangar Building #7", building.toString());
    }

    @Test
    void wallNamesWallOnce() {
        Building building = new Building(BuildingType.WALL, IBuilding.STANDARD, 5, Terrains.BUILDING);
        assertEquals("Wall #5", building.toString());
    }

    @Test
    void bridgeHasNoBuildingWord() {
        Building building = new Building(BuildingType.MEDIUM, IBuilding.STANDARD, 3, Terrains.BRIDGE);
        assertEquals("Medium Bridge #3", building.toString());
    }

    @Test
    void fuelTankHasNoBuildingWord() {
        Building building = new Building(BuildingType.HEAVY, IBuilding.STANDARD, 4, Terrains.FUEL_TANK);
        assertEquals("Heavy Fuel Tank #4", building.toString());
    }
}
