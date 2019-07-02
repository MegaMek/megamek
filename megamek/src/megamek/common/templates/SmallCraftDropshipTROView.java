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
import java.util.Map;

import megamek.common.Aero;
import megamek.common.Entity;
import megamek.common.Messages;
import megamek.common.Mounted;
import megamek.common.SmallCraft;
import megamek.common.verifier.EntityVerifier;
import megamek.common.verifier.TestSmallCraft;

/**
 * Creates a TRO template model for small craft and dropships
 *
 * @author Neoancient
 *
 */
public class SmallCraftDropshipTROView extends AeroTROView {

    private final SmallCraft aero;

    public SmallCraftDropshipTROView(SmallCraft aero) {
        super(aero);
        this.aero = aero;
    }

    @Override
    protected String getTemplateFileName(boolean html) {
        if (html) {
            return "aero_vessel.ftlh";
        }
        return "aero_vessel.ftl";
    }

    @Override
    protected void initModel(EntityVerifier verifier) {
        addBasicData(aero);
        addArmor();
        setModelData("formatBayRow", new FormatTableRowMethod(new int[] { 8, 24, 10 },
                new Justification[] { Justification.LEFT, Justification.LEFT, Justification.LEFT }));
        setModelData("usesWeaponBays", aero.usesWeaponBays());
        if (aero.usesWeaponBays()) {
            final int nameWidth = addWeaponBays(aero.isSpheroid() ? SPHEROID_ARCS : AERODYNE_ARCS);
            setModelData("formatWeaponBayRow",
                    new FormatTableRowMethod(new int[] { nameWidth, 5, 8, 8, 8, 8, 12 },
                            new Justification[] { Justification.LEFT, Justification.CENTER, Justification.CENTER,
                                    Justification.CENTER, Justification.CENTER, Justification.CENTER,
                                    Justification.LEFT }));
        } else {
            final int nameWidth = addEquipment(aero, false);
            setModelData("formatEquipmentRow",
                    new FormatTableRowMethod(new int[] { nameWidth, 12, 8, 8, 5, 5, 5, 5, 5 },
                            new Justification[] { Justification.LEFT, Justification.CENTER, Justification.CENTER,
                                    Justification.CENTER, Justification.CENTER, Justification.CENTER,
                                    Justification.CENTER, Justification.CENTER, Justification.CENTER }));
        }
        addFluff();
        final TestSmallCraft testAero = new TestSmallCraft(aero, verifier.aeroOption, null);

        setModelData("typeDesc", formatVesselType());
        setModelData("massDesc", aero.getWeight());
        setModelData("fuelMass", aero.getFuelTonnage());
        setModelData("fuelPoints", aero.getFuel());
        setModelData("safeThrust", aero.getWalkMP());
        setModelData("maxThrust", aero.getRunMP());
        setModelData("hsCount",
                aero.getHeatType() == Aero.HEAT_DOUBLE ? aero.getOHeatSinks() + " (" + (aero.getOHeatSinks() * 2) + ")"
                        : aero.getOHeatSinks());
        setModelData("si", aero.get0SI());
        setModelData("armorType", formatArmorType(aero, false).toLowerCase());
        setModelData("armorMass", testAero.getWeightArmor());
        setModelData("escapePods", aero.getEscapePods());
        setModelData("lifeBoats", aero.getLifeBoats());

        addTransportBays(aero);
        addAmmo();
        addCrew();
    }

    private void addFluff() {
        addEntityFluff(aero);
        addEntityFluff(aero);
        final Map<String, String> dimensions = new HashMap<>();
        if (aero.getFluff().getLength().length() > 0) {
            dimensions.put("length", aero.getFluff().getLength());
        }
        if (aero.getFluff().getWidth().length() > 0) {
            dimensions.put("width", aero.getFluff().getWidth());
        }
        if (aero.getFluff().getHeight().length() > 0) {
            dimensions.put("height", aero.getFluff().getHeight());
        }
        if (!dimensions.isEmpty()) {
            setModelData("dimensions", dimensions);
        }
        if (aero.getFluff().getUse().length() > 0) {
            setModelData("use", aero.getFluff().getUse());
        }
    }

    private String formatVesselType() {
        return ((aero.getDesignType() == Aero.CIVILIAN) ? Messages.getString("TROView.Civilian")
                : Messages.getString("TROView.Military")) + " "
                + (aero.isSpheroid() ? Messages.getString("TROView.Spheroid") : Messages.getString("TROView.Aerodyne"));
    }

    private static final String[][] SPHEROID_ARCS = { { "Nose" }, { "RS Fwd", "LS Fwd" }, { "RS Aft", "LS Aft" },
            { "Aft" } };

    private static final String[][] AERODYNE_ARCS = { { "Nose" }, { "RW", "LW" }, { "RW Aft", "LW Aft" }, { "Aft" } };

    @Override
    protected String getArcAbbr(Mounted m) {
        final String[][] arcs = aero.isSpheroid() ? SPHEROID_ARCS : AERODYNE_ARCS;
        switch (m.getLocation()) {
            case Aero.LOC_NOSE:
                return arcs[0][0];
            case Aero.LOC_RWING:
                return arcs[m.isRearMounted() ? 2 : 1][0];
            case Aero.LOC_LWING:
                return arcs[m.isRearMounted() ? 2 : 1][1];
            case Aero.LOC_AFT:
                return arcs[3][0];
        }
        return super.getArcAbbr(m);
    }

    private static final int[][] SC_ARMOR_LOCS = { { SmallCraft.LOC_NOSE },
            { SmallCraft.LOC_RWING, SmallCraft.LOC_LWING }, { Aero.LOC_AFT } };

    private void addArmor() {
        setModelData("armorValues", addArmorStructureEntries(aero, (en, loc) -> en.getOArmor(loc), SC_ARMOR_LOCS));
    }

    @Override
    protected String formatLocationTableEntry(Entity entity, Mounted mounted) {
        String str = null;
        if (mounted.getLocation() == Aero.LOC_RWING) {
            str = "TROView.RS";
        } else if (mounted.getLocation() == Aero.LOC_LWING) {
            str = "TROView.LS";
        } else {
            return aero.getLocationName(mounted.getLocation());
        }
        if (!aero.isSpheroid()) {
            str += "Wing";
        }
        if (mounted.isRearMounted()) {
            str += "R";
        }
        return Messages.getString(str);
    }

    protected void addCrew() {
        setModelData("crew", new ArrayList<>());
        if (aero.getNOfficers() > 0) {
            addCrewEntry("officer", aero.getNOfficers());
        }
        final int nEnlisted = aero.getNCrew() - aero.getBayPersonnel() - aero.getNGunners() - aero.getNOfficers();
        if (nEnlisted > 0) {
            addCrewEntry("enlisted", nEnlisted);
        }
        if (aero.getNGunners() > 0) {
            addCrewEntry("gunner", aero.getNGunners());
        }
        if (aero.getBayPersonnel() > 0) {
            addCrewEntry("bayPersonnel", aero.getBayPersonnel());
        }
        if (aero.getNPassenger() > 0) {
            addCrewEntry("passenger", aero.getNPassenger());
        }
        if (aero.getNMarines() > 0) {
            addCrewEntry("marine", aero.getNMarines());
        }
        if (aero.getNBattleArmor() > 0) {
            addCrewEntry("baMarine", aero.getNBattleArmor());
        }
    }

}
