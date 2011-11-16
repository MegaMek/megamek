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
import megamek.common.Compute;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.IGame;
import megamek.common.MovePath;
import megamek.common.MovePath.MoveStepType;
import megamek.common.TargetRoll;
import megamek.common.Targetable;

/**
 * A very basic pathranker
 */
public class BasicPathRanker extends PathRanker {

    FireControl firecontrol;
    PathEnumerator path_enumerator;
    
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
    double herd_mentality; // how much is it worth to me to stay near my buddies   
    
    TreeMap<Integer, Double> best_damage_by_enemies; // the best damage enemies
    // could expect were I not
    // here. Used to determine
    // whether they will target
    // me.

    public BasicPathRanker(Properties props) {
        // give some default values for tunable parameters
        fall_shame = 10.0;
//        blind_optimism = 0.9;
  //      enemy_underestimation = 0.5;
        foolish_bravery = 3.0;
        hyper_aggression = 0.05;
        herd_mentality = 0.01;

        fall_shame = Double.valueOf(props.getProperty("fall_shame"));
    //    blind_optimism = Double.valueOf(props.getProperty("blind_optimism"));
      //  enemy_underestimation = Double.valueOf(props
        //        .getProperty("enemy_underestimation"));
        foolish_bravery = Double.valueOf(props.getProperty("foolish_bravery"));
        hyper_aggression = Double
                .valueOf(props.getProperty("hyper_aggression"));
        herd_mentality = Double.valueOf(props.getProperty("herd_mentality"));

        best_damage_by_enemies = new TreeMap<Integer, Double>();
    }            
    
    class EntityEvaluationResponse {
        public EntityEvaluationResponse() {
            damage_enemy_can_do=0;
            damage_i_can_do=0;
        }
        
        public double damage_enemy_can_do;
        public double damage_i_can_do;
    };
    
    /**
     * Guesses a number of things about an enemy that has not yet moved
     * TODO estimated damage is sloppy.  Improve for missile attacks, gun skill, and range
     */
    public EntityEvaluationResponse evaluateUnmovedEnemy(Entity e,MovePath p,IGame game) {        
        //some preliminary calculations
        double damage_discount=0.25;
        EntityEvaluationResponse ret=new EntityEvaluationResponse();
        //Aeros always move after other units, and would require an entirely different evaluation
        //TODO (low priority) implement a way to see if I can dodge aero units
        if(e instanceof Aero) return ret;
        Coords mycoords=p.getFinalCoords();
        int myfacing=p.getFinalFacing();
        Coords behind=mycoords.translated((myfacing+3)%6);
        Coords leftflank=mycoords.translated((myfacing+2)%6);
        Coords rightflank=mycoords.translated((myfacing+4)%6);
        HashSet<CoordFacingCombo> enemy_facing_set=path_enumerator.unit_potential_locations.get(e.getId());
        Coords closest=path_enumerator.unit_movable_areas.get(e.getId()).getClosestCoordsTo(mycoords);
        int range=closest.distance(mycoords);
        //I would prefer if the enemy must end its move in my line of fire
        //if so, I can guess that I may do some damage to it
        //(cover nonwithstanding.  At the very least, I can force the enemy to take
        // cover on its move)
        HexLine leftbounds;
        HexLine rightbounds;
        if(p.getEntity().canChangeSecondaryFacing()) {
            leftbounds=new HexLine(behind,(myfacing+2)%6);
            rightbounds=new HexLine(behind,(myfacing+4)%6);
        } else { 
            leftbounds=new HexLine(behind,(myfacing+1)%6);
            rightbounds=new HexLine(behind,(myfacing+5)%6);
        }                
        if((leftbounds.judgeArea(path_enumerator.unit_movable_areas.get(e.getId()))>0) &&
           (rightbounds.judgeArea(path_enumerator.unit_movable_areas.get(e.getId()))<0)) {                                  
            ret.damage_i_can_do+=firecontrol.getMaxDamageAtRange(p.getEntity(), range)*damage_discount;        
        }
        //in general if an enemy can end its position in range, it can hit me
        ret.damage_enemy_can_do+=firecontrol.getMaxDamageAtRange(e,range)*damage_discount;
        //It is especially embarassing if the enemy can move behind or flank me and then kick me        
        if(enemy_facing_set!=null) {
        if(enemy_facing_set.contains(new CoordFacingCombo(behind,myfacing))||
           enemy_facing_set.contains(new CoordFacingCombo(behind,(myfacing+1)%6))||
           enemy_facing_set.contains(new CoordFacingCombo(behind,(myfacing+5)%6))||
           enemy_facing_set.contains(new CoordFacingCombo(leftflank,myfacing))||
           enemy_facing_set.contains(new CoordFacingCombo(leftflank,(myfacing+4)%6))||
           enemy_facing_set.contains(new CoordFacingCombo(leftflank,(myfacing+5)%6))||
           enemy_facing_set.contains(new CoordFacingCombo(rightflank,myfacing))||
           enemy_facing_set.contains(new CoordFacingCombo(rightflank,(myfacing+1)%6))||
           enemy_facing_set.contains(new CoordFacingCombo(rightflank,(myfacing+2)%6)))
            ret.damage_enemy_can_do+=Math.ceil(e.getWeight() / 5.0)*damage_discount;            
        } else
            System.err.println("warning, no facing set for "+e.getDisplayName());           
        return ret;
    }
    
    /**
     * A path ranking
     */
    @Override
    public double rankPath(MovePath p, IGame game) {
        boolean isaero=(p.getEntity() instanceof Aero);
        if(isaero) {
            //stalling is bad
            if(p.getFinalVelocity()==0) {
                return -1000;
            }
            //you know what else is bad?  this
            if(p.getFinalAltitude()<1) {
                return -10000;
            }
            //flying off board should only be done if necessary, but is better than taking much damage
            if((p.getLastStep()!=null)&&(p.getLastStep().getType()==MoveStepType.RETURN)) {
                return -5;
            }
        }

        // How likely is it that I will succeed in reaching this path
        MovePath pcopy=p.clone(); //get psr calls cliptopossible, I may not want to change p
        List<TargetRoll> targets = SharedUtility.getPSRList(pcopy);
        double success_probability = 1.0;
        for (TargetRoll t : targets) {
            success_probability *= Compute.oddsAbove(t.getValue()) / 100.0;
        }
        // Lets assume that I will fall if I fail. What's my expected damage
        // (and embarrassment) from that?
        int fall_damage = (int) (p.getEntity().getWeight() / 10);
        double expected_fall_damage = (fall_damage + fall_shame)
                * (1.0 - success_probability);

        // look at all of my enemies
        ArrayList<Entity> enemies = getEnemies(p.getEntity(), game);
        double maximum_damage_done = 0;
        double maximum_physical_damage = 0;
        double expected_damage_taken = 0;
        for (Entity e : enemies) {
            if((!e.isSelectableThisTurn())||e.isImmobile()) { //For units that have already moved
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
            FiringPlan my_firing_plan=null;
            if(p.getEntity() instanceof Aero) {
                my_firing_plan = firecontrol.guessFullAirToGroundPlan(p.getEntity(),e,new EntityState(e),p,game,false);
            } else {
                my_firing_plan = firecontrol.guessBestFiringPlanWithTwists(p.getEntity(),new EntityState(p),e,null,game);
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
                EntityEvaluationResponse resp=evaluateUnmovedEnemy(e,p,game);
                if(resp.damage_i_can_do>maximum_damage_done)
                    maximum_damage_done=resp.damage_i_can_do;
                expected_damage_taken+=resp.damage_enemy_can_do;
            }
        }
        // Include damage I can do to strategic targets
        for(int i=0;i<botbase.fire_control.additional_targets.size();i++) {
            Targetable t=botbase.fire_control.additional_targets.get(i);
            FiringPlan my_firing_plan = firecontrol.guessBestFiringPlanWithTwists(p.getEntity(),new EntityState(p),t,null,game);
            double my_damage_potential = my_firing_plan.utility;
            if(my_damage_potential>maximum_damage_done)
                maximum_damage_done = my_damage_potential;
            FireControl.PhysicalInfo mykick = new FireControl.PhysicalInfo(
                    p.getEntity(), new EntityState(p), t, null,
                    PhysicalAttackType.RIGHT_KICK, game);
            double expected_kick_damage = mykick.expected_damage_on_hit
            * mykick.prob_to_hit;
            if (expected_kick_damage > maximum_physical_damage) {
                maximum_physical_damage = expected_kick_damage;
            }
        }
        
        // I can kick a different target than I shoot, so add physical to total
        // damage after I've looked at all enemies
        maximum_damage_done += maximum_physical_damage;

        double utility = (success_probability
                * ((maximum_damage_done * foolish_bravery) - expected_damage_taken))
                - expected_fall_damage;

        if(p.getEntity() instanceof Aero) {

        } else {
            //ground unit specific things
            double dist_to_enemy = distanceToClosestEnemy(p.getEntity(),p.getFinalCoords(), game);
            utility -= dist_to_enemy * hyper_aggression;

            double dist_to_friend = distanceToClosestFriend(p, game);
            utility -= dist_to_friend * herd_mentality;
        }
        
        //Should I be trying to withdraw?
        if(((p.getEntity().isCrippled())&&(botbase.forced_withdrawal))||(botbase.should_flee)) {            
            int distance_to_edge=distanceToClosestEdge(p.getFinalCoords(),game);
            //TODO this number should not be hard coded, but instead be modifiable, 
            //if it's small enough, the unit should do more of a fighting withdrawal
            utility-=30*distance_to_edge;
        }

        return utility;

    };


    /**
     * Calculate who all other units would shoot at if I weren't around
     */
    @Override
    public void initUnitTurn(Entity unit, IGame game) {
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
    }

    /**
     * Gives the distance to the closest friendly unit, or zero if none exist
     */
    public double distanceToClosestFriend(MovePath p, IGame game) {
        Entity closest = findClosestFriend(p, game);
        if (closest == null) {
            return 0;
        }
        return closest.getPosition().distance(p.getFinalCoords());

    }

    /**
     * Gives the distance to the closest enemy unit, or zero if none exist
     *
     * @param me Entity who has enemies
     * @param position Coords from which the closest enemy is found
     * @param game IGame that we're playing
     */
    static public double distanceToClosestEnemy(Entity me,Coords position, IGame game) {
        Entity closest = findClosestEnemy(me,position, game);
        if (closest == null) {
            return 0;
        }
        return closest.getPosition().distance(position);
    }
    
    /**
     * Gives the distance to the closest edge
     * 
     */
    static public int distanceToClosestEdge(Coords position, IGame game) {
        int width=game.getBoard().getWidth();
        int height=game.getBoard().getHeight();
        int minimum=position.x;
        if(width-position.x<minimum) minimum=width=position.x;
        if(position.y<minimum) minimum=position.y;
        if(height-position.y<minimum) minimum=height-position.y;
        return minimum;
    }
}
