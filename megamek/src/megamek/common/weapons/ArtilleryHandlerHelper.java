/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.common.weapons;

import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.Game;
import megamek.common.INarcPod;
import megamek.common.LosEffects;
import megamek.common.Minefield;
import megamek.common.Report;
import megamek.common.Targetable;
import megamek.common.options.OptionsConstants;
import megamek.server.totalwarfare.TWGameManager;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Vector;

/**
 * Contains some helper methods for artillery and similar weapon handlers.
 */
public final class ArtilleryHandlerHelper {

    public static Optional<Entity> findSpotter(List<Integer> spottersBefore, int playerId, Game game,
          Targetable target) {
        Entity bestSpotter = null;

        // Are there any valid spotters?
        if (null != spottersBefore) {
            // fetch possible spotters now
            Iterator<Entity> spottersAfter = game.getSelectedEntities(entity -> {
                Integer id = entity.getId();
                return (playerId == entity.getOwnerId())
                      && spottersBefore.contains(id)
                      && !LosEffects.calculateLOS(game, entity, target, true).isBlocked()
                      && entity.isActive()
                      // airborne aeros can't spot for arty
                      && !(entity.isAero() && entity.isAirborne())
                      && !entity.isINarcedWith(INarcPod.HAYWIRE);
            });

            // Out of any valid spotters, pick the best.
            while (spottersAfter.hasNext()) {
                Entity spotter = spottersAfter.next();
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
        }
        return Optional.ofNullable(bestSpotter);
    }

    /**
     * @param entity The unit in question
     * @return True when the given unit has the Forward Observer ability
     * @see OptionsConstants#MISC_FORWARD_OBSERVER
     */
    public static boolean isForwardObserver(Entity entity) {
        return entity.hasAbility(OptionsConstants.MISC_FORWARD_OBSERVER);
    }

    public static void clearMines(Vector<Report> reports, Coords coords, Game game, Entity attacker,
          TWGameManager gameManager) {
        if (game.containsMinefield(coords)) {
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
    }

    private ArtilleryHandlerHelper() { }
}
