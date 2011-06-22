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
import java.util.Vector;

import megamek.client.bot.PhysicalOption;
import megamek.common.AmmoType;
import megamek.common.BattleArmor;
import megamek.common.Compute;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.EntityMovementType;
import megamek.common.IGame;
import megamek.common.IHex;
import megamek.common.Infantry;
import megamek.common.LosEffects;
import megamek.common.Mech;
import megamek.common.MechWarrior;
import megamek.common.Mounted;
import megamek.common.MovePath;
import megamek.common.RangeType;
import megamek.common.TargetRoll;
import megamek.common.Terrains;
import megamek.common.ToHitData;
import megamek.common.WeaponType;
import megamek.common.actions.EntityAction;
import megamek.common.actions.KickAttackAction;
import megamek.common.actions.PhysicalAttackAction;
import megamek.common.actions.PunchAttackAction;
import megamek.common.actions.TorsoTwistAction;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.weapons.infantry.InfantryWeapon;

/**
 * FireControl selects which weapons a unit wants to fire and at whom Pay
 * attention to the difference between "guess" and "get". Guess will be much
 * faster, but inaccurate
 */
public class FireControl {

    /**
     * EntityState describes a hypothetical situation an entity could be in when
     * firing
     */
    public static class EntityState {
        public Coords position;
        public int facing;
        public int secondary_facing; // to account for torso twists
        public int heat;
        public int hexes_moved;
        public boolean isprone;
        public boolean isimmobile;
        public boolean isjumping;
        public EntityMovementType movement_type;

        /**
         * Initialize an entity state from the state an entity is actually in
         */
        EntityState(Entity e) {
            position = e.getPosition();
            facing = e.getFacing();
            hexes_moved = e.delta_distance;
            heat = e.heat;
            isprone = e.isProne() || e.isHullDown();
            isimmobile = e.isImmobile();
            isjumping = (e.moved == EntityMovementType.MOVE_JUMP);
            movement_type = e.moved;
            secondary_facing = e.getSecondaryFacing();
        }

        /**
         * Initialize an entity state from a movement path
         */
        EntityState(MovePath path) {
            position = path.getFinalCoords();
            facing = path.getFinalFacing();
            hexes_moved = path.getHexesMoved();
            heat = path.getEntity().heat;
            if (path.getLastStepMovementType() == EntityMovementType.MOVE_WALK) {
                heat += 1;
            } else if (path.getLastStepMovementType() == EntityMovementType.MOVE_RUN) {
                heat += 2;
            } else if ((path.getLastStepMovementType() == EntityMovementType.MOVE_JUMP)
                    && (hexes_moved <= 3)) {
                heat += 3;
            } else if ((path.getLastStepMovementType() == EntityMovementType.MOVE_JUMP)
                    && (hexes_moved > 3)) {
                heat += hexes_moved;
            }
            isprone = path.getFinalProne() || path.getFinalHullDown();
            isimmobile = path.getEntity().isImmobile();
            isjumping = path.isJumping();
            movement_type = path.getLastStepMovementType();
            secondary_facing = facing;
        }

    }

    /**
     * WeaponFireInfo is a wrapper around a WeaponAttackAction that includes
     * probability to hit and expected damage
     */
    public class WeaponFireInfo {
        private WeaponAttackAction action;
        public Entity shooter;
        public Entity target;
        public Mounted weapon;
        public double prob_to_hit;
        public int heat;
        public double max_damage;
        public double expected_damage_on_hit;
        public ToHitData to_hit;

        public double getExpectedDamage() {
            return prob_to_hit * expected_damage_on_hit;
        };

        /**
         * This constructs a WeaponFireInfo using an actual WeaponAttackAction
         * with real to hit values
         */
        WeaponFireInfo(Entity sshooter, Entity ttarget, Mounted wep, IGame game) {
            shooter = sshooter;
            weapon = wep;
            target = ttarget;
            action = new WeaponAttackAction(shooter.getId(), target.getId(),
                    shooter.getEquipmentNum(weapon));
            // I would like to use Compute.getExpectedDamage here, but it's
            // private
            // I assume it has been made private for good reason, so I won't
            // mess with it
            to_hit = action.toHit(game);
            prob_to_hit = Compute.oddsAbove(to_hit.getValue()) / 100.0;
            heat = ((WeaponType) weapon.getType()).getHeat();
            if (max_damage == WeaponType.DAMAGE_MISSILE) {
                max_damage = ((WeaponType) weapon.getType()).getRackSize(); // I
                                                                            // think
                                                                            // this
                                                                            // is
                                                                            // the
                                                                            // right
                                                                            // amount
                expected_damage_on_hit = max_damage / 2; // not true. too lazy
                                                         // to calculate real
                                                         // value
            } else {
                max_damage = ((WeaponType) weapon.getType()).getDamage();
                expected_damage_on_hit = max_damage;
            }
        }

        /**
         * This constructs a WeaponFireInfo using the best guess of how likely
         * this is to hit without actually constructing the weaponattackaction
         */
        WeaponFireInfo(Entity sshooter, EntityState shooter_state,
                Entity ttarget, EntityState target_state, Mounted wep,
                IGame game) {
            shooter = sshooter;
            weapon = wep;
            target = ttarget;
            action = null;
            to_hit = guessToHitModifier(shooter, shooter_state, target,
                    target_state, wep, game);
            prob_to_hit = Compute.oddsAbove(to_hit.getValue()) / 100.0;
            heat = ((WeaponType) weapon.getType()).getHeat();
            if (max_damage == WeaponType.DAMAGE_MISSILE) {
                max_damage = ((WeaponType) weapon.getType()).getRackSize(); // I
                                                                            // think
                                                                            // this
                                                                            // is
                                                                            // the
                                                                            // right
                                                                            // amount
                expected_damage_on_hit = max_damage / 2; // not true. too lazy
                                                         // to calculate real
                                                         // value
            } else {
                max_damage = ((WeaponType) weapon.getType()).getDamage();
                expected_damage_on_hit = max_damage;
            }
        }

        WeaponAttackAction getWeaponAttackAction(IGame game) {
            if (action != null) {
                return action;
            }
            action = new WeaponAttackAction(shooter.getId(), target.getId(),
                    shooter.getEquipmentNum(weapon));
            prob_to_hit = Compute.oddsAbove(action.toHit(game).getValue()) / 100.0;
            return action;
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

        FiringPlan() {
            twist = 0;
        }

        int getHeat() {
            int heat = 0;
            for (WeaponFireInfo f : this) {
                heat += f.heat;
            }
            return heat;
        }

        double getExpectedDamage() {
            double exdam = 0;
            for (WeaponFireInfo f : this) {
                exdam += f.expected_damage_on_hit * f.prob_to_hit;
            }
            return exdam;
        }

        boolean containsWeapon(Mounted wep) {
            for (WeaponFireInfo f : this) {
                if (f.weapon == wep) {
                    return true;
                }
            }
            return false;
        }

        public Vector<EntityAction> getEntityActionVector(IGame game) {
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
        }
    }

    /**
     * PhysicalInfo is a wrapper around a PhysicalAttackAction that includes
     * probability to hit and expected damage
     */
    public static class PhysicalInfo {
        public Entity shooter;
        public Entity target;
        public PhysicalAttackAction action;
        public PhysicalAttackType attack_type;
        public ToHitData to_hit;
        public double prob_to_hit;
        public double max_damage;
        public double expected_damage_on_hit;

        public double getExpectedDamage() {
            return prob_to_hit * expected_damage_on_hit;
        };

        PhysicalInfo(Entity sshooter, EntityState shooter_state,
                Entity ttarget, EntityState target_state,
                PhysicalAttackType atype, IGame game) {
            shooter = sshooter;
            target = ttarget;
            attack_type = atype;
            to_hit = guessToHitModifier_Physical(shooter, shooter_state,
                    target, target_state, attack_type, game);
            prob_to_hit = Compute.oddsAbove(to_hit.getValue());
            if ((atype == PhysicalAttackType.LEFT_PUNCH)
                    || (atype == PhysicalAttackType.RIGHT_PUNCH)) {
                max_damage = (int) Math.ceil(shooter.getWeight() / 10.0);
            } else { // assuming kick
                max_damage = (int) Math.floor(shooter.getWeight() / 5.0);
            }
            expected_damage_on_hit = max_damage;
        }

        PhysicalInfo(Entity sshooter, Entity ttarget, PhysicalAttackType atype,
                IGame game) {
            shooter = sshooter;
            target = ttarget;
            attack_type = atype;
            if ((attack_type == PhysicalAttackType.RIGHT_PUNCH)
                    || (attack_type == PhysicalAttackType.LEFT_PUNCH)) {
                int armid = attack_type == PhysicalAttackType.RIGHT_PUNCH ? 2
                        : 1;
                action = new PunchAttackAction(shooter.getId(), target.getId(),
                        armid);
                to_hit = ((PunchAttackAction) action).toHit(game);
                max_damage = PunchAttackAction.getDamageFor(shooter, armid,
                        target instanceof Infantry);
            } else { // assume kick
                int legid = attack_type == PhysicalAttackType.RIGHT_KICK ? 2
                        : 1;
                action = new KickAttackAction(shooter.getId(), target.getId(),
                        legid);
                to_hit = ((KickAttackAction) action).toHit(game);
                max_damage = KickAttackAction.getDamageFor(shooter, legid,
                        target instanceof Infantry);
            }
            prob_to_hit = Compute.oddsAbove(to_hit.getValue());
            expected_damage_on_hit = max_damage;
        }

        /**
         * Current bot code requires physical attacks to be given as 'physical
         * option'. This does the necessary conversion
         */
        public PhysicalOption getAsPhysicalOption() {
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
        }
    }

    /**
     * Gets the toHit modifier common to both weapon and physical attacks
     */
    public static ToHitData guessToHitModifierHelper_AnyAttack(Entity shooter,
            EntityState shooter_state, Entity target, EntityState target_state,
            IGame game) {
        if (shooter_state == null) {
            shooter_state = new EntityState(shooter);
        }
        if (target_state == null) {
            target_state = new EntityState(target);
        }

        ToHitData tohit = new ToHitData();
        // If people are moving or lying down, there are consequences
        tohit.append(Compute.getAttackerMovementModifier(game, shooter.getId(),
                shooter_state.movement_type));
        tohit.append(Compute.getTargetMovementModifier(
                target_state.hexes_moved, target_state.isjumping, false));
        if (shooter_state.isprone) {
            tohit.addModifier(2, "attacker prone");
        }
        if (target_state.isimmobile) {
            tohit.addModifier(-4, "target immobile");
        }
        // terrain modifiers, since "compute" won't let me do these remotely
        IHex target_hex = game.getBoard().getHex(target_state.position);
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
        int distance = shooter_state.position.distance(target_state.position);
        if (target_state.isprone && (distance > 1)) {
            tohit.addModifier(1, "target prone and at range");
        } else if (target_state.isprone && (distance == 1)) {
            tohit.addModifier(-2, "target prone and adjacent");
        }

        return tohit;
    }

    public enum PhysicalAttackType {
        LEFT_KICK, RIGHT_KICK, LEFT_PUNCH, RIGHT_PUNCH
    };

    /**
     * Makes a rather poor guess as to what the to hit modifier will be with a
     * physical attack.
     */
    public static ToHitData guessToHitModifier_Physical(Entity shooter,
            EntityState shooter_state, Entity target, EntityState target_state,
            PhysicalAttackType attack_type, IGame game) {
        if (!(shooter instanceof Mech)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE,
                    "Non mechs don't make physical attacks");
        }
        // Base to hit is piloting skill +2
        ToHitData tohit = new ToHitData();
        if (shooter_state == null) {
            shooter_state = new EntityState(shooter);
        }
        if (target_state == null) {
            target_state = new EntityState(target);
        }
        int distance = shooter_state.position.distance(target_state.position);
        if (distance > 1) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Can't hit that far");
        }

        tohit.append(guessToHitModifierHelper_AnyAttack(shooter, shooter_state,
                target, target_state, game));
        // check if target is within arc
        int arc = 0;
        if (attack_type == PhysicalAttackType.LEFT_PUNCH) {
            arc = Compute.ARC_LEFTARM;
        } else if (attack_type == PhysicalAttackType.RIGHT_PUNCH) {
            arc = Compute.ARC_RIGHTARM;
        }
        else {
            arc = Compute.ARC_FORWARD; // assume kick
        }
        if (!(Compute.isInArc(shooter_state.position,
                shooter_state.secondary_facing, target_state.position, arc) || (distance == 0))) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Target not in arc");
        }

        IHex attHex = game.getBoard().getHex(shooter_state.position);
        IHex targHex = game.getBoard().getHex(target_state.position);
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

            if (shooter_state.isprone) {
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
            if (shooter_state.isprone) {
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
            if (!shooter.hasWorkingSystem(Mech.ACTUATOR_HIP, legLoc)) {
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
        }
        return tohit;
    }

    /**
     * Makes an educated guess as to the to hit modifier with a weapon attack.
     * Does not actually place unit into desired position, because that is
     * exceptionally slow. Most of this is copied from WeaponAttack.
     */
    ToHitData guessToHitModifier(Entity shooter, EntityState shooter_state,
            Entity target, EntityState target_state, Mounted mw, IGame game) {
        if (shooter_state == null) {
            shooter_state = new EntityState(shooter);
        }
        if (target_state == null) {
            target_state = new EntityState(target);
        }
        // first check if the shot is impossible
        if (!mw.canFire()) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "weapon cannot fire");
        }
        if (((WeaponType) mw.getType()).ammoType != AmmoType.T_NA) {
            if (mw.getLinked() == null) {
                return new ToHitData(TargetRoll.IMPOSSIBLE, "ammo is gone");
            }
            if (mw.getLinked().getShotsLeft() == 0) {
                return new ToHitData(TargetRoll.IMPOSSIBLE,
                        "weapon out of ammo");
            }
        }
        if ((shooter_state.isprone)
                && ((shooter.isLocationBad(Mech.LOC_RARM)) || (shooter
                        .isLocationBad(Mech.LOC_LARM)))) {
            return new ToHitData(TargetRoll.IMPOSSIBLE,
                    "prone and missing an arm.");
        }

        int shooter_facing = shooter_state.facing;
        if (shooter.isSecondaryArcWeapon(shooter.getEquipmentNum(mw)))
         {
            shooter_facing = shooter_state.secondary_facing; // check if torso
        }
                                                             // twists affect
                                                             // weapon
        boolean inarc = Compute.isInArc(shooter_state.position, shooter_facing,
                target_state.position,
                shooter.getWeaponArc(shooter.getEquipmentNum(mw)));
        if (!inarc) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "not in arc");
        }
        // Find out a bit about the shooter and target
        boolean isShooterInfantry = (shooter instanceof Infantry);
        boolean isWeaponInfantry = ((WeaponType) mw.getType())
                .hasFlag(WeaponType.F_INFANTRY);
        int distance = shooter_state.position.distance(target_state.position);

        if ((distance == 0) && (!isShooterInfantry)) {
            return new ToHitData(TargetRoll.AUTOMATIC_FAIL,
                    "noninfantry shooting with zero range");
        }
        // Base to hit is gunnery skill
        ToHitData tohit = new ToHitData(shooter.getCrew().getGunnery(),
                "gunnery skill");
        tohit.append(guessToHitModifierHelper_AnyAttack(shooter, shooter_state,
                target, target_state, game));
        // There is kindly already a class that will calculate line of sight for
        // me
        LosEffects loseffects = LosEffects.calculateLos(game, shooter.getId(),
                target, shooter_state.position, target_state.position, false);
        tohit.append(loseffects.losModifiers(game));
        if ((tohit.getValue() == TargetRoll.IMPOSSIBLE)
                || (tohit.getValue() == TargetRoll.AUTOMATIC_FAIL))
         {
            return tohit; // you can't hit what you can't see
        }
        // Now deal with range effects
        int range = RangeType.rangeBracket(distance,
                ((WeaponType) mw.getType()).getRanges(mw), false);
        if (!isWeaponInfantry) {
            if (range == RangeType.RANGE_SHORT) {
                tohit.addModifier(0, "Short Range");
            } else if (range == RangeType.RANGE_MEDIUM) {
                tohit.addModifier(2, "Medium Range");
            } else if (range == RangeType.RANGE_LONG) {
                tohit.addModifier(4, "Long Range");
            } else if (range == RangeType.RANGE_MINIMUM) {
                tohit.addModifier(((WeaponType) mw.getType()).getMinimumRange()
                        - distance + 1, "Minimum Range");
            }
            else {
                return new ToHitData(TargetRoll.AUTOMATIC_FAIL, "out of range"); // out
                                                                                 // of
                                                                                 // range
            }
        } else {
            tohit.append(Compute.getInfantryRangeMods(range,
                    (InfantryWeapon) mw.getType()));
        }

        // let us not forget about heat
        if (shooter.getHeatFiringModifier() != 0) {
            tohit.addModifier(shooter.getHeatFiringModifier(), "heat");
        }
        // and damage
        tohit.append(Compute.getDamageWeaponMods(shooter, mw));
        // and finally some special cases
        if (((WeaponType) mw.getType()).getToHitModifier() != 0) {
            tohit.addModifier(((WeaponType) mw.getType()).getToHitModifier(),
                    "weapon to-hit");
        }
        if (shooter.hasTargComp()
                && ((WeaponType) mw.getType())
                        .hasFlag(WeaponType.F_DIRECT_FIRE)) {
            tohit.addModifier(-1, "targeting computer");
        }
        if ((!isShooterInfantry) && (target instanceof BattleArmor)) {
            tohit.addModifier(1, " battle armor target");
        }
        if (target instanceof MechWarrior) {
            tohit.addModifier(2, " ejected mechwarrior target");
        }
        return tohit;
    }

    /**
     * Mostly for debugging, this returns a non-null string that describes how
     * the guess has failed to be perfectly accurate. or null if perfectly
     * accurate
     */
    String checkGuess(Entity shooter, Entity target, Mounted mw, IGame game) {
        String ret = null;
        WeaponFireInfo guess_info = new WeaponFireInfo(shooter, null, target,
                null, mw, game);
        WeaponFireInfo accurate_info = new WeaponFireInfo(shooter, target, mw,
                game);
        if (guess_info.to_hit.getValue() != accurate_info.to_hit.getValue()) {
            ret = new String();
            ret += "Incorrect To Hit prediction, weapon " + mw.getName()
                    + ":\n";
            ret += " Guess: " + Integer.toString(guess_info.to_hit.getValue())
                    + " " + guess_info.to_hit.getDesc() + "\n";
            ret += " Real:  "
                    + Integer.toString(accurate_info.to_hit.getValue()) + " "
                    + accurate_info.to_hit.getDesc() + "\n";
        }
        return ret;
    }

    /**
     * Mostly for debugging, this returns a non-null string that describes how
     * the guess on a physical attack failed to be perfectly accurate, or null
     * if accurate
     */
    String checkGuess_Physical(Entity shooter, Entity target,
            PhysicalAttackType attack_type, IGame game) {
        if (shooter instanceof Infantry)
         {
            return null; // can't do physicals
        }
        if (shooter instanceof BattleArmor)
         {
            return null; // can't do physicals
        }
        String ret = null;
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
    }

    /**
     * Mostly for debugging, this returns a non-null string that describes how
     * any possible guess has failed to be perfectly accurate. or null if
     * perfect
     */
    String checkAllGuesses(Entity shooter, IGame game) {
        String ret = new String("");
        ArrayList<Entity> enemies = getTargetableEnemyEntities(shooter, game);
        for (Entity e : enemies) {
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
    }

    /**
     * Creates a firing plan that fires all weapons with nonzero to hit value at
     * a target ignoring heat, and using best guess from different states Does
     * not change facing
     */
    FiringPlan guessFullFiringPlan(Entity shooter, EntityState shooter_state,
            Entity target, EntityState target_state, IGame game) {
        FiringPlan myplan = new FiringPlan();
        for (Mounted mw : shooter.getWeaponList()) { // cycle through my weapons
            WeaponFireInfo shoot = new WeaponFireInfo(shooter, shooter_state,
                    target, target_state, mw, game);
            if (shoot.prob_to_hit > 0) {
                myplan.add(shoot);
            }
        }
        return myplan;
    }

    /**
     * Guesses what the expected damage would be if the shooter fired all of its
     * weapons at the target
     */
    double guessExpectedDamage(Entity shooter, EntityState shooter_state,
            Entity target, EntityState target_state, IGame game) {
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
    FiringPlan getFullFiringPlan(Entity shooter, Entity target, IGame game) {
        FiringPlan myplan = new FiringPlan();
        for (Mounted mw : shooter.getWeaponList()) { // cycle through my weapons
            WeaponFireInfo shoot = new WeaponFireInfo(shooter, target, mw, game);
            if (shoot.prob_to_hit > 0) {
                myplan.add(shoot);
            }
        }
        return myplan;
    }

    /**
     * Creates an array that gives the 'best' firing plan (the maximum damage)
     * under the heat of the index
     */
    FiringPlan[] calcFiringPlansUnderHeat(FiringPlan maxplan, int maxheat,
            IGame game) {
        if (maxheat < 0)
         {
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
                if (i - f.heat >= 0) {
                    if (!best_plans[i - f.heat].containsWeapon(f.weapon)) {
                        if (best_plans[i - f.heat].getExpectedDamage()
                                + f.expected_damage_on_hit * f.prob_to_hit > best_plans[i]
                                .getExpectedDamage()) {
                            best_plans[i] = new FiringPlan();
                            best_plans[i].addAll(best_plans[i - f.heat]);
                            best_plans[i].add(f);
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
    FiringPlan getBestFiringPlanUnderHeat(Entity shooter, Entity target,
            int maxheat, IGame game) {
        if (maxheat < 0)
         {
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

    /**
     * Guesses the 'best' firing plan under a certain heat No twisting is done
     */
    FiringPlan guessBestFiringPlanUnderHeat(Entity shooter,
            EntityState shooter_state, Entity target, EntityState target_state,
            int maxheat, IGame game) {
        if (maxheat < 0)
         {
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
     * Gets the 'best' firing plan under a certain heat includes the option of
     * twisting
     */
    FiringPlan getBestFiringPlanUnderHeatWithTwists(Entity shooter,
            Entity target, int maxheat, IGame game) {
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
     * Guesses the 'best' firing plan under a certain heat includes the option
     * of twisting
     */
    FiringPlan guessBestFiringPlanUnderHeatWithTwists(Entity shooter,
            EntityState shooter_state, Entity target, EntityState target_state,
            int maxheat, IGame game) {
        if (shooter_state == null) {
            shooter_state = new EntityState(shooter);
        }
        int orig_facing = shooter_state.facing;
        FiringPlan notwist_plan = guessBestFiringPlanUnderHeat(shooter,
                shooter_state, target, target_state, maxheat, game);
        if (!shooter.canChangeSecondaryFacing()) {
            return notwist_plan;
        }
        shooter_state.secondary_facing = correct_facing(orig_facing + 1);
        FiringPlan righttwist_plan = guessBestFiringPlanUnderHeat(shooter,
                shooter_state, target, target_state, maxheat, game);
        righttwist_plan.twist = 1;
        shooter_state.secondary_facing = correct_facing(orig_facing - 1);
        FiringPlan lefttwist_plan = guessBestFiringPlanUnderHeat(shooter,
                shooter_state, target, target_state, maxheat, game);
        lefttwist_plan.twist = -1;
        shooter_state.secondary_facing = orig_facing;
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
     * Gets all the entities that are potential targets (even if you can't
     * technically hit them)
     */
    ArrayList<Entity> getTargetableEnemyEntities(Entity shooter, IGame game) {
        ArrayList<Entity> ret = new ArrayList<Entity>();
        for (Entity e : game.getEntitiesVector()) {
            if (e.getOwner().isEnemyOf(shooter.getOwner())
                    && (e.getPosition() != null) && !e.isOffBoard()) {
                ret.add(e);
            }
        }
        return ret;
    }

    /**
     * This is it. Calculate the 'best' possible firing plan for this entity.
     * Overload this function if you think you can do better.
     */
    FiringPlan getBestFiringPlan(Entity shooter, IGame game) {
        int maxheat = shooter.getHeatCapacity() + 5 - shooter.heat;
        FiringPlan bestplan = null;
        ArrayList<Entity> enemies = getTargetableEnemyEntities(shooter, game);
        for (Entity e : enemies) {
            FiringPlan plan = getBestFiringPlanUnderHeatWithTwists(shooter, e,
                    maxheat, game);
            if ((bestplan == null)
                    || (plan.getExpectedDamage() > bestplan.getExpectedDamage())) {
                bestplan = plan;
            }
        }
        return bestplan;
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
}
