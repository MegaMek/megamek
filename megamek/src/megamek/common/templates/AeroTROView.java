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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import megamek.common.Messages;
import megamek.common.Mounted;
import megamek.common.Aero;
import megamek.common.Bay;
import megamek.common.Entity;
import megamek.common.logging.DefaultMmLogger;
import megamek.common.verifier.BayData;
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
	
	protected void addBays() {
		List<Map<String, Object>> bays = new ArrayList<>();
		for (Bay bay : aero.getTransportBays()) {
			if (bay.isQuarters()) {
				continue;
			}
			BayData bayData = BayData.getBayType(bay);
			if (null != bayData) {
				Map<String, Object> bayRow = new HashMap<>();
				bayRow.put("name", bayData.getDisplayName());
				if (bayData.isCargoBay()) {
					bayRow.put("size", bay.getCapacity() + Messages.getString("TROView.tons"));
				} else {
					bayRow.put("size", (int) bay.getCapacity());
				}
				bayRow.put("doors", bay.getDoors());
				bays.add(bayRow);
			} else {
				DefaultMmLogger.getInstance().warning(getClass(), "addBays()",
						"Could not determine bay type for " + bay.toString());
			}
		}
		setModelData("bays", bays);
	}
	
	/**
	 * Adds ammo data used by large craft
	 */
	protected void addAmmo() {
		Map<String, List<Mounted>> ammoByType = aero.getAmmo().stream()
				.collect(Collectors.groupingBy(m -> m.getType().getName()));
		List<Map<String, Object>> ammo = new ArrayList<>();
		for (List<Mounted> aList : ammoByType.values()) {
			Map<String, Object> ammoEntry = new HashMap<>();
			ammoEntry.put("name", aList.get(0).getType().getName().replaceAll("\\s+Ammo", ""));
			ammoEntry.put("shots", aList.stream().mapToInt(Mounted::getUsableShotsLeft).sum());
			ammoEntry.put("tonnage", aList.stream().mapToDouble(m -> m.getType().getTonnage(aero)).sum());
			ammo.add(ammoEntry);
		}
		setModelData("ammo", ammo);
	}

	/**
	 * Convenience method to add the number of crew in a category to a list, and choose the singular or
	 * plural form. The localized string property should be provided for both singular and plural entries,
	 * even if they are the same (such as enlisted/non-rated and bay personnel in English).
	 * 
	 * The model needs to have a "crew" entry initialized to a {@code List<String>} before calling this
	 * method.
	 * 
	 * @param stringKey The key for the string property in the singular form. A "TROView." prefix will be added,
	 *                  and if the plural form is needed "s" will be appended.
	 * @param count     The number of crew in the category
	 * @throws NullPointerException If the "crew" property in the model has not been initialized
	 * @throws ClassCastException   If the crew property of the model is not a {@code List<String>}
	 */
	@SuppressWarnings("unchecked")
	protected void addCrewEntry(String stringKey, int count) {
		if (count > 1) {
			((List<String>) getModelData("crew"))
			.add(String.format(Messages.getString("TROView." + stringKey + "s"), count));
		} else if (count > 2) {
			((List<String>) getModelData("crew"))
			.add(String.format(Messages.getString("TROView." + stringKey), count));
		}
	}

}

