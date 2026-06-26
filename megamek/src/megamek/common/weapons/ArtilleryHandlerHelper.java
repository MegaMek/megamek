/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.weapons;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Vector;

import megamek.common.LosEffects;
import megamek.common.Report;
import megamek.common.board.Coords;
import megamek.common.equipment.INarcPod;
import megamek.common.equipment.Minefield;
import megamek.common.game.Game;
import megamek.common.options.OptionsConstants;
import megamek.common.units.Entity;
import megamek.common.units.Targetable;
import megamek.logging.MMLogger;
import megamek.server.totalWarfare.TWGameManager;

/**
 * Contains some helper methods for artillery and similar weapon handlers.
 */
public final class ArtilleryHandlerHelper {
    private static final MMLogger logger = MMLogger.create(ArtilleryHandlerHelper.class);

    public static Optional<Entity> findSpotter(List<Integer> spottersBefore, int playerId, Game game,
          Targetable target) {
        Entity bestSpotter = null;
        logger.debug("findSpotter called: spottersBefore={}, playerId={}, target={}",
              spottersBefore, playerId, target);

        // Are there any valid spotters?
        if (null != spottersBefore) {
            // fetch possible spotters now
            Iterator<Entity> spottersAfter = game.getSelectedEntities(entity -> {
                Integer id = entity.getId();
                boolean sameOwner = (playerId == entity.getOwnerId());
                boolean inList = spottersBefore.contains(id);
                boolean hasLOS = !LosEffects.calculateLOS(game, entity, target, true).isBlocked();
                boolean active = entity.isActive();
                boolean notAirborne = !(entity.isAero() && entity.isAirborne());
                boolean notHaywired = !entity.isINarcedWith(INarcPod.HAYWIRE);

                logger.debug(
                      "  Checking entity {}: sameOwner={}, inList={}, hasLOS={}, active={}, notAirborne={}, notHaywired={}",
                      entity.getDisplayName(),
                      sameOwner,
                      inList,
                      hasLOS,
                      active,
                      notAirborne,
                      notHaywired);

                return sameOwner && inList && hasLOS && active && notAirborne && notHaywired;
            });

            // Out of any valid spotters, pick the best.
            while (spottersAfter.hasNext()) {
                Entity spotter = spottersAfter.next();
                logger.debug("  Valid spotter found: {}", spotter.getDisplayName());
                if (bestSpotter == null) {
                    bestSpotter = spotter;
                } else if (isForwardObserver(spotter) && !isForwardObserver(bestSpotter)) {
                    bestSpotter = spotter;
                } else if (spotter.getCrew().getGunnery() < bestSpotter.getCrew().getGunnery()
                      && !isForwardObserver(bestSpotter)) {
                    bestSpotter = spotter;
                } else if (isForwardObserver(bestSpotter) && isForwardObserver(spotter)) {
                    if (spotter.getCrew().getGunnery() < bestSpotter.getCrew().getGunnery()) {
                        bestSpotter = spotter;
                    }
                }
            }
        } else {
            logger.debug("  spottersBefore is null!");
        }
        logger.debug("findSpotter result: {}", bestSpotter != null ? bestSpotter.getDisplayName() : "none");
        return Optional.ofNullable(bestSpotter);
    }

    /**
     * @param entity The unit in question
     *
     * @return True when the given unit has the Forward Observer ability
     *
     * @see OptionsConstants#MISC_FORWARD_OBSERVER
     */
    public static boolean isForwardObserver(Entity entity) {
        return entity.hasAbility(OptionsConstants.MISC_FORWARD_OBSERVER);
    }

    public static void clearMines(Vector<Report> reports, Coords coords, Game game, Entity attacker,
          TWGameManager gameManager) {
        if (game.containsMinefield(coords)) {
            getMinefields(reports, coords, game, attacker, gameManager);
        }
    }

    public static void getMinefields(Vector<Report> reports, Coords coords, Game game, Entity attacker,
          TWGameManager gameManager) {
        Enumeration<Minefield> minefields = game.getMinefields(coords).elements();
        ArrayList<Minefield> mfRemoved = new ArrayList<>();
        while (minefields.hasMoreElements()) {
            Minefield mf = minefields.nextElement();
            if (gameManager.clearMinefield(mf, attacker, 10, reports)) {
                mfRemoved.add(mf);
            }
        }
        for (Minefield mf : mfRemoved) {
            gameManager.removeMinefield(mf);
        }
    }

    private ArtilleryHandlerHelper() {}
}
