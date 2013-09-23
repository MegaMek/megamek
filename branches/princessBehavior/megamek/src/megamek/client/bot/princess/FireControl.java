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

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import megamek.common.Aero;
import megamek.common.AmmoType;
import megamek.common.BattleArmor;
import megamek.common.Building;
import megamek.common.BuildingTarget;
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
import megamek.common.MiscType;
import megamek.common.Mounted;
import megamek.common.MovePath;
import megamek.common.MoveStep;
import megamek.common.Protomech;
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
import megamek.common.weapons.ATMWeapon;
import megamek.common.weapons.MMLWeapon;
import megamek.common.weapons.PulseLaserBayWeapon;
import megamek.common.weapons.StopSwarmAttack;
import megamek.common.weapons.infantry.InfantryWeapon;

/**
 * FireControl selects which weapons a unit wants to fire and at whom. Pay attention to the difference between "guess"
 * and "get". Guess will be much faster, but inaccurate.
 *
 * @version %Id%
 * @lastEditBy Deric "Netzilla" Page (deric dot page at usa dot net)
 * @since 9/21/13 8:13 AM
 */
public class FireControl {

    private static Boolean extremeRangeUsed = false;

    /**
     * This is a list of things that aren't technically units but I want to be able to target anyway.  This should be
     * populated with buildings, bridges and the like.
     */
    private final List<Targetable> additionalTargets = Collections.synchronizedList(new ArrayList<Targetable>());

    // todo What about extended torso twist & turrets?

    public FireControl() {}

    public List<Targetable> getAdditionalTargets() {
        return additionalTargets;
    }

    public void addAdditionalTarget(Targetable target) {
        synchronized (additionalTargets) {
            additionalTargets.add(target);
        }
    }

    public static boolean isExtremeRangeUsed(IGame game) {
        if (extremeRangeUsed == null) {
            extremeRangeUsed = game.getOptions().booleanOption("tacops_range");
        }
        return extremeRangeUsed;
    }

    public void setExtremeRangeUsed(boolean useExtremeRange) {
        extremeRangeUsed = useExtremeRange;
    }

    /**
     * Gets the toHit modifier common to both weapon and physical attacks
     *
     * @param shooter The attacker.
     * @param shooterState The current {@link EntityState} of the attacker.
     * @param target What's being shot at.
     * @param targetState The current {@link EntityState} of the target.
     * @param game The game currently underway.
     * @return The compiled {@link ToHitData}.
     */
    public static ToHitData guessToHitModifierHelperAnyAttack(Entity shooter, EntityState shooterState,
                                                              Targetable target, EntityState targetState, IGame game) {
        final String METHOD_NAME = "guessToHitModifierHelperAnyAttack(Entity, EntityState, Targetable, EntityState, " +
                                   "IGame)";
        Logger.methodBegin(FireControl.class, METHOD_NAME);

        ToHitData toHit = new ToHitData();
        try {
            if (shooterState == null) {
                shooterState = new EntityState(shooter);
            }
            if (targetState == null) {
                targetState = new EntityState(target);
            }

            // If people are moving or lying down, there are consequences
            toHit.append(Compute.getAttackerMovementModifier(game, shooter.getId(), shooterState.getMovementType()));
            toHit.append(Compute.getTargetMovementModifier( targetState.getHexesMoved(),
                                                            targetState.isJumping(),
                                                            target instanceof VTOL));
            if (shooterState.isProne()) {
                toHit.addModifier(2, "attacker prone");
            }
            if (targetState.isImmobile()) {
                toHit.addModifier(-4, "target immobile");
            }
            if (targetState.getMovementType() == EntityMovementType.MOVE_SKID) {
                toHit.addModifier(2, "target skidded");
            }
            if (game.getOptions().booleanOption("tacops_standing_still")
                && (targetState.getMovementType() == EntityMovementType.MOVE_NONE)
                && !targetState.isImmobile()
                && !((target instanceof Infantry) || (target instanceof VTOL) || (target instanceof GunEmplacement))) {
                toHit.addModifier(-1, "target didn't move");
            }

            // did the target sprint?
            if (targetState.getMovementType() == EntityMovementType.MOVE_SPRINT) {
                toHit.addModifier(-1, "target sprinted");
            }

            // terrain modifiers, since "compute" won't let me do these remotely
            IHex targetHex = game.getBoard().getHex(targetState.getPosition());
            int woodsLevel = targetHex.terrainLevel(Terrains.WOODS);
            if (targetHex.terrainLevel(Terrains.JUNGLE) > woodsLevel) {
                woodsLevel = targetHex.terrainLevel(Terrains.JUNGLE);
            }
            if (woodsLevel > 0) {
                toHit.addModifier(woodsLevel, "woods");
            }

            // Target prone modifiers.
            int distance = shooterState.getPosition().distance(targetState.getPosition());
            if (targetState.isProne() && (distance > 1)) {
                toHit.addModifier(1, "target prone and at range");
            } else if (targetState.isProne() && (distance == 1)) {
                toHit.addModifier(-2, "target prone and adjacent");
            }

            // Infantry/BA modifiers.
            boolean isShooterInfantry = (shooter instanceof Infantry);
            if ((!isShooterInfantry) && (target instanceof BattleArmor)) {
                toHit.addModifier(1, " battle armor target");
            } else if ((!isShooterInfantry) && ((target instanceof Infantry))) {
                toHit.addModifier(1, " infantry target");
            }
            if ((!isShooterInfantry) && (target instanceof MechWarrior)) {
                toHit.addModifier(2, " ejected mechwarrior target");
            }

            return toHit;
        } finally {
            Logger.log(FireControl.class, METHOD_NAME, LogLevel.DEBUG, toHit.getCumulativePlainDesc());
        }
    }

    /**
     * Makes a rather poor guess as to what the to hit modifier will be with a physical attack.
     *
     * @param shooter The attacker.
     * @param shooterState The current {@link EntityState} of the attacker.
     * @param target What's being shot at.
     * @param targetState The current {@link EntityState} of the target.
     * @param attackType The {@link PhysicalAttackType} being made.
     * @param game The game currently underway.
     * @return The compiled {@link ToHitData}.
     */
    public static ToHitData guessToHitModifierPhysical(Entity shooter, EntityState shooterState,
                                                       Targetable target, EntityState targetState,
                                                       PhysicalAttackType attackType,
                                                       IGame game) {
        final String METHOD_NAME = "guessToHitModifier(Entity, EntityState, Targetable, EntityState, " +
                                   "PhysicalAttackType, IGame)";
        Logger.methodBegin(FireControl.class, METHOD_NAME);

        ToHitData toHit = new ToHitData();

        try {
            if (!(shooter instanceof Mech)) {
                return new ToHitData(TargetRoll.IMPOSSIBLE, "Non mechs don't make physical attacks"); // ToDo: what about infantry, BA and Protos?
            }

            if (shooterState == null) {
                shooterState = new EntityState(shooter);
            }
            if (targetState == null) {
                targetState = new EntityState(target);
            }

            // Make sure target is in range.
            int distance = shooterState.getPosition().distance(targetState.getPosition());
            if (distance > 1) {
                return new ToHitData(TargetRoll.IMPOSSIBLE, "Can't hit that far");
            }

            toHit.append(guessToHitModifierHelperAnyAttack(shooter, shooterState, target, targetState, game));

            // check if target is within arc
            // todo What about melee weapons?
            int arc;
            if (attackType == PhysicalAttackType.LEFT_PUNCH) {
                arc = Compute.ARC_LEFTARM;
            } else if (attackType == PhysicalAttackType.RIGHT_PUNCH) {
                arc = Compute.ARC_RIGHTARM;
            } else {
                arc = Compute.ARC_FORWARD; // assume kick
            }
            if (!(Compute.isInArc(shooterState.getPosition(), shooterState.getSecondaryFacing(),
                                  targetState.getPosition(), arc) || (distance == 0))) {
                return new ToHitData(TargetRoll.IMPOSSIBLE, "Target not in arc");
            }

            // Elevation differences.
            final int attackerElevation = shooterState.getTotalElevation(game);
            final int attackerHeight = shooterState.getTotalHeight(game);
            final int targetElevation = targetState.getTotalElevation(game);
            final int targetHeight = targetState.getTotalHeight(game);
            if ((attackType == PhysicalAttackType.LEFT_PUNCH) || (attackType == PhysicalAttackType.RIGHT_PUNCH)) {
                // Quick failures.
                if (shooterState.isProne()) {
                    return new ToHitData(TargetRoll.IMPOSSIBLE, "can't punch while prone");
                }
                if (target instanceof Infantry) {
                    return new ToHitData(TargetRoll.IMPOSSIBLE, "can't punch infantry");
                }
                if ((attackerHeight < targetElevation) || (attackerHeight > targetHeight)) {
                    return new ToHitData(TargetRoll.IMPOSSIBLE, "Target elevation not in range");
                }

                // Base to hit is piloting.
                toHit.addModifier(shooter.getCrew().getPiloting(), "base");

                // Is my arm working?
                int armLoc = (attackType == PhysicalAttackType.RIGHT_PUNCH) ? Mech.LOC_RARM : Mech.LOC_LARM;
                if (shooter.isLocationBad(armLoc)) {
                    return new ToHitData(TargetRoll.IMPOSSIBLE, "Your arm's off!");
                }
                if (!shooter.hasWorkingSystem(Mech.ACTUATOR_SHOULDER, armLoc)) {
                    return new ToHitData(TargetRoll.IMPOSSIBLE, "shoulder destroyed");
                }
                if (!shooter.hasWorkingSystem(Mech.ACTUATOR_UPPER_ARM, armLoc)) {
                    toHit.addModifier(2, "Upper arm actuator destroyed");
                }
                if (!shooter.hasWorkingSystem(Mech.ACTUATOR_LOWER_ARM, armLoc)) {
                    toHit.addModifier(2, "Lower arm actuator missing or destroyed");
                }
                if (!shooter.hasWorkingSystem(Mech.ACTUATOR_HAND, armLoc)) {
                    toHit.addModifier(1, "Hand actuator missing or destroyed");
                }
            } else {
                // todo What about melee weapons?
                // Assuming this is a kick.

                // Quick failures.
                if (shooterState.isProne()) {
                    return new ToHitData(TargetRoll.IMPOSSIBLE, "Can't kick while prone");
                }
                if ((attackerElevation < targetElevation) || (attackerElevation > targetHeight)) {
                    return new ToHitData(TargetRoll.IMPOSSIBLE, "Target elevation not in range");
                }

                // Base to hit is Piloting -2.
                toHit.addModifier(shooter.getCrew().getPiloting() - 2, "base");

                // Playing Godzilla.
                if (target instanceof Infantry) {
                    if (distance == 0) {
                        toHit.addModifier(3, "kicking infantry");
                    } else {
                        return new ToHitData(TargetRoll.IMPOSSIBLE, "Infantry too far away");
                    }
                }

                // Is my leg working?
                int legLoc = attackType == (PhysicalAttackType.RIGHT_KICK) ? Mech.LOC_RLEG : Mech.LOC_LLEG;
                if (shooter.hasHipCrit()) {
                    return new ToHitData(TargetRoll.IMPOSSIBLE, "can't kick with broken hip");
                }
                if (!shooter.hasWorkingSystem(Mech.ACTUATOR_UPPER_LEG, legLoc)) {
                    toHit.addModifier(2, "Upper leg actuator destroyed");
                }
                if (!shooter.hasWorkingSystem(Mech.ACTUATOR_LOWER_LEG, legLoc)) {
                    toHit.addModifier(2, "Lower leg actuator destroyed");
                }
                if (!shooter.hasWorkingSystem(Mech.ACTUATOR_FOOT, legLoc)) {
                    toHit.addModifier(1, "Foot actuator destroyed");
                }

                // Optional rules.
                if (game.getOptions().booleanOption("tacops_attack_physical_psr")) {
                    if (shooter.getWeightClass() == EntityWeightClass.WEIGHT_LIGHT) {
                        toHit.addModifier(-2, "Weight Class Attack Modifier");
                    } else if (shooter.getWeightClass() == EntityWeightClass.WEIGHT_MEDIUM) {
                        toHit.addModifier(-1, "Weight Class Attack Modifier");
                    }
                }

            }
            return toHit;
        } finally {
            Logger.log(FireControl.class, METHOD_NAME, LogLevel.DEBUG, toHit.getCumulativePlainDesc());
        }
    }

    /**
     * Makes an educated guess as to the to hit modifier with a weapon attack.  Does not actually place unit into
     * desired position, because that is  exceptionally slow. Most of this is copied from WeaponAttack.
     *
     * @param shooter The attacker.
     * @param shooterState The current {@link EntityState} of the attacker.
     * @param target What's being shot at.
     * @param targetState The current {@link EntityState} of the target.
     * @param weapon The weapon being used for the attack.
     * @param game The game currently underway.
     * @return The compiled {@link ToHitData}.
     */
    public static ToHitData guessToHitModifier(Entity shooter, EntityState shooterState,
                                               Targetable target, EntityState targetState,
                                               Mounted weapon, IGame game) {
        final String METHOD_NAME = "guessToHitModifier(Entity, EntityState, Targetable, EntityState, Mounted, IGame)";
        Logger.methodBegin(FireControl.class, METHOD_NAME);

        // Base to hit is gunnery skill
        ToHitData toHit = new ToHitData(shooter.getCrew().getGunnery(), "gunnery skill");

        try {
            if (shooterState == null) {
                shooterState = new EntityState(shooter);
            }
            if (targetState == null) {
                targetState = new EntityState(target);
            }

            // first check if the shot is impossible
            if (!weapon.canFire()) {
                return new ToHitData(TargetRoll.IMPOSSIBLE, "weapon cannot fire");
            }

            // Check ammo supply.
            if (((WeaponType) weapon.getType()).ammoType != AmmoType.T_NA) {
                if (weapon.getLinked() == null) {
                    return new ToHitData(TargetRoll.IMPOSSIBLE, "ammo is gone");
                }
                if (weapon.getLinked().getUsableShotsLeft() == 0) {
                    return new ToHitData(TargetRoll.IMPOSSIBLE, "weapon out of ammo");
                }
            }

            // If I'm prone, do I have a working arm?
            if ((shooterState.isProne())
                && ((shooter.isLocationBad(Mech.LOC_RARM)) || (shooter.isLocationBad(Mech.LOC_LARM)))) {
                return new ToHitData(TargetRoll.IMPOSSIBLE, "prone and missing an arm.");
            }

            // Check my facing, accounting for torso twists.
            // todo What about extended torso twist?
            int shooterFacing = shooterState.getFacing();
            if (shooter.isSecondaryArcWeapon(shooter.getEquipmentNum(weapon))) {
                shooterFacing = shooterState.getSecondaryFacing();
            }
            boolean inarc = Compute.isInArc(shooterState.getPosition(), shooterFacing, targetState.getPosition(),
                                            shooter.getWeaponArc(shooter.getEquipmentNum(weapon)));
            if (!inarc) {
                return new ToHitData(TargetRoll.IMPOSSIBLE, "not in arc");
            }

            // Make sure my target and I are actually on the board.
            if ((shooterState.getPosition() == null) || (targetState.getPosition() == null)) {
                return new ToHitData(TargetRoll.AUTOMATIC_FAIL, "null position");
            }

            // Get range to target.
            int distance = shooterState.getPosition().distance(targetState.getPosition());

            // Can't shoot a target I'm standing on, unless I am infantry.
            boolean isShooterInfantry = (shooter instanceof Infantry);
            boolean isWeaponInfantry = weapon.getType().hasFlag(WeaponType.F_INFANTRY);
            if ((distance == 0) && (!isShooterInfantry)) {
                return new ToHitData(TargetRoll.AUTOMATIC_FAIL, "noninfantry shooting with zero range");
            }

            // General modifiers.
            toHit.append(guessToHitModifierHelperAnyAttack(shooter, shooterState, target, targetState, game));

            // There is kindly already a class that will calculate line of sight for me
            LosEffects loseffects = LosEffects.calculateLos(game, shooter.getId(), target, shooterState.getPosition(),
                                                            targetState.getPosition(), false);

            // water is a separate los effect
            IHex targetHex = targetState.getHex(game);
            if (target instanceof Entity) {
                if (targetHex.containsTerrain(Terrains.WATER)
                    && (targetHex.terrainLevel(Terrains.WATER) == 1)
                    && (((Entity) target).height() > 0)) {
                    loseffects.setTargetCover(loseffects.getTargetCover() | LosEffects.COVER_HORIZONTAL);
                }
            }
            toHit.append(loseffects.losModifiers(game));

            // you can't hit what you can't see
            if ((toHit.getValue() == TargetRoll.IMPOSSIBLE) || (toHit.getValue() == TargetRoll.AUTOMATIC_FAIL)) {
                return toHit;
            }

            // deal with swarm attacks.
            if (((WeaponType) weapon.getType()) instanceof StopSwarmAttack) {
                if (Entity.NONE == shooter.getSwarmTargetId()) {
                    return new ToHitData(TargetRoll.IMPOSSIBLE, "Not swarming a Mek.");
                } else {
                    return new ToHitData(TargetRoll.AUTOMATIC_SUCCESS, "stops swarming");
                }
            }

            // Have I taken sensor damage?
            if (shooter instanceof Tank) {
                int sensors = ((Tank) shooter).getSensorHits();
                if (sensors > 0) {
                    toHit.addModifier(sensors, "sensor damage");
                }
            }

            // Am I infantry trying to swarm or make a leg attack?
            if (target instanceof Mech) {
                if (Infantry.SWARM_MEK.equals(weapon.getType().getInternalName())) {
                    toHit.append(Compute.getSwarmMekBaseToHit(shooter, (Entity) target, game));
                }
                if (Infantry.LEG_ATTACK.equals(weapon.getType().getInternalName())) {
                    toHit.append(Compute.getLegAttackBaseToHit(shooter, (Entity) target, game));
                }
            }

            // Do I still have a chance to hit?
            if ((toHit.getValue() == TargetRoll.IMPOSSIBLE) || (toHit.getValue() == TargetRoll.AUTOMATIC_FAIL)) {
                return toHit;
            }

            // Now deal with range effects
            int range = RangeType.rangeBracket(distance, ((WeaponType) weapon.getType()).getRanges(weapon),
                                               isExtremeRangeUsed(game));

            // Aeros are 2x further for each altitude
            if (target instanceof Aero) {
                range += 2 * target.getAltitude();
            }

            // Infantry weapons handle range different.
            if (!isWeaponInfantry) {
                if (range == RangeType.RANGE_SHORT) {
                    toHit.addModifier(0, "Short Range");
                } else if (range == RangeType.RANGE_MEDIUM) {
                    toHit.addModifier(2, "Medium Range");
                } else if (range == RangeType.RANGE_LONG) {
                    toHit.addModifier(4, "Long Range");
                } else if (range == RangeType.RANGE_MINIMUM) {
                    toHit.addModifier((((WeaponType) weapon.getType()).getMinimumRange() - distance) + 1,
                                      "Minimum Range");
                } else if (range == RangeType.RANGE_EXTREME) {
                    toHit.addModifier(6, "Extreme Range");
                } else {
                    return new ToHitData(TargetRoll.AUTOMATIC_FAIL, "out of range");
                }
            } else {
                toHit.append(Compute.getInfantryRangeMods(distance, (InfantryWeapon) weapon.getType()));
            }

            // let us not forget about heat
            if (shooter.getHeatFiringModifier() != 0) {
                toHit.addModifier(shooter.getHeatFiringModifier(), "heat");
            }

            // and damage
            toHit.append(Compute.getDamageWeaponMods(shooter, weapon));

            // Weapon-based modifier.
            if (weapon.getType().getToHitModifier() != 0) {
                toHit.addModifier(weapon.getType().getToHitModifier(), "weapon to-hit");
            }

            // Ammo-based modifier.
            if (((WeaponType) weapon.getType()).getAmmoType() != AmmoType.T_NA) {
                AmmoType atype = (AmmoType) weapon.getLinked().getType();
                if ((atype != null) && (atype.getToHitModifier() != 0)) {
                    toHit.addModifier(atype.getToHitModifier(), "ammunition to-hit modifier");
                }
            }

            // Targetting computer.
            if (shooter.hasTargComp() && weapon.getType().hasFlag(WeaponType.F_DIRECT_FIRE)) {
                toHit.addModifier(-1, "targeting computer");
            }

            // todo What about iATM?
            if (weapon.getType().hasFlag(WeaponType.F_MISSILE) && shooter.hasArtemisV()) {
                toHit.addModifier(-1, "artemis v");
            }

            return toHit;
        } finally {
            Logger.log(FireControl.class, METHOD_NAME, LogLevel.DEBUG, toHit.getCumulativePlainDesc());
        }
    }

    /**
     * Makes an educated guess as to the to hit modifier by an aerospace unit  flying on a ground map doing a strike
     * attack on a unit
     *
     * @param shooter The attacker.
     * @param target What's being shot at.
     * @param targetState The current {@link EntityState} of the target.
     * @param shooterPath The flight path of the attacker.
     * @param weapon The weapon being used for the attack.
     * @param game The game currently underway.
     * @param assumeUnderFlightPlan Set TRUE to assume the target is in my flight path.
     * @return The compiled {@link ToHitData}.
     */
    public static ToHitData guessAirToGroundStrikeToHitModifier(Entity shooter,
                                                                Targetable target, EntityState targetState,
                                                                MovePath shooterPath,
                                                                Mounted weapon, IGame game,
                                                                boolean assumeUnderFlightPlan) {
        final String METHOD_NAME = "guessAirToGroundStrikeToHitModifier(Entity, Targetable, EntityState, MovePath, " +
                                   "Mounted, IGame, boolean)";
        Logger.methodBegin(FireControl.class, METHOD_NAME);

        // Base to hit is gunnery skill
        ToHitData toHit = new ToHitData(shooter.getCrew().getGunnery(), "gunnery skill");

        try {
            if (targetState == null) {
                targetState = new EntityState(target);
            }
            EntityState shooterState = new EntityState(shooter);

            // Can my weapon fire?
            if (!weapon.canFire()) {
                return new ToHitData(TargetRoll.IMPOSSIBLE, "weapon cannot fire");
            }

            // Do I have ammo?
            if (((WeaponType) weapon.getType()).ammoType != AmmoType.T_NA) {
                if (weapon.getLinked() == null) {
                    return new ToHitData(TargetRoll.IMPOSSIBLE, "ammo is gone");
                }
                if (weapon.getLinked().getUsableShotsLeft() == 0) {
                    return new ToHitData(TargetRoll.IMPOSSIBLE, "weapon out of ammo");
                }
            }

            // check if target is even under our path
            if (!assumeUnderFlightPlan) {
                if (!isTargetUnderMovePath(shooterPath, targetState)) {
                    return new ToHitData(TargetRoll.IMPOSSIBLE, "target not under flight path");
                }
            }

            // General modifiers.
            toHit.append(guessToHitModifierHelperAnyAttack(shooter, shooterState, target, targetState, game));

            // Additional penalty due to strike attack
            toHit.addModifier(+2, "strike attack");

            return toHit;
        } finally {
            Logger.log(FireControl.class, METHOD_NAME, LogLevel.DEBUG, toHit.getCumulativePlainDesc());
        }
    }

    /**
     * Checks if a target lies under a move path, to see if an aero unit can attack it
     *
     * @param flightPath            move path to check
     * @param targetState used for targets position
     * @return
     */
    public static boolean isTargetUnderMovePath(MovePath flightPath,
                                                EntityState targetState) {
        final String METHOD_NAME = "isTargetUnderMovePath(MovePath, EntityState)";
        Logger.methodBegin(FireControl.class, METHOD_NAME);

        try {
            for (Enumeration<MoveStep> e = flightPath.getSteps(); e.hasMoreElements(); ) {
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
    List<Entity> getEnemiesUnderFlightPath(MovePath movePath, Entity shooter, IGame game) {
        final String METHOD_NAME = "getEnemiesUnderFlightPath(MovePath, Entity, IGame)";
        Logger.methodBegin(FireControl.class, METHOD_NAME);

        try {
            List<Entity> ret = new ArrayList<Entity>();
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
     * Mostly for debugging, this returns a non-null string that describes how the guess has failed to be perfectly
     * accurate. or null if perfectly accurate
     *
     * @param shooter The attacker
     * @param target Who's being shot at.
     * @param weapon The weapon used.
     * @param game The game in progress.
     * @return NULL for a good guess or a description of what was wrong with the guess.
     */
    String checkGuess(Entity shooter, Targetable target, Mounted weapon, IGame game) {
        final String METHOD_NAME = "checkGuess(Entity, Targetable, Mounted, IGame)";
        Logger.methodBegin(FireControl.class, METHOD_NAME);

        try {

            if ((shooter instanceof Aero)
                || (shooter.getPosition() == null)
                || (target.getPosition() == null)) {
                return null;
            }
            StringBuilder ret = new StringBuilder("");
            WeaponFireInfo guessInfo = new WeaponFireInfo(shooter, new EntityState(shooter), target, null, weapon, game);
            WeaponFireInfo accurateInfo = new WeaponFireInfo(shooter, target, weapon, game);
            if (guessInfo.getToHit().getValue() == accurateInfo.getToHit().getValue()) {
                return null;
            }
            ret.append("Incorrect To Hit prediction, weapon ")
               .append(weapon.getName())
               .append(" (")
               .append(shooter.getChassis())
               .append(" vs ")
               .append(target.getDisplayName())
               .append("):\n");
            ret.append(" Guess: ")
               .append(Integer.toString(guessInfo.getToHit().getValue()))
               .append(" ")
               .append(guessInfo.getToHit().getDesc())
               .append("\n");
            ret.append(" Real:  ")
               .append(Integer.toString(accurateInfo.getToHit().getValue()))
               .append(" ")
               .append(accurateInfo.getToHit().getDesc()).append("\n");
            return ret.toString();
        } finally {
            Logger.methodEnd(getClass(), METHOD_NAME);
        }
    }

    /**
     * Mostly for debugging, this returns a non-null string that describes how the guess on a physical attack failed to
     * be perfectly accurate, or null if accurate
     *
     * @param shooter The attacker
     * @param target Who's being shot at.
     * @param attackType Type of physical attack.
     * @param game The game in progress.
     * @return NULL for a good guess or a description of what was wrong with the guess.
     */
    String checkGuessPhysical(Entity shooter, Targetable target, PhysicalAttackType attackType, IGame game) {
        final String METHOD_NAME = "getGuess_Physical(Entity, Targetable, PhysicalAttackType, IGame)";
        Logger.methodBegin(FireControl.class, METHOD_NAME);

        try {
            // only mechs can do physicals
            if (!(shooter instanceof Mech)) {
                return null;
            }

            StringBuilder ret = new StringBuilder("");
            if (shooter.getPosition() == null) {
                return "Shooter has NULL coordinates!";
            } else if (target.getPosition() == null) {
                return "Target has NULL coordinates!";
            }
            PhysicalInfo guessInfo = new PhysicalInfo(shooter, null, target, null, attackType, game);
            PhysicalInfo accurateInfo = new PhysicalInfo(shooter, target, attackType, game);
            if (guessInfo.getToHit().getValue() == accurateInfo.getToHit().getValue()) {
                return null;
            }
            ret.append("Incorrect To Hit prediction, physical attack ").append(attackType.name()).append(":\n");
            ret.append(" Guess: ")
               .append(Integer.toString(guessInfo.getToHit().getValue()))
               .append(" ")
               .append(guessInfo.getToHit().getDesc())
               .append("\n");
            ret.append(" Real:  ")
               .append(Integer.toString(accurateInfo.getToHit().getValue()))
               .append(" ")
               .append(accurateInfo.getToHit().getDesc())
               .append("\n");
            return ret.toString();
        } finally {
            Logger.methodEnd(getClass(), METHOD_NAME);
        }
    }

    /**
     * Mostly for debugging, this returns a non-null string that describes how any possible guess has failed to be
     * perfectly accurate. or null if perfect
     *
     * @param shooter The attacker.
     * @param game The current game in progress.
     * @return NULL if all guesses were accurate, otherwise a description of what was wrong.
     */
    String checkAllGuesses(Entity shooter, IGame game) {
        final String METHOD_NAME = "checkAllGuesses(Entity, IGame)";
        Logger.methodBegin(FireControl.class, METHOD_NAME);

        try {
            StringBuilder ret = new StringBuilder();
            List<Targetable> enemies = getTargetableEnemyEntities(shooter, game);
            for (Targetable e : enemies) {
                for (Mounted weapon : shooter.getWeaponList()) {
                    String explanation = checkGuess(shooter, e, weapon, game);
                    if (explanation != null) {
                        ret.append(explanation);
                    }
                }
                String explanationPhysical = checkGuessPhysical(shooter, e, PhysicalAttackType.RIGHT_KICK, game);
                if (explanationPhysical != null) {
                    ret.append(explanationPhysical);
                }
                explanationPhysical = checkGuessPhysical(shooter, e, PhysicalAttackType.LEFT_KICK, game);
                if (explanationPhysical != null) {
                    ret.append(explanationPhysical);
                }
                explanationPhysical = checkGuessPhysical(shooter, e, PhysicalAttackType.RIGHT_PUNCH, game);
                if (explanationPhysical != null) {
                    ret.append(explanationPhysical);
                }
                explanationPhysical = checkGuessPhysical(shooter, e, PhysicalAttackType.LEFT_PUNCH, game);
                if (explanationPhysical != null) {
                    ret.append(explanationPhysical);
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
     * Creates a firing plan that fires all weapons with nonzero to hit value at a target ignoring heat, and using best
     * guess from different states. Does not change facing.
     *
     * @param shooter The attacker.
     * @param shooterState The attacker's current {@link EntityState}.
     * @param target The target of the attack.
     * @param targetState The target's current {@link EntityState}.
     * @param game The currently running game.
     * @return The resultant {@link FiringPlan}.
     */
    FiringPlan guessFullFiringPlan(Entity shooter, EntityState shooterState,
                                   Targetable target, EntityState targetState, IGame game) {
        if (shooterState == null) {
            shooterState = new EntityState(shooter);
        }
        FiringPlan myplan = new FiringPlan(target, shooter, game);

        // Make sure my target an I are actually on the board.
        // I believe that embarked/mounted units as well as off-board units cause a NULL possition, so these can
        // probably be downgraded to INFO at some point.
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

        // cycle through my weapons
        for (Mounted weapon : shooter.getWeaponList()) {
            WeaponFireInfo shoot = new WeaponFireInfo(shooter, shooterState, target, targetState, weapon, game);
            if (shoot.getProbabilityToHit() > 0) {
                myplan.addWeaponFire(shoot);
            }
        }

        // Calculate the utility of this firing plan.
        // ToDo Add a heatTolerance behavior setting.
        int overheatValue = shooter.tracksHeat() ?
                            ((shooter.getHeatCapacity() - shooterState.getHeat()) + 5) :
                            999;
        myplan.calcUtility(overheatValue);
        return myplan;
    }

    /**
     * Creates a firing plan that fires all weapons with nonzero to hit value in a air to ground strike
     *
     * @param shooter The attacker.
     * @param target The target of the attack.
     * @param targetState The target's current {@link EntityState}.
     * @param shooterPath The attacker's flight path.
     * @param game The currently running game.
     * @param assumeUnderFlightPath Set TRUE to autmatically assume the target is in the attacker's flight path.
     * @return The resultant {@link FiringPlan}.
     */
    FiringPlan guessFullAirToGroundPlan(Entity shooter, Targetable target,
                                        EntityState targetState, MovePath shooterPath, IGame game,
                                        boolean assumeUnderFlightPath) {
        if (targetState == null) {
            targetState = new EntityState(target);
        }

        // Make sure our target is in our flight path.
        if (!assumeUnderFlightPath && !isTargetUnderMovePath(shooterPath, targetState)) {
            return new FiringPlan(target, shooter, game);
        }

        FiringPlan myPlan = new FiringPlan(target, shooter, game);

        // Make sure my target an I are actually on the board.
        // I believe that embarked/mounted units as well as off-board units cause a NULL possition, so these can
        // probably be downgraded to INFO at some point.
        if (shooter.getPosition() == null) {
            Logger.log(getClass(),
                       "guessFullAirToGroundPlan(Entity, Targetable, EntityState, MovePath, IGame, boolean)",
                       LogLevel.ERROR, "Shooter's position is NULL!");
            return myPlan;
        }
        if (target.getPosition() == null) {
            Logger.log(getClass(),
                       "guessFullAirToGroundPlan(Entity, Targetable, EntityState, MovePath, IGame, boolean)",
                       LogLevel.ERROR, "Target's position is NULL!");
            return myPlan;
        }

        // cycle through my weapons
        for (Mounted mw : shooter.getWeaponList()) {
            WeaponFireInfo shoot = new WeaponFireInfo(shooter, shooterPath, target, targetState, mw, game, true);
            if (shoot.getProbabilityToHit() > 0) {
                myPlan.addWeaponFire(shoot);
            }
        }

        // Calculate the utility of this firing plan.
        // ToDo Add a heatTolerance behavior setting.
        int overheatValue = shooter.tracksHeat() ?
                            ((shooter.getHeatCapacity() - shooter.getHeat()) + 5) :
                            999;
        myPlan.calcUtility(overheatValue);
        return myPlan;
    }

    /**
     * Guesses what the expected damage would be if the shooter fired all of its weapons at the target
     *
     * @param shooter The attacker.
     * @param shooterState The attacker's current {@link EntityState}.
     * @param target The target of the attack.
     * @param targetState The target's current {@link EntityState}.
     * @param game The currently running game.
     * @return The expected amount of damage.
     */
    double guessExpectedDamage(Entity shooter, EntityState shooterState,
                               Targetable target, EntityState targetState, IGame game) {
        // Generate the firing plan.
        FiringPlan fullplan = guessFullFiringPlan(shooter, shooterState, target, targetState, game);
        return fullplan.getExpectedDamage();
    }

    /**
     * Creates a firing plan that fires all weapons with nonzero to hit value at a target ignoring heat, and using
     * actual game ruleset from different states
     *
     * @param shooter The attacker.
     * @param target The target of the attack.
     * @param game The currently running game.
     * @return The resulting {@link FiringPlan}.
     */
    FiringPlan getAlphaStrikePlan(Entity shooter, Targetable target, IGame game) {
        FiringPlan myPlan = new FiringPlan(target, shooter, game);

        // Make sure my target an I are actually on the board.
        // I believe that embarked/mounted units as well as off-board units cause a NULL possition, so these can
        // probably be downgraded to INFO at some point.
        if (shooter.getPosition() == null) {
            Logger.log(getClass(),
                       "getAlphaStrikePlan(Entity, Targetable, IGame)", LogLevel.ERROR,
                       "Shooter's position is NULL!");
            return myPlan;
        }
        if (target.getPosition() == null) {
            Logger.log(getClass(),
                       "getAlphaStrikePlan(Entity, Targetable, IGame)", LogLevel.ERROR,
                       "Target's position is NULL!");
            return myPlan;
        }

        // cycle through my weapons
        for (Mounted weapon : shooter.getWeaponList()) { // cycle through my weapons
            WeaponFireInfo shoot = new WeaponFireInfo(shooter, target, weapon, game);
            if ((shoot.getProbabilityToHit() > 0)) {
                myPlan.addWeaponFire(shoot);
            }
        }

        // Calculate the utility of this firing plan.
        // ToDo Add a heatTolerance behavior setting.
        int overheatValue = shooter.tracksHeat() ?
                            ((shooter.getHeatCapacity() - shooter.getHeat()) + 5) :
                            999;
        myPlan.calcUtility(overheatValue);
        return myPlan;
    }

    /**
     * Creates an array that gives the 'best' firing plan (the maximum utility) under the heat of the index
     *
     * @param alphaStrikePlan A {@link FiringPlan} firing every weapon that has any chance to hit the target at all.
     * @param maxHeat The highest heat value we're willing to tolerate.
     * @return
     */
    List<FiringPlan> calcFiringPlansUnderHeat(FiringPlan alphaStrikePlan, int maxHeat) {

        List<FiringPlan> bestPlans = new ArrayList<FiringPlan>();
        bestPlans.add(alphaStrikePlan);

         // Can't fire for less than zero heat.
        if (maxHeat < 0) {
            maxHeat = 0;
        }

        Targetable target = alphaStrikePlan.getTarget();
        Entity shooter = alphaStrikePlan.getShooter();
        IGame game = alphaStrikePlan.getGame();

        // Our first plan is a zero heat plan.
        FiringPlan zeroHeatPlan = new FiringPlan(target, shooter, game);

        // Everything else falls in between.
        List<WeaponFireInfo> hotWeapons = new ArrayList<WeaponFireInfo>();

        // first extract any firings of zero heat
        for (WeaponFireInfo weaponFireInfo : alphaStrikePlan.getFiringInfo()) {
            if (weaponFireInfo.getHeat() == 0) {
                zeroHeatPlan.addWeaponFire(weaponFireInfo);
            } else {
                hotWeapons.add(weaponFireInfo);
            }
        }

        // Add in the zero heat plan.
        // ToDo Add a heatTolerance behavior setting.
        int overheatValue = shooter.tracksHeat() ?
                            ((shooter.getHeatCapacity() - shooter.getHeat()) + 5) :
                            999;
        zeroHeatPlan.calcUtility(overheatValue);
        bestPlans.add(zeroHeatPlan);

        // Sort the weapon list by heat.
        Collections.sort(hotWeapons, new Comparator<WeaponFireInfo>() {
            public int compare(WeaponFireInfo o1, WeaponFireInfo o2) {
                if (o1.getHeat() > o2.getHeat()) {
                    return 1;
                } else if (o1.getHeat() < o2.getHeat()) {
                    return -1;
                }
                return 0;
            }
        });

        // Each firing plan will increase in heat from zero up to our limit.
        FiringPlan lastPlan = zeroHeatPlan;
        for (int i = 1; i <= maxHeat; i++) {
            for (WeaponFireInfo weaponFireInfo : hotWeapons) {

                // Create a new plan using all the weapons of the last plan.
                FiringPlan testPlan = new FiringPlan(target, shooter, game);
                testPlan.addWeaponFireList(lastPlan.getFiringInfo());

                // Does this weapon get us to our next heat level?
                if (i - weaponFireInfo.getHeat() < 0) {
                    continue;
                }

                // Skip weapons we've already accounted for.
                if (testPlan.containsWeapon(weaponFireInfo.getWeapon())) {
                    continue;
                }

                // Add this weapon and plan only if its utility is higher.
                testPlan.addWeaponFire(weaponFireInfo);
                testPlan.calcUtility(overheatValue);
                if (testPlan.getUtility() > lastPlan.getUtility()) {
                    bestPlans.add(testPlan);
                    lastPlan = testPlan;
                }
            }
        }

        // Sort the firing plans by their utility, highest to lowest.
        Collections.sort(bestPlans, new Comparator<FiringPlan>() {
            public int compare(FiringPlan o1, FiringPlan o2) {
                if (o1.getUtility() > o2.getUtility()) {
                    return -1;
                } else if (o1.getUtility() < o2.getUtility()) {
                    return 1;
                }
                return 0;
            }
        });

        return bestPlans;
    }

    /**
     * Gets the 'best' firing plan under a certain heat No twisting is done
     *
     * @param shooter The attacker.
     * @param target The target.
     * @param maxHeat The maximum heat level we're willing to risk.
     * @param game The currently running game.
     * @return The firing plan with the highest {@link FiringPlan#getUtility()} under the heat threshold.
     */
    FiringPlan getBestFiringPlanUnderHeat(Entity shooter, Targetable target,
                                          int maxHeat, IGame game) {

        // can't have less than zero heat
        if (maxHeat < 0) {
            maxHeat = 0;
        }
        FiringPlan alphaStrikePlan = getAlphaStrikePlan(shooter, target, game);
        if (alphaStrikePlan.getHeat() <= maxHeat) {
            return alphaStrikePlan;
        }
        List<FiringPlan> heatplans = calcFiringPlansUnderHeat(alphaStrikePlan, maxHeat);
        return heatplans.get(0);
    }

    /*
     * Gets the 'best' firing plan, using heat as a disutility. No twisting is done
     *
     * @param shooter The attacker.
     * @param target The target.
     * @param game The currently running game.
     * @return The firing plan with the highest {@link FiringPlan#getUtility()} taking into account heat.
     */
    FiringPlan getBestFiringPlan(Entity shooter, Targetable target, IGame game) {
        FiringPlan alphaStrikePlan = getAlphaStrikePlan(shooter, target, game);

        // no need to optimize heat if we don't track it.
        if (!shooter.tracksHeat()) {
            return alphaStrikePlan;
        }

        List<FiringPlan> heatPlans = calcFiringPlansUnderHeat(alphaStrikePlan, alphaStrikePlan.getHeat());
        return heatPlans.get(0);
    }

    /**
     * Guesses the 'best' firing plan under a certain heat No twisting is done
     *
     * @param shooter The attacker.
     * @param shooterState The current {@link EntityState} of the attacker.
     * @param target The target.
     * @param targetState The current {@link EntityState} of the target.
     * @param maxHeat The most heat we're willing to build up.
     * @param game The currently running game.
     * @return The firing plan with the highest {@link FiringPlan#getUtility()} taking into account heat.
     */
    FiringPlan guessBestFiringPlanUnderHeat(Entity shooter, EntityState shooterState,
                                            Targetable target, EntityState targetState,
                                            int maxHeat, IGame game) {
        // can't have less than zero heat
        if (maxHeat < 0) {
            maxHeat = 0;
        }
        FiringPlan fullPlan = guessFullFiringPlan(shooter, shooterState, target, targetState, game);
        if (fullPlan.getHeat() <= maxHeat) {
            return fullPlan;
        }
        List<FiringPlan> heatPlans = calcFiringPlansUnderHeat(fullPlan, maxHeat);
        return heatPlans.get(0);
    }

    /**
     * Guesses the 'best' firing plan, using heat as a disutility. No twisting
     * is done
     *
     * @param shooter The attacker.
     * @param shooterState The current {@link EntityState} of the attacker.
     * @param target The target.
     * @param targetState The current {@link EntityState} of the target.
     * @param game The currently running game.
     * @return The firing plan with the highest {@link FiringPlan#getUtility()} taking into account heat.
     */
    FiringPlan guessBestFiringPlan(Entity shooter, EntityState shooterState,
                                   Targetable target, EntityState targetState,
                                   IGame game) {
        if (shooterState == null) {
            shooterState = new EntityState(shooter);
        }
        FiringPlan fullPlan = guessFullFiringPlan(shooter, shooterState, target, targetState, game);

        // no need to optimize heat for units that don't track it.
        if (!shooter.tracksHeat()) {
            return fullPlan;
        }
        int overheatValue = shooter.tracksHeat() ?
                            ((shooter.getHeatCapacity() - shooter.getHeat()) + 4) :
                            999;
        List<FiringPlan> heatPlans = calcFiringPlansUnderHeat(fullPlan, fullPlan.getHeat());
        return heatPlans.get(0);
    }

    /**
     * Gets the 'best' firing plan under a certain heat includes the option of
     * twisting
     *
     * @param shooter The attacker.
     * @param target The target.
     * @param maxHeat The most heat we're willing to build up.
     * @param game The currently running game.
     * @return The firing plan with the highest {@link FiringPlan#getUtility()} taking into account heat.
     */
    FiringPlan getBestFiringPlanUnderHeatWithTwists(Entity shooter, Targetable target, int maxHeat, IGame game) {
        return guessBestFiringPlanUnderHeatWithTwists(shooter, null, target, null, maxHeat, game);
    }

    /**
     * Gets the 'best' firing plan using heat as disutiltiy includes the option of twisting
     *
     * @param shooter The attacker.
     * @param target The target.
     * @param game The currently running game.
     * @return The firing plan with the highest {@link FiringPlan#getUtility()} taking into account heat.
     */
    FiringPlan getBestFiringPlanWithTwists(Entity shooter, Targetable target, IGame game) {
        FiringPlan alphaStrikePlan = getAlphaStrikePlan(shooter, target, game);
        return getBestFiringPlanUnderHeatWithTwists(shooter, target, alphaStrikePlan.getHeat(), game);
    }

    /**
     * Guesses the 'best' firing plan under a certain heat includes the option of twisting
     *
     * @param shooter The attacker.
     * @param shooterState The current {@link EntityState} of the attacker.
     * @param target The target.
     * @param targetState The current {@link EntityState} of the target.
     * @param maxHeat The most heat we're willing to build up.
     * @param game The currently running game.
     * @return The firing plan with the highest {@link FiringPlan#getUtility()} taking into account heat.
     */
    FiringPlan guessBestFiringPlanUnderHeatWithTwists(Entity shooter, EntityState shooterState,
                                                      Targetable target, EntityState targetState,
                                                      int maxHeat, IGame game) {
        // ToDo Extended Torso Twist

        if (shooterState == null) {
            shooterState = new EntityState(shooter);
        }
        int originalFacing = shooterState.getFacing();

        // Base plan.
        FiringPlan noTwistPlan = guessBestFiringPlanUnderHeat(shooter, shooterState, target, targetState, maxHeat,
                                                              game);

        // If we can't twist, we're done.
        if (!shooter.canChangeSecondaryFacing()) {
            return noTwistPlan;
        }

        // Twist right.
        shooterState.setSecondaryFacing(correctFacing(originalFacing + 1));
        FiringPlan rightTwistPlan = guessBestFiringPlanUnderHeat(shooter, shooterState, target, targetState, maxHeat,
                                                                 game);
        rightTwistPlan.setTwist(1);

        // Twist left.
        shooterState.setSecondaryFacing(correctFacing(originalFacing - 1));
        FiringPlan leftTwistPlan = guessBestFiringPlanUnderHeat(shooter, shooterState, target, targetState, maxHeat,
                                                                game);
        leftTwistPlan.setTwist(-1);

        // Reset facing.
        shooterState.setSecondaryFacing(originalFacing);

        // Return the highest utility plan.
        if ((noTwistPlan.getUtility() > rightTwistPlan.getUtility())
            && (noTwistPlan.getUtility() > leftTwistPlan.getUtility())) {
            return noTwistPlan;
        }
        if (rightTwistPlan.getUtility() > leftTwistPlan.getUtility()) {
            return rightTwistPlan;
        }
        return leftTwistPlan;
    }

    /**
     * Guesses the 'best' firing plan under a certain heat includes the option of twisting
     *
     * @param shooter The attacker.
     * @param shooterState The current {@link EntityState} of the attacker.
     * @param target The target.
     * @param targetState The current {@link EntityState} of the target.
     * @param game The currently running game.
     * @return The firing plan with the highest {@link FiringPlan#getUtility()} taking into account heat.
     */
    FiringPlan guessBestFiringPlanWithTwists(Entity shooter, EntityState shooterState,
                                             Targetable target, EntityState targetState,
                                             IGame game) {
        FiringPlan fullPlan = guessFullFiringPlan(shooter, shooterState, target, targetState, game);
        return guessBestFiringPlanUnderHeatWithTwists(shooter, shooterState, target, targetState, fullPlan.getHeat(),
                                                      game);
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
     *
     * @param shooter The attacker
     * @param game The currently runninng game
     * @return A {@link List} of {@link Targetable} potential targets.
     */
    List<Targetable> getTargetableEnemyEntities(Entity shooter, IGame game) {
        List<Targetable> ret = new ArrayList<Targetable>();
        for (Entity e : game.getEntitiesVector()) {
            // Skip units that cannot be shot at.
            if (e.getOwner() == null) {
                continue;
            } else if (!e.getOwner().isEnemyOf(shooter.getOwner())) {
                continue;
            } else if (e.getPosition() == null) {
                continue;
            } else if (e.isOffBoard()) {
                continue;
            } else if (!e.isTargetable()) {
                continue;
            }
            ret.add(e);

            // If the target is in or on a building, I may want to shoot the building instead.
            Building building = game.getBoard().getBuildingAt(e.getPosition());
            if (building == null) {
                continue;
            }
            IHex hex = game.getBoard().getHex(e.getPosition());
            Targetable buildingTarget = new BuildingTarget(hex.getCoords(), game.getBoard(), building.getType());
            int buildingElevation = hex.terrainLevel(Terrains.BLDG_ELEV);
            // In the building.
            if (e.getElevation() < buildingElevation) {
                ret.add(buildingTarget);

            // On a building we might want to collapse.
            } else if (e.getElevation() == buildingElevation) {
                double dmgPotential = estimatedDamageAtRange(shooter, e.getPosition().distance(shooter.getPosition()));
                // If we can reduce the CF enough, add the building.
                if (dmgPotential >= building.getCurrentCF(hex.getCoords())) {
                    ret.add(buildingTarget);
                }
            }
        }

        // Add the non-entity targets.
        ret.addAll(additionalTargets);
        return ret;
    }

    /**
     * This is it. Calculate the 'best' possible firing plan for this entity.  Overload this function if you think you
     * can do better.
     *
     * @param shooter The attacker.
     * @param game The game currently being played.
     * @return The best {@link FiringPlan} I can come up with.
     */
    FiringPlan getBestFiringPlan(Entity shooter, IGame game) {
        FiringPlan bestPlan = null;
        List<Targetable> enemies = getTargetableEnemyEntities(shooter, game);

        // ToDo heat tolerance.
        int overheatValue = shooter.tracksHeat() ?
                            ((shooter.getHeatCapacity() - shooter.getHeat()) + 4) :
                            999;

        // Construct a plan for each target.
        for (Targetable e : enemies) {
            FiringPlan plan = getBestFiringPlanWithTwists(shooter, e, game);
            plan.calcUtility(overheatValue);

            // Find hte plan with the highest utility.
            if ((bestPlan == null) || (plan.getUtility() > bestPlan.getUtility())) {
                bestPlan = plan;
            }
        }
        return bestPlan;
    }

    /**
     * Returns the estimated damage I can do at a given range.
     *
     * @param shooter The attacker.
     * @param range The range to be tested.
     * @return the estimated damage I can do at a given range.
     */
    public double estimatedDamageAtRange(Entity shooter, int range) {
        return new EntityState(shooter).estimatedDamageAtRange(range, isExtremeRangeUsed(shooter.getGame()));
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
     *
     * @param shooter The attacker.
     */
    public void loadAmmo(Entity shooter, Targetable target) {
        if (shooter == null) {
            return;
        }

        // Loading ammo for all my weapons.
        Iterator<Mounted> weapons = shooter.getWeapons();
        while (weapons.hasNext()) {
            Mounted currentWeapon = weapons.next();
            WeaponType weaponType = (WeaponType)currentWeapon.getType();

            // Skip weapons that don't use ammo.
            if (AmmoType.T_NA == weaponType.getAmmoType()) {
                continue;
            }

            Mounted mountedAmmo = getPreferredAmmo(shooter, target, weaponType);
            // Log failures.
            if ((mountedAmmo != null) && !shooter.loadWeapon(currentWeapon, mountedAmmo)) {
                Logger.log(getClass(), "loadAmmo(Entity, Targetable)", LogLevel.WARNING,
                           shooter.getDisplayName() + " tried to load " + currentWeapon.getName() + " with ammo " +
                           mountedAmmo.getDesc() + " but failed somehow.");
            }
        }
    }

    protected Mounted getPreferredAmmo(Entity shooter, Targetable target, WeaponType weaponType) {
        final String METHOD_NAME = "getPreferredAmmo(Entity, Targetable, WeaponType)";

        StringBuilder msg = new StringBuilder("Getting ammo for ").append(weaponType.getShortName()).append(" firing at ").append(target.getDisplayName());
        Entity targetEntity = null;
        Mounted preferredAmmo = null;

        try {
            if (target instanceof Entity) {
                targetEntity = (Entity)target;
            }

            // Find the ammo that is valid for this weapon.
            List<Mounted> ammo = shooter.getAmmo();
            List<Mounted> validAmmo = new ArrayList<Mounted>();
            for (Mounted a : ammo) {
                if (AmmoType.isAmmoValid(a, weaponType)) {
                    validAmmo.add(a);
                }
            }

            // If no valid ammo was found, return nothing.
            if (validAmmo.isEmpty()) {
                return preferredAmmo;
            }
            msg.append("\n\tFound ").append(validAmmo.size()).append(" units of valid ammo.");

            int range = shooter.getPosition().distance(target.getPosition());
            msg.append("\n\tRange to target is ").append(range);

            // AMS only uses 1 type of ammo.
            if (weaponType.hasFlag(WeaponType.F_AMS)) {
                return validAmmo.get(0);
            }

            // ATMs
            if (weaponType instanceof ATMWeapon) {
                return getAtmAmmo(validAmmo, range);
            }

            // Target is a building.
            if (target instanceof BuildingTarget) {
                msg.append("\n\tTarget is a building... ");
                preferredAmmo = getHeatAmmo(validAmmo, weaponType, range);
                if (preferredAmmo != null) {
                    msg.append("Burn It Down!");
                    return preferredAmmo;
                }

            // Entity targets.
            } else if (targetEntity != null) {
                // Airborne targts
                if (targetEntity.isAirborne()) {
                    msg.append("\n\tTarget is airborne... ");
                    preferredAmmo = getAntiAirAmmo(validAmmo, weaponType, range);
                    if (preferredAmmo != null) {
                        msg.append("Shoot It Down!");
                        return preferredAmmo;
                    }
                }
                // Battle Armor, Tanks and Protos, oh my!
                if ((targetEntity instanceof BattleArmor)
                        || (targetEntity instanceof Tank)
                        || (targetEntity instanceof Protomech)) {
                    msg.append("\n\tTarget is BA/Proto/Tank... ");
                    preferredAmmo = getAntiVeeAmmo(validAmmo, weaponType, range);
                    if (preferredAmmo != null) {
                        msg.append("We have ways of dealing with that.");
                        return preferredAmmo;
                    }
                }
                // PBI
                if (targetEntity instanceof Infantry) {
                    msg.append("\n\tTarget is infantry... ");
                    preferredAmmo = getAntiInfantryAmmo(validAmmo, weaponType, range);
                    if (preferredAmmo != null) {
                        msg.append("They squish nicely.");
                        return preferredAmmo;
                    }
                }
                // On his last legs
                if (targetEntity.getDamageLevel() >= Entity.DMG_HEAVY) {
                    msg.append("\n\tTarget is heavily damaged... ");
                    preferredAmmo = getClusterAmmo(validAmmo, weaponType, range);
                    if (preferredAmmo != null) {
                        msg.append("Let's find a soft spot.");
                        return preferredAmmo;
                    }
                }
                // He's running hot.
                if (targetEntity.getHeat() >= 9) {
                    msg.append("\n\tTarget is at ").append(targetEntity.getHeat()).append(" heat... ");
                    preferredAmmo = getHeatAmmo(validAmmo, weaponType, range);
                    if (preferredAmmo != null) {
                        msg.append("Let's heat him up more.");
                        return preferredAmmo;
                    }
                }
                // Everything else.
                msg.append("\n\tTarget is a hard target... ");
                preferredAmmo = getHardTargetAmmo(validAmmo, weaponType, range);
                if (preferredAmmo != null) {
                    msg.append("Fill him with holes!");
                    return preferredAmmo;
                }
            }

            // If we've gotten this far, no specialized ammo has been loaded
            if (weaponType instanceof MMLWeapon) {
                msg.append("\n\tLoading MML Ammo.");
                preferredAmmo = getGeneralMmlAmmo(validAmmo, range);
            } else {
                msg.append("\n\tLoading first available ammo.");
                preferredAmmo = validAmmo.get(0);
            }
            return preferredAmmo;
        } finally {
            msg.append("\n\tReturning: ").append(preferredAmmo == null ? "null" : preferredAmmo.getDesc());
            Logger.log(getClass(), METHOD_NAME, LogLevel.DEBUG, msg.toString());
        }
    }

    protected Mounted getGeneralMmlAmmo(List<Mounted> ammoList, int range) {
        Mounted returnAmmo = null;

        // Get the LRM and SRM bins if we have them.
        Mounted mmlSrm = null;
        Mounted mmlLrm = null;
        for (Mounted ammo : ammoList) {
            AmmoType type = (AmmoType)ammo.getType();
            if ((mmlLrm == null) && type.hasFlag(AmmoType.F_MML_LRM)) {
                mmlLrm = ammo;
            } else if (mmlSrm == null) {
                mmlSrm = ammo;
            } else if ((mmlSrm != null) && (mmlLrm != null)) {
                break;
            }
        }

        // Out of SRM range.
        if (range > 9) {
            returnAmmo = mmlLrm;

        // LRMs have better chance to hit if we have them.
        } else if (range > 5) {
            returnAmmo = (mmlLrm == null ? mmlSrm : mmlLrm);

        // Left with SRMS.
        } else {
            returnAmmo = mmlSrm;
        }
        return returnAmmo;
    }

    protected Mounted getAtmAmmo(List<Mounted> ammoList, int range) {
        Mounted returnAmmo = null;

        // Get the Hi-Ex, Ex-Range and Standard ammo bins if we have them.
        Mounted heAmmo = null;
        Mounted erAmmo = null;
        Mounted stAmmo = null;
        for (Mounted ammo : ammoList) {
            AmmoType type = (AmmoType)ammo.getType();
            if ((heAmmo == null) && (AmmoType.M_HIGH_EXPLOSIVE == type.getMunitionType())) {
                heAmmo = ammo;
            } else if ((erAmmo == null) && (AmmoType.M_EXTENDED_RANGE == type.getMunitionType())) {
                erAmmo = ammo;
            } else if ((stAmmo == null) && (AmmoType.M_STANDARD == type.getMunitionType())) {
                stAmmo = ammo;
            } else if ((heAmmo != null) && (erAmmo == null) && (stAmmo == null)) {
                break;
            }
        }

        // Beyond 15 hexes is ER Ammo only range.
        if (range > 15) {
            returnAmmo = erAmmo;
        // ER Ammo has a better chance to hit past 10 hexes.
        } else if (range > 10) {
            returnAmmo = (erAmmo == null ? stAmmo : erAmmo);
        // At 7-10 hexes, go with Standard, then ER then HE due to hit odds.
        } else if (range > 6) {
            if (stAmmo != null) {
                returnAmmo = stAmmo;
            } else if (erAmmo != null) {
                returnAmmo = erAmmo;
            } else {
                returnAmmo = heAmmo;
            }
        // Six hexes is at min for ER, and medium for both ST & HE.
        } else if (range == 6) {
            if (heAmmo != null) {
                returnAmmo = heAmmo;
            } else if (stAmmo != null) {
                returnAmmo = stAmmo;
            } else {
                returnAmmo = erAmmo;
            }
        // 4-5 hexes is medium for HE, short for ST and well within min for ER.
        } else if (range > 3) {
            if (stAmmo != null) {
                returnAmmo = stAmmo;
            } else if (heAmmo != null) {
                returnAmmo = heAmmo;
            } else {
                returnAmmo = erAmmo;
            }
        // Short range for HE.
        } else {
            if (heAmmo != null) {
                returnAmmo = heAmmo;
            } else if (stAmmo != null) {
                returnAmmo = stAmmo;
            } else {
                returnAmmo = erAmmo;
            }
        }
        return returnAmmo;
    }

    protected Mounted getAntiVeeAmmo(List<Mounted> ammoList, WeaponType weaponType, int range) {
        Mounted returnAmmo = null;
        Mounted mmlLrm = null;
        Mounted mmlSrm = null;

        for (Mounted ammo : ammoList) {
            AmmoType ammoType = (AmmoType)ammo.getType();
            if (AmmoType.M_CLUSTER == ammoType.getMunitionType()
                    || AmmoType.M_INCENDIARY == ammoType.getMunitionType()
                    || AmmoType.M_INCENDIARY_AC == ammoType.getMunitionType()
                    || AmmoType.M_INCENDIARY_LRM == ammoType.getMunitionType()
                    || AmmoType.M_INFERNO == ammoType.getMunitionType()
                    || AmmoType.M_INFERNO_IV == ammoType.getMunitionType()) {

                // MMLs have additional considerations.
                if (!(weaponType instanceof MMLWeapon)) {
                    returnAmmo = ammo;
                    break;
                }
                if ((mmlLrm == null) && ammoType.hasFlag(AmmoType.F_MML_LRM)) {
                    mmlLrm = ammo;
                } else if (mmlSrm == null) {
                    mmlSrm = ammo;
                } else if (mmlLrm != null) {
                    break;
                }
            }
        }

        // MML ammo depends on range.
        if (weaponType instanceof MMLWeapon) {
            if (range > 9) { // Out of SRM range
                returnAmmo = mmlLrm;
            } else if (range > 6) { // SRM long range.
                returnAmmo = (mmlLrm == null ? mmlSrm : mmlLrm);
            } else {
                returnAmmo = (mmlSrm == null ? mmlLrm : mmlSrm);
            }
        }

        return returnAmmo;
    }

    protected Mounted getAntiInfantryAmmo(List<Mounted> ammoList, WeaponType weaponType, int range) {
        Mounted returnAmmo = null;
        Mounted mmlLrm = null;
        Mounted mmlSrm = null;

        for (Mounted ammo : ammoList) {
            AmmoType ammoType = (AmmoType)ammo.getType();
            if (AmmoType.M_FLECHETTE == ammoType.getMunitionType()
                || AmmoType.M_FRAGMENTATION == ammoType.getMunitionType()
                || AmmoType.M_CLUSTER == ammoType.getMunitionType()
                || AmmoType.M_INCENDIARY == ammoType.getMunitionType()
                || AmmoType.M_INCENDIARY_LRM == ammoType.getMunitionType()
                || AmmoType.M_INCENDIARY_AC == ammoType.getMunitionType()
                || AmmoType.M_INFERNO == ammoType.getMunitionType()
                || AmmoType.M_INFERNO_IV == ammoType.getMunitionType()) {

                // MMLs have additional considerations.
                if (!(weaponType instanceof MMLWeapon)) {
                    returnAmmo = ammo;
                    break;
                }
                if ((mmlLrm == null) && ammoType.hasFlag(AmmoType.F_MML_LRM)) {
                    mmlLrm = ammo;
                } else if (mmlSrm == null) {
                    mmlSrm = ammo;
                } else if ((mmlLrm != null) && (mmlSrm != null)) {
                    break;
                }
            }
        }

        // MML ammo depends on range.
        if (weaponType instanceof MMLWeapon) {
            if (range > 9) { // Out of SRM range
                returnAmmo = mmlLrm;
            } else if (range > 6) { // SRM long range.
                returnAmmo = (mmlLrm == null ? mmlSrm : mmlLrm);
            } else {
                returnAmmo = (mmlSrm == null ? mmlLrm : mmlSrm);
            }
        }

        return returnAmmo;
    }

    protected Mounted getHeatAmmo(List<Mounted> ammoList, WeaponType weaponType, int range) {
        Mounted returnAmmo = null;
        Mounted mmlLrm = null;
        Mounted mmlSrm = null;

        for (Mounted ammo : ammoList) {
            AmmoType ammoType = (AmmoType)ammo.getType();
            if (AmmoType.M_INCENDIARY == ammoType.getMunitionType()
                || AmmoType.M_INCENDIARY_LRM == ammoType.getMunitionType()
                || AmmoType.M_INCENDIARY_AC == ammoType.getMunitionType()
                || AmmoType.M_INFERNO == ammoType.getMunitionType()
                || AmmoType.M_INFERNO_IV == ammoType.getMunitionType()) {

                // MMLs have additional considerations.
                if (!(weaponType instanceof MMLWeapon)) {
                    returnAmmo = ammo;
                    break;
                }
                if ((mmlLrm == null) && ammoType.hasFlag(AmmoType.F_MML_LRM)) {
                    mmlLrm = ammo;
                } else if (mmlSrm == null) {
                    mmlSrm = ammo;
                } else if (mmlLrm != null) {
                    break;
                }
            }
        }

        // MML ammo depends on range.
        if (weaponType instanceof MMLWeapon) {
            if (range > 9) { // Out of SRM range
                returnAmmo = mmlLrm;
            } else if (range > 6) { // SRM long range.
                returnAmmo = (mmlLrm == null ? mmlSrm : mmlLrm);
            } else {
                returnAmmo = (mmlSrm == null ? mmlLrm : mmlSrm);
            }
        }

        return returnAmmo;
    }

    protected Mounted getClusterAmmo(List<Mounted> ammoList, WeaponType weaponType, int range) {
        Mounted returnAmmo = null;
        Mounted mmlLrm = null;
        Mounted mmlSrm = null;

        for (Mounted ammo : ammoList) {
            AmmoType ammoType = (AmmoType)ammo.getType();
            if (AmmoType.M_CLUSTER == ammoType.getMunitionType()) {
                // MMLs have additional considerations.
                // There are no "cluster" missile munitions at this point in time.  Code is included in case
                // they are added to the game at some later date.
                if (!(weaponType instanceof MMLWeapon)) {
                    returnAmmo = ammo;
                    break;
                }
                if ((mmlLrm == null) && ammoType.hasFlag(AmmoType.F_MML_LRM)) {
                    mmlLrm = ammo;
                } else if (mmlSrm == null) {
                    mmlSrm = ammo;
                } else if (mmlLrm != null) {
                    break;
                }
            }
        }

        // MML ammo depends on range.
        if (weaponType instanceof MMLWeapon) {
            if (range > 9) { // Out of SRM range
                returnAmmo = mmlLrm;
            } else if (range > 6) { // SRM long range.
                returnAmmo = (mmlLrm == null ? mmlSrm : mmlLrm);
            } else {
                returnAmmo = (mmlSrm == null ? mmlLrm : mmlSrm);
            }
        }

        return returnAmmo;
    }

    protected Mounted getHardTargetAmmo(List<Mounted> ammoList, WeaponType weaponType, int range) {
        Mounted returnAmmo = null;
        Mounted mmlLrm = null;
        Mounted mmlSrm = null;

        for (Mounted ammo : ammoList) {
            AmmoType ammoType = (AmmoType)ammo.getType();
            if (AmmoType.M_CLUSTER == ammoType.getMunitionType()
                || AmmoType.M_ANTI_FLAME_FOAM == ammoType.getMunitionType()
                || AmmoType.M_CHAFF == ammoType.getMunitionType()
                || AmmoType.M_COOLANT == ammoType.getMunitionType()
                || AmmoType.M_ECM == ammoType.getMunitionType()
                || AmmoType.M_FASCAM == ammoType.getMunitionType()
                || AmmoType.M_FLAK == ammoType.getMunitionType()
                || AmmoType.M_FLARE == ammoType.getMunitionType()
                || AmmoType.M_FLECHETTE == ammoType.getMunitionType()
                || AmmoType.M_FRAGMENTATION == ammoType.getMunitionType()
                || AmmoType.M_HAYWIRE == ammoType.getMunitionType()
                || AmmoType.M_INCENDIARY == ammoType.getMunitionType()
                || AmmoType.M_INCENDIARY_AC == ammoType.getMunitionType()
                || AmmoType.M_INCENDIARY_LRM == ammoType.getMunitionType()
                || AmmoType.M_INFERNO == ammoType.getMunitionType()
                || AmmoType.M_INFERNO_IV == ammoType.getMunitionType()
                || AmmoType.M_LASER_INHIB == ammoType.getMunitionType()
                || AmmoType.M_OIL_SLICK == ammoType.getMunitionType()
                || AmmoType.M_NEMESIS == ammoType.getMunitionType()
                || AmmoType.M_PAINT_OBSCURANT == ammoType.getMunitionType()
                || AmmoType.M_SMOKE == ammoType.getMunitionType()
                || AmmoType.M_SMOKE_WARHEAD == ammoType.getMunitionType()
                || AmmoType.M_SMOKEGRENADE == ammoType.getMunitionType()
                || AmmoType.M_THUNDER == ammoType.getMunitionType()
                || AmmoType.M_THUNDER_ACTIVE == ammoType.getMunitionType()
                || AmmoType.M_THUNDER_AUGMENTED == ammoType.getMunitionType()
                || AmmoType.M_THUNDER_INFERNO == ammoType.getMunitionType()
                || AmmoType.M_THUNDER_VIBRABOMB == ammoType.getMunitionType()
                || AmmoType.M_TORPEDO == ammoType.getMunitionType()
                || AmmoType.M_VIBRABOMB_IV == ammoType.getMunitionType()
                || AmmoType.M_WATER == ammoType.getMunitionType()
                || AmmoType.M_ANTI_TSM == ammoType.getMunitionType()
                || AmmoType.M_CORROSIVE == ammoType.getMunitionType()) {
                continue;
            }
            // MMLs have additional considerations.
            if (!(weaponType instanceof MMLWeapon)) {
                returnAmmo = ammo;
                break;
            }
            if ((mmlLrm == null) && ammoType.hasFlag(AmmoType.F_MML_LRM)) {
                mmlLrm = ammo;
            } else if (mmlSrm == null) {
                mmlSrm = ammo;
            } else if ((mmlLrm != null) && (mmlSrm != null)) {
                break;
            }
        }

        // MML ammo depends on range.
        if (weaponType instanceof MMLWeapon) {
            if (range > 9) { // Out of SRM range
                returnAmmo = mmlLrm;
            } else if (range > 6) { // SRM long range.
                returnAmmo = (mmlLrm == null ? mmlSrm : mmlLrm);
            } else {
                returnAmmo = (mmlSrm == null ? mmlLrm : mmlSrm);
            }
        }

        return returnAmmo;
    }

    protected Mounted getAntiAirAmmo(List<Mounted> ammoList, WeaponType weaponType, int range) {
        Mounted returnAmmo = null;
        Mounted mmlLrm = null;
        Mounted mmlSrm = null;

        for (Mounted ammo : ammoList) {
            AmmoType ammoType = (AmmoType)ammo.getType();
            if (AmmoType.M_CLUSTER == ammoType.getMunitionType()
                    || AmmoType.M_FLAK == ammoType.getMunitionType()) {

                // MMLs have additional considerations.
                // There are no "flak" or "cluster" missile munitions at this point in time.  Code is included in case
                // they are added to the game at some later date.
                if (!(weaponType instanceof MMLWeapon)) {
                    returnAmmo = ammo;
                    break;
                }
                if ((mmlLrm == null) && ammoType.hasFlag(AmmoType.F_MML_LRM)) {
                    mmlLrm = ammo;
                } else if (mmlSrm == null) {
                    mmlSrm = ammo;
                } else if (mmlLrm != null) {
                    break;
                }
            }
        }

        // MML ammo depends on range.
        if (weaponType instanceof MMLWeapon) {
            if (range > 9) { // Out of SRM range
                returnAmmo = mmlLrm;
            } else if (range > 6) { // SRM long range.
                returnAmmo = (mmlLrm == null ? mmlSrm : mmlLrm);
            } else {
                returnAmmo = (mmlSrm == null ? mmlLrm : mmlSrm);
            }
        }

        return returnAmmo;
    }
}
