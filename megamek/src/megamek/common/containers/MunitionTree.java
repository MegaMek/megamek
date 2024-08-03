/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
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

package megamek.common.containers;

import megamek.common.AmmoType;
import megamek.common.BombType;
import megamek.common.Entity;
import megamek.common.Mounted;
import org.apache.logging.log4j.LogManager;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MunitionTree {
    // Validated munition names that will work in ADF files.
    // TODO: validate all these strings!
    public static final ArrayList<String> LRM_MUNITION_NAMES = new ArrayList<>(List.of(
            "Dead-Fire", "Standard", "Swarm-I", "Swarm", "Heat-Seeking", "Semi-guided",
            "Artemis-capable", "Narc-capable", "Follow The Leader", "Fragmentation",
            "Thunder", "Thunder-Active", "Thunder-Augmented", "Thunder-Vibrabomb",
            "Thunder-Inferno", "Anti-TSM", "Listen-Kill", "Smoke", "Mine Clearance"
    ));

    public static final ArrayList<String> SRM_MUNITION_NAMES = new ArrayList<>(List.of(
            "Dead-Fire", "Standard", "Tandem-Charge", "Inferno", "Heat-Seeking",
            "Artemis-capable", "Narc-capable", "Fragmentation", "Acid",
            "Anti-TSM", "Listen-Kill", "Mine Clearance", "Smoke"
    ));

    public static final ArrayList<String> AC_MUNITION_NAMES = new ArrayList<>(List.of(
            "Precision", "Standard", "Armor-Piercing", "Caseless", "Flak", "Tracer", "Flechette"
    ));

    public static final ArrayList<String> ATM_MUNITION_NAMES = new ArrayList<>(List.of(
            "HE", "ER", "Standard"
    ));

    public static final ArrayList<String> ARROW_MUNITION_NAMES = new ArrayList<>(List.of(
            "Fuel-Air", "Standard", "ADA", "Cluster", "Inferno-IV", "Homing",
            "Thunder", "Thunder Vibrabomb-IV", "Illumination", "Smoke", "Laser Inhibiting", "Davy Crockett-M"
    ));

    public static final ArrayList<String> ARTILLERY_MUNITION_NAMES = new ArrayList<>(List.of(
            "Fuel-Air", "Standard", "Cluster", "Copperhead",
            "FASCAM", "Flechette", "Illumination", "Smoke", "Davy Crockett-M"

    ));

    public static final ArrayList<String> ARTILLERY_CANNON_MUNITION_NAMES = new ArrayList<>(List.of(
            "Fuel-Air", "Standard"
    ));

    public static final ArrayList<String> MEK_MORTAR_MUNITION_NAMES = new ArrayList<>(List.of(
            "Standard", "Semi-Guided", "Anti-personnel", "Airburst", "Flare", "Smoke"
    ));

    public static final ArrayList<String> NARC_MUNITION_NAMES = new ArrayList<>(List.of(
            "Narc Explosive", "Standard"
    ));

    // Shorter, guaranteed to work in lookups
    public static final ArrayList<String> BOMB_MUNITION_NAMES = new ArrayList<>(
            Arrays.asList(BombType.bombInternalNames)
    );

    private static String HEADER = String.join(
            System.getProperty("line.separator"),
            "# ADF (Autoconfiguration Data File) from MegaMek.",
            "# Lines are formatted as",
            "#      '<Chassis>:<Model>:<Pilot>::<Weapon type>:Muntion1[:Munition2[:...]]][::AmmoType2...]'",
            "# Values for <Chassis>, <Model>, <Pilot>, and <Weapon Type> may be 'any', or actual values.",
            "# Values for <Weapon Type> may also be specific or general, e.g. 'AC/20' ~ 'AC', 'SRM6' ~ 'SRM'",
            "# e.g. 'Shadow Hawk:any:Grayson Carlyle::LRM:Swarm::SRM:Inferno::AC:Precision:Flak'.",
            "# ",
            "# Left-most Munition is highest priority; if any ammo slots are unaccounted for, they will be filled",
            "# with this munition type (unless it is invalid for the time / faction, in which case the next valid",
            "# Munition type will be used).  If no imperative matches, the current munitions will be left in place.",
            "# An 'any:any:any::...' directive will be applied to any and all units, but is superseded by more",
            "# particular entries.  If no match is found at the <Pilot> level, the 'any' entry at that level will",
            "# be tried first, and onwards up until 'any:any:any' (if defined).",
            "######################################################################################################"
    );

    private LoadNode root = new LoadNode();

    public MunitionTree() {
    }

    public MunitionTree(MunitionTree mt) {
        this.root = new LoadNode(mt.root);
    }

    /**
     * Constructor for reading in files containing loadout imperatives.
     * @param fd
     */
    public MunitionTree(String fName) throws IllegalArgumentException {
        File fd = new File(fName);
        if (fd.canRead() && fd.exists()) {
            try (FileReader fr = new FileReader(fName)) {
                if (fd.getAbsoluteFile().toString().toLowerCase().endsWith("adf")) {
                    readFromADF(new BufferedReader(fr));
                } else if (fd.getAbsoluteFile().toString().toLowerCase().endsWith("xml")) {
                    readFromXML(new BufferedReader(fr));
                } else {
                    throw new IllegalArgumentException("Invalid filename: " + fName);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * TODO: Implement
     * @param br
     */
    public void readFromXML(BufferedReader br) {

    }

    public void readFromADFFilename(String fName) {
        try (BufferedReader br = new BufferedReader(new FileReader(fName))){
            readFromADF(br);
        } catch (FileNotFoundException e) {
            LogManager.getLogger().error("File not found: " + fName, e);
        } catch (IOException e) {
            LogManager.getLogger().error("Failed to read file: " + fName, e);
        }
    }

    public void readFromADF(BufferedReader br) throws IOException {
        for (String line = br.readLine(); line != null; line = br.readLine()) {
            // Ignore comments
            if (line.startsWith("#") || line.isBlank() || line.isEmpty()) {
                continue;
            }

            try {
                String[] parts = line.split("::");
                String[] keys = parts[0].split(":");
                HashMap<String, String> imperatives = new HashMap<>();
                String imperative;

                for (int idx = 1; idx < parts.length; idx++) {
                    // Populate imperatives by splitting at first ":" instance
                    imperative = parts[idx];
                    imperatives.put(
                            imperative.substring(0, imperative.indexOf(':')),
                            imperative.substring(imperative.indexOf(':') + 1)
                    );
                }
                insertImperatives(keys[0], keys[1], keys[2], imperatives);
            } catch (IndexOutOfBoundsException e) {
                LogManager.getLogger().error("Failed to read an imperative!", e);
            }
        }
    }

    public void writeToADFFilename(String fName) {
        File fd = new File(fName);
        if (!fd.exists()) {
            try {
                fd.createNewFile();
            } catch (IOException e) {
                LogManager.getLogger().error("Failed to create new file: " + fName, e);
                return;
            }
        }

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(fName))) {
            writeToADFFormat(bw);
        } catch (FileNotFoundException e) {
            LogManager.getLogger().error("File not found: " + fName, e);
        } catch (IOException e) {
            LogManager.getLogger().error("Failed to write file: " + fName, e);
        }
    }

    public void writeToADFFormat(BufferedWriter bw) throws IOException {
        bw.write(HEADER);
        bw.write(System.getProperty("line.separator"));
        bw.write(System.getProperty("line.separator"));
        bw.flush();
        bw.write(root.dumpTextFormat().toString());
        bw.flush();
    }

    /**
     * Convert List of Entities into a set of specific imperatives for each unit.
     * Used for backing up original loadout.
     * @param el
     */
    public void loadEntityList(ArrayList<Entity> el) {
        for (Entity e: el) {
            HashMap<String, String> imperatives = new HashMap<>();
            for (Mounted m : e.getAmmo()) {
                AmmoType aType = (AmmoType) m.getType();
                String baseName = aType.getBaseName();
                String sName = aType.getShortName();
                String munition = (aType.getSubMunitionName().equals(baseName)) ? "Standard" : aType.getSubMunitionName();
                if (!(imperatives.containsKey(baseName))) {
                    imperatives.put(baseName, munition);
                } else {
                    imperatives.put(baseName, imperatives.get(baseName) + ':' + munition);
                }
            }
            root.insert(imperatives, e.getFullChassis(), e.getModel(), e.getCrew().getName(0));
        }
    }

    // Can take multiple separate ammoType strings (e.g. "Standard", "HE", "ER") or one
    // pre-defined imperative set in priority order ("Standard:HE:ER")
    public void insertImperative(
            String chassis, String variant, String pilot, String binType, String... ammoTypes) throws IllegalArgumentException {

        // Need ammoTypes populated
        if (ammoTypes.length == 0) {
            throw new IllegalArgumentException("Must include at least one munition type (e.g. 'Standard'");
        }

        HashMap<String, String> imperatives = new HashMap<>();

        imperatives.put(binType, String.join(":",ammoTypes));
        insertImperatives(chassis, variant, pilot, imperatives);
    }

    public void insertImperatives(
            String chassis, String variant, String pilot, HashMap<String, String> imperatives){

        // Start insertions from root
        root.insert(imperatives, chassis, variant, pilot);
    }

    public void insertMangledImperatives(
            String chassis, String variant, String pilot, HashMap<String, String> imperatives){
        // switch imperative keys to lowercase to avoid case-based matching issues
        // strip out extraneous characters for ammo with sizes, e.g. LRM[ -/]15 -> LRM15
        HashMap<String, String> lcImp = new HashMap<String, String>(imperatives.size());
        for (Map.Entry<String, String> e: imperatives.entrySet()) {
            lcImp.put(e.getKey().toLowerCase().replaceAll(LoadNode.SIZE_REGEX, ""), e.getValue());
        }

        // Start insertions from root
        root.insert(lcImp, chassis, variant, pilot);
    }

    public HashMap<String, Integer> getCountsOfAmmosForKey(
        String chassis, String variant, String pilot, String binType) {
        return root.retrieveAmmoCounts(chassis, variant, pilot, binType);
    }

    public List<String> getPriorityList(
            String chassis, String variant, String pilot, String binType) {
        return root.retrievePriorityList(chassis, variant, pilot, binType);
    }

    /**
     * Return the entire imperative string that would act on the provided key set
     * @param chassis
     * @param variant
     * @param pilot
     * @param binType
     * @return
     */
    public String getEffectiveImperative(
        String chassis, String variant, String pilot, String binType) {
        LoadNode node = root.retrieve(chassis, variant, pilot);
        if (null != node) {
            return node.getImperative(binType).get(1);
        }
        return "";
    }

    /**
     * Return the actual, or effective, desired count of ammo bins for the given binType and ammoType
     * @param chassis
     * @param variant
     * @param pilot
     * @param binType
     * @param ammoType
     * @return int count of bins requested for this binType:ammoType set.
     */
    public int getCountOfAmmoForKey(
            String chassis, String variant, String pilot, String binType, String ammoType) {
        return root.retrieveAmmoCount(chassis, variant, pilot, binType, ammoType);
    }
}


// Node used to construct munition loading tree
class LoadNode {
    private HashMap<String, LoadNode> children = new HashMap<String, LoadNode>();
    // Imperatives in the form of: "<binType/weaponType>:<desiredAmmo1>[:desiredAmmo2[:...]]"
    private HashMap<String, String> imperatives = new HashMap<String, String>();
    private HashMap<String, HashMap<String, Integer>> counts = new HashMap<String, HashMap<String, Integer>>();
    private boolean dirty = false;

    public static String SIZE_REGEX = "[ -/\\\\]?(\\d{1,3})";
    public static Pattern SIZE_PATTERN = Pattern.compile(SIZE_REGEX);
    public static String LAC_REGEX = "([Ll][-/\\\\]?)(ac|AC)";
    public static Pattern LAC_PATTERN = Pattern.compile(LAC_REGEX);
    public static String ANY_KEY = "any";

    LoadNode() {
    }

    LoadNode(LoadNode ln) {
        // Copy all children in ln
        for (Map.Entry<String, LoadNode> e : ln.children.entrySet()) {
            children.put(e.getKey(), new LoadNode(e.getValue()));
        }

        // Copy all Imperatives in ln
        imperatives.putAll(ln.imperatives);

        // Set dirty
        dirty = true;
    }

    /**
     * Testing version of LoadNode, for imperative lookups
     * @param imperatives
     */
    LoadNode(HashMap<String, String> imperatives) {
        this.imperatives = imperatives;
    }

    /**
     * LoadNode that recursively populates a leaf of the lookup tree.
     * @param imperatives
     * @param keys
     */
    LoadNode(HashMap<String, String> imperatives, String... keys) {
        if (keys.length > 0){
            children.put(keys[0], new LoadNode(imperatives, Arrays.copyOfRange(keys, 1, keys.length)));
        } else {
            updateImperatives(imperatives);
        }
    }

    private void updateImperatives(HashMap<String, String> imperatives) {
        for (String key: imperatives.keySet()) {
            if (this.imperatives.containsKey(key)) {
                dirty = true;
            }
        }
        this.imperatives.putAll(imperatives);
    }

    /**
     * Utilizes recursion and variable length argument list to insert a set of imperatives at arbitrary depth
     * @param imperatives
     * @param keys
     */
    public void insert(HashMap<String, String> imperatives, String... keys) {
        if (keys.length > 0) {
            if (children.containsKey(keys[0])) {
                children.get(keys[0]).insert(imperatives, Arrays.copyOfRange(keys, 1, keys.length));
            } else {
                LoadNode child = new LoadNode(imperatives, Arrays.copyOfRange(keys, 1, keys.length));
                children.put(keys[0], child);
            }
        } else {
            updateImperatives(imperatives);
        }
    }

    public LoadNode retrieve(String... keys) {
        // Recursive retrieval method.  Either:
        // 1. we are the end of the chain (keys.length == 0), or
        // 2. we contain the first key, or
        // 2.5 we contain the first key but it returns no results, so
        // 3. we contain an "any" entry, or
        // 4. we return null (no matches)
        LoadNode ln = null;
        if (keys.length == 0){
            ln = this;
        } else if (children.containsKey(keys[0])){
            ln = children.get(keys[0]).retrieve(Arrays.copyOfRange(keys, 1, keys.length));

            // Found a defined branch without a match or an "any" so try our own "any" branch
            if (ln == null && children.containsKey(ANY_KEY)) {
                ln = children.get(ANY_KEY).retrieve(Arrays.copyOfRange(keys, 1, keys.length));
            }
        } else if (children.containsKey(ANY_KEY)){
            ln = children.get(ANY_KEY).retrieve(Arrays.copyOfRange(keys, 1, keys.length));
        }
        return ln;
    }

    public HashMap<String, Integer> retrieveAmmoCounts(String... keys) {
        HashMap<String, Integer> retval = new HashMap<>();
        if (keys.length == 1) {
            retval = getCounts(keys[0]);
        } else if (children.containsKey(keys[0])) {
            retval = children.get(keys[0]).retrieveAmmoCounts(Arrays.copyOfRange(keys, 1, keys.length));
            if (retval.isEmpty() && children.containsKey(ANY_KEY)) {
                retval = children.get(ANY_KEY).retrieveAmmoCounts(Arrays.copyOfRange(keys, 1, keys.length));
            }
        } else if (children.containsKey(ANY_KEY)) {
            retval = children.get(ANY_KEY).retrieveAmmoCounts(Arrays.copyOfRange(keys, 1, keys.length));
        }
        return retval;
    }

    public List<String> retrievePriorityList(String... keys) {
        List<String> retval = new ArrayList<>();
        if (keys.length == 1) {
            retval = getImperativeOrder(keys[0]);
        } else if (children.containsKey(keys[0])) {
            retval = children.get(keys[0]).retrievePriorityList(Arrays.copyOfRange(keys, 1, keys.length));
            if (retval.isEmpty() && children.containsKey(ANY_KEY)) {
                retval = children.get(ANY_KEY).retrievePriorityList(Arrays.copyOfRange(keys, 1, keys.length));
            }
        } else if (children.containsKey(ANY_KEY)) {
            retval = children.get(ANY_KEY).retrievePriorityList(Arrays.copyOfRange(keys, 1, keys.length));
        }
        return retval;
    }

    public int retrieveAmmoCount(String... keys) {
        int retval = 0;
        if (keys.length == 2) {
            retval = getCount(keys[0], keys[1]);
        } else if (children.containsKey(keys[0])) {
            retval = children.get(keys[0]).retrieveAmmoCount(Arrays.copyOfRange(keys, 1, keys.length));
            if (retval == 0 && children.containsKey(ANY_KEY)) {
                retval = children.get(ANY_KEY).retrieveAmmoCount(Arrays.copyOfRange(keys, 1, keys.length));
            }
        } else if (children.containsKey(ANY_KEY)) {
            retval = children.get(ANY_KEY).retrieveAmmoCount(Arrays.copyOfRange(keys, 1, keys.length));
        }
        return retval;
    }

    /**
     * Does the string conversions necessary to look up "parent" types, e.g AC for AC-20 (or LAC-5)
     * @param binType
     * @return
     */
    public List<String> getImperative(String binType) {
        // Found the raw string provided
        if (imperatives.containsKey(binType)) {
            return Arrays.asList(binType, imperatives.get(binType));
        }

        // Otherwise, try various combinations
        // Separate out size
        Matcher sizeM = SIZE_PATTERN.matcher(binType);
        final String size = (sizeM.find()) ? sizeM.group(1) : "";
        Matcher lacM = LAC_PATTERN.matcher(binType);
        final String lac = (lacM.find()) ? lacM.group(1) : "";
        final String base = (lac.isBlank()) ? binType.replaceAll(SIZE_REGEX, "") : lacM.group(2);

        List<String> candidates = new ArrayList<>();
        candidates.addAll(List.of(base, binType.toLowerCase(), base.toLowerCase()));
        if (!size.isBlank()) {
            for (Character c : " -/\\".toCharArray()) {
                candidates.add(base + c + size);
                candidates.add(base.toLowerCase() + c + size);
            }
        }
        if (!lac.isBlank()) {
            for (String l : List.of("L", "l", "L/", "l/", "L-", "l-")) {
                candidates.add(l + base);
                candidates.add(l + base.toLowerCase());
            }
        }

        String actualLookup = candidates.stream().filter(candidate -> imperatives.containsKey(candidate)).findFirst().orElse(null);
        String actualImperative = imperatives.getOrDefault(actualLookup, null);
        return (actualImperative == null) ? new ArrayList<>() : Arrays.asList(actualLookup, actualImperative);
    }

    public List<String> getImperativeOrder(String binType) {
        List<String> ordering = getImperative(binType);
        List<String> orderedSetList = new ArrayList<>();
        if (!ordering.isEmpty()) {
            for (String s : ordering.get(1).split(":")) {
                if (!orderedSetList.contains(s)) {
                    orderedSetList.add(s);
                }
            }
        }
        return orderedSetList;
    }

    public int getCount(String binType, String ammoType) {
        return getCounts(binType).getOrDefault(ammoType, 0);
    }

    /**
     * Method for retrieving counts of all imperatives defined for a given binType.
     * @param binType
     * @return HashMap <String AmmoType, count of bins requested>
     */
    public HashMap<String, Integer> getCounts(String binType) {

        List<String> mapping = getImperative(binType);

        if (mapping.isEmpty()) {
            return new HashMap<>();
        }
        String realImp = mapping.get(0);
        if (!dirty && counts.containsKey(realImp)) {
            // Return pre-calculated value
            return counts.get(realImp);
        }
        // Update stored counts and reset dirty state
        counts.put(realImp, decodeImperatives(mapping.get(1)));
        dirty = false;
        return counts.get(realImp);
    }

    /**
     * Given a set of desired ammo types from an imperative, breaks the string up and
     * constructs a HashMap of <String AmmoType name, count of bins requested> Entries.
     * @param iString
     * @return HashMap c
     */
    private HashMap<String, Integer> decodeImperatives(String iString) {
        HashMap<String, Integer> c = new HashMap<String, Integer>();
        if (null != iString) {
            for (String aType : iString.split(":")) {
                c.put(aType, c.getOrDefault(aType, 0) + 1);
            }
        }
        return c;
    }

    /**
     * Recursive text format dumper.
     * Cases:
     * 1. This is a leaf.  Three keys have been passed in; these form the start of the line.
     *    Print one line starting with the three keys in "chassis:model:pilot::" format.
     *    Each line contains all imperatives in "ammoType:munition1[:munition2[...]]::ammoType2..."
     *    format.
     * 2. This is a node.  1~2 keys have been passed in; pass these to leaves.
     * 3. This is the root.  Iterate over all child keys and pass them to the children.
     * @param keys
     * @return
     */
    public StringBuilder dumpTextFormat(String... keys) {
        StringBuilder sb = new StringBuilder();
        if (keys.length == 3) {
            // Create prefix of 3 keys
            sb.append(keys[0]).append(":").append(keys[1]).append(":").append(keys[2]);
            // Combine all imperatives in one line.
            for (Map.Entry<String, String> entry : imperatives.entrySet()) {
                sb.append("::").append(entry.getKey()).append(":").append(entry.getValue());
            }
            sb.append(System.getProperty("line.separator"));
        } else {
            for (Map.Entry<String, LoadNode> entry : children.entrySet()) {
                String[] keysPlusOne = java.util.Arrays.copyOf(keys, keys.length + 1);
                keysPlusOne[keys.length] = entry.getKey();
                sb.append(entry.getValue().dumpTextFormat(keysPlusOne));
            }
        }
        return sb;
    }
}
