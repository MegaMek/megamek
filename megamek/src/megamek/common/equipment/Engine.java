/*

 * Copyright (C) 2000-2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2005-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.equipment;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import megamek.common.Messages;
import megamek.common.SimpleTechLevel;
import megamek.common.TechAdvancement;
import megamek.common.TechConstants;
import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.Era;
import megamek.common.enums.Faction;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.interfaces.ITechnology;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import megamek.common.units.Tank;
import megamek.common.util.RoundWeight;

/**
 * This class represents an engine, such as those driving 'Meks.
 *
 * @author Reinhard Vicinus
 */
public class Engine implements Serializable, ITechnology {
    @Serial
    private static final long serialVersionUID = -246032529363109609L;

    public static final double[] ENGINE_RATINGS = { 0.0, 0.25, 0.5, 0.5,
                                                    0.5, 0.5, 1.0, 1.0, 1.0, 1.0, 1.5, 1.5, 1.5, 2.0, 2.0,
                                                    2.0, 2.5, 2.5, 3.0, 3.0, 3.0, 3.5, 3.5, 4.0, 4.0, 4.0,
                                                    4.5, 4.5, 5.0, 5.0, 5.5, 5.5, 6.0, 6.0, 6.0, 7.0, 7.0,
                                                    7.5, 7.5, 8.0, 8.5,

                                                    8.5, 9.0, 9.5, 10.0, 10.0, 10.5, 11.0, 11.5, 12.0, 12.5,
                                                    13.0, 13.5, 14.0, 14.5, 15.5, 16.0, 16.5, 17.5, 18.0,
                                                    19.0, 19.5, 20.5, 21.5, 22.5, 23.5, 24.5, 25.5, 27.0,
                                                    28.5, 29.5, 31.5, 33.0, 34.5, 36.5, 38.5, 41.0, 43.5,
                                                    46.0, 49.0, 52.5,

                                                    56.5, 61.0, 66.5, 72.5, 79.5, 87.5, 97.0, 107.5, 119.5,
                                                    133.5, 150.0, 168.5, 190.0, 214.5, 243.0, 275.5, 313.0,
                                                    356.0, 405.5, 462.5 };

    // flags
    public static final int CLAN_ENGINE = 0x1;
    public static final int TANK_ENGINE = 0x2;
    public static final int LARGE_ENGINE = 0x4;
    public static final int SUPERHEAVY_ENGINE = 0x8;
    public static final int SUPPORT_VEE_ENGINE = 0x10;

    // types
    public static final int COMBUSTION_ENGINE = 0;
    public static final int NORMAL_ENGINE = 1;
    public static final int XL_ENGINE = 2;
    public static final int XXL_ENGINE = 3;
    public static final int FUEL_CELL = 4;
    public static final int LIGHT_ENGINE = 5;
    public static final int COMPACT_ENGINE = 6;
    public static final int FISSION = 7;
    public static final int NONE = 8;
    public static final int MAGLEV = 9;
    public static final int STEAM = 10;
    public static final int BATTERY = 11;
    public static final int SOLAR = 12;
    public static final int EXTERNAL = 13;
    private static final int NUM_ENGINE_TYPES = 14;

    /** Keys for retrieving engine name from {@link Messages} */
    private static final String[] TYPE_KEYS = {
          "ICE", "Fusion", "XL", "XXL", "FuelCell", "Light", "Compact", "Fission", "None",
          "MagLev", "Steam", "Battery", "Solar", "External"
    };

    // These are the SUPPORT VEHICLE ENGINE WEIGHT MULTIPLIERS from TM PG 127
    // (MagLev from TO:AU&E pg 62)
    // The other engine types are assumed to have a value of 0 in the array
    // if not listed.
    private static final double[][] SV_ENGINE_RATINGS = new double[NUM_ENGINE_TYPES][TechRating.values().length];

    static {
        SV_ENGINE_RATINGS[COMBUSTION_ENGINE][TechRating.A.getIndex()] = 0.0;
        SV_ENGINE_RATINGS[COMBUSTION_ENGINE][TechRating.B.getIndex()] = 3.0;
        SV_ENGINE_RATINGS[COMBUSTION_ENGINE][TechRating.C.getIndex()] = 2.0;
        SV_ENGINE_RATINGS[COMBUSTION_ENGINE][TechRating.D.getIndex()] = 1.5;
        SV_ENGINE_RATINGS[COMBUSTION_ENGINE][TechRating.E.getIndex()] = 1.3;
        SV_ENGINE_RATINGS[COMBUSTION_ENGINE][TechRating.F.getIndex()] = 1.0;

        SV_ENGINE_RATINGS[NORMAL_ENGINE][TechRating.A.getIndex()] = 0.0;
        SV_ENGINE_RATINGS[NORMAL_ENGINE][TechRating.B.getIndex()] = 0.0;
        SV_ENGINE_RATINGS[NORMAL_ENGINE][TechRating.C.getIndex()] = 1.5;
        SV_ENGINE_RATINGS[NORMAL_ENGINE][TechRating.D.getIndex()] = 1.0;
        SV_ENGINE_RATINGS[NORMAL_ENGINE][TechRating.E.getIndex()] = 0.75;
        SV_ENGINE_RATINGS[NORMAL_ENGINE][TechRating.F.getIndex()] = 0.5;

        SV_ENGINE_RATINGS[FUEL_CELL][TechRating.A.getIndex()] = 0.0;
        SV_ENGINE_RATINGS[FUEL_CELL][TechRating.B.getIndex()] = 0.0;
        SV_ENGINE_RATINGS[FUEL_CELL][TechRating.C.getIndex()] = 1.2;
        SV_ENGINE_RATINGS[FUEL_CELL][TechRating.D.getIndex()] = 1.0;
        SV_ENGINE_RATINGS[FUEL_CELL][TechRating.E.getIndex()] = 0.9;
        SV_ENGINE_RATINGS[FUEL_CELL][TechRating.F.getIndex()] = 0.7;

        SV_ENGINE_RATINGS[FISSION][TechRating.A.getIndex()] = 0.0;
        SV_ENGINE_RATINGS[FISSION][TechRating.B.getIndex()] = 0.0;
        SV_ENGINE_RATINGS[FISSION][TechRating.C.getIndex()] = 1.75;
        SV_ENGINE_RATINGS[FISSION][TechRating.D.getIndex()] = 1.5;
        SV_ENGINE_RATINGS[FISSION][TechRating.E.getIndex()] = 1.4;
        SV_ENGINE_RATINGS[FISSION][TechRating.F.getIndex()] = 1.3;

        SV_ENGINE_RATINGS[NONE][TechRating.A.getIndex()] = 0.0;
        SV_ENGINE_RATINGS[NONE][TechRating.B.getIndex()] = 0.0;
        SV_ENGINE_RATINGS[NONE][TechRating.C.getIndex()] = 0.0;
        SV_ENGINE_RATINGS[NONE][TechRating.D.getIndex()] = 0.0;
        SV_ENGINE_RATINGS[NONE][TechRating.E.getIndex()] = 0.0;
        SV_ENGINE_RATINGS[NONE][TechRating.F.getIndex()] = 0.0;

        SV_ENGINE_RATINGS[MAGLEV][TechRating.A.getIndex()] = 0.0;
        SV_ENGINE_RATINGS[MAGLEV][TechRating.B.getIndex()] = 0.0;
        SV_ENGINE_RATINGS[MAGLEV][TechRating.C.getIndex()] = 0.8;
        SV_ENGINE_RATINGS[MAGLEV][TechRating.D.getIndex()] = 0.7;
        SV_ENGINE_RATINGS[MAGLEV][TechRating.E.getIndex()] = 0.6;
        SV_ENGINE_RATINGS[MAGLEV][TechRating.F.getIndex()] = 0.5;

        SV_ENGINE_RATINGS[STEAM][TechRating.A.getIndex()] = 4.0;
        SV_ENGINE_RATINGS[STEAM][TechRating.B.getIndex()] = 3.5;
        SV_ENGINE_RATINGS[STEAM][TechRating.C.getIndex()] = 3.0;
        SV_ENGINE_RATINGS[STEAM][TechRating.D.getIndex()] = 2.8;
        SV_ENGINE_RATINGS[STEAM][TechRating.E.getIndex()] = 2.6;
        SV_ENGINE_RATINGS[STEAM][TechRating.F.getIndex()] = 2.5;

        SV_ENGINE_RATINGS[BATTERY][TechRating.A.getIndex()] = 0.0;
        SV_ENGINE_RATINGS[BATTERY][TechRating.B.getIndex()] = 0.0;
        SV_ENGINE_RATINGS[BATTERY][TechRating.C.getIndex()] = 1.5;
        SV_ENGINE_RATINGS[BATTERY][TechRating.D.getIndex()] = 1.2;
        SV_ENGINE_RATINGS[BATTERY][TechRating.E.getIndex()] = 1.0;
        SV_ENGINE_RATINGS[BATTERY][TechRating.F.getIndex()] = 0.8;

        SV_ENGINE_RATINGS[SOLAR][TechRating.A.getIndex()] = 0.0;
        SV_ENGINE_RATINGS[SOLAR][TechRating.B.getIndex()] = 0.0;
        SV_ENGINE_RATINGS[SOLAR][TechRating.C.getIndex()] = 5.0;
        SV_ENGINE_RATINGS[SOLAR][TechRating.D.getIndex()] = 4.5;
        SV_ENGINE_RATINGS[SOLAR][TechRating.E.getIndex()] = 4.0;
        SV_ENGINE_RATINGS[SOLAR][TechRating.F.getIndex()] = 3.5;

        SV_ENGINE_RATINGS[EXTERNAL][TechRating.A.getIndex()] = 0.0;
        SV_ENGINE_RATINGS[EXTERNAL][TechRating.B.getIndex()] = 1.4;
        SV_ENGINE_RATINGS[EXTERNAL][TechRating.C.getIndex()] = 1.0;
        SV_ENGINE_RATINGS[EXTERNAL][TechRating.D.getIndex()] = 0.8;
        SV_ENGINE_RATINGS[EXTERNAL][TechRating.E.getIndex()] = 0.7;
        SV_ENGINE_RATINGS[EXTERNAL][TechRating.F.getIndex()] = 0.6;
    }

    public boolean engineValid;
    private int engineRating;
    private int engineType;
    private int engineFlags;
    private int baseChassisHeatSinks = -1;
    public StringBuffer problem = new StringBuffer("Illegal engine: ");

    /**
     * The constructor takes the rating of the engine, the type of engine and any flags. Engine ratings are divided by
     * the weight of the mek to get they walk MP.
     *
     * @param engineRating the rating of the engine
     * @param engineType   the type of the engine, either combustion or a type of fusion engine.
     * @param engineFlags  Weather the engine is a tank engine, a clan engine, or large engine, or any combination of
     *                     those.
     */
    public Engine(int engineRating, int engineType, int engineFlags) {
        engineValid = true;
        this.engineRating = engineRating;
        this.engineType = engineType;
        this.engineFlags = engineFlags;

        if (!isValidEngine()) {
            engineValid = false;
            this.engineRating = 0;
            this.engineType = -1;
            this.engineFlags = -1;
        }
    }

    /**
     * returns true if the engine has the flag set, false otherwise
     *
     * @param flag the flag to check for.
     *
     * @return true if the flag is set.
     */
    public boolean hasFlag(int flag) {
        return (engineFlags & flag) != 0;
    }

    /**
     * Sanity checks the engine, no negative ratings, and similar checks.
     *
     * @return true if the engine is usable.
     */
    private boolean isValidEngine() {
        if (hasFlag(~(CLAN_ENGINE | TANK_ENGINE | LARGE_ENGINE
              | SUPERHEAVY_ENGINE | SUPPORT_VEE_ENGINE))) {
            problem.append("Flags:").append(engineFlags);
            return false;
        }

        if (hasFlag(SUPPORT_VEE_ENGINE) && (engineType != STEAM)
              && (engineType != COMBUSTION_ENGINE) && (engineType != BATTERY)
              && (engineType != FUEL_CELL) && (engineType != SOLAR)
              && (engineType != FISSION) && (engineType != NORMAL_ENGINE)
              && (engineType != MAGLEV) && (engineType != NONE)
              && (engineType != EXTERNAL)) {
            problem.append("Invalid Engine type for support vehicle engines!");
            return false;
        }

        if ((((engineRating / 5) > ENGINE_RATINGS.length) || (engineRating < 0)) && !hasFlag(SUPPORT_VEE_ENGINE)) {
            problem.append("Rating:").append(engineRating);
            return false;
        }
        if ((engineRating > 400) && !hasFlag(SUPPORT_VEE_ENGINE)) {
            engineFlags |= LARGE_ENGINE;
        }

        switch (engineType) {
            case COMBUSTION_ENGINE:
            case NORMAL_ENGINE:
            case XL_ENGINE:
            case XXL_ENGINE:
            case FUEL_CELL:
            case NONE:
            case MAGLEV:
            case BATTERY:
            case SOLAR:
            case STEAM:
            case EXTERNAL:
                break;
            case COMPACT_ENGINE:
                if (hasFlag(LARGE_ENGINE)) {
                    problem.append(Messages
                          .getString("Engine.invalidCompactLarge"));
                    return false;
                }
                break;
            case LIGHT_ENGINE:
            case FISSION:
                if (hasFlag(CLAN_ENGINE)) {
                    problem.append(Messages
                          .getString("Engine.invalidSphereOnly"));
                    return false;
                }
                break;
            default:
                problem.append("Type:").append(engineType);
                return false;
        }

        return true;
    }

    /**
     * Parses a string to find the engine type.
     *
     * @param type the string to parse
     *
     * @return the type of the engine.
     */
    public static int getEngineTypeByString(String type) {
        if (type.toLowerCase().contains("xxl")) {
            return XXL_ENGINE;
        } else if (type.toLowerCase().contains("xl")) {
            return XL_ENGINE;
        } else if (type.toLowerCase().contains("light")) {
            return LIGHT_ENGINE;
        } else if (type.toLowerCase().contains("compact")) {
            return COMPACT_ENGINE;
        } else if (type.toLowerCase().contains("ice")) {
            return COMBUSTION_ENGINE;
        } else if (type.toLowerCase().contains("i.c.e.")) {
            return COMBUSTION_ENGINE;
        } else if (type.toLowerCase().contains("fission")) {
            return FISSION;
        } else if (type.toLowerCase().contains("fuel cell")) {
            return FUEL_CELL;
        } else if (type.toLowerCase().contains("fuel-cell")) {
            return FUEL_CELL;
        } else if (type.toLowerCase().contains("none")) {
            return NONE;
        } else if (type.toLowerCase().contains("maglev")) {
            return MAGLEV;
        } else if (type.toLowerCase().contains("steam")) {
            return STEAM;
        } else if (type.toLowerCase().contains("battery")) {
            return BATTERY;
        } else if (type.toLowerCase().contains("solar")) {
            return SOLAR;
        } else {
            return NORMAL_ENGINE;
        }
    }

    /** @return True if this engine is a fusion engine. */
    public boolean isFusion() {
        return (engineType != COMBUSTION_ENGINE) && (engineType != FISSION) && (engineType != FUEL_CELL)
              && (engineType != NONE) && (engineType != BATTERY) && (engineType != SOLAR)
              && (engineType != STEAM) && (engineType != MAGLEV) && (engineType != EXTERNAL);
    }

    /** @return True if this engine is a fission engine. */
    public boolean isFission() {
        return engineType == FISSION;
    }

    public boolean isSolar() {
        return engineType == SOLAR;
    }

    public boolean isICE() {
        return engineType == COMBUSTION_ENGINE;
    }

    /**
     * Returns the weight of the engine in tons, rounded to the next highest half ton.
     *
     * @return the weight of the engine.
     */
    public double getWeightEngine(Entity entity) {
        return getWeightEngine(entity, RoundWeight.STANDARD);
    }

    /**
     * Returns the weight of the engine, rounded by roundWeight.
     *
     * @param roundWeight One of the rounding factors given in {@link megamek.common.verifier.TestEntity}.
     *
     * @return the weight of the engine in tons.
     */
    public double getWeightEngine(Entity entity, RoundWeight roundWeight) {
        // Support Vehicles compute engine weight differently
        if ((entity.isSupportVehicle() || hasFlag(SUPPORT_VEE_ENGINE))
              && isValidEngine()) {
            double weight = getWeight(entity);

            // SV engine weight rounds to nearest half-ton (or kg for small SVs) per TM p.133
            return RoundWeight.SV_ENGINE.round(weight, entity);
        } else if (entity.hasETypeFlag(Entity.ETYPE_PROTOMEK) && (engineRating < 40)) {
            // ProtoMek engines with rating < 40 use a special calculation
            return roundWeight.round(engineRating * 0.025, entity);
        }

        double weight = ENGINE_RATINGS[(int) Math.ceil(engineRating / 5.0)];
        switch (engineType) {
            case COMBUSTION_ENGINE:
                weight *= 2.0;
                break;
            case NORMAL_ENGINE:
                break;
            case XL_ENGINE:
                weight *= 0.5;
                break;
            case LIGHT_ENGINE:
                weight *= 0.75;
                break;
            case XXL_ENGINE:
                weight /= 3;
                break;
            case COMPACT_ENGINE:
                weight *= 1.5;
                break;
            case FISSION:
                weight *= 1.75;
                weight = Math.max(5, weight);
                break;
            case FUEL_CELL:
                weight *= 1.2;
                break;
            case NONE:
                return 0;
        }
        weight = roundWeight.round(weight, entity);

        if (hasFlag(TANK_ENGINE) && (isFusion() || (engineType == FISSION))) {
            weight *= 1.5;
        }

        double toReturn = roundWeight.round(weight, entity);
        // hover have a minimum weight of 20%
        if (entity.getMovementMode().isHover() && (entity instanceof Tank)) {
            toReturn = Math.max(roundWeight.round(entity.getWeight() / 5.0, entity), toReturn);
        }
        return toReturn;
    }

    private double getWeight(Entity entity) {
        int mp = entity.getOriginalWalkMP();
        if (entity.getMovementMode().isTrain()) {
            mp = Math.max(0, mp - 2);
        }
        double movementFactor = 4 + mp * mp;
        double engineWeightMultiplier = SV_ENGINE_RATINGS[engineType][entity.getEngineTechRating().getIndex()];
        double weight = entity.getBaseEngineValue() * movementFactor
              * engineWeightMultiplier * entity.getWeight();
        // Fusion engines have a minimum weight of 0.25t at D+ and 0.5t at C. Fission
        // engines have
        // a minimum of 5t at all tech ratings.
        if ((engineType == NORMAL_ENGINE) && (entity.getEngineTechRating().isBetterOrEqualThan(TechRating.D))) {
            weight = Math.max(weight, 0.25);
        } else if ((engineType == NORMAL_ENGINE) || (engineType == FISSION)) {
            weight = Math.max(weight, 5);
        }

        // Hovercraft have a minimum engine weight of 20% of the vehicle.
        if (entity.getMovementMode().isHover()) {
            weight = Math.max(weight, entity.getWeight() * 0.2);
        }
        return weight;
    }

    /**
     * @return the number of heat sinks that fit weight-free into the engine
     */
    public int getWeightFreeEngineHeatSinks() {
        // Support Vee engines never provide free heat-sinks, TM pg 133
        if (hasFlag(SUPPORT_VEE_ENGINE)) {
            return 0;
        }
        if (isFusion()) {
            return 10;
        } else if (engineType == FISSION) {
            return 5;
        } else if (engineType == FUEL_CELL) {
            return 1;
        } else {
            return 0;
        }

    }

    /**
     * Returns the number of heat sinks which can be built into the engine and therefore don't require a critical slot.
     *
     * @param compact Whether this engine uses compact heat sinks or not.
     *
     * @return the maximum number of heat sinks built into the engine.
     */
    public int integralHeatSinkCapacity(boolean compact) {
        if (compact) {
            return (engineRating / 25) * 2;
        } else {
            return engineRating / 25;
        }
    }

    /**
     * Get the name of this engine, this is the localized name used in displays. The name of an Engine is based on its
     * type.
     *
     * @return the engine name.
     */
    public String getShortEngineName() {
        if (engineType < TYPE_KEYS.length) {
            if (hasFlag(SUPPORT_VEE_ENGINE)) {
                return Messages.getString("Engine." + TYPE_KEYS[engineType]).trim();
            } else {
                return String.format("%d%s", engineRating, Messages.getString("Engine." + TYPE_KEYS[engineType]));
            }
        } else {
            return Messages.getString("Engine.invalid");
        }
    }

    public static String getEngineTypeName(int engineType) {
        if ((engineType < 0) || (engineType >= TYPE_KEYS.length)) {
            return Messages.getString("Engine.invalid");
        }
        return Messages.getString("Engine." + TYPE_KEYS[engineType]);
    }

    public static Map<Integer, String> getAllEngineCodeName() {
        Map<Integer, String> result = new HashMap<>();

        result.put(COMBUSTION_ENGINE, getEngineTypeName(COMBUSTION_ENGINE));
        result.put(NORMAL_ENGINE, getEngineTypeName(NORMAL_ENGINE));
        result.put(XL_ENGINE, getEngineTypeName(XL_ENGINE));
        result.put(XXL_ENGINE, getEngineTypeName(XXL_ENGINE));
        result.put(FUEL_CELL, getEngineTypeName(FUEL_CELL));
        result.put(LIGHT_ENGINE, getEngineTypeName(LIGHT_ENGINE));
        result.put(COMPACT_ENGINE, getEngineTypeName(COMPACT_ENGINE));
        result.put(FISSION, getEngineTypeName(FISSION));
        result.put(NONE, getEngineTypeName(NONE));
        result.put(MAGLEV, getEngineTypeName(MAGLEV));
        result.put(STEAM, getEngineTypeName(STEAM));
        result.put(BATTERY, getEngineTypeName(BATTERY));
        result.put(SOLAR, getEngineTypeName(SOLAR));
        result.put(EXTERNAL, getEngineTypeName(EXTERNAL));

        return result;
    }

    /**
     * This returns a non-localized name of the engine, it's mostly used to generate files.
     */
    // Don't localize the marked strings below since they are used in mek
    // file parsing.
    public String getEngineName() {
        if (!isValidEngine()) {
            return Messages.getString("Engine.invalid");
        }
        StringBuilder sb = new StringBuilder();
        if (!hasFlag(SUPPORT_VEE_ENGINE)) {
            sb.append(engineRating);
        }
        if (hasFlag(LARGE_ENGINE)) {
            sb.append(Messages.getString("Engine.Large"));
        }
        sb.append(Messages.getString("Engine." + TYPE_KEYS[engineType]));
        if (hasFlag(CLAN_ENGINE)) {
            sb.append(Messages.getString("Engine.Clan"));
        }
        if (hasFlag(SUPPORT_VEE_ENGINE)) {
            sb.append(Messages.getString("Engine.SupportVehicle"));
        } else if (hasFlag(TANK_ENGINE)) {
            sb.append(Messages.getString("Engine.Vehicle"));
        }
        return sb.toString();
    }

    /**
     * @return The rating of the engine
     */
    public int getRating() {
        return engineRating;
    }

    /**
     * @return The slots taken up by the engine in the center torso.
     */
    public int[] getCenterTorsoCriticalSlots(int gyroType) {
        if (engineType == COMPACT_ENGINE) {
            int[] slots;
            if (hasFlag(SUPERHEAVY_ENGINE)) {
                slots = new int[] { 0, 1 };
            } else {
                slots = new int[] { 0, 1, 2 };
            }
            return slots;
        } else if (hasFlag(LARGE_ENGINE)) {
            if (gyroType == Mek.GYRO_COMPACT) {
                int[] slots;
                if (hasFlag(SUPERHEAVY_ENGINE)) {
                    slots = new int[] { 0, 1, 2, 5 };
                } else {
                    slots = new int[] { 0, 1, 2, 5, 6, 7, 8, 9 };
                }
                return slots;
            }
            int[] slots;
            if (hasFlag(SUPERHEAVY_ENGINE)) {
                slots = new int[] { 0, 1, 2, 5 };
            } else {
                slots = new int[] { 0, 1, 2, 7, 8, 9, 10, 11 };
            }
            return slots;
        } else {
            if (gyroType == Mek.GYRO_COMPACT) {
                int[] slots;
                if (hasFlag(SUPERHEAVY_ENGINE)) {
                    slots = new int[] { 0, 1, 2 };
                } else {
                    slots = new int[] { 0, 1, 2, 5, 6, 7 };
                }
                return slots;
            } else if (gyroType == Mek.GYRO_XL) {
                int[] slots;
                if (hasFlag(SUPERHEAVY_ENGINE)) {
                    slots = new int[] { 0, 1, 2 };
                } else {
                    slots = new int[] { 0, 1, 2, 9, 10, 11 };
                }
                return slots;
            } else {
                int[] slots;
                if (hasFlag(SUPERHEAVY_ENGINE)) {
                    slots = new int[] { 0, 1, 2 };
                } else {
                    slots = new int[] { 0, 1, 2, 7, 8, 9 };
                }
                return slots;
            }
        }
    }

    /**
     * @return the engine critical slots in the side torsos.
     */
    public int[] getSideTorsoCriticalSlots() {
        if ((engineType == LIGHT_ENGINE)
              || ((engineType == XL_ENGINE) && hasFlag(CLAN_ENGINE))) {
            int[] slots;
            if (hasFlag(SUPERHEAVY_ENGINE)) {
                slots = new int[] { 0 };
            } else {
                slots = new int[] { 0, 1 };
            }
            return slots;
        } else if (engineType == XL_ENGINE) {
            int[] slots;
            if (hasFlag(SUPERHEAVY_ENGINE)) {
                slots = new int[] { 0, 1 };
            } else {
                slots = new int[] { 0, 1, 2 };
            }
            return slots;
        } else if ((engineType == XXL_ENGINE) && hasFlag(CLAN_ENGINE)) {
            int[] slots;
            if (hasFlag(SUPERHEAVY_ENGINE)) {
                slots = new int[] { 0, 1 };
            } else {
                slots = new int[] { 0, 1, 2, 3 };
            }
            return slots;
        } else if (engineType == XXL_ENGINE) {
            int[] slots;
            if (hasFlag(SUPERHEAVY_ENGINE)) {
                slots = new int[] { 0, 1, 2 };
            } else {
                slots = new int[] { 0, 1, 2, 3, 4, 5 };
            }
            return slots;
        } else {
            return new int[] {};
        }
    }

    /**
     * @return the heat generated while the mek is standing still.
     */
    public int getStandingHeat(Entity entity) {
        return (engineType == XXL_ENGINE && !entity.hasWorkingSCM()) ? 2 : 0;
    }

    /**
     * @return the heat generated while the mek is walking. Only Meks generate movement heat.
     */
    public int getWalkHeat(Entity entity) {
        if (!(entity instanceof Mek mek)) {
            return 0;
        }
        return switch (engineType) {
            // ICE/Fuel Cell Meks generate movement heat per TacOps pg 85
            case COMBUSTION_ENGINE, FUEL_CELL -> 1;
            case XXL_ENGINE -> mek.hasWorkingSCM() ? 0 : 4;
            default -> mek.hasWorkingSCM() ? 0 : 1;
        };
    }

    /**
     * @return the heat generated while the mek is running. Only Meks generate movement heat.
     */
    public int getRunHeat(Entity entity) {
        if (!(entity instanceof Mek mek)) {
            return 0;
        }
        return switch (engineType) {
            // ICE/Fuel Cell Meks generate movement heat per TacOps pg 85
            case COMBUSTION_ENGINE, FUEL_CELL -> 2;
            case XXL_ENGINE -> mek.hasWorkingSCM() ? 0 : 6;
            default -> mek.hasWorkingSCM() ? 0 : 2;
        };
    }

    /**
     * @return the heat generated while the mek is sprinting. Only Meks generate movement heat.
     */
    public int getSprintHeat(Entity entity) {
        if (!(entity instanceof Mek mek)) {
            return 0;
        }
        return switch (engineType) {
            // ICE/Fuel Cell Meks generate movement heat per TacOps pg 85
            case COMBUSTION_ENGINE, FUEL_CELL -> 3;
            case XXL_ENGINE -> mek.hasWorkingSCM() ? 0 : 9;
            default -> mek.hasWorkingSCM() ? 0 : 3;
        };
    }

    /**
     * @return the heat generated while the mek is jumping.
     */
    public int getJumpHeat(int movedMP) {
        if (engineType == XXL_ENGINE) {
            return Math.max(6, movedMP * 2);
        }
        return Math.max(3, movedMP);
    }

    public double getBVMultiplier() {
        int sideCrits = getSideTorsoCriticalSlots().length;
        if (sideCrits >= 6) {
            return 0.25; // IS XXL
        } else if (sideCrits >= 3) {
            return 0.5; // IS XL, clan XXL, superheavy IS XXL, superheavy IS large XXL
        } else if (sideCrits == 2) {
            return 0.75; // IS light, clan XL, superheavy clan XXL, superheavy clan large XXL
        } else if (sideCrits == 1) {
            return 0.825; // superheavy IS Light, superheavy clan XL
        } else {
            return 1; // standard, compact, ice, fuel cell, fission
        }
    }

    public int getBaseCost() {
        int cost = switch (engineType) {
            case COMBUSTION_ENGINE -> 1250;
            case NORMAL_ENGINE -> 5000;
            case XL_ENGINE -> 20000;
            case XXL_ENGINE -> 100000;
            case COMPACT_ENGINE -> 10000;
            case LIGHT_ENGINE -> 15000;
            case FUEL_CELL -> 3500;
            case FISSION -> 7500;
            default -> 0;
        };
        if (hasFlag(LARGE_ENGINE)) {
            cost *= 2;
        }
        return cost;
    }

    /**
     * Values used for calculating the cost of a support vehicle engine. The engine cost in C-bills is tonnage * 5,000 *
     * type multiplier
     *
     * @param etype The engine type
     *
     * @return The type multiplier for cost
     */
    public static double getSVCostMultiplier(int etype) {
        return switch (etype) {
            case STEAM -> 0.8;
            case BATTERY -> 1.2;
            case FUEL_CELL -> 1.4;
            case SOLAR -> 1.6;
            case MAGLEV -> 2.5;
            case FISSION -> 3.0;
            case NORMAL_ENGINE -> 2.0;
            default -> 1.0;
        };
    }

    private static final TechAdvancement STANDARD_FUSION_TA = new TechAdvancement(TechBase.ALL)
          .setAdvancement(DATE_ES, DATE_ES, 2300)
          .setApproximate(false, false, true)
          .setTechRating(TechRating.D)
          .setAvailability(AvailabilityValue.C, AvailabilityValue.E, AvailabilityValue.D, AvailabilityValue.D)
          .setStaticTechLevel(SimpleTechLevel.INTRO);

    private static final TechAdvancement LARGE_FUSION_TA = new TechAdvancement(TechBase.ALL)
          .setAdvancement(2630, 3085, 3120)
          .setApproximate(false, true, true)
          .setPrototypeFactions(Faction.TH)
          .setProductionFactions(Faction.LC)
          .setTechRating(TechRating.D)
          .setAvailability(AvailabilityValue.C, AvailabilityValue.E, AvailabilityValue.D, AvailabilityValue.D)
          .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);

    private static final TechAdvancement STANDARD_ICE_TA = new TechAdvancement(TechBase.ALL)
          .setAdvancement(DATE_ES, DATE_ES, 2300)
          .setApproximate(false, false, true)
          .setTechRating(TechRating.C)
          .setAvailability(AvailabilityValue.A, AvailabilityValue.A, AvailabilityValue.A, AvailabilityValue.A)
          .setStaticTechLevel(SimpleTechLevel.INTRO);

    private static final TechAdvancement LARGE_ICE_TA = new TechAdvancement(TechBase.ALL)
          .setAdvancement(DATE_NONE, 2630, 3120, DATE_NONE, DATE_NONE)
          .setApproximate(false, false, true, false, false)
          .setPrototypeFactions(Faction.TH)
          .setProductionFactions(Faction.LC).setTechRating(TechRating.C)
          .setAvailability(AvailabilityValue.A, AvailabilityValue.A, AvailabilityValue.A, AvailabilityValue.A)
          .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);

    private static final TechAdvancement LIGHT_FUSION_TA = new TechAdvancement(TechBase.IS)
          .setISAdvancement(3055, 3062, 3067).setISApproximate(true, false, false)
          .setPrototypeFactions(Faction.MERC).setProductionFactions(Faction.LC).setTechRating(TechRating.D)
          .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.D)
          .setStaticTechLevel(SimpleTechLevel.STANDARD);

    private static final TechAdvancement LARGE_LIGHT_FUSION_TA = new TechAdvancement(TechBase.IS)
          .setISAdvancement(3064, 3065)
          .setISApproximate(true)
          .setPrototypeFactions(Faction.LC)
          .setProductionFactions(Faction.LC)
          .setTechRating(TechRating.D)
          .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.E)
          .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
    // Greekfire requested Errata March 2022 for RS Jihad book.
    private static final TechAdvancement COMPACT_FUSION_TA = new TechAdvancement(TechBase.IS)
          .setISAdvancement(3060, 3066, 3072, DATE_NONE, DATE_NONE)
          .setISApproximate(true, false, true, false, false)
          .setPrototypeFactions(Faction.LC).setProductionFactions(Faction.LC)
          .setTechRating(TechRating.E)
          .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.D)
          .setStaticTechLevel(SimpleTechLevel.STANDARD);

    private static final TechAdvancement IS_XL_TA = new TechAdvancement(TechBase.IS)
          .setISAdvancement(2556, 2579, 3045, 2865, 3035)
          .setPrototypeFactions(Faction.TH)
          .setProductionFactions(Faction.TH)
          .setReintroductionFactions(Faction.LC)
          .setTechRating(TechRating.E)
          .setAvailability(AvailabilityValue.D, AvailabilityValue.F, AvailabilityValue.E, AvailabilityValue.D)
          .setStaticTechLevel(SimpleTechLevel.STANDARD);

    private static final TechAdvancement CLAN_XL_TA = new TechAdvancement(TechBase.CLAN)
          .setClanAdvancement(2824, 2827, 2829)
          .setClanApproximate(true)
          .setPrototypeFactions(Faction.CSF)
          .setProductionFactions(Faction.CSF)
          .setTechRating(TechRating.F)
          .setAvailability(AvailabilityValue.D, AvailabilityValue.E, AvailabilityValue.D, AvailabilityValue.D)
          .setStaticTechLevel(SimpleTechLevel.STANDARD);

    private static final TechAdvancement LARGE_IS_XL_TA = new TechAdvancement(TechBase.IS)
          .setISAdvancement(2635, 3085, DATE_NONE, 2822, 3054)
          .setISApproximate(true, true)
          .setPrototypeFactions(Faction.TH)
          .setProductionFactions(Faction.TH)
          .setReintroductionFactions(Faction.LC, Faction.FS)
          .setTechRating(TechRating.E)
          .setAvailability(AvailabilityValue.D, AvailabilityValue.F, AvailabilityValue.E, AvailabilityValue.E)
          .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);

    private static final TechAdvancement LARGE_CLAN_XL_TA = new TechAdvancement(TechBase.CLAN)
          .setClanAdvancement(2850, 3080)
          .setClanApproximate(true, true)
          .setPrototypeFactions(Faction.CIH)
          .setProductionFactions(Faction.CHH)
          .setTechRating(TechRating.F)
          .setAvailability(AvailabilityValue.D, AvailabilityValue.F, AvailabilityValue.E, AvailabilityValue.E)
          .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
    // Greekfire requested Errata March 2022 for RS Jihad book.
    private static final TechAdvancement IS_XXL_TA = new TechAdvancement(TechBase.IS)
          .setISAdvancement(3055, 3125, DATE_NONE, DATE_NONE, DATE_NONE)
          .setISApproximate(false, true, false, false, false)
          .setPrototypeFactions(Faction.FS, Faction.LC)
          .setProductionFactions(Faction.LC)
          .setTechRating(TechRating.F)
          .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E)
          .setStaticTechLevel(SimpleTechLevel.ADVANCED);

    private static final TechAdvancement CLAN_XXL_TA = new TechAdvancement(TechBase.CLAN)
          .setClanAdvancement(3030, 3125, DATE_NONE, DATE_NONE, DATE_NONE)
          .setClanApproximate(false, true)
          .setPrototypeFactions(Faction.CSF)
          .setProductionFactions(Faction.CSF)
          .setTechRating(TechRating.F)
          .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E)
          .setStaticTechLevel(SimpleTechLevel.ADVANCED);

    private static final TechAdvancement LARGE_IS_XXL_TA = new TechAdvancement(TechBase.IS)
          .setISAdvancement(2630, 3130, DATE_NONE, DATE_NONE, DATE_NONE)
          .setISApproximate(false, true, false, false, false)
          .setTechRating(TechRating.F)
          .setPrototypeFactions(Faction.FS).setProductionFactions(Faction.LC)
          .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.F)
          .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);

    private static final TechAdvancement LARGE_CLAN_XXL_TA = new TechAdvancement(TechBase.CLAN)
          .setClanAdvancement(2630, 3130, DATE_NONE, DATE_NONE, DATE_NONE)
          .setClanApproximate(false, true, false, false, false)
          .setTechRating(TechRating.F)
          .setPrototypeFactions(Faction.CSF).setProductionFactions(Faction.CSF)
          .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.F)
          .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);

    private static final TechAdvancement FISSION_TA = new TechAdvancement(TechBase.ALL)
          .setAdvancement(2470, 2882, 3079).setTechRating(TechRating.D)
          .setPrototypeFactions(Faction.TH).setProductionFactions(Faction.TC)
          .setAvailability(AvailabilityValue.E, AvailabilityValue.E, AvailabilityValue.D, AvailabilityValue.D)
          .setStaticTechLevel(SimpleTechLevel.STANDARD);

    private static final TechAdvancement FUEL_CELL_TA = new TechAdvancement(TechBase.ALL)
          .setAdvancement(2300, 2470, 3078).setApproximate(true).setTechRating(TechRating.D)
          .setPrototypeFactions(Faction.TA).setProductionFactions(Faction.TH)
          .setAvailability(AvailabilityValue.C, AvailabilityValue.D, AvailabilityValue.D, AvailabilityValue.C)
          .setStaticTechLevel(SimpleTechLevel.STANDARD);

    private static final TechAdvancement SUPPORT_STEAM_TA = new TechAdvancement(TechBase.ALL)
          .setAdvancement(DATE_PS, DATE_PS, DATE_PS).setTechRating(TechRating.A)
          .setAvailability(AvailabilityValue.A, AvailabilityValue.A, AvailabilityValue.A, AvailabilityValue.A)
          .setStaticTechLevel(SimpleTechLevel.STANDARD);

    private static final TechAdvancement SUPPORT_ICE_TA = new TechAdvancement(TechBase.ALL)
          .setAdvancement(DATE_PS, DATE_PS, DATE_PS).setTechRating(TechRating.B)
          .setAvailability(AvailabilityValue.A, AvailabilityValue.A, AvailabilityValue.A, AvailabilityValue.A)
          .setStaticTechLevel(SimpleTechLevel.STANDARD);

    private static final TechAdvancement SUPPORT_BATTERY_TA = new TechAdvancement(TechBase.ALL)
          .setAdvancement(DATE_PS, DATE_PS, DATE_PS)
          .setTechRating(TechRating.C)
          .setAvailability(AvailabilityValue.A, AvailabilityValue.B, AvailabilityValue.A, AvailabilityValue.A)
          .setStaticTechLevel(SimpleTechLevel.STANDARD);

    private static final TechAdvancement SUPPORT_FUEL_CELL_TA = new TechAdvancement(TechBase.ALL)
          .setAdvancement(DATE_PS, DATE_PS, DATE_PS).setTechRating(TechRating.C)
          .setAvailability(AvailabilityValue.B, AvailabilityValue.C, AvailabilityValue.C, AvailabilityValue.B)
          .setStaticTechLevel(SimpleTechLevel.STANDARD);

    private static final TechAdvancement SUPPORT_SOLAR_TA = new TechAdvancement(TechBase.ALL)
          .setAdvancement(DATE_PS, DATE_PS, DATE_PS)
          .setTechRating(TechRating.C)
          .setAvailability(AvailabilityValue.C, AvailabilityValue.D, AvailabilityValue.C, AvailabilityValue.C)
          .setStaticTechLevel(SimpleTechLevel.STANDARD);

    private static final TechAdvancement SUPPORT_FISSION_TA = new TechAdvancement(TechBase.ALL)
          .setAdvancement(DATE_ES, DATE_ES, DATE_ES).setTechRating(TechRating.C)
          .setAvailability(AvailabilityValue.E, AvailabilityValue.E, AvailabilityValue.D, AvailabilityValue.C)
          .setStaticTechLevel(SimpleTechLevel.STANDARD);

    private static final TechAdvancement SUPPORT_FUSION_TA = new TechAdvancement(TechBase.ALL)
          .setAdvancement(DATE_ES, DATE_ES, DATE_ES).setTechRating(TechRating.C)
          .setAvailability(AvailabilityValue.C, AvailabilityValue.E, AvailabilityValue.D, AvailabilityValue.C)
          .setStaticTechLevel(SimpleTechLevel.STANDARD);

    private static final TechAdvancement SUPPORT_MAGLEV_TA = new TechAdvancement(TechBase.ALL)
          .setAdvancement(DATE_ES, DATE_ES, DATE_ES).setTechRating(TechRating.C)
          .setAvailability(AvailabilityValue.D, AvailabilityValue.F, AvailabilityValue.E, AvailabilityValue.D)
          .setStaticTechLevel(SimpleTechLevel.STANDARD);

    private static final TechAdvancement SUPPORT_NONE_TA = new TechAdvancement(TechBase.ALL)
          .setAdvancement(DATE_PS, DATE_PS, DATE_PS).setTechRating(TechRating.A)
          .setAvailability(AvailabilityValue.A, AvailabilityValue.A, AvailabilityValue.A, AvailabilityValue.A)
          .setStaticTechLevel(SimpleTechLevel.STANDARD);

    private static final TechAdvancement SUPPORT_EXTERNAL_TA = new TechAdvancement(TechBase.ALL)
          .setAdvancement(DATE_NONE, DATE_NONE, DATE_PS)
          .setTechRating(TechRating.B)
          .setAvailability(AvailabilityValue.C, AvailabilityValue.D, AvailabilityValue.C, AvailabilityValue.C);

    public TechAdvancement getTechAdvancement() {
        switch (engineType) {
            case COMBUSTION_ENGINE:
                if (hasFlag(SUPPORT_VEE_ENGINE)) {
                    return SUPPORT_ICE_TA;
                } else {
                    if (hasFlag(LARGE_ENGINE)) {
                        return LARGE_ICE_TA;
                    } else {
                        return STANDARD_ICE_TA;
                    }
                }
            case NORMAL_ENGINE:
                if (hasFlag(SUPPORT_VEE_ENGINE)) {
                    return SUPPORT_FUSION_TA;
                } else {
                    if (hasFlag(LARGE_ENGINE)) {
                        return LARGE_FUSION_TA;
                    } else {
                        return STANDARD_FUSION_TA;
                    }
                }
            case XL_ENGINE:
                if (hasFlag(CLAN_ENGINE)) {
                    if (hasFlag(LARGE_ENGINE)) {
                        return LARGE_CLAN_XL_TA;
                    } else {
                        return CLAN_XL_TA;
                    }
                } else {
                    if (hasFlag(LARGE_ENGINE)) {
                        return LARGE_IS_XL_TA;
                    } else {
                        return IS_XL_TA;
                    }
                }
            case XXL_ENGINE:
                if (hasFlag(CLAN_ENGINE)) {
                    if (hasFlag(LARGE_ENGINE)) {
                        return LARGE_CLAN_XXL_TA;
                    } else {
                        return CLAN_XXL_TA;
                    }
                } else {
                    if (hasFlag(LARGE_ENGINE)) {
                        return LARGE_IS_XXL_TA;
                    } else {
                        return IS_XXL_TA;
                    }
                }
            case FUEL_CELL:
                if (hasFlag(SUPPORT_VEE_ENGINE)) {
                    return SUPPORT_FUEL_CELL_TA;
                } else {
                    return FUEL_CELL_TA;
                }
            case LIGHT_ENGINE:
                if (hasFlag(LARGE_ENGINE)) {
                    return LARGE_LIGHT_FUSION_TA;
                } else {
                    return LIGHT_FUSION_TA;
                }
            case COMPACT_ENGINE:
                return COMPACT_FUSION_TA;
            case FISSION:
                if (hasFlag(SUPPORT_VEE_ENGINE)) {
                    return SUPPORT_FISSION_TA;
                } else {
                    return FISSION_TA;
                }
            case MAGLEV:
                return SUPPORT_MAGLEV_TA;
            case STEAM:
                return SUPPORT_STEAM_TA;
            case BATTERY:
                return SUPPORT_BATTERY_TA;
            case SOLAR:
                return SUPPORT_SOLAR_TA;
            case NONE:
                return SUPPORT_NONE_TA;
            case EXTERNAL:
                return SUPPORT_EXTERNAL_TA;
            default:
                return new TechAdvancement();
        }
    }

    /**
     * @param year The game year
     *
     * @return the tech type (tech level + tech base) for the current engine.
     */
    public int getTechType(int year) {
        boolean isLarge = hasFlag(LARGE_ENGINE);
        boolean isClan = hasFlag(CLAN_ENGINE);
        boolean isSV = hasFlag(SUPPORT_VEE_ENGINE);
        switch (engineType) {
            case NORMAL_ENGINE:
                if ((isClan) && (isLarge)) {
                    if (year <= 2630) {
                        return TechConstants.T_CLAN_UNOFFICIAL;
                    } else if (year <= 3080) {
                        return TechConstants.T_CLAN_EXPERIMENTAL;
                    } else if (year <= 3120) {
                        return TechConstants.T_CLAN_ADVANCED;
                    } else {
                        return TechConstants.T_CLAN_TW;
                    }
                } else if (isClan) {
                    return TechConstants.T_CLAN_TW;
                } else if (isSV) {
                    return TechConstants.T_ALLOWED_ALL;
                } else if (year <= 2285) {
                    return TechConstants.T_IS_UNOFFICIAL;
                } else {
                    return TechConstants.T_INTRO_BOX_SET;
                }
            case XL_ENGINE:
                if ((isClan) && (isLarge)) {
                    if (year <= 2845) {
                        return TechConstants.T_CLAN_UNOFFICIAL;
                    } else if (year <= 3075) {
                        return TechConstants.T_CLAN_EXPERIMENTAL;
                    } else {
                        return TechConstants.T_CLAN_ADVANCED;
                    }
                } else if (isClan) {
                    if (year <= 2819) {
                        return TechConstants.T_CLAN_UNOFFICIAL;
                    } else if (year <= 2827) {
                        return TechConstants.T_CLAN_EXPERIMENTAL;
                    } else if (year <= 2829) {
                        return TechConstants.T_CLAN_ADVANCED;
                    } else {
                        return TechConstants.T_CLAN_TW;
                    }
                } else if (isLarge) {
                    if (year <= 2630) {
                        return TechConstants.T_IS_UNOFFICIAL;
                    } else if (year <= 2822) {
                        return TechConstants.T_IS_ADVANCED;
                    } else if (year <= 3053) {
                        return TechConstants.T_IS_UNOFFICIAL;
                    } else if (year <= 3080) {
                        return TechConstants.T_IS_EXPERIMENTAL;
                    } else {
                        return TechConstants.T_IS_ADVANCED;
                    }
                } else {
                    if (year <= 2556) {
                        return TechConstants.T_IS_UNOFFICIAL;
                    } else if (year <= 2579) {
                        return TechConstants.T_IS_EXPERIMENTAL;
                    } else if (year <= 2865) {
                        return TechConstants.T_IS_ADVANCED;
                    } else if (year <= 3035) {
                        return TechConstants.T_IS_UNOFFICIAL;
                    } else if (year <= 3045) {
                        return TechConstants.T_IS_TW_NON_BOX;
                    }
                }
            case XXL_ENGINE:
                if ((isClan) && (isLarge)) {
                    if (year <= 3055) {
                        return TechConstants.T_CLAN_UNOFFICIAL;
                    } else if (year <= 3125) {
                        return TechConstants.T_CLAN_EXPERIMENTAL;
                    } else {
                        return TechConstants.T_CLAN_ADVANCED;
                    }
                } else if (isClan) {
                    if (year <= 3030) {
                        return TechConstants.T_CLAN_UNOFFICIAL;
                    } else if (year <= 3120) {
                        return TechConstants.T_CLAN_ADVANCED;
                    }
                } else if (isLarge) {
                    if (year <= 3055) {
                        return TechConstants.T_IS_UNOFFICIAL;
                    } else if (year <= 3120) {
                        return TechConstants.T_IS_ADVANCED;
                    }
                }
            case FISSION:
                if (isClan) {
                    return TechConstants.T_CLAN_UNOFFICIAL;
                } else if (isSV) {
                    return TechConstants.T_ALLOWED_ALL;
                } else if (isLarge) {
                    return TechConstants.T_IS_UNOFFICIAL;
                } else {
                    if (year <= 2470) {
                        return TechConstants.T_IS_EXPERIMENTAL;
                    } else if (year <= 2882) {
                        return TechConstants.T_IS_ADVANCED;
                    } else if (year <= 3079) {
                        return TechConstants.T_IS_TW_NON_BOX;
                    }
                }
            case FUEL_CELL:
                if (isSV) {
                    return TechConstants.T_ALLOWED_ALL;
                } else if (isClan) {
                    return (year > 3078) ? TechConstants.T_CLAN_TW : TechConstants.T_CLAN_ADVANCED;
                } else {
                    if (year <= 2285) {
                        return TechConstants.T_IS_UNOFFICIAL;
                    } else if (year <= 2470) {
                        return TechConstants.T_IS_EXPERIMENTAL;
                    } else if (year <= 3078) {
                        return TechConstants.T_IS_ADVANCED;
                    } else {
                        return TechConstants.T_IS_TW_NON_BOX;
                    }
                }
            case LIGHT_ENGINE:
                if (isClan) {
                    return TechConstants.T_CLAN_UNOFFICIAL;
                } else if (isLarge) {
                    if (year < 3064) {
                        return TechConstants.T_IS_UNOFFICIAL;
                    } else if (year < 3065) {
                        return TechConstants.T_IS_EXPERIMENTAL;
                    } else {
                        return TechConstants.T_IS_ADVANCED;
                    }
                } else {
                    // Dec 2021 - CGL requested errata to move dates back 5 years for all Tech
                    // levels.
                    if (year < 3050) {
                        return TechConstants.T_IS_UNOFFICIAL;
                    } else if (year < 3057) {
                        return TechConstants.T_IS_EXPERIMENTAL;
                    } else if (year < 3061) {
                        return TechConstants.T_IS_ADVANCED;
                    } else {
                        return TechConstants.T_IS_TW_NON_BOX;
                    }
                }
            case COMBUSTION_ENGINE:
                if (isSV) {
                    return TechConstants.T_ALLOWED_ALL;
                } else if (isClan) {
                    return (year > 2807) ? TechConstants.T_CLAN_TW : TechConstants.T_CLAN_UNOFFICIAL;
                } else if (isLarge) {
                    if (year <= 2630) {
                        return TechConstants.T_IS_UNOFFICIAL;
                    } else if (year <= 3080) {
                        return TechConstants.T_IS_EXPERIMENTAL;
                    } else {
                        return TechConstants.T_IS_TW_NON_BOX;
                    }
                } else {
                    return TechConstants.T_ALLOWED_ALL;
                }
            case MAGLEV:
            case STEAM:
            case BATTERY:
            case SOLAR:
            case NONE:
            case EXTERNAL:
                return TechConstants.T_ALLOWED_ALL;
            case COMPACT_ENGINE:
                if (isClan) {
                    return TechConstants.T_CLAN_UNOFFICIAL;
                } else {
                    if (year <= 3060) {
                        return TechConstants.T_IS_UNOFFICIAL;
                    } else if (year <= 3068) {
                        return TechConstants.T_IS_EXPERIMENTAL;
                    } else if (year <= 3072) {
                        return TechConstants.T_IS_ADVANCED;
                    } else {
                        return TechConstants.T_IS_TW_NON_BOX;
                    }
                }
            default:
                return TechConstants.T_TECH_UNKNOWN;
        }
    }

    public int getEngineType() {
        return engineType;
    }

    /**
     * For OmniMeks set the base Chassis HS any variants will only use this and the reset will have to be added.
     *
     * @param amount The number to set as base chassis heat sinks
     */
    public void setBaseChassisHeatSinks(int amount) {
        baseChassisHeatSinks = amount;
    }

    /**
     * Return the Base Chassis Engine heat Sinks or integralHeatSinkCapacity which ever is less.
     *
     * @param compact Whether this engine uses compact heat sinks or not.
     *
     * @return The number of integral heat sinks in the base chassis
     */
    public int getBaseChassisHeatSinks(boolean compact) {
        return Math.min(integralHeatSinkCapacity(compact), baseChassisHeatSinks);
    }

    public int getFlags() {
        return engineFlags;
    }

    @Override
    public TechBase getTechBase() {
        return getTechAdvancement().getTechBase();
    }

    @Override
    public boolean isClan() {
        return getTechAdvancement().isClan();
    }

    @Override
    public boolean isMixedTech() {
        return getTechAdvancement().isMixedTech();
    }

    @Override
    public boolean isIntroLevel() {
        return getTechAdvancement().isIntroLevel();
    }

    @Override
    public boolean isUnofficial() {
        return getTechAdvancement().isUnofficial();
    }

    @Override
    public int getIntroductionDate(boolean clan) {
        return getTechAdvancement().getIntroductionDate(clan);
    }

    @Override
    public int getIntroductionDate(boolean clan, Faction faction) {
        return getTechAdvancement().getIntroductionDate(clan, faction);
    }

    @Override
    public int getIntroductionDate() {
        return getTechAdvancement().getIntroductionDate();
    }

    @Override
    public int getPrototypeDate(boolean clan) {
        return getTechAdvancement().getPrototypeDate(clan);
    }

    @Override
    public int getPrototypeDate() {
        return getTechAdvancement().getPrototypeDate();
    }

    @Override
    public int getPrototypeDate(boolean clan, Faction faction) {
        return getTechAdvancement().getPrototypeDate(clan, faction);
    }

    @Override
    public int getProductionDate(boolean clan) {
        return getTechAdvancement().getProductionDate(clan);
    }

    @Override
    public int getProductionDate() {
        return getTechAdvancement().getProductionDate();
    }

    @Override
    public int getProductionDate(boolean clan, Faction faction) {
        return getTechAdvancement().getProductionDate(clan, faction);
    }

    @Override
    public int getCommonDate(boolean clan) {
        return getTechAdvancement().getCommonDate(clan);
    }

    @Override
    public int getCommonDate() {
        return getTechAdvancement().getCommonDate();
    }

    @Override
    public int getExtinctionDate(boolean clan) {
        return getTechAdvancement().getExtinctionDate(clan);
    }

    @Override
    public int getExtinctionDate() {
        return getTechAdvancement().getExtinctionDate();
    }

    @Override
    public int getExtinctionDate(boolean clan, Faction faction) {
        return getTechAdvancement().getExtinctionDate(clan, faction);
    }

    @Override
    public int getReintroductionDate(boolean clan) {
        return getTechAdvancement().getReintroductionDate(clan);
    }

    @Override
    public int getReintroductionDate() {
        return getTechAdvancement().getReintroductionDate();
    }

    @Override
    public int getReintroductionDate(boolean clan, Faction faction) {
        return getTechAdvancement().getReintroductionDate(clan, faction);
    }

    @Override
    public TechRating getTechRating() {
        return getTechAdvancement().getTechRating();
    }

    @Override
    public AvailabilityValue getBaseAvailability(Era era) {
        return getTechAdvancement().getBaseAvailability(era);
    }

    @Override
    public SimpleTechLevel getStaticTechLevel() {
        return getTechAdvancement().getStaticTechLevel();
    }
} // End class Engine
