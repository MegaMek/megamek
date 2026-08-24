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
import java.util.List;

import megamek.common.Player;
import megamek.common.game.Game;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;

/**
 * Says, once per deployment phase, who is able to put units on the board and why anybody is not.
 *
 * <p>"My units never got a deployment turn" has several possible causes that all look identical from the map: the
 * player is on no team and so was left out of the turn order, the unit is not due to arrive yet, or the unit is
 * marked as finished from the round it was added. None of them produce a message, and the difference between them
 * cannot be seen without a debugger.</p>
 *
 * <p>Written as one line per phase rather than one per unit: a line per unit in a large game would bury the answer
 * it is meant to give.</p>
 */
final class DeploymentDiagnostics {

    private static final MMLogger LOGGER = MMLogger.create(DeploymentDiagnostics.class);

    private DeploymentDiagnostics() {
    }

    /**
     * Records whether a deployment phase will happen this round, and what is waiting if it will not.
     *
     * <p>The phase only happens when something is due to arrive on exactly this round: the table is keyed by
     * arrival round and looked up by the round now being played. A unit whose round has already gone by is
     * therefore never called for, even though the unit itself reports that it is ready to go - so it waits for
     * ever, in silence.</p>
     *
     * @param game The game deciding whether to deploy
     */
    static void logDeploymentDecision(Game game) {
        int round = game.getRoundCount();
        List<String> waiting = new ArrayList<>();
        for (Entity entity : game.getEntitiesVector()) {
            if (!entity.isDeployed()) {
                waiting.add(entity.getShortName() + " (" + entity.getOwner().getName() + ") due round "
                      + entity.getDeployRound());
            }
        }
        if (game.shouldDeployThisRound()) {
            LOGGER.info("[Deployment] round {}: a deployment phase is due; waiting: {}", round, waiting);
            return;
        }
        if (waiting.isEmpty()) {
            LOGGER.info("[Deployment] round {}: no deployment phase, nothing is waiting to arrive", round);
            return;
        }
        LOGGER.info("[Deployment] round {}: NO deployment phase, yet {} unit(s) are still undeployed - a phase only "
                    + "happens when something is due on exactly this round, so anything whose round has passed is "
                    + "never called for: {}", round, waiting.size(), waiting);
    }

    /**
     * Records who can deploy this phase.
     *
     * @param game The game entering its deployment phase
     */
    static void logWhoCanDeploy(Game game) {
        int round = game.getRoundCount();
        List<String> perPlayer = new ArrayList<>();
        for (Player player : game.getPlayersList()) {
            perPlayer.add(describePlayer(game, player, round));
        }
        LOGGER.info("[Deployment] round {}: {}", round, String.join(" | ", perPlayer));
    }

    /** @return one player's deployment position, in words */
    private static String describePlayer(Game game, Player player, int round) {
        List<Entity> waiting = new ArrayList<>();
        for (Entity entity : game.getPlayerEntities(player, false)) {
            if (!entity.isDeployed()) {
                waiting.add(entity);
            }
        }

        String team = (player.getTeam() == Player.TEAM_UNASSIGNED)
              ? "NO TEAM - left out of the turn order"
              : "team " + player.getTeam();
        if (waiting.isEmpty()) {
            return player.getName() + " (" + team + ") nothing waiting to deploy";
        }

        List<String> blocked = new ArrayList<>();
        int ready = 0;
        for (Entity entity : waiting) {
            String reason = whyNotDeployable(entity, round);
            if (reason == null) {
                ready++;
            } else {
                blocked.add(entity.getShortName() + ": " + reason);
            }
        }
        String summary = player.getName() + " (" + team + ") " + waiting.size() + " waiting, " + ready + " ready";
        return blocked.isEmpty() ? summary : summary + ", held back [" + String.join("; ", blocked) + "]";
    }

    /**
     * @param entity The unit waiting to deploy
     * @param round  The round now being played
     *
     * @return why that unit cannot deploy this round, or {@code null} when it can
     */
    private static String whyNotDeployable(Entity entity, int round) {
        if (entity.getDeployRound() > round) {
            return "arrives round " + entity.getDeployRound();
        }
        if (entity.isOffBoard()) {
            return "off board";
        }
        if (entity.isDone()) {
            return "marked finished for this round";
        }
        if (entity.isUnloadedThisTurn()) {
            return "marked unloaded this turn";
        }
        if (!entity.getCrew().isActive()) {
            return "no active crew";
        }
        return null;
    }
}
