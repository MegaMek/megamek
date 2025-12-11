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
import megamek.common.Hex;
import megamek.common.Player;
import megamek.common.bays.CargoBay;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.enums.MoveStepType;
import megamek.common.equipment.EquipmentType;
import megamek.common.game.Game;
import megamek.common.moves.MovePath;
import megamek.common.rolls.PilotingRollData;
import megamek.common.units.AeroSpaceFighter;
import megamek.common.units.BipedMek;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.Infantry;
import megamek.common.units.LandAirMek;
import megamek.common.units.Mek;
import megamek.common.units.VTOL;
import megamek.server.Server;
import megamek.utils.ServerFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
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
}
