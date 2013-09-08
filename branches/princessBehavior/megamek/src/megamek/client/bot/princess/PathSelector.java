/*
 * MegaMek - Copyright (C) 2000-2011 Ben Mazur (bmazur@sev.org)
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */
package megamek.client.bot.princess;

import java.util.ArrayList;
import java.util.Enumeration;

import megamek.common.Aero;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.IGame;
import megamek.common.MovePath;
import megamek.common.MoveStep;
import megamek.common.util.Logger;

public class PathSelector {

    private Princess owner;

    public PathSelector(Princess owner) {
        this.owner = owner;
    }

    int min(int a, int b) {
        return a < b ? a : b;
    }

    public MovePath selectPath(IGame game, ArrayList<MovePath> paths) {
        final String METHOD_NAME = "selectPath(IGame, ArrayList<MovePath>)";
        Logger.methodBegin(getClass(), METHOD_NAME);

        try {
            if (paths.size() == 0) {
                return null;
            }
            Entity entity = paths.get(0).getEntity();
            if (entity instanceof Aero) {
                for (MovePath p : paths) {
                    // chooses the first path that overflies an enemy
                    for (Enumeration<MoveStep> e = p.getSteps(); e
                            .hasMoreElements(); ) {
                        Coords cord = (e.nextElement()).getPosition();
                        Entity enemy = game.getFirstEnemyEntity(cord, entity);
                        if (enemy != null) {
                            return p;
                        }
                    }
                }

                // dislikes:
                /*
                 * //ending in a square that I can't turn around from Coords
                 * finalcoords=p.getFinalCoords(); int remaining_hexes;
                 * switch(p.getFinalFacing()) { case 0:
                 * remaining_hexes=finalcoords.y; case 1:
                 * remaining_hexes=min(finalcoords
                 * .y,game.getBoard().getWidth()-finalcoords.x); case 2:
                 * remaining_hexes
                 * =min(game.getBoard().getHeight()-finalcoords.y,game
                 * .getBoard().getWidth()-finalcoords.x); case 3:
                 * remaining_hexes=game.getBoard().getHeight()-finalcoords.y; case
                 * 4: remaining_hexes=min(game.getBoard().getHeight()-finalcoords.y,
                 * finalcoords.x); case 5:
                 * remaining_hexes=min(finalcoords.y,finalcoords.x); }
                 */

                return paths.get(0);
            } else {
                return paths.get(0);
            }
        } finally {
            Logger.methodEnd(getClass(), METHOD_NAME);
        }
    }

}
