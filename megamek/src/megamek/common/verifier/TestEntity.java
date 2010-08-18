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

import megamek.common.AmmoType;
import megamek.common.BipedMech;
import megamek.common.CriticalSlot;
import megamek.common.Engine;
import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.Mech;
import megamek.common.MiscType;
import megamek.common.Mounted;
import megamek.common.QuadMech;
import megamek.common.TechConstants;
import megamek.common.WeaponType;
import megamek.common.util.StringUtil;

public abstract class TestEntity implements TestEntityOption {

    public final static float CEIL_TON = 1.0f;
    public final static float CEIL_HALFTON = 2.0f;
    public final static float CEIL_QUARTERTON = 4.0f;
    public final static float CEIL_TENTHTON = 10.0f;

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

    public abstract boolean correctEntity(StringBuffer buff, boolean ignoreAmmo);

    public abstract StringBuffer printEntity();

    public abstract String getName();

    public String fileString = null; // where the unit came from

    public TestEntity(TestEntityOption options, Engine engine, Armor armor, Structure structure) {
        this.options = options;
        this.engine = engine;
        this.armor = armor;
        this.structure = structure;
    }

    public boolean isClan() {
        return getEntity().isClan();
    }

    public boolean isClanArmor() {
        return getEntity().isClanArmor();
    }

    public float getWeight() {
        return getEntity().getWeight();
    }

    public int getTotalOArmor() {
        return getEntity().getTotalOArmor();
    }

    public String getLocationAbbr(int location) {
        return getEntity().getLocationAbbr(location);
    }

    public float getWeightCeilingEngine() {
        return options.getWeightCeilingEngine();
    }

    public float getWeightCeilingStructure() {
        return options.getWeightCeilingStructure();
    }

    public float getWeightCeilingArmor() {
        return options.getWeightCeilingArmor();
    }

    public float getWeightCeilingControls() {
        return options.getWeightCeilingControls();
    }

    public float getWeightCeilingWeapons() {
        return options.getWeightCeilingWeapons();
    }

    public float getWeightCeilingTargComp() {
        return options.getWeightCeilingTargComp();
    }

    public float getWeightCeilingGyro() {
        return options.getWeightCeilingGyro();
    }

    public float getWeightCeilingTurret() {
        return options.getWeightCeilingTurret();
    }

    public float getWeightCeilingPowerAmp() {
        return options.getWeightCeilingPowerAmp();
    }

    public float getMaxOverweight() {
        return options.getMaxOverweight();
    }

    public boolean showOverweightedEntity() {
        return options.showOverweightedEntity();
    }

    public float getMinUnderweight() {
        return options.getMinUnderweight();
    }

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

    protected static float ceil(float f, float type) {
        return (float) Math.ceil(f * type) / type;
    }

    public static float ceilMaxHalf(float f, float type) {
        if (type == CEIL_TON) {
            return TestEntity.ceil(f, CEIL_HALFTON);
        }
        return TestEntity.ceil(f, type);
    }

    protected static String makeWeightString(float weight) {
        return (weight < 100 ? " " : "") + (weight < 10 ? " " : "") + Float.toString(weight) + ((Math.ceil(weight * 10) == weight * 10) ? "0" : "");
    }

    private boolean hasMASC() {
        if (getEntity() instanceof Mech) {
            return ((Mech) getEntity()).hasMASC();
        }
        return false;
    }

    public String printShortMovement() {
        return "Movement: " + Integer.toString(getEntity().getOriginalWalkMP()) + "/" + Integer.toString((int) Math.ceil(getEntity().getOriginalWalkMP() * 1.5)) + (hasMASC() ? "(" + Integer.toString(getEntity().getOriginalWalkMP() * 2) + ")" : "") + (getEntity().getOriginalJumpMP() != 0 ? "/" + Integer.toString(getEntity().getOriginalJumpMP()) : "") + "\n";
    }

    public String printWeightHeatSinks() {
        return StringUtil.makeLength("Heat Sinks: " + Integer.toString(getCountHeatSinks()) + (hasDoubleHeatSinks() ? " [" + Integer.toString(2 * getCountHeatSinks()) + "]" : ""), getPrintSize() - 5) + TestEntity.makeWeightString(getWeightHeatSinks()) + "\n";
    }

    public String printWeightEngine() {
        return StringUtil.makeLength("Engine: " + engine.getEngineName(), getPrintSize() - 5) + TestEntity.makeWeightString(getWeightEngine()) + "\n";
    }

    public float getWeightEngine() {
        float weight = engine.getWeightEngine(getEntity(), getWeightCeilingEngine());
        return weight;
    }

    public String printWeightStructure() {
        return StringUtil.makeLength("Structure: " + Integer.toString(getEntity().getTotalOInternal()) + " " + structure.getShortName(), getPrintSize() - 5) + TestEntity.makeWeightString(getWeightStructure()) + "\n";
    }

    public float getWeightStructure() {
        return structure.getWeightStructure(getWeight(), getWeightCeilingStructure());
    }

    public String printWeightArmor() {
        return StringUtil.makeLength("Armor: " + Integer.toString(getTotalOArmor()) + " " + armor.getShortName(), getPrintSize() - 5) + TestEntity.makeWeightString(getWeightArmor()) + "\n";
    }

    public float getWeightArmor() {
        return armor.getWeightArmor(getTotalOArmor(), getWeightCeilingArmor());
    }

    public float getWeightMiscEquip() {
        float weightSum = 0.0f;
        for (Mounted m : getEntity().getMisc()) {
            MiscType mt = (MiscType) m.getType();
            if (mt.hasFlag(MiscType.F_ENDO_STEEL) ||
                    mt.hasFlag(MiscType.F_FERRO_FIBROUS) ||
                    mt.hasFlag(MiscType.F_FERRO_LAMELLOR) ||
                    mt.hasFlag(MiscType.F_ENDO_COMPOSITE) ||
                    mt.hasFlag(MiscType.F_HEAT_SINK) ||
                    mt.hasFlag(MiscType.F_DOUBLE_HEAT_SINK)) {
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

    public StringBuffer printMiscEquip(StringBuffer buff, int posLoc, int posWeight) {
        for (Mounted m : getEntity().getMisc()) {
            MiscType mt = (MiscType) m.getType();

            if (m.getLocation() == Entity.LOC_NONE) {
                continue;
            }

            if (mt.getTonnage(getEntity(), m.getLocation()) == 0f) {
                continue;
            }

            buff.append(StringUtil.makeLength(mt.getName(), 20));
            buff.append(StringUtil.makeLength(getLocationAbbr(m.getLocation()), getPrintSize() - 5 - 20)).append(TestEntity.makeWeightString(mt.getTonnage(getEntity())));
            buff.append("\n");
        }
        return buff;
    }

    public float getWeightWeapon() {
        float weight = 0.0f;
        for (Mounted m : getEntity().getWeaponList()) {
            WeaponType wt = (WeaponType) m.getType();
            weight += wt.getTonnage(getEntity());
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
            buff.append(StringUtil.makeLength(getLocationAbbr(m.getLocation()), getPrintSize() - 5 - 20)).append(TestEntity.makeWeightString(mt.getTonnage(getEntity()))).append("\n");
        }
        return buff;
    }

    public float getWeightAmmo() {
        float weight = 0.0f;
        for (Mounted m : getEntity().getAmmo()) {

            // One Shot Ammo
            if (m.getLocation() == Entity.LOC_NONE) {
                continue;
            }

            AmmoType mt = (AmmoType) m.getType();
            weight += mt.getTonnage(getEntity());
        }
        return weight;
    }

    public abstract float getWeightPowerAmp();

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
            buff.append(StringUtil.makeLength(getLocationAbbr(m.getLocation()), getPrintSize() - 5 - 20)).append(TestEntity.makeWeightString(mt.getTonnage(getEntity()))).append("\n");
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
                        buff.append(((Mech) getEntity()).getSystemName(slot.getIndex()));
                        buff.append("\n");
                    } else {
                        buff.append(Integer.toString(j) + ". UNKNOWN SYSTEM NAME");
                        buff.append("\n");
                    }
                } else if (slot.getType() == CriticalSlot.TYPE_EQUIPMENT) {
                    Mounted m = getEntity().getEquipment(slot.getIndex());

                    buff.append(Integer.toString(j) + ". " + m.getType().getInternalName());
                    buff.append("\n");
                }
            }
        }
        return buff.toString();
    }

    public int calcMiscCrits(MiscType mt) {
        if (mt.hasFlag(MiscType.F_CLUB) && (mt.hasSubType(MiscType.S_HATCHET) || mt.hasSubType(MiscType.S_SWORD) || mt.hasSubType(MiscType.S_CHAIN_WHIP) || mt.hasSubType(MiscType.S_MACE_THB))) {
            return (int) Math.ceil(getWeight() / 15.0);
        } else if (mt.hasFlag(MiscType.F_CLUB) && mt.hasSubType(MiscType.S_MACE)) {
            return (int) Math.ceil(getWeight() / 10.0);
        } else if (mt.hasFlag(MiscType.F_CLUB) && mt.hasSubType(MiscType.S_RETRACTABLE_BLADE)) {
            return 1 + (int) Math.ceil(getWeight() / 20.0);
        } else if (mt.hasFlag(MiscType.F_CLUB) && mt.hasSubType(MiscType.S_PILE_DRIVER)) {
            return 8;
        } else if (mt.hasFlag(MiscType.F_CLUB) && mt.hasSubType(MiscType.S_CHAINSAW)) {
            return 5;
        } else if (mt.hasFlag(MiscType.F_CLUB) && mt.hasSubType(MiscType.S_DUAL_SAW)) {
            return 7;
        } else if (mt.hasFlag(MiscType.F_CLUB) && mt.hasSubType(MiscType.S_BACKHOE)) {
            return 6;
        } else if (mt.hasFlag(MiscType.F_MASC)) {
            if (mt.getInternalName().equals("ISMASC")) {
                return Math.round(getWeight() / 20.0f);
            } else if (mt.getInternalName().equals("CLMASC")) {
                return Math.round(getWeight() / 25.0f);
            }
        } else if (mt.hasFlag(MiscType.F_TARGCOMP)) {
            float fTons = 0.0f;
            for (Mounted mo : getEntity().getWeaponList()) {
                WeaponType wt = (WeaponType) mo.getType();
                if (wt.hasFlag(WeaponType.F_DIRECT_FIRE)) {
                    fTons += wt.getTonnage(getEntity());
                }
            }
            float weight = 0.0f;
            if (mt.getInternalName().equals("ISTargeting Computer")) {
                weight = TestEntity.ceil(fTons / 4.0f, getWeightCeilingTargComp());
            } else if (mt.getInternalName().equals("CLTargeting Computer")) {
                weight = TestEntity.ceil(fTons / 5.0f, getWeightCeilingTargComp());
            }
            switch (getTargCompCrits()) {
                case CEIL_TARGCOMP_CRITS:
                    return (int) Math.ceil(weight);
                case ROUND_TARGCOMP_CRITS:
                    return Math.round(weight);
                case FLOOR_TARGCOMP_CRITS:
                    return (int) Math.floor(weight);
            }
        } else if (EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_FERRO_FIBROUS).equals(mt.getInternalName())) {
            if (isClanArmor()) {
                return 7;
            }
            return 14;
        } else if (EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_FERRO_FIBROUS_PROTO).equals(mt.getInternalName())) {
            return 16;
        } else if (EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_LIGHT_FERRO).equals(mt.getInternalName())) {
            return 7;
        } else if (EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_HEAVY_FERRO).equals(mt.getInternalName())) {
            return 21;
        } else if (EquipmentType.getStructureTypeName(EquipmentType.T_STRUCTURE_ENDO_STEEL).equals(mt.getInternalName())) {
            if (isClan()) {
                return 7;
            }
            return 14;
        } else if (EquipmentType.getStructureTypeName(EquipmentType.T_STRUCTURE_ENDO_PROTOTYPE).equals(mt.getInternalName())) {
            return 16;
        } else if (EquipmentType.getStructureTypeName(EquipmentType.T_STRUCTURE_ENDO_COMPOSITE).equals(mt.getInternalName())) {
            if (isClan()) {
                return 4;
            }
            return 7;
        } else if (EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_REACTIVE).equals(mt.getInternalName())) {
            if (isClanArmor()) {
                return 7;
            }
            return 14;
        } else if (EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_REFLECTIVE).equals(mt.getInternalName())) {
            if (isClanArmor()) {
                return 5;
            }
            return 10;
        }
        return mt.getCriticals(getEntity());
    }

    public float calculateWeight() {
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
        weight += getWeightPowerAmp();

        weight += getWeightCarryingSpace();

        weight += getArmoredComponentWeight();
        return weight;
    }

    public String printWeightCalculation() {
        return printWeightEngine() + printWeightStructure() + printWeightControls() + printWeightHeatSinks() + printWeightArmor() + printWeightMisc() + printWeightCarryingSpace() + "Equipment:\n" + printMiscEquip() + printWeapon() + printAmmo();
    }

    public boolean correctWeight(StringBuffer buff) {
        return correctWeight(buff, showOverweightedEntity(), showUnderweightedEntity());
    }

    public boolean correctWeight(StringBuffer buff, boolean showO, boolean showU) {
        float weightSum = calculateWeight();
        float weight = getWeight();

        if (showO && (weight + getMaxOverweight() < weightSum)) {
            buff.append("Weight: ").append(calculateWeight()).append(" is greater then ").append(getWeight()).append("\n");
            // buff.append(printWeightCalculation()).append("\n");
            return false;
        }
        if (showU && (weight - getMinUnderweight() > weightSum)) {
            buff.append("Weight: ").append(calculateWeight()).append(" is lesser then ").append(getWeight()).append("\n");
            // buff.append(printWeightCalculation()).append("\n");
            return false;
        }
        return true;
    }

    public boolean hasIllegalTechLevels(StringBuffer buff) {
        return hasIllegalTechLevels(buff, true);
    }

    public boolean hasIllegalTechLevels(StringBuffer buff, boolean ignoreAmmo) {
        boolean retVal = false;
        int eTechLevel = getEntity().getTechLevel();
        for (Mounted mounted : getEntity().getEquipment()) {
            EquipmentType nextE = mounted.getType();
            if ((ignoreAmmo) && (nextE instanceof AmmoType)) {
                continue;
            } else if (!(TechConstants.isLegal(eTechLevel, nextE.getTechLevel(), true))) {
                if (!retVal) {
                    buff.append("Equipment illegal at unit's tech level:\n");
                }
                retVal = true;
                buff.append(nextE.getName()).append("\n");
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
        int tagCount = 0;
        boolean illegal = false;
        for (Mounted m : getEntity().getWeaponList()) {
            if (m.getType().hasFlag(WeaponType.F_TAG) && !m.getType().hasFlag(WeaponType.F_C3M)) {
                tagCount++;
            }
        }
        if (tagCount > 1) {
            buff.append("Unit has more than one TAG\n");
            illegal = true;
        }
        if (getEntity() instanceof Mech) {
            Mech mech = (Mech) getEntity();
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
                if ((mech.getCockpitType() == Mech.COCKPIT_TORSO_MOUNTED) || (mech.getCockpitType() == Mech.COCKPIT_COMMAND_CONSOLE)) {
                    buff.append("full head ejection system incompatible with cockpit type");
                    illegal = true;
                }
            }

            // TODO: disallow the weapons on quads, except unless it's one per
            // side torso
            for (int loc = Mech.LOC_RARM; loc <= Mech.LOC_LARM; loc++) {
                if (mech.hasSystem(Mech.ACTUATOR_HAND, loc)) {
                    for (Mounted m : mech.getMisc()) {
                        EquipmentType et = m.getType();
                        if ((m.getLocation() == loc) && (et.hasSubType(MiscType.S_CHAINSAW) || et.hasSubType(MiscType.S_BACKHOE) || et.hasSubType(MiscType.S_DUAL_SAW) || et.hasSubType(MiscType.S_COMBINE) || et.hasSubType(MiscType.S_PILE_DRIVER) || et.hasSubType(MiscType.S_MINING_DRILL) || et.hasSubType(MiscType.S_ROCK_CUTTER) || et.hasSubType(MiscType.S_SPOT_WELDER) || et.hasSubType(MiscType.S_WRECKING_BALL) || et.hasSubType(MiscType.S_COMBINE) || et.hasFlag(MiscType.F_SALVAGE_ARM))) {
                            buff.append("Unit mounts hand-actuator incompatible system in arm with hand\n");
                            illegal = true;
                        }
                    }
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
                if (mech.hasTSM()) {
                    buff.append("industrial mech can't mount normal TSM\n");
                    illegal = true;
                }
                if (mech.hasMASC()) {
                    buff.append("industrial mech can't mount MASC\n");
                    illegal = true;
                }
                if ((mech.getCockpitType() == Mech.COCKPIT_INDUSTRIAL) || (mech.getCockpitType() == Mech.COCKPIT_PRIMITIVE_INDUSTRIAL)) {
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
                        if (mounted.getType().hasFlag(MiscType.F_ARTEMIS) || mounted.getType().hasFlag(MiscType.F_ARTEMIS_V)) {
                            buff.append("industrial mech without advanced fire control can't use artemis\n");
                            illegal = true;
                        }
                    }
                    if ((mech.getJumpType() != Mech.JUMP_STANDARD) && (mech.getJumpType() != Mech.JUMP_NONE)) {
                        buff.append("industrial mechs can only mount standard jump jets\n");
                        illegal = true;
                    }
                    if (mech.getGyroType() != Mech.GYRO_STANDARD) {
                        buff.append("industrial mechs can only mount standard gyros\n");
                        illegal = true;
                    }
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
            if (mech.isPrimitive()) {
                if (mech.isOmni()) {
                    buff.append("primitive mechs can't be omnis\n");
                    illegal = true;
                }
                if (!((mech.getStructureType() == EquipmentType.T_STRUCTURE_STANDARD) || (mech.getStructureType() == EquipmentType.T_STRUCTURE_INDUSTRIAL))) {
                    buff.append("primitive mechs can't mount advanced inner structure\n");
                    illegal = true;
                }
                if ((mech.getEngine().getEngineType() == Engine.XL_ENGINE) || (mech.getEngine().getEngineType() == Engine.LIGHT_ENGINE) || (mech.getEngine().getEngineType() == Engine.COMPACT_ENGINE) || mech.getEngine().hasFlag(Engine.LARGE_ENGINE) || (mech.getEngine().getEngineType() == Engine.XXL_ENGINE)) {
                    buff.append("primitive mechs can't mount XL, Light, Compact, XXL or Large Engines\n");
                    illegal = true;
                }
                if (mech.hasMASC() || mech.hasTSM()) {
                    buff.append("primitive mechs can't mount advanced myomers\n");
                    illegal = true;
                }
                if (mech.isIndustrial()) {
                    if (mech.getArmorType() != EquipmentType.T_ARMOR_COMMERCIAL) {
                        buff.append("primitive industrialmechs must mount commercial armor\n");
                        illegal = true;
                    }
                } else {
                    if (mech.getArmorType() != EquipmentType.T_ARMOR_INDUSTRIAL) {
                        buff.append("primitive battlemechs must mount primitive battlemech (industrial) armor\n");
                        illegal = true;
                    }
                }
            }

            for (Mounted mounted : mech.getMisc()) {
                if (mounted.getType().hasFlag(MiscType.F_ACTUATOR_ENHANCEMENT_SYSTEM)) {

                    if (mech.hasTargComp() || mech.hasTSM() || (mech.hasMASC() && !mech.hasWorkingMisc(MiscType.F_MASC, MiscType.S_SUPERCHARGER))) {
                        illegal = true;
                        buff.append("Unable to load AES due to incompatible systems\n");
                    }

                    if ((mounted.getLocation() != Mech.LOC_LARM) && (mounted.getLocation() != Mech.LOC_LLEG) && (mounted.getLocation() != Mech.LOC_RARM) && (mounted.getLocation() != Mech.LOC_RLEG)) {
                        illegal = true;
                        buff.append("Unable to load AES due to incompatible location\n");
                    }

                }

                if (mounted.getType().hasFlag(MiscType.F_HARJEL) && (mounted.getLocation() == Mech.LOC_HEAD)) {
                    illegal = true;
                    buff.append("Unable to load harjel in head.\n");
                }

                if (mounted.getType().hasFlag(MiscType.F_MASS) && ((mounted.getLocation() != Mech.LOC_HEAD) || ((mech.getCockpitType() == Mech.COCKPIT_TORSO_MOUNTED) && (mounted.getLocation() != Mech.LOC_CT)))) {
                    illegal = true;
                    buff.append("Unable to load MASS!  Must be located in the same location as the cockpit.\n");
                }

                if (mounted.getType().hasFlag(MiscType.F_MODULAR_ARMOR) && (mounted.getLocation() == Mech.LOC_HEAD)) {
                    illegal = true;
                    buff.append("Unable to load Modular Armor in Rotor/Head location\n");
                }

                if (mounted.getType().hasFlag(MiscType.F_HEAD_TURRET) && (mech.getCockpitType() != Mech.COCKPIT_TORSO_MOUNTED)) {
                    illegal = true;
                    buff.append("head turret requires torso mounted cockpit\n");
                }
                if (mounted.getType().hasFlag(MiscType.F_HEAD_TURRET) && (mech instanceof QuadMech)) {
                    illegal = true;
                    buff.append("quad mechs can't mount head turrets\n");
                }
                if (mounted.getType().hasFlag(MiscType.F_SHOULDER_TURRET) && (mech instanceof QuadMech)) {
                    illegal = true;
                    buff.append("quad mechs can't mount shoulder turrets\n");
                }
                if (mounted.getType().hasFlag(MiscType.F_SHOULDER_TURRET) && !((mounted.getLocation() == Mech.LOC_RT) || (mounted.getLocation() == Mech.LOC_LT))) {
                    illegal = true;
                    buff.append("shoulder turret must be mounted in side torso\n");
                }
                if (mounted.getType().hasFlag(MiscType.F_SHOULDER_TURRET) && (mech.countWorkingMisc(MiscType.F_SHOULDER_TURRET, mounted.getLocation()) > 1)) {
                    illegal = true;
                    buff.append("max of 1 shoulder turret per side torso\n");
                }
                if (mounted.getType().hasFlag(MiscType.F_TALON)) {
                    if (mech instanceof BipedMech) {
                        if ((mounted.getLocation() != Mech.LOC_LLEG) && (mounted.getLocation() != Mech.LOC_RLEG)) {
                            illegal = true;
                            buff.append("Talons are only legal in the Legs\n");
                        }

                        if (!mech.hasWorkingMisc(MiscType.F_TALON, -1, Mech.LOC_RLEG) || !mech.hasWorkingMisc(MiscType.F_TALON, -1, Mech.LOC_LLEG)) {
                            illegal = true;
                            buff.append("Talons must be in all legs\n");
                        }
                    } else if (mech instanceof QuadMech) {
                        if ((mounted.getLocation() != Mech.LOC_LLEG) && (mounted.getLocation() != Mech.LOC_RLEG) && (mounted.getLocation() != Mech.LOC_LARM) && (mounted.getLocation() != Mech.LOC_RARM)) {
                            buff.append("Talons are only legal in the Legs\n");
                            illegal = true;
                        }

                        if (!mech.hasWorkingMisc(MiscType.F_TALON, -1, Mech.LOC_RLEG) || !mech.hasWorkingMisc(MiscType.F_TALON, -1, Mech.LOC_LLEG) || !mech.hasWorkingMisc(MiscType.F_TALON, -1, Mech.LOC_LARM) || !mech.hasWorkingMisc(MiscType.F_TALON, -1, Mech.LOC_LARM)) {
                            buff.append("Talons must be in all legs\n");
                            illegal = true;
                        }

                    } else {
                        buff.append("Unable to load talons in non-Mek entity\n");
                        illegal = true;
                    }
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

    public float getWeightCarryingSpace() {
        return getEntity().getTroopCarryingSpace();
    }

    public String printWeightCarryingSpace() {
        if (getEntity().getTroopCarryingSpace() != 0) {
            return StringUtil.makeLength("Carrying Capacity:", getPrintSize() - 5) + TestEntity.makeWeightString(getEntity().getTroopCarryingSpace()) + "\n";
        }
        return "";
    }

    public String printArmorLocation(int loc) {
        if (getEntity().hasRearArmor(loc)) {
            return StringUtil.makeLength(getEntity().getLocationAbbr(loc) + ":", 5) + StringUtil.makeLength(getEntity().getOInternal(loc), 4) + StringUtil.makeLength(getEntity().getOArmor(loc), 3) + " / " + StringUtil.makeLength(getEntity().getOArmor(loc, true), 2);
        }
        return StringUtil.makeLength(getEntity().getLocationAbbr(loc) + ":", 5) + StringUtil.makeLength(getEntity().getOInternal(loc), 4) + StringUtil.makeLength(getEntity().getOArmor(loc), 6) + "  ";
    }

    public String printArmorPlacement() {
        StringBuffer buff = new StringBuffer();
        buff.append("Armor Placement:\n");
        for (int loc = 0; loc < getEntity().locations(); loc++) {
            buff.append(printArmorLocation(loc)).append("\n");
        }
        return buff.toString();
    }

    public String printTechLevel() {
        return "Chassis: " + getEntity().getMovementMode().toString() + " - " + TechConstants.getLevelName(getEntity().getTechLevel()) + " (" + Integer.toString(getEntity().getYear()) + ")\n";
    }

    public float getArmoredComponentWeight() {
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

    public float getWeightArmor(int totalOArmor, float roundWeight) {
        return Armor.getWeightArmor(armorType, armorFlags, totalOArmor, roundWeight);
    }

    public static float getWeightArmor(int armorType, int armorFlags, int totalOArmor, float roundWeight) {
        double points = totalOArmor;
        int techLevel;
        if ((armorFlags & CLAN_ARMOR) != 0) {
            techLevel = TechConstants.T_CLAN_TW;
        } else {
            techLevel = TechConstants.T_IS_TW_NON_BOX;
        }
        double multiplier = EquipmentType.getArmorPointMultiplier(armorType, techLevel);
        points /= multiplier;
        double pointsPerTon = 16.0f;
        if (armorType == EquipmentType.T_ARMOR_HARDENED) {
            pointsPerTon = 8.0f;
        }
        double armorWeight = points / pointsPerTon;
        return TestEntity.ceilMaxHalf((float)armorWeight, roundWeight);
    }

    public String getShortName() {
        return "(" + EquipmentType.getArmorTypeName(armorType) + ")";
    }

} // end class Armor

class Structure {
    public final static int CLAN_STRUCTURE = 0x01;

    private int structureType;

    public Structure(int structureType, int structureFlags) {
        this.structureType = structureType;
        // this.structureFlags = structureFlags;
    }

    public float getWeightStructure(float weight, float roundWeight) {
        return Structure.getWeightStructure(structureType, weight, roundWeight);
    }

    public static float getWeightStructure(int structureType, float weight, float roundWeight) {
        if (structureType == EquipmentType.T_STRUCTURE_ENDO_STEEL) {
            return TestEntity.ceilMaxHalf(weight / 20.0f, roundWeight);
        } else if (structureType == EquipmentType.T_STRUCTURE_ENDO_PROTOTYPE) {
            return TestEntity.ceilMaxHalf(weight / 20.0f, roundWeight);
        } else if (structureType == EquipmentType.T_STRUCTURE_REINFORCED) {
            return TestEntity.ceilMaxHalf(weight / 5.0f, roundWeight);
        } else if (structureType == EquipmentType.T_STRUCTURE_COMPOSITE) {
            return TestEntity.ceilMaxHalf(weight / 20.0f, roundWeight);
        } else if (structureType == EquipmentType.T_STRUCTURE_INDUSTRIAL) {
            return TestEntity.ceilMaxHalf(weight / 5.0f, roundWeight);
        } else if (structureType == EquipmentType.T_STRUCTURE_ENDO_COMPOSITE) {
            return TestEntity.ceilMaxHalf(weight / 10.0f * 0.75f, roundWeight);
        }
        return weight / 10.0f;
    }

    public String getShortName() {
        return "(" + EquipmentType.getStructureTypeName(structureType) + ")";
    }

} // End class Structure

