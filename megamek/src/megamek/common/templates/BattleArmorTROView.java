/*
 * MegaMek - Copyright (C) 2018 - The MegaMek Team
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
package megamek.common.templates;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import megamek.common.AmmoType;
import megamek.common.BattleArmor;
import megamek.common.EntityMovementMode;
import megamek.common.EntityWeightClass;
import megamek.common.EquipmentType;
import megamek.common.Messages;
import megamek.common.MiscType;
import megamek.common.Mounted;
import megamek.common.TechConstants;
import megamek.common.verifier.EntityVerifier;
import megamek.common.verifier.TestBattleArmor;
import megamek.common.weapons.InfantryAttack;

/**
 * Creates a TRO template model for BattleArmor.
 *
 * @author Neoancient
 *
 */
public class BattleArmorTROView extends TROView {

    private final BattleArmor ba;

    public BattleArmorTROView(BattleArmor ba) {
        this.ba = ba;
    }

    @Override
    protected String getTemplateFileName(boolean html) {
        if (html) {
            return "ba.ftlh";
        }
        return "ba.ftl";
    }

    @Override
    protected void initModel(EntityVerifier verifier) {
        addBasicData(ba);
        addEntityFluff(ba);
        setModelData("formatBasicDataRow", new FormatTableRowMethod(new int[] { 25, 20, 8, 8 }, new Justification[] {
                Justification.LEFT, Justification.LEFT, Justification.CENTER, Justification.RIGHT }));
        final TestBattleArmor testBA = new TestBattleArmor(ba, verifier.baOption, null);
        if (ba.getChassisType() == BattleArmor.CHASSIS_TYPE_QUAD) {
            setModelData("chassisType", Messages.getString("TROView.chassisQuad"));
        } else {
            setModelData("chassisType", Messages.getString("TROView.chassisBiped"));
        }
        setModelData("weightClass",
                EntityWeightClass.getClassName(EntityWeightClass.getWeightClass(ba.getTrooperWeight(), ba)));
        setModelData("weight", ba.getTrooperWeight() * 1000);
        setModelData("swarmAttack", ba.canMakeAntiMekAttacks() ? "Yes" : "No");
        // We need to allow it for UMU that otherwise qualifies
        setModelData("legAttack",
                (ba.canDoMechanizedBA() && (ba.getWeightClass() < EntityWeightClass.WEIGHT_HEAVY)) ? "Yes" : "No");
        setModelData("mechanized", ba.canDoMechanizedBA() ? "Yes" : "No");
        setModelData("antiPersonnel", ba.getEquipment().stream().anyMatch(Mounted::isAPMMounted) ? "Yes" : "No");

        setModelData("massChassis", testBA.getWeightChassis() * 1000);
        setModelData("groundMP", ba.getWalkMP());
        setModelData("groundMass", testBA.getWeightGroundMP() * 1000);
        if (ba.getMovementMode() == EntityMovementMode.VTOL) {
            setModelData("vtolMP", ba.getOriginalJumpMP());
            setModelData("vtolMass", testBA.getWeightSecondaryMotiveSystem() * 1000);
        } else if (ba.getMovementMode() == EntityMovementMode.INF_UMU) {
            setModelData("umuMP", ba.getOriginalJumpMP());
            setModelData("umuMass", testBA.getWeightSecondaryMotiveSystem() * 1000);
        } else {
            setModelData("jumpMP", ba.getOriginalJumpMP());
            setModelData("jumpMass", testBA.getWeightSecondaryMotiveSystem() * 1000);
        }
        final List<Map<String, Object>> manipulators = new ArrayList<>();
        manipulators.add(formatManipulatorRow(BattleArmor.MOUNT_LOC_LARM, ba.getLeftManipulator()));
        manipulators.add(formatManipulatorRow(BattleArmor.MOUNT_LOC_RARM, ba.getRightManipulator()));
        setModelData("manipulators", manipulators);
        final String armorName = EquipmentType.getArmorTypeName(ba.getArmorType(BattleArmor.LOC_TROOPER_1),
                TechConstants.isClan(ba.getArmorTechLevel(BattleArmor.LOC_TROOPER_1)));
        final EquipmentType armor = EquipmentType.get(armorName);
        setModelData("armorType", armor == null ? "Unknown" : armor.getName().replaceAll("^BA\\s+", ""));
        setModelData("armorSlots", armor == null ? 0 : armor.getCriticals(ba));
        setModelData("armorMass", testBA.getWeightArmor() * 1000);
        setModelData("armorValue", ba.getOArmor(BattleArmor.LOC_TROOPER_1));
        setModelData("internal", ba.getOInternal(BattleArmor.LOC_TROOPER_1));
        final int nameWidth = addBAEquipment();
        setModelData("formatEquipmentRow",
                new FormatTableRowMethod(new int[] { nameWidth, 8, 12, 8 }, new Justification[] { Justification.LEFT,
                        Justification.CENTER, Justification.CENTER, Justification.CENTER }));
        if (ba.getEquipment().stream().anyMatch(m -> m.getBaMountLoc() == BattleArmor.MOUNT_LOC_TURRET)) {
            final Map<String, Object> modularMount = new HashMap<>();
            modularMount.put("name", ba.hasModularTurretMount() ? Messages.getString("TROView.BAModularTurret")
                    : Messages.getString("TROView.BATurret"));
            modularMount.put("location", BattleArmor.getBaMountLocAbbr(BattleArmor.MOUNT_LOC_TURRET));
            int turretSlots = ba.getTurretCapacity();
            if (ba.hasModularTurretMount()) {
                turretSlots += 2;
            }
            modularMount.put("slots", turretSlots + " (" + ba.getTurretCapacity() + ")");
            modularMount.put("mass", testBA.getWeightTurret() * 1000);
            setModelData("modularMount", modularMount);
        }
    }

    private Map<String, Object> formatManipulatorRow(int mountLoc, Mounted manipulator) {
        final Map<String, Object> retVal = new HashMap<>();
        retVal.put("locName", BattleArmor.getBaMountLocAbbr(mountLoc));
        if (null == manipulator) {
            retVal.put("eqName", Messages.getString("TROView.None"));
            retVal.put("eqMass", 0);
        } else {
            String name = manipulator.getName();
            if (name.contains("[")) {
                name = name.replaceAll(".*\\[", "").replaceAll("].*", "");
            }
            retVal.put("eqName", name);
            retVal.put("eqMass", manipulator.getTonnage() * 1000);
        }
        return retVal;
    }

    private int addBAEquipment() {
        final List<Map<String, Object>> equipment = new ArrayList<>();
        final List<Map<String, Object>> modularEquipment = new ArrayList<>();
        final String at = EquipmentType.getBaArmorTypeName(ba.getArmorType(BattleArmor.LOC_TROOPER_1),
                TechConstants.isClan(ba.getArmorTechLevel(BattleArmor.LOC_TROOPER_1)));
        final EquipmentType armor = EquipmentType.get(at);
        Map<String, Object> row;
        int nameWidth = 30;
        for (final Mounted m : ba.getEquipment()) {
            if (m.isAPMMounted() || (m.getType() instanceof InfantryAttack)
                    || (m.getType() == armor) || (m.getLocation() == BattleArmor.LOC_NONE)) {
                continue;
            }
            if ((m.getType() instanceof MiscType) && m.getType().hasFlag(MiscType.F_BA_MANIPULATOR)) {
                continue;
            }
            row = new HashMap<>();
            final String name = stripNotes(m.getName());
            if (m.getType() instanceof AmmoType) {
                row.put("name", name.replaceAll("^BA\\s+", "") + " (" + m.getOriginalShots() + ")");
            } else {
                row.put("name", stripNotes(m.getName()));
            }
            row.put("location", BattleArmor.getBaMountLocAbbr(m.getBaMountLoc()));
            if (name.length() >= nameWidth) {
                nameWidth = name.length() + 1;
            }
            row.put("slots", m.getCriticals());
            if (m.getType() instanceof AmmoType) {
                row.put("mass", ((AmmoType) m.getType()).getKgPerShot() * m.getOriginalShots());
            } else {
                row.put("mass", m.getTonnage() * 1000);
            }
            if (m.getBaMountLoc() == BattleArmor.MOUNT_LOC_TURRET) {
                row.put("location", "-");
                modularEquipment.add(row);
            } else {
                equipment.add(row);
            }
        }
        setModelData("equipment", equipment);
        setModelData("modularEquipment", modularEquipment);
        return nameWidth;
    }

}
