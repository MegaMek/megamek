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
import megamek.common.BipedMech;
import megamek.common.CriticalSlot;
import megamek.common.Engine;
import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.LandAirMech;
import megamek.common.Mech;
import megamek.common.MiscType;
import megamek.common.Mounted;
import megamek.common.TechConstants;
import megamek.common.WeaponType;
import megamek.common.util.StringUtil;
import megamek.common.weapons.ACWeapon;
import megamek.common.weapons.EnergyWeapon;
import megamek.common.weapons.GaussWeapon;
import megamek.common.weapons.LBXACWeapon;
import megamek.common.weapons.PPCWeapon;
import megamek.common.weapons.UACWeapon;

public class TestMech extends TestEntity {
    private Mech mech = null;

    public TestMech(Mech mech, TestEntityOption option, String fileString) {
        super(option, mech.getEngine(), getArmor(mech), getStructure(mech));
        this.mech = mech;
        this.fileString = fileString;
    }

    private static Structure getStructure(Mech mech) {
        int type = mech.getStructureType();
        return new Structure(type, mech.isSuperHeavy(), mech.getMovementMode());
    }

    private static Armor[] getArmor(Mech mech) {
        Armor[] armor;
        if (!mech.hasPatchworkArmor()) {
            armor = new Armor[1];
            int type = mech.getArmorType(1);
            int flag = 0;
            if (mech.isClanArmor(1)) {
                flag |= Armor.CLAN_ARMOR;
            }
            armor[0] = new Armor(type, flag);
            return armor;
        } else {
            armor = new Armor[mech.locations()];
            for (int i = 0; i < mech.locations(); i++) {
                int type = mech.getArmorType(i);
                int flag = 0;
                if (mech.isClanArmor(i)) {
                    flag |= Armor.CLAN_ARMOR;
                }
                armor[i] = new Armor(type, flag);
            }
        }
        return armor;
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
    public boolean isAero() {
        return false;
    }

    @Override
    public double getWeightMisc() {
        if (mech instanceof LandAirMech) {
            // 10% of weight is conversion equipment
            return Math.ceil(mech.getWeight() / 10);
        }
        return 0.0f;
    }

    @Override
    public double getWeightPowerAmp() {
        if (mech.isIndustrial()
                && ((mech.getEngine().getEngineType() == Engine.COMBUSTION_ENGINE) || (mech
                        .getEngine().getEngineType() == Engine.FUEL_CELL))) {
            double powerAmpWeight = 0;
            for (Mounted m : mech.getWeaponList()) {
                WeaponType wt = (WeaponType) m.getType();
                if (wt instanceof EnergyWeapon) {
                    powerAmpWeight += wt.getTonnage(mech);
                }
                if ((m.getLinkedBy() != null)
                        && (m.getLinkedBy().getType() instanceof MiscType)
                        && m.getLinkedBy().getType()
                                .hasFlag(MiscType.F_PPC_CAPACITOR)) {
                    powerAmpWeight += ((MiscType) m.getLinkedBy().getType())
                            .getTonnage(mech);
                }
            }
            return TestEntity.ceil(powerAmpWeight / 10f, getWeightCeilingPowerAmp());
        }
        return 0;
    }

    public double getWeightCockpit() {
        double weight = 3.0;
        if (mech.getCockpitType() == Mech.COCKPIT_SMALL) {
            weight = 2.0;
        } else if (mech.getCockpitType() == Mech.COCKPIT_TORSO_MOUNTED) {
            weight = 4.0;
        } else if (mech.getCockpitType() == Mech.COCKPIT_COMMAND_CONSOLE) {
            // Technically, it's two separate 3-ton pieces of equipment.
            // We're ignoring that and returning the total, because it's easier.
            weight = 6.0;
        } else if (mech.getCockpitType() == Mech.COCKPIT_DUAL) {
            // Solaris VII - The Game World (German) This is not actually
            // canonical as it
            // has never been repeated in any English language source including
            // Tech Manual
            weight = 4.0;
        } else if (mech.getCockpitType() == Mech.COCKPIT_PRIMITIVE) {
            weight = 5.0;
        } else if (mech.getCockpitType() == Mech.COCKPIT_PRIMITIVE_INDUSTRIAL) {
            weight = 5.0;
        } else if ((mech.getCockpitType() == Mech.COCKPIT_SUPERHEAVY) || (mech.getCockpitType() == Mech.COCKPIT_TRIPOD)) {
            weight = 4.0;
        } else if (mech.getCockpitType() == Mech.COCKPIT_SUPERHEAVY_TRIPOD) {
            weight = 5.0;
        } else if (mech.getCockpitType() == Mech.COCKPIT_INTERFACE) {
            weight = 4.0;
        }

        return weight;
    }

    public double getWeightGyro() {
        double retVal = Math.ceil(engine.getRating() / 100.0f);
        if (mech.getGyroType() == Mech.GYRO_XL) {
            retVal /= 2;
        } else if (mech.getGyroType() == Mech.GYRO_COMPACT) {
            retVal *= 1.5;
        } else if (mech.getGyroType() == Mech.GYRO_HEAVY_DUTY) {
            retVal *= 2;
        } else if (mech.getGyroType() == Mech.GYRO_NONE) {
            retVal = 0;
        }
        retVal = ceil(retVal, getWeightCeilingGyro());
        return retVal;
    }

    @Override
    public double getWeightControls() {
        return getWeightCockpit() + getWeightGyro();
    }

    @Override
    public int getCountHeatSinks() {
        return mech.heatSinks();
    }

    @Override
    public double getWeightHeatSinks() {
        boolean hasCompact = false;
        double compactHsTons = 0;
        for (Mounted misc : mech.getMisc()) {
            if (misc.getType().hasFlag(MiscType.F_COMPACT_HEAT_SINK)) {
                hasCompact = true;
                compactHsTons += misc.getType().getTonnage(mech);
            }
        }
        if (hasCompact) {
            return compactHsTons
                    - (engine.getWeightFreeEngineHeatSinks() * 1.5f);
        } else {
            return mech.heatSinks() - engine.getWeightFreeEngineHeatSinks();
        }
    }

    @Override
    public boolean hasDoubleHeatSinks() {
        return mech.hasDoubleHeatSinks();
    }

    @Override
    public String printWeightMisc() {
        return "";
    }

    @Override
    public String printWeightControls() {
        StringBuffer retVal = new StringBuffer(StringUtil.makeLength(
                mech.getCockpitTypeString() + ":", getPrintSize() - 5));
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

    public int countCriticalSlotsFromEquipInLocation(Entity entity, Mounted mount,
            int location) {
        int count = 0;
        for (int slots = 0; slots < entity.getNumberOfCriticals(location); slots++) {
            CriticalSlot slot = entity.getCritical(location, slots);
            if ((slot == null) || (slot.getType() == CriticalSlot.TYPE_SYSTEM)) {
                continue;
            } else if (slot.getType() == CriticalSlot.TYPE_EQUIPMENT) {
                if (slot.getMount().equals(mount)) {
                    count++;
                }
            } else {
                // Ignore this?
            }
        }
        return count;
    }

    public boolean checkMiscSpreadAllocation(Entity entity, Mounted mounted,
            StringBuffer buff) {
        MiscType mt = (MiscType) mounted.getType();
        if (mt.hasFlag(MiscType.F_STEALTH) && !entity.hasPatchworkArmor()) {
            if (!entity.hasWorkingMisc(MiscType.F_ECM)) {
                buff.append("stealth armor needs ECM suite\n");
                return false;
            }
            // stealth needs to have 2 crits in legs arm and side torso
            if (countCriticalSlotsFromEquipInLocation(entity, mounted,
                    Mech.LOC_LARM) != 2) {
                buff.append("incorrect number of stealth crits in left arm\n");
                return false;
            }
            if (countCriticalSlotsFromEquipInLocation(entity, mounted,
                    Mech.LOC_RARM) != 2) {
                buff.append("incorrect number of stealth crits in right arm\n");
                return false;
            }
            if (countCriticalSlotsFromEquipInLocation(entity, mounted,
                    Mech.LOC_LLEG) != 2) {
                buff.append("incorrect number of stealth crits in left leg\n");
                return false;
            }
            if (countCriticalSlotsFromEquipInLocation(entity, mounted,
                    Mech.LOC_RLEG) != 2) {
                buff.append("incorrect number of stealth crits in right leg\n");
                return false;
            }
            if (countCriticalSlotsFromEquipInLocation(entity, mounted, Mech.LOC_LT) != 2) {
                buff.append("incorrect number of stealth crits in left torso\n");
                return false;
            }
            if (countCriticalSlotsFromEquipInLocation(entity, mounted, Mech.LOC_RT) != 2) {
                buff.append("incorrect number of stealth crits in right torso\n");
                return false;
            }
        }
        if (mt.hasFlag(MiscType.F_DRONE_CONTROL_CONSOLE)) {
            if (mounted.getLocation() != Mech.LOC_HEAD) {
                buff.append("Drone Control Console must be mounted in head");
                return false;
            }
        }
        if (mt.hasFlag(MiscType.F_MOBILE_HPG)) {
            if ((countCriticalSlotsFromEquipInLocation(entity, mounted,
                    Mech.LOC_LARM) > 0)
                    || (countCriticalSlotsFromEquipInLocation(entity, mounted,
                            Mech.LOC_RARM) > 0)
                    || (countCriticalSlotsFromEquipInLocation(entity, mounted,
                            Mech.LOC_HEAD) > 0)
                    || (countCriticalSlotsFromEquipInLocation(entity, mounted,
                            Mech.LOC_LLEG) > 0)
                    || (countCriticalSlotsFromEquipInLocation(entity, mounted,
                            Mech.LOC_RLEG) > 0)) {
                buff.append("ground mobile HPG must be mounted in torso locations\n");
            }
        }
        if (mt.hasFlag(MiscType.F_ENVIRONMENTAL_SEALING)) {
            // environmental sealing needs to have 1 crit per location
            for (int locations = 0; locations < entity.locations(); locations++) {
                if (countCriticalSlotsFromEquipInLocation(entity, mounted,
                        locations) != 1) {
                    buff.append("not an environmental sealing crit in each location\n");
                    return false;
                }
            }
        }
        if (mt.hasFlag(MiscType.F_BLUE_SHIELD)) {
            // blue shield needs to have 1 crit per location, except head
            for (int locations = 0; locations < entity.locations(); locations++) {
                if (locations != Mech.LOC_HEAD) {
                    if (countCriticalSlotsFromEquipInLocation(entity, mounted,
                            locations) != 1) {
                        buff.append("not a blue shield crit in each location except the head\n");
                        return false;
                    }
                }

            }
        }
        if (mt.hasFlag(MiscType.F_PARTIAL_WING)) {
            // partial wing needs 3/4 crits in the side torsos
            if (countCriticalSlotsFromEquipInLocation(entity, mounted, Mech.LOC_LT) != ((TechConstants.isClan(mt
                    .getTechLevel(entity.getTechLevelYear()))) ? 3
                    : 4)) {
                buff.append("incorrect number of partial wing crits in left torso\n");
                return false;
            }
            if (countCriticalSlotsFromEquipInLocation(entity, mounted, Mech.LOC_RT) != ((TechConstants.isClan(mt
                    .getTechLevel(entity.getTechLevelYear()))) ? 3
                    : 4)) {
                buff.append("incorrect number of partial wing crits in right torso\n");
                return false;
            }
        }
        return true;
    }

    public boolean criticalSlotsAllocated(Entity entity, Mounted mounted,
            Vector<Serializable> allocation, StringBuffer buff) {
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
            for (int locations = 0; locations < entity.locations(); locations++) {
                count += countCriticalSlotsFromEquipInLocation(entity, mounted,
                        locations);
            }
        } else {
            count = countCriticalSlotsFromEquipInLocation(entity, mounted,
                    location);
        }

        if ((et instanceof WeaponType) && mounted.isSplit()) {
            int secCound = 0;
            for (int locations = 0; locations < entity.locations(); locations++) {
                if (locations == location) {
                    continue;
                }

                secCound = countCriticalSlotsFromEquipInLocation(entity, mounted,
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
            Vector<Integer> heatSinks, StringBuffer buff) {
        int countInternalHeatSinks = 0;
        for (Mounted m : entity.getEquipment()) {
            int loc = m.getLocation();
            if (loc == Entity.LOC_NONE) {
                if ((m.getType() instanceof AmmoType)
                        && (m.getUsableShotsLeft() <= 1)) {
                    continue;
                }
                if ((entity instanceof Mech) && (m.getType().getCriticals(entity) == 0)) {
                    continue;
                }
                if (!(m.getType() instanceof MiscType)) {
                    unallocated.addElement(m);
                    continue;
                }
                MiscType mt = (MiscType) m.getType();
                if (mt.hasFlag(MiscType.F_HEAT_SINK)
                        || mt.hasFlag(MiscType.F_DOUBLE_HEAT_SINK)
                        || mt.hasFlag(MiscType.F_IS_DOUBLE_HEAT_SINK_PROTOTYPE)) {
                    countInternalHeatSinks++;
                } else {
                    unallocated.addElement(m);
                    continue;
                }
            }
            // Check for illegal allocations
            if (entity.isOmni()
                    && (entity instanceof BipedMech)
                    && ((loc == Mech.LOC_LARM) || (loc == Mech.LOC_RARM))
                    && ((m.getType() instanceof GaussWeapon)
                            || (m.getType() instanceof ACWeapon)
                            || (m.getType() instanceof UACWeapon)
                            || (m.getType() instanceof LBXACWeapon) 
                            || (m.getType() instanceof PPCWeapon))) {
                String weapon = "";
                if (m.getType() instanceof GaussWeapon) {
                    weapon = "gauss rifles"; 
                } else if ((m.getType() instanceof ACWeapon) 
                        || (m.getType() instanceof UACWeapon)
                        || (m.getType() instanceof LBXACWeapon)) {
                    weapon = "autocannons";
                } else if (m.getType() instanceof PPCWeapon) {
                    weapon = "PPCs";
                }
                if (entity.hasSystem(Mech.ACTUATOR_LOWER_ARM, loc)
                        || entity.hasSystem(Mech.ACTUATOR_HAND, loc)) {
                    buff.append("Omni mechs with arm mounted " + weapon
                            + " cannot have lower armor or hand actuators!");
                }
            }
            
        }
        if ((countInternalHeatSinks > engine.integralHeatSinkCapacity(mech
                .hasCompactHeatSinks()))
                || ((countInternalHeatSinks < engine
                        .integralHeatSinkCapacity(mech.hasCompactHeatSinks()))
                        && (countInternalHeatSinks != ((Mech) entity)
                                .heatSinks()) && !entity.isOmni())) {
            heatSinks.addElement(new Integer(countInternalHeatSinks));
        }
    }

    public boolean correctCriticals(StringBuffer buff) {
        Vector<Mounted> unallocated = new Vector<Mounted>();
        Vector<Serializable> allocation = new Vector<Serializable>();
        Vector<Integer> heatSinks = new Vector<Integer>();
        checkCriticalSlotsForEquipment(mech, unallocated, allocation,
                heatSinks, buff);
        boolean correct = true;
        /*
         * StringBuffer critAlloc = new StringBuffer(); need to redo this, in
         * MML, spread equipment gets one mounted per block that needs to be
         * allocated for (Mounted m : mech.getEquipment()) { if
         * ((m.getLocation() != Entity.LOC_NONE) && (m.getType() instanceof
         * MiscType)) { if (!checkMiscSpreadAllocation(mech, m, critAlloc)) {
         * correct = false; buff.append(critAlloc.toString()); } } }
         */

        if (!unallocated.isEmpty()) {
            buff.append("Unallocated Equipment:\n");
            for (Mounted mount : unallocated) {
                buff.append(mount.getType().getInternalName()).append("\n");
            }
            correct = false;
        }
        if (!allocation.isEmpty()) {
            buff.append("Allocated Equipment:\n");
            for (Enumeration<Serializable> serializableEnum = allocation
                    .elements(); serializableEnum.hasMoreElements();) {
                Mounted mount = (Mounted) serializableEnum.nextElement();
                int needCrits = ((Integer) serializableEnum.nextElement())
                        .intValue();
                int aktCrits = ((Integer) serializableEnum.nextElement())
                        .intValue();
                buff.append(mount.getType().getInternalName()).append(" has ")
                        .append(needCrits).append(" Slots, but ")
                        .append(aktCrits).append(" Slots are allocated!")
                        .append("\n");
            }
            correct = false;
        }
        if (!heatSinks.isEmpty()) {
            int sinks = heatSinks.elements().nextElement().intValue();
            buff.append(sinks)
                    .append(" of ")
                    .append(engine.integralHeatSinkCapacity(mech
                            .hasCompactHeatSinks()))
                    .append(" possible Internal Heat Sinks!").append("\n");
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
        int requiredCTCrits = engine.getCenterTorsoCriticalSlots(mech.getGyroType()).length;
        if (requiredCTCrits != mech
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

        if (((getMech().getGyroType() == Mech.GYRO_NONE))
                ) {
            buff.append("Missing Gyro!.\n");
            return false;
        }

        return true;
    }

    public String printArmorLocProp(int loc, int wert) {
        return " is greater than " + Integer.toString(wert) + "!";
    }

    public boolean correctArmor(StringBuffer buff) {
        boolean correct = true;
        for (int loc = 0; loc < mech.locations(); loc++) {
            if (loc == Mech.LOC_HEAD) {
                if (((mech.getOArmor(Mech.LOC_HEAD) > 9) && !mech.isSuperHeavy()) || ((mech.getOArmor(Mech.LOC_HEAD) > 12) && mech.isSuperHeavy())) {
                    buff.append(printArmorLocation(Mech.LOC_HEAD))
                            .append(printArmorLocProp(Mech.LOC_HEAD, 9))
                            .append("\n");
                    correct = false;
                }

            } else if ((mech.getOArmor(loc) + (mech.hasRearArmor(loc) ? mech
                    .getOArmor(loc, true) : 0)) > (2 * mech.getOInternal(loc))) {
                buff.append(printArmorLocation(loc))
                        .append(printArmorLocProp(loc,
                                2 * mech.getOInternal(loc))).append("\n");
                correct = false;
            }
        }
/*
        if (getEntity().getLabTotalArmorPoints() < getEntity().getTotalOArmor()) {
            correct = false;
            buff.append("Too many armor points allocated");
        }
*/
        return correct;
    }

    public boolean correctMovement(StringBuffer buff) {
        // Mechanical Jump Boosts can be greater then Running as long as
        // the unit can handle the weight.
        if ((mech.getJumpMP(false) > mech.getOriginalRunMPwithoutMASC())
                && !mech.hasJumpBoosters()
                && !mech.hasWorkingMisc(MiscType.F_PARTIAL_WING)) {
            buff.append("Jump MP exceeds run MP\n");
            return false;
        }
        if ((mech.getJumpMP(false) > mech.getOriginalWalkMP())
                && (((mech.getJumpType() != Mech.JUMP_IMPROVED) && (mech.getJumpType() != Mech.JUMP_PROTOTYPE_IMPROVED))
                        && !mech.hasWorkingMisc(MiscType.F_PARTIAL_WING) && !mech
                            .hasJumpBoosters())) {
            buff.append("Jump MP exceeds walk MP without IJJs\n");
            return false;
        }
        return true;
    }

    @Override
    public boolean correctEntity(StringBuffer buff) {
        return correctEntity(buff, getEntity().getTechLevel());
    }

    @Override
    public boolean correctEntity(StringBuffer buff, int ammoTechLvl) {
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
            buff.append(" Engine    "
                    + engine.integralHeatSinkCapacity(mech
                            .hasCompactHeatSinks()) + "\n");
            buff.append(" Total     " + getCountHeatSinks() + "\n");
            buff.append(" Required  " + engine.getWeightFreeEngineHeatSinks()
                    + "\n");
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
        if (hasIllegalTechLevels(buff, ammoTechLvl)) {
            correct = false;
        }
        if (hasIllegalEquipmentCombinations(buff)) {
            correct = false;
        }
        for (Mounted misc : mech.getMisc()) {
            correct = correct && checkMiscSpreadAllocation(mech, misc, buff);
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
        buff.append(printSource());
        buff.append(printShortMovement());
        if (correctWeight(buff, true, true)) {
            buff.append("Weight: ").append(getWeight()).append(" (")
                    .append(calculateWeight()).append(")\n");
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
    public double getArmoredComponentWeight() {
        double weight = 0.0;

        for (int location = Mech.LOC_HEAD; location <= Mech.LOC_LLEG; location++) {
            for (int slot = 0; slot < mech.getNumberOfCriticals(location); slot++) {
                CriticalSlot cs = mech.getCritical(location, slot);
                if ((cs != null) && cs.isArmored()) {
                    weight += 0.5;

                    if ((cs.getType() == CriticalSlot.TYPE_SYSTEM)
                            && (cs.getIndex() == Mech.SYSTEM_COCKPIT)) {
                        weight += 0.5;
                    }
                }
            }
        }
        return weight;
    }

}
