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

/*
 * FixElevationCommand.java
 *
 * Created on April 18, 2002, 11:53 PM
 */

package megamek.server.commands;

import megamek.common.*;
import megamek.common.actions.NukeAttackAction;
import megamek.server.*;

/**
 *
 * @author  fastsammy
 * @version 
 */
public class NukeCommand extends ServerCommand {

    /** Creates new NukeCommand */
    public NukeCommand(Server server) {
        super(server, "nuke", "Drops a nuke onto the board.");
    }

    /**
     * Run this command with the arguments supplied
     */
    public void run(int connId, String[] args) {
        int countbad = 0;

        // Check to make sure nuking is allowed by game options!
        boolean doBlind = server.getGame().getOptions().booleanOption("really_allow_nukes");
        if (!(server.getGame().getOptions().booleanOption("really_allow_nukes") && server.getGame().getOptions().booleanOption("allow_nukes"))) {
            server.sendServerChat(connId, "Command-line nukes are not enabled in this game.");
            return;
        }

        // Check argument integrity.
        if (args.length == 4) {
            // Check command type 1
            try {
                int x = new Integer(args[1]) - 1;
                int y = new Integer(args[2]) - 1;
                int type = new Integer(args[3]);
                server.getGame().addArtilleryAttack(new NukeAttackAction(new Coords(x, y), type, -1));
                server.sendServerChat(connId, "A nuke is incoming!  Take cover!");
            } catch (Exception e) {
                server.sendServerChat(connId, "Nuke command failed (1).  Proper format is \"/nuke <x> <y> <type>\" or \"/nuke <x> <y> <damage> <degredation> <secondary radius> <craterdepth>\"");
            }
        } else if (args.length == 7) {
            // Check command type 2.
            try {
                int x = new Integer(args[1]) - 1;
                int y = new Integer(args[2]) - 1;
                int damage = new Integer(args[3]);
                int degredation = new Integer(args[4]);
                int secRad = new Integer(args[5]);
                int cratDep = new Integer(args[6]);
                server.getGame().addArtilleryAttack(new NukeAttackAction(new Coords(x, y), damage, degredation, secRad, cratDep, -1));
                server.sendServerChat(connId, "A nuke is incoming!  Take cover!");
            } catch (Exception e) {
                server.sendServerChat(connId, "Nuke command failed (2).  Proper format is \"/nuke <x> <y> <type>\" or \"/nuke <x> <y> <damage> <degredation> <secondary radius> <craterdepth>\"");
            }
        } else {
            // Error out; it's not a valid call.
            server.sendServerChat(connId, "Nuke command failed (3).  Proper format is \"/nuke <x> <y> <type>\" or \"/nuke <x> <y> <damage> <degredation> <secondary radius> <craterdepth>\"");
        }
    }
}
