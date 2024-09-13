/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
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
        super(server, "validTargets",
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
