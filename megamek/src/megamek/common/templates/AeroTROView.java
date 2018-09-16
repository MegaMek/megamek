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

import java.text.NumberFormat;

import megamek.common.Aero;
import megamek.common.Entity;
import megamek.common.verifier.EntityVerifier;
import megamek.common.verifier.TestAero;

/**
 * Creates a TRO template model for aerospace and conventional fighters.
 * 
 * @author Neoancient
 *
 */
public class AeroTROView extends TROView {

	private final Aero aero;
	
	public AeroTROView(Aero aero) {
		this.aero = aero;
	}

	@Override
	protected String getTemplateFileName(boolean html) {
		if (html) {
			return "aero.ftlh";
		}
		return "aero.ftl";
	}

	@Override
	protected void initModel(EntityVerifier verifier) {
		setModelData("formatArmorRow", new FormatTableRowMethod(new int[] { 20, 10},
				new Justification[] { Justification.LEFT, Justification.CENTER }));
		addBasicData(aero);
		addArmorAndStructure();
		int nameWidth = addEquipment(aero);
		setModelData("formatEquipmentRow", new FormatTableRowMethod(new int[] { nameWidth, 12, 8, 8, 5, 5, 5, 5, 5},
				new Justification[] { Justification.LEFT, Justification.CENTER, Justification.CENTER,
						Justification.CENTER, Justification.CENTER, Justification.CENTER, Justification.CENTER, 
						Justification.CENTER, Justification.CENTER }));
		addFluff();
		setModelData("isOmni", aero.isOmni());
		setModelData("isConventional", aero.hasETypeFlag(Entity.ETYPE_CONV_FIGHTER));
		TestAero testAero = new TestAero(aero, verifier.aeroOption, null);
		setModelData("engineName", stripNotes(aero.getEngine().getEngineName()));
		setModelData("engineMass", NumberFormat.getInstance().format(testAero.getWeightEngine()));
		setModelData("safeThrust", aero.getWalkMP());
		setModelData("maxThrust", aero.getRunMP());
		setModelData("si", aero.get0SI());
		setModelData("hsCount", aero.getHeatType() == Aero.HEAT_DOUBLE?
				aero.getOHeatSinks() + " [" + (aero.getOHeatSinks() * 2) + "]" : aero.getOHeatSinks());
		setModelData("fuelPoints", aero.getFuel());
		setModelData("fuelMass", aero.getFuelTonnage());
		setModelData("hsMass", NumberFormat.getInstance().format(testAero.getWeightHeatSinks()));
		if (aero.getCockpitType() == Aero.COCKPIT_STANDARD) {
			setModelData("cockpitType", "Cockpit");
		} else {
			setModelData("cockpitType", Aero.getCockpitTypeString(aero.getCockpitType()));
		}
		setModelData("cockpitMass", NumberFormat.getInstance().format(testAero.getWeightControls()));
		String atName = formatArmorType(aero, true);
		if (atName.length() > 0) {
			setModelData("armorType", " (" + atName + ")");
		} else {
			setModelData("armorType", "");
		}
		setModelData("armorFactor", aero.getTotalOArmor());
		setModelData("armorMass", NumberFormat.getInstance().format(testAero.getWeightArmor()));
		if (aero.isOmni()) {
			addFixedOmni(aero);
		}
	}
	
	private void addFluff() {
		addMechVeeAeroFluff(aero);
		// Add fluff frame description
	}

	private static final int[][] AERO_ARMOR_LOCS = {
			{Aero.LOC_NOSE}, {Aero.LOC_RWING, Aero.LOC_LWING}, {Aero.LOC_AFT}
	};
	
	private void addArmorAndStructure() {
		setModelData("armorValues", addArmorStructureEntries(aero,
				(en, loc) -> en.getOArmor(loc),
				AERO_ARMOR_LOCS));
		if (aero.hasPatchworkArmor()) {
			setModelData("patchworkByLoc", addPatchworkATs(aero, AERO_ARMOR_LOCS));
		}
	}
	
}
