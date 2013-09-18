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

import megamek.common.Aero;
import megamek.common.AmmoType;
import megamek.common.BattleArmor;
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

    // todo What about extended torso twist & turrets?

    public FireControl() {}

    /**
     * Gets the toHit modifier common to both weapon and physical attacks
     */
    public static ToHitData guessToHitModifierHelperAnyAttack(Entity shooter,
                                                              EntityState shooterState, Targetable target,
                                                              EntityState targetState, IGame game) {
        final String METHOD_NAME = "guessToHitModifierHelperAnyAttack(Entity, EntityState, Targetable, EntityState, " +
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

    /**
     * Makes a rather poor guess as to what the to hit modifier will be with a
     * physical attack.
     */
    public static ToHitData guessToHitModifierPhysical(Entity shooter,
                                                       EntityState shooterState, Targetable target,
                                                       EntityState targetState, PhysicalAttackType attackType,
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

            tohit.append(guessToHitModifierHelperAnyAttack(shooter, shooterState,
                                                           target, targetState, game));
            // check if target is within arc
            int arc;
            if (attackType == PhysicalAttackType.LEFT_PUNCH) {
                arc = Compute.ARC_LEFTARM;
            } else if (attackType == PhysicalAttackType.RIGHT_PUNCH) {
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
            if ((attackType == PhysicalAttackType.LEFT_PUNCH)
                || (attackType == PhysicalAttackType.RIGHT_PUNCH)) {
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
                int armLoc = attackType == PhysicalAttackType.RIGHT_PUNCH ? Mech.LOC_RARM
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
                int legLoc = attackType == PhysicalAttackType.RIGHT_KICK ? Mech.LOC_RLEG
                                                                          : Mech.LOC_LLEG;
                if (shooter.hasHipCrit()) {
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
            boolean isWeaponInfantry = mw.getType().hasFlag(WeaponType.F_INFANTRY);
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
            tohit.append(guessToHitModifierHelperAnyAttack(shooter, shooterState,
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
            if (mw.getType().getToHitModifier() != 0) {
                tohit.addModifier(mw.getType().getToHitModifier(), "weapon to-hit");
            }
            if (((WeaponType) mw.getType()).getAmmoType() != AmmoType.T_NA) {
                AmmoType atype = (AmmoType) mw.getLinked().getType();
                if ((atype != null) && (atype.getToHitModifier() != 0)) {
                    tohit.addModifier(atype.getToHitModifier(), "ammunition to-hit modifier");
                }
            }
            if (shooter.hasTargComp() && mw.getType().hasFlag(WeaponType.F_DIRECT_FIRE)) {
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
            tohit.append(guessToHitModifierHelperAnyAttack(shooter, shooter_state,
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
     * @param movePath
     * @param shooter
     * @param game
     * @return
     */
    ArrayList<Entity> getEnemiesUnderFlightPath(MovePath movePath, Entity shooter, IGame game) {
        final String METHOD_NAME = "getEnemiesUnderFlightPath(MovePath, Entity, IGame)";
        Logger.methodBegin(FireControl.class, METHOD_NAME);

        try {
            ArrayList<Entity> ret = new ArrayList<Entity>();
            for (Enumeration<MoveStep> e = movePath.getSteps(); e.hasMoreElements(); ) {
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
            WeaponFireInfo guessInfo = new WeaponFireInfo(shooter,
                                                           new EntityState(shooter), target, null, mw, game);
            WeaponFireInfo accurateInfo = new WeaponFireInfo(shooter, target, mw,
                                                              game);
            if (guessInfo.getToHit().getValue() != accurateInfo.getToHit().getValue()) {
                ret = "Incorrect To Hit prediction, weapon " + mw.getName() + " ("
                       + shooter.getChassis() + " vs " + target.getDisplayName()
                       + ")" + ":\n";
                ret += " Guess: " + Integer.toString(guessInfo.getToHit().getValue())
                       + " " + guessInfo.getToHit().getDesc() + "\n";
                ret += " Real:  "
                       + Integer.toString(accurateInfo.getToHit().getValue()) + " "
                       + accurateInfo.getToHit().getDesc() + "\n";
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
    String checkGuessPhysical(Entity shooter, Targetable target,
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
            PhysicalInfo guessInfo = new PhysicalInfo(shooter, null, target, null, attack_type, game);
            PhysicalInfo accurateInfo = new PhysicalInfo(shooter, target, attack_type, game);
            if (guessInfo.getToHit().getValue() != accurateInfo.getToHit().getValue()) {
                ret = "Incorrect To Hit prediction, physical attack "
                       + attack_type.name() + ":\n";
                ret += " Guess: " + Integer.toString(guessInfo.getToHit().getValue())
                       + " " + guessInfo.getToHit().getDesc() + "\n";
                ret += " Real:  "
                       + Integer.toString(accurateInfo.getToHit().getValue()) + " "
                       + accurateInfo.getToHit().getDesc() + "\n";
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
            StringBuilder ret = new StringBuilder();
            ArrayList<Targetable> enemies = getTargetableEnemyEntities(shooter,
                                                                       game);
            for (Targetable e : enemies) {
                for (Mounted mw : shooter.getWeaponList()) {
                    String splain = checkGuess(shooter, e, mw, game);
                    if (splain != null) {
                        ret.append(splain);
                    }
                }
                String guessPlainPhysical = checkGuessPhysical(shooter, e,
                                                PhysicalAttackType.RIGHT_KICK, game);
                if (guessPlainPhysical != null) {
                    ret.append(guessPlainPhysical);
                }
                guessPlainPhysical = checkGuessPhysical(shooter, e,
                                                PhysicalAttackType.LEFT_KICK, game);
                if (guessPlainPhysical != null) {
                    ret.append(guessPlainPhysical);
                }
                guessPlainPhysical = checkGuessPhysical(shooter, e,
                                                PhysicalAttackType.RIGHT_PUNCH, game);
                if (guessPlainPhysical != null) {
                    ret.append(guessPlainPhysical);
                }
                guessPlainPhysical = checkGuessPhysical(shooter, e,
                                                PhysicalAttackType.LEFT_PUNCH, game);
                if (guessPlainPhysical != null) {
                    ret.append(guessPlainPhysical);
                }

            }
            if ("0".equalsIgnoreCase(ret.toString())) {
                return null;
            }
            return ret.toString();
        } finally {
            Logger.methodEnd(getClass(), METHOD_NAME);
        }
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
        FiringPlan myplan = new FiringPlan(target);
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
            if (shoot.getProbabilityToHit() > 0) {
                myplan.addWeaponFire(shoot);
            }
        }

        int overheatValue = shooter.tracksHeat() ?
                            ((shooter.getHeatCapacity() - shooterState.getHeat()) + 5) :
                            999;
        myplan.calcUtility(overheatValue);
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
                return new FiringPlan(target);
            }
        }
        FiringPlan myplan = new FiringPlan(target);
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
            if (shoot.getProbabilityToHit() > 0) {
                myplan.addWeaponFire(shoot);
            }
        }
        int overheatValue = shooter.tracksHeat() ?
                            ((shooter.getHeatCapacity() - shooter.getHeat()) + 5) :
                            999;
        myplan.calcUtility(overheatValue);
        return myplan;
    }

    /**
     * Guesses what the expected damage would be if the shooter fired all of its
     * weapons at the target
     */
    double guessExpectedDamage(Entity shooter, EntityState shooterState,
                               Targetable target, EntityState targetState, IGame game) {
        // FiringPlan
        // fullplan=guessFullFiringPlan(shooter,shooter_state,target,target_state,game);
        FiringPlan fullplan = guessFullFiringPlan(shooter, shooterState,
                                                  target, targetState, game);
        return fullplan.getExpectedDamage();
    }

    /**
     * Creates a firing plan that fires all weapons with nonzero to hit value at
     * a target ignoring heat, and using actual game ruleset from different
     * states
     */
    FiringPlan getFullFiringPlan(Entity shooter, Targetable target, IGame game) {
        FiringPlan myplan = new FiringPlan(target);
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
            if ((shoot.getProbabilityToHit() > 0)) {
                myplan.addWeaponFire(shoot);
            }
        }
        int overheatValue = shooter.tracksHeat() ?
                            ((shooter.getHeatCapacity() - shooter.getHeat()) + 5) :
                            999;
        myplan.calcUtility(overheatValue);
        return myplan;
    }

    /**
     * Creates an array that gives the 'best' firing plan (the maximum utility)
     * under the heat of the index
     */
    FiringPlan[] calcFiringPlansUnderHeat(FiringPlan maxPlan, int maxHeat) {
        if (maxHeat < 0) {
            maxHeat = 0; // can't be worse than zero heat
        }
        Targetable target = maxPlan.getTarget();
        FiringPlan[] bestPlans = new FiringPlan[maxHeat + 1];
        bestPlans[0] = new FiringPlan(target);
        FiringPlan nonZeroHeatOptions = new FiringPlan(target);
        // first extract any firings of zero heat
        for (WeaponFireInfo weaponFireInfo : maxPlan.getFiringInfo()) {
            if (weaponFireInfo.getHeat() == 0) {
                bestPlans[0].addWeaponFire(weaponFireInfo);
            } else {
                nonZeroHeatOptions.addWeaponFire(weaponFireInfo);
            }
        }
        // build up heat table
        for (int i = 1; i <= maxHeat; i++) {
            bestPlans[i] = new FiringPlan(target);
            bestPlans[i].addWeaponFireList(bestPlans[i - 1].getFiringInfo());
            for (WeaponFireInfo weaponFireInfo : nonZeroHeatOptions.getFiringInfo()) {
                if ((i - weaponFireInfo.getHeat()) >= 0) {
                    if (!bestPlans[i - weaponFireInfo.getHeat()].containsWeapon(weaponFireInfo.getWeapon())) {
                        FiringPlan testplan = new FiringPlan(target);
                        testplan.addWeaponFireList(bestPlans[i - weaponFireInfo.getHeat()].getFiringInfo());
                        testplan.addWeaponFire(weaponFireInfo);
                        Entity shooter = weaponFireInfo.getShooter();
                        int overheatValue = shooter.tracksHeat() ?
                                            ((shooter.getHeatCapacity() - shooter.getHeat()) + 5) :
                                            999;
                        testplan.calcUtility(overheatValue);
                        if (testplan.getUtility() > bestPlans[i].getUtility()) {
                            bestPlans[i] = testplan;
                        }
                    }
                }
            }
        }
        return bestPlans;
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
        FiringPlan heatplans[] = calcFiringPlansUnderHeat(fullplan, maxheat);
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
        int overheatValue = shooter.tracksHeat() ?
                            ((shooter.getHeatCapacity() - shooter.getHeat()) + 4) :
                            999;
        FiringPlan heatplans[] = calcFiringPlansUnderHeat(fullplan, fullplan.getHeat());
        FiringPlan best_plan = new FiringPlan(target);
        best_plan.calcUtility(overheatValue);
        for (int i = 0; i < (fullplan.getHeat() + 1); i++) {
            heatplans[i].calcUtility(overheatValue);
            if ((best_plan.getUtility() < heatplans[i].getUtility())) {
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
        FiringPlan heatplans[] = calcFiringPlansUnderHeat(fullplan, maxheat);
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
        int overheatValue = shooter.tracksHeat() ?
                            ((shooter.getHeatCapacity() - shooter.getHeat()) + 4) :
                            999;
        FiringPlan heatplans[] = calcFiringPlansUnderHeat(fullplan, fullplan.getHeat());
        FiringPlan best_plan = new FiringPlan(target);
        best_plan.calcUtility(overheatValue);
        for (int i = 0; i < fullplan.getHeat(); i++) {
            heatplans[i].calcUtility(overheatValue);
            if ((best_plan.getUtility() < heatplans[i].getUtility())) {
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
        shooter.setSecondaryFacing(correctFacing(orig_facing + 1));
        FiringPlan righttwist_plan = getBestFiringPlanUnderHeat(shooter,
                                                                target, maxheat, game);
        righttwist_plan.setTwist(1);
        shooter.setSecondaryFacing(correctFacing(orig_facing - 1));
        FiringPlan lefttwist_plan = getBestFiringPlanUnderHeat(shooter, target,
                                                               maxheat, game);
        lefttwist_plan.setTwist(-1);
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
        shooter.setSecondaryFacing(correctFacing(orig_facing + 1));
        FiringPlan righttwist_plan = getBestFiringPlan(shooter, target, game);
        righttwist_plan.setTwist(1);
        shooter.setSecondaryFacing(correctFacing(orig_facing - 1));
        FiringPlan lefttwist_plan = getBestFiringPlan(shooter, target, game);
        lefttwist_plan.setTwist(-1);
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
        shooterState.setSecondaryFacing(correctFacing(origFacing + 1));
        FiringPlan righttwist_plan = guessBestFiringPlanUnderHeat(shooter,
                                                                  shooterState, target, target_state, maxheat, game);
        righttwist_plan.setTwist(1);
        shooterState.setSecondaryFacing(correctFacing(origFacing - 1));
        FiringPlan lefttwist_plan = guessBestFiringPlanUnderHeat(shooter,
                                                                 shooterState, target, target_state, maxheat, game);
        lefttwist_plan.setTwist(-1);
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
        shooterState.setSecondaryFacing(correctFacing(orig_facing + 1));
        FiringPlan rightTwistPlan = guessBestFiringPlan(shooter,
                                                         shooterState, target, targetState, game);
        rightTwistPlan.setTwist(1);
        shooterState.setSecondaryFacing(correctFacing(orig_facing - 1));
        FiringPlan leftTwistPlan = guessBestFiringPlan(shooter, shooterState,
                                                        target, targetState, game);
        leftTwistPlan.setTwist(-1);
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
        FiringPlan bestplan = null;
        ArrayList<Targetable> enemies = getTargetableEnemyEntities(shooter,
                                                                   game);
        int overheatValue = shooter.tracksHeat() ?
                            ((shooter.getHeatCapacity() - shooter.getHeat()) + 4) :
                            999;
        for (Targetable e : enemies) {
            FiringPlan plan = getBestFiringPlanWithTwists(shooter, e, game);
            plan.calcUtility(overheatValue);
            if ((bestplan == null) || (plan.getUtility() > bestplan.getUtility())) {
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
    public static int correctFacing(int f) {
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
    public void loadAmmo(Entity shooter) {
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
