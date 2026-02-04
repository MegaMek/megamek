/*
 * Copyright (C) 2000-2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2002-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */

package megamek.common.compute;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static megamek.codeUtilities.MathUtility.clamp;

import java.util.*;

import megamek.common.*;
import megamek.common.actions.*;
import megamek.common.annotations.Nullable;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.board.Board;
import megamek.common.board.BoardHelper;
import megamek.common.board.Coords;
import megamek.common.board.CrossBoardAttackHelper;
import megamek.common.enums.AimingMode;
import megamek.common.enums.BasementType;
import megamek.common.enums.MoveStepType;
import megamek.common.enums.TechRating;
import megamek.common.equipment.*;
import megamek.common.equipment.AmmoType.AmmoTypeEnum;
import megamek.common.game.Game;
import megamek.common.interfaces.ILocationExposureStatus;
import megamek.common.moves.MoveStep;
import megamek.common.options.OptionsConstants;
import megamek.common.planetaryConditions.Atmosphere;
import megamek.common.planetaryConditions.IlluminationLevel;
import megamek.common.planetaryConditions.PlanetaryConditions;
import megamek.common.rolls.MMRoll;
import megamek.common.rolls.Roll;
import megamek.common.rolls.TargetRoll;
import megamek.common.units.*;
import megamek.common.weapons.TeleMissile;
import megamek.common.weapons.Weapon;
import megamek.common.weapons.artillery.ArtilleryCannonWeapon;
import megamek.common.weapons.attacks.DiveBombAttack;
import megamek.common.weapons.attacks.InfantryAttack;
import megamek.common.weapons.autoCannons.ACWeapon;
import megamek.common.weapons.autoCannons.RACWeapon;
import megamek.common.weapons.autoCannons.UACWeapon;
import megamek.common.weapons.battleArmor.innerSphere.ISBAPopUpMineLauncher;
import megamek.common.weapons.bayWeapons.BayWeapon;
import megamek.common.weapons.gaussRifles.HAGWeapon;
import megamek.common.weapons.handlers.AreaEffectHelper;
import megamek.common.weapons.handlers.DamageFalloff;
import megamek.common.weapons.infantry.InfantryWeapon;
import megamek.common.weapons.mgs.MGWeapon;
import megamek.logging.MMLogger;
import megamek.server.Server;
import megamek.server.SmokeCloud;

/**
 * The Compute class is designed to provide static methods for 'Meks and other entities moving, firing, etc.
 */
public class Compute {
    private static final MMLogger LOGGER = MMLogger.create(Compute.class);

    public static final int ARC_360 = 0;
    public static final int ARC_FORWARD = 1;
    public static final int ARC_LEFT_ARM = 2;
    public static final int ARC_RIGHT_ARM = 3;
    public static final int ARC_REAR = 4;
    public static final int ARC_LEFT_SIDE = 5;
    public static final int ARC_RIGHT_SIDE = 6;
    public static final int ARC_MAIN_GUN = 7;
    public static final int ARC_NORTH = 8;
    public static final int ARC_EAST = 9;
    public static final int ARC_WEST = 10;
    public static final int ARC_NOSE = 11;
    public static final int ARC_LEFT_WING = 12;
    public static final int ARC_RIGHT_WING = 13;
    public static final int ARC_LEFT_WING_AFT = 14;
    public static final int ARC_RIGHT_WING_AFT = 15;
    public static final int ARC_LEFT_SIDE_SPHERE = 16;
    public static final int ARC_RIGHT_SIDE_SPHERE = 17;
    public static final int ARC_LEFT_SIDE_AFT_SPHERE = 18;
    public static final int ARC_RIGHT_SIDE_AFT_SPHERE = 19;
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
    public static final int ARC_LEFT_WING_WPL = 39;
    public static final int ARC_RIGHT_WING_WPL = 40;
    public static final int ARC_LEFT_WING_AFT_WPL = 41;
    public static final int ARC_RIGHT_WING_AFT_WPL = 42;
    public static final int ARC_LEFT_SIDE_SPHERE_WPL = 43;
    public static final int ARC_RIGHT_SIDE_SPHERE_WPL = 44;
    public static final int ARC_LEFT_SIDE_AFT_SPHERE_WPL = 45;
    public static final int ARC_RIGHT_SIDE_AFT_SPHERE_WPL = 46;
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
        return d6(1);
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
     *
     * @param number the number of dice to roll
     * @param faces  the number of faces on those dice
     *
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
     *
     * @param count  the count of sets of dice to roll
     * @param number the number of dice to roll per set
     * @param faces  the number of faces per die
     *
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
     * Returns the sum of the highest two values from the provided list of integer values.
     *
     * <p>This method efficiently computes the top two results without sorting, making it suitable for rules that
     * require selecting the best two dice out of several rolled, such as natural aptitude-empowered skill checks.</p>
     *
     * <p>Edge cases are handled gracefully:</p>
     * <ul>
     *     <li>If no values are provided, the result is {@code 0}.</li>
     *     <li>If only one value is provided, that value is returned.</li>
     *     <li>If two or more values are provided, the sum of the highest two is returned.</li>
     * </ul>
     *
     * @param values one or more integers
     *
     * @return the sum of the highest two values (clamped within Integer Min/Max), or a fallback value for edge cases
     *
     * @author Illiani
     * @since 0.50.10
     */
    public static int getHighestTwoIntegers(int... values) {
        int highest = Integer.MIN_VALUE;
        int second = Integer.MIN_VALUE;

        // Edge cases
        if (values.length == 0) {
            return 0;
        } else if (values.length == 1) {
            return values[0];
        }

        // Find the highest two values
        for (int value : values) {
            if (value > highest) {
                second = highest;
                highest = value;
            } else if (value > second) {
                second = value;
            }
        }

        // Compute sum in long to avoid overflow, then clamp to int range
        long sum = (long) highest + (long) second;
        return (int) clamp(sum, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    /**
     * Generates a number between 0 and  max value exclusive (this means maxValue-1). e.g. randomInt(3) will generate 0,
     * 1, or 2.
     */
    public static int randomInt(int maxValue) {
        Roll roll = new MMRoll(random, maxValue);
        return roll.getIntValue();
    }

    /**
     * Generates a number between 0 and  max value inclusive (this means maxValue). e.g. randomInt(3) will generate 0,
     * 1, 2 or 3.
     */
    public static int randomIntInclusive(int maxValue) {
        Roll roll = new MMRoll(random, maxValue + 1);
        return roll.getIntValue();
    }


    /**
     * Generates a number between 1 and  max value inclusive (this means maxValue). e.g. randomInt(3) will generate 1, 2
     * or 3.
     */
    public static int randomRealIntInclusive(int maxValue) {
        Roll roll = new MMRoll(random, maxValue);
        return roll.getIntValue() + 1;
    }

    /**
     * Wrapper to random#randomFloat()
     */
    public static float randomFloat() {
        return random.randomFloat();
    }

    /**
     * Selects a random element from a list
     *
     * @param list The list of items to select from
     * @param <T>  The list type
     *
     * @return An element in the list
     *
     * @throws IllegalArgumentException when the given list is empty
     */
    public static <T> T randomListElement(List<T> list) {
        if (list.isEmpty()) {
            throw new IllegalArgumentException("Tried to select random element from empty list");
        } else {
            return list.get(randomInt(list.size()));
        }
    }

    /**
     * Sets the RNG to the desired type
     */
    public static void setRNG(int type) {
        random = MMRandom.generate(type);
    }

    /**
     * Sets the RNG to the specific instance.
     *
     * @param random A non-null instance of {@link MMRandom} to use for all random number generation.
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
     * Returns the odds that a certain number or above will be rolled on 2d6, or on 3d6 drop the lowest if the flag is
     * set.
     *
     * @param dropLowest Flag that determines whether 2d6 or 3d6 drop the lowest is used
     */
    public static double oddsAbove(int n, boolean dropLowest) {
        if (n <= 2) {
            return 100.0;
        } else if (n > 12) {
            return 0;
        }

        final double[] odds;
        if (dropLowest) {
            odds = new double[] { 100.0, 100.0, 100.0, 99.54, 98.15, 94.91,
                                  89.35, 80.56, 68.06, 52.32, 35.65, 19.91, 7.41, 0 };
        } else {
            odds = new double[] { 100.0, 100.0, 100.0, 97.2, 91.6, 83.3, 72.2,
                                  58.3, 41.6, 27.7, 16.6, 8.3, 2.78, 0 };
        }
        return odds[n];
    }

    /**
     * Returns an entity if the specified entity would cause a stacking violation entering a hex, or returns null if it
     * would not. The returned entity is the entity causing the violation.
     * <p>
     * The position and elevation for the stacking violation are derived from the Entity represented by the passed
     * Entity ID.
     * By default, ignores hidden units.
     *
     * @param game       The Game instance
     * @param enteringId The gameId of the moving Entity
     * @param coords     The hex being entered
     * @param climbMode  The moving Entity's climb mode at the point it enters the destination hex
     * @return Entity instance that is causing the violation
     */
    public static Entity stackingViolation(Game game, int enteringId, Coords coords, boolean climbMode) {
        Entity entering = game.getEntity(enteringId);
        if (entering == null) {
            return null;
        }
        return Compute.stackingViolation(game, entering, coords, null, climbMode, true);
    }

    /**
     * When compiling an unloading step, both the transporter and the unloaded unit probably occupy some other position
     * on the board.
     * <p>
     * The position and elevation for the stacking violation are derived from the passed Entity.
     *
     * @param game      The Game instance
     * @param entering  The Entity entering the hex
     * @param dest      The hex being entered
     * @param transport Represents the unit transporting entering, which may affect stacking, can be null
     * @param climbMode The moving Entity's climb mode at the point it enters the destination hex
     * @param ignoreHidden true by default.
     * @return Entity instance that is causing the violation
     */
    public static Entity stackingViolation(Game game, Entity entering,
          Coords dest, Entity transport, boolean climbMode, boolean ignoreHidden) {
        return stackingViolation(game, entering, entering.getElevation(), dest,
              transport, climbMode, ignoreHidden);
    }

    /**
     * Returns an entity if the specified entity would cause a stacking violation entering a hex, or returns null if it
     * would not. The returned entity is the entity causing the violation.
     * <p>
     * The position is derived from the passed Entity, while the elevation is derived from the passed Entity parameter.
     *
     * @param game      The Game instance
     * @param entering  The Entity entering the hex
     * @param elevation The elevation of the moving Entity
     * @param dest      The hex being entered
     * @param transport Represents the unit transporting entering, which may affect stacking, can be null
     * @param climbMode The moving Entity's climb mode at the point it enters the destination hex
     * @param ignoreHidden true by default.
     * @return Entity instance that is causing the violation
     */
    public static Entity stackingViolation(Game game, Entity entering,
          int elevation, Coords dest, Entity transport, boolean climbMode, boolean ignoreHidden) {
        return stackingViolation(game, entering, entering.getPosition(),
              elevation, dest, entering.getBoardId(), transport, climbMode, ignoreHidden);
    }

    /** Used by Princess / bots for checking deployment positions.
     *
     * @param game      The Game instance
     * @param entering  The Entity entering the hex
     * @param origPosition The coords of the hex the moving Entity is leaving
     * @param elevation The elevation of the moving Entity
     * @param dest      The hex being entered
     * @param transport Represents the unit transporting entering, which may affect stacking, can be null
     * @param climbMode The moving Entity's climb mode at the point it enters the destination hex
     * @param ignoreHidden true by default.
     * @return Entity instance that is causing the violation
     */
    public static Entity stackingViolation(Game game, Entity entering,
          Coords origPosition, int elevation, Coords dest, Entity transport, boolean climbMode, boolean ignoreHidden) {
        return stackingViolation(game, entering, origPosition,
              elevation, dest, entering.getBoardId(), transport, climbMode, ignoreHidden);
    }

    /**
     * Board-aware check used when compiling movepaths
     *
     * @param game      The Game instance
     * @param entering  The Entity entering the hex
     * @param elevation The elevation of the moving Entity
     * @param dest      The hex being entered
     * @param destBoardId Allows setting a different board for checking destination hex
     * @param transport Represents the unit transporting entering, which may affect stacking, can be null
     * @param climbMode The moving Entity's climb mode at the point it enters the destination hex
     * @param ignoreHidden true by default.
     * @return Entity instance that is causing the violation
     */
    public static Entity stackingViolation(Game game, Entity entering,
          int elevation, Coords dest, int destBoardId, Entity transport, boolean climbMode, boolean ignoreHidden) {
        return stackingViolation(game, entering, entering.getPosition(),
              elevation, dest, destBoardId, transport, climbMode, ignoreHidden);
    }

    /**
     * Returns an entity if the specified entity would cause a stacking violation entering a hex, or returns null if it
     * would not. The returned entity is the entity causing the violation.
     * <p>
     * The position and elevation is derived from the passed Entity parameter.
     *
     * @param game         The Game instance
     * @param entering     The Entity entering the hex
     * @param origPosition The coords of the hex the moving Entity is leaving
     * @param elevation    The elevation of the moving Entity
     * @param dest         The hex being entered
     * @param destBoardId Allows setting a different board for checking destination hex
     * @param transport    Represents the unit transporting entering, which may affect stacking, can be null
     * @param climbMode    The moving Entity's climb mode at the point it enters the destination hex
     * @param ignoreHidden true by default.
     * @return Entity instance that is causing the violation
     */
    public static Entity stackingViolation(Game game, Entity entering,
          Coords origPosition, int elevation, Coords dest, int destBoardId, Entity transport, boolean climbMode,
          boolean ignoreHidden) {
        // no stacking violations on low-atmosphere and space maps
        if (!game.getBoard(destBoardId).isGround()) {
            return null;
        }

        // no stacking violations for flying aerospace, except during deployment - no crushing units during deployment!
        if (entering.isAirborne() && !(game.getPhase().isDeployment() && (elevation == 0))) {
            return null;
        }

        // LAM in fighter mode shouldn't be treated as a Mek for all this
        boolean isMek =
              ((entering instanceof Mek) && !(entering instanceof LandAirMek lam && lam.getConversionMode() == LandAirMek.CONV_MODE_FIGHTER))
              || (entering instanceof SmallCraft);
        boolean isLargeSupport = (entering instanceof LargeSupportTank)
              || (entering instanceof Dropship)
              || ((entering instanceof Mek) && entering.isSuperHeavy());

        boolean isTrain = !entering.getAllTowedUnits().isEmpty();
        boolean isDropship = entering instanceof Dropship;
        boolean isInfantry = entering instanceof Infantry;
        Entity firstEntity = transport;
        int totalUnits = 1;
        Vector<Coords> positions = new Vector<>();
        positions.add(dest);
        if (isDropship) {
            positions.addAll(dest.allAdjacent());
        }
        Board board = game.getBoard(destBoardId);
        for (Coords coords : positions) {
            int thisLowStackingLevel = elevation;
            if ((coords != null) && (origPosition != null)) {
                thisLowStackingLevel = entering.calcElevation(
                      board.getHex(origPosition),
                      board.getHex(coords),
                      elevation, climbMode);
            }
            int thisHighStackingLevel = thisLowStackingLevel;
            // meks only occupy one level of a building
            if (!Compute.isInBuilding(game, entering, coords)) {
                thisHighStackingLevel += entering.height();
            }

            // remember a single small/medium trailer that may be ignored, TW p.57; this is also true when it is a
            // trailer or tractor that enters the hex
            boolean trailerToIgnore = (entering.isTrailer() || entering.isTractor()) && !entering.isSuperHeavy();

            // A train of small/medium units may take the room of more than one unit and must be considered together
            // *if* it is not yet in that hex
            List<Integer> towedUnits = entering.getAllTowedUnits();
            boolean isEnteringTrain = !entering.isSuperHeavy()
                  && !towedUnits.isEmpty()
                  && (coords != null)
                  && !coords.equals(origPosition);

            if (isEnteringTrain) {
                // a single or three trailers (in the hex with their tractor) take up the "ignore spots" of their
                // respective towing unit
                if ((towedUnits.size() == 1) || (towedUnits.size() >= 3)) {
                    trailerToIgnore = false;
                }
                // a train of three or more units takes up the room of two units when entering a hex
                if (towedUnits.size() > 1) {
                    totalUnits++;
                }
            }

            // Walk through the entities in the given hex.
            for (Entity inHex : game.getEntitiesVector(coords, destBoardId)) {

                if (inHex.isAirborne()) {
                    continue;
                }

                // We are not allowed to consider hidden units here!
                if (ignoreHidden && inHex.isHidden()) {
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

                    // One small/medium tractor and trailer or two such trailers are counted as a single unit. I'm
                    // making the assumption that it is not required that one is towing the other to allow forming
                    // and dissolving trains in a graceful way. TW, p.57
                    if (!inHex.isSuperHeavy() && !trailerToIgnore && (inHex.isTrailer() || inHex.isTractor())) {
                        trailerToIgnore = true;
                    } else if (inHex.isTrailer() && !inHex.isSuperHeavy() && trailerToIgnore) {
                        trailerToIgnore = false;
                        continue;
                    }

                    // DFA-ing units don't count towards stacking
                    if (inHex.isMakingDfa()) {
                        continue;
                    }

                    // If the entering entity is a mek, then any other mek in the hex is a violation. Unless grappled
                    // (but chain whip grapples don't count) grounded small craft are treated as meks for purposes
                    // of stacking. A LAM in fighter mode is not treated like a mek for this.
                    if (isMek
                          && ((((inHex instanceof Mek && !(entering instanceof LandAirMek lam
                          && lam.getConversionMode() == LandAirMek.CONV_MODE_FIGHTER))) && (inHex
                          .getGrappled() != entering.getId() || inHex
                          .isChainWhipGrappled()))
                          || (inHex instanceof SmallCraft))) {
                        return inHex;
                    }

                    // only inf can be in the same hex as a large support vee
                    // grounded dropships are treated as large support vees,
                    // ditto for superheavy meks
                    if (isLargeSupport && !(inHex instanceof Infantry)) {
                        return inHex;
                    }
                    if (((inHex instanceof LargeSupportTank)
                          || (inHex instanceof Dropship) || ((inHex instanceof Mek) && inHex
                          .isSuperHeavy()))
                          && !isInfantry) {
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
     * Returns true if there is any unit that is an enemy of the specified unit in the specified hex. This is only
     * called for stacking purposes, and so does not return true if the enemy unit is currently making a DFA.
     */
    public static boolean isEnemyIn(Game game, Entity entity, Coords coords,
          boolean onlyMeks, boolean ignoreInfantry, int enLowEl, boolean ignoreHidden) {
        int enHighEl = enLowEl + entity.getHeight();
        for (Entity inHex : game.getEntitiesVector(coords)) {
            // If we're ignoring hidden units and this one *is* hidden, pretend we don't see it.
            if (inHex.isHidden() && ignoreHidden) {
                continue;
            }

            int inHexAlt = inHex.getAltitude();
            boolean crewOnGround = (inHex instanceof EjectedCrew) && (inHexAlt == 0);
            int inHexEnLowEl = inHex.getElevation();
            int inHexEnHighEl = inHexEnLowEl + inHex.getHeight();
            if ((!onlyMeks || (inHex instanceof Mek))
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
        // It's possible to get a real ID for an entity we've forgotten (Double Blind, for instance).
        final Entity entity = game.getEntity(entityId);
        if (entity == null) {
            if (game.getEntityFromAllSources(entityId) == null) {
                // We have no recollection of the entity anywhere. At this point an error will be thrown.
                throw new IllegalArgumentException("Entity invalid. ID " + entityId);
            }

            // Otherwise, it's likely the unit has been destroyed prior to this point.
            return false;
        }

        Board board = game.getBoard(moveStep.getBoardId());
        final Hex srcHex = board.getHex(src);
        final Hex destHex = board.getHex(dest);
        final boolean isInfantry = (entity instanceof Infantry);
        int delta_alt = (destElevation + destHex.getLevel())
              - (srcElevation + srcHex.getLevel());

        // arguments valid?
        if (src.distance(dest) > 1) {
            throw new IllegalArgumentException("Coordinates must be adjacent.");
        }

        // let's only worry about actual movement, please
        if (src.equals(dest)) {
            return false;
        }

        // airborne aircraft do not require pavement-related checks
        final boolean isPavementStep = !entity.isAirborne() && Compute.canMoveOnPavement(srcHex, destHex, moveStep);

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
        boolean quadveeVehMode = entity instanceof QuadVee && entity.getConversionMode() == QuadVee.CONV_MODE_VEHICLE;
        boolean vehicleAffectedByCliff = entity instanceof Tank && !entity.isAirborneVTOLorWIGE();
        boolean mekAffectedByCliff = (entity instanceof Mek || entity instanceof ProtoMek)
              && movementType != EntityMovementType.MOVE_JUMP
              && !entity.isAero(); // LAM
        int stepHeight = destElevation + destHex.getLevel() - (srcElevation + srcHex.getLevel());
        // Cliffs should only exist towards 1 or 2 level drops, check just to make sure
        // Everything that does not have a 1 or 2 level drop shouldn't be handled as a
        // cliff
        boolean isUpCliff = !src.equals(dest)
              && destHex.hasCliffTopTowards(srcHex)
              && (stepHeight == 1 || stepHeight == 2);
        boolean isDownCliff = !src.equals(dest)
              && srcHex.hasCliffTopTowards(destHex)
              && (stepHeight == -1 || stepHeight == -2);

        // Meks and Vehicles moving down a cliff
        // QuadVees in vee mode ignore PSRs to avoid falls, IO p.133
        if ((mekAffectedByCliff || vehicleAffectedByCliff)
              && !quadveeVehMode
              && isDownCliff
              && !isPavementStep) {
            return true;
        }

        // Meks moving up a cliff
        if (mekAffectedByCliff
              && !quadveeVehMode
              && isUpCliff
              && !isPavementStep) {
            return true;
        }

        // Check for skid. Please note, the skid will be rolled on the
        // current step, but starts from the previous step's location.
        // TODO: add check for elevation of pavement, road, or bridge matches entity elevation.
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

        // If we're entering a building, all non-infantry
        // need to make a piloting check to avoid damage.
        if ((destElevation < destHex.terrainLevel(Terrains.BLDG_ELEV))
              && !(entity instanceof Infantry)) {
            IBuilding bldg = board.getBuildingAt(dest);
            boolean insideHangar = (null != bldg)
                  && bldg.isIn(src)
                  && (bldg.getBldgClass() == IBuilding.HANGAR)
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
              && destElevation > 0 && !(entity instanceof ProtoMek))) {
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
        return (entity instanceof Mek) && (delta_alt < -2)
              && (movementType != EntityMovementType.MOVE_JUMP
              && (movementType != EntityMovementType.MOVE_VTOL_WALK
              && (movementType != EntityMovementType.MOVE_VTOL_RUN)));
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
            return game.getOptions().booleanOption(OptionsConstants.BASE_PUSH_OFF_BOARD);
        }

        // can't be displaced into prohibited terrain
        // unless we're displacing a tracked or wheeled vee into water
        if (entity.isLocationProhibited(dest)
              && !((entity instanceof Tank)
              && destHex.containsTerrain(Terrains.WATER)
              && ((entity.getMovementMode() == EntityMovementMode.TRACKED)
              || (entity.getMovementMode() == EntityMovementMode.WHEELED)))) {
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
     * Gets a valid displacement, from the hexes around src, as close to the original direction as is possible.
     *
     * @return valid displacement coords, or null if none
     */
    public static Coords getValidDisplacement(Game game, int entityId,
          Coords src, int direction) {
        // check the surrounding hexes, nearest to the original direction first
        int[] offsets = { 0, 1, 5, 2, 4, 3 };
        int range = 1;
        // check for a central drop-ship hex and if so, then displace to a two
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
        // have fun being instant-killed!
        return null;
    }

    /**
     * Gets a preferred displacement. Right now this picks the surrounding hex with the same elevation as original hex,
     * if not available it picks the highest elevation that is a valid displacement. This will preferably not displace
     * into friendly units
     *
     * @return valid displacement coords, or null if none
     */
    public static Coords getPreferredDisplacement(Game game, int entityId, Coords src, int direction) {
        final Entity entity = game.getEntity(entityId);
        int highestElev = Integer.MIN_VALUE;

        if (entity == null) {
            return null;
        }

        Coords highest = null;
        int srcElevation = entity.elevationOccupied(game.getBoard().getHex(src));

        // check the surrounding hexes, nearest to the original direction first
        int[] offsets = { 0, 1, 5, 2, 4, 3 };
        // first, try not to displace into friendly units
        for (int offset : offsets) {
            Coords dest = src.translated((direction + offset) % 6);
            if (Compute.isValidDisplacement(game, entityId, src, dest) && game.getBoard().contains(dest)) {
                Iterator<Entity> entities = game.getFriendlyEntities(dest, game.getEntity(entityId));
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

        // ok, all hexes occupied, now displace preferably to same elevation, else highest
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
     * Gets a hex to displace a missed charge to. Picks left or right, first preferring higher hexes, then randomly, or
     * returns the base hex if they're impassible.
     */
    public static Coords getMissedChargeDisplacement(Game game, int entityId, Coords src, int direction) {
        Coords first = src.translated((direction + 1) % 6);
        Coords second = src.translated((direction + 5) % 6);
        Hex firstHex = game.getBoard().getHex(first);
        Hex secondHex = game.getBoard().getHex(second);
        Entity entity = game.getEntity(entityId);

        if (entity == null) {
            return null;
        }

        if ((firstHex == null) || (secondHex == null)) {
            // leave it, will be handled
        } else if (entity.elevationOccupied(firstHex) > entity.elevationOccupied(secondHex)) {
            // leave it
        } else if (entity.elevationOccupied(firstHex) < entity.elevationOccupied(secondHex)) {
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
     * Finds the best spotter for the attacker. The best spotter is the one with the lowest attack modifiers, of course.
     * LOS modifiers and movement are considered.
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
                // unless it has a command console or has TAG-ged the target
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
     *
     * @param target The non-entity target to check
     * @param game   The current {@link Game}
     *
     * @return Whether the given entity or other targetable is tagged.
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
     *
     * @param attacker The attacker.
     * @param target   The non-entity target to check
     * @param game     The current {@link Game}
     *
     * @return Whether the given entity or other targetable is tagged by the specific attacker.
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
     *
     * @param target     The target being considered for firing
     * @param aimingAt   The location of the unit being aimed at
     * @param aimingMode The aiming mode
     *
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
            if ((target instanceof Mek) && (aimingAt == Mek.LOC_HEAD) && aimingMode.isImmobile()) {
                return new ToHitData(3, "aiming at head");
            }
            ToHitData immobileTHD = new ToHitData(-4, "target immobile");
            if (target instanceof Tank targetTank) {
                // An "immobilized" but jumping CV is not actually immobile for targeting purposes (See issue #3917)
                return targetTank.moved == EntityMovementType.MOVE_JUMP ? null : immobileTHD;
            }
            return immobileTHD;
        }
        return null;
    }

    /**
     * Determines the to-hit modifier due to range for an attack with the specified parameters. Includes minimum range,
     * infantry 0-range mods, and target stealth mods. Accounts for friendly C3 units.
     *
     * @return the modifiers
     */
    public static ToHitData getRangeMods(Game game, Entity attackingEntity, WeaponMounted weapon, AmmoMounted ammo,
          Targetable target) {
        WeaponType weaponType = weapon.getType();
        int[] weaponRanges = weaponType.getRanges(weapon, ammo);
        boolean isAttackerInfantry = (attackingEntity instanceof Infantry);
        boolean isAttackerBA = (attackingEntity instanceof BattleArmor);
        boolean isWeaponInfantry = (weaponType instanceof InfantryWeapon) && !weaponType.hasFlag(WeaponType.F_TAG);
        boolean isSwarmOrLegAttack = (weaponType instanceof InfantryAttack);
        boolean isIndirect = weaponType.hasIndirectFire() && weapon.curMode().equals("Indirect");
        boolean useExtremeRange = game.getOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_RANGE);
        boolean useLOSRange = game.getOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_LOS_RANGE);
        // Naval C3 only provides full C3 range benefits to energy weapons and guided
        // missiles
        boolean nc3EnergyGuided = ((weaponType.hasFlag(WeaponType.F_ENERGY))
              || (weaponType.getAtClass() == WeaponType.CLASS_CAPITAL_MISSILE)
              || (weaponType.getAtClass() == WeaponType.CLASS_TELE_MISSILE)
              || (weaponType.getAtClass() == WeaponType.CLASS_AR10)
              || (weaponType.getAtClass() == WeaponType.CLASS_ATM)
              || (weaponType.getAtClass() == WeaponType.CLASS_LRM)
              || (weaponType.getAtClass() == WeaponType.CLASS_SRM)
              || (weaponType.getAtClass() == WeaponType.CLASS_MML)
              || (weaponType.getAtClass() == WeaponType.CLASS_THUNDERBOLT));

        if (attackingEntity.isAirborne()) {
            useExtremeRange = true;
            // This is a separate SO rule, and isn't implemented yet
            useLOSRange = false;
        }

        ToHitData mods = new ToHitData();

        Entity targetEntity = null;
        if (target instanceof Entity te) {
            targetEntity = te;
        }

        // No _standard_ range mods for Artillery Flak or ADA vs airborne Aerospace or VTOL/WiGE
        if (weaponType.hasFlag(WeaponType.F_ARTILLERY) && (target.isAirborne() || target.isAirborneVTOLorWIGE())) {
            return mods;
        }

        // We need to adjust the ranges for Centurion Weapon Systems: it's
        // default range is 6/12/18 but that's only for units that are
        // susceptible to CWS, for those that aren't the ranges are 1/2/3
        if (weaponType.hasFlag(WeaponType.F_CWS) && ((targetEntity == null)
              || !targetEntity.hasQuirk("susceptible_cws"))) {
            weaponRanges[RangeType.RANGE_MINIMUM] = 0;
            weaponRanges[RangeType.RANGE_SHORT] = 1;
            weaponRanges[RangeType.RANGE_MEDIUM] = 2;
            weaponRanges[RangeType.RANGE_LONG] = 3;
            weaponRanges[RangeType.RANGE_EXTREME] = 4;
        }

        //
        // modify the ranges for PPCs when field inhibitors are turned off
        // TODO: See above, it should be coded elsewhere...
        //
        if (weaponType.hasFlag(WeaponType.F_PPC)) {
            if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_PPC_INHIBITORS)) {
                if ((weapon.curMode() != null)
                      && weapon.curMode().equals("Field Inhibitor OFF")) {
                    weaponRanges[RangeType.RANGE_MINIMUM] = 0;
                }
            }
        }

        // Hot loaded weapons
        if (weapon.isHotLoaded() && game.getOptions()
              .booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_HOT_LOAD)) {
            weaponRanges[RangeType.RANGE_MINIMUM] = 0;
        }

        // is water involved?
        Hex targHex = game.getHexOf(target);
        int targTop = target.relHeight();
        int targBottom = target.getElevation();

        boolean targetInPartialWater = false;
        boolean targetUnderwater = false;
        boolean weaponUnderwater = (attackingEntity.getLocationStatus(weapon.getLocation())
              == ILocationExposureStatus.WET);
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
        if ((null != targetEntity) && (targBottom == 0) && (targetEntity.getUnitType() == UnitType.NAVAL)) {
            targetInPartialWater = true;
        }

        // allow naval units to target underwater units,
        // torpedo tubes are mounted underwater
        if ((targetUnderwater || (weaponType.getAmmoType() == AmmoTypeEnum.LRM_TORPEDO) ||
              (weaponType.getAmmoType() == AmmoTypeEnum.SRM_TORPEDO))
              && (attackingEntity.getUnitType() == UnitType.NAVAL)) {
            weaponUnderwater = true;
            weaponRanges = weaponType.getWRanges();
        }

        // allow ice to be cleared from below
        if ((targHex != null) && targHex.containsTerrain(Terrains.WATER)
              && (target.getTargetType() == Targetable.TYPE_HEX_CLEAR)) {
            targetInPartialWater = true;
        }

        if (weaponUnderwater) {
            weaponRanges = weaponType.getWRanges();
            boolean MPM = false;
            if ((weaponType.getAmmoType() == AmmoTypeEnum.SRM)
                  || (weaponType.getAmmoType() == AmmoTypeEnum.SRM_IMP)
                  || (weaponType.getAmmoType() == AmmoTypeEnum.MRM)
                  || (weaponType.getAmmoType() == AmmoTypeEnum.LRM)
                  || (weaponType.getAmmoType() == AmmoTypeEnum.LRM_IMP)
                  || (weaponType.getAmmoType() == AmmoTypeEnum.MML)) {
                AmmoType ammoType = (AmmoType) weapon.getLinked().getType();
                if (ammoType.getMunitionType().contains(AmmoType.Munitions.M_TORPEDO)) {
                    weaponRanges = weaponType.getRanges(weapon);
                } else if (ammoType.getMunitionType().contains(AmmoType.Munitions.M_MULTI_PURPOSE)) {
                    weaponRanges = weaponType.getRanges(weapon);
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
            // special case: meks can only fire upper body weapons at surface
            // naval
            if ((targetEntity != null)
                  && (targetEntity.getUnitType() == UnitType.NAVAL)
                  && (attackingEntity instanceof Mek) && (attackingEntity.height() > 0)
                  && (attackingEntity.getElevation() == -1)) {
                return new ToHitData(TargetRoll.IMPOSSIBLE,
                      "Partially submerged mek cannot fire leg weapons at surface naval vessels.");
            }
        } else if (targetUnderwater) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Target underwater, but not weapon.");
        } else if ((weaponType.getAmmoType() == AmmoTypeEnum.LRM_TORPEDO)
              || (weaponType.getAmmoType() == AmmoTypeEnum.SRM_TORPEDO)) {
            // Torpedoes only fire underwater.
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Weapon can only fire underwater.");
        }

        // if Aero then adjust to standard ranges
        if (attackingEntity.isAero() && (attackingEntity.isAirborne()
              || (attackingEntity.usesWeaponBays() && game.isOnGroundMap(attackingEntity)))) {
            weaponRanges = weaponType.getATRanges();
        }
        // And if you're using bearings-only capital missiles, update the extreme range
        if (weapon.isInBearingsOnlyMode()) {
            weaponRanges = new int[] { Integer.MIN_VALUE, 12, 24, 40, RangeType.RANGE_BEARINGS_ONLY_OUT };
        }

        // determine base distance & range bracket
        int distance = effectiveWeaponDistance(game, attackingEntity, weapon, target);
        int range = RangeType.rangeBracket(distance, weaponRanges, useExtremeRange, useLOSRange);

        // Additional checks for LOS range and some weapon types, TO 85
        if (range == RangeType.RANGE_LOS) {
            // Swarm or leg attacks can't use LoS range
            if (isSwarmOrLegAttack) {
                range = RangeType.RANGE_OUT;
            }

            // MGs lack range for LOS Range, but don't have F_DIRECT_FIRE flag
            if (weaponType instanceof MGWeapon) {
                range = RangeType.RANGE_OUT;
            }

            // AMS lack range for LOS Range, but don't have F_DIRECT_FIRE flag
            if (weaponType.hasFlag(WeaponType.F_AMS)) {
                range = RangeType.RANGE_OUT;
            }

            // Flamers lack range for LOS Range, but don't have F_DIRECT_FIRE
            if (weaponType.hasFlag(WeaponType.F_FLAMER)) {
                range = RangeType.RANGE_OUT;
            }

            int longRange = weaponType.getRanges(weapon)[RangeType.RANGE_LONG];
            // No Missiles or Direct Fire Ballistics with range < 13
            if (weaponType.hasFlag(WeaponType.F_MISSILE) || (weaponType.hasFlag(WeaponType.F_DIRECT_FIRE)
                  && weaponType.hasFlag(WeaponType.F_BALLISTIC))) {
                if (longRange < 13) {
                    range = RangeType.RANGE_OUT;
                }
            }
            // No Direct Fire Energy or Pulse with range < 7
            if (weaponType.hasFlag(WeaponType.F_PULSE) || (weaponType.hasFlag(WeaponType.F_ENERGY)
                  && weaponType.hasFlag(WeaponType.F_DIRECT_FIRE))) {
                if (longRange < 7) {
                    range = RangeType.RANGE_OUT;
                }
            }
        }
        int maxRange = weaponType.getMaxRange(weapon, ammo);

        // if aero and greater than max range then switch to range_out
        if ((attackingEntity.isAirborne() || (attackingEntity.usesWeaponBays() && game.isOnGroundMap(attackingEntity)))
              && (range > maxRange)) {
            range = RangeType.RANGE_OUT;
        }

        // Swarm/Leg attacks need to be impossible, not auto-fail, so that the
        // attack can't even be attempted
        if (isSwarmOrLegAttack && (distance > 0)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Swarm/Leg attacks can only target units in the same hex!");
        }
        // short circuit if at zero range or out of range
        if ((range == RangeType.RANGE_OUT) && !isWeaponInfantry) {
            return new ToHitData(TargetRoll.AUTOMATIC_FAIL, "Target out of range");
        }

        // Infantry with infantry weapons (rifles, etc., i.e. not field pieces)
        // and BattleArmor can fire at zero range, among other things
        if ((distance == 0)
              && (!isAttackerInfantry || !(isWeaponInfantry || isSwarmOrLegAttack || isAttackerBA))
              && !(attackingEntity.isAirborne())
              && !(attackingEntity.isBomber() && ((IBomber) attackingEntity).isVTOLBombing())
              && !((attackingEntity instanceof Dropship) && attackingEntity.isSpheroid()
              && !attackingEntity.isAirborne() && !attackingEntity.isSpaceborne())
              && !((attackingEntity instanceof Mek) && (attackingEntity.getGrappled() == target.getId()))) {
            return new ToHitData(TargetRoll.AUTOMATIC_FAIL, "Only infantry weapons shoot at zero range");
        }

        // Account for "dead zones" between Aeros at different altitudes
        if (!Compute.useSpheroidAtmosphere(game, attackingEntity) && Compute.inDeadZone(game,
              attackingEntity,
              target)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Target in dead zone");
        }

        // find any c3 spotters that could help
        Entity c3spotter = ComputeC3Spotter.findC3Spotter(game, attackingEntity, target);
        Entity c3spotterWithECM = ComputeC3Spotter.playtestFindC3Spotter(game, attackingEntity, target);

        if (isIndirect) {
            c3spotter = attackingEntity; // no c3 when using indirect fire
        }

        if (isIndirect && indirectAttackImpossible(game, attackingEntity, target, weaponType, weapon)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, Messages.getString("WeaponAttackAction.NoIndirectWithLOS"));
        }

        int c3dist = Compute.effectiveDistance(game, c3spotter, target, false);
        // PLAYTEST3 if there is a member that is ECM blocked
        int c3ecmDist = Compute.effectiveDistance(game, c3spotterWithECM, target, false);

        // C3 can't benefit from LOS range.
        int c3range = RangeType.rangeBracketC3(c3dist, distance, weaponRanges, useExtremeRange, false);
        // PLAYTEST3 checking for ECM ranged member
        int c3ecmRange = RangeType.rangeBracketC3(c3ecmDist, distance, weaponRanges, useExtremeRange, false);

        /*
         * Tac Ops Extreme Range Rule p. 85 if the weapons normal range is
         * Extreme then C3 uses the next highest range bracket, i.e. medium
         * instead of short.
         */
        if ((range == RangeType.RANGE_EXTREME) && (c3range < range || c3ecmRange < range)) {
            c3range++;
            c3ecmRange++;
        }

        // determine which range we're using
        int usingRange = range;
        boolean usingC3 = false;

        if (game.getOptions().booleanOption(OptionsConstants.PLAYTEST_3)) {
            // PLAYTEST3 check ecm vs non ecm affected C3
            if ((c3range > c3ecmRange) && (c3range > range)) {
                usingRange = c3ecmRange;
                usingC3 = true;
            } else if (range > c3range) {
                usingRange = c3range;
                usingC3 = true;
            }
        } else {
            usingRange = min(range, c3range);
            if (usingRange == c3range && range > c3range) {
                usingC3 = true;
            }
        }

        // add range modifier, C3 can't be used with LOS Range
        if (((usingRange == range) && !usingC3) || (range == RangeType.RANGE_LOS) || (attackingEntity.hasNavalC3()
              && !nc3EnergyGuided)) {
            // Ensure usingRange is set to range, ie with C3
            usingRange = range;
            // Naval C3 adjustment for ballistic and unguided weapons
            if ((attackingEntity.hasNavalC3() && !nc3EnergyGuided) && (c3range < range)) {
                if (((range == RangeType.RANGE_SHORT) || (range == RangeType.RANGE_MINIMUM))
                      && (attackingEntity.getShortRangeModifier() != 0)) {
                    mods.addModifier((attackingEntity.getShortRangeModifier() / 2), "NC3 modified short range");
                } else if (range == RangeType.RANGE_MEDIUM) {
                    mods.addModifier((attackingEntity.getMediumRangeModifier() / 2), "NC3 modified medium range");
                } else if (range == RangeType.RANGE_LONG) {
                    mods.addModifier((attackingEntity.getLongRangeModifier() / 2), "NC3 modified long range");
                } else if (range == RangeType.RANGE_EXTREME) {
                    mods.addModifier((attackingEntity.getExtremeRangeModifier() / 2), "NC3 modified Extreme range");
                }
            } else {
                // no c3 adjustment
                if (((range == RangeType.RANGE_SHORT) || (range == RangeType.RANGE_MINIMUM))
                      && (attackingEntity.getShortRangeModifier() != 0)) {
                    mods.addModifier(attackingEntity.getShortRangeModifier(), "short range");
                } else if (range == RangeType.RANGE_MEDIUM) {
                    // Right now, the range-mod affecting targeting systems DON'T
                    // affect medium range, so we won't add that here ever.
                    mods.addModifier(attackingEntity.getMediumRangeModifier(), "medium range");
                } else if (range == RangeType.RANGE_LONG) {
                    // ProtoMeks that loose head sensors can't shoot long range.
                    if ((attackingEntity instanceof ProtoMek protoMek)
                          && (2 == protoMek.getCritsHit(ProtoMek.LOC_HEAD))) {
                        mods.addModifier(TargetRoll.IMPOSSIBLE, "No long range attacks with destroyed head sensors.");
                    } else {
                        mods.addModifier(attackingEntity.getLongRangeModifier(), "long range");
                    }
                } else if (range == RangeType.RANGE_EXTREME) {
                    // ProtoMeks that loose head sensors can't shoot extreme range.
                    if ((attackingEntity instanceof ProtoMek protoMek)
                          && (2 == protoMek.getCritsHit(ProtoMek.LOC_HEAD))) {
                        mods.addModifier(TargetRoll.IMPOSSIBLE,
                              "No extreme range attacks with destroyed head sensors.");
                    } else {
                        mods.addModifier(attackingEntity.getExtremeRangeModifier(), "extreme range");
                    }
                } else if (range == RangeType.RANGE_LOS) {
                    // ProtoMeks that loose head sensors can't shoot LOS range.
                    if ((attackingEntity instanceof ProtoMek protoMek)
                          && (2 == protoMek.getCritsHit(ProtoMek.LOC_HEAD))) {
                        mods.addModifier(TargetRoll.IMPOSSIBLE, "No LOS range attacks with destroyed head sensors.");
                    } else {
                        mods.addModifier(attackingEntity.getLOSRangeModifier(), "LOS range");
                    }
                }
            }
        } else {
            // report c3 adjustment
            // PLAYTEST3 C3 ECM halving
            if (game.getOptions().booleanOption(OptionsConstants.PLAYTEST_3)
                  && usingRange == c3ecmRange
                  && usingRange != c3range
                  && c3spotterWithECM.getC3ecmAffected()) {
                // Halve the bonus, so we need to know what the original range was too.
                int rangeModifier = 0;
                if (range == RangeType.RANGE_LONG) {
                    rangeModifier = attackingEntity.getLongRangeModifier();
                } else if (range == RangeType.RANGE_MEDIUM) {
                    rangeModifier = attackingEntity.getMediumRangeModifier();
                } else if (range == RangeType.RANGE_EXTREME) {
                    rangeModifier = attackingEntity.getExtremeRangeModifier();
                }
                if ((c3ecmRange == RangeType.RANGE_SHORT) || (c3ecmRange == RangeType.RANGE_MINIMUM)) {
                    rangeModifier = (int) (rangeModifier + attackingEntity.getShortRangeModifier()) / 2;
                    mods.addModifier(rangeModifier, "short range due to C3 spotter under ECM");
                } else if (c3ecmRange == RangeType.RANGE_MEDIUM) {
                    rangeModifier = (int) (rangeModifier + attackingEntity.getMediumRangeModifier()) / 2;
                    mods.addModifier(rangeModifier, "medium range due to C3 spotter under ECM");
                } else if (c3ecmRange == RangeType.RANGE_LONG) {
                    rangeModifier = (int) (rangeModifier + attackingEntity.getLongRangeModifier()) / 2;
                    mods.addModifier(rangeModifier, "long range due to C3 spotter under ECM");
                }
            } else {
                // Normal C3 operation, no ECM
                if ((c3range == RangeType.RANGE_SHORT) || (c3range == RangeType.RANGE_MINIMUM)) {
                    mods.addModifier(attackingEntity.getShortRangeModifier(), "short range due to C3 spotter");
                } else if (c3range == RangeType.RANGE_MEDIUM) {
                    mods.addModifier(attackingEntity.getMediumRangeModifier(), "medium range due to C3 spotter");
                } else if (c3range == RangeType.RANGE_LONG) {
                    mods.addModifier(attackingEntity.getLongRangeModifier(), "long range due to C3 spotter");
                }
            }
        }

        // Variable Range Targeting quirk modifier (BMM pg. 86)
        // Shows as a separate line item in the to-hit breakdown
        int vrtModifier = attackingEntity.getVariableRangeTargetingModifier(usingRange);
        if (vrtModifier != 0) {
            String vrtMode = attackingEntity.getVariableRangeTargetingMode().isShort()
                  ? Messages.getString("Compute.VariableRangeTargetingShort")
                  : Messages.getString("Compute.VariableRangeTargetingLong");
            mods.addModifier(vrtModifier, vrtMode);
        }

        // add minimum range modifier (only for ground-to-ground attacks)
        int minRange = weaponRanges[RangeType.RANGE_MINIMUM];
        if ((minRange > 0) && (distance <= minRange) && Compute.isGroundToGround(attackingEntity, target)) {
            int minPenalty = (minRange - distance) + 1;
            mods.addModifier(minPenalty, "minimum range");
        }

        // if this is an infantry weapon then we use a whole different
        // calculation
        // to figure out range, so overwrite whatever we have at this point
        if (isWeaponInfantry) {
            mods = Compute.getInfantryRangeMods(min(distance, c3dist),
                  (InfantryWeapon) weaponType,
                  (attackingEntity instanceof Infantry) ? ((Infantry) attackingEntity).getSecondaryWeapon() : null,
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
        if (target instanceof Entity entity) {
            TargetRoll tmpTR = entity.getStealthModifier(usingRange, attackingEntity);
            if ((tmpTR != null) && (tmpTR.getValue() != 0)) {
                mods.append(entity.getStealthModifier(usingRange, attackingEntity));
            }
        }

        return mods;
    }

    /**
     * Calculate the range modifiers for a conventional infantry attack.
     *
     * @param distance        - range to target
     * @param primaryWeapon   - the weapon used to calculate range -- secondaryWeapon if 2/squad, otherwise primary
     * @param secondaryWeapon - the secondaryWeapon weapon, if any. Range zero penalties apply even if primary is used
     *                        for range
     * @param underwater      - underwater range is half, rounded down
     *
     * @return - all modifiers for range
     */
    public static ToHitData getInfantryRangeMods(int distance, InfantryWeapon primaryWeapon,
          InfantryWeapon secondaryWeapon, boolean underwater) {
        ToHitData mods = new ToHitData();
        int range = primaryWeapon.getInfantryRange();
        if (underwater) {
            range /= 2;
        }
        int mod = 0;

        switch (range) {
            case 0:
                if (distance > 0) {
                    return new ToHitData(TargetRoll.AUTOMATIC_FAIL, "Target out of range");
                }
                break;
            case 1:
                if (distance > 3) {
                    return new ToHitData(TargetRoll.AUTOMATIC_FAIL, "Target out of range");
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
                    return new ToHitData(TargetRoll.AUTOMATIC_FAIL, "Target out of range");
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
                    return new ToHitData(TargetRoll.AUTOMATIC_FAIL, "Target out of range");
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
                    return new ToHitData(TargetRoll.AUTOMATIC_FAIL, "Target out of range");
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
                    return new ToHitData(TargetRoll.AUTOMATIC_FAIL, "Target out of range");
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
                    return new ToHitData(TargetRoll.AUTOMATIC_FAIL, "Target out of range");
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
                    return new ToHitData(TargetRoll.AUTOMATIC_FAIL, "Target out of range");
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
                return new ToHitData(TargetRoll.AUTOMATIC_FAIL, "Target out of range");
        }

        // a bunch of special conditions at range 0 penalties due to point-blank or encumbering apply for
        // secondaryWeapon weapon even if primary is used to determine range
        if (distance == 0) {

            if (primaryWeapon.hasFlag(WeaponType.F_INF_POINT_BLANK)
                  || (secondaryWeapon != null && secondaryWeapon.hasFlag(WeaponType.F_INF_POINT_BLANK))) {
                mods.addModifier(1, "point blank weapon");
            }
            if (primaryWeapon.hasFlag(WeaponType.F_INF_ENCUMBER) || (primaryWeapon.getCrew() > 1)
                  || (secondaryWeapon != null
                  && (secondaryWeapon.hasFlag(WeaponType.F_INF_ENCUMBER)
                  || secondaryWeapon.getCrew() > 1))) {
                mods.addModifier(1, "point blank support weapon");
            }

            if (primaryWeapon.hasFlag(WeaponType.F_INF_BURST)) {
                mods.addModifier(-1, "point blank burst fire weapon");
            }
        }

        // TODO: we need to adjust for stealth modifiers for Chameleon LPS but we don't have range brackets
        // http://bg.battletech.com/forums/index.php/topic,27433.new.html#new

        if (mod != 0) {
            mods.addModifier(mod, "infantry range");
        }

        return mods;
    }

    /**
     * Finds the effective distance between an attacker and a target. Includes the distance bonus if the attacker and
     * target are in the same building and on different levels. Also takes account of altitude differences
     *
     * @return the effective distance
     */
    /**
     * Calculates effective distance from a weapon's firing position to a target, accounting for altitude differences
     * and same-building elevation modifiers. This is used for entities with multiple firing positions like
     * BuildingEntity.
     *
     * @param game         The current game
     * @param weaponEntity The entity with the weapon
     * @param weapon       The weapon being fired
     * @param target       The target being attacked
     *
     * @return The effective distance from the weapon's firing position to the target
     */
    public static int effectiveWeaponDistance(final Game game, final Entity weaponEntity,
          final WeaponMounted weapon, final Targetable target) {
        Coords weaponFiringPos = weaponEntity.getWeaponFiringPosition(weapon);

        if (weaponFiringPos != null && !weaponFiringPos.equals(weaponEntity.getPosition())) {
            // For entities with multiple firing positions (BuildingEntity, etc.),
            // calculate distance from the weapon's actual firing position
            int distance = weaponFiringPos.distance(target.getPosition());

            // If attack is inside same building, add elevation difference
            if (isInSameBuilding(game, weaponEntity, target)) {
                int aElev = weaponEntity.getElevation();
                int tElev = target.getElevation();
                distance += Math.abs(aElev - tElev);
            }

            // Air-to-air altitude differences
            if (isAirToAir(game, weaponEntity, target) && !weaponEntity.isSpaceborne()) {
                int aAlt = weaponEntity.getAltitude();
                int tAlt = target.getAltitude();
                if (target.isAirborneVTOLorWIGE()) {
                    tAlt++;
                }
                distance += Math.abs(aAlt - tAlt);
            }

            // Ground-to-air altitude adjustments
            if (isGroundToAir(weaponEntity, target)) {
                if (weaponEntity.usesWeaponBays() && game.getBoard().isGround()) {
                    distance += target.getAltitude();
                } else {
                    distance += (2 * target.getAltitude());
                }
            }

            // Attacking ground unit while dropping
            if (weaponEntity.isDropping() && target.getAltitude() == 0) {
                distance += (2 * weaponEntity.getAltitude());
            }

            return distance;
        } else {
            // Use standard effective distance calculation
            return effectiveDistance(game, weaponEntity, target, false);
        }
    }

    public static int effectiveDistance(final Game game, final Entity attacker,
          final @Nullable Targetable target) {
        return Compute.effectiveDistance(game, attacker, target, false);
    }

    /**
     * Finds the effective distance between an attacker and a target. Includes the distance bonus if the attacker and
     * target are in the same building and on different levels. Also takes account of altitude differences
     *
     * @return the effective distance
     */
    public static int effectiveDistance(final Game game, final Entity attacker,
          final @Nullable Targetable target,
          final boolean useGroundDistance) {
        if (target == null) {
            LOGGER.error("Attempted to determine the effective distance to a null target");
            return 0;
        } else if (isAirToGround(attacker, target)
              || (attacker.isBomber() && target.getTargetType() == Targetable.TYPE_HEX_AERO_BOMB)) {
            // always a distance of zero
            return 0;
        } else if (!game.onConnectedBoards(attacker, target)) {
            return Integer.MAX_VALUE;
        }

        Vector<Coords> attackPos = new Vector<>();
        attackPos.add(attacker.getPosition());
        Vector<Coords> targetPos = new Vector<>();

        if (target instanceof BuildingEntity) {
            targetPos.addAll(target.getSecondaryPositions().values());
        } else {
            targetPos.add(target.getPosition());
        }

        if (CrossBoardAttackHelper.isOrbitToSurface(game, attacker, target)) {
            // The effective position of the target is the ground map position on the ground hex row of the high
            // altitude map that the attacker is firing from
            targetPos.clear();
            int atmosphericBoardId = BoardHelper.enclosingBoardId(game, target.getBoardLocation());
            targetPos.add(BoardHelper.positionOnEnclosingBoard(game, atmosphericBoardId));
        } else if (isAirToAir(game, attacker, target)) {
            // In A2A attacks between different maps (only ground/ground, ground/atmo or atmo/ground), replace the
            // position of the unit on the ground map with the position of the ground map itself in the atmo map
            if (game.isOnGroundMap(attacker) && game.isOnAtmosphericMap(target)) {
                attackPos.clear();
                attackPos.add(BoardHelper.positionOnEnclosingBoard(game, attacker.getBoardId()));

            } else if (game.isOnAtmosphericMap(attacker) && game.isOnGroundMap(target)) {
                targetPos.clear();
                targetPos.add(BoardHelper.positionOnEnclosingBoard(game, target.getBoardId()));

            } else if (BoardHelper.onDifferentGroundMaps(game, attacker, target)) {
                // Different ground maps, here replace both positions with their respective atmo map hexes
                attackPos.clear();
                attackPos.add(BoardHelper.positionOnEnclosingBoard(game, attacker.getBoardId()));
                targetPos.clear();
                targetPos.add(BoardHelper.positionOnEnclosingBoard(game, target.getBoardId()));
            }
        } else if (!game.onTheSameBoard(attacker, target)
              && game.isOnGroundMap(attacker)
              && game.isOnGroundMap(target)) {
            // S2S attacks (either artillery or capital weapons) need the ground distance instead of the atmo distance
            return CrossBoardAttackHelper.getCrossBoardGroundMapDistance(attacker, target, game);
        }

        // if a grounded drop-ship is the attacker, then it gets to choose the
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
        int distance = smallestDistance(attackPos, targetPos);

        if (isGroundToAir(attacker, target) && (target instanceof Entity)) {
            // distance is determined by closest point on flight path
            distance = attacker.getPosition().distance(getClosestFlightPath(attacker.getId(),
                  attacker.getPosition(), (Entity) target));

            // if the ground attacker uses weapon bays, and we are on a ground map, then we will divide this distance
            // by 16. This is totally crazy, but I don't see how else to do it. Use the unofficial "grounded
            // dropships use individual weapons" for sanity.
            if (attacker.usesWeaponBays() && game.getBoard().isGround()) {
                distance = (int) Math.ceil(distance / 16.0);
            }
        }

        // if this is an air-to-air attack on the ground map, then divide distance by 16
        if (isAirToAir(game, attacker, target) && game.isOnGroundMap(attacker)
              && game.onTheSameBoard(attacker, target) && !useGroundDistance) {
            distance = (int) Math.ceil(distance / 16.0);
        }

        // If the attack is completely inside a building, add the difference in elevations between the attacker and
        // target to the range.
        // TODO: should the player be explicitly notified?
        if (isInSameBuilding(game, attacker, target)) {
            int aElev = attacker.getElevation();
            int tElev = target.getElevation();
            distance += Math.abs(aElev - tElev);
        }

        // air-to-air attacks add one for altitude differences
        if (isAirToAir(game, attacker, target) && !attacker.isSpaceborne()) {
            int aAlt = attacker.getAltitude();
            int tAlt = target.getAltitude();
            if (target.isAirborneVTOLorWIGE()) { //FIXME VTOLs cannot be A2A
                tAlt++;
            }
            distance += Math.abs(aAlt - tAlt);
        }

        if (isGroundToAir(attacker, target)) {
            if (attacker.usesWeaponBays() && game.getBoard().isGround()) {
                distance += (target.getAltitude());
            } else {
                distance += (2 * target.getAltitude());
            }
        }

        // Attacking a ground unit while dropping
        if (attacker.isDropping() && target.getAltitude() == 0) {
            distance += (2 * attacker.getAltitude());
        }

        if (game.isOnSpaceMap(attacker) && !attacker.getPosition().equals(targetPos.get(0))) {
            // Atmospheric hexes count as extra range
            Board attackerBoard = game.getBoard(attacker);
            Coords currentCoords = attacker.getPosition();
            currentCoords = Coords.nextHex(currentCoords, targetPos.get(0));
            int safetyCounter = 0;
            while (!currentCoords.equals(targetPos.get(0)) && (safetyCounter < 1000)) {
                safetyCounter++; // prevent infinite loops
                currentCoords = Coords.nextHex(currentCoords, targetPos.get(0));
                if (BoardHelper.isAtmosphericRow(game, attackerBoard, currentCoords)
                      || BoardHelper.isGroundRowHex(attackerBoard, currentCoords)) {
                    distance += BoardHelper.highAltAtmosphereRowRangeIncrease(game);
                } else if (BoardHelper.isSpaceAtmosphereInterface(game, attackerBoard, currentCoords)) {
                    distance += BoardHelper.highAltSpaceAtmosphereRangeIncrease(game);
                }
            }
        }

        return distance;
    }

    static int smallestDistance(Collection<Coords> firstList, Collection<Coords> secondList) {
        int distance = Integer.MAX_VALUE;
        for (Coords first : firstList) {
            for (Coords second : secondList) {
                if ((second != null) && (first != null) && (first.distance(second) < distance)) {
                    distance = first.distance(second);
                }
            }
        }
        return distance;
    }

    /**
     * @param aPos         the attacker's position
     * @param targetEntity the target entity
     *
     * @return the closest position along <code>targetEntity</code>'s flight path to <code>aPos</code>. In the case of
     *       multiple equidistance positions, the first one is picked unless <code>targetEntity</code>'s
     *       playerPickedPassThrough position is non-null.
     */
    public static @Nullable Coords getClosestFlightPath(int attackerId, Coords aPos, Entity targetEntity) {
        Coords finalPos = targetEntity.getPosition();
        if (targetEntity.getPlayerPickedPassThrough(attackerId) != null) {
            finalPos = targetEntity.getPlayerPickedPassThrough(attackerId);
        }
        int distance = Integer.MAX_VALUE;
        if (finalPos != null) {
            distance = aPos.distance(finalPos);
        }
        // don't return zero distance Coords, but rather the Coords immediately
        // before this.
        // This is necessary to determine angle of attack and arc information
        // for direct fly-overs
        for (Coords c : targetEntity.getPassedThrough()) {
            if (!aPos.equals(c) && (c != null)
                  && ((aPos.distance(c) < distance) || (distance == 0))) {
                finalPos = c;
                distance = aPos.distance(c);
            }
        }
        return finalPos;
    }

    public static int getClosestFlightPathFacing(int attackerId, Coords aPos, Entity targetEntity) {

        Coords finalPos = targetEntity.getPosition();
        if (targetEntity.getPlayerPickedPassThrough(attackerId) != null) {
            finalPos = targetEntity.getPlayerPickedPassThrough(attackerId);
        }
        int distance = Integer.MAX_VALUE;
        if (finalPos != null) {
            distance = aPos.distance(finalPos);
        }
        int finalFacing = targetEntity.getFacing();
        // don't return zero distance Coords, but rather the Coords immediately
        // before this.
        // This is necessary to determine angle of attack and arc information
        // for direct fly-overs
        for (int i = 0; i < targetEntity.getPassedThrough().size(); i++) {
            Coords c = targetEntity.getPassedThrough().get(i);
            if (!aPos.equals(c) && (c != null)
                  && ((aPos.distance(c) < distance) || (distance == 0))) {
                finalFacing = targetEntity.getPassedThroughFacing().get(i);
                finalPos = c;
                distance = aPos.distance(c);
            } else if (c.equals(finalPos)) {
                finalFacing = targetEntity.getPassedThroughFacing().get(i);
            }
        }
        return finalFacing;
    }

    /**
     * @param flyingEntity   the flyer
     * @param targetPosition target
     *
     * @return the closest position along <code>flyingEntity</code>'s flight path to <code>targetPosition</code>. In the
     *       case of multiple equidistance positions, the first one is picked.
     */
    public static @Nullable Coords getClosestToFlightPath(Entity flyingEntity, Coords targetPosition) {
        Coords flyerPosition = flyingEntity.getPosition();
        Coords finalPos = flyerPosition;
        int distance = Integer.MAX_VALUE;
        if (finalPos != null) {
            distance = flyerPosition.distance(finalPos);
        }
        for (Coords coord : flyingEntity.getPassedThrough()) {
            if ((coord != null)
                  && ((coord.distance(targetPosition) < distance) || (distance == 0))) {
                finalPos = coord;
                distance = coord.distance(targetPosition);
            }
        }
        return finalPos;
    }

    /**
     * WOR: Need this function to find out where my nova stuff doesn't work. Delete it if nova works but remember to
     * alter the /nova debug server command.
     */
    @Deprecated
    public static Entity exposed_findC3Spotter(Game game, Entity attacker, Targetable target) {
        return ComputeC3Spotter.findC3Spotter(game, attacker, target);
    }

    /**
     * Function that attempts to find one TAG spotter, or the best TAG spotter,
     *
     * @param attacker    should be an artillery unit
     * @param target      (if null, do not return a spotter!)
     * @param stopAtFirst if we only want to know that there is one, not which is best
     *
     * @return The Spotter.
     */
    public static Entity findTAGSpotter(Game game, Entity attacker, Targetable target, boolean stopAtFirst) {
        if (target == null) {
            return null;
        }
        StringBuilder msg = new StringBuilder("Looking for TAG spotter for ")
              .append(attacker.getDisplayName())
              .append(" targeting ")
              .append(target.getDisplayName());

        Entity spotter = null;
        int distance = -1;

        // Compute friendly spotters
        for (Entity friend : game.getPlayerEntities(attacker.getOwner(), true)) {

            if (friend == null
                  || !friend.isDeployed()
                  || friend.isOffBoard()
                  || (friend.getTransportId() != Entity.NONE)
                  || friend.isAero() // Much higher bar for TAG-ging
            ) {
                continue; // useless to us...
            }

            Mounted<?> tag = null;
            int range = 0;
            for (Mounted<?> mounted : friend.getWeaponList()) {
                WeaponType weaponType = ((WeaponType) mounted.getType());
                if (weaponType.hasFlag(WeaponType.F_TAG)) {
                    tag = mounted;
                    range = weaponType.getLongRange();
                    break;
                }
            }
            if (tag == null) {
                continue;
            }

            int friendRange = Compute.effectiveDistance(game, friend, target, false);
            int ownRange = Compute.effectiveDistance(game, attacker, target, false);
            // Friend has to be as close as their max running speed * flight time, + TAG
            // range, + 8
            int taggingRange = ((1 + Compute.turnsTilHit(ownRange)) * friend.getWalkMP()) + range + 8;

            msg.append("\n").append(friend.getDisplayName()).append(" has TAG at ")
                  .append(friendRange).append(" from target; must be within ")
                  .append(taggingRange).append(" to be able to TAG this target for us.");

            // Need a target hex within 8 of the main target, and within shooting distance
            // of the spotter.
            if (friendRange > taggingRange) {
                continue;
            }

            // is this guy a better spotter?
            if ((spotter == null) || range < distance) {
                msg.append("\n").append(friend.getDisplayName()).append(" is a good candidate.");
                spotter = friend;
                distance = friendRange;
                if (stopAtFirst) {
                    break;
                }
            }
        }
        msg.append("\nFinal result: ")
              .append((spotter == null) ? "no TAG friendly in range" : spotter.getDisplayName())
              .append("!");
        LOGGER.debug(msg.toString());

        return spotter;
    }

    /**
     * Gets the modifiers, if any, that the mek receives from being prone.
     *
     * @return any applicable modifiers due to being prone
     */
    public static ToHitData getProneMods(Game game, Entity attacker, int weaponId) {
        if (!attacker.isProne()) {
            return null; // no modifier
        }
        ToHitData mods = new ToHitData();
        Mounted<?> weapon = attacker.getEquipment(weaponId);
        if (attacker.entityIsQuad()) {
            int legsDead = ((Mek) attacker).countBadLegs();
            if (legsDead == 0 && !attacker.hasHipCrit()) {
                // No legs destroyed and no hip crits: no penalty and can fire all weapons
                return null; // no modifier
            } else if (legsDead >= 3) {
                return new ToHitData(TargetRoll.IMPOSSIBLE,
                      Messages.getString("WeaponAttackAction.AeProneQuadThreeLegs"));
            }
            // we have one or two dead legs...

            // Need an intact front leg
            if (attacker.isLocationBad(Mek.LOC_RIGHT_ARM) && attacker.isLocationBad(Mek.LOC_LEFT_ARM)) {
                return new ToHitData(TargetRoll.IMPOSSIBLE,
                      Messages.getString("WeaponAttackAction.AeProneQuadBothFrontLegs"));
            }

            // front leg-mounted weapons have additional trouble
            if ((weapon.getLocation() == Mek.LOC_RIGHT_ARM) || (weapon.getSecondLocation() == Mek.LOC_RIGHT_ARM)
                  || (weapon.getLocation() == Mek.LOC_LEFT_ARM || (weapon.getSecondLocation() == Mek.LOC_LEFT_ARM))) {
                int otherArm = (weapon.getLocation() == Mek.LOC_RIGHT_ARM
                      || weapon.getSecondLocation() == Mek.LOC_RIGHT_ARM) ? Mek.LOC_LEFT_ARM : Mek.LOC_RIGHT_ARM;
                // check previous attacks for weapons fire from the other arm
                if (Compute.isFiringFromArmAlready(game, weaponId, attacker, otherArm)) {
                    return new ToHitData(TargetRoll.IMPOSSIBLE,
                          Messages.getString("WeaponAttackAction.AeProneQuadFiringOtherLeg"));
                }
            }
            // can't fire rear leg weapons
            if ((weapon.getLocation() == Mek.LOC_LEFT_LEG) || (weapon.getLocation() == Mek.LOC_RIGHT_LEG)) {
                return new ToHitData(TargetRoll.IMPOSSIBLE,
                      Messages.getString("WeaponAttackAction.AeProneQuadRearLegWeapon"));
            }
            if (((Mek) attacker).getCockpitType() == Mek.COCKPIT_DUAL && attacker.getCrew().hasDedicatedGunner()) {
                mods.addModifier(1, Messages.getString("WeaponAttackAction.AeProne"));
            } else {
                mods.addModifier(2, Messages.getString("WeaponAttackAction.AeProne"));
            }
        } else if (attacker.isTripodMek()) {
            // Tripod Meks have special prone rules per TacOps
            Mek tripod = (Mek) attacker;
            int legsDead = tripod.countBadLegs();
            if (legsDead == 0 && !attacker.hasHipCrit()) {
                // All 3 legs intact and no hip crits: +1 modifier (or +0 with dual cockpit/dedicated gunner)
                // Can fire weapons from head, torsos, and both arms
                // Can't fire leg weapons
                if (weapon.getLocation() == Mek.LOC_LEFT_LEG
                      || weapon.getLocation() == Mek.LOC_RIGHT_LEG
                      || weapon.getLocation() == Mek.LOC_CENTER_LEG) {
                    return new ToHitData(TargetRoll.IMPOSSIBLE,
                          Messages.getString("WeaponAttackAction.AeProneLegWeapon"));
                }
                if (tripod.getCockpitType() == Mek.COCKPIT_DUAL && attacker.getCrew().hasDedicatedGunner()) {
                    mods.addModifier(0, Messages.getString("WeaponAttackAction.AeProneTripod"));
                } else {
                    mods.addModifier(1, Messages.getString("WeaponAttackAction.AeProneTripod"));
                }
            } else {
                // Has hip crit or lost leg(s): follow standard biped prone rules
                int l3ProneFiringArm = Entity.LOC_NONE;

                if (attacker.isLocationBad(Mek.LOC_RIGHT_ARM) || attacker.isLocationBad(Mek.LOC_LEFT_ARM)) {
                    if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_PRONE_FIRE)) {
                        // Can fire with only one arm
                        if (attacker.isLocationBad(Mek.LOC_RIGHT_ARM) && attacker.isLocationBad(Mek.LOC_LEFT_ARM)) {
                            return new ToHitData(TargetRoll.IMPOSSIBLE,
                                  Messages.getString("WeaponAttackAction.AeProneBothArmsDestroyed"));
                        }

                        l3ProneFiringArm = attacker.isLocationBad(Mek.LOC_RIGHT_ARM) ?
                              Mek.LOC_LEFT_ARM :
                              Mek.LOC_RIGHT_ARM;
                    } else {
                        // must have an arm intact
                        return new ToHitData(TargetRoll.IMPOSSIBLE,
                              Messages.getString("WeaponAttackAction.AeProneArmDestroyed"));
                    }
                }

                // arm-mounted weapons have additional trouble
                if ((weapon.getLocation() == Mek.LOC_RIGHT_ARM) || (weapon.getSecondLocation() == Mek.LOC_RIGHT_ARM)
                      || (weapon.getLocation() == Mek.LOC_LEFT_ARM) || (weapon.getSecondLocation()
                      == Mek.LOC_LEFT_ARM)) {
                    if (l3ProneFiringArm == weapon.getLocation() || (weapon.getSecondLocation() != Entity.LOC_NONE
                          && l3ProneFiringArm == weapon.getSecondLocation())) {
                        return new ToHitData(TargetRoll.IMPOSSIBLE,
                              Messages.getString("WeaponAttackAction.AeProneProppingArm"));
                    }

                    int otherArm = (weapon.getLocation() == Mek.LOC_RIGHT_ARM
                          || weapon.getSecondLocation() == Mek.LOC_RIGHT_ARM) ? Mek.LOC_LEFT_ARM : Mek.LOC_RIGHT_ARM;
                    // check previous attacks for weapons fire from the other arm
                    if (Compute.isFiringFromArmAlready(game, weaponId, attacker, otherArm)) {
                        return new ToHitData(TargetRoll.IMPOSSIBLE,
                              Messages.getString("WeaponAttackAction.AeProneFiringOtherArm"));
                    }
                }
                // can't fire leg weapons
                if (weapon.getLocation() == Mek.LOC_LEFT_LEG
                      || weapon.getLocation() == Mek.LOC_RIGHT_LEG
                      || weapon.getLocation() == Mek.LOC_CENTER_LEG) {
                    return new ToHitData(TargetRoll.IMPOSSIBLE,
                          Messages.getString("WeaponAttackAction.AeProneLegWeapon"));
                }
                if (tripod.getCockpitType() == Mek.COCKPIT_DUAL && attacker.getCrew().hasDedicatedGunner()) {
                    mods.addModifier(1, Messages.getString("WeaponAttackAction.AeProne"));
                } else {
                    mods.addModifier(2, Messages.getString("WeaponAttackAction.AeProne"));
                }

                if (l3ProneFiringArm != Entity.LOC_NONE) {
                    mods.addModifier(1, Messages.getString("WeaponAttackAction.AePronePropping"));
                }
            }
        } else {
            int l3ProneFiringArm = Entity.LOC_NONE;

            if (attacker.isLocationBad(Mek.LOC_RIGHT_ARM) || attacker.isLocationBad(Mek.LOC_LEFT_ARM)) {
                if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_PRONE_FIRE)) {
                    // Can fire with only one arm
                    if (attacker.isLocationBad(Mek.LOC_RIGHT_ARM) && attacker.isLocationBad(Mek.LOC_LEFT_ARM)) {
                        return new ToHitData(TargetRoll.IMPOSSIBLE,
                              Messages.getString("WeaponAttackAction.AeProneBothArmsDestroyed"));
                    }

                    l3ProneFiringArm = attacker.isLocationBad(Mek.LOC_RIGHT_ARM) ? Mek.LOC_LEFT_ARM : Mek.LOC_RIGHT_ARM;
                } else {
                    // must have an arm intact
                    return new ToHitData(TargetRoll.IMPOSSIBLE,
                          Messages.getString("WeaponAttackAction.AeProneArmDestroyed"));
                }
            }

            // arm-mounted weapons have additional trouble
            if ((weapon.getLocation() == Mek.LOC_RIGHT_ARM) || (weapon.getSecondLocation() == Mek.LOC_RIGHT_ARM)
                  || (weapon.getLocation() == Mek.LOC_LEFT_ARM) || (weapon.getSecondLocation() == Mek.LOC_LEFT_ARM)) {
                if (l3ProneFiringArm == weapon.getLocation() || (weapon.getSecondLocation() != Entity.LOC_NONE
                      && l3ProneFiringArm == weapon.getSecondLocation())) {
                    return new ToHitData(TargetRoll.IMPOSSIBLE,
                          Messages.getString("WeaponAttackAction.AeProneProppingArm"));
                }

                int otherArm = (weapon.getLocation() == Mek.LOC_RIGHT_ARM
                      || weapon.getSecondLocation() == Mek.LOC_RIGHT_ARM) ? Mek.LOC_LEFT_ARM : Mek.LOC_RIGHT_ARM;
                // check previous attacks for weapons fire from the other arm
                if (Compute.isFiringFromArmAlready(game, weaponId, attacker, otherArm)) {
                    return new ToHitData(TargetRoll.IMPOSSIBLE,
                          Messages.getString("WeaponAttackAction.AeProneFiringOtherArm"));
                }
            }
            // can't fire leg weapons
            if ((weapon.getLocation() == Mek.LOC_LEFT_LEG) || (weapon.getLocation() == Mek.LOC_RIGHT_LEG)) {
                return new ToHitData(TargetRoll.IMPOSSIBLE,
                      Messages.getString("WeaponAttackAction.AeProneLegWeapon"));
            }
            if (((Mek) attacker).getCockpitType() == Mek.COCKPIT_DUAL && attacker.getCrew().hasDedicatedGunner()) {
                mods.addModifier(1, Messages.getString("WeaponAttackAction.AeProne"));
            } else {
                mods.addModifier(2, Messages.getString("WeaponAttackAction.AeProne"));
            }

            if (l3ProneFiringArm != Entity.LOC_NONE) {
                mods.addModifier(1, Messages.getString("WeaponAttackAction.AePronePropping"));
            }
        }
        return mods;
    }

    /**
     * Checks to see if there is an attack previous to the one with this weapon from the specified arm.
     *
     * @return true if there is a previous attack from this arm
     */
    private static boolean isFiringFromArmAlready(Game game, int weaponId, final Entity attacker, int armLoc) {
        int torsoLoc = Mek.getInnerLocation(armLoc);
        for (Enumeration<EntityAction> i = game.getActions(); i.hasMoreElements(); ) {
            EntityAction entityAction = i.nextElement();
            if (!(entityAction instanceof WeaponAttackAction prevAttack)) {
                continue;
            }
            // stop when we get to this weapon attack (does this always work?)
            if ((prevAttack.getEntityId() == attacker.getId()) && (prevAttack.getWeaponId() == weaponId)) {
                break;
            }
            if (((prevAttack.getEntityId() == attacker.getId())
                  && (attacker.getEquipment(prevAttack.getWeaponId()).getLocation() == armLoc))
                  || ((prevAttack.getEntityId() == attacker.getId())
                  && (attacker.getEquipment(prevAttack.getWeaponId()).getLocation() == torsoLoc)
                  && attacker.getEquipment(prevAttack.getWeaponId()).isSplit())) {
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
    public static ToHitData getDamageWeaponMods(Entity attacker, Mounted<?> weapon) {
        ToHitData mods = new ToHitData();
        if (attacker instanceof ProtoMek attackingProtoMek) {
            // Head critical slots add to target number of all weapons.
            int hits = attackingProtoMek.getCritsHit(ProtoMek.LOC_HEAD);
            if (hits > 0) {
                mods.addModifier(hits, hits + " head critical(s)");
            }

            // Arm mounted (and main gun) weapons get DRMs from arm crits.
            switch (weapon.getLocation()) {
                case ProtoMek.LOC_LEFT_ARM:
                case ProtoMek.LOC_RIGHT_ARM:
                    hits = attackingProtoMek.getCritsHit(weapon.getLocation());
                    if (hits > 0) {
                        mods.addModifier(hits, hits + " arm critical(s)");
                    }
                    break;
                case ProtoMek.LOC_MAIN_GUN:
                    // Main gun is affected by crits in *both* arms.
                    hits = attackingProtoMek.getCritsHit(ProtoMek.LOC_LEFT_ARM);
                    hits += attackingProtoMek.getCritsHit(ProtoMek.LOC_RIGHT_ARM);
                    if (4 == hits) {
                        mods.addModifier(TargetRoll.IMPOSSIBLE,
                              "Cannot fire main gun with no arms.");
                    } else if (hits > 0) {
                        mods.addModifier(hits, hits + " arm critical(s)");
                    }
                    break;
            }

        } // End attacker-is-Protomek

        // only meks have arm actuators - for those, we check whether
        // there is arm actuator damage
        else if (attacker instanceof Mek) {
            // split weapons need to account for arm actuator hits, to
            // see bug 1363690
            // we don't need to specifically check for weapons split between
            // torso and leg, because for those, the location stored in the
            // Mounted is the leg.
            int location = weapon.getLocation();
            if (weapon.isSplit()) {
                switch (location) {
                    case Mek.LOC_LEFT_TORSO:
                        location = Mek.LOC_LEFT_ARM;
                        break;
                    case Mek.LOC_RIGHT_TORSO:
                        location = Mek.LOC_RIGHT_ARM;
                        break;
                    default:
                }
            }

            // only arms can have damaged arm actuators
            if ((location == Mek.LOC_LEFT_ARM || location == Mek.LOC_RIGHT_ARM) && (attacker.braceLocation()
                  != location)) {
                if (attacker.getBadCriticalSlots(CriticalSlot.TYPE_SYSTEM, Mek.ACTUATOR_SHOULDER, location) > 0) {
                    mods.addModifier(4, "shoulder actuator destroyed");
                } else {
                    // no shoulder hits, add other arm hits
                    int actuatorHits = 0;
                    if (attacker.getBadCriticalSlots(CriticalSlot.TYPE_SYSTEM, Mek.ACTUATOR_UPPER_ARM, location) > 0) {
                        actuatorHits++;
                    }
                    if (attacker.getBadCriticalSlots(CriticalSlot.TYPE_SYSTEM, Mek.ACTUATOR_LOWER_ARM, location) > 0) {
                        actuatorHits++;
                    }
                    if (actuatorHits > 0) {
                        mods.addModifier(actuatorHits, actuatorHits + " destroyed arm actuators");
                    }
                }
            }
        }

        // sensors critical hit to attacker
        int sensorHits = attacker.getBadCriticalSlots(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_SENSORS, Mek.LOC_HEAD);
        if ((attacker instanceof Mek attackerMek) && (attackerMek.getCockpitType() == Mek.COCKPIT_TORSO_MOUNTED)) {
            sensorHits += attackerMek.getBadCriticalSlots(CriticalSlot.TYPE_SYSTEM,
                  Mek.SYSTEM_SENSORS,
                  Mek.LOC_CENTER_TORSO);
            if (sensorHits > 1) {
                mods.addModifier(4, "attacker sensors badly damaged");
            } else if (sensorHits > 0) {
                mods.addModifier(2, "attacker sensors damaged");
            }
        } else if (sensorHits > 0) {
            if (attacker instanceof Mek attackerMek
                  && attackerMek.getCockpitType() == Mek.COCKPIT_DUAL
                  && attacker.getCrew().hasDedicatedGunner()) {
                mods.addModifier(1, "attacker sensors damaged");
            } else {
                mods.addModifier(2, "attacker sensors damaged");
            }
        }

        // if partial sensor/stabilizer/fcs/cic repairs are present the shot will be
        // more difficult
        if (attacker.getPartialRepairs() != null) {
            if (attacker.getPartialRepairs().booleanOption("sensors_1_crit")) {
                mods.addModifier(1, "sensor damage");
            }
            if (attacker.getPartialRepairs().booleanOption("mek_sensors_2_crit")) {
                mods.addModifier(2, "sensor damage");
            }
            if (attacker.getPartialRepairs().booleanOption("veh_stabilizer_crit")) {
                mods.addModifier(1, "stabilizer damage");
            }
            if (attacker.getPartialRepairs().booleanOption("aero_cic_fcs_replace")) {
                mods.addModifier(1, "mis-replaced cic/fcs equipment");
            }
            if (attacker.getPartialRepairs().booleanOption("aero_cic_fcs_crit")) {
                mods.addModifier(1, "faulty cic/fcs repairs");
            }
        }

        return mods;
    }

    /**
     * Determines if the current target is a secondary target, and if so, returns the appropriate modifier.
     *
     * @return The secondary target modifier.
     *
     * @author Ben
     */
    public static ToHitData getSecondaryTargetMod(Game game, Entity attacker,
          Targetable target) {

        // large craft do not get secondary target mod
        // http://www.classicbattletech.com/forums/index.php/topic,37661.0.html
        if (attacker.getCrew().getCrewType().getMaxPrimaryTargets() < 0) {
            return null;
        }

        boolean curInFrontArc = ComputeArc.isInArc(attacker.getPosition(),
              attacker.getSecondaryFacing(),
              target,
              attacker.getForwardArc());
        boolean curInRearArc = ComputeArc.isInArc(attacker.getPosition(),
              attacker.getSecondaryFacing(),
              target,
              attacker.getRearArc());
        if (!curInRearArc && attacker.hasQuirk(OptionsConstants.QUIRK_POS_MULTI_TRAC)) {
            return null;
        }

        int primaryTarget = Entity.NONE;
        boolean primaryInFrontArc = false;
        // Track # of targets, for secondary modifiers w/ multi-crew vehicles
        Set<Integer> targIds = new HashSet<>();
        for (Enumeration<EntityAction> i = game.getActions(); i.hasMoreElements(); ) {
            Object o = i.nextElement();
            if (!(o instanceof WeaponAttackAction prevAttack)) {
                continue;
            }
            if (prevAttack.getEntityId() == attacker.getId()) {
                // Don't add id of current target, as it gets counted elsewhere
                if (prevAttack.getTargetId() != target.getId()) {
                    targIds.add(prevAttack.getTargetId());
                }
                // first front arc target is our primary.
                // if first target is non-front, and either a later target or
                // the current one is in front, use that instead.
                if (!game.getOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_NO_FORCED_PRIMARY_TARGETS)) {
                    Targetable pte = game.getTarget(prevAttack.getTargetType(), prevAttack.getTargetId());
                    // in double-blind play, we might not have the target in our
                    // local copy of the game. In that case, the sprite won't
                    // have the correct to-hit number, but at least we don't crash
                    if (pte == null) {
                        continue;
                    }

                    // Determine primary target
                    if ((primaryTarget == Entity.NONE || !primaryInFrontArc)
                          && ComputeArc.isInArc(attacker.getPosition(),
                          attacker.getSecondaryFacing(),
                          pte,
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
        // Tripods and QuadVees with dedicated gunnery can target up to three units
        // before incurring a penalty, and two for dual cockpit
        if (attacker.getCrew().hasDedicatedGunner()) {
            maxPrimary = attacker.getCrew().getCrewType().getMaxPrimaryTargets();
        }
        if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_TAC_OPS_TANK_CREWS)
              && (attacker instanceof Tank)) {

            // If we are a tank, and only have 1 crew then we have some special
            // restrictions
            if (countTargets > 1 && attacker.getCrew().getSize() == 1) {
                return new ToHitData(TargetRoll.IMPOSSIBLE,
                      "Vehicles with only 1 crewman may not attack secondary targets");
            }
            // If we are a tank, we can have Crew Size - 1 targets before
            // incurring a secondary target penalty (or crew size - 2 secondary
            // targets without penalty)
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

        // Stealth-ed Meks can't be secondary targets (TW, pg. 142)
        if (((target instanceof Tank) || (target instanceof Mek) || (target instanceof Aero))
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
     * Damage that a mek does with an accidental fall from above.
     */
    public static int getAccidentalFallFromAboveDamageFor(Entity entity, int fallElevation) {
        return (fallElevation * (int) Math.ceil(entity.getWeight() / 10));
    }

    /**
     * Modifier to attacks due to attacker movement
     */
    public static ToHitData getAttackerMovementModifier(Game game, int entityId) {
        Entity entity = game.getEntity(entityId);

        if (entity == null) {
            return new ToHitData(TargetRoll.AUTOMATIC_FAIL, "Entity Does Not Exist");
        }
        return Compute.getAttackerMovementModifier(game, entityId, entity.moved);
    }

    /**
     * Modifier to attacks due to attacker movement
     */
    public static ToHitData getAttackerMovementModifier(Game game, int entityId,
          EntityMovementType movement) {
        final Entity entity = game.getEntity(entityId);
        ToHitData toHit = new ToHitData();

        if (entity == null) {
            return new ToHitData(TargetRoll.AUTOMATIC_FAIL, "Entity Does Not Exist");
        }

        // infantry aren't affected by their own movement.
        if (entity instanceof Infantry) {
            return toHit;
        }

        int dedicatedGunnerMod = ((entity instanceof Mek mek) && (mek.getCockpitType() == Mek.COCKPIT_DUAL)
              && entity.getCrew().hasDedicatedGunner()) ? 2 : 1;

        if ((entity.getMovementMode() == EntityMovementMode.BIPED_SWIM)
              || (entity.getMovementMode() == EntityMovementMode.QUAD_SWIM)) {
            toHit.addModifier(3 / dedicatedGunnerMod, "attacker used UMUs");
        } else if (entity instanceof LandAirMek && movement == EntityMovementType.MOVE_VTOL_WALK) {
            toHit.addModifier(3 / dedicatedGunnerMod, "attacker cruised");
        } else if (entity instanceof LandAirMek && movement == EntityMovementType.MOVE_VTOL_RUN) {
            toHit.addModifier(4 / dedicatedGunnerMod, "attacker flanked");
        } else if ((movement == EntityMovementType.MOVE_WALK) || (movement == EntityMovementType.MOVE_VTOL_WALK)
              || (movement == EntityMovementType.MOVE_CAREFUL_STAND)) {
            toHit.addModifier(1 / dedicatedGunnerMod, "attacker walked");
        } else if ((movement == EntityMovementType.MOVE_RUN) || (movement == EntityMovementType.MOVE_VTOL_RUN)) {
            toHit.addModifier(2 / dedicatedGunnerMod, "attacker ran");
        } else if (movement == EntityMovementType.MOVE_SKID) {
            toHit.addModifier(3 / dedicatedGunnerMod, "attacker ran and skidded");
        } else if (movement == EntityMovementType.MOVE_JUMP) {
            if (entity.hasAbility(OptionsConstants.PILOT_JUMPING_JACK)) {
                toHit.addModifier(1 / dedicatedGunnerMod, "attacker jumped");
            } else if (entity.hasAbility(OptionsConstants.PILOT_HOPPING_JACK)) {
                toHit.addModifier(2 / dedicatedGunnerMod, "attacker jumped");
            } else {
                toHit.addModifier(3 / dedicatedGunnerMod, "attacker jumped");
            }
        } else if (movement == EntityMovementType.MOVE_SPRINT
              || movement == EntityMovementType.MOVE_VTOL_SPRINT) {
            return new ToHitData(TargetRoll.AUTOMATIC_FAIL, "attacker sprinted");
        }

        return toHit;
    }

    /**
     * Modifier to attacks due to spotter movement
     */
    public static ToHitData getSpotterMovementModifier(Game game, int entityId) {
        Entity entity = game.getEntity(entityId);

        if (entity == null) {
            return new ToHitData(TargetRoll.AUTOMATIC_FAIL, "Entity Does Not Exist");
        }

        return Compute.getSpotterMovementModifier(game, entityId, entity.moved);
    }

    /**
     * Modifier to attacks due to spotter movement
     */
    public static ToHitData getSpotterMovementModifier(Game game,
          int entityId, EntityMovementType movement) {
        ToHitData toHit = new ToHitData();

        Entity entity = game.getEntity(entityId);
        if ((entity instanceof Infantry)) {
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

        if (attacker.getCrew()
              .getOptions()
              .stringOption(OptionsConstants.MISC_ENV_SPECIALIST)
              .equals(Crew.ENVIRONMENT_SPECIALIST_LIGHT)
              && !target.isIlluminated()
              && game.getPlanetaryConditions().getLight().isMoonlessOrSolarFlareOrPitchBack()) {
            toHit.addModifier(-1, "light specialist");
        }

        if (attacker.hasAbility(OptionsConstants.PILOT_MELEE_SPECIALIST) && (attacker instanceof Mek)) {
            toHit.addModifier(-1, "melee specialist");
        }

        if (attacker.hasAbility(OptionsConstants.PILOT_TM_FROGMAN)
              && ((attacker instanceof Mek) || (attacker instanceof ProtoMek))
              && (game.getBoard().getHex(attacker.getPosition()).terrainLevel(Terrains.WATER) > 1)) {
            toHit.addModifier(-1, "Frogman");
        }

        if (attacker.hasAbility(OptionsConstants.UNOFFICIAL_CLAN_PILOT_TRAINING)) {
            toHit.addModifier(1, "clan pilot training");
        }

        // Mek targets that are dodging are harder to hit.
        if ((target instanceof Mek) && target.hasAbility(OptionsConstants.PILOT_DODGE_MANEUVER) && target.dodging) {
            toHit.addModifier(2, "target is dodging");
        }
    }


    /**
     * Modifier to attacks due to target movement
     *
     * @param game     current game
     * @param entityId targetId
     *
     * @return toHitData for the target's movement modifiers
     *
     * @see ToHitData
     */
    public static ToHitData getTargetMovementModifier(Game game, int entityId) {
        Entity entity = game.getEntity(entityId);

        if (entity == null) {
            return new ToHitData(TargetRoll.AUTOMATIC_FAIL, "Entity Does Not Exist");
        }

        if (entity.isAero()) {
            return new ToHitData();
        }

        // If we're a trailer and being towed, return data for the tractor
        if (entity.isTrailer() && entity.getTractor() != Entity.NONE) {
            return getTargetMovementModifier(game, entity.getTractor());
        }

        if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_GROUND_MOVEMENT_TAC_OPS_STANDING_STILL)
              && (entity.mpUsed == 0)
              && !entity.isImmobile()
              && !((entity instanceof Infantry) || (entity instanceof VTOL) || (entity.isBuildingEntityOrGunEmplacement()))) {
            ToHitData toHit = new ToHitData();
            toHit.addModifier(-1, "target did not move");
            return toHit;
        }

        if (entity.isAssaultDropInProgress()) {
            ToHitData toHit = new ToHitData();
            toHit.addModifier(3, "target is assault dropping");
            return toHit;
        }

        // Compile various state information to determine if the entity jumped,
        // "jumped", or is VTOL
        // Airborne non-ASF vehicles like WiGE can get +1 TMM for jumping _or_ being
        // airborne, but not both.
        // Non-flying WiGE _can_ get +1 TMM for jumping.
        // See TW: pg. 307, "Attack Modifiers Table"
        boolean jumped = !entity.isAirborneVTOLorWIGE()
              && ((entity.moved == EntityMovementType.MOVE_JUMP)
              || (entity.moved == EntityMovementType.MOVE_VTOL_RUN)
              || (entity.moved == EntityMovementType.MOVE_VTOL_WALK)
              || (entity.moved == EntityMovementType.MOVE_VTOL_SPRINT));

        boolean airborneNonAerospace = (entity.moved == EntityMovementType.MOVE_VTOL_RUN)
              || (entity.moved == EntityMovementType.MOVE_VTOL_WALK)
              || ((entity.getMovementMode() == EntityMovementMode.VTOL)
              && ((entity.moved != EntityMovementType.MOVE_NONE) || entity.isAirborneVTOLorWIGE()))
              || (entity.moved == EntityMovementType.MOVE_VTOL_SPRINT);

        ToHitData toHit = Compute
              .getTargetMovementModifier(
                    entity.delta_distance,
                    jumped,
                    airborneNonAerospace,
                    game);

        if (entity.moved != EntityMovementType.MOVE_JUMP
              && entity.delta_distance > 0
              && entity instanceof Mek && ((Mek) entity).getCockpitType() == Mek.COCKPIT_DUAL
              && entity.getCrew().hasDedicatedPilot()) {
            if (toHit.getModifiers().isEmpty()) {
                toHit.addModifier(1, "target moved 1-2 hexes");
            } else {
                toHit.addModifier(1, "dedicated pilot");
            }
        }

        // Did the target skid this turn?
        if (entity.moved == EntityMovementType.MOVE_SKID) {
            toHit.addModifier(2, "target skidded");
        }

        // did the target sprint?
        if (entity.moved == EntityMovementType.MOVE_SPRINT
              || entity.moved == EntityMovementType.MOVE_VTOL_SPRINT) {
            toHit.addModifier(-1, "target sprinted");
        }

        return toHit;
    }

    /**
     * Target movement modifier for the specified distance
     *
     * @param distance             how many hexes did the target unit move?
     * @param jumped               did the target unit jump?
     * @param airborneNonAerospace was the target an airborne, non-aerospace unit?
     * @param game                 current game
     *
     * @return toHitData for the target's movement modifiers
     *
     * @see ToHitData
     */
    public static ToHitData getTargetMovementModifier(int distance,
          boolean jumped, boolean airborneNonAerospace, Game game) {
        ToHitData toHit = new ToHitData();
        if (distance == 0 && !jumped && !airborneNonAerospace) {
            return toHit;
        }

        if ((game != null)
              && game.getOptions().booleanOption(OptionsConstants.ADVANCED_MAX_TECH_MOVEMENT_MODS)) {
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

        // TW p. 117 Jumped/Airborne (non-aerospace units) get +1 to hit modifier,
        // calculate that info outside of this method
        if (airborneNonAerospace) {
            toHit.addModifier(1, "target was airborne");
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
        final Hex hex = game.getHexOf(attacker);

        ToHitData toHit = new ToHitData();

        // space screens; bonus depends on number (level)
        if ((hex != null) && (hex.terrainLevel(Terrains.SCREEN) > 0)) {
            toHit.addModifier(hex.terrainLevel(Terrains.SCREEN) + 1,
                  "attacker in screen(s)");
        }

        return toHit;
    }

    /**
     * Modifier to attacks due to target terrain TODO:um....should VTOLs get modifiers for smoke, etc.
     */
    public static ToHitData getTargetTerrainModifier(Game game, Targetable target) {
        return Compute.getTargetTerrainModifier(game, target, 0);
    }

    public static ToHitData getTargetTerrainModifier(Game game, Targetable target, int eiStatus) {
        return Compute.getTargetTerrainModifier(game, target, eiStatus, false);
    }

    public static ToHitData getTargetTerrainModifier(Game game, Targetable target, int eiStatus,
          boolean attackerInSameBuilding) {
        return Compute.getTargetTerrainModifier(game, target, eiStatus, attackerInSameBuilding, false);
    }

    public static ToHitData getTargetTerrainModifier(Game game, Targetable targetable, int eiStatus,
          boolean attackerInSameBuilding, boolean underwaterWeapon) {
        ToHitData toHit = new ToHitData();

        // no terrain mods for bombs, artillery strikes
        if (targetable.getTargetType() == Targetable.TYPE_HEX_AERO_BOMB ||
              targetable.getTargetType() == Targetable.TYPE_HEX_ARTILLERY) {
            return toHit;
        }

        Entity entityTarget = null;
        Hex hex = game.getHexOf(targetable);

        if (targetable.getTargetType() == Targetable.TYPE_ENTITY && targetable instanceof Entity) {
            entityTarget = (Entity) targetable;
            if (hex == null) {
                Entity gameEntity = game.getEntity(entityTarget.getId());

                if (gameEntity != null) {
                    entityTarget.setPosition(gameEntity.getPosition());
                    hex = game.getHexOf(gameEntity);
                }
            }
        }

        // if the hex doesn't targetable exist, it's unlikely to have terrain modifiers
        if (hex == null) {
            return toHit;
        }

        boolean hasWoods = hex.containsTerrain(Terrains.WOODS) || hex.containsTerrain(Terrains.JUNGLE);
        // Standard meks (standing) report their height as 1, tanks as 0
        // Standard meks should not benefit from 1 level high woods

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
        boolean isAboveStructures = (entityTarget != null) &&
              ((entityTarget.relHeight() > hex.ceiling()) ||
                    entityTarget.isAirborne());

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

        if (!game.getOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_WOODS_COVER)
              && !isAboveWoods
              && !((targetable.getTargetType() == Targetable.TYPE_HEX_CLEAR)
              || (targetable.getTargetType() == Targetable.TYPE_HEX_IGNITE)
              || (targetable.getTargetType() == Targetable.TYPE_HEX_BOMB)
              || (targetable.getTargetType() == Targetable.TYPE_HEX_ARTILLERY)
              || (targetable.getTargetType() == Targetable.TYPE_MINEFIELD_DELIVER))) {
            if (woodsLevel == 1) {
                // EI reduces modifier by 1 per hex, minimum +1 per hex (IO p.69)
                // Light woods is already +1, so EI provides no benefit
                toHit.addModifier(1, woodsText);
            } else if (woodsLevel > 1) {
                // Always add full woods modifier
                toHit.addModifier(woodsLevel, woodsText);
                if (eiStatus > 0) {
                    // EI reduces modifier by 1 per hex, minimum +1 per hex (IO p.69)
                    // Track in ToHitData for combined EI modifier at the end
                    toHit.addEiReduction(woodsLevel - 1);
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
                    // EI reduces modifier by 1 per hex, minimum +1 per hex (IO p.69)
                    // Light smoke is already +1, so EI provides no benefit
                    toHit.addModifier(1, "target in light smoke");
                    break;
                case SmokeCloud.SMOKE_HEAVY:
                    // Always add full heavy smoke modifier
                    toHit.addModifier(2, "target in heavy smoke");
                    if (eiStatus > 0) {
                        // EI reduces modifier by 1 per hex (IO p.69)
                        // Track in ToHitData for combined EI modifier at the end
                        toHit.addEiReduction(1);
                    }
                    break;
            }
        }
        if (hex.terrainLevel(Terrains.GEYSER) == 2) {
            // Always add full geyser modifier
            toHit.addModifier(2, "target in erupting geyser");
            if (eiStatus > 0) {
                // EI reduces geyser modifier by 1 (IO p.69)
                // Track in ToHitData for combined EI modifier at the end
                toHit.addEiReduction(1);
            }
        }

        if (!isAboveStructures && hex.containsTerrain(Terrains.INDUSTRIAL)) {
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
            // flag for VTOLs and such
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

    public static ToHitData getStrafingTerrainModifier(Game game, int eiStatus, Hex hex) {
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

        if (!game.getOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_WOODS_COVER)) {
            if (woodsLevel == 1) {
                // EI reduces modifier by 1 per hex, minimum +1 per hex (IO p.69)
                // Light woods is already +1, so EI provides no benefit
                toHit.addModifier(1, woodsText);
            } else if (woodsLevel > 1) {
                // Always add full woods modifier
                toHit.addModifier(woodsLevel, woodsText);
                if (eiStatus > 0) {
                    // EI reduces modifier by 1 per hex, minimum +1 per hex (IO p.69)
                    // Track in ToHitData for combined EI modifier at the end
                    toHit.addEiReduction(woodsLevel - 1);
                }
            }
        }

        switch (hex.terrainLevel(Terrains.SMOKE)) {
            case SmokeCloud.SMOKE_LIGHT:
            case SmokeCloud.SMOKE_LI_LIGHT:
            case SmokeCloud.SMOKE_LI_HEAVY:
            case SmokeCloud.SMOKE_CHAFF_LIGHT:
            case SmokeCloud.SMOKE_GREEN:
                // EI reduces modifier by 1 per hex, minimum +1 per hex (IO p.69)
                // Light smoke is already +1, so EI provides no benefit
                toHit.addModifier(1, "target in light smoke");
                break;
            case SmokeCloud.SMOKE_HEAVY:
                // Always add full heavy smoke modifier
                toHit.addModifier(2, "target in heavy smoke");
                if (eiStatus > 0) {
                    // EI reduces modifier by 1 per hex (IO p.69)
                    // Track in ToHitData for combined EI modifier at the end
                    toHit.addEiReduction(1);
                }
                break;
        }

        if (hex.terrainLevel(Terrains.GEYSER) == 2) {
            // Always add full geyser modifier
            toHit.addModifier(2, "erupting geyser");
            if (eiStatus > 0) {
                // EI reduces geyser modifier by 1 (IO p.69)
                // Track in ToHitData for combined EI modifier at the end
                toHit.addEiReduction(1);
            }
        }

        return toHit;
    }

    /**
     * Calculates the current theoretical damage absorbable (armor+structure, etc.) by the given target. Used as a
     * measure of the potential durability of the target under fire.
     */
    public static int getTargetTotalHP(Game game, Targetable target) {
        int targetType = target.getTargetType();
        int targetId = target.getId();
        Coords position = target.getPosition();

        // First, handle buildings versus entities, since they are handled differently.
        if (targetType == Targetable.TYPE_BUILDING) {
            // Buildings are a simple sum of their current CF and armor values.
            // the building the targeted hex belongs to. We have to get this and then get
            // values for the specific hex internally to it.
            final IBuilding parentBuilding = game.getBoard().getBuildingAt(position);
            return (parentBuilding == null) ? 0
                  : parentBuilding.getCurrentCF(position) + parentBuilding.getArmor(position);
        } else if (targetType == Targetable.TYPE_ENTITY) {
            // I don't *think* we have to handle infantry differently here - I think these
            // methods should return the total number of men remaining as internal
            // structure.
            Entity targetEntity = game.getEntity(targetId);

            if (targetEntity == null) {
                return 0;
            } else if (targetEntity.isBuildingEntityOrGunEmplacement()) {
                // If this is a gun emplacement, handle it as the building hex it is in.
                final IBuilding parentBuilding = game.getBoard().getBuildingAt(position);
                return (parentBuilding == null) ? 0
                      : parentBuilding.getCurrentCF(position) + parentBuilding.getArmor(position);
            } else {
                return targetEntity.getTotalArmor() + targetEntity.getTotalInternal();
            }
        } else if (targetType == Targetable.TYPE_HEX_CLEAR) {
            // clearing a hex - the "HP" is the terrain factor of destroyable terrain on
            // this hex
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
     * Returns the weapon attack out of a list that has the highest expected damage
     */
    public static WeaponAttackAction getHighestExpectedDamage(Game g,
          List<WeaponAttackAction> vAttacks, boolean assumeHit) {
        float fHighest = -1.0f;
        WeaponAttackAction waaHighest = null;
        for (WeaponAttackAction waa : vAttacks) {
            float fDanger = Compute.getExpectedDamage(g, waa, assumeHit);
            if (fDanger > fHighest) {
                fHighest = fDanger;
                waaHighest = waa;
            }
        }
        return waaHighest;
    }

    /**
     * Returns the weapon attack out of a list that has the second highest expected damage
     * Used for Playtest 3 AMS engaging multiple salvos
     */
    public static WeaponAttackAction getSecondHighestExpectedDamage(Game g,
          List<WeaponAttackAction> vAttacks, boolean assumeHit) {
        WeaponAttackAction waaHighest = null;
        WeaponAttackAction waaSecondHighest = null;
        
        // Copy the list to a new list
        List<WeaponAttackAction> attacksClone = new ArrayList<>(vAttacks);
        // Find the highest damage
        waaHighest = getHighestExpectedDamage(g, attacksClone, assumeHit);
        // Remove that entry from the list
        attacksClone.remove(waaHighest);
        // Get the next highest damage
        waaSecondHighest = getHighestExpectedDamage(g, attacksClone, assumeHit);
        // Returns the second highest damage
        return waaSecondHighest;
    }

    // store these as constants since the tables will never change
    private static final float[] expectedHitsByRackSize = { 0.0f, 1.0f, 1.58f, 2.0f, 2.63f, 3.17f, 4.0f, 4.49f, 4.98f,
                                                            5.47f, 6.31f, 7.23f, 8.14f, 8.59f, 9.04f, 9.5f, 10.1f,
                                                            10.8f, 11.42f, 12.1f, 12.7f };

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
     * Determines the expected damage to a weapon attack, based on to-hit, salvo sizes, etc.
     */
    public static float getExpectedDamage(Game g, WeaponAttackAction waa,
          boolean assumeHit) {
        return Compute.getExpectedDamage(g, waa, assumeHit, null);
    }

    /**
     * Determines the expected damage to a weapon attack, based on to-hit, salvo sizes, etc.
     */
    public static float getExpectedDamage(Game game, WeaponAttackAction weaponAttackAction, boolean assumeHit,
          List<ECMInfo> allECMInfo) {

        if (weaponAttackAction.getAssumedHit() == assumeHit && weaponAttackAction.getExpectedDamage() >= 0.0) {
            // Reuse previous calc results
            return weaponAttackAction.getExpectedDamage();
        }

        weaponAttackAction.setAssumedHit(assumeHit);

        boolean use_table = false;

        AmmoType loaded_ammo;

        Entity attacker = game.getEntity(weaponAttackAction.getEntityId());
        Entity target = game.getEntity(weaponAttackAction.getTargetId());

        if (attacker == null) {
            weaponAttackAction.setExpectedDamage(0.0f);
            return 0.0f;
        }

        int baShootingStrength = attacker instanceof BattleArmor ? ((BattleArmor) attacker).getShootingStrength() : 0;

        int infShootingStrength = 0;
        double infDamagePerTrooper = 0;

        WeaponMounted weapon = (WeaponMounted) attacker.getEquipment(weaponAttackAction.getWeaponId());
        Mounted<?> lnk_guide;

        ToHitData hitData = weaponAttackAction.toHit(game, allECMInfo);

        if (attacker.isConventionalInfantry() && attacker instanceof Infantry infantry) {
            infShootingStrength = infantry.getShootingStrength();
            infDamagePerTrooper = infantry.getDamagePerTrooper();
        }

        WeaponType wt = weapon.getType();

        float fDamage = 0.0f;
        float fChance;

        if (assumeHit) {
            fChance = 1.0f;
        } else {
            if ((hitData.getValue() == TargetRoll.IMPOSSIBLE) || (hitData.getValue() == TargetRoll.AUTOMATIC_FAIL)) {
                weaponAttackAction.setExpectedDamage(0.0f);
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

        // Missiles, HAGs, LBX cluster rounds, and ultra/rotary cannons (when spun up) use the missile hits table
        if (wt.getDamage() == WeaponType.DAMAGE_BY_CLUSTER_TABLE) {
            use_table = true;
        }

        // Unless it's a fighter squadron, which uses a weird group of single weapons and should return mass AV
        if (attacker.isCapitalFighter()) {
            use_table = false;
        }

        if ((wt.getAmmoType() == AmmoTypeEnum.AC_LBX) || (wt.getAmmoType() == AmmoTypeEnum.AC_LBX_THB)) {
            loaded_ammo = (AmmoType) weapon.getLinked().getType();
            if (((loaded_ammo.getAmmoType() == AmmoTypeEnum.AC_LBX) || (loaded_ammo
                  .getAmmoType() == AmmoTypeEnum.AC_LBX_THB))
                  && (loaded_ammo.getMunitionType().contains(AmmoType.Munitions.M_CLUSTER))) {
                use_table = true;
            }
        }

        if ((wt.getAmmoType() == AmmoTypeEnum.AC_ULTRA)
              || (wt.getAmmoType() == AmmoTypeEnum.AC_ULTRA_THB)
              || (wt.getAmmoType() == AmmoTypeEnum.AC_ROTARY)) {
            if ((weapon.curMode().getName().equals("Ultra"))
                  || (weapon.curMode().getName().equals("2-shot"))
                  || (weapon.curMode().getName().equals("3-shot"))
                  || (weapon.curMode().getName().equals("4-shot"))
                  || (weapon.curMode().getName().equals("5-shot"))
                  || (weapon.curMode().getName().equals("6-shot"))) {
                use_table = true;
            }
        }

        // Kinda cheap, but let's use the missile hits table for Battle armor
        // weapons too

        if (attacker instanceof BattleArmor) {
            if ((!Objects.equals(wt.getInternalName(), Infantry.SWARM_MEK))
                  && (!Objects.equals(wt.getInternalName(), Infantry.LEG_ATTACK))) {
                use_table = true;
            }
        }

        if (use_table) {
            if (!(attacker instanceof BattleArmor)) {
                if (weapon.getLinked() == null) {
                    weaponAttackAction.setExpectedDamage(0.0f);
                    return 0.0f;
                }
            }

            AmmoType at = null;
            if (weaponAttackAction.getAmmoId() != WeaponAttackAction.UNASSIGNED) {
                // If a preferred ammo has been set for this WAA, use that
                at = weaponAttackAction.getEntity(game).getAmmo(weaponAttackAction.getAmmoId()).getType();
            } else if ((weapon.getLinked() != null)
                  && (weapon.getLinked().getType() instanceof AmmoType)) {
                at = (AmmoType) weapon.getLinked().getType();
            }
            fDamage = (at != null) ? at.getDamagePerShot() : fDamage;

            float fHits;
            if ((wt.getRackSize() != 40) && (wt.getRackSize() != 30)) {
                fHits = expectedHitsByRackSize[wt.getRackSize()];
            } else {
                fHits = 2.0f * expectedHitsByRackSize[wt.getRackSize() / 2];
            }
            // Streaks / iATMs will _all_ hit, if they hit at all.
            if (((wt.getAmmoType() == AmmoTypeEnum.SRM_STREAK)
                  || (wt.getAmmoType() == AmmoTypeEnum.LRM_STREAK))
                  || (wt.getAmmoType() == AmmoTypeEnum.IATM)
                  && !ComputeECM.isAffectedByAngelECM(attacker, attacker
                        .getPosition(), weaponAttackAction.getTarget(game).getPosition(),
                  allECMInfo)) {
                fHits = wt.getRackSize();
            }
            if ((wt.getAmmoType() == AmmoTypeEnum.AC_ULTRA)
                  || (wt.getAmmoType() == AmmoTypeEnum.AC_ULTRA_THB)
                  || (wt.getAmmoType() == AmmoTypeEnum.AC_ROTARY)) {
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
                if (wt.getDamage() == WeaponType.DAMAGE_BY_CLUSTER_TABLE) {
                    fHits *= expectedHitsByRackSize[wt.getRackSize()];
                }
                if (wt.getDamage() != WeaponType.DAMAGE_BY_CLUSTER_TABLE) {
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

            Entity weaponTarget = game.getEntity(weaponAttackAction.getWeaponId());
            if ((weaponTarget != null) &&
                  (!ComputeECM.isAffectedByECM(attacker,
                        attacker.getPosition(),
                        weaponTarget.getPosition(),
                        allECMInfo))
                  && (wt.getDamage() == WeaponType.DAMAGE_BY_CLUSTER_TABLE)
                  && (wt.hasFlag(WeaponType.F_MISSILE)) && null != at) {
                // Check for linked artemis guidance system
                if ((wt.getAmmoType() == AmmoTypeEnum.LRM)
                      || (wt.getAmmoType() == AmmoTypeEnum.LRM_IMP)
                      || (wt.getAmmoType() == AmmoTypeEnum.MML)
                      || (wt.getAmmoType() == AmmoTypeEnum.SRM)
                      || (wt.getAmmoType() == AmmoTypeEnum.SRM_IMP)) {
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
                if (wt.getAmmoType() == AmmoTypeEnum.ATM) {
                    fHits *= 1.2f;
                }

                // Check for target with attached Narc or iNarc homing pod from
                // friendly unit
                if ((target != null)
                      && (target.isNarcedBy(attacker.getOwner().getTeam())
                      || target.isINarcedBy(attacker.getOwner().getTeam()))) {
                    if (((at.getAmmoType() == AmmoTypeEnum.LRM)
                          || (at.getAmmoType() == AmmoTypeEnum.LRM_IMP)
                          || (at.getAmmoType() == AmmoTypeEnum.MML)
                          || (at.getAmmoType() == AmmoTypeEnum.SRM)
                          || (at.getAmmoType() == AmmoTypeEnum.SRM_IMP))
                          && (at.getMunitionType().contains(AmmoType.Munitions.M_NARC_CAPABLE))) {
                        fHits *= 1.2f;
                    }
                }
            }

            if (wt.getAmmoType() == AmmoTypeEnum.MRM) {
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
            if ((wt.getDamage() == WeaponType.DAMAGE_BY_CLUSTER_TABLE)
                  && wt.hasFlag(WeaponType.F_MISSILE)) {
                List<WeaponMounted> vCounters = weaponAttackAction.getCounterEquipment();
                if (vCounters != null) {
                    for (WeaponMounted vCounter : vCounters) {
                        EquipmentType type = vCounter.getType();
                        if ((type != null) && type.hasFlag(WeaponType.F_AMS)) {
                            fHits *= 0.6f;
                        }
                    }
                }
            }

            // * HAGs modify their cluster hits for range.
            if (target != null && wt instanceof HAGWeapon) {
                int distance = attacker.getPosition().distance(target.getPosition());
                if (distance <= wt.getShortRange()) {
                    fHits *= 1.2f;
                } else if (distance > wt.getMediumRange()) {
                    fHits *= 0.8f;
                }
            }

            fDamage *= fHits;

            if ((wt.getAmmoType() == AmmoTypeEnum.AC_ULTRA)
                  || (wt.getAmmoType() == AmmoTypeEnum.AC_ULTRA_THB)
                  || (wt.getAmmoType() == AmmoTypeEnum.AC_ROTARY)) {
                fDamage = fHits * wt.getDamage();
            }

        } else {
            // Direct fire weapons (and LBX slug rounds) just do a single shot so they don't use the missile hits
            // table. Weapon bays also deal damage in a single block
            if ((attacker.getPosition() != null)
                  && (target != null)
                  && (target.getPosition() != null)) {
                // Damage may vary by range for some weapons, so let's see how far
                // away we actually are and then set the damage accordingly.
                int rangeToTarget = attacker.getPosition().distance(target.getPosition());

                // Convert AV to fDamage for bay weapons, fighters, etc
                if (attacker.usesWeaponBays()) {
                    double av = 0;
                    double threat = 1;
                    for (WeaponMounted bayW : weapon.getBayWeapons()) {
                        WeaponType bayWType = bayW.getType();
                        // Capital weapons have a different range scale
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
                    // Capital weapons have a different range scale
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

                    //TODO: rules need to be checked.
                } else if ((wt.getAmmoType() == AmmoTypeEnum.ARROW_IV)
                      || wt.getAmmoType() == AmmoTypeEnum.ARROWIV_PROTO
                      || wt.getAmmoType() == AmmoTypeEnum.ARROW_IV_BOMB) {
                    // This is for arrow IV AMS threat processing
                    fDamage = (float) wt.getRackSize();
                } else {
                    fDamage = wt.getDamage(rangeToTarget);
                }
            }

            // Infantry follow some special rules, but do fixed amounts of damage
            // Anti-mek attacks are weapon-like in nature, so include them here as well
            if (attacker instanceof Infantry) {
                if (Objects.equals(wt.getInternalName(), Infantry.LEG_ATTACK)) {
                    fDamage = 20.0f; // Actually 5, but the chance of crits
                    // deserves a boost
                    // leg attacks are mutually exclusive with swarm attacks,
                } else {
                    boolean targetCanBeSwarmed = (target instanceof Mek) || (target instanceof Tank);

                    if (attacker.isConventionalInfantry()) {
                        if (Objects.equals(wt.getInternalName(), Infantry.SWARM_MEK)) {
                            // If the target is a Mek that is not swarmed, this is a
                            // good thing
                            if (target != null && (target.getSwarmAttackerId() == Entity.NONE) && targetCanBeSwarmed) {
                                fDamage = 1.5f * (float) infDamagePerTrooper * infShootingStrength;
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
                        if (Objects.equals(wt.getInternalName(), Infantry.SWARM_MEK)) {
                            // If the target is a Mek that is not swarmed, this is a
                            // good thing
                            if ((target != null)
                                  && (target.getSwarmAttackerId() == Entity.NONE)
                                  && targetCanBeSwarmed) {
                                // Overestimated, but the chance at crits and headshots deserves a boost
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
        Entity weaponTarget = game.getEntity(weaponAttackAction.getTargetId());

        if (weaponTarget != null && weaponTarget.isConventionalInfantry()) {
            fDamage = directBlowInfantryDamage(fDamage,
                  0,
                  wt.getInfantryDamageClass(),
                  ((Infantry) (weaponTarget)).isMechanized(),
                  false);
        }

        fDamage *= fChance;

        // Conventional infantry take double damage in the open
        if (weaponTarget != null && weaponTarget.isConventionalInfantry()) {
            Hex e_hex = game.getBoard().getHex(
                  weaponTarget.getPosition().getX(),
                  weaponTarget.getPosition().getY());
            if (!e_hex.containsTerrain(Terrains.WOODS)
                  && !e_hex.containsTerrain(Terrains.JUNGLE)
                  && !e_hex.containsTerrain(Terrains.BUILDING)) {
                fDamage *= 2.0f;
            }

            // Cap damage to prevent run-away values
            if (infShootingStrength > 0) {
                fDamage = min(infShootingStrength, fDamage);
            }
        }
        weaponAttackAction.setExpectedDamage(fDamage);
        return fDamage;
    }

    /**
     * If the unit is carrying multiple types of ammo for the specified weapon, cycle through them and choose the type
     * best suited to engage the specified target Value returned is expected damage Note that some ammo types, such as
     * infernos, do no damage or have special properties and so the damage is an estimation of effectiveness
     */

    public static double getAmmoAdjDamage(Game game, WeaponAttackAction weaponAttackAction) {
        boolean noBin = true;
        boolean multiBin = false;

        double ammoMultiple, exDamage, maxDamage;

        Entity shooter, target;

        AmmoMounted firstAmmoBin, bestAmmoBin;
        AmmoType ammoBinType = new AmmoType();
        AmmoType firstAmmoBinType = new AmmoType();
        WeaponType weaponType;
        WeaponType targetWeaponType;

        // Get shooter entity, target entity, and weapon being fired
        target = game.getEntity(weaponAttackAction.getTargetId());
        shooter = weaponAttackAction.getEntity(game);
        weaponType = (WeaponType) shooter.getEquipment(weaponAttackAction.getWeaponId()).getType();

        // If the weapon doesn't require ammo, just get the estimated damage
        if (weaponType.hasFlag(WeaponType.F_ENERGY)
              || weaponType.hasFlag(WeaponType.F_ONE_SHOT)
              || weaponType.hasFlag(WeaponType.F_INFANTRY)
              || (weaponType.getAmmoType() == AmmoTypeEnum.NA)) {
            return Compute.getExpectedDamage(game, weaponAttackAction, false);
        }

        // Get a list of ammo bins and the first valid bin
        firstAmmoBin = null;

        for (AmmoMounted ammoMountedBin : shooter.getAmmo()) {
            if (shooter.loadWeapon((WeaponMounted) shooter.getEquipment(weaponAttackAction.getWeaponId()),
                  ammoMountedBin)) {
                if (ammoMountedBin.getUsableShotsLeft() > 0) {
                    ammoBinType = ammoMountedBin.getType();
                    if (!AmmoType.canDeliverMinefield(ammoBinType)) {
                        firstAmmoBin = ammoMountedBin;
                        firstAmmoBinType = firstAmmoBin.getType();
                        break;
                    }
                }
            }
        }

        // To save processing time, lets see if we have more than one type of
        // bin
        // Thunder-type ammunition and empty bins are excluded from the list
        for (AmmoMounted ammoBin : shooter.getAmmo()) {
            if (shooter.loadWeapon((WeaponMounted) shooter.getEquipment(weaponAttackAction.getWeaponId()), ammoBin)) {
                if (ammoBin.getUsableShotsLeft() > 0) {
                    ammoBinType = ammoBin.getType();
                    if (!AmmoType.canDeliverMinefield(ammoBinType)) {
                        noBin = false;
                        if (ammoBinType.getMunitionType() != firstAmmoBinType
                              .getMunitionType()) {
                            multiBin = true;
                            break;
                        }
                    }
                }
            }
        }

        // If noBin is true, then either all bins are empty or contain
        // Thunder-type rounds, and
        // we can safely say that the expected damage is 0.0
        // If noBin is false, then we have at least one good bin
        if (noBin) {
            return 0.0;
        }
        // If multiBin is true, then multiple ammo types are present and an
        // appropriate type must be selected
        // If multiBin is false, then all bin types are the same; skip down
        // to getting the expected damage
        if (!multiBin) {
            return Compute.getExpectedDamage(game, weaponAttackAction, false);
        } else {

            // Set default max damage as 0, and the best bin as the first
            // bin
            maxDamage = 0.0;
            bestAmmoBin = firstAmmoBin;

            // For each valid ammo bin
            for (AmmoMounted ammoBin : shooter.getAmmo()) {
                if (shooter.loadWeapon((WeaponMounted) shooter.getEquipment(weaponAttackAction.getWeaponId()),
                      ammoBin)) {
                    if (ammoBin.getUsableShotsLeft() > 0) {
                        ammoBinType = ammoBin.getType();
                        if (!AmmoType.canDeliverMinefield(ammoBinType)) {

                            // Load weapon with specified bin
                            shooter.loadWeapon((WeaponMounted) shooter.getEquipment(weaponAttackAction.getWeaponId()),
                                  ammoBin);
                            weaponAttackAction.setAmmoId(shooter.getEquipmentNum(ammoBin));
                            weaponAttackAction.setAmmoMunitionType(ammoBinType.getMunitionType());

                            // Get expected damage
                            exDamage = Compute.getExpectedDamage(game, weaponAttackAction, false);

                            // Calculate any modifiers due to ammo type
                            ammoMultiple = 1.0;

                            // Frag missiles, fléchette AC rounds do double damage against conventional infantry and
                            // 0 damage against everything else Any further anti-personnel specialized rounds should
                            // be tested for here
                            if ((target != null) &&
                                  (((((ammoBinType.getAmmoType() == AmmoTypeEnum.LRM)
                                        || (ammoBinType.getAmmoType() == AmmoTypeEnum.LRM_IMP)
                                        || (ammoBinType.getAmmoType() == AmmoTypeEnum.MML)
                                        || (ammoBinType.getAmmoType() == AmmoTypeEnum.SRM)
                                        || (ammoBinType.getAmmoType() == AmmoTypeEnum.SRM_IMP)))
                                        && (ammoBinType.getMunitionType().contains(AmmoType.Munitions.M_FRAGMENTATION)))
                                        || (((ammoBinType.getAmmoType() == AmmoTypeEnum.AC)
                                        || (ammoBinType.getAmmoType() == AmmoTypeEnum.LAC)
                                        || (ammoBinType.getAmmoType() == AmmoTypeEnum.AC_IMP)
                                        || (ammoBinType.getAmmoType() == AmmoTypeEnum.PAC))
                                        && (ammoBinType.getMunitionType().contains(AmmoType.Munitions.M_FLECHETTE))))) {
                                ammoMultiple = target.isConventionalInfantry() ? 2.0 : 0.0;
                            }

                            // LBX cluster rounds work better against units
                            // with little armor, vehicles, and Meks in
                            // partial cover
                            // Other ammo that deliver lots of small
                            // submunitions should be tested for here too
                            if ((target != null) && (
                                  ((ammoBinType.getAmmoType() == AmmoTypeEnum.AC_LBX)
                                        || (ammoBinType.getAmmoType() == AmmoTypeEnum.AC_LBX_THB)
                                        || (ammoBinType.getAmmoType() == AmmoTypeEnum.SBGAUSS))
                                        && (ammoBinType.getMunitionType().contains(AmmoType.Munitions.M_CLUSTER)))) {
                                if (target.getArmorRemainingPercent() <= 0.25) {
                                    ammoMultiple = 1.0 + (weaponType.getRackSize() / 10.0);
                                }
                                if (target instanceof Tank) {
                                    ammoMultiple += 1.0;
                                }
                            }

                            // AP autocannon rounds work much better against
                            // Meks and vehicles than infantry,
                            // give a damage boost in proportion to calibre
                            // to reflect scaled crit chance
                            // Other armor-penetrating ammo types should be
                            // tested here, such as Tandem-charge SRMs

                            // PLAYTEST added
                            if (((ammoBinType.getAmmoType() == AmmoTypeEnum.AC)
                                  || (ammoBinType.getAmmoType() == AmmoTypeEnum.LAC)
                                  || (ammoBinType.getAmmoType() == AmmoTypeEnum.AC_IMP)
                                  || (ammoBinType.getAmmoType() == AmmoTypeEnum.PAC))
                                  && (ammoBinType.getMunitionType().contains(AmmoType.Munitions.M_ARMOR_PIERCING)
                                  || ammoBinType.getMunitionType()
                                  .contains(AmmoType.Munitions.M_ARMOR_PIERCING_PLAYTEST))) {
                                if ((target instanceof Mek) || (target instanceof Tank)) {
                                    ammoMultiple = 1.0 + (weaponType.getRackSize() / 10.0);
                                }
                                if (target instanceof Infantry) {
                                    ammoMultiple = 0.6;
                                }
                            }

                            // Inferno SRMs work better against overheating
                            // Meks that are not/almost not on fire,
                            // and against vehicles and ProtoMeks if allowed by
                            // game option
                            if (((ammoBinType.getAmmoType() == AmmoTypeEnum.SRM)
                                  || (ammoBinType.getAmmoType() == AmmoTypeEnum.SRM_IMP)
                                  || (ammoBinType.getAmmoType() == AmmoTypeEnum.MML))
                                  && (ammoBinType.getMunitionType().contains(AmmoType.Munitions.M_INFERNO))) {
                                ammoMultiple = 0.5;
                                if (target instanceof Mek) {
                                    if ((target.infernos.getTurnsLeftToBurn() < 4)
                                          && (target.heat >= 5)) {
                                        ammoMultiple = 1.1;
                                    }
                                }
                                if ((target instanceof Tank)
                                      && !(game.getOptions()
                                      .booleanOption(
                                            OptionsConstants.ADVANCED_COMBAT_VEHICLES_SAFE_FROM_INFERNOS))) {
                                    ammoMultiple = 1.1;
                                }
                                if ((target instanceof ProtoMek)
                                      && !(game.getOptions()
                                      .booleanOption(OptionsConstants.ADVANCED_COMBAT_PROTOMEKS_SAFE_FROM_INFERNOS))) {
                                    ammoMultiple = 1.1;
                                }
                            }

                            // Narc beacon doesn't really do damage but if
                            // the target is not infantry and doesn't have
                            // one, give 'em one by making it an attractive
                            // option
                            if ((weaponType.getAmmoType() == AmmoTypeEnum.NARC)
                                  && (ammoBinType.getMunitionType().contains(AmmoType.Munitions.M_STANDARD))) {
                                if ((target != null) && !(target.isNarcedBy(shooter.getOwner().getTeam()))
                                      && !(target instanceof Infantry)) {
                                    exDamage = 5.0;
                                } else {
                                    exDamage = 0.5;
                                }
                            }

                            // iNarc beacon doesn't really do damage, but if the target is not infantry and doesn't have
                            // one, give 'em one by making it an attractive option
                            if (weaponType.getAmmoType() == AmmoTypeEnum.INARC) {
                                if ((ammoBinType.getMunitionType().contains(AmmoType.Munitions.M_STANDARD))
                                      && !(target instanceof Infantry)) {
                                    if ((target != null) && !(target.isINarcedBy(shooter.getOwner().getTeam()))) {
                                        exDamage = 7.0;
                                    } else {
                                        exDamage = 1.0;
                                    }
                                }

                                // iNarc ECM doesn't really do damage, but if the target has a C3 link or missile
                                // launchers make it a priority
                                // Checking for actual ammo types carried would be nice, but can't be sure of exact
                                // loads
                                // when "true" double-blind is implemented
                                if ((ammoBinType.getAmmoType() == AmmoTypeEnum.INARC)
                                      && (ammoBinType.getMunitionType().contains(AmmoType.Munitions.M_ECM))
                                      && !(target instanceof Infantry)) {
                                    if ((target != null) && !target.isINarcedWith(INarcPod.ECM)) {
                                        if (!(target.getC3MasterId() == Entity.NONE)
                                              || target.hasC3M()
                                              || target.hasC3MM()
                                              || target.hasC3i()) {
                                            exDamage = 8.0;
                                        } else {
                                            exDamage = 0.5;
                                        }
                                        for (Mounted<?> weapon : shooter.getWeaponList()) {
                                            targetWeaponType = (WeaponType) weapon.getType();
                                            if ((targetWeaponType.getAmmoType() == AmmoTypeEnum.LRM)
                                                  || (targetWeaponType.getAmmoType() == AmmoTypeEnum.LRM_IMP)
                                                  || (targetWeaponType.getAmmoType() == AmmoTypeEnum.MML)
                                                  || (targetWeaponType.getAmmoType() == AmmoTypeEnum.SRM)
                                                  || (targetWeaponType.getAmmoType() == AmmoTypeEnum.SRM_IMP)) {
                                                exDamage = exDamage + (targetWeaponType.getRackSize() / 2.0);
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
                                if ((ammoBinType.getAmmoType() == AmmoTypeEnum.INARC)
                                      && (ammoBinType.getMunitionType().contains(AmmoType.Munitions.M_NEMESIS))
                                      && !(target instanceof Infantry)) {
                                    if ((target != null) && !target.isINarcedWith(INarcPod.NEMESIS)) {
                                        exDamage = (double) (target.getWalkMP() + target.getAnyTypeMaxJumpMP()) / 2;
                                    } else {
                                        exDamage = 0.5;
                                    }
                                }
                            }

                            // If the adjusted damage is highest, store the
                            // damage and bin
                            if ((exDamage * ammoMultiple) > maxDamage) {
                                maxDamage = exDamage * ammoMultiple;
                                bestAmmoBin = ammoBin;
                            }
                        }
                    }
                }
            }

            // Now that the best bin has been found, reload the weapon with it
            if (bestAmmoBin != null) {
                shooter.loadWeapon((WeaponMounted) shooter.getEquipment(weaponAttackAction.getWeaponId()), bestAmmoBin);
                weaponAttackAction.setAmmoId(shooter.getEquipmentNum(bestAmmoBin));
            }

            weaponAttackAction.setAmmoMunitionType(ammoBinType.getMunitionType());
        }
        return maxDamage;
    }

    /**
     * If this is an ultra or rotary cannon, lets see about 'spinning it up' for extra damage
     *
     * @return the <code>int</code> ID of weapon mode
     */
    @Deprecated
    public static int spinUpCannon(Game cgame, WeaponAttackAction atk) {
        return spinUpCannon(cgame, atk, Compute.d6(2) - 1);
    }

    /**
     * Determine if autocannon should fire more than one round. Includes standard ACs if the game option for
     * rapid-fire-mode is enabled.
     *
     * @param atk             Attack action with weapon attack properties
     * @param spinupThreshold Maximum to-hit number to consider for rapid fire
     *
     * @return the <code>int</code> ID of weapon mode, which is also the number of mode changes from single shot
     */

    public static int spinUpCannon(Game cgame, WeaponAttackAction atk, int spinupThreshold) {

        int to_hit;
        // The number of mode changes needed to set a specific rate of fire
        int final_spin = 0;
        Entity shooter;
        Mounted<?> weapon;
        WeaponType weaponType;
        boolean isUAC = false;
        boolean isRAC = false;

        // Basic protections against null values
        if (null == atk || null == cgame || null == atk.toHit(cgame)) {
            LOGGER.warn("null parameter passed to Compute.spinUpCannon");
            return final_spin;
        }

        // Get the to-hit number for this attack
        to_hit = atk.toHit(cgame).getValue();

        // If weapon can't hit target, exit with the default mode setting
        if (to_hit > 12) {
            return final_spin;
        }

        shooter = atk.getEntity(cgame);
        weapon = shooter.getEquipment(atk.getWeaponId());
        weaponType = (WeaponType) shooter.getEquipment(atk.getWeaponId()).getType();

        // If optional rapid fire autocannons are enabled, check for conventional, LAC, and
        // PAC types
        boolean isRapidFireAC =
              cgame.getOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_RAPID_AC) &&
                    weaponType instanceof ACWeapon;

        // Anything other than a standard AC or equivalent, UAC, or RAC does not apply
        if (!isRapidFireAC) {
            isRAC = weaponType instanceof RACWeapon;
            isUAC = !isRAC && (weaponType instanceof UACWeapon);

            if (!isRAC && !isUAC) {
                return final_spin;
            }
        }

        // Set the weapon to single shot mode
        weapon.setMode(isRapidFireAC ? "" : Weapon.MODE_AC_SINGLE);

        // If the to-hit number is under or at the provided threshold, set multiple shots
        if (to_hit <= spinupThreshold) {
            final_spin = 1;
            if (isUAC) {
                weapon.setMode(Weapon.MODE_UAC_ULTRA);
            } else if (isRAC) {

                weapon.setMode(Weapon.MODE_RAC_TWO_SHOT);

                // If the to-hit number is significantly lower than the provided threshold,
                // set for either five or six shots

                if (to_hit <= (spinupThreshold - 3)) {
                    final_spin = 5;
                    weapon.setMode(Weapon.MODE_RAC_SIX_SHOT);
                    return final_spin;
                }

                if (to_hit <= (spinupThreshold - 2)) {
                    final_spin = 4;
                    weapon.setMode(Weapon.MODE_RAC_FIVE_SHOT);
                    return final_spin;
                }

                // If the to-hit number is slightly lower than the provided threshold, set for
                // four shots.  Reduce to three shots for high to-hit numbers to reduce ammo
                // use and chance of jamming.
                if (to_hit <= (spinupThreshold - 1)) {
                    final_spin = to_hit >= 6 ? 2 : 3;
                    weapon.setMode(to_hit >= 6 ? Weapon.MODE_RAC_THREE_SHOT : Weapon.MODE_RAC_FOUR_SHOT);
                    return final_spin;
                }

            } else {
                // Rapid firing standard autocannon is risky, so save it for better to-hit numbers,
                // infantry field guns, or when the 'kinder' optional rule is set
                if (to_hit <= (spinupThreshold - 2) ||
                      shooter.isConventionalInfantry() ||
                      cgame.getOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_KIND_RAPID_AC)) {
                    weapon.setMode(Weapon.MODE_AC_RAPID);
                } else {
                    final_spin = 0;
                    weapon.setMode("");
                }
            }
        }

        // Return the number of mode changes needed to set the rate of fire
        return final_spin;
    }

    /**
     * Returns true if the line between source Coords and target goes through the hex in front of the attacker
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
     *
     * @return The firing arc
     */
    public static int firingArcFromVGLFacing(int facing) {
        return VGL_FIRING_ARCS[facing % 6];
    }

    /**
     * checks to see whether the target is within visual range of the entity, but not necessarily LoS
     */
    public static boolean inVisualRange(Game game, Entity ae, Targetable target) {
        return inVisualRange(game, null, ae, target);
    }

    /**
     * Determine whether the attacking entity is within visual range of the target. This requires line of sight effects
     * to determine if there are certain intervening obstructions, like smoke, that can reduce visual range. Since
     * repeated LoSEffects computations can be expensive, it is possible to pass in the LosEffects, since they are
     * commonly already computed when this method is called.
     *
     * @param game The current {@link Game}
     */
    public static boolean inVisualRange(Game game, LosEffects los, Entity attackingEntity,
          Targetable target) {
        // Use firing solution if Advanced Sensors is on
        if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_AERO_RULES_STRATOPS_ADVANCED_SENSORS)
              && target.getTargetType() == Targetable.TYPE_ENTITY
              && game.getBoard(target).isSpace()) {
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
            teIlluminated = !IlluminationLevel.determineIlluminationLevel(game, target.getBoardId(),
                  target.getPosition()).isNone();
        }

        // if either does not have a position then return false
        if ((attackingEntity.getPosition() == null) || (target.getPosition() == null)) {
            return false;
        }

        // check visual range based on planetary conditions
        if (los == null) {
            los = LosEffects.calculateLOS(game, attackingEntity, target);
        }
        int visualRange = getVisualRange(game, attackingEntity, los, teIlluminated);

        // Check for factors that only apply to an entity target
        Coords targetPos = target.getPosition();
        if (target.getTargetType() == Targetable.TYPE_ENTITY) {
            Entity targetedEntity = (Entity) target;

            // Beyond altitude 8, on ground maps, aerospace can't spot ground units
            if (attackingEntity.isAirborneAeroOnGroundMap() && (attackingEntity.getAltitude() > 8) && !target.isAirborne()) {
                visualRange = 0;
            }

            // check for camo and null sig on the target
            if (targetedEntity.isVoidSigActive()) {
                visualRange = visualRange / 4;
            } else if (targetedEntity.hasWorkingMisc(MiscType.F_VISUAL_CAMO)) {
                visualRange = visualRange / 2;
            } else if (targetedEntity.isChameleonShieldActive()) {
                visualRange = visualRange / 2;
            } else if (targetedEntity.isConventionalInfantry() && ((Infantry) targetedEntity).hasSneakCamo()) {
                visualRange = visualRange / 2;
            }

            // Ground targets pick the closest path to Aeros (TW pg 107)
            if ((targetedEntity.isAero()) && isGroundToAir(attackingEntity, target)) {
                targetPos = Compute.getClosestFlightPath(attackingEntity.getId(),
                      attackingEntity.getPosition(), targetedEntity);
            }
        }

        // Airborne units targeting ground have special rules
        Coords attackingPos = attackingEntity.getPosition();
        if (isAirToGround(attackingEntity, target)) {
            // In Low Altitude, Airborne aerosphere can only see ground targets
            // they overfly, and only at Alt <=8. It should also spot units
            // next to this; Low-atmo board with ground units isn't implemented
            if (game.isOnAtmosphericMap(attackingEntity)) {
                if (attackingEntity.getAltitude() > 8) {
                    return false;
                }
                return attackingEntity.passedOver(target);
            }
            // On ground maps, we should consider the aircraft to be attacking from
            // the closest point on the flight path
            if (attackingEntity.isAirborneAeroOnGroundMap()) {
                attackingPos = Compute.getClosestToFlightPath(attackingEntity, targetPos);
            }
        }
        // Undoes any negative visual ranges
        visualRange = max(visualRange, 1);
        // Ground distance
        int distance = attackingPos.distance(targetPos);
        // Need to track difference in altitude, not just add altitude to the range
        distance += Math.abs(2 * target.getAltitude() - 2 * attackingEntity.getAltitude());
        return distance <= visualRange;

    }

    // Space Combat Detection stuff

    /**
     * Checks to see if an entity has already been detected by anyone Used for sensor return icons on board
     *
     * @param game     The current {@link Game}
     * @param targetId - the ID# of the target entity we're looking for
     */
    public static boolean isAnySensorContact(Game game, int targetId) {
        Entity targetEntity = game.getEntity(targetId);

        if (targetEntity == null) {
            return false;
        }

        for (Entity detector : game.getEntitiesVector()) {
            if (detector.hasSensorContactFor(targetId)) {
                targetEntity.addBeenDetectedBy(detector.getOwner());
                return true;
            }
        }
        return false;
    }

    /**
     * Checks to see if target entity has already appeared on detector's sensors Used with Naval C3 to determine if
     * detector can fire weapons at @target
     *
     * @param detector - the entity making a sensor scan
     * @param targetId - the entity id of the scan target
     */
    public static boolean hasSensorContact(Entity detector, int targetId) {
        return detector.hasSensorContactFor(targetId);
    }

    /**
     * Checks to see if an entity is in anyone's firing solutions list Used for visibility
     *
     * @param game     The current {@link Game}
     * @param targetId - the ID # of the target we're firing at
     */
    public static boolean hasAnyFiringSolution(Game game, int targetId) {
        Entity targetEntity = game.getEntity(targetId);
        if (targetEntity == null) {
            return false;
        }

        for (Entity detector : game.getEntitiesVector()) {
            if (detector.hasFiringSolutionFor(targetId)) {
                targetEntity.addBeenSeenBy(detector.getOwner());
                return true;
            }
        }
        return false;
    }

    /**
     * Calculates the ECM effects in play between a detector and target pair
     *
     * @param game            The current {@link Game}
     * @param attackingEntity - the entity making a sensor scan
     * @param target          - the entity we're trying to spot
     */
    private static int calcSpaceECM(Game game, Entity attackingEntity, Targetable target) {
        int mod = 0;
        int ecm = ComputeECM.getLargeCraftECM(attackingEntity, attackingEntity.getPosition(), target.getPosition());
        if (!attackingEntity.isLargeCraft()) {
            ecm += ComputeECM.getSmallCraftECM(attackingEntity, attackingEntity.getPosition(), target.getPosition());
        }
        ecm = min(4, ecm);
        int eccm = 0;
        if (attackingEntity.isLargeCraft()) {
            eccm = ((Aero) attackingEntity).getECCMBonus();
        }
        if (ecm > 0) {
            mod += ecm;
            if (eccm > 0) {
                mod -= (min(ecm, eccm));
            }
        }
        return mod;
    }

    /**
     * Calculates the Sensor Shadow effects in play between a detector and target pair
     *
     * @param game   The current {@link Game}
     * @param ae     the entity making a sensor scan
     * @param target the entity we're trying to spot
     */
    private static int calcSensorShadow(Game game, Entity ae, Targetable target) {
        int mod = 0;
        if (target.getTargetType() != Targetable.TYPE_ENTITY) {
            return 0;
        }
        Entity te = (Entity) target;
        for (Entity en : Compute.getAdjacentEntitiesAlongAttack(ae.getPosition(), target.getPosition(), game)) {
            if (!en.isEnemyOf(te) && en.isLargeCraft() && !en.equals(te)
                  && ((en.getWeight() - te.getWeight()) >= -100000.0)) {
                mod++;
                break;
            }
        }
        for (Entity en : game.getEntitiesVector(target.getPosition())) {
            if (!en.isEnemyOf(te) && en.isLargeCraft() && !en.equals(ae) && !en.equals(te)
                  && ((en.getWeight() - te.getWeight()) >= -100000.0)) {
                mod++;
                break;
            }
        }
        return mod;
    }

    /**
     * Updates an entity's firingSolutions, removing any objects that no longer meet criteria for being tracked as
     * targets. Also, if the detecting entity no longer meets criteria for having firing solutions, empty the list. We
     * wouldn't want a dead ship to be providing NC3 data, now would we...
     */
    public static void updateFiringSolutions(Game game, Entity detector) {
        List<Integer> toRemove = new ArrayList<>();
        // Flush the detecting unit's firing solutions if any of these conditions applies
        if (detector.isDestroyed()
              || detector.isDoomed()
              || detector.isTransported()
              || detector.isPartOfFighterSquadron()
              || detector.isOffBoard()
              || !game.hasBoardLocation(detector.getPosition(), detector.getBoardId())) {
            detector.clearFiringSolutions();
            return;
        }
        for (int id : detector.getFiringSolutions()) {
            Entity target = game.getEntity(id);
            // The target should be removed if it's off the board for any of these reasons
            if (target == null
                  || !game.onTheSameBoard(detector, target)
                  || !game.hasBoardLocation(target.getPosition(), target.getBoardId())
                  || target.isDestroyed()
                  || target.isDoomed()
                  || target.isTransported()
                  || target.isPartOfFighterSquadron()
                  || target.isOffBoard()) {
                toRemove.add(id);
                continue;
            }
            Coords targetPos = target.getPosition();
            int distance = detector.getPosition().distance(targetPos);
            // Per SO p119, optical firing solutions are lost if the target moves beyond
            // 1/10 max range
            if (detector.getActiveSensor().type() == Sensor.TYPE_AERO_THERMAL
                  && distance > Sensor.ASF_OPTICAL_FIRING_SOLUTION_RANGE) {
                toRemove.add(id);
            } else if (detector.getActiveSensor().type() == Sensor.TYPE_SPACECRAFT_THERMAL
                  && distance > Sensor.LC_OPTICAL_FIRING_SOLUTION_RANGE) {
                toRemove.add(id);
                // For ASF sensors, make sure we're using the space range of 555...
            } else if (detector.getActiveSensor().type() == Sensor.TYPE_AERO_SENSOR
                  && distance > Sensor.ASF_RADAR_MAX_RANGE) {
                toRemove.add(id);
            } else {
                // Radar firing solutions are only lost if the target moves out of range
                if (distance > detector.getActiveSensor().getRangeByBracket()) {
                    toRemove.add(id);
                }
            }
        }
        detector.removeFiringSolution(toRemove);
    }

    /**
     * Updates an entity's sensorContacts, removing any objects that no longer meet criteria for being tracked. Also, if
     * the detecting entity no longer meets criteria for having sensor contacts, empty the list. We wouldn't want a dead
     * ship to be providing sensor data, now would we...
     */
    public static void updateSensorContacts(Game game, Entity detector) {
        List<Integer> toRemove = new ArrayList<>();
        // Flush the detecting unit's sensor contacts if any of these conditions applies
        if (!game.hasBoardLocation(detector.getPosition(), detector.getBoardId())
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
            // The target should be removed if it's off the board for any of these reasons
            if (target == null
                  || !game.hasBoardLocation(target.getPosition(), target.getBoardId())
                  || !game.onTheSameBoard(detector, target)
                  || target.isDestroyed()
                  || target.isDoomed()
                  || target.getTransportId() != Entity.NONE
                  || target.isPartOfFighterSquadron()
                  || target.isOffBoard()) {
                toRemove.add(id);
                continue;
            }
            // And now calculate whether the target has moved out of range. Per SO p117-119,
            // sensor contacts remain tracked on the plotting board until this occurs.
            Coords targetPos = target.getPosition();
            int distance = detector.getPosition().distance(targetPos);
            if (distance > detector.getActiveSensor().getRangeByBracket()) {
                toRemove.add(id);
            }
        }
        detector.removeSensorContact(toRemove);
    }

    /**
     * If the game is in space, "visual range" represents a firing solution as defined in SO starting on p117 Also, in
     * most cases each target must be detected with sensors before it can be seen, so we need to make sensor rolls for
     * detection. This should only be used if TacOps sensor rules are in use. This requires line of sight effects to
     * determine if there are certain intervening obstructions, like sensor shadows, asteroids and that sort of thing,
     * that can reduce visual range. Since repeated LoSEffects computations can be expensive, it is possible to pass in
     * the LosEffects, since they are commonly already computed when this method is called.
     *
     * @param game   The current {@link Game}
     * @param ae     the entity making a sensor scan
     * @param target the entity we're trying to spot
     */
    public static boolean calcFiringSolution(Game game, Entity ae, Targetable target) {
        if (target.getTargetType() == Targetable.TYPE_ENTITY) {
            Entity te = (Entity) target;

            if (te.isOffBoard()) {
                return false;
            }
        }

        // NPE check. Fighter squadrons don't start with sensors, but pick them up from
        // the component fighters each round
        if (ae.getActiveSensor() == null) {
            return false;
        }

        // ESM sensor can't produce a firing solution
        if (ae.getActiveSensor().type() == Sensor.TYPE_SPACECRAFT_ESM) {
            return false;
        }
        Coords targetPos = target.getPosition();
        int distance = ae.getPosition().distance(targetPos);
        int roll = Compute.d6(2);
        int tn = ae.getCrew().getPiloting();
        int autoVisualRange = 1;
        int outOfVisualRange = (ae.getActiveSensor().getRangeByBracket());
        int rangeIncrement = (int) Math.ceil(outOfVisualRange / 10.0);

        // A bit of a hack here. "Aero Sensors" return the ground range, because Sensor
        // doesn't know about Game or Entity
        // to do otherwise. We need to use the space range instead.
        if (ae.getActiveSensor().type() == Sensor.TYPE_AERO_SENSOR) {
            outOfVisualRange = Sensor.ASF_RADAR_MAX_RANGE;
            rangeIncrement = Sensor.ASF_RADAR_AUTO_SPOT_RANGE;
        }

        if (distance > outOfVisualRange) {
            return false;
        }

        if (ae instanceof Aero aero) {
            // Account for sensor damage
            if (aero.isAeroSensorDestroyed()) {
                return false;
            } else {
                tn += aero.getSensorHits();
            }
        }

        // Targets at 1/10 max range are automatically detected
        if (ae.getActiveSensor().type() == Sensor.TYPE_AERO_SENSOR) {
            autoVisualRange = Sensor.ASF_RADAR_AUTO_SPOT_RANGE;
        } else if (ae.getActiveSensor().type() == Sensor.TYPE_SPACECRAFT_RADAR) {
            autoVisualRange = Sensor.LC_RADAR_AUTO_SPOT_RANGE;
        } else if (ae.getActiveSensor().type() == Sensor.TYPE_AERO_THERMAL) {
            autoVisualRange = Sensor.ASF_OPTICAL_FIRING_SOLUTION_RANGE;
        } else if (ae.getActiveSensor().type() == Sensor.TYPE_SPACECRAFT_THERMAL) {
            autoVisualRange = Sensor.LC_OPTICAL_FIRING_SOLUTION_RANGE;
        }

        if (distance <= autoVisualRange) {
            return true;
        }

        // Apply Sensor Geek SPA, if present
        if (ae.hasAbility(OptionsConstants.UNOFFICIAL_SENSOR_GEEK)) {
            tn -= 2;
        }

        // Otherwise, we add +1 to the tn for detection for each increment of the
        // auto visual range between attacker and target
        tn += (distance / rangeIncrement);

        // Apply ECM/ECCM effects
        if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_AERO_RULES_STRATOPS_ECM)) {
            tn += calcSpaceECM(game, ae, target);
        }

        // Apply large craft sensor shadows
        if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_AERO_RULES_STRATOPS_SENSOR_SHADOW)) {
            tn += calcSensorShadow(game, ae, target);
        }

        // Apply modifiers for attacker's equipment
        // -2 for a working Large NCSS
        if (ae.hasWorkingMisc(MiscType.F_LARGE_COMM_SCANNER_SUITE)) {
            tn -= 2;
        }
        // -1 for a working Small NCSS
        if (ae.hasWorkingMisc(MiscType.F_SMALL_COMM_SCANNER_SUITE)) {
            tn -= 1;
        }
        // -2 for any type of BAP or EW Equipment. ECM is already accounted for, so
        // don't let the BAP check do that
        if (ae.hasWorkingMisc(MiscType.F_EW_EQUIPMENT)
              || ae.hasBAP(false)) {
            tn -= 2;
        }

        // Now, determine if we've detected the target this round
        return roll >= tn;
    }

    public static boolean calcFutureTargetFiringSolution(Game game, Entity attacker,
          Targetable target, Coords futureTargetPosition) {
        if (target.getTargetType() == Targetable.TYPE_ENTITY) {
            Entity te = (Entity) target;

            if (te.isOffBoard()) {
                return false;
            }
        }

        // NPE check. Fighter squadrons don't start with sensors, but pick them up from
        // the component fighters each round
        if (attacker.getActiveSensor() == null) {
            return false;
        }

        // ESM sensor can't produce a firing solution
        if (attacker.getActiveSensor().type() == Sensor.TYPE_SPACECRAFT_ESM) {
            return false;
        }
        int distance = attacker.getPosition().distance(futureTargetPosition);
        int roll = Compute.d6(2);
        int tn = attacker.getCrew().getPiloting();
        int autoVisualRange = 1;
        int outOfVisualRange = (attacker.getActiveSensor().getRangeByBracket());
        int rangeIncrement = (int) Math.ceil(outOfVisualRange / 10.0);

        // A bit of a hack here. "Aero Sensors" return the ground range, because Sensor
        // doesn't know about Game or Entity
        // to do otherwise. We need to use the space range instead.
        if (attacker.getActiveSensor().type() == Sensor.TYPE_AERO_SENSOR) {
            outOfVisualRange = Sensor.ASF_RADAR_MAX_RANGE;
            rangeIncrement = Sensor.ASF_RADAR_AUTO_SPOT_RANGE;
        }

        if (distance > outOfVisualRange) {
            return false;
        }

        if (attacker instanceof Aero aero) {
            // Account for sensor damage
            if (aero.isAeroSensorDestroyed()) {
                return false;
            } else {
                tn += aero.getSensorHits();
            }
        }

        // Targets at 1/10 max range are automatically detected
        if (attacker.getActiveSensor().type() == Sensor.TYPE_AERO_SENSOR) {
            autoVisualRange = Sensor.ASF_RADAR_AUTO_SPOT_RANGE;
        } else if (attacker.getActiveSensor().type() == Sensor.TYPE_SPACECRAFT_RADAR) {
            autoVisualRange = Sensor.LC_RADAR_AUTO_SPOT_RANGE;
        } else if (attacker.getActiveSensor().type() == Sensor.TYPE_AERO_THERMAL) {
            autoVisualRange = Sensor.ASF_OPTICAL_FIRING_SOLUTION_RANGE;
        } else if (attacker.getActiveSensor().type() == Sensor.TYPE_SPACECRAFT_THERMAL) {
            autoVisualRange = Sensor.LC_OPTICAL_FIRING_SOLUTION_RANGE;
        }

        if (distance <= autoVisualRange) {
            return true;
        }

        // Apply Sensor Geek SPA, if present
        if (attacker.hasAbility(OptionsConstants.UNOFFICIAL_SENSOR_GEEK)) {
            tn -= 2;
        }

        // Otherwise, we add +1 to the tn for detection for each increment of the
        // auto visual range between attacker and target
        tn += (distance / rangeIncrement);

        // Apply ECM/ECCM effects
        if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_AERO_RULES_STRATOPS_ECM)) {
            tn += calcSpaceECM(game, attacker, target);
        }

        // Apply large craft sensor shadows
        if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_AERO_RULES_STRATOPS_SENSOR_SHADOW)) {
            tn += calcSensorShadow(game, attacker, target);
        }

        // Apply modifiers for attacker's equipment
        // -2 for a working Large NCSS
        if (attacker.hasWorkingMisc(MiscType.F_LARGE_COMM_SCANNER_SUITE)) {
            tn -= 2;
        }
        // -1 for a working Small NCSS
        if (attacker.hasWorkingMisc(MiscType.F_SMALL_COMM_SCANNER_SUITE)) {
            tn -= 1;
        }
        // -2 for any type of BAP or EW Equipment. ECM is already accounted for, so
        // don't let the BAP check do that
        if (attacker.hasWorkingMisc(MiscType.F_EW_EQUIPMENT)
              || attacker.hasBAP(false)) {
            tn -= 2;
        }

        // Now, determine if we've detected the target this round
        return roll >= tn;
    }

    /**
     * Determines whether we have an "object" detection as defined in SO's Advanced Sensors rules starting on p117
     *
     * @param game   The current {@link Game}
     * @param ae     the entity making a sensor scan
     * @param target the entity we're trying to spot
     */
    public static boolean calcSensorContact(Game game, Entity ae, Targetable target) {
        // NPE check. Fighter squadrons don't start with sensors, but pick them up from
        // the component fighters each round
        if (ae.getActiveSensor() == null) {
            return false;
        }
        Coords targetPos = target.getPosition();
        int distance = ae.getPosition().distance(targetPos);
        int roll = Compute.d6(2);
        int tn = ae.getCrew().getPiloting();
        int maxSensorRange = ae.getActiveSensor().getRangeByBracket();
        int rangeIncrement = (int) Math.ceil(maxSensorRange / 10.0);

        // A bit of a hack here. "Aero Sensors" return the ground range, because Sensor
        // doesn't know about Game or Entity
        // to do otherwise. We need to use the space range instead.
        if (ae.getActiveSensor().type() == Sensor.TYPE_AERO_SENSOR) {
            maxSensorRange = Sensor.ASF_RADAR_MAX_RANGE;
            rangeIncrement = Sensor.ASF_RADAR_AUTO_SPOT_RANGE;
        }

        if (ae instanceof Aero aero) {
            // Account for sensor damage
            if (aero.isAeroSensorDestroyed()) {
                return false;
            } else {
                tn += aero.getSensorHits();
            }
        }

        // Apply modifiers for attacker's equipment
        // -2 for a working Large NCSS. Triple the detection range.
        if (ae.hasWorkingMisc(MiscType.F_LARGE_COMM_SCANNER_SUITE)) {
            maxSensorRange *= 3;
            tn -= 2;
        }
        // -1 for a working Small NCSS. Double the detection range.
        if (ae.hasWorkingMisc(MiscType.F_SMALL_COMM_SCANNER_SUITE)) {
            maxSensorRange *= 2;
            tn -= 1;
        }
        // -2 for any type of BAP or EW Equipment. ECM is already accounted for, so
        // don't let the BAP check do that
        if (ae.hasWorkingMisc(MiscType.F_EW_EQUIPMENT)
              || ae.hasBAP(false)) {
            tn -= 2;
        }

        // Military ESM automatically detects anyone using active sensors, which
        // includes all telemissiles
        if (ae.getActiveSensor().type() == Sensor.TYPE_SPACECRAFT_ESM
              && target.getTargetType() == Targetable.TYPE_ENTITY) {
            Entity te = (Entity) target;
            return te.getActiveSensor().type() == Sensor.TYPE_AERO_SENSOR
                  || te.getActiveSensor().type() == Sensor.TYPE_SPACECRAFT_RADAR
                  || te instanceof TeleMissile;
        }

        // Can't detect anything beyond this distance
        if (distance > maxSensorRange) {
            return false;
        }

        // Apply Sensor Geek SPA, if present
        if (ae.hasAbility(OptionsConstants.UNOFFICIAL_SENSOR_GEEK)) {
            tn -= 2;
        }

        // Otherwise, we add +1 to the tn for each 1/10 of the max sensor range (rounded
        // up) between attacker and target
        tn += (distance / rangeIncrement);

        // Apply ECM/ECCM effects
        if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_AERO_RULES_STRATOPS_ECM)) {
            tn += calcSpaceECM(game, ae, target);
        }

        // Apply large craft sensor shadows
        if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_AERO_RULES_STRATOPS_SENSOR_SHADOW)) {
            tn += calcSensorShadow(game, ae, target);
        }

        // Now, determine if we've detected the target this round
        return roll >= tn;
    }

    /**
     * @return visual range in hexes along a specific line of sight
     */
    public static int getVisualRange(Game game, Entity ae, LosEffects los, boolean targetIlluminated) {
        int visualRange = game.getPlanetaryConditions().getVisualRange(ae, targetIlluminated);
        visualRange -= los.getLightSmoke();
        visualRange -= 2 * los.getHeavySmoke();
        visualRange = max(1, visualRange);
        return visualRange;
    }

    /**
     * @return visual range in hexes given current planetary conditions and no los obstruction
     */
    public static int getMaxVisualRange(Entity entity, boolean targetIlluminated) {
        Game game = entity.getGame();
        if (game == null) {
            return DEFAULT_MAX_VISUAL_RANGE;
        }

        int visualRange;
        if (entity.isSpaceborne() && entity.getGame().getOptions()
              .booleanOption(OptionsConstants.ADVANCED_AERO_RULES_STRATOPS_ADVANCED_SENSORS)) {
            visualRange = 0;
            // For squadrons. Default to the passive thermal/optical value used by component
            // fighters
            if (entity.hasETypeFlag(Entity.ETYPE_FIGHTER_SQUADRON)) {
                visualRange = Sensor.ASF_OPTICAL_FIRING_SOLUTION_RANGE;
            }
            if (entity.getActiveSensor() != null) {
                if (entity.getActiveSensor().type() == Sensor.TYPE_AERO_SENSOR) {
                    // required because the return on this from the method below is for ground maps
                    visualRange = Sensor.ASF_RADAR_AUTO_SPOT_RANGE;
                } else {
                    visualRange = (int) Math.ceil(entity.getActiveSensor().getRangeByBracket() / 10.0);
                }
            }
        } else {
            visualRange = game.getPlanetaryConditions().getVisualRange(entity, targetIlluminated);
        }
        return visualRange;
    }

    /**
     * Checks to see whether the target is within sensor range (but not necessarily LoS or visual range)
     *
     * @param allECMInfo A collection of ECMInfo for all entities, this value can be null, and it will be computed when
     *                   it's needed, however passing in the pre-computed collection is much faster
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

        // For Space games with this option, return something different
        if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_AERO_RULES_STRATOPS_ADVANCED_SENSORS)
              && target.getTargetType() == Targetable.TYPE_ENTITY
              && game.getBoard(target).isSpace()) {
            Entity te = (Entity) target;
            return hasSensorContact(ae, te.getId());
        }

        if (!game.getOptions().booleanOption(OptionsConstants.ADVANCED_TAC_OPS_SENSORS)) {
            return false;
        }

        // if either does not have a position then return false
        if ((ae.getPosition() == null) || (target.getPosition() == null)) {
            return false;
        }

        // For now, units that are not on the same board are not is sensor range
        if (!game.onTheSameBoard(ae, target)) {
            return false;
        }

        // If we have no sensors then return false
        if (ae.getActiveSensor() == null) {
            return false;
        }

        int bracket = Compute.getSensorRangeBracket(ae, target, allECMInfo);
        int range = Compute.getSensorRangeByBracket(game, ae, target, los);

        int maxSensorRange = bracket * range;
        int minSensorRange = max((bracket - 1) * range, 0);
        if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_INCLUSIVE_SENSOR_RANGE)) {
            minSensorRange = 0;
        }

        int distance = ae.getPosition().distance(target.getPosition());

        // Aeros have to check visibility to ground targets for the closest point of
        // approach along their flight path
        // Because the rules state "within X hexes of the flight path" we're using
        // ground distance so altitude doesn't screw us up
        if (isAirToGround(ae, target) && (target instanceof Entity te)) {
            distance = te.getPosition().distance(
                  getClosestFlightPath(te.getId(),
                        te.getPosition(), ae));
            return (distance > minSensorRange) && (distance <= maxSensorRange);
        }
        // This didn't work right for Aeros. Should account for the difference in
        // altitude, not just add the target's altitude to distance
        distance += Math.abs(2 * target.getAltitude() - 2 * ae.getAltitude());

        // if this is an air-to-air scan on the ground map, then divide distance by 16
        // to match weapon ranges
        // I purposely left this calculation out of visual spotting, so we should do
        // some testing with this and
        // see if it's errata-worthy. The idea is that you'll boost sensor range to help
        // find an enemy aero on the map
        // but still won't be able to see it and shoot at it beyond normal visual
        // conditions.
        if (isAirToAir(game, ae, target) && game.getBoard(ae).isGround()) {
            distance = (int) Math.ceil(distance / 16.0);
        }
        return (distance > minSensorRange) && (distance <= maxSensorRange);
    }

    /**
     * Checks to see if the target is visible to the unit, always considering sensors.
     */
    public static boolean canSee(Game game, Entity ae, Targetable target) {
        return canSee(game, ae, target, true, null, null);
    }

    /**
     * Checks to see if the target is visible to the unit, if the sensor flag is true then sensors are checked as well.
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
     * gets the sensor range bracket when detecting a particular type of target. target may be null here, which gives
     * you the bracket without target entity modifiers
     *
     * @param allECMInfo A collection of ECMInfo for all entities, this value can be null, and it will be computed when
     *                   it's needed, however passing in the pre-computed collection is much faster
     */
    public static int getSensorRangeBracket(Entity ae, Targetable target, List<ECMInfo> allECMInfo) {

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

        // if this sensor is an active probe, and it is critted, then no can see
        if (sensor.isBAP() && !ae.hasBAP(false)) {
            return 0;
        }

        // In space, sensors don't have brackets, so we should always return the range
        // for bracket 1.
        if (ae.isSpaceborne()) {
            return getSensorBracket(7);
        }

        int check = ae.getSensorCheck();
        if ((null != ae.getCrew()) && ae.hasAbility(OptionsConstants.UNOFFICIAL_SENSOR_GEEK)) {
            check -= 2;
        }
        if (null != te) {
            check += sensor.getModsForStealth(te);
            // Metal Content...
            if (ae.getGame().getOptions().booleanOption(OptionsConstants.ADVANCED_METAL_CONTENT)) {
                check += sensor.getModForMetalContent(ae, te);
            }

            check += sensor.getModForTargetECM(te, allECMInfo);

        }
        // ECM bubbles
        check += sensor.getModForECM(ae, allECMInfo);

        return getSensorBracket(check);
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
     * gets the size of the sensor range bracket when detecting a particular type of target. target may be null here,
     * which gives you the range without target entity modifiers
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

        if (!(target instanceof Entity te)) {
            return 0;
        }

        // if this sensor is an active probe, and it is critted, then no can see
        if (sensor.isBAP() && !ae.hasBAP(false)) {
            return 0;
        }

        // if we are crossing water then only mag scan will work unless we are a
        // naval vessel
        if (los.isBlockedByWater()
              && sensor.type() != Sensor.TYPE_MEK_MAG_SCAN
              && sensor.type() != Sensor.TYPE_VEE_MAG_SCAN
              && ae.getMovementMode() != EntityMovementMode.HYDROFOIL
              && ae.getMovementMode() != EntityMovementMode.NAVAL) {
            return 0;
        }

        // now get the range
        int range = sensor.getRangeByBracket();

        // adjust the range based on LOS and planetary conditions
        range = sensor.adjustRange(range, game, los);

        // If we're an airborne aero, sensor range is limited to within a few hexes of
        // the flight line against ground targets
        // TO:AR Errata forum post clarifies that ground
        // map sheet aero use ground sensor table
        if (!game.getBoard(ae).isGround() && ae.isAirborne() && !te.isAirborne()) {
            // Can't see anything if above Alt 8.
            if (ae.getAltitude() > 8) {
                range = 0;
            } else if (sensor.isBAP()) {
                // Add 1 to range for active probe of any type
                range = 2;
            } else {
                // Basic sensor range listed in errata
                range = 1;
            }
            return range;
        }

        // now adjust for anything about the target entity (size, heat, etc.)
        range = sensor.entityAdjustments(range, te, game);
        return max(range, 0);
    }

    public static int getADARangeModifier(int distance) {
        // +0 for same ground map / Low-Altitude hex
        // +2 for 1 LAH away
        // +4 for 2 LAH away
        if (distance <= 0) {
            return 0;
        }
        return (((distance - 1) / Board.DEFAULT_BOARD_HEIGHT) * 2);

    }

    public static final class SensorRangeHelper {
        public int minSensorRange;
        public int maxSensorRange;
        public int minGroundSensorRange;
        public int maxGroundSensorRange;

        public SensorRangeHelper(int minSensorRange, int maxSensorRange, int minGroundSensorRange,
              int maxGroundSensorRange) {
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
        if ((null != e.getCrew()) && e.hasAbility(OptionsConstants.UNOFFICIAL_SENSOR_GEEK)) {
            check -= 2;
        }

        int bracket = Compute.getSensorBracket(check);
        if (e.isSpaceborne()) {
            bracket = Compute.getSensorBracket(7);
        }
        int range = e.getActiveSensor().getRangeByBracket();
        int groundRange;
        if (e.getActiveSensor().isBAP()) {
            groundRange = 2;
        } else {
            groundRange = 1;
        }

        // ASF sensors change range when in space, so we do that here
        if (e.isSpaceborne()) {
            if (e.getActiveSensor().type() == Sensor.TYPE_AERO_SENSOR) {
                range = Sensor.ASF_RADAR_MAX_RANGE;
            }

            // If Aero/Spacecraft sensors are destroyed while in space, the range is 0.
            if (e.isAeroSensorDestroyed()) {
                range = 0;
            }
        }

        // Dropships using radar in an atmosphere need a range that's a bit more
        // sensible
        if (e.hasETypeFlag(Entity.ETYPE_DROPSHIP) && !e.isSpaceborne()) {
            if (e.getActiveSensor().type() == Sensor.TYPE_SPACECRAFT_RADAR) {
                range = Sensor.LC_RADAR_GROUND_RANGE;
            }
        }

        int maxSensorRange = bracket * range;
        int minSensorRange = max((bracket - 1) * range, 0);
        int maxGroundSensorRange = bracket * groundRange;
        int minGroundSensorRange = max((maxGroundSensorRange - 1), 0);

        if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_INCLUSIVE_SENSOR_RANGE)) {
            minSensorRange = 0;
            minGroundSensorRange = 0;
        }

        return new SensorRangeHelper(minSensorRange, maxSensorRange, minGroundSensorRange, maxGroundSensorRange);
    }

    /**
     * Compares the initiative of two aerospace units in the same hex to determine attack angle. The attack angle is
     * computed as if the unit with the higher initiative were in its previous hex.
     *
     * @param e1 The first <code>Entity</code> to compare
     * @param e2 The second <code>Entity</code> to compare
     *
     * @return &lt; 0 if the first unit has a higher initiative, &gt; 0 if the second is higher, or 0 if one of the
     *       units is not an aerospace unit, does not have a valid position, or the two units are not in the same hex.
     */
    public static int shouldMoveBackHex(Entity e1, Entity e2) {
        if (null == e1.getPosition()
              || null == e2.getPosition()
              || e1.getBoardId() != e2.getBoardId()
              || !e1.getPosition().equals(e2.getPosition())
              || !e1.isAero()
              || !e2.isAero()) {
            return 0;
        }

        int retVal = e1.getUnitType() - e2.getUnitType();
        if (retVal == 0) {
            retVal = ((IAero) e2).getCurrentVelocity() - ((IAero) e1).getCurrentVelocity();
        }
        // if all criteria are the same, select randomly
        if (retVal == 0) {
            retVal = d6() < 4 ? -1 : 1;
        }
        return retVal;
    }

    /**
     * Maintain backwards compatibility.
     *
     * @param missiles - the <code>int</code> number of missiles in the pack.
     */
    public static int missilesHit(int missiles) {
        return missilesHit(missiles, 0);
    }

    /**
     * Maintain backwards compatability.
     */
    public static int missilesHit(int missiles, int nMod) {
        return missilesHit(missiles, nMod, false);
    }

    /**
     * Maintain backwards compatability.
     */
    public static int missilesHit(int missiles, int nMod, boolean hotLoaded) {
        return Compute.missilesHit(missiles, nMod, hotLoaded, false, false);
    }

    /**
     * Roll the number of missiles (or whatever) on the missile hit table, with the specified mod to the roll.
     *
     * @param missiles    - the <code>int</code> number of missiles in the pack.
     * @param nMod        - the <code>int</code> modifier to the roll for number of missiles that hit.
     * @param hotLoaded   - roll 3d6 take worst 2
     * @param streak      - force a roll of 11 on the cluster table
     * @param advancedAMS - the roll can now go below 2, indicating no damage
     */
    public static int missilesHit(int missiles, int nMod, boolean hotLoaded, boolean streak, boolean advancedAMS) {
        int nRoll = Compute.d6(2);

        if (hotLoaded) {
            int roll1 = Compute.d6();
            int roll2 = Compute.d6();
            int roll3 = Compute.d6();
            int lowRoll1;
            int lowRoll2;

            if ((roll1 <= roll2) && (roll1 <= roll3)) {
                lowRoll1 = roll1;
                lowRoll2 = min(roll2, roll3);
            } else if ((roll2 <= roll1) && (roll2 <= roll3)) {
                lowRoll1 = roll2;
                lowRoll2 = min(roll1, roll3);
            } else {
                lowRoll1 = roll3;
                lowRoll2 = min(roll2, roll1);
            }
            nRoll = lowRoll1 + lowRoll2;
        }
        if (streak) {
            nRoll = 11;
        }
        nRoll += nMod;
        if (!advancedAMS) {
            nRoll = min(max(nRoll, 2), 12);
        } else {
            nRoll = min(nRoll, 12);
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
                      hotLoaded, streak, advancedAMS);
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
     * @param hit - the <code>int</code> number of the crew hit currently being rolled.
     *
     * @return The <code>int</code> number that must be rolled on 2d6 for the crew to stay conscious.
     */
    public static int getConsciousnessNumber(int hit) {
        return switch (hit) {
            case 0 -> 2;
            case 1 -> 3;
            case 2 -> 5;
            case 3 -> 7;
            case 4 -> 10;
            case 5 -> 11;
            default -> Integer.MAX_VALUE;
        };
    }

    /**
     * Check for ferrous metal content in terrain on path from a to b return the total content.
     */
    public static int getMetalInPath(Entity ae, Coords a, Coords b) {
        // If we're in space, or anything is null... get out.
        if ((ae == null) || (a == null) || (b == null)) {
            return 0;
        }
        Board board = ae.getGame().getBoard();
        if (board.isSpace()) {
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
     * Check for ECM bubbles in Ghost Target mode along the path from a to b and return the highest target roll. -1 if
     * no Ghost Targets
     */
    public static int getGhostTargetNumber(Entity ae, Coords a, Coords b) {
        if (ae.getGame().getBoard().isSpace()) {
            // ghost targets don't work in space
            return 0;
        }
        if ((a == null) || (b == null)) {
            return 0;
        }

        // Only grab enemies with active ECM
        // need to create two hash tables for ghost targeting, one with mods
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
        // loop through all intervening coords, if they are not ECM'd by
        // friendly's then add any Ghost Targets
        // to the hash list
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
                    ecmStatus -= (int) Math.round(strength);
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
        return min(4, highestMod + totalGT);
    }

    /**
     * Get the base to-hit number of a space bomb attack by the given attacker upon the given defender
     *
     * @param attacker - the <code>Entity</code> conducting the leg attack.
     * @param defender - the <code>Entity</code> being attacked.
     *
     * @return The base <code>ToHitData</code> of the attack.
     */
    public static ToHitData getSpaceBombBaseToHit(Entity attacker,
          Entity defender, Game game) {
        int base = TargetRoll.IMPOSSIBLE;
        StringBuilder reason = new StringBuilder();

        if (!attacker.isAero()) {
            return new ToHitData(base, "attacker is not an Aero");
        }

        IAero a = (IAero) attacker;

        // the fighters nose must be aligned with its direction of travel
        boolean rightFacing = !game.useVectorMove();
        // using normal movement, I think this means that the last move can't be
        // a turn
        // for advanced movement, it must be aligned with the largest vector
        if (game.useVectorMove()) {
            for (int h : attacker.getHeading()) {
                if (h == attacker.getFacing()) {
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
                    break;
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
        } else if (attacker.getBombs(AmmoType.F_SPACE_BOMB).isEmpty()) {
            reason.append("the attacker has no usable bombs");
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
        else if (defender.getWeight() < 10000) {
            reason.append("the defender weighs less than 10,000 tons");
        }

        // ok if we are still alive then lets calculate the to hit
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
        if ((defender instanceof SpaceStation) || (defender.getWalkMP() == 0)
              || (defender.braceLocation() != Entity.LOC_NONE)) {
            toHit.addModifier(-4, "immobile");
        }
        if (defender.getWeight() < 100000) {
            int penalty = (int) Math.ceil((100000 - defender.getWeight()) / 10000);
            toHit.addModifier(penalty, "defender weight");
        }

        return toHit;
    }

    /**
     * This assembles attack roll modifiers for infantry swarm and leg attacks.
     */
    private static ToHitData getAntiMekMods(ToHitData data, Infantry attacker,
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
                  // because we underwater capable BAs can only jump via
                  // mechanical jump boosters
                  // otherwise, normal JJs give the same MP and do not have
                  // this restriction
                  && ((attacker.getOriginalJumpMP() == 0) || (attacker
                  .getMovementMode() == EntityMovementMode.INF_UMU))
                  && (attacker.moved == EntityMovementType.MOVE_JUMP)) {
                data.addModifier(
                      TargetRoll.IMPOSSIBLE,
                      "can't jump using mechanical jump booster and anti-mek attack in the same turn");
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

        if ((defender instanceof Mek) && ((Mek) defender).isIndustrial()) {
            data.addModifier(-1, "targeting industrial mek");
        }

        // protected/exposed actuator quirk may adjust target roll
        if (defender.hasQuirk(OptionsConstants.QUIRK_POS_PRO_ACTUATOR)) {
            data.addModifier(+1, "protected actuators");
        }
        if (defender.hasQuirk(OptionsConstants.QUIRK_NEG_EXP_ACTUATOR)) {
            data.addModifier(-1, "exposed actuators");
        }

        // Prosthetic enhancement anti-Mek bonus (Grappler or Climbing Claws) - IO p.84
        // Uses the best (most negative) modifier from either enhancement slot
        // Only applies if the unit has the MD_PL_ENHANCED or MD_PL_I_ENHANCED ability
        if (attacker.hasProstheticEnhancement()
              && (attacker.hasAbility(OptionsConstants.MD_PL_ENHANCED)
              || attacker.hasAbility(OptionsConstants.MD_PL_I_ENHANCED))) {
            int antiMekMod = attacker.getBestProstheticAntiMekModifier();
            if (antiMekMod != 0) {
                String modName = attacker.getBestProstheticAntiMekName();
                data.addModifier(antiMekMod,
                      modName != null ? modName : Messages.getString("Compute.ProstheticEnhancement"));
            }
        }

        // Enhanced Imaging bonus for anti-Mek attacks - IO p.69
        // "All Piloting Skill rolls required for the EI-equipped unit receives a -1
        // target number modifier. This includes checks made for physical attacks,
        // as well as anti-Mek attacks by EI-equipped battle armor."
        if (attacker.hasActiveEiCockpit()) {
            data.addModifier(-1, Messages.getString("Compute.EnhancedImaging"));
        }

        // swarm/leg attacks take target movement mods into account
        data.append(getTargetMovementModifier(attacker.getGame(), defender.getId()));

        return data;
    }

    /**
     * Get the base to-hit number of a Leg Attack by the given attacker upon the given defender
     *
     * @param attacker - the <code>Entity</code> conducting the leg attack.
     * @param defender - the <code>Entity</code> being attacked.
     *
     * @return The base <code>ToHitData</code> of the attack.
     */
    public static ToHitData getLegAttackBaseToHit(Entity attacker, Entity defender, Game game) {
        String reason = "Non Infantry not allowed to do AM attacks.";
        ToHitData toReturn = null;

        boolean alreadyPerformingOther = false;

        for (Enumeration<EntityAction> actions = game.getActions(); actions.hasMoreElements(); ) {
            EntityAction ea = actions.nextElement();
            if (ea instanceof WeaponAttackAction weaponAttackAction) {
                Entity waaAE = weaponAttackAction.getEntity(game);
                if ((waaAE != null) && waaAE.equals(attacker)) {
                    // impossible if already doing a swarm attack
                    if (weaponAttackAction.getEntity(game).getEquipment(weaponAttackAction.getWeaponId())
                          .getType().getInternalName()
                          .equals(Infantry.SWARM_MEK)) {
                        alreadyPerformingOther = true;

                    }
                }
            }
        }

        if (alreadyPerformingOther) {
            reason = "already performing a swarm attack";
        } else if (!(defender instanceof Mek)) {
            // Can only attack a Mek's legs.
            reason = "Defender is not a Mek.";
        } else if (attacker.getElevation() > defender.getElevation()) {
            // Can't attack if flying
            reason = "Cannot do leg attack while flying.";
        } else if (attacker instanceof BattleArmor inf) {
            // Handle BattleArmor attackers.

            toReturn = new ToHitData(inf.getCrew().getPiloting(),
                  "anti-mek skill",
                  ToHitData.HIT_KICK,
                  ToHitData.SIDE_FRONT);
            int men = inf.getShootingStrength();
            int modifier = TargetRoll.IMPOSSIBLE;
            if (men >= 4) {
                modifier = 0;
            } else if (men == 3) {
                modifier = 2;
            } else if (men == 2) {
                modifier = 5;
            } else if (men == 1) {
                modifier = 7;
            }
            toReturn.addModifier(modifier, men + " trooper(s) active");
        } else if (attacker instanceof Infantry inf) {
            // Non-BattleArmor infantry need many more men.
            toReturn = new ToHitData(inf.getCrew().getPiloting(),
                  "anti-mek skill", ToHitData.HIT_KICK, ToHitData.SIDE_FRONT);
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

        if (defender instanceof Mek && ((Mek) defender).hasTracks()) {
            if (toReturn != null) {
                toReturn.addModifier(-2, "has tracks");
            }
        }

        // If the swarm is impossible, ToHitData wasn't created
        if (toReturn == null) {
            toReturn = new ToHitData(TargetRoll.IMPOSSIBLE, reason, ToHitData.HIT_KICK, ToHitData.SIDE_FRONT);
        }

        if (toReturn.getValue() == TargetRoll.IMPOSSIBLE) {
            return toReturn;
        }

        if (attacker instanceof Infantry inf) {
            return Compute.getAntiMekMods(toReturn, inf, defender);
        }

        return new ToHitData(TargetRoll.IMPOSSIBLE, "all other checks failed - ideally should not happen");
    }

    /**
     * Get the base to-hit number of a Swarm Mek by the given attacker upon the given defender.
     *
     * @param attacker - the <code>Entity</code> swarming.
     * @param defender - the <code>Entity</code> being swarmed.
     *
     * @return The base <code>ToHitData</code> of the mek.
     */
    public static ToHitData getSwarmMekBaseToHit(Entity attacker,
          Entity defender, Game game) {
        ToHitData toReturn = null;
        String reason = "Non Infantry not allowed to do AM attacks.";

        boolean alreadyPerformingOther = false;
        for (Enumeration<EntityAction> actions = game.getActions(); actions.hasMoreElements(); ) {
            EntityAction ea = actions.nextElement();
            if (ea instanceof WeaponAttackAction waa) {
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
        else if (!(defender instanceof Mek) && !(defender instanceof Tank)) {
            reason = "Defender is not a Mek or vehicle.";
        }
        // Can't swarm a friendly Mek.
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
        else if (attacker instanceof BattleArmor inf) {
            toReturn = new ToHitData(inf.getCrew().getPiloting(), "anti-mek skill");
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
        else if (attacker instanceof Infantry inf) {
            toReturn = new ToHitData(inf.getCrew().getPiloting(), "anti-mek skill");
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
            toReturn = new ToHitData(TargetRoll.IMPOSSIBLE, reason);
        }
        if (toReturn.getValue() == TargetRoll.IMPOSSIBLE) {
            return toReturn;
        }

        // Apply shared anti-mek modifiers (includes isBurdened check for BA)
        if (attacker instanceof Infantry inf) {
            toReturn = Compute.getAntiMekMods(toReturn, inf, defender);
            if (toReturn.getValue() == TargetRoll.IMPOSSIBLE) {
                return toReturn;
            }
        }

        // If the attacker has assault claws, give a -1 modifier.
        // We can stop looking when we find our first match.
        for (Mounted<?> mount : attacker.getMisc()) {
            EquipmentType equip = mount.getType();
            if (equip.hasFlag(MiscType.F_MAGNET_CLAW)) {
                toReturn.addModifier(-1, "attacker has magnetic claws");
                break;
            }
        }
        return toReturn;
    }

    public static boolean canPhysicalTarget(Game game, int entityId, Targetable target) {

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

        if ((game.getEntity(entityId) instanceof QuadMek)
              && ((KickAttackAction.toHit(game, entityId, target,
              KickAttackAction.LEFT_MULE).getValue() != TargetRoll.IMPOSSIBLE) ||
              (KickAttackAction
                    .toHit(game, entityId, target,
                          KickAttackAction.RIGHT_MULE)
                    .getValue() != TargetRoll.IMPOSSIBLE))) {
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

        if (ProtoMekPhysicalAttackAction.toHit(game, entityId, target)
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

        Entity entityWithClubs = game.getEntity(entityId);
        if (entityWithClubs != null) {
            for (Mounted<?> club : entityWithClubs.getClubs()) {
                if (null != club) {
                    if (ClubAttackAction.toHit(game, entityId, target, club,
                          ToHitData.HIT_NORMAL, false).getValue() != TargetRoll.IMPOSSIBLE) {
                        return true;
                    }
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

        if (PheromoneAttackAction.toHit(game, entityId, target).getValue() != TargetRoll.IMPOSSIBLE) {
            return true;
        }

        return ToxinAttackAction.toHit(game, entityId, target).getValue() != TargetRoll.IMPOSSIBLE;
    }

    /**
     * Can movement between the two coordinates be on pavement (which includes roads and bridges)? If so it will
     * override prohibited terrain, it may change movement costs, and it may lead to skids.
     *
     * @param srcHex   the {@link Hex} being left.
     * @param destHex  the {@link Hex} being entered.
     * @param moveStep the {@link MoveStep} being performed.
     *
     * @return <code>true</code> if movement between <code>srcHex</code> and
     *       <code>destHex</code> can be on pavement; <code>false</code> otherwise.
     */
    public static boolean canMoveOnPavement(final Hex srcHex, final Hex destHex, MoveStep moveStep) {
        final Coords src = srcHex.getCoords();
        final Coords dest = destHex.getCoords();
        final int src2destDir = src.direction(dest);
        final int dest2srcDir = (src2destDir + 3) % 6;
        boolean result = false;

        // Jumping shouldn't be considered to be moving on pavement
        if (moveStep.isJumping()) {
            return false;
        }

        // We may be moving in the same hex.
        if (src.equals(dest)
              && (srcHex.hasPavement())) {
            result = true;
        }
        // If the source is a pavement hex, then see if the destination
        // hex is also a pavement hex or has a road or bridge that exits
        // into the source hex and the entity is climbing onto the bridge.
        else if (srcHex.containsTerrain(Terrains.PAVEMENT)
              && (destHex.containsTerrain(Terrains.PAVEMENT)
              || (destHex.containsTerrainExit(Terrains.ROAD,
              dest2srcDir)
              && destHex.hasPavedRoad())
              || (destHex.containsTerrainExit(
              Terrains.BRIDGE, dest2srcDir) && moveStep.climbMode()))) {
            result = true;
        }
        // See if the source hex has a road or bridge (and the entity is on the
        // bridge) that exits into the destination hex, and the dest hex has
        // pavement or a corresponding exit to the src hex
        else if (((srcHex.containsTerrainExit(Terrains.ROAD, src2destDir) && srcHex.hasPavedRoad())
              || (srcHex.containsTerrainExit(Terrains.BRIDGE, src2destDir)
              && (moveStep.getElevation() == srcHex
              .terrainLevel(Terrains.BRIDGE_ELEV))))
              && ((destHex.containsTerrainExit(Terrains.ROAD, dest2srcDir) && destHex.hasPavedRoad())
              || (destHex.containsTerrainExit(Terrains.BRIDGE,
              dest2srcDir) && moveStep.climbMode())
              || destHex
              .containsTerrain(Terrains.PAVEMENT))) {
            result = true;
        }

        return result;
    }

    /**
     * Can movement between the two coordinates be on pavement (which includes roads and bridges)? If so it will
     * override prohibited terrain, it may change movement costs, and it may lead to skids.
     *
     * @param game     The current {@link Game}
     * @param src      the <code>Coords</code> being left.
     * @param dest     the <code>Coords</code> being entered.
     * @param moveStep the {@link MoveStep} being performed.
     *
     * @return <code>true</code> if movement between <code>src</code> and
     *       <code>dest</code> can be on pavement; <code>false</code> otherwise.
     */
    public static boolean canMoveOnPavement(Game game, Coords src,
          Coords dest, MoveStep moveStep) {
        final Hex srcHex = game.getBoard(moveStep.getBoardId()).getHex(src);
        final Hex destHex = game.getBoard(moveStep.getBoardId()).getHex(dest);
        return canMoveOnPavement(srcHex, destHex, moveStep);
    }

    /**
     * Determines whether the attacker and the target are in the same building.
     *
     * @return true if the target can and does occupy the same building, false otherwise.
     */
    public static boolean isInSameBuilding(Game game, Entity attacker, Targetable target) {
        if (!(target instanceof Entity targetEntity)) {
            return false;
        }
        if (!isInBuilding(game, attacker) || !isInBuilding(game, targetEntity)) {
            return false;
        }

        IBuilding attackingBuilding = game.getBoard().getBuildingAt(attacker.getPosition());
        IBuilding targetBuilding = game.getBoard().getBuildingAt(target.getPosition());
        return attackingBuilding.equals(targetBuilding);
    }

    /**
     * Determine if the given unit is inside a building at the given coordinates.
     *
     * @param game   The current {@link Game}. This value may be <code>null</code>.
     * @param entity the <code>Entity</code> to be checked. This value may be
     *               <code>null</code>.
     *
     * @return <code>true</code> if the entity is inside the building at
     *       those coordinates. <code>false</code> if there is no building at those coordinates or if the entity is on
     *       the roof or in the air above the building, or if any input argument is <code>null</code>
     */
    public static boolean isInBuilding(@Nullable Game game, @Nullable Entity entity) {
        return (entity != null) && isInBuilding(game, entity, entity.getPosition());
    }

    /**
     * Determine if the given unit is inside a building at the given coordinates.
     *
     * @param game   The current {@link Game}. This value may be <code>null</code>.
     * @param entity the <code>Entity</code> to be checked. This value may be
     *               <code>null</code>.
     * @param coords the <code>Coords</code> of the building hex. This value may be
     *               <code>null</code>.
     *
     * @return <code>true</code> if the entity is inside the building at
     *       those coordinates. <code>false</code> if there is no building at those coordinates or if the entity is on
     *       the roof or in the air above the building, or if any input argument is <code>null</code>
     */
    public static boolean isInBuilding(@Nullable Game game, @Nullable Entity entity, @Nullable Coords coords) {
        return (game != null) && (entity != null) && (coords != null)
              && isInBuilding(game, entity.getElevation(), coords, entity.getBoardId());
    }

    /**
     * Returns true when the given place is inside a building, false when the location is not on a board, has no
     * building or the elevation is below the basement or on or above the building. Note that it is safe to pass in any
     * values for elevation, coords and board ID.
     *
     * @param game      The game (not null)
     * @param elevation The elevation to test
     * @param coords    The coords of the location
     * @param boardId   The board ID of the location
     *
     * @return True if the given place is inside a building
     */
    public static boolean isInBuilding(@Nullable Game game, int elevation, @Nullable Coords coords, int boardId) {
        if ((game == null) || !game.hasBoardLocation(coords, boardId)) {
            return false;
        }
        final Hex hex = game.getBoard(boardId).getHex(coords);

        if (!hex.containsTerrain(Terrains.BLDG_ELEV)) {
            return false;
        }

        // Get the elevations occupied by the building.
        int buildingHeight = hex.terrainLevel(Terrains.BLDG_ELEV);
        int basementDepth = 0;
        if (hex.containsTerrain(Terrains.BLDG_BASEMENT_TYPE)) {
            basementDepth = BasementType.getType(hex.terrainLevel(Terrains.BLDG_BASEMENT_TYPE)).getDepth();
        }
        // When the elevation is equal to the building's height, the unit stands on top (not inside)
        return (elevation >= -basementDepth) && (elevation < buildingHeight);
    }

    /**
     * Scatter from hex according to dive-bombing rules (based on MoF), TW pg 246. The scatter can happen in any
     * direction.
     *
     * @param coords The <code>Coords</code> to scatter from
     * @param moF    The margin of failure, which determines scatter distance
     *
     * @return the <code>Coords</code> scattered to and distance (moF)
     */
    public static Coords scatterDiveBombs(Coords coords, int moF) {
        return Compute.scatter(coords, moF);
    }

    /**
     * Scatter from hex according to altitude bombing rules (based on MoF), TW pg 246. The scatter only happens in the
     * "front" three facings.
     *
     * @param coords The <code>Coords</code> to scatter from
     * @param facing Direction we were going at the time the bomb was dropped
     * @param moF    How badly we failed
     *
     * @return the <code>Coords</code> scattered to and distance (moF)
     */
    public static Coords scatterAltitudeBombs(Coords coords, int facing, int moF) {
        int dir = 0;
        int scatterDirection = Compute.d6(1);
        dir = switch (scatterDirection) {
            case 1, 2 -> (facing - 1) % 6;
            case 3, 4 -> facing;
            case 5, 6 -> (facing + 1) % 6;
            default -> dir;
        };

        return coords.translated(dir, moF);
    }

    /**
     * scatter from hex according to direct fire artillery rules (based on MoF)
     *
     * @param coords The <code>Coords</code> to scatter from
     * @param moF    The margin of failure, which determines scatter distance
     *
     * @return the <code>Coords</code> scattered to
     */
    public static Coords scatterDirectArty(Coords coords, int moF) {
        return Compute.scatter(coords, moF);
    }

    /**
     * scatter from a hex according, roll d6 to choose scatter direction
     *
     * @param coords The <code>Coords</code> to scatter from
     * @param margin the <code>int</code> margin of failure, scatter distance will be the margin of failure
     *
     * @return the <code>Coords</code> scattered to
     */
    public static Coords scatter(Coords coords, int margin) {
        int scatterDirection = Compute.d6(1) - 1;
        return coords.translated(scatterDirection, margin);
    }

    /**
     * scatter from hex according to atmospheric drop rules d6 for direction, 1d6 per point of MOF
     *
     * @param coords The <code>Coords</code> to scatter from
     * @param margin the <code>int</code> margin of failure
     *
     * @return the <code>Coords</code> scattered to
     */
    public static Coords scatterAssaultDrop(Coords coords, int margin) {
        int scatterDirection = Compute.d6(1) - 1;
        int distance = Compute.d6(margin);
        return coords.translated(scatterDirection, distance);
    }

    /**
     * Gets a new target for a flight of swarm missiles that was just shot at an entity and has missiles left
     *
     * @param game     The current {@link Game}
     * @param aeId     The attacking <code>Entity</code>
     * @param weaponId The <code>int</code> ID of the launcher used to fire this volley
     *
     * @return the new target <code>Entity</code>. May return null if no new target available
     */
    public static @Nullable Entity getSwarmMissileTarget(Game game, int aeId, Coords coords,
          int weaponId) {
        Entity tempEntity;
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

    public static @Nullable Coords getFinalPosition(Coords currentPosition, int... v) {
        if ((v == null) || (v.length != 6) || (currentPosition == null)) {
            return currentPosition;
        }

        // step through each vector and move the direction indicated
        int thrust;
        Coords endPosition = currentPosition;
        for (int dir = 0; dir < 6; dir++) {
            thrust = v[dir];
            while (thrust > 0) {
                endPosition = endPosition.translated(dir);
                thrust--;
            }
        }

        return endPosition;
    }

    /**
     * method to change a set of active vectors for a one-point thrust expenditure in the giving facing
     */
    public static int[] changeVectors(int[] v, int facing) {

        if ((v == null) || (v.length != 6)) {
            return v;
        }

        // first look at opposing vectors
        int opposingVectors = facing + 3;
        if (opposingVectors > 5) {
            opposingVectors -= 6;
        }
        // is this vector active
        if (v[opposingVectors] > 0) {
            // then decrement it by one and return
            v[opposingVectors]--;
            return v;
        }

        // now check oblique vectors
        int obliqueVectors1 = facing + 2;
        if (obliqueVectors1 > 5) {
            obliqueVectors1 -= 6;
        }
        int obliqueVectors2 = facing - 2;
        if (obliqueVectors2 < 0) {
            obliqueVectors2 += 6;
        }

        // check both of these and if either is active
        // deal with it and then return
        if ((v[obliqueVectors1] > 0) || (v[obliqueVectors2] > 0)) {

            int newFacing = facing + 1;
            if (newFacing > 5) {
                newFacing = 0;
            }
            if (v[obliqueVectors1] > 0) {
                v[obliqueVectors1]--;
                v[newFacing]++;
            }

            newFacing = facing - 1;
            if (newFacing < 0) {
                newFacing = 5;
            }
            if (v[obliqueVectors2] > 0) {
                v[obliqueVectors2]--;
                v[newFacing]++;
            }
            return v;
        }

        // if nothing was found, then just increase velocity in this vector
        v[facing]++;
        return v;
    }

    /**
     * compare two vectors and determine if they are the same
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
     * Get the net velocity of two aerospace for ramming attacks
     */
    public static int getNetVelocity(Coords src, Entity te, int attackerVelocity, int targetVelocity) {
        int angle = te.sideTableRam(src);

        return switch (angle) {
            case Aero.RAM_TOWARD_DIR -> max(attackerVelocity + targetVelocity, 1);
            case Aero.RAM_TOWARD_OBL -> max(attackerVelocity + (targetVelocity / 2), 1);
            case Aero.RAM_AWAY_OBL -> max(attackerVelocity - (targetVelocity / 2), 1);
            case Aero.RAM_AWAY_DIR -> max(attackerVelocity - targetVelocity, 1);
            default -> 0;
        };
    }

    /**
     * Returns how much damage a weapon will do against a BattleArmor target if the BattleArmor vs BattleArmor rules on
     * TO pg 109 are in effect.
     *
     * @param damage     Original weapon damage
     * @param damageType The damage type for BA vs BA damage
     * @param target     The target, used for ensuring the target BA isn't fire-resistant
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
                    damage = 2 + Compute.d6(1);
                }
                break;
        }
        damage = Math.ceil(damage);
        return (int) damage;
    }

    /**
     * Used to get a human-readable string that represents the passed damage type.
     *
     * @param damageType      The damageType constant
     * @param burstMultiplier The multiplier for burst damage, used by machine gun arrays against conventional infantry
     *
     * @return A string representation of the damage type
     */
    public static String getDamageTypeString(int damageType, int burstMultiplier) {
        return switch (damageType) {
            case WeaponType.WEAPON_DIRECT_FIRE -> Messages.getString("WeaponType.DirectFire");
            case WeaponType.WEAPON_CLUSTER_BALLISTIC -> Messages.getString("WeaponType.BallisticCluster");
            case WeaponType.WEAPON_PULSE -> Messages.getString("WeaponType.Pulse");
            case WeaponType.WEAPON_CLUSTER_MISSILE,
                 WeaponType.WEAPON_CLUSTER_MISSILE_1D6,
                 WeaponType.WEAPON_CLUSTER_MISSILE_2D6,
                 WeaponType.WEAPON_CLUSTER_MISSILE_3D6 -> Messages.getString("WeaponType.Missile");
            case WeaponType.WEAPON_BURST_HALF_D6 -> Messages.getString("WeaponType.BurstHalf");
            default -> String.format("%s (%dD6)", Messages.getString("WeaponType.Burst"),
                  burstMultiplier * (damageType - WeaponType.WEAPON_BURST_HALF_D6));
        };
    }

    public static int directBlowInfantryDamage(double damage, int mos,
          int damageType, boolean isNonInfantryAgainstMechanized,
          boolean isAttackThruBuilding) {
        return directBlowInfantryDamage(damage, mos, damageType,
              isNonInfantryAgainstMechanized, isAttackThruBuilding,
              Entity.NONE, null);
    }

    /**
     * Method replicates the Non-Conventional Damage against Infantry damage table as well as shifting for direct blows.
     * also adjust for non-infantry damaging mechanized infantry
     */
    public static int directBlowInfantryDamage(double damage, int mos, int damageType,
          boolean isNonInfantryAgainstMechanized, boolean isAttackThruBuilding, int attackerId,
          Vector<Report> vReport) {
        return directBlowInfantryDamage(damage, mos, damageType, isNonInfantryAgainstMechanized,
              isAttackThruBuilding, attackerId, vReport, 1);
    }

    /**
     * @return the maximum damage that a set of weapons can generate.
     */
    public static int computeTotalDamage(List<WeaponMounted> weaponList) {
        return weaponList.stream().map(Compute::computeTotalDamage).mapToInt(e -> e).sum();
    }


    /**
     * @return the maximum damage that a weapon can generate.
     */
    public static int computeTotalDamage(WeaponMounted weapon) {
        int totalDmg = 0;
        if (weapon.isBombMounted() || !weapon.isCrippled()) {
            WeaponType type = weapon.getType();
            if (type.getDamage() == WeaponType.DAMAGE_VARIABLE) {
                // Estimate rather than compute exact bay / trooper damage sum.
                totalDmg += type.getRackSize();
            } else if (type.getDamage() == WeaponType.DAMAGE_ARTILLERY
                  || type.getDamage() == WeaponType.DAMAGE_BY_CLUSTER_TABLE) {
                totalDmg += type.getRackSize();
            } else if (type.getDamage() == WeaponType.DAMAGE_SPECIAL) {// Handle dive bomb attacks here!
                if (type instanceof DiveBombAttack) {
                    totalDmg += weapon.getEntity().getBombs().stream().mapToInt(Mounted::getExplosionDamage).sum();
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
     * Method replicates the Non-Conventional Damage against Infantry damage table as well as shifting for direct blows.
     * also adjust for non-infantry damaging mechanized infantry
     *
     * @param damage                         The base amount of damage
     * @param mos                            The margin of success
     * @param damageType                     The damage class of the weapon, used to adjust damage against infantry
     * @param isNonInfantryAgainstMechanized Whether this is a non-infantry attack against mechanized infantry
     * @param isAttackThruBuilding           Whether the attack is coming through a building hex
     * @param attackerId                     The entity id of the attacking unit
     * @param vReport                        The report messages vector
     * @param mgaSize                        For machine gun array attacks, the number of linked weapons. For other
     *                                       weapons this should be 1.
     *
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
            case WeaponType.WEAPON_BURST_HALF_D6:
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
            if (damageType < WeaponType.WEAPON_BURST_HALF_D6) {
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
     * @return new damage
     */
    public static int dialDownDamage(Mounted<?> weapon, WeaponType weaponType) {
        return Compute.dialDownDamage(weapon, weaponType, 1);
    }

    /**
     * Method computes how much damage a dial down weapon has done
     *
     * @return new damage
     */
    public static int dialDownDamage(Mounted<?> weapon, WeaponType weaponType, int range) {
        int toReturn = weaponType.getDamage(range);

        if (!weapon.hasModes()) {
            return toReturn;
        }

        String damage = weapon.curMode().getName().toLowerCase();

        // Vehicle flamers have damage and heat modes so lets make sure this is
        // an actual dial down Damage.
        if ((damage.trim().length() > 6) && damage.contains("damage")) {
            try {
                toReturn = Integer.parseInt(damage.substring(damage.indexOf("damage") + 6).trim());
            } catch (NumberFormatException e) {
                LOGGER.warn("Failed to get dialed down damage. {}", e.getMessage());
            }
        }

        return min(weaponType.getDamage(range), toReturn);

    }

    /**
     * Method computes how much heat a dial down weapon generates
     *
     * @return Heat, minimum of 1;
     */
    public static int dialDownHeat(Mounted<?> weapon, WeaponType weaponType) {
        return Compute.dialDownHeat(weapon, weaponType, 1);
    }

    /**
     * Method computes how much heat a dial down weapon generates
     *
     * @return Heat, minimum of 1;
     */
    public static int dialDownHeat(Mounted<?> weapon, WeaponType weaponType, int range) {
        int toReturn = weaponType.getHeat();

        if (!weapon.hasModes()) {
            return toReturn;
        }

        int damage = weaponType.getDamage(range);
        int newDamage = Compute.dialDownDamage(weapon, weaponType, range);

        toReturn = max(1,
              weaponType.getHeat() - max(0, damage - newDamage));
        return toReturn;

    }

    /**
     * @param aPos - attacking entity
     * @param tPos - targeted entity
     *
     * @return a vector of all the entities that are adjacent to the targeted entity and would fall along the angle of
     *       attack
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
            // now let's add all the entities here
            entities.addAll(game.getEntitiesVector(c));
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

    /**
     * Returns true when an attack of the attacker against the target is considered an A2G attack, see TW p.242-247.
     * This includes strafing, striking and bombing. Attacks on Hex targets and Entity targets can be A2G attacks.
     * Artillery attacks, even from an airborne aero unit, do not count as A2G attacks. Attacks from spaceborne
     * aerospace do not count as A2G attacks (they may count as O2G attacks). Also note that flying ground units such as
     * VTOLs and WiGEs count as ground targets and so attacks against them may count as A2G attacks.
     *
     * @param attacker The assumed attacker
     * @param target   The assumed target
     *
     * @return True if the attack would be considered an A2G attack
     */
    public static boolean isAirToGround(Entity attacker, Targetable target) {
        return (attacker != null) && (target != null)
              && (target.getTargetType() != Targetable.TYPE_HEX_ARTILLERY)
              && !attacker.isSpaceborne()
              && attacker.isAirborne()
              && !target.isAirborne()
              && attacker.isAero();
    }

    /**
     * Returns true when an attack of the given units would be an A2A attack. Checks for null units and if both units
     * are airborne, {@link Entity#isAirborne()}. Also checks if they're either on the same map or on connected maps
     * (atmosphere/ground or ground/ground within one atmosphere map).
     *
     * @param game     The game
     * @param attacker The attacking unit
     * @param target   The target
     *
     * @return True when the supposed attack would be an A2A attack
     */
    public static boolean isAirToAir(Game game, Entity attacker, Targetable target) {
        if ((attacker == null) || (target == null) || !attacker.isAirborne() || !target.isAirborne()) {
            return false;
        }
        return game.onTheSameBoard(attacker, target)
              || game.onDirectlyConnectedBoards(attacker, target)
              || CrossBoardAttackHelper.onGroundMapsWithinOneAtmosphereMap(game, attacker, target);
    }

    public static boolean isGroundToAir(Entity attacker, Targetable target) {
        return (attacker != null) && (target != null) && !attacker.isAirborne() && target.isAirborne();
    }

    public static boolean isGroundToGround(Entity attacker, Targetable target) {
        return (attacker != null) && (target != null) && !attacker.isAirborne() && !target.isAirborne();
    }

    /**
     * This is a homebrew function partially drawn from pg. 40-1 of AT2R that allows units that flee the field for any
     * reason to return after a certain number of rounds It can potentially be expanded to include other conditions
     *
     * @return number of rounds until return (-1 if never)
     */
    public static int roundsUntilReturn(Game game, Entity en) {

        if (!en.isAero()) {
            return -1;
        }

        if (!game.getOptions().booleanOption(OptionsConstants.ADVANCED_AERO_RULES_RETURN_FLYOVER)) {
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
        if (Compute.isAirToAir(game, ae, target)) {
            int distance = Compute.effectiveDistance(game, ae, target,
                  target.isAirborneVTOLorWIGE());
            int aAlt = ae.getAltitude();
            int tAlt = target.getAltitude();
            if (target.isAirborneVTOLorWIGE()) {
                tAlt++;
            }
            int altDiff = Math.abs(aAlt - tAlt);
            return altDiff >= (distance - altDiff);
        }
        return false;
    }

    public static List<Coords> getAcceptableUnloadPositions(List<Coords> candidates, int boardId, Entity unitToUnload,
          Game game, int elev) {
        List<Coords> acceptable = new ArrayList<>(candidates);
        acceptable.removeIf(c -> !isAcceptableUnloadPosition(c, boardId, unitToUnload, game, elev));
        return acceptable;
    }

    public static boolean isAcceptableUnloadPosition(Coords position, int boardId, Entity unitToUnload,
          Game game, int elev) {
        Hex hex = game.getHex(position, boardId);
        // Prohibited terrain is any that the unit cannot move into or through, or would cause a stacking violation, or
        // is not 0, 1, or 2 elevations up or down from the hex elevation - but ignore that last if the unloading unit
        // has Jump MP, VTOL movement, or glider wings (IO p.85).
        boolean canIgnoreElevation = unitToUnload.getMovementMode() == EntityMovementMode.VTOL ||
              unitToUnload.getMovementMode() == EntityMovementMode.INF_JUMP ||
              ((unitToUnload.getAnyTypeMaxJumpMP() > 0) && !unitToUnload.isImmobileForJump()) ||
              (unitToUnload.isInfantry() && ((Infantry) unitToUnload).canExitVTOLWithGliderWings());
        return (hex != null) && !unitToUnload.isLocationProhibited(position, boardId, unitToUnload.getElevation())
              && (null == stackingViolation(game, unitToUnload.getId(), position, unitToUnload.climbMode()))
              && ((Math.abs(hex.getLevel() - elev) < 3) || canIgnoreElevation);
    }

    /**
     * Returns a list of all adjacent units that can load the given Entity.
     *
     * @param entity  The entity to load
     * @param pos     The coordinates of the hex to load from
     * @param boardId The board ID of the hex to load from
     * @param elev    The absolute elevation of the unit at the point of loading (surface of the hex + elevation over
     *                the surface)
     * @param game    The game
     *
     * @return All adjacent units that can transport the Entity
     */
    public static List<Entity> getMountableUnits(Entity entity, Coords pos, int boardId, int elev, Game game) {
        if ((entity == null) || (pos == null) || (game == null) || !game.hasBoardLocation(pos, boardId)) {
            LOGGER.error("Invalid argument; cannot find mountable units");
            return Collections.emptyList();
        }

        List<Entity> mountable = new ArrayList<>();
        // the rules don't say that the unit must be facing loader, so lets take the ring
        for (Coords c : pos.allAdjacent()) {
            Hex hex = game.getBoard(boardId).getHex(c);
            if (null == hex) {
                continue;
            }
            for (Entity other : game.getEntitiesVector(c, boardId)) {
                if ((entity.getOwner().equals(other.getOwner())
                      || (entity.getOwner().getTeam() == other.getOwner().getTeam()))
                      && !entity.equals(other)
                      && ((other instanceof SmallCraft) || other.getTowing() != Entity.NONE
                      || other.getTowedBy() != Entity.NONE)
                      && other.canLoad(entity)
                      && !other.isAirborne()
                      && (Math.abs((hex.getLevel() + other.getElevation()) - elev) < 3)
                      && !mountable.contains(other)) {
                    mountable.add(other);
                }
            }
        }
        return mountable;
    }

    public static boolean allowAimedShotWith(WeaponMounted weapon, AimingMode aimingMode) {
        WeaponType weaponType = weapon.getType();
        boolean isWeaponInfantry = weaponType.hasFlag(WeaponType.F_INFANTRY);
        boolean usesAmmo = (weaponType.getAmmoType() != AmmoTypeEnum.NA) && !isWeaponInfantry;
        AmmoMounted ammo = usesAmmo ? weapon.getLinkedAmmo() : null;
        AmmoType ammoType = ammo == null ? null : ammo.getType();

        // Leg, swarm, and BA LB-X AC attacks can't be aimed.
        if (weaponType.getInternalName().equals(Infantry.LEG_ATTACK)
              || weaponType.getInternalName().equals(Infantry.SWARM_MEK)
              || weaponType.getInternalName().equals("Battle Armor LB-X AC")) {
            return false;
        }

        switch (aimingMode) {
            case NONE:
                return false;
            case IMMOBILE:
                if (weapon.getCurrentShots() > 1) {
                    return false;
                }

                if (ammoType == null) {
                    break;
                }

                switch (ammoType.getAmmoType()) {
                    case SRM_STREAK, LRM_STREAK, LRM, LRM_IMP, LRM_TORPEDO, SRM, SRM_IMP, SRM_TORPEDO, MRM, NARC,
                         INARC, AMS, ARROW_IV, LONG_TOM, SNIPER, THUMPER, SRM_ADVANCED, LRM_TORPEDO_COMBO, ATM, IATM,
                         MML, EXLRM, NLRM, TBOLT_5, TBOLT_10, TBOLT_15, TBOLT_20, HAG, ROCKET_LAUNCHER -> {
                        return false;
                    }
                    default -> {
                        // intentional fallthrough
                    }
                }
                if (((ammoType.getAmmoType() == AmmoTypeEnum.AC_LBX_THB)
                      || (ammoType.getAmmoType() == AmmoTypeEnum.AC_LBX)
                      || (ammoType.getAmmoType() == AmmoTypeEnum.SBGAUSS))
                      && (ammoType.getMunitionType().contains(AmmoType.Munitions.M_CLUSTER))) {
                    return false;
                }
                // Flak Ammo can't make aimed shots
                if (((ammoType.getAmmoType() == AmmoTypeEnum.AC)
                      || (ammoType.getAmmoType() == AmmoTypeEnum.AC_ULTRA)
                      || (ammoType.getAmmoType() == AmmoTypeEnum.AC_ULTRA_THB))
                      && (ammoType.getMunitionType().contains(AmmoType.Munitions.M_FLAK))) {
                    return false;
                }

                break;
            case TARGETING_COMPUTER:
                if (!weaponType.hasFlag(WeaponType.F_DIRECT_FIRE)
                      || weaponType.hasFlag(WeaponType.F_PULSE)
                      || weapon.curMode().getName().startsWith("Pulse")
                      || (weaponType instanceof HAGWeapon)) {
                    return false;
                }
                if (weapon.getCurrentShots() > 1) {
                    return false;
                }

                if ((ammoType != null)
                      && ((ammoType.getAmmoType() == AmmoTypeEnum.AC_LBX_THB)
                      || (ammoType.getAmmoType() == AmmoTypeEnum.AC_LBX)
                      || (ammoType.getAmmoType() == AmmoTypeEnum.SBGAUSS))
                      && (ammoType.getMunitionType().contains(AmmoType.Munitions.M_CLUSTER))) {
                    return false;
                }

                // Flak Ammo can't make aimed shots
                if ((ammoType != null)
                      && ((ammoType.getAmmoType() == AmmoTypeEnum.AC)
                      || (ammoType.getAmmoType() == AmmoTypeEnum.AC_ULTRA)
                      || (ammoType.getAmmoType() == AmmoTypeEnum.AC_ULTRA_THB))
                      && (ammoType.getMunitionType().contains(AmmoType.Munitions.M_FLAK))) {
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
        if (entity.hasDroneOs()) {
            return 0;
        }

        if (entity instanceof SmallCraft || entity instanceof Jumpship) {
            int nStandardW = 0;
            int nCapitalW = 0;
            for (Mounted<?> m : entity.getTotalWeaponList()) {
                EquipmentType type = m.getType();
                if (type instanceof BayWeapon) {
                    continue;
                }
                if (type instanceof WeaponType) {
                    if ((((WeaponType) m.getType()).getLongRange() <= 1)
                          // MML range depends on ammo, and getLongRange() returns 0
                          && (((WeaponType) m.getType()).getAmmoType() != AmmoTypeEnum.MML)) {
                        continue;
                    }
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
            return (getFullCrewSize(entity)
                  - getTotalDriverNeeds(entity)
                  - getAdditionalNonGunner(entity));
        } else if (entity instanceof Infantry) {
            return getFullCrewSize(entity);
        } else if (entity.getCrew().getCrewType().getGunnerPos() > 0) {
            // Tripod, QuadVee, or dual cockpit
            return 1;
        }
        return 0;
    }

    // Taken from MekHQ, assumptions are whatever Taharqa made for there - Dylan
    public static int getAeroCrewNeeds(Entity entity) {
        if (entity.hasDroneOs()) {
            return 0;
        }
        if (entity instanceof Dropship) {
            if (entity.isMilitary()) {
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
     *
     * @return The minimum base crew
     */
    public static int getSVBaseCrewNeeds(Entity entity) {
        if (entity.hasDroneOs()) {
            return 0;
        }

        if (entity.isTrailer() && (entity.getEngine().getEngineType() == Engine.NONE)) {
            return 0;
        }
        final boolean naval = entity.getMovementMode().equals(EntityMovementMode.NAVAL)
              || entity.getMovementMode().equals(EntityMovementMode.HYDROFOIL)
              || entity.getMovementMode().equals(EntityMovementMode.SUBMARINE);
        return getCrew(entity, naval);
    }

    private static int getCrew(Entity entity, boolean naval) {
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
     *
     * @return The number of gunners required.
     */
    public static int getSupportVehicleGunnerNeeds(Entity entity) {
        if (entity.hasDroneOs()) {
            return 0;
        }

        final boolean advFireCon = entity.hasMisc(MiscType.F_ADVANCED_FIRE_CONTROL);
        final boolean basicFireCon = !advFireCon && entity.hasMisc(MiscType.F_BASIC_FIRE_CONTROL);
        if (entity.getWeightClass() == EntityWeightClass.WEIGHT_SMALL_SUPPORT) {
            if (!advFireCon && !basicFireCon) {
                // No fire control requires one gunner per weapon.
                return entity.getWeaponList().size();
            } else {
                // Otherwise we require one gunner per facing, with turrets and pintle mounts
                // counting
                // as separate facings
                Set<Integer> facings = new HashSet<>();
                Set<Integer> pintleLocations = new HashSet<>();
                for (Mounted<?> m : entity.getWeaponList()) {
                    if (m.isPintleTurretMounted()) {
                        // We consider pintle-mounted weapons in the same location to be in the same
                        // pintle
                        pintleLocations.add(m.getLocation());
                    } else {
                        facings.add(m.getLocation());
                    }
                }
                if (advFireCon) {
                    // Advanced fire control lets the driver count as a gunner, so one fewer
                    // dedicated gunners is needed.
                    return max(0, pintleLocations.size() + facings.size() - 1);
                } else {
                    return pintleLocations.size() + facings.size();
                }
            }
        } else {
            // Medium and large support vehicle gunner requirements are based on weapon
            // tonnage
            double tonnage = entity.getWeaponList().stream().filter(m -> !m.getType().hasFlag(WeaponType.F_AMS))
                  .mapToDouble(Mounted::getTonnage).sum();
            if (advFireCon) {
                if (entity.getStructuralTechRating() == TechRating.F) {
                    return (int) Math.ceil(tonnage / 6.0);
                } else if (entity.getStructuralTechRating() == TechRating.E) {
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
     * Calculates the number of additional non-gunner crew members required by vehicles and advanced aerospace vessels
     * due to specific miscellaneous equipment mounts or special unit features.
     *
     * <p>Crew additions are based on the tonnage or equipment size of certain mounted systems, such as:</p>
     *
     * <ul>
     *   <li>Communications equipment ({@code F_COMMUNICATIONS}): +1 crew per ton of equipment</li>
     *   <li>Field Kitchens ({@code F_FIELD_KITCHEN}): +3 crew per mount</li>
     *   <li>Mobile Field Bases ({@code F_MOBILE_FIELD_BASE}): +5 crew per mount</li>
     *   <li>MASH units ({@code F_MASH}): +5 crew per size unit</li>
     * </ul>
     *
     * <p>For tanks, any additional crew seats (via {@code getExtraCrewSeats()}) are added.</p>
     *
     * <p>If the unit has a drone operating system it requires 0 additional crew. For super-heavy meks, this always
     * returns {@code 1} (to represent the Tactical Officer).</p>
     *
     * @param entity The unit for which to calculate the additional crew requirements
     *
     * @return The number of additional non-gunner crew required for the given unit
     */
    public static int getAdditionalNonGunner(Entity entity) {
        if (entity.hasDroneOs()) {
            return 0;
        }

        int crew = 0;
        crew += getCommunicationsCrew(entity);
        crew += getDoctorCrew(entity);
        crew += getMedicCrew(entity);
        crew += getCombatTechCrew(entity);
        crew += getAstechCrew(entity);
        for (Mounted<?> m : entity.getMisc()) {
            if (m.getType().hasFlag(MiscType.F_FIELD_KITCHEN)) {
                crew += 3;
            }
        }

        if (entity instanceof Tank tank) {
            crew += tank.getExtraCrewSeats();
        }

        if (entity instanceof Mek && entity.isSuperHeavy()) {
            // Tactical Officer
            return 1;
        }
        return crew;
    }

    public static int getCommunicationsCrew(Entity entity) {
        if (entity.hasDroneOs()) {
            return 0;
        }

        int crew = 0;
        for (Mounted<?> m : entity.getMisc()) {
            if (m.getType().hasFlag(MiscType.F_COMMUNICATIONS)) {
                crew += (int) m.getTonnage();
            }
        }

        return crew;
    }

    public static int getDoctorCrew(Entity entity) {
        if (entity.hasDroneOs()) {
            return 0;
        }

        int crew = 0;
        for (Mounted<?> m : entity.getMisc()) {
            if (m.getType().hasFlag(MiscType.F_MASH)) {
                crew += (int) m.getSize();
            }
        }

        return crew;
    }

    public static int getMedicCrew(Entity entity) {
        if (entity.hasDroneOs()) {
            return 0;
        }

        int crew = 0;
        for (Mounted<?> m : entity.getMisc()) {
            if (m.getType().hasFlag(MiscType.F_MASH)) {
                crew += 4 * (int) m.getSize();
            }
        }

        return crew;
    }

    public static int getCombatTechCrew(Entity entity) {
        if (entity.hasDroneOs()) {
            return 0;
        }

        int crew = 0;
        for (Mounted<?> m : entity.getMisc()) {
            if (m.getType().hasFlag(MiscType.F_MOBILE_FIELD_BASE)) {
                crew++;
            }
        }

        return crew;
    }

    public static int getAstechCrew(Entity entity) {
        if (entity.hasDroneOs()) {
            return 0;
        }

        int crew = 0;
        for (Mounted<?> m : entity.getMisc()) {
            if (m.getType().hasFlag(MiscType.F_MOBILE_FIELD_BASE)) {
                crew += 4;
            }
        }

        return crew;
    }

    // Taken from MekHQ, assumptions are whatever Taharqa made for there - Dylan
    public static int getFullCrewSize(Entity entity) {
        if (entity.hasDroneOs()) {
            return 0;
        }
        if (entity.isNotCrewedEntityType()) {
            return 0;
        }
        if (entity.isSupportVehicle()) {
            int crew = getSVBaseCrewNeeds(entity) + getSupportVehicleGunnerNeeds(entity)
                  + getAdditionalNonGunner(entity);
            if (crew < 4) {
                return crew;
            }
            return crew + (int) Math.ceil(crew / 6.0);
        } else if (entity instanceof Tank) {
            return (int) Math.ceil(entity.getWeight() / 15.0) + getAdditionalNonGunner(entity);
        } else if (entity instanceof BattleArmor) {
            int numTroopers = 0;
            for (int trooper = 1; trooper < entity.locations(); trooper++) {
                // less than zero means the suit is destroyed
                if (entity.getInternal(trooper) >= 0) {
                    // Also, if any modular equipment is missing, then we will consider this
                    // unit to be inoperable and will not allow it to load soldiers. This is because
                    // we have no mechanism in MM to handle BA where some suits have the equipment
                    // and others do not
                    boolean useSuit = true;
                    for (Mounted<?> m : entity.getEquipment()) {
                        if (m.isMissingForTrooper(trooper)) {
                            useSuit = false;
                            break;
                        }
                    }
                    if (useSuit) {
                        numTroopers++;
                    }
                }
            }
            return numTroopers;
        } else if (entity instanceof Infantry) {
            return ((Infantry) entity).getSquadCount() * ((Infantry) entity).getSquadSize();
        } else if (entity instanceof Jumpship || entity instanceof SmallCraft) {
            return getAeroCrewNeeds(entity) + getTotalGunnerNeeds(entity) + getAdditionalNonGunner(entity);
        } else if (entity.isSuperHeavy() || entity.isTripodMek()) {
            return getTotalDriverNeeds(entity) + getTotalGunnerNeeds(entity) + getAdditionalNonGunner(entity);
        } else {
            return 1;
        }
    }

    // Taken from MekHQ, assumptions are whatever Taharqa made for there - Dylan
    public static int getTotalDriverNeeds(Entity entity) {
        if (entity.hasDroneOs()) {
            return 0;
        }
        if (entity.isNotCrewedEntityType()) {
            return 0;
        }
        // Fix for MHQ Bug #3. Space stations have as much need for pilots as jumpships
        // do.
        if (entity instanceof SpaceStation) {
            return 2;
        }
        if (entity instanceof SmallCraft || entity instanceof Jumpship) {
            // it's not at all clear how many pilots dropships and jumpships
            // should have, but the old BattleSpace book suggests they should
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
        if (entity instanceof Mek || entity instanceof Tank || entity instanceof Aero || entity instanceof ProtoMek) {
            // only one driver please
            return 1;
        } else if (entity instanceof Infantry) {
            return getFullCrewSize(entity);
        }
        return 0;
    }

    /**
     * Should we treat this entity, in its current state, as if it is a spheroid unit flying in atmosphere?
     */
    public static boolean useSpheroidAtmosphere(Game game, Entity en) {
        if (!(en instanceof IAero aero) || en.isSpaceborne()) {
            return false;
        }
        // aerodyne's will operate like spheroids in vacuum
        PlanetaryConditions conditions = game.getPlanetaryConditions();
        if (!aero.isSpheroid() && !conditions.getAtmosphere().isLighterThan(Atmosphere.THIN)) {
            return false;
        }
        // are we in atmosphere?
        return en.isAirborne();
    }

    /**
     * Worker function that checks if an indirect attack is impossible for the given passed-in arguments
     */
    public static boolean indirectAttackImpossible(Game game, Entity ae, Targetable target, WeaponType weaponType,
          Mounted<?> weapon) {
        boolean isLandedSpheroid = ae.isAero() && ((IAero) ae).isSpheroid() && (ae.getAltitude() == 0)
              && game.getBoard().isGround();
        int altDif = target.getAltitude() - ae.getAltitude();
        boolean noseWeaponAimedAtGroundTarget = (weapon != null) && (weapon.getLocation() == Aero.LOC_NOSE)
              && (altDif < 1);

        return game.getOptions().booleanOption(OptionsConstants.BASE_INDIRECT_FIRE)
              && !game.getOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_INDIRECT_ALWAYS_POSSIBLE)
              && LosEffects.calculateLOS(game, ae, target).canSee()
              && (!game.getOptions().booleanOption(OptionsConstants.ADVANCED_DOUBLE_BLIND)
              || Compute.canSee(game, ae, target))
              && !(weaponType instanceof ArtilleryCannonWeapon)
              && !weaponType.hasFlag(WeaponType.F_MORTAR_TYPE_INDIRECT)
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
        // See indirect flight times table, TO:AR p149
        if (distance <= Board.DEFAULT_BOARD_HEIGHT) {
            turnsTilHit = 0;
        } else if (distance <= (8 * Board.DEFAULT_BOARD_HEIGHT)) {
            turnsTilHit = 1;
        } else if (distance <= (15 * Board.DEFAULT_BOARD_HEIGHT)) {
            turnsTilHit = 2;
        } else if (distance <= (21 * Board.DEFAULT_BOARD_HEIGHT)) {
            turnsTilHit = 3;
        } else if (distance <= (26 * Board.DEFAULT_BOARD_HEIGHT)) {
            turnsTilHit = 4;
        } else {
            turnsTilHit = 5;
        }
        return turnsTilHit;
    }

    /**
     * Get turns for an indirect or off-board round to hit with its current velocity
     *
     * @param ae       Attacker
     * @param target   Target hex/entity
     * @param velocity speed of round, default 50 according to WeaponAttackAction
     */
    public static int turnsTilBOMHit(Game game, Entity ae, Targetable target, int velocity) {
        int distance = Compute.effectiveDistance(game, ae, target);
        distance = (int) Math.floor((double) distance / game.getPlanetaryConditions().getGravity());
        return distance / velocity;
    }

    /**
     * @param homing to determine if we need the homing lead or some other value.
     *
     * @return Coordinates to aim at to hit this target while it's on the move (we think).
     */
    public static Coords calculateArtilleryLead(Game game, Entity ae, Targetable target, boolean homing) {
        int leadAmount = 0;
        int direction = 0;
        int turnsTilHit = turnsTilHit(effectiveDistance(game, ae, target, true));

        // Hexes can't move...
        if (target instanceof Entity te) {
            int mp = te.getPriorPosition().distance(te.getPosition()); // Assume last move presages the next
            if (mp == 0 && game.getRoundCount() == 0) {
                // Assume a mobile enemy will move somewhat after deploying
                mp = te.getWalkMP();
            }

            // Try to keep the current position within the homing radius, unless they're
            // real fast...
            if (homing) {
                leadAmount = (mp * (turnsTilHit + 1)) + HOMING_RADIUS;
            } else {
                leadAmount = mp * (turnsTilHit + 2);
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
     * @return Coordinates to target given this lead and direction
     */
    public static Coords calculateArtilleryLead(Coords targetPoint, int direction, int leadAmount) {
        Coords newPoint = targetPoint.translated(direction, leadAmount);
        StringBuilder msg = new StringBuilder("Computed coordinates ( ")
              .append(newPoint.toString())
              .append(" ) for target point ( ").append(targetPoint)
              .append(" ), direction ").append(direction)
              .append(", lead range ").append(leadAmount);

        LOGGER.debug(msg);
        return newPoint;
    }

    /**
     * Lightweight helper to determine if a given unit can take a Pointblank shot on another
     *
     * @param attacker Prospective Pointblank Shot shooter
     * @param target   Prospective Pointblank Shot target
     *
     * @return boolean true if shot can be taken legally; false otherwise
     */
    public static boolean canPointBlankShot(Entity attacker, Entity target) {
        if (!attacker.isHidden() || !attacker.isEnemyOf(target)) {
            // PBS attacker has to be hidden and an enemy of the PBS target
            return false;
        }

        if (!target.isAerospace()) {
            // The simpler path: mainly worried about distance
            // PBS attacker has to be exactly 1 hex away from target
            return attacker.getPosition().distance(target.getPosition()) == 1;
        } else {
            // More complex; may need to worry about a flight path, and Infantry ranges
            if (attacker.isInfantry()) {
                if (attacker.getMaxWeaponRange(true) <= 1) {
                    // Infantry attacker needs long-range weapons that can hit an aircraft
                    return false;
                } else {
                    boolean hasFieldGuns = ((Infantry) attacker).hasActiveFieldWeapon();
                    boolean hasInfantryAA = attacker.getEquipment().stream().anyMatch(
                          eq -> eq instanceof WeaponMounted
                                && ((WeaponMounted) eq).getType().hasFlag(WeaponType.F_INF_AA)
                    );
                    // Either allows infantry PBS on Aerospace.
                    return (hasFieldGuns || hasInfantryAA);
                }
            }
        }

        return true;
    }

    /**
     * Lightweight helper for some step evaluation.  No side effects.
     *
     * @param detector Entity that will detect a hidden unit
     * @param distance int Distance from detector to hidden entity
     * @param endStep  boolean whether this detection is occurring at the last step of a move path
     *
     * @return true if detector can detect a unit in this situation
     */
    public static boolean canDetectHidden(Entity detector, int distance, boolean endStep) {
        // Ending movement adjacent to a hidden unit also reveals it.
        if (detector.isAerospace()) {
            // Errata says Aerospace flying over hidden units detect them
            // (https://bg.battletech.com/forums/index.php?topic=84054.0)
            return distance == 0;
        } else {return (distance == 1) && endStep;}
        // Active Probe detection is handled in detectHiddenUnits
        // Anything not explicitly detected is not detected.
    }

    public static boolean allEnemiesOutsideBlast(
          Targetable target, Entity attacker, AmmoType ammoType, final boolean artillery, final boolean flak,
          final boolean asfFlak, Game game
    ) {
        return enemiesInsideBlast(target, attacker, ammoType, artillery, flak, asfFlak, game).isEmpty();
    }

    public static Set<Entity> enemiesInsideBlast(
          Targetable target, Entity attacker, AmmoType ammoType, final boolean artillery, final boolean flak,
          final boolean asfFlak, Game game
    ) {
        Set<Entity> entities = new HashSet<>();
        Coords position = target.getPosition();
        if (position == null) {
            return entities;
        }

        // We don't need the exact positional details to show entities are outside the blast zone of a given
        // AE munition:
        // 1. The highest* an entity can be is: hex.ceiling() + 2 for R1+ bombs, OR
        //                                      hex.ceiling() + 1 for R0 bombs, OR
        //                                      hex.getLevel() + base damage / 25 for Cruise Missiles, OR
        //                                      hex.getLevel() + base damage / 10 for non-homing Artillery
        // *(For bombs: only over water or building hex; for artillery, any hex**)
        // **(Artillery uses base AE rules for building/water hexes, based on radius rather than damage)
        //
        // 2. The lowest* an entity can be is:  hex.getLevel() - 2 for R1+ bombs/artillery, OR
        //                                      hex.getLevel() - 1 for R0 bombs/artillery
        // *(for all AE: only in building or water hexes)
        //
        // 3. Farthest out from the center a unit can be is Radius, set per munition.
        // 4. Blast deals damage in "sphere" where horizontal + vertical displacement <= Radius, but
        //    only for building / water hexes.
        // 5. Artillery Flak creates a blast up and down in the target hex only
        // 6. Anti-ASF Artillery Flak creates a blast in the target hex at the target altitude only
        //
        // To prove an AE attack will catch _zero_ enemies, we just need to prove any enemies in the zone
        // are too deep or too high for _any_ blast damage to reach.

        Hex hex = game.getBoard().getHex(position);
        final boolean causeAEBlast = hex != null && hex.containsAnyTerrainOf(Terrains.BLDG_ELEV, Terrains.WATER);
        final int baseHeight;
        final int ceiling;
        if (flak) {
            if (asfFlak) {
                ceiling = baseHeight = target.getAltitude();
            } else {
                ceiling = baseHeight = target.getElevation();
            }
        } else {
            baseHeight = (hex != null) ? hex.getLevel() : 0;
            ceiling = (hex != null) ? hex.ceiling() : baseHeight;
        }

        // Get radius, base damage
        DamageFalloff falloff = AreaEffectHelper.calculateDamageFallOff(
              ammoType,
              attacker.isBattleArmor() ? ((BattleArmor) attacker).getTroopers() : 0,
              false
        );

        int radius = falloff.radius;
        if (asfFlak) {
            // Anti-ASF Flak shots only affect the target hex
            radius = 0;
        }

        double damage = falloff.damage;

        boolean cruiseMissile = ammoType.hasFlag(AmmoType.F_CRUISE_MISSILE);
        final int verticalLevels;
        if (cruiseMissile || artillery) {
            // Levels above (and possibly below) level/center hex
            // e.g. LT has damage 25, falloff 10, radius 2 -> round up (25/10) -> 3, -1 = 2.
            verticalLevels = (int) Math.ceil(damage / ((cruiseMissile) ? 25.0 : 10.0)) - 1;
        } else {
            verticalLevels = (radius >= 0) ? ((radius > 1) ? 2 : 1) : 0;
        }

        if (causeAEBlast || flak || asfFlak) {
            // Both artillery and bombs cause AE blast spheres when hitting water or buildings.
            // For
            for (int r = 0; r <= radius; r++) {
                final int rad = r;
                List<Coords> ringCoords = position.allAtDistance(r);
                // Get all enemy entities that protrude into the blast sphere, or, for Anti-ASF Flak,
                // are in the target's hex at the same altitude
                for (Coords coords : ringCoords) {
                    List<Entity> cEntities = game.getEntitiesVector(coords);
                    entities.addAll(cEntities.stream().filter(
                          e -> e.isEnemyOf(attacker) &&
                                ((e.getElevation() + e.getHeight() >= rad + baseHeight - verticalLevels)
                                      && (e.getElevation() <= ceiling + verticalLevels - rad))
                                || (e.isAero() && e.getAltitude() == baseHeight)
                    ).toList());
                }
            }
        } else if (artillery) {
            // Central vertical blast column
            entities.addAll(game.getEntitiesVector(position).stream().filter(
                  e -> e.isEnemyOf(attacker) &&
                        (e.getElevation() + e.getHeight() >= baseHeight)
                        && (e.getElevation() <= ceiling + verticalLevels)
            ).toList());
            for (int r = 1; r <= radius; r++) {
                // Get all the entities that cross the blast ring
                List<Coords> ringCoords = position.allAtDistance(r);
                for (Coords coords : ringCoords) {
                    List<Entity> cEntities = game.getEntitiesVector(coords);
                    entities.addAll(cEntities.stream().filter(
                          e -> e.isEnemyOf(attacker) &&
                                ((e.getElevation() + e.getHeight() >= baseHeight)
                                      && (e.getElevation() <= ceiling))
                    ).toList());
                }
            }


        }
        // No enemies in the volume == all outside
        return entities;
    }

    /**
     * Fast log2 implementation; throws if number &le; 0
     * @param number        positive int to get the log2 of
     * @return int          approximate log2 of number; functionally (Math.floor(log10(10)/log10(2))
     */
    public static int log2(int number) throws IllegalArgumentException {
        if (number <= 0) {
            throw new IllegalArgumentException();
        }
        return 31 - Integer.numberOfLeadingZeros(number);
    }

    /**
     * Helper to get the coordinates from which a unit can load other units.  Not in Entity to avoid bloat.
     * May need extension for different map types but unlikely.
     * @param carrier   Entity that will be doing the loading
     * @param position  Coords of hex to use as the center of the carrier; may not match carrier's current position
     *                  value.
     * @param boardId   For future use, e.g. low-altitude or multi-board maps
     * @return ArrayList of Coords that the carrier entity can legally load units from
     */
    public static ArrayList<Coords> getLoadableCoords(Entity carrier, Coords position, int boardId) {
        ArrayList<Coords> list = new ArrayList<Coords>();
        // No coords for no carrier
        if (carrier == null) {
            return list;
        }

        // Landed DropShip occupies 7 hexes, loads from the adjacent ring of hexes
        if (carrier.isDropShip() && carrier.isAeroLandedOnGroundMap()) {
            list.addAll(position.allAtDistance(2));
        } else if (
              // SmallCraft, Large Support Vehicles, flying DropShips, and presumably spaceborne WarShips load from
              // directly adjacent hexes
              carrier instanceof SmallCraft ||
              (carrier.isSupportVehicle() && (carrier.getWeightClass() == EntityWeightClass.WEIGHT_LARGE_SUPPORT)) ||
              (carrier.isDropShip() && carrier.isAirborne()) ||
              (carrier.isWarShip())
        ) {
            list.addAll(position.allAtDistance(1));
        } else {
            list.add(position);
        }

        return list;
    }

    private Compute() {}
}
