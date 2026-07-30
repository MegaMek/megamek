/*
 * Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2016-2026 The MegaMek Team. All Rights Reserved.
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

import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import megamek.common.annotations.Nullable;
import megamek.common.units.UnitType;
import megamek.common.universe.DateRangeDeserializer;
import megamek.common.universe.Faction2;
import megamek.common.universe.Factions2;
import megamek.logging.MMLogger;
import org.apache.commons.collections4.CollectionUtils;
import org.w3c.dom.Node;

/**
 * Stores data about factions used for building RATs, including parent factions, factions to salvage from, and
 * percentages of Clan/SL/Omni tech. Keys are compatible with those used by MekHQ, with various subcommands indicated by
 * a period and an additional key (e.g. DC.SL for Draconis Combine/Sword of Light).
 *
 * @author Neoancient
 */
public class FactionRecord {

    private final static MMLogger logger = MMLogger.create(FactionRecord.class);

    public static final String ALWAYS_ACTIVE = "-";

    /**
     * Proportions of Clan/SL/Omni tech are given for major commands in the various sourcebooks such as the Field Manual
     * series. Canon OmniVehicles are uncommon so no separate category is provided for them.
     */
    public enum TechCategory {
        OMNI, CLAN, IS_ADVANCED,
        OMNI_AERO, CLAN_AERO, IS_ADVANCED_AERO,
        CLAN_VEE, IS_ADVANCED_VEE
    }

    private final String key;
    private boolean minor;
    private boolean clan;
    private boolean periphery;
    private String name;
    private final TreeMap<Integer, String> altNames;
    private final TreeMap<Integer, String> aliases;
    private final ArrayList<DateRange> yearsActive;
    private final ArrayList<String> ratingLevels;
    private final HashMap<Integer, Integer> pctSalvage;
    private final HashMap<TechCategory, HashMap<Integer, ArrayList<Integer>>> pctTech;
    /**
     * Partly-filled tech percentage lists already reported, so the warning in
     * {@link #getPctTech(TechCategory, int, int)} is emitted once per category and era rather than on
     * every lookup. Concurrent because era data is read on the RAT Generator's populator thread while
     * the UI may be querying the same record.
     */
    private final Set<String> reportedPartialPctTechLists = ConcurrentHashMap.newKeySet();
    private final HashMap<Integer, HashMap<String, Integer>> salvage;
    /*
     * FM:Updates gives percentage values for omni, Clan, and SL tech. Later manuals
     * are less precise, giving omni percentages for Clans and (in FM:3085) upgrade
     * percentage for IS and Periphery factions. In order to use the values that are
     * available without either forcing conformity to guesses for later eras or suddenly
     * removing constraints, we extrapolate some values but provide a margin of
     * conformity that grows as we get farther from known values.
     */
    /**
     * Margin of uncertainty for Omni-unit percentages
     */
    private final HashMap<Integer, Integer> omniMargin;
    /**
     * Margin of uncertainty for Clan/Star League tech percentages
     */
    private final HashMap<Integer, Integer> techMargin;
    /**
     * Margin of uncertainty for basic IS tech percentages which may be applied to Star League or advanced IS tech
     */
    private final HashMap<Integer, Integer> upgradeMargin;

    private final HashMap<Integer, HashMap<Integer, ArrayList<Integer>>> weightDistribution;
    private final ArrayList<String> parentFactions;

    // Constants: IS and Clan "general" keys
    public static final String IS_GENERAL_KEY = "IS";
    public static final String CL_GENERAL_KEY = "CLAN";
    public static final String PER_GENERAL_KEY = "PERIPHERY";
    /** The catch-all availability key, used when a unit is equally available to everyone. */
    public static final String GENERAL_KEY = "General";

    public FactionRecord() {
        this("Periphery", "Periphery");
    }

    public FactionRecord(String key) {
        this(key, key);
    }

    public FactionRecord(String key, String name) {
        this.key = key;
        this.name = name;
        minor = clan = periphery = false;
        ratingLevels = new ArrayList<>();
        altNames = new TreeMap<>();
        aliases = new TreeMap<>();
        yearsActive = new ArrayList<>();
        pctSalvage = new HashMap<>();
        pctTech = new HashMap<>();
        omniMargin = new HashMap<>();
        upgradeMargin = new HashMap<>();
        techMargin = new HashMap<>();
        salvage = new HashMap<>();
        weightDistribution = new HashMap<>();
        parentFactions = new ArrayList<>();
    }

    public FactionRecord(Faction2 faction2) {
        this(faction2.getKey(), faction2.getName());
        setMinor(faction2.isMinorPower());
        setClan(faction2.isClan());
        setPeriphery(faction2.isPeriphery());
        setParentFactions(String.join(",", faction2.getFallBackFactions()));
        setRatings(String.join(",", faction2.getRatingLevels()));
        List<String> dateRanges = new ArrayList<>(faction2.getYearsActive().stream().map(DateRange::toString).toList());
        if (dateRanges.isEmpty()) {
            dateRanges.add(ALWAYS_ACTIVE);
        }
        try {
            setYears(String.join(",", dateRanges));
        } catch (ParseException exception) {
            // can't help
        }

        if (!faction2.getNameChanges().isEmpty()) {
            for (Map.Entry<Integer, String> entry : faction2.getNameChanges().entrySet()) {
                setName(entry.getKey(), entry.getValue());
            }
        }
        faction2.getAliases().forEach(this::addAlias);
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof FactionRecord
              && ((FactionRecord) other).getKey().equals(getKey());
    }

    public String getKey() {
        return key;
    }

    public boolean isMinor() {
        return minor;
    }

    public void setMinor(boolean minor) {
        this.minor = minor;
    }

    public boolean isClan() {
        return clan;
    }

    public void setClan(boolean clan) {
        this.clan = clan;
    }

    public boolean isPeriphery() {
        return periphery;
    }

    public void setPeriphery(boolean periphery) {
        this.periphery = periphery;
    }

    public TreeMap<Integer, String> getAltNames() {
        return altNames;
    }

    public TreeMap<Integer, String> getAliases() {
        return new TreeMap<>(aliases);
    }

    /**
     * Adds a historical faction-code alias effective from the given year. See {@link #getAliases()}.
     *
     * @param year      the year the alias code became active
     * @param aliasCode the retired faction code that resolves to this faction
     */
    public void addAlias(int year, String aliasCode) {
        aliases.put(year, aliasCode);
    }

    /**
     * Returns the faction codes under which this faction's unit availability may be recorded, for the given year,
     * ordered so the era-active code (per the {@link #getAliases() alias} date map) is tried first, then this
     * faction's own key, then any remaining historical alias codes.
     *
     * <p>For a faction with no aliases this is simply a single-element list of {@link #getKey() the key}, so
     * availability resolution is unchanged. For a consolidated rename lineage (for example Clan Goliath Scorpion,
     * aliased to {@code CEI} from 3080 and {@code SE} from 3141) generating in 3100 this yields
     * {@code [CEI, CGS, SE]} - so the era-appropriate {@code CEI} availability is preferred while units still listed
     * only under the legacy {@code CGS} code remain reachable.</p>
     *
     * @param year the game year the availability is being resolved for
     *
     * @return the ordered, de-duplicated lineage codes to try. Never {@code null} or empty.
     */
    public List<String> getLineageCodesForYear(int year) {
        if (aliases.isEmpty()) {
            return List.of(key);
        }
        List<String> lineageCodes = new ArrayList<>();
        Map.Entry<Integer, String> activeAlias = aliases.floorEntry(year);
        lineageCodes.add((activeAlias != null) ? activeAlias.getValue() : key);
        if (!lineageCodes.contains(key)) {
            lineageCodes.add(key);
        }
        for (String aliasCode : aliases.values()) {
            if (!lineageCodes.contains(aliasCode)) {
                lineageCodes.add(aliasCode);
            }
        }
        return lineageCodes;
    }

    /**
     * Get the tech levels appropriate for this faction
     */
    public ArrayList<String> getRatingLevels() {
        return ratingLevels;
    }

    /**
     * Checks the faction parent hierarchy as necessary to find a set of ratings with at least two values. If
     * ratingLevels is empty, this faction inherits the parent's system. If ratingLevels has one member, it indicates a
     * set value for this faction within the parent faction's system.
     *
     * @return The list of available equipment ratings for the faction.
     */

    public ArrayList<String> getRatingLevelSystem() {
        if (ratingLevels.size() < 2) {
            for (String parent : parentFactions) {
                FactionRecord fr = RATGenerator.getInstance().getFaction(parent);
                if (fr != null) {
                    ArrayList<String> retVal = fr.getRatingLevelSystem();
                    if (retVal.size() > 1 &&
                          (ratingLevels.isEmpty() || retVal.contains(ratingLevels.getFirst()))) {
                        return retVal;
                    }
                }
            }
        }
        return ratingLevels;
    }

    public String getName() {
        return name;
    }

    public String getName(int year) {
        String retVal = name;
        for (Integer y : altNames.keySet()) {
            if (y <= year) {
                retVal = altNames.get(y);
            } else {
                break;
            }
        }
        return retVal;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setName(int era, String name) {
        altNames.put(era, name);
    }

    public void setNames(String names) {
        String[] fields = names.split(",");
        name = fields[0];
        altNames.clear();
        for (int i = 1; i < fields.length; i++) {
            String[] entry = fields[i].split(":");
            altNames.put(Integer.parseInt(entry[0]), entry[1]);
        }
    }

    /**
     * Check for whether the faction is active in a given year
     *
     * @param year The year to check
     *
     * @return Whether the faction is active
     *
     * @see #isInEra(int)
     */
    public boolean isActiveInYear(int year) {
        for (DateRange dr : yearsActive) {
            if (dr.isInRange(year)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Whether the faction is active in the given year or becomes active in some later year. Used to offer a unit
     * factions that do not exist yet at its introduction date but will field it later, such as the Republic of the
     * Sphere for a unit built before 3081.
     *
     * @param year the earliest year of interest
     *
     * @return {@code true} if the faction is active at or after that year
     */
    public boolean isActiveInOrAfterYear(int year) {
        for (DateRange dateRange : yearsActive) {
            if ((dateRange.end == null) || (dateRange.end >= year)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Sets one or more ranges of years the faction is active.
     *
     * @param formattedRange The range of years the faction is active, as YYYY-YYYY. The start or end year can be
     *                       omitted to make the range open at that end. Factions that vanish and reappear can provide
     *                       multiple ranges separated by a comma.
     *
     * @throws ParseException If the date range format is invalid
     */
    public void setYears(String formattedRange) throws ParseException {
        yearsActive.clear();
        String[] ranges = formattedRange.split(",");
        int offset = 0;
        try {
            for (String range : ranges) {
                if (range.equals(ALWAYS_ACTIVE)) {
                    yearsActive.add(new DateRange(null, null));
                } else if (range.startsWith("-")) {
                    yearsActive.add(new DateRange(null, Integer.parseInt(range.substring(1))));
                } else if (range.endsWith("-")) {
                    yearsActive.add(new DateRange(Integer.parseInt(range.substring(0, range.length() - 1)), null));
                } else {
                    String[] termini = range.split("-");
                    if (termini.length == 2) {
                        yearsActive.add(new DateRange(Integer.parseInt(termini[0]),
                              Integer.parseInt(termini[1])));
                    }
                }
                offset += range.length();
            }
        } catch (Exception ex) {
            throw new ParseException("Could not parse year ranges for faction " + key, offset);
        }
    }

    public Integer getPctSalvage(int era) {
        return pctSalvage.get(era);
    }

    public void setPctSalvage(int era, Integer pct) {
        pctSalvage.put(era, pct);
    }

    public HashMap<String, Integer> getSalvage(int era) {
        if (salvage.containsKey(era) && !salvage.get(era).isEmpty()) {
            return salvage.get(era);
        }
        HashMap<String, Integer> retVal = new HashMap<>();
        if (!parentFactions.isEmpty()) {
            for (String pKey : parentFactions) {
                FactionRecord fRec = RATGenerator.getInstance().getFaction(pKey);
                if (fRec != null) {
                    for (String fKey : fRec.getSalvage(era).keySet()) {
                        retVal.merge(fKey, fRec.getSalvage(era).get(fKey), Integer::sum);
                    }
                } else {
                    logger.debug("RATGenerator: could not locate salvage faction {} for {}", pKey, key);
                }
            }
        }
        salvage.put(era, retVal);
        return retVal;
    }

    public void setSalvage(int era, String faction, Integer wt) {
        salvage.get(era).put(faction, wt);
    }

    public void removeSalvage(int era, String faction) {
        salvage.get(era).remove(faction);
    }

    /**
     * The tech percentage this faction declares for one equipment rating, with a single declared value
     * taken to describe every rating.
     *
     * <p>Era files index these lists worst rating first - index 0 is F, index 4 is A - and the great
     * majority of sub-factions declare a single value, meaning "this is our profile" rather than "this
     * is our profile at rating F only". Reading such a list positionally answered only for F and
     * returned {@code null} for every better rating, so the faction silently inherited its parent's
     * numbers. Because the rulesets default most commands to rating A, an authored one-value profile
     * almost never applied.</p>
     *
     * <p>A list that is longer than one but still shorter than the rating being asked for is a
     * different case: it is ambiguous rather than shorthand, since there is no way to tell which of
     * the missing ratings the author meant to leave to the parent. Those keep the old behaviour and
     * are reported once so the data can be corrected.</p>
     *
     * @param category which of the tech categories to look up
     * @param era      the year of the era table to read
     * @param rating   equipment rating index, typically F (0) to A (4)
     *
     * @return the percentage (0 - 100), or {@code null} if this faction declares nothing usable for
     *       that rating, in which case the caller should fall back to the parent factions
     */
    public @Nullable Integer getPctTech(TechCategory category, int era, int rating) {
        List<Integer> declaredPercentages = declaredPctTechList(category, era);
        if (declaredPercentages == null) {
            return null;
        }
        if (rating < declaredPercentages.size()) {
            return declaredPercentages.get(rating);
        }
        // A lone value describes the faction as a whole, so it answers for every rating.
        if (declaredPercentages.size() == 1) {
            return declaredPercentages.getFirst();
        }
        reportPartialPctTechList(category, era, declaredPercentages.size(), rating);
        return null;
    }

    /**
     * The tech percentage exactly as the era file declares it, without a single value being taken to
     * describe every rating.
     *
     * <p>This is what an editor should show: {@link #getPctTech(TechCategory, int, int)} would report
     * the same number against all five ratings for a faction whose file holds one, making a one-value
     * entry look like a five-value one and inviting an edit that writes back five.</p>
     *
     * @param category which of the tech categories to look up
     * @param era      the year of the era table to read
     * @param rating   equipment rating index, typically F (0) to A (4)
     *
     * @return the declared percentage (0 - 100), or {@code null} if the era file declares no value at
     *       that position
     */
    public @Nullable Integer getDeclaredPctTech(TechCategory category, int era, int rating) {
        List<Integer> declaredPercentages = declaredPctTechList(category, era);
        if ((declaredPercentages == null) || (rating >= declaredPercentages.size())) {
            return null;
        }
        return declaredPercentages.get(rating);
    }

    /**
     * @return the declared percentages for this category and era, or {@code null} when the era file
     *       declares none at all
     */
    private @Nullable List<Integer> declaredPctTechList(TechCategory category, int era) {
        HashMap<Integer, ArrayList<Integer>> percentagesByEra = pctTech.get(category);
        if (percentagesByEra == null) {
            return null;
        }
        ArrayList<Integer> declaredPercentages = percentagesByEra.get(era);
        if ((declaredPercentages == null) || declaredPercentages.isEmpty()) {
            return null;
        }
        return declaredPercentages;
    }

    /**
     * Reports a list that declares some ratings but not the one asked for, once per category and era.
     * Left to the parent factions rather than guessed at, because the author's intent for the missing
     * ratings cannot be recovered from the file.
     */
    private void reportPartialPctTechList(TechCategory category, int era, int declaredCount, int rating) {
        if (reportedPartialPctTechLists.add(category + "/" + era)) {
            logger.warn("Faction {} declares {} {} percentages for era {} but rating {} was requested;"
                        + " falling back to the parent faction. A single value would apply to every"
                        + " rating - list all {} to differentiate them.",
                  key, declaredCount, category, era, rating, ratingLevels.size());
        }
    }

    /**
     * Get the desired percentage of Clan, Star League/advanced IS tech, or Omni-units that should be present as part of
     * the overall random generation table.
     *
     * @param category controls which of the three categories, and unit types to lookup
     * @param era      current year
     * @param rating   equipment rating based on available range, typically F (0)/D/C/B/A (4)
     *
     * @return integer with the percentage (0 - 100) of units that should meet these criteria. Returns null if the era
     *       data contains no C/SL/O data for that unit type.
     */
    public Integer findPctTech(TechCategory category, int era, int rating) {
        // Return the value if present
        Integer retVal = getPctTech(category, era, rating);
        if (retVal != null) {
            return retVal;
        }

        // If the value isn't present, check parent factions and averaging if multiple
        // parent factions are present
        int total = 0;
        int count = 0;
        if (parentFactions != null && !parentFactions.isEmpty()) {
            RATGenerator ratGen = RATGenerator.getInstance();

            for (String parent : parentFactions) {
                FactionRecord parentRecord = ratGen.getFaction(parent);
                if (parentRecord != null) {
                    Integer pct = parentRecord.findPctTech(category, era, rating);
                    if (pct != null) {
                        total += pct;
                        count++;
                    }
                }
            }

        }

        if (count > 0) {
            return total / count;
        } else {
            return null;
        }
    }

    public void setRatings(String str) {
        ratingLevels.clear();
        if (!str.isBlank()) {
            String[] fields = str.split(",");
            Collections.addAll(ratingLevels, fields);
        }
    }

    /**
     * Sets the target percentage for a tech category.
     *
     * <p>The list runs from the <em>lowest</em> unit rating to the highest - index 0 is F and index 4
     * is A - matching the order of {@code ratingLevels}. A list of one value describes the faction as
     * a whole and applies to every rating; see {@link #getPctTech(TechCategory, int, int)}. A list
     * that declares some ratings but not all is reported and left to the parent factions.</p>
     *
     * @param category    The category (Clan, SL/advanced IS, Omni)
     * @param era         The year for which to set the tech percentage
     * @param percentages A comma-separated list of percentages, lowest unit rating first, or
     *                    {@code null} to declare none
     */
    public void setPctTech(TechCategory category, int era, String percentages) {
        if (!pctTech.containsKey(category)) {
            pctTech.put(category, new HashMap<>());
        }
        ArrayList<Integer> list = new ArrayList<>();
        if ((percentages != null) && !percentages.isBlank()) {
            for (String pct : percentages.split(",")) {
                try {
                    list.add(Integer.parseInt(pct));
                } catch (NumberFormatException ex) {
                    logger.error(ex, "Failed to parse tech percent while loading faction data for {}", key);
                }
            }
        }
        pctTech.get(category).put(era, list);
    }

    /**
     * Sets the target percentage for a tech category for a particular rating.
     *
     * @param category The category (omni, clan, SL/upgraded)
     * @param era      The year for which to set the tech percentage
     * @param rating   The unit rating for which to set the tech percentage
     * @param pct      The percentage of the force that should match the given tech category
     */
    public void setPctTech(TechCategory category, int era, int rating, int pct) {
        pctTech.putIfAbsent(category, new HashMap<>());
        pctTech.get(category).putIfAbsent(era, new ArrayList<>());
        while (pctTech.get(category).get(era).size() <= rating) {
            pctTech.get(category).get(era).add(0);
        }
        pctTech.get(category).get(era).set(rating, pct);

    }

    public int getOmniMargin(int era) {
        if (omniMargin.containsKey(era)) {
            return omniMargin.get(era);
        }
        return 0;
    }

    public int getTechMargin(int era) {
        if (techMargin.containsKey(era)) {
            return techMargin.get(era);
        }
        return 0;
    }

    public int getUpgradeMargin(int era) {
        if (upgradeMargin.containsKey(era)) {
            return upgradeMargin.get(era);
        }
        return 0;
    }

    public @Nullable ArrayList<Integer> getWeightDistribution(int era, int unitType) {
        if (weightDistribution.containsKey(era)
              && weightDistribution.get(era).containsKey(unitType)) {
            return weightDistribution.get(era).get(unitType);
        }

        if (!parentFactions.isEmpty()) {
            ArrayList<Integer> retVal = new ArrayList<>();
            for (String fKey : parentFactions) {
                FactionRecord fRec = RATGenerator.getInstance().getFaction(fKey);
                if (fRec != null) {
                    ArrayList<Integer> wd = fRec.getWeightDistribution(era, unitType);
                    if (wd != null) {
                        if (retVal.isEmpty()) {
                            retVal.addAll(wd);
                        } else {
                            for (int i = 0; i < retVal.size(); i++) {
                                retVal.set(i, retVal.get(i) + wd.get(i));
                            }
                        }
                    }
                }
            }
            return retVal;
        }
        return null;
    }

    public void setWeightDistribution(int era, int unitType, String dist) {
        // If the distribution parameter is null, clear the record if it exists.
        if (dist == null) {
            if (weightDistribution.containsKey(era)) {
                weightDistribution.get(era).remove(unitType);
            }
            return;
        }
        ArrayList<Integer> list = new ArrayList<>();
        for (String s : dist.split(",")) {
            list.add(Integer.valueOf(s));
        }

        if (!weightDistribution.containsKey(era)) {
            weightDistribution.put(era, new HashMap<>());
        }
        weightDistribution.get(era).put(unitType, list);
    }

    public ArrayList<String> getParentFactions() {
        return parentFactions;
    }

    public void setParentFactions(String factions) {
        parentFactions.clear();
        Collections.addAll(parentFactions, factions.split(","));
    }

    public void loadEra(Node node, int era) {
        for (int i = 0; i < node.getChildNodes().getLength(); i++) {
            Node wn = node.getChildNodes().item(i);
            switch (wn.getNodeName()) {
                case "pctOmni":
                    if ((wn.getAttributes().getNamedItem("unitType") != null)
                          && wn.getAttributes().getNamedItem("unitType").getTextContent()
                          .equalsIgnoreCase("AeroSpaceFighter")) {
                        setPctTech(TechCategory.OMNI_AERO, era, wn.getTextContent());
                    } else {
                        setPctTech(TechCategory.OMNI, era, wn.getTextContent());
                    }
                    break;
                case "pctClan":
                    if ((wn.getAttributes().getNamedItem("unitType") != null)
                          && wn.getAttributes().getNamedItem("unitType").getTextContent()
                          .equalsIgnoreCase("AeroSpaceFighter")) {
                        setPctTech(TechCategory.CLAN_AERO, era, wn.getTextContent());
                    } else if ((wn.getAttributes().getNamedItem("unitType") != null)
                          && wn.getAttributes().getNamedItem("unitType").getTextContent()
                          .equalsIgnoreCase("Vehicle")) {
                        setPctTech(TechCategory.CLAN_VEE, era, wn.getTextContent());
                    } else {
                        setPctTech(TechCategory.CLAN, era, wn.getTextContent());
                    }
                    break;
                case "pctSL":
                    if ((wn.getAttributes().getNamedItem("unitType") != null)
                          && wn.getAttributes().getNamedItem("unitType").getTextContent()
                          .equalsIgnoreCase("AeroSpaceFighter")) {
                        setPctTech(TechCategory.IS_ADVANCED_AERO, era, wn.getTextContent());
                    } else if ((wn.getAttributes().getNamedItem("unitType") != null)
                          && wn.getAttributes().getNamedItem("unitType").getTextContent()
                          .equalsIgnoreCase("Vehicle")) {
                        setPctTech(TechCategory.IS_ADVANCED_VEE, era, wn.getTextContent());
                    } else {
                        setPctTech(TechCategory.IS_ADVANCED, era, wn.getTextContent());
                    }
                    break;
                case "omniMargin":
                    omniMargin.put(era, Integer.parseInt(wn.getTextContent()));
                    break;
                case "techMargin":
                    techMargin.put(era, Integer.parseInt(wn.getTextContent()));
                    break;
                case "upgradeMargin":
                    upgradeMargin.put(era, Integer.parseInt(wn.getTextContent()));
                    break;
                case "salvage":
                    pctSalvage.put(era, Integer.parseInt(wn.getAttributes().getNamedItem("pct").getTextContent()));
                    salvage.put(era, new HashMap<>());
                    String[] fields = wn.getTextContent().trim().split(",");
                    for (String field : fields) {
                        if (!field.isBlank()) {
                            String[] subfields = field.split(":");
                            if (subfields.length == 2) {
                                salvage.get(era).put(subfields[0], Integer.parseInt(subfields[1]));
                            }
                        }
                    }
                    break;
                case "weightDistribution":
                    try {
                        int unitType = ModelRecord
                              .parseUnitType(wn.getAttributes().getNamedItem("unitType").getTextContent());
                        setWeightDistribution(era, unitType, wn.getTextContent());
                    } catch (Exception ex) {
                        logger.error(ex,
                              "RATGenerator: error parsing weight distributions for {}, {}", key, era);
                    }
                    break;
            }
        }
    }

    public void saveIfChanged() throws IOException {
        Optional<Faction2> original = Factions2.getInstance().getFaction(key);
        if (original.isEmpty()) {
            new Faction2().saveToFile(this);
        } else if (hasDifferences(new FactionRecord(original.get()))) {
            original.get().saveToFile(this);
        }
    }

    public void writeToXml(PrintWriter pw, int era) {
        StringBuilder factionRecordBuilder = new StringBuilder();
        if (pctTech.containsKey(TechCategory.OMNI)
              && pctTech.get(TechCategory.OMNI).containsKey(era)
              && !pctTech.get(TechCategory.OMNI).get(era).isEmpty()) {
            factionRecordBuilder.append(
                  String.format("\t\t<pctOmni>%s</pctOmni>\n",
                        pctTech.get(TechCategory.OMNI).get(era).stream().map(Object::toString)
                              .collect(Collectors.joining(","))));
        }

        if (pctTech.containsKey(TechCategory.CLAN)
              && pctTech.get(TechCategory.CLAN).containsKey(era)
              && !pctTech.get(TechCategory.CLAN).get(era).isEmpty()) {
            factionRecordBuilder.append(
                  String.format("\t\t<pctClan>%s</pctClan>\n",
                        pctTech.get(TechCategory.CLAN).get(era).stream().map(Object::toString)
                              .collect(Collectors.joining(","))));
        }

        if (pctTech.containsKey(TechCategory.IS_ADVANCED)
              && pctTech.get(TechCategory.IS_ADVANCED).containsKey(era)
              && !pctTech.get(TechCategory.IS_ADVANCED).get(era).isEmpty()) {
            factionRecordBuilder.append(
                  String.format("\t\t<pctSL>%s</pctSL>\n",
                        pctTech.get(TechCategory.IS_ADVANCED).get(era).stream().map(Object::toString)
                              .collect(Collectors.joining(","))));
        }

        if (pctTech.containsKey(TechCategory.OMNI_AERO)
              && pctTech.get(TechCategory.OMNI_AERO).containsKey(era)
              && !pctTech.get(TechCategory.OMNI_AERO).get(era).isEmpty()) {
            factionRecordBuilder.append(
                  String.format("\t\t<pctOmni unitType='Aero'>%s</pctOmni>\n",
                        pctTech.get(TechCategory.OMNI_AERO).get(era).stream().map(Object::toString)
                              .collect(Collectors.joining(","))));
        }

        if (pctTech.containsKey(TechCategory.CLAN_AERO)
              && pctTech.get(TechCategory.CLAN_AERO).containsKey(era)
              && !pctTech.get(TechCategory.CLAN_AERO).get(era).isEmpty()) {
            factionRecordBuilder.append(
                  String.format("\t\t<pctClan unitType='Aero'>%s</pctClan>\n",
                        pctTech.get(TechCategory.CLAN_AERO).get(era).stream().map(Object::toString)
                              .collect(Collectors.joining(","))));
        }

        if (pctTech.containsKey(TechCategory.IS_ADVANCED_AERO)
              && pctTech.get(TechCategory.IS_ADVANCED_AERO).containsKey(era)
              && !pctTech.get(TechCategory.IS_ADVANCED_AERO).get(era).isEmpty()) {
            factionRecordBuilder.append(
                  String.format("\t\t<pctSL unitType='Aero'>%s</pctSL>\n",
                        pctTech.get(TechCategory.IS_ADVANCED_AERO).get(era).stream().map(Object::toString)
                              .collect(Collectors.joining(","))));
        }
        if (pctTech.containsKey(TechCategory.CLAN_VEE)
              && pctTech.get(TechCategory.CLAN_VEE).containsKey(era)
              && !pctTech.get(TechCategory.CLAN_VEE).get(era).isEmpty()) {
            factionRecordBuilder.append(
                  String.format("\t\t<pctClan unitType='Vehicle'>%s</pctClan>\n",
                        pctTech.get(TechCategory.CLAN_VEE).get(era).stream().map(Object::toString)
                              .collect(Collectors.joining(","))));
        }

        if (pctTech.containsKey(TechCategory.IS_ADVANCED_VEE)
              && pctTech.get(TechCategory.IS_ADVANCED_VEE).containsKey(era)
              && !pctTech.get(TechCategory.IS_ADVANCED_VEE).get(era).isEmpty()) {
            factionRecordBuilder.append(
                  String.format("\t\t<pctSL unitType='Vehicle'>%s</pctSL>\n",
                        pctTech.get(TechCategory.IS_ADVANCED_VEE).get(era).stream().map(Object::toString)
                              .collect(Collectors.joining(","))));
        }

        if (era > 3067) {
            if (!isClan() && !key.equals("CGB.FRR") && !key.equals("RA.OA")) {
                factionRecordBuilder.append("\t\t<omniMargin>");
                factionRecordBuilder.append((era - 3067) / 5);
                factionRecordBuilder.append("</omniMargin>\n");
                factionRecordBuilder.append("\t\t<techMargin>");
                factionRecordBuilder.append((era - 3067) / 5);
                factionRecordBuilder.append("</techMargin>\n");
                if (era > 3085) {
                    factionRecordBuilder.append("\t\t<upgradeMargin>");
                    factionRecordBuilder.append((era - 3085) / 5);
                    factionRecordBuilder.append("</upgradeMargin>\n");
                }
            } else {
                factionRecordBuilder.append("\t\t<techMargin>");
                factionRecordBuilder.append((era - 3067) / 5);
                factionRecordBuilder.append("</techMargin>\n");
            }
        }

        Integer pct = pctSalvage.get(era);
        if (pct != null) {
            StringJoiner sj = new StringJoiner(",");
            for (String faction : salvage.get(era).keySet()) {
                FactionRecord fr = RATGenerator.getInstance().getFaction(faction);
                if (fr == null || !fr.isInEra(era)) {
                    continue;
                }
                sj.add(faction + ":" + salvage.get(era).get(faction));
            }
            factionRecordBuilder.append("\t\t<salvage pct='").append(pct).append("'>")
                  .append(sj).append("</salvage>\n");
        }
        final int[] unitWeightKeys = { UnitType.MEK, UnitType.TANK, UnitType.AEROSPACE_FIGHTER };
        if (weightDistribution.containsKey(era)) {
            for (int unitType : unitWeightKeys) {
                if (weightDistribution.get(era).containsKey(unitType)
                      && !weightDistribution.get(era).get(unitType).isEmpty()) {
                    factionRecordBuilder.append("\t\t<weightDistribution era='");
                    factionRecordBuilder.append(era);
                    factionRecordBuilder.append("' unitType='");
                    factionRecordBuilder.append(UnitType.getTypeName(unitType));
                    factionRecordBuilder.append("'>");
                    factionRecordBuilder.append(getWeightDistributionAsString(era, unitType));
                    factionRecordBuilder.append("</weightDistribution>\n");
                }
            }
        }

        if (!factionRecordBuilder.isEmpty()) {
            pw.println("\t<faction key='" + key + "'>");
            pw.println(factionRecordBuilder.toString().replaceFirst("\\n$", ""));
            pw.println("\t</faction>");
        } else {
            pw.println("\t<faction key='" + key + "'/>");
        }
    }

    /**
     * Checks whether the faction is active at any point from the given year to the next reference
     *
     * @param era The era start year
     *
     * @return Whether the faction is active
     *
     * @see #isActiveInYear(int)
     */
    public boolean isInEra(int era) {
        if (isActiveInYear(era)) {
            return true;
        }
        /* May still be in era if start date comes before the next era */
        TreeSet<Integer> eraSet = RATGenerator.getInstance().getEraSet();
        Integer next = eraSet.ceiling(era + 1);
        if (next == null) {
            return false;
        }

        for (DateRange dr : yearsActive) {
            if (dr.start != null && dr.start > era && dr.start < next) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return CSV of all names of the faction, with the original name given first followed by name changes in the
     *       format year:name
     */
    public Object getNamesAsString() {
        StringBuilder retVal = new StringBuilder(name);
        for (Integer y : altNames.keySet()) {
            retVal.append(",").append(y).append(":").append(altNames.get(y));
        }
        return retVal.toString();
    }

    /**
     * @return CSV String of all date ranges in which the faction is active.
     */
    public String getYearsAsString() {
        StringBuilder sb = new StringBuilder();
        for (Iterator<DateRange> iter = yearsActive.iterator(); iter.hasNext(); ) {
            DateRange dr = iter.next();
            sb.append(dr.toString());
            if (iter.hasNext()) {
                sb.append(",");
            }
        }
        return sb.toString();
    }

    /**
     * Formats the weight distribution for a given unit type in an era as a String.
     *
     * @param era      The game year
     * @param unitType The type of unit
     *
     * @return A formatted String
     */
    public String getWeightDistributionAsString(int era, int unitType) {
        StringJoiner sj = new StringJoiner(",");
        if (weightDistribution.containsKey(era) && weightDistribution.get(era).containsKey(unitType)) {
            for (Integer i : weightDistribution.get(era).get(unitType)) {
                sj.add(String.valueOf(i));
            }
        }
        return sj.toString();
    }

    public List<DateRange> getYears() {
        return yearsActive;
    }

    @Override
    public String toString() {
        return key;
    }

    @JsonDeserialize(using = DateRangeDeserializer.class)
    public static class DateRange {
        public Integer start;
        public Integer end;

        public DateRange(Integer start, Integer end) {
            this.start = start;
            this.end = end;
        }

        public boolean isInRange(int year) {
            return (start == null || start <= year) && (end == null || end >= year);
        }

        @Override
        public String toString() {
            return "%s-%s".formatted((start != null ? start : ""), (end != null ? end : ""));
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof DateRange dateRange)) {
                return false;
            }
            return Objects.equals(start, dateRange.start) && Objects.equals(end, dateRange.end);
        }

        @Override
        public int hashCode() {
            return Objects.hash(start, end);
        }
    }

    /**
     * Returns true when there's a difference between this and the given other FactionRecord. This is intentionally not
     * overriding equals() as FactionRecord equals() is based on a comparison of the key only.
     *
     * @param other The FactionRecord to compare this to
     *
     * @return True when there's a save-worthy difference, false otherwise
     */
    public boolean hasDifferences(FactionRecord other) {
        return !key.equals(other.key) || !name.equals(other.name) || minor != other.minor
              || clan != other.clan || periphery != other.periphery
              || !CollectionUtils.isEqualCollection(yearsActive, other.yearsActive)
              || !CollectionUtils.isEqualCollection(ratingLevels, other.ratingLevels)
              || !CollectionUtils.isEqualCollection(parentFactions, other.parentFactions)
              || !CollectionUtils.isEqualCollection(altNames.keySet(), other.altNames.keySet())
              || !CollectionUtils.isEqualCollection(altNames.values(), other.altNames.values());
    }
}
