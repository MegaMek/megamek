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
import java.util.Iterator;
import java.util.Vector;

import megamek.client.bot.PhysicalOption;
import megamek.common.Aero;
import megamek.common.AmmoType;
import megamek.common.BattleArmor;
import megamek.common.BipedMech;
import megamek.common.Compute;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.EntityMovementType;
import megamek.common.EntityWeightClass;
import megamek.common.GunEmplacement;
import megamek.common.IGame;
import megamek.common.IHex;
import megamek.common.Infantry;
import megamek.common.LosEffects;
import megamek.common.Mech;
import megamek.common.MechWarrior;
import megamek.common.Mounted;
import megamek.common.MovePath;
import megamek.common.MoveStep;
import megamek.common.RangeType;
import megamek.common.Tank;
import megamek.common.TargetRoll;
import megamek.common.Targetable;
import megamek.common.Terrains;
import megamek.common.ToHitData;
import megamek.common.VTOL;
import megamek.common.WeaponType;
import megamek.common.actions.EntityAction;
import megamek.common.actions.KickAttackAction;
import megamek.common.actions.PhysicalAttackAction;
import megamek.common.actions.PunchAttackAction;
import megamek.common.actions.TorsoTwistAction;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.util.LogLevel;
import megamek.common.util.Logger;
import megamek.common.weapons.StopSwarmAttack;
import megamek.common.weapons.infantry.InfantryWeapon;

/**
 * FireControl selects which weapons a unit wants to fire and at whom Pay
 * attention to the difference between "guess" and "get". Guess will be much
 * faster, but inaccurate
 */
public class FireControl {

    private static Princess owner;

    public FireControl(Princess owningPrincess) {
        owner = owningPrincess;
    }

    /**
     * WeaponFireInfo is a wrapper around a WeaponAttackAction that includes
     * probability to hit and expected damage
     */
    public class WeaponFireInfo {
        private WeaponAttackAction action;
        public Entity shooter;
        public Targetable target;
        public Mounted weapon;
        public double prob_to_hit;
        public int heat;
        public double max_damage;
        public double expected_damage_on_hit;
        public int damage_direction; // direction damage is coming from relative
        // to target
        public ToHitData to_hit;
        public double expected_criticals;
        public double kill_probability; // probability to destroy CT or HEAD
        // (ignores criticals)

        public double getExpectedDamage() {
            return prob_to_hit * expected_damage_on_hit;
        }

        /**
         * This constructs a WeaponFireInfo using an actual WeaponAttackAction
         * with real to hit values
         */
        WeaponFireInfo(Entity sshooter, Targetable ttarget, Mounted wep,
                       IGame game) {
            final String METHOD_NAME = "WeaponFireInfo(Entity, Targetable, Mounted)";
            Logger.methodBegin(getClass(), METHOD_NAME);

            try {
                shooter = sshooter;
                weapon = wep;
                target = ttarget;
                action = new WeaponAttackAction(shooter.getId(),
                                                ttarget.getTargetType(), ttarget.getTargetId(),
                                                shooter.getEquipmentNum(weapon));
                to_hit = action.toHit(game);
                if (ttarget instanceof Entity) {
                    Entity etarget = (Entity) ttarget;
                    // action = new WeaponAttackAction(shooter.getId(),
                    // etarget.getId(),
                    // shooter.getEquipmentNum(weapon));
                    int fromdir = target.getPosition().direction(
                            shooter.getPosition());
                    damage_direction = ((fromdir - etarget.getFacing()) + 6) % 6;
                }
                initDamage(game);
            } finally {
                Logger.methodEnd(getClass(), METHOD_NAME);
            }
        }

        /**
         * This constructs a WeaponFireInfo using the best guess of how likely
         * this is to hit without actually constructing the weaponattackaction
         */
        WeaponFireInfo(Entity sshooter, EntityState shooterState,
                       Targetable ttarget, EntityState targetState, Mounted wep,
                       IGame game) {
            final String METHOD_NAME = "WeaponFireInfo(Entity, EntityState, Targetable, EntityState, Mounted, IGame)";
            Logger.methodBegin(getClass(), METHOD_NAME);

            try {
                if (shooterState == null) {
                    shooterState = new EntityState(sshooter);
                }
                if (targetState == null) {
                    targetState = new EntityState(ttarget);
                }
                shooter = sshooter;
                weapon = wep;
                target = ttarget;
                // action = null;
                // warning, this action has the wrong to-hit, since shooter is
                // likely somewhere else
                action = new WeaponAttackAction(shooter.getId(),
                                                ttarget.getTargetType(), ttarget.getTargetId(),
                                                shooter.getEquipmentNum(weapon));
                // action = new WeaponAttackAction(shooter.getId(),ttarget.getId(),
                // shooter.getEquipmentNum(weapon));
                to_hit = guessToHitModifier(shooter, shooterState, ttarget,
                                            targetState, wep, game);
                int fromdir = targetState.getPosition()
                                          .direction(shooterState.getPosition());
                damage_direction = ((fromdir - targetState.getFacing()) + 6) % 6;
                initDamage(game);
            } finally {
                Logger.methodEnd(getClass(), METHOD_NAME);
            }
        }

        /**
         * This constructs a WeaponFireInfo using the best guess of how likely
         * an aerospace unit using a strike attack will hit, without actually
         * constructing the weaponattackaction
         *
         * @param sshooter
         * @param shooter_path
         * @param ttarget
         * @param targetState
         * @param wep
         * @param game
         */
        WeaponFireInfo(Entity sshooter, MovePath shooter_path,
                       Targetable ttarget, EntityState targetState, Mounted wep,
                       IGame game, boolean assume_under_flight_path) {
            final String METHOD_NAME = "WeaponFireInfo(Entity, MovePath, Targetable, EntityState, Mounted, IGame, " +
                                       "boolean)";
            Logger.methodBegin(getClass(), METHOD_NAME);

            try {
                if (targetState == null) {
                    targetState = new EntityState(ttarget);
                }
                shooter = sshooter;
                weapon = wep;
                target = ttarget;
                // warning, this action has the wrong to-hit, since shooter is
                // likely somewhere else
                // action = new WeaponAttackAction(shooter.getId(),ttarget.getId(),
                // shooter.getEquipmentNum(weapon));
                action = new WeaponAttackAction(shooter.getId(),
                                                ttarget.getTargetType(), ttarget.getTargetId(),
                                                shooter.getEquipmentNum(weapon));
                to_hit = guessAirToGroundStrikeToHitModifier(shooter, ttarget,
                                                             targetState, shooter_path, wep, game,
                                                             assume_under_flight_path);
                int fromdir = targetState.getPosition()
                                          .direction(shooter.getPosition());
                damage_direction = ((fromdir - targetState.getFacing()) + 6) % 6;
                initDamage(game);
            } finally {
                Logger.methodEnd(getClass(), METHOD_NAME);
            }
        }

        /*
         * Helper function that calculates expected damage
         */
        private void initDamage(IGame game) {
            final String METHOD_NAME = "initDamage(IGame)";
            Logger.methodBegin(getClass(), METHOD_NAME);

            try {
                if (to_hit.getValue() > 12) {
                    prob_to_hit = 0;
                    max_damage = 0;
                    heat = 0;
                    expected_criticals = 0;
                    kill_probability = 0;
                    expected_damage_on_hit = 0;
                    return;
                }
                prob_to_hit = Compute.oddsAbove(to_hit.getValue()) / 100.0;
                heat = ((WeaponType) weapon.getType()).getHeat();
                // if(action!=null) {
                if (target instanceof Entity) {
                    expected_damage_on_hit = Compute.getExpectedDamage(game,
                                                                       action, true);
                } else {
                    expected_damage_on_hit = ((WeaponType) weapon.getType())
                            .getDamage();
                }
                max_damage = expected_damage_on_hit;
                /*
                 * } else { if((weapon.getType() instanceof
                 * InfantryWeapon)&&(shooter instanceof Infantry)) {
                 * max_damage=((InfantryWeapon
                 * )(weapon.getType())).getInfantryDamage(
                 * )*((Infantry)shooter).getShootingStrength();
                 * expected_damage_on_hit=max_damage/2.0; //ignoring cluster hits }
                 * else if(((WeaponType)weapon.getType()).getDamage()==WeaponType.
                 * DAMAGE_BY_CLUSTERTABLE) {
                 * max_damage=((WeaponType)weapon.getType()).getRackSize(); //I
                 * think this is the right amount
                 * expected_damage_on_hit=max_damage/2; //not true. too lazy to
                 * calculate real value } else {
                 * max_damage=((WeaponType)weapon.getType()).getDamage();
                 * expected_damage_on_hit=max_damage; } if(shooter instanceof
                 * Infantry) { //each member of infantry squads get to shoot
                 * max_damage*=((Infantry)shooter).getShootingStrength();
                 * expected_damage_on_hit=max_damage/2; } }
                 */
                // now guess how many critical hits will be done
                expected_criticals = 0;
                kill_probability = 0;
                if (target instanceof Mech) {
                    Mech mtarget = (Mech) target;
                    // for(int i=Mech.LOC_HEAD;i<=Mech.LOC_LLEG;i++) {
                    for (int i = 0; i <= 7; i++) {
                        int hitloc = i;
                        while (mtarget.isLocationBad(hitloc)
                               && (hitloc != Mech.LOC_CT)) {
                            hitloc++;
                            if (hitloc >= 7) {
                                hitloc = 0;
                            }
                            hitloc = Mech.getInnerLocation(hitloc);
                        }
                        double hprob = ProbabilityCalculator.getHitProbability(
                                damage_direction, hitloc);
                        int target_armor = mtarget.getArmor(hitloc,
                                                            (damage_direction == 3 ? true : false));
                        int target_internals = mtarget.getInternal(hitloc);
                        if (target_armor < 0) {
                            target_armor = 0; // ignore NA or Destroyed cases
                        }
                        if (target_internals < 0) {
                            target_internals = 0;
                        }
                        // System.err.println("HP Calc: hloc: "+Integer.toString(hitloc)+
                        // " hprob "+Double.toString(hprob)+
                        // " target armor "+Integer.toString(target_armor)+
                        // " target internals "+Integer.toString(target_internals)+
                        // " expected_damage "+Double.toString(expected_damage_on_hit));
                        // destroying counts as a critical hit
                        if (expected_damage_on_hit > (target_armor + target_internals)) {
                            expected_criticals += hprob * prob_to_hit;
                            if ((hitloc == Mech.LOC_HEAD)
                                || (hitloc == Mech.LOC_CT)) {
                                kill_probability += hprob * prob_to_hit;
                            }
                        } else if (expected_damage_on_hit > (target_armor)) {
                            expected_criticals += hprob
                                                  * ProbabilityCalculator
                                    .getExpectedCriticalHitCount()
                                                  * prob_to_hit;
                        }
                    }
                    // there's always the chance of rolling a '2'
                    expected_criticals += 0.028
                                          * ProbabilityCalculator.getExpectedCriticalHitCount()
                                          * prob_to_hit;
                }
            } finally {
                Logger.methodEnd(getClass(), METHOD_NAME);
            }
        }

        WeaponAttackAction getWeaponAttackAction(IGame game) {
            final String METHOD_NAME = "getWeaponAttackAction(IGame)";
            Logger.methodBegin(getClass(), METHOD_NAME);

            try {
                if (action != null) {
                    return action;
                }
                if (target instanceof Entity) {
                    action = new WeaponAttackAction(shooter.getId(),
                                                    ((Entity) target).getId(),
                                                    shooter.getEquipmentNum(weapon));
                }
                prob_to_hit = Compute.oddsAbove(action.toHit(game).getValue()) / 100.0;
                return action;
            } finally {
                Logger.methodEnd(getClass(), METHOD_NAME);
            }
        }

        String getDebugDescription() {
            return weapon.getName() + " P_hit: " + Double.toString(prob_to_hit)
                   + " Max Dam: " + Double.toString(max_damage)
                   + " Exp. Dam: " + Double.toString(expected_damage_on_hit)
                   + " Num Crits: " + Double.toString(expected_criticals)
                   + " Kill Prob: " + kill_probability;

        }

    }

    /**
     * FiringPlan is a series of WeaponFireInfos describing a full attack turn
     */
    public class FiringPlan extends ArrayList<WeaponFireInfo> {
        /**
         *
         */
        private static final long serialVersionUID = 8938385222775928559L;
        int twist;
        public double utility; // calculated elsewhere

        FiringPlan() {
            twist = 0;
            utility = 0;
        }

        int getHeat() {
            final String METHOD_NAME = "getHeat()";
            Logger.methodBegin(getClass(), METHOD_NAME);

            try {
                int heat = 0;
                for (WeaponFireInfo f : this) {
                    heat += f.heat;
                }
                return heat;
            } finally {
                Logger.methodEnd(getClass(), METHOD_NAME);
            }
        }

        double getExpectedDamage() {
            final String METHOD_NAME = "getExpectedDamage()";
            Logger.methodBegin(getClass(), METHOD_NAME);

            try {
                double exdam = 0;
                for (WeaponFireInfo f : this) {
                    exdam += f.expected_damage_on_hit * f.prob_to_hit;
                }
                return exdam;
            } finally {
                Logger.methodEnd(getClass(), METHOD_NAME);
            }
        }

        double getExpectedCriticals() {
            final String METHOD_NAME = "getExpectedCriticals()";
            Logger.methodBegin(getClass(), METHOD_NAME);

            try {
                double expcrit = 0;
                for (WeaponFireInfo f : this) {
                    expcrit += f.expected_criticals;
                }
                return expcrit;
            } finally {
                Logger.methodEnd(getClass(), METHOD_NAME);
            }
        }

        double getKillProbability() {
            final String METHOD_NAME = "getKillProbability()";
            Logger.methodBegin(getClass(), METHOD_NAME);

            try {
                double killprob = 0;
                for (WeaponFireInfo f : this) {
                    killprob = killprob + ((1 - killprob) * f.kill_probability);
                }
                return killprob;
            } finally {
                Logger.methodEnd(getClass(), METHOD_NAME);
            }
        }

        boolean containsWeapon(Mounted wep) {
            final String METHOD_NAME = "containsWeapon(Mounted)";
            Logger.methodBegin(getClass(), METHOD_NAME);

            try {
                for (WeaponFireInfo f : this) {
                    if (f.weapon == wep) {
                        return true;
                    }
                }
                return false;
            } finally {
                Logger.methodEnd(getClass(), METHOD_NAME);
            }
        }

        public Vector<EntityAction> getEntityActionVector(IGame game) {
            final String METHOD_NAME = "getEntiyActionVector(IGame)";
            Logger.methodBegin(getClass(), METHOD_NAME);

            try {
                Vector<EntityAction> ret = new Vector<EntityAction>();
                if (size() == 0) {
                    return ret;
                }
                if (twist == -1) {
                    ret.add(new TorsoTwistAction(get(0).shooter.getId(),
                                                 correct_facing(get(0).shooter.getFacing() - 1)));
                } else if (twist == +1) {
                    ret.add(new TorsoTwistAction(get(0).shooter.getId(),
                                                 correct_facing(get(0).shooter.getFacing() + 1)));
                }
                for (WeaponFireInfo f : this) {
                    ret.add(f.getWeaponAttackAction(game));
                }
                return ret;
            } finally {
                Logger.methodEnd(getClass(), METHOD_NAME);
            }
        }

        /*
         * Returns a string describing the firing actions, their likelyhood to
         * hit, and damage
         */
        String getDebugDescription(boolean detailed) {
            if (size() == 0) {
                return "Empty FiringPlan!";
            }
            String ret = new String("Firing Plan for "
                                    + get(0).shooter.getChassis() + " at "
                                    + get(0).target.getDisplayName() + " "
                                    + Integer.toString(size()) + " weapons fired \n");
            if (detailed) {
                for (WeaponFireInfo wfi : this) {
                    ret += wfi.getDebugDescription() + "\n";
                }
            }
            ret += "Total Expected Damage="
                   + Double.toString(getExpectedDamage()) + "\n";
            ret += "Total Expected Criticals="
                   + Double.toString(getExpectedCriticals()) + "\n";
            ret += "Kill Probability=" + Double.toString(getKillProbability())
                   + "\n";
            return ret;
        }

    }

    /**
     * PhysicalInfo is a wrapper around a PhysicalAttackAction that includes
     * probability to hit and expected damage
     */
    public static class PhysicalInfo {
        public Entity shooter;
        public Targetable target;
        public PhysicalAttackAction action;
        public PhysicalAttackType attack_type;
        public ToHitData to_hit;
        public double prob_to_hit;
        public double max_damage;
        public double expected_damage_on_hit;
        public int damage_direction; // direction damage is coming from relative
        // to target
        public double expected_criticals;
        public double kill_probability; // probability to destroy CT or HEAD
        // (ignores criticals)
        public double utility; // filled out externally

        public double getExpectedDamage() {
            return prob_to_hit * expected_damage_on_hit;
        }

        ;

        PhysicalInfo(Entity sshooter, EntityState shooterState,
                     Targetable ttarget, EntityState targetState,
                     PhysicalAttackType atype, IGame game) {
            final String METHOD_NAME = "PhysicalInfo(Entity, EntityState, Targetable, EntityState, " +
                                       "PhysicalAttackType, IGame)";
            Logger.methodBegin(getClass(), METHOD_NAME);

            try {
                shooter = sshooter;
                target = ttarget;
                if (shooterState == null) {
                    shooterState = new EntityState(sshooter);
                }
                if (targetState == null) {
                    targetState = new EntityState(ttarget);
                }
                attack_type = atype;
                to_hit = guessToHitModifier_Physical(shooter, shooterState,
                                                     target, targetState, attack_type, game);
                int fromdir = targetState.getPosition()
                                          .direction(shooterState.getPosition());
                damage_direction = ((fromdir - targetState.getFacing()) + 6) % 6;
                if ((atype == PhysicalAttackType.LEFT_PUNCH)
                    || (atype == PhysicalAttackType.RIGHT_PUNCH)) {
                    if (sshooter instanceof BipedMech) {
                        max_damage = (int) Math.ceil(shooter.getWeight() / 10.0);
                    } else {
                        max_damage = 0;
                    }
                } else { // assuming kick
                    max_damage = (int) Math.floor(shooter.getWeight() / 5.0);
                }
                initDamage();
            } finally {
                Logger.methodEnd(getClass(), METHOD_NAME);
            }
        }

        PhysicalInfo(Entity sshooter, Targetable ttarget,
                     PhysicalAttackType atype, IGame game) {
            final String METHOD_NAME = "PhysicalInfo(Entity, Targetable, PhysicalAttackType, IGame)";
            Logger.methodBegin(getClass(), METHOD_NAME);

            try {
                shooter = sshooter;
                target = ttarget;
                attack_type = atype;
                int fromdir = target.getPosition().direction(shooter.getPosition());
                if (target instanceof Entity) {
                    damage_direction = ((fromdir - ((Entity) target).getFacing()) + 6) % 6;
                } else {
                    damage_direction = 0;
                }
                if ((attack_type == PhysicalAttackType.RIGHT_PUNCH)
                    || (attack_type == PhysicalAttackType.LEFT_PUNCH)) {
                    int armid = attack_type == PhysicalAttackType.RIGHT_PUNCH ? 2
                                                                              : 1;
                    // action = new PunchAttackAction(shooter.getId(),
                    // target.getId(),
                    // armid);
                    action = new PunchAttackAction(shooter.getId(),
                                                   target.getTargetType(), target.getTargetId(), armid,
                                                   false, false);
                    to_hit = ((PunchAttackAction) action).toHit(game);
                    if (sshooter instanceof BipedMech) {
                        max_damage = PunchAttackAction.getDamageFor(shooter, armid,
                                                                    target instanceof Infantry);
                    } else {
                        max_damage = 0;
                    }
                } else { // assume kick
                    int legid = attack_type == PhysicalAttackType.RIGHT_KICK ? 2
                                                                             : 1;
                    // action = new KickAttackAction(shooter.getId(),
                    // target.getId(),
                    // legid);
                    action = new KickAttackAction(shooter.getId(),
                                                  target.getTargetType(), target.getTargetId(), legid);
                    to_hit = ((KickAttackAction) action).toHit(game);
                    max_damage = KickAttackAction.getDamageFor(shooter, legid,
                                                               target instanceof Infantry);
                }
                initDamage();
            } finally {
                Logger.methodEnd(getClass(), METHOD_NAME);
            }
        }

        /**
         * Helper function to determine damage and criticals
         */
        public void initDamage() {
            final String METHOD_NAME = "initDamage()";
            Logger.methodBegin(getClass(), METHOD_NAME);

            try {
                prob_to_hit = Compute.oddsAbove(to_hit.getValue());
                expected_damage_on_hit = max_damage;
                // now guess how many critical hits will be done
                expected_criticals = 0;
                kill_probability = 0;
                if (target instanceof Mech) {
                    Mech mtarget = (Mech) target;
                    for (int i = 0; i <= 7; i++) {
                        int hitloc = i;
                        while (mtarget.isLocationBad(hitloc)
                               && (hitloc != Mech.LOC_CT)) {
                            hitloc++;
                            if (hitloc >= 7) {
                                hitloc = 0;
                            }
                            hitloc = Mech.getInnerLocation(hitloc);
                        }
                        double hprob = 0;
                        if ((attack_type == PhysicalAttackType.RIGHT_PUNCH)
                            || (attack_type == PhysicalAttackType.LEFT_PUNCH)) {
                            hprob = ProbabilityCalculator.getHitProbability_Punch(
                                    damage_direction, hitloc);
                        } else { // assume kick
                            hprob = ProbabilityCalculator.getHitProbability_Kick(
                                    damage_direction, hitloc);
                        }
                        int target_armor = mtarget.getArmor(hitloc,
                                                            (damage_direction == 3 ? true : false));
                        int target_internals = mtarget.getInternal(hitloc);
                        if (target_armor < 0) {
                            target_armor = 0; // ignore NA or Destroyed cases
                        }
                        if (target_internals < 0) {
                            target_internals = 0;
                        }
                        if (expected_damage_on_hit > ((target_armor + target_internals))) {
                            expected_criticals += hprob * prob_to_hit;
                            if ((hitloc == Mech.LOC_HEAD)
                                || (hitloc == Mech.LOC_CT)) {
                                kill_probability += hprob * prob_to_hit;
                            }
                        } else if (expected_damage_on_hit > (target_armor)) {
                            expected_criticals += hprob
                                                  * ProbabilityCalculator
                                    .getExpectedCriticalHitCount()
                                                  * prob_to_hit;
                        }
                    }
                }
                // there's always the chance of rolling a '2'
                expected_criticals += 0.028
                                      * ProbabilityCalculator.getExpectedCriticalHitCount()
                                      * prob_to_hit;
            } finally {
                Logger.methodEnd(getClass(), METHOD_NAME);
            }
        }

        /**
         * Current bot code requires physical attacks to be given as 'physical
         * option'. This does the necessary conversion
         */
        public PhysicalOption getAsPhysicalOption() {
            final String METHOD_NAME = "getAsPhysicalOption()";
            Logger.methodBegin(getClass(), METHOD_NAME);

            try {
                int option_integer = 0;
                if (attack_type == PhysicalAttackType.RIGHT_PUNCH) {
                    option_integer = PhysicalOption.PUNCH_RIGHT;
                }
                if (attack_type == PhysicalAttackType.LEFT_PUNCH) {
                    option_integer = PhysicalOption.PUNCH_LEFT;
                }
                if (attack_type == PhysicalAttackType.RIGHT_KICK) {
                    option_integer = PhysicalOption.KICK_RIGHT;
                }
                if (attack_type == PhysicalAttackType.LEFT_KICK) {
                    option_integer = PhysicalOption.KICK_LEFT;
                }
                PhysicalOption physical_attack = new PhysicalOption(shooter,
                                                                    target, 0, option_integer, null);
                return physical_attack;
            } finally {
                Logger.methodEnd(getClass(), METHOD_NAME);
            }
        }
    }

    /**
     * Gets the toHit modifier common to both weapon and physical attacks
     */
    public static ToHitData guessToHitModifierHelper_AnyAttack(Entity shooter,
                                                               EntityState shooterState, Targetable target,
                                                               EntityState targetState, IGame game) {
        final String METHOD_NAME = "guessToHitModifierHelper_AnyAttack(Entity, EntityState, Targetable, EntityState, " +
                                   "IGame)";
        Logger.methodBegin(FireControl.class, METHOD_NAME);

        try {
            if (shooterState == null) {
                shooterState = new EntityState(shooter);
            }
            if (targetState == null) {
                targetState = new EntityState(target);
            }

            ToHitData tohit = new ToHitData();
            // If people are moving or lying down, there are consequences
            tohit.append(Compute.getAttackerMovementModifier(game, shooter.getId(),
                                                             shooterState.getMovementType()));
            tohit.append(Compute.getTargetMovementModifier(
                    targetState.getHexesMoved(), targetState.isJumping(), target instanceof VTOL));
            if (shooterState.isProne()) {
                tohit.addModifier(2, "attacker prone");
            }
            if (targetState.isImmobile()) {
                tohit.addModifier(-4, "target immobile");
            }
            if (targetState.getMovementType() == EntityMovementType.MOVE_SKID) {
                tohit.addModifier(2, "target skidded");
            }
            if (game.getOptions().booleanOption("tacops_standing_still") && (targetState.getMovementType() ==
                                                                             EntityMovementType.MOVE_NONE)
                && !targetState.isImmobile()
                && !((target instanceof Infantry) || (target instanceof VTOL) || (target instanceof GunEmplacement))) {
                tohit.addModifier(-1, "target didn't move");
            }

            // did the target sprint?
            if (targetState.getMovementType() == EntityMovementType.MOVE_SPRINT) {
                tohit.addModifier(-1, "target sprinted");
            }

            // terrain modifiers, since "compute" won't let me do these remotely
            IHex target_hex = game.getBoard().getHex(targetState.getPosition());
            int woodslevel = target_hex.terrainLevel(Terrains.WOODS);
            if (target_hex.terrainLevel(Terrains.JUNGLE) > woodslevel) {
                woodslevel = target_hex.terrainLevel(Terrains.JUNGLE);
            }
            if (woodslevel == 1) {
                tohit.addModifier(1, " woods");
            }
            if (woodslevel == 2) {
                tohit.addModifier(2, " woods");
            }
            if (woodslevel == 3) {
                tohit.addModifier(3, " woods");
            }
            int distance = shooterState.getPosition().distance(targetState.getPosition());
            if (targetState.isProne() && (distance > 1)) {
                tohit.addModifier(1, "target prone and at range");
            } else if (targetState.isProne() && (distance == 1)) {
                tohit.addModifier(-2, "target prone and adjacent");
            }
            boolean isShooterInfantry = (shooter instanceof Infantry);

            if ((!isShooterInfantry) && (target instanceof BattleArmor)) {
                tohit.addModifier(1, " battle armor target");
            } else if ((!isShooterInfantry) && ((target instanceof Infantry))) {
                tohit.addModifier(1, " infantry target");
            }
            if ((!isShooterInfantry) && (target instanceof MechWarrior)) {
                tohit.addModifier(2, " ejected mechwarrior target");
            }

            return tohit;
        } finally {
            Logger.methodEnd(FireControl.class, METHOD_NAME);
        }
    }

    public enum PhysicalAttackType {
        LEFT_KICK, RIGHT_KICK, LEFT_PUNCH, RIGHT_PUNCH
    }

    ;

    /**
     * Makes a rather poor guess as to what the to hit modifier will be with a
     * physical attack.
     */
    public static ToHitData guessToHitModifier_Physical(Entity shooter,
                                                        EntityState shooterState, Targetable target,
                                                        EntityState targetState, PhysicalAttackType attack_type,
                                                        IGame game) {
        final String METHOD_NAME = "guessToHitModifier(Entity, EntityState, Targetable, EntityState, " +
                                   "PhysicalAttackType, IGame)";
        Logger.methodBegin(FireControl.class, METHOD_NAME);

        try {
            if (!(shooter instanceof Mech)) {
                return new ToHitData(TargetRoll.IMPOSSIBLE,
                                     "Non mechs don't make physical attacks");
            }
            // Base to hit is piloting skill +2
            ToHitData tohit = new ToHitData();
            if (shooterState == null) {
                shooterState = new EntityState(shooter);
            }
            if (targetState == null) {
                targetState = new EntityState(target);
            }
            int distance = shooterState.getPosition().distance(targetState.getPosition());
            if (distance > 1) {
                return new ToHitData(TargetRoll.IMPOSSIBLE, "Can't hit that far");
            }

            tohit.append(guessToHitModifierHelper_AnyAttack(shooter, shooterState,
                                                            target, targetState, game));
            // check if target is within arc
            int arc = 0;
            if (attack_type == PhysicalAttackType.LEFT_PUNCH) {
                arc = Compute.ARC_LEFTARM;
            } else if (attack_type == PhysicalAttackType.RIGHT_PUNCH) {
                arc = Compute.ARC_RIGHTARM;
            } else {
                arc = Compute.ARC_FORWARD; // assume kick
            }
            if (!(Compute.isInArc(shooterState.getPosition(),
                                  shooterState.getSecondaryFacing(), targetState.getPosition(), arc) || (distance == 0))) {
                return new ToHitData(TargetRoll.IMPOSSIBLE, "Target not in arc");
            }

            IHex attHex = game.getBoard().getHex(shooterState.getPosition());
            IHex targHex = game.getBoard().getHex(targetState.getPosition());
            final int attackerElevation = shooter.getElevation()
                                          + attHex.getElevation();
            final int attackerHeight = shooter.absHeight() + attHex.getElevation();
            final int targetElevation = target.getElevation()
                                        + targHex.getElevation();
            final int targetHeight = targetElevation + target.getHeight();
            if ((attack_type == PhysicalAttackType.LEFT_PUNCH)
                || (attack_type == PhysicalAttackType.RIGHT_PUNCH)) {
                if ((attackerHeight < targetElevation)
                    || (attackerHeight > targetHeight)) {
                    return new ToHitData(TargetRoll.IMPOSSIBLE,
                                         "Target elevation not in range");
                }

                if (shooterState.isProne()) {
                    return new ToHitData(TargetRoll.IMPOSSIBLE,
                                         "can't punch while prone");
                }
                if (target instanceof Infantry) {
                    return new ToHitData(TargetRoll.IMPOSSIBLE,
                                         "can't punch infantry");
                }
                int armLoc = attack_type == PhysicalAttackType.RIGHT_PUNCH ? Mech.LOC_RARM
                                                                           : Mech.LOC_LARM;
                if (shooter.isLocationBad(armLoc)) {
                    return new ToHitData(TargetRoll.IMPOSSIBLE, "Your arm's off!");
                }
                if (!shooter.hasWorkingSystem(Mech.ACTUATOR_SHOULDER, armLoc)) {
                    return new ToHitData(TargetRoll.IMPOSSIBLE,
                                         "shoulder destroyed");
                }
                tohit.addModifier(shooter.getCrew().getPiloting(), "base");
                if (!shooter.hasWorkingSystem(Mech.ACTUATOR_UPPER_ARM, armLoc)) {
                    tohit.addModifier(2, "Upper arm actuator destroyed");
                }
                if (!shooter.hasWorkingSystem(Mech.ACTUATOR_LOWER_ARM, armLoc)) {
                    tohit.addModifier(2, "Lower arm actuator missing or destroyed");
                }
                if (!shooter.hasWorkingSystem(Mech.ACTUATOR_HAND, armLoc)) {
                    tohit.addModifier(1, "Hand actuator missing or destroyed");
                }
            } else // assuming kick
            {
                tohit.addModifier(shooter.getCrew().getPiloting() - 2, "base");
                if (shooterState.isProne()) {
                    return new ToHitData(TargetRoll.IMPOSSIBLE,
                                         "Can't kick while prone");
                }
                if (target instanceof Infantry) {
                    if (distance == 0) {
                        tohit.addModifier(3, "kicking infantry");
                    } else {
                        return new ToHitData(TargetRoll.IMPOSSIBLE,
                                             "Infantry too far away");
                    }
                }
                if ((attackerElevation < targetElevation)
                    || (attackerElevation > targetHeight)) {
                    return new ToHitData(TargetRoll.IMPOSSIBLE,
                                         "Target elevation not in range");
                }
                int legLoc = attack_type == PhysicalAttackType.RIGHT_KICK ? Mech.LOC_RLEG
                                                                          : Mech.LOC_LLEG;
                if (((Mech) shooter).hasHipCrit()) {
                    // if (!shooter.hasWorkingSystem(Mech.ACTUATOR_HIP,
                    // legLoc)||!shooter.hasWorkingSystem(Mech.ACTUATOR_HIP,otherLegLoc))
                    // {
                    return new ToHitData(TargetRoll.IMPOSSIBLE,
                                         "can't kick with broken hip");
                }
                if (!shooter.hasWorkingSystem(Mech.ACTUATOR_UPPER_LEG, legLoc)) {
                    tohit.addModifier(2, "Upper leg actuator destroyed");
                }
                if (!shooter.hasWorkingSystem(Mech.ACTUATOR_LOWER_LEG, legLoc)) {
                    tohit.addModifier(2, "Lower leg actuator destroyed");
                }
                if (!shooter.hasWorkingSystem(Mech.ACTUATOR_FOOT, legLoc)) {
                    tohit.addModifier(1, "Foot actuator destroyed");
                }
                if (game.getOptions().booleanOption("tacops_attack_physical_psr")) {
                    if (shooter.getWeightClass() == EntityWeightClass.WEIGHT_LIGHT) {
                        tohit.addModifier(-2, "Weight Class Attack Modifier");
                    } else if (shooter.getWeightClass() == EntityWeightClass.WEIGHT_MEDIUM) {
                        tohit.addModifier(-1, "Weight Class Attack Modifier");
                    }
                }

            }
            return tohit;
        } finally {
            Logger.methodEnd(FireControl.class, METHOD_NAME);
        }
    }

    /**
     * Makes an educated guess as to the to hit modifier with a weapon attack.
     * Does not actually place unit into desired position, because that is
     * exceptionally slow. Most of this is copied from WeaponAttack.
     */
    public static ToHitData guessToHitModifier(Entity shooter,
                                               EntityState shooterState, Targetable target,
                                               EntityState targetState, Mounted mw, IGame game) {
        final String METHOD_NAME = "guessToHitModifier(Entity, EntityState, Targetable, EntityState, Mounted, IGame)";
        Logger.methodBegin(FireControl.class, METHOD_NAME);

        try {
            if (shooterState == null) {
                shooterState = new EntityState(shooter);
            }
            if (targetState == null) {
                targetState = new EntityState(target);
            }
            // first check if the shot is impossible
            if (!mw.canFire()) {
                return new ToHitData(TargetRoll.IMPOSSIBLE, "weapon cannot fire");
            }
            if (((WeaponType) mw.getType()).ammoType != AmmoType.T_NA) {
                if (mw.getLinked() == null) {
                    return new ToHitData(TargetRoll.IMPOSSIBLE, "ammo is gone");
                }
                if (mw.getLinked().getUsableShotsLeft() == 0) {
                    return new ToHitData(TargetRoll.IMPOSSIBLE,
                                         "weapon out of ammo");
                }
            }
            if ((shooterState.isProne())
                && ((shooter.isLocationBad(Mech.LOC_RARM)) || (shooter
                                                                       .isLocationBad(Mech.LOC_LARM)))) {
                return new ToHitData(TargetRoll.IMPOSSIBLE,
                                     "prone and missing an arm.");
            }

            int shooterFacing = shooterState.getFacing();
            if (shooter.isSecondaryArcWeapon(shooter.getEquipmentNum(mw))) {
                shooterFacing = shooterState.getSecondaryFacing(); // check if torso
            }
            // twists affect
            // weapon
            boolean inarc = Compute.isInArc(shooterState.getPosition(), shooterFacing,
                                            targetState.getPosition(),
                                            shooter.getWeaponArc(shooter.getEquipmentNum(mw)));
            if (!inarc) {
                return new ToHitData(TargetRoll.IMPOSSIBLE, "not in arc");
            }
            // Find out a bit about the shooter and target
            boolean isShooterInfantry = (shooter instanceof Infantry);
            boolean isWeaponInfantry = ((WeaponType) mw.getType())
                    .hasFlag(WeaponType.F_INFANTRY);
            if ((shooterState.getPosition() == null) || (targetState.getPosition() == null)) {
                return new ToHitData(TargetRoll.AUTOMATIC_FAIL, "null position");
            }
            int distance = shooterState.getPosition().distance(targetState.getPosition());

            if ((distance == 0) && (!isShooterInfantry)) {
                return new ToHitData(TargetRoll.AUTOMATIC_FAIL,
                                     "noninfantry shooting with zero range");
            }
            // Base to hit is gunnery skill
            ToHitData tohit = new ToHitData(shooter.getCrew().getGunnery(),
                                            "gunnery skill");
            tohit.append(guessToHitModifierHelper_AnyAttack(shooter, shooterState,
                                                            target, targetState, game));
            // There is kindly already a class that will calculate line of sight for
            // me
            LosEffects loseffects = LosEffects.calculateLos(game, shooter.getId(),
                                                            target, shooterState.getPosition(), targetState.getPosition(),
                                                            false);
            // water is a separate loseffect
            IHex target_hex = game.getBoard().getHex(targetState.getPosition());
            if (target instanceof Entity) {
                if (target_hex.containsTerrain(Terrains.WATER)
                    && (target_hex.terrainLevel(Terrains.WATER) == 1)
                    && (((Entity) target).height() > 0)) {
                    loseffects.setTargetCover(loseffects.getTargetCover()
                                              | LosEffects.COVER_HORIZONTAL);
                }
            }
            tohit.append(loseffects.losModifiers(game));
            if ((tohit.getValue() == TargetRoll.IMPOSSIBLE)
                || (tohit.getValue() == TargetRoll.AUTOMATIC_FAIL)) {
                return tohit; // you can't hit what you can't see
            }
            // deal with some special cases
            if (((WeaponType) mw.getType()) instanceof StopSwarmAttack) {
                if (Entity.NONE == shooter.getSwarmTargetId()) {
                    return new ToHitData(TargetRoll.IMPOSSIBLE,
                                         "Not swarming a Mek.");
                } else {
                    return new ToHitData(TargetRoll.AUTOMATIC_SUCCESS,
                                         "stops swarming");
                }
            }
            if (shooter instanceof Tank) {
                int sensors = ((Tank) shooter).getSensorHits();
                if (sensors > 0) {
                    tohit.addModifier(sensors, "sensor damage");
                }
            }

            if (target instanceof Mech) {
                if (Infantry.SWARM_MEK.equals(mw.getType().getInternalName())) {
                    tohit.append(Compute.getSwarmMekBaseToHit(shooter,
                                                              (Entity) target, game));
                }
                if (Infantry.LEG_ATTACK.equals(mw.getType().getInternalName())) {
                    tohit.append(Compute.getLegAttackBaseToHit(shooter,
                                                               (Entity) target, game));
                }
            }
            if ((tohit.getValue() == TargetRoll.IMPOSSIBLE)
                || (tohit.getValue() == TargetRoll.AUTOMATIC_FAIL)) {
                return tohit;
            }
            // Now deal with range effects
            int range = RangeType.rangeBracket(distance,
                                               ((WeaponType) mw.getType()).getRanges(mw),
                                               game.getOptions().booleanOption("tacops_range"));
            // Aeros are 2x further for each altitude
            if (target instanceof Aero) {
                range += 2 * target.getAltitude();
            }
            if (!isWeaponInfantry) {
                if (range == RangeType.RANGE_SHORT) {
                    tohit.addModifier(0, "Short Range");
                } else if (range == RangeType.RANGE_MEDIUM) {
                    tohit.addModifier(2, "Medium Range");
                } else if (range == RangeType.RANGE_LONG) {
                    tohit.addModifier(4, "Long Range");
                } else if (range == RangeType.RANGE_MINIMUM) {
                    tohit.addModifier(
                            (((WeaponType) mw.getType()).getMinimumRange() - distance) + 1,
                            "Minimum Range");
                } else {
                    return new ToHitData(TargetRoll.AUTOMATIC_FAIL, "out of range"); // out
                    // of
                    // range
                }
            } else {
                tohit.append(Compute.getInfantryRangeMods(distance,
                                                          (InfantryWeapon) mw.getType()));
            }

            // let us not forget about heat
            if (shooter.getHeatFiringModifier() != 0) {
                tohit.addModifier(shooter.getHeatFiringModifier(), "heat");
            }
            // and damage
            tohit.append(Compute.getDamageWeaponMods(shooter, mw));
            // and finally some more special cases
            if (((WeaponType) mw.getType()).getToHitModifier() != 0) {
                tohit.addModifier(((WeaponType) mw.getType()).getToHitModifier(),
                                  "weapon to-hit");
            }
            if (((WeaponType) mw.getType()).getAmmoType() != AmmoType.T_NA) {
                AmmoType atype = (AmmoType) mw.getLinked().getType();
                if ((atype != null) && (atype.getToHitModifier() != 0)) {
                    tohit.addModifier(atype.getToHitModifier(),
                                      "ammunition to-hit modifier");
                }
            }
            if (shooter.hasTargComp()
                && ((WeaponType) mw.getType())
                    .hasFlag(WeaponType.F_DIRECT_FIRE)) {
                tohit.addModifier(-1, "targeting computer");
            }

            return tohit;
        } finally {
            Logger.methodEnd(FireControl.class, METHOD_NAME);
        }
    }

    /**
     * Makes an educated guess as to the to hit modifier by an aerospace unit
     * flying on a ground map doing a strike attack on a unit
     */
    public static ToHitData guessAirToGroundStrikeToHitModifier(Entity shooter,
                                                                Targetable target, EntityState target_state,
                                                                MovePath shooter_path,
                                                                Mounted mw, IGame game,
                                                                boolean assume_under_flight_plan) {
        final String METHOD_NAME = "guessAirToGroundStrikeToHitModifier(Entity, Targetable, EntityState, MovePath, " +
                                   "Mounted, IGame, boolean)";
        Logger.methodBegin(FireControl.class, METHOD_NAME);

        try {
            if (target_state == null) {
                target_state = new EntityState(target);
            }
            EntityState shooter_state = new EntityState(shooter);
            // first check if the shot is impossible
            if (!mw.canFire()) {
                return new ToHitData(TargetRoll.IMPOSSIBLE, "weapon cannot fire");
            }
            if (((WeaponType) mw.getType()).ammoType != AmmoType.T_NA) {
                if (mw.getLinked() == null) {
                    return new ToHitData(TargetRoll.IMPOSSIBLE, "ammo is gone");
                }
                if (mw.getLinked().getUsableShotsLeft() == 0) {
                    return new ToHitData(TargetRoll.IMPOSSIBLE,
                                         "weapon out of ammo");
                }
            }
            // check if target is even under our path
            if (!assume_under_flight_plan) {
                if (!isTargetUnderMovePath(shooter_path, target_state)) {
                    return new ToHitData(TargetRoll.IMPOSSIBLE,
                                         "target not under flight path");
                }
            }
            // Base to hit is gunnery skill
            ToHitData tohit = new ToHitData(shooter.getCrew().getGunnery(),
                                            "gunnery skill");
            tohit.append(guessToHitModifierHelper_AnyAttack(shooter, shooter_state,
                                                            target, target_state, game));
            // Additional penalty due to strike attack
            tohit.addModifier(+2, "strike attack");

            return tohit;
        } finally {
            Logger.methodEnd(FireControl.class, METHOD_NAME);
        }
    }

    /**
     * Checks if a target lies under a move path, to see if an aero unit can
     * attack it
     *
     * @param p            move path to check
     * @param targetState used for targets position
     * @return
     */
    public static boolean isTargetUnderMovePath(MovePath p,
                                                EntityState targetState) {
        final String METHOD_NAME = "isTargetUnderMovePath(MovePath, EntityState)";
        Logger.methodBegin(FireControl.class, METHOD_NAME);

        try {
            for (Enumeration<MoveStep> e = p.getSteps(); e.hasMoreElements(); ) {
                Coords cord = e.nextElement().getPosition();
                if (cord.equals(targetState.getPosition())) {
                    return true;
                }
            }
            return false;
        } finally {
            Logger.methodEnd(FireControl.class, METHOD_NAME);
        }
    }

    /**
     * Returns a list of enemies that lie under this flight path
     *
     * @param p
     * @param shooter
     * @param game
     * @return
     */
    ArrayList<Entity> getEnemiesUnderFlightPath(MovePath p, Entity shooter,
                                                IGame game) {
        final String METHOD_NAME = "getEnemiesUnderFlightPath(MovePath, Entity, IGame)";
        Logger.methodBegin(FireControl.class, METHOD_NAME);

        try {
            ArrayList<Entity> ret = new ArrayList<Entity>();
            for (Enumeration<MoveStep> e = p.getSteps(); e.hasMoreElements(); ) {
                Coords cord = e.nextElement().getPosition();
                Entity enemy = game.getFirstEnemyEntity(cord, shooter);
                if (enemy != null) {
                    ret.add(enemy);
                }
            }
            return ret;
        } finally {
            Logger.methodEnd(FireControl.class, METHOD_NAME);
        }
    }

    /**
     * Mostly for debugging, this returns a non-null string that describes how
     * the guess has failed to be perfectly accurate. or null if perfectly
     * accurate
     */
    String checkGuess(Entity shooter, Targetable target, Mounted mw, IGame game) {
        final String METHOD_NAME = "checkGuess(Entity, Targetable, Mounted, IGame)";
        Logger.methodBegin(FireControl.class, METHOD_NAME);

        try {

            if ((shooter instanceof Aero) ||
                (shooter.getPosition() == null) ||
                (target.getPosition() == null)) {
                return null;
            }
            String ret = null;
            WeaponFireInfo guess_info = new WeaponFireInfo(shooter,
                                                           new EntityState(shooter), target, null, mw, game);
            WeaponFireInfo accurate_info = new WeaponFireInfo(shooter, target, mw,
                                                              game);
            if (guess_info.to_hit.getValue() != accurate_info.to_hit.getValue()) {
                ret = new String();
                ret += "Incorrect To Hit prediction, weapon " + mw.getName() + " ("
                       + shooter.getChassis() + " vs " + target.getDisplayName()
                       + ")" + ":\n";
                ret += " Guess: " + Integer.toString(guess_info.to_hit.getValue())
                       + " " + guess_info.to_hit.getDesc() + "\n";
                ret += " Real:  "
                       + Integer.toString(accurate_info.to_hit.getValue()) + " "
                       + accurate_info.to_hit.getDesc() + "\n";
            }
            return ret;
        } finally {
            Logger.methodEnd(getClass(), METHOD_NAME);
        }
    }

    /**
     * Mostly for debugging, this returns a non-null string that describes how
     * the guess on a physical attack failed to be perfectly accurate, or null
     * if accurate
     */
    String checkGuess_Physical(Entity shooter, Targetable target,
                               PhysicalAttackType attack_type, IGame game) {
        final String METHOD_NAME = "getGuess_Physical(Entity, Targetable, PhysicalAttackType, IGame)";
        Logger.methodBegin(FireControl.class, METHOD_NAME);

        try {
            if (!(shooter instanceof Mech)) {
                return null; // only mechs can do physicals
            }

            String ret = null;
            if (shooter.getPosition() == null) {
                return "Shooter has NULL coordinates!";
            } else if (target.getPosition() == null) {
                return "Target has NULL coordinates!";
            }
            PhysicalInfo guess_info = new PhysicalInfo(shooter, null, target, null,
                                                       attack_type, game);
            PhysicalInfo accurate_info = new PhysicalInfo(shooter, target,
                                                          attack_type, game);
            if (guess_info.to_hit.getValue() != accurate_info.to_hit.getValue()) {
                ret = new String();
                ret += "Incorrect To Hit prediction, physical attack "
                       + attack_type.name() + ":\n";
                ret += " Guess: " + Integer.toString(guess_info.to_hit.getValue())
                       + " " + guess_info.to_hit.getDesc() + "\n";
                ret += " Real:  "
                       + Integer.toString(accurate_info.to_hit.getValue()) + " "
                       + accurate_info.to_hit.getDesc() + "\n";
            }
            return ret;
        } finally {
            Logger.methodEnd(getClass(), METHOD_NAME);
        }
    }

    /**
     * Mostly for debugging, this returns a non-null string that describes how
     * any possible guess has failed to be perfectly accurate. or null if
     * perfect
     */
    String checkAllGuesses(Entity shooter, IGame game) {
        final String METHOD_NAME = "checkAllGuesses(Entity, IGame)";
        Logger.methodBegin(FireControl.class, METHOD_NAME);

        try {
            String ret = new String("");
            ArrayList<Targetable> enemies = getTargetableEnemyEntities(shooter,
                                                                       game);
            for (Targetable e : enemies) {
                for (Mounted mw : shooter.getWeaponList()) {
                    String splain = checkGuess(shooter, e, mw, game);
                    if (splain != null) {
                        ret += splain;
                    }
                }
                String splainphys = null;
                splainphys = checkGuess_Physical(shooter, e,
                                                 PhysicalAttackType.RIGHT_KICK, game);
                if (splainphys != null) {
                    ret += splainphys;
                }
                splainphys = checkGuess_Physical(shooter, e,
                                                 PhysicalAttackType.LEFT_KICK, game);
                if (splainphys != null) {
                    ret += splainphys;
                }
                splainphys = checkGuess_Physical(shooter, e,
                                                 PhysicalAttackType.RIGHT_PUNCH, game);
                if (splainphys != null) {
                    ret += splainphys;
                }
                splainphys = checkGuess_Physical(shooter, e,
                                                 PhysicalAttackType.LEFT_PUNCH, game);
                if (splainphys != null) {
                    ret += splainphys;
                }

            }
            if (ret.compareTo("") == 0) {
                return null;
            }
            return ret;
        } finally {
            Logger.methodEnd(getClass(), METHOD_NAME);
        }
    }

    /**
     * calculates the 'utility' of a firing plan. override this function if you
     * have a better idea about what firing plans are good
     */
    void calculateUtility(FiringPlan p, int overheat_value) {
        double damage_utility = 1.0;
        double critical_utility = 10.0;
        double kill_utility = 50.0;
        double overheat_disutility = 5.0;
        int overheat = 0;
        if (p.getHeat() > overheat_value) {
            overheat = p.getHeat() - overheat_value;
        }
        p.utility = ((damage_utility * p.getExpectedDamage())
                     + (critical_utility * p.getExpectedCriticals()) + (kill_utility * p
                .getKillProbability())) - (overheat_disutility * overheat);
    }

    /**
     * calculates the 'utility' of a physical action.
     */
    void calculateUtility(PhysicalInfo p) {
        double damage_utility = 1.0;
        double critical_utility = 10.0;
        double kill_utility = 50.0;
        p.utility = (damage_utility * p.getExpectedDamage())
                    + (critical_utility * p.expected_criticals)
                    + (kill_utility * p.kill_probability);
    }

    /**
     * Creates a firing plan that fires all weapons with nonzero to hit value at
     * a target ignoring heat, and using best guess from different states Does
     * not change facing
     */
    FiringPlan guessFullFiringPlan(Entity shooter, EntityState shooterState,
                                   Targetable target, EntityState target_state, IGame game) {
        if (shooterState == null) {
            shooterState = new EntityState(shooter);
        }
        FiringPlan myplan = new FiringPlan();
        if (shooter.getPosition() == null) {
            Logger.log(getClass(), "guessFullFiringPlan(Entity, EntityState, Targetable, EntityState, IGame)",
                       LogLevel.ERROR, "Shooter's position is NULL!");
            return myplan;
        }
        if (target.getPosition() == null) {
            Logger.log(getClass(), "guessFullFiringPlan(Entity, EntityState, Targetable, EntityState, IGame)",
                       LogLevel.ERROR, "Target's position is NULL!");
            return myplan;
        }
        for (Mounted mw : shooter.getWeaponList()) { // cycle through my weapons
            WeaponFireInfo shoot = new WeaponFireInfo(shooter, shooterState,
                                                      target, target_state, mw, game);
            if (shoot.prob_to_hit > 0) {
                myplan.add(shoot);
            }
        }
        calculateUtility(
                myplan,
                (shooter instanceof Mech) ? ((shooter.getHeatCapacity() - shooterState.getHeat()) + 5)
                                          : 999);
        return myplan;
    }

    /**
     * Creates a firing plan that fires all weapons with nonzero to hit value in
     * a air to ground strike
     *
     * @param shooter
     * @param target
     * @param target_state
     * @param shooter_path
     * @param game
     * @param assume_under_flight_path
     * @return
     */
    FiringPlan guessFullAirToGroundPlan(Entity shooter, Targetable target,
                                        EntityState target_state, MovePath shooter_path, IGame game,
                                        boolean assume_under_flight_path) {
        if (target_state == null) {
            target_state = new EntityState(target);
        }
        if (!assume_under_flight_path) {
            if (!isTargetUnderMovePath(shooter_path, target_state)) {
                return new FiringPlan();
            }
        }
        FiringPlan myplan = new FiringPlan();
        if (shooter.getPosition() == null) {
            Logger.log(getClass(),
                       "guessFullAirToGroundPlan(Entity, Targetable, EntityState, MovePath, IGame, boolean)",
                       LogLevel.ERROR, "Shooter's position is NULL!");
            return myplan;
        }
        if (target.getPosition() == null) {
            Logger.log(getClass(),
                       "guessFullAirToGroundPlan(Entity, Targetable, EntityState, MovePath, IGame, boolean)",
                       LogLevel.ERROR, "Target's position is NULL!");
            return myplan;
        }
        for (Mounted mw : shooter.getWeaponList()) { // cycle through my weapons

            WeaponFireInfo shoot = new WeaponFireInfo(shooter, shooter_path,
                                                      target, target_state, mw, game, true);
            if (shoot.prob_to_hit > 0) {
                myplan.add(shoot);
            }
        }
        calculateUtility(myplan, 999); // Aeros don't have heat capacity, (I
        // think?)
        return myplan;
    }

    /**
     * Guesses what the expected damage would be if the shooter fired all of its
     * weapons at the target
     */
    double guessExpectedDamage(Entity shooter, EntityState shooter_state,
                               Targetable target, EntityState target_state, IGame game) {
        // FiringPlan
        // fullplan=guessFullFiringPlan(shooter,shooter_state,target,target_state,game);
        FiringPlan fullplan = guessFullFiringPlan(shooter, shooter_state,
                                                  target, target_state, game);
        return fullplan.getExpectedDamage();
    }

    /**
     * Creates a firing plan that fires all weapons with nonzero to hit value at
     * a target ignoring heat, and using actual game ruleset from different
     * states
     */
    FiringPlan getFullFiringPlan(Entity shooter, Targetable target, IGame game) {
        FiringPlan myplan = new FiringPlan();
        if (shooter.getPosition() == null) {
            Logger.log(getClass(),
                       "getFullFiringPlan(Entity, Targetable, IGame)", LogLevel.ERROR,
                       "Shooter's position is NULL!");
            return myplan;
        }
        if (target.getPosition() == null) {
            Logger.log(getClass(),
                       "getFullFiringPlan(Entity, Targetable, IGame)", LogLevel.ERROR,
                       "Target's position is NULL!");
            return myplan;
        }
        for (Mounted mw : shooter.getWeaponList()) { // cycle through my weapons
            WeaponFireInfo shoot = new WeaponFireInfo(shooter, target, mw, game);
            if ((shoot.prob_to_hit > 0)) {
                myplan.add(shoot);
            }
        }
        calculateUtility(myplan, (shooter.getHeatCapacity() - shooter.heat) + 5);
        return myplan;
    }

    /**
     * Creates an array that gives the 'best' firing plan (the maximum utility)
     * under the heat of the index
     */
    FiringPlan[] calcFiringPlansUnderHeat(FiringPlan maxplan, int maxheat,
                                          IGame game) {
        if (maxheat < 0) {
            maxheat = 0; // can't be worse than zero heat
        }
        FiringPlan[] best_plans = new FiringPlan[maxheat + 1];
        best_plans[0] = new FiringPlan();
        FiringPlan nonzeroheat_options = new FiringPlan();
        // first extract any firings of zero heat
        for (WeaponFireInfo f : maxplan) {
            if (f.heat == 0) {
                best_plans[0].add(f);
            } else {
                nonzeroheat_options.add(f);
            }
        }
        // build up heat table
        for (int i = 1; i <= maxheat; i++) {
            best_plans[i] = new FiringPlan();
            best_plans[i].addAll(best_plans[i - 1]);
            for (WeaponFireInfo f : nonzeroheat_options) {
                if ((i - f.heat) >= 0) {
                    if (!best_plans[i - f.heat].containsWeapon(f.weapon)) {
                        FiringPlan testplan = new FiringPlan();
                        testplan.addAll(best_plans[i - f.heat]);
                        testplan.add(f);
                        calculateUtility(testplan, 999); // TODO fix overheat
                        if (testplan.utility > best_plans[i].utility) {
                            best_plans[i] = testplan;
                        }
                    }
                }
            }
        }
        return best_plans;
    }

    /**
     * Gets the 'best' firing plan under a certain heat No twisting is done
     */
    FiringPlan getBestFiringPlanUnderHeat(Entity shooter, Targetable target,
                                          int maxheat, IGame game) {
        if (maxheat < 0) {
            maxheat = 0; // can't have less than zero heat
        }
        FiringPlan fullplan = getFullFiringPlan(shooter, target, game);
        if (fullplan.getHeat() <= maxheat) {
            return fullplan;
        }
        FiringPlan heatplans[] = calcFiringPlansUnderHeat(fullplan, maxheat,
                                                          game);
        return heatplans[maxheat];
    }

    /*
     * Gets the 'best' firing plan, using heat as a disutility. No twisting is
     * done
     */
    FiringPlan getBestFiringPlan(Entity shooter, Targetable target, IGame game) {
        FiringPlan fullplan = getFullFiringPlan(shooter, target, game);
        if (!(shooter instanceof Mech)) {
            return fullplan; // no need to optimize heat for non-mechs
        }
        FiringPlan heatplans[] = calcFiringPlansUnderHeat(fullplan,
                                                          fullplan.getHeat(), game);
        FiringPlan best_plan = new FiringPlan();
        int overheat = (shooter.getHeatCapacity() - shooter.heat) + 4;
        for (int i = 0; i < (fullplan.getHeat() + 1); i++) {
            calculateUtility(heatplans[i], overheat);
            if ((best_plan.utility < heatplans[i].utility)) {
                best_plan = heatplans[i];
            }
        }
        return best_plan;
    }

    /**
     * Guesses the 'best' firing plan under a certain heat No twisting is done
     */
    FiringPlan guessBestFiringPlanUnderHeat(Entity shooter,
                                            EntityState shooter_state, Targetable target,
                                            EntityState target_state, int maxheat, IGame game) {
        if (maxheat < 0) {
            maxheat = 0; // can't have less than zero heat
        }
        FiringPlan fullplan = guessFullFiringPlan(shooter, shooter_state,
                                                  target, target_state, game);
        if (fullplan.getHeat() <= maxheat) {
            return fullplan;
        }
        FiringPlan heatplans[] = calcFiringPlansUnderHeat(fullplan, maxheat,
                                                          game);
        return heatplans[maxheat];
    }

    /**
     * Guesses the 'best' firing plan, using heat as a disutility. No twisting
     * is done
     */
    FiringPlan guessBestFiringPlan(Entity shooter, EntityState shooterState,
                                   Targetable target, EntityState target_state, IGame game) {
        if (shooterState == null) {
            shooterState = new EntityState(shooter);
        }
        FiringPlan fullplan = guessFullFiringPlan(shooter, shooterState,
                                                  target, target_state, game);
        if (!(shooter instanceof Mech)) {
            return fullplan; // no need to optimize heat for non-mechs
        }
        FiringPlan heatplans[] = calcFiringPlansUnderHeat(fullplan,
                                                          fullplan.getHeat(), game);
        FiringPlan best_plan = new FiringPlan();
        int overheat = (shooter.getHeatCapacity() - shooterState.getHeat()) + 4;
        for (int i = 0; i < fullplan.getHeat(); i++) {
            calculateUtility(heatplans[i], overheat);
            if ((best_plan.utility < heatplans[i].utility)) {
                best_plan = heatplans[i];
            }
        }
        return best_plan;
    }

    /**
     * Gets the 'best' firing plan under a certain heat includes the option of
     * twisting
     */
    FiringPlan getBestFiringPlanUnderHeatWithTwists(Entity shooter,
                                                    Targetable target, int maxheat, IGame game) {
        int orig_facing = shooter.getSecondaryFacing();
        FiringPlan notwist_plan = getBestFiringPlanUnderHeat(shooter, target,
                                                             maxheat, game);
        if (!shooter.canChangeSecondaryFacing()) {
            return notwist_plan;
        }
        shooter.setSecondaryFacing(correct_facing(orig_facing + 1));
        FiringPlan righttwist_plan = getBestFiringPlanUnderHeat(shooter,
                                                                target, maxheat, game);
        righttwist_plan.twist = 1;
        shooter.setSecondaryFacing(correct_facing(orig_facing - 1));
        FiringPlan lefttwist_plan = getBestFiringPlanUnderHeat(shooter, target,
                                                               maxheat, game);
        lefttwist_plan.twist = -1;
        shooter.setSecondaryFacing(orig_facing);
        if ((notwist_plan.getExpectedDamage() > righttwist_plan
                .getExpectedDamage())
            && (notwist_plan.getExpectedDamage() > lefttwist_plan
                .getExpectedDamage())) {
            return notwist_plan;
        }
        if (lefttwist_plan.getExpectedDamage() > righttwist_plan
                .getExpectedDamage()) {
            return lefttwist_plan;
        }
        return righttwist_plan;
    }

    /**
     * Gets the 'best' firing plan using heat as disutiltiy includes the option
     * of twisting
     */
    FiringPlan getBestFiringPlanWithTwists(Entity shooter, Targetable target,
                                           IGame game) {
        int orig_facing = shooter.getSecondaryFacing();
        FiringPlan notwist_plan = getBestFiringPlan(shooter, target, game);
        if (!shooter.canChangeSecondaryFacing()) {
            return notwist_plan;
        }
        shooter.setSecondaryFacing(correct_facing(orig_facing + 1));
        FiringPlan righttwist_plan = getBestFiringPlan(shooter, target, game);
        righttwist_plan.twist = 1;
        shooter.setSecondaryFacing(correct_facing(orig_facing - 1));
        FiringPlan lefttwist_plan = getBestFiringPlan(shooter, target, game);
        lefttwist_plan.twist = -1;
        shooter.setSecondaryFacing(orig_facing);
        if ((notwist_plan.getExpectedDamage() > righttwist_plan
                .getExpectedDamage())
            && (notwist_plan.getExpectedDamage() > lefttwist_plan
                .getExpectedDamage())) {
            return notwist_plan;
        }
        if (lefttwist_plan.getExpectedDamage() > righttwist_plan
                .getExpectedDamage()) {
            return lefttwist_plan;
        }
        return righttwist_plan;
    }

    /**
     * Guesses the 'best' firing plan under a certain heat includes the option
     * of twisting
     */
    FiringPlan guessBestFiringPlanUnderHeatWithTwists(Entity shooter,
                                                      EntityState shooterState, Targetable target,
                                                      EntityState target_state, int maxheat, IGame game) {
        if (shooterState == null) {
            shooterState = new EntityState(shooter);
        }
        int origFacing = shooterState.getFacing();
        FiringPlan notwist_plan = guessBestFiringPlanUnderHeat(shooter,
                                                               shooterState, target, target_state, maxheat, game);
        if (!shooter.canChangeSecondaryFacing()) {
            return notwist_plan;
        }
        shooterState.setSecondaryFacing(correct_facing(origFacing + 1));
        FiringPlan righttwist_plan = guessBestFiringPlanUnderHeat(shooter,
                                                                  shooterState, target, target_state, maxheat, game);
        righttwist_plan.twist = 1;
        shooterState.setSecondaryFacing(correct_facing(origFacing - 1));
        FiringPlan lefttwist_plan = guessBestFiringPlanUnderHeat(shooter,
                                                                 shooterState, target, target_state, maxheat, game);
        lefttwist_plan.twist = -1;
        shooterState.setSecondaryFacing(origFacing);
        if ((notwist_plan.getExpectedDamage() > righttwist_plan
                .getExpectedDamage())
            && (notwist_plan.getExpectedDamage() > lefttwist_plan
                .getExpectedDamage())) {
            return notwist_plan;
        }
        if (lefttwist_plan.getExpectedDamage() > righttwist_plan
                .getExpectedDamage()) {
            return lefttwist_plan;
        }
        return righttwist_plan;
    }

    /**
     * Guesses the 'best' firing plan under a certain heat includes the option
     * of twisting
     */
    FiringPlan guessBestFiringPlanWithTwists(Entity shooter,
                                             EntityState shooterState, Targetable target,
                                             EntityState targetState, IGame game) {
        if (shooterState == null) {
            shooterState = new EntityState(shooter);
        }
        int orig_facing = shooterState.getFacing();
        FiringPlan notwist_plan = guessBestFiringPlan(shooter, shooterState,
                                                      target, targetState, game);
        if (!shooter.canChangeSecondaryFacing()) {
            return notwist_plan;
        }
        shooterState.setSecondaryFacing(correct_facing(orig_facing + 1));
        FiringPlan rightTwistPlan = guessBestFiringPlan(shooter,
                                                         shooterState, target, targetState, game);
        rightTwistPlan.twist = 1;
        shooterState.setSecondaryFacing(correct_facing(orig_facing - 1));
        FiringPlan leftTwistPlan = guessBestFiringPlan(shooter, shooterState,
                                                        target, targetState, game);
        leftTwistPlan.twist = -1;
        shooterState.setSecondaryFacing(orig_facing);
        if ((notwist_plan.getExpectedDamage() > rightTwistPlan
                .getExpectedDamage())
            && (notwist_plan.getExpectedDamage() > leftTwistPlan
                .getExpectedDamage())) {
            return notwist_plan;
        }
        if (leftTwistPlan.getExpectedDamage() > rightTwistPlan
                .getExpectedDamage()) {
            return leftTwistPlan;
        }
        return rightTwistPlan;
    }

    /*
     * Skeleton for guessing the best air to ground firing plan. Currently this
     * code is working in basicpathranker FiringPlan
     * guessBestAirToGroundFiringPlan(Entity shooter,MovePath shooter_path,IGame
     * game) { ArrayList<Entity>
     * targets=getEnemiesUnderFlightPath(shooter_path,shooter,game); for(Entity
     * target:targets) { FiringPlan theplan=guessFullAirToGroundPlan(shooter,
     * target,new EntityState(target),shooter_path,game,true);
     *
     * }
     *
     *
     * }
     */

    /**
     * Gets all the entities that are potential targets (even if you can't
     * technically hit them)
     */
    ArrayList<Targetable> getTargetableEnemyEntities(Entity shooter, IGame game) {
        ArrayList<Targetable> ret = new ArrayList<Targetable>();
        for (Entity e : game.getEntitiesVector()) {
            if (e.getOwner().isEnemyOf(shooter.getOwner())
                && (e.getPosition() != null) && !e.isOffBoard() && e.isTargetable()) {
                ret.add(e);
            }
        }
        ret.addAll(additional_targets);
        return ret;
    }

    /**
     * This is it. Calculate the 'best' possible firing plan for this entity.
     * Overload this function if you think you can do better.
     */
    FiringPlan getBestFiringPlan(Entity shooter, IGame game) {
        FiringPlan bestplan = new FiringPlan();
        ArrayList<Targetable> enemies = getTargetableEnemyEntities(shooter,
                                                                   game);
        for (Targetable e : enemies) {
            FiringPlan plan = getBestFiringPlanWithTwists(shooter, e, game);
            if ((bestplan == null) || (plan.utility > bestplan.utility)) {
                bestplan = plan;
            }
        }
        return bestplan;
    }

    public double getMaxDamageAtRange(Entity shooter, int range) {
        double ret = 0;
        for (Mounted mw : shooter.getWeaponList()) { // cycle through my weapons
            WeaponType wtype = (WeaponType) mw.getType();
            if (range < wtype.getLongRange()) {
                if (wtype.getDamage() > 0) {
                    ret += wtype.getDamage();
                }
            }
        }
        return ret;
    }

    /**
     * makes sure facing falls between 0 and 5 This function likely already
     * exists somewhere else
     */
    public static int correct_facing(int f) {
        while (f < 0) {
            f += 6;
        }
        if (f > 5) {
            f = f % 6;
        }
        return f;
    }

    /**
     * Makes sure ammo is loaded for each weapon
     */
    public void loadAmmo(Entity shooter, IGame game) {
        if (shooter == null) {
            return;
        }
        Iterator<Mounted> weps = shooter.getWeapons();
        while (weps.hasNext()) {
            Mounted onwep = weps.next();
            WeaponType weptype = (WeaponType) onwep.getType();
            if (weptype.ammoType != AmmoType.T_NA) {
                for (Mounted mountedAmmo : shooter.getAmmo()) {
                    AmmoType atype = (AmmoType) mountedAmmo.getType();
                    if (mountedAmmo.isAmmoUsable()
                        && (atype.getAmmoType() == weptype.getAmmoType())
                        && (atype.getRackSize() == weptype.getRackSize())) {
                        if (!shooter.loadWeapon(onwep, mountedAmmo)) {
                            System.err.println(shooter.getChassis()
                                               + " tried to load " + onwep.getName()
                                               + " with ammo " + mountedAmmo.getName()
                                               + " but failed somehow");
                        }
                    }
                }
            }

        }
    }

    /*
     * Here's a list of things that aren't technically units, but I want to be
     * able to target anyways. This is create with buildings and bridges and
     * mind
     */
    public ArrayList<Targetable> additional_targets = new ArrayList<Targetable>();
}
