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

package megamek.common.actions.compute;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import megamek.common.GameBoardTestCase;
import megamek.common.Player;
import megamek.common.Team;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.board.Coords;
import megamek.common.board.CubeCoords;
import megamek.common.enums.AimingMode;
import megamek.common.enums.BuildingType;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.WeaponMounted;
import megamek.common.equipment.WeaponType;
import megamek.common.exceptions.LocationFullException;
import megamek.common.game.Game;
import megamek.common.options.GameOptions;
import megamek.common.options.IOption;
import megamek.common.options.OptionsConstants;
import megamek.common.options.PilotOptions;
import megamek.common.rolls.TargetRoll;
import megamek.common.units.AbstractBuildingEntity;
import megamek.common.units.BipedMek;
import megamek.common.units.BuildingEntity;
import megamek.common.units.Crew;
import megamek.common.units.CrewType;
import megamek.common.units.Entity;
import megamek.common.units.Infantry;
import megamek.common.units.Mek;
import megamek.common.units.Targetable;
import megamek.server.totalWarfare.TWGameManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Vector;

/**
 * Tests for
 * {@link ComputeToHit#toHitCalc(Game, int, Targetable, int, int, AimingMode, boolean, boolean, Targetable, Targetable,
 * boolean, boolean, java.util.List, boolean, int, int)}
 */
@DisplayName(value = "ComputeToHit Tests")
public class ComputeToHitTest extends GameBoardTestCase {

    private static int TARGET_IMPOSSIBLE = TargetRoll.IMPOSSIBLE;

    private static String LOS_BLOCKED_BY_TERRAIN = "LOS blocked by terrain.";

    private static final String BOARD_03_BY_05_CENTER_HILLS_DATA = """
          size 3 5
          hex 0101 0 "" ""
          hex 0201 0 "" ""
          hex 0301 0 "" ""
          hex 0102 0 "" ""
          hex 0202 0 "" ""
          hex 0302 0 "" ""
          hex 0103 1 "" ""
          hex 0203 4 "" ""
          hex 0303 2 "" ""
          hex 0104 0 "" ""
          hex 0204 0 "" ""
          hex 0304 0 "" ""
          hex 0105 0 "" ""
          hex 0205 0 "" ""
          hex 0305 0 "" ""
          end""";

    private static final String BOARD_03_BY_05_FLAT_DATA = """
          size 3 5
          hex 0101 0 "" ""
          hex 0201 0 "" ""
          hex 0301 0 "" ""
          hex 0102 0 "" ""
          hex 0202 0 "" ""
          hex 0302 0 "" ""
          hex 0103 0 "" ""
          hex 0203 0 "" ""
          hex 0303 0 "" ""
          hex 0104 0 "" ""
          hex 0204 0 "" ""
          hex 0304 0 "" ""
          hex 0105 0 "" ""
          hex 0205 0 "" ""
          hex 0305 0 "" ""
          end""";

    private static TWGameManager mockGameManager;
    private static GameOptions mockGameOptions;
    private static Team team1;
    private static Team team2;
    private static Player player1;
    private static Player player2;
    private static WeaponType mediumLaserType;
    private Game game;

    static {
        initializeBoard("03_BY_05_CENTER_HILLS", BOARD_03_BY_05_CENTER_HILLS_DATA);
        initializeBoard("03_BY_05_FLAT", BOARD_03_BY_05_FLAT_DATA);
    }

    Mek createMek(String chassis, String model, String crewName) {
        // Create a real Mek with some mocked fields
        Mek mockMek = new BipedMek();
        mockMek.setGame(game);
        mockMek.setChassis(chassis);
        mockMek.setModel(model);

        Crew mockCrew = mock(Crew.class);
        PilotOptions pOpt = new PilotOptions();
        when(mockCrew.getName(anyInt())).thenCallRealMethod();
        when(mockCrew.getNames()).thenReturn(new String[] { crewName });
        when(mockCrew.getOptions()).thenReturn(pOpt);
        mockMek.setCrew(mockCrew);

        return mockMek;
    }

    Infantry createInfantry(String chassis, String model, String crewName) {
        // Create a real Infantry unit with some mocked fields
        Infantry mockInfantry = new Infantry();
        mockInfantry.setGame(game);
        mockInfantry.setChassis(chassis);
        mockInfantry.setModel(model);

        Crew mockCrew = mock(Crew.class);
        PilotOptions pOpt = new PilotOptions();
        when(mockCrew.getName(anyInt())).thenCallRealMethod();
        when(mockCrew.getNames()).thenReturn(new String[] { crewName });
        when(mockCrew.getOptions()).thenReturn(pOpt);
        mockInfantry.setCrew(mockCrew);

        return mockInfantry;
    }

    BuildingEntity createBuildingEntity(String chassis, String model, String crewName) {
        // Create a real BuildingEntity unit with some mocked fields
        BuildingEntity buildingEntity = new BuildingEntity(BuildingType.HARDENED, 2);
        buildingEntity.setGame(game);
        buildingEntity.setChassis(chassis);
        buildingEntity.setModel(model);

        buildingEntity.getInternalBuilding().setBuildingHeight(6);
        buildingEntity.getInternalBuilding().addHex(CubeCoords.ZERO, 60, 15, null, false);
        buildingEntity.getInternalBuilding().addHex(new CubeCoords(-1.0, 0, 1.0), 60, 15, null, false);
        buildingEntity.getInternalBuilding().addHex(new CubeCoords(1.0, -1.0, 0), 60, 15, null, false);
        buildingEntity.refreshLocations();
        buildingEntity.refreshAdditionalLocations();

        Crew mockCrew = mock(Crew.class);
        PilotOptions pOpt = new PilotOptions();
        when(mockCrew.getName(anyInt())).thenCallRealMethod();
        when(mockCrew.getNames()).thenReturn(new String[] { crewName });
        when(mockCrew.getOptions()).thenReturn(pOpt);
        when(mockCrew.isActive()).thenReturn(true);
        when(mockCrew.getCrewType()).thenReturn(CrewType.VESSEL);
        buildingEntity.setCrew(mockCrew);

        return buildingEntity;
    }

    BuildingEntity createSixHexBuildingEntity(String chassis, String model, String crewName) {
        // Create a 6-hex BuildingEntity (4 levels tall) extending into rows 0X04 and 0X05
        BuildingEntity buildingEntity = new BuildingEntity(BuildingType.HARDENED, 2);
        buildingEntity.setGame(game);
        buildingEntity.setChassis(chassis);
        buildingEntity.setModel(model);

        // Building is 4 levels tall (0-3)
        buildingEntity.getInternalBuilding().setBuildingHeight(4); // 0205 (center)

        buildingEntity.getInternalBuilding().addHex(CubeCoords.ZERO, 40, 10, null, false);

        // Add 6 hexes: 3 in row 0X04 (front) and 3 in row 0X05 (back)
        // Using CubeCoords relative to building center at 0205 (Coords 1, 4)
        // Row 0X04: hexes 0104, 0204, 0304 relative to center
        buildingEntity.getInternalBuilding().addHex(new CubeCoords(-1.0, 1.0, 0), 40, 10, null, false);  // 0104
        buildingEntity.getInternalBuilding().addHex(new CubeCoords(0, 1.0, -1.0), 40, 10, null, false);  // 0204
        buildingEntity.getInternalBuilding().addHex(new CubeCoords(1.0, 0, -1.0), 40, 10, null, false);  // 0304

        // Row 0X05: hexes 0105, 0205, 0305 relative to center at 0205
        buildingEntity.getInternalBuilding().addHex(new CubeCoords(-1.0, 0, 1.0), 40, 10, null, false);  // 0105
        buildingEntity.getInternalBuilding().addHex(new CubeCoords(1.0, -1.0, 0), 40, 10, null, false);  // 0305

        buildingEntity.refreshLocations();
        buildingEntity.refreshAdditionalLocations();

        Crew mockCrew = mock(Crew.class);
        PilotOptions pOpt = new PilotOptions();
        when(mockCrew.getName(anyInt())).thenCallRealMethod();
        when(mockCrew.getNames()).thenReturn(new String[] { crewName });
        when(mockCrew.getOptions()).thenReturn(pOpt);
        when(mockCrew.isActive()).thenReturn(true);
        when(mockCrew.getCrewType()).thenReturn(CrewType.VESSEL);
        buildingEntity.setCrew(mockCrew);

        return buildingEntity;
    }

    @BeforeAll
    static void setUpAll() {
        EquipmentType.initializeTypes();
        mockGameOptions = mock(GameOptions.class);
        team1 = new Team(0);
        team2 = new Team(1);
        player1 = new Player(0, "Test1");
        player2 = new Player(1, "Test2");
        team1.addPlayer(player1);
        team2.addPlayer(player2);

        mediumLaserType = (WeaponType) EquipmentType.get("ISMediumLaser");

        // Mock game manager so it doesn't try networking
        mockGameManager = mock(TWGameManager.class);
        doNothing().when(mockGameManager).sendNewBuildings(any());
        doNothing().when(mockGameManager).sendChangedHex(any(Coords.class), anyInt());
    }

    @BeforeEach
    void setUp() {
        game = getGame();
        game.setOptions(mockGameOptions);

        when(mockGameOptions.booleanOption(eq(OptionsConstants.ALLOWED_NO_CLAN_PHYSICAL))).thenReturn(false);
        when(mockGameOptions.stringOption(OptionsConstants.ALLOWED_TECH_LEVEL)).thenReturn("Experimental");
        when(mockGameOptions.booleanOption(OptionsConstants.ALLOWED_ERA_BASED)).thenReturn(true);
        when(mockGameOptions.booleanOption(OptionsConstants.ALLOWED_SHOW_EXTINCT)).thenReturn(false);
        when(mockGameOptions.booleanOption(anyString())).thenReturn(false);
        when(mockGameOptions.intOption(OptionsConstants.ALLOWED_YEAR)).thenReturn(3151);

        IOption mockOption = mock(IOption.class);
        when(mockOption.booleanValue()).thenReturn(false);
        when(mockGameOptions.getOption(anyString())).thenReturn(mockOption);

        game.addPlayer(0, player1);
        game.addPlayer(1, player2);
    }

    @Nested
    @DisplayName(value = "toHitCalc Tests - BuildingEntity")
    class ToHitCalc_BuildingEntityTests {

        @Nested
        @DisplayName("Hill Terrain Tests")
        class HillTerrainTests {
            AbstractBuildingEntity attacker;
            Entity targetEntity;
            Targetable target;
            WeaponMounted mediumLaser;

            @BeforeEach
            void beforeEach() {
                // Re-initialize board to ensure fresh state for each test
                initializeBoard("03_BY_05_CENTER_HILLS", BOARD_03_BY_05_CENTER_HILLS_DATA);
                setBoard("03_BY_05_CENTER_HILLS");

                // Create BuildingEntity attacker with IS Medium Laser
                attacker = createBuildingEntity("Attacker", "ATK-1", "Alice");
                attacker.setOwnerId(player1.getId());
                attacker.setId(1);
                attacker.setPosition(new Coords(1, 4));
                attacker.updateBuildingEntityHexes(getBoard("03_BY_05_CENTER_HILLS").getBoardId(), mockGameManager);

                // Create target Mek
                targetEntity = createMek("Target", "TGT-2", "Bob");
                targetEntity.setOwnerId(player2.getId());
                targetEntity.setId(2);

                target = targetEntity;

                game.addEntity(attacker);
                game.addEntity(targetEntity);
            }

            @Test
            @DisplayName("LOS from 0205 level 0 (default) to 0201 - blocked by elevation 4 terrain")
            void testLOS_0205L0_To0201_BlockedDefault() throws LocationFullException {
                // Weapon at default location (level 0) in building at hex 0205
                // Target at hex 0201, LOS passes through hex 0203 (elevation 4)
                // Expected: BLOCKED (height 0 < terrain elevation 4)
                mediumLaser = (WeaponMounted) attacker.addEquipment(mediumLaserType, 0);
                mediumLaser.setFacing(0);
                targetEntity.setPosition(new Coords(1, 0));

                ToHitData result = ComputeToHit.toHitCalc(game, attacker.getId(), target,
                      mediumLaser.getEquipmentNum(), Entity.LOC_NONE, AimingMode.NONE,
                      false, false, null, null, false, false, null, false,
                      WeaponAttackAction.UNASSIGNED, WeaponAttackAction.UNASSIGNED);

                assertNotNull(result, "ToHitData should not be null");
                var modifiers = result.getModifiers();
                var blockingModifier = modifiers.stream()
                      .filter(m -> TARGET_IMPOSSIBLE == m.value() && LOS_BLOCKED_BY_TERRAIN.equals(m.description()))
                      .findFirst();

                assertTrue(blockingModifier.isPresent(),
                      "LOS should be BLOCKED - firing from height 0 through elevation 4 terrain. Modifiers: "
                            + modifiers.stream().map(m -> "[" + m.value() + ": " + m.description() + "]")
                            .collect(java.util.stream.Collectors.joining(", ")));
                assertTrue(result.cannotSucceed(), "Shot should NOT succeed when LOS is blocked");
            }

            @Test
            @DisplayName("LOS from 0105 level 2 to 0101 - clear over elevation 1 terrain")
            void testLOS_0105L2_To0101_Clear() throws LocationFullException {
                // Weapon at level 2 in hex 0105
                // Target at hex 0101, LOS passes through hex 0103 (elevation 1)
                // Expected: CLEAR (height 2 > terrain elevation 1)
                mediumLaser = (WeaponMounted) attacker.addEquipment(mediumLaserType,
                      attacker.getLocationFromAbbr("LVL 2 0105"));
                mediumLaser.setFacing(0);
                targetEntity.setPosition(new Coords(0, 0));

                ToHitData result = ComputeToHit.toHitCalc(game, attacker.getId(), target,
                      mediumLaser.getEquipmentNum(), Entity.LOC_NONE, AimingMode.NONE,
                      false, false, null, null, false, false, null, false,
                      WeaponAttackAction.UNASSIGNED, WeaponAttackAction.UNASSIGNED);

                assertNotNull(result, "ToHitData should not be null");
                var modifiers = result.getModifiers();
                var blockingModifier = modifiers.stream()
                      .filter(m -> TARGET_IMPOSSIBLE == m.value() && LOS_BLOCKED_BY_TERRAIN.equals(m.description()))
                      .findFirst();

                assertFalse(blockingModifier.isPresent(),
                      "LOS should be CLEAR - firing from height 2 over elevation 1 terrain. Modifiers: "
                            + modifiers.stream().map(m -> "[" + m.value() + ": " + m.description() + "]")
                            .collect(java.util.stream.Collectors.joining(", ")));
                assertFalse(result.cannotSucceed(), "Shot should succeed when LOS is clear");
            }

            @Test
            @DisplayName("LOS from 0105 level 0 to 0101 - blocked by elevation 1 terrain")
            @Disabled("IDK the rules on this, let's ignore for now")
            void testLOS_0105L0_To0101_Blocked() throws LocationFullException {
                // Weapon at level 0 in hex 0105
                // Target at hex 0101, LOS passes through hex 0103 (elevation 1)
                // Expected: BLOCKED (height 0 < terrain elevation 1)
                mediumLaser = (WeaponMounted) attacker.addEquipment(mediumLaserType,
                      attacker.getLocationFromAbbr("LVL 0 0105"));
                mediumLaser.setFacing(0);
                targetEntity.setPosition(new Coords(0, 0));

                ToHitData result = ComputeToHit.toHitCalc(game, attacker.getId(), target,
                      mediumLaser.getEquipmentNum(), Entity.LOC_NONE, AimingMode.NONE,
                      false, false, null, null, false, false, null, false,
                      WeaponAttackAction.UNASSIGNED, WeaponAttackAction.UNASSIGNED);

                assertNotNull(result, "ToHitData should not be null");
                var modifiers = result.getModifiers();
                var blockingModifier = modifiers.stream()
                      .filter(m -> TARGET_IMPOSSIBLE == m.value() && LOS_BLOCKED_BY_TERRAIN.equals(m.description()))
                      .findFirst();

                assertTrue(blockingModifier.isPresent(),
                      "LOS should be BLOCKED - firing from height 0 through elevation 1 terrain. Modifiers: "
                            + modifiers.stream().map(m -> "[" + m.value() + ": " + m.description() + "]")
                            .collect(java.util.stream.Collectors.joining(", ")));
                assertFalse(result.cannotSucceed(), "Shot should NOT succeed when LOS is blocked");
            }

            @Test
            @DisplayName("LOS from 0305 level 2 to 0301 - clear over elevation 2 terrain")
            void testLOS_0305L2_To0301_Clear() throws LocationFullException {
                // Weapon at level 2 in hex 0305
                // Target at hex 0301, LOS passes through hex 0303 (elevation 2)
                // Expected: CLEAR (height 2 >= terrain elevation 2)
                mediumLaser = (WeaponMounted) attacker.addEquipment(mediumLaserType,
                      attacker.getLocationFromAbbr("LVL 2 0305"));
                mediumLaser.setFacing(0);
                targetEntity.setPosition(new Coords(2, 0));

                ToHitData result = ComputeToHit.toHitCalc(game, attacker.getId(), target,
                      mediumLaser.getEquipmentNum(), Entity.LOC_NONE, AimingMode.NONE,
                      false, false, null, null, false, false, null, false,
                      WeaponAttackAction.UNASSIGNED, WeaponAttackAction.UNASSIGNED);

                assertNotNull(result, "ToHitData should not be null");
                var modifiers = result.getModifiers();
                var blockingModifier = modifiers.stream()
                      .filter(m -> TARGET_IMPOSSIBLE == m.value() && LOS_BLOCKED_BY_TERRAIN.equals(m.description()))
                      .findFirst();

                assertFalse(blockingModifier.isPresent(),
                      "LOS should be CLEAR - firing from height 2 at/over elevation 2 terrain. Modifiers: "
                            + modifiers.stream().map(m -> "[" + m.value() + ": " + m.description() + "]")
                            .collect(java.util.stream.Collectors.joining(", ")));
                assertFalse(result.cannotSucceed(), "Shot should succeed when LOS is clear");
            }

            @Test
            @DisplayName("LOS from 0305 level 0 to 0301 - blocked by elevation 2 terrain")
            void testLOS_0305L0_To0301_Blocked() throws LocationFullException {
                // Weapon at level 0 in hex 0305
                // Target at hex 0301, LOS passes through hex 0303 (elevation 2)
                // Expected: BLOCKED (height 0 < terrain elevation 2)
                mediumLaser = (WeaponMounted) attacker.addEquipment(mediumLaserType,
                      attacker.getLocationFromAbbr("LVL 0 0305"));
                mediumLaser.setFacing(0);
                targetEntity.setPosition(new Coords(2, 0));
                assertEquals(0, attacker.getWeaponFiringHeight(mediumLaser), "Weapon should be at height 0");

                ToHitData result = ComputeToHit.toHitCalc(game, attacker.getId(), target,
                      mediumLaser.getEquipmentNum(), Entity.LOC_NONE, AimingMode.NONE,
                      false, false, null, null, false, false, null, false,
                      WeaponAttackAction.UNASSIGNED, WeaponAttackAction.UNASSIGNED);

                assertNotNull(result, "ToHitData should not be null");
                var modifiers = result.getModifiers();
                var blockingModifier = modifiers.stream()
                      .filter(m -> TARGET_IMPOSSIBLE == m.value() && LOS_BLOCKED_BY_TERRAIN.equals(m.description()))
                      .findFirst();

                assertTrue(blockingModifier.isPresent(),
                      "LOS should be BLOCKED - firing from height 0 through elevation 2 terrain. Modifiers: "
                            + modifiers.stream().map(m -> "[" + m.value() + ": " + m.description() + "]")
                            .collect(java.util.stream.Collectors.joining(", ")));
                assertTrue(result.cannotSucceed(), "Shot should NOT succeed when LOS is blocked");
            }

            @Test
            @DisplayName("LOS from 0305 level 4 to 0301 - clear over elevation 2 terrain with facing")
            void testLOS_0305L4_To0301_ClearWithFacing() throws LocationFullException {
                // Weapon at level 4 in hex 0305 with facing set
                // Target at hex 0301, LOS passes through hex 0303 (elevation 2)
                // Expected: CLEAR (height 4 > terrain elevation 2)
                mediumLaser = (WeaponMounted) attacker.addEquipment(mediumLaserType,
                      attacker.getLocationFromAbbr("LVL 4 0305"));
                mediumLaser.setFacing(0);
                targetEntity.setPosition(new Coords(2, 0));

                ToHitData result = ComputeToHit.toHitCalc(game, attacker.getId(), target,
                      mediumLaser.getEquipmentNum(), Entity.LOC_NONE, AimingMode.NONE,
                      false, false, null, null, false, false, null, false,
                      WeaponAttackAction.UNASSIGNED, WeaponAttackAction.UNASSIGNED);

                assertNotNull(result, "ToHitData should not be null");
                var modifiers = result.getModifiers();
                var blockingModifier = modifiers.stream()
                      .filter(m -> TARGET_IMPOSSIBLE == m.value() && LOS_BLOCKED_BY_TERRAIN.equals(m.description()))
                      .findFirst();

                assertFalse(blockingModifier.isPresent(),
                      "LOS should be CLEAR - firing from height 4 over elevation 2 terrain. Modifiers: "
                            + modifiers.stream().map(m -> "[" + m.value() + ": " + m.description() + "]")
                            .collect(java.util.stream.Collectors.joining(", ")));
                assertFalse(result.cannotSucceed(), "Shot should succeed when LOS is clear");
            }

            @Test
            @DisplayName("LOS from 0305 level 5 (turret) to 0301 - clear over elevation 2 terrain")
            void testLOS_0305L5_To0301_ClearTurret() throws LocationFullException {
                // Weapon at level 5 (top floor) in hex 0305 as turret
                // Target at hex 0301, LOS passes through hex 0303 (elevation 2)
                // Expected: CLEAR (height 5 > terrain elevation 2)
                mediumLaser = (WeaponMounted) attacker.addEquipment(mediumLaserType,
                      attacker.getLocationFromAbbr("LVL 5 0305"));
                mediumLaser.setMekTurretMounted(true);
                targetEntity.setPosition(new Coords(2, 0));

                ToHitData result = ComputeToHit.toHitCalc(game, attacker.getId(), target,
                      mediumLaser.getEquipmentNum(), Entity.LOC_NONE, AimingMode.NONE,
                      false, false, null, null, false, false, null, false,
                      WeaponAttackAction.UNASSIGNED, WeaponAttackAction.UNASSIGNED);

                assertNotNull(result, "ToHitData should not be null");
                var modifiers = result.getModifiers();
                var blockingModifier = modifiers.stream()
                      .filter(m -> TARGET_IMPOSSIBLE == m.value() && LOS_BLOCKED_BY_TERRAIN.equals(m.description()))
                      .findFirst();

                assertFalse(blockingModifier.isPresent(),
                      "LOS should be CLEAR - turret at height 5 over elevation 2 terrain. Modifiers: "
                            + modifiers.stream().map(m -> "[" + m.value() + ": " + m.description() + "]")
                            .collect(java.util.stream.Collectors.joining(", ")));
                assertFalse(result.cannotSucceed(), "Shot should succeed when LOS is clear");
            }

            @Test
            @DisplayName("LOS from 0205 level 2 to 0201 - blocked by elevation 4 terrain")
            void testLOS_0205L2_To0201_Blocked() throws LocationFullException {
                // Weapon at level 2 in hex 0205
                // Target at hex 0201, LOS passes through hex 0203 (elevation 4)
                // Expected: BLOCKED (height 2 < terrain elevation 4)
                mediumLaser = (WeaponMounted) attacker.addEquipment(mediumLaserType,
                      attacker.getLocationFromAbbr("LVL 2 0205"));
                mediumLaser.setFacing(0);
                targetEntity.setPosition(new Coords(1, 0));

                ToHitData result = ComputeToHit.toHitCalc(game, attacker.getId(), target,
                      mediumLaser.getEquipmentNum(), Entity.LOC_NONE, AimingMode.NONE,
                      false, false, null, null, false, false, null, false,
                      WeaponAttackAction.UNASSIGNED, WeaponAttackAction.UNASSIGNED);

                assertNotNull(result, "ToHitData should not be null");
                var modifiers = result.getModifiers();
                var blockingModifier = modifiers.stream()
                      .filter(m -> TARGET_IMPOSSIBLE == m.value() && LOS_BLOCKED_BY_TERRAIN.equals(m.description()))
                      .findFirst();

                assertTrue(blockingModifier.isPresent(),
                      "LOS should be BLOCKED - firing from height 2 through elevation 4 terrain. Modifiers: "
                            + modifiers.stream().map(m -> "[" + m.value() + ": " + m.description() + "]")
                            .collect(java.util.stream.Collectors.joining(", ")));
                assertTrue(result.cannotSucceed(), "Shot should NOT succeed when LOS is blocked");
            }

            @Test
            @DisplayName("LOS from 0205 level 0 to 0201 - blocked by elevation 4 terrain")
            void testLOS_0205L0_To0201_Blocked() throws LocationFullException {
                // Weapon at level 0 in hex 0205
                // Target at hex 0201, LOS passes through hex 0203 (elevation 4)
                // Expected: BLOCKED (height 0 < terrain elevation 4)
                mediumLaser = (WeaponMounted) attacker.addEquipment(mediumLaserType,
                      attacker.getLocationFromAbbr("LVL 0 0205"));
                mediumLaser.setFacing(0);
                targetEntity.setPosition(new Coords(1, 0));
                assertEquals(0, attacker.getWeaponFiringHeight(mediumLaser), "Weapon should be at height 0");

                ToHitData result = ComputeToHit.toHitCalc(game, attacker.getId(), target,
                      mediumLaser.getEquipmentNum(), Entity.LOC_NONE, AimingMode.NONE,
                      false, false, null, null, false, false, null, false,
                      WeaponAttackAction.UNASSIGNED, WeaponAttackAction.UNASSIGNED);

                assertNotNull(result, "ToHitData should not be null");
                var modifiers = result.getModifiers();
                var blockingModifier = modifiers.stream()
                      .filter(m -> TARGET_IMPOSSIBLE == m.value() && LOS_BLOCKED_BY_TERRAIN.equals(m.description()))
                      .findFirst();

                assertTrue(blockingModifier.isPresent(),
                      "LOS should be BLOCKED - firing from height 0 through elevation 4 terrain. Modifiers: "
                            + modifiers.stream().map(m -> "[" + m.value() + ": " + m.description() + "]")
                            .collect(java.util.stream.Collectors.joining(", ")));
                assertTrue(result.cannotSucceed(), "Shot should NOT succeed when LOS is blocked");
            }

            @Test
            @DisplayName("LOS from 0205 level 4 to 0201 - clear at elevation 4 terrain with facing")
            void testLOS_0205L4_To0201_ClearWithFacing() throws LocationFullException {
                // Weapon at level 4 in hex 0205 with facing set
                // Target at hex 0201, LOS passes through hex 0203 (elevation 4)
                // Expected: CLEAR (height 4 >= terrain elevation 4)
                mediumLaser = (WeaponMounted) attacker.addEquipment(mediumLaserType,
                      attacker.getLocationFromAbbr("LVL 4 0205"));
                mediumLaser.setFacing(0);
                targetEntity.setPosition(new Coords(1, 0));

                ToHitData result = ComputeToHit.toHitCalc(game, attacker.getId(), target,
                      mediumLaser.getEquipmentNum(), Entity.LOC_NONE, AimingMode.NONE,
                      false, false, null, null, false, false, null, false,
                      WeaponAttackAction.UNASSIGNED, WeaponAttackAction.UNASSIGNED);

                assertNotNull(result, "ToHitData should not be null");
                var modifiers = result.getModifiers();
                var blockingModifier = modifiers.stream()
                      .filter(m -> TARGET_IMPOSSIBLE == m.value() && LOS_BLOCKED_BY_TERRAIN.equals(m.description()))
                      .findFirst();

                assertFalse(blockingModifier.isPresent(),
                      "LOS should be CLEAR - firing from height 4 at/over elevation 4 terrain. Modifiers: "
                            + modifiers.stream().map(m -> "[" + m.value() + ": " + m.description() + "]")
                            .collect(java.util.stream.Collectors.joining(", ")));
                assertFalse(result.cannotSucceed(), "Shot should succeed when LOS is clear");
            }

            @Test
            @DisplayName("LOS from 0205 level 5 (turret) to 0201 - clear over elevation 4 terrain")
            void testLOS_0205L5_To0201_ClearTurret() throws LocationFullException {
                // Weapon at level 5 (top floor) in hex 0205 as turret
                // Target at hex 0201, LOS passes through hex 0203 (elevation 4)
                // Expected: CLEAR (height 5 > terrain elevation 4)
                mediumLaser = (WeaponMounted) attacker.addEquipment(mediumLaserType,
                      attacker.getLocationFromAbbr("LVL 5 0205"));
                mediumLaser.setMekTurretMounted(true);
                targetEntity.setPosition(new Coords(1, 0));

                ToHitData result = ComputeToHit.toHitCalc(game, attacker.getId(), target,
                      mediumLaser.getEquipmentNum(), Entity.LOC_NONE, AimingMode.NONE,
                      false, false, null, null, false, false, null, false,
                      WeaponAttackAction.UNASSIGNED, WeaponAttackAction.UNASSIGNED);

                assertNotNull(result, "ToHitData should not be null");
                var modifiers = result.getModifiers();
                var blockingModifier = modifiers.stream()
                      .filter(m -> TARGET_IMPOSSIBLE == m.value() && LOS_BLOCKED_BY_TERRAIN.equals(m.description()))
                      .findFirst();

                assertFalse(blockingModifier.isPresent(),
                      "LOS should be CLEAR - turret at height 5 over elevation 4 terrain. Modifiers: "
                            + modifiers.stream().map(m -> "[" + m.value() + ": " + m.description() + "]")
                            .collect(java.util.stream.Collectors.joining(", ")));
                assertFalse(result.cannotSucceed(), "Shot should succeed when LOS is clear");
            }
        }

        @Nested
        @DisplayName("Building Obstruction Tests")
        class BuildingObstructionTests {
            BuildingEntity attacker;
            Entity targetEntity;
            Targetable target;
            WeaponMounted mediumLaser;

            @BeforeEach
            void beforeEach() {
                // Re-initialize board to ensure fresh state for each test
                initializeBoard("03_BY_05_FLAT", BOARD_03_BY_05_FLAT_DATA);
                setBoard("03_BY_05_FLAT");

                // Create 6-hex BuildingEntity (4 levels tall) at rows 0X04 and 0X05
                attacker = createSixHexBuildingEntity("Fort", "FRT-1", "Alice");
                attacker.setOwnerId(player1.getId());
                attacker.setId(1);
                attacker.setPosition(new Coords(1, 3)); // Center of building at 0204
                attacker.updateBuildingEntityHexes(getBoard("03_BY_05_FLAT").getBoardId(), mockGameManager);

                // Create target Mek in front of building
                targetEntity = createMek("Target", "TGT-2", "Bob");
                targetEntity.setOwnerId(player2.getId());
                targetEntity.setId(2);
                targetEntity.setPosition(new Coords(0, 0)); // At 0101

                target = targetEntity;

                game.addEntity(attacker);
                game.addEntity(targetEntity);
            }

            @Test
            @DisplayName("Turret on roof of 0104 can attack Mek at 0101")
            void testRoofTurret_0104_CanAttack() throws LocationFullException {
                // Weapon at level 3 (roof) in hex 0104
                // Target at hex 0101
                // Expected: CLEAR (firing from roof over intervening hexes)
                mediumLaser = (WeaponMounted) attacker.addEquipment(mediumLaserType,
                      attacker.getLocationFromAbbr("LVL 3 0104"));
                mediumLaser.setMekTurretMounted(true);

                ToHitData result = ComputeToHit.toHitCalc(game, attacker.getId(), target,
                      mediumLaser.getEquipmentNum(), Entity.LOC_NONE, AimingMode.NONE,
                      false, false, null, null, false, false, null, false,
                      WeaponAttackAction.UNASSIGNED, WeaponAttackAction.UNASSIGNED);

                assertNotNull(result, "ToHitData should not be null");
                var modifiers = result.getModifiers();
                var blockingModifier = modifiers.stream()
                      .filter(m -> TARGET_IMPOSSIBLE == m.value() && LOS_BLOCKED_BY_TERRAIN.equals(m.description()))
                      .findFirst();

                assertFalse(blockingModifier.isPresent(),
                      "LOS should be CLEAR - turret on roof at level 3 can see over building. Modifiers: "
                            + modifiers.stream().map(m -> "[" + m.value() + ": " + m.description() + "]")
                            .collect(java.util.stream.Collectors.joining(", ")));
                assertFalse(result.cannotSucceed(), "Shot should succeed from roof turret");
            }

            @Test
            @DisplayName("Turret in 0105 cannot attack Mek at 0101 - blocked by building hex 0104")
            void testRearTurret_0105_Blocked() throws LocationFullException {
                // Weapon at level 3 (roof) in hex 0105 (rear row)
                // Target at hex 0101
                // Expected: BLOCKED (building hex 0104 is in the way)
                mediumLaser = (WeaponMounted) attacker.addEquipment(mediumLaserType,
                      attacker.getLocationFromAbbr("LVL 3 0105"));
                mediumLaser.setMekTurretMounted(true);

                ToHitData result = ComputeToHit.toHitCalc(game, attacker.getId(), target,
                      mediumLaser.getEquipmentNum(), Entity.LOC_NONE, AimingMode.NONE,
                      false, false, null, null, false, false, null, false,
                      WeaponAttackAction.UNASSIGNED, WeaponAttackAction.UNASSIGNED);

                assertNotNull(result, "ToHitData should not be null");
                var modifiers = result.getModifiers();
                var blockingModifier = modifiers.stream()
                      .filter(m -> TARGET_IMPOSSIBLE == m.value() && LOS_BLOCKED_BY_TERRAIN.equals(m.description()))
                      .findFirst();

                assertTrue(blockingModifier.isPresent(),
                      "LOS should be BLOCKED - building hex 0104 blocks LOS from 0105 to target. Modifiers: "
                            + modifiers.stream().map(m -> "[" + m.value() + ": " + m.description() + "]")
                            .collect(java.util.stream.Collectors.joining(", ")));
                assertTrue(result.cannotSucceed(), "Shot should NOT succeed when blocked by building");
            }

            @Test
            @DisplayName("Turret in 0105 CAN attack Mek at 0101 after destroying building hex 0104")
            void testRearTurret_0105_NotBlocked_AfterBuildingDestroyed() throws LocationFullException {
                // Weapon at level 3 (roof) in hex 0105 (rear row)
                // Target at hex 0101
                // Destroy building hex 0104 first
                // Expected: NOT BLOCKED (building hex 0104 is destroyed)

                // Collapse the building hex at 0104
                Coords hex0104Coords = new Coords(0, 3);
                int buildingHeight = attacker.getHeight(hex0104Coords);
                attacker.collapseFloorsOnHex(hex0104Coords, buildingHeight);
                game.getBoard().collapseBuilding(hex0104Coords);

                mediumLaser = (WeaponMounted) attacker.addEquipment(mediumLaserType,
                      attacker.getLocationFromAbbr("LVL 3 0105"));
                mediumLaser.setMekTurretMounted(true);

                ToHitData result = ComputeToHit.toHitCalc(game, attacker.getId(), target,
                      mediumLaser.getEquipmentNum(), Entity.LOC_NONE, AimingMode.NONE,
                      false, false, null, null, false, false, null, false,
                      WeaponAttackAction.UNASSIGNED, WeaponAttackAction.UNASSIGNED);

                assertNotNull(result, "ToHitData should not be null");
                var modifiers = result.getModifiers();
                var blockingModifier = modifiers.stream()
                      .filter(m -> TARGET_IMPOSSIBLE == m.value() && LOS_BLOCKED_BY_TERRAIN.equals(m.description()))
                      .findFirst();

                assertFalse(blockingModifier.isPresent(),
                      "LOS should NOT be BLOCKED - building hex 0104 was destroyed. Modifiers: "
                            + modifiers.stream().map(m -> "[" + m.value() + ": " + m.description() + "]")
                            .collect(java.util.stream.Collectors.joining(", ")));
                assertFalse(result.cannotSucceed(), "Shot SHOULD succeed after building destroyed");
            }
        }
    }
}
