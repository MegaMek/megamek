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
package megamek.common.game;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for the per-bot dishonored-players state on {@link Game}, populated from
 * {@link megamek.common.net.enums.PacketCommand#PRINCESS_DISHONORED} reports and read by the dishonor warning.
 */
class GameDishonorTest {

    private static final int BOT_ID = 7;
    private static final int OTHER_BOT_ID = 8;
    private static final int PLAYER_ID = 3;

    private Game game;

    @BeforeEach
    void setUp() {
        game = new Game();
    }

    @Test
    void unknownBotConsidersNobodyDishonored() {
        assertFalse(game.isPlayerDishonoredBy(BOT_ID, PLAYER_ID));
    }

    @Test
    void recordedPlayerIsReportedDishonored() {
        game.setDishonoredPlayers(BOT_ID, List.of(PLAYER_ID));
        assertTrue(game.isPlayerDishonoredBy(BOT_ID, PLAYER_ID));
    }

    @Test
    void unlistedPlayerIsNotDishonored() {
        game.setDishonoredPlayers(BOT_ID, List.of(PLAYER_ID));
        assertFalse(game.isPlayerDishonoredBy(BOT_ID, PLAYER_ID + 1));
    }

    @Test
    void dishonorIsTrackedPerBot() {
        game.setDishonoredPlayers(BOT_ID, List.of(PLAYER_ID));
        // A different bot has made no report, so it holds no grudge against the same player.
        assertFalse(game.isPlayerDishonoredBy(OTHER_BOT_ID, PLAYER_ID));
    }

    @Test
    void laterReportReplacesEarlierOne() {
        game.setDishonoredPlayers(BOT_ID, List.of(PLAYER_ID));
        // The bot re-reports each round; a report that no longer lists the player clears the grudge.
        game.setDishonoredPlayers(BOT_ID, List.of());
        assertFalse(game.isPlayerDishonoredBy(BOT_ID, PLAYER_ID));
    }

    @Test
    void storedStateIsDefensivelyCopied() {
        List<Integer> reported = new ArrayList<>();
        reported.add(PLAYER_ID);
        game.setDishonoredPlayers(BOT_ID, reported);
        // Mutating the caller's collection afterward must not change what the game remembers.
        reported.clear();
        assertTrue(game.isPlayerDishonoredBy(BOT_ID, PLAYER_ID));
    }

    @Test
    void addDishonoredPlayerMarksPlayerWithoutPriorReport() {
        game.addDishonoredPlayer(BOT_ID, PLAYER_ID);
        assertTrue(game.isPlayerDishonoredBy(BOT_ID, PLAYER_ID));
    }

    @Test
    void addDishonoredPlayerKeepsExistingGrudges() {
        game.setDishonoredPlayers(BOT_ID, List.of(PLAYER_ID));
        game.addDishonoredPlayer(BOT_ID, PLAYER_ID + 1);
        assertTrue(game.isPlayerDishonoredBy(BOT_ID, PLAYER_ID));
        assertTrue(game.isPlayerDishonoredBy(BOT_ID, PLAYER_ID + 1));
    }

    @Test
    void authoritativeReportReplacesOptimisticGuess() {
        // Optimistically flag the player, then let the bot's real report (which does not list them) overwrite it.
        game.addDishonoredPlayer(BOT_ID, PLAYER_ID);
        game.setDishonoredPlayers(BOT_ID, List.of());
        assertFalse(game.isPlayerDishonoredBy(BOT_ID, PLAYER_ID));
    }
}
