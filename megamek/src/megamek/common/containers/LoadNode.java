/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.containers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Node used to construct munition loading tree
class LoadNode {
    private final HashMap<String, LoadNode> children = new HashMap<>();
    // Imperatives in the form of:
    // "<binType/weaponType>:<desiredAmmo1>[:desiredAmmo2[:...]]"
    private HashMap<String, String> imperatives = new HashMap<>();
    private final HashMap<String, HashMap<String, Integer>> counts = new HashMap<>();
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
     */
    LoadNode(HashMap<String, String> imperatives) {
        this.imperatives = imperatives;
    }

    /**
     * LoadNode that recursively populates a leaf of the lookup tree.
     */
    LoadNode(HashMap<String, String> imperatives, String... keys) {
        if (keys.length > 0) {
            children.put(keys[0], new LoadNode(imperatives, Arrays.copyOfRange(keys, 1, keys.length)));
        } else {
            updateImperatives(imperatives);
        }
    }

    private void updateImperatives(HashMap<String, String> imperatives) {
        for (String key : imperatives.keySet()) {
            if (this.imperatives.containsKey(key)) {
                dirty = true;
                break;
            }
        }
        this.imperatives.putAll(imperatives);
    }

    /**
     * Utilizes recursion and variable length argument list to insert a set of imperatives at arbitrary depth
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
        // Recursive retrieval method. Either:
        // 1. we are the end of the chain (keys.length == 0), or
        // 2. we contain the first key, or
        // 2.5 we contain the first key, but it returns no results, so
        // 3. we contain an "any" entry, or
        // 4. we return null (no matches)
        LoadNode ln = null;
        if (keys.length == 0) {
            ln = this;
        } else if (children.containsKey(keys[0])) {
            ln = children.get(keys[0]).retrieve(Arrays.copyOfRange(keys, 1, keys.length));

            // Found a defined branch without a match or an "any" so try our own "any"
            // branch
            if (ln == null && children.containsKey(ANY_KEY)) {
                ln = children.get(ANY_KEY).retrieve(Arrays.copyOfRange(keys, 1, keys.length));
            }
        } else if (children.containsKey(ANY_KEY)) {
            ln = children.get(ANY_KEY).retrieve(Arrays.copyOfRange(keys, 1, keys.length));
        }
        return ln;
    }

    public HashMap<String, Integer> retrieveAmmoCounts(String... keys) {
        HashMap<String, Integer> retValue = new HashMap<>();
        if (keys.length == 1) {
            retValue = getCounts(keys[0]);
        } else if (children.containsKey(keys[0])) {
            retValue = children.get(keys[0]).retrieveAmmoCounts(Arrays.copyOfRange(keys, 1, keys.length));
            if (retValue.isEmpty() && children.containsKey(ANY_KEY)) {
                retValue = children.get(ANY_KEY).retrieveAmmoCounts(Arrays.copyOfRange(keys, 1, keys.length));
            }
        } else if (children.containsKey(ANY_KEY)) {
            retValue = children.get(ANY_KEY).retrieveAmmoCounts(Arrays.copyOfRange(keys, 1, keys.length));
        }
        return retValue;
    }

    public List<String> retrievePriorityList(String... keys) {
        List<String> retValue = new ArrayList<>();
        if (keys.length == 1) {
            retValue = getImperativeOrder(keys[0]);
        } else if (children.containsKey(keys[0])) {
            retValue = children.get(keys[0]).retrievePriorityList(Arrays.copyOfRange(keys, 1, keys.length));
            if (retValue.isEmpty() && children.containsKey(ANY_KEY)) {
                retValue = children.get(ANY_KEY).retrievePriorityList(Arrays.copyOfRange(keys, 1, keys.length));
            }
        } else if (children.containsKey(ANY_KEY)) {
            retValue = children.get(ANY_KEY).retrievePriorityList(Arrays.copyOfRange(keys, 1, keys.length));
        }
        return retValue;
    }

    public int retrieveAmmoCount(String... keys) {
        int retValue = 0;
        if (keys.length == 2) {
            retValue = getCount(keys[0], keys[1]);
        } else if (children.containsKey(keys[0])) {
            retValue = children.get(keys[0]).retrieveAmmoCount(Arrays.copyOfRange(keys, 1, keys.length));
            if (retValue == 0 && children.containsKey(ANY_KEY)) {
                retValue = children.get(ANY_KEY).retrieveAmmoCount(Arrays.copyOfRange(keys, 1, keys.length));
            }
        } else if (children.containsKey(ANY_KEY)) {
            retValue = children.get(ANY_KEY).retrieveAmmoCount(Arrays.copyOfRange(keys, 1, keys.length));
        }
        return retValue;
    }

    /**
     * Does the string conversions necessary to look up "parent" types, e.g. AC for AC-20 (or LAC-5)
     *
     */
    public List<String> getImperative(String binType) {
        // Found the raw string provided
        if (imperatives.containsKey(binType)) {
            return Arrays.asList(binType, imperatives.get(binType));
        }

        // Otherwise, try various combinations
        // Separate out size
        List<String> candidates = getCandidatesForBinType(binType);

        String actualLookup = candidates.stream()
              .filter(candidate -> imperatives.containsKey(candidate))
              .findFirst()
              .orElse(null);
        String actualImperative = imperatives.getOrDefault(actualLookup, null);
        return (actualImperative == null) ? new ArrayList<>() : Arrays.asList(actualLookup, actualImperative);
    }

    private static List<String> getCandidatesForBinType(String binType) {
        Matcher sizeM = SIZE_PATTERN.matcher(binType);
        final String size = (sizeM.find()) ? sizeM.group(1) : "";
        Matcher lacM = LAC_PATTERN.matcher(binType);
        final String lac = (lacM.find()) ? lacM.group(1) : "";
        final String base = (lac.isBlank()) ? binType.replaceAll(SIZE_REGEX, "") : lacM.group(2);

        List<String> candidates = new ArrayList<>(List.of(base, binType.toLowerCase(), base.toLowerCase()));
        if (!size.isBlank()) {
            for (char c : " -/\\".toCharArray()) {
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
        return candidates;
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
     *
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
     * Given a set of desired ammo types from an imperative, breaks the string up and constructs a HashMap of <String
     * AmmoType name, count of bins requested> Entries.
     *
     * @return HashMap c
     */
    private HashMap<String, Integer> decodeImperatives(String iString) {
        HashMap<String, Integer> c = new HashMap<>();
        if (null != iString) {
            for (String aType : iString.split(":")) {
                c.put(aType, c.getOrDefault(aType, 0) + 1);
            }
        }
        return c;
    }

    /**
     * Recursive text format dumper. Cases: 1. This is a leaf. Three keys have been passed in; these form the start of
     * the line. Print one line starting with the three keys in "chassis:model:pilot::" format. Each line contains all
     * imperatives in "ammoType:munition1[:munition2[...]]::ammoType2..." format. 2. This is a node. 1~2 keys have been
     * passed in; pass these to leaves. 3. This is the root. Iterate over all child keys and pass them to the children.
     *
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
            sb.append(System.lineSeparator());
        } else {
            for (Map.Entry<String, LoadNode> entry : children.entrySet()) {
                String[] keysPlusOne = Arrays.copyOf(keys, keys.length + 1);
                keysPlusOne[keys.length] = entry.getKey();
                sb.append(entry.getValue().dumpTextFormat(keysPlusOne));
            }
        }
        return sb;
    }
}
