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

import java.io.File;

import megamek.common.board.Coords;
import megamek.common.equipment.EquipmentType;
import megamek.common.loaders.MekFileParser;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Pins the way an Advanced Building's locations map onto its hexes and levels, which the damage editor relies on
 * to group a building's locations by hex.
 */
class BuildingLocationLayoutTest {

    /** Seven hexes of three levels each: 21 locations. */
    private static final String LARGE_BUILDING_FILE = "Simple Large Building Entity.blk";

    @BeforeAll
    static void initialize() {
        EquipmentType.initializeTypes();
    }

    private AbstractBuildingEntity loadLargeBuilding() throws Exception {
        File file = new File("testresources/megamek/common/units/" + LARGE_BUILDING_FILE);
        return (AbstractBuildingEntity) new MekFileParser(file).getEntity();
    }

    @Test
    @DisplayName("locations run hex by hex, one per level, in declaration order")
    void locationsMapToHexAndLevel() throws Exception {
        AbstractBuildingEntity building = loadLargeBuilding();

        assertEquals(0, building.getHexIndex(0));
        assertEquals(0, building.getLocationLevel(0));
        assertEquals(0, building.getHexIndex(2));
        assertEquals(2, building.getLocationLevel(2));
        assertEquals(1, building.getHexIndex(3), "the second hex starts where the first hex's levels end");
        assertEquals(0, building.getLocationLevel(3));
        assertEquals(6, building.getHexIndex(20), "the last location is the top level of the last hex");
        assertEquals(2, building.getLocationLevel(20));
    }

    @Test
    @DisplayName("every location of a hex reports the same hex index")
    void everyLevelOfAHexSharesItsIndex() throws Exception {
        AbstractBuildingEntity building = loadLargeBuilding();
        int height = building.getInternalBuilding().getBuildingHeight();

        for (int location = 0; location < building.locations(); location++) {
            assertEquals(location / height, building.getHexIndex(location), "location " + location);
        }
    }

    @Test
    @DisplayName("before placement a location's abbreviation names its hex by number, not by coordinates")
    void unplacedAbbreviationNamesTheHexByNumber() throws Exception {
        AbstractBuildingEntity building = loadLargeBuilding();

        assertEquals("LVL 0 H1", building.getLocationAbbr(0));
        assertEquals("LVL 2 H2", building.getLocationAbbr(5));
        assertEquals("LVL 1 H7", building.getLocationAbbr(19));
    }

    @Test
    @DisplayName("before placement a location's name keeps the cube coordinates the unit file is keyed by")
    void unplacedNameKeepsTheFileKey() throws Exception {
        AbstractBuildingEntity building = loadLargeBuilding();

        assertEquals("Level 0 0.0,0.0,0.0", building.getLocationName(0));
        assertEquals("Level 2 1.0,-1.0,0.0", building.getLocationName(5));
    }

    @Test
    @DisplayName("once placed, both the name and the abbreviation carry the board hex")
    void placedLocationsCarryTheBoardHex() throws Exception {
        AbstractBuildingEntity building = loadLargeBuilding();
        building.setPosition(new Coords(3, 4));

        assertEquals("LVL 0 0405", building.getLocationAbbr(0));
        assertEquals("Level 0 0405", building.getLocationName(0));
    }

    @Test
    @DisplayName("an unknown location belongs to no hex")
    void unknownLocationHasNoHex() throws Exception {
        AbstractBuildingEntity building = loadLargeBuilding();

        assertEquals(-1, building.getHexIndex(building.locations()));
        assertEquals(-1, building.getHexIndex(-1));
    }
}
