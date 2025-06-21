/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package megamek.common.universe;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import megamek.MMConstants;
import megamek.client.ratgenerator.FactionRecord;

/**
 * This is a Faction class that unifies MHQ's Faction and the RATGenerator's FactionRecord and makes it available to all
 * project parts. The class encompasses both factions such as the Lyran Alliance or Clan Ghost Bear as well as commands
 * such as the Capellan Brigade or the Hesperus Guards as both are used to generate RATs and both could potentially be
 * used as a playable faction. It may be that later on, commands and factions will be separated.
 * <p>
 * To limit the changes, the original Faction and FactionRecord classes are - at least for now - kept largely unchanged,
 * but they now use the data of this class instead of loading their own data.
 * <p>
 * The alternate faction codes and parent factions properties of the two original faction types have been lumped
 * together. They were sometimes equal, sometimes not. Both were used for RAT generation purposes. The alternate names
 * feature (independent of year) of MHQ factions has been dropped as it was not used anywhere. Currency codes are
 * currently not part of this Faction class as there was no data for it
 * <p>
 * Notes for future improvements: There are multiple fallback factions. This may be useful for providing entertaining
 * fallback RATs but a single chain of parent factions may be more useful for other data. Commands could be given a list
 * of specific camos. Specific dates could be used instead of years. Rating levels could be gathered from fallback to
 * avoid repetition.
 */
@SuppressWarnings("unused") // Class fields are assigned when factions are loaded from YAML files
@JsonPropertyOrder({"key", "name", "nameChanges", "capital", "capitalChanges", "yearsActive", "successor",
      "tags", "color", "logo", "background", "camos", "camosChanges", "nameGenerator", "eraMods", "ratingLevels",
      "fallBackFactions", "preInvasionHonorRating", "postInvasionHonorRating", "formationBaseSize", "formationGrouping"})
public class Faction2 {

    private static final int UNKNOWN = -1;

    private String key;
    private String name;
    private final NavigableMap<Integer, String> nameChanges = new TreeMap<>();
    private String capital;
    private final NavigableMap<Integer, String> capitalChanges = new TreeMap<>();
    private final ArrayList<FactionRecord.DateRange> yearsActive = new ArrayList<>();
    private String successor;
    private Set<FactionTag> tags = new HashSet<>();
    private Color color = Color.LIGHT_GRAY;
    private String logo;
    private String background;

    @JsonProperty("camos")
    private String camosFolder;

    private final NavigableMap<Integer, String> camosChanges = new TreeMap<>();
    private String nameGenerator;
    private int[] eraMods;
    private final List<String> ratingLevels = new ArrayList<>();
    private final Set<String> fallBackFactions = new HashSet<>();
    private final HonorRating preInvasionHonorRating = HonorRating.NONE;
    private final HonorRating postInvasionHonorRating = HonorRating.NONE;
    private int formationBaseSize = UNKNOWN;
    private int formationGrouping = UNKNOWN;

    public List<String> getRatingLevels() {
        return ratingLevels;
    }

    public String getKey() {
        return key;
    }

    public String getName() {
        return name;
    }

    public String getName(int year) {
        final Map.Entry<Integer, String> nameByYear = nameChanges.floorEntry(year);
        return (nameByYear == null) ? name : nameByYear.getValue();
    }

    public Set<FactionTag> getTags() {
        return tags;
    }

    public List<FactionRecord.DateRange> getYearsActive() {
        return yearsActive;
    }

    public String getBackground() {
        return background;
    }

    public String getLogo() {
        return logo;
    }

    public int[] getEraMods() {
        return eraMods;
    }

    public String getSuccessor() {
        return successor;
    }

    public String getCapital() {
        return capital;
    }

    public Color getColor() {
        return color;
    }

    public String getNameGenerator() {
        return nameGenerator;
    }

    public HonorRating getPreInvasionHonorRating() {
        return preInvasionHonorRating;
    }

    public HonorRating getPostInvasionHonorRating() {
        return postInvasionHonorRating;
    }

    public String getCamosFolder(int year) {
        final Map.Entry<Integer, String> folderByYear = camosChanges.floorEntry(year);
        return (folderByYear == null) ? camosFolder : folderByYear.getValue();
    }

    public NavigableMap<Integer, String> getCamosChanges() {
        return camosChanges;
    }

    public NavigableMap<Integer, String> getNameChanges() {
        return nameChanges;
    }

    public NavigableMap<Integer, String> getCapitalChanges() {
        return capitalChanges;
    }

    public Set<String> getFallBackFactions() {
        return fallBackFactions;
    }

    /**
     * Returns the size of the lowest formation type (lance). If this faction gives the size directly
     * (formationBaseSize:) this value is returned. Otherwise the fallback Factions are called recursively. When there
     * is no callback Faction, 5 is returned for a clan faction and 4 otherwise.
     * <p>
     * This means that the Word of Blake Faction will give a value of 6 and WoB subcommands do not have to give any
     * value as long as their fallback Faction is WoB.
     *
     * @return The size of a lance, point or analogous formation type
     */
    public int getFormationBaseSize() {
        if (formationBaseSize != UNKNOWN) {
            return formationBaseSize;
        } else if (!fallBackFactions.isEmpty()) {
            for (String factionCode : fallBackFactions) {
                Optional<Faction2> fallBackFaction = Factions2.getInstance().getFaction(factionCode);
                if (fallBackFaction.isPresent()) {
                    return fallBackFaction.get().getFormationBaseSize();
                }
            }
        }
        return isClan() ? 5 : 4;
    }

    /**
     * Returns the grouping multiplier for accumulated formations such as company, galaxy or level 3. If this faction
     * gives the value directly (formationGrouping:) this value is returned. Otherwise the fallback Factions are called
     * recursively. When there is no callback Faction, 5 is returned for a clan faction and 3 otherwise (3 lances form a
     * company, 3 companies form a battalion etc)
     * <p>
     * This means that the Word of Blake Faction will give a value of 6 and WoB subcommands do not have to give any
     * value as long as their fallback Faction is WoB.
     *
     * @return How many formations form a formation of a higher type (e.g., lances in a company)
     */
    public int getFormationGrouping() {
        if (formationGrouping != UNKNOWN) {
            return formationGrouping;
        } else if (!fallBackFactions.isEmpty()) {
            for (String factionCode : fallBackFactions) {
                Optional<Faction2> fallBackFaction = Factions2.getInstance().getFaction(factionCode);
                if (fallBackFaction.isPresent()) {
                    return fallBackFaction.get().getFormationGrouping();
                }
            }
        }
        return isClan() ? 5 : 3;
    }

    @JsonIgnore
    public boolean isClan() {
        return is(FactionTag.CLAN);
    }

    @JsonIgnore
    public boolean isPeriphery() {
        return is(FactionTag.PERIPHERY);
    }

    @JsonIgnore
    public boolean isMinorPower() {
        return is(FactionTag.MINOR);
    }

    public boolean is(FactionTag tag) {
        return tags.contains(tag);
    }

    /**
     * @return True when this faction is active in the given year.
     */
    public boolean isActiveInYear(int year) {
        return yearsActive.isEmpty() || yearsActive.stream().anyMatch(dr -> dr.isInRange(year));
    }

    /**
     * Writes this faction as YAML to the given file.
     *
     * @param file The file to write to.
     * @throws IOException When an error occurs
     */
    public void saveToFile(File file) throws IOException {
        ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory()
              .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
              .disable(YAMLGenerator.Feature.SPLIT_LINES)
              .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
              .enable(YAMLGenerator.Feature.INDENT_ARRAYS_WITH_INDICATOR));
        SimpleModule module = new SimpleModule();
        module.addSerializer(Color.class, new ColorSerializer());
        yamlMapper.registerModule(module);
        yamlMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        yamlMapper.writeValue(file, this);
    }

    /**
     * Writes this faction as YAML to the standard directories in data/universe/factions or data/universe/commands with
     * the standard name (key).yml, depending on whether the key contains a ".".
     *
     * @throws IOException When an error occurs
     */
    public void saveToFile() throws IOException {
        String path = key.contains(".") ? MMConstants.COMMANDS_DIR : MMConstants.FACTIONS_DIR;
        saveToFile(new File(path, key + ".yml"));
    }

    /**
     * Writes this faction with updates from a changed FactionRecord as YAML to the given file. This method is
     * hopefully temporary; it is used for RatGeneratorEditor changes while FactionRecord stays a separate class.
     * Note that this method applies the changes to the "real" faction and keeps the change for the present runtime.
     *
     * @param updatedRecord A FactionRecord with changes to apply to the present faction and save to file
     * @throws IOException When an error occurs
     */
    public void saveToFile(FactionRecord updatedRecord) throws IOException {
        // apply FR changes
        key = updatedRecord.getKey();
        name = updatedRecord.getName();
        if (updatedRecord.isMinor()) {
            tags.add(FactionTag.MINOR);
        } else {
            tags.remove(FactionTag.MINOR);
        }
        if (updatedRecord.isPeriphery()) {
            tags.add(FactionTag.PERIPHERY);
        } else {
            tags.remove(FactionTag.PERIPHERY);
        }
        if (updatedRecord.isClan()) {
            tags.add(FactionTag.CLAN);
        } else {
            tags.remove(FactionTag.CLAN);
        }
        fallBackFactions.clear();
        fallBackFactions.addAll(updatedRecord.getParentFactions());
        fallBackFactions.remove("");
        ratingLevels.clear();
        ratingLevels.addAll(updatedRecord.getRatingLevels());
        yearsActive.clear();
        yearsActive.addAll(updatedRecord.getYears());
        nameChanges.clear();
        nameChanges.putAll(updatedRecord.getAltNames());
        saveToFile();
    }

    // specialized methods for YAML serialization

    @JsonGetter("preInvasionHonorRating")
    private HonorRating preInvasionHonorRatingSerializer() {
        return preInvasionHonorRating != HonorRating.NONE ? preInvasionHonorRating : null;
    }

    @JsonGetter("postInvasionHonorRating")
    private HonorRating getPostInvasionHonorRatingSerializer() {
        return postInvasionHonorRating != HonorRating.NONE ? postInvasionHonorRating : null;
    }

    @JsonGetter("formationGrouping")
    private Integer originalFormationGrouping() {
        return formationGrouping != UNKNOWN ? formationGrouping : null;
    }

    @JsonGetter("formationBaseSize")
    private Integer originalformationBaseSize() {
        return formationBaseSize != UNKNOWN ? formationBaseSize : null;
    }

    @JsonGetter("tags") // sorts tags alphabetically (would be random otherwise)
    private List<FactionTag> tagsSerializer() {
        return tags.stream().sorted(Comparator.comparing(Enum::name)).toList();
    }

    /**
     * @return True if this faction performs Batchalls.
     */
    public boolean performsBatchalls() {
        return tags.contains(FactionTag.BATCHALL);
    }

    /**
     * @return {@code true} if the faction is an aggregate of independent 'factions', rather than a singular
     *       organization.
     *
     *       <p>For example, "PIR" (pirates) is used to abstractly represent all pirates, not individual pirate
     *       groups.</p>
     *
     * @author Illiani
     * @since 0.50.07
     */
    public boolean isAggregate() {
        return tags.contains(FactionTag.AGGREGATE);
    }

    @Override
    public String toString() {
        return key;
    }
}
