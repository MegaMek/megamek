/*
 * MegaMek - Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
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

import megamek.common.options.OptionsConstants;
import megamek.server.commands.arguments.*;
import megamek.server.totalwarfare.TWGameManager;
import megamek.server.Server;

import java.util.List;

/**
 * @author fastsammy
 * @author Luana Coppio
 */
public class NukeCommand extends ClientServerCommand {

    private final TWGameManager gameManager;

    /** Creates new NukeCommand */
    public NukeCommand(Server server, TWGameManager gameManager) {
        super(server, gameManager, "nuke", "Drops a nuke onto the board, to be exploded at" +
                "the end of the next weapons attack phase." +
                "Allowed formats:"+
                "/nuke <x> <y> <type> and" +
                "/nuke <x> <y> <damage> <degredation> <secondary radius> <crater>" +
                "where type is 0-4 (0: Davy-Crockett-I, 1: Davy-Crockett-M, 2: Alamo, 3: Santa Ana, 4: Peacemaker)" +
                "and hex x, y is x=column number and y=row number (hex 0923 would be x=9 and y=23)", "Nuclear Strike (old)");
        this.gameManager = gameManager;
    }

    @Override
    public List<Argument<?>> defineArguments() {
        return List.of(
            new CoordXArgument("x", "The x-coordinate of the hex to nuke."),
            new CoordYArgument("y", "The y-coordinate of the hex to nuke."),
            new OptionalIntegerArgument("type", "The type of nuke to drop. " +
                "(0: Davy-Crockett-I, 1: Davy-Crockett-M, 2: Alamo, 3: Santa Ana, 4: Peacemaker)", 0, 4),
            new OptionalIntegerArgument("dmg", "The damage of the nuke.", 0, 1_000_000),
            new OptionalIntegerArgument("deg", "The degredation of the nuke.", 0, 1_000_000),
            new OptionalIntegerArgument("radius", "The secondary radius of the nuke.", 1, 1000),
            new OptionalIntegerArgument("depth", "The crater depth of the nuke.", 0, 100)
        );
    }

    @Override
    protected void runCommand(int connId, Arguments args) {
        // Check to make sure nuking is allowed by game options!
        if (!(server.getGame().getOptions().booleanOption(OptionsConstants.ALLOWED_REALLY_ALLOW_NUKES)
            && server.getGame().getOptions().booleanOption(OptionsConstants.ALLOWED_ALLOW_NUKES))) {
            server.sendServerChat(connId, "Command-line nukes are not enabled in this game.");
            return;
        }

        var typeOpt = ((OptionalIntegerArgument) args.get("type")).getValue();

        if (typeOpt.isPresent()) {
            //
            try {
                int[] nuke = new int[3];
                nuke[0] = ((IntegerArgument) args.get("x")).getValue() - 1;
                nuke[1] = ((IntegerArgument) args.get("y")).getValue() - 1;
                nuke[2] = typeOpt.orElseThrow();
                // is the hex on the board?
                if (!gameManager.getGame().getBoard().contains(nuke[0] , nuke[1])) {
                    server.sendServerChat(connId, "Specified hex is not on the board.");
                    return;
                }
                gameManager.addScheduledNuke(nuke);
                server.sendServerChat(connId, "A nuke is incoming!  Take cover!");
            } catch (Exception e) {
                server.sendServerChat(connId, "Nuke command failed (1). " + getHelp());
            }
        } else {
            try {
                int[] nuke = new int[6];
                nuke[0] = ((IntegerArgument) args.get("x")).getValue() - 1;
                nuke[1] = ((IntegerArgument) args.get("y")).getValue() - 1;
                nuke[2] = ((OptionalIntegerArgument) args.get("dmg")).getValue().orElseThrow();
                nuke[3] = ((OptionalIntegerArgument) args.get("deg")).getValue().orElseThrow();
                nuke[4] = ((OptionalIntegerArgument) args.get("radius")).getValue().orElseThrow();
                nuke[5] = ((OptionalIntegerArgument) args.get("depth")).getValue().orElseThrow();

                // is the hex on the board?
                if (!gameManager.getGame().getBoard().contains(nuke[0], nuke[1])) {
                    server.sendServerChat(connId, "Specified hex is not on the board.");
                    return;
                }
                gameManager.addScheduledNuke(nuke);
                server.sendServerChat(connId, "A nuke is incoming!  Take cover!");
            } catch (Exception e) {
                server.sendServerChat(connId, "Nuke command failed (2). " + getHelp());
            }
        }
    }
}
