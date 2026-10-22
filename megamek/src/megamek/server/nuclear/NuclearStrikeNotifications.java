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

import jakarta.annotation.Nullable;
import megamek.client.ui.Messages;
import megamek.common.Player;
import megamek.common.board.Coords;
import megamek.common.event.GameToastEvent;
import megamek.common.net.enums.PacketCommand;
import megamek.common.net.packets.Packet;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;
import megamek.server.totalWarfare.TWGameManager;

/**
 * Tells the players about nuclear strikes with toasts, on the same terms as artillery: the hex a strike is aimed at
 * goes only to the side that fired it, and everyone learns of it when it detonates.
 */
public class NuclearStrikeNotifications {
    private static final MMLogger LOGGER = MMLogger.create(NuclearStrikeNotifications.class);

    private final TWGameManager gameManager;

    /**
     * @param gameManager the game manager whose players are told
     */
    public NuclearStrikeNotifications(TWGameManager gameManager) {
        this.gameManager = gameManager;
    }

    /**
     * Warns the firing player and their teammates that a strike is inbound and where. Enemies are not told, just as
     * they are not told where artillery is aimed.
     *
     * @param groundZero      the hex the strike is aimed at
     * @param detonationRound the round the strike detonates in
     * @param firingPlayer the player held responsible for the strike, or {@code null} when there is none
     */
    public void announceIncoming(Coords groundZero, int detonationRound, @Nullable Player firingPlayer) {
        if (firingPlayer == null) {
            LOGGER.info("[Nukes] No incoming toast for hex {}: the strike has no firing player",
                  groundZero.getBoardNum());
            return;
        }
        String message = Messages.getString("NuclearStrike.toast.incoming", groundZero.getBoardNum(),
              detonationRound);
        int told = 0;
        for (Player player : gameManager.getGame().getPlayersList()) {
            if (!player.isEnemyOf(firingPlayer)) {
                sendToast(player.getId(), GameToastEvent.Level.WARNING, message);
                told++;
            }
        }
        LOGGER.info("[Nukes] Incoming toast for hex {} sent to {} player(s) on {}'s side", groundZero.getBoardNum(),
              told, firingPlayer.getName());
    }

    /**
     * Tells every player that a nuclear weapon has gone off, where, and how many units it destroyed. A detonation is
     * seen across the whole battlefield, so nobody is left out.
     *
     * @param warheadName    the weapon's name, such as "Santa Ana"
     * @param groundZero     the hex it detonated on
     * @param unitsDestroyed the units the detonation destroyed
     */
    public void announceDetonation(String warheadName, Coords groundZero, int unitsDestroyed) {
        LOGGER.info("[Nukes] Detonation toast to all players: {} at hex {}, {} unit(s) destroyed", warheadName,
              groundZero.getBoardNum(), unitsDestroyed);
        gameManager.sendToast(GameToastEvent.Level.ERROR,
              Messages.getString("NuclearStrike.toast.detonation", warheadName, groundZero.getBoardNum(),
                    unitsDestroyed),
              null);
    }

    /**
     * Tells the player who asked for a strike that it was refused because nuclear weapons are off.
     *
     * @param connectionId the connection of the player who asked
     */
    public void announceDisabled(int connectionId) {
        sendToast(connectionId, GameToastEvent.Level.ERROR, Messages.getString("NuclearStrike.toast.disabled"));
    }

    private void sendToast(int playerId, GameToastEvent.Level level, String message) {
        gameManager.send(playerId, new Packet(PacketCommand.SEND_TOAST, level, message, Entity.NONE));
    }
}
