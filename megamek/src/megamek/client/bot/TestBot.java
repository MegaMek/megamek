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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;
import java.util.Vector;

import megamek.client.bot.MoveOption.DamageInfo;
import megamek.common.AmmoType;
import megamek.common.BattleArmor;
import megamek.common.Compute;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.EntityMovementType;
import megamek.common.EquipmentType;
import megamek.common.IAimingModes;
import megamek.common.IHex;
import megamek.common.Infantry;
import megamek.common.Mech;
import megamek.common.Minefield;
import megamek.common.MiscType;
import megamek.common.Mounted;
import megamek.common.MovePath;
import megamek.common.MovePath.MoveStepType;
import megamek.common.Protomech;
import megamek.common.TargetRoll;
import megamek.common.Terrains;
import megamek.common.ToHitData;
import megamek.common.WeaponType;
import megamek.common.actions.ChargeAttackAction;
import megamek.common.actions.DfaAttackAction;
import megamek.common.actions.EntityAction;
import megamek.common.actions.TorsoTwistAction;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.containers.PlayerIDandList;
import megamek.common.event.GamePlayerChatEvent;
import megamek.common.options.OptionsConstants;

public class TestBot extends BotClient {

    public CEntity.Table centities = new CEntity.Table(this);
    protected ChatProcessor chatp = new ChatProcessor();
    protected int ignore = 10;
    boolean debug = false;
    private int enemies_moved = 0;
    private GALance old_moves = null;

    public TestBot(String name, String host, int port) {
        super(name, host, port);
        ignore = config.getIgnoreLevel();
        debug = config.isDebug();
    }

    @Override
    public void initialize() {
        // removed
    }

    @Override
    public PhysicalOption calculatePhysicalTurn() {
        return PhysicalCalculator.calculatePhysicalTurn(this);
    }

    /**
     * Used by the function calculateMoveTurn to run each entities movement
     * calculation in a separate thread.
     *
     * @author Mike Kiscaden
     */
    public class CalculateEntityMove implements Runnable {
        private Entity entity;
        private MoveOption[] result;

        CalculateEntityMove(Entity entity) {
            this.entity = entity;
        }

        public void run() {
            result = calculateMove(entity);
        }

        public Entity getEntity() {
            return entity;
        }

        public MoveOption[] getResult() {
            return result;
        }

    }

    @Override
    public MovePath calculateMoveTurn() {
        long enter = System.currentTimeMillis();
        int initiative = 0;
        MoveOption min = null;

        System.out.println("beginning movement calculations...");

        // first check and that someone else has moved so we don't replan
        Object[] enemy_array = getEnemyEntities().toArray();
        for (int j = 0; j < enemy_array.length; j++) {
            if (!((Entity) enemy_array[j]).isSelectableThisTurn()) {
                initiative++;
            }
        }
        // if nobody's moved and we have a valid move waiting, use that
        if ((initiative == enemies_moved) && (old_moves != null)) {
            min = old_moves.getResult();
            if ((min == null)
                || !min.isMoveLegal()
                || (min.isPhysical && centities.get(min
                                                            .getPhysicalTargetId()).isPhysicalTarget)) {
                old_moves = null;
                System.out
                        .println("recalculating moves since the old move was invalid");
                return calculateMoveTurn();
            }
        } else {
            enemies_moved = initiative;
            ArrayList<MoveOption[]> possible = new ArrayList<MoveOption[]>();

            for (Entity entity : game.getEntitiesVector()) {

                // ignore loaded and off-board units
                if ((entity.getPosition() == null) || entity.isOffBoard()) {
                    continue;
                }

                CEntity cen = centities.get(entity);
                cen.refresh();
                firstPass(cen);
            }

            Iterator<Entity> i = getEntitiesOwned().iterator();
            boolean short_circuit = false;

            List<Thread> threads = new ArrayList<Thread>();
            List<CalculateEntityMove> tasks = new ArrayList<CalculateEntityMove>();
            while (i.hasNext() && !short_circuit) {
                Entity entity = i.next();

                // ignore loaded units
                // (not really necessary unless bot manages to load units)
                if (entity.getPosition() == null) {
                    continue;
                }

                // if we can't move this entity right now, ignore it
                if (!game.getTurn().isValidEntity(entity, game)) {
                    continue;
                }

                CalculateEntityMove task = new CalculateEntityMove(entity);
                tasks.add(task);
                Thread worker = new Thread(task);
                worker.setName("Entity:" + entity.getId());
                worker.start();
                threads.add(worker);

            }
            int running = 0;
            synchronized (this) {
                do {
                    running = 0;
                    for (Thread thread : threads) {
                        if (thread.isAlive()) {
                            running++;
                        }
                    }
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e1) {
                        System.out
                                .println("Interrupted waiting for Bot to move.");
                        e1.printStackTrace();
                    } // Technically we should be using wait() but its not
                    // waking up reliably.
                    if (running > 0) {
                        sendChat("Calculating the move for " + running
                                 + " units. ");
                    } else {
                        sendChat("Finalizing move.");
                    }
                } while (running > 0);
            }
            // Threads are done running. Process the results.
            for (CalculateEntityMove task : tasks) {
                MoveOption[] result = task.getResult();
                CEntity cen = centities.get(task.getEntity());
                if (game.getOptions().booleanOption("skip_ineligable_movement")
                    && cen.getEntity().isImmobile()) {
                    cen.moved = true;
                } else if (result == null) {
                    short_circuit = true;
                } else if (!cen.moved) {
                    if (result.length < 6) {
                        min = result.length > 0 ? (MoveOption) result[0] : null;
                        short_circuit = true;
                    }
                    possible.add(result);
                }
            }

            // should ignore mechs that are not engaged
            // and only do the below when there are 2 or mechs left to move
            if (!short_circuit) {
                if ((getEntitiesOwned().size() > 1) && (possible.size() > 0)) {
                    GALance lance = new GALance(this, possible, 50, 80);
                    lance.evolve();
                    min = lance.getResult();
                    old_moves = lance;
                } else if ((possible.get(0) != null)
                           && (possible.get(0).length > 0)) {
                    min = possible.get(0)[0];
                }
            }
        }
        if (min == null) {
            min = new MoveOption(game, centities.get(getFirstEntityNum()));
        }
        for (Object element : enemy_array) {
            Entity en = (Entity) element;

            // ignore loaded units
            if (en.getPosition() == null) {
                continue;
            }

            CEntity enemy = centities.get(en);
            int enemy_hit_arc = CEntity.getThreatHitArc(
                    enemy.current.getFinalCoords(),
                    enemy.current.getFinalFacing(), min.getFinalCoords());
            MoveOption.DamageInfo di = min.damageInfos.get(enemy);
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
        min.getCEntity().moved = true;

        long exit = System.currentTimeMillis();
        System.out.println("move turn took " + (exit - enter) + " ms");

        // If this unit has a jammed RAC, and it has only walked,
        // add an unjam action
        if (min.getLastStep() != null) {
            if (min.getCEntity().entity.canUnjamRAC()) {
                if ((min.getLastStep().getMovementType(true) == EntityMovementType.MOVE_WALK)
                    || (min.getLastStep().getMovementType(true) == EntityMovementType.MOVE_VTOL_WALK)
                    || (min.getLastStep().getMovementType(true) == EntityMovementType.MOVE_NONE)) {
                    // Cycle through all available weapons, only unjam if the
                    // jam(med)
                    // RACs count for a significant portion of possible damage
                    int rac_damage = 0;
                    int other_damage = 0;
                    int clearance_range = 0;
                    for (Mounted equip : min.getCEntity().entity
                            .getWeaponList()) {
                        WeaponType test_weapon = new WeaponType();

                        test_weapon = (WeaponType) equip.getType();
                        if (((test_weapon.getAmmoType() == AmmoType.T_AC_ROTARY)
                             || (game.getOptions().booleanOption("uac_tworolls")
                                 && ((test_weapon.getAmmoType() == AmmoType.T_AC_ULTRA)
                                     || (test_weapon.getAmmoType() == AmmoType.T_AC_ULTRA_THB))))
                            && (equip.isJammed() == true)) {
                            rac_damage = rac_damage + (4 * (test_weapon.getDamage()));
                        } else {
                            if (equip.canFire()) {
                                other_damage += test_weapon.getDamage();
                                if (test_weapon.getMediumRange() > clearance_range) {
                                    clearance_range = test_weapon.getMediumRange();
                                }
                            }
                        }
                    }
                    // Even if the jammed RAC doesn't make up a significant
                    // portion
                    // of the units damage, its still better to have it
                    // functional
                    // If nothing is "close" then unjam anyways
                    int check_range = 100;
                    for (Entity enemy : game.getEntitiesVector()) {
                        if ((min.getCEntity().entity.getPosition() != null)
                            && (enemy.getPosition() != null)
                            && (enemy.isEnemyOf(min.getCEntity().entity))) {
                            if (enemy.isVisibleToEnemy()) {
                                if (min.getCEntity().entity.getPosition()
                                                           .distance(enemy.getPosition()) < check_range) {
                                    check_range = min.getCEntity().entity
                                            .getPosition().distance(
                                                    enemy.getPosition());
                                }
                            }
                        }
                    }
                    if ((rac_damage >= other_damage)
                        || (check_range < clearance_range)) {
                        min.addStep(MoveStepType.UNJAM_RAC);
                    }
                }
            }
        }

        return min;
    }

    public MoveOption[] calculateMove(Entity entity) {
        List<Entity> enemy_array = myEnemies(entity);
        ArrayList<Entity> entities = new ArrayList<Entity>(
                game.getEntitiesVector());
        CEntity self = centities.get(entity);
        MoveOption[] move_array;
        int friends = entities.size() - enemy_array.size();

        move_array = secondPass(self, friends, enemy_array, entities);
        // top balanced
        filterMoves(move_array, self.pass, new MoveOption.WeightedComparator(1,
                                                                             1), 50);
        // top damage
        filterMoves(move_array, self.pass, new MoveOption.WeightedComparator(
                .5, 1), 50);

        move_array = thirdPass(self, enemy_array);

        // top balanced
        filterMoves(move_array, self.pass, new MoveOption.WeightedComparator(1,
                                                                             1), 30);
        // top damage
        filterMoves(move_array, self.pass, new MoveOption.WeightedComparator(
                .5, 1), 30);

        // reduce self threat, and add bonus for terrain
        for (MoveOption option : self.pass.values()) {
            option.setState();
            option.self_damage *= .5;
            option.self_threat *= .5;
            // TODO: should scale to the unit bv
            double terrain = 2 * ((double) Compute.getTargetTerrainModifier(
                    game, option.getEntity()).getValue());
            if (debug) {
                option.tv.add(terrain + " Terrain Adjusment " + "\n");
            }
            option.self_threat -= terrain;
        }

        move_array = fourthPass(self, enemy_array);
        // top balanced
        filterMoves(move_array, self.pass, new MoveOption.WeightedComparator(1,
                                                                             1), 20);
        // top damage
        filterMoves(move_array, self.pass, new MoveOption.WeightedComparator(
                .5, 1), 20);

        // reduce transient damage estimates
        for (MoveOption option : self.pass.values()) {
            option.self_threat *= .5;
            option.self_damage *= .5;
        }

        move_array = fifthPass(self, enemy_array);

        /*******************************************************************************************
         * Return top twenty moves to the lance algorithm
         ******************************************************************************************/
        MoveOption[] result = new MoveOption[Math.min(move_array.length, 20)];
        int offset = 0;
        for (int i = 0; i < Math.min(move_array.length, 20); i++) {
            MoveOption next = move_array[i];
            if (next.isPhysical
                && (self.range_damages[CEntity.RANGE_SHORT] > 5)
                && next.doomed) {
                if ((offset + 20) < move_array.length) {
                    next = move_array[offset + 20];
                    offset++;
                }
            }
            result[i] = next;
        }
        return result;
    }

    private List<Entity> myEnemies(Entity me) {
        List<Entity> possibles = game.getValidTargets(me);
        List<Entity> retVal = new ArrayList<Entity>();
        for (Entity ent : possibles) {
            if (ent.isEnemyOf(me)) {
                retVal.add(ent);
            }
        }
        return retVal;
    }

    /**
     * ************************************************************************
     * first pass, filter moves based upon present case
     * ************************************************************************
     */
    private void firstPass(CEntity self) {
        List<Entity> enemies = getEnemyEntities();
        MoveOption[] move_array;
        if (self.getEntity().isSelectableThisTurn() && !self.moved) {
            move_array = self.getAllMoves(this).values()
                             .toArray(new MoveOption[0]);
        } else {
            move_array = new MoveOption[]{self.current};
        }
        System.out.println(self.getEntity().getShortName() + " has "
                           + move_array.length + " moves");
        for (MoveOption option : move_array) {
            option.setState();
            boolean aptPiloting = option.getEntity().getCrew().getOptions()
                                        .booleanOption(OptionsConstants.PILOT_APTITUDE_PILOTING);
            for (int e = 0; e < enemies.size(); e++) { // for each enemy
                Entity en = enemies.get(e);

                // ignore loaded units
                if (en.getPosition() == null) {
                    continue;
                }

                CEntity enemy = centities.get(en);
                int[] modifiers = option.getModifiers(enemy.getEntity());
                if ((modifiers[MoveOption.DEFENCE_MOD] == TargetRoll.IMPOSSIBLE)
                    && (modifiers[MoveOption.ATTACK_MOD] == TargetRoll.IMPOSSIBLE)) {
                    continue;
                }
                int enemy_hit_arc = CEntity
                        .getThreatHitArc(enemy.current.getFinalCoords(),
                                         enemy.current.getFinalFacing(),
                                         option.getFinalCoords());
                int self_hit_arc = CEntity.getThreatHitArc(
                        option.getFinalCoords(), option.getFinalFacing(),
                        enemy.current.getFinalCoords());
                if (!enemy.getEntity().isImmobile()
                    && (modifiers[MoveOption.DEFENCE_MOD] != TargetRoll.IMPOSSIBLE)) {
                    self.engaged = true;
                    int mod = modifiers[MoveOption.DEFENCE_MOD];
                    double max = option.getMaxModifiedDamage(enemy.current,
                                                             mod, modifiers[MoveOption.DEFENCE_PC]);
                    if (en.isSelectableThisTurn()) {
                        enemy.current.addStep(MoveStepType.TURN_RIGHT);
                        max = Math.max(option.getMaxModifiedDamage(
                                enemy.current, mod + 1,
                                modifiers[MoveOption.DEFENCE_PC]), max);
                        enemy.current.removeLastStep();
                        enemy.current.addStep(MoveStepType.TURN_LEFT);
                        max = Math.max(option.getMaxModifiedDamage(
                                enemy.current, mod + 1,
                                modifiers[MoveOption.DEFENCE_PC]), max);
                        // return to original facing
                        enemy.current.removeLastStep();
                    }
                    max = self.getThreatUtility(max, self_hit_arc);
                    if (enemy.getEntity().isProne()) {
                        max *= enemy.base_psr_odds;
                    }
                    MoveOption.DamageInfo di = option
                            .getDamageInfo(enemy, true);
                    di.threat = max;
                    di.max_threat = max;
                    option.threat += max;
                    if (debug) {
                        option.tv.add(max + " Threat " + e + "\n");
                    }
                }
                /*
                 * As a first approximation, take the maximum to a single target
                 */
                if (!option.isPhysical) {
                    if (modifiers[MoveOption.ATTACK_MOD] != TargetRoll.IMPOSSIBLE) {
                        self.engaged = true;
                        double max = enemy.current.getMaxModifiedDamage(option,
                                                                        modifiers[0], modifiers[MoveOption.ATTACK_PC]);
                        max = enemy.getThreatUtility(max, enemy_hit_arc);
                        MoveOption.DamageInfo di = option.getDamageInfo(enemy,
                                                                        true);
                        di.damage = max;
                        di.min_damage = max;
                        if (debug) {
                            option.tv.add(max + " Damage " + e + "\n");
                        }
                        option.damage = Math.max(max, option.damage);
                    }
                } else {
                    CEntity target = centities
                            .get(option.getPhysicalTargetId());
                    try {
                        if (target.getEntity().getId() == enemy.getEntity()
                                                               .getId()) {
                            if (!target.isPhysicalTarget) {
                                ToHitData toHit = null;
                                double self_threat = 0;
                                double damage = 0;
                                if (option.isJumping()
                                    && option.getEntity().canDFA()) {
                                    self.current.setState();
                                    toHit = DfaAttackAction.toHit(game, option
                                            .getEntity().getId(), target
                                                                          .getEntity(), option);
                                    damage = 2 * DfaAttackAction
                                            .getDamageFor(
                                                    option.getEntity(),
                                                    (target.getEntity() instanceof Infantry)
                                                    && !(target
                                                            .getEntity() instanceof BattleArmor)
                                                         );
                                    self_threat = (option
                                                           .getCEntity()
                                                           .getThreatUtility(
                                                                   DfaAttackAction.getDamageTakenBy(option
                                                                                                            .getEntity()),
                                                                   ToHitData.SIDE_REAR
                                                                            ) * Compute
                                            .oddsAbove(toHit.getValue(), aptPiloting)) / 100;
                                    self_threat += option.getCEntity()
                                                         .getThreatUtility(
                                                                 .1 * self.getEntity()
                                                                          .getWeight(),
                                                                 ToHitData.SIDE_REAR
                                                                          );
                                    self_threat *= 100 / option.getCEntity()
                                                               .getEntity().getWeight();
                                } else if (option.getEntity().canCharge()) {
                                    self.current.setState();
                                    toHit = new ChargeAttackAction(
                                            option.getEntity(),
                                            target.getEntity()).toHit(game,
                                                                      option);
                                    damage = ChargeAttackAction.getDamageFor(
                                            option.getEntity(),
                                            target.getEntity(), false,
                                            option.getHexesMoved());
                                    self_threat = option
                                                          .getCEntity()
                                                          .getThreatUtility(
                                                                  ChargeAttackAction
                                                                          .getDamageTakenBy(
                                                                                  option.getEntity(),
                                                                                  target.getEntity()),
                                                                  ToHitData.SIDE_FRONT
                                                                           )
                                                  * (Compute.oddsAbove(toHit.getValue(), aptPiloting) / 100);
                                    option.setState();
                                } else {
                                    toHit = new ToHitData(
                                            TargetRoll.IMPOSSIBLE, "");
                                }
                                damage = (target.getThreatUtility(damage,
                                                                  toHit.getSideTable()) * Compute.oddsAbove(toHit.getValue(), aptPiloting)) / 100;
                                // charging is a good tactic against larger
                                // mechs
                                if (!option.isJumping()) {
                                    damage *= Math.sqrt((double) enemy.bv
                                                        / (double) self.bv);
                                }
                                // these are always risky, just don't on 11 or
                                // 12
                                if (toHit.getValue() > 10) {
                                    damage = 0;
                                }
                                // 7 or less is good
                                if (toHit.getValue() < 8) {
                                    damage *= 1.5;
                                }
                                // this is all you are good for
                                if (self.range_damages[CEntity.RANGE_SHORT] < 5) {
                                    damage *= 2;
                                }
                                MoveOption.DamageInfo di = option
                                        .getDamageInfo(enemy, true);
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
            } // -- end while of each enemy
            self.current.setState();
        } // -- end while of first pass
        // top balanced
        filterMoves(move_array, self.pass, new MoveOption.WeightedComparator(1,
                                                                             1), 100);
        // top damage
        filterMoves(move_array, self.pass, new MoveOption.WeightedComparator(
                .5, 1), 100);
    }

    /**
     * ********************************************************************
     * Second pass, combination moves/firing based only on the present case,
     * since only one mech moves at a time
     * ********************************************************************
     */
    private MoveOption[] secondPass(CEntity self, int friends,
                                    List<Entity> enemy_array, ArrayList<Entity> entities) {
        MoveOption[] move_array = self.pass.values().toArray(new MoveOption[0]);
        self.pass.clear();
        for (int j = 0; (j < move_array.length) && (friends > 2); j++) {
            MoveOption option = move_array[j];
            for (int e = 0; e < enemy_array.size(); e++) {
                Entity en = enemy_array.get(e);
                CEntity enemy = centities.get(en);
                for (Entity other : entities) {
                    if (other.isEnemyOf(self.entity)) {
                        continue;
                    }
                    MoveOption foption = centities.get(other).current;
                    double threat_divisor = 1;
                    MoveOption.DamageInfo di = option
                            .getDamageInfo(enemy, true);
                    if (foption.getDamageInfo(enemy, false) != null) {
                        option.damage += (enemy.canMove() ? .1 : .2)
                                         * di.damage;
                        threat_divisor += foption.getCEntity().canMove() ? .4
                                                                         : .6;
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
     * third pass, (not so bad) oppurtunistic planner gives preference to good
     * ranges/defensive positions based upon the mech characterization
     * ********************************************************************
     */
    private MoveOption[] thirdPass(CEntity self, List<Entity> enemy_array) {
        MoveOption[] move_array = self.pass.values().toArray(new MoveOption[0]);
        self.pass.clear();

        for (MoveOption option : move_array) {
            option.setState();
            double adjustment = 0;
            double temp_adjustment = 0;
            for (int e = 0; e < enemy_array.size(); e++) { // for each enemy
                Entity en = enemy_array.get(e);
                CEntity enemy = centities.get(en);
                int current_range = self.current.getFinalCoords().distance(
                        enemy.current.getFinalCoords());
                int range = option.getFinalCoords().distance(
                        enemy.current.getFinalCoords());
                if (range > self.long_range) {
                    temp_adjustment += (!(range < enemy.long_range) ? .5 : 1)
                                       * (1 + self.range_damages[self.range])
                                       * (Math.max(
                            range
                            - self.long_range
                            - (.5 * Math.max(self.jumpMP,
                                             .8 * self.runMP)), 0
                                                  ));
                }
                if (((self.range == CEntity.RANGE_SHORT) && ((current_range > 5) || (range > 9)))
                    || ((self.range_damages[CEntity.RANGE_SHORT] < 4) && (current_range > 10))) {
                    temp_adjustment += ((enemy.range > CEntity.RANGE_SHORT) ? .5
                                                                            : 1)
                                       * (Math.max(
                            1 + self.range_damages[CEntity.RANGE_SHORT],
                            5))
                                       * Math.max(
                            range
                            - (.5 * Math.max(self.jumpMP,
                                             .8 * self.runMP)), 0
                                                 );
                } else if (self.range == CEntity.RANGE_MEDIUM) {
                    temp_adjustment += (((current_range < 6) || (current_range > 12)) ? 1
                                                                                      : .25)
                                       * ((enemy.range > CEntity.RANGE_SHORT) ? .5 : 1)
                                       * (1 + self.range_damages[CEntity.RANGE_MEDIUM])
                                       * Math.abs(range
                                                  - (.5 * Math.max(self.jumpMP,
                                                                   .8 * self.runMP)));
                } else if (option.damage < (.25 * self.range_damages[CEntity.RANGE_LONG])) {
                    temp_adjustment += ((range < 10) ? .25 : 1)
                                       * (Math.max(
                            1 + self.range_damages[CEntity.RANGE_LONG],
                            3)) * (1 / (1 + option.threat));
                }
                adjustment += Math.sqrt((temp_adjustment * enemy.bv) / self.bv);
                // I would always like to face the opponent
                if (!(enemy.getEntity().isProne() || enemy.getEntity()
                                                          .isImmobile())
                    && (CEntity.getThreatHitArc(option.getFinalCoords(),
                                                option.getFinalFacing(), enemy.getEntity()
                                                                              .getPosition()
                                               ) != ToHitData.SIDE_FRONT)) {
                    int fa = CEntity.getFiringAngle(option.getFinalCoords(),
                                                    option.getFinalFacing(), enemy.getEntity()
                                                                                  .getPosition()
                                                   );
                    if ((fa > 90) && (fa < 270)) {
                        int distance = option.getFinalCoords().distance(
                                enemy.current.getFinalCoords());
                        double mod = 1;
                        if ((fa > 130) && (fa < 240)) {
                            mod = 2;
                        }
                        // big formula that says don't do it
                        mod *= (((Math.max(self.jumpMP, .8 * self.runMP) < 5) ? 2
                                                                              : 1)
                                * ((double) self.bv / (double) 50) * Math
                                .sqrt(((double) self.bv) / enemy.bv))
                               / (((double) distance / 6) + 1);
                        option.self_threat += mod;
                        if (debug) {
                            option.tv.add(mod + " " + fa + " Back to enemy\n");
                        }
                    }
                }
            }
            adjustment *= (self.overall_armor_percent * self.strategy.attack)
                          / enemy_array.size();
            // fix for hiding in level 2 water
            // To a greedy bot, it always seems nice to stay in here...
            IHex h = game.getBoard().getHex(option.getFinalCoords());
            if (h.containsTerrain(Terrains.WATER)
                && (h.surface() > (self.getEntity().getElevation() + ((option
                    .getFinalProne()) ? 0 : 1)))) {
                double mod = ((self.getEntity().heat + option
                        .getMovementheatBuildup()) <= 7) ? 100 : 30;
                adjustment += self.bv / mod;
            }
            // add them in now, then re-add them later
            if (self.range > CEntity.RANGE_SHORT) {
                int ele_dif = game.getBoard().getHex(option.getFinalCoords())
                                  .getLevel()
                              - game.getBoard().getHex(self.current.getFinalCoords())
                                    .getLevel();
                adjustment -= (Math.max(ele_dif, 0) + 1)
                              * ((double) Compute.getTargetTerrainModifier(game,
                                                                           option.getEntity()).getValue() + 1);
            }

            // close the range if nothing else and healthy
            if ((option.damage < (.25 * self.range_damages[self.range]))
                && (adjustment < self.range_damages[self.range])) {
                for (int e = 0; e < enemy_array.size(); e++) {
                    Entity en = enemy_array.get(e);
                    CEntity enemy = centities.get(en);
                    int range = option.getFinalCoords().distance(
                            enemy.current.getFinalCoords());
                    if (range > 5) {
                        adjustment += (Math.pow(self.overall_armor_percent, 2) * Math
                                .sqrt(((double) (range - 4) * enemy.bv)
                                      / self.bv))
                                      / enemy_array.size();
                    }
                }
            }

            if (option.damage < (.25 * (1 + self.range_damages[self.range]))) {
                option.self_threat += 2 * adjustment;
            } else if (option.damage < (.5 * (1 + self.range_damages[self.range]))) {
                option.self_threat += adjustment;
            }
            if (debug) {
                option.tv.add(option.self_threat
                              + " Initial Damage Adjustment " + "\n");
            }
        }

        return move_array;
    }

    // pass should contains 30 ~ 60

    /**
     * ********************************************************************
     * fourth pass, speculation on top moves use averaging to filter
     * ********************************************************************
     */
    private MoveOption[] fourthPass(CEntity self, List<Entity> enemy_array) {
        MoveOption[] move_array = self.pass.values().toArray(new MoveOption[0]);
        self.pass.clear();
        for (int e = 0; e < enemy_array.size(); e++) { // for each enemy
            Entity en = enemy_array.get(e);
            CEntity enemy = centities.get(en);
            // engage in speculation on "best choices" when you loose iniative
            if (enemy.canMove()) {
                ArrayList<MoveOption> enemy_move_array = enemy.pass.getArray();
                ArrayList<MoveOption> to_check = new ArrayList<MoveOption>();
                // check some enemy moves
                for (MoveOption element : move_array) {
                    MoveOption option = null;
                    to_check.clear();
                    option = element;
                    option.setState();
                    // check for damning hexes specifically
                    // could also look at intervening defensive
                    ArrayList<Coords> coord = new ArrayList<Coords>();
                    Coords back = option.getFinalCoords().translated(
                            (option.getFinalFacing() + 3) % 6);
                    coord.add(back);
                    coord.add(back.translated((option.getFinalFacing() + 2) % 6));
                    coord.add(back.translated((option.getFinalFacing() + 4) % 6));
                    coord.add(option.getFinalCoords().translated(
                            (option.getFinalFacing())));
                    coord.add(option.getFinalCoords().translated(
                            (option.getFinalFacing() + 1) % 6));
                    coord.add(option.getFinalCoords().translated(
                            (option.getFinalFacing() + 2) % 6));
                    coord.add(option.getFinalCoords().translated(
                            (option.getFinalFacing() + 4) % 6));
                    coord.add(option.getFinalCoords().translated(
                            (option.getFinalFacing() + 5) % 6));
                    Iterator<Coords> ci = coord.iterator();
                    while (ci.hasNext()) {
                        Coords test = ci.next();
                        List<MoveOption> c = enemy.findMoves(test, this);
                        if (c.size() != 0) {
                            to_check.addAll(c);
                        }
                    }
                    int range = option.getFinalCoords().distance(
                            enemy.current.getFinalCoords());
                    int compare = 0;
                    if ((enemy.long_range) > (range - Math.max(enemy.jumpMP,
                                                               enemy.runMP))) {
                        compare = 30;
                    } else if (enemy.long_range > range) {
                        compare = 10;
                    }
                    double mod = enemies_moved / getEnemyEntities().size();
                    compare *= (1 + mod);
                    for (int k = 0; (k <= compare)
                                    && (k < enemy_move_array.size()); k++) {
                        if (enemy_move_array.size() < compare) {
                            to_check.add(enemy_move_array.get(k));
                        } else {
                            int value = Compute.randomInt(enemy_move_array
                                                                  .size());
                            if ((value % 2) == 1) {
                                to_check.add(enemy_move_array.get(value));
                            } else {
                                to_check.add(enemy_move_array.get(k));
                            }
                        }
                    }
                    Iterator<MoveOption> eo = to_check.iterator();
                    while (eo.hasNext()) {
                        MoveOption enemy_option = eo.next();
                        double max_threat = 0;
                        double max_damage = 0;
                        enemy_option.setState();
                        int enemy_hit_arc = CEntity.getThreatHitArc(
                                enemy_option.getFinalCoords(),
                                enemy_option.getFinalFacing(),
                                option.getFinalCoords());
                        int self_hit_arc = CEntity.getThreatHitArc(
                                enemy_option.getFinalCoords(),
                                enemy_option.getFinalFacing(),
                                option.getFinalCoords());
                        if (enemy_option.isJumping()) {
                            enemy_hit_arc = Compute.ARC_FORWARD;
                        }
                        int[] modifiers = option.getModifiers(enemy_option
                                                                      .getEntity());
                        if (modifiers[1] != TargetRoll.IMPOSSIBLE) {
                            self.engaged = true;
                            if (!enemy_option.isJumping()) {
                                max_threat = option.getMaxModifiedDamage(
                                        enemy_option, modifiers[1],
                                        modifiers[MoveOption.DEFENCE_PC]);
                            } else {
                                boolean enemyAptGunnery = enemy.getEntity().getCrew().getOptions()
                                                               .booleanOption(OptionsConstants.PILOT_APTITUDE_GUNNERY);
                                max_threat = .8 * enemy
                                        .getModifiedDamage(
                                                (modifiers[MoveOption.DEFENCE_PC] == 1) ? CEntity.TT
                                                                                        : ToHitData.SIDE_FRONT,
                                                enemy_option
                                                        .getFinalCoords()
                                                        .distance(
                                                                option.getFinalCoords()),
                                                modifiers[1], enemyAptGunnery);
                            }
                            max_threat = self.getThreatUtility(max_threat,
                                                               self_hit_arc);
                        }
                        if (modifiers[0] != TargetRoll.IMPOSSIBLE) {
                            self.engaged = true;
                            max_damage = enemy_option.getMaxModifiedDamage(
                                    option, modifiers[0],
                                    modifiers[MoveOption.ATTACK_PC]);
                            max_damage = enemy.getThreatUtility(max_damage,
                                                                enemy_hit_arc);
                            if (option.isPhysical) {
                                if (centities.get(option.getPhysicalTargetId())
                                             .getEntity().getId() == enemy
                                            .getEntity().getId()) {
                                    max_damage = option.getDamage(enemy);
                                } else {
                                    max_damage = 0;
                                }
                            }
                        }
                        MoveOption.DamageInfo di = option.getDamageInfo(enemy,
                                                                        true);
                        di.max_threat = Math.max(max_threat, di.max_threat);
                        di.min_damage = Math.min(di.min_damage, max_damage);
                        if ((max_threat - max_damage) > (di.threat - di.damage)) {
                            di.threat = max_threat;
                            di.damage = max_damage;
                            if (debug) {
                                option.tv.add(max_threat + " Spec Threat " + e
                                              + "\n");
                                option.tv.add(max_damage + " Spec Damage " + e
                                              + "\n");
                            }
                        }
                    }
                    // update estimates
                    option.damage = 0;
                    option.threat = 0;
                    for (CEntity cen : option.damageInfos.keySet()) {
                        // rescale
                        MoveOption.DamageInfo di = option.getDamageInfo(cen,
                                                                        true);
                        di.min_damage /= cen.strategy.target;
                        di.damage /= cen.strategy.target;
                        option.damage += (di.min_damage + di.damage) / 2;

                        // my threat is average of absolute worst, and expected
                        option.threat = Math.max(option.threat, di.max_threat
                                                                + di.threat) / 2;
                        di.threat = (di.max_threat + (2 * di.threat)) / 3;
                    }
                }
                // restore enemy
                enemy.current.setState();
            }
            self.current.setState();
        } // --end move speculation
        return move_array;
    }

    // pass should now be 20 ~ 40

    /**
     * ********************************************************************
     * fifth pass, final damage and threat approximation --prevents moves that
     * from the previous pass would cause the mech to die
     * ********************************************************************
     */
    private MoveOption[] fifthPass(CEntity self, List<Entity> enemy_array) {
        MoveOption[] move_array = self.pass.values().toArray(new MoveOption[0]);
        self.pass.clear();

        if (self.engaged) {
            for (MoveOption option : move_array) {
                option.setState();
                GAAttack temp = this.bestAttack(option);
                if (temp != null) {
                    option.damage = (option.damage + temp
                            .getFittestChromosomesFitness()) / 2;
                } else {
                    option.damage /= 2;
                }
                for (int e = 0; e < enemy_array.size(); e++) { // for each
                    // enemy
                    Entity en = enemy_array.get(e);
                    CEntity enemy = centities.get(en);
                    if (!enemy.canMove()) {
                        option.setThreat(
                                enemy,
                                (option.getThreat(enemy) + attackUtility(
                                        enemy.current, self)) / 2
                                        );
                        if (debug) {
                            option.tv.add(option.getThreat(enemy)
                                          + " Revised Threat " + e + " \n");
                        }
                        if (!option.isPhysical) {
                            if (temp != null) {
                                option.setDamage(enemy, (option
                                                                 .getDamage(enemy) + temp
                                                                 .getDamageUtility(enemy)) / 2);
                            } else {
                                // probably zero, but just in case
                                option.setDamage(enemy,
                                                 option.getMinDamage(enemy));
                            }
                            if (debug) {
                                option.tv.add(option.getDamage(enemy)
                                              + " Revised Damage " + e + " \n");
                            }
                            // this needs to be reworked
                            if (option.getFinalCoords().distance(
                                    enemy.current.getFinalCoords()) == 1) {
                                PhysicalOption p = PhysicalCalculator
                                        .getBestPhysicalAttack(
                                                option.getEntity(),
                                                enemy.getEntity(), game);
                                if (p != null) {
                                    option.setDamage(enemy,
                                                     option.getDamage(enemy)
                                                     + p.expectedDmg
                                                    );
                                    if (debug) {
                                        option.tv.add(p.expectedDmg
                                                      + " Physical Damage " + e
                                                      + " \n");
                                    }
                                }
                                p = PhysicalCalculator.getBestPhysicalAttack(
                                        enemy.getEntity(), option.getEntity(),
                                        game);
                                if (p != null) {
                                    option.setThreat(enemy,
                                                     option.getThreat(enemy)
                                                     + (.5 * p.expectedDmg)
                                                    );
                                    if (debug) {
                                        option.tv.add((.5 * p.expectedDmg)
                                                      + " Physical Threat " + e
                                                      + " \n");
                                    }
                                }
                            }
                        }
                    } else if (!option.isPhysical) { // enemy can move (not
                        if (temp != null) {
                            option.setDamage(enemy, ((2 * option
                                    .getDamage(enemy)) + temp
                                                             .getDamageUtility(enemy)) / 3);
                        } else {
                            option.setDamage(enemy, option.getMinDamage(enemy));
                        }
                    } else {
                        // get a more accurate estimate
                        option.setDamage(
                                enemy,
                                option.getDamage(enemy)
                                / Math.sqrt((double) enemy.bv
                                            / (double) self.bv)
                                        );
                        option.damage = option.getDamage(enemy);
                    }
                }
                option.threat = 0;
                for (DamageInfo damageInfo : option.damageInfos.values()) {
                    option.threat += damageInfo.threat;
                }
                if (debug) {
                    option.tv.add(option.threat + " Revised Threat Utility\n");
                    option.tv.add(option.damage + " Revised Damage Utility\n");
                }
            }
        }
        Arrays.<MoveOption>sort(move_array, new MoveOption.WeightedComparator(
                1, 1));
        self.current.setState();

        return move_array;
    }

    private void filterMoves(MoveOption[] move_array, MoveOption.Table pass,
                             MoveOption.WeightedComparator comp, int filter) {
        Arrays.sort(move_array, comp);

        // top 100 utility, mostly conservative
        for (int i = 0; (i < filter) && (i < move_array.length); i++) {
            pass.put(move_array[i]);
        }
    }

    @Override
    protected void initFiring() {
        ArrayList<Entity> entities = new ArrayList<Entity>(
                game.getEntitiesVector());
        for (int i = 0; i < entities.size(); i++) {
            Entity entity = entities.get(i);
            CEntity centity = centities.get(entity);
            centity.reset();
            centity.enemy_num = i;
        }
        for (Entity entity : getEnemyEntities()) {
            CEntity centity = centities.get(entity);
            if (entity.isMakingDfa() || entity.isCharging()) {
                // try to prevent a physical attack from happening
                // but should take into account the toHit of the attack
                centity.strategy.target = 2.5;
            }
        }
    }

    protected ArrayList<AttackOption> calculateWeaponAttacks(Entity en,
                                                             Mounted mw, boolean best_only) {
        int from = en.getId();
        int weaponID = en.getEquipmentNum(mw);
        int spin_mode = 0;
        int starg_mod;
        ArrayList<AttackOption> result = new ArrayList<AttackOption>();
        List<Entity> ents = myEnemies(en);
        WeaponAttackAction wep_test;
        WeaponType spinner;
        AttackOption a = null;
        AttackOption max = new AttackOption(null, null, 0, null, 1, en.getCrew().getOptions()
                                                                      .booleanOption(
                                                                              OptionsConstants.PILOT_APTITUDE_GUNNERY));
        for (Entity e : ents) {
            CEntity enemy = centities.get(e);
            // long entry = System.currentTimeMillis();
            ToHitData th = WeaponAttackAction.toHit(game, from, e, weaponID, false);
            // long exit = System.currentTimeMillis();
            // if (exit != entry)
            // System.out.println("Weapon attack toHit took "+(exit-entry));
            if ((th.getValue() != TargetRoll.IMPOSSIBLE)
                && !(th.getValue() >= 13)) {
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

                // Ammo cycler runs each valid ammo type through the weapon
                // while calling for expected damage on each type; best type
                // by damage is loaded

                expectedDmg = Compute.getAmmoAdjDamage(game, wep_test);

                // Get the secondary target modifier for this weapon/target
                // combo

                starg_mod = 1;

                if (en.getFacing() != -1) {
                    if (en.canChangeSecondaryFacing()) {

                        if (!Compute.isInArc(en.getPosition(),
                                             en.getSecondaryFacing(), e, en.getForwardArc())) {
                            starg_mod = 2;
                        }
                    } else {
                        if (!Compute.isInArc(en.getPosition(), en.getFacing(),
                                             e, en.getForwardArc())) {
                            starg_mod = 2;
                        }

                    }
                }

                // For good measure, infantry cannot attack multiple targets
                if ((en instanceof Infantry) && !(en instanceof BattleArmor)) {
                    starg_mod = 13;
                }

                a = new AttackOption(enemy, mw, expectedDmg, th, starg_mod,
                                     en.getCrew().getOptions().booleanOption(OptionsConstants.PILOT_APTITUDE_GUNNERY));
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
        if (best_only && (max.target != null)) {
            result.add(max);
        }
        if (result.size() > 0) {
            result.add(new AttackOption(null, mw, 0, null, 1, en.getCrew().getOptions()
                                                                .booleanOption(OptionsConstants
                                                                                       .PILOT_APTITUDE_GUNNERY)));
        }
        return result;
    }

    public GAAttack bestAttack(MoveOption es) {
        return bestAttack(es, null, 2);
    }

    public GAAttack bestAttack(MoveOption es, CEntity target, int search_level) {
        Entity en = es.getEntity();
        int attacks[] = new int[4];
        ArrayList<AttackOption> c = new ArrayList<AttackOption>();
        ArrayList<ArrayList<AttackOption>> front = new ArrayList<ArrayList<AttackOption>>();
        ArrayList<ArrayList<AttackOption>> left = new ArrayList<ArrayList<AttackOption>>();
        ArrayList<ArrayList<AttackOption>> right = new ArrayList<ArrayList<AttackOption>>();
        ArrayList<ArrayList<AttackOption>> rear = new ArrayList<ArrayList<AttackOption>>();
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
        for (Mounted mw : en.getWeaponList()) {

            // If this weapon is in the same arm as a
            // brush off attack skip to next weapon.
            c = calculateWeaponAttacks(en, mw, true);

            // Get best physical attack
            best_front_po = PhysicalCalculator.getBestPhysical(en, game);

            if ((best_front_po != null) && (en instanceof Mech)) {

                // If this weapon is in the same arm as a brush off attack
                // skip to next weapon

                if (((best_front_po.type == PhysicalOption.BRUSH_LEFT) || (best_front_po.type == PhysicalOption
                        .BRUSH_BOTH))
                    && (mw.getLocation() == Mech.LOC_LARM)) {
                    continue;
                }
                if (((best_front_po.type == PhysicalOption.BRUSH_RIGHT) || (best_front_po.type == PhysicalOption
                        .BRUSH_BOTH))
                    && (mw.getLocation() == Mech.LOC_RARM)) {
                    continue;
                }

                // Total the damage of all weapons fired from each arm
                if (((best_front_po.type == PhysicalOption.PUNCH_LEFT) || (best_front_po.type == PhysicalOption
                        .PUNCH_BOTH))
                    && (mw.getLocation() == Mech.LOC_LARM)) {
                    if (c.size() > 0) {
                        front_la_dmg += c.get(c.size() - 2).value;
                    }
                }
                if (((best_front_po.type == PhysicalOption.PUNCH_RIGHT) || (best_front_po.type == PhysicalOption
                        .PUNCH_BOTH))
                    && (mw.getLocation() == Mech.LOC_RARM)) {
                    if (c.size() > 0) {
                        front_ra_dmg += c.get(c.size() - 2).value;
                    }
                }
                // If this weapon is a push attack and an arm mounted
                // weapon skip to next weapon

                if ((best_front_po.type == PhysicalOption.PUSH_ATTACK)
                    && ((mw.getLocation() == Mech.LOC_LARM) || (mw
                                                                        .getLocation() == Mech.LOC_RARM))) {
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
                c = calculateWeaponAttacks(en, mw, true);
                if (c.size() > 0) {
                    // Get best physical attack
                    best_left_po = PhysicalCalculator.getBestPhysical(en, game);
                    if ((best_left_po != null) && (en instanceof Mech)) {
                        if (((best_left_po.type == PhysicalOption.PUNCH_LEFT) || (best_left_po.type == PhysicalOption
                                .PUNCH_BOTH))
                            && (mw.getLocation() == Mech.LOC_LARM)) {
                            left_la_dmg += c.get(c.size() - 2).value;
                        }
                        if (((best_left_po.type == PhysicalOption.PUNCH_RIGHT) || (best_left_po.type ==
                                                                                   PhysicalOption.PUNCH_BOTH))
                            && (mw.getLocation() == Mech.LOC_RARM)) {
                            left_ra_dmg += c.get(c.size() - 2).value;
                        }
                    }
                    left.add(c);
                    attacks[1] = Math.max(attacks[1], c.size());
                }
                en.setSecondaryFacing((o_facing + 1) % 6);
                c = calculateWeaponAttacks(en, mw, true);
                if (c.size() > 0) {
                    // Get best physical attack
                    best_right_po = PhysicalCalculator
                            .getBestPhysical(en, game);
                    if ((best_right_po != null) && (en instanceof Mech)) {
                        if (((best_right_po.type == PhysicalOption.PUNCH_LEFT) || (best_right_po.type ==
                                                                                   PhysicalOption.PUNCH_BOTH))
                            && (mw.getLocation() == Mech.LOC_LARM)) {
                            right_la_dmg += c.get(c.size() - 2).value;
                        }
                        if (((best_right_po.type == PhysicalOption.PUNCH_RIGHT) || (best_right_po.type ==
                                                                                    PhysicalOption.PUNCH_BOTH))
                            && (mw.getLocation() == Mech.LOC_RARM)) {
                            right_ra_dmg += c.get(c.size() - 2).value;
                        }
                    }
                    right.add(c);
                    attacks[2] = Math.max(attacks[2], c.size());
                }
                en.setSecondaryFacing((o_facing + 3) % 6);
                c = calculateWeaponAttacks(en, mw, true);
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

        fireOrPhysicalCheck(best_front_po, en, front, front_la_dmg,
                            front_ra_dmg);

        ArrayList<ArrayList<ArrayList<AttackOption>>> arcs = new ArrayList<ArrayList<ArrayList<AttackOption>>>();
        arcs.add(front);
        if (!es.getFinalProne() && en.canChangeSecondaryFacing()) {
            fireOrPhysicalCheck(best_left_po, en, left, left_la_dmg,
                                left_ra_dmg);
            arcs.add(left);
            fireOrPhysicalCheck(best_right_po, en, right, right_la_dmg,
                                right_ra_dmg);
            arcs.add(right);
            // Meks and protos can't twist all the way around.
            if (!(en instanceof Mech) && !(en instanceof Protomech)) {
                arcs.add(rear);
            }
        }
        for (int i = 0; i < arcs.size(); i++) {
            ArrayList<ArrayList<AttackOption>> v = arcs.get(i);
            if (v.size() > 0) {
                GAAttack test = new GAAttack(this, centities.get(en), v,
                                             Math.max((v.size() + attacks[i]) * search_level,
                                                      20 * search_level), 30 * search_level,
                                             en.isEnemyOf(getEntitiesOwned().get(0))
                );
                test.setFiringArc(i);
                test.evolve();
                if (target != null) {
                    if ((result == null)
                        || (test.getDamageUtility(target) > result
                            .getDamageUtility(target))) {
                        result = test;
                    }
                } else if ((result == null)
                           || (test.getFittestChromosomesFitness() > result
                        .getFittestChromosomesFitness())) {
                    result = test;
                }
            }
        }
        return result;
    }

    /**
     * If the best attack is a punch, then check each punch damage against the
     * weapons damage from the appropriate arm; if the punch does more damage,
     * drop the weapons in that arm to 0 expected damage Repeat this for left
     * and right twists
     *
     * @param best_po
     * @param entity
     * @param attackOptions
     * @param la_dmg
     * @param ra_dmg
     */
    private void fireOrPhysicalCheck(PhysicalOption best_po, Entity entity,
                                     ArrayList<ArrayList<AttackOption>> attackOptions, double la_dmg,
                                     double ra_dmg) {
        ArrayList<AttackOption> c;
        if ((best_po != null) && (entity instanceof Mech)) {
            if (best_po.type == PhysicalOption.PUNCH_LEFT) {
                if ((la_dmg < best_po.expectedDmg)
                    && (attackOptions.size() > 0)) {
                    for (int i = 0; i < attackOptions.size(); i++) {
                        c = attackOptions.get(i);
                        for (int j = 0; j < c.size(); j++) {
                            if (c.get(j).weapon.getLocation() == Mech.LOC_LARM) {
                                c.get(j).expected = 0;
                                c.get(j).primary_expected = 0;
                            }
                        }
                    }
                }
            }
            if (best_po.type == PhysicalOption.PUNCH_RIGHT) {
                if ((ra_dmg < best_po.expectedDmg)
                    && (attackOptions.size() > 0)) {
                    for (int i = 0; i < attackOptions.size(); i++) {
                        c = attackOptions.get(i);
                        for (int j = 0; j < c.size(); j++) {
                            if (c.get(j).weapon.getLocation() == Mech.LOC_RARM) {
                                c.get(j).expected = 0;
                                c.get(j).primary_expected = 0;
                            }
                        }
                    }
                }
            }
            if (best_po.type == PhysicalOption.PUNCH_BOTH) {
                if (((la_dmg + ra_dmg) < best_po.expectedDmg)
                    && (attackOptions.size() > 0)) {
                    for (int i = 0; i < attackOptions.size(); i++) {
                        c = attackOptions.get(i);
                        for (int j = 0; j < c.size(); j++) {
                            if (c.get(j).weapon.getLocation() == Mech.LOC_LARM) {
                                c.get(j).expected = 0;
                                c.get(j).primary_expected = 0;
                            }
                            if (c.get(j).weapon.getLocation() == Mech.LOC_RARM) {
                                c.get(j).expected = 0;
                                c.get(j).primary_expected = 0;
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

    @Override
    public void calculateFiringTurn() {
        int first_entity = game.getFirstEntityNum(getMyTurn());
        int entity_num = first_entity;
        int best_entity = first_entity;
        int spin_mode = 0;
        double max = java.lang.Double.NEGATIVE_INFINITY;
        int[] results = null;
        ArrayList<ArrayList<AttackOption>> winner = null;
        int arc = 0;
        WeaponType spinner;

        if (entity_num == -1) {
            return;
        }

        do {
            Entity en = game.getEntity(entity_num);
            CEntity cen = centities.get(en);

            GAAttack test = bestAttack(cen.current, null, 3);

            if ((test != null) && (test.getFittestChromosomesFitness() > max)) {
                max = test.getFittestChromosomesFitness();
                results = test.getResultChromosome();
                arc = test.getFiringArc();
                best_entity = entity_num;
                winner = test.getAttack();
            }
            entity_num = game.getNextEntityNum(getMyTurn(), entity_num);
        } while ((entity_num != first_entity) && (entity_num != -1));

        Vector<EntityAction> av = new Vector<EntityAction>();
        // maximum already selected (or default)
        Entity en = game.getEntity(best_entity);
        if (results != null) {
            Entity primary_target = game.getEntitiesVector().get(
                    results[results.length - 1]);
            TreeSet<AttackOption> tm = new TreeSet<AttackOption>(
                    new AttackOption.Sorter(centities.get(primary_target)));
            for (int i = 0; i < (results.length - 1); i++) {
                if (winner != null) {
                    AttackOption a = winner.get(i).get(results[i]);
                    if (a.target != null) {
                        a.target.expected_damage[a.toHit.getSideTable()] += a.value;
                        a.target.hasTakenDamage = true;
                        tm.add(a);
                    }
                }
            }
            Iterator<AttackOption> i = tm.iterator();
            while (i.hasNext()) {
                AttackOption a = i.next();

                WeaponAttackAction new_attack = new WeaponAttackAction(
                        en.getId(), a.target.getEntity().getId(),
                        en.getEquipmentNum(a.weapon));

                if (en.getEquipment(new_attack.getWeaponId()).getLinked() != null) {
                    spinner = (WeaponType) a.weapon.getType();

                    // If this is an ultra-cannon or rotary cannon, try to spin
                    // it up

                    if ((spinner.getAmmoType() == AmmoType.T_AC_ULTRA)
                        || (spinner.getAmmoType() == AmmoType.T_AC_ULTRA_THB)
                        || (spinner.getAmmoType() == AmmoType.T_AC_ROTARY)) {
                        spin_mode = Compute.spinUpCannon(game, new_attack);
                        super.sendModeChange(en.getId(),
                                             en.getEquipmentNum(a.weapon), spin_mode);
                    }
                    Mounted cur_ammo = en
                            .getEquipment(new_attack.getWeaponId()).getLinked();
                    new_attack.setAmmoId(en.getEquipmentNum(cur_ammo));
                    Compute.getAmmoAdjDamage(game, new_attack);

                }
                av.add(new_attack);

            }

            // Use the attack options and weapon attack actions to determine the
            // best aiming point

            if (av.size() > 0) {
                getAimPoint(tm, av);
            }

        }
        switch (arc) {
            case 1:
                av.add(0, new TorsoTwistAction(en.getId(),
                                               (en.getFacing() + 5) % 6));
                break;
            case 2:
                av.add(0, new TorsoTwistAction(en.getId(),
                                               (en.getFacing() + 1) % 6));
                break;
            case 3:
                av.add(0, new TorsoTwistAction(en.getId(),
                                               (en.getFacing() + 3) % 6));
                break;
        }
        sendAttackData(best_entity, av);
    }

    /**
     * consider how to put more pre-turn logic here
     */
    @Override
    protected void initMovement() {
        old_moves = null;
        enemies_moved = 0;
        double max_modifier = 1.4;
        ArrayList<Entity> entities = new ArrayList<Entity>(
                game.getEntitiesVector());
        double num_entities = Math.sqrt(entities.size()) / 100;
        ArrayList<CEntity> friends = new ArrayList<CEntity>();
        ArrayList<CEntity> foes = new ArrayList<CEntity>();
        double friend_sum = 0;
        double foe_sum = 0;
        double max_foe_bv = 0;
        CEntity max_foe = null;
        for (int i = 0; i < entities.size(); i++) {
            Entity entity = entities.get(i);
            CEntity centity = centities.get(entity);
            centity.enemy_num = i;
            double old_value = centity.bv * (centity.overall_armor_percent + 1);
            centity.reset(); // should get fresh values
            double new_value = centity.bv * (centity.overall_armor_percent + 1);
            double percent = 1 + ((new_value - old_value) / old_value);
            if (entity.getOwner().equals(getLocalPlayer())) {
                friends.add(centity);
                friend_sum += new_value;
                if (percent < .85) {
                    // small retreat
                    centity.strategy.attack = .85;
                } else if (percent < .95) {
                    centity.strategy.attack = 1;
                } else if ((percent <= 1)
                           && (centity.strategy.attack < max_modifier)) {
                    if (percent == 1) {
                        if (centity.strategy.attack < 1) {
                            centity.strategy.attack = Math.min(
                                    1.4 * centity.strategy.attack, 1);
                        } else {
                            centity.strategy.attack *= (1.0 + num_entities);
                        }
                    } else {
                        centity.strategy.attack *= (1.0 + (2 * num_entities));
                    }
                }
            } else if (!entity.getOwner().isEnemyOf(getLocalPlayer())) {
                friend_sum += new_value;
            } else {
                foes.add(centity);
                foe_sum += new_value;
                if (entity.isCommander()) {
                    new_value *= 3; // make bots like to attack commanders
                }
                if ((new_value > max_foe_bv) || (max_foe == null)) {
                    max_foe_bv = new_value;
                    max_foe = centity;
                }
                if (getEntitiesOwned().size() > 2) {
                    if (centity.strategy.target > 2) {
                        centity.strategy.target = 1 + (.5 * (centity.strategy.target - 2));
                    }
                    if ((percent < .85)
                        && (centity.strategy.target < max_modifier)) {
                        centity.strategy.target *= (1.0 + (6 * num_entities));
                    } else if ((percent < .95)
                               && (centity.strategy.target < max_modifier)) {
                        centity.strategy.target *= (1.0 + (4 * num_entities));
                    } else if (percent <= 1) {
                        if (percent == 1) {
                            centity.strategy.target /= (1.0 + (2 * num_entities));
                        } else {
                            centity.strategy.target /= (1.0 + num_entities);
                        }
                    }
                    // don't go below one
                    if (centity.strategy.target < 1) {
                        centity.strategy.target = 1;
                    }
                }
            }
        }
        System.out.println("Us " + friend_sum + " Them " + foe_sum);
        // do some more reasoning...
        double unit_values = friend_sum;
        double enemy_values = foe_sum;
        Iterator<CEntity> i = foes.iterator();

        if (friends.size() > 1) {
            if ((Strategy.MainTarget == null)
                || (null == game.getEntity(Strategy.MainTarget.getEntity()
                                                              .getId()))) {
                Strategy.MainTarget = max_foe;
            }
            // TODO : Handle this better.
            if (null == Strategy.MainTarget) {
                System.err
                        .println("TestBot#initMovement() - no main target for bot");
            } else if (null == Strategy.MainTarget.strategy) {
                System.err
                        .println("TestBot#initMovement() - no strategy for main target");
            } else {
                Strategy.MainTarget.strategy.target += .2;
                while (i.hasNext()) {
                    CEntity centity = i.next();
                    // good turn, keep up the work, but randomize to reduce
                    // predictability
                    if ((friend_sum - foe_sum) >= ((.9 * unit_values) - enemy_values)) {
                        if (Compute.randomInt(2) == 1) {
                            centity.strategy.target += .3;
                        }
                        // lost that turn, but still in the fight, just get a
                        // little more aggressive
                    } else if (friend_sum > (.9 * foe_sum)) {
                        centity.strategy.target += .15;
                        // lost that turn and loosing
                    } else if (centity.strategy.target < 2) { // go for the
                        // gusto
                        centity.strategy.target += .3;
                    }
                    System.out.println(centity.getEntity().getShortName() + " "
                                       + centity.strategy.target);
                }
            }
        }

        double ratio = friend_sum / foe_sum;
        double mod = 1;
        if (ratio < .9) {
            mod = .95;
        } else if (ratio < 1) {
            // no change
        } else { // attack
            mod = (1.0 + num_entities);
        }
        i = friends.iterator();
        while (i.hasNext()) {
            CEntity centity = i.next();
            if (!((mod < 1) && (centity.strategy.attack < .6))
                && !((mod > 1) && (centity.strategy.attack >= max_modifier))) {
                centity.strategy.attack *= mod;
            }
        }
        System.gc(); // just to make sure
    }

    @Override
    protected void processChat(GamePlayerChatEvent ge) {
        chatp.processChat(ge, this);
    }

    // Where do I put my units? This prioritizes hexes and facings
    @Override
    protected void calculateDeployment() {

        int weapon_count;
        int hex_count, x_ave, y_ave, nDir;
        double av_range;

        Coords pointing_to = new Coords();

        int entNum = game.getFirstDeployableEntityNum();
        assert (entNum != Entity.NONE) : "The bot is trying to deploy without units being left.";

        List<Coords> cStart = getStartingCoordsArray();
        Coords cDeploy = getFirstValidCoords(getEntity(entNum), cStart);

        if (cDeploy == null) {
            // bad event handeling, this unit is not deployable, remove it
            // instead.
            // This should not happen but does (eg ships on a deployment zone
            // without water.
            System.out
                    .println("The bot does not know how or is unable to deploy "
                             + getEntity(entNum) + ". Removing it instead.");
            sendChat("Oh dear I don't know how to deploy this "
                     + getEntity(entNum) + ". Skipping to the next one.");
            sendDeleteEntity(entNum);
            return;
        }

        // Now that we have a location to deploy to, get a direction
        // Using average long range of deploying unit, point towards the largest
        // cluster of enemies in range

        av_range = 0.0;
        weapon_count = 0;
        for (Mounted mounted : getEntity(entNum).getWeaponList()) {
            WeaponType wtype = (WeaponType) mounted.getType();
            if ((wtype.getName() != "ATM 3") && (wtype.getName() != "ATM 6")
                && (wtype.getName() != "ATM 9")
                && (wtype.getName() != "ATM 12")) {
                if (getEntity(entNum).getC3Master() != null) {
                    av_range += ((wtype.getLongRange()) * 1.25);
                } else {
                    av_range += wtype.getLongRange();
                }
                weapon_count++;
            }
        }
        for (Mounted mounted : getEntity(entNum).getAmmo()) {
            AmmoType atype = (AmmoType) mounted.getType();
            if (atype.getAmmoType() == AmmoType.T_ATM) {
                weapon_count++;
                av_range += 15.0;
                if (atype.getMunitionType() == AmmoType.M_HIGH_EXPLOSIVE) {
                    av_range -= 6;
                }
                if (atype.getMunitionType() == AmmoType.M_EXTENDED_RANGE) {
                    av_range += 12.0;
                }
            }
            if (atype.getAmmoType() == AmmoType.T_MML) {
                weapon_count++;
                if (atype.hasFlag(AmmoType.F_MML_LRM)) {
                    av_range = 9;
                } else {
                    av_range = 21.0;
                }
            }
        }

        av_range = av_range / weapon_count;

        hex_count = 0;
        x_ave = 0;
        y_ave = 0;
        for (Entity test_ent : game.getEntitiesVector()) {
            if (test_ent.isDeployed()) {
                if (test_ent.isVisibleToEnemy()) {
                    if (cDeploy.distance(test_ent.getPosition()) <= (int) av_range) {
                        hex_count++;
                        x_ave += test_ent.getPosition().getX();
                        y_ave += test_ent.getPosition().getY();
                    }
                }
            }
        }
        if (hex_count != 0) {
            pointing_to = new Coords((x_ave / hex_count), (y_ave / hex_count));
        } else {
            pointing_to = new Coords(game.getBoard().getWidth() / 2, game
                                                                             .getBoard().getHeight() / 2);
        }
        nDir = cDeploy.direction(pointing_to);

        // If unit has stealth armor, turn it on
        if ((getEntity(entNum) instanceof Mech)
            && (getEntity(entNum).getArmorType(0) == EquipmentType.T_ARMOR_STEALTH)
            && !getEntity(entNum).hasPatchworkArmor()) {
            for (Mounted test_equip : getEntity(entNum).getMisc()) {
                MiscType test_type = (MiscType) test_equip.getType();
                if (test_type.hasFlag(MiscType.F_STEALTH)) {
                    if (!test_equip.curMode().getName().equals("On")) {
                        test_equip.setMode("On");
                        super.sendModeChange(entNum, getEntity(entNum)
                                .getEquipmentNum(test_equip), 1);
                    }
                }
            }
        }

        Entity ce = game.getEntity(entNum);
        assert (!ce.isLocationProhibited(cDeploy)) : "Bot tried to deploy to an invalid hex";
        deploy(entNum, cDeploy, nDir, 0);
    }

    @Override
    protected MovePath continueMovementFor(Entity entity) {

        if (entity == null) {
            throw new NullPointerException("Entity is null.");
        }

        System.out.println("Contemplating movement of " + entity.getShortName()
                           + " " + entity.getId());
        CEntity cen = centities.get(entity);
        cen.refresh();
        firstPass(cen);

        Object[] enemy_array = getEnemyEntities().toArray();
        MoveOption result[] = calculateMove(entity);
        MoveOption min = null;
        ArrayList<MoveOption[]> possible = new ArrayList<MoveOption[]>();
        boolean short_circuit = false;

        if (result.length < 6) {
            min = result.length > 0 ? (MoveOption) result[0] : null;
            short_circuit = true;
        }
        possible.add(result);

        // should ignore mechs that are not engaged
        // and only do the below when there are 2 or mechs left to move
        if (!short_circuit) {
            if ((getEntitiesOwned().size() > 1) && (possible.size() > 0)) {
                GALance lance = new GALance(this, possible, 50, 80);
                lance.evolve();
                min = lance.getResult();
                old_moves = lance;
            } else if ((possible.get(0) != null)
                       && (possible.get(0).length > 0)) {
                min = possible.get(0)[0];
            }
        }
        if (min == null) {
            min = new MoveOption(game, centities.get(getFirstEntityNum()));
        }

        for (Object element : enemy_array) {
            Entity en = (Entity) element;

            // ignore loaded units
            if (en.getPosition() == null) {
                continue;
            }

            CEntity enemy = centities.get(en);
            int enemy_hit_arc = CEntity.getThreatHitArc(
                    enemy.current.getFinalCoords(),
                    enemy.current.getFinalFacing(), min.getFinalCoords());
            MoveOption.DamageInfo di = min.damageInfos.get(enemy);
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
        min.getCEntity().moved = true;

        // If this unit has a jammed RAC, and it has only walked,
        // add an unjam action
        if (min.getLastStep() != null) {
            if (min.getCEntity().entity.canUnjamRAC()) {
                if ((min.getLastStep().getMovementType(true) == EntityMovementType.MOVE_WALK)
                    || (min.getLastStep().getMovementType(true) == EntityMovementType.MOVE_VTOL_WALK)
                    || (min.getLastStep().getMovementType(true) == EntityMovementType.MOVE_NONE)) {
                    // Cycle through all available weapons, only unjam if the
                    // jam(med)
                    // RACs count for a significant portion of possible damage
                    int rac_damage = 0;
                    int other_damage = 0;
                    int clearance_range = 0;
                    for (Mounted equip : min.getCEntity().entity
                            .getWeaponList()) {
                        WeaponType test_weapon = new WeaponType();

                        test_weapon = (WeaponType) equip.getType();
                        if ((test_weapon.getAmmoType() == AmmoType.T_AC_ROTARY)
                            && (equip.isJammed() == true)) {
                            rac_damage = rac_damage
                                         + (4 * (test_weapon.getDamage()));
                        } else {
                            if (equip.canFire()) {
                                other_damage += test_weapon.getDamage();
                                if (test_weapon.getMediumRange() > clearance_range) {
                                    clearance_range = test_weapon
                                            .getMediumRange();
                                }
                            }
                        }
                    }
                    // Even if the jammed RAC doesn't make up a significant
                    // portion
                    // of the units damage, its still better to have it
                    // functional
                    // If nothing is "close" then unjam anyways
                    int check_range = 100;
                    for (Entity enemy : game.getEntitiesVector()) {
                        if ((min.getCEntity().entity.getPosition() != null)
                            && (enemy.getPosition() != null)
                            && (enemy.isEnemyOf(min.getCEntity().entity))) {
                            if (enemy.isVisibleToEnemy()) {
                                if (min.getCEntity().entity.getPosition()
                                                           .distance(enemy.getPosition()) < check_range) {
                                    check_range = min.getCEntity().entity
                                            .getPosition().distance(
                                                    enemy.getPosition());
                                }
                            }
                        }
                    }
                    if ((rac_damage >= other_damage)
                        || (check_range < clearance_range)) {
                        min.addStep(MoveStepType.UNJAM_RAC);
                    }
                }
            }
        }

        return min;
    }

    @Override
    protected Vector<Minefield> calculateMinefieldDeployment() {
        Vector<Minefield> deployedMinefields = new Vector<Minefield>();

        deployMinefields(deployedMinefields, getLocalPlayer()
                .getNbrMFConventional(), 0);
        deployMinefields(deployedMinefields,
                         getLocalPlayer().getNbrMFCommand(), 1);
        deployMinefields(deployedMinefields, getLocalPlayer().getNbrMFVibra(),
                         2);

        return deployedMinefields;
    }

    @Override
    protected PlayerIDandList<Coords> calculateArtyAutoHitHexes() {
        PlayerIDandList<Coords> artyAutoHitHexes = new PlayerIDandList<Coords>();
        artyAutoHitHexes.setPlayerID(getLocalPlayer().getId());
        return artyAutoHitHexes;
    }

    protected void deployMinefields(Vector<Minefield> deployedMinefields,
                                    int number, int type) {
        for (int i = 0; i < number; i++) {
            Coords coords = new Coords(Compute.randomInt(game.getBoard()
                                                             .getWidth()),
                                       Compute.randomInt(game.getBoard().getHeight())
            );

            if (game.containsMinefield(coords)) {
                Minefield mf = game.getMinefields(coords).get(0);
                if (mf.getPlayerId() == getLocalPlayer().getId()) {
                    i--;
                    continue;
                }
            } else {
                Minefield mf = null;

                if (type == 0) {
                    mf = Minefield.createMinefield(coords, getLocalPlayer()
                            .getId(), Minefield.TYPE_CONVENTIONAL, 10);
                } else if (type == 1) {
                    mf = Minefield.createMinefield(coords, getLocalPlayer()
                            .getId(), Minefield.TYPE_COMMAND_DETONATED, 10);
                } else if (type == 2) {
                    mf = Minefield.createMinefield(coords, getLocalPlayer()
                            .getId(), Minefield.TYPE_VIBRABOMB, 20);
                }
                deployedMinefields.add(mf);
            }
        }
    }

    /*
     * Calculate the best location to aim at on a target Mech. Attack options
     * must match 1:1 with WeaponAttackActions in Vector.
     */
    private void getAimPoint(TreeSet<AttackOption> attack_tree,
                             Vector<EntityAction> atk_action_list) {

        if ((attack_tree == null) || (atk_action_list == null)) {
            return;
        }

        WeaponAttackAction aimed_attack;
        AttackOption current_option;

        Vector<Integer> target_id_list; // List of viable aimed-shot targets

        // Adjusted damages
        double base_damage, base_odds;
        double refactored_damage, refactored_head;

        // Armor values
        // Order is: head, ct, lt, rt, la, ra, ll, rl
        double[] values = {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};

        // Internal structure values
        // Order is: head, ct, lt, rt, la, ra, ll, rl
        double[] is_values = {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};

        // Fitness values
        // Order is: head, ct, lt, rt, la, ra, ll, rl
        double[] fitness = {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};

        // Counters for armor penetration
        // Order is: head, ct, lt, rt, la, ra, ll, rl
        int[] pen_counters = {0, 0, 0, 0, 0, 0, 0, 0};

        int attacker_id, test_target;
        int action_index = 0;

        // Base to-hit
        int base_to_hit;

        // Best locations to aim for
        int best_loc, best_loc_head;

        boolean has_tcomp = false;
        boolean imob_target, rear_shot;
        boolean is_primary_target;

        // For each attack action

        target_id_list = new Vector<Integer>();
        for (EntityAction aea : atk_action_list) {

            if (aea instanceof WeaponAttackAction) {
                // Get the attacker

                attacker_id = ((WeaponAttackAction) atk_action_list.get(0))
                        .getEntityId();

                // Check to see if the attacker has a tcomp

                has_tcomp = game.getEntity(attacker_id).hasTargComp();

                // Get the target entity id

                test_target = ((WeaponAttackAction) aea).getTargetId();

                // If the target is a Mech

                if (game.getEntity(test_target) instanceof Mech) {

                    // If the target is officially immobile or if the attacker
                    // has a tcomp

                    if ((has_tcomp == true)
                        | (game.getEntity(test_target).isImmobile())) {
                        if (!target_id_list.contains(test_target)) {
                            target_id_list.add(test_target);
                        }
                    }
                }
            }
        }

        // For each valid target

        is_primary_target = true;
        for (Iterator<Integer> i = target_id_list.iterator(); i.hasNext(); ) {

            // Set the current target

            test_target = i.next();
            imob_target = game.getEntity(test_target).isImmobile();

            // Get the targets aspect ratio

            rear_shot = false;
            for (Iterator<AttackOption> j = attack_tree.iterator(); j.hasNext(); ) {
                current_option = j.next();
                if (current_option.target.getEntity().getId() == test_target) {
                    int attack_direction = current_option.toHit.getSideTable();
                    if (attack_direction == ToHitData.SIDE_REAR) {
                        rear_shot = true;
                    } else {
                        rear_shot = false;
                    }
                    break;
                }
            }

            // Get the armor values for the target and make them negative (count
            // up)

            values[0] = game.getEntity(test_target).getArmor(Mech.LOC_HEAD);
            values[1] = game.getEntity(test_target).getArmor(Mech.LOC_CT,
                                                             rear_shot);
            values[2] = game.getEntity(test_target).getArmor(Mech.LOC_LT,
                                                             rear_shot);
            values[3] = game.getEntity(test_target).getArmor(Mech.LOC_RT,
                                                             rear_shot);
            values[4] = game.getEntity(test_target).getArmor(Mech.LOC_LARM);
            values[5] = game.getEntity(test_target).getArmor(Mech.LOC_RARM);
            values[6] = game.getEntity(test_target).getArmor(Mech.LOC_LLEG);
            values[7] = game.getEntity(test_target).getArmor(Mech.LOC_RLEG);

            // Get the internals for the target

            is_values[0] = game.getEntity(test_target).getInternal(
                    Mech.LOC_HEAD);
            is_values[1] = game.getEntity(test_target).getInternal(Mech.LOC_CT);
            is_values[2] = game.getEntity(test_target).getInternal(Mech.LOC_LT);
            is_values[3] = game.getEntity(test_target).getInternal(Mech.LOC_RT);
            is_values[4] = game.getEntity(test_target).getInternal(
                    Mech.LOC_LARM);
            is_values[5] = game.getEntity(test_target).getInternal(
                    Mech.LOC_RARM);
            is_values[6] = game.getEntity(test_target).getInternal(
                    Mech.LOC_LLEG);
            is_values[7] = game.getEntity(test_target).getInternal(
                    Mech.LOC_RLEG);

            // Reset the fitness array
            for (int arr_index = 0; arr_index < 8; arr_index++) {
                fitness[arr_index] = 0.0;
            }

            // Reset the penetration counter

            for (int arr_index = 0; arr_index < 8; arr_index++) {
                pen_counters[arr_index] = 0;
            }

            // For each attack option

            action_index = 0;
            refactored_damage = 0.0;
            refactored_head = 0.0;

            best_loc = Mech.LOC_CT;
            best_loc_head = Mech.LOC_CT;
            for (Iterator<AttackOption> j = attack_tree.iterator(); j.hasNext(); ) {

                // If the target of the attack option is the current target

                current_option = j.next();
                if (test_target == current_option.target.getEntity().getId()) {

                    // Get the weapon

                    Mounted test_weapon = current_option.weapon;
                    boolean aptGunnery = current_option.target.getEntity().getCrew().getOptions()
                                                              .booleanOption(OptionsConstants.PILOT_APTITUDE_GUNNERY);

                    // If the weapon is not LBX cannon or LBX cannon loaded with
                    // slug

                    boolean direct_fire = true;
                    if (((WeaponType) test_weapon.getType())
                                .hasFlag(WeaponType.F_DIRECT_FIRE) == false) {
                        direct_fire = false;
                    }
                    if (test_weapon.getType().hasFlag(WeaponType.F_PULSE)) {
                        direct_fire = false;
                    }
                    if ((((WeaponType) test_weapon.getType()).getAmmoType() == AmmoType.T_AC_LBX)
                        || (((WeaponType) test_weapon.getType())
                                    .getAmmoType() == AmmoType.T_AC_LBX)) {
                        if (((AmmoType) test_weapon.getLinked().getType())
                                    .getAmmoType() == AmmoType.M_CLUSTER) {
                            direct_fire = false;
                        }
                    }
                    if (test_weapon.getCurrentShots() > 1) {
                        direct_fire = false;
                    }

                    // If the weapon is direct fire

                    if (direct_fire == true) {

                        // Get the expected damage, to-hit number, and odds
                        // (0-1) of hitting

                        base_damage = is_primary_target ? current_option.primary_expected
                                                        : current_option.expected;
                        base_to_hit = is_primary_target ? current_option.toHit
                                .getValue()
                                                        : current_option.toHit.getValue() + 1;
                        base_odds = is_primary_target ? current_option.primary_odds
                                                      : current_option.odds;
                        base_damage = base_odds == 0.0 ? 0.0 : base_damage
                                                               / base_odds;

                        // If the target is mobile, only a tcomp can make an
                        // aimed shot

                        if (!imob_target & has_tcomp) {

                            // Refactor the expected damage to account for
                            // increased to-hit number

                            refactored_head = 0.0;
                            if (((base_to_hit + 4) <= 12) && Compute.allowAimedShotWith(test_weapon,
                                                                                        IAimingModes
                                                                                                .AIM_MODE_TARG_COMP)) {
                                refactored_damage = base_damage
                                                    * (Compute.oddsAbove(base_to_hit + 4, aptGunnery) / 100.0);
                                ((WeaponAttackAction) atk_action_list
                                        .get(action_index))
                                        .setAimingMode(IAimingModes.AIM_MODE_TARG_COMP);
                                // Consider that a regular shot has a roughly
                                // 20% chance of hitting the same location
                                // Use the better of the regular shot or aimed
                                // shot
                                if ((0.2 * base_damage * (Compute.oddsAbove(base_to_hit, aptGunnery) / 100.0)) >
                                    refactored_damage) {
                                    refactored_damage = 0.2
                                                        * base_damage
                                                        * (Compute.oddsAbove(base_to_hit, aptGunnery) / 100.0);
                                    ((WeaponAttackAction) atk_action_list
                                            .get(action_index))
                                            .setAimingMode(IAimingModes.AIM_MODE_NONE);
                                }
                            } else {
                                refactored_damage = 0.0;
                                ((WeaponAttackAction) atk_action_list
                                        .get(action_index))
                                        .setAimingMode(IAimingModes.AIM_MODE_NONE);
                            }

                        }

                        // If the target is immobile, the shot will always be
                        // aimed

                        if (imob_target) {

                            // If the attacker has a tcomp, consider both
                            // options: immobile aim, tcomp aim

                            if (has_tcomp) {

                                if (Compute.allowAimedShotWith(test_weapon, IAimingModes.AIM_MODE_TARG_COMP)) {
                                    // Refactor the expected damage to account for
                                    // increased to-hit number of the tcomp

                                    refactored_damage = base_damage
                                                        * (Compute.oddsAbove(base_to_hit + 4, aptGunnery) / 100.0);
                                    refactored_head = 0.0;
                                    ((WeaponAttackAction) atk_action_list
                                            .get(action_index))
                                            .setAimingMode(IAimingModes.AIM_MODE_TARG_COMP);

                                    // Check against immobile aim mode w/tcomp
                                    // assist

                                }
                                if (((0.50 * base_damage * (Compute
                                                                    .oddsAbove(base_to_hit, aptGunnery) / 100.0)) >
                                     refactored_damage) && Compute.allowAimedShotWith(test_weapon,
                                                                                      IAimingModes.AIM_MODE_IMMOBILE)) {
                                    refactored_damage = 0.50
                                                        * base_damage
                                                        * (Compute.oddsAbove(base_to_hit, aptGunnery) / 100.0);
                                    refactored_head = 0.50
                                                      * base_damage
                                                      * (Compute
                                                                 .oddsAbove(base_to_hit + 7, aptGunnery) / 100.0);
                                    ((WeaponAttackAction) atk_action_list
                                            .get(action_index))
                                            .setAimingMode(IAimingModes.AIM_MODE_IMMOBILE);
                                }

                            } else if (Compute.allowAimedShotWith(test_weapon, IAimingModes.AIM_MODE_IMMOBILE)) {

                                // If the attacker doesn't have a tcomp, settle
                                // for immobile aim

                                refactored_damage = 0.50
                                                    * base_damage
                                                    * (Compute.oddsAbove(base_to_hit, aptGunnery) / 100.0);
                                refactored_head = 0.50
                                                  * base_damage
                                                  * (Compute.oddsAbove(base_to_hit + 7, aptGunnery) / 100.0);
                                ((WeaponAttackAction) atk_action_list
                                        .get(action_index))
                                        .setAimingMode(IAimingModes.AIM_MODE_IMMOBILE);

                            }
                        }

                        // Count the refactored damage off each location. Count
                        // hits to IS.
                        // Ignore locations that have been previously destroyed

                        for (int arr_index = 0; arr_index < 8; arr_index++) {
                            if (arr_index == 0) {
                                values[arr_index] -= refactored_head;
                            } else {
                                values[arr_index] -= refactored_damage;
                            }
                            if ((values[arr_index] < 0)
                                & (is_values[arr_index] > 0)) {
                                is_values[arr_index] += values[arr_index];
                                values[arr_index] = 0;
                                pen_counters[arr_index]++;
                            }
                        }
                    }

                    // End if (AttackAction against current target)

                }

                action_index++;

            }

            double loc_mod;
            for (int arr_index = 0; arr_index < 8; arr_index++) {
                loc_mod = 0.0;

                // If any location has had its armor stripped but is not
                // destroyed,
                // criticals may result

                if ((values[arr_index] <= 0) & (is_values[arr_index] > 0)) {
                    switch (arr_index) {
                        case 0: // Head hits are very good, pilot damage and
                            // critical systems
                            fitness[arr_index] = 4.0 * pen_counters[arr_index];
                            fitness[arr_index] += getAimModifier(test_target,
                                                                 Mech.LOC_HEAD);
                            break;
                        case 1: // CT hits are good, chances at hitting gyro,
                            // engine
                            fitness[arr_index] = 3.0 * pen_counters[arr_index];
                            fitness[arr_index] += getAimModifier(test_target,
                                                                 Mech.LOC_CT);
                            break;
                        case 2: // Side torso hits are good, equipment hits and
                            // ammo slots
                            loc_mod = getAimModifier(test_target, Mech.LOC_LT);
                            fitness[arr_index] = 2.0 * pen_counters[arr_index];
                            fitness[arr_index] += loc_mod;
                            break;
                        case 3:
                            loc_mod = getAimModifier(test_target, Mech.LOC_RT);
                            fitness[arr_index] = 2.0 * pen_counters[arr_index];
                            fitness[arr_index] += loc_mod;
                            break;
                        case 6: // Leg hits are good, reduces target mobility
                            loc_mod = getAimModifier(test_target, Mech.LOC_LLEG);
                            fitness[arr_index] = 2.0 * pen_counters[arr_index];
                            fitness[arr_index] += loc_mod;
                            break;
                        case 7:
                            loc_mod = getAimModifier(test_target, Mech.LOC_RLEG);
                            fitness[arr_index] = 2.0 * pen_counters[arr_index];
                            fitness[arr_index] += loc_mod;
                            break;
                        case 4: // Arm hits might damage some weapons, but not
                            // the best option
                            loc_mod = getAimModifier(test_target, Mech.LOC_LARM);
                            fitness[arr_index] = pen_counters[arr_index];
                            fitness[arr_index] += loc_mod;
                            break;
                        case 5:
                            loc_mod = getAimModifier(test_target, Mech.LOC_RARM);
                            fitness[arr_index] = pen_counters[arr_index];
                            fitness[arr_index] += loc_mod;
                    }
                }

                // If any location has been destroyed, adjust the location value
                // relative to its value

                if ((is_values[arr_index] <= 0) & (pen_counters[arr_index] > 0)) {

                    switch (arr_index) {
                        case 0: // Destroying the head is a hard kill and gets
                            // rid of the pilot, too
                            fitness[arr_index] += 3 * getAimModifier(
                                    test_target, Mech.LOC_HEAD);
                            break;
                        case 1: // Destroying the CT is a hard kill
                            fitness[arr_index] += 2 * getAimModifier(
                                    test_target, Mech.LOC_CT);
                            break;
                        case 2: // Destroying a side torso could be a soft kill
                            // or cripple
                            fitness[arr_index] += 1.5 * getAimModifier(
                                    test_target, Mech.LOC_LT);
                            break;
                        case 3:
                            fitness[arr_index] += 1.5 * getAimModifier(
                                    test_target, Mech.LOC_RT);
                            break;
                        case 6: // Destroying a leg is a mobility kill
                            fitness[arr_index] += 1.5 * getAimModifier(
                                    test_target, Mech.LOC_LLEG);
                            break;
                        case 7:
                            fitness[arr_index] += 1.5 * getAimModifier(
                                    test_target, Mech.LOC_RLEG);
                            break;
                        case 4: // Destroying an arm can cripple a Mech, but not
                            // the best option
                            fitness[arr_index] += getAimModifier(test_target,
                                                                 Mech.LOC_LARM);
                            break;
                        case 5:
                            fitness[arr_index] += getAimModifier(test_target,
                                                                 Mech.LOC_RARM);
                            break;

                    }
                }

            }

            // Get the best target location, including the head

            refactored_damage = fitness[1];
            for (int arr_index = 0; arr_index < 8; arr_index++) {
                if (fitness[arr_index] > refactored_damage) {
                    refactored_damage = fitness[arr_index];
                    switch (arr_index) {
                        case 0:
                            best_loc_head = Mech.LOC_HEAD;
                            break;
                        case 2: // case 1 is CT, which was initialized as
                            // default
                            best_loc_head = Mech.LOC_LT;
                            break;
                        case 3:
                            best_loc_head = Mech.LOC_RT;
                            break;
                        case 4:
                            best_loc_head = Mech.LOC_LARM;
                            break;
                        case 5:
                            best_loc_head = Mech.LOC_RARM;
                            break;
                        case 6:
                            best_loc_head = Mech.LOC_LLEG;
                            break;
                        case 7:
                            best_loc_head = Mech.LOC_RLEG;
                            break;
                        default:
                            best_loc_head = Mech.LOC_CT;
                    }
                }
            }

            // Get the best target location, not including the head
            int temp_index = 1;
            refactored_damage = fitness[1];
            for (int arr_index = 2; arr_index < 8; arr_index++) {
                if (fitness[arr_index] > refactored_damage) {
                    refactored_damage = fitness[arr_index];
                    temp_index = arr_index;
                    switch (arr_index) {
                        case 2: // case 1 is CT, which was set as default
                            best_loc = Mech.LOC_LT;
                            break;
                        case 3:
                            best_loc = Mech.LOC_RT;
                            break;
                        case 4:
                            best_loc = Mech.LOC_LARM;
                            break;
                        case 5:
                            best_loc = Mech.LOC_RARM;
                            break;
                        case 6:
                            best_loc = Mech.LOC_LLEG;
                            break;
                        case 7:
                            best_loc = Mech.LOC_RLEG;
                            break;
                        default:
                            best_loc = Mech.LOC_CT;
                    }
                }
            }

            // For all weapon attack actions

            for (EntityAction entityAction : atk_action_list) {
                aimed_attack = (WeaponAttackAction) entityAction;

                // If the target of the action is the current target

                if (aimed_attack.getTargetId() == test_target) {

                    // If the weapon aim mode is set to use a tcomp

                    if (aimed_attack.getAimingMode() == IAimingModes.AIM_MODE_TARG_COMP) {

                        // If the location is at least close to being breached
                        // or the target is immobile

                        if (values[temp_index] <= Compute.randomInt(5)) {
                            aimed_attack.setAimedLocation(best_loc);
                        } else {
                            aimed_attack
                                    .setAimingMode(IAimingModes.AIM_MODE_NONE);
                            aimed_attack.setAimedLocation(Entity.LOC_NONE);
                        }

                    }

                    // If the weapon aim mode is set for immobile aim

                    if (aimed_attack.getAimingMode() == IAimingModes.AIM_MODE_IMMOBILE) {
                        aimed_attack.setAimedLocation(best_loc_head);
                    }

                }

            }

            // Any targets after this are secondary targets. Use secondary odds
            // and damage.

            is_primary_target = false;
        }
    }

    private double getAimModifier(int target_id, int location) {

        double loc_total;

        // TODO: change the factor of 0.1 to float depending on critical item
        // type

        loc_total = 0.1 * game.getEntity(target_id).getHittableCriticals(
                location);

        return loc_total;
    }

    @Override
    protected void checkMoral() {
        // unused.
    }
}
