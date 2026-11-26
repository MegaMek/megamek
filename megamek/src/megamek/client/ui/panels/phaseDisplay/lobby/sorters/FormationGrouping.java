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
package megamek.client.ui.panels.phaseDisplay.lobby.sorters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.annotation.Nullable;
import megamek.common.force.Force;
import megamek.common.force.Forces;
import megamek.common.units.Entity;

/**
 * Keeps the units of each formation together in the lobby's Sortable View, whatever the player sorts by.
 * <p>
 * Formations nest: a battalion holds companies, a company holds lances. Two units are first ordered by the formations
 * they are in, from the top down, and only units of the same lowest formation are ordered against each other. So a
 * lance is always one block of rows, inside its company's block, inside its battalion's block.
 * </p>
 * <p>
 * With the default sorter the table follows the force tree: top-level formations in {@link Forces#TOP_LEVEL_ORDER},
 * sub formations and units in the order set with the up/down moves. With any other sorter, formations are ordered by
 * the unit in them that the sorter would list first, and units inside a formation by the sorter. A carrier and its
 * load are one object on the board, so a whole carrier stack is placed by its outermost carrier's formation. Inside a
 * formation the C3 and carrier stack wrappers of {@link MekTableSorter} still apply.
 * </p>
 * <p>
 * A {@link FormationSorter} (the Formation column's sorts) orders the formations by its metric - total BV, total
 * tonnage or slowest unit - instead, keeping the units inside each formation in force tree order.
 * </p>
 * <p>
 * A C3 network whose members sit in different formations cannot be one block of rows. Its members are reported by
 * {@link #splitC3NetIds()} so the table can mark them without drawing branches that point at the wrong row.
 * </p>
 */
public final class FormationGrouping {

    private final Forces forces;
    private final Comparator<Entity> baseSorter;
    private final boolean followTreeOrder;
    private final Comparator<Entity> insideFormation;
    /** The Formation column sort in use, or {@code null} for any other sorter. */
    private final @Nullable FormationSorter formationSorter;

    /** Per force id, for the Formation column sorts: the total BV of the units in it, at any depth. */
    private final Map<Integer, Integer> totalBattleValues = new HashMap<>();
    /** Per force id, for the Formation column sorts: the total tonnage of the units in it, at any depth. */
    private final Map<Integer, Double> totalTonnages = new HashMap<>();
    /** Per force id, for the Formation column sorts: the lowest walking MP of the units in it, at any depth. */
    private final Map<Integer, Integer> slowestWalkMPs = new HashMap<>();

    /** Per unit id: the formations of its outermost carrier, top-level first; empty outside any formation. */
    private final Map<Integer, List<Force>> chains = new HashMap<>();
    /** Per unit id: its outermost carrier. */
    private final Map<Integer, Entity> roots = new HashMap<>();
    /** Per unit id: its place in force tree order. */
    private final Map<Integer, Integer> treeIndexes = new HashMap<>();
    /** Per force id: the unit in it (at any depth) that the base sorter lists first. */
    private final Map<Integer, Entity> representatives = new HashMap<>();
    private final Set<String> splitC3NetIds = new HashSet<>();

    /**
     * @param entities        the units about to be sorted
     * @param forces          the game's forces
     * @param baseSorter      the sorter the player chose
     * @param followTreeOrder {@code true} for the default sorter, which lists formations and units in force tree order;
     *                        a {@link FormationSorter} always keeps units in force tree order
     */
    public FormationGrouping(List<Entity> entities, Forces forces, Comparator<Entity> baseSorter,
          boolean followTreeOrder) {
        this.forces = forces;
        this.baseSorter = baseSorter;
        formationSorter = (baseSorter instanceof FormationSorter sorter) ? sorter : null;
        this.followTreeOrder = followTreeOrder || (formationSorter != null);

        List<Entity> treeOrder = forces.sortedInTreeOrder(entities);
        for (int index = 0; index < treeOrder.size(); index++) {
            treeIndexes.put(treeOrder.get(index).getId(), index);
        }

        Map<String, Set<Integer>> lowestFormationsByNetwork = new HashMap<>();
        for (Entity entity : entities) {
            Entity root = MekTableSorter.carrierPath(entity).get(0);
            List<Force> chain = forces.forceChain(root);
            roots.put(entity.getId(), root);
            chains.put(entity.getId(), chain);
            for (Force force : chain) {
                Entity representative = representatives.get(force.getId());
                if ((representative == null) || (baseSorter.compare(root, representative) < 0)) {
                    representatives.put(force.getId(), root);
                }
            }
            if (entity.hasAnyC3System() && (entity.getC3NetId() != null)) {
                int lowestFormation = chain.isEmpty() ? Force.NO_FORCE : chain.get(chain.size() - 1).getId();
                lowestFormationsByNetwork.computeIfAbsent(entity.getC3NetId(), netId -> new HashSet<>())
                      .add(lowestFormation);
            }
        }
        if ((formationSorter != null) && (formationSorter.getMetric() != FormationSorter.Metric.TREE_ORDER)) {
            addUpFormations(entities);
        }
        for (Map.Entry<String, Set<Integer>> network : lowestFormationsByNetwork.entrySet()) {
            if (network.getValue().size() > 1) {
                splitC3NetIds.add(network.getKey());
            }
        }

        Comparator<Entity> byTreeOrder = Comparator.comparingInt(this::treeIndex);
        Comparator<Entity> unitOrder = this.followTreeOrder ? byTreeOrder.thenComparing(baseSorter)
              : baseSorter.thenComparing(byTreeOrder);
        insideFormation = MekTableSorter.keepingC3NetworksTogether(entities,
              MekTableSorter.keepingCarriedUnitsTogether(unitOrder));
    }

    /**
     * Adds up each formation's total BV, total tonnage and slowest walking MP over its units at any depth. A unit counts
     * for its own formations, even when it rides on a carrier in another one.
     */
    private void addUpFormations(List<Entity> entities) {
        for (Entity entity : entities) {
            int battleValue = entity.calculateBattleValue();
            double tonnage = entity.getWeight();
            int walkMP = entity.getWalkMP();
            for (Force force : forces.forceChain(entity)) {
                totalBattleValues.merge(force.getId(), battleValue, Integer::sum);
                totalTonnages.merge(force.getId(), tonnage, Double::sum);
                slowestWalkMPs.merge(force.getId(), walkMP, Math::min);
            }
        }
    }

    /**
     * Compares two formations by the Formation column sort's metric and direction; 0 when no such sort is in use or the
     * values are equal.
     */
    private int compareByMetric(Force formationA, Force formationB) {
        if (formationSorter == null) {
            return 0;
        }
        int comparison = switch (formationSorter.getMetric()) {
            case TREE_ORDER -> 0;
            case TOTAL_BV -> Integer.compare(totalBattleValues.getOrDefault(formationA.getId(), 0),
                  totalBattleValues.getOrDefault(formationB.getId(), 0));
            case TOTAL_TONNAGE -> Double.compare(totalTonnages.getOrDefault(formationA.getId(), 0.0),
                  totalTonnages.getOrDefault(formationB.getId(), 0.0));
            case SLOWEST_UNIT -> Integer.compare(slowestWalkMPs.getOrDefault(formationA.getId(), 0),
                  slowestWalkMPs.getOrDefault(formationB.getId(), 0));
        };
        return comparison * formationSorter.direction();
    }

    /** Returns the comparator that sorts the table with formations kept together. */
    public Comparator<Entity> comparator() {
        return this::compare;
    }

    /** Returns the C3 networks whose members sit in more than one formation, by C3 net id. */
    public Set<String> splitC3NetIds() {
        return Collections.unmodifiableSet(splitC3NetIds);
    }

    /**
     * Describes the sorted table as runs of rows per formation, for the log: "[Battalion > A Company > Alpha Lance x4,
     * no formation x2]".
     *
     * @param sortedEntities the units in table order
     *
     * @return one entry per run of consecutive rows in the same lowest formation
     */
    public String blocksDescription(List<Entity> sortedEntities) {
        List<String> blocks = new ArrayList<>();
        String currentName = null;
        int count = 0;
        for (Entity entity : sortedEntities) {
            String name = chainName(chainOf(entity)) + metricLabel(chainOf(entity));
            if (name.equals(currentName)) {
                count++;
            } else {
                if (currentName != null) {
                    blocks.add(currentName + " x" + count);
                }
                currentName = name;
                count = 1;
            }
        }
        if (currentName != null) {
            blocks.add(currentName + " x" + count);
        }
        return blocks.toString();
    }

    /**
     * Returns the lowest formation's value under the Formation column sort in use, for the log: " (385 t)",
     * " (BV 8249)", " (walk 3)"; empty for any other sorter or outside a formation.
     */
    private String metricLabel(List<Force> chain) {
        if ((formationSorter == null) || chain.isEmpty()) {
            return "";
        }
        int formationId = chain.get(chain.size() - 1).getId();
        return switch (formationSorter.getMetric()) {
            case TREE_ORDER -> "";
            case TOTAL_BV -> " (BV " + totalBattleValues.getOrDefault(formationId, 0) + ")";
            case TOTAL_TONNAGE -> " (" + Math.round(totalTonnages.getOrDefault(formationId, 0.0)) + " t)";
            case SLOWEST_UNIT -> " (walk " + slowestWalkMPs.getOrDefault(formationId, 0) + ")";
        };
    }

    private static String chainName(List<Force> chain) {
        if (chain.isEmpty()) {
            return "no formation";
        }
        List<String> names = new ArrayList<>();
        for (Force force : chain) {
            names.add(force.getName());
        }
        return String.join(" > ", names);
    }

    private int compare(Entity first, Entity second) {
        List<Force> chainA = chainOf(first);
        List<Force> chainB = chainOf(second);
        int level = 0;
        while ((level < chainA.size()) && (level < chainB.size())
              && (chainA.get(level).getId() == chainB.get(level).getId())) {
            level++;
        }
        boolean firstEnds = level == chainA.size();
        boolean secondEnds = level == chainB.size();

        // The same lowest formation, or both outside any formation
        if (firstEnds && secondEnds) {
            return insideFormation.compare(first, second);
        }
        if (level == 0) {
            return compareTopLevel(first, firstEnds ? null : chainA.get(0), second, secondEnds ? null : chainB.get(0));
        }
        // Inside a shared formation its own units come before its sub formations, as in the force tree
        if (firstEnds) {
            return -1;
        }
        if (secondEnds) {
            return 1;
        }
        return compareSiblings(chainA.get(level - 1), chainA.get(level), chainB.get(level));
    }

    /**
     * Orders two top-level entries: formations, or units outside any formation (given with a {@code null} formation).
     * Different owners are ordered by the base sorter, which is how the default sorter lists the local player first.
     */
    private int compareTopLevel(Entity first, @Nullable Force formationA, Entity second,
          @Nullable Force formationB) {
        Entity representativeA = (formationA == null) ? roots.get(first.getId()) : representative(formationA);
        Entity representativeB = (formationB == null) ? roots.get(second.getId()) : representative(formationB);
        int ownerA = (formationA == null) ? representativeA.getOwnerId() : formationA.getOwnerId();
        int ownerB = (formationB == null) ? representativeB.getOwnerId() : formationB.getOwnerId();

        if (ownerA != ownerB) {
            int comparison = baseSorter.compare(representativeA, representativeB);
            return (comparison != 0) ? comparison : Integer.compare(ownerA, ownerB);
        }
        if (!followTreeOrder) {
            int comparison = baseSorter.compare(representativeA, representativeB);
            if (comparison != 0) {
                return comparison;
            }
        }
        if ((formationA != null) && (formationB != null)) {
            int comparison = compareByMetric(formationA, formationB);
            return (comparison != 0) ? comparison : Forces.TOP_LEVEL_ORDER.compare(formationA, formationB);
        }
        // Formations before units outside any formation, as in the force tree
        return (formationA != null) ? -1 : 1;
    }

    /** Orders two sub formations of the same parent. */
    private int compareSiblings(Force parent, Force formationA, Force formationB) {
        int metricComparison = compareByMetric(formationA, formationB);
        if (metricComparison != 0) {
            return metricComparison;
        }
        if (!followTreeOrder) {
            int comparison = baseSorter.compare(representative(formationA), representative(formationB));
            if (comparison != 0) {
                return comparison;
            }
        }
        return Integer.compare(parent.subForceIndex(formationA), parent.subForceIndex(formationB));
    }

    private List<Force> chainOf(Entity entity) {
        List<Force> chain = chains.get(entity.getId());
        if (chain == null) {
            // Not in the list this grouping was built for; place it by its own formations
            chain = forces.forceChain(MekTableSorter.carrierPath(entity).get(0));
            chains.put(entity.getId(), chain);
            roots.put(entity.getId(), MekTableSorter.carrierPath(entity).get(0));
        }
        return chain;
    }

    private Entity representative(Force formation) {
        return representatives.get(formation.getId());
    }

    private int treeIndex(Entity entity) {
        return treeIndexes.getOrDefault(entity.getId(), Integer.MAX_VALUE);
    }
}
