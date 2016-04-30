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
    private double maxOverweight = 0.25;

    @XmlElement
    private boolean showOverweighted = true;

    @XmlElement
    private double minUnderweight = 1.0;

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
    public TestEntity.Ceil getWeightCeilingEngine() {
        return weightCeiling.engine;
    }

    @Override
    public TestEntity.Ceil getWeightCeilingStructure() {
        return weightCeiling.structure;
    }

    @Override
    public TestEntity.Ceil getWeightCeilingArmor() {
        return weightCeiling.armor;
    }

    @Override
    public TestEntity.Ceil getWeightCeilingControls() {
        return weightCeiling.controls;
    }

    @Override
    public TestEntity.Ceil getWeightCeilingWeapons() {
        return weightCeiling.weapons;
    }

    @Override
    public TestEntity.Ceil getWeightCeilingTargComp() {
        return weightCeiling.targComp;
    }

    @Override
    public TestEntity.Ceil getWeightCeilingGyro() {
        return weightCeiling.gyro;
    }

    @Override
    public TestEntity.Ceil getWeightCeilingTurret() {
        return weightCeiling.turret;
    }

    @Override
    public TestEntity.Ceil getWeightCeilingLifting() {
        return weightCeiling.lifting;
    }
    
    @Override
    public TestEntity.Ceil getWeightCeilingPowerAmp() {
        return weightCeiling.powerAmp;
    }

    @Override
    public double getMaxOverweight() {
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
    public double getMinUnderweight() {
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
                + Double.toString(getMaxOverweight()) + "\n"
                + "Show Underweighted Entity: " + showUnderweightedEntity()
                + "\n" + "Min Underweight: "
                + Double.toString(getMinUnderweight()) + "\n"
                + "Show bad Armor Placement: " + showCorrectArmor() + "\n"
                + "Show bad Critical Allocation: " + showCorrectCritical()
                + "\n" + "Show Failed to Load Equipment: " + showFailedEquip()
                + "\n" + "Weight Ceiling Engine: "
                + Double.toString(1 / getWeightCeilingEngine().mult) + "\n"
                + "Weight Ceiling Structure: "
                + Double.toString(1 / getWeightCeilingStructure().mult) + "\n"
                + "Weight Ceiling Armor: "
                + Double.toString(1 / getWeightCeilingArmor().mult) + "\n"
                + "Weight Ceiling Controls: "
                + Double.toString(1 / getWeightCeilingControls().mult) + "\n"
                + "Weight Ceiling Weapons: "
                + Double.toString(1 / getWeightCeilingWeapons().mult) + "\n"
                + "Weight Ceiling TargComp: "
                + Double.toString(1 / getWeightCeilingTargComp().mult) + "\n"
                + "Weight Ceiling Gyro: "
                + Double.toString(1 / getWeightCeilingGyro().mult) + "\n"
                + "Weight Ceiling Turret: "
                + Double.toString(1 / getWeightCeilingTurret().mult) + "\n"
                + "Weight Ceiling Lifting:"
                + Double.toString(1 / getWeightCeilingLifting().mult) + "\n"
                + "Weight Ceiling PowerAmp: "
                + Double.toString(1 / getWeightCeilingPowerAmp().mult) + "\n"
                + "Ignore Failed Equipment: \n" + printIgnoredFailedEquip();
    }
    
    /**
     * JAXB helper class for the ceilWeight tag.
     */
    @XmlType
    private static class WeightCeiling {
        @XmlElement
        TestEntity.Ceil engine = TestEntity.Ceil.HALFTON;

        @XmlElement
        TestEntity.Ceil structure = TestEntity.Ceil.HALFTON;

        @XmlElement
        TestEntity.Ceil armor = TestEntity.Ceil.HALFTON;

        @XmlElement
        TestEntity.Ceil controls = TestEntity.Ceil.HALFTON;

        @XmlElement
        TestEntity.Ceil weapons = TestEntity.Ceil.TON;

        @XmlElement(name = "targcomp")
        TestEntity.Ceil targComp = TestEntity.Ceil.TON;

        @XmlElement
        TestEntity.Ceil turret = TestEntity.Ceil.HALFTON;

        @XmlElement
        TestEntity.Ceil lifting = TestEntity.Ceil.HALFTON;

        @XmlElement(name = "poweramp")
        TestEntity.Ceil powerAmp = TestEntity.Ceil.HALFTON;

        @XmlElement
        TestEntity.Ceil gyro = TestEntity.Ceil.HALFTON;

        WeightCeiling() {
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
