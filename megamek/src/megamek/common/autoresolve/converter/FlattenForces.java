/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 *
 */

package megamek.common.autoresolve.converter;

import megamek.common.IGame;
import megamek.common.force.Force;
import megamek.common.force.Forces;

import java.util.ArrayList;

public class FlattenForces extends ForceConsolidation {

    @Override
    protected int getMaxEntitiesInSubForce() {
        return -1;
    }

    @Override
    protected int getMaxEntitiesInTopLevelForce() {
        return -1;
    }

    @Override
    public void consolidateForces(IGame game) {

        var newTopLevelForces = new ArrayList<Container>();
        int forceId = 0;
        for (var force : game.getForces().getTopLevelForces()) {
            var player = game.getPlayer(force.getOwnerId());
            var team = player.getTeam();
            var hasNoSubForce = force.subForceCount() == 0;
            var hasEntities = force.entityCount() > 0;
            if (hasNoSubForce && hasEntities) {
                forceId = transformIntoTopLevelForce(force, force, newTopLevelForces, forceId, team);
            } else {
                for (var subForce : game.getForces().getFullSubForces(force)) {
                    forceId = transformIntoTopLevelForce(force, subForce, newTopLevelForces, forceId, team);
                }
            }
        }
        game.setForces(new Forces(game));
        createForcesOnGame(game, newTopLevelForces, game.getForces());
    }

    private static int transformIntoTopLevelForce(Force force, Force subForce, ArrayList<Container> newTopLevelForces, int forceId, int team) {
        var hasNoSubForce = subForce.subForceCount() == 0;
        var hasEntities = subForce.entityCount() > 0;
        if (hasNoSubForce && hasEntities) {

            var topLevel = new Container(forceId++, subForce.getName(), force.getName(),  team, force.getOwnerId(), new ArrayList<>(), new ArrayList<>());
            topLevel.subs().add(
                new Container(forceId++, subForce.getName(), force.getName(), team, force.getOwnerId(),  new ArrayList<>(subForce.getEntities()), new ArrayList<>())
            );
            newTopLevelForces.add(topLevel);
        }
        return forceId;
    }

}
