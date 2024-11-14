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
import megamek.server.commands.arguments.Argument;
import megamek.server.commands.arguments.IntegerArgument;
import megamek.server.props.OrbitalBombardment;
import megamek.server.totalwarfare.TWGameManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Luana Scoppio
 */
public class OrbitalBombardmentCommand extends GamemasterServerCommand {

    public OrbitalBombardmentCommand(Server server, TWGameManager gameManager) {
        super(server, gameManager, "ob", "GM orders an unknown warship to strike the board doing of 100 damage with a 4 hex radius, to be exploded at"
            + "the end of the next weapons attack phase."
            + "Allowed formats: "
            + "/ob <x> <y> and"
            + "/ob <x> <y> [dmg=#] [r=#] and"
            + "/ob <x> <y> <dmg> and "
            + "/ob <x> <y> <dmg> <r> "
            + "X and Y are the hex position where is x=column number and y=row number (hex 0923 would be x=9 and y=23), "
            + "dmg is the amount of damage at impact point, default is 100, the damage drops off linearly according to the radius. "
            + "r is radius, defaults to 4. "
            + " Example: /ob 10 10 dmg=120 r=4 and /ob 10 10 120 4 are equivalent. "
            + " Parameters in square brackets may be omitted, and using named variables permits to write them out of order. "
            + " Example: /ob 10 10 r=12 dmg=300");
    }

    @Override
    public List<Argument<?>> defineArguments() {
        return List.of(
            new IntegerArgument("x"),
            new IntegerArgument("y"),
            new IntegerArgument("dmg", 10, Integer.MAX_VALUE, 100),
            new IntegerArgument("r", 1, 10, 4));
    }

    /**
     * Run this command with the arguments supplied
     */
    @Override
    protected void runAsGM(int connId, Map<String, Argument<?>> args) {

        var orbitalBombardmentBuilder = new OrbitalBombardment.Builder();

        orbitalBombardmentBuilder.x((int) args.get("x").getValue() - 1).y((int) args.get("y").getValue() - 1);
        orbitalBombardmentBuilder.radius((int) args.get("r").getValue());
        orbitalBombardmentBuilder.damageFactor((int) args.get("dmg").getValue());
        var orbitalBombardment = orbitalBombardmentBuilder.build();

        // is the hex on the board?
        if (!gameManager.getGame().getBoard().contains(orbitalBombardment.getX(), orbitalBombardment.getY())) {
            server.sendServerChat(connId, "Specified hex is not on the board.");
            return;
        }

        gameManager.addScheduledOrbitalBombardment(orbitalBombardment);
        server.sendServerChat("Orbital bombardment incoming!");
    }

}
