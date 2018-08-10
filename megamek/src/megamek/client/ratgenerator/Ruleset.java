/*
 * MegaMek - Copyright (C) 2016 The MegaMek Team
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
package megamek.client.ratgenerator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.client.RandomNameGenerator;
import megamek.common.logging.DefaultMmLogger;

/**
 * Container for all the rule nodes for a faction. Has methods for processing the rules to
 * fill out a ForceDescriptor.
 * 
 * @author Neoancient
 *
 */
public class Ruleset {

    public enum RatingSystem {
        IS ("F","D","C","B","A"),
        SL ("C","B","A"), // used for SLDF and CS/WoB
        CLAN ("PG","Sol","SL","FL","Keshik"),
        ROS ("TP","PG","HS","SB"),
        NONE ();

        String[] vals;
        private RatingSystem(String... vals) {
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
    };

    private final static String directory = "data/forcegenerator/faction_rules";
    private final static String CONSTANTS_FILE = "constants.txt";

    private static HashMap<String,String> constants;
    private static Pattern constantPattern = Pattern.compile("%(.*?)%");
    private static HashMap<String,Ruleset> rulesets;
    private static boolean initialized;
    private static boolean initializing;

    private String faction;
    private RatingSystem ratingSystem;
    private DefaultsNode defaults;
    private TOCNode toc;
    private int customRankBase;
    private HashMap<Integer,String> customRanks;
    private ArrayList<ForceNode> forceNodes;
    private String parent;

    private Ruleset() {
        faction = "IS";
        ratingSystem = RatingSystem.IS;
        forceNodes = new ArrayList<>();
        customRanks = new HashMap<>();
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
            faction = "IS";
        }
        if (rulesets.containsKey(faction)) {
            return rulesets.get(faction);
        }
        FactionRecord fRec = RATGenerator.getInstance().getFaction(faction);
        /* First check all parents without recursion. If none is found, do
         * a recursive check on all parents.
         */
        if (fRec != null) {
            for (String parent : fRec.getParentFactions()) {
                if (rulesets.containsKey(parent)) {
                    return findRuleset(parent);
                }
            }
            for (String parent : fRec.getParentFactions()) {
                Ruleset rs = findRuleset(parent);
                if (rs != null) {
                    return rs;
                }
            }
        }
        return null;
    }

    public int getCustomRankBase() {
        return customRankBase;
    }

    public HashMap<Integer,String> getCustomRanks() {
        return customRanks;
    }

    @FunctionalInterface
    public interface ProgressListener {
        /**
         * Notifies listener of progress in generating force.
         * 
         * @param progress The fraction of the task that has been completed in this step.
         * @param message A message that describes the current step.
         *
         */
        void updateProgress(double progress, String message);
    }

    public void processRoot(ForceDescriptor fd, ProgressListener l) {
        defaults.apply(fd);
        // save the setting so it can be restored after assigning names
        String rngFaction = RandomNameGenerator.getInstance().getChosenFaction();

        buildForceTree(fd, l, 0.05);
        fd.generateUnits(l, 0.5);
        l.updateProgress(0, "Finalizing formation");
        fd.recalcWeightClass();
        fd.assignCommanders();
        fd.assignPositions();
        l.updateProgress(0.05, "Finalizing formation");
        fd.loadEntities(l, 0.4);
        //      fd.assignBloodnames();

        ForceDescriptor transports = fd.assignTransport();
        if (null != transports) {
            transports.loadEntities(l, 0);
            fd.addAttached(transports);
        }

        if (null != l) {
            l.updateProgress(0, "Complete");
        }

        RandomNameGenerator.getInstance().setChosenFaction(rngFaction);
    }

    /**
     * Recursively build the force structure by assigning appropriate values to the current node,
     * including number and type of subforce and attached force nodes, and process those as well.
     * 
     * @param fd
     */
    private void buildForceTree (ForceDescriptor fd, ProgressListener l, double progress) {
        final String METHOD_NAME = "buildForceTree(ForceDescriptor, ProgressListener, double)"; //$NON-NLS-1$

        //Find the most specific ruleset for this faction.
        Ruleset rs = findRuleset(fd.getFaction());
        boolean applied = false;
        ForceNode fn = null;
        //Find the first node matching node in the ruleset and apply the options to the current force descriptor.
        //If no matching node is found in the ruleset, move to the parent ruleset.
        do {
            fn = rs.findForceNode(fd);
            if (fn == null) {
                if (rs.getParent() == null) {
                    rs = null;
                } else {
                    rs = rulesets.get(rs.getParent());
                }				
            } else {
                applied = fn.apply(fd);
                DefaultMmLogger.getInstance().debug(getClass(), METHOD_NAME,
                        "Selecting force node " + fn.show() //$NON-NLS-1$
                        + " from ruleset " + rs.getFaction()); //$NON-NLS-1$
            }
        } while (rs != null && (fn == null || !applied));

        int count = fd.getSubforces().size() + fd.getAttached().size();

        //Process subforces recursively. It is possible that the subforce has
        //a different faction, in which case the ruleset appropriate to that faction is used.
        for (ForceDescriptor sub : fd.getSubforces()) {
            rs = this;
            if (!fd.getFaction().equals(sub.getFaction())) {
                rs = findRuleset(sub.getFaction());
            }
            if (rs == null) {
                buildForceTree(sub, l, progress / count);
            } else {
                rs.buildForceTree(sub, l, progress / count);
            }
        }

        //Any attached support units are then built.
        for (ForceDescriptor sub : fd.getAttached()) {
            buildForceTree(sub, l, progress / count);
        }
        /*
		//Each attached formation is essentially a new top-level node
		for (ForceDescriptor sub : fd.getAttached()) {
		    sub.generateUnits(l, progress * 0.7 / count);
			sub.assignCommanders();
			sub.assignPositions();
			sub.loadEntities(l, progress * 0.1 / count);
//			sub.assignBloodnames();
		}
         */
        if (count == 0 && null != l) {
            l.updateProgress(progress, "Building force tree");
        }
    }

    public int getRatingIndex(String key) {
        return ratingSystem.indexOf(key);
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
            if (n.getEschelon().equals(fd.getEschelon()) && n.matches(fd)) {
                return n;
            }
        }		
        return null;
    }

    public ForceNode findForceNode(ForceDescriptor fd, int eschelon, boolean augmented) {
        for (ForceNode n : forceNodes) {
            if (n.getEschelon() == eschelon && n.matches(fd, augmented)) {
                return n;
            }
        }		
        return null;
    }

    public HashMap<String,String> getEschelonNames(String unitType) {
        HashMap<String,String> retVal = new HashMap<String,String>();
        for (ForceNode n : forceNodes) {
            if (n.matchesPredicate(unitType, "ifUnitType")) {
                retVal.put(n.getEschelonCode(), n.getEschelonName());
            }
        }
        return retVal;
    }

    public String getEschelonName(ForceDescriptor fd) {
        for (ForceNode fn : forceNodes) {
            if (fn.matches(fd) && fn.getEschelon() == fd.getEschelon()) {
                return fn.getEschelonName();
            }
        }
        return null;
    }

    public CommanderNode getCoNode(ForceDescriptor fd) {
        for (ForceNode fn : forceNodes) {
            if (fn.getEschelon() == fd.getEschelon() && fn.matches(fd)) {
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
            if (fn.getEschelon() == fd.getEschelon() && fn.matches(fd)) {
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
        final String METHOD_NAME = "loadConstants(File)"; //$NON-NLS-1$

        constants = new HashMap<>();
        InputStream is;
        try {
            is = new FileInputStream(f);
            BufferedReader reader = new BufferedReader(new InputStreamReader(is,
                    Charset.forName("UTF-8")));
            String line = null;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("#") && line.contains(":")) {
                    String[] fields = line.split(":");
                    try {
                        constants.put(fields[0], fields[1]);
                    } catch (NumberFormatException e) {
                        DefaultMmLogger.getInstance().error(Ruleset.class, METHOD_NAME,
                                "Malformed line in force generator constants file: " + line); //$NON-NLS-1$
                    }
                }
            }
            reader.close();
        } catch (IOException e) {
            DefaultMmLogger.getInstance().error(Ruleset.class, METHOD_NAME, e); //$NON-NLS-1$
        }
    }

    public static void loadData() {
        final String METHOD_NAME = "loadData()"; //$NON-NLS-1$
        initialized = false;
        initializing = true;
        rulesets = new HashMap<String,Ruleset>();

        File dir = new File(directory);
        if (!dir.exists()) {
            DefaultMmLogger.getInstance().error(Ruleset.class, METHOD_NAME,
                    "Could not locate force generator faction rules."); //$NON-NLS-1$
            initializing = false;
            return;
        }

        loadConstants(new File(dir, CONSTANTS_FILE));

        //We need this so we can determine parent faction if not stated explicitly.
        while (!RATGenerator.getInstance().isInitialized()) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                // Ignore
            }
        }

        for (File f : dir.listFiles()) {
            if (!f.getPath().endsWith(".xml")) {
                continue;
            }
            try {
                Ruleset rs = createFromFile(f);
                if (rs != null) {
                    rulesets.put(rs.getFaction(), rs);
                }
            } catch (IllegalArgumentException ex) {
                DefaultMmLogger.getInstance().error(Ruleset.class, METHOD_NAME,
                        "While parsing file " + f.toString() + ": " + ex.getMessage());
            }
        }
        initialized = true;
        initializing = false;
    }

    private static Ruleset createFromFile(File f) {
        final String METHOD_NAME = "createFromFile(File)"; //$NON-NLS-1$

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        Document xmlDoc = null;

        DocumentBuilder db;
        try {
            FileInputStream fis = new FileInputStream(f);
            db = dbf.newDocumentBuilder();
            xmlDoc = db.parse(fis);
            fis.close();
        } catch (Exception e) {
            DefaultMmLogger.getInstance().error(Ruleset.class, METHOD_NAME,
                    "While loading force template from file " + f.getName() + ": " //$NON-NLS-1$
                    + e.getMessage());
            return null;
        }

        Ruleset retVal = new Ruleset();

        Element elem = xmlDoc.getDocumentElement();
        if (!elem.getNodeName().equals("ruleset")) {
            DefaultMmLogger.getInstance().error(Ruleset.class, METHOD_NAME,
                    "Could not find ruleset element in file " + f.getName()); //$NON-NLS-1$
            return null;
        }
        if (elem.getAttribute("faction").length() > 0) {
            retVal.faction = elem.getAttribute("faction");
        } else {
            DefaultMmLogger.getInstance().error(Ruleset.class, METHOD_NAME,
                    "Faction is not declared in ruleset file " + f.getName()); //$NON-NLS-1$
            return null;
        }
        if (elem.getAttribute("parent").length() > 0) {
            retVal.parent = elem.getAttribute("parent");
        } else {
            if (retVal.faction.contains(".")) {
                retVal.parent = retVal.faction.split("\\.")[0];
            } else {
                FactionRecord fRec = RATGenerator.getInstance().getFaction(retVal.faction);
                if (fRec == null) {
                    retVal.parent = null;
                } else if (fRec.isClan()) {
                    retVal.parent = "CLAN"; 
                } else if (fRec.isPeriphery()) {
                    retVal.parent = "PERIPHERY"; 
                } else {
                    retVal.parent = "IS";
                }
                if (retVal.faction.equals(retVal.parent)) {
                    retVal.parent = null;
                }
            }
        }
        //Rating system defaults to IS if not present. If present but cannot be parsed, is set to NONE.
        if (elem.getAttribute("ratingSystem").length() > 0) {
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
                        DefaultMmLogger.getInstance().error(Ruleset.class, METHOD_NAME,
                                "In file " + f.getName() + " while processing force node" + //$NON-NLS-1$
                                        ((wn.getAttributes().getNamedItem("eschName") == null)?"": //$NON-NLS-1$
                                            " " + wn.getAttributes().getNamedItem("eschName")) + //$NON-NLS-1$
                                        ": " + ex.getMessage()); //$NON-NLS-1$
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
