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
import megamek.server.totalwarfare.TWGameManager;

import java.util.List;
import java.util.Map;

/**
 * @author Luana Scoppio
 */
public class KillCommand extends GamemasterServerCommand{

    private final TWGameManager gameManager;

    /** Creates new KillCommand */
    public KillCommand(Server server, TWGameManager gameManager) {
        super(server, gameManager, "kill", "Allows a GM to destroy a single unit instantly" +
                "Usage: "+
                "/kill <id> " +
                "where id is the units ID. The units ID can be found by hovering over the unit.");
        this.gameManager = gameManager;
    }

    @Override
    public List<Argument<?>> defineArguments() {
        return List.of(new IntegerArgument("unitID", 0, Integer.MAX_VALUE));
    }

    /**
     * Run this command with the arguments supplied
     */
    @Override
    protected void runAsGM(int connId, Map<String, Argument<?>> args) {
        int unitId = (int) args.get("unitID").getValue();
        // is the unit on the board?
        var unit = gameManager.getGame().getEntity(unitId);
        if (unit == null) {
            server.sendServerChat(connId, "Specified unit is not on the board.");
            return;
        }
        gameManager.destroyEntity(unit, "Act of God", false, false);
        server.sendServerChat(unit.getDisplayName() + " has been devastated.");
    }
}
