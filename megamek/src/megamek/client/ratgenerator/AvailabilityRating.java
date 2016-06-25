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

/**
 * Handles availability rating values and calculations for RAT generator.
 * Availability is rated on a logarithmic scale from 0 (non-existent) to 10 (ubiquitous),
 * with 6 being a typical value when the source material does not give an indication of frequency.
 * These values are stored separately for chassis and models; for example, there is
 * one value to indicate the likelihood that a medium Mek is a Phoenix Hawk and another
 * set of values to indicate the likelihood that a give Phoenix Hawk is a 1D or 1K, etc.
 *
 * @author Neoancient
 * 
 */

public class AvailabilityRating {
	public static final int MAX_AV_RATING = 12;
	//Base for calculating weights from availability ratings. Based on analysis of Xotl's tables.
	public static final double EXP_BASE = 1.48;
	public static final double LOG_BASE = Math.log(EXP_BASE);
	
	String faction = "General";
	int availability = 0;
	String ratings = null;
	int ratingAdjustment = 0;
	int year = 3025;
	String unitName = null;
	
	/**
	 * 
	 * @param unit The chassis or model key
	 * @param year The year that this availability code applies to.
	 * @param code A string with the format FKEY[!RATING]:AV[+/-]
	 * 				FKEY: the faction key
	 * 				RATING: if supplied, will limit this record to units with the indicated equipment rating
	 * 				AV: a value that indicates how common this unit is, from 0 (non-existent)
	 *                  to 10 (ubiquitous)
	 * 				+: the indicated av rating applies to the highest equipment rating for the faction
	 * 					(usually A or Keshik) and decreases for each step the rating is reduced.
	 * 				-: as +, but applies to the lowest equipment rating (F or PGC) and decreases
	 * 					as rating increases.
	 */
	public AvailabilityRating(String unit, int year, String code) {
		unitName = unit;
		this.year = year;
		this.ratingAdjustment = 0;
		String[] fields = code.split(":");
		if (fields[0].contains("!")) {
			String[] subfields = fields[0].split("!");
			ratings = subfields[1];
			fields[0] = subfields[0];
		}
		faction = fields[0];
		
		if (fields.length < 2) {
			System.err.println(unit + " (" + year + "): " + faction);
		}
		if (fields[1].endsWith("+")) {
			this.ratingAdjustment++;
			fields[1] = fields[1].replace("+", "");
		}
		if (fields[1].endsWith("-")) {
			this.ratingAdjustment--;
			fields[1] = fields[1].replace("-", "");
		}
		availability = Integer.parseInt(fields[1]);
	}

	public String getFaction() {
		return faction;
	}

	public void setFaction(String faction) {
		this.faction = faction;
	}

	public int getAvailability() {
		return availability;
	}
	
	public int adjustForRating(int rating, int numLevels) {
		if (rating < 0 || ratingAdjustment == 0) {
			return availability;
		} else if (ratingAdjustment < 0) {
			return Math.max(availability - rating, 0);
		} else {
			return Math.min(Math.max(0, availability - (numLevels - rating)), MAX_AV_RATING);
		}
	}

	public void setAvailability(int availability) {
		this.availability = availability;
	}

	public String getRatings() {
		return ratings;
	}

	public void setRatings(String ratings) {
		this.ratings = ratings;
	}

	public int getRatingAdjustment() {
		return ratingAdjustment;
	}

	public void setRatingAdjustment(int ratingAdjustment) {
		this.ratingAdjustment = ratingAdjustment;
	}

	public int getYear() {
		return year;
	}

	public void setYear(int year) {
		this.year = year;
	}

	public String getUnitName() {
		return unitName;
	}

	public void setUnitName(String unitName) {
		this.unitName = unitName;
	}
	
	public String getFactionCode() {
		String retVal = faction;
		if (ratings != null && ratings.length() > 0) {
			retVal += "!" + ratings;
		}
		return retVal;
	}
	
	public String getAvailabilityCode() {
		if (ratingAdjustment == 0) {
			return Integer.toString(availability);
		} else if (ratingAdjustment < 0) {
			return availability + "-";
		} else {
			return availability + "+";
		}
	}
	
	@Override
	public String toString() {
		return getFactionCode() + ":" + getAvailabilityCode();
	}
	
	public AvailabilityRating makeCopy(String newFaction) {
		return new AvailabilityRating(unitName, year, newFaction + ":" + getAvailabilityCode());
	}

	public double getWeight() {
		return calcWeight(availability);
	}
	
	static double calcWeight(double avRating) {
		return Math.pow(EXP_BASE, avRating);
	}
	
	static double calcAvRating(double weight) {
		return Math.log(weight) / LOG_BASE;
	}
}
