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

import megamek.common.Entity;
import megamek.common.ForceAssignable;
import megamek.common.IGame;
import megamek.common.Player;
import megamek.common.force.Force;
import megamek.common.force.Forces;
import megamek.common.icons.Camouflage;

import java.util.*;
import java.util.stream.Collectors;

/**
 * BalancedConsolidateForces is a helper class that redistribute entities and forces
 * in a way to consolidate then into valid forces to build Formations out of them.
 * @author Luana Coppio
 */
public abstract class ForceConsolidation {

    protected abstract int getMaxEntitiesInSubForce();
    protected abstract int getMaxEntitiesInTopLevelForce();

    /**
     * Consolidates forces by redistributing entities and sub forces as needed.
     * It will balance the forces by team, ensuring that each force has a maximum of 20 entities and 4 sub forces.
     * @param game The game to consolidate forces for
     */
    public void consolidateForces(IGame game) {
        Forces forces = game.getForces();
        var teamByPlayer = game.getTeamByPlayer();
        var forceNameByPlayer = new HashMap<Integer, String>();
        for (var force : forces.getAllForces()) {
            if (!forceNameByPlayer.containsKey(force.getOwnerId())) {
                forceNameByPlayer.put(force.getOwnerId(), force.getName());
            }
        }
        var representativeOwnerForForce = new HashMap<Integer, List<Player>>();
        for (var force : forces.getAllForces()) {
            representativeOwnerForForce.computeIfAbsent(teamByPlayer.get(force.getOwnerId()), k -> new ArrayList<>()).add(game.getPlayer(force.getOwnerId()));
        }

        List<BalancedConsolidateForces.ForceRepresentation> forceRepresentation = getForceRepresentations(forces, teamByPlayer);
        var balancedConsolidateForces = balanceForces(forceRepresentation);

        clearAllForces(forces);

        for (var forceRep : balancedConsolidateForces) {
            var player = representativeOwnerForForce.get(forceRep.teamId()).get(0);
            var parentForceId = forces.addTopLevelForce(
                new Force(
                    "[Team " + forceRep.teamId()  + "] "+ forceNameByPlayer.get(player.getId()) + " Formation",
                    -1,
                    new Camouflage(),
                    player),
                player);
            for (var subForce : forceRep.subs()) {
                var subForceId = forces.addSubForce(
                    new Force(
                        "[Team " + forceRep.teamId()  + "] " + subForce.uid() + " Unit",
                        -1,
                        new Camouflage(),
                        player),
                    forces.getForce(parentForceId));
                for (var entityId : subForce.entities()) {
                    forces.addEntity((Entity) game.getEntityFromAllSources(entityId), subForceId);
                }
            }
        }
    }

    private void clearAllForces(Forces forces) {
        // Remove all empty forces and sub forces after consolidation
        forces.deleteForces(forces.getAllForces());
    }

    /**
     * Converts the forces into a list of ForceRepresentations. It is an intermediary representation of a force, in a way that makes it very
     * lightweight to manipulate and balance. It only contains the representation of the force top-level, and the list of entities in it.
     * @param forces The forces to convert
     * @param teamByPlayer A map of player IDs to team IDs
     * @return A list of ForceRepresentations
     */
    private List<BalancedConsolidateForces.ForceRepresentation> getForceRepresentations(Forces forces, Map<Integer, Integer> teamByPlayer) {
        List<BalancedConsolidateForces.ForceRepresentation> forceRepresentations = new ArrayList<>();
        for (Force force : forces.getTopLevelForces()) {
            int[] entityIds = forces.getFullEntities(force).stream().mapToInt(ForceAssignable::getId).toArray();
            forceRepresentations.add(new BalancedConsolidateForces.ForceRepresentation(force.getId(), teamByPlayer.get(force.getOwnerId()), entityIds, new int[0]));
        }
        return forceRepresentations;
    }

    public record Container(int uid, int teamId, int[] entities, Container[] subs) {
        public boolean isLeaf() {
            return subs.length == 0 && entities.length > 0;
        }

        public boolean isTop() {
            return subs.length > 0 && entities.length == 0;
        }

        @Override
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
    public List<Container> balanceForces(List<ForceRepresentation> forces) {

        Map<Integer, Set<Integer>> entitiesByTeam = new HashMap<>();
        for (ForceRepresentation c : forces) {
            entitiesByTeam.computeIfAbsent(c.teamId(), k -> new HashSet<>()).addAll(Arrays.stream(c.entities()).boxed().toList());
        }

        // Find the number of top-level containers for each team
        Map<Integer, Integer> numOfEntitiesByTeam = entitiesByTeam.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().size()));

        int maxEntities = numOfEntitiesByTeam.values().stream().max(Integer::compareTo).orElse(0);
        int topCount = (int) Math.ceil((double) maxEntities / getMaxEntitiesInTopLevelForce());

        Map<Integer, Container> balancedForces = new HashMap<>();

        for (int team : entitiesByTeam.keySet()) {
            createTopLevelForTeam(balancedForces, team, new ArrayList<>(entitiesByTeam.get(team)), topCount);
        }

        return new ArrayList<>(balancedForces.values());
    }

    private void createTopLevelForTeam(Map<Integer, Container> balancedForces, int team, List<Integer> allEntityIds, int topCount) {
        int maxId = balancedForces.keySet().stream().max(Integer::compareTo).orElse(0) + 1;

        int maxEntitiesPerTopLevelForce = (int) Math.min(Math.ceil((double) allEntityIds.size() / topCount), getMaxEntitiesInTopLevelForce());

        int idx = 0;

        for (int i = 0; i < topCount; i++) {
            int end = Math.min(idx + maxEntitiesPerTopLevelForce, allEntityIds.size());
            List<Integer> subListOfEntityIds = allEntityIds.subList(idx, end);
            idx = end;

            List<Container> subForces = new ArrayList<>();
            int step = Math.min(subListOfEntityIds.size(), getMaxEntitiesInSubForce());
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
}

