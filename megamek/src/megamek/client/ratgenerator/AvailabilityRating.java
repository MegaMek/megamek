/*
 * Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2016-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ratgenerator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

import megamek.codeUtilities.MathUtility;
import megamek.logging.MMLogger;

/**
 * Handles availability rating values and calculations for RAT generator. Availability is rated on a base-2 logarithmic
 * scale from 0 (non-existent) to 10 (ubiquitous), with 6 being a typical value when the source material does not give
 * an indication of frequency. The availability rating is actually twice the exponent, which allows more precision while
 * still storing values as integers (so it's really a base-(sqrt(2)) scale, but using 2 as the base should theoretically
 * be faster).
 * <br>
 * These values are stored separately for chassis and models; there is one value to indicate the likelihood that a
 * medium Mek is a Phoenix Hawk and another set of values to indicate the likelihood that a give Phoenix Hawk is a 1D or
 * 1K, etc.
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
    int ratingAdjustment;
    int era;
    int startYear;
    String unitName;

    // Rating values indexed by equipment level names
    LinkedHashMap<String, Integer> ratingByLevel;
    // Rating values indexed by equipment level values
    HashMap<Integer, Integer> ratingByNumericLevel;

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
     *             rating for the faction (usually A or Keshik) and decreases for each step the rating is reduced.
     *             <br>-: as +, but applies to the lowest equipment rating (F or PGC)
     *             and decreases as rating increases.
     *             <br>YEAR: when the unit becomes available to the faction, if later
     *             than the beginning of the era. Any year before this within the era will be treated as having no
     *             availability.
     */
    public AvailabilityRating(String unit, int era, String code) {
        unitName = unit;
        this.era = era;
        startYear = era;
        this.ratingAdjustment = 0;
        ratingByLevel = new LinkedHashMap<>();
        ratingByNumericLevel = new HashMap<>();

        String[] fields;
        String loggerData = unit + " in " + era + " era: " + code;

        // The '!' character is used to indicate discrete equipment level ratings
        if (!code.contains("!")) {

            fields = code.split(":");

            // Simple availability will have either one or two values
            if (fields.length < 2 || fields.length > 3) {
                logger.warn("Availability Rating - Incorrect availability formatting for {}", loggerData);
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

            availability = MathUtility.parseInt(fields[1], 0);

            // A third field will always be a year modifier
            if (fields.length > 2) {
                startYear = MathUtility.parseInt(fields[2], -1);
                if (startYear < 0) {
                    throw new NumberFormatException("Invalid year value.");
                }
            }

        } else {

            fields = code.split("!");
            faction = fields[0];
            String[] subfields;
            int avRating;
            for (int i = 1; i < fields.length; i++) {
                subfields = fields[i].split(":");

                if (subfields.length != 2) {
                    logger.warn("Subfields - Incorrect availability formatting for {}", loggerData);
                    return;
                }

                avRating = MathUtility.parseInt(subfields[1], 0);

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
     * Returns the availability value, using the provided equipment rating if multiple levels are present or the raw
     * value if not
     *
     * @param equipmentLevel name of equipment level, typically one of A/B/C/D/F
     *
     * @return availability value for the specified equipment rating string
     */
    public int getAvailability(String equipmentLevel) {
        if (!hasMultipleRatings()) {
            return getAvailability();
        } else {
            return ratingByLevel.getOrDefault(equipmentLevel, 0);
        }
    }

    /**
     * Returns the availability value, using the provided equipment level if multiple levels are present or the raw
     * value if not
     *
     * @param equipmentLevelIndex index number of equipment level, typically 0 (F), 1 (D), 2 (C), 3 (B), 4 (A)
     *
     * @return availability value for the specified equipment rating value
     */
    public int getAvailability(int equipmentLevelIndex) {
        if (!hasMultipleRatings() || equipmentLevelIndex < 0) {
            return getAvailability();
        } else {
            return ratingByNumericLevel.getOrDefault(equipmentLevelIndex, 0);
        }
    }

    /**
     * Adjust availability rating for the dynamic +/- value, which is based on equipment quality rating.  The (+) will
     * reduce availability for commands with a lower rating, while the (-) will reduce availability for commands with a
     * higher rating.
     *
     * @param equipRating zero-based index based on {@code numLevels} of rating to check
     * @param numLevels   number of equipment levels available, typically 5 (A/B/C/D/F)
     *
     * @return integer, may be negative
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
     * Converts letter-based equipment level to index-based for working with systems that use it
     *
     * @param fRec faction-specific record, for equipment levels (typically A/B/C/D/F)
     */
    public void setRatingByNumericLevel(FactionRecord fRec) {
        if (null != fRec) {
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
    }

    public int getRatingAdjustment() {
        return ratingAdjustment;
    }

    public void setRatingAdjustment(int ratingAdjustment) {
        this.ratingAdjustment = ratingAdjustment;
    }

    /**
     * Indicates this availability rating object has different ratings for multiple levels.
     *
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
     * Get the string equivalent of the ratings values. Multiple ratings requires compiling them back into a
     * !RATING:VALUE!RATING:VALUE format
     *
     * @return string with properly formatted availability values, without the leading faction code
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
            return equipRatings.stream()
                  .map(curLevel -> '!' + curLevel + ':' + ratingByLevel.get(curLevel))
                  .collect(Collectors.joining());
        }
    }

    private String getAvailabilityText(int availability) {
        return switch (availability) {
            case 0 -> "-";
            case 1, 2 -> "Very Rare";
            case 3, 4 -> "Rare";
            case 5, 6 -> "Uncommon";
            case 7, 8 -> "Common";
            case 9, 10 -> "Ubiquitous";
            default -> "Unknown" + " (" + availability + ")";
        };
    }

    private String explodeAvailabilityByRatings(FactionRecord faction, int availability, int ratingAdjustment) {
        if (ratingAdjustment == 0) {
            return getAvailabilityText(availability);
        }
        //If an availability code is marked with +, it's for top-tier equipment (rating A/Keshik) and drops by 1 for each lower rating.
        //– means the opposite: it's for the lowest quality, and the number goes down for better ratings.
        StringBuilder result = new StringBuilder();
        ArrayList<String> ratingLevels;
        if (faction == null) {
            logger.warn("FactionRecord is null, cannot explode availability by ratings for {}", unitName);
            ratingLevels = new ArrayList<>(List.of("A", "B", "C", "D", "F"));
        } else {
            ratingLevels = new ArrayList<>(faction.getRatingLevels());
            Collections.reverse(ratingLevels);
        }
        int currentAvailability = availability;
        if (ratingAdjustment < 0) {
            currentAvailability -= ratingLevels.size() - 1;
        }
        for (String curLevel : ratingLevels) {
            if (currentAvailability > 0) {
                result.append(getAvailabilityText(currentAvailability))
                      //                      .append(" [").append(currentAvailability).append("]")
                      .append(" (").append(curLevel).append(")\n");
            }
            if (ratingAdjustment > 0) {
                currentAvailability--;
            } else {
                currentAvailability++;
            }
        }

        // Remove trailing newline if present
        if (!result.isEmpty() && result.charAt(result.length() - 1) == '\n') {
            result.setLength(result.length() - 1);
        }
        return result.toString();
    }

    public String formatAvailability(FactionRecord factionRecord) {
        if (!hasMultipleRatings()) {
            if (ratingAdjustment == 0) {
                return getAvailabilityText(availability);
            } else {
                return explodeAvailabilityByRatings(factionRecord, availability, ratingAdjustment);
            }
        } else {
            Collection<String> equipRatings = ratingByLevel.keySet();
            return equipRatings.stream().map((curLevel) -> {
                Integer rating = ratingByLevel.get(curLevel);
                if (rating == null || rating <= 0) {
                    return "";
                }
                return getAvailabilityText(rating) + " (" + curLevel + ")";
            }).filter(curText -> !curText.isEmpty()
            ).collect(Collectors.joining("\n"));
        }
    }

    @Override
    public String toString() {
        if (era != startYear) {
            return getFactionCode() + ":" + getAvailabilityCode()
                  + ":" + startYear;
        }
        return getFactionCode() + (hasMultipleRatings() ? "" : ":") + getAvailabilityCode();
    }

    public AvailabilityRating makeCopy(String newFaction) {
        return new AvailabilityRating(unitName,
              era,
              newFaction + (hasMultipleRatings() ? "" : ":") + getAvailabilityCode());
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
