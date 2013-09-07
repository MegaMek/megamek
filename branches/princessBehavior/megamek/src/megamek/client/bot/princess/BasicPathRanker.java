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
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.TreeMap;

import megamek.client.bot.princess.BotGeometry.CoordFacingCombo;
import megamek.client.bot.princess.BotGeometry.HexLine;
import megamek.client.bot.princess.FireControl.EntityState;
import megamek.client.bot.princess.FireControl.FiringPlan;
import megamek.client.bot.princess.FireControl.PhysicalAttackType;
import megamek.client.ui.SharedUtility;
import megamek.common.Aero;
import megamek.common.Building;
import megamek.common.Building.BasementType;
import megamek.common.Compute;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.IGame;
import megamek.common.MovePath;
import megamek.common.MovePath.MoveStepType;
import megamek.common.TargetRoll;
import megamek.common.Targetable;
import megamek.common.util.LogLevel;
import megamek.common.util.Logger;

/**
 * A very basic pathranker
 */
public class BasicPathRanker extends PathRanker {

    FireControl firecontrol;
    PathEnumerator path_enumerator;
    private static Princess owner;

    double fall_shame; // how many extra damage points will a fall effectively
    // make me lose
    //double blind_optimism; // unmoved units that can do damage to me -might-
    // move away (<1) or might move to a better position
    // (>1)
    //double enemy_underestimation; // unmoved units will likely move out of my
    // way (<1)
    double foolish_bravery; // how many of my armor points am I willing to
    // sacrifice to do one armor point worth of damage
    double hyper_aggression; // how much is it worth to me to get all up in the
    // face of an enemy
    //    double herd_mentality; // how much is it worth to me to stay near my buddies

    //Fleeing the board
    double self_preservation; //how closely will I follow the forced withdrawal rules.

    Properties properties = null;

    static HomeEdge defaultHomeEdge = HomeEdge.NORTH;

    TreeMap<Integer, Double> best_damage_by_enemies; // the best damage enemies
    // could expect were I not
    // here. Used to determine
    // whether they will target
    // me.

    public BasicPathRanker(Properties props, Princess owningPrincess) {
        super(owningPrincess);
        // Replaced by call to resetParametersFromProperties()
        /*
        // give some default values for tunable parameters
        fall_shame = 10.0;
//        blind_optimism = 0.9;
  //      enemy_underestimation = 0.5;
        foolish_bravery = 3.0;
        //hyper_aggression = 0.05;
        hyper_aggression = 10.0;
        //herd_mentality = 0.01;
        self_preservation = 30.0;

        fall_shame = Double.valueOf(props.getProperty("fall_shame"));
    //    blind_optimism = Double.valueOf(props.getProperty("blind_optimism"));
      //  enemy_underestimation = Double.valueOf(props
        //        .getProperty("enemy_underestimation"));
        foolish_bravery = Double.valueOf(props.getProperty("foolish_bravery"));
        //hyper_aggression = Double
//                .valueOf(props.getProperty("hyper_aggression"));
        //herd_mentality = Double.valueOf(props.getProperty("herd_mentality"));

        self_preservation = Double.valueOf(props.getProperty("self_preservation"));
        */
        best_damage_by_enemies = new TreeMap<Integer, Double>();

        defaultHomeEdge = HomeEdge.getHomeEdge(Integer.valueOf(props.getProperty("home_edge")));
        owner = owningPrincess;
        properties = props;

        resetParametersFromProperties();
    }

    public double getFall_shame() {
        return fall_shame;
    }

    public void setFall_shame(double fall_shame) {
        this.fall_shame = fall_shame;
    }

    public double getFoolish_bravery() {
        return foolish_bravery;
    }

    public void setFoolish_bravery(double foolish_bravery) {
        this.foolish_bravery = foolish_bravery;
    }

    public double getHyper_aggression() {
        return hyper_aggression;
    }

    public void setHyper_aggression(double hyper_aggression) {
        this.hyper_aggression = hyper_aggression;
    }

    public double getSelf_preservation() {
        return self_preservation;
    }

    public void setSelf_preservation(double self_preservation) {
        this.self_preservation = self_preservation;
    }

    public static HomeEdge getDefaultHomeEdge() {
        return defaultHomeEdge;
    }

    public void resetParametersFromProperties() {
        // give some default values for tunable parameters
        fall_shame = 10.0;
        foolish_bravery = 3.0;
        hyper_aggression = 10.0;
        self_preservation = 30.0;

        fall_shame = Double.valueOf(properties.getProperty("fall_shame"));
        foolish_bravery = Double.valueOf(properties.getProperty("foolish_bravery"));
        hyper_aggression = Double.valueOf(properties.getProperty("hyper_aggression"));
        self_preservation = Double.valueOf(properties.getProperty("self_preservation"));
    }

    class EntityEvaluationResponse {
        public EntityEvaluationResponse() {
            damage_enemy_can_do = 0;
            damage_i_can_do = 0;
        }

        public double damage_enemy_can_do;
        public double damage_i_can_do;
    }

    /**
     * Guesses a number of things about an enemy that has not yet moved
     * TODO estimated damage is sloppy.  Improve for missile attacks, gun skill, and range
     */
    public EntityEvaluationResponse evaluateUnmovedEnemy(Entity e, MovePath p, IGame game) {
        final String METHOD_NAME = "EntityEvaluationResponse evaluateUnmovedEnemy(Entity,MovePath,IGame)";
        Logger.methodBegin(getClass(), METHOD_NAME);

        try {
            //some preliminary calculations
            double damage_discount = 0.25;
            EntityEvaluationResponse ret = new EntityEvaluationResponse();
            //Aeros always move after other units, and would require an entirely different evaluation
            //TODO (low priority) implement a way to see if I can dodge aero units
            if (e instanceof Aero) {
                return ret;
            }
            Coords mycoords = p.getFinalCoords();
            int myfacing = p.getFinalFacing();
            Coords behind = mycoords.translated((myfacing + 3) % 6);
            Coords leftflank = mycoords.translated((myfacing + 2) % 6);
            Coords rightflank = mycoords.translated((myfacing + 4) % 6);
            HashSet<CoordFacingCombo> enemy_facing_set = path_enumerator.unit_potential_locations.get(e.getId());
            Coords closest = path_enumerator.unit_movable_areas.get(e.getId()).getClosestCoordsTo(mycoords);
            int range = closest.distance(mycoords);
            //I would prefer if the enemy must end its move in my line of fire
            //if so, I can guess that I may do some damage to it
            //(cover nonwithstanding.  At the very least, I can force the enemy to take
            // cover on its move)
            HexLine leftbounds;
            HexLine rightbounds;
            if (p.getEntity().canChangeSecondaryFacing()) {
                leftbounds = new HexLine(behind, (myfacing + 2) % 6);
                rightbounds = new HexLine(behind, (myfacing + 4) % 6);
            } else {
                leftbounds = new HexLine(behind, (myfacing + 1) % 6);
                rightbounds = new HexLine(behind, (myfacing + 5) % 6);
            }
            if ((leftbounds.judgeArea(path_enumerator.unit_movable_areas.get(e.getId())) > 0) &&
                (rightbounds.judgeArea(path_enumerator.unit_movable_areas.get(e.getId())) < 0)) {
                ret.damage_i_can_do += firecontrol.getMaxDamageAtRange(p.getEntity(), range) * damage_discount;
            }
            //in general if an enemy can end its position in range, it can hit me
            ret.damage_enemy_can_do += firecontrol.getMaxDamageAtRange(e, range) * damage_discount;
            //It is especially embarassing if the enemy can move behind or flank me and then kick me
            if (enemy_facing_set != null) {
                if (enemy_facing_set.contains(new CoordFacingCombo(behind, myfacing)) ||
                    enemy_facing_set.contains(new CoordFacingCombo(behind, (myfacing + 1) % 6)) ||
                    enemy_facing_set.contains(new CoordFacingCombo(behind, (myfacing + 5) % 6)) ||
                    enemy_facing_set.contains(new CoordFacingCombo(leftflank, myfacing)) ||
                    enemy_facing_set.contains(new CoordFacingCombo(leftflank, (myfacing + 4) % 6)) ||
                    enemy_facing_set.contains(new CoordFacingCombo(leftflank, (myfacing + 5) % 6)) ||
                    enemy_facing_set.contains(new CoordFacingCombo(rightflank, myfacing)) ||
                    enemy_facing_set.contains(new CoordFacingCombo(rightflank, (myfacing + 1) % 6)) ||
                    enemy_facing_set.contains(new CoordFacingCombo(rightflank, (myfacing + 2) % 6))) {
                    ret.damage_enemy_can_do += Math.ceil(e.getWeight() / 5.0) * damage_discount;
                }
            } else {
                Logger.log(getClass(), METHOD_NAME, LogLevel.WARNING, "warning, " +
                                                                      "no facing set for " + e.getDisplayName
                        ());
            }
            return ret;
        } finally {
            Logger.methodEnd(getClass(), METHOD_NAME);
        }
    }

    /**
     * A path ranking
     */
    @Override
    public double rankPath(MovePath p, IGame game) {
        final String METHOD_NAME = "rankPath(MovePath, IGame)";
        Logger.methodBegin(getClass(), METHOD_NAME);

        try {
            boolean isaero = (p.getEntity() instanceof Aero);
            if (isaero) {
                //stalling is bad
                if (p.getFinalVelocity() == 0) {
                    return -1000;
                }
                //you know what else is bad?  this
                if (p.getFinalAltitude() < 1) {
                    return -10000;
                }
                //flying off board should only be done if necessary, but is better than taking much damage
                if ((p.getLastStep() != null) && (p.getLastStep().getType() == MoveStepType.RETURN)) {
                    return -5;
                }
            }

            // How likely is it that I will succeed in reaching this path
            MovePath pathCopy = p.clone(); //get psr calls cliptopossible, I may not want to change p
            List<TargetRoll> targets = SharedUtility.getPSRList(pathCopy);
            double successProbability = 1.0;
            for (TargetRoll t : targets) {
                successProbability *= Compute.oddsAbove(t.getValue()) / 100.0;
            }

            //Try not to jump on buildings that cannot support our weight.
            Coords finalCoords = pathCopy.getFinalCoords();
            if (finalCoords != null) {
                Building b = game.getBoard().getBuildingAt(finalCoords);
                if ((b != null) && (pathCopy.isJumping() || (b.getBasement(finalCoords).getValue() > BasementType
                        .NONE.getValue()))) {
                    Logger.log(getClass(), METHOD_NAME, LogLevel.WARNING,
                               "Final hex is on top of a building...");
                    if (b.getCurrentCF(finalCoords) < pathCopy.getEntity().getWeight()) {
                        Logger.log(getClass(), METHOD_NAME, LogLevel.WARNING,
                                   "\tthat cannot hold my weight.");
                        return -1000;
                    }
                }
            }

            // Factor the possibility of MASC failure in like a PSR (even though the penalty is
            // significantly higher).
            if (p.hasActiveMASC()) {
                successProbability *= Compute.oddsAbove(p.getEntity().getMASCTarget()) / 100.0;
            }
            // Lets assume that I will fall if I fail. What's my expected damage
            // (and embarrassment) from that?
            int fall_damage = (int) (p.getEntity().getWeight() / 10);
            double expected_fall_damage = (fall_damage + fall_shame)
                                          * (1.0 - successProbability);

            // look at all of my enemies
            ArrayList<Entity> enemies = getEnemies(p.getEntity(), game);
            double maximum_damage_done = 0;
            double maximum_physical_damage = 0;
            double expected_damage_taken = 0;
            for (Entity e : enemies) {
                if (e.getPosition() == null) {
                    continue; // Skip units not actually on the board.
                }
                if ((!e.isSelectableThisTurn()) || e.isImmobile()) { //For units that have already moved
                    // How much damage can they do to me?
                    double their_damage_potential = firecontrol
                            .guessBestFiringPlanUnderHeatWithTwists(e, null,
                                                                    p.getEntity(), new EntityState(p),
                                                                    (e.getHeatCapacity() - e.heat) + 5, game)
                            .utility;
                    // if they can kick me, and probably hit, they probably will.
                    FireControl.PhysicalInfo theirkick = new FireControl.PhysicalInfo(
                            e, null, p.getEntity(), new EntityState(p),
                            PhysicalAttackType.RIGHT_KICK, game);
                    if (theirkick.prob_to_hit > 0.5) {
                        their_damage_potential += theirkick.expected_damage_on_hit
                                                  * theirkick.prob_to_hit;
                    }

                    // How much damage can I do to them?
                    FiringPlan my_firing_plan;
                    if (p.getEntity() instanceof Aero) {
                        my_firing_plan = firecontrol.guessFullAirToGroundPlan(p.getEntity(), e, new EntityState(e),
                                                                              p, game, false);
                    } else {
                        my_firing_plan = firecontrol.guessBestFiringPlanWithTwists(p.getEntity(), new EntityState(p),
                                                                                   e, null, game);
                    }
                    double my_damage_potential = my_firing_plan.utility;
                    // If I can kick them and probably hit, I probably will
                    FireControl.PhysicalInfo mykick = new FireControl.PhysicalInfo(
                            p.getEntity(), new EntityState(p), e, null,
                            PhysicalAttackType.RIGHT_KICK, game);
                    if (mykick.prob_to_hit > 0.5) {
                        double expected_kick_damage = mykick.expected_damage_on_hit
                                                      * mykick.prob_to_hit;
                        if (expected_kick_damage > maximum_physical_damage) {
                            maximum_physical_damage = expected_kick_damage;
                        }
                    }

                    // If this enemy is likely to fire at me, include that in the damage
                    // I will likely take
                    if (best_damage_by_enemies.get(e.getId()) < their_damage_potential) {
                        expected_damage_taken += their_damage_potential;
                    }
                    // If this enemy is likely my target, use my damage to them as the
                    // maximum damage I can do
                    if (my_damage_potential >= maximum_damage_done) {
                        maximum_damage_done = my_damage_potential;
                    }
                } else { //for units that have moved this round
                    //I would prefer not to have the unit be able to move directly behind or flank me
                    EntityEvaluationResponse resp = evaluateUnmovedEnemy(e, p, game);
                    if (resp.damage_i_can_do > maximum_damage_done) {
                        maximum_damage_done = resp.damage_i_can_do;
                    }
                    expected_damage_taken += resp.damage_enemy_can_do;
                }
            }
            // Include damage I can do to strategic targets
            for (int i = 0; i < botbase.fire_control.additional_targets.size(); i++) {
                Targetable t = botbase.fire_control.additional_targets.get(i);
                if (t.getPosition() == null) {
                    continue; // Skip targets not actually on the board.
                }
                FiringPlan my_firing_plan = firecontrol.guessBestFiringPlanWithTwists(p.getEntity(),
                                                                                      new EntityState(p), t, null,
                                                                                      game);
                double my_damage_potential = my_firing_plan.utility;
                if (my_damage_potential > maximum_damage_done) {
                    maximum_damage_done = my_damage_potential;
                }
                FireControl.PhysicalInfo mykick = new FireControl.PhysicalInfo(
                        p.getEntity(), new EntityState(p), t, null,
                        PhysicalAttackType.RIGHT_KICK, game);
                double expected_kick_damage = mykick.expected_damage_on_hit
                                              * mykick.prob_to_hit;
                if (expected_kick_damage > maximum_physical_damage) {
                    maximum_physical_damage = expected_kick_damage;
                }
            }

            //If I cannot kick because I am a clan unit and "No physical attacks for the clans"
            //is enabled, set maximum physical damage for this path to zero.
            if (game.getOptions().booleanOption("no_clan_physical") && p.getEntity().isClan()) {
                maximum_physical_damage = 0;
            }

            // I can kick a different target than I shoot, so add physical to total
            // damage after I've looked at all enemies
            maximum_damage_done += maximum_physical_damage;

            double utility = (successProbability
                              * ((maximum_damage_done * foolish_bravery) - expected_damage_taken))
                             - expected_fall_damage;

            if (p.getEntity() instanceof Aero) {

            } else {
                //ground unit specific things
                double dist_to_enemy = distanceToClosestEnemy(p.getEntity(), p.getFinalCoords(), game);
                utility -= dist_to_enemy * hyper_aggression;

                //            double dist_to_friend = distanceToClosestFriend(p, game);
                //          utility -= dist_to_friend * herd_mentality;
            }

            //Should I be trying to withdraw?
            if (((p.getEntity().isCrippled()) && (botbase.forced_withdrawal))
                || (botbase.should_flee)) {
                int new_distance_to_edge = distanceToHomeEdge(p.getFinalCoords(), botbase.getHomeEdge(), game);
                int current_distance_to_edge = distanceToHomeEdge(p.getEntity().getPosition(), botbase.getHomeEdge(),
                                                                  game);
                int delta_distance_to_edge = current_distance_to_edge - new_distance_to_edge;

                if (delta_distance_to_edge > 0) {
                    //if it's small enough, the unit should do more of a fighting withdrawal
                    utility += self_preservation * delta_distance_to_edge;
                } else {
                    utility -= (self_preservation * 100);
                }
            }

            return utility;
        } finally {
            Logger.methodEnd(getClass(), METHOD_NAME);
        }

    }


    /**
     * Calculate who all other units would shoot at if I weren't around
     */
    @Override
    public void initUnitTurn(Entity unit, IGame game) {
        final String METHOD_NAME = "initUnitTurn(Entity, IGame)";
        Logger.methodBegin(getClass(), METHOD_NAME);

        try {
            best_damage_by_enemies.clear();
            ArrayList<Entity> enemies = getEnemies(unit, game);
            ArrayList<Entity> friends = getFriends(unit, game);
            for (Entity e : enemies) {
                double max_damage = 0;
                for (Entity f : friends) {
                    double damage = firecontrol
                            .guessBestFiringPlanUnderHeatWithTwists(e, null, f,
                                                                    null, (e.getHeatCapacity() - e.heat) + 5, game)
                            .getExpectedDamage();
                    if (damage > max_damage) {
                        max_damage = damage;
                    }

                }
                best_damage_by_enemies.put(e.getId(), max_damage);
            }
        } finally {
            Logger.methodEnd(getClass(), METHOD_NAME);
        }
    }

    /**
     * Gives the distance to the closest friendly unit, or zero if none exist
     */
    public double distanceToClosestFriend(MovePath p, IGame game) {
        final String METHOD_NAME = "distanceToClosestFriend(MovePath, IGame)";
        Logger.methodBegin(getClass(), METHOD_NAME);

        try {
            Entity closest = findClosestFriend(p, game);
            if (closest == null) {
                return 0;
            }
            return closest.getPosition().distance(p.getFinalCoords());
        } finally {
            Logger.methodEnd(getClass(), METHOD_NAME);
        }
    }

    /**
     * Gives the distance to the closest enemy unit, or zero if none exist
     *
     * @param me       Entity who has enemies
     * @param position Coords from which the closest enemy is found
     * @param game     IGame that we're playing
     */
    static public double distanceToClosestEnemy(Entity me, Coords position, IGame game) {
        final String METHOD_NAME = "distanceToClosestEnemy(Entity, Coords, IGame)";
        Logger.methodBegin(BasicPathRanker.class, METHOD_NAME);

        try {
            Entity closest = findClosestEnemy(me, position, game);
            if (closest == null) {
                return 0;
            }
            return closest.getPosition().distance(position);
        } finally {
            Logger.methodEnd(BasicPathRanker.class, METHOD_NAME);
        }
    }

    /**
     * Gives the distance to the closest edge
     */
    static public int distanceToClosestEdge(Coords position, IGame game) {
        final String METHOD_NAME = "distanceToClosestEdge(Coords, IGame)";
        Logger.methodBegin(BasicPathRanker.class, METHOD_NAME);

        try {
            int width = game.getBoard().getWidth();
            int height = game.getBoard().getHeight();
            int minimum = position.x;
            if ((width - position.x) < minimum) {
                minimum = position.x;
            }
            if (position.y < minimum) {
                minimum = position.y;
            }
            if ((height - position.y) < minimum) {
                minimum = height - position.y;
            }
            return minimum;
        } finally {
            Logger.methodEnd(BasicPathRanker.class, METHOD_NAME);
        }
    }


    /*
     * Returns distance to the unit's home edge.
     * Gives the distance to the closest edge
     *
     * @param position Final coordinates of the proposed move.
     * @param homeEdge Unit's home edge.
     * @param game
     * @return The distance to the unit's home edge.
     */
    /*
    public static int distanceToHomeEdge(Coords position, HomeEdge homeEdge, IGame game) {
        final String METHOD_NAME = "distanceToHomeEdge(Coords, HomeEdge, IGame)";
        owner.methodBegin(BasicPathRanker.class, METHOD_NAME);

        try {
            Coords edgeCoords;
            int boardHeight = game.getBoard().getHeight();
            int boardWidth = game.getBoard().getWidth();
            String msg = "Getting distance to home edge: ";
            if (HomeEdge.NORTH.equals(homeEdge)) {
                msg += "North";
                edgeCoords = new Coords(boardWidth/2, 0);
            } else if (HomeEdge.SOUTH.equals(homeEdge)) {
                msg += "South";
                edgeCoords = new Coords(boardWidth/2, boardHeight);
            } else if (HomeEdge.WEST.equals(homeEdge)) {
                msg += "West";
                edgeCoords = new Coords(0, boardHeight/2);
            } else if (HomeEdge.EAST.equals(homeEdge)) {
                msg += "East";
                edgeCoords = new Coords(boardWidth, boardHeight/2);
            } else {
                msg += "Default";
                owner.log(BasicPathRanker.class, METHOD_NAME, Princess.LogLevel.WARNING,
                "Invalid home edge.  Defaulting to NORTH.");
                edgeCoords = new Coords(boardWidth/2, 0);
            }

            owner.log(BasicPathRanker.class, METHOD_NAME, msg);
            return edgeCoords.distance(position);
        } finally {
            owner.methodEnd(BasicPathRanker.class, METHOD_NAME);
        }
    }
    */

    /**
     * Returns distance to the unit's home edge.
     * Gives the distance to the closest edge
     *
     * @param position Final coordinates of the proposed move.
     * @param homeEdge Unit's home edge.
     * @param game
     * @return The distance to the unit's home edge.
     */
    public static int distanceToHomeEdge(Coords position, HomeEdge homeEdge, IGame game) {
        final String METHOD_NAME = "distanceToHomeEdge(Coords, HomeEdge, IGame)";
        Logger.methodBegin(BasicPathRanker.class, METHOD_NAME);

        try {
            String msg = "Getting distance to home edge: " + homeEdge.toString();

            int width = game.getBoard().getWidth();
            int height = game.getBoard().getHeight();

            int distance = 9999;
            switch (homeEdge) {
                case NORTH: {
                    distance = position.y;
                    break;
                }
                case SOUTH: {
                    distance = height - position.y - 1;
                    break;
                }
                case WEST: {
                    distance = position.x;
                    break;
                }
                case EAST: {
                    distance = width - position.x - 1;
                    break;
                }
                default: {
                    Logger.log(BasicPathRanker.class, METHOD_NAME, LogLevel.WARNING,
                               "Invalid home edge.  Defaulting to NORTH.");
                    distance = position.y;
                }
            }

            msg += " -> " + distance;
            Logger.log(BasicPathRanker.class, METHOD_NAME, msg);
            return distance;
        } finally {
            Logger.methodEnd(BasicPathRanker.class, METHOD_NAME);
        }
    }
}
