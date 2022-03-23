/*
 * Copyright (C) 2018 - The MegaMek Team
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

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import megamek.common.AmmoType;
import megamek.common.Entity;
import megamek.common.EntityFluff;
import megamek.common.EquipmentType;
import megamek.common.Messages;
import megamek.common.MiscType;
import megamek.common.Mounted;
import megamek.common.Protomech;
import megamek.common.verifier.EntityVerifier;
import megamek.common.verifier.TestProtomech;

/**
 * Creates a TRO template model for Protomechs.
 *
 * @author Neoancient
 *
 */
public class ProtomechTROView extends TROView {

    private final Protomech proto;

    public ProtomechTROView(Protomech proto) {
        this.proto = proto;
    }

    @Override
    protected String getTemplateFileName(boolean html) {
        if (html) {
            return "protomech.ftlh";
        }
        return "protomech.ftl";
    }

    @Override
    protected void initModel(EntityVerifier verifier) {
        setModelData("formatArmorRow", new FormatTableRowMethod(new int[] { 20, 10, 10 },
                new Justification[] { Justification.LEFT, Justification.CENTER, Justification.CENTER }));
        addBasicData(proto);
        addArmorAndStructure();
        final int nameWidth = addEquipment(proto);
        setModelData("formatEquipmentRow", new FormatTableRowMethod(new int[] { nameWidth, 12, 10 },
                new Justification[] { Justification.LEFT, Justification.CENTER, Justification.RIGHT }));
        addFluff();
        setModelData("isGlider", proto.isGlider());
        setModelData("isQuad", proto.isQuad());
        final TestProtomech testproto = new TestProtomech(proto, verifier.protomechOption, null);
        setModelData("isMass", NumberFormat.getInstance().format(testproto.getWeightStructure() * 1000));
        setModelData("engineRating", proto.getEngine().getRating());
        setModelData("engineMass", NumberFormat.getInstance().format(testproto.getWeightEngine() * 1000));
        setModelData("walkMP", proto.getWalkMP());
        setModelData("runMP", proto.getRunMPasString());
        final List<Mounted> umu = proto.getMisc().stream().filter(m -> m.getType().hasFlag(MiscType.F_UMU))
                .collect(Collectors.toList());
        if (umu.isEmpty()) {
            setModelData("jumpMP", proto.getOriginalJumpMP());
            setModelData("jumpMass",
                    Math.round(1000 * proto.getMisc().stream().filter(m -> m.getType().hasFlag(MiscType.F_JUMP_JET))
                            .mapToDouble(Mounted::getTonnage).sum()));
        } else {
            setModelData("umuMP", umu.size());
            setModelData("umuMass",
                    Math.round(1000 * umu.stream().mapToDouble(Mounted::getTonnage).sum()));
        }
        setModelData("hsCount", testproto.getCountHeatSinks());
        setModelData("hsMass", NumberFormat.getInstance().format(testproto.getWeightHeatSinks() * 1000));
        setModelData("cockpitMass", NumberFormat.getInstance().format(testproto.getWeightControls() * 1000));
        final String atName = formatArmorType(proto, true);
        if (!atName.isBlank()) {
            setModelData("armorType", " (" + atName + ")");
        } else {
            setModelData("armorType", "");
        }
        setModelData("armorFactor", proto.getTotalOArmor());
        setModelData("armorMass", NumberFormat.getInstance().format(testproto.getWeightArmor() * 1000));
    }

    private void addFluff() {
        addMechVeeAeroFluff(proto);
        if (proto.getOriginalJumpMP() > 0) {
            setModelData("chassisDesc", formatSystemFluff(EntityFluff.System.CHASSIS, proto.getFluff(), () -> ""));
        }
        if (!proto.isGlider()) {
            setModelData("jjDesc", formatSystemFluff(EntityFluff.System.JUMPJET, proto.getFluff(), () -> ""));
            setModelData("jumpCapacity", proto.getJumpMP() * 30);
        }
        if (proto.isGlider()) {
            setModelData("configurationDesc", Messages.getString("TROView.ProtoGlider"));
        } else if (proto.isQuad()) {
            setModelData("configurationDesc", Messages.getString("TROView.ProtoQuad"));
        }
    }

    private static final int[][] PROTO_ARMOR_LOCS = { { Protomech.LOC_HEAD }, { Protomech.LOC_TORSO },
            { Protomech.LOC_RARM, Protomech.LOC_LARM }, { Protomech.LOC_LEG }, { Protomech.LOC_MAINGUN } };

    private void addArmorAndStructure() {
        setModelData("structureValues",
                addArmorStructureEntries(proto, Entity::getOInternal, PROTO_ARMOR_LOCS));
        setModelData("armorValues", addArmorStructureEntries(proto, Entity::getOArmor, PROTO_ARMOR_LOCS));
    }

    @Override
    protected int addEquipment(Entity entity, boolean includeAmmo) {
        final Map<String, Map<EquipmentKey, Integer>> equipment = new HashMap<>();
        int nameWidth = 20;
        for (final Mounted m : entity.getEquipment()) {
            if ((m.getLocation() < 0) || m.isWeaponGroup() || (!includeAmmo && (m.getType() instanceof AmmoType))) {
                continue;
            }
            if ((m.getType() instanceof MiscType)
                    && (m.getType().hasFlag(MiscType.F_JUMP_JET) || m.getType().hasFlag(MiscType.F_UMU))) {
                continue;
            }
            final String loc = formatLocationTableEntry(entity, m);
            equipment.putIfAbsent(loc, new HashMap<>());
            if (m.getType() instanceof AmmoType) {
                equipment.get(loc).merge(new EquipmentKey(m.getType(), m.getSize()), m.getBaseShotsLeft(), Integer::sum);
            } else {
                equipment.get(loc).merge(new EquipmentKey(m.getType(), m.getSize()), 1, Integer::sum);
            }
        }
        final List<Map<String, Object>> eqList = new ArrayList<>();
        for (final String loc : equipment.keySet()) {
            for (final Map.Entry<EquipmentKey, Integer> entry : equipment.get(loc).entrySet()) {
                final EquipmentType eq = entry.getKey().getType();
                final int count = equipment.get(loc).get(entry.getKey());
                String name = stripNotes(entry.getKey().name());
                if (eq instanceof AmmoType) {
                    name = String.format("%s (%d)", name, count);
                } else if (count > 1) {
                    name = String.format("%d %ss", count, entry.getKey().name());
                }
                final Map<String, Object> fields = new HashMap<>();
                fields.put("name", name);
                if (name.length() >= nameWidth) {
                    nameWidth = name.length() + 1;
                }
                if (eq instanceof AmmoType) {
                    fields.put("mass", Math.round((((AmmoType) eq).getKgPerShot()) * count));
                } else {
                    fields.put("mass", Math.round(eq.getTonnage(entity, entry.getKey().getSize()) * 1000 * count));
                }
                fields.put("location", loc);
                eqList.add(fields);
            }
        }
        setModelData("equipment", eqList);
        return nameWidth;
    }
}
