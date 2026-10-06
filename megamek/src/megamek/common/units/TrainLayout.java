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

package megamek.common.units;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import megamek.common.board.Coords;
import megamek.common.annotations.Nullable;
import megamek.common.game.Game;
import megamek.logging.MMLogger;

/**
 * Stateless helper that lays a tractor's trailers out along a path of hexes.
 * <p>
 * How a train packs into hexes is a rules matter: "Small and medium Trailers act as part of the Tractor Support
 * Vehicle for purposes of movement, stacking and firing (two small and medium Trailers together act as a single
 * Support Vehicle). Large Trailers are treated as individual units for purposes of stacking" (TM, Trailers). In
 * practice that means the first trailer shares the tractor's hex, further trailers pack two to a hex behind it, and
 * superheavy units take a hex to themselves.
 * </p>
 * The path and facings are supplied by the caller so the same rule serves both movement, which follows the hexes the
 * tractor actually drove through, and deployment, which lays the train out behind the hex the tractor is placed in.
 * Extracted from {@code TWGameManager.processTrailerMovement} so the packing rule has one implementation; this mirrors
 * the codebase's other Entity-operating utilities.
 *
 * @author Claude Code (Opus 5)
 */
public final class TrainLayout {

    private static final MMLogger LOGGER = MMLogger.create(TrainLayout.class);

    private static final String LOG_TAG = "[Train]";

    private TrainLayout() {
    }

    /**
     * Where a single trailer ends up.
     *
     * @param entityId the trailer
     * @param position the hex it occupies
     * @param facing   the facing it takes, or {@code null} to keep its current facing
     */
    public record TrainPlacement(int entityId, Coords position, Integer facing) {
    }

    /**
     * Works out where every trailer towed by the given tractor would sit, without moving anything. Callers that want
     * to know whether a placement is legal before committing to it use this; {@link #layOutTrain} applies the result.
     *
     * @param game          the game holding the towed units
     * @param tractor       the powered tractor at the head of the train
     * @param tractorHex    the hex the tractor occupies, passed in so a placement can be tested before the tractor is
     *                      actually there
     * @param tractorFacing the facing the tractor has in that hex
     * @param trainPath     hexes the train occupies, ordered so that the last entry is the tractor's hex and earlier
     *                      entries lie progressively further back along the train
     * @param trainFacings  facings matching {@code trainPath}, ordered the same way
     *
     * @return one placement per towed unit, in train order
     */
    public static List<TrainPlacement> computeLayout(Game game, Entity tractor, Coords tractorHex, int tractorFacing,
          List<Coords> trainPath, List<Integer> trainFacings) {
        List<TrainPlacement> placements = new ArrayList<>();

        List<Integer> towedUnits = tractor.getAllTowedUnits();
        for (int trailerNumber = 0; trailerNumber < towedUnits.size(); trailerNumber++) {
            Entity trailer = game.getEntity(towedUnits.get(trailerNumber));

            if (trailer == null) {
                continue;
            }

            int towedId = towedUnits.get(trailerNumber);
            // Offset so we get the right position index
            double trailerPositionOffset = (trailerNumber + 1);

            // Unless the tractor is superheavy, put the first trailer in its hex.
            // Technically this would be true for a superheavy trailer too, but only a
            // superheavy tractor can tow one.
            if ((trailerNumber == 0) && !tractor.isSuperHeavy()) {
                placements.add(new TrainPlacement(towedId, tractorHex, tractorFacing));
                continue;
            }

            if (trailer.isSuperHeavy()) {
                // If the trailer is superheavy, place it in a hex by itself
                trailerPositionOffset++;
            } else if (tractor.isSuperHeavy()) {
                // If the tractor is superheavy, we can put two trailers in each hex
                // starting trailer 0 in the hex behind the tractor
                trailerPositionOffset = (Math.ceil((trailerPositionOffset / 2.0)) + 1);
            } else {
                // Otherwise, we can put two trailers in each hex
                // starting trailer 1 in the hex behind the tractor
                trailerPositionOffset++;
                trailerPositionOffset = Math.ceil((trailerPositionOffset / 2.0));
            }

            int stepNumber = (trainPath.size() - (int) trailerPositionOffset);
            if (stepNumber < 0) {
                // The path is shorter than the train. Movement always supplies a path at least as long as the train,
                // and deployment builds one to fit, so this means a caller got the path wrong.
                LOGGER.warn("{} train path of {} hex(es) is too short for trailer {} of {}; keeping its position",
                      LOG_TAG, trainPath.size(), trailerNumber + 1, tractor.getDisplayName());
                continue;
            }

            Integer facing = null;
            if ((trainFacings.size() - trailerPositionOffset) >= 0) {
                facing = trainFacings.get(trainFacings.size() - (int) trailerPositionOffset);
            }
            placements.add(new TrainPlacement(towedId, trainPath.get(stepNumber), facing));
        }

        return placements;
    }

    /**
     * Sets the position and facing of every trailer towed by the given tractor. Movement state and any server-side
     * notification are the caller's business; this only places the units.
     *
     * @param game         the game holding the towed units
     * @param tractor      the powered tractor at the head of the train
     * @param trainPath    hexes the train occupies, last entry first
     * @param trainFacings facings matching {@code trainPath}
     */
    public static void layOutTrain(Game game, Entity tractor, List<Coords> trainPath, List<Integer> trainFacings) {
        applyLayout(game, computeLayout(game, tractor, tractor.getPosition(), tractor.getFacing(),
              trainPath, trainFacings));
    }

    /** Moves each trailer to its computed placement. */
    public static void applyLayout(Game game, List<TrainPlacement> placements) {
        for (TrainPlacement placement : placements) {
            Entity trailer = game.getEntity(placement.entityId());
            if (trailer == null) {
                continue;
            }
            trailer.setPosition(placement.position());
            if (placement.facing() != null) {
                trailer.setFacing(placement.facing());
            }
        }
    }

    /** Returned by {@link #trainPosition(Entity)} when the unit is not part of a train. */
    public static final int NOT_IN_TRAIN = -1;

    /** Returned by {@link #trainPosition(Entity)} for the tractor heading a train. */
    public static final int TRACTOR_POSITION = 0;

    /**
     * Returns where a unit sits in its train: {@link #TRACTOR_POSITION} for the tractor heading it, then 1, 2 and so
     * on back along the trailers, or {@link #NOT_IN_TRAIN} when the unit is not part of one.
     * <p>
     * The position is what a player needs to know. Ordering fixes the hitch chain and so which hex each unit
     * occupies, and with several identical carriages the name alone does not say which is which. The wording is left
     * to the caller, since this is common code and the labels a player reads belong in the UI's resource bundle.
     * </p>
     *
     * @param unit the unit to locate
     *
     * @return the position in the train, or {@link #NOT_IN_TRAIN}
     */
    public static int trainPosition(@Nullable Entity unit) {
        if ((unit == null) || (unit.getGame() == null)) {
            return NOT_IN_TRAIN;
        }

        // A tractor heads its own train: it tows units but is not towed itself.
        if (unit.getTractor() == Entity.NONE) {
            return unit.getAllTowedUnits().isEmpty() ? NOT_IN_TRAIN : TRACTOR_POSITION;
        }

        Entity tractor = unit.getGame().getEntity(unit.getTractor());
        if (tractor == null) {
            return NOT_IN_TRAIN;
        }

        int trailerIndex = tractor.getAllTowedUnits().indexOf(unit.getId());
        return (trailerIndex < 0) ? NOT_IN_TRAIN : (trailerIndex + 1);
    }

    public static List<Coords> deploymentPath(Coords tractorHex, int facing, int trailerCount) {
        int rearDirection = (facing + 3) % 6;
        List<Coords> path = new ArrayList<>();
        Coords current = tractorHex;
        // One hex per trailer plus the tractor's own covers even the worst packing, where every trailer takes a hex
        // to itself.
        for (int step = 0; step <= trailerCount; step++) {
            path.add(current);
            current = current.translated(rearDirection);
        }
        Collections.reverse(path);
        return path;
    }

    /**
     * Every hex a train would occupy if its tractor were placed in the given hex, including the tractor's own.
     * <p>
     * Used to check a placement before committing to it: under the strict reading of the deployment rules the whole
     * train has to land in a legal deployment zone, not just the tractor.
     * </p>
     *
     * @return the distinct occupied hexes; just the tractor's hex when it tows nothing
     */
    public static Set<Coords> deploymentFootprint(Game game, Entity tractor, Coords tractorHex, int facing) {
        Set<Coords> footprint = new LinkedHashSet<>();
        footprint.add(tractorHex);

        int trailerCount = tractor.getAllTowedUnits().size();
        if (trailerCount == 0) {
            return footprint;
        }

        List<Coords> path = deploymentPath(tractorHex, facing, trailerCount);
        List<Integer> facings = new ArrayList<>();
        for (int index = 0; index < path.size(); index++) {
            facings.add(facing);
        }

        for (TrainPlacement placement : computeLayout(game, tractor, tractorHex, facing, path, facings)) {
            footprint.add(placement.position());
        }
        return footprint;
    }
}
