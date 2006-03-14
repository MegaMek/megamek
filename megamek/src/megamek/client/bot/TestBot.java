/*
 * MegaMek -
 * Copyright (C) 2000,2001,2002,2003,2004,2005 Ben Mazur (bmazur@sev.org)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 */

package megamek.client.bot;

import megamek.common.AmmoType;
import megamek.common.Compute;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.IEntityMovementType;
import megamek.common.IHex;
import megamek.common.Mech;
import megamek.common.Minefield;
import megamek.common.MiscType;
import megamek.common.Mounted;
import megamek.common.MovePath;
import megamek.common.Protomech;
import megamek.common.Terrains;
import megamek.common.ToHitData;
import megamek.common.WeaponType;
import megamek.common.actions.ChargeAttackAction;
import megamek.common.actions.DfaAttackAction;
import megamek.common.actions.TorsoTwistAction;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.event.GamePlayerChatEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;
import java.util.Vector;

public class TestBot extends BotClient {

    public LinkedList unit_values = new LinkedList();
    public LinkedList enemy_values = new LinkedList();

    public CEntity.Table centities = new CEntity.Table(this);

    protected ChatProcessor chatp = new ChatProcessor();

    public int ignore = 10;

    int enemies_moved = 0;
    GALance old_moves = null;
    int my_mechs_moved = 0;

    public TestBot(String name, String host, int port) {
        super(name, host, port);
        ignore = config.getIgnoreLevel();
    }

    public void initialize() {
        //removed
    }

    public PhysicalOption calculatePhysicalTurn() {
        return PhysicalCalculator.calculatePhysicalTurn(this);
    }

    public MovePath calculateMoveTurn() {
        long enter = System.currentTimeMillis();
        int initiative = 0;
        MoveOption min = null;

        System.out.println("beginning movement calculations...");

        //first check and that someone else has moved so we don't replan
        Object[] enemy_array = this.getEnemyEntities().toArray();
        for (int j = 0; j < enemy_array.length; j++) {
            if (!((Entity) enemy_array[j]).isSelectableThisTurn()) {
                initiative++;
            }
        }
        // if nobody's moved and we have a valid move waiting, use that
        if (initiative == enemies_moved && old_moves != null) {
            min = this.old_moves.getResult();
            if (min == null
                    || !min.isMoveLegal()
                    || (min.isPhysical && centities.get(min.getPhysicalTargetId()).isPhysicalTarget)) {
                this.old_moves = null;
                System.out.println("recalculating moves since the old move was invalid");
                return calculateMoveTurn();
            }
        } else {
            enemies_moved = initiative;
            ArrayList possible = new ArrayList();

            Enumeration e = game.getEntities();

            while (e.hasMoreElements()) {
                Entity entity = (Entity) e.nextElement();
                
                // ignore loaded and off-board units
                if (entity.getPosition() == null || entity.isOffBoard()) {
                    continue;
                }

                CEntity cen = centities.get(entity);
                cen.refresh();
                firstPass(cen);
            }

            Iterator i = this.getEntitiesOwned().iterator();
            boolean short_circuit = false;

            while (i.hasNext() && !short_circuit) {
                Entity entity = (Entity) i.next();

                // ignore loaded units 
                // (not really necessary unless bot manages to load units)
                if (entity.getPosition() == null) {
                    continue;
                }

                // if we can't move this entity right now, ignore it
                if (!game.getTurn().isValidEntity(entity, game)) {
                    continue;
                }

                CEntity cen = centities.get(entity);

                System.out.println("Contemplating movement of " + entity.getShortName() + " " + entity.getId());

                MoveOption[] result = calculateMove(entity);

                if (game.getOptions().booleanOption("skip_ineligable_movement") && cen.getEntity().isImmobile()) {
                    cen.moved = true;
                } else if (!cen.moved) {
                    if (result.length < 6) {
                        min = result.length > 0 ? (MoveOption) result[0] : null;
                        short_circuit = true;
                    }
                    possible.add(result);
                }
            }

            //should ignore mechs that are not engaged
            //and only do the below when there are 2 or mechs left to move
            if (!short_circuit) {
                if (this.getEntitiesOwned().size() > 1) {
                    GALance lance = new GALance(this, possible, 50, 80);
                    lance.evolve();
                    min = lance.getResult();
                    this.old_moves = lance;
                } else if (
                        ((MoveOption[]) possible.get(0)) != null
                        && ((MoveOption[]) possible.get(0)).length > 0) {
                    min = ((MoveOption[]) possible.get(0))[0];
                }
            }
        }
        if (min == null) {
            min = new MoveOption(game, centities.get(getFirstEntityNum()));
        }
        for (int d = 0; d < enemy_array.length; d++) {
            Entity en = (Entity) enemy_array[d];
            
            // ignore loaded units
            if (en.getPosition() == null) {
                continue;
            }

            CEntity enemy = centities.get(en);
            int enemy_hit_arc =
                    CEntity.getThreatHitArc(enemy.current.getFinalCoords(),
                            enemy.current.getFinalFacing(),
                            min.getFinalCoords());
            MoveOption.DamageInfo di = (MoveOption.DamageInfo) min.damageInfos.get(enemy);
            if (di != null) {
                enemy.expected_damage[enemy_hit_arc] += di.min_damage;
            }
            if (enemy.expected_damage[enemy_hit_arc] > 0) {
                enemy.hasTakenDamage = true;
            }
        }
        if (min.isPhysical) {
            centities.get(min.getPhysicalTargetId()).isPhysicalTarget = true;
        }
        System.out.println(min);
        min.getCEntity().current = min;
        min.getCEntity().last = min;
        this.my_mechs_moved++;
        min.getCEntity().moved = true;

        long exit = System.currentTimeMillis();
        System.out.println("move turn took " + (exit - enter) + " ms");
        
        // If this unit has a jammed RAC, and it has only walked,
        // add an unjam action
        if (min != null) {
            if (min.getLastStep() != null) {
                if (min.getCEntity().entity.canUnjamRAC()) {
                    if ((min.getLastStep().getMovementType() == IEntityMovementType.MOVE_WALK) ||
                            (min.getLastStep().getMovementType() == IEntityMovementType.MOVE_VTOL_WALK) ||
                            (min.getLastStep().getMovementType() == IEntityMovementType.MOVE_NONE)) {
                        // Cycle through all available weapons, only unjam if the jam(med)
                        // RACs count for a significant portion of possible damage
                        int rac_damage = 0;
                        int other_damage = 0;
                        int clearance_range = 0;
                        for (Enumeration mounted_weapons = min.getCEntity().entity.getWeapons();
                             mounted_weapons.hasMoreElements();) {
                            WeaponType test_weapon = new WeaponType();
                            Mounted equip = (Mounted) mounted_weapons.nextElement();

                            test_weapon = (WeaponType) equip.getType();
                            if ((test_weapon.getAmmoType() == AmmoType.T_AC_ROTARY) &&
                                    (equip.isJammed() == true)) {
                                rac_damage = rac_damage + 4 * (test_weapon.getDamage());
                            } else {
                                if (equip.canFire()) {
                                    other_damage += test_weapon.getDamage();
                                    if (test_weapon.getMediumRange() > clearance_range) {
                                        clearance_range = test_weapon.getMediumRange();
                                    }
                                }
                            }
                        }
                        // Even if the jammed RAC doesn't make up a significant portion
                        // of the units damage, its still better to have it functional
                        // If nothing is "close" then unjam anyways
                        int check_range = 100;
                        for (Enumeration unit_selection = game.getEntities();
                             unit_selection.hasMoreElements();) {
                            Entity enemy = (Entity) unit_selection.nextElement();
                            if ((min.getCEntity().entity.getPosition() != null) &&
                                    (enemy.getPosition() != null) &&
                                    (enemy.isEnemyOf(min.getCEntity().entity))) {
                                if (enemy.isVisibleToEnemy()) {
                                    if (min.getCEntity().entity.getPosition().distance
                                            (enemy.getPosition()) < check_range) {
                                        check_range = min.getCEntity().entity.getPosition().
                                                distance(enemy.getPosition());
                                    }
                                }
                            }
                        }
                        if ((rac_damage >= other_damage) || (check_range < clearance_range)) {
                            min.addStep(MovePath.STEP_UNJAM_RAC);
                        }
                    }
                }
            }
        }

        return min;
    }

    public MoveOption[] calculateMove(Entity entity) {
        ArrayList enemy_array = new ArrayList(game.getValidTargets(entity));
        ArrayList<Entity> entities = new ArrayList<Entity>(game.getEntitiesVector());
        CEntity self = centities.get(entity);
        MoveOption[] move_array;
        int friends = entities.size() - enemy_array.size();

        move_array = secondPass(self, friends, enemy_array, entities);
        //top balanced
        filterMoves(move_array, self.pass, new MoveOption.WeightedComparator(1, 1), 50);
        //top damage
        filterMoves(move_array, self.pass, new MoveOption.WeightedComparator(.5, 1), 50);

        move_array = thirdPass(self, enemy_array);

        //top balanced
        filterMoves(move_array, self.pass, new MoveOption.WeightedComparator(1, 1), 30);
        //top damage
        filterMoves(move_array, self.pass, new MoveOption.WeightedComparator(.5, 1), 30);

        //reduce self threat, and add bonus for terrain
        for (Iterator i = self.pass.values().iterator(); i.hasNext();) {
            MoveOption option = (MoveOption) i.next();
            option.setState();
            option.self_damage *= .5;
            option.self_threat *= .5;
            //TODO: should scale to the unit bv
            double terrain = 2 * ((double) Compute.getTargetTerrainModifier(game, option.getEntity()).getValue());
            option.tv.add(terrain + " Terrain Adjusment " + "\n");
            option.self_threat -= terrain;
        }

        move_array = fourthPass(self, enemy_array);
        //top balanced
        filterMoves(move_array, self.pass, new MoveOption.WeightedComparator(1, 1), 20);
        //top damage
        filterMoves(move_array, self.pass, new MoveOption.WeightedComparator(.5, 1), 20);

        //reduce transient damage estimates
        for (Iterator i = self.pass.values().iterator(); i.hasNext();) {
            MoveOption option = (MoveOption) i.next();
            option.self_threat *= .5;
            option.self_damage *= .5;
        }

        move_array = fifthPass(self, enemy_array);

        /***********************************************************************
         * Return top twenty moves to the lance algorithm
         **********************************************************************/
        MoveOption[] result = new MoveOption[Math.min(move_array.length, 20)];
        int offset = 0;
        for (int i = 0; i < Math.min(move_array.length, 20); i++) {
            MoveOption next = (MoveOption) move_array[i];
            if (next.isPhysical && self.range_damages[CEntity.RANGE_SHORT] > 5 && next.doomed) {
                if (offset + 20 < move_array.length) {
                    next = (MoveOption) move_array[offset + 20];
                    offset++;
                }
            }
            result[i] = next;
        }
        return result;
    }

    /**
     * ************************************************************************
     * first pass, filter moves based upon present case
     * ************************************************************************
     */
    public void firstPass(CEntity self) {
        ArrayList enemies = getEnemyEntities();
        MoveOption[] move_array;
        if (self.getEntity().isSelectableThisTurn() && !self.moved) {
            move_array = self.getAllMoves().values().toArray(new MoveOption[0]);
        } else {
            move_array = new MoveOption[]{self.current};
        }
        System.out.println(String.format("%s has %d moves", self.getEntity().getShortName(), move_array.length));
        for (int i = 0; i < move_array.length; i++) {
            MoveOption option = (MoveOption) move_array[i];
            option.setState();
            for (int e = 0; e < enemies.size(); e++) { // for each enemy
                Entity en = (Entity) enemies.get(e);
                
                // ignore loaded units
                if (en.getPosition() == null) {
                    continue;
                }

                CEntity enemy = centities.get(en);
                int[] modifiers = option.getModifiers(enemy.getEntity());
                if (modifiers[MoveOption.DEFENCE_MOD] == ToHitData.IMPOSSIBLE
                        && modifiers[MoveOption.ATTACK_MOD] == ToHitData.IMPOSSIBLE) {
                    continue;
                }
                int enemy_hit_arc =
                        CEntity.getThreatHitArc(enemy.current.getFinalCoords(),
                                enemy.current.getFinalFacing(),
                                option.getFinalCoords());
                int self_hit_arc =
                        CEntity.getThreatHitArc(option.getFinalCoords(),
                                option.getFinalFacing(),
                                enemy.current.getFinalCoords());
                if (!enemy.getEntity().isImmobile() && modifiers[MoveOption.DEFENCE_MOD] != ToHitData.IMPOSSIBLE) {
                    self.engaged = true;
                    int mod = modifiers[MoveOption.DEFENCE_MOD];
                    double max = option.getMaxModifiedDamage(enemy.current, mod, modifiers[MoveOption.DEFENCE_PC]);
                    if (en.isSelectableThisTurn()) {
                        enemy.current.addStep(MovePath.STEP_TURN_RIGHT);
                        max =
                                Math.max(option.getMaxModifiedDamage(enemy.current, mod + 1, modifiers[MoveOption.DEFENCE_PC]),
                                        max);
                        enemy.current.removeLastStep();
                        enemy.current.addStep(MovePath.STEP_TURN_LEFT);
                        max =
                                Math.max(option.getMaxModifiedDamage(enemy.current, mod + 1, modifiers[MoveOption.DEFENCE_PC]),
                                        max);
                        //return to original facing
                        enemy.current.removeLastStep();
                    }
                    max = self.getThreatUtility(max, self_hit_arc);
                    if (enemy.getEntity().isProne())
                        max *= enemy.base_psr_odds;
                    MoveOption.DamageInfo di = option.getDamageInfo(enemy, true);
                    di.threat = max;
                    di.max_threat = max;
                    option.threat += max;
                    option.tv.add(max + " Threat " + e + "\n");
                }
                /*
                 * As a first approximation, take the maximum to a single
                 * target
                 */
                if (!option.isPhysical) {
                    if (modifiers[MoveOption.ATTACK_MOD] != ToHitData.IMPOSSIBLE) {
                        self.engaged = true;
                        double max =
                                enemy.current.getMaxModifiedDamage(option, modifiers[0], modifiers[MoveOption.ATTACK_PC]);
                        max = enemy.getThreatUtility(max, enemy_hit_arc);
                        MoveOption.DamageInfo di = option.getDamageInfo(enemy, true);
                        di.damage = max;
                        di.min_damage = max;
                        option.tv.add(max + " Damage " + e + "\n");
                        option.damage = Math.max(max, option.damage);
                    }
                } else {
                    CEntity target = centities.get(option.getPhysicalTargetId());
                    try {
                        if (target.getEntity().getId() == enemy.getEntity().getId()) {
                            if (!target.isPhysicalTarget) {
                                ToHitData toHit = null;
                                double self_threat = 0;
                                double damage = 0;
                                if (option.isJumping()) {
                                    self.current.setState();
                                    toHit =
                                            DfaAttackAction.toHit(game, option.getEntity().getId(), target.getEntity(), option);
                                    damage = 2 * DfaAttackAction.getDamageFor(option.getEntity());
                                    self_threat =
                                            option.getCEntity().getThreatUtility(DfaAttackAction.getDamageTakenBy(option.getEntity()),
                                                    ToHitData.SIDE_REAR)
                                            * Compute.oddsAbove(toHit.getValue())
                                            / 100;
                                    self_threat
                                            += option.getCEntity().getThreatUtility(.1 * self.getEntity().getWeight(),
                                                    ToHitData.SIDE_REAR);
                                    self_threat *= 100 / option.getCEntity().getEntity().getWeight();
                                } else {
                                    self.current.setState();
                                    toHit =
                                            new ChargeAttackAction(option.getEntity(), target.getEntity()).toHit(game,
                                                    option);
                                    damage =
                                            ChargeAttackAction.getDamageFor(option.getEntity(),
                                                    option.getHexesMoved());
                                    self_threat =
                                            option.getCEntity().getThreatUtility(ChargeAttackAction.getDamageTakenBy(option.getEntity(),
                                                    target.getEntity()),
                                                    ToHitData.SIDE_FRONT)
                                            * (Compute.oddsAbove(toHit.getValue()) / 100);
                                    option.setState();
                                }
                                damage =
                                        target.getThreatUtility(damage, toHit.getSideTable())
                                        * Compute.oddsAbove(toHit.getValue())
                                        / 100;
                                //charging is a good tactic against larger
                                // mechs
                                if (!option.isJumping())
                                    damage *= Math.sqrt((double) enemy.bv / (double) self.bv);
                                //these are always risky, just don't on 11 or
                                // 12
                                if (toHit.getValue() > 10)
                                    damage = 0;
                                //7 or less is good
                                if (toHit.getValue() < 8)
                                    damage *= 1.5;
                                //this is all you are good for
                                if (self.range_damages[CEntity.RANGE_SHORT] < 5)
                                    damage *= 2;
                                MoveOption.DamageInfo di = option.getDamageInfo(enemy, true);
                                di.damage = damage;
                                di.min_damage = damage;
                                option.damage = damage;
                                option.movement_threat += self_threat;
                            } else {
                                option.threat += Integer.MAX_VALUE;
                            }
                        }
                    } catch (Exception e1) {
                        e1.printStackTrace();
                        option.threat += Integer.MAX_VALUE;
                    }
                }
            } //-- end while of each enemy
            self.current.setState();
        } //-- end while of first pass
        //top balanced
        filterMoves(move_array, self.pass, new MoveOption.WeightedComparator(1, 1), 100);
        //top damage
        filterMoves(move_array, self.pass, new MoveOption.WeightedComparator(.5, 1), 100);
    }

    /**
     * ********************************************************************
     * Second pass, combination moves/firing based only on the present
     * case, since only one mech moves at a time
     * ********************************************************************
     */
    private MoveOption[] secondPass(CEntity self, int friends, ArrayList enemy_array, ArrayList<Entity> entities) {
        MoveOption[] move_array = self.pass.values().toArray(new MoveOption[0]);
        self.pass.clear();
        for (int j = 0; j < move_array.length && friends > 2; j++) {
            MoveOption option = (MoveOption) move_array[j];
            for (int e = 0; e < enemy_array.size(); e++) {
                Entity en = (Entity) enemy_array.get(e);
                CEntity enemy = centities.get(en);
                for (Entity other : entities) {
                    if (other.isEnemyOf(self.entity)) {
                        continue;
                    }
                    MoveOption foption = centities.get(other).current;
                    double threat_divisor = 1;
                    MoveOption.DamageInfo di = option.getDamageInfo(enemy, true);
                    if (foption.getDamageInfo(enemy, false) != null) {
                        option.damage += (enemy.canMove() ? .1 : .2) * di.damage;
                        threat_divisor += foption.getCEntity().canMove() ? .4 : .6;
                    }
                    option.threat -= di.threat;
                    di.threat /= threat_divisor;
                    option.threat += di.threat;
                }
            }
        }
        return move_array;
    }

    /**
     * ********************************************************************
     * third pass, (not so bad) oppurtunistic planner gives preference to
     * good ranges/defensive positions based upon the mech characterization
     * ********************************************************************
     */
    private MoveOption[] thirdPass(CEntity self, ArrayList enemy_array) {
        MoveOption[] move_array = self.pass.values().toArray(new MoveOption[0]);
        self.pass.clear();

        for (int j = 0; j < move_array.length; j++) {
            MoveOption option = (MoveOption) move_array[j];
            option.setState();
            double adjustment = 0;
            double temp_adjustment = 0;
            for (int e = 0; e < enemy_array.size(); e++) { // for each enemy
                Entity en = (Entity) enemy_array.get(e);
                CEntity enemy = centities.get(en);
                int current_range = self.current.getFinalCoords().distance(enemy.current.getFinalCoords());
                int range = option.getFinalCoords().distance(enemy.current.getFinalCoords());
                if (range > self.long_range) {
                    temp_adjustment += (!(range < enemy.long_range) ? .5 : 1)
                            * (1 + self.range_damages[self.range])
                            * (Math.max(range - self.long_range - .5 * Math.max(self.jumpMP, .8 * self.runMP), 0));
                }
                if ((self.range == CEntity.RANGE_SHORT && (current_range > 5 || range > 9))
                        || (self.range_damages[CEntity.RANGE_SHORT] < 4 && current_range > 10)) {
                    temp_adjustment += ((enemy.range > CEntity.RANGE_SHORT) ? .5 : 1)
                            * (Math.max(1 + self.range_damages[CEntity.RANGE_SHORT], 5))
                            * Math.max(range - .5 * Math.max(self.jumpMP, .8 * self.runMP), 0);
                } else if (self.range == CEntity.RANGE_MEDIUM) {
                    temp_adjustment += ((current_range < 6 || current_range > 12) ? 1 : .25)
                            * ((enemy.range > CEntity.RANGE_SHORT) ? .5 : 1)
                            * (1 + self.range_damages[CEntity.RANGE_MEDIUM])
                            * Math.abs(range - .5 * Math.max(self.jumpMP, .8 * self.runMP));
                } else if (option.damage < .25 * self.range_damages[CEntity.RANGE_LONG]) {
                    temp_adjustment += ((range < 10) ? .25 : 1)
                            * (Math.max(1 + self.range_damages[CEntity.RANGE_LONG], 3))
                            * (1 / (1 + option.threat));
                }
                adjustment += Math.sqrt(temp_adjustment * enemy.bv / self.bv);
                //I would always like to face the opponent
                if (!(enemy.getEntity().isProne() || enemy.getEntity().isImmobile())
                        && CEntity.getThreatHitArc(option.getFinalCoords(),
                                option.getFinalFacing(),
                                enemy.getEntity().getPosition())
                        != ToHitData.SIDE_FRONT) {
                    int fa =
                            CEntity.getFiringAngle(option.getFinalCoords(),
                                    option.getFinalFacing(),
                                    enemy.getEntity().getPosition());
                    if (fa > 90 && fa < 270) {
                        int distance = option.getFinalCoords().distance(enemy.current.getFinalCoords());
                        double mod = 1;
                        if (fa > 130 && fa < 240)
                            mod = 2;
                        //big formula that says don't do it
                        mod *= ((Math.max(self.jumpMP, .8 * self.runMP) < 5) ? 2 : 1)
                                * ((double) self.bv / (double) 50)
                                * Math.sqrt(((double) self.bv) / enemy.bv)
                                / ((double) distance / 6 + 1);
                        option.self_threat += mod;
                        option.tv.add(mod + " " + fa + " Back to enemy\n");
                    }
                }
            }
            adjustment *= self.overall_armor_percent * self.strategy.attack / enemy_array.size();
            //fix for hiding in level 2 water
            //To a greedy bot, it always seems nice to stay in here...
            IHex h = game.getBoard().getHex(option.getFinalCoords());
            if (h.containsTerrain(Terrains.WATER)
                    && h.surface() > (self.getEntity().getElevation() + ((option.getFinalProne()) ? 0 : 1))) {
                double mod = (self.getEntity().heat + option.getMovementheatBuildup() <= 7) ? 100 : 30;
                adjustment += self.bv / mod;
            }
            //add them in now, then re-add them later
            if (self.range > CEntity.RANGE_SHORT) {
                int ele_dif =
                        game.getBoard().getHex(option.getFinalCoords()).getElevation()
                        - game.getBoard().getHex(self.current.getFinalCoords()).getElevation();
                adjustment -= (Math.max(ele_dif, 0) + 1)
                        * ((double) Compute.getTargetTerrainModifier(game, option.getEntity()).getValue() + 1);
            }

            //close the range if nothing else and healthy
            if (option.damage < .25 * self.range_damages[self.range] && adjustment < self.range_damages[self.range]) {
                for (int e = 0; e < enemy_array.size(); e++) {
                    Entity en = (Entity) enemy_array.get(e);
                    CEntity enemy = centities.get(en);
                    int range = option.getFinalCoords().distance(enemy.current.getFinalCoords());
                    if (range > 5)
                        adjustment += Math.pow(self.overall_armor_percent, 2)
                                * Math.sqrt((double) (range - 4) * enemy.bv / self.bv)
                                / enemy_array.size();
                }
            }

            if (option.damage < .25 * (1 + self.range_damages[self.range])) {
                option.self_threat += 2 * adjustment;
            } else if (option.damage < .5 * (1 + self.range_damages[self.range])) {
                option.self_threat += adjustment;
            }
            option.tv.add(option.self_threat + " Initial Damage Adjustment " + "\n");
        }

        return move_array;
    }

    //pass should contains 30 ~ 60
    /**
     * ********************************************************************
     * fourth pass, speculation on top moves use averaging to filter
     * ********************************************************************
     */
    private MoveOption[] fourthPass(CEntity self, ArrayList enemy_array) {
        MoveOption[] move_array = self.pass.values().toArray(new MoveOption[0]);
        self.pass.clear();
        for (int e = 0; e < enemy_array.size(); e++) { // for each enemy
            Entity en = (Entity) enemy_array.get(e);
            CEntity enemy = centities.get(en);
            //engage in speculation on "best choices" when you loose iniative
            if (enemy.canMove()) {
                Object[] enemy_move_array = enemy.pass.values().toArray();
                ArrayList to_check = new ArrayList();
                //check some enemy moves
                for (int j = 0; j < move_array.length; j++) {
                    MoveOption option = null;
                    to_check.clear();
                    option = (MoveOption) move_array[j];
                    option.setState();
                    //check for damning hexes specifically
                    //could also look at intervening defensive
                    ArrayList coord = new ArrayList();
                    Coords back = option.getFinalCoords().translated((option.getFinalFacing() + 3) % 6);
                    coord.add(back);
                    coord.add(back.translated((option.getFinalFacing() + 2) % 6));
                    coord.add(back.translated((option.getFinalFacing() + 4) % 6));
                    coord.add(option.getFinalCoords().translated((option.getFinalFacing())));
                    coord.add(option.getFinalCoords().translated((option.getFinalFacing() + 1) % 6));
                    coord.add(option.getFinalCoords().translated((option.getFinalFacing() + 2) % 6));
                    coord.add(option.getFinalCoords().translated((option.getFinalFacing() + 4) % 6));
                    coord.add(option.getFinalCoords().translated((option.getFinalFacing() + 5) % 6));
                    Iterator ci = coord.iterator();
                    while (ci.hasNext()) {
                        Coords test = (Coords) ci.next();
                        List c = enemy.findMoves(test);
                        if (c.size() != 0)
                            to_check.addAll(c);
                    }
                    int range = option.getFinalCoords().distance(enemy.current.getFinalCoords());
                    int compare = 0;
                    if ((enemy.long_range) > range - Math.max(enemy.jumpMP, enemy.runMP)) {
                        compare = 30;
                    } else if (enemy.long_range > range) {
                        compare = 10;
                    }
                    double mod = this.enemies_moved / this.getEnemyEntities().size();
                    compare *= (1 + mod);
                    for (int k = 0; k <= compare && k < enemy_move_array.length; k++) {
                        if (enemy_move_array.length < compare) {
                            to_check.add(enemy_move_array[k]);
                        } else {
                            int value = Compute.randomInt(enemy_move_array.length);
                            if (value % 2 == 1) {
                                to_check.add(enemy_move_array[value]);
                            } else {
                                to_check.add(enemy_move_array[k]);
                            }
                        }
                    }
                    Iterator eo = to_check.iterator();
                    while (eo.hasNext()) {
                        MoveOption enemy_option = (MoveOption) eo.next();
                        double max_threat = 0;
                        double max_damage = 0;
                        enemy_option.setState();
                        int enemy_hit_arc =
                                CEntity.getThreatHitArc(enemy_option.getFinalCoords(),
                                        enemy_option.getFinalFacing(),
                                        option.getFinalCoords());
                        int self_hit_arc =
                                CEntity.getThreatHitArc(enemy_option.getFinalCoords(),
                                        enemy_option.getFinalFacing(),
                                        option.getFinalCoords());
                        if (enemy_option.isJumping()) {
                            enemy_hit_arc = Compute.ARC_FORWARD;
                        }
                        int[] modifiers = option.getModifiers(enemy_option.getEntity());
                        if (modifiers[1] != ToHitData.IMPOSSIBLE) {
                            self.engaged = true;
                            if (!enemy_option.isJumping()) {
                                max_threat =
                                        option.getMaxModifiedDamage(enemy_option,
                                                modifiers[1],
                                                modifiers[MoveOption.DEFENCE_PC]);
                            } else {
                                max_threat =
                                        .8
                                        * enemy.getModifiedDamage((modifiers[MoveOption.DEFENCE_PC] == 1) ? CEntity.TT : ToHitData.SIDE_FRONT,
                                                enemy_option.getFinalCoords().distance(option.getFinalCoords()),
                                                modifiers[1]);
                            }
                            max_threat = self.getThreatUtility(max_threat, self_hit_arc);
                        }
                        if (modifiers[0] != ToHitData.IMPOSSIBLE) {
                            self.engaged = true;
                            max_damage =
                                    enemy_option.getMaxModifiedDamage(option,
                                            modifiers[0],
                                            modifiers[MoveOption.ATTACK_PC]);
                            max_damage = enemy.getThreatUtility(max_damage, enemy_hit_arc);
                            if (option.isPhysical) {
                                if (centities.get(option.getPhysicalTargetId()).getEntity().getId()
                                        == enemy.getEntity().getId()) {
                                    max_damage = option.getDamage(enemy);
                                } else {
                                    max_damage = 0;
                                }
                            }
                        }
                        MoveOption.DamageInfo di = option.getDamageInfo(enemy, true);
                        di.max_threat = Math.max(max_threat, di.max_threat);
                        di.min_damage = Math.min(di.min_damage, max_damage);
                        if (max_threat - max_damage > di.threat - di.damage) {
                            di.threat = max_threat;
                            di.damage = max_damage;
                            option.tv.add(max_threat + " Spec Threat " + e + "\n");
                            option.tv.add(max_damage + " Spec Damage " + e + "\n");
                        }
                    }
                    //update estimates
                    option.damage = 0;
                    option.threat = 0;
                    for (Iterator i = option.damageInfos.keySet().iterator(); i.hasNext();) {
                        //my damage is the average of expected and min
                        CEntity cen = (CEntity) i.next();
                        //rescale
                        MoveOption.DamageInfo di = option.getDamageInfo(cen, true);
                        di.min_damage /= cen.strategy.target;
                        di.damage /= cen.strategy.target;
                        option.damage += (di.min_damage + di.damage) / 2;

                        //my threat is average of absolute worst, and expected
                        option.threat = Math.max(option.threat, di.max_threat + di.threat) / 2;
                        di.threat = (di.max_threat + 2 * di.threat) / 3;
                    }
                }
                //restore enemy
                enemy.current.setState();
            }
            self.current.setState();
        } //--end move speculation
        return move_array;
    }

    //pass should now be 20 ~ 40
    /**
     * ********************************************************************
     * fifth pass, final damage and threat approximation --prevents moves
     * that from the previous pass would cause the mech to die
     * ********************************************************************
     */
    private MoveOption[] fifthPass(CEntity self, ArrayList enemy_array) {
        MoveOption[] move_array = self.pass.values().toArray(new MoveOption[0]);
        self.pass.clear();

        if (self.engaged) {
            for (int j = 0; j < move_array.length; j++) {
                MoveOption option = (MoveOption) move_array[j];
                option.setState();
                GAAttack temp = this.bestAttack(option);
                if (temp != null) {
                    option.damage = (option.damage + temp.getFittestChromosomesFitness()) / 2;
                } else {
                    option.damage /= 2;
                }
                for (int e = 0; e < enemy_array.size(); e++) { // for each
                    // enemy
                    Entity en = (Entity) enemy_array.get(e);
                    CEntity enemy = centities.get(en);
                    if (!enemy.canMove()) {
                        option.setThreat(enemy,
                                (option.getThreat(enemy) + this.attackUtility(enemy.current, self)) / 2);
                        option.tv.add(option.getThreat(enemy) + " Revised Threat " + e + " \n");
                        if (!option.isPhysical) {
                            if (temp != null) {
                                option.setDamage(enemy, (option.getDamage(enemy) + temp.getDamageUtility(enemy)) / 2);
                            } else {
                                //probably zero, but just in case
                                option.setDamage(enemy, option.getMinDamage(enemy));
                            }
                            option.tv.add(option.getDamage(enemy) + " Revised Damage " + e + " \n");
                            //this needs to be reworked
                            if (option.getFinalCoords().distance(enemy.current.getFinalCoords()) == 1) {
                                PhysicalOption p =
                                        PhysicalCalculator.getBestPhysicalAttack(option.getEntity(), enemy.getEntity(), game);
                                if (p != null) {
                                    option.setDamage(enemy, option.getDamage(enemy) + p.expectedDmg);
                                    option.tv.add(p.expectedDmg + " Physical Damage " + e + " \n");
                                }
                                p = PhysicalCalculator.getBestPhysicalAttack(enemy.getEntity(), option.getEntity(), game);
                                if (p != null) {
                                    option.setThreat(enemy, option.getThreat(enemy) + .5 * p.expectedDmg);
                                    option.tv.add(.5 * p.expectedDmg + " Physical Threat " + e + " \n");
                                }
                            }
                        }
                    } else if (!option.isPhysical) { //enemy can move (not
                        if (temp != null) {
                            option.setDamage(enemy, (2 * option.getDamage(enemy) + temp.getDamageUtility(enemy)) / 3);
                        } else {
                            option.setDamage(enemy, option.getMinDamage(enemy));
                        }
                    } else {
                        //get a more accurate estimate
                        option.setDamage(enemy,
                                option.getDamage(enemy) / Math.sqrt((double) enemy.bv / (double) self.bv));
                        option.damage = option.getDamage(enemy);
                    }
                }
                option.threat = 0;
                for (Iterator i = option.damageInfos.values().iterator(); i.hasNext();) {
                    option.threat += ((MoveOption.DamageInfo) i.next()).threat;
                }
                option.tv.add(option.threat + " Revised Threat Utility\n");
                option.tv.add(option.damage + " Revised Damage Utility\n");
            }
        }
        Arrays.sort(move_array, new MoveOption.WeightedComparator(1, 1));
        self.current.setState();

        return move_array;
    }

    private void filterMoves(Object[] move_array,
                             MoveOption.Table pass,
                             MoveOption.WeightedComparator comp,
                             int filter) {
        Arrays.sort(move_array, comp);

        //top 100 utility, mostly conservative
        for (int i = 0; i < filter && i < move_array.length; i++) {
            pass.put((MoveOption) move_array[i]);
        }
    }

    protected void initFiring() {
        ArrayList entities = new ArrayList(game.getEntitiesVector());
        for (int i = 0; i < entities.size(); i++) {
            Entity entity = (Entity) entities.get(i);
            CEntity centity = centities.get(entity);
            centity.reset();
            centity.enemy_num = i;
        }
        for (Iterator i = this.getEnemyEntities().iterator(); i.hasNext();) {
            Entity entity = (Entity) i.next();
            CEntity centity = centities.get(entity);
            if (entity.isMakingDfa() || entity.isCharging()) {
                //try to prevent a physical attack from happening
                //but should take into account the toHit of the attack
                centity.strategy.target = 2.5;
            }
        }
    }

    protected ArrayList calculateWeaponAttacks(Entity en, Mounted mw, boolean best_only) {
        int from = en.getId();
        int weaponID = en.getEquipmentNum(mw);
        int spin_mode = 0;
        ArrayList result = new ArrayList();
        Enumeration ents = game.getValidTargets(en).elements();
        WeaponAttackAction wep_test;
        WeaponType spinner;
        AttackOption a = null;
        AttackOption max = new AttackOption(null, null, 0, null);
        while (ents.hasMoreElements()) {
            Entity e = (Entity) ents.nextElement();
            CEntity enemy = centities.get(e);
//            long entry = System.currentTimeMillis();
            ToHitData th = WeaponAttackAction.toHit(game, from, e, weaponID);
//            long exit = System.currentTimeMillis();
//            if (exit != entry)
//                System.out.println("Weapon attack toHit took "+(exit-entry));
            if (th.getValue() != ToHitData.IMPOSSIBLE && !(th.getValue() >= 13)) {
                double expectedDmg;

                wep_test = new WeaponAttackAction(from, e.getId(), weaponID);

                // If this is an Ultra or Rotary cannon, check for spin up
                spinner = (WeaponType) mw.getType();
                if ((spinner.getAmmoType() == AmmoType.T_AC_ULTRA)
                        || (spinner.getAmmoType() == AmmoType.T_AC_ULTRA_THB)
                        || (spinner.getAmmoType() == AmmoType.T_AC_ROTARY)) {
                    spin_mode = Compute.spinUpCannon(game, wep_test);
                    super.sendModeChange(from, weaponID, spin_mode);
                }

                // Ammo cycler runs each valid ammo type through the weapon while 
                // calling for expected damage on each type; best type by damage is loaded

                expectedDmg = Compute.getAmmoAdjDamage(game, wep_test);

                a = new AttackOption(enemy, mw, expectedDmg, th);
                if (a.value > max.value) {
                    if (best_only) {
                        max = a;
                    } else {
                        result.add(0, a);
                    }
                } else {
                    result.add(a);
                }
            }
        }
        if (best_only && max.target != null) {
            result.add(max);
        }
        if (result.size() > 0) {
            result.add(new AttackOption(null, mw, 0, null));
        }
        return result;
    }

    public GAAttack bestAttack(MoveOption es) {
        return bestAttack(es, null, 2);
    }

    public GAAttack bestAttack(MoveOption es, CEntity target, int search_level) {
        Entity en = es.getEntity();
        int attacks[] = new int[4];
        ArrayList c = new ArrayList();
        ArrayList front = new ArrayList();
        ArrayList left = new ArrayList();
        ArrayList right = new ArrayList();
        ArrayList rear = new ArrayList();
        GAAttack result = null;
        int o_facing = en.getFacing();
        double front_la_dmg = 0;
        double front_ra_dmg = 0;
        double left_la_dmg = 0;
        double left_ra_dmg = 0;
        double right_la_dmg = 0;
        double right_ra_dmg = 0;
        PhysicalOption best_front_po = new PhysicalOption(en);
        PhysicalOption best_left_po = new PhysicalOption(en);
        PhysicalOption best_right_po = new PhysicalOption(en);


        // Get best physical attack
        for (Enumeration i = en.getWeapons(); i.hasMoreElements();) {
            Mounted mw = (Mounted) i.nextElement();

            // If this weapon is in the same arm as a
            // brush off attack skip to next weapon.
            c = this.calculateWeaponAttacks(en, mw, true);       

            // Get best physical attack
            best_front_po = PhysicalCalculator.getBestPhysical(en, game);

            if ((best_front_po != null) && (en instanceof Mech)) {

                // If this weapon is in the same arm as a brush off attack
                // skip to next weapon

                if (((best_front_po.type == PhysicalOption.BRUSH_LEFT) ||
                        (best_front_po.type == PhysicalOption.BRUSH_BOTH)) &&
                        (mw.getLocation() == Mech.LOC_LARM)) {
                    continue;
                }
                if (((best_front_po.type == PhysicalOption.BRUSH_RIGHT) ||
                        (best_front_po.type == PhysicalOption.BRUSH_BOTH)) &&
                        (mw.getLocation() == Mech.LOC_RARM)) {
                    continue;
                }

                // Total the damage of all weapons fired from each arm
                if (((best_front_po.type == PhysicalOption.PUNCH_LEFT) ||
                        (best_front_po.type == PhysicalOption.PUNCH_BOTH)) &&
                        (mw.getLocation() == Mech.LOC_LARM)) {
                    if (c.size() > 0) {
                        front_la_dmg += ((AttackOption) c.get(c.size() - 2)).value;
                    }
                }
                if (((best_front_po.type == PhysicalOption.PUNCH_RIGHT) ||
                        (best_front_po.type == PhysicalOption.PUNCH_BOTH)) &&
                        (mw.getLocation() == Mech.LOC_RARM)) {
                    if (c.size() > 0) {
                        front_ra_dmg += ((AttackOption) c.get(c.size() - 2)).value;
                    }
                }
                // If this weapon is a push attack and an arm mounted
                // weapon skip to next weapon

                if ((best_front_po.type == PhysicalOption.PUSH_ATTACK) &&
                        ((mw.getLocation() == Mech.LOC_LARM) ||
                        (mw.getLocation() == Mech.LOC_RARM))) {
                    continue;
                }
            }
            

            // If this weapon is in the same arm as a punch
            // attack, add the damage to the running total.
            if (c.size() > 0) {
                front.add(c);
                attacks[0] = Math.max(attacks[0], c.size());
            }
            if (!es.getFinalProne() && en.canChangeSecondaryFacing()) {
                en.setSecondaryFacing((o_facing + 5) % 6);
                c = this.calculateWeaponAttacks(en, mw, true);
                if (c.size() > 0) {
                    // Get best physical attack
                    best_left_po = PhysicalCalculator.getBestPhysical(en, game);
                    if ((best_left_po != null) && (en instanceof Mech)) {
                        if (((best_left_po.type == PhysicalOption.PUNCH_LEFT) ||
                                (best_left_po.type == PhysicalOption.PUNCH_BOTH)) &&
                                (mw.getLocation() == Mech.LOC_LARM)) {
                            left_la_dmg += ((AttackOption) c.get(c.size() - 2)).value;
                        }
                        if (((best_left_po.type == PhysicalOption.PUNCH_RIGHT) ||
                                (best_left_po.type == PhysicalOption.PUNCH_BOTH)) &&
                                (mw.getLocation() == Mech.LOC_RARM)) {
                            left_ra_dmg += ((AttackOption) c.get(c.size() - 2)).value;
                        }
                    }
                    left.add(c);
                    attacks[1] = Math.max(attacks[1], c.size());
                }
                en.setSecondaryFacing((o_facing + 1) % 6);
                c = this.calculateWeaponAttacks(en, mw, true);
                if (c.size() > 0) {
                    // Get best physical attack
                    best_right_po = PhysicalCalculator.getBestPhysical(en, game);
                    if ((best_right_po != null) && (en instanceof Mech)) {
                        if (((best_right_po.type == PhysicalOption.PUNCH_LEFT) ||
                                (best_right_po.type == PhysicalOption.PUNCH_BOTH)) &&
                                (mw.getLocation() == Mech.LOC_LARM)) {
                            right_la_dmg += ((AttackOption) c.get(c.size() - 2)).value;
                        }
                        if (((best_right_po.type == PhysicalOption.PUNCH_RIGHT) ||
                                (best_right_po.type == PhysicalOption.PUNCH_BOTH)) &&
                                (mw.getLocation() == Mech.LOC_RARM)) {
                            right_ra_dmg += ((AttackOption) c.get(c.size() - 2)).value;
                        }
                    }
                    right.add(c);
                    attacks[2] = Math.max(attacks[2], c.size());
                }
                en.setSecondaryFacing((o_facing + 3) % 6);
                c = this.calculateWeaponAttacks(en, mw, true);
                if (c.size() > 0) {
                    rear.add(c);
                    attacks[3] = Math.max(attacks[3], c.size());
                }
            } else {
                attacks[1] = 0;
                attacks[2] = 0;
            }
            en.setSecondaryFacing(o_facing);
        }

        fireOrPhysicalCheck(best_front_po, en, front, front_la_dmg, front_ra_dmg);

        ArrayList arcs = new ArrayList();
        arcs.add(front);
        if (!es.getFinalProne() && en.canChangeSecondaryFacing()) {
            fireOrPhysicalCheck(best_left_po, en, left, left_la_dmg, left_ra_dmg);
            arcs.add(left);
            fireOrPhysicalCheck(best_right_po, en, right, right_la_dmg, right_ra_dmg);
            arcs.add(right);
            // Meks and protos can't twist all the way around.
            if (!(en instanceof Mech)
                    && !(en instanceof Protomech)) {
                arcs.add(rear);
            }
        }
        for (int i = 0; i < arcs.size(); i++) {
            ArrayList v = (ArrayList) arcs.get(i);
            if (v.size() > 0) {
                GAAttack test =
                        new GAAttack(this,
                                centities.get(en),
                                v,
                                Math.max((v.size() + attacks[i]) * search_level, 20 * search_level),
                                30 * search_level,
                                en.isEnemyOf((Entity) getEntitiesOwned().get(0)));
                test.setFiringArc(i);
                test.evolve();
                if (target != null) {
                    if (result == null || test.getDamageUtility(target) > result.getDamageUtility(target)) {
                        result = test;
                    }
                } else if (
                        result == null || test.getFittestChromosomesFitness() > result.getFittestChromosomesFitness()) {
                    result = test;
                }
            }
        }
        return result;
    }

    /**
     * If the best attack is a punch, then check each
     * punch damage against the weapons damage from the
     * appropriate arm; if the punch does more damage,
     * drop the weapons in that arm to 0 expected damage
     * Repeat this for left and right twists
     *
     * @param best_po
     * @param entity
     * @param attackOptions
     * @param la_dmg
     * @param ra_dmg
     */
    private void fireOrPhysicalCheck(PhysicalOption best_po, Entity entity, ArrayList attackOptions, double la_dmg, double ra_dmg) {
        ArrayList c;
        if ((best_po != null) && (entity instanceof Mech)) {
            if (best_po.type == PhysicalOption.PUNCH_LEFT) {
                if ((la_dmg < best_po.expectedDmg) && (attackOptions.size() > 0)) {
                    for (int i = 0; i < attackOptions.size(); i++) {
                        c = (ArrayList) attackOptions.get(i);
                        for (int j = 0; j < c.size(); j++) {
                            if (((AttackOption) c.get(j)).weapon.getLocation() ==
                                    Mech.LOC_LARM) {
                                ((AttackOption) c.get(j)).expected = 0;
                                ((AttackOption) c.get(j)).primary_expected = 0;
                            }
                        }
                    }
                }
            }
            if (best_po.type == PhysicalOption.PUNCH_RIGHT) {
                if ((ra_dmg < best_po.expectedDmg) && (attackOptions.size() > 0)) {
                    for (int i = 0; i < attackOptions.size(); i++) {
                        c = (ArrayList) attackOptions.get(i);
                        for (int j = 0; j < c.size(); j++) {
                            if (((AttackOption) c.get(j)).weapon.getLocation() ==
                                    Mech.LOC_RARM) {
                                ((AttackOption) c.get(j)).expected = 0;
                                ((AttackOption) c.get(j)).primary_expected = 0;
                            }
                        }
                    }
                }
            }
            if (best_po.type == PhysicalOption.PUNCH_BOTH) {
                if (((la_dmg + ra_dmg) < best_po.expectedDmg) && (attackOptions.size() > 0)) {
                    for (int i = 0; i < attackOptions.size(); i++) {
                        c = (ArrayList) attackOptions.get(i);
                        for (int j = 0; j < c.size(); j++) {
                            if (((AttackOption) c.get(j)).weapon.getLocation() ==
                                    Mech.LOC_LARM) {
                                ((AttackOption) c.get(j)).expected = 0;
                                ((AttackOption) c.get(j)).primary_expected = 0;
                            }
                            if (((AttackOption) c.get(j)).weapon.getLocation() ==
                                    Mech.LOC_RARM) {
                                ((AttackOption) c.get(j)).expected = 0;
                                ((AttackOption) c.get(j)).primary_expected = 0;
                            }
                        }
                    }
                }
            }
        }
    }

    /* could use best of best strategy instead of expensive ga */
    public double attackUtility(MoveOption es, CEntity target) {
        GAAttack result = bestAttack(es, target, 1);
        if (result == null) {
            return 0;
        }
        return result.getFittestChromosomesFitness();
    }

    public void calculateFiringTurn() {
        int first_entity = game.getFirstEntityNum();
        int entity_num = first_entity;
        int best_entity = first_entity;
        int spin_mode = 0;
        double max = java.lang.Double.MIN_VALUE;
        int[] results = null;
        ArrayList winner = null;
        int arc = 0;
        WeaponType spinner;

        if (entity_num == -1) {
            return;
        }

        do {
            Entity en = game.getEntity(entity_num);
            CEntity cen = centities.get(en);

            GAAttack test = bestAttack(cen.current, null, 3);

            if (test != null && test.getFittestChromosomesFitness() > max) {
                max = test.getFittestChromosomesFitness();
                results = test.getResultChromosome();
                arc = test.getFiringArc();
                best_entity = entity_num;
                winner = test.getAttack();
            }
            entity_num = game.getNextEntityNum(entity_num);
        } while (entity_num != first_entity && entity_num != -1);

        java.util.Vector av = new java.util.Vector();
        //maximum already selected (or default)
        Entity en = game.getEntity(best_entity);
        if (results != null) {
            Entity primary_target = (Entity) game.getEntitiesVector().get(results[results.length - 1]);
            TreeMap tm = new TreeMap(new AttackOption.Sorter(centities.get(primary_target)));
            for (int i = 0; i < results.length - 1; i++) {
                AttackOption a = (AttackOption) ((ArrayList) winner.get(i)).get(results[i]);
                if (a.target != null) {
                    a.target.expected_damage[a.toHit.getSideTable()] += a.value;
                    a.target.hasTakenDamage = true;
                    tm.put(a, a);
                }
            }
            Iterator i = tm.values().iterator();
            while (i.hasNext()) {
                AttackOption a = (AttackOption) i.next();
                WeaponAttackAction new_attack = new WeaponAttackAction(en.getId(), a.target.getEntity().getId(), en.getEquipmentNum(a.weapon));
                if (en.getEquipment(new_attack.getWeaponId()).getLinked() != null) {
                    spinner = (WeaponType) a.weapon.getType();

// If this is an ultra-cannon or rotary cannon, try to spin it up

                    if ((spinner.getAmmoType() == AmmoType.T_AC_ULTRA)
                            || (spinner.getAmmoType() == AmmoType.T_AC_ULTRA_THB)
                            || (spinner.getAmmoType() == AmmoType.T_AC_ROTARY)) {
                        spin_mode = Compute.spinUpCannon(game, new_attack);
                        super.sendModeChange(en.getId(), en.getEquipmentNum(a.weapon), spin_mode);
                    }
                    Mounted cur_ammo = en.getEquipment(new_attack.getWeaponId()).getLinked();
                    new_attack.setAmmoId(en.getEquipmentNum(cur_ammo));
                    Compute.getAmmoAdjDamage(game, new_attack);

                }
                av.add(new_attack);

            }
        }
        switch (arc) {
            case 1:
                av.add(0, new TorsoTwistAction(en.getId(), (en.getFacing() + 5) % 6));
                break;
            case 2:
                av.add(0, new TorsoTwistAction(en.getId(), (en.getFacing() + 1) % 6));
                break;
            case 3:
                av.add(0, new TorsoTwistAction(en.getId(), (en.getFacing() + 3) % 6));
                break;
        }
        sendAttackData(best_entity, av);
    }

    /**
     * consider how to put more pre-turn logic here
     */
    protected void initMovement() {
        this.my_mechs_moved = 0;
        this.old_moves = null;
        this.enemies_moved = 0;
        double max_modifier = 1.4;
        ArrayList entities = new ArrayList(game.getEntitiesVector());
        double num_entities = Math.sqrt(entities.size()) / 100;
        ArrayList friends = new ArrayList();
        ArrayList foes = new ArrayList();
        double friend_sum = 0;
        double foe_sum = 0;
        double max_foe_bv = 0;
        CEntity max_foe = null;
        for (int i = 0; i < entities.size(); i++) {
            Entity entity = (Entity) entities.get(i);
            CEntity centity = centities.get(entity);
            centity.enemy_num = i;
            double old_value = centity.bv * (centity.overall_armor_percent + 1);
            centity.reset(); //should get fresh values
            double new_value = centity.bv * (centity.overall_armor_percent + 1);
            double percent = 1 + (new_value - old_value) / old_value;
            if (entity.getOwner().equals(getLocalPlayer())) {
                friends.add(centity);
                friend_sum += new_value;
                if (percent < .85) {
                    //small retreat
                    centity.strategy.attack = .85;
                } else if (percent < .95) {
                    centity.strategy.attack = 1;
                } else if (percent <= 1 && centity.strategy.attack < max_modifier) {
                    if (percent == 1) {
                        if (centity.strategy.attack < 1) {
                            centity.strategy.attack = Math.min(1.4 * centity.strategy.attack, 1);
                        } else {
                            centity.strategy.attack *= (1.0 + num_entities);
                        }
                    } else {
                        centity.strategy.attack *= (1.0 + 2 * num_entities);
                    }
                }
            } else if (!entity.getOwner().isEnemyOf(getLocalPlayer())) {
                friend_sum += new_value;
            } else {
                foes.add(centity);
                foe_sum += new_value;
                if (new_value > max_foe_bv) {
                    max_foe_bv = new_value;
                    max_foe = centity;
                }
                if (this.getEntitiesOwned().size() > 2) {
                    if (centity.strategy.target > 2) {
                        centity.strategy.target = 1 + .5 * (centity.strategy.target - 2);
                    }
                    if (percent < .85 && centity.strategy.target < max_modifier) {
                        centity.strategy.target *= (1.0 + 6 * num_entities);
                    } else if (percent < .95 && centity.strategy.target < max_modifier) {
                        centity.strategy.target *= (1.0 + 4 * num_entities);
                    } else if (percent <= 1) {
                        if (percent == 1) {
                            centity.strategy.target /= (1.0 + 2 * num_entities);
                        } else {
                            centity.strategy.target /= (1.0 + num_entities);
                        }
                    }
                    //don't go below one
                    if (centity.strategy.target < 1)
                        centity.strategy.target = 1;
                }
            }
        }
        System.out.println("Us " + friend_sum + " Them " + foe_sum);
        //do some more reasoning...
        if (this.unit_values.size() == 0) {
            this.unit_values.add(new Double(friend_sum));
            this.enemy_values.add(new Double(foe_sum));
            return;
        }
        Iterator i = foes.iterator();

        if (friends.size() > 1) {
            if (Strategy.MainTarget == null
                    || null == game.getEntity
                    (Strategy.MainTarget.getEntity().getId())) {
                Strategy.MainTarget = max_foe;
            }
            // TODO : Handle this better.
            if (null == Strategy.MainTarget)
                System.err.println
                        ("TestBot#initMovement() - no main target for bot");
            else if (null == Strategy.MainTarget.strategy)
                System.err.println
                        ("TestBot#initMovement() - no strategy for main target");
            else {
                Strategy.MainTarget.strategy.target += .2;
                while (i.hasNext()) {
                    CEntity centity = (CEntity) i.next();
                    // good turn, keep up the work, but randomize to reduce
                    // predictability
                    if (friend_sum - foe_sum
                            >= .9
                            * (((Double) this.unit_values.getLast()).doubleValue()
                            - ((Double) this.enemy_values.getLast()).doubleValue())) {
                        if (Compute.randomInt(2) == 1) {
                            centity.strategy.target += .3;
                        }
                        //lost that turn, but still in the fight, just get a
                        // little more aggressive
                    } else if (friend_sum > .9 * foe_sum) {
                        centity.strategy.target += .15;
                        //lost that turn and loosing
                    } else if (centity.strategy.target < 2) { //go for the gusto
                        centity.strategy.target += .3;
                    }
                    System.out.println(centity.getEntity().getShortName() + " " + centity.strategy.target);
                }
            }
        }

        double ratio = friend_sum / foe_sum;
        double mod = 1;
        if (ratio < .9) {
            mod = .95;
        } else if (ratio < 1) {
            //no change
        } else { //attack
            mod = (1.0 + num_entities);
        }
        i = friends.iterator();
        while (i.hasNext()) {
            CEntity centity = (CEntity) i.next();
            if (!(mod < 1 && centity.strategy.attack < .6) && !(mod > 1 && centity.strategy.attack >= max_modifier))
                centity.strategy.attack *= mod;
        }
        System.gc(); //just to make sure
    }

    protected void processChat(GamePlayerChatEvent ge) {
        chatp.processChat(ge, this);
    }

    // Where do I put my units?  This prioritizes hexes and facings
    protected void calculateDeployment() {

        int weapon_count;
        int hex_count, x_ave, y_ave, nDir;
        double av_range;

        Coords pointing_to = new Coords();
        Entity test_ent;
        Enumeration weapons, ammo_slots, valid_attackers, equips;

        int entNum = game.getFirstDeployableEntityNum();
        Coords[] cStart = getStartingCoordsArray();
        Coords cDeploy = getCoordsAround(getEntity(entNum), cStart);

        // Now that we have a location to deploy to, get a direction
        // Using average long range of deploying unit, point towards the largest cluster of enemies in range

        av_range = 0.0;
        weapon_count = 0;
        weapons = getEntity(entNum).getWeapons();
        while (weapons.hasMoreElements()) {
            Mounted mounted = (Mounted) weapons.nextElement();
            WeaponType wtype = (WeaponType) mounted.getType();
            if ((wtype.getName() != "ATM 3") && (wtype.getName() != "ATM 6") && (wtype.getName() != "ATM 9") && (wtype.getName() != "ATM 12")) {
                if (getEntity(entNum).getC3Master() != null) {
                    av_range += ((wtype.getLongRange()) * 1.25);
                } else {
                    av_range += wtype.getLongRange();
                }
                weapon_count = ++weapon_count;
            }
        }
        ammo_slots = getEntity(entNum).getAmmo();
        while (ammo_slots.hasMoreElements()) {
            Mounted mounted = (Mounted) ammo_slots.nextElement();
            AmmoType atype = (AmmoType) mounted.getType();
            if (atype.getAmmoType() == AmmoType.T_ATM) {
                weapon_count = ++weapon_count;
                av_range += 15.0;
                if ((atype.getAmmoType() == AmmoType.T_ATM)
                        && atype.getMunitionType() == AmmoType.M_HIGH_EXPLOSIVE) {
                    av_range -= 6;
                }
                if ((atype.getAmmoType() == AmmoType.T_ATM)
                        && atype.getMunitionType() == AmmoType.M_EXTENDED_RANGE) {
                    av_range += 12.0;
                }
            }
        }

        av_range = av_range / weapon_count;

        hex_count = 0;
        x_ave = 0;
        y_ave = 0;
        valid_attackers = game.getEntities();
        while (valid_attackers.hasMoreElements()) {
            test_ent = (Entity) valid_attackers.nextElement();
            if (test_ent.isDeployed()) {
                if (test_ent.isVisibleToEnemy()) {
                    if (cDeploy.distance(test_ent.getPosition()) <= (int) av_range) {
                        hex_count++;
                        x_ave += test_ent.getPosition().x;
                        y_ave += test_ent.getPosition().y;
                    }
                }
            }
        }
        if (hex_count != 0) {
            pointing_to = new Coords((x_ave / hex_count), (y_ave / hex_count));
        } else {
            pointing_to = new Coords(game.getBoard().getWidth() / 2, game.getBoard().getHeight() / 2);
        }
        nDir = cDeploy.direction(pointing_to);

        // If unit has stealth armor, turn it on
        if (getEntity(entNum) instanceof Mech &&
                getEntity(entNum).getArmorType() == EquipmentType.T_ARMOR_STEALTH) {
            for (equips = getEntity(entNum).getMisc(); equips.hasMoreElements();) {
                Mounted test_equip = (Mounted) equips.nextElement();
                MiscType test_type = (MiscType) test_equip.getType();
                if (Mech.STEALTH.equals(test_type.getInternalName())) {
                    if (test_equip.curMode().getName() != "On") {
                        test_equip.setMode("On");
                        super.sendModeChange(entNum, getEntity(entNum).getEquipmentNum(test_equip), 1);
                    }
                }
            }
        }

        Entity ce = game.getEntity(entNum);
        megamek.debug.Assert.assertTrue(!ce.isHexProhibited(game.getBoard().getHex(cDeploy)));
        deploy(entNum, cDeploy, nDir);
    }


    protected MovePath continueMovementFor(Entity entity) {
        return new MovePath(game, entity);
    }

    protected Vector calculateMinefieldDeployment() {
        Vector deployedMinefields = new Vector();

        deployMinefields(deployedMinefields, getLocalPlayer().getNbrMFConventional(), 0);
        deployMinefields(deployedMinefields, getLocalPlayer().getNbrMFCommand(), 1);
        deployMinefields(deployedMinefields, getLocalPlayer().getNbrMFVibra(), 2);

        return deployedMinefields;
    }

    protected Vector calculateArtyAutoHitHexes() {
        Vector artyAutoHitHexes = new Vector();
        artyAutoHitHexes.add(new Integer(this.getLocalPlayer().getId()));
        return artyAutoHitHexes;
    }

    protected void deployMinefields(Vector deployedMinefields, int number, int type) {
        for (int i = 0; i < number; i++) {
            Coords coords = new Coords(Compute.randomInt(game.getBoard().getWidth()),
                    Compute.randomInt(game.getBoard().getHeight()));

            if (game.containsMinefield(coords)) {
                Minefield mf = (Minefield) game.getMinefields(coords).get(0);
                if (mf.getPlayerId() == getLocalPlayer().getId()) {
                    i--;
                    continue;
                }
            } else {
                Minefield mf = null;

                if (type == 0) {
                    mf = Minefield.createConventionalMF(coords, getLocalPlayer().getId());
                } else if (type == 1) {
                    mf = Minefield.createCommandDetonatedMF(coords, getLocalPlayer().getId());
                } else if (type == 2) {
                    mf = Minefield.createVibrabombMF(coords, getLocalPlayer().getId(), 20);
                }
                deployedMinefields.add(mf);
            }
        }
    }
}
