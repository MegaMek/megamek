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

import megamek.common.Aero;
import megamek.common.AmmoType;
import megamek.common.BattleArmor;
import megamek.common.BuildingTarget;
import megamek.common.Compute;
import megamek.common.Coords;
import megamek.common.Dropship;
import megamek.common.Entity;
import megamek.common.EntityMovementType;
import megamek.common.EntityWeightClass;
import megamek.common.EquipmentType;
import megamek.common.FixedWingSupport;
import megamek.common.GunEmplacement;
import megamek.common.IGame;
import megamek.common.IHex;
import megamek.common.Infantry;
import megamek.common.LargeSupportTank;
import megamek.common.LosEffects;
import megamek.common.Mech;
import megamek.common.MechWarrior;
import megamek.common.TargetRollModifier;
import megamek.common.Mounted;
import megamek.common.MovePath;
import megamek.common.MoveStep;
import megamek.common.annotations.Nullable;
import megamek.common.Protomech;
import megamek.common.RangeType;
import megamek.common.Tank;
import megamek.common.TargetRoll;
import megamek.common.Targetable;
import megamek.common.Terrains;
import megamek.common.ToHitData;
import megamek.common.VTOL;
import megamek.common.WeaponType;
import megamek.common.annotations.StaticWrapper;
import megamek.common.logging.LogLevel;
import megamek.common.options.OptionsConstants;
import megamek.common.util.StringUtil;
import megamek.common.weapons.ATMWeapon;
import megamek.common.weapons.MMLWeapon;
import megamek.common.weapons.StopSwarmAttack;
import megamek.common.weapons.infantry.InfantryWeapon;

/**
 * FireControl selects which weapons a unit wants to fire and at whom Pay attention to the difference between "guess"
 * and "get". Guess will be much faster, but inaccurate
 */
public class FireControl {

    protected static final String TH_WOODS = "woods";
    protected static final String TH_SMOKE = "smoke";
    protected static final String TH_PHY_BASE = "base";
    protected static final String TH_GUNNERY = "gunnery skill";
    protected static final String TH_SENSORS = "attacker sensors damaged";
    protected static final String TH_MINIMUM_RANGE = "Minimum Range";
    protected static final String TH_HEAT = "heat";
    protected static final String TH_WEAPON_MOD = "weapon to-hit";
    protected static final String TH_AMMO_MOD = "ammunition to-hit modifier";
    protected static final TargetRollModifier TH_ATT_PRONE = new TargetRollModifier(2, "attacker prone");
    protected static final TargetRollModifier TH_TAR_IMMOBILE = new TargetRollModifier(-4, "target immobile");
    protected static final TargetRollModifier TH_TAR_SKID = new TargetRollModifier(2, "target skidded");
    protected static final TargetRollModifier TH_TAR_NO_MOVE = new TargetRollModifier(1, "target didn't move");
    protected static final TargetRollModifier TH_TAR_SPRINT = new TargetRollModifier(-1, "target sprinted");
    protected static final TargetRollModifier TH_TAR_PRONE_RANGE = new TargetRollModifier(1,
                                                                                          "target prone and at range");
    protected static final TargetRollModifier TH_TAR_PRONE_ADJ = new TargetRollModifier(-2,
                                                                                        "target prone and adjacent");
    protected static final TargetRollModifier TH_TAR_BA = new TargetRollModifier(1, "battle armor target");
    protected static final TargetRollModifier TH_TAR_MW = new TargetRollModifier(2, "ejected mechwarrior target");
    protected static final TargetRollModifier TH_TAR_INF = new TargetRollModifier(1, "infantry target");
    protected static final TargetRollModifier TH_ANTI_AIR = new TargetRollModifier(-2, "anti-aircraft quirk");
    protected static final TargetRollModifier TH_INDUSTRIAL =
            new TargetRollModifier(1, "industrial cockpit without advanced fire control");
    protected static final TargetRollModifier TH_PRIMATIVE_INDUSTRIAL =
            new TargetRollModifier(2, "primitive industrial cockpit without advanced fire control");
    protected static final TargetRollModifier TH_TAR_SUPER = new TargetRollModifier(-1, "superheavy target");
    protected static final TargetRollModifier TH_TAR_GROUND_DS = new TargetRollModifier(-4, "grounded dropship target");
    protected static final TargetRollModifier TH_TAR_LOW_PROFILE = new TargetRollModifier(1,
                                                                                          "narrow/low profile target");
    protected static final TargetRollModifier TH_PHY_NOT_MECH =
            new TargetRollModifier(TargetRoll.IMPOSSIBLE, "non-mechs don't make physical attacks");
    protected static final TargetRollModifier TH_PHY_TOO_FAR = new TargetRollModifier(TargetRoll.IMPOSSIBLE,
                                                                                      "target not adjacent");
    protected static final TargetRollModifier TH_NULL_POSITION = new TargetRollModifier(TargetRoll.AUTOMATIC_FAIL,
                                                                                        "null position");
    protected static final TargetRollModifier TH_RNG_TOO_FAR = new TargetRollModifier(TargetRoll.IMPOSSIBLE,
                                                                                      "target beyond max range");
    protected static final TargetRollModifier TH_PHY_NOT_IN_ARC = new TargetRollModifier(TargetRoll.IMPOSSIBLE,
                                                                                         "target not in arc");
    protected static final TargetRollModifier TH_PHY_TOO_MUCH_ELEVATION =
            new TargetRollModifier(TargetRoll.IMPOSSIBLE, "target elevation not in range");
    protected static final TargetRollModifier TH_PHY_P_TAR_PRONE = new TargetRollModifier(TargetRoll.IMPOSSIBLE,
                                                                                          "can't punch while prone");
    protected static final TargetRollModifier TH_PHY_P_TAR_INF = new TargetRollModifier(TargetRoll.IMPOSSIBLE,
                                                                                        "can't punch infantry");
    protected static final TargetRollModifier TH_PHY_P_NO_ARM = new TargetRollModifier(TargetRoll.IMPOSSIBLE,
                                                                                       "Your arm's off!");
    protected static final TargetRollModifier TH_PHY_P_NO_SHOULDER = new TargetRollModifier(TargetRoll.IMPOSSIBLE,
                                                                                            "shoulder destroyed");
    protected static final TargetRollModifier TH_PHY_P_UPPER_ARM = new TargetRollModifier(2,
                                                                                          "upper arm actuator " +
                                                                                                  "destroyed");
    protected static final TargetRollModifier TH_PHY_P_LOWER_ARM = new TargetRollModifier(2,
                                                                                          "lower arm actuator missing" +
                                                                                                  " or destroyed");
    protected static final TargetRollModifier TH_PHY_P_HAND = new TargetRollModifier(1,
                                                                                     "hand actuator missing or " +
                                                                                             "destroyed");
    protected static final TargetRollModifier TH_PHY_K_PRONE = new TargetRollModifier(TargetRoll.IMPOSSIBLE,
                                                                                      "can't kick while prone");
    protected static final TargetRollModifier TH_PHY_K_INF = new TargetRollModifier(3, "kicking infantry");
    protected static final TargetRollModifier TH_PHY_K_INF_RNG = new TargetRollModifier(TargetRoll.IMPOSSIBLE,
                                                                                        "Infantry too far away");
    protected static final TargetRollModifier TH_PHY_K_HIP = new TargetRollModifier(TargetRoll.IMPOSSIBLE,
                                                                                    "can't kick with broken hip");
    protected static final TargetRollModifier TH_PHY_K_UPPER_LEG = new TargetRollModifier(2,
                                                                                          "upper leg actuator " +
                                                                                                  "destroyed");
    protected static final TargetRollModifier TH_PHY_K_LOWER_LEG = new TargetRollModifier(2,
                                                                                          "lower leg actuator " +
                                                                                                  "destroyed");
    protected static final TargetRollModifier TH_PHY_K_FOOT = new TargetRollModifier(1, "foot actuator destroyed");
    protected static final TargetRollModifier TH_PHY_LIGHT = new TargetRollModifier(-2, "weight class attack modifier");
    protected static final TargetRollModifier TH_PHY_MEDIUM = new TargetRollModifier(-1,
                                                                                     "weight class attack modifier");
    protected static final TargetRollModifier TH_PHY_LARGE = new TargetRollModifier(-2, "target large vehicle");
    protected static final TargetRollModifier TH_PHY_SUPER = new TargetRollModifier(1, "superheavy attacker");
    protected static final TargetRollModifier TH_PHY_EASY_PILOT = new TargetRollModifier(-1, "easy to pilot quirk");
    protected static final TargetRollModifier TH_PHY_P_NO_ARMS_QUIRK = new TargetRollModifier(TargetRoll.IMPOSSIBLE,
                                                                                              "no/minimal arms quirk");
    protected static final TargetRollModifier TH_WEAP_CANNOT_FIRE = new TargetRollModifier(TargetRoll.IMPOSSIBLE,
                                                                                           "weapon cannot fire");
    protected static final TargetRollModifier TH_WEAP_NO_AMMO = new TargetRollModifier(TargetRoll.IMPOSSIBLE,
                                                                                       "ammo is gone");
    protected static final TargetRollModifier TH_WEAP_PRONE_ARMLESS =
            new TargetRollModifier(TargetRoll.IMPOSSIBLE, "prone and missing an arm");
    protected static final TargetRollModifier TH_WEAP_ARM_PROP = new TargetRollModifier(TargetRoll.IMPOSSIBLE,
                                                                                        "using arm as prop");
    protected static final TargetRollModifier TH_WEAP_PRONE_LEG = new TargetRollModifier(TargetRoll.IMPOSSIBLE,
                                                                                         "prone leg weapon");
    protected static final TargetRollModifier TH_WEAPON_NO_ARC = new TargetRollModifier(TargetRoll.IMPOSSIBLE,
                                                                                        "not in arc");
    protected static final TargetRollModifier TH_INF_ZERO_RNG =
            new TargetRollModifier(TargetRoll.AUTOMATIC_FAIL, "noninfantry shooting with zero range");
    protected static final TargetRollModifier TH_STOP_SWARM_INVALID = new TargetRollModifier(TargetRoll.IMPOSSIBLE,
                                                                                             "not swarming a Mek");
    protected static final TargetRollModifier TH_SWARM_STOPPED = new TargetRollModifier(TargetRoll.AUTOMATIC_SUCCESS,
                                                                                        "stops swarming");
    protected static final TargetRollModifier TH_OUT_OF_RANGE = new TargetRollModifier(TargetRoll.IMPOSSIBLE,
                                                                                       "out of range");
    protected static final TargetRollModifier TH_SHORT_RANGE = new TargetRollModifier(0, "Short Range");
    protected static final TargetRollModifier TH_MEDIUM_RANGE = new TargetRollModifier(2, "Medium Range");
    protected static final TargetRollModifier TH_LONG_RANGE = new TargetRollModifier(4, "Long Range");
    protected static final TargetRollModifier TH_EXTREME_RANGE = new TargetRollModifier(6, "Extreme Range");
    protected static final TargetRollModifier TH_TARGETTING_COMP = new TargetRollModifier(-1, "targeting computer");
    protected static final TargetRollModifier TH_IMP_TARG_SHORT =
            new TargetRollModifier(-1, "improved targetting (short) quirk");
    protected static final TargetRollModifier TH_IMP_TARG_MEDIUM =
            new TargetRollModifier(-1, "improved targetting (medium) quirk");
    protected static final TargetRollModifier TH_IMP_TARG_LONG =
            new TargetRollModifier(-1, "improved targetting (long) quirk");
    protected static final TargetRollModifier TH_VAR_RNG_TARG_SHORT_AT_SHORT =
            new TargetRollModifier(-1, "variable range targetting (short) quirk");
    protected static final TargetRollModifier TH_VAR_RNG_TARG_SHORT_AT_LONG =
            new TargetRollModifier(1, "variable range targetting (short) quirk");
    protected static final TargetRollModifier TH_VAR_RNG_TARG_LONG_AT_LONG =
            new TargetRollModifier(-1, "variable range targetting (long) quirk");
    protected static final TargetRollModifier TH_VAR_RNG_TARG_LONG_AT_SHORT =
            new TargetRollModifier(1, "variable range targetting (long) quirk");
    protected static final TargetRollModifier TH_POOR_TARG_SHORT =
            new TargetRollModifier(1, "poor targetting (short) quirk");
    protected static final TargetRollModifier TH_POOR_TARG_MEDIUM =
            new TargetRollModifier(1, "poor targetting (medium) quirk");
    protected static final TargetRollModifier TH_POOR_TARG_LONG =
            new TargetRollModifier(1, "poor targetting (long) quirk");
    protected static final TargetRollModifier TH_ACCURATE_WEAP =
            new TargetRollModifier(-1, "accurate weapon quirk");
    protected static final TargetRollModifier TH_INACCURATE_WEAP =
            new TargetRollModifier(1, "inaccurate weapon quirk");
    protected static final TargetRollModifier TH_RNG_LARGE =
            new TargetRollModifier(-1, "target large vehicle or superheavy mech");
    protected static final TargetRollModifier TH_AIR_STRIKE_PATH =
            new TargetRollModifier(TargetRoll.IMPOSSIBLE, "target not under flight path");
    protected static final TargetRollModifier TH_AIR_STRIKE = new TargetRollModifier(2, "strike attack");

    private final Princess owner;

    /**
     * Constructor
     *
     * @param owningPrincess The {@link Princess} bot that utilizes this this class for computing firing solutions.
     */
    public FireControl(Princess owningPrincess) {
        owner = owningPrincess;
    }

    /**
     * Returns the movement modifier calculated by {@link Compute#getAttackerMovementModifier(IGame, int,
     * EntityMovementType)}.
     *
     * @param game            The {@link IGame} being played.
     * @param shooterId       The ID of the unit doing the shooting.
     * @param shooterMoveType The {@link EntityMovementType} of the unit doing the shooting.
     * @return The attacker movement modifier as a {@link ToHitData} object.
     */
    @StaticWrapper()
    protected ToHitData getAttackerMovementModifier(IGame game, int shooterId,
                                                    EntityMovementType shooterMoveType) {
        return Compute.getAttackerMovementModifier(game, shooterId, shooterMoveType);
    }

    /**
     * Returns the movement modifier calculated by {@link Compute#getTargetMovementModifier(int, boolean, boolean,
     * IGame)}
     *
     * @param hexesMoved The number of hexes the target unit moved.
     * @param jumping    Set TRUE if the target jumped.
     * @param vtol       Set TRUE if the target is a {@link VTOL}.
     * @param game       The {@link IGame} being played.
     * @return The target movement modifier as a {@link ToHitData} object.
     */
    @StaticWrapper()
    protected ToHitData getTargetMovementModifier(int hexesMoved, boolean jumping, boolean vtol, IGame game) {
        return Compute.getTargetMovementModifier(hexesMoved, jumping, vtol, game);
    }

    /**
     * Gets the toHit modifier common to both weapon and physical attacks
     *
     * @param shooter      The unit doing the shooting.
     * @param shooterState The state of the unit doing the shooting.
     * @param target       Who is being shot at.
     * @param targetState  The state of the target.
     * @param game         The game being played.
     * @return The estimated to hit modifiers.
     */
    public ToHitData guessToHitModifierHelperForAnyAttack(Entity shooter,
                                                          @Nullable EntityState shooterState,
                                                          Targetable target,
                                                          @Nullable EntityState targetState,
                                                          IGame game) {

        if (shooterState == null) {
            shooterState = new EntityState(shooter);
        }
        if (targetState == null) {
            targetState = new EntityState(target);
        }

        // Can't shoot if one of us is not on the board.
        // todo exception for off-board artillery.
        if ((shooterState.getPosition() == null) || (targetState.getPosition() == null)) {
            return new ToHitData(TH_NULL_POSITION);
        }

        // Is the target in range at all?
        int distance = shooterState.getPosition().distance(targetState.getPosition());
        int maxRange = shooter.getMaxWeaponRange();
        if (distance > maxRange) {
            return new ToHitData(TH_RNG_TOO_FAR);
        }

        ToHitData toHitData = new ToHitData();

        // If people are moving or lying down, there are consequences
        toHitData.append(getAttackerMovementModifier(game, shooter.getId(), shooterState.getMovementType()));
        toHitData.append(getTargetMovementModifier(targetState.getHexesMoved(), targetState.isJumping(),
                                                   target instanceof VTOL, game));
        if (shooterState.isProne()) {
            toHitData.addModifier(TH_ATT_PRONE);
        }
        if (targetState.isImmobile()) {
            toHitData.addModifier(TH_TAR_IMMOBILE);
        }
        if (game.getOptions().booleanOption(OptionsConstants.AGM_TAC_OPS_STANDING_STILL)
                && (targetState.getMovementType() == EntityMovementType.MOVE_NONE)
                && !targetState.isImmobile()
                && !((target instanceof Infantry) || (target instanceof VTOL) || (target instanceof
                GunEmplacement))) {
            toHitData.addModifier(TH_TAR_NO_MOVE);
        }

        // did the target sprint?
        if (targetState.getMovementType() == EntityMovementType.MOVE_SPRINT) {
            toHitData.addModifier(TH_TAR_SPRINT);
        }

        // terrain modifiers, since "compute" won't let me do these remotely
        IHex targetHex = game.getBoard().getHex(targetState.getPosition());
        int woodsLevel = targetHex.terrainLevel(Terrains.WOODS);
        if (targetHex.terrainLevel(Terrains.JUNGLE) > woodsLevel) {
            woodsLevel = targetHex.terrainLevel(Terrains.JUNGLE);
        }
        if (woodsLevel >= 1) {
            toHitData.addModifier(woodsLevel, TH_WOODS);
        }

        int smokeLevel = targetHex.terrainLevel(Terrains.SMOKE);
        if (smokeLevel >= 1) {
            toHitData.addModifier(smokeLevel, TH_SMOKE);
        }

        if (targetState.isProne() && (distance > 1)) {
            toHitData.addModifier(TH_TAR_PRONE_RANGE);
        } else if (targetState.isProne() && (distance == 1)) {
            toHitData.addModifier(TH_TAR_PRONE_ADJ);
        }

        if (targetState.getMovementType() == EntityMovementType.MOVE_SKID) {
            toHitData.addModifier(TH_TAR_SKID);
        }

        boolean isShooterInfantry = (shooter instanceof Infantry);
        if (!isShooterInfantry) {
            if (target instanceof BattleArmor) {
                toHitData.addModifier(TH_TAR_BA);
            } else if (target instanceof MechWarrior) {
                toHitData.addModifier(TH_TAR_MW);
            } else if (target instanceof Infantry) {
                toHitData.addModifier(TH_TAR_INF);
            }
        }

        if (shooter.hasQuirk(OptionsConstants.QUIRK_POS_ANTI_AIR) &&
                (target.isAirborne() || target.isAirborneVTOLorWIGE())) {
            toHitData.addModifier(TH_ANTI_AIR);
        }

        if (shooter instanceof Mech) {
            Mech shooterMech = (Mech) shooter;
            if (shooterMech.getCockpitType() == Mech.COCKPIT_INDUSTRIAL) {
                toHitData.addModifier(TH_INDUSTRIAL);
            } else if (shooterMech.getCockpitType() == Mech.COCKPIT_PRIMITIVE_INDUSTRIAL) {
                toHitData.addModifier(TH_PRIMATIVE_INDUSTRIAL);
            }
        }

        if (target instanceof Mech) {
            Mech targetMech = (Mech) target;
            if (targetMech.getCockpitType() == Mech.COCKPIT_SUPERHEAVY ||
                    targetMech.getCockpitType() == Mech.COCKPIT_SUPERHEAVY_TRIPOD) {
                toHitData.addModifier(TH_TAR_SUPER);
            }
        }

        if ((target instanceof Dropship) && !target.isAirborne()) {
            toHitData.addModifier(TH_TAR_GROUND_DS);
        }

        return toHitData;
    }

    /**
     * Makes a rather poor guess as to what the to hit modifier will be with a physical attack.
     *
     * @param shooter      The unit doing the attacking.
     * @param shooterState The state of the unit doing the attacking.
     * @param target       Who is being attacked.
     * @param targetState  The state of the target.
     * @param attackType   The tyep of physical attack being made.
     * @param game         The game being played.
     * @return The estimated to hit modifiers.
     */
    public ToHitData guessToHitModifierPhysical(Entity shooter,
                                                @Nullable EntityState shooterState,
                                                Targetable target,
                                                @Nullable EntityState targetState,
                                                PhysicalAttackType attackType,
                                                IGame game) {

        // todo weapons, frenzy (pg 144) & vehicle charges.
        // todo head mods to piloting?

        if (!(shooter instanceof Mech)) {
            return new ToHitData(TH_PHY_NOT_MECH);
        }

        if (shooterState == null) {
            shooterState = new EntityState(shooter);
        }
        if (targetState == null) {
            targetState = new EntityState(target);
        }

        // We can hit someone who isn't standing right next to us.
        int distance = shooterState.getPosition().distance(targetState.getPosition());
        if (distance > 1) {
            return new ToHitData(TH_PHY_TOO_FAR);
        }

        // Get the general to hit modifiers.
        ToHitData toHitData = new ToHitData();
        toHitData.append(guessToHitModifierHelperForAnyAttack(shooter, shooterState, target, targetState, game));
        if (toHitData.getValue() == TargetRoll.IMPOSSIBLE || toHitData.getValue() == TargetRoll.AUTOMATIC_FAIL) {
            return toHitData;
        }

        // Check if target is within arc
        int arc;
        if (PhysicalAttackType.LEFT_PUNCH == attackType) {
            arc = Compute.ARC_LEFTARM;
        } else if (PhysicalAttackType.RIGHT_PUNCH == attackType) {
            arc = Compute.ARC_RIGHTARM;
        } else {
            arc = Compute.ARC_FORWARD; // assume kick
        }
        if (!isInArc(shooterState.getPosition(), shooterState.getSecondaryFacing(), targetState.getPosition(), arc)) {
            return new ToHitData(TH_PHY_NOT_IN_ARC);
        }

        // Check elevation difference.
        IHex attackerHex = game.getBoard().getHex(shooterState.getPosition());
        IHex targetHex = game.getBoard().getHex(targetState.getPosition());
        final int attackerElevation = shooter.getElevation() + attackerHex.getElevation();
        final int attackerHeight = shooter.absHeight() + attackerHex.getElevation();
        final int targetElevation = target.getElevation() + targetHex.getElevation();
        final int targetHeight = targetElevation + target.getHeight();
        if (attackType.isPunch()) {
            if (shooter.hasQuirk(OptionsConstants.QUIRK_NEG_NO_ARMS)) {
                return new ToHitData(TH_PHY_P_NO_ARMS_QUIRK);
            }

            if ((attackerHeight < targetElevation) || (attackerHeight > targetHeight)) {
                return new ToHitData(TH_PHY_TOO_MUCH_ELEVATION);
            }

            if (shooterState.isProne()) {
                return new ToHitData(TH_PHY_P_TAR_PRONE);
            }
            if (target instanceof Infantry) {
                return new ToHitData(TH_PHY_P_TAR_INF);
            }
            int armLocation = attackType == PhysicalAttackType.RIGHT_PUNCH ? Mech.LOC_RARM : Mech.LOC_LARM;
            if (shooter.isLocationBad(armLocation)) {
                return new ToHitData(TH_PHY_P_NO_ARM);
            }
            if (!shooter.hasWorkingSystem(Mech.ACTUATOR_SHOULDER, armLocation)) {
                return new ToHitData(TH_PHY_P_NO_SHOULDER);
            }

            // Base to hit chance.
            toHitData.addModifier(shooter.getCrew().getPiloting(), TH_PHY_BASE);
            if (!shooter.hasWorkingSystem(Mech.ACTUATOR_UPPER_ARM, armLocation)) {
                toHitData.addModifier(TH_PHY_P_UPPER_ARM);
            }
            if (!shooter.hasWorkingSystem(Mech.ACTUATOR_LOWER_ARM, armLocation)) {
                toHitData.addModifier(TH_PHY_P_LOWER_ARM);
            }
            if (!shooter.hasWorkingSystem(Mech.ACTUATOR_HAND, armLocation)) {
                toHitData.addModifier(TH_PHY_P_HAND);
            }
        } else { // assuming kick

            if (shooterState.isProne()) {
                return new ToHitData(TH_PHY_K_PRONE);
            }
            if ((attackerElevation < targetElevation) || (attackerElevation > targetHeight)) {
                return new ToHitData(TH_PHY_TOO_MUCH_ELEVATION);
            }
            if ((shooter).hasHipCrit()) {
                return new ToHitData(TH_PHY_K_HIP);
            }
            int legLocation = attackType == PhysicalAttackType.RIGHT_KICK ? Mech.LOC_RLEG : Mech.LOC_LLEG;

            // Base to hit chance.
            toHitData.addModifier(shooter.getCrew().getPiloting() - 2, TH_PHY_BASE);
            if (!shooter.hasWorkingSystem(Mech.ACTUATOR_UPPER_LEG, legLocation)) {
                toHitData.addModifier(TH_PHY_K_UPPER_LEG);
            }
            if (!shooter.hasWorkingSystem(Mech.ACTUATOR_LOWER_LEG, legLocation)) {
                toHitData.addModifier(TH_PHY_K_LOWER_LEG);
            }
            if (!shooter.hasWorkingSystem(Mech.ACTUATOR_FOOT, legLocation)) {
                toHitData.addModifier(TH_PHY_K_FOOT);
            }
            if (target instanceof Infantry) {
                if (distance == 0) {
                    toHitData.addModifier(TH_PHY_K_INF);
                } else {
                    return new ToHitData(TH_PHY_K_INF_RNG);
                }
            }
        }

        if (game.getOptions().booleanOption(OptionsConstants.AGM_TAC_OPS_PHYSICAL_ATTACK_PSR)) {
            if (shooter.getWeightClass() == EntityWeightClass.WEIGHT_LIGHT) {
                toHitData.addModifier(TH_PHY_LIGHT);
            } else if (shooter.getWeightClass() == EntityWeightClass.WEIGHT_MEDIUM) {
                toHitData.addModifier(TH_PHY_MEDIUM);
            }
        }

        if ((target instanceof LargeSupportTank) || (target instanceof FixedWingSupport) ||
                (target instanceof Dropship && target.isAirborne())) {
            toHitData.addModifier(TH_PHY_LARGE);
        }
        if (shooter instanceof Mech) {
            Mech shooterMech = (Mech) shooter;
            if (shooterMech.getCockpitType() == Mech.COCKPIT_SUPERHEAVY ||
                    shooterMech.getCockpitType() == Mech.COCKPIT_SUPERHEAVY_TRIPOD) {
                toHitData.addModifier(TH_PHY_SUPER);
            }
        }

        if (shooter.hasQuirk(OptionsConstants.QUIRK_POS_EASY_PILOT) && (shooter.getCrew().getPiloting() > 3)) {
            toHitData.addModifier(TH_PHY_EASY_PILOT);
        }

        return toHitData;
    }

    /**
     * Returns the value of {@link Compute#isInArc(Coords, int, Targetable, int)}.
     *
     * @param shooterPosition The current {@link Coords} of the shooter.
     * @param shooterFacing   The shooter's current facing.
     * @param targetPosition  The current {@link Coords} of the target.
     * @param weaponArc       The arc of the weapon being fired.
     * @return TRUE if the target falls within the weapon's firing arc.
     */
    @StaticWrapper
    protected boolean isInArc(Coords shooterPosition, int shooterFacing, Coords targetPosition, int weaponArc) {
        return Compute.isInArc(shooterPosition, shooterFacing, targetPosition, weaponArc);
    }

    /**
     * Returns the value of {@link LosEffects#calculateLos(IGame, int, Targetable, Coords, Coords, boolean)}.
     *
     * @param game            The {@link IGame} being played.
     * @param shooterId       The id of the shooting unit.
     * @param target          The unit being shot at as a {@link Targetable} object.
     * @param shooterPosition The current {@link Coords} of the shooter.
     * @param targetPosition  The current {@link Coords} of the target.
     * @param spotting        Set TRUE if the shooter is simply spotting for indrect fire.
     * @return The resulting {@link LosEffects}.
     */
    @StaticWrapper
    protected LosEffects getLosEffects(IGame game, int shooterId, Targetable target, Coords shooterPosition,
                                       Coords targetPosition, boolean spotting) {
        return LosEffects.calculateLos(game, shooterId, target, shooterPosition, targetPosition, spotting);
    }

    /**
     * Returns the value of {@link Compute#getSwarmMekBaseToHit(Entity, Entity, IGame)}.
     *
     * @param attacker The attacking {@link Entity}.
     * @param defender The target of the attack.
     * @param game     The game being played.
     * @return The to hit modifiers as a {@link ToHitData} object.
     */
    @StaticWrapper
    protected ToHitData getSwarmMekBaseToHit(Entity attacker, Entity defender, IGame game) {
        return Compute.getSwarmMekBaseToHit(attacker, defender, game);
    }

    /**
     * Returns the value of {@link Compute#getLegAttackBaseToHit(Entity, Entity, IGame)}.
     *
     * @param attacker The attacking {@link Entity}.
     * @param defender The target of the attack.
     * @param game     The game being played.
     * @return The to hit modifiers as a {@link ToHitData} object.
     */
    @StaticWrapper
    protected ToHitData getLegAttackBaseToHit(Entity attacker, Entity defender, IGame game) {
        return Compute.getLegAttackBaseToHit(attacker, defender, game);
    }

    /**
     * Returns the value of {@link Compute#getInfantryRangeMods(int, InfantryWeapon)}.
     *
     * @param distance The distance to the target.
     * @param weapon   The {@link InfantryWeapon} being fired.
     * @return The to hit modifiers as a {@link ToHitData} object.
     */
    @StaticWrapper
    protected ToHitData getInfantryRangeMods(int distance, InfantryWeapon weapon) {
        return Compute.getInfantryRangeMods(distance, weapon);
    }

    /**
     * Returns the value of {@link Compute#getDamageWeaponMods(Entity, Mounted)}.
     *
     * @param attacker The attacking {@link Entity}.
     * @param weapon   The {@link Mounted} weapon being fired.
     * @return The to hit modifiers as a {@link ToHitData} object.
     */
    @StaticWrapper
    protected ToHitData getDamageWeaponMods(Entity attacker, Mounted weapon) {
        return Compute.getDamageWeaponMods(attacker, weapon);
    }

    private boolean isLargeTarget(Targetable target) {
        if ((target instanceof LargeSupportTank) || (target instanceof FixedWingSupport) ||
                (target instanceof Dropship && target.isAirborne())) {
            return true;
        }
        if (!(target instanceof Mech)) {
            return false;
        }

        Mech targetMech = (Mech) target;
        return (targetMech.getCockpitType() == Mech.COCKPIT_SUPERHEAVY) ||
                (targetMech.getCockpitType() == Mech.COCKPIT_SUPERHEAVY_TRIPOD);
    }

    /**
     * Makes an educated guess as to the to hit modifier with a weapon attack.  Does not actually place unit into
     * desired position, because that is exceptionally slow. Most of this is copied from WeaponAttack.
     *
     * @param shooter      The {@link Entity} doing the shooting.
     * @param shooterState The {@link EntityState} of the unit doing the shooting.
     * @param target       The {@link Targetable} being shot at.
     * @param targetState  The {@link EntityState} of the unit being shot at.
     * @param weapon       The weapon being fired as a {@link Mounted} object.
     * @param game         The {@link IGame being played.}
     * @return The to hit modifiers for the given weapon firing at the given target as a {@link ToHitData} object.
     */
    public ToHitData guessToHitModifierForWeapon(Entity shooter,
                                                 @Nullable EntityState shooterState,
                                                 Targetable target,
                                                 @Nullable EntityState targetState,
                                                 Mounted weapon,
                                                 IGame game) {

        if (shooterState == null) {
            shooterState = new EntityState(shooter);
        }
        if (targetState == null) {
            targetState = new EntityState(target);
        }

        // First check if the shot is impossible
        if (!weapon.canFire()) {
            return new ToHitData(TH_WEAP_CANNOT_FIRE);
        }

        // Make sure we have ammo.
        WeaponType weaponType = (WeaponType) weapon.getType();
        if (weaponType.getAmmoType() != AmmoType.T_NA) {
            if (weapon.getLinked() == null) {
                return new ToHitData(TH_WEAP_NO_AMMO);
            }
            if (weapon.getLinked().getUsableShotsLeft() == 0) {
                return new ToHitData(TH_WEAP_NO_AMMO);
            }
        }

        if (shooterState.isProne()) {
            // Cannot fire if we cannot at least prop ourselves up.
            if (shooter.isLocationBad(Mech.LOC_LARM) && shooter.isLocationBad(Mech.LOC_RARM)) {
                return new ToHitData(TH_WEAP_PRONE_ARMLESS);
            }
            // Cannot fire weapons mounted in the propping arm.
            if ((weapon.getLocation() == Mech.LOC_LARM || weapon.getLocation() == Mech.LOC_RARM)
                    && shooter.isLocationBad(weapon.getLocation())) {
                return new ToHitData(TH_WEAP_ARM_PROP);
            }
            // Cannot fire leg-mounted weapons while prone.)
            if ((weapon.getLocation() == Mech.LOC_LLEG) || (weapon.getLocation() == Mech.LOC_RLEG)) {
                return new ToHitData(TH_WEAP_PRONE_LEG);
            }
        }

        // Check if torso twists affect weapon
        int shooterFacing = shooterState.getFacing();
        if (shooter.isSecondaryArcWeapon(shooter.getEquipmentNum(weapon))) {
            shooterFacing = shooterState.getSecondaryFacing();
        }
        boolean inArc = isInArc(shooterState.getPosition(), shooterFacing, targetState.getPosition(),
                                shooter.getWeaponArc(shooter.getEquipmentNum(weapon)));
        if (!inArc) {
            return new ToHitData(TH_WEAPON_NO_ARC);
        }

        // Check range.
        int distance = shooterState.getPosition().distance(targetState.getPosition());
        if (target instanceof Aero) {
            distance += 2 * target.getAltitude(); // Aeros are +2 hexes further for each altitude.
        }
        int range = RangeType.rangeBracket(distance, weaponType.getRanges(weapon),
                                           game.getOptions().booleanOption(OptionsConstants.AC_TAC_OPS_RANGE));
        if (RangeType.RANGE_OUT == range) {
            return new ToHitData(TH_OUT_OF_RANGE);
        }

        // Cannot shoot at 0 range infantry unless shooter is also infantry.
        boolean isShooterInfantry = (shooter instanceof Infantry);
        if ((distance == 0) && (!isShooterInfantry) && !(weaponType instanceof StopSwarmAttack)) {
            return new ToHitData(TH_INF_ZERO_RNG);
        }

        // Handle stopping a swarm attack.
        if (weaponType instanceof StopSwarmAttack) {
            if (Entity.NONE == shooter.getSwarmTargetId()) {
                return new ToHitData(TH_STOP_SWARM_INVALID);
            } else {
                return new ToHitData(TH_SWARM_STOPPED);
            }
        }

        // Get the mods that apply to all attacks.
        ToHitData baseMods = guessToHitModifierHelperForAnyAttack(shooter, shooterState, target, targetState, game);
        if (baseMods.getValue() > TargetRoll.IMPOSSIBLE || baseMods.getValue() == TargetRoll.AUTOMATIC_FAIL) {
            return baseMods;
        }

        // Base to hit is gunnery skill
        ToHitData toHit = new ToHitData(shooter.getCrew().getGunnery(), TH_GUNNERY);
        toHit.append(baseMods);

        // There is kindly already a class that will calculate line of sight for me
        // todo take into account spotting for indirect fire.
        LosEffects losEffects = getLosEffects(game, shooter.getId(), target, shooterState.getPosition(),
                                              targetState.getPosition(), false);

        // water is a separate los effect
        IHex targetHex = game.getBoard().getHex(targetState.getPosition());
        Entity targetEntity = null;
        if (target instanceof Entity) {
            targetEntity = (Entity) target;
        }
        if (targetEntity != null) {
            if (targetHex.containsTerrain(Terrains.WATER)
                    && (targetHex.terrainLevel(Terrains.WATER) == 1)
                    && (targetEntity.height() > 0)) {
                losEffects.setTargetCover(losEffects.getTargetCover() | LosEffects.COVER_HORIZONTAL);
            }
        }

        // Can we still hit after taking into account LoS?
        toHit.append(losEffects.losModifiers(game));
        if ((toHit.getValue() == TargetRoll.IMPOSSIBLE) || (toHit.getValue() == TargetRoll.AUTOMATIC_FAIL)) {
            return toHit; // you can't hit what you can't see
        }

        // Handle sensor damage.  Mek sensor damage is handled under general damage mods.
        if (shooter instanceof Tank) {
            int sensors = ((Tank) shooter).getSensorHits();
            if (sensors > 0) {
                toHit.addModifier(sensors, TH_SENSORS);
            }
        }

        // Handle mechs being swarmed.
        if (targetEntity instanceof Mech) {
            if (Infantry.SWARM_MEK.equals(weaponType.getInternalName())) {
                toHit.append(getSwarmMekBaseToHit(shooter, targetEntity, game));
            }
            if (Infantry.LEG_ATTACK.equals(weapon.getType().getInternalName())) {
                toHit.append(getLegAttackBaseToHit(shooter, targetEntity, game));
            }
        }
        if ((toHit.getValue() == TargetRoll.IMPOSSIBLE) || (toHit.getValue() == TargetRoll.AUTOMATIC_FAIL)) {
            return toHit;
        }

        // Now deal with range effects
        if (!weaponType.hasFlag(WeaponType.F_INFANTRY)) {
            if (range == RangeType.RANGE_SHORT) {
                toHit.addModifier(TH_SHORT_RANGE);
            } else if (range == RangeType.RANGE_MEDIUM) {
                toHit.addModifier(TH_MEDIUM_RANGE);
            } else if (range == RangeType.RANGE_LONG) {
                toHit.addModifier(TH_LONG_RANGE);
            } else if (range == RangeType.RANGE_EXTREME) {
                toHit.addModifier(TH_EXTREME_RANGE);
            } else if ((range == RangeType.RANGE_MINIMUM) && !(target instanceof Aero)) {
                toHit.addModifier((weaponType.getMinimumRange() - distance) + 1, TH_MINIMUM_RANGE);
            }
        } else {
            toHit.append(getInfantryRangeMods(distance, (InfantryWeapon) weapon.getType()));
        }

        // let us not forget about heat
        if (shooter.getHeatFiringModifier() != 0) {
            toHit.addModifier(shooter.getHeatFiringModifier(), TH_HEAT);
        }

        // and damage
        toHit.append(getDamageWeaponMods(shooter, weapon));

        // weapon mods
        if (weaponType.getToHitModifier() != 0) {
            toHit.addModifier(weaponType.getToHitModifier(), TH_WEAPON_MOD);
        }

        // Target size.
        if (isLargeTarget(target)) {
            toHit.addModifier(TH_RNG_LARGE);
        }

        // ammo mods
        if (weaponType.getAmmoType() != AmmoType.T_NA) {
            AmmoType ammoType = (AmmoType) weapon.getLinked().getType();
            if ((ammoType != null) && (ammoType.getToHitModifier() != 0)) {
                toHit.addModifier(ammoType.getToHitModifier(), TH_AMMO_MOD);
            }
        }

        // targetting computer
        if (shooter.hasTargComp() && weaponType.hasFlag(WeaponType.F_DIRECT_FIRE)) {
            toHit.addModifier(TH_TARGETTING_COMP);
        }

        // target quirks
        if (targetEntity != null) {
            if (targetEntity.hasQuirk(OptionsConstants.QUIRK_POS_LOW_PROFILE)) {
                toHit.addModifier(TH_TAR_LOW_PROFILE);
            }
        }

        // shooter quirks
        if (RangeType.RANGE_SHORT == range) {
            if (shooter.hasQuirk(OptionsConstants.QUIRK_POS_IMP_TARG_S)) {
                toHit.addModifier(TH_IMP_TARG_SHORT);
            }
            if (shooter.hasQuirk(OptionsConstants.QUIRK_POS_VAR_RNG_TARG_S)) {
                toHit.addModifier(TH_VAR_RNG_TARG_SHORT_AT_SHORT);
            }
            if (shooter.hasQuirk(OptionsConstants.QUIRK_POS_VAR_RNG_TARG_L)) {
                toHit.addModifier(TH_VAR_RNG_TARG_LONG_AT_SHORT);
            }
            if (shooter.hasQuirk(OptionsConstants.QUIRK_NEG_POOR_TARG_S)) {
                toHit.addModifier(TH_POOR_TARG_SHORT);
            }
        }
        if (RangeType.RANGE_MEDIUM == range) {
            if (shooter.hasQuirk(OptionsConstants.QUIRK_POS_IMP_TARG_M)) {
                toHit.addModifier(TH_IMP_TARG_MEDIUM);
            }
            if (shooter.hasQuirk(OptionsConstants.QUIRK_NEG_POOR_TARG_M)) {
                toHit.addModifier(TH_POOR_TARG_MEDIUM);
            }
        }
        if (RangeType.RANGE_LONG == range) {
            if (shooter.hasQuirk(OptionsConstants.QUIRK_POS_IMP_TARG_L)) {
                toHit.addModifier(TH_IMP_TARG_LONG);
            }
            if (shooter.hasQuirk(OptionsConstants.QUIRK_POS_VAR_RNG_TARG_S)) {
                toHit.addModifier(TH_VAR_RNG_TARG_SHORT_AT_LONG);
            }
            if (shooter.hasQuirk(OptionsConstants.QUIRK_POS_VAR_RNG_TARG_L)) {
                toHit.addModifier(TH_VAR_RNG_TARG_LONG_AT_LONG);
            }
            if (shooter.hasQuirk(OptionsConstants.QUIRK_NEG_POOR_TARG_L)) {
                toHit.addModifier(TH_POOR_TARG_LONG);
            }
        }

        // weapon quirks
        if (weapon.hasQuirk(OptionsConstants.QUIRK_WEAP_POS_ACCURATE)) {
            toHit.addModifier(TH_ACCURATE_WEAP);
        }
        if (weapon.hasQuirk(OptionsConstants.QUIRK_WEAP_NEG_INACCURATE)) {
            toHit.addModifier(TH_INACCURATE_WEAP);
        }

        return toHit;
    }

    /**
     * Makes an educated guess as to the to hit modifier by an aerospace unit flying on a ground map doing a strike
     * attack on a unit
     *
     * @param shooter               The {@link megamek.common.Entity} doing the shooting.
     * @param shooterState          The {@link EntityState} of the unit doing the shooting.
     * @param target                The {@link megamek.common.Targetable} being shot at.
     * @param targetState           The {@link megamek.client.bot.princess.EntityState} of the unit being shot at.
     * @param flightPath            The path the shooter is taking.
     * @param weapon                The weapon being fired as a {@link megamek.common.Mounted} object.
     * @param game                  The {@link megamek.common.IGame being played.}
     * @param assumeUnderFlightPlan Set TRUE to assume that the target falls under the given flight path.
     * @return The to hit modifiers for the given weapon firing at the given target as a {@link ToHitData} object.
     */
    public ToHitData guessAirToGroundStrikeToHitModifier(Entity shooter, @Nullable EntityState shooterState,
                                                         Targetable target, @Nullable EntityState targetState,
                                                         MovePath flightPath, Mounted weapon, IGame game,
                                                         boolean assumeUnderFlightPlan) {

        if (targetState == null) {
            targetState = new EntityState(target);
        }
        if (shooterState == null) {
            shooterState = new EntityState(shooter);
        }

        // first check if the shot is impossible
        if (!weapon.canFire()) {
            return new ToHitData(TH_WEAP_CANNOT_FIRE);
        }

        // Is the weapon loaded?
        if (((WeaponType) weapon.getType()).ammoType != AmmoType.T_NA) {
            if (weapon.getLinked() == null) {
                return new ToHitData(TH_WEAP_NO_AMMO);
            }
            if (weapon.getLinked().getUsableShotsLeft() == 0) {
                return new ToHitData(TH_WEAP_NO_AMMO);
            }
        }

        // check if target is even under our path
        if (!assumeUnderFlightPlan && !isTargetUnderFlightPath(flightPath, targetState)) {
            return new ToHitData(TH_AIR_STRIKE_PATH);
        }

        // Base to hit is gunnery skill
        ToHitData tohit = new ToHitData(shooter.getCrew().getGunnery(), TH_GUNNERY);

        // Get general modifiers.
        tohit.append(guessToHitModifierHelperForAnyAttack(shooter, shooterState, target, targetState, game));

        // Additional penalty due to strike attack
        tohit.addModifier(TH_AIR_STRIKE);

        return tohit;
    }

    /**
     * Checks if a target lies under a move path, to see if an aero unit can attack it.
     *
     * @param flightPath  move path to check
     * @param targetState used for targets position
     * @return TRUE if the target is under the path.
     */
    public boolean isTargetUnderFlightPath(MovePath flightPath,
                                           EntityState targetState) {

        Coords targetCoords = targetState.getPosition();
        for (Enumeration<MoveStep> step = flightPath.getSteps(); step.hasMoreElements(); ) {
            Coords stepCoords = step.nextElement().getPosition();
            if (targetCoords.equals(stepCoords)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Mostly for debugging, this returns a non-null string that describes how the guess has failed to be perfectly
     * accurate. or null if perfectly accurate
     *
     * @param shooter The unit doing the shooting.
     * @param target  The unit being shot at.
     * @param weapon  The weapon being fired.
     * @param game    The game being played.
     * @return A description of the differences or NULL if there are none.
     */
    String checkGuess(Entity shooter, Targetable target, Mounted weapon, IGame game) {

        // This really should only be done for debugging purposes.  Regular play should avoid the overhead.
        if (!LogLevel.DEBUG.equals(owner.getVerbosity())) {
            return null;
        }

        // Don't bother checking these as the guesses are minimal (or non-existant).
        if ((shooter instanceof Aero) || (shooter.getPosition() == null) || (target.getPosition() == null)) {
            return null;
        }

        String ret = null;
        WeaponFireInfo guessInfo = new WeaponFireInfo(shooter, new EntityState(shooter), target, null, weapon, game,
                                                      true, owner);
        WeaponFireInfo accurateInfo = new WeaponFireInfo(shooter, target, weapon, game, false, owner);

        if (guessInfo.getToHit().getValue() != accurateInfo.getToHit().getValue()) {
            ret += "Incorrect To Hit prediction, weapon " + weapon.getName() + " (" + shooter.getChassis() + " vs " +
                    target.getDisplayName() + ")" + ":\n";
            ret += " Guess: " + Integer.toString(guessInfo.getToHit().getValue()) + " " +
                    guessInfo.getToHit().getDesc() + "\n";
            ret += " Real:  " + Integer.toString(accurateInfo.getToHit().getValue()) + " " +
                    accurateInfo.getToHit().getDesc() + "\n";
        }
        return ret;
    }

    /**
     * Mostly for debugging, this returns a non-null string that describes how the guess on a physical attack failed
     * to be perfectly accurate, or null if accurate
     *
     * @param shooter    The unit doing the shooting.
     * @param target     The unit being shot at.
     * @param attackType The attack being made.
     * @param game       The game being played.
     * @return A description of the differences or NULL if there are none.
     */
    String checkGuessPhysical(Entity shooter, Targetable target, PhysicalAttackType attackType, IGame game) {

        // This really should only be done for debugging purposes.  Regular play should avoid the overhead.
        if (!LogLevel.DEBUG.equals(owner.getVerbosity())) {
            return null;
        }

        // only mechs can do physicals
        if (!(shooter instanceof Mech)) {
            return null;
        }

        String ret = null;
        if (shooter.getPosition() == null) {
            return "Shooter has NULL coordinates!";
        } else if (target.getPosition() == null) {
            return "Target has NULL coordinates!";
        }

        PhysicalInfo guessInfo = new PhysicalInfo(shooter, null, target, null, attackType, game, owner);
        PhysicalInfo accurateInfo = new PhysicalInfo(shooter, target, attackType, game, owner);
        if (guessInfo.to_hit.getValue() != accurateInfo.to_hit.getValue()) {
            ret += "Incorrect To Hit prediction, physical attack " + attackType.name() + ":\n";
            ret += " Guess: " + Integer.toString(guessInfo.to_hit.getValue()) + " " + guessInfo.to_hit.getDesc() +
                    "\n";
            ret += " Real:  " + Integer.toString(accurateInfo.to_hit.getValue()) + " " +
                    accurateInfo.to_hit.getDesc() + "\n";
        }
        return ret;
    }

    /**
     * Mostly for debugging, this returns a non-null string that describes how  any possible guess has failed to be
     * perfectly accurate. or null if  perfect
     *
     * @param shooter The unit doing the shooting.
     * @param game    The game being played.
     * @return A description of the differences or NULL if there are none.
     */
    String checkAllGuesses(Entity shooter, IGame game) {

        // This really should only be done for debugging purposes.  Regular play should avoid the overhead.
        if (!LogLevel.DEBUG.equals(owner.getVerbosity())) {
            return null;
        }

        String ret = "";
        ArrayList<Targetable> enemies = getTargetableEnemyEntities(shooter, game);
        for (Targetable enemy : enemies) {
            for (Mounted weapon : shooter.getWeaponList()) {
                String shootingCheck = checkGuess(shooter, enemy, weapon, game);
                if (shootingCheck != null) {
                    ret += shootingCheck;
                }
            }
            String physicalCheck = null;
            physicalCheck = checkGuessPhysical(shooter, enemy, PhysicalAttackType.RIGHT_KICK, game);
            if (physicalCheck != null) {
                ret += physicalCheck;
            }
            physicalCheck = checkGuessPhysical(shooter, enemy, PhysicalAttackType.LEFT_KICK, game);
            if (physicalCheck != null) {
                ret += physicalCheck;
            }
            physicalCheck = checkGuessPhysical(shooter, enemy, PhysicalAttackType.RIGHT_PUNCH, game);
            if (physicalCheck != null) {
                ret += physicalCheck;
            }
            physicalCheck = checkGuessPhysical(shooter, enemy, PhysicalAttackType.LEFT_PUNCH, game);
            if (physicalCheck != null) {
                ret += physicalCheck;
            }

        }
        if (StringUtil.isNullOrEmpty(ret)) {
            return null;
        }
        return ret;
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
        double ejected_pilot_disutility = (p.getTarget() instanceof MechWarrior ? 1000.0 : 0.0);
        int overheat = 0;
        if (p.getHeat() > overheat_value) {
            overheat = p.getHeat() - overheat_value;
        }
        p.setUtility(((damage_utility * p.getExpectedDamage())
                + (critical_utility * p.getExpectedCriticals()) + (kill_utility * p
                .getKillProbability())) - (overheat_disutility * overheat) - ejected_pilot_disutility);
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
        FiringPlan myplan = new FiringPlan(owner, target);
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
                                                      target, target_state, mw, game, true, owner);
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
            if (!isTargetUnderFlightPath(shooter_path, target_state)) {
                return new FiringPlan(owner, target);
            }
        }
        FiringPlan myplan = new FiringPlan(owner, target);
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
                                                      target, target_state, mw, game, true, true, owner);
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
        FiringPlan myplan = new FiringPlan(owner, target);
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
            WeaponFireInfo shoot = new WeaponFireInfo(shooter, target, mw, game, false, owner);
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
    FiringPlan[] calcFiringPlansUnderHeat(FiringPlan maxplan, int maxheat, Targetable target,
                                          IGame game) {
        if (maxheat < 0) {
            maxheat = 0; // can't be worse than zero heat
        }
        FiringPlan[] best_plans = new FiringPlan[maxheat + 1];
        best_plans[0] = new FiringPlan(owner, target);
        FiringPlan nonzeroheat_options = new FiringPlan(owner, target);
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
            best_plans[i] = new FiringPlan(owner, target);
            best_plans[i].addAll(best_plans[i - 1]);
            for (WeaponFireInfo f : nonzeroheat_options) {
                if ((i - f.getHeat()) >= 0) {
                    if (!best_plans[i - f.getHeat()].containsWeapon(f.getWeapon())) {
                        FiringPlan testplan = new FiringPlan(owner, target);
                        testplan.addAll(best_plans[i - f.getHeat()]);
                        testplan.add(f);
                        calculateUtility(testplan, 999); // TODO fix overheat
                        if (testplan.getUtility() > best_plans[i].getUtility()) {
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
        FiringPlan heatplans[] = calcFiringPlansUnderHeat(fullplan, maxheat, target, game);
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
        FiringPlan heatplans[] = calcFiringPlansUnderHeat(fullplan, fullplan.getHeat(), target, game);
        FiringPlan best_plan = new FiringPlan(owner, target);
        int overheat = (shooter.getHeatCapacity() - shooter.heat) + 4;
        for (int i = 0; i < (fullplan.getHeat() + 1); i++) {
            calculateUtility(heatplans[i], overheat);
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
        FiringPlan heatplans[] = calcFiringPlansUnderHeat(fullplan, maxheat, target, game);
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
        FiringPlan heatplans[] = calcFiringPlansUnderHeat(fullplan, fullplan.getHeat(), target, game);
        FiringPlan best_plan = new FiringPlan(owner, target);
        int overheat = (shooter.getHeatCapacity() - shooter_state.getHeat()) + 4;
        for (int i = 0; i < fullplan.getHeat(); i++) {
            calculateUtility(heatplans[i], overheat);
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
        FiringPlan bestplan = null;
        ArrayList<Targetable> enemies = getTargetableEnemyEntities(shooter,
                                                                   game);
        for (Targetable e : enemies) {
            FiringPlan plan = getBestFiringPlanWithTwists(shooter, e, game);
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
    public void loadAmmo(Entity shooter, Targetable target) {
        if (shooter == null) {
            return;
        }

        // Loading ammo for all my weapons.
        Iterator<Mounted> weapons = shooter.getWeapons();
        while (weapons.hasNext()) {
            Mounted currentWeapon = weapons.next();
            WeaponType weaponType = (WeaponType) currentWeapon.getType();

            // Skip weapons that don't use ammo.
            if (AmmoType.T_NA == weaponType.getAmmoType()) {
                continue;
            }

            Mounted mountedAmmo = getPreferredAmmo(shooter, target, weaponType);
            // Log failures.
            if ((mountedAmmo != null) && !shooter.loadWeapon(currentWeapon, mountedAmmo)) {
                owner.log(getClass(), "loadAmmo(Entity, Targetable)", LogLevel.WARNING,
                          shooter.getDisplayName() + " tried to load " + currentWeapon.getName() + " with ammo " +
                                  mountedAmmo.getDesc() + " but failed somehow.");
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

    protected Mounted getClusterAmmo(List<Mounted> ammoList, WeaponType weaponType, int range) {
        Mounted returnAmmo = null;
        Mounted mmlLrm = null;
        Mounted mmlSrm = null;

        for (Mounted ammo : ammoList) {
            AmmoType ammoType = (AmmoType) ammo.getType();
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

    protected Mounted getPreferredAmmo(Entity shooter, Targetable target, WeaponType weaponType) {
        final String METHOD_NAME = "getPreferredAmmo(Entity, Targetable, WeaponType)";

        StringBuilder msg = new StringBuilder("Getting ammo for ").append(weaponType.getShortName()).append(" firing " +
                                                                                                                    "at ").append(target.getDisplayName());
        Entity targetEntity = null;
        Mounted preferredAmmo = null;

        try {
            boolean fireResistant = false;
            if (target instanceof Entity) {
                targetEntity = (Entity) target;
                int armorType = targetEntity.getArmorType(0);
                if (targetEntity instanceof Mech) {
                    targetEntity.getArmorType(1);
                }
                if (EquipmentType.T_ARMOR_BA_FIRE_RESIST == armorType
                        || EquipmentType.T_ARMOR_HEAT_DISSIPATING == armorType) {
                    fireResistant = true;
                }
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
                return getAtmAmmo(validAmmo, range, new EntityState(target), fireResistant);
            }

            // Target is a building.
            if (target instanceof BuildingTarget) {
                msg.append("\n\tTarget is a building... ");
                preferredAmmo = getIncendiaryAmmo(validAmmo, weaponType, range);
                if (preferredAmmo != null) {
                    msg.append("Burn It Down!");
                    return preferredAmmo;
                }

                // Entity targets.
            } else if (targetEntity != null) {
                // Airborne targets
                if (targetEntity.isAirborne() || (targetEntity instanceof VTOL)) {
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
                    preferredAmmo = getAntiVeeAmmo(validAmmo, weaponType, range, fireResistant);
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
                if (targetEntity.getHeat() >= 9 && !fireResistant) {
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
            owner.log(getClass(), METHOD_NAME, LogLevel.DEBUG, msg.toString());
        }
    }

    protected Mounted getGeneralMmlAmmo(List<Mounted> ammoList, int range) {
        Mounted returnAmmo = null;

        // Get the LRM and SRM bins if we have them.
        Mounted mmlSrm = null;
        Mounted mmlLrm = null;
        for (Mounted ammo : ammoList) {
            AmmoType type = (AmmoType) ammo.getType();
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

            // If we only have LRMs left.
        } else if (mmlSrm == null) {
            returnAmmo = mmlLrm;

            // Left with SRMS.
        } else {
            returnAmmo = mmlSrm;
        }
        return returnAmmo;
    }

    protected Mounted getAtmAmmo(List<Mounted> ammoList, int range, EntityState target, boolean fireResistant) {
        Mounted returnAmmo = null;

        // Get the Hi-Ex, Ex-Range and Standard ammo bins if we have them.
        Mounted heAmmo = null;
        Mounted erAmmo = null;
        Mounted stAmmo = null;
        Mounted infernoAmmo = null;
        for (Mounted ammo : ammoList) {
            AmmoType type = (AmmoType) ammo.getType();
            if ((heAmmo == null) && (AmmoType.M_HIGH_EXPLOSIVE == type.getMunitionType())) {
                heAmmo = ammo;
            } else if ((erAmmo == null) && (AmmoType.M_EXTENDED_RANGE == type.getMunitionType())) {
                erAmmo = ammo;
            } else if ((stAmmo == null) && (AmmoType.M_STANDARD == type.getMunitionType())) {
                stAmmo = ammo;
            } else if ((infernoAmmo == null) && (AmmoType.M_IATM_IIW == type.getMunitionType())) {
                infernoAmmo = ammo;
            } else if ((heAmmo != null) && (erAmmo != null) && (stAmmo != null) && (infernoAmmo != null)) {
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

        if ((returnAmmo == stAmmo) && (infernoAmmo != null)
                && ((target.getHeat() >= 9) || target.isBuilding())
                && !fireResistant) {
            returnAmmo = infernoAmmo;
        }

        return returnAmmo;
    }

    protected Mounted getAntiVeeAmmo(List<Mounted> ammoList, WeaponType weaponType, int range, boolean fireResistant) {
        Mounted returnAmmo = null;
        Mounted mmlLrm = null;
        Mounted mmlSrm = null;

        for (Mounted ammo : ammoList) {
            AmmoType ammoType = (AmmoType) ammo.getType();
            if (AmmoType.M_CLUSTER == ammoType.getMunitionType()
                    || (AmmoType.M_INFERNO == ammoType.getMunitionType() && !fireResistant)
                    || (AmmoType.M_INFERNO_IV == ammoType.getMunitionType() && !fireResistant)) {

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
            AmmoType ammoType = (AmmoType) ammo.getType();
            if (AmmoType.M_FLECHETTE == ammoType.getMunitionType()
                    || AmmoType.M_FRAGMENTATION == ammoType.getMunitionType()
                    || AmmoType.M_CLUSTER == ammoType.getMunitionType()
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
            AmmoType ammoType = (AmmoType) ammo.getType();
            if (AmmoType.M_INFERNO == ammoType.getMunitionType()
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

    protected Mounted getIncendiaryAmmo(List<Mounted> ammoList, WeaponType weaponType, int range) {
        Mounted returnAmmo = null;
        Mounted mmlLrm = null;
        Mounted mmlSrm = null;

        for (Mounted ammo : ammoList) {
            AmmoType ammoType = (AmmoType) ammo.getType();
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

    protected Mounted getHardTargetAmmo(List<Mounted> ammoList, WeaponType weaponType, int range) {
        Mounted returnAmmo = null;
        Mounted mmlLrm = null;
        Mounted mmlSrm = null;

        for (Mounted ammo : ammoList) {
            AmmoType ammoType = (AmmoType) ammo.getType();
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
            AmmoType ammoType = (AmmoType) ammo.getType();
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
