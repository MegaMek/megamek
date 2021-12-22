/*
 * MegaMek -
 * Copyright (C) 2000,2001,2002,2003,2004,2005 Ben Mazur (bmazur@sev.org)
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
package megamek.common;

import java.io.Serializable;

/**
 * This class represents an engine, such as those driving 'Meks.
 * @author Reinhard Vicinus
 */
public class Engine implements Serializable, ITechnology {
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
    
    //These are the SUPPORT VEHICLE ENGINE WEIGHT MULTIPLIERS from TM PG 127
    //The other engine types are assumed to have a value of ) in the array
    //if not listed.
    private static final double[][] SV_ENGINE_RATINGS = new double[NUM_ENGINE_TYPES][6];
    static { 
        SV_ENGINE_RATINGS[STEAM][EquipmentType.RATING_A] = 4.0;
        SV_ENGINE_RATINGS[STEAM][EquipmentType.RATING_B] = 3.5;
        SV_ENGINE_RATINGS[STEAM][EquipmentType.RATING_C] = 3.0;
        SV_ENGINE_RATINGS[STEAM][EquipmentType.RATING_D] = 2.8;
        SV_ENGINE_RATINGS[STEAM][EquipmentType.RATING_E] = 2.6;
        SV_ENGINE_RATINGS[STEAM][EquipmentType.RATING_F] = 2.5;
        
        SV_ENGINE_RATINGS[COMBUSTION_ENGINE][EquipmentType.RATING_A] = 0.0;
        SV_ENGINE_RATINGS[COMBUSTION_ENGINE][EquipmentType.RATING_B] = 3.0;
        SV_ENGINE_RATINGS[COMBUSTION_ENGINE][EquipmentType.RATING_C] = 2.0;
        SV_ENGINE_RATINGS[COMBUSTION_ENGINE][EquipmentType.RATING_D] = 1.5;
        SV_ENGINE_RATINGS[COMBUSTION_ENGINE][EquipmentType.RATING_E] = 1.3;
        SV_ENGINE_RATINGS[COMBUSTION_ENGINE][EquipmentType.RATING_F] = 1.0;
        
        SV_ENGINE_RATINGS[BATTERY][EquipmentType.RATING_A] = 0.0;
        SV_ENGINE_RATINGS[BATTERY][EquipmentType.RATING_B] = 0.0;
        SV_ENGINE_RATINGS[BATTERY][EquipmentType.RATING_C] = 1.5;
        SV_ENGINE_RATINGS[BATTERY][EquipmentType.RATING_D] = 1.2;
        SV_ENGINE_RATINGS[BATTERY][EquipmentType.RATING_E] = 1.0;
        SV_ENGINE_RATINGS[BATTERY][EquipmentType.RATING_F] = 0.8;
        
        SV_ENGINE_RATINGS[FUEL_CELL][EquipmentType.RATING_A] = 0.0;
        SV_ENGINE_RATINGS[FUEL_CELL][EquipmentType.RATING_B] = 0.0;
        SV_ENGINE_RATINGS[FUEL_CELL][EquipmentType.RATING_C] = 1.2;
        SV_ENGINE_RATINGS[FUEL_CELL][EquipmentType.RATING_D] = 1.0;
        SV_ENGINE_RATINGS[FUEL_CELL][EquipmentType.RATING_E] = 0.9;
        SV_ENGINE_RATINGS[FUEL_CELL][EquipmentType.RATING_F] = 0.7;
        
        SV_ENGINE_RATINGS[SOLAR][EquipmentType.RATING_A] = 0.0;
        SV_ENGINE_RATINGS[SOLAR][EquipmentType.RATING_B] = 0.0;
        SV_ENGINE_RATINGS[SOLAR][EquipmentType.RATING_C] = 5.0;
        SV_ENGINE_RATINGS[SOLAR][EquipmentType.RATING_D] = 4.5;
        SV_ENGINE_RATINGS[SOLAR][EquipmentType.RATING_E] = 4.0;
        SV_ENGINE_RATINGS[SOLAR][EquipmentType.RATING_F] = 3.5;
        
        SV_ENGINE_RATINGS[FISSION][EquipmentType.RATING_A] = 0.0;
        SV_ENGINE_RATINGS[FISSION][EquipmentType.RATING_B] = 0.0;
        SV_ENGINE_RATINGS[FISSION][EquipmentType.RATING_C] = 1.75;
        SV_ENGINE_RATINGS[FISSION][EquipmentType.RATING_D] = 1.5;
        SV_ENGINE_RATINGS[FISSION][EquipmentType.RATING_E] = 1.4;
        SV_ENGINE_RATINGS[FISSION][EquipmentType.RATING_F] = 1.3;

        SV_ENGINE_RATINGS[NORMAL_ENGINE][EquipmentType.RATING_A] = 0.0;
        SV_ENGINE_RATINGS[NORMAL_ENGINE][EquipmentType.RATING_B] = 0.0;
        SV_ENGINE_RATINGS[NORMAL_ENGINE][EquipmentType.RATING_C] = 1.5;
        SV_ENGINE_RATINGS[NORMAL_ENGINE][EquipmentType.RATING_D] = 1.0;
        SV_ENGINE_RATINGS[NORMAL_ENGINE][EquipmentType.RATING_E] = 0.75;
        SV_ENGINE_RATINGS[NORMAL_ENGINE][EquipmentType.RATING_F] = 0.5;

        SV_ENGINE_RATINGS[NONE][EquipmentType.RATING_A] = 0.0;
        SV_ENGINE_RATINGS[NONE][EquipmentType.RATING_B] = 0.0;
        SV_ENGINE_RATINGS[NONE][EquipmentType.RATING_C] = 0.0;
        SV_ENGINE_RATINGS[NONE][EquipmentType.RATING_D] = 0.0;
        SV_ENGINE_RATINGS[NONE][EquipmentType.RATING_E] = 0.0;
        SV_ENGINE_RATINGS[NONE][EquipmentType.RATING_F] = 0.0;

        SV_ENGINE_RATINGS[EXTERNAL][EquipmentType.RATING_A] = 0.0;
        SV_ENGINE_RATINGS[EXTERNAL][EquipmentType.RATING_B] = 1.4;
        SV_ENGINE_RATINGS[EXTERNAL][EquipmentType.RATING_C] = 1.0;
        SV_ENGINE_RATINGS[EXTERNAL][EquipmentType.RATING_D] = 0.8;
        SV_ENGINE_RATINGS[EXTERNAL][EquipmentType.RATING_E] = 0.7;
        SV_ENGINE_RATINGS[EXTERNAL][EquipmentType.RATING_F] = 0.6;
    }

    public boolean engineValid;
    private int engineRating;
    private int engineType;
    private int engineFlags;
    private int baseChassisHeatSinks = -1;
    public StringBuffer problem = new StringBuffer("Illegal engine: ");

    /**
     * The constructor takes the rating of the engine, the type of engine and
     * any flags. Engine ratings are divided by the weight of the mech to get
     * they walk MP.
     *
     * @param engineRating the rating of the engine
     * @param engineType the type of the engine, either combustion or a type of
     *            fusion engine.
     * @param engineFlags Wether the engine is a tank engine, a clan engine, or
     *            large engine, or any combination of those.
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
     * @return true if the flag is set.
     */
    public boolean hasFlag(int flag) {
        return (engineFlags & flag) != 0;
    }

    /**
     * Sanity checks the engine, no negative ratings, and similar checks.
     *
     * @return true if the engine is useable.
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

        if ((((int) Math.ceil(engineRating / 5) > ENGINE_RATINGS.length)
                || (engineRating < 0)) && !hasFlag(SUPPORT_VEE_ENGINE)) {
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

    /**
     * returns true if and only if this engine is a fusion engine
     *
     * @return true if it is not an internal combustion engine.
     */
    public boolean isFusion() {
        return (engineType != COMBUSTION_ENGINE) && (engineType != FISSION) && (engineType != FUEL_CELL)
                && (engineType != NONE) && (engineType != BATTERY) && (engineType != SOLAR)
                && (engineType != STEAM) && (engineType != MAGLEV) && (engineType != EXTERNAL);
    }
 

    /**
     * Returns the weight of the engine in tons, rounded to the next highest half
     * ton.
     *
     * @return the weight of the engine.
     */
    public double getWeightEngine(Entity entity) {
        return getWeightEngine(entity, RoundWeight.STANDARD);
    }

    /**
     * Returns the weight of the engine, rounded by roundWeight.
     *
     * @param roundWeight One of the rounding factors given in
     *            {@link megamek.common.verifier.TestEntity}.
     * @return the weight of the engine in tons.
     */
    public double getWeightEngine(Entity entity, RoundWeight roundWeight) {
        // Support Vehicles compute engine weight differently
        if ((entity.isSupportVehicle() || hasFlag(SUPPORT_VEE_ENGINE))
                && isValidEngine()) {
            int mp = entity.getOriginalWalkMP();
            if (entity.getMovementMode().equals(EntityMovementMode.RAIL)
                    || entity.getMovementMode().equals(EntityMovementMode.MAGLEV)) {
                mp = Math.max(0, mp - 2);
            }
            double movementFactor = 4 + mp * mp;
            double engineWeightMult = SV_ENGINE_RATINGS[engineType][entity
                    .getEngineTechRating()];
            double weight = entity.getBaseEngineValue() * movementFactor
                    * engineWeightMult * entity.getWeight();
            // Fusion engines have a minimum weight of 0.25t at D+ and 0.5t at C. Fission engines have
            // a minimum of 0.5t at all tech ratings.
            if ((engineType == NORMAL_ENGINE) && (entity.getEngineTechRating() >= RATING_D)) {
                weight = Math.max(weight, 0.25);
            } else if ((engineType == NORMAL_ENGINE) || (engineType == FISSION)) {
                weight = Math.max(weight, 0.5);
            }
            // Hovercraft have a minimum engine weight of 20% of the vehicle.
            if (entity.getMovementMode().equals(EntityMovementMode.HOVER)) {
                weight = Math.max(weight, entity.getWeight() * 0.2);
            }
            return roundWeight.round(weight, entity);
        }
        // Protomech engines with rating < 40 use a special calculation
        if (entity.hasETypeFlag(Entity.ETYPE_PROTOMECH) && (engineRating < 40)) {
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
        if ((entity.getMovementMode() == EntityMovementMode.HOVER) && (entity instanceof Tank)) {
            toReturn = Math.max(roundWeight.round(entity.getWeight() / 5.0, entity), toReturn);
        }
        return toReturn;
    }

    /**
     * @return the number of heatsinks that fit weight-free into the engine
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
     * Returns the number of heat sinks which can be built into the engine and
     * therefore don't require a critical slot.
     *
     * @param compact Whether this engine uses compact heat sinks or not.
     *
     * @return the maximum number of heat sinks built into the engine.
     */
    public int integralHeatSinkCapacity(boolean compact) {
        if (compact) {
            return (engineRating / 25) * 2;
        }
        else {
            return engineRating / 25;
        }
    }

    /**
     * Get the name of this engine, this is the localized name used in displays.
     * The name of an Engine is based on its type.
     *
     * @return the engine name.
     */
    public String getShortEngineName() {
        if (engineType < TYPE_KEYS.length) {
            return String.format("%d%s", engineRating, Messages.getString("Engine." + TYPE_KEYS[engineType]));
        } else {
            return Messages.getString("Engine.invalid");
        }
    }

    /**
     * This returns a non-localized name of the engine, it's mostly used to
     * generate files.
     */
    // Don't localize the marked strings below since they are used in mech
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
                slots = new int[]{ 0, 1 };
            } else {
                slots = new int[]{ 0, 1, 2 };
            }
            return slots;
        } else if (hasFlag(LARGE_ENGINE)) {
            if (gyroType == Mech.GYRO_COMPACT) {
                int[] slots;
                if (hasFlag(SUPERHEAVY_ENGINE)) {
                    slots = new int[]{ 0, 1, 2, 5 };
                } else {
                    slots = new int[]{ 0, 1, 2, 5, 6, 7, 8, 9 };
                }
                return slots;
            }
            int[] slots;
            if (hasFlag(SUPERHEAVY_ENGINE)) {
                slots = new int[]{ 0, 1, 2, 5 };
            } else {
                slots = new int[]{ 0, 1, 2, 7, 8, 9, 10, 11 };
            }
            return slots;
        } else {
            if (gyroType == Mech.GYRO_COMPACT) {
                int[] slots;
                if (hasFlag(SUPERHEAVY_ENGINE)) {
                    slots = new int[]{ 0, 1, 2 };
                } else {
                    slots = new int[]{ 0, 1, 2, 5, 6, 7 };
                }
                return slots;
            } else if (gyroType == Mech.GYRO_XL) {
                int[] slots;
                if (hasFlag(SUPERHEAVY_ENGINE)) {
                    slots = new int[]{ 0, 1, 2 };
                } else {
                    slots = new int[]{ 0, 1, 2, 9, 10, 11 };
                }
                return slots;
            } else {
                int[] slots;
                if (hasFlag(SUPERHEAVY_ENGINE)) {
                    slots = new int[]{ 0, 1, 2 };
                } else {
                    slots = new int[]{ 0, 1, 2, 7, 8, 9 };
                }
                return slots;
            }
        }
    }

    /**
     * @return the engine criticals in the side torsos.
     */
    public int[] getSideTorsoCriticalSlots() {
        if ((engineType == LIGHT_ENGINE)
                || ((engineType == XL_ENGINE) && hasFlag(CLAN_ENGINE))) {
            int[] slots;
            if (hasFlag(SUPERHEAVY_ENGINE)) {
                slots = new int[]{ 0 };
            } else {
                slots = new int[]{ 0, 1 };
            }
            return slots;
        } else if (engineType == XL_ENGINE) {
            int[] slots;
            if (hasFlag(SUPERHEAVY_ENGINE)) {
                slots = new int[]{ 0, 1 };
            } else {
                slots = new int[]{ 0, 1, 2 };
            }
            return slots;
        } else if ((engineType == XXL_ENGINE) && hasFlag(CLAN_ENGINE)) {
            int[] slots;
            if (hasFlag(SUPERHEAVY_ENGINE)) {
                slots = new int[]{ 0, 1 };
            } else {
                slots = new int[]{ 0, 1, 2, 3 };
            }
            return slots;
        } else if (engineType == XXL_ENGINE) {
            int[] slots;
            if (hasFlag(SUPERHEAVY_ENGINE)) {
                slots = new int[]{ 0, 1, 2 };
            } else {
                slots = new int[]{ 0, 1, 2, 3, 4, 5 };
            }
            return slots;
        } else {
            return new int[]{};
        }
    }

    /**
     * @return the heat generated while the mech is standing still.
     */
    public int getStandingHeat() {
        if (engineType == XXL_ENGINE) {
            return 2;
        }
        return 0;
    }

    /**
     * @return the heat generated while the mech is walking.
     */
    public int getWalkHeat(Entity e) {
        switch (engineType) {
            case COMBUSTION_ENGINE:
            case FUEL_CELL:
                return 0;
            case XXL_ENGINE:
                return 4;
            default:
                return 1;
        }
    }

    /**
     * @return the heat generated while the mech is running.
     */
    public int getRunHeat(Entity e) {
        switch (engineType) {
            case COMBUSTION_ENGINE:
            case FUEL_CELL:
                return 0;
            case XXL_ENGINE:
                return 6;
            default:
                return 2;
        }
    }

    /**
     * @return the heat generated while the mech is sprinting.
     */
    public int getSprintHeat() {
        switch (engineType) {
            case COMBUSTION_ENGINE:
            case FUEL_CELL:
                return 0;
            case XXL_ENGINE:
                return 9;
            default:
                return 3;
        }
    }

    /**
     * @return the heat generated while the mech is jumping.
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
        } else if (sideCrits >= 2) {
            return 0.75; // IS light, clan XL, superheavy clan XXL, superheavy clan large XXL
        } else if (sideCrits >= 1) {
            return 0.825; // superheavy IS Light, superheavy clan XL
        } else {
            return 1; // standard, compact, ice, fuel cell, fission
        }
    }

    public int getBaseCost() {
        int cost = 0;
        switch (engineType) {
            case COMBUSTION_ENGINE:
                cost = 1250;
                break;
            case NORMAL_ENGINE:
                cost = 5000;
                break;
            case XL_ENGINE:
                cost = 20000;
                break;
            case XXL_ENGINE:
                cost = 100000;
                break;
            case COMPACT_ENGINE:
                cost = 10000;
                break;
            case LIGHT_ENGINE:
                cost = 15000;
                break;
            case FUEL_CELL:
                cost = 3500;
                break;
            case FISSION:
                cost = 7500;
                break;
            case NONE:
                cost = 0;
                break;
            }
        if (hasFlag(LARGE_ENGINE)) {
            cost *= 2;
        }
        return cost;
    }

    /**
     * Values used for calculating the cost of a support vehicle engine. The engine cost in C-bills is
     * tonnage * 5,000 * type multiplier
     *
     * @param etype The engine type
     * @return      The type multiplier for cost
     */
    public static double getSVCostMultiplier(int etype) {
        switch (etype) {
            case STEAM:
                return 0.8;
            case BATTERY:
                return 1.2;
            case FUEL_CELL:
                return 1.4;
            case SOLAR:
                return 1.6;
            case MAGLEV:
                return 2.5;
            case FISSION:
                return 3.0;
            case NORMAL_ENGINE:
                return 2.0;
            case COMBUSTION_ENGINE:
            case EXTERNAL:
            default:
                return 1.0;
        }
    }

    private static final TechAdvancement STANDARD_FUSION_TA = new TechAdvancement(TECH_BASE_ALL)
            .setAdvancement(DATE_ES, DATE_ES, 2300).setApproximate(false, false, true)
            .setTechRating(RATING_D).setAvailability(RATING_C, RATING_E, RATING_D, RATING_D)
            .setStaticTechLevel(SimpleTechLevel.INTRO);
    
    private static final TechAdvancement LARGE_FUSION_TA = new TechAdvancement(TECH_BASE_ALL)
            .setAdvancement(2630, 3085, 3120).setApproximate(false, true, true)
            .setPrototypeFactions(F_TH).setProductionFactions(F_LC)
            .setTechRating(RATING_D).setAvailability(RATING_C, RATING_E, RATING_D, RATING_D)
            .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
    
    private static final TechAdvancement STANDARD_ICE_TA = new TechAdvancement(TECH_BASE_ALL)
            .setAdvancement(DATE_ES, DATE_ES, 2300).setApproximate(false, false, true)
            .setTechRating(RATING_C).setAvailability(RATING_A, RATING_A, RATING_A, RATING_A)
            .setStaticTechLevel(SimpleTechLevel.INTRO);
    
    private static final TechAdvancement LARGE_ICE_TA = new TechAdvancement(TECH_BASE_ALL)
            .setAdvancement(DATE_NONE, 2630, 3120, DATE_NONE,DATE_NONE)
            .setApproximate(false, false, true, false, false)
            .setPrototypeFactions(F_TH)
            .setProductionFactions(F_LC).setTechRating(RATING_C)
            .setAvailability(RATING_A, RATING_A, RATING_A, RATING_A)
            .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
    
    private static final TechAdvancement LIGHT_FUSION_TA = new TechAdvancement(TECH_BASE_IS)
            .setISAdvancement(3055, 3062, 3067).setISApproximate(true, false, false)
            .setPrototypeFactions(F_MERC).setProductionFactions(F_LC).setTechRating(RATING_D)
            .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
            .setStaticTechLevel(SimpleTechLevel.STANDARD);
    
    private static final TechAdvancement LARGE_LIGHT_FUSION_TA = new TechAdvancement(TECH_BASE_IS)
            .setISAdvancement(3064, 3065).setISApproximate(true)
            .setPrototypeFactions(F_LC).setProductionFactions(F_LC)
            .setTechRating(RATING_D).setAvailability(RATING_X, RATING_X, RATING_E, RATING_E)
            .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
    
    private static final TechAdvancement COMPACT_FUSION_TA = new TechAdvancement(TECH_BASE_IS)
            .setISAdvancement(3060, 3068, 3072).setISApproximate(true)
            .setPrototypeFactions(F_LC).setProductionFactions(F_LC)
            .setTechRating(RATING_E)
            .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
            .setStaticTechLevel(SimpleTechLevel.STANDARD);            
    
    private static final TechAdvancement IS_XL_TA = new TechAdvancement(TECH_BASE_IS)
            .setISAdvancement(2556, 2579, 3045, 2865, 3035)
            .setPrototypeFactions(F_TH).setProductionFactions(F_TH).setReintroductionFactions(F_LC)
            .setTechRating(RATING_E).setAvailability(RATING_D, RATING_F, RATING_E, RATING_D)
            .setStaticTechLevel(SimpleTechLevel.STANDARD);
    
    private static final TechAdvancement CLAN_XL_TA = new TechAdvancement(TECH_BASE_CLAN)
            .setClanAdvancement(2824, 2827, 2829).setClanApproximate(true)
            .setPrototypeFactions(F_CSF).setProductionFactions(F_CSF)
            .setTechRating(RATING_F).setAvailability(RATING_D, RATING_E, RATING_D, RATING_D)
            .setStaticTechLevel(SimpleTechLevel.STANDARD);
    
    private static final TechAdvancement LARGE_IS_XL_TA = new TechAdvancement(TECH_BASE_IS)
            .setISAdvancement(2635, 3085, DATE_NONE, 2822, 3054).setISApproximate(true, true)
            .setPrototypeFactions(F_TH).setProductionFactions(F_TH).setReintroductionFactions(F_LC, F_FS)
            .setTechRating(RATING_E).setAvailability(RATING_D, RATING_F, RATING_E, RATING_E)
            .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
    
    private static final TechAdvancement LARGE_CLAN_XL_TA = new TechAdvancement(TECH_BASE_CLAN)
            .setClanAdvancement(2850, 3080).setClanApproximate(true, true)
            .setPrototypeFactions(F_CIH).setProductionFactions(F_CHH)
            .setTechRating(RATING_F).setAvailability(RATING_D, RATING_F, RATING_E, RATING_E)
            .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
    
    private static final TechAdvancement IS_XXL_TA = new TechAdvancement(TECH_BASE_IS)
            .setISAdvancement(3055, 3125, DATE_NONE, DATE_NONE, DATE_NONE)
            .setISApproximate(false, true, false, false, false)
            .setPrototypeFactions(F_FS, F_LC).setProductionFactions(F_LC)
            .setTechRating(RATING_F).setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
            .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
    
    private static final TechAdvancement CLAN_XXL_TA = new TechAdvancement(TECH_BASE_CLAN)
            .setClanAdvancement(3030, 3125, DATE_NONE, DATE_NONE, DATE_NONE)
            .setClanApproximate(false, true)
            .setPrototypeFactions(F_CSF).setProductionFactions(F_CSF)
            .setTechRating(RATING_F).setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
            .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
    
    private static final TechAdvancement LARGE_IS_XXL_TA = new TechAdvancement(TECH_BASE_IS)
            .setISAdvancement(2630, 3130, DATE_NONE, DATE_NONE, DATE_NONE)
            .setISApproximate(false,true,false,false,false)
            .setTechRating(RATING_F)
            .setPrototypeFactions(F_FS).setProductionFactions(F_LC)
            .setAvailability(RATING_X, RATING_X, RATING_F, RATING_F)
            .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
    
    private static final TechAdvancement LARGE_CLAN_XXL_TA = new TechAdvancement(TECH_BASE_CLAN)
            .setClanAdvancement(2630, 3130, DATE_NONE, DATE_NONE, DATE_NONE)
            .setClanApproximate(false,true,false,false,false)
            .setTechRating(RATING_F)
            .setPrototypeFactions(F_CSF).setProductionFactions(F_CSF)
            .setAvailability(RATING_X, RATING_X, RATING_F, RATING_F)
            .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
    
    private static final TechAdvancement FISSION_TA = new TechAdvancement(TECH_BASE_ALL)
            .setAdvancement(2470, 2882, 3079).setTechRating(RATING_D)
            .setPrototypeFactions(F_TH).setProductionFactions(F_TC)
            .setAvailability(RATING_E, RATING_E, RATING_D, RATING_D)
            .setStaticTechLevel(SimpleTechLevel.STANDARD);
    
    private static final TechAdvancement FUEL_CELL_TA = new TechAdvancement(TECH_BASE_ALL)
            .setAdvancement(2300, 2470, 3078).setApproximate(true).setTechRating(RATING_D)
            .setPrototypeFactions(F_TA).setProductionFactions(F_TH)
            .setAvailability(RATING_C, RATING_D, RATING_D, RATING_C)
            .setStaticTechLevel(SimpleTechLevel.STANDARD);    
    
    private static final TechAdvancement SUPPORT_STEAM_TA = new TechAdvancement(TECH_BASE_ALL)
            .setAdvancement(DATE_PS, DATE_PS, DATE_PS).setTechRating(RATING_A)
            .setAvailability(RATING_A, RATING_A, RATING_A, RATING_A)
            .setStaticTechLevel(SimpleTechLevel.STANDARD);
    
    private static final TechAdvancement SUPPORT_ICE_TA = new TechAdvancement(TECH_BASE_ALL)
            .setAdvancement(DATE_PS, DATE_PS, DATE_PS).setTechRating(RATING_B)
            .setAvailability(RATING_A, RATING_A, RATING_A, RATING_A)
            .setStaticTechLevel(SimpleTechLevel.STANDARD);
    
    private static final TechAdvancement SUPPORT_BATTERY_TA = new TechAdvancement(TECH_BASE_ALL)
            .setAdvancement(DATE_PS, DATE_PS, DATE_PS)
            .setTechRating(RATING_C)
            .setAvailability(RATING_A, RATING_B, RATING_A, RATING_A)
            .setStaticTechLevel(SimpleTechLevel.STANDARD);
    
    private static final TechAdvancement SUPPORT_FUEL_CELL_TA = new TechAdvancement(TECH_BASE_ALL)
            .setAdvancement(DATE_PS, DATE_PS, DATE_PS).setTechRating(RATING_C)
            .setAvailability(RATING_B, RATING_C, RATING_C, RATING_B)
            .setStaticTechLevel(SimpleTechLevel.STANDARD);
    
    private static final TechAdvancement SUPPORT_SOLAR_TA = new TechAdvancement(TECH_BASE_ALL)
            .setAdvancement(DATE_PS, DATE_PS, DATE_PS)
            .setTechRating(RATING_C)
            .setAvailability(RATING_C, RATING_D, RATING_C, RATING_C)
            .setStaticTechLevel(SimpleTechLevel.STANDARD);
    
    private static final TechAdvancement SUPPORT_FISSION_TA = new TechAdvancement(TECH_BASE_ALL)
            .setAdvancement(DATE_ES, DATE_ES, DATE_ES).setTechRating(RATING_C)
            .setAvailability(RATING_E, RATING_E, RATING_D, RATING_C)
            .setStaticTechLevel(SimpleTechLevel.STANDARD);
    
    private static final TechAdvancement SUPPORT_FUSION_TA = new TechAdvancement(TECH_BASE_ALL)
            .setAdvancement(DATE_ES, DATE_ES, DATE_ES).setTechRating(RATING_C)
            .setAvailability(RATING_C, RATING_E, RATING_D, RATING_C)
            .setStaticTechLevel(SimpleTechLevel.STANDARD);
   
    private static final TechAdvancement SUPPORT_MAGLEV_TA = new TechAdvancement(TECH_BASE_ALL)
            .setAdvancement(DATE_ES, DATE_ES, DATE_ES).setTechRating(RATING_C)
            .setAvailability(RATING_D, RATING_F, RATING_E, RATING_D)
            .setStaticTechLevel(SimpleTechLevel.STANDARD);

    private static final TechAdvancement SUPPORT_NONE_TA = new TechAdvancement(TECH_BASE_ALL)
            .setAdvancement(DATE_PS, DATE_PS, DATE_PS).setTechRating(RATING_A)
            .setAvailability(RATING_A, RATING_A, RATING_A, RATING_A)
            .setStaticTechLevel(SimpleTechLevel.STANDARD);

    private static final TechAdvancement SUPPORT_EXTERNAL_TA = new TechAdvancement(TECH_BASE_ALL)
            .setAdvancement(DATE_NONE, DATE_NONE, DATE_PS)
            .setTechRating(RATING_B).setAvailability(RATING_C, RATING_D, RATING_C, RATING_C);

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
                    return TechConstants.T_INTRO_BOXSET;
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
                    if (year < 3055) {
                        return TechConstants.T_IS_UNOFFICIAL;
                    } else if (year < 3062) {
                        return TechConstants.T_IS_EXPERIMENTAL;
                    } else if (year < 3067) {
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
     * For omnis set the base Chassies HS any variants will only use this and the reset
     * will have to be added.
     *
     * @param amount The number to set as base chassis heat sinks
     */
    public void setBaseChassisHeatSinks(int amount) {
        baseChassisHeatSinks = amount;
    }

    /**
     * Return the Base Chassies Engine heat Sinks or integralHeatSinkCapacity which ever is less.
     *
     * @param compact Whether this engine uses compact heat sinks or not.
     *
     * @return        The number of integral heat sinks in the base chassis
     */
    public int getBaseChassisHeatSinks(boolean compact) {
        return Math.min(integralHeatSinkCapacity(compact), baseChassisHeatSinks);
    }

    public int getFlags() {
        return engineFlags;
    }
    
    @Override
    public int getTechBase() {
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
    public int getIntroductionDate(boolean clan, int faction) {
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
    public int getPrototypeDate(boolean clan, int faction) {
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
    public int getProductionDate(boolean clan, int faction) {
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
    public int getExtinctionDate(boolean clan, int faction) {
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
    public int getReintroductionDate(boolean clan, int faction) {
        return getTechAdvancement().getReintroductionDate(clan, faction);
    }

    @Override
    public int getTechRating() {
        return getTechAdvancement().getTechRating();
    }

    @Override
    public int getBaseAvailability(int era) {
        return getTechAdvancement().getBaseAvailability(era);
    }
    
    @Override
    public SimpleTechLevel getStaticTechLevel() {
        return getTechAdvancement().getStaticTechLevel();
    }
} // End class Engine
