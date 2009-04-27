/*
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
 * BLkFile.java
 *
 * Created on April 6, 2002, 2:06 AM
 */

/**
 * This class loads 'Proto BLK files.
 *
 * @author  Suvarov454@sourceforge.net (James A. Damour)
 * @version $revision:$
 */
package megamek.common.loaders;

import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.IEntityMovementMode;
import megamek.common.LocationFullException;
import megamek.common.Protomech;
import megamek.common.TechConstants;
import megamek.common.util.BuildingBlock;

public class BLKProtoFile extends BLKFile implements IMechLoader {

	public BLKProtoFile(BuildingBlock bb) {
		dataFile = bb;
	}

	public Entity getEntity() throws EntityLoadingException {

		Protomech t = new Protomech();

		if (!dataFile.exists("name")) {
			throw new EntityLoadingException("Could not find name block.");
		}
		t.setChassis(dataFile.getDataAsString("Name")[0]);

		// Model is not strictly necessary.
		if (dataFile.exists("Model") && dataFile.getDataAsString("Model")[0] != null) {
			t.setModel(dataFile.getDataAsString("Model")[0]);
		} else {
			t.setModel("");
		}

		if (dataFile.exists("source")) {
			t.setSource(dataFile.getDataAsString("source")[0]);
		}

		if (!dataFile.exists("year")) {
			throw new EntityLoadingException("Could not find year block.");
		}
		t.setYear(dataFile.getDataAsInt("year")[0]);

		setTechLevel(t);

		if (!dataFile.exists("tonnage")) {
			throw new EntityLoadingException("Could not find weight block.");
		}
		t.setWeight(dataFile.getDataAsFloat("tonnage")[0]);

		/***********************************************************************
		 * 'Protos have only one motion type. if
		 * (!dataFile.exists("motion_type")) throw new
		 * EntityLoadingException("Could not find movement block."); String
		 * sMotion = dataFile.getDataAsString("motion_type")[0]; int nMotion =
		 * -1; for (int x = 0; x < MOVES.length; x++) { if
		 * (sMotion.equals(MOVES[x])) { nMotion = x; break; } } if (nMotion ==
		 * -1) throw new EntityLoadingException("Invalid movment type: " +
		 * sMotion); t.setMovementType(nMotion); 'Protos have only one motion
		 * type. *
		 **********************************************************************/
		t.setMovementMode(IEntityMovementMode.INF_JUMP);

		if (!dataFile.exists("cruiseMP")) {
			throw new EntityLoadingException("Could not find cruiseMP block.");
		}
		t.setOriginalWalkMP(dataFile.getDataAsInt("cruiseMP")[0]);

		if (dataFile.exists("jumpingMP")) {
			t.setOriginalJumpMP(dataFile.getDataAsInt("jumpingMP")[0]);
		}

		if (!dataFile.exists("armor")) {
			throw new EntityLoadingException("Could not find armor block.");
		}

		int[] armor = dataFile.getDataAsInt("armor");

		boolean hasMainGun = false;
		if (Protomech.NUM_PMECH_LOCATIONS == armor.length) {
			hasMainGun = true;
		} else if (Protomech.NUM_PMECH_LOCATIONS - 1 == armor.length) {
			hasMainGun = false;
		} else {
			throw new EntityLoadingException("Incorrect armor array length");
		}

		t.setHasMainGun(hasMainGun);

		// add the body to the armor array
		for (int x = 0; x < armor.length; x++) {
			t.initializeArmor(armor[x], x);
		}

		t.autoSetInternal();

		String[] abbrs = t.getLocationNames();
		for (int loop = 0; loop < t.locations(); loop++) {
			loadEquipment(t, abbrs[loop], loop);
		}
		return t;
	}

	private void loadEquipment(Protomech t, String sName, int nLoc) throws EntityLoadingException {
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

		for (String element : saEquip) {
			String equipName = element.trim();

			// ProtoMech Ammo comes in non-standard amounts.
			int ammoIndex = equipName.indexOf("Ammo (");
			int shotsCount = 0;
			if (ammoIndex > 0) {
				// Try to get the number of shots.
				try {
					String shots = equipName.substring(ammoIndex + 6, equipName.length() - 1);
					shotsCount = Integer.parseInt(shots);
					if (shotsCount < 0) {
						throw new EntityLoadingException("Invalid number of shots in: " + equipName + ".");
					}
				} catch (NumberFormatException badShots) {
					throw new EntityLoadingException("Could not determine the number of shots in: " + equipName + ".");
				}

				// Strip the shots out of the ammo name.
				equipName = equipName.substring(0, ammoIndex + 4);
			}
			EquipmentType etype = EquipmentType.get(equipName);

			if (etype == null) {
				// try w/ prefix
				etype = EquipmentType.get(prefix + equipName);
			}

			if (etype != null) {
				try {
					// If this is an Ammo slot, only add
					// the indicated number of shots.
					if (ammoIndex > 0) {
						t.addEquipment(etype, nLoc, false, shotsCount);
					} else {
						t.addEquipment(etype, nLoc);
					}
				} catch (LocationFullException ex) {
					throw new EntityLoadingException(ex.getMessage());
				}
			}
			/*******************************************************************
			 * remove this block after TAG and Active Probes are implemented
			 * else if ( !equipName.equals("0") ) { System.err.println("Could
			 * not find " + equipName + " for " + t.getShortName() ); } //killme
			 * remove this block after TAG and Active Probes are implemented *
			 ******************************************************************/
		}
	}
}
