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
        if (mech.isClan())
            flag |= Structure.CLAN_STRUCTURE;
        return new Structure(type, flag);
    }

    private static Armor getArmor(Mech mech) {
        int type = mech.getArmorType();
        int flag = 0;
        if (mech.isClanArmor())
            flag |= Armor.CLAN_ARMOR;
        return new Armor(type, flag);
    }

    public Entity getEntity() {
        return mech;
    }

    public boolean isTank() {
        return false;
    }

    public boolean isMech() {
        return true;
    }

    public float getWeightMisc() {
        return 0.0f;
    }

    public float getWeightCockpit() {
        if (mech.getCockpitType() == Mech.COCKPIT_SMALL) {
            return 2.0f;
        } else if (mech.getCockpitType() == Mech.COCKPIT_TORSO_MOUNTED) {
            return 4.0f;
        } else if (mech.getCockpitType() == Mech.COCKPIT_COMMAND_CONSOLE) {
            // Technically, it's two separate 3-ton pieces of equipment.
            // We're ignoring that and returning the total, because it's easier.
            return 6.0f;
        } else if (mech.getCockpitType() == Mech.COCKPIT_DUAL) {
            // This is wrong; I just don't remember the correct weight.
            // FIXME
            return 3.0f;
        } else {
            return 3.0f;
        }
    }

    public float getWeightGyro() {
        float retVal = ceil(engine.getRating() / 100.0f, getWeightCeilingGyro());
        if (mech.getGyroType() == Mech.GYRO_XL)
            retVal /= 2;
        else if (mech.getGyroType() == Mech.GYRO_COMPACT)
            retVal *= 1.5;
        else if (mech.getGyroType() == Mech.GYRO_HEAVY_DUTY)
            retVal *= 2;
        return retVal;
    }

    public float getWeightControls() {
        return getWeightCockpit() + getWeightGyro();
    }

    public int getCountHeatSinks() {
        return mech.heatSinks();
    }

    public int getWeightHeatSinks() {
        return mech.heatSinks() - engine.getCountEngineHeatSinks();
    }

    public boolean hasDoubleHeatSinks() {
        if (mech.heatSinks() != mech.getHeatCapacity())
            return true;
        return false;
    }

    public String printWeightMisc() {
        return "";
    }

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
            if (slot == null || slot.getType() == CriticalSlot.TYPE_SYSTEM) {
                continue;
            } else if (slot.getType() == CriticalSlot.TYPE_EQUIPMENT) {
                if (slot.getIndex() == eNum)
                    count++;
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
        } else
            criticals = et.getCriticals(entity);
        int count = 0;

        if (location == Entity.LOC_NONE)
            return true;

        if (et.isSpreadable() && !et.getName().equals("Targeting Computer")) {
            for (int locations = 0; locations < entity.locations(); locations++) {
                count += countCriticalSlotsFromEquipInLocation(entity, eNum,
                        locations);
            }
        } else
            count = countCriticalSlotsFromEquipInLocation(entity, eNum,
                    location);

        if (et instanceof WeaponType && mounted.isSplit()) {
            int secCound = 0;
            for (int locations = 0; locations < entity.locations(); locations++) {
                if (locations == location)
                    continue;

                secCound = countCriticalSlotsFromEquipInLocation(entity, eNum,
                        locations);
                if (secCound != 0
                        && location == Mech.mostRestrictiveLoc(locations,
                                location)) {
                    count += secCound;
                    break;
                }
            }
        }

        if (count == criticals)
            return true;

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
                if (m.getType() instanceof AmmoType && m.getShotsLeft() <= 1) {
                    continue;
                }
                if (!(m.getType() instanceof MiscType)) {
                    unallocated.addElement(m);
                    continue;
                }
                MiscType mt = (MiscType) m.getType();
                if (mt.hasFlag(MiscType.F_HEAT_SINK)
                        || mt.hasFlag(MiscType.F_DOUBLE_HEAT_SINK))
                    countInternalHeatSinks++;
                else {
                    unallocated.addElement(m);
                    continue;
                }

            } else if (!criticalSlotsAllocated(entity, m, allocation)) {
            }
        }
        if ((countInternalHeatSinks > engine.integralHeatSinkCapacity())
                || (countInternalHeatSinks < engine.integralHeatSinkCapacity()
                        && countInternalHeatSinks != ((Mech) entity)
                                .heatSinks() && !entity.isOmni())) {
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
            for (Enumeration<Mounted> e = unallocated.elements(); e
                    .hasMoreElements();) {
                Mounted m = e.nextElement();
                buff.append(m.getType().getInternalName()).append("\n");
            }
            correct = false;
        }
        if (!allocation.isEmpty()) {
            buff.append("Allocated Equipment:\n");
            for (Enumeration<Serializable> e = allocation.elements(); e
                    .hasMoreElements();) {
                Mounted m = (Mounted) e.nextElement();
                int needCrits = ((Integer) e.nextElement()).intValue();
                int aktCrits = ((Integer) e.nextElement()).intValue();
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
                        Mech.LOC_RT)))
            engineCorrect = false;
        if (engine.getCenterTorsoCriticalSlots().length != mech
                .getNumberOfCriticals(CriticalSlot.TYPE_SYSTEM,
                        Mech.SYSTEM_ENGINE, Mech.LOC_CT))
            engineCorrect = false;
        if (!engineCorrect)
            buff.append("Engine: Incorrect number of criticals allocated.\n");

        if (!engineCorrect)
            return false;

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
        if (mech.getJumpMP(false) > mech.getOriginalRunMPwithoutMASC()
                && !mech.hasJumpBoosters()) {
            buff.append("Jump MP exceeds run MP\n");
            return false;
        }
        if (mech.getJumpMP(false) > mech.getOriginalWalkMP() &&
               mech.getJumpType() != Mech.JUMP_IMPROVED) {
          buff.append("Jump MP exceeds walk MP without IJJs");
          return false;
        }
        return true;
    }

    public boolean correctEntity(StringBuffer buff) {
        return correctEntity(buff, true);
    }

    public boolean correctEntity(StringBuffer buff, boolean ignoreAmmo) {
        boolean correct = true;
        if (skip())
            return true;
        if (!correctWeight(buff)) {
            buff.insert(0, printTechLevel() + printShortMovement());
            buff.append(printWeightCalculation());
            correct = false;
        }
        if (!engine.engineValid) {
            buff.append(engine.problem.toString()).append("\n\n");
            correct = false;
        }
        if (getCountHeatSinks() < engine.getCountEngineHeatSinks()) {
            buff.append("Heat Sinks:\n");
            buff.append(" Engine    "+engine.integralHeatSinkCapacity()+"\n");
            buff.append(" Total     "+getCountHeatSinks()+"\n");
            buff.append(" Required  "+engine.getCountEngineHeatSinks()+"\n");
            correct = false;
        }
        if (showCorrectArmor() && !correctArmor(buff))
            correct = false;
        if (showCorrectCritical() && !correctCriticals(buff))
            correct = false;
        if (showFailedEquip() && hasFailedEquipment(buff))
            correct = false;
        if (hasIllegalTechLevels(buff, ignoreAmmo))
            correct = false;
        if (hasIllegalEquipmentCombinations(buff))
            correct = false;
        correct = correct && correctMovement(buff);
        return correct;
    }

    public StringBuffer printEntity() {
        StringBuffer buff = new StringBuffer();
        buff.append("Mech: ").append(mech.getDisplayName()).append("\n");
        buff.append("Found in: ").append(this.fileString).append("\n");
        buff.append(printTechLevel());
        buff.append(printShortMovement());
        if (correctWeight(buff, true, true))
            buff.append("Weight: ").append(getWeight()).append(" (").append(
                    calculateWeight()).append(")\n");
        buff.append(printWeightCalculation()).append("\n");
        buff.append(printArmorPlacement());
        correctArmor(buff);
        buff.append(printLocations());
        correctCriticals(buff);

        // printArmor(buff);
        printFailedEquipment(buff);
        return buff;
    }

    public String getName() {
        return "Mech: " + mech.getDisplayName();
    }
}
