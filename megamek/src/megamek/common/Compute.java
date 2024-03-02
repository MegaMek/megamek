/*
* MegaMek -
* Copyright (C) 2000-2005 Ben Mazur (bmazur@sev.org)
* Copyright (C) 2018 The MegaMek Team
*
* This program is free software; you can redistribute it and/or modify it under
* the terms of the GNU General Public License as published by the Free Software
* Foundation; either version 2 of the License, or (at your option) any later
* version.
*
* This program is distributed in the hope that it will be useful, but WITHOUT
* ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
* FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
* details.
*/
package megamek.common;

import megamek.common.MovePath.MoveStepType;
import megamek.common.actions.*;
import megamek.common.annotations.Nullable;
import megamek.common.enums.*;
import megamek.common.options.OptionsConstants;
import megamek.common.weapons.DiveBombAttack;
import megamek.common.weapons.InfantryAttack;
import megamek.common.weapons.Weapon;
import megamek.common.weapons.artillery.ArtilleryCannonWeapon;
import megamek.common.weapons.battlearmor.ISBAPopUpMineLauncher;
import megamek.common.weapons.bayweapons.BayWeapon;
import megamek.common.weapons.gaussrifles.HAGWeapon;
import megamek.common.weapons.infantry.InfantryWeapon;
import megamek.common.weapons.mgs.MGWeapon;
import megamek.server.Server;
import megamek.server.SmokeCloud;
import org.apache.commons.logging.Log;
import org.apache.logging.log4j.LogManager;

import java.util.*;

/**
 * The Compute class is designed to provide static methods for 'Mechs and other
 * entities moving, firing, etc.
 */
public class Compute {

    public static final int ARC_360 = 0;
    public static final int ARC_FORWARD = 1;
    public static final int ARC_LEFTARM = 2;
    public static final int ARC_RIGHTARM = 3;
    public static final int ARC_REAR = 4;
    public static final int ARC_LEFTSIDE = 5;
    public static final int ARC_RIGHTSIDE = 6;
    public static final int ARC_MAINGUN = 7;
    public static final int ARC_NORTH = 8;
    public static final int ARC_EAST = 9;
    public static final int ARC_WEST = 10;
    public static final int ARC_NOSE = 11;
    public static final int ARC_LWING = 12;
    public static final int ARC_RWING = 13;
    public static final int ARC_LWINGA = 14;
    public static final int ARC_RWINGA = 15;
    public static final int ARC_LEFTSIDE_SPHERE = 16;
    public static final int ARC_RIGHTSIDE_SPHERE = 17;
    public static final int ARC_LEFTSIDEA_SPHERE = 18;
    public static final int ARC_RIGHTSIDEA_SPHERE = 19;
    public static final int ARC_LEFT_BROADSIDE = 20;
    public static final int ARC_RIGHT_BROADSIDE = 21;
    public static final int ARC_AFT = 22;
    public static final int ARC_LEFT_SPHERE_GROUND = 23;
    public static final int ARC_RIGHT_SPHERE_GROUND = 24;
    public static final int ARC_TURRET = 25;
    public static final int ARC_SPONSON_TURRET_LEFT = 26;
    public static final int ARC_SPONSON_TURRET_RIGHT = 27;
    public static final int ARC_PINTLE_TURRET_LEFT = 28;
    public static final int ARC_PINTLE_TURRET_RIGHT = 29;
    public static final int ARC_PINTLE_TURRET_FRONT = 30;
    public static final int ARC_PINTLE_TURRET_REAR = 31;
    public static final int ARC_VGL_FRONT = 32;
    public static final int ARC_VGL_RF = 33;
    public static final int ARC_VGL_RR = 34;
    public static final int ARC_VGL_REAR = 35;
    public static final int ARC_VGL_LR = 36;
    public static final int ARC_VGL_LF = 37;
    // Expanded arcs for Waypoint Launched Capital Missiles
    public static final int ARC_NOSE_WPL = 38;
    public static final int ARC_LWING_WPL = 39;
    public static final int ARC_RWING_WPL = 40;
    public static final int ARC_LWINGA_WPL = 41;
    public static final int ARC_RWINGA_WPL = 42;
    public static final int ARC_LEFTSIDE_SPHERE_WPL = 43;
    public static final int ARC_RIGHTSIDE_SPHERE_WPL = 44;
    public static final int ARC_LEFTSIDEA_SPHERE_WPL = 45;
    public static final int ARC_RIGHTSIDEA_SPHERE_WPL = 46;
    public static final int ARC_AFT_WPL = 47;
    public static final int ARC_LEFT_BROADSIDE_WPL = 48;
    public static final int ARC_RIGHT_BROADSIDE_WPL = 49;
    public static final int HOMING_RADIUS = 8;

    public static int DEFAULT_MAX_VISUAL_RANGE = 1;

    /** Lookup table for vehicular grenade launcher firing arc from facing */
    private static final int[] VGL_FIRING_ARCS = { ARC_VGL_FRONT, ARC_VGL_RF, ARC_VGL_RR,
            ARC_VGL_REAR, ARC_VGL_LR, ARC_VGL_LF
    };

    private static MMRandom random = MMRandom.generate(MMRandom.R_DEFAULT);

    private static final int[][] clusterHitsTable = new int[][] {
            { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 },
            { 2, 1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2 },
            { 3, 1, 1, 1, 2, 2, 2, 2, 2, 3, 3, 3 },
            { 4, 1, 2, 2, 2, 2, 3, 3, 3, 3, 4, 4 },
            { 5, 1, 2, 2, 3, 3, 3, 3, 4, 4, 5, 5 },
            { 6, 2, 2, 3, 3, 4, 4, 4, 5, 5, 6, 6 },
            { 7, 2, 2, 3, 4, 4, 4, 4, 6, 6, 7, 7 },
            { 8, 2, 3, 3, 4, 4, 5, 5, 6, 7, 8, 8 },
            { 9, 3, 3, 4, 5, 5, 5, 5, 7, 7, 9, 9 },
            { 10, 3, 3, 4, 6, 6, 6, 6, 8, 8, 10, 10 },
            { 11, 4, 4, 5, 7, 7, 7, 7, 9, 9, 11, 11 },
            { 12, 4, 4, 5, 8, 8, 8, 8, 10, 10, 12, 12 },
            { 13, 4, 4, 5, 8, 8, 8, 8, 11, 11, 13, 13 },
            { 14, 5, 5, 6, 9, 9, 9, 9, 11, 11, 14, 14 },
            { 15, 5, 5, 6, 9, 9, 9, 9, 12, 12, 15, 15 },
            { 16, 5, 5, 7, 10, 10, 10, 10, 13, 13, 16, 16 },
            { 17, 5, 5, 7, 10, 10, 10, 10, 14, 14, 17, 17 },
            { 18, 6, 6, 8, 11, 11, 11, 11, 14, 14, 18, 18 },
            { 19, 6, 6, 8, 11, 11, 11, 11, 15, 15, 19, 19 },
            { 20, 6, 6, 9, 12, 12, 12, 12, 16, 16, 20, 20 },
            { 21, 7, 7, 9, 13, 13, 13, 13, 17, 17, 21, 21 },
            { 22, 7, 7, 9, 14, 14, 14, 14, 18, 18, 22, 22 },
            { 23, 7, 7, 10, 15, 15, 15, 15, 19, 19, 23, 23 },
            { 24, 8, 8, 10, 16, 16, 16, 16, 20, 20, 24, 24 },
            { 25, 8, 8, 10, 16, 16, 16, 16, 21, 21, 25, 25 },
            { 26, 9, 9, 11, 17, 17, 17, 17, 21, 21, 26, 26 },
            { 27, 9, 9, 11, 17, 17, 17, 17, 22, 22, 27, 27 },
            { 28, 9, 9, 11, 17, 17, 17, 17, 23, 23, 28, 28 },
            { 29, 10, 10, 12, 18, 18, 18, 18, 23, 23, 29, 29 },
            { 30, 10, 10, 12, 18, 18, 18, 18, 24, 24, 30, 30 },
            { 40, 12, 12, 18, 24, 24, 24, 24, 32, 32, 40, 40 } };

    /**
     * Wrapper to random#d6()
     */
    public static int d6() {
        return d6( 1);
    }

    /**
     * Wrapper to random#d6(n)
     */
    public static int d6(int dice) {
        return rollD6(dice).getIntValue();
    }

    /**
     * Wrapper to random#d6(n)
     */
    public static int d6(int dice, int keep) {
        return rollD6(dice, keep).getIntValue();
    }

    /**
     * Wrapper to random#d6(n)
     */
    public static Roll rollD6(int dice) {
        Roll roll = random.d6(dice);
        if (Server.getServerInstance() != null) {
            if (Server.getServerInstance().getGame().getOptions()
                    .booleanOption(OptionsConstants.BASE_RNG_LOG)) {
                Server.getServerInstance().reportRoll(roll);
            }
        }
        return roll;
    }

    /**
     * Wrapper to random#d6(n)
     */
    public static Roll rollD6(int dice, int keep) {
        Roll roll = random.d6(dice, keep);
        if (Server.getServerInstance() != null) {
            if (Server.getServerInstance().getGame().getOptions()
                    .booleanOption(OptionsConstants.BASE_RNG_LOG)) {
                Server.getServerInstance().reportRoll(roll);
            }
        }
        return roll;
    }

    /**
     * Input is in format "ndf", so this can handle 2d6 or 3d10
     * @param number the number of dice to roll
     * @param faces  the number of faces on those dice
     * @return an Integer list of every dice roll, with index 0 containing the summed result
     */
    public static List<Integer> individualDice(final int number, final int faces) {
        final List<Integer> individualRolls = new ArrayList<>();
        int result = 0, roll;
        individualRolls.add(result);

        for (int i = 0; i < number; i++) {
            roll = randomInt(faces) + 1;
            individualRolls.add(roll);
            result += roll;
        }

        individualRolls.set(0, result);

        return individualRolls;
    }

    /**
     * Input is in format "c ndf", so that this can handle 10 rolls of 3d6
     * @param count  the count of sets of dice to roll
     * @param number the number of dice to roll per set
     * @param faces  the number of faces per die
     * @return an Integer list of every summed dice roll, with index 0 containing the summed result
     */
    public static List<Integer> individualRolls(int count, int number, int faces) {
        List<Integer> individualRolls = new ArrayList<>();
        int result = 0, roll;
        individualRolls.add(result);

        for (int x = 0; x < count; x++) {
            roll = 0;
            for (int y = 0; y < number; y++) {
                roll += randomInt(faces) + 1;
            }
            individualRolls.add(roll);
            result += roll;
        }

        individualRolls.set(0, result);

        return individualRolls;
    }

    /**
     * Generates a number between 0 and maxValue - 1.
     * e.g. randomInt(2) will generate either 0s or 1s
     */
    public static int randomInt(int maxValue) {
        Roll roll = new MMRoll(random, maxValue);
        return roll.getIntValue();
    }

    /**
     * Wrapper to random#randomFloat()
     */
    public static float randomFloat() {
        return random.randomFloat();
    }

    /**
     * Sets the RNG to the desired type
     */
    public static void setRNG(int type) {
        random = MMRandom.generate(type);
    }

    /**
     * Sets the RNG to the specific instance.
     * @param random A non-null instance of {@link MMRandom} to use
     *               for all random number generation.
     */
    public static void setRNG(MMRandom random) {
        Compute.random = Objects.requireNonNull(random);
    }

    /**
     * Returns the odds that a certain number or above will be rolled on 2d6.
     */
    public static double oddsAbove(int n) {
        return oddsAbove(n, false);
    }


    /**
     * Returns the odds that a certain number or above will be rolled on 2d6,
     * or on 3d6 drop the lowest if the flag is set.
     *
     * @param n
     * @param dropLowest Flag that determines whether 2d6 or 3d6 drop the
     *                   lowest is used
     * @return
     */
    public static double oddsAbove(int n, boolean dropLowest) {
        if (n <= 2) {
            return 100.0;
        } else if (n > 12) {
            return 0;
        }

        if (dropLowest) {
            final double[] odds = {100.0, 100.0, 100.0, 99.54, 98.15, 94.91,
                                   89.35, 80.56, 68.06, 52.32, 35.65, 19.91, 7.41, 0};
            return odds[n];
        } else {
            final double[] odds = {100.0, 100.0, 100.0, 97.2, 91.6, 83.3, 72.2,
                                   58.3, 41.6, 27.7, 16.6, 8.3, 2.78, 0};
            return odds[n];
        }
    }

    /**
     * Returns an entity if the specified entity would cause a stacking
     * violation entering a hex, or returns null if it would not. The returned
     * entity is the entity causing the violation.
     *
     * The position, and elevation for the stacking violation are derived from
     * the Entity represented by the passed Entity ID.
     *
     * @param game       The Game instance
     * @param enteringId The gameId of the moving Neity
     * @param coords     The hex being entered
     * @param climbMode  The moving Entity's climb mode at the point it enters the destination hex
     */
    public static Entity stackingViolation(Game game, int enteringId,
            Coords coords, boolean climbMode) {
        Entity entering = game.getEntity(enteringId);
        if (entering == null) {
            return null;
        }
        return Compute.stackingViolation(game, entering, coords, null, climbMode);
    }

    /**
     * When compiling an unloading step, both the transporter and the unloaded
     * unit probably occupy some other position on the board.
     *
     * The position, and elevation for the stacking violation are derived from
     * the passed Entity.
     *
     * @param game       The Game instance
     * @param entering   The Entity entering the hex
     * @param dest       The hex being entered
     * @param transport  Represents the unit transporting entering, which may affect
     *                   stacking, can be null
     * @param climbMode  The moving Entity's climb mode at the point it enters the destination hex
     */
    public static Entity stackingViolation(Game game, Entity entering,
            Coords dest, Entity transport, boolean climbMode) {
        return stackingViolation(game, entering, entering.getElevation(), dest,
                transport, climbMode);
    }

    /**
     * Returns an entity if the specified entity would cause a stacking
     * violation entering a hex, or returns null if it would not. The returned
     * entity is the entity causing the violation.
     *
     * The position is derived from the passed Entity, while the elevation is
     * derived from the passed Entity parameter.
     *
     * @param game       The Game instance
     * @param entering   The Entity entering the hex
     * @param elevation  The elevation of the moving Entity
     * @param dest       The hex being entered
     * @param transport  Represents the unit transporting entering, which may affect
     *                   stacking, can be null
     * @param climbMode  The moving Entity's climb mode at the point it enters the destination hex
     */
    public static Entity stackingViolation(Game game, Entity entering,
            int elevation, Coords dest, Entity transport, boolean climbMode) {
        return stackingViolation(game, entering, entering.getPosition(),
                elevation, dest, transport, climbMode);
    }

    /**
     * Returns an entity if the specified entity would cause a stacking
     * violation entering a hex, or returns null if it would not. The returned
     * entity is the entity causing the violation.
     *
     * The position and elevation is derived from the passed Entity parameter.
     *
     * @param game         The Game instance
     * @param entering     The Entity entering the hex
     * @param origPosition The coords of the hex the moving Entity is leaving
     * @param elevation    The elevation of the moving Entity
     * @param dest         The hex being entered
     * @param transport    Represents the unit transporting entering, which may affect
     *                     stacking, can be null
     * @param climbMode    The moving Entity's climb mode at the point it enters the destination hex
     */
    public static Entity stackingViolation(Game game, Entity entering,
            Coords origPosition, int elevation, Coords dest, Entity transport, boolean climbMode) {
        // no stacking violations on the low-atmosphere and space maps
        if (!game.getBoard().onGround()) {
            return null;
        }

        // no stacking violations for flying aeros
        if (entering.isAirborne()) {
            return null;
        }

        boolean isMech = (entering instanceof Mech)
                || (entering instanceof SmallCraft);
        boolean isLargeSupport = (entering instanceof LargeSupportTank)
                || (entering instanceof Dropship)
                || ((entering instanceof Mech) && ((Mech) entering)
                        .isSuperHeavy());

        boolean isTrain = !entering.getAllTowedUnits().isEmpty();
        boolean isDropship = entering instanceof Dropship;
        boolean isInfantry = entering instanceof Infantry;
        Entity firstEntity = transport;
        int totalUnits = 1;
        Vector<Coords> positions = new Vector<>();
        positions.add(dest);
        if (isDropship) {
            for (int dir = 0; dir < 6; dir++) {
                positions.add(dest.translated(dir));
            }
        }
        for (Coords coords : positions) {
            int thisLowStackingLevel = elevation;
            if ((coords != null) && (origPosition != null)) {
                thisLowStackingLevel = entering.calcElevation(game.getBoard()
                        .getHex(origPosition), game.getBoard()
                        .getHex(coords), elevation, climbMode, false);
            }
            int thisHighStackingLevel = thisLowStackingLevel;
            // mechs only occupy one level of a building
            if (!Compute.isInBuilding(game, entering, coords)) {
                thisHighStackingLevel += entering.height();
            }

            // Walk through the entities in the given hex.
            for (Entity inHex : game.getEntitiesVector(coords)) {

                if (inHex.isAirborne()) {
                    continue;
                }

                int lowStackingLevel = inHex.getElevation();
                int highStackingLevel = lowStackingLevel;
                // units only occupy one level of a building
                if (!Compute.isInBuilding(game, inHex)) {
                    highStackingLevel += inHex.height();
                }

                // Only do all this jazz if they're close enough together on lvl
                // to interfere.
                if ((thisLowStackingLevel <= highStackingLevel)
                        && (thisHighStackingLevel >= lowStackingLevel)) {
                    // Don't compare the entering entity to itself.
                    if (inHex.equals(entering)) {
                        continue;
                    }

                    // Ignore the transport of the entering entity.
                    if (inHex.equals(transport)) {
                        continue;
                    }

                    //ignore the first trailer behind a non-superheavy tractor
                    //which can be in the same hex
                    if (isTrain && !entering.isSuperHeavy()) {
                        Entity firstTrailer = game.getEntity(entering.getAllTowedUnits().get(0));
                        if (inHex.equals(firstTrailer)) {
                            continue;
                        }
                    }

                    // DFAing units don't count towards stacking
                    if (inHex.isMakingDfa()) {
                        continue;
                    }

                    // If the entering entity is a mech,
                    // then any other mech in the hex is a violation.
                    // Unless grappled (but chain whip grapples don't count)
                    // grounded small craft are treated as mechs for purposes
                    // of stacking
                    if (isMech
                            && (((inHex instanceof Mech) && (inHex
                                    .getGrappled() != entering.getId() || inHex
                                    .isChainWhipGrappled())) || (inHex instanceof SmallCraft))) {
                        return inHex;
                    }

                    // only inf can be in the same hex as a large support vee
                    // grounded dropships are treated as large support vees,
                    // ditto for superheavy mechs
                    if (isLargeSupport && !(inHex instanceof Infantry)) {
                        return inHex;
                    }
                    if (((inHex instanceof LargeSupportTank)
                            || (inHex instanceof Dropship) || ((inHex instanceof Mech) && ((Mech) inHex)
                            .isSuperHeavy())) && !isInfantry) {
                        return inHex;
                    }

                    totalUnits++;
                    // If the new one is the most
                    if (totalUnits > 4) {
                        // Arbitrarily return this one, because we can, and it's
                        // simpler.
                        return inHex;
                    }

                    // Otherwise, if there are two present entities controlled
                    // by this player, returns a random one of the two.
                    // Somewhat arbitrary, but how else should we resolve it?
                    if (!inHex.getOwner().isEnemyOf(entering.getOwner())) {
                        if (firstEntity == null) {
                            firstEntity = inHex;
                        } else {
                            return Compute.d6() > 3 ? firstEntity : inHex;
                        }
                    }
                }
            }
        }
        // okay, all clear
        return null;
    }

    /**
     * Returns true if there is any unit that is an enemy of the specified unit
     * in the specified hex. This is only called for stacking purposes, and so
     * does not return true if the enemy unit is currenly making a DFA.
     */
    public static boolean isEnemyIn(Game game, Entity entity, Coords coords,
                                    boolean onlyMechs, boolean ignoreInfantry, int enLowEl) {
        int enHighEl = enLowEl + entity.getHeight();
        for (Entity inHex : game.getEntitiesVector(coords)) {
            int inHexAlt = inHex.getAltitude();
            boolean crewOnGround = (inHex instanceof EjectedCrew) && (inHexAlt == 0);
            int inHexEnLowEl = inHex.getElevation();
            int inHexEnHighEl = inHexEnLowEl + inHex.getHeight();
            if ((!onlyMechs || (inHex instanceof Mech))
                && !(ignoreInfantry && (inHex instanceof Infantry))
                && inHex.isEnemyOf(entity) && !inHex.isMakingDfa()
                && (enLowEl <= inHexEnHighEl) && (enHighEl >= inHexEnLowEl)
                && (!(inHex instanceof EjectedCrew) || (crewOnGround))) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return true if a piloting skill roll is needed to traverse the terrain
     */
    public static boolean isPilotingSkillNeeded(Game game, int entityId,
            Coords src, Coords dest, EntityMovementType movementType,
            boolean isTurning, boolean prevStepIsOnPavement, int srcElevation,
            int destElevation, MoveStep moveStep) {
        final Entity entity = game.getEntity(entityId);
        final Hex srcHex = game.getBoard().getHex(src);
        final Hex destHex = game.getBoard().getHex(dest);
        final boolean isInfantry = (entity instanceof Infantry);
        int delta_alt = (destElevation + destHex.getLevel())
                        - (srcElevation + srcHex.getLevel());

        // arguments valid?
        if (entity == null) {
            throw new IllegalArgumentException("Entity invalid. ID " + entityId);
        }
        if (src.distance(dest) > 1) {
            throw new IllegalArgumentException("Coordinates must be adjacent.");
        }

        // let's only worry about actual movement, please
        if (src.equals(dest)) {
            return false;
        }

        // airborne aircraft do not require pavement-related checks
        final boolean isPavementStep = entity.isAirborne() ? false : Compute.canMoveOnPavement(game, src, dest, moveStep);

        // check for rubble
        if ((movementType != EntityMovementType.MOVE_JUMP)
            && (destHex.terrainLevel(Terrains.RUBBLE) > 0)
            && (destElevation == 0)
            && !isPavementStep
            && entity.canFall()) {
            return true;
        }

        // check for swamp
        if (destHex.containsTerrain(Terrains.SWAMP)
            && !(entity.getElevation() > destHex.getLevel())
            && (entity.getMovementMode() != EntityMovementMode.HOVER)
            && (entity.getMovementMode() != EntityMovementMode.VTOL)
            && (movementType != EntityMovementType.MOVE_JUMP)
            && (entity.getMovementMode() != EntityMovementMode.WIGE)
            && !isPavementStep) {
            return true;
        }

        // check for thin ice
        if (destHex.containsTerrain(Terrains.ICE)
            && destHex.containsTerrain(Terrains.WATER)
            && !(entity.getElevation() > destHex.getLevel())
            && !isPavementStep
            && (movementType != EntityMovementType.MOVE_JUMP)) {
            return true;
        }

        // Check for black ice on pavement
        if (destHex.containsTerrain(Terrains.BLACK_ICE)
                && !(entity.getElevation() > destHex.getLevel())
                && isPavementStep
                && (movementType != EntityMovementType.MOVE_JUMP)) {
            return true;
        }

        // Check for water unless we're a hovercraft or naval or using a bridge
        // or flying or QuadVee in vehicle mode.
        if ((movementType != EntityMovementType.MOVE_JUMP)
                && !(entity.getElevation() > destHex.getLevel())
                && !((entity.getMovementMode() == EntityMovementMode.HOVER)
                        || (entity.getMovementMode() == EntityMovementMode.NAVAL)
                        || (entity.getMovementMode() == EntityMovementMode.HYDROFOIL)
                        || (entity.getMovementMode() == EntityMovementMode.SUBMARINE)
                        || (entity.getMovementMode() == EntityMovementMode.INF_UMU)
                        || (entity.getMovementMode() == EntityMovementMode.BIPED_SWIM)
                        || (entity.getMovementMode() == EntityMovementMode.QUAD_SWIM)
                        || (entity.getMovementMode() == EntityMovementMode.WIGE)
                        || (entity instanceof QuadVee && entity.getConversionMode() == QuadVee.CONV_MODE_VEHICLE))
                && (destHex.terrainLevel(Terrains.WATER) > 0)
                && !isPavementStep) {
            return true;
        }

        // Sheer Cliffs, TO p.39
        // Roads over cliffs cancel the cliff effects for units that move on roads
        boolean quadveeVehMode = entity instanceof QuadVee
                && entity.getConversionMode() == QuadVee.CONV_MODE_VEHICLE;
        boolean vehicleAffectedByCliff = entity instanceof Tank
                && !entity.isAirborneVTOLorWIGE();
        boolean mechAffectedByCliff = (entity instanceof Mech || entity instanceof Protomech)
                && movementType != EntityMovementType.MOVE_JUMP
                && !entity.isAero(); // LAM
        int stepHeight = destElevation + destHex.getLevel() - (srcElevation + srcHex.getLevel());
        // Cliffs should only exist towards 1 or 2 level drops, check just to make sure
        // Everything that does not have a 1 or 2 level drop shouldn't be handled as a cliff
        boolean isUpCliff = !src.equals(dest)
                && destHex.hasCliffTopTowards(srcHex)
                && (stepHeight == 1 || stepHeight == 2);
        boolean isDownCliff = !src.equals(dest)
                && srcHex.hasCliffTopTowards(destHex)
                && (stepHeight == -1 || stepHeight == -2);

        // Mechs and Vehicles moving down a cliff
        // Quadvees in vee mode ignore PSRs to avoid falls, IO p.133
        if ((mechAffectedByCliff || vehicleAffectedByCliff)
                && !quadveeVehMode
                && isDownCliff
                && !isPavementStep) {
            return true;
        }

        // Mechs moving up a cliff
        if (mechAffectedByCliff
                && !quadveeVehMode
                && isUpCliff
                && !isPavementStep) {
            return true;
        }

        // Check for skid. Please note, the skid will be rolled on the
        // current step, but starts from the previous step's location.
        // TODO: add check for elevation of pavement, road,
        // or bridge matches entity elevation.
        /*
         * Bug 754610: Revert fix for bug 702735. if ( (
         * srcHex.contains(Terrain.PAVEMENT) || srcHex.contains(Terrain.ROAD) ||
         * srcHex.contains(Terrain.BRIDGE) )
         */
        if (((prevStepIsOnPavement
                && ((movementType == EntityMovementType.MOVE_RUN)
                        || (movementType == EntityMovementType.MOVE_SPRINT)))
                        || ((srcHex.containsTerrain(Terrains.ICE))
                                && (movementType != EntityMovementType.MOVE_JUMP)))
                && (entity.getMovementMode() != EntityMovementMode.HOVER)
                && (entity.getMovementMode() != EntityMovementMode.WIGE)
                && isTurning && !isInfantry) {
            return true;
        }

        // If we entering a building, all non-infantry
        // need to make a piloting check to avoid damage.
        if ((destElevation < destHex.terrainLevel(Terrains.BLDG_ELEV))
            && !(entity instanceof Infantry)) {
            Building bldg = game.getBoard().getBuildingAt(dest);
            boolean insideHangar = (null != bldg)
                                   && bldg.isIn(src)
                                   && (bldg.getBldgClass() == Building.HANGAR)
                                   && (destHex.terrainLevel(Terrains.BLDG_ELEV) > entity
                    .height());
            if (!insideHangar) {
                return true;
            }
        }

        // check sideslips
        if ((entity instanceof VTOL)
                || (entity.getMovementMode() == EntityMovementMode.HOVER)
                || (entity.getMovementMode() == EntityMovementMode.WIGE
                        && destElevation > 0 && !(entity instanceof Protomech))) {
            if (isTurning
                    && ((movementType == EntityMovementType.MOVE_RUN)
                            || (movementType == EntityMovementType.MOVE_SPRINT)
                            || (movementType == EntityMovementType.MOVE_VTOL_RUN)
                            || (movementType == EntityMovementType.MOVE_VTOL_SPRINT))) {
                return true;
            }
            // Controlled sideslip requires check to avoid extra hex of sideslip movement.
            if ((moveStep.getType() == MoveStepType.LATERAL_LEFT
                    || moveStep.getType() == MoveStepType.LATERAL_RIGHT
                    || moveStep.getType() == MoveStepType.LATERAL_LEFT_BACKWARDS
                    || moveStep.getType() == MoveStepType.LATERAL_RIGHT_BACKWARDS)
                    && (!entity.isUsingManAce()
                            || movementType != EntityMovementType.MOVE_WALK
                            || movementType != EntityMovementType.MOVE_VTOL_WALK)) {
                return true;
            }
        }

        // check leaps
        if ((entity instanceof Mech) && (delta_alt < -2)
            && (movementType != EntityMovementType.MOVE_JUMP
            && (movementType != EntityMovementType.MOVE_VTOL_WALK
            && (movementType != EntityMovementType.MOVE_VTOL_RUN)))) {
            return true;
        }

        return false;
    }

    /**
     * Can the defending unit be displaced from the source to the destination?
     */
    public static boolean isValidDisplacement(Game game, int entityId,
            Coords src, int direction) {
        return Compute.isValidDisplacement(game, entityId, src,
                src.translated(direction));
    }

    /**
     * Can the defending unit be displaced from the source to the destination?
     */
    public static boolean isValidDisplacement(Game game, int entityId,
            Coords src, Coords dest) {
        final Entity entity = game.getEntity(entityId);
        final Hex srcHex = game.getBoard().getHex(src);
        final Hex destHex = game.getBoard().getHex(dest);
        final ArrayList<Coords> intervening = Coords.intervening(src, dest);
        final int direction = src.direction(dest);

        // arguments valid?
        if (entity == null) {
            throw new IllegalArgumentException("Entity invalid.");
        }

        // dropships should never be displaceable
        // this should also take care of the situation of displacing another
        // entity
        // into a grounded droppers hex, because of the stacking violation check
        // below
        if (entity instanceof Dropship) {
            return false;
        }

        // an easy check
        if (!game.getBoard().contains(dest)) {
            if (game.getOptions().booleanOption(OptionsConstants.BASE_PUSH_OFF_BOARD)) {
                return true;
            }
            return false;
        }

        // can't be displaced into prohibited terrain
        // unless we're displacing a tracked or wheeled vee into water
        if (entity.isLocationProhibited(dest)
                && !((entity instanceof Tank)
                        && destHex.containsTerrain(Terrains.WATER)
                        && ((entity.movementMode == EntityMovementMode.TRACKED)
                                || (entity.movementMode == EntityMovementMode.WHEELED)))) {
            return false;
        }

        // can't go up more levels than normally possible
        for (Coords c : intervening) {
            // ignore off-board hexes
            if (!game.getBoard().contains(c)) {
                continue;
            }
            final Hex hex = game.getBoard().getHex(c);
            int change = entity.elevationOccupied(hex)
                         - entity.elevationOccupied(srcHex);
            if (change > entity.getMaxElevationChange()) {
                return false;
            }
        }

        // if there's an entity in the way, can they be displaced in that
        // direction?
        Entity inTheWay = Compute.stackingViolation(game, entityId, dest, false);
        if (inTheWay != null) {
            return Compute.isValidDisplacement(game, inTheWay.getId(),
                    inTheWay.getPosition(), direction);
        }

        // okay, that's about all the checks
        return true;
    }

    /**
     * Gets a valid displacement, from the hexes around src, as close to the
     * original direction as is possible.
     *
     * @return valid displacement coords, or null if none
     */
    public static Coords getValidDisplacement(Game game, int entityId,
            Coords src, int direction) {
        // check the surrounding hexes, nearest to the original direction first
        int[] offsets = {0, 1, 5, 2, 4, 3};
        int range = 1;
        // check for a central dropship hex and if so, then displace to a two
        // hex radius
        for (Entity en : game.getEntitiesVector(src)) {
            if ((en instanceof Dropship) && !en.isAirborne()
                && en.getPosition().equals(src)) {
                range = 2;
            }
        }
        for (int offset : offsets) {
            Coords dest = src.translated((direction + offset) % 6, range);
            if (Compute.isValidDisplacement(game, entityId, src, dest)) {
                return dest;
            }
            // code here borrowed from Compute.coordsAtRange
            for (int count = 1; count < range; count++) {
                dest = dest.translated((direction + offset + 2) % 6);
                if (Compute.isValidDisplacement(game, entityId, src, dest)) {
                    return dest;
                }
            }
        }
        // have fun being insta-killed!
        return null;
    }

    /**
     * Gets a preferred displacement. Right now this picks the surrounding hex
     * with the same elevation as original hex, if not available it picks the
     * highest elevation that is a valid displacement. This will preferably not
     * displace into friendly units
     *
     * @return valid displacement coords, or null if none
     */
    public static Coords getPreferredDisplacement(Game game, int entityId,
            Coords src, int direction) {
        final Entity entity = game.getEntity(entityId);
        int highestElev = Integer.MIN_VALUE;
        Coords highest = null;
        int srcElevation =
                entity.elevationOccupied(game.getBoard().getHex(src));

        // check the surrounding hexes, nearest to the original direction first
        int[] offsets = {0, 1, 5, 2, 4, 3};
        // first, try not to displace into friendly units
        for (int offset : offsets) {
            Coords dest = src.translated((direction + offset) % 6);
            if (Compute.isValidDisplacement(game, entityId, src, dest)
                && game.getBoard().contains(dest)) {
                Iterator<Entity> entities = game.getFriendlyEntities(dest,
                        game.getEntity(entityId));
                if (entities.hasNext()) {
                    // friendly unit here, try next hex
                    continue;
                }
                Hex hex = game.getBoard().getHex(dest);
                int elevation = entity.elevationOccupied(hex);
                if (elevation > highestElev) {
                    highestElev = elevation;
                    highest = dest;
                }
                // preferably, go to same elevation
                if (elevation == srcElevation) {
                    return dest;
                }
            }
        }
        if (highest != null) {
            return highest;
        }
        // ok, all hexes occupied, now displace preferably to same elevation,
        // else highest
        for (int offset : offsets) {
            Coords dest = src.translated((direction + offset) % 6);
            if (Compute.isValidDisplacement(game, entityId, src, dest)
                && game.getBoard().contains(dest)) {
                Hex hex = game.getBoard().getHex(dest);
                int elevation = entity.elevationOccupied(hex);
                if (elevation > highestElev) {
                    highestElev = elevation;
                    highest = dest;
                }
                // preferably, go to same elevation
                if (elevation == entity.getElevation()) {
                    return dest;
                }
            }
        }
        return highest;
    }

    /**
     * Gets a hex to displace a missed charge to. Picks left or right, first
     * preferring higher hexes, then randomly, or returns the base hex if
     * they're impassible.
     */
    public static Coords getMissedChargeDisplacement(Game game, int entityId,
                                                     Coords src, int direction) {
        Coords first = src.translated((direction + 1) % 6);
        Coords second = src.translated((direction + 5) % 6);
        Hex firstHex = game.getBoard().getHex(first);
        Hex secondHex = game.getBoard().getHex(second);
        Entity entity = game.getEntity(entityId);

        if ((firstHex == null) || (secondHex == null)) {
            // leave it, will be handled
        } else if (entity.elevationOccupied(firstHex) > entity
                .elevationOccupied(secondHex)) {
            // leave it
        } else if (entity.elevationOccupied(firstHex) < entity
                .elevationOccupied(secondHex)) {
            // switch
            Coords temp = first;
            first = second;
            second = temp;
        } else if (Compute.d6() > 3) {
            // switch randomly
            Coords temp = first;
            first = second;
            second = temp;
        }

        if (Compute.isValidDisplacement(game, entityId, src,
                                        src.direction(first))
            && game.getBoard().contains(first)) {
            return first;
        } else if (Compute.isValidDisplacement(game, entityId, src,
                                               src.direction(second))
                   && game.getBoard().contains(second)) {
            return second;
        } else {
            return src;
        }
    }

    /**
     * Finds the best spotter for the attacker. The best spotter is the one with
     * the lowest attack modifiers, of course. LOS modifiers and movement are
     * considered.
     */
    public static Entity findSpotter(Game game, Entity attacker,
                                     Targetable target) {
        Entity spotter = null;
        int taggedBy = -1;
        if (target instanceof Entity) {
            taggedBy = ((Entity) target).getTaggedBy();
        }
        ToHitData bestMods = new ToHitData(TargetRoll.IMPOSSIBLE, "");

        for (Entity other : game.getEntitiesVector()) {
            if (((other.isSpotting() && (other.getSpotTargetId() == target
                    .getId())) || (taggedBy == other.getId()))
                && !attacker.isEnemyOf(other)) {
                // what are this guy's mods to the attack?
                LosEffects los = LosEffects.calculateLOS(game, other, target, true);
                ToHitData mods = los.losModifiers(game);
                // If the target isn't spotted, can't target
                if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_DOUBLE_BLIND)
                    && !Compute.inVisualRange(game, los, other, target)
                    && !Compute.inSensorRange(game, los, other, target, null)) {
                    mods.addModifier(TargetRoll.IMPOSSIBLE,
                                     "outside of visual and sensor range");
                }
                los.setTargetCover(LosEffects.COVER_NONE);
                mods.append(Compute.getAttackerMovementModifier(game,
                                                                other.getId()));

                // a spotter suffers a penalty if it's also making an attack this round
                // unless it has a command console or has TAGged the target
                if (other.isAttackingThisTurn() && !other.getCrew().hasActiveCommandConsole() &&
                        (!isTargetTagged(attacker, target, game) || (taggedBy != -1))) {
                    mods.addModifier(1, "spotter is making an attack this turn");
                }

                // is this guy a better spotter?
                if ((spotter == null)
                    || (mods.getValue() < bestMods.getValue())) {
                    spotter = other;
                    bestMods = mods;
                }
            }
        }

        return spotter;
    }

    /**
     * Worker function to determine if the target has been tagged.
     * @param target The non-entity target to check
     * @param game The current {@link Game}
     * @return Whether or not the given entity or other targetable is tagged.
     */
    public static boolean isTargetTagged(Targetable target, Game game) {
        boolean targetTagged = false;

        Entity te = null;
        if (target instanceof Entity) {
            te = (Entity) target;
        }

        // If this is an entity, we can see if it's tagged
        if (te != null) {
            targetTagged = te.getTaggedBy() != -1;
        } else { // Non entities will require us to look harder
            for (TagInfo ti : game.getTagInfo()) {
                if (target.getId() == ti.target.getId()) {
                    return true;
                }
            }
        }

        return targetTagged;
    }

    /**
     * Worker function to determine if the target has been tagged by the specific attacker.
     * @param attacker The attacker.
     * @param target The non-entity target to check
     * @param game The current {@link Game}
     * @return Whether or not the given entity or other targetable is tagged by the specific attacker.
     */
    public static boolean isTargetTagged(Entity attacker, Targetable target, Game game) {
        boolean targetTagged = false;

        Entity te = null;
        if (target instanceof Entity) {
            te = (Entity) target;
        }

        // If this is an entity, we can see if it's tagged
        if (te != null) {
            targetTagged = te.getTaggedBy() == attacker.getId();
        } else { // Non entities will require us to look harder
            for (TagInfo ti : game.getTagInfo()) {
                if ((target.getId() == ti.target.getId()) &&
                        (ti.attackerId == attacker.getId())) {
                    return true;
                }
            }
        }

        return targetTagged;
    }


    public static ToHitData getImmobileMod(Targetable target) {
        return Compute.getImmobileMod(target, Entity.LOC_NONE, AimingMode.NONE);
    }

    /**
     * Gets the ToHitData associated with firing at an immobile target. Returns null if target isn't.
     * @param target The target being considered for firing
     * @param aimingAt The location of the unit being aimed at
     * @param aimingMode The aiming mode
     * @return The relevant ToHitData
     */
    @Nullable
    public static ToHitData getImmobileMod(Targetable target, int aimingAt, AimingMode aimingMode) {
        // if we are bombing hexes, they are not considered immobile.
        if ((target.getTargetType() == Targetable.TYPE_HEX_BOMB)
                || (target.getTargetType() == Targetable.TYPE_HEX_AERO_BOMB)) {
            return null;
        }

        if (target.isImmobile() || target.isBracing()) {
            if ((target instanceof Mech) && (aimingAt == Mech.LOC_HEAD) && aimingMode.isImmobile()) {
                return new ToHitData(3, "aiming at head");
            }
            ToHitData immobileTHD = new ToHitData(-4, "target immobile");
            if(target instanceof Tank) {
                // An "immobilized" but jumping CV is not actually immobile for targeting purposes
                // (See issue #3917)
                return ((Tank)target).moved == EntityMovementType.MOVE_JUMP ? null : immobileTHD;
            }
            return immobileTHD;
        }
        return null;
    }

    /**
     * Determines the to-hit modifier due to range for an attack with the
     * specified parameters. Includes minimum range, infantry 0-range mods, and
     * target stealth mods. Accounts for friendly C3 units.
     *
     * @return the modifiers
     */
    public static ToHitData getRangeMods(Game game, Entity ae, Mounted weapon, Mounted ammo,
                                         Targetable target) {
        WeaponType wtype = (WeaponType) weapon.getType();
        int[] weaponRanges = wtype.getRanges(weapon, ammo);
        boolean isAttackerInfantry = (ae instanceof Infantry);
        boolean isAttackerBA = (ae instanceof BattleArmor);
        boolean isWeaponInfantry = (wtype instanceof InfantryWeapon) && !wtype.hasFlag(WeaponType.F_TAG);
        boolean isSwarmOrLegAttack = (wtype instanceof InfantryAttack);
        boolean isIndirect = wtype.hasIndirectFire() && weapon.curMode().equals("Indirect");
        boolean useExtremeRange = game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_RANGE);
        boolean useLOSRange = game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_LOS_RANGE);
        //Naval C3 only provides full C3 range benefits to energy weapons and guided missiles
        boolean nc3EnergyGuided = ((wtype.hasFlag(WeaponType.F_ENERGY))
                || (wtype.getAtClass() == WeaponType.CLASS_CAPITAL_MISSILE)
                || (wtype.getAtClass() == WeaponType.CLASS_TELE_MISSILE)
                || (wtype.getAtClass() == WeaponType.CLASS_AR10)
                || (wtype.getAtClass() == WeaponType.CLASS_ATM)
                || (wtype.getAtClass() == WeaponType.CLASS_LRM)
                || (wtype.getAtClass() == WeaponType.CLASS_SRM)
                || (wtype.getAtClass() == WeaponType.CLASS_MML)
                || (wtype.getAtClass() == WeaponType.CLASS_THUNDERBOLT));

        if (ae.isAirborne()) {
            useExtremeRange = true;
            // This is a separate SO rule, and isn't implemented yet
            useLOSRange = false;
        }

        ToHitData mods = new ToHitData();

        Entity te = null;
        if (target instanceof Entity) {
            te = (Entity) target;
        }

        // We need to adjust the ranges for Centurion Weapon Systems: it's
        //  default range is 6/12/18 but that's only for units that are
        //  susceptible to CWS, for those that aren't the ranges are 1/2/3
        if (wtype.hasFlag(WeaponType.F_CWS)
            && ((te == null) || !te.hasQuirk("susceptible_cws"))) {
            weaponRanges[RangeType.RANGE_MINIMUM] = 0;
            weaponRanges[RangeType.RANGE_SHORT] = 1;
            weaponRanges[RangeType.RANGE_MEDIUM] = 2;
            weaponRanges[RangeType.RANGE_LONG] = 3;
            weaponRanges[RangeType.RANGE_EXTREME] = 4;
        }

        //
        // modifiy the ranges for PPCs when field inhibitors are turned off
        // TODO: See above, it should be coded elsewhere...
        //
        if (wtype.hasFlag(WeaponType.F_PPC)) {
            if (game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_PPC_INHIBITORS)) {
                if ((weapon.curMode() != null)
                    && weapon.curMode().equals("Field Inhibitor OFF")) {
                    weaponRanges[RangeType.RANGE_MINIMUM] = 0;
                }
            }
        }

        // Hotloaded weapons
        if (weapon.isHotLoaded()
            && game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_HOTLOAD)) {
            weaponRanges[RangeType.RANGE_MINIMUM] = 0;
        }

        // is water involved?
        Hex targHex = game.getBoard().getHex(target.getPosition());
        int targTop = target.relHeight();
        int targBottom = target.getElevation();

        boolean targetInPartialWater = false;
        boolean targetUnderwater = false;
        boolean weaponUnderwater = (ae.getLocationStatus(weapon.getLocation()) == ILocationExposureStatus.WET);
        if ((target.getTargetType() == Targetable.TYPE_ENTITY)
            && (targHex != null) && targHex.containsTerrain(Terrains.WATER)
            && (targBottom < 0)) {

                if (targTop >= 0) {
                    targetInPartialWater = true;
                } else {
                    targetUnderwater = true;
                }
        }

        // allow naval units on surface to be attacked from above or below
        if ((null != te) && (targBottom == 0)
            && (te.getUnitType() == UnitType.NAVAL)) {
            targetInPartialWater = true;
        }

        // allow naval units to target underwater units,
        // torpedo tubes are mounted underwater
        if ((targetUnderwater
                || (wtype.getAmmoType() == AmmoType.T_LRM_TORPEDO) || (wtype
                .getAmmoType() == AmmoType.T_SRM_TORPEDO))
            && (ae.getUnitType() == UnitType.NAVAL)) {
            weaponUnderwater = true;
            weaponRanges = wtype.getWRanges();
        }

        // allow ice to be cleared from below
        if ((targHex != null) && targHex.containsTerrain(Terrains.WATER)
            && (target.getTargetType() == Targetable.TYPE_HEX_CLEAR)) {
            targetInPartialWater = true;
        }

        if (weaponUnderwater) {
            weaponRanges = wtype.getWRanges();
            boolean MPM = false;
            if ((wtype.getAmmoType() == AmmoType.T_SRM)
                || (wtype.getAmmoType() == AmmoType.T_SRM_IMP)
                || (wtype.getAmmoType() == AmmoType.T_MRM)
                || (wtype.getAmmoType() == AmmoType.T_LRM)
                || (wtype.getAmmoType() == AmmoType.T_LRM_IMP)
                || (wtype.getAmmoType() == AmmoType.T_MML)) {
                AmmoType atype = (AmmoType) weapon.getLinked().getType();
                if (atype.getMunitionType().contains(AmmoType.Munitions.M_TORPEDO)) {
                    weaponRanges = wtype.getRanges(weapon);
                } else if (atype.getMunitionType().contains(AmmoType.Munitions.M_MULTI_PURPOSE)) {
                    weaponRanges = wtype.getRanges(weapon);
                    MPM = true;
                }
            }

            // HACK on ranges: for those without underwater range,
            // long == medium; iteration in rangeBracket() allows this
            if (weaponRanges[RangeType.RANGE_SHORT] == 0) {
                return new ToHitData(TargetRoll.IMPOSSIBLE,
                                     "Weapon cannot fire underwater.");
            }
            if (!targetUnderwater && !targetInPartialWater && !MPM) {
                // target on land or over water
                return new ToHitData(TargetRoll.IMPOSSIBLE,
                                     "Weapon underwater, but not target.");
            }
            // special case: mechs can only fire upper body weapons at surface
            // naval
            if ((te != null)
                && (te.getUnitType() == UnitType.NAVAL)
                && (ae instanceof Mech) && (ae.height() > 0)
                && (ae.getElevation() == -1)) {
                return new ToHitData(TargetRoll.IMPOSSIBLE,
                                     "Partially submerged mech cannot fire leg weapons at surface naval vessels.");
            }
        } else if (targetUnderwater) {
            return new ToHitData(TargetRoll.IMPOSSIBLE,
                                 "Target underwater, but not weapon.");
        } else if ((wtype.getAmmoType() == AmmoType.T_LRM_TORPEDO)
                   || (wtype.getAmmoType() == AmmoType.T_SRM_TORPEDO)) {
            // Torpedos only fire underwater.
            return new ToHitData(TargetRoll.IMPOSSIBLE,
                                 "Weapon can only fire underwater.");
        }

        // if Aero then adjust to standard ranges
        if (ae.isAero() && (ae.isAirborne()
            || (ae.usesWeaponBays() && game.getBoard().onGround()))) {
            weaponRanges = wtype.getATRanges();
        }
        // And if you're using bearings-only capital missiles, update the extreme range
        if (weapon.isInBearingsOnlyMode()) {
            weaponRanges = new int[] { Integer.MIN_VALUE, 12, 24, 40, RangeType.RANGE_BEARINGS_ONLY_OUT };
        }

        // determine base distance & range bracket
        int distance = Compute.effectiveDistance(game, ae, target, false);
        int range = RangeType.rangeBracket(distance, weaponRanges,
                                           useExtremeRange, useLOSRange);

        // Additional checks for LOS range and some weapon types, TO 85
        if (range == RangeType.RANGE_LOS) {
            // Swarm or leg attacks can't use LoS range
            if (isSwarmOrLegAttack) {
                range = RangeType.RANGE_OUT;
            }

            // MGs lack range for LOS Range, but don't have F_DIRECT_FIRE flag
            if (wtype instanceof MGWeapon) {
                range = RangeType.RANGE_OUT;
            }

            // AMS lack range for LOS Range, but don't have F_DIRECT_FIRE flag
            if (wtype.hasFlag(WeaponType.F_AMS)) {
                range = RangeType.RANGE_OUT;
            }

            // Flamers lack range for LOS Range, but don't have F_DIRECT_FIRE
            if (wtype.hasFlag(WeaponType.F_FLAMER)) {
                range = RangeType.RANGE_OUT;
            }

            int longRange = wtype.getRanges(weapon)[RangeType.RANGE_LONG];
            // No Missiles or Direct Fire Ballistics with range < 13
            if (wtype.hasFlag(WeaponType.F_MISSILE)
                || (wtype.hasFlag(WeaponType.F_DIRECT_FIRE)
                    && wtype.hasFlag(WeaponType.F_BALLISTIC))) {
                if (longRange < 13) {
                    range = RangeType.RANGE_OUT;
                }
            }
            // No Direct Fire Energy or Pulse with range < 7
            if (wtype.hasFlag(WeaponType.F_PULSE)
                || (wtype.hasFlag(WeaponType.F_ENERGY)
                    && wtype.hasFlag(WeaponType.F_DIRECT_FIRE))) {
                if (longRange < 7) {
                    range = RangeType.RANGE_OUT;
                }
            }
        }
        int maxRange = wtype.getMaxRange(weapon, ammo);

        // if aero and greater than max range then swith to range_out
        if ((ae.isAirborne() || (ae.usesWeaponBays() && game.getBoard()
                .onGround())) && (range > maxRange)) {
            range = RangeType.RANGE_OUT;
        }

        // Swarm/Leg attacks need to  be impossible, not auto-fail, so that the
        // attack can't even be attempted
        if (isSwarmOrLegAttack && (distance > 0)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE,
                    "Swarm/Leg attacks can "
                            + "only target units in the same hex!");
        }
        // short circuit if at zero range or out of range
        if ((range == RangeType.RANGE_OUT) && !isWeaponInfantry) {
            return new ToHitData(TargetRoll.AUTOMATIC_FAIL,
                                 "Target out of range");
        }

        // Infantry with infantry weapons (rifles, etc, i.e. not field pieces)
        //  and BattleArmor can fire at zero range, among other things
        if ((distance == 0)
            && (!isAttackerInfantry ||
                !(isWeaponInfantry || isSwarmOrLegAttack
                  || isAttackerBA))
            && !(ae.isAirborne())
            && !(ae.isBomber() && ((IBomber) ae).isVTOLBombing())
            && !((ae instanceof Dropship) && ((Dropship) ae).isSpheroid()
                 && !ae.isAirborne() && !ae.isSpaceborne())
            && !((ae instanceof Mech) && (((Mech) ae).getGrappled() == target
                .getId()))) {
            return new ToHitData(TargetRoll.AUTOMATIC_FAIL,
                                 "Only infantry weapons shoot at zero range");
        }

        // Account for "dead zones" between Aeros at different altitudes
        if (!Compute.useSpheroidAtmosphere(game, ae) && Compute.inDeadZone(game, ae, target)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Target in dead zone");
        }

        // find any c3 spotters that could help
        Entity c3spotter = Compute.findC3Spotter(game, ae, target);
        if (isIndirect) {
            c3spotter = ae; // no c3 when using indirect fire
        }

        if (isIndirect && indirectAttackImpossible(game, ae, target, wtype, weapon)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE,
                    Messages.getString("WeaponAttackAction.NoIndirectWithLOS"));
        }

        int c3dist = Compute.effectiveDistance(game, c3spotter, target, false);
        // C3 can't benefit from LOS range
        int c3range = RangeType.rangeBracket(c3dist, weaponRanges,
                useExtremeRange, false);

        /*
         * Tac Ops Extreme Range Rule p. 85 if the weapons normal range is
         * Extreme then C3 uses the next highest range bracket, i.e. medium
         * instead of short.
         */
        if ((range == RangeType.RANGE_EXTREME) && (c3range < range)) {
            c3range++;
        }

        // determine which range we're using
        int usingRange = Math.min(range, c3range);

        // add range modifier, C3 can't be used with LOS Range
        if ((usingRange == range) || (range == RangeType.RANGE_LOS) || (ae.hasNavalC3() && !nc3EnergyGuided)) {
            // Ensure usingRange is set to range, ie with C3
            usingRange = range;
            // Naval C3 adjustment for ballistic and unguided weapons
            if ((ae.hasNavalC3() && !nc3EnergyGuided) && (c3range < range)) {
                if (((range == RangeType.RANGE_SHORT) || (range == RangeType.RANGE_MINIMUM))
                        && (ae.getShortRangeModifier() != 0)) {
                    mods.addModifier((ae.getShortRangeModifier() / 2), "NC3 modified short range");
                } else if (range == RangeType.RANGE_MEDIUM) {
                    mods.addModifier((ae.getMediumRangeModifier() / 2), "NC3 modified medium range");
                } else if (range == RangeType.RANGE_LONG) {
                    mods.addModifier((ae.getLongRangeModifier() / 2), "NC3 modified long range");
                } else if (range == RangeType.RANGE_EXTREME) {
                    mods.addModifier((ae.getExtremeRangeModifier() / 2), "NC3 modified Extreme range");
                }
            } else {
                // no c3 adjustment
                if (((range == RangeType.RANGE_SHORT) || (range == RangeType.RANGE_MINIMUM))
                        && (ae.getShortRangeModifier() != 0)) {
                    mods.addModifier(ae.getShortRangeModifier(), "short range");
                } else if (range == RangeType.RANGE_MEDIUM) {
                    // Right now, the range-mod affecting targeting systems DON'T
                    // affect medium range, so we won't add that here ever.
                    mods.addModifier(ae.getMediumRangeModifier(), "medium range");
                } else if (range == RangeType.RANGE_LONG) {
                    // Protos that loose head sensors can't shoot long range.
                    if ((ae instanceof Protomech)
                            && (2 == ((Protomech) ae)
                            .getCritsHit(Protomech.LOC_HEAD))) {
                        mods.addModifier(TargetRoll.IMPOSSIBLE,
                                         "No long range attacks with destroyed head sensors.");
                    } else {
                        mods.addModifier(ae.getLongRangeModifier(), "long range");
                    }
                } else if (range == RangeType.RANGE_EXTREME) {
                    // Protos that loose head sensors can't shoot extreme range.
                    if ((ae instanceof Protomech)
                            && (2 == ((Protomech) ae)
                            .getCritsHit(Protomech.LOC_HEAD))) {
                        mods.addModifier(TargetRoll.IMPOSSIBLE,
                                         "No extreme range attacks with destroyed head sensors.");
                    } else {
                        mods.addModifier(ae.getExtremeRangeModifier(),
                                         "extreme range");
                    }
                } else if (range == RangeType.RANGE_LOS) {
                    // Protos that loose head sensors can't shoot LOS range.
                    if ((ae instanceof Protomech)
                            && (2 == ((Protomech) ae)
                            .getCritsHit(Protomech.LOC_HEAD))) {
                        mods.addModifier(TargetRoll.IMPOSSIBLE,
                                         "No LOS range attacks with destroyed head sensors.");
                    } else {
                        mods.addModifier(ae.getLOSRangeModifier(),
                                         "LOS range");
                    }
                }
            }
        } else {
            // report c3 adjustment
            if ((c3range == RangeType.RANGE_SHORT)
                || (c3range == RangeType.RANGE_MINIMUM)) {
                mods.addModifier(ae.getShortRangeModifier(),
                                 "short range due to C3 spotter");
            } else if (c3range == RangeType.RANGE_MEDIUM) {
                mods.addModifier(ae.getMediumRangeModifier(),
                                 "medium range due to C3 spotter");
            } else if (c3range == RangeType.RANGE_LONG) {
                mods.addModifier(ae.getLongRangeModifier(),
                                 "long range due to C3 spotter");
            }
        }

        // add minimum range modifier (only for ground-to-ground attacks)
        int minRange = weaponRanges[RangeType.RANGE_MINIMUM];
        if ((minRange > 0) && (distance <= minRange)
            && Compute.isGroundToGround(ae, target)) {
            int minPenalty = (minRange - distance) + 1;
            mods.addModifier(minPenalty, "minimum range");
        }

        // if this is an infantry weapon then we use a whole different
        // calculation
        // to figure out range, so overwrite whatever we have at this point
        if (isWeaponInfantry) {
            mods = Compute.getInfantryRangeMods(Math.min(distance, c3dist),
                    (InfantryWeapon) wtype,
                    (ae instanceof Infantry) ? ((Infantry) ae).getSecondaryWeapon() : null,
                            weaponUnderwater);

            int rangeModifier = mods.getValue();
            if (rangeModifier == TargetRoll.AUTOMATIC_FAIL) {
                usingRange = RangeType.RANGE_OUT;
            } else if (rangeModifier == 0) {
                usingRange = RangeType.RANGE_SHORT;
            } else if (rangeModifier <= 2) {
                usingRange = RangeType.RANGE_MEDIUM;
            } else if (rangeModifier <= 4) {
                usingRange = RangeType.RANGE_LONG;
            } else {
                usingRange = RangeType.RANGE_EXTREME;
            }
        }

        // add any target stealth modifier
        if (target instanceof Entity) {
            TargetRoll tmpTR = ((Entity) target).getStealthModifier(usingRange,
                                                                    ae);
            if ((tmpTR != null) && (tmpTR.getValue() != 0)) {
                mods.append(((Entity) target)
                                    .getStealthModifier(usingRange, ae));
            }
        }

        return mods;
    }

    /**
     * Calculate the range modifiers for a conventional infantry attack.
     *
     * @param distance - range to target
     * @param wpn - the weapon used to calculate range -- secondary if 2/squad, otherwise primary
     * @param secondary - the secondary weapon, if any. Range zero penalties apply even if primary is used for range
     * @param underwater - underwater range is half, rounded down
     * @return - all modifiers for range
     */
    public static ToHitData getInfantryRangeMods(int distance, InfantryWeapon wpn,
            InfantryWeapon secondary, boolean underwater) {
        ToHitData mods = new ToHitData();
        int range = wpn.getInfantryRange();
        if (underwater) {
            range /= 2;
        }
        int mod = 0;

        switch (range) {
            case 0:
                if (distance > 0) {
                    return new ToHitData(TargetRoll.AUTOMATIC_FAIL,
                                         "Target out of range");
                } else if (distance == 0) {
                    mod = 0;
                }
                break;
            case 1:
                if (distance > 3) {
                    return new ToHitData(TargetRoll.AUTOMATIC_FAIL,
                                         "Target out of range");
                } else if (distance == 0) {
                    mod = -2;
                } else if (distance == 2) {
                    mod = 2;
                } else if (distance == 3) {
                    mod = 4;
                }
                break;
            case 2:
                if (distance > 6) {
                    return new ToHitData(TargetRoll.AUTOMATIC_FAIL,
                                         "Target out of range");
                } else if (distance > 4) {
                    mod = 4;
                } else if (distance > 2) {
                    mod = 2;
                } else if (distance == 0) {
                    mod = -2;
                }
                break;
            case 3:
                if (distance > 9) {
                    return new ToHitData(TargetRoll.AUTOMATIC_FAIL,
                                         "Target out of range");
                } else if (distance > 6) {
                    mod = 4;
                } else if (distance > 3) {
                    mod = 2;
                } else if (distance == 0) {
                    mod = -2;
                }
                break;
            case 4:
                if (distance > 12) {
                    return new ToHitData(TargetRoll.AUTOMATIC_FAIL,
                                         "Target out of range");
                } else if (distance > 10) {
                    mod = 4;
                } else if (distance > 8) {
                    mod = 3;
                } else if (distance > 6) {
                    mod = 2;
                } else if (distance > 4) {
                    mod = 1;
                } else if (distance == 0) {
                    mod = -2;
                }
                break;
            case 5:
                if (distance > 15) {
                    return new ToHitData(TargetRoll.AUTOMATIC_FAIL,
                                         "Target out of range");
                } else if (distance > 12) {
                    mod = 4;
                } else if (distance > 10) {
                    mod = 3;
                } else if (distance > 7) {
                    mod = 2;
                } else if (distance > 5) {
                    mod = 1;
                } else if (distance == 0) {
                    mod = -1;
                }
                break;
            case 6:
                if (distance > 18) {
                    return new ToHitData(TargetRoll.AUTOMATIC_FAIL,
                                         "Target out of range");
                } else if (distance > 15) {
                    mod = 5;
                } else if (distance > 12) {
                    mod = 4;
                } else if (distance > 9) {
                    mod = 2;
                } else if (distance > 6) {
                    mod = 1;
                } else if (distance == 0) {
                    mod = -1;
                }
                break;
            case 7:
                if (distance > 21) {
                    return new ToHitData(TargetRoll.AUTOMATIC_FAIL,
                                         "Target out of range");
                } else if (distance > 17) {
                    mod = 6;
                } else if (distance > 14) {
                    mod = 4;
                } else if (distance > 10) {
                    mod = 2;
                } else if (distance > 7) {
                    mod = 1;
                } else if (distance == 0) {
                    mod = -1;
                }
                break;
            default:
                return new ToHitData(TargetRoll.AUTOMATIC_FAIL,
                                     "Target out of range");
        }

        // a bunch of special conditions at range 0
        // penalties due to point blank or encumbering apply for secondary weapon even if
        // primary is used to determine range
        if (distance == 0) {

            if (wpn.hasFlag(WeaponType.F_INF_POINT_BLANK)
                    || (secondary != null && secondary.hasFlag(WeaponType.F_INF_POINT_BLANK))) {
                mods.addModifier(1, "point blank weapon");
            }
            if (wpn.hasFlag(WeaponType.F_INF_ENCUMBER) || (wpn.getCrew() > 1)
                    || (secondary != null
                        && (secondary.hasFlag(WeaponType.F_INF_ENCUMBER)
                                || secondary.getCrew() > 1))) {
                mods.addModifier(1, "point blank support weapon");
            }

            if (wpn.hasFlag(WeaponType.F_INF_BURST)) {
                mods.addModifier(-1, "point blank burst fire weapon");
            }
        }

        // TODO: we need to adjust for stealth modifiers for Chameleon LPS but
        // we don't have range brackets
        // http://bg.battletech.com/forums/index.php/topic,27433.new.html#new

        if (mod != 0) {
            mods.addModifier(mod, "infantry range");
        }

        return mods;
    }

    /**
     * Finds the effective distance between an attacker and a target. Includes
     * the distance bonus if the attacker and target are in the same building
     * and on different levels. Also takes account of altitude differences
     *
     * @return the effective distance
     */
    public static int effectiveDistance(final Game game, final Entity attacker,
                                        final @Nullable Targetable target) {
        return Compute.effectiveDistance(game, attacker, target, false);
    }

    /**
     * If BAP setting is enbabled and unit has a BAP.
     * check if target is within BAP range.
     * check if entity is Affected by ECM.
     *
     * @return if target si in range and entity is not affected
     */
    public static boolean bapInRange(Game game, Entity entity, Entity target) {
        return (target != null)
                && entity.hasBAP()
                && (entity.getBAPRange() >= Compute.effectiveDistance(game, entity, target))
                && !ComputeECM.isAffectedByECM(entity, entity.getPosition(), target.getPosition());
    }

    /**
     * Finds the effective distance between an attacker and a target. Includes
     * the distance bonus if the attacker and target are in the same building
     * and on different levels. Also takes account of altitude differences
     *
     * @return the effective distance
     */
    public static int effectiveDistance(final Game game, final Entity attacker,
                                        final @Nullable Targetable target,
                                        final boolean useGroundDistance) {
        if (target == null) {
            LogManager.getLogger().error("Attempted to determine the effective distance to a null target");
            return 0;
        } else if (Compute.isAirToGround(attacker, target)
                || (attacker.isBomber() && target.getTargetType() == Targetable.TYPE_HEX_AERO_BOMB)) {
            // always a distance of zero
            return 0;
        }

        Vector<Coords> attackPos = new Vector<>();
        attackPos.add(attacker.getPosition());
        Vector<Coords> targetPos = new Vector<>();
        targetPos.add(target.getPosition());
        // if a grounded dropship is the attacker, then it gets to choose the
        // best secondary position for LoS
        if ((attacker instanceof Dropship) && !attacker.isAirborne() && !attacker.isSpaceborne()) {
            attackPos = new Vector<>();
            for (int key : attacker.getSecondaryPositions().keySet()) {
                attackPos.add(attacker.getSecondaryPositions().get(key));
            }
        }
        if ((target instanceof Dropship) && !target.isAirborne() && !((Entity) target).isSpaceborne()) {
            targetPos = new Vector<>();
            for (final int key : target.getSecondaryPositions().keySet()) {
                targetPos.add(target.getSecondaryPositions().get(key));
            }
        }
        int distance = Integer.MAX_VALUE;
        for (Coords apos : attackPos) {
            for (Coords tpos : targetPos) {
                if ((tpos != null) && (apos != null)
                    && (apos.distance(tpos) < distance)) {
                    distance = apos.distance(tpos);
                }
            }
        }

        if (Compute.isGroundToAir(attacker, target) && (target instanceof Entity)) {
            // distance is determined by closest point on flight path
            distance = attacker.getPosition().distance(getClosestFlightPath(attacker.getId(),
                    attacker.getPosition(), (Entity) target));

            // if the ground attacker uses weapon bays and we are on a
            // ground map, then we will divide this distance by 16
            // This is totally crazy, but I don't see how else to do it. Use
            // the unofficial
            // "grounded dropships use individual weapons" for sanity.
            if (attacker.usesWeaponBays() && game.getBoard().onGround()) {
                distance = (int) Math.ceil(distance / 16.0);
            }
        }

        // if this is an air-to-air attack on the ground map, then divide
        // distance by 16
        if (Compute.isAirToAir(attacker, target) && game.getBoard().onGround() && !useGroundDistance) {
            distance = (int) Math.ceil(distance / 16.0);
        }

        // If the attack is completely inside a building, add the difference
        // in elevations between the attacker and target to the range.
        // TODO: should the player be explicitly notified?
        if (Compute.isInSameBuilding(game, attacker, target)) {
            int aElev = attacker.getElevation();
            int tElev = target.getElevation();
            distance += Math.abs(aElev - tElev);
        }

        // air-to-air attacks add one for altitude differences
        if (Compute.isAirToAir(attacker, target) && !attacker.isSpaceborne()) {
            int aAlt = attacker.getAltitude();
            int tAlt = target.getAltitude();
            if (target.isAirborneVTOLorWIGE()) {
                tAlt++;
            }
            distance += Math.abs(aAlt - tAlt);
        }

        if (Compute.isGroundToAir(attacker, target)) {
            if (attacker.usesWeaponBays() && game.getBoard().onGround()) {
                distance += (target.getAltitude());
            } else {
                distance += (2 * target.getAltitude());
            }
        }

        // Attacking a ground unit while dropping
        if (attacker.isDropping() && target.getAltitude() == 0) {
            distance += (2 * attacker.getAltitude());
        }

        return distance;
    }

    /**
     * @param aPos the attacker's position
     * @param te the target entity
     * @return the closest position along <code>te</code>'s flight path to <code>aPos</code>. In
     * the case of multiple equi-distance positions, the first one is picked unless
     * <code>te</code>'s playerPickedPassThrough position is non-null.
     */
    public static @Nullable Coords getClosestFlightPath(int attackerId, Coords aPos, Entity te) {
        Coords finalPos = te.getPosition();
        if (te.getPlayerPickedPassThrough(attackerId) != null) {
            finalPos = te.getPlayerPickedPassThrough(attackerId);
        }
        int distance = Integer.MAX_VALUE;
        if (finalPos != null) {
            distance = aPos.distance(finalPos);
        }
        // don't return zero distance Coords, but rather the Coords immediately
        // before this
        // This is necessary to determine angle of attack and arc information
        // for direct fly-overs
        for (Coords c : te.getPassedThrough()) {
            if (!aPos.equals(c) && (c != null)
                && ((aPos.distance(c) < distance) || (distance == 0))) {
                finalPos = c;
                distance = aPos.distance(c);
            }
        }
        return finalPos;
    }

    public static int getClosestFlightPathFacing(int attackerId,
            Coords aPos, Entity te) {

        Coords finalPos = te.getPosition();
        if (te.getPlayerPickedPassThrough(attackerId) != null) {
            finalPos = te.getPlayerPickedPassThrough(attackerId);
        }
        int distance = Integer.MAX_VALUE;
        if (finalPos != null) {
            distance = aPos.distance(finalPos);
        }
        int finalFacing = te.getFacing();
        // don't return zero distance Coords, but rather the Coords immediately
        // before this
        // This is necessary to determine angle of attack and arc information
        // for direct fly-overs
        for (int i = 0; i < te.getPassedThrough().size(); i++) {
            Coords c = te.getPassedThrough().get(i);
            if (!aPos.equals(c) && (c != null)
                && ((aPos.distance(c) < distance) || (distance == 0))) {
                finalFacing = te.getPassedThroughFacing().get(i);
                finalPos = c;
                distance = aPos.distance(c);
            } else if (c.equals(finalPos)) {
                finalFacing = te.getPassedThroughFacing().get(i);
            }
        }
        return finalFacing;
    }

    /**
     * WOR: Need this function to find out where my nova stuff doesn't work.
     * Delete it if nova works but remember to alter the /nova debug server
     * command.
     */
    public static Entity exposed_findC3Spotter(Game game, Entity attacker,
                                               Targetable target) {
        return findC3Spotter(game, attacker, target);
    }

    /**
     * Function that attempts to find one TAG spotter, or the best TAG spotter,
     * @param game
     * @param attacker should be an artillery unit
     * @param target (if null, do not return a spotter!)
     * @param stopAtFirst if we only want to know that there is one, not which is best
     * @return The Spotter.
     */
    public static Entity findTAGSpotter(Game game, Entity attacker, Targetable target, boolean stopAtFirst) {
        if (target == null) {
            return null;
        }
        boolean debug = LogManager.getLogger().isDebugEnabled();
        StringBuilder msg = (debug) ? new StringBuilder("Looking for TAG spotter for ")
                .append(attacker.getDisplayName())
                .append(" targeting ")
                .append(target.getDisplayName())
                : null;

        Entity spotter = null;
        int distance = -1;

        // Compute friendly spotters
        for (Entity friend : game.getPlayerEntities(attacker.getOwner(), true)) {

            if (friend == null
                    || !friend.isDeployed()
                    || friend.isOffBoard()
                    || (friend.getTransportId() != Entity.NONE)
                    || friend.isAero() // Much higher bar for TAGging
            ) {
                continue; // useless to us...
            }

            Mounted tag = null;
            int range = 0;
            for (Mounted m : friend.getWeaponList()) {
                WeaponType wtype = ((WeaponType) m.getType());
                if (wtype.hasFlag(WeaponType.F_TAG)) {
                    tag = m;
                    range = wtype.getLongRange();
                    break;
                }
            }
            if (tag == null) {
                continue;
            }

            int friendRange =  Compute.effectiveDistance(game, friend, target, false);
            int ownRange = Compute.effectiveDistance(game, attacker, target, false);
            // Friend has to be as close as their max running speed * flight time, + TAG range, + 8
            int taggingRange = ((1 + Compute.turnsTilHit(ownRange)) * friend.getWalkMP()) + range + 8;
            if (debug) {
                msg.append("\n").append(friend.getDisplayName()).append(" has TAG at ")
                        .append(friendRange).append(" from target; must be within ")
                        .append(taggingRange).append(" to be able to TAG this target for us.");
            }

            // Need a target hex within 8 of the main target, and within shooting distance of the spotter.
            if (friendRange > taggingRange) {
                continue;
            }

            // is this guy a better spotter?
            if ((spotter == null )
                    || range < distance) {
                if (debug) {
                    msg.append("\n").append(friend.getDisplayName()).append(" is a good candidate.");
                }
                spotter = friend;
                distance = friendRange;
                if (stopAtFirst) {
                    break;
                }
            }
        }
        if (debug) {
            msg.append("\nFinal result: ")
                    .append((spotter == null) ? "no TAG friendly in range" : spotter.getDisplayName())
                    .append("!");
            LogManager.getLogger().debug(msg.toString());
        }

        return spotter;
    }

    /**
     * find a c3, c3i, NC3, or nova spotter that is closer to the target than the
     * attacker.
     *
     * @param game The current {@link Game}
     * @param attacker
     * @param target
     * @return A closer C3/C3i/Nova spotter, or the attacker if no spotters are found
     */
    private static Entity findC3Spotter(Game game, Entity attacker,
            Targetable target) {
        // no available C3-like system
        if (!attacker.hasC3() && !attacker.hasC3i()
                && !attacker.hasActiveNovaCEWS() && !attacker.hasNavalC3()) {
            return attacker;
        }

        ArrayList<Entity> network = new ArrayList<>();

        // Compute friends in network
        for (Entity friend : game.getEntitiesVector()) {

            if (attacker.equals(friend)
                    || !attacker.onSameC3NetworkAs(friend, true)
                    || !friend.isDeployed()
                    || (friend.getTransportId() != Entity.NONE)) {
                continue; // useless to us...
            }

            // Must have LoS, Compute.canSee considers sensors and visual range
            if (!LosEffects.calculateLOS(game, friend, target).canSee()) {
                continue;
            }

            int buddyRange = Compute.effectiveDistance(game, friend, target,
                    false);

            boolean added = false;
            // put everyone in the C3 network into a list and sort it by range.
            for (int pos = 0; pos < network.size(); pos++) {
                if (Compute.effectiveDistance(game, network.get(pos), target,
                        false) >= buddyRange) {
                    network.add(pos, friend);
                    added = true;
                    break;
                }
            }

            if (!added) {
                network.add(friend);
            }
        }

        // ensure network connectivity
        List<ECMInfo> allECMInfo = ComputeECM.computeAllEntitiesECMInfo(game
                .getEntitiesVector());
        int position = 0;
        for (Entity spotter : network) {
            for (int count = position++; count < network.size(); count++) {
                if (Compute.canCompleteNodePath(spotter, attacker, network,
                        count, allECMInfo)) {
                    return spotter;
                }
            }
        }

        return attacker;
    }

    /**
     * Looks through the network list to ensure that the given Entity is
     * connected to the network.
     *
     * @param start
     * @param end
     * @param network
     * @param startPosition
     * @return
     */
    private static boolean canCompleteNodePath(Entity start, Entity end,
            ArrayList<Entity> network, int startPosition,
            List<ECMInfo> allECMInfo) {

        Entity spotter = network.get(startPosition);

        // ECMInfo for line between spotter's position and start's position
        ECMInfo spotterStartECM = ComputeECM.getECMEffects(spotter,
                start.getPosition(), spotter.getPosition(), true, allECMInfo);

        // Check for ECM between spotter and start
        boolean isC3BDefeated = start.hasBoostedC3()
                && (spotterStartECM != null) && spotterStartECM.isAngelECM();
        boolean isNovaDefeated = start.hasNovaCEWS()
                && (spotterStartECM != null) && spotterStartECM.isNovaECM();
        boolean isC3Defeated = !(start.hasBoostedC3() || start.hasNovaCEWS())
                && (spotterStartECM != null) && spotterStartECM.isECM();
        if (isC3BDefeated || isNovaDefeated || isC3Defeated) {
            return false;
        }

        // ECMInfo for line between spotter's position and end's position
        ECMInfo spotterEndECM = ComputeECM.getECMEffects(spotter,
                spotter.getPosition(), end.getPosition(), true, allECMInfo);
        isC3BDefeated = start.hasBoostedC3() && (spotterEndECM != null)
                && spotterEndECM.isAngelECM();
        isNovaDefeated = start.hasNovaCEWS() && (spotterEndECM != null)
                && spotterEndECM.isNovaECM();
        isC3Defeated = !(start.hasBoostedC3() || start.hasNovaCEWS())
                && (spotterEndECM != null) && spotterEndECM.isECM();
        // If there's no ECM between spotter and end, we're done
        if (!(isC3BDefeated || isNovaDefeated || isC3Defeated)) {
            return true;
        }

        for (++startPosition; startPosition < network.size(); startPosition++) {
            if (Compute.canCompleteNodePath(spotter, end, network,
                    startPosition, allECMInfo)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Gets the modifiers, if any, that the mech receives from being prone.
     *
     * @return any applicable modifiers due to being prone
     */
    public static ToHitData getProneMods(Game game, Entity attacker,
                                         int weaponId) {
        if (!attacker.isProne()) {
            return null; // no modifier
        }
        ToHitData mods = new ToHitData();
        Mounted weapon = attacker.getEquipment(weaponId);
        if (attacker.entityIsQuad()) {
            int legsDead = ((Mech) attacker).countBadLegs();
            if (legsDead == 0 && !attacker.hasHipCrit()) {
                // No legs destroyed and no hip crits: no penalty and can fire all weapons
                return null; // no modifier
            } else if (legsDead >= 3) {
                return new ToHitData(TargetRoll.IMPOSSIBLE,
                                     "Prone with three or more legs destroyed.");
            }
            // we have one or two dead legs...

            // Need an intact front leg
            if (attacker.isLocationBad(Mech.LOC_RARM)
                && attacker.isLocationBad(Mech.LOC_LARM)) {
                return new ToHitData(TargetRoll.IMPOSSIBLE,
                                     "Prone with both front legs destroyed.");
            }

            // front leg-mounted weapons have addidional trouble
            if ((weapon.getLocation() == Mech.LOC_RARM) || (weapon.getSecondLocation() == Mech.LOC_RARM)
                || (weapon.getLocation() == Mech.LOC_LARM || (weapon.getSecondLocation() == Mech.LOC_LARM))) {
                int otherArm = (weapon.getLocation() == Mech.LOC_RARM
                        || weapon.getSecondLocation() == Mech.LOC_RARM) ? Mech.LOC_LARM : Mech.LOC_RARM;
                // check previous attacks for weapons fire from the other arm
                if (Compute.isFiringFromArmAlready(game, weaponId, attacker, otherArm)) {
                    return new ToHitData(TargetRoll.IMPOSSIBLE,
                                         "Prone and firing from other front leg already.");
                }
            }
            // can't fire rear leg weapons
            if ((weapon.getLocation() == Mech.LOC_LLEG)
                || (weapon.getLocation() == Mech.LOC_RLEG)) {
                return new ToHitData(TargetRoll.IMPOSSIBLE,
                                     "Can't fire rear leg-mounted weapons while prone with destroyed legs.");
            }
            if (((Mech) attacker).getCockpitType() == Mech.COCKPIT_DUAL
                    && attacker.getCrew().hasDedicatedGunner()) {
                mods.addModifier(1, "attacker prone");
            } else {
                mods.addModifier(2, "attacker prone");
            }
        } else {
            int l3ProneFiringArm = Entity.LOC_NONE;

            if (attacker.isLocationBad(Mech.LOC_RARM)
                || attacker.isLocationBad(Mech.LOC_LARM)) {
                if (game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_PRONE_FIRE)) {
                    // Can fire with only one arm
                    if (attacker.isLocationBad(Mech.LOC_RARM)
                        && attacker.isLocationBad(Mech.LOC_LARM)) {
                        return new ToHitData(TargetRoll.IMPOSSIBLE,
                                             "Prone with both arms destroyed.");
                    }

                    l3ProneFiringArm = attacker.isLocationBad(Mech.LOC_RARM) ? Mech.LOC_LARM
                                                                             : Mech.LOC_RARM;
                } else {
                    // must have an arm intact
                    return new ToHitData(TargetRoll.IMPOSSIBLE,
                                         "Prone with one or both arms destroyed.");
                }
            }

            // arm-mounted weapons have addidional trouble
            if ((weapon.getLocation() == Mech.LOC_RARM) || (weapon.getSecondLocation() == Mech.LOC_RARM)
                || (weapon.getLocation() == Mech.LOC_LARM) || (weapon.getSecondLocation() == Mech.LOC_LARM)) {
                if (l3ProneFiringArm == weapon.getLocation()
                        || (weapon.getSecondLocation() != Entity.NONE && l3ProneFiringArm == weapon.getSecondLocation())) {
                    return new ToHitData(TargetRoll.IMPOSSIBLE,
                                         "Prone and propping up with this arm.");
                }

                int otherArm = (weapon.getLocation() == Mech.LOC_RARM
                        || weapon.getSecondLocation() == Mech.LOC_RARM) ? Mech.LOC_LARM : Mech.LOC_RARM;
                // check previous attacks for weapons fire from the other arm
                if (Compute.isFiringFromArmAlready(game, weaponId, attacker,
                                                   otherArm)) {
                    return new ToHitData(TargetRoll.IMPOSSIBLE,
                                         "Prone and firing from other arm already.");
                }
            }
            // can't fire leg weapons
            if ((weapon.getLocation() == Mech.LOC_LLEG)
                || (weapon.getLocation() == Mech.LOC_RLEG)) {
                return new ToHitData(TargetRoll.IMPOSSIBLE,
                                     "Can't fire leg-mounted weapons while prone.");
            }
            if (((Mech) attacker).getCockpitType() == Mech.COCKPIT_DUAL
                    && attacker.getCrew().hasDedicatedGunner()) {
                mods.addModifier(1, "attacker prone");
            } else {
                mods.addModifier(2, "attacker prone");
            }

            if (l3ProneFiringArm != Entity.LOC_NONE) {
                mods.addModifier(1, "attacker propping on single arm");
            }
        }
        return mods;
    }

    /**
     * Checks to see if there is an attack previous to the one with this weapon
     * from the specified arm.
     *
     * @return true if there is a previous attack from this arm
     */
    private static boolean isFiringFromArmAlready(Game game, int weaponId,
                                                  final Entity attacker, int armLoc) {
        int torsoLoc = Mech.getInnerLocation(armLoc);
        for (Enumeration<EntityAction> i = game.getActions(); i.hasMoreElements(); ) {
            EntityAction ea = i.nextElement();
            if (!(ea instanceof WeaponAttackAction)) {
                continue;
            }
            WeaponAttackAction prevAttack = (WeaponAttackAction) ea;
            // stop when we get to this weaponattack (does this always work?)
            if ((prevAttack.getEntityId() == attacker.getId())
                && (prevAttack.getWeaponId() == weaponId)) {
                break;
            }
            if (((prevAttack.getEntityId() == attacker.getId()) && (attacker
                                                                            .getEquipment(prevAttack.getWeaponId())
                                                                            .getLocation() == armLoc))
                || ((prevAttack.getEntityId() == attacker.getId())
                    && (attacker.getEquipment(prevAttack.getWeaponId())
                                .getLocation() == torsoLoc) && attacker
                            .getEquipment(prevAttack.getWeaponId()).isSplit())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Adds any damage modifiers from arm critical hits or sensor damage.
     *
     * @return Any applicable damage modifiers
     */
    public static ToHitData getDamageWeaponMods(Entity attacker, Mounted weapon) {
        ToHitData mods = new ToHitData();
        if (attacker instanceof Protomech) {
            // Head criticals add to target number of all weapons.
            int hits = ((Protomech) attacker).getCritsHit(Protomech.LOC_HEAD);
            if (hits > 0) {
                mods.addModifier(hits, hits + " head critical(s)");
            }

            // Arm mounted (and main gun) weapons get DRMs from arm crits.
            switch (weapon.getLocation()) {
                case Protomech.LOC_LARM:
                case Protomech.LOC_RARM:
                    hits = ((Protomech) attacker).getCritsHit(weapon
                                                                      .getLocation());
                    if (hits > 0) {
                        mods.addModifier(hits, hits + " arm critical(s)");
                    }
                    break;
                case Protomech.LOC_MAINGUN:
                    // Main gun is affected by crits in *both* arms.
                    hits = ((Protomech) attacker)
                            .getCritsHit(Protomech.LOC_LARM);
                    hits += ((Protomech) attacker)
                            .getCritsHit(Protomech.LOC_RARM);
                    if (4 == hits) {
                        mods.addModifier(TargetRoll.IMPOSSIBLE,
                                         "Cannot fire main gun with no arms.");
                    } else if (hits > 0) {
                        mods.addModifier(hits, hits + " arm critical(s)");
                    }
                    break;
            }

        } // End attacker-is-Protomech

        // only mechs have arm actuators - for those, we check whether
        // there is arm actuator damage
        else if (attacker instanceof Mech) {
            // split weapons need to account for arm actuator hits, too
            // see bug 1363690
            // we don't need to specifically check for weapons split between
            // torso and leg, because for those, the location stored in the
            // Mounted is the leg.
            int location = weapon.getLocation();
            if (weapon.isSplit()) {
                switch (location) {
                    case Mech.LOC_LT:
                        location = Mech.LOC_LARM;
                        break;
                    case Mech.LOC_RT:
                        location = Mech.LOC_RARM;
                        break;
                    default:
                }
            }

            // only arms can have damaged arm actuators
            if ((location == Mech.LOC_LARM || location == Mech.LOC_RARM) &&
                    (attacker.braceLocation() != location)) {
                if (attacker.getBadCriticals(CriticalSlot.TYPE_SYSTEM,
                                             Mech.ACTUATOR_SHOULDER, location) > 0) {
                    mods.addModifier(4, "shoulder actuator destroyed");
                } else {
                    // no shoulder hits, add other arm hits
                    int actuatorHits = 0;
                    if (attacker.getBadCriticals(CriticalSlot.TYPE_SYSTEM,
                                                 Mech.ACTUATOR_UPPER_ARM, location) > 0) {
                        actuatorHits++;
                    }
                    if (attacker.getBadCriticals(CriticalSlot.TYPE_SYSTEM,
                                                 Mech.ACTUATOR_LOWER_ARM, location) > 0) {
                        actuatorHits++;
                    }
                    if (actuatorHits > 0) {
                        mods.addModifier(actuatorHits, actuatorHits
                                                       + " destroyed arm actuators");
                    }
                }
            }
        }

        // sensors critical hit to attacker
        int sensorHits = attacker.getBadCriticals(CriticalSlot.TYPE_SYSTEM,
                                                  Mech.SYSTEM_SENSORS, Mech.LOC_HEAD);
        if ((attacker instanceof Mech)
            && (((Mech) attacker).getCockpitType() == Mech.COCKPIT_TORSO_MOUNTED)) {
            sensorHits += attacker.getBadCriticals(CriticalSlot.TYPE_SYSTEM,
                                                   Mech.SYSTEM_SENSORS, Mech.LOC_CT);
            if (sensorHits > 1) {
                mods.addModifier(4, "attacker sensors badly damaged");
            } else if (sensorHits > 0) {
                mods.addModifier(2, "attacker sensors damaged");
            }
        } else if (sensorHits > 0) {
            if (attacker instanceof Mech && ((Mech) attacker).getCockpitType() == Mech.COCKPIT_DUAL
                    && attacker.getCrew().hasDedicatedGunner()) {
                mods.addModifier(1, "attacker sensors damaged");
            } else {
                mods.addModifier(2, "attacker sensors damaged");
            }
        }

        // if partial sensor/stabilizer/fcs/cic repairs are present the shot will be more difficult
        if (attacker.getPartialRepairs() != null) {
            if (attacker.getPartialRepairs().booleanOption("sensors_1_crit")) {
                mods.addModifier(1, "sensor damage");
            }
            if (attacker.getPartialRepairs().booleanOption("mech_sensors_2_crit")) {
                mods.addModifier(2, "sensor damage");
            }
            if (attacker.getPartialRepairs().booleanOption("veh_stabilizer_crit")) {
                mods.addModifier(1, "stabilizer damage");
            }
            if (attacker.getPartialRepairs().booleanOption("aero_cic_fcs_replace")) {
                mods.addModifier(1, "misreplaced cic/fcs equipment");
            }
            if (attacker.getPartialRepairs().booleanOption("aero_cic_fcs_crit")) {
                 mods.addModifier(1, "faulty cic/fcs repairs");
            }
        }

        return mods;
    }

    /**
     * Determines if the current target is a secondary target, and if so,
     * returns the appropriate modifier.
     *
     * @return The secondary target modifier.
     * @author Ben
     */
    public static ToHitData getSecondaryTargetMod(Game game, Entity attacker,
            Targetable target) {

        // large craft do not get secondary target mod
        // http://www.classicbattletech.com/forums/index.php/topic,37661.0.html
        if (attacker.getCrew().getCrewType().getMaxPrimaryTargets() < 0) {
            return null;
        }

        boolean curInFrontArc = Compute
                .isInArc(attacker.getPosition(), attacker.getSecondaryFacing(),
                         target, attacker.getForwardArc());
        boolean curInRearArc = Compute.isInArc(attacker.getPosition(),
                                               attacker.getSecondaryFacing(), target, attacker.getRearArc());
        if (!curInRearArc && attacker.hasQuirk(OptionsConstants.QUIRK_POS_MULTI_TRAC)) {
            return null;
        }

        int primaryTarget = Entity.NONE;
        boolean primaryInFrontArc = false;
        // Track # of targets, for secondary modifiers w/ multi-crew vehicles
        Set<Integer> targIds = new HashSet<>();
        for (Enumeration<EntityAction> i = game.getActions(); i.hasMoreElements(); ) {
            Object o = i.nextElement();
            if (!(o instanceof WeaponAttackAction)) {
                continue;
            }
            WeaponAttackAction prevAttack = (WeaponAttackAction) o;
            if (prevAttack.getEntityId() == attacker.getId()) {
                // Don't add id of current target, as it gets counted elsewhere
                if (prevAttack.getTargetId() != target.getId()) {
                    targIds.add(prevAttack.getTargetId());
                }
                // first front arc target is our primary.
                // if first target is non-front, and either a later target or
                // the current one is in front, use that instead.
                if (!game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_NO_FORCED_PRIMARY_TARGETS)) {
                    Targetable pte = game.getTarget(prevAttack.getTargetType(),
                                                    prevAttack.getTargetId());
                    // in double blind play, we might not have the target in our
                    // local copy of the game. In that case, the sprite won't
                    // have the correct to-hit number, but at least we don't crash
                    if (pte == null) {
                        continue;
                    }

                    // Determine primary target
                    if ((primaryTarget == Entity.NONE || !primaryInFrontArc)
                        && Compute.isInArc(attacker.getPosition(),
                                           attacker.getSecondaryFacing(), pte,
                                           attacker.getForwardArc())) {
                        primaryTarget = prevAttack.getTargetId();
                        primaryInFrontArc = true;
                    } else if ((primaryTarget == Entity.NONE) && !curInFrontArc) {
                        primaryTarget = prevAttack.getTargetId();
                    }
                } else if (primaryTarget == Entity.NONE) {
                    primaryTarget = prevAttack.getTargetId();
                }
            }
        }

        // # of targets, +1 for the passed target
        int countTargets = 1 + targIds.size();

        int maxPrimary = 1;
        //Tripods and QuadVees with dedicated gunnery can target up to three units before incurring a penalty, and two for dual cockpit
        if (attacker.getCrew().hasDedicatedGunner()) {
            maxPrimary = attacker.getCrew().getCrewType().getMaxPrimaryTargets();
        }
        if (game.getOptions().booleanOption("tacops_tank_crews")
            && (attacker instanceof Tank)) {

            // If we are a tank, and only have 1 crew then we have some special
            //  restrictions
            if (countTargets > 1 && attacker.getCrew().getSize() == 1) {
                return new ToHitData(TargetRoll.IMPOSSIBLE,
                        "Vehicles with only 1 crewman may not attack "
                                + "secondary targets");
            }
            // If we are a tank, we can have Crew Size - 1 targets before
            //  incurring a secondary target penalty (or crew size - 2 secondary
            //  targets without penalty)
            maxPrimary = attacker.getCrew().getSize() - 1;
        }
        if (countTargets <= maxPrimary) {
            return null; // no modifier
        }

        if ((primaryTarget == Entity.NONE) || (primaryTarget == target.getId())) {
            // current target is primary target
            return null; // no modifier
        }

        // current target is secondary

        // Stealthed Mechs can't be secondary targets (TW, pg. 142)
        if (((target instanceof Tank) || (target instanceof Mech) || (target instanceof Aero))
                && ((Entity) target).isStealthActive()) {
            return new ToHitData(TargetRoll.IMPOSSIBLE,
                    "Can't target unit with active stealth armor as a secondary target");
        }

        int mod = 2;
        if (curInFrontArc || (attacker instanceof BattleArmor)) {
            mod--;
        }

        if (attacker.hasAbility(OptionsConstants.GUNNERY_MULTI_TASKER)) {
            mod--;
        }

        return new ToHitData(mod, "secondary target modifier");
    }

    /**
     * Damage that a mech does with a accidental fall from above.
     */
    public static int getAffaDamageFor(Entity entity) {
        return (int) entity.getWeight() / 10;
    }

    /**
     * Modifier to attacks due to attacker movement
     */
    public static ToHitData getAttackerMovementModifier(Game game, int entityId) {
        return Compute.getAttackerMovementModifier(game, entityId, game.getEntity(entityId).moved);
    }

    /**
     * Modifier to attacks due to attacker movement
     */
    public static ToHitData getAttackerMovementModifier(Game game, int entityId,
                                                        EntityMovementType movement) {
        final Entity entity = game.getEntity(entityId);
        ToHitData toHit = new ToHitData();

        // infantry aren't affected by their own movement.
        if (entity instanceof Infantry) {
            return toHit;
        }

        if ((entity.getMovementMode() == EntityMovementMode.BIPED_SWIM)
            || (entity.getMovementMode() == EntityMovementMode.QUAD_SWIM)) {
            toHit.addModifier(3, "attacker used UMUs");
        } else if (entity instanceof LandAirMech && movement == EntityMovementType.MOVE_VTOL_WALK) {
            toHit.addModifier(3, "attacker cruised");
        } else if (entity instanceof LandAirMech && movement == EntityMovementType.MOVE_VTOL_RUN) {
            toHit.addModifier(4, "attacker flanked");
        } else if ((movement == EntityMovementType.MOVE_WALK) || (movement == EntityMovementType.MOVE_VTOL_WALK)
                || (movement == EntityMovementType.MOVE_CAREFUL_STAND)) {
            toHit.addModifier(1, "attacker walked");
        } else if ((movement == EntityMovementType.MOVE_RUN) || (movement == EntityMovementType.MOVE_VTOL_RUN)) {
            toHit.addModifier(2, "attacker ran");
        } else if (movement == EntityMovementType.MOVE_SKID) {
            toHit.addModifier(3, "attacker ran and skidded");
        } else if (movement == EntityMovementType.MOVE_JUMP) {
            if (entity.hasAbility(OptionsConstants.PILOT_JUMPING_JACK)) {
                toHit.addModifier(1, "attacker jumped");
            } else if (entity.hasAbility(OptionsConstants.PILOT_HOPPING_JACK)) {
                toHit.addModifier(2, "attacker jumped");
            } else {
                toHit.addModifier(3, "attacker jumped");
            }
        } else if (movement == EntityMovementType.MOVE_SPRINT
                || movement == EntityMovementType.MOVE_VTOL_SPRINT) {
            return new ToHitData(TargetRoll.AUTOMATIC_FAIL, "attacker sprinted");
        }

        //Dual cockpit with both pilot and gunner has lower modifier for attacker movement.
        if (toHit.getValue() != TargetRoll.AUTOMATIC_FAIL
                && entity instanceof Mech && ((Mech) entity).getCockpitType() == Mech.COCKPIT_DUAL
                && entity.getCrew().hasDedicatedGunner()) {
            for (TargetRollModifier mod : toHit.getModifiers()) {
                mod.setValue(mod.getValue() / 2);
            }
        }
        return toHit;
    }

    /**
     * Modifier to attacks due to spotter movement
     */
    public static ToHitData getSpotterMovementModifier(Game game, int entityId) {
        return Compute.getSpotterMovementModifier(game, entityId,
                                                  game.getEntity(entityId).moved);
    }

    /**
     * Modifier to attacks due to spotter movement
     */
    public static ToHitData getSpotterMovementModifier(Game game,
                                                       int entityId, EntityMovementType movement) {
        ToHitData toHit = new ToHitData();

        Entity e = game.getEntity(entityId);
        if ((e != null) && (e instanceof Infantry)) {
            return toHit;
        }

        if ((movement == EntityMovementType.MOVE_WALK)
            || (movement == EntityMovementType.MOVE_VTOL_WALK)) {
            toHit.addModifier(1, "spotter walked");
        } else if ((movement == EntityMovementType.MOVE_RUN)
                   || (movement == EntityMovementType.MOVE_VTOL_RUN)
                   || (movement == EntityMovementType.MOVE_SKID)) {
            toHit.addModifier(2, "spotter ran");
        } else if (movement == EntityMovementType.MOVE_JUMP) {
            toHit.addModifier(3, "spotter jumped");
        } else if (movement == EntityMovementType.MOVE_SPRINT
                || movement == EntityMovementType.MOVE_VTOL_SPRINT) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "spotter sprinted");
        }

        return toHit;
    }

    /**
     * Modifier to physical attack BTH due to pilot advantages
     */
    public static void modifyPhysicalBTHForAdvantages(final Entity attacker, final Entity target,
                                                      final ToHitData toHit, final Game game) {
        Objects.requireNonNull(attacker);

        if (attacker.getCrew().getOptions().stringOption(OptionsConstants.MISC_ENV_SPECIALIST).equals(Crew.ENVSPC_LIGHT)
                && !target.isIlluminated()
                && game.getPlanetaryConditions().getLight().isDarkerThan(Light.FULL_MOON)) {
            toHit.addModifier(-1, "light specialist");
        }

        if (attacker.hasAbility(OptionsConstants.PILOT_MELEE_SPECIALIST)
                && (attacker instanceof Mech)) {
            toHit.addModifier(-1, "melee specialist");
        }

        if (attacker.hasAbility(OptionsConstants.PILOT_TM_FROGMAN)
                && ((attacker instanceof Mech) || (attacker instanceof Protomech))
                && (game.getBoard().getHex(attacker.getPosition()).terrainLevel(Terrains.WATER) > 1)) {
            toHit.addModifier(-1, "Frogman");
        }

        if (attacker.hasAbility(OptionsConstants.UNOFF_CLAN_PILOT_TRAINING)) {
            toHit.addModifier(1, "clan pilot training");
        }

        // Mek targets that are dodging are harder to hit.
        if ((target instanceof Mech)
                && target.hasAbility(OptionsConstants.PILOT_DODGE_MANEUVER) && target.dodging) {
            toHit.addModifier(2, "target is dodging");
        }
    }

    /**
     * Modifier to attacks due to target movement
     */
    public static ToHitData getTargetMovementModifier(Game game, int entityId) {
        Entity entity = game.getEntity(entityId);

        if (entity.isAero()) {
            return new ToHitData();
        }

        //If we're a trailer and being towed, return data for the tractor
        if (entity.isTrailer() && entity.getTractor() != Entity.NONE) {
            return getTargetMovementModifier(game, entity.getTractor());
        }

        if (game.getOptions().booleanOption(OptionsConstants.ADVGRNDMOV_TACOPS_STANDING_STILL)
            && (entity.mpUsed == 0)
            && !entity.isImmobile()
            && !((entity instanceof Infantry) || (entity instanceof VTOL) || (entity instanceof GunEmplacement))) {
            ToHitData toHit = new ToHitData();
            toHit.addModifier(-1, "target didn't move");
            return toHit;
        }

        if (entity.isAssaultDropInProgress()) {
            ToHitData toHit = new ToHitData();
            toHit.addModifier(3, "target is assault dropping");
            return toHit;
        }

        // Compile various state information to determine if the entity jumped, "jumped", or is VTOL
        // Airborne non-ASF vehicles like WiGE can get +1 TMM for jumping _or_ being airborne, but not both.
        // Non-flying WiGE _can_ get +1 TMM for jumping.
        // See TW: pg. 307, "Attack Modifiers Table"
        boolean jumped = !entity.isAirborneVTOLorWIGE()
                        && (
                            (entity.moved == EntityMovementType.MOVE_JUMP)
                            || (entity.moved == EntityMovementType.MOVE_VTOL_RUN)
                            || (entity.moved == EntityMovementType.MOVE_VTOL_WALK)
                            || (entity.moved == EntityMovementType.MOVE_VTOL_SPRINT)
                        );

        boolean validFlying = (entity.moved == EntityMovementType.MOVE_VTOL_RUN)
                        || (entity.moved == EntityMovementType.MOVE_VTOL_WALK)
                        || (entity.getMovementMode() == EntityMovementMode.VTOL)
                        || (entity.moved == EntityMovementType.MOVE_VTOL_SPRINT);

        ToHitData toHit = Compute
                .getTargetMovementModifier(
                        entity.delta_distance,
                        jumped,
                        validFlying,
                        game);

        if (entity.moved != EntityMovementType.MOVE_JUMP
                && entity.delta_distance > 0
                && entity instanceof Mech && ((Mech) entity).getCockpitType() == Mech.COCKPIT_DUAL
                && entity.getCrew().hasDedicatedPilot()) {
            if (toHit.getModifiers().isEmpty()) {
                toHit.addModifier(1, "target moved 1-2 hexes");
            } else {
                toHit.getModifiers().get(0).setValue(toHit.getModifiers().get(0).getValue() + 1);
            }
        }

        // Did the target skid this turn?
        if (entity.moved == EntityMovementType.MOVE_SKID) {
            toHit.addModifier(2, "target skidded");
        }
        if ((entity.getElevation() > 0)
            && (entity.getMovementMode() == EntityMovementMode.WIGE)) {
                toHit.addModifier(1, "target is airborne");
        }

        // did the target sprint?
        if (entity.moved == EntityMovementType.MOVE_SPRINT
                || entity.moved == EntityMovementType.MOVE_VTOL_SPRINT) {
            toHit.addModifier(-1, "target sprinted");
        }

        return toHit;
    }

    /**
     * Target movement modifer for the specified delta_distance
     */
    public static ToHitData getTargetMovementModifier(int distance,
            boolean jumped, boolean isVTOL, Game game) {
        ToHitData toHit = new ToHitData();
        if (distance == 0 && !jumped) {
            return toHit;
        }

        if ((game != null)
            && game.getOptions().booleanOption(OptionsConstants.ADVANCED_MAXTECH_MOVEMENT_MODS)) {
            if ((distance >= 3) && (distance <= 4)) {
                toHit.addModifier(1, "target moved 3-4 hexes");
            } else if ((distance >= 5) && (distance <= 6)) {
                toHit.addModifier(2, "target moved 5-6 hexes");
            } else if ((distance >= 7) && (distance <= 9)) {
                toHit.addModifier(3, "target moved 7-9 hexes");
            } else if ((distance >= 10) && (distance <= 13)) {
                toHit.addModifier(4, "target moved 10-13 hexes");
            } else if ((distance >= 14) && (distance <= 18)) {
                toHit.addModifier(5, "target moved 18-24 hexes");
            } else if ((distance >= 19) && (distance <= 24)) {
                toHit.addModifier(6, "target moved 18-24 hexes");
            } else if (distance >= 25) {
                toHit.addModifier(7, "target moved 25+ hexes");
            }
        } else {
            if ((distance >= 3) && (distance <= 4)) {
                toHit.addModifier(1, "target moved 3-4 hexes");
            } else if ((distance >= 5) && (distance <= 6)) {
                toHit.addModifier(2, "target moved 5-6 hexes");
            } else if ((distance >= 7) && (distance <= 9)) {
                toHit.addModifier(3, "target moved 7-9 hexes");
            } else if ((distance >= 10) && (distance <= 17)) {
                toHit.addModifier(4, "target moved 10-17 hexes");
            } else if ((distance >= 18) && (distance <= 24)) {
                toHit.addModifier(5, "target moved 18-24 hexes");
            } else if (distance >= 25) {
                toHit.addModifier(6, "target moved 25+ hexes");
            }
        }

        if (isVTOL && (distance > 0)) {
            toHit.addModifier(1, "target VTOL used MPs");
        } else if (jumped) {
            toHit.addModifier(1, "target jumped");
        }

        return toHit;
    }

    /**
     * Modifier to attacks due to attacker terrain
     */
    public static ToHitData getAttackerTerrainModifier(Game game, int entityId) {
        final Entity attacker = game.getEntity(entityId);
        final Hex hex = game.getBoard().getHex(attacker.getPosition());

        ToHitData toHit = new ToHitData();

        // space screens; bonus depends on number (level)
        if ((hex != null) && (hex.terrainLevel(Terrains.SCREEN) > 0)) {
            toHit.addModifier(hex.terrainLevel(Terrains.SCREEN) + 1,
                              "attacker in screen(s)");
        }

        return toHit;
    }

    /**
     * Modifier to attacks due to target terrain TODO:um....should VTOLs get
     * modifiers for smoke, etc.
     */
    public static ToHitData getTargetTerrainModifier(Game game, Targetable t) {
        return Compute.getTargetTerrainModifier(game, t, 0);
    }

    public static ToHitData getTargetTerrainModifier(Game game, Targetable t,
                                                     int eistatus) {
        return Compute.getTargetTerrainModifier(game, t, eistatus, false);
    }

    public static ToHitData getTargetTerrainModifier(Game game, Targetable t,
                                                     int eistatus, boolean attackerInSameBuilding) {
        return Compute.getTargetTerrainModifier(game, t, eistatus,
                                                attackerInSameBuilding, false);
    }

    public static ToHitData getTargetTerrainModifier(Game game, Targetable t,
                                                     int eistatus, boolean attackerInSameBuilding,
                                                     boolean underwaterWeapon) {
        ToHitData toHit = new ToHitData();

        // no terrain mods for bombs, artillery strikes
        if (t.getTargetType() == Targetable.TYPE_HEX_AERO_BOMB ||
                t.getTargetType() == Targetable.TYPE_HEX_ARTILLERY) {
            return toHit;
        }

        Entity entityTarget = null;
        Hex hex = game.getBoard().getHex(t.getPosition());
        if (t.getTargetType() == Targetable.TYPE_ENTITY) {
            entityTarget = (Entity) t;
            if (hex == null) {
                entityTarget.setPosition(game.getEntity(entityTarget.getId())
                                             .getPosition());
                hex = game.getBoard().getHex(
                        game.getEntity(entityTarget.getId()).getPosition());
            }
        }

        // if the hex doesn't exist, it's unlikely to have terrain modifiers
        if (hex == null) {
            return toHit;
        }


        boolean hasWoods = hex.containsTerrain(Terrains.WOODS) || hex.containsTerrain(Terrains.JUNGLE);
        // Standard mechs (standing) report their height as 1, tanks as 0
        // Standard mechs should not benefit from 1 level high woods

        boolean isAboveWoods = (entityTarget == null)
                || (entityTarget.relHeight() + 1 > hex.terrainLevel(Terrains.FOLIAGE_ELEV))
                || entityTarget.isAirborne()
                || !hasWoods;
        boolean isAboveSmoke = (entityTarget == null)
                || (entityTarget.relHeight() + 1 > 2)
                || !hex.containsTerrain(Terrains.SMOKE);
        boolean isUnderwater = (entityTarget != null)
                               && hex.containsTerrain(Terrains.WATER) && (hex.depth() > 0)
                               && (entityTarget.getElevation() < hex.getLevel());

        // if we have in-building combat, it's a +1
        if (attackerInSameBuilding) {
            toHit.addModifier(1, "target in a building hex");
        }

        // Smoke and woods. With L3, the effects STACK.
        int woodsLevel = hex.terrainLevel(Terrains.WOODS);
        int jungleLevel = hex.terrainLevel(Terrains.JUNGLE);
        String woodsText = "woods";
        if (woodsLevel < jungleLevel) {
            woodsLevel = jungleLevel;
            woodsText = "jungle";
        }
        if (woodsLevel == 1) {
            woodsText = "target in light " + woodsText;
        } else if (woodsLevel == 2) {
            woodsText = "target in heavy " + woodsText;
        } else if (woodsLevel == 3) {
            woodsText = "target in ultra heavy " + woodsText;
        }

        if (!game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_WOODS_COVER)
            && !isAboveWoods
            && !((t.getTargetType() == Targetable.TYPE_HEX_CLEAR)
                 || (t.getTargetType() == Targetable.TYPE_HEX_IGNITE)
                 || (t.getTargetType() == Targetable.TYPE_HEX_BOMB)
                 || (t.getTargetType() == Targetable.TYPE_HEX_ARTILLERY)
                 || (t.getTargetType() == Targetable.TYPE_MINEFIELD_DELIVER))) {
            if ((woodsLevel == 1) && (eistatus != 2)) {
                toHit.addModifier(1, woodsText);
            } else if (woodsLevel > 1) {
                if (eistatus > 0) {
                    toHit.addModifier(woodsLevel - 1, woodsText);
                } else {
                    toHit.addModifier(woodsLevel, woodsText);
                }
            }
        }
        if (!isAboveSmoke && !isUnderwater && !underwaterWeapon) {
            switch (hex.terrainLevel(Terrains.SMOKE)) {
                case SmokeCloud.SMOKE_LIGHT:
                case SmokeCloud.SMOKE_LI_LIGHT:
                case SmokeCloud.SMOKE_LI_HEAVY:
                case SmokeCloud.SMOKE_CHAFF_LIGHT:
                case SmokeCloud.SMOKE_GREEN:
                    toHit.addModifier(1, "target in light smoke");
                    break;
                case SmokeCloud.SMOKE_HEAVY:
                    if (eistatus > 0) {
                        toHit.addModifier(1, "target in heavy smoke");
                    } else {
                        toHit.addModifier(2, "target in heavy smoke");
                    }
                    break;
            }
        }
        if (hex.terrainLevel(Terrains.GEYSER) == 2) {
            if (eistatus > 0) {
                toHit.addModifier(1, "target in erupting geyser");
            } else {
                toHit.addModifier(2, "target in erupting geyser");
            }
        }

        if (hex.containsTerrain(Terrains.INDUSTRIAL)) {
            toHit.addModifier(+1, "target in heavy industrial zone");
        }
        // space screens; bonus depends on number (level)
        if (hex.terrainLevel(Terrains.SCREEN) > 0) {
            toHit.addModifier(hex.terrainLevel(Terrains.SCREEN) + 1,
                              "target in screen(s)");
        }

        // only entities get remaining terrain bonuses
        // TODO: should this be changed for buildings???
        if (entityTarget == null) {
            return toHit;
        } else if (entityTarget.isMakingDfa()) {
            // you don't get terrain modifiers in midair
            // should be abstracted more into a 'not on the ground'
            // flag for vtols and such
            return toHit;
        }

        if (entityTarget.isStuck()) {
            toHit.addModifier(-2, "target stuck in swamp");
        }
        if ((entityTarget instanceof Infantry)
            && hex.containsTerrain(Terrains.FIELDS)) {
            toHit.addModifier(+1, "target in planted fields");
        }
        return toHit;
    }

    public static ToHitData getStrafingTerrainModifier(Game game,
                                                       int eistatus, Hex hex) {
        ToHitData toHit = new ToHitData();
        // Smoke and woods. With L3, the effects STACK.
        int woodsLevel = hex.terrainLevel(Terrains.WOODS);
        int jungleLevel = hex.terrainLevel(Terrains.JUNGLE);
        String woodsText = "woods";
        if (woodsLevel < jungleLevel) {
            woodsLevel = jungleLevel;
            woodsText = "jungle";
        }
        if (woodsLevel == 1) {
            woodsText = "light " + woodsText;
        } else if (woodsLevel == 2) {
            woodsText = "heavy " + woodsText;
        } else if (woodsLevel == 3) {
            woodsText = "heavy " + woodsText;
        }

        if (!game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_WOODS_COVER)) {
            if ((woodsLevel == 1) && (eistatus != 2)) {
                toHit.addModifier(1, woodsText);
            } else if (woodsLevel > 1) {
                if (eistatus > 0) {
                    toHit.addModifier(woodsLevel - 1, woodsText);
                } else {
                    toHit.addModifier(woodsLevel, woodsText);
                }
            }
        }

        switch (hex.terrainLevel(Terrains.SMOKE)) {
            case SmokeCloud.SMOKE_LIGHT:
            case SmokeCloud.SMOKE_LI_LIGHT:
            case SmokeCloud.SMOKE_LI_HEAVY:
            case SmokeCloud.SMOKE_CHAFF_LIGHT:
            case SmokeCloud.SMOKE_GREEN:
                toHit.addModifier(1, "target in light smoke");
                break;
            case SmokeCloud.SMOKE_HEAVY:
                if (eistatus > 0) {
                    toHit.addModifier(1, "target in heavy smoke");
                } else {
                    toHit.addModifier(2, "target in heavy smoke");
                }
                break;
        }

        if (hex.terrainLevel(Terrains.GEYSER) == 2) {
            if (eistatus > 0) {
                toHit.addModifier(1, "erupting geyser");
            } else {
                toHit.addModifier(2, "erupting geyser");
            }
        }
        return toHit;
    }

    /**
     * Calculates the current theoretical damage absorbable (armor+structure, etc) by the given target.
     * Used as a measure of the potential durability of the target under fire.
     */
    public static int getTargetTotalHP(Game game, Targetable target) {
        int targetType = target.getTargetType();
        int targetId = target.getId();
        Coords position = target.getPosition();

        // First, handle buildings versus entities, since they are handled differently.
        if (targetType == Targetable.TYPE_BUILDING) {
            // Buildings are a simple sum of their current CF and armor values.
            // the building the targeted hex belongs to. We have to get this and then get values for the specific hex internally to it.
            final Building parentBuilding = game.getBoard().getBuildingAt(position);
            return (parentBuilding == null) ? 0
                    : parentBuilding.getCurrentCF(position) + parentBuilding.getArmor(position);
        } else if (targetType == Targetable.TYPE_ENTITY) {
            //I don't *think* we have to handle infantry differently here - I think these methods should return the total number of men remaining as internal structure.
            Entity targetEntity = game.getEntity(targetId);

            if (targetEntity == null) {
                return 0;
            } else if (targetEntity instanceof GunEmplacement) {
                // If this is a gun emplacement, handle it as the building hex it is in.
                final Building parentBuilding = game.getBoard().getBuildingAt(position);
                return (parentBuilding == null) ? 0
                        : parentBuilding.getCurrentCF(position) + parentBuilding.getArmor(position);
            } else {
                return targetEntity.getTotalArmor() + targetEntity.getTotalInternal();
            }
        } else if (targetType == Targetable.TYPE_HEX_CLEAR) {
            // clearing a hex - the "HP" is the terrain factor of destroyable terrain on this hex
            Hex mhex = game.getBoard().getHex(position);
            int totalTF = 0;
            for (final int terrainType : mhex.getTerrainTypes()) {
                totalTF += mhex.containsTerrain(terrainType) ? mhex.getTerrain(terrainType).getTerrainFactor() : 0;
            }
            return totalTF;
        } else { // something else, e.g. terrain. We probably don't need to handle it for now.
            return 0;
        }
    }

    /**
     * Returns the weapon attack out of a list that has the highest expected
     * damage
     */
    public static WeaponAttackAction getHighestExpectedDamage(Game g,
            List<WeaponAttackAction> vAttacks, boolean assumeHit) {
        float fHighest = -1.0f;
        WeaponAttackAction waaHighest = null;
        for (int x = 0, n = vAttacks.size(); x < n; x++) {
            WeaponAttackAction waa = vAttacks.get(x);
            float fDanger = Compute.getExpectedDamage(g, waa, assumeHit);
            if (fDanger > fHighest) {
                fHighest = fDanger;
                waaHighest = waa;
            }
        }
        return waaHighest;
    }

    // store these as constants since the tables will never change
    private static float[] expectedHitsByRackSize = { 0.0f, 1.0f, 1.58f, 2.0f,
            2.63f, 3.17f, 4.0f, 4.49f, 4.98f, 5.47f, 6.31f, 7.23f, 8.14f,
            8.59f, 9.04f, 9.5f, 10.1f, 10.8f, 11.42f, 12.1f, 12.7f };

    /*
     * | No Modifier | +2 (Artemis, Narc) | -2 (HAG, AMS v Art)| -4 (AMS) | |
     * Avg | Avg | Avg | Avg | | Hits Pct | Hits Pct | Hits Pct | Hits Pct | |
     * Avg Per vs | Avg Per vs | Avg Per vs | Avg Per vs | Size| Hits Size Avg |
     * Hits Size Avg | Hits Size Avg | Hits Size Avg |
     * ----+--------------------+
     * --------------------+--------------------+--------------------+ 2 | 1.42
     * 0.708 9.1 | 1.72 0.861 10.3 | 1.17 0.583 10.7 | 1.03 0.514 21.9 | 3 |
     * 2.00 0.667 2.7 | 2.39 0.796 2.0 | 1.61 0.537 2.0 | 1.28 0.426 1.0 | 4 |
     * 2.64 0.660 1.6 | 3.11 0.778 -0.4 | 2.11 0.528 0.2 | 1.67 0.417 -1.2 | 5 |
     * 3.17 0.633 -2.5 | 3.83 0.767 -1.8 | 2.50 0.500 -5.1 | 1.86 0.372 -11.7 |
     * 6 | 4.00 0.667 2.7 | 4.78 0.796 2.0 | 3.22 0.537 2.0 | 2.58 0.431 2.1 | 7
     * | 4.39 0.627 -3.4 | 5.42 0.774 -0.9 | 3.47 0.496 -5.8 | 2.69 0.385 -8.7 |
     * 8 | 5.08 0.635 -2.1 | 6.06 0.757 -3.0 | 4.22 0.528 0.2 | 3.58 0.448 6.2 |
     * 9 | 5.47 0.608 -6.4 | 6.69 0.744 -4.7 | 4.47 0.497 -5.7 | 3.69 0.410 -2.7
     * | 10 | 6.31 0.631 -2.9 | 7.67 0.767 -1.8 | 5.06 0.506 -4.0 | 3.97 0.397
     * -5.8 | 11 | 7.31 0.664 2.3 | 8.67 0.788 0.9 | 6.06 0.551 4.5 | 4.97 0.452
     * 7.2 | 12 | 8.14 0.678 4.5 | 9.64 0.803 2.9 | 6.64 0.553 5.0 | 5.25 0.438
     * 3.7 | 13 | 8.42 0.647 -0.3 | 10.22 0.786 0.7 | 6.72 0.517 -1.8 | 5.25
     * 0.404 -4.2 | 14 | 9.22 0.659 1.5 | 10.92 0.780 -0.1 | 7.64 0.546 3.6 |
     * 6.25 0.446 5.9 | 15 | 9.50 0.633 -2.5 | 11.50 0.767 -1.8 | 7.72 0.515
     * -2.3 | 6.25 0.417 -1.2 | 16 | 10.42 0.651 0.3 | 12.50 0.781 0.1 | 8.44
     * 0.528 0.2 | 6.67 0.417 -1.2 | 17 | 10.69 0.629 -3.1 | 13.08 0.770 -1.4 |
     * 8.53 0.502 -4.8 | 6.67 0.392 -7.0 | 18 | 11.50 0.639 -1.6 | 13.78 0.765
     * -1.9 | 9.44 0.525 -0.4 | 7.67 0.426 1.0 | 19 | 11.78 0.620 -4.5 | 14.36
     * 0.756 -3.2 | 9.53 0.501 -4.8 | 7.67 0.404 -4.3 | 20 | 12.69 0.635 -2.2 |
     * 15.36 0.768 -1.6 | 10.25 0.512 -2.7 | 8.08 0.404 -4.2 | 21 | 13.61 0.648
     * -0.2 | 16.33 0.778 -0.4 | 11.11 0.529 0.4 | 8.94 0.426 1.0 | 22 | 14.44
     * 0.657 1.1 | 17.31 0.787 0.8 | 11.69 0.532 0.9 | 9.22 0.419 -0.6 | 23 |
     * 15.36 0.668 2.9 | 18.31 0.796 2.0 | 12.42 0.540 2.5 | 9.64 0.419 -0.6 |
     * 24 | 16.28 0.678 4.5 | 19.28 0.803 2.9 | 13.28 0.553 5.0 | 10.50 0.438
     * 3.7 | 25 | 16.56 0.662 2.0 | 19.86 0.794 1.8 | 13.36 0.534 1.5 | 10.50
     * 0.420 -0.4 | 26 | 17.36 0.668 2.8 | 20.56 0.791 1.3 | 14.28 0.549 4.2 |
     * 11.50 0.442 4.9 | 27 | 17.64 0.653 0.6 | 21.14 0.783 0.3 | 14.36 0.532
     * 1.0 | 11.50 0.426 1.0 | 28 | 17.92 0.640 -1.4 | 21.72 0.776 -0.6 | 14.44
     * 0.516 -2.1 | 11.50 0.411 -2.6 | 29 | 18.72 0.646 -0.6 | 22.42 0.773 -1.0
     * | 15.36 0.530 0.6 | 12.50 0.431 2.2 | 30 | 19.00 0.633 -2.5 | 23.00 0.767
     * -1.8 | 15.44 0.515 -2.3 | 12.50 0.417 -1.2 | 40 | 25.39 0.635 -2.2 |
     * 30.72 0.768 -1.6 | 20.50 0.512 -2.7 | 16.17 0.404 -4.2 | ----- -----
     * ----- ----- Average: 0.649 0.781 0.527 0.422 1.202 0.811 0.649
     */

    /**
     * Determines the expected damage of a weapon attack, based on to-hit, salvo
     * sizes, etc.
     */
    public static float getExpectedDamage(Game g, WeaponAttackAction waa,
            boolean assumeHit) {
        return Compute.getExpectedDamage(g, waa, assumeHit, null);
    }

    /**
     * Determines the expected damage of a weapon attack, based on to-hit, salvo
     * sizes, etc.
     */
    public static float getExpectedDamage(Game g, WeaponAttackAction waa,
            boolean assumeHit, List<ECMInfo> allECMInfo) {
        boolean use_table = false;

        AmmoType loaded_ammo = new AmmoType();

        Entity attacker = g.getEntity(waa.getEntityId());
        Entity target = g.getEntity(waa.getTargetId());

        int baShootingStrength = attacker instanceof BattleArmor ?
                ((BattleArmor) attacker).getShootingStrength() : 0;

        int infShootingStrength = 0;
        double infDamagePerTrooper = 0;

        Mounted weapon = attacker.getEquipment(waa.getWeaponId());
        Mounted lnk_guide;

        ToHitData hitData = waa.toHit(g, allECMInfo);

        if (attacker.isConventionalInfantry()) {
            infShootingStrength = ((Infantry) attacker).getShootingStrength();
            infDamagePerTrooper = ((Infantry) attacker).getDamagePerTrooper();
        }

        WeaponType wt = (WeaponType) weapon.getType();

        float fDamage = 0.0f;
        float fChance = 0.0f;
        if (assumeHit) {
            fChance = 1.0f;
        } else {
            if ((hitData.getValue() == TargetRoll.IMPOSSIBLE)
                || (hitData.getValue() == TargetRoll.AUTOMATIC_FAIL)) {
                return 0.0f;
            }

            if (hitData.getValue() == TargetRoll.AUTOMATIC_SUCCESS) {
                fChance = 1.0f;
            } else {
                fChance = (float) Compute.oddsAbove(hitData.getValue(),
                                                    attacker.hasAbility(OptionsConstants.PILOT_APTITUDE_GUNNERY))
                          / 100.0f;
            }
        }

        // Missiles, HAGs, LBX cluster rounds, and ultra/rotary cannons (when
        // spun up)
        // use the missile hits table
        if (wt.getDamage() == WeaponType.DAMAGE_BY_CLUSTERTABLE) {
            use_table = true;
        }

        //Unless it's a fighter squadron, which uses a weird group of single weapons and should return mass AV
        if (attacker.isCapitalFighter()) {
            use_table = false;
        }

        if ((wt.getAmmoType() == AmmoType.T_AC_LBX)
            || (wt.getAmmoType() == AmmoType.T_AC_LBX_THB)
            || (wt.getAmmoType() == AmmoType.T_AC_LBX_THB)) {
            loaded_ammo = (AmmoType) weapon.getLinked().getType();
            if (((loaded_ammo.getAmmoType() == AmmoType.T_AC_LBX) || (loaded_ammo
                                                                              .getAmmoType() == AmmoType.T_AC_LBX_THB))
                && (loaded_ammo.getMunitionType().contains(AmmoType.Munitions.M_CLUSTER))) {
                use_table = true;
            }
        }

        if ((wt.getAmmoType() == AmmoType.T_AC_ULTRA)
            || (wt.getAmmoType() == AmmoType.T_AC_ULTRA_THB)
            || (wt.getAmmoType() == AmmoType.T_AC_ROTARY)) {
            if ((weapon.curMode().getName().equals("Ultra"))
                || (weapon.curMode().getName().equals("2-shot"))
                || (weapon.curMode().getName().equals("3-shot"))
                || (weapon.curMode().getName().equals("4-shot"))
                || (weapon.curMode().getName().equals("5-shot"))
                || (weapon.curMode().getName().equals("6-shot"))) {
                use_table = true;
            }
        }

        // Kinda cheap, but lets use the missile hits table for Battle armor
        // weapons too

        if (attacker instanceof BattleArmor) {
            if ((wt.getInternalName() != Infantry.SWARM_MEK)
                && (wt.getInternalName() != Infantry.LEG_ATTACK)) {
                use_table = true;
            }
        }

        if (use_table == true) {
            if (!(attacker instanceof BattleArmor)) {
                if (weapon.getLinked() == null) {
                    return 0.0f;
                }
            }
            AmmoType at = null;
            if ((weapon.getLinked() != null)
                    && (weapon.getLinked().getType() instanceof AmmoType)) {
                at = (AmmoType) weapon.getLinked().getType();
                fDamage = at.getDamagePerShot();
            }

            float fHits = 0.0f;
            if ((wt.getRackSize() != 40) && (wt.getRackSize() != 30)) {
                fHits = expectedHitsByRackSize[wt.getRackSize()];
            } else {
                fHits = 2.0f * expectedHitsByRackSize[wt.getRackSize() / 2];
            }
            if (((wt.getAmmoType() == AmmoType.T_SRM_STREAK)
                    || (wt.getAmmoType() == AmmoType.T_LRM_STREAK))
                    && !ComputeECM.isAffectedByAngelECM(attacker, attacker
                            .getPosition(), waa.getTarget(g).getPosition(),
                            allECMInfo)) {
                fHits = wt.getRackSize();
            }
            if ((wt.getAmmoType() == AmmoType.T_AC_ULTRA)
                || (wt.getAmmoType() == AmmoType.T_AC_ULTRA_THB)
                || (wt.getAmmoType() == AmmoType.T_AC_ROTARY)) {
                if ((weapon.curMode().getName().equals("Ultra"))
                    || (weapon.curMode().getName().equals("2-shot"))) {
                    fHits = expectedHitsByRackSize[2];
                }
                if (weapon.curMode().getName().equals("3-shot")) {
                    fHits = expectedHitsByRackSize[3];
                }
                if (weapon.curMode().getName().equals("4-shot")) {
                    fHits = expectedHitsByRackSize[4];
                }
                if (weapon.curMode().getName().equals("5-shot")) {
                    fHits = expectedHitsByRackSize[5];
                }
                if (weapon.curMode().getName().equals("6-shot")) {
                    fHits = expectedHitsByRackSize[6];
                }
            }

            // Most Battle Armor units have a weapon per trooper, plus their
            // weapons do odd things when mounting multiples
            if (attacker instanceof BattleArmor) {
                // The number of troopers hitting
                fHits = expectedHitsByRackSize[baShootingStrength];
                if (wt.getDamage() == WeaponType.DAMAGE_BY_CLUSTERTABLE) {
                    fHits *= expectedHitsByRackSize[wt.getRackSize()];
                }
                if (wt.getDamage() != WeaponType.DAMAGE_BY_CLUSTERTABLE) {
                    if (wt.getDamage() != WeaponType.DAMAGE_VARIABLE) {
                        fDamage = wt.getDamage();
                    } else {
                        fDamage = wt.getRackSize();
                    }
                }
                if (wt.hasFlag(WeaponType.F_MISSILE_HITS)) {
                    fHits *= expectedHitsByRackSize[wt.getRackSize()];
                }
            }

            // If there is no ECM coverage to the target, guidance systems are
            // good for another 1.20x damage on missile weapons
            if ((!ComputeECM.isAffectedByECM(attacker, attacker.getPosition(), g
                    .getEntity(waa.getTargetId()).getPosition(), allECMInfo))
                && (wt.getDamage() == WeaponType.DAMAGE_BY_CLUSTERTABLE)
                && (wt.hasFlag(WeaponType.F_MISSILE)) && null != at) {
                // Check for linked artemis guidance system
                if ((wt.getAmmoType() == AmmoType.T_LRM)
                        || (wt.getAmmoType() == AmmoType.T_LRM_IMP)
                        || (wt.getAmmoType() == AmmoType.T_MML)
                        || (wt.getAmmoType() == AmmoType.T_SRM)
                        || (wt.getAmmoType() == AmmoType.T_SRM_IMP)) {
                    lnk_guide = weapon.getLinkedBy();
                    if ((lnk_guide != null) && (lnk_guide.getType() instanceof MiscType) && !lnk_guide.isDestroyed()
                            && !lnk_guide.isMissing() && !lnk_guide.isBreached()
                            && lnk_guide.getType().hasFlag(MiscType.F_ARTEMIS)) {

                        // Don't use artemis if this is indirect fire
                        // -> Hook for Artemis V Level 3 Clan tech here; use
                        // 1.30f multiplier when implemented
                        if (((weapon.curMode() == null) || !weapon.curMode().equals("Indirect"))
                                && (at.getMunitionType().contains(AmmoType.Munitions.M_ARTEMIS_CAPABLE))) {
                            fHits *= 1.2f;
                        }
                        if (((weapon.curMode() == null) || !weapon.curMode().equals("Indirect"))
                                && (at.getMunitionType().contains(AmmoType.Munitions.M_ARTEMIS_V_CAPABLE))) {
                            fHits *= 1.3f;
                        }
                    }
                }

                // Check for ATMs, which have built in Artemis
                if (wt.getAmmoType() == AmmoType.T_ATM) {
                    fHits *= 1.2f;
                }

                // Check for target with attached Narc or iNarc homing pod from
                // friendly unit
                if (target.isNarcedBy(attacker.getOwner().getTeam())
                        || target.isINarcedBy(attacker.getOwner().getTeam())) {
                    if (((at.getAmmoType() == AmmoType.T_LRM)
                            || (at.getAmmoType() == AmmoType.T_LRM_IMP)
                            || (at.getAmmoType() == AmmoType.T_MML)
                            || (at.getAmmoType() == AmmoType.T_SRM)
                            || (at.getAmmoType() == AmmoType.T_SRM_IMP))
                            && (at.getMunitionType().contains(AmmoType.Munitions.M_NARC_CAPABLE))) {
                        fHits *= 1.2f;
                    }
                }
            }

            if (wt.getAmmoType() == AmmoType.T_MRM) {
                lnk_guide = weapon.getLinkedBy();
                if ((lnk_guide != null)
                    && (lnk_guide.getType() instanceof MiscType)
                    && !lnk_guide.isDestroyed() && !lnk_guide.isMissing()
                    && !lnk_guide.isBreached()
                    && lnk_guide.getType().hasFlag(MiscType.F_APOLLO)) {
                    fHits *= .9f;
                }
            }

            // adjust for previous AMS
            if ((wt.getDamage() == WeaponType.DAMAGE_BY_CLUSTERTABLE)
                && wt.hasFlag(WeaponType.F_MISSILE)) {
                ArrayList<Mounted> vCounters = waa.getCounterEquipment();
                if (vCounters != null) {
                    for (int x = 0; x < vCounters.size(); x++) {
                        EquipmentType type = vCounters.get(x).getType();
                        if ((type instanceof WeaponType) && type.hasFlag(WeaponType.F_AMS)) {
                            fHits *= 0.6f;
                        }
                    }
                }
            }

            // * HAGs modify their cluster hits for range.
            if (wt instanceof HAGWeapon) {
                int distance = attacker.getPosition().distance(target.getPosition());
                if (distance <= wt.getShortRange()) {
                    fHits *= 1.2f;
                } else if (distance > wt.getMediumRange()) {
                    fHits *= 0.8f;
                }
            }

            fDamage *= fHits;

            if ((wt.getAmmoType() == AmmoType.T_AC_ULTRA)
                    || (wt.getAmmoType() == AmmoType.T_AC_ULTRA_THB)
                    || (wt.getAmmoType() == AmmoType.T_AC_ROTARY)) {
                fDamage = fHits * wt.getDamage();
            }

        } else {
            // Direct fire weapons (and LBX slug rounds) just do a single shot
            // so they don't use the missile hits table. Weapon bays also deal
            // damage in a single block
            if ((attacker.getPosition() != null) && (target.getPosition() != null)) {
                // Damage may vary by range for some weapons, so let's see how far
                // away we actually are and then set the damage accordingly.
                int rangeToTarget = attacker.getPosition().distance(target.getPosition());

                //Convert AV to fDamage for bay weapons, fighters, etc
                if (attacker.usesWeaponBays()) {
                    double av = 0;
                    double threat = 1;
                    for (int wId : weapon.getBayWeapons()) {
                        Mounted bayW = attacker.getEquipment(wId);
                        WeaponType bayWType = ((WeaponType) bayW.getType());
                        //Capital weapons have a different range scale
                        if (wt.isCapital()) {
                            // Capital missiles get higher priority than standard missiles:
                            // damage plus a bonus for the critical hit threat they represent
                            threat = 12;
                            if (rangeToTarget > 50) {
                                av = 0;
                            } else if (rangeToTarget > 40) {
                                av += bayWType.getExtAV();
                            } else if (rangeToTarget > 25) {
                                av += bayWType.getLongAV();
                            } else if (rangeToTarget > 12) {
                                av += bayWType.getMedAV();
                            } else {
                                av += bayWType.getShortAV();
                            }
                        } else {
                            if (rangeToTarget > 25) {
                                av = 0;
                            } else if (rangeToTarget > 20) {
                                av += bayWType.getExtAV();
                            } else if (rangeToTarget > 12) {
                                av += bayWType.getLongAV();
                            } else if (rangeToTarget > 6) {
                                av += bayWType.getMedAV();
                            } else {
                                av += bayWType.getShortAV();
                            }
                        }
                        fDamage = (float) (av * threat);
                    }
                } else if (attacker.isCapitalFighter()) {
                    double av = 0;
                    double threat = 1;
                    //Capital weapons have a different range scale
                    if (wt.isCapital()) {
                        // Capital missiles should have higher priority than standard missiles
                        threat = 12;
                        if (rangeToTarget > 50) {
                            av = 0;
                        } else if (rangeToTarget > 40) {
                            av += (wt.getExtAV() * weapon.getNWeapons());
                        } else if (rangeToTarget > 25) {
                            av += (wt.getLongAV() * weapon.getNWeapons());
                        } else if (rangeToTarget > 12) {
                            av += (wt.getMedAV() * weapon.getNWeapons());
                        } else {
                            av += (wt.getShortAV() * weapon.getNWeapons());
                        }
                    } else {
                        if (rangeToTarget > 25) {
                            av = 0;
                        } else if (rangeToTarget > 20) {
                            av += (wt.getExtAV() * weapon.getNWeapons());
                        } else if (rangeToTarget > 12) {
                            av += (wt.getLongAV() * weapon.getNWeapons());
                        } else if (rangeToTarget > 6) {
                            av += (wt.getMedAV() * weapon.getNWeapons());
                        } else {
                            av += (wt.getShortAV() * weapon.getNWeapons());
                        }
                    }
                    fDamage = (float) (av * threat);

                } else if ((wt.getAmmoType() == AmmoType.T_ARROW_IV)
                        || wt.getAmmoType() == BombType.B_HOMING) {
                    //This is for arrow IV AMS threat processing
                    fDamage = (float) wt.getRackSize();
                } else {
                    fDamage = wt.getDamage(rangeToTarget);
                }
            }

            // Infantry follow some special rules, but do fixed amounts of damage
            // Anti-mek attacks are weapon-like in nature, so include them here as well
            if (attacker instanceof Infantry) {
                if (wt.getInternalName() == Infantry.LEG_ATTACK) {
                    fDamage = 20.0f; // Actually 5, but the chance of crits
                    // deserves a boost
                // leg attacks are mutually exclusive with swarm attacks,
                } else {
                    boolean targetIsSwarmable = (target instanceof Mech) || (target instanceof Tank);

                    if (attacker.isConventionalInfantry()) {
                        if (wt.getInternalName() == Infantry.SWARM_MEK) {
                            // If the target is a Mek that is not swarmed, this is a
                            // good thing
                            if ((target.getSwarmAttackerId() == Entity.NONE) && targetIsSwarmable) {
                                fDamage = 1.5f
                                        * (float) infDamagePerTrooper
                                        * infShootingStrength;
                            }
                            // Otherwise, call it 0 damage
                            else {
                                fDamage = 0.0f;
                            }
                        } else {
                            // conventional weapons; field guns should be handled
                            // under the standard weapons section
                            fDamage = 0.6f
                                    * (float) infDamagePerTrooper
                                    * infShootingStrength;
                        }
                    } else {
                        // Battle armor units conducting swarm attack
                        if (wt.getInternalName() == Infantry.SWARM_MEK) {
                            // If the target is a Mek that is not swarmed, this is a
                            // good thing
                            if ((target.getSwarmAttackerId() == Entity.NONE) && targetIsSwarmable) {
                                // Overestimated, but the chance at crits and head
                                // shots deserves a boost
                                fDamage = 10.0f * baShootingStrength;
                            }
                            // Otherwise, call it 0 damage
                            else {
                                fDamage = 0.0f;
                            }
                        }
                    }
                }
            }

        }

        // Need to adjust damage if the target is infantry.
        if (g.getEntity(waa.getTargetId()).isConventionalInfantry()) {
            fDamage = directBlowInfantryDamage(fDamage, 0,
                    wt.getInfantryDamageClass(), ((Infantry) (g.getEntity(waa
                            .getTargetId()))).isMechanized(), false);
        }

        fDamage *= fChance;

        // Conventional infantry take double damage in the open
        if (g.getEntity(waa.getTargetId()).isConventionalInfantry()) {
            Hex e_hex = g.getBoard().getHex(
                    g.getEntity(waa.getTargetId()).getPosition().getX(),
                    g.getEntity(waa.getTargetId()).getPosition().getY());
            if (!e_hex.containsTerrain(Terrains.WOODS)
                && !e_hex.containsTerrain(Terrains.JUNGLE)
                && !e_hex.containsTerrain(Terrains.BUILDING)) {
                fDamage *= 2.0f;
            }

            // Cap damage to prevent run-away values
            if (infShootingStrength > 0) {
                fDamage = Math.min(infShootingStrength, fDamage);
            }
        }
        return fDamage;
    }

    /**
     * If the unit is carrying multiple types of ammo for the specified weapon,
     * cycle through them and choose the type best suited to engage the
     * specified target Value returned is expected damage Note that some ammo
     * types, such as infernos, do no damage or have special properties and so
     * the damage is an estimation of effectiveness
     */

    public static double getAmmoAdjDamage(Game cgame, WeaponAttackAction atk) {
        boolean no_bin = true;
        boolean multi_bin = false;

        double ammo_multiple, ex_damage, max_damage;

        Entity shooter, target;

        Mounted fabin, best_bin;
        AmmoType abin_type = new AmmoType();
        AmmoType fabin_type = new AmmoType();
        WeaponType wtype = new WeaponType();
        WeaponType target_weapon = new WeaponType();

        // Get shooter entity, target entity, and weapon being fired
        target = cgame.getEntity(atk.getTargetId());
        shooter = atk.getEntity(cgame);
        wtype = (WeaponType) shooter.getEquipment(atk.getWeaponId()).getType();

        max_damage = 0.0;

        // If the weapon doesn't require ammo, just get the estimated damage
        if (wtype.hasFlag(WeaponType.F_ENERGY)
            || wtype.hasFlag(WeaponType.F_ONESHOT)
            || wtype.hasFlag(WeaponType.F_INFANTRY)
            || (wtype.getAmmoType() == AmmoType.T_NA)) {
            return Compute.getExpectedDamage(cgame, atk, false);
        }

        // Get a list of ammo bins and the first valid bin
        fabin = null;
        best_bin = null;

        for (Mounted abin : shooter.getAmmo()) {
            if (shooter.loadWeapon(shooter.getEquipment(atk.getWeaponId()),
                                   abin)) {
                if (abin.getUsableShotsLeft() > 0) {
                    abin_type = (AmmoType) abin.getType();
                    if (!AmmoType.canDeliverMinefield(abin_type)) {
                        fabin = abin;
                        fabin_type = (AmmoType) fabin.getType();
                        break;
                    }
                }
            }
        }

        // To save processing time, lets see if we have more than one type of
        // bin
        // Thunder-type ammos and empty bins are excluded from the list
        for (Mounted abin : shooter.getAmmo()) {
            if (shooter.loadWeapon(shooter.getEquipment(atk.getWeaponId()),
                                   abin)) {
                if (abin.getUsableShotsLeft() > 0) {
                    abin_type = (AmmoType) abin.getType();
                    if (!AmmoType.canDeliverMinefield(abin_type)) {
                        no_bin = false;
                        if (abin_type.getMunitionType() != fabin_type
                                .getMunitionType()) {
                            multi_bin = true;
                            break;
                        }
                    }
                }
            }
        }

        // If no_bin is true, then either all bins are empty or contain
        // Thunder-type rounds and
        // we can safely say that the expected damage is 0.0
        // If no_bin is false, then we have at least one good bin
        if (no_bin) {
            return 0.0;
        }
        // If multi_bin is true, then multiple ammo types are present and an
        // appropriate type must be selected
        // If multi_bin is false, then all bin types are the same; skip down
        // to getting the expected damage
        if (!multi_bin) {
            return Compute.getExpectedDamage(cgame, atk, false);
        }
        if (multi_bin) {

            // Set default max damage as 0, and the best bin as the first
            // bin
            max_damage = 0.0;
            best_bin = fabin;

            // For each valid ammo bin
            for (Mounted abin : shooter.getAmmo()) {
                if (shooter.loadWeapon(shooter.getEquipment(atk.getWeaponId()), abin)) {
                    if (abin.getUsableShotsLeft() > 0) {
                        abin_type = (AmmoType) abin.getType();
                        if (!AmmoType.canDeliverMinefield(abin_type)) {

                            // Load weapon with specified bin
                            shooter.loadWeapon(shooter.getEquipment(atk.getWeaponId()), abin);
                            atk.setAmmoId(shooter.getEquipmentNum(abin));
                            atk.setAmmoMunitionType(abin_type.getMunitionType());

                            // Get expected damage
                            ex_damage = Compute.getExpectedDamage(cgame, atk, false);

                            // Calculate any modifiers due to ammo type
                            ammo_multiple = 1.0;

                            // Frag missiles, flechette AC rounds do double
                            // damage against conventional infantry
                            // and 0 damage against everything else
                            // Any further anti-personnel specialized rounds
                            // should be tested for here
                            if (((((abin_type.getAmmoType() == AmmoType.T_LRM)
                                    || (abin_type.getAmmoType() == AmmoType.T_LRM_IMP)
                                    || (abin_type.getAmmoType() == AmmoType.T_MML)
                                    || (abin_type.getAmmoType() == AmmoType.T_SRM)
                                    || (abin_type.getAmmoType() == AmmoType.T_SRM_IMP)))
                                    && (abin_type.getMunitionType().contains(AmmoType.Munitions.M_FRAGMENTATION)))
                                    || (((abin_type.getAmmoType() == AmmoType.T_AC)
                                            || (abin_type.getAmmoType() == AmmoType.T_LAC)
                                            || (abin_type.getAmmoType() == AmmoType.T_AC_IMP)
                                            || (abin_type.getAmmoType() == AmmoType.T_PAC))
                                            && (abin_type.getMunitionType().contains(AmmoType.Munitions.M_FLECHETTE)))) {
                                ammo_multiple = target.isConventionalInfantry() ? 2.0 : 0.0;
                            }

                            // LBX cluster rounds work better against units
                            // with little armor, vehicles, and Meks in
                            // partial cover
                            // Other ammo that deliver lots of small
                            // submunitions should be tested for here too
                            if (((abin_type.getAmmoType() == AmmoType.T_AC_LBX)
                                    || (abin_type.getAmmoType() == AmmoType.T_AC_LBX_THB)
                                    || (abin_type.getAmmoType() == AmmoType.T_SBGAUSS))
                                    && (abin_type.getMunitionType().contains(AmmoType.Munitions.M_CLUSTER))) {
                                if (target.getArmorRemainingPercent() <= 0.25) {
                                    ammo_multiple = 1.0 + (wtype.getRackSize() / 10.0);
                                }
                                if (target instanceof Tank) {
                                    ammo_multiple += 1.0;
                                }
                            }

                            // AP autocannon rounds work much better against
                            // Meks and vehicles than infantry,
                            // give a damage boost in proportion to calibre
                            // to reflect scaled crit chance
                            // Other armor-penetrating ammo types should be
                            // tested here, such as Tandem-charge SRMs
                            if (((abin_type.getAmmoType() == AmmoType.T_AC)
                                    || (abin_type.getAmmoType() == AmmoType.T_LAC)
                                    || (abin_type.getAmmoType() == AmmoType.T_AC_IMP)
                                    || (abin_type.getAmmoType() == AmmoType.T_PAC))
                                && (abin_type.getMunitionType().contains(AmmoType.Munitions.M_ARMOR_PIERCING))) {
                                if ((target instanceof Mech)
                                    || (target instanceof Tank)) {
                                    ammo_multiple = 1.0 + (wtype.getRackSize() / 10);
                                }
                                if (target instanceof Infantry) {
                                    ammo_multiple = 0.6;
                                }
                            }

                            // Inferno SRMs work better against overheating
                            // Meks that are not/almost not on fire,
                            // and against vehicles and protos if allowed by
                            // game option
                            if (((abin_type.getAmmoType() == AmmoType.T_SRM)
                                    || (abin_type.getAmmoType() == AmmoType.T_SRM_IMP)
                                    || (abin_type.getAmmoType() == AmmoType.T_MML))
                                && (abin_type.getMunitionType().contains(AmmoType.Munitions.M_INFERNO))) {
                                ammo_multiple = 0.5;
                                if (target instanceof Mech) {
                                    if ((target.infernos.getTurnsLeftToBurn() < 4)
                                        && (target.heat >= 5)) {
                                        ammo_multiple = 1.1;
                                    }
                                }
                                if ((target instanceof Tank)
                                    && !(cgame.getOptions()
                                              .booleanOption(OptionsConstants.ADVCOMBAT_VEHICLES_SAFE_FROM_INFERNOS))) {
                                    ammo_multiple = 1.1;
                                }
                                if ((target instanceof Protomech)
                                    && !(cgame.getOptions()
                                              .booleanOption(OptionsConstants.ADVCOMBAT_PROTOS_SAFE_FROM_INFERNOS))) {
                                    ammo_multiple = 1.1;
                                }
                            }

                            // Narc beacon doesn't really do damage but if
                            // the target is not infantry and doesn't have
                            // one, give 'em one by making it an attractive
                            // option
                            if ((wtype.getAmmoType() == AmmoType.T_NARC)
                                    && (abin_type.getMunitionType().contains(AmmoType.Munitions.M_STANDARD))) {
                                if (!(target.isNarcedBy(shooter.getOwner().getTeam()))
                                        && !(target instanceof Infantry)) {
                                    ex_damage = 5.0;
                                } else {
                                    ex_damage = 0.5;
                                }
                            }

                            // iNarc beacon doesn't really do damage, but if
                            // the target is not infantry and doesn't have
                            // one, give 'em one by making it an attractive
                            // option
                            if (wtype.getAmmoType() == AmmoType.T_INARC) {
                                if ((abin_type.getMunitionType().contains(AmmoType.Munitions.M_STANDARD))
                                        && !(target instanceof Infantry)) {
                                    if (!(target.isINarcedBy(shooter.getOwner().getTeam()))) {
                                        ex_damage = 7.0;
                                    } else {
                                        ex_damage = 1.0;
                                    }
                                }

                                // iNarc ECM doesn't really do damage, but
                                // if the target has a C3 link or missile
                                // launchers
                                // make it a priority
                                // Checking for actual ammo types carried
                                // would be nice, but can't be sure of exact
                                // loads
                                // when "true" double blind is implemented
                                if ((abin_type.getAmmoType() == AmmoType.T_INARC)
                                        && (abin_type.getMunitionType().contains(AmmoType.Munitions.M_ECM))
                                        && !(target instanceof Infantry)) {
                                    if (!target.isINarcedWith(INarcPod.ECM)) {
                                        if (!(target.getC3MasterId() == Entity.NONE)
                                                || target.hasC3M()
                                                || target.hasC3MM()
                                                || target.hasC3i()) {
                                            ex_damage = 8.0;
                                        } else {
                                            ex_damage = 0.5;
                                        }
                                        for (Mounted weapon : shooter.getWeaponList()) {
                                            target_weapon = (WeaponType) weapon.getType();
                                            if ((target_weapon.getAmmoType() == AmmoType.T_LRM)
                                                    || (target_weapon.getAmmoType() == AmmoType.T_LRM_IMP)
                                                    || (target_weapon.getAmmoType() == AmmoType.T_MML)
                                                    || (target_weapon.getAmmoType() == AmmoType.T_SRM)
                                                    || (target_weapon.getAmmoType() == AmmoType.T_SRM_IMP)) {
                                                ex_damage = ex_damage + (target_weapon.getRackSize() / 2);
                                            }
                                        }
                                    }
                                }

                                // iNarc Nemesis doesn't really do damage,
                                // but if the target is not infantry and
                                // doesn't have
                                // one give it a try; make fast units a
                                // priority because they are usually out
                                // front
                                if ((abin_type.getAmmoType() == AmmoType.T_INARC)
                                        && (abin_type.getMunitionType().contains(AmmoType.Munitions.M_NEMESIS))
                                        && !(target instanceof Infantry)) {
                                    if (!target.isINarcedWith(INarcPod.NEMESIS)) {
                                        ex_damage = (double) (target.getWalkMP() + target.getJumpMP()) / 2;
                                    } else {
                                        ex_damage = 0.5;
                                    }
                                }
                            }

                            // If the adjusted damage is highest, store the
                            // damage and bin
                            if ((ex_damage * ammo_multiple) > max_damage) {
                                max_damage = ex_damage * ammo_multiple;
                                best_bin = abin;
                            }
                        }
                    }
                }
            }

            // Now that the best bin has been found, reload the weapon with
            // it
            shooter.loadWeapon(shooter.getEquipment(atk.getWeaponId()), best_bin);
            atk.setAmmoId(shooter.getEquipmentNum(best_bin));
            atk.setAmmoMunitionType(abin_type.getMunitionType());
        }
        return max_damage;
    }

    /**
     * If this is an ultra or rotary cannon, lets see about 'spinning it up' for
     * extra damage
     *
     * @return the <code>int</code> ID of weapon mode
     */
    public static int spinUpCannon(Game cgame, WeaponAttackAction atk) {
        return spinUpCannon(cgame, atk, Compute.d6(2) - 1);
    }

    /**
     * If this is an ultra or rotary cannon, lets see about 'spinning it up' for
     * extra damage
     *
     * @return the <code>int</code> ID of weapon mode
     */

    public static int spinUpCannon(Game cgame, WeaponAttackAction atk, int spinupThreshold) {

        int threshold = 12;
        int final_spin;
        Entity shooter;
        Mounted weapon;
        WeaponType wtype = new WeaponType();

        // Double check this is an Ultra or Rotary cannon
        // or a standard AC with the TacOps rapid fire rule turned on
        shooter = atk.getEntity(cgame);
        weapon = shooter.getEquipment(atk.getWeaponId());
        wtype = (WeaponType) shooter.getEquipment(atk.getWeaponId()).getType();

        boolean rapidAC = (wtype.getAmmoType() == AmmoType.T_AC) && cgame.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_RAPID_AC);

        if (!((wtype.getAmmoType() == AmmoType.T_AC_ULTRA)
              || (wtype.getAmmoType() == AmmoType.T_AC_ULTRA_THB)
              || (wtype.getAmmoType() == AmmoType.T_AC_ROTARY)
              || rapidAC)) {
            return 0;
        }

        // Get the to-hit number
        threshold = atk.toHit(cgame).getValue();

        // Set the weapon to single shot mode
        weapon.setMode(rapidAC ? "" : Weapon.MODE_AC_SINGLE);
        final_spin = 0;

        // If weapon can't hit target, exit the function with the weapon on
        // single shot
        if ((threshold == TargetRoll.IMPOSSIBLE)
            || (threshold == TargetRoll.AUTOMATIC_FAIL)) {
            return final_spin;
        }

        // If random roll is >= to-hit + 1, then set double-spin
        if (spinupThreshold >= threshold) {
            final_spin = 1;
            if ((wtype.getAmmoType() == AmmoType.T_AC_ULTRA)
                || (wtype.getAmmoType() == AmmoType.T_AC_ULTRA_THB)) {
                weapon.setMode(Weapon.MODE_UAC_ULTRA);
            } else if (wtype.getAmmoType() == AmmoType.T_AC_ROTARY) {
                weapon.setMode(Weapon.MODE_RAC_TWO_SHOT);
            } else if (rapidAC) {
                weapon.setMode(Weapon.MODE_AC_RAPID);
            }
        }

        // If this is a Rotary cannon
        if (wtype.getAmmoType() == AmmoType.T_AC_ROTARY) {

            // If random roll is >= to-hit + 2 then set to quad-spin
            if (spinupThreshold >= (threshold + 1)) {
                final_spin = 2;
                weapon.setMode(Weapon.MODE_RAC_FOUR_SHOT);
            }

            // If random roll is >= to-hit + 3 then set to six-spin
            if (spinupThreshold >= (threshold + 2)) {
                final_spin = 3;
                weapon.setMode(Weapon.MODE_RAC_SIX_SHOT);
            }
        }
        return final_spin;
    }

    /**
     * Checks to see if a target is in arc of the specified weapon, on the
     * specified entity
     */
    public static boolean isInArc(Game game, int attackerId, int weaponId,
            Targetable t) {
        Entity ae = game.getEntity(attackerId);
        if ((ae instanceof Mech)
            && (((Mech) ae).getGrappled() == t.getId())) {
            return true;
        }
        int facing = ae.isSecondaryArcWeapon(weaponId) ? ae
                .getSecondaryFacing() : ae.getFacing();
        if ((ae instanceof Tank)
            && (ae.getEquipment(weaponId).getLocation() == ((Tank) ae)
                .getLocTurret2())) {
            facing = ((Tank) ae).getDualTurretFacing();
        }
        if (ae.getEquipment(weaponId).isMechTurretMounted()) {
            facing = ae.getSecondaryFacing()
                     + (ae.getEquipment(weaponId).getFacing() % 6);
        }
        Coords aPos = ae.getPosition();
        Vector<Coords> tPosV = new Vector<>();
        Coords tPos = t.getPosition();
        // aeros in the same hex in space may still be able to fire at one
        // another. First I need to translate
        // their positions to see who was further back
        if (game.getBoard().inSpace()
            && ae.getPosition().equals(t.getPosition())
            && ae.isAero() && t.isAero()) {
            int moveSort = shouldMoveBackHex(ae, (Entity) t);
            if (moveSort < 0) {
                aPos = ae.getPriorPosition();
            }
            if (moveSort > 0) {
                tPos = ((Entity) t).getPriorPosition();
            }
        }

        // Allow dive-bombing VTOLs to attack the hex they are in, if they didn't select one for bombing while moving.
        if ((ae.getMovementMode() == EntityMovementMode.VTOL)
                && aPos.equals(tPos)) {
            if (ae.getEquipment(weaponId).getType().hasFlag(WeaponType.F_DIVE_BOMB)) {
                return true;
            }
        }

        // if using advanced AA options, then ground-to-air fire determines arc
        // by closest position
        if (isGroundToAir(ae, t) && (t instanceof Entity)) {
            tPos = getClosestFlightPath(ae.getId(), ae.getPosition(),
                    (Entity) t);
        }

        // AMS defending against Ground to Air fire needs to calculate arc based on the closest flight path
        // Technically it's an AirToGround attack since the AMS is on the aircraft
        if (isAirToGround(ae, t) && (t instanceof Entity)
                && (ae.getEquipment(weaponId).getType().hasFlag(WeaponType.F_AMS)
                        || ae.getEquipment(weaponId).getType().hasFlag(WeaponType.F_AMSBAY))) {
            Entity te = (Entity) t;
            aPos = getClosestFlightPath(te.getId(), te.getPosition(),
                    ae);
        }

        tPosV.add(tPos);
        // check for secondary positions
        if ((t instanceof Entity)
            && (null != ((Entity) t).getSecondaryPositions())) {
            for (int key : ((Entity) t).getSecondaryPositions().keySet()) {
                tPosV.add(((Entity) t).getSecondaryPositions().get(key));
            }
        }
        return Compute.isInArc(aPos, facing, tPosV, ae.getWeaponArc(weaponId));
    }

    /**
     * Returns true if the line between source Coords and target goes through
     * the hex in front of the attacker
     */
    public static boolean isThroughFrontHex(Game game, Coords src, Entity t) {
        Coords dest = t.getPosition();
        int fa = dest.degree(src) - (t.getFacing() * 60);
        if (fa < 0) {
            fa += 360;
        }
        return (fa > 330) || (fa < 30);
    }

    /**
     * Converts the facing of a vehicular grenade launcher to the corresponding firing arc.
     *
     * @param facing The VGL facing returned by {@link Mounted#getFacing()}
     * @return       The firing arc
     */
    public static int firingArcFromVGLFacing(int facing) {
        return VGL_FIRING_ARCS[facing % 6];
    }

    public static boolean isInArc(Coords src, int facing, Targetable target,
                                  int arc) {

        Vector<Coords> tPosV = new Vector<>();
        tPosV.add(target.getPosition());
        // check for secondary positions
        if ((target instanceof Entity)
            && (null != ((Entity) target).getSecondaryPositions())) {
            for (int key : ((Entity) target).getSecondaryPositions().keySet()) {
                tPosV.add(((Entity) target).getSecondaryPositions().get(key));
            }
        }

        return isInArc(src, facing, tPosV, arc);
    }

    public static boolean isInArc(Coords src, int facing, Coords dest, int arc) {
        Vector<Coords> destV = new Vector<>();
        destV.add(dest);
        return isInArc(src, facing, destV, arc);
    }

    /**
     * Returns true if the target is in the specified arc. Note: This has to
     * take vectors of coordinates to account for potential secondary positions
     *
     * @param src    the attack coordinates
     * @param facing the appropriate attacker sfacing
     * @param destV  A vector of target coordinates
     * @param arc    the arc
     */
    public static boolean isInArc(Coords src, int facing, Vector<Coords> destV,
                                  int arc) {
        if ((src == null) || (destV == null)) {
            return true;
        }

        // Jay: I have to adjust this to take in vectors of coordinates to
        // account for secondary positions of the
        // target - I am fairly certain that secondary positions of the attacker
        // shouldn't matter because you don't get
        // to move the angle based on the secondary positions

        // if any of the destination coords are in the right place, then return
        // true
        for (Coords dest : destV) {
            // calculate firing angle
            int fa = src.degree(dest) - (facing * 60);
            if (fa < 0) {
                fa += 360;
            }
            // is it in the specifed arc?
            switch (arc) {
                case ARC_FORWARD:
                    if ((fa >= 300) || (fa <= 60)) {
                        return true;
                    }
                    break;
                case Compute.ARC_RIGHTARM:
                    if ((fa >= 300) || (fa <= 120)) {
                        return true;
                    }
                    break;
                case Compute.ARC_LEFTARM:
                    if ((fa >= 240) || (fa <= 60)) {
                        return true;
                    }
                    break;
                case ARC_REAR:
                    if ((fa > 120) && (fa < 240)) {
                        return true;
                    }
                    break;
                case ARC_RIGHTSIDE:
                    if ((fa > 60) && (fa <= 120)) {
                        return true;
                    }
                    break;
                case ARC_LEFTSIDE:
                    if ((fa < 300) && (fa >= 240)) {
                        return true;
                    }
                    break;
                case ARC_MAINGUN:
                    if ((fa >= 240) || (fa <= 120)) {
                        return true;
                    }
                    break;
                case ARC_360:
                    return true;
                case ARC_NORTH:
                    if ((fa >= 270) || (fa <= 30)) {
                        return true;
                    }
                    break;
                case ARC_EAST:
                    if ((fa >= 30) && (fa <= 150)) {
                        return true;
                    }
                    break;
                case ARC_WEST:
                    if ((fa >= 150) && (fa <= 270)) {
                        return true;
                    }
                    break;
                case ARC_NOSE:
                    if ((fa > 300) || (fa < 60)) {
                        return true;
                    }
                    break;
                case ARC_NOSE_WPL:
                    if ((fa > 240) || (fa < 120)) {
                        return true;
                    }
                    break;
                case ARC_LWING:
                    if ((fa > 300) || (fa <= 0)) {
                        return true;
                    }
                    break;
                case ARC_LWING_WPL:
                    if ((fa > 240) || (fa < 60)) {
                        return true;
                    }
                    break;
                case ARC_RWING:
                    if ((fa >= 0) && (fa < 60)) {
                        return true;
                    }
                    break;
                case ARC_RWING_WPL:
                    if ((fa > 300) || (fa < 120)) {
                        return true;
                    }
                    break;
                case ARC_LWINGA:
                    if ((fa >= 180) && (fa < 240)) {
                        return true;
                    }
                    break;
                case ARC_LWINGA_WPL:
                    if ((fa > 120) && (fa < 300)) {
                        return true;
                    }
                    break;
                case ARC_RWINGA:
                    if ((fa > 120) && (fa <= 180)) {
                        return true;
                    }
                    break;
                case ARC_RWINGA_WPL:
                    if ((fa > 60) && (fa < 240)) {
                        return true;
                    }
                    break;
                case ARC_AFT:
                    if ((fa > 120) && (fa < 240)) {
                        return true;
                    }
                    break;
                case ARC_AFT_WPL:
                    if ((fa > 60) && (fa < 300)) {
                        return true;
                    }
                    break;
                case ARC_LEFTSIDE_SPHERE:
                    if ((fa > 240) || (fa < 0)) {
                        return true;
                    }
                    break;
                case ARC_LEFTSIDE_SPHERE_WPL:
                    if ((fa > 180) || (fa < 60)) {
                        return true;
                    }
                    break;
                case ARC_RIGHTSIDE_SPHERE:
                    if ((fa > 0) && (fa < 120)) {
                        return true;
                    }
                    break;
                case ARC_RIGHTSIDE_SPHERE_WPL:
                    if ((fa > 300) || (fa < 180)) {
                        return true;
                    }
                    break;
                case ARC_LEFTSIDEA_SPHERE:
                    if ((fa > 180) && (fa < 300)) {
                        return true;
                    }
                    break;
                case ARC_LEFTSIDEA_SPHERE_WPL:
                    if ((fa > 120) && (fa < 360)) {
                        return true;
                    }
                    break;
                case ARC_RIGHTSIDEA_SPHERE:
                    if ((fa > 60) && (fa < 180)) {
                        return true;
                    }
                    break;
                case ARC_RIGHTSIDEA_SPHERE_WPL:
                    if ((fa > 0) && (fa < 240)) {
                        return true;
                    }
                    break;
                case ARC_LEFT_BROADSIDE:
                    if ((fa >= 240) && (fa <= 300)) {
                        return true;
                    }
                    break;
                case ARC_LEFT_BROADSIDE_WPL:
                    if ((fa > 180) && (fa <= 360)) {
                        return true;
                    }
                    break;
                case ARC_RIGHT_BROADSIDE:
                    if ((fa >= 60) && (fa <= 120)) {
                        return true;
                    }
                    break;
                case ARC_RIGHT_BROADSIDE_WPL:
                    if ((fa > 0) && (fa < 180)) {
                        return true;
                    }
                    break;
                case ARC_LEFT_SPHERE_GROUND:
                    if ((fa >= 180) && (fa < 360)) {
                        return true;
                    }
                    break;
                case ARC_RIGHT_SPHERE_GROUND:
                    if ((fa >= 0) && (fa < 180)) {
                        return true;
                    }
                    break;
                case ARC_TURRET:
                    if ((fa >= 330) || (fa <= 30)) {
                        return true;
                    }
                    break;
                case ARC_SPONSON_TURRET_LEFT:
                case ARC_PINTLE_TURRET_LEFT:
                    if ((fa >= 180) || (fa == 0)) {
                        return true;
                    }
                    break;
                case ARC_SPONSON_TURRET_RIGHT:
                case ARC_PINTLE_TURRET_RIGHT:
                    if ((fa >= 0) && (fa <= 180)) {
                        return true;
                    }
                    break;
                case ARC_PINTLE_TURRET_FRONT:
                    if ((fa >= 270) || (fa <= 90)) {
                        return true;
                    }
                    break;
                case ARC_PINTLE_TURRET_REAR:
                    if ((fa >= 90) && (fa <= 270)) {
                        return true;
                    }
                    break;
                case ARC_VGL_FRONT:
                    return (fa >= 270) || (fa <= 90);
                case ARC_VGL_RF:
                    return (fa >= 330) || (fa <= 150);
                case ARC_VGL_RR:
                    return (fa >= 30) && (fa <= 210);
                case ARC_VGL_REAR:
                    return (fa >= 90) && (fa <= 270);
                case ARC_VGL_LR:
                    return (fa >= 150) && (fa <= 330);
                case ARC_VGL_LF:
                    return (fa >= 210) || (fa <= 30);
            }
        }
        // if we got here then no matches
        return false;
    }

    /**
     * checks to see whether the target is within visual range of the entity,
     * but not necessarily LoS
     */
    public static boolean inVisualRange(Game game, Entity ae, Targetable target) {
        return inVisualRange(game, null, ae, target);
    }

    /**
     * Determine whether the attacking entity is within visual range of the
     * target.  This requires line of sight effects to determine if there are
     * certain intervening obstructions, like smoke, that can reduce visual
     * range.  Since repeated LoSEffects computations can be expensive, it is
     * possible to pass in the LosEffects, since they are commonly already
     * computed when this method is called.
     *
     * @param game The current {@link Game}
     * @param los
     * @param ae
     * @param target
     * @return
     */
    public static boolean inVisualRange(Game game, LosEffects los, Entity ae,
            Targetable target) {
        //Use firing solution if Advanced Sensors is on
        if (game.getOptions().booleanOption(OptionsConstants.ADVAERORULES_STRATOPS_ADVANCED_SENSORS)
                && target.getTargetType() == Targetable.TYPE_ENTITY
                && game.getBoard().inSpace()) {
            Entity te = (Entity) target;
            return hasAnyFiringSolution(game, te.getId());
        }
        boolean teIlluminated = false;
        if (target.getTargetType() == Targetable.TYPE_ENTITY) {
            Entity te = (Entity) target;
            teIlluminated = te.isIlluminated();
            if (te.isOffBoard()) {
                return false;
            }
        }

        // Target may be in an illuminated hex
        if (!teIlluminated) {
            teIlluminated = !IlluminationLevel.determineIlluminationLevel(game, target.getPosition()).isNone();
        }

        // if either does not have a position then return false
        if ((ae.getPosition() == null) || (target.getPosition() == null)) {
            return false;
        }

        // check visual range based on planetary conditions
        if (los == null) {
            los = LosEffects.calculateLOS(game, ae, target);
        }
        int visualRange = getVisualRange(game, ae, los, teIlluminated);

        //Check for factors that only apply to an entity target
        Coords targetPos = target.getPosition();
        if (target.getTargetType() == Targetable.TYPE_ENTITY) {
            Entity te = (Entity) target;

            // check for camo and null sig on the target
            if (te.isVoidSigActive()) {
                visualRange = visualRange / 4;
            } else if (te.hasWorkingMisc(MiscType.F_VISUAL_CAMO, -1)) {
                visualRange = visualRange / 2;
            } else if (te.isChameleonShieldActive()) {
                visualRange = visualRange / 2;
            } else if (te.isConventionalInfantry() && ((Infantry) te).hasSneakCamo()) {
                visualRange = visualRange / 2;
            }

            // Ground targets pick the closest path to Aeros (TW pg 107)
            if ((te.isAero()) && isGroundToAir(ae, target)) {
                targetPos = Compute.getClosestFlightPath(ae.getId(),
                        ae.getPosition(), te);
            }
            //Airborne aeros can only see ground targets they overfly, and only at Alt <=8
            if (isAirToGround(ae, target)) {
                if (ae.getAltitude() > 8) {
                    return false;
                }
                if (ae.passedOver(target)) {
                    return true;
                } else {
                    return false;
                }
            }
        }

        visualRange = Math.max(visualRange, 1);
        int distance;
        // Ground distance
        distance = ae.getPosition().distance(targetPos);
        //Need to track difference in altitude, not just add altitude to the range
        distance += Math.abs(2 * target.getAltitude() - 2 * ae.getAltitude());
        return distance <= visualRange;

    }

    //Space Combat Detection stuff

    /**
     * Checks to see if an entity has already been detected by anyone
     * Used for sensor return icons on board
     *
     * @param game The current {@link Game}
     * @param targetId - the ID# of the target entity we're looking for
     */
    public static boolean isAnySensorContact(Game game, int targetId) {
        for (Entity detector : game.getEntitiesVector()) {
            if (detector.hasSensorContactFor(targetId) && game.getEntity(targetId) != null) {
                game.getEntity(targetId).addBeenDetectedBy(detector.getOwner());
                return true;
            }
        }
        return false;
    }

    /**
     * Checks to see if target entity has already appeared on @detector's sensors
     * Used with Naval C3 to determine if @detector can fire weapons at @target
     * @param detector - the entity making a sensor scan
     * @param targetId - the entity id of the scan target
     */
    public static boolean hasSensorContact(Entity detector, int targetId) {
        return detector.hasSensorContactFor(targetId);
    }

    /**
     * Checks to see if an entity is in anyone's firing solutions list
     * Used for visibility
     *
     * @param game The current {@link Game}
     * @param targetId - the ID # of the target we're firing at
     */
    public static boolean hasAnyFiringSolution(Game game, int targetId) {
        for (Entity detector : game.getEntitiesVector()) {
            if (detector.hasFiringSolutionFor(targetId) && game.getEntity(targetId) != null) {
                game.getEntity(targetId).addBeenSeenBy(detector.getOwner());
                return true;
            }
        }
        return false;
    }

    /**
     * Calculates the ECM effects in play between a detector and target pair
     *
     * @param game The current {@link Game}
     * @param ae - the entity making a sensor scan
     * @param target - the entity we're trying to spot
     * @return
     */
    private static int calcSpaceECM(Game game, Entity ae,
            Targetable target) {
        int mod = 0;
        int ecm = ComputeECM.getLargeCraftECM(ae, ae.getPosition(), target.getPosition());
        if (!ae.isLargeCraft()) {
            ecm += ComputeECM.getSmallCraftECM(ae, ae.getPosition(), target.getPosition());
        }
        ecm = Math.min(4, ecm);
        int eccm = 0;
        if (ae.isLargeCraft()) {
            eccm = ((Aero) ae).getECCMBonus();
        }
        if (ecm > 0) {
            mod += ecm;
            if (eccm > 0) {
                mod -= (Math.min(ecm, eccm));
            }
        }
        return mod;
    }

    /**
     * Calculates the Sensor Shadow effects in play between a detector and target pair
     *
     * @param game The current {@link Game}
     * @param ae the entity making a sensor scan
     * @param target the entity we're trying to spot
     * @return
     */
    private static int calcSensorShadow(Game game, Entity ae, Targetable target) {
        int mod = 0;
        if (target.getTargetType() != Targetable.TYPE_ENTITY) {
            return 0;
        }
        Entity te = (Entity) target;
        for (Entity en : Compute.getAdjacentEntitiesAlongAttack(ae.getPosition(), target.getPosition(), game)) {
            if (!en.isEnemyOf(te) && en.isLargeCraft() && !en.equals((Entity) te) && ((en.getWeight() - te.getWeight()) >= -100000.0)) {
                mod ++;
                break;
            }
        }
        for (Entity en : game.getEntitiesVector(target.getPosition())) {
            if (!en.isEnemyOf(te) && en.isLargeCraft() && !en.equals((Entity) ae) && !en.equals((Entity) te)
                    && ((en.getWeight() - te.getWeight()) >= -100000.0)) {
                mod ++;
                break;
            }
        }
        return mod;
    }

    /**
     * Updates an entity's firingSolutions, removing any objects that no longer meet criteria for being
     * tracked as targets. Also, if the detecting entity no longer meets criteria for having firing solutions,
     * empty the list. We wouldn't want a dead ship to be providing NC3 data, now would we...
     */
    public static void updateFiringSolutions(Game game, Entity detector) {
        List<Integer> toRemove = new ArrayList<>();
        //Flush the detecting unit's firing solutions if any of these conditions applies
        if (detector.isDestroyed()
                || detector.isDoomed()
                || detector.getTransportId() != Entity.NONE
                || detector.isPartOfFighterSquadron()
                || detector.isOffBoard()
                || detector.getPosition() == null) {
            detector.clearFiringSolutions();
            return;
        }
        for (int id : detector.getFiringSolutions()) {
            Entity target = game.getEntity(id);
            //The target should be removed if it's off the board for any of these reasons
            if (target == null
                    || target.getPosition() == null
                    || target.isDestroyed()
                    || target.isDoomed()
                    || target.getTransportId() != Entity.NONE
                    || target.isPartOfFighterSquadron()
                    || target.isOffBoard()) {
                toRemove.add(id);
                continue;
            }
            Coords targetPos = target.getPosition();
            int distance = detector.getPosition().distance(targetPos);
            //Per SO p119, optical firing solutions are lost if the target moves beyond 1/10 max range
            if (detector.getActiveSensor().getType() == Sensor.TYPE_AERO_THERMAL
                    && distance > Sensor.ASF_OPTICAL_FIRING_SOLUTION_RANGE) {
                    toRemove.add(id);
            } else if (detector.getActiveSensor().getType() == Sensor.TYPE_SPACECRAFT_THERMAL
                    && distance > Sensor.LC_OPTICAL_FIRING_SOLUTION_RANGE) {
                    toRemove.add(id);
            //For ASF sensors, make sure we're using the space range of 555...
            } else if (detector.getActiveSensor().getType() == Sensor.TYPE_AERO_SENSOR
                    && distance > Sensor.ASF_RADAR_MAX_RANGE) {
                    toRemove.add(id);
            } else {
                //Radar firing solutions are only lost if the target moves out of range
                if (distance > detector.getActiveSensor().getRangeByBracket()) {
                    toRemove.add(id);
                }
            }
        }
        detector.removeFiringSolution(toRemove);
    }

    /**
     * Updates an entity's sensorContacts, removing any objects that no longer meet criteria for being
     * tracked. Also, if the detecting entity no longer meets criteria for having sensor contacts,
     * empty the list. We wouldn't want a dead ship to be providing sensor data, now would we...
     */
    public static void updateSensorContacts(Game game, Entity detector) {
        List<Integer> toRemove = new ArrayList<>();
        //Flush the detecting unit's sensor contacts if any of these conditions applies
        if (detector.getPosition() == null
                || detector.isDestroyed()
                || detector.isDoomed()
                || detector.getTransportId() != Entity.NONE
                || detector.isPartOfFighterSquadron()
                || detector.isOffBoard()) {
            detector.clearSensorContacts();
            return;
        }
        for (int id : detector.getSensorContacts()) {
            Entity target = game.getEntity(id);
            //The target should be removed if it's off the board for any of these reasons
            if (target == null
                    || target.getPosition() == null
                    || target.isDestroyed()
                    || target.isDoomed()
                    || target.getTransportId() != Entity.NONE
                    || target.isPartOfFighterSquadron()
                    || target.isOffBoard()) {
                toRemove.add(id);
                continue;
            }
            //And now calculate whether or not the target has moved out of range. Per SO p117-119,
            //sensor contacts remain tracked on the plotting board until this occurs.
            Coords targetPos = target.getPosition();
            int distance = detector.getPosition().distance(targetPos);
            if (distance > detector.getActiveSensor().getRangeByBracket()) {
                toRemove.add(id);
            }
        }
        detector.removeSensorContact(toRemove);
    }

    /**
     * If the game is in space, "visual range" represents a firing solution as defined in SO starting on p117
     * Also, in most cases each target must be detected with sensors before it can be seen, so we need to make
     * sensor rolls for detection. This should only be used if Tacops sensor rules are in use.
     * This requires line of sight effects to determine if there are
     * certain intervening obstructions, like sensor shadows, asteroids and that sort of thing, that can reduce visual
     * range. Since repeated LoSEffects computations can be expensive, it is
     * possible to pass in the LosEffects, since they are commonly already
     * computed when this method is called.
     *
     * @param game The current {@link Game}
     * @param ae the entity making a sensor scan
     * @param target the entity we're trying to spot
     * @return
     */
    public static boolean calcFiringSolution(Game game, Entity ae,
            Targetable target) {
        if (target.getTargetType() == Targetable.TYPE_ENTITY) {
            Entity te = (Entity) target;
            if (te.isOffBoard()) {
                return false;
            }
        }

        //NPE check. Fighter squadrons don't start with sensors, but pick them up from the component fighters each round
        if (ae.getActiveSensor() == null) {
            return false;
        }

        //ESM sensor can't produce a firing solution
        if (ae.getActiveSensor().getType() == Sensor.TYPE_SPACECRAFT_ESM) {
            return false;
        }
        Coords targetPos = target.getPosition();
        int distance = ae.getPosition().distance(targetPos);
        int roll = Compute.d6(2);
        int tn = ae.getCrew().getPiloting();
        int autoVisualRange = 1;
        int outOfVisualRange = (ae.getActiveSensor().getRangeByBracket());
        int rangeIncrement = (int) Math.ceil(outOfVisualRange / 10.0);

        //A bit of a hack here. "Aero Sensors" return the ground range, because Sensor doesn't know about Game or Entity
        //to do otherwise. We need to use the space range instead.
        if (ae.getActiveSensor().getType() == Sensor.TYPE_AERO_SENSOR) {
            outOfVisualRange = Sensor.ASF_RADAR_MAX_RANGE;
            rangeIncrement = Sensor.ASF_RADAR_AUTOSPOT_RANGE;
        }

        if (distance > outOfVisualRange) {
            return false;
        }

        if (ae instanceof Aero) {
            Aero aero = (Aero) ae;
            //Account for sensor damage
            if (aero.isAeroSensorDestroyed()) {
                return false;
            } else {
                tn += aero.getSensorHits();
            }
        }

        //Targets at 1/10 max range are automatically detected
        if (ae.getActiveSensor().getType() == Sensor.TYPE_AERO_SENSOR) {
            autoVisualRange = Sensor.ASF_RADAR_AUTOSPOT_RANGE;
        } else if (ae.getActiveSensor().getType() == Sensor.TYPE_SPACECRAFT_RADAR) {
            autoVisualRange = Sensor.LC_RADAR_AUTOSPOT_RANGE;
        } else if (ae.getActiveSensor().getType() == Sensor.TYPE_AERO_THERMAL) {
            autoVisualRange = Sensor.ASF_OPTICAL_FIRING_SOLUTION_RANGE;
        } else if (ae.getActiveSensor().getType() == Sensor.TYPE_SPACECRAFT_THERMAL) {
            autoVisualRange = Sensor.LC_OPTICAL_FIRING_SOLUTION_RANGE;
        }

        if (distance <= autoVisualRange) {
            return true;
        }

        //Apply Sensor Geek SPA, if present
        if (ae.hasAbility(OptionsConstants.UNOFF_SENSOR_GEEK)) {
            tn -= 2;
        }

        //Otherwise, we add +1 to the tn for detection for each increment of the autovisualrange between attacker and target
        tn += (distance / rangeIncrement);

        // Apply ECM/ECCM effects
        if (game.getOptions().booleanOption(OptionsConstants.ADVAERORULES_STRATOPS_ECM)) {
            tn += calcSpaceECM(game, ae, target);
        }

        // Apply large craft sensor shadows
        if (game.getOptions().booleanOption(OptionsConstants.ADVAERORULES_STRATOPS_SENSOR_SHADOW)) {
            tn += calcSensorShadow(game, ae, target);
        }

        //Apply modifiers for attacker's equipment
        //-2 for a working Large NCSS
        if (ae.hasWorkingMisc(MiscType.F_LARGE_COMM_SCANNER_SUITE)) {
            tn -= 2;
        }
        //-1 for a working Small NCSS
        if (ae.hasWorkingMisc(MiscType.F_SMALL_COMM_SCANNER_SUITE)) {
            tn -= 1;
        }
        // -2 for any type of BAP or EW Equipment. ECM is already accounted for, so don't let the BAP check do that
        if (ae.hasWorkingMisc(MiscType.F_EW_EQUIPMENT)
                || ae.hasBAP(false)) {
            tn -= 2;
        }

        // Now, determine if we've detected the target this round
        return roll >= tn;
    }

    /**
     * Determines whether we have an "object" detection as defined in SO's Advanced Sensors rules starting on p117
     *
     * @param game The current {@link Game}
     * @param ae the entity making a sensor scan
     * @param target the entity we're trying to spot
     * @return
     */
    public static boolean calcSensorContact(Game game, Entity ae, Targetable target) {
        // NPE check. Fighter squadrons don't start with sensors, but pick them up from the component fighters each round
        if (ae.getActiveSensor() == null) {
            return false;
        }
        Coords targetPos = target.getPosition();
        int distance = ae.getPosition().distance(targetPos);
        int roll = Compute.d6(2);
        int tn = ae.getCrew().getPiloting();
        int maxSensorRange = ae.getActiveSensor().getRangeByBracket();
        int rangeIncrement = (int) Math.ceil(maxSensorRange / 10.0);

        // A bit of a hack here. "Aero Sensors" return the ground range, because Sensor doesn't know about Game or Entity
        // to do otherwise. We need to use the space range instead.
        if (ae.getActiveSensor().getType() == Sensor.TYPE_AERO_SENSOR) {
            maxSensorRange = Sensor.ASF_RADAR_MAX_RANGE;
            rangeIncrement = Sensor.ASF_RADAR_AUTOSPOT_RANGE;
        }

        if (ae instanceof Aero) {
            Aero aero = (Aero) ae;
            //Account for sensor damage
            if (aero.isAeroSensorDestroyed()) {
                return false;
            } else {
                tn += aero.getSensorHits();
            }
        }

        //Apply modifiers for attacker's equipment
        //-2 for a working Large NCSS.  Triple the detection range.
        if (ae.hasWorkingMisc(MiscType.F_LARGE_COMM_SCANNER_SUITE)) {
            maxSensorRange *= 3;
            tn -= 2;
        }
        //-1 for a working Small NCSS. Double the detection range.
        if (ae.hasWorkingMisc(MiscType.F_SMALL_COMM_SCANNER_SUITE)) {
            maxSensorRange *= 2;
            tn -= 1;
        }
        //-2 for any type of BAP or EW Equipment. ECM is already accounted for, so don't let the BAP check do that
        if (ae.hasWorkingMisc(MiscType.F_EW_EQUIPMENT)
                || ae.hasBAP(false)) {
            tn -= 2;
        }

        //Military ESM automatically detects anyone using active sensors, which includes all telemissiles
        if (ae.getActiveSensor().getType() == Sensor.TYPE_SPACECRAFT_ESM && target.getTargetType() == Targetable.TYPE_ENTITY) {
            Entity te = (Entity) target;
            if (te.getActiveSensor().getType() == Sensor.TYPE_AERO_SENSOR
                    || te.getActiveSensor().getType() == Sensor.TYPE_SPACECRAFT_RADAR
                    || te instanceof TeleMissile) {
                return true;
            }
            return false;
        }

        //Can't detect anything beyond this distance
        if (distance > maxSensorRange) {
            return false;
        }

        //Apply Sensor Geek SPA, if present
        if (ae.hasAbility(OptionsConstants.UNOFF_SENSOR_GEEK)) {
            tn -= 2;
        }

        //Otherwise, we add +1 to the tn for each 1/10 of the max sensor range (rounded up) between attacker and target
        tn += (distance / rangeIncrement);

        // Apply ECM/ECCM effects
        if (game.getOptions().booleanOption(OptionsConstants.ADVAERORULES_STRATOPS_ECM)) {
            tn += calcSpaceECM(game, ae, target);
        }

        // Apply large craft sensor shadows
        if (game.getOptions().booleanOption(OptionsConstants.ADVAERORULES_STRATOPS_SENSOR_SHADOW)) {
            tn += calcSensorShadow(game, ae, target);
        }

        //Now, determine if we've detected the target this round
        return roll >= tn;
    }

    /**
     * @return visual range in hexes along a specific line of sight
     */
    public static int getVisualRange(Game game, Entity ae, LosEffects los, boolean targetIlluminated) {
        int visualRange = game.getPlanetaryConditions().getVisualRange(ae, targetIlluminated);
        visualRange -= los.getLightSmoke();
        visualRange -= 2 * los.getHeavySmoke();
        visualRange = Math.max(1, visualRange);
        return visualRange;
    }

    /**
     * @return visual range in hexes given current planetary conditions and no los obstruction
     */
    public static int getMaxVisualRange(Entity entity, boolean targetIlluminated ) {
        Game game = entity.getGame();
        if (game == null) {
            return DEFAULT_MAX_VISUAL_RANGE;
        }

        int visualRange;
        if (entity.isSpaceborne() && entity.getGame().getOptions().booleanOption(OptionsConstants.ADVAERORULES_STRATOPS_ADVANCED_SENSORS)) {
            visualRange = 0;
            //For squadrons. Default to the passive thermal/optical value used by component fighters
            if (entity.hasETypeFlag(Entity.ETYPE_FIGHTER_SQUADRON)) {
                visualRange = Sensor.ASF_OPTICAL_FIRING_SOLUTION_RANGE;
            }
            if (entity.getActiveSensor() != null) {
                if (entity.getActiveSensor().getType() == Sensor.TYPE_AERO_SENSOR) {
                    // required because the return on this from the method below is for ground maps
                    visualRange = Sensor.ASF_RADAR_AUTOSPOT_RANGE;
                } else {
                    visualRange = (int) Math.ceil(entity.getActiveSensor().getRangeByBracket() / 10.0);
                }
            }
        } else {
            visualRange =  game.getPlanetaryConditions().getVisualRange(entity, targetIlluminated);
        }
        return visualRange;
    }

    /**
     * Checks to see whether the target is within sensor range (but not
     * necessarily LoS or visual range)
     *
     * @param allECMInfo A collection of ECMInfo for all entities, this value
     *                   can be null and it will be computed when it's
     *                   needed, however passing in the pre-computed
     *                   collection is much faster
     */
    public static boolean inSensorRange(Game game, Entity ae,
            Targetable target, List<ECMInfo> allECMInfo) {
        return inSensorRange(game, null, ae, target, allECMInfo);
    }

    public static boolean inSensorRange(Game game, LosEffects los, Entity ae,
            Targetable target, List<ECMInfo> allECMInfo) {
        // This is not applicable to objects on the same team.
        if (!target.isEnemyOf(ae)) {
            return false;
        }

        //For Space games with this option, return something different
        if (game.getOptions().booleanOption(OptionsConstants.ADVAERORULES_STRATOPS_ADVANCED_SENSORS)
                && target.getTargetType() == Targetable.TYPE_ENTITY
                && game.getBoard().inSpace()) {
            Entity te = (Entity) target;
            return hasSensorContact(ae, te.getId());
        }

        if (!game.getOptions().booleanOption(OptionsConstants.ADVANCED_TACOPS_SENSORS)) {
            return false;
        }

        // if either does not have a position then return false
        if ((ae.getPosition() == null) || (target.getPosition() == null)) {
            return false;
        }

        // If we have no sensors then return false
        if (ae.getActiveSensor() == null) {
            return false;
        }

        int bracket = Compute.getSensorRangeBracket(ae, target, allECMInfo);
        int range = Compute.getSensorRangeByBracket(game, ae, target, los);

        int maxSensorRange = bracket * range;
        int minSensorRange = Math.max((bracket - 1) * range, 0);
        if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_INCLUSIVE_SENSOR_RANGE)) {
            minSensorRange = 0;
        }

        int distance = ae.getPosition().distance(target.getPosition());

        //Aeros have to check visibility to ground targets for the closest point of approach along their flight path
        //Because the rules state "within X hexes of the flight path" we're using ground distance so altitude doesn't screw us up
        if (isAirToGround(ae, target) && (target instanceof Entity)) {
            Entity te = (Entity) target;
            distance = te.getPosition().distance(
                    getClosestFlightPath(te.getId(),
                            te.getPosition(), (Entity) ae));
            return (distance > minSensorRange) && (distance <= maxSensorRange);
        }
        //This didn't work right for Aeros. Should account for the difference in altitude, not just add the target's altitude to distance
        distance += Math.abs(2 * target.getAltitude() - 2 * ae.getAltitude());

        // if this is an air-to-air scan on the ground map, then divide distance by 16 to match weapon ranges
        // I purposely left this calculation out of visual spotting, so we should do some testing with this and
        // see if it's errata-worthy. The idea is that you'll boost sensor range to help find an enemy aero on the map
        // but still won't be able to see it and shoot at it beyond normal visual conditions.
        if (isAirToAir(ae, target) && game.getBoard().onGround()) {
            distance = (int) Math.ceil(distance / 16.0);
        }
        return (distance > minSensorRange) && (distance <= maxSensorRange);
    }

    /**
     * Checks to see if the target is visible to the unit, always considering
     * sensors.
     */
    public static boolean canSee(Game game, Entity ae, Targetable target) {
        return canSee(game, ae, target, true, null, null);
    }

    /**
     * Checks to see if the target is visible to the unit, if the sensor flag
     * is true then sensors are checked as well.
     */
    public static boolean canSee(Game game, Entity ae, Targetable target,
            boolean useSensors, LosEffects los, List<ECMInfo> allECMInfo) {

        if (!ae.getCrew().isActive()) {
            return false;
        }
        if (target.isOffBoard()) {
            return false;
        }

        if (los == null) {
            los = LosEffects.calculateLOS(game, ae, target);
        }
        boolean isVisible = los.canSee() && Compute.inVisualRange(game, los, ae, target);
        if (useSensors) {
            isVisible = isVisible
                    || Compute.inSensorRange(game, los, ae, target, allECMInfo);
        }
        return isVisible;
    }

    /**
     * gets the sensor range bracket when detecting a particular type of target.
     * target may be null here, which gives you the bracket without target
     * entity modifiers
     *
     * @param allECMInfo A collection of ECMInfo for all entities, this value
     *                   can be null and it will be computed when it's
     *                   needed, however passing in the pre-computed
     *                   collection is much faster
     */
    public static int getSensorRangeBracket(Entity ae, Targetable target,
                                            List<ECMInfo> allECMInfo) {

        Sensor sensor = ae.getActiveSensor();
        if (null == sensor) {
            return 0;
        }
        // only works for entities
        Entity te = null;
        if (null != target) {
            if (target.getTargetType() != Targetable.TYPE_ENTITY) {
                return 0;
            }
            te = (Entity) target;
        }

        // if this sensor is an active probe and it is critted, then no can see
        if (sensor.isBAP() && !ae.hasBAP(false)) {
            return 0;
        }

        //In space, sensors don't have brackets, so we should always return the range for bracket 1.
        if (ae.isSpaceborne()) {
            return Compute.getSensorBracket(7);
        }

        int check = ae.getSensorCheck();
        if ((null != ae.getCrew()) && ae.hasAbility(OptionsConstants.UNOFF_SENSOR_GEEK)) {
            check -= 2;
        }
        if (null != te) {
            check += sensor.getModsForStealth(te);
            // Metal Content...
            if (ae.getGame().getOptions().booleanOption(OptionsConstants.ADVANCED_METAL_CONTENT)) {
                check += sensor.getModForMetalContent(ae, te);
            }
        }
        // ECM bubbles
        check += sensor.getModForECM(ae, allECMInfo);

        return Compute.getSensorBracket(check);
    }

    /**
     * returns the brackets for sensor checks
     */
    public static int getSensorBracket(int check) {
        // get the range bracket (0 - none; 1 - short; 2 - medium; 3 - long)
        int bracket = 0;
        if ((check == 7) || (check == 8)) {
            bracket = 1;
        }
        if ((check == 5) || (check == 6)) {
            bracket = 2;
        }
        if (check < 5) {
            bracket = 3;
        }
        return bracket;
    }

    /**
     * gets the size of the sensor range bracket when detecting a particular
     * type of target. target may be null here, which gives you the range
     * without target entity modifiers
     */
    public static int getSensorRangeByBracket(Game game, Entity ae, @Nullable Targetable target,
                                              @Nullable LosEffects los) {
        if (los == null) {
            los = LosEffects.calculateLOS(game, ae, target);
        }

        Sensor sensor = ae.getActiveSensor();
        if (null == sensor) {
            return 0;
        }
        // only works for entities
        Entity te = null;
        if (null != target) {
            if (target.getTargetType() != Targetable.TYPE_ENTITY) {
                return 0;
            }
            te = (Entity) target;
        }

        // if this sensor is an active probe and it is critted, then no can see
        if (sensor.isBAP() && !ae.hasBAP(false)) {
            return 0;
        }

        // if we are crossing water then only magscan will work unless we are a
        // naval vessel
        if ((null != te) && los.isBlockedByWater()
            && (sensor.getType() != Sensor.TYPE_MEK_MAGSCAN)
            && (sensor.getType() != Sensor.TYPE_VEE_MAGSCAN)
            && (ae.getMovementMode() != EntityMovementMode.HYDROFOIL)
            && (ae.getMovementMode() != EntityMovementMode.NAVAL)) {
            return 0;
        }

        // now get the range
        int range = sensor.getRangeByBracket();

        // adjust the range based on LOS and planetary conditions
        range = sensor.adjustRange(range, game, los);

        //If we're an airborne aero, sensor range is limited to within a few hexes of the flightline against ground targets
        //TO Dec 2017 Errata p17
        if (te != null && ae.isAirborne() && !te.isAirborne()) {
            //Can't see anything if above Alt 8.
            if (ae.getAltitude() > 8) {
                range = 0;
            } else if (sensor.isBAP()) {
            //Add 1 to range for active probe of any type
                range = 2;
            } else {
            //Basic sensor range listed in errata
                range = 1;
            }
            return range;
        }

        // now adjust for anything about the target entity (size, heat, etc)
        if (null != te) {
            range = sensor.entityAdjustments(range, te, game);
        }

        if (range < 0) {
            range = 0;
        }

        return range;

    }

    public static int getADARangeModifier(int distance) {
        // +0 for same ground map / Low-Altitude hex
        // +2 for 1 LAH away
        // +4 for 2 LAH away
        if (distance <= 0){
            return 0;
        }
        return (((distance - 1) / Board.DEFAULT_BOARD_HEIGHT) * 2);

    }

    public static final class SensorRangeHelper {
        public int minSensorRange;
        public int maxSensorRange;
        public int minGroundSensorRange;
        public int maxGroundSensorRange;

        public SensorRangeHelper(int minSensorRange, int maxSensorRange, int minGroundSensorRange, int maxGroundSensorRange) {
            this.minSensorRange = minSensorRange;
            this.maxSensorRange = maxSensorRange;
            this.minGroundSensorRange = minGroundSensorRange;
            this.maxGroundSensorRange = maxGroundSensorRange;
        }
    }

    /**
     * returns the current sensing ranges of the active sensor
     */
    @Nullable
    public static SensorRangeHelper getSensorRanges(Game game, Entity e) {
        if (null == e.getActiveSensor()) {
            return null;
        }

        int check = e.getSensorCheck();
        if ((null != e.getCrew()) && e.hasAbility(OptionsConstants.UNOFF_SENSOR_GEEK)) {
            check -= 2;
        }

        int bracket = Compute.getSensorBracket(check);
        if (e.isSpaceborne()) {
            bracket = Compute.getSensorBracket(7);
        }
        int range = e.getActiveSensor().getRangeByBracket();
        int groundRange = 0;
        if (e.getActiveSensor().isBAP()) {
            groundRange = 2;
        } else {
            groundRange = 1;
        }

        //ASF sensors change range when in space, so we do that here
        if (e.isSpaceborne()) {
            if (e.getActiveSensor().getType() == Sensor.TYPE_AERO_SENSOR) {
                range = Sensor.ASF_RADAR_MAX_RANGE;
            }

            //If Aero/Spacecraft sensors are destroyed while in space, the range is 0.
            if (e.isAeroSensorDestroyed()) {
                range = 0;
            }
        }

        //Dropships using radar in an atmosphere need a range that's a bit more sensible
        if (e.hasETypeFlag(Entity.ETYPE_DROPSHIP) && !e.isSpaceborne()) {
            if (e.getActiveSensor().getType() == Sensor.TYPE_SPACECRAFT_RADAR) {
                range = Sensor.LC_RADAR_GROUND_RANGE;
            }
        }

        int maxSensorRange = bracket * range;
        int minSensorRange = Math.max((bracket - 1) * range, 0);
        int maxGroundSensorRange = bracket * groundRange;
        int minGroundSensorRange = Math.max((maxGroundSensorRange - 1), 0);

        if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_INCLUSIVE_SENSOR_RANGE)) {
            minSensorRange = 0;
            minGroundSensorRange = 0;
        }

        return new SensorRangeHelper(minSensorRange, maxSensorRange, minGroundSensorRange, maxGroundSensorRange);
    }

    public static int targetSideTable(Coords inPosition, Targetable target) {
        return target.sideTable(inPosition);
    }

    public static int targetSideTable(Entity attacker, Targetable target) {
        return Compute
                .targetSideTable(attacker, target, CalledShot.CALLED_NONE);
    }

    public static int targetSideTable(Entity attacker, Targetable target,
                                      int called) {
        Coords attackPos = attacker.getPosition();

        Entity te = null;
        if (target instanceof Entity) {
            te = (Entity) target;
        }

        boolean usePrior = false;
        // aeros in the same hex need to adjust position to get side
        // table
        if (isAirToAir(attacker, target)
            && attackPos.equals(target.getPosition())
            && attacker.isAero() && target.isAero()) {
            int moveSort = shouldMoveBackHex(attacker, (Entity) target);
            if (moveSort < 0) {
                attackPos = attacker.getPriorPosition();
            }
            usePrior = moveSort > 0;
        }

        // if this is a air to ground attack, then attacker position is given by
        // the direction from which they entered the target hex
        if (isAirToGround(attacker, target)) {
            attackPos = attacker.passedThroughPrevious(target.getPosition());
        }

        if (isGroundToAir(attacker, target) && (null != te)) {
            int facing = Compute.getClosestFlightPathFacing(attacker.getId(),
                    attackPos, te);
            Coords pos = Compute.getClosestFlightPath(attacker.getId(),
                    attackPos, te);
            return te.sideTable(attackPos, usePrior, facing, pos);
        }

        if ((null != te) && (called == CalledShot.CALLED_LEFT)) {
            return te.sideTable(attackPos, usePrior, (te.getFacing() + 5) % 6);
        } else if ((null != te) && (called == CalledShot.CALLED_RIGHT)) {
            return te.sideTable(attackPos, usePrior, (te.getFacing() + 1) % 6);
        }

        return target.sideTable(attackPos, usePrior);
    }


        /**
         * Compares the initiative of two aerospace units in the same hex to determine attack angle.
         * The attack angle is computed as if the unit with the higher initiative were in its previous hex.
         *
         * @param e1 The first <code>Entity</code> to compare
         * @param e2 The second <code>Entity</code> to compare
         * @return &lt; 0 if the first unit has a higher initiative, &gt; 0 if the second is higher,
         *         or 0 if one of the units is not an aerospace unit, does not have a valid position,
         *         or the two units are not in the same hex.
         */
    public static int shouldMoveBackHex(Entity e1, Entity e2) {
        if (null == e1.getPosition() || null == e2.getPosition()
                || !e1.getPosition().equals(e2.getPosition())
                || !e1.isAero() || !e2.isAero()) {
            return 0;
        }

        int retVal = e1.getUnitType() - e2.getUnitType();
        if (retVal == 0) {
            retVal = ((IAero) e2).getCurrentVelocity() - ((IAero) e1).getCurrentVelocity();
        }
        // if all criteria are the same, select randomly
        if (retVal == 0) {
            retVal = d6() < 4? -1 : 1;
        }
        return retVal;
    }

    /**
     * Maintain backwards compatibility.
     *
     * @param missiles - the <code>int</code> number of missiles in the pack.
     */
    public static int missilesHit(int missiles) {
        return Compute.missilesHit(missiles, 0);
    }

    /**
     * Maintain backwards compatability.
     *
     * @param missiles
     * @param nMod
     * @return
     */
    public static int missilesHit(int missiles, int nMod) {
        return Compute.missilesHit(missiles, nMod, false);
    }

    /**
     * Maintain backwards compatability.
     *
     * @param missiles
     * @param nMod
     * @param hotloaded
     * @return
     */
    public static int missilesHit(int missiles, int nMod, boolean hotloaded) {
        return Compute.missilesHit(missiles, nMod, hotloaded, false, false);
    }

    /**
     * Roll the number of missiles (or whatever) on the missile hit table, with
     * the specified mod to the roll.
     *
     * @param missiles    - the <code>int</code> number of missiles in the pack.
     * @param nMod        - the <code>int</code> modifier to the roll for number of
     *                    missiles that hit.
     * @param hotloaded   - roll 3d6 take worst 2
     * @param streak      - force a roll of 11 on the cluster table
     * @param advancedAMS - the roll can now go below 2, indicating no damage
     */
    public static int missilesHit(int missiles, int nMod, boolean hotloaded,
                                  boolean streak, boolean advancedAMS) {
        int nRoll = Compute.d6(2);

        if (hotloaded) {
            int roll1 = Compute.d6();
            int roll2 = Compute.d6();
            int roll3 = Compute.d6();
            int lowRoll1 = 0;
            int lowRoll2 = 0;

            if ((roll1 <= roll2) && (roll1 <= roll3)) {
                lowRoll1 = roll1;
                lowRoll2 = Math.min(roll2, roll3);
            } else if ((roll2 <= roll1) && (roll2 <= roll3)) {
                lowRoll1 = roll2;
                lowRoll2 = Math.min(roll1, roll3);
            } else if ((roll3 <= roll1) && (roll3 <= roll2)) {
                lowRoll1 = roll3;
                lowRoll2 = Math.min(roll2, roll1);
            }
            nRoll = lowRoll1 + lowRoll2;
        }
        if (streak) {
            nRoll = 11;
        }
        nRoll += nMod;
        if (!advancedAMS) {
            nRoll = Math.min(Math.max(nRoll, 2), 12);
        } else {
            nRoll = Math.min(nRoll, 12);
        }
        if (nRoll < 2) {
            return 0;
        }

        for (int[] element : clusterHitsTable) {
            if (element[0] == missiles) {
                return element[nRoll - 1];
            }
        }
        // BA missiles may have larger number of missiles than max entry on the
        // table
        // if so, take largest, subtract value and try again
        for (int i = clusterHitsTable.length - 1; i >= 0; i--) {
            if (missiles > clusterHitsTable[i][0]) {
                return clusterHitsTable[i][nRoll - 1]
                       + Compute.missilesHit(
                        missiles - clusterHitsTable[i][0], nMod,
                        hotloaded, streak, advancedAMS);
            }
        }
        throw new RuntimeException(
                "Could not find number of missiles in hit table");
    }

    public static int calculateClusterHitTableAmount(int roll, int rackSize) {
        for (int[] element : clusterHitsTable) {
            if (element[0] == rackSize) {
                return element[roll - 1];
            }
        }

        return 0;
    }

    /**
     * Returns the consciousness roll number
     *
     * @param hit - the <code>int</code> number of the crew hit currently being
     *            rolled.
     * @return The <code>int</code> number that must be rolled on 2d6 for the
     * crew to stay conscious.
     */
    public static int getConsciousnessNumber(int hit) {
        switch (hit) {
            case 0:
                return 2;
            case 1:
                return 3;
            case 2:
                return 5;
            case 3:
                return 7;
            case 4:
                return 10;
            case 5:
                return 11;
            default:
                return Integer.MAX_VALUE;
        }
    }

    /**
     * Check for ferrous metal content in terrain on path from a to b return the
     * total content.
     */
    public static int getMetalInPath(Entity ae, Coords a, Coords b) {
        // If we're in space, or anything is null... get out.
        if ((ae == null) || (a == null) || (b == null)) {
            return 0;
        }
        Board board = ae.getGame().getBoard();
        if (board.inSpace()) {
            return 0;
        }

        if (!board.contains(a) || !board.contains(b)) {
            return 0;
        }

        // get intervening Coords.
        ArrayList<Coords> coords = Coords.intervening(a, b);
        // loop through all intervening coords, check each if they are ECM
        // affected
        int metalContent = 0;
        for (Coords c : coords) {
            Hex hex = board.getHex(c);
            if (hex != null && hex.containsTerrain(Terrains.METAL_CONTENT)) {
                metalContent += hex.terrainLevel(Terrains.METAL_CONTENT);
            }
        }
        return metalContent;
    }

    /**
     * Check for ECM bubbles in Ghost Target mode along the path from a to b and
     * return the highest target roll. -1 if no Ghost Targets
     */
    public static int getGhostTargetNumber(Entity ae, Coords a, Coords b) {
        if (ae.getGame().getBoard().inSpace()) {
            // ghost targets don't work in space
            return 0;
        }
        if ((a == null) || (b == null)) {
            return 0;
        }

        // Only grab enemies with active ECM
        // need to create two hashtables for ghost targeting, one with mods
        // and one with booleans indicating that this ghost target was
        // intersected
        // the keys will be the entity id
        Hashtable<Integer, Boolean> hEnemyGTCrossed = new Hashtable<>();
        Hashtable<Integer, Integer> hEnemyGTMods = new Hashtable<>();
        Vector<Coords> vEnemyECCMCoords = new Vector<>(16);
        Vector<Integer> vEnemyECCMRanges = new Vector<>(16);
        Vector<Double> vEnemyECCMStrengths = new Vector<>(16);
        Vector<Coords> vEnemyGTCoords = new Vector<>(16);
        Vector<Integer> vEnemyGTRanges = new Vector<>(16);
        Vector<Integer> vEnemyGTId = new Vector<>(16);
        Vector<Coords> vFriendlyECMCoords = new Vector<>(16);
        Vector<Integer> vFriendlyECMRanges = new Vector<>(16);
        Vector<Double> vFriendlyECMStrengths = new Vector<>(16);
        for (Entity ent : ae.getGame().getEntitiesVector()) {
            Coords entPos = ent.getPosition();
            if (ent.isEnemyOf(ae) && ent.hasGhostTargets(true)
                && (entPos != null)) {
                vEnemyGTCoords.addElement(entPos);
                vEnemyGTRanges.addElement(ent.getECMRange());
                vEnemyGTId.addElement(ent.getId());
                hEnemyGTCrossed.put(ent.getId(), false);
                hEnemyGTMods.put(ent.getId(), ent.getGhostTargetRollMoS());
            }
            if (ent.isEnemyOf(ae) && ent.hasActiveECCM() && (entPos != null)) {
                vEnemyECCMCoords.addElement(entPos);
                vEnemyECCMRanges.addElement(ent.getECMRange());
                vEnemyECCMStrengths.add(ent.getECCMStrength());
            }
            if (!ent.isEnemyOf(ae) && ent.hasActiveECM() && (entPos != null)) {
                vFriendlyECMCoords.addElement(entPos);
                vFriendlyECMRanges.addElement(ent.getECMRange());
                vFriendlyECMStrengths.add(ent.getECMStrength());
            }

            // Check the ECM effects of the entity's passengers.
            for (Entity other : ent.getLoadedUnits()) {
                if (other.isEnemyOf(ae) && other.hasGhostTargets(true)
                    && (entPos != null)) {
                    vEnemyGTCoords.addElement(entPos);
                    vEnemyGTRanges.addElement(other.getECMRange());
                    vEnemyGTId.addElement(ent.getId());
                    hEnemyGTCrossed.put(ent.getId(), false);
                    hEnemyGTMods.put(ent.getId(), ent.getGhostTargetRollMoS());
                }
                if (other.isEnemyOf(ae) && other.hasActiveECCM()
                    && (entPos != null)) {
                    vEnemyECCMCoords.addElement(entPos);
                    vEnemyECCMRanges
                            .addElement(other.getECMRange());
                    vEnemyECCMStrengths.add(ent.getECCMStrength());
                }
                if (!other.isEnemyOf(ae) && ent.hasActiveECM()
                    && (entPos != null)) {
                    vFriendlyECMCoords.addElement(entPos);
                    vFriendlyECMRanges
                            .addElement(ent.getECMRange());
                    vFriendlyECMStrengths.add(ent.getECMStrength());
                }
            }
        }

        // none? get out of here
        if (vEnemyGTCoords.isEmpty()) {
            return -1;
        }

        // get intervening Coords.
        ArrayList<Coords> coords = Coords.intervening(a, b);
        // loop through all intervening coords, if they are not ecm'ed by
        // friendlys then add any Ghost Targets
        // to the hashlist
        // According to the rules clarification below ECM cancels Ghost Targets
        // http://www.classicbattletech.com/forums/index.php/topic,66035.new.html#new
        for (Coords c : coords) {
            // >0: in friendly ECM
            // <=0: not in friendly ECM
            int ecmStatus = 0;
            // first, add 1 for each friendly ECM that affects us
            Enumeration<Integer> ranges = vFriendlyECMRanges.elements();
            Enumeration<Double> strengths = vFriendlyECMStrengths.elements();
            for (Coords friendlyECMCoords : vFriendlyECMCoords) {
                int range = ranges.nextElement();
                int nDist = c.distance(friendlyECMCoords);
                double strength = strengths.nextElement();
                if (nDist <= range) {
                    ecmStatus += (int) Math.round(strength);
                }
            }
            // now, subtract one for each enemy ECCM
            ranges = vEnemyECCMRanges.elements();
            strengths = vEnemyECCMStrengths.elements();
            for (Coords enemyECCMCoords : vEnemyECCMCoords) {
                int range = ranges.nextElement();
                int nDist = c.distance(enemyECCMCoords);
                double strength = strengths.nextElement();
                if (nDist <= range) {
                    ecmStatus -= strength;
                }
            }

            if (ecmStatus < 1) {
                // find any new Ghost Targets that we have crossed
                ranges = vEnemyGTRanges.elements();
                Enumeration<Integer> ids = vEnemyGTId.elements();
                for (Coords enemyGTCoords : vEnemyGTCoords) {
                    int range = ranges.nextElement();
                    int id = ids.nextElement();
                    int nDist = c.distance(enemyGTCoords);
                    if ((nDist <= range) && !hEnemyGTCrossed.get(id)) {
                        hEnemyGTCrossed.put(id, true);
                    }
                }
            }
        }

        // ok so now we have a hashtable that tells us which Ghost Targets have
        // been crossed
        // lets loop through that and identify the highest bonus and count the
        // total number crossed
        int totalGT = -1;
        int highestMod = -1;
        Enumeration<Integer> ids = hEnemyGTCrossed.keys();
        while (ids.hasMoreElements()) {
            int id = ids.nextElement();
            if (hEnemyGTCrossed.get(id)) {
                totalGT++;
                if (hEnemyGTMods.get(id) > highestMod) {
                    highestMod = hEnemyGTMods.get(id);
                }
            }
        }

        // according to the following rules clarification, this should be maxed
        // out at +4
        // http://www.classicbattletech.com/forums/index.php?topic=66036.0
        return Math.min(4, highestMod + totalGT);
    }

    /**
     * Get the base to-hit number of a space bomb attack by the given attacker
     * upon the given defender
     *
     * @param attacker - the <code>Entity</code> conducting the leg attack.
     * @param defender - the <code>Entity</code> being attacked.
     * @return The base <code>ToHitData</code> of the attack.
     */
    public static ToHitData getSpaceBombBaseToHit(Entity attacker,
                                                  Entity defender, Game game) {
        int base = TargetRoll.IMPOSSIBLE;
        StringBuffer reason = new StringBuffer();

        if (!attacker.isAero()) {
            return new ToHitData(base, "attacker is not an Aero");
        }

        IAero a = (IAero) attacker;

        // the fighters nose must be aligned with its direction of travel
        boolean rightFacing = false;
        // using normal movement, I think this means that the last move can't be
        // a turn
        if (!game.useVectorMove()) {
            rightFacing = true;
        }
        // for advanced movement, it must be aligned with largest vector
        if (game.useVectorMove()) {
            for (int h : attacker.getHeading()) {
                if (h == attacker.facing) {
                    rightFacing = true;
                    break;
                }
            }
        }

        boolean canTarget = false;
        Coords attackCoords = null;
        for (Coords c : attacker.getPassedThrough()) {
            for (Entity target : game.getEntitiesVector(c)) {
                if (target.getId() == defender.getId()) {
                    canTarget = true;
                }
            }
            if (canTarget) {
                break;
            }
            attackCoords = c;
        }
        if (null == attackCoords) {
            attackCoords = attacker.getPosition();
        }

        // must be in control
        if (a.isOutControlTotal()) {
            reason.append("the attacker is out of control");
        } else if (attacker.getBombs(AmmoType.F_SPACE_BOMB).size() < 1) {
            reason.append("the attacker has no useable bombs");
        } else if (!rightFacing) {
            reason.append("the attacker is not facing the direction of travel");
        }
        // attacker and defender must both be in space hex
        else if (!game.getBoard().getHex(attacker.getPosition())
                      .containsTerrain(Terrains.SPACE)) {
            reason.append("attacker not in space hex");
        } else if (!game.getBoard().getHex(defender.getPosition())
                        .containsTerrain(Terrains.SPACE)) {
            reason.append("defender not in space hex");
        } else if (!canTarget) {
            reason.append("defender is not in hex passed through by attacker this turn");
        }
        // the defender must weight 10000+ tons
        else if (defender.weight < 10000) {
            reason.append("the defender weighs less than 10,000 tons");
        }

        // ok if we are still alive then lets calculate the tohit
        else {
            base = attacker.getCrew().getGunnery();
            reason.append("base");
        }

        ToHitData toHit = new ToHitData(base, reason.toString(),
                                        ToHitData.HIT_NORMAL, defender.sideTable(attackCoords));

        toHit.addModifier(+4, "space bomb attack");
        if (attacker.mpUsed > 0) {
            toHit.addModifier(attacker.mpUsed, "attacker thrust");
        }
        if (defender.mpUsed > 0) {
            toHit.addModifier(defender.mpUsed, "defender thrust");
        }
        if ((defender instanceof SpaceStation) || (defender.getWalkMP() == 0) || (defender.braceLocation() != Entity.LOC_NONE)) {
            toHit.addModifier(-4, "immobile");
        }
        if (defender.weight < 100000) {
            int penalty = (int) Math.ceil((100000 - defender.weight) / 10000);
            toHit.addModifier(penalty, "defender weight");
        }

        return toHit;
    }

    /**
     * This assembles attack roll modifiers for infantry swarm and leg attacks.
     */
    private static ToHitData getAntiMechMods(ToHitData data, Infantry attacker,
                                             Entity defender) {
        if (attacker == null) {
            data.addModifier(TargetRoll.IMPOSSIBLE, "Unknown attacker");
            return data;
        }
        if (defender == null) {
            data.addModifier(TargetRoll.IMPOSSIBLE, "Unknown defender");
            return data;
        }

        if (attacker instanceof BattleArmor) {
            // Battle Armor units can't do an AM Attack if they're burdened.
            if (((BattleArmor) attacker).isBurdened()) {
                data.addModifier(TargetRoll.IMPOSSIBLE,
                                 "Launcher not jettisoned.");
                return data;
            }
            // BA units that jumped using mechanical jump boosters can't attack
            if (attacker.hasWorkingMisc(MiscType.F_MECHANICAL_JUMP_BOOSTER)
                // we used a mechanical jump booster for jumping only if we
                // don't have normal JJs, or if we are underwater-capable
                // because we underwatercapable BAs can only jump via
                // mechanical jump boosters
                // otherwise, normal JJs give the same MP and do not have
                // this restriction
                && ((attacker.getOriginalJumpMP() == 0) || (attacker
                                                                    .getMovementMode() == EntityMovementMode.INF_UMU))
                && (attacker.moved == EntityMovementType.MOVE_JUMP)) {
                data.addModifier(
                        TargetRoll.IMPOSSIBLE,
                        "can't jump using mechanical jump booster and anti-mech attack in the same turn");
                return data;
            }
        } else {
            // Infantry can't have encumbering armor
            if (attacker.isArmorEncumbering()) {
                data.addModifier(TargetRoll.IMPOSSIBLE,
                                 "can't engage in anti-mek attacks with encumbering armor.");
                return data;
            }
        }

        // Can't target a transported entity.
        if (Entity.NONE != defender.getTransportId()) {
            data.addModifier(TargetRoll.IMPOSSIBLE, "Target is a passenger.");
            return data;
        }

        if (defender.isMakingDfa()) {
            data.addModifier(TargetRoll.IMPOSSIBLE, "Target is making a DFA.");
            return data;
        }

        // Already conducting a swarm attack.
        if (Entity.NONE != attacker.getSwarmTargetId()) {
            data.addModifier(TargetRoll.IMPOSSIBLE,
                             "Attacker is currently swarming.");
            return data;
        }

        if (defender.isAirborneVTOLorWIGE()) {
            data.addModifier(TargetRoll.IMPOSSIBLE, "Cannot target airborne unit.");
            return data;
        }

        if ((defender instanceof Mech) && ((Mech) defender).isIndustrial()) {
            data.addModifier(-1, "targeting industrial mech");
        }

        // protected/exposed actuator quirk may adjust target roll
        if (defender.hasQuirk(OptionsConstants.QUIRK_POS_PRO_ACTUATOR)) {
            data.addModifier(+1, "protected actuators");
        }
        if (defender.hasQuirk(OptionsConstants.QUIRK_NEG_EXP_ACTUATOR)) {
            data.addModifier(-1, "exposed actuators");
        }

        // MD Infantry with grappler/magnets get bonus
        if (attacker.hasAbility(OptionsConstants.MD_PL_ENHANCED)) {
            data.addModifier(-2, "MD Grapple/Magnet");
        }

        // swarm/leg attacks take target movement mods into account
        data.append(getTargetMovementModifier(attacker.getGame(), defender.getId()));

        return data;
    }

    /**
     * Get the base to-hit number of a Leg Attack by the given attacker upon the
     * given defender
     *
     * @param attacker - the <code>Entity</code> conducting the leg attack.
     * @param defender - the <code>Entity</code> being attacked.
     * @return The base <code>ToHitData</code> of the attack.
     */
    public static ToHitData getLegAttackBaseToHit(Entity attacker,
            Entity defender, Game game) {
        String reason = "Non Infantry not allowed to do AM attacks.";
        ToHitData toReturn = null;
        boolean alreadyPerformingOther = false;
        for (Enumeration<EntityAction> actions = game.getActions(); actions.hasMoreElements(); ) {
            EntityAction ea = actions.nextElement();
            if (ea instanceof WeaponAttackAction) {
                WeaponAttackAction waa = (WeaponAttackAction) ea;
                Entity waaAE = waa.getEntity(game);
                if ((waaAE != null) && waaAE.equals(attacker)) {
                    // impossible if already doing a swarm attack
                    if (waa.getEntity(game).getEquipment(waa.getWeaponId())
                           .getType().getInternalName()
                           .equals(Infantry.SWARM_MEK)) {
                        alreadyPerformingOther = true;

                    }
                }
            }
        }

        if (alreadyPerformingOther) {
            reason = "already performing a swarm attack";
        }
        // Can only attack a Mek's legs.
        else if (!(defender instanceof Mech)) {
            reason = "Defender is not a Mech.";
        }

        // Can't attack if flying
        else if (attacker.getElevation() > defender.getElevation()) {
            reason = "Cannot do leg attack while flying.";
        }

        // Handle BattleArmor attackers.
        else if (attacker instanceof BattleArmor) {
            BattleArmor inf = (BattleArmor) attacker;
            toReturn = new ToHitData(inf.getCrew().getPiloting(),
                    "anti-mech skill", ToHitData.HIT_KICK, ToHitData.SIDE_FRONT);
            int men = inf.getShootingStrength();
            int modifier = TargetRoll.IMPOSSIBLE;
            if (men >= 4) {
                modifier = 0;
            } else if (men >= 3) {
                modifier = 2;
            } else if (men >= 2) {
                modifier = 5;
            } else if (men >= 1) {
                modifier = 7;
            }
            toReturn.addModifier(modifier, men + " trooper(s) active");
        } else if (attacker instanceof Infantry) {
            // Non-BattleArmor infantry need many more men.
            Infantry inf = (Infantry) attacker;
            toReturn = new ToHitData(inf.getCrew().getPiloting(),
                    "anti-mech skill", ToHitData.HIT_KICK, ToHitData.SIDE_FRONT);
            int men = inf.getShootingStrength();
            int modifier = TargetRoll.IMPOSSIBLE;
            if (men >= 22) {
                modifier = 0;
            } else if (men >= 16) {
                modifier = 2;
            } else if (men >= 10) {
                modifier = 5;
            } else if (men >= 5) {
                modifier = 7;
            }
            toReturn.addModifier(modifier, men + " trooper(s) active");
        }

        if (defender instanceof Mech && ((Mech) defender).hasTracks()) {
            toReturn.addModifier(-2, "has tracks");
        }

        // If the swarm is impossible, ToHitData wasn't created
        if (toReturn == null) {
            toReturn = new ToHitData(TargetRoll.IMPOSSIBLE, reason.toString(),
                    ToHitData.HIT_KICK, ToHitData.SIDE_FRONT);
        }
        if (toReturn.getValue() == TargetRoll.IMPOSSIBLE) {
            return toReturn;
        }
        toReturn = Compute.getAntiMechMods(toReturn, (Infantry) attacker,
                defender);
        return toReturn;
    }

    /**
     * Get the base to-hit number of a Swarm Mek by the given attacker upon the
     * given defender.
     *
     * @param attacker - the <code>Entity</code> swarming.
     * @param defender - the <code>Entity</code> being swarmed.
     * @return The base <code>ToHitData</code> of the mek.
     */
    public static ToHitData getSwarmMekBaseToHit(Entity attacker,
            Entity defender, Game game) {
        ToHitData toReturn = null;
        String reason = "Non Infantry not allowed to do AM attacks.";

        boolean alreadyPerformingOther = false;
        for (Enumeration<EntityAction> actions = game.getActions(); actions.hasMoreElements(); ) {
            EntityAction ea = actions.nextElement();
            if (ea instanceof WeaponAttackAction) {
                WeaponAttackAction waa = (WeaponAttackAction) ea;
                Entity waaAE = waa.getEntity(game);
                if ((waaAE != null) && waaAE.equals(attacker)) {
                    // impossible if already doing a swarm attack
                    if (waa.getEntity(game).getEquipment(waa.getWeaponId())
                           .getType().getInternalName()
                           .equals(Infantry.LEG_ATTACK)) {
                        alreadyPerformingOther = true;

                    }
                }
            }
        }
        if (alreadyPerformingOther) {
            reason = "attacker is already performing a leg attack";
        }
        // Can only swarm a Mek.
        else if (!(defender instanceof Mech) && !(defender instanceof Tank)) {
            reason = "Defender is not a Mech or vehicle.";
        }
        // Can't swarm a friendly Mek. See
        // http://www.classicbattletech.com/w3t/showflat
        // .php?Cat=&Board=ask&Number=632321&page=0&view=collapsed&sb=5&o=0&fpart=
        else if (!attacker.isEnemyOf(defender)
                 && !attacker.getGame().getOptions()
                             .booleanOption(OptionsConstants.BASE_FRIENDLY_FIRE)) {
            reason = "Can only swarm an enemy.";
        }
        // target is already swarmed
        else if (defender.getSwarmAttackerId() != Entity.NONE) {
            reason = "Only one swarm allowed at a time.";
        }
        // Handle BattleArmor attackers.
        else if (attacker instanceof BattleArmor) {
            BattleArmor inf = (BattleArmor) attacker;
            toReturn = new ToHitData(inf.getCrew().getPiloting(), "anti-mech skill");
            int men = inf.getShootingStrength();
            int modifier = TargetRoll.IMPOSSIBLE;
            if (men >= 4) {
                modifier = 2;
            } else if (men >= 1) {
                modifier = 5;
            }
            toReturn.addModifier(modifier, men + " trooper(s) active");
        }
        // Non-BattleArmor infantry need many more men.
        else if (attacker instanceof Infantry) {
            Infantry inf = (Infantry) attacker;
            toReturn = new ToHitData(inf.getCrew().getPiloting(), "anti-mech skill");
            int men = inf.getShootingStrength();
            int modifier = TargetRoll.IMPOSSIBLE;
            if (men >= 22) {
                modifier = 2;
            } else if (men >= 16) {
                modifier = 5;
            }
            toReturn.addModifier(modifier, men + " trooper(s) active");
        }
        // If the swarm is impossible, ToHitData wasn't created
        if (toReturn == null) {
            toReturn = new ToHitData(TargetRoll.IMPOSSIBLE, reason.toString());
        }
        if (toReturn.getValue() == TargetRoll.IMPOSSIBLE) {
            return toReturn;
        }
        toReturn = Compute.getAntiMechMods(toReturn, (Infantry) attacker,
                                           defender);

        // If the attacker has assault claws, give a -1 modifier.
        // We can stop looking when we find our first match.
        for (Mounted mount : attacker.getMisc()) {
            EquipmentType equip = mount.getType();
            if (equip.hasFlag(MiscType.F_MAGNET_CLAW)) {
                toReturn.addModifier(-1, "attacker has magnetic claws");
                break;
            }
        }
        return toReturn;
    }

    public static boolean canPhysicalTarget(Game game, int entityId,
                                            Targetable target) {

        if (PunchAttackAction.toHit(game, entityId, target,
                                    PunchAttackAction.LEFT, false).getValue() != TargetRoll.IMPOSSIBLE) {
            return true;
        }

        if (PunchAttackAction.toHit(game, entityId, target,
                                    PunchAttackAction.RIGHT, false).getValue() != TargetRoll.IMPOSSIBLE) {
            return true;
        }

        if (KickAttackAction.toHit(game, entityId, target,
                                   KickAttackAction.LEFT).getValue() != TargetRoll.IMPOSSIBLE) {
            return true;
        }

        if (KickAttackAction.toHit(game, entityId, target,
                                   KickAttackAction.RIGHT).getValue() != TargetRoll.IMPOSSIBLE) {
            return true;
        }

        if ((game.getEntity(entityId) instanceof QuadMech)
            && ((KickAttackAction.toHit(game, entityId, target,
                                        KickAttackAction.LEFTMULE).getValue() != TargetRoll.IMPOSSIBLE) ||
                (KickAttackAction
                         .toHit(game, entityId, target,
                                KickAttackAction.RIGHTMULE).getValue() != TargetRoll.IMPOSSIBLE))) {
            return true;
        }

        if (BrushOffAttackAction.toHit(game, entityId, target,
                                       BrushOffAttackAction.LEFT).getValue() != TargetRoll.IMPOSSIBLE) {
            return true;
        }

        if (BrushOffAttackAction.toHit(game, entityId, target,
                                       BrushOffAttackAction.RIGHT).getValue() != TargetRoll.IMPOSSIBLE) {
            return true;
        }

        if (new ThrashAttackAction(entityId, target).toHit(game).getValue() != TargetRoll.IMPOSSIBLE) {
            return true;
        }

        if (ProtomechPhysicalAttackAction.toHit(game, entityId, target)
                                         .getValue() != TargetRoll.IMPOSSIBLE) {
            return true;
        }

        if (PushAttackAction.toHit(game, entityId, target).getValue() != TargetRoll.IMPOSSIBLE) {
            return true;
        }

        if (LayExplosivesAttackAction.toHit(game, entityId, target).getValue() != TargetRoll.IMPOSSIBLE) {
            return true;
        }

        if (TripAttackAction.toHit(game, entityId, target).getValue() != TargetRoll.IMPOSSIBLE) {
            return true;
        }

        if (GrappleAttackAction.toHit(game, entityId, target).getValue() != TargetRoll.IMPOSSIBLE) {
            return true;
        }

        if (BreakGrappleAttackAction.toHit(game, entityId, target).getValue() != TargetRoll.IMPOSSIBLE) {
            return true;
        }

        for (Mounted club : game.getEntity(entityId).getClubs()) {
            if (null != club) {
                if (ClubAttackAction.toHit(game, entityId, target, club,
                                           ToHitData.HIT_NORMAL, false).getValue() != TargetRoll.IMPOSSIBLE) {
                    return true;
                }
            }
        }

        if (JumpJetAttackAction.toHit(game, entityId, target,
                                      JumpJetAttackAction.BOTH).getValue() != TargetRoll.IMPOSSIBLE) {
            return true;
        }
        if (JumpJetAttackAction.toHit(game, entityId, target,
                                      JumpJetAttackAction.LEFT).getValue() != TargetRoll.IMPOSSIBLE) {
            return true;
        }
        if (JumpJetAttackAction.toHit(game, entityId, target,
                                      JumpJetAttackAction.RIGHT).getValue() != TargetRoll.IMPOSSIBLE) {
            return true;
        }

        if (BAVibroClawAttackAction.toHit(game, entityId, target).getValue() != TargetRoll.IMPOSSIBLE) {
            return true;
        }

        return false;
    }

    /**
     * Can movement between the two coordinates be on pavement (which includes
     * roads and bridges)? If so it will override prohibited terrain, it may
     * change movement costs, and it may lead to skids.
     *
     * @param game The current {@link Game}
     * @param src the <code>Coords</code> being left.
     * @param dest the <code>Coords</code> being entered.
     * @param moveStep
     * @return <code>true</code> if movement between <code>src</code> and
     * <code>dest</code> can be on pavement; <code>false</code> otherwise.
     */
    public static boolean canMoveOnPavement(Game game, Coords src,
            Coords dest, MoveStep moveStep) {
        final Hex srcHex = game.getBoard().getHex(src);
        final Hex destHex = game.getBoard().getHex(dest);
        final int src2destDir = src.direction(dest);
        final int dest2srcDir = (src2destDir + 3) % 6;
        boolean result = false;

        // Jumping shouldn't be considered to be moving on pavement
        if (moveStep.isJumping()) {
            return false;
        }

        // We may be moving in the same hex.
        if (src.equals(dest)
                && (srcHex.containsTerrain(Terrains.PAVEMENT)
                        || srcHex.containsTerrain(Terrains.ROAD) || srcHex
                            .containsTerrain(Terrains.BRIDGE))) {
            result = true;
        }
        // If the source is a pavement hex, then see if the destination
        // hex is also a pavement hex or has a road or bridge that exits
        // into the source hex and the entity is climbing onto the bridge.
        else if (srcHex.containsTerrain(Terrains.PAVEMENT)
                && (destHex.containsTerrain(Terrains.PAVEMENT)
                        || destHex.containsTerrainExit(Terrains.ROAD,
                                dest2srcDir) || (destHex.containsTerrainExit(
                        Terrains.BRIDGE, dest2srcDir) && moveStep.climbMode()))) {
            result = true;
        }
        // See if the source hex has a road or bridge (and the entity is on the
        // bridge) that exits into the destination hex, and the dest hex has
        // pavement or a corresponding exit to the src hex
        else if ((srcHex.containsTerrainExit(Terrains.ROAD, src2destDir) || (srcHex
                .containsTerrainExit(Terrains.BRIDGE, src2destDir) && (moveStep.getElevation() == srcHex
                .terrainLevel(Terrains.BRIDGE_ELEV))))
                && (destHex.containsTerrainExit(Terrains.ROAD, dest2srcDir)
                        || (destHex.containsTerrainExit(Terrains.BRIDGE,
                                dest2srcDir) && moveStep.climbMode()) || destHex
                            .containsTerrain(Terrains.PAVEMENT))) {
            result = true;
        }

        return result;
    }

    /**
     * Determines whether the attacker and the target are in the same building.
     *
     * @return true if the target can and does occupy the same building, false
     *         otherwise.
     */
    public static boolean isInSameBuilding(Game game, Entity attacker,
            Targetable target) {
        if (!(target instanceof Entity)) {
            return false;
        }
        Entity targetEntity = (Entity) target;
        if (!Compute.isInBuilding(game, attacker)
                || !Compute.isInBuilding(game, targetEntity)) {
            return false;
        }

        Building attkBldg = game.getBoard().getBuildingAt(
                attacker.getPosition());
        Building targBldg = game.getBoard().getBuildingAt(target.getPosition());

        return attkBldg.equals(targBldg);
    }

    /**
     * Determine if the given unit is inside of a building at the given coordinates.
     *
     * @param game The current {@link Game}. This value may be <code>null</code>.
     * @param entity the <code>Entity</code> to be checked. This value may be <code>null</code>.
     * @return <code>true</code> if the entity is inside of the building at
     * those coordinates. <code>false</code> if there is no building at
     * those coordinates or if the entity is on the roof or in the air
     * above the building, or if any input argument is <code>null</code>
     */
    public static boolean isInBuilding(@Nullable Game game, @Nullable Entity entity) {
        // No game, no building.
        if (game == null) {
            return false;
        }

        // Null entities can't be in a building.
        if (entity == null) {
            return false;
        }

        // Call the version of the function that requires coordinates.
        return Compute.isInBuilding(game, entity, entity.getPosition());
    }

    /**
     * Determine if the given unit is inside of a building at the given coordinates.
     *
     * @param game The current {@link Game}. This value may be <code>null</code>.
     * @param entity the <code>Entity</code> to be checked. This value may be <code>null</code>.
     * @param coords the <code>Coords</code> of the building hex. This value may be <code>null</code>.
     * @return <code>true</code> if the entity is inside of the building at
     * those coordinates. <code>false</code> if there is no building at
     * those coordinates or if the entity is on the roof or in the air
     * above the building, or if any input argument is <code>null</code>
     */
    public static boolean isInBuilding(@Nullable Game game, @Nullable Entity entity,
                                       @Nullable Coords coords) {
        // No game, no building.
        if (game == null) {
            return false;
        }

        // Null entities can't be in a building.
        if (entity == null) {
            return false;
        }

        // Null coordinates can't have buildings.
        if (coords == null) {
            return false;
        }

        // Get the Hex at those coordinates.

        return Compute.isInBuilding(game, entity.getElevation(), coords);
    }

    public static boolean isInBuilding(Game game, int entityElev, Coords coords) {

        // Get the Hex at those coordinates.
        final Hex curHex = game.getBoard().getHex(coords);

        if (curHex == null) {
            // probably off board artillery or reinforcement
            return false;
        }

        // The entity can't be inside of a building that isn't there.
        if (!curHex.containsTerrain(Terrains.BLDG_ELEV)) {
            return false;
        }

        // The entity can't be inside of a building that isn't there.
        if (!curHex.containsTerrain(Terrains.BUILDING)) {
            return false;
        }

        // Get the elevations occupied by the building.
        int bldgHeight = curHex.terrainLevel(Terrains.BLDG_ELEV);
        int basement = 0;
        if (curHex.containsTerrain(Terrains.BLDG_BASEMENT_TYPE)) {
            basement = BasementType.getType(curHex.terrainLevel(Terrains.BLDG_BASEMENT_TYPE)).getDepth();
        }

        // Return true if the entity is in the range of building elevations.
        if ((entityElev >= (-basement)) && (entityElev < (bldgHeight))) {
            return true;
        }

        // Entity is not *inside* of the building.
        return false;
    }

    /**
     * Scatter from hex according to dive bombing rules (based on MoF),
     * TW pg 246.  The scatter can happen in any direction.
     *
     * @param coords The <code>Coords</code> to scatter from
     * @param moF The margin of failure, which deterimines scatter distance
     * @return the <code>Coords</code> scattered to and distance (moF)
     */
    public static Coords scatterDiveBombs(Coords coords, int moF) {
        return Compute.scatter(coords, moF);
    }

    /**
     * Scatter from hex according to altitude bombing rules (based on MoF),
     * TW pg 246.  The scatter only happens in the "front" three facings.
     *
     * @param coords The <code>Coords</code> to scatter from
     * @param facing Direction we were going at the time the bomb was dropped
     * @param moF How badly we failed
     * @return the <code>Coords</code> scattered to and distance (moF)
     */
    public static Coords scatterAltitudeBombs(Coords coords, int facing, int moF) {
        int dir = 0;
        int scatterDirection = Compute.d6(1);
        switch (scatterDirection) {
            case 1:
            case 2:
                dir = (facing - 1) % 6;
                break;
            case 3:
            case 4:
                dir = facing;
                break;
            case 5:
            case 6:
                dir = (facing + 1) % 6;
                break;
        }

        return coords.translated(dir, moF);
    }

    /**
     * scatter from hex according to direct fire artillery rules (based on MoF)
     *
     * @param coords The <code>Coords</code> to scatter from
     * @param moF The margin of failure, which deterimines scatter distance
     * @return the <code>Coords</code> scattered to
     */
    public static Coords scatterDirectArty(Coords coords, int moF) {
        return Compute.scatter(coords, moF);
    }

    /**
     * scatter from a hex according, roll d6 to choose scatter direction
     *
     * @param coords The <code>Coords</code> to scatter from
     * @param margin the <code>int</code> margin of failure, scatter distance will
     *               be the margin of failure
     * @return the <code>Coords</code> scattered to
     */
    public static Coords scatter(Coords coords, int margin) {
        int scatterDirection = Compute.d6(1) - 1;
        return coords.translated(scatterDirection, margin);
    }

    /**
     * scatter from hex according to atmospheric drop rules d6 for direction,
     * 1d6 per point of MOF
     *
     * @param coords The <code>Coords</code> to scatter from
     * @param margin the <code>int</code> margin of failure
     * @return the <code>Coords</code> scattered to
     */
    public static Coords scatterAssaultDrop(Coords coords, int margin) {
        int scatterDirection = Compute.d6(1) - 1;
        int distance = Compute.d6(margin);
        return coords.translated(scatterDirection, distance);
    }

    /**
     * Gets a new target for a flight of swarm missiles that was just shot at an
     * entity and has missiles left
     *
     * @param game The current {@link Game}
     * @param aeId The attacking <code>Entity</code>
     * @param coords
     * @param weaponId The <code>int</code> ID of the launcher used to fire this volley
     * @return the new target <code>Entity</code>. May return null if no new target available
     */
    public static @Nullable Entity getSwarmMissileTarget(Game game, int aeId, Coords coords,
                                                         int weaponId) {
        Entity tempEntity = null;
        // first, check the hex of the original target
        Iterator<Entity> entities = game.getEntities(coords);
        Vector<Entity> possibleTargets = new Vector<>();
        while (entities.hasNext()) {
            tempEntity = entities.next();
            if (!tempEntity.getTargetedBySwarm(aeId, weaponId)) {
                // we found a target
                possibleTargets.add(tempEntity);
            }
        }
        // if there is at least one target, get a random one of them
        if (!possibleTargets.isEmpty()) {
            return possibleTargets
                    .get(Compute.randomInt(possibleTargets.size()));
        }
        // loop through adjacent hexes
        for (int dir = 0; dir <= 5; dir++) {
            Coords tempcoords = coords.translated(dir);
            if (!game.getBoard().contains(tempcoords)) {
                continue;
            }
            if (coords.equals(tempcoords)) {
                continue;
            }
            entities = game.getEntities(tempcoords);
            if (entities.hasNext()) {
                tempEntity = entities.next();
                if (!tempEntity.getTargetedBySwarm(aeId, weaponId)) {
                    // we found a target
                    possibleTargets.add(tempEntity);
                }
            }
        }
        // if there is at least one target, get a random one of them
        if (!possibleTargets.isEmpty()) {
            return possibleTargets
                    .get(Compute.randomInt(possibleTargets.size()));
        }
        return null;
    }

    public static @Nullable Coords getFinalPosition(Coords curpos, int... v) {
        if ((v == null) || (v.length != 6) || (curpos == null)) {
            return curpos;
        }

        // step through each vector and move the direction indicated
        int thrust = 0;
        Coords endpos = curpos;
        for (int dir = 0; dir < 6; dir++) {
            thrust = v[dir];
            while (thrust > 0) {
                endpos = endpos.translated(dir);
                thrust--;
            }
        }

        return endpos;
    }

    /**
     * method to change a set of active vectors for a one-point thrust
     * expenditure in the giving facing
     *
     * @param v
     * @param facing
     * @return
     */
    public static int[] changeVectors(int[] v, int facing) {

        if ((v == null) || (v.length != 6)) {
            return v;
        }

        // first look at opposing vectors
        int oppv = facing + 3;
        if (oppv > 5) {
            oppv -= 6;
        }
        // is this vector active
        if (v[oppv] > 0) {
            // then decrement it by one and return
            v[oppv]--;
            return v;
        }

        // now check oblique vectors
        int oblv1 = facing + 2;
        if (oblv1 > 5) {
            oblv1 -= 6;
        }
        int oblv2 = facing - 2;
        if (oblv2 < 0) {
            oblv2 += 6;
        }

        // check both of these and if either is active
        // deal with it and then return
        if ((v[oblv1] > 0) || (v[oblv2] > 0)) {

            int newface = facing + 1;
            if (newface > 5) {
                newface = 0;
            }
            if (v[oblv1] > 0) {
                v[oblv1]--;
                v[newface]++;
            }

            newface = facing - 1;
            if (newface < 0) {
                newface = 5;
            }
            if (v[oblv2] > 0) {
                v[oblv2]--;
                v[newface]++;
            }
            return v;
        }

        // if nothing was found, then just increase velocity in this vector
        v[facing]++;
        return v;
    }

    /**
     * compare two vectors and determine if they are the same
     *
     * @param v1
     * @param v2
     * @return
     */
    public static boolean sameVectors(int[] v1, int[] v2) {

        for (int i = 0; i < 6; i++) {
            if (v1[i] != v2[i]) {
                return false;
            }
        }

        return true;
    }

    /**
     * Get the net velocity of two aeros for ramming attacks
     */
    public static int getNetVelocity(Coords src, Entity te, int avel, int tvel) {
        int angle = te.sideTableRam(src);

        switch (angle) {
            case Aero.RAM_TOWARD_DIR:
                return Math.max(avel + tvel, 1);
            case Aero.RAM_TOWARD_OBL:
                return Math.max(avel + (tvel / 2), 1);
            case Aero.RAM_AWAY_OBL:
                return Math.max(avel - (tvel / 2), 1);
            case Aero.RAM_AWAY_DIR:
                return Math.max(avel - tvel, 1);
        }
        return 0;
    }

    /**
     * Returns how much damage a weapon will do against against a BattleArmor
     * target if the BattleArmor vs BattleArmor rules on TO pg 109 are in
     * effect.
     *
     * @param damage     Original weapon damage
     * @param damageType The damage type for BA vs BA damage
     * @param target     The target, used for ensuring the target BA isn't
     *                   fire resistant
     * @return
     */
    public static int directBlowBADamage(double damage, int damageType,
                                         BattleArmor target) {
        switch (damageType) {
            case WeaponType.WEAPON_BURST_1D6:
                damage = Compute.d6();
                break;
            case WeaponType.WEAPON_BURST_3D6:
                damage = Compute.d6(3);
                break;
            case WeaponType.WEAPON_PLASMA:
                // If the target is fire-resistant BA, damage is normal
                if (!target.isFireResistant()) {
                    damage = 1 + Compute.d6(1);
                }
                break;
        }
        damage = Math.ceil(damage);
        return (int) damage;
    }

    /**
     * Used to get a human-readable string that represents the passed damage
     * type.
     *
     * @param damageType      The dmaageType constant
     * @param burstMultiplier The multiplier for burst damage, used by machine gun arrays against conventional infantry
     * @return A string representation of the damage type
     */
    public static String getDamageTypeString(int damageType, int burstMultiplier) {
        switch (damageType) {
            case WeaponType.WEAPON_DIRECT_FIRE:
                return Messages.getString("WeaponType.DirectFire");
            case WeaponType.WEAPON_CLUSTER_BALLISTIC:
                return Messages.getString("WeaponType.BallisticCluster");
            case WeaponType.WEAPON_PULSE:
                return Messages.getString("WeaponType.Pulse");
            case WeaponType.WEAPON_CLUSTER_MISSILE:
            case WeaponType.WEAPON_CLUSTER_MISSILE_1D6:
            case WeaponType.WEAPON_CLUSTER_MISSILE_2D6:
            case WeaponType.WEAPON_CLUSTER_MISSILE_3D6:
                return Messages.getString("WeaponType.Missile");
            case WeaponType.WEAPON_BURST_HALFD6:
                return Messages.getString("WeaponType.BurstHalf");
            case WeaponType.WEAPON_BURST_1D6:
            case WeaponType.WEAPON_BURST_2D6:
            case WeaponType.WEAPON_BURST_3D6:
            case WeaponType.WEAPON_BURST_4D6:
            case WeaponType.WEAPON_BURST_5D6:
            case WeaponType.WEAPON_BURST_6D6:
            case WeaponType.WEAPON_BURST_7D6:
            default:
                return String.format("%s (%dD6)", Messages.getString("WeaponType.Burst"),
                        burstMultiplier * (damageType - WeaponType.WEAPON_BURST_HALFD6));
        }
    }

    public static int directBlowInfantryDamage(double damage, int mos,
            int damageType, boolean isNonInfantryAgainstMechanized,
            boolean isAttackThruBuilding) {
        return directBlowInfantryDamage(damage, mos, damageType,
                isNonInfantryAgainstMechanized, isAttackThruBuilding,
                Entity.NONE, null);
    }

    /**
     * Method replicates the Non-Conventional Damage against Infantry damage
     * table as well as shifting for direct blows. also adjust for non-infantry
     * damaging mechanized infantry
     *
     * @param damage
     * @param mos
     * @param damageType
     * @return
     */
    public static int directBlowInfantryDamage(double damage, int mos,
               int damageType, boolean isNonInfantryAgainstMechanized,
               boolean isAttackThruBuilding, int attackerId, Vector<Report> vReport) {
        return directBlowInfantryDamage(damage, mos, damageType, isNonInfantryAgainstMechanized,
                isAttackThruBuilding, attackerId, vReport, 1);
    }

    /**
     * @return the maximum damage that a set of weapons can generate.
     */
    public static int computeTotalDamage(List<Mounted> weaponList){
        int totalDmg = 0;
        for (Mounted weapon : weaponList) {
            if (!weapon.isBombMounted() && weapon.isCrippled()) {
                continue;
            }
            WeaponType type = (WeaponType) weapon.getType();
            if (type.getDamage() == WeaponType.DAMAGE_VARIABLE) {
                // Estimate rather than compute exact bay / trooper damage sum.
                totalDmg += type.getRackSize();
            } else if (type.getDamage() == WeaponType.DAMAGE_ARTILLERY
                      || type.getDamage() == WeaponType.DAMAGE_BY_CLUSTERTABLE) {
                totalDmg += type.getRackSize();
            } else if (type.getDamage() == WeaponType.DAMAGE_SPECIAL) {// Handle dive bomb attacks here!
                if (type instanceof DiveBombAttack) {
                    totalDmg += weapon.getEntity().bombList.stream().mapToInt(Mounted::getExplosionDamage).sum();
                }
                if (type instanceof ISBAPopUpMineLauncher) {
                    totalDmg += 4;
                }
            } else {
                totalDmg += type.getDamage();
            }
        }

        return totalDmg;
    }

    /**
     * Method replicates the Non-Conventional Damage against Infantry damage
     * table as well as shifting for direct blows. also adjust for non-infantry
     * damaging mechanized infantry
     *
     * @param damage      The base amount of damage
     * @param mos         The margin of success
     * @param damageType  The damage class of the weapon, used to adjust damage against infantry
     * @param isNonInfantryAgainstMechanized Whether this is a non-infantry attack against mechanized infantry
     * @param isAttackThruBuilding Whether the attack is coming through a building hex
     * @param attackerId  The entity id of the attacking unit
     * @param vReport     The report messages vector
     * @param mgaSize     For machine gun array attacks, the number of linked weapons. For other weapons this should be 1.
     * @return The adjusted damage
     */
    public static int directBlowInfantryDamage(double damage, int mos,
            int damageType, boolean isNonInfantryAgainstMechanized,
            boolean isAttackThruBuilding, int attackerId, Vector<Report> vReport,
            int mgaSize) {

        int origDamageType = damageType;
        damageType += mos;
        double origDamage = damage;
        switch (damageType) {
            case WeaponType.WEAPON_DIRECT_FIRE:
                damage /= 10;
                break;
            case WeaponType.WEAPON_CLUSTER_BALLISTIC:
                damage /= 10;
                damage++;
                break;
            case WeaponType.WEAPON_PULSE:
                damage /= 10;
                damage += 2;
                break;
            case WeaponType.WEAPON_CLUSTER_MISSILE:
                damage /= 5;
                break;
            case WeaponType.WEAPON_CLUSTER_MISSILE_1D6:
                damage /= 5;
                damage += Compute.d6();
                break;
            case WeaponType.WEAPON_CLUSTER_MISSILE_2D6:
                damage /= 5;
                damage += Compute.d6(2);
                break;
            case WeaponType.WEAPON_CLUSTER_MISSILE_3D6:
                damage /= 5;
                damage += Compute.d6(3);
                break;
            case WeaponType.WEAPON_BURST_HALFD6:
                damage = Compute.d6() / 2.0;
                if (isAttackThruBuilding) {
                    damage *= 0.5;
                }
                break;
            case WeaponType.WEAPON_BURST_1D6:
                damage = Compute.d6(mgaSize);
                if (isAttackThruBuilding) {
                    damage *= 0.5;
                }
                break;
            case WeaponType.WEAPON_BURST_2D6:
                damage = Compute.d6(2 * mgaSize);
                if (isAttackThruBuilding) {
                    damage *= 0.5;
                }
                break;
            case WeaponType.WEAPON_BURST_3D6:
                damage = Compute.d6(3 * mgaSize);
                if (isAttackThruBuilding) {
                    damage *= 0.5;
                }
                break;
            case WeaponType.WEAPON_BURST_4D6:
                damage = Compute.d6(4 * mgaSize);
                if (isAttackThruBuilding) {
                    damage *= 0.5;
                }
                break;
            case WeaponType.WEAPON_BURST_5D6:
                damage = Compute.d6(5 * mgaSize);
                if (isAttackThruBuilding) {
                    damage *= 0.5;
                }
                break;
            case WeaponType.WEAPON_BURST_6D6:
                damage = Compute.d6(6 * mgaSize);
                if (isAttackThruBuilding) {
                    damage *= 0.5;
                }
                break;
            case WeaponType.WEAPON_BURST_7D6:
                damage = Compute.d6(7 * mgaSize);
                if (isAttackThruBuilding) {
                    damage *= 0.5;
                }
                break;
        }
        damage = Math.ceil(damage);

        // according to the following ruling, the half damage that mechanized
        // inf get against burst fire should trump the double damage they get
        // from non-infantry rather than cancel it out
        // http://bg.battletech.com/forums/index.php/topic,23928.0.html
        if (isNonInfantryAgainstMechanized) {
            if (damageType < WeaponType.WEAPON_BURST_HALFD6) {
                damage *= 2;
            } else {
                damage /= 2;
            }
        }

        if (vReport != null) {
            Report r = new Report();
            r.subject = attackerId;
            r.indent(2);

            r.add(getDamageTypeString(origDamageType, mgaSize));
            if (origDamageType != damageType) {
                if (isAttackThruBuilding) {
                    r.messageId = 9973;
                } else {
                    r.messageId = 9972;
                }
                r.add(getDamageTypeString(damageType, mgaSize));
            } else if (isAttackThruBuilding) {
                r.messageId = 9971;
            } else {
                r.messageId = 9970;
            }

            r.add((int) origDamage);
            r.add((int) damage);
            vReport.addElement(r);
        }
        return (int) damage;
    }


    /**
     * Method computes how much damage a dial down weapon has done
     *
     * @param weapon
     * @param wtype
     * @return new damage
     */
    public static int dialDownDamage(Mounted weapon, WeaponType wtype) {
        return Compute.dialDownDamage(weapon, wtype, 1);
    }

    /**
     * Method computes how much damage a dial down weapon has done
     *
     * @param weapon
     * @param wtype
     * @param range
     * @return new damage
     */
    public static int dialDownDamage(Mounted weapon, WeaponType wtype, int range) {
        int toReturn = wtype.getDamage(range);

        if (!weapon.hasModes()) {
            return toReturn;
        }

        String damage = weapon.curMode().getName();

        // Vehicle flamers have damage and heat modes so lets make sure this is
        // an actual dial down Damage.
        if ((damage.trim().toLowerCase().indexOf("damage") == 0)
                && (damage.trim().length() > 6)) {
            toReturn = Integer.parseInt(damage.substring(6).trim());
        }

        return Math.min(wtype.getDamage(range), toReturn);

    }

    /**
     * Method computes how much heat a dial down weapon generates
     *
     * @param weapon
     * @param wtype
     * @return Heat, minimum of 1;
     */
    public static int dialDownHeat(Mounted weapon, WeaponType wtype) {
        return Compute.dialDownHeat(weapon, wtype, 1);
    }

    /**
     * Method computes how much heat a dial down weapon generates
     *
     * @param weapon
     * @param wtype
     * @param range
     * @return Heat, minimum of 1;
     */
    public static int dialDownHeat(Mounted weapon, WeaponType wtype, int range) {
        int toReturn = wtype.getHeat();

        if (!weapon.hasModes()) {
            return toReturn;
        }

        int damage = wtype.getDamage(range);
        int newDamage = Compute.dialDownDamage(weapon, wtype, range);

        toReturn = Math.max(1,
                            wtype.getHeat() - Math.max(0, damage - newDamage));
        return toReturn;

    }

    /**
     * @param aPos - attacking entity
     * @param tPos - targeted entity
     * @return a vector of all the entities that are adjacent to the targeted
     * entity and would fall along the angle of attack
     */
    public static ArrayList<Entity> getAdjacentEntitiesAlongAttack(Coords aPos,
                                                                   Coords tPos, Game game) {
        ArrayList<Entity> entities = new ArrayList<>();
        ArrayList<Coords> coords = Coords.intervening(aPos, tPos);
        // loop through all intervening coords
        for (Coords c : coords) {
            // must be adjacent to the target
            if ((c.distance(tPos) > 1) || c.equals(tPos)) {
                continue;
            }
            // now lets add all the entities here
            for (Entity en : game.getEntitiesVector(c)) {
                entities.add(en);
            }
        }
        return entities;
    }

    public static boolean isInUrbanEnvironment(Game game, Coords unitPOS) {
        Hex unitHex = game.getBoard().getHex(unitPOS);

        if (unitHex.containsTerrain(Terrains.PAVEMENT)
                || unitHex.containsTerrain(Terrains.BUILDING)
                || unitHex.containsTerrain(Terrains.RUBBLE)) {
            return true;
        }

        // loop through adjacent hexes
        for (int dir = 0; dir <= 5; dir++) {
            Coords adjCoords = unitPOS.translated(dir);
            Hex adjHex = game.getBoard().getHex(adjCoords);

            if (!game.getBoard().contains(adjCoords)) {
                continue;
            }
            if (unitPOS.equals(adjCoords)) {
                continue;
            }

            // hex pavement or building?
            if (adjHex.containsTerrain(Terrains.PAVEMENT)
                    || adjHex.containsTerrain(Terrains.BUILDING)
                    || adjHex.containsTerrain(Terrains.RUBBLE)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isAirToGround(Entity attacker, Targetable target) {
        if ((attacker == null) || (target == null)) {
            return false;
        }

        //Artillery attacks need to return differently, since none of the usual air to ground modifiers apply to them
        if (target.getTargetType() == Targetable.TYPE_HEX_ARTILLERY) {
            return false;
        }

        if (attacker.isSpaceborne()) {
            return false;
        }
        // According to errata, VTOL and WiGes are considered ground targets
        return attacker.isAirborne() && !target.isAirborne() && attacker.isAero();

    }

    public static boolean isAirToAir(Entity attacker, Targetable target) {
        if ((attacker == null) || (target == null)) {
            return false;
        }
        // According to errata, VTOL and WiGes are considered ground targets
        return attacker.isAirborne() && target.isAirborne();
    }

    public static boolean isGroundToAir(Entity attacker, Targetable target) {
        if ((attacker == null) || (target == null)) {
            return false;
        }
        return !attacker.isAirborne() && target.isAirborne();
    }

    public static boolean isGroundToGround(Entity attacker, Targetable target) {
        if ((attacker == null) || (target == null)) {
            return false;
        }
        return !attacker.isAirborne() && !target.isAirborne();
    }

    /**
     * This is a homebrew function partially drawn from pg. 40-1 of AT2R that
     * allows units that flee the field for any reason to return after a certain
     * number of rounds It can potentially be expanded to include other
     * conditions
     *
     * @param en
     * @return number of rounds until return (-1 if never)
     */
    public static int roundsUntilReturn(Game game, Entity en) {

        if (!en.isAero()) {
            return -1;
        }

        if (!game.getOptions().booleanOption(OptionsConstants.ADVAERORULES_RETURN_FLYOVER)) {
            return -1;
        }

        IAero a = (IAero) en;

        // the table in AT2R is backwards, it should take longer to return if
        // your velocity is higher
        int turns = 1 + (int) Math.ceil(a.getCurrentVelocity() / 4.0);

        // OOC units should take longer, how about two extra turns?
        if (a.isOutControlTotal()) {
            turns += 2;
        }
        return turns;
    }

    public static boolean inDeadZone(Game game, Entity ae, Targetable target) {
        if (ae.isSpaceborne()) {
            return false;
        }
        // Account for "dead zones" between Aeros at different altitudes
        if (Compute.isAirToAir(ae, target)) {
            int distance = Compute.effectiveDistance(game, ae, target,
                                                     target.isAirborneVTOLorWIGE());
            int aAlt = ae.getAltitude();
            int tAlt = target.getAltitude();
            if (target.isAirborneVTOLorWIGE()) {
                tAlt++;
            }
            int altDiff = Math.abs(aAlt - tAlt);
            if (altDiff >= (distance - altDiff)) {
                return true;
            }
        }
        return false;
    }

    public static ArrayList<Coords> getAcceptableUnloadPositions(
            List<Coords> ring, Entity unit, Game game, int elev) {

        ArrayList<Coords> acceptable = new ArrayList<>();

        for (Coords pos : ring) {
            Hex hex = game.getBoard().getHex(pos);
            if (null == hex) {
                continue;
            }
            // no stacking violations, no prohibited terrain, and within 2
            // elevations

            if (!unit.isLocationProhibited(pos)
                && (null == stackingViolation(game, unit.getId(), pos, unit.climbMode()))
                && (Math.abs(hex.getLevel() - elev) < 3)) {
                acceptable.add(pos);
            }
        }
        return acceptable;
    }

    /**
     * Builds a list of all adjacent units that can load the given Entity.
     * @param en   The entity to load
     * @param pos  The coordinates of the hex to load from
     * @param elev The absolute elevation of the unit at the point of loading (surface
     *             of the hex + elevation over the surface)
     * @param game The current {@link Game}
     * @return     All adjacent units that can mount the Entity
     */
    public static List<Entity> getMountableUnits(Entity en, Coords pos, int elev, Game game) {
        List<Entity> mountable = new ArrayList<>();
        // Expanded to include trains

        // the rules don't say that the unit must be facing loader
        // so lets take the ring
        for (Coords c : pos.allAdjacent()) {
            Hex hex = game.getBoard().getHex(c);
            if (null == hex) {
                continue;
            }
            for (Entity other : game.getEntitiesVector(c)) {
                // Is the other unit friendly and not the current entity?
                if ((en.getOwner().equals(other.getOwner()) || (en.getOwner()
                                                                  .getTeam() == other.getOwner().getTeam()))
                    && !en.equals(other)
                    && ((other instanceof SmallCraft) || other.getTowing() != Entity.NONE || other.getTowedBy() != Entity.NONE)
                    && other.canLoad(en)
                    && !other.isAirborne()
                    && (Math.abs((hex.getLevel() + other.getElevation())
                                 - elev) < 3) && !mountable.contains(other)) {
                    mountable.add(other);
                }
            }
        }

        return mountable;

    }

    public static boolean allowAimedShotWith(Mounted weapon, AimingMode aimingMode) {
        WeaponType wtype = (WeaponType) weapon.getType();
        boolean isWeaponInfantry = wtype.hasFlag(WeaponType.F_INFANTRY);
        boolean usesAmmo = (wtype.getAmmoType() != AmmoType.T_NA) && !isWeaponInfantry;
        Mounted ammo = usesAmmo ? weapon.getLinked() : null;
        AmmoType atype = ammo == null ? null : (AmmoType) ammo.getType();

        // Leg and swarm attacks can't be aimed.
        if (wtype.getInternalName().equals(Infantry.LEG_ATTACK)
                || wtype.getInternalName().equals(Infantry.SWARM_MEK)) {
            return false;
        }

        switch (aimingMode) {
            case NONE:
                return false;
            case IMMOBILE:
                if (weapon.getCurrentShots() > 1) {
                    return false;
                }

                if (atype == null) {
                    break;
                }

                switch (atype.getAmmoType()) {
                    case AmmoType.T_SRM_STREAK:
                    case AmmoType.T_LRM_STREAK:
                    case AmmoType.T_LRM:
                    case AmmoType.T_LRM_IMP:
                    case AmmoType.T_LRM_TORPEDO:
                    case AmmoType.T_SRM:
                    case AmmoType.T_SRM_IMP:
                    case AmmoType.T_SRM_TORPEDO:
                    case AmmoType.T_MRM:
                    case AmmoType.T_NARC:
                    case AmmoType.T_INARC:
                    case AmmoType.T_AMS:
                    case AmmoType.T_ARROW_IV:
                    case AmmoType.T_LONG_TOM:
                    case AmmoType.T_SNIPER:
                    case AmmoType.T_THUMPER:
                    case AmmoType.T_SRM_ADVANCED:
                    case AmmoType.T_LRM_TORPEDO_COMBO:
                    case AmmoType.T_ATM:
                    case AmmoType.T_IATM:
                    case AmmoType.T_MML:
                    case AmmoType.T_EXLRM:
                    case AmmoType.T_NLRM:
                    case AmmoType.T_TBOLT_5:
                    case AmmoType.T_TBOLT_10:
                    case AmmoType.T_TBOLT_15:
                    case AmmoType.T_TBOLT_20:
                    case AmmoType.T_HAG:
                    case AmmoType.T_ROCKET_LAUNCHER:
                        return false;
                }
                if (((atype.getAmmoType() == AmmoType.T_AC_LBX_THB)
                     || (atype.getAmmoType() == AmmoType.T_AC_LBX)
                     || (atype.getAmmoType() == AmmoType.T_SBGAUSS))
                    && (atype.getMunitionType().contains(AmmoType.Munitions.M_CLUSTER))) {
                    return false;
                }
                // Flak Ammo can't make aimed shots
                if (((atype.getAmmoType() == AmmoType.T_AC)
                     || (atype.getAmmoType() == AmmoType.T_AC_ULTRA)
                     || (atype.getAmmoType() == AmmoType.T_AC_ULTRA_THB))
                    && (atype.getMunitionType().contains(AmmoType.Munitions.M_FLAK))) {
                    return false;
                }

                break;
            case TARGETING_COMPUTER:
                if (!wtype.hasFlag(WeaponType.F_DIRECT_FIRE)
                    || wtype.hasFlag(WeaponType.F_PULSE)
                    || (wtype instanceof HAGWeapon)) {
                    return false;
                }
                if (weapon.getCurrentShots() > 1) {
                    return false;
                }

                if ((atype != null)
                    && ((atype.getAmmoType() == AmmoType.T_AC_LBX_THB)
                        || (atype.getAmmoType() == AmmoType.T_AC_LBX)
                        || (atype.getAmmoType() == AmmoType.T_SBGAUSS))
                    && (atype.getMunitionType().contains(AmmoType.Munitions.M_CLUSTER))) {
                    return false;
                }

                // Flak Ammo can't make aimed shots
                if ((atype != null)
                    && ((atype.getAmmoType() == AmmoType.T_AC)
                        || (atype.getAmmoType() == AmmoType.T_AC_ULTRA)
                        || (atype.getAmmoType() == AmmoType.T_AC_ULTRA_THB))
                    && (atype.getMunitionType().contains(AmmoType.Munitions.M_FLAK))) {
                    return false;
                }
                break;
            default:
                break;
        }
        return true;
    }

    // Taken from MekHQ, assumptions are whatever Taharqa made for there - Dylan
    public static int getTotalGunnerNeeds(Entity entity) {
        if (entity instanceof SmallCraft || entity instanceof Jumpship) {
            int nStandardW = 0;
            int nCapitalW = 0;
            for (Mounted m : entity.getTotalWeaponList()) {
                EquipmentType type = m.getType();
                if (type instanceof BayWeapon) {
                    continue;
                }
                if (type instanceof WeaponType) {
                    if (((WeaponType) type).isCapital()) {
                        nCapitalW++;
                    } else {
                        nStandardW++;
                    }
                }
            }
            return nCapitalW + (int) Math.ceil(nStandardW / 6.0);
        } else if (entity.isSupportVehicle()) {
            return getSupportVehicleGunnerNeeds(entity);
        } else if (entity instanceof Tank) {
            return (getFullCrewSize(entity) - 1);
        } else if (entity instanceof Infantry) {
            return getFullCrewSize(entity);
        } else if (entity.getCrew().getCrewType().getGunnerPos() > 0) {
            //Tripod, QuadVee, or dual cockpit
            return 1;
        }
        return 0;
    }

    // Taken from MekHQ, assumptions are whatever Taharqa made for there - Dylan
    public static int getAeroCrewNeeds(Entity entity) {
        if (entity instanceof Dropship) {
            if (((Dropship) entity).isMilitary()) {
                return 4 + (int) Math.ceil(entity.getWeight() / 5000.0);
            } else {
                return 3 + (int) Math.ceil(entity.getWeight() / 5000.0);
            }
        } else if (entity instanceof SmallCraft) {
            return getTotalDriverNeeds(entity);
        } else if (entity instanceof Warship || entity instanceof SpaceStation) {
            return 45 + (int) Math.ceil(entity.getWeight() / 5000.0);
        } else if (entity instanceof Jumpship) {
            return 6 + (int) Math.ceil(entity.getWeight() / 20000.0);
        }
        return 0;
    }

    /**
     * Calculates the base crew requirements for support vehicles.
     *
     * @param entity The support vehicle
     * @return       The minimum base crew
     */
    public static int getSVBaseCrewNeeds(Entity entity) {
        if (entity.isTrailer() && (entity.getEngine().getEngineType() == Engine.NONE)) {
            return 0;
        }
        final boolean naval = entity.getMovementMode().equals(EntityMovementMode.NAVAL)
                || entity.getMovementMode().equals(EntityMovementMode.HYDROFOIL)
                || entity.getMovementMode().equals(EntityMovementMode.SUBMARINE);
        int crew;
        if (entity.getWeightClass() == EntityWeightClass.WEIGHT_SMALL_SUPPORT) {
            crew = 1;
        } else if (entity.getWeightClass() == EntityWeightClass.WEIGHT_MEDIUM_SUPPORT) {
            if (naval || entity.getMovementMode().equals(EntityMovementMode.AIRSHIP)) {
                crew = 4;
            } else {
                crew = 2;
            }
        } else {
            crew = 3;
            if (naval) {
                crew += (int) Math.ceil(entity.getWeight() / 5000);
            } else if (entity.getMovementMode().equals(EntityMovementMode.AIRSHIP)) {
                crew += (int) Math.ceil(entity.getWeight() / 500);
            }
        }
        return crew;
    }

    /**
     * Calculates number of gunners required for a support vehicle. See TM, 131.
     *
     * @param entity The support vehicle
     * @return       The number of gunners required.
     */
    public static int getSupportVehicleGunnerNeeds(Entity entity) {
        final boolean advFireCon = entity.hasMisc(MiscType.F_ADVANCED_FIRECONTROL);
        final boolean basicFireCon = !advFireCon && entity.hasMisc(MiscType.F_BASIC_FIRECONTROL);
        if (entity.getWeightClass() == EntityWeightClass.WEIGHT_SMALL_SUPPORT) {
            if (!advFireCon && !basicFireCon) {
                // No fire control requires one gunner per weapon.
                return entity.getWeaponList().size();
            } else {
                // Otherwise we require one gunner per facing, with turrets and pintle mounts counting
                // as separate facings
                Set<Integer> facings = new HashSet<>();
                int pintles = 0;
                for (Mounted m : entity.getWeaponList()) {
                    if (m.isPintleTurretMounted()) {
                        pintles++;
                    } else {
                        facings.add(m.getLocation());
                    }
                }
                if (advFireCon) {
                    // Advanced fire control lets the driver count as a gunner, so one fewer dedicated gunners is needed.
                    return Math.max(0, pintles + facings.size() - 1);
                } else {
                    return pintles + facings.size();
                }
            }
        } else {
            // Medium and large support vehicle gunner requirements are based on weapon tonnage
            double tonnage = entity.getWeaponList().stream().filter(m -> !m.getType().hasFlag(WeaponType.F_AMS))
                    .mapToDouble(Mounted::getTonnage).sum();
            if (advFireCon) {
                if (entity.getStructuralTechRating() == ITechnology.RATING_F) {
                    return (int) Math.ceil(tonnage / 6.0);
                } else if (entity.getStructuralTechRating() == ITechnology.RATING_E) {
                    return (int) Math.ceil(tonnage / 5.0);
                }
                return (int) Math.ceil(tonnage / 4.0);
            } else if (basicFireCon) {
                return (int) Math.ceil(tonnage / 3.0);
            } else {
                return (int) Math.ceil(tonnage / 2.0);
            }
        }
    }

    /**
     * Calculates addiontal crew required by support vehicles and advanced aerospace vessels
     * for certain misc equipment.
     *
     * @param entity The unit
     * @return       The number of additional crew required
     */
    public static int getAdditionalNonGunner(Entity entity) {
        int crew = 0;
        for (Mounted m : entity.getMisc()) {
            if (m.getType().hasFlag(MiscType.F_COMMUNICATIONS)) {
                crew += (int) m.getTonnage();
            } else if (m.getType().hasFlag(MiscType.F_FIELD_KITCHEN)) {
                crew += 3;
            } else if (m.getType().hasFlag(MiscType.F_MOBILE_FIELD_BASE)) {
                crew += 5;
            } else if (m.getType().hasFlag(MiscType.F_MASH)) {
                crew += 5 * (int) m.getSize();
            }
        }
        if (entity.isSuperHeavy()) {
            // Tactical Officer
            return 1;
        }
        return crew;
    }

    // Taken from MekHQ, assumptions are whatever Taharqa made for there - Dylan
    public static int getFullCrewSize(Entity entity) {
        if (entity.isSupportVehicle()) {
            int crew = getSVBaseCrewNeeds(entity) + getSupportVehicleGunnerNeeds(entity)
                    + getAdditionalNonGunner(entity);
            if (crew < 4) {
                return crew;
            }
            return crew + (int) Math.ceil(crew / 6.0);
        } else if (entity instanceof Tank) {
            return (int) Math.ceil(entity.getWeight() / 15.0) + ((Tank) entity).getExtraCrewSeats();
        } else if (entity instanceof BattleArmor) {
            int ntroopers = 0;
            for (int trooper = 1; trooper < entity.locations(); trooper++) {
                //less than zero means the suit is destroyed
                if (entity.getInternal(trooper) >= 0) {
                    //Also, if any modular equipment is missing, then we will consider this
                    //unit to be inoperable and will not allow it to load soldiers. This is because
                    //we have no mechanism in MM to handle BA where some suits have the equipment
                    // and others do not
                    boolean useSuit = true;
                    for (Mounted m : entity.getEquipment()) {
                        if (m.isMissingForTrooper(trooper)) {
                            useSuit = false;
                            break;
                        }
                    }
                    if (useSuit) {
                        ntroopers++;
                    }
                }
            }
            return ntroopers;
        } else if (entity instanceof Infantry) {
            return ((Infantry) entity).getSquadCount() * ((Infantry) entity).getSquadSize();
        } else if (entity instanceof Jumpship || entity instanceof SmallCraft) {
            return getAeroCrewNeeds(entity) + getTotalGunnerNeeds(entity);
        } else if (entity.isSuperHeavy() || entity.isTripodMek()) {
            return getTotalDriverNeeds(entity) + getTotalGunnerNeeds(entity) + getAdditionalNonGunner(entity);
        } else {
            return 1;
        }
    }

    // Taken from MekHQ, assumptions are whatever Taharqa made for there - Dylan
    public static int getTotalDriverNeeds(Entity entity) {
        //Fix for MHQ Bug #3. Space stations have as much need for pilots as jumpships do.
        if (entity instanceof SpaceStation) {
            return 2;
        }
        if (entity instanceof SmallCraft || entity instanceof Jumpship) {
            //its not at all clear how many pilots dropships and jumpships
            //should have, but the old BattleSpace book suggests they should
            // be able to get by with 2. For warships, lets go with 2 per shift
            // so 6.
            if (entity instanceof Warship) {
                return 6;
            }
            if (entity instanceof SmallCraft) {
                return 3;
            }
            return 2;
        }
        if (entity.getCrew().getCrewType() == CrewType.COMMAND_CONSOLE) {
            return 2;
        }
        if (entity instanceof Mech || entity instanceof Tank || entity instanceof Aero || entity instanceof Protomech) {
            //only one driver please
            return 1;
        } else if (entity instanceof Infantry) {
            return getFullCrewSize(entity);
        }
        return 0;
    }

    /**
     * Should we treat this entity, in its current state, as if it is a spheroid unit
     * flying in atmosphere?
     */
    public static boolean useSpheroidAtmosphere(Game game, Entity en) {
        if (!en.isAero()) {
            return false;
        }
        // are we in space?
        if (game.getBoard().inSpace()) {
            return false;
        }
        // aerodyne's will operate like spheroids in vacuum
        PlanetaryConditions conditions = game.getPlanetaryConditions();
        if (!((IAero) en).isSpheroid()
                && !conditions.getAtmosphere().isLighterThan(Atmosphere.THIN)) {
            return false;
        }
        // are we in atmosphere?
        return en.isAirborne();
    }

    /**
     * Worker function that checks if an indirect attack is impossible for the given passed-in arguments
     */
    public static boolean indirectAttackImpossible(Game game, Entity ae, Targetable target, WeaponType wtype, Mounted weapon) {
        boolean isLandedSpheroid = ae.isAero() && ((IAero) ae).isSpheroid() && (ae.getAltitude() == 0) && game.getBoard().onGround();
        int altDif = target.getAltitude() - ae.getAltitude();
        boolean noseWeaponAimedAtGroundTarget = (weapon != null) && (weapon.getLocation() == Aero.LOC_NOSE) && (altDif < 1);

        return game.getOptions().booleanOption(OptionsConstants.BASE_INDIRECT_FIRE)
                    && !game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_INDIRECT_ALWAYS_POSSIBLE)
                    && LosEffects.calculateLOS(game, ae, target).canSee()
                    && (!game.getOptions().booleanOption(OptionsConstants.ADVANCED_DOUBLE_BLIND)
                            || Compute.canSee(game, ae, target))
                    && !(wtype instanceof ArtilleryCannonWeapon)
                    && !wtype.hasFlag(WeaponType.F_MORTARTYPE_INDIRECT)
                    && !(isLandedSpheroid && noseWeaponAimedAtGroundTarget);
    }

    public static boolean isFlakAttack(Entity attacker, Entity target) {
        boolean validLocation = !(attacker.isSpaceborne()
                || target.isSpaceborne()
                || attacker.isOffBoard()
                || target.isOffBoard());
        return validLocation && (target.isAirborne() || target.isAirborneVTOLorWIGE());
    }

    public static int turnsTilHit(int distance) {
        final int turnsTilHit;
        // See indirect flight times table, TO p181
        if (distance <= Board.DEFAULT_BOARD_HEIGHT) {
            turnsTilHit = 0;
        } else if (distance <= (8 * Board.DEFAULT_BOARD_HEIGHT)) {
            turnsTilHit = 1;
        } else if (distance <= (15 * Board.DEFAULT_BOARD_HEIGHT)) {
            turnsTilHit = 2;
        } else if (distance <= (21 * Board.DEFAULT_BOARD_HEIGHT)) {
            turnsTilHit =3;
        } else if (distance <= (26 * Board.DEFAULT_BOARD_HEIGHT)) {
            turnsTilHit = 4;
        } else {
            turnsTilHit = 5;
        }
        return turnsTilHit;
    }

    /**
     * Get turns for an indirect or off-board round to hit with its current velocity
     * @param game
     * @param ae Attacker
     * @param target Target hex/entity
     * @param velocity speed of round, default 50 according to WeaponAttackAction
     * @return
     */
    public static int turnsTilBOMHit(Game game, Entity ae, Targetable target, int velocity) {
        int distance = Compute.effectiveDistance(game, ae, target);
        distance = (int) Math.floor((double) distance / game.getPlanetaryConditions().getGravity());
        return distance / velocity;
    }

    /**
     * @param game
     * @param ae
     * @param target
     * @param homing to determine if we need the homing lead or some other value.
     * @return Coordinates to aim at to hit this target while it's on the move (we think).
     */
    public static Coords calculateArtilleryLead(Game game, Entity ae, Targetable target, boolean homing) {
        int leadAmount = 0;
        int direction = 0;
        int turnsTilHit = turnsTilHit(effectiveDistance(game, ae, target, true));

        // Hexes can't move...
        if (target instanceof Entity) {
            Entity te = (Entity) target;
            int mp = te.getPriorPosition().distance(te.getPosition()); // Assume last move presages the next
            if (mp == 0 && game.getRoundCount() == 0) {
                // Assume a mobile enemy will move somewhat after deploying
                mp = te.getWalkMP();
            }

            // Try to keep the current position within the homing radius, unless they're real fast...
            if (homing) {
                leadAmount = (mp * (turnsTilHit)) + HOMING_RADIUS;
            } else {
                leadAmount = mp * (turnsTilHit + 1);
            }

            // Guess at the target's movement direction
            if (te.movedLastRound != EntityMovementType.MOVE_NONE) {
                // Assume they'll keep moving in approximately the same direction
                direction = te.getPriorPosition().direction(te.getPosition());
            } else {
                // They'll likely move in the direction they're facing...?
                direction = te.getFacing();
            }
        }

        return calculateArtilleryLead(target.getPosition(), direction, leadAmount);
    }

    /**
     * @param targetPoint
     * @param direction
     * @param leadAmount
     * @return Coordinates to target given this lead and direction
     */
    public static Coords calculateArtilleryLead(Coords targetPoint, int direction, int leadAmount) {
        Coords newPoint = targetPoint.translated(direction, leadAmount);
        if (LogManager.getLogger().isDebugEnabled()) {
            StringBuilder msg = new StringBuilder("Computed coordinates ( ")
                    .append(newPoint.toString())
                    .append(" ) for target point ( ").append(targetPoint.toString())
                    .append(" ), direction ").append(direction)
                    .append(", lead range ").append(leadAmount);

            LogManager.getLogger().debug(msg.toString());
        }
        return newPoint;
    }

} // End public class Compute
