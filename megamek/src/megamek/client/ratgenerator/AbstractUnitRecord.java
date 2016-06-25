/*
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
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
package megamek.client.ratgenerator;

import java.util.HashSet;

/**
 * Base functionality for chassis and model records for RAT generator.
 *
 * @author Neoancient
 * 
 */
public class AbstractUnitRecord {
	public static final int MOVEMENT_LEG = 0;
	public static final int MOVEMENT_TRACKED = 1;
	public static final int MOVEMENT_WHEELED = 2;
	public static final int MOVEMENT_HOVER = 3;
	public static final int MOVEMENT_WIGE = 4;
	public static final int MOVEMENT_VTOL = 5;
	public static final int MOVEMENT_NAVAL = 6;
	public static final int MOVEMENT_UW = 7;
	public static final int MOVEMENT_JUMP = 8;
	public static final int MOVEMENT_MOTORIZED = 9;
	public static final int MOVEMENT_ATMOSPHERIC = 10;
	public static final int MOVEMENT_AEROSPACE = 11;
	public static final int MOVEMENT_SPACE = 12;
	public static final int MOVEMENT_NONE = 13;
	public static final String[] movementTypeNames = {
		"Leg", "Tracked", "Wheeled", "Hover", "WiGE", "VTOL",
		"Naval", "Underwater", "Jump", "Motorized", "Atmospheric",
		"Aerospace", "Space", "None"
	};
	
	public static final int NETWORK_NONE = 0;
	public static final int NETWORK_C3_SLAVE = 1;
	public static final int NETWORK_C3_MASTER = 2;
	public static final int NETWORK_C3I = 3;
	public static final int NETWORK_NAVAL_C3 = 4;
	public static final int NETWORK_BA_C3 = 5;
	public static final int NETWORK_BA_C3I = 6;
	public static final int NETWORK_NOVA = 7;

	protected String chassis = "";
	protected boolean omni;
	protected String unitType;
	protected int movementType;
	protected int introYear;
	protected HashSet<String> includedFactions;

	public AbstractUnitRecord(String chassis){
		this.chassis = chassis;
		unitType = "Mek";
		omni = false;
		movementType = MOVEMENT_LEG;
		includedFactions = new HashSet<String>();
	}
	
	/**
	 * Adjusts availability rating for the first couple years after introduction.
	 * 
	 * @param ar The AvailabilityRecord for the chassis or model.
	 * @param rating The force equipment rating.
	 * @param ratingLevels The number of equipment rating levels used by the faction.
	 * @param year The game year
	 * @return The adjusted availability rating.
	 */
	public int calcAvailability(AvailabilityRating ar,
			int rating, int ratingLevels, int year) {
		int retVal = ar.adjustForRating(rating, ratingLevels);
		
		if (introYear == year) {
			retVal -= 2;
		}
		if (introYear == year + 1) {
			retVal -= 1;
		}
		if (retVal < 0) {
			return 0;
		}
		return retVal;
	}

	public String getChassis() {
		return chassis;
	}
	public void setChassis(String chassis) {
		this.chassis = chassis;
	}
	public final String getChassisKey() {
		return chassis + "[" + unitType + (omni?"] Omni":"]");
	}
	public String getKey() {
		return getChassisKey();
	}
	public String getUnitType() {
		return unitType;
	}
	public void setUnitType(String type) {
		unitType = type;
	}
	public boolean isOmni() {
		return omni;
	}
	public void setOmni(boolean omni) {
		this.omni = omni;
	}
	public int getMovementType() {
		return movementType;
	}
	public void setMovementType(int movementType) {
		this.movementType = movementType;
	}
	public int getIntroYear() {
		return introYear;
	}
	public void setIntroYear(int year) {
		this.introYear = year;
	}
	
	public HashSet<String> getIncludedFactions() {
		return includedFactions;
	}

	public String toString() {
		return getKey();
	}
}

