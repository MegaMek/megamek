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

public class Engine implements Serializable
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

        56.5f, 61.0f, 66.5f, 72.5f, 79.5f, 87.5f, 97.0f, 107.5f, 119.5f,
        133.5f, 150.0f, 168.5f, 190.0f, 214.5f, 243.0f, 275.5f, 313.0f,
        356.0f, 405.5f, 462.5f };

    //flags
    public final static int CLAN_ENGINE =  0x1;
    public final static int TANK_ENGINE =  0x2;
    public final static int LARGE_ENGINE = 0x4;

    //types
    public final static int COMBUSTION_ENGINE = 0;
    public final static int NORMAL_ENGINE =     1;
    public final static int XL_ENGINE =         2;
    public final static int LIGHT_ENGINE =      3;
    public final static int XXL_ENGINE =        4;
    public final static int COMPACT_ENGINE =    5;

    public boolean engineValid;
    private int engineRating;
    private int engineType;
    private int engineFlags;
    public StringBuffer problem = new StringBuffer("Illegal engine: ");

    public Engine(int engineRating, int engineType, int engineFlags)
    {
        this.engineValid = true;
        this.engineRating = engineRating;
        this.engineType = engineType;
        this.engineFlags = engineFlags;

        if (!isValidEngine())
        {
            this.engineValid = false;
            this.engineRating = 0;
            this.engineType = -1;
            this.engineFlags = -1;
        }
    }

    private boolean hasFlag(int flag)
    {
        if ((this.engineFlags & flag) !=0)
            return true;
        return false;
    }

    private boolean isValidEngine()
    {
        if (hasFlag(~(CLAN_ENGINE|TANK_ENGINE|LARGE_ENGINE)))
        {
            this.problem.append("Flags:" + this.engineFlags);
            return false;
        }

        if ((int)Math.ceil(this.engineRating/5)>ENGINE_RATINGS.length ||
                this.engineRating<0)
        {
            this.problem.append("Rating:" + this.engineRating);
            return false;
        }
        if (this.engineRating > 400)
            this.engineFlags |= LARGE_ENGINE;

        switch (this.engineType)
        {
            case COMBUSTION_ENGINE:
            case NORMAL_ENGINE:
            case XL_ENGINE:
            case XXL_ENGINE:
                break;
            case COMPACT_ENGINE:
                if (hasFlag(TANK_ENGINE)) {
                    this.problem.append(Messages.getString("Engine.invalidMechOnly"));
                    return false;
                }
                if (hasFlag(LARGE_ENGINE)) {
                    this.problem.append(Messages.getString("Engine.invalidCompactLarge"));
                    return false;
                }
                break;
            case LIGHT_ENGINE:
                if (hasFlag(CLAN_ENGINE)) {
                    this.problem.append(Messages.getString("Engine.invalidSphereOnly"));
                    return false;
                }
                break;
            default:
                this.problem.append("Type:" + this.engineType);
                return false;
        }


        return true;
    }

    public static int getEngineTypeByString(String type) {
        if (type.toLowerCase().indexOf("xxl") != -1)
            return XXL_ENGINE;
        else if (type.toLowerCase().indexOf("xl") != -1)
            return XL_ENGINE;
        else if (type.toLowerCase().indexOf("light") != -1)
            return LIGHT_ENGINE;
        else if (type.toLowerCase().indexOf("compact") != -1)
            return COMPACT_ENGINE;
        else if (type.toLowerCase().indexOf("ice") != -1)
            return COMBUSTION_ENGINE;
        else if (type.toLowerCase().indexOf("i.c.e.") != -1)
            return COMBUSTION_ENGINE;
        else
            return NORMAL_ENGINE;
    }

    public boolean isFusion()
    {
        if (engineType==COMBUSTION_ENGINE)
            return false;
        return true;
    }

    public float getWeightEngine()
    {
        return getWeightEngine(TestEntity.CEIL_HALFTON);
    }

    public float getWeightEngine(float roundWeight)
    {
        float weight = ENGINE_RATINGS[(int)Math.ceil(engineRating/5)];
        switch (engineType)
        {
            case COMBUSTION_ENGINE:
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
            case XXL_ENGINE:
                weight *= 0.33f;
                break;
            case COMPACT_ENGINE:
                weight *= 1.5f;
                break;
        }

        if (hasFlag(TANK_ENGINE) && engineType != COMBUSTION_ENGINE)
            weight *= 1.5f;

        return TestEntity.ceilMaxHalf(weight, roundWeight);
    }

    public int getCountEngineHeatSinks()
    {
        if (!isFusion())
            return 0;
        return 10;
    }

    public int integralHeatSinkCapacity()
    {
        if (!isFusion())
            return 0;
        return engineRating / 25;
    }

    public String getShortEngineName()
    {
        switch (engineType)
        {
            case COMBUSTION_ENGINE:
                return Integer.toString(engineRating)+
                    Messages.getString("Engine.ICE");
            case NORMAL_ENGINE:
                return Integer.toString(engineRating);
            case XL_ENGINE:
                return Integer.toString(engineRating)+
                    Messages.getString("Engine.XL");
            case LIGHT_ENGINE:
                return Integer.toString(engineRating)+
                    Messages.getString("Engine.Light");
            case XXL_ENGINE:
                return Integer.toString(engineRating)+
                    Messages.getString("Engine.XXL");
            case COMPACT_ENGINE:
                return Integer.toString(engineRating)+
                    Messages.getString("Engine.Compact");
            default:
                return Messages.getString("Engine.invalid");
        }
    }

    //Don't localize the marked strings below since they are used in mech
    //file parsing.
    public String getEngineName()
    {
        StringBuffer sb = new StringBuffer();
        sb.append(Integer.toString(engineRating));
        if (hasFlag(LARGE_ENGINE))
            sb.append(Messages.getString("Engine.Large"));
        switch (engineType)
        {
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
            default:
                return this.problem.toString();
        }
        if (hasFlag(CLAN_ENGINE))
            sb.append(Messages.getString("Engine.Clan"));
        if (hasFlag(TANK_ENGINE))
            sb.append(Messages.getString("Engine.Vehicle"));
        return sb.toString();
    }

    public int getRating()
    {
        return engineRating;
    }

    public int[] getCenterTorsoCriticalSlots() {
        if (this.engineType == COMPACT_ENGINE) {
            int[] slots = {0, 1, 2};
            return slots;
        } else if (hasFlag(LARGE_ENGINE)) {
            int[] slots = {0, 1, 2, 7, 8, 9, 10, 11};
            return slots;
        } else {
            int[] slots = {0, 1, 2, 7, 8, 9};
            return slots;
        }
    }

    public int[] getSideTorsoCriticalSlots() {
        if (this.engineType == LIGHT_ENGINE
            || (this.engineType == XL_ENGINE
                && hasFlag(CLAN_ENGINE))) {
            int[] slots = {0, 1};
            return slots;
        } else if (this.engineType == XL_ENGINE) {
            int[] slots = {0, 1, 2};
            return slots;
        } else if (this.engineType == XXL_ENGINE
                   && hasFlag(CLAN_ENGINE)) {
            int[] slots = {0, 1, 2, 3};
            return slots;
        } else if (this.engineType == XXL_ENGINE) {
            int[] slots = {0, 1, 2, 3, 4, 5};
            return slots;
        } else {
            int[] slots = {};
            return slots;
        }
    }

    public int getStandingHeat() {
        switch (engineType) {
            case XXL_ENGINE:
                return 2;
            default:
                return 0;
        }
    }

    public int getWalkHeat() {
        switch (engineType) {
            case XXL_ENGINE:
                return 4;
            default:
                return 1;
        }
    }

    public int getRunHeat() {
        switch (engineType) {
            case XXL_ENGINE:
                return 6;
            default:
                return 2;
        }
    }

    public int getJumpHeat(int movedMP) {
        switch (engineType) {
            case XXL_ENGINE:
                return Math.max(6,movedMP*2);
            default:
                return Math.max(3,movedMP);
        }

    }

    public double getBVMultiplier() {
        int centerCrits = getCenterTorsoCriticalSlots().length;
        int sideCrits = getSideTorsoCriticalSlots().length;
        if(centerCrits > 6) {
            //large engine of some kind
            if(sideCrits >=6)
                return 0.375; // IS large XXL
            else if (sideCrits >=4)
                return 0.5; // clan large XXL
            else if (sideCrits >=2)
                return 0.75; // large XL
            else
                return 1.125; // large
        }
		//normal sized or compact engine
		if(sideCrits >=6)
		    return 0.5; // IS XXL
		else if (sideCrits >=3)
		    return 0.75; // IS XL, clan XXL
		else if (sideCrits >0)
		    return 1.125; // IS L, clan XL
		else
		    return 1.5; //standard, compact, ice
    }

    public int getBaseCost() {
        int cost = 0;
        switch (this.engineType) {
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
        }
        if (hasFlag(LARGE_ENGINE))
            cost *= 2;
        return cost;
    }

    public int getTechType() {
        int level = 1;
        switch (this.engineType) {
            case XL_ENGINE:
            case LIGHT_ENGINE:
                level = 2;
                break;
            case XXL_ENGINE:
            case COMPACT_ENGINE:
                level = 3;
                break;
        }
        if (hasFlag(LARGE_ENGINE))
            level = 3;
        if (level == 3) {
            if (hasFlag(CLAN_ENGINE))
                return TechConstants.T_CLAN_LEVEL_3;
			return TechConstants.T_IS_LEVEL_3;
        } else if (level == 2) {
            if (hasFlag(CLAN_ENGINE))
                return TechConstants.T_CLAN_LEVEL_2;
			return TechConstants.T_IS_LEVEL_2;
        } else {
            return TechConstants.T_IS_LEVEL_1;
        }
    }

} // End class Engine
