/*
 * MegaMek - Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */

package megamek.server.commands;

import megamek.common.options.OptionsConstants;
import megamek.server.Server;

/**
 *
 * @author  fastsammy
 * @version
 */
public class NukeCommand extends ServerCommand {

    /** Creates new NukeCommand */
    public NukeCommand(Server server) {
        super(server, "nuke", "Drops a nuke onto the board, to be exploded at" +
                "the end of the next weapons attack phase." +
                "Allowed formats:"+
                "/nuke <x> <y> <type> and" +
                "/nuke <x> <y> <damage> <degredation> <secondary radius> <craterdepth>" +
                "where type is 0-4 (0: Davy-Crockett-I, 1: Davy-Crockett-M, 2: Alamo, 3: Santa Ana, 4: Peacemaker)" +
                "and hex x,y is x=column number and y=row number (hex 0923 would be x=9 and y=23)");

    }

    /**
     * Run this command with the arguments supplied
     */
    @Override
    public void run(int connId, String[] args) {

        // Check to make sure nuking is allowed by game options!
        if (!(server.getGame().getOptions().booleanOption(OptionsConstants.ALLOWED_REALLY_ALLOW_NUKES) && server.getGame().getOptions().booleanOption(OptionsConstants.ALLOWED_ALLOW_NUKES))) {
            server.sendServerChat(connId, "Command-line nukes are not enabled in this game.");
            return;
        }

        // Check argument integrity.
        if (args.length == 4) {
            // Check command type 1
            try {
                int[] nuke = new int[3];
                for (int i = 1; i < 4; i++) {
                    nuke[i-1] = Integer.parseInt(args[i]);
                }
             // is the hex on the board?
                if (!server.getGame().getBoard().contains(nuke[0]-1, nuke[1]-1)) {
                    server.sendServerChat(connId, "Specified hex is not on the board.");
                    return;
                }
                server.addScheduledNuke(nuke);
                server.sendServerChat(connId, "A nuke is incoming!  Take cover!");
            } catch (Exception e) {
                server.sendServerChat(connId, "Nuke command failed (1).  Proper format is \"/nuke <x> <y> <type>\" or \"/nuke <x> <y> <damage> <degredation> <secondary radius> <craterdepth>\" where type is 0-4 (0: Davy-Crockett-I, 1: Davy-Crockett-M, 2: Alamo, 3: Santa Ana, 4: Peacemaker) and hex x,y is x=column number and y=row number (hex 0923 would be x=9 and y=23)");
            }
        } else if (args.length == 7) {
            // Check command type 2.
            try {
                int[] nuke = new int[6];
                for (int i = 1; i < 7; i++) {
                    nuke[i-1] = Integer.parseInt(args[i]);
                }
                // is the hex on the board?
                if (!server.getGame().getBoard().contains(nuke[0]-1, nuke[1]-1)) {
                    server.sendServerChat(connId, "Specified hex is not on the board.");
                    return;
                }
                server.addScheduledNuke(nuke);
                server.sendServerChat(connId, "A nuke is incoming!  Take cover!");
            } catch (Exception e) {
                server.sendServerChat(connId, "Nuke command failed (2).  Proper format is \"/nuke <x> <y> <type>\" or \"/nuke <x> <y> <damage> <degredation> <secondary radius> <craterdepth>\"");
            }
        } else {
            // Error out; it's not a valid call.
            server.sendServerChat(connId, "Nuke command failed (3).  Proper format is \"/nuke <x> <y> <type>\" or \"/nuke <x> <y> <damage> <degredation> <secondary radius> <craterdepth>\" where type is 0-4 (0: Davy-Crockett-I, 1: Davy-Crockett-M, 2: Alamo, 3: Santa Ana, 4: Peacemaker) and hex x,y is x=column number and y=row number (hex 0923 would be x=9 and y=23)");
        }
    }
}
