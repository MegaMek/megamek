/*
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;
import java.util.Vector;

import megamek.common.Compute;
import megamek.common.MechSummary;
import megamek.common.MechSummaryCache;

/**
 * Author: Jay Lawson
 * This class sets up a random unit generator that can then be used
 * to read in user-created input files of random assignment tables
 *
 * Files should be located in data/rat/
 * All files should comma-delimited text files.
 *
 * The first line of the file should contain the title of the RAT
 * The second line of the file should give the unit type number corresponding to UnitType.java
 * The remaining lines should be comma split. The first field should give the frequency of that unit
 * and the second line should give the name of that unit written as <Model> <Chassis>
 * Comment lines can also be added with "#"
 *
 */

public class RandomUnitGenerator implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 5765118329881301375L;

    //The RATs are stored in a hashmap of string vectors. The keys are the RAT names
    //and the vectors just contain the unit names listed a number of times equal to
    //the frequency
    Map<String, Vector<String>> rats;

    private String chosenRAT;

    public RandomUnitGenerator() {
        chosenRAT = "TW Heavy Mech (Kurita)";
    }

    public void populateUnits() {
        rats = new HashMap<String, Vector<String>>();
        File dir = new File("./data/rat/");
        loadRatsFromDirectory(dir);
    }

    private void loadRatsFromDirectory(File dir) {
        if(null == dir) {
            return;
        }

        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }

        Scanner input = null;

        for(int i = 0; i < files.length; i++) {
            //READ IN RATS
            File file = files[i];
            if (file.isDirectory()) {
                if (file.getName().toLowerCase().equals("_svn") || file.getName().toLowerCase().equals(".svn")) {
                    // This is a Subversion work directory. Lets ignore it.
                    continue;
                }
                // recursion is fun
                loadRatsFromDirectory(file);
                continue;
            }
            if (!file.getName().toLowerCase().endsWith(".txt")) {
                continue;
            }
            try {
                FileInputStream ratst = new FileInputStream(file);
                input = new Scanner(ratst, "UTF-8");
                int linen = 0;
                String key = "Huh";
                Vector<String> v = new Vector<String>();
                while (input.hasNextLine()) {
                    String line = input.nextLine();
                    if(line.startsWith("#")) {
                        continue;
                    }
                    linen++;
                    if(linen==1) {
                        key = line;
                    }
                    else {
                        String[] values = line.split(",");
                        if(values.length < 2) {
                            System.err.println("Not enough fields in " + file.getName() + " on " + linen);
                            continue;
                        }
                        String name = values[0];
                        int weight = 0;
                        try {
                            weight = Integer.parseInt(values[1].trim());
                        } catch (NumberFormatException nef) {
                            System.err.println("the frequency field could not be interpreted on line "  + linen + " of " + file.getName());
                            continue;
                        }
                        MechSummary unit = MechSummaryCache.getInstance().getMech(name);
                        if(null == unit) {
                            System.err.println("The unit " + name + " could not be found in the " + key + " RAT");
                        } else {
                            int j = 0;
                            while(j < weight) {
                                v.add(name);
                                j++;
                            }
                        }
                    }
                }
                if(v.size() > 0) {
                    rats.put(key, v);
                }
            } catch (FileNotFoundException fne) {
                System.err.println("Unable to find " + file.getName());
            }
        }
    }

    /**
     * Generate a single random name
     * @return - a string giving the name
     */
    public ArrayList<MechSummary> generate(int n) {
        ArrayList<MechSummary> units = new ArrayList<MechSummary>();

        if(null != rats) {
            Vector<String> rat = rats.get(chosenRAT);
            if((null != rat) && (rat.size() > 0)) {
                for(int i = 0; i < n; i++) {
                    String name =  rat.get(Compute.randomInt(rat.size()));
                    MechSummary unit = MechSummaryCache.getInstance().getMech(name);
                    if(null != unit) {
                        units.add(unit);
                    }
                }
            }
        }
        return units;
    }

    public String getChosenRAT() {
        return chosenRAT;
    }

    public void setChosenRAT(String s) {
        chosenRAT = s;
    }

    public Iterator<String> getRatList() {
        if(null == rats) {
            return null;
        }
        return rats.keySet().iterator();
    }

    public void clear() {
        rats = null;
    }

}
