/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import megamek.common.annotations.Nullable;
import megamek.common.loaders.MekSummary;
import megamek.common.loaders.MekSummaryCache;
import megamek.common.units.ForceGeneratorAvailability;
import megamek.logging.MMLogger;

/**
 * Feeds the Force Generator with units that declare their own availability inside their unit file (.mtf or .blk),
 * rather than in the era files under data/forcegenerator. This is what lets a player's custom unit turn up in a
 * generated force without editing canon data that an update would overwrite.
 * <p>
 * Canon data always wins:
 * </p>
 * <ul>
 *     <li>A canon unit is skipped outright. Editing a canon unit file can never change how canon forces
 *     generate.</li>
 *     <li>A faction the era files already rate for a chassis keeps that rating. A custom variant can never make a canon
 *     design more or less common.</li>
 *     <li>A faction the era files do not rate takes the value from the unit file. This is what lets a custom variant
 *     reach a faction that does not otherwise field the chassis.</li>
 * </ul>
 * <p>
 * Where several custom variants of one chassis rate the same unrated faction, the highest value wins. Extra variants
 * therefore split the chassis's share rather than growing it, which is how canon data behaves.
 * </p>
 *
 * @see ForceGeneratorAvailability
 */
class UnitFileAvailabilityLoader {
    private static final MMLogger LOGGER = MMLogger.create(UnitFileAvailabilityLoader.class);

    /** Separates the chassis key from the faction code in {@link #chassisRatingKey(String, String)}. */
    private static final char KEY_SEPARATOR = '|';

    private final Map<String, ModelRecord> models;
    private final Map<String, ChassisRecord> chassis;
    private final Map<String, FactionRecord> factions;
    private final Map<Integer, HashMap<String, HashMap<String, AvailabilityRating>>> chassisIndex;
    private final Map<Integer, HashMap<String, HashMap<String, AvailabilityRating>>> modelIndex;
    private final TreeSet<Integer> eraSet;

    /**
     * Builds a loader over the Force Generator's live record store. The collections are shared with
     * {@link RATGenerator}, not copied, so the loader writes straight into the indexes it is given.
     *
     * @param models       every model record, keyed by unit name
     * @param chassis      every chassis record, keyed by chassis key
     * @param factions     every faction record, keyed by faction code
     * @param chassisIndex chassis availability, keyed by era, then chassis key, then faction code
     * @param modelIndex   model availability, keyed by era, then model key, then faction code
     * @param eraSet       every era the Force Generator has data for
     */
    UnitFileAvailabilityLoader(Map<String, ModelRecord> models,
          Map<String, ChassisRecord> chassis,
          Map<String, FactionRecord> factions,
          Map<Integer, HashMap<String, HashMap<String, AvailabilityRating>>> chassisIndex,
          Map<Integer, HashMap<String, HashMap<String, AvailabilityRating>>> modelIndex,
          TreeSet<Integer> eraSet) {

        this.models = models;
        this.chassis = chassis;
        this.factions = factions;
        this.chassisIndex = chassisIndex;
        this.modelIndex = modelIndex;
        this.eraSet = eraSet;
    }

    /**
     * Adds records for every unit that declares availability in its own unit file and is available in this era. Call
     * this after the era's XML has been parsed, so that canon data is already in place to be protected.
     *
     * @param era the era being loaded
     */
    void loadEra(int era) {
        // Snapshot what the era XML defined, so custom units added below are not mistaken for canon data
        Set<String> canonChassisRatings = snapshotChassisRatings(era);
        Set<String> canonModelKeys = new HashSet<>(modelIndex.get(era).keySet());
        int eraEndYear = eraEndYear(era);

        for (MekSummary mekSummary : MekSummaryCache.getInstance().getAllMeks()) {
            List<ForceGeneratorAvailability> declaredAvailability = mekSummary.getForceGeneratorAvailability();
            if (declaredAvailability.isEmpty()) {
                continue;
            }

            // A canon unit's availability belongs to the era files. Editing one must never shift canon forces,
            // no matter which era is being loaded or whether the era files happen to cover it
            if (mekSummary.isCanon()) {
                LOGGER.warn("[UnitAvailability] Canon unit {} declares availability in its unit file. Canon availability comes from "
                            + "data/forcegenerator, so the unit file entry is ignored. Save it under a new model "
                            + "name to make it a custom variant.", mekSummary.getName());
                continue;
            }

            if (canonModelKeys.contains(mekSummary.getName())) {
                LOGGER.warn("[UnitAvailability] Unit {} declares availability in its unit file, but the era {} data already covers it. "
                            + "The unit file availability is ignored.", mekSummary.getName(), era);
                continue;
            }

            Map<String, UnitFileRating> ratingsForEra = collectRatingsForEra(mekSummary,
                  declaredAvailability,
                  era,
                  eraEndYear);
            if (ratingsForEra.isEmpty()) {
                continue;
            }

            ModelRecord modelRecord = addModel(mekSummary);
            ChassisRecord chassisRecord = addChassis(modelRecord);

            addModelAvailability(era, modelRecord, ratingsForEra);
            addChassisAvailability(era, chassisRecord, ratingsForEra, canonChassisRatings);
        }
    }

    /**
     * Returns the last year covered by an era bucket, which runs up to the year before the next era starts. The final
     * era has no end.
     *
     * @param era the era to measure
     *
     * @return the last year in the era
     */
    private int eraEndYear(int era) {
        Integer nextEra = eraSet.higher(era);
        return (nextEra == null) ? Integer.MAX_VALUE : nextEra - 1;
    }

    /**
     * Records which chassis/faction pairs the era XML rated, before any unit file adds to them. Anything in this set is
     * canon and must not be overwritten.
     *
     * @param era the era being loaded
     *
     * @return the canon chassis/faction pairs, as produced by {@link #chassisRatingKey(String, String)}
     */
    private Set<String> snapshotChassisRatings(int era) {
        Set<String> canonChassisRatings = new HashSet<>();

        for (Map.Entry<String, HashMap<String, AvailabilityRating>> chassisEntry : chassisIndex.get(era).entrySet()) {
            for (String factionCode : chassisEntry.getValue().keySet()) {
                canonChassisRatings.add(chassisRatingKey(chassisEntry.getKey(), factionCode));
            }
        }

        return canonChassisRatings;
    }

    /**
     * Builds the lookup key identifying one faction's rating for one chassis.
     *
     * @param chassisKey  the chassis key
     * @param factionCode the faction code
     *
     * @return the combined key
     */
    private static String chassisRatingKey(String chassisKey, String factionCode) {
        return chassisKey + KEY_SEPARATOR + factionCode;
    }

    /**
     * Builds the availability codes a unit declares for one era, keyed by faction. Entries whose year range misses the
     * era are ignored, as are codes naming a faction that does not exist. When two entries cover the same era and
     * faction, the last one in the file wins.
     *
     * @param mekSummary           the unit
     * @param declaredAvailability the availability entries from the unit file
     * @param era                  the era being loaded
     * @param eraEndYear           the last year of the era
     *
     * @return the ratings for this era, keyed by faction code; empty if the unit is not available in this era
     */
    private Map<String, UnitFileRating> collectRatingsForEra(MekSummary mekSummary,
          List<ForceGeneratorAvailability> declaredAvailability,
          int era,
          int eraEndYear) {

        Map<String, UnitFileRating> ratingsForEra = new HashMap<>();
        int introYear = mekSummary.getYear();

        for (ForceGeneratorAvailability availability : declaredAvailability) {
            if (!availability.appliesToEra(era, eraEndYear, introYear)) {
                continue;
            }

            int startYear = availability.effectiveStartYear(introYear);

            for (String code : availability.availabilityCodes().split(",")) {
                String trimmedCode = code.trim();
                if (trimmedCode.isEmpty()) {
                    continue;
                }

                AvailabilityRating availabilityRating = new AvailabilityRating(mekSummary.getName(), era, trimmedCode);
                if ((factions.get(availabilityRating.getFaction()) == null)
                      && !trimmedCode.startsWith(FactionRecord.GENERAL_KEY)) {
                    LOGGER.warn("[UnitAvailability] Unit {} declares availability for unknown faction '{}'. Check the faction code "
                                + "against data/universe/factions. The entry is ignored.",
                          mekSummary.getName(),
                          availabilityRating.getFaction());
                    continue;
                }

                // A unit that arrives partway through an era is unavailable until then
                int resolvedStartYear = Math.max(startYear, availabilityRating.getStartYear());

                ratingsForEra.put(availabilityRating.getFactionCode(),
                      new UnitFileRating(trimmedCode, resolvedStartYear, availabilityRating.getAvailability()));
            }
        }

        return ratingsForEra;
    }

    /**
     * Looks up the model record for a unit that declares its own availability, creating it if the Force Generator has
     * not seen it before.
     *
     * @param mekSummary the unit
     *
     * @return the model record
     */
    private ModelRecord addModel(MekSummary mekSummary) {
        ModelRecord modelRecord = models.get(mekSummary.getName());
        if (modelRecord != null) {
            return modelRecord;
        }

        modelRecord = new ModelRecord(mekSummary);
        if (!mekSummary.getMissionRoles().isBlank()) {
            modelRecord.addRoles(mekSummary.getMissionRoles());
        }
        models.put(modelRecord.getKey(), modelRecord);

        return modelRecord;
    }

    /**
     * Looks up the chassis record for a unit that declares its own availability, creating it if the Force Generator has
     * not seen it before, and attaches the model to it.
     *
     * @param modelRecord the model record for the unit
     *
     * @return the chassis record
     */
    private ChassisRecord addChassis(ModelRecord modelRecord) {
        String chassisKey = modelRecord.getChassisKey();
        ChassisRecord chassisRecord = chassis.get(chassisKey);

        if (chassisRecord == null) {
            chassisRecord = new ChassisRecord(modelRecord.getChassis());
            chassisRecord.setUnitType(modelRecord.getUnitType());
            chassisRecord.setOmni(modelRecord.isOmni());
            chassisRecord.setClan(modelRecord.isClan());
            chassisRecord.setIntroYear(modelRecord.getIntroYear());
            chassis.put(chassisKey, chassisRecord);
        }

        chassisRecord.addModel(modelRecord);

        return chassisRecord;
    }

    /**
     * Records a custom unit's model availability for an era. These values decide how a chassis's share is split among
     * its variants, so only their ratio to sibling variants matters.
     *
     * @param era           the era being loaded
     * @param modelRecord   the model record for the unit
     * @param ratingsForEra the unit's ratings for this era, keyed by faction code
     */
    private void addModelAvailability(int era, ModelRecord modelRecord,
          Map<String, UnitFileRating> ratingsForEra) {

        Map<String, AvailabilityRating> modelRatings = modelIndex.get(era)
              .computeIfAbsent(modelRecord.getKey(), key -> new HashMap<>());

        for (Map.Entry<String, UnitFileRating> entry : ratingsForEra.entrySet()) {
            String factionCode = entry.getKey();
            AvailabilityRating modelRating = entry.getValue()
                  .toRating(modelRecord.getKey(), era, factions.get(factionCode));

            modelRecord.getIncludedFactions().add(modelRating.getFaction());
            modelRatings.put(factionCode, modelRating);
        }
    }

    /**
     * Records a custom unit's chassis availability for an era. This value competes against every other chassis in the
     * table, so it decides how often the design shows up at all.
     * <p>
     * A faction the era XML already rates for this chassis is left exactly as canon has it. A faction canon does not
     * rate takes the value from the unit file. Where several custom variants rate the same unrated faction, the highest
     * wins.
     * </p>
     *
     * @param era                 the era being loaded
     * @param chassisRecord       the chassis record for the unit
     * @param ratingsForEra       the unit's ratings for this era, keyed by faction code
     * @param canonChassisRatings the chassis/faction pairs the era XML defined, which must not be overwritten
     */
    private void addChassisAvailability(int era, ChassisRecord chassisRecord,
          Map<String, UnitFileRating> ratingsForEra,
          Set<String> canonChassisRatings) {

        String chassisKey = chassisRecord.getChassisKey();
        Map<String, AvailabilityRating> chassisRatings = chassisIndex.get(era)
              .computeIfAbsent(chassisKey, key -> new HashMap<>());

        for (Map.Entry<String, UnitFileRating> entry : ratingsForEra.entrySet()) {
            String factionCode = entry.getKey();

            // Canon wins: a faction the era XML already rates for this chassis keeps its value
            if (canonChassisRatings.contains(chassisRatingKey(chassisKey, factionCode))) {
                continue;
            }

            // Several variants rating the same faction: the chassis takes the highest value given
            AvailabilityRating existingRating = chassisRatings.get(factionCode);
            if ((existingRating != null) && (existingRating.getAvailability() >= entry.getValue().availability())) {
                continue;
            }

            AvailabilityRating chassisRating = entry.getValue()
                  .toRating(chassisKey, era, factions.get(factionCode));

            chassisRecord.getIncludedFactions().add(chassisRating.getFaction());
            chassisRatings.put(factionCode, chassisRating);
        }
    }

    /**
     * One validated availability code from a unit file, resolved against a single era.
     * <p>
     * The raw code is kept so that a fresh {@link AvailabilityRating} can be built for the chassis index and the model
     * index separately. Those two carry different unit names, and {@link AvailabilityRating#makeCopy(String)} rebuilds
     * from a code string that does not carry the start year, so it cannot be used here.
     * </p>
     *
     * @param code         the raw availability code, for example {@code "FS:5"}
     * @param startYear    the first year the unit is available within the era
     * @param availability the parsed availability value, used to pick the highest when variants disagree
     */
    private record UnitFileRating(String code, int startYear, int availability) {

        /**
         * Builds a rating for one index entry.
         *
         * @param unitKey       the chassis key or model key this rating is filed under
         * @param era           the era being loaded
         * @param factionRecord the faction, or {@code null} for the General fallback
         *
         * @return the rating
         */
        private AvailabilityRating toRating(String unitKey, int era, @Nullable FactionRecord factionRecord) {
            AvailabilityRating rating = new AvailabilityRating(unitKey, era, code);

            // If the code gives a value per equipment rating, generate the index values alongside the letters
            if (rating.hasMultipleRatings()) {
                rating.setRatingByNumericLevel(factionRecord);
            }

            if (startYear > rating.getStartYear()) {
                rating.setStartYear(startYear);
            }

            return rating;
        }
    }
}
