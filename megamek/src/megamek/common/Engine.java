/*
 * MegaMek -
 * Copyright (C) 2000,2001,2002,2003,2004,2005 Ben Mazur (bmazur@sev.org)
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

/*
 * Author: Reinhard Vicinus
 */

package megamek.common;

import java.io.Serializable;

import megamek.common.verifier.TestEntity;

/**
 * This class represents an engine, such as those driving mechs.
 */
public class Engine implements Serializable {
    /**
     *
     */
    private static final long serialVersionUID = -246032529363109609L;

    public final static double[] ENGINE_RATINGS = { 0.0, 0.25, 0.5, 0.5,
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
    public final static int CLAN_ENGINE = 0x1;
    public final static int TANK_ENGINE = 0x2;
    public final static int LARGE_ENGINE = 0x4;
    public final static int SUPERHEAVY_ENGINE = 0x8;
    public final static int SUPPORT_VEE_ENGINE = 0x10;

    // types
    public final static int COMBUSTION_ENGINE = 0;
    public final static int NORMAL_ENGINE = 1;
    public final static int XL_ENGINE = 2;
    public final static int XXL_ENGINE = 3;
    public final static int FUEL_CELL = 4;
    public final static int LIGHT_ENGINE = 5;
    public final static int COMPACT_ENGINE = 6;
    public final static int FISSION = 7;
    public final static int NONE = 8;
    public final static int MAGLEV = 9;
    public final static int STEAM = 10;
    public final static int BATTERY = 11;
    public final static int SOLAR = 12;
    
    //These are the SUPPORT VEHICLE ENGINE WEIGHT MULTIPLIERS from TM PG 127
    //The other engine types are assumed to have a value of ) in the array
    //if not listed.
    public final static double[][] SV_ENGINE_RATINGS = new double[13][6];
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
        if ((engineFlags & flag) != 0) {
            return true;
        }
        return false;
    }

    /**
     * Sanity checks the engine, no negative ratings, and similar checks.
     *
     * @return true if the engine is useable.
     */
    private boolean isValidEngine() {
        if (hasFlag(~(CLAN_ENGINE | TANK_ENGINE | LARGE_ENGINE
                | SUPERHEAVY_ENGINE | SUPPORT_VEE_ENGINE))) {
            problem.append("Flags:" + engineFlags);
            return false;
        }
        
        if (hasFlag(SUPPORT_VEE_ENGINE) && (engineType != STEAM)
                && (engineType != COMBUSTION_ENGINE) && (engineType != BATTERY)
                && (engineType != FUEL_CELL) && (engineType != SOLAR)
                && (engineType != FISSION) && (engineType != NORMAL_ENGINE)
                && (engineType != NONE)) {
            problem.append("Invalid Engine type for support vehicle engines!");
            return false;
        }

        if ((((int) Math.ceil(engineRating / 5) > ENGINE_RATINGS.length)
                || (engineRating < 0)) && !hasFlag(SUPPORT_VEE_ENGINE)) {
            problem.append("Rating:" + engineRating);
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
                problem.append("Type:" + engineType);
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
        if (type.toLowerCase().indexOf("xxl") != -1) {
            return XXL_ENGINE;
        } else if (type.toLowerCase().indexOf("xl") != -1) {
            return XL_ENGINE;
        } else if (type.toLowerCase().indexOf("light") != -1) {
            return LIGHT_ENGINE;
        } else if (type.toLowerCase().indexOf("compact") != -1) {
            return COMPACT_ENGINE;
        } else if (type.toLowerCase().indexOf("ice") != -1) {
            return COMBUSTION_ENGINE;
        } else if (type.toLowerCase().indexOf("i.c.e.") != -1) {
            return COMBUSTION_ENGINE;
        } else if (type.toLowerCase().indexOf("fission") != -1) {
            return FISSION;
        } else if (type.toLowerCase().indexOf("fuel cell") != -1) {
            return FUEL_CELL;
        } else if (type.toLowerCase().indexOf("fuel-cell") != -1) {
            return FUEL_CELL;
        } else if (type.toLowerCase().indexOf("none") != -1) {
            return NONE;
        } else if (type.toLowerCase().indexOf("maglev") != -1) {
            return MAGLEV;
        } else if (type.toLowerCase().indexOf("steam") != -1) {
            return STEAM;
        } else if (type.toLowerCase().indexOf("battery") != -1) {
            return BATTERY;
        } else if (type.toLowerCase().indexOf("solar") != -1) {
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
        if ((engineType == COMBUSTION_ENGINE) || (engineType == FISSION) || (engineType == FUEL_CELL) || (engineType == NONE)
        		|| (engineType == BATTERY) || (engineType == SOLAR) || (engineType == STEAM) || (engineType == MAGLEV)      		        		
        		) {
            return false;
        }
        return true;
    }
 

    /**
     * Returns the weight of the engine in tons, rounded to the next highest half
     * ton.
     *
     * @return the weight of the engine.
     */
    public double getWeightEngine(Entity entity) {
        return getWeightEngine(entity, TestEntity.Ceil.HALFTON);
    }

    /**
     * Returns the weight of the engine, rounded by roundWeight.
     *
     * @param roundWeight One of the rounding factors given in
     *            {@link megamek.common.verifier.TestEntity}.
     * @return the weight of the engine in tons.
     */
    public double getWeightEngine(Entity entity, TestEntity.Ceil roundWeight) {
        // Support Vehicles compute engine weight differently
        if ((entity.isSupportVehicle() || hasFlag(SUPPORT_VEE_ENGINE))
                && isValidEngine()) {
            double movementFactor = 4 + entity.getOriginalWalkMP()
                    * entity.getOriginalWalkMP();
            double engineWeightMult = SV_ENGINE_RATINGS[engineType][entity
                    .getEngineTechRating()];
            double weight = entity.getBaseEngineValue() * movementFactor
                    * engineWeightMult * entity.getWeight();
            weight = TestEntity.setPrecision(weight, 4);
            roundWeight = TestEntity.Ceil.HALFTON;
            if (entity.getWeight() < 5) {
                roundWeight = TestEntity.Ceil.KILO;
            }
            return TestEntity.ceil(weight, roundWeight);
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
        weight = TestEntity.ceilMaxHalf(weight, roundWeight);

        if (hasFlag(TANK_ENGINE) && (isFusion() || (engineType == FISSION))) {
            weight *= 1.5;
        }
        
        
        double toReturn = TestEntity.ceilMaxHalf(weight, roundWeight);
        // hover have a minimum weight of 20%
        if ((entity.getMovementMode() == EntityMovementMode.HOVER) && (entity instanceof Tank)) {
            return Math.max(TestEntity.ceilMaxHalf(entity.getWeight()/5, TestEntity.Ceil.HALFTON), toReturn);
        }
        return toReturn;
    }

    /**
     * return the number of heatsinks that fit weight-free into the engine
     * @return
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
     * The name of an Engine is based on it's type.
     *
     * @return the engine name.
     */
    public String getShortEngineName() {
        switch (engineType) {
            case COMBUSTION_ENGINE:
                return Integer.toString(engineRating)
                        + Messages.getString("Engine.ICE");
            case NORMAL_ENGINE:
                return Integer.toString(engineRating);
            case XL_ENGINE:
                return Integer.toString(engineRating)
                        + Messages.getString("Engine.XL");
            case LIGHT_ENGINE:
                return Integer.toString(engineRating)
                        + Messages.getString("Engine.Light");
            case XXL_ENGINE:
                return Integer.toString(engineRating)
                        + Messages.getString("Engine.XXL");
            case COMPACT_ENGINE:
                return Integer.toString(engineRating)
                        + Messages.getString("Engine.Compact");
            case FISSION:
                return Integer.toString(engineRating)
                        + Messages.getString("Engine.Fission");
            case FUEL_CELL:
                return Integer.toString(engineRating)
                        + Messages.getString("Engine.FuelCell");
            case NONE:
                return Integer.toString(engineRating)
                        + Messages.getString("Engine.None");
            case STEAM:
                return Integer.toString(engineRating)
                        + Messages.getString("Engine.Steam");
            case BATTERY:
                return Integer.toString(engineRating)
                        + Messages.getString("Engine.Battery");
            case SOLAR:
                return Integer.toString(engineRating)
                        + Messages.getString("Engine.Solar");
            default:
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
        StringBuffer sb = new StringBuffer();
        sb.append(Integer.toString(engineRating));
        if (hasFlag(LARGE_ENGINE)) {
            sb.append(Messages.getString("Engine.Large"));
        }
        switch (engineType) {
            case COMBUSTION_ENGINE:
                sb.append(" ICE"); //$NON-NLS-1$
                break;
            case NORMAL_ENGINE:
                sb.append(" Fusion"); //$NON-NLS-1$
                break;
            case XL_ENGINE:
                sb.append(" XL"); //$NON-NLS-1$
                break;
            case LIGHT_ENGINE:
                sb.append(" Light"); //$NON-NLS-1$
                break;
            case XXL_ENGINE:
                sb.append(" XXL"); //$NON-NLS-1$
                break;
            case COMPACT_ENGINE:
                sb.append(" Compact"); //$NON-NLS-1$
                break;
            case FUEL_CELL:
                sb.append(" Fuel Cell"); //$NON-NLS-1$
                break;
            case FISSION:
                sb.append(" Fission"); //$NON-NLS-1$
                break;
            case BATTERY:
                sb.append(" Battery"); //$NON-NLS-1$
            case SOLAR:
                sb.append(" Solar");  //$NON-NLS-1$
            case NONE:
                sb.append(" NONE"); //$NON-NLS-1$
                break;
            default:
                return problem.toString();
        }
        if (hasFlag(CLAN_ENGINE)) {
            sb.append(Messages.getString("Engine.Clan"));
        }
        if (hasFlag(TANK_ENGINE)) {
            sb.append(Messages.getString("Engine.Vehicle"));
        }
        return sb.toString();
    }

    /**
     * Returns the rating of the engine.
     *
     * @return
     */
    public int getRating() {
        return engineRating;
    }

    /**
     * returns the slots taken up by the engine in the center torso.
     *
     * @return
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
                slots = new int[]{ 0, 1, 2, 7 };
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
     * Returns the engine criticals in the side torsos.
     *
     * @return
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
            int[] slots = {};
            return slots;
        }
    }

    /**
     * Return the heat generated while the mech is standing still.
     *
     * @return
     */
    public int getStandingHeat() {
        switch (engineType) {
            case XXL_ENGINE:
                return 2;
            default:
                return 0;
        }
    }

    public int getWalkHeat(Entity e) {
        switch (engineType) {
            case COMBUSTION_ENGINE:
            case FUEL_CELL:
                // Industrial Mechs with these engines don't generate heat 
                if (e.getStructureType() 
                        == EquipmentType.T_STRUCTURE_INDUSTRIAL){
                    return 0;
                } else {
                    return 1;
                }
            case XXL_ENGINE:
                return 4;
            default:
                return 1;
        }
    }

    public int getRunHeat(Entity e) {
        switch (engineType) {
            case COMBUSTION_ENGINE:
            case FUEL_CELL:
                // Industrial Mechs with these engines don't generate heat 
                if (e.getStructureType() 
                        == EquipmentType.T_STRUCTURE_INDUSTRIAL){
                    return 0;
                } else {
                    return 2;
                }
            case XXL_ENGINE:
                return 6;
            default:
                return 2;
        }
    }

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

    public int getJumpHeat(int movedMP) {
        switch (engineType) {
            case XXL_ENGINE:
                return Math.max(6, movedMP * 2);
            default:
                return Math.max(3, movedMP);
        }
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
     * Return the tech type (tech level + tech base) for the current engine.
     *
     * @param year
     * @return
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
                }
                if (isClan) {
                    return TechConstants.T_CLAN_TW;
                }
                if (isSV) {
                    return TechConstants.T_ALLOWED_ALL;
                }
                if (year <= 2285) {
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
                }
                if (isClan) {
                    if (year <= 2819) {
                        return TechConstants.T_CLAN_UNOFFICIAL;
                    } else if (year <= 2827) {
                        return TechConstants.T_CLAN_EXPERIMENTAL;
                    } else if (year <= 2829) {
                        return TechConstants.T_CLAN_ADVANCED;
                    } else {
                        return TechConstants.T_CLAN_TW;
                    }
                }
                if (isLarge) {
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
                        } else if (year <=3045){
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
                }
                if (isClan) {
                    if (year <= 2949) {
                        return TechConstants.T_CLAN_UNOFFICIAL;
                    } else if (year <= 3079) {
                        return TechConstants.T_CLAN_EXPERIMENTAL;
                    } else {
                        return TechConstants.T_CLAN_ADVANCED;
                    }
                }
                if (isLarge) {
                    if (year <= 3058) {
                        return TechConstants.T_IS_UNOFFICIAL;
                    } else if (year <= 3130) {
                        return TechConstants.T_IS_EXPERIMENTAL;
                    } else {
                        if (year <= 3050) {
                            return TechConstants.T_IS_EXPERIMENTAL;
                        } else if (year <= 3105) {
                            return TechConstants.T_IS_ADVANCED;
                        }
                    }
                }
            case FISSION:
                if (isClan) {
                    return TechConstants.T_CLAN_UNOFFICIAL;
                }
                if (isSV) {
                    return TechConstants.T_ALLOWED_ALL;
                }
                if (isLarge) {
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
                }
                if (isClan) {
                    if (year <= 3078) {
                        return TechConstants.T_CLAN_ADVANCED;
                    } else {
                        return TechConstants.T_CLAN_TW;
                    }
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
                }
                if (isLarge) {
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
                }
                if (isClan) {
                    if (year <= 2807) {
                        return TechConstants.T_CLAN_UNOFFICIAL;
                    } else {
                        return TechConstants.T_CLAN_TW;
                    }

                }
                if (isLarge) {
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
                return TechConstants.T_ALLOWED_ALL;
            case STEAM:
                return TechConstants.T_ALLOWED_ALL;
            case BATTERY:
                return TechConstants.T_ALLOWED_ALL;
            case SOLAR:
                return TechConstants.T_ALLOWED_ALL;
            case NONE:
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
        }
        return TechConstants.T_TECH_UNKNOWN;
    }
    

    public int getEngineType() {
        return engineType;
    }

    /**
     * For omnis set the base Chassies HS any variants will only use this and the reset will have to be added.
     *
     * @param amount
     * @return
     */
    public void setBaseChassisHeatSinks(int amount) {
        baseChassisHeatSinks = amount;
    }

    /**
     * Return the Base Chassies Engine heat Sinks or intergalHeatSinkCapacity which ever is less.
     *
     * @param compact Whether this engine uses compact heat sinks or not.
     *
     * @return
     */
    public int getBaseChassisHeatSinks(boolean compact) {
        return Math.min(integralHeatSinkCapacity(compact), baseChassisHeatSinks);
    }

    public int getFlags() {
        return engineFlags;
    }
} // End class Engine
