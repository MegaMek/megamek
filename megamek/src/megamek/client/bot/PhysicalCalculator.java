/*
 * MegaMek - Copyright (C) 2003,2004,2005 Ben Mazur (bmazur@sev.org)
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

package megamek.client.bot;

import java.util.Enumeration;
import megamek.common.BattleArmor;
import megamek.common.Compute;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.IGame;
import megamek.common.IHex;
import megamek.common.INarcPod;
import megamek.common.Infantry;
import megamek.common.Mech;
import megamek.common.Protomech;
import megamek.common.Tank;
import megamek.common.Terrains;
import megamek.common.ToHitData;
import megamek.common.actions.BrushOffAttackAction;
import megamek.common.actions.ClubAttackAction;
import megamek.common.actions.KickAttackAction;
import megamek.common.actions.PunchAttackAction;
import megamek.common.actions.PushAttackAction;

public class PhysicalCalculator {

    PhysicalOption calculatePhysicalTurn(TestBot bot) {
        int entNum = bot.game.getFirstEntityNum();
        int first = entNum;
        do {
            // take the first entity that can do an attack
            Entity en = bot.game.getEntity(entNum);
            PhysicalOption bestAttack = getBestPhysical(en, bot.game);
            
            if (bestAttack != null) {
                
                return bestAttack;

            } // End no-attack
            entNum = bot.game.getNextEntityNum(entNum);

        }
        while (entNum != -1 && entNum != first);

        // Didn't find any physical attack.
        return null;
    }

    PhysicalOption getBestPhysical(Entity entity, IGame game) {
        // Infantry can't conduct physical attacks.
        if (entity instanceof Infantry) {
            return null;
        }

        // if you're charging, it's already declared
        if (entity.isCharging() || entity.isMakingDfa()) {
            return null;
        }

        PhysicalOption best = null;
        ToHitData odds;
        double breach, breach_a, l_dmg, r_dmg, final_dmg;
        int best_brush = PhysicalOption.NONE;

        // If the attacker is a Mech
        
        if (entity instanceof Mech){
            
            l_dmg = 0.0;
            r_dmg = 0.0;
            final_dmg = 0.0;
            breach_a = 0.0;
            
            // If the attacker is being swarmed
            
            if (entity.getSwarmAttackerId() != Entity.NONE){
        
                // Check for left arm punch damage to self
                
                odds = BrushOffAttackAction.toHit(game, entity.getId(),
                        game.getEntity(entity.getSwarmAttackerId()), BrushOffAttackAction.LEFT);
                if (odds.getValue() != ToHitData.IMPOSSIBLE) {
                    
                    l_dmg = BrushOffAttackAction.getDamageFor(entity, BrushOffAttackAction.LEFT);
                    l_dmg *= (1 - Compute.oddsAbove(odds.getValue()) / 100.0);
                    breach = punchThroughMod(entity, game, ToHitData.HIT_PUNCH, ToHitData.SIDE_FRONT,
                            l_dmg, l_dmg);
                    if (breach < 1.5){
                        best_brush = PhysicalOption.BRUSH_LEFT;
                        breach_a = breach;
                        final_dmg = BrushOffAttackAction.getDamageFor(entity, BrushOffAttackAction.LEFT);
                    }
                }
        
                // Check for right arm punch damage to self
                odds = BrushOffAttackAction.toHit(game, entity.getId(),
                        game.getEntity(entity.getSwarmAttackerId()), BrushOffAttackAction.RIGHT);
                if (odds.getValue() != ToHitData.IMPOSSIBLE) {
        
                // If chance of breaching armor is minimal set brush left
                    
                    r_dmg = BrushOffAttackAction.getDamageFor(entity, BrushOffAttackAction.RIGHT);
                    r_dmg *= (1 - Compute.oddsAbove(odds.getValue()) / 100.0);
                    breach = punchThroughMod(entity, game, ToHitData.HIT_PUNCH, ToHitData.SIDE_FRONT,
                            r_dmg, r_dmg);
                    if (breach < Math.min(breach_a, 1.5)){
                        best_brush = PhysicalOption.BRUSH_RIGHT;
                        breach_a = breach;
                        final_dmg = BrushOffAttackAction.getDamageFor(entity, BrushOffAttackAction.RIGHT);
                    }
                }
        
                // If both arms are capable of punching, check double punch damage
                
                if (l_dmg > 0 && r_dmg > 0){
        
                    // If chance of breaching armor is minimal set double brush
                    
                    breach = punchThroughMod(entity, game, ToHitData.HIT_PUNCH, ToHitData.SIDE_FRONT,
                            l_dmg + r_dmg, (l_dmg + r_dmg)/2);
                    if (breach < Math.min(breach_a, 1.5)){
                        best_brush = PhysicalOption.BRUSH_BOTH;
                        breach_a = breach;
                        final_dmg = BrushOffAttackAction.getDamageFor(entity, BrushOffAttackAction.LEFT);
                        final_dmg += BrushOffAttackAction.getDamageFor(entity, BrushOffAttackAction.RIGHT);
                    }
                }
        
                // Construct and return Physical option
                if (best_brush != PhysicalOption.NONE){
                    return new PhysicalOption(entity, game.getEntity(entity.getSwarmAttackerId()), 
                            final_dmg, best_brush);
                }
            }
        
            // If the attacker has attached iNarc pods, assign
            // a competing damage value for comparison with other
            // attacks

            if (entity.hasINarcPodsAttached()){
                double test_ranking, pod_ranking;
                INarcPod test_pod, best_pod;
                pod_ranking = 0.0;
                Enumeration pod_list = entity.getINarcPodsAttached();
                best_pod = (INarcPod) pod_list.nextElement();
                for (pod_list = entity.getINarcPodsAttached(); pod_list.hasMoreElements();){
                    test_ranking = 1.0;
                    test_pod = (INarcPod) pod_list.nextElement();
                    // If pod is homing and attacker has no ECM
                    if ((test_pod.getType() == INarcPod.HOMING) && !entity.hasActiveECM()){
                        // Pod is +1
                        test_ranking += 1.0;
                    }
                    // If pod is ECM and attacker has C3 link
                    if ((test_pod.getType() == INarcPod.ECM) && ((entity.hasC3()) || 
                            (entity.hasC3i()))){
                        // Pod is +2
                        test_ranking += 2.0;
                    }
                    // If pod is Nemesis
                    if (test_pod.getType() == INarcPod.NEMESIS){
                        // Pod is +variable, based on movement
                        test_ranking += (double) (entity.getWalkMP() + entity.getJumpMP())/2;
                    }
                    // If this pod is best, retain it and its ranking
                    if (test_ranking > pod_ranking){
                        pod_ranking = test_ranking;
                        best_pod = test_pod;
                    }
                }
                if (best_pod != null){
                    // Check for left arm punch damage to self
                    odds = BrushOffAttackAction.toHit(game, entity.getId(),
                            best_pod, BrushOffAttackAction.LEFT);
                    if (odds.getValue() != ToHitData.IMPOSSIBLE) {
                        
                        l_dmg = BrushOffAttackAction.getDamageFor(entity, 
                                BrushOffAttackAction.LEFT);
                        l_dmg *= (1 - Compute.oddsAbove(odds.getValue()) / 100.0);
                        breach = punchThroughMod(entity, game, ToHitData.HIT_PUNCH, 
                                ToHitData.SIDE_FRONT, l_dmg, l_dmg);
                        if (breach < 1.5){
                            best_brush = PhysicalOption.BRUSH_LEFT;
                            breach_a = breach;
                            final_dmg = BrushOffAttackAction.getDamageFor(entity, 
                                    BrushOffAttackAction.LEFT);
                        }
                    }
            
                    // Check for right arm punch damage to self
                    odds = BrushOffAttackAction.toHit(game, entity.getId(),
                            best_pod, BrushOffAttackAction.RIGHT);
                    if (odds.getValue() != ToHitData.IMPOSSIBLE) {
            
                    // If chance of breaching armor is minimal set brush left
                        
                        r_dmg = BrushOffAttackAction.getDamageFor(entity, BrushOffAttackAction.RIGHT);
                        r_dmg *= (1 - Compute.oddsAbove(odds.getValue()) / 100.0);
                        breach = punchThroughMod(entity, game, ToHitData.HIT_PUNCH, ToHitData.SIDE_FRONT,
                                r_dmg, r_dmg);
                        if (breach < Math.min(breach_a, 1.5)){
                            best_brush = PhysicalOption.BRUSH_RIGHT;
                            breach_a = breach;
                            final_dmg = BrushOffAttackAction.getDamageFor(entity, BrushOffAttackAction.RIGHT);
                        }
                    }
            
                    // If both arms are capable of punching, check double punch damage
                    
                    if (l_dmg > 0 && r_dmg > 0){
            
                        // If chance of breaching armor is minimal set double brush
                        
                        breach = punchThroughMod(entity, game, ToHitData.HIT_PUNCH, ToHitData.SIDE_FRONT,
                                l_dmg + r_dmg, (l_dmg + r_dmg)/2);
                        if (breach < Math.min(breach_a, 1.5)){
                            best_brush = PhysicalOption.BRUSH_BOTH;
                            breach_a = breach;
                            final_dmg = BrushOffAttackAction.getDamageFor(entity, BrushOffAttackAction.LEFT);
                            final_dmg += BrushOffAttackAction.getDamageFor(entity, BrushOffAttackAction.RIGHT);
                        }
                    }
            
                    // Construct and return Physical option
                    if(best_brush != PhysicalOption.NONE){
                        return new PhysicalOption(entity, best_pod, final_dmg, best_brush);
                    }
                }
            }
        }
        
        for (Enumeration e = game.getEntities(); e.hasMoreElements();) {
            Entity target = (Entity) e.nextElement();

            if (target.equals(entity))
                continue;
            if (!target.isEnemyOf(entity))
                continue;
            if (null == target.getPosition())
                continue;
            if (Compute.effectiveDistance(game, entity, target) > 1)
                continue;
            
            PhysicalOption one = getBestPhysicalAttack(entity, target, game);
            if (one != null) {
                if (best == null || one.expectedDmg > best.expectedDmg) {
                    best = one;
                }
            }
        }
        if(best == null) best = new PhysicalOption(entity);   
        return best;
    }

    PhysicalOption getBestPhysicalAttack(Entity from, Entity to, IGame game) {
        double bestDmg = 0, dmg, coll_damage, self_damage;
        int damage;
        int target_arc, location_table;
        int bestType = PhysicalOption.PUNCH_LEFT;

        // Infantry and tanks can't conduct any of these attacks
        if ((from instanceof Infantry) || (from instanceof Tank)){
            return null;
        }
        
        // Find arc the attack comes in
        target_arc = CEntity.getThreatHitArc(to.getPosition(),
                to.getFacing(), from.getPosition());
        
        // Check for punches
        // If the target is a Mech, must determine if punch lands on the punch, kick, or full table
        if (to instanceof Mech){
            if (!to.isProne()){
                location_table = ToHitData.HIT_PUNCH;
                if (to.getElevation() == from.getElevation()+1){
                    location_table = ToHitData.HIT_KICK;
                }
            } else {
                location_table = ToHitData.HIT_NORMAL;
            }
        } else {
            location_table = ToHitData.HIT_NORMAL;
        }
        
        ToHitData odds = PunchAttackAction.toHit(game, from.getId(), to, PunchAttackAction.LEFT);
        if (odds.getValue() != ToHitData.IMPOSSIBLE) {
            damage = PunchAttackAction.getDamageFor(from, PunchAttackAction.LEFT);
            bestDmg = Compute.oddsAbove(odds.getValue()) / 100.0 * damage;
            // Adjust damage for targets armor
            bestDmg *= punchThroughMod(to, game, location_table, target_arc, bestDmg, bestDmg);
        }

        odds = PunchAttackAction.toHit(game, from.getId(), to, PunchAttackAction.RIGHT);
        if (odds.getValue() != ToHitData.IMPOSSIBLE) {
            damage = PunchAttackAction.getDamageFor(from, PunchAttackAction.RIGHT);
            dmg = Compute.oddsAbove(odds.getValue()) / 100.0 * damage;
            // Adjust damage for targets armor
            dmg *= punchThroughMod(to, game, location_table, target_arc, dmg, dmg);
            if (dmg > bestDmg){
                bestType = PhysicalOption.PUNCH_RIGHT;
                bestDmg = dmg;
            }
        }
        
        // Check for a double punch
        odds = PunchAttackAction.toHit(game, from.getId(), to, PunchAttackAction.LEFT);
        ToHitData odds_a = PunchAttackAction.toHit(game, from.getId(), to, PunchAttackAction.RIGHT);
        if ((odds.getValue() != ToHitData.IMPOSSIBLE) && (odds_a.getValue() != ToHitData.IMPOSSIBLE)){
            damage = PunchAttackAction.getDamageFor(from, PunchAttackAction.LEFT);
            dmg = Compute.oddsAbove(odds.getValue()) / 100.0 * damage;
            double dmg_a = Compute.oddsAbove(odds_a.getValue()) / 100.0 * damage;
            dmg += dmg_a;
            dmg *= punchThroughMod(to, game, location_table, target_arc, dmg, dmg/2);
            if (dmg > bestDmg){
                bestType = PhysicalOption.PUNCH_BOTH;
                bestDmg = dmg;
            }
        }

        // Check for a kick
        // If the target is a Mech, must determine if it lands on the kick or punch table
        if (to instanceof Mech){
            location_table = ToHitData.HIT_KICK;
            if (!to.isProne()){
                if (to.getElevation() == from.getElevation()-1){
                    location_table = ToHitData.HIT_PUNCH;
                }
            } else {
                location_table = ToHitData.HIT_NORMAL;
            }
        } else {
            location_table = ToHitData.HIT_NORMAL;
        }

        // Calculate collateral damage, due to possible target fall
        // = chance of successful kick * chance of target falling * target falling damage
        coll_damage = 0;
        if (to instanceof Mech){
            coll_damage = (Compute.oddsAbove(odds.getValue())/100.0) * 
                (Compute.oddsAbove((1-to.getBasePilotingRoll().getValue()))/100.0) * 
                to.getWeight() * 0.1;
        }
        odds = KickAttackAction.toHit(game, from.getId(), to, KickAttackAction.LEFT);
        if (odds.getValue() != ToHitData.IMPOSSIBLE) {
            self_damage = 0;
            damage = KickAttackAction.getDamageFor(from, KickAttackAction.LEFT);
            dmg = Compute.oddsAbove(odds.getValue()) / 100.0 * damage;
            // Adjust damage for targets armor
            dmg *= punchThroughMod(to, game, location_table, target_arc, dmg, dmg);
            // Calculate self damage, due to possible fall from missing a kick
            // Odds of missing the kick, odds of falling, falling damage, and
            // a factor to account for how bad it is for this unit to be prone
            self_damage = 1 - Compute.oddsAbove(odds.getValue()) / 100.0;
            self_damage = self_damage * (1 - Compute.oddsAbove(from.getBasePilotingRoll().getValue())/100.0);
            self_damage = self_damage * from.getWeight() * 0.1;
            if (from.getWalkMP() > 0){
                self_damage = self_damage * Math.sqrt((1/(double)from.getWalkMP()) + from.getJumpMP());
            } else {
                self_damage = self_damage * Math.sqrt(from.getJumpMP());
            }
            // Add together damage values for comparison
            dmg = dmg + coll_damage - self_damage;
            if (dmg > bestDmg) {
                bestType = PhysicalOption.KICK_LEFT;
                bestDmg = dmg;
            }
        }
        odds = KickAttackAction.toHit(game, from.getId(), to, KickAttackAction.RIGHT);
        if (odds.getValue() != ToHitData.IMPOSSIBLE) {
            self_damage = 0;
            damage = KickAttackAction.getDamageFor(from, KickAttackAction.RIGHT);
            dmg = Compute.oddsAbove(odds.getValue()) / 100.0 * damage;
            // Adjust damage for targets armor
            dmg *= punchThroughMod(to, game, location_table, target_arc, dmg, dmg);
            // Calculate self damage, due to possible fall from missing a kick
            // Odds of missing the kick, odds of falling, falling damage, and
            // a factor to account for how bad it is for this unit to be prone
            self_damage = 1 - (Compute.oddsAbove(odds.getValue()) / 100.0);
            self_damage = self_damage * (1 - Compute.oddsAbove(from.getBasePilotingRoll().getValue())/100.0);
            self_damage = self_damage * from.getWeight() * 0.1;
            if (from.getWalkMP() > 0){
                self_damage = self_damage * Math.sqrt((1/(double)from.getWalkMP()) + from.getJumpMP());
            } else {
                self_damage = self_damage * Math.sqrt(from.getJumpMP());
            }
            // Add together damage values for comparison
            dmg = dmg + coll_damage - self_damage;
            if (dmg > bestDmg) {
                bestType = PhysicalOption.KICK_RIGHT;
                bestDmg = dmg;
            }
        }
        
        // Check for mounted club-type weapon or carried improvised club
        if (Compute.clubMechHas(from) != null) {
            
        // If the target is a Mech, must determine if it hits full body, punch, or kick table
            if (to instanceof Mech){
                location_table = ToHitData.HIT_NORMAL;
                if ((to.getElevation() == from.getElevation()-1) && !(to.isProne())){
                    location_table = ToHitData.HIT_PUNCH;
                }
                if ((to.getElevation() == from.getElevation()+1) && !(to.isProne())){
                    location_table = ToHitData.HIT_KICK;
                }
            } else {
                location_table = ToHitData.HIT_NORMAL;
            }
            odds = ClubAttackAction.toHit(game, from.getId(), to, Compute.clubMechHas(from));
            if (odds.getValue() != ToHitData.IMPOSSIBLE) {
                damage = ClubAttackAction.getDamageFor(from, Compute.clubMechHas(from));
                dmg = Compute.oddsAbove(odds.getValue()) / 100.0 * damage;
                // Adjust damage for targets armor
                dmg *= punchThroughMod(to, game, location_table, target_arc, dmg, dmg);
                // Some types of clubs, such as the mace, require a piloting check on a missed attack
                // Calculate self damage in the same manner as a missed kick
                if (dmg > bestDmg) {
                    bestType = PhysicalOption.USE_CLUB;
                    bestDmg = dmg;
                }
            }
        }
        // Check for a push attack
        odds = PushAttackAction.toHit(game, from.getId(), to);
        if (odds.getValue() != ToHitData.IMPOSSIBLE) {
            int elev_diff = 1;
            double breach = 1;
            boolean water_landing = false;
            dmg = 0;
            int disp_dir = from.getPosition().direction(to.getPosition());
            Coords disp_c = to.getPosition().translated(disp_dir);
            // If the displacement hex is a valid one
            if (Compute.isValidDisplacement(game, to.getId(), to.getPosition(), disp_c)){
                // If the displacement hex is not on the map, credit damage
                // against full target armor
                if (!game.getBoard().contains(disp_c)){
                    dmg = to.getTotalArmor() * Compute.oddsAbove(odds.getValue()) / 100.0;
                }
                if (game.getBoard().contains(disp_c)){
                // Find the elevation difference
                    elev_diff = game.getBoard().getHex(to.getPosition()).getElevation();
                    elev_diff -= game.getBoard().getHex(disp_c).getElevation();
                    if (elev_diff < 0){
                        elev_diff = 0;
                    }
                // Set a flag if the displacement hex has water
                    if (game.getBoard().getHex(disp_c).containsTerrain(Terrains.WATER)){
                        water_landing = true;
                    }
                // Get the base damage from target falling -> chance of hitting,
                // chance of target falling, falling damage
                    dmg = Compute.oddsAbove(odds.getValue()) / 100.0;
                    dmg = dmg * (1 - Compute.oddsAbove(elev_diff + to.getBasePilotingRoll().getValue())/100.0);
                    dmg = dmg * to.getWeight() * 0.1 * (1 + elev_diff);
                // Calculate breach factor of falling damage
                    breach = punchThroughMod(to, game, ToHitData.HIT_NORMAL, ToHitData.SIDE_FRONT, dmg, Math.min(dmg, 5));
                // If breach factor is > 1 and displacement hex has water
                    if ((breach > 1) && water_landing){
                        breach *= 2;
                    }
                // Modify damage to reflect how bad it is for target to be prone
                    if (to.getWalkMP() > 0){
                        dmg = dmg * Math.sqrt((1/(double)to.getWalkMP()) + to.getJumpMP());
                    } else {
                        dmg = dmg * Math.max(1, Math.sqrt(to.getJumpMP()));
                    }
                // Modify damage by breach factor
                    dmg *= breach;
                }
            }
            // If the displacement hex is not valid
            if (!Compute.isValidDisplacement(game, to.getId(), to.getPosition(), disp_c)){
                // Set a flag if the displacement hex has water
                if (game.getBoard().getHex(to.getPosition()).containsTerrain(Terrains.WATER)){
                    water_landing = true;
                }
                // Get the base damage from target falling -> chance of hitting,
                // chance of target falling, falling damage
                dmg = Compute.oddsAbove(odds.getValue()) / 100.0;
                dmg = dmg * (1 - Compute.oddsAbove(to.getBasePilotingRoll().getValue())/100.0);
                dmg = dmg * to.getWeight() * 0.1;
                // Calculate breach factor of falling damage
                breach = punchThroughMod(to, game, ToHitData.HIT_NORMAL, ToHitData.SIDE_FRONT, dmg, Math.min(dmg, 5));
                // If breach factor is > 1 and target hex is in water
                if ((breach > 1) && water_landing){
                    breach *= 2;
                }
                // Modify damage to reflect how bad it is for target to be prone
                if (to.getWalkMP() > 0){
                    dmg = dmg * Math.sqrt((1/to.getWalkMP()) + to.getJumpMP());
                } else {
                    dmg = dmg * Math.max(1, Math.sqrt(to.getJumpMP()));
                }
                // Modify damage by breach factor
                dmg *= breach;
            }
            // If damage is better than best damage
            if (dmg > bestDmg) {
                bestType = PhysicalOption.PUSH_ATTACK;
                bestDmg = dmg;
            }
        }
        
        // Conventional infantry in the open suffer double damage.
        if ((to instanceof Infantry) && !(to instanceof BattleArmor)) {
            IHex e_hex = game.getBoard().getHex(to.getPosition());
            if (!e_hex.containsTerrain(Terrains.WOODS) && !e_hex.containsTerrain(Terrains.BUILDING)) {
                bestDmg *= 2;
            }
        }

        if (bestDmg > 0) {
            return new PhysicalOption(from, to, bestDmg, bestType);
        }
        return null;
    }
    
    /* 
     * This checks to see if the damage will punch through armor anywhere in the
     * attacked arc.
     * damage argument is divided into hits, using the group argument (ie, group = 5.0 for LRM).
     * Each hit of group damage is checked against each location; if it penetrates increase the
     * multiplier to reflect potential for additional damage
     * Multiple passes are made with each hit being multiples of group damage to reflect shot
     * grouping; as each pass is made the increase to the multiplier is lowered due to the lower
     * chance of hitting the same location
     */
    
    public static double punchThroughMod (Entity target, IGame g, int hit_table, int hit_side,
            double damage, double group){
        
        int armor_values[] = new int[8];
        int max_index = 1;
        armor_values[0] = 0;
        
        // Set the final multiplier as 1.0 (no chance of penetrating armor)
        double final_multiplier = 1.0;
        
        // Set the base multiplier as 0.5 (good bonus for penetrating with a single hit)
        double base_multiplier = 0.5;
        
        // If the target is a Mech
        if (target instanceof Mech){
            // Create vector of body locations with targets current armor values
            // Use hit table and direction to determine locations that are hit
            if (hit_table == ToHitData.HIT_NORMAL){
                max_index = 7;
                armor_values[0] = target.getArmor(Mech.LOC_HEAD,false);
                if (hit_side != ToHitData.SIDE_FRONT){
                armor_values[1] = target.getArmor(Mech.LOC_CT,true);
                } else {
                    armor_values[1] = target.getArmor(Mech.LOC_CT,false);
                }
                if (hit_side != ToHitData.SIDE_FRONT){
                    armor_values[2] = target.getArmor(Mech.LOC_RT,true);
                } else {
                    armor_values[2] = target.getArmor(Mech.LOC_RT,false);
                }
                if (hit_side != ToHitData.SIDE_FRONT){
                    armor_values[3] = target.getArmor(Mech.LOC_LT,true);
                } else {
                    armor_values[3] = target.getArmor(Mech.LOC_LT,false);
                }
                armor_values[4] = target.getArmor(Mech.LOC_RARM,false);
                armor_values[5] = target.getArmor(Mech.LOC_LARM,false);
                armor_values[6] = target.getArmor(Mech.LOC_RLEG,false);
                armor_values[7] = target.getArmor(Mech.LOC_RLEG,false);
            }
            if (hit_table == ToHitData.HIT_PUNCH){
                armor_values[0] = target.getArmor(Mech.LOC_HEAD,false);
                if (hit_side == ToHitData.SIDE_RIGHT){
                    max_index = 3;
                    armor_values[1] = target.getArmor(Mech.LOC_CT,false);
                    armor_values[2] = target.getArmor(Mech.LOC_RT,false);
                    armor_values[3] = target.getArmor(Mech.LOC_RARM,false);
                }
                if (hit_side == ToHitData.SIDE_LEFT){
                    max_index = 3;
                    armor_values[1] = target.getArmor(Mech.LOC_CT,false);
                    armor_values[2] = target.getArmor(Mech.LOC_LT,false);
                    armor_values[3] = target.getArmor(Mech.LOC_LARM,false);
                }
                if (hit_side == ToHitData.SIDE_FRONT){
                    max_index = 5;
                    armor_values[1] = target.getArmor(Mech.LOC_CT,false);
                    armor_values[2] = target.getArmor(Mech.LOC_RT,false);
                    armor_values[3] = target.getArmor(Mech.LOC_LT,false);
                    armor_values[4] = target.getArmor(Mech.LOC_RARM,false);
                    armor_values[5] = target.getArmor(Mech.LOC_LARM,false);
                }
                if (hit_side == ToHitData.SIDE_REAR){
                    max_index = 5;
                    armor_values[1] = target.getArmor(Mech.LOC_CT,true);
                    armor_values[2] = target.getArmor(Mech.LOC_RT,true);
                    armor_values[3] = target.getArmor(Mech.LOC_LT,true);
                    armor_values[4] = target.getArmor(Mech.LOC_RARM,false);
                    armor_values[5] = target.getArmor(Mech.LOC_LARM,false);
                }
            }
            if (hit_table == ToHitData.HIT_KICK){
                max_index = -1;
                if ((hit_side == ToHitData.SIDE_FRONT) || (hit_side == ToHitData.SIDE_REAR) ||
                        (hit_side == ToHitData.SIDE_RIGHT)){
                    max_index++;
                    armor_values[max_index] = target.getArmor(Mech.LOC_RLEG,false);
                }
                if ((hit_side == ToHitData.SIDE_FRONT) || (hit_side == ToHitData.SIDE_REAR) ||
                        (hit_side == ToHitData.SIDE_LEFT)){
                    max_index++;
                    armor_values[max_index] = target.getArmor(Mech.LOC_LLEG,false);
                }
            }
        }
        // If the target is a ProtoMech
        if (target instanceof Protomech){
            max_index = 6;
            // Create vector of body locations with targets current armor values
            // Create two high-armor dummy locations to represent the 'near miss' hit locations
            armor_values[0] = target.getArmor(Protomech.LOC_TORSO, false);
            armor_values[1] = target.getArmor(Protomech.LOC_LEG, false);
            armor_values[2] = target.getArmor(Protomech.LOC_RARM, false);
            armor_values[3] = target.getArmor(Protomech.LOC_LARM, false);
            armor_values[4] = target.getArmor(Protomech.LOC_HEAD, false);
            armor_values[5] = 100;
            armor_values[6] = 100;
            if (((Protomech)target).hasMainGun()){
                max_index++;
                armor_values[max_index] = target.getArmor(Protomech.LOC_MAINGUN, false);
            }
        }
        // If the target is a vehicle
        if (target instanceof Tank){
            // Create vector of armor locations
            max_index = 0;
            if (hit_side == ToHitData.SIDE_FRONT){
                armor_values[0] = target.getArmor(Tank.LOC_FRONT);
            }
            if (hit_side == ToHitData.SIDE_RIGHT){
                armor_values[0] = target.getArmor(Tank.LOC_RIGHT);
            }
            if (hit_side == ToHitData.SIDE_LEFT){
                armor_values[0] = target.getArmor(Tank.LOC_LEFT);
            }
            if (hit_side == ToHitData.SIDE_REAR){
                armor_values[0] = target.getArmor(Tank.LOC_REAR);
            }
            if (!(((Tank)target).hasNoTurret())){
                max_index++;
                armor_values[max_index] = target.getArmor(Tank.LOC_TURRET);
            }
        }
        // If the target is Battle Armor
        if (target instanceof BattleArmor){
            // Create vector of armor of surviving troopers
            max_index = -1;
            for (int i=1; i < ((BattleArmor)target).getShootingStrength(); i++){
                if (target.getArmor(i) >= 0){
                    max_index++;
                    armor_values[max_index] = target.getArmor(i);
                }
            }
        }
        // If the target is conventional infantry
        if ((target instanceof Infantry) && !(target instanceof BattleArmor)){
            // Create a single element vector with total number of troopers
            max_index = 0;
            armor_values[0] = ((Infantry)target).getShootingStrength();
        }
        
        double hit_total = group;
        // While hit damage is less than total damage applied, increment by group value
        for (; hit_total <= damage; hit_total += group){
            for (int i = 0; i <= max_index; i++){
                // If hit damage can penetrate location
                if (hit_total > armor_values[i]){
                    final_multiplier += base_multiplier;
                }
            }
            base_multiplier = base_multiplier/2;
        }
        
        // Return final multiplier
        
        return final_multiplier;
        
    }
}
