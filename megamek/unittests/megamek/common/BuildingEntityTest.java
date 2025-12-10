/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.board.Coords;
import megamek.common.board.CubeCoords;
import megamek.common.enums.BasementType;
import megamek.common.enums.BuildingType;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.WeaponMounted;
import megamek.common.rolls.PilotingRollData;
import megamek.common.units.Building;
import megamek.common.units.BuildingEntity;
import megamek.common.units.EntityMovementType;
import megamek.common.weapons.lasers.innerSphere.medium.ISLaserMedium;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link BuildingEntity} that aren't tested by {@link IBuildingTests}. If the method is from the
 * {@link Building} interface, the test should probably be in {@code IBuildingTests}.
 *
 * Many of these tests do not have their final values - this class is not yet fully implemented.
 */
public class BuildingEntityTest extends GameBoardTestCase {
    private BuildingEntity building;

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void setUp() {
        building = new BuildingEntity(BuildingType.MEDIUM, 1);
        building.getInternalBuilding().setBuildingHeight(3);
        building.getInternalBuilding().addHex(new CubeCoords(0, 0, 0), 50, 10, BasementType.UNKNOWN, false);
        building.setPosition(new Coords(5, 5));
    }

    @Test
    void testGetUnitType() {
        assertEquals(0, building.getUnitType());
    }

    @Test
    void testIsImmobile() {
        assertTrue(building.isImmobile());
    }

    @Test
    void testIsEligibleForMovement() {
        assertFalse(building.isEligibleForMovement());
    }

    @Test
    void testMovementPoints() {
        assertEquals(0, building.getWalkMP(MPCalculationSetting.STANDARD));
        assertEquals(0, building.getRunMP(MPCalculationSetting.STANDARD));
        assertEquals(0, building.getSprintMP(MPCalculationSetting.STANDARD));
        assertEquals(0, building.getJumpMP(MPCalculationSetting.STANDARD));
    }

    @Test
    void testGetConstructionTechAdvancement() {
        assertNotNull(building.getConstructionTechAdvancement());
    }

    @Test
    void testLocations() {
        // 1 hex * 3 height = 3 locations
        assertEquals(3, building.locations());

        building.getInternalBuilding().addHex(new CubeCoords(1, -1, 0), 50, 10, BasementType.UNKNOWN, false);
        building.refreshAdditionalLocations();

        // 2 hexes * 3 height = 6 locations
        assertEquals(6, building.locations());
    }

    @Test
    void testCanChangeSecondaryFacing() {
        assertFalse(building.canChangeSecondaryFacing());
    }

    @Test
    void testIsValidSecondaryFacing() {
        for (int dir = 0; dir < 6; dir++) {
            assertFalse(building.isValidSecondaryFacing(dir));
        }
    }

    @Test
    void testClipSecondaryFacing() {
        assertEquals(0, building.clipSecondaryFacing(0));
        assertEquals(0, building.clipSecondaryFacing(3));
    }

    @Test
    void testGetMovementString() {
        assertEquals("Not possible!", building.getMovementString(EntityMovementType.MOVE_NONE));
    }

    @Test
    void testGetMovementAbbr() {
        assertEquals("!", building.getMovementAbbr(EntityMovementType.MOVE_NONE));
    }

    @Test
    void testGetLocationNames() {
        String[] names = building.getLocationNames();
        assertEquals(3, names.length);
        assertTrue(names[0].startsWith("Level"));
    }

    @Test
    void testGetLocationAbbreviations() {
        String[] abbrs = building.getLocationAbbreviations();
        assertEquals(3, abbrs.length);
        assertTrue(abbrs[0].startsWith("LVL"));
    }

    @Test
    void testSideTable() {
        assertEquals(ToHitData.SIDE_FRONT, building.sideTable(new Coords(3, 3)));
    }

    @Test
    void testRollHitLocation() {
        HitData hit = building.rollHitLocation(ToHitData.HIT_NORMAL, ToHitData.SIDE_FRONT);
        assertNotNull(hit);
        assertEquals(0, hit.getLocation());
    }

    @Test
    void testGetTransferLocation() {
        HitData hit = new HitData(0);
        assertEquals(hit, building.getTransferLocation(hit));
    }

    @Test
    void testGetWeaponArc() {
        assertEquals(0, building.getWeaponArc(0));
    }

    @Test
    void testIsSecondaryArcWeapon() {
        assertFalse(building.isSecondaryArcWeapon(0));
    }

    @Test
    void testGetNoOfSlots() {
        int[] slots = building.getNoOfSlots();
        assertEquals(1, slots.length);
        assertEquals(100, slots[0]);
    }

    @Test
    void testGetGenericBattleValue() {
        assertEquals(0, building.getGenericBattleValue());
    }

    @Test
    void testAddEquipment() throws Exception {
        WeaponMounted weapon = new WeaponMounted(building, new ISLaserMedium());
        building.addEquipment(weapon, 0, false);
        assertTrue(building.getEquipment().contains(weapon));
    }

    @Test
    void testVictoryReport() {
        assertNull(building.victoryReport());
    }

    @Test
    void testAddEntityBonuses() {
        PilotingRollData roll = new PilotingRollData(building.getId(), 0, "test");
        assertEquals(roll, building.addEntityBonuses(roll));
    }

    @Test
    void testGetMaxElevationChange() {
        assertEquals(0, building.getMaxElevationChange());
    }

    @Test
    void testIsRepairable() {
        assertEquals(building.isSalvage(), building.isRepairable());
    }

    @Test
    void testIsTargetable() {
        assertFalse(building.isTargetable());
    }

    @Test
    void testCanCharge() {
        assertFalse(building.canCharge());
    }

    @Test
    void testCanFlee() {
        assertFalse(building.canFlee(new Coords(0, 0)));
    }

    @Test
    void testCanGoDown() {
        assertFalse(building.canGoDown());
        assertFalse(building.canGoDown(0, new Coords(0, 0), 0));
    }

    @Test
    void testGetCost() {
        assertEquals(0.0, building.getCost(null, false));
    }

    @Test
    void testIsNuclearHardened() {
        assertFalse(building.isNuclearHardened());
    }

    @Test
    void testDoomedInVacuum() {
        assertFalse(building.doomedInVacuum());
    }

    @Test
    void testGetTotalCommGearTons() {
        assertEquals(building.getExtraCommGearTons(), building.getTotalCommGearTons());
    }

    @Test
    void testGetEngineHits() {
        assertEquals(0, building.getEngineHits());
    }

    @Test
    void testGetLocationDamage() {
        assertEquals("", building.getLocationDamage(0));
    }

    @Test
    void testIsCrippled() {
        assertFalse(building.isCrippled());
        assertFalse(building.isCrippled(true));
    }

    @Test
    void testIsDmgHeavy() {
        assertFalse(building.isDmgHeavy());
    }

    @Test
    void testIsDmgModerate() {
        assertFalse(building.isDmgModerate());
    }

    @Test
    void testIsDmgLight() {
        assertFalse(building.isDmgLight());
    }

    @Test
    void testHasEngine() {
        assertFalse(building.hasEngine());
    }

    @Test
    void testGetArmorType() {
        assertTrue(building.getArmorType(0) >= 0);
    }

    @Test
    void testGetArmorTechLevel() {
        assertTrue(building.getArmorTechLevel(0) >= 0);
    }

    @Test
    void testHasStealth() {
        assertFalse(building.hasStealth());
    }

    @Test
    void testGetEntityType() {
        assertTrue(building.getEntityType() > 0);
    }
}
