/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
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
package megamek.common.autoresolve.converter;

import java.util.*;
import java.util.stream.Collectors;

/**
 * BalancedConsolidateForces is a helper class that redistribute entities and forces
 * in a way to consolidate then into valid forces to build Formations out of them.
 * @author Luana Coppio
 */
public class BalancedConsolidateForces {

    public static final int MAX_ENTITIES_IN_SUB_FORCE = 6;
    public static final int MAX_ENTITIES_IN_TOP_LEVEL_FORCE = 20;

    public record Container(int uid, int teamId, int[] entities, Container[] subs) {
        public boolean isLeaf() {
            return subs.length == 0 && entities.length > 0;
        }

        public boolean isTop() {
            return subs.length > 0 && entities.length == 0;
        }

        public String toString() {
            return "Container(uid=" + uid + ", team=" + teamId + ", ent=" + Arrays.toString(entities) + ", subs=" + Arrays.toString(subs) + ")";
        }
    }

    public record ForceRepresentation(int uid, int teamId, int[] entities, int[] subForces) {
        public boolean isLeaf() {
            return subForces.length == 0 && entities.length > 0;
        }

        public boolean isTop() {
            return subForces.length > 0 && entities.length == 0;
        }
    }

    /**
     * Balances the forces by team, tries to ensure that every team has the same number of top level forces, each within the ACS parameters
     * of a maximum of 20 entities and 4 sub forces. It also aggregates the entities by team instead of keeping segregated by player.
     * See the test cases for examples on how it works.
     * @param forces List of Forces to balance
     * @return List of Trees representing the balanced forces
     */
    public static List<Container> balancedLists(List<ForceRepresentation> forces) {
        Map<Integer, Set<Integer>> entitiesByTeam = new HashMap<>();
        for (ForceRepresentation c : forces) {
            entitiesByTeam.computeIfAbsent(c.teamId(), k -> new HashSet<>()).addAll(Arrays.stream(c.entities()).boxed().toList());
        }

        // Find the number of top-level containers for each team
        Map<Integer, Integer> numOfEntitiesByTeam = entitiesByTeam.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().size()));

        int maxEntities = numOfEntitiesByTeam.values().stream().max(Integer::compareTo).orElse(0);
        int topCount = (int) Math.ceil((double) maxEntities / MAX_ENTITIES_IN_TOP_LEVEL_FORCE);

        Map<Integer, Container> balancedForces = new HashMap<>();

        for (int team : entitiesByTeam.keySet()) {
            createTopLevelForTeam(balancedForces, team, new ArrayList<>(entitiesByTeam.get(team)), topCount);
        }

        return new ArrayList<>(balancedForces.values());
    }

    private static void createTopLevelForTeam(Map<Integer, Container> balancedForces, int team, List<Integer> allEntityIds, int topCount) {
        int maxId = balancedForces.keySet().stream().max(Integer::compareTo).orElse(0) + 1;

        int maxEntitiesPerTopLevelForce = (int) Math.min(Math.ceil((double) allEntityIds.size() / topCount), MAX_ENTITIES_IN_TOP_LEVEL_FORCE);

        int idx = 0;

        for (int i = 0; i < topCount; i++) {
            int end = Math.min(idx + maxEntitiesPerTopLevelForce, allEntityIds.size());
            List<Integer> subListOfEntityIds = allEntityIds.subList(idx, end);
            idx = end;

            List<Container> subForces = new ArrayList<>();
            int step = Math.min(subListOfEntityIds.size(), MAX_ENTITIES_IN_SUB_FORCE);
            for (int start = 0; start < subListOfEntityIds.size(); start += step) {
                var subForceSize = Math.min(subListOfEntityIds.size(), start + step);
                Container leaf = new Container(
                    maxId++,
                    team,
                    subListOfEntityIds.subList(start, subForceSize).stream().mapToInt(Integer::intValue).toArray(),
                    new Container[0]);
                subForces.add(leaf);
            }

            if (subForces.isEmpty()) {
                // no entities? skip creating top-level
                break;
            }

            var subForcesArray = new Container[subForces.size()];
            for (int k = 0; k < subForcesArray.length; k++) {
                subForcesArray[k] = subForces.get(k);
            }

            Container top = new Container(maxId++, team, new int[0], subForcesArray);
            balancedForces.put(top.uid(), top);
        }
    }

    /**
     * Checks if the forces are balanced, meaning that each team has the same number of top level forces, each within the ACS parameters
     * This function isn't used outside of tests, but it can be useful to check if the forces are balanced after consolidation
     * @param postBalanceForces The list of forces post balance
     * @return True if the forces are balanced, false otherwise
     */
    @SuppressWarnings("unused")
    public static boolean isBalanced(List<Container> postBalanceForces) {
        if (postBalanceForces.isEmpty()) {
            return false;
        }
        Map<Integer, List<Container>> resMap = new HashMap<>();
        for (Container c : postBalanceForces) {
            if (c.isTop()) resMap.computeIfAbsent(c.teamId(), k -> new ArrayList<>()).add(c);
        }

        List<Integer> counts = resMap.values().stream().map(List::size).toList();
        int min = Collections.min(counts), max = Collections.max(counts);
        return max - min <= 1;
    }
}

