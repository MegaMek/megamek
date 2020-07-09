/*
 * MegaMek -
 * Copyright (C) 2020 The MegaMek Team
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 */

package megamek.common.templates;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import megamek.common.AmmoType;
import megamek.common.Entity;
import megamek.common.EntityWeightClass;
import megamek.common.EquipmentType;
import megamek.common.LargeSupportTank;
import megamek.common.Messages;
import megamek.common.MiscType;
import megamek.common.Mounted;
import megamek.common.Tank;
import megamek.common.VTOL;
import megamek.common.verifier.EntityVerifier;
import megamek.common.verifier.TestSupportVehicle;

/**
 * Creates a TRO template model for support vehicles.
 *
 * @author Neoancient
 *
 */
public class SupportVeeTROView extends TROView {

    private final Tank tank;
    private final boolean kgStandard;

    public SupportVeeTROView(Tank tank) {
        this.tank = tank;
        kgStandard = tank.getWeight() <= 5.0;
    }

    private double adjustWeight(double tonnage) {
        if (kgStandard) {
            return tonnage * 1000;
        } else {
            return tonnage;
        }
    }

    @Override
    protected String getTemplateFileName(boolean html) {
        if (html) {
            return "support.ftlh";
        }
        return "support.ftl";
    }

    @Override
    protected void initModel(EntityVerifier verifier) {
        setModelData("formatArmorRow", new FormatTableRowMethod(new int[] { 20, 10, 10 },
                new Justification[] { Justification.LEFT, Justification.CENTER, Justification.CENTER }));
        addBasicData(tank);
        addArmorAndStructure();
        final int nameWidth = addEquipment(tank);
        setModelData("formatEquipmentRow",
                new FormatTableRowMethod(new int[] { nameWidth, 12, 12 }, new Justification[] { Justification.LEFT,
                        Justification.CENTER, Justification.CENTER, Justification.CENTER, Justification.CENTER }));
        setModelData("formatBayRow", new FormatTableRowMethod(new int[] { 8, 24, 10 },
                new Justification[] { Justification.LEFT, Justification.LEFT, Justification.LEFT }));
        addFluff();
        addTransportBays(tank);
        final TestSupportVehicle testTank = new TestSupportVehicle(tank, verifier.tankOption, null);
        setModelData("isOmni", tank.isOmni());
        setModelData("isVTOL", tank.hasETypeFlag(Entity.ETYPE_VTOL));
        setModelData("isSuperheavy", tank.isSuperHeavy());
        setModelData("isSupport", tank.isSupportVehicle());
        setModelData("hasTurret", !tank.hasNoTurret());
        setModelData("hasTurret2", !tank.hasNoDualTurret());
        setModelData("weightStandard", Messages.getString(kgStandard ? "TROView.kg" : "TROView.tons").replace(" ", ""));
        setModelData("moveType", Messages.getString("MovementType." + tank.getMovementModeAsString()));
        setModelData("mass", adjustWeight(tank.getWeight()));
        setModelData("weightClass", EntityWeightClass
                .getClassName(EntityWeightClass.getWeightClass(tank.getWeight(), tank)).replaceAll("\\s.*", ""));
        setModelData("chassisControlMass", adjustWeight(testTank.getWeightStructure() + testTank.getWeightControls()));
        setModelData("engineName", stripNotes(tank.getEngine().getEngineName()));
        setModelData("engineMass", adjustWeight(testTank.getWeightEngine()));
        setModelData("walkMP", tank.getWalkMP());
        setModelData("runMP", tank.getRunMPasString());
        setModelData("hsCount",
                Math.max(testTank.getCountHeatSinks(), tank.getEngine().getWeightFreeEngineHeatSinks()));
        setModelData("hsMass", adjustWeight(testTank.getWeightHeatSinks()));
        setModelData("amplifierMass", adjustWeight(testTank.getWeightPowerAmp()));
        setModelData("turretMass", adjustWeight(testTank.getTankWeightTurret()));
        setModelData("turretMass2", adjustWeight(testTank.getTankWeightDualTurret()));
        setModelData("barRating", formatArmorType(tank, true));
        setModelData("armorFactor", tank.getTotalOArmor());
        setModelData("armorMass", adjustWeight(testTank.getWeightArmor()));
        setModelData("fuelRange", ""); // We do not yet record the data to compute range from tonnage.
        setModelData("fuelMass", adjustWeight(tank.getFuelTonnage()));
        if (tank.isOmni()) {
            addFixedOmni(tank);
        }
    }

    private void addFluff() {
        addMechVeeAeroFluff(tank);
    }

    private static final int[][] TANK_ARMOR_LOCS = { { Tank.LOC_FRONT }, { Tank.LOC_RIGHT, Tank.LOC_LEFT },
            { Tank.LOC_REAR }, { Tank.LOC_TURRET }, { Tank.LOC_TURRET_2 }, { VTOL.LOC_ROTOR } };

    private static final int[][] LARGE_SUPPORT_ARMOR_LOCS = { { LargeSupportTank.LOC_FRONT },
            { LargeSupportTank.LOC_FRONTRIGHT, LargeSupportTank.LOC_FRONTLEFT },
            { LargeSupportTank.LOC_REARRIGHT, LargeSupportTank.LOC_REARLEFT }, { LargeSupportTank.LOC_REAR },
            { LargeSupportTank.LOC_TURRET }, { LargeSupportTank.LOC_TURRET_2 } };

    private void addArmorAndStructure() {
        if (tank.hasETypeFlag(Entity.ETYPE_LARGE_SUPPORT_TANK)) {
            setModelData("structureValues",
                    addArmorStructureEntries(tank, Entity::getOInternal, LARGE_SUPPORT_ARMOR_LOCS));
            setModelData("armorValues",
                    addArmorStructureEntries(tank, Entity::getOArmor, LARGE_SUPPORT_ARMOR_LOCS));
        } else {
            setModelData("structureValues",
                    addArmorStructureEntries(tank, Entity::getOInternal, TANK_ARMOR_LOCS));
            setModelData("armorValues",
                    addArmorStructureEntries(tank, Entity::getOArmor, TANK_ARMOR_LOCS));
        }
    }

    @Override
    protected String formatLocationTableEntry(Entity entity, Mounted mounted) {
        if (mounted.isPintleTurretMounted()) {
            return Messages.getString("TROView.Pintle");
        } else if (mounted.isSponsonTurretMounted()) {
            return Messages.getString("TROView.Sponson");
        }
        return entity.getLocationName(mounted.getLocation());
    }

    @Override
    protected int addEquipment(Entity entity) {
        final Map<String, Map<EquipmentKey, Integer>> weapons = new HashMap<>();
        final List<String> chassisMods = new ArrayList<>();
        final Map<EquipmentKey, Integer> miscCount = new HashMap<>();
        int nameWidth = 20;
        for (final Mounted m : entity.getEquipment()) {
            if ((m.getLocation() < 0) || m.isWeaponGroup()) {
                continue;
            }
            if ((m.getType() instanceof MiscType) && (m.getLinked() == null) && (m.getLinkedBy() == null)) {
                if (m.getType().hasFlag(MiscType.F_CHASSIS_MODIFICATION)) {
                    chassisMods.add(m.getName().replaceAll(".*\\[", "").replace("]", ""));
                } else {
                    miscCount.merge(new EquipmentKey(m.getType(), m.getSize()), 1, Integer::sum);
                }
                continue;
            }
            if (m.isOmniPodMounted() || !entity.isOmni()) {
                final String loc = formatLocationTableEntry(entity, m);
                weapons.putIfAbsent(loc, new HashMap<>());
                weapons.get(loc).merge(new EquipmentKey(m.getType(), m.getSize()), 1, Integer::sum);
            }
        }
        final List<Map<String, Object>> weaponList = new ArrayList<>();
        for (final String loc : weapons.keySet()) {
            for (final Map.Entry<EquipmentKey, Integer> entry : weapons.get(loc).entrySet()) {
                final EquipmentType eq = entry.getKey().getType();
                final int count = weapons.get(loc).get(entry.getKey());
                String name = stripNotes(entry.getKey().name());
                if (eq instanceof AmmoType) {
                    name = String.format("%s (%d)", name, ((AmmoType) eq).getShots() * count);
                } else if (count > 1) {
                    name = String.format("%d %ss", count, entry.getKey().name());
                }
                final Map<String, Object> fields = new HashMap<>();
                fields.put("name", name);
                if (name.length() >= nameWidth) {
                    nameWidth = name.length() + 1;
                }
                fields.put("tonnage", adjustWeight(eq.getTonnage(entity, entry.getKey().getSize()) * count));
                fields.put("location", loc);
                fields.put("slots", eq.getCriticals(entity, entry.getValue()) * count);
                weaponList.add(fields);
            }
        }
        setModelData("weaponList", weaponList);
        setModelData("chassisMods", chassisMods);
        final List<String> miscEquipment = new ArrayList<>();
        for (final Map.Entry<EquipmentKey, Integer> entry : miscCount.entrySet()) {
            final EquipmentType eq = entry.getKey().getType();
            final int count = entry.getValue();
            final double tonnage = eq.getTonnage(tank, entry.getKey().getSize());
            final StringBuilder sb = new StringBuilder(stripNotes(entry.getKey().name()));
            if (tonnage > 0) {
                sb.append("(");
                if (entry.getValue() > 1) {
                    sb.append(entry.getValue()).append("; ");
                }
                sb.append(NumberFormat.getInstance().format(adjustWeight(tonnage)));
                if (kgStandard) {
                    sb.append(Messages.getString("TROView.kg"));
                } else if (tonnage == 1.0) {
                    sb.append(Messages.getString("TROView.ton"));
                } else {
                    sb.append(Messages.getString("TROView.tons"));
                }
                if (entry.getValue() > 1) {
                    sb.append(Messages.getString("TROView.each"));
                }
                sb.append(")");
            } else if (count > 1) {
                sb.append("(").append(count).append(")");
            }
            miscEquipment.add(sb.toString());
        }
        setModelData("miscEquipment", miscEquipment);
        return nameWidth;
    }

}
