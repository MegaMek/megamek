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
package megamek.utilities.ratdata;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.GZIPOutputStream;

import com.fasterxml.jackson.databind.ObjectMapper;
import megamek.SuiteConstants;
import megamek.client.ratgenerator.AvailabilityRating;
import megamek.client.ratgenerator.ChassisRecord;
import megamek.client.ratgenerator.FactionRecord;
import megamek.client.ratgenerator.FactionRecord.TechCategory;
import megamek.client.ratgenerator.MissionRole;
import megamek.client.ratgenerator.ModelRecord;
import megamek.client.ratgenerator.RATGenerator;
import megamek.common.units.UnitType;
import megamek.logging.MMLogger;

/**
 * Writes the RAT Generator's inputs, as loaded by MegaMek, for the website's copy of the table-building steps.
 * <p>
 * Unit availability codes are written raw, exactly as the era files (and units that declare their own availability)
 * give them, because the website resolves them itself and lets people edit them to test weights. Faction settings
 * that MegaMek resolves through parent factions (tech percentages, margins, salvage, weight distributions) are written
 * already resolved, per era, by MegaMek's own methods.
 * </p>
 * <p>
 * An availability code is written as {@code FACTION=CODE}, with {@code @YEAR} appended when the code starts later
 * than its era, for example {@code FS=7+}, {@code LA=!A:5!B:3} or {@code FWL=4@3057}.
 * </p>
 */
public final class RatInputExporter {

    private static final MMLogger LOGGER = MMLogger.create(RatInputExporter.class);

    /** Unit types that use a weight distribution: MEK, TANK and AEROSPACE_FIGHTER, as generateTable checks. */
    private static final int[] WEIGHT_DISTRIBUTION_TYPES = { UnitType.MEK, UnitType.TANK, UnitType.AEROSPACE_FIGHTER };

    private final RATGenerator ratGenerator;
    private final List<String> weightDistributionFailures = new ArrayList<>();

    /**
     * @param ratGenerator a fully loaded RAT Generator
     */
    public RatInputExporter(RATGenerator ratGenerator) {
        this.ratGenerator = ratGenerator;
    }

    /**
     * Writes {@code inputs.json.gz} into the folder.
     *
     * @param outputFolder where to write
     * @param buildId      free text recorded in the file, such as the data commit
     *
     * @throws IOException if the file cannot be written
     */
    public void write(Path outputFolder, String buildId) throws IOException {
        Files.createDirectories(outputFolder);
        Map<String, Object> inputs = new LinkedHashMap<>();
        inputs.put("formatVersion", 1);
        inputs.put("generatedAt", Instant.now().toString());
        inputs.put("megamekVersion", SuiteConstants.VERSION.toString());
        inputs.put("buildId", buildId);
        inputs.put("eras", new ArrayList<>(ratGenerator.getEraSet()));
        inputs.put("unitTypes", unitTypeNames());
        inputs.put("roles", roleNames());
        inputs.put("networks", networkMasks());

        List<ModelRecord> models = sortedModels();
        Map<String, Integer> modelIndexByKey = new HashMap<>();
        for (int index = 0; index < models.size(); index++) {
            modelIndexByKey.put(models.get(index).getKey(), index);
        }
        List<Map<String, Object>> modelRows = new ArrayList<>();
        for (ModelRecord model : models) {
            modelRows.add(modelRow(model));
        }
        inputs.put("models", modelRows);

        List<ChassisRecord> chassisList = sortedChassis();
        List<Map<String, Object>> chassisRows = new ArrayList<>();
        for (ChassisRecord chassisRecord : chassisList) {
            chassisRows.add(chassisRow(chassisRecord, modelIndexByKey));
        }
        inputs.put("chassis", chassisRows);

        Map<String, Object> codesByEra = new LinkedHashMap<>();
        for (int era : ratGenerator.getEraSet()) {
            codesByEra.put(Integer.toString(era), eraCodes(era, chassisList, models));
        }
        inputs.put("codes", codesByEra);

        List<Map<String, Object>> factionRows = new ArrayList<>();
        Map<String, String> aliasToKey = new TreeMap<>();
        for (FactionRecord faction : sortedFactions()) {
            factionRows.add(factionRow(faction));
            for (String aliasCode : faction.getAliases().values()) {
                aliasToKey.putIfAbsent(aliasCode, faction.getKey());
            }
        }
        inputs.put("factions", factionRows);
        inputs.put("factionAliases", aliasToKey);
        if (!weightDistributionFailures.isEmpty()) {
            LOGGER.warn("[RatData] Weight distribution lookup fails for {} faction eras (several weight classes "
                  + "cannot be generated there): {}", weightDistributionFailures.size(), weightDistributionFailures);
        }

        try (OutputStream output = new GZIPOutputStream(
              Files.newOutputStream(outputFolder.resolve("inputs.json.gz")))) {
            new ObjectMapper().writeValue(output, inputs);
        }
    }

    private static List<String> unitTypeNames() {
        List<String> names = new ArrayList<>();
        for (int unitType = 0; unitType < UnitType.SIZE; unitType++) {
            names.add(UnitType.getTypeName(unitType));
        }
        return names;
    }

    private static List<String> roleNames() {
        List<String> names = new ArrayList<>();
        for (MissionRole role : MissionRole.values()) {
            names.add(role.toString());
        }
        return names;
    }

    private static Map<String, Integer> networkMasks() {
        Map<String, Integer> masks = new LinkedHashMap<>();
        masks.put("none", ModelRecord.NETWORK_NONE);
        masks.put("c3Slave", ModelRecord.NETWORK_C3_SLAVE);
        masks.put("c3Master", ModelRecord.NETWORK_C3_MASTER);
        masks.put("c3i", ModelRecord.NETWORK_C3I);
        masks.put("nova", ModelRecord.NETWORK_NOVA);
        masks.put("boosted", ModelRecord.NETWORK_BOOSTED);
        masks.put("companyCommand", ModelRecord.NETWORK_COMPANY_COMMAND);
        masks.put("boostedSlave", ModelRecord.NETWORK_BOOSTED_SLAVE);
        masks.put("boostedMaster", ModelRecord.NETWORK_BOOSTED_MASTER);
        masks.put("baC3", ModelRecord.NETWORK_BA_C3);
        masks.put("navalC3", ModelRecord.NETWORK_NAVAL_C3);
        return masks;
    }

    private List<ModelRecord> sortedModels() {
        TreeMap<String, ModelRecord> sorted = new TreeMap<>();
        for (ModelRecord model : ratGenerator.getModelList()) {
            if (model.getMekSummary() != null) {
                sorted.put(model.getKey(), model);
            }
        }
        return new ArrayList<>(sorted.values());
    }

    private List<ChassisRecord> sortedChassis() {
        TreeMap<String, ChassisRecord> sorted = new TreeMap<>();
        for (ChassisRecord chassisRecord : ratGenerator.getChassisList()) {
            sorted.put(chassisRecord.getChassisKey(), chassisRecord);
        }
        return new ArrayList<>(sorted.values());
    }

    private List<FactionRecord> sortedFactions() {
        TreeMap<String, FactionRecord> sorted = new TreeMap<>();
        for (FactionRecord faction : ratGenerator.getFactionList()) {
            sorted.put(faction.getKey(), faction);
        }
        return new ArrayList<>(sorted.values());
    }

    private static Map<String, Object> modelRow(ModelRecord model) {
        List<String> roles = new ArrayList<>();
        for (MissionRole role : model.getRoles()) {
            roles.add(role.toString());
        }
        roles.sort(null);
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("key", model.getKey());
        row.put("chassis", model.getMekSummary().getFullChassis());
        row.put("model", model.getMekSummary().getModel());
        row.put("chassisKey", model.getChassisKey());
        row.put("unitType", model.getUnitType());
        row.put("introYear", model.getIntroYear());
        row.put("weightClass", model.getWeightClass());
        row.put("movementMode", model.getMovementMode().name());
        row.put("networkMask", model.getNetworkMask());
        row.put("speed", model.getSpeed());
        row.put("longRange", model.getLongRange());
        row.put("flak", model.getFlak());
        row.put("ammoRequirement", model.getAmmoRequirement());
        row.put("incendiary", model.hasIncendiaryWeapon());
        row.put("apWeapons", model.hasAPWeapons());
        row.put("magClamp", model.hasMagClamp());
        row.put("mechanizedBA", model.canDoMechanizedBA());
        row.put("omni", model.isOmni());
        row.put("clan", model.isClan());
        row.put("starLeague", model.isSL());
        row.put("mixedOrClanTech", model.isMixedOrClanTech());
        row.put("roles", roles);
        row.put("excludedFactions", model.getExcludedFactions());
        row.put("mulId", model.getMekSummary().getMulId());
        row.put("tons", model.getMekSummary().getTons());
        // What the website needs to link a unit to other MegaMek tools: the chassis without any Clan name, the
        // unit type as MegaMek names it, whether it is a support vehicle, and its sub-type ("Industrial" for an
        // IndustrialMek)
        row.put("baseChassis", model.getMekSummary().getChassis());
        row.put("unitTypeName", model.getMekSummary().getUnitType());
        row.put("support", model.getMekSummary().isSupport());
        row.put("unitSubType", model.getMekSummary().getUnitSubType());
        // The unit file's own UUID, the stable key other MegaMek tools (MekBay) use for the same unit
        row.put("uuid", model.getMekSummary().getUnitFileUUID());
        return row;
    }

    private static Map<String, Object> chassisRow(ChassisRecord chassisRecord, Map<String, Integer> modelIndexByKey) {
        List<Integer> modelIndexes = new ArrayList<>();
        for (ModelRecord model : chassisRecord.getModels()) {
            Integer modelIndex = modelIndexByKey.get(model.getKey());
            if ((modelIndex != null) && !modelIndexes.contains(modelIndex)) {
                modelIndexes.add(modelIndex);
            }
        }
        modelIndexes.sort(null);
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("key", chassisRecord.getChassisKey());
        row.put("name", chassisRecord.getChassis());
        row.put("unitType", chassisRecord.getUnitType());
        row.put("omni", chassisRecord.isOmni());
        row.put("clan", chassisRecord.isClan());
        row.put("introYear", chassisRecord.getIntroYear());
        row.put("models", modelIndexes);
        return row;
    }

    /**
     * @return the era's codes: {@code chassis} keyed by chassis index and {@code models} keyed by model index, each a
     *       list of encoded codes. A unit with an entry, even an empty one, is listed in that era.
     */
    private Map<String, Object> eraCodes(int era, List<ChassisRecord> chassisList, List<ModelRecord> models) {
        Map<String, List<String>> chassisCodes = new LinkedHashMap<>();
        for (int index = 0; index < chassisList.size(); index++) {
            Collection<AvailabilityRating> ratings =
                  ratGenerator.getChassisFactionRatings(era, chassisList.get(index).getChassisKey());
            if (ratings != null) {
                chassisCodes.put(Integer.toString(index), encodeRatings(ratings));
            }
        }
        Map<String, List<String>> modelCodes = new LinkedHashMap<>();
        for (int index = 0; index < models.size(); index++) {
            Collection<AvailabilityRating> ratings = ratGenerator.getModelFactionRatings(era, models.get(index).getKey());
            if (ratings != null) {
                modelCodes.put(Integer.toString(index), encodeRatings(ratings));
            }
        }
        Map<String, Object> codes = new LinkedHashMap<>();
        codes.put("chassis", chassisCodes);
        codes.put("models", modelCodes);
        return codes;
    }

    static List<String> encodeRatings(Collection<AvailabilityRating> ratings) {
        TreeMap<String, String> sorted = new TreeMap<>();
        for (AvailabilityRating rating : ratings) {
            sorted.put(rating.getFactionCode(), encodeRating(rating));
        }
        return new ArrayList<>(sorted.values());
    }

    static String encodeRating(AvailabilityRating rating) {
        String encoded = rating.getFactionCode() + "=" + rating.getAvailabilityCode();
        if (rating.getStartYear() != rating.getEra()) {
            encoded += "@" + rating.getStartYear();
        }
        return encoded;
    }

    private Map<String, Object> factionRow(FactionRecord faction) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("key", faction.getKey());
        row.put("name", faction.getName());
        row.put("namesByYear", faction.getAltNames());
        row.put("aliasesByYear", faction.getAliases());
        row.put("ratingLevels", faction.getRatingLevels());
        row.put("ratingSystem", faction.getRatingLevelSystem());
        row.put("parents", faction.getParentFactions());
        row.put("clan", faction.isClan());
        row.put("periphery", faction.isPeriphery());
        row.put("minor", faction.isMinor());
        row.put("activeYears", activeRanges(faction));

        int ratingCount = Math.max(1, faction.getRatingLevelSystem().size());
        Map<String, Object> eras = new LinkedHashMap<>();
        for (int era : ratGenerator.getEraSet()) {
            Map<String, Object> eraRow = new LinkedHashMap<>();
            eraRow.put("pctSalvage", faction.getPctSalvage(era));
            eraRow.put("salvage", new TreeMap<>(faction.getSalvage(era)));
            eraRow.put("omniMargin", faction.getOmniMargin(era));
            eraRow.put("techMargin", faction.getTechMargin(era));
            eraRow.put("upgradeMargin", faction.getUpgradeMargin(era));
            Map<String, List<Integer>> pctTech = new LinkedHashMap<>();
            for (TechCategory category : TechCategory.values()) {
                List<Integer> byRating = new ArrayList<>();
                for (int rating = 0; rating < ratingCount; rating++) {
                    byRating.add(faction.findPctTech(category, era, rating));
                }
                pctTech.put(category.name(), byRating);
            }
            eraRow.put("pctTech", pctTech);
            Map<String, List<Integer>> weightDistribution = new LinkedHashMap<>();
            List<Integer> failingUnitTypes = new ArrayList<>();
            for (int unitType : WEIGHT_DISTRIBUTION_TYPES) {
                try {
                    List<Integer> distribution = faction.getWeightDistribution(era, unitType);
                    if (distribution != null) {
                        weightDistribution.put(Integer.toString(unitType), distribution);
                    }
                } catch (IndexOutOfBoundsException exception) {
                    // MegaMek adds parent distributions slot by slot and fails when the parents' lists differ in
                    // length, so generateTable fails for this faction whenever several weight classes are asked
                    // for. Record it so the website can say so rather than invent a distribution.
                    failingUnitTypes.add(unitType);
                }
            }
            eraRow.put("weightDistribution", weightDistribution);
            if (!failingUnitTypes.isEmpty()) {
                eraRow.put("weightDistributionFails", failingUnitTypes);
                weightDistributionFailures.add(faction.getKey() + "/" + era);
            }
            eras.put(Integer.toString(era), eraRow);
        }
        row.put("eras", eras);
        return row;
    }

    /**
     * @return the year ranges, from the first era to the last, in which the faction is active, as [first, last]
     */
    private List<int[]> activeRanges(FactionRecord faction) {
        int fromYear = ratGenerator.getEraSet().first();
        int toYear = ratGenerator.getEraSet().last();
        List<int[]> ranges = new ArrayList<>();
        int rangeStart = -1;
        for (int year = fromYear; year <= toYear; year++) {
            boolean active = faction.isActiveInYear(year);
            if (active && (rangeStart < 0)) {
                rangeStart = year;
            } else if (!active && (rangeStart >= 0)) {
                ranges.add(new int[] { rangeStart, year - 1 });
                rangeStart = -1;
            }
        }
        if (rangeStart >= 0) {
            ranges.add(new int[] { rangeStart, toYear });
        }
        return ranges;
    }
}
