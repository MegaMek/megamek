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
package megamek.client.bot.princess;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import megamek.common.Player;
import megamek.common.annotations.Nullable;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.game.Game;

/**
 * Works out where the enemy will come from: the middle of the enemy's deployment zones on a board. A bot faces the
 * units it deploys that way and lays its formations out facing it.
 *
 * <p>The answer is worked out once a round for each board, since it scans every hex of the board and the deployment
 * zones do not change while units deploy.</p>
 */
class EnemyDeploymentZone {

    private final Map<Integer, Optional<Coords>> centersByBoard = new HashMap<>();
    private int centersRound = -1;

    /**
     * @param game        the game
     * @param board       the board the bot is deploying on
     * @param localPlayer the bot's player
     *
     * @return the average of every hex an enemy player with units may deploy in, or empty when there is none
     */
    Optional<Coords> center(Game game, @Nullable Board board, @Nullable Player localPlayer) {
        if ((board == null) || (localPlayer == null)) {
            return Optional.empty();
        }
        if (centersRound != game.getCurrentRound()) {
            centersByBoard.clear();
            centersRound = game.getCurrentRound();
        }
        return centersByBoard.computeIfAbsent(board.getBoardId(), boardId -> scan(game, board, localPlayer));
    }

    private static Optional<Coords> scan(Game game, Board board, Player localPlayer) {
        List<Player> enemies = new ArrayList<>();
        for (Player player : game.getPlayersList()) {
            if (player.isEnemyOf(localPlayer) && !player.isObserver()
                  && !game.getPlayerEntities(player, false).isEmpty()) {
                enemies.add(player);
            }
        }
        if (enemies.isEmpty()) {
            return Optional.empty();
        }
        long totalX = 0;
        long totalY = 0;
        int zoneHexes = 0;
        for (int x = 0; x < board.getWidth(); x++) {
            for (int y = 0; y < board.getHeight(); y++) {
                Coords hex = new Coords(x, y);
                for (Player enemy : enemies) {
                    if (board.isLegalDeployment(hex, enemy)) {
                        totalX += x;
                        totalY += y;
                        zoneHexes++;
                        break;
                    }
                }
            }
        }
        if (zoneHexes == 0) {
            return Optional.empty();
        }
        return Optional.of(new Coords((int) (totalX / zoneHexes), (int) (totalY / zoneHexes)));
    }
}
