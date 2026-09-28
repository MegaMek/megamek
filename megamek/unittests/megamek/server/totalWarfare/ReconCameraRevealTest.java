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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;

import megamek.common.Player;
import megamek.common.Report;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.enums.GamePhase;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.Mounted;
import megamek.common.exceptions.LocationFullException;
import megamek.common.game.Game;
import megamek.common.net.packets.Packet;
import megamek.common.options.OptionsConstants;
import megamek.common.rolls.Roll;
import megamek.common.units.Aero;
import megamek.common.units.AeroSpaceFighter;
import megamek.common.units.BipedMek;
import megamek.common.units.Crew;
import megamek.common.units.CrewType;
import megamek.common.units.ReconCameraRules;
import megamek.utils.BoardLoader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * The aerospace Recon Camera's reveal pass at the end of the Movement phase (TO:AUE p.150): each hostile hidden unit on
 * the ground map below the camera's flight is revealed when its owner rolls 9 plus terrain or more, and flying in Reveal
 * mode uses the camera for the turn.
 */
class ReconCameraRevealTest {

    private static final int GROUND_BOARD = 0;
    private static final int SKY_BOARD = 1;
    private static final Coords SKY_HEX_OVER_GROUND = new Coords(3, 3);

    private TWGameManager gameManager;
    private Game game;
    private Player enemy;
    private AeroSpaceFighter fighter;
    private BipedMek hiddenEnemy;

    /** The handler with the reveal dice decided by the test. */
    private static final class HandlerUnderTest extends ReconCameraHandler {
        private final int nextRoll;

        HandlerUnderTest(TWGameManager gameManager, int nextRoll) {
            super(gameManager);
            this.nextRoll = nextRoll;
        }

        @Override
        Roll rollReveal() {
            return new FixedRoll(nextRoll);
        }
    }

    /** Two dice that always show the same total. */
    private static final class FixedRoll extends Roll {
        private final int value;

        FixedRoll(int value) {
            super(2, 1);
            this.value = value;
        }

        @Override
        public int getIntValue() {
            return value;
        }

        @Override
        public String toString() {
            return String.valueOf(value);
        }

        @Override
        public String getReport() {
            return String.valueOf(value);
        }

        @Override
        public int[] getIntValues() {
            return new int[] { value };
        }
    }

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void setUp() throws LocationFullException {
        gameManager = Mockito.spy(new TWGameManager());
        Mockito.doNothing().when(gameManager).send(any(Packet.class));
        Mockito.doNothing().when(gameManager).send(Mockito.anyInt(), any(Packet.class));
        Mockito.doNothing().when(gameManager).sendServerChat(Mockito.anyInt(), Mockito.anyString());
        game = gameManager.getGame();
        Player owner = new Player(0, "Camera side");
        owner.setTeam(1);
        enemy = new Player(1, "Enemy");
        enemy.setTeam(2);
        game.addPlayer(0, owner);
        game.addPlayer(1, enemy);
        Board groundBoard = BoardLoader.initializeBoard("""
              size 16 17
              end""");
        Board skyBoard = Board.getSkyBoard(8, 8);
        groundBoard.setBoardId(GROUND_BOARD);
        skyBoard.setBoardId(SKY_BOARD);
        game.setBoard(GROUND_BOARD, groundBoard);
        game.setBoard(SKY_BOARD, skyBoard);
        skyBoard.setEmbeddedBoard(GROUND_BOARD, SKY_HEX_OVER_GROUND);
        groundBoard.setEnclosingBoard(SKY_BOARD);
        game.setPhase(GamePhase.MOVEMENT);

        fighter = new AeroSpaceFighter();
        fighter.setGame(game);
        fighter.setId(game.getNextEntityId());
        fighter.setChassis("Spotter");
        fighter.setModel("S-1");
        fighter.setCrew(new Crew(CrewType.SINGLE));
        fighter.setOwner(owner);
        fighter.setBoardId(SKY_BOARD);
        fighter.setPosition(SKY_HEX_OVER_GROUND);
        fighter.setAltitude(7);
        fighter.setDeployed(true);
        fighter.addEquipment(EquipmentType.get("ISReconCamera"), Aero.LOC_NOSE);
        game.addEntity(fighter);

        hiddenEnemy = new BipedMek();
        hiddenEnemy.setGame(game);
        hiddenEnemy.setId(game.getNextEntityId());
        hiddenEnemy.setChassis("Ambusher");
        hiddenEnemy.setModel("A-1");
        hiddenEnemy.setCrew(new Crew(CrewType.SINGLE));
        hiddenEnemy.setOwner(enemy);
        hiddenEnemy.setBoardId(GROUND_BOARD);
        hiddenEnemy.setPosition(new Coords(6, 6));
        hiddenEnemy.setDeployed(true);
        hiddenEnemy.setHidden(true);
        game.addEntity(hiddenEnemy);
    }

    private void setRevealMode() {
        for (Mounted<?> mounted : fighter.getMisc()) {
            mounted.setMode(ReconCameraRules.MODE_REVEAL);
        }
    }

    @Test
    void testAGoodRollRevealsTheHiddenUnit() {
        setRevealMode();

        new HandlerUnderTest(gameManager, 9).revealHiddenUnits();

        assertFalse(hiddenEnemy.isHidden(), "9 meets the 9 needed in the open");
        assertTrue(ReconCameraRules.forbidsOtherAttacks(fighter), "revealing uses the camera: no other attacks");
    }

    @Test
    void testALowRollLeavesTheUnitHidden() {
        setRevealMode();

        new HandlerUnderTest(gameManager, 8).revealHiddenUnits();

        assertTrue(hiddenEnemy.isHidden());
        assertTrue(fighter.hasReconCameraSpotThisTurn(), "the attempt still uses the camera for the turn");
    }

    @Test
    void testAFailedRollIsToldOnlyToTheHiddenUnitsOwner() {
        setRevealMode();

        new HandlerUnderTest(gameManager, 2).revealHiddenUnits();

        assertOnlyTheOwnerIsTold();
    }

    @Test
    void testAFailedRollIsToldOnlyToTheHiddenUnitsOwnerUnderDoubleBlind() {
        // under double-blind the other players would get an obscured copy of a report, so it must not be a report
        game.getOptions().getOption(OptionsConstants.ADVANCED_DOUBLE_BLIND).setValue(true);
        setRevealMode();

        new HandlerUnderTest(gameManager, 2).revealHiddenUnits();

        assertOnlyTheOwnerIsTold();
    }

    private void assertOnlyTheOwnerIsTold() {
        Mockito.verify(gameManager).sendServerChat(Mockito.eq(enemy.getId()), Mockito.anyString());
        Mockito.verify(gameManager, Mockito.never()).sendServerChat(Mockito.eq(fighter.getOwnerId()),
              Mockito.anyString());
        for (Report report : gameManager.getMainPhaseReport()) {
            assertNotEquals(ReconCameraHandler.REPORT_CAMERA_REVEALED, report.messageId,
                  "a failed roll reveals nothing and is not reported to the table");
        }
    }

    @Test
    void testAFighterInSpotModeRevealsNothing() {
        new HandlerUnderTest(gameManager, 12).revealHiddenUnits();

        assertTrue(hiddenEnemy.isHidden());
        assertFalse(fighter.hasReconCameraSpotThisTurn(), "the camera is still free to spot");
    }

    @Test
    void testAFighterAboveAltitudeTenRevealsNothing() {
        setRevealMode();
        fighter.setAltitude(11);

        new HandlerUnderTest(gameManager, 12).revealHiddenUnits();

        assertTrue(hiddenEnemy.isHidden());
        assertFalse(ReconCameraRules.forbidsOtherAttacks(fighter), "no reveal attempt, so it may still attack");
    }

    @Test
    void testTheRevealPassWithRealDiceUsesTheCamera() {
        setRevealMode();
        // real dice: the outcome varies, so check only that flying in Reveal mode used the camera
        new ReconCameraHandler(gameManager).revealHiddenUnits();

        assertTrue(fighter.hasReconCameraSpotThisTurn());
    }
}
