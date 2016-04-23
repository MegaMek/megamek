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
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

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

        @Override
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
        
        // Give the MSC some time to initialize
        MechSummaryCache msc = MechSummaryCache.getInstance();
        long waitLimit = System.currentTimeMillis() + 3000; /* 3 seconds */
        while( !interrupted && !msc.isInitialized() && waitLimit > System.currentTimeMillis() ) {
            try {
                Thread.sleep(50);
            } catch(InterruptedException e) {
                // Ignore
            }
        }

        loadRatsFromDirectory(Configuration.armyTablesDir(), msc);
        cleanupNode(ratTree);
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
    
    private void readRat(InputStream is, RatTreeNode node, String fileName, MechSummaryCache msc) throws IOException {
        try(@SuppressWarnings("resource")
        BufferedReader reader
                = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")))) { //$NON-NLS-1$
            int lineNumber = 0;
            String key = "Huh"; //$NON-NLS-1$
            float totalWeight = 0.0f;
            RatEntry re = new RatEntry();
            String line = null;
            while (null != (line = reader.readLine())) {
                if (interrupted) {
                    return;
                }
                if (line.startsWith("#")) { //$NON-NLS-1$
                    continue;
                }
                lineNumber++;
                if (lineNumber == 1) {
                    key = line;
                } else {
                    String[] values = line.split(","); //$NON-NLS-1$
                    if (values.length < 2) {
                        System.err.println(String.format("Not enough fields in %s on %d", //$NON-NLS-1$
                            fileName, lineNumber));
                        continue;
                    }
                    String name = values[0];
                    float weight;
                    try {
                        weight = Integer.parseInt(values[1].trim());
                    } catch (NumberFormatException nef) {
                        System.err.println(
                            String.format("The frequency field could not be interpreted on line %d of %s", //$NON-NLS-1$
                                lineNumber, fileName));
                        continue;
                    }
                    if( weight <= 0.0f ) {
                        System.err.println(
                            String.format("The frequency field is zero or negative (%d) on line %d of %s", //$NON-NLS-1$
                                weight, lineNumber, fileName));
                        continue;
                    }
    
                    // The @ symbol denotes a reference to another RAT rather than a unit.
                    if (!name.startsWith("@") && (null == msc.getMech(name))) { //$NON-NLS-1$
                        System.err.println(
                            String.format("The unit %s could not be found in the %s RAT (%s)", //$NON-NLS-1$
                                name, key, fileName));
                        continue;
                    }
                    re.getUnits().add(name.intern());
                    re.getWeights().add(weight);
                    totalWeight += weight;
                }
            }
            
            // Calculate total weights
            if (re.getUnits().size() > 0) {
                for (int i = 0; i < re.getWeights().size(); i++) {
                    re.getWeights().set(i, re.getWeights().get(i) / totalWeight);
                }  
                rats.put(key, re);
                if (null != node) {
                    node.children.add(new RatTreeNode(key));
                }
            }
        }

    }
    
    private RatTreeNode getNodeByPath(RatTreeNode root, String path) {
        RatTreeNode result = root;
        String[] pathElements = path.split("/", -1); //$NON-NLS-1$
        for( int i = 0; i < pathElements.length - 1; ++ i ) {
            if( pathElements[i].length() == 0 ) {
                continue;
            }
            RatTreeNode subNode = null;
            for( RatTreeNode rtn : result.children ) {
                if( rtn.name.equals(pathElements[i]) ) {
                    subNode = rtn;
                    break;
                }
            }
            if( null == subNode ) {
                subNode = new RatTreeNode(pathElements[i]);
                result.children.addElement(subNode);
            }
            result = subNode;
        }
        return result;
    }
    
    private void cleanupNode(RatTreeNode node) {
        for(RatTreeNode child : node.children) {
               cleanupNode(child);
        }
        Collections.sort(node.children);
    }
    
    private void loadRatsFromDirectory(File dir, MechSummaryCache msc) {
        loadRatsFromDirectory(dir, msc, ratTree);
    }
    
    private void loadRatsFromDirectory(File dir, MechSummaryCache msc, RatTreeNode node) {
        if (interrupted) {
            return;
        }

        if ((null == dir) || (null == node)) {
            return;
        }

        File[] files = dir.listFiles();
        if (null == files) {
            return;
        }

        for (File ratFile : files) {
            // Check to see if we've been interrupted
            if (interrupted) {
                return;
            }
            String ratFileNameLC = ratFile.getName().toLowerCase(Locale.ROOT);

            if (ratFileNameLC.equals("_svn") || ratFileNameLC.equals(".svn")) { //$NON-NLS-1$ //$NON-NLS-2$
                // This is a Subversion work directory. Lets ignore it.
                continue;
            }
            
            // READ IN RATS
            if (ratFile.isDirectory()) {
                RatTreeNode newNode = getNodeByPath(node, ratFile.getName() + "/"); //$NON-NLS-1$

                // recursion is fun
                loadRatsFromDirectory(ratFile, msc, newNode);

                // Prune empty nodes (this removes the "Unofficial" place holder)
                if (newNode.children.size() == 0) {
                    node.children.remove(newNode);
                }
                continue;
            }
            if( ratFileNameLC.endsWith(".zip") ) { //$NON-NLS-1$
                try(ZipFile zipFile = new ZipFile(ratFile)) {
                    Enumeration<? extends ZipEntry> entries = zipFile.entries();
                    while(entries.hasMoreElements()) {
                        ZipEntry entry = entries.nextElement();
                        String entryName = entry.getName();
                        if(!entry.isDirectory() && entryName.toLowerCase(Locale.ROOT).endsWith(".txt")) //$NON-NLS-1$
                        {
                            RatTreeNode subNode = getNodeByPath(node, entryName);
                            try(InputStream zis = zipFile.getInputStream(entry))
                            {
                                readRat(zis, subNode, ratFile.getName() + ":" + entryName, msc); //$NON-NLS-1$
                            }
                        }
                    }
                } catch(IOException e) {
                    System.err.println(String.format("Unable to load %s", ratFile.getName())); //$NON-NLS-1$
                }
            }
            if (!ratFileNameLC.endsWith(".txt")) { //$NON-NLS-1$
                continue;
            }
            try(InputStream ratInputStream = new FileInputStream(ratFile)) {
                readRat(ratInputStream, node, ratFile.getName(), msc);
            } catch(IOException e) {
                System.err.println(String.format("Unable to load %s", ratFile.getName())); //$NON-NLS-1$
            }
        }
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
                    long start = System.currentTimeMillis();
                    rug.populateUnits();
                    long end = System.currentTimeMillis();
                    System.out.println("Loaded Rats in: " + (end - start)
                            + "ms.");
                    System.out.flush();
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
