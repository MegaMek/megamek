/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.server.commands;

import java.util.List;

import megamek.common.Entity;
import megamek.common.LosEffects;
import megamek.common.TargetRoll;
import megamek.common.ToHitData;
import megamek.server.Server;
import megamek.server.totalwarfare.TWGameManager;

public class ShowValidTargetsCommand extends ServerCommand {

    private final TWGameManager gameManager;

    public ShowValidTargetsCommand(Server server, TWGameManager gameManager) {
        super(server,
              "validTargets",
              "Shows a list of entity id's that are valid targets for the current entity. Usage: /validTargets # where # is the id number of the entity you are shooting from.");
        this.gameManager = gameManager;
    }

    @Override
    public void run(int connId, String... args) {
        try {
            int id = Integer.parseInt(args[1]);
            Entity ent = gameManager.getGame().getEntity(id);

            if (ent != null) {
                String str = "No valid targets.";
                boolean canHit = false;
                ToHitData thd;

                List<Entity> entList = gameManager.getGame().getValidTargets(ent);
                Entity target;

                for (int i = 0; i < entList.size(); i++) {
                    target = entList.get(i);
                    thd = LosEffects.calculateLOS(gameManager.getGame(), ent, target)
                          .losModifiers(gameManager.getGame());
                    if (thd.getValue() != TargetRoll.IMPOSSIBLE) {
                        thd.setSideTable(target.sideTable(ent.getPosition()));

                        if (!canHit) {
                            str = "This entity(" + id
                                  + ") can shoot the following entities: \n";
                            canHit = true;
                        }
                        str = str + entList.get(i).getId()
                              + " at a to hit penalty of ";
                        str = str
                              + thd.getValue()
                              + ", at range " + ent.getPosition().distance(entList.get(i).getPosition())
                              + thd.getTableDesc() + ";\n";
                    }

                }

                server.sendServerChat(connId, str);
            } else {
                server.sendServerChat(connId, "No such entity.");
            }
        } catch (Exception ignored) {

        }
    }
}
