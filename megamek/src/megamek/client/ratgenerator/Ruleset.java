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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilder;

import megamek.client.generator.RandomNameGenerator;
import megamek.common.annotations.Nullable;
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
        IS("F", "D", "C", "B", "A"), SL("C", "B", "A"), // used for SLDF and CS/WoB
        CLAN("PG", "Sol", "SL", "FL", "Keshik"), ROS("TP", "PG", "HS", "SB"), NONE();

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

    public static Ruleset findRuleset(ForceDescriptor forceDescriptor) {
        return findRuleset(forceDescriptor.getFaction());
    }

    public static Ruleset findRuleset(String faction) {
        if (faction == null) {
            faction = FactionRecord.IS_GENERAL_KEY;
        }

        if (rulesets.containsKey(faction)) {
            return rulesets.get(faction);
        }

        FactionRecord factionRecord = RATGenerator.getInstance().getFaction(faction);
        /*
         * First, check all parents without recursion. If none is found, do a recursive check on all parents.
         */
        if (factionRecord != null) {
            for (String parent : factionRecord.getParentFactions()) {
                if (rulesets.containsKey(parent)) {
                    return findRuleset(parent);
                }
            }
            for (String parent : factionRecord.getParentFactions()) {
                Ruleset ruleset = findRuleset(parent);
                if (ruleset != null) {
                    return ruleset;
                }
            }
        }
        // This shouldn't happen unless the data is missing. Throw out a default ruleset to prevent barfing.
        return new Ruleset();
    }

    /**
     * @deprecated no indicated uses
     */
    @Deprecated(since = "0.50.05", forRemoval = true)
    public int getCustomRankBase() {
        return customRankBase;
    }

    /**
     * @deprecated no indicated uses
     */
    @Deprecated(since = "0.50.05", forRemoval = true)
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

    public void processRoot(ForceDescriptor forceDescriptor, ProgressListener progressListener) {
        defaults.apply(forceDescriptor);
        // save the setting so it can be restored after assigning names
        String rngFaction = RandomNameGenerator.getInstance().getChosenFaction();

        buildForceTree(forceDescriptor, progressListener, 0.05);
        forceDescriptor.generateUnits(progressListener, 0.5);
        if (null != progressListener) {
            progressListener.updateProgress(0, "Finalizing formation");
        }
        forceDescriptor.recalcWeightClass();
        forceDescriptor.assignCommanders();
        forceDescriptor.assignPositions();

        if (null != progressListener) {
            progressListener.updateProgress(0.05, "Finalizing formation");
        }
        forceDescriptor.loadEntities(progressListener, 0.4);

        ForceDescriptor transports = forceDescriptor.assignTransport();
        if (null != transports) {
            transports.loadEntities(progressListener, 0);
            forceDescriptor.addAttached(transports);
        }

        if (null != progressListener) {
            progressListener.updateProgress(0, "Complete");
        }

        RandomNameGenerator.getInstance().setChosenFaction(rngFaction);
    }

    /**
     * Recursively build the force structure by assigning appropriate values to the current node, including number and
     * type of sub force and attached force nodes, and process those as well.
     *
     * @param forceDescriptor
     */
    private void buildForceTree(ForceDescriptor forceDescriptor, ProgressListener progressListener, double progress) {
        // Find the most specific ruleset for this faction.
        Ruleset ruleset = findRuleset(forceDescriptor.getFaction());
        boolean applied = false;
        ForceNode forceNode;
        // Find the first node matching node in the ruleset and apply the options to the current force descriptor. If
        // no matching node is found in the ruleset, move to the parent ruleset.
        do {
            forceNode = ruleset.findForceNode(forceDescriptor);
            if (forceNode == null) {
                if (ruleset.getParent() == null) {
                    ruleset = null;
                } else {
                    ruleset = rulesets.get(ruleset.getParent());
                }
            } else {
                applied = forceNode.apply(forceDescriptor);
                logger.debug("Selecting force node {} from ruleset {}", forceNode.show(), ruleset.getFaction());
            }
        } while (ruleset != null && (forceNode == null || !applied));

        int count = forceDescriptor.getSubforces().size() + forceDescriptor.getAttached().size();

        // Process forces recursively. It is possible that the sub force has a different faction, in which case the
        // ruleset appropriate to that faction is used.
        for (ForceDescriptor subForceDescriptor : forceDescriptor.getSubforces()) {
            ruleset = this;
            if (!forceDescriptor.getFaction().equals(subForceDescriptor.getFaction())) {
                ruleset = findRuleset(subForceDescriptor.getFaction());
            }
            if (ruleset == null) {
                buildForceTree(subForceDescriptor, progressListener, progress / count);
            } else {
                ruleset.buildForceTree(subForceDescriptor, progressListener, progress / count);
            }
        }

        // Any attached support units are then built.
        for (ForceDescriptor subForceDescriptor : forceDescriptor.getAttached()) {
            buildForceTree(subForceDescriptor, progressListener, progress / count);
        }

        if (count == 0 && null != progressListener) {
            progressListener.updateProgress(progress, "Building force tree");
        }
    }

    public int getRatingIndex(String key) {
        return ratingSystem.indexOf(key);
    }

    public Integer getDefaultUnitType(ForceDescriptor forceDescriptor) {
        String def = defaults.getUnitType(forceDescriptor);
        if (def != null) {
            return ModelRecord.parseUnitType(def);
        }
        return null;
    }

    public String getDefaultEschelon(ForceDescriptor forceDescriptor) {
        return defaults.getEschelon(forceDescriptor);
    }

    public String getDefaultRating(ForceDescriptor forceDescriptor) {
        return defaults.getRating(forceDescriptor);
    }

    public TOCNode getTOCNode() {
        return toc;
    }

    public ForceNode findForceNode(ForceDescriptor forceDescriptor) {
        for (ForceNode forceNode : forceNodes) {
            if (forceNode.getEschelon().equals(forceDescriptor.getEschelon()) && forceNode.matches(forceDescriptor)) {
                return forceNode;
            }
        }
        return null;
    }

    public ForceNode findForceNode(ForceDescriptor forceDescriptor, int echelon, boolean augmented) {
        for (ForceNode forceNode : forceNodes) {
            if (forceNode.getEschelon() == echelon && forceNode.matches(forceDescriptor, augmented)) {
                return forceNode;
            }
        }
        return null;
    }

    public HashMap<String, String> getEschelonNames(String unitType) {
        HashMap<String, String> retVal = new HashMap<>();
        for (ForceNode forceNode : forceNodes) {
            if (forceNode.matchesPredicate(unitType, "ifUnitType")) {
                retVal.put(forceNode.getEschelonCode(), forceNode.getEschelonName());
            }
        }
        return retVal;
    }

    public String getEschelonName(ForceDescriptor forceDescriptor) {
        for (ForceNode forceNode : forceNodes) {
            if (forceNode.matches(forceDescriptor) && forceNode.getEschelon().equals(forceDescriptor.getEschelon())) {
                return forceNode.getEschelonName();
            }
        }
        return null;
    }

    public CommanderNode getCoNode(ForceDescriptor forceDescriptor) {
        for (ForceNode forceNode : forceNodes) {
            if (forceNode.getEschelon().equals(forceDescriptor.getEschelon()) && forceNode.matches(forceDescriptor)) {
                for (CommanderNode commanderNode : forceNode.getCoNodes()) {
                    if (commanderNode.matches(forceDescriptor)) {
                        return commanderNode;
                    }
                }
            }
        }
        return null;
    }

    public CommanderNode getXoNode(ForceDescriptor forceDescriptor) {
        for (ForceNode forceNode : forceNodes) {
            if (forceNode.getEschelon().equals(forceDescriptor.getEschelon()) && forceNode.matches(forceDescriptor)) {
                for (CommanderNode commanderNode : forceNode.getXoNodes()) {
                    if (commanderNode.matches(forceDescriptor)) {
                        return commanderNode;
                    }
                }
            }
        }
        return null;
    }

    public String getParent() {
        return parent;
    }

    public static void loadConstants(File file) {
        constants = new HashMap<>();
        InputStream inputStream;
        try {
            inputStream = new FileInputStream(file);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
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
            } catch (InterruptedException ignored) {
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

    private static @Nullable Ruleset createFromFile(File file) {
        Document xmlDoc;

        DocumentBuilder documentBuilder;
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            documentBuilder = MMXMLUtility.newSafeDocumentBuilder();
            xmlDoc = documentBuilder.parse(fileInputStream);
        } catch (Exception ex) {
            logger.error(ex, "Failed loading force template from file {}", file.getName());
            return null;
        }

        Ruleset retVal = new Ruleset();

        Element elem = xmlDoc.getDocumentElement();
        if (!elem.getNodeName().equals("ruleset")) {
            logger.error("Could not find ruleset element in file {}", file.getName());
            return null;
        }

        if (!elem.getAttribute("faction").isBlank()) {
            retVal.faction = elem.getAttribute("faction");
        } else {
            logger.error("Faction is not declared in ruleset file {}", file.getName());
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
        // Rating system defaults to IS if not present. If present but cannot be parsed, is set to NONE.
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
        NodeList childNodes = elem.getChildNodes();
        elem.normalize();

        for (int x = 0; x < childNodes.getLength(); x++) {
            Node childNode = childNodes.item(x);
            switch (childNode.getNodeName()) {
                case "defaults":
                    retVal.defaults = DefaultsNode.createFromXml(childNode);
                    break;
                case "toc":
                    retVal.toc = TOCNode.createFromXml(childNode);
                    break;
                case "customRanks":
                    for (int y = 0; y < childNode.getChildNodes().getLength(); y++) {
                        Node customRankChildNode = childNode.getChildNodes().item(y);
                        switch (customRankChildNode.getNodeName()) {
                            case "base":
                                retVal.customRankBase = Integer.parseInt(substituteConstants(customRankChildNode.getTextContent()));
                                break;
                            case "rank":
                                String[] fields = customRankChildNode.getTextContent().split(":");
                                int rank = Integer.parseInt(substituteConstants(fields[0]));
                                retVal.customRanks.put(rank, fields[1]);
                                break;
                        }
                    }
                    break;
                case "force":
                    ForceNode forceNode = null;
                    try {
                        forceNode = ForceNode.createFromXml(childNode);
                    } catch (IllegalArgumentException ex) {
                        logger.error(ex,
                              "In file {} while processing force node {}: {}",
                              file.getName(),
                              ((childNode.getAttributes().getNamedItem("eschName") == null) ?
                                     "" :
                                     " " + childNode.getAttributes().getNamedItem("eschName")),
                              ex.getMessage());
                    }
                    if (forceNode != null) {
                        retVal.forceNodes.add(forceNode);
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
