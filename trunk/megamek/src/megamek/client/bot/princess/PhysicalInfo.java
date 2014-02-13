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

import megamek.client.bot.PhysicalOption;
import megamek.common.BipedMech;
import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.IGame;
import megamek.common.Infantry;
import megamek.common.Mech;
import megamek.common.Targetable;
import megamek.common.ToHitData;
import megamek.common.actions.KickAttackAction;
import megamek.common.actions.PhysicalAttackAction;
import megamek.common.actions.PunchAttackAction;

/**
 * @version $Id$
 * @lastEditBy Deric "Netzilla" Page (deric dot page at usa dot net)
 * @since 12/18/13 1:29 PM
 */
public class PhysicalInfo {
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
    private Princess owner;

    public double getExpectedDamage() {
        return prob_to_hit * expected_damage_on_hit;
    }

    PhysicalInfo(Entity sshooter, EntityState shooter_state,
                 Targetable ttarget, EntityState target_state,
                 PhysicalAttackType atype, IGame game, Princess owner) {
        final String METHOD_NAME = "PhysicalInfo(Entity, EntityState, Targetable, EntityState, PhysicalAttackType, " +
                "IGame)";
        owner.methodBegin(getClass(), METHOD_NAME);
        this.owner = owner;

        try {
            shooter = sshooter;
            target = ttarget;
            if (shooter_state == null) {
                shooter_state = new EntityState(sshooter);
            }
            if (target_state == null) {
                target_state = new EntityState(ttarget);
            }
            attack_type = atype;
            to_hit = owner.getFireControl().guessToHitModifier_Physical(shooter, shooter_state,
                                                                        target, target_state, attack_type, game);
            int fromdir = target_state.getPosition()
                                      .direction(shooter_state.getPosition());
            damage_direction = ((fromdir - target_state.getFacing()) + 6) % 6;
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
            owner.methodEnd(getClass(), METHOD_NAME);
        }
    }

    PhysicalInfo(Entity sshooter, Targetable ttarget,
                 PhysicalAttackType atype, IGame game, Princess owner) {
        final String METHOD_NAME = "PhysicalInfo(Entity, Targetable, PhysicalAttackType, IGame)";
        owner.methodBegin(getClass(), METHOD_NAME);
        this.owner = owner;

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
            owner.methodEnd(getClass(), METHOD_NAME);
        }
    }

    /**
     * Helper function to determine damage and criticals
     */
    public void initDamage() {
        final String METHOD_NAME = "initDamage()";
        owner.methodBegin(getClass(), METHOD_NAME);

        try {
            prob_to_hit = Compute.oddsAbove(to_hit.getValue()) / 100.0;
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
                    double hprob;
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
            owner.methodEnd(getClass(), METHOD_NAME);
        }
    }

    /**
     * Current bot code requires physical attacks to be given as 'physical
     * option'. This does the necessary conversion
     */
    public PhysicalOption getAsPhysicalOption() {
        final String METHOD_NAME = "getAsPhysicalOption()";
        owner.methodBegin(getClass(), METHOD_NAME);

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
            owner.methodEnd(getClass(), METHOD_NAME);
        }
    }
}
