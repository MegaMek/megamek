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

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.annotation.Nullable;
import megamek.common.board.BoardLocation;
import megamek.common.board.Coords;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;

/**
 * Remembers the deployment a bot last sent and the hexes the server has refused this phase. The server refuses a
 * deployment that would stack on a unit the bot cannot see, and hands the same turn back. Without this record the bot
 * treated that as a duplicate turn and never deployed, which left the Deployment phase stuck (issue 9580).
 * <p>
 * The deployment is recorded on the bot's calculation thread and read on its turn-change listener thread.
 */
class RefusedDeploymentTracker {
    private static final MMLogger LOGGER = MMLogger.create(RefusedDeploymentTracker.class);

    /** How many refused deployments one unit may retry in a phase before the bot gives up on it. */
    static final int MAX_RETRIES_PER_UNIT = 10;

    /**
     * A deployment sent to the server.
     *
     * @param entityId  the ID of the deployed unit
     * @param location  the hex the unit was sent to
     * @param turnIndex the turn index the deployment was sent on
     */
    private record SentDeployment(int entityId, BoardLocation location, int turnIndex) {}

    private volatile @Nullable SentDeployment lastSentDeployment = null;
    private final Set<BoardLocation> refusedLocations = ConcurrentHashMap.newKeySet();
    private final Map<Integer, Integer> retriesByEntityId = new ConcurrentHashMap<>();

    /**
     * Records a deployment the bot is about to send.
     *
     * @param entityId  the ID of the unit being deployed
     * @param coords    the hex it is sent to
     * @param boardId   the board that hex is on
     * @param turnIndex the current turn index
     */
    void recordSentDeployment(int entityId, Coords coords, int boardId, int turnIndex) {
        lastSentDeployment = new SentDeployment(entityId, BoardLocation.of(coords, boardId), turnIndex);
    }

    /**
     * @return the ID of the unit last sent to deploy, or {@link Entity#NONE} if none was sent since the last refusal
     */
    int getLastSentEntityId() {
        SentDeployment sentDeployment = lastSentDeployment;
        return (sentDeployment == null) ? Entity.NONE : sentDeployment.entityId();
    }

    /**
     * Whether the server handing this turn back means it refused the bot's last deployment, and the bot should try
     * again. A unit that has used up its retries no longer counts, so a unit that keeps being refused cannot loop.
     *
     * @param turnIndex the turn index the server handed back
     *
     * @return {@code true} if a deployment sent on that turn index should be tried again on another hex
     */
    boolean isRetryableRefusal(int turnIndex) {
        SentDeployment sentDeployment = lastSentDeployment;
        if ((sentDeployment == null) || (sentDeployment.turnIndex() != turnIndex)) {
            return false;
        }
        int retries = retriesByEntityId.getOrDefault(sentDeployment.entityId(), 0);
        if (retries >= MAX_RETRIES_PER_UNIT) {
            LOGGER.error("[DeployRetry] entity ID {} was refused at {} after {} retries; giving up on it this phase",
                  sentDeployment.entityId(), sentDeployment.location().getBoardNum(), retries);
            return false;
        }
        return true;
    }

    /**
     * Marks the last sent deployment as refused: its hex is ruled out for the rest of the phase and the unit has used
     * one retry.
     *
     * @param botName the bot's name, for the log line
     */
    void markLastDeploymentRefused(String botName) {
        SentDeployment sentDeployment = lastSentDeployment;
        if (sentDeployment == null) {
            return;
        }
        refusedLocations.add(sentDeployment.location());
        int retries = retriesByEntityId.merge(sentDeployment.entityId(), 1, Integer::sum);
        lastSentDeployment = null;
        LOGGER.warn("[DeployRetry] {}: the server refused entity ID {} at {} on turn {}; "
                    + "ruling that hex out and deploying again (retry {} of {})",
              botName, sentDeployment.entityId(), sentDeployment.location().getBoardNum(),
              sentDeployment.turnIndex(), retries, MAX_RETRIES_PER_UNIT);
    }

    /**
     * @param coords  the hex to check
     * @param boardId the board that hex is on
     *
     * @return {@code true} if the server refused a deployment to this hex earlier in this phase
     */
    boolean isRefusedLocation(Coords coords, int boardId) {
        return refusedLocations.contains(BoardLocation.of(coords, boardId));
    }

    /** Forgets everything, for the start of a new phase. */
    void reset() {
        lastSentDeployment = null;
        refusedLocations.clear();
        retriesByEntityId.clear();
    }
}
