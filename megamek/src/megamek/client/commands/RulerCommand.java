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
import megamek.client.TwGameClient;
import megamek.common.Coords;
import megamek.common.LosEffects;
import megamek.common.TargetRoll;
import megamek.common.ToHitData;

/**
 * @author dirk
 *         This is the ruler for LOS stuff implemented in command line.
 *         There should be a more intuitive ruler.
 */
public class RulerCommand extends ClientCommand {

    public RulerCommand(TwGameClient client) {
        super(
                client,
                "ruler",
                "Show Line of Sight (LOS) information between two points of the map. " +
                        "Usage: #ruler x1 y1 x2 y2 [elev1 [elev2]]. " +
                        "Where x1, y1 and x2, y2 are the coordinates of the tiles, and the optional elev " +
                        "numbers are the elevations of the targets over the terrain. " +
                        "If elev is not given 1 is assumed which is for standing mechs. " +
                        "Prone mechs and most other units are at elevation 0. " +
                        "cur hex can be given as the start hex, which will use the client's current hex. " +
                        "eg; #ruler cur hex x2 y2, or as a short form #ruler x2 y2, " +
                        "in which case elevation can't be used.");
    }

    @Override
    public String run(String[] args) {
        try {
            int elev1 = 1, elev2 = 1;
            Coords start = null, end = null;
            String toHit1 = "", toHit2 = "";
            ToHitData thd;

            if (args.length == 3) {
                start = client.getCurrentHex();
                end = new Coords(Integer.parseInt(args[1]) - 1, Integer.parseInt(args[2]) - 1);
            } else {
                if (args[1].equals("cur")) {
                    start = client.getCurrentHex();
                } else {
                    start = new Coords(Integer.parseInt(args[1]) - 1, Integer.parseInt(args[2]) - 1);
                }
                end = new Coords(Integer.parseInt(args[3]) - 1, Integer.parseInt(args[4]) - 1);
                if (args.length > 5) {
                    try {
                        elev1 = Integer.parseInt(args[5]);
                    } catch (NumberFormatException e) {
                        // leave at default value
                    }
                    if (args.length > 6) {
                        try {
                            elev1 = Integer.parseInt(args[6]);
                        } catch (NumberFormatException e) {
                            // leave at default value
                        }
                    }
                }
            }

            thd = LosEffects.calculateLos(getClient().getGame(),
                    LosEffects.buildAttackInfo(start, end, elev1, elev2,
                            getClient().getBoard().getHex(start).floor(),
                            getClient().getBoard().getHex(end).floor())
            ).losModifiers(getClient().getGame());

            if (thd.getValue() != TargetRoll.IMPOSSIBLE) {
                toHit1 = thd.getValue() + " because ";
            }
            toHit1 += thd.getDesc();

            thd = LosEffects.calculateLos(getClient().getGame(),
                    LosEffects.buildAttackInfo(end, start, elev2, elev1,
                            getClient().getBoard().getHex(end).floor(),
                            getClient().getBoard().getHex(start).floor())
            ).losModifiers(getClient().getGame());

            if (thd.getValue() != TargetRoll.IMPOSSIBLE) {
                toHit2 = thd.getValue() + " because  ";
            }
            toHit2 += thd.getDesc();

            return "The ToHit from hex (" + (start.getX() + 1) + ", "
                   + (start.getY() + 1) + ") at elevation " + elev1 + " to ("
                   + (end.getX() + 1) + ", " + (end.getY() + 1) + ") at elevation "
                   + elev2 + " has a range of " + start.distance(end)
                   + " and a modifier of " + toHit1
                   + " and return fire has a modifier of " + toHit2 + ".";
        } catch (Exception ignored) {

        }

        return "Error parsing the ruler command. Usage: #ruler x1 y1 x2 y2 [elev1 [elev2]] where x1, y1, x2, y2, and the optional elev arguments are integers.";
    }
}
