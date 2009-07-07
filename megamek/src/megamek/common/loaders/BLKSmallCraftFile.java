/*
 * MegaMek - Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
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
 * BLkFile.java
 *
 * Created on April 6, 2002, 2:06 AM
 */

/**
 *
 * @author  taharqa
 * @version 
 */
package megamek.common.loaders;

import megamek.common.Aero;
import megamek.common.BattleArmorBay;
import megamek.common.CargoBay;
import megamek.common.Engine;
import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.InfantryBay;
import megamek.common.LocationFullException;
import megamek.common.SmallCraft;
import megamek.common.TechConstants;
import megamek.common.util.BuildingBlock;

public class BLKSmallCraftFile extends BLKFile implements IMechLoader {

	// armor locatioms
	public static final int NOSE = 0;
	public static final int RW = 1;
	public static final int LW = 2;
	public static final int AFT = 3;

	public BLKSmallCraftFile(BuildingBlock bb) {
		dataFile = bb;
	}

	public Entity getEntity() throws EntityLoadingException {

		SmallCraft a = new SmallCraft();

		if (!dataFile.exists("Name")) {
			throw new EntityLoadingException("Could not find name block.");
		}
		a.setChassis(dataFile.getDataAsString("Name")[0]);
		if (dataFile.exists("Model") && dataFile.getDataAsString("Model")[0] != null) {
			a.setModel(dataFile.getDataAsString("Model")[0]);
		} else {
			a.setModel("");
		}

		if (dataFile.exists("source")) {
			a.setSource(dataFile.getDataAsString("source")[0]);
		}

		setTechLevel(a);

		if (!dataFile.exists("tonnage")) {
			throw new EntityLoadingException("Could not find weight block.");
		}
		a.setWeight(dataFile.getDataAsFloat("tonnage")[0]);

		if (!dataFile.exists("crew")) {
			throw new EntityLoadingException("Could not find crew block.");
		}
		a.setNCrew(dataFile.getDataAsInt("crew")[0]);

		if (!dataFile.exists("passengers")) {
			throw new EntityLoadingException("Could not find passenger block.");
		}
		a.setNPassenger(dataFile.getDataAsInt("passengers")[0]);

		if (!dataFile.exists("motion_type")) {
			throw new EntityLoadingException("Could not find movement block.");
		}
		String sMotion = dataFile.getDataAsString("motion_type")[0];
		int nMotion = 16;
		if (sMotion.equals("spheroid")) {
			nMotion = 17;
			a.setSpheroid(true);
		}
		a.setMovementMode(nMotion);

		// figure out structural integrity
		if (!dataFile.exists("structural_integrity")) {
			throw new EntityLoadingException("Could not find structual integrity block.");
		}
		a.set0SI(dataFile.getDataAsInt("structural_integrity")[0]);

		// figure out heat
		if (!dataFile.exists("heatsinks")) {
			throw new EntityLoadingException("Could not find weight block.");
		}
		a.setHeatSinks(dataFile.getDataAsInt("heatsinks")[0]);
		if (!dataFile.exists("sink_type")) {
			throw new EntityLoadingException("Could not find weight block.");
		}
		a.setHeatType(dataFile.getDataAsInt("sink_type")[0]);

		// figure out fuel
		if (!dataFile.exists("fuel")) {
			throw new EntityLoadingException("Could not find fuel block.");
		}
		a.setFuel(dataFile.getDataAsInt("fuel")[0]);

		// figure out engine stuff
		// not done for small craft and up
		if (!dataFile.exists("SafeThrust")) {
			throw new EntityLoadingException("Could not find Safe Thrust block.");
		}
		a.setOriginalWalkMP(dataFile.getDataAsInt("SafeThrust")[0]);

		a.setEngine(new Engine(400, 0, 0));

		if (dataFile.exists("armor_type")) {
			a.setArmorType(dataFile.getDataAsInt("armor_type")[0]);
		}
		if (dataFile.exists("armor_tech")) {
			a.setArmorTechLevel(dataFile.getDataAsInt("armor_tech")[0]);
		}
		if (dataFile.exists("internal_type")) {
			a.setStructureType(dataFile.getDataAsInt("internal_type")[0]);
		}

		if (!dataFile.exists("armor")) {
			throw new EntityLoadingException("Could not find armor block.");
		}

		int[] armor = dataFile.getDataAsInt("armor");

		if (armor.length != 4) {
			throw new EntityLoadingException("Incorrect armor array length");
		}

		a.initializeArmor(armor[BLKAeroFile.NOSE], Aero.LOC_NOSE);
		a.initializeArmor(armor[BLKAeroFile.RW], Aero.LOC_RWING);
		a.initializeArmor(armor[BLKAeroFile.LW], Aero.LOC_LWING);
		a.initializeArmor(armor[BLKAeroFile.AFT], Aero.LOC_AFT);

		a.autoSetInternal();
		// This is not working right for arrays for some reason
		a.autoSetThresh();

		loadEquipment(a, "Nose", Aero.LOC_NOSE);
		loadEquipment(a, "Right Side", Aero.LOC_RWING);
		loadEquipment(a, "Left Side", Aero.LOC_LWING);
		loadEquipment(a, "Aft", Aero.LOC_AFT);

		// get the bays on this dropship
		// should be of format name:units:doors
		if (dataFile.exists("transporters")) {
			String[] transporters = dataFile.getDataAsString("transporters");
			// Walk the array of transporters.
			for (String transporter : transporters) {
				if (transporter.startsWith("InfantryBay:", 0)) {
					String numbers = transporter.substring(12);
					String temp[] = numbers.split(":");
					int size = Integer.parseInt(temp[0]);
					int doors = Integer.parseInt(temp[1]);
					a.addTransporter(new InfantryBay(size, doors));
				} else if (transporter.startsWith("BattleArmorBay:", 0)) {
					String numbers = transporter.substring(15);
					String temp[] = numbers.split(":");
					int size = Integer.parseInt(temp[0]);
					int doors = Integer.parseInt(temp[1]);
					a.addTransporter(new BattleArmorBay(size, doors));
				} else if (transporter.startsWith("CargoBay:", 0)) {
					String numbers = transporter.substring(9);
					String temp[] = numbers.split(":");
					int size = Integer.parseInt(temp[0]);
					int doors = Integer.parseInt(temp[1]);
					a.addTransporter(new CargoBay(size, doors));
				}
			}
		}

		return a;
	}

	@Override
	protected void loadEquipment(Entity t, String sName, int nLoc) throws EntityLoadingException {
		String[] saEquip = dataFile.getDataAsString(sName + " Equipment");
		if (saEquip == null) {
			return;
		}

		// prefix is "Clan " or "IS "
		String prefix;
		if (t.getTechLevel() == TechConstants.T_CLAN_TW) {
			prefix = "Clan ";
		} else {
			prefix = "IS ";
		}

		boolean rearMount = false;

		if (saEquip[0] != null) {
			for (String element : saEquip) {
				rearMount = false;
				String equipName = element.trim();

				if (equipName.startsWith("(R) ")) {
					rearMount = true;
					equipName = equipName.substring(4);
				}

				EquipmentType etype = EquipmentType.get(equipName);

				if (etype == null) {
					// try w/ prefix
					etype = EquipmentType.get(prefix + equipName);
				}

				if (etype != null) {
					try {
						t.addEquipment(etype, nLoc, rearMount);
					} catch (LocationFullException ex) {
						throw new EntityLoadingException(ex.getMessage());
					}
				} else if (equipName != "0") {
					t.addFailedEquipment(equipName);
				}
			}
		}
	}

}
