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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;

import java.util.Vector;
import java.util.stream.Stream;

import megamek.common.board.Coords;
import megamek.common.board.CubeCoords;
import megamek.common.enums.BasementType;
import megamek.common.enums.BuildingType;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.WeaponMounted;
import megamek.common.game.Game;
import megamek.common.net.packets.Packet;
import megamek.common.rolls.PilotingRollData;
import megamek.common.units.Building;
import megamek.common.units.AbstractBuildingEntity;
import megamek.common.units.BuildingEntity;
import megamek.common.units.MobileStructure;
import megamek.common.weapons.lasers.innerSphere.medium.ISLaserMedium;
import megamek.server.totalWarfare.TWGameManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

/**
 * Tests for {@link AbstractBuildingEntity} that aren't tested by {@link IBuildingTests}. If the method is from the
 * {@link Building} interface, the test should probably be in {@code IBuildingTests}.
 *
 * Many of these tests do not have their final values - this class is not yet fully implemented. Many of these
 * method's'll be removed from this class as they're overriden.
 */
public class AbstractBuildingEntityTest extends GameBoardTestCase {

    TWGameManager gameManager;
    private Game game;

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void beforeEach() {
        Player player = new Player(0, "Test");
        gameManager = Mockito.spy(new TWGameManager());

        // Mock methods that require Server to avoid NullPointerException
        Mockito.doNothing().when(gameManager).send(any(Packet.class));
        Mockito.doNothing().when(gameManager).sendChangedHex(any(Coords.class), any(int.class));
        Mockito.doNothing().when(gameManager).entityUpdate(any(int.class));
        Mockito.doNothing().when(gameManager).sendChangedBuildings(any());

        game = gameManager.getGame();
        game.addPlayer(0, player);
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

    private static AbstractBuildingEntity setupBuilding(
          AbstractBuildingEntity building) {        // Initialize a test board for BuildingEntity tests
        building.getInternalBuilding().setBuildingHeight(3);
        building.getInternalBuilding().addHex(new CubeCoords(0, 0, 0), 50, 10, BasementType.UNKNOWN, false);
        return building;
    }

    @ParameterizedTest
    @MethodSource("buildingProvider")
    void testLocations(AbstractBuildingEntity building) {
        initializeBuildingOnBoard(building);
        // 1 hex * 3 height = 3 locations
        assertEquals(3, building.locations());

        building.getInternalBuilding().addHex(new CubeCoords(1, -1, 0), 50, 10, BasementType.UNKNOWN, false);
        building.refreshLocations();
        building.refreshAdditionalLocations();

        // 2 hexes * 3 height = 6 locations
        assertEquals(6, building.locations());
    }

    private void initializeBuildingOnBoard(AbstractBuildingEntity building) {
        initializeBoard("BUILDING_ENTITY_TEST_BOARD", """
              size 16 17
              hex 0101 0 "" ""
              hex 0505 0 "" ""
              hex 0506 0 "" ""
              end"""
        );
        building.setOwner(game.getPlayer(0));
        building.refreshLocations();
        building.refreshAdditionalLocations();
        building.setId(0);
        game.addEntity(building);
        building.setPosition(new Coords(5, 5));
        building.updateBuildingEntityHexes(getBoard("BUILDING_ENTITY_TEST_BOARD").getBoardId(), gameManager);

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
        initializeBuildingOnBoard(building);
        String[] names = building.getLocationNames();
        assertEquals(3, names.length);
        assertTrue(names[0].startsWith("Level"));
    }

    @ParameterizedTest
    @MethodSource("buildingProvider")
    void testGetLocationAbbreviations(AbstractBuildingEntity building) {
        initializeBuildingOnBoard(building);
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
        // Arrange
        initializeBuildingOnBoard(building);

        // Act
        Vector<Report> victoryReports = building.victoryReport();

        // Assert
        assertNotNull(victoryReports);
        assertEquals(4, victoryReports.size());
    }

    @ParameterizedTest
    @MethodSource("buildingProvider")
    void testAddEntityBonuses(AbstractBuildingEntity building) {
        PilotingRollData roll = new PilotingRollData(building.getId(), 0, "test");
        assertEquals(roll, building.addEntityBonuses(roll));
    }

    @ParameterizedTest
    @MethodSource("buildingProvider")
    void testIsRepairable_WithStructure(AbstractBuildingEntity building) {
        initializeBuildingOnBoard(building);
        // Building with structure is repairable
        assertTrue(building.isRepairable());
    }

    @ParameterizedTest
    @MethodSource("buildingProvider")
    void testIsRepairable_CompletelyCollapsed(AbstractBuildingEntity building) {
        initializeBuildingOnBoard(building);
        // Destroy all structure (completely collapsed)
        Coords buildingCoords = building.getPosition();
        building.setCurrentCF(0, buildingCoords);

        // Completely collapsed building is not repairable
        assertFalse(building.isRepairable());
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
    void testIsCrippled_NotCrippled(AbstractBuildingEntity building) {
        // Pristine building with no weapons is not crippled
        assertFalse(building.isCrippled());
        assertFalse(building.isCrippled(true));
    }

    @ParameterizedTest
    @MethodSource("buildingProvider")
    void testIsCrippled_WithDisabledWeapons(AbstractBuildingEntity building) throws Exception {
        initializeBuildingOnBoard(building);

        // Add a weapon
        WeaponMounted weapon = new WeaponMounted(building, new ISLaserMedium());
        building.addEquipment(weapon, 0, false);

        // Initialize as military building (has weapons)
        building.initMilitary();

        // Building with operational weapon is not crippled
        assertFalse(building.isCrippled());

        // Disable the weapon
        weapon.setDestroyed(true);

        // Military building with all weapons disabled is crippled
        assertTrue(building.isCrippled());
    }

    @ParameterizedTest
    @MethodSource("buildingProvider")
    void testIsDmgHeavy_NotHeavyDamage(AbstractBuildingEntity building) {
        // Pristine building is not heavily damaged
        assertFalse(building.isDmgHeavy());
    }

    @ParameterizedTest
    @MethodSource("buildingProvider")
    void testIsDmgHeavy_WithHeavyDamage(AbstractBuildingEntity building) {
        initializeBuildingOnBoard(building);

        // Building starts with CF=50 per hex, 1 hex
        // Heavy damage is <= 50% of original, so <= 25 CF
        Coords buildingCoords = building.getPosition();

        // Damage to 25 CF (exactly 50%) - should be heavy damage
        building.setCurrentCF(25, buildingCoords);
        assertTrue(building.isDmgHeavy());

        // Damage to 20 CF (40% - less than 50%) - should still be heavy damage
        building.setCurrentCF(20, buildingCoords);
        assertTrue(building.isDmgHeavy());

        // Restore to 26 CF (52% - more than 50%) - should not be heavy damage
        building.setCurrentCF(26, buildingCoords);
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
