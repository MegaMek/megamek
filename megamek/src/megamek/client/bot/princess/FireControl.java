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
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

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
import megamek.common.IPlayer;
import megamek.common.IHex;
import megamek.common.Infantry;
import megamek.common.LargeSupportTank;
import megamek.common.LosEffects;
import megamek.common.Mech;
import megamek.common.MechWarrior;
import megamek.common.Mounted;
import megamek.common.MovePath;
import megamek.common.MoveStep;
import megamek.common.Protomech;
import megamek.common.RangeType;
import megamek.common.Tank;
import megamek.common.TargetRoll;
import megamek.common.TargetRollModifier;
import megamek.common.Targetable;
import megamek.common.Terrains;
import megamek.common.ToHitData;
import megamek.common.VTOL;
import megamek.common.WeaponType;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.annotations.Nullable;
import megamek.common.annotations.StaticWrapper;
import megamek.common.logging.LogLevel;
import megamek.common.options.OptionsConstants;
import megamek.common.util.StringUtil;
import megamek.common.weapons.ATMWeapon;
import megamek.common.weapons.MMLWeapon;
import megamek.common.weapons.StopSwarmAttack;
import megamek.common.weapons.infantry.InfantryWeapon;

/**
 * FireControl selects which weapons a unit wants to fire and at whom Pay
 * attention to the difference between "guess" and "get". Guess will be much
 * faster, but inaccurate
 */
public class FireControl {

    private static double DAMAGE_UTILITY = 1.0;
    private static double CRITICAL_UTILITY = 10.0;
    private static double KILL_UTILITY = 50.0;
    private static double OVERHEAT_DISUTILITY = 5.0;
    private static double OVERHEAT_DISUTILITY_AERO = 50.0; // Aeros *really* don't want to overheat.
    private static double EJECTED_PILOT_DISUTILITY = 1000.0;
    private static double CIVILIAN_TARGET_DISUTILITY = 250.0;
    private static double TARGET_HP_FRACTION_DEALT_UTILITY = -30.0;

    protected static double TARGET_POTENTIAL_DAMAGE_UTILITY = 1.0;
    protected static double COMMANDER_UTILITY = 0.5;
    protected static double SUB_COMMANDER_UTILITY = 0.25;
    protected static double STRATEGIC_TARGET_UTILITY = 0.5;
    protected static double PRIORITY_TARGET_UTILITY = 0.25;

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
    protected static final TargetRollModifier TH_TAR_AERO_NOE_ADJ = new TargetRollModifier(1,
                                                                                           "NOE aero adjacent flight " +
                                                                                           "path");
    protected static final TargetRollModifier TH_TAR_AERO_NOE = new TargetRollModifier(3,
                                                                                       "NOE aero non-adjacent flight " +
                                                                                       "path");
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
    protected static final TargetRollModifier TH_LOS_RANGE = new TargetRollModifier(8, "LOS Range");
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
     * Returns the {@link Coords} computed by {@link Compute#getClosestFlightPath(Coords, Entity)}.
     *
     * @param shooterPosition The shooter's position.
     * @param targetAero      The aero unit being attacked.
     * @return The {@link Coords} from the target's flight path closest to the shooter.
     */
    @StaticWrapper
    protected Coords getNearestPointInFlightPath(Coords shooterPosition, Aero targetAero) {
        return Compute.getClosestFlightPath(-1, shooterPosition, targetAero);
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
     * @param distance
     * @param game         The game being played.  @return The estimated to hit modifiers.
     */
    protected ToHitData guessToHitModifierHelperForAnyAttack(Entity shooter,
                                                             @Nullable EntityState shooterState,
                                                             Targetable target,
                                                             @Nullable EntityState targetState,
                                                             int distance, IGame game) {

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
        int maxRange = shooter.getMaxWeaponRange();
        if (distance > maxRange) {
            return new ToHitData(TH_RNG_TOO_FAR);
        }

        ToHitData toHitData = new ToHitData();

        // If people are moving or lying down, there are consequences
        toHitData.append(getAttackerMovementModifier(game, shooter.getId(), shooterState.getMovementType()));

        // Ground units attacking airborne aeros.
        if (!shooterState.isAero() && targetState.isAirborneAero()) {
            Aero targetAero = (Aero) target;
            if (targetAero.isNOE()) {
                Coords closestInFlightPath = getNearestPointInFlightPath(shooterState.getPosition(), targetAero);
                int aeroDistance = closestInFlightPath.distance(shooterState.getPosition());
                if (aeroDistance <= 1) {
                    toHitData.addModifier(TH_TAR_AERO_NOE_ADJ);
                } else {
                    toHitData.addModifier(TH_TAR_AERO_NOE);
                }
            }
        } else {
            toHitData.append(getTargetMovementModifier(targetState.getHexesMoved(), targetState.isJumping(),
                                                       target instanceof VTOL, game));
        }
        if (shooterState.isProne()) {
            toHitData.addModifier(TH_ATT_PRONE);
        }
        if (targetState.isImmobile()) {
            toHitData.addModifier(TH_TAR_IMMOBILE);
        }
        if (game.getOptions().booleanOption(OptionsConstants.ADVGRNDMOV_TACOPS_STANDING_STILL)
            && (targetState.getMovementType() == EntityMovementType.MOVE_NONE)
            && !targetState.isImmobile()
            && !((target instanceof Infantry) || (target instanceof VTOL) ||
                 (target instanceof GunEmplacement))) {
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
            // Smoke level doesn't necessary correspond to the to-hit modifier
            // even levels are light smoke, odd are heavy smoke
            toHitData.addModifier((smokeLevel % 2) + 1, TH_SMOKE);
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
        // todo heat mods to piloting?

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
        toHitData.append(guessToHitModifierHelperForAnyAttack(shooter, shooterState, target, targetState, distance,
                                                              game));
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
        final int attackerElevation = shooter.getElevation() + attackerHex.getLevel();
        final int attackerHeight = shooter.relHeight() + attackerHex.getLevel();
        final int targetElevation = target.getElevation() + targetHex.getLevel();
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

        if (game.getOptions().booleanOption(OptionsConstants.ADVGRNDMOV_TACOPS_PHYSICAL_ATTACK_PSR)) {
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
        Mech shooterMech = (Mech) shooter;
        if (shooterMech.getCockpitType() == Mech.COCKPIT_SUPERHEAVY ||
            shooterMech.getCockpitType() == Mech.COCKPIT_SUPERHEAVY_TRIPOD) {
            toHitData.addModifier(TH_PHY_SUPER);
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
     * Makes an educated guess as to the to hit modifier with a weapon attack.
     * Does not actually place unit into desired position, because that is
     * exceptionally slow. Most of this is copied from WeaponAttack.
     *
     * @param shooter
     *            The {@link Entity} doing the shooting.
     * @param shooterState
     *            The {@link EntityState} of the unit doing the shooting.
     * @param target
     *            The {@link Targetable} being shot at.
     * @param targetState
     *            The {@link EntityState} of the unit being shot at.
     * @param weapon
     *            The weapon being fired as a {@link Mounted} object.
     * @param game
     *            The {@link IGame being played.}
     * @return The to hit modifiers for the given weapon firing at the given
     *         target as a {@link ToHitData} object.
     */
    public ToHitData guessToHitModifierForWeapon(Entity shooter,
            @Nullable EntityState shooterState, Targetable target,
            @Nullable EntityState targetState, Mounted weapon, IGame game) {

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

        // Ground units attacking airborne aero considerations.
        if (targetState.isAirborneAero() && !shooterState.isAero()) {

            // If the aero is attacking me, there is no range.
            if (target.getTargetId() == shooter.getId()) {
                distance = 0;
            } else {
                // Take into account altitude.
                distance += 2 * target.getAltitude();
            }
        }
        int range = RangeType.rangeBracket(distance, weaponType.getRanges(weapon),
                                           game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_RANGE),
                                           game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_LOS_RANGE));
        if (RangeType.RANGE_OUT == range) {
            return new ToHitData(TH_OUT_OF_RANGE);
        } else if ((range == RangeType.RANGE_MINIMUM) && targetState.isAirborneAero()) {
            range = RangeType.RANGE_SHORT;
        }

        // Cannot shoot at 0 range infantry unless shooter is also infantry.
        boolean isShooterInfantry = (shooter instanceof Infantry);
        if ((distance == 0) && (!isShooterInfantry) && !(weaponType instanceof StopSwarmAttack) &&
            !targetState.isAirborneAero()) {
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
        ToHitData baseMods = guessToHitModifierHelperForAnyAttack(shooter,
                shooterState, target, targetState, distance, game);
        if (baseMods.getValue() == TargetRoll.IMPOSSIBLE || baseMods.getValue() == TargetRoll.AUTOMATIC_FAIL) {
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
            } else if (range == RangeType.RANGE_MINIMUM) {
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
        if (weaponType.getAmmoType() != AmmoType.T_NA
                && (weapon.getLinked() != null)
                && (weapon.getLinked().getType() instanceof AmmoType)) {
            AmmoType ammoType = (AmmoType) weapon.getLinked().getType();
            if ((ammoType != null) && (ammoType.getToHitModifier() != 0)) {
                toHit.addModifier(ammoType.getToHitModifier(), TH_AMMO_MOD);
            }
        }

        // targeting computer
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
     * Makes an educated guess as to the to hit modifier by an aerospace unit
     * flying on a ground map doing a strike attack on a unit
     *
     * @param shooter
     *            The {@link Entity} doing the shooting.
     * @param shooterState
     *            The {@link EntityState} of the unit doing the shooting.
     * @param target
     *            The {@link megamek.common.Targetable} being shot at.
     * @param targetState
     *            The {@link megamek.client.bot.princess.EntityState} of the
     *            unit being shot at.
     * @param flightPath
     *            The path the shooter is taking.
     * @param weapon
     *            The weapon being fired as a {@link megamek.common.Mounted}
     *            object.
     * @param game
     *            The {@link megamek.common.IGame being played.}
     * @param assumeUnderFlightPlan
     *            Set TRUE to assume that the target falls under the given
     *            flight path.
     * @return The to hit modifiers for the given weapon firing at the given
     *         target as a {@link ToHitData} object.
     */
    public ToHitData guessAirToGroundStrikeToHitModifier(Entity shooter,
            @Nullable EntityState shooterState, Targetable target,
            @Nullable EntityState targetState, MovePath flightPath,
            Mounted weapon, IGame game, boolean assumeUnderFlightPlan) {

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
        tohit.append(guessToHitModifierHelperForAnyAttack(shooter, shooterState, target, targetState, 0, game));

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
     * Mostly for debugging, this returns a non-null string that describes how
     * the guess has failed to be perfectly accurate. or null if perfectly
     * accurate
     *
     * @param shooter
     *            The unit doing the shooting.
     * @param target
     *            The unit being shot at.
     * @param weapon
     *            The weapon being fired.
     * @param game
     *            The game being played.
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
     * Mostly for debugging, this returns a non-null string that describes how
     * the guess on a physical attack failed to be perfectly accurate, or null
     * if accurate
     *
     * @param shooter
     *            The unit doing the shooting.
     * @param target
     *            The unit being shot at.
     * @param attackType
     *            The attack being made.
     * @param game
     *            The game being played.
     * @return A description of the differences or NULL if there are none.
     */
    String checkGuessPhysical(Entity shooter, Targetable target,
            PhysicalAttackType attackType, IGame game) {

        // This really should only be done for debugging purposes. Regular play
        // should avoid the overhead.
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

        PhysicalInfo guessInfo = new PhysicalInfo(shooter, null, target, null, attackType, game, owner, true);
        PhysicalInfo accurateInfo = new PhysicalInfo(shooter, target, attackType, game, owner, false);
        if (guessInfo.getHitData().getValue() != accurateInfo.getHitData().getValue()) {
            ret += "Incorrect To Hit prediction, physical attack " + attackType.name() + ":\n";
            ret += " Guess: " + Integer.toString(guessInfo.getHitData().getValue()) + " " + guessInfo.getHitData()
                                                                                                     .getDesc() +
                   "\n";
            ret += " Real:  " + Integer.toString(accurateInfo.getHitData().getValue()) + " " +
                   accurateInfo.getHitData().getDesc() + "\n";
        }
        return ret;
    }

    /**
     * Mostly for debugging, this returns a non-null string that describes how
     * any possible guess has failed to be perfectly accurate. or null if
     * perfect
     *
     * @param shooter
     *            The unit doing the shooting.
     * @param game
     *            The game being played.
     * @return A description of the differences or NULL if there are none.
     */
    String checkAllGuesses(Entity shooter, IGame game) {

        // This really should only be done for debugging purposes.  Regular play should avoid the overhead.
        if (!LogLevel.DEBUG.equals(owner.getVerbosity())) {
            return null;
        }

        String ret = "";
        List<Targetable> enemies = getTargetableEnemyEntities(shooter, game);
        for (Targetable enemy : enemies) {
            for (Mounted weapon : shooter.getWeaponList()) {
                String shootingCheck = checkGuess(shooter, enemy, weapon, game);
                if (shootingCheck != null) {
                    ret += shootingCheck;
                }
            }
            String physicalCheck;
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
     *
     * @param firingPlan
     *            The {@link FiringPlan} to be calculated.
     * @param overheatTolerance
     *            How much overheat we're willing to forgive.
     * @param shooterIsAero
     *            Set TRUE if the shooter is an Aero unit. Overheating Aeros
     *            take stiffer penalties.
     */
    void calculateUtility(FiringPlan firingPlan, int overheatTolerance, boolean shooterIsAero) {
        int overheat = 0;
        if (firingPlan.getHeat() > overheatTolerance) {
            overheat = firingPlan.getHeat() - overheatTolerance;
        }

        double modifier = 1;
        modifier += calcCommandUtility(firingPlan.getTarget());
        modifier += calcStrategicBuildingTargetUtility(firingPlan.getTarget());
        modifier += calcPriorityUnitTargetUtility(firingPlan.getTarget());

        double utility = 0;
        utility += DAMAGE_UTILITY * firingPlan.getExpectedDamage();
        utility += CRITICAL_UTILITY * firingPlan.getExpectedCriticals();
        utility += KILL_UTILITY * firingPlan.getKillProbability();
        // Multiply the combined damage/crit/kill utility for a target by a log-scaled factor based on the target's damage potential.
        utility *= calcTargetPotentialDamageMultiplier(firingPlan.getTarget());
        utility += TARGET_HP_FRACTION_DEALT_UTILITY * calcDamageAllocationUtility(firingPlan.getTarget(), firingPlan.getExpectedDamage());
        utility -= calcCivilianTargetDisutility(firingPlan.getTarget());
        utility *= modifier;
        utility -= (shooterIsAero ? OVERHEAT_DISUTILITY_AERO : OVERHEAT_DISUTILITY) * overheat;
        utility -= (firingPlan.getTarget() instanceof MechWarrior) ? EJECTED_PILOT_DISUTILITY : 0;
        firingPlan.setUtility(utility);
    }

    private double calcStrategicBuildingTargetUtility(Targetable target) {
        if (!(target instanceof BuildingTarget)) {
            return 0;
        }

        DecimalFormat coordsFormat = new DecimalFormat("00");
        Coords targetCoords = target.getPosition();
        String coords = coordsFormat.format(targetCoords.getX() + 1) + coordsFormat.format(targetCoords.getY() + 1);
        if (owner.getBehaviorSettings().getStrategicBuildingTargets().contains(coords)) {
            return STRATEGIC_TARGET_UTILITY;
        }
        return 0;
    }

    private double calcPriorityUnitTargetUtility(Targetable target) {
        if (!(target instanceof Entity)) {
            return 0;
        }

        int id = ((Entity) target).getId();
        if (owner.getPriorityUnitTargets().contains(id)) {
            return PRIORITY_TARGET_UTILITY;
        }
        return 0;
    }

    private double calcCivilianTargetDisutility(Targetable target) {
        if (!(target instanceof Entity)) {
            return 0;
        }
        Entity entity = (Entity) target;
        if (entity.isMilitary()) {
            return 0;
        }
        if (owner.getPriorityUnitTargets().contains(entity.getId())) {
            return 0;
        }
        if (owner.getHonorUtil().isEnemyDishonored(entity.getOwnerId())) {
            return 0;
        }
        return CIVILIAN_TARGET_DISUTILITY;
    }

    private double calcCommandUtility(Targetable target) {
        if (!(target instanceof Entity)) {
            return 0;
        }

        Entity entity = (Entity) target;
        if (isCommander(entity)) {
            return COMMANDER_UTILITY;
        } else if (isSubCommander(entity)) {
            return SUB_COMMANDER_UTILITY;
        }
        return 0;
    }

    private boolean isCommander(Entity entity) {
        return entity.isCommander() || entity.hasC3M() || entity.hasC3i() || entity.hasC3MM() ||
               (owner.getHighestEnemyInitiativeId() == entity.getId());
    }

    private boolean isSubCommander(Entity entity) {
        int initBonus = entity.getHQIniBonus() + entity.getQuirkIniBonus();  //removed in IO + entity.getMDIniBonus()
        return entity.hasC3() || entity.hasTAG() || entity.hasBoostedC3() || entity.hasNovaCEWS() ||
               entity.isUsingSpotlight() || entity.hasBAP() || entity.hasActiveECM() || entity.hasActiveECCM() ||
               entity.hasQuirk(OptionsConstants.QUIRK_POS_IMPROVED_SENSORS) || entity.hasEiCockpit() ||
               (initBonus > 0);
    }

    /**
     * Calculates the utility value for doing the given amount of damage to the
     * given target, taking into account damage already applied to this unit by
     * other units belonging to this player(not including allied players!) This
     * utility term is intended to function as a penalty for overkilling targets
     * with fire from multiple units. As such, below certain(high) thresholds,
     * the term does nothing. Only when doing >50% of a target's HP this round
     * is a weight against this FiringPlan applied. In theory, since this term
     * scales linearly independently of the numeric damage dealt to a target,
     * this term will have a larger effect on low-damage units and a smaller
     * effect on high-damage units, which is probably okay for now(since really
     * high damage units tend to overkill as a matter of course more often). In
     * practice, this utility term results in Princess concentrating her fire
     * enough to reliably kill/cripple targets without falling into serious
     * overkill.
     */
    public double calcDamageAllocationUtility(Targetable target, double expectedDamage) {

        double existingDamage = owner.getDamageAlreadyAssigned(target);
        int targetHP = Compute.getTargetTotalHP(owner.getGame(), target);
        double damageFraction = (existingDamage + expectedDamage) / ((double)targetHP);
        double previousDamageFraction = existingDamage / ((double)targetHP);
        //double currentDamageFraction = expectedDamage / ((double)targetHP);

        //Do not shoot at units we already expect to deal more than their total HP of damage to!
        if (previousDamageFraction >= 1.0 ) {
            return 100; 

            // In cases that are not generally overkill(less than 50% of the
            // target's total HP in damage), target as normal(don't want to
            // spread damage in these cases).
            // Also want to disregard damage allocation weighting if the target
            // is a building or infantry/BA(as they don't die until you do 100%
            // damage to them normally).
        } else if (damageFraction < 0.5 
                   || target.getTargetType() == Targetable.TYPE_BUILDING 
                   || owner.getGame().getEntity(target.getTargetId()) instanceof Infantry 
                   || owner.getGame().getEntity(target.getTargetId()) instanceof BattleArmor) {
            return 0;
        }
        //In the remaining case(0.5<=damage), return the fraction of target HP dealt as the penalty scaling factor(multiplied by the weight value to produce a penalty).
        return damageFraction;
    }

    /**
     * Calculates the potential damage that the target could theoretically
     * deliver as a measure of it's potential "threat" to any allied unit on the
     * board, thus prioritizing highly damaging enemies over less damaging ones.
     * For now, this works by simply getting the max damage of the target at
     * range=1, ignoring to-hit, heat, etc.
     */
    public double calcTargetPotentialDamage(Targetable target) {
        if (!(target instanceof Entity)) {
            return 0;
        }
        Entity entity = (Entity) target;
        return getMaxDamageAtRange(entity,1,false,false);
    }

    /**
     * Calculates the logarithmic scaling factor for target damage potential in
     * the utility equation, using the target's potential damage, the weight
     * value TARGET_POTENTIAL_DAMAGE_UTILITY, and Princess's self-preservation
     * value. This is mostly here to not clutter up the utility calculation
     * method with all this extra math.
     */
    private double calcTargetPotentialDamageMultiplier(Targetable target) {
        double target_damage = calcTargetPotentialDamage(target);
        if( target_damage == 0.0 ) { // Do not calculate for zero damage units.
            return 1.0;
        }
        double self_preservation = owner.getBehaviorSettings().getSelfPreservationValue();
        double max_self_preservation =  owner.getBehaviorSettings().getSelfPreservationValue(10); // the preservation value of the highest index, i.e. the max value.
        double preservation_scaling_factor = max_self_preservation / self_preservation; // Because the variance in log value for large numbers is smaller, we need to make a big self-preservation value become a small multiplicative factor, and vice versa.
        return Math.log10(TARGET_POTENTIAL_DAMAGE_UTILITY * preservation_scaling_factor * target_damage + 10); // Add 10 to make the multiplier scale from 1 upwards(1 being a target that does 0 damage)).
    }
        
    /**
     * calculates the 'utility' of a physical action.
     *
     * @param physicalInfo The {@link PhysicalInfo} to be calculated.
     */
    void calculateUtility(PhysicalInfo physicalInfo) {

        // If we can't hit, there's no point.
        if (physicalInfo.getProbabilityToHit() <= 0.0) {
            physicalInfo.setUtility(-10000);
            return;
        }

        double utility = DAMAGE_UTILITY * physicalInfo.getExpectedDamage();
        utility += CRITICAL_UTILITY * physicalInfo.getExpectedCriticals();
        utility += KILL_UTILITY * physicalInfo.getKillProbability();
        utility *= calcTargetPotentialDamageMultiplier(physicalInfo.getTarget());
        utility -= (physicalInfo.getTarget() instanceof MechWarrior) ? EJECTED_PILOT_DISUTILITY : 0;
        utility += calcCommandUtility(physicalInfo.getTarget());
        utility += calcStrategicBuildingTargetUtility(physicalInfo.getTarget());
        utility += calcPriorityUnitTargetUtility(physicalInfo.getTarget());
        utility -= calcCivilianTargetDisutility(physicalInfo.getTarget());

        physicalInfo.setUtility(utility);
    }

    /**
     * Creates a new {@link WeaponFireInfo} object containing data about firing
     * the given weapon at the given target.
     *
     * @param shooter
     *            The unit doing the shooting.
     * @param shooterState
     *            The current state of the shooter.
     * @param target
     *            The target being fired on.
     * @param targetState
     *            The current state of the target.
     * @param weapon
     *            The weapon being fired.
     * @param game
     *            The game being played.
     * @param guessToHit
     *            Set TRUE to estimate the odds to hit rather than doing the
     *            full calculation.
     * @return The resulting {@link WeaponFireInfo}.
     */
    protected WeaponFireInfo buildWeaponFireInfo(Entity shooter,
            EntityState shooterState, Targetable target,
            EntityState targetState, Mounted weapon, IGame game,
            boolean guessToHit) {
        return new WeaponFireInfo(shooter, shooterState, target, targetState,
                weapon, game, guessToHit, owner);
    }

    /**
     * Creates a new {@link WeaponFireInfo} object containing data about firing the given weapon at the given target.
     *
     * @param shooter               The unit doing the shooting.
     * @param flightPath            The path the unit flies over this turn.
     * @param target                The target being fired on.
     * @param targetState           The current state of the target.
     * @param weapon                The weapon being fired.
     * @param game                  The game being played.
     * @param assumeUnderFlightPath Set TRUE to assume the target is under the flight path and avoid doing the full
     *                              calculation.
     * @param guessToHit            Set TRUE to estimate the odds to hit rather than doing the full calculation.
     * @return The resulting {@link WeaponFireInfo}.
     */
    protected WeaponFireInfo buildWeaponFireInfo(Entity shooter,
            MovePath flightPath, Targetable target, EntityState targetState,
            Mounted weapon, IGame game, boolean assumeUnderFlightPath,
            boolean guessToHit) {
        return new WeaponFireInfo(shooter, flightPath, target, targetState,
                weapon, game, assumeUnderFlightPath, guessToHit, owner);
    }

    /**
     * Creates a new {@link WeaponFireInfo} object containing data about firing the given weapon at the given target.
     *
     * @param shooter    The unit doing the shooting.
     * @param target     The target being fired on.
     * @param weapon     The weapon being fired.
     * @param game       The game being played.
     * @param guessToHit Set TRUE to estimate the odds to hit rather than doing the full calculation.
     * @return The resulting {@link WeaponFireInfo}.
     */
    protected WeaponFireInfo buildWeaponFireInfo(Entity shooter, Targetable target, Mounted weapon, IGame game,
                                                 boolean guessToHit) {
        return new WeaponFireInfo(shooter, target, weapon, game, guessToHit, owner);
    }

    /**
     * Creates a firing plan that fires all weapons with nonzero to hit value at
     * a target ignoring heat, and using best guess from different states. Does
     * not change facing.
     *
     * @param shooter
     *            The unit doing the shooting.
     * @param shooterState
     *            The current state of the shooter.
     * @param target
     *            The unit being fired on.
     * @param targetState
     *            The current state of the target.
     * @param game
     *            The game being played.
     * @return The {@link FiringPlan} containing all weapons to be fired.
     */
    FiringPlan guessFullFiringPlan(Entity shooter,
            @Nullable EntityState shooterState, Targetable target,
            @Nullable EntityState targetState, IGame game) {
        final String METHOD_NAME = "guessFullFiringPlan(Entity, EntityState, Targetable, EntityState, IGame)";

        if (shooterState == null) {
            shooterState = new EntityState(shooter);
        }
        if (targetState == null) {
            targetState = new EntityState(target);
        }

        FiringPlan myPlan = new FiringPlan(target);

        // Shooting isn't possible if one of us isn't on the board.
        if ((shooter.getPosition() == null) || shooter.isOffBoard() ||
            !game.getBoard().contains(shooter.getPosition())) {
            owner.log(getClass(), METHOD_NAME, LogLevel.ERROR, "Shooter's position is NULL/Off Board!");
            return myPlan;
        }
        if ((target.getPosition() == null) || target.isOffBoard() || !game.getBoard().contains(target.getPosition())) {
            owner.log(getClass(), METHOD_NAME, LogLevel.ERROR, "Target's position is NULL/Off Board!");
            return myPlan;
        }

        // cycle through my weapons
        for (Mounted weapon : shooter.getWeaponList()) {
            WeaponFireInfo shoot = buildWeaponFireInfo(shooter, shooterState, target, targetState, weapon, game, true);

            // If zero move infantry unit that moved, don't include any weapons
            if ((shooter instanceof Infantry)
                    && (shooter.getWalkMP() == 0)
                    && !(shooterState.getMovementType() == EntityMovementType.MOVE_NONE)) {
                continue;
            }

            //If infantry field gun unit that moved, don't include field guns
            if ((shooter instanceof Infantry)
                    && !(shooterState.getMovementType() == EntityMovementType.MOVE_NONE)
                    && (shoot.getWeapon().getLocation() == Infantry.LOC_FIELD_GUNS)) {
                continue;
            }

            if (shoot.getProbabilityToHit() > 0) {
                myPlan.add(shoot);
            }
        }

        // Rank how useful this plan is.
        calculateUtility(myPlan, calcHeatTolerance(shooter, null), shooterState.isAero());
        return myPlan;
    }

    /**
     * Creates a firing plan that fires all weapons with nonzero to hit value in a air to ground strike
     *
     * @param shooter               The unit doing the shooting.
     * @param target                The unit being fired on.
     * @param targetState           The current state of the target.
     * @param flightPath            The path the shooter is flying over.
     * @param game                  The game being played.
     * @param assumeUnderFlightPath Set TRUE to automatically assume the target will be under the flight path rather
     *                              than going through the full calculation.
     * @return The {@link FiringPlan} containing all weapons to be fired.
     */
    FiringPlan guessFullAirToGroundPlan(Entity shooter, Targetable target,
            @Nullable EntityState targetState, MovePath flightPath, IGame game,
            boolean assumeUnderFlightPath) {
        final String METHOD_NAME = "guessFullAirToGroundPlan(Entity, Targetable, EntityState, MovePath, IGame, " +
                                   "boolean)";

        if (targetState == null) {
            targetState = new EntityState(target);
        }

        // Must fly over the target to hit it.
        if (!assumeUnderFlightPath && !isTargetUnderFlightPath(flightPath, targetState)) {
            return new FiringPlan(target);
        }

        FiringPlan myPlan = new FiringPlan(target);

        // Shooting isn't possible if one of us isn't on the board.
        if ((shooter.getPosition() == null) || shooter.isOffBoard() ||
            !game.getBoard().contains(shooter.getPosition())) {
            owner.log(getClass(), METHOD_NAME, LogLevel.ERROR, "Shooter's position is NULL/Off Board!");
            return myPlan;
        }
        if ((target.getPosition() == null) || target.isOffBoard() || !game.getBoard().contains(target.getPosition())) {
            owner.log(getClass(), METHOD_NAME, LogLevel.ERROR, "Target's position is NULL/Off Board!");
            return myPlan;
        }

        // cycle through my weapons
        for (Mounted weapon : shooter.getWeaponList()) {

            WeaponFireInfo shoot = buildWeaponFireInfo(shooter, flightPath, target, targetState, weapon, game, true,
                                                       true);
            if (shoot.getProbabilityToHit() > 0) {
                myPlan.add(shoot);
            }
        }

        // Rank how useful this plan is.
        calculateUtility(myPlan, calcHeatTolerance(shooter, null), (shooter instanceof Aero));
        return myPlan;
    }

    /**
     * Creates a firing plan that fires all weapons with nonzero to hit value at a target ignoring heat, and using
     * actual game rules from different states
     *
     * @param shooter The unit doing the shooting.
     * @param target  The unit being fired on.
     * @param game    The game being played.
     * @return The {@link FiringPlan} containing all weapons to be fired.
     */
    FiringPlan getFullFiringPlan(Entity shooter, Targetable target,
                                 Map<Mounted, Double> ammoConservation, IGame game) {
        final String METHOD_NAME = "getFullFiringPlan(Entity, Targetable, IGame)";
        final NumberFormat DECF = new DecimalFormat("0.000");

        FiringPlan myPlan = new FiringPlan(target);

        // Shooting isn't possible if one of us isn't on the board.
        if ((shooter.getPosition() == null) || shooter.isOffBoard() ||
            !game.getBoard().contains(shooter.getPosition())) {
            owner.log(getClass(), METHOD_NAME, LogLevel.ERROR, "Shooter's position is NULL/Off Board!");
            return myPlan;
        }
        if ((target.getPosition() == null) || target.isOffBoard() || !game.getBoard().contains(target.getPosition())) {
            owner.log(getClass(), METHOD_NAME, LogLevel.ERROR, "Target's position is NULL/Off Board!");
            return myPlan;
        }

        // cycle through my weapons
        for (Mounted weapon : shooter.getWeaponList()) {
            double toHitThreshold = ammoConservation.get(weapon);
            WeaponFireInfo shoot = buildWeaponFireInfo(shooter, target, weapon, game, false);
            if ((shoot.getProbabilityToHit() > toHitThreshold)) {
                myPlan.add(shoot);
                continue;
            }
            owner.log(getClass(), METHOD_NAME, LogLevel.DEBUG,
                      "\nTo Hit Chance (" + DECF.format(shoot.getProbabilityToHit()) + ") for " + weapon.getName() +
                      " is less than threshold (" + DECF.format(toHitThreshold) + ")");
        }

        // Rank how useful this plan is.
        calculateUtility(myPlan, calcHeatTolerance(shooter, null), (shooter instanceof Aero));
        return myPlan;
    }

    private int calcHeatTolerance(Entity entity, @Nullable Boolean isAero) {

        // If the unit doesn't track heat, we won't worry about it.
        if (entity.getHeatCapacity() == 999) {
            return 999;
        }

        int baseTolerance = entity.getHeatCapacity() - entity.getHeat();

        if (isAero == null) {
            isAero = (entity instanceof Aero);
        }

        // Aeros *really* don't want to overheat.
        if (isAero) {
            return baseTolerance;
        }

        return baseTolerance + 5; // todo add Heat Tolerance to Behavior Settings.
    }

    /**
     * Creates an array that gives the 'best' firing plan (the maximum utility)
     * under the heat of the index
     *
     * @param shooter
     *            The unit doing the shooting.
     * @param alphaStrike
     *            The alpha strike plan.
     * @return
     */
    FiringPlan[] calcFiringPlansUnderHeat(Entity shooter, FiringPlan alphaStrike) {

        // can't be lower than zero heat
        int maxHeat = alphaStrike.getHeat();
        if (maxHeat < 0) {
            maxHeat = 0;
        }

        Targetable target = alphaStrike.getTarget();

        boolean isAero = (shooter instanceof Aero);
        int heatTolerance = calcHeatTolerance(shooter, isAero);

        // How many plans do I need to compute?
        FiringPlan [] bestPlans;
        if (shooter instanceof Infantry || shooter instanceof BattleArmor) {
            bestPlans = new FiringPlan[maxHeat + 4];
        } else {
            bestPlans = new FiringPlan[maxHeat + 1];
        }

        // First plan is a plan that fires only heatless weapons.
        // The remaining plans will build at least some heat.
        bestPlans[0] = new FiringPlan(target);
        FiringPlan nonZeroHeatOptions = new FiringPlan(target);
        FiringPlan swarmAttack = new FiringPlan(target);
        FiringPlan legAttack = new FiringPlan(target);
        FiringPlan fieldGuns = new FiringPlan(target);
        double fieldGunMassAlreadyFired = 0.0; //We need to track the tonnage of field guns being fired, because trying to fire more than the current possible total(# of men left) results in nothing being fired.
        for (WeaponFireInfo weaponFireInfo : alphaStrike) {

            //Leg and swarm attacks can't be mixed with any other attacks, so we have to consider each of those separately.
            if (shooter instanceof Infantry || shooter instanceof BattleArmor) { 
                if (((WeaponType) weaponFireInfo.getWeapon().getType()).getInternalName().equals(Infantry.LEG_ATTACK)) {
                    legAttack.add(weaponFireInfo);
                    continue;
                }
                else if (((WeaponType) weaponFireInfo.getWeapon().getType()).getInternalName().equals(Infantry.SWARM_MEK)) {
                    swarmAttack.add(weaponFireInfo);
                    continue;
                }
                // We probably shouldn't consider stopping swarm attacks, since Princess isn't smart enough to recognize the rare situations when this is a good idea(e.g. planning to put lots of allied fire on the swarm target next turn, target is likely to explode and ammo explosion splash damage is on, etc).
                else if (((WeaponType) weaponFireInfo.getWeapon().getType()) instanceof StopSwarmAttack) {
                    continue;
                }
                else if (! (shooter instanceof BattleArmor) && weaponFireInfo.getWeapon().getLocation() == Infantry.LOC_FIELD_GUNS) {
                    double fieldGunMass = weaponFireInfo.getWeapon().getType().getTonnage(shooter);
                    //Only fire field guns up until we no longer have the men to fire more, since going over that limit results in nothing firing.
                    //In theory we could adapt the heat system to handle this(with tonnage as heat and shooting strength as heat capacity, no heat tolerance).
                    //This would behave much better for units with mixed type field guns, but given that those are rare, this should serve for now.
                    if(fieldGunMassAlreadyFired + fieldGunMass <= ((Infantry)shooter).getShootingStrength()) {
                        fieldGuns.add(weaponFireInfo);
                        fieldGunMassAlreadyFired += fieldGunMass;
                    }
                    continue;
                }
            }
            if (weaponFireInfo.getHeat() == 0) {
                bestPlans[0].add(weaponFireInfo);
            } else {
                nonZeroHeatOptions.add(weaponFireInfo);
            }
        }
        calculateUtility(bestPlans[0], heatTolerance, isAero);

        if(shooter instanceof Infantry || shooter instanceof BattleArmor) {
            calculateUtility(swarmAttack, heatTolerance, isAero);
            calculateUtility(legAttack, heatTolerance, isAero);         
            calculateUtility(fieldGuns, heatTolerance, isAero);
            //Add these plans to the end of the list.
            bestPlans[maxHeat + 1] = swarmAttack;
            bestPlans[maxHeat + 2] = legAttack;
            bestPlans[maxHeat + 3] = fieldGuns;
        }

        // build up heat table
        for (int heatLevel = 1; heatLevel <= maxHeat; heatLevel++) {
            bestPlans[heatLevel] = new FiringPlan(target);

            // Include all the firing options that exist at the last heat level.
            bestPlans[heatLevel].addAll(bestPlans[heatLevel - 1]);
            calculateUtility(bestPlans[heatLevel], heatTolerance, isAero);

            for (WeaponFireInfo weaponFireInfo : nonZeroHeatOptions) {

                int leftoverHeatCapacity = heatLevel - weaponFireInfo.getHeat();

                // If this attack produces heat and is not already included in the plan, check its utility.
                if ((leftoverHeatCapacity >= 0) &&
                    !bestPlans[leftoverHeatCapacity].containsWeapon(weaponFireInfo.getWeapon())) {

                    FiringPlan testPlan = new FiringPlan(target);
                    testPlan.addAll(bestPlans[heatLevel - weaponFireInfo.getHeat()]);
                    testPlan.add(weaponFireInfo);
                    calculateUtility(testPlan, heatTolerance, isAero);

                    // If this plan has a higher utility, add it.
                    if (testPlan.getUtility() > bestPlans[heatLevel].getUtility()) {
                        bestPlans[heatLevel] = testPlan;
                    }
                }
            }
        }
        return bestPlans;
    }

    /*
     * Gets the 'best' firing plan, using heat as a disutility. No twisting is
     * done
     * 
     * @param shooter The unit doing the shooting.
     * 
     * @param target The unit being shot at.
     * 
     * @param game The game currently being played.
     * 
     * @return the 'best' firing plan, using heat as a disutility.
     */
    FiringPlan getBestFiringPlan(Entity shooter, Targetable target, IGame game,
            Map<Mounted, Double> ammoConservation) {

        // Start with an alpha strike.
        FiringPlan alphaStrike = getFullFiringPlan(shooter, target,
                ammoConservation, game);
        // Although they don't track heat, infantry/BA do need to make tradeoffs
        // between firing different weapons, because swarm/leg attacks are
        // mutually exclusive with normal firing, so we treat them similarly to
        // heat-tracking units.
        if (shooter.getHeatCapacity() == 999 && !(shooter instanceof Infantry)
                && !(shooter instanceof BattleArmor)) {
            return alphaStrike; // No need to worry about heat if the unit
                                // doesn't track it.
        }

        // Get all the best plans that generate less heat than an alpha strike.
        FiringPlan allPlans[] = calcFiringPlansUnderHeat(shooter, alphaStrike);

        // Determine the best plan taking into account our heat tolerance.
        return getBestFiringPlanUnderHeat(target, shooter, allPlans);
    }

    /**
     * Guesses the 'best' firing plan under a certain heat No twisting is done
     *
     * @param shooter
     *            The unit doing the shooting.
     * @param shooterState
     *            The current state of the shooting unit.
     * @param target
     *            The unit being shot at.
     * @param targetState
     *            The current state of the target unit.
     * @param maxHeat
     *            How much heat we're willing to tolerate.
     * @param game
     *            The game currently being played.
     * @return the 'best' firing plan under a certain heat.
     */
    FiringPlan guessBestFiringPlanUnderHeat(Entity shooter,
            @Nullable EntityState shooterState, Targetable target,
            @Nullable EntityState targetState, int maxHeat, IGame game) {

        // can't have less than zero heat
        if (maxHeat < 0) {
            maxHeat = 0;
        }

        // Start with an alpha strike. If it falls under our heat limit, use it.
        FiringPlan alphaStrike = guessFullFiringPlan(shooter, shooterState,
                target, targetState, game);
        // Infantry and BA may have alternative options, so we need to consider
        // different firing options.
        if (alphaStrike.getHeat() <= maxHeat && !(shooter instanceof Infantry)
                && !(shooter instanceof BattleArmor)) {
            return alphaStrike;
        }

        // Get the best firing plan that falls under our heat limit.
        FiringPlan heatPlans[] = calcFiringPlansUnderHeat(shooter, alphaStrike);
        Arrays.sort(heatPlans);
        if (heatPlans.length > 0) {
            return heatPlans[0];
        } else {
            // Return a do nothing plan
            return new FiringPlan(target);
        }
    }

    /**
     * Guesses the 'best' firing plan, using heat as a disutility. No twisting
     * is done
     *
     * @param shooter
     *            The unit doing the shooting.
     * @param shooterState
     *            The current state of the shooting unit.
     * @param target
     *            The unit being shot at.
     * @param targetState
     *            The current state of the target unit.
     * @param game
     *            The game currently being played.
     * @return the 'best' firing plan, using heat as a disutility.
     */
    FiringPlan guessBestFiringPlan(Entity shooter,
            @Nullable EntityState shooterState, Targetable target,
            @Nullable EntityState targetState, IGame game) {

        // Start with an alpha strike.
        FiringPlan alphaStrike = guessFullFiringPlan(shooter, shooterState,
                target, targetState, game);

        // If we don't track heat, use the alpha.
        // Although they don't track heat, infantry/BA do need to make tradeoffs
        // between firing different weapons, because swarm/leg attacks are
        // mutually exclusive with normal firing, so we treat them similarly to
        // heat-tracking units.
        if (shooter.getHeatCapacity() == 999 && !(shooter instanceof Infantry)
                && !(shooter instanceof BattleArmor)) {
            return alphaStrike;
        }

        // Get all the best plans that generate less heat than an alpha strike.
        FiringPlan allPlans[] = calcFiringPlansUnderHeat(shooter, alphaStrike);
        return getBestFiringPlanUnderHeat(target, shooter, allPlans);
    }

    private FiringPlan getBestFiringPlanUnderHeat(Targetable target,
            Entity shooter, FiringPlan[] allPlans) {

        // Determine the best plan taking into account our heat tolerance.
        FiringPlan bestPlan = new FiringPlan(target);
        boolean isAero = (shooter instanceof Aero);
        int heatTolerance = calcHeatTolerance(shooter, isAero);
        calculateUtility(bestPlan, heatTolerance, isAero);
        for (FiringPlan firingPlan : allPlans) {
            calculateUtility(firingPlan, heatTolerance, isAero);
            if ((bestPlan.getUtility() < firingPlan.getUtility())) {
                bestPlan = firingPlan;
            }
        }
        return bestPlan;
    }

    /**
     * Gets the 'best' firing plan using heat as disutiltiy includes the option
     * of twisting
     *
     * @param shooter
     *            The unit doing the shooting.
     * @param target
     *            The unit being shot at.
     * @param game
     *            The game currently being played.
     * @return the 'best' firing plan using heat as disutiltiy includes the
     *         option of twisting
     */
    FiringPlan getBestFiringPlanWithTwists(Entity shooter, Targetable target,
            IGame game, Map<Mounted, Double> ammoConservation) {

        // Keep track of our original facing so we can go back to it.
        int originalFacing = shooter.getSecondaryFacing();

        // Get the best plan without any twists.
        FiringPlan noTwistPlan = getBestFiringPlan(shooter, target, game,
                ammoConservation);

        // If we can't change facing, we're done.
        if (!shooter.canChangeSecondaryFacing()) {
            return noTwistPlan;
        }

        // Turn to the right.
        shooter.setSecondaryFacing(correctFacing(originalFacing + 1));
        FiringPlan rightTwistPlan = getBestFiringPlan(shooter, target, game, ammoConservation);
        rightTwistPlan.setTwist(1);

        // Turn to the left.
        shooter.setSecondaryFacing(correctFacing(originalFacing - 1));
        FiringPlan leftTwistPlan = getBestFiringPlan(shooter, target, game, ammoConservation);
        leftTwistPlan.setTwist(-1);

        // todo extended torso twist.

        // Back to where we started.
        shooter.setSecondaryFacing(originalFacing);

        // Return the highest utility plan.
        if ((noTwistPlan.getUtility() > rightTwistPlan.getUtility()) &&
            (noTwistPlan.getUtility() > leftTwistPlan.getUtility())) {
            return noTwistPlan;
        }
        if (leftTwistPlan.getUtility() > rightTwistPlan.getUtility()) {
            return leftTwistPlan;
        }
        return rightTwistPlan;
    }

    /**
     * Guesses the 'best' firing plan under a certain heat includes the option  of twisting
     *
     * @param shooter      The unit doing the shooting.
     * @param shooterState The current state of the shooting unit.
     * @param target       The unit being shot at.
     * @param targetState  The current state of the target unit.
     * @param maxHeat      How much heat we're willing to tolerate.
     * @param game         The game currently being played.
     * @return the 'best' firing plan under a certain heat includes the option  of twisting.
     */
    FiringPlan guessBestFiringPlanUnderHeatWithTwists(Entity shooter,
            @Nullable EntityState shooterState, Targetable target,
            @Nullable EntityState targetState, int maxHeat, IGame game) {

        // Get the best plan without any twists.
        FiringPlan noTwistPlan = guessBestFiringPlanUnderHeat(shooter,
                shooterState, target, targetState, maxHeat, game);

        // If we can't change facing, we're done.
        if (!shooter.canChangeSecondaryFacing()) {
            return noTwistPlan;
        }

        // Keep track of our original facing so we can go back to it.
        int originalFacing = shooter.getSecondaryFacing();

        // Turn to the right.
        shooter.setSecondaryFacing(correctFacing(originalFacing + 1));
        FiringPlan rightTwistPlan = guessBestFiringPlanUnderHeat(shooter,
                shooterState, target, targetState, maxHeat, game);
        rightTwistPlan.setTwist(1);

        // Turn to the left.
        shooter.setSecondaryFacing(correctFacing(originalFacing - 1));
        FiringPlan leftTwistPlan = guessBestFiringPlanUnderHeat(shooter,
                shooterState, target, targetState, maxHeat, game);
        leftTwistPlan.setTwist(-1);

        // todo extended torso twist.

        // Back to where we started.
        shooter.setSecondaryFacing(originalFacing);

        // Return the highest utility plan.
        if ((noTwistPlan.getUtility() > rightTwistPlan.getUtility()) &&
            (noTwistPlan.getUtility() > leftTwistPlan.getUtility())) {
            return noTwistPlan;
        }
        if (leftTwistPlan.getUtility() > rightTwistPlan.getUtility()) {
            return leftTwistPlan;
        }
        return rightTwistPlan;
    }

    /**
     * Guesses the 'best' firing plan under a certain heat includes the option of twisting
     */
    FiringPlan guessBestFiringPlanWithTwists(Entity shooter,
            @Nullable EntityState shooterState, Targetable target,
            @Nullable EntityState targetState, IGame game) {

        // Get the best plan without any twists.
        FiringPlan noTwistPlan = guessBestFiringPlan(shooter, shooterState,
                target, targetState, game);

        // If we can't change facing, we're done.
        if (!shooter.canChangeSecondaryFacing()) {
            return noTwistPlan;
        }

        // Keep track of our original facing so we can go back to it.
        int originalFacing = shooter.getSecondaryFacing();

        // Turn to the right.
        shooter.setSecondaryFacing(correctFacing(originalFacing + 1));
        FiringPlan rightTwistPlan = guessBestFiringPlan(shooter, shooterState,
                target, targetState, game);
        rightTwistPlan.setTwist(1);

        // Turn to the left.
        shooter.setSecondaryFacing(correctFacing(originalFacing - 1));
        FiringPlan leftTwistPlan = guessBestFiringPlan(shooter, shooterState,
                target, targetState, game);
        leftTwistPlan.setTwist(-1);

        // todo extended torso twist.

        // Back to where we started.
        shooter.setSecondaryFacing(originalFacing);

        // Return the highest utility plan.
        if ((noTwistPlan.getUtility() > rightTwistPlan.getUtility()) &&
            (noTwistPlan.getUtility() > leftTwistPlan.getUtility())) {
            return noTwistPlan;
        }
        if (leftTwistPlan.getUtility() > rightTwistPlan.getUtility()) {
            return leftTwistPlan;
        }
        return rightTwistPlan;
    }

    /**
     * Gets all the entities that are potential targets
     *
     * @param shooter The unit doing the shooting.
     * @param game    The game being played.
     * @return A list of potential targets.
     */
    List<Targetable> getTargetableEnemyEntities(Entity shooter, IGame game) {
        List<Targetable> targetableEnemyList = new ArrayList<>();

        // Go through every unit in the game.
        for (Entity entity : game.getEntitiesVector()) {

            // If they are my enenmy and on the board, they're a target.
            if (entity.getOwner().isEnemyOf(shooter.getOwner())
                && (entity.getPosition() != null)
                && !entity.isOffBoard()
                && entity.isTargetable()
                && (entity.getCrew() != null) && !entity.getCrew().isDead()) {

                LosEffects effects =
                        LosEffects.calculateLos(game, shooter.getId(), entity);
                if (effects.canSee()) {
                    targetableEnemyList.add(entity);
                }
            }
        }

        // Add in potential building targets and the like.
        targetableEnemyList.addAll(getAdditionalTargets());

        return targetableEnemyList;
    }

    /**
     * Variation on getTargetableEnemyEntities.
     * Returns all possible enemy targets, regardless of LOS status.
     * @param player The player from whose perspective enemies are determined.
     * @param game    The game being played.
     * @return A list of potential targets.
     */
    List<Targetable> getAllTargetableEnemyEntities(IPlayer player, IGame game) {
        List<Targetable> targetableEnemyList = new ArrayList<>();

        // Go through every unit in the game.
        for (Entity entity : game.getEntitiesVector()) {

            // If they are my enemy and on the board, they're a target.
            if (entity.getOwner().isEnemyOf(player)
                && (entity.getPosition() != null)
                && !entity.isOffBoard()
                && entity.isTargetable()
                && (entity.getCrew() != null) && !entity.getCrew().isDead()) {
                targetableEnemyList.add(entity);
            }
        }

        // Add in potential building targets and the like.
        targetableEnemyList.addAll(getAdditionalTargets());

        return targetableEnemyList;
    }

    /**
     * This is it. Calculate the 'best' possible firing plan for this entity.
     * Overload this function if you think you can do better.
     *
     * @param shooter The unit doing the shooting.
     * @param game    The game being played.
     * @return
     */
    FiringPlan getBestFiringPlan(Entity shooter, IHonorUtil honorUtil, IGame game,
                                 Map<Mounted, Double> ammoConservation) {
        final String METHOD_NAME = "getBestFiringPlan(Entity, IGame)";

        FiringPlan bestPlan = null;

        // Get a list of potential targets.
        List<Targetable> enemies = getTargetableEnemyEntities(shooter, game);

        // Loop through each enemy and find the best plan for attacking them.
        for (Targetable enemy : enemies) {

            boolean priorityTarget = owner.getPriorityUnitTargets().contains(enemy.getTargetId());

            // Skip retreating enemies so long as they haven't fired on me while retreating.
            int playerId = (enemy instanceof Entity) ? ((Entity) enemy).getOwnerId() : -1;
            if (!priorityTarget && honorUtil.isEnemyBroken(enemy.getTargetId(), playerId,
                                                           owner.getForcedWithdrawal())) {
                owner.log(getClass(), METHOD_NAME, LogLevel.INFO, enemy.getDisplayName() + " is broken - ignoring");
                continue;
            }

            FiringPlan plan = getBestFiringPlanWithTwists(shooter, enemy,
                                                          game, ammoConservation);
            owner.log(getClass(), METHOD_NAME, LogLevel.INFO, shooter.getDisplayName() + " at " + enemy
                    .getDisplayName() + " - Best Firing Plan: " + plan.getDebugDescription(true));
            if ((bestPlan == null) || (plan.getUtility() > bestPlan.getUtility())) {
                bestPlan = plan;
            }
        }

        // Return the best overall plan.
        return bestPlan;
    }

    /**
     * Calculates the maximum damage a unit can do at a given range.  Chance to hit is not a factor.
     *
     * @param shooter         The firing unit.
     * @param range           The range to be checked.
     * @param useExtremeRange
     * @return The most damage done at that range.
     */
    // todo cluster and other variable damage.
    public double getMaxDamageAtRange(Entity shooter, int range, boolean useExtremeRange, boolean useLOSRange) {
        double maxDamage = 0;

        // cycle through my weapons
        for (Mounted weapon : shooter.getWeaponList()) {
            WeaponType weaponType = (WeaponType) weapon.getType();
            int bracket = RangeType.rangeBracket(range, weaponType.getRanges(weapon), useExtremeRange, useLOSRange);
            if ((bracket != RangeType.RANGE_OUT) && (weaponType.getDamage() > 0)) {
                maxDamage += weaponType.getDamage();
            }
        }
        return maxDamage;
    }

    /**
     * makes sure facing falls between 0 and 5 This function likely already exists somewhere else
     *
     * @param facing The facing to be corrected.
     * @return The properly adjusted facing.
     */
    public static int correctFacing(int facing) {
        while (facing < 0) {
            facing += 6;
        }
        if (facing > 5) {
            facing = facing % 6;
        }
        return facing;
    }

    /**
     * Makes sure ammo is loaded for each weapon
     */
    public void loadAmmo(Entity shooter, FiringPlan plan) {
        if (shooter == null) {
            return;
        }
        if (plan == null) {
            return;
        }
        Targetable target = plan.getTarget();

        // Loading ammo for all my weapons.
        for (WeaponFireInfo info : plan) {
            Mounted currentWeapon = info.getWeapon();
            if (currentWeapon == null) {
                continue;
            }
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
            WeaponAttackAction action = info.getAction();
            action.setAmmoId(shooter.getEquipmentNum(mountedAmmo));
            info.setAction(action);
            owner.sendAmmoChange(info.getShooter().getId(), shooter.getEquipmentNum(currentWeapon),
                                 shooter.getEquipmentNum(mountedAmmo));
        }
    }

    /*
     * Here's a list of things that aren't technically units, but I want to be
     * able to target anyways. This is create with buildings and bridges and
     * mind
     */
    private List<Targetable> additionalTargets = new ArrayList<>();

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

        StringBuilder msg = new StringBuilder("Getting ammo for ").append(weaponType.getShortName())
                                                                  .append(" firing at ").append(target.getDisplayName
                        ());
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
            List<Mounted> validAmmo = new ArrayList<>();
            for (Mounted a : ammo) {
                if (AmmoType.isAmmoValid(a, weaponType)) {
                    validAmmo.add(a);
                }
            }

            // If no valid ammo was found, return nothing.
            if (validAmmo.isEmpty()) {
                return null;
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
        Mounted returnAmmo;

        // Get the LRM and SRM bins if we have them.
        Mounted mmlSrm = null;
        Mounted mmlLrm = null;
        for (Mounted ammo : ammoList) {
            AmmoType type = (AmmoType) ammo.getType();
            if ((mmlLrm == null) && type.hasFlag(AmmoType.F_MML_LRM)) {
                mmlLrm = ammo;
            } else if (mmlSrm == null) {
                mmlSrm = ammo;
            } else //noinspection ConstantConditions
                if ((mmlSrm != null) && (mmlLrm != null)) {
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
        Mounted returnAmmo;

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
                } else //noinspection ConstantConditions
                    if ((mmlLrm != null) && (mmlSrm != null)) {
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
            } else //noinspection ConstantConditions
                if ((mmlLrm != null) && (mmlSrm != null)) {
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
