/*
 * MegaMek - Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
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
 */
package megamek.server.commands;

import megamek.client.ui.Messages;
import megamek.common.moves.MovePath;
import megamek.server.Server;
import megamek.server.commands.arguments.Argument;
import megamek.server.commands.arguments.Arguments;
import megamek.server.commands.arguments.UnitArgument;
import megamek.server.totalwarfare.TWGameManager;

import java.util.List;

/**
 * @author Luana Coppio
 */
public class RescueCommand extends GamemasterServerCommand{

    public static final String UNIT_ID = "unitID";

    /** Creates new Rescue Command, it "flees" a unit no matter where they are */
    public RescueCommand(Server server, TWGameManager gameManager) {
        super(server, gameManager, "rescue", Messages.getString("Gamemaster.cmd.rescue.help"),
            Messages.getString("Gamemaster.cmd.rescue.longName"));
    }

    @Override
    public List<Argument<?>> defineArguments() {
        return List.of(new UnitArgument(UNIT_ID, Messages.getString("Gamemaster.cmd.rescue.unitID")));
    }

    /**
     * Run this command with the arguments supplied
     */
    @Override
    protected void runCommand(int connId, Arguments args) {
        int unitId = (int) args.get(UNIT_ID).getValue();
        // is the unit on the board?
        var unit = gameManager.getGame().getEntity(unitId);
        if (unit == null) {
            server.sendServerChat(connId, Messages.getString("Gamemaster.cmd.missingUnit"));
            return;
        }
        MovePath path = new MovePath(gameManager.getGame(), unit);
        path.addStep(MovePath.MoveStepType.FLEE);
        gameManager.addReport(gameManager.processLeaveMap(path));
        server.sendServerChat(Messages.getString("Gamemaster.cmd.rescue.success", unit.getDisplayName()));
    }
}
