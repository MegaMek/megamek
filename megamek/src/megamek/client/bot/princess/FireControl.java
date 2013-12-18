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
import java.util.List;

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
import megamek.common.actions.KickAttackAction;
import megamek.common.actions.PhysicalAttackAction;
import megamek.common.actions.PunchAttackAction;
import megamek.common.logging.LogLevel;
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
     * Gets the toHit modifier common to both weapon and physical attacks
     */
    public static ToHitData guessToHitModifierHelper_AnyAttack(Entity shooter,
                                                               EntityState shooter_state, Targetable target,
                                                               EntityState target_state, IGame game) {
        final String METHOD_NAME = "guessToHitModifierHelper_AnyAttack(Entity, EntityState, Targetable, EntityState, IGame)";
        owner.methodBegin(FireControl.class, METHOD_NAME);

        try {
            if (shooter_state == null) {
                shooter_state = new EntityState(shooter);
            }
            if (target_state == null) {
                target_state = new EntityState(target);
            }

            ToHitData tohit = new ToHitData();
            // If people are moving or lying down, there are consequences
            tohit.append(Compute.getAttackerMovementModifier(game, shooter.getId(),
                    shooter_state.getMovementType()));
            tohit.append(Compute.getTargetMovementModifier(
                    target_state.getHexesMoved(), target_state.isJumping(), target instanceof VTOL, game));
            if (shooter_state.isProne()) {
                tohit.addModifier(2, "attacker prone");
            }
            if (target_state.isImmobile()) {
                tohit.addModifier(-4, "target immobile");
            }
            if (target_state.getMovementType() == EntityMovementType.MOVE_SKID) {
                tohit.addModifier(2, "target skidded");
            }
            if (game.getOptions().booleanOption("tacops_standing_still") && (target_state.getMovementType() == EntityMovementType.MOVE_NONE)
                    && !target_state.isImmobile()
                    && !((target instanceof Infantry) || (target instanceof VTOL) || (target instanceof GunEmplacement))) {
                tohit.addModifier(-1, "target didn't move");
            }

            // did the target sprint?
            if (target_state.getMovementType() == EntityMovementType.MOVE_SPRINT) {
                tohit.addModifier(-1, "target sprinted");
            }

            // terrain modifiers, since "compute" won't let me do these remotely
            IHex target_hex = game.getBoard().getHex(target_state.getPosition());
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
            int distance = shooter_state.getPosition().distance(target_state.getPosition());
            if (target_state.isProne() && (distance > 1)) {
                tohit.addModifier(1, "target prone and at range");
            } else if (target_state.isProne() && (distance == 1)) {
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
            owner.methodEnd(FireControl.class, METHOD_NAME);
        }
    }

    /**
     * Makes a rather poor guess as to what the to hit modifier will be with a
     * physical attack.
     */
    public static ToHitData guessToHitModifier_Physical(Entity shooter,
                                                        EntityState shooter_state, Targetable target,
                                                        EntityState target_state, PhysicalAttackType attack_type, IGame game) {
        final String METHOD_NAME = "guessToHitModifier(Entity, EntityState, Targetable, EntityState, PhysicalAttackType, IGame)";
        owner.methodBegin(FireControl.class, METHOD_NAME);

        try {
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
            int distance = shooter_state.getPosition().distance(target_state.getPosition());
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
            } else {
                arc = Compute.ARC_FORWARD; // assume kick
            }
            if (!(Compute.isInArc(shooter_state.getPosition(),
                    shooter_state.getSecondaryFacing(), target_state.getPosition(), arc) || (distance == 0))) {
                return new ToHitData(TargetRoll.IMPOSSIBLE, "Target not in arc");
            }

            IHex attHex = game.getBoard().getHex(shooter_state.getPosition());
            IHex targHex = game.getBoard().getHex(target_state.getPosition());
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

                if (shooter_state.isProne()) {
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
                if (shooter_state.isProne()) {
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
            owner.methodEnd(FireControl.class, METHOD_NAME);
        }
    }

    /**
     * Makes an educated guess as to the to hit modifier with a weapon attack.
     * Does not actually place unit into desired position, because that is
     * exceptionally slow. Most of this is copied from WeaponAttack.
     */
    public static ToHitData guessToHitModifier(Entity shooter,
                                               EntityState shooter_state, Targetable target,
                                               EntityState target_state, Mounted mw, IGame game, Princess owner) {
        final String METHOD_NAME = "guessToHitModifier(Entity, EntityState, Targetable, EntityState, Mounted, IGame)";
        owner.methodBegin(FireControl.class, METHOD_NAME);

        try {
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
                if (mw.getLinked().getUsableShotsLeft() == 0) {
                    return new ToHitData(TargetRoll.IMPOSSIBLE,
                            "weapon out of ammo");
                }
            }
            if ((shooter_state.isProne())
                    && ((shooter.isLocationBad(Mech.LOC_RARM)) || (shooter
                    .isLocationBad(Mech.LOC_LARM)))) {
                return new ToHitData(TargetRoll.IMPOSSIBLE,
                        "prone and missing an arm.");
            }

            int shooter_facing = shooter_state.getFacing();
            if (shooter.isSecondaryArcWeapon(shooter.getEquipmentNum(mw))) {
                shooter_facing = shooter_state.getSecondaryFacing(); // check if torso
            }
            // twists affect
            // weapon
            boolean inarc = Compute.isInArc(shooter_state.getPosition(), shooter_facing,
                    target_state.getPosition(),
                    shooter.getWeaponArc(shooter.getEquipmentNum(mw)));
            if (!inarc) {
                return new ToHitData(TargetRoll.IMPOSSIBLE, "not in arc");
            }
            // Find out a bit about the shooter and target
            boolean isShooterInfantry = (shooter instanceof Infantry);
            boolean isWeaponInfantry = ((WeaponType) mw.getType())
                    .hasFlag(WeaponType.F_INFANTRY);
            if ((shooter_state.getPosition() == null) || (target_state.getPosition() == null)) {
                return new ToHitData(TargetRoll.AUTOMATIC_FAIL, "null position");
            }
            int distance = shooter_state.getPosition().distance(target_state.getPosition());

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
                    target, shooter_state.getPosition(), target_state.getPosition(), false);
            // water is a separate loseffect
            IHex target_hex = game.getBoard().getHex(target_state.getPosition());
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
                    ((WeaponType) mw.getType()).getRanges(mw), game.getOptions().booleanOption("tacops_range"));
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
            owner.methodEnd(FireControl.class, METHOD_NAME);
        }
    }

    /**
     * Makes an educated guess as to the to hit modifier by an aerospace unit
     * flying on a ground map doing a strike attack on a unit
     */
    public static ToHitData guessAirToGroundStrikeToHitModifier(Entity shooter,
                                                                Targetable target, EntityState target_state, MovePath shooter_path,
                                                                Mounted mw, IGame game, boolean assume_under_flight_plan) {
        final String METHOD_NAME = "guessAirToGroundStrikeToHitModifier(Entity, Targetable, EntityState, MovePath, Mounted, IGame, boolean)";
        owner.methodBegin(FireControl.class, METHOD_NAME);

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
            owner.methodEnd(FireControl.class, METHOD_NAME);
        }
    }

    /**
     * Checks if a target lies under a move path, to see if an aero unit can
     * attack it
     *
     * @param p            move path to check
     * @param target_state used for targets position
     * @return
     */
    public static boolean isTargetUnderMovePath(MovePath p,
                                                EntityState target_state) {
        final String METHOD_NAME = "isTargetUnderMovePath(MovePath, EntityState)";
        owner.methodBegin(FireControl.class, METHOD_NAME);

        try {
            for (Enumeration<MoveStep> e = p.getSteps(); e.hasMoreElements(); ) {
                Coords cord = e.nextElement().getPosition();
                if (cord.equals(target_state.getPosition())) {
                    return true;
                }
            }
            return false;
        } finally {
            owner.methodEnd(FireControl.class, METHOD_NAME);
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
        owner.methodBegin(FireControl.class, METHOD_NAME);

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
            owner.methodEnd(FireControl.class, METHOD_NAME);
        }
    }

    /**
     * Mostly for debugging, this returns a non-null string that describes how
     * the guess has failed to be perfectly accurate. or null if perfectly
     * accurate
     */
    String checkGuess(Entity shooter, Targetable target, Mounted mw, IGame game) {
        final String METHOD_NAME = "checkGuess(Entity, Targetable, Mounted, IGame)";
        owner.methodBegin(FireControl.class, METHOD_NAME);

        try {

            if ((shooter instanceof Aero) ||
                    (shooter.getPosition() == null) ||
                    (target.getPosition() == null)) {
                return null;
            }
            String ret = null;
            WeaponFireInfo guess_info = new WeaponFireInfo(shooter,
                    new EntityState(shooter), target, null, mw, game, owner);
            WeaponFireInfo accurate_info = new WeaponFireInfo(shooter, target, mw,
                    game, owner);
            if (guess_info.getToHit().getValue() != accurate_info.getToHit().getValue()) {
                ret = new String();
                ret += "Incorrect To Hit prediction, weapon " + mw.getName() + " ("
                        + shooter.getChassis() + " vs " + target.getDisplayName()
                        + ")" + ":\n";
                ret += " Guess: " + Integer.toString(guess_info.getToHit().getValue())
                        + " " + guess_info.getToHit().getDesc() + "\n";
                ret += " Real:  "
                        + Integer.toString(accurate_info.getToHit().getValue()) + " "
                        + accurate_info.getToHit().getDesc() + "\n";
            }
            return ret;
        } finally {
            owner.methodEnd(getClass(), METHOD_NAME);
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
        owner.methodBegin(FireControl.class, METHOD_NAME);

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
                    attack_type, game, owner);
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
            owner.methodEnd(getClass(), METHOD_NAME);
        }
    }

    /**
     * Mostly for debugging, this returns a non-null string that describes how
     * any possible guess has failed to be perfectly accurate. or null if
     * perfect
     */
    String checkAllGuesses(Entity shooter, IGame game) {
        final String METHOD_NAME = "checkAllGuesses(Entity, IGame)";
        owner.methodBegin(FireControl.class, METHOD_NAME);

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
            owner.methodEnd(getClass(), METHOD_NAME);
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
    FiringPlan guessFullFiringPlan(Entity shooter, EntityState shooter_state,
                                   Targetable target, EntityState target_state, IGame game) {
        if (shooter_state == null) {
            shooter_state = new EntityState(shooter);
        }
        FiringPlan myplan = new FiringPlan(owner);
        if (shooter.getPosition() == null) {
            owner.log(getClass(), "guessFullFiringPlan(Entity, EntityState, Targetable, EntityState, IGame)",
                    LogLevel.ERROR, "Shooter's position is NULL!");
            return myplan;
        }
        if (target.getPosition() == null) {
            owner.log(getClass(), "guessFullFiringPlan(Entity, EntityState, Targetable, EntityState, IGame)",
                    LogLevel.ERROR, "Target's position is NULL!");
            return myplan;
        }
        for (Mounted mw : shooter.getWeaponList()) { // cycle through my weapons
            WeaponFireInfo shoot = new WeaponFireInfo(shooter, shooter_state,
                    target, target_state, mw, game, owner);
            if (shoot.getProbabilityToHit() > 0) {
                myplan.add(shoot);
            }
        }
        calculateUtility(
                myplan,
                (shooter instanceof Mech) ? ((shooter.getHeatCapacity() - shooter_state.getHeat()) + 5)
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
                return new FiringPlan(owner);
            }
        }
        FiringPlan myplan = new FiringPlan(owner);
        if (shooter.getPosition() == null) {
            owner.log(getClass(),
                    "guessFullAirToGroundPlan(Entity, Targetable, EntityState, MovePath, IGame, boolean)",
                    LogLevel.ERROR, "Shooter's position is NULL!");
            return myplan;
        }
        if (target.getPosition() == null) {
            owner.log(getClass(),
                    "guessFullAirToGroundPlan(Entity, Targetable, EntityState, MovePath, IGame, boolean)",
                    LogLevel.ERROR, "Target's position is NULL!");
            return myplan;
        }
        for (Mounted mw : shooter.getWeaponList()) { // cycle through my weapons

            WeaponFireInfo shoot = new WeaponFireInfo(shooter, shooter_path,
                    target, target_state, mw, game, true, owner);
            if (shoot.getProbabilityToHit() > 0) {
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
        FiringPlan myplan = new FiringPlan(owner);
        if (shooter.getPosition() == null) {
            owner.log(getClass(),
                    "getFullFiringPlan(Entity, Targetable, IGame)", LogLevel.ERROR,
                    "Shooter's position is NULL!");
            return myplan;
        }
        if (target.getPosition() == null) {
            owner.log(getClass(),
                    "getFullFiringPlan(Entity, Targetable, IGame)", LogLevel.ERROR,
                    "Target's position is NULL!");
            return myplan;
        }
        for (Mounted mw : shooter.getWeaponList()) { // cycle through my weapons
            WeaponFireInfo shoot = new WeaponFireInfo(shooter, target, mw, game, owner);
            if ((shoot.getProbabilityToHit() > 0)) {
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
        best_plans[0] = new FiringPlan(owner);
        FiringPlan nonzeroheat_options = new FiringPlan(owner);
        // first extract any firings of zero heat
        for (WeaponFireInfo f : maxplan) {
            if (f.getHeat() == 0) {
                best_plans[0].add(f);
            } else {
                nonzeroheat_options.add(f);
            }
        }
        // build up heat table
        for (int i = 1; i <= maxheat; i++) {
            best_plans[i] = new FiringPlan(owner);
            best_plans[i].addAll(best_plans[i - 1]);
            for (WeaponFireInfo f : nonzeroheat_options) {
                if ((i - f.getHeat()) >= 0) {
                    if (!best_plans[i - f.getHeat()].containsWeapon(f.getWeapon())) {
                        FiringPlan testplan = new FiringPlan(owner);
                        testplan.addAll(best_plans[i - f.getHeat()]);
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
        FiringPlan best_plan = new FiringPlan(owner);
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
    FiringPlan guessBestFiringPlan(Entity shooter, EntityState shooter_state,
                                   Targetable target, EntityState target_state, IGame game) {
        if (shooter_state == null) {
            shooter_state = new EntityState(shooter);
        }
        FiringPlan fullplan = guessFullFiringPlan(shooter, shooter_state,
                target, target_state, game);
        if (!(shooter instanceof Mech)) {
            return fullplan; // no need to optimize heat for non-mechs
        }
        FiringPlan heatplans[] = calcFiringPlansUnderHeat(fullplan,
                fullplan.getHeat(), game);
        FiringPlan best_plan = new FiringPlan(owner);
        int overheat = (shooter.getHeatCapacity() - shooter_state.getHeat()) + 4;
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
                                                      EntityState shooter_state, Targetable target,
                                                      EntityState target_state, int maxheat, IGame game) {
        if (shooter_state == null) {
            shooter_state = new EntityState(shooter);
        }
        int orig_facing = shooter_state.getFacing();
        FiringPlan notwist_plan = guessBestFiringPlanUnderHeat(shooter,
                shooter_state, target, target_state, maxheat, game);
        if (!shooter.canChangeSecondaryFacing()) {
            return notwist_plan;
        }
        shooter_state.setSecondaryFacing(correct_facing(orig_facing + 1));
        FiringPlan righttwist_plan = guessBestFiringPlanUnderHeat(shooter,
                shooter_state, target, target_state, maxheat, game);
        righttwist_plan.twist = 1;
        shooter_state.setSecondaryFacing(correct_facing(orig_facing - 1));
        FiringPlan lefttwist_plan = guessBestFiringPlanUnderHeat(shooter,
                shooter_state, target, target_state, maxheat, game);
        lefttwist_plan.twist = -1;
        shooter_state.setSecondaryFacing(orig_facing);
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
                                             EntityState shooter_state, Targetable target,
                                             EntityState target_state, IGame game) {
        if (shooter_state == null) {
            shooter_state = new EntityState(shooter);
        }
        int orig_facing = shooter_state.getFacing();
        FiringPlan notwist_plan = guessBestFiringPlan(shooter, shooter_state,
                target, target_state, game);
        if (!shooter.canChangeSecondaryFacing()) {
            return notwist_plan;
        }
        shooter_state.setSecondaryFacing(correct_facing(orig_facing + 1));
        FiringPlan righttwist_plan = guessBestFiringPlan(shooter,
                shooter_state, target, target_state, game);
        righttwist_plan.twist = 1;
        shooter_state.setSecondaryFacing(correct_facing(orig_facing - 1));
        FiringPlan lefttwist_plan = guessBestFiringPlan(shooter, shooter_state,
                target, target_state, game);
        lefttwist_plan.twist = -1;
        shooter_state.setSecondaryFacing(orig_facing);
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
        ret.addAll(additionalTargets);
        return ret;
    }

    /**
     * This is it. Calculate the 'best' possible firing plan for this entity.
     * Overload this function if you think you can do better.
     */
    FiringPlan getBestFiringPlan(Entity shooter, IGame game) {
        FiringPlan bestplan = new FiringPlan(owner);
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
    private List<Targetable> additionalTargets = new ArrayList<Targetable>();

    public List<Targetable> getAdditionalTargets() {
        return additionalTargets;
    }

    public void setAdditionalTargets(List<Targetable> targets) {
        additionalTargets = targets;
    }
}
