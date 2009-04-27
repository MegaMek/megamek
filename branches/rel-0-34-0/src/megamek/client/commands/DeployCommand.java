/*
 * MegaMek -
 * Copyright (C) 2007 Ben Mazur (bmazur@sev.org)
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
package megamek.client.commands;

import megamek.client.Client;
import megamek.common.Coords;

/**
 * @author dirk
 */
public class DeployCommand extends ClientCommand {

    /**
     * @param client
     * @param name
     * @param helpText
     */
    public DeployCommand(Client client) {
        super(
                client,
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
            client.getBoard();
            return "The legal deployment zone is: " + legalDeploymentZone();
        } else if (args.length == 5) {
            int id = Integer.parseInt(args[1]);
            Coords coord = new Coords(Integer.parseInt(args[2]) - 1, Integer
                    .parseInt(args[3]) - 1);
            int nFacing = getDirection(args[4]);

            client.deploy(id, coord, nFacing);
            return "Unit " + id + " deployed to " + coord.toFriendlyString()
                    + ". (this is assuming it worked. No error checking done.)";
        }

        return "Wrong number of arguments supplied. No deployment done.";
    }

    public String legalDeploymentZone() {
        int width = client.getBoard().getWidth();
        int height = client.getBoard().getHeight();
        int nDir = client.getLocalPlayer().getStartingPos();
        int minx = 0;
        int maxx = width;
        int miny = 0;
        int maxy = height;
        String deep = "";
        if (nDir > 10) {
            // Deep deployment, the board is effectively smaller
            nDir -= 10;
            deep = "Deep ";
            minx = width / 5;
            maxx -= width / 5;
            miny = height / 5;
            maxy -= height / 5;
        }
        switch (nDir) {
            case 0: // Any
                return deep + "Deploy nearly anywhere. MinX: " + (minx + 1)
                        + " MinY: " + (miny + 1) + " MaxX: " + (maxx + 1)
                        + " MaxY: " + (maxy + 1);
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
                        + (Math.max(minx, width / 3) + 1) + " MinY: "
                        + (Math.max(miny, height / 3) + 1) + " MaxX: "
                        + (Math.min(maxx, 2 * width / 3) + 1) + " MaxY: "
                        + (Math.min(miny, 2 * height / 3) + 1);
            default: // ummm. .
                return "Something went wrong, unknown deployment schema.";
        }
    }
}
