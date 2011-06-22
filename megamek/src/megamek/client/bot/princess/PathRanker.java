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

import megamek.common.Entity;
import megamek.common.IGame;
import megamek.common.MovePath;

public class PathRanker {

    public PathRanker() {
    };

    /**
     * Gives the "utility" of a path; a number representing how good it is.
     * Rankers that extend this class should override this function
     */
    public double rankPath(MovePath p, IGame game) {
        return 0;
    };

    /**
     * Performs initialization to help speed later calls of rankPath for this
     * unit on this turn. Rankers that extend this class should override this
     * function
     */
    public void initUnitTurn(Entity unit, IGame game) {
    };

    /**
     * Find the closest enemy to a unit with a path
     */
    Entity findClosestEnemy(MovePath p, IGame game) {
        int range = 9999;
        Entity closest = null;
        ArrayList<Entity> enemies = getEnemies(p.getEntity(), game);
        for (Entity e : enemies) {
            if (p.getFinalCoords().distance(e.getPosition()) < range) {
                range = p.getFinalCoords().distance(e.getPosition());
                closest = e;
            }
        }
        return closest;
    }

    /**
     * Find the closest friend to a unit with a path
     */
    Entity findClosestFriend(MovePath p, IGame game) {
        int range = 9999;
        Entity closest = null;
        ArrayList<Entity> friends = getFriends(p.getEntity(), game);
        for (Entity e : friends) {
            if (p.getFinalCoords().distance(e.getPosition()) < range) {
                range = p.getFinalCoords().distance(e.getPosition());
                closest = e;
            }
        }
        return closest;
    }

    /**
     * Get all the enemies of a unit
     */
    ArrayList<Entity> getEnemies(Entity myunit, IGame game) {
        ArrayList<Entity> enemies = new ArrayList<Entity>();
        for (Enumeration<Entity> i = game.getEntities(); i.hasMoreElements();) {
            Entity entity = i.nextElement();
            if (entity.getOwner().isEnemyOf(myunit.getOwner())
                    && (entity.getPosition() != null) && !entity.isOffBoard()) {
                enemies.add(entity);
            }
        }
        return enemies;
    }

    /**
     * Get all the enemies of a unit
     */
    ArrayList<Entity> getFriends(Entity myunit, IGame game) {
        ArrayList<Entity> friends = new ArrayList<Entity>();
        for (Enumeration<Entity> i = game.getEntities(); i.hasMoreElements();) {
            Entity entity = i.nextElement();
            if (!entity.getOwner().isEnemyOf(myunit.getOwner())
                    && (entity.getPosition() != null) && !entity.isOffBoard()
                    && (entity != myunit)) {
                friends.add(entity);
            }
        }
        return friends;
    }

}
