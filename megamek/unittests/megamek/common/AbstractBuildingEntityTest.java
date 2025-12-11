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

import java.util.stream.Stream;

import megamek.common.board.Coords;
import megamek.common.board.CubeCoords;
import megamek.common.enums.BasementType;
import megamek.common.enums.BuildingType;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.WeaponMounted;
import megamek.common.rolls.PilotingRollData;
import megamek.common.units.Building;
import megamek.common.units.AbstractBuildingEntity;
import megamek.common.units.BuildingEntity;
import megamek.common.units.EntityMovementType;
import megamek.common.units.MobileStructure;
import megamek.common.weapons.lasers.innerSphere.medium.ISLaserMedium;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests for {@link AbstractBuildingEntity} that aren't tested by {@link IBuildingTests}. If the method is from the
 * {@link Building} interface, the test should probably be in {@code IBuildingTests}.
 *
 * Many of these tests do not have their final values - this class is not yet fully implemented.
 */
public class AbstractBuildingEntityTest extends GameBoardTestCase {

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    /**
     * Provides different building types for parameterized tests.
     */
    static Stream<AbstractBuildingEntity> buildingProvider() {
        return Stream.of(
            createBuildingEntity(BuildingType.LIGHT),
            createBuildingEntity(BuildingType.MEDIUM),
            createBuildingEntity(BuildingType.HEAVY),
            createBuildingEntity(BuildingType.HARDENED)/*,
            createMobileStructure(BuildingType.LIGHT),
            createMobileStructure(BuildingType.MEDIUM),
            createMobileStructure(BuildingType.HEAVY),
            createMobileStructure(BuildingType.HARDENED)
            */
        );
    }

    private static AbstractBuildingEntity createBuildingEntity(BuildingType type) {
        AbstractBuildingEntity building = new BuildingEntity(type, 1);
        return setupBuilding(building);
    }

    /**
     * Not yet implemented
     */
    private static AbstractBuildingEntity createMobileStructure(BuildingType type) {
        AbstractBuildingEntity building = new MobileStructure(type, 1);
        return setupBuilding(building);
    }

    private static AbstractBuildingEntity setupBuilding(AbstractBuildingEntity building) {
        building.getInternalBuilding().setBuildingHeight(3);
        building.getInternalBuilding().addHex(new CubeCoords(0, 0, 0), 50, 10, BasementType.UNKNOWN, false);
        building.setPosition(new Coords(5, 5));
        return building;
    }

    @ParameterizedTest
    @MethodSource("buildingProvider")
    void testLocations(AbstractBuildingEntity building) {
        // 1 hex * 3 height = 3 locations
        assertEquals(3, building.locations());

        building.getInternalBuilding().addHex(new CubeCoords(1, -1, 0), 50, 10, BasementType.UNKNOWN, false);
        building.refreshAdditionalLocations();

        // 2 hexes * 3 height = 6 locations
        assertEquals(6, building.locations());
    }

    @ParameterizedTest
    @MethodSource("buildingProvider")
    void testCanChangeSecondaryFacing(AbstractBuildingEntity building) {
        assertFalse(building.canChangeSecondaryFacing());
    }

    @ParameterizedTest
    @MethodSource("buildingProvider")
    void testIsValidSecondaryFacing(AbstractBuildingEntity building) {
        for (int dir = 0; dir < 6; dir++) {
            assertFalse(building.isValidSecondaryFacing(dir));
        }
    }

    @ParameterizedTest
    @MethodSource("buildingProvider")
    void testClipSecondaryFacing(AbstractBuildingEntity building) {
        assertEquals(0, building.clipSecondaryFacing(0));
        assertEquals(0, building.clipSecondaryFacing(3));
    }

    @ParameterizedTest
    @MethodSource("buildingProvider")
    void testGetLocationNames(AbstractBuildingEntity building) {
        String[] names = building.getLocationNames();
        assertEquals(3, names.length);
        assertTrue(names[0].startsWith("Level"));
    }

    @ParameterizedTest
    @MethodSource("buildingProvider")
    void testGetLocationAbbreviations(AbstractBuildingEntity building) {
        String[] abbrs = building.getLocationAbbreviations();
        assertEquals(3, abbrs.length);
        assertTrue(abbrs[0].startsWith("LVL"));
    }

    @ParameterizedTest
    @MethodSource("buildingProvider")
    void testSideTable(AbstractBuildingEntity building) {
        assertEquals(ToHitData.SIDE_FRONT, building.sideTable(new Coords(3, 3)));
    }

    @ParameterizedTest
    @MethodSource("buildingProvider")
    void testRollHitLocation(AbstractBuildingEntity building) {
        HitData hit = building.rollHitLocation(ToHitData.HIT_NORMAL, ToHitData.SIDE_FRONT);
        assertNotNull(hit);
        assertEquals(0, hit.getLocation());
    }

    @ParameterizedTest
    @MethodSource("buildingProvider")
    void testGetTransferLocation(AbstractBuildingEntity building) {
        HitData hit = new HitData(0);
        assertEquals(hit, building.getTransferLocation(hit));
    }

    @ParameterizedTest
    @MethodSource("buildingProvider")
    void testGetWeaponArc(AbstractBuildingEntity building) {
        assertEquals(0, building.getWeaponArc(0));
    }

    @ParameterizedTest
    @MethodSource("buildingProvider")
    void testIsSecondaryArcWeapon(AbstractBuildingEntity building) {
        assertFalse(building.isSecondaryArcWeapon(0));
    }

    @ParameterizedTest
    @MethodSource("buildingProvider")
    void testGetNoOfSlots(AbstractBuildingEntity building) {
        int[] slots = building.getNoOfSlots();
        assertEquals(1, slots.length);
        assertEquals(100, slots[0]);
    }

    @ParameterizedTest
    @MethodSource("buildingProvider")
    void testAddEquipment(AbstractBuildingEntity building) throws Exception {
        WeaponMounted weapon = new WeaponMounted(building, new ISLaserMedium());
        building.addEquipment(weapon, 0, false);
        assertTrue(building.getEquipment().contains(weapon));
    }

    @ParameterizedTest
    @MethodSource("buildingProvider")
    void testVictoryReport(AbstractBuildingEntity building) {
        assertNull(building.victoryReport());
    }

    @ParameterizedTest
    @MethodSource("buildingProvider")
    void testAddEntityBonuses(AbstractBuildingEntity building) {
        PilotingRollData roll = new PilotingRollData(building.getId(), 0, "test");
        assertEquals(roll, building.addEntityBonuses(roll));
    }

    @ParameterizedTest
    @MethodSource("buildingProvider")
    void testIsRepairable(AbstractBuildingEntity building) {
        assertEquals(building.isSalvage(), building.isRepairable());
    }

    @ParameterizedTest
    @MethodSource("buildingProvider")
    void testIsTargetable(AbstractBuildingEntity building) {
        assertFalse(building.isTargetable());
    }

    @ParameterizedTest
    @MethodSource("buildingProvider")
    void testCanCharge(AbstractBuildingEntity building) {
        assertFalse(building.canCharge());
    }

    @ParameterizedTest
    @MethodSource("buildingProvider")
    void testCanFlee(AbstractBuildingEntity building) {
        assertFalse(building.canFlee(new Coords(0, 0)));
    }

    @ParameterizedTest
    @MethodSource("buildingProvider")
    void testCanGoDown(AbstractBuildingEntity building) {
        assertFalse(building.canGoDown());
        assertFalse(building.canGoDown(0, new Coords(0, 0), 0));
    }

    @ParameterizedTest
    @MethodSource("buildingProvider")
    void testGetCost(AbstractBuildingEntity building) {
        assertEquals(0.0, building.getCost(null, false));
    }

    @ParameterizedTest
    @MethodSource("buildingProvider")
    void testIsNuclearHardened(AbstractBuildingEntity building) {
        assertFalse(building.isNuclearHardened());
    }

    @ParameterizedTest
    @MethodSource("buildingProvider")
    void testDoomedInVacuum(AbstractBuildingEntity building) {
        assertFalse(building.doomedInVacuum());
    }

    @ParameterizedTest
    @MethodSource("buildingProvider")
    void testGetTotalCommGearTons(AbstractBuildingEntity building) {
        assertEquals(building.getExtraCommGearTons(), building.getTotalCommGearTons());
    }

    @ParameterizedTest
    @MethodSource("buildingProvider")
    void testGetEngineHits(AbstractBuildingEntity building) {
        assertEquals(0, building.getEngineHits());
    }

    @ParameterizedTest
    @MethodSource("buildingProvider")
    void testGetLocationDamage(AbstractBuildingEntity building) {
        assertEquals("", building.getLocationDamage(0));
    }

    @ParameterizedTest
    @MethodSource("buildingProvider")
    void testIsCrippled(AbstractBuildingEntity building) {
        assertFalse(building.isCrippled());
        assertFalse(building.isCrippled(true));
    }

    @ParameterizedTest
    @MethodSource("buildingProvider")
    void testIsDmgHeavy(AbstractBuildingEntity building) {
        assertFalse(building.isDmgHeavy());
    }

    @ParameterizedTest
    @MethodSource("buildingProvider")
    void testIsDmgModerate(AbstractBuildingEntity building) {
        assertFalse(building.isDmgModerate());
    }

    @ParameterizedTest
    @MethodSource("buildingProvider")
    void testIsDmgLight(AbstractBuildingEntity building) {
        assertFalse(building.isDmgLight());
    }


    @ParameterizedTest
    @MethodSource("buildingProvider")
    void testGetArmorType(AbstractBuildingEntity building) {
        assertTrue(building.getArmorType(0) >= 0);
    }

    @ParameterizedTest
    @MethodSource("buildingProvider")
    void testGetArmorTechLevel(AbstractBuildingEntity building) {
        assertTrue(building.getArmorTechLevel(0) >= 0);
    }

    @ParameterizedTest
    @MethodSource("buildingProvider")
    void testHasStealth(AbstractBuildingEntity building) {
        assertFalse(building.hasStealth());
    }

    @ParameterizedTest
    @MethodSource("buildingProvider")
    void testGetEntityType(AbstractBuildingEntity building) {
        assertEquals(BuildingEntity.ETYPE_BUILDING_ENTITY, building.getEntityType());
    }
}
