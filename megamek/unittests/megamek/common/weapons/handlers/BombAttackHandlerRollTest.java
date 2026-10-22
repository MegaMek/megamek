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
package megamek.common.weapons.handlers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.util.HashMap;
import java.util.Vector;

import megamek.common.GameBoardTestCase;
import megamek.common.HexTarget;
import megamek.common.Player;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.board.Coords;
import megamek.common.enums.GamePhase;
import megamek.common.equipment.BombLoadout;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.WeaponMounted;
import megamek.common.equipment.enums.BombType.BombTypeEnum;
import megamek.common.game.Game;
import megamek.common.net.packets.Packet;
import megamek.common.rolls.Roll;
import megamek.common.units.AeroSpaceFighter;
import megamek.common.units.Crew;
import megamek.common.units.CrewType;
import megamek.common.units.Targetable;
import megamek.common.weapons.attacks.DiveBombAttack;
import megamek.server.totalWarfare.TWGameManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Every bomb in a run rolls its own to-hit, including the bomb after one that scatters off the board (issue #9314).
 */
class BombAttackHandlerRollTest extends GameBoardTestCase {

    private static final int REPORT_BOMB_HIT = 3190;
    private static final int REPORT_BOMB_MISSED = 3196;
    private static final int REPORT_BOMB_OFF_BOARD = 6699;
    /** A miss against 11 by 9: any scatter from the centre of a 3 by 3 board leaves it. */
    private static final int TARGET_NUMBER = 11;

    static {
        initializeBoard("BOMB_ROLL_BOARD", """
              size 3 3
              hex 0101 0 "" ""
              hex 0201 0 "" ""
              hex 0301 0 "" ""
              hex 0102 0 "" ""
              hex 0202 0 "" ""
              hex 0302 0 "" ""
              hex 0103 0 "" ""
              hex 0203 0 "" ""
              hex 0303 0 "" ""
              end"""
        );
    }

    private TWGameManager gameManager;
    private Game game;
    private AeroSpaceFighter bomber;
    private Crew crew;

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void beforeEach() {
        gameManager = Mockito.spy(new TWGameManager());
        Mockito.doNothing().when(gameManager).send(any(Packet.class));
        Mockito.doNothing().when(gameManager).entityUpdate(anyInt());
        game = gameManager.getGame();
        game.setBoard(getBoard("BOMB_ROLL_BOARD"));
        Player owner = new Player(0, "Bomber");
        owner.setTeam(1);
        game.addPlayer(0, owner);

        bomber = new AeroSpaceFighter();
        bomber.setGame(game);
        bomber.setId(game.getNextEntityId());
        bomber.setChassis("Test Fighter");
        bomber.setModel("Bomber");
        crew = Mockito.spy(new Crew(CrewType.SINGLE));
        bomber.setCrew(crew);
        bomber.setOwner(owner);
        bomber.setWeight(50);
        bomber.autoSetMaxBombPoints();
        BombLoadout loadout = new BombLoadout();
        loadout.put(BombTypeEnum.HE, 3);
        bomber.setExtBombChoices(loadout);
        bomber.applyBombs();
        game.addEntity(bomber);
        bomber.setPosition(new Coords(1, 1));
        bomber.setAltitude(3);
        bomber.setDeployed(true);
        // Bomb attacks (dive, altitude) are added at the start of each round from the bombs carried
        bomber.newRound(1);
    }

    private static Roll rollOf(int value) {
        Roll roll = mock(Roll.class);
        doReturn(value).when(roll).getIntValue();
        doReturn("").when(roll).getReport();
        return roll;
    }

    private Vector<Report> diveBombThreeHE() throws Exception {
        WeaponMounted diveBomb = null;
        for (WeaponMounted weapon : bomber.getWeaponList()) {
            if (weapon.getType() instanceof DiveBombAttack) {
                diveBomb = weapon;
            }
        }
        assertNotNull(diveBomb, "the loaded fighter can dive bomb");

        HexTarget hex = new HexTarget(new Coords(1, 1), 0, Targetable.TYPE_HEX_AERO_BOMB);
        WeaponAttackAction attack = new WeaponAttackAction(bomber.getId(), Targetable.TYPE_HEX_AERO_BOMB,
              hex.getId(), bomber.getEquipmentNum(diveBomb));
        BombLoadout payload = new BombLoadout();
        payload.put(BombTypeEnum.HE, 3);
        HashMap<String, BombLoadout> payloads = new HashMap<>();
        payloads.put("internal", new BombLoadout());
        payloads.put("external", payload);
        attack.setBombPayloads(payloads);

        BombAttackHandler handler = new BombAttackHandler(new ToHitData(TARGET_NUMBER, "test"), attack, game,
              gameManager);
        Vector<Report> reports = new Vector<>();
        handler.handle(GamePhase.FIRING, reports);
        return reports;
    }

    private static int count(Vector<Report> reports, int messageId) {
        int found = 0;
        for (Report report : reports) {
            if (report.messageId == messageId) {
                found++;
            }
        }
        return found;
    }

    /**
     * The first bomb rolls 2 and scatters off the board; the next two roll 12 and hit. Before the fix the off-board
     * miss skipped the re-roll, so all three bombs used the 2.
     */
    @Test
    void bombAfterAnOffBoardMissRollsItsOwnDice() throws Exception {
        doReturn(rollOf(2), rollOf(12), rollOf(12)).when(crew).rollGunnerySkill(any(), any());

        Vector<Report> reports = diveBombThreeHE();

        assertEquals(1, count(reports, REPORT_BOMB_OFF_BOARD), "only the first bomb leaves the board");
        assertEquals(1, count(reports, REPORT_BOMB_MISSED), "only the first bomb misses");
        assertEquals(2, count(reports, REPORT_BOMB_HIT), "the second and third bombs roll 12 and hit");
    }
}
