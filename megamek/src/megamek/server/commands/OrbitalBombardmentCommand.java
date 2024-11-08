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

import megamek.common.options.OptionsConstants;
import megamek.server.Server;
import megamek.server.props.OrbitalBombardment;
import megamek.server.totalwarfare.TWGameManager;

/**
 * @author Luana Scoppio
 */
public class OrbitalBombardmentCommand extends ServerCommand implements IsGM {

    private final TWGameManager gameManager;

    /** Creates new NukeCommand */
    public OrbitalBombardmentCommand(Server server, TWGameManager gameManager) {
        super(server, "ob", "GM Drops a bomb onto the board doing of 100 damage with a 3 hex radius, to be exploded at" +
                "the end of the next weapons attack phase." +
                "Allowed formats:"+
                "/bomb <x> <y> and" +
                "/bomb <x> <y> [factor=10] [radius=4]" +
                "the damage at impact point is 10 times the factor, default is 10. " +
                "and hex x, y is x=column number and y=row number (hex 0923 would be x=9 and y=23), the explosion blast radius default " +
                "is equal to 4, it automatically applies a linear damage dropoff each hex away from the center." +
                " All parameters in square brackets may be ommited. " +
                " Example: /ob 10 10 factor=12 ");
        this.gameManager = gameManager;
    }

    /**
     * Run this command with the arguments supplied
     */
    @Override
    public void run(int connId, String[] args) {

        // Check to make sure nuking is allowed by game options!
        if (!isGM(connId)) {
            server.sendServerChat(connId, "You are not a Game Master.");
            return;
        }

        if (args.length >= 3) {
            var orbitalBombardmentBuilder = new OrbitalBombardment.Builder();
            try {
                int[] position = new int[2];
                for (int i = 1; i < 3; i++) {
                    position[i-1] = Integer.parseInt(args[i]) - 1;
                }
                // is the hex on the board?
                if (!gameManager.getGame().getBoard().contains(position[0], position[1])) {
                    server.sendServerChat(connId, "Specified hex is not on the board.");
                    return;
                }

                orbitalBombardmentBuilder
                    .x(position[0])
                    .y(position[1]);

                if (args.length > 3) {
                    for (int i = 3; i < args.length; i++) {
                        String[] keyValue = args[i].split("=");
                        if (keyValue[0].equals("factor")) {
                            orbitalBombardmentBuilder.damageFactor(Integer.parseInt(keyValue[1]));
                        } else if (keyValue[0].equals("radius")) {
                            orbitalBombardmentBuilder.radius(Integer.parseInt(keyValue[1]));
                        }
                    }
                }

                gameManager.addScheduledOrbitalBombardment(orbitalBombardmentBuilder.build());
                server.sendServerChat(connId, "This isn't a shooting star!  Take cover!");
            } catch (Exception e) {
                server.sendServerChat(connId, "Orbital bombardment command failed (2). Proper format is \"/ob <x> <y> [factor=10] [radius=4]\" where hex x, y is x=column number and y=row number (hex 0923 would be x=9 and y=23)");
            }
        } else {
            // Error out; it's not a valid call.
            server.sendServerChat(connId, "Orbital bombardment command failed (1). Proper format is \"/ob <x> <y> [factor=10] [radius=4]\" where hex x, y is x=column number and y=row number (hex 0923 would be x=9 and y=23)");
        }
    }

    @Override
    public TWGameManager getGameManager() {
        return gameManager;
    }
}
