/**
 * MegaMek - Copyright (C) 2004 Ben Mazur (bmazur@sev.org)
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
/*
 * Created on Jun 2, 2005
 *
 */
package megamek.common.loaders;

import megamek.common.Engine;
import megamek.common.Entity;
import megamek.common.Tank;
import megamek.common.TroopSpace;
import megamek.common.VTOL;
import megamek.common.util.BuildingBlock;

/**
 * @author Andrew Hunter
 */
public class BLKVTOLFile extends BLKFile implements IMechLoader {

	private static final String[] MOVES = { "", "", "", "Tracked", "Wheeled", "Hover", "VTOL" };

	public BLKVTOLFile(BuildingBlock bb) {
		dataFile = bb;
	}

	public Entity getEntity() throws EntityLoadingException {
		VTOL t = new VTOL();

		if (!dataFile.exists("Name")) {
			throw new EntityLoadingException("Could not find name block.");
		}
		t.setChassis(dataFile.getDataAsString("Name")[0]);
		if (dataFile.exists("Model") && dataFile.getDataAsString("Model")[0] != null) {
			t.setModel(dataFile.getDataAsString("Model")[0]);
		} else {
			t.setModel("");
		}

		setTechLevel(t);

		if (dataFile.exists("source")) {
			t.setSource(dataFile.getDataAsString("source")[0]);
		}

		if (!dataFile.exists("tonnage")) {
			throw new EntityLoadingException("Could not find weight block.");
		}
		t.setWeight(dataFile.getDataAsFloat("tonnage")[0]);

		if (!dataFile.exists("motion_type")) {
			throw new EntityLoadingException("Could not find movement block.");
		}
		String sMotion = dataFile.getDataAsString("motion_type")[0];
		int nMotion = -1;
		for (int x = 0; x < MOVES.length; x++) {
			if (sMotion.equals(MOVES[x])) {
				nMotion = x;
				break;
			}
		}
		if (nMotion == -1) {
			throw new EntityLoadingException("Invalid movment type: " + sMotion);
		}
		t.setMovementMode(nMotion);

		if (dataFile.exists("transporters")) {
			String[] transporters = dataFile.getDataAsString("transporters");
			// Walk the array of transporters.
			for (String transporter : transporters) {
				// TroopSpace:
				if (transporter.startsWith("TroopSpace:", 0)) {
					// Everything after the ':' should be the space's size.
					Double fsize = new Double(transporter.substring(11));
					t.addTransporter(new TroopSpace(fsize.doubleValue()));
				}

			} // Handle the next transportation component.

		} // End has-transporters

		int engineCode = BLKFile.FUSION;
		if (dataFile.exists("engine_type")) {
			engineCode = dataFile.getDataAsInt("engine_type")[0];
		}
		int engineFlags = Engine.TANK_ENGINE;
		if (t.isClan()) {
			engineFlags |= Engine.CLAN_ENGINE;
		}
		if (!dataFile.exists("cruiseMP")) {
			throw new EntityLoadingException("Could not find cruiseMP block.");
		}
		int engineRating = dataFile.getDataAsInt("cruiseMP")[0] * (int) t.getWeight() - t.getSuspensionFactor();
		t.setEngine(new Engine(engineRating, BLKFile.translateEngineCode(engineCode), engineFlags));

		if (dataFile.exists("armor_type")) {
			t.setArmorType(dataFile.getDataAsInt("armor_type")[0]);
		}
		if (dataFile.exists("armor_tech")) {
			t.setArmorTechLevel(dataFile.getDataAsInt("armor_tech")[0]);
		}
		if (dataFile.exists("internal_type")) {
			t.setStructureType(dataFile.getDataAsInt("internal_type")[0]);
		}

		if (!dataFile.exists("armor")) {
			throw new EntityLoadingException("Could not find armor block.");
		}

		int[] armor = dataFile.getDataAsInt("armor");

		if (armor.length != 5) {
			throw new EntityLoadingException("Incorrect armor array length");
		}
		// add the body to the armor array
		int[] fullArmor = new int[armor.length + 1];
		fullArmor[0] = 0;
		System.arraycopy(armor, 0, fullArmor, 1, armor.length);
		for (int x = 0; x < fullArmor.length; x++) {
			t.initializeArmor(fullArmor[x], x);
		}

		t.autoSetInternal();

		loadEquipment(t, "Front", Tank.LOC_FRONT);
		loadEquipment(t, "Right", Tank.LOC_RIGHT);
		loadEquipment(t, "Left", Tank.LOC_LEFT);
		loadEquipment(t, "Rear", Tank.LOC_REAR);
		loadEquipment(t, "Body", Tank.LOC_BODY);

		if (dataFile.exists("omni")) {
			t.setOmni(true);
		}

		return t;
	}

}
