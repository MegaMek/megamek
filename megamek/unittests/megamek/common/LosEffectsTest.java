/*
 * Copyright (C) 2025-2026 The MegaMek Team. All Rights Reserved.
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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.EquipmentTypeLookup;
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
import megamek.common.units.EntityMovementMode;
import megamek.common.units.Mek;
import megamek.common.units.VTOL;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
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

        initializeBoard("02_BY_08_FIELDS", """
              size 2 8
              hex 0101 0 "planted_fields:1" ""
              hex 0201 0 "planted_fields:1" ""
              hex 0102 0 "planted_fields:1" ""
              hex 0202 0 "planted_fields:1" ""
              hex 0103 0 "planted_fields:1" ""
              hex 0203 0 "planted_fields:1" ""
              hex 0104 0 "planted_fields:1" ""
              hex 0204 1 "planted_fields:1" ""
              hex 0105 0 "planted_fields:1" ""
              hex 0205 0 "planted_fields:1" ""
              hex 0106 0 "planted_fields:1" ""
              hex 0206 0 "planted_fields:1" ""
              hex 0107 0 "planted_fields:1" ""
              hex 0207 0 "planted_fields:1" ""
              hex 0108 0 "planted_fields:1" ""
              hex 0208 0 "planted_fields:1" ""
              end"""
        );

        // Board with 1 light woods + 2 light smoke intervening (bug #8167 scenario)
        initializeBoard("01_BY_05_WOODS_SMOKE_COMBINED", """
              size 1 5
              hex 0101 0 "" ""
              hex 0102 0 "woods:1;foliage_elev:2" ""
              hex 0103 0 "smoke:1" ""
              hex 0104 0 "smoke:1" ""
              hex 0105 0 "" ""
              end"""
        );

        // Board with 3 light woods intervening
        initializeBoard("01_BY_05_THREE_LIGHT_WOODS", """
              size 1 5
              hex 0101 0 "" ""
              hex 0102 0 "woods:1;foliage_elev:2" ""
              hex 0103 0 "woods:1;foliage_elev:2" ""
              hex 0104 0 "woods:1;foliage_elev:2" ""
              hex 0105 0 "" ""
              end"""
        );

        // Board with 3 light smoke intervening
        initializeBoard("01_BY_05_THREE_LIGHT_SMOKE", """
              size 1 5
              hex 0101 0 "" ""
              hex 0102 0 "smoke:1" ""
              hex 0103 0 "smoke:1" ""
              hex 0104 0 "smoke:1" ""
              hex 0105 0 "" ""
              end"""
        );

        // Board with 1 light woods + 1 light smoke (should NOT block)
        initializeBoard("01_BY_04_WOODS_SMOKE_PARTIAL", """
              size 1 4
              hex 0101 0 "" ""
              hex 0102 0 "woods:1;foliage_elev:2" ""
              hex 0103 0 "smoke:1" ""
              hex 0104 0 "" ""
              end"""
        );

        // Board with 1 heavy woods + 1 light smoke (combined = 3, should block)
        initializeBoard("01_BY_04_HEAVY_WOODS_LIGHT_SMOKE", """
              size 1 4
              hex 0101 0 "" ""
              hex 0102 0 "woods:2;foliage_elev:2" ""
              hex 0103 0 "smoke:1" ""
              hex 0104 0 "" ""
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

        // Board with an erupting geyser (level 2) in the intervening hex 0103
        initializeBoard("01_BY_05_ERUPTING_GEYSER_INTERVENING", """
              size 1 5
              hex 0101 0 "" ""
              hex 0102 0 "" ""
              hex 0103 0 "geyser:2" ""
              hex 0104 0 "" ""
              hex 0105 0 "" ""
              end"""
        );

        // Board with a dormant geyser (level 1) in the intervening hex 0103
        initializeBoard("01_BY_05_DORMANT_GEYSER_INTERVENING", """
              size 1 5
              hex 0101 0 "" ""
              hex 0102 0 "" ""
              hex 0103 0 "geyser:1" ""
              hex 0104 0 "" ""
              hex 0105 0 "" ""
              end"""
        );

        // Board with an erupting geyser (level 2) in the target's own hex 0105
        initializeBoard("01_BY_05_ERUPTING_GEYSER_AT_TARGET", """
              size 1 5
              hex 0101 0 "" ""
              hex 0102 0 "" ""
              hex 0103 0 "" ""
              hex 0104 0 "" ""
              hex 0105 0 "geyser:2" ""
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
     * Tests for {@link LosEffects#calculateLos(Game, LosEffects.AttackInfo)} with Standard LOS Rules
     */
    @Nested
    @DisplayName(value = "calculateLos with Standard LOS Tests")
    class CalculateLosTestsWithStandardLOS {
        private static GameOptions mockGameOptions;

        @BeforeEach
        void setUp() {
            setBoard("02_BY_08_FIELDS");
            mockGameOptions = mock(GameOptions.class);
            game.setOptions(mockGameOptions);
            when(mockGameOptions.booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_LOS1)).thenReturn(false);
        }

        @Test
        @DisplayName(value = "should have clear LOS over planted fields")
        void shouldHaveClearLos_OverPlantedFields() {
            // Arrange
            LosEffects.AttackInfo attackInfo = new LosEffects.AttackInfo();
            attackInfo.attackPos = new Coords(0, 0);
            attackInfo.targetPos = new Coords(0, 7);
            attackInfo.attackAbsHeight = 1;
            attackInfo.targetAbsHeight = 1;
            attackInfo.attOnLand = true;
            attackInfo.targetOnLand = true;
            attackInfo.targetEntity = true;

            // Act
            LosEffects result = LosEffects.calculateLos(game, attackInfo);

            // Assert
            assertNotNull(result, "LosEffects should not be null");
            assertFalse(result.blocked, "Should not have line of sight blocked by terrain");
            assertEquals(0, result.plantedFields, """
                  Should have exactly 0 intervening planted fields""");
            assertTrue(result.hasLoS, "Should have line of sight over planted fields");
        }

        @Test
        @DisplayName(value = "should have blocked LOS with 6 planted fields")
        void shouldBlockLos_OverPlantedFields() {
            // Arrange
            LosEffects.AttackInfo attackInfo = new LosEffects.AttackInfo();
            attackInfo.attackPos = new Coords(0, 0);
            attackInfo.targetPos = new Coords(0, 7);
            attackInfo.attackAbsHeight = 0;
            attackInfo.targetAbsHeight = 0;
            attackInfo.attOnLand = true;
            attackInfo.targetOnLand = true;
            attackInfo.targetEntity = true;

            // Act
            LosEffects result = LosEffects.calculateLos(game, attackInfo);

            // Assert
            assertNotNull(result, "LosEffects should not be null");
            assertFalse(result.blocked, "Should not have line of sight blocked by terrain");
            assertEquals(6, result.plantedFields, """
                  Should have exactly 6 intervening planted fields""");
            assertFalse(result.hasLoS, "Should not have line of sight because of 6 intervening planted fields");

        }

        @Test
        @DisplayName(value = "should have intervening LOS over planted fields")
        void shouldHaveInterveningLos_OverPlantedFields() {
            // Arrange
            LosEffects.AttackInfo attackInfo = new LosEffects.AttackInfo();
            attackInfo.attackPos = new Coords(0, 0);
            attackInfo.targetPos = new Coords(0, 7);
            attackInfo.attackAbsHeight = 2;
            attackInfo.targetAbsHeight = 0;
            attackInfo.attOnLand = true;
            attackInfo.targetOnLand = true;
            attackInfo.targetEntity = true;

            // Act
            LosEffects result = LosEffects.calculateLos(game, attackInfo);

            // Assert
            assertNotNull(result, "LosEffects should not be null");
            assertFalse(result.blocked, "Should not have line of sight blocked by terrain");
            assertEquals(1, result.plantedFields, """
                  Should have exactly 1 intervening planted field""");
            assertTrue(result.hasLoS, "Should have line of sight thru 1 planted field");
        }


        @Test
        @DisplayName(value = "should have intervening LOS over raised planted fields")
        void shouldHaveInterveningLos_OverRaisedPlantedFields() {
            // Arrange
            LosEffects.AttackInfo attackInfo = new LosEffects.AttackInfo();
            attackInfo.attackPos = new Coords(1, 0);
            attackInfo.targetPos = new Coords(1, 7);
            attackInfo.attackAbsHeight = 1;
            attackInfo.targetAbsHeight = 1;
            attackInfo.attOnLand = true;
            attackInfo.targetOnLand = true;
            attackInfo.targetEntity = true;

            // Act
            LosEffects result = LosEffects.calculateLos(game, attackInfo);

            // Assert
            assertNotNull(result, "LosEffects should not be null");
            assertFalse(result.blocked, "Should not have line of sight blocked by terrain");
            assertEquals(1, result.plantedFields, """
                  Should have exactly 1 intervening planted field""");
            assertTrue(result.hasLoS, "Should have line of sight thru 1 raised planted field");
        }

    }

    /**
     * Tests for {@link LosEffects#calculateLos(Game, LosEffects.AttackInfo)} with Diagramming LOS
     */
    @Nested
    @DisplayName(value = "calculateLos with Diagramming LOS Tests")
    class CalculateLosTestsWithDigramLOS {
        private static GameOptions mockGameOptions;

        @BeforeEach
        void setUp() {
            setBoard("02_BY_08_FIELDS");
            mockGameOptions = mock(GameOptions.class);
            game.setOptions(mockGameOptions);
            when(mockGameOptions.booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_LOS1)).thenReturn(true);
        }

        @Test
        @DisplayName(value = "should have clear LOS over planted fields")
        void shouldHaveClearLos_OverPlantedFields() {
            // Arrange
            LosEffects.AttackInfo attackInfo = new LosEffects.AttackInfo();
            attackInfo.attackPos = new Coords(0, 0);
            attackInfo.targetPos = new Coords(0, 7);
            attackInfo.attackAbsHeight = 1;
            attackInfo.targetAbsHeight = 1;
            attackInfo.attOnLand = true;
            attackInfo.targetOnLand = true;
            attackInfo.targetEntity = true;

            // Act
            LosEffects result = LosEffects.calculateLos(game, attackInfo);

            // Assert
            assertNotNull(result, "LosEffects should not be null");
            assertFalse(result.blocked, "Should not have line of sight blocked by terrain");
            assertEquals(0, result.plantedFields, """
                  Should have exactly 0 intervening planted fields""");
            assertTrue(result.hasLoS, "Should have line of sight over planted fields");
        }

        @Test
        @DisplayName(value = "should have blocked LOS with 6 planted fields")
        void shouldBlockLos_OverPlantedFields() {
            // Arrange
            LosEffects.AttackInfo attackInfo = new LosEffects.AttackInfo();
            attackInfo.attackPos = new Coords(0, 0);
            attackInfo.targetPos = new Coords(0, 7);
            attackInfo.attackAbsHeight = 0;
            attackInfo.targetAbsHeight = 0;
            attackInfo.attOnLand = true;
            attackInfo.targetOnLand = true;
            attackInfo.targetEntity = true;

            // Act
            LosEffects result = LosEffects.calculateLos(game, attackInfo);

            // Assert
            assertNotNull(result, "LosEffects should not be null");
            assertFalse(result.blocked, "Should not have line of sight blocked by terrain");
            assertEquals(6, result.plantedFields, """
                  Should have exactly 6 intervening planted fields""");
            assertFalse(result.hasLoS, "Should not have line of sight because of 6 intervening planted fields");

        }

        @Test
        @DisplayName(value = "should not have intervening LOS over planted fields")
        void shouldNotHaveInterveningLos_OverPlantedFields() {
            // Arrange
            LosEffects.AttackInfo attackInfo = new LosEffects.AttackInfo();
            attackInfo.attackPos = new Coords(0, 0);
            attackInfo.targetPos = new Coords(0, 7);
            attackInfo.attackAbsHeight = 1;
            attackInfo.targetAbsHeight = 0;
            attackInfo.attOnLand = true;
            attackInfo.targetOnLand = true;
            attackInfo.targetEntity = true;

            // Act
            LosEffects result = LosEffects.calculateLos(game, attackInfo);

            // Assert
            assertNotNull(result, "LosEffects should not be null");
            assertFalse(result.blocked, "Should not have line of sight blocked by terrain");
            assertEquals(0, result.plantedFields, """
                  Should have exactly 0 intervening planted field""");
            assertTrue(result.hasLoS, "Should have line of sight over planted fields");
        }

        @Test
        @DisplayName(value = "should have intervening LOS over raised planted fields")
        void shouldHaveInterveningLos_OverRaisedPlantedFields() {
            // Arrange
            LosEffects.AttackInfo attackInfo = new LosEffects.AttackInfo();
            attackInfo.attackPos = new Coords(1, 0);
            attackInfo.targetPos = new Coords(1, 7);
            attackInfo.attackAbsHeight = 1;
            attackInfo.targetAbsHeight = 1;
            attackInfo.attOnLand = true;
            attackInfo.targetOnLand = true;
            attackInfo.targetEntity = true;

            // Act
            LosEffects result = LosEffects.calculateLos(game, attackInfo);

            // Assert
            assertNotNull(result, "LosEffects should not be null");
            assertFalse(result.blocked, "Should not have line of sight blocked by terrain");
            assertEquals(1, result.plantedFields, """
                  Should have exactly 1 intervening planted field""");
            assertTrue(result.hasLoS, "Should have line of sight thru 1 raised planted field");
        }
    }

    /**
     * Tests for woods and smoke combined LOS blocking (issue #8167). TW rules: woods and smoke modifiers are combined
     * for the blocking threshold. LOS is blocked when (lightWoods + lightSmoke) + ((heavyWoods + heavySmoke) * 2) +
     * (ultraWoods * 3) >= 3.
     */
    @Nested
    @DisplayName("Woods and Smoke Combined LOS Blocking Tests (Issue #8167)")
    class WoodsSmokeBlockingTests {

        @BeforeEach
        void setUp() {
        }

        private LosEffects.AttackInfo buildGroundAttackInfo(Coords attackPos, Coords targetPos) {
            LosEffects.AttackInfo ai = new LosEffects.AttackInfo();
            ai.attackPos = attackPos;
            ai.targetPos = targetPos;
            ai.attackAbsHeight = 0;
            ai.targetAbsHeight = 0;
            ai.attOnLand = true;
            ai.targetOnLand = true;
            ai.targetEntity = true;
            return ai;
        }

        @Test
        @DisplayName("1 light wood + 2 light smoke should block LOS (combined = 3)")
        void shouldBlockLos_WoodsAndSmokeCombined() {
            // This is the exact scenario from bug #8167
            setBoard("01_BY_05_WOODS_SMOKE_COMBINED");
            LosEffects.AttackInfo ai = buildGroundAttackInfo(new Coords(0, 0), new Coords(0, 4));

            LosEffects result = LosEffects.calculateLos(game, ai);

            assertNotNull(result);
            assertEquals(1, result.lightWoods, "Should count 1 light woods");
            assertEquals(2, result.lightSmoke, "Should count 2 light smoke");
            assertFalse(result.hasLoS, "hasLoS should be false (combined woods+smoke = 3)");

            // The critical check: losModifiers must also return IMPOSSIBLE
            ToHitData thd = result.losModifiers(game);
            assertEquals(TargetRoll.IMPOSSIBLE, thd.getValue(),
                  "losModifiers should return IMPOSSIBLE when combined woods+smoke >= 3");
        }

        @Test
        @DisplayName("3 light woods should block LOS (woods alone = 3)")
        void shouldBlockLos_ThreeLightWoods() {
            setBoard("01_BY_05_THREE_LIGHT_WOODS");
            LosEffects.AttackInfo ai = buildGroundAttackInfo(new Coords(0, 0), new Coords(0, 4));

            LosEffects result = LosEffects.calculateLos(game, ai);

            assertNotNull(result);
            assertEquals(3, result.lightWoods, "Should count 3 light woods");
            assertFalse(result.hasLoS, "hasLoS should be false (3 light woods)");

            ToHitData thd = result.losModifiers(game);
            assertEquals(TargetRoll.IMPOSSIBLE, thd.getValue(),
                  "losModifiers should return IMPOSSIBLE for 3 light woods");
        }

        @Test
        @DisplayName("3 light smoke should block LOS (smoke alone = 3)")
        void shouldBlockLos_ThreeLightSmoke() {
            setBoard("01_BY_05_THREE_LIGHT_SMOKE");
            LosEffects.AttackInfo ai = buildGroundAttackInfo(new Coords(0, 0), new Coords(0, 4));

            LosEffects result = LosEffects.calculateLos(game, ai);

            assertNotNull(result);
            assertEquals(3, result.lightSmoke, "Should count 3 light smoke");
            assertFalse(result.hasLoS, "hasLoS should be false (3 light smoke)");

            ToHitData thd = result.losModifiers(game);
            assertEquals(TargetRoll.IMPOSSIBLE, thd.getValue(),
                  "losModifiers should return IMPOSSIBLE for 3 light smoke");
        }

        @Test
        @DisplayName("1 light wood + 1 light smoke should NOT block LOS (combined = 2)")
        void shouldNotBlockLos_WoodsAndSmokePartial() {
            setBoard("01_BY_04_WOODS_SMOKE_PARTIAL");
            LosEffects.AttackInfo ai = buildGroundAttackInfo(new Coords(0, 0), new Coords(0, 3));

            LosEffects result = LosEffects.calculateLos(game, ai);

            assertNotNull(result);
            assertEquals(1, result.lightWoods, "Should count 1 light woods");
            assertEquals(1, result.lightSmoke, "Should count 1 light smoke");
            assertTrue(result.hasLoS, "hasLoS should be true (combined woods+smoke = 2)");

            ToHitData thd = result.losModifiers(game);
            assertNotEquals(TargetRoll.IMPOSSIBLE,
                  thd.getValue(),
                  "losModifiers should NOT return IMPOSSIBLE when combined < 3");
        }

        @Test
        @DisplayName("1 heavy wood + 1 light smoke should block LOS (combined = 3)")
        void shouldBlockLos_HeavyWoodsAndLightSmoke() {
            setBoard("01_BY_04_HEAVY_WOODS_LIGHT_SMOKE");
            LosEffects.AttackInfo ai = buildGroundAttackInfo(new Coords(0, 0), new Coords(0, 3));

            LosEffects result = LosEffects.calculateLos(game, ai);

            assertNotNull(result);
            assertEquals(1, result.heavyWoods, "Should count 1 heavy woods");
            assertEquals(1, result.lightSmoke, "Should count 1 light smoke");
            assertFalse(result.hasLoS,
                  "hasLoS should be false (1 heavy woods * 2 + 1 light smoke = 3)");

            ToHitData thd = result.losModifiers(game);
            assertEquals(TargetRoll.IMPOSSIBLE, thd.getValue(),
                  "losModifiers should return IMPOSSIBLE when combined >= 3");
        }

        @Test
        @DisplayName("hasLoS and losModifiers should agree on blocking decision")
        void hasLoSAndLosModifiersShouldAgree() {
            // Test the exact bug: hasLoS says blocked but losModifiers says not blocked
            setBoard("01_BY_05_WOODS_SMOKE_COMBINED");
            LosEffects.AttackInfo ai = buildGroundAttackInfo(new Coords(0, 0), new Coords(0, 4));

            LosEffects result = LosEffects.calculateLos(game, ai);
            ToHitData thd = result.losModifiers(game);

            // These MUST agree - this is the core of bug #8167
            if (!result.hasLoS) {
                assertEquals(TargetRoll.IMPOSSIBLE, thd.getValue(),
                      "When hasLoS is false, losModifiers must return IMPOSSIBLE. "
                            + "Combined woods+smoke: lightWoods=" + result.lightWoods
                            + " heavyWoods=" + result.heavyWoods
                            + " lightSmoke=" + result.lightSmoke
                            + " heavySmoke=" + result.heavySmoke);
            }
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

    /**
     * Tests for the TacOps erupting-geyser rule: an erupting geyser is treated as ultra-heavy woods for the purpose of
     * determining line of sight into or through its hex.
     */
    @Nested
    @DisplayName("Erupting Geyser LOS Blocking Tests")
    class EruptingGeyserBlockingTests {

        private LosEffects.AttackInfo buildGroundAttackInfo(Coords attackPos, Coords targetPos,
              int attackAbsHeight, int targetAbsHeight) {
            LosEffects.AttackInfo ai = new LosEffects.AttackInfo();
            ai.attackPos = attackPos;
            ai.targetPos = targetPos;
            ai.attackAbsHeight = attackAbsHeight;
            ai.targetAbsHeight = targetAbsHeight;
            ai.attOnLand = true;
            ai.targetOnLand = true;
            ai.targetEntity = true;
            return ai;
        }

        @Test
        @DisplayName("erupting geyser in an intervening hex blocks LOS (treated as ultra-heavy woods)")
        void shouldBlockLos_EruptingGeyserIntervening() {
            setBoard("01_BY_05_ERUPTING_GEYSER_INTERVENING");
            LosEffects.AttackInfo ai = buildGroundAttackInfo(new Coords(0, 0), new Coords(0, 4), 0, 0);

            LosEffects result = LosEffects.calculateLos(game, ai);

            assertNotNull(result);
            assertEquals(1, result.ultraWoods, "Erupting geyser should count as 1 ultra woods");
            assertFalse(result.hasLoS, "hasLoS should be false through an erupting geyser");

            ToHitData thd = result.losModifiers(game);
            assertEquals(TargetRoll.IMPOSSIBLE, thd.getValue(),
                  "losModifiers should return IMPOSSIBLE through an erupting geyser");
        }

        @Test
        @DisplayName("dormant geyser in an intervening hex does NOT block LOS")
        void shouldNotBlockLos_DormantGeyserIntervening() {
            setBoard("01_BY_05_DORMANT_GEYSER_INTERVENING");
            LosEffects.AttackInfo ai = buildGroundAttackInfo(new Coords(0, 0), new Coords(0, 4), 0, 0);

            LosEffects result = LosEffects.calculateLos(game, ai);

            assertNotNull(result);
            assertEquals(0, result.ultraWoods, "Dormant geyser should not count as ultra woods");
            assertTrue(result.hasLoS, "hasLoS should be true through a dormant geyser");

            ToHitData thd = result.losModifiers(game);
            assertNotEquals(TargetRoll.IMPOSSIBLE, thd.getValue(),
                  "losModifiers should NOT return IMPOSSIBLE through a dormant geyser");
        }

        @Test
        @DisplayName("target standing in an erupting geyser cannot be seen (LOS into the hex blocked)")
        void shouldBlockLos_TargetInEruptingGeyser() {
            setBoard("01_BY_05_ERUPTING_GEYSER_AT_TARGET");
            LosEffects.AttackInfo ai = buildGroundAttackInfo(new Coords(0, 0), new Coords(0, 4), 0, 0);

            LosEffects result = LosEffects.calculateLos(game, ai);

            assertNotNull(result);
            assertEquals(1, result.ultraWoods, "Target's own erupting geyser should count as 1 ultra woods");
            assertFalse(result.hasLoS, "hasLoS should be false for a target engulfed by an erupting geyser");

            ToHitData thd = result.losModifiers(game);
            assertEquals(TargetRoll.IMPOSSIBLE, thd.getValue(),
                  "losModifiers should return IMPOSSIBLE for a target engulfed by an erupting geyser");
        }

        @Test
        @DisplayName("target raised above the geyser plume remains visible")
        void shouldNotBlockLos_TargetAboveEruptingGeyserPlume() {
            setBoard("01_BY_05_ERUPTING_GEYSER_AT_TARGET");
            // Plume rises 3 levels above the hex (level 0), so a unit at absolute height 4 is above it.
            LosEffects.AttackInfo ai = buildGroundAttackInfo(new Coords(0, 0), new Coords(0, 4), 4, 4);

            LosEffects result = LosEffects.calculateLos(game, ai);

            assertNotNull(result);
            assertEquals(0, result.ultraWoods, "A target above the plume should not be engulfed");
            assertTrue(result.hasLoS, "hasLoS should be true for a target raised above the geyser plume");
        }
    }

    /**
     * Tests for the VTOL Mast Mount spotting rule (issue #8385). A Mast Mount treats the VTOL's onboard sensors as
     * 1 elevation higher for spotting only (TacOps), letting a VTOL hovering behind cover spot over it. The +1 must
     * apply only when {@code spotting} is true, and never for a unit without a working Mast Mount.
     */
    @Nested
    @DisplayName("VTOL Mast Mount Spotting Tests (Issue #8385)")
    class MastMountSpottingTests {
        private static GameOptions mockGameOptions;

        @BeforeAll
        static void initEquipment() {
            EquipmentType.initializeTypes();
        }

        @BeforeEach
        void setUp() {
            setBoard("03_BY_05_CENTER_HILLS");
            mockGameOptions = mock(GameOptions.class);
            game.setOptions(mockGameOptions);
            when(mockGameOptions.booleanOption(anyString())).thenReturn(false);
        }

        private VTOL createVtol(Coords position, int elevation, boolean withMastMount) {
            VTOL vtol = new VTOL();
            vtol.setGame(game);
            vtol.setChassis("Mantis");
            vtol.setModel("ECCM");
            vtol.setMovementMode(EntityMovementMode.VTOL);
            vtol.setCrew(new Crew(CrewType.SINGLE));
            vtol.setId(1);
            vtol.setOwnerId(player.getId());
            vtol.setPosition(position);
            vtol.setElevation(elevation);
            if (withMastMount) {
                try {
                    vtol.addEquipment(EquipmentType.get(EquipmentTypeLookup.MAST_MOUNT), VTOL.LOC_ROTOR);
                } catch (LocationFullException exception) {
                    throw new IllegalStateException("Could not mount Mast Mount on test VTOL", exception);
                }
            }
            return vtol;
        }

        private Mek createGroundTarget(Coords position) {
            Mek target = new BipedMek();
            target.setGame(game);
            target.setChassis("Target");
            target.setModel("TGT");
            target.setCrew(new Crew(CrewType.SINGLE));
            target.setId(2);
            target.setOwnerId(player.getId());
            target.setPosition(position);
            return target;
        }

        // Attacker fires from 0305 toward 0301 across the level-3 hill at 0303. At eye height 3 the hill
        // strictly blocks the line; the Mast Mount's +1 spotting elevation (eye 4) clears it.
        private static final Coords ATTACKER_POS = new Coords(0, 4);
        private static final Coords TARGET_POS = new Coords(2, 0);
        private static final int BLOCKED_EYE_HEIGHT = 3;

        @Test
        @DisplayName("mast-mount VTOL spotting over a level-3 hill: the +1 elevation clears the block")
        void shouldSpotOverHill_WithMastMount() {
            VTOL vtol = createVtol(ATTACKER_POS, 0, true);
            Mek target = createGroundTarget(TARGET_POS);
            game.addEntity(vtol);
            game.addEntity(target);
            int boardId = vtol.getBoardId();

            boolean directCanSee = LosEffects.calculateLOS(game, vtol, target,
                  ATTACKER_POS, TARGET_POS, BLOCKED_EYE_HEIGHT, boardId, false).canSee();
            boolean spottingCanSee = LosEffects.calculateLOS(game, vtol, target,
                  ATTACKER_POS, TARGET_POS, BLOCKED_EYE_HEIGHT, boardId, true).canSee();

            assertFalse(directCanSee,
                  "Without the spotting +1, the level-3 hill blocks the VTOL eye at height 3");
            assertTrue(spottingCanSee,
                  "Mast Mount +1 spotting elevation should clear the level-3 hill");
        }

        @Test
        @DisplayName("VTOL without a mast mount is unaffected by the spotting flag")
        void shouldNotChangeLos_WithoutMastMount() {
            VTOL vtol = createVtol(ATTACKER_POS, 0, false);
            Mek target = createGroundTarget(TARGET_POS);
            game.addEntity(vtol);
            game.addEntity(target);
            int boardId = vtol.getBoardId();

            boolean directCanSee = LosEffects.calculateLOS(game, vtol, target,
                  ATTACKER_POS, TARGET_POS, BLOCKED_EYE_HEIGHT, boardId, false).canSee();
            boolean spottingCanSee = LosEffects.calculateLOS(game, vtol, target,
                  ATTACKER_POS, TARGET_POS, BLOCKED_EYE_HEIGHT, boardId, true).canSee();

            assertFalse(spottingCanSee, "A plain VTOL gets no +1, so the level-3 hill still blocks it");
            assertEquals(directCanSee, spottingCanSee,
                  "The spotting flag must not change LOS for a unit without a Mast Mount");
        }
    }
}
