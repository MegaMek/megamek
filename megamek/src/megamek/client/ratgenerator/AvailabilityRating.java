/*
* MegaMek -
* Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
* Copyright (C) 2025 The MegaMek Team
*
* This program is free software; you can redistribute it and/or modify it under
* the terms of the GNU General Public License as published by the Free Software
* Foundation; either version 2 of the License, or (at your option) any later
* version.
*
* This program is distributed in the hope that it will be useful, but WITHOUT
* ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
* FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
* details.
*/
package megamek.client.ratgenerator;

import megamek.logging.MMLogger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.stream.Collectors;

/**
 * Handles availability rating values and calculations for RAT generator.
 * Availability is rated on a base-2 logarithmic scale from 0 (non-existent) to
 * 10 (ubiquitous), with 6 being a typical value when the source material does
 * not give an indication of frequency.
 * The availability rating is actually twice the exponent, which allows more
 * precision while still storing values as integers (so it's really a
 * base-(sqrt(2)) scale, but using 2 as the base should theoretically be faster).
 * <br>
 * These values are stored separately for chassis and models; there is one
 * value to indicate the likelihood that a medium Mek is a Phoenix Hawk and
 * another set of values to indicate the likelihood that a give Phoenix Hawk
 * is a 1D or 1K, etc.
 *
 * @author Neoancient
 */
public class AvailabilityRating {
    private final static MMLogger logger = MMLogger.create(AvailabilityRating.class);

    // Used to calculate av rating from weight.
    public static final double LOG_BASE = Math.log(2);

    String faction = "General";
    int availability = 0;
    String ratings = null;
    int ratingAdjustment = 0;
    int era;
    int startYear;
    String unitName = null;

    // Rating values indexed by equipment level names
    HashMap<String,Integer> ratingByLevel;
    // Rating values indexed by equipment level values
    HashMap<Integer,Integer> ratingByNumericLevel;

    /**
     * @param unit The chassis or model key
     * @param era  The era that this availability code applies to.
     * @param code A string with the format FKEY[!RATING]:AV[+/-][:YEAR]
     *             <br>examples: LA:7, FS:3+:3024, DC!A:8!B:7
     *             <br>FKEY: the faction key
     *             <br>!RATING: provides direct control over each equipment
     *             level. Not compatible with +/- or year values.
     *             <br>AV: an integer that indicates how common this unit is
     *             relative to others (chassis or model of same chassis)
     *             <br>+: the indicated av rating applies to the highest equipment
     *             rating for the faction (usually A or Keshik) and decreases
     *             for each step the rating is reduced.
     *             <br>-: as +, but applies to the lowest equipment rating (F or PGC)
     *             and decreases as rating increases.
     *             <br>YEAR: when the unit becomes available to the faction, if later
     *             than the beginning of the era. Any year before this within the era
     *             will be treated as having no availability.
     */
    public AvailabilityRating(String unit, int era, String code) {
        unitName = unit;
        this.era = era;
        startYear = era;
        this.ratingAdjustment = 0;
        ratingByLevel = new HashMap<>();
        ratingByNumericLevel = new HashMap<>();

        String[] fields;
        String loggerData = unit + " in " + era + " era: " + code;

        // The '!' character is used to indicate discrete equipment level ratings
        if (!code.contains("!")) {

            fields = code.split(":");

            // Simple availability will have either one or two values
            if (fields.length < 2 || fields.length > 3) {
                logger.warn("Incorrect availability formatting for " + loggerData);
                return;
            }

            faction = fields[0];

            // The '+' character indicates decreasing values for decreasing
            // equipment levels
            if (fields[1].endsWith("+")) {
                this.ratingAdjustment++;
                fields[1] = fields[1].replace("+", "");
            }

            // The '-' character indicates decreasing values for increasing
            // equipment levels
            if (fields[1].endsWith("-")) {
                this.ratingAdjustment--;
                fields[1] = fields[1].replace("-", "");
            }

            try {
                availability = Integer.parseInt(fields[1]);
            } catch (NumberFormatException ex) {
                availability = 0;
                logger.warn(ex, "Incorrect availability formatting for " + loggerData);
            }

            // A third field will always be a year modifier
            if (fields.length > 2) {
                try {
                    startYear = Integer.parseInt(fields[2]);
                    if (startYear < 0) {
                        throw new NumberFormatException("Invalid year value.");
                    }
                } catch (NumberFormatException ex) {
                    startYear = era;
                    logger.warn(ex, "Could not parse start year for " + loggerData);
                }
            }

        } else {

            fields = code.split("!");
            // FIXME: See if this property can be removed entirely
//            String[] subfields = fields[0].split("!");
//            ratings = subfields[1];
            faction = fields[0];
            String[] subfields;
            int avRating;
            for (int i = 1; i < fields.length; i++) {
                subfields = fields[i].split(":");

                if (subfields.length != 2) {
                    logger.warn("Incorrect availability formatting for " + loggerData);
                    return;
                }

                try {
                    avRating = Integer.parseInt(subfields[1]);
                } catch (NumberFormatException ex) {
                    avRating = 0;
                    logger.warn(ex, "Incorrect availability formatting for " + loggerData);
                }

                ratingByLevel.put(subfields[0], avRating);

            }

            // Use the highest availability as a backup value
            availability = Collections.max(ratingByLevel.values());

        }

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

    /**
     * Returns the availability value, using the provided equipment value if
     * multiple levels are present or the raw value if not
     * @param equipmentLevel  name of equipment level, typically one of A/B/C/D/F
     * @return
     */
    public int getAvailability(String equipmentLevel) {
        if (!hasMultipleRatings()) {
            return getAvailability();
        } else {
            return ratingByLevel.getOrDefault(equipmentLevel, 0);
        }
    }

    /**
     * Returns the availability value, using the provided equipment level if
     * multiple levels are present or the raw value if not
     * @param equipmentLevelIndex index number of equipment level, typically
     *                            0 (F), 1 (D), 2 (C), 3 (B), 4 (A)
     * @return
     */
    public int getAvailability(int equipmentLevelIndex) {
        if (!hasMultipleRatings() || equipmentLevelIndex < 0) {
            return getAvailability();
        } else {
            return ratingByNumericLevel.getOrDefault(equipmentLevelIndex, 0);
        }
    }

    /**
     * Adjust availability rating for the dynamic +/- value, which is based on
     * equipment quality rating.  The (+) will reduce availability for commands
     * with a lower rating, while the (-) will reduce availability for commands
     * with a higher rating.
     * @param equipRating zero-based index based on {@code numLevels} of rating to check
     * @param numLevels   number of equipment levels available, typically 5 (A/B/C/D/F)
     * @return  integer, may be negative
     */
    public int adjustForRating(int equipRating, int numLevels) {
        if (ratingAdjustment == 0 || equipRating < 0) {
            return availability;
        }

        if (ratingAdjustment > 0) {
            // (+) adjustment, reduce availability as equipment rating decreases
            return availability - (numLevels - 1 - equipRating);
        } else {
            // (-) adjustment, reduce availability as equipment rating increases
            return availability - equipRating;
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

    /**
     * Converts letter-based equipment level to index-based for working with
     * systems that use it
     * @param fRec faction-specific record, for equipment levels (typically
     *             A/B/C/D/F)
     */
    public void setRatingByNumericLevel(FactionRecord fRec) {
        if (hasMultipleRatings()) {
            Collection<String> levelNames = ratingByLevel.keySet();

            int ratingLevel = -1;
            ArrayList<String> factionRatings = fRec.getRatingLevelSystem();
            int numRatingLevels = factionRatings.size();

            for (String curLevel : levelNames) {

                if (curLevel == null && fRec.getRatingLevels().size() == 1) {
                    ratingLevel = factionRatings.indexOf(fRec.getRatingLevels().get(0));
                }

                if (curLevel != null && numRatingLevels > 1) {
                    ratingLevel = factionRatings.indexOf(curLevel);
                }

                ratingByNumericLevel.put(ratingLevel, ratingByLevel.get(curLevel));

            }
        }
    }

    public int getRatingAdjustment() {
        return ratingAdjustment;
    }

    public void setRatingAdjustment(int ratingAdjustment) {
        this.ratingAdjustment = ratingAdjustment;
    }

    /**
     * Indicates this availability rating object has different ratings for
     * multiple levels.
     * @return true if ratings for multiple levels are set
     */
    public boolean hasMultipleRatings() {
        return !ratingByLevel.isEmpty() && ratingByLevel.values().stream().anyMatch(curRating -> curRating > 0);
    }

    public int getEra() {
        return era;
    }

    public void setEra(int era) {
        this.era = era;
    }

    public int getStartYear() {
        return startYear;
    }

    public void setStartYear(int year) {
        startYear = year;
    }

    public String getUnitName() {
        return unitName;
    }

    public void setUnitName(String unitName) {
        this.unitName = unitName;
    }

    public String getFactionCode() {
        return faction;
    }

    /**
     * Get the string equivalent of the ratings values. Multiple ratings
     * requires compiling them back into a !RATING:VALUE!RATING:VALUE format
     * @return string with properly formatted availability values, without
     * the leading faction code
     */
    public String getAvailabilityCode() {
        if (!hasMultipleRatings()) {
            if (ratingAdjustment == 0) {
                return Integer.toString(availability);
            } else if (ratingAdjustment < 0) {
                return availability + "-";
            } else {
                return availability + "+";
            }
        } else {
            Collection<String> equipRatings = ratingByLevel.keySet();
            return equipRatings.stream().map(curLevel -> '!' + curLevel + ':' + ratingByLevel.get(curLevel)).collect(Collectors.joining());
        }
    }

    @Override
    public String toString() {
        if (era != startYear) {
            return getFactionCode() + ":" + getAvailabilityCode()
                    + ":" + startYear;
        }
        return getFactionCode() + ":" + getAvailabilityCode();
    }

    public AvailabilityRating makeCopy(String newFaction) {
        return new AvailabilityRating(unitName, era, newFaction + ":" + getAvailabilityCode());
    }

    public double getWeight() {
        return calcWeight(availability);
    }

    static double calcWeight(double avRating) {
        return Math.pow(2, avRating / 2.0);
    }

    static double calcAvRating(double weight) {
        return 2.0 * Math.log(weight) / LOG_BASE;
    }
}
