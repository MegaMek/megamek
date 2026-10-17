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

import java.util.Optional;
import java.util.Set;

import jakarta.annotation.Nullable;
import megamek.common.Player;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.game.Game;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;

/**
 * Works out where the enemy will come from: the middle of the enemy's deployment zones on a board. A bot faces the
 * units it deploys that way and lays its formations out facing it.
 *
 * <p>Each zone's middle comes from the zone's shape rather than from looking at the board hex by hex: the middle of
 * its edge or corner for a zone along the board edge, the middle of its rectangle for a zone set by corners, and the
 * average of its hexes for a zone drawn on the map.</p>
 */
final class EnemyDeploymentZone {

    private static final MMLogger LOGGER = MMLogger.create(EnemyDeploymentZone.class);

    private EnemyDeploymentZone() {}

    /**
     * @param game        the game
     * @param board       the board the bot is deploying on
     * @param localPlayer the bot's player
     *
     * @return the average of the middles of the zones the enemy players with units deploy in, or empty when there is
     *       none
     */
    static Optional<Coords> center(Game game, @Nullable Board board, @Nullable Player localPlayer) {
        if ((board == null) || (localPlayer == null)) {
            return Optional.empty();
        }
        long totalX = 0;
        long totalY = 0;
        int zones = 0;
        for (Player player : game.getPlayersList()) {
            boolean isEnemyWithUnits = player.isEnemyOf(localPlayer) && !player.isObserver()
                  && !game.getPlayerEntities(player, false).isEmpty();
            if (!isEnemyWithUnits) {
                continue;
            }
            Optional<Coords> zoneCenter = zoneCenter(board, player);
            if (zoneCenter.isPresent()) {
                totalX += zoneCenter.get().getX();
                totalY += zoneCenter.get().getY();
                zones++;
            }
        }
        if (zones == 0) {
            LOGGER.debug("[BotOrders] enemy deployment center for {}: no enemy player with units has a zone on board "
                  + "{}", localPlayer.getName(), board.getBoardId());
            return Optional.empty();
        }
        Coords center = new Coords((int) (totalX / zones), (int) (totalY / zones));
        LOGGER.debug("[BotOrders] enemy deployment center for {}: {} from {} zone(s)", localPlayer.getName(),
              center.getBoardNum(), zones);
        return Optional.of(center);
    }

    /**
     * @return the middle of the zone the player deploys in, or empty for a drawn zone with no hexes on this board
     */
    private static Optional<Coords> zoneCenter(Board board, Player player) {
        int zone = player.getStartingPos();
        if (zone >= Board.NUM_ZONES) {
            return drawnZoneCenter(board.getCustomDeploymentZone(Board.decodeCustomDeploymentZoneID(zone)));
        }
        if (zone == Board.START_ANY) {
            int west = sideOrDefault(player.getStartingAnyNWx(), 0);
            int north = sideOrDefault(player.getStartingAnyNWy(), 0);
            int east = sideOrDefault(player.getStartingAnySEx(), board.getWidth() - 1);
            int south = sideOrDefault(player.getStartingAnySEy(), board.getHeight() - 1);
            return Optional.of(new Coords((west + east) / 2, (north + south) / 2));
        }
        return Optional.of(board.getDeploymentCenter(zone));
    }

    private static int sideOrDefault(int side, int boardSide) {
        return (side == Entity.STARTING_ANY_NONE) ? boardSide : side;
    }

    private static Optional<Coords> drawnZoneCenter(Set<Coords> hexes) {
        if (hexes.isEmpty()) {
            return Optional.empty();
        }
        long totalX = 0;
        long totalY = 0;
        for (Coords hex : hexes) {
            totalX += hex.getX();
            totalY += hex.getY();
        }
        return Optional.of(new Coords((int) (totalX / hexes.size()), (int) (totalY / hexes.size())));
    }
}
