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
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import megamek.client.ui.SharedUtility;
import megamek.common.Compute;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.IGame;
import megamek.common.MovePath;
import megamek.common.TargetRoll;

public class PathRanker {       

    Princess botbase;
    private static Princess owner;

    class RankedPath implements Comparable<RankedPath>{
        public MovePath path;
        public double rank;

        public RankedPath(double r,MovePath p) {
            rank=r;
            path=p;
        }
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
    }

    public PathRanker(Princess princess) {
        botbase = princess;
        owner = princess;
    }

    /**
     * Gives the "utility" of a path; a number representing how good it is.
     * Rankers that extend this class should override this function
     */
    public double rankPath(MovePath p, IGame game) {
        return 0;
    }

    public ArrayList<RankedPath> rankPaths(ArrayList<MovePath> ps,IGame game) {
        final String METHOD_NAME = "rankPaths(ArrayList<MovePath>, IGame)";
        owner.methodBegin(getClass(), METHOD_NAME);

        try {
            ArrayList<RankedPath> ret=new ArrayList<RankedPath>();
            for(MovePath p:ps) {
                ret.add(new RankedPath(rankPath(p,game),p));
            }
            return ret;
        } finally {
            owner.methodEnd(getClass(), METHOD_NAME);
        }
    }

    public static ArrayList<RankedPath> filterPathsLessThan(ArrayList<RankedPath> ps,double lessthan) {
        final String METHOD_NAME = "filterPathsLessThan(ArrayList<Rankedpath>, double)";
        owner.methodBegin(PathRanker.class, METHOD_NAME);

        try {
            ArrayList<RankedPath> ret=new ArrayList<RankedPath>();
            for(RankedPath p:ps) {
                if(p.rank>lessthan) {
                    ret.add(p);
                }
            }
            return ret;
        } finally {
            owner.methodEnd(PathRanker.class, METHOD_NAME);
        }
    }

    public static RankedPath getBestPath(ArrayList<RankedPath> ps) {
        final String METHOD_NAME = "getBestPath(ArrayList<Rankedpath>)";
        owner.methodBegin(PathRanker.class, METHOD_NAME);

        try {
            if(ps.size()==0) {
                return null;
            }
            return Collections.max(ps);
        } finally {
            owner.methodEnd(PathRanker.class, METHOD_NAME);
        }
    }


    /**
     * Performs initialization to help speed later calls of rankPath for this
     * unit on this turn. Rankers that extend this class should override this
     * function
     */
    public void initUnitTurn(Entity unit, IGame game) {
    }

    /**
     * Find the closest enemy to a unit with a path
     */
    static Entity findClosestEnemy(Entity me,Coords position, IGame game) {
        final String METHOD_NAME = "findClosestEnemy(Entity, Coords, IGame)";
        owner.methodBegin(PathRanker.class, METHOD_NAME);

        try {
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
        } finally {
            owner.methodEnd(PathRanker.class, METHOD_NAME);
        }
    }

    /**
     * Find the closest friend to a unit with a path
     */
    Entity findClosestFriend(MovePath p, IGame game) {
        final String METHOD_NAME = "findClosestFriend(MovePath, IGame";
        owner.methodBegin(getClass(), METHOD_NAME);

        try {
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
        } finally {
            owner.methodEnd(getClass(), METHOD_NAME);
        }
    }

    /**
     * Get all the enemies of a unit
     */
    static ArrayList<Entity> getEnemies(Entity myunit, IGame game) {
        final String METHOD_NAME = "getEnemies(Entity, IGame)";
        owner.methodBegin(PathRanker.class, METHOD_NAME);

        try {
            ArrayList<Entity> enemies = new ArrayList<Entity>();
            for (Enumeration<Entity> i = game.getEntities(); i.hasMoreElements();) {
                Entity entity = i.nextElement();
                if (entity.getOwner().isEnemyOf(myunit.getOwner())
                        && (entity.getPosition() != null) && !entity.isOffBoard()) {
                    enemies.add(entity);
                }
            }
            return enemies;
        } finally {
            owner.methodEnd(PathRanker.class, METHOD_NAME);
        }
    }

    /**
     * Get all the friends of a unit
     */
    ArrayList<Entity> getFriends(Entity myunit, IGame game) {
        final String METHOD_NAME = "filterPathsLessThan(ArrayList<Rankedpath>, double)";
        owner.methodBegin(getClass(), METHOD_NAME);

        try {
            ArrayList<Entity> friends = new ArrayList<Entity>();
            for (Enumeration<Entity> i = game.getEntities(); i.hasMoreElements();) {
                Entity entity = i.nextElement();
                if (!entity.getOwner().isEnemyOf(myunit.getOwner())
                        && (entity.getPosition() != null) && !entity.isOffBoard()
                        && (!entity.equals(myunit))) {
                    friends.add(entity);
                }
            }
            return friends;
        } finally {
            owner.methodEnd(getClass(), METHOD_NAME);
        }
    }
    
    /**
     * Returns the probability of success of a movepath
     */
    public static double getMovePathSuccessProbability(MovePath mp) {
    	MovePath pcopy=mp.clone();
        List<TargetRoll> targets = SharedUtility.getPSRList(pcopy);
        double success_probability = 1.0;
        for (TargetRoll t : targets) {
            success_probability *= Compute.oddsAbove(t.getValue()) / 100;
        }
        return success_probability;
    }

}
