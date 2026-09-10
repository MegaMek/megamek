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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import megamek.common.Player;
import megamek.common.compute.InfantryActionStrengths;
import megamek.common.game.Game;
import megamek.common.game.GameTurn;
import megamek.common.units.AbstractBuildingEntity;
import megamek.logging.MMLogger;

/**
 * Puts the players with an attack to declare ahead of the players who only defend in the Pre-End Declarations
 * phase. The book's order is attacker first, then the defender answers with units and crew (TO:AR pp. 169 to 172);
 * the phase's turns follow initiative, which can ask the defender to commit before anything has been declared
 * against them. The relative order within each group is kept, so initiative still decides between two attackers.
 */
final class InfantryActionTurnOrder {

    private static final MMLogger LOGGER = MMLogger.create(InfantryActionTurnOrder.class);

    private InfantryActionTurnOrder() {}

    /**
     * Reorders the game's turn list for the phase: attackers first, everyone else after, each group in its
     * initiative order. Does nothing when nobody has an attack to declare.
     *
     * @param game the game, with the phase's turns already built
     */
    static void putAttackersFirst(Game game) {
        Set<Integer> attackingPlayers = new HashSet<>();
        for (Player player : game.getPlayersList()) {
            if (attacksSomewhere(game, player)) {
                attackingPlayers.add(player.getId());
            }
        }
        List<GameTurn> turns = game.getTurnsList();
        if (attackingPlayers.isEmpty() || turns.isEmpty()) {
            return;
        }
        List<GameTurn> attackers = new ArrayList<>();
        List<GameTurn> others = new ArrayList<>();
        for (GameTurn turn : turns) {
            if (attackingPlayers.contains(turn.playerId())) {
                attackers.add(turn);
            } else {
                others.add(turn);
            }
        }
        List<GameTurn> reordered = new ArrayList<>(attackers);
        reordered.addAll(others);
        game.setTurnVector(reordered);
        LOGGER.info("[InfantryAction] declaration turn order, attackers first: {}", reordered.stream()
              .map(turn -> describe(game, turn.playerId()))
              .collect(Collectors.joining(", ")));
    }

    /** Whether the player has infantry inside an enemy building, or an attack already running, anywhere. */
    private static boolean attacksSomewhere(Game game, Player player) {
        for (AbstractBuildingEntity building : InfantryActionStrengths.stakes(game, player)) {
            if (!InfantryActionStrengths.defends(player, building)) {
                return true;
            }
        }
        return false;
    }

    private static String describe(Game game, int playerId) {
        Player player = game.getPlayer(playerId);
        if (player == null) {
            return "player " + playerId;
        }
        List<String> roles = new ArrayList<>();
        for (AbstractBuildingEntity building : InfantryActionStrengths.stakes(game, player)) {
            String role = InfantryActionStrengths.defends(player, building) ? "defends " : "attacks ";
            roles.add(role + building.getShortName());
        }
        return player.getName() + (roles.isEmpty() ? " (no stake)" : " (" + String.join("; ", roles) + ")");
    }
}
