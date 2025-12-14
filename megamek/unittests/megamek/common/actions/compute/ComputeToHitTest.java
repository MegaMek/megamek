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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
import megamek.common.units.BipedMek;
import megamek.common.units.BuildingEntity;
import megamek.common.units.Crew;
import megamek.common.units.CrewType;
import megamek.common.units.Entity;
import megamek.common.units.Infantry;
import megamek.common.units.Mek;
import megamek.common.units.Targetable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ComputeToHit#toHitCalc(Game, int, Targetable, int, int, AimingMode, boolean, boolean, Targetable, Targetable, boolean, boolean, java.util.List, boolean, int, int)}
 */
@DisplayName(value = "ComputeToHit Tests")
public class ComputeToHitTest extends GameBoardTestCase {

    private static int TARGET_IMPOSSIBLE = TargetRoll.IMPOSSIBLE;

    private static String LOS_BLOCKED_BY_TERRAIN = "LOS blocked by terrain.";

    private static GameOptions mockGameOptions;
    private static Team team1;
    private static Team team2;
    private static Player player1;
    private static Player player2;
    private static WeaponType mediumLaserType;
    private Game game;

    static {
        initializeBoard("03_BY_05_CENTER_HILLS", """
              size 3 5
              hex 0101 0 "" ""
              hex 0102 0 "" ""
              hex 0103 1 "" ""
              hex 0104 0 "" ""
              hex 0105 0 "" ""
              hex 0201 0 "" ""
              hex 0202 0 "" ""
              hex 0203 4 "" ""
              hex 0204 0 "" ""
              hex 0205 0 "" ""
              hex 0301 0 "" ""
              hex 0302 0 "" ""
              hex 0303 3 "" ""
              hex 0304 0 "" ""
              hex 0305 0 "" ""
              end""");
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
        Entity attacker;
        Entity targetEntity;
        Targetable target;
        WeaponMounted mediumLaser;

        @BeforeEach
        void beforeEach() {
            setBoard("03_BY_05_CENTER_HILLS");

            // Create BuildingEntity attacker with IS Medium Laser
            attacker = createBuildingEntity("Attacker", "ATK-1", "Alice");
            attacker.setOwnerId(player1.getId());
            attacker.setId(1);
            attacker.setPosition(new Coords(1, 4));



            // Create target Mek
            targetEntity = createMek("Target", "TGT-2", "Bob");
            targetEntity.setOwnerId(player2.getId());
            targetEntity.setId(2);


            target = targetEntity;

            game.addEntity(attacker);
            game.addEntity(targetEntity);
        }

        @Test
        @DisplayName(value = "should calculate to-hit for BuildingEntity firing at Mek")
        void shouldCalculateToHit_ForBuildingEntityFiringAtMekBehindTallHill() throws LocationFullException {
            // Add IS Medium Laser to location 0
            mediumLaser = (WeaponMounted) attacker.addEquipment(mediumLaserType, 0);
            mediumLaser.setFacing(0);

            targetEntity.setPosition(new Coords(1, 0));

            // Act
            ToHitData result = ComputeToHit.toHitCalc(
                  game,
                  attacker.getId(),
                  target,
                  mediumLaser.getEquipmentNum(),
                  Entity.LOC_NONE,
                  AimingMode.NONE,
                  false,
                  false,
                  null,
                  null,
                  false,
                  false,
                  null,
                  false,
                  WeaponAttackAction.UNASSIGNED,
                  WeaponAttackAction.UNASSIGNED
            );

            if (result != null) {

            }

            // Assert
            assertNotNull(result, "ToHitData should not be null");

            // Break down the assertion for better debugging
            var modifiers = result.getModifiers();
            var matchingModifier = modifiers.stream()
                  .filter(m -> TARGET_IMPOSSIBLE == m.value() && LOS_BLOCKED_BY_TERRAIN.equals(m.description()))
                  .findFirst();

            assertFalse(modifiers.isEmpty(), "Modifiers list should not be empty");
            assertTrue(matchingModifier.isPresent(),
                  "Should have modifier with value="
                        + TARGET_IMPOSSIBLE
                        + " and description='"
                        + LOS_BLOCKED_BY_TERRAIN
                        + "'. Actual modifiers: "
                        + modifiers.stream()
                        .map(m -> "[value=" + m.value() + ", desc='" + m.description() + "']")
                        .collect(java.util.stream.Collectors.joining(", ")));
            assertTrue(result.cannotSucceed(), "Result should not able to succeed.");
        }

        @Test
        @DisplayName(value = "should calculate to-hit for BuildingEntity firing at Mek")
        void shouldCalculateToHit_ForBuildingEntityFiringAtMekBehindShortHill() throws LocationFullException {
            // Add IS Medium Laser to location 0
            mediumLaser = (WeaponMounted) attacker.addEquipment(mediumLaserType,
                  attacker.getLocationFromAbbr("LVL 2 0105"));
            mediumLaser.setFacing(0);

            targetEntity.setPosition(new Coords(0, 0));

            // Act
            ToHitData result = ComputeToHit.toHitCalc(
                  game,
                  attacker.getId(),
                  target,
                  mediumLaser.getEquipmentNum(),
                  Entity.LOC_NONE,
                  AimingMode.NONE,
                  false,
                  false,
                  null,
                  null,
                  false,
                  false,
                  null,
                  false,
                  WeaponAttackAction.UNASSIGNED,
                  WeaponAttackAction.UNASSIGNED
            );

            // Assert
            assertNotNull(result, "ToHitData should not be null");

            // Break down the assertion for better debugging
            var modifiers = result.getModifiers();
            var matchingModifier = modifiers.stream()
                  .filter(m -> TARGET_IMPOSSIBLE == m.value() && LOS_BLOCKED_BY_TERRAIN.equals(m.description()))
                  .findFirst();

            assertFalse(modifiers.isEmpty(), "Modifiers list should not be empty");
            assertFalse(matchingModifier.isPresent(),
                  "Should have modifier with value="
                        + TARGET_IMPOSSIBLE
                        + " and description='"
                        + LOS_BLOCKED_BY_TERRAIN
                        + "'. Actual modifiers: "
                        + modifiers.stream()
                        .map(m -> "[value=" + m.value() + ", desc='" + m.description() + "']")
                        .collect(java.util.stream.Collectors.joining(", ")));
            assertFalse(result.cannotSucceed(), "Result should be able to succeed.");
        }

        @Test
        @DisplayName(value = "should calculate to-hit for BuildingEntity firing at Mek")
        @Disabled ( value = "IDK the rules on this, let's ignore for now")
        void shouldCalculateToHit_ForBuildingEntityFiringAtMekBehindShortHillButLowGun() throws LocationFullException {
            // Add IS Medium Laser to location 0
            mediumLaser = (WeaponMounted) attacker.addEquipment(mediumLaserType,
                  attacker.getLocationFromAbbr("LVL 0 0105"));
            mediumLaser.setFacing(0);

            targetEntity.setPosition(new Coords(0, 0));

            // Act
            ToHitData result = ComputeToHit.toHitCalc(
                  game,
                  attacker.getId(),
                  target,
                  mediumLaser.getEquipmentNum(),
                  Entity.LOC_NONE,
                  AimingMode.NONE,
                  false,
                  false,
                  null,
                  null,
                  false,
                  false,
                  null,
                  false,
                  WeaponAttackAction.UNASSIGNED,
                  WeaponAttackAction.UNASSIGNED
            );

            // Assert
            assertNotNull(result, "ToHitData should not be null");

            // Break down the assertion for better debugging
            var modifiers = result.getModifiers();
            var matchingModifier = modifiers.stream()
                  .filter(m -> TARGET_IMPOSSIBLE == m.value() && LOS_BLOCKED_BY_TERRAIN.equals(m.description()))
                  .findFirst();

            assertFalse(modifiers.isEmpty(), "Modifiers list should not be empty");
            assertTrue(matchingModifier.isPresent(),
                  "Should have modifier with value="
                        + TARGET_IMPOSSIBLE
                        + " and description='"
                        + LOS_BLOCKED_BY_TERRAIN
                        + "'. Actual modifiers: "
                        + modifiers.stream()
                        .map(m -> "[value=" + m.value() + ", desc='" + m.description() + "']")
                        .collect(java.util.stream.Collectors.joining(", ")));
            assertFalse(result.cannotSucceed(), "Result should be able to succeed.");
        }

        @Test
        @DisplayName(value = "should calculate to-hit for BuildingEntity firing at Mek")
        void shouldCalculateToHit_ForBuildingEntityFiringAtMekBehindMekHeightHill() throws LocationFullException {
            // Add IS Medium Laser to location 0
            mediumLaser = (WeaponMounted) attacker.addEquipment(mediumLaserType,
                  attacker.getLocationFromAbbr("LVL 2 0305"));
            mediumLaser.setFacing(0);

            targetEntity.setPosition(new Coords(2, 0));

            // Act
            ToHitData result = ComputeToHit.toHitCalc(
                  game,
                  attacker.getId(),
                  target,
                  mediumLaser.getEquipmentNum(),
                  Entity.LOC_NONE,
                  AimingMode.NONE,
                  false,
                  false,
                  null,
                  null,
                  false,
                  false,
                  null,
                  false,
                  WeaponAttackAction.UNASSIGNED,
                  WeaponAttackAction.UNASSIGNED
            );

            // Assert
            assertNotNull(result, "ToHitData should not be null");

            // Break down the assertion for better debugging
            var modifiers = result.getModifiers();
            var matchingModifier = modifiers.stream()
                  .filter(m -> TARGET_IMPOSSIBLE == m.value() && LOS_BLOCKED_BY_TERRAIN.equals(m.description()))
                  .findFirst();

            assertFalse(modifiers.isEmpty(), "Modifiers list should not be empty");
            assertFalse(matchingModifier.isPresent(),
                  "Should have modifier with value="
                        + TARGET_IMPOSSIBLE
                        + " and description='"
                        + LOS_BLOCKED_BY_TERRAIN
                        + "'. Actual modifiers: "
                        + modifiers.stream()
                        .map(m -> "[value=" + m.value() + ", desc='" + m.description() + "']")
                        .collect(java.util.stream.Collectors.joining(", ")));
            assertFalse(result.cannotSucceed(), "Result should be able to succeed.");
        }

        @Test
        @DisplayName(value = "should calculate to-hit for BuildingEntity firing at Mek")
        @Disabled
        void shouldCalculateToHit_ForBuildingEntityFiringAtMekBehindMekHeightButLowGun() throws LocationFullException {
            // Add IS Medium Laser to location 0
            mediumLaser = (WeaponMounted) attacker.addEquipment(mediumLaserType,
                  attacker.getLocationFromAbbr("LVL 0 0305"));
            mediumLaser.setFacing(0);

            targetEntity.setPosition(new Coords(2, 0));

            // Verify weapon is at level 0
            assertEquals(0, attacker.getWeaponFiringHeight(mediumLaser), "Weapon should be at height 0 for level 0");

            // Act
            ToHitData result = ComputeToHit.toHitCalc(
                  game,
                  attacker.getId(),
                  target,
                  mediumLaser.getEquipmentNum(),
                  Entity.LOC_NONE,
                  AimingMode.NONE,
                  false,
                  false,
                  null,
                  null,
                  false,
                  false,
                  null,
                  false,
                  WeaponAttackAction.UNASSIGNED,
                  WeaponAttackAction.UNASSIGNED
            );

            // Assert
            assertNotNull(result, "ToHitData should not be null");

            // Break down the assertion for better debugging
            var modifiers = result.getModifiers();
            var matchingModifier = modifiers.stream()
                  .filter(m -> TARGET_IMPOSSIBLE == m.value() && LOS_BLOCKED_BY_TERRAIN.equals(m.description()))
                  .findFirst();

            assertFalse(modifiers.isEmpty(), "Modifiers list should not be empty");
            assertTrue(matchingModifier.isPresent(),
                  "Should have modifier with value="
                        + TARGET_IMPOSSIBLE
                        + " and description='"
                        + LOS_BLOCKED_BY_TERRAIN
                        + "'. Actual modifiers: "
                        + modifiers.stream()
                        .map(m -> "[value=" + m.value() + ", desc='" + m.description() + "']")
                        .collect(java.util.stream.Collectors.joining(", ")));
            assertTrue(result.cannotSucceed(), "Result should NOT be able to succeed.");
        }
    }
}
