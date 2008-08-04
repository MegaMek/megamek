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
import megamek.common.LosEffects;
import megamek.common.TargetRoll;
import megamek.common.ToHitData;

/**
 * @author dirk 
 * This is the ruler for LOS stuff implemented in command line.
 * There should be a more intuitive ruler.
 */
public class RulerCommand extends ClientCommand {

    public RulerCommand(Client client) {
        super(
                client,
                "ruler",
                "Show Line of Sight (LOS) information between two points of the map. Usage: #ruler x1 y1 x2 y2 [elev1 [elev2]]. Where x1, y1 and x2, y2 are the coordiantes of the tiles, and the optional elev numbers are the elevations of the targets over the terrain. If elev is not given 1 is assumed which is for standing mechs. Prone mechs and most other units are at elevation 0.");
    }

    public String run(String[] args) {
        try {
            int elev1 = 1, elev2 = 1;
            Coords start = null, end = null;
            String toHit1 = "", toHit2 = "";
            ToHitData thd;

            start = new Coords(Integer.parseInt(args[1]) - 1, Integer
                    .parseInt(args[2]) - 1);
            end = new Coords(Integer.parseInt(args[3]) - 1, Integer
                    .parseInt(args[4]) - 1);
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

            thd = LosEffects.calculateLos(client.game,
                    buildAttackInfo(start, end, elev1, elev2)).losModifiers(
                    client.game);
            if (thd.getValue() != TargetRoll.IMPOSSIBLE) {
                toHit1 = thd.getValue() + " because "; //$NON-NLS-1$
            }
            toHit1 += thd.getDesc();

            thd = LosEffects.calculateLos(client.game,
                    buildAttackInfo(end, start, elev2, elev1)).losModifiers(
                    client.game);
            if (thd.getValue() != TargetRoll.IMPOSSIBLE) {
                toHit2 = thd.getValue() + " because  "; //$NON-NLS-1$
            }
            toHit2 += thd.getDesc();

            return "The ToHit from hex (" + (start.x + 1) + ", "
                    + (start.y + 1) + ") at elevation " + elev1 + " to ("
                    + (end.x + 1) + ", " + (end.y + 1) + ") at elevation "
                    + elev2 + " has a range of " + start.distance(end)
                    + " and a modifier of " + toHit1
                    + " and return fire has a modifier of " + toHit2 + ".";
        } catch (NumberFormatException nfe) {
        } catch (NullPointerException npe) {
        } catch (IndexOutOfBoundsException ioobe) {
        }

        return "Error parsing the ruler command. Usage: #ruler x1 y1 x2 y2 [elev1 [elev2]] where x1, y1, x2, y2, and the optional elev agruments are integers.";
    }

    /**
     * Build line of sight effects between coordinates c1 and c2 at height h1
     * and h2 respectivly.
     * 
     * @param c1 the source coordiantes.
     * @param c2 the target coordinates.
     * @param h1 the height in the source tile that is being shot from.
     * @param h2 the height of the target tile to shoot for.
     * @return an attackInfo object that describes the apliable modifiers.
     */
    private LosEffects.AttackInfo buildAttackInfo(Coords c1, Coords c2, int h1,
            int h2) {
        LosEffects.AttackInfo ai = new LosEffects.AttackInfo();
        ai.attackPos = c1;
        ai.targetPos = c2;
        ai.attackHeight = h1;
        ai.targetHeight = h2;
        ai.attackAbsHeight = client.getBoard().getHex(c1).floor() + h1;
        ai.targetAbsHeight = client.getBoard().getHex(c2).floor() + h2;
        return ai;
    }

}
