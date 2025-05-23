/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package megamek.common.autoresolve.converter;

import megamek.common.IGame;
import megamek.common.force.Force;
import megamek.common.force.Forces;

import java.util.*;

public class KeepCurrentForces extends ForceConsolidation {

    @Override
    public void consolidateForces(IGame game) {
        var newTopLevelForces = new ArrayList<Container>();
        var cycleFound = KeepCurrentForces.cycleFinder(game.getForces());
        if (cycleFound != null) {
            throw new IllegalStateException("Cycle detected in forces " + cycleFound.getName() + " " + cycleFound.getId());
        }
        var forcesInternalRepresentation = game.getForces().getForcesInternalRepresentation();

        Deque<Force> queue = new ArrayDeque<>(game.getForces().getTopLevelForces());
        int forceId = 0;
        var newForceMap = new HashMap<Integer, Container>();
        var entityDuplicationChecker = new HashSet<Integer>();
        while (!queue.isEmpty()) {
            var force = queue.poll();
            if (force == null) {
                continue;
            }
            var parentForce = forcesInternalRepresentation.get(force.getParentId());
            var parentNode = parentForce == null ? null : newForceMap.get(parentForce.getId());
            var breadcrumb = "";
            while (parentForce != null) {
                breadcrumb = parentForce.getName() + " > ";
                parentForce = forcesInternalRepresentation.get(parentForce.getParentId());
            }
            var player = game.getPlayer(force.getOwnerId());
            var team = player.getTeam();
            var container = new Container(forceId++, force.getName(), breadcrumb, team, force.getOwnerId(), new ArrayList<>(), new ArrayList<>());
            newForceMap.put(force.getId(), container);

            for (var entityId : force.getEntities()) {
                if (entityDuplicationChecker.contains(entityId)) {
                    throw new IllegalStateException("Entity " + entityId + " is duplicated");
                }
                container.entities().add(entityId);
                entityDuplicationChecker.add(entityId);
            }
            if (parentNode != null) {
                parentNode.subs().add(container);
            } else {
                newTopLevelForces.add(container);
            }

            for (var subForceId : force.getSubForces()) {
                var subForce = forcesInternalRepresentation.get(subForceId);
                queue.add(subForce);
            }
        }

        game.setForces(new Forces(game));
        createForcesOnGame(game, newTopLevelForces, game.getForces());
    }
}
