/*
 * Copyright (c) 2000-2005 - Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2013-2025 The MegaMek Team. All Rights Reserved.
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

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

import megamek.common.actions.DfaAttackAction;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.Mounted;
import megamek.common.game.Game;
import megamek.common.loaders.MekFileParser;
import megamek.common.options.OptionsConstants;
import megamek.common.units.BipedMek;
import megamek.common.units.Crew;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import megamek.common.units.Tank;
import megamek.common.units.Terrain;
import megamek.common.units.Terrains;
import megamek.common.units.VTOL;
import megamek.common.util.C3Util;
import megamek.utils.EntityLoader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * @author Deric "Netzilla" Page (deric dot page at usa dot net)
 * @since 11/3/13 8:48 AM
 */
class EntityTest {


    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    @Test
    void testCalculateBattleValue() {
        // Test a gun emplacement.
        Entity testEntity = EntityLoader.loadFromFile("Medium Blaze Turret 3025.blk");

        int expected = 203;
        int actual = testEntity.calculateBattleValue(true, true);

        // Hit its main weapon
        testEntity.getEquipment(0).setHit(true);
        assertEquals(expected, actual);
        int expectedPostDamage = 105;
        int actualPostDamage = testEntity.calculateBattleValue(true, true);
        assertEquals(expectedPostDamage, actualPostDamage);
    }

    @Test
    void testCalculateWeight() {
        Entity e = EntityLoader.loadFromFile("Exterminator EXT-4A.mtf");
        assertEquals(65, (int) e.getWeight());
    }

    @Test
    void testFormatHeat() {
        Entity entity = EntityLoader.loadFromFile("Sagittaire SGT-14D.mtf");
        assertEquals("28, 42 with RHS", entity.formatHeat());
    }

    /**
     * Verify that if a unit's name appears in the list of canon unit names, it is canon
     */
    @Test
    void testCanon() {
        MekFileParser.setCanonUnitNames(new Vector<>(Collections.singleton("Exterminator EXT-4A")));
        Entity e = EntityLoader.loadFromFile("Exterminator EXT-4A.mtf");
        assertTrue(e.isCanon());
    }

    /**
     * Verify that if a unit's name does _not_ appear in the list of canon unit names, it is not canon
     */
    @Test
    void testForceCanonicityFailure() {
        MekFileParser.setCanonUnitNames(new Vector<>(Collections.singleton("foobar")));
        Entity e = EntityLoader.loadFromFile("Exterminator EXT-4A.mtf");
        assertFalse(e.isCanon());
    }

    /**
     * Verify that if a unit's name does appear in the _file_ listing canon unit names, it is canon
     */
    @Test
    void testCanonUnitInCanonUnitListFile() {
        MekFileParser.initCanonUnitNames(EntityLoader.RESOURCE_FOLDER, "mockOfficialUnitList.txt");
        Entity entity = EntityLoader.loadFromFile("Exterminator EXT-4A.mtf");
        assertTrue(entity.isCanon());
        entity = EntityLoader.loadFromFile("Kanga Medium Hovertank.blk");
        assertTrue(entity.isCanon());
    }

    /**
     * Verify new Tank method .isImmobilizedForJump() returns correct values in various states. Note: vehicles cannot
     * lose individual Jump Jets via crits, so this is not tested.
     */
    @Test
    void testIsImmobilizedForJump() {
        // Test 1/1
        try {
            Tank t = EntityLoader.loadFromFile("Kanga Medium Hovertank.blk", Tank.class);
            Crew c = t.getCrew();

            // 1 Crew condition
            // 1.a Killed crew should prevent jumping; live crew should allow jumping
            c.setDead(true);
            assertTrue(t.isImmobileForJump(), "Killed crew should prevent jumping");
            c.resetGameState();
            assertFalse(t.isImmobileForJump(), "Live crew should allow jumping");

            // 1.b Unconscious crew should prevent jumping; conscious crew should allow
            // jumping
            c.setUnconscious(true);
            assertTrue(t.isImmobileForJump(), "Unconscious crew should prevent jumping");
            c.resetGameState();
            assertFalse(t.isImmobileForJump(), "Conscious crew should allow jumping");

            // 1.c Stunned crew should _not_ prevent jumping
            t.setStunnedTurns(1);
            assertFalse(t.isImmobileForJump(), "Stunned crew should not prevent jumping");
            t.setStunnedTurns(0);

            // 2. Engine condition
            // 2.a Engine hit should prevent jumping; fixing engine should enable jumping
            t.engineHit();
            assertTrue(t.isImmobileForJump(), "Engine hit should prevent jumping");
            t.engineFix();
            assertFalse(t.isImmobileForJump(), "Engine working should enable jumping");

            // 2.b Shutdown should prevent jumping; restarting should enable jumping
            t.setShutDown(true);
            assertTrue(t.isImmobileForJump(), "Shutdown should prevent jumping");
            t.setShutDown(false);
            assertFalse(t.isImmobileForJump(), "Restarting should enable jumping");

            // 3. Immobilization due to massive damage motive hit / reducing MP to 0 should
            // _not_ prevent jumping
            t.setMotiveDamage(t.getOriginalWalkMP());
            assertFalse(t.isImmobileForJump(),
                  "Immobilization due to massive damage motive hit / reducing MP to 0 should not prevent jumping");
            t.setMotiveDamage(0);

        } catch (Exception ex) {
            fail(ex.getMessage());
        }
    }

    @Test
    public void testCalcElevationDuringDfaIntoWater() {
        // Create a Mek that is capable of DFA
        Mek mek = new BipedMek();
        int postDfaElevationOnTargetUnderwater = 1;
        int assumedElevationOnDfaIsAlwaysZero = 0;
        mek.setElevation(postDfaElevationOnTargetUnderwater); // post DFA elevation

        // Setup hexes
        Hex sameHex = createWaterHexWithDepth(2);

        // Create a DFA attack to simulate we're in DFA resolution
        DfaAttackAction dfaAction = new DfaAttackAction(1, 2, new Coords(0, 0));
        mek.setDisplacementAttack(dfaAction);
        assertEquals(-2,
              mek.calcElevation(sameHex, sameHex, assumedElevationOnDfaIsAlwaysZero, false),
              "DFA attack should end at bottom of water hex (-2)");
    }

    @Test
    public void testCalcElevationIntoWater() {
        // Create a Mek for normal movement
        BipedMek mek = new BipedMek();

        assertAll("Check elevation",
              () -> assertEquals(-1, mek.calcElevation(createWaterHexWithDepth(2), createWaterHexWithDepth(1),
                          createWaterHexWithDepth(2).floor(), false),
                    "Normal movement should maintain consistent elevation"),
              () -> assertEquals(-1, mek.calcElevation(createWaterHexWithDepth(0), createWaterHexWithDepth(1)),
                    "Normal movement should maintain consistent elevation"),
              () -> assertEquals(-1, mek.calcElevation(createWaterHexWithDepth(3), createWaterHexWithDepth(1),
                          createWaterHexWithDepth(3).floor(), false),
                    "Normal movement should maintain consistent elevation"),
              () -> assertEquals(-3, mek.calcElevation(createWaterHexWithDepth(1), createWaterHexWithDepth(3),
                          createWaterHexWithDepth(1).floor(), false),
                    "Normal movement should maintain consistent elevation")
        );
    }

    private static Hex createWaterHexWithDepth(int depth) {
        return createWaterHexWithDepth(depth, 0);
    }

    private static Hex createWaterHexWithDepth(int depth, int level) {
        Hex sourceHex = new Hex(level);
        sourceHex.addTerrain(new Terrain(Terrains.WATER, depth));
        return sourceHex;
    }

    @Nested
    class isUnderwaterTests {
        BipedMek mek;

        @BeforeEach
        public void beforeEach() {
            mek = spy(BipedMek.class);
            doReturn(true).when(mek).hasOccupiedHex();
        }

        @Nested
        class isUnderwaterEntityUnderwaterTests {

            @BeforeEach
            public void beforeEach() {
                mek.setElevation(-2); //Underwater!
            }

            @Test
            public void testUnderwater() {
                // Arrange
                doReturn(createWaterHexWithDepth(2)).when(mek).getOccupiedHex();

                // Act
                boolean isUnderwater = mek.isUnderwater();

                // Assert
                assertTrue(isUnderwater);
            }

            @Test
            public void testUnderwaterDeeper() {
                // Arrange
                doReturn(createWaterHexWithDepth(2, -2)).when(mek).getOccupiedHex();

                // Act
                boolean isUnderwater = mek.isUnderwater();

                // Assert
                assertTrue(isUnderwater);
            }

            @Test
            public void testUnderwaterHigher() {
                // Arrange
                doReturn(createWaterHexWithDepth(2, 2)).when(mek).getOccupiedHex();

                // Act
                boolean isUnderwater = mek.isUnderwater();

                // Assert
                assertTrue(isUnderwater);
            }
        }

        @Nested
        class isUnderwaterEntityAbovewaterTests {

            @BeforeEach
            public void beforeEach() {
                mek.setElevation(0); // On surface!
            }

            @Test
            public void testNotUnderwater() {
                // Arrange
                doReturn(createWaterHexWithDepth(2)).when(mek).getOccupiedHex();

                // Act
                boolean isUnderwater = mek.isUnderwater();

                // Assert
                assertFalse(isUnderwater);
            }

            @Test
            public void testNotUnderwaterDeeper() {
                // Arrange
                doReturn(createWaterHexWithDepth(2, -2)).when(mek).getOccupiedHex();

                // Act
                boolean isUnderwater = mek.isUnderwater();

                // Assert
                assertFalse(isUnderwater);
                ;
            }

            @Test
            public void testNotUnderwaterHigher() {
                // Arrange
                doReturn(createWaterHexWithDepth(2, 2)).when(mek).getOccupiedHex();

                // Act
                boolean isUnderwater = mek.isUnderwater();

                // Assert
                assertFalse(isUnderwater);
            }
        }
    }

    // region ECM Checks
    @Nested
    class ecmTopTests {
        public void setUpECMEntity(Game game, Player player, Entity entity, Coords entityPosition, boolean angelECM) {
            if (entity.getId() == -1) {
                entity.setId(3);
            }
            entity.setOwner(player);
            entity.setPosition(entityPosition);
            entity.setBoardId(1);
            game.addEntity(entity);

            String ecmName = (angelECM) ? "ISAngelECMSuite" : "ISGuardianECMSuite";
            try {
                EquipmentType ecmType = EquipmentType.get(ecmName);
                entity.addEquipment(ecmType, Entity.LOC_NONE);
            } catch (Exception e) {
                fail("Failed to add equipment: " + e.getMessage());
            }
        }

        public void setUpC3Link(Game game, Player player, Entity e1, Coords e1Position, Entity e2,
              Coords e2Position, boolean boosted)
              throws C3Util.C3CapacityException, C3Util.MismatchingC3MException {

            // Configure IDs, owner, game state
            if (e1.getId() == -1) {
                e1.setId(1);
            }
            if (e2.getId() == -1) {
                e2.setId(2);
            }
            e1.setOwner(player);
            e2.setOwner(player);
            e1.setPosition(e1Position);
            e1.setBoardId(1);
            e2.setPosition(e2Position);
            e2.setBoardId(1);
            game.addEntity(e1);
            game.addEntity(e2);

            // Add and connect Boosted C3 equipment
            String slaveName = (boosted) ? "ISC3BoostedSystemSlaveUnit" : "ISC3SlaveUnit";
            String masterName = (boosted) ? "ISC3MasterBoostedSystemUnit" : "ISC3MasterUnit";
            try {
                EquipmentType boostedSlave = EquipmentType.get(slaveName);
                e2.addEquipment(boostedSlave, Entity.LOC_NONE);
                EquipmentType boostedMaster = EquipmentType.get(masterName);
                e1.addEquipment(boostedMaster, Entity.LOC_NONE);
            } catch (Exception e) {
                fail("Failed to add Boosted C3 equipment: " + e.getMessage());
            }

            C3Util.connect(game, new ArrayList<Entity>(List.of(e1, e2)), e1.getId(), false);

        }

        public Game setUpGame() {
            Game game = new Game();
            game.setBoard(1, new Board(16, 17));
            return game;
        }

        @Test
        public void testGetBoostedC3TopUnderAECMNoInfiniteLoop()
              throws C3Util.C3CapacityException, C3Util.MismatchingC3MException {

            Mek mek = new BipedMek();
            Tank tank = new Tank();
            VTOL vtol = new VTOL();

            // Set up game
            Game game = setUpGame();
            Player player1 = new Player(1, "C3 side");
            Player player2 = new Player(2, "ECM side");
            game.addPlayer(player1.getId(), player1);
            game.addPlayer(player2.getId(), player2);

            game.getOptions().getOption(OptionsConstants.PLAYTEST_3).setValue(false);

            // Set up initial C3 link
            setUpC3Link(game, player1, mek, new Coords(1, 1), tank, new Coords(3, 3), true);

            // Initial test of connection
            assertEquals(mek, tank.getC3Master());
            assertEquals(mek, tank.getC3Top());

            // Actual test: if AECM affects Boosted connection, slave entity returns self as "top" of network.
            setUpECMEntity(game, player2, vtol, new Coords(2, 2), true);
            assertEquals(tank, tank.getC3Top());
        }

        @Test
        public void testGetC3TopUnderECMNoInfiniteLoop()
              throws C3Util.C3CapacityException, C3Util.MismatchingC3MException {

            Mek mek = new BipedMek();
            Tank tank = new Tank();
            VTOL vtol = new VTOL();

            // Set up game
            Game game = setUpGame();
            Player player1 = new Player(1, "C3 side");
            Player player2 = new Player(2, "ECM side");
            game.addPlayer(player1.getId(), player1);
            game.addPlayer(player2.getId(), player2);

            game.getOptions().getOption(OptionsConstants.PLAYTEST_3).setValue(false);

            // Set up initial C3 link
            setUpC3Link(game, player1, mek, new Coords(1, 1), tank, new Coords(3, 3), false);

            // Initial test of connection
            assertEquals(mek, tank.getC3Master());
            assertEquals(mek, tank.getC3Top());

            // Actual test: if ECM affects C3 connection, slave entity returns self as "top" of network.
            setUpECMEntity(game, player2, vtol, new Coords(2, 2), false);
            assertEquals(tank, tank.getC3Top());
        }

        @Test
        public void testGetBoostedC3TopUnderECMNoInfiniteLoop()
              throws C3Util.C3CapacityException, C3Util.MismatchingC3MException {

            Mek mek = new BipedMek();
            Tank tank = new Tank();
            VTOL vtol = new VTOL();

            // Set up game
            Game game = setUpGame();
            Player player1 = new Player(1, "C3 side");
            Player player2 = new Player(2, "ECM side");
            game.addPlayer(player1.getId(), player1);
            game.addPlayer(player2.getId(), player2);

            game.getOptions().getOption(OptionsConstants.PLAYTEST_3).setValue(false);

            // Set up initial C3 link
            setUpC3Link(game, player1, mek, new Coords(1, 1), tank, new Coords(3, 3), true);

            // Initial test of connection
            assertEquals(mek, tank.getC3Master());
            assertEquals(mek, tank.getC3Top());

            // Actual test: ECM doesn't affect C3 connection, slave entity returns master as "top" of network.
            setUpECMEntity(game, player2, vtol, new Coords(2, 2), false);
            assertEquals(mek, tank.getC3Top());
        }

        @Test
        public void testGetC3TopUnderAECMNoInfiniteLoop()
              throws C3Util.C3CapacityException, C3Util.MismatchingC3MException {

            Mek mek = new BipedMek();
            Tank tank = new Tank();
            VTOL vtol = new VTOL();

            // Set up game
            Game game = setUpGame();
            Player player1 = new Player(1, "C3 side");
            Player player2 = new Player(2, "ECM side");
            game.addPlayer(player1.getId(), player1);
            game.addPlayer(player2.getId(), player2);

            game.getOptions().getOption(OptionsConstants.PLAYTEST_3).setValue(false);

            // Set up initial C3 link
            setUpC3Link(game, player1, mek, new Coords(1, 1), tank, new Coords(3, 3), false);

            // Initial test of connection
            assertEquals(mek, tank.getC3Master());
            assertEquals(mek, tank.getC3Top());

            // Actual test: if AECM affects C3 connection, slave entity returns self as "top" of network.
            setUpECMEntity(game, player2, vtol, new Coords(2, 2), true);
            assertEquals(tank, tank.getC3Top());
        }

        @Test
        public void testGetBoostedC3TopOnlyMasterUnderAECMNoInfiniteLoop()
              throws C3Util.C3CapacityException, C3Util.MismatchingC3MException {

            Mek mek = new BipedMek();
            Tank tank = new Tank();
            VTOL vtol = new VTOL();

            // Set up game
            Game game = setUpGame();
            Player player1 = new Player(1, "C3 side");
            Player player2 = new Player(2, "ECM side");
            game.addPlayer(player1.getId(), player1);
            game.addPlayer(player2.getId(), player2);

            game.getOptions().getOption(OptionsConstants.PLAYTEST_3).setValue(false);

            // Set up initial C3 link
            setUpC3Link(game, player1, mek, new Coords(1, 1), tank, new Coords(3, 3), true);

            // Initial test of connection
            assertEquals(mek, tank.getC3Master());
            assertEquals(mek, tank.getC3Top());

            // Actual test: if AECM affects Boosted connection, even only at one end,
            // slave entity returns self as "top" of network.
            setUpECMEntity(game, player2, vtol, new Coords(3, 9), true);
            assertEquals(tank, tank.getC3Top());
        }

    }
    // endregion ECM Checks
}
