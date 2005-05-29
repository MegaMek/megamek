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

package megamek.common.verifier;

import megamek.common.Entity;
import megamek.common.Mech;
import megamek.common.TechConstants;
import megamek.common.CriticalSlot;
import megamek.common.Mounted;
import megamek.common.EquipmentType;
import megamek.common.WeaponType;
import megamek.common.AmmoType;
import megamek.common.MiscType;
import megamek.common.util.StringUtil;

import java.util.Enumeration;
import java.lang.StringBuffer;

public abstract class TestEntity implements TestEntityOption
{
    public final static float CEIL_TON = 1.0f;
    public final static float CEIL_HALFTON = 2.0f;
    public final static float CEIL_QUARTERTON = 4.0f;

    public final static String[] MOVEMENT_CHASSIS_NAMES = { "Building",
        "Biped Mech", "Quad Mech", "Tracked Vehicle", "Wheeled Vehicle",
        "Hovercraft" };
    
    protected Engine engine = null;
    protected Armor armor = null;
    protected Structure structure = null;
    private TestEntityOption options = null;


    public abstract Entity getEntity();
    public abstract boolean isTank();
    public abstract boolean isMech();
    public abstract float getWeightControls();
    public abstract float getWeightMisc();
    public abstract int getWeightHeatSinks();
    public abstract boolean hasDoubleHeatSinks();
    public abstract int getCountHeatSinks();

    public abstract String printWeightMisc();
    public abstract String printWeightControls();

    public abstract boolean correctEntity(StringBuffer buff);
    public abstract StringBuffer printEntity();
    public abstract String getName();

    public String fileString = null; //where the unit came from

    public TestEntity(TestEntityOption options, Engine engine,
            Armor armor, Structure structure)
    {
        this.options = options;
        this.engine = engine;
        this.armor = armor;
        this.structure = structure;
    }

    public boolean isClan()
    {
        return getEntity().isClan();
    }

    public float getWeight()
    {
        return getEntity().getWeight();
    }

    public int getTotalOArmor()
    {
        return getEntity().getTotalOArmor();
    }

    public String getLocationAbbr(int location)
    {
        return getEntity().getLocationAbbr(location);
    }

    public float getWeightCeilingEngine()
    {
        return options.getWeightCeilingEngine();
    }
    public float getWeightCeilingStructure()
    {
        return options.getWeightCeilingStructure();
    }
    public float getWeightCeilingArmor()
    {
        return options.getWeightCeilingArmor();
    }
    public float getWeightCeilingControls()
    {
        return options.getWeightCeilingControls();
    }
    public float getWeightCeilingWeapons()
    {
        return options.getWeightCeilingWeapons();
    }
    public float getWeightCeilingTargComp()
    {
        return options.getWeightCeilingTargComp();
    }
    public float getWeightCeilingGyro()
    {
        return options.getWeightCeilingGyro();
    }
    public float getWeightCeilingTurret()
    {
        return options.getWeightCeilingTurret();
    }
    public float getWeightCeilingPowerAmp()
    {
        return options.getWeightCeilingPowerAmp();
    }
    public float getMaxOverweight()
    {
        return options.getMaxOverweight();
    }
    public boolean showOverweightedEntity()
    {
        return options.showOverweightedEntity();
    }
    public float getMinUnderweight()
    {
        return options.getMinUnderweight();
    }
    public boolean showUnderweightedEntity()
    {
        return options.showUnderweightedEntity();
    }
    public boolean showCorrectArmor()
    {
        return options.showCorrectArmor();
    }
    public boolean showCorrectCritical()
    {
        return options.showCorrectCritical();
    }
    public boolean showFailedEquip()
    {
        return options.showFailedEquip();
    }
    public boolean ignoreFailedEquip(String name)
    {
        return options.ignoreFailedEquip(name);
    }
    public boolean skip()
    {
        return options.skip();
    }
    public int getTargCompCrits()
    {
        return options.getTargCompCrits();
    }
    public int getPrintSize()
    {
        return options.getPrintSize();
    }
    
    protected static float ceil(float f, float type)
    {
        return (float) Math.ceil(f*type)/type;
    }
    protected static float ceilMaxHalf(float f, float type)
    {
        if (type==CEIL_TON)
            return ceil(f, CEIL_HALFTON);
        else
            return ceil(f, type);
    }

    protected static String makeWeightString(float weight)
    {
        return ( weight < 100 ? " ": "")+
            (weight < 10 ? " ": "")+
            Float.toString(weight)+
            ((Math.ceil(weight*10)==weight*10)?"0":"");
    }

    private boolean hasMASC()
    {
        if (getEntity() instanceof Mech)
            return ((Mech)getEntity()).hasMASC();
        else
            return false;
    }

    public String printShortMovement()
    {
        return "Movement: "+Integer.toString(getEntity().getOriginalWalkMP())+
            "/"+Integer.toString((int)Math.ceil(
                        getEntity().getOriginalWalkMP()*1.5))+
            (hasMASC()?"("+Integer.toString(getEntity().getOriginalWalkMP()*2)+
            ")":"")+(getEntity().getOriginalJumpMP()!=0?"/"+
            Integer.toString(getEntity().getOriginalJumpMP()):"")+"\n";
    }

    public String printWeightHeatSinks()
    {
        return StringUtil.makeLength("Heat Sinks: "+
                Integer.toString(getCountHeatSinks())+
                (hasDoubleHeatSinks()?" ["+
                 Integer.toString(2*getCountHeatSinks())+"]":""),
                getPrintSize()-5)+makeWeightString(getWeightHeatSinks())+"\n";
    }

    public String printWeightEngine()
    {
        return StringUtil.makeLength("Engine: "+engine.getShortEngineName(), getPrintSize()-5)+
                makeWeightString(getWeightEngine())+"\n";
    }

    public float getWeightEngine()
    {
        return engine.getWeightEngine(getWeightCeilingEngine());
    }

    public String printWeightStructure()
    {
        return StringUtil.makeLength("Structure: "+
                Integer.toString(getEntity().getTotalOInternal())+" "+
                structure.getShortName(), getPrintSize()-5)+
            makeWeightString(getWeightStructure())+"\n";
    }

    public float getWeightStructure()
    {
        return structure.getWeightStructure(getWeight(),
                getWeightCeilingStructure());
    }

    public String printWeightArmor()
    {
        return StringUtil.makeLength("Armor: "+
                Integer.toString(getTotalOArmor())+" "+
                armor.getShortName(), getPrintSize()-5)+
            makeWeightString(getWeightArmor())+"\n";
    }

    public float getWeightArmor()
    {
        return armor.getWeightArmor(getTotalOArmor(),
                getWeightCeilingArmor());
    }

    public float getWeightMiscEquip(MiscType mt)
    {
        if (mt.hasFlag(MiscType.F_HEAT_SINK) ||
                mt.hasFlag(MiscType.F_DOUBLE_HEAT_SINK))
            return 0f;
        if (mt.hasFlag(MiscType.F_FERRO_FIBROUS))
            return 0f;
        if (mt.hasFlag(MiscType.F_ENDO_STEEL))
            return 0f;

        if (mt.hasFlag(MiscType.F_JUMP_JET))
        {
            if (getWeight() <= 55.0)
                return 0.5f;
            else if (getWeight() <= 85.0)
                return 1.0f;
            else
                return 2.0f;
        } else if (mt.hasFlag(MiscType.F_HATCHET))
            return ceil(getWeight() / 15.0f, getWeightCeilingWeapons());
        else if (mt.hasFlag(MiscType.F_SWORD))
            return ceilMaxHalf(getWeight() / 20.0f,
                    getWeightCeilingWeapons());
        else if (mt.hasFlag(MiscType.F_MASC))
        {
            if (mt.getInternalName().equals("ISMASC"))
                return (float)Math.round(getWeight() / 20.0f);
            else if (mt.getInternalName().equals("CLMASC"))
                return (float)Math.round(getWeight() / 25.0f);
        } else if (mt.hasFlag(MiscType.F_TARGCOMP))
        {
            float fTons = 0.0f;
            for (Enumeration i = getEntity().getWeapons(); i.hasMoreElements();)
            {
                Mounted mo = (Mounted)i.nextElement();
                WeaponType wt = (WeaponType)mo.getType();
                if (wt.hasFlag(WeaponType.F_DIRECT_FIRE))
                    fTons += wt.getTonnage(getEntity());
            }
            if (mt.getInternalName().equals("ISTargeting Computer"))
                return ceil(fTons / 4.0f, getWeightCeilingTargComp());
            else if (mt.getInternalName().equals("CLTargeting Computer"))
                return ceil(fTons / 5.0f, getWeightCeilingTargComp());
        } else
            return mt.getTonnage(getEntity());
        return 0f;
    }

    public float getWeightMiscEquip()
    {
        float weightSum = 0.0f;
        for (Enumeration e = getEntity().getMisc(); e.hasMoreElements(); )
        {
            Mounted m = (Mounted) e.nextElement();
            MiscType mt = (MiscType) m.getType();
            weightSum += getWeightMiscEquip(mt);
        }
        return weightSum;
    }

    public StringBuffer printMiscEquip()
    {
        return printMiscEquip(new StringBuffer());
    }

    public StringBuffer printMiscEquip(StringBuffer buff)
    {
        return printMiscEquip(buff, 20, getPrintSize());
    }

    public StringBuffer printMiscEquip(StringBuffer buff, int posLoc,
            int posWeight)
    {
        for (Enumeration e = getEntity().getMisc(); e.hasMoreElements(); )
        {
            Mounted m = (Mounted) e.nextElement();
            MiscType mt = (MiscType) m.getType();

            if (m.getLocation()==Entity.LOC_NONE)
                continue;

            if (getWeightMiscEquip(mt)==0f)
                continue;

            buff.append(StringUtil.makeLength(mt.getName(),20));
            buff.append(StringUtil.makeLength(getLocationAbbr(m.getLocation()),
                        getPrintSize()-5-20))
                .append(makeWeightString(getWeightMiscEquip(mt)));
            buff.append("\n");
        }
        return buff;
    }

    public float getWeightWeapon()
    {
        float weight = 0.0f;
        for (Enumeration e = getEntity().getWeapons(); e.hasMoreElements(); )
        {
            Mounted m = (Mounted) e.nextElement();
            WeaponType mt = (WeaponType) m.getType();
            weight += mt.getTonnage(getEntity());
        }
        return weight;
    }

    public StringBuffer printWeapon()
    {
        return printWeapon(new StringBuffer());
    }

    public StringBuffer printWeapon(StringBuffer buff)
    {
        return printWeapon(buff, 20, getPrintSize());
    }

    public StringBuffer printWeapon(StringBuffer buff, int posLoc,
            int posWeight)
    {
        for (Enumeration e = getEntity().getWeapons(); e.hasMoreElements(); )
        {
            Mounted m = (Mounted) e.nextElement();
            WeaponType mt = (WeaponType) m.getType();

            // Don't think this can happen, but ...
            if (m.getLocation()==Entity.LOC_NONE)
                continue;

            buff.append(StringUtil.makeLength(mt.getName(),20));
            buff.append(StringUtil.makeLength(getLocationAbbr(m.getLocation()),
                        getPrintSize()-5-20))
                .append(makeWeightString(mt.getTonnage(getEntity())))
                .append("\n");
        }
        return buff;
    }

    public float getWeightAmmo()
    {
        float weight = 0.0f;
        for (Enumeration e = getEntity().getAmmo(); e.hasMoreElements(); )
        {
            Mounted m = (Mounted) e.nextElement();

            // One Shot Ammo
            if (m.getLocation()==Entity.LOC_NONE)
                continue;

            AmmoType mt = (AmmoType) m.getType();
            weight += mt.getTonnage(getEntity());
        }
        return weight;
    }

    public StringBuffer printAmmo()
    {
        return printAmmo(new StringBuffer());
    }

    public StringBuffer printAmmo(StringBuffer buff)
    {
        return printAmmo(buff, 20, getPrintSize());
    }

    public StringBuffer printAmmo(StringBuffer buff, int posLoc, int posWeight)
    {
        for (Enumeration e = getEntity().getAmmo(); e.hasMoreElements(); )
        {
            Mounted m = (Mounted) e.nextElement();
            AmmoType mt = (AmmoType) m.getType();

            // Don't think this can happen, but ...
            if (m.getLocation()==Entity.LOC_NONE)
                continue;

            buff.append(StringUtil.makeLength(mt.getName(),20));
            buff.append(StringUtil.makeLength(getLocationAbbr(m.getLocation()),
                        getPrintSize()-5-20))
                .append(makeWeightString(mt.getTonnage(getEntity())))
                .append("\n");
        }
        return buff;
    }


    public String printLocations()
    {
        StringBuffer buff = new StringBuffer();
        for (int i = 0; i<getEntity().locations(); i++)
        {
            String locationName = getEntity().getLocationName(i);
            buff.append(locationName+":");
            buff.append("\n");
            for (int j = 0; j<getEntity().getNumberOfCriticals(i); j++)
            {
                CriticalSlot slot = getEntity().getCritical(i, j);
                if (slot == null)
                {
                    buff.append(Integer.toString(j)+". -Emtpy-");
                    buff.append("\n");
                } else if (slot.getType()==CriticalSlot.TYPE_SYSTEM)
                {
                    if (isMech())
                    {
                        buff.append(Integer.toString(j)+". "+
                                Mech.systemNames[slot.getIndex()]);
                        buff.append("\n");
                    }
                    else
                    {
                        buff.append(Integer.toString(j)+
                                ". UNKNOWN SYSTEM NAME");
                        buff.append("\n");
                    }
                } else if (slot.getType()==CriticalSlot.TYPE_EQUIPMENT)
                {
                    Mounted m = getEntity().getEquipment(slot.getIndex());

                    buff.append(Integer.toString(j)+". "+
                            m.getType().getInternalName());
                    buff.append("\n");
                }
            }
        }
        return buff.toString();
    }

    public int calcMiscCrits(MiscType mt)
    {
        if (mt.hasFlag(MiscType.F_HATCHET) || mt.hasFlag(MiscType.F_SWORD))
            return (int) Math.ceil(getWeight() / 15.0);
        else if (mt.hasFlag(MiscType.F_MASC))
        {
            if (mt.getInternalName().equals("ISMASC"))
                return (int)Math.round(getWeight() / 20.0f);
            else if (mt.getInternalName().equals("CLMASC"))
                return (int)Math.round(getWeight() / 25.0f);
        } else if (mt.hasFlag(MiscType.F_TARGCOMP))
        {
            float fTons = 0.0f;
            for (Enumeration i = getEntity().getWeapons(); i.hasMoreElements(); )
            {
                 Mounted mo = (Mounted)i.nextElement();
                 WeaponType wt = (WeaponType)mo.getType();
                 if (wt.hasFlag(WeaponType.F_DIRECT_FIRE))
                     fTons += wt.getTonnage(getEntity());
            }
            float weight = 0.0f;
            if (mt.getInternalName().equals("ISTargeting Computer"))
                weight = ceil(fTons / 4.0f, getWeightCeilingTargComp());
            else if (mt.getInternalName().equals("CLTargeting Computer"))
                weight = ceil(fTons / 5.0f, getWeightCeilingTargComp());
            switch(getTargCompCrits())
            {
                case CEIL_TARGCOMP_CRITS:
                    return (int) Math.ceil(weight);
                case ROUND_TARGCOMP_CRITS:
                    return (int) Math.round(weight);
                case FLOOR_TARGCOMP_CRITS:
                    return (int) Math.floor(weight);
            }
        } else if ( MiscType.FERRO_FIBROUS.equals(mt.getInternalName()) )
        {
            if ( isClan() )
                return 7;
            else
                return 14;
        } else if ( MiscType.ENDO_STEEL.equals(mt.getInternalName()) )
        {
            if ( isClan() )
                return 7;
            else
                return 14;
        }
        return mt.getCriticals(getEntity());
    }

    public float calculateWeight()
    {
        float weight = 0;
        weight += getWeightEngine();
        weight += getWeightStructure();
        weight += getWeightControls();
        weight += getWeightHeatSinks();
        weight += getWeightArmor();
        weight += getWeightMisc();

        weight += getWeightMiscEquip();
        weight += getWeightWeapon();
        weight += getWeightAmmo();

        weight += getWeightCarryingSpace();
        return weight;
    }

    public String printWeightCalculation()
    {
        return printWeightEngine()+printWeightStructure()+
            printWeightControls()+printWeightHeatSinks()+
            printWeightArmor()+printWeightMisc()+
            printWeightCarryingSpace()+
            "Equipment:\n"+
            printMiscEquip()+printWeapon()+
            printAmmo();
    }

    public boolean correctWeight(StringBuffer buff)
    {
        return correctWeight(buff,
                showOverweightedEntity(),showUnderweightedEntity());
    }

    public boolean correctWeight(StringBuffer buff, boolean showO,
            boolean showU)
    {
        float weightSum = calculateWeight();
        float weight = getWeight();

        if (showO && weight+getMaxOverweight()<weightSum)
        {
             buff.append("Weight: ").append(calculateWeight())
                 .append(" is greater then ").append(getWeight())
                 .append("\n"); 
             //buff.append(printWeightCalculation()).append("\n");
             return false;
        }
        if (showU && weight-getMinUnderweight()>weightSum)
        {
             buff.append("Weight: ").append(calculateWeight())
                 .append(" is lesser then ").append(getWeight())
                 .append("\n");
             //buff.append(printWeightCalculation()).append("\n");
             return false;
        }
        return true;
    }

    public boolean hasFailedEquipment(StringBuffer buff)
    {
        boolean hasFailedEquipment = false;
        for(Enumeration e = getEntity().getFailedEquipment();
                e.hasMoreElements(); )
        {
            String name = (String) e.nextElement();
            if (!ignoreFailedEquip(name))
            {
                if (!hasFailedEquipment)
                    buff.append("Equipment that Failed to Load:\n");
                buff.append(name).append("\n");
                hasFailedEquipment = true;
            }
        }
        if (hasFailedEquipment)
            buff.append("\n");
        return hasFailedEquipment;
    }

    public StringBuffer printFailedEquipment(StringBuffer buff)
    {
        if (getEntity().getFailedEquipment().hasMoreElements())
            buff.append("Equipment that Failed to Load:\n");
        for(Enumeration e = getEntity().getFailedEquipment();
                e.hasMoreElements(); )
            buff.append(e.nextElement()).append("\n");
        return buff;
    }

    public int getWeightCarryingSpace()
    {
        return getEntity().getTroopCarryingSpace();
    }

    public String printWeightCarryingSpace()
    {
        if (getEntity().getTroopCarryingSpace()!=0)
            return StringUtil.makeLength("Carrying Capacity:", getPrintSize()-5)+
                makeWeightString(getEntity().getTroopCarryingSpace())+"\n";
        return "";
    }

    public String printArmorLocation(int loc)
    {
        if (getEntity().hasRearArmor(loc))
            return StringUtil.makeLength(getEntity().getLocationAbbr(loc)+":", 5)+
                StringUtil.makeLength(getEntity().getOInternal(loc), 4)+
                StringUtil.makeLength(getEntity().getOArmor(loc),3)+" / "+
                StringUtil.makeLength(getEntity().getOArmor(loc,true),2);
        else 
            return StringUtil.makeLength(getEntity().getLocationAbbr(loc)+":", 5)+
                StringUtil.makeLength(getEntity().getOInternal(loc), 4)+
                StringUtil.makeLength(getEntity().getOArmor(loc),6)+"  ";
    }

    public String printArmorPlacement()
    {
        StringBuffer buff = new StringBuffer();
        buff.append("Armor Placement:\n");
        for (int loc=0; loc<getEntity().locations(); loc++)
        {
            buff.append(printArmorLocation(loc)).append("\n");
        }
        return buff.toString();
    }

    public String printTechLevel()
    {
        return "Chassis: "+
            MOVEMENT_CHASSIS_NAMES[getEntity().getMovementType()]+" - "+
            TechConstants.getLevelName(getEntity().getTechLevel())+
            " ("+Integer.toString(getEntity().getYear())+")\n";
    }

} // End class TestEntity


class Engine
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

    private int engineRating;
    private int engineType;
    private int engineFlags;

    public Engine(int engineRating, int engineType, int engineFlags)
        throws EngineException
    {
        isValidEngine(engineRating, engineType, engineFlags);
        this.engineRating = engineRating;
        this.engineType = engineType;
        this.engineFlags = engineFlags;
    }

    private static boolean hasFlag(int x, int flag)
    {
        if ((x & flag) !=0)
            return true;
        return false;
    }

    public static boolean isValidEngine(int rating, int type, int flags)
        throws EngineException
    {
        if (hasFlag(flags, ~(CLAN_ENGINE|TANK_ENGINE)))
            throw new EngineException("Flags", flags);
        switch (type)
        {
            case COMPUSTION_ENGINE:
            case NORMAL_ENGINE:
            case XL_ENGINE:
            case LIGHT_ENGINE:
                break;
            default:
                throw new EngineException("Type", type);
        }
        if ((int)Math.ceil(rating/5)>ENGINE_RATINGS.length ||
                rating<0)
            throw new EngineException("Rating", rating);
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
        }
        return null;
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

class Armor
{
    public final static int NORMAL_ARMOR = 0;
    public final static int FERRO_FIBROUS_ARMOR = 1;

    public final static int CLAN_ARMOR = 0x01;

    private int armorType;
    private int armorFlags;

    public Armor(int armorType, int armorFlags)
    {
        this.armorType = armorType;
        this.armorFlags = armorFlags;
    }

    public float getWeightArmor(int totalOArmor, float roundWeight)
    {
        return getWeightArmor(armorType, armorFlags, totalOArmor, roundWeight);
    }

    public static float getWeightArmor(int armorType, int armorFlags, 
            int totalOArmor, float roundWeight)
    {
        float totalOArmorWeight = (float) totalOArmor / 16.0f;
        if (armorType==FERRO_FIBROUS_ARMOR)
        {
            if ((armorFlags & CLAN_ARMOR) != 0)
                totalOArmorWeight /= 1.2f;
            else
                totalOArmorWeight /= 1.12f;
        }
        return TestEntity.ceilMaxHalf(totalOArmorWeight, roundWeight);
    }

    public String getShortName()
    {
        return getShortName(armorType);
    }   
     
    public static String getShortName(int armorType)
    {
        switch(armorType)
        {
            case NORMAL_ARMOR:
                return "";
            case FERRO_FIBROUS_ARMOR:
                return "(ferrous)";
            default:
                return null;
        }
    }

} // end class Armor


class Structure
{
    public final static int NORMAL_STRUCTURE = 0;
    public final static int ENDO_STEEL_STRUCTURE = 1;

    public final static int CLAN_STRUCTURE = 0x01;

    private int structureType;
    private int structureFlags;

    public Structure(int structureType, int structureFlags)
    {
        this.structureType = structureType;
        this.structureFlags = structureFlags;
    }

    public float getWeightStructure(float weight, float roundWeight)
    {
        return getWeightStructure(structureType, weight, roundWeight);
    }

    public static float getWeightStructure(int structureType, float weight,
            float roundWeight)
    {
        if (structureType==ENDO_STEEL_STRUCTURE)
            return TestEntity.ceilMaxHalf(weight / 20.0f, roundWeight);
        return weight / 10.0f;
    }

    public String getShortName()
    {
        return getShortName(structureType);
    }

    public static String getShortName(int structureType)
    {
        switch(structureType)
        {
            case NORMAL_STRUCTURE:
                return "";
            case ENDO_STEEL_STRUCTURE:
                return "(endo steel)";
            default:
                return null;
        }
    }
} // End class Structure


class EngineException extends Exception
{
    private int failure;
    private String art;

    public EngineException(String art, int failure)
    {
        super("Engine "+art+" Failure: "+Integer.toString(failure));
        this.art = art;
        this.failure = failure;
    }
} // End class EngineException
