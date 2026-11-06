/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
package megamek.server.nuclear;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;

import megamek.common.Hex;
import megamek.common.HitData;
import megamek.common.Player;
import megamek.common.Report;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.enums.NuclearWeaponType;
import megamek.common.game.Game;
import megamek.common.loaders.MekFileParser;
import megamek.common.net.packets.Packet;
import megamek.common.options.GameOptions;
import megamek.common.rolls.PilotingRollData;
import megamek.common.rolls.Roll;
import megamek.common.units.Aero;
import megamek.common.weapons.DamageType;
import megamek.server.Server;
import megamek.server.totalWarfare.TWGameManager;
import megamek.utils.ServerFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

/**
 * Nuclear missiles hitting spacecraft (IO:AE p.166), on real units with the dice forced.
 */
class NuclearSpaceHitTest {
    private static final String UNIT_FILES = "testresources/megamek/common/units/";
    private static final int BOARD_SIZE = 10;

    private TWGameManager gameManager;
    private Game game;
    private Server server;
    private Player player;

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void beforeEach() throws Exception {
        gameManager = Mockito.spy(new TWGameManager());
        Mockito.doNothing().when(gameManager).send(any(Packet.class));
        Mockito.doNothing().when(gameManager).send(anyInt(), any(Packet.class));
        Mockito.doNothing().when(gameManager).entityUpdate(anyInt());
        game = gameManager.getGame();
        game.setOptions(new GameOptions());
        server = ServerFactory.createServer(gameManager);
        player = new Player(0, "Test");
        game.addPlayer(0, player);

        Hex[] hexes = new Hex[BOARD_SIZE * BOARD_SIZE];
        for (int index = 0; index < hexes.length; index++) {
            hexes[index] = new Hex(0);
        }
        Board board = new Board();
        board.newData(BOARD_SIZE, BOARD_SIZE, hexes, null);
        game.setBoard(board);
    }

    @AfterEach
    void afterEach() {
        server.die();
    }

    private Aero placeShip(String fileName) throws Exception {
        Aero ship = (Aero) new MekFileParser(new File(UNIT_FILES + fileName)).getEntity();
        ship.setId(game.getNextEntityId());
        game.addEntity(ship);
        ship.setOwner(player);
        ship.setPosition(new Coords(5, 5));
        ship.setDeployed(true);
        return ship;
    }

    private static Roll fixedRoll(int value) {
        Roll roll = mock(Roll.class);
        when(roll.getIntValue()).thenReturn(value);
        when(roll.getReport()).thenReturn(String.valueOf(value));
        return roll;
    }

    private static HitData nuclearHit(NuclearWeaponType... warheads) {
        HitData hit = new HitData(Aero.LOC_NOSE);
        hit.setCapital(true);
        hit.setNuclearWarheads(List.of(warheads));
        return hit;
    }

    /** Resolves the special critical hits with every 2d6 roll forced to {@code value}. */
    private Vector<Report> resolveWithRoll(Aero ship, HitData hit, int value) {
        Vector<Report> reports = new Vector<>();
        Roll roll = fixedRoll(value);
        try (MockedStatic<Compute> compute = Mockito.mockStatic(Compute.class, Mockito.CALLS_REAL_METHODS)) {
            compute.when(() -> Compute.rollD6(2)).thenReturn(roll);
            new NuclearSpaceHit(gameManager).resolve(ship, hit, reports);
        }
        return reports;
    }

    private int nuclearControlRolls(Aero ship) {
        int rolls = 0;
        for (Enumeration<PilotingRollData> controlRolls = game.getControlRolls(); controlRolls.hasMoreElements(); ) {
            PilotingRollData controlRoll = controlRolls.nextElement();
            if ((controlRoll.getEntityId() == ship.getId()) && controlRoll.getPlainDesc().contains("Nuclear attack")) {
                assertEquals(NuclearSpaceHit.CONTROL_ROLL_MODIFIER, controlRoll.getValue());
                rolls++;
            }
        }
        return rolls;
    }

    private static int countReports(Vector<Report> reports, int messageId) {
        int count = 0;
        for (Report report : reports) {
            if (report.messageId == messageId) {
                count++;
            }
        }
        return count;
    }

    @Test
    void specialCriticalIsTenTimesTheCapitalDamage() {
        assertEquals(100, NuclearSpaceHit.specialCriticalDamage(NuclearWeaponType.ALAMO));
        assertEquals(1_000, NuclearSpaceHit.specialCriticalDamage(NuclearWeaponType.SANTA_ANA));
        assertEquals(10_000, NuclearSpaceHit.specialCriticalDamage(NuclearWeaponType.PEACEMAKER));
    }

    @Test
    void missedCriticalLeavesTheStructureAndQueuesTheControlRoll() throws Exception {
        Aero union = placeShip("Union (3055).blk");
        int structureBefore = union.getSI();

        // The Alamo needs 10; a 9 misses
        Vector<Report> reports = resolveWithRoll(union, nuclearHit(NuclearWeaponType.ALAMO), 9);

        assertEquals(structureBefore, union.getSI());
        assertEquals(1, countReports(reports, 9147), "the roll reports no additional effect");
        assertEquals(1, nuclearControlRolls(union), "a survivor makes the +4 Control Roll");
    }

    @Test
    void criticalThatDestroysTheShipQueuesNoControlRoll() throws Exception {
        Aero aegis = placeShip("Aegis Heavy Cruiser (2843).blk");

        // The Santa Ana needs 9; its 1,000 points tear through the 75 SI of the Aegis
        resolveWithRoll(aegis, nuclearHit(NuclearWeaponType.SANTA_ANA), 9);

        assertEquals(0, aegis.getSI());
        assertTrue(aegis.isDoomed() || aegis.isDestroyed());
        assertEquals(0, nuclearControlRolls(aegis), "a destroyed ship makes no Control Roll");
    }

    @Test
    void eachMissileInAMixedBayRollsItsOwnTarget() throws Exception {
        Aero aegis = placeShip("Aegis Heavy Cruiser (2843).blk");

        // One roll of 9 for each missile: the Alamo (needs 10) misses, the Santa Ana (needs 9) hits
        Vector<Report> reports = resolveWithRoll(aegis,
              nuclearHit(NuclearWeaponType.ALAMO, NuclearWeaponType.SANTA_ANA), 9);

        assertEquals(2, countReports(reports, 9145), "each warhead rolls");
        assertEquals(1, countReports(reports, 9147), "the Alamo misses");
        assertEquals(1, countReports(reports, 9146), "the Santa Ana hits");
        assertEquals(1_000, aegis.damageThisPhase, "only the Santa Ana's special critical lands");
    }

    @Test
    void warheadsAfterTheShipIsDestroyedDoNotRoll() throws Exception {
        Aero union = placeShip("Union (3055).blk");

        Vector<Report> reports = resolveWithRoll(union,
              nuclearHit(NuclearWeaponType.SANTA_ANA, NuclearWeaponType.SANTA_ANA), 12);

        assertEquals(1, countReports(reports, 9145), "the second missile finds nothing left to hit");
        assertEquals(2, countReports(reports, 6564), "the game log still names both warheads");
        assertEquals(1_000, union.damageThisPhase);
    }

    @Test
    void warheadIsNamedWhenTheOrdinaryDamageAlreadyDestroyedTheShip() throws Exception {
        Aero union = placeShip("Union (3055).blk");
        union.setDoomed(true);

        Vector<Report> reports = resolveWithRoll(union, nuclearHit(NuclearWeaponType.SANTA_ANA), 12);

        assertEquals(1, countReports(reports, 6564), "the player still sees the attack was nuclear");
        assertEquals(0, countReports(reports, 9145), "no special critical hit on a destroyed ship");
        assertEquals(0, nuclearControlRolls(union));
    }

    @Test
    void everyUnitInTheDetonationHexSuffersEmi() throws Exception {
        Aero aegis = placeShip("Aegis Heavy Cruiser (2843).blk");
        Aero escort = placeShip("Union (3055).blk");
        Aero distantShip = placeShip("Union (3055).blk");
        distantShip.setPosition(new Coords(2, 2));

        // Even a missed special critical leaves the hex flooded with interference
        Vector<Report> reports = resolveWithRoll(aegis, nuclearHit(NuclearWeaponType.ALAMO), 2);

        assertTrue(aegis.isSufferingEMI(), "the target");
        assertTrue(escort.isSufferingEMI(), "a ship sharing the target's hex");
        assertFalse(distantShip.isSufferingEMI(), "a ship in another hex");
        assertEquals(2, countReports(reports, 6565));
    }

    @Test
    void nuclearDamageThroughArmorIsHalvedIntoTheStructure() throws Exception {
        Aero union = placeShip("Union (3055).blk");
        int noseArmor = union.getArmor(Aero.LOC_NOSE);
        int structureBefore = union.getSI();
        HitData hit = nuclearHit(NuclearWeaponType.ALAMO);
        hit.setCapital(false);

        // Ten points past the nose armor halve to five off the SI; every roll is a 2, so no critical lands
        Roll roll = fixedRoll(2);
        try (MockedStatic<Compute> compute = Mockito.mockStatic(Compute.class, Mockito.CALLS_REAL_METHODS)) {
            compute.when(() -> Compute.rollD6(2)).thenReturn(roll);
            gameManager.damageEntity(union, hit, noseArmor + 10, false, DamageType.NONE, false, false, false,
                  false, true);
        }

        assertEquals(structureBefore - 5, union.getSI());
        assertFalse(union.isDoomed());
        assertEquals(1, nuclearControlRolls(union));
    }
}
