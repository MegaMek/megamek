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

import megamek.common.EntityFluff;
import megamek.common.Messages;
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
        setModelData("formatArmorRow", new FormatTableRowMethod(new int[] { 20, 10, 10},
                new Justification[] { Justification.LEFT, Justification.CENTER, Justification.CENTER }));
        addBasicData(proto);
        addArmorAndStructure();
        int nameWidth = addEquipment(proto);
        setModelData("formatEquipmentRow", new FormatTableRowMethod(new int[] { nameWidth, 12, 10},
                new Justification[] { Justification.LEFT, Justification.CENTER, Justification.CENTER}));
        addFluff();
        setModelData("isGlider", proto.isGlider());
        TestProtomech testproto = new TestProtomech(proto, verifier.protomechOption, null);
        setModelData("isMass", NumberFormat.getInstance().format(testproto.getWeightStructure() * 1000));
        setModelData("engineRating", proto.getEngine().getRating());
        setModelData("engineMass", NumberFormat.getInstance().format(testproto.getWeightEngine() * 1000));
        setModelData("walkMP", proto.getWalkMP());
        setModelData("runMP", proto.getRunMPasString());
        setModelData("jumpMP", proto.getJumpMP());
        setModelData("hsCount", testproto.getCountHeatSinks());
        setModelData("hsMass", NumberFormat.getInstance().format(testproto.getWeightHeatSinks() * 1000));
        setModelData("cockpitMass", NumberFormat.getInstance().format(testproto.getWeightControls() * 1000));
        String atName = formatArmorType(proto, true);
        if (atName.length() > 0) {
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
            setModelData("chassisDesc", formatSystemFluff(EntityFluff.System.CHASSIS,
                    proto.getFluff(), () -> ""));
        }
        if (!proto.isGlider()) {
            setModelData("jjDesc", formatSystemFluff(EntityFluff.System.JUMPJET,
                    proto.getFluff(), () -> ""));
            setModelData("jumpCapacity", proto.getJumpMP() * 30);
        }
        if (proto.isGlider()) {
            setModelData("configurationDesc", Messages.getString("TROView.ProtoGlider"));
        } else if (proto.isQuad()) {
            setModelData("configurationDesc", Messages.getString("TROView.ProtoQuad"));
        }
    }
    private static final int[][] PROTO_ARMOR_LOCS = {
            {Protomech.LOC_HEAD}, {Protomech.LOC_TORSO}, {Protomech.LOC_RARM, Protomech.LOC_LARM},
            {Protomech.LOC_LEG}, {Protomech.LOC_MAINGUN}
    };

    private void addArmorAndStructure() {
        setModelData("structureValues", addArmorStructureEntries(proto,
                (en, loc) -> en.getOInternal(loc),
                PROTO_ARMOR_LOCS));
        setModelData("armorValues", addArmorStructureEntries(proto,
                (en, loc) -> en.getOArmor(loc),
                PROTO_ARMOR_LOCS));
    }


}

