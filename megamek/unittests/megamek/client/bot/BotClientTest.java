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

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Vector;

import megamek.client.bot.princess.BehaviorSettings;
import megamek.common.board.BoardLocation;
import megamek.common.enums.GamePhase;
import megamek.common.event.player.GamePlayerChatEvent;
import megamek.common.moves.MovePath;
import megamek.common.units.Entity;
import org.junit.jupiter.api.Test;

class BotClientTest {

    /** A concrete BotClient with every strategy hook stubbed out, so phase plumbing can run alone. */
    private static final class RecordingBotClient extends BotClient {

        private boolean dismissedItself = false;

        RecordingBotClient() {
            super("tester", "localhost", 0);
        }

        @Override
        public void die() {
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
        protected void calculateInfantryVsInfantryCombatTurn() {
        }

        @Override
        protected MovePath continueMovementFor(Entity entity) {
            return null;
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
     * A bot must never dismiss itself from a running game. The removed rule made a bot {@code die()} at the
     * start of the movement phase whenever it owned every entity on the board and the game was not double
     * blind - which is exactly the state when the last enemy is a fighter that flew off the map and is
     * scheduled to return: the bot quit mid-game and the server waited forever on its units. An empty game
     * reproduces the trigger arithmetic (own everything visible) without a connection.
     */
    @Test
    void aBotNeverDismissesItselfMidGame() {
        RecordingBotClient bot = new RecordingBotClient();

        bot.changePhase(GamePhase.MOVEMENT);

        assertFalse(bot.dismissedItself,
              "a bot that owns everything visible must keep playing - the victory check ends games");
    }
}
