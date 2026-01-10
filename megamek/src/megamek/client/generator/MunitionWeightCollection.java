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

package megamek.client.generator;

import static java.util.Map.entry;
import static megamek.client.generator.TeamLoadOutGenerator.castPropertyDouble;
import static megamek.client.generator.TeamLoadOutGenerator.searchMap;
import static megamek.common.equipment.AmmoType.INCENDIARY_MOD;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import megamek.codeUtilities.MathUtility;
import megamek.common.annotations.Nullable;
import megamek.common.containers.MunitionTree;

// region MunitionWeightCollection
class MunitionWeightCollection {
    private HashMap<String, Double> lrmWeights;
    private HashMap<String, Double> srmWeights;
    private HashMap<String, Double> acWeights;
    private HashMap<String, Double> atmWeights;
    private HashMap<String, Double> iatmWeights;
    private HashMap<String, Double> arrowWeights;
    private HashMap<String, Double> longtomWeights;
    private HashMap<String, Double> sniperWeights;
    private HashMap<String, Double> thumperWeights;
    private HashMap<String, Double> longtomCannonWeights;
    private HashMap<String, Double> sniperCannonWeights;
    private HashMap<String, Double> thumperCannonWeights;
    private HashMap<String, Double> mortarWeights;
    private HashMap<String, Double> narcWeights;
    private HashMap<String, Double> bombWeights;
    private Map<String, HashMap<String, Double>> mapTypeToWeights;
    private final String factionName;
    private final boolean clan;

    // Default constructor, for backwards compatibility
    MunitionWeightCollection() {
        factionName = "IS";
        clan = false;
        resetWeights();
    }

    // For new default handling
    MunitionWeightCollection(String factionName, boolean clan) {
        this.factionName = factionName;
        this.clan = clan;
        resetWeights();
    }

    public void resetWeights() {
        // Initialize weights for all the weapon types using known munition names
        lrmWeights = setIncendiary(initializeWeaponWeightsYAML("LRM", factionName, clan,
              MunitionTree.LRM_MUNITION_NAMES));
        srmWeights = initializeWeaponWeightsYAML("SRM", factionName, clan, MunitionTree.SRM_MUNITION_NAMES);
        acWeights = initializeWeaponWeightsYAML( "AC", factionName, clan, MunitionTree.AC_MUNITION_NAMES);
        arrowWeights = initializeWeaponWeightsYAML("Arrow IV", factionName, clan, MunitionTree.ARROW_MUNITION_NAMES);
        longtomWeights = initializeWeaponWeightsYAML("Long Tom", factionName, clan,
              MunitionTree.ARTILLERY_MUNITION_NAMES);
        sniperWeights = initializeWeaponWeightsYAML("Sniper", factionName, clan,
              MunitionTree.ARTILLERY_MUNITION_NAMES);
        thumperWeights = initializeWeaponWeightsYAML("Thumper", factionName, clan,
              MunitionTree.ARTILLERY_MUNITION_NAMES);
        longtomCannonWeights = initializeWeaponWeightsYAML("Long Tom Cannon", factionName, clan,
              MunitionTree.ARTILLERY_CANNON_MUNITION_NAMES);
        sniperCannonWeights = initializeWeaponWeightsYAML("Sniper Cannon", factionName, clan,
              MunitionTree.ARTILLERY_CANNON_MUNITION_NAMES);
        thumperCannonWeights = initializeWeaponWeightsYAML("Thumper Cannon", factionName, clan,
              MunitionTree.ARTILLERY_CANNON_MUNITION_NAMES);
        atmWeights = initializeWeaponWeightsYAML("ATM", factionName, clan, MunitionTree.ATM_MUNITION_NAMES);
        iatmWeights = initializeWeaponWeightsYAML("iATM", factionName, clan, MunitionTree.iATM_MUNITION_NAMES);
        mortarWeights = initializeWeaponWeightsYAML("Mortar", factionName, clan,
              MunitionTree.MORTAR_MUNITION_NAMES);
        narcWeights = initializeWeaponWeightsYAML("Narc", factionName, clan, MunitionTree.NARC_MUNITION_NAMES);
        bombWeights = initializeWeaponWeightsYAML("Bomb", factionName, clan, MunitionTree.BOMB_MUNITION_NAMES);

        mapTypeToWeights = new HashMap<>(Map.ofEntries(entry("LRM", lrmWeights),
              entry("SRM", srmWeights),
              entry("AC", acWeights),
              entry("ATM", atmWeights),
              entry("iATM", iatmWeights),
              entry("Arrow IV", arrowWeights),
              entry("Long Tom", longtomWeights),
              entry("Sniper", sniperWeights),
              entry("Thumper", thumperWeights),
              entry("Long Tom Cannon", longtomCannonWeights),
              entry("Sniper Cannon", sniperCannonWeights),
              entry("Thumper Cannon", thumperCannonWeights),
              entry("Mortar", mortarWeights),
              entry("Narc", narcWeights),
              entry("Bomb", bombWeights)));
    }

    /**
     * We want to be able to search for valid LRM munitions types that also include 'w/ Incendiary',
     * without having to track every IS/Clan, rack size, Mek/BA/Infantry weapon, OS/not OS, etc.
     * combination.
     *
     * For now, MunitionTree adds a version of each ammo type string with ' w/ Incendiary' appended;
     * here we set the weights at 0.8 x the original value (representing 20% damage loss from Incendiary).
     * @param original
     * @return original, but modified
     */
    public HashMap<String, Double> setIncendiary(HashMap<String, Double> original) {
        HashMap<String, Double> withIncendiary = new HashMap<>();

        for (Map.Entry<String, Double> entry : original.entrySet()) {
            // Skip the top-level Incendiary class, which is needed for weight calcs.
            if (entry.getKey().equals("Incendiary")) {
                continue;
            } else if (!entry.getKey().contains(INCENDIARY_MOD)) {
                withIncendiary.put(
                      entry.getKey() + " " + INCENDIARY_MOD,
                      entry.getValue() *
                            castPropertyDouble("Defaults.Factors.mwcDefaultIncendiaryModWeight", 0.8)
                );
            }
        }

        // Update all " w/ Incendiary" entries
        original.putAll(withIncendiary);

        return original;
    }

    /**
     * Use values from the Properties file defined in TeamLoadOutGenerator class if available; else use provided
     * default
     *
     * @param field    Dotted field path in YAML file
     * @param defValue Default value to use
     *
     * @return Double read value or default
     */
    private static Double getPropDouble(String field, Double defValue) {
        return TeamLoadOutGenerator.castPropertyDouble(field, defValue);
    }

    // Section: initializing weights

    /**
     *
     * @param weapon        Weapon type name, matches YAML Defaults.Munitions.* keys
     * @param factionName   The Faction for which this weight collection will be generated, or "IS" or "Clan"
     * @param clan          Whether the Faction counts as a Clan faction (see YAML entries)
     * @param munitionsList The list of munitions to create weight values over
     * @return HashMap      A map of munition types to weights for this weapon
     */
    private static HashMap<String, Double> initializeWeaponWeightsYAML(String weapon,
          String factionName, boolean clan, List<String> munitionsList) {
        HashMap<String, Double> weights = new HashMap<>();
        String basePath;

        for (String name : munitionsList) {
            basePath = String.format("Defaults.Munitions.%s.%s.%s", weapon, name, (clan) ? "Clan" : "IS");
            double weight = getPropDouble(basePath, getPropDouble("Defaults.Munitions.Weight", 1.0));

            try {
                // Check for a faction-specific default value under the base path
                  weight = (double) searchMap(String.format("%s.%s", basePath, factionName));
            } catch (Exception e) {
                // Not found; use Any as a fallback
                try {
                    weight = (double) searchMap(String.format("%s.%s", basePath, "Any"));
                } catch (Exception e2) {
                    // Ignore and use the default value
                }
            }
            weights.put(name, weight);
        }

        return weights;
    }

    // Increase/Decrease functions. Increase is 2x + 1, decrease is 0.5x, so items voted up and down multiple times
    // should still exceed items never voted up _or_ down.
    public void increaseMunitions(List<String> munitions) {
        mapTypeToWeights.forEach((key, value) -> modifyMatchingWeights(value,
              munitions,
              getPropDouble("Defaults.Factors.increaseWeightFactor", 2.0),
              getPropDouble("Defaults.Factors.increaseWeightIncrement", 1.0)));
    }

    public void decreaseMunitions(List<String> munitions) {
        mapTypeToWeights.forEach((key, value) -> modifyMatchingWeights(value,
              munitions,
              getPropDouble("Defaults.Factors.decreaseWeightFactor", 0.5),
              getPropDouble("Defaults.Factors.decreaseWeightDecrement", -1.0)));
    }

    public void zeroMunitionsWeight(List<String> munitions) {
        mapTypeToWeights.forEach((key, value) -> modifyMatchingWeights(value, munitions, 0.0, 0.0));
    }

    public void increaseAPMunitions() {
        increaseMunitions(TeamLoadOutGenerator.AP_MUNITIONS);
    }

    public void decreaseAPMunitions() {
        decreaseMunitions(TeamLoadOutGenerator.AP_MUNITIONS);
    }

    public void increaseFlakMunitions() {
        increaseMunitions(TeamLoadOutGenerator.FLAK_MUNITIONS);
    }

    public void decreaseFlakMunitions() {
        decreaseMunitions(TeamLoadOutGenerator.FLAK_MUNITIONS);
    }

    public void increaseAccurateMunitions() {
        increaseMunitions(TeamLoadOutGenerator.ACCURATE_MUNITIONS);
    }

    public void decreaseAccurateMunitions() {
        decreaseMunitions(TeamLoadOutGenerator.ACCURATE_MUNITIONS);
    }

    public void increaseAntiInfMunitions() {
        increaseMunitions(TeamLoadOutGenerator.ANTI_INF_MUNITIONS);
    }

    public void decreaseAntiInfMunitions() {
        decreaseMunitions(TeamLoadOutGenerator.ANTI_INF_MUNITIONS);
    }

    public void increaseAntiBAMunitions() {
        increaseMunitions(TeamLoadOutGenerator.ANTI_BA_MUNITIONS);
    }

    public void decreaseAntiBAMunitions() {
        decreaseMunitions(TeamLoadOutGenerator.ANTI_BA_MUNITIONS);
    }

    public void increaseHeatMunitions() {
        increaseMunitions(TeamLoadOutGenerator.HEAT_MUNITIONS);
    }

    public void decreaseHeatMunitions() {
        decreaseMunitions(TeamLoadOutGenerator.HEAT_MUNITIONS);
    }

    public void increaseIlluminationMunitions() {
        increaseMunitions(TeamLoadOutGenerator.ILLUMINATION_MUNITIONS);
    }

    public void decreaseIlluminationMunitions() {
        decreaseMunitions(TeamLoadOutGenerator.ILLUMINATION_MUNITIONS);
    }

    public void increaseUtilityMunitions() {
        increaseMunitions(TeamLoadOutGenerator.UTILITY_MUNITIONS);
    }

    public void decreaseUtilityMunitions() {
        decreaseMunitions(TeamLoadOutGenerator.UTILITY_MUNITIONS);
    }

    public void increaseArtilleryUtilityMunitions() {
        for (String weaponType: List.of("Long Tom", "Sniper", "Thumper")) {
            modifyMatchingWeights(mapTypeToWeights.get(weaponType),
                  TeamLoadOutGenerator.ARTILLERY_UTILITY_MUNITIONS,
                  getPropDouble("Defaults.Factors.increaseWeightFactor", 2.0),
                  getPropDouble("Defaults.Factors.increaseWeightIncrement", 1.0));
        }
    }

    public void decreaseArtilleryUtilityMunitions() {
        for (String weaponType: List.of("Long Tom", "Sniper", "Thumper")) {
            modifyMatchingWeights(mapTypeToWeights.get(weaponType),
                  TeamLoadOutGenerator.ARTILLERY_UTILITY_MUNITIONS,
                  getPropDouble("Defaults.Factors.decreaseWeightFactor", 0.5),
                  getPropDouble("Defaults.Factors.decreaseWeightDecrement", -1.0));
        }
    }

    public void increaseGuidedMunitions() {
        increaseMunitions(TeamLoadOutGenerator.GUIDED_MUNITIONS);
    }

    public void increaseTagGuidedMunitions() {
        increaseMunitions(TeamLoadOutGenerator.TAG_GUIDED_MUNITIONS);
    }

    public void increaseNARCGuidedMunitions() {
        increaseMunitions(TeamLoadOutGenerator.NARC_GUIDED_MUNITIONS);
    }

    public void decreaseGuidedMunitions() {
        decreaseMunitions(TeamLoadOutGenerator.GUIDED_MUNITIONS);
    }

    public void decreaseTagGuidedMunitions() {
        decreaseMunitions(TeamLoadOutGenerator.TAG_GUIDED_MUNITIONS);
    }

    public void decreaseNARCGuidedMunitions() {
        decreaseMunitions(TeamLoadOutGenerator.NARC_GUIDED_MUNITIONS);
    }

    public void increaseAmmoReducingMunitions() {
        increaseMunitions(TeamLoadOutGenerator.AMMO_REDUCING_MUNITIONS);
    }

    public void decreaseAmmoReducingMunitions() {
        decreaseMunitions(TeamLoadOutGenerator.AMMO_REDUCING_MUNITIONS);
    }

    public void increaseSeekingMunitions() {
        increaseMunitions(TeamLoadOutGenerator.SEEKING_MUNITIONS);
    }

    public void decreaseSeekingMunitions() {
        decreaseMunitions(TeamLoadOutGenerator.SEEKING_MUNITIONS);
    }

    public void increaseHighPowerMunitions() {
        increaseMunitions(TeamLoadOutGenerator.HIGH_POWER_MUNITIONS);
    }

    public void decreaseHighPowerMunitions() {
        decreaseMunitions(TeamLoadOutGenerator.HIGH_POWER_MUNITIONS);
    }

    /**
     * Update all matching types in a category by multiplying by a factor and adding an increment (1.0, 0.0) = no
     * change; (2.0, 0.0) = double, (0.5, 0.0) = halve, (1.0, 1.0) = increment by 1, (1.0, -1.0) = decrement by 1, etc.
     *
     */
    private static void modifyMatchingWeights(HashMap<String, Double> current, List<String> types, double factor,
          double increment) {
        for (String key : types) {
            if (current.containsKey(key)) {
                current.put(key, current.get(key) * factor + increment);
            }
        }
    }

    public List<String> getMunitionTypesInWeightOrder(Map<String, Double> weightMap) {
        ArrayList<String> orderedTypes = new ArrayList<>();
        weightMap.entrySet()
              .stream()
              .sorted((E1, E2) -> E2.getValue().compareTo(E1.getValue()))
              .forEach(k -> orderedTypes.add(String.valueOf(k)));
        // Make Standard the first entry if tied with other highest-weight munitions
        for (String munitionString : orderedTypes) {
            if (munitionString.contains("Standard") && !orderedTypes.get(0).contains("Standard")) {
                int idx = orderedTypes.indexOf(munitionString);
                if (weightMap.get("Standard") == Double.parseDouble(orderedTypes.get(0).split("=")[1])) {
                    Collections.swap(orderedTypes, idx, 0);
                    break;
                }
            }
        }
        return orderedTypes;
    }

    /**
     * Select all munitions with weights above the cutoff value.
     * @param cutoff    Double cutoff weight; munitions with this weight or lower are discarded
     * @return  HashMap of lists of munitions, by weapon type, in weight order
     */
    public HashMap<String, List<String>> getAboveCutoff(double cutoff) {
        HashMap<String, List<String>> topMunitionsMap = new HashMap<>();
        for (String key : TeamLoadOutGenerator.TYPE_MAP.keySet()) {
            List<String> orderedList = getMunitionTypesInWeightOrder(mapTypeToWeights.get(key));
            topMunitionsMap.put(
                  key,
                  orderedList.stream()
                        .filter(m -> MathUtility.parseDouble(m.split("=")[1], 0.0) > cutoff)
                        .collect(Collectors.toList())
            );
        }
        return topMunitionsMap;
    }

    /**
     * Select the top N weighted munitions of each type, where N is the count to include.
     * @param count     Int count of top munition types to include.
     * @return  HashMap of lists of munitions, by weapon type, in weight order
     */
    public HashMap<String, List<String>> getTopN(int count) {
        HashMap<String, List<String>> topMunitionsMap = new HashMap<>();
        for (String key : TeamLoadOutGenerator.TYPE_MAP.keySet()) {
            List<String> orderedList = getMunitionTypesInWeightOrder(mapTypeToWeights.get(key));
            topMunitionsMap.put(key, (orderedList.size() >= count) ? orderedList.subList(0, count) : orderedList);
        }
        return topMunitionsMap;
    }

    public HashMap<String, Double> getLrmWeights() {
        return lrmWeights;
    }

    public HashMap<String, Double> getSrmWeights() {
        return srmWeights;
    }

    public HashMap<String, Double> getAcWeights() {
        return acWeights;
    }

    public HashMap<String, Double> getAtmWeights() {
        return atmWeights;
    }

    public HashMap<String, Double> getArrowWeights() {
        return arrowWeights;
    }

    public HashMap<String, Double> getLongTomWeights() {
        return longtomWeights;
    }

    public HashMap<String, Double> getSniperWeights() {
        return sniperWeights;
    }

    public HashMap<String, Double> getThumperWeights() {
        return thumperWeights;
    }

    public HashMap<String, Double> getBombWeights() {
        return bombWeights;
    }

    public HashMap<String, Double> getLongTomCannonWeights() {
        return longtomCannonWeights;
    }

    public HashMap<String, Double> getSniperCannonWeights() {
        return sniperCannonWeights;
    }

    public HashMap<String, Double> getThumperCannonWeights() {
        return thumperCannonWeights;
    }

    public HashMap<String, Double> getMortarWeights() {
        return mortarWeights;
    }

    @Nullable
    public HashMap<String, Double> getMunitionWeights(String key) {
        return switch(key) {
            case "LRM" -> lrmWeights;
            case "SRM" -> srmWeights;
            case "AC" -> acWeights;
            case "ATM" -> atmWeights;
            case "iATM" -> iatmWeights;
            case "Arrow IV" -> arrowWeights;
            case "Long Tom" -> longtomWeights;
            case "Sniper" -> sniperWeights;
            case "Thumper" -> thumperWeights;
            case "Long Tom Cannon" -> longtomCannonWeights;
            case "Sniper Cannon" -> sniperCannonWeights;
            case "Thumper Cannon" -> thumperCannonWeights;
            case "Mortar" -> mortarWeights;
            case "Narc" -> narcWeights;
            case "Bomb" -> bombWeights;
            default -> null;
        };
    }
}
