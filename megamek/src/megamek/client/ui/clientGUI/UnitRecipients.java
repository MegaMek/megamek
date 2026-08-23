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
package megamek.client.ui.clientGUI;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import megamek.common.Player;
import megamek.logging.MMLogger;

/**
 * Which players the person at this screen may add units to.
 *
 * <p>Ordinarily that is themselves and any bot they are running, because those are the forces they own. A
 * gamemaster may add units to anyone: handing out forces is most of what a gamemaster does before a game starts,
 * and doing it any other way means adding every batch to yourself and then reassigning it a group at a time.</p>
 *
 * <p>The rule lives here, on its own, because four separate dialogs ask it - the unit selector, the random army
 * generator, the force generator and the unit list loader - and a rule answered differently in one of them is how
 * a dialog comes to say it gave units to one player while giving them to another.</p>
 *
 * <p>This asks about players rather than clients on purpose. A remote human has no client on this machine, so
 * anything that looks for one finds nothing and quietly falls back to the local player; the units are owned by
 * whoever is named here and sent over the local connection, which is how the in-game reinforcement path has
 * always done it.</p>
 */
public final class UnitRecipients {

    private static final MMLogger LOGGER = MMLogger.create(UnitRecipients.class);

    private UnitRecipients() {
    }

    /**
     * The players the local player may add units to, in the order they should be offered.
     *
     * <p>The local player comes first so that a chooser landing on its first entry lands on the person using it,
     * which is what happens today and what someone adding units to their own force expects.</p>
     *
     * @param localPlayer   The player at this screen
     * @param allPlayers    Every player in the game
     * @param localBotNames The names of the bots being run from this machine
     *
     * @return the players that may be given units, local player first; never empty
     */
    public static List<Player> availableTo(Player localPlayer, Collection<Player> allPlayers,
          Set<String> localBotNames) {
        List<Player> recipients = new ArrayList<>();
        recipients.add(localPlayer);
        for (Player player : allPlayers) {
            if (mayBeGivenUnits(player, localPlayer, localBotNames)) {
                recipients.add(player);
            }
        }
        // at INFO because the shipped logging runs at INFO, and the question this answers - "why is that player
        // not in the list" - is one a playtest has to be able to settle from the log. Once per dialog opening.
        LOGGER.info("[GMAddUnit] {} (gamemaster: {}) may add units to {} of {} player(s): {}",
              localPlayer.getName(), localPlayer.isGameMaster(), recipients.size(), allPlayers.size(),
              recipients.stream().map(Player::getName).toList());
        return recipients;
    }

    /**
     * @param player        The player being considered
     * @param localPlayer   The player at this screen
     * @param localBotNames The names of the bots being run from this machine
     *
     * @return {@code true} when the local player may add units to that player, other than to themselves
     */
    private static boolean mayBeGivenUnits(Player player, Player localPlayer, Set<String> localBotNames) {
        if (player.getId() == localPlayer.getId()) {
            // already first in the list; a second entry would offer the same person twice
            return false;
        }
        boolean isABotFromThisMachine = localBotNames.contains(player.getName());
        return isABotFromThisMachine || localPlayer.isGameMaster();
    }
}
