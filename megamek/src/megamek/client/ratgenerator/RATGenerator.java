/*
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
 */

package megamek.client.ratgenerator;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.xml.parsers.DocumentBuilder;

import megamek.client.ratgenerator.FactionRecord.TechCategory;
import megamek.client.ratgenerator.UnitTable.TableEntry;
import megamek.common.Configuration;
import megamek.common.EntityMovementMode;
import megamek.common.MekSummary;
import megamek.common.MekSummaryCache;
import megamek.common.UnitType;
import megamek.common.annotations.Nullable;
import megamek.common.util.fileUtils.MegaMekFile;
import megamek.logging.MMLogger;
import megamek.utilities.xml.MMXMLUtility;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Generates a random assignment table (RAT) dynamically based on a variety of criteria, including faction, era, unit
 * type, weight class, equipment rating, faction subcommand, vehicle movement mode, and mission role.
 *
 * @author Neoancient
 */
public class RATGenerator {
    private final static MMLogger logger = MMLogger.create(RATGenerator.class);

    private final HashMap<String, ModelRecord> models;
    private final HashMap<String, ChassisRecord> chassis;
    private final HashMap<String, FactionRecord> factions;
    private final HashMap<Integer, HashMap<String, HashMap<String, AvailabilityRating>>> modelIndex;
    private final HashMap<Integer, HashMap<String, HashMap<String, AvailabilityRating>>> chassisIndex;

    private final TreeSet<Integer> eraSet;

    private static RATGenerator ratGenerator = null;
    private static boolean interrupted = false;
    private static boolean dispose = false;
    private Thread loader;
    private boolean initialized;
    private boolean initializing;

    private final ArrayList<ActionListener> listeners;

    /**
     * Minimum difference between actual percentage and desired percentage of Omni-units that will trigger re-balancing
     */
    private static final double MIN_OMNI_DIFFERENCE = 2.5;

    /**
     * Minimum difference between actual percentage and desired percentage of base-Clan units that will trigger
     * re-balancing
     */
    private static final double MIN_CLAN_DIFFERENCE = 2.5;
    /**
     * Minimum difference between actual percentage and desired percentage of Star League and advanced IS tech units
     * that will trigger re-balancing
     */
    private static final double MIN_SL_DIFFERENCE = 2.5;

    protected RATGenerator() {
        models = new HashMap<>();
        chassis = new HashMap<>();
        factions = new HashMap<>();
        modelIndex = new HashMap<>();
        chassisIndex = new HashMap<>();
        eraSet = new TreeSet<>();
        listeners = new ArrayList<>();
    }

    public static RATGenerator getInstance() {
        if (ratGenerator == null) {
            ratGenerator = new RATGenerator();
        }

        if (!ratGenerator.initialized && !ratGenerator.initializing) {
            ratGenerator.initializing = true;
            interrupted = false;
            dispose = false;
            ratGenerator.loader = new Thread(() -> ratGenerator.initialize(Configuration.forceGeneratorDir()),
                  "RAT Generator unit populator");
            ratGenerator.loader.setPriority(Thread.NORM_PRIORITY - 1);
            ratGenerator.loader.start();
        }

        return ratGenerator;
    }

    public boolean isInitialized() {
        return initialized;
    }

    /**
     * Clears all data and loads from the given directory
     *
     * @param dir The directory to load from
     */
    public void reloadFromDir(File dir) {
        models.clear();
        chassis.clear();
        factions.clear();
        chassisIndex.clear();
        modelIndex.clear();
        eraSet.clear();
        initialized = false;
        initializing = false;
        initialize(dir);
        ratGenerator.getEraSet().forEach(e -> ratGenerator.loadEra(e, dir));
    }

    public AvailabilityRating findChassisAvailabilityRecord(int era, String unit, String faction, int year) {
        if (factions.containsKey(faction)) {
            return findChassisAvailabilityRecord(era, unit, factions.get(faction), year);
        }

        if (chassisIndex.containsKey(era) && chassisIndex.get(era).containsKey(unit)) {
            AvailabilityRating availabilityRating = chassisIndex.get(era).get(unit).get("General");
            if (availabilityRating != null && year >= availabilityRating.getStartYear()) {
                return availabilityRating;
            }
        }

        return null;
    }

    /**
     * Return the availability rating for a given chassis. If the specific faction is not directly listed, the parents
     * of the provided faction are used as a lookup. If multiple parent factions are provided, all of them are
     * calculated and then averaged.
     *
     * @param era           year for era
     * @param unit          string with chassis name
     * @param factionRecord faction data
     * @param year          year to test
     *
     * @return chassis availability rating, relative to other chassis in a collection
     */
    public @Nullable AvailabilityRating findChassisAvailabilityRecord(int era, String unit, FactionRecord factionRecord,
          int year) {

        if (factionRecord == null) {
            return null;
        }

        AvailabilityRating retVal = null;
        if (chassisIndex.containsKey(era) && chassisIndex.get(era).containsKey(unit)) {

            if (chassisIndex.get(era).get(unit).containsKey(factionRecord.getKey())) {
                retVal = chassisIndex.get(era).get(unit).get(factionRecord.getKey());
            } else if (factionRecord.getParentFactions().size() == 1) {
                retVal = findChassisAvailabilityRecord(era, unit, factionRecord.getParentFactions().get(0), year);
            } else if (!factionRecord.getParentFactions().isEmpty()) {

                ArrayList<AvailabilityRating> list = new ArrayList<>();
                for (String alt : factionRecord.getParentFactions()) {
                    AvailabilityRating availabilityRecord = findChassisAvailabilityRecord(era, unit, alt, year);
                    if (availabilityRecord != null) {
                        list.add(availabilityRecord);
                    }
                }
                retVal = mergeFactionAvailability(factionRecord.getKey(), list);

            } else {
                retVal = chassisIndex.get(era).get(unit).get("General");
            }
        }

        if (retVal != null && year >= retVal.getStartYear()) {
            return retVal;
        }

        return null;
    }

    public @Nullable AvailabilityRating findModelAvailabilityRecord(int era, String unit, String faction) {
        if (factions.containsKey(faction)) {
            return findModelAvailabilityRecord(era, unit, factions.get(faction));
        } else if (modelIndex.containsKey(era) && modelIndex.get(era).containsKey(unit)) {
            return modelIndex.get(era).get(unit).get("General");
        } else {
            return null;
        }
    }

    /**
     * Generate the availability rating for a specific model
     *
     * @param era           era designation
     * @param unit          string full chassis-model name
     * @param factionRecord faction data
     *
     * @return the availability value relative to other models of the same chassis
     */
    public @Nullable AvailabilityRating findModelAvailabilityRecord(int era, String unit,
          @Nullable FactionRecord factionRecord) {

        if (null == models.get(unit)) {
            logger.error("Trying to find record for unknown model {}", unit);
            return null;
        } else if ((factionRecord == null) || models.get(unit).factionIsExcluded(factionRecord)) {
            return null;
        }

        if (modelIndex.containsKey(era) && modelIndex.get(era).containsKey(unit)) {

            // If the provided faction is directly specified, return its availability
            if (modelIndex.get(era).get(unit).containsKey(factionRecord.getKey())) {
                return modelIndex.get(era).get(unit).get(factionRecord.getKey());
            }

            // If the provided faction has a single parent, return its availability
            if (factionRecord.getParentFactions().size() == 1) {
                return findModelAvailabilityRecord(era, unit, factionRecord.getParentFactions().get(0));
            } else if (!factionRecord.getParentFactions().isEmpty()) {

                // If neither the faction nor a direct parent is directly specified and multiple parent factions are
                // available, calculate an average between them
                ArrayList<AvailabilityRating> availabilityRatings = new ArrayList<>();
                for (String curParent : factionRecord.getParentFactions()) {
                    AvailabilityRating ar = findModelAvailabilityRecord(era, unit, curParent);
                    if (ar != null) {
                        availabilityRatings.add(ar);
                    }
                }
                return mergeFactionAvailability(factionRecord.getKey(), availabilityRatings);

            }

            // As a fallback, check for General availability
            return modelIndex.get(era).get(unit).get("General");
        }

        return null;
    }

    /**
     * Provides a list of availability ratings for a unit in a given era. Used in editing and reporting.
     *
     * @param era  The year of the record. This must be one of the years in the
     *             <code>eraSet</code>.
     * @param unit The lookup name of the unit to find records for.
     *
     * @return A <code>Collection</code> of all the availability ratings for the unit in the era, or null if there are
     *       no records for that era.
     */
    public @Nullable Collection<AvailabilityRating> getModelFactionRatings(int era, String unit) {
        if (modelIndex.containsKey(era) && modelIndex.get(era).containsKey(unit)) {
            return modelIndex.get(era).get(unit).values();
        }

        return null;
    }

    /**
     * Adds or changes an availability rating entry for a model.
     *
     * @param era                The year of the record to change
     * @param unitKey            The model key for the unit which is having its model record updated
     * @param availabilityRating The new <code>AvailabilityRating</code> for the unit in the era. This provides the
     *                           faction.
     */
    public void setModelFactionRating(int era, String unitKey, AvailabilityRating availabilityRating) {
        modelIndex.get(era).computeIfAbsent(unitKey, k -> new HashMap<>());
        modelIndex.get(era).get(unitKey).put(availabilityRating.getFactionCode(), availabilityRating);
        models.get(unitKey).getIncludedFactions().add(availabilityRating.getFactionCode());
    }

    /**
     * Removes the availability rating entry.
     *
     * @param era     The year of the record to remove.
     * @param unit    The model to remove the record for.
     * @param faction The faction to remove the record for.
     */
    public void removeModelFactionRating(int era, String unit, String faction) {
        if (modelIndex.containsKey(era) && modelIndex.get(era).containsKey(unit)) {
            modelIndex.get(era).get(unit).remove(faction);
        }

        for (int e : eraSet) {
            if (modelIndex.get(e).containsKey(unit) && modelIndex.get(e).get(unit).containsKey(faction)) {
                return;
            }
        }

        models.get(unit).getIncludedFactions().remove(faction);
    }

    /**
     * Provides a list of availability ratings for a chassis in a given era. Used in editing and reporting.
     *
     * @param era        The year of the record. This must be one of the years in the <code>eraSet</code>.
     * @param chassisKey The chassis name to find records for.
     *
     * @return A <code>Collection</code> of all the availability ratings for the chassis in the era, or null if there
     *       are no records for that era.
     */
    public @Nullable Collection<AvailabilityRating> getChassisFactionRatings(int era, String chassisKey) {
        if (chassisIndex.containsKey(era) && chassisIndex.get(era).containsKey(chassisKey)) {
            return chassisIndex.get(era).get(chassisKey).values();
        }

        return null;
    }

    /**
     * Adds or changes an availability rating entry for a chassis.
     *
     * @param era                The year of the record to change
     * @param unit               The name of the chassis for which to change the record
     * @param availabilityRating The new <code>AvailabilityRating</code> for the unit in the era. This provides the
     *                           faction.
     */
    public void setChassisFactionRating(int era, String unit, AvailabilityRating availabilityRating) {
        chassisIndex.get(era).computeIfAbsent(unit, k -> new HashMap<>());
        chassisIndex.get(era).get(unit).put(availabilityRating.getFactionCode(), availabilityRating);
        chassis.get(unit).getIncludedFactions().add(availabilityRating.getFactionCode());
    }

    /**
     * Removes the availability rating entry.
     *
     * @param era     The year of the record to remove.
     * @param unit    The chassis to remove the record for.
     * @param faction The faction to remove the record for.
     */
    public void removeChassisFactionRating(int era, String unit, String faction) {
        if (chassisIndex.containsKey(era) && chassisIndex.get(era).containsKey(unit)) {
            chassisIndex.get(era).get(unit).remove(faction);
        }

        for (int e : eraSet) {
            if (chassisIndex.get(e).containsKey(unit) && chassisIndex.get(e).get(unit).containsKey(faction)) {
                return;
            }
        }
        chassis.get(unit).getIncludedFactions().remove(faction);
    }

    public TreeSet<Integer> getEraSet() {
        return eraSet;
    }

    public Collection<ModelRecord> getModelList() {
        return models.values();
    }

    public ModelRecord getModelRecord(String key) {
        return models.get(key);
    }

    public Collection<ChassisRecord> getChassisList() {
        return chassis.values();
    }

    public ChassisRecord getChassisRecord(String key) {
        return chassis.get(key);
    }

    public Collection<FactionRecord> getFactionList() {
        return factions.values();
    }

    public FactionRecord getFaction(String key) {
        return factions.get(key);
    }

    public void addFaction(FactionRecord factionRecord) {
        factions.put(factionRecord.getKey(), factionRecord);
    }

    public void removeFaction(FactionRecord factionRecord) {
        factions.remove(factionRecord.getKey());
    }

    /**
     * @deprecated no indicated uses
     */
    @Deprecated(since = "0.50.05", forRemoval = true)
    public void removeFaction(String key) {
        factions.remove(key);
    }

    /**
     * @deprecated no indicated uses
     */
    @Deprecated(since = "0.50.05", forRemoval = true)
    public Collection<String> getFactionKeySet() {
        return factions.keySet();
    }

    public int eraForYear(final int year) {
        if (year < getEraSet().first()) {
            return getEraSet().first();
        } else {
            final Integer floor = getEraSet().floor(year);
            return (floor == null) ? year : floor;
        }
    }

    public boolean eraIsLoaded(int era) {
        return chassisIndex.containsKey(era);
    }

    /**
     * Used for a faction with multiple parent factions (e.g., FC == FS + LA) to find the average availability among the
     * parents. Based on average weight rather than AV rating.
     *
     * @param faction             The faction code to use for the new AvailabilityRecord
     * @param availabilityRatings A list of ratings from the various parent factions
     *
     * @return A new availability rating with the average value from the various factions.
     */
    private AvailabilityRating mergeFactionAvailability(String faction, List<AvailabilityRating> availabilityRatings) {
        if (availabilityRatings.isEmpty()) {
            return null;
        }

        double totalWt = 0;
        int totalAdj = 0;

        for (AvailabilityRating availabilityRating : availabilityRatings) {
            totalWt += AvailabilityRating.calcWeight(availabilityRating.availability);
            totalAdj += availabilityRating.ratingAdjustment;
        }
        AvailabilityRating retVal = availabilityRatings.get(0).makeCopy(faction);

        retVal.availability = (int) (AvailabilityRating.calcAvRating(totalWt / availabilityRatings.size()));
        if (totalAdj != 0) {
            retVal.ratingAdjustment = totalAdj > 0 ? 1 : -1;
        }

        return retVal;
    }

    /**
     * Given values for two years, interpolates or extrapolates value for another given year. If one of the two values
     * is null, it is treated as 0.
     *
     * @param av1   The first value.
     * @param av2   The second value.
     * @param year1 The year for the first value.
     * @param year2 The year for the second value.
     * @param now   The year for which to calculate a value.
     *
     * @return The value for the year in question. Returns null if av1 and av2 are both null.
     */
    private Double interpolate(Number av1, Number av2, int year1, int year2, int now) {
        if (av1 == null && av2 == null) {
            return null;
        }

        if (av1 == null) {
            av1 = 0.0;
        }

        if (av2 == null) {
            av2 = 0.0;
        }

        if (year1 == year2) {
            return av1.doubleValue();
        }

        return av1.doubleValue() + (av2.doubleValue() - av1.doubleValue()) * (now - year1) / (year2 - year1);
    }

    /**
     * Generate random selection table entries, given a range of parameters
     *
     * @param factionRecord  faction data for selecting units
     * @param unitType       type of unit (Mek, conventional infantry, etc.)
     * @param year           current game year
     * @param rating         equipment rating, typically A/B/C/D/F with `A` best and `F` worst
     * @param weightClasses  which weight classes to select, empty, or null for all
     * @param networkMask    type of C3 system required, 0 for none
     * @param movementModes  which movement types to select, empty, or null for all
     * @param roles          apply force generator roles when calculating random selection weights
     * @param roleStrictness how strictly to apply roles, 0 (none) or higher (more)
     * @param user           used with OmniMek and salvage balancing
     *
     * @return the list of entries suitable for building a random generation table may be empty
     */
    public List<UnitTable.TableEntry> generateTable(FactionRecord factionRecord, int unitType, int year, String rating,
          Collection<Integer> weightClasses, int networkMask, Collection<EntityMovementMode> movementModes,
          Collection<MissionRole> roles, int roleStrictness, FactionRecord user) {

        HashMap<ModelRecord, Double> unitWeights = new HashMap<>();

        loadYear(year);

        if (factionRecord == null) {
            factionRecord = new FactionRecord();
        }

        Integer currentEra = eraSet.floor(year);
        if (currentEra == null) {
            currentEra = eraSet.first();
        }
        Integer nextEra = null;
        if (!eraSet.contains(year)) {
            nextEra = eraSet.ceiling(year);
        }
        if (nextEra == null) {
            nextEra = currentEra;
        }

        /*
         * Adjustments for unit rating require knowing both how many ratings are available to the faction and where
         * the rating falls within the whole. If a faction does not have designated rating levels, it inherits those
         * of the parent faction; if there are multiple parent factions, the first match is used. Some very minor or
         * generic factions do not use rating adjustments, indicated by a rating level of -1. A faction that has one
         * rating level is a special case that always has the indicated rating within the parent faction's system.
         */

        int ratingLevel = -1;
        ArrayList<String> factionRatings = factionRecord.getRatingLevelSystem();
        int numRatingLevels = factionRatings.size();
        if (rating == null && factionRecord.getRatingLevels().size() == 1) {
            ratingLevel = factionRatings.indexOf(factionRecord.getRatingLevels().get(0));
        }

        if (rating != null && numRatingLevels > 1) {
            ratingLevel = factionRatings.indexOf(rating);
        }

        // Iterate through all available chassis
        double chassisWeightTotal = 0.0;
        for (String chassisKey : chassisIndex.get(currentEra).keySet()) {
            ChassisRecord curChassis = chassis.get(chassisKey);
            if (curChassis == null) {
                logger.error("Could not locate chassis {}", chassisKey);
                continue;
            }

            // Pre-production prototypes may show up one year before an official introduction
            if (curChassis.introYear > year + 1) {
                continue;
            }

            // Handle ChassisRecords saved as "AERO" units as ASFs for now
            if (Arrays.asList(UnitType.AERO, UnitType.AEROSPACEFIGHTER).contains(curChassis.getUnitType())) {
                curChassis.setUnitType(UnitType.AEROSPACEFIGHTER);
            }

            // Only return VTOLs when specifically requesting the unit type
            if (curChassis.getUnitType() != unitType &&
                      !(unitType == UnitType.TANK &&
                              curChassis.getUnitType() == UnitType.VTOL &&
                              movementModes.contains(EntityMovementMode.VTOL))) {
                continue;
            }

            // Preliminary filtering by weight class. Most units that have a weight class are the same for all models,
            // although a few outliers exist, so look for the first.
            if (weightClasses != null && !weightClasses.isEmpty()) {
                boolean validChassis = curChassis.getModels()
                                             .stream()
                                             .mapToInt(ModelRecord::getWeightClass)
                                             .anyMatch(weightClasses::contains);
                if (!validChassis) {
                    continue;
                }
            }

            AvailabilityRating chassisAvRating = findChassisAvailabilityRecord(currentEra,
                  chassisKey,
                  factionRecord,
                  year);

            if (chassisAvRating == null) {
                continue;
            }

            double chassisAdjRating;

            // If necessary, interpolate chassis availability between era values
            if (year != currentEra && year != nextEra) {
                AvailabilityRating chassisNextAvRating = findChassisAvailabilityRecord(nextEra,
                      chassisKey,
                      factionRecord,
                      nextEra);

                // Find the chassis availability at the start of the era, or at intro date, including dynamic modifiers
                int interpolationStart = Math.max(currentEra, Math.min(year, curChassis.introYear));
                chassisAdjRating = curChassis.calcAvailability(chassisAvRating,
                      ratingLevel,
                      numRatingLevels,
                      interpolationStart);


                double chassisNextAdj = 0.0;
                if (chassisNextAvRating != null) {
                    chassisNextAdj = curChassis.calcAvailability(chassisNextAvRating,
                          ratingLevel,
                          numRatingLevels,
                          nextEra);
                }

                if (chassisAdjRating != chassisNextAdj) {
                    chassisAdjRating = interpolate(chassisAdjRating, chassisNextAdj, interpolationStart, nextEra, year);
                }
            } else {
                // Find the chassis availability taking into account +/- dynamic modifiers and introduction year
                chassisAdjRating = curChassis.calcAvailability(chassisAvRating, ratingLevel, numRatingLevels, year);
            }

            if (chassisAdjRating > 0) {
                // Apply basic filters to models before summing the total weight
                HashSet<ModelRecord> validModels = curChassis.getFilteredModels(year,
                      weightClasses,
                      movementModes,
                      networkMask);

                HashMap<String, Double> modelWeights = new HashMap<>();

                double totalWeight = curChassis.totalModelWeight(validModels,
                      currentEra,
                      year,
                      nextEra,
                      curChassis.isOmni() ? user : factionRecord,
                      roles,
                      roleStrictness,
                      ratingLevel,
                      numRatingLevels,
                      modelWeights);

                if (totalWeight > 0 && !modelWeights.isEmpty()) {
                    double chassisWeight = AvailabilityRating.calcWeight(chassisAdjRating);
                    boolean hasModels = false;
                    for (ModelRecord curModel : validModels) {
                        if (!modelWeights.containsKey(curModel.getKey())) {
                            continue;
                        }

                        // Overall availability is the odds of the chassis multiplied by the odds of the model. Note
                        // that the chassis weight total is factored later after all chassis are processed.
                        double curWeight = chassisWeight * modelWeights.get(curModel.getKey()) / totalWeight;

                        // Add the random selection weight for this specific model to the tracker
                        if (curWeight > 0) {
                            unitWeights.put(curModel, curWeight);
                            hasModels = true;
                        }
                    }

                    if (hasModels) {
                        chassisWeightTotal += chassisWeight;
                    }
                }

            }
        }

        if (unitWeights.isEmpty() || chassisWeightTotal == 0.0) {
            return new ArrayList<>();
        }

        // Factor chassis total into every weight
        for (ModelRecord curModel : unitWeights.keySet()) {
            unitWeights.merge(curModel, chassisWeightTotal, (a, b) -> 100.0 * a / b);
        }

        // Adjust random weights based on faction weight class proportions if multiple weight classes are specified.
        // Do not re-balance conventional infantry, battle armor, VTOLs, large craft, or other unit types. Also, skip
        // when generating tables for specific roles.
        if ((weightClasses != null && weightClasses.size() > 1) &&
                  (unitType == UnitType.MEK || unitType == UnitType.TANK || unitType == UnitType.AEROSPACEFIGHTER) &&
                  (roles == null || roles.isEmpty())) {

            // Get standard weight class distribution for faction
            ArrayList<Integer> weightClassDistribution = factionRecord.getWeightDistribution(currentEra, unitType);

            if ((weightClassDistribution != null) && !weightClassDistribution.isEmpty()) {
                // Ultra-light and superheavy are too rare to warrant their own values, and for weight class
                // distribution purposes are grouped with light and assault, respectively.
                final int[] wcdIndex = { 0, 0, 1, 2, 3, 3 };
                // Find the totals of the weights for the generated table
                double totalTableWeight = unitWeights.values().stream().mapToDouble(Double::doubleValue).sum();
                // Find the sum of the weight distribution values for each weight class being called for
                int totalWCDWeights = weightClasses.stream()
                                            .filter(wc -> wcdIndex[wc] < weightClassDistribution.size())
                                            .mapToInt(wc -> weightClassDistribution.get(wcdIndex[wc]))
                                            .sum();

                if (totalWCDWeights > 0) {
                    // Group all the models of the generated table by weight class.
                    Function<ModelRecord, Integer> grouper = modelRecord -> wcdIndex[modelRecord.getWeightClass()];
                    Map<Integer, List<ModelRecord>> weightGroups = unitWeights.keySet()
                                                                         .stream()
                                                                         .collect(Collectors.groupingBy(grouper));

                    // Go through the weight class groups and adjust the table weights so the total of each group
                    // corresponds to the distribution for this faction.
                    for (int i : weightGroups.keySet()) {
                        double totalWeight = weightGroups.get(i).stream().mapToDouble(unitWeights::get).sum();
                        if (totalWeight > 0) {
                            double adj = totalTableWeight * weightClassDistribution.get(i) /
                                               (totalWeight * totalWCDWeights);
                            weightGroups.get(i)
                                  .forEach(modelRecord -> unitWeights.merge(modelRecord, adj, (x, y) -> x * y));
                        }
                    }
                }
            }
        }

        // If there are salvage percentages defined for the generating faction
        HashMap<FactionRecord, Double> salvageWeights = new HashMap<>();
        if (factionRecord.getPctSalvage(currentEra) != null) {
            HashMap<String, Double> salvageEntries = new HashMap<>();

            // If the current year is directly on an era data point, take it, otherwise interpolate between current era
            // and next era values.
            for (Entry<String, Integer> entry : factionRecord.getSalvage(currentEra).entrySet()) {
                salvageEntries.put(entry.getKey(),
                      currentEra == year ?
                            entry.getValue() :
                            interpolate(entry.getValue(),
                                  factionRecord.getSalvage(nextEra).get(entry.getKey()),
                                  currentEra,
                                  nextEra,
                                  year));
            }

            // Add salvage from the next era that is not already present
            if (!nextEra.equals(currentEra)) {
                for (Entry<String, Integer> entry : factionRecord.getSalvage(nextEra).entrySet()) {
                    if (!salvageEntries.containsKey(entry.getKey())) {
                        salvageEntries.put(entry.getKey(),
                              interpolate(0.0, entry.getValue(), currentEra, nextEra, year));
                    }
                }
            }

            // Use the total salvage percentage from the faction data to get the total weight of all salvage entries,
            // from the current overall table weight. If a salvage percentage of 100 percent is specified (unlikely,
            // but possible), then clear the existing table and regenerate everything again based purely on salvage.
            double totalTableWeight = unitWeights.values().stream().mapToDouble(Double::doubleValue).sum();
            double overallSalvage = factionRecord.getPctSalvage(currentEra);
            if (overallSalvage >= 100) {
                overallSalvage = totalTableWeight;
                unitWeights.clear();
            } else {
                overallSalvage = totalTableWeight * overallSalvage / 100.0;
            }

            // Break down the total salvage weight by relative weights of each provided salvage faction
            double totalFactionWeight = salvageEntries.values().stream().mapToDouble(Double::doubleValue).sum();
            for (String factionKey : salvageEntries.keySet()) {
                FactionRecord salvageFaction = factions.get(factionKey);
                if (salvageFaction == null) {
                    logger.debug("Could not locate faction {} for {} salvage.", factionKey, factionRecord.getKey());
                } else {
                    double factionSalvageWeight = overallSalvage * salvageEntries.get(factionKey) / totalFactionWeight;
                    salvageWeights.put(salvageFaction, factionSalvageWeight);
                }
            }
        }

        // Adjust weights of standard table entries and salvage entries for established percentages of Omni-units,
        // base Clan tech, and Star League/advanced tech. Only do this for Omni-capable unit types, which also cover
        // those which are commonly fitted with Clan or Star League/advanced technology. Do not re-balance
        // conventional infantry, battle armor, large craft, or other unit types. Also, skip when generating tables
        // for specific roles.
        if (ratingLevel >= 0 &&
                  (unitType == UnitType.MEK ||
                         unitType == UnitType.AEROSPACEFIGHTER ||
                         unitType == UnitType.TANK ||
                         unitType == UnitType.VTOL) &&
                  ((roles == null) || roles.isEmpty())) {
            adjustForRating(factionRecord,
                  unitType,
                  year,
                  ratingLevel,
                  unitWeights,
                  salvageWeights,
                  currentEra,
                  nextEra);
        }

        // Incorporate the salvage entries with the unit entries. Then re-calculate weights as necessary to keep the
        // range of values between 0 and 1000.
        double adjustments = 1.0;
        DoubleSummaryStatistics stats = Stream.concat(salvageWeights.values().stream(), unitWeights.values().stream())
                                              .mapToDouble(Double::doubleValue)
                                              .filter(d -> d > 0)
                                              .summaryStatistics();
        if ((stats.getMin() < 0.5) || (stats.getMax() > 1000)) {
            adjustments = 0.5 / stats.getMin();
            if (stats.getMax() * adjustments > 1000.0) {
                adjustments = 1000.0 / stats.getMax();
            }
        }

        List<TableEntry> retVal = new ArrayList<>();
        for (FactionRecord faction : salvageWeights.keySet()) {
            int wt = (int) (salvageWeights.get(faction) * adjustments + 0.5);
            if (wt > 0) {
                retVal.add(new TableEntry(wt, faction));
            }
        }
        for (ModelRecord modelRecord : unitWeights.keySet()) {
            int wt = (int) (unitWeights.get(modelRecord) * adjustments + 0.5);
            if (wt > 0) {
                retVal.add(new TableEntry(wt, modelRecord.getMekSummary()));
            }
        }

        return retVal;
    }

    /**
     * Adjust the weighted random selection value based on percentage values for Omni-units, Clan-tech units, and Star
     * League/advanced tech units from faction data. The {@code unitWeights} and {@code salvageWeights} parameters are
     * modified rather than returning a single unified set.
     *
     * @param factionRecord  faction used to generate units
     * @param unitType       type of unit being generated
     * @param year           current game year
     * @param rating         equipment rating based on available range, typically F (0)/D/C/B/A (4)
     * @param unitWeights    random frequency rates (entries for the table), excluding salvage
     * @param salvageWeights random frequency rates of salvaged units by faction
     * @param currentEra     current era
     * @param nextEra        next era
     */
    private void adjustForRating(FactionRecord factionRecord, int unitType, int year, int rating,
          Map<ModelRecord, Double> unitWeights, Map<FactionRecord, Double> salvageWeights, Integer currentEra,
          Integer nextEra) {

        double totalWeight = 0.0;
        double totalOmniWeight = 0.0;
        double totalClanWeight = 0.0;
        double totalSLWeight = 0.0;
        double totalOtherWeight = 0.0;

        // Total the unit weight of all selected units, plus get totals of all Omni-units, base Clan-tech units, and
        // Star League/advanced tech units
        for (Entry<ModelRecord, Double> entry : unitWeights.entrySet()) {
            totalWeight += entry.getValue();
            if (entry.getKey().isOmni()) {
                totalOmniWeight += entry.getValue();
            }
            if (entry.getKey().isSL()) {
                totalSLWeight += entry.getValue();
            } else if (!entry.getKey().isMixedOrClanTech()) {
                totalOtherWeight += entry.getValue();
            }
        }

        Double percentOmni = null;
        Double percentStarLeague = null;
        Double percentClan = null;
        Double percentOther = null;

        // Get the desired percentages from faction data and interpolate between eras if needed. Note that vehicles
        // do not re-balance based on Omni/non-Omni ratios.
        if (unitType == UnitType.MEK) {
            percentOmni = interpolate(factionRecord.findPctTech(TechCategory.OMNI, currentEra, rating),
                  factionRecord.findPctTech(TechCategory.OMNI, nextEra, rating),
                  currentEra,
                  nextEra,
                  year);
            percentClan = interpolate(factionRecord.findPctTech(TechCategory.CLAN, currentEra, rating),
                  factionRecord.findPctTech(TechCategory.CLAN, nextEra, rating),
                  currentEra,
                  nextEra,
                  year);
            percentStarLeague = interpolate(factionRecord.findPctTech(TechCategory.IS_ADVANCED, currentEra, rating),
                  factionRecord.findPctTech(TechCategory.IS_ADVANCED, nextEra, rating),
                  currentEra,
                  nextEra,
                  year);
        }

        if (unitType == UnitType.AEROSPACEFIGHTER) {
            percentOmni = interpolate(factionRecord.findPctTech(TechCategory.OMNI_AERO, currentEra, rating),
                  factionRecord.findPctTech(TechCategory.OMNI_AERO, nextEra, rating),
                  currentEra,
                  nextEra,
                  year);
            percentClan = interpolate(factionRecord.findPctTech(TechCategory.CLAN_AERO, currentEra, rating),
                  factionRecord.findPctTech(TechCategory.CLAN_AERO, nextEra, rating),
                  currentEra,
                  nextEra,
                  year);
            percentStarLeague = interpolate(factionRecord.findPctTech(TechCategory.IS_ADVANCED_AERO,
                        currentEra,
                        rating),
                  factionRecord.findPctTech(TechCategory.IS_ADVANCED_AERO, nextEra, rating),
                  currentEra,
                  nextEra,
                  year);
        }

        if (unitType == UnitType.TANK || unitType == UnitType.VTOL) {
            percentClan = interpolate(factionRecord.findPctTech(TechCategory.CLAN_VEE, currentEra, rating),
                  factionRecord.findPctTech(TechCategory.CLAN_VEE, nextEra, rating),
                  currentEra,
                  nextEra,
                  year);
            percentStarLeague = interpolate(factionRecord.findPctTech(TechCategory.IS_ADVANCED_VEE, currentEra, rating),
                  factionRecord.findPctTech(TechCategory.IS_ADVANCED_VEE, nextEra, rating),
                  currentEra,
                  nextEra,
                  year);
        }

        // Omni percentage should never be higher than the Clan percentage for Clan factions, and never higher than Clan
        // plus SL for IS factions. This may lead to unexpected results.
        if (factionRecord.isClan()) {
            if (percentOmni != null && percentClan != null && percentOmni > percentClan) {
                logger.warn("Clan faction {} Clan/SL/Omni rating has higher Omni ({}) than Clan ({}) value in era {}.",
                      factionRecord.getKey(),
                      percentOmni,
                      percentClan,
                      currentEra);
            }
        } else {
            if (percentOmni != null &&
                      percentClan != null &&
                      percentStarLeague != null &&
                      percentOmni > percentClan + percentStarLeague) {
                logger.warn(
                      "Non-Clan faction {} Clan/SL/Omni rating has higher Omni ({}) than Clan ({}) + SL ({}) value in era {}.",
                      factionRecord.getKey(),
                      percentOmni,
                      percentClan,
                      percentStarLeague,
                      currentEra);
            }
        }

        // Adjust Omni-unit percentage by margin values from faction data
        if (percentOmni != null) {
            Double omniMargin = interpolate(factionRecord.getOmniMargin(currentEra),
                  factionRecord.getOmniMargin(nextEra),
                  currentEra,
                  nextEra,
                  year);

            if (omniMargin != null && omniMargin > 0) {
                double percent = 100.0 * totalOmniWeight / totalWeight;
                if (percent < percentOmni - omniMargin) {
                    percentOmni -= omniMargin;
                } else if (percent > percentOmni + omniMargin) {
                    percentOmni += omniMargin;
                }
            }

        }

        double totalWeightPostMod = 0.0;
        // Only balance Meks and aerospace for Omni/non-Omni ratios
        if ((unitType == UnitType.MEK || unitType == UnitType.AEROSPACEFIGHTER) && percentOmni != null) {

            // Get the difference between the ideal Omni level and the current one
            double omniPctDifference = percentOmni - (100.0 * totalOmniWeight / totalWeight);

            // If there are not enough or too many Omni-units based on the faction data, re-balance all the weights
            // to bring them back into line. If the faction data specifies Omni-units but none are present, nothing
            // can be done.
            if (Math.abs(omniPctDifference) > MIN_OMNI_DIFFERENCE && totalOmniWeight > 0.0 && percentOmni >= 0.0) {
                // Total weight of non-Omni units. Sign is deliberately inverted so weights of non-Omni units are
                // moved in the opposite direction from Omni units.
                double totalNonOmniWeight = totalOmniWeight - totalWeight;

                // Apply the weight modifications proportionally, using the unit weight relative to the weight total
                // of either Omni or non-Omni as appropriate
                double curWeight;
                totalClanWeight = 0.0;
                totalSLWeight = 0.0;
                totalOtherWeight = 0.0;
                double totalOmniWeightPostMod = 0.0;
                for (ModelRecord curModel : unitWeights.keySet()) {
                    curWeight = unitWeights.get(curModel);

                    if (curModel.isOmni()) {
                        if (percentOmni > 0.0) {
                            curWeight = curWeight + curWeight * omniPctDifference / totalOmniWeight;
                        } else {
                            curWeight = 0.0;
                        }
                    } else {
                        if (percentOmni < 100.0) {
                            curWeight = curWeight + curWeight * omniPctDifference / totalNonOmniWeight;
                        } else {
                            curWeight = 0.0;
                        }
                    }

                    unitWeights.put(curModel, curWeight);
                    totalWeightPostMod += curWeight;

                    // Re-calculate total weights of the various categories
                    if (curModel.isMixedOrClanTech()) {
                        totalClanWeight += curWeight;
                    } else if (curModel.isSL()) {
                        totalSLWeight += curWeight;
                    } else {
                        totalOtherWeight += curWeight;
                    }
                    if (curModel.isOmni()) {
                        totalOmniWeightPostMod += curWeight;
                    }

                }

                if (totalOmniWeightPostMod > 0.0) {
                    totalOmniWeight = totalOmniWeightPostMod;
                }

            }

            if (totalWeightPostMod > 0.0) {
                totalWeight = totalWeightPostMod;
            }

        }

        // Use margin values from the faction data to adjust for lack of precision in post-FM:Updates era
        // extrapolations of Clan and Star League ratios
        if (percentStarLeague != null || percentClan != null) {
            percentOther = 100.0;
            if (percentStarLeague != null) {
                percentOther -= percentStarLeague;
            }

            if (percentClan != null) {
                percentOther -= percentClan;
            }
            Double techMargin = interpolate(factionRecord.getTechMargin(currentEra),
                  factionRecord.getTechMargin(nextEra),
                  currentEra,
                  nextEra,
                  year);
            if (techMargin != null && techMargin > 0) {
                if (percentClan != null) {
                    double pct = 100.0 * totalClanWeight / totalWeight;
                    if (pct < percentClan - techMargin) {
                        percentClan -= techMargin;
                    } else if (pct > percentClan + techMargin) {
                        percentClan += techMargin;
                    }
                }

                if (percentStarLeague != null) {
                    double pct = 100.0 * totalSLWeight / totalWeight;
                    if (pct < percentStarLeague - techMargin) {
                        percentStarLeague -= techMargin;
                    } else if (pct > percentStarLeague + techMargin) {
                        percentStarLeague += techMargin;
                    }
                }
            }

            Double upgradeMargin = interpolate(factionRecord.getUpgradeMargin(currentEra),
                  factionRecord.getUpgradeMargin(nextEra),
                  currentEra,
                  nextEra,
                  year);
            if ((upgradeMargin != null) && (upgradeMargin > 0)) {
                double pct = 100.0 * (totalWeight - totalClanWeight - totalSLWeight) / totalWeight;
                if (pct < percentOther - upgradeMargin) {
                    percentOther -= upgradeMargin;
                } else if (pct > percentOther + upgradeMargin) {
                    percentOther += upgradeMargin;
                }

                // If Clan-tech, Star League tech, and Other are all adjusted, the values probably don't add up to
                // 100. This is acceptable unless the upgradeMargin is less than techMargin. Then percentOther is more
                // certain, and we adjust the values of Clan-tech and Star League tech to keep the value of Other
                // equal to a percentage.

                if (techMargin != null) {
                    if (upgradeMargin <= techMargin) {
                        if (percentClan == null || percentClan == 0) {
                            percentStarLeague = 100.0 - percentOther;
                        } else if (percentStarLeague == null || percentStarLeague == 0) {
                            percentClan = 100.0 - percentOther;
                        } else {
                            percentStarLeague = (100.0 - percentOther) * percentStarLeague /
                                                      (percentStarLeague + percentClan);
                            percentClan = 100.0 - percentOther - percentStarLeague;
                        }
                    }
                }

            }
        }

        // Re-balance Star League/advanced IS tech designs against Clan and low-tech units
        if (percentStarLeague != null) {

            double slPctDifference = percentStarLeague - (100.0 * totalSLWeight / totalWeight);
            if (Math.abs(slPctDifference) > MIN_SL_DIFFERENCE && totalSLWeight > 0.0) {
                // Total weight of non-Star League/advanced units. Sign is deliberately inverted so weights of non-SL
                // units are moved in the opposite direction from SL units.
                double totalNonSLWeight = totalSLWeight - totalWeight;

                // Apply the weight modifications proportionally, using the unit weight relative to the weight total
                // of either Clan or Other as appropriate
                double curWeight;
                totalClanWeight = 0.0;
                totalOtherWeight = 0.0;
                totalOmniWeight = 0.0;
                totalWeightPostMod = 0.0;
                double totalSLWeightPostMod = 0.0;
                for (ModelRecord curModel : unitWeights.keySet()) {
                    curWeight = unitWeights.get(curModel);

                    if (curModel.isSL()) {
                        if (percentStarLeague > 0.0) {
                            curWeight = curWeight + curWeight * slPctDifference / totalSLWeight;
                        } else {
                            curWeight = 0.0;
                        }
                    } else {
                        if (percentStarLeague < 100.0) {
                            curWeight = curWeight + curWeight * slPctDifference / totalNonSLWeight;
                        } else {
                            curWeight = 0.0;
                        }
                    }

                    unitWeights.put(curModel, curWeight);
                    totalWeightPostMod += curWeight;

                    // Re-calculate total weights of the various categories
                    if (curModel.isMixedOrClanTech()) {
                        totalClanWeight += curWeight;
                    } else if (curModel.isSL()) {
                        totalSLWeightPostMod += curWeight;
                    } else {
                        totalOtherWeight += curWeight;
                    }
                    if (curModel.isOmni()) {
                        totalOmniWeight += curWeight;
                    }

                }

                if (totalWeightPostMod > 0.0) {
                    totalWeight = totalWeightPostMod;
                }
                if (totalSLWeightPostMod > 0.0) {
                    totalSLWeight = totalSLWeightPostMod;
                }
            }
        }

        // Re-balance Clan designs against SL and low-tech units to match faction data.
        double clanSalvageWeight = 0.0;
        if (percentClan != null) {

            // Non-Clan factions count salvage weights from Clan factions as Clan tech
            if (!factionRecord.isClan()) {
                clanSalvageWeight = salvageWeights.keySet()
                                          .stream()
                                          .filter(FactionRecord::isClan)
                                          .mapToDouble(salvageWeights::get)
                                          .sum();
            }

            double clanPctDifference = percentClan -
                                             (100.0 * Math.min(totalWeight, totalClanWeight + clanSalvageWeight) /
                                                    totalWeight);

            if (Math.abs(clanPctDifference) > MIN_CLAN_DIFFERENCE && totalClanWeight > 0.0) {

                // Total weight of non-Clan units, including salvage if appropriate. Sign is deliberately inverted so
                // weights of non-Clan units are moved in the opposite direction from Clan units.
                double totalNonClanWeight = Math.min(totalWeight, totalClanWeight + clanSalvageWeight) - totalWeight;

                // Apply the weight modifications proportionally, using the unit weight relative to the weight total
                // of either Star League or Other as appropriate
                double curWeight;
                totalSLWeight = 0.0;
                totalOtherWeight = 0.0;
                totalOmniWeight = 0.0;
                totalWeightPostMod = 0.0;
                double totalClanWeightPostMod = 0.0;
                for (ModelRecord curModel : unitWeights.keySet()) {
                    curWeight = unitWeights.get(curModel);

                    if (curModel.isMixedOrClanTech()) {
                        if (percentClan > 0.0) {
                            curWeight = curWeight + curWeight * clanPctDifference / totalClanWeight;
                        } else {
                            curWeight = 0.0;
                        }
                    } else {
                        if (percentClan < 100.0) {
                            curWeight = curWeight + curWeight * clanPctDifference / totalNonClanWeight;
                        } else {
                            curWeight = 0.0;
                        }
                    }

                    unitWeights.put(curModel, curWeight);
                    totalWeightPostMod += curWeight;

                    // Re-calculate total weights of the various categories
                    if (curModel.isMixedOrClanTech()) {
                        totalClanWeightPostMod += curWeight;
                    } else if (curModel.isSL()) {
                        totalSLWeight += curWeight;
                    } else {
                        totalOtherWeight += curWeight;
                    }
                    if (curModel.isOmni()) {
                        totalOmniWeight += curWeight;
                    }

                }

                if (totalWeightPostMod > 0.0) {
                    totalWeight = totalWeightPostMod;
                }
                if (totalClanWeightPostMod > 0.0) {
                    totalClanWeight = totalClanWeightPostMod;
                }
            }
        }

        // If Clan and Star League/advanced IS percentages leave no allowance for Another/low tech, then remove them
        // using weight of 0.0
        if (percentStarLeague != null &&
                  percentClan != null &&
                  (percentOther == 0.0 || percentStarLeague + percentClan >= 100.0)) {

            double pctOtherDifference = percentOther - 100.0 * totalOtherWeight / totalWeight;
            double totalAdvancedWeight = totalOtherWeight - totalWeight;

            double curWeight;
            totalOmniWeight = 0.0;
            totalClanWeight = 0.0;
            totalSLWeight = 0.0;
            totalOtherWeight = 0.0;
            totalWeightPostMod = 0.0;
            for (ModelRecord curModel : unitWeights.keySet()) {
                curWeight = unitWeights.get(curModel);

                if (!curModel.isSL() && !curModel.isMixedOrClanTech() && curWeight > 0.0) {
                    curWeight = 0.0;
                } else {
                    curWeight = curWeight + curWeight * pctOtherDifference / totalAdvancedWeight;
                }

                unitWeights.put(curModel, curWeight);
                totalWeightPostMod += curWeight;

                // Re-calculate total weights of the various categories
                if (curModel.isMixedOrClanTech()) {
                    totalClanWeight += curWeight;
                } else if (curModel.isSL()) {
                    totalSLWeight += curWeight;
                } else {
                    totalOtherWeight += curWeight;
                }
                if (curModel.isOmni()) {
                    totalOmniWeight += curWeight;
                }
            }

            if (totalWeightPostMod > 0.0) {
                totalWeight = totalWeightPostMod;
            }
        }

        // Check each of the weight totals against the faction specified percentages and log any that are
        // significantly different
        DecimalFormat pctFormatter = new DecimalFormat("#.##");
        if (percentOmni != null &&
                  Math.abs(percentOmni - (100.0 * totalOmniWeight / totalWeight)) > MIN_OMNI_DIFFERENCE) {
            logger.info(
                  "Faction {} {} Omni percentage ({}) differs significantly from faction C/SL/O data ({}) in year {}.",
                  factionRecord.getKey(),
                  UnitType.getTypeName(unitType),
                  pctFormatter.format(100.0 * totalOmniWeight / totalWeight),
                  percentOmni,
                  year);
        }

        if (percentStarLeague != null &&
                  Math.abs(percentStarLeague - (100.0 * totalSLWeight / totalWeight)) > MIN_SL_DIFFERENCE) {
            logger.info(
                  "Faction {} {} Star League/advanced IS percentage ({}) differs significantly from faction C/SL/O data ({}) in year {}.",
                  factionRecord.getKey(),
                  UnitType.getTypeName(unitType),
                  pctFormatter.format(100.0 * totalSLWeight / totalWeight),
                  percentStarLeague,
                  year);
        }

        if (percentClan != null &&
                  Math.abs(percentClan -
                                 (100.0 * Math.min(totalWeight, totalClanWeight + clanSalvageWeight) / totalWeight)) >
                        MIN_CLAN_DIFFERENCE) {
            logger.info(
                  "Faction {} {} Clan percentage ({}) differs significantly from faction C/SL/O data ({}) in year {}.",
                  factionRecord.getKey(),
                  UnitType.getTypeName(unitType),
                  pctFormatter.format(100.0 * Math.min(totalWeight, totalClanWeight + clanSalvageWeight) / totalWeight),
                  percentClan,
                  year);
        }
    }

    public void dispose() {
        interrupted = true;
        dispose = true;

        if (initialized) {
            ratGenerator = null;
        }
    }

    private synchronized void initialize(File dir) {
        // Give the MSC some time to initialize
        MekSummaryCache msc = MekSummaryCache.getInstance();
        long waitLimit = System.currentTimeMillis() + 3000; /* 3 seconds */
        while (!interrupted && !msc.isInitialized() && waitLimit > System.currentTimeMillis()) {
            try {
                wait(50);
            } catch (InterruptedException ignored) {
                // Ignore
            }
        }

        if (!(dir.exists() && dir.isDirectory())) {
            logger.error("{} is not a directory", dir);
        } else {
            loadFactions(dir);

            File[] files = dir.listFiles();

            if (files != null) {
                for (File f : files) {
                    if (f.getName().matches("\\d+\\.xml")) {
                        eraSet.add(Integer.parseInt(f.getName().replace(".xml", "")));
                    }
                }
            }
        }

        if (!interrupted) {
            ratGenerator.initialized = true;
            ratGenerator.notifyListenersOfInitialization();
        }

        if (dispose) {
            ratGenerator = null;
            dispose = false;
        }
    }

    /**
     * If the year is equal to one of the era marks, it loads that era. If it is between two, it loads eras on both
     * sides. Otherwise, load the closest era.
     */
    public void loadYear(final int year) {
        if (getEraSet().isEmpty()) {
            return;
        } else if (getEraSet().contains(year)) {
            loadEra(year);
            return;
        }

        if (year > getEraSet().first()) {
            loadEra(getEraSet().floor(year));
        }

        if (year < getEraSet().last()) {
            loadEra(getEraSet().ceiling(year));
        }
    }

    private void loadFactions(File dir) {
        File file = new MegaMekFile(dir, "factions.xml").getFile(); // TODO : Remove inline file path
        FileInputStream fis;
        try {
            fis = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            logger.error(e, "Unable to read RAT generator factions file");
            return;
        }

        Document xmlDoc;

        try {
            DocumentBuilder db = MMXMLUtility.newSafeDocumentBuilder();
            xmlDoc = db.parse(fis);
        } catch (Exception ex) {
            logger.error(ex, "loadFactions");
            return;
        }

        Element element = xmlDoc.getDocumentElement();
        NodeList childNodes = element.getChildNodes();

        element.normalize();

        for (int x = 0; x < childNodes.getLength(); x++) {
            Node workingNode = childNodes.item(x);
            if (workingNode.getNodeName().equalsIgnoreCase("faction")) {
                if (workingNode.getAttributes().getNamedItem("key") != null) {
                    FactionRecord rec = FactionRecord.createFromXml(workingNode);
                    factions.put(rec.getKey(), rec);
                } else {
                    logger.warn("Faction key not found in {}", file.getPath());
                }
            }
        }
    }

    /**
     * @param era the era to load, which may be null if there isn't anything found
     */
    private void loadEra(final @Nullable Integer era) {
        if (era != null) {
            loadEra(era, Configuration.forceGeneratorDir());
        }
    }

    private synchronized void loadEra(int era, File dir) {
        if (eraIsLoaded(era)) {
            return;
        }

        chassisIndex.put(era, new HashMap<>());
        modelIndex.put(era, new HashMap<>());
        File file = new MegaMekFile(dir, era + ".xml").getFile();
        FileInputStream fileInputStream;
        try {
            fileInputStream = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            logger.error(e, "Unable to read RAT generator file for era {}", era);
            return;
        }

        while (!MekSummaryCache.getInstance().isInitialized()) {
            try {
                wait(50);
            } catch (InterruptedException ignored) {

            }
        }

        Document xmlDoc;

        try {
            DocumentBuilder db = MMXMLUtility.newSafeDocumentBuilder();
            xmlDoc = db.parse(fileInputStream);
        } catch (Exception ex) {
            logger.error(ex, "loadEra");
            return;
        }

        Element element = xmlDoc.getDocumentElement();
        NodeList childNodes = element.getChildNodes();

        element.normalize();

        for (int x = 0; x < childNodes.getLength(); x++) {
            Node mainNode = childNodes.item(x);
            if (mainNode.getNodeName().equalsIgnoreCase("factions")) {
                for (int i = 0; i < mainNode.getChildNodes().getLength(); i++) {
                    Node workingNode = mainNode.getChildNodes().item(i);
                    if (workingNode.getNodeName().equalsIgnoreCase("faction")) {
                        String factionKey = workingNode.getAttributes().getNamedItem("key").getTextContent();
                        if (factionKey != null) {
                            FactionRecord factionRecord = factions.get(factionKey);
                            if (factionRecord != null) {
                                factionRecord.loadEra(workingNode, era);
                            } else {
                                logger.error("Faction {} not found in {}", factionKey, file.getPath());
                            }
                        } else {
                            logger.error("Faction key not found in {}", file.getPath());
                        }
                    }
                }
            } else if (mainNode.getNodeName().equalsIgnoreCase("units")) {
                for (int i = 0; i < mainNode.getChildNodes().getLength(); i++) {
                    Node workingNode = mainNode.getChildNodes().item(i);
                    if (workingNode.getNodeName().equalsIgnoreCase("chassis")) {
                        parseChassisNode(era, workingNode);
                    }
                }
            }
        }
        notifyListenersEraLoaded();
    }

    /**
     * Creates model and chassis records for all units that don't already have entries. This should only be called after
     * all availability records are loaded, otherwise they will be overwritten. Used for editing.
     */
    public void initRemainingUnits() {
        for (MekSummary mekSummary : MekSummaryCache.getInstance().getAllMeks()) {
            if (models.containsKey(mekSummary.getName())) {
                continue;
            }
            ModelRecord modelRecord = new ModelRecord(mekSummary);

            models.put(modelRecord.getKey(), modelRecord);
            String chassisKey = modelRecord.getChassisKey();
            if (chassis.containsKey(chassisKey)) {
                if (chassis.get(chassisKey).getIntroYear() == 0 ||
                          chassis.get(chassisKey).getIntroYear() > mekSummary.getYear()) {
                    chassis.get(chassisKey).setIntroYear(mekSummary.getYear());
                }
                chassis.get(chassisKey).addModel(modelRecord);
            } else {
                ChassisRecord chassisRecord = new ChassisRecord(modelRecord.getChassis());
                chassisRecord.setIntroYear(modelRecord.getIntroYear());
                chassisRecord.setOmni(modelRecord.isOmni());
                chassisRecord.setClan(modelRecord.isClan());
                chassisRecord.setUnitType(modelRecord.getUnitType());
                chassisRecord.addModel(modelRecord);
                chassis.put(chassisKey, chassisRecord);
            }
        }
    }

    private void parseChassisNode(int era, Node workingNode) {
        boolean omni = false;
        String chassisName = workingNode.getAttributes().getNamedItem("name").getTextContent();
        String unitType = workingNode.getAttributes().getNamedItem("unitType").getTextContent();
        String chassisKey = chassisName + '[' + unitType + ']';
        if (workingNode.getAttributes().getNamedItem("omni") != null) {
            omni = true;
            if (workingNode.getAttributes().getNamedItem("omni").getTextContent().equalsIgnoreCase("IS")) {
                chassisKey += "ISOmni";
            } else {
                chassisKey += "ClanOmni";
            }
        }
        ChassisRecord chassisRecord = chassis.get(chassisKey);
        if (chassisRecord == null) {
            chassisRecord = new ChassisRecord(chassisName);
            chassisRecord.setOmni(omni);
            chassisRecord.setUnitType(unitType);
            chassisRecord.setClan(chassisKey.endsWith("ClanOmni"));
            chassis.put(chassisKey, chassisRecord);
        }

        for (int j = 0; j < workingNode.getChildNodes().getLength(); j++) {
            Node availableWorkingNode = workingNode.getChildNodes().item(j);
            if (availableWorkingNode.getNodeName().equalsIgnoreCase("availability")) {
                chassisIndex.get(era).put(chassisKey, new HashMap<>());
                String[] codes = availableWorkingNode.getTextContent().trim().split(",");
                // Create a separate availability rating for each provided faction
                for (String code : codes) {
                    AvailabilityRating availabilityRating = new AvailabilityRating(chassisKey, era, code);
                    // If it provides availability values based on equipment ratings, generate index values in
                    // addition to letter values
                    if (availabilityRating.hasMultipleRatings()) {
                        availabilityRating.setRatingByNumericLevel(factions.get(availabilityRating.getFaction()));
                    }

                    chassisRecord.getIncludedFactions().add(availabilityRating.getFaction());
                    chassisIndex.get(era).get(chassisKey).put(availabilityRating.getFactionCode(), availabilityRating);

                }
            } else if (availableWorkingNode.getNodeName().equalsIgnoreCase("model")) {
                parseModelNode(era, chassisRecord, availableWorkingNode);
            }
        }
    }

    private void parseModelNode(int era, ChassisRecord chassisRecord, Node workingNode) {
        String modelKey = (chassisRecord.getChassis() +
                                 ' ' +
                                 workingNode.getAttributes().getNamedItem("name").getTextContent()).trim();
        boolean newEntry = false;
        ModelRecord modelRecord = models.get(modelKey);
        if (modelRecord == null) {
            newEntry = true;
            MekSummary mekSummary = MekSummaryCache.getInstance().getMek(modelKey);
            if (mekSummary != null) {
                modelRecord = new ModelRecord(mekSummary);
                modelRecord.setOmni(chassisRecord.isOmni());
                models.put(modelKey, modelRecord);
            }

            if (modelRecord == null) {
                logger.error("{} {} not found.",
                      chassisRecord.getChassis(),
                      workingNode.getAttributes().getNamedItem("name").getTextContent());
                return;
            }
        }
        chassisRecord.addModel(modelRecord);
        if (workingNode.getAttributes().getNamedItem("mechanized") != null) {
            modelRecord.setMechanizedBA(Boolean.parseBoolean(workingNode.getAttributes()
                                                                   .getNamedItem("mechanized")
                                                                   .getTextContent()));
        }

        for (int k = 0; k < workingNode.getChildNodes().getLength(); k++) {
            Node childNode = workingNode.getChildNodes().item(k);
            if (childNode.getNodeName().equalsIgnoreCase("roles") && newEntry) {
                modelRecord.addRoles(childNode.getTextContent().trim());
            } else if (childNode.getNodeName().equalsIgnoreCase("deployedWith") && newEntry) {
                modelRecord.setRequiredUnits(childNode.getTextContent().trim());
            } else if (childNode.getNodeName().equalsIgnoreCase("availability")) {
                modelIndex.get(era).put(modelRecord.getKey(), new HashMap<>());
                String[] codes = childNode.getTextContent().trim().split(",");
                // Create a separate availability rating for each provided faction
                for (String code : codes) {
                    AvailabilityRating availabilityRating = new AvailabilityRating(modelRecord.getKey(), era, code);
                    // If it provides availability values based on equipment ratings, generate index values in
                    // addition to letter values
                    if (availabilityRating.hasMultipleRatings()) {
                        availabilityRating.setRatingByNumericLevel(factions.get(availabilityRating.getFaction()));
                    }

                    modelRecord.getIncludedFactions().add(availabilityRating.getFaction());
                    modelIndex.get(era)
                          .get(modelRecord.getKey())
                          .put(availabilityRating.getFactionCode(), availabilityRating);

                }
            }
        }
    }

    public synchronized void registerListener(ActionListener actionListener) {
        listeners.add(actionListener);
    }

    public synchronized void removeListener(ActionListener actionListener) {
        listeners.remove(actionListener);
    }

    /**
     * Notifies all the listeners that initialization is finished
     */
    public void notifyListenersOfInitialization() {
        if (initialized) {
            // Possibility of adding a new listener during notification.
            for (ActionListener actionListener : new ArrayList<>(listeners)) {
                actionListener.actionPerformed(new ActionEvent(this,
                      ActionEvent.ACTION_PERFORMED,
                      "ratGenInitialized"));
            }
        }
    }

    /**
     * Notifies all the listeners that era is loaded
     */
    public void notifyListenersEraLoaded() {
        if (initialized) {
            for (ActionListener actionListener : listeners) {
                actionListener.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "ratGenEraLoaded"));
            }
        }
    }

    public void exportRATGen(File dir) {
        PrintWriter printWriter;

        FactionRecord[] factionRecords = factions.values().toArray(new FactionRecord[0]);
        Arrays.sort(factionRecords, (arg0, arg1) -> {
            if ((arg0.getParentFactions() == null) && (arg1.getParentFactions() != null)) {
                return -1;
            } else if ((arg0.getParentFactions() != null) && (arg1.getParentFactions() == null)) {
                return 1;
            } else if (arg0.getKey().contains(".") && !arg1.getKey().contains(".")) {
                return 1;
            } else if (!arg0.getKey().contains(".") && arg1.getKey().contains(".")) {
                return -1;
            } else {
                return arg0.getName().compareTo(arg1.getName());
            }
        });

        File file = new File(dir + "/factions.xml"); // TODO : Remove inline file path
        try {
            printWriter = new PrintWriter(file, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            logger.error(ex, "exportRATGen");
            return;
        }

        printWriter.println("<?xml version='1.0' encoding='UTF-8'?>");
        printWriter.println("<factions>");
        for (FactionRecord factionRecord : factionRecords) {
            factionRecord.writeToXml(printWriter);
        }
        printWriter.println("</factions>");
        printWriter.close();

        ChassisRecord[] chassisRecords = chassis.values().toArray(new ChassisRecord[0]);
        Arrays.sort(chassisRecords, Comparator.comparing(AbstractUnitRecord::getKey));
        ArrayList<String> avFields = new ArrayList<>();

        final List<Integer> ERAS = new ArrayList<>(eraSet);

        for (int i = 0; i < ERAS.size(); i++) {
            int era = ERAS.get(i);
            int nextEra = (i < ERAS.size() - 1) ? ERAS.get(i + 1) : Integer.MAX_VALUE;
            try {
                file = new File(dir + "/" + era + ".xml");
                printWriter = new PrintWriter(file, StandardCharsets.UTF_8);
                printWriter.println("<?xml version='1.0' encoding='UTF-8'?>");
                printWriter.println("<!-- Era " + era + "-->");
                printWriter.println("<ratgen>");
                printWriter.println("<factions>");
                for (FactionRecord factionRecord : factionRecords) {
                    if (factionRecord.isInEra(era)) {
                        factionRecord.writeToXml(printWriter, era);
                    }
                }
                printWriter.println("</factions>");
                printWriter.println("<units>");
                for (ChassisRecord chassisRecord : chassisRecords) {
                    if (chassisRecord.getIntroYear() < nextEra &&
                              chassisIndex.get(era).containsKey(chassisRecord.getKey())) {
                        avFields.clear();
                        for (AvailabilityRating availabilityRating : chassisIndex.get(era)
                                                                           .get(chassisRecord.getKey())
                                                                           .values()) {
                            if (shouldExportAv(availabilityRating, era)) {
                                avFields.add(availabilityRating.toString());
                            }
                        }

                        if (!avFields.isEmpty()) {
                            String omni = "";
                            if (chassisRecord.isOmni() && !chassisRecord.getModels().isEmpty()) {
                                omni = chassisRecord.getModels().iterator().next().isClan() ?
                                             "' omni='Clan" :
                                             "' omni='IS";
                            }
                            printWriter.println("\t<chassis name='" +
                                                      chassisRecord.getChassis().replaceAll("'", "&apos;") +
                                                      "' unitType='" +
                                                      UnitType.getTypeName(chassisRecord.getUnitType()) +
                                                      omni +
                                                      "'>");
                            printWriter.print("\t\t<availability>");
                            for (Iterator<String> iter = avFields.iterator(); iter.hasNext(); ) {
                                printWriter.print(iter.next());
                                if (iter.hasNext()) {
                                    printWriter.print(",");
                                }
                            }
                            printWriter.println("</availability>");

                            for (ModelRecord modelRecord : chassisRecord.getSortedModels()) {
                                if ((chassisRecord.getIntroYear() < nextEra) &&
                                          modelIndex.get(era).containsKey(modelRecord.getKey())) {
                                    avFields.clear();
                                    for (AvailabilityRating availabilityRating : modelIndex.get(era)
                                                                                       .get(modelRecord.getKey())
                                                                                       .values()) {
                                        if (shouldExportAv(availabilityRating, era)) {
                                            avFields.add(availabilityRating.toString());
                                        }
                                    }

                                    for (String factionKey : modelRecord.getExcludedFactions()) {
                                        avFields.add(factionKey + ":0");
                                    }

                                    if (!avFields.isEmpty()) {
                                        printWriter.print("\t\t<model name='" +
                                                                modelRecord.getModel().replaceAll("'", "&apos;"));
                                        if (modelRecord.getUnitType() == UnitType.BATTLE_ARMOR) {
                                            printWriter.print("' mechanized='" + modelRecord.canDoMechanizedBA());
                                        }
                                        printWriter.println("'>");
                                        if (!modelRecord.getRoles().isEmpty()) {
                                            String joinedRoles = modelRecord.getRoles()
                                                                       .stream()
                                                                       .map(Object::toString)
                                                                       .collect(Collectors.joining(","));
                                            if (!joinedRoles.isBlank()) {
                                                printWriter.println("\t\t\t<roles>" + joinedRoles + "</roles>");
                                            }
                                        }

                                        if (!modelRecord.getDeployedWith().isEmpty() ||
                                                  !modelRecord.getRequiredUnits().isEmpty()) {
                                            printWriter.print("\t\t\t<deployedWith>");
                                            StringJoiner stringJoiner = new StringJoiner(",");
                                            modelRecord.getDeployedWith().forEach(stringJoiner::add);
                                            modelRecord.getRequiredUnits().forEach(s -> stringJoiner.add("req:" + s));
                                            printWriter.print(stringJoiner);
                                            printWriter.println("</deployedWith>");
                                        }
                                        printWriter.print("\t\t\t<availability>");
                                        for (Iterator<String> iter = avFields.iterator(); iter.hasNext(); ) {
                                            printWriter.print(iter.next());
                                            if (iter.hasNext()) {
                                                printWriter.print(",");
                                            }
                                        }
                                        printWriter.println("</availability>");
                                        printWriter.println("\t\t</model>");
                                    }
                                }
                            }
                            printWriter.println("\t</chassis>");
                        }
                    }
                }
                printWriter.println("</units>");
                printWriter.println("</ratgen>");
                printWriter.close();
            } catch (Exception ex) {
                logger.error(ex, "exportRARGen");
            }
        }
    }

    private boolean shouldExportAv(AvailabilityRating availabilityRating, int era) {
        final FactionRecord factionRecord = factions.get(availabilityRating.getFaction());
        return (factionRecord == null) || factionRecord.isInEra(era);
    }
}
