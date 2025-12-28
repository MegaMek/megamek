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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import megamek.common.board.Coords;
import megamek.common.board.CubeCoords;
import megamek.common.enums.BuildingType;
import megamek.common.game.Game;
import megamek.common.options.GameOptions;
import megamek.common.options.IOption;
import megamek.common.options.OptionsConstants;
import megamek.common.options.PilotOptions;
import megamek.common.units.BipedMek;
import megamek.common.units.BuildingEntity;
import megamek.common.units.Crew;
import megamek.common.units.CrewType;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName(value = "LosEffects Unit Tests")
public class LosEffectsTest extends GameBoardTestCase {

    private Game game;
    private Player player;

    static {
        initializeBoard("01_BY_05_NO_OBSTRUCTIONS", """
              size 1 5
              hex 0101 0 "" ""
              hex 0102 0 "" ""
              hex 0103 0 "" ""
              hex 0104 0 "" ""
              hex 0105 0 "" ""
              end"""
        );

        initializeBoard("03_BY_05_CENTER_HILLS", """
              size 3 5
              hex 0101 0 "" ""
              hex 0201 0 "" ""
              hex 0301 0 "" ""
              hex 0102 0 "" ""
              hex 0202 0 "" ""
              hex 0302 0 "" ""
              hex 0103 1 "" ""
              hex 0203 4 "" ""
              hex 0303 3 "" ""
              hex 0104 0 "" ""
              hex 0204 0 "" ""
              hex 0304 0 "" ""
              hex 0105 0 "" ""
              hex 0205 0 "" ""
              hex 0305 0 "" ""
              end"""
        );
    }

    @BeforeEach
    void beforeEach() {
        player = new Player(0, "Test Player");
        game = getGame();
        game.addPlayer(0, player);
    }

    /**
     * Tests for {@link LosEffects#calculateLos(Game, LosEffects.AttackInfo)}
     */
    @Nested
    @DisplayName(value = "calculateLos Tests")
    class CalculateLosTests {

        @BeforeEach
        void setUp() {
            setBoard("01_BY_05_NO_OBSTRUCTIONS");
        }

        @Test
        @DisplayName(value = "should have clear LOS on flat unobstructed terrain")
        void shouldHaveClearLos_OnFlatUnobstructedTerrain() {
            // Arrange
            LosEffects.AttackInfo attackInfo = new LosEffects.AttackInfo();
            attackInfo.attackPos = new Coords(0, 0);
            attackInfo.targetPos = new Coords(0, 4);
            attackInfo.attackAbsHeight = 0;
            attackInfo.targetAbsHeight = 0;
            attackInfo.attOnLand = true;
            attackInfo.targetOnLand = true;
            attackInfo.targetEntity = true;

            // Act
            LosEffects result = LosEffects.calculateLos(game, attackInfo);

            // Assert
            assertNotNull(result, "LosEffects should not be null");
            assertTrue(result.hasLoS, "Should have line of sight on flat unobstructed terrain");
            assertFalse(result.blocked, "LOS should not be blocked on flat unobstructed terrain");
        }
    }

    /**
     * Tests for
     * {@link LosEffects#calculateLOS(Game, Entity, megamek.common.units.Targetable, Coords, Coords, int, int,
     * boolean)}
     */
    @Nested
    @DisplayName(value = "calculateLOS with explicit attackHeight Tests")
    class CalculateLosWithAttackHeightTests {
        private static GameOptions mockGameOptions;
        private Player player1;
        private Player player2;

        Mek createMek(String chassis, String model, String crewName) {
            Mek mockMek = new BipedMek();
            mockMek.setGame(game);
            mockMek.setChassis(chassis);
            mockMek.setModel(model);

            Crew mockCrew = mock(Crew.class);
            PilotOptions pOpt = new PilotOptions();
            when(mockCrew.getName(anyInt())).thenCallRealMethod();
            when(mockCrew.getNames()).thenReturn(new String[] { crewName });
            when(mockCrew.getOptions()).thenReturn(pOpt);
            when(mockCrew.isActive()).thenReturn(true);
            when(mockCrew.getCrewType()).thenReturn(CrewType.SINGLE);
            mockMek.setCrew(mockCrew);

            return mockMek;
        }

        BuildingEntity createBuildingEntity(String chassis, String model, String crewName) {
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

        @BeforeEach
        void setUp() {
            setBoard("03_BY_05_CENTER_HILLS");

            mockGameOptions = mock(GameOptions.class);
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

            player1 = new Player(0, "Player 1");
            player2 = new Player(1, "Player 2");

            game.addPlayer(0, player1);
            game.addPlayer(1, player2);
        }

        @Test
        @DisplayName(value = "should block LOS when firing from height 0 through elevation 3 terrain")
        void shouldBlockLos_WhenFiringFromHeight0ThroughElevation3Terrain() {
            // Arrange
            BuildingEntity attacker = createBuildingEntity("Attacker", "ATK-1", "Alice");
            attacker.setOwnerId(player1.getId());
            attacker.setId(1);
            attacker.setPosition(new Coords(1, 4));

            Mek targetEntity = createMek("Target", "TGT-2", "Bob");
            targetEntity.setOwnerId(player2.getId());
            targetEntity.setId(2);
            targetEntity.setPosition(new Coords(2, 0));

            game.addEntity(attacker);
            game.addEntity(targetEntity);

            // Act - Simulate firing from hex 0305 (elevation 0) at height 0
            // to target at hex 0301 (elevation 0), through hex 0303 (elevation 3)
            Coords attackerPosition = new Coords(0, 4); // hex 0305
            Coords targetPosition = new Coords(2, 0);   // hex 0301
            int attackHeight = 0;  // Firing from height 0 (level 0 of building)
            int boardId = attacker.getBoardId();

            LosEffects result = LosEffects.calculateLOS(game, attacker, targetEntity,
                  attackerPosition, targetPosition, attackHeight, boardId, false);

            // Assert
            assertNotNull(result, "LosEffects should not be null");
            assertTrue(result.blocked, "LOS should be blocked by elevation 3 terrain when firing from height 0");
            assertFalse(result.hasLoS, "Should not have line of sight when blocked by terrain");
        }

        @Test
        @DisplayName(value = "should not block LOS when firing from height 4 through elevation 3 terrain")
        void shouldBlockLos_WhenFiringFromHeight4ThroughElevation3Terrain() {
            // Arrange
            BuildingEntity attacker = createBuildingEntity("Attacker", "ATK-1", "Alice");
            attacker.setOwnerId(player1.getId());
            attacker.setId(1);
            attacker.setPosition(new Coords(1, 4));

            Mek targetEntity = createMek("Target", "TGT-2", "Bob");
            targetEntity.setOwnerId(player2.getId());
            targetEntity.setId(2);
            targetEntity.setPosition(new Coords(2, 0));

            game.addEntity(attacker);
            game.addEntity(targetEntity);

            // Act - Simulate firing from hex 0305 (elevation 0) at height 4
            // to target at hex 0301 (elevation 0), through hex 0303 (elevation 3)
            Coords attackerPosition = new Coords(0, 4); // hex 0305
            Coords targetPosition = new Coords(2, 0);   // hex 0301
            int attackHeight = 4;  // Firing from height 0 (level 0 of building)
            int boardId = attacker.getBoardId();

            LosEffects result = LosEffects.calculateLOS(game, attacker, targetEntity,
                  attackerPosition, targetPosition, attackHeight, boardId, false);

            // Assert
            assertNotNull(result, "LosEffects should not be null");
            assertFalse(result.blocked, "LOS should not be blocked by level 3 terrain when firing from level 4");
            assertTrue(result.hasLoS, "Should have line of sight");
        }
    }
}
