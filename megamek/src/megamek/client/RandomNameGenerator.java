/*
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 * Copyright © 2013 Edward Cullen (eddy@obsessedcomputers.co.uk)
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
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;
import java.util.Vector;

import megamek.common.Compute;
import megamek.common.Configuration;

/** 
 * This class sets up a random name generator that can then
 * be used to generate random pilot names. it will have a couple different
 * settings and flexible input files
 * <p>
 * Files are located in {@link Configuration#namesDir()}. All files are comma-delimited text files.
 * </p>
 * <p>
 * The masterancestry.txt file shows the correspondence between the different ethnic names and their numeric
 * code in the database. This file is currently not actually read in by MM, but is provided as a reference. The same
 * numeric code must be used across all of the files listed below. Currently the numeric codes must be listed in exact
 * sequential order (i.e. no skipping numbers) for the program to work correctly.</p>
 * <p>
 * The name database is located in three files: firstname_males.txt, firstname_females.txt, and surnames.txt.
 * There ar three comma-delimited fields in each of these data files: fld1,fld2,fld3
 * <ul>
 * <li>fld1 - The name itself, either a male/female first name or a surname.</li>
 * <li>fld2 - a frequency weight to account for some names being more common than others. Currently this is
 *           not being used.</li>
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
 * <li>fld4-fldn - These fields identify the relative frequency of first names from an ethnic group given the surname
 *                 listed in fld1.
 * </ul>
 * </p>
 * @author Jay Lawson
 */
public class RandomNameGenerator implements Serializable {
    private static final String PROP_INITIALIZED = "initialized"; //$NON-NLS-1$

    /** Default directory containing the faction-specific name files. */
    private static final String DIR_NAME_FACTIONS = "factions"; //$NON-NLS-1$

    /** Default filename for the list of male first names. */
    private static final String FILENAME_FIRSTNAMES_MALE = "firstnames_male.txt"; //$NON-NLS-1$
    
    /** Default filename for the list of female first names. */
    private static final String FILENAME_FIRSTNAMES_FEMALE = "firstnames_female.txt"; //$NON-NLS-1$

    /** Default filename for the list of surnames names. */
    private static final String FILENAME_SURNAMES = "surnames.txt"; //$NON-NLS-1$

    private static final long serialVersionUID = 5765118329881301375L;

    private static RandomNameGenerator rng;

    Map<String, Vector<String>> firstm;
    Map<String, Vector<String>> firstf;
    Map<String, Vector<String>> last;
    Map<String, Vector<String>> factionLast;
    Map<String, Map<String, Vector<String>>> factionFirst;

    private int percentFemale;
    private String chosenFaction;
    private Thread loader;
    private boolean initialized;
    private boolean initializing;

    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    public RandomNameGenerator() {
        percentFemale = 50;
        chosenFaction = "General"; //$NON-NLS-1$
    }

    public void populateNames() {
        // TODO: how do I weight name vectors by frequency, without making them
        // gargantuan?
        if (null == firstm) {
            firstm = new HashMap<String, Vector<String>>();
        }
        if (null == firstf) {
            firstf = new HashMap<String, Vector<String>>();
        }
        if (null == last) {
            last = new HashMap<String, Vector<String>>();
        }
        if (null == factionLast) {
            factionLast = new HashMap<String, Vector<String>>();
        }
        if (null == factionFirst) {
            factionFirst = new HashMap<String, Map<String, Vector<String>>>();
        }

        // READ IN MALE FIRST NAMES
        File male_firstnames_path = new File(Configuration.namesDir(), FILENAME_FIRSTNAMES_MALE);
        try(Scanner input = new Scanner(new FileInputStream(male_firstnames_path), "UTF-8")) { //$NON-NLS-1$
            int linen = 0;
            while (input.hasNextLine()) {
                String line = input.nextLine();
                linen++;
                String[] values = line.split(","); //$NON-NLS-1$
                if (values.length < 3) {
                    System.err.println(
                            "Not enough fields in '" + male_firstnames_path.toString() + "' on " + linen //$NON-NLS-1$ //$NON-NLS-2$
                    );
                    continue;
                }
                String name = values[0];
                int weight = Integer.parseInt(values[1]);
                String key = values[2];
                int i = 0;
                if (!firstm.containsKey(key)) {
                    Vector<String> v = new Vector<String>();
                    while (i < weight) {
                        v.add(name);
                        i++;
                    }
                    firstm.put(key, v);
                } else {
                    while (i < weight) {
                        firstm.get(key).add(name);
                        i++;
                    }
                }
            }
        } catch (IOException fne) {
            System.err.println("RandomNameGenerator.populateNames(): Could not find '" + male_firstnames_path + "'"); //$NON-NLS-1$ //$NON-NLS-2$
        }

        // READ IN FEMALE FIRST NAMES
        File female_firstnames_path = new File(Configuration.namesDir(), FILENAME_FIRSTNAMES_FEMALE);
        try(Scanner input = new Scanner(new FileInputStream(female_firstnames_path), "UTF-8")) { //$NON-NLS-1$
            int linen = 0;
            while (input.hasNextLine()) {
                String line = input.nextLine();
                linen++;
                String[] values = line.split(","); //$NON-NLS-1$
                if (values.length < 3) {
                    System.err.println(
                            "RandomNameGenerator.populateNames(): Not enough fields in '" + female_firstnames_path.toString() + "' on " + linen //$NON-NLS-1$ //$NON-NLS-2$
                    );
                    continue;
                }
                String name = values[0];
                int weight = Integer.parseInt(values[1]);
                String key = values[2];
                int i = 0;
                if (!firstf.containsKey(key)) {
                    Vector<String> v = new Vector<String>();
                    while (i < weight) {
                        v.add(name);
                        i++;
                    }
                    firstf.put(key, v);
                } else {
                    while (i < weight) {
                        firstf.get(key).add(name);
                        i++;
                    }
                }
            }
        } catch (IOException fne) {
            System.err.println("RandomNameGenerator.populateNames(): Could not find '" + female_firstnames_path + "'"); //$NON-NLS-1$ //$NON-NLS-2$
        }

        // READ IN SURNAMES
        File surnames_path = new File(Configuration.namesDir(), FILENAME_SURNAMES);
        try(Scanner input = new Scanner(new FileInputStream(surnames_path), "UTF-8")) { //$NON-NLS-1$
            int linen = 0;
            while (input.hasNextLine()) {
                String line = input.nextLine();
                linen++;
                String[] values = line.split(","); //$NON-NLS-1$
                if (values.length < 3) {
                    System.err.println(
                            "Not enough fields in '" + surnames_path + "' on " + linen //$NON-NLS-1$ //$NON-NLS-2$
                    );
                    continue;
                }
                String name = values[0];
                int weight = Integer.parseInt(values[1]);
                String key = values[2];
                int i = 0;
                if (!last.containsKey(key)) {
                    Vector<String> v = new Vector<String>();
                    while (i < weight) {
                        v.add(name);
                        i++;
                    }
                    last.put(key, v);
                } else {
                    while (i < weight) {
                        last.get(key).add(name);
                        i++;
                    }
                }
            }
        } catch (IOException fne) {
            System.err.println("RandomNameGenerator.populateNames(): Could not find '" + surnames_path + "'"); //$NON-NLS-1$ //$NON-NLS-2$
        }

        // READ IN FACTION FILES
        // all faction files should be in the faction directory
        File factions_dir_path = new File(Configuration.namesDir(), DIR_NAME_FACTIONS);
        String[] filenames = factions_dir_path.list();
        if (null == filenames) {
            return;
        }
        for (int filen = 0; filen < filenames.length; filen++) {
            String filename = filenames[filen];
            String key = filename.split("\\.txt")[0]; //$NON-NLS-1$
            if ((key.length() < 1) || factionLast.containsKey(key)) {
                continue;
            }
            factionLast.put(key, new Vector<String>());
            factionFirst.put(key, new HashMap<String, Vector<String>>());
            File ff = new File(factions_dir_path, filename);
            try(Scanner factionInput = new Scanner(new FileInputStream(ff), "UTF-8")) { //$NON-NLS-1$
                Map<String, Vector<String>> hash = new HashMap<String, Vector<String>>();
                while (factionInput.hasNextLine()) {
                    String line = factionInput.nextLine();
                    String[] values = line.split(","); //$NON-NLS-1$
                    String ethnicity = values[0];
                    int freq = Integer.parseInt(values[2]);
                    while (freq > 0) {
                        factionLast.get(key).add(ethnicity);
                        freq--;
                    }
                    Vector<String> v = new Vector<String>();
                    for (int i = 3; i < values.length; i++) {
                        freq = Integer.parseInt(values[i]);
                        // TODO: damm - I don't have the integer codes for ethnicity
                        // here, for now just assume they are the
                        // same as i-2
                        while (freq > 0) {
                            v.add(Integer.toString(i - 2));
                            freq--;
                        }
                    }
                    hash.put(ethnicity, v);
                }
                factionFirst.put(key, hash);
            } catch (IOException fne) {
                System.err.println("RandomNameGenerator.populateNames(): Could not find '" + ff + "'"); //$NON-NLS-1$ //$NON-NLS-2$
                continue;
            }
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
    public String generate() {
        return generate(isFemale());
    }

    public String generate(boolean isFemale) {
        if ((null != chosenFaction) && (null != factionLast)
                && (null != factionFirst) && (null != firstm)
                && (null != firstf) && (null != last)) {
            // this is a total hack, but for now lets assume that
            // if the chosenFaction name contains the word "clan"
            // we should only spit out first names
            boolean isClan = chosenFaction.toLowerCase().contains("clan");

            Vector<String> ethnicities = factionLast.get(chosenFaction);
            if ((null != ethnicities) && (ethnicities.size() > 0)) {
                String eLast = ethnicities.get(Compute.randomInt(ethnicities
                        .size()));
                // ok now we need to decide on a first name list
                ethnicities = factionFirst.get(chosenFaction).get(eLast);
                if ((null != ethnicities) && (ethnicities.size() > 0)) {
                    String eFirst = ethnicities.get(Compute
                            .randomInt(ethnicities.size()));
                    // ok now we can get the first and last name vectors
                    if (isClan) {
                        eFirst = eLast;
                    }
                    Vector<String> fnames = firstm.get(eFirst);
                    if (isFemale) {
                        fnames = firstf.get(eFirst);
                    }
                    Vector<String> lnames = last.get(eLast);
                    if ((null != fnames) && (null != lnames)
                            && (fnames.size() > 0) && (lnames.size() > 0)) {
                        String first = fnames.get(Compute.randomInt(fnames
                                .size()));
                        String last = lnames.get(Compute.randomInt(lnames
                                .size()));
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

    public void setPerentFemale(int i) {
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
        if ((rng != null) && (rng.last != null)) {
            return;
        }
        if (null == rng) {
            rng = new RandomNameGenerator();
        }
        if (!rng.initialized && !rng.initializing) {
            rng.loader = new Thread(new Runnable() {
                public void run() {
                    rng.initializing = true;
                    rng.populateNames();
                    if (rng != null) {
                        rng.setInitialized(true);
                    }
                }
            }, "Random Name Generator name populator");
            rng.loader.setPriority(Thread.NORM_PRIORITY - 1);
            rng.loader.start();
        }
    }

    public static RandomNameGenerator getInstance() {
        if (null == rng) {
            initialize();
        }
        return rng;
    }
    
    // Deactivated methods
    public void dispose() {}
    public void clear() {}
}
