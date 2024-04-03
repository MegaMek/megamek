package megamek.common.containers;

import java.io.*;
import java.util.*;

public class MunitionTree {
    private LoadNode root = new LoadNode();

    public MunitionTree() {
    }

    /**
     * Constructor for reading in files containing loadout imperatives.
     * @param fd
     */
    public MunitionTree(File fd) {
        if (fd.canRead() && fd.exists()) {
            String fname = fd.getName();
            try (FileReader fr = new FileReader(fname)) {
                if (fd.getAbsoluteFile().toString().toLowerCase().endsWith("adf")) {
                    readFromADF(new BufferedReader(fr));
                } else if (fd.getAbsoluteFile().toString().toLowerCase().endsWith("xml")) {
                    readFromXML(new BufferedReader(fr));
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

    }

    public void readFromXML(BufferedReader br) {

    }

    public void readFromADF(BufferedReader br) {
        try {
            for (String line = br.readLine(); line != null; line = br.readLine()) {
                // Ignore comments
                if (line.startsWith("#")) {
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
                    continue;
                }
            }
        } catch (IOException e) {
            // do something cool
        }
    }

    public void insertImperative(
            String chassis, String variant, String pilot, String binType, String... ammoTypes){
        HashMap<String, String> imperatives = new HashMap<>();
        imperatives.put(binType.toLowerCase(), String.join(":",ammoTypes));
        insertImperatives(chassis, variant, pilot, imperatives);
    }

    public void insertImperatives(
            String chassis, String variant, String pilot, HashMap<String, String> imperatives){
        // switch imperative keys to lowercase to avoid case-based matching issues
        HashMap<String, String> lcImp = new HashMap<String, String>(imperatives.size());
        for (Map.Entry<String, String> e: imperatives.entrySet()) {
            lcImp.put(e.getKey().toLowerCase(), e.getValue());
        }

        // Start insertions from root
        root.insert(lcImp, chassis, variant, pilot);
    }

    public HashMap<String, Integer> getCountsOfAmmosForKey(
        String chassis, String variant, String pilot, String binType) {
        LoadNode node = root.retrieve(chassis, variant, pilot);
        if (null != node) {
            return node.getCounts(binType);
        }
        return new HashMap<>();
    }

    public List<String> getPriorityList(
            String chassis, String variant, String pilot, String binType) {
        LoadNode node = root.retrieve(chassis, variant, pilot);
        if (null != node) {
            return node.getImperativeOrder(binType);
        }
        return new ArrayList<>();
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
        LoadNode node = root.retrieve(chassis, variant, pilot);
        if (null != node) {
            return node.getCount(binType, ammoType);
        }
        else {
            return 0;
        }
    }
}


// Node used to construct munition loading tree
class LoadNode {
    private HashMap<String, LoadNode> children = new HashMap<String, LoadNode>();
    // Imperatives in the form of: "<binType/weaponType>:<desiredAmmo1>[:desiredAmmo2[:...]]"
    private HashMap<String, String> imperatives = new HashMap<String, String>();
    private HashMap<String, HashMap<String, Integer>> counts = new HashMap<String, HashMap<String, Integer>>();
    private boolean dirty = false;

    private String SIZE_REGEX = "[-\\\\]?(\\d{1,3})";
    private String LAC_REGEX = "l[-/\\\\]?ac";
    private String ANY_KEY = "any";

    LoadNode() {
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
        // 3. we contain an "any" entry, or
        // 3. we return null (no matches)
        if (keys.length == 0){
            return this;
        } else if (children.containsKey(keys[0])){
            return children.get(keys[0]).retrieve(Arrays.copyOfRange(keys, 1, keys.length));
        } else if (children.containsKey(ANY_KEY)){
            return children.get(ANY_KEY).retrieve(Arrays.copyOfRange(keys, 1, keys.length));
        } else {
            return null;
        }
    }

    /**
     * Does the string conversions necessary to look up "parent" types, e.g AC for AC-20 (or LAC-5)
     * @param binType
     * @return
     */
    public List<String> getImperative(String binType) {
        // all keys should be in lower-case; also check higher-level imperatives like "AC" for "AC20", etc.
        final String lbt = binType.toLowerCase();
        final String parentLBT = lbt.replaceAll(SIZE_REGEX, "");

        // Light AC imperatives need special handling
        final String lacLBT = lbt.replaceAll(LAC_REGEX, "ac");
        final String lacPLBT = lacLBT.replaceAll(SIZE_REGEX, "");

        List<String> candidates = Arrays.asList(lbt, parentLBT, lacLBT, lacPLBT);

        String actualLookup = candidates.stream().filter(candidate -> imperatives.containsKey(candidate)).findFirst().orElse(null);
        String actualImperative = imperatives.getOrDefault(actualLookup, null);
        return (actualImperative == null) ? new ArrayList<>() : Arrays.asList(actualLookup, actualImperative);
    }

    public List<String> getImperativeOrder(String binType) {
        List<String> ordering = getImperative(binType);
        return (ordering.isEmpty()) ? new ArrayList<>() : List.of(ordering.get(1).split(":"));
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
}
