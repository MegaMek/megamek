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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Luana Coppio
 */
public class ConsolidateForces {
    /**
     * Consolidates forces by redistributing entities and sub forces as needed.
     * It will balance the forces by team, ensuring that each force has a maximum of 20 entities and 4 sub forces.
     * @param game The game to consolidate forces for
     */
    public static void consolidateForces(IGame game) {
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
        var balancedConsolidateForces = BalancedConsolidateForces.balancedLists(forceRepresentation);

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

    private static void clearAllForces(Forces forces) {
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
    private static List<BalancedConsolidateForces.ForceRepresentation> getForceRepresentations(Forces forces, Map<Integer, Integer> teamByPlayer) {
        List<BalancedConsolidateForces.ForceRepresentation> forceRepresentations = new ArrayList<>();
        for (Force force : forces.getTopLevelForces()) {
            int[] entityIds = forces.getFullEntities(force).stream().mapToInt(ForceAssignable::getId).toArray();
            forceRepresentations.add(new BalancedConsolidateForces.ForceRepresentation(force.getId(), teamByPlayer.get(force.getOwnerId()), entityIds, new int[0]));
        }
        return forceRepresentations;
    }
}
