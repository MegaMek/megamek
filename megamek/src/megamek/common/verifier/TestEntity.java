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
import megamek.common.BipedMech;
import megamek.common.BombType;
import megamek.common.CriticalSlot;
import megamek.common.Engine;
import megamek.common.Entity;
import megamek.common.EntityMovementMode;
import megamek.common.EquipmentType;
import megamek.common.Mech;
import megamek.common.MiscType;
import megamek.common.Mounted;
import megamek.common.Protomech;
import megamek.common.QuadMech;
import megamek.common.Tank;
import megamek.common.TechConstants;
import megamek.common.TripodMech;
import megamek.common.VTOL;
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
        return StringUtil.makeLength("Engine: " + engine.getEngineName(),
                getPrintSize() - 5)
                + TestEntity.makeWeightString(getWeightEngine()) + "\n";
    }

    public double getWeightEngine() {
        double weight = engine.getWeightEngine(getEntity(),
                getWeightCeilingEngine());
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
        boolean retVal = false;
        int eTechLevel = getEntity().getTechLevel();
        int eTLYear = getEntity().getTechLevelYear();
        boolean mixedTech = getEntity().isMixedTech();
        for (Mounted mounted : getEntity().getEquipment()) {
            EquipmentType nextE = mounted.getType();
            int eqTechLvl = nextE.getTechLevel(eTLYear);
            if (nextE instanceof AmmoType) {
                if (!TechConstants.isLegal(ammoTechLvl, eqTechLvl, mixedTech)) {
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
                            .getLevelDisplayableName(eqTechLvl));
                    buff.append(")\n");
                }
            } else if (!(TechConstants.isLegal(eTechLevel, eqTechLvl, true,
                    mixedTech))) {
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
                        .getLevelDisplayableName(eqTechLvl));
                buff.append(")\n");
            }
        }
        // Check cockpit TL
        int cockpitTL;
        int cockpitType;
        if (getEntity() instanceof Aero) {
            cockpitType =  ((Aero) getEntity()).getCockpitType();
            cockpitTL = TechConstants.getCockpitTechLevel(
                    cockpitType, Entity.ETYPE_AERO,
                    getEntity().isClan(), eTLYear);
            if (!TechConstants.isLegal(eTechLevel, cockpitTL, mixedTech)) {
                buff.append("Cockpit is illegal at unit's tech level (");
                buff.append(TechConstants
                        .getLevelDisplayableName(eTechLevel));
                buff.append(", ");
                buff.append(eTLYear);
                buff.append("): ");
                buff.append(Mech.getCockpitDisplayString(cockpitType));
                buff.append(" (");
                buff.append(TechConstants
                        .getLevelDisplayableName(cockpitTL));
                buff.append(")\n");
                retVal = true;
            }
        } else if (getEntity() instanceof Mech) {
            // TODO: Enable TL testing for cockpits/gyros
            //  This ends up causing canon units to fail, and we have to come
            //  up with a way to deal with this
            /*
            Mech mech = (Mech) getEntity();
            cockpitType = mech.getCockpitType();
            cockpitTL = TechConstants.getCockpitTechLevel(cockpitType,
                    mech.getEntityType(), mech.isClan(), eTLYear);
            int gyroType = mech.getGyroType();
            int gyroTL = TechConstants.getGyroTechLevel(gyroType,
                    mech.isClan(), eTLYear);
            if (!TechConstants.isLegal(eTechLevel, cockpitTL, mixedTech)) {
                buff.append("Cockpit is illegal at unit's tech level (");
                buff.append(TechConstants
                        .getLevelDisplayableName(eTechLevel));
                buff.append(", ");
                buff.append(eTLYear);
                buff.append("): ");
                buff.append(Mech.getCockpitDisplayString(cockpitType));
                buff.append(" (");
                buff.append(TechConstants
                        .getLevelDisplayableName(cockpitTL));
                buff.append(")\n");
                retVal = true;
            }
            if (!TechConstants.isLegal(eTechLevel, gyroTL, mixedTech)) {
                buff.append("Gyro is illegal at unit's tech level (");
                buff.append(TechConstants
                        .getLevelDisplayableName(eTechLevel));
                buff.append(", ");
                buff.append(eTLYear);
                buff.append("): ");
                buff.append(Mech.getGyroDisplayString(gyroType));
                buff.append(" (");
                buff.append(TechConstants
                        .getLevelDisplayableName(cockpitTL));
                buff.append(")\n");
                retVal = true;
            }
            */
        }
        if (getEntity().getEngine() != null) {
            // TODO: Enable TL testing for engines
            //  This ends up causing canon units to fail, and we have to come
            //  up with a way to deal with this
            /*
            int engineTL = getEntity().getEngine().getTechType(eTLYear);
            if (!TechConstants.isLegal(eTechLevel, engineTL, mixedTech)) {
                buff.append("Engine is illegal at unit's tech level (");
                buff.append(TechConstants
                        .getLevelDisplayableName(eTechLevel));
                buff.append(", ");
                buff.append(eTLYear);
                buff.append("): ");
                buff.append(getEntity().getEngine().getShortEngineName());
                buff.append(" (");
                buff.append(TechConstants
                        .getLevelDisplayableName(engineTL));
                buff.append(")\n");
                buff.append("Engine is illegal at unit's tech level: ");
                buff.append(getEntity().getEngine().getShortEngineName());
                buff.append("\n");
                retVal = true;
            }
            */
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
        boolean hasSponsonTurret = false;
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
            if (m.getType().hasFlag(MiscType.F_LIGHT_FLUID_SUCTION_SYSTEM)) {
                if ((getEntity() instanceof Mech) && !((Mech)getEntity()).isIndustrial()) {
                    illegal = true;
                    buff.append("BattleMech can't mount light fluid suction system\n");
                }
                if (getEntity() instanceof Protomech) {
                    illegal = true;
                    buff.append("ProtoMech can't mount light fluid suction system\n");
                }
                if ((getEntity() instanceof Tank) && (m.getLocation() == Tank.LOC_BODY)) {
                    illegal = true;
                    buff.append("Vehicle must not mount light fluid suction system in body\n");
                }
            }
            if (m.getType().hasFlag(MiscType.F_EMERGENCY_COOLANT_SYSTEM)) {
                emergencyCoolantCount++;
            }
            if (m.getType().hasFlag(MiscType.F_VOIDSIG)
                    && !getEntity().hasWorkingMisc(MiscType.F_ECM)) {
                illegal = true;
                buff.append("void signature system needs ECM suite\n");
            }
            if (m.getType().hasFlag(MiscType.F_FIELD_KITCHEN)) {
                fieldKitchenCount++;
            }
            if (m.getType().hasFlag(MiscType.F_MINESWEEPER)) {
                minesweeperCount++;
            }
            if (m.getType().hasFlag(MiscType.F_SPONSON_TURRET)) {
                hasSponsonTurret = true;
            }
            if (m.getType().hasFlag(MiscType.F_HARJEL_II)) {
                hasHarjelII = true;
            }
            if (m.getType().hasFlag(MiscType.F_HARJEL_III)) {
                hasHarjelIII = true;
            }
            if (m.getType().hasFlag(MiscType.F_BULLDOZER)) {
                for (Mounted m2 : getEntity().getMisc()) {
                    if (m2.getLocation() == m.getLocation()) {
                        if (m2.getType().hasFlag(MiscType.F_CLUB)) {
                            if (m2.getType().hasSubType(MiscType.S_BACKHOE)
                                    || m2.getType().hasSubType(
                                            MiscType.S_CHAINSAW)
                                    || m2.getType().hasSubType(
                                            MiscType.S_COMBINE)
                                    || m2.getType().hasSubType(
                                            MiscType.S_DUAL_SAW)
                                    || m2.getType().hasSubType(
                                            MiscType.S_PILE_DRIVER)
                                    || m2.getType().hasSubType(
                                            MiscType.S_MINING_DRILL)
                                    || m2.getType().hasSubType(
                                            MiscType.S_ROCK_CUTTER)
                                    || m2.getType().hasSubType(
                                            MiscType.S_WRECKING_BALL)) {
                                illegal = true;
                                buff.append("bulldozer in same location as prohibited physical weapon\n");
                            }
                        }
                    }
                }
                if ((m.getLocation() != Tank.LOC_FRONT) && (m.getLocation() != Tank.LOC_REAR)) {
                    illegal = true;
                    buff.append("bulldozer must be mounted in front\n");
                }
                if ((getEntity().getMovementMode() != EntityMovementMode.TRACKED)
                        && (getEntity().getMovementMode() != EntityMovementMode.WHEELED)) {
                    illegal = true;
                    buff.append("bulldozer must be mounted in unit with tracked or wheeled movement mode\n");
                }
            }

        }
        if (getEntity() instanceof Tank) {
            for (Mounted m : getEntity().getMisc()) {
                if (m.getType().hasFlag(MiscType.F_JUMP_JET)) {
                    if (hasSponsonTurret) {
                        buff.append("can't combine vehicular jump jets and sponson turret\n");
                        illegal = true;
                    }
                    if ((getEntity().getMovementMode() != EntityMovementMode.HOVER)
                            && (getEntity().getMovementMode() != EntityMovementMode.WHEELED)
                            && (getEntity().getMovementMode() != EntityMovementMode.TRACKED)
                            && (getEntity().getMovementMode() != EntityMovementMode.WIGE)) {
                        buff.append("jump jets only possible on vehicles with hover, wheeled, tracked, or Wing-in-Ground Effect movement mode\n");
                        illegal = true;
                    }
                }


                if (m.getType().hasFlag(MiscType.F_HARJEL)
                        && ((m.getLocation() == Tank.LOC_BODY)
                                || ((getEntity() instanceof VTOL)
                                    && (m.getLocation() == VTOL.LOC_ROTOR)))) {
                    illegal = true;
                    buff.append("Unable to load harjel in body or rotor.\n");
                }
            }
        }

        // Ensure that omni tank turrets aren't overloaded
        if ((getEntity() instanceof Tank) && getEntity().isOmni()) {
            Tank tank = (Tank) getEntity();
            // Check to see if the base chassis turret weight is set
            double turretWeight = 0;
            double turret2Weight = 0;
            for (Mounted m : tank.getEquipment()) {
                if ((m.getLocation() == tank.getLocTurret2())
                        && !(m.getType() instanceof AmmoType)) {
                    turret2Weight += m.getType().getTonnage(tank);
                }
                if ((m.getLocation() == tank.getLocTurret())
                        && !(m.getType() instanceof AmmoType)) {
                    turretWeight += m.getType().getTonnage(tank);
                }
            }
            turretWeight *= 0.1f;
            turret2Weight *= 0.1f;
            if (tank.isSupportVehicle()) {
                if (getEntity().getWeight() < 5) {
                    turretWeight = TestEntity.ceil(turretWeight, Ceil.KILO);
                    turret2Weight = TestEntity.ceil(turret2Weight, Ceil.KILO);
                } else {
                    turretWeight = TestEntity.ceil(turretWeight, Ceil.HALFTON);
                    turret2Weight = TestEntity.ceil(turret2Weight, Ceil.HALFTON);
                }
            } else {
                turretWeight = TestEntity.ceil(turretWeight,
                        getWeightCeilingTurret());
                turret2Weight = TestEntity.ceil(turret2Weight,
                        getWeightCeilingTurret());
            }
            if ((tank.getBaseChassisTurretWeight() >= 0)
                    && (turretWeight > tank.getBaseChassisTurretWeight())) {
                buff.append("Unit has more weight in the turret than allowed "
                        + "by base chassis!  Current weight: " + turretWeight
                        + ", base chassis turret weight: "
                        + tank.getBaseChassisTurretWeight() + "\n");
                illegal = true;
            }
            if ((tank.getBaseChassisTurret2Weight() >= 0)
                    && (turret2Weight > tank.getBaseChassisTurret2Weight())) {
                buff.append("Unit has more weight in the second turret than "
                        + "allowed by base chassis!  Current weight: "
                        + turret2Weight + ", base chassis turret weight: "
                        + tank.getBaseChassisTurret2Weight() + "\n");
                illegal = true;
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
        if (getEntity() instanceof Tank) {
            Tank tank = (Tank) getEntity();
            if ((tank.getMovementMode() == EntityMovementMode.VTOL)
                    || (tank.getMovementMode() == EntityMovementMode.WIGE)
                    || (tank.getMovementMode() == EntityMovementMode.HOVER)) {
                for (int i = 0; i < tank.locations(); i++) {
                    if (tank.getArmorType(i) == EquipmentType.T_ARMOR_HARDENED) {
                        buff.append("Hardened armor can't be mounted on WiGE/Hover/Wheeled vehicles\n");
                        illegal = true;
                    }
                }
            }

        }
        if (!(getEntity() instanceof Mech) && (hasHarjelII || hasHarjelIII)) {
            buff.append("Cannot mount HarJel repair system on non-Mech\n");
            illegal = true;
        }
        if (getEntity() instanceof Mech) {
            Mech mech = (Mech) getEntity();
            if (hasHarjelII && hasHarjelIII) {
                illegal = true;
                buff.append("Can't mix HarJel II and HarJel III\n");
            }
            if (hasHarjelII || hasHarjelIII) {
                if (mech.isIndustrial()) {
                    buff.append("Cannot mount HarJel repair system on IndustrialMech\n");
                    illegal = true;
                }
                // note: should check for pod mount on Omni here, but we don't
                // track equipment fixed vs pod-mount status
                for (int loc = 0; loc < mech.locations(); ++loc) {
                    int count = 0;
                    for (Mounted m : mech.getMisc()) {
                        if ((m.getLocation() == loc)
                            && (m.getType().hasFlag(MiscType.F_HARJEL_II)
                             || m.getType().hasFlag(MiscType.F_HARJEL_III))) {
                            ++count;
                        }
                    }
                    if (count > 1) {
                        buff.append("Cannot mount multiple HarJel repair systems in a location\n");
                        illegal = true;
                    }
                    if (count == 1) {
                        int armor = mech.getArmorType(loc);
                        switch (armor) {
                            case EquipmentType.T_ARMOR_STANDARD:
                            case EquipmentType.T_ARMOR_FERRO_FIBROUS:
                            case EquipmentType.T_ARMOR_LIGHT_FERRO:
                            case EquipmentType.T_ARMOR_HEAVY_FERRO:
                            case EquipmentType.T_ARMOR_HEAVY_INDUSTRIAL:
                                // these armors are legal with HarJel
                                break;
                            default:
                                buff.append("Cannot mount HarJel repair system in location with this armor type\n");
                                illegal = true;
                        }
                    }
                }
            }
            if (mech.hasWorkingWeapon(WeaponType.F_TASER)) {
                switch (mech.getEngine().getEngineType()) {
                    case Engine.FISSION:
                    case Engine.FUEL_CELL:
                    case Engine.COMBUSTION_ENGINE:
                        buff.append("Mech Taser needs fusion engine\n");
                        illegal = true;
                        break;
                    default:
                        break;
                }
            }
            if (mech.hasFullHeadEject()) {
                if ((mech.getCockpitType() == Mech.COCKPIT_TORSO_MOUNTED)
                        || (mech.getCockpitType() == Mech.COCKPIT_COMMAND_CONSOLE)) {
                    buff.append("full head ejection system incompatible with cockpit type\n");
                    illegal = true;
                }
            }
            // only one sword/vibroblade per arm
            for (int loc = Mech.LOC_RARM; loc <= Mech.LOC_LARM; loc++) {
                int count = 0;
                for (Mounted m : mech.getMisc()) {
                    if (m.getLocation() == loc) {
                        if (m.getType().hasFlag(MiscType.F_CLUB)
                                && (m.getType().hasSubType(MiscType.S_SWORD)
                                        || m.getType().hasSubType(
                                                MiscType.S_VIBRO_LARGE)
                                        || m.getType().hasSubType(
                                                MiscType.S_VIBRO_MEDIUM) || m
                                        .getType().hasSubType(
                                                MiscType.S_VIBRO_SMALL))) {
                            count++;
                        }
                    }
                }
                if (count > 1) {
                    buff.append("only one sword/vibroblade per arm\n");
                    illegal = true;
                }
            }

            // TODO: disallow the weapons on quads, except unless it's one per
            // side torso
            for (int loc = Mech.LOC_RARM; loc <= Mech.LOC_LARM; loc++) {
                if (mech.hasSystem(Mech.ACTUATOR_HAND, loc)) {
                    for (Mounted m : mech.getMisc()) {
                        EquipmentType et = m.getType();
                        if ((m.getLocation() == loc)
                                && (et.hasSubType(MiscType.S_CHAINSAW)
                                        || et.hasSubType(MiscType.S_BACKHOE)
                                        || et.hasSubType(MiscType.S_DUAL_SAW)
                                        || et.hasSubType(MiscType.S_PILE_DRIVER)
                                        || et.hasSubType(MiscType.S_MINING_DRILL)
                                        || et.hasSubType(MiscType.S_ROCK_CUTTER)
                                        || et.hasSubType(MiscType.S_SPOT_WELDER)
                                        || et.hasSubType(MiscType.S_WRECKING_BALL)
                                        || et.hasSubType(MiscType.S_COMBINE) || et
                                            .hasFlag(MiscType.F_SALVAGE_ARM))) {
                            buff.append("Unit mounts hand-actuator incompatible system in arm with hand\n");
                            illegal = true;
                        }
                    }
                }
            }
            for (Mounted m : mech.getMisc()) {
                if (m.getType().hasFlag(MiscType.F_MASC)
                        && m.getType().hasSubType(MiscType.S_SUPERCHARGER)) {
                    boolean foundEngine = false;
                    int numCrits = mech.getNumberOfCriticals(m.getLocation());
                    for (int i = 0; i < numCrits; i++) {
                        CriticalSlot ccs = mech.getCritical(m.getLocation(), i);
                        if ((ccs != null)
                                && (ccs.getType() == CriticalSlot.TYPE_SYSTEM)
                                && (ccs.getIndex() == Mech.SYSTEM_ENGINE)) {
                            foundEngine = true;
                        }
                    }
                    if (!foundEngine) {
                        buff.append("supercharger in location without engine\n");
                        illegal = true;
                    }
                }
                if (m.getType().hasFlag(MiscType.F_AP_POD)) {
                    if ((mech instanceof QuadMech)) {
                        if (((m.getLocation() != Mech.LOC_LLEG)
                                && (m.getLocation() != Mech.LOC_RLEG)
                                && (m.getLocation() != Mech.LOC_LARM) && (m
                                    .getLocation() != Mech.LOC_RARM))) {
                            buff.append("A-Pod must be mounted in leg\n");
                            illegal = true;
                        }
                    } else if (mech instanceof TripodMech) {
                        if ((m.getLocation() != Mech.LOC_LLEG)
                                && (m.getLocation() != Mech.LOC_RLEG)
                                && (m.getLocation() != Mech.LOC_CLEG)) {
                            buff.append("A-Pod must be mounted in leg\n");
                            illegal = true;
                        }
                    } else {
                        if ((m.getLocation() != Mech.LOC_LLEG)
                                && (m.getLocation() != Mech.LOC_RLEG)) {
                            buff.append("A-Pod must be mounted in leg\n");
                            illegal = true;
                        }
                    }
                }
                if (m.getType().hasFlag(MiscType.F_REMOTE_DRONE_COMMAND_CONSOLE)) {
                    if (mech.getCockpitType() == Mech.COCKPIT_COMMAND_CONSOLE) {
                        buff.append("cockpit command console can't be combined with remote drone command console\n");
                        illegal = true;
                    }
                    if ((mech.getCockpitType() == Mech.COCKPIT_TORSO_MOUNTED) && (m.getLocation() != Mech.LOC_CT)) {
                        buff.append("remote drone command console must be placed in same location as cockpit\n");
                        illegal = true;
                    } else {
                        if (m.getLocation() != Mech.LOC_HEAD) {
                            buff.append("remote drone command console must be placed in same location as cockpit\n");
                            illegal = true;
                        }
                    }

                }
                if (m.getType().hasFlag(MiscType.F_EMERGENCY_COOLANT_SYSTEM) && !mech.hasWorkingSystem(Mech.SYSTEM_ENGINE, m.getLocation())) {
                    buff.append("RISC emergency coolant system must be mounted in location with engine crit\n");
                    illegal = true;
                }
            }

            if (mech.hasNullSig()) {
                if (mech.hasStealth()) {
                    buff.append("Unit mounts both null-signature-system and stealth armor\n");
                    illegal = true;
                }
                if (mech.hasTargComp()) {
                    buff.append("Unit mounts both null-signature-system and targeting computer\n");
                    illegal = true;
                }
                if (mech.hasVoidSig()) {
                    buff.append("Unit mounts both null-signature-system and void-signature-system\n");
                    illegal = true;
                }
                if (mech.hasC3()) {
                    buff.append("Unit mounts both null-signature-system and a c3 system\n");
                    illegal = true;
                }
            }
            if (mech.hasVoidSig()) {
                if (mech.hasStealth()) {
                    buff.append("Unit mounts both void-signature-system and stealth armor\n");
                    illegal = true;
                }
                if (mech.hasTargComp()) {
                    buff.append("Unit mounts both void-signature-system and targeting computer\n");
                    illegal = true;
                }
                if (mech.hasC3()) {
                    buff.append("Unit mounts both void-signature-system and a c3 system\n");
                    illegal = true;
                }
                if (mech.hasChameleonShield()) {
                    buff.append("Unit mounts both void-signature-system and a chameleon light polarisation shield\n");
                    illegal = true;
                }
            }
            if (mech.hasChameleonShield() && mech.hasStealth()) {
                buff.append("Unit mounts both chameleon-light-polarization-system and stealth armor\n");
                illegal = true;
            }
            if (mech.isIndustrial()) {
                if (mech.hasMisc(MiscType.F_SCM)) {
                    buff.append("industrial mech can't mount normal SCM\n");
                    illegal = true;
                }
                if (mech.hasTSM()) {
                    buff.append("industrial mech can't mount normal TSM\n");
                    illegal = true;
                }
                if (mech.hasMASC()) {
                    buff.append("industrial mech can't mount MASC\n");
                    illegal = true;
                }
                if ((mech.getCockpitType() == Mech.COCKPIT_INDUSTRIAL)
                        || (mech.getCockpitType() == Mech.COCKPIT_PRIMITIVE_INDUSTRIAL)) {
                    if (mech.hasC3()) {
                        buff.append("industrial mech without advanced fire control can't use c3 computer\n");
                        illegal = true;
                    }
                    if (mech.hasTargComp()) {
                        buff.append("industrial mech without advanced fire control can't use targeting computer\n");
                        illegal = true;
                    }
                    if (mech.hasBAP()) {
                        buff.append("industrial mech without advanced fire control can't use BAP\n");
                        illegal = true;
                    }
                    for (Mounted mounted : mech.getMisc()) {
                        if (mounted.getType().hasFlag(MiscType.F_ARTEMIS)
                                || mounted.getType().hasFlag(
                                        MiscType.F_ARTEMIS_V)) {
                            buff.append("industrial mech without advanced fire control can't use artemis\n");
                            illegal = true;
                        }
                    }
                }
                if ((mech.getJumpType() != Mech.JUMP_STANDARD)
                        && (mech.getJumpType() != Mech.JUMP_NONE)
                        && (mech.getJumpType() != Mech.JUMP_PROTOTYPE)
                        && (mech.getJumpType() != Mech.JUMP_PROTOTYPE_IMPROVED)
                        && (mech.getJumpType() != Mech.JUMP_BOOSTER)) {
                    buff.append("industrial mechs can only mount standard jump jets or mechanical jump boosters\n");
                    illegal = true;
                }
                if (mech.getGyroType() != Mech.GYRO_STANDARD) {
                    buff.append("industrial mechs can only mount standard gyros\n");
                    illegal = true;
                }
            } else {
                if (mech.hasIndustrialTSM()) {
                    buff.append("standard mech can't mount industrial TSM\n");
                    illegal = true;
                }
                if (mech.hasEnvironmentalSealing()) {
                    buff.append("standard mech can't mount environmental sealing\n");
                    illegal = true;
                }
            }
            if (mech.hasMASC() && mech.hasMisc(MiscType.F_SCM)) {
                buff.append("can't combine SCM and MASC\n");
                illegal = true;
            }
            if (mech.hasTSM() && mech.hasMisc(MiscType.F_SCM)) {
                buff.append("can't combine SCM and TSM\n");
                illegal = true;
            }
            if (mech.isPrimitive()) {
                if (mech.isOmni()) {
                    buff.append("primitive mechs can't be omnis\n");
                    illegal = true;
                }
                if (!((mech.getStructureType() == EquipmentType.T_STRUCTURE_STANDARD) || (mech
                        .getStructureType() == EquipmentType.T_STRUCTURE_INDUSTRIAL))) {
                    buff.append("primitive mechs can't mount advanced inner structure\n");
                    illegal = true;
                }
                if ((mech.getEngine().getEngineType() == Engine.XL_ENGINE)
                        || (mech.getEngine().getEngineType() == Engine.LIGHT_ENGINE)
                        || (mech.getEngine().getEngineType() == Engine.COMPACT_ENGINE)
                        || mech.getEngine().hasFlag(Engine.LARGE_ENGINE)
                        || (mech.getEngine().getEngineType() == Engine.XXL_ENGINE)) {
                    buff.append("primitive mechs can't mount XL, Light, Compact, XXL or Large Engines\n");
                    illegal = true;
                }
                if (mech.hasMASC() || mech.hasTSM()) {
                    buff.append("primitive mechs can't mount advanced myomers\n");
                    illegal = true;
                }
                if (mech.isIndustrial()) {
                    if (mech.getArmorType(0) != EquipmentType.T_ARMOR_COMMERCIAL) {
                        buff.append("primitive industrialmechs must mount commercial armor\n");
                        illegal = true;
                    }
                } else {
                    if ((mech.getArmorType(0) != EquipmentType.T_ARMOR_PRIMITIVE)
                            && (mech.getArmorType(0) != EquipmentType.T_ARMOR_INDUSTRIAL)) {
                        buff.append("primitive battlemechs must mount primitive battlemech armor\n");
                        illegal = true;
                    }
                }
            }

            for (Mounted mounted : mech.getMisc()) {
                if (mounted.getType().hasFlag(
                        MiscType.F_ACTUATOR_ENHANCEMENT_SYSTEM)) {

                    if (mech.hasTargComp()
                            || mech.hasTSM()
                            || (mech.hasMASC() && !mech.hasWorkingMisc(
                                    MiscType.F_MASC, MiscType.S_SUPERCHARGER))) {
                        illegal = true;
                        buff.append("Unable to load AES due to incompatible systems\n");
                    }

                    if ((mounted.getLocation() != Mech.LOC_LARM)
                            && (mounted.getLocation() != Mech.LOC_LLEG)
                            && (mounted.getLocation() != Mech.LOC_RARM)
                            && (mounted.getLocation() != Mech.LOC_RLEG)) {
                        illegal = true;
                        buff.append("Unable to load AES due to incompatible location\n");
                    }
                }

                if (((mounted.getType().hasFlag(MiscType.F_HARJEL))
                        || mounted.getType().hasFlag(MiscType.F_HARJEL_II)
                        || mounted.getType().hasFlag(MiscType.F_HARJEL_III))
                        && (((mounted.getLocation() == Mech.LOC_CT) && (mech
                            .getCockpitType() == Mech.COCKPIT_TORSO_MOUNTED))
                            || ((mounted.getLocation() == Mech.LOC_HEAD) && (mech
                            .getCockpitType() != Mech.COCKPIT_TORSO_MOUNTED)))) {
                    illegal = true;
                    buff.append("Harjel can't be mounted in a location with a "
                            + "cockpit!");
                }

                if (mounted.getType().hasFlag(MiscType.F_MASS)
                        && ((mounted.getLocation() != Mech.LOC_HEAD) || ((mech
                                .getCockpitType() == Mech.COCKPIT_TORSO_MOUNTED) && (mounted
                                .getLocation() != Mech.LOC_CT)))) {
                    illegal = true;
                    buff.append("Unable to load MASS!  Must be located in the same location as the cockpit.\n");
                }

                if (mounted.getType().hasFlag(MiscType.F_MODULAR_ARMOR)
                        && (mounted.getLocation() == Mech.LOC_HEAD)) {
                    illegal = true;
                    buff.append("Unable to load Modular Armor in Head location\n");
                }

                if (mounted.getType().hasFlag(MiscType.F_HEAD_TURRET)
                        && (mech.getCockpitType() != Mech.COCKPIT_TORSO_MOUNTED)) {
                    illegal = true;
                    buff.append("head turret requires torso mounted cockpit\n");
                }
                if (mounted.getType().hasFlag(MiscType.F_SHOULDER_TURRET)
                        && (mech instanceof QuadMech)) {
                    illegal = true;
                    buff.append("quad mechs can't mount shoulder turrets\n");
                }
                if (mounted.getType().hasFlag(MiscType.F_SHOULDER_TURRET)
                        && !((mounted.getLocation() == Mech.LOC_RT) || (mounted
                                .getLocation() == Mech.LOC_LT))) {
                    illegal = true;
                    buff.append("shoulder turret must be mounted in side torso\n");
                }
                if (mounted.getType().hasFlag(MiscType.F_SHOULDER_TURRET)
                        && (mech.countWorkingMisc(MiscType.F_SHOULDER_TURRET,
                                mounted.getLocation()) > 1)) {
                    illegal = true;
                    buff.append("max of 1 shoulder turret per side torso\n");
                }
                if (mounted.getType().hasFlag(MiscType.F_TALON)) {
                    if (mech instanceof BipedMech) {
                        if ((mounted.getLocation() != Mech.LOC_LLEG)
                                && (mounted.getLocation() != Mech.LOC_RLEG)) {
                            illegal = true;
                            buff.append("Talons are only legal in the Legs\n");
                        }

                        if (!mech.hasWorkingMisc(MiscType.F_TALON, -1,
                                Mech.LOC_RLEG)
                                || !mech.hasWorkingMisc(MiscType.F_TALON, -1,
                                        Mech.LOC_LLEG)) {
                            illegal = true;
                            buff.append("Talons must be in all legs\n");
                        }
                    } else if (mech instanceof QuadMech) {
                        if ((mounted.getLocation() != Mech.LOC_LLEG)
                                && (mounted.getLocation() != Mech.LOC_RLEG)
                                && (mounted.getLocation() != Mech.LOC_LARM)
                                && (mounted.getLocation() != Mech.LOC_RARM)) {
                            buff.append("Talons are only legal in the Legs\n");
                            illegal = true;
                        }

                        if (!mech.hasWorkingMisc(MiscType.F_TALON, -1,
                                Mech.LOC_RLEG)
                                || !mech.hasWorkingMisc(MiscType.F_TALON, -1,
                                        Mech.LOC_LLEG)
                                || !mech.hasWorkingMisc(MiscType.F_TALON, -1,
                                        Mech.LOC_LARM)
                                || !mech.hasWorkingMisc(MiscType.F_TALON, -1,
                                        Mech.LOC_LARM)) {
                            buff.append("Talons must be in all legs\n");
                            illegal = true;
                        }

                    } else {
                        buff.append("Unable to load talons in non-Mek entity\n");
                        illegal = true;
                    }
                }
            }

            if (mech.hasUMU() && (mech.getJumpType() != Mech.JUMP_NONE)
                    && (mech.getJumpType() != Mech.JUMP_BOOSTER)) {
                illegal = true;
                buff.append("UMUs cannot be mounted with jump jets "
                        + "(jump boosters are legal)");
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

