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
import java.util.List;
import java.util.TreeMap;

import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.IGame;
import megamek.common.MovePath;
import megamek.common.MovePath.MoveStepType;

public class PathSearcher {

    private Princess owner;

    public PathSearcher(Princess owner) {
        this.owner = owner;
    }

    public class WeightedPath implements Comparable<Object> {
        public WeightedPath(MovePath a, double w) {
            path = a;
            weight = w;
        }

        public double weight;
        public MovePath path;

        public int compareTo(Object o) {
            if (o instanceof WeightedPath)
                return compareTo(o);

            return 0;
        }
    }

    public class PathState implements Comparable<PathState> {
        public PathState(Coords l, int f) {
            location = l;
            facing = f;
            hulldown = false;
            isjumping = false;
        }

        public PathState(MovePath p) {
            location = p.getFinalCoords();
            facing = p.getFinalFacing();
            hulldown = p.getFinalHullDown() || p.getFinalProne();
            isjumping = p.isJumping();
        }

        public int compareTo(PathState po) {
            final String METHOD_NAME = "compareTo(PathState)";
            owner.methodBegin(getClass(), METHOD_NAME);
            try {
                if (facing < po.facing) {
                    return -1;
                }
                if (po.facing < facing) {
                    return 1;
                }
                if (location.x < po.location.x) {
                    return -1;
                }
                if (po.location.x < location.x) {
                    return 1;
                }
                if (location.y < po.location.y) {
                    return -1;
                }
                if (po.location.y < location.y) {
                    return 1;
                }
                if (po.hulldown && (!hulldown)) {
                    return -1;
                }
                if (!po.hulldown && (hulldown)) {
                    return 1;
                }
                if (po.isjumping && (!isjumping)) {
                    return -1;
                }
                if (!po.isjumping && isjumping) {
                    return 1;
                }

                return 0;
            } finally {
                owner.methodEnd(getClass(), METHOD_NAME);
            }
        }

        public Coords location;
        public int facing;
        public boolean hulldown;
        public boolean isjumping;

    }

    /**
     * Breadth first search through all legal moves, with only the shortest path
     * to each location/facing considered
     */
    ArrayList<WeightedPath> seeAllPaths(ArrayList<WeightedPath> start_paths,
            boolean forward, boolean backward, IGame game,
            TreeMap<PathState, WeightedPath> pathmap) {
        final String METHOD_NAME = "seeAllPaths(ArrayList<WeightedPath>, boolean, boolean, IGame, TreeMap<PathState, WeightedPath>)";
        owner.methodBegin(getClass(), METHOD_NAME);
        try {
            ArrayList<WeightedPath> next_steps = new ArrayList<WeightedPath>();
            for (WeightedPath sp : start_paths) {
                List<MovePath> nextmoves = sp.path.getNextMoves(backward, forward);
                for (MovePath p : nextmoves) {
                    if (!p.getLastStep().isLegal())
                    {
                        continue; // don't make illegal moves
                    }
                    if (p.getLastStep().getType() == MovePath.MoveStepType.GET_UP)
                    {
                        if (p.getLastStep().getMpUsed() > sp.path.getEntity()
                                .getRunMP())
                        {
                            continue; // ignore if I'm out of MP
                        }
                    }
                    // check if I've already found a better way to this location
                    PathState mystate = new PathState(p);
                    WeightedPath other_wp = pathmap.get(mystate);
                    if (other_wp != null) {
                        if (other_wp.path.getMpUsed() <= p.getMpUsed()) {
                            continue;
                        }
                    }
                    // ok, path is good, add it to the list and mark it in the
                    // treemap
                    PathSearcher.WeightedPath nextpath = new PathSearcher.WeightedPath(
                            p, ranker.rankPath(p, game));
                    pathmap.put(mystate, nextpath);
                    next_steps.add(nextpath);
                }

            }
            if (next_steps.size() != 0) {
                ArrayList<WeightedPath> all_paths = seeAllPaths(next_steps,
                        forward, backward, game, pathmap);
                all_paths.addAll(start_paths);
                return all_paths;
            }

            return start_paths;
        } finally {
            owner.methodEnd(getClass(), METHOD_NAME);
        }
    }

    ArrayList<WeightedPath> getAllWeightedPaths(Entity entity,IGame game) {
        final String METHOD_NAME = "getAllWeightedPaths(Entity, IGame)";
        owner.methodBegin(getClass(), METHOD_NAME);
        try {
            ranker.initUnitTurn(entity, game);
            TreeMap<PathState, WeightedPath> pathmap = new TreeMap<PathState, WeightedPath>();
            ArrayList<WeightedPath> start_path = new ArrayList<WeightedPath>();

            MovePath empty_path = new MovePath(game, entity);
            double empty_rank = ranker.rankPath(empty_path, game);
            WeightedPath start = new WeightedPath(empty_path, empty_rank);
            pathmap.put(new PathState(start.path), start);
            start_path.add(start);

            if(entity.getJumpMP()>0) { //allow jumping
                MovePath jump_path=new MovePath(game,entity);
                jump_path.addStep(MoveStepType.START_JUMP);
                WeightedPath startjump=new WeightedPath(jump_path,empty_rank-0.1);
                pathmap.put(new PathState(startjump.path),startjump);
                start_path.add(startjump);
            }

            return seeAllPaths(start_path, true, true, game, pathmap);
        } finally {
            owner.methodEnd(getClass(), METHOD_NAME);
        }
    }

    ArrayList<WeightedPath> getTopPaths(Entity entity,IGame game,int npaths) {
        final String METHOD_NAME = "getTopPaths(Entity, IGame, int)";
        owner.methodBegin(getClass(), METHOD_NAME);
        try {
            ArrayList<WeightedPath> allpaths = getAllWeightedPaths(entity,game);
            Collections.sort(allpaths);
            return (ArrayList<WeightedPath>)allpaths.subList(0, npaths);
        } finally {
            owner.methodEnd(getClass(), METHOD_NAME);
        }
    }

    MovePath getBestPath(Entity entity, IGame game) {
        final String METHOD_NAME = "getBestPath(Entity, IGame)";
        owner.methodBegin(getClass(), METHOD_NAME);
        try {
            //System.err.println("Unit: " + entity.getDisplayName() + " is pathing.");
            ArrayList<WeightedPath> allpaths=getAllWeightedPaths(entity,game);

            //System.err.println("choosing between "
            //        + Integer.toString(allpaths.size()) + " paths");
            double min_value = allpaths.get(0).weight;
            double max_value = allpaths.get(0).weight;
            MovePath max_path = allpaths.get(0).path;
            for (WeightedPath wp : allpaths) {
                if (max_value < wp.weight) {
                    max_value = wp.weight;
                    max_path = wp.path;
                }
                if (min_value > wp.weight) {
                    min_value = wp.weight;
                }
            }
            //long stop_time = System.currentTimeMillis();

            //System.err.println("took " + Long.toString(stop_time - start_time)
            //        + " milliseconds");
            //System.err.println("best path value: " + Double.toString(max_value));
            //System.err.println("worst path value: " + Double.toString(min_value));
            //System.err.println("choosing path "
            //        + Integer.toString(max_path.length()) + " steps long");

            return max_path;
        } finally {
            owner.methodEnd(getClass(), METHOD_NAME);
        }
    }

    public PathRanker ranker;
}
