/*
 * Copyright (C) 2020 - The MegaMek Team. All Rights Reserved
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.client.generator;

import megamek.MMConstants;
import megamek.common.enums.Gender;
import megamek.common.util.weightedMaps.WeightedIntMap;
import org.apache.logging.log4j.LogManager;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

/**
 * This class sets up a random name generator that can then be used to generate random pilot names.
 * It has a couple different settings and flexible input file directory locations
 *
 * Files are located in /data/names/. All files are comma spaced csv files
 *
 * The historicalEthnicity.csv file shows the correspondence between the different ethnic names
 * and their numeric code in the database. This file is used to initialize the name mapping, and
 * must be kept current for all additions. The same numeric code MUST be used across all of the
 * files listed below.
 * The numeric codes MUST be listed in exact sequential order (NO skipped numbers) for the load
 * to work correctly.
 *
 * The name database is located in three files: maleGivenNames.csv, femaleGivenNames.csv,
 * and surnames.csv.
 *
 * The database is divided into three fields; an Integer Ethnic Code, a String name, and an Integer weight.
 * The Ethnic Code is an Integer identifying the ethnic group from the historicalEthnicity.csv file the name is from
 * The Name is a String containing either a male/female first name or a surname, dependant on the origin file.
 * The Weight is an Integer that is used to set the generation chance of the name. The higher the number,
 * the more common the name is.
 *
 * Faction files are located in /data/names/factions/ directory.
 * The faction key is the filename without the extension.
 * The faction files will have varying number of fields depending on how many ethnic groups exist.
 * The faction file does two things:
 * First, it identifies the relative frequency of different ethnic surnames for a faction.
 * Second, it identifies the correspondence between first names and surnames.
 * This allows, for example, for more Japanese first names regardless of surname in the Draconis
 * Combine. There MUST be a line in the Faction file for each ethnic group, although a weight of 0
 * can be used to prevent the generation of a grouping of names
 *
 * This is divided into 3 + n fields, where n is the number of ethnic groups listed in historicalEthnicity.csv,
 * divided into the following groupings:
 * The Integer Ethnic Code is the first field
 * The String Ethnic Name is the second field. This is included for ease of reference, and
 * is NOT used by the generator.
 * The Integer Weight for generating a surname of the specified ethnicity. The higher the number,
 * the more common the surname is for a faction.
 * This is followed by n fields each containing the Integer Weight for generating a given name for
 * the ethnicity with Ethnic Code n. The higher the number for the weight, the more common that
 * given name ethnicity is in generation for the specific ethnicity of the generated surname.
 *
 * @author Justin "Windchild" Bowen (current version - April 29th, 2020)
 * @author Jay Lawson (original version)
 */
public class RandomNameGenerator implements Serializable {
    //region Variable Declarations
    private static final long serialVersionUID = 5765118329881301375L;

    private static RandomNameGenerator rng; // This is using a singleton, because only a single usage of this class is required

    //region Data Maps
    /**
     * femaleGivenNames, maleGivenNames, and surnames contain values in the following format:
     * Map<Integer Ethnic_Code, WeightedMap<String Name>>
     * The ethnic code is an Integer value that is used to determine the ethnicity of the name, while
     * the name is a String value. The name is stored in a WeightedMap for each ethnic code to ensure
     * that there is a range from common to rare names. This is determined based on the input weights
     */
    private static Map<Integer, WeightedIntMap<String>> femaleGivenNames;
    private static Map<Integer, WeightedIntMap<String>> maleGivenNames;
    private static Map<Integer, WeightedIntMap<String>> surnames;

    /**
     * factionGivenNames contains values in the following format:
     * Map<String Faction_Name, Map<Integer Surname_Ethnic_Code, WeightedIntMap<Integer Given_Name_Ethnic_Code>>>
     * The faction name is the key to determining which list of names should be used, with the default being "General"
     * The Surname Ethnic Code is the code that the surname will be generated from
     * The Given Name Ethnic Code is the code to generate the given name from, from the femaleGivenNames or maleGivenNames
     * maps, and this is weighted to ensure that more common pairings are more common
     */
    private static Map<String, Map<Integer, WeightedIntMap<Integer>>> factionGivenNames;

    /**
     * factionEthnicCodes contains values in the following format:
     * Map<String Faction_Name, WeightedIntMap<Integer Surname_Ethnic_Code>>
     * The faction name is the key to determining which list of names should be used, with the default being "General"
     * The Surname Ethnic Code is the code that the surname will be generated from, and
     * this is weighted to ensure that more common pairings for the faction are more common
     */
    private static Map<String, WeightedIntMap<Integer>> factionEthnicCodes;

    /**
     * historical ethnicity is a map of the ethnic code to the historical region of origin on Earth
     */
    private static Map<Integer, String> historicalEthnicity;
    //endregion Data Maps

    //region Faction Keys
    public static final String KEY_DEFAULT_FACTION = "General";
    public static final String KEY_DEFAULT_CLAN = "Clan";
    //endregion Faction Keys

    //region Default Names
    public static final String UNNAMED = "Unnamed";
    public static final String UNNAMED_SURNAME = "Person";
    public static final String UNNAMED_FULL_NAME = "Unnamed Person";
    //endregion Default Names

    private String chosenFaction;

    private static volatile boolean initialized = false; // volatile to ensure readers get the current version
    //endregion Variable Declarations

    public RandomNameGenerator() {
        chosenFaction = KEY_DEFAULT_FACTION;
    }

    //region Name Generators
    /**
     * This is used to generate a name for MegaMek only that uses the chosen faction
     * @param gender the gender to generate the name for
     * @param clanPilot if the name is for a clanPilot
     * @return a string containing the randomly generated name
     */
    public String generate(Gender gender, boolean clanPilot) {
        return generate(gender, clanPilot, getChosenFaction());
    }

    /**
     * Generate a single random name for MegaMek only
     *
     * @param gender the gender to generate the name for
     * @param clanPilot if the name is for a clanPilot
     * @param faction a string containing the faction key with which to generate the name from.
     *                If the faction is not a key for the <code>factionSurnames</code> Map,
     *                it will instead generate based on the General list
     * @return a string containing the randomly generated name
     */
    public String generate(Gender gender, boolean clanPilot, String faction) {
        String name = UNNAMED_FULL_NAME;
        if (initialized) {
            // This checks to see if we've got a name map for the faction. If we do not, then we
            // go to check if the person is a clanPilot. If they are, then they default to the default
            // clan key provided that exists.
            // If the key isn't set by either case above, then the name is generated based on the
            // default faction key
            faction = factionEthnicCodes.containsKey(faction) ? faction
                    : ((clanPilot && (factionEthnicCodes.containsKey(KEY_DEFAULT_CLAN)))
                        ? KEY_DEFAULT_CLAN : KEY_DEFAULT_FACTION);
            final int ethnicCode = factionEthnicCodes.get(faction).randomItem();
            final int givenNameEthnicCode = factionGivenNames.get(faction).get(ethnicCode).randomItem();

            name = (gender.isFemale() ? femaleGivenNames : maleGivenNames).get(givenNameEthnicCode).randomItem();

            if (!clanPilot) {
                name += " " + surnames.get(ethnicCode).randomItem();
            }
        }
        return name;
    }

    /**
     * @param gender the gender to generate the name for
     * @param clanPilot if the person is a clanPilot
     * @param ethnicCode the specified ethnic code
     * @return a string containing the randomly generated name
     */
    public String generateWithEthnicCode(Gender gender, boolean clanPilot, int ethnicCode) {
        String name = UNNAMED_FULL_NAME;
        if (initialized) {
            name = (gender.isFemale() ? femaleGivenNames : maleGivenNames).get(ethnicCode).randomItem();

            if (!clanPilot) {
                name += " " + surnames.get(ethnicCode).randomItem();
            }
        }
        return name;
    }

    /**
     * Generate a single random name split between a given name and surname
     *
     * @param gender the gender to generate the name for
     * @param clanPilot if the person is a clanPilot
     * @param faction a string containing the faction key with which to generate the name from.
     *                If the faction is not a key for the <code>factionSurnames</code> Map,
     *                it will instead generate based on the General list
     * @return - a String[] containing the name,
     *              with the given name at String[0]
     *              and the surname at String[1]
     */
    public String[] generateGivenNameSurnameSplit(Gender gender, boolean clanPilot, String faction) {
        String[] name = { UNNAMED, UNNAMED_SURNAME };
        if (initialized) {
            // This checks to see if we've got a name map for the faction. If we do not, then we
            // go to check if the person is a clanPilot. If they are, then they default to the default
            // clan key provided that exists.
            // If the key isn't set by either case above, then the name is generated based on the
            // default faction key
            faction = factionEthnicCodes.containsKey(faction) ? faction
                    : ((clanPilot && (factionEthnicCodes.containsKey(KEY_DEFAULT_CLAN)))
                        ? KEY_DEFAULT_CLAN : KEY_DEFAULT_FACTION);
            final int ethnicCode = factionEthnicCodes.get(faction).randomItem();
            final int givenNameEthnicCode = factionGivenNames.get(faction).get(ethnicCode).randomItem();

            name[0] = (gender.isFemale() ? femaleGivenNames : maleGivenNames).get(givenNameEthnicCode).randomItem();

            name[1] = clanPilot ? "" : surnames.get(ethnicCode).randomItem();
        }
        return name;
    }

    /**
     * @param gender the gender to generate the name for
     * @param clanPilot if the person is a clanPilot
     * @param ethnicCode the specified ethnic code
     * @return - a String[] containing the name,
     *              with the given name at String[0]
     *              and the surname at String[1]
     */
    public String[] generateGivenNameSurnameSplitWithEthnicCode(Gender gender, boolean clanPilot, int ethnicCode) {
        String[] name = { UNNAMED, UNNAMED_SURNAME };
        if (initialized) {
            name[0] = (gender.isFemale() ? femaleGivenNames : maleGivenNames).get(ethnicCode).randomItem();
            name[1] = clanPilot ? "" : surnames.get(ethnicCode).randomItem();
        }
        return name;
    }
    //endregion Name Generators

    //region Getters and Setters
    /**
     * @return the list of potential keys to generate the name from - this MUST NOT be modified
     * once it has been gotten
     */
    public Set<String> getFactions() {
        return (factionEthnicCodes == null) ? null : factionEthnicCodes.keySet();
    }

    /**
     * @return the chosen faction to generate the name from
     */
    public String getChosenFaction() {
        return chosenFaction;
    }

    /**
     * @param chosenFaction the faction to use to generate the name
     */
    public void setChosenFaction(String chosenFaction) {
        this.chosenFaction = chosenFaction;
    }

    /**
     * @return the historical ethnicity map
     */
    public Map<Integer, String> getHistoricalEthnicity() {
        return (historicalEthnicity != null) ? historicalEthnicity : new HashMap<>();
    }

    /**
     * @return the instance of the RandomNameGenerator to use
     */
    public static synchronized RandomNameGenerator getInstance() {
        // only this code reads and writes `rng`
        if (rng == null) {
            // synchronized ensures this will only be entered exactly once
            rng = new RandomNameGenerator();
            rng.runThreadLoader();
        }
        // when getInstance returns, rng will always be non-null
        return rng;
    }
    //endregion Getters and Setters

    //region Initialization
    private void runThreadLoader() {
        Thread loader = new Thread(() -> rng.populateNames(), "Random Name Generator name initializer");
        loader.setPriority(Thread.NORM_PRIORITY - 1);
        loader.start();
    }

    private void populateNames() {
        initializeHistoricalEthnicity();
        initializeFactions();
        initializeNames();
        initialized = true;
    }

    private void initializeHistoricalEthnicity() {
        historicalEthnicity = new HashMap<>();
        loadHistoricalEthnicityFromFile(new File(MMConstants.HISTORICAL_ETHNICITY_FILE));
        loadHistoricalEthnicityFromFile(new File(MMConstants.USER_HISTORICAL_ETHNICITY_FILE));
    }

    private void loadHistoricalEthnicityFromFile(final File file) {
        if (!file.exists()) {
            return;
        }

        try (InputStream is = new FileInputStream(file);
             Scanner input = new Scanner(is, StandardCharsets.UTF_8.name())) {
            while (input.hasNextLine()) {
                final String[] values = input.nextLine().split(",");
                if (values.length >= 2) {
                    historicalEthnicity.put(Integer.parseInt(values[0]), values[1]);
                }
            }
        } catch (Exception e) {
            LogManager.getLogger().error("Failed to parse historical ethnicity file " + file, e);
        }
    }

    private void initializeFactions() {
        factionGivenNames = new HashMap<>();
        factionEthnicCodes = new HashMap<>();
        final Map<String, Map<Integer, Map<Integer, Integer>>> factionGivenNamesLoadMap = new HashMap<>();
        final Map<String, Map<Integer, Integer>> factionEthnicCodesLoadMap = new HashMap<>();
        loadFactionsFromFile(new File(MMConstants.NAME_FACTIONS_DIRECTORY_PATH),
                factionGivenNamesLoadMap, factionEthnicCodesLoadMap);
        loadFactionsFromFile(new File(MMConstants.USER_NAME_FACTIONS_DIRECTORY_PATH),
                factionGivenNamesLoadMap, factionEthnicCodesLoadMap);

        if (factionGivenNamesLoadMap.isEmpty() || factionEthnicCodesLoadMap.isEmpty()) {
            LogManager.getLogger().error("No faction files found!");

            // We will create a general list where everything is weighted at one to allow players to
            // continue to play with named characters, indexing it at 1
            // Initialize Maps
            factionGivenNames.put(KEY_DEFAULT_FACTION, new HashMap<>());
            factionEthnicCodes.put(KEY_DEFAULT_FACTION, new WeightedIntMap<>());

            // Add information to maps
            for (int i = 1; i <= historicalEthnicity.size(); i++) {
                factionGivenNames.get(KEY_DEFAULT_FACTION).put(i, new WeightedIntMap<>());
                factionGivenNames.get(KEY_DEFAULT_FACTION).get(i).add(1, i);
                factionEthnicCodes.get(KEY_DEFAULT_FACTION).add(1, i);
            }
        } else {
            for (final Map.Entry<String, Map<Integer, Map<Integer, Integer>>> externalEntry : factionGivenNamesLoadMap.entrySet()) {
                factionGivenNames.put(externalEntry.getKey(), new HashMap<>());
                for (final Map.Entry<Integer, Map<Integer, Integer>> middleEntry : externalEntry.getValue().entrySet()) {
                    factionGivenNames.get(externalEntry.getKey()).put(middleEntry.getKey(), new WeightedIntMap<>());
                    for (final Map.Entry<Integer, Integer> internalEntry : middleEntry.getValue().entrySet()) {
                        factionGivenNames.get(externalEntry.getKey()).get(middleEntry.getKey()).add(internalEntry.getValue(), internalEntry.getKey());
                    }
                }
            }

            for (final Map.Entry<String, Map<Integer, Integer>> externalEntry : factionEthnicCodesLoadMap.entrySet()) {
                factionEthnicCodes.put(externalEntry.getKey(), new WeightedIntMap<>());
                for (final Map.Entry<Integer, Integer> internalEntry : externalEntry.getValue().entrySet()) {
                    factionEthnicCodes.get(externalEntry.getKey()).add(internalEntry.getValue(), internalEntry.getKey());
                }
            }
        }
    }

    private void loadFactionsFromFile(final File file,
                                      final Map<String, Map<Integer, Map<Integer, Integer>>> factionGivenNamesLoadMap,
                                      final Map<String, Map<Integer, Integer>> factionEthnicCodesLoadMap) {
        if (!file.exists() || !file.isDirectory()) {
            return;
        }

        final String[] filenames = file.list();
        if ((filenames != null) && (filenames.length > 0)) {
            for (final String filename : filenames) {
                if (!filename.endsWith(".csv")) {
                    continue;
                }
                loadFactionFile(new File(file, filename), filename.split("\\.csv")[0],
                        factionGivenNamesLoadMap, factionEthnicCodesLoadMap);
            }
        }
    }

    private void loadFactionFile(final File file, final String key,
                                 final Map<String, Map<Integer, Map<Integer, Integer>>> factionGivenNamesLoadMap,
                                 final Map<String, Map<Integer, Integer>> factionEthnicCodesLoadMap) {
        if (!file.exists() || key.isBlank()) {
            return;
        }

        factionGivenNamesLoadMap.putIfAbsent(key, new HashMap<>());
        factionEthnicCodesLoadMap.putIfAbsent(key, new HashMap<>());

        try (InputStream is = new FileInputStream(file);
             Scanner input = new Scanner(is, StandardCharsets.UTF_8.name())) {
            while (input.hasNextLine()) {
                final String[] values = input.nextLine().split(",");
                final int ethnicCode = Integer.parseInt(values[0]);

                factionGivenNamesLoadMap.get(key).put(ethnicCode, new HashMap<>());

                // Add information to maps
                // The weights for ethnic given names for each surname ethnicity will be
                // stored in the file at i + 2, so that is where we will parse them from
                for (int i = 1; i <= historicalEthnicity.size(); i++) {
                    factionGivenNamesLoadMap.get(key).get(ethnicCode).put(i, Integer.parseInt(values[i + 2].trim()));
                }

                if (!factionGivenNamesLoadMap.get(key).get(ethnicCode).isEmpty()) {
                    factionEthnicCodesLoadMap.get(key).put(ethnicCode, Integer.parseInt(values[2]));
                } else {
                    LogManager.getLogger().error("There are no possible options for " + ethnicCode + " for file " + file);
                }
            }
        } catch (Exception e) {
            LogManager.getLogger().error("Failed to parse " + file, e);
        }
    }

    private void initializeNames() {
        maleGivenNames = new HashMap<>();
        femaleGivenNames = new HashMap<>();
        surnames = new HashMap<>();

        final Map<Integer, Map<String, Integer>> maleGivenNamesLoadMap = new HashMap<>();
        final Map<Integer, Map<String, Integer>> femaleGivenNamesLoadMap = new HashMap<>();
        final Map<Integer, Map<String, Integer>> surnamesLoadMap = new HashMap<>();

        // Then immediately instantiate the number of weighted maps needed for Given Names and Surnames
        for (int i = 1; i <= historicalEthnicity.size(); i++) {
            maleGivenNames.put(i, new WeightedIntMap<>());
            femaleGivenNames.put(i, new WeightedIntMap<>());
            surnames.put(i, new WeightedIntMap<>());
            maleGivenNamesLoadMap.put(i, new HashMap<>());
            femaleGivenNamesLoadMap.put(i, new HashMap<>());
            surnamesLoadMap.put(i, new HashMap<>());
        }

        loadNamesFromFile(new File(MMConstants.GIVEN_NAME_MALE_FILE), maleGivenNamesLoadMap);
        loadNamesFromFile(new File(MMConstants.USER_GIVEN_NAME_MALE_FILE), maleGivenNamesLoadMap);
        loadNamesFromFile(new File(MMConstants.GIVEN_NAME_FEMALE_FILE), femaleGivenNamesLoadMap);
        loadNamesFromFile(new File(MMConstants.USER_GIVEN_NAME_FEMALE_FILE), femaleGivenNamesLoadMap);
        loadNamesFromFile(new File(MMConstants.SURNAME_FILE), surnamesLoadMap);
        loadNamesFromFile(new File(MMConstants.USER_SURNAME_FILE), surnamesLoadMap);

        for (final Map.Entry<Integer, Map<String, Integer>> externalEntry : maleGivenNamesLoadMap.entrySet()) {
            for (final Map.Entry<String, Integer> internalEntry : externalEntry.getValue().entrySet()) {
                maleGivenNames.get(externalEntry.getKey()).add(internalEntry.getValue(), internalEntry.getKey());
            }
        }

        for (final Map.Entry<Integer, Map<String, Integer>> externalEntry : femaleGivenNamesLoadMap.entrySet()) {
            for (final Map.Entry<String, Integer> internalEntry : externalEntry.getValue().entrySet()) {
                femaleGivenNames.get(externalEntry.getKey()).add(internalEntry.getValue(), internalEntry.getKey());
            }
        }

        for (final Map.Entry<Integer, Map<String, Integer>> externalEntry : surnamesLoadMap.entrySet()) {
            for (final Map.Entry<String, Integer> internalEntry : externalEntry.getValue().entrySet()) {
                surnames.get(externalEntry.getKey()).add(internalEntry.getValue(), internalEntry.getKey());
            }
        }
    }

    private void loadNamesFromFile(final File file, final Map<Integer, Map<String, Integer>> map) {
        if (!file.exists()) {
            return;
        }

        int lineNumber = 0;

        try (InputStream is = new FileInputStream(file);
             Scanner input = new Scanner(is, StandardCharsets.UTF_8.name())) {
            input.nextLine(); // this is used to skip over the header line

            while (input.hasNextLine()) {
                lineNumber++;
                final String[] values = input.nextLine().split(",");
                if (values.length < 3) {
                    LogManager.getLogger().error("Not enough fields in " + file + " on " + lineNumber);
                    continue;
                }

                map.get(Integer.parseInt(values[0])).put(values[1], Integer.parseInt(values[2]));
            }
        } catch (IOException e) {
            LogManager.getLogger().error("Could not find " + file + "!");
        }
    }
    //endregion Initialization
}
