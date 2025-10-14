/*
 * Copyright (C) 2018-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.templates;

import java.text.NumberFormat;
import java.util.StringJoiner;

import megamek.common.Messages;
import megamek.common.equipment.Engine;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.Mounted;
import megamek.common.units.Entity;
import megamek.common.units.LandAirMek;
import megamek.common.units.Mek;
import megamek.common.units.QuadVee;
import megamek.common.units.System;
import megamek.common.verifier.EntityVerifier;
import megamek.common.verifier.TestMek;

/**
 * Creates a TRO template model for BattleMeks, OmniMeks, and IndustrialMeks of all leg configurations.
 *
 * @author Neoancient
 */
public class MekTROView extends TROView {

    private final Mek mek;

    public MekTROView(Mek mek) {
        this.mek = mek;
    }

    @Override
    protected String getTemplateFileName(boolean html) {
        if (html) {
            return "mek.ftlh";
        }
        return "mek.ftl";
    }

    @Override
    protected void initModel(EntityVerifier verifier) {
        setModelData("formatArmorRow", new FormatTableRowMethod(new int[] { 20, 10, 10 },
              new Justification[] { Justification.LEFT, Justification.CENTER, Justification.CENTER }));
        addBasicData(mek);
        addArmorAndStructure();
        final int nameWidth = addEquipment(mek);
        setModelData("formatEquipmentRow",
              new FormatTableRowMethod(new int[] { nameWidth, 12, 8, 10, 8 },
                    new Justification[] { Justification.LEFT, Justification.CENTER, Justification.CENTER,
                                          Justification.CENTER, Justification.CENTER }));
        addFluff();
        mek.setConversionMode(0);
        setModelData("isOmni", mek.isOmni());
        setModelData("isQuad", mek.hasETypeFlag(Entity.ETYPE_QUAD_MEK));
        setModelData("isTripod", mek.hasETypeFlag(Entity.ETYPE_TRIPOD_MEK));
        final TestMek testMek = new TestMek(mek, verifier.mekOption, null);
        setModelData("structureName", mek.getStructureType() == EquipmentType.T_STRUCTURE_STANDARD ? ""
              : EquipmentType.getStructureTypeName(mek.getStructureType()));
        setModelData("isMass", NumberFormat.getInstance().format(testMek.getWeightStructure()));
        setModelData("engineName", stripNotes(mek.getEngine().getEngineName()));
        setModelData("engineMass", NumberFormat.getInstance().format(testMek.getWeightEngine()));
        setModelData("walkMP", mek.getWalkMP());
        setModelData("runMP", mek.getRunMPasString());
        setModelData("jumpMP", mek.getJumpMP());
        setModelData("jumpBoosterMP", mek.getMechanicalJumpBoosterMP());
        setModelData("hsType", mek.getHeatSinkTypeName());
        setModelData("hsCount",
              mek.hasDoubleHeatSinks() ? mek.heatSinks() + " [" + (mek.heatSinks() * 2) + "]" : mek.heatSinks());
        if (mek.hasRiscHeatSinkOverrideKit()) {
            setModelData("riscKit", true);
        }
        setModelData("hsMass", NumberFormat.getInstance().format(testMek.getWeightHeatSinks()));
        if (mek.getGyroType() == Mek.GYRO_STANDARD) {
            setModelData("gyroType", mek.getRawSystemName(Mek.SYSTEM_GYRO));
        } else {
            setModelData("gyroType", Mek.getGyroDisplayString(mek.getGyroType()));
        }
        setModelData("gyroMass", NumberFormat.getInstance().format(testMek.getWeightGyro()));
        if ((mek.getCockpitType() == Mek.COCKPIT_STANDARD) || (mek.getCockpitType() == Mek.COCKPIT_INDUSTRIAL)) {
            setModelData("cockpitType", mek.getRawSystemName(Mek.SYSTEM_COCKPIT));
        } else {
            setModelData("cockpitType", Mek.getCockpitDisplayString(mek.getCockpitType()));
        }
        setModelData("cockpitMass", NumberFormat.getInstance().format(testMek.getWeightCockpit()));
        final String atName = formatArmorType(mek, true);
        if (!atName.isBlank()) {
            setModelData("armorType", " (" + atName + ")");
        } else {
            setModelData("armorType", "");
        }
        setModelData("armorFactor", mek.getTotalOArmor());
        setModelData("armorMass", NumberFormat.getInstance().format(testMek.getWeightArmor()));
        if (mek.isOmni()) {
            addFixedOmni(mek);
        }
        if (mek.hasETypeFlag(Entity.ETYPE_LAND_AIR_MEK)) {
            final LandAirMek lam = (LandAirMek) mek;
            final int mode = lam.getConversionMode();
            setModelData("lamConversionMass", testMek.getWeightMisc());
            if (lam.getLAMType() == LandAirMek.LAM_STANDARD) {
                setModelData("airmekCruise", lam.getAirMekCruiseMP());
                setModelData("airmekFlank", lam.getAirMekFlankMP());
            } else {
                setModelData("airmekCruise", "N/A");
                setModelData("airmekFlank", "N/A");
            }
            lam.setConversionMode(LandAirMek.CONV_MODE_FIGHTER);
            setModelData("safeThrust", lam.getWalkMP());
            setModelData("maxThrust", lam.getRunMP());
            lam.setConversionMode(mode);
        } else if (mek.hasETypeFlag(Entity.ETYPE_QUADVEE)) {
            final QuadVee qv = (QuadVee) mek;
            final int mode = qv.getConversionMode();
            qv.setConversionMode(QuadVee.CONV_MODE_VEHICLE);
            setModelData("qvConversionMass", testMek.getWeightMisc());
            setModelData("qvType", Messages.getString("MovementType." + qv.getMovementModeAsString()));
            setModelData("qvCruise", qv.getWalkMP());
            setModelData("qvFlank", qv.getRunMPasString());
            qv.setConversionMode(mode);
        }
        setModelData("rightArmActuators", countArmActuators(Mek.LOC_RIGHT_ARM));
        setModelData("leftArmActuators", countArmActuators(Mek.LOC_LEFT_ARM));
    }

    private String countArmActuators(int location) {
        final StringJoiner sj = new StringJoiner(", ");
        for (int act = Mek.ACTUATOR_SHOULDER; act <= Mek.ACTUATOR_HAND; act++) {
            if (mek.hasSystem(act, location)) {
                sj.add(mek.getRawSystemName(act));
            }
        }
        return sj.toString();
    }

    protected void addFluff() {
        addMekVeeAeroFluff(mek);
        setModelData("chassisDesc",
              formatSystemFluff(System.CHASSIS, mek.getFluff(), this::formatChassisDesc));
        setModelData("jjDesc", formatSystemFluff(System.JUMP_JET, mek.getFluff(), this::formatJJDesc));
        setModelData("jumpCapacity", mek.getJumpMP() * 30);
        setModelData("jumpBoosterCapacity", mek.getMechanicalJumpBoosterMP() * 30);
    }

    private static final int[][] MEK_ARMOR_LOCS = { { Mek.LOC_HEAD }, { Mek.LOC_CENTER_TORSO },
                                                    { Mek.LOC_RIGHT_TORSO, Mek.LOC_LEFT_TORSO },
                                                    { Mek.LOC_RIGHT_ARM, Mek.LOC_LEFT_ARM },
                                                    { Mek.LOC_RIGHT_LEG, Mek.LOC_CENTER_LEG, Mek.LOC_LEFT_LEG } };

    private static final int[][] MEK_ARMOR_LOCS_REAR = { { Mek.LOC_CENTER_TORSO },
                                                         { Mek.LOC_RIGHT_TORSO, Mek.LOC_LEFT_TORSO } };

    private void addArmorAndStructure() {
        setModelData("structureValues",
              addArmorStructureEntries(mek, Entity::getOInternal, MEK_ARMOR_LOCS));
        setModelData("armorValues", addArmorStructureEntries(mek, Entity::getOArmor, MEK_ARMOR_LOCS));
        setModelData("rearArmorValues",
              addArmorStructureEntries(mek, (en, loc) -> en.getOArmor(loc, true), MEK_ARMOR_LOCS_REAR));
        if (mek.hasPatchworkArmor()) {
            setModelData("patchworkByLoc", addPatchworkATs(mek, MEK_ARMOR_LOCS));
        }
    }

    private String formatChassisDesc() {
        String chassisDesc = EquipmentType.getStructureTypeName(mek.getStructureType());
        if (mek.isIndustrial()) {
            chassisDesc += Messages.getString("TROView.chassisIndustrial");
        }
        if (mek.isSuperHeavy()) {
            chassisDesc += Messages.getString("TROView.chassisSuperheavy");
        }
        if (mek.hasETypeFlag(Entity.ETYPE_QUADVEE)) {
            chassisDesc += Messages.getString("TROView.chassisQuadVee");
        } else if (mek.hasETypeFlag(Entity.ETYPE_QUAD_MEK)) {
            chassisDesc += Messages.getString("TROView.chassisQuad");
        } else if (mek.hasETypeFlag(Entity.ETYPE_TRIPOD_MEK)) {
            chassisDesc += Messages.getString("TROView.chassisTripod");
        } else if (mek.hasETypeFlag(Entity.ETYPE_LAND_AIR_MEK)) {
            chassisDesc += Messages.getString("TROView.chassisLAM");
        } else {
            chassisDesc += Messages.getString("TROView.chassisBiped");
        }
        return chassisDesc;
    }

    private String formatJJDesc() {
        return switch (mek.getJumpType()) {
            case Mek.JUMP_STANDARD -> Messages.getString("TROView.jjStandard");
            case Mek.JUMP_IMPROVED -> Messages.getString("TROView.jjImproved");
            case Mek.JUMP_PROTOTYPE -> Messages.getString("TROView.jjPrototype");
            case Mek.JUMP_PROTOTYPE_IMPROVED -> Messages.getString("TROView.jjImpPrototype");
            default -> Messages.getString("TROView.jjNone");
        };
    }

    @Override
    protected boolean showFixedSystem(Entity entity, int index, int loc) {
        return ((index != Mek.SYSTEM_COCKPIT) || (loc != Mek.LOC_HEAD))
              && ((index != Mek.SYSTEM_SENSORS) || (loc != Mek.LOC_HEAD))
              && ((index != Mek.SYSTEM_LIFE_SUPPORT) || (loc != Mek.LOC_HEAD))
              && ((index != Mek.SYSTEM_ENGINE) || (loc != Mek.LOC_CENTER_TORSO)) && (index != Mek.SYSTEM_GYRO)
              && (index != Mek.ACTUATOR_SHOULDER) && (index != Mek.ACTUATOR_UPPER_ARM)
              && (index != Mek.ACTUATOR_LOWER_ARM) && (index != Mek.ACTUATOR_HAND) && (index != Mek.ACTUATOR_HIP)
              && (index != Mek.ACTUATOR_UPPER_LEG) && (index != Mek.ACTUATOR_LOWER_LEG)
              && (index != Mek.ACTUATOR_FOOT);
    }

    @Override
    protected String getSystemName(Entity entity, int index) {
        // Here we're only concerned with engines that take extra critical slots in the
        // side torso
        if (index == Mek.SYSTEM_ENGINE) {
            final StringBuilder sb = new StringBuilder();
            if (entity.getEngine().hasFlag(Engine.LARGE_ENGINE)) {
                sb.append("Large ");
            }
            switch (entity.getEngine().getEngineType()) {
                case Engine.XL_ENGINE:
                    sb.append("XL");
                    break;
                case Engine.LIGHT_ENGINE:
                    sb.append("Light");
                    break;
                case Engine.XXL_ENGINE:
                    sb.append("XXL");
                    break;
            }
            sb.append(" Engine");
            return sb.toString();
        } else {
            return ((Mek) entity).getRawSystemName(index);
        }
    }

    @Override
    protected String formatLocationTableEntry(Entity entity, Mounted<?> mounted) {
        String loc = entity.getLocationAbbr(mounted.getLocation());
        if (mounted.isRearMounted()) {
            loc += "(R)";
        }
        return loc;
    }

    @Override
    protected boolean skipMount(Mounted<?> mount, boolean includeAmmo) {
        if (mount.getLocation() == Entity.LOC_NONE) {
            // Skip heat sinks, Clan CASE, armor, and structure. We do want to show things
            // like robotic control systems.
            return (mount.getNumCriticalSlots() > 0)
                  || mount.getType().hasFlag(MiscType.F_CASE)
                  || EquipmentType.isArmorType(mount.getType())
                  || EquipmentType.isStructureType(mount.getType());
        }
        return super.skipMount(mount, includeAmmo);
    }
}
