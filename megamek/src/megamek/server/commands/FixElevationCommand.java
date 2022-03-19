/*
 * MegaMek - Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
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

import megamek.common.Building;
import megamek.common.Entity;
import megamek.server.Server;

/**
 * @author Coelocanth
 * @since April 18, 2002, 11:53 PM
 */
public class FixElevationCommand extends ServerCommand {

    /** Creates new FixElevationCommand */
    public FixElevationCommand(Server server) {
        super(server, "fixelevation",
                "Fix elevation of any units that are messed up");
    }

    /**
     * Run this command with the arguments supplied
     */
    @Override
    public void run(int connId, String[] args) {
        int countbad = 0;
        for (Entity entity : server.getGame().getEntitiesVector()) {
            if (entity.fixElevation()) {
                Building bldg = server.getGame().getBoard().getBuildingAt(entity.getPosition());
                if (bldg != null) {
                    server.checkForCollapse(bldg, server.getGame().getPositionMap(), entity.getPosition(), true, server.getvPhaseReport());
                }
                server.sendServerChat(entity.getDisplayName()
                        + " elevation fixed, see megameklog.txt for details & report a bug if you know how this happened");
                countbad++;
            }
        }
        server.sendServerChat(connId, "" + countbad
                + " unit(s) had elevation problems");
    }

}
