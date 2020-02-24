/*
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 * Copyright © 2013 Edward Cullen (eddy@obsessedcomputers.co.uk)
 * Copyright (C) 2020 - MegaMek team
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */
package megamek.client;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;
import java.util.Vector;

import megamek.common.Compute;
import megamek.common.Configuration;
import megamek.common.util.MegaMekFile;
import megamek.common.util.WeightedMap;

/**
 * This class sets up a random name generator that can then
 * be used to generate random pilot names. it will have a couple different
 * settings and flexible input files
 * <p>
 * Files are located in {@link Configuration#namesDir()}. All files are comma-delimited text files.
 * </p>
 * <p>
 * The masterancestry.txt file shows the correspondence between the different ethnic names and their numeric
 * code in the database. This file is used to initialize the name mapping, and must be kept current
 * for all additions. The same numeric code must be used across all of the files listed below.
 * The numeric codes MUST be listed in exact sequential order (i.e. no skipping numbers)
 * for the program to work correctly.</p>
 * <p>
 * The name database is located in three files: firstname_males.txt, firstname_females.txt, and surnames.txt.
 * There ar three comma-delimited fields in each of these data files: fld1,fld2,fld3
 * <ul>
 * <li>fld1 - The name itself, either a male/female first name or a surname.</li>
 * <li>fld2 - a frequency weight to account for some names being more common than others.</li>
 * <li>fld3 - the numeric code identifying the "ethnic" group this name belongs to.</li>
 * </ul>
 * </p>
 * <p>
 * Faction files are located in {@link Configuration#namesDir()}{@code /factions}.
 * The name that is given before ".txt" is used as the key for the faction.
 * The faction files will have varying number of fields depending on how many
 * ethnic groups exist. The faction file does two things. First, it identifies
 * the relative frequency of different ethnic surnames for a faction.
 * Second, it identifies the correspondence between first names and surnames.
 * This allows, for example, for more Japanese first names regardless of surname
 * in the Draconis Combine. There should be a line in the Faction file for each
 * ethnic group.
 * <ul>
 * <li>fld1 - the id for the ethnic group
 * <li>fld2 - the ethnic group name. Not currently read in, just for easy reference.
 * <li>fld3 - The relative frequency of this ethnic surname in the faction.
 * <li>fld4-fldn - These fields identify the relative frequency of first names from an ethnic group
 *                 given the surname listed in fld1.
 * </ul>
 * </p>
 * @author Jay Lawson
 */
public class RandomNameGenerator implements Serializable {
    private static final String PROP_INITIALIZED = "initialized";

    /** Default directory containing the faction-specific name files. */
    private static final String DIR_NAME_FACTIONS = "factions";

    /** Default filename for the list of male first names. */
    private static final String FILENAME_FIRSTNAMES_MALE = "firstnames_male.txt";

    /** Default filename for the list of female first names. */
    private static final String FILENAME_FIRSTNAMES_FEMALE = "firstnames_female.txt";

    /** Default filename for the list of surnames names. */
    private static final String FILENAME_SURNAMES = "surnames.txt";

    private static final String FILENAME_MASTER_ANCESTRY = "masterancestry.txt";

    private static final long serialVersionUID = 5765118329881301375L;

    private static RandomNameGenerator rng;

    Map<String, Vector<String>> firstM;
    Map<String, Vector<String>> firstF;
    Map<String, Vector<String>> last;
    Map<String, Vector<String>> factionLast;
    Map<String, Map<String, Vector<String>>> factionFirst;

    Map<String, Map<Integer, WeightedMap<WeightedMap<String>>>> factionMaleGivenNames;
    Map<String, Map<Integer, WeightedMap<WeightedMap<String>>>> factionFemaleGivenNames;
    Map<String, WeightedMap<WeightedMap<String>>> factionSurnames;

    private int percentFemale;
    private String chosenFaction;
    private Thread loader;
    private boolean initialized;
    private boolean initializing;

    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    public RandomNameGenerator() {
        percentFemale = 50;
        chosenFaction = "General";
    }

    public void populateNames() {
        //region Variable Instantiation
        int numEthnicCodes = 0;
        //endregion Variable Instantiation

        //region Map Instantiation
        Map<Integer, WeightedMap<String>> maleGivenNames = new HashMap<>();
        Map<Integer, WeightedMap<String>> femaleGivenNames = new HashMap<>();
        Map<Integer, WeightedMap<String>> surnames = new HashMap<>();

        factionMaleGivenNames = new HashMap<>();
        factionFemaleGivenNames = new HashMap<>();
        factionSurnames = new HashMap<>();

        // Determine the number of ethnic codes
        File masterAncestryFile = new MegaMekFile(Configuration.namesDir(), FILENAME_MASTER_ANCESTRY).getFile();
        try (InputStream is = new FileInputStream(masterAncestryFile);
             Scanner input = new Scanner(is, "UTF-8")) {

            while (input.hasNextLine()) {
                input.nextLine();
                numEthnicCodes++;
            }
        } catch (IOException e) {
            System.err.println("RandomNameGenerator.populateNames(): Could not find '" + masterAncestryFile + "'");
        }

        // Then immediately instantiate the number of weighted maps needed for Given Names and Surnames
        for (int i = 1; i <= numEthnicCodes; i++) {
            maleGivenNames.put(i, new WeightedMap<>());
            femaleGivenNames.put(i, new WeightedMap<>());
            surnames.put(i, new WeightedMap<>());
        }
        //endregion Map Instantiation

        //region Read Names
        readNamesFileToMap(maleGivenNames, FILENAME_FIRSTNAMES_MALE);
        readNamesFileToMap(femaleGivenNames, FILENAME_FIRSTNAMES_FEMALE);
        readNamesFileToMap(surnames, FILENAME_SURNAMES);
        //endregion Read Names

        //region Faction Files
        // all faction files should be in the faction directory
        File factionsDir = new MegaMekFile(Configuration.namesDir(), DIR_NAME_FACTIONS).getFile();
        String[] fileNames = factionsDir.list();

        if (fileNames == null) {
            //region No Factions Specified
            System.err.println("RandomNameGenerator.populateNames(): No faction files found!");
            // We will create a general list where everything is weighted at one to allow player to
            // play with named characters, indexing it at 1
            String key = "General";

            // Initialize Maps
            factionMaleGivenNames.put(key, new HashMap<>());
            factionFemaleGivenNames.put(key, new HashMap<>());
            factionSurnames.put(key, new WeightedMap<>());

            // Add information to maps
            for (int i = 0; i <= numEthnicCodes; i++) {
                factionMaleGivenNames.get(key).put(i, new WeightedMap<>());
                factionMaleGivenNames.get(key).get(i).add(1, maleGivenNames.get(i));
                factionFemaleGivenNames.get(key).put(i, new WeightedMap<>());
                factionFemaleGivenNames.get(key).get(i).add(1, femaleGivenNames.get(i));
                factionSurnames.get(key).add(1, surnames.get(i));
            }
            //endregion No Factions Specified
        } else {
            for (String filename : fileNames) {
                // Determine the key based on the file name
                String key = filename.split("\\.txt")[0];

                // Just check with surnames, as if it has the key then the other two do
                if ((key.length() < 1) || factionSurnames.containsKey(key)) {
                    continue;
                }

                // Initialize Maps
                factionMaleGivenNames.put(key, new HashMap<>());
                factionFemaleGivenNames.put(key, new HashMap<>());
                factionSurnames.put(key, new WeightedMap<>());

                File factionFile = new MegaMekFile(factionsDir, filename).getFile();
                try (InputStream is = new FileInputStream(factionFile);
                     Scanner input = new Scanner(is, "UTF-8")) {

                    while (input.hasNextLine()) {
                        String[] values = input.nextLine().split(",");
                        int ethnicCode = Integer.parseInt(values[0]);

                        // Add information to maps
                        // The weights for ethnic given names for each surname ethnicity will be
                        // stored in the file at i + 2, so that is where we will parse them from
                        for (int i = 0; i <= numEthnicCodes; i++) {
                            factionMaleGivenNames.get(key).put(ethnicCode, new WeightedMap<>());
                            factionMaleGivenNames.get(key).get(ethnicCode).add(
                                    Integer.parseInt(values[i + 2]), maleGivenNames.get(i));
                            factionFemaleGivenNames.get(key).put(ethnicCode, new WeightedMap<>());
                            factionFemaleGivenNames.get(key).get(ethnicCode).add(
                                    Integer.parseInt(values[i + 2]), femaleGivenNames.get(i));
                        }

                        factionSurnames.get(key).add(Integer.parseInt(values[2]),
                                surnames.get(ethnicCode));
                    }
                } catch (IOException fne) {
                    System.err.println("RandomNameGenerator.populateNames(): Could not find '" + factionFile + "'");
                }
            }
        }
        //endregion Faction Files
    }

    private void readNamesFileToMap(Map<Integer, WeightedMap<String>> map, String fileName) {
        int lineNumber = 0;
        File file = new MegaMekFile(Configuration.namesDir(), fileName).getFile();

        try (InputStream is = new FileInputStream(file);
             Scanner input = new Scanner(is, "UTF-8")) {

            while (input.hasNextLine()) {
                lineNumber++;
                String[] values = input.nextLine().split(",");
                if (values.length < 3) {
                    System.err.println("Not enough fields in '" + file.toString() + "' on " + lineNumber);
                    continue;
                }

                map.get(Integer.parseInt(values[2])).add(Integer.parseInt(values[1]), values[0]);
            }
        } catch (IOException e) {
            System.err.println("RandomNameGenerator.populateNames(): Could not find '" + file + "'");
        }
    }

    public synchronized void addInitializationListener(PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(listener);
        if(initialized) {
            // Fire and remove
            pcs.firePropertyChange(PROP_INITIALIZED, false, true);
            pcs.removePropertyChangeListener(listener);
        }
    }

    protected void setInitialized(boolean initialized) {
        pcs.firePropertyChange(PROP_INITIALIZED, this.initialized, this.initialized = initialized);
    }

    public boolean isInitialized() {
        return initialized;
    }

    /**
     * Generate a single random name
     *
     * @return - a string giving the name
     */
    @Deprecated //17-Feb-2020 as part of the addition of gender tracking to MegaMek
    public String generate() {
        return generate(isFemale());
    }

    @Deprecated //24-Feb-2020, this is included to keep current functionality working while other
                //improvements are being finished
    public String generate(boolean isFemale) {
        // this is a total hack, but for now lets assume that
        // if the chosenFaction name contains the word "clan"
        // we should only spit out first names
        return generate(isFemale, chosenFaction.toLowerCase().contains("clan"));
    }

    /**
     * Generate a single random name
     *
     * @return - a string containing the randomly generated name
     */
    public String generate(boolean isFemale, boolean isClan) {
        if ((null != chosenFaction) && (null != factionLast)
                && (null != factionFirst) && (null != firstM)
                && (null != firstF) && (null != last)) {

            Vector<String> ethnicities = factionLast.get(chosenFaction);
            if ((null != ethnicities) && (ethnicities.size() > 0)) {
                String eLast = ethnicities.get(Compute.randomInt(ethnicities.size()));
                // ok now we need to decide on a first name list
                ethnicities = factionFirst.get(chosenFaction).get(eLast);
                if ((null != ethnicities) && (ethnicities.size() > 0)) {
                    String eFirst = ethnicities.get(Compute.randomInt(ethnicities.size()));
                    // ok now we can get the first and last name vectors
                    if (isClan) {
                        eFirst = eLast;
                    }
                    Vector<String> firstNames = isFemale ? firstF.get(eFirst) : firstM.get(eFirst);
                    Vector<String> lastNames = last.get(eLast);
                    if ((null != firstNames) && (null != lastNames)
                            && (firstNames.size() > 0) && (lastNames.size() > 0)) {
                        String first = firstNames.get(Compute.randomInt(firstNames.size()));
                        String last = lastNames.get(Compute.randomInt(lastNames.size()));
                        if (isClan) {
                            return first;
                        }
                        return first + " " + last;
                    }
                }
            }
        }
        return "Unnamed";
    }

    /**
     * Generate a single random name split between a given name and surname
     *
     * @return - a String[] containing the name,
     *              with the given name at String[0]
     *              and the surname at String[1]
     */
    public String[] generateGivenNameSurnameSplit(boolean isFemale, boolean isClan) {
        String[] name = { "", "" };
        if ((chosenFaction != null) && (factionLast != null)
                && (factionFirst != null) && (firstM != null)
                && (firstF != null) && (last != null)) {
            Vector<String> ethnicities = factionLast.get(chosenFaction);
            if ((null != ethnicities) && (ethnicities.size() > 0)) {
                String eLast = ethnicities.get(Compute.randomInt(ethnicities.size()));
                // ok now we need to decide on a first name list
                ethnicities = factionFirst.get(chosenFaction).get(eLast);
                if ((null != ethnicities) && (ethnicities.size() > 0)) {
                    String eFirst = ethnicities.get(Compute.randomInt(ethnicities.size()));
                    // ok now we can get the first and last name vectors
                    if (isClan) {
                        eFirst = eLast;
                    }
                    Vector<String> firstNames = isFemale ? firstF.get(eFirst) : firstM.get(eFirst);
                    Vector<String> lastNames = last.get(eLast);
                    if ((null != firstNames) && (null != lastNames)
                            && (firstNames.size() > 0) && (lastNames.size() > 0)) {
                        name[0] = firstNames.get(Compute.randomInt(firstNames.size()));
                        if (!isClan) {
                            name[1] = lastNames.get(Compute.randomInt(lastNames.size()));
                        }
                    }
                }
            }
        } else {
            name[0] = "Unnamed";
            name[1] = "Person";
        }
        return name;
    }

    public Iterator<String> getFactions() {
        if (null == factionLast) {
            return null;
        }
        return factionLast.keySet().iterator();
    }

    public String getChosenFaction() {
        return chosenFaction;
    }

    public void setChosenFaction(String s) {
        chosenFaction = s;
    }

    public int getPercentFemale() {
        return percentFemale;
    }

    public void setPercentFemale(int i) {
        percentFemale = i;
    }

    /**
     * randomly select gender
     *
     * @return true if female
     */
    public boolean isFemale() {
        return Compute.randomInt(100) < percentFemale;
    }

    public static void initialize() {
        if ((rng != null) && (rng.factionSurnames != null)) {
            return;
        } else if (null == rng) {
            rng = new RandomNameGenerator();
        }

        if (!rng.initialized && !rng.initializing) {
            rng.loader = new Thread(() -> {
                rng.initializing = true;
                rng.populateNames();
                if (rng != null) {
                    rng.setInitialized(true);
                }
            }, "Random Name Generator name populator");
            rng.loader.setPriority(Thread.NORM_PRIORITY - 1);
            rng.loader.start();
        }
    }

    public static RandomNameGenerator getInstance() {
        if (rng == null) {
            initialize();
        }
        return rng;
    }

    // Deactivated methods
    public void dispose() {}
    public void clear() {}
}
