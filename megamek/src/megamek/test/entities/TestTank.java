package megamek.test.entities;

import megamek.common.*;

import java.util.Enumeration;
import java.lang.StringBuffer;

public class TestTank extends TestEntity
{
    private Tank tank = null;
    
    public TestTank(Tank tank, TestEntityOption options)
        throws EngineException
    {
        super(options, getEngine(tank), getArmor(tank), getStructure(tank));
        this.tank = tank;
    }

    public static int getTankEngineRating(Tank tank)
    {
        switch(tank.getMovementType())
        {
            case Entity.MovementType.TRACKED:
                return (int) Math.round(tank.getOriginalWalkMP()*
                        tank.getWeight());
            case Entity.MovementType.WHEELED:
                return (int) Math.round(tank.getOriginalWalkMP()*
                        tank.getWeight())-20;
            case Entity.MovementType.HOVER:
                int sf = 0;
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
        }
        return 0;
    }

    private static Engine getEngine(Tank tank)
        throws EngineException
    {
        int type = 0;
        int flag = Engine.TANK_ENGINE;
        if (tank.getEngineType()==0)
            type = Engine.NORMAL_ENGINE;
        else if (tank.getEngineType()==1)
        {
            type = Engine.COMPUSTION_ENGINE;
            flag = 0;
        }
        else if (tank.getEngineType()==2)
            type = Engine.XL_ENGINE;
        if (tank.isClan())
            flag |= Engine.CLAN_ENGINE;
        return new Engine(getTankEngineRating(tank), type, flag);
    }

    private static Structure getStructure(Tank tank)
    {
        int type = Structure.NORMAL_STRUCTURE;
        int flag = 0;

        if (tank.getStructureType()==1)
            type = Structure.ENDO_STEEL_STRUCTURE;

        if (tank.isClan())
            flag |= Structure.CLAN_STRUCTURE;
        return new Structure(type, flag);
    }
    
    private static Armor getArmor(Tank tank)
    {
        int type = Armor.NORMAL_ARMOR;
        int flag = 0;

        if (tank.getArmorType()==1)
            type = Armor.FERRO_FIBROUS_ARMOR;
        if (tank.isClan())
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
        if (tank.getMovementType()==Entity.MovementType.HOVER)
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
        return (!tank.hasNoTurret()?makeLength("Turret:", getPrintSize()-5)+
                makeWeightString(getTankWeightTurret())+"\n":"")+
            (getTankWeightLifting()!=0?makeLength("Lifting Equip:",getPrintSize()-5)+
             makeWeightString(getTankWeightLifting())+"\n":"")+
            (getTankPowerAmplifier()!=0?makeLength("Power Amp:", getPrintSize()-5)+
             makeWeightString(getTankPowerAmplifier())+"\n":"");
    }

    public String printWeightControls()
    {
        return makeLength("Controls:", getPrintSize()-5)+
            makeWeightString(getWeightControls())+"\n";
    }

    public Tank getTank()
    {
        return tank;
    }

    public boolean correctEntity(StringBuffer buff)
    {
        boolean correct = true;
        if (skip())
            return true;
        //buff.append(getName()).append("\n");
        if (!correctWeight(buff))
        {
            buff.insert(0, printTechLevel()+printShortMovement());
            buff.append(printWeightCalculation()).append("\n");
            correct = false;
        }
        if (showFailedEquip() && hasFailedEquipment(buff))
            correct = false;
        return correct;
    }

    public StringBuffer printEntity()
    {
        StringBuffer buff = new StringBuffer();
        buff.append("Tank: ").append(tank.getDisplayName()).append("\n");
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
