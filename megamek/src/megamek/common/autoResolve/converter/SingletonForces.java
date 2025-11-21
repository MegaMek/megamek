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
package megamek.common.autoResolve.converter;

import java.util.ArrayList;
import java.util.List;

import megamek.common.force.Force;
import megamek.common.force.Forces;
import megamek.common.game.IGame;
import megamek.common.units.Entity;

/**
 * Each unit is a formation
 *
 * @author Luana Coppio
 */
public class SingletonForces extends ForceConsolidation {

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
                forceId = transformIntoTopLevelForce(game, force, force, newTopLevelForces, forceId, team);
            } else {
                for (var subForce : game.getForces().getFullSubForces(force)) {
                    forceId = transformIntoTopLevelForce(game, force, subForce, newTopLevelForces, forceId, team);
                }
            }
        }
        game.setForces(new Forces(game));
        createForcesOnGame(game, newTopLevelForces, game.getForces());
    }

    private static int transformIntoTopLevelForce(IGame game, Force force, Force subForce,
          ArrayList<Container> newTopLevelForces, int forceId, int team) {
        var hasEntities = subForce.entityCount() > 0;
        if (hasEntities) {
            for (var entityId : subForce.getEntities()) {
                var optionalEntity = game.getInGameObject(entityId);
                if (optionalEntity.isPresent() && optionalEntity.get() instanceof Entity entity) {
                    var topLevel = new Container(forceId++,
                          subForce.getName(),
                          force.getName(),
                          team,
                          force.getOwnerId(),
                          new ArrayList<>(),
                          new ArrayList<>());
                    topLevel.subs().add(
                          new Container(forceId++,
                                entity.getDisplayName(),
                                force.getName() + ">" + subForce.getName(),
                                team,
                                force.getOwnerId(),
                                List.of(entityId),
                                new ArrayList<>())
                    );
                    newTopLevelForces.add(topLevel);
                }
            }
        }
        return forceId;
    }

}

