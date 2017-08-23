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

import java.util.Iterator;

import megamek.common.Aero;
import megamek.common.AmmoType;
import megamek.common.Bay;
import megamek.common.BombType;
import megamek.common.CriticalSlot;
import megamek.common.Engine;
import megamek.common.Entity;
import megamek.common.EntityMovementMode;
import megamek.common.EquipmentType;
import megamek.common.ITechnology;
import megamek.common.Mech;
import megamek.common.MiscType;
import megamek.common.Mounted;
import megamek.common.Protomech;
import megamek.common.SimpleTechLevel;
import megamek.common.TechConstants;
import megamek.common.Transporter;
import megamek.common.WeaponType;
import megamek.common.util.StringUtil;

/**
 * Abstract parent class for testing and validating instantiations of <code>
 * Entity</code> subclasses.
 *
 */
public abstract class TestEntity implements TestEntityOption {
    public static enum Ceil {
        TON(1.0), HALFTON(2.0), QUARTERTON(4.0), TENTHTON(10.0), KILO(1000.0);
        
        public final double mult;
        
        private Ceil(double mult) {
            this.mult = mult;
        }
    }
    
    protected Engine engine = null;
    protected Armor[] armor = null;
    protected Structure structure = null;
    private TestEntityOption options = null;

    public abstract Entity getEntity();

    public abstract boolean isTank();

    public abstract boolean isMech();

    public abstract boolean isAero();

    public abstract double getWeightControls();

    public abstract double getWeightMisc();

    public abstract double getWeightHeatSinks();

    public abstract boolean hasDoubleHeatSinks();

    public abstract int getCountHeatSinks();

    public abstract String printWeightMisc();

    public abstract String printWeightControls();

    public abstract boolean correctEntity(StringBuffer buff);

    public abstract boolean correctEntity(StringBuffer buff, int ammoTechLvl);

    public abstract StringBuffer printEntity();

    public abstract String getName();

    public String fileString = null; // where the unit came from

    public TestEntity(TestEntityOption options, Engine engine, Armor[] armor,
            Structure structure) {
        this.options = options;
        this.engine = engine;
        this.armor = armor;
        this.structure = structure;
    }

    public boolean isClan() {
        return getEntity().isClan();
    }

    public boolean isClanArmor() {
        return getEntity().isClanArmor(0) && !getEntity().hasPatchworkArmor();
    }

    public double getWeight() {
        return getEntity().getWeight();
    }

    public int getTotalOArmor() {
        return getEntity().getTotalOArmor();
    }

    public String getLocationAbbr(int location) {
        return getEntity().getLocationAbbr(location);
    }

    @Override
    public Ceil getWeightCeilingEngine() {
        return options.getWeightCeilingEngine();
    }

    @Override
    public Ceil getWeightCeilingStructure() {
        return options.getWeightCeilingStructure();
    }

    @Override
    public Ceil getWeightCeilingArmor() {
        return options.getWeightCeilingArmor();
    }

    @Override
    public Ceil getWeightCeilingControls() {
        return options.getWeightCeilingControls();
    }

    @Override
    public Ceil getWeightCeilingWeapons() {
        return options.getWeightCeilingWeapons();
    }

    @Override
    public Ceil getWeightCeilingTargComp() {
        return options.getWeightCeilingTargComp();
    }

    @Override
    public Ceil getWeightCeilingGyro() {
        return options.getWeightCeilingGyro();
    }

    @Override
    public Ceil getWeightCeilingTurret() {
        return options.getWeightCeilingTurret();
    }

    @Override
    public Ceil getWeightCeilingLifting() {
        return options.getWeightCeilingLifting();
    }

    @Override
    public Ceil getWeightCeilingPowerAmp() {
        return options.getWeightCeilingPowerAmp();
    }

    @Override
    public double getMaxOverweight() {
        return options.getMaxOverweight();
    }

    @Override
    public boolean showOverweightedEntity() {
        return options.showOverweightedEntity();
    }

    @Override
    public double getMinUnderweight() {
        return options.getMinUnderweight();
    }

    @Override
    public boolean showUnderweightedEntity() {
        return options.showUnderweightedEntity();
    }

    public boolean showCorrectArmor() {
        return options.showCorrectArmor();
    }

    public boolean showCorrectCritical() {
        return options.showCorrectCritical();
    }

    public boolean showFailedEquip() {
        return options.showFailedEquip();
    }

    public boolean ignoreFailedEquip(String name) {
        return options.ignoreFailedEquip(name);
    }

    public boolean skip() {
        return options.skip();
    }

    public int getTargCompCrits() {
        return options.getTargCompCrits();
    }

    public int getPrintSize() {
        return options.getPrintSize();
    }

    /**
     * Used to round values up based on the specified type.
     *
     * @param f     Value to round
     * @param type  Specifies the number of decimals to round to, see
     *              TestEntity.CEIL_TON, etc.
     * @return      Rounded value
     */
    public static double ceil(double f, Ceil type) {
        return Math.ceil(f * type.mult) / type.mult;
    }

    public static double ceilMaxHalf(double f, Ceil type) {
        if (type == Ceil.TON) {
            return TestEntity.ceil(f, Ceil.HALFTON);
        }
        return TestEntity.ceil(f, type);
    }

    public static double floor(double f, Ceil type) {
        return Math.floor(f * type.mult) / type.mult;
    }

    public static double round(double f, Ceil type) {
        return Math.round(f * type.mult) / type.mult;
    }

    protected static String makeWeightString(double weight) {
        return (weight < 100 ? " " : "") + (weight < 10 ? " " : "")
                + Double.toString(weight)
                + ((Math.ceil(weight * 10) == (weight * 10)) ? "0" : "");
    }

    /**
     * Allows a value to be truncuated to an arbitrary number of decimal places.
     *
     * @param value
     *            The input value
     * @param precision
     *            The number of decimals to truncate at
     *
     * @return The input value truncated to the number of decimal places
     *         supplied
     */
    public static double setPrecision(double value, int precision) {
        return Math.round(value * Math.pow(10, precision))
                / Math.pow(10, precision);
    }

    private boolean hasMASC() {
        if (getEntity() instanceof Mech) {
            return ((Mech) getEntity()).hasMASC();
        }
        return false;
    }

    public String printShortMovement() {
        return "Movement: "
                + Integer.toString(getEntity().getOriginalWalkMP())
                + "/"
                + Integer.toString((int) Math.ceil(getEntity()
                        .getOriginalWalkMP() * 1.5))
                + (hasMASC() ? "("
                        + Integer.toString(getEntity().getOriginalWalkMP() * 2)
                        + ")" : "")
                + (getEntity().getOriginalJumpMP() != 0 ? "/"
                        + Integer.toString(getEntity().getOriginalJumpMP())
                        : "") + "\n";
    }

    public String printWeightHeatSinks() {
        return StringUtil.makeLength(
                "Heat Sinks: "
                        + Integer.toString(getCountHeatSinks())
                        + (hasDoubleHeatSinks() ? " ["
                                + Integer.toString(2 * getCountHeatSinks())
                                + "]" : ""), getPrintSize() - 5)
                + TestEntity.makeWeightString(getWeightHeatSinks()) + "\n";
    }

    public String printWeightEngine() {
        return StringUtil.makeLength("Engine: " + ((null != engine) ? engine.getEngineName() : "---"),
                getPrintSize() - 5)
                + TestEntity.makeWeightString(getWeightEngine()) + "\n";
    }

    public double getWeightEngine() {
        double weight = ((null != engine) ? engine.getWeightEngine(getEntity(), getWeightCeilingEngine()) : 0);
        return weight;
    }

    public String printWeightStructure() {
        return StringUtil.makeLength(
                "Structure: "
                        + Integer.toString(getEntity().getTotalOInternal())
                        + " " + structure.getShortName(), getPrintSize() - 5)
                + TestEntity.makeWeightString(getWeightStructure()) + "\n";
    }

    public double getWeightStructure() {
        return structure.getWeightStructure(getWeight(),
                getWeightCeilingStructure());
    }

    public String printWeightArmor() {
        if (!getEntity().hasPatchworkArmor()) {
            return StringUtil.makeLength(
                    "Armor: " + Integer.toString(getTotalOArmor()) + " "
                            + armor[0].getShortName(), getPrintSize() - 5)
                    + TestEntity.makeWeightString(getWeightArmor()) + "\n";
        } else {
            return StringUtil.makeLength(
                    "Armor: " + Integer.toString(getTotalOArmor()) + " "
                            + "Patchwork", getPrintSize() - 5)
                    + TestEntity.makeWeightString(getWeightArmor()) + "\n";
        }

    }

    public double getWeightArmor() {
        return getEntity().getLabArmorTonnage();
    }

    public double getWeightAllocatedArmor() {
        double armorWeight = 0;
        if (!getEntity().hasPatchworkArmor()) {
            armorWeight += armor[0].getWeightArmor(getTotalOArmor(),
                    getWeightCeilingArmor());
        } else {
            for (int i = 0; i < armor.length; i++) {
                int points = getEntity().getOArmor(i);
                if (getEntity().hasRearArmor(i) &&
                        (getEntity().getOArmor(i, true) > 0)) {
                    points += getEntity().getOArmor(i, true);
                }
                armorWeight += armor[i].getWeightArmor(points,
                        getWeightCeilingArmor());
            }
        }
        return armorWeight;
    }

    public double getWeightMiscEquip() {
        double weightSum = 0.0;
        for (Mounted m : getEntity().getMisc()) {
            MiscType mt = (MiscType) m.getType();
            if (mt.hasFlag(MiscType.F_ENDO_STEEL)
                    || mt.hasFlag(MiscType.F_ENDO_COMPOSITE)
                    || mt.hasFlag(MiscType.F_ENDO_STEEL_PROTO)
                    || mt.hasFlag(MiscType.F_ENDO_COMPOSITE)
                    || mt.hasFlag(MiscType.F_COMPOSITE)
                    || mt.hasFlag(MiscType.F_INDUSTRIAL_STRUCTURE)
                    || mt.hasFlag(MiscType.F_REINFORCED)
                    || mt.hasFlag(MiscType.F_FERRO_FIBROUS)
                    || mt.hasFlag(MiscType.F_FERRO_FIBROUS_PROTO)
                    || mt.hasFlag(MiscType.F_FERRO_LAMELLOR)
                    || mt.hasFlag(MiscType.F_LIGHT_FERRO)
                    || mt.hasFlag(MiscType.F_HEAVY_FERRO)
                    || mt.hasFlag(MiscType.F_REACTIVE)
                    || mt.hasFlag(MiscType.F_REFLECTIVE)
                    || mt.hasFlag(MiscType.F_HARDENED_ARMOR)
                    || mt.hasFlag(MiscType.F_PRIMITIVE_ARMOR)
                    || mt.hasFlag(MiscType.F_COMMERCIAL_ARMOR)
                    || mt.hasFlag(MiscType.F_INDUSTRIAL_ARMOR)
                    || mt.hasFlag(MiscType.F_HEAVY_INDUSTRIAL_ARMOR)
                    || mt.hasFlag(MiscType.F_ANTI_PENETRATIVE_ABLATIVE)
                    || mt.hasFlag(MiscType.F_HEAT_DISSIPATING)
                    || mt.hasFlag(MiscType.F_IMPACT_RESISTANT)
                    || mt.hasFlag(MiscType.F_BALLISTIC_REINFORCED)
                    || mt.hasFlag(MiscType.F_HEAT_SINK)
                    || mt.hasFlag(MiscType.F_DOUBLE_HEAT_SINK)
                    || mt.hasFlag(MiscType.F_IS_DOUBLE_HEAT_SINK_PROTOTYPE)) {
                continue;
            }
            weightSum += mt.getTonnage(getEntity(), m.getLocation());
        }
        return weightSum;
    }

    public StringBuffer printMiscEquip() {
        return printMiscEquip(new StringBuffer());
    }

    public StringBuffer printMiscEquip(StringBuffer buff) {
        return printMiscEquip(buff, 20, getPrintSize());
    }

    public StringBuffer printMiscEquip(StringBuffer buff, int posLoc,
            int posWeight) {
        for (Mounted m : getEntity().getMisc()) {
            MiscType mt = (MiscType) m.getType();

            if (m.getLocation() == Entity.LOC_NONE) {
                continue;
            }
            if (mt.hasFlag(MiscType.F_ENDO_COMPOSITE)
                    || mt.hasFlag(MiscType.F_ENDO_STEEL)
                    || mt.hasFlag(MiscType.F_ENDO_STEEL_PROTO)
                    || mt.hasFlag(MiscType.F_REINFORCED)
                    || mt.hasFlag(MiscType.F_FERRO_FIBROUS)
                    || mt.hasFlag(MiscType.F_FERRO_FIBROUS_PROTO)
                    || mt.hasFlag(MiscType.F_LIGHT_FERRO)
                    || mt.hasFlag(MiscType.F_HEAVY_FERRO)
                    || mt.hasFlag(MiscType.F_REACTIVE)
                    || mt.hasFlag(MiscType.F_REFLECTIVE)
                    || mt.hasFlag(MiscType.F_FERRO_LAMELLOR)
                    || mt.hasFlag(MiscType.F_INDUSTRIAL_STRUCTURE)) {
                continue;
            }

            if (mt.getTonnage(getEntity(), m.getLocation()) == 0f) {
                continue;
            }

            buff.append(StringUtil.makeLength(mt.getName(), 20));
            buff.append(
                    StringUtil.makeLength(getLocationAbbr(m.getLocation()),
                            getPrintSize() - 5 - 20)).append(
                    TestEntity.makeWeightString(mt.getTonnage(getEntity())));
            buff.append("\n");
        }
        return buff;
    }

    public double getWeightWeapon() {
        double weight = 0.0;
        for (Mounted m : getEntity().getWeaponList()) {
            WeaponType wt = (WeaponType) m.getType();
            if (m.isDWPMounted()){
                weight += wt.getTonnage(getEntity()) * 0.75;
            } else {
                weight += wt.getTonnage(getEntity());
            }
        }
        return weight;
    }

    public StringBuffer printWeapon() {
        return printWeapon(new StringBuffer());
    }

    public StringBuffer printWeapon(StringBuffer buff) {
        return printWeapon(buff, 20, getPrintSize());
    }

    public StringBuffer printWeapon(StringBuffer buff, int posLoc, int posWeight) {
        for (Mounted m : getEntity().getWeaponList()) {
            WeaponType mt = (WeaponType) m.getType();

            // Don't think this can happen, but ...
            if (m.getLocation() == Entity.LOC_NONE) {
                continue;
            }

            buff.append(StringUtil.makeLength(mt.getName(), 20));
            buff.append(
                    StringUtil.makeLength(getLocationAbbr(m.getLocation()),
                            getPrintSize() - 5 - 20))
                    .append(TestEntity.makeWeightString(mt
                            .getTonnage(getEntity()))).append("\n");
        }
        return buff;
    }

    public double getWeightAmmo() {
        double weight = 0.0;
        for (Mounted m : getEntity().getAmmo()) {

            // One Shot Ammo
            if (m.getLocation() == Entity.LOC_NONE) {
                continue;
            }

            // Bombs on ASF don't count!
            if ((getEntity() instanceof Aero) && (m.getType() instanceof BombType)) {
                continue;
            }

            AmmoType mt = (AmmoType) m.getType();
            weight += mt.getTonnage(getEntity());
        }
        return weight;
    }

    public abstract double getWeightPowerAmp();

    public StringBuffer printAmmo() {
        return printAmmo(new StringBuffer());
    }

    public StringBuffer printAmmo(StringBuffer buff) {
        return printAmmo(buff, 20, getPrintSize());
    }

    public StringBuffer printAmmo(StringBuffer buff, int posLoc, int posWeight) {
        for (Mounted m : getEntity().getAmmo()) {
            AmmoType mt = (AmmoType) m.getType();

            // Don't think this can happen, but ...
            if (m.getLocation() == Entity.LOC_NONE) {
                continue;
            }

            buff.append(StringUtil.makeLength(mt.getName(), 20));
            buff.append(
                    StringUtil.makeLength(getLocationAbbr(m.getLocation()),
                            getPrintSize() - 5 - 20))
                    .append(TestEntity.makeWeightString(mt
                            .getTonnage(getEntity()))).append("\n");
        }
        return buff;
    }

    public String printLocations() {
        StringBuffer buff = new StringBuffer();
        for (int i = 0; i < getEntity().locations(); i++) {
            String locationName = getEntity().getLocationName(i);
            buff.append(locationName + ":");
            buff.append("\n");
            for (int j = 0; j < getEntity().getNumberOfCriticals(i); j++) {
                CriticalSlot slot = getEntity().getCritical(i, j);
                if (slot == null) {
                    buff.append(Integer.toString(j) + ". -Emtpy-");
                    buff.append("\n");
                } else if (slot.getType() == CriticalSlot.TYPE_SYSTEM) {
                    if (isMech()) {
                        buff.append(Integer.toString(j));
                        buff.append(". ");
                        buff.append(((Mech) getEntity()).getSystemName(slot
                                .getIndex()));
                        buff.append("\n");
                    } else {
                        buff.append(Integer.toString(j)
                                + ". UNKNOWN SYSTEM NAME");
                        buff.append("\n");
                    }
                } else if (slot.getType() == CriticalSlot.TYPE_EQUIPMENT) {
                    EquipmentType e = getEntity().getEquipmentType(slot);
                    buff.append(Integer.toString(j) + ". "
                            + e.getInternalName());
                    buff.append("\n");
                }
            }
        }
        return buff.toString();
    }

    public int calcMiscCrits(MiscType mt) {
        if (mt.hasFlag(MiscType.F_CLUB)
                && (mt.hasSubType(MiscType.S_HATCHET)
                        || mt.hasSubType(MiscType.S_SWORD)
                        || mt.hasSubType(MiscType.S_CHAIN_WHIP) || mt
                            .hasSubType(MiscType.S_MACE_THB))) {
            return (int) Math.ceil(getWeight() / 15.0);
        } else if (mt.hasFlag(MiscType.F_CLUB)
                && mt.hasSubType(MiscType.S_MACE)) {
            return (int) Math.ceil(getWeight() / 10.0);
        } else if (mt.hasFlag(MiscType.F_CLUB)
                && mt.hasSubType(MiscType.S_RETRACTABLE_BLADE)) {
            return 1 + (int) Math.ceil(getWeight() / 20.0);
        } else if (mt.hasFlag(MiscType.F_CLUB)
                && mt.hasSubType(MiscType.S_PILE_DRIVER)) {
            return 8;
        } else if (mt.hasFlag(MiscType.F_CLUB)
                && mt.hasSubType(MiscType.S_CHAINSAW)) {
            return 5;
        } else if (mt.hasFlag(MiscType.F_CLUB)
                && mt.hasSubType(MiscType.S_DUAL_SAW)) {
            return 7;
        } else if (mt.hasFlag(MiscType.F_CLUB)
                && mt.hasSubType(MiscType.S_BACKHOE)) {
            return 6;
        } else if (mt.hasFlag(MiscType.F_MASC)) {
            if (mt.getInternalName().equals("ISMASC")) {
                return (int) Math.round(getWeight() / 20.0);
            } else if (mt.getInternalName().equals("CLMASC")) {
                return (int) Math.round(getWeight() / 25.0);
            }
        } else if (mt.hasFlag(MiscType.F_TARGCOMP)) {
            double fTons = 0.0f;
            for (Mounted mo : getEntity().getWeaponList()) {
                WeaponType wt = (WeaponType) mo.getType();
                if (wt.hasFlag(WeaponType.F_DIRECT_FIRE)) {
                    fTons += wt.getTonnage(getEntity());
                }
            }
            for (Mounted mo : getEntity().getMisc()) {
                MiscType mt2 = (MiscType) mo.getType();
                if (mt2.hasFlag(MiscType.F_RISC_LASER_PULSE_MODULE)) {
                    fTons += mt.getTonnage(getEntity());
                }
            }
            double weight = 0.0f;
            if (mt.getInternalName().equals("ISTargeting Computer")) {
                weight = TestEntity.ceil(fTons / 4.0f,
                        getWeightCeilingTargComp());
            } else if (mt.getInternalName().equals("CLTargeting Computer")) {
                weight = TestEntity.ceil(fTons / 5.0f,
                        getWeightCeilingTargComp());
            }
            switch (getTargCompCrits()) {
                case CEIL_TARGCOMP_CRITS:
                    return (int) Math.ceil(weight);
                case ROUND_TARGCOMP_CRITS:
                    return (int) Math.round(weight);
                case FLOOR_TARGCOMP_CRITS:
                    return (int) Math.floor(weight);
            }
        } else if (EquipmentType.getArmorTypeName(
                EquipmentType.T_ARMOR_FERRO_FIBROUS).equals(
                mt.getInternalName())) {
            if (isClanArmor()) {
                return 7;
            }
            return 14;
        } else if (EquipmentType.getArmorTypeName(
                EquipmentType.T_ARMOR_FERRO_FIBROUS_PROTO).equals(
                mt.getInternalName())) {
            return 16;
        } else if (EquipmentType.getArmorTypeName(
                EquipmentType.T_ARMOR_LIGHT_FERRO).equals(mt.getInternalName())) {
            return 7;
        } else if (EquipmentType.getArmorTypeName(
                EquipmentType.T_ARMOR_HEAVY_FERRO).equals(mt.getInternalName())) {
            return 21;
        } else if (mt.hasFlag(MiscType.F_ENDO_STEEL)) {
            if (isClan()
                    || mt.getInternalName()
                            .equals("Clan "
                                    + EquipmentType
                                            .getStructureTypeName(EquipmentType.T_STRUCTURE_ENDO_STEEL))) {
                return 7;
            }
            return 14;
        } else if (mt.hasFlag(MiscType.F_ENDO_STEEL_PROTO)) {
            return 16;
        } else if (mt.hasFlag(MiscType.F_ENDO_COMPOSITE)) {
            if (isClan()
                    || mt.getInternalName()
                            .equals("Clan "
                                    + EquipmentType
                                            .getStructureTypeName(EquipmentType.T_STRUCTURE_ENDO_COMPOSITE))) {
                return 4;
            }
            return 7;
        } else if (mt.hasFlag(MiscType.F_REACTIVE)) {
            if (isClanArmor()) {
                return 7;
            }
            return 14;
        } else if (mt.hasFlag(MiscType.F_REFLECTIVE)) {
            if (isClanArmor()) {
                return 5;
            }
            return 10;
        }
        return mt.getCriticals(getEntity());
    }

    public double calculateWeight() {
        double weight = 0;
        weight += getWeightEngine();
        weight += getWeightStructure();
        weight += getWeightControls();
        weight += getWeightHeatSinks();
        weight += getWeightArmor();
        weight += getWeightMisc();

        weight += getWeightMiscEquip();
        weight += getWeightWeapon();
        weight += getWeightAmmo();
        weight += getWeightPowerAmp();

        weight += getWeightCarryingSpace();

        weight += getArmoredComponentWeight();
        return weight;
    }

    public String printWeightCalculation() {
        return printWeightEngine() + printWeightStructure()
                + printWeightControls() + printWeightHeatSinks()
                + printWeightArmor() + printWeightMisc()
                + printWeightCarryingSpace() + "Equipment:\n"
                + printMiscEquip() + printWeapon() + printAmmo();
    }

    public boolean correctWeight(StringBuffer buff) {
        return correctWeight(buff, showOverweightedEntity(),
                showUnderweightedEntity());
    }

    public boolean correctWeight(StringBuffer buff, boolean showO, boolean showU) {
        double weightSum = calculateWeight();
        double weight = getWeight();

        if (showO && ((weight + getMaxOverweight()) < weightSum)) {
            buff.append("Weight: ").append(calculateWeight())
                    .append(" is greater than ").append(getWeight())
                    .append("\n");
            // buff.append(printWeightCalculation()).append("\n");
            return false;
        }
        if (showU && ((weight - getMinUnderweight()) > weightSum)) {
            buff.append("Weight: ").append(calculateWeight())
                    .append(" is less than ").append(getWeight()).append("\n");
            // buff.append(printWeightCalculation()).append("\n");
            return false;
        }
        return true;
    }

    public boolean hasIllegalTechLevels(StringBuffer buff) {
        return hasIllegalTechLevels(buff, getEntity().getTechLevel());
    }

    public boolean hasIllegalTechLevels(StringBuffer buff, int ammoTechLvl) {
        /* A large number of units have official tech levels lower than their components at the
         * intro date. We test instead whether the stated tech level is ever possible based on the
         * equipment. We also test for mixed IS/Clan tech in units that are not designated as mixed.
         */
        boolean retVal = false;
        int eTechLevel = SimpleTechLevel.convertCompoundToSimple(getEntity().getTechLevel()).ordinal();
        int eRulesLevel = getEntity().findMinimumRulesLevel().ordinal();
        if (eTechLevel >= eRulesLevel) {
            return false;
        }
        
        int eTLYear = getEntity().getTechLevelYear();
        for (Mounted mounted : getEntity().getEquipment()) {
            EquipmentType nextE = mounted.getType();
            int eqRulesLevel = getEntity().isMixedTech()?
                    nextE.findMinimumRulesLevel().ordinal() : nextE.findMinimumRulesLevel(getEntity().isClan()).ordinal();
            boolean illegal = eqRulesLevel > eRulesLevel;
            if (!getEntity().isMixedTech()) {
                illegal |= getEntity().isClan() && nextE.getTechBase() == ITechnology.TECH_BASE_IS;
                illegal |= !getEntity().isClan() && nextE.getTechBase() == ITechnology.TECH_BASE_CLAN;
            }
            int eqTechLevel = TechConstants.convertFromSimplelevel(eqRulesLevel, nextE.isClan());
            if (nextE instanceof AmmoType) {
                if (eqRulesLevel > eRulesLevel) {
                    if (!retVal) {
                        buff.append("Ammo illegal at unit's tech level (");
                        buff.append(TechConstants
                                .getLevelDisplayableName(ammoTechLvl));
                        buff.append(", ");
                        buff.append(eTLYear);
                        buff.append("):\n");
                    }
                    retVal = true;
                    buff.append(nextE.getName());
                    buff.append(", (");
                    buff.append(TechConstants
                            .getLevelDisplayableName(eqTechLevel));
                    buff.append(")\n");
                }
            } else if (illegal) {
                if (!retVal) {
                    buff.append("Equipment illegal at unit's tech level ");
                    buff.append(TechConstants
                            .getLevelDisplayableName(ammoTechLvl));
                    buff.append(", ");
                    buff.append(eTLYear);
                    buff.append("):\n");
                }
                retVal = true;
                buff.append(nextE.getName());
                buff.append(", (");
                buff.append(TechConstants
                        .getLevelDisplayableName(eqTechLevel));
                buff.append(")\n");
            }
        }
        // Check cockpit TL
        ITechnology cockpit = null;
        String cockpitName = null;
        if (getEntity() instanceof Aero) {
            cockpit = ((Aero)getEntity()).getCockpitTechAdvancement();
            cockpitName = ((Aero)getEntity()).getCockpitTypeString();
        } else if (getEntity() instanceof Mech) {
            cockpit = ((Mech)getEntity()).getCockpitTechAdvancement();
            cockpitName = ((Mech)getEntity()).getCockpitTypeString();
        }
        if (cockpit != null) {
            int eqRulesLevel = getEntity().isMixedTech()?
                    cockpit.findMinimumRulesLevel().ordinal() : cockpit.findMinimumRulesLevel(getEntity().isClan()).ordinal();
            boolean illegal = eqRulesLevel > eRulesLevel;
            if (!getEntity().isMixedTech()) {
                illegal |= getEntity().isClan() && cockpit.getTechBase() == ITechnology.TECH_BASE_IS;
                illegal |= !getEntity().isClan() && cockpit.getTechBase() == ITechnology.TECH_BASE_CLAN;                
            }
            if (illegal) {
                buff.append("Cockpit is illegal at unit's tech level (");
                buff.append(TechConstants
                        .getLevelDisplayableName(eTechLevel));
                buff.append(", ");
                buff.append(eTLYear);
                buff.append("): ");
                buff.append(cockpitName);
                buff.append(" (");
                buff.append(TechConstants
                        .getLevelDisplayableName(TechConstants.convertFromSimplelevel(eqRulesLevel, cockpit.isClan())));
                buff.append(")\n");
                retVal = true;
            }
        }
        if (getEntity() instanceof Mech) {
            ITechnology gyro = ((Mech)getEntity()).getGyroTechAdvancement();
            if (gyro != null) {
                int eqRulesLevel = getEntity().isMixedTech()?
                        gyro.findMinimumRulesLevel().ordinal() : gyro.findMinimumRulesLevel(getEntity().isClan()).ordinal();
                boolean illegal = eqRulesLevel > eRulesLevel;
                if (!getEntity().isMixedTech()) {
                    illegal |= getEntity().isClan() && gyro.getTechBase() == ITechnology.TECH_BASE_IS;
                    illegal |= !getEntity().isClan() && gyro.getTechBase() == ITechnology.TECH_BASE_CLAN;                
                }
                if (illegal) {
                    buff.append("Gyro is illegal at unit's tech level (");
                    buff.append(TechConstants
                            .getLevelDisplayableName(eTechLevel));
                    buff.append(", ");
                    buff.append(eTLYear);
                    buff.append("): ");
                    buff.append(((Mech)getEntity()).getGyroTypeString());
                    buff.append(" (");
                    buff.append(TechConstants
                            .getLevelDisplayableName(TechConstants.convertFromSimplelevel(eqRulesLevel,
                                    gyro.isClan())));
                    buff.append(")\n");
                    retVal = true;
                }
            }
        }
        if (getEntity().hasEngine()) {
            ITechnology engine = getEntity().getEngine().getTechAdvancement();
            int eqRulesLevel = getEntity().isMixedTech()?
                    engine.findMinimumRulesLevel().ordinal() : engine.findMinimumRulesLevel(getEntity().isClan()).ordinal();
            boolean illegal = eqRulesLevel > eRulesLevel;
            if (!getEntity().isMixedTech()) {
                illegal |= getEntity().isClan() && engine.getTechBase() == ITechnology.TECH_BASE_IS;
                illegal |= !getEntity().isClan() && engine.getTechBase() == ITechnology.TECH_BASE_CLAN;                
            }
            if (illegal) {
                buff.append("Engine is illegal at unit's tech level (");
                buff.append(TechConstants
                        .getLevelDisplayableName(eTechLevel));
                buff.append(", ");
                buff.append(eTLYear);
                buff.append("): ");
                buff.append(getEntity().getEngine().getShortEngineName());
                buff.append(" (");
                buff.append(TechConstants
                        .getLevelDisplayableName(TechConstants.convertFromSimplelevel(eqRulesLevel,
                                engine.isClan())));
                buff.append(")\n");
                buff.append("\n");
                retVal = true;
            }
        }

        return retVal;
    }

    public boolean hasFailedEquipment(StringBuffer buff) {
        boolean hasFailedEquipment = false;
        for (Iterator<String> e = getEntity().getFailedEquipment(); e.hasNext();) {
            String name = e.next();
            if (!ignoreFailedEquip(name)) {
                if (!hasFailedEquipment) {
                    buff.append("Equipment that Failed to Load:\n");
                }
                buff.append(name).append("\n");
                hasFailedEquipment = true;
            }
        }

        return hasFailedEquipment;
    }

    /**
     * Check if the unit has combinations of equipment which are not allowed in
     * the construction rules.
     *
     * @param buff
     *            diagnostics are appended to this
     * @return true if the entity is illegal
     */
    public boolean hasIllegalEquipmentCombinations(StringBuffer buff) {
        boolean illegal = false;
        int fieldKitchenCount = 0;
        int minesweeperCount = 0;
        boolean hasHarjelII = false;
        boolean hasHarjelIII = false;
        boolean hasCoolantPod = false;
        int emergencyCoolantCount = 0;
        for (Mounted m : getEntity().getAmmo()) {
            if (((AmmoType)m.getType()).getAmmoType() == AmmoType.T_COOLANT_POD) {
                hasCoolantPod = true;
            }
        }
        for (Mounted m : getEntity().getMisc()) {
            if (m.getType().hasFlag(MiscType.F_EMERGENCY_COOLANT_SYSTEM)) {
                emergencyCoolantCount++;
            }
            if (m.getType().hasFlag(MiscType.F_FIELD_KITCHEN)) {
                fieldKitchenCount++;
            }
            if (m.getType().hasFlag(MiscType.F_MINESWEEPER)) {
                minesweeperCount++;
            }

            if (m.getType().hasFlag(MiscType.F_LIGHT_FLUID_SUCTION_SYSTEM)) {
                if (getEntity() instanceof Protomech) {
                    illegal = true;
                    buff.append("ProtoMech can't mount light fluid suction system\n");
                }
            }
            if (m.getType().hasFlag(MiscType.F_VOIDSIG)
                    && !getEntity().hasWorkingMisc(MiscType.F_ECM)) {
                illegal = true;
                buff.append("void signature system needs ECM suite\n");
            }
            if (m.getType().hasFlag(MiscType.F_HARJEL_II)) {
                hasHarjelII = true;
            }
            if (m.getType().hasFlag(MiscType.F_HARJEL_III)) {
                hasHarjelIII = true;
            }
        }

        if (minesweeperCount > 1) {
            buff.append("Unit has more than one minesweeper!\n");
            illegal = true;
        }
        if (fieldKitchenCount > 3) {
            buff.append("Unit has more than three Field Kitchens\n");
            illegal = true;
        }

        if (hasCoolantPod && (emergencyCoolantCount > 0)) {
            buff.append("Unit has coolant pod and RISC emergency coolant system\n");
            illegal = true;
        }
        if (emergencyCoolantCount > 1) {
            buff.append("Unit has more than one RISC emergency coolant system\n");
            illegal = true;
        }
        if (!(getEntity() instanceof Mech) && (hasHarjelII || hasHarjelIII)) {
            buff.append("Cannot mount HarJel repair system on non-Mech\n");
            illegal = true;
        }
        
        if (getEntity().isOmni()) {
            for (Mounted m : getEntity().getEquipment()) {
                if (m.isOmniPodMounted() && m.getType().isOmniFixedOnly()) {
                    illegal = true;
                    buff.append(m.getType().getName() + " cannot be pod mounted.");
                }
            }
        } else {
            for (Mounted m : getEntity().getEquipment()) {
                if (m.isOmniPodMounted()) {
                    buff.append(m.getType().getName() + " is pod mounted in non-omni unit\n");
                    illegal = true;
                }
            }
            for (Transporter t : getEntity().getTransports()) {
                if (getEntity().isPodMountedTransport(t)) {
                    buff.append("Pod mounted troop space in non-omni unit\n");
                    illegal = true;
                }
            }
        }
        return illegal;
    }

    public StringBuffer printFailedEquipment(StringBuffer buff) {
        if (getEntity().getFailedEquipment().hasNext()) {
            buff.append("Equipment that Failed to Load:\n");
        }
        for (Iterator<String> e = getEntity().getFailedEquipment(); e.hasNext();) {
            buff.append(e.next()).append("\n");
        }
        return buff;
    }

    public double getWeightCarryingSpace() {
        double carryingSpace = getEntity().getTroopCarryingSpace();
        double cargoWeight = 0;
        for (Bay bay : getEntity().getTransportBays()) {
            cargoWeight += bay.getWeight();
        }
        return carryingSpace + cargoWeight;
    }

    public String printWeightCarryingSpace() {
        String carryingSpace = "";
        if (getEntity().getTroopCarryingSpace() != 0) {
            carryingSpace = StringUtil.makeLength("Carrying Capacity:",
                    getPrintSize() - 5)
                    + TestEntity.makeWeightString(getEntity()
                            .getTroopCarryingSpace()) + "\n";
        }
        String cargoWeightString = "";
        double cargoWeight = 0;
        for (Bay bay : getEntity().getTransportBays()) {
            cargoWeight += bay.getWeight();
        }
        if (cargoWeight > 0) {
            cargoWeightString = StringUtil.makeLength("Cargo Weight:",
                    getPrintSize() - 5)
                    + TestEntity.makeWeightString(cargoWeight) + "\n";
        }
        return carryingSpace + cargoWeightString;
    }

    public String printArmorLocation(int loc) {
        if (getEntity().hasRearArmor(loc)) {
            return StringUtil.makeLength(
                    getEntity().getLocationAbbr(loc) + ":", 5)
                    + StringUtil.makeLength(getEntity().getOInternal(loc), 4)
                    + StringUtil.makeLength(getEntity().getOArmor(loc), 3)
                    + " / "
                    + StringUtil
                            .makeLength(getEntity().getOArmor(loc, true), 2);
        }
        return StringUtil.makeLength(getEntity().getLocationAbbr(loc) + ":", 5)
                + StringUtil.makeLength(getEntity().getOInternal(loc), 4)
                + StringUtil.makeLength(getEntity().getOArmor(loc), 6) + "  ";
    }

    public String printArmorPlacement() {
        StringBuffer buff = new StringBuffer();
        buff.append("Armor Placement:\n");
        for (int loc = 0; loc < getEntity().locations(); loc++) {
            buff.append(printArmorLocation(loc)).append("\n");
        }
        return buff.toString();
    }

    public String printSource(){
        return "Source: " + getEntity().getSource() + "\n";
    }

    public String printTechLevel() {
        return "Chassis: " + getEntity().getDisplayName() + " - "
                + TechConstants.getLevelName(getEntity().getTechLevel()) + " ("
                + Integer.toString(getEntity().getYear()) + ")\n";
    }

    public double getArmoredComponentWeight() {
        return 0.0f;
    }

} // End class TestEntity

class Armor {
    public final static int CLAN_ARMOR = 0x01;

    private int armorType;

    private int armorFlags;

    public Armor(int armorType, int armorFlags) {
        this.armorType = armorType;
        this.armorFlags = armorFlags;
    }

    public double getWeightArmor(int totalOArmor, TestEntity.Ceil roundWeight) {
        return Armor.getWeightArmor(armorType, armorFlags, totalOArmor,
                roundWeight);
    }

    public static double getWeightArmor(int armorType, int armorFlags,
            int totalOArmor, TestEntity.Ceil roundWeight) {
        double points = totalOArmor;
        int techLevel;
        if ((armorFlags & CLAN_ARMOR) != 0) {
            techLevel = TechConstants.T_CLAN_TW;
        } else {
            techLevel = TechConstants.T_IS_TW_NON_BOX;
        }
        double multiplier = EquipmentType.getArmorPointMultiplier(armorType,
                techLevel);
        points /= multiplier;
        double pointsPerTon = 16.0f;
        double armorWeight = points / pointsPerTon;
        return TestEntity.ceilMaxHalf(armorWeight, roundWeight);
    }

    public String getShortName() {
        return "(" + EquipmentType.getArmorTypeName(armorType) + ")";
    }

} // end class Armor

class Structure {

    private int structureType;
    private boolean isSuperHeavy;
    private EntityMovementMode movementmode;

    public Structure() {
    }

    public Structure(int structureType, boolean superHeavy,
            EntityMovementMode movementMode) {
        this.structureType = structureType;
        isSuperHeavy = superHeavy;
        movementmode = movementMode;
    }

    public double getWeightStructure(double weight, TestEntity.Ceil roundWeight) {
        return Structure.getWeightStructure(structureType, weight, roundWeight,
                isSuperHeavy, movementmode);
    }

    public static double getWeightStructure(int structureType, double weight,
            TestEntity.Ceil roundWeight, boolean isSuperHeavy,
            EntityMovementMode movementmode) {
        double multiplier = 1.0;
        if (movementmode == EntityMovementMode.TRIPOD) {
            multiplier = 1.1;
        }
        if (structureType == EquipmentType.T_STRUCTURE_ENDO_STEEL) {
            if (isSuperHeavy) {
                return TestEntity.ceilMaxHalf((weight / 10.0f) * multiplier,
                        roundWeight);
            } else {
                return TestEntity.ceilMaxHalf((weight / 20.0f) * multiplier,
                        roundWeight);
            }
        } else if (structureType == EquipmentType.T_STRUCTURE_ENDO_PROTOTYPE) {
            return TestEntity.ceilMaxHalf((weight / 20.0f) * multiplier,
                    roundWeight);
        } else if (structureType == EquipmentType.T_STRUCTURE_REINFORCED) {
            return TestEntity.ceilMaxHalf((weight / 5.0f) * multiplier,
                    roundWeight);
        } else if (structureType == EquipmentType.T_STRUCTURE_COMPOSITE) {
            return TestEntity.ceilMaxHalf((weight / 20.0f) * multiplier,
                    roundWeight);
        } else if (structureType == EquipmentType.T_STRUCTURE_INDUSTRIAL) {
            if (isSuperHeavy) {
                return TestEntity.ceilMaxHalf((weight / 2.5f) * multiplier,
                        roundWeight);
            } else {
                return TestEntity.ceilMaxHalf((weight / 5.0f) * multiplier,
                        roundWeight);
            }

        } else if (structureType == EquipmentType.T_STRUCTURE_ENDO_COMPOSITE) {
            if (isSuperHeavy) {
                return TestEntity.ceilMaxHalf((weight / 10.0f) * 1.5f
                        * multiplier, roundWeight);
            } else {
                return TestEntity.ceilMaxHalf((weight / 10.0f) * 0.75f
                        * multiplier, roundWeight);
            }
        }
        if (isSuperHeavy
                && ((movementmode != EntityMovementMode.NAVAL)
                        && (movementmode != EntityMovementMode.SUBMARINE))) {
            return TestEntity.ceilMaxHalf((weight / 5.0f) * multiplier,
                    roundWeight);
        } else {
            return TestEntity.ceilMaxHalf((weight / 10.0f) * multiplier,
                    roundWeight);
        }
    }

    public String getShortName() {
        return "(" + EquipmentType.getStructureTypeName(structureType) + ")";
    }

} // End class Structure

