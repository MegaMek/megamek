package megamek.test.entities;

import java.util.Enumeration;
import java.util.Vector;
import java.util.StringTokenizer;

import gd.xml.tiny.*;

public class TestXMLOption implements TestEntityOption
{
    public final static String CEIL_WEIGHT = "ceilWeight";
    public final static String SHOW_OVERWEIGHTED = "showOverweighted";
    public final static String MAX_OVERWEIGHT = "maxOverweight";
    public final static String SHOW_UNDERWEIGHTED = "showUnderweighted";
    public final static String MIN_UNDERWEIGHT = "minUnderweight";
    public final static String IGNORE_FAILED_EQUIP = "ignoreFailedEquipment";
    public final static String TARGCOMP_CRITS = "targCompCrits";
    public final static String SKIP = "skip";
    public final static String SHOW_CORRECTARMOR = "showCorrectArmorPlacement";
    public final static String SHOW_CORRECTCRITICAL =
        "showCorrectCriticalAllocation";
    public final static String SHOW_FAILEDEQUIP = "showFailedEquip";

    public final static String ENGINE = "engine";
    public final static String STRUCTURE = "structure";
    public final static String ARMOR = "armor";
    public final static String CONTROLS = "controls";
    public final static String WEAPONS = "weapons";
    public final static String TARGCOMP = "tragcomp";
    public final static String TURRET = "turret";
    public final static String POWERAMP = "poweramp";
    public final static String GYRO = "gyro";
    public final static String PRINTSIZE = "printSize";


    private float ceilEngine = TestEntity.CEIL_QUARTERTON;
    private float ceilStructure = TestEntity.CEIL_QUARTERTON;
    private float ceilArmor = TestEntity.CEIL_QUARTERTON;
    private float ceilControls = TestEntity.CEIL_QUARTERTON;
    private float ceilWeapons = TestEntity.CEIL_TON;
    private float ceilTargComp = TestEntity.CEIL_TON;
    private float ceilTurret = TestEntity.CEIL_QUARTERTON;
    private float ceilPowerAmp = TestEntity.CEIL_QUARTERTON;
    private float ceilGyro = TestEntity.CEIL_TON;
        
    private float maxOverweight = 0.25f;
    private boolean showOverweighted = true;
    private float minUnderweight = 1.0f;
    private boolean showUnderweighted = false;
    private Vector ignoreFailedEquip = new Vector();
    private boolean skip = false;
    private boolean showCorrectArmor = true;
    private boolean showCorrectCritical = true;
    private boolean showFailedEquip = true;

    private int targCompCrits = 0;
    private int printSize = 70;


    public TestXMLOption()
    {
    }

    private static String getContent(ParsedXML node)
    {
        if (node.elements().hasMoreElements())
            return ((ParsedXML) node.elements().nextElement()).getContent();
        return "";
    }

    private static float getContentAsFloat(ParsedXML node)
    {
        if (node.elements().hasMoreElements())
            return Float.valueOf(((ParsedXML) node.elements().nextElement())
                    .getContent().trim()).floatValue();
        return 0;
    }

    private static boolean getContentAsBoolean(ParsedXML node)
    {
        if (node.elements().hasMoreElements())
            return (new Boolean(((ParsedXML) node.elements().nextElement()).getContent().trim())).booleanValue();
//            return Boolean.parseBoolean(
//                    ((ParsedXML) node.elements().nextElement()).getContent().trim());
        return false;
    }

    private static int getContentAsInteger(ParsedXML node)
    {
        if (node.elements().hasMoreElements())
            return Integer.parseInt(
                    ((ParsedXML) node.elements().nextElement()).getContent().trim());
        return 0;
    }

    public void readXMLOptions(ParsedXML node)
    {
        for (Enumeration e = node.elements(); e.hasMoreElements(); )
        {
            ParsedXML child = (ParsedXML) e.nextElement();
            if (child.getName().equals(CEIL_WEIGHT))
            {
                readCeilWeight(child);
            }
            else if (child.getName().equals(MAX_OVERWEIGHT))
                maxOverweight = getContentAsFloat(child);
            else if (child.getName().equals(SHOW_OVERWEIGHTED))
                showOverweighted = getContentAsBoolean(child);
            else if (child.getName().equals(MIN_UNDERWEIGHT))
                minUnderweight = getContentAsFloat(child);
            else if (child.getName().equals(SHOW_UNDERWEIGHTED))
                showUnderweighted = getContentAsBoolean(child);
            else if (child.getName().equals(SHOW_CORRECTARMOR))
                showCorrectArmor = getContentAsBoolean(child);
            else if (child.getName().equals(SHOW_CORRECTCRITICAL))
                showCorrectCritical = getContentAsBoolean(child);
            else if (child.getName().equals(SHOW_FAILEDEQUIP))
                showFailedEquip = getContentAsBoolean(child);
            else if (child.getName().equals(IGNORE_FAILED_EQUIP))
            {
                StringTokenizer st = new StringTokenizer(getContent(child), ",");
                while (st.hasMoreTokens()) {
                    ignoreFailedEquip.addElement(st.nextToken());
                }

                for(int i = 0; i < ignoreFailedEquip.size(); i++)
                    ignoreFailedEquip.setElementAt(((String)ignoreFailedEquip.elementAt(i)).trim(), i);
            }
            else if (child.getName().equals(SKIP))
                skip = getContentAsBoolean(child);
            else if (child.getName().equals(TARGCOMP_CRITS))
                targCompCrits = getContentAsInteger(child);
            else if (child.getName().equals(PRINTSIZE))
                printSize = getContentAsInteger(child);
        }
    }

    private void readCeilWeight(ParsedXML node)
    {
        for (Enumeration e = node.elements(); e.hasMoreElements(); )
        {
            ParsedXML child = (ParsedXML) e.nextElement();
            String name = child.getName();
            if (name.equals(ENGINE))
                ceilEngine = getContentAsFloat(child);
            else if (name.equals(STRUCTURE))
                ceilStructure = getContentAsFloat(child);
            else if (name.equals(ARMOR))
                ceilArmor = getContentAsFloat(child);
            else if (name.equals(CONTROLS))
                ceilControls = getContentAsFloat(child);
            else if (name.equals(WEAPONS))
                ceilWeapons = getContentAsFloat(child);
            else if (name.equals(TARGCOMP))
                ceilTargComp = getContentAsFloat(child);
            else if (name.equals(TURRET))
                ceilTurret = getContentAsFloat(child);
            else if (name.equals(POWERAMP))
                ceilPowerAmp = getContentAsFloat(child);
            else if (name.equals(GYRO))
                ceilGyro = getContentAsFloat(child);
        }
    }



    public float getWeightCeilingEngine()
    {
        return ceilEngine;
    }
    public float getWeightCeilingStructure()
    {
        return ceilStructure;
    }
    public float getWeightCeilingArmor()
    {
        return ceilArmor;
    }
    public float getWeightCeilingControls()
    {
        return ceilControls;
    }
    public float getWeightCeilingWeapons() 
    {
        return ceilWeapons;
    }
    public float getWeightCeilingTargComp()
    {
        return ceilTargComp;
    }
    public float getWeightCeilingGyro()
    {
        return ceilGyro;
    }
    public float getWeightCeilingTurret()
    {
        return ceilTurret;
    }
    public float getWeightCeilingPowerAmp()
    {
        return ceilPowerAmp;
    }

    public float getMaxOverweight()
    {
        return maxOverweight;
    }
    public boolean showOverweightedEntity()
    {
        return showOverweighted;
    }
    public boolean showUnderweightedEntity()
    {
        return showUnderweighted;
    }
    public float getMinUnderweight()
    {
        return minUnderweight;
    }

    public boolean ignoreFailedEquip(String name)
    {
        for(int i = 0; i < ignoreFailedEquip.size(); i++)
            if (ignoreFailedEquip.elementAt(i).equals(name))
                return true;
        return false;
    }

    public boolean skip()
    {
        return skip;
    }

    public boolean showCorrectArmor()
    {
        return showCorrectArmor;
    }
    public boolean showCorrectCritical()
    {
        return showCorrectCritical;
    }
    public boolean showFailedEquip()
    {
        return showFailedEquip;
    }

    public int getTargCompCrits()
    {
        return targCompCrits;
    }

    public int getPrintSize()
    {
        return printSize;
    }

    public String printIgnoredFailedEquip()
    {
        System.out.println("--->printIgnoredFailedEquip");
        String ret = "";
        for(int i = 0; i < ignoreFailedEquip.size(); i++)
            ret += "  "+ignoreFailedEquip.elementAt(i)+"\n";
        return ret;
    }

    public String printOptions()
    {
        return 
            "Skip: "+skip()+"\n"+
            "Show Overweighted Entity: "+
            showOverweightedEntity()+"\n"+
            "Max Overweight: "+
            Float.toString(getMaxOverweight())+"\n"+
            "Show Underweighted Entity: "+
            showUnderweightedEntity()+"\n"+
            "Min Underweight: "+
            Float.toString(getMinUnderweight())+"\n"+
            "Show bad Armor Placement: "+
            showCorrectArmor()+"\n"+
            "Show bad Critical Allocation: "+
            showCorrectCritical()+"\n"+
            "Show Failed to Load Equipment: "+
            showFailedEquip()+"\n"+
            "Weight Ceiling Engine: "+
            Float.toString(1 / getWeightCeilingEngine())+"\n"+
            "Weight Ceiling Structure: "+
            Float.toString(1 / getWeightCeilingStructure())+"\n"+
            "Weight Ceiling Armor: "+
            Float.toString(1 / getWeightCeilingArmor())+"\n"+
            "Weight Ceiling Controls: "+
            Float.toString(1 / getWeightCeilingControls())+"\n"+
            "Weight Ceiling Weapons: "+
            Float.toString(1 / getWeightCeilingWeapons())+"\n"+
            "Weight Ceiling TargComp: "+
            Float.toString(1 / getWeightCeilingTargComp())+"\n"+
            "Weight Ceiling Gyro: "+
            Float.toString(1 / getWeightCeilingGyro())+"\n"+
            "Weight Ceiling Turret: "+
            Float.toString(1 / getWeightCeilingTurret())+"\n"+
            "Weight Ceiling PowerAmp: "+
            Float.toString(1 / getWeightCeilingPowerAmp())+"\n"+
            "Ignore Failed Equipment: \n"+
            printIgnoredFailedEquip();
    }
}
