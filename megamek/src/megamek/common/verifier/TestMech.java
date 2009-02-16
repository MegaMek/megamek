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

import java.io.Serializable;
import java.util.Enumeration;
import java.util.Vector;

import megamek.common.AmmoType;
import megamek.common.CriticalSlot;
import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.Mech;
import megamek.common.MiscType;
import megamek.common.Mounted;
import megamek.common.WeaponType;
import megamek.common.util.StringUtil;

public class TestMech extends TestEntity {
    private Mech mech = null;

    public TestMech(Mech mech, TestEntityOption option, String fileString) {
        super(option, mech.getEngine(), getArmor(mech), getStructure(mech));
        this.mech = mech;
        this.fileString = fileString;
    }

    private static Structure getStructure(Mech mech) {
        int type = mech.getStructureType();
        int flag = 0;
        if (mech.isClan()) {
            flag |= Structure.CLAN_STRUCTURE;
        }
        return new Structure(type, flag);
    }

    private static Armor getArmor(Mech mech) {
        int type = mech.getArmorType();
        int flag = 0;
        if (mech.isClanArmor()) {
            flag |= Armor.CLAN_ARMOR;
        }
        return new Armor(type, flag);
    }

    @Override
    public Entity getEntity() {
        return mech;
    }

    @Override
    public boolean isTank() {
        return false;
    }

    @Override
    public boolean isMech() {
        return true;
    }

    @Override
    public float getWeightMisc() {
        return 0.0f;
    }

    public float getWeightCockpit() {
        float weight = 3.0f;
        if (mech.getCockpitType() == Mech.COCKPIT_SMALL) {
            weight = 2.0f;
        } else if (mech.getCockpitType() == Mech.COCKPIT_TORSO_MOUNTED) {
            weight = 4.0f;
        } else if (mech.getCockpitType() == Mech.COCKPIT_COMMAND_CONSOLE) {
            // Technically, it's two separate 3-ton pieces of equipment.
            // We're ignoring that and returning the total, because it's easier.
            weight = 6.0f;
        } else if (mech.getCockpitType() == Mech.COCKPIT_DUAL) {
            // This is wrong; I just don't remember the correct weight.
            // FIXME
            weight = 3.0f;
        } else if (mech.getCockpitType() == Mech.COCKPIT_PRIMITIVE) {
            weight = 5.0f;
        } else if (mech.getCockpitType() == Mech.COCKPIT_PRIMITIVE_INDUSTRIAL) {
            weight = 5.0f;
        }

        return weight;
    }

    public float getWeightGyro() {
        float retVal = (float) Math.ceil(engine.getRating() / 100.0f);
        if (mech.getGyroType() == Mech.GYRO_XL) {
            retVal /= 2;
        } else if (mech.getGyroType() == Mech.GYRO_COMPACT) {
            retVal *= 1.5;
        } else if (mech.getGyroType() == Mech.GYRO_HEAVY_DUTY) {
            retVal *= 2;
        }
        retVal = ceil(retVal, getWeightCeilingGyro());
        return retVal;
    }

    @Override
    public float getWeightControls() {
        return getWeightCockpit() + getWeightGyro();
    }

    @Override
    public int getCountHeatSinks() {
        return mech.heatSinks();
    }

    @Override
    public int getWeightHeatSinks() {
        return mech.heatSinks() - engine.getWeightFreeEngineHeatSinks();
    }

    @Override
    public boolean hasDoubleHeatSinks() {
        if (mech.heatSinks() != mech.getHeatCapacity()) {
            return true;
        }
        return false;
    }

    @Override
    public String printWeightMisc() {
        return "";
    }

    @Override
    public String printWeightControls() {
        StringBuffer retVal = new StringBuffer(StringUtil.makeLength(mech
                .getCockpitTypeString()
                + ":", getPrintSize() - 5));
        retVal.append(makeWeightString(getWeightCockpit()));
        retVal.append("\n");
        retVal.append(StringUtil.makeLength(mech.getGyroTypeString() + ":",
                getPrintSize() - 5));
        retVal.append(makeWeightString(getWeightGyro()));
        retVal.append("\n");
        return retVal.toString();
    }

    public Mech getMech() {
        return mech;
    }

    public int countCriticalSlotsFromEquipInLocation(Entity entity, int eNum,
            int location) {
        int count = 0;
        for (int slots = 0; slots < entity.getNumberOfCriticals(location); slots++) {
            CriticalSlot slot = entity.getCritical(location, slots);
            if ((slot == null) || (slot.getType() == CriticalSlot.TYPE_SYSTEM)) {
                continue;
            } else if (slot.getType() == CriticalSlot.TYPE_EQUIPMENT) {
                if (slot.getIndex() == eNum) {
                    count++;
                }
            } else {
                // Ignore this?
            }
        }
        return count;
    }

    public boolean criticalSlotsAllocated(Entity entity, Mounted mounted,
            Vector<Serializable> allocation) {
        int eNum = entity.getEquipmentNum(mounted);
        int location = mounted.getLocation();
        EquipmentType et = mounted.getType();
        int criticals = 0;
        if (et instanceof MiscType) {
            criticals = calcMiscCrits((MiscType) et);
        } else {
            criticals = et.getCriticals(entity);
        }
        int count = 0;

        if (location == Entity.LOC_NONE) {
            return true;
        }

        if (et.isSpreadable() && !et.getName().equals("Targeting Computer")) {
            if ((et instanceof MiscType) && et.hasFlag(MiscType.F_STEALTH)) {
                // stealth needs to have 2 crits in legs arm and side torso
                if (countCriticalSlotsFromEquipInLocation(entity, eNum,
                        Mech.LOC_LARM) != 2) {
                    return false;
                }
                if (countCriticalSlotsFromEquipInLocation(entity, eNum,
                        Mech.LOC_RARM) != 2) {
                    return false;
                }
                if (countCriticalSlotsFromEquipInLocation(entity, eNum,
                        Mech.LOC_LLEG) != 2) {
                    return false;
                }
                if (countCriticalSlotsFromEquipInLocation(entity, eNum,
                        Mech.LOC_RLEG) != 2) {
                    return false;
                }
                if (countCriticalSlotsFromEquipInLocation(entity, eNum,
                        Mech.LOC_LT) != 2) {
                    return false;
                }
                if (countCriticalSlotsFromEquipInLocation(entity, eNum,
                        Mech.LOC_RT) != 2) {
                    return false;
                }
            }
            if ((et instanceof MiscType) && et.hasFlag(MiscType.F_ENVIRONMENTAL_SEALING)) {
                // environmental sealing needs to have 1 crit per location
                for (int locations = 0; locations < entity.locations(); locations++) {
                    if (countCriticalSlotsFromEquipInLocation(entity, eNum,
                            locations) != 1) {
                        return false;
                    }
                }
            }
            for (int locations = 0; locations < entity.locations(); locations++) {
                count += countCriticalSlotsFromEquipInLocation(entity, eNum,
                        locations);
            }
        } else {
            count = countCriticalSlotsFromEquipInLocation(entity, eNum,
                    location);
        }

        if ((et instanceof WeaponType) && mounted.isSplit()) {
            int secCound = 0;
            for (int locations = 0; locations < entity.locations(); locations++) {
                if (locations == location) {
                    continue;
                }

                secCound = countCriticalSlotsFromEquipInLocation(entity, eNum,
                        locations);
                if ((secCound != 0)
                        && (location == Mech.mostRestrictiveLoc(locations,
                                location))) {
                    count += secCound;
                    break;
                }
            }
        }

        if (count == criticals) {
            return true;
        }

        allocation.addElement(mounted);
        allocation.addElement(new Integer(criticals));
        allocation.addElement(new Integer(count));
        return false;
    }

    public void checkCriticalSlotsForEquipment(Entity entity,
            Vector<Mounted> unallocated, Vector<Serializable> allocation,
            Vector<Integer> heatSinks) {
        int countInternalHeatSinks = 0;
        for (Mounted m : entity.getEquipment()) {
            if (m.getLocation() == Entity.LOC_NONE) {
                if ((m.getType() instanceof AmmoType) && (m.getShotsLeft() <= 1)) {
                    continue;
                }
                if (!(m.getType() instanceof MiscType)) {
                    unallocated.addElement(m);
                    continue;
                }
                MiscType mt = (MiscType) m.getType();
                if (mt.hasFlag(MiscType.F_HEAT_SINK)
                        || mt.hasFlag(MiscType.F_DOUBLE_HEAT_SINK)) {
                    countInternalHeatSinks++;
                } else {
                    unallocated.addElement(m);
                    continue;
                }

            } else if (!criticalSlotsAllocated(entity, m, allocation)) {
            }
        }
        if ((countInternalHeatSinks > engine.integralHeatSinkCapacity())
                || ((countInternalHeatSinks < engine.integralHeatSinkCapacity())
                        && (countInternalHeatSinks != ((Mech) entity)
                                .heatSinks()) && !entity.isOmni())) {
            heatSinks.addElement(new Integer(countInternalHeatSinks));
        }
    }

    public void checkCriticals() {
        Vector<Mounted> unallocated = new Vector<Mounted>();
        Vector<Serializable> allocation = new Vector<Serializable>();
        Vector<Integer> heatSinks = new Vector<Integer>();
        checkCriticalSlotsForEquipment(mech, unallocated, allocation, heatSinks);
    }

    public boolean correctCriticals(StringBuffer buff) {
        Vector<Mounted> unallocated = new Vector<Mounted>();
        Vector<Serializable> allocation = new Vector<Serializable>();
        Vector<Integer> heatSinks = new Vector<Integer>();
        checkCriticalSlotsForEquipment(mech, unallocated, allocation, heatSinks);
        boolean correct = true;
        if (!unallocated.isEmpty()) {
            buff.append("Unallocated Equipment:\n");
            for (Mounted m : unallocated) {
            buff.append(m.getType().getInternalName()).append("\n");
         }
            correct = false;
        }
        if (!allocation.isEmpty()) {
            buff.append("Allocated Equipment:\n");
            for (Enumeration<Serializable> serializableEnum = allocation.elements();serializableEnum.hasMoreElements();) {
                Mounted m = (Mounted) serializableEnum.nextElement();
                int needCrits = ((Integer) serializableEnum.nextElement()).intValue();
                int aktCrits = ((Integer) serializableEnum.nextElement()).intValue();
                buff.append(m.getType().getInternalName()).append(" has ")
                .append(needCrits).append(" Slots, but ").append(
                        aktCrits).append(" Slots are allocated!")
                        .append("\n");
            }
            correct = false;
        }
        if (!heatSinks.isEmpty()) {
            int sinks = heatSinks.elements().nextElement().intValue();
            buff.append(sinks).append(" of ").append(
                    engine.integralHeatSinkCapacity()).append(
                    " possible Internal Heat Sinks!").append("\n");
            correct = false;
        }
        if (!checkSystemCriticals(buff)) {
            correct = false;
        }

        return correct;
    }

    private boolean checkSystemCriticals(StringBuffer buff) {
        // Engine criticals
        boolean engineCorrect = true;
        int requiredSideCrits = engine.getSideTorsoCriticalSlots().length;
        if ((requiredSideCrits != mech.getNumberOfCriticals(
                CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_ENGINE, Mech.LOC_LT))
                || (requiredSideCrits != mech.getNumberOfCriticals(
                        CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_ENGINE,
                        Mech.LOC_RT))) {
            engineCorrect = false;
        }
        if (engine.getCenterTorsoCriticalSlots(mech.getGyroType()).length != mech
                .getNumberOfCriticals(CriticalSlot.TYPE_SYSTEM,
                        Mech.SYSTEM_ENGINE, Mech.LOC_CT)) {
            engineCorrect = false;
        }
        if (!engineCorrect) {
            buff.append("Engine: Incorrect number of criticals allocated.\n");
        }

        if (!engineCorrect) {
            return false;
        }

        return true;
    }

    public String printArmorLocProp(int loc, int wert) {
        return " is greater then " + Integer.toString(wert) + "!";
    }

    public boolean correctArmor(StringBuffer buff) {
        boolean correct = true;
        for (int loc = 0; loc < mech.locations(); loc++) {
            if (loc == Mech.LOC_HEAD) {
                if (mech.getOArmor(Mech.LOC_HEAD) > 9) {
                    buff.append(printArmorLocation(Mech.LOC_HEAD)).append(
                            printArmorLocProp(Mech.LOC_HEAD, 9)).append("\n");
                    correct = false;
                }

            } else if ((mech.getOArmor(loc) + (mech.hasRearArmor(loc) ? mech
                    .getOArmor(loc, true) : 0)) > 2 * mech.getOInternal(loc)) {
                buff.append(printArmorLocation(loc)).append(
                        printArmorLocProp(loc, 2 * mech.getOInternal(loc)))
                        .append("\n");
                correct = false;
            }
        }

        return correct;
    }

    public boolean correctMovement(StringBuffer buff) {
        // Mechanical Jump Boosts can be greater then Running as long as
        // the unit can handle the weight.
        if ((mech.getJumpMP(false) > mech.getOriginalRunMPwithoutMASC())
                && !mech.hasJumpBoosters()) {
            buff.append("Jump MP exceeds run MP\n");
            return false;
        }
        if ((mech.getJumpMP(false) > mech.getOriginalWalkMP()) &&
               (mech.getJumpType() != Mech.JUMP_IMPROVED)) {
          buff.append("Jump MP exceeds walk MP without IJJs");
          return false;
        }
        return true;
    }

    @Override
    public boolean correctEntity(StringBuffer buff) {
        return correctEntity(buff, true);
    }

    @Override
    public boolean correctEntity(StringBuffer buff, boolean ignoreAmmo) {
        boolean correct = true;
        if (skip()) {
            return true;
        }
        if (!correctWeight(buff)) {
            buff.insert(0, printTechLevel() + printShortMovement());
            buff.append(printWeightCalculation());
            correct = false;
        }
        if (!engine.engineValid) {
            buff.append(engine.problem.toString()).append("\n\n");
            correct = false;
        }
        if (getCountHeatSinks() < engine.getWeightFreeEngineHeatSinks()) {
            buff.append("Heat Sinks:\n");
            buff.append(" Engine    "+engine.integralHeatSinkCapacity()+"\n");
            buff.append(" Total     "+getCountHeatSinks()+"\n");
            buff.append(" Required  "+engine.getWeightFreeEngineHeatSinks()+"\n");
            correct = false;
        }
        if (showCorrectArmor() && !correctArmor(buff)) {
            correct = false;
        }
        if (showCorrectCritical() && !correctCriticals(buff)) {
            correct = false;
        }
        if (showFailedEquip() && hasFailedEquipment(buff)) {
            correct = false;
        }
        if (hasIllegalTechLevels(buff, ignoreAmmo)) {
            correct = false;
        }
        if (hasIllegalEquipmentCombinations(buff)) {
            correct = false;
        }
        correct = correct && correctMovement(buff);
        return correct;
    }

    @Override
    public StringBuffer printEntity() {
        StringBuffer buff = new StringBuffer();
        buff.append("Mech: ").append(mech.getDisplayName()).append("\n");
        buff.append("Found in: ").append(fileString).append("\n");
        buff.append(printTechLevel());
        buff.append(printShortMovement());
        if (correctWeight(buff, true, true)) {
            buff.append("Weight: ").append(getWeight()).append(" (").append(
                    calculateWeight()).append(")\n");
        }
        buff.append(printWeightCalculation()).append("\n");
        buff.append(printArmorPlacement());
        correctArmor(buff);
        buff.append(printLocations());
        correctCriticals(buff);

        // printArmor(buff);
        printFailedEquipment(buff);
        return buff;
    }

    @Override
    public String getName() {
        return "Mech: " + mech.getDisplayName();
    }

    /**
     * calculates the total weight of all armored components.
     */
    @Override
    public float getArmoredComponentWeight() {
        float weight = 0.0f;

        for (int location = Mech.LOC_HEAD; location <= Mech.LOC_LLEG; location++) {
            for (int slot = 0; slot < mech.getNumberOfCriticals(location); slot++) {
                CriticalSlot cs = mech.getCritical(location, slot);
                if ((cs != null) && cs.isArmored()) {
                    weight += 0.5f;

                    if ((cs.getType() == CriticalSlot.TYPE_SYSTEM) && (cs.getIndex() == Mech.SYSTEM_COCKPIT)) {
                        weight += 0.5f;
                    }
                }
            }
        }
        return weight;
    }


}
