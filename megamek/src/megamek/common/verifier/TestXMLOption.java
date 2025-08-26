/*
 * Copyright (C) 2000-2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2005-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */

package megamek.common.verifier;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
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

    @XmlElement(name = "showIncorrectIntroYear")
    private boolean showIncorrectIntroYear = true;

    @XmlElement(name = "introYearMargin")
    private int introYearMargin = 5;

    @XmlElement
    private int targCompCrits = 0;

    @XmlElement
    private int printSize = 70;

    public TestXMLOption() {
    }

    @Override
    public Ceil getWeightCeilingEngine() {
        return weightCeiling.engine;
    }

    @Override
    public Ceil getWeightCeilingStructure() {
        return weightCeiling.structure;
    }

    @Override
    public Ceil getWeightCeilingArmor() {
        return weightCeiling.armor;
    }

    @Override
    public Ceil getWeightCeilingControls() {
        return weightCeiling.controls;
    }

    @Override
    public Ceil getWeightCeilingWeapons() {
        return weightCeiling.weapons;
    }

    @Override
    public Ceil getWeightCeilingTargComp() {
        return weightCeiling.targComp;
    }

    @Override
    public Ceil getWeightCeilingGyro() {
        return weightCeiling.gyro;
    }

    @Override
    public Ceil getWeightCeilingTurret() {
        return weightCeiling.turret;
    }

    @Override
    public Ceil getWeightCeilingLifting() {
        return weightCeiling.lifting;
    }

    @Override
    public Ceil getWeightCeilingPowerAmp() {
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
    public boolean showUnderweightEntity() {
        return showUnderweighted;
    }

    @Override
    public double getMinUnderweight() {
        return minUnderweight;
    }

    @Override
    public boolean ignoreFailedEquip(String name) {
        for (String s : ignoreFailedEquip) {
            if (s.equals(name)) {
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
    public boolean showIncorrectIntroYear() {
        return showIncorrectIntroYear;
    }

    @Override
    public int getIntroYearMargin() {
        return introYearMargin;
    }

    @Override
    public int getTargetingComputerCrits() {
        return targCompCrits;
    }

    @Override
    public int getPrintSize() {
        return printSize;
    }

    public String printIgnoredFailedEquip() {
        System.out.println("--->printIgnoredFailedEquip");
        StringBuilder sb = new StringBuilder();
        for (String s : ignoreFailedEquip) {
            sb.append("  ").append(s).append("\n");
        }
        return sb.toString();
    }

    public String printOptions() {
        return "Skip: " + skip() + "\n" + "Show Overweighted Entity: "
              + showOverweightedEntity() + "\n" + "Max Overweight: "
              + getMaxOverweight() + "\n"
              + "Show Underweighted Entity: " + showUnderweightEntity()
              + "\n" + "Min Underweight: "
              + getMinUnderweight() + "\n"
              + "Show bad Armor Placement: " + showCorrectArmor() + "\n"
              + "Show bad Critical Allocation: " + showCorrectCritical()
              + "\n" + "Show Failed to Load Equipment: " + showFailedEquip()
              + "\n" + "Show Incorrect Intro Year: " + showIncorrectIntroYear()
              + "\n" + "Margin of error for Intro Year: " + getIntroYearMargin()
              + "\n" + "Weight Ceiling Engine: "
              + 1 / getWeightCeilingEngine().multiplier + "\n"
              + "Weight Ceiling Structure: "
              + 1 / getWeightCeilingStructure().multiplier + "\n"
              + "Weight Ceiling Armor: "
              + 1 / getWeightCeilingArmor().multiplier + "\n"
              + "Weight Ceiling Controls: "
              + 1 / getWeightCeilingControls().multiplier + "\n"
              + "Weight Ceiling Weapons: "
              + 1 / getWeightCeilingWeapons().multiplier + "\n"
              + "Weight Ceiling TargComp: "
              + 1 / getWeightCeilingTargComp().multiplier + "\n"
              + "Weight Ceiling Gyro: "
              + 1 / getWeightCeilingGyro().multiplier + "\n"
              + "Weight Ceiling Turret: "
              + 1 / getWeightCeilingTurret().multiplier + "\n"
              + "Weight Ceiling Lifting:"
              + 1 / getWeightCeilingLifting().multiplier + "\n"
              + "Weight Ceiling PowerAmp: "
              + 1 / getWeightCeilingPowerAmp().multiplier + "\n"
              + "Ignore Failed Equipment: \n" + printIgnoredFailedEquip();
    }

    /**
     * JAXB helper class for the ceilWeight tag.
     */
    @XmlType
    private static class WeightCeiling {
        @XmlElement
        Ceil engine = Ceil.HALF_TON;

        @XmlElement
        Ceil structure = Ceil.HALF_TON;

        @XmlElement
        Ceil armor = Ceil.HALF_TON;

        @XmlElement
        Ceil controls = Ceil.HALF_TON;

        @XmlElement
        Ceil weapons = Ceil.TON;

        @XmlElement(name = "targcomp")
        Ceil targComp = Ceil.TON;

        @XmlElement
        Ceil turret = Ceil.HALF_TON;

        @XmlElement
        Ceil lifting = Ceil.HALF_TON;

        @XmlElement(name = "poweramp")
        Ceil powerAmp = Ceil.HALF_TON;

        @XmlElement
        Ceil gyro = Ceil.HALF_TON;

        WeightCeiling() {
        }
    }

    /**
     * An adapter that unmarshal a comma-separated string of values into a list of values.
     */
    private static class CSVAdapter extends XmlAdapter<String, List<String>> {

        @Override
        public String marshal(final List<String> v) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public List<String> unmarshal(final String v) {
            List<String> list = new ArrayList<>();
            StringTokenizer st = new StringTokenizer(v, ",");

            while (st.hasMoreTokens()) {
                list.add(st.nextToken().trim());
            }

            return list;
        }

    }
}
