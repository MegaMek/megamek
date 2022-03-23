/*
 * MegaMek - Copyright (C) 2016 The MegaMek Team
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.client.ratgenerator;

import megamek.client.ratgenerator.FactionRecord.TechCategory;
import megamek.client.ratgenerator.UnitTable.TableEntry;
import megamek.common.*;
import megamek.common.annotations.Nullable;
import megamek.common.util.fileUtils.MegaMekFile;
import megamek.utils.MegaMekXmlUtil;
import org.apache.logging.log4j.LogManager;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Generates a random assignment table (RAT) dynamically based on a variety of criteria,
 * including faction, era, unit type, weight class, equipment rating, faction subcommand, vehicle
 * movement mode, and mission role.
 * 
 * @author Neoancient
 */
public class RATGenerator {
    
    private final HashMap<String, ModelRecord> models;
    private final HashMap<String, ChassisRecord> chassis;
    private final HashMap<String, FactionRecord> factions;
    private final HashMap<Integer, HashMap<String, HashMap<String, AvailabilityRating>>> modelIndex;
    private final HashMap<Integer, HashMap<String, HashMap<String, AvailabilityRating>>> chassisIndex;

    private final TreeSet<Integer> eraSet;

    private static RATGenerator rg = null;
    private static boolean interrupted = false;
    private static boolean dispose = false;
    private Thread loader;
    private boolean initialized;
    private boolean initializing;

    private ArrayList<ActionListener> listeners;
    
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
        if (rg == null) {
            rg = new RATGenerator();
        }
        if (!rg.initialized && !rg.initializing) {
            rg.initializing = true;
            interrupted = false;
            dispose = false;
            rg.loader = new Thread(() -> rg.initialize(Configuration.forceGeneratorDir()),
                    "RAT Generator unit populator");
            rg.loader.setPriority(Thread.NORM_PRIORITY - 1);
            rg.loader.start();
        }
        return rg;
    }

    public boolean isInitialized() {
        return initialized;
    }

    /**
     * Clears all data and loads from the given directory
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
        rg.getEraSet().forEach(e -> rg.loadEra(e, dir));
    }

    public AvailabilityRating findChassisAvailabilityRecord(int era, String unit, String faction,
            int year) {
        if (factions.containsKey(faction)) {
            return findChassisAvailabilityRecord(era, unit, factions.get(faction), year);
        }
        if (chassisIndex.containsKey(era) && chassisIndex.get(era).containsKey(unit)) {
            AvailabilityRating av = chassisIndex.get(era).get(unit).get("General");
            if (av != null && year >= av.getStartYear()) {
                return av;
            }
        }
        return null;
    }

    public @Nullable AvailabilityRating findChassisAvailabilityRecord(int era, String unit,
                                                                      FactionRecord fRec, int year) {
        if (fRec == null) {
            return null;
        }
        AvailabilityRating retVal = null;
        if (chassisIndex.containsKey(era) && chassisIndex.get(era).containsKey(unit)) {
            if (chassisIndex.get(era).get(unit).containsKey(fRec.getKey())) {
                retVal = chassisIndex.get(era).get(unit).get(fRec.getKey());
            } else if (fRec.getParentFactions().size() == 1) {
                retVal = findChassisAvailabilityRecord(era, unit, fRec.getParentFactions().get(0), year);
            } else if (!fRec.getParentFactions().isEmpty()) {
                ArrayList<AvailabilityRating> list = new ArrayList<>();
                for (String alt : fRec.getParentFactions()) {
                    AvailabilityRating ar = findChassisAvailabilityRecord(era, unit, alt, year);
                    if (ar != null) {
                        list.add(ar);
                    }
                }
                retVal = mergeFactionAvailability(fRec.getKey(), list);
            } else {
                retVal = chassisIndex.get(era).get(unit).get("General");
            }
        }

        if (retVal != null && year >= retVal.getStartYear()) {
            return retVal;
        }

        return null;
    }

    public @Nullable AvailabilityRating findModelAvailabilityRecord(int era, String unit,
                                                                    String faction) {
        if (factions.containsKey(faction)) {
            return findModelAvailabilityRecord(era, unit, factions.get(faction));
        } else if (modelIndex.containsKey(era) && modelIndex.get(era).containsKey(unit)) {
            return modelIndex.get(era).get(unit).get("General");
        } else {
            return null;
        }
    }

    public @Nullable AvailabilityRating findModelAvailabilityRecord(int era, String unit,
                                                                    @Nullable FactionRecord fRec) {
        if (null == models.get(unit)) {
            LogManager.getLogger().error("Trying to find record for unknown model " + unit);
            return null;
        } else if ((fRec == null) || models.get(unit).factionIsExcluded(fRec)) {
            return null;
        }
        if (modelIndex.containsKey(era) && modelIndex.get(era).containsKey(unit)) {
            if (modelIndex.get(era).get(unit).containsKey(fRec.getKey())) {
                return modelIndex.get(era).get(unit).get(fRec.getKey());
            }

            if (fRec.getParentFactions().size() == 1) {
                return findModelAvailabilityRecord(era, unit, fRec.getParentFactions().get(0));
            } else if (!fRec.getParentFactions().isEmpty()) {
                ArrayList<AvailabilityRating> list = new ArrayList<>();
                for (String alt : fRec.getParentFactions()) {
                    AvailabilityRating ar = findModelAvailabilityRecord(era, unit, alt);
                    if (ar != null) {
                        list.add(ar);
                    }
                }
                return mergeFactionAvailability(fRec.getKey(), list);
            }
            return modelIndex.get(era).get(unit).get("General");
        }

        return null;
    }

    /**
     * Provides a list of availability ratings for a unit in a given era. Used in editing and reporting.
     * 
     * @param era  The year of the record. This must be one of the years in the <code>eraSet</code>.
     * @param unit The lookup name of the unit to find records for.
     * @return     A <code>Collection</code> of all the availability ratings for the unit in the era,
     *             or null if there are no records for that era.
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
     * @param era  The year of the record to change
     * @param unitKey The model key for the unit which is having its model record updated
     * @param ar   The new <code>AvailabilityRating</code> for the unit in the era. This provides the
     *             faction.
     */
    public void setModelFactionRating(int era, String unitKey, AvailabilityRating ar) {
        modelIndex.get(era).computeIfAbsent(unitKey, k -> new HashMap<>());
        modelIndex.get(era).get(unitKey).put(ar.getFactionCode(), ar);
        models.get(unitKey).getIncludedFactions().add(ar.getFactionCode());
    }

    /**
     * Removes the availability rating entry.
     * 
     * @param era      The year of the record to remove.
     * @param unit     The model to remove the record for.
     * @param faction  The faction to remove the record for.
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
     * @param era  The year of the record. This must be one of the years in the <code>eraSet</code>.
     * @param chassisKey The chassis name to find records for.
     * @return     A <code>Collection</code> of all the availability ratings for the chassis in the era,
     *             or null if there are no records for that era.
     */
    public @Nullable Collection<AvailabilityRating> getChassisFactionRatings(int era,
                                                                             String chassisKey) {
        if (chassisIndex.containsKey(era) && chassisIndex.get(era).containsKey(chassisKey)) {
            return chassisIndex.get(era).get(chassisKey).values();
        }
        return null;
    }

    /**
     * Adds or changes an availability rating entry for a chassis.
     * 
     * @param era  The year of the record to change
     * @param unit The name of the chassis for which to change the record
     * @param ar   The new <code>AvailabilityRating</code> for the unit in the era. This provides the
     *             faction.
     */
    public void setChassisFactionRating(int era, String unit, AvailabilityRating ar) {
        chassisIndex.get(era).computeIfAbsent(unit, k -> new HashMap<>());
        chassisIndex.get(era).get(unit).put(ar.getFactionCode(), ar);
        chassis.get(unit).getIncludedFactions().add(ar.getFactionCode());
    }

    /**
     * Removes the availability rating entry.
     * 
     * @param era      The year of the record to remove.
     * @param unit     The chassis to remove the record for.
     * @param faction  The faction to remove the record for.
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
     * Used for a faction with multiple parent factions (e.g. FC == FS + LA) to find the average
     * availability among the parents. Based on average weight rather than av rating.
     * 
     * @param faction The faction code to use for the new AvailabilityRecord
     * @param list A list of ARs for the various parent factions
     * @return A new AR with the average availability code from the various factions.
     */
    private AvailabilityRating mergeFactionAvailability(String faction, List<AvailabilityRating> list) {
        if (list.isEmpty()) {
            return null;
        }
        double totalWt = 0;
        int totalAdj = 0;
        for (AvailabilityRating ar : list) {
            totalWt += AvailabilityRating.calcWeight(ar.availability);
            totalAdj += ar.ratingAdjustment;
        }
        AvailabilityRating retVal = list.get(0).makeCopy(faction);
        
        retVal.availability = (int) (AvailabilityRating.calcAvRating(totalWt / list.size()));
        if (totalAdj < 0) {
            retVal.ratingAdjustment = (totalAdj - 1)/ list.size();
        } else {
            retVal.ratingAdjustment = (totalAdj + 1)/ list.size();
        }
        return retVal;
    }
    
    /**
     * Given values for two years, interpolates or extrapolates value for another given year.
     * If one of the two values is null, it is treated as 0.
     * 
     * @param av1 The first value.
     * @param av2 The second value.
     * @param year1 The year for the first value.
     * @param year2 The year for the second value.
     * @param now The year for which to calculate a value.
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
    
    public List<UnitTable.TableEntry> generateTable(FactionRecord fRec, int unitType, int year,
            String rating, Collection<Integer> weightClasses, int networkMask,
            Collection<EntityMovementMode> movementModes,
            Collection<MissionRole> roles, int roleStrictness,
            FactionRecord user) {
        HashMap<ModelRecord, Double> unitWeights = new HashMap<>();
        HashMap<FactionRecord, Double> salvageWeights = new HashMap<>();
        
        loadYear(year);
        
        if (fRec == null) {
            fRec = new FactionRecord();
        }
        
        Integer early = eraSet.floor(year);
        if (early == null) {
            early = eraSet.first();
        }
        Integer late = null;
        if (!eraSet.contains(year)) {
            late = eraSet.ceiling(year);
        }
        if (late == null) {
            late = early;
        }
        
        /* Adjustments for unit rating require knowing both how many ratings are available
         * to the faction and where the rating falls within the whole. If a faction does
         * not have designated rating levels, it inherits those of the parent faction;
         * if there are multiple parent factions the first match is used. Some very minor
         * or generic factions do not use rating adjustments, indicated by a rating level
         * of -1. A faction that has one rating level is a special case that always has
         * the indicated rating within the parent faction's system.
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
        
        for (String chassisKey : chassisIndex.get(early).keySet()) {
            ChassisRecord cRec = chassis.get(chassisKey);
            if (cRec == null) {
                LogManager.getLogger().error("Could not locate chassis " + chassisKey);
                continue;
            }
            
            if (cRec.getUnitType() != unitType &&
                    !(unitType == UnitType.TANK
                        && cRec.getUnitType() == UnitType.VTOL
                        && movementModes.contains(EntityMovementMode.VTOL))) {
                continue;
            }

            AvailabilityRating ar = findChassisAvailabilityRecord(early,
                        cRec.getChassisKey(), fRec, year);
            if (ar == null) {
                continue;
            }
            double cAv = cRec.calcAvailability(ar, ratingLevel, numRatingLevels, early);
            cAv = interpolate(cAv,
                    cRec.calcAvailability(ar, ratingLevel, numRatingLevels, late),
                    Math.max(early, cRec.getIntroYear()), late, year);
            if (cAv > 0) {
                double totalModelWeight = cRec.totalModelWeight(early,
                        cRec.isOmni() ? user : fRec);
                for (ModelRecord mRec : cRec.getModels()) {
                    if (mRec.getIntroYear() >= year
                            || (!weightClasses.isEmpty()
                                    && !weightClasses.contains(mRec.getWeightClass()))
                            || (networkMask & mRec.getNetworkMask()) != networkMask) {
                        continue;
                    }

                    if (!movementModes.isEmpty() && !movementModes.contains(mRec.getMovementMode())) {
                        continue;
                    }
                    ar = findModelAvailabilityRecord(early, mRec.getKey(), fRec);
                    if ((ar == null) || (ar.getAvailability() == 0)) {
                        continue;
                    }
                    double mAv = mRec.calcAvailability(ar, ratingLevel, numRatingLevels, early);
                    mAv = interpolate(mAv,
                            mRec.calcAvailability(ar, ratingLevel, numRatingLevels, late),
                            Math.max(early, mRec.getIntroYear()), late, year);
                    Double adjMAv = MissionRole.adjustAvailabilityByRole(mAv, roles, mRec, year, roleStrictness);
                    if (adjMAv != null) {
                        double mWt = AvailabilityRating.calcWeight(adjMAv) / totalModelWeight
                                * AvailabilityRating.calcWeight(cAv);
                        if (mWt > 0) {
                            unitWeights.put(mRec, mWt);
                        }
                    }
                }
            }                        
        }

        if (unitWeights.isEmpty()) {
            return new ArrayList<>();
        }

        // If there is more than one weight class and the faction record (or parent) indicates a
        // certain distribution of weight classes, adjust the weight value to conform to the given
        // ratio.
        if (weightClasses.size() > 1) {
            // Get standard weight class distribution for faction
            ArrayList<Integer> wcd = fRec.getWeightDistribution(early, unitType);

            if ((wcd != null) && !wcd.isEmpty()) {
                // Ultra-light and superheavy are too rare to warrant their own values and for
                // weight class distribution purposes are grouped with light and assault,
                // respectively.
                final int[] wcdIndex = {0, 0, 1, 2, 3, 3};
                // Find the totals of the weight for the generated table
                double totalMRWeight = unitWeights.values().stream().mapToDouble(Double::doubleValue).sum();
                // Find the sum of the weight distribution values for all weight classes in use.
                int totalWCDWeights = weightClasses.stream().filter(wc -> wcdIndex[wc] < wcd.size())
                        .mapToInt(wc -> wcd.get(wcdIndex[wc])).sum();

                if (totalWCDWeights > 0) {
                    // Group all the models of the generated table by weight class.
                    Function<ModelRecord,Integer> grouper = mr -> wcdIndex[mr.getWeightClass()];
                    Map<Integer,List<ModelRecord>> weightGroups = unitWeights.keySet().stream()
                            .collect(Collectors.groupingBy(grouper));
                    
                    // Go through the weight class groups and adjust the table weights so the total
                    // of each group corresponds to the distribution for this faction.
                    for (int i : weightGroups.keySet()) {
                        double totalWeight = weightGroups.get(i).stream().mapToDouble(unitWeights::get).sum();
                        if (totalWeight > 0) {
                            double adj = totalMRWeight * wcd.get(i) / (totalWeight * totalWCDWeights);
                            weightGroups.get(i).forEach(mr -> unitWeights.merge(mr, adj, (x, y) -> x * y));
                        }
                    }
                }
            }
        }
        
        double total = unitWeights.values().stream().mapToDouble(Double::doubleValue).sum();

        if (fRec.getPctSalvage(early) != null) {
            HashMap<String,Double> salvageEntries = new HashMap<>();
            for (Entry<String,Integer> entry : fRec.getSalvage(early).entrySet()) {
                salvageEntries.put(entry.getKey(),
                        interpolate(entry.getValue(),
                                fRec.getSalvage(late).get(entry.getKey()),
                                        early, late, year));
            }
            if (!late.equals(early)) {
                for (Entry<String,Integer> entry : fRec.getSalvage(late).entrySet()) {
                    if (!salvageEntries.containsKey(entry.getKey())) {
                        salvageEntries.put(entry.getKey(), interpolate(0.0,
                                entry.getValue(), early, late, year));
                    }
                }
            }            
            
            double salvage = fRec.getPctSalvage(early);
            if (salvage >= 100) {
                salvage = total;
                unitWeights.clear();
            } else {
                salvage = salvage * total / (100 - salvage);
            }
            double totalFactionWeight = salvageEntries.values().stream()
                    .mapToDouble(Double::doubleValue).sum();
            for (String fKey : salvageEntries.keySet()) {
                FactionRecord salvageFaction = factions.get(fKey);
                if (salvageFaction == null) {
                    LogManager.getLogger().debug("Could not locate faction " + fKey 
                            + " for " + fRec.getKey() + " salvage");
                } else {
                    double wt = salvage * salvageEntries.get(fKey) / totalFactionWeight;
                    salvageWeights.put(salvageFaction, wt);
                }
            }
        }
        
        if (ratingLevel >= 0) {
            adjustForRating(fRec, unitType, year, ratingLevel,
                    unitWeights, salvageWeights, early, late);
        }
        
        // Increase weights if necessary to keep smallest from rounding down to zero
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
                retVal.add(new TableEntry(wt, mRec.getMechSummary()));
            }
        }
        return retVal;
    }

    private void adjustForRating(FactionRecord fRec, int unitType, int year, int rating,
                                 Map<ModelRecord, Double> unitWeights,
                                 Map<FactionRecord, Double> salvageWeights, Integer early,
                                 Integer late) {
        double total = 0.0;
        double totalOmni = 0.0;
        double totalClan = 0.0;
        double totalSL = 0.0;
        for (Entry<ModelRecord, Double> entry : unitWeights.entrySet()) {
            total += entry.getValue();
            if (entry.getKey().isOmni()) {
                totalOmni += entry.getValue();
            }
            if (entry.getKey().isClan()) {
                totalClan += entry.getValue();
            } else if (entry.getKey().isSL()) {
                totalSL += entry.getValue();
            }
        }
        Double pctOmni = null;
        Double pctNonOmni = null;
        Double pctSL = null;
        Double pctClan = null;
        Double pctOther = null;
        if (unitType == UnitType.MEK) {
            pctOmni = interpolate(fRec.findPctTech(TechCategory.OMNI, early, rating),
                    fRec.findPctTech(TechCategory.OMNI, late, rating), early, late, year);
            pctClan = interpolate(fRec.findPctTech(TechCategory.CLAN, early, rating),
                    fRec.findPctTech(TechCategory.CLAN, late, rating), early, late, year);
            pctSL = interpolate(fRec.findPctTech(TechCategory.IS_ADVANCED, early, rating),
                    fRec.findPctTech(TechCategory.IS_ADVANCED, late, rating), early, late, year);
        }
        if (unitType == UnitType.AERO) {
            pctOmni = interpolate(fRec.findPctTech(TechCategory.OMNI_AERO, early, rating),
                    fRec.findPctTech(TechCategory.OMNI_AERO, late, rating), early, late, year);
            pctClan = interpolate(fRec.findPctTech(TechCategory.CLAN_AERO, early, rating),
                    fRec.findPctTech(TechCategory.CLAN_AERO, late, rating), early, late, year);
            pctSL = interpolate(fRec.findPctTech(TechCategory.IS_ADVANCED_AERO, early, rating),
                    fRec.findPctTech(TechCategory.IS_ADVANCED_AERO, late, rating), early, late, year);
        }
        if (unitType == UnitType.TANK || unitType == UnitType.VTOL) {
            pctClan = interpolate(fRec.findPctTech(TechCategory.CLAN_VEE, early, rating),
                    fRec.findPctTech(TechCategory.CLAN_VEE, late, rating), early, late, year);
            pctSL = interpolate(fRec.findPctTech(TechCategory.IS_ADVANCED_VEE, early, rating),
                    fRec.findPctTech(TechCategory.IS_ADVANCED_VEE, late, rating), early, late, year);
        }

        /* Adjust for lack of precision in post-FM:Updates extrapolations */
        if (pctSL != null || pctClan != null) {
            pctOther = 100.0;
            if (pctSL != null) {
                pctOther -= pctSL;
            }

            if (pctClan != null) {
                pctOther -= pctClan;
            }
            Double techMargin = interpolate(fRec.getTechMargin(early), fRec.getTechMargin(late),
                    early, late, year);
            if (techMargin != null && techMargin > 0) {
                if (pctClan != null) {
                    double pct = 100.0 * totalClan / total;
                    if (pct < pctClan - techMargin) {
                        pctClan -= techMargin;
                    } else if (pct > pctClan + techMargin) {
                        pctClan += techMargin;
                    }
                }

                if (pctSL != null) {
                    double pct = 100.0 * totalSL / total;
                    if (pct < pctSL - techMargin) {
                        pctSL -= techMargin;
                    } else if (pct > pctSL + techMargin) {
                        pctSL += techMargin;
                    }
                }                    
            }

            Double upgradeMargin = interpolate(fRec.getUpgradeMargin(early),
                    fRec.getUpgradeMargin(late), early, late, year);
            if ((upgradeMargin != null) && (upgradeMargin > 0)) {
                double pct = 100.0 * (total - totalClan - totalSL) / total;
                if (pct < pctOther - upgradeMargin) {
                    pctOther -= upgradeMargin;
                } else if (pct > pctOther + upgradeMargin) {
                    pctOther += upgradeMargin;
                }
                /* If clan, sl, and other are all adjusted, the values probably
                 * don't add up to 100, which is fine unless the upgradeMargin is
                 * <= techMargin. Then pctOther is more certain, and we adjust 
                 * the values of clan and sl to keep the value of "other" equal to
                 * a percentage. 
                 */
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
        if (pctOmni != null) {
            Double omniMargin = interpolate(fRec.getOmniMargin(early), fRec.getOmniMargin(late),
                    early, late, year);
            if ((omniMargin != null) && (omniMargin > 0)) {
                double pct = 100.0 * totalOmni / total;
                if (pct < pctOmni - omniMargin) {
                    pctOmni -= omniMargin;
                } else if (pct > pctOmni + omniMargin) {
                    pctOmni += omniMargin;
                }
            }
            pctNonOmni = 100.0 - pctOmni;
        }            
                
        // For non-Clan factions, the amount of salvage from Clan factions is part of the overall
        // Clan percentage.
        if (!fRec.isClan() && (pctClan != null) && (totalClan > 0)) {
            double clanSalvage = salvageWeights.keySet().stream().filter(FactionRecord::isClan)
                    .mapToDouble(salvageWeights::get).sum();
            total += clanSalvage;
            totalClan += clanSalvage;
            for (FactionRecord fr : salvageWeights.keySet()) {
                if (fr.isClan()) {
                    salvageWeights.put(fr, salvageWeights.get(fr)
                            * (pctClan / 100.0) * (total / totalClan));
                }
            }
        }
        double totalOther = total - totalClan - totalSL;
        for (ModelRecord mRec : unitWeights.keySet()) {
            if (pctOmni != null && mRec.isOmni() && totalOmni < total) {
                unitWeights.put(mRec, unitWeights.get(mRec) * (pctOmni / 100.0) * (total / totalOmni));
            }
            if (pctNonOmni != null && !mRec.isOmni() && totalOmni > 0) {
                unitWeights.put(mRec, unitWeights.get(mRec) * (pctNonOmni / 100.0) * (total / (total - totalOmni)));                        
            }
            if (pctSL != null && mRec.isSL()
                    && totalSL > 0) {
                unitWeights.put(mRec, unitWeights.get(mRec) * (pctSL / 100.0) * (total / totalSL));
            }
            if (pctClan != null && mRec.isClan()
                    && totalClan > 0) {
                unitWeights.put(mRec, unitWeights.get(mRec) * (pctClan / 100.0) * (total / totalClan));
            }
            if (pctOther != null && pctOther > 0 && !mRec.isClan() && !mRec.isSL()) {
                unitWeights.put(mRec, unitWeights.get(mRec) * (pctOther / 100.0)
                        * (total / totalOther));
            }
        }
        double multiplier = total / unitWeights.values().stream().mapToDouble(Double::doubleValue).sum();
        for (ModelRecord mRec : unitWeights.keySet()) {
            unitWeights.merge(mRec, multiplier, (a, b) -> a * b);
        }
    }

    public void dispose() {
        interrupted = true;
        dispose = true;
        if (initialized) {
            rg = null;
        }
    }

    private synchronized void initialize(File dir) {
        // Give the MSC some time to initialize
        MechSummaryCache msc = MechSummaryCache.getInstance();
        long waitLimit = System.currentTimeMillis() + 3000; /* 3 seconds */
        while (!interrupted && !msc.isInitialized() && waitLimit > System.currentTimeMillis()) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                // Ignore
            }
        }

        if (!(dir.exists() && dir.isDirectory())) {
            LogManager.getLogger().error(dir + " is not a directory");
        } else {
            loadFactions(dir);

            for (File f : dir.listFiles()) {
                if (f.getName().matches("\\d+\\.xml")) {
                    eraSet.add(Integer.parseInt(f.getName().replace(".xml", "")));
                }
            }
        }

        if (!interrupted) {
            rg.initialized = true;
            rg.notifyListenersOfInitialization();
        }

        if (dispose) {
            rg = null;
            dispose = false;
        }
    }

    /**
     * If the year is equal to one of the era marks, it loads that era. If it is between two, it
     * loads eras on both sides. Otherwise, just load the closest era.
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
            LogManager.getLogger().error("Unable to read RAT generator factions file");
            return;
        }

        Document xmlDoc;

        try {
            DocumentBuilder db = MegaMekXmlUtil.newSafeDocumentBuilder();
            xmlDoc = db.parse(fis);
        } catch (Exception ex) {
            LogManager.getLogger().error("", ex);
            return;
        }

        Element element = xmlDoc.getDocumentElement();
        NodeList nl = element.getChildNodes();

        element.normalize();

        for (int x = 0; x < nl.getLength(); x++) {
            Node wn = nl.item(x);
            if (wn.getNodeName().equalsIgnoreCase("faction")) {
                if (wn.getAttributes().getNamedItem("key") != null) {
                    FactionRecord rec = FactionRecord.createFromXml(wn);
                    factions.put(rec.getKey(), rec);
                } else {
                    LogManager.getLogger().warn("Faction key not found in " + file.getPath());
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
        FileInputStream fis;
        try {
            fis = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            LogManager.getLogger().error("Unable to read RAT generator file for era " + era);
            return;
        }
        while (!MechSummaryCache.getInstance().isInitialized()) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException ignored) {

            }
        }

        Document xmlDoc;

        try {
            DocumentBuilder db = MegaMekXmlUtil.newSafeDocumentBuilder();
            xmlDoc = db.parse(fis);
        } catch (Exception ex) {
            LogManager.getLogger().error("", ex);
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
                                LogManager.getLogger().error("Faction " + fKey + " not found in " + file.getPath());
                            }
                        } else {
                            LogManager.getLogger().error("Faction key not found in " + file.getPath());
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
     * Creates model and chassis records for all units that don't already have entries. This should
     * only be called after all availability records are loaded, otherwise they will be overwritten.
     * 
     * Used for editing.
     */
    public void initRemainingUnits() {
        for (MechSummary ms : MechSummaryCache.getInstance().getAllMechs()) {
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
                for (String code : codes) {
                    AvailabilityRating ar = new AvailabilityRating(chassisKey, era, code);
                    cr.getIncludedFactions().add(code.split(":")[0]);
                    chassisIndex.get(era).get(chassisKey).put(ar.getFactionCode(), ar);
                }
            } else if (wn2.getNodeName().equalsIgnoreCase("model")) {
                parseModelNode(era, cr, wn2);
            }
        }
    }
    
    private void parseModelNode(int era, ChassisRecord cr, Node wn) {
        String modelKey = (cr.getChassis() + ' ' + wn.getAttributes().getNamedItem("name").getTextContent()).trim();
        boolean newEntry = false;
        ModelRecord mr = models.get(modelKey);
        if (mr == null) {
            newEntry = true;
            MechSummary ms = MechSummaryCache.getInstance().getMech(modelKey);
            if (ms != null) {
                mr = new ModelRecord(ms);
                mr.setOmni(cr.isOmni());
                models.put(modelKey, mr);
            }

            if (mr == null) {
                LogManager.getLogger().error(cr.getChassis() + ' '
                        + wn.getAttributes().getNamedItem("name").getTextContent() + " not found.");
                return;
            }
        }
        cr.addModel(mr);
        if (wn.getAttributes().getNamedItem("mechanized") != null) {
            mr.setMechanizedBA(Boolean.parseBoolean(wn.getAttributes().getNamedItem("mechanized").getTextContent()));
        }
        
        for (int k = 0; k < wn.getChildNodes().getLength(); k++) {
            Node wn2 = wn.getChildNodes().item(k);
            if (wn2.getNodeName().equalsIgnoreCase("roles") && newEntry) {
                mr.addRoles(wn2.getTextContent().trim());
            } else if (wn2.getNodeName().equalsIgnoreCase("deployedWith") && newEntry) {
                mr.setRequiredUnits(wn2.getTextContent().trim());                                        
            } else if (wn2.getNodeName().equalsIgnoreCase("availability")) {
                modelIndex.get(era).put(mr.getKey(), new HashMap<>());
                String[] codes = wn2.getTextContent().trim().split(",");
                for (String code : codes) {
                    AvailabilityRating ar = new AvailabilityRating(mr.getKey(), era, code);
                    mr.getIncludedFactions().add(code.split(":")[0]);
                    modelIndex.get(era).get(mr.getKey()).put(ar.getFactionCode(), ar);
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
                l.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED,"ratGenInitialized"));
            }
        }
    }

    /**
     * Notifies all the listeners that era is loaded
     */
    public void notifyListenersEraLoaded() {
        if (initialized) {
            for (ActionListener l : listeners) {
                l.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED,"ratGenEraLoaded"));
            }
        }
    }
    
    public void exportRATGen(File dir) {
        PrintWriter pw;
        
        FactionRecord[] factionRecs = factions.values().toArray(new FactionRecord[0]);
        Arrays.sort(factionRecs, (arg0, arg1) -> {
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
            pw = new PrintWriter(file, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            LogManager.getLogger().error("", ex);
            return;
        }
        pw.println("<?xml version='1.0' encoding='UTF-8'?>");
        pw.println("<factions>");
        for (FactionRecord fRec : factionRecs) {
            fRec.writeToXml(pw);
        }
        pw.println("</factions>");
        pw.close();

        ChassisRecord[] chassisRecs = chassis.values().toArray(new ChassisRecord[0]);
        Arrays.sort(chassisRecs, Comparator.comparing(AbstractUnitRecord::getKey));
        ArrayList<String> avFields = new ArrayList<>();
        
        final List<Integer> ERAS = new ArrayList<>(eraSet);

        for (int i = 0; i < ERAS.size(); i++) {
            int era = ERAS.get(i);
            int nextEra = (i < ERAS.size() - 1) ? ERAS.get(i + 1) : Integer.MAX_VALUE;
            try {
                file = new File(dir + "/" + era + ".xml");
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
                                        ? "' omni='Clan" : "' omni='IS";
                            }
                            pw.println("\t<chassis name='" + cr.getChassis().replaceAll("'", "&apos;")
                                    + "' unitType='" + UnitType.getTypeName(cr.getUnitType())
                                    + omni + "'>");
                            pw.print("\t\t<availability>");
                            for (Iterator<String> iter = avFields.iterator(); iter.hasNext();) {
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
                                            String str = mr.getRoles().stream().map(Object::toString).collect(Collectors.joining(","));
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
                                        for (Iterator<String> iter = avFields.iterator(); iter.hasNext();) {
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
                LogManager.getLogger().error("", ex);
            }
        }
    }

    private boolean shouldExportAv(AvailabilityRating av, int era) {
        final FactionRecord fRec = factions.get(av.getFaction());
        return (fRec == null) || fRec.isInEra(era);
    }
}
