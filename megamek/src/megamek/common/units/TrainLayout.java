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

import java.util.List;

import megamek.common.board.Coords;
import megamek.common.game.Game;

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

    private TrainLayout() {
    }

    /**
     * Sets the position and facing of every trailer towed by the given tractor. Movement state and any server-side
     * notification are the caller's business; this only places the units.
     *
     * @param game        the game holding the towed units
     * @param tractor     the powered tractor at the head of the train
     * @param trainPath   hexes the train occupies, ordered so that the last entry is the tractor's own hex and earlier
     *                    entries lie progressively further back along the train
     * @param trainFacings facings matching {@code trainPath}, ordered the same way. A trailer keeps its current facing
     *                    when the list is too short to cover its position.
     */
    public static void layOutTrain(Game game, Entity tractor, List<Coords> trainPath, List<Integer> trainFacings) {
        for (int towedId : tractor.getAllTowedUnits()) {
            Entity trailer = game.getEntity(towedId);

            if (trailer == null) {
                continue;
            }

            int trailerNumber = tractor.getAllTowedUnits().indexOf(towedId);
            // Offset so we get the right position index
            double trailerPositionOffset = (trailerNumber + 1);

            // Unless the tractor is superheavy, put the first trailer in its hex.
            // Technically this would be true for a superheavy trailer too, but only a
            // superheavy tractor can tow one.
            if ((trailerNumber == 0) && !tractor.isSuperHeavy()) {
                trailer.setPosition(tractor.getPosition());
                trailer.setFacing(tractor.getFacing());
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
            trailer.setPosition(trainPath.get(stepNumber));
            if ((trainFacings.size() - trailerPositionOffset) >= 0) {
                trailer.setFacing(trainFacings.get(trainFacings.size() - (int) trailerPositionOffset));
            }
        }
    }
}
