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
package megamek.client.formation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import megamek.client.formation.AssemblyUnit.Family;
import megamek.client.ratgenerator.FormationType;
import megamek.client.ratgenerator.ModelRecord;
import megamek.common.annotations.Nullable;
import megamek.common.loaders.MekSummary;
import megamek.common.units.Entity;
import megamek.common.units.EntityWeightClass;
import megamek.common.units.UnitRole;
import megamek.common.units.UnitType;

/**
 * Partitions a player's loose lobby units into Campaign Operations formations - lances, stars or Level
 * IIs by {@link Organization} - deterministically: the same roster always yields the same formations.
 *
 * <p>The search space is constrained before it is scored. Units that must stay together are fused into
 * atoms first (a C3 network is worthless split across formations; battle armor and its ride must share
 * one - see {@link #buildAtoms}), then atoms split by combined-arms family ({@link #familyOf}: CamOps
 * builds type-pure elements, aerospace never mixes with ground). Within each family every partition of
 * the atoms into equal-as-possible elements is enumerated exhaustively at lobby scale (10 units into two
 * stars is 126 partitions; 12 into three lances is 5,775) and the best-scoring partition wins; above
 * {@link #EXHAUSTIVE_UNIT_LIMIT} units a greedy fill with a swap-improvement pass uses the same scoring
 * function.</p>
 *
 * <p>The score is the design surface - three element-level weights and two partition-level ones, in
 * place of buried if-else: a formation that qualifies as a CamOps type is worth {@link #TYPE_MATCH_SCORE}
 * (dominant, so legality beats everything), sharing a battlefield role is worth {@link #ROLE_PURITY_WEIGHT}
 * per unanimous share, spreading walking speeds costs {@link #SPEED_SPREAD_WEIGHT} per MP of spread; a
 * partition is further charged for uneven battle value across its elements and credited for spreading
 * ECM carriers between them.</p>
 */
public final class FormationAssembler {

    /** A qualified CamOps formation type outbids every softer preference. */
    static final double TYPE_MATCH_SCORE = 1000;

    /**
     * Weight of role purity, applied to the SQUARED share of units holding the element's most common
     * battlefield role. The square rewards concentration: a 75%-pure fire core plus a mixed lance beats
     * two half-pure lances, which is how a human builds the same roster - linear share ties on that
     * comparison and lets the speed term scatter the fire support.
     */
    static final double ROLE_PURITY_WEIGHT = 200;

    /** Cost per MP of walking-speed spread within an element - a lance moves at its slowest member. */
    static final double SPEED_SPREAD_WEIGHT = 10;

    /** Cost per point of battle-value standard deviation across a partition's elements. */
    static final double BV_IMBALANCE_WEIGHT = 0.05;

    /** Credit per element that carries ECM - spreading coverage beats stacking it. */
    static final double ECM_SPREAD_BONUS = 25;

    /** No element below this size; a remainder this small folds in rather than standing alone. */
    static final int MINIMUM_ELEMENT = 3;

    /** Above this many units in one family pool, greedy-with-swaps replaces exhaustive enumeration. */
    static final int EXHAUSTIVE_UNIT_LIMIT = 15;

    private static final List<String> NAME_SUFFIXES = List.of("Alpha", "Bravo", "Charlie", "Delta",
          "Echo", "Foxtrot", "Golf", "Hotel", "India", "Juliett", "Kilo", "Lima", "Mike", "November",
          "Oscar", "Papa", "Quebec", "Romeo", "Sierra", "Tango");

    /** Memoized element evaluations, keyed by the sorted entity ids of the element's units. */
    private final Map<List<Integer>, ElementEval> elementEvalCache = new HashMap<>();

    private record ElementEval(double score, @Nullable FormationType type, long battleValue, boolean hasEcm) {
    }

    private FormationAssembler() {
    }

    /**
     * Partitions the given loose units into named formations.
     *
     * @param looseUnits   the units to assemble; an empty list yields an empty result
     * @param organization the doctrine to assemble under; {@link Organization#AUTO} resolves from the
     *                     units' majority tech base
     * @param namesInUse   force names already present in the lobby, so generated names never collide
     *
     * @return the proposed formations, every input unit in exactly one of them
     */
    public static List<AssembledFormation> assemble(List<AssemblyUnit> looseUnits,
          Organization organization, Set<String> namesInUse) {
        return assemble(looseUnits, organization, namesInUse, null);
    }

    /**
     * Partitions the given loose units into named formations, using the force's faction to resolve
     * {@link Organization#AUTO}.
     *
     * @param looseUnits   the units to assemble; an empty list yields an empty result
     * @param organization the doctrine to assemble under
     * @param namesInUse   force names already present in the lobby, so generated names never collide
     * @param factionKey   the force's generator faction key, or {@code null} when the player never chose one
     *
     * @return the proposed formations, every input unit in exactly one of them
     */
    public static List<AssembledFormation> assemble(List<AssemblyUnit> looseUnits,
          Organization organization, Set<String> namesInUse, @Nullable String factionKey) {
        if (looseUnits.isEmpty()) {
            return List.of();
        }
        return new FormationAssembler().run(looseUnits, organization, namesInUse, factionKey);
    }

    private List<AssembledFormation> run(List<AssemblyUnit> looseUnits, Organization organization,
          Set<String> namesInUse, @Nullable String factionKey) {
        List<AssemblyUnit> units = new ArrayList<>(looseUnits);
        units.sort(Comparator.comparingInt(AssemblyUnit::entityId));
        Organization resolved = organization.resolve(units, factionKey);

        List<List<AssemblyUnit>> atoms = buildAtoms(units);
        Map<Family, List<List<AssemblyUnit>>> pools = poolByFamily(atoms);

        List<List<AssemblyUnit>> elements = new ArrayList<>();
        for (Family family : List.of(Family.MEK, Family.VEHICLE, Family.INFANTRY)) {
            List<List<AssemblyUnit>> pool = pools.get(family);
            if (pool != null) {
                elements.addAll(partitionPool(pool, resolved.getElementSize()));
            }
        }
        // Aerospace never mixes down: one air element per side in v1, split only when Phase B demands.
        List<List<AssemblyUnit>> airPool = pools.get(Family.AERO);
        if (airPool != null) {
            elements.add(flatten(airPool));
        }

        return nameElements(elements, resolved, namesInUse);
    }

    // ======================== Atoms: what may never be split ========================

    /**
     * Fuses units that must share a formation into indivisible atoms with union-find: members of one C3
     * network, and a carried or towed unit with its carrier. A constraint on the search space, not a
     * score - no partition that strands battle armor from its ride is ever considered.
     */
    private static List<List<AssemblyUnit>> buildAtoms(List<AssemblyUnit> units) {
        Map<Integer, Integer> indexById = new HashMap<>();
        for (int i = 0; i < units.size(); i++) {
            indexById.put(units.get(i).entityId(), i);
        }
        int[] parent = new int[units.size()];
        for (int i = 0; i < parent.length; i++) {
            parent[i] = i;
        }

        Map<String, Integer> firstOfNetwork = new HashMap<>();
        for (int i = 0; i < units.size(); i++) {
            AssemblyUnit unit = units.get(i);
            if (unit.c3NetworkId() != null) {
                Integer first = firstOfNetwork.putIfAbsent(unit.c3NetworkId(), i);
                if (first != null) {
                    union(parent, first, i);
                }
            }
            // Only fuse when the carrier is part of the same loose pool; a unit riding something
            // already hand-assigned assembles on its own.
            for (int carrierId : new int[] { unit.transportId(), unit.towedById() }) {
                if (carrierId != Entity.NONE) {
                    Integer carrierIndex = indexById.get(carrierId);
                    if (carrierIndex != null) {
                        union(parent, carrierIndex, i);
                    }
                }
            }
        }

        Map<Integer, List<AssemblyUnit>> atomsByRoot = new HashMap<>();
        for (int i = 0; i < units.size(); i++) {
            atomsByRoot.computeIfAbsent(find(parent, i), root -> new ArrayList<>()).add(units.get(i));
        }
        List<List<AssemblyUnit>> atoms = new ArrayList<>(atomsByRoot.values());
        atoms.sort(Comparator.comparingInt((List<AssemblyUnit> atom) -> -atom.size())
              .thenComparingInt(atom -> atom.getFirst().entityId()));
        return atoms;
    }

    private static int find(int[] parent, int node) {
        while (parent[node] != node) {
            parent[node] = parent[parent[node]];
            node = parent[node];
        }
        return node;
    }

    private static void union(int[] parent, int first, int second) {
        parent[find(parent, first)] = find(parent, second);
    }

    // ======================== Families: combined arms ========================

    /**
     * Groups atoms by combined-arms family, then folds any ground family too small to stand alone into
     * the largest other ground family (two lonely tanks join the Mek pool rather than orphaning).
     * Aerospace never folds.
     */
    private static Map<Family, List<List<AssemblyUnit>>> poolByFamily(List<List<AssemblyUnit>> atoms) {
        Map<Family, List<List<AssemblyUnit>>> pools = new EnumMap<>(Family.class);
        for (List<AssemblyUnit> atom : atoms) {
            pools.computeIfAbsent(familyOf(atom), family -> new ArrayList<>()).add(atom);
        }

        List<Family> groundFamilies = List.of(Family.MEK, Family.VEHICLE, Family.INFANTRY);
        Family largestGround = null;
        int largestCount = 0;
        for (Family family : groundFamilies) {
            int count = unitCount(pools.get(family));
            if (count > largestCount) {
                largestCount = count;
                largestGround = family;
            }
        }
        for (Family family : groundFamilies) {
            List<List<AssemblyUnit>> pool = pools.get(family);
            if ((pool != null) && (family != largestGround) && (unitCount(pool) < MINIMUM_ELEMENT)) {
                pools.get(largestGround).addAll(pool);
                pools.remove(family);
            }
        }
        return pools;
    }

    /**
     * An atom's family follows its most formation-defining member: mechanized battle armor lands in its
     * carrier's family by construction - Elementals stay with their Omni star, Clan doctrine for free.
     */
    private static Family familyOf(List<AssemblyUnit> atom) {
        Family best = null;
        for (AssemblyUnit unit : atom) {
            Family family = unit.family();
            if ((best == null) || (family.ordinal() < best.ordinal())) {
                best = family;
            }
        }
        return best;
    }

    private static int unitCount(@Nullable List<List<AssemblyUnit>> pool) {
        if (pool == null) {
            return 0;
        }
        int count = 0;
        for (List<AssemblyUnit> atom : pool) {
            count += atom.size();
        }
        return count;
    }

    private static List<AssemblyUnit> flatten(List<List<AssemblyUnit>> atoms) {
        List<AssemblyUnit> units = new ArrayList<>();
        for (List<AssemblyUnit> atom : atoms) {
            units.addAll(atom);
        }
        units.sort(Comparator.comparingInt(AssemblyUnit::entityId));
        return units;
    }

    // ======================== Partitioning one family pool ========================

    /**
     * Splits one family pool into elements: the size ladder first (1-2 units stay one group; up to the
     * element size stays one understrength element; larger pools split into round(n / size) elements as
     * equal as possible, never below {@link #MINIMUM_ELEMENT}), then the best assignment of atoms to
     * those element sizes by exhaustive enumeration or, above {@link #EXHAUSTIVE_UNIT_LIMIT} units,
     * greedy fill with swap improvement.
     */
    private List<List<AssemblyUnit>> partitionPool(List<List<AssemblyUnit>> pool, int elementSize) {
        List<List<AssemblyUnit>> elements = new ArrayList<>();
        List<List<AssemblyUnit>> divisible = new ArrayList<>();
        for (List<AssemblyUnit> atom : pool) {
            // An atom at or beyond a full element (a five-Mek C3 net under lance doctrine) is its own
            // element; forcing other units in with it would only starve the remaining elements.
            if (atom.size() >= elementSize) {
                elements.add(flatten(List.of(atom)));
            } else {
                divisible.add(atom);
            }
        }
        int unitTotal = unitCount(divisible);
        if (unitTotal == 0) {
            return elements;
        }

        // Round UP, never down: an element may be understrength (Campaign Operations allows it) but
        // never oversize, so twelve units under star doctrine are three stars of four, not two of six.
        int elementCount = Math.toIntExact((long) Math.ceil(unitTotal / (double) elementSize));
        elementCount = Math.max(1, elementCount);
        while ((elementCount > 1) && (unitTotal < MINIMUM_ELEMENT * elementCount)) {
            elementCount--;
        }

        while (true) {
            int[] targets = targetSizes(unitTotal, elementCount);
            List<List<AssemblyUnit>> best = (unitTotal > EXHAUSTIVE_UNIT_LIMIT)
                  ? flattenParts(greedyPartition(divisible, targets))
                  : new PoolSolver(divisible).solve(targets);
            if (best != null) {
                elements.addAll(best);
                return elements;
            }
            // Lumpy atoms can make the exact sizes unreachable (three C3 pairs into 3+3); one part
            // fewer always converges - a single part accepts any atom mix.
            elementCount--;
        }
    }

    private static List<List<AssemblyUnit>> flattenParts(List<List<List<AssemblyUnit>>> parts) {
        List<List<AssemblyUnit>> elements = new ArrayList<>();
        for (List<List<AssemblyUnit>> part : parts) {
            if (!part.isEmpty()) {
                elements.add(flatten(part));
            }
        }
        return elements;
    }

    private static int[] targetSizes(int unitTotal, int elementCount) {
        int base = unitTotal / elementCount;
        int extra = unitTotal % elementCount;
        int[] targets = new int[elementCount];
        for (int i = 0; i < elementCount; i++) {
            targets[i] = base + ((i < extra) ? 1 : 0);
        }
        return targets;
    }

    /**
     * The exhaustive path: enumerates every assignment of atoms to parts that fills the target sizes
     * exactly, scores each complete partition, and keeps the best. Elements are identified by bitmasks
     * over the atom indices, and every distinct element is evaluated once - at lobby scale (a
     * 15-unit pool in lances of four is roughly 2.6 million partitions over at most a few hundred
     * distinct elements) the leaves are cache lookups, not allocations. Symmetry between same-sized
     * empty parts is broken by only ever opening the first of them, so each set partition is visited
     * once.
     */
    private static final class PoolSolver {
        private final List<List<AssemblyUnit>> atoms;
        private final int[] atomSizes;
        private final Map<Integer, ElementEval> evalByMask = new HashMap<>();
        private int[] bestMasks;
        private double bestScore = Double.NEGATIVE_INFINITY;

        private PoolSolver(List<List<AssemblyUnit>> atoms) {
            this.atoms = atoms;
            atomSizes = new int[atoms.size()];
            for (int i = 0; i < atoms.size(); i++) {
                atomSizes[i] = atoms.get(i).size();
            }
        }

        /** @return the best partition as unit lists, or {@code null} when the atom sizes cannot fill the targets */
        private @Nullable List<List<AssemblyUnit>> solve(int[] targets) {
            place(0, new int[targets.length], targets.clone(), targets);
            if (bestMasks == null) {
                return null;
            }
            List<List<AssemblyUnit>> elements = new ArrayList<>();
            for (int mask : bestMasks) {
                if (mask != 0) {
                    elements.add(unitsOf(mask));
                }
            }
            return elements;
        }

        private void place(int atomIndex, int[] partMasks, int[] remaining, int[] targets) {
            if (atomIndex == atoms.size()) {
                scoreComplete(partMasks);
                return;
            }
            int size = atomSizes[atomIndex];
            int bit = 1 << atomIndex;
            for (int partIndex = 0; partIndex < partMasks.length; partIndex++) {
                if (remaining[partIndex] < size) {
                    continue;
                }
                if ((partMasks[partIndex] == 0) && isLaterDuplicateEmpty(partMasks, targets, partIndex)) {
                    continue;
                }
                partMasks[partIndex] |= bit;
                remaining[partIndex] -= size;
                place(atomIndex + 1, partMasks, remaining, targets);
                remaining[partIndex] += size;
                partMasks[partIndex] &= ~bit;
            }
        }

        /** Same-target empty parts are interchangeable; an atom only ever opens the first of each size. */
        private boolean isLaterDuplicateEmpty(int[] partMasks, int[] targets, int partIndex) {
            for (int earlier = 0; earlier < partIndex; earlier++) {
                if ((partMasks[earlier] == 0) && (targets[earlier] == targets[partIndex])) {
                    return true;
                }
            }
            return false;
        }

        private void scoreComplete(int[] partMasks) {
            List<ElementEval> evals = new ArrayList<>(partMasks.length);
            for (int mask : partMasks) {
                if (mask != 0) {
                    evals.add(evalOf(mask));
                }
            }
            double score = aggregateScore(evals);
            if (score > bestScore) {
                bestScore = score;
                bestMasks = partMasks.clone();
            }
        }

        private ElementEval evalOf(int mask) {
            return evalByMask.computeIfAbsent(mask, m -> evaluateElement(unitsOf(m)));
        }

        private List<AssemblyUnit> unitsOf(int mask) {
            List<AssemblyUnit> units = new ArrayList<>();
            for (int atomIndex = 0; atomIndex < atoms.size(); atomIndex++) {
                if ((mask & (1 << atomIndex)) != 0) {
                    units.addAll(atoms.get(atomIndex));
                }
            }
            units.sort(Comparator.comparingInt(AssemblyUnit::entityId));
            return units;
        }
    }

    /**
     * The large-pool path: place each atom (largest first) where it scores best, then keep applying the
     * best score-improving swap of two atoms between parts until no swap improves. Same scoring function
     * as the exhaustive path, so the two paths disagree only on how hard they search.
     */
    private List<List<List<AssemblyUnit>>> greedyPartition(List<List<AssemblyUnit>> atoms, int[] targets) {
        List<List<List<AssemblyUnit>>> parts = new ArrayList<>();
        for (int i = 0; i < targets.length; i++) {
            parts.add(new ArrayList<>());
        }
        int[] remaining = targets.clone();

        for (List<AssemblyUnit> atom : atoms) {
            int bestPart = -1;
            double bestGain = Double.NEGATIVE_INFINITY;
            for (int partIndex = 0; partIndex < parts.size(); partIndex++) {
                if (remaining[partIndex] < atom.size()) {
                    continue;
                }
                List<List<AssemblyUnit>> part = parts.get(partIndex);
                double gain = elementEval(withAtom(part, atom)).score()
                      - (part.isEmpty() ? 0 : elementEval(flatten(part)).score());
                if (gain > bestGain) {
                    bestGain = gain;
                    bestPart = partIndex;
                }
            }
            if (bestPart < 0) {
                // Lumpy atoms can overflow every target; the roomiest part absorbs the overflow.
                for (int partIndex = 0; partIndex < parts.size(); partIndex++) {
                    if ((bestPart < 0) || (remaining[partIndex] > remaining[bestPart])) {
                        bestPart = partIndex;
                    }
                }
            }
            parts.get(bestPart).add(atom);
            remaining[bestPart] -= atom.size();
        }

        improveBySwaps(parts);
        return parts;
    }

    private void improveBySwaps(List<List<List<AssemblyUnit>>> parts) {
        boolean improved = true;
        int guard = 0;
        while (improved && (guard++ < 200)) {
            improved = false;
            double currentScore = partitionScore(parts);
            for (int firstPart = 0; firstPart < parts.size() && !improved; firstPart++) {
                for (int secondPart = firstPart + 1; secondPart < parts.size() && !improved; secondPart++) {
                    List<List<AssemblyUnit>> first = parts.get(firstPart);
                    List<List<AssemblyUnit>> second = parts.get(secondPart);
                    for (int firstAtom = 0; firstAtom < first.size() && !improved; firstAtom++) {
                        for (int secondAtom = 0; secondAtom < second.size() && !improved; secondAtom++) {
                            // Only same-sized atoms may trade places, or the elements change size.
                            if (first.get(firstAtom).size() != second.get(secondAtom).size()) {
                                continue;
                            }
                            List<AssemblyUnit> fromFirst = first.set(firstAtom, second.get(secondAtom));
                            second.set(secondAtom, fromFirst);
                            if (partitionScore(parts) > currentScore) {
                                improved = true;
                            } else {
                                // No better: put both atoms back where they came from.
                                List<AssemblyUnit> restored = first.set(firstAtom, second.get(secondAtom));
                                second.set(secondAtom, restored);
                            }
                        }
                    }
                }
            }
        }
    }

    private static List<AssemblyUnit> withAtom(List<List<AssemblyUnit>> part, List<AssemblyUnit> atom) {
        List<List<AssemblyUnit>> extended = new ArrayList<>(part);
        extended.add(atom);
        return flatten(extended);
    }

    // ======================== Scoring ========================

    private double partitionScore(List<List<List<AssemblyUnit>>> parts) {
        List<ElementEval> evals = new ArrayList<>(parts.size());
        for (List<List<AssemblyUnit>> part : parts) {
            if (!part.isEmpty()) {
                evals.add(elementEval(flatten(part)));
            }
        }
        return aggregateScore(evals);
    }

    /**
     * The whole-partition score: the sum of its elements' own scores, charged for uneven battle value
     * between them and credited for spreading ECM carriers. The single definition of the aggregate,
     * shared by the greedy path, the exhaustive path and {@link #explain}, so no two searches can
     * drift apart on what "better" means.
     */
    private static double aggregateScore(List<ElementEval> evals) {
        double score = 0;
        double battleValueSum = 0;
        double battleValueSquaredSum = 0;
        int elementsWithEcm = 0;
        for (ElementEval eval : evals) {
            score += eval.score();
            battleValueSum += eval.battleValue();
            battleValueSquaredSum += (double) eval.battleValue() * eval.battleValue();
            if (eval.hasEcm()) {
                elementsWithEcm++;
            }
        }
        double standardDeviation = 0;
        if (evals.size() > 1) {
            double mean = battleValueSum / evals.size();
            standardDeviation = Math.sqrt(
                  Math.max(0, (battleValueSquaredSum / evals.size()) - (mean * mean)));
        }
        return score - (BV_IMBALANCE_WEIGHT * standardDeviation) + (ECM_SPREAD_BONUS * elementsWithEcm);
    }

    private ElementEval elementEval(List<AssemblyUnit> element) {
        List<Integer> key = element.stream().map(AssemblyUnit::entityId).sorted().toList();
        return elementEvalCache.computeIfAbsent(key, ignored -> evaluateElement(element));
    }

    private static ElementEval evaluateElement(List<AssemblyUnit> element) {
        FormationType type = bestType(element);
        double score = (type != null) ? TYPE_MATCH_SCORE : 0;
        double purity = rolePurity(element);
        score += ROLE_PURITY_WEIGHT * purity * purity;

        int minWalk = Integer.MAX_VALUE;
        int maxWalk = Integer.MIN_VALUE;
        long battleValue = 0;
        boolean hasEcm = false;
        for (AssemblyUnit unit : element) {
            minWalk = Math.min(minWalk, unit.walkMp());
            maxWalk = Math.max(maxWalk, unit.walkMp());
            battleValue += unit.battleValue();
            hasEcm |= unit.carriesEcm();
        }
        score -= SPEED_SPREAD_WEIGHT * (maxWalk - minWalk);
        return new ElementEval(score, type, battleValue, hasEcm);
    }

    /** @return the share of units holding the element's most common determined role, 0 when none have one */
    private static double rolePurity(List<AssemblyUnit> element) {
        Map<UnitRole, Integer> counts = new EnumMap<>(UnitRole.class);
        int modal = 0;
        for (AssemblyUnit unit : element) {
            if (unit.role() != UnitRole.UNDETERMINED) {
                modal = Math.max(modal, counts.merge(unit.role(), 1, Integer::sum));
            }
        }
        return modal / (double) element.size();
    }

    /**
     * The best CamOps type this element qualifies as, for its name and (in Phase B) its behavior preset.
     *
     * <p>Among the types that qualify, the pick goes to the one that says the most about the units.
     * First preference is a type whose ideal role every unit shares, because the rulebook treats that
     * as what a formation IS. Otherwise the most demanding type wins: some entries in the catalog ask
     * almost nothing - a Ranger Lance imposes no criteria beyond ground units of heavy weight or less -
     * and a type that everything qualifies for tells a player nothing. Alphabetical order settles the
     * rest so the pick stays deterministic.</p>
     *
     * <p>Faction-exclusive types are never offered: the Hammer and Anvil Lances are Free Worlds League
     * doctrine, the Rifle Lance House Davion, the Order Lance House Kurita, and nothing here knows
     * whose force this is. {@link FormationType#qualifies(List)} does not test that, so it is tested
     * here. When the force generation context lands, these become available to the factions entitled
     * to them.</p>
     *
     * @return the chosen type, or {@code null} when the catalog cannot judge the element - any unit without a
     *       catalog entry, or nothing qualifies
     */
    private static @Nullable FormationType bestType(List<AssemblyUnit> element) {
        List<MekSummary> summaries = new ArrayList<>();
        for (AssemblyUnit unit : element) {
            if (unit.summary() == null) {
                return null;
            }
            summaries.add(unit.summary());
        }
        boolean air = element.getFirst().family() == Family.AERO;

        FormationType best = null;
        boolean bestSharesIdealRole = false;
        int bestDemands = -1;
        for (FormationType candidate : formationTypesByName()) {
            if ((candidate.isGround() == air) || (candidate.getExclusiveFaction() != null)
                  || !candidate.qualifies(summaries)) {
                continue;
            }
            boolean sharesIdealRole = (candidate.getIdealRole() != UnitRole.UNDETERMINED)
                  && element.stream().allMatch(unit -> unit.role() == candidate.getIdealRole());
            int demands = demandCount(candidate);
            if ((best == null)
                  || (sharesIdealRole && !bestSharesIdealRole)
                  || ((sharesIdealRole == bestSharesIdealRole) && (demands > bestDemands))) {
                best = candidate;
                bestSharesIdealRole = sharesIdealRole;
                bestDemands = demands;
            }
        }
        return best;
    }

    /**
     * The formation catalog in name order, built once. The exhaustive search evaluates a few hundred
     * distinct elements on a large pool and each one asks the catalog the same question, so copying
     * and sorting the list every time is work with no result. The catalog is filled once on first
     * access and never changes afterwards, so one sorted view serves every caller.
     *
     * @return the catalog, sorted by name, not to be modified
     */
    private static List<FormationType> formationTypesByName() {
        return SortedCatalog.BY_NAME;
    }

    /** Holds the sorted catalog, built on first use by the class loader rather than by a lock. */
    private static final class SortedCatalog {
        private static final List<FormationType> BY_NAME = sortByName();

        private static List<FormationType> sortByName() {
            List<FormationType> types = new ArrayList<>(FormationType.getAllFormations());
            types.sort(Comparator.comparing(FormationType::getName));
            return List.copyOf(types);
        }
    }

    /** @return how many separate things a formation type asks of its units, the measure of how much it says */
    private static int demandCount(FormationType type) {
        int demands = 0;
        if (type.getMainDescription() != null) {
            demands++;
        }
        Iterator<FormationType.Constraint> others = type.getOtherCriteria();
        while (others.hasNext()) {
            others.next();
            demands++;
        }
        if (type.getGroupingCriteria() != null) {
            demands++;
        }
        if ((type.getMinWeightClass() > EntityWeightClass.WEIGHT_LIGHT)
              || (type.getMaxWeightClass() < EntityWeightClass.WEIGHT_ASSAULT)) {
            demands++;
        }
        return demands;
    }

    private static UnitRole modalRole(List<AssemblyUnit> element) {
        Map<UnitRole, Integer> counts = new EnumMap<>(UnitRole.class);
        UnitRole modal = UnitRole.UNDETERMINED;
        int modalCount = 0;
        for (AssemblyUnit unit : element) {
            if (unit.role() != UnitRole.UNDETERMINED) {
                int count = counts.merge(unit.role(), 1, Integer::sum);
                if (count > modalCount) {
                    modalCount = count;
                    modal = unit.role();
                }
            }
        }
        return modal;
    }

    // ======================== Explanation ========================

    /**
     * Explains one finished formation: the doctrine name it earned, the ledger behind it, what could
     * not be split, and the swaps that came closest to being chosen instead. Recomputed from the
     * formation's current members rather than remembered from assembly time, so it stays honest after
     * a player edits the force by hand - the answer is always about the group as it stands now.
     *
     * @param formationName the formation's name, for the report
     * @param units         its current members
     * @param siblings      the owner's other formations, keyed by name, for the alternatives pass;
     *                      may be empty, in which case no alternatives are reported
     *
     * @return the rationale, ready to render
     */
    public static FormationRationale explain(String formationName, List<AssemblyUnit> units,
          Map<String, List<AssemblyUnit>> siblings) {
        return explain(formationName, units, siblings, null);
    }

    /**
     * Explains one finished formation, using the force's faction to name the doctrine it was built
     * under.
     *
     * @param formationName the formation's name, for the report
     * @param units         its current members
     * @param siblings      the owner's other formations, keyed by name, for the alternatives pass
     * @param factionKey    the force's generator faction key, or {@code null} when the player never chose one
     *
     * @return the rationale, ready to render
     */
    public static FormationRationale explain(String formationName, List<AssemblyUnit> units,
          Map<String, List<AssemblyUnit>> siblings, @Nullable String factionKey) {
        List<AssemblyUnit> members = new ArrayList<>(units);
        members.sort(Comparator.comparingInt(AssemblyUnit::entityId));

        List<AssemblyUnit> wholeForce = new ArrayList<>(members);
        siblings.values().forEach(wholeForce::addAll);
        Organization organization = Organization.AUTO.resolve(wholeForce, factionKey);

        ElementEval eval = evaluateElement(members);
        UnitRole modalRole = modalRole(members);
        int modalRoleCount = 0;
        int slowest = Integer.MAX_VALUE;
        int fastest = Integer.MIN_VALUE;
        int ecmCarriers = 0;
        List<String> unknownToCatalog = new ArrayList<>();
        for (AssemblyUnit unit : members) {
            if (unit.role() == modalRole) {
                modalRoleCount++;
            }
            slowest = Math.min(slowest, unit.walkMp());
            fastest = Math.max(fastest, unit.walkMp());
            if (unit.carriesEcm()) {
                ecmCarriers++;
            }
            if (unit.summary() == null) {
                unknownToCatalog.add(unit.displayName());
            }
        }

        List<String> bindings = new ArrayList<>();
        List<List<AssemblyUnit>> atoms = buildAtoms(members);
        for (List<AssemblyUnit> atom : atoms) {
            if (atom.size() > 1) {
                bindings.add(describeBinding(atom));
            }
        }

        UnitRole idealRole = (eval.type() != null) ? eval.type().getIdealRole() : UnitRole.UNDETERMINED;
        boolean idealRoleWaived = (idealRole != UnitRole.UNDETERMINED)
              && members.stream().allMatch(unit -> unit.role() == idealRole);

        return new FormationRationale(formationName, eval.type(), organization, members, modalRole,
              modalRoleCount, (slowest == Integer.MAX_VALUE) ? 0 : slowest,
              (fastest == Integer.MIN_VALUE) ? 0 : fastest, eval.battleValue(), ecmCarriers, bindings,
              closestAlternatives(members, siblings, atoms), unknownToCatalog, idealRole,
              idealRoleWaived, requirementsOf(eval.type(), members));
    }

    /**
     * Scores each of a formation type's requirements against the units in hand, so the report can put
     * the rulebook and the roster side by side. Mirrors the order
     * {@link FormationType#qualifies(List)} tests them in: unit type first (never waived), then
     * weight class and the main criterion, then the counted secondary criteria.
     *
     * @return one entry per requirement, or an empty list when there is no type to measure against
     */
    private static List<FormationRationale.Requirement> requirementsOf(@Nullable FormationType type,
          List<AssemblyUnit> members) {
        List<FormationRationale.Requirement> requirements = new ArrayList<>();
        if (type == null) {
            return requirements;
        }
        List<MekSummary> summaries = new ArrayList<>();
        for (AssemblyUnit unit : members) {
            if (unit.summary() == null) {
                return requirements;
            }
            summaries.add(unit.summary());
        }
        int size = members.size();

        requirements.add(new FormationRationale.Requirement(FormationRationale.Kind.UNIT_TYPE,
              allowedTypeNames(type), size,
              scoreEach(summaries, summary ->
                    type.isAllowedUnitType(ModelRecord.parseUnitType(summary.getUnitType()))),
              false));

        String weightRange = EntityWeightClass.getClassName(
              Math.max(type.getMinWeightClass(), EntityWeightClass.WEIGHT_LIGHT))
              + " to " + EntityWeightClass.getClassName(
              Math.min(type.getMaxWeightClass(), EntityWeightClass.WEIGHT_ASSAULT));
        requirements.add(new FormationRationale.Requirement(FormationRationale.Kind.WEIGHT_CLASS,
              weightRange, size,
              scoreEach(summaries, summary -> (summary.getWeightClass() >= type.getMinWeightClass())
                    && (summary.getWeightClass() <= type.getMaxWeightClass())), true));

        if (type.getMainDescription() != null) {
            requirements.add(new FormationRationale.Requirement(FormationRationale.Kind.EVERY_UNIT,
                  type.getMainDescription(), size,
                  scoreEach(summaries, summary -> type.getMainCriteria().test(summary)), true));
        }

        Iterator<FormationType.Constraint> others = type.getOtherCriteria();
        while (others.hasNext()) {
            FormationType.Constraint constraint = others.next();
            requirements.add(new FormationRationale.Requirement(
                  constraint.isPairedWithPrevious()
                        ? FormationRationale.Kind.AT_LEAST_ALTERNATIVE
                        : FormationRationale.Kind.AT_LEAST,
                  constraint.getDescription(), constraint.getMinimum(size),
                  scoreEach(summaries, constraint::matches), true));
        }

        if (type.getGroupingCriteria() != null) {
            FormationType.GroupingConstraint grouping = type.getGroupingCriteria();
            requirements.add(new FormationRationale.Requirement(FormationRationale.Kind.GROUPING,
                  grouping.getDescription(), groupedUnitsRequired(grouping, summaries),
                  scoreEach(summaries, grouping::matches), true));
        }
        return requirements;
    }

    /** @return the unit types this formation admits, in plain words: "Meks, Vehicles" */
    private static String allowedTypeNames(FormationType type) {
        List<String> names = new ArrayList<>();
        for (int unitType = 0; unitType <= UnitType.AEROSPACE_FIGHTER; unitType++) {
            if (type.isAllowedUnitType(unitType)) {
                names.add(UnitType.getTypeDisplayableName(unitType));
            }
        }
        return names.isEmpty() ? "-" : String.join(", ", names);
    }

    /**
     * How many units a grouping rule actually demands, resolved the same way
     * {@link FormationType#qualifies(List)} resolves it. Two cases would otherwise report zero and
     * make a demanding rule look like it asks nothing: a group size of zero means one group of
     * everything (the Order Lance, where every unit must be the same model), and the group count is
     * capped by how many whole groups the units it applies to can actually form.
     *
     * @param grouping  the rule to measure
     * @param summaries the units in hand
     *
     * @return how many units must fall into matched groups
     */
    private static int groupedUnitsRequired(FormationType.GroupingConstraint grouping,
          List<MekSummary> summaries) {
        int applicable = 0;
        for (MekSummary summary : summaries) {
            if (grouping.appliesTo(ModelRecord.parseUnitType(summary.getUnitType()))) {
                applicable++;
            }
        }
        if (applicable == 0) {
            return 0;
        }
        int groupSize = (grouping.getGroupSize() <= 0)
              ? applicable
              : Math.min(grouping.getGroupSize(), applicable);
        int groupCount = Math.min(grouping.getNumGroups(), applicable / groupSize);
        return Math.max(0, groupCount * groupSize);
    }

    private static List<Boolean> scoreEach(List<MekSummary> summaries, Predicate<MekSummary> test) {
        List<Boolean> results = new ArrayList<>(summaries.size());
        for (MekSummary summary : summaries) {
            results.add(test.test(summary));
        }
        return results;
    }

    /** Names what fused an atom, so the report can say why those units cannot be parted. */
    private static String describeBinding(List<AssemblyUnit> atom) {
        List<String> names = new ArrayList<>();
        for (AssemblyUnit unit : atom) {
            names.add(unit.displayName());
        }
        String joined = String.join(", ", names);
        for (AssemblyUnit unit : atom) {
            if (unit.c3NetworkId() != null) {
                return joined + " share a C3 network";
            }
        }
        return joined + " are carried or towed together";
    }

    /**
     * The trades the search passed over: for every member that is free to move, the cost of swapping
     * it for a unit in another formation, cheapest (closest call) first. A swap rather than a move
     * because element sizes are fixed - the real question is always "why this unit and not that one".
     * Units fused into an atom are skipped: they were never free to move in the first place.
     */
    private static List<FormationRationale.AlternativeSwap> closestAlternatives(
          List<AssemblyUnit> members, Map<String, List<AssemblyUnit>> siblings,
          List<List<AssemblyUnit>> atoms) {
        // Bound on BOTH sides: a unit fused to a team-mate cannot be traded away, and cannot be taken
        // from the formation it is fused inside either - such a trade was never in the search space.
        Set<Integer> boundIds = boundUnitIds(atoms);
        for (List<AssemblyUnit> sibling : siblings.values()) {
            boundIds.addAll(boundUnitIds(buildAtoms(sibling)));
        }

        List<ElementEval> baseline = new ArrayList<>();
        baseline.add(evaluateElement(members));
        for (List<AssemblyUnit> sibling : siblings.values()) {
            baseline.add(evaluateElement(sibling));
        }
        double baselineScore = aggregateScore(baseline);

        List<FormationRationale.AlternativeSwap> alternatives = new ArrayList<>();
        for (AssemblyUnit member : members) {
            if (boundIds.contains(member.entityId())) {
                continue;
            }
            for (Map.Entry<String, List<AssemblyUnit>> sibling : siblings.entrySet()) {
                for (AssemblyUnit candidate : sibling.getValue()) {
                    if ((candidate.family() != member.family())
                          || boundIds.contains(candidate.entityId())) {
                        continue;
                    }
                    List<ElementEval> swapped = new ArrayList<>();
                    swapped.add(evaluateElement(replace(members, member, candidate)));
                    for (Map.Entry<String, List<AssemblyUnit>> other : siblings.entrySet()) {
                        swapped.add(other.getKey().equals(sibling.getKey())
                              ? evaluateElement(replace(other.getValue(), candidate, member))
                              : evaluateElement(other.getValue()));
                    }
                    alternatives.add(new FormationRationale.AlternativeSwap(member.displayName(),
                          candidate.displayName(), sibling.getKey(),
                          baselineScore - aggregateScore(swapped)));
                }
            }
        }
        alternatives.sort(Comparator.comparingDouble(FormationRationale.AlternativeSwap::cost));
        return alternatives.subList(0, Math.min(3, alternatives.size()));
    }

    /** @return the ids of every unit fused to at least one other, which are never free to move alone */
    private static Set<Integer> boundUnitIds(List<List<AssemblyUnit>> atoms) {
        Set<Integer> bound = new HashSet<>();
        for (List<AssemblyUnit> atom : atoms) {
            if (atom.size() > 1) {
                for (AssemblyUnit unit : atom) {
                    bound.add(unit.entityId());
                }
            }
        }
        return bound;
    }

    private static List<AssemblyUnit> replace(List<AssemblyUnit> units, AssemblyUnit removed,
          AssemblyUnit added) {
        List<AssemblyUnit> result = new ArrayList<>();
        for (AssemblyUnit unit : units) {
            result.add((unit.entityId() == removed.entityId()) ? added : unit);
        }
        result.sort(Comparator.comparingInt(AssemblyUnit::entityId));
        return result;
    }

    // ======================== Naming ========================

    /**
     * Names the finished elements. Ground elements come first ordered by descending battle value, the
     * air element last; each gets its qualified type as a prefix ("Battle Lance Alpha") except under
     * ComStar and Word of Blake doctrine, which name by element alone ("Level II Alpha"). Suffixes skip
     * names the lobby already uses, so repeated assembly never collides.
     */
    private List<AssembledFormation> nameElements(List<List<AssemblyUnit>> elements,
          Organization organization, Set<String> namesInUse) {
        List<AssemblyUnit> airElement = null;
        List<List<AssemblyUnit>> groundElements = new ArrayList<>();
        for (List<AssemblyUnit> element : elements) {
            if (element.getFirst().family() == Family.AERO) {
                airElement = element;
            } else {
                groundElements.add(element);
            }
        }
        groundElements.sort(Comparator.comparingLong((List<AssemblyUnit> element) ->
              -elementEval(element).battleValue()).thenComparingInt(element -> element.getFirst().entityId()));

        Set<String> taken = new HashSet<>(namesInUse);
        List<AssembledFormation> formations = new ArrayList<>();
        for (List<AssemblyUnit> element : groundElements) {
            FormationType type = elementEval(element).type();
            String prefix = prefixFor(type, organization, false);
            String name = firstFreeName(prefix, taken);
            formations.add(new AssembledFormation(name, type, element));
        }
        if (airElement != null) {
            FormationType type = elementEval(airElement).type();
            String prefix = prefixFor(type, organization, true);
            String name = firstFreeName(prefix, taken);
            formations.add(new AssembledFormation(name, type, airElement));
        }
        return formations;
    }

    private static String prefixFor(@Nullable FormationType type, Organization organization, boolean air) {
        String elementWord = (air ? "Air " : "") + organization.getElementWord();
        // Air elements stay type-free: squadron catalog names ("Aerospace Superiority Squadron")
        // make unreadable prefixes, and v1 fields one air element per side anyway.
        if (air || (type == null) || !organization.usesTypePrefix()) {
            return elementWord;
        }
        // "Striker/Cavalry" reads as "Striker Star" - the first segment is the spoken name.
        String typeName = type.getName().split("/")[0].trim();
        return typeName + " " + elementWord;
    }

    private static String firstFreeName(String prefix, Set<String> taken) {
        for (String suffix : NAME_SUFFIXES) {
            String candidate = prefix + " " + suffix;
            if (taken.add(candidate)) {
                return candidate;
            }
        }
        // Twenty suffixes exhausted for one prefix: fall back to a numbered name, still unique.
        int number = NAME_SUFFIXES.size() + 1;
        while (!taken.add(prefix + " " + number)) {
            number++;
        }
        return prefix + " " + number;
    }
}
