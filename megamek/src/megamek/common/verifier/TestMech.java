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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.stream.Collectors;

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
import megamek.common.QuadMech;
import megamek.common.QuadVee;
import megamek.common.TechConstants;
import megamek.common.WeaponType;
import megamek.common.util.StringUtil;
import megamek.common.weapons.ACWeapon;
import megamek.common.weapons.ArtilleryWeapon;
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
        if (mech instanceof LandAirMech || mech instanceof QuadVee) {
            // 10% of weight is conversion equipment
            return Math.ceil(mech.getWeight() / 10);
        }
        return 0.0f;
    }

    @Override
    public double getWeightPowerAmp() {
        if (mech.isIndustrial()
                && (!mech.hasEngine()
                    || (mech.getEngine().getEngineType() == Engine.COMBUSTION_ENGINE)
                    || (mech.getEngine().getEngineType() == Engine.FUEL_CELL))) {
            double powerAmpWeight = 0;
            for (Mounted m : mech.getWeaponList()) {
                WeaponType wt = (WeaponType) m.getType();
                if (wt instanceof EnergyWeapon) {
                    powerAmpWeight += wt.getTonnage(mech);
                }
                if ((m.getLinkedBy() != null)
                        && (m.getLinkedBy().getType() instanceof MiscType)
                        && m.getLinkedBy().getType().hasFlag(MiscType.F_PPC_CAPACITOR)) {
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
        } else if (mech.getCockpitType() == Mech.COCKPIT_QUADVEE) {
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
        } else if ((mech.getGyroType() == Mech.GYRO_HEAVY_DUTY)
                || (mech.getGyroType() == Mech.GYRO_SUPERHEAVY)) {
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

    public boolean isCockpitLocation(int location) {
        if (mech.getCockpitType() == Mech.COCKPIT_TORSO_MOUNTED
                || mech.getCockpitType() == Mech.COCKPIT_VRRP) {
            return location == Mech.LOC_CT;
        }
        return location == Mech.LOC_HEAD;
    }
    
    public boolean isEngineLocation(int location) {
        return mech.hasSystem(Mech.SYSTEM_ENGINE, location);
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

    public boolean checkCriticalSlotsForEquipment(Entity entity,
            Vector<Mounted> unallocated, Vector<Serializable> allocation,
            Vector<Integer> heatSinks, StringBuffer buff) {
        boolean legal = true;
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
                            + " cannot have lower armor or hand actuators!\n");
                    legal = false;
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
        return legal;
    }

    public boolean correctCriticals(StringBuffer buff) {
        Vector<Mounted> unallocated = new Vector<Mounted>();
        Vector<Serializable> allocation = new Vector<Serializable>();
        Vector<Integer> heatSinks = new Vector<Integer>();
        boolean correct = checkCriticalSlotsForEquipment(mech, unallocated, allocation,
                heatSinks, buff);
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

        if (getMech().getGyroType() == Mech.GYRO_NONE
                && getMech().getCockpitType() != Mech.COCKPIT_INTERFACE) {
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
        if ((mech instanceof LandAirMech) && mech.getJumpMP(false) < 3) {
            buff.append("LAMs must have at least 3 jumping MP.\n");
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

    /**
     * Check if the unit has combinations of equipment which are not allowed in
     * the construction rules.
     *
     * @param buff
     *            diagnostics are appended to this
     * @return true if the entity is illegal
     */
    @Override
    public boolean hasIllegalEquipmentCombinations(StringBuffer buff) {
        boolean illegal = super.hasIllegalEquipmentCombinations(buff);
        
        boolean hasStealth = mech.hasStealth();
        boolean hasC3 = mech.hasC3();
        boolean hasHarjelII = false;
        boolean hasHarjelIII = false;
        boolean hasNullSig = false;
        boolean hasVoidSig = false;
        boolean hasTC = false;
        boolean hasMASC = false;
        boolean hasAES = false;
        EquipmentType advancedMyomer = null;
        
        //First we find all the equipment that is required or incompatible with other equipment,
        //so we don't have to execute another loop each time one of those situations comes up.
        for (Mounted m : mech.getMisc()) {
            hasHarjelII |= m.getType().hasFlag(MiscType.F_HARJEL_II);
            hasHarjelIII |= m.getType().hasFlag(MiscType.F_HARJEL_III);
            hasNullSig |= m.getType().hasFlag(MiscType.F_NULLSIG);
            hasVoidSig |= m.getType().hasFlag(MiscType.F_VOIDSIG);
            hasTC |= m.getType().hasFlag(MiscType.F_TARGCOMP);
            hasMASC |= m.getType().hasFlag(MiscType.F_MASC)
                    && !m.getType().hasSubType(MiscType.S_SUPERCHARGER);
            hasAES |= m.getType().hasFlag(MiscType.F_ACTUATOR_ENHANCEMENT_SYSTEM);
            if (m.getType().hasFlag(MiscType.F_TSM)
                    || m.getType().hasFlag(MiscType.F_INDUSTRIAL_TSM)
                    || m.getType().hasFlag(MiscType.F_SCM)) {
                advancedMyomer = m.getType();
            }
                
        }
        
        for (Mounted m : getEntity().getMisc()) {
            final MiscType misc = (MiscType)m.getType();

            if (misc.hasFlag(MiscType.F_JUMP_JET)
                    && m.getLocation() != Mech.LOC_CT
                    && m.getLocation() != Mech.LOC_RT
                    && m.getLocation() != Mech.LOC_LT
                    && !mech.locationIsLeg(m.getLocation())) {
                buff.append("Jump jet must be mounted in leg or torso\n");
                illegal = true;
            }
            
            if (misc.hasFlag(MiscType.F_UMU) && (mech.getJumpType() != Mech.JUMP_NONE)
                    && (mech.getJumpType() != Mech.JUMP_BOOSTER)) {
                illegal = true;
                buff.append("UMUs cannot be mounted with jump jets "
                        + "(jump boosters are legal)\n");
            }

            if (misc.hasFlag(MiscType.F_MASC)
                    && misc.hasSubType(MiscType.S_SUPERCHARGER)) {
                if (!isEngineLocation(m.getLocation())) {
                    buff.append("supercharger in location without engine\n");
                    illegal = true;
                }
                if (mech instanceof LandAirMech) {
                    buff.append("LAMs may not mount a supercharger\n");
                    illegal = true;
                }
            }
            
            if ((misc.hasFlag(MiscType.F_TSM)
                    || misc.hasFlag(MiscType.F_INDUSTRIAL_TSM)
                    || misc.hasFlag(MiscType.F_SCM))
                    && (misc != advancedMyomer)) {
                buff.append("Cannot mount more than one type of myomer.\n");
                illegal = true;
            }
            
            if (misc.hasFlag(MiscType.F_AP_POD)
                    && !mech.locationIsLeg(m.getLocation())) {
                buff.append("A-Pod must be mounted in leg\n");
                illegal = true;
            }

            if (misc.hasFlag(MiscType.F_REMOTE_DRONE_COMMAND_CONSOLE)) {
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

            if (misc.hasFlag(MiscType.F_CHAMELEON_SHIELD)) {
                if (hasStealth) {
                    buff.append("Unit mounts both chameleon-light-polarization-system and stealth armor\n");
                    illegal = true;
                }
                if (hasVoidSig) {
                    buff.append("Unit mounts both void-signature-system and a chameleon light polarisation shield\n");
                    illegal = true;
                }
            }

            if (m.getType().hasFlag(MiscType.F_EMERGENCY_COOLANT_SYSTEM)
                    && !isEngineLocation(m.getLocation())) {
                buff.append("RISC emergency coolant system must be mounted in location with engine crit\n");
                illegal = true;
            }
            
            if (misc.hasFlag(MiscType.F_LIGHT_FLUID_SUCTION_SYSTEM)
                    && !mech.isIndustrial()) {
                illegal = true;
                buff.append("BattleMech can't mount light fluid suction system\n");
            }

            if (misc.hasFlag(MiscType.F_SALVAGE_ARM)
                    || (misc.hasFlag(MiscType.F_CLUB)
                            && (misc.hasSubType(MiscType.S_CHAINSAW)
                                    || misc.hasSubType(MiscType.S_BACKHOE)
                                    || misc.hasSubType(MiscType.S_DUAL_SAW)
                                    || misc.hasSubType(MiscType.S_PILE_DRIVER)
                                    || misc.hasSubType(MiscType.S_MINING_DRILL)
                                    || misc.hasSubType(MiscType.S_ROCK_CUTTER)
                                    || misc.hasSubType(MiscType.S_SPOT_WELDER)
                                    || misc.hasSubType(MiscType.S_WRECKING_BALL)
                                    || misc.hasSubType(MiscType.S_COMBINE)))) {
                if (mech.entityIsQuad()) {
                    if (m.getLocation() != Mech.LOC_LT && m.getLocation() != Mech.LOC_RT) {
                        illegal = true;
                        buff.append("Quad Mechs can only mount " + misc.getName() + " in side torso location.\n");
                    }
                } else {
                    if ((m.getLocation() != Mech.LOC_LARM && m.getLocation() != Mech.LOC_RARM)
                            || mech.hasSystem(Mech.ACTUATOR_HAND, m.getLocation())) {
                        illegal = true;
                        buff.append("Mech can only mount " + misc.getName() + " in arm with no hand actuator.\n");
                    }
                }
            }
            
            if ((misc.hasFlag(MiscType.F_HARJEL)
                    || misc.hasFlag(MiscType.F_HARJEL_II)
                    || misc.hasFlag(MiscType.F_HARJEL_III))
                    && isCockpitLocation(m.getLocation())) {
                illegal = true;
                buff.append("Harjel can't be mounted in a location with a cockpit!\n");                    
            }
            
            if (misc.hasFlag(MiscType.F_MASS) && !isCockpitLocation(m.getLocation())) {
                illegal = true;
                buff.append("MW aquatic survival system must be located in the same location as the cockpit.\n");
            }

            if (misc.hasFlag(MiscType.F_MODULAR_ARMOR)
                    && (m.getLocation() == Mech.LOC_HEAD)) {
                illegal = true;
                buff.append("Unable to load Modular Armor in Head location\n");
            }

            if (misc.hasFlag(MiscType.F_HEAD_TURRET)
                    && isCockpitLocation(Mech.LOC_HEAD)) {
                illegal = true;
                buff.append("head turret requires torso mounted cockpit\n");
            }

            if (misc.hasFlag(MiscType.F_SHOULDER_TURRET) && mech instanceof QuadMech) {
                illegal = true;
                buff.append("quad mechs can't mount shoulder turrets\n");
            }
            
            if (misc.hasFlag(MiscType.F_SHOULDER_TURRET)) {
                if (m.getLocation() != Mech.LOC_RT
                        && m.getLocation() != Mech.LOC_LT) {
                    illegal = true;
                    buff.append("shoulder turret must be mounted in side torso\n");

                    if (mech.countWorkingMisc(MiscType.F_SHOULDER_TURRET, m.getLocation()) > 1) {
                        illegal = true;
                        buff.append("max of 1 shoulder turret per side torso\n");
                    }
                }
            }

            if (misc.hasFlag(MiscType.F_TRACKS)) {
                if (mech instanceof QuadVee) {
                    if (misc.hasSubType(MiscType.S_QUADVEE_WHEELS)
                            != (((QuadVee)mech).getMotiveType() == QuadVee.MOTIVE_WHEEL)) {
                        illegal = true;
                        buff.append("Motive equipment does not match QuadVee motive type.\n");
                    }
                } else if (misc.hasSubType(MiscType.S_QUADVEE_WHEELS)) {
                    illegal = true;
                    buff.append("Wheels can only be used on QuadVees.\n");
                }
                if (!mech.locationIsLeg(m.getLocation())) {
                    illegal = true;
                    buff.append(misc.getName() + " are only legal in the Legs\n");
                }
                for (int loc = 0; loc < mech.locations(); loc++) {
                    if (mech.locationIsLeg(loc)
                            && countCriticalSlotsFromEquipInLocation(mech, m, loc) != 1) {
                        illegal = true;
                        buff.append(misc.getName() + " require one critical slot in each leg.\n");
                        break;
                    }
                }
            }
            
            if (m.getType().hasFlag(MiscType.F_TALON)) {
                if (!mech.locationIsLeg(m.getLocation())) {
                    illegal = true;
                    buff.append("Talons are only legal in the Legs\n");
                }
                for (int loc = 0; loc < mech.locations(); loc++) {
                    if (mech.locationIsLeg(loc)
                            && countCriticalSlotsFromEquipInLocation(mech, m, loc) != 2) {
                        illegal = true;
                        buff.append("Talons require two critical slots in each leg.\n");
                        break;
                    }
                }
            }

            if (mech.isSuperHeavy()
                    && (misc.hasFlag(MiscType.F_TSM)
                            || misc.hasFlag(MiscType.F_INDUSTRIAL_TSM)
                            || misc.hasFlag(MiscType.F_SCM)
                            || misc.hasFlag(MiscType.F_MASC)
                            || misc.hasFlag(MiscType.F_JUMP_JET)
                            || misc.hasFlag(MiscType.F_MECHANICAL_JUMP_BOOSTER)
                            || misc.hasFlag(MiscType.F_UMU)
                            || misc.hasFlag(MiscType.F_ACTUATOR_ENHANCEMENT_SYSTEM)
                            || misc.hasFlag(MiscType.F_MODULAR_ARMOR)
                            || misc.hasFlag(MiscType.F_PARTIAL_WING))) {
                buff.append("Superheavy may not mount " + m.getType().getName() + "\n");
                illegal = true;
            }
            
            if (mech.isIndustrial()) {
                if (misc.hasFlag(MiscType.F_TSM)
                        || misc.hasFlag(MiscType.F_SCM)
                        || (misc.hasFlag(MiscType.F_MASC) && !misc.hasSubType(MiscType.S_SUPERCHARGER))) {
                    buff.append("industrial mech can't mount " + misc.getName() + "\n");
                    illegal = true;                    
                }
                if ((mech.getCockpitType() == Mech.COCKPIT_INDUSTRIAL
                        || mech.getCockpitType() == Mech.COCKPIT_PRIMITIVE_INDUSTRIAL)
                    && (misc.hasFlag(MiscType.F_TARGCOMP)
                        || misc.hasFlag(MiscType.F_ARTEMIS)
                        || misc.hasFlag(MiscType.F_ARTEMIS_V)
                        || misc.hasFlag(MiscType.F_BAP))) {
                    buff.append("Industrial mech without advanced fire control can't mount " + misc.getName() + "\n");
                    illegal = true;                    
                }
            } else {
                if (misc.hasFlag(MiscType.F_INDUSTRIAL_TSM)
                        || misc.hasFlag(MiscType.F_ENVIRONMENTAL_SEALING)) {
                    buff.append("Non-industrial mech can't mount " + misc.getName() + "\n");
                }
            }
            
            if ((mech instanceof LandAirMech)
                    && (misc.hasFlag(MiscType.F_MODULAR_ARMOR)
                            || misc.hasFlag(MiscType.F_JUMP_BOOSTER)
                            || misc.hasFlag(MiscType.F_PARTIAL_WING)
                            || misc.hasFlag(MiscType.F_DUMPER)
                            || misc.hasFlag(MiscType.F_HEAVY_BRIDGE_LAYER)
                            || misc.hasFlag(MiscType.F_MEDIUM_BRIDGE_LAYER)
                            || misc.hasFlag(MiscType.F_LIGHT_BRIDGE_LAYER)
                            || (misc.hasFlag(MiscType.F_CLUB)
                                    && (misc.getSubType() == MiscType.S_BACKHOE)
                                    || (misc.getSubType() == MiscType.S_COMBINE)))) {
                buff.append("LAMs may not mount ").append(misc.getName()).append("\n");
                illegal = true;
            }
        }
        
        if (mech.isSuperHeavy()) {
            switch (mech.hasEngine()? mech.getEngine().getEngineType() : Engine.NONE) {
            case Engine.NORMAL_ENGINE:
            case Engine.LARGE_ENGINE:
                break;
            case Engine.XL_ENGINE:
            case Engine.XXL_ENGINE:
            case Engine.COMPACT_ENGINE:
            case Engine.LIGHT_ENGINE:
                if (mech.isIndustrial()) {
                    buff.append("Superheavy industrialMechs can only use standard or large fusion engine\n");
                    illegal = true;
                }
                break;
            default:
                buff.append("Superheavy Mechs must use some type of fusion engine\n");
                illegal = true;
            }
            
            if (mech.getArmoredComponentBV() > 0) {
                buff.append("Superheavy Mechs cannot have armored components\n");
                illegal = true;
            }
            
            if (mech instanceof QuadVee) {
                buff.append("QuadVees cannot be constructed as superheavies.\n");
            }
        }
        
        if (mech.isIndustrial()) {
            if ((mech.getCockpitType() == Mech.COCKPIT_INDUSTRIAL
                    || mech.getCockpitType() == Mech.COCKPIT_PRIMITIVE_INDUSTRIAL) && hasC3) {
                buff.append("industrial mech without advanced fire control can't use c3 computer\n");
                illegal = true;
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
            if(mech.hasEngine() && ((mech.getEngine().getEngineType() == Engine.XL_ENGINE)
                    || (mech.getEngine().getEngineType() == Engine.LIGHT_ENGINE)
                    || (mech.getEngine().getEngineType() == Engine.COMPACT_ENGINE)
                    || mech.getEngine().hasFlag(Engine.LARGE_ENGINE)
                    || (mech.getEngine().getEngineType() == Engine.XXL_ENGINE))) {
                buff.append("primitive mechs can't mount XL, Light, Compact, XXL or Large Engines\n");
                illegal = true;
            }
            if (advancedMyomer != null) {
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
        
        if (mech instanceof LandAirMech) {
            if (mech.isOmni()) {
                buff.append("LAMs may not be constructed as omnis\n");
                illegal = true;
            }
            if (mech.getWeight() > 55) {
                buff.append("LAMs cannot be larger than 55 tons.\n");
                illegal = true;
            }
            EquipmentType structure = EquipmentType.get(EquipmentType.getStructureTypeName(mech.getStructureType(),
                    mech.isClan()));
            if (structure.getCriticals(mech) > 0) {
                buff.append("LAMs may not use ").append(structure.getName()).append("\n");
                illegal = true;
            }
            
            Set<Integer> ats = new HashSet<>();
            for (int i = 0; i < mech.locations(); i++) {
                ats.add(mech.getArmorType(i));
            }
            for (int at : ats) {
                if (at == EquipmentType.T_ARMOR_HARDENED) {
                    buff.append("LAMs cannot use hardened armor.\n");
                    illegal = true;
                } else {
                    final EquipmentType eq = EquipmentType.get(EquipmentType.getArmorTypeName(at, mech.isClan()));
                    if (eq != null && eq.getCriticals(mech) > 0) {
                        buff.append("LAMs cannot use ").append(eq.getName()).append("\n");
                        illegal = true;
                    }
                }
            }
            if (mech.countWorkingMisc(MiscType.F_BOMB_BAY) > 20) {
                buff.append("A LAM has a maximum of 20 bomb bays.\n");
                illegal = true;
            }
            if (isCockpitLocation(Mech.LOC_CT)) {
                buff.append("LAMs may not use torso-mounted cockpits.\n");
                illegal = true;
            }
            if (mech.getNumberOfCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_COCKPIT, Mech.LOC_HEAD) > 1) {
                buff.append("LAMs may not cockpits that require multiple critical slots.\n");
                illegal = true;
            }
            if (mech.getCockpitType() == Mech.COCKPIT_PRIMITIVE
                    || mech.getCockpitType() == Mech.COCKPIT_PRIMITIVE_INDUSTRIAL) {
                illegal = true;
            }
            if (mech.getGyroType() != Mech.GYRO_STANDARD
                    && mech.getGyroType() != Mech.GYRO_COMPACT
                    && mech.getGyroType() != Mech.GYRO_HEAVY_DUTY) {
                buff.append("LAMs may not use ").append(Mech.getGyroDisplayString(mech.getGyroType()))
                .append("\n");
                illegal = true;
            }
            if (mech.getEngine().getEngineType() != Engine.NORMAL_ENGINE
                    && mech.getEngine().getEngineType() != Engine.COMPACT_ENGINE) {
                buff.append("LAMs may only use standard or compact fusion engines.\n");
                illegal = true;
            }
            
            Map<EquipmentType,Set<Integer>> spread = new HashMap<>();
            for (Mounted m : mech.getEquipment()) {
                if (m.isSplit()) {
                    buff.append("Cannot split ").append(m.getType().getName())
                        .append(" between locations");
                    illegal = true;
                } else if (m.getType() instanceof ArtilleryWeapon) {
                    buff.append("LAMs cannot mount artillery weapons.\n");
                    illegal = true;
                } else if (m.getType() instanceof WeaponType
                        && ((((WeaponType)m.getType()).getAmmoType() == AmmoType.T_GAUSS_HEAVY)
                                || (((WeaponType)m.getType()).getAmmoType() == AmmoType.T_IGAUSS_HEAVY))) {
                    buff.append("LAMs cannot mount heavy gauss rifles.\n");
                    illegal = true;
                } else if (m.getType().isSpreadable()) {
                    if (spread.containsKey(m.getType())) {
                        spread.get(m.getType()).add(m.getLocation());
                    } else {
                        spread.put(m.getType(), new HashSet<>());
                        spread.get(m.getType()).add(m.getLocation());
                    }
                }
            }
            for (EquipmentType et : spread.keySet()) {
                if (spread.get(et).size() > 1) {
                    buff.append(et.getName()).append(" must be allocated to a single location.\n");
                    illegal = true;
                }
            }
            if (!mech.hasSystem(Mech.ACTUATOR_LOWER_ARM, Mech.LOC_RARM)
                    || !mech.hasSystem(Mech.ACTUATOR_LOWER_ARM, Mech.LOC_LARM)
                    || !mech.hasSystem(Mech.ACTUATOR_UPPER_ARM, Mech.LOC_RARM)
                    || !mech.hasSystem(Mech.ACTUATOR_UPPER_ARM, Mech.LOC_LARM)) {
                buff.append("LAMs require upper and lower arm actuators in both arms.\n");
                illegal = true;
            }
        }
        
        //Make sure all base chassis heat sinks are allocated
        if (mech.isOmni()) {
            int total = 0;
            int allocated = 0;
            boolean compact = false;
            for (Mounted m : mech.getMisc()) {
                if (m.getType().hasFlag(MiscType.F_HEAT_SINK)
                            || m.getType().hasFlag(MiscType.F_DOUBLE_HEAT_SINK)
                            || m.getType().hasFlag(MiscType.F_IS_DOUBLE_HEAT_SINK_PROTOTYPE)) {
                    total++;
                    compact |= m.getType().hasFlag(MiscType.F_COMPACT_HEAT_SINK);
                    if (m.getLocation() != Entity.LOC_NONE) {
                        allocated++;
                    }
                }
            }
            int required = total - (mech.isOmni()?
                    mech.getEngine().getBaseChassisHeatSinks(compact) :
                        mech.getEngine().integralHeatSinkCapacity(compact));
            if (allocated < required) {
                illegal = true;
                buff.append("Only " + allocated + " of the required " + required + " heat sinks are allocated to critical slots.");
            }
        }
        
        if (hasMASC && advancedMyomer != null) {
            buff.append("MASC is incompatible with " + advancedMyomer.getName() + "\n");
            illegal = true;
        }
        
        if (hasAES) {
            if (hasMASC) {
                buff.append("AES is incompatible with MASC.\n");
                illegal = true;
            }
            if (hasTC) {
                buff.append("AES is incompatible with Targeting computers.\n");
                illegal = true;
            }
            if (advancedMyomer != null) {
                buff.append("AES is incompatible with advanced myomers.\n");
            }
            //Find all locations with an AES and map the number in that location to the location index.
            Map<Integer,Long> byLocation = mech.getMisc().stream()
                    .filter(m -> m.getType().hasFlag(MiscType.F_ACTUATOR_ENHANCEMENT_SYSTEM))
                    .collect(Collectors.groupingBy(Mounted::getLocation, Collectors.counting()));
            boolean multiple = false;
            boolean wrongLocation = false;
            int legCount = 0;
            for (Integer loc : byLocation.keySet()) {
                if (mech.locationIsLeg(loc)) {
                    legCount++;
                } else if (loc == Mech.LOC_HEAD
                        || loc == Mech.LOC_CT
                        || loc == Mech.LOC_LT
                        || loc == Mech.LOC_RT) {
                    wrongLocation = true;
                }
                multiple |= byLocation.get(loc) > 1;
            }
            if (multiple) {
                buff.append("Only one AES can be mounted in a single location.\n");
                illegal = true;
            }
            if (wrongLocation) {
                buff.append("AES can only be mounted in an arm or leg location.\n");
                illegal = true;
            }
            if (legCount > 0) {
                for (int loc = 0; loc < mech.locations(); loc++) {
                    if (mech.locationIsLeg(loc)) {
                        legCount--;
                    }
                }
                if (legCount < 0) {
                    buff.append("If an AES is mounted in a leg, all legs must mount one.\n");
                    illegal = true;
                }
            }
        }
        
        if (hasNullSig) {
            if (hasStealth) {
                buff.append("Unit mounts both null-signature-system and stealth armor\n");
                illegal = true;
            }
            if (hasTC) {
                buff.append("Unit mounts both null-signature-system and targeting computer\n");
                illegal = true;
            }
            if (hasVoidSig) {
                buff.append("Unit mounts both null-signature-system and void-signature-system\n");
                illegal = true;
            }
            if (hasC3) {
                buff.append("Unit mounts both null-signature-system and a c3 system\n");
                illegal = true;
            }
        }

        if (hasVoidSig) {
            if (hasStealth) {
                buff.append("Unit mounts both void-signature-system and stealth armor\n");
                illegal = true;
            }
            if (hasTC) {
                buff.append("Unit mounts both void-signature-system and targeting computer\n");
                illegal = true;
            }
            if (hasC3) {
                buff.append("Unit mounts both void-signature-system and a c3 system\n");
                illegal = true;
            }
        }

        if (hasHarjelII && hasHarjelIII) {
            illegal = true;
            buff.append("Can't mix HarJel II and HarJel III\n");
        }

        if (hasHarjelII || hasHarjelIII) {
            if (mech.isIndustrial()) {
                buff.append("Cannot mount HarJel repair system on IndustrialMech\n");
                illegal = true;
            }
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
            switch (mech.hasEngine() ? mech.getEngine().getEngineType() : Engine.NONE) {
                case Engine.FISSION:
                case Engine.FUEL_CELL:
                case Engine.COMBUSTION_ENGINE:
                case Engine.NONE:
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
        
        return illegal;
    }
}
