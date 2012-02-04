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
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;

import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.IGame;
import megamek.common.MovePath;

public class PathRanker {       

    Princess botbase;

    class RankedPath implements Comparable<RankedPath>{
        public RankedPath() {};
        public RankedPath(double r,MovePath p) {
            rank=r;
            path=p;
        }
        public MovePath path;
        public double rank;
        public int compareTo(RankedPath p) {
            if(rank<p.rank) {
                return -1;
            }
            if(p.rank<rank) {
                return 1;
            }
            if(path.getKey().hashCode()<p.path.getKey().hashCode()) {
                return -1;
            }
            if(path.getKey().hashCode()>p.path.getKey().hashCode()) {
                return 1;
            }
            return 0;
        }
    };

    public PathRanker() {
    };

    /**
     * Gives the "utility" of a path; a number representing how good it is.
     * Rankers that extend this class should override this function
     */
    public double rankPath(MovePath p, IGame game) {
        return 0;
    };

    public ArrayList<RankedPath> rankPaths(ArrayList<MovePath> ps,IGame game) {
        ArrayList<RankedPath> ret=new ArrayList<RankedPath>();
        for(MovePath p:ps) {
            ret.add(new RankedPath(rankPath(p,game),p));
        }
        return ret;
    }

    public static ArrayList<RankedPath> filterPathsLessThan(ArrayList<RankedPath> ps,double lessthan) {
        ArrayList<RankedPath> ret=new ArrayList<RankedPath>();
        for(RankedPath p:ps) {
            if(p.rank>lessthan) {
                ret.add(p);
            }
        }
        return ret;
    }

    public static RankedPath getBestPath(ArrayList<RankedPath> ps) {
        if(ps.size()==0) {
            return null;
        }
        return Collections.max(ps);
        
//        RankedPath best=ps.get(0);
//        for(RankedPath p:ps) {
//            if(p.rank>best.rank) {
//                best=p;
//            }
//        }
//        return best;
    }


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
    static Entity findClosestEnemy(Entity me,Coords position, IGame game) {
        int range = 9999;
        Entity closest = null;
        ArrayList<Entity> enemies = getEnemies(me, game);
        for (Entity e : enemies) {
            if (position.distance(e.getPosition()) < range) {
                range = position.distance(e.getPosition());
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
    static ArrayList<Entity> getEnemies(Entity myunit, IGame game) {
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
