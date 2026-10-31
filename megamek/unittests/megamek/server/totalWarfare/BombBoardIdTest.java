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
package megamek.server.totalWarfare;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;

import java.util.Vector;
import java.util.stream.Stream;

import megamek.common.Hex;
import megamek.common.HexTarget;
import megamek.common.Player;
import megamek.common.Report;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.enums.BombType.BombTypeEnum;
import megamek.common.game.Game;
import megamek.common.net.packets.Packet;
import megamek.common.options.OptionsConstants;
import megamek.common.units.AeroSpaceFighter;
import megamek.common.units.BipedMek;
import megamek.common.units.Crew;
import megamek.common.units.CrewType;
import megamek.common.units.Targetable;
import megamek.common.units.Terrains;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Fuel-air and inferno bombs resolve on the board they were dropped on, not on the first board (issue #9315). Two
 * identical boards each hold a Mek at the same hex; a bomb dropped on the second board must affect only that board.
 */
class BombBoardIdTest {

    private static final int SECOND_BOARD = 1;
    private static final Coords TARGET_HEX = new Coords(4, 4);

    private TWGameManager gameManager;
    private Game game;
    private AeroSpaceFighter bomber;
    private BipedMek firstBoardMek;
    private BipedMek secondBoardMek;

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
        game.setBoard(0, board(0));
        game.setBoard(SECOND_BOARD, board(SECOND_BOARD));
        game.getOptions().getOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_START_FIRE).setValue(true);

        Player attackerOwner = new Player(0, "Attacker");
        Player defenderOwner = new Player(1, "Defender");
        attackerOwner.setTeam(1);
        defenderOwner.setTeam(2);
        game.addPlayer(0, attackerOwner);
        game.addPlayer(1, defenderOwner);

        bomber = new AeroSpaceFighter();
        bomber.setGame(game);
        bomber.setId(game.getNextEntityId());
        bomber.setChassis("Test Fighter");
        bomber.setModel("Bomber");
        bomber.setCrew(new Crew(CrewType.SINGLE));
        bomber.setOwner(attackerOwner);
        bomber.setWeight(50);
        game.addEntity(bomber);
        bomber.setPosition(new Coords(4, 2));
        bomber.setBoardId(SECOND_BOARD);
        bomber.setAltitude(3);
        bomber.setDeployed(true);

        firstBoardMek = mek(defenderOwner, "First Board", 0);
        secondBoardMek = mek(defenderOwner, "Second Board", SECOND_BOARD);
    }

    private static Board board(int boardId) {
        int width = 16;
        int height = 17;
        Board board = new Board(width, height,
              Stream.generate(Hex::new).limit((long) width * height).toArray(Hex[]::new));
        board.setBoardId(boardId);
        return board;
    }

    private BipedMek mek(Player owner, String model, int boardId) {
        BipedMek mek = new BipedMek();
        mek.setGame(game);
        mek.setId(game.getNextEntityId());
        mek.setChassis("Test Mek");
        mek.setModel(model);
        mek.setCrew(new Crew(CrewType.SINGLE));
        mek.setOwner(owner);
        mek.setWeight(50.0);
        mek.autoSetInternal();
        for (int location = 0; location < mek.locations(); location++) {
            mek.initializeArmor(20, location);
        }
        game.addEntity(mek);
        mek.setPosition(TARGET_HEX);
        mek.setBoardId(boardId);
        mek.setFacing(0);
        mek.setDeployed(true);
        return mek;
    }

    @Test
    void fuelAirBombHitsOnlyTheBoardItWasDroppedOn() {
        int firstBoardArmor = firstBoardMek.getTotalArmor();
        int secondBoardArmor = secondBoardMek.getTotalArmor();
        HexTarget target = new HexTarget(TARGET_HEX, SECOND_BOARD, Targetable.TYPE_HEX_AERO_BOMB);

        Vector<Integer> hit = gameManager.deliverBombDamage(target, BombTypeEnum.FAE_LARGE, bomber.getId(), bomber,
              new Vector<Report>());

        assertTrue(hit.contains(secondBoardMek.getId()), "the Mek under the bomb is caught in the blast");
        assertFalse(hit.contains(firstBoardMek.getId()), "the Mek on the other board is not");
        assertTrue(secondBoardMek.getTotalArmor() < secondBoardArmor, "the Mek under the bomb takes damage");
        assertEquals(firstBoardArmor, firstBoardMek.getTotalArmor(), "the Mek on the other board is untouched");
    }

    @Test
    void infernoBombBurnsOnlyTheBoardItWasDroppedOn() {
        Vector<Integer> hit = gameManager.deliverBombInferno(TARGET_HEX, SECOND_BOARD, bomber, bomber.getId(),
              new Vector<Report>());

        assertTrue(hit.contains(secondBoardMek.getId()), "the Mek under the bomb is hit by the inferno");
        assertFalse(hit.contains(firstBoardMek.getId()), "the Mek on the other board is not");
        assertTrue(game.getBoard(SECOND_BOARD).getHex(TARGET_HEX).containsTerrain(Terrains.FIRE),
              "the hex under the bomb is on fire");
        assertFalse(game.getBoard(0).getHex(TARGET_HEX).containsTerrain(Terrains.FIRE),
              "the same hex on the other board is not");
    }
}
