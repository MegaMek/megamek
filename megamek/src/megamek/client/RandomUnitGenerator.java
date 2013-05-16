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
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;
import java.util.Vector;

import megamek.common.Compute;
import megamek.common.MechSummary;
import megamek.common.MechSummaryCache;

/**
 * Author: Jay Lawson This class sets up a random unit generator that can then
 * be used to read in user-created input files of random assignment tables
 * 
 * Files should be located in data/rat/ All files should comma-delimited text
 * files.
 * 
 * The first line of the file should contain the title of the RAT The second
 * line of the file should give the unit type number corresponding to
 * UnitType.java The remaining lines should be comma split. The first field
 * should give the frequency of that unit and the second line should give the
 * name of that unit written as <Model> <Chassis> Comment lines can also be
 * added with "#"
 * 
 */

public class RandomUnitGenerator implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 5765118329881301375L;

    // The RATs are stored in a hashmap of string vectors. The keys are the RAT
    // names
    // and the vectors just contain the unit names listed a number of times
    // equal to
    // the frequency
    Map<String, Vector<String>> rats;
    private static RandomUnitGenerator rug;
    private static boolean interrupted = false;
    private static boolean dispose = false;
    private Thread loader;
    private boolean initialized;
    private boolean initializing;

    /**
     * Plain old data class used to represent nodes in a Random Assignment Table
     * tree. RATs are grouped into categories based on directory structure, and
     * will be displayed hierarchically to the user.
     */
    public static class RatTreeNode implements Comparable<RatTreeNode> {
        public RatTreeNode(String name) {
            this.name = name;
            children = new Vector<RatTreeNode>();
        }

        public int compareTo(RatTreeNode rtn) {
            return name.compareTo(rtn.name);
        }

        public String name;
        public Vector<RatTreeNode> children;
    }

    private RatTreeNode ratTree;
    private RatTreeNode currentNode;

    private String chosenRAT;

    public RandomUnitGenerator() {
        chosenRAT = "TW Heavy Mech (Kurita)";
    }

    public synchronized void populateUnits() {
        rats = new HashMap<String, Vector<String>>();
        ratTree = new RatTreeNode("Random Assignment Tables");

        File dir = new File("./data/rat/");
        loadRatsFromDirectory(dir);
        if (!interrupted) {
            rug.initialized = true;
        }

        if (dispose) {
            clear();
            dispose = false;
        }
    }

    private void loadRatsFromDirectory(File dir) {

        if (interrupted) {
            return;
        }

        if (null == dir) {
            return;
        }

        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }

        RatTreeNode oldParentNode = null;
        if (null == currentNode) {
            currentNode = ratTree;
            oldParentNode = currentNode;
        }

        Scanner input = null;

        for (int i = 0; i < files.length; i++) {
            // Check to see if we've been interrupted
            if (interrupted) {
                return;
            }

            // READ IN RATS
            File file = files[i];
            if (file.isDirectory()) {
                if (file.getName().toLowerCase().equals("_svn")
                        || file.getName().toLowerCase().equals(".svn")) {
                    // This is a Subversion work directory. Lets ignore it.
                    continue;
                }

                RatTreeNode newNode = new RatTreeNode(file.getName());
                oldParentNode = currentNode;
                currentNode = newNode;

                // Add non-root nodes to the tree.
                if (ratTree != currentNode) {
                    oldParentNode.children.add(currentNode);
                }

                // recursion is fun
                loadRatsFromDirectory(file);

                // Prune empty nodes (this removes the "Unofficial" place
                // holder)
                if (currentNode.children.size() == 0) {
                    oldParentNode.children.remove(currentNode);
                }

                currentNode = oldParentNode;
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
                    if (interrupted) {
                        return;
                    }
                    String line = input.nextLine();
                    if (line.startsWith("#")) {
                        continue;
                    }
                    linen++;
                    if (linen == 1) {
                        key = line;
                    } else {
                        String[] values = line.split(",");
                        if (values.length < 2) {
                            System.err.println("Not enough fields in "
                                    + file.getName() + " on " + linen);
                            continue;
                        }
                        String name = values[0];
                        int weight = 0;
                        try {
                            weight = Integer.parseInt(values[1].trim());
                        } catch (NumberFormatException nef) {
                            System.err
                                    .println("the frequency field could not be interpreted on line "
                                            + linen + " of " + file.getName());
                            continue;
                        }
                        MechSummary unit = MechSummaryCache.getInstance()
                                .getMech(name);
                        if (null == unit) {
                            System.err.println("The unit " + name
                                    + " could not be found in the " + key
                                    + " RAT");
                        } else {
                            int j = 0;
                            while (j < weight) {
                                v.add(name);
                                j++;
                            }
                        }
                    }
                }
                if (v.size() > 0) {
                    rats.put(key, v);
                    if (null != currentNode) {
                        currentNode.children.add(new RatTreeNode(key));
                    }
                }
            } catch (FileNotFoundException fne) {
                System.err.println("Unable to find " + file.getName());
            }
        }

        Collections.sort(currentNode.children);
    }

    /**
     * Generate a single random name
     * 
     * @return - a string giving the name
     */
    public ArrayList<MechSummary> generate(int n) {
        ArrayList<MechSummary> units = new ArrayList<MechSummary>();

        if (null != rats) {
            Vector<String> rat = rats.get(chosenRAT);
            if ((null != rat) && (rat.size() > 0)) {
                for (int i = 0; i < n; i++) {
                    String name = rat.get(Compute.randomInt(rat.size()));
                    MechSummary unit = MechSummaryCache.getInstance().getMech(
                            name);
                    if (null != unit) {
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
        if (null == rats) {
            return null;
        }
        return rats.keySet().iterator();
    }

    public RatTreeNode getRatTree() {
        return ratTree;
    }

    public void dispose() {
        interrupted = true;
        dispose = true;
        if (initialized){
            clear();
        }
    }

    public void clear() {
        rug = null;
        rats = null;
        ratTree = null;
        initialized = false;
        initializing = false;
    }

    public static synchronized RandomUnitGenerator getInstance() {
        if (null == rug) {
            rug = new RandomUnitGenerator();
        }
        if (!rug.initialized && !rug.initializing) {
            rug.initializing = true;
            interrupted = false;
            dispose = false;
            rug.loader = new Thread(new Runnable() {
                public void run() {
                    rug.populateUnits();

                }
            }, "Random Unit Generator unit populator");
            rug.loader.setPriority(Thread.NORM_PRIORITY - 1);
            rug.loader.start();
        }
        return rug;
    }

    public boolean isInitialized() {
        return initialized;
    }

}
