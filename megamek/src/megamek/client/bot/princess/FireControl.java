/*
 * MegaMek - Copyright (C) 2000-2011 Ben Mazur (bmazur@sev.org)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.client.bot.princess;

import megamek.common.*;
import megamek.common.actions.*;
import megamek.common.annotations.Nullable;
import megamek.common.annotations.StaticWrapper;
import megamek.common.enums.IlluminationLevel;
import megamek.common.options.OptionsConstants;
import megamek.common.pathfinder.AeroGroundPathFinder;
import megamek.common.weapons.StopSwarmAttack;
import megamek.common.weapons.Weapon;
import megamek.common.weapons.infantry.InfantryWeapon;
import megamek.common.weapons.missiles.ATMWeapon;
import megamek.common.weapons.missiles.MMLWeapon;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;

/**
 * FireControl selects which weapons a unit wants to fire and at whom Pay
 * attention to the difference between "guess" and "get". Guess will be much
 * faster, but inaccurate
 */
public class FireControl {

    protected static final double DAMAGE_UTILITY = 1.0;
    protected static final double CRITICAL_UTILITY = 10.0;
    protected static final double KILL_UTILITY = 50.0;
    protected static final double OVERHEAT_DISUTILITY = 5.0;
    protected static final double OVERHEAT_DISUTILITY_AERO = 50.0; // Aeros *really* don't want to overheat.
    protected static final double EJECTED_PILOT_DISUTILITY = 1000.0;
    protected static final double CIVILIAN_TARGET_DISUTILITY = 250.0;
    protected static final double TARGET_HP_FRACTION_DEALT_UTILITY = -30.0;

    private static final double TARGET_POTENTIAL_DAMAGE_UTILITY = 1.0;
    static final double COMMANDER_UTILITY = 0.5;
    static final double SUB_COMMANDER_UTILITY = 0.25;
    static final double STRATEGIC_TARGET_UTILITY = 0.5;
    static final double PRIORITY_TARGET_UTILITY = 0.25;

    static final String TH_WOODS = "woods";
    static final String TH_SMOKE = "smoke";
    static final String TH_PHY_BASE = "base";
    static final String TH_GUNNERY = "gunnery skill";
    static final String TH_SENSORS = "attacker sensors damaged";
    static final String TH_MINIMUM_RANGE = "Minimum Range";
    static final String TH_HEAT = "heat";
    static final String TH_WEAPON_MOD = "weapon to-hit";
    static final String TH_AMMO_MOD = "ammunition to-hit modifier";
    static final TargetRollModifier TH_ATT_PRONE = new TargetRollModifier(2, "attacker prone");
    static final TargetRollModifier TH_TAR_IMMOBILE = new TargetRollModifier(-4, "target immobile");
    static final TargetRollModifier TH_TAR_SKID = new TargetRollModifier(2, "target skidded");
    static final TargetRollModifier TH_TAR_NO_MOVE = new TargetRollModifier(1, "target didn't move");
    static final TargetRollModifier TH_TAR_SPRINT = new TargetRollModifier(-1, "target sprinted");
    static final TargetRollModifier TH_TAR_AERO_NOE_ADJ = new TargetRollModifier(1, "NOE aero adjacent flight path");
    static final TargetRollModifier TH_TAR_AERO_NOE = new TargetRollModifier(3, "NOE aero non-adjacent flight path");
    static final TargetRollModifier TH_TAR_PRONE_RANGE = new TargetRollModifier(1, "target prone and at range");
    static final TargetRollModifier TH_TAR_PRONE_ADJ = new TargetRollModifier(-2, "target prone and adjacent");
    static final TargetRollModifier TH_TAR_BA = new TargetRollModifier(1, "battle armor target");
    static final TargetRollModifier TH_TAR_MW = new TargetRollModifier(2, "ejected MechWarrior target");
    static final TargetRollModifier TH_TAR_INF = new TargetRollModifier(1, "infantry target");
    static final TargetRollModifier TH_ANTI_AIR = new TargetRollModifier(-2, "anti-aircraft quirk");
    static final TargetRollModifier TH_INDUSTRIAL = new TargetRollModifier(1, "industrial cockpit without advanced fire control");
    static final TargetRollModifier TH_PRIMATIVE_INDUSTRIAL = new TargetRollModifier(2, "primitive industrial cockpit without advanced fire control");
    static final TargetRollModifier TH_TAR_SUPER = new TargetRollModifier(-1, "superheavy target");
    static final TargetRollModifier TH_TAR_GROUND_DS = new TargetRollModifier(-4, "grounded DropShip target");
    static final TargetRollModifier TH_TAR_LOW_PROFILE = new TargetRollModifier(1, "narrow/low profile target");
    static final TargetRollModifier TH_PHY_NOT_MECH = new TargetRollModifier(TargetRoll.IMPOSSIBLE, "non-Meks don't make physical attacks");
    static final TargetRollModifier TH_PHY_TOO_FAR = new TargetRollModifier(TargetRoll.IMPOSSIBLE, "target not adjacent");
    static final TargetRollModifier TH_NULL_POSITION = new TargetRollModifier(TargetRoll.AUTOMATIC_FAIL, "null position");
    static final TargetRollModifier TH_RNG_TOO_FAR = new TargetRollModifier(TargetRoll.IMPOSSIBLE, "target beyond max range");
    static final TargetRollModifier TH_PHY_NOT_IN_ARC = new TargetRollModifier(TargetRoll.IMPOSSIBLE, "target not in arc");
    static final TargetRollModifier TH_PHY_TOO_MUCH_ELEVATION = new TargetRollModifier(TargetRoll.IMPOSSIBLE, "target elevation not in range");
    static final TargetRollModifier TH_PHY_P_TAR_PRONE = new TargetRollModifier(TargetRoll.IMPOSSIBLE, "can't punch while prone");
    static final TargetRollModifier TH_PHY_P_TAR_INF = new TargetRollModifier(TargetRoll.IMPOSSIBLE, "can't punch infantry");
    static final TargetRollModifier TH_PHY_P_NO_ARM = new TargetRollModifier(TargetRoll.IMPOSSIBLE, "Your arm's off!");
    static final TargetRollModifier TH_PHY_P_NO_SHOULDER = new TargetRollModifier(TargetRoll.IMPOSSIBLE, "shoulder destroyed");
    static final TargetRollModifier TH_PHY_P_UPPER_ARM = new TargetRollModifier(2, "upper arm actuator destroyed");
    static final TargetRollModifier TH_PHY_P_LOWER_ARM = new TargetRollModifier(2, "lower arm actuator missing or destroyed");
    static final TargetRollModifier TH_PHY_P_HAND = new TargetRollModifier(1, "hand actuator missing or destroyed");
    static final TargetRollModifier TH_PHY_K_PRONE = new TargetRollModifier(TargetRoll.IMPOSSIBLE, "can't kick while prone");
    static final TargetRollModifier TH_PHY_K_INF = new TargetRollModifier(3, "kicking infantry");
    static final TargetRollModifier TH_PHY_K_INF_RNG = new TargetRollModifier(TargetRoll.IMPOSSIBLE, "Infantry too far away");
    static final TargetRollModifier TH_PHY_K_HIP = new TargetRollModifier(TargetRoll.IMPOSSIBLE, "can't kick with broken hip");
    static final TargetRollModifier TH_PHY_K_UPPER_LEG = new TargetRollModifier(2, "upper leg actuator destroyed");
    static final TargetRollModifier TH_PHY_K_LOWER_LEG = new TargetRollModifier(2, "lower leg actuator destroyed");
    static final TargetRollModifier TH_PHY_K_FOOT = new TargetRollModifier(1, "foot actuator destroyed");
    static final TargetRollModifier TH_PHY_LIGHT = new TargetRollModifier(-2, "weight class attack modifier");
    static final TargetRollModifier TH_PHY_MEDIUM = new TargetRollModifier(-1, "weight class attack modifier");
    static final TargetRollModifier TH_PHY_SUPER = new TargetRollModifier(1, "superheavy attacker");
    static final TargetRollModifier TH_PHY_EASY_PILOT = new TargetRollModifier(-1, "easy to pilot quirk");
    static final TargetRollModifier TH_PHY_P_NO_ARMS_QUIRK = new TargetRollModifier(TargetRoll.IMPOSSIBLE, "no/minimal arms quirk");
    static final TargetRollModifier TH_WEAP_CANNOT_FIRE = new TargetRollModifier(TargetRoll.IMPOSSIBLE, "weapon cannot fire");
    static final TargetRollModifier TH_WEAP_NO_AMMO = new TargetRollModifier(TargetRoll.IMPOSSIBLE, "ammo is gone");
    static final TargetRollModifier TH_WEAP_PRONE_ARMLESS = new TargetRollModifier(TargetRoll.IMPOSSIBLE, "prone and missing an arm");
    static final TargetRollModifier TH_WEAP_ARM_PROP = new TargetRollModifier(TargetRoll.IMPOSSIBLE, "using arm as prop");
    static final TargetRollModifier TH_WEAP_PRONE_LEG = new TargetRollModifier(TargetRoll.IMPOSSIBLE, "prone leg weapon");
    static final TargetRollModifier TH_WEAP_ADA = new TargetRollModifier(-2, "Air-Defense Arrow IV vs airborne target");
    static final TargetRollModifier TH_WEAP_FLAK = new TargetRollModifier(-1, "Flak vs airborne target");
    static final TargetRollModifier TH_WEAPON_NO_ARC = new TargetRollModifier(TargetRoll.IMPOSSIBLE, "not in arc");
    static final TargetRollModifier TH_INF_ZERO_RNG = new TargetRollModifier(TargetRoll.AUTOMATIC_FAIL, "non-infantry shooting with zero range");
    static final TargetRollModifier TH_STOP_SWARM_INVALID = new TargetRollModifier(TargetRoll.IMPOSSIBLE, "not swarming a Mek");
    static final TargetRollModifier TH_SWARM_STOPPED = new TargetRollModifier(TargetRoll.AUTOMATIC_SUCCESS, "stops swarming");
    static final TargetRollModifier TH_OUT_OF_RANGE = new TargetRollModifier(TargetRoll.IMPOSSIBLE, "out of range");
    static final TargetRollModifier TH_SHORT_RANGE = new TargetRollModifier(0, "Short Range");
    static final TargetRollModifier TH_MEDIUM_RANGE = new TargetRollModifier(2, "Medium Range");
    static final TargetRollModifier TH_LONG_RANGE = new TargetRollModifier(4, "Long Range");
    static final TargetRollModifier TH_EXTREME_RANGE = new TargetRollModifier(6, "Extreme Range");
    static final TargetRollModifier TH_TARGETTING_COMP = new TargetRollModifier(-1, "targeting computer");
    static final TargetRollModifier TH_IMP_TARG_SHORT = new TargetRollModifier(-1, "improved targetting (short) quirk");
    static final TargetRollModifier TH_IMP_TARG_MEDIUM = new TargetRollModifier(-1, "improved targetting (medium) quirk");
    static final TargetRollModifier TH_IMP_TARG_LONG = new TargetRollModifier(-1, "improved targetting (long) quirk");
    static final TargetRollModifier TH_VAR_RNG_TARG_SHORT_AT_SHORT = new TargetRollModifier(-1, "variable range targetting (short) quirk");
    static final TargetRollModifier TH_VAR_RNG_TARG_SHORT_AT_LONG = new TargetRollModifier(1, "variable range targetting (short) quirk");
    static final TargetRollModifier TH_VAR_RNG_TARG_LONG_AT_LONG = new TargetRollModifier(-1, "variable range targetting (long) quirk");
    static final TargetRollModifier TH_VAR_RNG_TARG_LONG_AT_SHORT = new TargetRollModifier(1, "variable range targetting (long) quirk");
    static final TargetRollModifier TH_POOR_TARG_SHORT = new TargetRollModifier(1, "poor targetting (short) quirk");
    static final TargetRollModifier TH_POOR_TARG_MEDIUM = new TargetRollModifier(1, "poor targetting (medium) quirk");
    static final TargetRollModifier TH_POOR_TARG_LONG = new TargetRollModifier(1, "poor targetting (long) quirk");
    static final TargetRollModifier TH_ACCURATE_WEAP = new TargetRollModifier(-1, "accurate weapon quirk");
    static final TargetRollModifier TH_INACCURATE_WEAP = new TargetRollModifier(1, "inaccurate weapon quirk");
    static final TargetRollModifier TH_RNG_LARGE = new TargetRollModifier(-1, "target large vehicle or superheavy mech");
    static final TargetRollModifier TH_AIR_STRIKE_PATH = new TargetRollModifier(TargetRoll.IMPOSSIBLE, "target not under flight path");
    static final TargetRollModifier TH_AIR_STRIKE = new TargetRollModifier(2, "strike attack");
    private static final TargetRollModifier TH_STABLE_WEAP = new TargetRollModifier(1, "stabilized weapon quirk");
    private static final TargetRollModifier TH_PHY_LARGE = new TargetRollModifier(-2, "target large vehicle");

    /**
     * The possible fire control types.
     * If you're adding a new one, add it here then make sure to add it to Princess.InitializeFireControls
     */
    public enum FireControlType {
        Basic,
        Infantry,
        MultiTarget
    }

    protected final Princess owner;

    /**
     * Constructor
     *
     * @param owningPrincess The {@link Princess} bot that utilizes this this class for computing firing solutions.
     */
    public FireControl(final Princess owningPrincess) {
        owner = owningPrincess;
    }

    /**
     * Returns the movement modifier calculated by {@link Compute#getAttackerMovementModifier(Game, int,
     * EntityMovementType)}.
     *
     * @param game The current {@link Game}
     * @param shooterId The ID of the unit doing the shooting.
     * @param shooterMoveType The {@link EntityMovementType} of the unit doing the shooting.
     * @return The attacker movement modifier as a {@link ToHitData} object.
     */
    @StaticWrapper()
    protected ToHitData getAttackerMovementModifier(final Game game,
                                                    final int shooterId,
                                                    final EntityMovementType shooterMoveType) {
        return Compute.getAttackerMovementModifier(game, shooterId, shooterMoveType);
    }

    /**
     * Returns the {@link Coords} computed by
     * {@link Compute#getClosestFlightPath(int, Coords, Entity)}.
     *
     * @param shooterPosition The shooter's position.
     * @param targetAero      The aero unit being attacked.
     * @return The {@link Coords} from the target's flight path closest to the shooter.
     */
    @StaticWrapper
    Coords getNearestPointInFlightPath(final Coords shooterPosition, final IAero targetAero) {
        return Compute.getClosestFlightPath(-1, shooterPosition, (Entity) targetAero);
    }

    /**
     * Returns the movement modifier calculated by
     * {@link Compute#getTargetMovementModifier(int, boolean, boolean, Game)}
     *
     * @param hexesMoved The number of hexes the target unit moved.
     * @param jumping Set TRUE if the target jumped.
     * @param vtol Set TRUE if the target is a {@link VTOL}.
     * @param game The current {@link Game}
     * @return The target movement modifier as a {@link ToHitData} object.
     */
    @StaticWrapper()
    protected ToHitData getTargetMovementModifier(final int hexesMoved,
                                                  final boolean jumping,
                                                  final boolean vtol,
                                                  final Game game) {
        return Compute.getTargetMovementModifier(hexesMoved, jumping, vtol, game);
    }

    /**
     * Gets the toHit modifier common to both weapon and physical attacks
     *
     * @param shooter The unit doing the shooting.
     * @param shooterState The state of the unit doing the shooting.
     * @param target Who is being shot at.
     * @param targetState The state of the target.
     * @param distance Distance between shooter and target.
     * @param game The current {@link Game}
     * @return The estimated to hit modifiers.
     */
    ToHitData guessToHitModifierHelperForAnyAttack(final Entity shooter,
                                                   @Nullable EntityState shooterState,
                                                   final Targetable target,
                                                   @Nullable EntityState targetState,
                                                   final int distance,
                                                   final Game game) {

        if (null == shooterState) {
            shooterState = new EntityState(shooter);
        }
        if (null == targetState) {
            targetState = new EntityState(target);
        }

        // Can't shoot if one of us is not on the board.
        // todo exception for off-board artillery.
        if ((null == shooterState.getPosition()) || (null == targetState.getPosition())) {
            return new ToHitData(TH_NULL_POSITION);
        }

        // Is the target in range at all?
        final int maxRange = owner.getMaxWeaponRange(shooter, target.isAirborne());
        if (distance > maxRange) {
            return new ToHitData(TH_RNG_TOO_FAR);
        }

        final ToHitData toHitData = new ToHitData();

        // If people are moving or lying down, there are consequences
        toHitData.append(getAttackerMovementModifier(game, shooter.getId(), shooterState.getMovementType()));

        // Ground units attacking airborne aeros.
        if (!shooterState.isAero() && targetState.isAirborneAero()) {
            final IAero targetAero = (IAero) target;
            if (((Entity) targetAero).isNOE()) {
                final Coords closestInFlightPath = getNearestPointInFlightPath(shooterState.getPosition(), targetAero);
                final int aeroDistance = closestInFlightPath.distance(shooterState.getPosition());
                if (1 >= aeroDistance) {
                    toHitData.addModifier(TH_TAR_AERO_NOE_ADJ);
                } else {
                    toHitData.addModifier(TH_TAR_AERO_NOE);
                }
            }
        } else {
            // Now more inclusive VTOL/WiGE check, should cover VTOL CI/BA as well.
            toHitData.append(getTargetMovementModifier(targetState.getHexesMoved(), targetState.isJumping(),
                                                       target.isAirborneVTOLorWIGE(), game));
        }
        if (shooterState.isProne()) {
            toHitData.addModifier(TH_ATT_PRONE);
        }
        if (targetState.isImmobile() && !target.isHexBeingBombed()) {
            toHitData.addModifier(TH_TAR_IMMOBILE);
        }
        if (game.getOptions().booleanOption(OptionsConstants.ADVGRNDMOV_TACOPS_STANDING_STILL)
            && (EntityMovementType.MOVE_NONE == targetState.getMovementType())
            && !targetState.isImmobile()
            && !((target instanceof Infantry) || (target instanceof VTOL) ||
                 (target instanceof GunEmplacement))) {
            toHitData.addModifier(TH_TAR_NO_MOVE);
        }

        // did the target sprint?
        if (EntityMovementType.MOVE_SPRINT == targetState.getMovementType()
            || EntityMovementType.MOVE_VTOL_SPRINT == targetState.getMovementType()) {
            toHitData.addModifier(TH_TAR_SPRINT);
        }

        // terrain modifiers, since "compute" won't let me do these remotely
        final Hex targetHex = game.getBoard().getHex(targetState.getPosition());
        int woodsLevel = targetHex.terrainLevel(Terrains.WOODS);
        if (targetHex.terrainLevel(Terrains.JUNGLE) > woodsLevel) {
            woodsLevel = targetHex.terrainLevel(Terrains.JUNGLE);
        }
        if (1 <= woodsLevel) {
            toHitData.addModifier(woodsLevel, TH_WOODS);
        }

        final int smokeLevel = targetHex.terrainLevel(Terrains.SMOKE);
        if (1 <= smokeLevel) {
            // Smoke level doesn't necessarily correspond to the to-hit modifier
            // even levels are light smoke, odd are heavy smoke
            toHitData.addModifier((smokeLevel % 2) + 1, TH_SMOKE);
        }

        if (targetState.isProne() && (1 < distance)) {
            toHitData.addModifier(TH_TAR_PRONE_RANGE);
        } else if (targetState.isProne() && (1 == distance)) {
            toHitData.addModifier(TH_TAR_PRONE_ADJ);
        }

        if (EntityMovementType.MOVE_SKID == targetState.getMovementType()) {
            toHitData.addModifier(TH_TAR_SKID);
        }

        final boolean isShooterInfantry = (shooter instanceof Infantry);
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
            final Mech shooterMech = (Mech) shooter;
            if (Mech.COCKPIT_INDUSTRIAL == shooterMech.getCockpitType()) {
                toHitData.addModifier(TH_INDUSTRIAL);
            } else if (Mech.COCKPIT_PRIMITIVE_INDUSTRIAL == shooterMech.getCockpitType()) {
                toHitData.addModifier(TH_PRIMATIVE_INDUSTRIAL);
            }
        }

        if (target instanceof Mech) {
            final Mech targetMech = (Mech) target;
            if (Mech.COCKPIT_SUPERHEAVY == targetMech.getCockpitType() ||
                Mech.COCKPIT_SUPERHEAVY_TRIPOD == targetMech.getCockpitType()) {
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
     * @param shooter The unit doing the attacking.
     * @param shooterState The state of the unit doing the attacking.
     * @param target Who is being attacked.
     * @param targetState The state of the target.
     * @param attackType The type of physical attack being made.
     * @param game The current {@link Game}
     * @return The estimated to hit modifiers.
     */
    ToHitData guessToHitModifierPhysical(final Entity shooter,
                                         @Nullable EntityState shooterState,
                                         final Targetable target,
                                         @Nullable EntityState targetState,
                                         final PhysicalAttackType attackType,
                                         final Game game) {

        // todo weapons, frenzy (pg 144) & vehicle charges.
        // todo heat mods to piloting?

        if (!(shooter instanceof Mech)) {
            return new ToHitData(TH_PHY_NOT_MECH);
        }

        if (null == shooterState) {
            shooterState = new EntityState(shooter);
        }
        if (null == targetState) {
            targetState = new EntityState(target);
        }

        // We can hit someone who isn't standing right next to us.
        final int distance = shooterState.getPosition().distance(targetState.getPosition());
        if (1 < distance) {
            return new ToHitData(TH_PHY_TOO_FAR);
        }

        // Get the general to hit modifiers.
        final ToHitData toHitData = new ToHitData();
        toHitData.append(guessToHitModifierHelperForAnyAttack(shooter, shooterState, target, targetState, distance,
                                                              game));
        if (TargetRoll.IMPOSSIBLE == toHitData.getValue() || TargetRoll.AUTOMATIC_FAIL == toHitData.getValue()) {
            return toHitData;
        }

        // Check if target is within arc
        final int arc;
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
        final Hex attackerHex = game.getBoard().getHex(shooterState.getPosition());
        final Hex targetHex = game.getBoard().getHex(targetState.getPosition());
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
            final int armLocation = PhysicalAttackType.RIGHT_PUNCH == attackType ? Mech.LOC_RARM : Mech.LOC_LARM;
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
            final int legLocation = PhysicalAttackType.RIGHT_KICK == attackType ? Mech.LOC_RLEG : Mech.LOC_LLEG;

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
                if (0 == distance) {
                    toHitData.addModifier(TH_PHY_K_INF);
                } else {
                    return new ToHitData(TH_PHY_K_INF_RNG);
                }
            }
        }

        if (game.getOptions().booleanOption(OptionsConstants.ADVGRNDMOV_TACOPS_PHYSICAL_ATTACK_PSR)) {
            if (EntityWeightClass.WEIGHT_LIGHT == shooter.getWeightClass()) {
                toHitData.addModifier(TH_PHY_LIGHT);
            } else if (EntityWeightClass.WEIGHT_MEDIUM == shooter.getWeightClass()) {
                toHitData.addModifier(TH_PHY_MEDIUM);
            }
        }

        if ((target instanceof LargeSupportTank) || (target instanceof FixedWingSupport) ||
            (target instanceof Dropship && target.isAirborne())) {
            toHitData.addModifier(TH_PHY_LARGE);
        }
        final Mech shooterMech = (Mech) shooter;
        if (Mech.COCKPIT_SUPERHEAVY == shooterMech.getCockpitType() ||
            Mech.COCKPIT_SUPERHEAVY_TRIPOD == shooterMech.getCockpitType()) {
            toHitData.addModifier(TH_PHY_SUPER);
        }

        if (shooter.hasQuirk(OptionsConstants.QUIRK_POS_EASY_PILOT) && (3 < shooter.getCrew().getPiloting())) {
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
    protected boolean isInArc(final Coords shooterPosition,
                              final int shooterFacing,
                              final Coords targetPosition,
                              final int weaponArc) {
        return Compute.isInArc(shooterPosition, shooterFacing, targetPosition, weaponArc);
    }

    /**
     * Returns the value of {@link LosEffects#calculateLOS(Game, Entity, Targetable, Coords, Coords, boolean)}.
     *
     * @param game The current {@link Game}
     * @param shooter The shooting unit.
     * @param target The unit being shot at as a {@link Targetable} object.
     * @param shooterPosition The current {@link Coords} of the shooter.
     * @param targetPosition The current {@link Coords} of the target.
     * @param spotting Set TRUE if the shooter is simply spotting for indirect fire.
     * @return The resulting {@link LosEffects}.
     */
    @StaticWrapper
    LosEffects getLosEffects(final Game game, final @Nullable Entity shooter,
                             final @Nullable Targetable target,
                             final @Nullable Coords shooterPosition,
                             final @Nullable Coords targetPosition, final boolean spotting) {
        return LosEffects.calculateLOS(game, shooter, target, shooterPosition, targetPosition, spotting);
    }

    /**
     * Returns the value of {@link Compute#getSwarmMekBaseToHit(Entity, Entity, Game)}.
     *
     * @param attacker The attacking {@link Entity}.
     * @param defender The target of the attack.
     * @param game The current {@link Game}
     * @return The to hit modifiers as a {@link ToHitData} object.
     */
    @StaticWrapper
    private ToHitData getSwarmMekBaseToHit(final Entity attacker,
                                           final Entity defender,
                                           final Game game) {
        return Compute.getSwarmMekBaseToHit(attacker, defender, game);
    }

    /**
     * Returns the value of {@link Compute#getLegAttackBaseToHit(Entity, Entity, Game)}.
     *
     * @param attacker The attacking {@link Entity}.
     * @param defender The target of the attack.
     * @param game The current {@link Game}
     * @return The to hit modifiers as a {@link ToHitData} object.
     */
    @StaticWrapper
    private ToHitData getLegAttackBaseToHit(final Entity attacker,
                                            final Entity defender,
                                            final Game game) {
        return Compute.getLegAttackBaseToHit(attacker, defender, game);
    }

    /**
     * Returns the value of {@link Compute#getInfantryRangeMods(int, InfantryWeapon, InfantryWeapon, boolean)}.
     *
     * @param distance The distance to the target.
     * @param weapon   The {@link InfantryWeapon} being fired.
     * @return The to hit modifiers as a {@link ToHitData} object.
     */
    @StaticWrapper
    private ToHitData getInfantryRangeMods(final int distance,
                                           final InfantryWeapon weapon,
                                           final InfantryWeapon secondary,
                                           final boolean underwater) {
        return Compute.getInfantryRangeMods(distance, weapon, secondary, underwater);
    }

    /**
     * Returns the value of {@link Compute#getDamageWeaponMods(Entity, Mounted)}.
     *
     * @param attacker The attacking {@link Entity}.
     * @param weapon   The {@link Mounted} weapon being fired.
     * @return The to hit modifiers as a {@link ToHitData} object.
     */
    @StaticWrapper
    private ToHitData getDamageWeaponMods(final Entity attacker,
                                          final Mounted weapon) {
        return Compute.getDamageWeaponMods(attacker, weapon);
    }

    private boolean isLargeTarget(final Targetable target) {
        if ((target instanceof LargeSupportTank) || (target instanceof FixedWingSupport) ||
            (target instanceof Dropship && target.isAirborne())) {
            return true;
        }
        if (!(target instanceof Mech)) {
            return false;
        }

        final Mech targetMech = (Mech) target;
        return (Mech.COCKPIT_SUPERHEAVY == targetMech.getCockpitType()) ||
               (Mech.COCKPIT_SUPERHEAVY_TRIPOD == targetMech.getCockpitType());
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
     * @param game The current {@link Game}
     * @return The to hit modifiers for the given weapon firing at the given
     *         target as a {@link ToHitData} object.
     */
    ToHitData guessToHitModifierForWeapon(final Entity shooter,
                                          @Nullable EntityState shooterState,
                                          final Targetable target,
                                          @Nullable EntityState targetState,
                                          final Mounted weapon,
                                          final Game game) {

        if (null == shooterState) {
            shooterState = new EntityState(shooter);
        }
        if (null == targetState) {
            targetState = new EntityState(target);
        }

        // First check if the shot is impossible
        if (!weapon.canFire()) {
            return new ToHitData(TH_WEAP_CANNOT_FIRE);
        }

        // Make sure we have ammo.
        final WeaponType weaponType = (WeaponType) weapon.getType();
        if (AmmoType.T_NA != weaponType.getAmmoType()) {
            if (null == weapon.getLinked()) {
                return new ToHitData(TH_WEAP_NO_AMMO);
            }
            if (0 == weapon.getLinked().getUsableShotsLeft()) {
                return new ToHitData(TH_WEAP_NO_AMMO);
            }
        }

        if (shooterState.isProne()) {
            // Cannot fire if we cannot at least prop ourselves up.
            if (shooter.isLocationBad(Mech.LOC_LARM) && shooter.isLocationBad(Mech.LOC_RARM)) {
                return new ToHitData(TH_WEAP_PRONE_ARMLESS);
            }
            // Cannot fire weapons mounted in the propping arm.
            if ((Mech.LOC_LARM == weapon.getLocation() || Mech.LOC_RARM == weapon.getLocation())
                && shooter.isLocationBad(weapon.getLocation())) {
                return new ToHitData(TH_WEAP_ARM_PROP);
            }
            // Cannot fire leg-mounted weapons while prone.)
            if ((Mech.LOC_LLEG == weapon.getLocation()) || (Mech.LOC_RLEG == weapon.getLocation())) {
                return new ToHitData(TH_WEAP_PRONE_LEG);
            }
        }

        // Check if torso twists affect weapon
        int shooterFacing = shooterState.getFacing();
        if (shooter.isSecondaryArcWeapon(shooter.getEquipmentNum(weapon))) {
            shooterFacing = shooterState.getSecondaryFacing();
        }
        final boolean inArc = isInArc(shooterState.getPosition(), shooterFacing, targetState.getPosition(),
                                      shooter.getWeaponArc(shooter.getEquipmentNum(weapon)));
        if (!inArc) {
            return new ToHitData(TH_WEAPON_NO_ARC);
        }

        // Check range.
        int distance = shooterState.getPosition().distance(targetState.getPosition());

        // Ground units attacking airborne aero considerations.
        if (targetState.isAirborneAero() && !shooterState.isAero()) {

            // If the aero is attacking me, there is no range.
            if (target.getId() == shooter.getId()) {
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
        } else if ((RangeType.RANGE_MINIMUM == range) && targetState.isAirborneAero()) {
            range = RangeType.RANGE_SHORT;
        }

        // Cannot shoot at 0 range infantry unless shooter is also infantry.
        final boolean isShooterInfantry = (shooter instanceof Infantry);
        if ((0 == distance) && (!isShooterInfantry) && !(weaponType instanceof StopSwarmAttack) &&
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
        final ToHitData baseMods = guessToHitModifierHelperForAnyAttack(shooter,
                                                                        shooterState,
                                                                        target,
                                                                        targetState,
                                                                        distance,
                                                                        game);
        if (TargetRoll.IMPOSSIBLE == baseMods.getValue() || TargetRoll.AUTOMATIC_FAIL == baseMods.getValue()) {
            return baseMods;
        }

        // Base to hit is gunnery skill
        final ToHitData toHit = new ToHitData(shooter.getCrew().getGunnery(), TH_GUNNERY);
        toHit.append(baseMods);

        // There is kindly already a class that will calculate line of sight for me
        // todo take into account spotting for indirect fire.
        final LosEffects losEffects = getLosEffects(game, shooter, target, shooterState.getPosition(),
                targetState.getPosition(), false);

        // water is a separate los effect
        final Hex targetHex = game.getBoard().getHex(targetState.getPosition());
        Entity targetEntity = null;
        if (target instanceof Entity) {
            targetEntity = (Entity) target;
        }
        if (null != targetEntity) {
            if (targetHex.containsTerrain(Terrains.WATER)
                && (1 == targetHex.terrainLevel(Terrains.WATER))
                && (0 < targetEntity.height())) {
                losEffects.setTargetCover(losEffects.getTargetCover() | LosEffects.COVER_HORIZONTAL);
            }
        }

        // Can we still hit after taking into account LoS?
        toHit.append(losEffects.losModifiers(game));
        if ((TargetRoll.IMPOSSIBLE == toHit.getValue()) || (TargetRoll.AUTOMATIC_FAIL == toHit.getValue())) {
            return toHit; // you can't hit what you can't see
        }

        // Handle sensor damage.  Mek sensor damage is handled under general damage mods.
        if (shooter instanceof Tank) {
            final int sensors = ((Tank) shooter).getSensorHits();
            if (0 < sensors) {
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
        if ((TargetRoll.IMPOSSIBLE == toHit.getValue()) || (TargetRoll.AUTOMATIC_FAIL == toHit.getValue())) {
            return toHit;
        }

        // Now deal with range effects
        if (!weaponType.hasFlag(WeaponType.F_INFANTRY)) {
            if (RangeType.RANGE_SHORT == range) {
                toHit.addModifier(TH_SHORT_RANGE);
            } else if (RangeType.RANGE_MEDIUM == range) {
                toHit.addModifier(TH_MEDIUM_RANGE);
            } else if (RangeType.RANGE_LONG == range) {
                toHit.addModifier(TH_LONG_RANGE);
            } else if (RangeType.RANGE_EXTREME == range) {
                toHit.addModifier(TH_EXTREME_RANGE);
            } else if (RangeType.RANGE_MINIMUM == range) {
                toHit.addModifier((weaponType.getMinimumRange() - distance) + 1, TH_MINIMUM_RANGE);
            }
        } else {
            toHit.append(getInfantryRangeMods(distance, (InfantryWeapon) weapon.getType(),
                                              isShooterInfantry ? ((Infantry) shooter).getSecondaryWeapon() : null,
                                              ILocationExposureStatus.WET == shooter.getLocationStatus(weapon.getLocation())));
        }

        // let us not forget about heat
        if (0 != shooter.getHeatFiringModifier()) {
            toHit.addModifier(shooter.getHeatFiringModifier(), TH_HEAT);
        }

        // and damage
        toHit.append(getDamageWeaponMods(shooter, weapon));

        // weapon mods
        if (0 != weaponType.getToHitModifier()) {
            toHit.addModifier(weaponType.getToHitModifier(), TH_WEAPON_MOD);
        }

        // Target size.
        if (isLargeTarget(target)) {
            toHit.addModifier(TH_RNG_LARGE);
        }

        // ammo mods
        if (AmmoType.T_NA != weaponType.getAmmoType()
            && (null != weapon.getLinked())
            && (weapon.getLinked().getType() instanceof AmmoType)) {
            final AmmoType ammoType = (AmmoType) weapon.getLinked().getType();
            if (null != ammoType){
                // Set of munitions we'll consider for Flak targeting
                EnumSet<AmmoType.Munitions> aaMunitions = EnumSet.of(
                        AmmoType.Munitions.M_CLUSTER,
                        AmmoType.Munitions.M_FLAK
                );
                EnumSet<AmmoType.Munitions> ArtyOnlyMunitions = EnumSet.of(
                        AmmoType.Munitions.M_FLECHETTE,
                        AmmoType.Munitions.M_FAE
                );
                if (0 != ammoType.getToHitModifier()) {
                    toHit.addModifier(ammoType.getToHitModifier(), TH_AMMO_MOD);
                }
                // Air-defense Arrow IV handling; can only fire at airborne targets
                if (ammoType.getMunitionType().contains(AmmoType.Munitions.M_ADA)){
                    if(target.isAirborne() || target.isAirborneVTOLorWIGE()){
                        toHit.addModifier(TH_WEAP_ADA);
                    }
                    else{
                        toHit.addModifier(TH_WEAP_CANNOT_FIRE);
                    }
                }
                // Handle cluster, flak, AAA vs Airborne, Arty-only vs Airborne
                if (target.isAirborne() || target.isAirborneVTOLorWIGE()){
                    if (ammoType.getMunitionType().stream().anyMatch(aaMunitions::contains)
                        || ammoType.countsAsFlak()){
                        toHit.addModifier(TH_WEAP_FLAK);
                    } else if (ammoType.getMunitionType().stream().anyMatch(ArtyOnlyMunitions::contains)){
                        toHit.addModifier(TH_WEAP_CANNOT_FIRE);
                    }
                }
            }
        }

        // targeting computer
        if (shooter.hasTargComp() && weaponType.hasFlag(WeaponType.F_DIRECT_FIRE)) {
            toHit.addModifier(TH_TARGETTING_COMP);
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
        if (weapon.hasQuirk(OptionsConstants.QUIRK_WEAP_POS_STABLE_WEAPON) && (EntityMovementType.MOVE_RUN == shooter.moved)) {
            toHit.addModifier(TH_STABLE_WEAP);
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
     * @param game The current {@link Game}
     * @param assumeUnderFlightPlan
     *            Set TRUE to assume that the target falls under the given
     *            flight path.
     * @return The to hit modifiers for the given weapon firing at the given
     *         target as a {@link ToHitData} object.
     */
    ToHitData guessAirToGroundStrikeToHitModifier(final Entity shooter,
                                                  @Nullable EntityState shooterState,
                                                  final Targetable target,
                                                  @Nullable EntityState targetState,
                                                  final MovePath flightPath,
                                                  final Mounted weapon,
                                                  final Game game,
                                                  final boolean assumeUnderFlightPlan) {

        if (null == targetState) {
            targetState = new EntityState(target);
        }
        if (null == shooterState) {
            shooterState = new EntityState(shooter);
        }

        // first check if the shot is impossible
        if (!weapon.canFire()) {
            return new ToHitData(TH_WEAP_CANNOT_FIRE);
        }

        // Is the weapon loaded?
        if (AmmoType.T_NA != ((WeaponType) weapon.getType()).ammoType) {
            if (null == weapon.getLinked()) {
                return new ToHitData(TH_WEAP_NO_AMMO);
            }
            if (0 == weapon.getLinked().getUsableShotsLeft()) {
                return new ToHitData(TH_WEAP_NO_AMMO);
            }
        }

        // check if target is even under our path
        if (!assumeUnderFlightPlan && !isTargetUnderFlightPath(flightPath, targetState)) {
            return new ToHitData(TH_AIR_STRIKE_PATH);
        }

        // Base to hit is gunnery skill
        final ToHitData tohit = new ToHitData(shooter.getCrew().getGunnery(), TH_GUNNERY);

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
    boolean isTargetUnderFlightPath(final MovePath flightPath,
                                    final EntityState targetState) {

        final Coords targetCoords = targetState.getPosition();
        for (final Enumeration<MoveStep> step = flightPath.getSteps(); step.hasMoreElements(); ) {
            final Coords stepCoords = step.nextElement().getPosition();
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
     * @param game The current {@link Game}
     * @return A description of the differences or NULL if there are none.
     */
    private String checkGuess(final Entity shooter,
                              final Targetable target,
                              final Mounted weapon,
                              final Game game) {

        // This really should only be done for debugging purposes. Regular play should avoid the overhead.
        if (LogManager.getLogger().getLevel().isMoreSpecificThan(Level.INFO)) {
            return null;
        }

        // Don't bother checking these as the guesses are minimal (or non-existent).
        if (shooter.isAero() || (null == shooter.getPosition()) || (null == target.getPosition())) {
            return null;
        }

        String ret = "";
        final WeaponFireInfo guessInfo = new WeaponFireInfo(shooter, new EntityState(shooter),
                target, null, weapon, game, true, owner);
        final WeaponFireInfo accurateInfo = new WeaponFireInfo(shooter, target, weapon, game, false, owner);

        if (guessInfo.getToHit().getValue() != accurateInfo.getToHit().getValue()) {
            ret += "Incorrect To Hit prediction, weapon " + weapon.getName() + " (" + shooter.getChassis() + " vs " +
                   target.getDisplayName() + ")" + ":\n";
            ret += " Guess: " + guessInfo.getToHit().getValue() + " " +
                   guessInfo.getToHit().getDesc() + "\n";
            ret += " Real:  " + accurateInfo.getToHit().getValue() + " " +
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
     * @param game The current {@link Game}
     * @return A description of the differences or NULL if there are none.
     */
    private @Nullable String checkGuessPhysical(final Entity shooter, final Targetable target,
                                                final PhysicalAttackType attackType, final Game game) {
        // This really should only be done for debugging purposes. Regular play should avoid the overhead.
        if (LogManager.getLogger().getLevel().isMoreSpecificThan(Level.INFO)) {
            return null;
        }

        // only mechs can do physicals
        if (!(shooter instanceof Mech)) {
            return null;
        }

        String ret = "";
        if (null == shooter.getPosition()) {
            return "Shooter has NULL coordinates!";
        } else if (null == target.getPosition()) {
            return "Target has NULL coordinates!";
        }

        final PhysicalInfo guessInfo = new PhysicalInfo(shooter, null, target, null, attackType, game, owner, true);
        final PhysicalInfo accurateInfo = new PhysicalInfo(shooter, target, attackType, game, owner, false);
        if (guessInfo.getHitData().getValue() != accurateInfo.getHitData().getValue()) {
            ret += "Incorrect To Hit prediction, physical attack " + attackType.name() + ":\n";
            ret += " Guess: " + guessInfo.getHitData().getValue() + " " + guessInfo.getHitData().getDesc() + "\n";
            ret += " Real:  " + accurateInfo.getHitData().getValue() + " " +
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
     * @param game The current {@link Game}
     * @return A description of the differences or NULL if there are none.
     */
    @Nullable String checkAllGuesses(final Entity shooter, final Game game) {
        // This really should only be done for debugging purposes.  Regular play should avoid the overhead.
        if (LogManager.getLogger().getLevel().isMoreSpecificThan(Level.INFO)) {
            return null;
        }

        final StringBuilder ret = new StringBuilder();
        final List<Targetable> enemies = getTargetableEnemyEntities(shooter, game, owner.getFireControlState());
        for (final Targetable enemy : enemies) {
            for (final Mounted weapon : shooter.getWeaponList()) {
                final String shootingCheck = checkGuess(shooter, enemy, weapon, game);
                if (null != shootingCheck) {
                    ret.append(shootingCheck);
                }
            }
            String physicalCheck;
            physicalCheck = checkGuessPhysical(shooter, enemy, PhysicalAttackType.RIGHT_KICK, game);
            if (null != physicalCheck) {
                ret.append(physicalCheck);
            }
            physicalCheck = checkGuessPhysical(shooter, enemy, PhysicalAttackType.LEFT_KICK, game);
            if (null != physicalCheck) {
                ret.append(physicalCheck);
            }
            physicalCheck = checkGuessPhysical(shooter, enemy, PhysicalAttackType.RIGHT_PUNCH, game);
            if (null != physicalCheck) {
                ret.append(physicalCheck);
            }
            physicalCheck = checkGuessPhysical(shooter, enemy, PhysicalAttackType.LEFT_PUNCH, game);
            if (null != physicalCheck) {
                ret.append(physicalCheck);
            }

        }
        return ret.toString();
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
    void calculateUtility(final FiringPlan firingPlan,
                          final int overheatTolerance,
                          final boolean shooterIsAero) {
        int overheat = 0;
        if (firingPlan.getHeat() > overheatTolerance) {
            overheat = firingPlan.getHeat() - overheatTolerance;
        }

        double modifier = 1;
        modifier += calcCommandUtility(firingPlan.getTarget());
        modifier += calcStrategicBuildingTargetUtility(firingPlan.getTarget());
        modifier += calcPriorityUnitTargetUtility(firingPlan.getTarget());

        double expectedDamage = firingPlan.getExpectedDamage();
        double utility = 0;
        utility += DAMAGE_UTILITY * expectedDamage;
        utility += CRITICAL_UTILITY * firingPlan.getExpectedCriticals();
        utility += KILL_UTILITY * firingPlan.getKillProbability();
        // Multiply the combined damage/crit/kill utility for a target by a log-scaled factor based on the target's damage potential.
        utility *= calcTargetPotentialDamageMultiplier(firingPlan.getTarget());
        utility += TARGET_HP_FRACTION_DEALT_UTILITY * calcDamageAllocationUtility(firingPlan.getTarget(), expectedDamage);
        utility -= calcCivilianTargetDisutility(firingPlan.getTarget());
        utility *= modifier;
        utility -= (shooterIsAero ? OVERHEAT_DISUTILITY_AERO : OVERHEAT_DISUTILITY) * overheat;
        utility -= (firingPlan.getTarget() instanceof MechWarrior) ? EJECTED_PILOT_DISUTILITY : 0;
        firingPlan.setUtility(utility);
    }

    protected double calcStrategicBuildingTargetUtility(final Targetable target) {
        if (!(target instanceof BuildingTarget)) {
            return 0;
        }

        final DecimalFormat coordsFormat = new DecimalFormat("00");
        final Coords targetCoords = target.getPosition();
        final String coords = coordsFormat.format(targetCoords.getX() + 1) + coordsFormat.format(targetCoords.getY() + 1);
        if (owner.getBehaviorSettings().getStrategicBuildingTargets().contains(coords)) {
            return STRATEGIC_TARGET_UTILITY;
        }
        return 0;
    }

    protected double calcPriorityUnitTargetUtility(final Targetable target) {
        if (!(target instanceof Entity)) {
            return 0;
        }

        final int id = ((Entity) target).getId();
        if (owner.getPriorityUnitTargets().contains(id)) {
            return PRIORITY_TARGET_UTILITY;
        }
        return 0;
    }

    protected double calcCivilianTargetDisutility(final Targetable target) {
        if (!(target instanceof Entity)) {
            return 0;
        }
        final Entity entity = (Entity) target;
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

    protected double calcCommandUtility(final Targetable target) {
        if (!(target instanceof Entity)) {
            return 0;
        }

        final Entity entity = (Entity) target;
        if (isCommander(entity)) {
            return COMMANDER_UTILITY;
        } else if (isSubCommander(entity)) {
            return SUB_COMMANDER_UTILITY;
        }
        return 0;
    }

    protected boolean isCommander(final Entity entity) {
        if (owner.getFireControlState().commanderCached(entity)) {
            return owner.getFireControlState().isCommander(entity);
        }

        owner.getFireControlState().setCommander(entity,
                entity.isCommander() || entity.hasC3M() || entity.hasC3i() || entity.hasC3MM() ||
                (owner.getHighestEnemyInitiativeId() == entity.getId()));

        return owner.getFireControlState().isCommander(entity);
    }

    protected boolean isSubCommander(final Entity entity) {
        if (owner.getFireControlState().subCommanderCached(entity)) {
            return owner.getFireControlState().isSubCommander(entity);
        }

        final int initBonus = entity.getHQIniBonus() + entity.getQuirkIniBonus();
        owner.getFireControlState().setSubCommander(entity,
                entity.hasC3() || entity.hasTAG() || entity.hasBoostedC3() || entity.hasNovaCEWS() ||
               entity.isUsingSearchlight() || entity.hasBAP() || entity.hasActiveECM() || entity.hasActiveECCM() ||
               entity.hasQuirk(OptionsConstants.QUIRK_POS_IMPROVED_SENSORS) || entity.hasEiCockpit() ||
               (0 < initBonus));

        return owner.getFireControlState().isSubCommander(entity);
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
    double calcDamageAllocationUtility(final Targetable target,
                                       final double expectedDamage) {

        final double existingDamage = owner.getDamageAlreadyAssigned(target);
        final int targetHP = Compute.getTargetTotalHP(owner.getGame(), target);
        final double damageFraction = (existingDamage + expectedDamage) / ((double) targetHP);
        final double previousDamageFraction = existingDamage / ((double) targetHP);

        // Do not shoot at units we already expect to deal more than their total HP of damage to!
        if (1.0 <= previousDamageFraction) {
            return 100;

            // In cases that are not generally overkill (less than 50% of the target's total HP in
            // damage), target as normal (don't want to spread damage in these cases).
            // Also want to disregard damage allocation weighting if the target is a building or
            // infantry/BA (as they don't die until you do 100% damage to them normally).
        } else if ((damageFraction < 0.5)
                || (target.getTargetType() == Targetable.TYPE_BUILDING)
                   || (target.getTargetType() == Targetable.TYPE_HEX_CLEAR)
                   || (owner.getGame().getEntity(target.getId()) instanceof Infantry)) {
            return 0;
        }

        // In the remaining case, namely 0.5 <= damage, return the fraction of target HP dealt as
        // the penalty scaling factor (multiplied by the weight value to produce a penalty).
        return damageFraction;
    }

    /**
     * Calculates the potential damage that the target could theoretically deliver as a measure of
     * its potential "threat" to any allied unit on the board, thus prioritizing highly damaging
     * enemies over less damaging ones.
     * For now, this works by simply getting the max damage of the target at range = 1 while
     * ignoring to-hit, heat, etc.
     */
    private double calcTargetPotentialDamage(final Targetable target) {
        if (!(target instanceof Entity)) {
            return 0;
        }
        final Entity entity = (Entity) target;
        return getMaxDamageAtRange(entity, 1, false, false);
    }

    /**
     * Calculates the logarithmic scaling factor for target damage potential in
     * the utility equation, using the target's potential damage, the weight
     * value TARGET_POTENTIAL_DAMAGE_UTILITY, and Princess's self-preservation
     * value. This is mostly here to not clutter up the utility calculation
     * method with all this extra math.
     */
    protected double calcTargetPotentialDamageMultiplier(final Targetable target) {
        final double target_damage = calcTargetPotentialDamage(target);
        if (0.0 == target_damage) { // Do not calculate for zero damage units.
            return 1.0;
        }
        final double self_preservation = owner.getBehaviorSettings().getSelfPreservationValue();
        final double max_self_preservation = owner.getBehaviorSettings()
                                                  .getSelfPreservationValue(10); // the preservation value of the highest index, i.e. the max value.
        final double preservation_scaling_factor = max_self_preservation / self_preservation; // Because the variance in log value for large numbers is smaller, we need to make a big self-preservation value become a small multiplicative factor, and vice versa.
        return Math.log10(TARGET_POTENTIAL_DAMAGE_UTILITY * preservation_scaling_factor * target_damage + 10); // Add 10 to make the multiplier scale from 1 upwards(1 being a target that does 0 damage)).
    }

    /**
     * calculates the 'utility' of a physical action.
     *
     * @param physicalInfo The {@link PhysicalInfo} to be calculated.
     */
    void calculateUtility(final PhysicalInfo physicalInfo) {
        // If we can't hit, there's no point.
        if (0.0 >= physicalInfo.getProbabilityToHit()) {
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
     * @param game The current {@link Game}
     * @param guessToHit
     *            Set TRUE to estimate the odds to hit rather than doing the
     *            full calculation.
     * @return The resulting {@link WeaponFireInfo}.
     */
    WeaponFireInfo buildWeaponFireInfo(final Entity shooter,
                                       final EntityState shooterState,
                                       final Targetable target,
                                       final EntityState targetState,
                                       final Mounted weapon,
                                       final Game game,
                                       final boolean guessToHit) {
        return new WeaponFireInfo(shooter, shooterState, target, targetState,
                weapon, game, guessToHit, owner);
    }

    /**
     * Creates a new {@link WeaponFireInfo} object containing data about firing the given weapon at the given target.
     *
     * @param shooter The unit doing the shooting.
     * @param flightPath The path the unit flies over this turn.
     * @param target The target being fired on.
     * @param targetState The current state of the target.
     * @param weapon The weapon being fired.
     * @param game The current {@link Game}
     * @param assumeUnderFlightPath Set TRUE to assume the target is under the flight path and avoid
     *                             doing the full calculation.
     * @param guessToHit Set TRUE to estimate the odds to hit rather than doing the full calculation.
     * @return The resulting {@link WeaponFireInfo}.
     */
    WeaponFireInfo buildWeaponFireInfo(final Entity shooter,
                                       final MovePath flightPath,
                                       final Targetable target,
                                       final EntityState targetState,
                                       final Mounted weapon,
                                       final Game game,
                                       final boolean assumeUnderFlightPath,
                                       final boolean guessToHit) {
        return new WeaponFireInfo(shooter, flightPath, target, targetState,
                weapon, game, assumeUnderFlightPath, guessToHit, owner, null);
    }

    /**
     * Creates a new {@link WeaponFireInfo} object containing data about firing the given weapon at the given target.
     *
     * @param shooter The unit doing the shooting.
     * @param flightPath The path the unit flies over this turn.
     * @param target The target being fired on.
     * @param targetState The current state of the target.
     * @param weapon The weapon being fired.
     * @param game The current {@link Game}
     * @param assumeUnderFlightPath Set TRUE to assume the target is under the flight path and avoid
     *                             doing the full calculation.
     * @param guessToHit Set TRUE to estimate the odds to hit rather than doing the full calculation.
     * @param bombPayload The bomb payload, as described in WeaponAttackAction.setBombPayload
     * @return The resulting {@link WeaponFireInfo}.
     */
    private WeaponFireInfo buildWeaponFireInfo(final Entity shooter,
                                               final MovePath flightPath,
                                               final Targetable target,
                                               @SuppressWarnings("SameParameterValue") final EntityState targetState,
                                               final Mounted weapon,
                                               final Game game,
                                               final boolean assumeUnderFlightPath,
                                               final boolean guessToHit,
                                               final HashMap<String, int[]> bombPayloads) {
        return new WeaponFireInfo(shooter, flightPath, target, targetState,
                weapon, game, assumeUnderFlightPath, guessToHit, owner, bombPayloads);
    }

    /**
     * Creates a new {@link WeaponFireInfo} object containing data about firing the given weapon at the given target.
     *
     * @param shooter The unit doing the shooting.
     * @param target The target being fired on.
     * @param weapon The weapon being fired.
     * @param game The current {@link Game}
     * @param guessToHit Set TRUE to estimate the odds to hit rather than doing the full calculation.
     * @return The resulting {@link WeaponFireInfo}.
     */
    WeaponFireInfo buildWeaponFireInfo(final Entity shooter,
                                       final Targetable target,
                                       final Mounted weapon,
                                       final Game game,
                                       final boolean guessToHit) {
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
     * @param game The current {@link Game}
     * @return The {@link FiringPlan} containing all weapons to be fired.
     */
    FiringPlan guessFullFiringPlan(final Entity shooter,
                                   @Nullable EntityState shooterState,
                                   final Targetable target,
                                   @Nullable EntityState targetState,
                                   final Game game) {
        if (null == shooterState) {
            shooterState = new EntityState(shooter);
        }
        if (null == targetState) {
            targetState = new EntityState(target);
        }

        final FiringPlan myPlan = new FiringPlan(target);

        // Shooting isn't possible if one of us isn't on the board.
        if ((null == shooter.getPosition()) || shooter.isOffBoard() ||
            !game.getBoard().contains(shooter.getPosition())) {
            LogManager.getLogger().error("Shooter's position is NULL/Off Board!");
            return myPlan;
        }
        if ((null == target.getPosition()) || target.isOffBoard() || !game.getBoard().contains(target.getPosition())) {
            LogManager.getLogger().error("Target's position is NULL/Off Board!");
            return myPlan;
        }

        // cycle through my weapons
        for (final Mounted weapon : shooter.getWeaponList()) {
            // respect restriction on manual AMS firing.
            if (!game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_MANUAL_AMS) &&
                    weapon.getType().hasFlag(WeaponType.F_AMS)) {
                continue;
            }

            final WeaponFireInfo shoot = buildWeaponFireInfo(shooter,
                                                             shooterState,
                                                             target,
                                                             targetState,
                                                             weapon,
                                                             game,
                                                             true);

            if (0 < shoot.getProbabilityToHit()) {
                myPlan.add(shoot);
            }
        }

        // Rank how useful this plan is.
        calculateUtility(myPlan, calcHeatTolerance(shooter, null), shooterState.isAero());

        // if we're in a position to drop bombs because we're an aircraft on a ground map, then
        // the "alpha strike" may be a bombing plan.
        if (shooter.isAirborneAeroOnGroundMap()) {
            final FiringPlan bombingPlan = this.getDiveBombPlan(shooter, null, target, game, shooter.passedOver(target), true);
            calculateUtility(bombingPlan, Entity.DOES_NOT_TRACK_HEAT, true); // bomb drops never cause heat

            if (bombingPlan.getUtility() > myPlan.getUtility()) {
                return bombingPlan;
            }
        }

        return myPlan;
    }

    /**
     * Creates a firing plan that fires all weapons with nonzero to hit value in a air to ground strike
     *
     * @param shooter The unit doing the shooting.
     * @param target The unit being fired on.
     * @param targetState The current state of the target.
     * @param flightPath The path the shooter is flying over.
     * @param game The current {@link Game}
     * @param assumeUnderFlightPath Set TRUE to automatically assume the target will be under the
     *                              flight path rather than going through the full calculation.
     * @return The {@link FiringPlan} containing all weapons to be fired.
     */
    FiringPlan guessFullAirToGroundPlan(final Entity shooter,
                                        final Targetable target,
                                        @Nullable EntityState targetState,
                                        final MovePath flightPath,
                                        final Game game,
                                        final boolean assumeUnderFlightPath) {
        if (null == targetState) {
            targetState = new EntityState(target);
        }

        // Must fly over the target to hit it.
        if (!assumeUnderFlightPath && !isTargetUnderFlightPath(flightPath, targetState)) {
            return new FiringPlan(target);
        }

        final FiringPlan myPlan = new FiringPlan(target);

        // Shooting isn't possible if one of us isn't on the board.
        if ((null == shooter.getPosition()) || shooter.isOffBoard() ||
            !game.getBoard().contains(shooter.getPosition())) {
            LogManager.getLogger().error("Shooter's position is NULL/Off Board!");
            return myPlan;
        }

        if ((null == target.getPosition()) || target.isOffBoard() || !game.getBoard().contains(target.getPosition())) {
            LogManager.getLogger().error("Target's position is NULL/Off Board!");
            return myPlan;
        }

        // if we have no bombs on board, we can't attack from down here
        if (AeroGroundPathFinder.NAP_OF_THE_EARTH >= flightPath.getFinalAltitude()
                && shooter.getBombs(BombType.F_GROUND_BOMB).isEmpty()) {
            LogManager.getLogger().error("Shooter will crash if striking at altitude 1!");
            return myPlan;
        }

        if (AeroGroundPathFinder.OPTIMAL_STRIKE_ALTITUDE < flightPath.getFinalAltitude()) {
            LogManager.getLogger().error("Shooter's altitude is too high!");
            return myPlan;
        }

        // cycle through my weapons
        for (final Mounted weapon : shooter.getWeaponList()) {
            // bombing attacks have to be carried out separately from other weapon attacks, so we handle them in a special case
            if (weapon.isGroundBomb()) {
                continue;
            }

            final WeaponFireInfo shoot = buildWeaponFireInfo(shooter,
                                                             flightPath,
                                                             target,
                                                             targetState,
                                                             weapon,
                                                             game,
                                                             true,
                                                             true);

            // for now, just fire weapons that will do damage until we get to heat capacity
            if (0 < shoot.getProbabilityToHit() &&
                myPlan.getHeat() + shoot.getHeat() + shooter.getHeat() <= shooter.getHeatCapacity() &&
                0 < shoot.getExpectedDamage()) {
                myPlan.add(shoot);
            }
        }

        // if we are here, we have already confirmed the target is under the flight path and are guessing
        final FiringPlan bombPlan = getDiveBombPlan(shooter, flightPath, target, game, true, true);
        calculateUtility(bombPlan, Entity.DOES_NOT_TRACK_HEAT, shooter.isAero()); // bombs don't generate heat so don't bother with this calculation

        // Rank how useful this plan is.
        calculateUtility(myPlan, calcHeatTolerance(shooter, null), shooter.isAero());

        if (myPlan.getUtility() >= bombPlan.getUtility()) {
            return myPlan;
        } else {
            return bombPlan;
        }
    }

    /**
     * Creates a firing plan that fires dive bombs, dropping all bombs on the given target
     *
     * @param shooter               The unit doing the shooting.
     * @param target                The unit being fired on.
     * @param game                  The current {@link Game}
     * @param passedOverTarget      Set TRUE to automatically assume the target will be under the flight path rather
     *                              than going through the full calculation.
     * @param guess                 Whether we're just thinking about this firing plan or about to
     * @return The {@link FiringPlan} containing all bombs on target, if the shooter is capable of dropping bombs.
     */
    private FiringPlan getDiveBombPlan(final Entity shooter,
                                      final MovePath flighPath,
                                      final Targetable target,
                                       final Game game,
                                      final boolean passedOverTarget,
                                      final boolean guess) {
        final FiringPlan diveBombPlan = new FiringPlan(target);
        final HexTarget hexToBomb = new HexTarget(target.getPosition(),
                shooter.isAero() ? Targetable.TYPE_HEX_AERO_BOMB : Targetable.TYPE_HEX_BOMB);

        // things that cause us to avoid calculating a bomb plan:
        // not having any bombs (in the first place)
        final Iterator<Mounted> weaponIter = shooter.getWeapons();
        if (null == weaponIter) {
            return diveBombPlan;
        }

        // not having any bombs (due to expenditure/damage)
        if (shooter.getBombs(BombType.F_GROUND_BOMB).isEmpty()) {
            return diveBombPlan;
        }

        while (weaponIter.hasNext()) {
            final Mounted weapon = weaponIter.next();
            if (weapon.getType().hasFlag(WeaponType.F_DIVE_BOMB)) {
                final HashMap<String, int[]> bombPayloads = new HashMap<String, int[]>();
                bombPayloads.put("internal", new int[BombType.B_NUM]);
                bombPayloads.put("external", new int[BombType.B_NUM]);

                // load up all droppable bombs, yeah baby! Mix thunder bombs and infernos 'cause why the hell not.
                // seriously, though, TODO: more intelligent bomb drops
                for (final Mounted bomb : shooter.getBombs(BombType.F_GROUND_BOMB)) {
                    int bType = ((BombType) bomb.getType()).getBombType();
                    if (bomb.isInternalBomb()) {
                        // Can only drop 6 internal bombs in one turn.
                        if (bombPayloads.get("internal")[bType] < 6) {
                            bombPayloads.get("internal")[bType]++;
                        }
                    } else {
                        bombPayloads.get("external")[bType]++;
                    }
                }

                final WeaponFireInfo diveBomb = buildWeaponFireInfo(shooter,
                                                                    flighPath,
                                                                    hexToBomb,
                                                                    null,
                                                                    weapon,
                                                                    game,
                                                                    passedOverTarget,
                                                                    guess,
                                                                    bombPayloads);
                diveBombPlan.add(diveBomb);
            }
        }

        return diveBombPlan;
    }

    /**
     * Creates a firing plan that fires all weapons with nonzero to hit value at a target ignoring heat, and using
     * actual game rules from different states
     *
     * @param shooter The unit doing the shooting.
     * @param target The unit being fired on.
     * @param game The current {@link Game}
     * @return The {@link FiringPlan} containing all weapons to be fired.
     */
    FiringPlan getFullFiringPlan(final Entity shooter,
                                 final Targetable target,
                                 final Map<Mounted, Double> ammoConservation,
                                 final Game game) {
        final NumberFormat DECF = new DecimalFormat("0.000");

        final FiringPlan myPlan = new FiringPlan(target);

        // Shooting isn't possible if one of us isn't on the board.
        if ((null == shooter.getPosition()) || shooter.isOffBoard() ||
            !game.getBoard().contains(shooter.getPosition())) {
            LogManager.getLogger().error("Shooter's position is NULL/Off Board!");
            return myPlan;
        }
        if ((null == target.getPosition()) || target.isOffBoard() || !game.getBoard().contains(target.getPosition())) {
            LogManager.getLogger().error("Target's position is NULL/Off Board!");
            return myPlan;
        }

        // cycle through my weapons
        for (final Mounted weapon : shooter.getWeaponList()) {
            // respect restriction on manual AMS firing.
            if (!game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_MANUAL_AMS) &&
                    weapon.getType().hasFlag(WeaponType.F_AMS)) {
                continue;
            }

            final double toHitThreshold = ammoConservation.get(weapon);
            WeaponFireInfo shoot = buildWeaponFireInfo(shooter, target, weapon, game, false);

            // if we're below the threshold, try switching missile modes
            if (shoot.getProbabilityToHit() <= toHitThreshold) {

                int updatedMissileMode = switchMissileMode(weapon);

                if (updatedMissileMode > -1) {
                    shoot = buildWeaponFireInfo(shooter, target, weapon, game, false);
                    shoot.setUpdatedFiringMode(updatedMissileMode);
                }
            }

            if ((shoot.getProbabilityToHit() > toHitThreshold)) {
                myPlan.add(shoot);
                continue;
            }

            LogManager.getLogger().debug("\nTo Hit Chance (" + DECF.format(shoot.getProbabilityToHit())
                      + ") for " + weapon.getName() +
                      " is less than threshold (" + DECF.format(toHitThreshold) + ")");
        }

        // Rank how useful this plan is.
        calculateUtility(myPlan, calcHeatTolerance(shooter, null), shooter.isAero());

        if (shooter.isAero()) {
            final FiringPlan bombingPlan = this.getDiveBombPlan(shooter, null, target, game, shooter.passedOver(target), false);
            calculateUtility(bombingPlan, Entity.DOES_NOT_TRACK_HEAT, true); // bomb drops never cause heat

            // if the bombing plan actually involves doing something
            if (!bombingPlan.isEmpty() &&
                    (bombingPlan.getUtility() > myPlan.getUtility())) {
                return bombingPlan;
            }
        }

        return myPlan;
    }

    protected int calcHeatTolerance(final Entity entity,
                                  @Nullable Boolean isAero) {

        // If the unit doesn't track heat, we won't worry about it.
        if (Entity.DOES_NOT_TRACK_HEAT == entity.getHeatCapacity()) {
            return Entity.DOES_NOT_TRACK_HEAT;
        }

        int baseTolerance = entity.getHeatCapacity() - entity.getHeat();

        // if we've got a combat computer, we get an automatic
        if (entity.hasQuirk(OptionsConstants.QUIRK_POS_COMBAT_COMPUTER)) {
            baseTolerance += 4;
        }

        if (null == isAero) {
            isAero = entity.isAero();
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
     * @return An array of all the resulting firing plans.
     */
    FiringPlan[] calcFiringPlansUnderHeat(final Entity shooter,
                                          final FiringPlan alphaStrike) {

        // can't be lower than zero heat
        int maxHeat = alphaStrike.getHeat();
        if (0 > maxHeat) {
            maxHeat = 0;
        }

        final Targetable target = alphaStrike.getTarget();

        final boolean isAero = shooter.isAero();
        final int heatTolerance = calcHeatTolerance(shooter, isAero);

        // How many plans do I need to compute?
        final FiringPlan[] bestPlans;
        if (shooter instanceof Infantry) {
            bestPlans = new FiringPlan[maxHeat + 4];
        } else {
            bestPlans = new FiringPlan[maxHeat + 1];
        }

        // First plan is a plan that fires only heatless weapons.
        // The remaining plans will build at least some heat.
        // we include arm flip information into the regular heat plans, but infantry don't flip arms so we don't bother.
        bestPlans[0] = new FiringPlan(target, alphaStrike.getFlipArms());
        final FiringPlan nonZeroHeatOptions = new FiringPlan(target);
        final FiringPlan swarmAttack = new FiringPlan(target);
        final FiringPlan legAttack = new FiringPlan(target);
        final FiringPlan fieldGuns = new FiringPlan(target);
        for (final WeaponFireInfo weaponFireInfo : alphaStrike) {

            //Leg and swarm attacks can't be mixed with any other attacks, so we have to consider each of those separately.
            if (shooter instanceof Infantry) {
                if ((weaponFireInfo.getWeapon().getType()).getInternalName().equals(Infantry.LEG_ATTACK)) {
                    legAttack.add(weaponFireInfo);
                    continue;
                } else if ((weaponFireInfo.getWeapon().getType()).getInternalName().equals(Infantry.SWARM_MEK)) {
                    swarmAttack.add(weaponFireInfo);
                    continue;
                } else if ((weaponFireInfo.getWeapon().getType()) instanceof StopSwarmAttack) {
                    // We probably shouldn't consider stopping swarm attacks, since Princess isn't
                    // smart enough to recognize the rare situations when this is a good idea
                    // (e.g. planning to put lots of allied fire on the swarm target next turn,
                    // target is likely to explode and ammo explosion splash damage is on, etc).
                    continue;
                } else if (!(shooter instanceof BattleArmor)
                        && (Infantry.LOC_FIELD_GUNS == weaponFireInfo.getWeapon().getLocation())) {
                    fieldGuns.add(weaponFireInfo);
                    continue;
                }
            }
            if (0 == weaponFireInfo.getHeat()) {
                bestPlans[0].add(weaponFireInfo);
            } else {
                nonZeroHeatOptions.add(weaponFireInfo);
            }
        }
        calculateUtility(bestPlans[0], heatTolerance, isAero);

        if (shooter instanceof Infantry) {
            calculateUtility(swarmAttack, heatTolerance, isAero);
            calculateUtility(legAttack, heatTolerance, isAero);
            calculateUtility(fieldGuns, heatTolerance, isAero);
            // Add these plans to the end of the list.
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

            for (final WeaponFireInfo weaponFireInfo : nonZeroHeatOptions) {

                final int leftoverHeatCapacity = heatLevel - weaponFireInfo.getHeat();

                // If this attack produces heat and is not already included in the plan, check its utility.
                if ((0 <= leftoverHeatCapacity) &&
                    !bestPlans[leftoverHeatCapacity].containsWeapon(weaponFireInfo.getWeapon())) {

                    // make sure to pass along arm flip state from the alpha strike, if any
                    final FiringPlan testPlan = new FiringPlan(target, alphaStrike.getFlipArms());
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

        // if we are an aero blasting away at ground targets, another good option for a heatless plan is to bomb the crap out of the enemy
        // bombs cannot be mixed with other attack types, so we calculate it separately and overwrite the 0-heat plan if it's better
        // currently, this will probably result in the aero blowing its bomb load as soon as it passes over an enemy
        // dropping everything it has, including specialized munitions such as thunder bombs and infernos
        if (shooter.isAirborne() && !shooter.getBombs(BombType.F_GROUND_BOMB).isEmpty()) {
            final FiringPlan diveBombPlan = this.getDiveBombPlan(shooter, null, target,
                    shooter.getGame(), shooter.passedOver(target), false);

            calculateUtility(diveBombPlan, Entity.DOES_NOT_TRACK_HEAT, true);
            if (diveBombPlan.getUtility() > bestPlans[0].getUtility()) {
                bestPlans[0] = diveBombPlan;
            }
        }

        return bestPlans;
    }

    /**
     * Gets the 'best' firing plan, using heat as a disutility. No twisting is
     * done
     *
     * @param shooter The unit doing the shooting.
     * @param target The unit being shot at.
     * @param game The current {@link Game}
     * @return the 'best' firing plan, using heat as a disutility.
     */
    FiringPlan getBestFiringPlan(final Entity shooter,
                                 final Targetable target,
                                 final Game game,
                                 final Map<Mounted, Double> ammoConservation) {

        // Start with an alpha strike.
        FiringPlan alphaStrike = getFullFiringPlan(shooter, target,
                                                    ammoConservation, game);

        if (shooter.canFlipArms()) {
            shooter.setArmsFlipped(true, false);
            FiringPlan betaStrike = getFullFiringPlan(shooter, target, ammoConservation, game);
            betaStrike.setFlipArms(true);
            if (betaStrike.getUtility() > alphaStrike.getUtility()) {
                alphaStrike = betaStrike;
            }

            shooter.setArmsFlipped(false, false);
        }

        // Although they don't track heat, infantry/BA do need to make tradeoffs
        // between firing different weapons, because swarm/leg attacks are
        // mutually exclusive with normal firing, so we treat them similarly to
        // heat-tracking units.

        // conventional fighters can drop bombs
        if (Entity.DOES_NOT_TRACK_HEAT == shooter.getHeatCapacity()
                && ((shooter.getEntityType() & Entity.ETYPE_INFANTRY) == 0)) {
            return alphaStrike; // No need to worry about heat if the unit doesn't track it.
        }

        // Get all the best plans that generate less heat than an alpha strike.
        final FiringPlan[] allPlans = calcFiringPlansUnderHeat(shooter, alphaStrike);

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
     * @param game The current {@link Game}
     * @return the 'best' firing plan under a certain heat.
     */
    protected FiringPlan guessBestFiringPlanUnderHeat(final Entity shooter,
                                                    @Nullable final EntityState shooterState,
                                                    final Targetable target,
                                                    @Nullable final EntityState targetState,
                                                    int maxHeat,
                                                    final Game game) {

        // can't have less than zero heat
        if (0 > maxHeat) {
            maxHeat = 0;
        }

        // Start with an alpha strike. If it falls under our heat limit, use it.
        FiringPlan alphaStrike = guessFullFiringPlan(shooter, shooterState,
                                                       target, targetState, game);

        if (shooter.canFlipArms()) {
            shooter.setArmsFlipped(true, false);
            FiringPlan betaStrike = guessFullFiringPlan(shooter, shooterState,
                                                        target, targetState, game);
            betaStrike.setFlipArms(true);
            if (betaStrike.getUtility() > alphaStrike.getUtility()) {
                alphaStrike = betaStrike;
            }

            shooter.setArmsFlipped(false, false);
        }

        // Infantry and BA may have alternative options, so we need to consider
        // different firing options.
        if (alphaStrike.getHeat() <= maxHeat && !(shooter instanceof Infantry)) {
            return alphaStrike;
        }

        // Get the best firing plan that falls under our heat limit.
        // Now emulates the logic from getBestFiringPlanUnderHeat, rather than sorting the firing plans low to high then picking the lowest one
        final FiringPlan[] heatPlans = calcFiringPlansUnderHeat(shooter, alphaStrike);
        FiringPlan bestPlan = new FiringPlan(target);

        for (final FiringPlan firingPlan : heatPlans) {
            if ((bestPlan.getUtility() < firingPlan.getUtility())) {
                bestPlan = firingPlan;
            }
        }
        return bestPlan;
    }

    private FiringPlan getBestFiringPlanUnderHeat(final Targetable target,
                                                  final Entity shooter,
                                                  final FiringPlan[] allPlans) {

        // Determine the best plan taking into account our heat tolerance.
        FiringPlan bestPlan = new FiringPlan(target);
        final boolean isAero = shooter.isAero();
        final int heatTolerance = calcHeatTolerance(shooter, isAero);
        calculateUtility(bestPlan, heatTolerance, isAero);
        for (final FiringPlan firingPlan : allPlans) {
            calculateUtility(firingPlan, heatTolerance, isAero);
            if ((bestPlan.getUtility() < firingPlan.getUtility())) {
                bestPlan = firingPlan;
            }
        }
        return bestPlan;
    }

    /**
     * Figures out the best firing plan
     *
     * @param params - the appropriate firing plan calculation parameters
     * @return the 'best' firing plan - uses heat as disutility and includes the possibility of twisting
     */
    FiringPlan determineBestFiringPlan(final FiringPlanCalculationParameters params) {
        // unpack parameters for easier reference
        final Entity shooter = params.getShooter();
        final Targetable target = params.getTarget();
        final EntityState shooterState = params.getShooterState();
        final EntityState targetState = params.getTargetState();
        final int maxHeat = params.getMaxHeat();
        final Map<Mounted, Double> ammoConservation = params.getAmmoConservation();

        // Get the best plan without any twists.
        FiringPlan noTwistPlan = null;

        switch (params.getCalculationType()) {
            case GET:
                noTwistPlan = getBestFiringPlan(shooter, target, owner.getGame(), ammoConservation);
                break;
            case GUESS:
                noTwistPlan = guessBestFiringPlanUnderHeat(shooter,
                                                           shooterState,
                                                           target,
                                                           targetState,
                                                           maxHeat,
                                                           owner.getGame());
                break;
        }

        // If we can't change facing, we're done.
        if (!params.getShooter().canChangeSecondaryFacing()) {
            return noTwistPlan;
        }

        // Keep track of our original facing so we can go back to it.
        final int originalFacing = shooter.getSecondaryFacing();

        final List<Integer> validFacingChanges = getValidFacingChanges(shooter);

        // Now, we loop through all possible facings. If one facing produces a better plan
        // than what we currently have as the best plan then use that. Start with "no twist" as default.
        FiringPlan bestFiringPlan = noTwistPlan;
        for (final int currentTwist : validFacingChanges) {
            shooter.setSecondaryFacing(correctFacing(originalFacing + currentTwist), false);

            FiringPlan twistPlan = null;
            switch (params.getCalculationType()) {
                case GET:
                    twistPlan = getBestFiringPlan(shooter, target, owner.getGame(), ammoConservation);
                    break;
                case GUESS:
                    twistPlan = guessBestFiringPlanUnderHeat(shooter,
                                                             shooterState,
                                                             target,
                                                             targetState,
                                                             maxHeat,
                                                             owner.getGame());
            }
            twistPlan.setTwist(currentTwist);

            if (twistPlan.getUtility() > bestFiringPlan.getUtility()) {
                bestFiringPlan = twistPlan;
            }
        }

        // Back to where we started.
        shooter.setSecondaryFacing(originalFacing, false);

        return bestFiringPlan;
    }

    /**
     * Determines if the given entity can use indirect fire as in LRMs.
     */
    public boolean entityCanIndirectFireMissile(FireControlState fireControlState, Entity shooter) {
        // cache the results of our computation
        if (fireControlState.getEntityIDFStates().containsKey(shooter.getId())) {
            return fireControlState.getEntityIDFStates().get(shooter.getId());
        }

        // airborne aerospace units cannot use indirect fire
        if (shooter.isAirborne()) {
            fireControlState.getEntityIDFStates().put(shooter.getId(), false);
            return false;
        }

        for (Mounted weapon : shooter.getWeaponList()) {
            if (weapon.hasModeType(Weapon.MODE_MISSILE_INDIRECT) || weapon.hasModeType(Weapon.MODE_INDIRECT_HEAT)) {
                fireControlState.getEntityIDFStates().put(shooter.getId(), true);
                return true;
            }
        }

        fireControlState.getEntityIDFStates().put(shooter.getId(), false);
        return false;
    }

    /**
     * Determines if the given entity (potentially employing a given firing plan)
     * can/should spot. If yes, then return a spot action.
     */
    public SpotAction getSpotAction(FiringPlan plan, Entity spotter, FireControlState fireControlState) {
        // logic applies as follows:
        // if I am disqualified from spotting, don't spot
        // disqualifiers are:
        //     legally can't spot
        //     am firing and don't have a command console to mitigate the spotting penalty
        // otherwise, attempt to spot the closest enemy
        if (spotter.isSpotting() || !spotter.canSpot() || spotter.isNarcedBy(INarcPod.HAYWIRE) ||
                (plan != null) && (plan.getExpectedDamage() > 0) &&
                !spotter.getCrew().hasActiveCommandConsole()) {
            return null;
        }

        List<Targetable> enemyTargets = getAllTargetableEnemyEntities(
                spotter.getOwner(), spotter.getGame(), fireControlState);
        List<Targetable> closestTargets = new ArrayList<>();
        int shortestDistance = Integer.MAX_VALUE;

        // loop through all enemy targets, pick a random one out of the closest.
        // future revision: pick one that's the least evasive
        for (Targetable target : enemyTargets) {
            // don't spot aerospace units
            if (target.isAirborne()) {
                continue;
            }

            // don't spot sensor returns
            if ((target.getTargetType() == Targetable.TYPE_ENTITY) &&
                ((Entity) target).isSensorReturn(spotter.getOwner())) {
                continue;
            }

            LosEffects effects = LosEffects.calculateLOS(spotter.getGame(), spotter, target);

            // if we're in LOS
            if (effects.canSee()) {
                int targetDistance = spotter.getPosition().distance(target.getPosition());
                if (targetDistance < shortestDistance) {
                    shortestDistance = targetDistance;
                    closestTargets.clear();
                    closestTargets.add(target);
                } else if (targetDistance == shortestDistance) {
                    closestTargets.add(target);
                }
            }
        }

        // if we found one or more targets, pick at random from the closest ones.
        // otherwise, we still can't spot
        if (!closestTargets.isEmpty()) {
            Targetable target = closestTargets.get(Compute.randomInt(closestTargets.size()));
            return new SpotAction(spotter.getId(), target.getId());
        }

        return null;
    }

    /**
     * Gets all the entities that are potential targets
     *
     * @param shooter The unit doing the shooting.
     * @param game The current {@link Game}
     * @return A list of potential targets.
     */
    protected List<Targetable> getTargetableEnemyEntities(final Entity shooter,
                                                        final Game game,
                                                        final FireControlState fireControlState) {
        final List<Targetable> targetableEnemyList = new ArrayList<>();

        boolean shooterHasIDF = entityCanIndirectFireMissile(fireControlState, shooter);

        // Go through every enemy unit
        for (final Entity entity : owner.getEnemyEntities()) {

            // If they are my enemy and we can either see them or have IDF capability
            if (entity.isTargetable()) {
                final LosEffects effects = LosEffects.calculateLOS(game, shooter, entity);

                // if we're in LOS or we have IDF capability
                if (effects.canSee() || shooterHasIDF) {
                    targetableEnemyList.add(entity);
                }
            }
        }

        // Add in potential building targets and the like.
        targetableEnemyList.addAll(fireControlState.getAdditionalTargets());

        return targetableEnemyList;
    }

    /**
     * Variation on getTargetableEnemyEntities.
     * Returns all possible enemy targets, regardless of LOS status.
     * @param player The player from whose perspective enemies are determined.
     * @param game The current {@link Game}
     * @return A list of potential targets.
     */
    static List<Targetable> getAllTargetableEnemyEntities(final Player player, final Game game,
                                                          final FireControlState fireControlState) {
        final List<Targetable> targetableEnemyList = new ArrayList<>();

        // Go through every unit in the game.
        for (final Entity entity : game.getEntitiesVector()) {
            // If they are my enemy and on the board, they're a target.
            if (entity.getOwner().isEnemyOf(player)
                && (null != entity.getPosition())
                && !entity.isOffBoard()
                && entity.isTargetable()
                && (null != entity.getCrew()) && !entity.getCrew().isDead()) {
                targetableEnemyList.add(entity);
            }
        }

        // Add in potential building targets and the like.
        targetableEnemyList.addAll(fireControlState.getAdditionalTargets());

        return targetableEnemyList;
    }

    /**
     * This is it. Calculate the 'best' possible firing plan for this entity.
     * Overload this function if you think you can do better.
     *
     * @param shooter The unit doing the shooting.
     * @param game The current {@link Game}
     * @return The best firing plan according to our calculations.
     */
    FiringPlan getBestFiringPlan(final Entity shooter,
                                 final IHonorUtil honorUtil,
                                 final Game game,
                                 final Map<Mounted, Double> ammoConservation) {
        FiringPlan bestPlan = null;

        // Get a list of potential targets.
        final List<Targetable> enemies = getTargetableEnemyEntities(shooter, game, owner.getFireControlState());

        // Loop through each enemy and find the best plan for attacking them.
        for (final Targetable enemy : enemies) {

            if (owner.getBehaviorSettings().getIgnoredUnitTargets().contains(enemy.getId())) {
                LogManager.getLogger().info(enemy.getDisplayName() + " is being explicitly ignored");
                continue;
            }

            final boolean priorityTarget = owner.getPriorityUnitTargets().contains(enemy.getId());

            // Skip retreating enemies so long as they haven't fired on me while retreating.
            final int playerId = (enemy instanceof Entity) ? ((Entity) enemy).getOwnerId() : -1;
            if (!priorityTarget && honorUtil.isEnemyBroken(enemy.getId(), playerId,
                                                           owner.getForcedWithdrawal())) {
                LogManager.getLogger().info(enemy.getDisplayName() + " is broken - ignoring");
                continue;
            }

            final FiringPlanCalculationParameters parameters =
                    new FiringPlanCalculationParameters.Builder().buildExact(shooter,
                                                                             enemy,
                                                                             ammoConservation);
            final FiringPlan plan = determineBestFiringPlan(parameters);

            LogManager.getLogger().info(shooter.getDisplayName() + " at " + enemy
                    .getDisplayName() + " - Best Firing Plan: " + plan.getDebugDescription(true));
            if ((null == bestPlan) || (plan.getUtility() > bestPlan.getUtility())) {
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
     * @param useExtremeRange Is the extreme range optional rule in effect?
     * @return The most damage done at that range.
     */
    // todo: cluster and other variable damage.
    public static double getMaxDamageAtRange(final Entity shooter,
                               final int range,
                               final boolean useExtremeRange,
                               final boolean useLOSRange) {
        double maxDamage = 0;

        // cycle through my weapons
        for (final Mounted weapon : shooter.getWeaponList()) {
            final WeaponType weaponType = (WeaponType) weapon.getType();
            final int bracket = RangeType.rangeBracket(range,
                                                       weaponType.getRanges(weapon),
                                                       useExtremeRange,
                                                       useLOSRange);
            // if the weapon has been disabled or is out of ammo, don't count it
            if (weapon.isCrippled()) {
                continue;
            }

            int weaponDamage = weaponType.getDamage();

            // just a ball park estimate of missile and/or other cluster damage
            // only a little over half of a cluster will generally hit
            // but some cluster munitions do more than 1 point of damage per individual hit
            // still better than just discounting them completely.
            if (weaponDamage == WeaponType.DAMAGE_BY_CLUSTERTABLE || weaponType.hasFlag(WeaponType.F_ARTILLERY)) {
                weaponDamage = weaponType.getRackSize();
            }

            if ((RangeType.RANGE_OUT != bracket) && (0 < weaponDamage)) {
                maxDamage += weaponDamage;
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
        while (0 > facing) {
            facing += 6;
        }
        if (5 < facing) {
            facing = facing % 6;
        }
        return facing;
    }

    /**
     * Makes sure ammo is loaded for each weapon
     */
    void loadAmmo(final Entity shooter,
                  final FiringPlan plan) {
        if (null == shooter) {
            return;
        }
        if (null == plan) {
            return;
        }

        // Loading ammo for all my weapons.
        for (final WeaponFireInfo info : plan) {
            final Mounted currentWeapon = info.getWeapon();
            if (null == currentWeapon) {
                continue;
            }
            final WeaponType weaponType = (WeaponType) currentWeapon.getType();

            // Skip weapons that don't use ammo.
            if (AmmoType.T_NA == weaponType.getAmmoType()) {
                continue;
            }

            final Mounted mountedAmmo = getPreferredAmmo(shooter, info.getTarget(), currentWeapon);
            // if we found preferred ammo but can't apply it to the weapon, log it and continue.
            if ((null != mountedAmmo) && !shooter.loadWeapon(currentWeapon, mountedAmmo)) {
                LogManager.getLogger().warn(shooter.getDisplayName() + " tried to load "
                          + currentWeapon.getName() + " with ammo " +
                          mountedAmmo.getDesc() + " but failed somehow.");
                continue;
            // if we didn't find preferred ammo after all, continue
            } else if (mountedAmmo == null) {
                continue;
            }
            final WeaponAttackAction action = info.getAction();
            action.setAmmoId(shooter.getEquipmentNum(mountedAmmo));
            action.setAmmoMunitionType(((AmmoType) mountedAmmo.getType()).getMunitionType());
            action.setAmmoCarrier(mountedAmmo.getEntity().getId());
            info.setAction(action);
            owner.sendAmmoChange(info.getShooter().getId(), shooter.getEquipmentNum(currentWeapon),
                                 shooter.getEquipmentNum(mountedAmmo));
        }
    }

    Mounted getClusterAmmo(final List<Mounted> ammoList,
                           final WeaponType weaponType,
                           final int range) {
        Mounted returnAmmo = null;
        Mounted mmlLrm = null;
        Mounted mmlSrm = null;

        for (final Mounted ammo : ammoList) {
            final AmmoType ammoType = (AmmoType) ammo.getType();
            if (ammoType.getMunitionType().contains(AmmoType.Munitions.M_CLUSTER)) {
                // MMLs have additional considerations.
                // There are no "cluster" missile munitions at this point in time.  Code is included in case
                // they are added to the game at some later date.
                if (!(weaponType instanceof MMLWeapon)) {
                    returnAmmo = ammo;
                    break;
                }
                if ((null == mmlLrm) && ammoType.hasFlag(AmmoType.F_MML_LRM)) {
                    mmlLrm = ammo;
                } else if (null == mmlSrm) {
                    mmlSrm = ammo;
                } else if (null != mmlLrm) {
                    break;
                }
            }
        }

        // MML ammo depends on range.
        if (weaponType instanceof MMLWeapon) {
            if (9 < range) { // Out of SRM range
                returnAmmo = mmlLrm;
            } else if (6 < range) { // SRM long range.
                returnAmmo = (null == mmlLrm ? mmlSrm : mmlLrm);
            } else {
                returnAmmo = (null == mmlSrm ? mmlLrm : mmlSrm);
            }
        }

        return returnAmmo;
    }

    Mounted getPreferredAmmo(final Entity shooter,
                             final Targetable target,
                             final Mounted weapon) {
        final StringBuilder msg = new StringBuilder("Getting ammo for ").append(weapon.getType().getShortName())
                                                                        .append(" firing at ")
                                                                        .append(target.getDisplayName
                        ());
        Entity targetEntity = null;
        Mounted preferredAmmo = null;
        WeaponType weaponType = (WeaponType) weapon.getType();

        try {
            boolean fireResistant = false;
            if (target instanceof Entity) {
                targetEntity = (Entity) target;
                final int armorType = targetEntity.getArmorType(0);
                if (targetEntity instanceof Mech) {
                    targetEntity.getArmorType(1);
                }
                if (EquipmentType.T_ARMOR_BA_FIRE_RESIST == armorType
                    || EquipmentType.T_ARMOR_HEAT_DISSIPATING == armorType) {
                    fireResistant = true;
                }
            }

            // Find the ammo that is valid for this weapon.
            final List<Mounted> ammo = shooter.getAmmo();
            final List<Mounted> validAmmo = new ArrayList<>();
            for (final Mounted a : ammo) {
                if (AmmoType.isAmmoValid(a, weaponType)
                        && AmmoType.canSwitchToAmmo(weapon, (AmmoType) a.getType())
                        && (!shooter.isLargeCraft()
                            || shooter.whichBay(shooter.getEquipmentNum(weapon)).ammoInBay(shooter.getEquipmentNum(a)))) {
                    validAmmo.add(a);
                }
            }

            // If no valid ammo was found, return nothing.
            if (validAmmo.isEmpty()) {
                return null;
            }
            msg.append("\n\tFound ").append(validAmmo.size()).append(" units of valid ammo.");

            final int range = shooter.getPosition().distance(target.getPosition());
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
                if (null != preferredAmmo) {
                    msg.append("Burn It Down!");
                    return preferredAmmo;
                }

                // Entity targets.
            } else if (null != targetEntity) {
                // Airborne targets
                if (targetEntity.isAirborne() || (targetEntity.isAirborneVTOLorWIGE())) {
                    msg.append("\n\tTarget is airborne... ");
                    preferredAmmo = getAntiAirAmmo(validAmmo, weaponType, range);
                    if (null != preferredAmmo) {
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
                    if (null != preferredAmmo) {
                        msg.append("We have ways of dealing with that.");
                        return preferredAmmo;
                    }
                }
                // PBI
                if (targetEntity instanceof Infantry) {
                    msg.append("\n\tTarget is infantry... ");
                    preferredAmmo = getAntiInfantryAmmo(validAmmo, weaponType, range);
                    if (null != preferredAmmo) {
                        msg.append("They squish nicely.");
                        return preferredAmmo;
                    }
                }
                // On his last legs
                if (Entity.DMG_HEAVY <= targetEntity.getDamageLevel()) {
                    msg.append("\n\tTarget is heavily damaged... ");
                    preferredAmmo = getClusterAmmo(validAmmo, weaponType, range);
                    if (null != preferredAmmo) {
                        msg.append("Let's find a soft spot.");
                        return preferredAmmo;
                    }
                }
                // He's running hot.
                if (9 <= targetEntity.getHeat() && !fireResistant) {
                    msg.append("\n\tTarget is at ").append(targetEntity.getHeat()).append(" heat... ");
                    preferredAmmo = getHeatAmmo(validAmmo, weaponType, range);
                    if (null != preferredAmmo) {
                        msg.append("Let's heat him up more.");
                        return preferredAmmo;
                    }
                }
                // Everything else.
                msg.append("\n\tTarget is a hard target... ");
                preferredAmmo = getHardTargetAmmo(validAmmo, weaponType, range);
                if (null != preferredAmmo) {
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
            msg.append("\n\tReturning: ").append(null == preferredAmmo ? "null" : preferredAmmo.getDesc());
            LogManager.getLogger().debug(msg.toString());
        }
    }

    Mounted getGeneralMmlAmmo(final List<Mounted> ammoList,
                              final int range) {
        final Mounted returnAmmo;

        // Get the LRM and SRM bins if we have them.
        Mounted mmlSrm = null;
        Mounted mmlLrm = null;
        for (final Mounted ammo : ammoList) {
            final AmmoType type = (AmmoType) ammo.getType();
            if ((null == mmlLrm) && type.hasFlag(AmmoType.F_MML_LRM)) {
                mmlLrm = ammo;
            } else if (null == mmlSrm) {
                mmlSrm = ammo;
            } else if (null != mmlLrm) {
                break;
            }
        }

        // Out of SRM range.
        if (9 < range) {
            returnAmmo = mmlLrm;

            // LRMs have better chance to hit if we have them.
        } else if (5 < range) {
            returnAmmo = (null == mmlLrm ? mmlSrm : mmlLrm);

            // If we only have LRMs left.
        } else if (null == mmlSrm) {
            returnAmmo = mmlLrm;

            // Left with SRMS.
        } else {
            returnAmmo = mmlSrm;
        }
        return returnAmmo;
    }

    Mounted getAtmAmmo(final List<Mounted> ammoList,
                       final int range,
                       final EntityState target,
                       final boolean fireResistant) {
        Mounted returnAmmo;

        // Get the Hi-Ex, Ex-Range and Standard ammo bins if we have them.
        Mounted heAmmo = null;
        Mounted erAmmo = null;
        Mounted stAmmo = null;
        Mounted infernoAmmo = null;
        for (final Mounted ammo : ammoList) {
            final AmmoType type = (AmmoType) ammo.getType();
            if ((null == heAmmo) && (type.getMunitionType().contains(AmmoType.Munitions.M_HIGH_EXPLOSIVE))) {
                heAmmo = ammo;
            } else if ((null == erAmmo) && (type.getMunitionType().contains(AmmoType.Munitions.M_EXTENDED_RANGE))) {
                erAmmo = ammo;
            } else if ((null == stAmmo) && (type.getMunitionType().contains(AmmoType.Munitions.M_STANDARD))) {
                stAmmo = ammo;
            } else if ((null == infernoAmmo) && (type.getMunitionType().contains(AmmoType.Munitions.M_IATM_IIW))) {
                infernoAmmo = ammo;
            } else if ((null != heAmmo) && (null != erAmmo) && (null != stAmmo) && (null != infernoAmmo)) {
                break;
            }
        }

        // Beyond 15 hexes is ER Ammo only range.
        if (15 < range) {
            returnAmmo = erAmmo;
            // ER Ammo has a better chance to hit past 10 hexes.
        } else if (10 < range) {
            returnAmmo = (null == erAmmo ? stAmmo : erAmmo);
            // At 7-10 hexes, go with Standard, then ER then HE due to hit odds.
        } else if (6 < range) {
            if (null != stAmmo) {
                returnAmmo = stAmmo;
            } else if (null != erAmmo) {
                returnAmmo = erAmmo;
            } else {
                returnAmmo = heAmmo;
            }
            // Six hexes is at min for ER, and medium for both ST & HE.
        } else if (6 == range) {
            if (null != heAmmo) {
                returnAmmo = heAmmo;
            } else if (null != stAmmo) {
                returnAmmo = stAmmo;
            } else {
                returnAmmo = erAmmo;
            }
            // 4-5 hexes is medium for HE, short for ST and well within min for ER.
        } else if (3 < range) {
            if (null != stAmmo) {
                returnAmmo = stAmmo;
            } else if (null != heAmmo) {
                returnAmmo = heAmmo;
            } else {
                returnAmmo = erAmmo;
            }
            // Short range for HE.
        } else {
            if (null != heAmmo) {
                returnAmmo = heAmmo;
            } else if (null != stAmmo) {
                returnAmmo = stAmmo;
            } else {
                returnAmmo = erAmmo;
            }
        }

        if ((returnAmmo == stAmmo) && (null != infernoAmmo)
            && ((9 <= target.getHeat()) || target.isBuilding())
            && !fireResistant) {
            returnAmmo = infernoAmmo;
        }

        return returnAmmo;
    }

    Mounted getAntiVeeAmmo(final List<Mounted> ammoList,
                           final WeaponType weaponType,
                           final int range,
                           final boolean fireResistant) {
        Mounted returnAmmo = null;
        Mounted mmlLrm = null;
        Mounted mmlSrm = null;

        for (final Mounted ammo : ammoList) {
            final AmmoType ammoType = (AmmoType) ammo.getType();
            if (ammoType.getMunitionType().contains(AmmoType.Munitions.M_CLUSTER)
                || (ammoType.getMunitionType().contains(AmmoType.Munitions.M_INFERNO) && !fireResistant)
                || (ammoType.getMunitionType().contains(AmmoType.Munitions.M_INFERNO_IV) && !fireResistant)) {

                // MMLs have additional considerations.
                if (!(weaponType instanceof MMLWeapon)) {
                    returnAmmo = ammo;
                    break;
                }
                if ((null == mmlLrm) && ammoType.hasFlag(AmmoType.F_MML_LRM)) {
                    mmlLrm = ammo;
                } else if (null == mmlSrm) {
                    mmlSrm = ammo;
                } else if (null != mmlLrm) {
                    break;
                }
            }
        }

        // MML ammo depends on range.
        if (weaponType instanceof MMLWeapon) {
            if (9 < range) { // Out of SRM range
                returnAmmo = mmlLrm;
            } else if (6 < range) { // SRM long range.
                returnAmmo = (null == mmlLrm ? mmlSrm : mmlLrm);
            } else {
                returnAmmo = (null == mmlSrm ? mmlLrm : mmlSrm);
            }
        }

        return returnAmmo;
    }

    Mounted getAntiInfantryAmmo(final List<Mounted> ammoList,
                                final WeaponType weaponType,
                                final int range) {
        Mounted returnAmmo = null;
        Mounted mmlLrm = null;
        Mounted mmlSrm = null;

        for (final Mounted ammo : ammoList) {
            final AmmoType ammoType = (AmmoType) ammo.getType();
            if (ammoType.getMunitionType().contains(AmmoType.Munitions.M_FLECHETTE)
                || ammoType.getMunitionType().contains(AmmoType.Munitions.M_FRAGMENTATION)
                || ammoType.getMunitionType().contains(AmmoType.Munitions.M_CLUSTER)
                || ammoType.getMunitionType().contains(AmmoType.Munitions.M_INFERNO)
                || ammoType.getMunitionType().contains(AmmoType.Munitions.M_INFERNO_IV)) {

                // MMLs have additional considerations.
                if (!(weaponType instanceof MMLWeapon)) {
                    returnAmmo = ammo;
                    break;
                }
                if ((null == mmlLrm) && ammoType.hasFlag(AmmoType.F_MML_LRM)) {
                    mmlLrm = ammo;
                } else if (null == mmlSrm) {
                    mmlSrm = ammo;
                } else if (null != mmlLrm) {
                    break;
                }
            }
        }

        // MML ammo depends on range.
        if (weaponType instanceof MMLWeapon) {
            if (9 < range) { // Out of SRM range
                returnAmmo = mmlLrm;
            } else if (6 < range) { // SRM long range.
                returnAmmo = (null == mmlLrm ? mmlSrm : mmlLrm);
            } else {
                returnAmmo = (null == mmlSrm ? mmlLrm : mmlSrm);
            }
        }

        return returnAmmo;
    }

    private Mounted getHeatAmmo(final List<Mounted> ammoList,
                                final WeaponType weaponType,
                                final int range) {
        Mounted returnAmmo = null;
        Mounted mmlLrm = null;
        Mounted mmlSrm = null;

        for (final Mounted ammo : ammoList) {
            final AmmoType ammoType = (AmmoType) ammo.getType();
            if (ammoType.getMunitionType().contains(AmmoType.Munitions.M_INFERNO)
                || ammoType.getMunitionType().contains(AmmoType.Munitions.M_INFERNO_IV)) {

                // MMLs have additional considerations.
                if (!(weaponType instanceof MMLWeapon)) {
                    returnAmmo = ammo;
                    break;
                }
                if ((null == mmlLrm) && ammoType.hasFlag(AmmoType.F_MML_LRM)) {
                    mmlLrm = ammo;
                } else if (null == mmlSrm) {
                    mmlSrm = ammo;
                } else if (null != mmlLrm) {
                    break;
                }
            }
        }

        // MML ammo depends on range.
        if (weaponType instanceof MMLWeapon) {
            if (9 < range) { // Out of SRM range
                returnAmmo = mmlLrm;
            } else if (6 < range) { // SRM long range.
                returnAmmo = (null == mmlLrm ? mmlSrm : mmlLrm);
            } else {
                returnAmmo = (null == mmlSrm ? mmlLrm : mmlSrm);
            }
        }

        return returnAmmo;
    }

    Mounted getIncendiaryAmmo(final List<Mounted> ammoList,
                              final WeaponType weaponType,
                              final int range) {
        Mounted returnAmmo = null;
        Mounted mmlLrm = null;
        Mounted mmlSrm = null;

        for (final Mounted ammo : ammoList) {
            final AmmoType ammoType = (AmmoType) ammo.getType();
            if (ammoType.getMunitionType().contains(AmmoType.Munitions.M_INCENDIARY)
                || ammoType.getMunitionType().contains(AmmoType.Munitions.M_INCENDIARY_LRM)
                || ammoType.getMunitionType().contains(AmmoType.Munitions.M_INCENDIARY_AC)
                || ammoType.getMunitionType().contains(AmmoType.Munitions.M_INFERNO)
                || ammoType.getMunitionType().contains(AmmoType.Munitions.M_INFERNO_IV)) {

                // MMLs have additional considerations.
                if (!(weaponType instanceof MMLWeapon)) {
                    returnAmmo = ammo;
                    break;
                }
                if ((null == mmlLrm) && ammoType.hasFlag(AmmoType.F_MML_LRM)) {
                    mmlLrm = ammo;
                } else if (null == mmlSrm) {
                    mmlSrm = ammo;
                } else if (null != mmlLrm) {
                    break;
                }
            }
        }

        // MML ammo depends on range.
        if (weaponType instanceof MMLWeapon) {
            if (9 < range) { // Out of SRM range
                returnAmmo = mmlLrm;
            } else if (6 < range) { // SRM long range.
                returnAmmo = (null == mmlLrm ? mmlSrm : mmlLrm);
            } else {
                returnAmmo = (null == mmlSrm ? mmlLrm : mmlSrm);
            }
        }

        return returnAmmo;
    }

    Mounted getHardTargetAmmo(final List<Mounted> ammoList,
                              final WeaponType weaponType,
                              final int range) {
        Mounted returnAmmo = null;
        Mounted mmlLrm = null;
        Mounted mmlSrm = null;

        for (final Mounted ammo : ammoList) {
            final AmmoType ammoType = (AmmoType) ammo.getType();
            if (ammoType.getMunitionType().contains(AmmoType.Munitions.M_CLUSTER)
                || ammoType.getMunitionType().contains(AmmoType.Munitions.M_ANTI_FLAME_FOAM)
                || ammoType.getMunitionType().contains(AmmoType.Munitions.M_CHAFF)
                || ammoType.getMunitionType().contains(AmmoType.Munitions.M_COOLANT)
                || ammoType.getMunitionType().contains(AmmoType.Munitions.M_ECM)
                || ammoType.getMunitionType().contains(AmmoType.Munitions.M_FASCAM)
                || ammoType.getMunitionType().contains(AmmoType.Munitions.M_FLAK)
                || ammoType.getMunitionType().contains(AmmoType.Munitions.M_FLARE)
                || ammoType.getMunitionType().contains(AmmoType.Munitions.M_FLECHETTE)
                || ammoType.getMunitionType().contains(AmmoType.Munitions.M_FRAGMENTATION)
                || ammoType.getMunitionType().contains(AmmoType.Munitions.M_HAYWIRE)
                || ammoType.getMunitionType().contains(AmmoType.Munitions.M_INCENDIARY)
                || ammoType.getMunitionType().contains(AmmoType.Munitions.M_INCENDIARY_AC)
                || ammoType.getMunitionType().contains(AmmoType.Munitions.M_INCENDIARY_LRM)
                || ammoType.getMunitionType().contains(AmmoType.Munitions.M_INFERNO)
                || ammoType.getMunitionType().contains(AmmoType.Munitions.M_INFERNO_IV)
                || ammoType.getMunitionType().contains(AmmoType.Munitions.M_LASER_INHIB)
                || ammoType.getMunitionType().contains(AmmoType.Munitions.M_OIL_SLICK)
                || ammoType.getMunitionType().contains(AmmoType.Munitions.M_NEMESIS)
                || ammoType.getMunitionType().contains(AmmoType.Munitions.M_PAINT_OBSCURANT)
                || ammoType.getMunitionType().contains(AmmoType.Munitions.M_SMOKE)
                || ammoType.getMunitionType().contains(AmmoType.Munitions.M_SMOKE_WARHEAD)
                || ammoType.getMunitionType().contains(AmmoType.Munitions.M_THUNDER)
                || ammoType.getMunitionType().contains(AmmoType.Munitions.M_THUNDER_ACTIVE)
                || ammoType.getMunitionType().contains(AmmoType.Munitions.M_THUNDER_AUGMENTED)
                || ammoType.getMunitionType().contains(AmmoType.Munitions.M_THUNDER_INFERNO)
                || ammoType.getMunitionType().contains(AmmoType.Munitions.M_THUNDER_VIBRABOMB)
                || ammoType.getMunitionType().contains(AmmoType.Munitions.M_TORPEDO)
                || ammoType.getMunitionType().contains(AmmoType.Munitions.M_VIBRABOMB_IV)
                || ammoType.getMunitionType().contains(AmmoType.Munitions.M_WATER)
                || ammoType.getMunitionType().contains(AmmoType.Munitions.M_ANTI_TSM)
                || ammoType.getMunitionType().contains(AmmoType.Munitions.M_CORROSIVE)) {
                continue;
            }
            // MMLs have additional considerations.
            if (!(weaponType instanceof MMLWeapon)) {
                returnAmmo = ammo;
                break;
            }
            if ((null == mmlLrm) && ammoType.hasFlag(AmmoType.F_MML_LRM)) {
                mmlLrm = ammo;
            } else if (null == mmlSrm) {
                mmlSrm = ammo;
            } else if (null != mmlLrm) {
                break;
            }
        }

        // MML ammo depends on range.
        if (weaponType instanceof MMLWeapon) {
            if (9 < range) { // Out of SRM range
                returnAmmo = mmlLrm;
            } else if (6 < range) { // SRM long range.
                returnAmmo = (null == mmlLrm ? mmlSrm : mmlLrm);
            } else {
                returnAmmo = (null == mmlSrm ? mmlLrm : mmlSrm);
            }
        }

        return returnAmmo;
    }

    Mounted getAntiAirAmmo(final List<Mounted> ammoList,
                           final WeaponType weaponType,
                           final int range) {
        Mounted returnAmmo = null;
        Mounted mmlLrm = null;
        Mounted mmlSrm = null;

        for (final Mounted ammo : ammoList) {
            final AmmoType ammoType = (AmmoType) ammo.getType();
            if (ammoType.getMunitionType().contains(AmmoType.Munitions.M_ADA)){
                // Air-Defense Arrow IVs are the premiere long-range AA munition.
                returnAmmo = ammo;
                break;
            } else if (ammoType.getMunitionType().contains(AmmoType.Munitions.M_CLUSTER)
                || ammoType.getMunitionType().contains(AmmoType.Munitions.M_FLAK)
                || ammoType.countsAsFlak()) {

                // MMLs have additional considerations.
                // There are no "flak" or "cluster" missile munitions at this point in time.  Code is included in case
                // they are added to the game at some later date.
                if (!(weaponType instanceof MMLWeapon)) {
                    // Naively assume that easier-hitting is better
                    if (returnAmmo != null) {
                        AmmoType returnAmmoType = (AmmoType)(returnAmmo.getType());
                        returnAmmo = ((ammoType.getToHitModifier() > returnAmmoType.getToHitModifier())? ammo : returnAmmo);
                    } else {
                        // Any Cluster/Flak ammo is usually a good bet for AAA.
                        returnAmmo = ammo;
                    }
                }
                if ((null == mmlLrm) && ammoType.hasFlag(AmmoType.F_MML_LRM)) {
                    mmlLrm = ammo;
                } else if (null == mmlSrm) {
                    mmlSrm = ammo;
                } else if (null != mmlLrm) {
                    break;
                }
            }
        }

        // MML ammo depends on range.
        if (weaponType instanceof MMLWeapon) {
            if (9 < range) { // Out of SRM range
                returnAmmo = mmlLrm;
            } else if (6 < range) { // SRM long range.
                returnAmmo = (null == mmlLrm ? mmlSrm : mmlLrm);
            } else {
                returnAmmo = (null == mmlSrm ? mmlLrm : mmlSrm);
            }
        }

        return returnAmmo;
    }

    // Helper method that figures out the valid facing changes for the given shooter
    public static List<Integer> getValidFacingChanges(final Entity shooter) {
        // figure out all valid twists or turret turns
        // mechs can turn:
        //     one left, one right unless he has "no torso twist" quirk or is on the ground
        //     two left, two right if he has "extended torso twist" quirk
        // vehicles and turrets can turn any direction unless he has no turret
        final List<Integer> validFacingChanges = new ArrayList<>();
        if (((Entity.ETYPE_MECH & shooter.getEntityType()) > 0)
            && !shooter.hasQuirk(OptionsConstants.QUIRK_NEG_NO_TWIST)
            && !shooter.hasFallen()) {
            validFacingChanges.add(1);
            validFacingChanges.add(-1);

            if (shooter.hasQuirk(OptionsConstants.QUIRK_POS_EXT_TWIST)) {
                validFacingChanges.add(2);
                validFacingChanges.add(-2);
            }
        } else if ((shooter instanceof Tank
                   && !((Tank) shooter).hasNoTurret()) || (shooter instanceof Infantry)) {
            validFacingChanges.add(1);
            validFacingChanges.add(-1);
            validFacingChanges.add(2);
            validFacingChanges.add(-2);
            validFacingChanges.add(3);
        }

        return validFacingChanges;
    }

    /**
     * This function evaluates whether or not a unit should spend its time
     * unjamming weapons instead of firing, and returns the appropriate firing plan if that's the case.
     * @param shooter Entity being considered.
     * @return Unjam action plan, if we conclude that we should spend time unjamming weapons.
     */
    public Vector<EntityAction> getUnjamWeaponPlan(Entity shooter) {
        int maxJammedDamage = 0;
        int maxDamageWeaponID = -1;
        Vector<EntityAction> unjamVector = new Vector<>();

        // apparently, only tank type units can unjam weapons/clear turrets
        // unconscious crews can't do this
        if (!shooter.hasETypeFlag(Entity.ETYPE_TANK) || !shooter.getCrew().isActive()) {
            return unjamVector;
        }

        Tank tankShooter = (Tank) shooter;

        // can't unjam if crew is stunned. Skip the rest of the logic to save time.
        if (tankShooter.getStunnedTurns() > 0) {
            return unjamVector;
        }

        // step 1: loop through all the unit's jammed weapons to determine the biggest one
        for (Mounted mounted : tankShooter.getJammedWeapons()) {
            int weaponDamage = ((WeaponType) mounted.getType()).getDamage();
            if (weaponDamage == WeaponType.DAMAGE_BY_CLUSTERTABLE) {
                weaponDamage = ((WeaponType) mounted.getType()).getRackSize();
            }

            if (weaponDamage > maxJammedDamage) {
                    maxDamageWeaponID = shooter.getEquipmentNum(mounted);
                    maxJammedDamage = weaponDamage;
            }
        }

        // if any of the unit's weapons are jammed, unjam the biggest one.
        // we can only unjam one per turn.
        if (maxDamageWeaponID >= 0) {
            RepairWeaponMalfunctionAction rwma = new RepairWeaponMalfunctionAction(
                    shooter.getId(), maxDamageWeaponID);

            unjamVector.add(rwma);
        // if the unit has a jammed turret, attempt to clear it
        } else if (tankShooter.canClearTurret()) {
            UnjamTurretAction uta = new UnjamTurretAction(shooter.getId());
            unjamVector.add(uta);
        }

        return unjamVector;
    }

    /**
     * Return a "Find Club" action, if the unit in question can find a club.
     */
    @Nullable
    public FindClubAction getFindClubAction(Entity shooter) {
        if (FindClubAction.canMechFindClub(shooter.getGame(), shooter.getId())) {
            FindClubAction findClubAction = new FindClubAction(shooter.getId());
            return findClubAction;
        }

        return null;
    }

    /**
     * Given a firing plan, calculate the best target to light up with a searchlight
     */
    public SearchlightAttackAction getSearchLightAction(Entity shooter, FiringPlan plan) {
        // no search light if it's not on, unit doesn't have one, or is hidden
        // or the crew is indisposed
        if (!shooter.isUsingSearchlight() || !shooter.hasSearchlight() || shooter.isHidden()
                || !shooter.getCrew().isActive()) {
            return null;
        }

        // assemble set of targets we're planning on shooting
        Set<Coords> planTargets = new HashSet<>();
        if (plan != null) {
            for (WeaponFireInfo wfi : plan) {
                planTargets.add(wfi.getTarget().getPosition());
            }
        }

        List<SearchlightAttackAction> searchlights = new ArrayList<>();
        for (EntityAction action : shooter.getGame().getActionsVector()) {
            if (action instanceof SearchlightAttackAction) {
                searchlights.add((SearchlightAttackAction) action);
            }
        }

        // for each potential target on the board, draw a line between "shooter" and target
        // and assign it a score. Score is determined by:
        // # hostiles lit up - # friendlies lit up + # targets lit up
        Targetable bestTarget = null;
        int bestTargetScore = 0;

        for (Targetable target : getTargetableEnemyEntities(shooter, shooter.getGame(), owner.getFireControlState())) {
            int score = 0;

            for (Coords intervening : Coords.intervening(shooter.getPosition(), target.getPosition())) {
                // if it's already lit up, don't count it
                if (!IlluminationLevel.determineIlluminationLevel(shooter.getGame(), intervening).isNone()) {
                    continue;
                }

                for (Entity ent : shooter.getGame().getEntitiesVector(intervening, true)) {
                    // don't count ourselves, or the target if it's already lit itself up
                    // or the target if it will be lit up by a previously declared search light
                    if ((ent.getId() == shooter.getId()) || ent.isIlluminated()) {
                        continue;
                    } else {
                        boolean willbeIlluminated = false;

                        for (SearchlightAttackAction searchlight : searchlights) {
                            if (searchlight.willIlluminate(shooter.getGame(), ent)) {
                                willbeIlluminated = true;
                                break;
                            }
                        }

                        if (willbeIlluminated) {
                            continue;
                        }
                    }

                    if (ent.isEnemyOf(shooter)) {
                        score++;
                    } else {
                        score--;
                    }

                    if (planTargets.contains(intervening)) {
                        score++;
                    }
                }
            }

            // don't bother considering impossible searchlight actions
            if (score > bestTargetScore && SearchlightAttackAction.isPossible(shooter.getGame(), shooter.getId(), target, null)) {
                bestTargetScore = score;
                bestTarget = target;
            }
        }

        if (bestTarget != null) {
            SearchlightAttackAction slaa = new SearchlightAttackAction(shooter.getId(), bestTarget.getTargetType(), bestTarget.getId());
            return slaa;
        }

        return null;
    }

    /**
     * Attempts to switch the current weapon's firing mode between direct and indirect
     * or vice versa. Returns -1 if the mode switch fails, or the weapon mode index if it succeeds.
     * @return Mode switch result.
     */
    private int switchMissileMode(Mounted weapon) {
        // check that we're operating a missile weapon that can switch direct/indirect modes
        // don't bother checking non-missile weapons
        if (weapon.getType().hasFlag(Weapon.F_MISSILE) &&
                (weapon.hasModeType(Weapon.MODE_MISSILE_INDIRECT) || weapon.hasModeType(Weapon.MODE_INDIRECT_HEAT))) {

            // if we are able to switch the weapon to indirect fire mode, do so and try again
            if (!weapon.curMode().equals(Weapon.MODE_MISSILE_INDIRECT)) {
                return weapon.setMode(Weapon.MODE_MISSILE_INDIRECT);
            } else {
                return weapon.setMode("");
            }
        }

        return -1;
    }
}
