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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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

import megamek.common.Configuration;
import megamek.common.MechSummary;
import megamek.common.MechSummaryCache;

/**
 * This class sets up a random unit generator that can then
 * be used to read in user-created input files of random assignment tables
 * <p>
 * Files must be located in in the directory defined by {@link Configuration#armyTablesDir()}.
 * All files should comma-delimited text files.
 * </p>
 * <p>
 * The first line of the file should contain the title of the RAT The second
 * line of the file should give the unit type number corresponding to
 * UnitType.java The remaining lines should be comma split. The first field
 * should give the frequency of that unit and the second line should give the
 * name of that unit written as <Model> <Chassis> Comment lines can also be
 * added with "#"
 * </p>
 * 
 * @author Jay Lawson
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
    private Map<String, RatEntry> rats;
    private static RandomUnitGenerator rug;
    private static boolean interrupted = false;
    private static boolean dispose = false;
    private Thread loader;
    private boolean initialized;
    private boolean initializing;
    
    private ArrayList<ActionListener> listeners;
    
    /**
     * Plain old data class used to represent nodes in a Random Assignment Table
     * tree. RATs are grouped into categories based on directory structure, and
     * will be displayed hierarchically to the user.
     */
    public static class RatTreeNode implements Comparable<RatTreeNode> {
        public RatTreeNode(String name) {
            this.name = name;
            children = new Vector<>();
        }

        public int compareTo(RatTreeNode rtn) {
            return name.compareTo(rtn.name);
        }

        public String name;
        public Vector<RatTreeNode> children;
    }
    
    /**
     * Keeps track of a RAT entry, stores the name of a unit in the RAT, and 
     * its change of appearing (weight).
     * 
     * @author arlith
     *
     */
    protected class RatEntry {
        private Vector<String> units;
        private Vector<Float> weights;
        
        RatEntry(){
            setUnits(new Vector<String>());
            setWeights(new Vector<Float>());
        }

        public Vector<String> getUnits() {
            return units;
        }

        public void setUnits(Vector<String> units) {
            this.units = units;
        }

        public Vector<Float> getWeights() {
            return weights;
        }

        public void setWeights(Vector<Float> weights) {
            this.weights = weights;
        }
    }

    private RatTreeNode ratTree;
    private RatTreeNode currentNode;

    private String chosenRAT;

    public RandomUnitGenerator() {
        chosenRAT = "TW Heavy Mech (Kurita)";
        listeners = new ArrayList<>();
    }

    protected void initRats() {
        rats = new HashMap<>();
    }

    protected void initRatTree() {
        ratTree = new RatTreeNode("Random Assignment Tables");
    }

    public synchronized void populateUnits() {
        initRats();
        initRatTree();

        loadRatsFromDirectory(Configuration.armyTablesDir());
        if (!interrupted) {
            rug.initialized = true;
            rug.notifyListenersOfInitialization();
        }

        if (dispose) {
            clear();
            dispose = false;
        }
    }
    
    public synchronized void registerListener(ActionListener l){
        listeners.add(l);
    }

    @SuppressWarnings("UnusedDeclaration")
    // todo Not being used.  Is this really needed?
    public synchronized void removeListener(ActionListener l){
        listeners.remove(l);
    }
    
    /**
     * Notifies all the listeners that initialization is finished
     */
    public void notifyListenersOfInitialization(){
        if (initialized){
            for (ActionListener l : listeners){
                l.actionPerformed(new ActionEvent(
                        this,ActionEvent.ACTION_PERFORMED,"rugInitialized"));
            }
        }
    }

    protected void addRat(String ratName, RatEntry ratEntry) {
        rats.put(ratName, ratEntry);
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

        RatTreeNode oldParentNode;
        if (null == currentNode) {
            currentNode = ratTree;
        }

        Scanner input;

        for (File ratFile : files) {
            // Check to see if we've been interrupted
            if (interrupted) {
                return;
            }

            // READ IN RATS
            if (ratFile.isDirectory()) {
                if (ratFile.getName().toLowerCase().equals("_svn") || ratFile.getName().toLowerCase().equals(".svn")) {
                    // This is a Subversion work directory. Lets ignore it.
                    continue;
                }

                RatTreeNode newNode = new RatTreeNode(ratFile.getName());
                oldParentNode = currentNode;
                currentNode = newNode;

                // Add non-root nodes to the tree.
                if (ratTree != currentNode) {
                    oldParentNode.children.add(currentNode);
                }

                // recursion is fun
                loadRatsFromDirectory(ratFile);

                // Prune empty nodes (this removes the "Unofficial" place holder)
                if (currentNode.children.size() == 0) {
                    oldParentNode.children.remove(currentNode);
                }

                currentNode = oldParentNode;
                continue;
            }
            if (!ratFile.getName().toLowerCase().endsWith(".txt")) {
                continue;
            }
            FileInputStream ratInputStream = null;
            try {
                ratInputStream = new FileInputStream(ratFile);
                input = new Scanner(ratInputStream, "UTF-8");
                int lineNumber = 0;
                String key = "Huh";
                RatEntry re = new RatEntry();
                while (input.hasNextLine()) {
                    if (interrupted) {
                        return;
                    }
                    String line = input.nextLine();
                    if (line.startsWith("#")) {
                        continue;
                    }
                    lineNumber++;
                    if (lineNumber == 1) {
                        key = line;
                    } else {
                        String[] values = line.split(",");
                        if (values.length < 2) {
                            System.err.println("Not enough fields in " + ratFile.getName() + " on " + lineNumber);
                            continue;
                        }
                        String name = values[0];
                        float weight;
                        try {
                            weight = Integer.parseInt(values[1].trim());
                        } catch (NumberFormatException nef) {
                            System.err.println("the frequency field could not be interpreted on line "
                                               + lineNumber + " of " + ratFile.getName());
                            continue;
                        }

                        // The @ symbol denotes a reference to another RAT rather than a unit.
                        MechSummary unit = null;
                        if (!name.startsWith("@")) {
                            unit = MechSummaryCache.getInstance().getMech(name);
                        }
                        if ((null == unit) && !name.startsWith("@")) {
                            System.err.println("The unit " + name + " could not be found in the " + key + " RAT");
                        } else {
                            re.getUnits().add(name);
                            re.getWeights().add(weight);
                        }
                    }
                }
                if (re.getUnits().size() > 0) {
                    float sum = 0;
                    for (int i = 0; i < re.getWeights().size(); i++) {
                        sum += re.getWeights().get(i);
                    }
                    for (int i = 0; i < re.getWeights().size(); i++) {
                        re.getWeights().set(i, re.getWeights().get(i) / sum);
                    }  
                    rats.put(key, re);
                    if (null != currentNode) {
                        currentNode.children.add(new RatTreeNode(key));
                    }
                }
            } catch (FileNotFoundException fne) {
                System.err.println("Unable to find " + ratFile.getName());
            } finally {
                if (ratInputStream != null) {
                    try {
                        ratInputStream.close();
                    } catch (Exception e){
                        // Nothing to do...
                    }
                }
            }
        }

        Collections.sort(currentNode.children);
    }

    /**
     * Generate a list of units from the RAT
     * 
     * @return - a string giving the name
     */
    public ArrayList<MechSummary> generate(int numRolls, String ratName) {
        ArrayList<MechSummary> units = new ArrayList<>();

        try {
            Map<String, RatEntry> ratMap = getRatMap();
            if (null != ratMap) {
                RatEntry re = ratMap.get(ratName);
                if ((null != re) && (re.getUnits().size() > 0)) {
                    for (int roll = 0; roll < numRolls; roll++) {
                        double rand = getRandom();
                        int i = 0;
                        while (i < re.getWeights().size() && rand > re.getWeights().get(i)) {
                            rand -= re.getWeights().get(i);
                            i++;
                        }
                        String name = re.getUnits().get(i);

                        // If this is a RAT reference, roll the unit on the referenced RAT.
                        if (name.startsWith("@")) {
                            units.addAll(generate(1, name.replaceFirst("@", "")));
                            continue;
                        }

                        MechSummary unit = getMechByName(name);
                        if (null != unit) {
                            units.add(unit);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return units;
    }

    protected MechSummary getMechByName(String name) {
        return MechSummaryCache.getInstance().getMech(name);
    }

    protected double getRandom() {
        return Math.random();
    }

    public ArrayList<MechSummary> generate(int numRolls) {
        return generate(numRolls, getChosenRAT());
    }

    public Map<String, RatEntry> getRatMap() {
        return new HashMap<>(rats);
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
