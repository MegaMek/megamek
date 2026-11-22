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
package megamek.client.bot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Vector;

import megamek.client.bot.princess.BehaviorSettings;
import megamek.common.Hex;
import megamek.common.Player;
import megamek.common.board.Board;
import megamek.common.board.BoardLocation;
import megamek.common.board.Coords;
import megamek.common.enums.GamePhase;
import megamek.common.enums.MoveStepType;
import megamek.common.event.player.GamePlayerChatEvent;
import megamek.common.game.GameTurn;
import megamek.common.moves.MovePath;
import megamek.common.moves.MoveStep;
import megamek.common.turns.SpecificEntityTurn;
import megamek.common.units.BipedMek;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import org.junit.jupiter.api.Test;

class BotClientTest {

    /** A concrete BotClient with every strategy hook stubbed out, so phase plumbing can run alone. */
    private static final class RecordingBotClient extends BotClient {

        private boolean dismissedItself = false;
        private int unitMovesAsked = 0;
        private int movesReported = 0;

        RecordingBotClient() {
            super("tester", "localhost", 0);
        }

        @Override
        public synchronized void die() {
            dismissedItself = true;
        }

        @Override
        public void initialize() {
        }

        @Override
        protected void processChat(GamePlayerChatEvent gamePlayerChatEvent) {
        }

        @Override
        protected void initMovement() {
        }

        @Override
        protected void initFiring() {
        }

        @Override
        protected MovePath calculateMoveTurn() {
            return null;
        }

        @Override
        protected void calculateFiringTurn() {
        }

        @Override
        protected void calculateDeployment() {
        }

        @Override
        public void setBehaviorSettings(BehaviorSettings behaviorSettings) {
        }

        @Override
        protected PhysicalOption calculatePhysicalTurn() {
            return null;
        }

        @Override
        protected void calculatePreEndDeclarationsTurn() {
        }

        @Override
        protected MovePath continueMovementFor(Entity entity) {
            unitMovesAsked++;
            return null;
        }

        @Override
        protected void onMovePathChosen(MovePath path) {
            movesReported++;
        }

        @Override
        protected Vector<BoardLocation> calculateArtyAutoHitHexes() {
            return new Vector<>();
        }

        @Override
        protected void checkMorale() {
        }

        @Override
        protected void postMovementProcessing() {
        }
    }

    /**
     * The turn-dropper, pinned: in simultaneous phases every player's turn-end fires a turn-change
     * event at every client, and the old ignoreSimTurn guard skipped any turn arriving after another
     * player's event once the bot had calculated once - silently dropping every second-and-later turn
     * of a multi-unit bot ("myTurn true -> skipping", caught verbatim in ~50 corrupted benchmark
     * games). Duplicate events for the SAME turn index must still spawn only once; a NEW turn index
     * must always spawn, whoever's event carried it.
     */
    @Test
    void aNewTurnAlwaysSpawnsAndDuplicateEventsDoNot() {
        RecordingBotClient bot = new RecordingBotClient();

        // Turn index 0, our turn: first event spawns, duplicate event for the same index does not.
        bot.lastCalculatedTurnIndex = -1;
        assertTrue(bot.shouldSpawnForTest(0), "a fresh turn index must spawn");
        bot.lastCalculatedTurnIndex = 0;
        assertFalse(bot.shouldSpawnForTest(0), "a duplicate event for the same turn index must not");
        // The killer case: another player's event delivered our NEXT turn. Old guard skipped it.
        assertTrue(bot.shouldSpawnForTest(1), "a new turn index must spawn whoever's event carried it");
    }

    /**
     * Issue #9390: when the server refuses a move it resends the same turn index naming the bot as the previous
     * player. That one event must restart the turn, the refused unit must be left out afterwards, and the same unit
     * must never restart the turn twice.
     */
    @Test
    void aRefusedMoveRestartsTheTurnOncePerUnit() {
        RecordingBotClient bot = new RecordingBotClient();
        bot.setLocalPlayerNumber(4);
        Entity refusedUnit = mock(Entity.class);
        when(refusedUnit.getId()).thenReturn(86);
        int turnIndex = 3;
        bot.getGame().setTurnIndex(turnIndex, 0);

        bot.lastCalculatedTurnIndex = turnIndex;
        bot.moveEntity(86, null);

        assertFalse(bot.isRefusedTurnRestart(turnIndex, 3, false),
              "another player's event is not a refusal of our move");
        assertFalse(bot.isRefusedTurnRestart(turnIndex, 4, true),
              "duplicate events are normal in a simultaneous phase");
        assertTrue(bot.isRefusedTurnRestart(turnIndex, 4, false), "the same turn handed back to us is a refusal");

        bot.startRefusedTurnOver();

        assertTrue(bot.shouldSpawnForTest(turnIndex), "a refused turn is calculated again");
        assertTrue(bot.isRefusedThisPhase(refusedUnit), "the refused unit is left out for the rest of the phase");
        bot.lastCalculatedTurnIndex = turnIndex;
        assertFalse(bot.isRefusedTurnRestart(turnIndex, 4, false),
              "the same refused unit never restarts the turn twice");
    }

    private static final int BOT_PLAYER_ID = 4;
    private static final int ENEMY_PLAYER_ID = 5;
    private static final Coords REFUSED_HEX = new Coords(1, 1);
    private static final Coords FREE_HEX = new Coords(2, 2);

    /** A bot on a flat 4x4 board, with itself and one enemy player in its game. */
    private static RecordingBotClient botOnAFlatBoard() {
        RecordingBotClient bot = new RecordingBotClient();
        bot.setLocalPlayerNumber(BOT_PLAYER_ID);
        int boardSize = 4;
        Hex[] hexes = new Hex[boardSize * boardSize];
        int index = 0;
        for (int y = 0; y < boardSize; y++) {
            for (int x = 0; x < boardSize; x++) {
                hexes[index++] = new Hex(0, "", "", new Coords(x, y));
            }
        }
        bot.getGame().setBoard(0, new Board(boardSize, boardSize, hexes));
        Player botPlayer = new Player(BOT_PLAYER_ID, "Bot");
        botPlayer.setTeam(1);
        bot.getGame().addPlayer(BOT_PLAYER_ID, botPlayer);
        Player enemyPlayer = new Player(ENEMY_PLAYER_ID, "Enemy");
        enemyPlayer.setTeam(2);
        bot.getGame().addPlayer(ENEMY_PLAYER_ID, enemyPlayer);
        return bot;
    }

    private static Mek addMek(BotClient bot, int entityId, int ownerId) {
        Mek mek = new BipedMek();
        mek.setId(entityId);
        mek.setOwner(bot.getGame().getPlayer(ownerId));
        bot.getGame().addEntity(mek);
        return mek;
    }

    private static Mek addHiddenMekAt(BotClient bot, int entityId, int ownerId, Coords position) {
        Mek mek = addMek(bot, entityId, ownerId);
        mek.setPosition(position);
        mek.setDeployed(true);
        mek.setHidden(true);
        return mek;
    }

    /**
     * Issue #9580: the server refuses a deployment onto a unit the bot cannot see and hands the same turn back. The
     * bot must deploy the same unit again, on another hex. Before the fix it took the event for a duplicate and never
     * deployed, which left the Deployment phase stuck.
     */
    @Test
    void aRefusedDeploymentIsTriedAgainOnAnotherHex() {
        RecordingBotClient bot = botOnAFlatBoard();
        Mek refusedUnit = addMek(bot, 86, BOT_PLAYER_ID);
        int turnIndex = 3;
        bot.getGame().setTurnIndex(turnIndex, 0);
        List<Coords> rankedHexes = List.of(REFUSED_HEX, FREE_HEX);
        assertEquals(REFUSED_HEX, bot.getFirstValidCoords(refusedUnit, rankedHexes),
              "control: before any refusal the best ranked hex is chosen");

        bot.lastCalculatedTurnIndex = turnIndex;
        bot.deploy(86, REFUSED_HEX, 0, 0, 0, List.of(), false);

        assertFalse(bot.isRefusedDeploymentRestart(turnIndex, ENEMY_PLAYER_ID, false),
              "another player's event is not a refusal of our deployment");
        assertFalse(bot.isRefusedDeploymentRestart(turnIndex, BOT_PLAYER_ID, true),
              "duplicate events are normal in a simultaneous phase");
        assertTrue(bot.isRefusedDeploymentRestart(turnIndex, BOT_PLAYER_ID, false),
              "the same turn handed back to us is a refused deployment");

        bot.startRefusedDeploymentOver();

        assertTrue(bot.shouldSpawnForTest(turnIndex), "a refused deployment turn is calculated again");
        assertFalse(bot.isRefusedThisPhase(refusedUnit), "the refused unit still has to deploy, so it is not left out");
        assertEquals(FREE_HEX, bot.getFirstValidCoords(refusedUnit, rankedHexes),
              "the refused hex is ruled out, so the next best hex is chosen");
    }

    /** A move that deploys its unit on the given hex. */
    private static MovePath walkOnPathTo(Coords hex) {
        MoveStep deployStep = mock(MoveStep.class);
        when(deployStep.getType()).thenReturn(MoveStepType.DEPLOY);
        when(deployStep.getPosition()).thenReturn(hex);
        MovePath walkOnPath = mock(MovePath.class);
        when(walkOnPath.getStepVector()).thenReturn(new Vector<>(List.of(deployStep)));
        return walkOnPath;
    }

    /**
     * A unit walking on from the map edge deploys as part of its move. When the server refuses the hex, the same unit
     * must be tried again on another one, and not be left out as a refused move would be.
     */
    @Test
    void aRefusedWalkOnIsTriedAgainOnAnotherHex() {
        RecordingBotClient bot = botOnAFlatBoard();
        Mek refusedUnit = addMek(bot, 86, BOT_PLAYER_ID);
        int turnIndex = 3;
        bot.getGame().setTurnIndex(turnIndex, 0);

        bot.lastCalculatedTurnIndex = turnIndex;
        bot.moveEntity(86, walkOnPathTo(REFUSED_HEX));

        assertTrue(bot.isRefusedDeploymentRestart(turnIndex, BOT_PLAYER_ID, false),
              "the same turn handed back to us is a refused walk-on");

        bot.startRefusedDeploymentOver();

        assertTrue(bot.shouldSpawnForTest(turnIndex), "a refused walk-on turn is calculated again");
        assertFalse(bot.isRefusedThisPhase(refusedUnit), "the refused unit still has to walk on, so it is not left out");
        assertEquals(FREE_HEX, bot.getFirstValidCoords(refusedUnit, List.of(REFUSED_HEX, FREE_HEX)),
              "the refused hex is ruled out, so the next best hex is chosen");
    }

    /** A move that does not deploy its unit is still handled as a refused move, not as a refused deployment. */
    @Test
    void aRefusedMoveThatDoesNotDeployIsNotARefusedWalkOn() {
        RecordingBotClient bot = botOnAFlatBoard();
        addMek(bot, 86, BOT_PLAYER_ID);
        int turnIndex = 3;
        bot.getGame().setTurnIndex(turnIndex, 0);
        MovePath plainMove = mock(MovePath.class);
        when(plainMove.getStepVector()).thenReturn(new Vector<>());

        bot.lastCalculatedTurnIndex = turnIndex;
        bot.moveEntity(86, plainMove);

        assertFalse(bot.isRefusedDeploymentRestart(turnIndex, BOT_PLAYER_ID, false));
        assertTrue(bot.isRefusedTurnRestart(turnIndex, BOT_PLAYER_ID, false), "control: it is a refused move");
    }

    /** A turn handed back after the server did deploy the unit is not a refusal. */
    @Test
    void aDeploymentTheServerAcceptedDoesNotRestart() {
        RecordingBotClient bot = botOnAFlatBoard();
        Mek deployedUnit = addMek(bot, 86, BOT_PLAYER_ID);
        int turnIndex = 3;
        bot.getGame().setTurnIndex(turnIndex, 0);
        bot.lastCalculatedTurnIndex = turnIndex;
        bot.deploy(86, REFUSED_HEX, 0, 0, 0, List.of(), false);
        assertTrue(bot.isRefusedDeploymentRestart(turnIndex, BOT_PLAYER_ID, false),
              "control: while the unit is still undeployed the handed-back turn is a refusal");

        deployedUnit.setDeployed(true);

        assertFalse(bot.isRefusedDeploymentRestart(turnIndex, BOT_PLAYER_ID, false),
              "a unit the server deployed was not refused");
    }

    /** A unit that keeps being refused stops restarting the turn, so the bot cannot loop forever. */
    @Test
    void aUnitThatKeepsBeingRefusedStopsRetrying() {
        RecordingBotClient bot = botOnAFlatBoard();
        addMek(bot, 86, BOT_PLAYER_ID);
        int turnIndex = 3;
        bot.getGame().setTurnIndex(turnIndex, 0);

        for (int retry = 0; retry < RefusedDeploymentTracker.MAX_RETRIES_PER_UNIT; retry++) {
            bot.lastCalculatedTurnIndex = turnIndex;
            bot.deploy(86, REFUSED_HEX, 0, 0, 0, List.of(), false);
            assertTrue(bot.isRefusedDeploymentRestart(turnIndex, BOT_PLAYER_ID, false),
                  "a refusal within the retry limit restarts the turn");
            bot.startRefusedDeploymentOver();
        }
        bot.lastCalculatedTurnIndex = turnIndex;
        bot.deploy(86, REFUSED_HEX, 0, 0, 0, List.of(), false);

        assertFalse(bot.isRefusedDeploymentRestart(turnIndex, BOT_PLAYER_ID, false),
              "past the retry limit the unit no longer restarts the turn");
    }

    /**
     * Issue #9580: the bot skipped every hidden unit when it checked a hex for stacking, its own included, so it sent
     * a second unit onto its own hidden one and the server refused it.
     */
    @Test
    void theBotsOwnHiddenUnitBlocksAHex() {
        RecordingBotClient bot = botOnAFlatBoard();
        addHiddenMekAt(bot, 87, BOT_PLAYER_ID, REFUSED_HEX);
        Mek deployingUnit = addMek(bot, 86, BOT_PLAYER_ID);

        assertTrue(bot.isBlockedByKnownUnit(deployingUnit, REFUSED_HEX),
              "the bot knows where its own hidden unit is, and the server refuses to stack on it");
        assertFalse(bot.isBlockedByKnownUnit(deployingUnit, FREE_HEX), "control: an empty hex is not blocked");
        assertEquals(FREE_HEX, bot.getFirstValidCoords(deployingUnit, List.of(REFUSED_HEX, FREE_HEX)));
    }

    /** The bot has not found a hidden enemy, so it must not steer around one. */
    @Test
    void aHiddenEnemyDoesNotBlockAHexForTheBot() {
        RecordingBotClient bot = botOnAFlatBoard();
        Mek hiddenEnemy = addHiddenMekAt(bot, 87, ENEMY_PLAYER_ID, REFUSED_HEX);
        Mek deployingUnit = addMek(bot, 86, BOT_PLAYER_ID);

        assertFalse(bot.isBlockedByKnownUnit(deployingUnit, REFUSED_HEX),
              "a hidden enemy is not something the bot may know about");

        hiddenEnemy.setHidden(false);

        assertTrue(bot.isBlockedByKnownUnit(deployingUnit, REFUSED_HEX),
              "control: the same enemy in plain view blocks the hex");
    }

    /** With every hex blocked or refused there is nowhere to deploy. */
    @Test
    void noHexIsChosenWhenEveryHexIsRuledOut() {
        RecordingBotClient bot = botOnAFlatBoard();
        addHiddenMekAt(bot, 87, BOT_PLAYER_ID, REFUSED_HEX);
        Mek deployingUnit = addMek(bot, 86, BOT_PLAYER_ID);

        assertNull(bot.getFirstValidCoords(deployingUnit, List.of(REFUSED_HEX)));
    }

    /** A move sent on an earlier turn that reaches the server late is not a refusal of the current turn. */
    @Test
    void aLateMoveFromAnEarlierTurnDoesNotRestart() {
        RecordingBotClient bot = new RecordingBotClient();
        bot.setLocalPlayerNumber(4);
        int sentOnTurnIndex = 3;
        int currentTurnIndex = sentOnTurnIndex + 1;
        bot.getGame().setTurnIndex(sentOnTurnIndex, 0);

        bot.moveEntity(86, null);
        bot.lastCalculatedTurnIndex = sentOnTurnIndex;
        assertTrue(bot.isRefusedTurnRestart(sentOnTurnIndex, 4, false),
              "control: handed back on the turn it was sent, the move would restart");

        bot.lastCalculatedTurnIndex = currentTurnIndex;
        assertFalse(bot.isRefusedTurnRestart(currentTurnIndex, 4, false),
              "the move was sent on an earlier turn, not the current one");
    }

    /**
     * A bot must never dismiss itself from a running game. The removed rule made a bot {@code die()} at
     * the start of the movement phase whenever it owned every entity on the board and the game was not
     * double blind - the state when the last enemy is a fighter that flew off scheduled to return.
     */
    @Test
    void aBotNeverDismissesItselfMidGame() {
        RecordingBotClient bot = new RecordingBotClient();

        bot.changePhase(GamePhase.MOVEMENT);

        assertFalse(bot.dismissedItself,
              "a bot that owns everything visible must keep playing - the victory check ends games");
    }

    /**
     * A turn that names the unit to move skips the bot's own choice of unit and asks for that unit's move directly.
     * The chosen move must still be reported, or a bot that remembers its moves loses every such turn.
     */
    @Test
    void aTurnThatNamesTheUnitStillReportsTheChosenMove() throws Exception {
        RecordingBotClient bot = new RecordingBotClient();
        bot.getGame().setPhase(GamePhase.MOVEMENT);
        bot.getGame().setTurnVector(List.of(new SpecificEntityTurn(0, 7)));
        bot.getGame().setTurnIndex(0, 0);
        Method worker = BotClient.class.getDeclaredMethod("calculateMyTurnWorker", boolean.class);
        worker.setAccessible(true);

        worker.invoke(bot, false);

        assertEquals(1, bot.unitMovesAsked, "the named unit's move is asked for directly");
        assertEquals(1, bot.movesReported, "and the move chosen for it is reported");
    }

    /**
     * A bot that finds no unit to move must still answer its turn, or the server waits on it forever. Before its last
     * attempt it keeps trying; on the last one it sends an empty move for the unit the server is waiting on.
     */
    @Test
    void aBotThatFindsNoUnitToMoveStillAnswersItsTurn() throws Exception {
        RecordingBotClient bot = botOnAFlatBoard();
        addMek(bot, 86, BOT_PLAYER_ID);
        bot.getGame().setPhase(GamePhase.MOVEMENT);
        bot.getGame().setTurnVector(List.of(new GameTurn(BOT_PLAYER_ID)));
        // Set before the turn change, so the bot does not answer this turn on a calculation thread of its own
        bot.lastCalculatedTurnIndex = 0;
        bot.getGame().setTurnIndex(0, ENEMY_PLAYER_ID);
        Method worker = BotClient.class.getDeclaredMethod("calculateMyTurnWorker", boolean.class);
        worker.setAccessible(true);

        assertEquals(false, worker.invoke(bot, false), "control: an earlier attempt leaves the turn to a retry");
        assertFalse(bot.isRefusedTurnRestart(0, BOT_PLAYER_ID, false), "control: no move has been sent yet");

        assertEquals(true, worker.invoke(bot, true), "the last attempt answers the turn");
        assertTrue(bot.isRefusedTurnRestart(0, BOT_PLAYER_ID, false), "a move was sent on this turn");
    }
}
