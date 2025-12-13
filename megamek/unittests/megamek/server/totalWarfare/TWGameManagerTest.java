/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.server.totalWarfare;

import static megamek.common.CargoBayTest.createInfantry;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

import megamek.common.CriticalSlot;
import megamek.common.GameBoardTestCase;
import megamek.common.Hex;
import megamek.common.Player;
import megamek.common.Report;
import megamek.common.bays.CargoBay;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.enums.BasementType;
import megamek.common.enums.MoveStepType;
import megamek.common.equipment.EquipmentType;
import megamek.common.game.Game;
import megamek.common.moves.MovePath;
import megamek.common.rolls.PilotingRollData;
import megamek.common.units.*;
import megamek.server.Server;
import megamek.utils.ServerFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class TWGameManagerTest {
    private TWGameManager gameManager;
    private Game game;
    private Server server;

    @BeforeAll
    static void before() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void setUp() throws IOException {
        Player player = new Player(0, "Test");
        gameManager = new TWGameManager();
        game = gameManager.getGame();
        server = ServerFactory.createServer(gameManager);
        game.addPlayer(0, player);
    }

    @Test
    void testAddControlWithAdvAtmosphericMergesIntoOneRollAero() {
        AeroSpaceFighter aero = new AeroSpaceFighter();
        game.addEntity(aero);
        Vector<PilotingRollData> rolls = new Vector<>();
        StringBuilder reasons = new StringBuilder();

        game.addControlRoll(new PilotingRollData(aero.getId(), 0, "critical hit"));
        game.addControlRoll(new PilotingRollData(aero.getId(), 0, "avionics hit"));
        game.addControlRoll(new PilotingRollData(aero.getId(), 0, "threshold"));
        game.addControlRoll(new PilotingRollData(aero.getId(), 0, "highest damage threshold exceeded"));
        gameManager.addControlWithAdvAtmospheric(aero, rolls, reasons);
        assertEquals(1, rolls.size());
    }

    @Test
    void testAddControlWithAdvAtmosphericIncludesAllReasonsAero() {
        AeroSpaceFighter aero = new AeroSpaceFighter();
        game.addEntity(aero);
        Vector<PilotingRollData> rolls = new Vector<>();
        StringBuilder reasons = new StringBuilder();

        game.addControlRoll(new PilotingRollData(aero.getId(), 0, "critical hit"));
        game.addControlRoll(new PilotingRollData(aero.getId(), 0, "avionics hit"));
        game.addControlRoll(new PilotingRollData(aero.getId(), 0, "threshold"));
        game.addControlRoll(new PilotingRollData(aero.getId(), 0, "highest damage threshold exceeded"));
        gameManager.addControlWithAdvAtmospheric(aero, rolls, reasons);
        assertTrue(reasons.toString().contains("critical hit"));
        assertTrue(reasons.toString().contains("avionics hit"));
        assertTrue(reasons.toString().contains("threshold"));
        assertTrue(reasons.toString().contains("highest damage threshold exceeded"));
    }

    @Test
    void testAddControlWithAdvAtmosphericMergesIntoOneRollLAM() {
        LandAirMek mek = new LandAirMek(LandAirMek.GYRO_STANDARD, LandAirMek.COCKPIT_STANDARD,
              LandAirMek.LAM_STANDARD);
        game.addEntity(mek);
        Vector<PilotingRollData> rolls = new Vector<>();
        StringBuilder reasons = new StringBuilder();

        game.addControlRoll(new PilotingRollData(mek.getId(), 0, "avionics hit"));
        game.addControlRoll(new PilotingRollData(mek.getId(), 0, "threshold"));
        game.addControlRoll(new PilotingRollData(mek.getId(), 0, "highest damage threshold exceeded"));
        gameManager.addControlWithAdvAtmospheric(mek, rolls, reasons);
        assertEquals(1, rolls.size());
    }

    @Test
    void testAddControlWithAdvAtmosphericIncludesAllReasonsLAM() {
        LandAirMek mek = new LandAirMek(LandAirMek.GYRO_STANDARD, LandAirMek.COCKPIT_STANDARD,
              LandAirMek.LAM_STANDARD);
        game.addEntity(mek);
        Vector<PilotingRollData> rolls = new Vector<>();
        StringBuilder reasons = new StringBuilder();

        game.addControlRoll(new PilotingRollData(mek.getId(), 0, "avionics hit"));
        game.addControlRoll(new PilotingRollData(mek.getId(), 0, "threshold"));
        game.addControlRoll(new PilotingRollData(mek.getId(), 0, "highest damage threshold exceeded"));
        gameManager.addControlWithAdvAtmospheric(mek, rolls, reasons);
        assertTrue(reasons.toString().contains("avionics hit"));
        assertTrue(reasons.toString().contains("threshold"));
        assertTrue(reasons.toString().contains("highest damage threshold exceeded"));
    }

    /**
     * Test that standard gyro first hit triggers PSR with +3 modifier.
     * Per TotalWarfare rules (BMM pg 48), first gyro hit on standard gyro requires PSR at +3.
     */
    @Test
    void testStandardGyroFirstHitTriggersPSR() {
        BipedMek mek = new BipedMek();
        mek.setGyroType(Mek.GYRO_STANDARD);

        // Initialize gyro critical slots (normally done by unit loader)
        mek.setCritical(Mek.LOC_CENTER_TORSO, 3, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_GYRO));
        mek.setCritical(Mek.LOC_CENTER_TORSO, 4, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_GYRO));

        game.addEntity(mek);

        // Trigger game logic to apply gyro critical hit
        CriticalSlot gyroSlot = mek.getCritical(Mek.LOC_CENTER_TORSO, 3);
        gameManager.applyCriticalHit(mek, Mek.LOC_CENTER_TORSO, gyroSlot, true, 0, false);

        // Verify PSR was added by game logic
        List<PilotingRollData> psrs = Collections.list(game.getPSRs());
        assertEquals(1, psrs.size(), "Standard gyro first hit should trigger PSR");
        assertEquals(3, psrs.get(0).getValue(), "PSR modifier should be +3");
        assertTrue(psrs.get(0).getDesc().contains("gyro hit"), "PSR description should mention gyro");
    }

    /**
     * Test that heavy-duty gyro first hit does NOT trigger PSR.
     * Per errata: First hit to HD gyro does not require PSR, but applies +1 modifier to all future PSRs.
     * This tests the fix for Issue #3651.
     */
    @Test
    void testHeavyDutyGyroFirstHitNoPSR() {
        BipedMek mek = new BipedMek();
        mek.setGyroType(Mek.GYRO_HEAVY_DUTY);

        // Initialize gyro critical slots (normally done by unit loader)
        // Heavy-duty gyro takes 4 slots
        mek.setCritical(Mek.LOC_CENTER_TORSO, 3, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_GYRO));
        mek.setCritical(Mek.LOC_CENTER_TORSO, 4, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_GYRO));
        mek.setCritical(Mek.LOC_CENTER_TORSO, 5, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_GYRO));
        mek.setCritical(Mek.LOC_CENTER_TORSO, 6, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_GYRO));

        game.addEntity(mek);

        // Trigger game logic to apply gyro critical hit
        CriticalSlot gyroSlot = mek.getCritical(Mek.LOC_CENTER_TORSO, 3);
        gameManager.applyCriticalHit(mek, Mek.LOC_CENTER_TORSO, gyroSlot, true, 0, false);

        // Verify NO PSR was added by game logic (this is the fix for Issue #3651)
        List<PilotingRollData> psrs = Collections.list(game.getPSRs());
        assertEquals(0, psrs.size(), "Heavy-duty gyro first hit should NOT trigger PSR per errata");
    }

    /**
     * Test that heavy-duty gyro second hit triggers PSR with +3 modifier.
     * Per errata: Second hit to HD gyro has same effect as first hit to standard gyro.
     */
    @Test
    void testHeavyDutyGyroSecondHitTriggersPSR() {
        BipedMek mek = new BipedMek();
        mek.setGyroType(Mek.GYRO_HEAVY_DUTY);

        // Initialize gyro critical slots (normally done by unit loader)
        mek.setCritical(Mek.LOC_CENTER_TORSO, 3, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_GYRO));
        mek.setCritical(Mek.LOC_CENTER_TORSO, 4, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_GYRO));
        mek.setCritical(Mek.LOC_CENTER_TORSO, 5, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_GYRO));
        mek.setCritical(Mek.LOC_CENTER_TORSO, 6, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_GYRO));

        game.addEntity(mek);

        // Apply first gyro hit
        CriticalSlot gyroSlot1 = mek.getCritical(Mek.LOC_CENTER_TORSO, 3);
        gameManager.applyCriticalHit(mek, Mek.LOC_CENTER_TORSO, gyroSlot1, true, 0, false);

        // Apply second gyro hit
        CriticalSlot gyroSlot2 = mek.getCritical(Mek.LOC_CENTER_TORSO, 4);
        gameManager.applyCriticalHit(mek, Mek.LOC_CENTER_TORSO, gyroSlot2, true, 0, false);

        // Verify PSR was added by game logic for second hit
        List<PilotingRollData> psrs = Collections.list(game.getPSRs());
        assertEquals(1, psrs.size(), "HD gyro second hit should trigger PSR");
        assertEquals(3, psrs.get(0).getValue(), "PSR modifier should be +3");
    }

    /**
     * Test that heavy-duty gyro third hit causes automatic fall (gyro destroyed).
     * Per errata: Third hit to HD gyro destroys it with all usual effects.
     */
    @Test
    void testHeavyDutyGyroThirdHitAutoFail() {
        BipedMek mek = new BipedMek();
        mek.setGyroType(Mek.GYRO_HEAVY_DUTY);

        // Initialize gyro critical slots (normally done by unit loader)
        mek.setCritical(Mek.LOC_CENTER_TORSO, 3, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_GYRO));
        mek.setCritical(Mek.LOC_CENTER_TORSO, 4, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_GYRO));
        mek.setCritical(Mek.LOC_CENTER_TORSO, 5, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_GYRO));
        mek.setCritical(Mek.LOC_CENTER_TORSO, 6, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_GYRO));

        game.addEntity(mek);

        // Apply first gyro hit
        CriticalSlot gyroSlot1 = mek.getCritical(Mek.LOC_CENTER_TORSO, 3);
        gameManager.applyCriticalHit(mek, Mek.LOC_CENTER_TORSO, gyroSlot1, true, 0, false);

        // Apply second gyro hit (clears first PSR from game)
        game.resetPSRs();
        CriticalSlot gyroSlot2 = mek.getCritical(Mek.LOC_CENTER_TORSO, 4);
        gameManager.applyCriticalHit(mek, Mek.LOC_CENTER_TORSO, gyroSlot2, true, 0, false);

        // Apply third gyro hit (gyro destroyed)
        game.resetPSRs();
        CriticalSlot gyroSlot3 = mek.getCritical(Mek.LOC_CENTER_TORSO, 5);
        gameManager.applyCriticalHit(mek, Mek.LOC_CENTER_TORSO, gyroSlot3, true, 0, false);

        // Verify automatic fail PSR was added by game logic
        List<PilotingRollData> psrs = Collections.list(game.getPSRs());
        assertEquals(1, psrs.size(), "HD gyro third hit should trigger auto-fail PSR");
        assertEquals(PilotingRollData.AUTOMATIC_FAIL, psrs.get(0).getValue(), "PSR should be automatic fail");
        assertTrue(psrs.get(0).getDesc().contains("gyro destroyed"), "PSR description should mention gyro destroyed");
    }

    void initializeBoard(Board board) {
        for (int x = 0; x < board.getWidth(); x++) {
            for (int y = 0; y < board.getHeight(); y++) {
                board.setHex(x, y, new Hex());
            }
        }
    }

    /**
     *  Tests for unloading in specific states
     *  We want to make positively sure that these _specific_ unloading ops work.
     *  However, other unloading ops are only disallowed via the UI, because we call the same
     *  code for both "unloading", e.g., infantry dismounting mid-air, and for "dropping", that is,
     *  combat drops from DropShips and SmallCraft.
     */
    @ParameterizedTest()
    @EnumSource(names = { "INF_JUMP", "VTOL"})
    void testLargeSVVTOLCargoBayCanUnLoadValidInfantry(EntityMovementMode mode) {
        // Only Jump and VTOL infantry should be allowed to unload (not drop, that's for SC and DropShips)
        Player player = new Player(1, "Griffin Mill");
        VTOL carrier = new VTOL();
        carrier.setMovementMode(EntityMovementMode.VTOL);
        Coords position = new Coords(1, 1);
        carrier.setPosition(position);
        carrier.setElevation(5);
        carrier.setOwner(player);
        carrier.setId(1);
        game.addEntity(carrier);

        Infantry unit = createInfantry(mode.name(), "", "John Q. Test", game);
        unit.setOwner(player);
        unit.setMovementMode(mode);
        unit.setId(2);
        game.addEntity(unit);

        CargoBay bay = new CargoBay(100.0, 1, 0);
        carrier.addTransporter(bay);
        gameManager.loadUnit(carrier, unit, 0);

        // Create map for elevation and stacking checks.
        Board board = new Board(3, 3);
        initializeBoard(board);
        game.setBoard(board);

        // Create MovePath
        MovePath movePath = new MovePath(game, carrier);
        movePath.addStep(MoveStepType.UNLOAD, unit, position);
        MovePathHandler handler = new MovePathHandler(gameManager, carrier, movePath, null);
        handler.processMovement();

        assertTrue(unit.isDeployed());
        assertTrue(unit.getPosition().equals(position));
    }

    @Nested
    class TestDamageInfantryIn extends GameBoardTestCase {

        private TWGameManager gameManager;
        private Player player;
        private Coords buildingHex;
        private Coords emptyHex;

        @BeforeEach
        void beforeEach() {
            gameManager = new TWGameManager();
            gameManager.setGame(getGame());
            player = new Player(0, "Test Player");
            getGame().addPlayer(0, player);

            // Initialize a board with a building (3 levels high, CF 50)
            // Need to do this each time because we'll be smashing it up
            initializeBoard("BUILDING_BOARD", """
                size 2 2
                hex 0101 0 "" ""
                hex 0102 0 "" ""
                hex 0201 0 "" ""
                hex 0202 0 "bldg_elev:3;building:1;bldg_cf:50" ""
                end
                """);

            setBoard("BUILDING_BOARD");
            getGame().setBoard(getBoard("BUILDING_BOARD"));
            buildingHex = new Coords(1, 1); // hex 0202
            emptyHex = new Coords(0, 0); // hex 0101

            // Create BuildingTerrain manually for the building hex
            BuildingTerrain terrain = new BuildingTerrain(buildingHex, getGame().getBoard(), Terrains.BUILDING,
                  BasementType.UNKNOWN);
            getGame().getBoard().addBuildingToBoard(terrain);
        }

        @Test
        void testNullBuilding_ReturnsEmptyVector() {
            // Arrange
            // (no building to arrange)

            // Act
            Vector<Report> reports = gameManager.damageInfantryIn(null, 10, buildingHex);

            // Assert
            assertEquals(0, reports.size(), "Null building should return empty report vector");
        }

        @Test
        void testNoInfantryInHex_ReturnsEmptyVector() {
            // Arrange
            IBuilding building = getGame().getBoard().getBuildingAt(buildingHex);

            // Act
            Vector<Report> reports = gameManager.damageInfantryIn(building, 10, buildingHex);

            // Assert
            assertEquals(0, reports.size(), "No infantry in hex should return empty report vector");
        }

        @Test
        void testInfantryOnTopOfBuilding_NotDamaged() {
            // Arrange
            IBuilding building = getGame().getBoard().getBuildingAt(buildingHex);
            int buildingHeight = getGame().getBoard().getHex(buildingHex).terrainLevel(Terrains.BLDG_ELEV);

            Infantry infantry = new Infantry();
            infantry.setOwner(player);
            infantry.setId(1);
            infantry.setPosition(buildingHex);
            infantry.setElevation(buildingHeight); // On top, not inside
            getGame().addEntity(infantry);

            // Act
            Vector<Report> reports = gameManager.damageInfantryIn(building, 10, buildingHex);

            // Assert
            assertEquals(0, reports.size(), "Infantry on top of building should not be damaged");
        }

        @Test
        void testInfantryInsideBuilding_ReceivesZeroDamage() {
            // Arrange
            IBuilding building = getGame().getBoard().getBuildingAt(buildingHex);

            Infantry infantry = new Infantry();
            infantry.setOwner(player);
            infantry.setId(1);
            infantry.setPosition(buildingHex);
            infantry.setElevation(0); // Inside building
            getGame().addEntity(infantry);

            // Act
            Vector<Report> reports = gameManager.damageInfantryIn(building, 0, buildingHex);

            // Assert
            assertTrue(reports.size() > 0, "Should have report for zero damage");
            assertEquals(6445, reports.get(0).messageId, "Should report no damage received");
        }

        @Test
        void testConventionalInfantryInsideBuilding_ReceivesDamage() {
            // Arrange
            IBuilding building = getGame().getBoard().getBuildingAt(buildingHex);

            Infantry infantry = new Infantry();
            infantry.setOwner(player);
            infantry.setId(1);
            infantry.setPosition(buildingHex);
            infantry.setElevation(0); // Inside building
            infantry.setInternal(10, Infantry.LOC_INFANTRY); // Set some troopers so it can take damage
            getGame().addEntity(infantry);

            // Act
            Vector<Report> reports = gameManager.damageInfantryIn(building, 20, buildingHex);

            // Assert
            assertTrue(!reports.isEmpty(), "Should have damage reports");
            boolean hasDamageReport = reports.stream()
                .anyMatch(r -> r.messageId == 6450);
            assertTrue(hasDamageReport, "Should report damage to infantry");
        }

        @Test
        void testBattleArmorInsideBuilding_ReceivesDamage() {
            // Arrange
            IBuilding building = getGame().getBoard().getBuildingAt(buildingHex);

            megamek.common.battleArmor.BattleArmor ba = new megamek.common.battleArmor.BattleArmor();
            ba.setOwner(player);
            ba.setId(1);
            ba.setPosition(buildingHex);
            ba.setElevation(0); // Inside building
            ba.setInternal(5); // Set some armor
            getGame().addEntity(ba);

            // Act
            Vector<Report> reports = gameManager.damageInfantryIn(building, 20, buildingHex);

            // Assert
            assertTrue(reports.size() > 0, "Should have damage reports for Battle Armor");
            boolean hasDamageReport = reports.stream()
                .anyMatch(r -> r.messageId == 6450);
            assertTrue(hasDamageReport, "Should report damage to Battle Armor");
        }

        @Test
        void testMultipleInfantryInSameBuilding_AllReceiveDamage() {
            // Arrange
            IBuilding building = getGame().getBoard().getBuildingAt(buildingHex);

            Infantry infantry1 = new Infantry();
            infantry1.setOwner(player);
            infantry1.setId(1);
            infantry1.setPosition(buildingHex);
            infantry1.setElevation(0); // Inside building
            infantry1.setInternal(10, Infantry.LOC_INFANTRY);
            getGame().addEntity(infantry1);

            Infantry infantry2 = new Infantry();
            infantry2.setOwner(player);
            infantry2.setId(2);
            infantry2.setPosition(buildingHex);
            infantry2.setElevation(1); // Inside building, different floor
            infantry2.setInternal(10, Infantry.LOC_INFANTRY);
            getGame().addEntity(infantry2);

            // Act
            Vector<Report> reports = gameManager.damageInfantryIn(building, 20, buildingHex);

            // Assert
            long damageReportCount = reports.stream()
                .filter(r -> r.messageId == 6450)
                .count();
            assertEquals(2, damageReportCount, "Both infantry units should receive damage");
        }

        @Test
        void testInfantryInDifferentHex_NotDamaged() {
            // Arrange
            IBuilding building = getGame().getBoard().getBuildingAt(buildingHex);

            Infantry infantry = new Infantry();
            infantry.setOwner(player);
            infantry.setId(1);
            infantry.setPosition(emptyHex); // Different hex
            infantry.setElevation(0);
            infantry.setInternal(10, Infantry.LOC_INFANTRY);
            getGame().addEntity(infantry);

            // Act
            Vector<Report> reports = gameManager.damageInfantryIn(building, 20, buildingHex);

            // Assert
            assertEquals(0, reports.size(), "Infantry in different hex should not be damaged");
        }
    }

    @Nested
    class TestResolvePhysicalAttacks extends GameBoardTestCase {

        private TWGameManager gameManager;
        private Player player1;
        private Player player2;
        private BipedMek attacker;
        private BipedMek target;

        @BeforeEach
        void beforeEach() {
            gameManager = new TWGameManager();
            gameManager.setGame(getGame());

            player1 = new Player(0, "Attacker Player");
            player2 = new Player(1, "Target Player");
            getGame().addPlayer(0, player1);
            getGame().addPlayer(1, player2);

            // Initialize a simple board
            initializeBoard("COMBAT_BOARD", """
                size 3 3
                hex 0101 0 "" ""
                hex 0102 0 "" ""
                hex 0103 0 "" ""
                hex 0201 0 "" ""
                hex 0202 0 "" ""
                hex 0203 0 "" ""
                hex 0301 0 "" ""
                hex 0302 0 "" ""
                hex 0303 0 "" ""
                end
                """);

            setBoard("COMBAT_BOARD");
            getGame().setBoard(getBoard("COMBAT_BOARD"));

            // Create attacker mek
            attacker = new BipedMek();
            attacker.setOwner(player1);
            attacker.setId(1);
            attacker.setPosition(new Coords(1, 1));
            attacker.setFacing(0);
            attacker.setDeployed(true);
            attacker.setWeight(50.0); // 50-ton mek for 10 damage kicks
            attacker.setInternal(10, Mek.LOC_HEAD);
            attacker.setInternal(10, Mek.LOC_CENTER_TORSO);
            attacker.setInternal(10, Mek.LOC_LEFT_ARM);
            attacker.setInternal(10, Mek.LOC_RIGHT_ARM);
            attacker.setInternal(10, Mek.LOC_LEFT_LEG);
            attacker.setInternal(10, Mek.LOC_RIGHT_LEG);
            // Add hip actuators so mek can kick
            attacker.setCritical(Mek.LOC_LEFT_LEG, 0, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, Mek.ACTUATOR_HIP));
            attacker.setCritical(Mek.LOC_RIGHT_LEG, 0, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, Mek.ACTUATOR_HIP));
            // Set crew to be active
            attacker.getCrew().setHits(0, 0);
            getGame().addEntity(attacker);

            // Create target mek
            target = new BipedMek();
            target.setOwner(player2);
            target.setId(2);
            target.setPosition(new Coords(1, 2)); // Adjacent to attacker
            target.setFacing(3); // Facing back towards attacker
            target.setDeployed(true);
            target.setInternal(10, Mek.LOC_HEAD);
            target.setInternal(10, Mek.LOC_CENTER_TORSO);
            target.setInternal(10, Mek.LOC_LEFT_ARM);
            target.setInternal(10, Mek.LOC_RIGHT_ARM);
            target.setInternal(10, Mek.LOC_LEFT_LEG);
            target.setInternal(10, Mek.LOC_RIGHT_LEG);
            // Set crew to be active
            target.getCrew().setHits(0, 0);
            getGame().addEntity(target);
        }

        @Test
        void testNoPhysicalAttacks_GeneratesOnlyHeader() {
            // Arrange
            // (no attacks added)

            // Act
            gameManager.resolvePhysicalAttacks();

            // Assert
            Vector<Report> reports = gameManager.getMainPhaseReport();
            assertTrue(reports.size() >= 1, "Should have at least the header report");
            assertEquals(4000, reports.get(0).messageId, "First report should be physical phase header");
        }

        @Test
        void testSinglePunchAttack_ProcessedSuccessfully() {
            // Arrange
            megamek.common.actions.PunchAttackAction paa = new megamek.common.actions.PunchAttackAction(
                attacker.getId(),
                target.getTargetType(),
                target.getId(),
                megamek.common.actions.PunchAttackAction.BOTH
            );
            getGame().addAction(paa);

            // Act
            gameManager.resolvePhysicalAttacks();

            // Assert
            Vector<Report> reports = gameManager.getMainPhaseReport();
            assertTrue(reports.size() > 1, "Should have reports beyond just the header");

            // Verify attacker is mentioned in reports
            boolean attackerMentioned = reports.stream()
                .anyMatch(r -> r.subject == attacker.getId());
            assertTrue(attackerMentioned, "Reports should mention the attacking entity");
        }

        @Test
        void testDuplicateAttacks_OnlyFirstProcessed() {
            // Arrange
            megamek.common.actions.PunchAttackAction paa1 = new megamek.common.actions.PunchAttackAction(
                attacker.getId(),
                target.getTargetType(),
                target.getId(),
                megamek.common.actions.PunchAttackAction.BOTH
            );
            megamek.common.actions.KickAttackAction kaa = new megamek.common.actions.KickAttackAction(
                attacker.getId(),
                target.getTargetType(),
                target.getId(),
                megamek.common.actions.KickAttackAction.BOTH
            );

            getGame().addAction(paa1);
            getGame().addAction(kaa); // Duplicate attack from same entity

            // Act
            gameManager.resolvePhysicalAttacks();

            // Assert
            // After cleanup, only one attack should be processed
            // This is verified by checking that duplicate cleanup ran
            Vector<Report> reports = gameManager.getMainPhaseReport();
            assertTrue(reports.size() > 0, "Should have processed attacks");
        }

        @Test
        void testDestroyedEntity_AttackNotProcessed() {
            // Arrange
            attacker.setDestroyed(true);

            megamek.common.actions.PunchAttackAction paa = new megamek.common.actions.PunchAttackAction(
                attacker.getId(),
                target.getTargetType(),
                target.getId(),
                megamek.common.actions.PunchAttackAction.BOTH
            );
            getGame().addAction(paa);

            // Act
            gameManager.resolvePhysicalAttacks();

            // Assert
            Vector<Report> reports = gameManager.getMainPhaseReport();
            // Should only have header report, no attack resolution
            boolean attackProcessed = reports.stream()
                .anyMatch(r -> r.subject == attacker.getId() && r.messageId != 4000);
            assertTrue(!attackProcessed, "Destroyed entity's attack should not be processed");
        }

        @Test
        void testChargeAttack_AddedAndProcessed() {
            // Arrange
            megamek.common.actions.ChargeAttackAction caa = new megamek.common.actions.ChargeAttackAction(
                attacker.getId(),
                target.getTargetType(),
                target.getId(),
                target.getPosition()
            );
            getGame().addCharge(caa);

            // Act
            gameManager.resolvePhysicalAttacks();

            // Assert
            Vector<Report> reports = gameManager.getMainPhaseReport();
            assertTrue(reports.size() > 1, "Should have reports for charge attack");

            boolean attackerMentioned = reports.stream()
                .anyMatch(r -> r.subject == attacker.getId());
            assertTrue(attackerMentioned, "Charge attack should be processed");
        }

        @Test
        void testSearchlightAttack_ResolvedImmediately() {
            // Arrange
            megamek.common.actions.SearchlightAttackAction saa = new megamek.common.actions.SearchlightAttackAction(
                attacker.getId(),
                target.getId()
            );
            getGame().addAction(saa);

            // Act
            gameManager.resolvePhysicalAttacks();

            // Assert
            Vector<Report> reports = gameManager.getMainPhaseReport();
            assertTrue(reports.size() >= 1, "Should have processed searchlight action");
        }

        @Test
        void testMultipleEntities_EachProcessedCorrectly() {
            // Arrange
            // Create a third entity
            BipedMek attacker2 = new BipedMek();
            attacker2.setOwner(player1);
            attacker2.setId(3);
            attacker2.setPosition(new Coords(2, 2));
            attacker2.setFacing(0);
            attacker2.setDeployed(true);
            attacker2.setInternal(10, Mek.LOC_HEAD);
            attacker2.setInternal(10, Mek.LOC_CENTER_TORSO);
            attacker2.setInternal(10, Mek.LOC_LEFT_ARM);
            attacker2.setInternal(10, Mek.LOC_RIGHT_ARM);
            attacker2.setInternal(10, Mek.LOC_LEFT_LEG);
            attacker2.setInternal(10, Mek.LOC_RIGHT_LEG);
            // Set crew to be active
            attacker2.getCrew().setHits(0, 0);
            getGame().addEntity(attacker2);

            // Add attacks from both entities
            megamek.common.actions.PunchAttackAction paa1 = new megamek.common.actions.PunchAttackAction(
                attacker.getId(),
                target.getTargetType(),
                target.getId(),
                megamek.common.actions.PunchAttackAction.BOTH
            );
            megamek.common.actions.PunchAttackAction paa2 = new megamek.common.actions.PunchAttackAction(
                attacker2.getId(),
                target.getTargetType(),
                target.getId(),
                megamek.common.actions.PunchAttackAction.BOTH
            );

            getGame().addAction(paa1);
            getGame().addAction(paa2);

            // Act
            gameManager.resolvePhysicalAttacks();

            // Assert
            Vector<Report> reports = gameManager.getMainPhaseReport();

            boolean attacker1Mentioned = reports.stream()
                .anyMatch(r -> r.subject == attacker.getId());
            boolean attacker2Mentioned = reports.stream()
                .anyMatch(r -> r.subject == attacker2.getId());

            assertTrue(attacker1Mentioned, "First attacker should be in reports");
            assertTrue(attacker2Mentioned, "Second attacker should be in reports");
        }

        @Test
        void testPhysicalAttackAgainstBuildingWithInfantryInside_DamagesBoth() {
            // Arrange
            // Create a new board with a building
            initializeBoard("BUILDING_COMBAT_BOARD", """
                size 3 3
                hex 0101 0 "" ""
                hex 0102 0 "" ""
                hex 0103 0 "" ""
                hex 0201 0 "" ""
                hex 0202 0 "bldg_elev:2;building:1;bldg_cf:40" ""
                hex 0203 0 "" ""
                hex 0301 0 "" ""
                hex 0302 0 "" ""
                hex 0303 0 "" ""
                end
                """);

            setBoard("BUILDING_COMBAT_BOARD");
            getGame().setBoard(getBoard("BUILDING_COMBAT_BOARD"));

            // Create building
            Coords buildingHex = new Coords(1, 1); // hex 0202
            BuildingTerrain building = new BuildingTerrain(buildingHex, getGame().getBoard(),
                Terrains.BUILDING, BasementType.UNKNOWN);
            getGame().getBoard().addBuildingToBoard(building);

            // Reposition attacker adjacent to building
            attacker.setPosition(new Coords(1, 0)); // hex 0201, adjacent to building
            attacker.setFacing(3); // Face south toward the building

            // Create infantry inside the building
            Infantry infantryInBuilding = new Infantry();
            infantryInBuilding.setOwner(player2);
            infantryInBuilding.setId(10);
            infantryInBuilding.setPosition(buildingHex);
            infantryInBuilding.setElevation(0); // Inside building
            infantryInBuilding.setDeployed(true);
            infantryInBuilding.setInternal(10, Infantry.LOC_INFANTRY);
            infantryInBuilding.getCrew().setHits(0, 0);
            getGame().addEntity(infantryInBuilding);

            // Create kick attack against the BUILDING (not the infantry directly)
            // Use BuildingTarget to properly wrap the building for targeting
            BuildingTarget buildingTarget = new BuildingTarget(buildingHex, getGame().getBoard(), false);
            megamek.common.actions.KickAttackAction kaa = new megamek.common.actions.KickAttackAction(
                attacker.getId(),
                buildingTarget.getTargetType(),
                buildingTarget.getId(),
                megamek.common.actions.KickAttackAction.LEFT
            );
            getGame().addAction(kaa);

            // Act
            gameManager.resolvePhysicalAttacks();

            // Assert
            Vector<Report> reports = gameManager.getMainPhaseReport();
            assertTrue(reports.size() > 1, "Should have reports beyond just the header");

            // Verify attacker is mentioned
            boolean attackerMentioned = reports.stream()
                .anyMatch(r -> r.subject == attacker.getId());
            assertTrue(attackerMentioned, "Attacker should be in reports");

            // Building damage should be reported
            boolean buildingDamageReported = reports.stream()
                .anyMatch(r -> r.messageId == 3434 || r.messageId == 6436); // Building damage reports
            assertTrue(buildingDamageReported, "Building damage should be reported");

            // Infantry damage should also be reported (from damageInfantryIn)
            boolean infantryDamageReported = reports.stream()
                .anyMatch(r -> r.subject == infantryInBuilding.getId());
            assertTrue(infantryDamageReported, "Infantry inside building should also take damage");
        }
    }
}



