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
 * LoadGameCommand.java
 *
 * Created on November 19, 2008
 */

package megamek.server.commands;

import java.io.File;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import megamek.common.IPlayer;
import megamek.common.net.IConnection;
import megamek.server.Server;

/**
 * Resets the server
 * 
 * @author Taharqa
 * @version
 */
public class LoadGameCommand extends ServerCommand {

    /** Creates new ResetCommand */
    public LoadGameCommand(Server server) {
        super(server, "load",
                "load a saved game from the savegames directory.  Usage: /load [filename]");
    }

    /**
     * Run this command with the arguments supplied
     */
    @Override
    public void run(int connId, String[] args) {
        if (!canRunRestrictedCommand(connId)) {
            server.sendServerChat(connId,
                    "Observers are restricted from loading games.");
            return;
        }
        if (args.length > 1) {
            String sFinalFile = args[1];
            if (!sFinalFile.endsWith(".sav") 
                    && !sFinalFile.endsWith(".sav.gz")) {
                sFinalFile = sFinalFile + ".sav";
            }
            if (!sFinalFile.endsWith(".gz")) {
                sFinalFile = sFinalFile + ".gz";
            }
            load(new File("savegames", sFinalFile), connId);
        } else {
            server.sendServerChat(connId, "you must provide a file name");
        }
    }

    private void load(File f, int connId) {
        server.sendServerChat(server.getPlayer(connId).getName()
                + " loaded a new game.");
        // Keep track of the current id to name mapping
        Map<String, Integer> nameToIdMap = new HashMap<>();
        Map<Integer, String> idToNameMap = new HashMap<>();
        for (IPlayer p: server.getGame().getPlayersVector()) {
            nameToIdMap.put(p.getName(), p.getId());
            idToNameMap.put(p.getId(), p.getName());
        }
        if(!server.loadGame(f, false)) {
            server.sendServerChat(f.getName() + " could not be loaded");
        } else {
            server.remapConnIds(nameToIdMap, idToNameMap);
            // update all the clients with the new game info
            Enumeration<IConnection> connEnum = server.getConnections();
            while (connEnum.hasMoreElements()) {
                IConnection conn = connEnum.nextElement();
                server.sendCurrentInfo(conn.getId());
            }
        }
    }

}
