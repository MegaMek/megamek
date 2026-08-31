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
import megamek.common.annotations.Nullable;
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
        return availableTo(localPlayer, allPlayers, localBotNames, false);
    }

    /**
     * The players the local player may add units to, leaving out any who could not use them yet.
     *
     * <p>During a game a player on no team is left out of the turn order entirely, so units given to them can never
     * deploy - and because a unit is called for only on the exact round it is due, one handed over too early is
     * stranded for good. They are therefore not offered until somebody has put them on a team. In the lobby this
     * does not arise: nobody has a turn order yet and teams are settled before the game starts.</p>
     *
     * @param localPlayer   The player at this screen
     * @param allPlayers    Every player in the game
     * @param localBotNames The names of the bots being run from this machine
     * @param isInGame      Whether the game is under way, rather than still in the lobby
     *
     * @return the players that may be given units, local player first
     */
    public static List<Player> availableTo(Player localPlayer, Collection<Player> allPlayers,
          Set<String> localBotNames, boolean isInGame) {
        return availableTo(localPlayer, allPlayers, localBotNames, isInGame, null);
    }

    /**
     * The players the local player may add units to, with one player asked for by name.
     *
     * <p>A gamemaster tool that has just put somebody on a team and pressed a reinforcement button has said plainly
     * who the units are for, and the team change does not reach the board until the end of the round - so the rule
     * that hides players who cannot deploy yet would otherwise hide the very person being set up. The player asked
     * for is therefore offered even when they are on no team.</p>
     *
     * <p>Being asked for does not get anyone past the ownership rule, though. The lobby hands over whichever player
     * happens to be highlighted in its player table, and for somebody who has just connected that is the host: an
     * ordinary player asking for the host is refused, and the chooser stays on the person using it.</p>
     *
     * @param localPlayer         The player at this screen
     * @param allPlayers          Every player in the game
     * @param localBotNames       The names of the bots being run from this machine
     * @param isInGame            Whether the game is under way, rather than still in the lobby
     * @param explicitlyRequested The player a caller asked for by name, or {@code null} when nobody was asked for
     *
     * @return the players that may be given units, local player first
     */
    public static List<Player> availableTo(Player localPlayer, Collection<Player> allPlayers,
          Set<String> localBotNames, boolean isInGame, @Nullable Player explicitlyRequested) {
        List<Player> recipients = new ArrayList<>();
        recipients.add(localPlayer);
        List<String> notYetPlaying = new ArrayList<>();
        boolean requestedPlayerFound = false;
        for (Player player : allPlayers) {
            boolean isTheRequestedPlayer = (explicitlyRequested != null)
                  && (player.getId() == explicitlyRequested.getId());
            requestedPlayerFound |= isTheRequestedPlayer;
            if (player.getId() == localPlayer.getId()) {
                // already first in the list; a second entry would offer the same person twice
                continue;
            }
            if (!mayAddUnitsTo(localPlayer, player, localBotNames)) {
                if (isTheRequestedPlayer) {
                    LOGGER.info("[GMAddUnit] refusing to offer {} although they were asked for by name: {} is not "
                                + "a gamemaster, and {} is neither them nor a bot they run",
                          player.getName(), localPlayer.getName(), player.getName());
                }
                continue;
            }
            boolean cannotDeployYet = isInGame && (player.getTeam() == Player.TEAM_UNASSIGNED);
            if (cannotDeployYet && !isTheRequestedPlayer) {
                notYetPlaying.add(player.getName());
                continue;
            }
            if (cannotDeployYet) {
                LOGGER.info("[GMAddUnit] offering {} on no team because a gamemaster tool asked for them by name",
                      player.getName());
            }
            recipients.add(player);
        }
        if ((explicitlyRequested != null) && !requestedPlayerFound) {
            LOGGER.info("[GMAddUnit] {} was asked for by name but is no longer in the game, so they are not offered",
                  explicitlyRequested.getName());
        }
        if (!notYetPlaying.isEmpty()) {
            LOGGER.info("[GMAddUnit] not offering {} - on no team, so units given to them could never deploy; "
                        + "use Set Up Player first", notYetPlaying);
        }
        // at INFO because the shipped logging runs at INFO, and the question this answers - "why is that player
        // not in the list" - is one a playtest has to be able to settle from the log. Once per dialog opening.
        LOGGER.info("[GMAddUnit] {} (gamemaster: {}) may add units to {} of {} player(s): {}",
              localPlayer.getName(), localPlayer.isGameMaster(), recipients.size(), allPlayers.size(),
              recipients.stream().map(Player::getName).toList());
        return recipients;
    }

    /**
     * Whether the local player may add units to one particular player.
     *
     * <p>This is the whole ownership rule, asked about a single player and without any logging, for the places
     * that need a quiet yes or no: the lobby deciding whether the highlighted player may be handed to a unit
     * chooser, and a dialog checking one last time who it is about to send units for.</p>
     *
     * @param localPlayer   The player at this screen
     * @param recipient     The player who would receive the units
     * @param localBotNames The names of the bots being run from this machine
     *
     * @return {@code true} when the recipient is the local player, a bot run from this machine, or anyone at all
     *       when the local player holds the gamemaster role
     */
    public static boolean mayAddUnitsTo(Player localPlayer, Player recipient, Set<String> localBotNames) {
        if (recipient.getId() == localPlayer.getId()) {
            return true;
        }
        boolean isABotFromThisMachine = localBotNames.contains(recipient.getName());
        return isABotFromThisMachine || localPlayer.isGameMaster();
    }
}
