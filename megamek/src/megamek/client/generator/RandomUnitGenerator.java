/*
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 * Copyright © 2013 Edward Cullen (eddy@obsessedcomputers.co.uk)
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved
 *
 * This file is part of MegaMek.
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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import megamek.common.Configuration;
import megamek.common.MekSummary;
import megamek.common.MekSummaryCache;
import megamek.logging.MMLogger;

/**
 * This class sets up a random unit generator that can then be used to read in
 * user-created input files of random assignment tables
 * <p>
 * Files must be located in in the directory defined by
 * {@link Configuration#armyTablesDir()}. All files should comma-delimited text
 * files.
 * </p>
 * <p>
 * The first line of the file should contain the title of the RAT The second
 * line of the file should give the unit type number corresponding to
 * UnitType.java The remaining lines should be comma split. The first field
 * should give the frequency of that unit and the second line should give the
 * name of that unit written as { Model } { Chassis }. Comment lines can also be
 * added with "#".
 * </p>
 *
 * @author Jay Lawson
 */
public class RandomUnitGenerator implements Serializable {
    private static final MMLogger logger = MMLogger.create(RandomUnitGenerator.class);

    private static final long serialVersionUID = 5765118329881301375L;

    // The RATs are stored in a hashmap of string vectors. The keys are the RAT
    // names and the vectors just contain the unit names listed a number of times
    // equal to the frequency
    private final Map<String, RatEntry> rats = new HashMap<>();
    private static RandomUnitGenerator rug;
    private static boolean interrupted = false;
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
     */
    protected class RatEntry {
        private Vector<String> units;
        private Vector<Float> weights;

        RatEntry() {
            setUnits(new Vector<>());
            setWeights(new Vector<>());
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
        chosenRAT = "House Kurita Heavy 'Mek";
        listeners = new ArrayList<>();
    }

    protected void initRatTree() {
        ratTree = new RatTreeNode("Random Assignment Tables");
    }

    public synchronized void populateUnits() {
        initRatTree();

        // Give the MSC some time to initialize
        MekSummaryCache msc = MekSummaryCache.getInstance();
        long waitLimit = System.currentTimeMillis() + 3000; /* 3 seconds */
        while (!interrupted && !msc.isInitialized() && waitLimit > System.currentTimeMillis()) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                // Ignore
            }
        }

        loadRatsFromDirectory(Configuration.armyTablesDir(), msc);
        cleanupNode(ratTree);
        if (!interrupted) {
            rug.initialized = true;
            rug.notifyListenersOfInitialization();
        }
    }

    public synchronized void registerListener(ActionListener l) {
        listeners.add(l);
    }

    /**
     * Notifies all the listeners that initialization is finished
     */
    public void notifyListenersOfInitialization() {
        if (initialized) {
            for (ActionListener l : listeners) {
                l.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "rugInitialized"));
            }
        }
    }

    protected void addRat(String ratName, RatEntry ratEntry) {
        rats.put(ratName, ratEntry);
    }

    private void readRat(InputStream is, RatTreeNode node, String fileName, MekSummaryCache msc) throws IOException {
        try (InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
                BufferedReader reader = new BufferedReader(isr)) {
            int lineNumber = 0;
            String key = "Huh";
            float totalWeight = 0.0f;
            RatEntry re = new RatEntry();
            String line = null;
            while (null != (line = reader.readLine())) {
                if (interrupted) {
                    return;
                }

                if (line.startsWith("#")) {
                    continue;
                }

                lineNumber++;
                if (lineNumber == 1) {
                    key = line;
                } else {
                    String[] values = line.split(",");
                    if (values.length < 2) {
                        logger.error(String.format("Not enough fields in %s on %d",
                                fileName, lineNumber));
                        continue;
                    }
                    String name = values[0];
                    float weight;
                    try {
                        weight = Integer.parseInt(values[1].trim());
                    } catch (NumberFormatException nef) {
                        logger.error(nef, String.format(
                                "The frequency field could not be interpreted on line %d of %s",
                                lineNumber, fileName));
                        continue;
                    }

                    if (weight <= 0.0f) {
                        logger.error(String.format(
                                "The frequency field is zero or negative (%d) on line %d of %s",
                                Math.round(weight), lineNumber, fileName));
                        continue;
                    }

                    // The @ symbol denotes a reference to another RAT rather than a unit.
                    if (!name.startsWith("@") && (null == msc.getMek(name))) {
                        logger.error(String.format(
                                "The unit %s could not be found in the %s RAT (%s)",
                                name, key, fileName));
                        continue;
                    }
                    re.getUnits().add(name.intern());
                    re.getWeights().add(weight);
                    totalWeight += weight;
                }
            }

            // Calculate total weights
            if (!re.getUnits().isEmpty()) {
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
        String[] pathElements = path.split("/", -1);
        for (int i = 0; i < pathElements.length - 1; i++) {
            if (pathElements[i].isBlank()) {
                continue;
            }
            RatTreeNode subNode = null;
            for (RatTreeNode rtn : result.children) {
                if (rtn.name.equals(pathElements[i])) {
                    subNode = rtn;
                    break;
                }
            }

            if (null == subNode) {
                subNode = new RatTreeNode(pathElements[i]);
                result.children.addElement(subNode);
            }
            result = subNode;
        }
        return result;
    }

    private void cleanupNode(RatTreeNode node) {
        for (RatTreeNode child : node.children) {
            cleanupNode(child);
        }
        Collections.sort(node.children);
    }

    private void loadRatsFromDirectory(File dir, MekSummaryCache msc) {
        loadRatsFromDirectory(dir, msc, ratTree);
    }

    private void loadRatsFromDirectory(File dir, MekSummaryCache msc, RatTreeNode node) {
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

            if (ratFileNameLC.equals("_svn") || ratFileNameLC.equals(".svn")) {
                // This is a Subversion work directory. Lets ignore it.
                continue;
            }

            // READ IN RATS
            if (ratFile.isDirectory()) {
                RatTreeNode newNode = getNodeByPath(node, ratFile.getName() + "/");

                // recursion is fun
                loadRatsFromDirectory(ratFile, msc, newNode);

                // Prune empty nodes (this removes the "Unofficial" place holder)
                if (newNode.children.isEmpty()) {
                    node.children.remove(newNode);
                }
                continue;
            }

            if (ratFileNameLC.endsWith(".zip")) {
                try (ZipFile zipFile = new ZipFile(ratFile)) {
                    Enumeration<? extends ZipEntry> entries = zipFile.entries();
                    while (entries.hasMoreElements()) {
                        ZipEntry entry = entries.nextElement();
                        String entryName = entry.getName();
                        if (!entry.isDirectory() && entryName.toLowerCase(Locale.ROOT).endsWith(".txt")) {
                            RatTreeNode subNode = getNodeByPath(node, entryName);
                            try (InputStream zis = zipFile.getInputStream(entry)) {
                                readRat(zis, subNode, ratFile.getName() + ":" + entryName, msc);
                            }
                        }
                    }
                } catch (Exception ex) {
                    logger.error(ex, "Unable to load " + ratFile.getName());
                }
            }

            if (!ratFileNameLC.endsWith(".txt")) {
                continue;
            }

            try (InputStream ratInputStream = new FileInputStream(ratFile)) {
                readRat(ratInputStream, node, ratFile.getName(), msc);
            } catch (Exception ex) {
                logger.error(ex, "Unable to load " + ratFile.getName());
            }
        }
    }

    /**
     * Generate a list of units from the RAT
     *
     * @return - a string giving the name
     */
    public ArrayList<MekSummary> generate(int numRolls, String ratName) {
        return generate(numRolls, ratName, null);
    }

    /**
     * Generate a list of units from the RAT.
     *
     * @param numRolls - the number of units to roll from the RAT
     * @param ratName  - name of the RAT to roll on
     * @param filter   - entries in the RAT must pass this condition to be included.
     *                 If null, no filter is applied.
     * @return - a list of units determined by the random rolls
     */
    public ArrayList<MekSummary> generate(int numRolls, String ratName, Predicate<MekSummary> filter) {
        ArrayList<MekSummary> units = new ArrayList<>();

        try {
            int retryCount = 0;

            // give the RATs a few seconds to load
            while (!initialized && (retryCount < 5)) {
                try {
                    Thread.sleep(1000);
                } catch (Exception ignored) {

                }

                retryCount++;
            }

            Map<String, RatEntry> ratMap = getRatMap();
            if (null != ratMap) {
                RatEntry re = ratMap.get(ratName);
                if (filter != null) {
                    RatEntry filtered = new RatEntry();
                    float totalWeight = 0.0f;
                    MekSummaryCache msc = MekSummaryCache.getInstance();
                    for (int i = 0; i < re.getUnits().size(); i++) {
                        if (!re.getUnits().get(i).startsWith("@")) {
                            MekSummary ms = msc.getMek(re.getUnits().get(i));
                            if (ms == null || !filter.test(ms)) {
                                continue;
                            }
                        }
                        filtered.getUnits().add(re.getUnits().get(i));
                        filtered.getWeights().add(re.getWeights().get(i));
                        totalWeight += re.getWeights().get(i);
                    }
                    for (int i = 0; i < filtered.getWeights().size(); i++) {
                        filtered.getWeights().set(i, filtered.getWeights().get(i) / totalWeight);
                    }
                    re = filtered;
                }
                if ((null != re) && !re.getUnits().isEmpty()) {
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
                            units.addAll(generate(1, name.replaceFirst("@", ""), filter));
                            continue;
                        }

                        MekSummary unit = getMekByName(name);
                        if (null != unit) {
                            units.add(unit);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error(e, "generate");
        }
        return units;
    }

    protected MekSummary getMekByName(String name) {
        return MekSummaryCache.getInstance().getMek(name);
    }

    protected double getRandom() {
        return Math.random();
    }

    public ArrayList<MekSummary> generate(int numRolls) {
        return generate(numRolls, getChosenRAT(), null);
    }

    public ArrayList<MekSummary> generate(int numRolls, Predicate<MekSummary> filter) {
        return generate(numRolls, getChosenRAT(), filter);
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
        return rats.keySet().iterator();
    }

    public RatTreeNode getRatTree() {
        return ratTree;
    }

    public static synchronized RandomUnitGenerator getInstance() {
        if (null == rug) {
            rug = new RandomUnitGenerator();
        }

        if (!rug.initialized && !rug.initializing) {
            rug.initializing = true;
            interrupted = false;
            rug.loader = new Thread(() -> {
                long start = System.currentTimeMillis();
                rug.populateUnits();
                long end = System.currentTimeMillis();
                logger.info("Loaded Rats in: " + (end - start) + "ms.");
            }, "Random Unit Generator unit populater");
            rug.loader.setPriority(Thread.NORM_PRIORITY - 1);
            rug.loader.start();
        }
        return rug;
    }

    public boolean isInitialized() {
        return initialized;
    }
}
