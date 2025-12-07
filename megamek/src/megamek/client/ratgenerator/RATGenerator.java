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
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package megamek.client.ratgenerator;

import static megamek.utilities.ImageUtilities.addTintToImageIcon;

import java.awt.Color;
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
import javax.swing.ImageIcon;
import javax.xml.parsers.DocumentBuilder;

import megamek.client.ratgenerator.FactionRecord.TechCategory;
import megamek.client.ratgenerator.UnitTable.TableEntry;
import megamek.common.Configuration;
import megamek.common.annotations.Nullable;
import megamek.common.enums.Faction;
import megamek.common.loaders.MekSummary;
import megamek.common.loaders.MekSummaryCache;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.UnitType;
import megamek.common.universe.Factions2;
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
    private final static MMLogger LOGGER = MMLogger.create(RATGenerator.class);

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
            LOGGER.error("Trying to find record for unknown model {}", unit);
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
                ArrayList<AvailabilityRating> availabilityRatingList = new ArrayList<>();
                for (String curParent : factionRecord.getParentFactions()) {
                    AvailabilityRating availabilityRecord = findModelAvailabilityRecord(era, unit, curParent);
                    if (availabilityRecord != null) {
                        availabilityRatingList.add(availabilityRecord);
                    }
                }

                return mergeFactionAvailability(factionRecord.getKey(), availabilityRatingList);
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
            if (modelIndex.get(e).containsKey(unit) &&
                  modelIndex.get(e).get(unit).containsKey(faction)) {
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
            if (chassisIndex.get(e).containsKey(unit) &&
                  chassisIndex.get(e).get(unit).containsKey(faction)) {
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

    public void addFaction(FactionRecord rec) {
        factions.put(rec.getKey(), rec);
    }

    public void removeFaction(FactionRecord rec) {
        factions.remove(rec.getKey());
    }

    public void removeFaction(String key) {
        factions.remove(key);
    }

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
     * Used for a faction with multiple parent factions (e.g. FC == FS + LA) to find the average availability among the
     * parents. Based on average weight rather than av rating.
     *
     * @param faction The faction code to use for the new AvailabilityRecord
     * @param avList  A list of ratings from the various parent factions
     *
     * @return A new availability rating with the average value from the various factions.
     */
    public AvailabilityRating mergeFactionAvailability(String faction, List<AvailabilityRating> avList) {

        if (avList.isEmpty()) {
            return null;
        }

        double totalWt = 0;
        int totalAdj = 0;

        for (AvailabilityRating ar : avList) {
            totalWt += AvailabilityRating.calcWeight(ar.availability);
            totalAdj += ar.ratingAdjustment;
        }

        AvailabilityRating retVal = avList.get(0).makeCopy(faction);

        retVal.availability = (int) (AvailabilityRating.calcAvRating(totalWt / avList.size()));
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
        return av1.doubleValue()
              + (av2.doubleValue() - av1.doubleValue()) * (now - year1) / (year2 - year1);
    }

    /**
     * Generate random selection table entries, given a range of parameters
     *
     * @param fRec           faction data for selecting units
     * @param unitType       type of unit (Mek, conventional infantry, etc.)
     * @param year           current game year
     * @param rating         equipment rating, typically A/B/C/D/F with A best and F worst
     * @param weightClasses  which weight classes to select, empty or null for all
     * @param networkMask    type of C3 system required, 0 for none
     * @param movementModes  which movement types to select, empty or null for all
     * @param roles          apply force generator roles when calculating random selection weights
     * @param roleStrictness how strictly to apply roles, 0 (none) or higher (more)
     * @param user           used with OmniMek and salvage balancing
     *
     * @return list of entries suitable for building a random generation table, may be empty
     */
    public List<UnitTable.TableEntry> generateTable(FactionRecord fRec,
          int unitType,
          int year,
          String rating,
          Collection<Integer> weightClasses,
          int networkMask,
          Collection<EntityMovementMode> movementModes,
          Collection<MissionRole> roles,
          int roleStrictness,
          FactionRecord user) {

        HashMap<ModelRecord, Double> unitWeights = new HashMap<>();

        loadYear(year);

        if (fRec == null) {
            fRec = new FactionRecord();
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
         * Adjustments for unit rating require knowing both how many ratings are available
         * to the faction and where the rating falls within the whole. If a faction does
         * not have designated rating levels, it inherits those of the parent faction;
         * if there are multiple parent factions the first match is used.
         * Some very minor or generic factions do not use rating adjustments, indicated by
         *  a rating level of -1. A faction that has one rating level is a special case that
         * always has the indicated rating within the parent faction's system.
         */

        int ratingLevel = -1;
        ArrayList<String> factionRatings = fRec.getRatingLevelSystem();
        int numRatingLevels = factionRatings.size();
        if (rating == null && fRec.getRatingLevels().size() == 1) {
            ratingLevel = factionRatings.indexOf(fRec.getRatingLevels().get(0));
        }

        if (rating != null && numRatingLevels > 1) {
            ratingLevel = factionRatings.indexOf(rating);
        }

        // Iterate through all available chassis
        double chassisWeightTotal = 0.0;
        for (String chassisKey : chassisIndex.get(currentEra).keySet()) {
            ChassisRecord curChassis = chassis.get(chassisKey);
            if (curChassis == null) {
                LOGGER.error("Could not locate chassis {}", chassisKey);
                continue;
            }

            // Pre-production prototypes may show up one year before official introduction
            if (curChassis.introYear > year + 1) {
                continue;
            }

            // Handle ChassisRecords saved as "AERO" units as ASFs for now
            if (Arrays.asList(UnitType.AERO, UnitType.AEROSPACE_FIGHTER).contains(curChassis.getUnitType())) {
                curChassis.setUnitType(UnitType.AEROSPACE_FIGHTER);
            }

            // Only return VTOLs when specifically requesting the unit type
            if (curChassis.getUnitType() != unitType &&
                  !(unitType == UnitType.TANK
                        && curChassis.getUnitType() == UnitType.VTOL
                        && movementModes.contains(EntityMovementMode.VTOL))) {
                continue;
            }

            // Preliminary filtering by weight class. Most units that have a weight
            // class are the same for all models although a few outliers exist, so
            // just look for the first.
            if (weightClasses != null && !weightClasses.isEmpty()) {
                boolean validChassis = curChassis.
                      getModels().
                      stream().
                      mapToInt(ModelRecord::getWeightClass).
                      anyMatch(weightClasses::contains);
                if (!validChassis) {
                    continue;
                }
            }

            AvailabilityRating chassisAvRating = findChassisAvailabilityRecord(currentEra,
                  chassisKey, fRec, year);
            if (chassisAvRating == null) {
                continue;
            }

            double chassisAdjRating;

            // If necessary, interpolate chassis availability between era values
            if (year != currentEra && year != nextEra) {
                AvailabilityRating chassisNextAvRating = findChassisAvailabilityRecord(nextEra,
                      chassisKey,
                      fRec,
                      nextEra);

                // Find the chassis availability at the start of the era, or at
                // intro date, including dynamic modifiers
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
                    chassisAdjRating = interpolate(
                          chassisAdjRating,
                          chassisNextAdj,
                          interpolationStart,
                          nextEra,
                          year);
                }

            } else {
                // Find the chassis availability taking into account +/- dynamic
                // modifiers and introduction year
                chassisAdjRating = curChassis.calcAvailability(chassisAvRating,
                      ratingLevel, numRatingLevels, year);
            }

            if (chassisAdjRating > 0) {

                // Apply basic filters to models before summing the total weight
                HashSet<ModelRecord> validModels = curChassis.getFilteredModels(year,
                      weightClasses, movementModes, networkMask);

                HashMap<String, Double> modelWeights = new HashMap<>();

                double totalWeight = curChassis.totalModelWeight(validModels,
                      currentEra,
                      year,
                      nextEra,
                      curChassis.isOmni() ? user : fRec,
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

                        // Overall availability is the odds of the chassis multiplied
                        // by the odds of the model. Note that the chassis weight total
                        // is factored later after all chassis are processed.
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

        // Adjust random weights based on faction weight class proportions if multiple
        // weight classes are specified.
        // Do not re-balance conventional infantry, battle armor, VTOLs, large craft,
        // or other unit types. Also skip when generating tables for specific roles.
        if ((weightClasses != null &&
              weightClasses.size() > 1) &&
              (unitType == UnitType.MEK ||
                    unitType == UnitType.TANK ||
                    unitType == UnitType.AEROSPACE_FIGHTER) &&
              (roles == null || roles.isEmpty())) {

            // Get standard weight class distribution for faction
            ArrayList<Integer> weightClassDistribution = fRec.getWeightDistribution(currentEra,
                  unitType);

            if ((weightClassDistribution != null) && !weightClassDistribution.isEmpty()) {
                // Ultra-light and superheavy are too rare to warrant their own values and for
                // weight class distribution purposes are grouped with light and assault,
                // respectively.
                final int[] wcdIndex = { 0, 0, 1, 2, 3, 3 };
                // Find the totals of the weights for the generated table
                double totalTableWeight = unitWeights.values().stream()
                      .mapToDouble(Double::doubleValue)
                      .sum();
                // Find the sum of the weight distribution values for each weight
                // class being called for
                int totalWCDWeights = weightClasses.stream()
                      .filter(wc -> wcdIndex[wc] < weightClassDistribution.size())
                      .mapToInt(wc -> weightClassDistribution.get(wcdIndex[wc]))
                      .sum();

                if (totalWCDWeights > 0) {
                    // Group all the models of the generated table by weight class.
                    Function<ModelRecord, Integer> grouper = mr -> wcdIndex[mr.getWeightClass()];
                    Map<Integer, List<ModelRecord>> weightGroups = unitWeights.
                          keySet().
                          stream()
                          .collect(Collectors.groupingBy(grouper));

                    // Go through the weight class groups and adjust the table weights so the total
                    // of each group corresponds to the distribution for this faction.
                    for (int i : weightGroups.keySet()) {
                        double totalWeight = weightGroups.get(i).stream()
                              .mapToDouble(unitWeights::get)
                              .sum();
                        if (totalWeight > 0) {
                            double adj = totalTableWeight * weightClassDistribution.get(i) /
                                  (totalWeight * totalWCDWeights);
                            weightGroups.get(i).forEach(mr -> unitWeights.merge(mr, adj, (x, y) -> x * y));
                        }
                    }
                }
            }
        }

        // If there are salvage percentages defined for the generating faction
        HashMap<FactionRecord, Double> salvageWeights = new HashMap<>();
        if (fRec.getPctSalvage(currentEra) != null) {
            HashMap<String, Double> salvageEntries = new HashMap<>();

            // If current year is directly on an era data point take it, otherwise
            // interpolate between current era and next era values.
            for (Entry<String, Integer> entry : fRec.getSalvage(currentEra).entrySet()) {
                salvageEntries.put(entry.getKey(),
                      currentEra == year ?
                            entry.getValue() :
                            interpolate(entry.getValue(),
                                  fRec.getSalvage(nextEra).get(entry.getKey()),
                                  currentEra,
                                  nextEra,
                                  year));
            }

            // Add salvage from the next era that is not already present
            if (!nextEra.equals(currentEra)) {
                for (Entry<String, Integer> entry : fRec.getSalvage(nextEra).entrySet()) {
                    if (!salvageEntries.containsKey(entry.getKey())) {
                        salvageEntries.put(entry.getKey(),
                              interpolate(0.0,
                                    entry.getValue(),
                                    currentEra,
                                    nextEra,
                                    year));
                    }
                }
            }

            // Use the total salvage percentage from the faction data to get the total
            // weight of all salvage entries, from the current overall table weight.
            // If a salvage percentage of 100 percent is specified (unlikely, but possible)
            // then clear the existing table and regenerate everything again based purely
            // on salvage.
            double totalTableWeight = unitWeights.values().stream().mapToDouble(Double::doubleValue).sum();
            double overallSalvage = fRec.getPctSalvage(currentEra);
            if (overallSalvage >= 100) {
                overallSalvage = totalTableWeight;
                unitWeights.clear();
            } else {
                overallSalvage = totalTableWeight * overallSalvage / 100.0;
            }

            // Break down the total salvage weight by relative weights of each
            // provided salvage faction
            double totalFactionWeight = salvageEntries.values().stream()
                  .mapToDouble(Double::doubleValue)
                  .sum();
            for (String fKey : salvageEntries.keySet()) {
                FactionRecord salvageFaction = factions.get(fKey);
                if (salvageFaction == null) {
                    LOGGER.debug("Could not locate faction {} for {} salvage.",
                          fKey, fRec.getKey());
                } else {
                    double factionSalvageWeight = overallSalvage * salvageEntries.get(fKey) / totalFactionWeight;
                    salvageWeights.put(salvageFaction, factionSalvageWeight);
                }
            }
        }

        // Adjust weights of standard table entries and salvage entries for established
        // percentages of Omni-units, base Clan tech, and Star League/advanced tech.
        // Only do this for Omni-capable unit types, which also covers those which are
        // commonly fitted with Clan or Star League/advanced technology.
        // Do not re-balance conventional infantry, battle armor, large craft, or other
        // unit types. Also skip when generating tables for specific roles.
        if (ratingLevel >= 0 &&
              (unitType == UnitType.MEK ||
                    unitType == UnitType.AEROSPACE_FIGHTER ||
                    unitType == UnitType.TANK ||
                    unitType == UnitType.VTOL) &&
              ((roles == null) || roles.isEmpty())) {
            adjustForRating(fRec, unitType, year, ratingLevel, unitWeights, salvageWeights, currentEra, nextEra);
        }

        // Incorporate the salvage entries with the unit entries. Then re-calculate
        // weights as necessary to keep the range of values between 0 and 1000.
        double adj = 1.0;
        DoubleSummaryStatistics stats = Stream
              .concat(salvageWeights.values().stream(), unitWeights.values().stream())
              .mapToDouble(Double::doubleValue)
              .filter(d -> d > 0)
              .summaryStatistics();
        if ((stats.getMin() < 0.5) || (stats.getMax() > 1000)) {
            adj = 0.5 / stats.getMin();
            if (stats.getMax() * adj > 1000.0) {
                adj = 1000.0 / stats.getMax();
            }
        }

        List<TableEntry> retVal = new ArrayList<>();
        for (FactionRecord faction : salvageWeights.keySet()) {
            int wt = (int) (salvageWeights.get(faction) * adj + 0.5);
            if (wt > 0) {
                retVal.add(new TableEntry(wt, faction));
            }
        }
        for (ModelRecord mRec : unitWeights.keySet()) {
            int wt = (int) (unitWeights.get(mRec) * adj + 0.5);
            if (wt > 0) {
                retVal.add(new TableEntry(wt, mRec.getMekSummary()));
            }
        }

        return retVal;
    }

    /**
     * Adjust weighted random selection value based on percentage values for Omni-units, Clan-tech units, and Star
     * League/advanced tech units from faction data. The {@code unitWeights} and {@code salvageWeights} parameters are
     * modified rather than returning a single unified set.
     *
     * @param fRec           faction used to generate units
     * @param unitType       type of unit being generated
     * @param year           current game year
     * @param rating         equipment rating based on available range, typically F (0)/D/C/B/A (4)
     * @param unitWeights    random frequency rates (entries for the table), excluding salvage
     * @param salvageWeights random frequency rates of salvaged units by faction
     * @param currentEra     current era
     * @param nextEra        next era
     */
    private void adjustForRating(FactionRecord fRec,
          int unitType,
          int year,
          int rating,
          Map<ModelRecord, Double> unitWeights,
          Map<FactionRecord, Double> salvageWeights,
          Integer currentEra,
          Integer nextEra) {

        double totalWeight = 0.0;
        double totalOmniWeight = 0.0;
        double totalClanWeight = 0.0;
        double totalSLWeight = 0.0;
        double totalOtherWeight = 0.0;

        // Total the unit weight of all selected units, plus get totals
        // of all Omni-units, base Clan-tech units, and Star League/advanced
        // tech units
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

        Double pctOmni = null;
        Double pctSL = null;
        Double pctClan = null;
        Double pctOther = null;

        // Get the desired percentages from faction data, and interpolate between
        // eras if needed.
        // Note that vehicles do not re-balance based on Omni/non-Omni ratios.
        if (unitType == UnitType.MEK) {
            pctOmni = interpolate(fRec.findPctTech(TechCategory.OMNI, currentEra, rating),
                  fRec.findPctTech(TechCategory.OMNI, nextEra, rating), currentEra, nextEra, year);
            pctClan = interpolate(fRec.findPctTech(TechCategory.CLAN, currentEra, rating),
                  fRec.findPctTech(TechCategory.CLAN, nextEra, rating), currentEra, nextEra, year);
            pctSL = interpolate(fRec.findPctTech(TechCategory.IS_ADVANCED, currentEra, rating),
                  fRec.findPctTech(TechCategory.IS_ADVANCED, nextEra, rating), currentEra, nextEra, year);
        }
        if (unitType == UnitType.AEROSPACE_FIGHTER) {
            pctOmni = interpolate(fRec.findPctTech(TechCategory.OMNI_AERO, currentEra, rating),
                  fRec.findPctTech(TechCategory.OMNI_AERO, nextEra, rating), currentEra, nextEra, year);
            pctClan = interpolate(fRec.findPctTech(TechCategory.CLAN_AERO, currentEra, rating),
                  fRec.findPctTech(TechCategory.CLAN_AERO, nextEra, rating), currentEra, nextEra, year);
            pctSL = interpolate(fRec.findPctTech(TechCategory.IS_ADVANCED_AERO, currentEra, rating),
                  fRec.findPctTech(TechCategory.IS_ADVANCED_AERO, nextEra, rating), currentEra, nextEra, year);
        }
        if (unitType == UnitType.TANK || unitType == UnitType.VTOL) {
            pctClan = interpolate(fRec.findPctTech(TechCategory.CLAN_VEE, currentEra, rating),
                  fRec.findPctTech(TechCategory.CLAN_VEE, nextEra, rating), currentEra, nextEra, year);
            pctSL = interpolate(fRec.findPctTech(TechCategory.IS_ADVANCED_VEE, currentEra, rating),
                  fRec.findPctTech(TechCategory.IS_ADVANCED_VEE, nextEra, rating), currentEra, nextEra, year);
        }

        // Omni percentage should never be higher than Clan percentage for
        // Clan factions, and never higher than Clan plus SL for IS factions.
        // This may lead to unexpected results.
        if (fRec.isClan()) {
            if (pctOmni != null && pctClan != null && pctOmni > pctClan) {
                LOGGER.warn("Clan faction {} Clan/SL/Omni rating has" +
                            " higher Omni ({}) than Clan ({}) value in era {}.",
                      fRec.getKey(), pctOmni, pctClan, currentEra);
            }
        } else {
            if (pctOmni != null && pctClan != null && pctSL != null && pctOmni > pctClan + pctSL) {
                LOGGER.warn("Non-Clan faction {} Clan/SL/Omni rating has" +
                            " higher Omni ({}) than Clan ({}) + SL ({}) value in era {}.",
                      fRec.getKey(), pctOmni, pctClan, pctSL, currentEra);
            }
        }

        // Adjust Omni-unit percentage by margin values from faction data
        if (pctOmni != null) {
            Double omniMargin = interpolate(fRec.getOmniMargin(currentEra), fRec.getOmniMargin(nextEra),
                  currentEra, nextEra, year);

            if (omniMargin != null && omniMargin > 0) {
                double pct = 100.0 * totalOmniWeight / totalWeight;
                if (pct < pctOmni - omniMargin) {
                    pctOmni -= omniMargin;
                } else if (pct > pctOmni + omniMargin) {
                    pctOmni += omniMargin;
                }
            }

        }

        double totalWeightPostMod = 0.0;
        // Only balance Meks and aerospace for Omni/non-Omni ratios
        if ((unitType == UnitType.MEK || unitType == UnitType.AEROSPACE_FIGHTER) && pctOmni != null) {

            // Get the difference between the ideal Omni level and the current one
            double omniPctDifference = pctOmni - (100.0 * totalOmniWeight / totalWeight);

            // If there are not enough or too many Omni-units based on the faction data,
            // re-balance all the weights to bring them back into line. If the faction
            // data specifies Omni-units but none are present, nothing can be done.
            if (Math.abs(omniPctDifference) > MIN_OMNI_DIFFERENCE && totalOmniWeight > 0.0 && pctOmni >= 0.0) {

                // Total weight of non-Omni units. Sign is deliberately inverted
                // so weights of non-Omni units are moved in opposite direction from
                // Omni units.
                double totalNonOmniWeight = totalOmniWeight - totalWeight;

                // Apply the weight modifications proportionally, using the unit weight relative
                // to the weight total of either Omni or non-Omni as appropriate
                double curWeight;
                totalClanWeight = 0.0;
                totalSLWeight = 0.0;
                totalOtherWeight = 0.0;
                double totalOmniWeightPostMod = 0.0;
                for (ModelRecord curModel : unitWeights.keySet()) {
                    curWeight = unitWeights.get(curModel);

                    if (curModel.isOmni()) {
                        if (pctOmni > 0.0) {
                            curWeight = curWeight + curWeight * omniPctDifference / totalOmniWeight;
                        } else {
                            curWeight = 0.0;
                        }
                    } else {
                        if (pctOmni < 100.0) {
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

        // Use margin values from the faction data to adjust for lack of precision
        // in post-FM:Updates era extrapolations of Clan and Star League ratios
        if (pctSL != null || pctClan != null) {
            pctOther = 100.0;
            if (pctSL != null) {
                pctOther -= pctSL;
            }

            if (pctClan != null) {
                pctOther -= pctClan;
            }
            Double techMargin = interpolate(fRec.getTechMargin(currentEra),
                  fRec.getTechMargin(nextEra), currentEra, nextEra, year);
            if (techMargin != null && techMargin > 0) {
                if (pctClan != null) {
                    double pct = 100.0 * totalClanWeight / totalWeight;
                    if (pct < pctClan - techMargin) {
                        pctClan -= techMargin;
                    } else if (pct > pctClan + techMargin) {
                        pctClan += techMargin;
                    }
                }

                if (pctSL != null) {
                    double pct = 100.0 * totalSLWeight / totalWeight;
                    if (pct < pctSL - techMargin) {
                        pctSL -= techMargin;
                    } else if (pct > pctSL + techMargin) {
                        pctSL += techMargin;
                    }
                }
            }

            Double upgradeMargin = interpolate(fRec.getUpgradeMargin(currentEra),
                  fRec.getUpgradeMargin(nextEra), currentEra, nextEra, year);
            if ((upgradeMargin != null) && (upgradeMargin > 0)) {
                double pct = 100.0 * (totalWeight - totalClanWeight - totalSLWeight) / totalWeight;
                if (pct < pctOther - upgradeMargin) {
                    pctOther -= upgradeMargin;
                } else if (pct > pctOther + upgradeMargin) {
                    pctOther += upgradeMargin;
                }

                // If Clan-tech, Star League tech, and Other are all adjusted, the
                // values probably don't add up to 100. This is acceptable unless
                // the upgradeMargin is less than techMargin. Then pctOther is more
                // certain, and we adjust the values of Clan-tech and Star League
                // tech to keep the value of Other equal to a percentage.

                if (techMargin != null) {
                    if (upgradeMargin <= techMargin) {
                        if (pctClan == null || pctClan == 0) {
                            pctSL = 100.0 - pctOther;
                        } else if (pctSL == null || pctSL == 0) {
                            pctClan = 100.0 - pctOther;
                        } else {
                            pctSL = (100.0 - pctOther) * pctSL / (pctSL + pctClan);
                            pctClan = 100.0 - pctOther - pctSL;
                        }
                    }
                }

            }
        }

        // Re-balance Star League/advanced IS tech designs against Clan and
        // low-tech units
        if (pctSL != null) {

            double slPctDifference = pctSL - (100.0 * totalSLWeight / totalWeight);
            if (Math.abs(slPctDifference) > MIN_SL_DIFFERENCE && totalSLWeight > 0.0) {

                // Total weight of non-Star League/advanced units. Sign is deliberately
                // inverted so weights of non-SL units are moved in opposite direction from
                // SL units.
                double totalNonSLWeight = totalSLWeight - totalWeight;

                // Apply the weight modifications proportionally, using the unit weight relative
                // to the weight total of either Clan or Other as appropriate
                double curWeight;
                totalClanWeight = 0.0;
                totalOtherWeight = 0.0;
                totalOmniWeight = 0.0;
                totalWeightPostMod = 0.0;
                double totalSLWeightPostMod = 0.0;
                for (ModelRecord curModel : unitWeights.keySet()) {
                    curWeight = unitWeights.get(curModel);

                    if (curModel.isSL()) {
                        if (pctSL > 0.0) {
                            curWeight = curWeight + curWeight * slPctDifference / totalSLWeight;
                        } else {
                            curWeight = 0.0;
                        }
                    } else {
                        if (pctSL < 100.0) {
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

        // Re-balance Clan designs against SL and low-tech units to match
        // faction data.
        double clanSalvageWeight = 0.0;
        if (pctClan != null) {

            // Non-Clan factions count salvage weights from Clan factions as Clan tech
            if (!fRec.isClan()) {
                clanSalvageWeight = salvageWeights.
                      keySet().
                      stream().
                      filter(FactionRecord::isClan).
                      mapToDouble(salvageWeights::get).
                      sum();
            }

            double clanPctDifference = pctClan -
                  (100.0 * Math.min(totalWeight, totalClanWeight + clanSalvageWeight) /
                        totalWeight);

            if (Math.abs(clanPctDifference) > MIN_CLAN_DIFFERENCE && totalClanWeight > 0.0) {

                // Total weight of non-Clan units, including salvage if appropriate.
                // Sign is deliberately inverted so weights of non-Clan units are moved
                // in opposite direction from Clan units.
                double totalNonClanWeight = Math.min(totalWeight, totalClanWeight + clanSalvageWeight) - totalWeight;

                // Apply the weight modifications proportionally, using the unit weight relative
                // to the weight total of either Star League or Other as appropriate
                double curWeight;
                totalSLWeight = 0.0;
                totalOtherWeight = 0.0;
                totalOmniWeight = 0.0;
                totalWeightPostMod = 0.0;
                double totalClanWeightPostMod = 0.0;
                for (ModelRecord curModel : unitWeights.keySet()) {
                    curWeight = unitWeights.get(curModel);

                    if (curModel.isMixedOrClanTech()) {
                        if (pctClan > 0.0) {
                            curWeight = curWeight + curWeight * clanPctDifference / totalClanWeight;
                        } else {
                            curWeight = 0.0;
                        }
                    } else {
                        if (pctClan < 100.0) {
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

        // If Clan and Star League/advanced IS percentages leave no allowance
        // for Other/low-tech then remove them using weight of 0.0

        if (pctSL != null && pctClan != null &&
              (pctOther == 0.0 || pctSL + pctClan >= 100.0)) {

            double pctOtherDifference = pctOther - 100.0 * totalOtherWeight / totalWeight;
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

        // Check each of the weight totals against the faction specified percentages
        // and log any that are significantly different
        DecimalFormat pctFormatter = new DecimalFormat("#.##");
        if (pctOmni != null &&
              Math.abs(pctOmni - (100.0 * totalOmniWeight / totalWeight)) > MIN_OMNI_DIFFERENCE) {
            LOGGER.info("Faction {} {} Omni percentage ({}) differs significantly from" +
                        " faction C/SL/O data ({}) in year {}.",
                  fRec.getKey(),
                  UnitType.getTypeName(unitType),
                  pctFormatter.format(100.0 * totalOmniWeight / totalWeight),
                  pctOmni,
                  year);
        }
        if (pctSL != null &&
              Math.abs(pctSL - (100.0 * totalSLWeight / totalWeight)) > MIN_SL_DIFFERENCE) {
            LOGGER.info("Faction {} {} Star League/advanced IS percentage ({}) differs" +
                        " significantly from faction C/SL/O data ({}) in year {}.",
                  fRec.getKey(),
                  UnitType.getTypeName(unitType),
                  pctFormatter.format(100.0 * totalSLWeight / totalWeight),
                  pctSL,
                  year);
        }
        if (pctClan != null &&
              Math.abs(pctClan -
                    (100.0 * Math.min(totalWeight, totalClanWeight + clanSalvageWeight) / totalWeight)
              ) > MIN_CLAN_DIFFERENCE) {
            LOGGER.info("Faction {} {} Clan percentage ({}) differs significantly from" +
                        " faction C/SL/O data ({}) in year {}.",
                  fRec.getKey(),
                  UnitType.getTypeName(unitType),
                  pctFormatter.format(100.0 * Math.min(totalWeight, totalClanWeight + clanSalvageWeight) / totalWeight),
                  pctClan,
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
        long waitLimit = java.lang.System.currentTimeMillis() + 3000; /* 3 seconds */
        while (!interrupted && !msc.isInitialized() && waitLimit > java.lang.System.currentTimeMillis()) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                // Ignore
            }
        }

        if (!(dir.exists() && dir.isDirectory())) {
            LOGGER.error("{} is not a directory", dir);
        } else {
            loadFactions(dir);
            File[] files = dir.listFiles();

            if (files != null) {
                for (File file : files) {
                    if (file.getName().matches("\\d+\\.xml")) {
                        eraSet.add(Integer.parseInt(file.getName().replace(".xml", "")));
                    }
                }
            }
        }

        if (!interrupted) {
            ratGenerator.initialized = true;
            ratGenerator.getEraSet().forEach(e -> ratGenerator.loadEra(e, dir));
            ratGenerator.notifyListenersOfInitialization();
        }

        if (dispose) {
            ratGenerator = null;
            dispose = false;
        }
    }

    /**
     * If the year is equal to one of the era marks, it loads that era. If it is between two, it loads eras on both
     * sides. Otherwise, just load the closest era.
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
        // As a temporary measure, the RAT Generator factions are populated from the new unified factions
        // list instead of using that directly.
        var yamlFactions = Factions2.getInstance();
        yamlFactions.getFactions().stream()
              .map(FactionRecord::new)
              .forEach(f -> factions.put(f.getKey(), f));
        // Since the unification of MHQ and RatGen factions, every faction can create a FactionRecord but not every
        // FactionRecord has enough data to produce units, so remove those
        factions.values().removeIf(fr -> fr.getRatingLevelSystem().isEmpty());
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
        FileInputStream fis;
        try {
            fis = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            LOGGER.error(e, "Unable to read RAT generator file for era {}", era);
            return;
        }
        while (!MekSummaryCache.getInstance().isInitialized()) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException ignored) {

            }
        }

        Document xmlDoc;

        try {
            DocumentBuilder db = MMXMLUtility.newSafeDocumentBuilder();
            xmlDoc = db.parse(fis);
        } catch (Exception ex) {
            LOGGER.error(ex, "loadEra");
            return;
        }

        Element element = xmlDoc.getDocumentElement();
        NodeList nl = element.getChildNodes();

        element.normalize();

        for (int x = 0; x < nl.getLength(); x++) {
            Node mainNode = nl.item(x);
            if (mainNode.getNodeName().equalsIgnoreCase("factions")) {
                for (int i = 0; i < mainNode.getChildNodes().getLength(); i++) {
                    Node wn = mainNode.getChildNodes().item(i);
                    if (wn.getNodeName().equalsIgnoreCase("faction")) {
                        String fKey = wn.getAttributes().getNamedItem("key").getTextContent();
                        if (fKey != null) {
                            FactionRecord rec = factions.get(fKey);
                            if (rec != null) {
                                rec.loadEra(wn, era);
                            } else {
                                LOGGER.error("Faction {} not found in {}", fKey, file.getPath());
                            }
                        } else {
                            LOGGER.error("Faction key not found in {}", file.getPath());
                        }
                    }
                }
            } else if (mainNode.getNodeName().equalsIgnoreCase("units")) {
                for (int i = 0; i < mainNode.getChildNodes().getLength(); i++) {
                    Node wn = mainNode.getChildNodes().item(i);
                    if (wn.getNodeName().equalsIgnoreCase("chassis")) {
                        parseChassisNode(era, wn);
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
        for (MekSummary ms : MekSummaryCache.getInstance().getAllMeks()) {
            if (models.containsKey(ms.getName())) {
                continue;
            }

            ModelRecord mr = new ModelRecord(ms);

            models.put(mr.getKey(), mr);
            String chassisKey = mr.getChassisKey();

            if (chassis.containsKey(chassisKey)) {
                if (chassis.get(chassisKey).getIntroYear() == 0 ||
                      chassis.get(chassisKey).getIntroYear() > ms.getYear()) {
                    chassis.get(chassisKey).setIntroYear(ms.getYear());
                }
                chassis.get(chassisKey).addModel(mr);
            } else {
                ChassisRecord cr = new ChassisRecord(mr.getChassis());
                cr.setIntroYear(mr.getIntroYear());
                cr.setOmni(mr.isOmni());
                cr.setClan(mr.isClan());
                cr.setUnitType(mr.getUnitType());
                cr.addModel(mr);
                chassis.put(chassisKey, cr);
            }
        }
    }

    private void parseChassisNode(int era, Node wn) {
        boolean omni = false;
        String chassisName = wn.getAttributes().getNamedItem("name").getTextContent();
        String unitType = wn.getAttributes().getNamedItem("unitType").getTextContent();
        String chassisKey = chassisName + '[' + unitType + ']';
        if (wn.getAttributes().getNamedItem("omni") != null) {
            omni = true;
            if (wn.getAttributes().getNamedItem("omni").getTextContent().equalsIgnoreCase("IS")) {
                chassisKey += "ISOmni";
            } else {
                chassisKey += "ClanOmni";
            }
        }
        ChassisRecord cr = chassis.get(chassisKey);
        if (cr == null) {
            cr = new ChassisRecord(chassisName);
            cr.setOmni(omni);
            cr.setUnitType(unitType);
            cr.setClan(chassisKey.endsWith("ClanOmni"));
            chassis.put(chassisKey, cr);
        }

        for (int j = 0; j < wn.getChildNodes().getLength(); j++) {
            Node wn2 = wn.getChildNodes().item(j);
            if (wn2.getNodeName().equalsIgnoreCase("availability")) {
                chassisIndex.get(era).put(chassisKey, new HashMap<>());
                String[] codes = wn2.getTextContent().trim().split(",");
                // Create a separate availability rating for each provided faction
                for (String code : codes) {

                    AvailabilityRating ar = new AvailabilityRating(chassisKey, era, code);
                    FactionRecord chassisFaction = factions.get(ar.getFaction());
                    if (null != chassisFaction || code.startsWith("General")) {

                        // If it provides availability values based on equipment ratings,
                        // generate index values in addition to letter values
                        if (ar.hasMultipleRatings()) {
                            ar.setRatingByNumericLevel(factions.get(ar.getFaction()));
                        }

                        cr.getIncludedFactions().add(ar.getFaction());
                        chassisIndex.get(era).get(chassisKey).put(ar.getFactionCode(), ar);

                    } else {
                        LOGGER.warn("{} not a valid faction code in year {}. See {}.",
                              ar.getFaction(),
                              ar.era,
                              ar.unitName);
                    }

                }
            } else if (wn2.getNodeName().equalsIgnoreCase("model")) {
                parseModelNode(era, cr, wn2);
            }
        }
    }

    private void parseModelNode(int era, ChassisRecord chassisRecord, Node node) {
        String modelKey = (chassisRecord.getChassis() + ' ' + node.getAttributes()
              .getNamedItem("name")
              .getTextContent()).trim();
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
                LOGGER.error("{} {} not found.",
                      chassisRecord.getChassis(),
                      node.getAttributes().getNamedItem("name").getTextContent());
                return;
            }
        }
        chassisRecord.addModel(modelRecord);
        if (node.getAttributes().getNamedItem("mechanized") != null) {
            modelRecord.setMechanizedBA(Boolean.parseBoolean(node.getAttributes()
                  .getNamedItem("mechanized")
                  .getTextContent()));
        }

        for (int k = 0; k < node.getChildNodes().getLength(); k++) {
            Node wn2 = node.getChildNodes().item(k);
            if (wn2.getNodeName().equalsIgnoreCase("roles") && newEntry) {
                modelRecord.addRoles(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("deployedWith") && newEntry) {
                modelRecord.setRequiredUnits(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("availability")) {
                modelIndex.get(era).put(modelRecord.getKey(), new HashMap<>());
                String[] codes = wn2.getTextContent().trim().split(",");
                // Create a separate availability rating for each provided faction
                for (String code : codes) {

                    AvailabilityRating ar = new AvailabilityRating(modelRecord.getKey(), era, code);
                    FactionRecord modelFaction = factions.get(ar.getFaction());
                    if (null != modelFaction || code.startsWith("General")) {
                        // If it provides availability values based on equipment ratings,
                        // generate index values in addition to letter values
                        if (ar.hasMultipleRatings()) {
                            ar.setRatingByNumericLevel(factions.get(ar.getFaction()));
                        }

                        modelRecord.getIncludedFactions().add(ar.getFaction());
                        modelIndex.get(era).get(modelRecord.getKey()).put(ar.getFactionCode(), ar);

                    } else {
                        LOGGER.warn("{} not a valid faction code in year {}. See model {}.",
                              ar.getFaction(),
                              ar.era,
                              ar.unitName);
                    }

                }
            }
        }
    }

    public synchronized void registerListener(ActionListener l) {
        listeners.add(l);
    }

    public synchronized void removeListener(ActionListener l) {
        listeners.remove(l);
    }

    /**
     * Notifies all the listeners that initialization is finished
     */
    public void notifyListenersOfInitialization() {
        if (initialized) {
            // Possibility of adding a new listener during notification.
            for (ActionListener l : new ArrayList<>(listeners)) {
                l.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "ratGenInitialized"));
            }
        }
    }

    /**
     * Notifies all the listeners that era is loaded
     */
    public void notifyListenersEraLoaded() {
        if (initialized) {
            for (ActionListener l : listeners) {
                l.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "ratGenEraLoaded"));
            }
        }
    }

    public void exportRATGen(File dir) {
        PrintWriter pw;

        FactionRecord[] factionRecs = factions.values().toArray(new FactionRecord[0]);
        for (FactionRecord fRec : factionRecs) {
            try {
                fRec.saveIfChanged();
            } catch (Exception ex) {
                LOGGER.error(ex, "exportRATGen");
                return;
            }
        }

        ChassisRecord[] chassisRecs = chassis.values().toArray(new ChassisRecord[0]);
        Arrays.sort(chassisRecs, Comparator.comparing(AbstractUnitRecord::getKey));
        ArrayList<String> avFields = new ArrayList<>();

        final List<Integer> ERAS = new ArrayList<>(eraSet);

        for (int i = 0; i < ERAS.size(); i++) {
            int era = ERAS.get(i);
            int nextEra = (i < ERAS.size() - 1) ? ERAS.get(i + 1) : Integer.MAX_VALUE;
            try {
                File file = new File(dir + "/" + era + ".xml");
                pw = new PrintWriter(file, StandardCharsets.UTF_8);
                pw.println("<?xml version='1.0' encoding='UTF-8'?>");
                pw.println("<!-- Era " + era + "-->");
                pw.println("<ratgen>");
                pw.println("<factions>");
                for (FactionRecord fRec : factionRecs) {
                    if (fRec.isInEra(era)) {
                        fRec.writeToXml(pw, era);
                    }
                }
                pw.println("</factions>");
                pw.println("<units>");
                for (ChassisRecord cr : chassisRecs) {
                    if (cr.getIntroYear() < nextEra && chassisIndex.get(era).containsKey(cr.getKey())) {
                        avFields.clear();
                        for (AvailabilityRating av : chassisIndex.get(era).get(cr.getKey()).values()) {
                            if (shouldExportAv(av, era)) {
                                avFields.add(av.toString());
                            }
                        }

                        if (!avFields.isEmpty()) {
                            String omni = "";
                            if (cr.isOmni() && !cr.getModels().isEmpty()) {
                                omni = cr.getModels().iterator().next().isClan()
                                      ? "' omni='Clan"
                                      : "' omni='IS";
                            }
                            pw.println("\t<chassis name='" + cr.getChassis().replaceAll("'", "&apos;")
                                  + "' unitType='" + UnitType.getTypeName(cr.getUnitType())
                                  + omni + "'>");
                            pw.print("\t\t<availability>");
                            for (Iterator<String> iter = avFields.iterator(); iter.hasNext(); ) {
                                pw.print(iter.next());
                                if (iter.hasNext()) {
                                    pw.print(",");
                                }
                            }
                            pw.println("</availability>");

                            for (ModelRecord mr : cr.getSortedModels()) {
                                if ((cr.getIntroYear() < nextEra)
                                      && modelIndex.get(era).containsKey(mr.getKey())) {
                                    avFields.clear();
                                    for (AvailabilityRating av : modelIndex.get(era).get(mr.getKey()).values()) {
                                        if (shouldExportAv(av, era)) {
                                            avFields.add(av.toString());
                                        }
                                    }

                                    for (String fKey : mr.getExcludedFactions()) {
                                        avFields.add(fKey + ":0");
                                    }

                                    if (!avFields.isEmpty()) {
                                        pw.print("\t\t<model name='" + mr.getModel().replaceAll("'", "&apos;"));
                                        if (mr.getUnitType() == UnitType.BATTLE_ARMOR) {
                                            pw.print("' mechanized='" + mr.canDoMechanizedBA());
                                        }
                                        pw.println("'>");
                                        if (!mr.getRoles().isEmpty()) {
                                            String str = mr.getRoles().stream().map(Object::toString)
                                                  .collect(Collectors.joining(","));
                                            if (!str.isBlank()) {
                                                pw.println("\t\t\t<roles>" + str + "</roles>");
                                            }
                                        }

                                        if (!mr.getDeployedWith().isEmpty() || !mr.getRequiredUnits().isEmpty()) {
                                            pw.print("\t\t\t<deployedWith>");
                                            StringJoiner sj = new StringJoiner(",");
                                            mr.getDeployedWith().forEach(sj::add);
                                            mr.getRequiredUnits().forEach(s -> sj.add("req:" + s));
                                            pw.print(sj);
                                            pw.println("</deployedWith>");
                                        }
                                        pw.print("\t\t\t<availability>");
                                        for (Iterator<String> iter = avFields.iterator(); iter.hasNext(); ) {
                                            pw.print(iter.next());
                                            if (iter.hasNext()) {
                                                pw.print(",");
                                            }
                                        }
                                        pw.println("</availability>");
                                        pw.println("\t\t</model>");
                                    }
                                }
                            }
                            pw.println("\t</chassis>");
                        }
                    }
                }
                pw.println("</units>");
                pw.println("</ratgen>");
                pw.close();
            } catch (Exception ex) {
                LOGGER.error(ex, "exportRARGen");
            }
        }
    }

    private boolean shouldExportAv(AvailabilityRating av, int era) {
        final FactionRecord fRec = factions.get(av.getFaction());
        return (fRec == null) || fRec.isInEra(era);
    }

    /**
     * Returns the logo ImageIcon for a specific faction and game year.
     *
     * <p>This method resolves the appropriate logo file for the given {@code factionCode} and {@code gameYear},
     * accounting for historical changes in faction logos over time where applicable.</p>
     *
     * <p>The resulting image is loaded from the predefined directory as a PNG file and tinted black.
     * For unknown or missing faction codes, a default logo is used.</p>
     *
     * @param gameYear    the year in the game context, potentially affecting logo selection for some factions
     * @param factionCode the code identifying the faction (e.g., "FS" for Federated Suns)
     *
     * @return an {@link ImageIcon} object for the specified faction, tinted black
     */
    public @Nullable ImageIcon getFactionLogo(int gameYear, String factionCode, Color tintColor) {
        final String IMAGE_DIRECTORY = "data/images/universe/factions/";
        final String FILE_TYPE = ".png";

        String key = switch (factionCode) {
            case "ARC", "ARD" -> "logo_aurigan_coalition";
            case "CDP" -> "logo_calderon_protectorate";
            case "CC" -> "logo_capellan_confederation";
            case "CIR" -> "logo_circinus_federation";
            case "CBS" -> "logo_clan_blood_spirit";
            case "CB" -> "logo_clan_burrock";
            case "CCC" -> "logo_clan_cloud_cobra";
            case "CCO" -> "logo_clan_coyote";
            case "CFM" -> "logo_clan_fire_mandrills";
            case "CGB" -> {
                if (gameYear >= 3060) {
                    yield "logo_rasalhague_dominion";
                } else {
                    yield "logo_clan_ghost_bear";
                }
            }
            case "CGS" -> "logo_clan_goliath_scorpion";
            case "CHH" -> "logo_clan_hells_horses";
            case "CIH" -> "logo_clan_ice_hellion";
            case "CJF" -> "logo_clan_jade_falcon";
            case "CMG" -> "logo_clan_mongoose";
            case "CNC" -> "logo_clan_nova_cat";
            case "CDS" -> {
                if (gameYear <= 2984 || gameYear >= 3100) {
                    yield "logo_clan_sea_fox";
                } else {
                    yield "logo_clan_diamond_sharks";
                }
            }
            case "CSJ" -> "logo_clan_smoke_jaguar";
            case "CSR" -> "logo_clan_snow_raven";
            case "CSA" -> "logo_clan_star_adder";
            case "CSV" -> "logo_clan_steel_viper";
            case "CSL" -> "logo_clan_stone_lion";
            case "CW", "CWE", "CWIE" -> "logo_clan_wolf";
            case "CWOV" -> "logo_clan_wolverine";
            case "CS" -> "logo_comstar";
            case "DC" -> "logo_draconis_combine";
            case "DA" -> "logo_duchy_of_andurien";
            case "DTA" -> "logo_duchy_of_tamarind_abbey";
            case "CEI" -> "logo_escorpion_imperio";
            case "FC" -> "logo_federated_commonwealth";
            case "FS" -> "logo_federated_suns";
            case "FOR" -> "logo_fiefdom_of_randis";
            case "FVC" -> "logo_filtvelt_coalition";
            case "FRR" -> "logo_free_rasalhague_republic";
            case "FWL" -> "logo_free_worlds_league";
            case "FR" -> "logo_fronc_reaches";
            case "HL" -> "logo_hanseatic_league";
            case "IP" -> "logo_illyrian_palatinate";
            case "LL" -> "logo_lothian_league";
            case "LA" -> "logo_lyran_alliance";
            case "MOC" -> "logo_magistracy_of_canopus";
            case "MH" -> "logo_marian_hegemony";
            case "MERC" -> "logo_mercenaries";
            case "MV" -> "logo_morgrains_valkyrate";
            case "NC" -> "logo_nueva_castile";
            case "OC" -> "logo_oberon_confederation";
            case "OA" -> "logo_outworld_alliance";
            case "PIR" -> "logo_pirates";
            case "RD" -> "logo_rasalhague_dominion";
            case "RF" -> "logo_regulan_fiefs";
            case "ROS" -> "logo_republic_of_the_sphere";
            case "RWR" -> "logo_rim_worlds_republic";
            case "IND" -> "logo_security_forces";
            case "SIC" -> "logo_st_ives_compact";
            case "SL" -> "logo_star_league";
            case "TC" -> "logo_taurian_concordat";
            case "TD" -> "logo_tortuga_dominions";
            case "UC" -> "logo_umayyad_caliphate";
            case "WOB" -> "logo_word_of_blake";
            case "TH" -> "logo_terran_hegemony";
            case "CI" -> "logo_chainelane_isles";
            case "SOC" -> "logo_the_society";
            case "CWI" -> "logo_clan_widowmaker";
            case "EF" -> "logo_elysian_fields";
            case "GV" -> "logo_greater_valkyrate";
            case "JF" -> "logo_jarnfolk";
            case "MSC" -> "logo_marik_stewart_commonwealth";
            case "OP" -> "logo_oriente_protectorate";
            case "RA" -> "logo_raven_alliance";
            case "RCM" -> "logo_rim_commonality";
            case "NIOPS" -> "logo_niops_association";
            case "AXP" -> "logo_axumite_providence";
            case "NDC" -> "logo_new_delphi_compact";
            case "REB" -> "logo_rebels";
            // Fallbacks
            default -> {
                Faction faction = Faction.fromAbbr(factionCode);
                if (faction.isClan()) {
                    yield "logo_clan_generic";
                } else {
                    yield "logo_mercenaries";
                }
            }
        };

        final String filename = IMAGE_DIRECTORY + key + FILE_TYPE;
        File file = new File(filename);
        if (file.exists()) {
            ImageIcon icon = new ImageIcon(filename);
            icon = addTintToImageIcon(icon.getImage(), tintColor);
            return icon;
        } else {
            return null;
        }
    }
}
