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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.stream.Collectors;

import megamek.common.CriticalSlot;
import megamek.common.MPCalculationSetting;
import megamek.common.SimpleTechLevel;
import megamek.common.TechConstants;
import megamek.common.annotations.Nullable;
import megamek.common.equipment.*;
import megamek.common.equipment.enums.MiscTypeFlag;
import megamek.common.interfaces.ITechManager;
import megamek.common.options.OptionsConstants;
import megamek.common.units.BipedMek;
import megamek.common.units.Entity;
import megamek.common.units.LandAirMek;
import megamek.common.units.Mek;
import megamek.common.units.QuadMek;
import megamek.common.units.QuadVee;
import megamek.common.util.StringUtil;
import megamek.common.weapons.artillery.ArtilleryWeapon;
import megamek.common.weapons.autoCannons.ACWeapon;
import megamek.common.weapons.autoCannons.LBXACWeapon;
import megamek.common.weapons.autoCannons.UACWeapon;
import megamek.common.weapons.gaussRifles.GaussWeapon;
import megamek.common.weapons.lasers.EnergyWeapon;
import megamek.common.weapons.ppc.PPCWeapon;

/**
 * @author Reinhard Vicinus
 */
public class TestMek extends TestEntity {

    /**
     * Filters all mek armor according to given tech constraints
     *
     * @param entityType  Entity type bitmap
     * @param industrial  Whether to include IndustrialMek armors
     * @param techManager The tech manager that determines legality
     *
     * @return A list of legal armors for the mek
     */
    public static List<ArmorType> legalArmorsFor(long entityType, boolean industrial, ITechManager techManager) {
        List<ArmorType> legalArmors = new ArrayList<>();
        boolean industrialOnly = industrial
              && (techManager.getTechLevel().ordinal() < SimpleTechLevel.EXPERIMENTAL.ordinal());
        boolean isLam = (entityType & Entity.ETYPE_LAND_AIR_MEK) != 0;
        for (ArmorType armor : ArmorType.allArmorTypes()) {
            if ((armor.getArmorType() == EquipmentType.T_ARMOR_PATCHWORK)
                  || (isLam && (armor.getArmorType() == EquipmentType.T_ARMOR_HARDENED))) {
                continue;
            }
            if (armor.hasFlag(MiscType.F_MEK_EQUIPMENT)
                  && ((armor.getArmorType() != EquipmentType.T_ARMOR_COMMERCIAL) || industrial)
                  && techManager.isLegal(armor)
                  && (!isLam || (armor.getNumCriticalSlots(null) == 0))
                  && (!industrialOnly || armor.isIndustrial())) {
                legalArmors.add(armor);
            }
        }
        return legalArmors;
    }

    private final Mek mek;

    public TestMek(Mek mek, TestEntityOption option, String fileString) {
        super(option, mek.getEngine(), getStructure(mek));
        this.mek = mek;
        this.fileString = fileString;
    }

    private static Structure getStructure(Mek mek) {
        int type = mek.getStructureType();
        return new Structure(type, mek.isSuperHeavy(), mek.getMovementMode());
    }

    public static Integer maxJumpMP(Mek mek) {
        if (mek.isSuperHeavy()) {
            return 0;
        }
        if (!mek.hasEngine()
              || (!mek.getEngine().isFusion() && (mek.getEngine().getEngineType() != Engine.FISSION))) {
            return 0;
        } else if ((mek.getJumpType() == Mek.JUMP_IMPROVED)
              || (mek.getJumpType() == Mek.JUMP_PROTOTYPE_IMPROVED)) {
            return (int) Math.ceil(mek.getOriginalWalkMP() * 1.5);
        } else {
            return mek.getOriginalWalkMP();
        }
    }

    @Override
    public Entity getEntity() {
        return mek;
    }

    @Override
    public boolean isTank() {
        return false;
    }

    @Override
    public boolean isMek() {
        return true;
    }

    @Override
    public boolean isAero() {
        return false;
    }

    @Override
    public boolean isSmallCraft() {
        return false;
    }

    @Override
    public boolean isAdvancedAerospace() {
        return false;
    }

    @Override
    public boolean isProtoMek() {
        return false;
    }

    @Override
    public double getWeightMisc() {
        // LAM/QuadVee equipment is 10% of mass, rounded up to whole number (15% for bimodal LAM).
        // IO p. 113 (LAM), 134 (QV)
        if (mek instanceof LandAirMek) {
            return Math.ceil(mek.getWeight() *
                  (((LandAirMek) mek).getLAMType() == LandAirMek.LAM_BIMODAL ? 0.15 : 0.1));
        } else if (mek instanceof QuadVee) {
            return Math.ceil(mek.getWeight() * 0.1);
        }
        return 0.0;
    }

    @Override
    public double getWeightPowerAmp() {
        if (!mek.hasEngine()
              || (mek.getEngine().getEngineType() == Engine.COMBUSTION_ENGINE)
              || (mek.getEngine().getEngineType() == Engine.FUEL_CELL)) {
            double powerAmpWeight = 0;
            for (Mounted<?> m : mek.getWeaponList()) {
                WeaponType wt = (WeaponType) m.getType();
                if (wt instanceof EnergyWeapon) {
                    powerAmpWeight += m.getTonnage();
                }
                if ((m.getLinkedBy() != null)
                      && (m.getLinkedBy().getType() instanceof MiscType)
                      && m.getLinkedBy().getType().hasFlag(MiscType.F_PPC_CAPACITOR)) {
                    powerAmpWeight += m.getLinkedBy().getTonnage();
                }
            }
            return TestEntity.ceil(powerAmpWeight / 10f, getWeightCeilingPowerAmp());
        }
        return 0;
    }

    public double getWeightCockpit() {
        return switch (mek.getCockpitType()) {
            case Mek.COCKPIT_SMALL -> 2.0;
            case Mek.COCKPIT_TORSO_MOUNTED,
                 Mek.COCKPIT_DUAL,
                 Mek.COCKPIT_SUPERHEAVY,
                 Mek.COCKPIT_SUPERHEAVY_INDUSTRIAL,
                 Mek.COCKPIT_TRIPOD,
                 Mek.COCKPIT_TRIPOD_INDUSTRIAL,
                 Mek.COCKPIT_INTERFACE,
                 Mek.COCKPIT_QUADVEE -> 4.0;
            case Mek.COCKPIT_PRIMITIVE,
                 Mek.COCKPIT_PRIMITIVE_INDUSTRIAL,
                 Mek.COCKPIT_SUPERHEAVY_TRIPOD,
                 Mek.COCKPIT_SUPERHEAVY_TRIPOD_INDUSTRIAL,
                 Mek.COCKPIT_SMALL_COMMAND_CONSOLE -> 5.0;
            case Mek.COCKPIT_COMMAND_CONSOLE -> 6.0;
            case Mek.COCKPIT_SUPERHEAVY_COMMAND_CONSOLE -> 7.0;
            default -> 3.0;
        };
    }

    public double getWeightGyro() {
        double retVal = Math.ceil(engine.getRating() / 100.0f);
        if (mek.getGyroType() == Mek.GYRO_XL) {
            retVal /= 2;
        } else if (mek.getGyroType() == Mek.GYRO_COMPACT) {
            retVal *= 1.5;
        } else if ((mek.getGyroType() == Mek.GYRO_HEAVY_DUTY)
              || (mek.getGyroType() == Mek.GYRO_SUPERHEAVY)) {
            retVal *= 2;
        } else if (mek.getGyroType() == Mek.GYRO_NONE) {
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
        return mek.heatSinks();
    }

    @Override
    public double getWeightHeatSinks() {
        boolean hasCompact = false;
        double compactHsTons = 0;
        for (Mounted<?> misc : mek.getMisc()) {
            if (misc.getType().hasFlag(MiscType.F_COMPACT_HEAT_SINK)) {
                hasCompact = true;
                compactHsTons += misc.getTonnage();
            }
        }
        if (hasCompact) {
            return compactHsTons
                  - (engine.getWeightFreeEngineHeatSinks() * 1.5f);
        } else {
            return mek.heatSinks() - engine.getWeightFreeEngineHeatSinks();
        }
    }

    @Override
    public boolean hasDoubleHeatSinks() {
        return mek.hasDoubleHeatSinks();
    }

    @Override
    public String printWeightMisc() {
        return "";
    }

    @Override
    public String printWeightControls() {
        return StringUtil.makeLength(
              mek.getCockpitTypeString() + ":", getPrintSize() - 5) + makeWeightString(getWeightCockpit())
              + "\n"
              + StringUtil.makeLength(mek.getGyroTypeString() + ":",
              getPrintSize() - 5)
              + makeWeightString(getWeightGyro())
              + "\n";
    }

    public Mek getMek() {
        return mek;
    }

    public boolean isCockpitLocation(int location) {
        if (mek.getCockpitType() == Mek.COCKPIT_TORSO_MOUNTED
              || mek.getCockpitType() == Mek.COCKPIT_VRRP) {
            return location == Mek.LOC_CENTER_TORSO;
        }
        return location == Mek.LOC_HEAD;
    }

    public boolean isEngineLocation(int location) {
        return mek.hasSystem(Mek.SYSTEM_ENGINE, location);
    }

    public int countCriticalSlotsFromEquipInLocation(Entity entity, Mounted<?> mount,
          int location) {
        int count = 0;
        for (int slots = 0; slots < entity.getNumberOfCriticalSlots(location); slots++) {
            CriticalSlot slot = entity.getCritical(location, slots);
            if (slot != null && slot.getType() == CriticalSlot.TYPE_EQUIPMENT) {
                if (slot.getMount().equals(mount)) {
                    count++;
                }
            }
        }
        return count;
    }

    public boolean checkMiscSpreadAllocation(Entity entity, Mounted<?> mounted,
          StringBuffer buff) {
        MiscType mt = (MiscType) mounted.getType();
        if (mt.hasFlag(MiscType.F_STEALTH) && !entity.hasPatchworkArmor()) {
            // stealth needs to have 2 crits in legs arm and side torso
            if (countCriticalSlotsFromEquipInLocation(entity, mounted,
                  Mek.LOC_LEFT_ARM) != 2) {
                buff.append("incorrect number of stealth crits in left arm\n");
                return false;
            }
            if (countCriticalSlotsFromEquipInLocation(entity, mounted,
                  Mek.LOC_RIGHT_ARM) != 2) {
                buff.append("incorrect number of stealth crits in right arm\n");
                return false;
            }
            if (countCriticalSlotsFromEquipInLocation(entity, mounted,
                  Mek.LOC_LEFT_LEG) != 2) {
                buff.append("incorrect number of stealth crits in left leg\n");
                return false;
            }
            if (countCriticalSlotsFromEquipInLocation(entity, mounted,
                  Mek.LOC_RIGHT_LEG) != 2) {
                buff.append("incorrect number of stealth crits in right leg\n");
                return false;
            }
            if (countCriticalSlotsFromEquipInLocation(entity, mounted, Mek.LOC_LEFT_TORSO) != 2) {
                buff.append("incorrect number of stealth crits in left torso\n");
                return false;
            }
            if (countCriticalSlotsFromEquipInLocation(entity, mounted, Mek.LOC_RIGHT_TORSO) != 2) {
                buff.append("incorrect number of stealth crits in right torso\n");
                return false;
            }
        }
        if (mt.hasFlag(MiscType.F_DRONE_CONTROL_CONSOLE)) {
            if (mounted.getLocation() != Mek.LOC_HEAD) {
                buff.append("Drone Control Console must be mounted in head");
                return false;
            }
        }
        if (mt.hasFlag(MiscType.F_MOBILE_HPG)) {
            if ((countCriticalSlotsFromEquipInLocation(entity, mounted,
                  Mek.LOC_LEFT_ARM) > 0)
                  || (countCriticalSlotsFromEquipInLocation(entity, mounted,
                  Mek.LOC_RIGHT_ARM) > 0)
                  || (countCriticalSlotsFromEquipInLocation(entity, mounted,
                  Mek.LOC_HEAD) > 0)
                  || (countCriticalSlotsFromEquipInLocation(entity, mounted,
                  Mek.LOC_LEFT_LEG) > 0)
                  || (countCriticalSlotsFromEquipInLocation(entity, mounted,
                  Mek.LOC_RIGHT_LEG) > 0)) {
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
                if (locations != Mek.LOC_HEAD) {
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
            if (countCriticalSlotsFromEquipInLocation(entity, mounted, Mek.LOC_LEFT_TORSO) != ((TechConstants.isClan(mt
                  .getTechLevel(entity.getTechLevelYear()))) ? 3
                  : 4)) {
                buff.append("incorrect number of partial wing crits in left torso\n");
                return false;
            }
            if (countCriticalSlotsFromEquipInLocation(entity, mounted, Mek.LOC_RIGHT_TORSO) != ((TechConstants.isClan(mt
                  .getTechLevel(entity.getTechLevelYear()))) ? 3
                  : 4)) {
                buff.append("incorrect number of partial wing crits in right torso\n");
                return false;
            }
        }
        if (mt.hasFlag(MiscType.F_CHAIN_DRAPE)) {
            if (countCriticalSlotsFromEquipInLocation(entity, mounted, Mek.LOC_LEFT_TORSO) != 3) {
                buff.append("incorrect number of chain drape crits in left torso\n");
                return false;
            }
            if (countCriticalSlotsFromEquipInLocation(entity, mounted, Mek.LOC_RIGHT_TORSO) != 3) {
                buff.append("incorrect number of chain drape crits in right torso\n");
                return false;
            }
        }
        return true;
    }

    public boolean criticalSlotsAllocated(Entity entity, Mounted<?> mounted, Vector<Serializable> allocation,
          StringBuffer buff) {
        int location = mounted.getLocation();
        EquipmentType et = mounted.getType();
        int criticalSlots;
        if (et instanceof MiscType) {
            criticalSlots = calcMiscCrits((MiscType) et, mounted.getSize());
        } else {
            criticalSlots = mounted.getNumCriticalSlots();
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
            int secCound;
            for (int locations = 0; locations < entity.locations(); locations++) {
                if (locations == location) {
                    continue;
                }

                secCound = countCriticalSlotsFromEquipInLocation(entity, mounted,
                      locations);
                if ((secCound != 0)
                      && (location == Mek.mostRestrictiveLoc(locations,
                      location))) {
                    count += secCound;
                    break;
                }
            }
        }

        if (count == criticalSlots) {
            return true;
        }

        allocation.addElement(mounted);
        allocation.addElement(criticalSlots);
        allocation.addElement(count);
        return false;
    }

    public boolean checkCriticalSlotsForEquipment(Mek mek, Vector<Mounted<?>> unallocated,
          Vector<Serializable> allocation, Vector<Integer> heatSinks, StringBuffer buff) {
        boolean legal = true;
        int countInternalHeatSinks = 0;
        for (Mounted<?> m : mek.getEquipment()) {
            int loc = m.getLocation();
            if (loc == Entity.LOC_NONE) {
                if ((m.getType() instanceof AmmoType)
                      && (m.getUsableShotsLeft() <= 1)) {
                    continue;
                }
                if (m.getNumCriticalSlots() == 0) {
                    continue;
                }
                if (!(m.getType() instanceof MiscType mt)) {
                    unallocated.addElement(m);
                    continue;
                }
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
            if (mek.isOmni()
                  && (mek instanceof BipedMek)
                  && ((loc == Mek.LOC_LEFT_ARM) || (loc == Mek.LOC_RIGHT_ARM))
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
                if (mek.hasSystem(Mek.ACTUATOR_LOWER_ARM, loc)
                      || mek.hasSystem(Mek.ACTUATOR_HAND, loc)) {
                    buff.append("Omni meks with arm mounted ").append(weapon)
                          .append(" cannot have lower armor or hand actuators!\n");
                    legal = false;
                }
            }
        }
        if ((countInternalHeatSinks > engine.integralHeatSinkCapacity(this.mek.hasCompactHeatSinks()))
              || ((countInternalHeatSinks < engine.integralHeatSinkCapacity(this.mek.hasCompactHeatSinks()))
              && (countInternalHeatSinks != mek.heatSinks(false))
              && !mek.isOmni())) {
            heatSinks.addElement(countInternalHeatSinks);
        }
        return legal;
    }

    public boolean correctCriticalSlots(StringBuffer buff) {
        Vector<Mounted<?>> unallocated = new Vector<>();
        Vector<Serializable> allocation = new Vector<>();
        Vector<Integer> heatSinks = new Vector<>();
        boolean correct = checkCriticalSlotsForEquipment(mek, unallocated, allocation, heatSinks, buff);
        if (!unallocated.isEmpty()) {
            buff.append("Unallocated Equipment:\n");
            for (Mounted<?> mount : unallocated) {
                buff.append(mount.getType().getInternalName()).append("\n");
            }
            correct = false;
        }
        if (!allocation.isEmpty()) {
            buff.append("Allocated Equipment:\n");
            for (Enumeration<Serializable> serializableEnum = allocation.elements(); serializableEnum
                  .hasMoreElements(); ) {
                Mounted<?> mount = (Mounted<?>) serializableEnum.nextElement();
                int needCrits = (Integer) serializableEnum.nextElement();
                int aktCrits = (Integer) serializableEnum.nextElement();
                buff.append(mount.getType().getInternalName()).append(" has ")
                      .append(needCrits).append(" Slots, but ")
                      .append(aktCrits).append(" Slots are allocated!")
                      .append("\n");
            }
            correct = false;
        }
        if (!heatSinks.isEmpty()) {
            int sinks = heatSinks.elements().nextElement();
            buff.append(sinks - engine.integralHeatSinkCapacity(mek
                  .hasCompactHeatSinks())).append(" unallocated heat sinks\n");
            correct = false;
        }
        if (!checkSystemCriticalSlots(buff)) {
            correct = false;
        }
        return correct;
    }

    private boolean checkSystemCriticalSlots(StringBuffer buff) {
        // Engine criticalSlots
        boolean engineCorrect = true;
        int requiredSideCrits = engine.getSideTorsoCriticalSlots().length;
        if ((requiredSideCrits != mek.getNumberOfCriticalSlots(
              CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_ENGINE, Mek.LOC_LEFT_TORSO))
              || (requiredSideCrits != mek.getNumberOfCriticalSlots(
              CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_ENGINE,
              Mek.LOC_RIGHT_TORSO))) {
            engineCorrect = false;
        }
        int requiredCTCrits = engine.getCenterTorsoCriticalSlots(mek.getGyroType()).length;
        if (requiredCTCrits != mek
              .getNumberOfCriticalSlots(CriticalSlot.TYPE_SYSTEM,
                    Mek.SYSTEM_ENGINE, Mek.LOC_CENTER_TORSO)) {
            engineCorrect = false;
        }
        if (!engineCorrect) {
            buff.append("Engine: Incorrect number of criticalSlots allocated.\n");
        }

        if (!engineCorrect) {
            return false;
        }

        if (getMek().getGyroType() == Mek.GYRO_NONE
              && getMek().getCockpitType() != Mek.COCKPIT_INTERFACE) {
            buff.append("Missing Gyro!.\n");
            return false;
        }

        return true;
    }

    public String printArmorLocProp(int loc, int wert) {
        return " is greater than " + wert + "!";
    }

    public boolean correctArmor(StringBuffer buff) {
        boolean correct = true;
        for (int loc = 0; loc < mek.locations(); loc++) {
            if (loc == Mek.LOC_HEAD) {
                if (((mek.getOArmor(Mek.LOC_HEAD) > 9) && !mek.isSuperHeavy())
                      || ((mek.getOArmor(Mek.LOC_HEAD) > 12) && mek.isSuperHeavy())) {
                    buff.append(printArmorLocation(Mek.LOC_HEAD))
                          .append(printArmorLocProp(Mek.LOC_HEAD, 9))
                          .append("\n");
                    correct = false;
                }

            } else if ((mek.getOArmor(loc) + (mek.hasRearArmor(loc) ? mek
                  .getOArmor(loc, true) : 0)) > (2 * mek.getOInternal(loc))) {
                buff.append(printArmorLocation(loc))
                      .append(printArmorLocProp(loc,
                            2 * mek.getOInternal(loc)))
                      .append("\n");
                correct = false;
            }
        }

        if (!getEntity().hasPatchworkArmor()
              && (getEntity().getLabTotalArmorPoints() < getEntity().getTotalOArmor())) {
            correct = false;
            buff.append("Too many armor points allocated.\n");
        }

        return correct;
    }

    public boolean correctMovement(StringBuffer buff) {
        // Mechanical Jump Boosts can be greater than Running as long as
        // the unit can handle the weight.
        if ((mek.getJumpMP(MPCalculationSetting.NO_GRAVITY) > mek.getOriginalRunMP())
              && !mek.hasWorkingMisc(MiscType.F_PARTIAL_WING)) {
            buff.append("Jump MP exceeds run MP\n");
            return false;
        }
        if ((mek.getJumpMP(MPCalculationSetting.NO_GRAVITY) > mek.getOriginalWalkMP())
              && (((mek.getJumpType() != Mek.JUMP_IMPROVED) && (mek.getJumpType() != Mek.JUMP_PROTOTYPE_IMPROVED))
              && !mek.hasWorkingMisc(MiscType.F_PARTIAL_WING))) {
            buff.append("Jump MP exceeds walk MP without IJJs\n");
            return false;
        }
        if ((mek instanceof LandAirMek) && mek.getJumpMP(MPCalculationSetting.NO_GRAVITY) < 3) {
            buff.append("LAMs must have at least 3 jumping MP.\n");
            return false;
        }
        return true;
    }

    @Override
    public boolean correctEntity(StringBuffer stringBuffer, int ammoTechLvl) {
        boolean correct = true;
        if (skip()) {
            return true;
        }

        if (!allowOverweightConstruction() && !correctWeight(stringBuffer)) {
            stringBuffer.insert(0, printTechLevel() + printShortMovement());
            stringBuffer.append(printWeightCalculation());
            correct = false;
        }
        if (!engine.engineValid) {
            stringBuffer.append(engine.problem.toString()).append("\n\n");
            correct = false;
        }
        if (getCountHeatSinks() < engine.getWeightFreeEngineHeatSinks()) {
            stringBuffer.append("Heat Sinks:\n");
            stringBuffer.append(" Engine    ").append(engine.integralHeatSinkCapacity(mek
                  .hasCompactHeatSinks())).append("\n");
            stringBuffer.append(" Total     ").append(getCountHeatSinks()).append("\n");
            stringBuffer.append(" Required  ").append(engine.getWeightFreeEngineHeatSinks()).append("\n");
            correct = false;
        }
        if (showCorrectArmor() && !correctArmor(stringBuffer)) {
            correct = false;
        }
        if (showCorrectCritical() && !correctCriticalSlots(stringBuffer)) {
            correct = false;
        }
        if (showFailedEquip() && hasFailedEquipment(stringBuffer)) {
            correct = false;
        }
        if (hasIllegalTechLevels(stringBuffer, ammoTechLvl)) {
            correct = false;
        }
        if (showIncorrectIntroYear() && hasIncorrectIntroYear(stringBuffer)) {
            correct = false;
        }
        if (hasIllegalEquipmentCombinations(stringBuffer)) {
            correct = false;
        }
        for (Mounted<?> misc : mek.getMisc()) {
            correct = correct && checkMiscSpreadAllocation(mek, misc, stringBuffer);
        }
        correct = correct && correctMovement(stringBuffer);
        if (getEntity().hasQuirk(OptionsConstants.QUIRK_NEG_ILLEGAL_DESIGN)
              || getEntity().canonUnitWithInvalidBuild()) {
            correct = true;
        }
        return correct;
    }

    @Override
    public StringBuffer printEntity() {
        StringBuffer buff = new StringBuffer();
        buff.append("Mek: ").append(mek.getDisplayName()).append("\n");
        buff.append("Found in: ").append(fileString).append("\n");
        buff.append(printTechLevel());
        buff.append("Intro year: ").append(mek.getYear()).append("\n");
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
        correctCriticalSlots(buff);

        // printArmor(buff);
        printFailedEquipment(buff);
        return buff;
    }

    @Override
    public String getName() {
        return "Mek: " + mek.getDisplayName();
    }

    @Override
    public double getArmoredComponentWeight() {
        double weight = 0.0;
        double cockpitWeight = 0.0;
        for (int location = 0; location < mek.locations(); location++) {
            for (int slot = 0; slot < mek.getNumberOfCriticalSlots(location); slot++) {
                CriticalSlot cs = mek.getCritical(location, slot);
                if ((cs != null) && cs.isArmored()) {
                    // Armored cockpit (including command console) adds 1 ton, regardless of number of slots
                    if ((cs.getType() == CriticalSlot.TYPE_SYSTEM) && (cs.getIndex() == Mek.SYSTEM_COCKPIT)) {
                        cockpitWeight = 1.0;
                    } else {
                        weight += 0.5;
                    }
                }
            }
        }
        return weight + cockpitWeight;
    }

    /**
     * Check if the unit has combinations of equipment which are not allowed in the construction rules.
     *
     * @param buff diagnostics are appended to this
     *
     * @return true if the entity is illegal
     */
    @Override
    public boolean hasIllegalEquipmentCombinations(StringBuffer buff) {
        boolean illegal = super.hasIllegalEquipmentCombinations(buff);

        boolean hasStealth = mek.hasStealth();
        boolean hasC3 = mek.hasAnyC3System();
        boolean hasHarjelII = false;
        boolean hasHarjelIII = false;
        boolean hasNullSig = false;
        boolean hasVoidSig = false;
        boolean hasTC = false;
        boolean hasMASC = false;
        boolean hasAES = false;
        boolean hasMekJumpBooster = false;
        boolean hasPartialWing = false;
        EquipmentType advancedMyomer = null;
        HashSet<Integer> shieldLocations = new HashSet<>();

        // First we find all the equipment that is required or incompatible with other
        // equipment,
        // so we don't have to execute another loop each time one of those situations
        // comes up.
        for (MiscMounted m : mek.getMisc()) {
            hasHarjelII |= m.getType().hasFlag(MiscType.F_HARJEL_II);
            hasHarjelIII |= m.getType().hasFlag(MiscType.F_HARJEL_III);
            hasNullSig |= m.getType().hasFlag(MiscType.F_NULL_SIG);
            hasVoidSig |= m.getType().hasFlag(MiscType.F_VOID_SIG);
            hasTC |= m.getType().hasFlag(MiscType.F_TARGETING_COMPUTER);
            hasMASC |= m.getType().hasFlag(MiscType.F_MASC)
                  && !m.getType().hasFlag(MiscTypeFlag.S_SUPERCHARGER);
            hasAES |= m.getType().hasFlag(MiscType.F_ACTUATOR_ENHANCEMENT_SYSTEM);
            if (m.getType().hasFlag(MiscType.F_TSM)
                  || m.getType().hasFlag(MiscType.F_INDUSTRIAL_TSM)
                  || m.getType().hasFlag(MiscType.F_SCM)) {
                advancedMyomer = m.getType();
            }
            hasMekJumpBooster |= m.is(EquipmentTypeLookup.MECHANICAL_JUMP_BOOSTER);
            hasPartialWing |= m.getType().hasFlag(MiscType.F_PARTIAL_WING);

            if (m.getType().hasFlag(MiscType.F_CLUB) &&
                  (m.getType().hasAnyFlag(MiscTypeFlag.S_SHIELD_SMALL,
                        MiscTypeFlag.S_SHIELD_MEDIUM,
                        MiscTypeFlag.S_SHIELD_LARGE))) {
                if (shieldLocations.contains(m.getLocation())) {
                    illegal = true;
                    buff.append("Only one shield can be mounted in a location.\n");
                } else {
                    shieldLocations.add(m.getLocation());
                }
            }
        }

        for (Mounted<?> m : getEntity().getMisc()) {
            final MiscType misc = (MiscType) m.getType();

            // TO:AUE p.104
            if (misc.hasFlag(MiscType.F_UMU)) {
                if (mek.getJumpType() != Mek.JUMP_NONE) {
                    illegal = true;
                    buff.append("UMUs cannot be mounted with jump jets\n");
                }
                if (!mek.locationIsTorso(m.getLocation()) && !mek.locationIsLeg(m.getLocation())) {
                    illegal = true;
                    buff.append("UMUs must be mounted in the torso or legs\n");
                }
            }

            if (misc.hasFlag(MiscType.F_MASC)
                  && misc.hasFlag(MiscTypeFlag.S_SUPERCHARGER)) {
                if (mek instanceof LandAirMek) {
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

            if (misc.hasFlag(MiscType.F_REMOTE_DRONE_COMMAND_CONSOLE)) {
                if (mek.getCockpitType() == Mek.COCKPIT_COMMAND_CONSOLE) {
                    buff.append("cockpit command console can't be combined with remote drone command console\n");
                    illegal = true;
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

            if (misc.hasFlag(MiscType.F_LIGHT_FLUID_SUCTION_SYSTEM)
                  && !mek.isIndustrial()) {
                illegal = true;
                buff.append("BattleMek can't mount light fluid suction system\n");
            }

            if (!mek.entityIsQuad()) {
                if (replacesHandActuator(misc)
                      && mek.hasSystem(Mek.ACTUATOR_HAND, m.getLocation())) {
                    illegal = true;
                    buff.append("Mek can only mount ").append(misc.getName())
                          .append(" in arm with no hand actuator.\n");
                } else if (replacesLowerArm(misc)
                      && mek.hasSystem(Mek.ACTUATOR_LOWER_ARM, m.getLocation())) {
                    illegal = true;
                    buff.append("Mek can only mount ").append(misc.getName())
                          .append(" in arm with no lower arm actuator.\n");
                } else if (requiresHandActuator(misc) && (m.getLocation() != Entity.LOC_NONE)
                      && !mek.hasSystem(Mek.ACTUATOR_HAND, m.getLocation())) {
                    illegal = true;
                    buff.append("Mek requires a hand actuator in the arm that mounts ").append(misc.getName())
                          .append("\n");
                } else if (requiresLowerArm(misc) && (m.getLocation() != Entity.LOC_NONE)
                      && !mek.hasSystem(Mek.ACTUATOR_LOWER_ARM, m.getLocation())) {
                    illegal = true;
                    buff.append("Mek requires a lower arm actuator in the arm that mounts ").append(misc.getName())
                          .append("\n");
                }
                if (replacesHandActuator(misc) && (m.getLocation() != Entity.LOC_NONE)) {
                    String errorMsg = "Can only mount a single equipment item in the "
                          + mek.getLocationName(m.getLocation())
                          + " that replaces the hand actuator\n";
                    // This error message would appear once for each offending equipment; instead,
                    // add it only once
                    if (!buff.toString().contains(errorMsg)) {
                        int miscId = mek.getEquipmentNum(m);
                        if (mek.getMisc().stream().filter(otherMisc -> mek.getEquipmentNum(otherMisc) != miscId)
                              .filter(otherMisc -> otherMisc.getLocation() == m.getLocation())
                              .anyMatch(otherMisc -> replacesHandActuator(otherMisc.getType()))) {
                            illegal = true;
                            buff.append(errorMsg);
                        }
                    }
                }
            }
            if (misc.hasFlag(MiscType.F_HEAD_TURRET) && isCockpitLocation(Mek.LOC_HEAD)) {
                illegal = true;
                buff.append("head turret requires torso mounted cockpit\n");
            }

            if (misc.hasFlag(MiscType.F_SHOULDER_TURRET) && mek instanceof QuadMek) {
                illegal = true;
                buff.append("quad meks can't mount shoulder turrets\n");
            }

            if (misc.hasFlag(MiscType.F_SHOULDER_TURRET)) {
                if (m.getLocation() != Mek.LOC_RIGHT_TORSO
                      && m.getLocation() != Mek.LOC_LEFT_TORSO) {
                    if (mek.countWorkingMisc(MiscType.F_SHOULDER_TURRET, m.getLocation()) > 1) {
                        illegal = true;
                        buff.append("max of 1 shoulder turret per side torso\n");
                    }
                }
            }

            if (misc.hasFlag(MiscType.F_TRACKS)) {
                if (mek instanceof QuadVee) {
                    if (misc.hasFlag(
                          MiscTypeFlag.S_QUADVEE_WHEELS) != (((QuadVee) mek).getMotiveType() == QuadVee.MOTIVE_WHEEL)) {
                        illegal = true;
                        buff.append("Motive equipment does not match QuadVee motive type.\n");
                    }
                } else if (misc.hasFlag(MiscTypeFlag.S_QUADVEE_WHEELS)) {
                    illegal = true;
                    buff.append("Wheels can only be used on QuadVees.\n");
                }
                for (int loc = 0; loc < mek.locations(); loc++) {
                    if (mek.locationIsLeg(loc)
                          && countCriticalSlotsFromEquipInLocation(mek, m, loc) != 1) {
                        illegal = true;
                        buff.append(misc.getName()).append(" require one critical slot in each leg.\n");
                        break;
                    }
                }
            }

            if (m.getType().hasFlag(MiscType.F_TALON)) {
                int slots = getMek().isSuperHeavy() ? 1 : 2;
                for (int loc = 0; loc < mek.locations(); loc++) {
                    if (mek.locationIsLeg(loc) && countCriticalSlotsFromEquipInLocation(mek, m, loc) != slots) {
                        illegal = true;
                        buff.append("Talons require ").append(slots).append(" critical slots in each leg.\n");
                        break;
                    }
                }
            }

            if (misc.hasFlag(MiscType.F_RAM_PLATE)) {
                if (!(mek instanceof QuadMek)) {
                    buff.append(misc.getName()).append(" can only be mounted on a quad mek.\n");
                    illegal = true;
                }
                if (!mek.hasReinforcedStructure()) {
                    buff.append(misc.getName()).append(" requires reinforced structure.\n");
                    illegal = true;
                }
                for (int loc = 0; loc < mek.locations(); loc++) {
                    if (mek.locationIsTorso(loc)
                          && countCriticalSlotsFromEquipInLocation(mek, m, loc) != 1) {
                        illegal = true;
                        buff.append(misc.getName()).append(" requires one critical slot in each torso location.\n");
                        break;
                    }
                }
            }

            if (mek.isSuperHeavy()
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
                buff.append("Superheavy may not mount ").append(m.getType().getName()).append("\n");
                illegal = true;
            }

            if (mek.isIndustrial()) {
                if (misc.hasFlag(MiscType.F_TSM)
                      || misc.hasFlag(MiscType.F_SCM)
                      || (misc.hasFlag(MiscType.F_MASC) && !misc.hasFlag(MiscTypeFlag.S_SUPERCHARGER))) {
                    buff.append("industrial mek can't mount ").append(misc.getName()).append("\n");
                    illegal = true;
                }
                if (!mek.hasAdvancedFireControl()
                      && (misc.hasFlag(MiscType.F_TARGETING_COMPUTER)
                      || misc.hasFlag(MiscType.F_ARTEMIS)
                      || misc.hasFlag(MiscType.F_ARTEMIS_PROTO)
                      || misc.hasFlag(MiscType.F_ARTEMIS_V)
                      || misc.hasFlag(MiscType.F_BAP))) {
                    buff.append("Industrial mek without advanced fire control can't mount ")
                          .append(misc.getName()).append("\n");
                    illegal = true;
                }

                // TM p.71
                if (misc.hasFlag(MiscTypeFlag.F_DOUBLE_HEAT_SINK)
                      || misc.hasFlag(MiscTypeFlag.F_COMPACT_HEAT_SINK)
                      || misc.hasFlag(MiscTypeFlag.F_IS_DOUBLE_HEAT_SINK_PROTOTYPE)) {
                    buff.append("Industrial Meks may only mount standard single heat sinks\n");
                    illegal = true;
                }
            } else {
                if (misc.hasFlag(MiscType.F_INDUSTRIAL_TSM)
                      || misc.hasFlag(MiscType.F_ENVIRONMENTAL_SEALING)
                      || misc.hasFlag(MiscType.F_FUEL)) {
                    buff.append("Non-industrial mek can't mount ").append(misc.getName()).append("\n");
                    illegal = true;
                }
            }

            if ((mek instanceof LandAirMek)
                  && (misc.hasFlag(MiscType.F_MODULAR_ARMOR)
                  || misc.hasFlag(MiscType.F_JUMP_BOOSTER)
                  || misc.hasFlag(MiscType.F_PARTIAL_WING)
                  || misc.hasFlag(MiscType.F_DUMPER)
                  || misc.hasFlag(MiscType.F_HEAVY_BRIDGE_LAYER)
                  || misc.hasFlag(MiscType.F_MEDIUM_BRIDGE_LAYER)
                  || misc.hasFlag(MiscType.F_LIGHT_BRIDGE_LAYER)
                  || (misc.hasFlag(MiscType.F_CLUB)
                  && misc.hasFlag(MiscTypeFlag.S_BACKHOE)
                  || misc.hasFlag(MiscTypeFlag.S_COMBINE)))) {
                buff.append("LAMs may not mount ").append(misc.getName()).append("\n");
                illegal = true;
            }

            if ((misc.hasFlag(MiscType.F_CHAIN_DRAPE_APRON) || misc.hasFlag(MiscType.F_CHAIN_DRAPE_PONCHO))
                  && (mek.isQuadMek() || mek.getCockpitType() == Mek.COCKPIT_TORSO_MOUNTED)
            ) {
                buff.append("Quad meks and meks with torso cockpits may only mount a chain drape as a Cape");
                illegal = true;
            }
        }

        if (mek.isSuperHeavy()) {
            if (mek.getGyroType() != Mek.GYRO_SUPERHEAVY) {
                buff.append("Superheavy Meks must use a superheavy gyro.\n");
                illegal = true;
            }

            if (mek.getArmoredComponentBV() > 0) {
                buff.append("Superheavy Meks cannot have armored components\n");
                illegal = true;
            }

            if (mek instanceof QuadVee) {
                buff.append("QuadVees cannot be constructed as superheavies.\n");
            }
        } else if (mek.getGyroType() == Mek.GYRO_SUPERHEAVY) {
            buff.append("Only superheavy Meks can use a superheavy gyro.\n");
            illegal = true;
        }

        if (mek.getCockpitType() == Mek.COCKPIT_INTERFACE) {
            if (mek.getCockpit().stream().anyMatch(CriticalSlot::isArmored)) {
                buff.append("Interface cockpits may not use component armoring.\n");
                illegal = true;
            }
        }

        if (mek.isIndustrial()) {
            if ((mek.getCockpitType() == Mek.COCKPIT_INDUSTRIAL
                  || mek.getCockpitType() == Mek.COCKPIT_PRIMITIVE_INDUSTRIAL) && hasC3) {
                buff.append("Industrial meks without advanced fire control cannot use C3 equipment\n");
                illegal = true;
            }
            if ((mek.getJumpType() != Mek.JUMP_STANDARD)
                  && (mek.getJumpType() != Mek.JUMP_NONE)
                  && (mek.getJumpType() != Mek.JUMP_PROTOTYPE)) {
                buff.append("industrial meks can only mount standard jump jets or mechanical jump boosters\n");
                illegal = true;
            }
            if ((mek.getGyroType() != Mek.GYRO_STANDARD) && (mek.getGyroType() != Mek.GYRO_SUPERHEAVY)) {
                buff.append("industrial meks can only mount standard gyros\n");
                illegal = true;
            }
            switch (mek.hasEngine() ? engine.getEngineType() : Engine.NONE) {
                case Engine.NORMAL_ENGINE:
                    break;
                case Engine.COMBUSTION_ENGINE:
                case Engine.FUEL_CELL:
                case Engine.FISSION:
                    if (mek.isSuperHeavy()) {
                        buff.append("Superheavy Industrial Meks can only use standard or large fusion engines\n");
                        illegal = true;
                    }
                    break;
                default:
                    buff.append(
                          "Industrial Meks can only use standard and large fusion engines, ICEs, fuel cells or fission\n");
                    illegal = true;
                    break;
            }
        }

        if (mek.isPrimitive()) {
            if (mek.isOmni()) {
                buff.append("primitive meks can't be OmniMeks\n");
                illegal = true;
            }
            if (!((mek.getStructureType() == EquipmentType.T_STRUCTURE_STANDARD) || (mek
                  .getStructureType() == EquipmentType.T_STRUCTURE_INDUSTRIAL))) {
                buff.append("primitive meks can't mount advanced inner structure\n");
                illegal = true;
            }
            if (mek.hasEngine() && ((mek.getEngine().getEngineType() == Engine.XL_ENGINE)
                  || (mek.getEngine().getEngineType() == Engine.LIGHT_ENGINE)
                  || (mek.getEngine().getEngineType() == Engine.COMPACT_ENGINE)
                  || mek.getEngine().hasFlag(Engine.LARGE_ENGINE)
                  || (mek.getEngine().getEngineType() == Engine.XXL_ENGINE))) {
                buff.append("primitive meks can't mount XL, Light, Compact, XXL or Large Engines\n");
                illegal = true;
            }
            if (advancedMyomer != null) {
                buff.append("primitive meks can't mount advanced myomers\n");
                illegal = true;
            }
            if (mek.isIndustrial()) {
                if (mek.getArmorType(0) != EquipmentType.T_ARMOR_COMMERCIAL) {
                    buff.append("primitive IndustrialMeks must mount commercial armor\n");
                    illegal = true;
                }
            } else {
                if ((mek.getArmorType(0) != EquipmentType.T_ARMOR_PRIMITIVE)
                      && (mek.getArmorType(0) != EquipmentType.T_ARMOR_INDUSTRIAL)) {
                    buff.append("primitive BattleMeks must mount primitive battlemek armor\n");
                    illegal = true;
                }
            }
        }

        if (mek instanceof LandAirMek) {
            if (mek.isOmni()) {
                buff.append("LAMs may not be constructed as OmniMeks\n");
                illegal = true;
            }
            if (mek.getWeight() > 55) {
                buff.append("LAMs cannot be larger than 55 tons.\n");
                illegal = true;
            }
            EquipmentType structure = EquipmentType.get(EquipmentType.getStructureTypeName(mek.getStructureType(),
                  mek.isClan()));
            if (structure.getNumCriticalSlots(mek) > 0) {
                buff.append("LAMs may not use ").append(structure.getName()).append("\n");
                illegal = true;
            }

            Set<Integer> ats = new HashSet<>();
            for (int i = 0; i < mek.locations(); i++) {
                ats.add(mek.getArmorType(i));
            }
            for (int at : ats) {
                if (at == EquipmentType.T_ARMOR_HARDENED) {
                    buff.append("LAMs cannot use hardened armor.\n");
                    illegal = true;
                } else {
                    final EquipmentType eq = EquipmentType.get(EquipmentType.getArmorTypeName(at, mek.isClan()));
                    if (eq != null && eq.getNumCriticalSlots(mek) > 0) {
                        buff.append("LAMs cannot use ").append(eq.getName()).append("\n");
                        illegal = true;
                    }
                }
            }
            if (mek.countWorkingMisc(MiscType.F_BOMB_BAY) > 20) {
                buff.append("A LAM has a maximum of 20 bomb bays.\n");
                illegal = true;
            }
            if (isCockpitLocation(Mek.LOC_CENTER_TORSO)) {
                buff.append("LAMs may not use torso-mounted cockpits.\n");
                illegal = true;
            }
            if (mek.getNumberOfCriticalSlots(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_COCKPIT, Mek.LOC_HEAD) > 1) {
                buff.append("LAMs may not cockpits that require multiple critical slots.\n");
                illegal = true;
            }
            if (mek.getCockpitType() == Mek.COCKPIT_PRIMITIVE
                  || mek.getCockpitType() == Mek.COCKPIT_PRIMITIVE_INDUSTRIAL) {
                illegal = true;
            }
            if (mek.getGyroType() != Mek.GYRO_STANDARD
                  && mek.getGyroType() != Mek.GYRO_COMPACT
                  && mek.getGyroType() != Mek.GYRO_HEAVY_DUTY) {
                buff.append("LAMs may not use ").append(Mek.getGyroDisplayString(mek.getGyroType()))
                      .append("\n");
                illegal = true;
            }
            if (mek.getEngine().getEngineType() != Engine.NORMAL_ENGINE
                  && mek.getEngine().getEngineType() != Engine.COMPACT_ENGINE) {
                buff.append("LAMs may only use standard or compact fusion engines.\n");
                illegal = true;
            }

            Map<EquipmentType, Set<Integer>> spread = new HashMap<>();
            for (Mounted<?> m : mek.getEquipment()) {
                if (m.isSplit()) {
                    buff.append("Cannot split ").append(m.getType().getName())
                          .append(" between locations");
                    illegal = true;
                } else if (m.getType() instanceof ArtilleryWeapon) {
                    buff.append("LAMs cannot mount artillery weapons.\n");
                    illegal = true;
                } else if (m.getType() instanceof WeaponType
                      && ((((WeaponType) m.getType()).getAmmoType() == AmmoType.AmmoTypeEnum.GAUSS_HEAVY)
                      || (((WeaponType) m.getType()).getAmmoType() == AmmoType.AmmoTypeEnum.IGAUSS_HEAVY))) {
                    buff.append("LAMs cannot mount heavy gauss rifles.\n");
                    illegal = true;
                } else if ((m.getType() instanceof MiscType)
                      && m.getType().hasFlag(MiscType.F_CLUB)) {
                    buff.append("LAMs cannot be constructed with physical weapons.\n");
                    illegal = true;
                } else if (m.getType().isSpreadable()) {
                    if (!spread.containsKey(m.getType())) {
                        spread.put(m.getType(), new HashSet<>());
                    }
                    spread.get(m.getType()).add(m.getLocation());
                }
            }
            for (EquipmentType et : spread.keySet()) {
                if (spread.get(et).size() > 1) {
                    buff.append(et.getName()).append(" must be allocated to a single location.\n");
                    illegal = true;
                }
            }
            if (!mek.hasSystem(Mek.ACTUATOR_LOWER_ARM, Mek.LOC_RIGHT_ARM)
                  || !mek.hasSystem(Mek.ACTUATOR_LOWER_ARM, Mek.LOC_LEFT_ARM)
                  || !mek.hasSystem(Mek.ACTUATOR_UPPER_ARM, Mek.LOC_RIGHT_ARM)
                  || !mek.hasSystem(Mek.ACTUATOR_UPPER_ARM, Mek.LOC_LEFT_ARM)) {
                buff.append("LAMs require upper and lower arm actuators in both arms.\n");
                illegal = true;
            }
        }

        // Make sure all base chassis heat sinks are allocated
        if (mek.isOmni()) {
            int total = 0;
            int allocated = 0;
            boolean compact = false;
            for (Mounted<?> m : mek.getMisc()) {
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
            int required = total - (mek.isOmni()
                  ? mek.getEngine().getBaseChassisHeatSinks(compact)
                  : mek.getEngine().integralHeatSinkCapacity(compact));
            if (allocated < required) {
                illegal = true;
                buff.append("Only ")
                      .append(allocated)
                      .append(" of the required ")
                      .append(required)
                      .append(" heat sinks are allocated to critical slots.");
            }
        }

        if (hasMASC && advancedMyomer != null) {
            buff.append("MASC is incompatible with ").append(advancedMyomer.getName()).append("\n");
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
            // Find all locations with an AES and map the number in that location to the
            // location index.
            Map<Integer, Long> byLocation = mek.getMisc().stream()
                  .filter(m -> m.getType().hasFlag(MiscType.F_ACTUATOR_ENHANCEMENT_SYSTEM))
                  .collect(Collectors.groupingBy(Mounted::getLocation, Collectors.counting()));
            boolean multiple = false;
            boolean wrongLocation = false;
            int legCount = 0;
            for (Integer loc : byLocation.keySet()) {
                if (mek.locationIsLeg(loc)) {
                    legCount++;
                } else if (loc == Mek.LOC_HEAD
                      || loc == Mek.LOC_CENTER_TORSO
                      || loc == Mek.LOC_LEFT_TORSO
                      || loc == Mek.LOC_RIGHT_TORSO) {
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
                for (int loc = 0; loc < mek.locations(); loc++) {
                    if (mek.locationIsLeg(loc)) {
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
            // Nova CEWS is exempt and can be mounted with a null-sig (but not a void-sig)
            if (mek.hasC3() || mek.hasC3i()) {
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
            if (mek.isIndustrial()) {
                buff.append("Cannot mount HarJel repair system on IndustrialMek\n");
                illegal = true;
            }
            for (int loc = 0; loc < mek.locations(); ++loc) {
                int count = 0;
                for (Mounted<?> m : mek.getMisc()) {
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
                    int armor = mek.getArmorType(loc);
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

        for (WeaponMounted m : mek.getWeaponList()) {
            if ((m.getType().getAmmoType() == AmmoType.AmmoTypeEnum.GAUSS_HEAVY)
                  || (m.getType().getAmmoType() == AmmoType.AmmoTypeEnum.IGAUSS_HEAVY)) {
                boolean torso = mek.locationIsTorso(m.getLocation());
                if (m.getSecondLocation() != Entity.LOC_NONE) {
                    torso = torso && mek.locationIsTorso(m.getSecondLocation());
                }
                if (!mek.isSuperHeavy() && !torso) {
                    buff.append("Heavy Gauss can only be mounted in a torso location.\n");
                    illegal = true;
                }

                if (m.isMekTurretMounted()) {
                    buff.append("Heavy Gauss cannot be mounted in a turret.\n");
                    illegal = true;
                }
            }
            if ((m.getType().hasFlag(WeaponType.F_TASER))
                  && !(mek.hasEngine() && mek.getEngine().isFusion())) {
                buff.append(m.getType().getName()).append(" needs fusion engine\n");
                illegal = true;
            }
        }

        if (mek.hasFullHeadEject()) {
            if ((mek.getCockpitType() == Mek.COCKPIT_TORSO_MOUNTED)
                  || (mek.getCockpitType() == Mek.COCKPIT_COMMAND_CONSOLE)) {
                buff.append("full head ejection system incompatible with cockpit type\n");
                illegal = true;
            }
        }

        // Test if the crit slots for internal structure match the required crits; Note
        // that this intentionally
        // counts crit slots, not mounted equipment as the latter only works with a
        // fully loaded unit in MML and
        // will make units appear invalid during loading (MML calls
        // UnitUtil.expandUnitMounts() after loading)
        String structureName = EquipmentType.getStructureTypeName(mek.getStructureType(),
              TechConstants.isClan(mek.getStructureTechLevel()));
        EquipmentType structure = EquipmentType.get(structureName);
        int requiredStructureCrits = structure.getNumCriticalSlots(mek);
        if (mek.getNumberOfCriticalSlots(structure) != requiredStructureCrits) {
            buff.append("The internal structure of this mek is not using the correct number of crit slots\n");
            illegal = true;
        }

        if (hasPartialWing && hasMekJumpBooster) {
            buff.append("Partial wings cannot be combined with any type of jump boosters\n");
            illegal = true;
        }

        return illegal;
    }

    /**
     * @param misc A type of equipment
     *
     * @return Whether the equipment replaces the hand actuator when mounted in an arm
     */
    public static boolean replacesHandActuator(MiscType misc) {
        return misc.hasFlag(MiscType.F_SALVAGE_ARM)
              || misc.hasFlag(MiscType.F_HAND_WEAPON)
              || (misc.hasFlag(MiscType.F_CLUB)
              && (misc.hasAnyFlag(MiscTypeFlag.S_CHAINSAW,
              MiscTypeFlag.S_BACKHOE,
              MiscTypeFlag.S_DUAL_SAW,
              MiscTypeFlag.S_MINING_DRILL,
              MiscTypeFlag.S_ROCK_CUTTER,
              MiscTypeFlag.S_SPOT_WELDER,
              MiscTypeFlag.S_WRECKING_BALL,
              MiscTypeFlag.S_FLAIL)));
    }

    /**
     * @param equipment A type of equipment that can be mounted in a mek arm
     *
     * @return Whether the equipment requires the hand actuator
     */
    public static boolean requiresHandActuator(EquipmentType equipment) {
        return (equipment instanceof MiscType)
              && equipment.hasFlag(MiscType.F_CLUB)
              && equipment.hasAnyFlag(MiscTypeFlag.S_CHAIN_WHIP,
              MiscTypeFlag.S_HATCHET,
              MiscTypeFlag.S_MACE,
              MiscTypeFlag.S_SWORD,
              MiscTypeFlag.S_VIBRO_SMALL,
              MiscTypeFlag.S_VIBRO_MEDIUM,
              MiscTypeFlag.S_VIBRO_LARGE);
    }

    /**
     * @param misc A type of equipment that can be mounted in a mek arm
     *
     * @return Whether the equipment requires the lower arm actuator
     */
    public static boolean requiresLowerArm(MiscType misc) {
        return replacesHandActuator(misc) ||
              (misc.hasFlag(MiscType.F_CLUB)
                    && misc.hasAnyFlag(MiscTypeFlag.S_LANCE, MiscTypeFlag.S_RETRACTABLE_BLADE));
    }

    /**
     * @param misc A type of equipment that can be mounted in a mek arm
     *
     * @return Whether the equipment replaces the lower arm and hand actuators
     */
    public static boolean replacesLowerArm(MiscType misc) {
        return misc.hasFlag(MiscType.F_CLUB) && misc.hasFlag(MiscTypeFlag.S_PILE_DRIVER);
    }

    /**
     * @param mek      The Mek
     * @param eq       The equipment
     * @param location A location index on the Entity
     * @param buffer   If non-null and the location is invalid, will be appended with an explanation
     *
     * @return Whether the equipment can be mounted in the location on the Mek
     */
    public static boolean isValidMekLocation(Mek mek, EquipmentType eq, int location, @Nullable StringBuffer buffer) {
        if (eq instanceof MiscType) {
            if (eq.hasFlag(MiscType.F_CLUB) && (eq.hasAnyFlag(MiscTypeFlag.S_DUAL_SAW, MiscTypeFlag.S_PILE_DRIVER,
                  MiscTypeFlag.S_BACKHOE, MiscTypeFlag.S_MINING_DRILL,
                  MiscTypeFlag.S_COMBINE, MiscTypeFlag.S_CHAINSAW,
                  MiscTypeFlag.S_ROCK_CUTTER, MiscTypeFlag.S_BUZZSAW, MiscTypeFlag.S_SPOT_WELDER))) {
                if (mek.entityIsQuad() && (location != Mek.LOC_LEFT_TORSO) && (location != Mek.LOC_RIGHT_TORSO)) {
                    if (buffer != null) {
                        buffer.append(eq.getName()).append(" must be mounted in a side torso.\n");
                    }
                    return false;
                } else if (!mek.entityIsQuad() && (location != Mek.LOC_LEFT_ARM) && (location != Mek.LOC_RIGHT_ARM)) {
                    if (buffer != null) {
                        buffer.append(eq.getName()).append(" must be mounted in an arm.\n");
                    }
                    return false;
                }
            }
            if (eq.hasFlag(MiscType.F_CLUB) && (eq.hasAnyFlag(MiscTypeFlag.S_HATCHET, MiscTypeFlag.S_SWORD,
                  MiscTypeFlag.S_CHAIN_WHIP, MiscTypeFlag.S_FLAIL, MiscTypeFlag.S_LANCE, MiscTypeFlag.S_WRECKING_BALL,
                  MiscTypeFlag.S_MACE, MiscTypeFlag.S_RETRACTABLE_BLADE)
                  || ((MiscType) eq).isShield() || ((MiscType) eq).isVibroblade())
                  && (mek.entityIsQuad() || ((location != Mek.LOC_LEFT_ARM) && (location != Mek.LOC_RIGHT_ARM)))) {
                if (buffer != null) {
                    buffer.append(eq.getName()).append(" must be mounted in an arm.\n");
                }
                return false;
            }
            if (eq.hasFlag(MiscType.F_HAND_WEAPON) && eq.hasFlag(MiscTypeFlag.S_CLAW)
                  && (mek.entityIsQuad() || ((location != Mek.LOC_LEFT_ARM) && (location != Mek.LOC_RIGHT_ARM)))) {
                if (buffer != null) {
                    buffer.append(eq.getName()).append(" must be mounted in an arm.\n");
                }
                return false;
            }
            if (eq.hasFlag(MiscType.F_SALVAGE_ARM) && (mek.entityIsQuad()
                  || ((location != Mek.LOC_LEFT_ARM) && (location != Mek.LOC_RIGHT_ARM)))) {
                if (buffer != null) {
                    buffer.append(eq.getName()).append(" must be mounted in an arm.\n");
                }
                return false;
            }
            if (eq.hasFlag(MiscType.F_ACTUATOR_ENHANCEMENT_SYSTEM)
                  && ((location == Mek.LOC_HEAD) || mek.locationIsTorso(location))) {
                if (buffer != null) {
                    buffer.append(eq.getName()).append(" must be mounted in an arm or leg location.\n");
                }
                return false;
            }
            if (eq.hasFlag(MiscType.F_HEAD_TURRET) && (location != Mek.LOC_CENTER_TORSO)) {
                if (buffer != null) {
                    buffer.append(eq.getName()).append(" must be placed in the center torso.\n");
                }
                return false;
            }
            if ((eq.hasFlag(MiscType.F_QUAD_TURRET) || eq.hasFlag(MiscType.F_SHOULDER_TURRET))
                  && (location != Mek.LOC_RIGHT_TORSO) && (location != Mek.LOC_LEFT_TORSO)) {
                if (buffer != null) {
                    buffer.append(eq.getName()).append(" must be placed in a side torso.\n");
                }
                return false;
            }
            if (eq.hasFlag(MiscType.F_HARJEL) && mek.hasSystem(Mek.SYSTEM_COCKPIT, location)) {
                if (buffer != null) {
                    buffer.append(eq.getName()).append(" cannot be placed in the same location as the cockpit.\n");
                }
                return false;
            }
            if ((eq.hasFlag(MiscType.F_MASS) || eq.hasFlag(MiscType.F_REMOTE_DRONE_COMMAND_CONSOLE))
                  && !mek.hasSystem(Mek.SYSTEM_COCKPIT, location)) {
                if (buffer != null) {
                    buffer.append(eq.getName()).append(" must be placed in the same location as the cockpit.\n");
                }
                return false;
            }
            if (((eq.hasFlag(MiscType.F_MASC) && eq.hasFlag(MiscTypeFlag.S_SUPERCHARGER))
                  || eq.hasFlag(MiscType.F_EMERGENCY_COOLANT_SYSTEM))
                  && !mek.hasSystem(Mek.SYSTEM_ENGINE, location)) {
                if (buffer != null) {
                    buffer.append(eq.getName()).append(" must be placed in the a location with an engine critical.\n");
                }
                return false;
            }
            if ((eq.hasFlag(MiscType.F_FUEL) || eq.is(EquipmentTypeLookup.IS_CASE)
                  || eq.is(EquipmentTypeLookup.IS_CASE_P)
                  || eq.hasFlag(MiscType.F_LADDER) || eq.hasFlag(MiscType.F_LIGHT_BRIDGE_LAYER)
                  || eq.hasFlag(MiscType.F_MEDIUM_BRIDGE_LAYER) || eq.hasFlag(MiscType.F_HEAVY_BRIDGE_LAYER))
                  && !mek.locationIsTorso(location)) {
                if (buffer != null) {
                    buffer.append(eq.getName()).append(" must be placed in a torso location.\n");
                }
                return false;
            }
            if (eq.hasFlag(MiscType.F_LIFT_HOIST) && ((location == Mek.LOC_HEAD) || mek.locationIsLeg(location))) {
                if (buffer != null) {
                    buffer.append(eq.getName()).append(" must be placed in a torso or arm location.\n");
                }
                return false;
            }
            if (eq.hasFlag(MiscType.F_JUMP_JET)
                  && !mek.locationIsLeg(location) && !mek.locationIsTorso(location)) {
                if (buffer != null) {
                    buffer.append(eq.getName()).append(" must be placed in a torso or leg location.\n");
                }
                return false;
            }
            if ((eq.hasFlag(MiscType.F_AP_POD) || eq.hasFlag(MiscType.F_TRACKS) || eq.hasFlag(MiscType.F_TALON))
                  && !mek.locationIsLeg(location)) {
                if (buffer != null) {
                    buffer.append(eq.getName()).append(" must be placed in a leg location.\n");
                }
                return false;
            }
            if (eq.hasFlag(MiscType.F_MODULAR_ARMOR) && (location == Mek.LOC_HEAD)) {
                if (buffer != null) {
                    buffer.append(eq.getName()).append(" cannot be placed in the head.\n");
                }
                return false;
            }
            if (eq.hasFlag(MiscType.F_EJECTION_SEAT) && (location != Mek.LOC_HEAD)) {
                if (buffer != null) {
                    buffer.append(eq.getName()).append(" must be placed in the head.\n");
                }
                return false;
            }
        } else if (eq instanceof WeaponType) {
            if (eq.hasFlag(WeaponType.F_VGL) && !mek.locationIsTorso(location)) {
                if (buffer != null) {
                    buffer.append(eq.getName()).append(" must be placed in a torso location.\n");
                }
                return false;
            }
            if (((((WeaponType) eq).getAmmoType() == AmmoType.AmmoTypeEnum.GAUSS_HEAVY)
                  || ((WeaponType) eq).getAmmoType() == AmmoType.AmmoTypeEnum.IGAUSS_HEAVY)
                  && !mek.isSuperHeavy() && !mek.locationIsTorso(location)) {
                if (buffer != null) {
                    buffer.append(eq.getName()).append(" must be placed in a torso location.\n");
                }
                return false;
            }
        }
        return true;
    }
}
