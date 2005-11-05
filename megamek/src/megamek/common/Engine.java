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
import megamek.common.verifier.TestEntity;

public class Engine
{
    public final static float[] ENGINE_RATINGS = { 0.0f, 0.25f,
        0.5f, 0.5f, 0.5f, 0.5f, 1.0f, 1.0f, 1.0f, 1.0f, 1.5f, 1.5f, 1.5f,
        2.0f, 2.0f, 2.0f, 2.5f, 2.5f, 3.0f, 3.0f, 3.0f, 3.5f, 3.5f, 4.0f,
        4.0f, 4.0f, 4.5f, 4.5f, 5.0f, 5.0f, 5.5f, 5.5f, 6.0f, 6.0f, 6.0f,
        7.0f, 7.0f, 7.5f, 7.5f, 8.0f, 8.5f,

        8.5f, 9.0f, 9.5f, 10.0f, 10.0f, 10.5f, 11.0f, 11.5f, 12.0f, 12.5f,
        13.0f, 13.5f, 14.0f, 14.5f, 15.5f, 16.0f, 16.5f, 17.5f, 18.0f,
        19.0f, 19.5f, 10.5f, 21.5f, 22.5f, 23.5f, 24.5f, 25.5f, 27.0f,
        28.5f, 29.5f, 31.5f, 33.0f, 34.5f, 36.5f, 38.5f, 41.0f, 43.5f,
        46.0f, 49.0f, 52.5f,

        56.5f, 61.0f, -1f, 72.5f, 79.5f, 87.5f, 97.0f, 107.5f, -1f,
        133.5f, -1f, 168.5f, -1f, 214.5f, -1f, 275.5f, -1f, 356.0f, 
        405.5f, 462.5f };



    public final static int TANK_ENGINE =  0x10;
    public final static int CLAN_ENGINE =  0x01;


    public final static int COMPUSTION_ENGINE = 0;
    public final static int NORMAL_ENGINE = 1;
    public final static int XL_ENGINE =    2;
    public final static int LIGHT_ENGINE = 3;

    public boolean engineValid;
    private int engineRating;
    private int engineType;
    private int engineFlags;
    public StringBuffer problem = new StringBuffer("Illegal engine: ");

    public Engine(int engineRating, int engineType, int engineFlags)
    {
        if (!isValidEngine(engineRating, engineType, engineFlags))
        {
            this.engineValid = false;
            this.engineRating = 0;
            this.engineType = -1;
            this.engineFlags = -1;
        }
        else
        {
            this.engineValid = true;
            this.engineRating = engineRating;
            this.engineType = engineType;
            this.engineFlags = engineFlags;
        }
    }

    private static boolean hasFlag(int x, int flag)
    {
        if ((x & flag) !=0)
            return true;
        return false;
    }

    public boolean isValidEngine(int rating, int type, int flags)
    {
        if (hasFlag(flags, ~(CLAN_ENGINE|TANK_ENGINE)))
        {
            this.problem.append("Flags:" + flags);
            return false;
        }
        switch (type)
        {
            case COMPUSTION_ENGINE:
            case NORMAL_ENGINE:
            case XL_ENGINE:
            case LIGHT_ENGINE:
                break;
            default:
                this.problem.append("Type:" + type);
                return false;
        }
        if ((int)Math.ceil(rating/5)>ENGINE_RATINGS.length ||
                rating<0)
        {
            this.problem.append("Rating:" + rating);
            return false;
        }
        return true;
    }
    public boolean isFusionEngine()
    {
        return isFusionEngine(engineType);
    }
    public static boolean isFusionEngine(int engineType)
    {
        if (engineType==COMPUSTION_ENGINE)
            return false;
        return true;
    }

    public float getWeightEngine()
    {
        return getWeightEngine(engineRating, engineType, engineFlags,
                TestEntity.CEIL_HALFTON);
    }
    public float getWeightEngine(float roundWeight)
    {
        return getWeightEngine(engineRating, engineType, engineFlags,
                roundWeight);
    }
    public static float getWeightEngine(int engineRating, int engineType,
            int engineFlags, float roundWeight)
    {
        float weight = ENGINE_RATINGS[(int)Math.ceil(engineRating/5)];
        switch (engineType)
        {
            case COMPUSTION_ENGINE:
                weight *= 2.0f;
                break;
            case NORMAL_ENGINE:
                break;
            case XL_ENGINE:
                weight *= 0.5f;
                break;
            case LIGHT_ENGINE:
                weight *= 0.75f;
                break;
        }

        if (hasFlag(engineFlags, TANK_ENGINE))
            weight *= 1.5f;

        return TestEntity.ceilMaxHalf(weight, roundWeight);
        //        getWeightOption(CEIL_ENGINE));
    }
    public int getCountEngineHeatSinks()
    {
        return getCountEngineHeatSinks(engineType);
    }
    public static int getCountEngineHeatSinks(int engineType)
    {
        if (!isFusionEngine(engineType))
            return 0;
        return 10;
    }

    public int integralHeatSinkCapacity()
    {
        return integralHeatSinkCapacity(engineRating, engineType);
    }

    public static int integralHeatSinkCapacity(int engineRating,
            int engineType)
    {
        if (!isFusionEngine(engineType))
            return 0;
        return engineRating / 25;
    }

    public String getShortEngineName()
    {
        return getShortEngineName(engineRating, engineType, engineFlags);
    }
    public static String getShortEngineName(int engineRating, int engineType,
            int engineFlags)
    {
        switch (engineType)
        {
            case COMPUSTION_ENGINE:
                return Integer.toString(engineRating)+" Comp";
            case NORMAL_ENGINE:
                return Integer.toString(engineRating);
            case XL_ENGINE:
                return Integer.toString(engineRating)+ " XL"+
                    (hasFlag(engineFlags, CLAN_ENGINE)?" (Clan)":"");
            case LIGHT_ENGINE:
                return Integer.toString(engineRating)+ " Light"+
                    (hasFlag(engineFlags, CLAN_ENGINE)?" (Clan)":"");
            default:
                return "Invalid Engine!";
        }
    }
    public static String getEngineName(int engineRating, int engineType,
            int engineFlags)
    {
        switch (engineType)
        {
            case COMPUSTION_ENGINE:
                return Integer.toString(engineRating)+" Compustion";
            case NORMAL_ENGINE:
                return (hasFlag(engineFlags, TANK_ENGINE)?"Tank ":"")+
                    Integer.toString(engineRating);
            case XL_ENGINE:
                return (hasFlag(engineFlags, TANK_ENGINE)?"Tank ":"")+
                    Integer.toString(engineRating)+ " XL"+
                    (hasFlag(engineFlags, CLAN_ENGINE)?" (Clan)":"");
            case LIGHT_ENGINE:
                return (hasFlag(engineFlags, TANK_ENGINE)?"Tank ":"")+
                    Integer.toString(engineRating)+ " Light"+
                    (hasFlag(engineFlags, CLAN_ENGINE)?" (Clan)":"");
        }
        return null;
    }

    public int getRating()
    {
        return engineRating;
    }
} // End class Engine
