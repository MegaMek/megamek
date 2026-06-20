/*
 * Copyright (C) 2016-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ratgenerator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilder;

import megamek.client.generator.RandomNameGenerator;
import megamek.common.annotations.Nullable;
import megamek.common.units.EntityWeightClass;
import megamek.logging.MMLogger;
import megamek.utilities.xml.MMXMLUtility;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Container for all the rule nodes for a faction. Has methods for processing the rules to fill out a ForceDescriptor.
 *
 * @author Neoancient
 */
public class Ruleset {
    private final static MMLogger logger = MMLogger.create(Ruleset.class);

    public enum RatingSystem {
        IS("F", "D", "C", "B", "A"),
        SL("C", "B", "A"), // used for SLDF and CS/WoB
        CLAN("PG", "Sol", "SL", "FL", "Keshik"),
        ROS("TP", "PG", "HS", "SB"),
        NONE();

        final String[] vals;

        RatingSystem(String... vals) {
            this.vals = vals;
        }

        public int indexOf(String val) {
            for (int i = 0; i < vals.length; i++) {
                if (val.equals(vals[i])) {
                    return i;
                }
            }
            return -1;
        }
    }

    private static final String directory = "data/forcegenerator/faction_rules";
    private static final String CONSTANTS_FILE = "constants.txt";

    // Progress-bar weights for the phases of processRoot(), as fractions of the force-generation
    // pass. They are display hints for the ProgressListener only and do not affect generation.
    private static final double PROGRESS_BUILD_TREE = 0.05;
    private static final double PROGRESS_GENERATE_UNITS = 0.5;
    private static final double PROGRESS_LOAD_ENTITIES = 0.4;
    private static final double PROGRESS_FINALIZE = 0.05;

    private static HashMap<String, String> constants;
    private static final Pattern constantPattern = Pattern.compile("%(.*?)%");
    private static HashMap<String, Ruleset> rulesets;
    private static boolean initialized;
    private static boolean initializing;

    private String faction;
    private RatingSystem ratingSystem;
    private DefaultsNode defaults;
    private TOCNode toc;
    private int customRankBase;
    private final HashMap<Integer, String> customRanks;
    private final ArrayList<ForceNode> forceNodes;
    private String parent;

    private Ruleset() {
        faction = FactionRecord.IS_GENERAL_KEY;
        ratingSystem = RatingSystem.IS;
        defaults = new DefaultsNode();
        toc = new TOCNode();
        customRanks = new HashMap<>();
        forceNodes = new ArrayList<>();
        parent = null;
    }

    public static String substituteConstants(String str) {
        Matcher matcher = constantPattern.matcher(str);
        while (matcher.find()) {
            String val = constants.get(matcher.group(1));
            if (val == null) {
                val = "0";
            }
            str = str.replace(matcher.group(0), val);
        }
        return str;
    }

    public static Ruleset findRuleset(ForceDescriptor fd) {
        return findRuleset(fd.getFaction());
    }

    public static Ruleset findRuleset(String faction) {
        if (faction == null) {
            faction = FactionRecord.IS_GENERAL_KEY;
        }
        if (rulesets.containsKey(faction)) {
            logger.debug("findRuleset({}): direct match", faction);
            return rulesets.get(faction);
        }
        FactionRecord fRec = RATGenerator.getInstance().getFaction(faction);
        /*
         * First check all parents without recursion. If none is found, do
         * a recursive check on all parents.
         */
        if (fRec != null) {
            for (String parent : fRec.getParentFactions()) {
                if (rulesets.containsKey(parent)) {
                    logger.debug("findRuleset({}): parent match {}", faction, parent);
                    return findRuleset(parent);
                }
            }
            for (String parent : fRec.getParentFactions()) {
                Ruleset rs = findRuleset(parent);
                if (rs != null) {
                    logger.debug("findRuleset({}): recursive parent match via {} -> {}",
                          faction, parent, rs.getFaction());
                    return rs;
                }
            }
        }
        // This shouldn't happen unless the data is missing. Throw out a default ruleset
        // to prevent barfing.
        logger.warn("findRuleset({}): no match in any parent — returning empty default ruleset", faction);
        return new Ruleset();
    }

    @Deprecated(since = "0.51.0", forRemoval = true)
    public int getCustomRankBase() {
        return customRankBase;
    }

    @Deprecated(since = "0.51.0", forRemoval = true)
    public HashMap<Integer, String> getCustomRanks() {
        return customRanks;
    }

    @FunctionalInterface
    public interface ProgressListener {
        /**
         * Notifies listener of progress in generating force.
         *
         * @param progress The fraction of the task that has been completed in this step.
         * @param message  A message that describes the current step.
         */
        void updateProgress(double progress, String message);
    }

    public void processRoot(ForceDescriptor fd, ProgressListener l) {
        logger.debug("[ForceGen][Weight] processRoot ENTER: faction={} echelon={} unitType={} rating={} " +
                    "weightClass={} ({})",
              fd.getFaction(), fd.getEchelon(), fd.getUnitType(), fd.getRating(),
              fd.getWeightClass(),
              fd.getWeightClassCode().isEmpty() ? "RANDOM - ruleset will roll one" : fd.getWeightClassCode());
        defaults.apply(fd);
        // save the setting so it can be restored after assigning names
        String rngFaction = RandomNameGenerator.getInstance().getChosenFaction();

        buildForceTree(fd, l, PROGRESS_BUILD_TREE);
        // Capture the weight class the ruleset ROLLED for this force (the value that drove the
        // <weightTarget> selection) before recalcWeightClass() below overwrites it with the
        // weight implied by the units actually generated. This is the right label for tuning.
        String rolledWeight = fd.getWeightClassCode().isEmpty() ? "RANDOM" : fd.getWeightClassCode();
        // Cluster identity flags (e.g. CCC's battle/coil/fang named types). These let the CSV tune
        // per named cluster type, since all of them roll the same H/M/L weight picker.
        String clusterFlags = String.join(";", fd.getFlags());
        // Per-cluster-type weight budget: reshape element weights to the faction's <weightTarget>
        // blocks before units are picked. Data-gated -- a no-op for any cluster that declares no
        // targets, so factions without <weightTarget> generate exactly as before.
        WeightBudgetAllocator.allocate(fd);
        fd.generateUnits(l, PROGRESS_GENERATE_UNITS);
        if (null != l) {
            l.updateProgress(0, "Finalizing formation");
        }
        fd.recalcWeightClass();
        // Optional: fill each large craft's ASF bays with its carried fighter complement and nest the
        // fighters under the ship. Run before commander/id/entity assignment so the normal passes handle
        // the new fighters. Off unless the user ticks the option.
        if (fd.isFighterComplement()) {
            fd.addFighterComplement();
        }
        fd.assignCommanders();
        fd.assignPositions();

        if (null != l) {
            l.updateProgress(PROGRESS_FINALIZE, "Finalizing formation");
        }
        // Stamp every node with a unique force id before loading entities, so the force strings
        // written onto the entities are collision-free and the server reconstructs the exact tree.
        int nextForceId = fd.assignForceIds(1);
        fd.loadEntities(l, PROGRESS_LOAD_ENTITIES);
        // fd.assignBloodnames();

        ForceDescriptor transports = fd.assignTransport();
        if (null != transports) {
            // Attach first so the transports' parent is set, then number and load them; their force
            // strings then correctly nest the transport force under the force it carries.
            fd.addAttached(transports);
            transports.assignForceIds(nextForceId);
            transports.loadEntities(l, 0);
        }

        if (null != l) {
            l.updateProgress(0, "Complete");
        }

        // Diagnostic: tally the weight class of every generated BattleMek so a caller can verify
        // that a requested force weight (e.g. an Assault regiment) produced the expected mix.
        // Compare against the per-faction subforce tables in the ruleset XML.
        int[] mekWeights = fd.tallyMekWeightClasses();
        int totalMeks = 0;
        for (int count : mekWeights) {
            totalMeks += count;
        }
        if (totalMeks > 0) {
            logger.debug("[ForceGen][Weight] generated BattleMek weight distribution ({} total): " +
                        "UltraLight={} Light={} Medium={} Heavy={} Assault={} SuperHeavy={}",
                  totalMeks,
                  mekWeights[EntityWeightClass.WEIGHT_ULTRA_LIGHT],
                  mekWeights[EntityWeightClass.WEIGHT_LIGHT],
                  mekWeights[EntityWeightClass.WEIGHT_MEDIUM],
                  mekWeights[EntityWeightClass.WEIGHT_HEAVY],
                  mekWeights[EntityWeightClass.WEIGHT_ASSAULT],
                  mekWeights[EntityWeightClass.WEIGHT_SUPER_HEAVY]);
        }
        // Append machine-readable rows for weight-mix tuning (logs/forcegen_weights.csv): one row per
        // weight-classed unit type (Mek/Aero/Vehicle/BA). Logged independently of the Mek-only summary
        // above so Mek-less forces (solahma/infantry, pure-aero) still record. Per CLUSTER so factions
        // whose identity lives on the cluster (e.g. CCC's flag-named types) are tagged individually; if
        // the generated force is a single cluster or smaller, log it directly with its own flags.
        List<ForceDescriptor> clusters = new ArrayList<>();
        collectClusters(fd, clusters);
        int year = (fd.getYear() != null) ? fd.getYear() : -1;
        if (clusters.isEmpty()) {
            ForceGenWeightCsv.append(fd.getFaction(), year, fd.getRating(), rolledWeight, clusterFlags,
                  fd.tallyWeightClassesByType());
        } else {
            for (ForceDescriptor cluster : clusters) {
                String cwc = cluster.getWeightClassCode().isEmpty() ? "RANDOM" : cluster.getWeightClassCode();
                int cyear = (cluster.getYear() != null) ? cluster.getYear() : year;
                ForceGenWeightCsv.append(cluster.getFaction(), cyear, cluster.getRating(), cwc,
                      String.join(";", cluster.getFlags()), cluster.tallyWeightClassesByType());
            }
        }

        // Large craft (WarShips/DropShips/JumpShips/Space Stations) are absent from the weight CSV
        // (no L/M/H/A class), so record them separately with their structural path for naval
        // verification (logs/forcegen_warships.csv): correct galaxy/reserve nesting, no duplicates,
        // and EMPTY-point detection.
        ForceGenWarshipCsv.append(fd);

        RandomNameGenerator.getInstance().setChosenFaction(rngFaction);
    }

    /** CLUSTER echelon level (see forcegenerator/faction_rules/constants.txt). */
    private static final int CLUSTER_ECHELON = 6;

    /**
     * Collects every CLUSTER-echelon descriptor in the tree (not descending into a cluster's own subforces), so
     * weight-mix logging can record one row-set per cluster tagged with that cluster's flags. Used for factions whose
     * identity lives on the cluster, e.g. Cloud Cobra's named types.
     *
     * @param fd  the node to search from
     * @param out accumulator for cluster nodes found
     */
    private static void collectClusters(ForceDescriptor fd, List<ForceDescriptor> out) {
        Integer echelon = fd.getEchelon();
        if ((echelon != null) && (echelon == CLUSTER_ECHELON)) {
            out.add(fd);
            return;
        }
        for (ForceDescriptor sub : fd.getSubForces()) {
            collectClusters(sub, out);
        }
        // Also walk attached forces: a faction's aerospace and naval clusters are often attached to
        // a galaxy/touman (e.g. Clan Blood Spirit puts all its ASF clusters on the Blood Galaxy),
        // so without this they never reach the weight log.
        for (ForceDescriptor att : fd.getAttached()) {
            collectClusters(att, out);
        }
    }

    /**
     * Recursively build the force structure by assigning appropriate values to the current node, including number and
     * type of subforce and attached force nodes, and process those as well.
     *
     */
    private void buildForceTree(ForceDescriptor fd, ProgressListener l, double progress) {
        // Find the most specific ruleset for this faction.
        Ruleset ruleset = findRuleset(fd.getFaction());
        boolean applied = false;
        ForceNode forceNode;
        // Find the first node matching node in the ruleset and apply the options to the
        // current force descriptor.
        // If no matching node is found in the ruleset, move to the parent ruleset.
        // Cap the retry count: changeEschelon should only re-loop a finite number of times.
        // A malformed ruleset (e.g., a changeEschelon option whose echelon equals the current
        // one and whose assertions fail to clear its triggering predicate) could otherwise
        // spin forever. After maxIterations failed applies on the same node we log an ERROR
        // and bail out so the UI doesn't lock up.
        int safetyCounter = 0;
        final int maxIterations = 32;
        do {
            forceNode = ruleset.findForceNode(fd);
            if (forceNode == null) {
                if (ruleset.getParent() == null) {
                    ruleset = null;
                } else {
                    ruleset = rulesets.get(ruleset.getParent());
                }
            } else {
                applied = forceNode.apply(fd);
                logger.debug("Selecting force node {} from ruleset {}", forceNode.show(), ruleset.getFaction());
                if (!applied && ++safetyCounter >= maxIterations) {
                    logger.error("buildForceTree: aborting after {} iterations on force node {} " +
                          "(ruleset {}, faction {}, echelon {}). Likely a changeEschelon loop in the ruleset.",
                          maxIterations, forceNode.show(), ruleset.getFaction(),
                          fd.getFaction(), fd.getEchelon());
                    break;
                }
            }
        } while (ruleset != null && (forceNode == null || !applied));

        int count = fd.getSubForces().size() + fd.getAttached().size();

        // Process subForces recursively. It is possible that the subforce has
        // a different faction, in which case the ruleset appropriate to that faction is
        // used.
        for (ForceDescriptor sub : fd.getSubForces()) {
            ruleset = this;
            if (!fd.getFaction().equals(sub.getFaction())) {
                ruleset = findRuleset(sub.getFaction());
            }
            if (ruleset == null) {
                buildForceTree(sub, l, progress / count);
            } else {
                ruleset.buildForceTree(sub, l, progress / count);
            }
        }

        // Any attached support units are then built.
        for (ForceDescriptor sub : fd.getAttached()) {
            logger.debug("[ForceGen][Attached] buildForceTree ENTER: parent='{}' (esch={} ut={}) " +
                        "attached='{}' (esch={} ut={} wc={} faction={})",
                  fd.getName(), fd.getEchelon(), fd.getUnitType(),
                  sub.getName(), sub.getEchelon(), sub.getUnitType(),
                  sub.getWeightClass(), sub.getFaction());
            int subCountBefore = sub.getSubForces().size();
            int attCountBefore = sub.getAttached().size();
            buildForceTree(sub, l, progress / count);
            logger.debug("[ForceGen][Attached] buildForceTree DONE:  attached='{}' (esch={}) " +
                        "produced subForces={} (was {}) attached={} (was {})",
                  sub.getName(), sub.getEchelon(),
                  sub.getSubForces().size(), subCountBefore,
                  sub.getAttached().size(), attCountBefore);
        }
        /*
         * // Each attached formation is essentially a new top-level node
         * for (ForceDescriptor sub : fd.getAttached()) {
         * sub.generateUnits(l, progress * 0.7 / count);
         * sub.assignCommanders();
         * sub.assignPositions();
         * sub.loadEntities(l, progress * 0.1 / count);
         * sub.assignBloodnames();
         * }
         */
        if (count == 0 && null != l) {
            l.updateProgress(progress, "Building force tree");
        }
    }

    public int getRatingIndex(String key) {
        return ratingSystem.indexOf(key);
    }

    /**
     * Returns the given equipment rating followed by every worse rating in this ruleset's rating system, ordered from
     * the given rating down to the worst. Used as an equipment-rating fallback ladder during unit generation: a force
     * tries its own rating first and only steps down to worse-equipped ratings when nothing can be generated, never to
     * a better rating. If the rating is not part of this system, the list contains only the rating itself.
     *
     * @param rating the force's own equipment rating
     *
     * @return the rating and all worse ratings, closest first
     */
    public List<String> getRatingsAtOrWorseThan(String rating) {
        List<String> result = new ArrayList<>();
        int idx = ratingSystem.indexOf(rating);
        if (idx < 0) {
            if (rating != null) {
                result.add(rating);
            }
            return result;
        }
        // RatingSystem values are ordered worst-to-best, so worse ratings are lower indices.
        for (int i = idx; i >= 0; i--) {
            result.add(ratingSystem.vals[i]);
        }
        return result;
    }

    public Integer getDefaultUnitType(ForceDescriptor fd) {
        String def = defaults.getUnitType(fd);
        if (def != null) {
            return ModelRecord.parseUnitType(def);
        }
        return null;
    }

    public String getDefaultEschelon(ForceDescriptor fd) {
        return defaults.getEschelon(fd);
    }

    public String getDefaultRating(ForceDescriptor fd) {
        return defaults.getRating(fd);
    }

    public TOCNode getTOCNode() {
        return toc;
    }

    public ForceNode findForceNode(ForceDescriptor fd) {
        for (ForceNode n : forceNodes) {
            if (n.getEchelon().equals(fd.getEchelon()) && n.matches(fd)) {
                return n;
            }
        }
        return null;
    }

    public ForceNode findForceNode(ForceDescriptor fd, int eschelon, boolean augmented) {
        for (ForceNode n : forceNodes) {
            if (n.getEchelon() == eschelon && n.matches(fd, augmented)) {
                return n;
            }
        }
        return null;
    }

    public HashMap<String, String> getEschelonNames(String unitType) {
        HashMap<String, String> retVal = new HashMap<>();
        for (ForceNode n : forceNodes) {
            if (n.matchesPredicate(unitType, "ifUnitType")) {
                retVal.put(n.getEchelonCode(), n.getEchelonName());
            }
        }
        return retVal;
    }

    public String getEschelonName(ForceDescriptor fd) {
        for (ForceNode fn : forceNodes) {
            if (fn.matches(fd) && fn.getEchelon().equals(fd.getEchelon())) {
                return fn.getEchelonName();
            }
        }
        return null;
    }

    public CommanderNode getCoNode(ForceDescriptor fd) {
        for (ForceNode fn : forceNodes) {
            if (fn.getEchelon().equals(fd.getEchelon()) && fn.matches(fd)) {
                for (CommanderNode rn : fn.getCoNodes()) {
                    if (rn.matches(fd)) {
                        return rn;
                    }
                }
            }
        }
        return null;
    }

    public CommanderNode getXoNode(ForceDescriptor fd) {
        for (ForceNode fn : forceNodes) {
            if (fn.getEchelon().equals(fd.getEchelon()) && fn.matches(fd)) {
                for (CommanderNode rn : fn.getXoNodes()) {
                    if (rn.matches(fd)) {
                        return rn;
                    }
                }
            }
        }
        return null;
    }

    public String getParent() {
        return parent;
    }

    public static void loadConstants(File f) {
        constants = new HashMap<>();
        InputStream is;
        try {
            is = new FileInputStream(f);
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("#") && line.contains(":")) {
                    String[] fields = line.split(":");
                    try {
                        constants.put(fields[0], fields[1]);
                    } catch (NumberFormatException e) {
                        logger.error(e, "Malformed line in force generator constants file: {}", line);
                    }
                }
            }
            reader.close();
        } catch (Exception ex) {
            logger.error(ex, "loadConstants");
        }
    }

    public static void loadData() {
        initialized = false;
        initializing = true;
        rulesets = new HashMap<>();

        File dir = new File(directory);
        if (!dir.exists()) {
            logger.error("Could not locate force generator faction rules.");
            initializing = false;
            return;
        }

        loadConstants(new File(dir, CONSTANTS_FILE));

        // We need this so we can determine parent faction if not stated explicitly.
        while (!RATGenerator.getInstance().isInitialized()) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                // Ignore
            }
        }

        File[] files = dir.listFiles();

        if (files != null) {
            for (File file : files) {
                if (!file.getPath().endsWith(".xml")) {
                    continue;
                }
                try {
                    Ruleset ruleset = createFromFile(file);
                    if (ruleset != null) {
                        rulesets.put(ruleset.getFaction(), ruleset);
                    }
                } catch (Exception ex) {
                    logger.error(ex, "Failed while parsing file {}", file);
                }
            }
        }

        initialized = true;
        initializing = false;
    }

    private static @Nullable Ruleset createFromFile(File f) {
        Document xmlDoc;

        DocumentBuilder db;
        try (FileInputStream fis = new FileInputStream(f)) {
            db = MMXMLUtility.newSafeDocumentBuilder();
            xmlDoc = db.parse(fis);
        } catch (Exception ex) {
            logger.error(ex, "Failed loading force template from file {}", f.getName());
            return null;
        }

        Ruleset retVal = new Ruleset();

        Element elem = xmlDoc.getDocumentElement();
        if (!elem.getNodeName().equals("ruleset")) {
            logger.error("Could not find ruleset element in file {}", f.getName());
            return null;
        }

        if (!elem.getAttribute("faction").isBlank()) {
            retVal.faction = elem.getAttribute("faction");
        } else {
            logger.error("Faction is not declared in ruleset file {}", f.getName());
            return null;
        }
        if (!elem.getAttribute("parent").isBlank()) {
            retVal.parent = elem.getAttribute("parent");
        } else {
            if (retVal.faction.contains(".")) {
                retVal.parent = retVal.faction.split("\\.")[0];
            } else {
                FactionRecord fRec = RATGenerator.getInstance().getFaction(retVal.faction);
                if (fRec == null) {
                    retVal.parent = null;
                } else if (fRec.isClan()) {
                    retVal.parent = FactionRecord.CL_GENERAL_KEY;
                } else if (fRec.isPeriphery()) {
                    retVal.parent = FactionRecord.PER_GENERAL_KEY;
                } else {
                    retVal.parent = FactionRecord.IS_GENERAL_KEY;
                }

                if (retVal.faction.equals(retVal.parent)) {
                    retVal.parent = null;
                }
            }
        }
        // Rating system defaults to IS if not present. If present but cannot be parsed,
        // is set to NONE.
        if (!elem.getAttribute("ratingSystem").isBlank()) {
            switch (elem.getAttribute("ratingSystem")) {
                case "IS":
                    retVal.ratingSystem = RatingSystem.IS;
                    break;
                case "SL":
                    retVal.ratingSystem = RatingSystem.SL;
                    break;
                case "CLAN":
                    retVal.ratingSystem = RatingSystem.CLAN;
                    break;
                case "ROS":
                    retVal.ratingSystem = RatingSystem.ROS;
                    break;
                default:
                    retVal.ratingSystem = RatingSystem.NONE;
                    break;
            }
        } else {
            retVal.ratingSystem = RatingSystem.IS;
        }
        NodeList nl = elem.getChildNodes();
        elem.normalize();

        for (int x = 0; x < nl.getLength(); x++) {
            Node wn = nl.item(x);
            switch (wn.getNodeName()) {
                case "defaults":
                    retVal.defaults = DefaultsNode.createFromXml(wn);
                    break;
                case "toc":
                    retVal.toc = TOCNode.createFromXml(wn);
                    break;
                case "customRanks":
                    for (int y = 0; y < wn.getChildNodes().getLength(); y++) {
                        Node wn2 = wn.getChildNodes().item(y);
                        switch (wn2.getNodeName()) {
                            case "base":
                                retVal.customRankBase = Integer.parseInt(substituteConstants(wn2.getTextContent()));
                                break;
                            case "rank":
                                String[] fields = wn2.getTextContent().split(":");
                                int rank = Integer.parseInt(substituteConstants(fields[0]));
                                retVal.customRanks.put(rank, fields[1]);
                                break;
                        }
                    }
                    break;
                case "force":
                    ForceNode fn = null;
                    try {
                        fn = ForceNode.createFromXml(wn);
                    } catch (IllegalArgumentException ex) {
                        logger.error(ex, "In file {} while processing force node{}: {}",
                              f.getName(),
                              (wn.getAttributes().getNamedItem("eschName") == null) ? ""
                                    : " "
                                      + wn.getAttributes().getNamedItem("eschName"),
                              ex.getMessage());
                    }
                    if (fn != null) {
                        retVal.forceNodes.add(fn);
                    }
                    break;
            }
        }

        return retVal;
    }

    public static boolean isInitialized() {
        return initialized;
    }

    public static boolean isInitializing() {
        return initializing;
    }

    public String getFaction() {
        return faction;
    }
}
