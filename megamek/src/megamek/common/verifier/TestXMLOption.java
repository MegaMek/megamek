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
package megamek.common.verifier;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * 
 * @author Reinhard Vicinus
 */
public class TestXMLOption implements TestEntityOption {

    @XmlElement(name = "ceilWeight")
    private WeightCeiling weightCeiling = new WeightCeiling();
    
    @XmlElement
    private float maxOverweight = 0.25f;

    @XmlElement
    private boolean showOverweighted = true;

    @XmlElement
    private float minUnderweight = 1.0f;

    @XmlElement
    private boolean showUnderweighted = false;

    @XmlElement(name = "ignoreFailedEquipment")
    @XmlJavaTypeAdapter(CSVAdapter.class)
    private List<String> ignoreFailedEquip = new ArrayList<>();

    @XmlElement
    private boolean skip = false;

    @XmlElement(name = "showCorrectArmorPlacement")
    private boolean showCorrectArmor = true;

    @XmlElement(name = "showCorrectCriticalAllocation")
    private boolean showCorrectCritical = true;

    @XmlElement
    private boolean showFailedEquip = true;

    @XmlElement
    private int targCompCrits = 0;

    @XmlElement
    private int printSize = 70;

    public TestXMLOption() {
    }

    @Override
    public float getWeightCeilingEngine() {
        return weightCeiling.engine;
    }

    @Override
    public float getWeightCeilingStructure() {
        return weightCeiling.structure;
    }

    @Override
    public float getWeightCeilingArmor() {
        return weightCeiling.armor;
    }

    @Override
    public float getWeightCeilingControls() {
        return weightCeiling.controls;
    }

    @Override
    public float getWeightCeilingWeapons() {
        return weightCeiling.weapons;
    }

    @Override
    public float getWeightCeilingTargComp() {
        return weightCeiling.targComp;
    }

    @Override
    public float getWeightCeilingGyro() {
        return weightCeiling.gyro;
    }

    @Override
    public float getWeightCeilingTurret() {
        return weightCeiling.turret;
    }

    @Override
    public float getWeightCeilingLifting() {
        return weightCeiling.lifting;
    }
    
    @Override
    public float getWeightCeilingPowerAmp() {
        return weightCeiling.powerAmp;
    }

    @Override
    public float getMaxOverweight() {
        return maxOverweight;
    }

    @Override
    public boolean showOverweightedEntity() {
        return showOverweighted;
    }

    @Override
    public boolean showUnderweightedEntity() {
        return showUnderweighted;
    }

    @Override
    public float getMinUnderweight() {
        return minUnderweight;
    }

    @Override
    public boolean ignoreFailedEquip(String name) {
        for (int i = 0; i < ignoreFailedEquip.size(); i++) {
            if (ignoreFailedEquip.get(i).equals(name)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean skip() {
        return skip;
    }

    @Override
    public boolean showCorrectArmor() {
        return showCorrectArmor;
    }

    @Override
    public boolean showCorrectCritical() {
        return showCorrectCritical;
    }

    @Override
    public boolean showFailedEquip() {
        return showFailedEquip;
    }

    @Override
    public int getTargCompCrits() {
        return targCompCrits;
    }

    @Override
    public int getPrintSize() {
        return printSize;
    }

    public String printIgnoredFailedEquip() {
        System.out.println("--->printIgnoredFailedEquip");
        String ret = "";
        for (int i = 0; i < ignoreFailedEquip.size(); i++) {
            ret += "  " + ignoreFailedEquip.get(i) + "\n";
        }
        return ret;
    }

    public String printOptions() {
        return "Skip: " + skip() + "\n" + "Show Overweighted Entity: "
                + showOverweightedEntity() + "\n" + "Max Overweight: "
                + Float.toString(getMaxOverweight()) + "\n"
                + "Show Underweighted Entity: " + showUnderweightedEntity()
                + "\n" + "Min Underweight: "
                + Float.toString(getMinUnderweight()) + "\n"
                + "Show bad Armor Placement: " + showCorrectArmor() + "\n"
                + "Show bad Critical Allocation: " + showCorrectCritical()
                + "\n" + "Show Failed to Load Equipment: " + showFailedEquip()
                + "\n" + "Weight Ceiling Engine: "
                + Float.toString(1 / getWeightCeilingEngine()) + "\n"
                + "Weight Ceiling Structure: "
                + Float.toString(1 / getWeightCeilingStructure()) + "\n"
                + "Weight Ceiling Armor: "
                + Float.toString(1 / getWeightCeilingArmor()) + "\n"
                + "Weight Ceiling Controls: "
                + Float.toString(1 / getWeightCeilingControls()) + "\n"
                + "Weight Ceiling Weapons: "
                + Float.toString(1 / getWeightCeilingWeapons()) + "\n"
                + "Weight Ceiling TargComp: "
                + Float.toString(1 / getWeightCeilingTargComp()) + "\n"
                + "Weight Ceiling Gyro: "
                + Float.toString(1 / getWeightCeilingGyro()) + "\n"
                + "Weight Ceiling Turret: "
                + Float.toString(1 / getWeightCeilingTurret()) + "\n"
                + "Weight Ceiling Lifting:"
                + Float.toString(1 / getWeightCeilingLifting()) + "\n"
                + "Weight Ceiling PowerAmp: "
                + Float.toString(1 / getWeightCeilingPowerAmp()) + "\n"
                + "Ignore Failed Equipment: \n" + printIgnoredFailedEquip();
    }
    
    /**
     * JAXB helper class for the ceilWeight tag.
     */
    @XmlType
    private static class WeightCeiling {
        @XmlElement
        @XmlJavaTypeAdapter(FloatInvertAdapter.class)
        Float engine = TestEntity.CEIL_HALFTON;

        @XmlElement
        @XmlJavaTypeAdapter(FloatInvertAdapter.class)
        Float structure = TestEntity.CEIL_HALFTON;

        @XmlElement
        @XmlJavaTypeAdapter(FloatInvertAdapter.class)
        Float armor = TestEntity.CEIL_HALFTON;

        @XmlElement
        @XmlJavaTypeAdapter(FloatInvertAdapter.class)
        Float controls = TestEntity.CEIL_HALFTON;

        @XmlElement
        @XmlJavaTypeAdapter(FloatInvertAdapter.class)
        Float weapons = TestEntity.CEIL_TON;

        @XmlElement(name = "targcomp")
        @XmlJavaTypeAdapter(FloatInvertAdapter.class)
        Float targComp = TestEntity.CEIL_TON;

        @XmlElement
        @XmlJavaTypeAdapter(FloatInvertAdapter.class)
        Float turret = TestEntity.CEIL_HALFTON;

        @XmlElement
        @XmlJavaTypeAdapter(FloatInvertAdapter.class)
        Float lifting = TestEntity.CEIL_HALFTON;

        @XmlElement(name = "poweramp")
        @XmlJavaTypeAdapter(FloatInvertAdapter.class)
        Float powerAmp = TestEntity.CEIL_HALFTON;

        @XmlElement
        @XmlJavaTypeAdapter(FloatInvertAdapter.class)
        Float gyro = TestEntity.CEIL_HALFTON;

        WeightCeiling() {
        }
    }
    
    /**
     * An adapter that unmarshals a float, then returns 1 divided by the value.
     */
    private static class FloatInvertAdapter extends XmlAdapter<Float, Float> {

        @Override
        public Float marshal(final Float v) throws Exception {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public Float unmarshal(final Float v) throws Exception {
            return 1 / v;
        }
        
    }
    
    /**
     * An adapter that unmarshals a comma-separated string of values into a list of values.
     */
    private static class CSVAdapter extends XmlAdapter<String, List<String>> {

        @Override
        public String marshal(final List<String> v) throws Exception {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public List<String> unmarshal(final String v) throws Exception {
            List<String> list = new ArrayList<>();
            StringTokenizer st = new StringTokenizer(v, ",");
            
            while (st.hasMoreTokens()) {
                list.add(st.nextToken().trim());
            }
            
            return list;
        }
        
    }
}
