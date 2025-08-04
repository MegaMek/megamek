/*
 * Copyright (C) 2007 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2018-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.client.commands;

import java.util.Vector;

import megamek.client.ui.clientGUI.ClientGUI;
import megamek.common.Coords;

/**
 * @author dirk
 */
public class DeployCommand extends ClientCommand {

    public DeployCommand(ClientGUI clientGUI) {
        super(
              clientGUI,
              "deploy",
              "This command deploys a given unit to the specified hex. Usage: '#deploy unit x y facing' where unit is the unit id number and x and y are the coordinates of the hex, and facing is the direction it's looking in. #deploy without any options will provide legal deployment zones.");
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.client.commands.ClientCommand#run(java.lang.String[])
     */
    // FIXME: Add error checking
    @Override
    public String run(String[] args) {
        if (args.length == 1) {
            return "The legal deployment zone is: " + legalDeploymentZone();
        } else if (args.length == 5) {
            int id = Integer.parseInt(args[1]);
            Coords coord = new Coords(Integer.parseInt(args[2]) - 1, Integer
                  .parseInt(args[3]) - 1);
            int nFacing = getDirection(args[4]);

            getClient().deploy(id, coord, 0, nFacing, 0, new Vector<>(), false);
            return "Unit " + id + " deployed to " + coord.toFriendlyString()
                  + ". (this is assuming it worked. No error checking done.)";
        }

        return "Wrong number of arguments supplied. No deployment done.";
    }

    public String legalDeploymentZone() {
        int width = getClient().getBoard().getWidth();
        int height = getClient().getBoard().getHeight();
        int nDir = getClient().getLocalPlayer().getStartingPos();
        int minHorizontal = 0;
        int maxHorizontal = width;
        int minVertical = 0;
        int maxVertical = height;
        String deep = "";
        if (nDir > 10) {
            // Deep deployment, the board is effectively smaller
            nDir -= 10;
            deep = "Deep ";
            minHorizontal = width / 5;
            maxHorizontal -= width / 5;
            minVertical = height / 5;
            maxVertical -= height / 5;
        }
        switch (nDir) {
            case 0: // Any
                return deep + "Deploy nearly anywhere. MinX: " + (minHorizontal + 1)
                      + " MinY: " + (minVertical + 1) + " MaxX: " + (maxHorizontal + 1)
                      + " MaxY: " + (maxVertical + 1);
            case 1: // NW
                return deep + "Deploy NW.";
            case 2: // N
                return deep + "Deploy N.";
            case 3: // NE
                return deep + "Deploy NE.";
            case 4: // E
                return deep + "Deploy E.";
            case 5: // SE
                return deep + "Deploy SE.";
            case 6: // S
                return deep + "Deploy S.";
            case 7: // SW
                return deep + "Deploy SW.";
            case 8: // W
                return deep + "Deploy W.";
            case 9: // Edge
                return deep + "Deploy at any edge.";
            case 10: // Centre
                return deep + "Deploy in the center. MinX: "
                      + (Math.max(minHorizontal, width / 3) + 1) + " MinY: "
                      + (Math.max(minVertical, height / 3) + 1) + " MaxX: "
                      + (Math.min(maxHorizontal, 2 * width / 3) + 1) + " MaxY: "
                      + (Math.min(maxVertical, 2 * height / 3) + 1);
            default:
                return "Something went wrong, unknown deployment schema.";
        }
    }
}
