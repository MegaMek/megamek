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
import megamek.logging.MMLogger;

import java.util.*;
import java.util.stream.Collectors;

/**
 * BalancedConsolidateForces is a helper class that redistribute entities and forces
 * in a way to consolidate then into valid forces to build Formations out of them.
 * @author Luana Coppio
 */
public abstract class ForceConsolidation {
    private static final MMLogger logger = MMLogger.create(ForceConsolidation.class);

    public record Container(int uid, String name, String breadcrumb, int teamId, int playerId, List<Integer> entities, List<Container> subs) {
        public boolean isLeaf() {
            return subs.isEmpty() && !entities.isEmpty();
        }

        public boolean isTop() {
            return !subs.isEmpty() && entities.isEmpty();
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", Container.class.getSimpleName() + "[", "]")
                .add("uid=" + uid)
                .add("breadcrumb='" + breadcrumb + "'")
                .add("name='" + name + "'")
                .add("teamId=" + teamId)
                .add("playerId=" + playerId)
                .add("entities=" + entities)
                .add("subs=" + subs)
                .toString();
        }
    }

    public record ForceRepresentation(int uid, int teamId, int playerId, int[] entities, int[] subForces) {
        public boolean isLeaf() {
            return subForces.length == 0 && entities.length > 0;
        }

        public boolean isTop() {
            return subForces.length > 0 && entities.length == 0;
        }
    }

    /**
     * Finds a cycle in the forces, if any.
     * @param forces The forces to check for cycles
     * @return The force that is the root of a cycle, or null if no cycle is found
     */
    public static Force cycleFinder(Forces forces) {
        var forcesInternalRepresentation = forces.getForcesInternalRepresentation();
        var visited = new HashSet<Integer>();
        var stack = new ArrayDeque<>(forces.getTopLevelForces());

        while (!stack.isEmpty()) {
            var force = stack.pop();
            if (!visited.add(force.getId())) {
                return force;
            }
            for (var subForceId : force.getSubForces()) {
                var subForce = forcesInternalRepresentation.get(subForceId);
                if (subForce != null) {
                    stack.push(subForce);
                }
            }
        }
        return null;
    }

    /**
     * Consolidates forces by redistributing entities and sub forces as needed.
     * It will balance the forces by team, ensuring that each force has a maximum of 20 entities and 4 sub forces.
     * @param game The game to consolidate forces for
     */
    public abstract void consolidateForces(IGame game);

    protected static void createForcesOnGame(
        IGame game,
        List<Container> consolidatedForces,
        Forces forces
    )
    {
        for (var forceRep : consolidatedForces) {
            var player = game.getPlayer(forceRep.playerId());
            var camouflage = player.getCamouflage().clone();

            depthFirstSearch(
                game,
                forceRep,
                forces,
                player,
                camouflage,
                -1);
        }
    }

    protected static void depthFirstSearch(
        IGame game,
        Container forceRep,
        Forces forces,
        Player player,
        Camouflage camouflage,
        int parentForceId
    ) {
        var node = new Force(
            forceRep.name(),
            -1,
            camouflage,
            player);
        int newForceId;
        if (parentForceId == -1) {
            newForceId = forces.addTopLevelForce(node, player);
        } else {
            newForceId = forces.addSubForce(node, forces.getForce(parentForceId));
        }
        for (var entityId : forceRep.entities()) {
            var entity = (Entity) game.getEntityFromAllSources(entityId);
            if (entity == null) {
                logger.error("Entity id " + entityId + " not found in game, could not load at " + forceRep);
                continue;
            }
            forces.addEntity(entity, newForceId);
        }
        for (var subForce : forceRep.subs()) {
            depthFirstSearch(
                game,
                subForce,
                forces,
                player,
                camouflage,
                newForceId);
        }

    }

    protected static void clearAllForces(Forces forces) {
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
    protected List<ForceRepresentation> translateForcesToForceRepresentation(Forces forces, Map<Integer, Integer> teamByPlayer) {
        List<ForceRepresentation> forceRepresentations = new ArrayList<>();
        for (Force force : forces.getTopLevelForces()) {
            int[] entityIds = forces.getFullEntities(force).stream().mapToInt(ForceAssignable::getId).toArray();
            forceRepresentations.add(new ForceRepresentation(force.getId(), teamByPlayer.get(force.getOwnerId()), force.getOwnerId(), entityIds, new int[0]));
        }
        return forceRepresentations;
    }

}

