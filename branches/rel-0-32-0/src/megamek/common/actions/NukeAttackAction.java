/**
 * MegaMek - Copyright (C) 2004 Ben Mazur (bmazur@sev.org)
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

package megamek.common.actions;

import megamek.common.*;
import java.io.Serializable;
/**
 *
 * NukeAttackAction--not *actually* an action, but here's just as good as anywhere.  Holds the data needed for an artillery attack in flight.
 */
public class NukeAttackAction extends ArtilleryAttackAction implements Serializable {
    public static int TYPE_GENERIC = 0;
    public static int TYPE_SPECIFIC = 1;

    public int attackType = -1;
    public int nukeClass = -1;
    public Coords target;
    public int damage = -1;
    public int secondaryRadius = -1;
    public int degeneration = -1;
    public int craterDepth = -1;

    public NukeAttackAction(Coords t, int d, int sr, int dg, int cd, int pl) {
        super();
        attackType = TYPE_GENERIC;
        target = t;
        damage = d;
        secondaryRadius = sr;
        degeneration = dg;
        craterDepth = cd;
        playerId = pl;
    }

    public NukeAttackAction(Coords t, int nukeType, int pl) {
        super();
        attackType = TYPE_SPECIFIC;
        target = t;
        nukeClass = nukeType;
        playerId = pl;
    }
}
