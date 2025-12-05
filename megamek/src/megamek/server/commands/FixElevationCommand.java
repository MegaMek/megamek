/*
 * Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2005-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */

package megamek.server.commands;

import megamek.common.units.Entity;
import megamek.common.units.IBuilding;
import megamek.server.Server;
import megamek.server.totalWarfare.TWGameManager;

/**
 * @author Coelocanth
 * @since April 18, 2002, 11:53 PM
 */
public class FixElevationCommand extends ServerCommand {

    private final TWGameManager gameManager;

    /** Creates new FixElevationCommand */
    public FixElevationCommand(Server server, TWGameManager gameManager) {
        super(server, "fixelevation",
              "Fix elevation of any units that are messed up");
        this.gameManager = gameManager;
    }

    /**
     * Run this command with the arguments supplied
     */
    @Override
    public void run(int connId, String[] args) {
        int countBad = 0;
        for (Entity entity : gameManager.getGame().getEntitiesVector()) {
            if (entity.fixElevation()) {
                IBuilding bldg = gameManager.getGame().getBoard().getBuildingAt(entity.getPosition());
                if (bldg != null) {
                    gameManager.checkForCollapse(bldg, entity.getPosition(), true, gameManager.getMainPhaseReport());
                }
                server.sendServerChat(entity.getDisplayName()
                      + " elevation fixed, see megamek.log for details & report a bug if you know how this happened");
                countBad++;
            }
        }
        server.sendServerChat(connId, countBad + " unit(s) had elevation problems");
    }
}
