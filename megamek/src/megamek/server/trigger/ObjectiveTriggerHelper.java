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

package megamek.server.trigger;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import megamek.common.Player;
import megamek.common.annotations.Nullable;
import megamek.common.equipment.ICarryable;
import megamek.common.equipment.ObjectiveMarker;
import megamek.common.game.AbstractGame;
import megamek.common.game.IGame;
import megamek.common.game.InGameObject;
import megamek.common.units.Entity;

/**
 * Shared lookups for the objective-based victory triggers: finding an objective marker by name (on the ground or
 * carried) and matching a marker's stored controller against a player's side.
 */
final class ObjectiveTriggerHelper {

    /**
     * Finds an objective marker by its name, whether it lies on the ground or is carried by a unit. Marker names are
     * matched exactly; the first match wins, so missions should give objectives unique names.
     *
     * @param game          The game
     * @param objectiveName The objective's name
     *
     * @return The marker, or {@code null} when no objective of that name exists (e.g. a useless Potential Objective
     *       candidate that was removed from the battlefield)
     */
    @Nullable
    static ObjectiveMarker findMarker(IGame game, String objectiveName) {
        if (game instanceof AbstractGame abstractGame) {
            for (List<ICarryable> groundObjects : abstractGame.getGroundObjects().values()) {
                ObjectiveMarker marker = findMarkerIn(groundObjects, objectiveName);
                if (marker != null) {
                    return marker;
                }
            }
        }
        for (InGameObject inGameObject : game.getInGameObjects()) {
            if (inGameObject instanceof Entity entity) {
                ObjectiveMarker marker = findMarkerIn(entity.getDistinctCarriedObjects(), objectiveName);
                if (marker != null) {
                    return marker;
                }
            }
        }
        return null;
    }

    @Nullable
    private static ObjectiveMarker findMarkerIn(List<ICarryable> carryables, String objectiveName) {
        for (ICarryable carryable : carryables) {
            if ((carryable instanceof ObjectiveMarker marker) && objectiveName.equals(marker.generalName())) {
                return marker;
            }
        }
        return null;
    }

    /**
     * Checks whether the marker's stored controller (see {@link ObjectiveMarker#setController(int, int)}) matches
     * the given player's side: the player's team when it is on one, otherwise the player itself.
     *
     * @param game       The game
     * @param playerName The player whose side must control the objective; blank matches any controller
     * @param marker     The objective marker
     *
     * @return {@code true} when the objective is controlled by the matching side
     */
    static boolean controllerMatches(IGame game, String playerName, ObjectiveMarker marker) {
        boolean isControlled = (marker.getControllingTeam() != ObjectiveMarker.NO_CONTROLLER)
              || (marker.getControllingPlayerId() != ObjectiveMarker.NO_CONTROLLER);
        if (!isControlled) {
            return false;
        }
        if (playerName.isBlank()) {
            return true;
        }
        Player player = findPlayerByName(game, playerName);
        if (player == null) {
            return false;
        }
        if (player.getTeam() != Player.TEAM_NONE) {
            return marker.getControllingTeam() == player.getTeam();
        }
        return marker.getControllingPlayerId() == player.getId();
    }

    @Nullable
    static Player findPlayerByName(IGame game, String playerName) {
        for (Player player : game.getPlayersList()) {
            if (playerName.equals(player.getName())) {
                return player;
            }
        }
        return null;
    }

    /**
     * Evaluates a per-side condition: for the named player's side when a player name is given, otherwise for every
     * non-observer side in the game (each team considered once) until one satisfies it.
     *
     * @param game          The game
     * @param playerName    The player whose side to test, or blank for any side
     * @param sideCondition The condition, tested with a representative player of each side
     *
     * @return {@code true} when the tested side (or any side) satisfies the condition
     */
    static boolean anySideMatches(IGame game, String playerName, Predicate<Player> sideCondition) {
        if (!playerName.isBlank()) {
            Player player = findPlayerByName(game, playerName);
            return (player != null) && sideCondition.test(player);
        }
        Set<Integer> testedTeams = new HashSet<>();
        for (Player player : game.getPlayersList()) {
            if (player.isObserver()) {
                continue;
            }
            if ((player.getTeam() != Player.TEAM_NONE) && !testedTeams.add(player.getTeam())) {
                continue;
            }
            if (sideCondition.test(player)) {
                return true;
            }
        }
        return false;
    }

    private ObjectiveTriggerHelper() {}
}
