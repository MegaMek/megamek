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

import megamek.common.EquipmentType;
import megamek.common.Entity;
import megamek.common.IEntityMovementMode;
import megamek.common.Tank;
import megamek.common.TechConstants;
import megamek.common.Mounted;
import megamek.common.WeaponType;
import megamek.common.util.StringUtil;
import megamek.common.VTOL;

import java.util.Enumeration;
import java.lang.StringBuffer;

public class TestTank extends TestEntity
{
    private Tank tank = null;

    public TestTank(Tank tank, TestEntityOption options, String fileString)
    {
        super(options, getEngine(tank), getArmor(tank), getStructure(tank));
        this.tank = tank;
        this.fileString = fileString;
    }

    public static int getTankEngineRating(Tank tank)
    {
        int sf = 0;
        switch(tank.getMovementMode())
        {
            case IEntityMovementMode.TRACKED:
                return (int) Math.round(tank.getOriginalWalkMP()*
                        tank.getWeight());
            case IEntityMovementMode.WHEELED:
                return (int) Math.round(tank.getOriginalWalkMP()*
                        tank.getWeight())-20;
            case IEntityMovementMode.HOVER:
                sf = 0;
                if (tank.getWeight()<=10)
                    sf = 40;
                else if (tank.getWeight()<=20)
                    sf = 85;
                else if (tank.getWeight()<=30)
                    sf = 130;
                else if (tank.getWeight()<=40)
                    sf = 175;
                else if (tank.getWeight()<=50)
                    sf = 235;
                return (int) Math.round(tank.getOriginalWalkMP()*
                        tank.getWeight())-sf;
            case IEntityMovementMode.HYDROFOIL:
                sf = 0;
                if (tank.getWeight()<=10)
                    sf = 60;
                else if (tank.getWeight()<=20)
                    sf = 105;
                else if (tank.getWeight()<=30)
                    sf = 150;
                else if (tank.getWeight()<=40)
                    sf = 195;
                else if (tank.getWeight()<=50)
                    sf = 255;
                else if (tank.getWeight()<=60)
                    sf = 300;
                else if (tank.getWeight()<=70)
                    sf = 345;
                else if (tank.getWeight()<=80)
                    sf = 390;
                else if (tank.getWeight()<=90)
                    sf = 435;
                else if (tank.getWeight()<=100)
                    sf = 480;
                return (int) Math.round(tank.getOriginalWalkMP()*
                        tank.getWeight())-sf;
            case IEntityMovementMode.NAVAL:
            case IEntityMovementMode.SUBMARINE:
                return (int) Math.round(tank.getOriginalWalkMP()*
                        tank.getWeight())-30;
        }
        return 0;
    }

    private static Engine getEngine(Tank tank)
    {
        int type = 0;
        int flag = Engine.TANK_ENGINE;
        if (tank.getEngineType()==EquipmentType.T_ENGINE_ICE) {
            type = Engine.COMPUSTION_ENGINE;
            flag = 0;
        } else if (tank.getEngineType()==EquipmentType.T_ENGINE_FUSION) {
            type = Engine.NORMAL_ENGINE;
        } else if (tank.getEngineType()==EquipmentType.T_ENGINE_XL) {
            type = Engine.XL_ENGINE;
        } else if (tank.getEngineType() == EquipmentType.T_ENGINE_LIGHT) {
            type = Engine.LIGHT_ENGINE;
        }
        if (tank.isClan())
            flag |= Engine.CLAN_ENGINE;
        return new Engine(getTankEngineRating(tank), type, flag);
    }

    private static Structure getStructure(Tank tank)
    {
        int type = EquipmentType.T_STRUCTURE_STANDARD;
        int flag = 0;

        if (tank.getStructureType()==1)
            type = EquipmentType.T_STRUCTURE_ENDO_STEEL;

        if (tank.isClan())
            flag |= Structure.CLAN_STRUCTURE;
        return new Structure(type, flag);
    }
    
    private static Armor getArmor(Tank tank)
    {
        int type = EquipmentType.T_ARMOR_STANDARD;
        int flag = 0;

        if (tank.getArmorType()==1)
            type = EquipmentType.T_ARMOR_FERRO_FIBROUS;
        if (tank.isClanArmor())
            flag |= Armor.CLAN_ARMOR;
        return new Armor(type, flag);
    }
    

    public Entity getEntity()
    {
        return tank;
    }

    public boolean isTank()
    {
        return true;
    }

    public boolean isMech()
    {
        return false;
    }

    public float getTankWeightTurret()
    {
        float weight = 0f;
        for (Enumeration e = tank.getWeapons(); e.hasMoreElements(); )
        {
            Mounted m = (Mounted) e.nextElement();
            if (m.getLocation()==Tank.LOC_TURRET)
                weight += ((WeaponType) m.getType()).getTonnage(tank);
        }
        return ceilMaxHalf(weight / 10.0f, getWeightCeilingTurret());
    }

    public float getTankWeightLifting()
    {
        if (tank.getMovementMode()==IEntityMovementMode.HOVER)
            return tank.getWeight() / 10.0f;
        else if (tank.getMovementMode()==IEntityMovementMode.VTOL)
            return tank.getWeight() / 10.0f;
        else if (tank.getMovementMode()==IEntityMovementMode.HYDROFOIL)
            return tank.getWeight() / 10.0f;
        else if (tank.getMovementMode()==IEntityMovementMode.SUBMARINE)
            return tank.getWeight() / 10.0f;
        return 0f;
    }
    public float getTankPowerAmplifier()
    {
        if (!engine.isFusionEngine())
        {
            int weight = 0;
            for (Enumeration e = tank.getWeapons(); e.hasMoreElements(); )
            {   
                Mounted m = (Mounted) e.nextElement();
                WeaponType wt = (WeaponType) m.getType();
                if (wt.hasFlag(WeaponType.F_LASER) ||
                        wt.hasFlag(WeaponType.F_PPC))
                    weight += wt.getTonnage(tank);
            }
            return ceil(weight / 10f, getWeightCeilingPowerAmp());
        }
        return 0f;
    }
    public float getWeightMisc()
    {
        return getTankWeightTurret()+getTankWeightLifting()+
            getTankPowerAmplifier();
    }

    public float getWeightControls()
    {   
        return tank.getWeight() / 20.0f;
    }

    private int getTankCountHeatLaserWeapons()
    {
        int heat = 0; 
        for (Enumeration e = tank.getWeapons(); e.hasMoreElements(); )
        {   
            Mounted m = (Mounted) e.nextElement();
            WeaponType wt = (WeaponType) m.getType();
            if (wt.hasFlag(WeaponType.F_LASER) || wt.hasFlag(WeaponType.F_PPC))
                heat += wt.getHeat();
        }
        return heat;
    }

    public boolean hasDoubleHeatSinks()
    {
        if (!engine.isFusionEngine())
            return false;
        if (getTankCountHeatLaserWeapons() <= 10)
            return false;
        if (tank.getTechLevel()==TechConstants.T_IS_LEVEL_1)
            return false;
        return false;
        //return true;
    }

    public int getCountHeatSinks()
    {
        float heat = getTankCountHeatLaserWeapons();
        if (hasDoubleHeatSinks())
            heat = heat / 2.0f;
        return Math.round(heat);
    }

    public int getWeightHeatSinks()
    {
        int heat = getCountHeatSinks();
        heat -= engine.getCountEngineHeatSinks();
        if (heat<0)
            heat = 0;
        return heat;
    }

    public String printWeightMisc()
    {
        return (!tank.hasNoTurret()?StringUtil.makeLength("Turret:", getPrintSize()-5)+
                makeWeightString(getTankWeightTurret())+"\n":"")+
            (getTankWeightLifting()!=0?StringUtil.makeLength("Lifting Equip:",getPrintSize()-5)+
             makeWeightString(getTankWeightLifting())+"\n":"")+
            (getTankPowerAmplifier()!=0?StringUtil.makeLength("Power Amp:", getPrintSize()-5)+
             makeWeightString(getTankPowerAmplifier())+"\n":"");
    }

    public String printWeightControls()
    {
        return StringUtil.makeLength("Controls:", getPrintSize()-5)+
            makeWeightString(getWeightControls())+"\n";
    }

    public Tank getTank()
    {
        return tank;
    }

    public boolean correctEntity(StringBuffer buff) {
        return correctEntity(buff, true);
    }

    public boolean correctEntity(StringBuffer buff, boolean ignoreAmmo)
    {
        if(tank instanceof VTOL) {return true;} //don't bother checking, won't work.  Needs fixing (new class needed.)
        boolean correct = true;
        if (skip())
            return true;
        if (!correctWeight(buff))
        {
            buff.insert(0, printTechLevel()+printShortMovement());
            buff.append(printWeightCalculation()).append("\n");
            correct = false;
        }
        if (!engine.engineValid)
        {
            buff.append(engine.problem.toString()).append("\n\n");
            correct = false;
        }
        if (showFailedEquip() && hasFailedEquipment(buff))
            correct = false;
        if (hasIllegalTechLevels(buff, ignoreAmmo))
            correct = false;
        // only tanks with fusion engine can be vacuum protected
        if (tank.getEngineType() == 1 && tank.doomedInVacuum())
            correct = false;
        return correct;
    }

    public StringBuffer printEntity()
    {
        StringBuffer buff = new StringBuffer();
        buff.append("Tank: ").append(tank.getDisplayName()).append("\n");
        buff.append("Found in: ").append(this.fileString).append("\n");
        buff.append(printTechLevel());
        buff.append(printShortMovement());
        if (correctWeight(buff, true, true))
            buff.append("Weight: ").append(getWeight())
                .append(" (").append(calculateWeight())
                .append(")\n");

        buff.append(printWeightCalculation()).append("\n");
        printFailedEquipment(buff);
        return buff;
    }

    public String getName()
    {
        return "Tank: "+tank.getDisplayName();
    }
}
