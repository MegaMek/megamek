/*
 *  MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
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

import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.util.*;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import megamek.common.universe.DateRangeDeserializer;
import megamek.common.universe.Faction2;
import megamek.common.universe.Factions2;
import org.apache.commons.collections4.CollectionUtils;
import org.w3c.dom.Node;

import megamek.common.UnitType;
import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;

/**
 * Stores data about factions used for building RATs, including
 * parent factions, factions to salvage from, and percentages of
 * Clan/SL/Omni tech. Keys are compatible with those used by MekHQ,
 * with various subcommands indicated by a period and an additional
 * key (e.g. DC.SL for Draconis Combine/Sword of Light).
 *
 * @author Neoancient
 */
public class FactionRecord {

    private final static MMLogger logger = MMLogger.create(FactionRecord.class);

    public static final String ALWAYS_ACTIVE = "-";

    /**
     * Proportions of Clan/SL/Omni tech are given for major commands in the various
     * sourcebooks such as the Field Manual series.
     * Canon OmniVehicles are uncommon so no separate category is provided for them.
     */
    public enum TechCategory {
        OMNI, CLAN, IS_ADVANCED,
        OMNI_AERO, CLAN_AERO, IS_ADVANCED_AERO,
        CLAN_VEE, IS_ADVANCED_VEE
    }

    private String key;
    private boolean minor;
    private boolean clan;
    private boolean periphery;
    private String name;
    private TreeMap<Integer, String> altNames;
    private ArrayList<DateRange> yearsActive;
    private ArrayList<String> ratingLevels;
    private HashMap<Integer, Integer> pctSalvage;
    private HashMap<TechCategory, HashMap<Integer, ArrayList<Integer>>> pctTech;
    private HashMap<Integer, HashMap<String, Integer>> salvage;
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
    private HashMap<Integer, Integer> omniMargin;
    /**
     * Margin of uncertainty for Clan/Star League tech percentages
     */
    private HashMap<Integer, Integer> techMargin;
    /**
     * Margin of uncertainty for basic IS tech percentages which may be applied to Star League
     * or advanced IS tech
     */
    private HashMap<Integer, Integer> upgradeMargin;

    private HashMap<Integer, HashMap<Integer, ArrayList<Integer>>> weightDistribution;
    private ArrayList<String> parentFactions;

    // Constants: IS and Clan "general" keys
    public static final String IS_GENERAL_KEY = "IS";
    public static final String CL_GENERAL_KEY = "CLAN";
    public static final String PER_GENERAL_KEY = "PERIPHERY";

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

    /**
     * Get the tech levels appropriate for this faction
     */
    public ArrayList<String> getRatingLevels() {
        return ratingLevels;
    }

    /**
     * Checks the faction parent hierarchy as necessary to find a set of ratings
     * with at least two values. If ratingLevels is empty, this faction inherits
     * the parent's system.
     * If ratingLevels has one member, it indicates a set value for this faction
     * within the parent faction's system.
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
                            (ratingLevels.isEmpty() || retVal.contains(ratingLevels.get(0)))) {
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
     * @return Whether the faction is active
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
     * Sets one or more ranges of years the faction is active.
     *
     * @param formattedRange The range of years the faction is active, as YYYY-YYYY.
     *            The start or end year can be omitted to make the range open at that end.
     *            Factions that vanish and reappear can provide multiple ranges separated
     *            by a comma.
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

    public Integer getPctTech(TechCategory category, int era, int rating) {
        if (!pctTech.containsKey(category) || !pctTech.get(category).containsKey(era)
                || pctTech.get(category).get(era).isEmpty()
                || pctTech.get(category).get(era).size() <= rating) {
            return null;
        }
        return pctTech.get(category).get(era).get(rating);
    }

    /**
     * Get the desired percentage of Clan, Star League/advanced IS tech, or Omni-units
     * that should be present as part of the overall random generation table.
     * @param category  controls which of the three categories, and unit types to lookup
     * @param era       current year
     * @param rating    equipment rating based on available range, typically F (0)/D/C/B/A (4)
     * @return   integer with the percentage (0 - 100) of units that should meet these
     *           criteria. Returns null if the era data contains no C/SL/O data for that
     *           unit type.
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
            return (int) ((total / count + 0.5));
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
     * @param category    The category (Clan, SL/advanced IS, Omni)
     * @param era         The year for which to set the tech percentage
     * @param percentages A comma-separated list of percentages from the highest unit
     *                 rating to the lowest.
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
                    logger.error(ex, "Failed to parse tech percent while loading faction data for " + key);
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
     * @param pct      The percentage of the force that should match the given tech
     *                 category
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
                        logger.error(ex, "RATGenerator: error parsing weight distributions for " + key + ", " + era);
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
        final int[] unitWeightKeys = { UnitType.MEK, UnitType.TANK, UnitType.AEROSPACEFIGHTER };
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

        if (factionRecordBuilder.length() > 0) {
            pw.println("\t<faction key='" + key + "'>");
            pw.println(factionRecordBuilder.toString().replaceFirst("\\n$", ""));
            pw.println("\t</faction>");
        } else {
            pw.println("\t<faction key='" + key + "'/>");
        }
    }

    /**
     * Checks whether the faction is active at any point from the given year to the
     * next reference
     *
     * @param era The era start year
     * @return Whether the faction is active
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
     * @return CSV of all names of the faction, with the original name given first
     *         followed by name changes in the format year:name
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
        for (Iterator<DateRange> iter = yearsActive.iterator(); iter.hasNext();) {
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
