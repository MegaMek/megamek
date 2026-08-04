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

import megamek.common.Player;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;
import megamek.server.ServerLobbyHelper;

/**
 * Connects a tractor and an ordered list of trailers into a train in one operation.
 * <p>
 * The whole chain is validated before anything is attached, and any failure part way through is rolled back, so a
 * rejected request always leaves every unit unattached rather than producing a half-built train. Ordering is taken
 * from the client verbatim: trailers are hitched front to back in the order given, which fixes the hitch chain and
 * therefore where each unit sits on the board, since the first trailer shares the tractor's hex and the rest pack
 * behind it.
 * </p>
 * Every rejection logs its own reason under the {@code [Train]} tag so a playtest can be diagnosed from megamek.log
 * without a debugger.
 */
class TrainBuildHandler extends AbstractTWRuleHandler {

    private static final MMLogger LOGGER = MMLogger.create(TrainBuildHandler.class);

    private static final String LOG_TAG = "[Train]";

    TrainBuildHandler(TWGameManager gameManager) {
        super(gameManager);
    }

    /**
     * Validates and applies a build-train request.
     *
     * @param tractorId    the powered tractor that will head the train
     * @param trailerIds   the trailers to hitch behind it, ordered front to back
     * @param requestingPlayer the player who sent the request, or {@code null} when the sender is unknown
     */
    void buildTrain(int tractorId, List<Integer> trailerIds, Player requestingPlayer) {
        Entity tractor = getGame().getEntity(tractorId);

        if (tractor == null) {
            LOGGER.warn("{} rejected: no such tractor #{}", LOG_TAG, tractorId);
            return;
        }
        if ((requestingPlayer != null) && (tractor.getOwner() != requestingPlayer)) {
            LOGGER.warn("{} rejected: player {} does not own tractor {}",
                  LOG_TAG, requestingPlayer.getName(), tractor.getDisplayName());
            return;
        }
        if (trailerIds.isEmpty()) {
            LOGGER.warn("{} rejected: no trailers named for tractor {}", LOG_TAG, tractor.getDisplayName());
            return;
        }
        if (!tractor.isTractor()) {
            LOGGER.warn("{} rejected: {} is not a tractor", LOG_TAG, tractor.getDisplayName());
            return;
        }
        if (tractor.getTractor() != Entity.NONE) {
            LOGGER.warn("{} rejected: tractor {} is already part of a train", LOG_TAG, tractor.getDisplayName());
            return;
        }
        if (tractor.getTowing() != Entity.NONE) {
            LOGGER.warn("{} rejected: tractor {} already tows a trailer", LOG_TAG, tractor.getDisplayName());
            return;
        }

        List<Entity> trailers = collectTrailers(tractor, trailerIds, requestingPlayer);
        if (trailers == null) {
            return;
        }

        LOGGER.info("{} building train: {} + {} trailer(s)",
              LOG_TAG, tractor.getDisplayName(), trailers.size());
        applyTrain(tractor, trailers);
    }

    /**
     * Resolves and checks every named trailer before anything is attached.
     *
     * @return the trailers in the requested order, or {@code null} when any of them is unusable
     */
    private List<Entity> collectTrailers(Entity tractor, List<Integer> trailerIds, Player requestingPlayer) {
        List<Entity> trailers = new ArrayList<>();
        Set<Integer> seenIds = new HashSet<>();

        for (int trailerId : trailerIds) {
            if (trailerId == tractor.getId()) {
                LOGGER.warn("{} rejected: tractor {} named as its own trailer", LOG_TAG, tractor.getDisplayName());
                return null;
            }
            if (!seenIds.add(trailerId)) {
                LOGGER.warn("{} rejected: trailer #{} named twice", LOG_TAG, trailerId);
                return null;
            }

            Entity trailer = getGame().getEntity(trailerId);
            if (trailer == null) {
                LOGGER.warn("{} rejected: no such trailer #{}", LOG_TAG, trailerId);
                return null;
            }
            if ((requestingPlayer != null) && (trailer.getOwner() != requestingPlayer)) {
                LOGGER.warn("{} rejected: player {} does not own trailer {}",
                      LOG_TAG, requestingPlayer.getName(), trailer.getDisplayName());
                return null;
            }
            if (!trailer.isTrailer()) {
                LOGGER.warn("{} rejected: {} is not a trailer", LOG_TAG, trailer.getDisplayName());
                return null;
            }
            if (trailer.getTractor() != Entity.NONE) {
                LOGGER.warn("{} rejected: trailer {} is already part of a train",
                      LOG_TAG, trailer.getDisplayName());
                return null;
            }
            if (trailer.getTowedBy() != Entity.NONE) {
                LOGGER.warn("{} rejected: trailer {} is already hitched to another unit",
                      LOG_TAG, trailer.getDisplayName());
                return null;
            }

            trailers.add(trailer);
        }

        return trailers;
    }

    /**
     * Hitches each trailer in turn, checking capacity through the same {@code canTow} rule the single-unit tow menu
     * uses so the two paths cannot disagree. Anything already attached is detached again if a later trailer fails.
     */
    private void applyTrain(Entity tractor, List<Entity> trailers) {
        List<Entity> attached = new ArrayList<>();

        for (Entity trailer : trailers) {
            // Trailers are always appended at the tail, so the attach point is the last unit in the train so far.
            Entity attachPoint = attached.isEmpty() ? tractor : attached.get(attached.size() - 1);

            if (!attachPoint.canTow(trailer.getId())) {
                LOGGER.warn("{} rejected: {} cannot tow {} (capacity, hitches or elevation) - rolling back {} link(s)",
                      LOG_TAG, attachPoint.getDisplayName(), trailer.getDisplayName(), attached.size());
                rollBack(tractor, attached);
                return;
            }

            tractor.towUnit(trailer.getId());

            // canTow checks the same hitch towUnit uses, so this should not fail. Confirm rather than assume, since
            // a trailer that is not actually hitched would otherwise be reported as part of a completed train.
            if (trailer.getTowedBy() == Entity.NONE) {
                LOGGER.warn("{} rejected: {} could not be hitched behind {} - rolling back {} link(s)",
                      LOG_TAG, trailer.getDisplayName(), attachPoint.getDisplayName(), attached.size());
                rollBack(tractor, attached);
                return;
            }

            attached.add(trailer);
            LOGGER.info("{} hitched {} behind {}",
                  LOG_TAG, trailer.getDisplayName(), attachPoint.getDisplayName());
        }

        for (Entity trailer : attached) {
            gameManager.entityUpdate(trailer.getId());
        }
        gameManager.entityUpdate(tractor.getId());

        if (getGame().getPhase().isLounge()) {
            gameManager.sendServerChat(ServerLobbyHelper.entityUpdateMessage(tractor, getGame()));
        }
        LOGGER.info("{} train complete: {} now tows {} trailer(s)",
              LOG_TAG, tractor.getDisplayName(), attached.size());
    }

    /** Detaches everything attached so far, leaving the units as they were before the request. */
    private void rollBack(Entity tractor, List<Entity> attached) {
        if (attached.isEmpty()) {
            return;
        }
        // Disconnecting the first trailer cascades to every trailer behind it, which is exactly the rollback wanted.
        tractor.disconnectUnit(attached.get(0).getId());
        for (Entity trailer : attached) {
            gameManager.entityUpdate(trailer.getId());
        }
        gameManager.entityUpdate(tractor.getId());
        LOGGER.info("{} rolled back partial train on {}", LOG_TAG, tractor.getDisplayName());
    }
}
