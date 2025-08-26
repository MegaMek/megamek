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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import megamek.common.Messages;
import megamek.common.equipment.Mounted;
import megamek.common.units.Aero;
import megamek.common.units.Entity;
import megamek.common.units.SmallCraft;
import megamek.common.verifier.EntityVerifier;
import megamek.common.verifier.TestSmallCraft;

/**
 * Creates a TRO template model for small craft and DropShips
 *
 * @author Neoancient
 */
public class SmallCraftDropshipTROView extends AeroTROView {
    private final SmallCraft aero;

    public SmallCraftDropshipTROView(SmallCraft aero) {
        super(aero);
        this.aero = aero;
    }

    @Override
    protected String getTemplateFileName(boolean html) {
        return html ? "aero_vessel.ftlh" : "aero_vessel.ftl";
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
        setModelData("si", aero.getOSI());
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
        if (!aero.getFluff().getLength().isEmpty()) {
            dimensions.put("length", aero.getFluff().getLength());
        }

        if (!aero.getFluff().getWidth().isEmpty()) {
            dimensions.put("width", aero.getFluff().getWidth());
        }

        if (!aero.getFluff().getHeight().isEmpty()) {
            dimensions.put("height", aero.getFluff().getHeight());
        }

        if (!dimensions.isEmpty()) {
            setModelData("dimensions", dimensions);
        }

        if (!aero.getFluff().getUse().isEmpty()) {
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
    protected String getArcAbbr(Mounted<?> m) {
        final String[][] arcs = aero.isSpheroid() ? SPHEROID_ARCS : AERODYNE_ARCS;
        return switch (m.getLocation()) {
            case Aero.LOC_NOSE -> arcs[0][0];
            case Aero.LOC_RIGHT_WING -> arcs[m.isRearMounted() ? 2 : 1][0];
            case Aero.LOC_LEFT_WING -> arcs[m.isRearMounted() ? 2 : 1][1];
            case Aero.LOC_AFT -> arcs[3][0];
            default -> super.getArcAbbr(m);
        };
    }

    private static final int[][] SC_ARMOR_LOCS = { { SmallCraft.LOC_NOSE },
                                                   { SmallCraft.LOC_RIGHT_WING, SmallCraft.LOC_LEFT_WING },
                                                   { Aero.LOC_AFT } };

    private void addArmor() {
        setModelData("armorValues", addArmorStructureEntries(aero, Entity::getOArmor, SC_ARMOR_LOCS));
    }

    @Override
    protected String formatLocationTableEntry(Entity entity, Mounted<?> mounted) {
        String str;
        if (mounted.getLocation() == Aero.LOC_RIGHT_WING) {
            str = "TROView.RS";
        } else if (mounted.getLocation() == Aero.LOC_LEFT_WING) {
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
