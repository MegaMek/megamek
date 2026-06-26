/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import megamek.common.compute.Compute;
import megamek.common.units.EntityWeightClass;
import megamek.logging.MMLogger;

/**
 * Reshapes a cluster's element weight classes to a per-cluster-type target after the force tree is built.
 *
 * <p>A faction file can declare {@code <weightTarget>} blocks on a CLUSTER force node (see {@link WeightTarget}).
 * This allocator runs between the build and fill phases ({@code Ruleset.processRoot}). For each unit type with a target
 * it derives an integer slot budget from the target percentages against the cluster's REAL element count (after trinary
 * collapse and ProtoMek substitution, which the built tree already reflects), then assigns the heaviest slots to the
 * stars whose formation category most "wants" heavy units. The element weight classes it writes are picked up by
 * {@code ForceDescriptor.generateFormation}, which constrains unit selection to them.</p>
 *
 * <p>Data-gated: does nothing for a cluster that declares no {@code <weightTarget>}, so a faction without targets
 * generates exactly as before. The numeric category preferences mirror the Python prototype (ratgen_sim.py
 * {@code CATEGORY_WEIGHT_PREF}); keep the two in sync.</p>
 */
final class WeightBudgetAllocator {
    private static final MMLogger logger = MMLogger.create(WeightBudgetAllocator.class);

    // Echelon levels (see forcegenerator/faction_rules/constants.txt). No shared Java enum exists, so
    // these are hardcoded with a pointer back to the constants file.
    private static final int CLUSTER_ECHELON = 6;
    private static final int STAR_ECHELON = 3;

    // Order budget tokens are handed out: heaviest first, so heavy-preferring stars fill first.
    private static final int[] WEIGHT_CLASSES_HEAVY_FIRST = {
          EntityWeightClass.WEIGHT_ASSAULT, EntityWeightClass.WEIGHT_HEAVY,
          EntityWeightClass.WEIGHT_MEDIUM, EntityWeightClass.WEIGHT_LIGHT };

    /** Preference for an unknown or unset formation category: neutral (medium). */
    static final double DEFAULT_CATEGORY_PREF = 2.5;

    // "Wants heavier units" preference per FormationType name. Single source of truth; mirrors the sim.
    private static final Map<String, Double> CATEGORY_WEIGHT_PREF = buildCategoryPref();

    private static Map<String, Double> buildCategoryPref() {
        Map<String, Double> categoryPref = new HashMap<>();
        categoryPref.put("Assault", 4.0);
        categoryPref.put("Heavy Battle", 3.5);
        categoryPref.put("Direct Fire", 3.3);
        categoryPref.put("Fire", 3.2);
        categoryPref.put("Fire Support", 3.2);
        categoryPref.put("Anti-Air", 3.0);
        categoryPref.put("Battle", 3.0);
        categoryPref.put("Heavy Striker/Cavalry", 3.0);
        categoryPref.put("Heavy Recon", 3.0);
        categoryPref.put("Medium Battle", 2.5);
        categoryPref.put("Urban", 2.3);
        categoryPref.put("Striker/Cavalry", 2.0);
        categoryPref.put("Light Battle", 2.0);
        categoryPref.put("Ranger", 2.0);
        categoryPref.put("Sweep", 1.8);
        categoryPref.put("Probe", 1.8);
        categoryPref.put("Pursuit", 1.6);
        categoryPref.put("Light Fire", 1.6);
        categoryPref.put("Recon", 1.5);
        categoryPref.put("Light Striker/Cavalry", 1.4);
        categoryPref.put("Light Recon", 1.2);
        return categoryPref;
    }

    private WeightBudgetAllocator() {}

    /** Walk the built force tree and reshape every cluster that declares weight targets. */
    static void allocate(ForceDescriptor root) {
        if (root == null) {
            return;
        }
        Integer echelon = root.getEchelon();
        Map<Integer, WeightTarget> targets = root.getWeightTargets();
        if ((echelon != null) && (echelon == CLUSTER_ECHELON) && (targets != null) && !targets.isEmpty()) {
            allocateCluster(root, targets);
        }
        for (ForceDescriptor sub : root.getSubForces()) {
            allocate(sub);
        }
        // Also reshape clusters reached through attached forces (e.g. aerospace/naval clusters
        // attached to a galaxy or touman), mirroring how collectClusters walks attached forces.
        for (ForceDescriptor attachedForce : root.getAttached()) {
            allocate(attachedForce);
        }
    }

    /** A generated element slot together with the star it belongs to. */
    private record Leaf(ForceDescriptor element, ForceDescriptor star) {}

    private static void allocateCluster(ForceDescriptor cluster, Map<Integer, WeightTarget> targets) {
        List<Leaf> leaves = new ArrayList<>();
        collectLeaves(cluster, null, leaves);

        Map<Integer, List<Leaf>> byType = new HashMap<>();
        for (Leaf leaf : leaves) {
            Integer unitType = leaf.element().getUnitType();
            if (unitType != null) {
                byType.computeIfAbsent(unitType, key -> new ArrayList<>()).add(leaf);
            }
        }

        int defaultType = ModelRecord.parseUnitType(WeightTarget.DEFAULT_UNIT_TYPE);
        int reshaped = 0;
        for (Map.Entry<Integer, List<Leaf>> entry : byType.entrySet()) {
            WeightTarget spec = targets.get(entry.getKey());
            if (spec == null) {
                spec = targets.get(defaultType);
            }
            if (spec != null) {
                assignBudget(entry.getValue(), spec);
                reshaped += entry.getValue().size();
            }
        }
        logger.debug("[ForceGen][Budget] cluster faction={} weightClass={} reshaped {} of {} element slots",
              cluster.getFaction(), cluster.getWeightClassCode(), reshaped, leaves.size());
    }

    // DFS tracking the nearest STAR ancestor so element slots can be grouped per star.
    private static void collectLeaves(ForceDescriptor forceDescriptor, ForceDescriptor currentStar, List<Leaf> leaves) {
        ForceDescriptor star = currentStar;
        Integer echelon = forceDescriptor.getEchelon();
        if ((echelon != null) && (echelon == STAR_ECHELON)) {
            star = forceDescriptor;
        }
        // A leaf unit slot: no children, carries a weight class, and uses the L/M/H/A system (so
        // ProtoMeks and infantry are skipped, matching the prototype). Tested structurally rather than
        // via the element flag, which may not be set until the later fill phase.
        if (forceDescriptor.getSubForces().isEmpty() && (forceDescriptor.getWeightClass() != null)
              && forceDescriptor.useWeightClass()) {
            leaves.add(new Leaf(forceDescriptor, star));
        }
        for (ForceDescriptor sub : forceDescriptor.getSubForces()) {
            collectLeaves(sub, star, leaves);
        }
    }

    private static void assignBudget(List<Leaf> group, WeightTarget target) {
        int slotCount = group.size();
        if (slotCount == 0) {
            return;
        }
        Map<Integer, Integer> budget = integerBudget(target.pct(), target.spread(), slotCount);

        List<Integer> tokens = new ArrayList<>(slotCount);
        for (int weightClass : WEIGHT_CLASSES_HEAVY_FIRST) {
            int count = budget.getOrDefault(weightClass, 0);
            for (int i = 0; i < count; i++) {
                tokens.add(weightClass);
            }
        }

        Map<ForceDescriptor, List<Leaf>> stars = new LinkedHashMap<>();
        for (Leaf leaf : group) {
            stars.computeIfAbsent(leaf.star(), key -> new ArrayList<>()).add(leaf);
        }

        List<ForceDescriptor> ordered = new ArrayList<>(stars.keySet());
        ordered.sort((first, second) -> Double.compare(starPref(second), starPref(first)));

        int tokenIndex = 0;
        for (ForceDescriptor star : ordered) {
            for (Leaf leaf : stars.get(star)) {
                if (tokenIndex < tokens.size()) {
                    leaf.element().setWeightClass(tokens.get(tokenIndex));
                }
                tokenIndex++;
            }
        }
    }

    private static double starPref(ForceDescriptor star) {
        if (star == null) {
            return DEFAULT_CATEGORY_PREF;
        }
        FormationType formationType = star.getFormation();
        if (formationType == null) {
            return DEFAULT_CATEGORY_PREF;
        }
        return CATEGORY_WEIGHT_PREF.getOrDefault(formationType.getName(), DEFAULT_CATEGORY_PREF);
    }

    /**
     * Largest-remainder integer allocation of {@code slotCount} slots across L/M/H/A from a target percentage map, with
     * optional +/- spread jitter. Mirrors ratgen_sim.py {@code _integer_budget}. The result always sums to exactly
     * {@code slotCount}.
     *
     * @param targetPercentages target percentage by weight-class int (missing classes treated as 0)
     * @param spread            +/- percentage-point jitter applied per class before normalizing (0 = none)
     * @param slotCount         total number of slots to distribute
     *
     * @return integer slot count by weight-class int, summing to {@code slotCount}
     */
    static Map<Integer, Integer> integerBudget(Map<Integer, Double> targetPercentages, double spread, int slotCount) {
        int[] weightClasses = { EntityWeightClass.WEIGHT_LIGHT, EntityWeightClass.WEIGHT_MEDIUM,
                                EntityWeightClass.WEIGHT_HEAVY, EntityWeightClass.WEIGHT_ASSAULT };
        Map<Integer, Double> jittered = new HashMap<>();
        double total = 0.0;
        for (int weightClass : weightClasses) {
            double base = targetPercentages.getOrDefault(weightClass, 0.0);
            double jitter = (spread > 0) ? ((Compute.randomInt(2001) / 1000.0 - 1.0) * spread) : 0.0;
            double value = Math.max(0.0, base + jitter);
            jittered.put(weightClass, value);
            total += value;
        }
        if (total <= 0.0) {
            total = 1.0;
        }
        Map<Integer, Double> raw = new HashMap<>();
        Map<Integer, Integer> budget = new HashMap<>();
        int assigned = 0;
        for (int weightClass : weightClasses) {
            double scaled = jittered.get(weightClass) / total * slotCount;
            raw.put(weightClass, scaled);
            int floor = (int) Math.floor(scaled);
            budget.put(weightClass, floor);
            assigned += floor;
        }
        int leftover = slotCount - assigned;
        List<Integer> order = new ArrayList<>();
        for (int weightClass : weightClasses) {
            order.add(weightClass);
        }
        order.sort((first, second) -> Double.compare(raw.get(second) - Math.floor(raw.get(second)),
              raw.get(first) - Math.floor(raw.get(first))));
        for (int index = 0; index < leftover; index++) {
            int weightClass = order.get(index % order.size());
            budget.put(weightClass, budget.get(weightClass) + 1);
        }
        return budget;
    }
}
